param(
    [ValidateSet('Run', 'Check')][string]$Mode = 'Run',
    [string]$ApiUrl = 'http://127.0.0.1:18085',
    [switch]$NoStart,
    [string]$RunId,
    [ValidateRange(1, 600)][int]$TimeoutSeconds = 180
)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
$api = [Uri]$ApiUrl
if (-not $api.IsLoopback) { throw 'The local demonstration requires a loopback API URL' }

function Read-Ready {
    # PostgreSQL, Kafka and protocol checks have a combined 15-second budget.
    $ready = Invoke-RestMethod -Uri "$ApiUrl/api/ready" -TimeoutSec 20
    if ($ready.status -ne 'UP') { throw 'Required services are not ready' }
    return $ready
}

try {
    if ($Mode -eq 'Run' -and -not $NoStart) {
        Push-Location $repoRoot
        try {
            & docker compose -f deploy/compose.yaml up -d --build --wait --wait-timeout $TimeoutSeconds
            if ($LASTEXITCODE -ne 0) { throw 'Docker services did not become ready' }
            $alreadyReady = $false
            try { $null = Read-Ready; $alreadyReady = $true } catch { }
            if (-not $alreadyReady) {
                & mvn -B -ntp package '-DskipTests'
                if ($LASTEXITCODE -ne 0) { throw 'Java service build failed' }
                $jar = Join-Path $repoRoot 'production-service/target/production-service-0.1.0-SNAPSHOT.jar'
                $target = Join-Path $repoRoot 'target'
                $null = New-Item -ItemType Directory -Path $target -Force
                $launch = @{
                    FilePath = (Get-Command java).Source
                    ArgumentList = @('--add-opens=java.base/java.lang=ALL-UNNAMED', '--add-opens=java.base/java.util=ALL-UNNAMED',
                        '-jar', ('"' + $jar + '"'), "--server.port=$($api.Port)", '--server.address=127.0.0.1')
                    WorkingDirectory = $repoRoot
                    RedirectStandardOutput = (Join-Path $target 'service.log')
                    RedirectStandardError = (Join-Path $target 'service.err.log')
                    PassThru = $true
                }
                if ($env:OS -eq 'Windows_NT') { $launch.WindowStyle = 'Hidden' }
                $serviceProcess = Start-Process @launch
                $serviceProcess.Id | Set-Content (Join-Path $target 'service.pid')
                $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
                while ($true) {
                    if ($serviceProcess.HasExited) { throw 'Java service exited; see target/service.err.log' }
                    try { $null = Read-Ready; break } catch {
                        if ((Get-Date) -ge $deadline) { throw 'Required services did not become ready before the deadline' }
                        Start-Sleep -Milliseconds 500
                    }
                }
            }
        } finally { Pop-Location }
    }
    $ready = Read-Ready
    if ($Mode -eq 'Check') {
        $ready | ConvertTo-Json -Depth 20 -Compress
        exit 0
    }
    $configuration = Get-Content -LiteralPath (Join-Path $repoRoot 'deploy/normal-v1.json') -Raw | ConvertFrom-Json
    if ($RunId) { $configuration.runId = $RunId }
    else { $configuration.runId = 'normal-demo-v1-' + [Guid]::NewGuid().ToString('N') }
    $run = Invoke-RestMethod -Uri "$ApiUrl/api/runs" -Method Post -ContentType 'application/json' `
        -Body ($configuration | ConvertTo-Json -Depth 10 -Compress) -TimeoutSec 360
    if ($run.status -ne 'COMPLETED') { throw "Production run did not complete: $($run.status)" }
    $encodedId = [Uri]::EscapeDataString($run.runId)
    $metrics = Invoke-RestMethod -Uri "$ApiUrl/api/lines/line-a/metrics?runId=$encodedId&startMillis=0&endMillis=$($run.endMillis)" -TimeoutSec 10
    $report = @{configurationVersion = 'normal-v1'; run = $run; metrics = $metrics}
    $encodedReport = $report | ConvertTo-Json -Depth 30 -Compress
    $target = Join-Path $repoRoot 'target'
    $null = New-Item -ItemType Directory -Path $target -Force
    $encodedReport | Set-Content -LiteralPath (Join-Path $target 'normal-demo-result.json') -Encoding UTF8
    Write-Output $encodedReport
    exit 0
} catch {
    [Console]::Error.WriteLine("Demonstration failed: $($_.Exception.Message)")
    exit 1
}
