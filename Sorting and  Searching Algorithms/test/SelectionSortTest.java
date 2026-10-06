import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SelectionSort}, extracted into a pure static method.
 *
 * <p>Same five input shapes as {@code BubbleSortTest} and {@code InsertionSortTest} so the
 * three extractions stay comparable.
 *
 * <p>Selection Sort's trace records one state per <em>outer iteration</em>, so {@code n} elements
 * give {@code n} entries — unlike Bubble Sort (per swap) or Insertion Sort (per insertion).
 * Because the original swaps unconditionally, an iteration where the minimum is already in
 * place still records a state, and all-duplicates input records {@code n} identical lines.
 * That is asserted below so the behaviour cannot change by accident.
 */
class SelectionSortTest {

    @Test
    void sortsAnUnsortedArrayAscending() {
        assertArrayEquals(new int[]{1, 3, 5, 7, 9},
                SelectionSort.selectionSort(new int[]{5, 3, 9, 1, 7}));
    }

    @Test
    void leavesTheArgumentUntouched() {
        int[] input = {5, 3, 9, 1, 7};
        SelectionSort.selectionSort(input);
        assertArrayEquals(new int[]{5, 3, 9, 1, 7}, input,
                "selectionSort must not mutate its argument");
    }

    @Test
    void returnsAnArrayDistinctFromTheInput() {
        int[] input = {3, 1, 2};
        assertNotSame(input, SelectionSort.selectionSort(input));
    }

    @Test
    void handlesAnAlreadySortedArray() {
        assertArrayEquals(new int[]{1, 2, 3, 4},
                SelectionSort.selectionSort(new int[]{1, 2, 3, 4}));
    }

    @Test
    void handlesASingleElement() {
        assertArrayEquals(new int[]{42}, SelectionSort.selectionSort(new int[]{42}));
    }

    @Test
    void handlesAnEmptyArray() {
        assertArrayEquals(new int[]{}, SelectionSort.selectionSort(new int[]{}));
    }

    @Test
    void handlesAllDuplicatesDespiteTheUnconditionalSwap() {
        // Every iteration finds min == i and swaps an element with itself. The swap is
        // harmless, but the trace still records one state per iteration, so this input yields
        // length-many identical entries. Asserted because it looks like a bug otherwise.
        assertArrayEquals(new int[]{4, 4, 4, 4},
                SelectionSort.selectionSort(new int[]{4, 4, 4, 4}));
        assertArrayEquals(new int[]{2, 2, 5, 5, 5},
                SelectionSort.selectionSort(new int[]{5, 2, 5, 2, 5}));

        var trace = SelectionSort.selectionSortTrace(new int[]{4, 4, 4, 4});
        assertEquals(4, trace.size(), "one state per outer iteration, not per real swap");
        for (int[] state : trace) {
            assertArrayEquals(new int[]{4, 4, 4, 4}, state);
        }
    }

    @Test
    void handlesNegativeAndMixedValues() {
        assertArrayEquals(new int[]{-9, -1, 0, 3, 12},
                SelectionSort.selectionSort(new int[]{3, -9, 12, 0, -1}));
    }

    @Test
    void handlesAReverseSortedArray() {
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9},
                SelectionSort.selectionSort(new int[]{9, 8, 7, 6, 5, 4, 3, 2, 1}));
    }

    @Test
    void traceHasOneEntryPerOuterIteration() {
        // i runs from 0 to length-1 inclusive, so n elements give n entries.
        assertEquals(5, SelectionSort.selectionSortTrace(new int[]{5, 3, 9, 1, 7}).size());
        assertEquals(9, SelectionSort.selectionSortTrace(new int[]{9, 8, 7, 6, 5, 4, 3, 2, 1}).size());
    }

    @Test
    void traceIsEmptyOnlyWhenThereIsNothingToIterate() {
        assertTrue(SelectionSort.selectionSortTrace(new int[]{}).isEmpty());
        assertEquals(1, SelectionSort.selectionSortTrace(new int[]{7}).size(),
                "a single element still runs one outer iteration");
    }

    @Test
    void everyTraceStateIsAnIndependentSnapshot() {
        var trace = SelectionSort.selectionSortTrace(new int[]{4, 3, 2, 1});
        assertEquals(4, trace.size());
        Set<int[]> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int[] state : trace) {
            assertTrue(seen.add(state), "each trace entry must be a distinct array object");
        }
        assertArrayEquals(new int[]{1, 3, 2, 4}, trace.get(0),
                "after the first pass the smallest value leads, having swapped with index 0");
        assertArrayEquals(new int[]{1, 2, 3, 4}, trace.get(trace.size() - 1),
                "the last recorded state is fully sorted");
    }

    @Test
    void traceAgreesWithTheFinalResultAndLeavesTheArgumentUntouched() {
        int[] input = {9, 1, 8, 2, 7};
        var trace = SelectionSort.selectionSortTrace(input);
        assertArrayEquals(new int[]{9, 1, 8, 2, 7}, input,
                "the trace variant must not mutate its argument either");
        assertArrayEquals(SelectionSort.selectionSort(input), trace.get(trace.size() - 1));
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
            assertArrayEquals(expected, SelectionSort.selectionSort(input.clone()),
                    "failed for " + java.util.Arrays.toString(input));
        }
    }
}