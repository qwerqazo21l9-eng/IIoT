package io.iiot.production;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.configuration.Configuration;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.TimeUnit;

public final class FlinkMetricsJob {
    public static void execute(String brokers, String topic, String database, String user, String password) throws Exception {
        var source = KafkaSource.<String>builder().setBootstrapServers(brokers).setTopics(topic)
                .setGroupId(topic + "-metrics").setStartingOffsets(OffsetsInitializer.earliest())
                .setBounded(OffsetsInitializer.latest()).setValueOnlyDeserializer(new SimpleStringSchema()).build();
        var environment = StreamExecutionEnvironment.getExecutionEnvironment();
        environment.setParallelism(1);
        environment.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka observable events")
                .map(new Contribution()).name("Outlet quality and line entry contributions")
                .addSink(new Persist(database, user, password)).name("PostgreSQL versioned evidence and metrics");
        var job = environment.executeAsync("Normal production outlet metrics");
        try {
            job.getJobExecutionResult().get(90, TimeUnit.SECONDS);
        } catch (Exception failure) {
            job.cancel().get(10, TimeUnit.SECONDS);
            throw failure;
        }
    }

    public static class Contribution implements MapFunction<String, String> {
        @Override
        public String map(String input) throws Exception {
            var json = new ObjectMapper();
            var receipt = json.readTree(input);
            var event = receipt.path("event");
            var result = json.createObjectNode();
            result.set("evidence", receipt);
            String type = event.path("type").asText();
            int station = event.path("station").asInt();
            boolean outlet = station == 4;
            int good = outlet && type.equals("GOOD_UNIT") ? 1 : 0;
            int rejected = outlet && type.equals("REJECTED_UNIT") ? 1 : 0;
            int entered = station == 1 && type.equals("OPERATION_STARTED") ? 1 : 0;
            result.put("good", good).put("rejected", rejected).put("wip", entered - good - rejected);
            result.put("state", switch (type) {
                case "OPERATION_STARTED" -> "PROCESSING";
                case "BLOCKED" -> "BLOCKED";
                case "CHANGEOVER_STARTED" -> "CHANGEOVER";
                case "BUFFER_CHANGED" -> "BUFFER_CHANGED";
                default -> "IDLE";
            });
            return json.writeValueAsString(result);
        }
    }

    public static class Persist extends RichSinkFunction<String> {
        private final String database;
        private final String user;
        private final String password;
        private transient Connection connection;

        public Persist(String database, String user, String password) {
            this.database = database;
            this.user = user;
            this.password = password;
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            connection = DriverManager.getConnection(database, user, password);
        }

        @Override
        public void invoke(String value, Context context) throws Exception {
            var json = new ObjectMapper();
            var contribution = json.readTree(value);
            var evidence = contribution.path("evidence");
            var event = evidence.path("event");
            try (var statement = connection.prepareStatement("""
                    INSERT INTO production_facts(event_id,run_id,batch_id,station,original_millis,
                    good_delta,rejected_delta,wip_delta,state,evidence) VALUES (?,?,?,?,?,?,?,?,?,?::jsonb)
                    ON CONFLICT(event_id) DO NOTHING
                    """)) {
                statement.setString(1, event.path("eventId").asText());
                statement.setString(2, event.path("runId").asText());
                statement.setString(3, event.path("batchId").asText());
                statement.setInt(4, event.path("station").asInt());
                statement.setLong(5, event.path("originalTimeMillis").asLong());
                statement.setInt(6, contribution.path("good").asInt());
                statement.setInt(7, contribution.path("rejected").asInt());
                statement.setInt(8, contribution.path("wip").asInt());
                statement.setString(9, contribution.path("state").asText());
                statement.setString(10, json.writeValueAsString(evidence));
                statement.executeUpdate();
            }
        }

        @Override
        public void close() throws Exception {
            if (connection != null) connection.close();
        }
    }
}
