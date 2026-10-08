package algorithms;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exponential search: find a range by doubling, then binary search within it.
 *
 * <p>Differential testing against a brute-force oracle, same shape as the other search suites.
 * The oracle is {@code indexOf} on a linear scan - deliberately dumb, so a bug in the algorithm
 * cannot hide behind a bug in the reference.
 *
 * <p>Requires a <b>sorted</b> array, same precondition as Jump Search. Exponential search exists
 * precisely because it avoids scanning the front of a long sorted array.
 */
class ExponentialSearchTest {

    // ------------------------------------------------------------------
    // The oracle: simplest possible correct implementation
    // ------------------------------------------------------------------

    /** First index of {@code target}, or -1. */
    private static int bruteForce(int[] arr, int target) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == target) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // Hand-picked shapes
    // ------------------------------------------------------------------

    @Test
    void emptyArrayReturnsMinusOne() {
        assertEquals(-1, ExponentialSearch.exponentialSearch(new int[0], 42));
    }

    @Test
    void singleElementFound() {
        assertEquals(0, ExponentialSearch.exponentialSearch(new int[]{7}, 7));
    }

    @Test
    void singleElementMissing() {
        assertEquals(-1, ExponentialSearch.exponentialSearch(new int[]{7}, 3));
    }

    @Test
    void foundAtFirstPosition() {
        // The case exponential search is worst at: it still doubles from index 1.
        assertEquals(0, ExponentialSearch.exponentialSearch(new int[]{1, 2, 3, 4, 5}, 1));
    }

    @Test
    void foundAtLastPosition() {
        assertEquals(4, ExponentialSearch.exponentialSearch(new int[]{1, 2, 3, 4, 5}, 5));
    }

    @Test
    void foundInTheMiddle() {
        assertEquals(2, ExponentialSearch.exponentialSearch(new int[]{1, 3, 5, 7, 9}, 5));
    }

    @Test
    void missingBelowRange() {
        assertEquals(-1, ExponentialSearch.exponentialSearch(new int[]{10, 20, 30}, 5));
    }

    @Test
    void missingAboveRange() {
        assertEquals(-1, ExponentialSearch.exponentialSearch(new int[]{10, 20, 30}, 40));
    }

    @Test
    void missingInsideRange() {
        assertEquals(-1, ExponentialSearch.exponentialSearch(new int[]{10, 20, 30, 40}, 25));
    }

    @Test
    void duplicateValuesReturnTheFirst() {
        int[] arr = {1, 2, 2, 2, 3};
        assertEquals(1, ExponentialSearch.exponentialSearch(arr, 2),
                "duplicates should resolve to the first index, matching linear search");
    }

    // ------------------------------------------------------------------
    // Differential fuzzing against the oracle
    // ------------------------------------------------------------------

    /**
     * 600,000 trials on sorted arrays with duplicates, negatives, and varied sizes.
     *
     * <p>Fixed seed: a failure must be reproducible, not a lottery ticket.
     */
    @Test
    void matchesTheOracleOnSixHundredThousandSortedArrays() {
        Random rng = new Random(20261008L);
        for (int trial = 0; trial < 600_000; trial++) {
            int size = rng.nextInt(0, 60);
            int[] arr = new int[size];
            for (int i = 0; i < size; i++) {
                // Small range forces duplicates; negatives exercise the sign handling.
                arr[i] = rng.nextInt(-10, 40);
            }
            Arrays.sort(arr);

            int target = rng.nextInt(-12, 42);
            int expected = bruteForce(arr, target);
            int actual = ExponentialSearch.exponentialSearch(arr, target);

            assertEquals(expected, actual,
                    "trial " + trial + ": searching " + target + " in " + Arrays.toString(arr));
        }
    }

    /**
     * The same, but always asking for a value that <b>is</b> present.
     *
     * <p>Split out because the random trials above hit a present value only about a third of the
     * time, and the found-path is where the doubling bound can go wrong.
     */
    @Test
    void matchesTheOracleWhenTheTargetIsAlwaysPresent() {
        Random rng = new Random(4242L);
        for (int trial = 0; trial < 200_000; trial++) {
            int size = 1 + rng.nextInt(50);
            int[] arr = new int[size];
            for (int i = 0; i < size; i++) {
                arr[i] = rng.nextInt(-5, 25);
            }
            Arrays.sort(arr);

            int target = arr[rng.nextInt(size)];
            assertEquals(bruteForce(arr, target), ExponentialSearch.exponentialSearch(arr, target),
                    "trial " + trial + ": searching " + target + " in " + Arrays.toString(arr));
        }
    }

    // ------------------------------------------------------------------
    // Contract: it must not touch the array it is given
    // ------------------------------------------------------------------

    @Test
    void doesNotModifyTheInput() {
        int[] arr = {1, 3, 5, 7, 9, 11, 13};
        int[] copy = arr.clone();
        ExponentialSearch.exponentialSearch(arr, 7);
        assertTrue(Arrays.equals(copy, arr), "the search must not sort or rearrange its input");
    }

    // ------------------------------------------------------------------
    // Large array: exponential search's whole reason to exist
    // ------------------------------------------------------------------

    @Test
    void handlesAnArrayLargerThanTheDoublingBound() {
        // 1,000,000 elements. Binary search alone needs 20 probes; exponential search finds the
        // range in ~10 doublings then binary-searches it - but critically it never scans from 0.
        int[] arr = new int[1_000_000];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = i * 2;
        }
        assertEquals(999_999, ExponentialSearch.exponentialSearch(arr, 1_999_998));
        assertEquals(0, ExponentialSearch.exponentialSearch(arr, 0));
        assertEquals(-1, ExponentialSearch.exponentialSearch(arr, 1_999_999));
    }

    @Test
    void returnsAllMatchesWhenAskedLikeLinearSearchDoes() {
        // The other searches expose both a first-match and an all-matches view. Keep the shape
        // consistent so Runner does not need special-casing per algorithm.
        int[] arr = {1, 2, 2, 2, 3};
        List<Integer> all = ExponentialSearch.exponentialSearchAll(arr, 2);
        assertEquals(List.of(1, 2, 3), all);
    }

    @Test
    void allMatchesIsEmptyWhenTheTargetIsMissing() {
        assertEquals(List.of(), ExponentialSearch.exponentialSearchAll(new int[]{1, 3, 5}, 2));
    }
}
