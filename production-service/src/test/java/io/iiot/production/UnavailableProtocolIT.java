package io.iiot.production;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.iiot.simulator.ProductionLine;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ProductionApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "iiot.bridge=http://127.0.0.1:1")
class UnavailableProtocolIT {
    @LocalServerPort int port;

    @Test
    void unavailableProtocolHasNamedFailureAndCannotCreateRunOrCommandReport() throws Exception {
        var http = HttpClient.newHttpClient();
        String api = "http://127.0.0.1:" + port;
        var ready = http.send(HttpRequest.newBuilder(URI.create(api + "/api/ready")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(503, ready.statusCode());
        assertTrue(ready.body().contains("opcUaAndEdgeX"), ready.body());
        var configuration = new ProductionLine.Configuration("unavailable-" + UUID.randomUUID(), 42, 1);
        var submitted = http.send(HttpRequest.newBuilder(URI.create(api + "/api/runs"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(configuration))).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(503, submitted.statusCode());
        var run = http.send(HttpRequest.newBuilder(URI.create(api + "/api/runs/" + configuration.runId())).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, run.statusCode());
        String shell = System.getProperty("os.name").startsWith("Windows") ? "powershell.exe" : "pwsh";
        var command = new ProcessBuilder(shell, "-NoProfile", "-File",
                Path.of("..", "scripts", "demo.ps1").toAbsolutePath().normalize().toString(),
                "-NoStart", "-ApiUrl", api).redirectErrorStream(true).start();
        boolean finished = command.waitFor(30, TimeUnit.SECONDS);
        if (!finished) command.destroyForcibly();
        assertTrue(finished, "Unavailable command must terminate");
        String output = new String(command.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertNotEquals(0, command.exitValue(), output);
        assertTrue(output.contains("Demonstration failed"), output);
        assertFalse(output.contains("goodUnitsPerHour"), output);
    }
}
