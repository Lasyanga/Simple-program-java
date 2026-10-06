import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * End-to-end tests that drive the real {@link Runner} GUI with no human present.
 *
 * <p>These exist because every flow in this app sits behind a modal {@code JOptionPane}. Until now
 * that left Tasks 3 and 6-9 at "code complete" but unverified: a green {@code mvn test} said
 * nothing about whether a dialog appeared, showed the right text, or handed control back to the
 * menu. {@link GuiDriver} answers the dialogs; these tests assert on the transcript.
 *
 * <p>Each test forks a separate JVM, because the app calls {@code System.exit(0)} on menu option 9
 * and on Cancel. In-process that would take the test runner down with it.
 *
 * <p>Scope, stated plainly. This verifies which dialogs appeared, in what order, carrying what
 * text, and that the app reached the menu again afterwards. It reads a label's contents, so it
 * cannot judge whether that text is legible once rendered - that stays a human call. It also
 * cannot run without a display, so on a headless machine every test skips rather than fails.
 */
class GuiSessionTest {

    /** Generous: each run forks a JVM and waits on real windows. */
    private static final long TIMEOUT_SECONDS = 90;

    /** What one driven session produced: its transcript, and how many turns were scripted. */
    private record Session(String transcript, int turns) { }

    // ------------------------------------------------------------------
    // The four extracted sorts
    // ------------------------------------------------------------------

    @Test
    void bubbleSortShowsItsTraceAndReturnsToTheMenu() throws Exception {
        Session s = drive(then(enterArray("5 3 9 1 7"),
                sortThenExit("1", "Bubble Sort Process")));

        assertRanCleanly(s);
        assertContains(s, "Bubble Sort Process:",
                "option 1 must show its trace, which is what Task 6 extracted");
        assertShowsArrayTypedIn(s);
        assertMenuShownTwice(s);
    }

    @Test
    void insertionSortShowsItsTraceAndReturnsToTheMenu() throws Exception {
        Session s = drive(then(enterArray("5 3 9 1 7"),
                sortThenExit("2", "Insertion Sort Process")));

        assertRanCleanly(s);
        assertContains(s, "Insertion Sort Process:",
                "option 2 must show its trace, which is what Task 7 extracted");
        assertShowsArrayTypedIn(s);
        assertMenuShownTwice(s);
    }

    @Test
    void selectionSortShowsItsTraceAndReturnsToTheMenu() throws Exception {
        Session s = drive(then(enterArray("5 3 9 1 7"),
                sortThenExit("3", "Selection Sort Process")));

        assertRanCleanly(s);
        assertContains(s, "Selection Sort Process:",
                "option 3 must show its trace, which is what Task 8 extracted");
        assertShowsArrayTypedIn(s);
        assertMenuShownTwice(s);
    }

    @Test
    void mergeSortShowsItsTraceAndReturnsToTheMenu() throws Exception {
        Session s = drive(then(enterArray("5 3 9 1 7"),
                sortThenExit("4", "Merge Sort Process")));

        assertRanCleanly(s);
        assertContains(s, "Merge Sort Process:",
                "option 4 must show its trace, which is what Task 9 extracted");
        assertShowsArrayTypedIn(s);
        assertMenuShownTwice(s);
    }

    /**
     * The trace has to show the algorithm working, not just exist.
     *
     * <p>Every sort here reports the same final result, so asserting only on that would pass even
     * if all four displayed the input unchanged. This checks the intermediate states are present
     * and ordered - the deepest swaps first, the sorted array last - which is what distinguishes a
     * real trace from a placeholder.
     */
    @Test
    void theTraceShowsIntermediateStatesAndEndsSorted() throws Exception {
        Session s = drive(then(enterArray("5 3 9 1 7"),
                sortThenExit("1", "Bubble Sort Process")));

        assertRanCleanly(s);
        // Bubble sort records a state after each swap, so the first state is the array after the
        // leading 5 and 3 have traded places - not the array as typed, which the dialog shows on
        // its own "Unsorted element:" line.
        assertContains(s, "3   5   9   1   7",
                "the first recorded state should be the array after the first swap");
        assertContains(s, "1   3   5   7   9",
                "the last recorded state should be the finished sort");
        assertContains(s, "Sorted Element: [1, 3, 5, 7, 9]",
                "the dialog must report the sorted array, not 1..n");
    }

    // ------------------------------------------------------------------
    // Task 3's crash fixes, exercised through the GUI
    // ------------------------------------------------------------------

    /**
     * Empty input must be re-asked, at both the length prompt and the element prompt.
     *
     * <p>Before Task 3 an empty field was treated as valid input and handed to
     * {@code Integer.parseInt}, ending the session.
     */
    @Test
    void reAsksWhenTheFieldIsSubmittedEmpty() throws Exception {
        Session s = drive(new Plan(new String[]{
                "length of your array", "",
                "length of your array", "3",
                "Element[0]", "",
                "Element[0]", "7",
                "Element[1]", "2",
                "Element[2]", "9",
                "Length of your Array", "9"}));

        assertRanCleanly(s);
        assertDidNotCrash(s);
    }

    /**
     * A length of 0 must be refused.
     *
     * <p>The element-entry loop is a {@code do { do { ... } while } while (count < size)}, so its
     * body runs at least once whatever the length is. A length of 0 therefore demanded one element
     * and wrote it into a zero-length array - ArrayIndexOutOfBoundsException, session over.
     */
    @Test
    void rejectsAZeroLengthAndReAsks() throws Exception {
        Session s = drive(new Plan(new String[]{
                "length of your array", "0",
                "length of your array", "2",
                "Element[0]", "4",
                "Element[1]", "6",
                "Length of your Array", "9"}));

        assertRanCleanly(s);
        assertDidNotCrash(s);
        // The menu appears once here, not twice: this scenario ends by choosing Exit, so the
        // session never comes back to it. Reaching it at all is what proves the length prompt
        // recovered.
        assertMenuShown(s, 1);
    }

    /** Non-digits at the menu must re-ask rather than do nothing. */
    @Test
    void reAsksWhenTheMenuGetsNonDigits() throws Exception {
        Session s = drive(new Plan(new String[]{
                "length of your array", "2",
                "Element[0]", "8",
                "Element[1]", "3",
                "Length of your Array", "abc",
                "Length of your Array", "9"}));

        assertRanCleanly(s);
        assertMenuShownTwice(s);
    }

    /**
     * Cancelling at the first prompt must leave quietly.
     *
     * <p>{@code showInputDialog} returns null on Cancel, and that null used to go straight to the
     * digit validator. There is no screen above that prompt to go back to, so returning is the
     * only sensible outcome.
     */
    @Test
    void cancellingAtTheFirstPromptLeavesQuietly() throws Exception {
        Session s = drive(new Plan(new String[]{"length of your array", "!"}));

        assertRanCleanly(s);
        assertDidNotCrash(s);
    }

    /** Cancelling at the menu used to re-prompt forever, with no way out. */
    @Test
    void cancellingAtTheMenuEndsTheSessionRatherThanLoopingForever() throws Exception {
        Session s = drive(new Plan(new String[]{
                "length of your array", "2",
                "Element[0]", "8",
                "Element[1]", "3",
                "Length of your Array", "!"}));

        assertRanCleanly(s);
        assertDidNotCrash(s);
        assertEquals(4, s.turns(),
                "the prompt must appear once per screen and then the session must end");
    }

    /**
     * Many round trips through a search and back, with no degradation.
     *
     * <p>The brief's manual check — "navigate the menu 50+ times, including into searches and
     * back" — is automatable, so it is not left as a manual step. Each cycle picks option 6 and
     * cancels straight out, which is the path that used to nest: {@code Menu()} recursing into
     * {@code searchLinear} and back, once per click.
     *
     * <p><b>What this does and does not prove.</b> It proves the app survives repeated navigation and
     * that every cycle returns control to the menu — a hang, a lost dialog or a stack overflow would
     * fail it. It does <i>not</i> prove the stack stopped growing, because a hundred recursions would
     * not overflow a JVM stack. That is what {@code ArchitectureTest.noMenuOrSearchLoopCallsItself}
     * is for: recursion and looping are indistinguishable from a transcript, so the property is
     * checked against the source instead.
     */
    @Test
    void repeatedNavigationInAndOutOfASearchDoesNotDegrade() throws Exception {
        final int cycles = 12;
        List<String> steps = new ArrayList<>(List.of(
                "length of your array", "2",
                "Element[0]", "8",
                "Element[1]", "3"));
        for (int i = 0; i < cycles; i++) {
            steps.add("Length of your Array");
            steps.add("6");
            steps.add("Enter the value you want to search for");
            steps.add("!");
        }
        steps.add("Length of your Array");
        steps.add("9");

        Session s = drive(new Plan(steps.toArray(new String[0])));

        assertRanCleanly(s);
        assertDidNotCrash(s);
        // Three prompts to type the array, then two turns per cycle - the menu and the search
        // dialog - then the final menu for Exit.
        assertEquals(3 + (cycles * 2) + 1, s.turns(),
                "every cycle must reach the menu again");
        // Menus sit on every turn from the third onwards: one before each cycle, and one after
        // the last cancel to carry the Exit.
        assertMenuShown(s, cycles + 1);
    }

    // ------------------------------------------------------------------
    // Documented behaviour worth pinning
    // ------------------------------------------------------------------

    @Test
    void quickSortShowsItsTraceAndReturnsToTheMenu() throws Exception {
        Session s = drive(then(enterArray("5 3 9 1 7"),
                sortThenExit("5", "Quick Sort Process")));

        assertRanCleanly(s);
        assertContains(s, "Quick Sort Process:",
                "option 5 must show its trace, which is what Task 10 extracted");
        assertShowsArrayTypedIn(s);
        assertMenuShownTwice(s);
    }

    /**
     * The dialog must report the array, not {@code 1..n}.
     *
     * <p>This is the 2019 display bug, and it is the one assertion here that a "did a dialog open"
     * check would miss: the dialog opens, shows the right title, and hands control back - while
     * reporting "Sorted element: 1 2 3 4 5" for the input 5 3 9 1 7.
     */
    @Test
    void quickSortReportsTheSortedArrayNotACountdown() throws Exception {
        Session s = drive(then(enterArray("5 3 9 1 7"),
                sortThenExit("5", "Quick Sort Process")));

        assertRanCleanly(s);
        assertContains(s, "Sorted Element: [1, 3, 5, 7, 9]",
                "the dialog must report the sorted array");
        assertFalse(s.transcript().contains("Sorted Element: [1, 2, 3, 4, 5]"),
                "this is the literal string the old code produced from 1..n");
        assertFalse(s.transcript().contains("1 2 3 4 5"),
                "no dialog may report a 1..n countdown");
    }

    /**
     * Searching for the value {@code 2} must actually search.
     *
     * <p>This is the {@code CANCEL_OPTION}-is-2 bug, and it was only ever visible in the dialog. The
     * old code compared the parsed search value against {@code JOptionPane.CANCEL_OPTION}, which is
     * 2, so typing 2 returned to the menu without searching. A unit test on the search function
     * could never have caught it: the search was correct, the wiring was not.
     */
    @Test
    void searchingForTwoSearchesRatherThanCancelling() throws Exception {
        Session s = drive(new Plan(new String[]{
                "length of your array", "3",
                "Element[0]", "1",
                "Element[1]", "2",
                "Element[2]", "3",
                "Length of your Array", "6",
                "Enter the value you want to search for", "2",
                "2 is @ index: 1", "-",
                "Enter the value you want to search for", "!",
                "Length of your Array", "9"}));

        assertRanCleanly(s);
        assertContains(s, "2 is @ index: 1",
                "searching for 2 must report the match, not silently cancel");
    }

    /**
     * A value that is not in the array must say so, and every match must be listed.
     *
     * <p>Also pins the deliberate duplicate-key choice: searching 9 in {@code 9 1 9} reports both
     * indices, matching what the 2019 dialog did.
     */
    @Test
    void linearSearchReportsEveryMatchAndSaysWhenThereIsNone() throws Exception {
        Session s = drive(new Plan(new String[]{
                "length of your array", "3",
                "Element[0]", "9",
                "Element[1]", "1",
                "Element[2]", "9",
                "Length of your Array", "6",
                "Enter the value you want to search for", "9",
                "9 is @ index: 0 2", "-",
                "Enter the value you want to search for", "4",
                "is not found", "-",
                "Enter the value you want to search for", "!",
                "Length of your Array", "9"}));

        assertRanCleanly(s);
        assertContains(s, "9 is @ index: 0 2",
                "every matching index must be reported, not just the first");
        assertContains(s, "Element 4 is not found.",
                "an absent key must be reported plainly");
    }

    /**
     * Jump Search must actually search.
     *
     * <p>This option has never worked, in any build, since 2019. The dialog's loop condition called
     * {@code st.nextToken()} on a {@code StringTokenizer} that was declared and never assigned, so it
     * threw NPE on the first keystroke, and an empty {@code catch} swallowed it - the app appeared to
     * do nothing. So this asserts the option now reaches its search and reports an index.
     *
     * <p>It searches {@code Array.getsorted()}, so the dialog shows the array sorted regardless of
     * what was typed.
     *
     * <p><b>The index here is 2, and getting that wrong was instructive.</b> This test originally
     * expected {@code Element @ index: 3} for 17, having counted from the typed order
     * {@code 42 3 17 8}. The sorted copy is {@code 3 8 17 42}, so 17 is at index 2 and the app was
     * right. The wrong expectation passed anyway: the driver records a mismatch as
     * {@code "07 | !! MISMATCH expected a dialog containing [Element @ index: 3] | title=Jump Search"},
     * so {@code assertContains} found the expected fragment <i>inside the error message</i>. The test
     * was green because of the failure it should have failed on. {@link GuiDriver#isMarkerLine} now
     * catches embedded mismatches, and {@link #assertContains} ignores marker lines so that nothing
     * can be satisfied by one again.
     */
    @Test
    void jumpSearchReturnsAnIndexAndComesBackToTheMenu() throws Exception {
        Session s = drive(new Plan(new String[]{
                "length of your array", "4",
                "Element[0]", "42",
                "Element[1]", "3",
                "Element[2]", "17",
                "Element[3]", "8",
                "Length of your Array", "8",
                "Enter the value you want to search for", "17",
                "Element @ index: 2", "-",
                "Enter the value you want to search for", "!",
                "Length of your Array", "9"}));

        assertRanCleanly(s);
        assertContains(s, "Element @ index: 2",
                "option 8 must report the index of 17 in the sorted array 3 8 17 42");
        // The heading and the window title are separate arguments to Presenter.askSearchKey, so
        // ("Jump Search", "Unsorted", ...) compiles and shows a plausible dialog. Nothing else
        // catches the swap: the assertions above read the result line, which is the same either way.
        assertContains(s, "Sorted element:",
                "jump search must label the array Sorted, since it searches the sorted copy");
        assertDidNotCrash(s);
        assertMenuShownTwice(s);
    }

    /**
     * Cancelling from the jump search dialog must reach the menu, not loop forever.
     *
     * <p>Specific risk once the NPE was fixed: {@code intOnly} reports a null input as invalid, so a
     * Cancel that only re-asked would leave the user trapped. This is the same trap
     * {@link LinearSearch} had.
     */
    @Test
    void cancellingJumpSearchReturnsToTheMenu() throws Exception {
        Session s = drive(new Plan(new String[]{
                "length of your array", "3",
                "Element[0]", "5",
                "Element[1]", "6",
                "Element[2]", "7",
                "Length of your Array", "8",
                "Enter the value you want to search for", "!",
                "Length of your Array", "9"}));

        assertRanCleanly(s);
        assertDidNotCrash(s);
        assertEquals(7, s.turns(),
                "the prompt must appear once per screen, then Cancel must leave");
    }

    /** Option 7 is a stub. Pinning that down so nobody mistakes it for a working search. */
    @Test
    void exponentialSearchSaysItIsNotImplemented() throws Exception {
        Session s = drive(new Plan(new String[]{
                "length of your array", "3",
                "Element[0]", "1",
                "Element[1]", "2",
                "Element[2]", "3",
                "Length of your Array", "7",
                "not implemented", "-",
                "Length of your Array", "9"}));

        assertRanCleanly(s);
        assertContains(s, "Exponential Search is not implemented yet.",
                "option 7 must state plainly that it does nothing");
    }

    // ------------------------------------------------------------------
    // Script building
    // ------------------------------------------------------------------

    /** One scripted session: the prompts for typing in an array, then the menu steps. */
    private record Plan(String[] steps) { }

    /** The prompts for typing in an array, as {@code fragment, reply} pairs. */
    private static Plan enterArray(String elements) {
        String[] values = elements.split(" ");
        List<String> steps = new ArrayList<>();
        steps.add("length of your array");
        steps.add(String.valueOf(values.length));
        for (int i = 0; i < values.length; i++) {
            steps.add("Element[" + i + "]");
            steps.add(values[i]);
        }
        return new Plan(steps.toArray(new String[0]));
    }

    /** Choose {@code option} at the menu, dismiss the dialog it opens, then choose Exit. */
    private static Plan sortThenExit(String option, String dialogFragment) {
        return new Plan(new String[]{
                "Length of your Array", option,
                dialogFragment, "-",
                "Length of your Array", "9"
        });
    }

    private static Plan then(Plan first, Plan second) {
        String[] both = new String[first.steps().length + second.steps().length];
        System.arraycopy(first.steps(), 0, both, 0, first.steps().length);
        System.arraycopy(second.steps(), 0, both, first.steps().length, second.steps().length);
        return new Plan(both);
    }

    // ------------------------------------------------------------------
    // Driving the app
    // ------------------------------------------------------------------

    /**
     * Runs one scripted session and returns its transcript.
     *
     * <p>Each argument pair is {@code <text the dialog must contain>, <reply>}. {@code *} accepts
     * any dialog.
     */
    private static Session drive(Plan plan) throws Exception {
        String[] steps = plan.steps();
        requireDisplay();
        Path scenario = Files.createTempFile("gui-scenario", ".txt");
        Path transcript = Files.createTempFile("gui-transcript", ".txt");
        try {
            StringBuilder script = new StringBuilder();
            for (int i = 0; i < steps.length; i += 2) {
                script.append(steps[i]).append('\t').append(steps[i + 1]).append('\n');
            }
            Files.writeString(scenario, script.toString(), StandardCharsets.UTF_8);

            String java = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
            if (!Files.exists(Path.of(java))) {
                java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            }
            ProcessBuilder builder = new ProcessBuilder(
                    java,
                    "-cp", System.getProperty("java.class.path"),
                    "GuiDriver",
                    scenario.toString(),
                    transcript.toString());
            builder.redirectErrorStream(true);
            Process process = builder.start();

            String output = new String(process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                fail("the GUI session did not finish within " + TIMEOUT_SECONDS + "s"
                        + "\ndriver output:\n" + output);
            }
            if (!Files.exists(transcript)) {
                fail("the driver wrote no transcript (exit " + process.exitValue() + ")"
                        + "\ndriver output:\n" + output);
            }

            String text = Files.readString(transcript, StandardCharsets.UTF_8);
            return new Session(text, steps.length / 2);
        } finally {
            Files.deleteIfExists(scenario);
            Files.deleteIfExists(transcript);
        }
    }

    private static void requireDisplay() {
        assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "no display: modal dialogs cannot be driven headlessly");
    }

    // ------------------------------------------------------------------
    // Assertions over a transcript
    // ------------------------------------------------------------------

    /**
     * Every scripted turn arrived, matched, and was answered without incident.
     *
     * <p>Turn count is checked against the script rather than trusting the driver to announce
     * completion. That announcement is unreachable in the common case: the last turn of nearly
     * every scenario is menu option 9, the app calls {@code System.exit(0)}, and the shutdown hook
     * writes the transcript before the watcher's loop can record finishing. Asserting on the
     * marker would fail every session that worked perfectly.
     */
    private static void assertRanCleanly(Session session) {
        String transcript = session.transcript();
        long reached = transcript.lines().filter(line -> line.matches("\\d\\d \\| .*")).count();
        assertEquals(session.turns(), reached,
                "the session did not reach every scripted dialog"
                        + "\nfull transcript:\n" + transcript);

        List<String> complaints = transcript.lines()
                .filter(GuiDriver::isMarkerLine)
                .toList();
        if (!complaints.isEmpty()) {
            fail("the session did not go as scripted:\n" + String.join("\n", complaints)
                    + "\n\nfull transcript:\n" + transcript);
        }
    }

    /**
     * Asserts the transcript contains {@code needle}, ignoring the driver's failure markers.
     *
     * <p>The filtering is not cosmetic. A mismatch is recorded as
     * {@code "07 | !! MISMATCH expected a dialog containing [NEEDLE] | title=..."} — it echoes the
     * expected fragment back into the transcript. A plain {@code contains} would therefore find the
     * needle <i>inside the record of it not being there</i>, which is how
     * {@link #jumpSearchReturnsAnIndexAndComesBackToTheMenu} passed while asserting the wrong index
     * for a month of task work. Marker lines are the driver's complaints, never evidence.
     */
    private static void assertContains(Session session, String needle, String why) {
        String evidence = String.join("\n", session.transcript().lines()
                .filter(line -> !GuiDriver.isMarkerLine(line))
                .toList());
        assertTrue(evidence.contains(needle),
                why + "\nexpected to find: " + needle
                        + "\nfull transcript:\n" + session.transcript());
    }

    /**
     * The app must not have reached its catch-all.
     *
     * <p>Every exception in {@code GUI()} ends at the same dialog - {@code System.out.print(e)}
     * then "Bye.. bye..", which is the app's visible symptom of having died. Getting back to the
     * menu is what proves the flow survived.
     */
    private static void assertDidNotCrash(Session session) {
        assertFalse(session.transcript().contains("Bye.. bye.."),
                "the app fell into its catch-all, meaning an exception ended the session"
                        + "\nfull transcript:\n" + session.transcript());
    }

    /** The menu must appear twice, proving control returned rather than the app falling out. */
    private static void assertMenuShownTwice(Session session) {
        assertMenuShown(session, 2);
    }

    private static void assertMenuShown(Session session, int expected) {
        long menus = session.transcript().lines()
                .filter(line -> line.contains("title=Menu"))
                .count();
        assertEquals(expected, menus,
                "expected the menu " + expected + " time(s)"
                        + "\nfull transcript:\n" + session.transcript());
    }

    private static void assertShowsArrayTypedIn(Session session) {
        assertContains(session, "Unsorted element:  5 3 9 1 7",
                "the dialog must show the array that was typed in");
        assertContains(session, "Sorted Element: [1, 3, 5, 7, 9]",
                "the dialog must report the sorted array");
    }

    /**
     * Re-entering a search must not show the previous search's result.
     *
     * <p>The one behaviour change in Task 13, pinned deliberately. {@code position} used to be a
     * {@code static} field on {@code LinearSearch} and {@code JumpSearch}, so cancelling out of a
     * search and picking the same option again left the old result sitting above the input line —
     * a stale result for a search the user had not performed this time. It is a local now, so the
     * second visit starts blank.
     *
     * <p>Asserted positionally rather than with {@code assertFalse(contains(...))}: the first search's
     * result <i>is</i> in the transcript, and it must stay there. Only the second prompt may lack it.
     */
    @Test
    void reenteringLinearSearchDoesNotShowThePreviousResult() throws Exception {
        Session s = drive(new Plan(new String[]{
                "length of your array", "3",
                "Element[0]", "1",
                "Element[1]", "2",
                "Element[2]", "3",
                "Length of your Array", "6",
                "Enter the value you want to search for", "2",
                "2 is @ index: 1", "-",
                "Enter the value you want to search for", "!",
                "Length of your Array", "6",
                "Enter the value you want to search for", "!",
                "Length of your Array", "9"}));

        assertRanCleanly(s);
        assertDidNotCrash(s);

        // Driver turns are zero-based and each pair in the script is one turn. All eleven:
        //   0-3 enter the array, 4 menu, 5 first search, 6 result, 7 cancel, 8 menu,
        //   9 second search, 10 menu and Exit.
        assertTurnContains(s, 6, "2 is @ index: 1",
                "the first search's result must still be reported - otherwise the check below"
                        + " would pass simply because the result never appeared");
        assertTurnContains(s, 9, "Unsorted element:  1 2 3",
                "the search dialog must still show the array it is searching");
        assertTurnDoesNotContain(s, 9, "2 is @ index: 1",
                "a re-entered search must not inherit the previous search's result");
    }

    /** What {@code GuiDriver} puts in front of each recorded message line. */
    private static final String MESSAGE_PREFIX = "    | ";

    /** The message text of one scripted turn, as the driver recorded it. */
    private static String turnText(Session session, int turn) {
        StringBuilder out = new StringBuilder();
        boolean inside = false;
        for (String line : session.transcript().lines().toList()) {
            if (line.matches("\\d\\d \\| .*")) {
                inside = Integer.parseInt(line.substring(0, 2)) == turn;
                continue;
            }
            if (inside && line.startsWith(MESSAGE_PREFIX)) {
                out.append(line.substring(MESSAGE_PREFIX.length())).append('\n');
            }
        }
        return out.toString();
    }

    private static void assertTurnContains(Session session, int turn, String needle, String why) {
        String text = turnText(session, turn);
        assertTrue(text.contains(needle),
                why + "\nturn " + turn + " was:\n" + text + "\nfull transcript:\n"
                        + session.transcript());
    }

    private static void assertTurnDoesNotContain(Session session, int turn, String needle, String why) {
        String text = turnText(session, turn);
        assertFalse(text.contains(needle),
                why + "\nturn " + turn + " was:\n" + text + "\nfull transcript:\n"
                        + session.transcript());
    }
}
