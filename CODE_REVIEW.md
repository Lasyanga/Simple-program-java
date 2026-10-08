# Code review: Simple-program-java

**Reviewed:** 2026-10-06
**Subject:** the 11 sources in `Sorting and  Searching Algorithms/src/` as committed (2019)
**Reviewer note:** this is a 2nd-year college submission whose stated goal was to demonstrate
five sorting and two searching algorithms with visible intermediate state. It is reviewed
against that goal, and separately against what a portfolio repo would need today.

> **Status: every finding below has been acted on.** The modernization ran as 22 tasks
> tracked in `tasks/enhance-sorting-app/todo.md`. This document is kept as the *evidence*
> for why each change was made, not as a live list of open work — the findings are
> reproduced verbatim below so a reader can see what was wrong, with a resolution line
> pointing at the task that fixed it. The standing summary:
>
> | Section | Outcome |
> |---|---|
> | `## Correctness` | all 7 findings fixed (Tasks 3, 4, 5, 11, 12) |
> | `## Architecture` | extraction done (Tasks 6-13), plus the QuickSort correction (Task 10) |
> | `## Readability` | typos fixed (Task 16), `partion`→`partition` (Task 10) |
> | `## Dead code` | all five items removed (Tasks 8, 11, 12, 15) |
> | `## Performance` | left alone, as the section itself recommends |
> | `## Security` | nothing to do |
> | `## Suggested order of work` | all 7 items complete |
>
> What remains open is **not** in this review: the legibility check (a human must judge how
> dialog text reads on screen) and the two items the original review never covered — queue
> test coverage (Task 21) and line endings (Task 22).

## How this review was produced

The first pass came from reading the source only. **Updated 2026-10-06, after Task 1 installed
JDK 27:** the machine now has a working JDK and Maven, and the findings below have been
**executed rather than merely read**. Findings dated 2026-10-06 with quoted exception messages
were confirmed by running code — the algorithm and queue cores were driven directly from a
scratch class in the default package, compiled against the build output. Doing so confirmed
every original claim and **surfaced three defects this review had missed**, recorded in
`## Correctness` below.

What still cannot be verified: the Swing dialog behavior itself. `Runner` is a modal
`JOptionPane` loop, so anything about what the user actually sees needs a human clicking
through. Line numbers refer to the sources as committed at the `v1.0.0` tag.

## Verdict

**The algorithms are correct. The code around them is not good.**

All five sorts and both searches were traced line by line for algorithmic bugs and none
were found. Bubble's early-exit flag, Selection's min-scan, Insertion's shift loop, Hoare
partition plus its recursion bounds, and Merge's asymmetric tail-copy all hold up. That is
the hard part of the assignment and it is right.

Every problem below lives in the layer that draws dialogs.

---

## Correctness

**Critical — `QueueJava`'s overflow and underflow guards don't stop execution.**
~~Open.~~ **Fixed 2026-10-06 (Task 4).** `QueueJava.java:26-35` printed
"Overflow Program terminated." and then fell straight through to
`rear = (rear+1)%capacity; arr[rear]=item; count++`, because the `System.exit(1)` that would
have stopped it was commented out. On underflow (lines 46-53) it was worse: `count--` took
`count` to `-1`, and since `isEmpty()` is `size()==0`, the queue became permanently non-empty
*and* non-full, printing garbage forever. Reachable by a user, not just by inspection.

Both `//System.exit(1);` lines are now `return;`, so the guards actually stop. The constructor
also rejects `size < 1` with `IllegalArgumentException`, replacing the `ArithmeticException`
from `% capacity` that used to surface the mistake some distance away. Covered by
`QueueJavaTest` (7 tests), which failed 5 of 7 against the old code — reporting size 3 into
capacity 2, size -1, and no exception at all for capacity 0.

`CircularQueue` was checked for the same defect and is **clean**: its guards use `if/else`, so
they genuinely prevent fall-through. Verified by execution.

**Required — an empty text field kills the app.** ~~Open.~~ **Fixed 2026-10-06 (Task 3).**
`Runner.intOnly("")` returns `true`: the
loop at `Runner.java:140` never executes for a zero-length string, so it falls through to
`return true`. Clearing the field and pressing OK therefore produces
`Integer.parseInt("")` → `NumberFormatException`. At the array-length prompt, `GUI()`'s
catch swallows it and the app exits with "Bye.. bye..". At the menu, `Menu()` has no
try/catch, so the exception propagates up to that same handler and the app dies.
*Verified 2026-10-06 by execution, not inspection.* `intOnly("")` returns `true`;
`Integer.parseInt("")` throws `NumberFormatException: For input string: ""`. **Cancel takes a
second route to the same place:** `showInputDialog` returns `null`, and `intOnly(null)` throws
`NullPointerException` on `str.length()` before the digit loop runs. So both "clear the field"
and "press Cancel" end in the same handler — there are two distinct crash paths, not one.

**Required — `Menu()` recurses instead of looping.** ~~Open.~~ **Fixed 2026-10-07 (Task 14).**
`Runner.java:135` calls `Menu()`
unconditionally at the bottom of a `do/while`, and several `case` labels call it again. The
loop condition is never reached, because the method never returns normally — every path
either recurses deeper or hits `System.exit(0)` (case 9). Each menu click adds stack frames.
Practically harmless at demo scale, but it is a misunderstanding of the language that
infects the whole app, and `LinearSearch.Searching` (line 57) and
`JumpSearch.JumpsearchGUI` (line 45) do the same thing.

**Required — the `sort` counter guards nothing.** ~~Open.~~ **Fixed 2026-10-06 (Task 5).**
`sort += 1` in cases 1-5 was a boolean written as an integer, read only for the `sort == 0`
test in cases 7 and 8. But case 8 passed `arr.getsorted()`, which clones and `Arrays.sort`s a
fresh array (`Array.java:37-41`) every time — so the array handed to Jump Search was sorted by
the JDK regardless of the gate and regardless of which sort the user ran. The check looked like
validation; the JDK did the work.

Deliberately resolved by **deleting** the counter, not by making it honest. Making it honest
would mean passing `arr.getCopy()` to Jump Search, and that was verified to be *wrong*: the
stored array is never sorted, because `setCopy()` runs exactly once after input and every sort
clones without writing back (`getSortedBubble()` and `getSortedInsertion()` are dead). Jump
Search would have received unsorted input and silently returned wrong indices.

The false "You Must sort the array element in able to perform this algorithm." message is gone.
`ArrayTest` now pins the mechanism: `getsorted()` sorts a clone and does not mutate the stored
copy, and `getCopy()` returns the live reference.

**Required — searching for the number 2 silently returns to the menu.** ~~Open.~~ **Fixed
2026-10-07 (Task 11).** `JOptionPane.CANCEL_OPTION`
is **2**, not -1. `LinearSearch.java:28` and `JumpSearch.java:39` both compare the parsed search
value against it, so typing `2` into the search box is interpreted as "user cancelled" and the
app navigates away instead of searching. Every other digit searches normally, which is exactly
why this survives a casual try. Found 2026-10-06; I had assumed `CANCEL_OPTION` was -1 and was
wrong — it is only reachable because `intOnly` accepts all digits, and `2` is a digit.
The check is unreachable *as written* in `JumpSearch` (line 35 NPEs first) but live in
`LinearSearch`.

**Required — an array length of 0 ends the app.** ~~Open.~~ **Fixed 2026-10-06 (Task 3).**
`GUI()`'s loop is
`do { do { prompt; arr.setElement(count, …); count++ } while(!intOnly(insElem)) } while(count < size)`.
The outer `do/while` runs its body **once** before testing the condition, so one element is
always requested. With `size == 0`, `Array.setElement(0, …)` indexed a zero-length array and
threw `ArrayIndexOutOfBoundsException`, landing in the catch that prints "Bye.. bye..".
Verified by execution 2026-10-06; the user only had to type `0` as the length. The fix is
`Runner.isValidLength`, which requires a value of at least 1 **before** the array is created,
so `size == 0` can no longer reach `setElement` at all.

**Required — `jumpSearch` throws on an empty array.** ~~Open.~~ **Guard added 2026-10-07
(Task 12), with the NPE fix.** `JumpSearch.java:59` evaluates
`array[Math.min(step, len) - 1]`; with `len == 0` and `step == 0` that is `array[-1]`, so
`ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 0`. Verified 8/8 correct
against brute force on non-empty inputs, so the core is sound — only the empty case is missing
a guard.

> **Superseded 2026-10-06.** This entry previously claimed the empty-array crash was
> "unreachable, and fixing length-0 without this one simply moves the crash." **That was
> wrong.** `isValidLength` refuses 0 outright, so the length-0 crash was *removed*, not moved:
> `size`'s only writer is `Runner.java`, `arr` is built from it, and `JumpSearch` receives
> `arr.getsorted()`. `jumpSearch` therefore cannot see an empty array from the GUI at all.
> The guard below is still worth having for the method's own sake — it is a public static
> that any caller can pass `new int[0]` to — but it is defensive, not a live user path.

**Verified correct 2026-10-06, contradicting no claim above:** `JumpSearch.jumpSearch`'s core
(lines 53-75) is correct — 8 of 8 differential cases against a brute-force reference, covering
first, last, interior, both out-of-range directions, and single-element input. And the
never-assigned `private Runner quiano` fields do **not** NPE, because `Runner.Menu()` and
`Runner.intOnly()` are `static` and Java discards the receiver for a static call. Confirmed by
invoking `intOnly` through a null reference. This is why those fields survive at all — and why
"fixing" them by assigning `new Runner()` would be a regression, not a repair.

## Architecture — the one structural problem

**Resolved across Tasks 6-13.** The plan below was executed in full: each algorithm is now a
pure `static` method with no fields and no dialogs, every `JOptionPane` call lives in one
`Presenter` class, and `Runner` owns control flow. The six near-identical `getXProcess()` /
`xxxGUI()` pairs collapsed as predicted. What follows is the original analysis, kept because
it explains *why* the structure is what it is now.

**Every algorithm class is three things welded together:** the sort, the trace-string
builder, and the dialog. `BubbleSort` clones, sorts, appends every intermediate state to a
`String`, shows a `JOptionPane`, and calls back into the menu. That coupling is why this
project has no tests and cannot have any without a display — and the algorithms, which are
the valuable part, are unreachable except through a GUI.

The move that deletes the most complexity:

1. Make each algorithm a pure `static int[] bubbleSort(int[] a)` — no fields, no dialogs,
   no callbacks.
2. Have the algorithm return the trace (`int[][] states`) or accept a `List<int[]>` listener.
3. Put *all* rendering in one presenter class.
4. The algorithms become testable, the six near-identical `getXProcess()` / `xxxGUI()` pairs
   collapse into one, and `JumpSearch`, `LinearSearch`, and the menu recursion stop being
   special cases.

Step 1 alone makes `assertArrayEquals(sorted, BubbleSort.sort(input))` possible, which would
have caught the Quicksort display bug at compile time instead of 2019.

### Correction, 2026-10-07: Quicksort's algorithm was also wrong

This review reported only Quicksort's **display** bug and said the sort itself was correct. **That
was wrong, and the claim has been acted on incorrectly.** `QuicksortTest` found the algorithm
returns the wrong answer on a large fraction of ordinary inputs.

**The defect.** The 2019 code partitioned, then recursed on `[low..pi-1]` and `[pi..high]`:

```java
int pi = partion(arr, low, high);
if (low < pi - 1) { quickRecursion(arr, low, pi-1); ... }
if (pi < high)    { quickRecursion(arr, pi, high);   ... }
```

Hoare's partition returns a **boundary**, not the pivot's final position. Its guarantee is that
everything in `[low..pi]` is `<=` everything in `[pi+1..high]`. Recursing as above puts `arr[pi]`
in the *right*-hand range, where nothing ever compares it against that range - so the pivot's
guarantee is silently discarded. Every index is still covered by one branch or the other, which is
exactly why the bug is invisible on inspection: no element is lost, elements are merely left in
the wrong order.

**Evidence.** Delta-debugged to a minimal case: `[4, 0, 4, 3, 0, 4]` sorted to
`[0, 0, 4, 3, 4, 4]` - a `3` stranded behind a `4`. Failure rate by length over random inputs
with duplicates: 0% at n<=3, then 14% (n=4) rising to 25% (n=8). Over 200,000 random inputs of
length 1-13, **38,264 wrong answers, about one in five.**

**Why a bounds-only fix was not enough.** Correcting the bounds to `[low..pi]` / `[pi+1..high]`
overflowed the stack immediately. The 2019 partition's inner scans are unbounded -
`while(arr[low] < pivot) low++;` and `while(arr[high] > pivot) high--;` - relying on the pivot
value being present to stop them, which stops being true once elements have been swapped past it.
The partition itself had to be replaced with explicitly bounded scans, which is what shipped.

**Now fixed**, in the same commit as the extraction (Task 10). Verified by `QuicksortTest`: 20,000
fuzz cases against `Arrays.sort` on a fixed seed, plus already-sorted and reverse-sorted input up
to 4,000 elements. `Arrays.sort` is the oracle, so this is not a reimplementation agreeing with
itself.

**The lesson about this document.** "Correct" was asserted from reading, and reading is exactly
what missed it: the split looked sound because it covered every index. A review that states an
algorithm is correct without running it is making an unverified claim, and this one did it in the
direction of under-reporting. The other sorts' algorithms were verified by execution before this
point and are unaffected.

## Readability

`MergeSort` is the hard one. `count`, `x`, `y`, and `round` (lines 8-9) exist only to indent
the visualization, and `getMergeProcess()` (line 97) is a `getX()` method called at line 58
purely for its side effect, with the return value discarded. A reader has to hold four
coupled mutable fields to understand spacing in a string. `getMergeProcess()` should be
`recordStep()` returning `void`.

Worth knowing before editing that file: `MergeSort`'s single tail-copy loop looks like a bug
(where is the right-side loop?) and is not one — the main loop already guarantees only one
side can have leftovers. Add a comment, because the next reader will second-guess it too.

**Nit** (~~partly~~ **fully resolved**): `partion` was misspelled (`Quicksort.java:34`) and is now `partition`,
though the spelling only went away because the method was rewritten - see the Quicksort correction
above; the dialog title reads "Insetion
Sort" (`InsertionSort.java:21`); the search prompt is "Enter the you want to
Search:[element][interval]" in both search classes, and that `[element][interval]` hint is
false — the code does `Integer.parseInt` on the whole input; `Quicksort` vs `BubbleSort`
casing is inconsistent. **→ all four resolved: the rename and the search prompt in Task 12,
the title in Task 7, and `Quicksort` → `QuickSort` in Task 16. Task 16 also swept the
remaining user-visible strings, finding nine defects this list had not enumerated.**

## Dead code

*Ask before deleting.*

**All five items are now removed:**

- `SelectionSort.java:5` — `private Array arr;`, never assigned or read. **→ removed Task 8.**
- `JumpSearch.java:13` — `inpt[] = new int[2]`, never used. **→ removed Task 12.**
- `LinearSearch.java:34-36` — `opt == JOptionPane.CANCEL_OPTION`. **This was misfiled as dead
  code in an earlier draft of this review and that was wrong.** `CANCEL_OPTION` is **2**, not
  -1, and `intOnly("2")` is true, so the comparison is reachable by ordinary input: see the
  Required finding above. It was only unreachable in `JumpSearch`, where line 35 throws first.
  **→ deleted, not repaired, in Task 11: the check conflated "user cancelled" with a parsed
  value, and Cancel is now handled by a null check before parsing.**
- `CircularQueue.java:143-156` — commented-out demo block. **→ removed Task 15.**
- The digit validator, copy-pasted three times as `Runner.intOnly`, `QueueJava.isInteger`,
  `CircularQueue.isInteger`. **→ collapsed into `Validator.isInt` in Task 15, which also
  fixed a live queue crash the copies had drifted into.**

**Superseded 2026-10-06 — this note would now cause an outage.** This review previously said the
empty `catch (Exception e) {}` blocks in `LinearSearch` and `JumpSearch` were *load-bearing*:
"Cancel returns `null`, `intOnly(null)` throws NPE, and the empty catch is the only reason Cancel
returns you to the menu instead of crashing."

**That stopped being true the moment `intOnly` was fixed.** `intOnly(null)` now returns `false`
instead of throwing, which turned `LinearSearch`'s
`do { … } while(!quiano.intOnly(input))` into an **infinite re-prompt loop on Cancel** — the user
could not leave the dialog. It is now guarded by an explicit `input == null` check
(`LinearSearch.java:28-31`) that returns to the menu. So:

- Do **not** delete `LinearSearch`'s null check on the strength of this section. It is load-bearing
  *now*, for the opposite reason.
- `JumpSearch`'s empty catch is still doing load-bearing work, for its own separate reason: line
  35 dereferences a `StringTokenizer` that is never assigned, so it NPEs before `intOnly` is
  reached, on every input including valid ones. Task 12 replaces this.

## Performance

String concatenation with `+=` inside the trace loops is O(n^2) — `MergeSort.element`,
`BubbleSort.element`, and `Array.getElement()` each rebuild the whole trace per swap. For a
10-element demo: irrelevant. For 500 elements, `JOptionPane` is handed a string with tens of
thousands of lines and visibly stalls. The algorithms themselves are all appropriate
complexity. Leave this alone unless the intent is to demo on large arrays.

## Security

Nothing. No file I/O, no network, no SQL, no secrets, no external input beyond
`Integer.parseInt` on dialog text. This axis is clean.

The robustness gaps in that input path were three, not one, and an earlier draft of this review
named only the first: an **empty field**, a **null** from Cancel, and **integer overflow** on a
long run of digits. All three reached `Integer.parseInt` through a validator that said yes. Task
3 closed all three; see `## Correctness`.

---

## What is genuinely good

Worth recording, because the findings above are long:

- **The algorithms are right.** All five sorts and both searches are textbook-correct
  implementations. That is the hard part of the assignment.
- **Java's default-value trap was understood.** `flag = 0`, `count = -1`, `element = " "`
  are initialized at declaration even where the default would have sufficed. That awareness
  is not universal.
- **`Array.setCopy()` / `getCopy()` snapshotting is correct design.** Cloning before sorting
  means running three sorts in a row does not corrupt state.
- **"Show the intermediate state" is a legitimate goal,** not scope creep. Most sorting demos
  print only the answer; this one teaches something.
- **It builds and runs.** The committed `.class` files are proof it compiled and worked in
  2019.

## Suggested order of work

**All seven items are complete.** Tracking lived in `tasks/enhance-sorting-app/todo.md`
— that file owns the work items and their status; this list is the original summary,
reproduced so a reader can see what was proposed. Resolution in **bold**.

1. Fix the two reachable crashes (`QueueJava` overflow/underflow fall-through;
   `intOnly("")`). Both are small and both are real. **Both confirmed by execution
   2026-10-06.** Note there are *two* crash paths in the second one, not one: an empty field
   gives `NumberFormatException`, Cancel gives `NullPointerException`.
   **→ Tasks 4 and 3; a third path, integer overflow, was found and fixed too.**
2. Add tests for the six algorithms. After step 4 this is nearly free, and it is the single
   biggest improvement available to this repo.
   **→ Done: 210 tests across 16 suites, fuzzed against independent oracles.**
3. Make `sort` a boolean, or delete the gate.
   **→ Deleted in Task 5 — verified the gate guarded nothing: `getsorted()` sorts its own
   clone regardless.**
4. Extract the algorithms into pure static methods; collapse the six `xxxGUI()` methods into
   one presenter. **Correction:** `LinearSearch` has *no* pure method to extract — its logic
   lives in `Searching()`, which mutates static `position` and opens a dialog. It needs
   authoring, not extraction. `JumpSearch.jumpSearch` *is* already pure and correct.
   **→ Tasks 6-13. The correction was right: Task 11 authored `linearSearch`/
   `linearSearchAll` rather than extracting them.**
5. ~~Drop `bin/*.class` from git, add a `.gitignore`~~ **done 2026-10-06** — `.gitignore` added
   and the 11 `.class` files untracked. ~~Still open: add a real build file~~ **done
   2026-10-06** — Maven 3.9.16 and `pom.xml` added, JUnit 5 wired and verified running.
6. Never, under any circumstances, "fix" `quiano = new Runner()`. It restarts the entire
   input dialog. **Confirmed 2026-10-06** that the null field is harmless today *only*
   because `Menu()` and `intOnly()` are `static`.
   **→ Moot since Task 15: the `quiano` fields were deleted along with the dead code they
   were part of. There is nothing left to assign.**
7. **New, found by execution 2026-10-06** — see `## Correctness`:
   (a) searching for the value `2` silently cancels, because `CANCEL_OPTION` is 2, not -1;
   (b) array length `0` ends the app via the `do/while` that always prompts once;
   (c) `jumpSearch` throws `ArrayIndexOutOfBoundsException` on an empty array. (b) and (c)
   must be fixed together — (c) is unreachable only because (b) crashes first.
   **→ (a) Task 11, (b) Task 3, (c) Task 12. The claim that they must be fixed together was
   itself superseded inside `## Correctness`: Task 3 removed the length-0 path entirely, so
   (c) became unreachable for the opposite reason — a guard was still added for the method's
   own sake.**

## On presenting this repo

The honest version of this project's story is: *2019 college project, algorithms implemented
correctly, no tests, GUI layer needs work.* That is a perfectly good portfolio narrative,
and these gaps read as learning rather than carelessness. What is worth avoiding is
publishing the repo as-is and letting a reader discover the `intOnly("")` crash themselves.

**Updated 2026-10-08:** that caveat no longer applies — the crash was fixed in Task 3 and
every other finding in this review has since been resolved. The narrative that fits now is
*a 2019 college submission, modernized: Maven build, 210 tests fuzzed against independent
oracles, algorithms extracted into pure methods, and a GUI harness that drives the real
dialogs.* The one thing still worth a human's eyes before publishing is **legibility** —
`GuiSessionTest` proves which dialogs appeared and what text they held, never how that text
reads on screen.
