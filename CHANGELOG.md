# Changelog

Notable changes to this project, newest first. Grouped by impact rather than by
commit, and phrased for someone deciding whether to upgrade — not a dump of the
commit log. Versions follow [Semantic Versioning](https://semver.org/).

## [2.0.0] - 2026-10-08

The modernization release. The 2019 college submission was rebuilt on Maven with
JUnit 5, its algorithms were pulled out of the GUI code, and every defect found in
it was fixed. **Breaking** for anyone who imported the old class names or called
the queue methods directly.

### Added

- **Maven build with JUnit 5** — 226 tests across 17 suites. Build with
  `mvn -q clean package`, which compiles and runs the whole suite.
- **Exponential search** — menu option 7 was a stub that only displayed
  "not implemented". It now searches, and is fuzz-tested against a brute-force
  oracle over 800k differential trials.
- **`Validator`** — one shared input-validation class. Replaces three
  near-identical copies of the same digit check that had drifted apart.
- **`Presenter`** — the single owner of every `JOptionPane`. No algorithm builds
  a dialog anymore, which is what made the algorithms unit-testable at all.
- **Automated GUI tests** (`GuiSessionTest`) that drive the real windows in a
  forked JVM. Dialog transcripts used to be verified by hand.

### Changed

- **Breaking:** every class moved into the `algorithms` package. Any
  fully-qualified name you referenced has changed.
- **Breaking:** the console queues report results instead of printing them —
  `enqueue` returns `boolean`, `dequeue` returns `int`, `display()` returns
  `String`. All three were `void` and wrote to `System.out` from inside the
  data structure; printing now lives in `main`.
- Target is **Java 21**, was Java 8. Avoids the "source value 8 is obsolete"
  warning modern JDKs emit, and stays buildable for years.
- `Menu()` is a `while(true)` loop instead of recursing into itself on every
  navigation.

### Fixed

- **QuickSort returned wrong results on roughly one input in five**, and its
  display was wrong as well. The partition was subtly incorrect; the project's
  own code review had asserted it was right by reading it, and that assertion
  was false. Caught by fuzzing against `java.util.Arrays.sort`.
- **Jump Search threw `NullPointerException`** on an empty array.
- **Searching for the number 2 silently cancelled.** `JOptionPane` returns `2`
  for `CANCEL_OPTION`, which collided with a perfectly valid input.
- **Input parsing crashed** on an empty string, a `null` string, or an integer
  larger than `Integer.MAX_VALUE`.
- **An array length of 0** threw `ArrayIndexOutOfBoundsException`.
- **Queue overflow and underflow fell through** and kept executing instead of
  reporting the condition.
- **Nine user-visible typos** across menus, prompts, and dialogs.
- The `sort` counter gate was removed — it guarded nothing; `getsorted()` sorted
  its own clone regardless.

## [1.0.0] - 2019

The original college submission, preserved at the `v1.0.0` tag. Five sorting
algorithms, two working search algorithms (a third was an unimplemented stub),
and two console queue demos — written to run from the IDE, with no build tool,
no dependencies, and no tests.
