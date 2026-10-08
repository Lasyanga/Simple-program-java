# Simple-program-java

A small Java Swing application that demonstrates sorting and searching algorithms.

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

Jump search only works on a sorted array, so the app asks you to run a sort first before
offering options 7 and 8. Linear search works on the array as typed.

## Requirements

- JDK 8 or newer. The sources use no language feature newer than Java 8.
- A graphical environment — the app is built on Swing dialogs (`JOptionPane`).

`bin/` holds `.class` files from an old Java 13 build. It is gitignored, so a fresh clone
starts with no `bin/` at all — just compile from `src/` using the steps below and everything
works. If a stale copy is lying around locally, delete it rather than trusting it.

## Build

There is no build tool; compile the sources directly:

```
cd "Sorting and  Searching Algorithms"
javac src\*.java -d bin
```

The directory name contains a double space after "and", which is why it needs quoting.

## Run

```
java -cp bin Runner          # the sorting and searching app (Swing)
java -cp bin QueueJava       # console queue demo
java -cp bin CircularQueue   # console circular queue demo
```

`QueueJava` and `CircularQueue` are standalone queue demonstrations. They are not part of
the sorting and searching app and can be run or ignored independently.

## Project layout

```
Sorting and  Searching Algorithms/
├── src/    # all 11 source files
└── bin/    # compiled .class files (gitignored, not in the repo)
```

Every class is in the default package — no `package` declaration is used anywhere.

## Known issues

- **Jump search (option 8) does not work.** `JumpSearch` reads from a `StringTokenizer` that
  is never initialized, so it throws a `NullPointerException` on the first keystroke, which
  an empty catch block then hides. The `jumpSearch()` algorithm itself is implemented
  correctly; only the dialog around it is broken.
- **Quicksort's result dialog is wrong.** It prints `1..n` instead of the sorted array. The
  sort itself is correct.

## Credits

As credited in the app's welcome dialog: submitted by Herminigildo Jr. Quiano for
Professor Nikka Salvador.
