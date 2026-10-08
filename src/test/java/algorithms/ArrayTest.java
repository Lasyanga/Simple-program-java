package algorithms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link Array}, which is what makes Runner's {@code sort} gate meaningless.
 *
 * <p>Background: {@code setCopy()} is called exactly once, right after the user finishes typing
 * elements. Every sort clones the array it is handed and never writes the result back, so the
 * stored copy stays in insertion order forever no matter how many sorts the user runs. The
 * {@code sort != 0} counter in {@code Menu()} therefore guarded nothing: the gate for Jump
 * Search was satisfied by "ran some sort", while the array Jump Search actually received came
 * from {@code getsorted()}, which sorts a fresh clone with {@code Arrays.sort} every call.
 */
class ArrayTest {

    @Test
    void getsortedReturnsTheElementsInAscendingOrder() {
        Array a = filled(5, 3, 9, 1, 7);
        assertArrayEquals(new int[]{1, 3, 5, 7, 9}, a.getsorted());
    }

    @Test
    void getsortedDoesNotMutateTheStoredCopy() {
        // The fact the whole gate rested on. If this ever starts failing, the sorts may
        // finally be writing back, and Runner's gate would become meaningful after all.
        Array a = filled(5, 3, 9, 1, 7);
        a.getsorted();
        assertArrayEquals(new int[]{5, 3, 9, 1, 7}, a.getCopy(),
                "sorting a copy must leave the stored array in insertion order");
    }

    @Test
    void getCopyHandsBackTheSameArrayEveryTimeRatherThanACopy() {
        // Documented trap: callers must clone before mutating. Every algorithm does.
        Array a = filled(1, 2, 3);
        assertSame(a.getCopy(), a.getCopy(),
                "getCopy returns the live reference, not a fresh copy");
        assertNotSame(a.getCopy(), a.getsorted(),
                "getsorted must at least hand back a distinct array");
    }

    @Test
    void setCopySnapshotsTheElementsAsTyped() {
        Array a = new Array(3);
        a.setElement(0, 4);
        a.setElement(1, 4);
        a.setElement(2, 4);
        a.setCopy();
        assertArrayEquals(new int[]{4, 4, 4}, a.getCopy());
        assertEquals(3, a.getLength());
    }

    @Test
    void repeatedGetsortedCallsAreStable() {
        Array a = filled(4, 2, 8);
        int[] first = a.getsorted();
        int[] second = a.getsorted();
        assertArrayEquals(first, second);
        assertArrayEquals(new int[]{2, 4, 8}, second);
    }

    @Test
    void getsortedOnASingleElementQueue() {
        Array a = filled(42);
        assertArrayEquals(new int[]{42}, a.getsorted());
    }

    private static Array filled(int... values) {
        Array a = new Array(values.length);
        for (int i = 0; i < values.length; i++) {
            a.setElement(i, values[i]);
        }
        a.setCopy();
        return a;
    }
}
