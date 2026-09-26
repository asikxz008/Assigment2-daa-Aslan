public class LinkedList implements IntList {
    private static class Node {
        int value;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    private Node head;
    private Node tail;
    private int size;
    private long accessCount;
    private long comparisonCount;
    private long movementCount;

    public void add(int value) {
        Node node = new Node(value);
        if (tail == null) {
            head = node;
        } else {
            tail.next = node;
        }
        tail = node;
        size++;
    }

    public void add(int index, int value) {
        checkInsertionIndex(index);
        if (index == size) {
            add(value);
        } else if (index == 0) {
            Node node = new Node(value);
            node.next = head;
            head = node;
            size++;
        } else {
            Node previous = nodeAt(index - 1);
            Node node = new Node(value);
            node.next = previous.next;
            previous.next = node;
            size++;
        }
    }

    public int remove(int index) {
        checkIndex(index);
        Node removed;
        if (index == 0) {
            removed = head;
            accessCount++;
            head = head.next;
            if (head == null) {
                tail = null;
            }
        } else {
            Node previous = nodeAt(index - 1);
            removed = previous.next;
            accessCount++;
            previous.next = removed.next;
            if (removed == tail) {
                tail = previous;
            }
        }
        size--;
        return removed.value;
    }

    public int get(int index) {
        checkIndex(index);
        return nodeAt(index).value;
    }

    public boolean contains(int value) {
        int visited = 0;
        for (Node current = head; current != null; current = current.next) {
            visited++;
            if (current.value == value) {
                accessCount += visited;
                comparisonCount += visited;
                return true;
            }
        }
        accessCount += visited;
        comparisonCount += visited;
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

    private Node nodeAt(int index) {
        Node current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        accessCount += index + 1L;
        return current;
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
