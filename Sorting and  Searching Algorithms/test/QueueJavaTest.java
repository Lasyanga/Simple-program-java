import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link QueueJava}'s bounds guards.
 *
 * <p>Background: both guards printed a warning and then carried on. {@code System.exit(1)},
 * the statement that used to stop them, is commented out in the source, so "Overflow Program
 * terminated." was printed and the insert happened anyway. Underflow was worse: {@code count--}
 * drove {@code count} to {@code -1}, and since {@code isEmpty()} tests {@code size() == 0} the
 * queue then reported neither empty nor full, permanently.
 *
 * <p>These tests drive the class directly rather than through {@code main}, so the console
 * menu and its {@code Scanner} loop are out of scope; see Task 21 for decoupling that loop.
 * {@code dequeue()} returns void, so removed values are checked via {@code peek()}.
 */
class QueueJavaTest {

    @Test
    void enqueueOnAFullQueueLeavesTheQueueUnchanged() {
        QueueJava q = new QueueJava(2);
        q.enqueue(10);
        q.enqueue(20);
        assertTrue(q.isFull());

        q.enqueue(30); // must be refused

        assertEquals(2, q.size(), "a full queue must not grow past its capacity");
        assertEquals(10, q.peek(), "the refused item must not have displaced what was stored");
    }

    @Test
    void dequeueOnAnEmptyQueueLeavesTheCountAtZero() {
        QueueJava q = new QueueJava(3);
        assertTrue(q.isEmpty());

        q.dequeue(); // must be refused

        assertEquals(0, q.size(), "an empty queue must not report a negative size");
        assertTrue(q.isEmpty(), "a refused dequeue must leave the queue empty");
    }

    @Test
    void theQueueIsStillUsableAfterARefusedDequeue() {
        // The original corruption: count went to -1, so isEmpty() and isFull() were both
        // false forever and every later operation was off by one.
        QueueJava q = new QueueJava(3);
        q.dequeue();
        q.dequeue();

        q.enqueue(7);
        assertEquals(1, q.size(), "one enqueue into an empty queue must read as size 1");
        assertEquals(7, q.peek());
        assertFalse(q.isFull());
    }

    @Test
    void theQueueIsStillUsableAfterARefusedEnqueue() {
        QueueJava q = new QueueJava(1);
        q.enqueue(5);
        q.enqueue(6); // refused

        assertEquals(1, q.size());
        assertEquals(5, q.peek(), "the first item must still be the one stored");
    }

    @Test
    void refusesACapacityBelowOneAtConstruction() {
        // Today a zero capacity is accepted and then throws ArithmeticException from the
        // % capacity in enqueue, some distance from the mistake that caused it.
        assertThrows(IllegalArgumentException.class, () -> new QueueJava(0));
        assertThrows(IllegalArgumentException.class, () -> new QueueJava(-1));
    }

    @Test
    void enqueueDequeueAndPeekStillWorkInTheNormalCase() {
        QueueJava q = new QueueJava(3);
        assertTrue(q.isEmpty());
        assertFalse(q.isFull());

        q.enqueue(1);
        q.enqueue(2);
        q.enqueue(3);

        assertEquals(3, q.size());
        assertTrue(q.isFull());
        assertEquals(1, q.peek());

        q.dequeue();
        assertEquals(2, q.size());
        assertEquals(2, q.peek());
    }

    @Test
    void fillAndDrainRepeatedlyWithoutDrift() {
        QueueJava q = new QueueJava(2);
        for (int round = 0; round < 3; round++) {
            q.enqueue(round * 2);
            q.enqueue(round * 2 + 1);
            assertEquals(2, q.size(), "round " + round);
            q.dequeue();
            assertEquals(1, q.size(), "round " + round);
            q.dequeue();
            assertEquals(0, q.size(), "round " + round);
        }
    }
}