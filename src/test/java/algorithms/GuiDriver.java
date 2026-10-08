package algorithms;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Drives the real {@link Runner} GUI through a scripted sequence of dialogs, with no human.
 *
 * <p>Why this exists: every flow in this app lives inside a modal {@code JOptionPane}, so
 * {@code mvn test} could never reach them. That left Tasks 3 and 6-9 "code complete" but
 * unverified, because only a person clicking through could confirm a dialog appeared, showed
 * the right text, and handed control back to the menu.
 *
 * <p>How it works: {@code JOptionPane} dialogs are ordinary {@link JDialog} windows, so a
 * watcher thread can enumerate {@link Window#getWindows()}, find the one showing, read the text
 * out of its labels, and answer it - typing into the text field and clicking OK, or clicking
 * Cancel. Nothing is mocked and no code under test changes; the app runs exactly as
 * {@code main()} starts it.
 *
 * <p>What it does and does not prove. It proves which dialogs appeared, in what order, with
 * what text, and that the app returned to the menu afterwards. It does not prove the text is
 * legible on screen - a label's contents are read, not rendered. Judging "does this look
 * readable" stays a human judgement; the automation checks the substance.
 *
 * <p>Usage: {@code java -cp target/classes;target/test-classes GuiDriver <scenario> <transcript>}
 * where the scenario file has one turn per line, {@code <fragment><TAB><reply>}. The fragment is
 * text the dialog must contain, or {@code *} to accept any. The reply is the text to type, or
 * {@code -} to just click OK, or {@code !} to click Cancel.
 *
 * <p>The transcript is written from a shutdown hook, so it survives the {@code System.exit(0)}
 * that menu option 9 - and cancelling at the first prompt - both trigger.
 */
public class GuiDriver {

    /** One scripted dialog: what we expect to read, and how to answer. */
    private record Turn(String fragment, String reply) { }

    /**
     * Whether a transcript line is one of this driver's own complaints.
     *
     * <p>Position is the test, not the mere presence of {@code !!}: a captured dialog can
     * legitimately contain it - the welcome screen reads "Welcome!!" - and searching for the
     * substring anywhere would flag every session as a failure.
     *
     * <p>The middle case is a mismatch, and it used to be missed. {@code handle} records it embedded
     * in the turn header - {@code "04 | !! MISMATCH ... | title=Menu"} - so it started with a digit,
     * and neither {@code startsWith} matched. Every call to {@code assertRanCleanly} claimed the
     * session "arrived, matched, and was answered without incident" while checking only arrival and
     * count. That silently disarms any assertion made by turn index: if a dialog appeared, vanished,
     * or duplicated earlier in the session, the indices shift and the assertion inspects a different
     * dialog while still passing.
     */
    static boolean isMarkerLine(String line) {
        return line.startsWith("!! ") || line.startsWith("    !! ")
                || line.contains(" | !! ");
    }

    private static final List<Turn> SCRIPT = new ArrayList<>();
    private static final List<String> TRANSCRIPT = new ArrayList<>();
    private static final Set<Window> HANDLED =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private static Path transcriptPath;
    private static volatile boolean finished;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("usage: GuiDriver <scenario-file> <transcript-file>");
            System.exit(2);
        }
        Path scenarioFile = Path.of(args[0]);
        transcriptPath = Path.of(args[1]);

        if (GraphicsEnvironment.isHeadless()) {
            fail("this machine is headless; there are no dialogs to drive");
            return;
        }

        SCRIPT.addAll(readScript(scenarioFile));

        Runtime.getRuntime().addShutdownHook(new Thread(GuiDriver::writeTranscript));

        // Same entry point main() uses, on its own thread so this one can keep watching.
        Thread app = new Thread(() -> new Runner());
        app.setDaemon(true);
        app.start();

        watch();

        // The script is spent; dismiss anything left over and stop. Reached when the app
        // exited by itself too, which is normal for option 9 and for Cancel.
        System.exit(finished ? 0 : 1);
    }

    /**
     * Answers each dialog as it appears, in order, until the script runs out.
     *
     * <p>Bounded twice over: a per-dialog wait, so a dialog that never appears is reported
     * instead of hanging the build, and a total cap.
     */
    private static void watch() {
        int step = 0;
        long overall = System.currentTimeMillis() + 60_000;

        while (step < SCRIPT.size() && System.currentTimeMillis() < overall) {
            Window next = awaitDialog(step);
            if (next == null) {
                TRANSCRIPT.add(String.format("!! TIMEOUT waiting for turn %d", step));
                break;
            }
            JDialog dialog = (JDialog) next;
            final int currentStep = step;
            try {
                SwingUtilities.invokeAndWait(() -> handle(dialog, currentStep));
            } catch (Exception e) {
                TRANSCRIPT.add("!! watcher error on turn " + currentStep + ": " + e);
            }
            HANDLED.add(dialog);
            step++;
            // Breathing room: the next dialog is built on the EDT right after this one closes,
            // and reading it too eagerly catches a window that is showing but not yet populated.
            sleep(120);
        }

        if (step < SCRIPT.size()) {
            TRANSCRIPT.add("!! script not completed: reached turn " + step
                    + " of " + SCRIPT.size());
        }
        // Deliberately no "script completed" line. In the common case the last turn is menu
        // option 9, the app calls System.exit(0), and the shutdown hook writes the transcript
        // before execution ever gets here - so such a marker would be missing from every session
        // that worked, and any test asserting on it would fail for the wrong reason.
        // Completeness is judged by counting the transcript's own turn records instead.
        finished = true;
    }

    /** Polls for an unhandled, populated dialog. */
    private static Window awaitDialog(int step) {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog d && d.isShowing() && !HANDLED.contains(w)) {
                    return d;
                }
            }
            sleep(40);
        }
        return null;
    }

    /** Runs on the EDT: read the dialog, check it against the script, answer it. */
    private static void handle(JDialog dialog, int step) {
        Turn turn = SCRIPT.get(step);
        String title = String.valueOf(dialog.getTitle());
        List<String> lines = messageLines(dialog);
        String flat = String.join(" ", lines);

        boolean matches = turn.fragment().equals("*") || flat.contains(turn.fragment());
        TRANSCRIPT.add(String.format("%02d | %s | title=%s", step,
                matches ? "OK" : "!! MISMATCH expected a dialog containing ["
                        + turn.fragment() + "]",
                title));
        for (String line : lines) {
            TRANSCRIPT.add("    | " + line);
        }

        if (turn.reply().equals("!")) {
            click(dialog, "cancel");
        } else if (turn.reply().equals("-")) {
            click(dialog, "ok");
        } else {
            typeInto(dialog, turn.reply());
            click(dialog, "ok");
        }
    }

    /**
     * The dialog's message text, one entry per line.
     *
     * <p>{@code JOptionPane} wraps any message containing a newline in HTML, so the label's text
     * arrives as {@code <html>...<br>...}. Left alone that would compare as one long run-on and
     * the line structure - which is the whole thing a sort trace needs to be legible - would be
     * invisible to assertions.
     */
    private static List<String> messageLines(Component c) {
        List<String> out = new ArrayList<>();
        collectMessage(c, out);
        return out;
    }

    private static void collectMessage(Component c, List<String> out) {
        if (c instanceof javax.swing.JLabel label) {
            String plain = toPlainText(label.getText());
            for (String line : plain.split("\n", -1)) {
                String trimmed = line.stripTrailing();
                if (!trimmed.isBlank()) {
                    out.add(trimmed);
                }
            }
        }
        forEachChild(c, kid -> collectMessage(kid, out));
    }

    /** HTML back to readable text: line breaks kept, tags dropped, entities resolved. */
    private static String toPlainText(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw;
        s = s.replaceAll("(?i)<br\\s*/?>", "\n");
        s = s.replaceAll("(?i)</p\\s*>", "\n");
        s = s.replaceAll("<[^>]+>", "");
        s = s.replace("&nbsp;", " ");
        s = s.replace("&lt;", "<");
        s = s.replace("&gt;", ">");
        s = s.replace("&quot;", "\"");
        // &amp; last, so "&amp;lt;" does not collapse into "<".
        s = s.replace("&amp;", "&");
        return s.replace("\r\n", "\n").replace('\r', '\n');
    }

    /** Finds the single input field of an input dialog and types into it. */
    private static void typeInto(Component c, String text) {
        List<JTextField> fields = new ArrayList<>();
        collectFields(c, fields);
        if (fields.isEmpty()) {
            TRANSCRIPT.add("    !! no input field found, nothing typed");
            return;
        }
        JTextField field = fields.get(fields.size() - 1);
        field.setText(text);
    }

    private static void collectFields(Component c, List<JTextField> out) {
        if (c instanceof JTextField f) {
            out.add(f);
        }
        forEachChild(c, kid -> collectFields(kid, out));
    }

    /** Clicks the named button, if the dialog has one. */
    private static void click(Component c, String want) {
        List<AbstractButton> buttons = new ArrayList<>();
        collectButtons(c, buttons);
        for (AbstractButton b : buttons) {
            String text = b.getText() == null ? "" : b.getText().toLowerCase();
            if (text.equals(want) || (want.equals("ok") && text.equals("yes"))) {
                b.doClick();
                return;
            }
        }
        TRANSCRIPT.add("    !! no '" + want + "' button; found " + buttons);
    }

    private static void collectButtons(Component c, List<AbstractButton> out) {
        if (c instanceof AbstractButton b) {
            out.add(b);
        }
        forEachChild(c, kid -> collectButtons(kid, out));
    }

    private static void forEachChild(Component c, java.util.function.Consumer<Component> fn) {
        if (c instanceof Container container) {
            for (Component kid : container.getComponents()) {
                fn.accept(kid);
            }
        }
    }

    private static List<Turn> readScript(Path file) throws IOException {
        List<Turn> turns = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.strip().startsWith("#")) {
                continue;
            }
            int tab = line.indexOf('\t');
            if (tab < 0) {
                throw new IOException("scenario line has no tab separator: " + line);
            }
            turns.add(new Turn(line.substring(0, tab).strip(), line.substring(tab + 1).strip()));
        }
        return turns;
    }

    private static void writeTranscript() {
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(
                transcriptPath, StandardCharsets.UTF_8))) {
            for (String line : TRANSCRIPT) {
                out.println(line);
            }
        } catch (IOException e) {
            System.err.println("could not write transcript: " + e);
        }
    }

    private static void fail(String message) {
        System.err.println(message);
        System.exit(3);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
