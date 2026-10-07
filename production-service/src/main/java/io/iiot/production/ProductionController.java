package io.iiot.production;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api")
public class ProductionController {
    private final ProductionService service;
    public ProductionController(ProductionService service) { this.service = service; }

    @GetMapping("/ready") public Map<String, Object> ready() { return service.readiness(); }
    @PostMapping("/runs") public Map<String, Object> run(@RequestBody JsonNode request) throws Exception {
        return service.run(request);
    }
    @GetMapping("/runs/{runId}") public Map<String, Object> run(@PathVariable("runId") String runId) throws Exception {
        return service.runDetails(runId);
    }
    @GetMapping("/runs/{runId}/events") public List<JsonNode> events(@PathVariable("runId") String runId) throws Exception {
        return service.events(runId);
    }
    @GetMapping("/lines/{lineId}/metrics") public Map<String, Object> metrics(@PathVariable("lineId") String lineId,
            @RequestParam("runId") String runId, @RequestParam(value = "batchId", required = false) String batchId,
            @RequestParam("startMillis") long startMillis, @RequestParam("endMillis") long endMillis,
            @RequestParam(value = "warmupMillis", defaultValue = "0") long warmupMillis) throws Exception {
        return service.metrics(lineId, runId, batchId, startMillis, endMillis, warmupMillis);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badInput(IllegalArgumentException error) { return Map.of("error", error.getMessage()); }
}
