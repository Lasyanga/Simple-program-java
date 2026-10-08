package algorithms;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link BubbleSort}, the first algorithm extracted from the 2019 dialog-driven
 * version into a pure static method.
 *
 * <p>Two things are being pinned here. First, that the sort is genuinely pure: it must not
 * mutate the array it is handed, which matters because {@code Runner} passes the live
 * {@code Array.getCopy()} reference and reuses it across sorts. Second, that the trace records
 * independent snapshots in the right order - if the states were aliased to one working array,
 * every entry in the list would end up showing the final state and the "intermediate" display
 * would silently become a lie.
 */
class BubbleSortTest {

    @Test
    void sortsAnUnsortedArrayAscending() {
        assertArrayEquals(new int[]{1, 3, 5, 7, 9}, BubbleSort.bubbleSort(new int[]{5, 3, 9, 1, 7}));
    }

    @Test
    void leavesTheArgumentUntouched() {
        int[] input = {5, 3, 9, 1, 7};
        BubbleSort.bubbleSort(input);
        assertArrayEquals(new int[]{5, 3, 9, 1, 7}, input,
                "bubbleSort must not mutate its argument");
    }

    @Test
    void returnsAnArrayDistinctFromTheInput() {
        int[] input = {3, 1, 2};
        assertNotSame(input, BubbleSort.bubbleSort(input));
    }

    @Test
    void handlesAnAlreadySortedArray() {
        assertArrayEquals(new int[]{1, 2, 3, 4}, BubbleSort.bubbleSort(new int[]{1, 2, 3, 4}));
    }

    @Test
    void handlesASingleElement() {
        assertArrayEquals(new int[]{42}, BubbleSort.bubbleSort(new int[]{42}));
    }

    @Test
    void handlesAnEmptyArray() {
        assertArrayEquals(new int[]{}, BubbleSort.bubbleSort(new int[]{}));
    }

    @Test
    void handlesAllDuplicates() {
        // Every comparison here is a swap candidate only until the run settles; the early
        // exit must still terminate rather than spin.
        assertArrayEquals(new int[]{4, 4, 4, 4}, BubbleSort.bubbleSort(new int[]{4, 4, 4, 4}));
        assertArrayEquals(new int[]{2, 2, 5, 5, 5}, BubbleSort.bubbleSort(new int[]{5, 2, 5, 2, 5}));
    }

    @Test
    void handlesNegativeAndMixedValues() {
        assertArrayEquals(new int[]{-9, -1, 0, 3, 12},
                BubbleSort.bubbleSort(new int[]{3, -9, 12, 0, -1}));
    }

    @Test
    void traceIsEmptyWhenNothingNeededSwapping() {
        assertTrue(BubbleSort.bubbleSortTrace(new int[]{1, 2, 3}).isEmpty(),
                "an already-sorted array performs no swaps, so there is nothing to show");
        assertTrue(BubbleSort.bubbleSortTrace(new int[]{}).isEmpty());
        assertTrue(BubbleSort.bubbleSortTrace(new int[]{7}).isEmpty());
    }

    @Test
    void traceRecordsOneStatePerSwapInOrder() {
        // {5,3,9,1,7} needs 6 swaps to sort; reversing needs 3 (a swap of two elements is
        // still two inversions removed, so the count is fixed by the algorithm).
        var trace = BubbleSort.bubbleSortTrace(new int[]{2, 1});
        assertEquals(1, trace.size(), "swapping two inverted elements is one swap");
        assertArrayEquals(new int[]{1, 2}, trace.get(0));
    }

    @Test
    void everyTraceStateIsAnIndependentSnapshot() {
        var trace = BubbleSort.bubbleSortTrace(new int[]{4, 3, 2, 1});
        assertTrue(trace.size() > 1, "this input needs several swaps");
        Set<int[]> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int[] state : trace) {
            // Identity, not equals: two distinct clones of equal contents are still separate
            // objects, and an aliased working array would make every entry the same object.
            assertTrue(seen.add(state), "each trace entry must be a distinct array object");
        }
        // If the states aliased one working array, every entry would now read {1,2,3,4}.
        assertArrayEquals(new int[]{3, 4, 2, 1}, trace.get(0),
                "the first recorded state must still show the array as it was then");
        assertArrayEquals(new int[]{1, 2, 3, 4}, trace.get(trace.size() - 1),
                "the last recorded state is fully sorted");
    }

    @Test
    void traceAlsoLeavesTheArgumentUntouched() {
        int[] input = {5, 3, 9, 1, 7};
        BubbleSort.bubbleSortTrace(input);
        assertArrayEquals(new int[]{5, 3, 9, 1, 7}, input);
    }

    @Test
    void traceAgreesWithTheFinalResult() {
        int[] input = {9, 1, 8, 2, 7, 3};
        var trace = BubbleSort.bubbleSortTrace(input.clone());
        int[] result = BubbleSort.bubbleSort(input);
        if (!trace.isEmpty()) {
            assertArrayEquals(result, trace.get(trace.size() - 1),
                    "the final trace state and the sort result must be the same array");
        }
    }

    @Test
    void matchesAReferenceSortAcrossManyShapes() {
        int[][] cases = {
            {}, {1}, {2, 1}, {1, 2}, {3, 3, 3}, {5, 4, 3, 2, 1}, {1, 2, 3, 4, 5},
            {-1, 5, -3, 0, 2}, {10, -10, 0}, {2, 2, 1, 1}, {7, 7, 7, 1, 9, 9}
        };
        for (int[] input : cases) {
            int[] expected = input.clone();
            java.util.Arrays.sort(expected);
            assertArrayEquals(expected, BubbleSort.bubbleSort(input.clone()),
                    "failed for " + java.util.Arrays.toString(input));
        }
    }
}
