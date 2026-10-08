import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link InsertionSort}, extracted into a pure static method.
 *
 * <p>Deliberately mirrors {@code BubbleSortTest}'s five input shapes — unsorted, already
 * sorted, single element, empty, all duplicates — plus negatives, so the two extractions are
 * comparable.
 *
 * <p>One semantic difference from Bubble Sort is intentional and asserted below: the trace
 * records one state per <em>insertion</em>, unconditionally, so an already-sorted input still
 * yields {@code length - 1} entries. Bubble Sort's trace is per swap and therefore empty for
 * sorted input. Both are faithful to the 2019 behaviour; see the note in todo.md Task 7.
 */
class InsertionSortTest {

    @Test
    void sortsAnUnsortedArrayAscending() {
        assertArrayEquals(new int[]{1, 3, 5, 7, 9},
                InsertionSort.insertionSort(new int[]{5, 3, 9, 1, 7}));
    }

    @Test
    void leavesTheArgumentUntouched() {
        int[] input = {5, 3, 9, 1, 7};
        InsertionSort.insertionSort(input);
        assertArrayEquals(new int[]{5, 3, 9, 1, 7}, input,
                "insertionSort must not mutate its argument");
    }

    @Test
    void returnsAnArrayDistinctFromTheInput() {
        int[] input = {3, 1, 2};
        assertNotSame(input, InsertionSort.insertionSort(input));
    }

    @Test
    void handlesAnAlreadySortedArray() {
        assertArrayEquals(new int[]{1, 2, 3, 4},
                InsertionSort.insertionSort(new int[]{1, 2, 3, 4}));
    }

    @Test
    void handlesASingleElement() {
        assertArrayEquals(new int[]{42}, InsertionSort.insertionSort(new int[]{42}));
    }

    @Test
    void handlesAnEmptyArray() {
        assertArrayEquals(new int[]{}, InsertionSort.insertionSort(new int[]{}));
    }

    @Test
    void handlesAllDuplicates() {
        assertArrayEquals(new int[]{4, 4, 4, 4}, InsertionSort.insertionSort(new int[]{4, 4, 4, 4}));
        assertArrayEquals(new int[]{2, 2, 5, 5, 5},
                InsertionSort.insertionSort(new int[]{5, 2, 5, 2, 5}));
    }

    @Test
    void handlesNegativeAndMixedValues() {
        assertArrayEquals(new int[]{-9, -1, 0, 3, 12},
                InsertionSort.insertionSort(new int[]{3, -9, 12, 0, -1}));
    }

    @Test
    void handlesAReverseSortedArray() {
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9},
                InsertionSort.insertionSort(new int[]{9, 8, 7, 6, 5, 4, 3, 2, 1}));
    }

    @Test
    void traceHasOneEntryPerInsertion() {
        // The outer loop runs from the second element to the last, so n elements give n-1
        // recorded states.
        assertEquals(4, InsertionSort.insertionSortTrace(new int[]{5, 3, 9, 1, 7}).size());
        assertEquals(8, InsertionSort.insertionSortTrace(new int[]{9, 8, 7, 6, 5, 4, 3, 2, 1}).size());
    }

    @Test
    void traceRecordsNoOpInsertionsForAnAlreadySortedArray() {
        // The asymmetry with Bubble Sort, asserted so a future change to "only record real
        // movement" is a deliberate decision rather than an accident.
        var trace = InsertionSort.insertionSortTrace(new int[]{1, 2, 3});
        assertEquals(2, trace.size(), "every insertion is recorded, moved or not");
        assertArrayEquals(new int[]{1, 2, 3}, trace.get(0));
        assertArrayEquals(new int[]{1, 2, 3}, trace.get(1));
    }

    @Test
    void traceIsEmptyForInputTooShortToInsert() {
        assertTrue(InsertionSort.insertionSortTrace(new int[]{}).isEmpty());
        assertTrue(InsertionSort.insertionSortTrace(new int[]{7}).isEmpty(),
                "one element means nothing to insert");
    }

    @Test
    void everyTraceStateIsAnIndependentSnapshot() {
        var trace = InsertionSort.insertionSortTrace(new int[]{4, 3, 2, 1});
        assertEquals(3, trace.size());
        Set<int[]> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int[] state : trace) {
            assertTrue(seen.add(state), "each trace entry must be a distinct array object");
        }
        assertArrayEquals(new int[]{3, 4, 2, 1}, trace.get(0),
                "the first state must still show the array as it was then");
        assertArrayEquals(new int[]{1, 2, 3, 4}, trace.get(trace.size() - 1),
                "the last recorded state is fully sorted");
    }

    @Test
    void traceAgreesWithTheFinalResultAndLeavesTheArgumentUntouched() {
        int[] input = {9, 1, 8, 2, 7};
        var trace = InsertionSort.insertionSortTrace(input);
        assertArrayEquals(new int[]{9, 1, 8, 2, 7}, input,
                "the trace variant must not mutate its argument either");
        assertArrayEquals(InsertionSort.insertionSort(input), trace.get(trace.size() - 1));
    }

    @Test
    void eachTraceStateHasTheSameLengthAsTheInput() {
        var trace = InsertionSort.insertionSortTrace(new int[]{4, 1, 3, 2});
        for (int[] state : trace) {
            assertEquals(4, state.length);
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
            assertArrayEquals(expected, InsertionSort.insertionSort(input.clone()),
                    "failed for " + java.util.Arrays.toString(input));
        }
    }
}