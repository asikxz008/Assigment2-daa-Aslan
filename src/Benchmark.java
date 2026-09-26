import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;

public final class Benchmark {
    private static final int[] SIZES = {100, 1_000, 10_000, 100_000};
    private static final int RUNS = 5;
    private static final int ACCESS_OPERATIONS = 10_000;
    private static final int SEARCH_OPERATIONS = 1_000;
    private static final int EDIT_OPERATIONS = 1_000;
    private static volatile long sink;

    private interface Builder {
        IntList create();
    }

    private static final class Measurement {
        private final long nanoseconds;
        private final long count;

        private Measurement(long nanoseconds, long count) {
            this.nanoseconds = nanoseconds;
            this.count = count;
        }
    }

    private Benchmark() {
    }

    public static void main(String[] args) throws IOException {
        Path tables = Path.of("results", "tables");
        Files.createDirectories(tables);
        StringBuilder access = header();
        StringBuilder search = header();
        StringBuilder edits = header();
        StringBuilder heap = header();
        Builder[] builders = {DynamicArray::new, LinkedList::new};
        String[] names = {"DynamicArray", "LinkedList"};

        for (int n : SIZES) {
            Random random = new Random(42);
            int[] data = new int[n];
            for (int i = 0; i < n; i++) {
                data[i] = random.nextInt(2 * n);
            }
            int[] indices = new int[ACCESS_OPERATIONS];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = random.nextInt(n);
            }
            int[] queries = new int[SEARCH_OPERATIONS];
            for (int i = 0; i < queries.length; i++) {
                queries[i] = (i & 1) == 0 ? data[random.nextInt(n)] : -1 - random.nextInt(n);
            }

            for (int i = 0; i < builders.length; i++) {
                Builder builder = builders[i];
                String name = names[i];
                row(access, n, name, "get", randomAccess(builder, data, indices), "accesses",
                        i == 0 ? "Theta(m)" : "Theta(m*n)");
                row(search, n, name, "contains", search(builder, data, queries), "comparisons", "Theta(m*n)");
                int middle = n / 2;
                row(edits, n, name, "insert_begin", insert(builder, data, 0, i == 0),
                        i == 0 ? "movements" : "accesses", i == 0 ? "Theta(m*n+m^2)" : "Theta(m)");
                row(edits, n, name, "remove_begin", remove(builder, data, 0, i == 0),
                        i == 0 ? "movements" : "accesses", i == 0 ? "Theta(m*n)" : "Theta(m)");
                row(edits, n, name, "insert_middle", insert(builder, data, middle, i == 0),
                        i == 0 ? "movements" : "accesses", i == 0 ? "Theta(m*n+m^2)" : "Theta(m*n)");
                row(edits, n, name, "remove_middle", remove(builder, data, middle, i == 0),
                        i == 0 ? "movements" : "accesses", "Theta(m*n)");
            }

            Measurement[] priority = priority(data);
            row(heap, n, "MinHeap", "insert", priority[0], "comparisons", "O(n*log(n))");
            row(heap, n, "MinHeap", "extractMin", priority[1], "comparisons", "O(n*log(n))");
        }

        Files.writeString(tables.resolve("random_access.csv"), access, StandardCharsets.UTF_8);
        Files.writeString(tables.resolve("search.csv"), search, StandardCharsets.UTF_8);
        Files.writeString(tables.resolve("insertion_removal.csv"), edits, StandardCharsets.UTF_8);
        Files.writeString(tables.resolve("priority_processing.csv"), heap, StandardCharsets.UTF_8);
        System.out.println("Benchmark results saved in results/tables");
    }

    private static StringBuilder header() {
        return new StringBuilder("n,structure,operation,mean_time_ms,metric,mean_count,theory\n");
    }

    private static void row(StringBuilder output, int n, String structure, String operation,
                            Measurement measurement, String metric, String theory) {
        output.append(String.format(Locale.US, "%d,%s,%s,%.6f,%s,%.1f,%s%n", n, structure, operation,
                measurement.nanoseconds / (RUNS * 1_000_000.0), metric,
                measurement.count / (double) RUNS, theory));
    }

    private static IntList filled(Builder builder, int[] data) {
        IntList list = builder.create();
        for (int value : data) {
            list.add(value);
        }
        list.resetMetrics();
        return list;
    }

    private static Measurement randomAccess(Builder builder, int[] data, int[] indices) {
        long time = 0;
        long accesses = 0;
        long checksum = 0;
        for (int run = 0; run < RUNS; run++) {
            IntList list = filled(builder, data);
            long start = System.nanoTime();
            for (int index : indices) {
                checksum += list.get(index);
            }
            time += System.nanoTime() - start;
            accesses += list.getAccessCount();
        }
        sink = checksum;
        return new Measurement(time, accesses);
    }

    private static Measurement search(Builder builder, int[] data, int[] queries) {
        long time = 0;
        long comparisons = 0;
        long checksum = 0;
        for (int run = 0; run < RUNS; run++) {
            IntList list = filled(builder, data);
            long start = System.nanoTime();
            for (int value : queries) {
                if (list.contains(value)) {
                    checksum++;
                }
            }
            time += System.nanoTime() - start;
            comparisons += list.getComparisonCount();
        }
        sink = checksum;
        return new Measurement(time, comparisons);
    }

    private static Measurement insert(Builder builder, int[] data, int index, boolean array) {
        long time = 0;
        long count = 0;
        for (int run = 0; run < RUNS; run++) {
            IntList list = filled(builder, data);
            long start = System.nanoTime();
            for (int i = 0; i < EDIT_OPERATIONS; i++) {
                list.add(index, -1);
            }
            time += System.nanoTime() - start;
            count += array ? list.getMovementCount() : list.getAccessCount();
            sink = list.size();
        }
        return new Measurement(time, count);
    }

    private static Measurement remove(Builder builder, int[] data, int index, boolean array) {
        long time = 0;
        long count = 0;
        long checksum = 0;
        for (int run = 0; run < RUNS; run++) {
            int remaining = EDIT_OPERATIONS;
            while (remaining > 0) {
                IntList list = filled(builder, data);
                int batch = Math.min(remaining, data.length - index);
                long start = System.nanoTime();
                for (int i = 0; i < batch; i++) {
                    checksum += list.remove(index);
                }
                time += System.nanoTime() - start;
                count += array ? list.getMovementCount() : list.getAccessCount();
                remaining -= batch;
            }
        }
        sink = checksum;
        return new Measurement(time, count);
    }

    private static Measurement[] priority(int[] data) {
        long insertTime = 0;
        long extractTime = 0;
        long insertComparisons = 0;
        long extractComparisons = 0;
        for (int run = 0; run < RUNS; run++) {
            MinHeap heap = new MinHeap();
            int[] extracted = new int[data.length];
            heap.resetMetrics();
            long start = System.nanoTime();
            for (int value : data) {
                heap.insert(value);
            }
            insertTime += System.nanoTime() - start;
            insertComparisons += heap.getComparisonCount();
            heap.resetMetrics();
            start = System.nanoTime();
            for (int i = 0; i < data.length; i++) {
                extracted[i] = heap.extractMin();
            }
            extractTime += System.nanoTime() - start;
            extractComparisons += heap.getComparisonCount();
            for (int i = 1; i < extracted.length; i++) {
                if (extracted[i] < extracted[i - 1]) {
                    throw new AssertionError("Heap extraction order is invalid");
                }
            }
            sink = extracted[extracted.length - 1];
        }
        return new Measurement[] {
                new Measurement(insertTime, insertComparisons),
                new Measurement(extractTime, extractComparisons)
        };
    }
}
