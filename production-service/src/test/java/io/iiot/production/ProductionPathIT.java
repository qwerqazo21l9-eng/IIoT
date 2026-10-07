package io.iiot.production;

import com.fasterxml.jackson.databind.JsonNode;
import io.iiot.simulator.ProductionLine;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ProductionApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductionPathIT {
    @LocalServerPort int port;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void unknownBatchAndInvalidIntervalsDoNotReturnInventedZeroMetrics() throws Exception {
        var configuration = new ProductionLine.Configuration("invalid-query-" + UUID.randomUUID(), 42, 1);
        var run = post("/api/runs", configuration);
        String base = "/api/lines/line-a/metrics?runId=" + configuration.runId();
        assertEquals(404, getResponse(base + "&startMillis=0&endMillis=" + run.path("endMillis").asLong() + "&batchId=unknown").statusCode());
        assertEquals(400, getResponse(base + "&startMillis=-1&endMillis=1").statusCode());
        assertEquals(400, getResponse(base + "&startMillis=2&endMillis=1").statusCode());
        assertEquals(400, getResponse(base + "&startMillis=0&endMillis=1&warmupMillis=2").statusCode());
    }

    @Test
    void warmupHalfOpenEndpointsBatchAndZeroDurationUsePersistedFlinkContributions() throws Exception {
        var configuration = new ProductionLine.Configuration("boundaries-" + UUID.randomUUID(), 42, 12,
                List.of(100L, 200L, 100L, 100L), 1,
                List.of(new ProductionLine.Batch("batch-a", "A", 6), new ProductionLine.Batch("batch-b", "B", 6)), 500, 0.2);
        var observations = new ProductionLine().run(configuration);
        post("/api/runs", configuration);
        long firstGood = observations.stream().filter(e -> e.type() == ProductionLine.Type.GOOD_UNIT)
                .findFirst().orElseThrow().timeMillis();
        long end = observations.get(observations.size() - 1).timeMillis() + 1;
        var warmed = get("/api/lines/line-a/metrics?runId=" + configuration.runId()
                + "&startMillis=0&endMillis=" + end + "&warmupMillis=" + (firstGood + 1));
        long warmedGood = observations.stream().filter(e -> e.type() == ProductionLine.Type.GOOD_UNIT
                && e.timeMillis() > firstGood).count();
        assertEquals(warmedGood, warmed.path("goodUnits").asLong());
        assertEquals(firstGood + 1, warmed.path("intervalStartMillis").asLong());
        assertEquals(warmedGood * 3_600_000.0 / (end - firstGood - 1), warmed.path("goodUnitsPerHour").asDouble(), 0.000001);
        for (long[] range : List.of(new long[]{0, firstGood}, new long[]{firstGood, firstGood + 1},
                new long[]{firstGood, firstGood}, new long[]{firstGood + 1, end})) {
            for (String batch : List.of("", "batch-a", "batch-b")) {
                var selected = observations.stream().filter(e -> batch.isEmpty() || e.batchId().equals(batch)).toList();
                var metrics = get("/api/lines/line-a/metrics?runId=" + configuration.runId()
                        + "&startMillis=" + range[0] + "&endMillis=" + range[1] + (batch.isEmpty() ? "" : "&batchId=" + batch));
                long good = selected.stream().filter(e -> e.type() == ProductionLine.Type.GOOD_UNIT
                        && e.timeMillis() >= range[0] && e.timeMillis() < range[1]).count();
                long rejected = selected.stream().filter(e -> e.type() == ProductionLine.Type.REJECTED_UNIT
                        && e.timeMillis() >= range[0] && e.timeMillis() < range[1]).count();
                var entered = selected.stream().filter(e -> e.station() == 1 && e.type() == ProductionLine.Type.OPERATION_STARTED
                        && e.timeMillis() < range[1]).map(ProductionLine.Observation::workpieceId).collect(java.util.stream.Collectors.toSet());
                var exited = selected.stream().filter(e -> (e.type() == ProductionLine.Type.GOOD_UNIT
                        || e.type() == ProductionLine.Type.REJECTED_UNIT) && e.timeMillis() < range[1])
                        .map(ProductionLine.Observation::workpieceId).collect(java.util.stream.Collectors.toSet());
                entered.removeAll(exited);
                assertEquals(good, metrics.path("goodUnits").asLong());
                assertEquals(rejected, metrics.path("rejectedUnits").asLong());
                assertEquals(entered.size(), metrics.path("endingWorkInProcess").asInt());
                if (range[0] == range[1]) assertTrue(metrics.path("goodUnitsPerHour").isNull());
                else assertEquals(good * 3_600_000.0 / (range[1] - range[0]), metrics.path("goodUnitsPerHour").asDouble(), 0.000001);
            }
        }
    }

    @Test
    void realProtocolKafkaFlinkPersistenceAndHttpMetricsMatchIndependentOutletAccounting() throws Exception {
        var configuration = new ProductionLine.Configuration("acceptance-" + UUID.randomUUID(), 42, 12,
                List.of(100L, 200L, 100L, 100L), 1,
                List.of(new ProductionLine.Batch("batch-a", "A", 6), new ProductionLine.Batch("batch-b", "B", 6)), 500, 0.2);
        var observations = new ProductionLine().run(configuration);
        var response = post("/api/runs", configuration);
        assertEquals("COMPLETED", response.path("status").asText());
        long end = observations.get(observations.size() - 1).timeMillis() + 1;
        var metrics = get("/api/lines/line-a/metrics?runId=" + configuration.runId() + "&startMillis=0&endMillis=" + end);
        long good = observations.stream().filter(e -> e.type() == ProductionLine.Type.GOOD_UNIT).count();
        long rejected = observations.stream().filter(e -> e.type() == ProductionLine.Type.REJECTED_UNIT).count();
        assertEquals(good, metrics.path("goodUnits").asLong());
        assertEquals(rejected, metrics.path("rejectedUnits").asLong());
        assertEquals(0, metrics.path("endingWorkInProcess").asInt());
        assertEquals(good * 3_600_000.0 / end, metrics.path("goodUnitsPerHour").asDouble(), 0.000001);
        assertEquals("COMPLETE", metrics.path("dataCompleteness").asText());
        assertEquals("mapping-v1", metrics.path("resultVersion").path("mapping").asText());
        var events = get("/api/runs/" + configuration.runId() + "/events");
        assertEquals(observations.size(), events.size());
        for (var event : events) {
            assertEquals(configuration.runId(), event.path("runId").asText());
            assertEquals("GOOD", event.path("quality").asText());
            assertFalse(event.path("edgexEventId").asText().isBlank());
            assertEquals("opc.tcp", event.path("protocol").asText());
            assertFalse(event.toString().contains("faultTruth"));
        }
    }

    JsonNode post(String path, Object payload) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build();
        var result = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, result.statusCode(), result.body());
        return json.readTree(result.body());
    }

    JsonNode get(String path) throws Exception {
        var result = getResponse(path);
        assertEquals(200, result.statusCode(), result.body());
        return json.readTree(result.body());
    }

    HttpResponse<String> getResponse(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
