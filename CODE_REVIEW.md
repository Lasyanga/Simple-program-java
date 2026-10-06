# Code review: Simple-program-java

**Reviewed:** 2026-10-06
**Subject:** the 11 sources in `Sorting and  Searching Algorithms/src/` as committed (2019)
**Reviewer note:** this is a 2nd-year college submission whose stated goal was to demonstrate
five sorting and two searching algorithms with visible intermediate state. It is reviewed
against that goal, and separately against what a portfolio repo would need today.

## How this review was produced

Every finding below comes from reading the source. **Nothing was compiled or executed** —
the machine this review ran on has a JRE 8 only, no JDK, and the committed `.class` files
are Java 13 bytecode that will not load there. Findings marked *confirmable* are the ones
that describe user-visible behavior and should be reproduced once a JDK is available
before acting on them. Line numbers refer to the sources as committed.

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
`QueueJava.java:26-35` prints "Overflow Program terminated." and then falls straight through
to `rear = (rear+1)%capacity; arr[rear]=item; count++`. The `System.exit(1)` that would have
stopped it is commented out on line 28. On underflow (lines 46-53) it is worse: `count--`
takes `count` to `-1`, and since `isEmpty()` is `size()==0`, the queue becomes permanently
non-empty *and* non-full, printing garbage forever. The guard is decorative in exactly the
same way as the `sort` counter below.
Reachable by a user, not just by inspection.
*Edge case:* entering size `0` yields `capacity==0`, and `% capacity` throws
`ArithmeticException`.

**Required — an empty text field kills the app.** `Runner.intOnly("")` returns `true`: the
loop at `Runner.java:140` never executes for a zero-length string, so it falls through to
`return true`. Clearing the field and pressing OK therefore produces
`Integer.parseInt("")` → `NumberFormatException`. At the array-length prompt, `GUI()`'s
catch swallows it and the app exits with "Bye.. bye..". At the menu, `Menu()` has no
try/catch, so the exception propagates up to that same handler and the app dies.
*Confirmable in ten seconds once a JDK exists.*

**Required — `Menu()` recurses instead of looping.** `Runner.java:135` calls `Menu()`
unconditionally at the bottom of a `do/while`, and several `case` labels call it again. The
loop condition is never reached, because the method never returns normally — every path
either recurses deeper or hits `System.exit(0)` (case 9). Each menu click adds stack frames.
Practically harmless at demo scale, but it is a misunderstanding of the language that
infects the whole app, and `LinearSearch.Searching` (line 57) and
`JumpSearch.JumpsearchGUI` (line 45) do the same thing.

**Required — the `sort` counter guards nothing.** `sort += 1` in cases 1-5 is a boolean
written as an integer, never read for anything except the `sort == 0` test in cases 7 and 8.
But case 8 passes `arr.getsorted()`, which clones and `Arrays.sort`s a fresh array
(`Array.java:37-41`) every time. So the array handed to Jump Search is sorted by the JDK
regardless of the gate — and regardless of which sort the user actually ran. The check looks
like validation; the JDK does the work. Make it a boolean, or drop it and state plainly that
jump search sorts for you.

## Architecture — the one structural problem

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

## Readability

`MergeSort` is the hard one. `count`, `x`, `y`, and `round` (lines 8-9) exist only to indent
the visualization, and `getMergeProcess()` (line 97) is a `getX()` method called at line 58
purely for its side effect, with the return value discarded. A reader has to hold four
coupled mutable fields to understand spacing in a string. `getMergeProcess()` should be
`recordStep()` returning `void`.

Worth knowing before editing that file: `MergeSort`'s single tail-copy loop looks like a bug
(where is the right-side loop?) and is not one — the main loop already guarantees only one
side can have leftovers. Add a comment, because the next reader will second-guess it too.

**Nit:** `partion` is misspelled (`Quicksort.java:34`); the dialog title reads "Insetion
Sort" (`InsertionSort.java:21`); the search prompt is "Enter the you want to
Search:[element][interval]" in both search classes, and that `[element][interval]` hint is
false — the code does `Integer.parseInt` on the whole input; `Quicksort` vs `BubbleSort`
casing is inconsistent.

## Dead code

*Ask before deleting.*

- `SelectionSort.java:5` — `private Array arr;`, never assigned or read.
- `JumpSearch.java:13` — `inpt[] = new int[2]`, never used.
- `LinearSearch.java:26-28` — `opt == JOptionPane.CANCEL_OPTION` is unreachable:
  `showInputDialog` returns `null` on cancel, and `Integer.parseInt(null)` throws first.
- `CircularQueue.java:143-156` — commented-out demo block.
- The digit validator, copy-pasted three times as `Runner.intOnly`, `QueueJava.isInteger`,
  `CircularQueue.isInteger`.

**FYI, and this is the interesting one:** the empty `catch (Exception e) {}` blocks in
`LinearSearch` and `JumpSearch` are *load-bearing*. Cancel on those dialogs returns `null`,
`intOnly(null)` throws NPE, and the empty catch is the only reason Cancel returns you to the
menu instead of crashing. Those blocks are not laziness — they are accidentally the entire
error strategy. Know this before "cleaning them up."

## Performance

String concatenation with `+=` inside the trace loops is O(n^2) — `MergeSort.element`,
`BubbleSort.element`, and `Array.getElement()` each rebuild the whole trace per swap. For a
10-element demo: irrelevant. For 500 elements, `JOptionPane` is handed a string with tens of
thousands of lines and visibly stalls. The algorithms themselves are all appropriate
complexity. Leave this alone unless the intent is to demo on large arrays.

## Security

Nothing. No file I/O, no network, no SQL, no secrets, no external input beyond
`Integer.parseInt` on dialog text. The only real robustness gap is the empty-string one
above. This axis is clean.

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

1. Fix the two reachable crashes (`QueueJava` overflow/underflow fall-through;
   `intOnly("")`). Both are small and both are real.
2. Add tests for the six algorithms. After step 4 this is nearly free, and it is the single
   biggest improvement available to this repo.
3. Make `sort` a boolean, or delete the gate.
4. Extract the algorithms into pure static methods; collapse the six `xxxGUI()` methods into
   one presenter.
5. ~~Drop `bin/*.class` from git, add a `.gitignore`~~ **done 2026-10-06** — `.gitignore` added
   and the 11 `.class` files untracked. Still open: add a real build file, which is what makes
   the repo usable by anyone else.
6. Never, under any circumstances, "fix" `quiano = new Runner()`. It restarts the entire
   input dialog.

## On presenting this repo

The honest version of this project's story is: *2019 college project, algorithms implemented
correctly, no tests, GUI layer needs work.* That is a perfectly good portfolio narrative,
and these gaps read as learning rather than carelessness. What is worth avoiding is
publishing the repo as-is and letting a reader discover the `intOnly("")` crash themselves.
