package algorithms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Validator#isInt(String)}, the digit validator that every input dialog in the app
 * relies on.
 *
 * <p>Background: this method decides whether a dialog's text is acceptable. A
 * {@code JOptionPane.showInputDialog} returns {@code null} when the user presses Cancel and
 * an empty string when the user clears the field and presses OK. Both used to be treated as
 * valid input, which sent a null or empty string on to {@code Integer.parseInt} and killed
 * the app.
 *
 * <p><b>Renamed in Task 15, not moved logic.</b> These assertions used to run against
 * {@code Runner.intOnly}; the implementation now lives in {@link Validator} so the two queue demos
 * can share it. The expectations are unchanged — the same cases, still as strong. The class keeps
 * its name because these are the validator behaviours <i>the app's prompts</i> depend on;
 * {@link ValidatorTest} covers the function itself.
 */
class RunnerInputValidationTest {

    @Test
    void rejectsAnEmptyFieldInsteadOfTreatingItAsAValidNumber() {
        // The original bug: the digit loop never runs for "", so the method fell through
        // to "return true" and GUI() went on to Integer.parseInt("").
        assertFalse(Validator.isInt(""), "an empty field must not be accepted as a number");
    }

    @Test
    void returnsFalseForNullSoCancellingADialogDoesNotThrow() {
        // showInputDialog returns null on Cancel. The original code called str.length()
        // on it and threw NullPointerException before the loop could run.
        assertFalse(Validator.isInt(null), "Cancel (null) must be reported as invalid, not throw");
    }

    @Test
    void acceptsAPlainRunOfDigits() {
        assertTrue(Validator.isInt("0"));
        assertTrue(Validator.isInt("7"));
        assertTrue(Validator.isInt("42"));
        assertTrue(Validator.isInt("1000000"));
    }

    @Test
    void rejectsAnythingContainingANonDigit() {
        assertFalse(Validator.isInt("12a"));
        assertFalse(Validator.isInt("1 2"));
        assertFalse(Validator.isInt("3.5"));
    }

    @Test
    void rejectsASignedNumberBecauseOnlyBareDigitsAreAccepted() {
        assertFalse(Validator.isInt("-1"));
        assertFalse(Validator.isInt("+1"));
    }

    @Test
    void acceptsTheDigitTwoWhichIsAlsoTheValueOfCancelOption() {
        // Pinned deliberately, because it is a trap: JOptionPane.CANCEL_OPTION is 2,
        // not -1. LinearSearch compares the parsed search value against it, so typing
        // "2" to search for the number 2 is treated as a cancel. See CODE_REVIEW.md.
        assertTrue(Validator.isInt("2"));
    }

    @Test
    void rejectsARunOfDigitsTooLongForAnInt() {
        // Validator.isInt's result is handed straight to Integer.parseInt at the element
        // prompt and in the menu switch, so a long enough run of digits would throw
        // NumberFormatException and end the session - the same shape as the
        // empty-field crash this class exists to prevent.
        assertFalse(Validator.isInt("9999999999"),
                "10 digits can exceed Integer.MAX_VALUE and must be refused here");
        assertFalse(Validator.isInt("99999999999"));
        assertFalse(Validator.isInt("123456789012345678901234567890"));
    }

    @Test
    void stillAcceptsTheLargestIntSoTheGuardIsNotOverEager() {
        assertTrue(Validator.isInt("2147483647"));
        assertTrue(Validator.isInt("999999999"), "9 digits always fit in an int");
    }
}
