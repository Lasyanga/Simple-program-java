package algorithms;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CircularQueue}'s ring-buffer structure.
 *
 * <p>Task 21. This class had no tests because its operations printed to the console as a side
 * effect — {@code enqueue} narrated "Inserted 5" — so exercising it meant running the demo's
 * Scanner loop. {@code Task 21} moved that narration into {@code main}, which is what lets these
 * tests drive the buffer directly.
 *
 * <p>Two cases the brief calls out specifically, because both are where ring buffers traditionally
 * go wrong:
 * <ul>
 *   <li><b>Wraparound</b> — the rear index must roll from the last slot back to the first. Get it
 *       wrong and items overwrite each other or the queue reports full while holding garbage.</li>
 *   <li><b>Single-element reset</b> — removing the last item must return the queue to its
 *       constructed state ({@code front == rear == -1}), not leave one index stranded.</li>
 * </ul>
 *
 * <p>The buffer is built through the package-private {@code CircularQueue(int)} so a wraparound
 * can be reached in four operations rather than a hundred. The no-arg constructor is the demo's
 * capacity of 100 and is checked separately.
 *
 * <p>Values here are deliberately never -1: {@code dequeue()} returns -1 to signal an empty queue,
 * so storing -1 would make "empty" and "stored -1" indistinguishable through the return value.
 */
class CircularQueueTest {

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    @Test
    void aNewQueueIsEmptyButNotFull() {
        CircularQueue q = new CircularQueue(4);
        assertTrue(q.isEmpty());
        assertFalse(q.isFull());
    }

    @Test
    void refusesACapacityBelowOneAtConstruction() {
        // Same rule QueueJava enforces; % size on a zero-sized array would surface later as an
        // ArithmeticException far from the mistake.
        assertThrows(IllegalArgumentException.class, () -> new CircularQueue(0));
        assertThrows(IllegalArgumentException.class, () -> new CircularQueue(-1));
    }

    @Test
    void theDefaultCapacityIsOneHundred() {
        // The demo's fixed size, asserted because the no-arg constructor now delegates.
        CircularQueue q = new CircularQueue();
        for (int i = 0; i < 100; i++) {
            assertTrue(q.enqueue(i), "item " + i + " must fit in the 100-slot default");
        }
        assertTrue(q.isFull(), "100 items must exactly fill the default capacity");
        assertFalse(q.enqueue(100), "the 101st item must be refused");
    }

    // ------------------------------------------------------------------
    // The basic FIFO contract
    // ------------------------------------------------------------------

    @Test
    void removesItemsInTheOrderTheyWereAdded() {
        CircularQueue q = new CircularQueue(3);
        q.enqueue(10);
        q.enqueue(20);
        q.enqueue(30);

        assertEquals(10, q.dequeue());
        assertEquals(20, q.dequeue());
        assertEquals(30, q.dequeue());
        assertTrue(q.isEmpty());
    }

    @Test
    void enqueueOnAFullQueueIsRefusedAndLeavesContentsIntact() {
        CircularQueue q = new CircularQueue(2);
        q.enqueue(1);
        q.enqueue(2);
        assertTrue(q.isFull());

        assertFalse(q.enqueue(3), "a full queue must refuse the new item");

        assertEquals(1, q.dequeue(), "the refused item must not have displaced what was stored");
        assertEquals(2, q.dequeue());
    }

    @Test
    void dequeueOnAnEmptyQueueReturnsTheSentinelAndLeavesStateAlone() {
        CircularQueue q = new CircularQueue(4);

        assertEquals(-1, q.dequeue(), "-1 is the 2019 empty-queue sentinel");

        assertTrue(q.isEmpty(), "a refused dequeue must not corrupt the indices");
        assertFalse(q.isFull());
        // Still usable afterwards.
        assertTrue(q.enqueue(7));
        assertEquals(7, q.dequeue());
    }

    // ------------------------------------------------------------------
    // The case the brief calls out: wraparound, last slot back to first
    // ------------------------------------------------------------------

    /**
     * The rear index must roll from the last slot to slot 0.
     *
     * <p>Capacity 4 makes the arithmetic visible: fill it (rear at slot 3), drain two (front moves
     * to slot 2), then enqueue two more. Those two land in slots 0 and 1 — past the end of the
     * array, which only works if {@code rear = (rear+1) % size} actually wraps. Then drain
     * everything and check the order survives the wrap.
     */
    @Test
    void theRearWrapsFromTheLastSlotToTheFirst() {
        CircularQueue q = new CircularQueue(4);

        // Fill: slots 0..3 hold 100,101,102,103. front=0, rear=3.
        q.enqueue(100);
        q.enqueue(101);
        q.enqueue(102);
        q.enqueue(103);
        assertTrue(q.isFull());

        // Drain two: front moves to slot 2. Two slots free, at the *end* of the array.
        assertEquals(100, q.dequeue());
        assertEquals(101, q.dequeue());
        assertFalse(q.isFull());

        // These two must wrap into slots 0 and 1.
        assertTrue(q.enqueue(104), "the queue has room; enqueue must succeed across the wrap");
        assertTrue(q.enqueue(105));
        assertTrue(q.isFull(), "after wrapping the queue is full again");

        // And the order must survive the boundary.
        assertEquals(102, q.dequeue(), "the oldest remaining item, still in slot 2");
        assertEquals(103, q.dequeue(), "slot 3, the last pre-wrap item");
        assertEquals(104, q.dequeue(), "slot 0 - the first post-wrap item");
        assertEquals(105, q.dequeue(), "slot 1");
        assertTrue(q.isEmpty());
    }

    /**
     * The same wrap, but through the front index on a buffer that wraps repeatedly.
     *
     * <p>Cycles more items through than the capacity holds, so both indices pass slot 0 several
     * times. If either used plain {@code ++} instead of {@code % size} this would either throw
     * or return the wrong item within the first cycle.
     */
    @Test
    void repeatedWraparoundPreservesFifoOrder() {
        CircularQueue q = new CircularQueue(3);
        List<Integer> expected = new ArrayList<>();

        // 15 items through a 3-slot buffer: five full cycles of wraparound.
        for (int i = 0; i < 15; i++) {
            assertTrue(q.enqueue(i), "enqueue " + i);
            expected.add(i);
            if (i % 3 == 2) {
                // Drain three, then refill - keeps the buffer moving through the wrap.
                for (int j = 0; j < 3; j++) {
                    assertEquals((Integer) expected.remove(0), (Integer) q.dequeue(),
                            "FIFO order broken at i=" + i + ", j=" + j);
                }
            }
        }

        // Drain whatever is left (15 % 3 == 0, so nothing, but assert it anyway).
        while (!q.isEmpty()) {
            assertEquals((Integer) expected.remove(0), (Integer) q.dequeue());
        }
        assertTrue(expected.isEmpty(), "every enqueued item should have come back out");
    }

    // ------------------------------------------------------------------
    // The case the brief calls out: removing the single element resets the queue
    // ------------------------------------------------------------------

    /**
     * Removing the one remaining item must return the queue to its constructed state.
     *
     * <p>The guard is {@code if (front == rear)}: when the last item leaves, both indices go back
     * to -1 rather than one of them advancing. If that reset were missing, {@code isEmpty()} —
     * which tests {@code front == -1} — would stay false forever while the queue held nothing.
     */
    @Test
    void removingTheLastElementResetsToTheEmptyState() {
        CircularQueue q = new CircularQueue(5);
        q.enqueue(42);
        assertFalse(q.isEmpty());

        assertEquals(42, q.dequeue());

        assertTrue(q.isEmpty(), "the reset must restore front == rear == -1");
        assertFalse(q.isFull(), "an empty queue must never report full");
        assertEquals(-1, q.dequeue(), "and it must still refuse a second dequeue");
    }

    /**
     * The reset must leave the queue genuinely reusable, not merely report empty.
     *
     * <p>This is the assertion that would fail if only one of the two indices were reset: the
     * buffer would look empty but {@code front} and {@code rear} would disagree, so the next
     * enqueue would write somewhere the dequeue would not read.
     */
    @Test
    void theQueueIsFullyReusableAfterTheSingleElementReset() {
        CircularQueue q = new CircularQueue(3);
        q.enqueue(9);
        assertEquals(9, q.dequeue());
        assertTrue(q.isEmpty());

        // A fresh fill/drain cycle must behave exactly as it did on a new queue.
        assertTrue(q.enqueue(1));
        assertTrue(q.enqueue(2));
        assertTrue(q.enqueue(3));
        assertTrue(q.isFull());
        assertFalse(q.enqueue(4));

        assertEquals(1, q.dequeue());
        assertEquals(2, q.dequeue());
        assertEquals(3, q.dequeue());
        assertTrue(q.isEmpty());
    }

    // ------------------------------------------------------------------
    // display(): the view the demo prints
    // ------------------------------------------------------------------

    @Test
    void displayReportsTheEmptyCase() {
        assertEquals("Queue is empty\n", new CircularQueue(4).display());
    }

    @Test
    void displayListsTheItemsBetweenFrontAndRear() {
        CircularQueue q = new CircularQueue(4);
        q.enqueue(10);
        q.enqueue(20);
        q.enqueue(30);

        // front+1 and rear+1 are 1-based, the 2019 display's convention.
        assertEquals("Front ->1\nItems:\t10\t20\t30\nRear ->3\n", q.display());
    }

    @Test
    void displayListsWrappedItemsInFifoOrderNotSlotOrder() {
        // The whole point of the ring: after a wrap the display must walk front -> rear through
        // the modulo, not 0 -> capacity.
        CircularQueue q = new CircularQueue(4);
        q.enqueue(100);
        q.enqueue(101);
        q.enqueue(102);
        q.enqueue(103);
        q.dequeue();
        q.dequeue();
        q.enqueue(104);   // wraps into slot 0
        q.enqueue(105);   // slot 1

        // After the two drains front sits in slot 2 (0-based) holding 102, and the two
        // wraparound enqueues put 104 in slot 0 and 105 in slot 1, so rear is slot 1.
        // The display reports both as 1-based, which is the 2019 convention.
        assertEquals("Front ->3\nItems:\t102\t103\t104\t105\nRear ->2\n", q.display());
    }

    // ------------------------------------------------------------------
    // A fill-and-drain cycle, the shape that catches index drift
    // ------------------------------------------------------------------

    @Test
    void fillAndDrainRepeatedlyWithoutDrift() {
        CircularQueue q = new CircularQueue(2);
        for (int round = 0; round < 3; round++) {
            assertTrue(q.enqueue(round * 2));
            assertTrue(q.enqueue(round * 2 + 1));
            assertTrue(q.isFull(), "round " + round);
            assertEquals(round * 2, q.dequeue(), "round " + round);
            assertEquals(round * 2 + 1, q.dequeue(), "round " + round);
            assertTrue(q.isEmpty(), "round " + round);
        }
    }
}
