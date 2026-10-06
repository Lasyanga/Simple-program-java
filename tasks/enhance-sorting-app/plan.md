# Implementation Plan: Enhance sorting/searching app

## Overview

Take the 2019 Swing demo from "unverifiable and undocumented" to "buildable, tested, and
reviewable." The algorithms are already correct — per `CODE_REVIEW.md` every defect is in the
layer that draws dialogs. So the plan fixes the reachable crashes first (cheap, and they are
real user-facing bugs), then does the one structural move that unlocks everything else:
separating each algorithm from its dialog so it can be unit tested. A Maven build with JUnit 5
is the forcing function, since there is currently no way to compile, run, or assert anything on
this machine.

Source of the work: `CODE_REVIEW.md` at the repo root. That file holds the evidence; this plan
and its task list hold only the work items.

Case: `enhance-sorting-app` · Tasks and checkpoints: `todo.md` in this folder

## Architecture Decisions

**Pure static algorithms, one presenter.** Each algorithm becomes
`public static int[] bubbleSort(int[] input)` plus, where a trace is wanted, a variant that
fills a `List<int[]>`. No fields, no dialogs, no callbacks. One `Presenter` class owns every
`JOptionPane` call. This is the change that makes the suite possible; it also collapses six
near-identical `xxxGUI()` / `getXProcess()` pairs into one place. Tasks 6-13.

**Maven with `sourceDirectory` pointed at the existing folder first.** `pom.xml` initially
declares `<sourceDirectory>Sorting and  Searching Algorithms/src</sourceDirectory>` so nothing
has to move before tests can run. Moving to the conventional `src/main/java` layout happens
later (Tasks 17-18), once a green suite exists to catch a bad move. Costs one ugly line in
`pom.xml` for a few tasks; buys an unbroken green suite throughout.

**Tests are a sibling of `src`, never nested inside it.** `<testSourceDirectory>` is
`Sorting and  Searching Algorithms/test`, *not* `src/test/java` as originally planned. Maven
scans `<sourceDirectory>` recursively, so tests under `src/` are compiled into `target/classes`
as main sources — the test class would ship in the jar. Verified: with tests in a sibling
directory no test class appears in `target/classes`. Task 17 restores the conventional
`src/main/java` + `src/test/java` pair, where the two are siblings again by construction.

**Target Java 21, not 8.** The plan originally said Java 8 to preserve old-runtime
compatibility. JDK 27 still accepts release 8 but warns that it is obsolete and will be
removed, so 21 buys durability at the cost of needing a JDK 21+ to build. Decided 2026-10-06.

**Fixes before refactor.** Tasks 3-5 are behavior fixes against the code as it exists. They are
ordered first because they are small, independently verifiable, and would otherwise be lost in
the noise of a six-class rewrite. Task 1's execution pass widened three of them: the length-0
crash joins Task 3, the `CANCEL_OPTION`-is-2 bug joins Task 11, and the empty-array guard joins
Task 12. Folding them into the task that owns each file beats renumbering 22 ids.

**One algorithm per task.** Tasks 6-12 each rewrite a single algorithm and add its test class in
the same step. Six algorithms in one task would be an L by the sizing table, and a broken
extraction would not surface until the whole phase was done.

**The `quiano` null-field pattern gets deleted, not patched.** During extraction each
algorithm's never-assigned `private Runner quiano` field disappears along with its dialog code.
Do not "fix" it by assigning `new Runner()` — that re-enters `Runner.GUI()` and restarts the
input dialog. Deleting the field is the fix.

**Queue classes stay, but are second-class.** `QueueJava` and `CircularQueue` are unrelated to
the sorting app. The Critical overflow bug in `QueueJava` is fixed early (Task 4) because it is
a real defect. Test coverage for them is deferred to Task 21 — not worth delaying the
algorithms for.

**No task renames the project.** The 2019 authorship is credited in the README and is left
alone.

## Task List

Ordered index. Ids, bodies, and checkpoints live in `todo.md`; this section only points at them.

### Phase 0: Unblock

Nothing can be compiled or run today, so every later task's verification depends on these two.

- Task 1 — Install a JDK and compile the existing sources
- Task 2 — Add Maven with JUnit 5

### Phase 1: Correctness fixes

- Task 3 — Fix `intOnly("")` so an empty field stops killing the app
- Task 4 — Fix `QueueJava` overflow and underflow falling through
- Task 5 — Make the `sort` gate a real check or delete it

### Phase 2: Separate algorithms from the GUI

- Task 6 — Extract Bubble Sort with tests
- Task 7 — Extract Insertion Sort with tests
- Task 8 — Extract Selection Sort with tests
- Task 9 — Extract Merge Sort with tests
- Task 10 — Extract Quicksort with tests, fixing the `1..n` display
- Task 11 — Extract Linear Search with tests — **done**: searching for `2` no longer cancels, and the search is pure
- Task 12 — Extract Jump Search with tests, fixing the NPE — **done**: option 8 works for the first time
- Task 13 — Collapse the six dialog methods into one presenter — **done**: one file owns every `JOptionPane` call, enforced by `ArchitectureTest`

### Phase 3: Control flow and cleanup

- Task 14 — Replace the recursive `Menu()` with a loop — **done**: no loop in the app advances by recursion, and the last `System.exit` is gone
- Task 15 — Remove dead code and the triplicated validators
- Task 16 — Fix typos and inconsistent naming

### Phase 4: Layout and polish

- Task 17 — Move sources to the conventional Maven layout
- Task 18 — Add a real package declaration
- Task 19 — Resolve menu option 7 (implement or remove Exponential Search)
- Task 20 — Update README and AGENTS.md to match the new structure
- Task 21 — Add test coverage for the two queue classes
- Task 22 — Add `.gitattributes` for line endings

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| No JDK on this machine — nothing can be verified | Blocks every other task | Resolved in Task 1. Every task since has been verified by `mvn -q clean package` |
| Rewriting six working algorithms breaks one that was correct | High — the algorithms are the project's only real value | **This happened.** Task 10's Quicksort was wrong in ~1 input in 5 and the task text asserted it was fine. Mitigation is now proven: verify the claim by execution *before* editing, and fuzz against an independent oracle rather than a reimplementation. Tasks 11-12 both did this |
| The GUI cannot be verified headlessly | Medium — a refactor could pass tests and still be broken on screen | Largely resolved. `GuiDriver` + `GuiSessionTest` (17 tests) drive the real dialogs with no human. Residual: rendering legibility still needs a person |
| The double space in `Sorting and  Searching Algorithms` breaks paths | Medium — quoting in shell, XML, and IDE import | Task 17 removes the directory entirely rather than working around it |
| Task 13 is the only task touching seven files at once | Medium | It is the single checkpointed consolidation step, and it relocates strings without changing what the user sees |
| Scope creep into rewriting the Swing presentation | Medium — the ask was tests, not a new UI | Tasks 6-12 change only what must change; Task 13 is the sole exception and its acceptance criteria forbid wording changes |
| Two Maven builds running at once produce plausible-looking failures | Medium — cost a false regression scare mid-Task-11 | Never run `mvn clean package` concurrently, and never kill Java processes while one is in flight. Both runs that hit this are recorded as invalid; re-run serially |

## Open Questions

- **Source layout.** The plan defaults to moving to `src/main/java` and adding a package
  (Tasks 17-18). The alternative is keeping the odd directory name and pointing Maven's
  `<sourceDirectory>` at it permanently — less churn, but every future contributor inherits the
  quoting problem. Say so if you prefer the alternative; it removes two tasks.
- **`JumpSearch`'s empty `catch` blocks were load-bearing.** Cancel worked *because* the NPE was
  swallowed. **Resolved in Task 12 (2026-10-07):** replaced with a real `input == null` guard, and
  `GuiSessionTest.cancellingJumpSearchReturnsToTheMenu` now pins the behaviour. Worth keeping in
  mind for Task 13, which touches the same dialogs — deleting an empty `catch` there would remove
  working behaviour again, in a file nobody suspects.
- **Whether to keep the queue demos at all.** They are unrelated to the project the repo is
  named for. Fixing and testing them (Tasks 4 and 21) is the conservative choice; deleting them
  is defensible and would remove two tasks.
- **Exponential search.** Task 19 offers implement-or-remove without recommending. Implementing
  it fits the project's purpose; removing it is smaller and leaves no lying menu entry.
