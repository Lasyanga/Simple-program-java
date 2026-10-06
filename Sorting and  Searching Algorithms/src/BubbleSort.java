/**
 * Bubble sort, extracted from the 2019 dialog-driven version of this class.
 *
 * <p>Pure: no fields, no dialogs, no callbacks, no reference to Runner. The original held a
 * never-assigned {@code private Runner quiano} and called {@code quiano.Menu()} from a
 * {@code showMessageDialog}; that field only ever worked because {@code Menu()} is static.
 * Deleting it is the fix - do not "repair" it by assigning {@code new Runner()}, which
 * re-enters {@code Runner.GUI()} and restarts the array-length dialog.
 *
 * <p>Presentation now lives in the caller (Runner until Task 13 introduces the Presenter).
 */
public class BubbleSort {

	private BubbleSort(){
		// Not instantiable: every entry point is static. The old constructor was what
		// forced the algorithm to be welded to a dialog.
	}

	/**
	 * Sorts ascending and returns a new array, leaving {@code input} untouched.
	 *
	 * <p>Keeps the original's early exit: if a whole pass completes without a swap the array
	 * is already ordered and the remaining passes are skipped. That optimisation was correct.
	 *
	 * @param input values to sort; may be empty, and is never modified
	 * @return a new ascending array
	 */
	public static int[] bubbleSort(int[] input){
		return sort(input, null);
	}

	/**
	 * Runs the sort and records the array state after every swap, in order.
	 *
	 * <p>The task list calls these "passes". They are recorded per <em>swap</em>, which is what
	 * the 2019 dialog showed and what makes the intermediate steps legible; one entry per outer
	 * pass would hide most of the movement.
	 *
	 * <p>Each entry is an independent snapshot, not a view of a shared working array - sharing
	 * one would leave every entry showing the final state.
	 *
	 * <p>Empty when the input was already sorted: no swaps means nothing happened to show. The
	 * final sorted array is deliberately not appended; callers display it from
	 * {@link #bubbleSort(int[])}.
	 *
	 * @param input values to sort; may be empty, and is never modified
	 * @return one state per swap, in the order they occurred
	 */
	public static java.util.List<int[]> bubbleSortTrace(int[] input){
		java.util.List<int[]> trace = new java.util.ArrayList<>();
		sort(input, trace);
		return trace;
	}

	/**
	 * The single implementation both entry points share.
	 *
	 * @param input values to sort; never modified
	 * @param trace when non-null, receives a snapshot after each swap
	 * @return a new ascending array
	 */
	private static int[] sort(int[] input, java.util.List<int[]> trace){
		int[] working = input.clone();
		for(int i = 0; i < working.length; i++){
			boolean swapped = false;
			for(int j = 0; j < working.length - 1 - i; j++){
				if(working[j] > working[j+1]){
					int temp = working[j];
					working[j] = working[j+1];
					working[j+1] = temp;
					swapped = true;
					if(trace != null){
						trace.add(working.clone());
					}
				}
			}
			if(!swapped){
				break;
			}
		}
		return working;
	}
}