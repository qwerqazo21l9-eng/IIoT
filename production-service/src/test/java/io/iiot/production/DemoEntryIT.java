package io.iiot.production;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ProductionApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoEntryIT {
    @LocalServerPort int port;

    @Test
    void commandEntryChecksReadinessAndReproducesSeedConfigurationAndResultVersions() throws Exception {
        var ready = invoke("Check", "http://127.0.0.1:" + port, null);
        assertEquals(0, ready.exit(), ready.output());
        assertEquals("UP", new ObjectMapper().readTree(ready.output()).path("status").asText());
        var first = invoke("Run", "http://127.0.0.1:" + port, "demo-first-" + UUID.randomUUID());
        assertEquals(0, first.exit(), first.output());
        var second = invoke("Run", "http://127.0.0.1:" + port, "demo-second-" + UUID.randomUUID());
        assertEquals(0, second.exit(), second.output());
        JsonNode a = new ObjectMapper().readTree(first.output());
        JsonNode b = new ObjectMapper().readTree(second.output());
        assertEquals(42, a.path("run").path("configuration").path("seed").asInt());
        assertEquals("COMPLETED", a.path("run").path("status").asText());
        assertEquals(a.path("run").path("resultVersion"), b.path("run").path("resultVersion"));
        assertFalse(a.path("run").path("resultVersion").path("configuration").asText().isBlank());
        for (String field : List.of("goodUnits", "rejectedUnits", "goodUnitsPerHour", "endingWorkInProcess")) {
            assertEquals(a.path("metrics").path(field), b.path("metrics").path(field), field);
        }
    }

    record CommandResult(int exit, String output) {}

    CommandResult invoke(String mode, String api, String runId) throws Exception {
        String shell = System.getProperty("os.name").startsWith("Windows") ? "powershell.exe" : "pwsh";
        var arguments = new ArrayList<>(List.of(shell, "-NoProfile", "-File",
                Path.of("..", "scripts", "demo.ps1").toAbsolutePath().normalize().toString(),
                "-Mode", mode, "-ApiUrl", api, "-NoStart", "-TimeoutSeconds", "5"));
        if (runId != null) arguments.addAll(List.of("-RunId", runId));
        var process = new ProcessBuilder(arguments).redirectErrorStream(true).start();
        boolean finished = process.waitFor(420, TimeUnit.SECONDS);
        if (!finished) process.destroyForcibly();
        assertTrue(finished, "Demonstration command must terminate");
        return new CommandResult(process.exitValue(), new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).strip());
    }
}
