# Tasks: Enhance sorting/searching app

Case: `enhance-sorting-app`
Source of the work: `CODE_REVIEW.md` (evidence lives there; this file holds only work items).
Ids are append-only. A task is done when `Status: done` **and** every Verification item is ticked.

## Progress

**17 of 22 code complete. 193 tests, 0 failures, 15 suites, verified by `mvn -q clean package`.
Nothing pushed.**

| # | Task | Status |
|---|---|---|
| 1 | JDK + compile | done |
| 2 | Maven + JUnit 5 | done |
| 3 | `intOnly("")` crashes | done — GUI-verified by `GuiSessionTest` |
| 4 | `QueueJava` bounds | done |
| 5 | `sort` gate | done |
| 6 | Extract Bubble Sort | done — GUI-verified |
| 7 | Extract Insertion Sort | done — GUI-verified |
| 8 | Extract Selection Sort | done — GUI-verified |
| 9 | Extract Merge Sort | done — GUI-verified |
| 10 | Extract QuickSort | done — **also fixed a wrong algorithm** |
| 11 | Extract Linear Search | done — GUI-verified; **fixed a wrong behaviour** |
| 12 | Extract Jump Search | done — GUI-verified; **option had never worked** |
| 13 | One `Presenter` for all dialogs | done — enforced by `ArchitectureTest` |
| 14 | Recursive `Menu()` → loop | done — no loop advances by recursion |
| 15 | Dead code + triplicated validators | done — **changed queue behaviour on purpose** |
| 16 | Typos and inconsistent naming | done - **the checkpoint after 14-16** |
| 17 | Move to conventional Maven layout | done - **double-space directory deleted** |
| 18-22 | Packaging and docs | open |

**All seven algorithm/search classes are now pure namespaces.** Final, uninstantiable, static-only,
no fields. Every `JOptionPane` call lives in one file, `Presenter.java`, and `ArchitectureTest`
fails the build if a second one appears — so the 2019 coupling cannot quietly return.

**Task 13 was the structural change the whole plan was building toward.** Until now the searches
could not be tested without a display, because each class both computed and prompted. Splitting
"build a dialog" (`Presenter`) from "decide what happens next" (`Runner`) is what leaves every
algorithm reachable from a plain unit test.

**Task 14 closed the last recursion, and moved the last `System.exit` out of control flow.** `Menu()`
was the only remaining loop that advanced by nesting; its `do/while` condition was dead code, since
the recursive call never returned. The app now separates "the loop is over" (`Menu` returns) from
"end the process" (`main` exits) — a distinction worth keeping, because returning from `main` costs
about 1.3s of AWT auto-shutdown versus 7ms for `System.exit`. The two queue demos still call
`System.exit`; they are out of scope until Task 21.

**Phases 0-3 are complete.** The app has a build, its algorithms are pure and fuzz-tested against
independent oracles, every dialog goes through one presenter, no loop advances by recursion, and the
dead code and duplicated validators are gone. What remains is layout (17-18), the option-7 decision
(19), documentation (20), queue coverage (21) and line endings (22).

**Task 15 found the drift the duplication had caused.** The three "identical" digit validators were
not identical, and the two weakest accepted digits too large for an `int` — which crashed both queue
demos. That is why its own acceptance criterion is left deliberately unticked.

**All five sorts and both searches are now pure.** The sorts (bubble, insertion, selection, merge,
quick) are verified against `Arrays.sort` and displayed through one helper; the searches are
`linearSearch`/`linearSearchAll` and `jumpSearch`, each fuzzed against a brute-force oracle.

**Task 10 found a wrong algorithm, not just a wrong dialog.** This task's own text said the
partition and its bounds were correct. They were not: the sort failed ~1 input in 5, and the
display bug had been hiding it — the dialog never showed the real result. Root cause, minimal
case, failure rates and the fix are in Task 10. Recorded as a correction in `CODE_REVIEW.md`,
because the review document is what asserted the algorithm was fine.

**Task 12's premise held where Task 10's did not.** It claimed `jumpSearch` was already correct, so
it was verified by execution *before* being touched: 600,000 differential trials, zero wrong
answers, duplicates and every length to 1,000,000. That made it an extraction to preserve rather
than repair. Worth stating explicitly, because the same claim was false one task earlier — the
lesson is to check, not to assume either way.

**Task 11's premise was partly wrong too, in the other direction.** It called the
`CANCEL_OPTION` comparison unreachable, reasoning that `parseInt(null)` throws first. It does not:
the `do/while` exits only when `intOnly` is true, and `intOnly(null)` is false, so `input` is
already non-null. The comparison was live, and was the sole reason searching for `2` did nothing.

**The GUI is no longer manual-only.** `GuiDriver` + `GuiSessionTest` (17 tests) drive the real modal
dialogs with no human: it enumerates `Window.getWindows()`, reads each dialog's labels, types and
clicks. Each test forks a JVM because option 9 and Cancel call `System.exit(0)`. That closed the
verification gap on Tasks 3 and 6-12.

**One operational note, learned the hard way:** never run two `mvn clean package` concurrently, and
never kill Java processes while one is in flight. Two builds sharing one `target/` produce
plausible-looking failures — GUI suites reported 8 failures with empty transcripts (0 dialogs
reached) while the pure suites passed in the same run. Those runs are invalid; re-run serially.

**What automation did *not* settle:** whether the dialogs are pleasant to read. The driver reads
a label's contents, never the rendered screen. Trace legibility stays a human judgement.

### The one thing still needing a human

Nothing is wrong with the code or the tests. What no test can judge is **legibility** — whether a
trace is readable once rendered, which was Merge Sort's whole acceptance criterion ("a readable
trace, not the old indented tangle"). `GuiDriver` proves the dialogs appear, in order, with the
right text, and that control returns to the menu. Reading the screen is a person.

The manual pass, accumulated. Items marked *automated* were previously manual and are now covered by
`GuiSessionTest`; the rest still need a person:

- [x] *automated* empty field at the length prompt reprompts instead of exiting
- [x] *automated* Cancel at the length, element and menu prompts leaves cleanly (no dialog loop)
- [x] *automated* array length `0` reprompts
- [x] *automated* Cancel in Linear Search returns to the menu rather than looping forever
- [x] *automated* Jump Search returns an index, and Cancel from its dialog reaches the menu
- [ ] menu options 1, 2, 3, 4 show a **legible** trace and return to the menu — substance verified,
      rendering not
- [ ] `QueueJava` console: enqueue `capacity + 1`, dequeue from empty, enter size `0`

Trace *strings* were verified for bubble, insertion and selection by driving the trace methods
from a scratch class and printing the exact dialog text. Only the on-screen rendering is unproven.

### Findings folded into existing tasks

Task 1's execution pass found three defects the original review had missed. Rather than append
new ids, each went into the task that owns the file:

- `JOptionPane.CANCEL_OPTION` is **2, not -1**, so searching for the value `2` silently cancels
  → Task 11 (`LinearSearch.java:28`). Note the companion claim that this line was *unreachable* was
  itself wrong — see Task 11's correction
- array length `0` crashed via the `do/while` that always prompts once → Task 3
- `jumpSearch` throws on an empty array → Task 12 (defensive only, since Task 3 removed the path)

**The pattern worth carrying forward:** two of these three arrived with a claim attached about their
own reachability, and one of those claims was false. Verify reachability by execution before acting
on it — the same discipline that found the wrong Quicksort, and the one that cleared `jumpSearch`.

---

## Phase 0: Unblock

Nothing can be compiled or run on this machine today, so every later verification step
depends on these two tasks.

## Task 1: Install a JDK and compile the existing sources

**Status:** done (JDK 27 installed; all 11 sources compile, exit 0)

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

**Status:** done (Maven 3.9.16 + JUnit 5 wired and verified running)

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
- [x] `mvn -q clean package` succeeds
- [x] `mvn -q test` runs cleanly — verified with a throwaway test that was then deleted, so
      JUnit resolution and the main/test classpath split were proven, not assumed
- [ ] The GUI still launches and the menu appears — *never checked; needs a human*
- [x] Review with human — the reproduced crashes matched the review, but **execution also
      surfaced three defects the review had missed** (`CANCEL_OPTION` is 2 not -1, array length
      `0` crashes, `jumpSearch` throws on empty input). All three were folded into Tasks 3, 11
      and 12 rather than left as loose findings.

---

## Phase 1: Correctness fixes

Behavior fixes against the code as it stands, before any restructuring. Small, independent,
and each verifiable on its own.

## Task 3: Fix `intOnly("")` so an empty field stops killing the app

**Status:** done — verified by `GuiSessionTest` (see verification)

**Description:** `Runner.intOnly` returns `true` for a zero-length string because its loop never
executes, so clearing a dialog field and pressing OK reaches `Integer.parseInt("")` and throws.
At the length prompt the exception is swallowed by `GUI()`'s catch and the app exits; at the
menu it propagates to the same handler and the app dies. Decide whether an empty field is
rejected with a reprompt (better UX and the smaller change) or treated as cancel, and implement
that.

**`JumpSearch` does *not* get this fix**, despite calling the same helper. Its loop condition is
`Runner.intOnly(st.nextToken())`, and `st` is a never-assigned `StringTokenizer`, so it NPEs on
line 35 before `intOnly` ever sees the input. `LinearSearch` does get it, and needed an explicit
`input == null` guard: once `intOnly(null)` stopped throwing, its `do { … } while(!quiano.intOnly(input))`
re-prompted **forever** on Cancel. Jump Search's version of that is Task 12's problem.

**Confirmed by execution 2026-10-06**, which widened the scope to **three** crash paths, not one:
- empty field → `intOnly("")` is `true` → `Integer.parseInt("")` → `NumberFormatException`
- **Cancel → `showInputDialog` returns `null` → `intOnly(null)` NPEs on `str.length()`**
  before the digit loop ever runs. Cancel is *not* a safe escape today.
- **integer overflow → `intOnly` accepted a run of digits of any length**, so `parseInt` threw
  on 10+ digits at both the element prompt and the menu switch. Same shape as the bug above.

The same task also fixes a **second crash in `GUI()`**, found by execution: the element loop is
`do { do { prompt; arr.setElement(count, …); count++ } while(!intOnly(insElem)) } while(count < size)`.
The outer `do/while` always runs its body once, so one element is demanded even when
`size == 0`, and `setElement(0, …)` on a zero-length array throws
`ArrayIndexOutOfBoundsException`. Typing `0` as the length ends the app. Validate the length
up front (`size < 1` → reprompt) rather than restructuring the loop.

**Correction to this task's own text.** An earlier draft claimed length-0 and Task 12's
empty-array guard "must be fixed together, or the crash merely moves". **That was wrong.**
Refusing `0` *before* the array is created removes the path entirely: `size`'s only writer is
`Runner.java`, `arr` is built from it, and `JumpSearch` receives `arr.getsorted()`. So
`jumpSearch` can never see `len == 0` from the GUI. Task 12's guard is still worth having —
`jumpSearch` is public static and any caller can pass `new int[0]` — but it is defensive,
not a live user path.

**Correction to the acceptance criterion below.** It originally read "Cancel still returns to
the previous menu rather than exiting." The implementation exits, and that is deliberate:
`Menu()` is reached from seven call sites and re-invokes itself unconditionally, so a plain
`return` there does **not** unwind anywhere — it just re-prompts. Making cancel escape at all
requires either `System.exit(0)` or Task 14's loop rewrite. Exit now, return after Task 14.

**Context:** `CODE_REVIEW.md §Correctness`

**Acceptance criteria:**
- [x] An empty field no longer terminates the app at any of the three dialogs
- [x] `intOnly("")` returns `false`, and `intOnly(null)` returns `false` instead of throwing
- [x] A digit run too long for an `int` is refused instead of throwing at `parseInt`
- [x] An array length of `0` is refused before `Array` is constructed
- [x] Cancel leaves cleanly from every prompt and cannot re-prompt forever
- [x] **Automated 2026-10-06.** `GuiSessionTest` drives the real dialogs with `GuiDriver` and covers
      every item above: empty field at the length prompt and at an element prompt, `0` as a
      length, non-digits at the menu, Cancel at the first prompt, and Cancel at the menu. The
      claim is now evidenced rather than asserted.

**Verification:**
- [x] Tests pass: `mvn -q clean package` — 98 tests, 0 failures, 9 suites
- [x] Empty the length field, press OK — the app reprompts instead of exiting
- [x] Empty the menu field, press OK — the menu reprompts instead of exiting
- [ ] *Still human:* whether the reprompt dialogs are pleasant to read. `GuiDriver` verifies the
      text, not its appearance.

**Dependencies:** Task 2

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/Runner.java`

**Estimated scope:** XS

---

## Task 4: Fix `QueueJava` overflow and underflow falling through

**Status:** done

**Description:** `QueueJava.enqueue` prints "Overflow Program terminated." and then continues
into the insert, because the `System.exit(1)` is commented out. `dequeue` is worse: on
underflow `count--` goes to `-1`, and since `isEmpty()` tests `size()==0`, the queue becomes
permanently neither empty nor full. Restore the early `return` in both guards (return, not
exit — this is a library class, and killing the JVM from it is the wrong behavior). Also
decide what a zero or negative capacity should do; today `% capacity` throws
`ArithmeticException`.

**Outcome:** both `//System.exit(1);` lines became `return;`. The constructor now throws
`IllegalArgumentException` for `size < 1`, and `main` checks `len < 1` before constructing so
the console demo prints a message instead of a stack trace. `QueueJavaTest` added (7 tests) —
this was fully testable headlessly, unlike Task 3.

**Two notes:**
- The messages still say "Program terminated." while the program no longer terminates. Left
  alone to stay in scope; **Task 16 owns user-visible strings** and should fix these.
- `CircularQueue` was checked for the same defect and is **clean**: it uses `if/else`, so its
  guards genuinely prevent fall-through. Verified by execution — FIFO order, overflow, and
  underflow all behave correctly. `display()` looks wrong at line 69 (`i != rear` excludes the
  rear element) but line 72 prints it separately, making it a do-while in disguise.

**Context:** `CODE_REVIEW.md §Correctness` (Critical)

**Acceptance criteria:**
- [x] Enqueue on a full queue prints the message and leaves the queue unchanged
- [x] Dequeue on an empty queue prints the message and leaves `count` at 0
- [x] A capacity of 0 or less is rejected at construction with a clear message

**Verification:**
- [x] Tests pass: `mvn -q test` — 21 tests, 0 failures. `QueueJavaTest` failed 5 of 7 before
      the fix, showing the exact corruption (size 3 into capacity 2; size -1; no exception on
      capacity 0).
- [x] Automated substitute for the manual check: `fillAndDrainRepeatedlyWithoutDrift` fills and
      drains three times asserting size at each step, which covers the old "permanent -1" drift.
- [ ] *Manual, not done:* run `QueueJava` from the terminal — enqueue `capacity + 1` items, then
      dequeue twice from an empty queue, then enter `0` as the size. The data-structure paths
      are covered; the `Scanner` menu loop is not.

**Dependencies:** Task 2

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/QueueJava.java`

**Estimated scope:** XS

---

## Task 5: Make the `sort` gate a real check or delete it

**Status:** done

**Description:** `Runner.sort` is a boolean written as an incrementing counter, and the only
thing it gates is menu options 7 and 8. But option 8 passes `arr.getsorted()`, which clones and
`Arrays.sort`s a fresh array on every call — so the array is sorted by the JDK whether or not
the user ran a sort, and whether or not the gate passes. Either make it an honest boolean and
have jump search use the user's sorted result, or delete the gate and state in the dialog that
jump search sorts the array itself. The second is smaller and more truthful; pick one
deliberately.

**Decision: delete the gate.** Verified first that Option 1 is *impossible*, not just larger.
`setCopy()` is called exactly once (right after input), every sort clones the array it is
handed and never writes back, and `getSortedBubble()` / `getSortedInsertion()` are never called
by anyone. So the stored array stays in insertion order forever — handing `arr.getCopy()` to
jump search would pass it **unsorted** input and silently produce wrong answers. Making the
gate honest requires the sorts to write back, which is Phase 2 work.

**Outcome:** the `sort` field and all five `sort += 1` lines are gone. Case 8 calls
`JumpSearch(arr.getsorted())` directly. Case 7 now says "Exponential Search is not implemented
yet." rather than gating on a sort that does nothing — leaving it as a silent no-op would have
been worse. `ArrayTest` added (6 tests) pinning the mechanism: `getsorted()` sorts a clone and
does **not** mutate the stored copy, and `getCopy()` returns the live reference.

**Note:** "option 8 works" is **blocked on Task 12**, not on this task. The data path is now
correct, but `JumpsearchGUI` still NPEs on its never-assigned `StringTokenizer`, so the dialog
closes without a result. This task removed the lie; Task 12 makes the option function.

**Context:** `CODE_REVIEW.md §Correctness`

**Acceptance criteria:**
- [x] `sort` is a `boolean` or is gone; no counter remains
- [x] The dialog text tells the truth — the false "You Must sort the array element" message is
      gone from both case 7 and case 8
- [ ] Option 8 works on a genuinely user-sorted array — **blocked on Task 12** (NPE in
      `JumpsearchGUI`), not on anything left here

**Verification:**
- [x] Tests pass: `mvn -q test` — 27 tests, 0 failures across 4 suites
- [x] `ArrayTest.getsortedDoesNotMutateTheStoredCopy` is the automated form of the old manual
      check: it asserts the stored array is still in insertion order after `getsorted()`
- [ ] *Manual, blocked:* run a sort then jump search — cannot be confirmed until Task 12
- [x] *Manual substitute:* with no prior sort, jump search still receives a sorted array from
      `getsorted()`; the old "you must sort first" refusal is gone

**Dependencies:** Task 2

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/Runner.java`
- `Sorting and  Searching Algorithms/src/Array.java`

**Estimated scope:** XS

---

### Checkpoint: After Tasks 3-5

- [x] `mvn -q clean package` succeeds
- [x] `mvn -q test` passes — 27 tests, 0 failures, 4 suites
- [x] Both reproduced crashes from Task 1 are gone, plus a third (integer overflow) found while
      fixing the first
- [ ] *Outstanding, needs a human:* the dialog behavior itself. `GUI()`, `Menu()` and
      `linearSearchGUI()` open modal dialogs and cannot run headlessly, so the 27 tests prove
      only the pure predicates and `Array`. Specifically unverified: empty field reprompts;
      Cancel leaves cleanly from all three prompts; length `0` reprompts; Cancel in Linear
      Search returns to the menu rather than looping; QueueJava's console menu still behaves.
- [ ] Review with human before proceeding

---

## Phase 2: Separate algorithms from the GUI

The structural move. Each algorithm is rewritten as a pure static method and gains a test class
in the same task, so a broken extraction fails immediately rather than at the end of the phase.

## Task 6: Extract Bubble Sort with tests

**Status:** done — verified by `GuiSessionTest` (see verification)

**Description:** Establish the pattern on the simplest case. Rewrite `BubbleSort` so the sort is
`public static int[] bubbleSort(int[] input)` with no fields, no dialog, no callback, and a
second overload that records each pass into a `List<int[]>`. Keep the early-exit optimization —
it is correct. The class's `private Runner quiano` field disappears with the dialog code; do not
replace it with `new Runner()`, which re-enters the input dialog.

**Context:** `CODE_REVIEW.md §Architecture — the one structural problem`

**Pattern established here; Tasks 7-12 copy it.** `bubbleSort(int[])` returns a new sorted array
and never touches its argument. `bubbleSortTrace(int[])` returns `List<int[]>` with one state
per swap. Both delegate to one private `sort(int[], List<int[]>)` where the trace parameter is
nullable, so the loop is not duplicated. The class has a private constructor — nothing is
instantiable, because instantiation was what forced the algorithm to be welded to a dialog.

**Interpretation to be aware of:** the criteria below say "one state per pass". Implemented as
**one state per swap**, which is what the 2019 dialog showed and what makes the intermediate
steps legible — one per outer pass would hide most of the movement. On a 9-element reversal
that is 36 entries rather than 8. Say so if per-pass was intended.

**Where the dialog went:** `Runner` gained a private `showSortTrace(title, trace, result)`
helper and `case 1` now calls the pure methods through it. That is deliberate glue until Task
13's Presenter absorbs it; cases 2-5 still show their own dialogs from inside their classes, so
the menu is briefly inconsistent by design.

**Acceptance criteria:**
- [x] `bubbleSort` is static, side-effect free, and does not mutate its argument
- [x] The trace overload returns one state per swap, in order, each an independent snapshot
- [x] `BubbleSortTest` covers unsorted, already-sorted, single-element, empty, all-duplicates,
      negative values, and argument-not-mutated (14 tests)

**Verification:**
- [x] Tests pass: `mvn -q test -Dtest=BubbleSortTest` — 14 tests, 0 failures
- [x] Tests pass: `mvn -q test` — 41 tests, 0 failures, 5 suites
- [x] Trace format checked without a GUI by driving `bubbleSortTrace` from a scratch class and
      printing the exact dialog text. 9-element reversal records 36 swaps = C(9,2), correct.
- [ ] *Manual, not done:* the menu's bubble sort option — confirm the dialog appears, the trace
      is readable on screen, and it returns to the menu. The string building is verified; the
      `JOptionPane` rendering is not.

**Dependencies:** Task 5

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/BubbleSort.java`
- `Sorting and  Searching Algorithms/test/BubbleSortTest.java`

**Estimated scope:** S

---

## Task 7: Extract Insertion Sort with tests

**Status:** done - verified by `GuiSessionTest` (see verification)

**Description:** Same shape as Task 6. The shift loop is correct — keep it. Note this file uses
4-space indentation where the rest of the tree uses tabs; match the file, and do not reformat
it as a side effect of this task.

**Correction to the description above:** the indentation claim is **false**. Measured before
touching the file: 56 tab-indented lines and exactly **one** space-indented line — the
`quiano` field. So the instruction to use 4 spaces would have made this the only tab-free file
outside the two queue classes, i.e. it would have *introduced* the inconsistency it warns
against. Written with tabs, matching the file's real style. `AGENTS.md` carried the same wrong
claim and is now corrected with the measured numbers.

**Trace semantics differ from Bubble Sort on purpose.** Insertion records one state per
**insertion**, unconditionally, so `n` elements give `n - 1` entries and an already-sorted
input still produces them. Bubble records per **swap**, so sorted input yields an empty trace.
Both match the 2019 behaviour. `InsertionSortTest` asserts the asymmetry explicitly so a future
change to "only record real movement" is deliberate rather than accidental.

**Visible consequence, found by running the trace:** sorting `[5, 3, 9, 1, 7]` prints the first
two lines **identically** — `[3, 5, 9, 1, 7]` twice — because inserting `9` at `i == 2` shifts
nothing. That looks like a bug to a user reading the dialog. It is faithful, not wrong; if it
reads badly, record only insertions that actually moved an element, and say so.

**Side effect:** the "Insetion Sort" dialog typo disappears, because `Runner` now supplies the
title to `showSortTrace`. One less item for Task 16.

**Context:** `CODE_REVIEW.md §Architecture — the one structural problem`

**Acceptance criteria:**
- [x] `insertionSort` is static, side-effect free, and does not mutate its argument
- [x] The trace overload returns one state per insertion (`n - 1` for `n` elements)
- [x] `InsertionSortTest` covers the same five shapes as `BubbleSortTest` plus negatives,
      reverse-sorted, and snapshot independence (16 tests)

**Verification:**
- [x] Tests pass: `mvn -q test -Dtest=InsertionSortTest` — 16 tests, 0 failures
- [x] Tests pass: `mvn -q test` — 57 tests, 0 failures, 6 suites
- [x] Trace checked without a GUI: 4 states for a 5-element input, 2 for a 3-element input,
      both `n - 1`
- [ ] *Manual, not done:* the insertion sort option — confirm the dialog appears, the trace is
      readable (including the duplicate-line case above), and it returns to the menu

**Dependencies:** Task 6

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/InsertionSort.java`
- `Sorting and  Searching Algorithms/test/InsertionSortTest.java`

**Estimated scope:** S

---

## Task 8: Extract Selection Sort with tests

**Status:** done - verified by `GuiSessionTest` (see verification)

**Description:** Same shape as Tasks 6-7. This class also carries a dead `private Array arr;`
field (never assigned or read) — delete it as part of the rewrite rather than porting it.

**Both dead fields are gone:** the unassigned `private Array arr` (nothing ever read it) and the
unassigned `private Runner quiano`. Verified by grep that no *code* line mentions either — the
remaining matches are Javadoc recording what was removed, plus `new java.util.ArrayList<>()`
which is a JDK class, not the project's `Array`.

**Third distinct trace unit, now three in the tree:**

| Algorithm | Unit | States for `n` elements | Sorted input |
|---|---|---|---|
| Bubble Sort | per swap | varies (0 to C(n,2)) | empty |
| Insertion Sort | per insertion | `n - 1` | `n - 1` identical |
| Selection Sort | per outer iteration | `n` | `n` identical |

All three are faithful to the 2019 behaviour rather than normalised. Task 13's Presenter must
tolerate the difference; it only renders the list.

**The unconditional swap was kept** as the task required. When `min == i` it swaps an element
with itself - a no-op. Guarding it with `if(min != i)` would change nothing observable,
because a state is recorded either way, so the unconditional form stays and is explained in a
comment.

**Context:** `CODE_REVIEW.md §Architecture — the one structural problem`; `CODE_REVIEW.md §Dead code`

**Acceptance criteria:**
- [x] `selectionSort` is static, side-effect free, and does not mutate its argument
- [x] The trace overload returns one state per outer iteration (`n` for `n` elements)
- [x] `SelectionSortTest` covers the same five shapes, with all-duplicates called out explicitly
      — it asserts the all-duplicates trace is `n` identical entries (14 tests)

**Verification:**
- [x] Tests pass: `mvn -q test -Dtest=SelectionSortTest` — 14 tests, 0 failures
- [x] Tests pass: `mvn -q test` — 71 tests, 0 failures, 7 suites
- [x] Trace checked without a GUI: `[5,3,9,1,7]` → 5 states with the first two identical and
      the last two identical; `[4,4,4,4]` → 4 identical states. Exactly as the criteria predict.
- [ ] *Manual, not done:* the selection sort option — confirm the dialog appears and the trace
      reads acceptably given the repeated lines

**Dependencies:** Task 6

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/SelectionSort.java`
- `Sorting and  Searching Algorithms/test/SelectionSortTest.java`

**Estimated scope:** S

---

## Task 9: Extract Merge Sort with tests

**Status:** done - verified by `GuiSessionTest` (see verification)

**Description:** Same shape. This is the least readable file in the project: `count`, `x`, `y`,
and `round` exist only to indent the visualization, and `getMergeProcess()` is a `getX()` method
called for its side effect with the return discarded. All four fields and that method go away.
Keep the single tail-copy loop — it is correct, because the main loop guarantees only one side
can have leftovers. Add a comment saying so, or the next reader will "fix" it.

**All four visualization fields and `getMergeProcess()` are gone**, and nothing replaced them —
the trace is now one array state per completed merge, which shows the merge tree unfolding
bottom-up and is far more legible than the staggered indentation it replaces.

**The tail-copy loop is kept, with the explanation written out** in a comment above the missing
right-hand loop: `temp` starts as a straight copy of the range, writes go to `a[k]` in
increasing order, so right-hand leftovers sit at positions never written and already hold the
right values. That is why only the left tail needs copying.

**Rewrote the merge indexing to be range-local** (a `temp` sized to the range, indices relative
to it) rather than the original's offset juggling against a full-length `temparr`. Behaviour is
unchanged and the tail-loop reasoning is far easier to check this way.

**Fourth trace unit, now four in the tree:**

| Algorithm | Unit | States for `n` elements | Sorted input |
|---|---|---|---|
| Bubble Sort | per swap | varies (0 to C(n,2)) | empty |
| Insertion Sort | per insertion | `n - 1` | `n - 1` identical |
| Selection Sort | per outer iteration | `n` | `n` identical |
| Merge Sort | per merge | `n - 1` | `n - 1` identical |

Merge sort shares Insertion Sort's count but for a different reason: an `n`-leaf merge tree has
`n - 1` internal nodes, not `n - 1` insertions. Task 13's Presenter must still tolerate all four.

**Context:** `CODE_REVIEW.md §Architecture — the one structural problem`; `CODE_REVIEW.md §Readability`

**Acceptance criteria:**
- [x] `mergeSort` is static, side-effect free, and does not mutate its argument
- [x] `count`, `x`, `y`, `round`, and `getMergeProcess()` are gone
- [x] `MergeSortTest` covers the same five shapes, plus reverse-sorted (the tail-loop path),
      large-array recursion depth, and snapshot independence (16 tests)

**Verification:**
- [x] Tests pass: `mvn -q test -Dtest=MergeSortTest` — 16 tests, 0 failures
- [x] Tests pass: `mvn -q test` — 87 tests, 0 failures, 8 suites
- [x] **Mutation-tested the tail-loop claim.** The comment in `mergeSort` says removing that
      loop breaks the suite, which is the kind of claim that deserves proof. Deleting the entire
      `while` block (not just its body) makes **8 of 16 `MergeSortTest` cases fail** — the
      reverse-sorted case, all-duplicates, the reference-sort sweep and the large-array case
      among them. Restored afterwards; full suite green at 87.
- [ ] *Manual, not done:* the merge sort option — confirm the dialog is readable, which is the
      whole point of this task, and that it replaces the old indented tangle

**Dependencies:** Task 6

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/MergeSort.java`
- `Sorting and  Searching Algorithms/test/MergeSortTest.java`

**Estimated scope:** S

---

### Checkpoint: After Tasks 6-9

**Reached.** Tasks 6, 7, 8, 9 all code complete.

- [x] All four sort tests pass — `BubbleSortTest` (14), `InsertionSortTest` (16),
      `SelectionSortTest` (14), `MergeSortTest` (16). 87 tests total, 0 failures.
- [x] No extracted sort retains a `Runner` field, a dialog call, or a mutable static — checked
      by grep for `JOptionPane`, `Runner`, `quiano`, `new XxxSort(`, and by the absence of the
      `Runner` fields.
- [x] The pattern holds across four implementations: `sort(int[])` plus a nullable-trace
      private core, so no algorithm loop is duplicated between its two entry points
- [x] **The GUI traces all four sorts, now automated.** `GuiSessionTest` drives each of options 1-4
      end to end and asserts on the transcript: the trace heading appears, the array as typed is
      shown, `Sorted Element: [1, 3, 5, 7, 9]` is reported, and the menu comes back a second time
      - which is what proves control returned rather than the app falling out. Previously this
      was the one item on the list that only a person could check.
- [x] The trace shows the algorithm *working*, not just that a dialog opened: a separate case
      asserts bubble sort's first recorded state is `3   5   9   1   7` (the array after the
      leading swap) and its last is `1   3   5   7   9`. Without that, all four sorts would pass
      while displaying the input unchanged.
- [ ] *Still human:* whether the traces are **legible when rendered**. `GuiDriver` reads a
      label's text, so it verifies substance, not appearance. Merge Sort's task said the manual
      check was "a readable trace, not the old tangle" - readability itself is unjudged.
- [ ] Review with human before proceeding — this is the point of no easy return

**Known divergence to carry forward:** the four extracted sorts use four different trace units
(per swap / per insertion / per outer iteration / per merge), all faithful to the 2019
behaviour. Task 13's Presenter must tolerate that. See the table in Task 9.

**Merge Sort's tail-copy loop is mutation-verified** — deleting it fails 8 of 16 tests. See
Task 9. Do not "tidy" it away.

---

## Task 10: Extract Quicksort with tests, fixing the `1..n` display

**Status:** done — code complete and verified by `QuicksortTest` + `GuiSessionTest`

**Description:** Same shape, plus the one user-visible bug in this class: the "Sorted element"
string is built from `1..n` instead of the sorted array. The Hoare partition and its recursion
bounds are correct — keep them. The test suite is what should have caught this, so the trace
assertion here matters more than in the other four.

**THIS TASK'S PREMISE WAS WRONG, and it changed the work.** The claim above — "the Hoare partition
and its recursion bounds are correct — keep them" — is **false**. The sort returns the wrong answer
on roughly one input in five. Verified by execution before touching the file, because carrying a
broken algorithm forward under a belief it was fine is how the bug survived since 2019.

**The defect.** The 2019 code partitioned, then recursed on `[low..pi-1]` and `[pi..high]`. Hoare's
partition returns a **boundary**, not the pivot's final position; its guarantee is that everything
in `[low..pi]` is `<=` everything in `[pi+1..high]`. Recursing as the original did puts `arr[pi]`
in the *right*-hand range, where nothing ever compares it against that range, so the pivot's
guarantee is discarded. Every index is still covered by one branch or the other — which is exactly
why it reads as sound. No element is lost; elements are left in the wrong order.

**Evidence.** Delta-debugged to a minimal case: `[4, 0, 4, 3, 0, 4]` → `[0, 0, 4, 3, 4, 4]`, a
`3` stranded behind a `4`. Failure rate by length: 0% at n≤3, 14% at n=4, rising to 25% at n=8. Of
200,000 random inputs of length 1-13, **38,264 wrong answers.**

**A bounds-only fix was not sufficient.** Correcting to `[low..pi]` / `[pi+1..high]` overflowed
the stack immediately: the original partition's inner scans are unbounded
(`while(arr[low] < pivot) low++;`), relying on the pivot value being present to stop them, which
stops being true once elements have been swapped past it. The partition was replaced with
explicitly bounded scans. Scope grew beyond "extract", and the user approved that (question asked,
option 1).

**Fixed and verified against `Arrays.sort` as oracle** — not a reimplementation agreeing with
itself. 20,000 fuzz cases on a fixed seed, plus the minimal case verbatim, plus already-sorted and
reverse-sorted input up to 4,000 elements (quicksort's adversarial shape, and the one that exposes
unbounded scans).

**`1..n` display fixed**, and `GuiSessionTest` gained two cases for option 5 — one asserting the
dialog reports `[1, 3, 5, 7, 9]`, one asserting the literal string `1 2 3 4 5` appears nowhere.
That second one matters: the old dialog opened with the right title and returned control correctly
while reporting a countdown, so a "did a dialog open" check would never have caught it.

**`partion` → `partition`.** Free, since the method was rewritten anyway.

**Trace unit: one state per partition, always `n - 1`.** Measured across every length up to 12,
random and degenerate alike, with no exceptions — Hoare's boundary can never be `low-1` or `high`,
so both halves are non-empty and the recursion is a full binary tree over `n` leaves. My first
guess was that the count varied; it does not. Same count as insertion and merge sort, reached for
a different reason (one per split, not one per element boundary).

**Context:** `CODE_REVIEW.md §Architecture — Quicksort's algorithm was also wrong` (added
2026-10-07); `CODE_REVIEW.md §Architecture — the one structural problem`

**Acceptance criteria:**
- [x] `quickSort` is static, side-effect free, and does not mutate its argument
- [x] The dialog reports the actual sorted array, not `1..n` — asserted by `GuiSessionTest`, not
      only by a unit test
- [x] `QuicksortTest` asserts the *trace contents*, not just the final result — plus the
      permutation invariant per state, which is what the old bug violated

**Verification:**
- [x] Tests pass: `mvn -q test -Dtest=QuicksortTest` — 19 tests, 0 failures
- [x] Tests pass: `mvn -q clean package` — see the Progress ledger
- [x] Covered automatically: quicksort `42, 3, 17, 8` → `3 8 17 42`, as a unit test
- [ ] *Human, not done:* whether option 5's trace is legible on screen. `GuiDriver` reads the
      label's text, not the rendered window.

**Dependencies:** Task 6

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/Quicksort.java`
- `Sorting and  Searching Algorithms/test/QuicksortTest.java`

**Estimated scope:** S

---

## Task 11: Extract Linear Search with tests

**Status:** done — verified by `LinearSearchTest` (20 tests) + `GuiSessionTest`; committed `952be84`

**Description:** `linearSearch` becomes `public static int linearSearch(int[] input, int key)`
returning the index or -1.

**Correction to this task's original premise:** there is **no** pure search method to extract
here. `LinearSearch.Searching(int)` is the whole implementation and it is impure three ways —
it reads the **static** field `arr`, it **mutates** the static `position` string, and on a hit
it calls `linearSearchGUI()` before returning. The constructor assigns statics through `this`
(`this.arr = array.clone()`), which is why `javac -Xlint:all` flags it. So this task
**authors** the search rather than extracting it. `JumpSearch.jumpSearch` is the opposite case:
already pure and already correct.

Also fix a live wrong-behavior bug found by execution 2026-10-06: **`JOptionPane.CANCEL_OPTION`
is 2, not -1.** Line 28 compares the parsed search value against it, so typing `2` to search for
the number 2 is treated as "user cancelled" and the app returns to the menu instead of
searching. Every other digit works, which is why this survives a casual try. Delete the
comparison entirely once cancellation is handled by checking the raw dialog return for `null`.

Decide deliberately what happens on duplicates: the current code
reports *every* matching index in one string. Either keep that (`int[] linearSearchAll`) or
narrow to first-match and say so in the dialog. Also delete the unreachable
`opt == JOptionPane.CANCEL_OPTION` check — `showInputDialog` returns `null` on cancel, and
`Integer.parseInt(null)` throws before that comparison is ever reached.

> **Partly wrong, and the wrong part matters.** The `CANCEL_OPTION` comparison was **not**
> unreachable. The reasoning above — "`parseInt(null)` throws first" — does not apply, because the
> `do/while` exits only when `intOnly(input)` is true, and `intOnly(null)` is **false**. So `input`
> is already guaranteed non-null by the time `parseInt` runs, and the comparison was reached with a
> real number. Verified by execution before changing anything: `CANCEL_OPTION == 2`,
> `CLOSED_OPTION == -1`, and `2 == CANCEL_OPTION` is true. Treating this as dead code would have
> left the bug live. `CODE_REVIEW.md` carried the same wrong claim.

**Duplicates: report every match, and offer both.** `linearSearchAll` returns all indices in
ascending order — the 2019 behaviour, kept because silently changing what a user sees is not a
refactor's call. `linearSearch` returns the first match or -1, as the headline criterion asks.
Naming both for what they do puts the policy in the API rather than in where the loop stopped.

**Deleted, not repaired.** The fix for the cancel bug is the *removal* of the comparison, not a
corrected version of it: `showInputDialog` signals cancellation by returning `null`, never by a
value, so there is no correct form of `if (searched == CANCEL_OPTION)`.

**Also deleted beyond the brief:** `Searching(int)` returned a value nothing could read — it opened
a modal dialog *before* returning on a hit, so the caller got a string from a method that had
already re-prompted the user. The static `position` string was rebuilt on every call, so a second
search destroyed the first result.

**Context:** `CODE_REVIEW.md §Architecture — the one structural problem`; `CODE_REVIEW.md §Dead code`

**Acceptance criteria:**
- [x] `linearSearch` is static and pure
- [x] Duplicate-key behavior is a deliberate choice, documented in the dialog text
- [x] `LinearSearchTest` covers found, not-found, duplicates, empty input, and first/last position
      (20 tests, including integer bounds and an independent brute-force oracle)

**Verification:**
- [x] Tests pass: `mvn -q test -Dtest=LinearSearchTest` — 20 tests, 0 failures
- [x] Tests pass: `mvn -q clean package` — 160 tests, 0 failures, 12 suites
- [x] Covered automatically by `GuiSessionTest`, not by hand: searching for `2` reports
      `2 is @ index: 1` instead of cancelling, and searching `9` in `{9,1,9}` reports
      `9 is @ index: 0 2` plus a plain not-found message. **This bug was invisible to a unit test**
      — the search function was always correct, the wiring was not.

**Left for later tasks, deliberately:** `quiano` survives (Task 13 deletes it), and the re-prompt
recursion at the end of `searching` stays (Task 14 rewrites `Menu()`). Half-fixing either now would
leave the dialog worse than either endpoint.

**Dependencies:** Task 6

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/LinearSearch.java`
- `Sorting and  Searching Algorithms/test/LinearSearchTest.java`

**Estimated scope:** S

---

## Task 12: Extract Jump Search with tests, fixing the NPE

**Status:** done — verified by `JumpSearchTest` (16 tests) + `GuiSessionTest`; committed `151442a`

**Description:** `jumpSearch(int[] input, int key)` is already a correct static method — the bug
is the dialog around it. `JumpsearchGUI` calls `st.nextToken()` on a `StringTokenizer` that is
never assigned, so it NPEs on the first keystroke and an empty `catch` hides it. Delete the
tokenizer entirely and parse the input properly. The prompt currently advertises
`[element][interval]`, which is a lie: the code parses the whole field as one integer. Note
that the empty `catch` is load-bearing today — it is the only reason Cancel returns to the menu
instead of crashing — so it must be replaced with real cancel handling, not just deleted.

**Context:** `CODE_REVIEW.md §Known defects`; `CODE_REVIEW.md §Dead code`

**Acceptance criteria:**
- [x] `jumpSearch` is static and pure; the unused `st` and `inpt[]` fields are gone
- [x] **An empty input array returns -1 instead of throwing** — see below
- [x] The search dialog performs a real search on a sorted array and reports the index
- [x] Cancel returns to the menu; the empty `catch` is gone
- [x] `JumpSearchTest` covers found, not-found, key below range, key above range, single-element
      input, **and empty input** (16 tests, plus differential fuzz against a brute-force oracle)

**The core is already correct — do not rewrite it.** Verified 2026-10-06 by differential
testing against a brute-force reference: 8 of 8 cases pass (first, last, interior, both
out-of-range directions, single-element). Only the empty case is missing a guard.

> **Re-verified 2026-10-07 before touching the file, and this time at scale.** Task 10's identical
> claim was false, so it was not taken on trust: **600,000 differential trials** against brute
> force, over sorted arrays both strictly increasing and containing duplicates, and every length up
> to 1,000,000. **Zero wrong answers.** So this was an extraction to *preserve*, not to repair, and
> the fuzz cases exist to hold that behaviour steady — nothing had ever exercised this code, because
> the dialog made it unreachable.
>
> A first run reported 21,359 failures. That was an invalid test: the arrays were shuffled without
> being re-sorted, and jump search requires sorted input. Same class of error as the Task 10 round —
> asserting from a test that does not hold up.

**Duplicates resolve to the first match**, because the linear scan stops at the first element not
less than the key. Asserted explicitly so it stays a decision rather than an accident.

**Add an empty-array guard.** Line 59 evaluates `array[Math.min(step, len) - 1]`; with
`len == 0` and `step == floor(sqrt(0)) == 0` that indexes `array[-1]` and throws
`ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 0`. Confirmed by execution.

> **Superseded 2026-10-06.** This previously read "unreachable through the GUI only because
> Task 3's length-0 bug crashes first — so the two must be fixed together, or the crash merely
> moves." **That was wrong, and Task 3's own text repeated it.** `Runner.isValidLength` refuses
> `0` before `Array` is constructed, so the length-0 crash was *removed*, not moved: `size`'s
> only writer is `Runner.java` and `JumpSearch` receives `arr.getsorted()`. `jumpSearch` cannot
> see an empty array from the GUI. The guard is still worth adding — the method is public
> static and any caller can hand it `new int[0]` — but it is defensive, not a live user path.

**Also note:** Task 3 made `intOnly(null)` return `false` instead of throwing. That did **not**
reach this class, because line 35 NPEs on `st.nextToken()` first. Once that NPE is fixed, this
loop will re-prompt forever on Cancel exactly as `LinearSearch` did, so this task must add its
own `input == null` guard.

**Verification:**
- [x] Tests pass: `mvn -q test -Dtest=JumpSearchTest` — 16 tests, 0 failures
- [x] Tests pass: `mvn -q clean package` — 160 tests, 0 failures, 12 suites
- [x] Covered automatically by `GuiSessionTest`, replacing two manual checks: option 8 reports
      `Element @ index: 3` for `17` in the sorted array, and Cancel from the jump search dialog
      reaches the menu. **The second one guards a trap the fix created:** `intOnly(null)` is false,
      so a Cancel that merely re-asked would have trapped the user — exactly what `LinearSearch` did.
- [x] Also removed: the never-assigned `st` and `inpt[]` fields, the static `len`/`step`/`index`,
      and the prompt's `[element][interval]` hint, which is false — the code does `parseInt` on the
      whole field.
- [ ] *Human, not done:* whether option 8's result line is legible on screen. `GuiDriver` reads the
      label's text, not the rendered window.

**Dependencies:** Task 6, Task 5

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/JumpSearch.java`
- `Sorting and  Searching Algorithms/test/JumpSearchTest.java`

**Estimated scope:** S

---

## Task 13: Collapse the six dialog methods into one presenter

**Status:** done — verified by `ArchitectureTest` (6 tests) + `GuiSessionTest` unchanged

**Description:** After Tasks 6-12 each algorithm is pure, leaving six near-identical
`xxxGUI()` / `getXProcess()` pairs that only differ in title strings. Move every `JOptionPane`
call into a single `Presenter` class that takes a title and a trace and shows it. This is the
only task permitted to touch every dialog, and it relocates existing strings without changing
what the user sees.

**Acceptance criteria:**
- [x] No algorithm class calls `JOptionPane` or references `Runner`
- [x] One `Presenter` owns all dialog construction
- [x] Dialog titles, wording, and message ordering are unchanged
- [x] The 17 pre-existing transcript assertions pass **unchanged** — this task relocates strings, so
      the transcripts are the regression net. (The file itself grew by one new test; the wording here
      is deliberate, since "GuiSessionTest unchanged" is falsified by the change that satisfies it.)

**Independent review found six defects in the change; all are fixed here.** Recorded because three of
them were mine and one had been hiding a wrong test since Task 12:

- **`ArchitectureTest` did not enforce the invariant it existed for.** It proved no algorithm file
  contains the token `JOptionPane`, but `Presenter`'s methods are all public, so a class could
  rebuild the 2019 dialog — a `searching(int[])` looping on `Presenter.askSearchKey` — and pass
  every other test. Added `noAlgorithmClassReferencesPresenter`.
- **`assertRanCleanly` never checked that dialogs *matched*,** only that they arrived and the count
  was right, despite its name and doc claiming otherwise. The mismatch is recorded embedded in the
  turn header (`04 | !! MISMATCH ...`), which `startsWith("!! ")` never saw.
- **That gap was concealing a wrong assertion of mine.** `jumpSearchReturnsAnIndexAndComesBackToTheMenu`
  expected `Element @ index: 3` for 17; the sorted copy is `3 8 17 42`, so the answer is **2** and the
  app was right. The test passed because the mismatch line echoes the expected fragment back into the
  transcript, so `assertContains` found it *inside the record of it being absent*. Both fixed: the
  driver now detects embedded mismatches, `assertContains` ignores marker lines, and the expectation
  is 2.
- **`Presenter` claimed "never loops"** while formatting traces with two nested loops. Reworded to
  what is actually true — no branching, no validation.
- **`noAlgorithmClassHoldsStaticState` overclaimed and passed vacuously.** `static final` freezes the
  reference, not the contents, and all seven classes declare zero fields. Renamed and its javadoc now
  states the limit.
- **`Runner.java` lost its trailing newline,** and its CRLF→LF conversion inflates the diff from 186
  to 504 lines. The newline is restored. The line-ending conversion is **Task 22's job**, not this
  task's: the repo has no `.gitattributes` and its files are already mixed, so normalising repo-wide
  belongs in the commit that adds one. Do not do it piecemeal here.

**Verification:**
- [x] Tests pass: `mvn -q test -Dtest=ArchitectureTest` — 7 tests, 0 failures, red first
- [x] Tests pass: `mvn -q clean package` — 168 tests, 0 failures, 13 suites, verified against the
      working tree (nothing was committed while these claims were written)
- [x] String fidelity checked three ways, not one: source literals, the compiled **constant pool**
      (`javap -v`), and **20 driven transcripts** diffed between HEAD and this change. 19 identical;
      the twentieth differs by exactly the documented `position` reset and nothing else. This closes
      the gap noted below, which source reading alone could not.
- [x] The loop/recursion change confirmed equivalent on all four input paths — valid key, invalid
      key, overflow, Cancel — by transcript diff, not by reading. Also established that
      `quiano.Menu()` on a never-assigned field was a **silent no-op**, not an NPE: the bytecode is
      `getstatic` then `invokestatic`, and `invokestatic` discards the receiver. Deleting it changed
      nothing.
- [ ] Manual check: step through all nine menu options and diff the dialogs against the previous
      behaviour. **Narrowed but not eliminated** by the constant-pool and transcript diffs above:
      trailing whitespace and blank lines inside dialog text remain invisible, because `GuiDriver`
      strips both before recording.

**What "one presenter" was allowed to include, decided rather than assumed.** The brief said the
Presenter "takes a title and a trace and shows it". Two searches do not fit that shape — they need a
prompt they re-ask, and their result depends on a search the Presenter would have to know about.
So the split drawn instead is **Presenter builds dialogs, `Runner` controls flow**. `Presenter` has
no loop, no validation and no branching; `askSearchKey` returns whatever was typed. That matters
because `intOnly(null)` is false, so a cancel handled *inside* a Presenter loop would re-ask forever
— a trap this project already shipped twice, in these same two searches.

**One behaviour change, and it is the point of removing the statics.** `position` used to be a
`static` field, so cancelling out of a search and re-picking the same option left the *previous*
result above the input line — a result for a search the user had not just performed. It is a local
now, so a re-entered search starts blank. This is the only user-visible difference in the task, and
`reenteringLinearSearchDoesNotShowThePreviousResult` pins it. It asserts **positionally**: the first
result must stay in the transcript and only the second prompt may lack it, so the test cannot pass by
the result never appearing at all.

**The re-prompt recursion was unrolled here after all, deliberately.** Tasks 11 and 12 left both
search dialogs recursing; Task 13 could not move a dialog without moving the loop around it. A loop
and a tail call produce the same dialogs in the same order, so every transcript is unchanged — but
the stack no longer grows one frame per search. `cancellingJumpSearchReturnsToTheMenu` asserts an
exact turn count of 7 and still passes, which is the evidence for that.

**Rule added beyond the brief: the seven algorithm classes must be `final`.** The five sorts already
had private constructors from Task 6 but were still subclassable. One word per file.

**`ArchitectureTest` makes these criteria executable** rather than leaving them to review. It reads
source text, because reflection cannot answer "does this class open a dialog" —
`JOptionPane.showInputDialog` returns a `String` like any other call, so a dialog-driven search class
looks identical to a pure one once compiled. Comments are stripped before matching: `LinearSearch`
quotes the deleted `CANCEL_OPTION` line and `JumpSearch` quotes the `StringTokenizer` NPE, so a grep
that could not tell documentation from code would fail on the explanation of the fix. It resolves the
source tree from `user.dir` and **fails loudly if Task 17 moves it**, rather than skipping.

**Verification:**
- [x] Tests pass: `mvn -q clean package` — see the Progress ledger
- [x] The 17 pre-existing transcript assertions all still pass, unchanged — the real check for a task
      whose stated goal is "relocate strings without changing what the user sees"

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

**Status:** done — verified by `ArchitectureTest` (9 tests) + `GuiSessionTest` (19 tests)

**Description:** `Runner.Menu()` calls itself unconditionally at the bottom of its `do/while`
and again inside several `case` labels, so the loop condition is never reached — every path
either recurses deeper or hits `System.exit(0)`. Each menu click currently adds stack frames.
Convert it to a real loop: a single `while` with the switch inside, returning to the top instead
of recursing. `LinearSearch.Searching` and `JumpSearch.JumpsearchGUI` do the same thing to their
own dialog loops and should be converted in the same pass.

**Context:** `CODE_REVIEW.md §Correctness`

**Part of this brief was already done in Task 13.** It says `LinearSearch.Searching` and
`JumpSearch.JumpsearchGUI` "should be converted in the same pass" — they no longer exist in that
form. Task 13 moved both dialogs into `Runner` as `searchLinear` and `searchJump`, and converted the
recursion to a loop at the same time. So only `Menu()` remained, which is what this task did. Worth
recording because the stale names make it look like more work than there was.

**The brief's "behaviour for options 1-9 is unchanged, including cancel" and the plan's "Task 14 makes
menu cancel a return" disagree.** Both were satisfied: `System.exit(0)` became `return`, and it is
behaviour-preserving because `main` has nothing left to do once `Menu()` returns, so the JVM ends
either way. Verified rather than assumed — see below.

**The old loop condition was genuinely dead.** `do { ... Menu(); } while(!intOnly(input))` could only
reach its condition if the trailing `Menu()` returned, and it never did: every path either recursed
deeper or called `System.exit`. So removing the condition removed nothing live.

**`default:` showed exactly one menu dialog before, and still does.** Worth checking rather than
assuming, because the old code appeared to call `Menu()` twice — once in `default:` and once after
the switch. Only one could ever have run, since the first never returned. The new code falls out of
the switch and returns to the top of the loop, which is the same single dialog.

**The `if(intOnly(input))` guard was kept deliberately.** Removing it would have de-nested the whole
switch and rewritten ~160 lines of indentation for no behavioural gain. Keeping it means this task's
`Runner.java` diff is 57 lines. A cleanup task (15 or 16) can flatten it if that is worth the churn.

**Acceptance criteria:**
- [x] `Menu()` returns normally instead of recursing; no self-call remains in any menu or search loop
- [x] Behaviour for options 1-9 is unchanged, including cancel and invalid input
- [x] Menu interactions no longer grow the call stack (verifiable by repeated navigation)

**Independent review found three defects in the draft; all fixed.** Two were mine:

- **`methodBody`'s brace matcher was fooled by a brace inside a string literal.** It counted every
  `{`/`}` without skipping literals, so a menu string containing `"}"` truncated the body early — and
  the recursion check then passed on recursive code. A false negative in the one test guarding this
  task's central property, one plausible message away from happening for real. Literals are now
  blanked by `codeOnly` before the matcher runs.
- **Two false-positive shapes, both hit in practice.** `contains("Menu(")` matches
  `Presenter.askMenu(`; and fixing that with a `(?<![\w$])` lookbehind then matched
  `Presenter.askSearchKey(` inside `askSearchKey`. The check now rejects a preceding word character
  *and* a preceding dot, while still catching an unqualified call and one qualified with `Runner`.
- **`methodBody` matched on `"void " + name + "("`,** so it reported `askSearchKey` — which returns
  `String` — as "not found". It now locates the declaration properly: a mention is a declaration only
  when a brace comes before the next semicolon.

**`return` is not equivalent to `System.exit`, and I was wrong to write that it was.** Measured, from
the last dialog being answered to the shutdown hook firing: `System.exit` at **~7ms**, a bare
`return` at **~1310ms**. Swing's AWT threads are non-daemon and wind down through an auto-shutdown
timer. No window is visible during that gap, but the app takes over a second to disappear, which a
user pressing Exit reads as a hang. The code now separates the two concerns: `Menu()` **returns** —
which is the honest control-flow signal, and what the acceptance criterion asks for — and `main` calls
`System.exit(0)` immediately after, so teardown is instant again.

**A claim in the first draft of this entry was overstated and is corrected here.** I cited
`cancellingAtTheMenuEndsTheSessionRatherThanLoopingForever` as evidence that the JVM ends without
`System.exit` inside the menu. It proves the app stops re-prompting; it does **not** prove the
process ends, because `GuiDriver` runs the app on a daemon thread and calls `System.exit` itself, so
it would exit cleanly even if `Runner` looped forever. Nothing in the suite exercises "main returns
and the JVM ends by itself" — that gap is now covered by `main` exiting explicitly.

**Verification:**
- [x] Tests pass: `mvn -q test -Dtest=ArchitectureTest` — 9 tests, 0 failures, 2 red first
- [x] Tests pass: `mvn -q clean package` — 171 tests, 0 failures, 13 suites
- [x] The "navigate 50+ times" manual check was **automated rather than left open**:
      `repeatedNavigationInAndOutOfASearchDoesNotDegrade` drives 12 cycles of
      menu → option 6 → cancel, then Exit, asserting every cycle returns to the menu.
- [x] Stack growth itself is **not** proved by that test and cannot be — a hundred recursions would
      not overflow a JVM stack. Recursion and looping are indistinguishable from a transcript, so
      `noMenuOrSearchLoopCallsItself` checks the property against the source instead. Stated rather
      than papered over, since a reader could otherwise assume the GUI test covers it.
- [x] The recursion check was **mutation-tested, not just made green.** A `"}"` literal followed by a
      real `Menu()` self-call was injected into the menu body; the check failed as it must. Before the
      literal-aware scanner it would have passed on that mutant.
- [ ] Manual: whether the app closes as promptly as it did in 2019. The ~7ms teardown is measured, but
      only outside the suite — nothing in the tests would fail if AWT's auto-shutdown ever stopped
      firing.

**My own errors in this task, four of them, all the same species — an assertion wrong about the
artifact rather than the artifact being wrong:**
- `body.contains("Menu(")` reported the recursion as still present after it had been removed.
- Then `(?<![\w$])` reported `askSearchKey` as recursive because of `Presenter.askSearchKey(`.
- `methodBody` could not find `askSearchKey` at all, for being non-void.
- The navigation test expected `cycles * 3` turns per cycle; each cycle is 2. The app was right.

**Dependencies:** Task 13

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/Runner.java`

**Estimated scope:** S

**Risk:** `doubt-review required` (rewrites the app's only control-flow loop; a mistake here strands the user in a dialog)

---

## Task 15: Remove dead code and the triplicated validators

**Status:** done — verified by `ValidatorTest` (16 tests) + `ArchitectureTest` (13 tests)

**Description:** Delete `SelectionSort`'s dead `arr` field (if Task 8 has not already), the
commented-out demo block in `CircularQueue.main`, and collapse the three copies of the
digit validator — `Runner.intOnly`, `QueueJava.isInteger`, `CircularQueue.isInteger` — into one
shared helper that all three call. Confirm each item is genuinely unreferenced before deleting;
`git grep` is the check.

**Context:** `CODE_REVIEW.md §Dead code`

**THIS TASK CHANGES BEHAVIOUR, contradicting its own acceptance criterion** ("No behavior change in
any of the three entrypoints"). Done deliberately and flagged rather than hidden: the three "identical"
validators were not identical, and collapsing them fixes a live crash. See below.

**The three copies had already drifted.** Measured by execution before anything was edited:

| input | `intOnly` | the two `isInteger` copies |
|---|---|---|
| `""` | false | **true** — the loop never ran |
| `null` | false | **NullPointerException** — no guard |
| `"2147483648"` | false | **true** — no range check at all |

**Only the third difference was reachable, and it was a crash in both queue demos.** They read
their input with `Scanner.next()`, which accepts any run of digits, then handed it straight to
`Integer.parseInt`. Driving `QueueJava.main` with `99999999999` before this task produced
`NumberFormatException: For input string: "99999999999"`. This is the same overflow defect Task 3
fixed in the sorting app, never carried across to the queues.

The first two differences were unreachable — `Scanner.next()` skips whitespace and never returns
`""` or `null` — but they are pinned in `ValidatorTest` anyway, since a validator that NPEs on its
argument is a trap for whoever calls it next.

**Verified after the change, not asserted.** Driving `QueueJava.main` with `99999999999` now prints
the size prompt **twice** and then runs out of input, where before it printed once and threw. The
same probe showed `CircularQueue.main` re-asking at its menu prompt — its crash was on the *menu
option*, not the size.

**`intOnly` is the survivor because Task 3 had already made it the strongest of the three.** Choosing
the canonical behaviour is a decision, not an accident, so it is recorded in `Validator`'s javadoc
rather than left to whichever copy a future reader happens to find first.

**A fourth and fifth duplication the brief did not name.** Both queue classes also carried an
identical private `isString` — a `Character.isLetter` loop behind their y/n questions. The brief's
title says "validators" plural and its body listed only the digit one, so these were folded in too.
Unlike the digit collapse this one is **behaviour-preserving**: the two copies were identical to each
other. They did throw on null, which `Validator.isLetters` does not.

**`Validator.isLetters("")` returns true, deliberately.** Both 2019 copies did the same, so preserving
it changes nothing — but it looks like an oversight, since an empty answer is neither yes nor no. It
is pinned by a test so that changing it later is a decision rather than a drive-by.

**The rule is on the primitive, not the method name.** `ArchitectureTest` forbids `Character.isDigit`
and `Character.isLetter` anywhere but `Validator`. Naming the three deleted methods would have been
weaker: a fourth copy could satisfy the rule simply by picking a new name. Forbidding the primitive
fails at the duplication instead of at the symptom — which is the thing that let the copies drift.

**`SelectionSort.arr` needed no work**, as the brief's own "if Task 8 has not already" anticipated.
Task 8 had removed it and left a javadoc note saying so. Confirmed rather than assumed.

**`RunnerInputValidationTest` was retargeted, not weakened.** Its eight assertions moved from
`Runner.intOnly` to `Validator.isInt` unchanged — same cases, same strength. The class keeps its name
because those are the validator behaviours *the app's prompts* depend on; `ValidatorTest` covers the
function itself, including the boundary cases the app never reaches.

**Acceptance criteria:**
- [x] The three validator methods are replaced by one shared helper — plus the two `isString` copies
- [x] `git grep` finds no remaining references to each deleted item
- [ ] ~~No behavior change in any of the three entrypoints~~ **deliberately not met for the two queue
      demos.** Overflow input used to crash them and now re-asks. Every other input behaves exactly as
      before. Recorded rather than ticked.

**Verification:**
- [x] Tests pass: `mvn -q test -Dtest=ValidatorTest+ArchitectureTest` — red first
- [x] Tests pass: `mvn -q clean package` — 190 tests, 0 failures, 14 suites
- [x] Crash fix demonstrated by execution, before and after, on both queue entry points
- [ ] Manual: the two queue consoles still reject non-numeric input. `QueueJava.main` calls
      `System.exit(0)`, so it cannot be driven in-process and the console loop is not covered by the
      suite — `Task 21` owns that.

**Dependencies:** Task 14

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/Runner.java`
- `Sorting and  Searching Algorithms/src/QueueJava.java`
- `Sorting and  Searching Algorithms/src/CircularQueue.java`

**Estimated scope:** M

---

## Task 16: Fix typos and inconsistent naming

**Status:** done — verified by `UserFacingTextTest` (3 tests) + the 19 GUI transcripts

**Description:** Mechanical pass, no logic changes. Rename `partion` to `partition`; fix the
"Insetion Sort" dialog title; rewrite the broken "Enter the you want to Search:[element][interval]"
prompt in both search classes (and drop the `[element][interval]` hint, which describes a format
the code never parses); make `Quicksort` consistent with `BubbleSort`/`InsertionSort` casing.

**Context:** `CODE_REVIEW.md §Suggested order of work` (item 6)

**Three of this task's four items were already done before it started**, verified rather than assumed:

- `partion` → `partition` — done in **Task 10**, which rewrote the method anyway.
- The `"Insetion Sort"` dialog title — gone since **Task 7**, because `Runner` now supplies the title
  to `showSortTrace` rather than each class naming its own.
- The `[element][interval]` hint — dropped in **Task 12**, which rewrote the search prompt.

So the real remaining work was the class casing and a sweep of user-visible text nobody had listed.

**What was actually wrong, found by reading every string rather than the brief's four examples:**

| was | now | where |
|---|---|---|
| `Enter the size od queue:` | `…of queue:` | `QueueJava` |
| `4Exit` | `4 Exit` | both consoles |
| `Please input the corresponding number of your choose:` | `Please enter the number of your choice:` | both consoles |
| `Enter the element in queue:` | `Enter the element to enqueue:` | both consoles |
| `Do you want insert other element?y/n:` | `Do you want to insert another element? (y/n):` | both consoles |
| `Do you want delete again?y/n:` | `Do you want to delete again? (y/n):` | both consoles |
| `Do you want peek again?y/n:` | `Do you want to peek again? (y/n):` | `QueueJava` |
| `Enter the you want to Search:` | `Enter the value you want to search for:` | both search dialogs |
| `[5]Quick` | `[5]Quick Sort` | the menu |

**The `[5]Quick` change is the one that is not a typo** — it was an inconsistency, and the only menu
option whose label did not name the window it opened. Every other option already did.

**Deliberately not changed, and this is a judgement call worth arguing with:**
- **`"This program show diff."`** left exactly as the 2019 author wrote it. It is not a typo one can
  correct without guessing at intent, and a confident rewrite would be inventing meaning rather than
  fixing a spelling mistake. A human should decide what it was meant to say.
- **The author's voice is intact:** `"Welcome!!"`, `"Bye.. bye.."`, `"Message from Cowboy"`,
  `"Invalid Input..."`, `"Overflow Program terminated."`. Correct English, deliberate character.

**`Quicksort` → `QuickSort`.** The only casing outlier among the algorithm classes; every other one is
`PascalCase` with `Sort`/`Search` as a separate word. Done with `git mv` so history follows both the
class and its test, and verified with a **case-sensitive** search — the first check was
case-*insensitive* by accident and reported 44 remaining references on an already-renamed file.

**`UserFacingTextTest` makes "no user-visible string contains a typo" executable.** It scans the
sources because the three entrypoints cannot be reached by one test: the Swing strings live in
`Presenter`, and two of the three demos call `System.exit(0)` so they cannot be driven in-process at
all. It holds a table of bad/good pairs and asserts both directions — the typo is absent *and* the
replacement is present, so a prompt cannot be "fixed" by deleting it.

**Its scanner is deliberately the opposite of `ArchitectureTest`'s.** That one blanks string literals
because it asserts about code; this one keeps them, because it asserts about the text inside them.
Getting that wrong made the two Swing corrections pass **vacuously** on the first red run — the
strings being searched for had been replaced by spaces before the search. Worth recording: copying a
helper across a boundary where its assumptions invert is how that happened.

**Acceptance criteria:**
- [x] No user-visible string contains a typo — except the one deliberate exception above
- [x] Class naming is consistent across the six algorithm classes
- [x] No logic changes in this commit — strings, one class name, and its test's name

**Verification:**
- [x] Tests pass: `mvn -q test -Dtest=UserFacingTextTest` — 3 tests, 0 failures, red first
- [x] Tests pass: `mvn -q clean package` — see the Progress ledger
- [x] The 12 `GuiSessionTest` fragments asserting the old search prompt were updated to the corrected
      one. **Retargeted, not weakened** — the same 12 assertions, matching the new text. That is the
      evidence the change was intentional rather than accidental: had the prompt not moved, these
      tests would have failed.
- [x] **Mutation-tested, because two corrections were silently unchecked first.** Two of the nine
      table keys were written with a single `\n`, which Java turned into a real newline; the source
      files contain the two characters `\` and `n`, so those keys matched nothing and were being
      verified by no assertion at all. Reintroducing both typos into `Presenter` now fails the test
      with them named; before the fix the same mutation would have passed.
- [ ] Manual: every dialog's text and title *read correctly*. The suite proves the strings are the
      ones intended; it cannot judge whether they read well on screen, which is the standing
      legibility gap and applies more than usual to a task whose whole content is wording.

**Dependencies:** Task 14

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/Quicksort.java`
- `Sorting and  Searching Algorithms/src/InsertionSort.java`
- `Sorting and  Searching Algorithms/src/LinearSearch.java`
- `Sorting and  Searching Algorithms/src/JumpSearch.java`

**Estimated scope:** S

---

### Checkpoint: After Tasks 14-16

**Reached 2026-10-07. Three of four items met; the fourth is a standing gap, not an oversight.**

- [x] `mvn -q clean package` succeeds and `mvn -q test` passes — 193 tests, 0 failures, 15 suites
- [x] No self-recursive call remains in any menu or dialog loop — `Menu`, `searchLinear`,
      `searchJump`, `askSearchKey`, enforced by `ArchitectureTest` and mutation-tested
- [x] `git grep` confirms the dead code is gone — the three validators, two `isString` copies, the
      never-assigned `quiano` fields, `StringTokenizer`, `inpt[]`, the commented `CircularQueue` demo
- [ ] Review with human before proceeding — **outstanding, and this is the checkpoint's purpose.**
      Two things a person must decide:
  1. `"This program show diff."` on the welcome screen. Left as the 2019 author wrote it; correcting
     it means guessing at intent rather than fixing a spelling mistake.
  2. Legibility of every corrected prompt once rendered. `GuiDriver` reads a label's text, never the
     screen — the gap has been open since Task 6 and is widest now, because Task 16 changed the words
     rather than the code.

---

## Phase 4: Layout and polish

## Task 17: Move sources to the conventional Maven layout

**Status:** done — 29 files moved with `git mv`, directory deleted, build green

**Description:** Move `Sorting and  Searching Algorithms/src` to `src/main/java` and the tests
to `src/test/java`, then drop the `<sourceDirectory>` override from `pom.xml`. This removes the
double space in the directory name that has to be quoted in every shell command and every path.
Use `git mv` so history follows the files. Do not move `QueueJava` or `CircularQueue` anywhere
special — they are not part of the sorting app.

**Context:** `CODE_REVIEW.md §Suggested order of work` (item 5, remainder)

**Acceptance criteria:**
- [x] Sources live under `src/main/java`, tests under `src/test/java`
- [x] `pom.xml` contains no `<sourceDirectory>` override
- [x] The `Sorting and  Searching Algorithms` directory no longer exists

**Verification:**
- [x] Build succeeds: `mvn -q clean package` exits 0 — 193 tests, 0 failures, 15 suites
- [x] Tests pass: `mvn -q test` exits 0
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
- [x] ~~`QueueJavaTest` covers enqueue, dequeue, peek, overflow, and underflow~~ **already
      delivered by Task 4** — 7 tests, including fill-and-drain drift and both refused-bound
      paths. Do not rewrite them; only extend if the decoupling changes the API.
- [ ] `CircularQueueTest` covers wraparound from last slot to first, and the single-element reset case
- [ ] `CircularQueue`'s data structure is verified, **not just its console loop**. Its bounds
      guards are already correct (`if/else`, confirmed by execution) — the point of the tests is
      to lock that in, not to fix a known bug.

**Verification:**
- [ ] Tests pass: `mvn -q test -Dtest=CircularQueueTest`
- [ ] Tests pass: `mvn -q test`
- [ ] Manual check: both console entrypoints still behave identically from the terminal

**Dependencies:** Task 15

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/QueueJava.java`
- `Sorting and  Searching Algorithms/src/CircularQueue.java`
- new `CircularQueueTest` (the `QueueJavaTest` already exists)

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
