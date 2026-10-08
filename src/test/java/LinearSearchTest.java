import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link LinearSearch}, the search this task had to <b>author</b> rather than extract.
 *
 * <p>{@code Searching(int)} was the entire implementation and it was impure three ways: it read
 * the static field {@code arr}, it mutated the static {@code position} string, and on a hit it
 * called {@code linearSearchGUI()} - a modal dialog - before returning. Its return value was
 * therefore never read by anything that could use it. There was no pure method hiding underneath
 * to lift out.
 *
 * <p>The duplicate-key decision is <b>report every match</b>, matching what the 2019 dialog did.
 * First-match-only would have been defensible and slightly tidier, but it would silently change
 * behaviour a user could see, and this task's brief says to make the choice deliberately. Both
 * forms are offered, so the choice is explicit rather than accidental:
 * {@link LinearSearch#linearSearch(int[], int)} for the first match and
 * {@link LinearSearch#linearSearchAll(int[], int)} for all of them.
 */
class LinearSearchTest {

    // ------------------------------------------------------------------
    // linearSearch: first match, or -1
    // ------------------------------------------------------------------

    @Test
    void findsAKeyInTheMiddle() {
        assertEquals(2, LinearSearch.linearSearch(new int[]{4, 8, 15, 16, 23, 42}, 15));
    }

    @Test
    void findsAKeyAtTheFirstPosition() {
        assertEquals(0, LinearSearch.linearSearch(new int[]{7, 8, 9}, 7));
    }

    @Test
    void findsAKeyAtTheLastPosition() {
        assertEquals(2, LinearSearch.linearSearch(new int[]{7, 8, 9}, 9));
    }

    @Test
    void findsAKeyInASingleElementArray() {
        assertEquals(0, LinearSearch.linearSearch(new int[]{5}, 5));
    }

    @Test
    void returnsMinusOneWhenTheKeyIsAbsent() {
        assertEquals(-1, LinearSearch.linearSearch(new int[]{1, 3, 5, 7}, 4));
    }

    @Test
    void returnsMinusOneForAnEmptyArray() {
        assertEquals(-1, LinearSearch.linearSearch(new int[]{}, 5));
    }

    /**
     * Duplicates resolve to the <i>first</i> match here.
     *
     * <p>Explicit, because the other method deliberately returns all of them. A reader who knows
     * one of these two behaviours should not have to check which.
     */
    @Test
    void returnsTheFirstMatchWhenTheKeyIsDuplicated() {
        assertEquals(0, LinearSearch.linearSearch(new int[]{4, 9, 4, 9, 4}, 4),
                "the 4s sit at 0, 2 and 4");
        assertEquals(1, LinearSearch.linearSearch(new int[]{4, 9, 4, 9, 4}, 9),
                "the 9s sit at 1 and 3");
        assertEquals(0, LinearSearch.linearSearch(new int[]{9, 4, 9}, 9));
    }

    @Test
    void searchesEveryPositionAndNotJustTheFirstFew() {
        // An off-by-one that stopped one element short would pass every test above but this one,
        // where the match is at the final index of a long array.
        int[] input = new int[500];
        for (int i = 0; i < input.length; i++) {
            input[i] = i * 3;
        }
        assertEquals(499, LinearSearch.linearSearch(input, 499 * 3));
        assertEquals(-1, LinearSearch.linearSearch(input, 499 * 3 + 1));
    }

    @Test
    void leavesTheArgumentUntouched() {
        int[] input = {4, 9, 4};
        LinearSearch.linearSearch(input, 9);
        LinearSearch.linearSearchAll(input, 9);
        assertArrayEquals(new int[]{4, 9, 4}, input,
                "searching must not reorder or modify the array");
    }

    @Test
    void handlesNegativeAndZeroValues() {
        assertEquals(1, LinearSearch.linearSearch(new int[]{-5, -1, 0, 3}, -1));
        assertEquals(2, LinearSearch.linearSearch(new int[]{-5, -1, 0, 3}, 0));
        assertEquals(-1, LinearSearch.linearSearch(new int[]{-5, -1, 0, 3}, 1));
    }

    @Test
    void handlesIntegerBounds() {
        // MIN_VALUE sorts below every other int, so a sign or overflow mistake in the comparison
        // shows up here first.
        assertEquals(2, LinearSearch.linearSearch(
                new int[]{0, Integer.MAX_VALUE, Integer.MIN_VALUE}, Integer.MIN_VALUE));
        assertEquals(0, LinearSearch.linearSearch(
                new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE}, Integer.MAX_VALUE));
        assertEquals(-1, LinearSearch.linearSearch(
                new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE}, 0));
    }

    // ------------------------------------------------------------------
    // linearSearchAll: every match, in ascending index order
    // ------------------------------------------------------------------

    @Test
    void reportsEveryMatchingIndexInOrder() {
        assertArrayEquals(new int[]{1, 3, 5},
                LinearSearch.linearSearchAll(new int[]{4, 9, 4, 9, 4, 9}, 9));
    }

    @Test
    void reportsASingleMatchAsASingleElementArray() {
        assertArrayEquals(new int[]{0},
                LinearSearch.linearSearchAll(new int[]{7, 8, 9}, 7));
    }

    @Test
    void reportsAnEmptyArrayWhenAbsent() {
        assertEquals(0, LinearSearch.linearSearchAll(new int[]{1, 2, 3}, 9).length);
    }

    @Test
    void reportsAnEmptyArrayForEmptyInput() {
        assertEquals(0, LinearSearch.linearSearchAll(new int[]{}, 1).length);
    }

    /**
     * Every occurrence of one key, including when the array is nothing but that key.
     *
     * <p>The all-duplicates case is where an implementation that stops early, or that reports
     * {@code first+1}, would be caught.
     */
    @Test
    void reportsEveryIndexWhenEveryElementMatches() {
        assertArrayEquals(new int[]{0, 1, 2, 3, 4},
                LinearSearch.linearSearchAll(new int[]{5, 5, 5, 5, 5}, 5));
    }

    @Test
    void theTwoFormsAgreeOnTheFirstMatch() {
        int[] input = {4, 9, 4, 9, 4, 9};
        int[] all = LinearSearch.linearSearchAll(input, 9);
        assertArrayEquals(new int[]{1, 3, 5}, all,
                "the 9s sit at 1, 3 and 5");
        assertEquals(all[0], LinearSearch.linearSearch(input, 9),
                "linearSearch must return the first element of linearSearchAll");
    }

    @Test
    void agreesWithAReferenceScanAcrossManyShapes() {
        int[][] cases = {
            {}, {5}, {1, 2, 3}, {3, 2, 1}, {1, 1, 1}, {1, 2, 1, 2, 1},
            {-3, 0, -3, 7, 0}, {10, -10, 10, -10}, {0}, {-1, -1, -1}
        };
        int[] keys = {1, 2, 3, 5, 7, 10, -1, -3, -10, 0};

        for (int[] input : cases) {
            for (int key : keys) {
                int[] expected = new int[input.length];
                int expectedCount = 0;
                for (int i = 0; i < input.length; i++) {
                    if (input[i] == key) {
                        expected[expectedCount++] = i;
                    }
                }
                int[] actual = LinearSearch.linearSearchAll(input, key);
                assertEquals(expectedCount, actual.length,
                        "match count for key " + key + " in " + java.util.Arrays.toString(input));
                for (int i = 0; i < expectedCount; i++) {
                    assertEquals(expected[i], actual[i],
                            "match order for key " + key + " in "
                                    + java.util.Arrays.toString(input));
                }
                assertEquals(expectedCount == 0 ? -1 : expected[0],
                        LinearSearch.linearSearch(input, key),
                        "first match for key " + key + " in "
                                + java.util.Arrays.toString(input));
            }
        }
    }

    @Test
    void findsAKeyAtTheVeryEndOfALargeArray() {
        int[] input = new int[10_000];
        for (int i = 0; i < input.length; i++) {
            input[i] = i + 1;
        }
        assertEquals(9_999, LinearSearch.linearSearch(input, 10_000));
        assertEquals(1, LinearSearch.linearSearchAll(input, 10_000).length);
    }

    // ------------------------------------------------------------------
    // The dialog bug: CANCEL_OPTION is 2, not -1
    // ------------------------------------------------------------------

    /**
     * Documents the constant that made searching for {@code 2} silently cancel.
     *
     * <p>Not a test of {@link LinearSearch} at all - of the JDK - but it is the cheapest possible
     * guard against someone "restoring" the old check. {@code JOptionPane.CANCEL_OPTION} is 2, so
     * {@code if (searched == JOptionPane.CANCEL_OPTION)} fires on ordinary input of the digit 2.
     * Cancellation is signalled by a null return from {@code showInputDialog}, never by a value.
     */
    @Test
    void cancelOptionIsTwoSoItMustNeverBeComparedAgainstAKey() {
        assertEquals(2, javax.swing.JOptionPane.CANCEL_OPTION,
                "this is the trap: the value 2 collides with CANCEL_OPTION");
        assertEquals(-1, javax.swing.JOptionPane.CLOSED_OPTION,
                "-1 is CLOSED_OPTION, not CANCEL_OPTION");

        // The behaviour that bug produced: search for 2, get nothing back.
        int[] input = {1, 2, 3};
        int searched = Integer.parseInt("2");
        boolean wouldHaveTreatedInputAsCancel = searched == javax.swing.JOptionPane.CANCEL_OPTION;
        assertTrue(wouldHaveTreatedInputAsCancel,
                "if this ever becomes false the constant changed and the old check would be safe;"
                        + " the point is that it currently is not");

        // What the app does now, which has no such comparison at all.
        assertEquals(1, LinearSearch.linearSearch(input, searched),
                "searching for 2 must actually search for 2");
    }
}