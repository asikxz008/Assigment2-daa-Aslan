public class DynamicArray implements IntList {
    private int[] elements = new int[16];
    private int size;
    private long accessCount;
    private long comparisonCount;
    private long movementCount;

    public void add(int value) {
        ensureCapacity();
        elements[size++] = value;
    }

    public void add(int index, int value) {
        checkInsertionIndex(index);
        ensureCapacity();
        for (int i = size; i > index; i--) {
            elements[i] = elements[i - 1];
        }
        accessCount += size - index;
        movementCount += size - index;
        elements[index] = value;
        size++;
    }

    public int remove(int index) {
        checkIndex(index);
        int removed = elements[index];
        accessCount++;
        for (int i = index; i < size - 1; i++) {
            elements[i] = elements[i + 1];
        }
        accessCount += size - index - 1;
        movementCount += size - index - 1;
        size--;
        return removed;
    }

    public int get(int index) {
        checkIndex(index);
        accessCount++;
        return elements[index];
    }

    public boolean contains(int value) {
        for (int i = 0; i < size; i++) {
            if (elements[i] == value) {
                accessCount += i + 1L;
                comparisonCount += i + 1L;
                return true;
            }
        }
        accessCount += size;
        comparisonCount += size;
        return false;
    }

    public int size() {
        return size;
    }

    public void resetMetrics() {
        accessCount = 0;
        comparisonCount = 0;
        movementCount = 0;
    }

    public long getAccessCount() {
        return accessCount;
    }

    public long getComparisonCount() {
        return comparisonCount;
    }

    public long getMovementCount() {
        return movementCount;
    }

    private void ensureCapacity() {
        if (size == elements.length) {
            int[] expanded = new int[elements.length * 2];
            for (int i = 0; i < size; i++) {
                expanded[i] = elements[i];
            }
            accessCount += size;
            movementCount += size;
            elements = expanded;
        }
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index: " + index + ", size: " + size);
        }
    }

    private void checkInsertionIndex(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("index: " + index + ", size: " + size);
        }
    }
}
