import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Runner#intOnly(String)}, the digit validator that every input dialog
 * in the app relies on.
 *
 * <p>Background: this method decides whether a dialog's text is acceptable. A
 * {@code JOptionPane.showInputDialog} returns {@code null} when the user presses Cancel and
 * an empty string when the user clears the field and presses OK. Both used to be treated as
 * valid input, which sent a null or empty string on to {@code Integer.parseInt} and killed
 * the app.
 */
class RunnerInputValidationTest {

    @Test
    void rejectsAnEmptyFieldInsteadOfTreatingItAsAValidNumber() {
        // The original bug: the digit loop never runs for "", so the method fell through
        // to "return true" and GUI() went on to Integer.parseInt("").
        assertFalse(Runner.intOnly(""), "an empty field must not be accepted as a number");
    }

    @Test
    void returnsFalseForNullSoCancellingADialogDoesNotThrow() {
        // showInputDialog returns null on Cancel. The original code called str.length()
        // on it and threw NullPointerException before the loop could run.
        assertFalse(Runner.intOnly(null), "Cancel (null) must be reported as invalid, not throw");
    }

    @Test
    void acceptsAPlainRunOfDigits() {
        assertTrue(Runner.intOnly("0"));
        assertTrue(Runner.intOnly("7"));
        assertTrue(Runner.intOnly("42"));
        assertTrue(Runner.intOnly("1000000"));
    }

    @Test
    void rejectsAnythingContainingANonDigit() {
        assertFalse(Runner.intOnly("12a"));
        assertFalse(Runner.intOnly("1 2"));
        assertFalse(Runner.intOnly("3.5"));
    }

    @Test
    void rejectsASignedNumberBecauseOnlyBareDigitsAreAccepted() {
        assertFalse(Runner.intOnly("-1"));
        assertFalse(Runner.intOnly("+1"));
    }

    @Test
    void acceptsTheDigitTwoWhichIsAlsoTheValueOfCancelOption() {
        // Pinned deliberately, because it is a trap: JOptionPane.CANCEL_OPTION is 2,
        // not -1. LinearSearch compares the parsed search value against it, so typing
        // "2" to search for the number 2 is treated as a cancel. See CODE_REVIEW.md.
        assertTrue(Runner.intOnly("2"));
    }

    @Test
    void rejectsARunOfDigitsTooLongForAnInt() {
        // intOnly's result is handed straight to Integer.parseInt at the element
        // prompt and in the menu switch, so a long enough run of digits would throw
        // NumberFormatException and end the session - the same shape as the
        // empty-field crash this class exists to prevent.
        assertFalse(Runner.intOnly("9999999999"),
                "10 digits can exceed Integer.MAX_VALUE and must be refused here");
        assertFalse(Runner.intOnly("99999999999"));
        assertFalse(Runner.intOnly("123456789012345678901234567890"));
    }

    @Test
    void stillAcceptsTheLargestIntSoTheGuardIsNotOverEager() {
        assertTrue(Runner.intOnly("2147483647"));
        assertTrue(Runner.intOnly("999999999"), "9 digits always fit in an int");
    }
}