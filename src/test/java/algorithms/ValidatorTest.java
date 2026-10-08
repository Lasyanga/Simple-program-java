package algorithms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Validator}, the one digit validator the project now has.
 *
 * <p>There were three copies of this function: {@code Runner.intOnly}, {@code QueueJava.isInteger}
 * and {@code CircularQueue.isInteger}. They were <b>not</b> the same function, and Task 15 collapsed
 * them onto the strongest of the three. The differences were measured before any code was touched:
 *
 * <table>
 *   <caption>what the copies did</caption>
 *   <tr><th>input</th><th>{@code intOnly}</th><th>the two {@code isInteger}</th></tr>
 *   <tr><td>{@code ""}</td><td>false</td><td><b>true</b> — the loop never ran</td></tr>
 *   <tr><td>{@code null}</td><td>false</td><td><b>NullPointerException</b></td></tr>
 *   <tr><td>{@code "2147483648"}</td><td>false</td><td><b>true</b> — no range check at all</td></tr>
 * </table>
 *
 * <p>Only the third difference is reachable by a user, and it was a crash: both queue demos read
 * their size with {@code Scanner.next()}, accepted any run of digits, and then handed the value to
 * {@code Integer.parseInt}. Typing {@code 99999999999} produced
 * {@code NumberFormatException: For input string: "99999999999"}. This is the same overflow defect
 * Task 3 fixed in the sorting app, never carried across to the queues.
 *
 * <p>The first two are unreachable today because {@code Scanner.next()} skips whitespace and never
 * returns {@code ""} or {@code null} — but they are one refactor away from mattering, and a validator
 * that would NPE on null is a trap regardless of who calls it.
 */
class ValidatorTest {

    // ------------------------------------------------------------------
    // Accepted
    // ------------------------------------------------------------------

    @Test
    void acceptsAPlainDigit() {
        assertTrue(Validator.isInt("5"));
    }

    @Test
    void acceptsZero() {
        // Zero is a legitimate value here. Whether it is a *legal* queue size is a separate rule,
        // which is why isValidLength stayed in Runner rather than moving in here.
        assertTrue(Validator.isInt("0"));
    }

    @Test
    void acceptsMultipleDigits() {
        assertTrue(Validator.isInt("42"));
        assertTrue(Validator.isInt("1000000"));
    }

    @Test
    void acceptsTheLargestRepresentableInt() {
        assertTrue(Validator.isInt("2147483647"),
                "Integer.MAX_VALUE is representable, so it must be accepted");
    }

    /**
     * Leading zeros do not change the value, so they do not overflow either.
     *
     * <p>Twenty-two zeros followed by a five is 23 characters long, which is exactly the shape that
     * made the old {@code isInteger} copies say yes. {@code Integer.parseInt} reads it as 5.
     */
    @Test
    void acceptsLeadingZerosHoweverLongTheRun() {
        assertTrue(Validator.isInt("007"));
        assertTrue(Validator.isInt("0000000000000000000005"),
                "a long run of zeros is still the number 5 and must not be refused for length");
    }

    // ------------------------------------------------------------------
    // Refused
    // ------------------------------------------------------------------

    @Test
    void refusesNullWithoutThrowing() {
        // Cancel hands a null to every one of these prompts. The queue copies would have thrown.
        assertFalse(Validator.isInt(null),
                "null means the user cancelled; it must be reported as invalid, not throw");
    }

    @Test
    void refusesTheEmptyString() {
        assertFalse(Validator.isInt(""),
                "an empty field is not a number; the old queue copies accepted it as one");
    }

    @Test
    void refusesANegativeOrSignedValue() {
        assertFalse(Validator.isInt("-1"));
        assertFalse(Validator.isInt("+1"));
    }

    @Test
    void refusesAnythingWithWhitespaceOrPunctuation() {
        assertFalse(Validator.isInt(" 5"));
        assertFalse(Validator.isInt("5 "));
        assertFalse(Validator.isInt("1 2"));
        assertFalse(Validator.isInt("1.5"));
        assertFalse(Validator.isInt("12a"));
        assertFalse(Validator.isInt("abc"));
    }

    /**
     * The defect this task fixes in the two queue demos.
     *
     * <p>Accepting these is what let {@code Integer.parseInt} throw at the size prompt. Every caller
     * in this project hands the result straight to {@code parseInt}, so refusing here is what stops
     * the throw.
     */
    @Test
    void refusesDigitsTooLongForAnInt() {
        assertFalse(Validator.isInt("9999999999"),
                "ten digits: over the int maximum, and it used to crash the queue demos");
        assertFalse(Validator.isInt("2147483648"),
                "one past Integer.MAX_VALUE");
        assertFalse(Validator.isInt("99999999999999999999999"));
        assertFalse(Validator.isInt("123456789012345678901234567890"));
    }

    @Test
    void acceptsNineDigitsSinceTheyAlwaysFit() {
        // The neighbouring boundary, so the refusal above is about range and not about length.
        assertTrue(Validator.isInt("999999999"));
    }

    @Test
    void refusesTextThatIsOnlyWhitespace() {
        assertFalse(Validator.isInt(" "));
        assertFalse(Validator.isInt("\t"));
    }

    // ------------------------------------------------------------------
    // isLetters: the y/n answers, which were also duplicated
    // ------------------------------------------------------------------
    //
    // Beyond the task brief, which named only the digit validator. Both queue classes carried an
    // identical private isString — a Character.isLetter loop — and the brief's own wording is
    // "the triplicated validators", plural. Collapsing these is behaviour-preserving, which the
    // digit collapse was not, so there is nothing to weigh here beyond whether to touch it.

    @Test
    void acceptsLettersOnly() {
        assertTrue(Validator.isLetters("y"));
        assertTrue(Validator.isLetters("Y"));
        assertTrue(Validator.isLetters("yes"));
    }

    /**
     * Pinned deliberately: an empty answer is accepted.
     *
     * <p>Both copies returned true for {@code ""}, because their loop never ran. This is recorded as
     * existing behaviour rather than fixed, so a future reader does not mistake it for an oversight
     * or "fix" it without deciding what the caller should do instead.
     */
    @Test
    void acceptsTheEmptyStringAsItAlwaysHas() {
        assertTrue(Validator.isLetters(""),
                "both 2019 copies returned true for an empty field; kept rather than quietly changed");
    }

    @Test
    void refusesAnythingThatIsNotLetters() {
        assertFalse(Validator.isLetters("n1"));
        assertFalse(Validator.isLetters("yes please"));
        assertFalse(Validator.isLetters("5"));
        assertFalse(Validator.isLetters("y "));
    }

    /**
     * These two copies threw on null; this one does not.
     *
     * <p>Unreachable today — the callers pass {@code Scanner.next()}, which never returns null —
     * but a validator that NPEs on its argument is a trap for whoever calls it next.
     */
    @Test
    void lettersRefusesNullWithoutThrowing() {
        assertFalse(Validator.isLetters(null));
    }
}
