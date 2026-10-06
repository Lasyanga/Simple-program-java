# AGENTS.md

Swing demo of sorting/searching algorithms. No build tool, no tests, no CI, no formatter.
All 11 classes sit in the **default package** - there is no `package` statement anywhere,
so nothing can be compiled into a package structure without a migration.

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

No `pom.xml`, Gradle, Makefile, or build script exists. `.gitignore` covers `*.class`,
`bin/`, and the usual IDE and OS droppings, so compiled output must never be staged.
The `.class` files still sitting in `bin/` locally are leftovers from a Java 13 build -
recompile before trusting them, or delete the directory.

From `Sorting and  Searching Algorithms/`:

    javac src\*.java -d bin
    java -cp bin Runner          # GUI
    java -cp bin QueueJava       # console
    java -cp bin CircularQueue   # console

Toolchain as of 2026-10-06: this machine has a **JRE 8 only** (`C:\Program Files\Java\jre-1.8`),
no `javac` on PATH. The leftover classes in `bin/` are class-file major version 57 (Java 13),
so `java -cp bin Runner` fails today with `UnsupportedClassVersionError` (57.0 vs max 52.0).
A JDK 8+ must be installed and the sources recompiled before anything runs; the commands
above are therefore **unverified end to end**.

## Control flow that reads as a bug but isn't

- `Runner.Menu()` re-invokes itself unconditionally at the bottom of its loop *and* inside
  several switch cases (`Runner.java:135`). The menu advances by recursion, not iteration.
- Each algorithm holds `private Runner quiano;` that is **never assigned**, then calls
  `quiano.Menu()` / `quiano.intOnly(...)`. This compiles and runs only because those `Runner`
  methods are `static`. Do not "fix" the null by assigning `new Runner()` - that re-enters
  `GUI()` and restarts the input dialog. `SelectionSort` calls `Runner.Menu()` directly
  instead; that inconsistency is not an oversight to tidy either.
- `Runner` passes `arr.getCopy()` - a **live** reference to `Array`'s `copy` snapshot, not a
  fresh copy. The algorithms clone it themselves, so sorting never mutates the typed array
  even across repeated sorts.

## Verification

No tests exist and nothing here runs headlessly - `Runner` is a modal dialog loop, so only a
human clicking through can confirm behavior. Report that honestly instead of implying a check
ran. `QueueJava` and `CircularQueue` are the only classes that accept piped stdin.

## Known defects (pre-existing, not regressions)

- **Jump Search (option 8) is dead on arrival.** `JumpSearch.java:35` calls `st.nextToken()`
  but the static `StringTokenizer st` (line 11) is never assigned, so it NPEs on first input;
  the empty `catch` at line 48 hides it. The `jumpSearch()` core at lines 53-75 is correct.
- **Quicksort's result dialog is wrong.** `Quicksort.java:17-20` builds the "Sorted element"
  string from `1..n` instead of the sorted array. The sort itself is correct.
- **Option 7 (Exponential Search) is a stub** - the call is commented out at
  `Runner.java:113` and no `ExponentialSearch` class exists.

Leave these unless asked. Bundling a fix into an unrelated edit is out of scope.

## Style

Mixed indentation already in the tree: `InsertionSort` uses 4 spaces, `CircularQueue` and
`QueueJava` 3, the rest tabs. Match the file you are in; do not reformat.

The digit-validation helper is copy-pasted three times (`Runner.intOnly`,
`QueueJava.isInteger`, `CircularQueue.isInteger`). There is no shared util class - don't
assume one when adding a class.
