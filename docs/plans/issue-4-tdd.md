# Issue #4 TDD implementation and acceptance

Date: 2026-10-06; continued 2026-10-07. Parent: [#1](https://github.com/qwerqazo21l9-eng/IIoT/issues/1).
Child: [#4](https://github.com/qwerqazo21l9-eng/IIoT/issues/4).

## Preflight

The current GitHub issue and complete parent PRD were read. #4 has
`ready-for-agent`, no dependencies, no comments and headless UI mode.
Repository skill configuration already exists in AGENTS.md and docs/agents/.
CONTEXT.md and ADR-0001 through ADR-0006 were read for this boundary.

| Required reading | Summary |
| --- | --- |
| Implementation decisions | Real OPC UA/EdgeX, Kafka/Java Flink, PostgreSQL and Spring Boot form the first path; stable observable identities and versioned results are required, hidden truth is isolated, outlet-only JPH excludes warmup, and startup must check readiness. |
| Test decisions | Deterministic simulator inputs, real protocol mapping and Kafka/Flink persisted outputs have independent integration boundaries; synthetic events cannot replace protocol acceptance. |
| US-1, US-12, US-13, US-15 | Query line/batch/time metrics, trace device mappings and event quality, control reproducible simulation inputs with isolated truth, and deliver a reproducible local entry point. |

The simulator uses one public SDK operation, `ProductionLine.run`, returning
an immutable ordered observation list. Times are elapsed milliseconds from run
start. Station numbers are 1 through 4; BUFFER_CHANGED at station n refers to
the buffer between n and n+1. Workpiece identity on a buffer event identifies
the transferred piece. Non-buffer observations have null buffer occupancy.
The first product does not require a changeover; later product transitions do.
Normal timing jitter and quality randomness are keyed by workpiece identity
and use separate random streams. The whole finite order drains in this offline
SDK; it does not yet model a statistics-window cutoff or online delivery.

## Observed RED and GREEN

Each test was introduced separately and executed before its implementation.
Initial Maven dependency resolution failures were not counted as RED.

| Test | Observed RED | GREEN |
| --- | --- | --- |
| sameConfigurationAndSeedReproduceNonEmptyObservations | UnsupportedOperationException from the unimplemented simulation | Deterministic nonempty observations |
| everyWorkpieceCompletesFourStationsInProductionOrder | Expected eight route events, observed one | Ordered start/end facts for all four stations |
| slowDownstreamBlocksUpstreamWithoutOverflowingFiniteBuffers | No buffer observations | Concurrent processing, bounded queues and blocking |
| productBatchesHaveExplicitChangeoverAndOneFinalQualityDecisionPerWorkpiece | Expected products A and B, observed only A | Explicit station changeovers and unique outlet decisions |
| invalidRunConfigurationsAreRejectedBeforeSimulation | Missing IllegalArgumentException | Validated immutable configuration |
| diagnosticEventStreamKeepsVersionedObservableFactsWithoutHiddenTruth | Missing DiagnosticEventStream type | Versioned observable event stream with stable line, station, sequence, time, quality and format facts; hidden truth keys absent |
| lineMetricsUseOutletQualityOnlyWithWarmupHalfOpenIntervalAndNoJphForZeroDuration | Missing LineMetricsQuery type, then invalid test range order | Outlet-only good/rejected counts, half-open statistics interval, warmup exclusion and empty JPH for zero-duration intervals |
| stationPauseChangesOnlySelectedOperationDurationAndPreservesQuality (stations 1-4) | Missing Pause input | Each station accepts a workpiece-specific extra duration; all other operation durations and final quality remain unchanged, repeat runs match and buffers remain bounded |
| pauseForUnknownWorkpieceIsRejectedBeforeRunning | Unknown workpiece silently ignored; expected exception not thrown | Cross-run and out-of-order workpiece identities are rejected before simulation; station and duration inputs are validated |
| observationGapSuppressesOnlySelectedStationAndPreservesSurvivingEventIdentity | Missing ObservationGap input | Station-specific half-open publication gap omits only matching observations, retains the endpoint event and preserves surviving identities, times and sequences |
| eachDeviceHasItsOwnContiguousSequenceAndEveryEventCarriesRunIdentity | Missing event runId; after adding it, device sequence assertion exposed global sequence gaps | Every event carries runId; each device independently numbers its observations from one before gap filtering |

Refactoring extracted shared operation-start and final-quality behavior.
The same public-interface tests were rerun after refactoring.

Verified environment: Microsoft JDK 21.0.9, Maven 3.9.12, Docker Desktop 4.55.0
with Linux Engine 29.1.3. Build targets Java 17; test dependencies are JUnit
5.11.4, compiler plugin 3.13.0 and Surefire 3.5.2. This was the initial simulator
environment; full-path compatibility and subsequent verification are recorded below.

## Initial Acceptance Plan (Now Implemented)

| Issue acceptance | Next RED boundary | PRD basis |
| --- | --- | --- |
| Reproducible simulation and isolated truth | Protocol publication with versioned configuration and independently held truth; offline pause/gap inputs now exist | Simulation and truth isolation; simulator input tests |
| Real OPC UA and EdgeX mapping | Actual protocol round-trip and mapped identity/sequence/time/quality/configuration version | Unified event contract; EdgeX ingestion tests |
| Kafka/Flink persistence and HTTP metrics | Real Kafka input through Flink/PostgreSQL to Spring Boot query, independently reconciled counts/JPH/WIP; warmup, interval endpoints and zero duration | Metrics and result versions; Kafka/Flink output tests; US-1 |
| Startup and reproducibility | Local startup failure when a required service is unavailable, then successful seeded full-path acceptance | Local demonstration delivery; US-15 |

At the end of the initial simulator cycles, fourteen simulator cases passed and
the real-service acceptance path had not yet been implemented. The subsequent
cycles below complete Issue #4's normal-production path. The wider Issue #1
diagnosis/approval/improvement loop remains for its other child issues.
No GitHub state or labels were changed.

## Full Service RED And GREEN

| Test | Observed RED | GREEN |
| --- | --- | --- |
| test_actual_opcua_and_edgex_preserve_observable_facts | Publication endpoint returned 501 | Actual OPC UA DataValue round-trip, EdgeX device-rest delivery, persisted Core Data read-back with matching identities, sequence, original time, quality and versions |
| realProtocolKafkaFlinkPersistenceAndHttpMetricsMatchIndependentOutletAccounting | Missing ProductionApplication | 196 real-protocol events through actual KafkaSource, Java Flink contribution computation and JDBC sink; persisted HTTP counts, JPH and ending WIP match independent outlet accounting |
| warmupHalfOpenEndpointsBatchAndZeroDurationUsePersistedFlinkContributions | Warmup query returned 11 good units instead of 10 | Warmup exclusion plus twelve range/batch scenarios agree with independent quality counts and workpiece entry/exit sets; zero-duration JPH is JSON null |
| commandEntryChecksReadinessAndReproducesSeedConfigurationAndResultVersions | Command entry threw not-implemented and exited 1 | Actual command readiness and two independent service runs reproduce seed, configuration hash, result versions and all queried metrics |
| unknownBatchAndInvalidIntervalsDoNotReturnInventedZeroMetrics | Unknown batch returned 200 instead of 404 | Unknown batch is 404; negative/reversed ranges and invalid warmup are 400 |
| unavailableProtocolHasNamedFailureAndCannotCreateRunOrCommandReport | Readiness reported an unnamed `null` failure | Named opcUaAndEdgeX 503, submission 503, absent run 404 and nonzero command exit without a metric report |
| executableJarRunsActualFlinkJobAndReturnsPersistedMetrics | Direct nested-JAR launch returned 503; JobMaster could not load ExecutionConfig through the system class loader | Standard executable JAR plus adjacent lib directory runs actual protocol/Kafka/Flink/persistence/HTTP path and yields one good unit with no ending WIP |

Startup environment failures were diagnosed separately: EdgeX's default Consul
hostname required network aliases; metadata batch responses contained individual
device-registration errors; single-direction device-rest definitions require
`other` protocol properties. Failsafe required the project's output directory
on its test classpath. Flink's connector-base runtime dependency was explicit.
These failures were fixed without replacing any service with a mock.

Refactoring extracted protocol read-back and Kafka publication from run
coordination. The complete public HTTP/command suite was rerun after refactoring
and after correcting the deployment packaging.

## Local Acceptance

- [x] Stable seeded four-station observations, bounded buffers, products/batches,
  changeover and final quality; observable payloads exclude hidden truth.
- [x] Automated actual OPC UA and EdgeX path with queryable mapped evidence.
- [x] Actual Kafka/Flink persisted contributions and HTTP good/rejected/JPH/WIP
  agree with independent accounting, including warmup and interval boundaries.
- [x] Readiness failures are explicit; seeded command runs and result versions
  reproduce; the built executable artifact has its own acceptance test.

Final combined verification on 2026-10-07 at 03:49:12 Asia/Shanghai:
`mvn -B -ntp -P integration verify` completed successfully in 6 minutes 37 seconds:
14 simulator cases and 6 service integration cases, zero failures/errors/skips.
The unchanged final protocol adapter also passed
`python -m unittest ingestion/test_protocol.py -v`: 1 case, no skips.
Total: 21 passing cases. Compose `up --build --wait` passed all health checks.

Runtime versions: Java 21.0.9 (build target 17), Maven 3.9.12, Spring Boot 3.4.4,
Flink 1.20.1, Kafka connector 3.3.0-1.20, Kafka 3.9.0, EdgeX 3.1.1,
PostgreSQL image 17-alpine with JDBC 42.7.5, asyncua 1.1.5 and aiohttp 3.11.16.
One observed container memory snapshot totaled approximately 478 MiB; this
does not include the host JVM or establish a peak-memory limit.
CI is configured in `.github/workflows/normal-production.yaml` but has not
been run on GitHub in this session. Code has not been committed or pushed.

See [entry, architecture and HTTP contract](../normal-production-path.md).

The default `./scripts/demo.ps1` entry was subsequently executed successfully
with the standard JAR/lib distribution. Run
`normal-demo-v1-fcfa62eca11946399976d11842acab02` completed with 196 evidence
events, seed 42, 11 good units, 1 rejected unit, ending WIP 0 and
JPH 8763.000663863686 over simulated `[0,4519)` milliseconds, including
changeover. The host API remains at http://127.0.0.1:18085.
The saved [acceptance snapshot](../evidence/issue-4-normal-production.json)
contains the actual configuration, result versions and HTTP metrics.

## Offline scenario inputs

`ProductionLine.run(configuration, pauses)` applies additional operation duration
to a selected station/workpiece. Multiple entries targeting the same operation
add their durations. A zero duration is a no-op. Pauses affect scheduling and
downstream propagation but do not consume the normal timing or quality random
streams. The pause inputs stay with the caller; output contains ordinary action,
buffer and quality facts, without injected fault labels.

`DiagnosticEventStream.from(configuration, observations, mapping, gaps)` suppresses
station observations in `[startMillis, endMillis)`. Zero-length gaps omit nothing.
Sequences are assigned per device before suppression so surviving observations
retain their source sequence. This models offline publication loss; it does not
yet implement online OPC UA delivery, gap detection or a separate truth artifact.
Do not use the current in-memory metrics query to claim completeness or valid
production metrics for an incomplete observation stream: quality/completeness
handling remains part of the real compute and HTTP acceptance cycles.

## Continued TDD Verification (2026-10-07)

The live Issue #4 and complete parent PRD were reopened. It remains
ready-for-agent, headless and dependency-free. Existing implementation and
recorded cycles above are baseline work, not claimed as newly test-first.

The initial full integration run passed 14 simulator cases, all four current
ProductionPathIT cases, the packaged-JAR case and unavailable-protocol case.
DemoEntryIT failed its first CLI readiness request with a timeout. A direct
repeat of the same CLI check returned UP. Isolated DemoEntryIT then passed
readiness but its 150-second process deadline killed a production request
that the command itself permits to take 240 seconds. A targeted JVM stack
confirmed the server was still publishing actual OPC UA/EdgeX observations.

The command readiness request now allows 20 seconds, covering the combined
PostgreSQL (3s), Kafka (4s) and protocol (8s) check budgets plus overhead.
An intermediate retry completed the first run but the second failed the
service's 180-second protocol publication limit. The final bounded budgets
are 240 seconds for protocol publication, 360 seconds for the CLI production
request (including Flink), and 420 seconds for its test subprocess.
Failure status and reproducibility assertions remain unchanged.

Additional HTTP coverage uses actual Spring Boot HTTP and PostgreSQL:

| Behavior | Observed result | Change |
| --- | --- | --- |
| Invalid configuration creates no run | Already returned 400; GET run returned 404 | Added regression coverage; not a RED cycle |
| JSON null run request is a parameter error | RED: 500 and NullPointerException after readiness | Reject missing configuration during request parsing, before readiness or persistence |

RunValidationIT is GREEN: both HTTP cases passed after the null guard.
PackagedPathIT and UnavailableProtocolIT also passed that intermediate run.
The final targeted regression passed at 2026-10-07 11:57:34 Asia/Shanghai:

```powershell
mvn -B -ntp -P integration verify '-Dit.test=DemoEntryIT,RunValidationIT,UnavailableProtocolIT' '-Dfailsafe.failIfNoSpecifiedTests=false'
```

It completed in 7 minutes 6 seconds: 14 simulator cases and four HTTP/CLI
integration cases, zero failures, errors or skips. DemoEntryIT's two actual
seeded runs reproduced configuration/result versions, good/rejected counts,
JPH and ending WIP. The earlier full-path and packaged-JAR successes above
were not repeated after the final timeout-only change; this is not a claim
that the initial full-suite command passed.

The independent actual OPC UA/EdgeX Python contract test also passed.
The final generated CLI report is preserved in
[continuation evidence](../evidence/issue-4-tdd-continuation-20261007.json).
No GitHub issue state, labels, commits or merge status were changed.
