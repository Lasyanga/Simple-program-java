package algorithms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Runner#isValidLength(String)}, the guard on the array-length prompt.
 *
 * <p>Background: the element-entry loop in {@code GUI()} is a
 * {@code do { do { ... } while (...) } while (count &lt; size)}, so its body runs at least
 * once no matter what {@code size} is. With a length of {@code 0} the app therefore demanded
 * one element and wrote it into a zero-length array, throwing
 * {@code ArrayIndexOutOfBoundsException} and ending the session. A length of {@code 0} has to
 * be refused up front instead.
 */
class RunnerLengthValidationTest {

    @Test
    void acceptsAPositiveLength() {
        assertTrue(Runner.isValidLength("1"));
        assertTrue(Runner.isValidLength("5"));
        assertTrue(Runner.isValidLength("100"));
    }

    @Test
    void rejectsZeroLengthBecauseTheEntryLoopAlwaysDemandsAnElement() {
        assertFalse(Runner.isValidLength("0"),
                "a zero-length array cannot hold the element GUI() insists on entering");
    }

    @Test
    void rejectsAnEmptyLength() {
        assertFalse(Runner.isValidLength(""));
    }

    @Test
    void rejectsNullSoCancellingTheLengthPromptIsNotAnError() {
        assertFalse(Runner.isValidLength(null));
    }

    @Test
    void rejectsANonNumericLength() {
        assertFalse(Runner.isValidLength("ten"));
        assertFalse(Runner.isValidLength("1.5"));
        assertFalse(Runner.isValidLength("-3"));
    }

    @Test
    void rejectsALengthTooLargeForAnIntegerRatherThanThrowing() {
        // Integer.parseInt throws on overflow. Left unhandled, a long enough run of
        // digits would reach that call and kill the app the same way an empty field did.
        assertFalse(Runner.isValidLength("99999999999999999999"));
    }
}
