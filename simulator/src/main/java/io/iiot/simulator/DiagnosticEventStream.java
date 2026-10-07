package io.iiot.simulator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DiagnosticEventStream(String runId, String lineId, String mappingConfigurationVersion,
                                    String formatVersion, List<Event> events) {
    public DiagnosticEventStream {
        if (runId == null || runId.isBlank() || lineId == null || lineId.isBlank()
                || mappingConfigurationVersion == null || mappingConfigurationVersion.isBlank()
                || formatVersion == null || formatVersion.isBlank() || events == null) {
            throw new IllegalArgumentException("Diagnostic event stream requires run and version identity");
        }
        events = List.copyOf(events);
    }

    public static DiagnosticEventStream from(ProductionLine.Configuration configuration,
                                             List<ProductionLine.Observation> observations,
                                             Mapping mapping) {
        if (configuration == null || observations == null || mapping == null) {
            throw new IllegalArgumentException("Configuration, observations and mapping are required");
        }
        var events = new java.util.ArrayList<Event>();
        var sequences = new java.util.HashMap<Integer, Long>();
        for (var observation : observations) {
            long sequence = sequences.merge(observation.station(), 1L, Long::sum);
            events.add(Event.from(configuration.runId(), observation, mapping, sequence));
        }
        return new DiagnosticEventStream(configuration.runId(), mapping.lineId(),
                mapping.mappingConfigurationVersion(), mapping.formatVersion(), events);
    }

    public record Mapping(String lineId, String mappingConfigurationVersion, String formatVersion) {
        public Mapping {
            if (lineId == null || lineId.isBlank()
                    || mappingConfigurationVersion == null || mappingConfigurationVersion.isBlank()
                    || formatVersion == null || formatVersion.isBlank()) {
                throw new IllegalArgumentException("Mapping requires line and version identity");
            }
        }
    }

    public record ObservationGap(int station, long startMillis, long endMillis) {
        public ObservationGap {
            if (station < 1 || station > 4 || startMillis < 0 || endMillis < startMillis) {
                throw new IllegalArgumentException("Observation gap requires a station and ordered nonnegative interval");
            }
        }
    }

    public static DiagnosticEventStream from(ProductionLine.Configuration configuration,
                                             List<ProductionLine.Observation> observations,
                                             Mapping mapping, List<ObservationGap> gaps) {
        var selectedGaps = List.copyOf(gaps);
        var complete = from(configuration, observations, mapping);
        var visible = complete.events().stream().filter(event -> selectedGaps.stream().noneMatch(gap ->
                event.station() == gap.station() && event.originalTimeMillis() >= gap.startMillis()
                        && event.originalTimeMillis() < gap.endMillis())).toList();
        return new DiagnosticEventStream(complete.runId(), complete.lineId(), complete.mappingConfigurationVersion(),
                complete.formatVersion(), visible);
    }

    public enum Quality { GOOD, BAD }

    public enum EventType {
        OPERATION_STARTED, OPERATION_FINISHED, BUFFER_CHANGED, BLOCKED,
        CHANGEOVER_STARTED, CHANGEOVER_FINISHED, GOOD_UNIT, REJECTED_UNIT
    }

    public record Event(String eventId, String runId, String lineId, int station, String deviceId,
                        String workpieceId, String batchId, String productType,
                        EventType type, long deviceSequence, long originalTimeMillis,
                        long receivedTimeMillis, Quality quality,
                        String mappingConfigurationVersion, String formatVersion,
                        Integer bufferOccupancy, Map<String, String> attributes) {
        public Event {
            if (eventId == null || eventId.isBlank() || runId == null || runId.isBlank() || lineId == null || lineId.isBlank()
                    || station < 1 || station > 4 || deviceId == null || deviceId.isBlank()
                    || workpieceId == null || workpieceId.isBlank() || batchId == null || batchId.isBlank()
                    || productType == null || productType.isBlank() || type == null || deviceSequence < 1
                    || originalTimeMillis < 0 || receivedTimeMillis < originalTimeMillis || quality == null
                    || mappingConfigurationVersion == null || mappingConfigurationVersion.isBlank()
                    || formatVersion == null || formatVersion.isBlank()) {
                throw new IllegalArgumentException("Diagnostic event contains invalid observable facts");
            }
            attributes = Map.copyOf(attributes == null ? Map.of() : attributes);
        }

        static Event from(String runId, ProductionLine.Observation observation, Mapping mapping, long sequence) {
            var attributes = new LinkedHashMap<String, String>();
            attributes.put("sourceEventId", observation.eventId());
            return new Event(observation.eventId(), runId, mapping.lineId(), observation.station(),
                    mapping.lineId() + ":station-" + observation.station(),
                    observation.workpieceId(), observation.batchId(), observation.productType(),
                    EventType.valueOf(observation.type().name()), sequence, observation.timeMillis(),
                    observation.timeMillis(), Quality.GOOD, mapping.mappingConfigurationVersion(),
                    mapping.formatVersion(), observation.bufferOccupancy(), attributes);
        }
    }
}
