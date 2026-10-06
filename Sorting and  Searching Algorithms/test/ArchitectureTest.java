import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Structural rules for where dialogs may live. Task 13's acceptance criteria, made executable.
 *
 * <p>These are assertions about <i>source text</i>, not behaviour, so most of them read the files
 * rather than the class files. That is deliberate: "no algorithm class calls {@code JOptionPane}" is
 * a fact about the source, and reflection cannot answer it — {@code JOptionPane.showInputDialog}
 * returns a {@code String} like any other call, so a compiled search class looks identical to a
 * dialog-driven one.
 *
 * <p>Comments are stripped before matching. Four of these classes carry javadoc explaining the very
 * defects being removed — {@link LinearSearch} quotes the deleted {@code JOptionPane.CANCEL_OPTION}
 * line, {@link JumpSearch} quotes the {@code StringTokenizer} NPE — and a grep that could not tell
 * documentation from code would fail on the explanation of the fix.
 *
 * <p><b>Reads from the working directory</b>, which Surefire sets to the Maven {@code basedir}. The
 * source tree is not on the classpath, so there is nowhere else to look. If the layout ever moves
 * (Task 17 renames this directory), these tests fail with a message naming the path they wanted —
 * which is the correct outcome: the rules should be re-pointed deliberately, not silently skipped.
 */
class ArchitectureTest {

    /** The seven algorithm/search classes. All should be pure: no fields, no dialogs, all static. */
    private static final List<String> ALGORITHMS = List.of(
            "BubbleSort", "InsertionSort", "SelectionSort", "MergeSort",
            "QuickSort", "LinearSearch", "JumpSearch");

    private static Path sourceDir() {
        Path dir = Path.of(System.getProperty("user.dir"), "Sorting and  Searching Algorithms", "src");
        if (!Files.isDirectory(dir)) {
            fail("cannot find the source tree at " + dir.toAbsolutePath()
                    + "\nuser.dir is " + System.getProperty("user.dir")
                    + "\nIf Task 17 moved the sources, repoint this test - do not delete it.",
                    new IOException("source tree not found"));
        }
        return dir;
    }

    private static String source(String fileName) {
        Path file = sourceDir().resolve(fileName);
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("cannot read " + file, e);
            throw new AssertionError("unreachable");
        }
    }

    /**
 * The file's code: comments removed, and the contents of string and char literals blanked.
 *
 * <p>Two jobs, both learned the hard way.
 *
 * <p>Comments, because {@link LinearSearch} quotes the {@code JOptionPane.CANCEL_OPTION} line it
 * deleted and {@link JumpSearch} quotes the {@code StringTokenizer} NPE. A matcher that cannot tell
 * documentation from code fails on the explanation of the fix.
 *
 * <p>Literals, because {@link #methodBody} counts braces to find a method's end, and a {@code "}"}
 * inside a string closed the body early — silently making the recursion check pass on recursive code.
 * That is a false negative in the one test guarding this task's central property, and it is one
 * plausible menu string away from happening for real.
 *
 * <p>Blanking rather than deleting keeps every other offset honest, and newlines inside literals are
 * preserved so the result still reads line by line. Not handled: {@code \\uXXXX} escapes, which Java
 * processes before lexing; none appear here.
 */
private static String codeOnly(String java) {
        StringBuilder out = new StringBuilder(java.length());
        int i = 0;
        boolean inString = false;
        boolean inChar = false;
        while (i < java.length()) {
            char c = java.charAt(i);

            if (inString || inChar) {
                if (c == '\\' && i + 1 < java.length()) {
                    out.append("  ");
                    i += 2;
                    continue;
                }
                boolean closes = (inString && c == '"') || (inChar && c == '\'');
                out.append(c == '\n' ? '\n' : ' ');
                if (closes) {
                    inString = false;
                    inChar = false;
                }
                i++;
                continue;
            }

            if (c == '"' || c == '\'') {
                if (c == '"') {
                    inString = true;
                } else {
                    inChar = true;
                }
                out.append(' ');
                i++;
                continue;
            }

            int blockStart = java.indexOf("/*", i);
            int lineStart = java.indexOf("//", i);
            if (blockStart < 0 && lineStart < 0) {
                out.append(java, i, java.length());
                break;
            }
            if (blockStart >= 0 && (lineStart < 0 || blockStart < lineStart)) {
                out.append(java, i, blockStart).append(' ');
                int end = java.indexOf("*/", blockStart + 2);
                if (end < 0) {
                    // Unterminated block comment: nothing after it can be trusted as code.
                    break;
                }
                i = end + 2;
            } else {
                out.append(java, i, lineStart).append('\n');
                int end = java.indexOf('\n', lineStart);
                i = end < 0 ? java.length() : end;
            }
        }
        return out.toString();
    }

    private static boolean mentions(String code, String token) {
        return code.contains(token);
    }

    // ------------------------------------------------------------------
    // Task 13's acceptance criteria
    // ------------------------------------------------------------------

    @Test
    void noAlgorithmClassCallsJOptionPane() {
        List<String> offenders = new ArrayList<>();
        for (String name : ALGORITHMS) {
            if (mentions(codeOnly(source(name + ".java")), "JOptionPane")) {
                offenders.add(name);
            }
        }
        assertEquals(List.of(), offenders,
                "these algorithm classes still construct dialogs themselves; Task 13 moves every"
                        + " JOptionPane call into Presenter");
    }

    @Test
    void noAlgorithmClassReferencesRunner() {
        List<String> offenders = new ArrayList<>();
        for (String name : ALGORITHMS) {
            if (mentions(codeOnly(source(name + ".java")), "Runner")) {
                offenders.add(name);
            }
        }
        assertEquals(List.of(), offenders,
                "these algorithm classes still reference Runner; an algorithm that knows about the"
                        + " app's entry point cannot be tested on its own");
    }

    /**
     * No algorithm class may name {@code Presenter} either.
     *
     * <p>This is the check that the {@code JOptionPane} one alone cannot make. {@code Presenter}'s
     * methods are all public, so an algorithm class can reopen the exact 2019 shape — a
     * {@code searching(int[])} that prompts in a loop — without ever naming {@code JOptionPane}, by
     * calling {@code Presenter.askSearchKey} instead. Every other test in this class would still pass:
     * no new fields, no {@code Runner} reference, {@code JOptionPane} still only in {@code Presenter}.
     *
     * <p>Found by review, not by a failing run: the rule it enforces was stated in the task ledger as
     * "the 2019 coupling cannot quietly return", which was true of the token and false of the coupling.
     */
    @Test
    void noAlgorithmClassReferencesPresenter() {
        List<String> offenders = new ArrayList<>();
        for (String name : ALGORITHMS) {
            if (mentions(codeOnly(source(name + ".java")), "Presenter")) {
                offenders.add(name);
            }
        }
        assertEquals(List.of(), offenders,
                "these algorithm classes reference Presenter; an algorithm that can prompt can be"
                        + " driven into a modal dialog again, which is what made it untestable");
    }

    /**
     * {@code Presenter} is the single owner of dialog construction.
     *
     * <p>Checked across the whole source tree rather than per class, so a new file that opens a
     * dialog is caught the day it is written rather than during the next consolidation.
     */
    @Test
    void presenterIsTheOnlyFileThatCallsJOptionPane() {
        List<String> users = new ArrayList<>();
        for (Path file : javaSources()) {
            String name = file.getFileName().toString();
            if (mentions(codeOnly(read(file)), "JOptionPane")) {
                users.add(name);
            }
        }
        assertEquals(List.of("Presenter.java"), users,
                "JOptionPane must appear in exactly one file. The list is sorted, so this message is"
                        + " stable across runs and filesystems");
    }

    /**
     * Every {@code .java} file under the source tree, sorted.
     *
     * <p>Recursive rather than single-directory, because Task 17 moves these into
     * {@code src/main/java} and a flat scan would silently match nothing there instead of failing.
     * Sorted because the assertion below compares the whole list, and filesystem order is not stable.
     */
    private static List<Path> javaSources() {
        try (var walk = Files.walk(sourceDir())) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            fail("cannot scan the source tree", e);
            throw new AssertionError("unreachable");
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("cannot read " + file, e);
            throw new AssertionError("unreachable");
        }
    }

    @Test
    void presenterExists() {
        assertTrue(Files.isRegularFile(sourceDir().resolve("Presenter.java")),
                "Task 13 creates Presenter.java to own every dialog");
    }

    // ------------------------------------------------------------------
    // The shape every algorithm class should have
    // ------------------------------------------------------------------

    /**
     * Every algorithm class is a namespace, not an object.
     *
     * <p>Private constructor plus all-static methods means nothing can be instantiated, which is what
     * stops the 2019 coupling returning: instantiation is what forced each algorithm to be welded to
     * a dialog. Applies to the two searches as well as the five sorts — the searches reached this
     * shape in Tasks 11 and 12 and Task 13 is what finishes the job.
     */
    @Test
    void everyAlgorithmClassIsUninstantiableAndStatic() {
        List<String> offenders = new ArrayList<>();
        for (String name : ALGORITHMS) {
            Class<?> type = load(name);
            if (!Modifier.isFinal(type.getModifiers())) {
                offenders.add(name + " is not final, so it can be subclassed");
            }
            for (var constructor : type.getDeclaredConstructors()) {
                if (!Modifier.isPrivate(constructor.getModifiers())) {
                    offenders.add(name + " has a non-private constructor; it must be uninstantiable");
                }
            }
            for (Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    offenders.add(name + "." + field.getName() + " is an instance field");
                }
            }
            for (Method method : type.getDeclaredMethods()) {
                if (method.isSynthetic()) {
                    continue;
                }
                if (!Modifier.isStatic(method.getModifiers())) {
                    offenders.add(name + "." + method.getName() + " is an instance method");
                }
            }
        }
        assertEquals(List.of(), offenders,
                "algorithm classes should be final, uninstantiable, and hold only static members");
    }

    /**
     * No algorithm class holds mutable static state.
     *
     * <p>Checks for a <i>non-final</i> field, which is the part of the rule reflection can see. It
     * does not prove the stronger claim: {@code static final} freezes the reference, so a
     * {@code private static final List} of mutable contents passes this. That is a known limit rather
     * than a claim being made — all seven classes currently declare zero fields, so a green run here
     * means nothing has been introduced yet, not that everything possible has been excluded.
     */
    @Test
    void noAlgorithmClassHoldsNonFinalStaticState() {
        List<String> offenders = new ArrayList<>();
        for (String name : ALGORITHMS) {
            for (Field field : load(name).getDeclaredFields()) {
                if (field.isSynthetic()) {
                    continue;
                }
                if (!Modifier.isFinal(field.getModifiers())) {
                    offenders.add(name + "." + field.getName()
                            + " is a non-final static; that is shared mutable state");
                }
            }
        }
        assertEquals(List.of(), offenders,
                "these should be stateless namespaces, so no field may be non-final");
    }

    // ------------------------------------------------------------------
    // Task 14: no loop in this app may advance by recursion
    // ------------------------------------------------------------------

    /**
     * No menu or search loop may call itself.
     *
     * <p>The 2019 {@code Menu()} ended with an unconditional {@code Menu()} and also called itself
     * from its {@code default} branch, so the enclosing {@code do/while}'s condition was dead: every
     * path recursed deeper or called {@code System.exit}. Each menu click cost a stack frame, and a
     * user navigating for a while grew the stack without bound.
     *
     * <p>Checked against source because this is not observable at runtime from outside. A recursive
     * menu and a looping one behave identically to a user, produce identical transcripts, and differ
     * only in stack depth — which is exactly the property a transcript assertion cannot see. The
     * previous GUI tests would all have passed against the recursive version.
     */
    @Test
    void noMenuOrSearchLoopCallsItself() {
        String code = codeOnly(source("Runner.java"));
        for (String name : List.of("Menu", "searchLinear", "searchJump", "askSearchKey")) {
            String body = methodBody(code, name);

            // Two ways this goes wrong, and both have happened here. A plain contains("Menu(") also matches
            // Presenter.askMenu(. And a lookbehind of (?<![\w$]) alone fixes that but then
            // matches Presenter.askSearchKey( inside askSearchKey. So: reject a preceding word
            // character (a longer identifier such as askMenu) and reject a preceding dot (a call
            // on some other object), while still catching an unqualified call and one qualified
            // with this class.
            Pattern selfCall = Pattern.compile(
                    "(?<![\\w$.])" + Pattern.quote(name) + "\\s*\\("
                            + "|(?<![\\w$])Runner\\s*\\.\\s*" + Pattern.quote(name) + "\\s*\\(");
            assertFalse(selfCall.matcher(body).find(),
                    name + "() calls itself; the menu must advance by looping, not by recursing"
                            + "\nfound in: " + body.strip());
        }
    }

    /**
     * {@code Menu()} is private.
     *
     * <p>Once the search dialogs stopped calling back into it, {@code GUI()} was the only caller, and
     * an entry point nobody outside the class can reach is easier to reason about. Verified by
     * reflection rather than by reading the modifier, so a later {@code public} is a test failure.
     */
    @Test
    void theMenuIsPrivate() {
        Method menu;
        try {
            menu = Runner.class.getDeclaredMethod("Menu");
        } catch (NoSuchMethodException e) {
            fail("Runner no longer has a Menu() method", e);
            throw new AssertionError("unreachable");
        }
        assertTrue(Modifier.isPrivate(menu.getModifiers()),
                "Menu() is internal to GUI()'s flow and must not be callable from outside");
    }

    /**
     * The body of {@code code}'s {@code name} method, by brace matching.
     *
     * <p>Safe only on text that has been through {@link #codeOnly}: literals are blanked there, so a
     * {@code "}"} cannot close the body early.
     *
     * <p>Finds the <i>declaration</i> rather than the first mention of the name. A plain
     * {@code indexOf} is wrong here because {@code GUI()} calls {@code Menu()} before {@code Menu()}
     * is declared, and matching on a leading {@code "void "} is wrong in the other direction — it
     * misses every method that returns something, which is how {@code askSearchKey} came to be
     * reported "not found". A mention is a declaration only when a brace comes before the next
     * semicolon; a call site is followed by a semicolon.
     */
    private static String methodBody(String code, String name) {
        Matcher mentions = Pattern
                .compile("(?<![\\w$])" + Pattern.quote(name) + "\\s*\\(")
                .matcher(code);
        while (mentions.find()) {
            int i = mentions.end();
            while (i < code.length() && code.charAt(i) != '{' && code.charAt(i) != ';') {
                i++;
            }
            if (i < code.length() && code.charAt(i) == '{') {
                int depth = 0;
                for (int j = i; j < code.length(); j++) {
                    char c = code.charAt(j);
                    if (c == '{') {
                        depth++;
                    } else if (c == '}' && --depth == 0) {
                        return code.substring(i + 1, j);
                    }
                }
                fail("unbalanced braces in " + name + "()");
            }
        }
        fail("no declaration of " + name + "() found in Runner.java");
        throw new AssertionError("unreachable");
    }

    // ------------------------------------------------------------------
    // Task 15: the digit validator exists in exactly one place
    // ------------------------------------------------------------------

    /**
     * Only {@code Validator} may inspect digits.
     *
     * <p>The triplication this task removed was literally three copies of a
     * {@code Character.isDigit} loop — {@code Runner.intOnly}, {@code QueueJava.isInteger} and
     * {@code CircularQueue.isInteger}. Forbidding the primitive is a sharper rule than naming the
     * methods, which a fourth copy could avoid by picking a new name, and it fails at the point of
     * the duplication rather than at the point of the symptom.
     *
     * <p>Those copies had already drifted: two of them accepted an empty string and threw on null,
     * and none of them range-checked, so both queue demos crashed on a large enough size. A test that
     * named the three methods would not have caught the drift either; one that forbids the primitive
     * cannot drift, because there is only one copy left.
     */
    @Test
    void onlyTheValidatorInspectsDigits() {
        List<String> offenders = new ArrayList<>();
        for (Path file : javaSources()) {
            if (file.getFileName().toString().equals("Validator.java")) {
                continue;
            }
            if (codeOnly(read(file)).contains("Character.isDigit")) {
                offenders.add(file.getFileName().toString());
            }
        }
        assertEquals(List.of(), offenders,
                "these files re-implement the digit check instead of calling Validator.isInt,"
                        + " which is how the three copies drifted apart in the first place");
    }

    /**
     * The same rule for the y/n answer check, which was duplicated too.
     *
     * <p>Not named in the Task 15 brief, which listed only the digit validator — but both queue
     * classes carried an identical private {@code isString}, the brief's own wording is "the
     * triplicated validators" plural, and {@link #onlyTheValidatorInspectsDigits()} would otherwise
     * leave half the duplication standing.
     */
    @Test
    void onlyTheValidatorInspectsLetters() {
        List<String> offenders = new ArrayList<>();
        for (Path file : javaSources()) {
            if (file.getFileName().toString().equals("Validator.java")) {
                continue;
            }
            if (codeOnly(read(file)).contains("Character.isLetter")) {
                offenders.add(file.getFileName().toString());
            }
        }
        assertEquals(List.of(), offenders,
                "these files re-implement the letter check instead of calling Validator.isLetters");
    }

    @Test
    void theValidatorExists() {
        assertTrue(Files.isRegularFile(sourceDir().resolve("Validator.java")),
                "Task 15 creates Validator.java as the single home for the digit check");
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            fail("cannot load " + name, e);
            throw new AssertionError("unreachable");
        }
    }
}