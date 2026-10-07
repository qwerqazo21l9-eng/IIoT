package io.iiot.production;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.iiot.simulator.ProductionLine;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ProductionApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RunValidationIT {
    @LocalServerPort int port;

    @Test
    void nullRunRequestIsAParameterError() throws Exception {
        var response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/runs"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("null")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode(), response.body());
    }

    @Test
    void invalidConfigurationIsAParameterErrorAndDoesNotCreateARun() throws Exception {
        String runId = "invalid-configuration-" + UUID.randomUUID();
        ObjectNode configuration = new ObjectMapper().valueToTree(new ProductionLine.Configuration(runId, 42, 1));
        configuration.put("workpieces", -1);
        var http = HttpClient.newHttpClient();
        String api = "http://127.0.0.1:" + port;
        var response = http.send(HttpRequest.newBuilder(URI.create(api + "/api/runs"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(configuration.toString())).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode(), response.body());
        var run = http.send(HttpRequest.newBuilder(URI.create(api + "/api/runs/" + runId)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, run.statusCode(), run.body());
    }
}
