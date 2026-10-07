package io.iiot.production;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.iiot.simulator.DiagnosticEventStream;
import io.iiot.simulator.ProductionLine;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ProductionService {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ProductionService.class);
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final String brokers, bridge, database, user, password;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public ProductionService(JdbcTemplate jdbc, ObjectMapper json, @Value("${iiot.kafka}") String brokers,
            @Value("${iiot.bridge}") String bridge, @Value("${spring.datasource.url}") String database,
            @Value("${spring.datasource.username}") String user, @Value("${spring.datasource.password}") String password) {
        this.jdbc = jdbc;
        this.json = json;
        this.brokers = brokers;
        this.bridge = bridge;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    public Map<String, Object> readiness() {
        var checks = new LinkedHashMap<String, Object>();
        String component = "postgresql";
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            checks.put("postgresql", "UP");
            component = "kafka";
            try (var admin = AdminClient.create(Map.of("bootstrap.servers", brokers, "request.timeout.ms", "3000", "default.api.timeout.ms", "3000"))) {
                admin.describeCluster().clusterId().get(4, TimeUnit.SECONDS);
            }
            checks.put("kafka", "UP");
            component = "opcUaAndEdgeX";
            var response = http.send(HttpRequest.newBuilder(URI.create(bridge + "/health"))
                    .timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new IllegalStateException(response.body());
            checks.put("opcUaAndEdgeX", json.readTree(response.body()));
            checks.put("status", "UP");
            return checks;
        } catch (Exception error) {
            String reason = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Services not ready: " + component + ": " + reason);
        }
    }

    public synchronized Map<String, Object> run(JsonNode request) throws Exception {
        var configuration = json.treeToValue(request, ProductionLine.Configuration.class);
        if (configuration == null) throw new IllegalArgumentException("Production configuration is required");
        readiness();
        String encoded = json.writeValueAsString(configuration);
        var existing = jdbc.queryForList("SELECT configuration::text,status FROM production_runs WHERE run_id=?", configuration.runId());
        if (!existing.isEmpty()) {
            if (!json.readTree(existing.get(0).get("configuration").toString()).equals(json.readTree(encoded))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Run identity is already bound to another configuration");
            }
            return runDetails(configuration.runId());
        }
        var observations = new ProductionLine().run(configuration);
        var mapping = new DiagnosticEventStream.Mapping("line-a", "mapping-v1", "format-v1");
        var events = DiagnosticEventStream.from(configuration, observations, mapping).events();
        long end = observations.isEmpty() ? 0 : observations.get(observations.size() - 1).timeMillis() + 1;
        jdbc.update("INSERT INTO production_runs(run_id,configuration,expected_events,end_millis,status) VALUES (?,?::jsonb,?,?,?)",
                configuration.runId(), encoded, events.size(), end, "RUNNING");
        try {
            var receipts = publishObservations(events);
            String topic = publishKafka(configuration.runId(), receipts);
            FlinkMetricsJob.execute(brokers, topic, database, user, password);
            int count = jdbc.queryForObject("SELECT count(*) FROM production_facts WHERE run_id=?", Integer.class, configuration.runId());
            if (count != events.size()) throw new IllegalStateException("Persisted event count differs from simulator output");
            jdbc.update("UPDATE production_runs SET status='COMPLETED' WHERE run_id=?", configuration.runId());
            return runDetails(configuration.runId());
        } catch (Exception failure) {
            LOG.error("Production run {} failed", configuration.runId(), failure);
            jdbc.update("UPDATE production_runs SET status='FAILED',error=? WHERE run_id=?", failure.getMessage(), configuration.runId());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Run failed: " + failure.getMessage(), failure);
        }
    }

    private JsonNode publishObservations(List<DiagnosticEventStream.Event> events) throws Exception {
        var response = http.send(HttpRequest.newBuilder(URI.create(bridge + "/publish"))
                .timeout(Duration.ofMinutes(4)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(events))).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IllegalStateException("OPC UA / EdgeX publication: " + response.body());
        return json.readTree(response.body());
    }

    private String publishKafka(String runId, JsonNode receipts) throws Exception {
        String topic = "iiot-run-" + java.util.UUID.nameUUIDFromBytes(runId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", brokers))) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get(10, TimeUnit.SECONDS);
        }
        try (var producer = new KafkaProducer<String, String>(Map.of("bootstrap.servers", brokers,
                "key.serializer", StringSerializer.class.getName(), "value.serializer", StringSerializer.class.getName(),
                "acks", "all", "delivery.timeout.ms", "15000", "request.timeout.ms", "5000"))) {
            for (var receipt : receipts) {
                producer.send(new ProducerRecord<>(topic, receipt.path("event").path("eventId").asText(), receipt.toString())).get(20, TimeUnit.SECONDS);
            }
        }
        return topic;
    }

    public Map<String, Object> runDetails(String runId) throws Exception {
        var rows = jdbc.queryForList("SELECT * FROM production_runs WHERE run_id=?", runId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found");
        var row = rows.get(0);
        var result = new LinkedHashMap<String, Object>();
        result.put("runId", runId);
        result.put("status", row.get("status"));
        result.put("configuration", json.readTree(row.get("configuration").toString()));
        result.put("expectedEvents", row.get("expected_events"));
        result.put("endMillis", row.get("end_millis"));
        result.put("error", row.get("error"));
        var configuration = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(row.get("configuration").toString());
        configuration.remove("runId");
        String configurationVersion = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(configuration.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        result.put("resultVersion", Map.of("input", row.get("input_version"), "mapping", row.get("mapping_version"),
                "format", row.get("format_version"), "rules", "not-used", "calculation", row.get("calculation_version"),
                "statistics", "half-open-v1", "configuration", configurationVersion));
        return result;
    }

    public List<JsonNode> events(String runId) throws Exception {
        runDetails(runId);
        var result = new ArrayList<JsonNode>();
        for (var row : jdbc.queryForList("SELECT evidence::text,state FROM production_facts WHERE run_id=? ORDER BY original_millis,event_id", runId)) {
            var receipt = json.readTree(row.get("evidence").toString());
            var event = (com.fasterxml.jackson.databind.node.ObjectNode) receipt.path("event").deepCopy();
            event.put("edgexEventId", receipt.path("edgexEventId").asText());
            event.put("protocol", receipt.path("protocol").asText());
            event.put("state", row.get("state").toString());
            result.add(event);
        }
        return result;
    }

    public Map<String, Object> metrics(String line, String runId, String batchId, long start, long end, long warmup) throws Exception {
        if (!line.equals("line-a")) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Line not found");
        if (start < 0 || end < start || warmup < 0 || warmup > end) {
            throw new IllegalArgumentException("Ordered nonnegative statistics interval and warmup required");
        }
        start = Math.max(start, warmup);
        var run = runDetails(runId);
        if (batchId != null) {
            boolean knownBatch = false;
            for (var batch : ((JsonNode) run.get("configuration")).path("batches")) {
                if (batch.path("batchId").asText().equals(batchId)) knownBatch = true;
            }
            if (!knownBatch) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Production batch not found");
        }
        if (!run.get("status").equals("COMPLETED")) throw new ResponseStatusException(HttpStatus.CONFLICT, "Run is not complete");
        if (end > ((Number) run.get("endMillis")).longValue()) throw new IllegalArgumentException("Interval exceeds observed run");
        // WIP includes warmup entries; only outlet totals use the statistics start.
        var values = jdbc.queryForMap("""
                SELECT coalesce(sum(good_delta) FILTER (WHERE original_millis>=?),0) AS good,
                coalesce(sum(rejected_delta) FILTER (WHERE original_millis>=?),0) AS rejected,
                coalesce(sum(wip_delta),0) AS wip FROM production_facts
                WHERE run_id=? AND original_millis<? AND (?::text IS NULL OR batch_id=?)
                """, start, start, runId, end, batchId, batchId);
        long good = ((Number) values.get("good")).longValue();
        var result = new LinkedHashMap<String, Object>();
        result.put("lineId", line);
        result.put("runId", runId);
        result.put("batchId", batchId);
        result.put("intervalStartMillis", start);
        result.put("intervalEndMillis", end);
        result.put("warmupMillis", warmup);
        result.put("goodUnits", good);
        result.put("rejectedUnits", values.get("rejected"));
        result.put("endingWorkInProcess", values.get("wip"));
        result.put("goodUnitsPerHour", end == start ? null : good * 3_600_000.0 / (end - start));
        result.put("dataCompleteness", "COMPLETE");
        result.put("timeConfidence", "SIMULATED_DEVICE_CLOCK");
        result.put("scope", "INCLUDING_CHANGEOVER");
        result.put("resultVersion", run.get("resultVersion"));
        return result;
    }
}
