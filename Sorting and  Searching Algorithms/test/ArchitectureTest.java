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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            "Quicksort", "LinearSearch", "JumpSearch");

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
     * Removes block and line comments, leaving code and string literals.
     *
     * <p>Deliberately crude and deliberately not a Java parser. It only has to be good enough that a
     * javadoc sentence mentioning {@code JOptionPane} does not read as a call to it, and that a
     * {@code "//} or {@code /*} inside a string literal is not mistaken for a comment — which matters
     * more than it used to, now that {@code Presenter} holds every user-visible string in the project
     * and is the most likely place for help text containing a URL or a path to appear.
     *
     * <p>String literals are tracked so their contents are never scanned for comment markers. What is
     * still not handled: escapes inside a literal, so {@code "\\"} in a string ends the literal early.
     * No file here contains one, and the failure mode is a scan that finds slightly too much rather
     * than too little.
     */
    private static String stripComments(String java) {
        StringBuilder out = new StringBuilder(java.length());
        int i = 0;
        boolean inString = false;
        while (i < java.length()) {
            char c = java.charAt(i);

            if (inString) {
                out.append(c);
                if (c == '\\' && i + 1 < java.length()) {
                    // Keep the escaped character with its backslash so the literal does not end here.
                    out.append(java.charAt(i + 1));
                    i += 2;
                    continue;
                }
                if (c == '"') {
                    inString = false;
                }
                i++;
                continue;
            }

            if (c == '"') {
                inString = true;
                out.append(c);
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
            if (mentions(stripComments(source(name + ".java")), "JOptionPane")) {
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
            if (mentions(stripComments(source(name + ".java")), "Runner")) {
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
            if (mentions(stripComments(source(name + ".java")), "Presenter")) {
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
            if (mentions(stripComments(read(file)), "JOptionPane")) {
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

    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            fail("cannot load " + name, e);
            throw new AssertionError("unreachable");
        }
    }
}