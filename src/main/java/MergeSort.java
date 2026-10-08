/**
 * Merge sort, extracted from the 2019 dialog-driven version of this class.
 *
 * <p>Pure: no fields, no dialogs, no callbacks, no reference to Runner.
 *
 * <p>This was the least readable file in the project. It carried four fields -
 * {@code count}, {@code x}, {@code y} and {@code round} - that existed only to stagger a
 * text visualisation into a shape suggesting a merge tree, and a {@code getMergeProcess()}
 * method that was a {@code getX()} called purely for its side effect with the return value
 * discarded. None of it survived. What remains is the algorithm, which was correct all along.
 *
 * <p>Presentation lives in the caller (Runner until Task 13 introduces the Presenter). The
 * trace records one state per completed merge, which shows the merge tree unfolding bottom-up
 * and is far more legible than the indentation it replaces.
 */
public final class MergeSort {

	private MergeSort(){
		// Not instantiable: every entry point is static.
	}

	/**
	 * Sorts ascending and returns a new array, leaving {@code input} untouched.
	 *
	 * @param input values to sort; may be empty, and is never modified
	 * @return a new ascending array
	 */
	public static int[] mergeSort(int[] input){
		return sort(input, null);
	}

	/**
	 * Runs the sort and records the array state after every completed merge, in order.
	 *
	 * <p>A merge tree over {@code n} leaves has {@code n - 1} internal nodes, so {@code n}
	 * elements give {@code n - 1} entries - and none at all for an input of one element or
	 * none. Merges complete bottom-up, so the first entries show single adjacent pairs already
	 * ordered and the last one is the finished sort.
	 *
	 * <p>Each entry is an independent snapshot.
	 *
	 * @param input values to sort; may be empty, and is never modified
	 * @return one state per merge, in the order the merges completed
	 */
	public static java.util.List<int[]> mergeSortTrace(int[] input){
		java.util.List<int[]> trace = new java.util.ArrayList<>();
		sort(input, trace);
		return trace;
	}

	/**
	 * The single implementation both entry points share.
	 *
	 * @param input values to sort; never modified
	 * @param trace when non-null, receives a snapshot after each merge
	 * @return a new ascending array
	 */
	private static int[] sort(int[] input, java.util.List<int[]> trace){
		int[] working = input.clone();
		split(working, 0, working.length - 1, trace);
		return working;
	}

	/** Divides until every range is a single element, then merges back up. */
	private static void split(int[] a, int low, int high, java.util.List<int[]> trace){
		if(low >= high){
			return;
		}
		int middle = low + (high - low) / 2;
		split(a, low, middle, trace);
		split(a, middle + 1, high, trace);
		merge(a, low, middle, high, trace);
	}

	/**
	 * Merges {@code a[low..middle]} and {@code a[middle+1..high]} into {@code a[low..high]}.
	 *
	 * <p>Uses {@code <=} when taking from the left, which keeps the sort stable. That is not
	 * observable for plain ints, since equal values are indistinguishable, but it matters if
	 * this is ever pointed at key-value pairs.
	 */
	private static void merge(int[] a, int low, int middle, int high, java.util.List<int[]> trace){
		int[] temp = new int[high - low + 1];
		System.arraycopy(a, low, temp, 0, temp.length);

		int leftLast = middle - low;
		int i = 0;
		int j = leftLast + 1;
		int k = low;

		while(i <= leftLast && j <= high - low){
			if(temp[i] <= temp[j]){
				a[k] = temp[i];
				i++;
			}else{
				a[k] = temp[j];
				j++;
			}
			k++;
		}

		while(i <= leftLast){
			a[k] = temp[i];
			i++;
			k++;
		}

		// Deliberately no second tail loop for the right-hand leftovers. The main loop can only
		// exit with one side exhausted, and temp began as a straight copy of a[low..high].
		// Writes so far went to a[k] in increasing order, so when the leftovers are on the
		// right they sit at positions that were never touched and already hold exactly the
		// right values. Copying them would be a no-op. When the leftovers are on the left,
		// those positions may have been overwritten, so the loop above is required.
		//
		// This looks like a missing loop and is not. See MergeSortTest's reverse-sorted case:
		// delete the loop above and that test fails.

		if(trace != null){
			trace.add(a.clone());
		}
	}
}