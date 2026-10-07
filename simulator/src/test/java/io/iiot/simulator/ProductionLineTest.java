package io.iiot.simulator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ProductionLineTest {
    @Test
    void eachDeviceHasItsOwnContiguousSequenceAndEveryEventCarriesRunIdentity() {
        var configuration = new ProductionLine.Configuration("identity", 42, 12);
        var stream = DiagnosticEventStream.from(configuration, new ProductionLine().run(configuration),
                new DiagnosticEventStream.Mapping("line-a", "mapping-v1", "format-v1"));
        for (var device : stream.events().stream().map(DiagnosticEventStream.Event::deviceId).distinct().toList()) {
            var events = stream.events().stream().filter(e -> e.deviceId().equals(device)).toList();
            assertEquals(java.util.stream.LongStream.rangeClosed(1, events.size()).boxed().toList(),
                    events.stream().map(DiagnosticEventStream.Event::deviceSequence).toList());
        }
        assertTrue(stream.events().stream().allMatch(e -> e.runId().equals(configuration.runId())));
    }

    @Test
    void observationGapSuppressesOnlySelectedStationAndPreservesSurvivingEventIdentity() {
        var configuration = new ProductionLine.Configuration("gap", 42, 12);
        var observations = new ProductionLine().run(configuration);
        var mapping = new DiagnosticEventStream.Mapping("line-a", "mapping-v1", "format-v1");
        var complete = DiagnosticEventStream.from(configuration, observations, mapping);
        long start = observations.stream().filter(e -> e.station() == 2).findFirst().orElseThrow().timeMillis();
        long end = observations.stream().filter(e -> e.station() == 2 && e.timeMillis() > start)
                .findFirst().orElseThrow().timeMillis();
        var gaps = java.util.List.of(new DiagnosticEventStream.ObservationGap(2, start, end));
        var missing = DiagnosticEventStream.from(configuration, observations, mapping, gaps);
        assertTrue(missing.events().size() < complete.events().size());
        assertEquals(complete.events().stream().filter(e -> !(e.station() == 2
                        && e.originalTimeMillis() >= start && e.originalTimeMillis() < end)).toList(), missing.events());
        assertTrue(missing.events().stream().anyMatch(e -> e.station() == 2 && e.originalTimeMillis() == end));
        assertEquals(missing, DiagnosticEventStream.from(configuration, observations, mapping, gaps));
        assertThrows(IllegalArgumentException.class, () -> new DiagnosticEventStream.ObservationGap(5, start, end));
        assertThrows(IllegalArgumentException.class, () -> new DiagnosticEventStream.ObservationGap(2, -1, end));
        assertThrows(IllegalArgumentException.class, () -> new DiagnosticEventStream.ObservationGap(2, end, start));
    }

    @Test
    void pauseForUnknownWorkpieceIsRejectedBeforeRunning() {
        var configuration = new ProductionLine.Configuration("pause", 42, 12);
        assertThrows(IllegalArgumentException.class, () -> new ProductionLine().run(configuration,
                java.util.List.of(new ProductionLine.Pause(2, "other-run:wp:2", 2000))));
        assertThrows(IllegalArgumentException.class, () -> new ProductionLine().run(configuration,
                java.util.List.of(new ProductionLine.Pause(2, "pause:wp:13", 2000))));
        assertThrows(IllegalArgumentException.class, () -> new ProductionLine().run(configuration,
                java.util.List.of(new ProductionLine.Pause(2, "pause:wp:0", 2000))));
        assertThrows(IllegalArgumentException.class, () -> new ProductionLine.Pause(5, "pause:wp:1", 2000));
        assertThrows(IllegalArgumentException.class, () -> new ProductionLine.Pause(2, "pause:wp:1", -1));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void stationPauseChangesOnlySelectedOperationDurationAndPreservesQuality(int station) {
        var configuration = new ProductionLine.Configuration("pause", 42, 12);
        var line = new ProductionLine();
        var normal = line.run(configuration);
        var pause = new ProductionLine.Pause(station, "pause:wp:2", 2000);
        var paused = line.run(configuration, java.util.List.of(pause));
        assertEquals(paused, line.run(configuration, java.util.List.of(pause)));
        assertEquals(normal.stream().map(ProductionLine.Observation::workpieceId).distinct().toList(),
                paused.stream().map(ProductionLine.Observation::workpieceId).distinct().toList());
        for (int piece = 1; piece <= 12; piece++) {
            String id = "pause:wp:" + piece;
            for (int selectedStation = 1; selectedStation <= 4; selectedStation++) {
                final int selected = selectedStation;
                var normalOperation = normal.stream().filter(e -> e.workpieceId().equals(id)
                        && e.station() == selected && (e.type() == ProductionLine.Type.OPERATION_STARTED
                        || e.type() == ProductionLine.Type.OPERATION_FINISHED)).toList();
                var pausedOperation = paused.stream().filter(e -> e.workpieceId().equals(id)
                        && e.station() == selected && (e.type() == ProductionLine.Type.OPERATION_STARTED
                        || e.type() == ProductionLine.Type.OPERATION_FINISHED)).toList();
                long normalDuration = normalOperation.get(1).timeMillis() - normalOperation.get(0).timeMillis();
                long pausedDuration = pausedOperation.get(1).timeMillis() - pausedOperation.get(0).timeMillis();
                assertEquals(normalDuration + (piece == 2 && selected == station ? 2000 : 0), pausedDuration);
            }
            assertEquals(normal.stream().filter(e -> e.workpieceId().equals(id)
                            && (e.type() == ProductionLine.Type.GOOD_UNIT || e.type() == ProductionLine.Type.REJECTED_UNIT))
                            .map(ProductionLine.Observation::type).toList(),
                    paused.stream().filter(e -> e.workpieceId().equals(id)
                            && (e.type() == ProductionLine.Type.GOOD_UNIT || e.type() == ProductionLine.Type.REJECTED_UNIT))
                            .map(ProductionLine.Observation::type).toList());
        }
        assertTrue(paused.stream().filter(e -> e.bufferOccupancy() != null)
                .allMatch(e -> e.bufferOccupancy() >= 0 && e.bufferOccupancy() <= configuration.bufferCapacity()));
    }

    @Test
    void sameConfigurationAndSeedReproduceNonEmptyObservations() {
        var configuration = new ProductionLine.Configuration("normal-1", 42, 12);
        var first = new ProductionLine().run(configuration);
        assertFalse(first.isEmpty());
        assertEquals(first, new ProductionLine().run(configuration));
    }

    @Test
    void everyWorkpieceCompletesFourStationsInProductionOrder() {
        var events = new ProductionLine().run(new ProductionLine.Configuration("normal-1", 42, 12));
        for (int piece = 1; piece <= 12; piece++) {
            String id = "normal-1:wp:" + piece;
            var route = events.stream().filter(e -> e.workpieceId().equals(id))
                    .filter(e -> e.type() == ProductionLine.Type.OPERATION_STARTED
                            || e.type() == ProductionLine.Type.OPERATION_FINISHED).toList();
            assertEquals(java.util.List.of(1, 1, 2, 2, 3, 3, 4, 4),
                    route.stream().map(ProductionLine.Observation::station).toList());
            assertEquals(java.util.List.of(ProductionLine.Type.OPERATION_STARTED,
                    ProductionLine.Type.OPERATION_FINISHED),
                    route.stream().map(ProductionLine.Observation::type).distinct().toList());
            for (int index = 1; index < route.size(); index++) {
                assertTrue(route.get(index).timeMillis() >= route.get(index - 1).timeMillis());
            }
        }
    }

    @Test
    void slowDownstreamBlocksUpstreamWithoutOverflowingFiniteBuffers() {
        var events = new ProductionLine().run(new ProductionLine.Configuration("slow", 42, 12,
                java.util.List.of(100L, 100L, 5000L, 100L), 1));
        var buffers = events.stream().filter(e -> e.type() == ProductionLine.Type.BUFFER_CHANGED).toList();
        assertFalse(buffers.isEmpty());
        assertTrue(buffers.stream().allMatch(e -> e.bufferOccupancy() >= 0 && e.bufferOccupancy() <= 1));
        assertTrue(events.stream().anyMatch(e -> e.type() == ProductionLine.Type.BLOCKED));
        long secondStart = events.stream().filter(e -> e.station() == 1 && e.workpieceId().equals("slow:wp:2")
                && e.type() == ProductionLine.Type.OPERATION_STARTED).findFirst().orElseThrow().timeMillis();
        long firstOutlet = events.stream().filter(e -> e.station() == 4 && e.workpieceId().equals("slow:wp:1")
                && e.type() == ProductionLine.Type.OPERATION_FINISHED).findFirst().orElseThrow().timeMillis();
        assertTrue(secondStart < firstOutlet, "The line must run concurrently, not one workpiece at a time");
    }

    @Test
    void productBatchesHaveExplicitChangeoverAndOneFinalQualityDecisionPerWorkpiece() {
        var configuration = new ProductionLine.Configuration("batches", 42, 100,
                java.util.List.of(100L, 200L, 100L, 100L), 2,
                java.util.List.of(new ProductionLine.Batch("order-a", "A", 50),
                        new ProductionLine.Batch("order-b", "B", 50)), 700, 0.1);
        var events = new ProductionLine().run(configuration);
        assertEquals(java.util.Set.of("A", "B"), events.stream()
                .map(ProductionLine.Observation::productType).collect(java.util.stream.Collectors.toSet()));
        for (int station = 1; station <= 4; station++) {
            final int selected = station;
            var changeover = events.stream().filter(e -> e.station() == selected &&
                    (e.type() == ProductionLine.Type.CHANGEOVER_STARTED ||
                     e.type() == ProductionLine.Type.CHANGEOVER_FINISHED)).toList();
            assertEquals(2, changeover.size());
            assertEquals(700, changeover.get(1).timeMillis() - changeover.get(0).timeMillis());
        }
        var quality = events.stream().filter(e -> e.type() == ProductionLine.Type.GOOD_UNIT ||
                e.type() == ProductionLine.Type.REJECTED_UNIT).toList();
        assertEquals(100, quality.size());
        assertEquals(100, quality.stream().map(ProductionLine.Observation::workpieceId).distinct().count());
        assertTrue(quality.stream().allMatch(e -> e.station() == 4));
        assertTrue(quality.stream().anyMatch(e -> e.type() == ProductionLine.Type.REJECTED_UNIT));
        assertTrue(quality.stream().anyMatch(e -> e.type() == ProductionLine.Type.GOOD_UNIT));
    }

    @Test
    void invalidRunConfigurationsAreRejectedBeforeSimulation() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProductionLine.Configuration("", 42, 12));
        assertThrows(IllegalArgumentException.class,
                () -> new ProductionLine.Configuration("invalid", 42, -1));
        assertThrows(IllegalArgumentException.class, () -> new ProductionLine.Configuration("invalid", 42, 12,
                java.util.List.of(100L, 100L, 100L), 1));
        assertThrows(IllegalArgumentException.class, () -> new ProductionLine.Configuration("invalid", 42, 12,
                java.util.List.of(100L, 0L, 100L, 100L), 1));
        assertThrows(IllegalArgumentException.class, () -> new ProductionLine.Configuration("invalid", 42, 12,
                java.util.List.of(100L, 100L, 100L, 100L), 0));
        assertThrows(IllegalArgumentException.class, () -> new ProductionLine.Configuration("invalid", 42, 12,
                java.util.List.of(100L, 100L, 100L, 100L), 1,
                java.util.List.of(new ProductionLine.Batch("a", "A", 11)), 100, 0.1));
        assertThrows(IllegalArgumentException.class, () -> new ProductionLine.Configuration("invalid", 42, 12,
                java.util.List.of(100L, 100L, 100L, 100L), 1,
                java.util.List.of(new ProductionLine.Batch("a", "A", 12)), -1, Double.NaN));
    }

    @Test
    void diagnosticEventStreamKeepsVersionedObservableFactsWithoutHiddenTruth() {
        var configuration = new ProductionLine.Configuration("normal-1", 42, 12);
        var stream = DiagnosticEventStream.from(configuration, new ProductionLine().run(configuration),
                new DiagnosticEventStream.Mapping("line-a", "mapping-v1", "format-v1"));

        assertFalse(stream.events().isEmpty());
        assertEquals("mapping-v1", stream.mappingConfigurationVersion());
        assertTrue(stream.events().stream().allMatch(e -> e.lineId().equals("line-a")));
        assertEquals(stream.events().size(),
                stream.events().stream().map(DiagnosticEventStream.Event::eventId).distinct().count());
        assertTrue(stream.events().stream().allMatch(e -> e.eventId() != null
                && e.originalTimeMillis() >= 0
                && e.receivedTimeMillis() >= e.originalTimeMillis()
                && e.quality() == DiagnosticEventStream.Quality.GOOD
                && e.mappingConfigurationVersion().equals("mapping-v1")
                && e.formatVersion().equals("format-v1")));
        assertTrue(stream.events().stream().noneMatch(e -> e.attributes().containsKey("faultTruth")
                || e.attributes().containsKey("faultStation")
                || e.attributes().containsKey("futureEvent")));
    }

    @Test
    void lineMetricsUseOutletQualityOnlyWithWarmupHalfOpenIntervalAndNoJphForZeroDuration() {
        var configuration = new ProductionLine.Configuration("metrics-1", 42, 12);
        var observations = new ProductionLine().run(configuration);
        var stream = DiagnosticEventStream.from(configuration, observations,
                new DiagnosticEventStream.Mapping("line-a", "mapping-v1", "format-v1"));

        long firstGoodTime = observations.stream()
                .filter(e -> e.type() == ProductionLine.Type.GOOD_UNIT)
                .findFirst().orElseThrow().timeMillis();
        long lastEventTime = observations.get(observations.size() - 1).timeMillis() + 1;

        var metrics = LineMetricsQuery.from(stream).query(0, firstGoodTime + 1, lastEventTime);
        long independentlyCountedGoodUnits = observations.stream()
                .filter(e -> e.type() == ProductionLine.Type.GOOD_UNIT)
                .filter(e -> e.timeMillis() >= firstGoodTime + 1 && e.timeMillis() < lastEventTime)
                .count();
        long independentlyCountedRejectedUnits = observations.stream()
                .filter(e -> e.type() == ProductionLine.Type.REJECTED_UNIT)
                .filter(e -> e.timeMillis() >= firstGoodTime + 1 && e.timeMillis() < lastEventTime)
                .count();

        assertEquals(independentlyCountedGoodUnits, metrics.goodUnits());
        assertEquals(independentlyCountedRejectedUnits, metrics.rejectedUnits());
        assertTrue(metrics.goodUnitsPerHour().isPresent());
        assertEquals(metrics.goodUnits() * 3_600_000.0 / (lastEventTime - firstGoodTime - 1),
                metrics.goodUnitsPerHour().orElseThrow(), 0.000_001);
        assertEquals("line-a", metrics.lineId());
        assertEquals("mapping-v1", metrics.resultVersion().mappingConfigurationVersion());
        assertTrue(metrics.endingWorkInProcess() >= 0);

        var zeroDuration = LineMetricsQuery.from(stream).query(0, 1000, 1000);
        assertTrue(zeroDuration.goodUnitsPerHour().isEmpty());
    }
}
