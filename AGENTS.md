# AGENTS.md

Swing demo of sorting/searching algorithms. **Maven build, JUnit 5, no CI, no formatter**
(both added 2026-10-06 - see Build). All 11 classes sit in the **default package** - there is no
`package` statement anywhere, so nothing can be compiled into a package structure without a
migration.

## Layout

- `Sorting and  Searching Algorithms/src` - the 11 `.java` files.
  The directory name has a **double space** after "and"; quote the path in shell commands.
- `Sorting and  Searching Algorithms/bin` - build output. Untracked since `.gitignore` was
  added; anything sitting in there locally is stale, not a reference (see Build).

## Entrypoints

Three independent `main`s; only the first belongs to the algorithm demo:

- `Runner` - the Swing GUI (`JOptionPane`). This is the app.
- `QueueJava` - console queue, unrelated to the GUI.
- `CircularQueue` - console queue, unrelated to the GUI, fixed size 100.

`Runner`'s menu maps 1-9 to Bubble, Insertion, Selection, Merge, Quick, Linear Search,
Exponential, Jump, Exit. That mapping exists only inside a string literal in `Runner.java`.

## Build and run

**Maven is the build** (added 2026-10-06, `pom.xml`). From the repo root:

    mvn -q clean package        # compile + test -> target/classes
    mvn -q test                 # tests only
    mvn -q test -Dtest=RunnerInputValidationTest
    java -cp target/classes Runner          # GUI
    java -cp target/classes QueueJava       # console
    java -cp target/classes CircularQueue   # console

`pom.xml` sets a non-default layout, temporarily:

- `<sourceDirectory>` = `Sorting and  Searching Algorithms/src` (double space and all)
- `<testSourceDirectory>` = `Sorting and  Searching Algorithms/test`

Tests must **not** live under `<sourceDirectory>`. Maven scans it recursively, so tests
nested in `src/` get compiled into `target/classes` as main sources. They are siblings
until Task 17 moves everything to the standard `src/main/java` + `src/test/java`.

Release 21. `.gitignore` covers `*.class`, `bin/`, `target/`, and the usual IDE and OS
droppings, so compiled output must never be staged. The `.class` files still sitting in
`Sorting and  Searching Algorithms/bin/` locally are leftovers from a Java 13 build -
delete them; Maven ignores that directory entirely.

### Toolchain

**JDK 27** at `C:\Program Files\Java\jdk-27`, on the machine PATH. A stale **JRE 8**
also remains at `C:\Program Files\Java\jre-1.8`. **Maven 3.9.16** at
`C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.16`.

`JAVA_HOME` and `MAVEN_HOME` are both **unset**. Maven therefore picks its JDK from PATH
order, where `Oracle\Java\javapath` shadows `jdk-27\bin` - javapath resolves to 27, so it
works, but nothing pins it. Set `JAVA_HOME=C:\Program Files\Java\jdk-27` to make the build
deterministic.

A long-lived shell may still see the *pre-install* environment and find no `javac`, or
resolve `java` to JRE 8. That is stale process state, not a broken install - open a new
terminal, or prepend the machine PATH yourself.

### Compiling without Maven

    cd "Sorting and  Searching Algorithms"
    javac src\*.java -d bin

In **PowerShell** the wildcard must be **unquoted** or PowerShell hands javac the literal
string and it fails with `error: Invalid filename`. The directory still needs quoting -
its name contains a double space.

`Runner` is still unrunnable headlessly: it is a modal `JOptionPane` loop. Only a human
clicking through can confirm dialog behavior. The algorithm cores (`JumpSearch.jumpSearch`
and the five sorts) can be exercised directly by a scratch class in the default package
compiled against `target/classes` (the Maven output, **not** `bin/`, which Maven ignores) -
that is how the review findings were verified.

## Control flow that reads as a bug but isn't

- `Runner.Menu()` re-invokes itself unconditionally at the bottom of its loop *and* inside
  several switch cases. The menu advances by recursion, not iteration. (Line numbers here are
  as of `v1.0.0`; `Task 3` added ~50 lines above them.)
- Each algorithm holds `private Runner quiano;` that is **never assigned**, then calls
  `quiano.Menu()` / `quiano.intOnly(...)`. This compiles and runs only because those `Runner`
  methods are `static`. Do not "fix" the null by assigning `new Runner()` - that re-enters
  `GUI()` and restarts the input dialog. `SelectionSort` calls `Runner.Menu()` directly
  instead; that inconsistency is not an oversight to tidy either.
- `Runner` passes `arr.getCopy()` - a **live** reference to `Array`'s `copy` snapshot, not a
  fresh copy. The algorithms clone it themselves, so sorting never mutates the typed array
  even across repeated sorts.

## Verification

Two layers, and the difference matters.

**Pure tests** cover the extracted algorithms and the input validators - `intOnly`,
`isValidLength`, `Array`, `QueueJava`, and one suite per sort. The sort suites fuzz against
`java.util.Arrays.sort` on a fixed seed. That is deliberate: `Quicksort` was wrong on ~1 input in 5
and every hand-written shape missed it. **Do not trust an algorithm here without running it against
the oracle** - the project's own code review asserted the partition was correct by reading it, and
that was false.

`Quicksort`'s partition returns a *boundary*, not the pivot's final index. Recursing on
`[low..pi]` / `[pi+1..high]` is required; the 2019 `[low..pi-1]` / `[pi..high]` covered every
index yet silently dropped the pivot's guarantee, which is why it read as sound. Its inner scans
must also be bounded - the original relied on the pivot value to stop `while(arr[low] < pivot)`,
which stops being true once elements swap past it.

**GUI tests** (`GuiSessionTest`) cover what used to be manual-only. `GuiDriver` enumerates
`Window.getWindows()`, finds the modal `JOptionPane`, reads its labels, types into the field and
clicks OK or Cancel. Nothing is mocked; the app runs as `main()` starts it. Each test **forks a
JVM**, because option 9 and Cancel both call `System.exit(0)` and would take the test runner with
them. The driver writes its transcript from a shutdown hook for exactly that reason.

What the GUI layer proves: which dialogs appeared, in what order, with what text, and whether the
app returned to the menu. What it cannot prove: **whether the text is legible once rendered** -
it reads a label's contents, never the screen. Do not report a green `GuiSessionTest` as "the
dialog looks right"; it proves the substance, not the appearance.

**A test class name must end in `Test`, `Tests` or `TestCase`.** Surefire's default includes are
`**/Test*.java`, `**/*Test.java`, `**/*Tests.java` and nothing else, and the POM sets no
`<includes>`. `GuiSession` passed under `-Dtest=GuiSession` while being **silently skipped by the
full build**, because `-Dtest` overrides those patterns. `mvn -q test -Dtest=X` proves a class
compiles and its assertions hold; it does not prove the class is *wired in*. `mvn -q clean package`
is the only check that does.

Three traps, all hit while building it:

- `JOptionPane` wraps any message containing a newline in HTML, so the label's text arrives as
  `<html>...<br>...`. `GuiDriver` converts it back to real lines. Without that, a sort trace
  compares as one long run-on and its line structure is invisible to assertions.
- The failure marker must be matched by **position**, not by `!!` anywhere in the line. The
  welcome screen says "Welcome!!", so a substring search flags every session as a failure.
- Assert completeness by **counting transcript turn records**. A `"script completed"` marker is
  unreachable: the app's `System.exit(0)` runs before the watcher can record finishing.

Requires a display; every GUI test **skips** rather than fails on a headless machine.

`QueueJava` and `CircularQueue` are the only classes that accept piped stdin.

## Known defects (pre-existing, not regressions)

Line numbers are as of `v1.0.0` unless noted. `Task 3` shifted `Runner.java` lines only.

- **Jump Search (option 8) is dead on arrival.** `JumpSearch.java:35` calls `st.nextToken()`
  but the static `StringTokenizer st` (line 11) is never assigned, so it NPEs on first input;
  the empty `catch` at line 48 hides it. The `jumpSearch()` core at lines 53-75 is correct.
- ~~**Quicksort's result dialog is wrong.**~~ **Fixed in Task 10 (2026-10-07).** Both the `1..n`
  display *and* the algorithm, which this file wrongly described as correct - it failed ~1 input
  in 5. See `CODE_REVIEW.md §Architecture — Quicksort's algorithm was also wrong`.
- **Option 7 (Exponential Search) is a stub** - the call is commented out and no
  `ExponentialSearch` class exists.
- **Searching for the value `2` silently cancels.** `JOptionPane.CANCEL_OPTION` is **2**, not
  -1, and `intOnly("2")` is true, so `LinearSearch`'s `opt == CANCEL_OPTION` check fires on
  ordinary input: type `2` to search for 2 and the app returns to the menu. Every other digit
  searches normally, which is why it survives a casual try.

Leave these unless asked. Bundling a fix into an unrelated edit is out of scope.

## Style

Most files indent with **tabs**. The exceptions, measured 2026-10-06:

- `CircularQueue.java` - 4 spaces throughout (172 indented lines, 0 tabs)
- `QueueJava.java` - mostly 3 spaces (134 lines) with 18 stray tab-indented lines
- everything else - tabs

Javadoc continuation lines start with a single space then `*`, as ` * text`. That is the
standard convention, not a style deviation - do not "fix" it to a tab.

Match the file you are in; do not reformat as a side effect of another change. Note that an
earlier version of this file claimed `InsertionSort` used 4 spaces. That was wrong: it had 56
tab-indented lines and exactly **one** space-indented line (the `quiano` field, since deleted).
An earlier draft of Task 7 repeated the same error.

The digit-validation helper is copy-pasted three times (`Runner.intOnly`,
`QueueJava.isInteger`, `CircularQueue.isInteger`). There is no shared util class - don't
assume one when adding a class.
