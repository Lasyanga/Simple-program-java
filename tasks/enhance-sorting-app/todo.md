# Tasks: Enhance sorting/searching app

Case: `enhance-sorting-app`
Source of the work: `CODE_REVIEW.md` (evidence lives there; this file holds only work items).
Ids are append-only. A task is done when `Status: done` **and** every Verification item is ticked.

## Progress

**10 of 22 code complete. 120 tests, 0 failures, 10 suites. Nothing committed.**

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
| 10 | Extract Quicksort | done — **also fixed a wrong algorithm** |
| 11-22 | Linear Search onward | open |

**All five sorts are now pure** (bubble, insertion, selection, merge, quick), all verified against
`Arrays.sort`, all displayed through one helper. Then two searches.

**Task 10 found a wrong algorithm, not just a wrong dialog.** This task's own text said the
partition and its bounds were correct. They were not: the sort failed ~1 input in 5, and the
display bug had been hiding it — the dialog never showed the real result. Root cause, minimal
case, failure rates and the fix are in Task 10. Recorded as a correction in `CODE_REVIEW.md`,
because the review document is what asserted the algorithm was fine.

**The GUI is no longer manual-only.** `GuiDriver` + `GuiSessionTest` (13 tests) drive the real modal
dialogs with no human: it enumerates `Window.getWindows()`, reads each dialog's labels, types and
clicks. Each test forks a JVM because option 9 and Cancel call `System.exit(0)`. That closed the
verification gap on Tasks 3 and 6-9, and Task 10 gained two cases from it.

**What automation did *not* settle:** whether the dialogs are pleasant to read. The driver reads
a label's contents, never the rendered screen. Trace legibility stays a human judgement.

### The one thing still needing a human

Nothing is wrong with the code or the tests. What no test can judge is **legibility** — whether a
trace is readable once rendered, which was Merge Sort's whole acceptance criterion ("a readable
trace, not the old indented tangle"). `GuiDriver` proves the dialogs appear, in order, with the
right text, and that control returns to the menu. Reading the screen is a person.

The manual pass, accumulated:

- [ ] empty field at the length prompt reprompts instead of exiting
- [ ] Cancel at the length, element and menu prompts leaves cleanly (no dialog loop)
- [ ] array length `0` reprompts
- [ ] Cancel in Linear Search returns to the menu rather than looping forever
- [ ] menu options 1, 2, 3, 4 show a readable trace and return to the menu
- [ ] `QueueJava` console: enqueue `capacity + 1`, dequeue from empty, enter size `0`

Trace *strings* were verified for bubble, insertion and selection by driving the trace methods
from a scratch class and printing the exact dialog text. Only the on-screen rendering is unproven.

### Findings folded into existing tasks

Task 1's execution pass found three defects the original review had missed. Rather than append
new ids, each went into the task that owns the file:

- `JOptionPane.CANCEL_OPTION` is **2, not -1**, so searching for the value `2` silently cancels
  → Task 11 (`LinearSearch.java:28`)
- array length `0` crashed via the `do/while` that always prompts once → Task 3
- `jumpSearch` throws on an empty array → Task 12 (defensive only, since Task 3 removed the path)

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

**Status:** open

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
- `Sorting and  Searching Algorithms/test/LinearSearchTest.java`

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
- [ ] **An empty input array returns -1 instead of throwing** — see below
- [ ] The search dialog performs a real search on a sorted array and reports the index
- [ ] Cancel returns to the menu; the empty `catch` is gone
- [ ] `JumpSearchTest` covers found, not-found, key below range, key above range, single-element input, **and empty input**

**The core is already correct — do not rewrite it.** Verified 2026-10-06 by differential
testing against a brute-force reference: 8 of 8 cases pass (first, last, interior, both
out-of-range directions, single-element). Only the empty case is missing a guard.

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
- [ ] Tests pass: `mvn -q test -Dtest=JumpSearchTest`
- [ ] Tests pass: `mvn -q test`
- [ ] Manual check: jump search actually returns an index — this option has never worked
- [ ] Manual check: Cancel from the jump search dialog returns to the menu

**Dependencies:** Task 6, Task 5

**Files likely touched:**
- `Sorting and  Searching Algorithms/src/JumpSearch.java`
- `Sorting and  Searching Algorithms/test/JumpSearchTest.java`

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
