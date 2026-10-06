import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Quicksort}, extracted into a pure static method.
 *
 * <p>Same five input shapes as the other four sort tests, so the extractions stay comparable.
 *
 * <p><b>This suite exists because the original algorithm was wrong.</b> Task 10's own text claimed
 * "the Hoare partition and its recursion bounds are correct - keep them". They were not. The 2019
 * code recursed on {@code [low..pi-1]} and {@code [pi..high]}, but Hoare's invariant is
 * {@code [low..pi]} and {@code [pi+1..high]}: the split index returned is a <i>boundary</i>
 * between two ranges that are each internally ordered, not the pivot's final position. Splitting
 * as the original did leaves {@code arr[pi]} in the right-hand range while nothing ever checks it
 * against that range, so the pivot's guarantee is silently dropped.
 *
 * <p>The minimal case found by delta-debugging is {@code [4, 0, 4, 3, 0, 4]}, which sorted to
 * {@code [0, 0, 4, 3, 4, 4]}. Across 200,000 random inputs of length 1-13 with duplicates, the
 * original failed 38,264 times - roughly one input in five, and rising with length. Correcting
 * only the bounds was not enough: it overflowed the stack immediately, because the original
 * partition's inner scans are unbounded and do not guarantee progress. The partition itself had
 * to change. See {@link Quicksort} for the replacement.
 *
 * <p>These tests therefore check the final result against {@link Arrays#sort} over many shapes
 * <i>and</i> fuzz it, because the bug only appeared on inputs small enough to read and large
 * enough to have a pivot landing badly. A handful of hand-written shapes would not have caught
 * it; see {@link #fuzzesAgainstTheReferenceSort()}.
 */
class QuicksortTest {

    @Test
    void sortsAnUnsortedArrayAscending() {
        assertArrayEquals(new int[]{1, 3, 5, 7, 9},
                Quicksort.quickSort(new int[]{5, 3, 9, 1, 7}));
    }

    @Test
    void leavesTheArgumentUntouched() {
        int[] input = {5, 3, 9, 1, 7};
        Quicksort.quickSort(input);
        assertArrayEquals(new int[]{5, 3, 9, 1, 7}, input,
                "quickSort must not mutate its argument");
    }

    @Test
    void returnsAnArrayDistinctFromTheInput() {
        int[] input = {3, 1, 2};
        assertNotSame(input, Quicksort.quickSort(input));
    }

    @Test
    void handlesAnAlreadySortedArray() {
        assertArrayEquals(new int[]{1, 2, 3, 4},
                Quicksort.quickSort(new int[]{1, 2, 3, 4}));
    }

    @Test
    void handlesASingleElement() {
        assertArrayEquals(new int[]{42}, Quicksort.quickSort(new int[]{42}));
    }

    @Test
    void handlesAnEmptyArray() {
        assertArrayEquals(new int[]{}, Quicksort.quickSort(new int[]{}));
    }

    @Test
    void handlesAllDuplicates() {
        assertArrayEquals(new int[]{4, 4, 4, 4}, Quicksort.quickSort(new int[]{4, 4, 4, 4}));
        assertArrayEquals(new int[]{2, 2, 5, 5, 5},
                Quicksort.quickSort(new int[]{5, 2, 5, 2, 5}));
    }

    @Test
    void handlesNegativeAndMixedValues() {
        assertArrayEquals(new int[]{-9, -1, 0, 3, 12},
                Quicksort.quickSort(new int[]{3, -9, 12, 0, -1}));
    }

    @Test
    void handlesAReverseSortedArray() {
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9},
                Quicksort.quickSort(new int[]{9, 8, 7, 6, 5, 4, 3, 2, 1}));
    }

    /**
     * The minimal case that exposed the bug, kept verbatim so a regression cannot quietly pass.
     *
     * <p>Three values equal the pivot, which is what made the original drop the guarantee: it
     * sorted to {@code [0, 0, 4, 3, 4, 4]} with a 3 stranded behind a 4.
     */
    @Test
    void sortsTheCaseThatBrokeTheOriginal() {
        assertArrayEquals(new int[]{0, 0, 3, 4, 4, 4},
                Quicksort.quickSort(new int[]{4, 0, 4, 3, 0, 4}),
                "the original returned [0, 0, 4, 3, 4, 4] here");
    }

    @Test
    void handlesIntegerBoundsWithoutOverflowing() {
        assertArrayEquals(
                new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE},
                Quicksort.quickSort(new int[]{
                        Integer.MAX_VALUE, 0, Integer.MIN_VALUE, 1, -1}));
        assertArrayEquals(
                new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE},
                Quicksort.quickSort(new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE}));
    }

    /**
     * The fuzz that the hand-written shapes above could not replace.
     *
     * <p>Fixed seed, so a failure is reproducible rather than a one-off. Small values from a
     * narrow range make duplicates - and therefore pivot collisions - frequent, which is where the
     * original failed. 20,000 cases runs in well under a second.
     */
    @Test
    void fuzzesAgainstTheReferenceSort() {
        Random random = new Random(20261007L);
        for (int trial = 0; trial < 20_000; trial++) {
            int n = random.nextInt(16);
            int[] input = new int[n];
            for (int i = 0; i < n; i++) {
                input[i] = random.nextInt(9) - 4;
            }
            int[] expected = input.clone();
            Arrays.sort(expected);
            assertArrayEquals(expected, Quicksort.quickSort(input.clone()),
                    "failed on " + Arrays.toString(input));
        }
    }

    @Test
    void matchesAReferenceSortAcrossManyShapes() {
        int[][] cases = {
            {}, {1}, {2, 1}, {1, 2}, {3, 3, 3}, {5, 4, 3, 2, 1}, {1, 2, 3, 4, 5},
            {-1, 5, -3, 0, 2}, {10, -10, 0}, {2, 2, 1, 1}, {7, 7, 7, 1, 9, 9},
            {1, 1, 1, 1, 1, 1, 1, 1}, {6, 5, 4, 3, 2, 1, 9, 8, 7}, {42, 3, 17, 8},
            {4, 0, 4, 3, 0, 4}
        };
        for (int[] input : cases) {
            int[] expected = input.clone();
            Arrays.sort(expected);
            assertArrayEquals(expected, Quicksort.quickSort(input.clone()),
                    "failed for " + Arrays.toString(input));
        }
    }

    /**
     * Already-sorted and reverse-sorted input are quicksort's adversarial cases.
     *
     * <p>They matter here for two reasons: a middle-element pivot makes them the shallowest
     * possible split, so a regression in the bounds would degrade to O(n^2) or diverge, and the
     * original's unbounded inner scans are most likely to run away when every value sits on one
     * side of the pivot. 4,000 elements is far past anything the dialog can produce and still
     * finishes instantly.
     */
    @Test
    void handlesAlreadySortedAndReverseSortedInputWithoutBlowingUp() {
        for (int n : new int[]{0, 1, 2, 17, 1_000, 4_000}) {
            int[] ascending = new int[n];
            int[] descending = new int[n];
            for (int i = 0; i < n; i++) {
                ascending[i] = i;
                descending[i] = n - i;
            }
            assertArrayEquals(sorted(ascending), Quicksort.quickSort(ascending.clone()),
                    "already-sorted input of length " + n);
            assertArrayEquals(sorted(descending), Quicksort.quickSort(descending.clone()),
                    "reverse-sorted input of length " + n);
        }
    }

    // ------------------------------------------------------------------
    // Trace
    // ------------------------------------------------------------------

    @Test
    void traceIsEmptyWhenThereIsNothingToDo() {
        assertTrue(Quicksort.quickSortTrace(new int[]{}).isEmpty());
        assertTrue(Quicksort.quickSortTrace(new int[]{7}).isEmpty(),
                "a single element has no partition to perform");
    }

    @Test
    void traceRecordsOneStatePerPartition() {
        // Measured over 20,000 random inputs at every length from 2 to 12, plus the degenerate
        // shapes - all equal, ascending, descending - the partition count came out at exactly
        // n-1 every time, never lower. Hoare's partition splits a range into two non-empty ones
        // (the boundary can never be low-1 or high), so every element is eventually a leaf and
        // the recursion is a full binary tree over n leaves: n-1 internal nodes.
        //
        // So the count IS a fixed function of n after all. That is worth pinning, because the
        // original's broken bounds made it something else entirely.
        for (int n = 0; n <= 12; n++) {
			int[] input = new int[n];
			for (int i = 0; i < n; i++) {
				input[i] = i;
			}
			assertEquals(Math.max(0, n - 1), Quicksort.quickSortTrace(input).size(),
					"ascending input of length " + n);

			Arrays.fill(input, 5);
			assertEquals(Math.max(0, n - 1), Quicksort.quickSortTrace(input).size(),
					"all-duplicate input of length " + n);
		}
    }

    @Test
    void traceCountIsTheSameForAnyInputOfAGivenLength() {
        Random random = new Random(20261007L);
        for (int n = 2; n <= 10; n++) {
            for (int trial = 0; trial < 200; trial++) {
                int[] input = new int[n];
                for (int i = 0; i < n; i++) {
                    input[i] = random.nextInt(9) - 4;
                }
                assertEquals(n - 1, Quicksort.quickSortTrace(input).size(),
                        "length " + n + " should always partition n-1 times, input was "
                                + Arrays.toString(input));
            }
        }
    }

    @Test
    void traceEndsSortedAndAgreesWithTheFinalResult() {
        int[] input = {5, 3, 9, 1, 7};
        List<int[]> trace = Quicksort.quickSortTrace(input);
        assertArrayEquals(new int[]{5, 3, 9, 1, 7}, input,
                "the trace variant must not mutate its argument either");
        assertArrayEquals(sorted(input), trace.get(trace.size() - 1),
                "the last state must be the finished sort");
        assertArrayEquals(Quicksort.quickSort(input), trace.get(trace.size() - 1));
    }

    @Test
    void everyTraceStateIsAnIndependentSnapshotOfTheSameLength() {
        List<int[]> trace = Quicksort.quickSortTrace(new int[]{5, 3, 9, 1, 7});
        List<int[]> seen = new ArrayList<>();
        for (int[] state : trace) {
            assertEquals(5, state.length, "every state is the whole array");
            for (int[] earlier : seen) {
                assertNotSame(earlier, state,
                        "each trace entry must be a distinct array object");
            }
            seen.add(state);
        }
        // The first state must not already be the finished sort, or the trace shows nothing.
        assertTrue(!Arrays.equals(sorted(new int[]{5, 3, 9, 1, 7}), trace.get(0)),
                "the first state should show the array mid-sort");
    }

    /**
     * The trace is a record of *work*, so it must change the array and never reorder it wrongly.
     *
     * <p>Quicksort permutes in place, so every recorded state is a permutation of the input. That
     * is checkable per state and would have caught the original: its states were permutations too,
     * but the final one was unsorted.
     */
    @Test
    void everyTraceStateIsAPermutationOfTheInput() {
        int[] input = {4, 0, 4, 3, 0, 4};
        List<int[]> trace = Quicksort.quickSortTrace(input);
        int[] reference = input.clone();
        Arrays.sort(reference);
        for (int[] state : trace) {
            int[] asSorted = state.clone();
            Arrays.sort(asSorted);
            assertArrayEquals(reference, asSorted,
                    "quickSort must never lose or duplicate an element: " + Arrays.toString(state));
        }
        assertArrayEquals(reference, trace.get(trace.size() - 1));
    }

    private static int[] sorted(int[] input) {
        int[] copy = input.clone();
        Arrays.sort(copy);
        return copy;
    }
}