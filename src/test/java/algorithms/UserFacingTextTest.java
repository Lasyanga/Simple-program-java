package algorithms;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Task 16's criterion, made executable: no user-visible string contains a known typo.
 *
 * <p>This reads source text rather than running the app. The strings live in three places that
 * cannot be reached by one test — {@code Presenter} for the Swing dialogs, and the two console demos
 * for everything else — and two of the three entrypoints call {@code System.exit(0)}, so they cannot
 * be driven in-process at all. Scanning the sources is the only place all of them are visible at
 * once.
 *
 * <p><b>Comments are stripped first.</b> Several files explain the very typos fixed here — this
 * class's own header quotes them, {@code Presenter} notes that {@code [5]Quick} was preserved on
 * purpose in Task 13, and Task 12's javadoc quotes the dropped {@code [element][interval]} hint. A
 * matcher that could not tell documentation from code would fail on the explanation of the fix.
 *
 * <p><b>What is deliberately not in here.</b> The list is typos and ungrammatical prompts a user has
 * to read in order to proceed. It is not a copy edit. The author's voice survives: "Welcome!!",
 * "Bye.. bye..", "Message from Cowboy", "Invalid Input..." and "Overflow Program terminated." are
 * all left alone, and so is "This program show diff." — a rewrite of that would be inventing intent
 * rather than correcting a typo, so it is a human's call. See Task 16 in {@code todo.md}.
 */
class UserFacingTextTest {

    /** Bad text, and what replaced it. Order is the order the fixes were made. */
    private static final Map<String, String> CORRECTIONS = new LinkedHashMap<>();

    static {
        // --- the two console demos -------------------------------------------------
        CORRECTIONS.put("Enter the size od queue: ", "Enter the size of queue: ");
        CORRECTIONS.put("4Exit", "4 Exit");
        CORRECTIONS.put("Please input the corresponding number of your choose:",
                "Please enter the number of your choice:");
        CORRECTIONS.put("Enter the element in queue: ", "Enter the element to enqueue: ");
        CORRECTIONS.put("Do you want insert other element?y/n: ",
                "Do you want to insert another element? (y/n): ");
        CORRECTIONS.put("Do you want delete again?y/n: ", "Do you want to delete again? (y/n): ");
        CORRECTIONS.put("Do you want peek again?y/n: ", "Do you want to peek again? (y/n): ");

        // --- the Swing dialogs -----------------------------------------------------
        // These two need a doubled backslash. The scan reads source TEXT, so it matches the two
        // characters "\" and "n" that appear in the file - not the newline that a single "\n"
        // means here. Writing one backslash made both keys unmatchable, so neither the typo nor its
        // replacement was ever found: two of the nine corrections were being checked by nothing.
        CORRECTIONS.put("\\nEnter the you want to Search:",
                "\\nEnter the value you want to search for:");
        CORRECTIONS.put("[5]Quick\\n", "[5]Quick Sort\\n");
    }

    // ------------------------------------------------------------------

    @Test
    void noKnownTypoSurvivesAnywhereInTheSources() {
        Map<String, List<String>> stillThere = new LinkedHashMap<>();
        for (String typo : CORRECTIONS.keySet()) {
            List<String> files = filesContaining(typo);
            if (!files.isEmpty()) {
                stillThere.put(typo, files);
            }
        }
        assertEquals(Map.of(), stillThere,
                "these strings are still in the source tree");
    }

    /**
     * Each replacement is actually present, so a fix cannot be "achieved" by deleting a prompt.
     *
     * <p>Weak on its own — it only checks that the text appears somewhere — but it is what stops the
     * absence check above from passing because a message was removed outright.
     */
    @Test
    void everyCorrectionIsActuallyInUse() {
        List<String> missing = new ArrayList<>();
        for (String replacement : CORRECTIONS.values()) {
            if (filesContaining(replacement).isEmpty()) {
                missing.add(replacement);
            }
        }
        assertEquals(List.of(), missing,
                "these corrected strings appear nowhere, so the typo was deleted rather than fixed");
    }

    /**
     * The menu label for option 5 matches the window title it opens.
     *
     * <p>Not a typo — an inconsistency. The menu read {@code [5]Quick} while the dialog it opened was
     * titled {@code Quick Sort}, so the only way to tell which sort you had picked was to read past
     * the menu. Every other option already named its window title.
     */
    @Test
    void everyMenuLabelNamesTheDialogItOpens() {
        String menu = textOf("Presenter.java");
        assertTrue(menu.contains("[5]Quick Sort"),
                "option 5's label must match its dialog title, 'Quick Sort'");
        for (int option = 1; option <= 4; option++) {
            assertTrue(menu.contains("[1]Bubble Sort") || menu.contains("[2]Insertion Sort")
                            || menu.contains("[3]Selection Sort") || menu.contains("[4]Merge Sort"),
                    "the four named sorts must keep their full names on the menu");
        }
    }

    // ------------------------------------------------------------------

    private static List<String> filesContaining(String needle) {
        List<String> hits = new ArrayList<>();
        for (Path file : sources()) {
            if (stripComments(read(file)).contains(needle)) {
                hits.add(file.getFileName().toString());
            }
        }
        return hits;
    }

    private static String textOf(String fileName) {
        return stripComments(read(sources().stream()
                .filter(p -> p.getFileName().toString().equals(fileName))
                .findFirst()
                .orElseThrow()));
    }

    private static List<Path> sources() {
        Path dir = Path.of(System.getProperty("user.dir"), "src", "main", "java", "algorithms");
        if (!Files.isDirectory(dir)) {
            fail("cannot find the source tree at " + dir.toAbsolutePath(), new IOException("no src"));
        }
        try (var walk = Files.walk(dir)) {
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

    /**
     * Drops comments, keeping string literals intact.
     *
     * <p>The opposite choice to {@code ArchitectureTest}'s scanner, which blanks literals because it
     * asserts about <i>code</i>. This test asserts about the <i>text inside</i> literals, so blanking
     * them would have made every Swing correction pass vacuously — the strings being looked for would
     * have been replaced by spaces before the search ran. The first red run caught exactly that.
     *
     * <p>Literal contents are still tracked, so a {@code //} or {@code /*} inside a string is not
     * mistaken for a comment. Not a Java parser, and does not handle {@code \}{@code uXXXX} escapes;
     * none appear here.
     */
    private static String stripComments(String java) {
        StringBuilder out = new StringBuilder(java.length());
        int i = 0;
        boolean inString = false;
        boolean inChar = false;
        while (i < java.length()) {
            char c = java.charAt(i);
            if (inString || inChar) {
                out.append(c);
                if (c == '\\' && i + 1 < java.length()) {
                    out.append(java.charAt(i + 1));
                    i += 2;
                    continue;
                }
                if ((inString && c == '"') || (inChar && c == '\'')) {
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
}
