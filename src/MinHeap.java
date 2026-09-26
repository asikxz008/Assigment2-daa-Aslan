import java.util.NoSuchElementException;

public class MinHeap {
    private int[] elements = new int[16];
    private int size;
    private long comparisonCount;

    public void insert(int value) {
        ensureCapacity();
        int index = size++;
        while (index > 0) {
            int parent = (index - 1) / 2;
            comparisonCount++;
            if (elements[parent] <= value) {
                break;
            }
            elements[index] = elements[parent];
            index = parent;
        }
        elements[index] = value;
    }

    public int peekMin() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return elements[0];
    }

    public int extractMin() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        int minimum = elements[0];
        int last = elements[--size];
        if (size > 0) {
            int index = 0;
            while (index * 2 + 1 < size) {
                int child = index * 2 + 1;
                if (child + 1 < size) {
                    comparisonCount++;
                    if (elements[child + 1] < elements[child]) {
                        child++;
                    }
                }
                comparisonCount++;
                if (last <= elements[child]) {
                    break;
                }
                elements[index] = elements[child];
                index = child;
            }
            elements[index] = last;
        }
        return minimum;
    }

    public int size() {
        return size;
    }

    public void resetMetrics() {
        comparisonCount = 0;
    }

    public long getComparisonCount() {
        return comparisonCount;
    }

    private void ensureCapacity() {
        if (size == elements.length) {
            int[] expanded = new int[elements.length * 2];
            for (int i = 0; i < size; i++) {
                expanded[i] = elements[i];
            }
            elements = expanded;
        }
    }
}
