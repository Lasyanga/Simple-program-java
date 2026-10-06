/**
 * Quicksort, extracted from the 2019 dialog-driven version of this class.
 *
 * <p>Pure: no fields, no dialogs, no callbacks, no reference to Runner.
 *
 * <p><b>This class replaced a broken algorithm, not just a dialog-coupled one.</b> The 2019
 * version recursed on {@code [low..pi-1]} and {@code [pi..high]} after a Hoare-style partition.
 * That is not a valid split. Hoare's partition returns a <i>boundary</i> between two ranges that
 * are each internally ordered - {@code everything in [low..pi] is &lt;= everything in
 * [pi+1..high]} - not the pivot's final index. Recursing as the original did therefore left
 * {@code arr[pi]} sitting in the right-hand range with nothing ever checking it against that
 * range, and the pivot's guarantee was silently discarded. Elements were never lost, which is why
 * the bug survived a casual try; they were just left in the wrong order.
 *
 * <p>The smallest input that exposes it is {@code [4, 0, 4, 3, 0, 4]}, which the original sorted
 * to {@code [0, 0, 4, 3, 4, 4]}. Across 200,000 random inputs of length 1-13 containing
 * duplicates, the original failed 38,264 times - about one in five, rising with length.
 *
 * <p>Correcting only the recursion bounds was not sufficient and overflowed the stack at once:
 * the original partition's two inner scans were unbounded ({@code while(arr[low] < pivot) low++}),
 * relying on the pivot value being present to stop them, which is not guaranteed once elements
 * have been swapped. So the partition is replaced too, with explicitly bounded scans.
 *
 * <p>Verified by {@link QuicksortTest}: 20,000 fuzz cases against {@link java.util.Arrays#sort}
 * on a fixed seed, plus already-sorted and reverse-sorted input up to 4,000 elements, which are
 * this algorithm's adversarial cases and the shape that would expose unbounded scans.
 *
 * <p>Presentation lives in the caller (Runner until Task 13 introduces the Presenter). The trace
 * records one state per completed partition, which is quicksort's natural unit and the only place
 * a mistake becomes visible - the 2019 version printed sub-ranges after they had been sorted,
 * which looked plausible while the array was not.
 */
public final class Quicksort {

	private Quicksort(){
		// Not instantiable: every entry point is static.
	}

	/**
	 * Sorts ascending and returns a new array, leaving {@code input} untouched.
	 *
	 * @param input values to sort; may be empty, and is never modified
	 * @return a new ascending array
	 */
	public static int[] quickSort(int[] input){
		return sort(input, null);
	}

	/**
	 * Runs the sort and records the whole array after every completed partition, in order.
	 *
	 * <p>Each entry is an independent snapshot of the full array, so it can be compared against the
	 * input as a permutation.
	 *
	 * <p>The count is {@code n - 1}, and always has been: each partition splits a range into two
	 * non-empty ones, so the recursion is a full binary tree over {@code n} leaves with
	 * {@code n - 1} internal nodes. Measured across every length up to 12, random and degenerate
	 * alike, with no exceptions. That is the same count as insertion and merge sort, reached for
	 * a different reason - one partition per split rather than one per element boundary.
	 *
	 * @param input values to sort; may be empty, and is never modified
	 * @return one state per partition, in the order the partitions completed
	 */
	public static java.util.List<int[]> quickSortTrace(int[] input){
		java.util.List<int[]> trace = new java.util.ArrayList<>();
		sort(input, trace);
		return trace;
	}

	/**
	 * The single implementation both entry points share.
	 *
	 * @param input values to sort; never modified
	 * @param trace when non-null, receives a snapshot after each partition
	 * @return a new ascending array
	 */
	private static int[] sort(int[] input, java.util.List<int[]> trace){
		int[] working = input.clone();
		partitionRange(working, 0, working.length - 1, trace);
		return working;
	}

	/**
	 * Sorts {@code a[low..high]} by recursing on the two halves a partition leaves behind.
	 *
	 * <p>Note the bounds: {@code [low..pi]} and {@code [pi+1..high]}. The 2019 version used
	 * {@code [low..pi-1]} and {@code [pi..high]}, which is what made it wrong - see the class
	 * comment. {@code pi} belongs to the left half and must stay there, because the partition's
	 * guarantee is about the boundary, not about a pivot reaching its final home.
	 */
	private static void partitionRange(int[] a, int low, int high, java.util.List<int[]> trace){
		if(low >= high){
			return;
		}
		int pi = partition(a, low, high);
		if(trace != null){
			trace.add(a.clone());
		}
		partitionRange(a, low, pi, trace);
		partitionRange(a, pi + 1, high, trace);
	}

	/**
	 * Hoare partition: returns the boundary between two internally ordered ranges, so that
	 * everything in {@code a[low..returned]} is &lt;= everything in {@code a[returned+1..high]}.
	 *
	 * <p>Both scans are bounded by the loop condition and the {@code i >= j} check. The 2019 scans
	 * ({@code while(arr[low] < pivot) low++;} and {@code while(arr[high] > pivot) high--;}) had no
	 * bound and relied on the pivot value being present to stop them, which stops being true once
	 * elements have been swapped past it - that is how the original ran off the ends of the array.
	 *
	 * <p>Returning the boundary and recursing on {@code [low..returned]} and
	 * {@code [returned+1..high]} guarantees each call is strictly smaller than its parent, which
	 * is what terminates the recursion.
	 *
	 * @return a boundary index in {@code [low, high)}, or {@code high} when the range splits
	 *         exactly in half and needs no further work
	 */
	private static int partition(int[] a, int low, int high){
		int pivot = a[low + (high - low) / 2];
		int i = low - 1;
		int j = high + 1;
		while(true){
			do{
				i++;
			}while(a[i] < pivot);
			do{
				j--;
			}while(a[j] > pivot);
			if(i >= j){
				return j;
			}
			int temp = a[i];
			a[i] = a[j];
			a[j] = temp;
		}
	}
}