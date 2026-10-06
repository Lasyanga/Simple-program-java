/**
 * Selection sort, extracted from the 2019 dialog-driven version of this class.
 *
 * <p>Pure: no fields, no dialogs, no callbacks, no reference to Runner. The original carried
 * two dead fields - an unassigned {@code private Array arr} that nothing read, and an
 * unassigned {@code private Runner quiano} that only appeared to work because the method it
 * called, {@code Runner.Menu()}, is static. Both are gone rather than ported.
 *
 * <p>Presentation lives in the caller (Runner until Task 13 introduces the Presenter).
 */
public final class SelectionSort {

	private SelectionSort(){
		// Not instantiable: every entry point is static.
	}

	/**
	 * Sorts ascending and returns a new array, leaving {@code input} untouched.
	 *
	 * <p>Each outer iteration finds the smallest remaining value and swaps it into place.
	 *
	 * @param input values to sort; may be empty, and is never modified
	 * @return a new ascending array
	 */
	public static int[] selectionSort(int[] input){
		return sort(input, null);
	}

	/**
	 * Runs the sort and records the array state after every outer iteration, in order.
	 *
	 * <p>{@code n} elements therefore give {@code n} entries - one more than Insertion Sort's
	 * {@code n - 1}. Recorded unconditionally: the original swapped even when the minimum was
	 * already in place, so an all-duplicates input records {@code n} identical lines.
	 *
	 * <p>Each entry is an independent snapshot.
	 *
	 * @param input values to sort; may be empty, and is never modified
	 * @return one state per outer iteration, in the order they happened
	 */
	public static java.util.List<int[]> selectionSortTrace(int[] input){
		java.util.List<int[]> trace = new java.util.ArrayList<>();
		sort(input, trace);
		return trace;
	}

	/**
	 * The single implementation both entry points share.
	 *
	 * @param input values to sort; never modified
	 * @param trace when non-null, receives a snapshot after each outer iteration
	 * @return a new ascending array
	 */
	private static int[] sort(int[] input, java.util.List<int[]> trace){
		int[] working = input.clone();
		for(int i = 0; i < working.length; i++){
			int min = i;
			for(int j = i + 1; j < working.length; j++){
				if(working[j] < working[min]){
					min = j;
				}
			}
			// Kept unconditional to match the original. When min == i this swaps an element
			// with itself, which is a no-op; guarding it would change nothing observable
			// because the trace records a state either way.
			int temp = working[min];
			working[min] = working[i];
			working[i] = temp;
			if(trace != null){
				trace.add(working.clone());
			}
		}
		return working;
	}
}