package io.iiot.simulator;

import java.util.HashSet;
import java.util.OptionalDouble;

public final class LineMetricsQuery {
    private final DiagnosticEventStream stream;

    private LineMetricsQuery(DiagnosticEventStream stream) {
        this.stream = stream;
    }

    public static LineMetricsQuery from(DiagnosticEventStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("Diagnostic event stream is required");
        }
        return new LineMetricsQuery(stream);
    }

    public Metrics query(long warmupStartMillis, long intervalStartMillis, long intervalEndMillis) {
        if (warmupStartMillis < 0 || intervalStartMillis < warmupStartMillis || intervalEndMillis < intervalStartMillis) {
            throw new IllegalArgumentException("Metrics query requires an ordered nonnegative time range");
        }
        long good = stream.events().stream()
                .filter(e -> e.type() == DiagnosticEventStream.EventType.GOOD_UNIT)
                .filter(e -> inside(e.originalTimeMillis(), intervalStartMillis, intervalEndMillis))
                .count();
        long rejected = stream.events().stream()
                .filter(e -> e.type() == DiagnosticEventStream.EventType.REJECTED_UNIT)
                .filter(e -> inside(e.originalTimeMillis(), intervalStartMillis, intervalEndMillis))
                .count();

        var entered = new HashSet<String>();
        var exited = new HashSet<String>();
        for (var event : stream.events()) {
            if (event.originalTimeMillis() >= intervalEndMillis) {
                continue;
            }
            if (event.type() == DiagnosticEventStream.EventType.OPERATION_STARTED) {
                entered.add(event.workpieceId());
            }
            if (event.type() == DiagnosticEventStream.EventType.GOOD_UNIT
                    || event.type() == DiagnosticEventStream.EventType.REJECTED_UNIT) {
                exited.add(event.workpieceId());
            }
        }
        int endingWorkInProcess = Math.max(0, entered.size() - exited.size());
        long duration = intervalEndMillis - intervalStartMillis;
        OptionalDouble jph = duration == 0
                ? OptionalDouble.empty()
                : OptionalDouble.of(good * 3_600_000.0 / duration);

        return new Metrics(stream.lineId(), intervalStartMillis, intervalEndMillis, good, rejected,
                endingWorkInProcess, jph, new ResultVersion(stream.mappingConfigurationVersion(), stream.formatVersion()));
    }

    private static boolean inside(long time, long start, long end) {
        return time >= start && time < end;
    }

    public record ResultVersion(String mappingConfigurationVersion, String formatVersion) {}

    public record Metrics(String lineId, long intervalStartMillis, long intervalEndMillis,
                          long goodUnits, long rejectedUnits, int endingWorkInProcess,
                          OptionalDouble goodUnitsPerHour, ResultVersion resultVersion) {
        public Metrics {
            if (lineId == null || lineId.isBlank() || intervalStartMillis < 0
                    || intervalEndMillis < intervalStartMillis || goodUnits < 0 || rejectedUnits < 0
                    || endingWorkInProcess < 0 || goodUnitsPerHour == null || resultVersion == null) {
                throw new IllegalArgumentException("Metrics require a valid range, counts and version");
            }
        }
    }
}
