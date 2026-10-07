package io.iiot.simulator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ProductionLine {
    public record Batch(String batchId, String productType, int units) {
        public Batch {
            if (batchId == null || batchId.isBlank() || productType == null || productType.isBlank() || units < 0) {
                throw new IllegalArgumentException("Batch requires identity, product and nonnegative unit count");
            }
        }
    }
    public record Configuration(String runId, long seed, int workpieces,
                                List<Long> cycleMillis, int bufferCapacity,
                                List<Batch> batches, long changeoverMillis, double rejectProbability) {
        public Configuration {
            if (runId == null || runId.isBlank() || workpieces < 0 || bufferCapacity < 1 || changeoverMillis < 0
                    || !Double.isFinite(rejectProbability) || rejectProbability < 0 || rejectProbability > 1) {
                throw new IllegalArgumentException("Invalid run identity, counts, timings or rejection probability");
            }
            if (cycleMillis == null || cycleMillis.size() != 4
                    || cycleMillis.stream().anyMatch(t -> t == null || t <= 0 || t > Long.MAX_VALUE / 8)) {
                throw new IllegalArgumentException("Four positive, bounded station cycle times are required");
            }
            if (batches == null || batches.stream().anyMatch(b -> b == null)
                    || batches.stream().mapToLong(Batch::units).sum() != workpieces
                    || batches.stream().map(Batch::batchId).distinct().count() != batches.size()) {
                throw new IllegalArgumentException("Unique batches must cover the production order exactly");
            }
            cycleMillis = List.copyOf(cycleMillis);
            batches = List.copyOf(batches);
        }
        public Configuration(String runId, long seed, int workpieces) {
            this(runId, seed, workpieces, List.of(1000L, 1100L, 1000L, 1000L), 1);
        }
        public Configuration(String runId, long seed, int workpieces, List<Long> cycleMillis, int bufferCapacity) {
            this(runId, seed, workpieces, cycleMillis, bufferCapacity,
                    List.of(new Batch("batch-a", "A", workpieces)), 500, 0.1);
        }
    }
    public enum Type { OPERATION_STARTED, OPERATION_FINISHED, BUFFER_CHANGED, BLOCKED,
                       CHANGEOVER_STARTED, CHANGEOVER_FINISHED, GOOD_UNIT, REJECTED_UNIT }
    public record Observation(String eventId, String workpieceId, long timeMillis,
                              int station, Type type, Integer bufferOccupancy,
                              String batchId, String productType) {}

    public record Pause(int station, String workpieceId, long durationMillis) {
        public Pause {
            if (station < 1 || station > 4 || workpieceId == null || workpieceId.isBlank() || durationMillis < 0) {
                throw new IllegalArgumentException("Pause requires a station, workpiece and nonnegative duration");
            }
        }
    }

    public List<Observation> run(Configuration configuration) {
        return run(configuration, List.of());
    }

    public List<Observation> run(Configuration configuration, List<Pause> pauses) {
        pauses = List.copyOf(pauses);
        if (!pauses.isEmpty()) {
            var workpieceIds = java.util.stream.IntStream.rangeClosed(1, configuration.workpieces())
                    .mapToObj(piece -> configuration.runId() + ":wp:" + piece)
                    .collect(java.util.stream.Collectors.toSet());
            if (pauses.stream().anyMatch(pause -> !workpieceIds.contains(pause.workpieceId()))) {
                throw new IllegalArgumentException("Pause workpiece must belong to this production order");
            }
        }
        var observations = new ArrayList<Observation>();
        var buffers = List.of(new ArrayDeque<Integer>(), new ArrayDeque<Integer>(), new ArrayDeque<Integer>());
        int[] pieces = new int[4];
        long[] finishes = new long[4];
        boolean[] finished = new boolean[4];
        boolean[] blocked = new boolean[4];
        boolean[] changing = new boolean[4];
        String[] products = new String[4];
        int nextPiece = 1;
        int completed = 0;
        long time = 0;
        while (completed < configuration.workpieces()) {
            boolean changed;
            do {
                changed = false;
                // Downstream first allows an upstream transfer at the same event time.
                for (int station = 3; station >= 0; station--) {
                    if (pieces[station] != 0 && !finished[station] && finishes[station] <= time) {
                        if (changing[station]) {
                            emit(observations, configuration, pieces[station], time, station, Type.CHANGEOVER_FINISHED, null);
                            changing[station] = false;
                            finishes[station] = startOperation(observations, configuration, pauses, pieces[station], time, station);
                        } else {
                            emit(observations, configuration, pieces[station], time, station, Type.OPERATION_FINISHED, null);
                            finished[station] = true;
                        }
                    }
                    if (pieces[station] != 0 && finished[station]) {
                        if (station == 3) {
                            emit(observations, configuration, pieces[station], time, station,
                                    quality(configuration, pieces[station]), null);
                            completed++;
                            pieces[station] = 0;
                            changed = true;
                        } else if (buffers.get(station).size() < configuration.bufferCapacity()) {
                            buffers.get(station).addLast(pieces[station]);
                            emit(observations, configuration, pieces[station], time, station, Type.BUFFER_CHANGED,
                                    buffers.get(station).size());
                            pieces[station] = 0;
                            changed = true;
                        } else if (!blocked[station]) {
                            emit(observations, configuration, pieces[station], time, station, Type.BLOCKED, null);
                            blocked[station] = true;
                        }
                    }
                    if (pieces[station] == 0) {
                        int piece = 0;
                        if (station == 0 && nextPiece <= configuration.workpieces()) {
                            piece = nextPiece++;
                        } else if (station > 0 && !buffers.get(station - 1).isEmpty()) {
                            piece = buffers.get(station - 1).removeFirst();
                            emit(observations, configuration, piece, time, station - 1, Type.BUFFER_CHANGED,
                                    buffers.get(station - 1).size());
                        }
                        if (piece != 0) {
                            pieces[station] = piece;
                            finished[station] = false;
                            blocked[station] = false;
                            String product = batch(configuration, piece).productType();
                            changing[station] = products[station] != null && !products[station].equals(product);
                            products[station] = product;
                            if (changing[station]) {
                                emit(observations, configuration, piece, time, station, Type.CHANGEOVER_STARTED, null);
                                finishes[station] = Math.addExact(time, configuration.changeoverMillis());
                            } else {
                                finishes[station] = startOperation(observations, configuration, pauses, piece, time, station);
                            }
                            changed = true;
                        }
                    }
                }
            } while (changed);
            long nextTime = Long.MAX_VALUE;
            for (int station = 0; station < 4; station++) {
                if (pieces[station] != 0 && !finished[station]) {
                    nextTime = Math.min(nextTime, finishes[station]);
                }
            }
            if (completed < configuration.workpieces() && nextTime == Long.MAX_VALUE) {
                throw new IllegalStateException("Line cannot make progress");
            }
            time = nextTime;
        }
        return List.copyOf(observations);
    }

    private static void emit(List<Observation> output, Configuration configuration, int piece,
                             long time, int station, Type type, Integer occupancy) {
        var batch = batch(configuration, piece);
        output.add(new Observation(configuration.runId() + ":event:" + (output.size() + 1),
                configuration.runId() + ":wp:" + piece, time, station + 1, type, occupancy,
                batch.batchId(), batch.productType()));
    }

    private static Batch batch(Configuration configuration, int piece) {
        int end = 0;
        for (var batch : configuration.batches()) {
            end += batch.units();
            if (piece <= end) return batch;
        }
        throw new IllegalArgumentException("Workpiece is outside the production order");
    }

    private static long duration(Configuration configuration, int piece, int station) {
        // Key normal timing by workpiece and station, independently of scheduling order.
        var random = new Random(configuration.seed() ^ (piece * 0xD1B54A32D192ED03L) ^ station);
        return configuration.cycleMillis().get(station) + random.nextInt(100);
    }

    private static long startOperation(List<Observation> output, Configuration configuration,
                                       List<Pause> pauses, int piece, long time, int station) {
        emit(output, configuration, piece, time, station, Type.OPERATION_STARTED, null);
        long operationDuration = duration(configuration, piece, station);
        for (var pause : pauses) {
            if (pause.station() == station + 1 && pause.workpieceId().equals(configuration.runId() + ":wp:" + piece)) {
                operationDuration = Math.addExact(operationDuration, pause.durationMillis());
            }
        }
        return Math.addExact(time, operationDuration);
    }

    private static Type quality(Configuration configuration, int piece) {
        var random = new Random(configuration.seed() ^ (piece * 0x9E3779B97F4A7C15L));
        return random.nextDouble() < configuration.rejectProbability() ? Type.REJECTED_UNIT : Type.GOOD_UNIT;
    }
}
