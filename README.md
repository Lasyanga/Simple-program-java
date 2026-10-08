# Simple-program-java

A Java Swing application that demonstrates sorting and searching algorithms.

You type in an array of integers, pick a method from the menu, and the app shows the state
of the array after every pass or swap — accumulated into a single dialog — so you can watch
the algorithm work rather than only read its final result.

## Sorting and searching methods

| Option | Class | Method |
|---|---|---|
| 1 | `BubbleSort` | Bubble sort |
| 2 | `InsertionSort` | Insertion sort |
| 3 | `SelectionSort` | Selection sort |
| 4 | `MergeSort` | Merge sort |
| 5 | `QuickSort` | Quick sort |
| 6 | `LinearSearch` | Linear search |
| 7 | `ExponentialSearch` | Exponential search |
| 8 | `JumpSearch` | Jump search |
| 9 | — | Exit |

Exponential search and Jump search both require a sorted array; the app sorts a copy before
offering them, so you do not need to run a sort first. Linear search works on the array as
typed. Every algorithm is a pure static method — no dialogs, no shared state — which is what
makes the test suite possible.

## Requirements

- **JDK 21 or newer.** The build targets Java 21 (the current LTS).
- **Maven 3.9 or newer.**
- A graphical environment — the app is built on Swing dialogs (`JOptionPane`).

## Build

From the repository root:

```
mvn -q clean package
```

This compiles the sources, runs the test suite, and writes classes to `target/classes`.
Tests live in `src/test/java` alongside the sources in `src/main/java`, following the
standard Maven layout.

To run just the tests:

```
mvn -q test
```

To run a single suite (note: `-Dtest` overrides Surefire's naming patterns, so this
proves the class compiles and its assertions hold but not that the full build would
pick it up — use `mvn -q clean package` for that):

```
mvn -q test -Dtest=QuickSortTest
```

## Run

After `mvn -q clean package`:

```
java -cp target/classes algorithms.Runner          # the sorting and searching app (Swing)
java -cp target/classes algorithms.QueueJava       # console queue demo
java -cp target/classes algorithms.CircularQueue   # console circular queue demo
```

`QueueJava` and `CircularQueue` are standalone queue demonstrations. They are not part of
the sorting and searching app and can be run or ignored independently.

## Project layout

```
src/
├── main/java/algorithms/     # 14 production classes
│   ├── Runner.java           # entrypoint and control flow
│   ├── Presenter.java        # sole owner of every JOptionPane dialog
│   ├── Validator.java        # shared input validation
│   ├── Array.java            # typed array + display snapshot
│   ├── BubbleSort.java       # ...
│   ├── InsertionSort.java    # ...
│   ├── SelectionSort.java    # ...
│   ├── MergeSort.java        # ...
│   ├── QuickSort.java        # ...
│   ├── LinearSearch.java     # ...
│   ├── ExponentialSearch.java# ...
│   ├── JumpSearch.java       # ...
│   ├── QueueJava.java        # console queue demo
│   └── CircularQueue.java    # console circular queue demo
└── test/java/algorithms/     # 18 files: 17 test classes + GuiDriver (the harness)
    ├── GuiSessionTest.java   # end-to-end GUI transcripts (forked JVM)
    ├── GuiDriver.java        # dialog driver for the GUI tests
    ├── ArchitectureTest.java # structural rules (source-text analysis)
    └── ...Test.java          # one suite per algorithm + validators
```

Everything is in the `algorithms` package. Tests sit in the same package so they can reach
package-visible members without imports.

## Verification

The test suite has two layers:

- **Pure tests** fuzz each algorithm against an independent oracle (`java.util.Arrays.sort`
  for sorts, a brute-force linear scan for searches) on a fixed seed. This is how a wrong
  QuickSort partition that "looked correct" was caught — reading the code had missed it.
- **GUI tests** fork a JVM and drive the real Swing dialogs through `GuiDriver`, asserting
  which dialogs appeared, in what order, with what text. Nothing is mocked. They skip on a
  headless machine rather than fail.

`mvn -q clean package` is the only command that proves every test class is actually wired
into the build — Surefire's default naming patterns silently skip classes that don't end in
`Test`, `Tests`, or `TestCase`, and `-Dtest=Name` overrides those patterns.

## Known issues

All previously documented defects have been fixed:

- **Jump search (option 8)** — NPE from an uninitialized `StringTokenizer` (fixed in Task 12).
- **QuickSort** — wrong result on ~1 input in 5, plus a `1..n` display bug (fixed in Task 10).
- **Exponential search (option 7)** — was a commented-out stub (implemented in Task 19).
- **Searching for the value `2`** — `CANCEL_OPTION` is 2, not -1 (fixed in Task 11).
- **Empty field / Cancel / integer overflow** — all three crashed `intOnly` (fixed in Task 3).
- **Array length `0`** — caused `ArrayIndexOutOfBoundsException` (fixed in Task 3).

The one remaining manual check is **legibility**: `GuiDriver` reads a label's text, never
the rendered window, so whether dialog text reads well on screen needs a human.

## Credits

As credited in the app's welcome dialog: submitted by Herminigildo Jr. Quiano for
Professor Nikka Salvador.
