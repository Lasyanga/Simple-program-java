# Tasks: Enhance sorting/searching app

Case: `enhance-sorting-app`
Source of the work: `CODE_REVIEW.md` (evidence lives there; this file holds only work items).
Ids are append-only. A task is done when `Status: done` **and** every Verification item is ticked.

---

## Phase 0: Unblock

Nothing can be compiled or run on this machine today, so every later verification step
depends on these two tasks.

## Task 1: Install a JDK and compile the existing sources

**Status:** open

**Description:** There is no `javac` on this machine — only a JRE 8 at
`C:\Program Files\Java\jre-1.8`. Install a JDK (13+ to match the old bytecode, or 8+ for the
sources; 17 or 21 LTS is the sensible choice) and prove the 2019 sources still compile before
changing any of them. This task also closes the open caveat in `CODE_REVIEW.md`: two crashes
were identified by reading, never by running.

**Acceptance criteria:**
- [ ] `javac` is on `PATH` and reports a version
- [ ] All 11 sources compile with zero errors and zero warnings that matter
- [ ] The two review findings marked *confirmable* are reproduced by hand, and the review is corrected if either turns out wrong

**Verification:**
- [ ] Build succeeds: `javac -d "$env:TEMP\sp-verify" "Sorting and  Searching Algorithms\src\*.java"` exits 0
- [ ] Manual check: launch `Runner`, clear the array-length field, press OK — confirm the app dies with "Bye.. bye.."
- [ ] Manual check: in `QueueJava`, enqueue `capacity + 1` items — confirm it prints "Overflow Program terminated." and then writes anyway

**Dependencies:** None

**Files likely touched:**
- none (environment only; record the JDK version in `AGENTS.md`)

**Estimated scope:** XS

---

## Task 2: Add Maven with JUnit 5

**Status:** open

**Description:** Introduce `pom.xml` so there is a compile, test, and package command. To avoid
blocking on any file moves, point `<sourceDirectory>` at the existing
`Sorting and  Searching Algorithms/src` for now; Tasks 17-18 clean that up. Add JUnit 5
(`junit-jupiter`) and Surefire so `mvn test` runs the suite. Target Java 8 in the compiler
plugin so the sources keep working on older runtimes.

**Context:** `CODE_REVIEW.md §Suggested order of work` (item 5, remainder)

**Acceptance criteria:**
- [ ] `mvn -q clean package` succeeds and produces `target/classes` with all 11 classes
- [ ] `mvn -q test` runs and reports zero tests without error
- [ ] `mvn -q test -Dtest=SomeName` runs a single test class

**Verification:**
- [ ] Build succeeds: `mvn -q clean package` exits 0
- [ ] Tests pass: `mvn -q test` exits 0
- [ ] Manual check: `java -cp target/classes Runner` still shows the welcome dialog

**Dependencies:** Task 1

**Files likely touched:**
- `pom.xml`

**Estimated scope:** S

**Risk:** `doubt-review required` (changes the build boundary — how the project is compiled and consumed from here on)

---

### Checkpoint: After Tasks 1-2
- [ ] `mvn -q clean package` succeeds
- [ ] `mvn -q test` runs cleanly with zero tests
- [ ] The GUI still launches and the menu appears
- [ ] Review with human before proceeding — confirm the reproduced crashes match the review

---

## Phase 1: Correctness fixes

Behavior fixes against the code as it stands, before any restructuring. Small, independent,
and each verifiable on its own.

## Task 3: Fix `intOnly("")` so an empty field stops killing the app

**Status:** open

**Description:** `Runner.intOnly` returns `true` for a zero-length string because its loop never
executes, so clearing a dialog field and pressing OK reaches `Integer.parseInt("")` and throws.
At the length prompt the exception is swallowed by `GUI()`'s catch and the app exits; at the
menu it propagates to the same handler and the app dies. Decide whether an empty field is
rejected with a reprompt (better UX and the smaller change) or treated as cancel, and implement
that. `JumpSearch` and `LinearSearch` call the same helper, so the fix reaches all three.

**Context:** `CODE_REVIEW.md §Correctness`

**Acceptance criteria:**
- [ ] An empty field no longer terminates the app at any of the three dialogs
- [ ] `intOnly("")` returns `false`, or the callers handle emptiness explicitly
- [ ] Cancel still returns to the previous menu rather than exiting

**Verification:**
- [ ] Tests pass: `mvn -q test`
- [ ] Manual check: empty the length field, press OK — the app reprompts instead of exiting
- [ ] Manual check: empty the menu field, press OK — the menu reprompts instead of exiting

**Dependencies:** Task 2

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/Runner.java`

**Estimated scope:** XS

---

## Task 4: Fix `QueueJava` overflow and underflow falling through

**Status:** open

**Description:** `QueueJava.enqueue` prints "Overflow Program terminated." and then continues
into the insert, because the `System.exit(1)` is commented out. `dequeue` is worse: on
underflow `count--` goes to `-1`, and since `isEmpty()` tests `size()==0`, the queue becomes
permanently neither empty nor full. Restore the early `return` in both guards (return, not
exit — this is a library class, and killing the JVM from it is the wrong behavior). Also
decide what a zero or negative capacity should do; today `% capacity` throws
`ArithmeticException`.

**Context:** `CODE_REVIEW.md §Correctness` (Critical)

**Acceptance criteria:**
- [ ] Enqueue on a full queue prints the message and leaves the queue unchanged
- [ ] Dequeue on an empty queue prints the message and leaves `count` at 0
- [ ] A capacity of 0 or less is rejected at construction with a clear message

**Verification:**
- [ ] Tests pass: `mvn -q test`
- [ ] Manual check: enqueue `capacity + 1` items, then display — the queue holds exactly `capacity`
- [ ] Manual check: dequeue from a fresh empty queue twice, then enqueue — behavior is still correct

**Dependencies:** Task 2

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/QueueJava.java`

**Estimated scope:** XS

---

## Task 5: Make the `sort` gate a real check or delete it

**Status:** open

**Description:** `Runner.sort` is a boolean written as an incrementing counter, and the only
thing it gates is menu options 7 and 8. But option 8 passes `arr.getsorted()`, which clones and
`Arrays.sort`s a fresh array on every call — so the array is sorted by the JDK whether or not
the user ran a sort, and whether or not the gate passes. Either make it an honest boolean and
have jump search use the user's sorted result, or delete the gate and state in the dialog that
jump search sorts the array itself. The second is smaller and more truthful; pick one
deliberately.

**Context:** `CODE_REVIEW.md §Correctness`

**Acceptance criteria:**
- [ ] `sort` is a `boolean` or is gone; no counter remains
- [ ] The dialog text tells the truth about whether the array was sorted by the user or by the app
- [ ] Option 8 works on a genuinely user-sorted array

**Verification:**
- [ ] Tests pass: `mvn -q test`
- [ ] Manual check: run a sort, then jump search — correct index reported
- [ ] Manual check: attempt jump search with no prior sort — behavior matches the chosen design

**Dependencies:** Task 2

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/Runner.java`
- `Sorting and  Searching Algorithms/src/Array.java`

**Estimated scope:** XS

---

### Checkpoint: After Tasks 3-5
- [ ] `mvn -q clean package` succeeds
- [ ] `mvn -q test` passes
- [ ] Both reproduced crashes from Task 1 are gone
- [ ] Review with human before proceeding

---

## Phase 2: Separate algorithms from the GUI

The structural move. Each algorithm is rewritten as a pure static method and gains a test class
in the same task, so a broken extraction fails immediately rather than at the end of the phase.

## Task 6: Extract Bubble Sort with tests

**Status:** open

**Description:** Establish the pattern on the simplest case. Rewrite `BubbleSort` so the sort is
`public static int[] bubbleSort(int[] input)` with no fields, no dialog, no callback, and a
second overload that records each pass into a `List<int[]>`. Keep the early-exit optimization —
it is correct. The class's `private Runner quiano` field disappears with the dialog code; do not
replace it with `new Runner()`, which re-enters the input dialog.

**Context:** `CODE_REVIEW.md §Architecture — the one structural problem`

**Acceptance criteria:**
- [ ] `bubbleSort` is static, side-effect free, and does not mutate its argument
- [ ] The trace overload returns one state per pass, in order
- [ ] `BubbleSortTest` covers unsorted, already-sorted, single-element, empty, and all-duplicates input

**Verification:**
- [ ] Tests pass: `mvn -q test -Dtest=BubbleSortTest`
- [ ] Tests pass: `mvn -q test`
- [ ] Manual check: the menu's bubble sort option still shows a readable trace and returns to the menu

**Dependencies:** Task 5

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/BubbleSort.java`
- `Sorting and  Searching Algorithms/src/test/java/BubbleSortTest.java`

**Estimated scope:** S

---

## Task 7: Extract Insertion Sort with tests

**Status:** open

**Description:** Same shape as Task 6. The shift loop is correct — keep it. Note this file uses
4-space indentation where the rest of the tree uses tabs; match the file, and do not reformat
it as a side effect of this task.

**Context:** `CODE_REVIEW.md §Architecture — the one structural problem`

**Acceptance criteria:**
- [ ] `insertionSort` is static, side-effect free, and does not mutate its argument
- [ ] The trace overload returns one state per insertion
- [ ] `InsertionSortTest` covers the same five input shapes as `BubbleSortTest`

**Verification:**
- [ ] Tests pass: `mvn -q test -Dtest=InsertionSortTest`
- [ ] Tests pass: `mvn -q test`
- [ ] Manual check: the insertion sort option still traces correctly

**Dependencies:** Task 6

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/InsertionSort.java`
- `Sorting and  Searching Algorithms/src/test/java/InsertionSortTest.java`

**Estimated scope:** S

---

## Task 8: Extract Selection Sort with tests

**Status:** open

**Description:** Same shape as Tasks 6-7. This class also carries a dead `private Array arr;`
field (never assigned or read) — delete it as part of the rewrite rather than porting it.

**Context:** `CODE_REVIEW.md §Architecture — the one structural problem`; `CODE_REVIEW.md §Dead code`

**Acceptance criteria:**
- [ ] `selectionSort` is static, side-effect free, and does not mutate its argument
- [ ] The trace overload returns one state per outer iteration
- [ ] `SelectionSortTest` covers the same five input shapes, including all-duplicates (the swap runs unconditionally here)

**Verification:**
- [ ] Tests pass: `mvn -q test -Dtest=SelectionSortTest`
- [ ] Tests pass: `mvn -q test`
- [ ] Manual check: the selection sort option still traces correctly

**Dependencies:** Task 6

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/SelectionSort.java`
- `Sorting and  Searching Algorithms/src/test/java/SelectionSortTest.java`

**Estimated scope:** S

---

## Task 9: Extract Merge Sort with tests

**Status:** open

**Description:** Same shape. This is the least readable file in the project: `count`, `x`, `y`,
and `round` exist only to indent the visualization, and `getMergeProcess()` is a `getX()` method
called for its side effect with the return discarded. All four fields and that method go away.
Keep the single tail-copy loop — it is correct, because the main loop guarantees only one side
can have leftovers. Add a comment saying so, or the next reader will "fix" it.

**Context:** `CODE_REVIEW.md §Architecture — the one structural problem`; `CODE_REVIEW.md §Readability`

**Acceptance criteria:**
- [ ] `mergeSort` is static, side-effect free, and does not mutate its argument
- [ ] `count`, `x`, `y`, `round`, and `getMergeProcess()` are gone
- [ ] `MergeSortTest` covers the same five input shapes

**Verification:**
- [ ] Tests pass: `mvn -q test -Dtest=MergeSortTest`
- [ ] Tests pass: `mvn -q test`
- [ ] Manual check: the merge sort option shows a readable merge trace, not the old indented tangle

**Dependencies:** Task 6

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/MergeSort.java`
- `Sorting and  Searching Algorithms/src/test/java/MergeSortTest.java`

**Estimated scope:** S

---

### Checkpoint: After Tasks 6-9
- [ ] All four sort tests pass
- [ ] No sort class retains a `Runner` field, a dialog call, or a mutable static
- [ ] The GUI still traces all four sorts correctly by hand
- [ ] Review with human before proceeding — this is the point of no easy return

---

## Task 10: Extract Quicksort with tests, fixing the `1..n` display

**Status:** open

**Description:** Same shape, plus the one user-visible bug in this class: the "Sorted element"
string is built from `1..n` instead of the sorted array. The Hoare partition and its recursion
bounds are correct — keep them. The test suite is what should have caught this, so the trace
assertion here matters more than in the other four.

**Context:** `CODE_REVIEW.md §Known defects`; `CODE_REVIEW.md §Architecture — the one structural problem`

**Acceptance criteria:**
- [ ] `quickSort` is static, side-effect free, and does not mutate its argument
- [ ] The dialog reports the actual sorted array, not `1..n`
- [ ] `QuicksortTest` asserts the *trace contents*, not just the final result — a test that only checked the result would pass against the old bug

**Verification:**
- [ ] Tests pass: `mvn -q test -Dtest=QuicksortTest`
- [ ] Tests pass: `mvn -q test`
- [ ] Manual check: quicksort a known array such as 42, 3, 17, 8 — the dialog shows `3 8 17 42`

**Dependencies:** Task 6

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/Quicksort.java`
- `Sorting and  Searching Algorithms/src/test/java/QuicksortTest.java`

**Estimated scope:** S

---

## Task 11: Extract Linear Search with tests

**Status:** open

**Description:** `linearSearch` becomes `public static int linearSearch(int[] input, int key)`
returning the index or -1. Decide deliberately what happens on duplicates: the current code
reports *every* matching index in one string. Either keep that (`int[] linearSearchAll`) or
narrow to first-match and say so in the dialog. Also delete the unreachable
`opt == JOptionPane.CANCEL_OPTION` check — `showInputDialog` returns `null` on cancel, and
`Integer.parseInt(null)` throws before that comparison is ever reached.

**Context:** `CODE_REVIEW.md §Architecture — the one structural problem`; `CODE_REVIEW.md §Dead code`

**Acceptance criteria:**
- [ ] `linearSearch` is static and pure
- [ ] Duplicate-key behavior is a deliberate choice, documented in the dialog text
- [ ] `LinearSearchTest` covers found, not-found, duplicates, empty input, and first/last position

**Verification:**
- [ ] Tests pass: `mvn -q test -Dtest=LinearSearchTest`
- [ ] Tests pass: `mvn -q test`
- [ ] Manual check: linear search reports the same matches as before, or the intentional change

**Dependencies:** Task 6

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/LinearSearch.java`
- `Sorting and  Searching Algorithms/src/test/java/LinearSearchTest.java`

**Estimated scope:** S

---

## Task 12: Extract Jump Search with tests, fixing the NPE

**Status:** open

**Description:** `jumpSearch(int[] input, int key)` is already a correct static method — the bug
is the dialog around it. `JumpsearchGUI` calls `st.nextToken()` on a `StringTokenizer` that is
never assigned, so it NPEs on the first keystroke and an empty `catch` hides it. Delete the
tokenizer entirely and parse the input properly. The prompt currently advertises
`[element][interval]`, which is a lie: the code parses the whole field as one integer. Note
that the empty `catch` is load-bearing today — it is the only reason Cancel returns to the menu
instead of crashing — so it must be replaced with real cancel handling, not just deleted.

**Context:** `CODE_REVIEW.md §Known defects`; `CODE_REVIEW.md §Dead code`

**Acceptance criteria:**
- [ ] `jumpSearch` is static and pure; the unused `st` and `inpt[]` fields are gone
- [ ] The search dialog performs a real search on a sorted array and reports the index
- [ ] Cancel returns to the menu; the empty `catch` is gone
- [ ] `JumpSearchTest` covers found, not-found, key below range, key above range, and single-element input

**Verification:**
- [ ] Tests pass: `mvn -q test -Dtest=JumpSearchTest`
- [ ] Tests pass: `mvn -q test`
- [ ] Manual check: jump search actually returns an index — this option has never worked
- [ ] Manual check: Cancel from the jump search dialog returns to the menu

**Dependencies:** Task 6, Task 5

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/JumpSearch.java`
- `Sorting and  Searching Algorithms/src/test/java/JumpSearchTest.java`

**Estimated scope:** S

---

## Task 13: Collapse the six dialog methods into one presenter

**Status:** open

**Description:** After Tasks 6-12 each algorithm is pure, leaving six near-identical
`xxxGUI()` / `getXProcess()` pairs that only differ in title strings. Move every `JOptionPane`
call into a single `Presenter` class that takes a title and a trace and shows it. This is the
only task permitted to touch every dialog, and it relocates existing strings without changing
what the user sees.

**Acceptance criteria:**
- [ ] No algorithm class calls `JOptionPane` or references `Runner`
- [ ] One `Presenter` owns all dialog construction
- [ ] Dialog titles, wording, and message ordering are unchanged

**Verification:**
- [ ] Tests pass: `mvn -q test`
- [ ] Build succeeds: `mvn -q clean package`
- [ ] Manual check: step through all nine menu options and diff the dialogs against the previous behavior

**Dependencies:** Task 6, Task 7, Task 8, Task 9, Task 10, Task 11, Task 12

**Files likely touched:**
- new `Sorting and  Searching Algorithms/src/Presenter.java`
- the six algorithm/search classes
- `Sorting and  Searching Algorithms/src/Runner.java`

**Estimated scope:** M

**Risk:** `doubt-review required` (touches shared UI state across seven files at once)

---

### Checkpoint: After Tasks 10-13
- [ ] All six algorithm test classes pass
- [ ] No source file under `src` references `JOptionPane` except `Presenter` and `Runner`
- [ ] All nine menu options exercised by hand, including the two that never worked
- [ ] Review with human before proceeding

---

## Phase 3: Control flow and cleanup

## Task 14: Replace the recursive `Menu()` with a loop

**Status:** open

**Description:** `Runner.Menu()` calls itself unconditionally at the bottom of its `do/while`
and again inside several `case` labels, so the loop condition is never reached — every path
either recurses deeper or hits `System.exit(0)`. Each menu click currently adds stack frames.
Convert it to a real loop: a single `while` with the switch inside, returning to the top instead
of recursing. `LinearSearch.Searching` and `JumpSearch.JumpsearchGUI` do the same thing to their
own dialog loops and should be converted in the same pass.

**Context:** `CODE_REVIEW.md §Correctness`

**Acceptance criteria:**
- [ ] `Menu()` returns normally instead of recursing; no self-call remains in any menu or search loop
- [ ] Behavior for options 1-9 is unchanged, including cancel and invalid input
- [ ] Menu interactions no longer grow the call stack (verifiable by repeated navigation)

**Verification:**
- [ ] Tests pass: `mvn -q test`
- [ ] Build succeeds: `mvn -q clean package`
- [ ] Manual check: navigate the menu 50+ times, including into searches and back — no degradation

**Dependencies:** Task 13

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/Runner.java`

**Estimated scope:** S

**Risk:** `doubt-review required` (rewrites the app's only control-flow loop; a mistake here strands the user in a dialog)

---

## Task 15: Remove dead code and the triplicated validators

**Status:** open

**Description:** Delete `SelectionSort`'s dead `arr` field (if Task 8 has not already), the
commented-out demo block in `CircularQueue.main`, and collapse the three copies of the
digit validator — `Runner.intOnly`, `QueueJava.isInteger`, `CircularQueue.isInteger` — into one
shared helper that all three call. Confirm each item is genuinely unreferenced before deleting;
`git grep` is the check.

**Context:** `CODE_REVIEW.md §Dead code`

**Acceptance criteria:**
- [ ] The three validator methods are replaced by one shared helper
- [ ] `git grep` finds no remaining references to each deleted item
- [ ] No behavior change in any of the three entrypoints

**Verification:**
- [ ] Tests pass: `mvn -q test`
- [ ] Build succeeds: `mvn -q clean package`
- [ ] Manual check: all three entrypoints still reject non-numeric input

**Dependencies:** Task 14

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/Runner.java`
- `Sorting and  Searching Algorithms/src/QueueJava.java`
- `Sorting and  Searching Algorithms/src/CircularQueue.java`

**Estimated scope:** M

---

## Task 16: Fix typos and inconsistent naming

**Status:** open

**Description:** Mechanical pass, no logic changes. Rename `partion` to `partition`; fix the
"Insetion Sort" dialog title; rewrite the broken "Enter the you want to Search:[element][interval]"
prompt in both search classes (and drop the `[element][interval]` hint, which describes a format
the code never parses); make `Quicksort` consistent with `BubbleSort`/`InsertionSort` casing.

**Acceptance criteria:**
- [ ] No user-visible string contains a typo
- [ ] Class naming is consistent across the six algorithm classes
- [ ] No logic changes in this commit — diff is strings and one method name only

**Verification:**
- [ ] Tests pass: `mvn -q test`
- [ ] Manual check: every dialog's text and title read correctly

**Dependencies:** Task 14

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/Quicksort.java`
- `Sorting and  Searching Algorithms/src/InsertionSort.java`
- `Sorting and  Searching Algorithms/src/LinearSearch.java`
- `Sorting and  Searching Algorithms/src/JumpSearch.java`

**Estimated scope:** S

---

### Checkpoint: After Tasks 14-16
- [ ] `mvn -q clean package` succeeds and `mvn -q test` passes
- [ ] No self-recursive call remains in any menu or dialog loop
- [ ] `git grep` confirms the dead code is gone
- [ ] Review with human before proceeding

---

## Phase 4: Layout and polish

## Task 17: Move sources to the conventional Maven layout

**Status:** open

**Description:** Move `Sorting and  Searching Algorithms/src` to `src/main/java` and the tests
to `src/test/java`, then drop the `<sourceDirectory>` override from `pom.xml`. This removes the
double space in the directory name that has to be quoted in every shell command and every path.
Use `git mv` so history follows the files. Do not move `QueueJava` or `CircularQueue` anywhere
special — they are not part of the sorting app.

**Context:** `CODE_REVIEW.md §Suggested order of work` (item 5, remainder)

**Acceptance criteria:**
- [ ] Sources live under `src/main/java`, tests under `src/test/java`
- [ ] `pom.xml` contains no `<sourceDirectory>` override
- [ ] The `Sorting and  Searching Algorithms` directory no longer exists

**Verification:**
- [ ] Build succeeds: `mvn -q clean package` exits 0
- [ ] Tests pass: `mvn -q test` exits 0
- [ ] Manual check: `java -cp target/classes Runner` launches without a path workaround

**Dependencies:** Task 13

**Files likely touched:**
- all 11 source files (moved)
- all test files (moved)
- `pom.xml`

**Estimated scope:** S

**Risk:** `doubt-review required` (renames paths for every file in the repo; a partial move breaks the build)

---

## Task 18: Add a real package declaration

**Status:** open

**Description:** All 11 classes sit in the default package. Add `package algorithms;` (or a
better name) to each, and move the files to match under `src/main/java/algorithms/`. Both halves
must land together — a package declaration without the directory move does not compile.

**Acceptance criteria:**
- [ ] Every class declares a package and sits in the matching directory
- [ ] No `import` of a same-package class remains
- [ ] The suite still passes with no test changes beyond the new package line

**Verification:**
- [ ] Build succeeds: `mvn -q clean package` exits 0
- [ ] Tests pass: `mvn -q test` exits 0
- [ ] Manual check: all three entrypoints launch

**Dependencies:** Task 17

**Files likely touched:**
- all 11 source files
- all test files

**Estimated scope:** S

**Risk:** `doubt-review required` (package and directory must agree exactly; a mismatch fails at compile, not silently)

---

## Task 19: Resolve menu option 7 (implement or remove Exponential Search)

**Status:** open

**Description:** Option 7 currently shows a dialog and does nothing — the call is commented out
and no `ExponentialSearch` class exists. Either implement it as a pure static method with tests
(same shape as the other searches) or remove the option and renumber the menu. Do not leave a
menu entry that lies. Implementing it is roughly an hour of work and fits this project's purpose;
removing it is smaller.

**Context:** `CODE_REVIEW.md §Known defects`

**Acceptance criteria:**
- [ ] Option 7 either performs a real exponential search, or is gone from the menu
- [ ] The README's method table matches whichever choice was made
- [ ] No commented-out code remains in the menu switch

**Verification:**
- [ ] Tests pass: `mvn -q test`
- [ ] Manual check: every menu option from 1 to Exit does what its label says

**Dependencies:** Task 14

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/Runner.java`
- new `Sorting and  Searching Algorithms/src/ExponentialSearch.java` (if implemented)

**Estimated scope:** S

---

## Task 20: Update README and AGENTS.md to match the new structure

**Status:** open

**Description:** Both documents describe a repo that will no longer exist after Tasks 17-19:
`AGENTS.md` documents the double-space directory, the default package, the recursive `Menu()`,
and the `javac` workflow; `README.md` documents the same paths plus a known-issues section whose
entries Tasks 10, 12, and 19 will have fixed. Rewrite both against the finished code. Also
correct the review's *unverified* caveat in `CODE_REVIEW.md` — by then the crashes will have
been reproduced and fixed, so the review should say so rather than leaving them as open findings.

**Acceptance criteria:**
- [ ] Every path, command, and package name in both documents matches the repo
- [ ] `CODE_REVIEW.md`'s known-issues and suggested-order sections are marked resolved with a pointer to the fixing task
- [ ] No documented command fails when run as written

**Verification:**
- [ ] Manual check: run every command in `README.md` verbatim, from a clean clone
- [ ] Manual check: every claim in `AGENTS.md` checked against the source

**Dependencies:** Task 17, Task 18, Task 19

**Files likely touched:**
- `README.md`
- `AGENTS.md`
- `CODE_REVIEW.md`

**Estimated scope:** S

---

## Task 21: Add test coverage for the two queue classes

**Status:** open

**Description:** `QueueJava` and `CircularQueue` are unrelated to the sorting app and have no
tests, because both are welded to `Scanner` and their own `main`. Decouple the data structure
from the console loop the same way the algorithms were decoupled — a plain queue class with
`enqueue`/`dequeue`/`peek`/`display`, and a thin `main` that drives it — then test the
structure. `QueueJava`'s early-exit bug (Task 4) is exactly the kind of defect these tests
should lock down.

**Context:** `CODE_REVIEW.md §Architecture — the one structural problem`

**Acceptance criteria:**
- [ ] Both queue classes separate the data structure from the `Scanner` loop
- [ ] `QueueJavaTest` covers enqueue, dequeue, peek, overflow, and underflow
- [ ] `CircularQueueTest` covers wraparound from last slot to first, and the single-element reset case

**Verification:**
- [ ] Tests pass: `mvn -q test -Dtest=QueueJavaTest`
- [ ] Tests pass: `mvn -q test`
- [ ] Manual check: both console entrypoints still behave identically from the terminal

**Dependencies:** Task 15

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/QueueJava.java`
- `Sorting and  Searching Algorithms/src/CircularQueue.java`
- new test classes for both

**Estimated scope:** M

---

## Task 22: Add `.gitattributes` for line endings

**Status:** open

**Description:** Git warns `LF will be replaced by CRLF` on every commit because there is no
`.gitattributes`. Add one that normalizes text files to LF in the repository and checks them
out per-platform. Then run a one-time renormalization so the existing files stop producing
warnings. This is the last task because renormalizing touches every file's blob and would
inflate the diff of any task that ran alongside it.

**Acceptance criteria:**
- [ ] `.gitattributes` normalizes `*.java`, `*.md`, `pom.xml`, and `.gitignore`
- [ ] A renormalization commit exists and is separate from any other change
- [ ] No line-ending warning appears on subsequent commits

**Verification:**
- [ ] Manual check: commit a trivial change and confirm no CRLF warning
- [ ] Manual check: clone on Windows and on a POSIX system; both check out sane line endings

**Dependencies:** Task 20

**Files likely touched:**
- `.gitattributes`
- every text file (renormalization only)

**Estimated scope:** XS

**Risk:** `doubt-review required` (rewrites line endings for every file in the repo; the resulting diff obscures any real change if bundled with one)

---

### Checkpoint: After Tasks 17-22
- [ ] `mvn -q clean package` succeeds from a clean clone
- [ ] `mvn -q test` passes with no warnings
- [ ] Every command in `README.md` runs verbatim
- [ ] Every claim in `AGENTS.md` matches the source
- [ ] `CODE_REVIEW.md` no longer lists a defect that has been fixed
- [ ] Review with human — this is the point to decide whether the repo is portfolio-ready
