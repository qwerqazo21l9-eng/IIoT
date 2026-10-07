package io.iiot.production;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.iiot.simulator.ProductionLine;
import org.junit.jupiter.api.Test;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class PackagedPathIT {
    @Test
    void executableJarRunsActualFlinkJobAndReturnsPersistedMetrics() throws Exception {
        int port;
        try (var socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        var log = Files.createTempFile("iiot-packaged-path-", ".log");
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.util=ALL-UNNAMED",
                "-jar", "target/production-service-0.1.0-SNAPSHOT.jar", "--server.port=" + port,
                "--server.address=127.0.0.1").redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            String api = "http://127.0.0.1:" + port;
            boolean ready = false;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
            while (process.isAlive() && System.nanoTime() < deadline) {
                try {
                    var response = http.send(HttpRequest.newBuilder(URI.create(api + "/api/ready"))
                            .timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) { ready = true; break; }
                } catch (java.io.IOException ignored) { }
                Thread.sleep(200);
            }
            assertTrue(ready, Files.readString(log));
            var configuration = new ProductionLine.Configuration("packaged-" + UUID.randomUUID(), 42, 1);
            var json = new ObjectMapper();
            var run = http.send(HttpRequest.newBuilder(URI.create(api + "/api/runs"))
                    .timeout(Duration.ofSeconds(120)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(configuration))).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, run.statusCode(), run.body() + "\n" + Files.readString(log));
            long end = json.readTree(run.body()).path("endMillis").asLong();
            var metrics = http.send(HttpRequest.newBuilder(URI.create(api + "/api/lines/line-a/metrics?runId="
                    + configuration.runId() + "&startMillis=0&endMillis=" + end)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, metrics.statusCode(), metrics.body());
            assertEquals(1, json.readTree(metrics.body()).path("goodUnits").asInt());
            assertEquals(0, json.readTree(metrics.body()).path("endingWorkInProcess").asInt());
        } finally {
            process.destroy();
            if (!process.waitFor(10, TimeUnit.SECONDS)) process.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
        }
    }
}
