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
 * Tests for {@link MergeSort}, extracted into a pure static method.
 *
 * <p>Same five input shapes as the other three sort tests so the extractions stay comparable.
 *
 * <p>The original file was the least readable in the project: four fields ({@code count},
 * {@code x}, {@code y}, {@code round}) existed only to stagger a text visualisation, and
 * {@code getMergeProcess()} was a {@code getX()} method called for its side effect with the
 * return value discarded. None of that survives, so this test suite covers the algorithm and
 * nothing about the drawing.
 */
class MergeSortTest {

    @Test
    void sortsAnUnsortedArrayAscending() {
        assertArrayEquals(new int[]{1, 3, 5, 7, 9},
                MergeSort.mergeSort(new int[]{5, 3, 9, 1, 7}));
    }

    @Test
    void leavesTheArgumentUntouched() {
        int[] input = {5, 3, 9, 1, 7};
        MergeSort.mergeSort(input);
        assertArrayEquals(new int[]{5, 3, 9, 1, 7}, input,
                "mergeSort must not mutate its argument");
    }

    @Test
    void returnsAnArrayDistinctFromTheInput() {
        int[] input = {3, 1, 2};
        assertNotSame(input, MergeSort.mergeSort(input));
    }

    @Test
    void handlesAnAlreadySortedArray() {
        assertArrayEquals(new int[]{1, 2, 3, 4},
                MergeSort.mergeSort(new int[]{1, 2, 3, 4}));
    }

    @Test
    void handlesASingleElement() {
        assertArrayEquals(new int[]{42}, MergeSort.mergeSort(new int[]{42}));
    }

    @Test
    void handlesAnEmptyArray() {
        assertArrayEquals(new int[]{}, MergeSort.mergeSort(new int[]{}));
    }

    @Test
    void handlesAllDuplicates() {
        assertArrayEquals(new int[]{4, 4, 4, 4}, MergeSort.mergeSort(new int[]{4, 4, 4, 4}));
        assertArrayEquals(new int[]{2, 2, 5, 5, 5},
                MergeSort.mergeSort(new int[]{5, 2, 5, 2, 5}));
    }

    @Test
    void handlesNegativeAndMixedValues() {
        assertArrayEquals(new int[]{-9, -1, 0, 3, 12},
                MergeSort.mergeSort(new int[]{3, -9, 12, 0, -1}));
    }

    @Test
    void handlesAReverseSortedArrayWhichExercisesTheTailCopyLoop() {
        // Descending input makes every merge exhaust the RIGHT side first, so the left-hand
        // leftovers are non-empty. That is precisely the path the original's single tail-copy
        // loop exists to serve - delete that loop and this test fails. Ascending input exercises
        // the mirror case, where the leftovers are on the right and nothing needs copying.
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9},
                MergeSort.mergeSort(new int[]{9, 8, 7, 6, 5, 4, 3, 2, 1}));
    }

    @Test
    void traceHasOneEntryPerMerge() {
        // A merge tree over n leaves has n-1 internal nodes, so n elements give n-1 merges.
        assertEquals(4, MergeSort.mergeSortTrace(new int[]{5, 3, 9, 1, 7}).size());
        assertEquals(8, MergeSort.mergeSortTrace(new int[]{9, 8, 7, 6, 5, 4, 3, 2, 1}).size());
    }

    @Test
    void traceIsEmptyWhenThereIsNothingToMerge() {
        assertTrue(MergeSort.mergeSortTrace(new int[]{}).isEmpty());
        assertTrue(MergeSort.mergeSortTrace(new int[]{7}).isEmpty(),
                "a single element has no merge to perform");
    }

    @Test
    void traceProgressesFromLeastToMostSorted() {
        // Merges happen bottom-up, so the first recorded state is the result of merging two
        // adjacent pairs and should already have two-element runs in order.
        var trace = MergeSort.mergeSortTrace(new int[]{4, 3, 2, 1});
        assertEquals(3, trace.size());
        assertArrayEquals(new int[]{3, 4, 2, 1}, trace.get(0),
                "deepest merge first: it fixes the leading pair [4,3] and nothing else yet");
        assertArrayEquals(new int[]{3, 4, 1, 2}, trace.get(1),
                "then the trailing pair [2,1] is fixed");
        assertArrayEquals(new int[]{1, 2, 3, 4}, trace.get(2),
                "the last merge combines the two halves and finishes the sort");
    }

    @Test
    void everyTraceStateIsAnIndependentSnapshot() {
        var trace = MergeSort.mergeSortTrace(new int[]{5, 3, 9, 1, 7});
        Set<int[]> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int[] state : trace) {
            assertTrue(seen.add(state), "each trace entry must be a distinct array object");
            assertEquals(5, state.length);
        }
        assertArrayEquals(new int[]{3, 5, 9, 1, 7}, trace.get(0),
                "the first state must show the array mid-sort, not the finished result");
        assertArrayEquals(new int[]{1, 3, 5, 7, 9}, trace.get(trace.size() - 1));
    }

    @Test
    void traceAgreesWithTheFinalResultAndLeavesTheArgumentUntouched() {
        int[] input = {9, 1, 8, 2, 7, 3};
        var trace = MergeSort.mergeSortTrace(input);
        assertArrayEquals(new int[]{9, 1, 8, 2, 7, 3}, input,
                "the trace variant must not mutate its argument either");
        assertArrayEquals(MergeSort.mergeSort(input), trace.get(trace.size() - 1));
    }

    @Test
    void matchesAReferenceSortAcrossManyShapes() {
        int[][] cases = {
            {}, {1}, {2, 1}, {1, 2}, {3, 3, 3}, {5, 4, 3, 2, 1}, {1, 2, 3, 4, 5},
            {-1, 5, -3, 0, 2}, {10, -10, 0}, {2, 2, 1, 1}, {7, 7, 7, 1, 9, 9},
            {1, 1, 1, 1, 1, 1, 1, 1}, {6, 5, 4, 3, 2, 1, 9, 8, 7}
        };
        for (int[] input : cases) {
            int[] expected = input.clone();
            java.util.Arrays.sort(expected);
            assertArrayEquals(expected, MergeSort.mergeSort(input.clone()),
                    "failed for " + java.util.Arrays.toString(input));
        }
    }

    @Test
    void sortsALargeArrayWithoutStackTrouble() {
        int[] input = new int[2000];
        for (int i = 0; i < input.length; i++) {
            input[i] = (input.length - i) * 7919 % 10007;
        }
        int[] expected = input.clone();
        java.util.Arrays.sort(expected);
        assertArrayEquals(expected, MergeSort.mergeSort(input.clone()));
        assertEquals(1999, MergeSort.mergeSortTrace(input.clone()).size(),
                "n-1 merges regardless of how deep the recursion went");
    }
}
