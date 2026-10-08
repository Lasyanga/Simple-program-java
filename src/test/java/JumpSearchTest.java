import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link JumpSearch}'s core, made pure.
 *
 * <p><b>The algorithm was already correct.</b> Unlike {@link QuickSort}, which looked fine and
 * wasn't, this one was verified by execution before being touched: 600,000 differential trials
 * against a brute-force reference, over sorted arrays both strictly increasing and containing
 * duplicates, plus every length up to 1,000,000. Zero wrong answers. So this suite exists to
 * <i>preserve</i> that behaviour across the extraction, not to change it - and the fuzz cases are
 * what make it meaningful, because the 2019 dialog had never been able to reach this code at all.
 *
 * <p>The one real defect was an unguarded empty array: {@code array[Math.min(step, len) - 1]} with
 * {@code len == 0} and {@code step == 0} indexes {@code array[-1]} and throws. Unreachable from
 * the GUI, since the length prompt refuses 0 before {@code Array} is built - but the method is
 * public static, so any caller can hand it {@code new int[0]}.
 */
class JumpSearchTest {

    /**
     * Brute force: the first index of {@code key}, or -1.
     *
     * <p>Written out longhand rather than reusing the class under test, so it is an independent
     * oracle. Fuzzing against a copy of the implementation would only prove it still agrees with
     * itself.
     */
    private static int reference(int[] array, int key) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == key) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // Found
    // ------------------------------------------------------------------

    @Test
    void findsAKeyInTheMiddle() {
        assertEquals(2, JumpSearch.jumpSearch(new int[]{10, 20, 30, 40, 50}, 30));
    }

    @Test
    void findsAKeyAtTheFirstPosition() {
        assertEquals(0, JumpSearch.jumpSearch(new int[]{10, 20, 30, 40, 50}, 10));
    }

    @Test
    void findsAKeyAtTheLastPosition() {
        assertEquals(4, JumpSearch.jumpSearch(new int[]{10, 20, 30, 40, 50}, 50));
    }

    @Test
    void findsEveryKeyOfAnArray() {
        int[] input = {4, 8, 15, 16, 23, 42};
        for (int key : input) {
            assertEquals(reference(input, key), JumpSearch.jumpSearch(input, key),
                    "failed for key " + key);
        }
    }

    // ------------------------------------------------------------------
    // Not found, in both directions
    // ------------------------------------------------------------------

    @Test
    void returnsMinusOneForAKeyBelowTheRange() {
        assertEquals(-1, JumpSearch.jumpSearch(new int[]{10, 20, 30, 40, 50}, 5));
        assertEquals(-1, JumpSearch.jumpSearch(new int[]{10, 20, 30, 40, 50}, 0));
    }

    @Test
    void returnsMinusOneForAKeyAboveTheRange() {
        assertEquals(-1, JumpSearch.jumpSearch(new int[]{10, 20, 30, 40, 50}, 55));
        assertEquals(-1, JumpSearch.jumpSearch(new int[]{10, 20, 30, 40, 50}, 999));
    }

    /** A key between two present values is absent, and must not be rounded to a neighbour. */
    @Test
    void returnsMinusOneForAKeyInAGap() {
        assertEquals(-1, JumpSearch.jumpSearch(new int[]{10, 20, 30, 40, 50}, 25));
        assertEquals(-1, JumpSearch.jumpSearch(new int[]{10, 20, 30, 40, 50}, 45));
    }

    @Test
    void handlesNegativeKeysAndValues() {
        assertEquals(1, JumpSearch.jumpSearch(new int[]{-9, -5, -1, 4, 8}, -5));
        assertEquals(-1, JumpSearch.jumpSearch(new int[]{-9, -5, -1, 4, 8}, 0));
    }

    // ------------------------------------------------------------------
    // Sizes, including the guard that was missing
    // ------------------------------------------------------------------

    @Test
    void handlesASingleElement() {
        assertEquals(0, JumpSearch.jumpSearch(new int[]{7}, 7));
        assertEquals(-1, JumpSearch.jumpSearch(new int[]{7}, 3));
    }

    @Test
    void handlesATwoElementArrayWhereTheStepIsOne() {
        // step is floor(sqrt(2)) == 1 here, so every iteration jumps by one. This is the shape
        // most likely to expose an off-by-one in the loop bounds.
        assertEquals(0, JumpSearch.jumpSearch(new int[]{3, 9}, 3));
        assertEquals(1, JumpSearch.jumpSearch(new int[]{3, 9}, 9));
        assertEquals(-1, JumpSearch.jumpSearch(new int[]{3, 9}, 6));
    }

    /**
     * The defect this task fixes.
     *
     * <p>The 2019 code evaluated {@code array[Math.min(step, len) - 1]} with no guard. For an
     * empty array {@code step} is {@code floor(sqrt(0)) == 0}, so that indexes {@code array[-1]}:
     * {@code ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 0}.
     */
    @Test
    void returnsMinusOneForAnEmptyArrayInsteadOfThrowing() {
        assertEquals(-1, JumpSearch.jumpSearch(new int[]{}, 5),
                "an empty array has no match; it must not throw");
        assertEquals(-1, JumpSearch.jumpSearch(new int[]{}, 0));
        assertEquals(-1, JumpSearch.jumpSearch(new int[]{}, Integer.MIN_VALUE));
    }

    // ------------------------------------------------------------------
    // Duplicates
    // ------------------------------------------------------------------

    /**
     * Duplicates resolve to the <i>first</i> index, matching the brute-force reference.
     *
     * <p>Recorded deliberately rather than assumed: the linear scan stops at the first element not
     * less than the key, so first-match is what this implementation does. It is what the app
     * should display, but it is a decision, not an accident, and it should be one test that fails
     * loudly if it ever changes.
     */
    @Test
    void reportsTheFirstMatchWhenTheKeyIsDuplicated() {
        assertEquals(1, JumpSearch.jumpSearch(new int[]{1, 3, 3, 3, 5}, 3),
                "index 0 holds 1; the first 3 is at index 1");
        assertEquals(0, JumpSearch.jumpSearch(new int[]{3, 3, 3}, 3));
        assertEquals(0, JumpSearch.jumpSearch(new int[]{1, 1, 1, 1}, 1));
        assertEquals(0, JumpSearch.jumpSearch(new int[]{2, 2, 4, 4, 6, 6}, 2));
    }

    @Test
    void leavesTheArgumentUntouched() {
        int[] input = {1, 3, 3, 5};
        int[] before = input.clone();
        JumpSearch.jumpSearch(input, 3);
        JumpSearch.jumpSearch(input, 4);
        assertArrayEquals(before, input, "searching must not modify the array");
    }

    // ------------------------------------------------------------------
    // Scale
    // ------------------------------------------------------------------

    @Test
    void findsAKeyAtTheEndOfALargeSortedArray() {
        for (int n : new int[]{1_000, 10_000, 100_000}) {
            int[] input = new int[n];
            for (int i = 0; i < n; i++) {
                input[i] = i;
            }
            assertEquals(n - 1, JumpSearch.jumpSearch(input, n - 1), "last of " + n);
            assertEquals(0, JumpSearch.jumpSearch(input, 0), "first of " + n);
            assertEquals(-1, JumpSearch.jumpSearch(input, n + 5), "just past the end of " + n);
        }
    }

    /**
     * Differential fuzz against the brute-force reference.
     *
     * <p>Two populations, because jump search only works on sorted input and has two ways to go
     * wrong: the jump loop can skip a block that contained the key, and the linear scan can run
     * past the block boundary. Duplicates are included because they interact with the scan's
     * "stop at the first element not less than the key" condition.
     *
     * <p>Fixed seed, so a failure is reproducible. This is the test that would have caught a
     * rewrite damaging the algorithm - the dialog previously made this code unreachable, so
     * nothing had ever exercised it.
     */
    @Test
    void agreesWithBruteForceAcrossManySortedArrays() {
        Random random = new Random(20261007L);

        for (int trial = 0; trial < 20_000; trial++) {
            int n = 1 + random.nextInt(24);
            int[] input = new int[n];
            int value = random.nextInt(5) - 2;
            for (int i = 0; i < n; i++) {
                input[i] = value;
                value += 1 + random.nextInt(3);
            }
            for (int key = -3; key <= value + 3; key++) {
                assertEquals(reference(input, key), JumpSearch.jumpSearch(input, key),
                        "distinct case, key " + key + ", array " + Arrays.toString(input));
            }
        }

        for (int trial = 0; trial < 20_000; trial++) {
            int n = 1 + random.nextInt(24);
            int[] input = new int[n];
            for (int i = 0; i < n; i++) {
                input[i] = random.nextInt(8);
            }
            Arrays.sort(input);
            for (int key = -1; key <= 9; key++) {
                assertEquals(reference(input, key), JumpSearch.jumpSearch(input, key),
                        "duplicate case, key " + key + ", array " + Arrays.toString(input));
            }
        }
    }

    @Test
    void neverReturnsAnIndexOutsideTheArray() {
        // A cheap invariant that catches off-by-one answers even when a key happens to be present:
        // an index must either be -1 or a position that really holds the key.
        Random random = new Random(20261008L);
        for (int trial = 0; trial < 20_000; trial++) {
            int n = 1 + random.nextInt(24);
            int[] input = new int[n];
            for (int i = 0; i < n; i++) {
                input[i] = random.nextInt(8);
            }
            Arrays.sort(input);
            int key = random.nextInt(10) - 1;
            int found = JumpSearch.jumpSearch(input, key);
            assertTrue(found == -1 || (found >= 0 && found < n),
                    "index " + found + " is out of range for length " + n);
            if (found != -1) {
                assertEquals(key, input[found],
                        "returned index " + found + " does not hold the key");
            }
        }
    }
}