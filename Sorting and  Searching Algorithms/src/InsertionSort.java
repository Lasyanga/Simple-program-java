/**
 * Insertion sort, extracted from the 2019 dialog-driven version of this class.
 *
 * <p>Pure: no fields, no dialogs, no callbacks, no reference to Runner. Same shape as
 * {@link BubbleSort}, and for the same reason - the old constructor ran the sort and then
 * opened a dialog, which is what forced the algorithm and its presentation to live together.
 * Do not "repair" the removed {@code quiano} field by assigning {@code new Runner()}; that
 * re-enters {@code Runner.GUI()} and restarts the array-length dialog.
 *
 * <p>Presentation lives in the caller (Runner until Task 13 introduces the Presenter).
 */
public final class InsertionSort {

	private InsertionSort(){
		// Not instantiable: every entry point is static.
	}

	/**
	 * Sorts ascending and returns a new array, leaving {@code input} untouched.
	 *
	 * <p>The shift loop from the original is kept unchanged in behaviour: hold the current
	 * value, walk left past anything larger, then drop the value into the gap it leaves.
	 *
	 * @param input values to sort; may be empty, and is never modified
	 * @return a new ascending array
	 */
	public static int[] insertionSort(int[] input){
		return sort(input, null);
	}

	/**
	 * Runs the sort and records the array state after every insertion, in order.
	 *
	 * <p>One entry per outer iteration, so {@code n} elements give {@code n - 1} entries.
	 * Unlike {@link BubbleSort#bubbleSortTrace(int[])} these are recorded unconditionally: an
	 * already-sorted input still produces one entry per insertion, each identical to the input.
	 * That matches the 2019 behaviour, which appended a line every iteration regardless of
	 * whether anything moved.
	 *
	 * <p>Each entry is an independent snapshot; sharing one working array would leave every
	 * entry showing the final state.
	 *
	 * @param input values to sort; may be empty, and is never modified
	 * @return one state per insertion, in the order they happened
	 */
	public static java.util.List<int[]> insertionSortTrace(int[] input){
		java.util.List<int[]> trace = new java.util.ArrayList<>();
		sort(input, trace);
		return trace;
	}

	/**
	 * The single implementation both entry points share.
	 *
	 * @param input values to sort; never modified
	 * @param trace when non-null, receives a snapshot after each insertion
	 * @return a new ascending array
	 */
	private static int[] sort(int[] input, java.util.List<int[]> trace){
		int[] working = input.clone();
		for(int i = 1; i < working.length; i++){
			int value = working[i];
			int j = i - 1;
			while(j >= 0 && working[j] > value){
				working[j+1] = working[j];
				j--;
			}
			working[j+1] = value;
			if(trace != null){
				trace.add(working.clone());
			}
		}
		return working;
	}
}