# AGENTS.md

Swing demo of sorting/searching algorithms. **Maven build, JUnit 5, no CI, no formatter.**
All production classes are in the **`algorithms` package** under `src/main/java/algorithms/`;
tests are in the same package under `src/test/java/algorithms/`.

## Layout

- `src/main/java/algorithms/` — 14 production classes: `Runner` (entrypoint), `Presenter`
  (sole `JOptionPane` owner), `Validator` (shared input validation), `Array`, five sorts,
  three searches, and two console queue demos.
- `src/test/java/algorithms/` — 18 files: 17 test classes plus `GuiDriver`, the harness
  `GuiSessionTest` forks. One suite per algorithm, plus
  `ArchitectureTest` (structural rules from source text), `GuiSessionTest` (end-to-end
  GUI transcripts), `UserFacingTextTest` (typo detection), and `ValidatorTest`.
- `pom.xml` — standard Maven layout, no `<sourceDirectory>` override.

## Entrypoints

Three independent `main`s; only the first belongs to the algorithm demo:

- `algorithms.Runner` — the Swing GUI (`JOptionPane`). This is the app.
- `algorithms.QueueJava` — console queue, unrelated to the GUI.
- `algorithms.CircularQueue` — console queue, unrelated to the GUI, fixed size 100.

`Runner`'s menu maps 1-9 to Bubble, Insertion, Selection, Merge, Quick, Linear Search,
Exponential, Jump, Exit. That mapping exists only inside a string literal in `Runner.java`.

## Build and run

**Maven is the build.** From the repo root:

    mvn -q clean package        # compile + test -> target/classes
    mvn -q test                 # tests only
    mvn -q test -Dtest=QuickSortTest
    java -cp target/classes algorithms.Runner          # GUI
    java -cp target/classes algorithms.QueueJava       # console
    java -cp target/classes algorithms.CircularQueue   # console

Release 21. `.gitignore` covers `*.class`, `target/`, and the usual IDE and OS droppings,
so compiled output must never be staged.

### Toolchain

**JDK 27** at `C:\Program Files\Java\jdk-27`, on the machine PATH. A stale **JRE 8**
also remains at `C:\Program Files\Java\jre-1.8`. **Maven 3.9.16** at
`C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.16`.

`JAVA_HOME` and `MAVEN_HOME` are both **unset**. Maven therefore picks its JDK from PATH
order, where `Oracle\Java\javapath` shadows `jdk-27\bin` — javapath resolves to 27, so it
works, but nothing pins it. Set `JAVA_HOME=C:\Program Files\Java\jdk-27` to make the build
deterministic.

A long-lived shell may still see the *pre-install* environment and find no `javac`, or
resolve `java` to JRE 8. That is stale process state, not a broken install — open a new
terminal, or prepend the machine PATH yourself.

`Runner` is unrunnable headlessly: it is a modal `JOptionPane` loop. Only `GuiSessionTest`,
which forks a JVM per test and drives real windows, can exercise it without a human.

## Control flow

- `Runner.Menu()` is a `while(true)` loop, not recursion (fixed in Task 14). Option 9 and
  Cancel both `return`, and `main` owns `System.exit(0)`.
- Each algorithm class is a pure static namespace: no fields, no dialogs, no callbacks.
  `Presenter` builds every dialog; `Runner` owns control flow.
- `Validator.isInt` is the sole home for `Character.isDigit`. `QueueJava` and `CircularQueue`
  call it instead of their own copies (Task 15 collapsed three copies into one).
- `Runner` passes `arr.getCopy()` — a **live** reference to `Array`'s `copy` snapshot, not a
  fresh copy. The algorithms clone it themselves, so sorting never mutates the typed array.

## Verification

Two layers, and the difference matters.

**Pure tests** cover the extracted algorithms and the input validators — `intOnly`,
`isValidLength`, `Array`, `QueueJava`, `ExponentialSearch`, and one suite per sort. The
sort suites fuzz against `java.util.Arrays.sort` on a fixed seed. That is deliberate:
`QuickSort` was wrong on ~1 input in 5 and every hand-written shape missed it. **Do not
trust an algorithm here without running it against the oracle** — the project's own code
review asserted the partition was correct by reading it, and that was false.

**GUI tests** (`GuiSessionTest`) cover what used to be manual-only. `GuiDriver` enumerates
`Window.getWindows()`, finds the modal `JOptionPane`, reads its labels, types into the field
and clicks OK or Cancel. Nothing is mocked; the app runs as `main()` starts it. Each test
**forks a JVM**, because option 9 and Cancel both call `System.exit(0)` and would take the
test runner with them. The driver writes its transcript from a shutdown hook for exactly
that reason.

What the GUI layer proves: which dialogs appeared, in what order, with what text, and whether
the app returned to the menu. What it cannot prove: **whether the text is legible once
rendered** — it reads a label's contents, never the screen. Do not report a green
`GuiSessionTest` as "the dialog looks right"; it proves the substance, not the appearance.

**A test class name must end in `Test`, `Tests` or `TestCase`.** Surefire's default includes
are `**/Test*.java`, `**/*Test.java`, `**/*Tests.java` and nothing else, and the POM sets no
`<includes>`. `GuiSession` passed under `-Dtest=GuiSession` while being **silently skipped
by the full build**, because `-Dtest` overrides those patterns. `mvn -q test -Dtest=X` proves
a class compiles and its assertions hold; it does not prove the class is *wired in*.
`mvn -q clean package` is the only check that does.

Three traps, all hit while building it:

- `JOptionPane` wraps any message containing a newline in HTML, so the label's text arrives
  as `<html>...<br>...`. `GuiDriver` converts it back to real lines. Without that, a sort
  trace compares as one long run-on and its line structure is invisible to assertions.
- The failure marker must be matched by **position**, not by `!!` anywhere in the line. The
  welcome screen says "Welcome!!", so a substring search flags every session as a failure.
- Assert completeness by **counting transcript turn records**. A `"script completed"` marker
  is unreachable: the app's `System.exit(0)` runs before the watcher can record finishing.

Requires a display; every GUI test **skips** rather than fails on a headless machine.

`QueueJava` and `CircularQueue` are the only classes that accept piped stdin.

## Known defects

**None.** All pre-existing defects from the 2019 code have been fixed:

- Jump Search NPE — Task 12.
- QuickSort wrong result + display bug — Task 10.
- Option 7 exponential search stub — Task 19.
- Searching for `2` silently cancels (`CANCEL_OPTION` is 2) — Task 11.
- `intOnly("")` / `intOnly(null)` / integer overflow crashes — Task 3.
- Array length `0` causes `ArrayIndexOutOfBoundsException` — Task 3.
- `QueueJava` overflow/underflow fall-through — Task 4.
- `sort` gate removed (it guarded nothing) — Task 5.
- Recursive `Menu()` — Task 14.
- Triplicated digit validators — Task 15.
- Nine user-visible typos — Task 16.

## Style

Most files indent with **tabs**. The exceptions:

- `CircularQueue.java` — 4 spaces throughout.
- `QueueJava.java` — mostly 3 spaces with some stray tab-indented lines.
- everything else — tabs.

Javadoc continuation lines start with a single space then `*`, as ` * text`. That is the
standard convention, not a style deviation — do not "fix" it to a tab.

Match the file you are in; do not reformat as a side effect of another change.

The digit-validation helper lives in `Validator.isInt`. Do not copy it into a new class.
