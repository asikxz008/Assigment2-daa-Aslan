import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;

public class Tests {
    public static void main(String[] args) throws ReflectiveOperationException {
        testBoundaries(new DynamicArray());
        testBoundaries(new LinkedList());
        testAgainstOracle(new DynamicArray(), new ArrayList<>());
        testAgainstOracle(new LinkedList(), new java.util.LinkedList<>());
        testLarge(new DynamicArray());
        testLarge(new LinkedList());
        testHeap();
        System.out.println("All tests passed");
    }

    private static void testBoundaries(IntList list) {
        check(list.size() == 0, "initial size");
        check(!list.contains(1), "empty search");
        expect(IndexOutOfBoundsException.class, () -> list.get(0));
        expect(IndexOutOfBoundsException.class, () -> list.remove(0));
        expect(IndexOutOfBoundsException.class, () -> list.add(-1, 1));
        expect(IndexOutOfBoundsException.class, () -> list.add(1, 1));

        list.add(3);
        check(list.get(0) == 3, "one element");
        list.add(0, 2);
        list.add(list.size(), 3);
        list.add(1, 2);
        check(list.size() == 4, "insertions");
        check(list.get(0) == 2 && list.get(1) == 2, "duplicate values");
        check(list.get(2) == 3 && list.get(3) == 3, "boundary insertions");
        check(list.contains(2) && list.contains(3) && !list.contains(4), "search");
        check(list.remove(0) == 2, "remove first");
        check(list.remove(1) == 3, "remove middle");
        check(list.remove(list.size() - 1) == 3, "remove last");
        check(list.remove(0) == 2 && list.size() == 0, "remove to empty");
        expect(IndexOutOfBoundsException.class, () -> list.get(-1));
        expect(IndexOutOfBoundsException.class, () -> list.remove(-1));
        expect(IndexOutOfBoundsException.class, () -> list.add(2, 1));

        for (int i = 0; i < 4; i++) {
            list.add(i);
        }
        list.resetMetrics();
        check(list.get(2) == 2, "measured get");
        check(list.getAccessCount() == (list instanceof DynamicArray ? 1 : 3), "get accesses");
        list.resetMetrics();
        check(!list.contains(9), "measured search");
        check(list.getComparisonCount() == 4, "search comparisons");
        list.resetMetrics();
        list.add(0, -1);
        check(list.getMovementCount() == (list instanceof DynamicArray ? 4 : 0), "insertion movements");
    }

    private static void testAgainstOracle(IntList actual, List<Integer> expected) {
        Random random = new Random(42);
        for (int i = 0; i < 5_000; i++) {
            int operation = expected.isEmpty() ? 0 : random.nextInt(5);
            int value = random.nextInt(101) - 50;
            if (operation == 0) {
                actual.add(value);
                expected.add(value);
            } else if (operation == 1) {
                int index = random.nextInt(expected.size() + 1);
                actual.add(index, value);
                expected.add(index, value);
            } else if (operation == 2) {
                int index = random.nextInt(expected.size());
                check(actual.remove(index) == expected.remove(index), "random remove");
            } else if (operation == 3) {
                int index = random.nextInt(expected.size());
                check(actual.get(index) == expected.get(index), "random get");
            } else {
                check(actual.contains(value) == expected.contains(value), "random contains");
            }
            check(actual.size() == expected.size(), "random size");
        }
        for (int i = 0; i < expected.size(); i++) {
            check(actual.get(i) == expected.get(i), "final sequence");
        }
        expect(IndexOutOfBoundsException.class, () -> actual.get(actual.size()));
        expect(IndexOutOfBoundsException.class, () -> actual.remove(actual.size()));
        expect(IndexOutOfBoundsException.class, () -> actual.add(actual.size() + 1, 0));
    }

    private static void testLarge(IntList list) {
        for (int i = 0; i < 100_000; i++) {
            list.add(i);
        }
        check(list.size() == 100_000, "large size");
        check(list.get(0) == 0 && list.get(50_000) == 50_000
                && list.get(99_999) == 99_999, "large access");
        check(list.contains(99_999) && !list.contains(-1), "large search");
        list.add(50_000, -1);
        check(list.get(50_000) == -1 && list.remove(50_000) == -1, "large middle update");
    }

    private static void testHeap() throws ReflectiveOperationException {
        MinHeap heap = new MinHeap();
        PriorityQueue<Integer> expected = new PriorityQueue<>();
        Field field = MinHeap.class.getDeclaredField("elements");
        field.setAccessible(true);
        expect(NoSuchElementException.class, heap::peekMin);
        expect(NoSuchElementException.class, heap::extractMin);

        int[] values = {5, 1, 3, 1, -4, Integer.MAX_VALUE, Integer.MIN_VALUE, 0, 5};
        for (int value : values) {
            heap.insert(value);
            expected.add(value);
            check(heap.peekMin() == expected.peek(), "heap minimum after insertion");
            checkHeapProperty(heap, field);
        }
        int previous = Integer.MIN_VALUE;
        while (!expected.isEmpty()) {
            int actual = heap.extractMin();
            check(actual == expected.remove() && actual >= previous, "heap extraction order");
            previous = actual;
            checkHeapProperty(heap, field);
        }
        check(heap.size() == 0, "empty heap after extraction");

        Random random = new Random(42);
        for (int i = 0; i < 100_000; i++) {
            int value = random.nextInt();
            heap.insert(value);
            expected.add(value);
        }
        checkHeapProperty(heap, field);
        heap.resetMetrics();
        for (int i = 0; i < 100_000; i++) {
            check(heap.extractMin() == expected.remove(), "large heap extraction");
        }
        check(heap.size() == 0 && heap.getComparisonCount() > 0, "large heap metrics");
        expect(NoSuchElementException.class, heap::peekMin);
    }

    private static void checkHeapProperty(MinHeap heap, Field field)
            throws IllegalAccessException {
        int[] elements = (int[]) field.get(heap);
        for (int i = 1; i < heap.size(); i++) {
            check(elements[(i - 1) / 2] <= elements[i], "heap property");
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable error) {
            check(type.isInstance(error), "wrong exception: " + error);
            return;
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
