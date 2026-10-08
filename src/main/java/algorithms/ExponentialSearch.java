package algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exponential search over a sorted array: find the range by doubling, then binary search it.
 *
 * <p>Why it exists: binary search needs to know the array's length to pick its initial midpoint,
 * but the real win here is avoiding the linear scan at the front. Jump Search already does that
 * with a fixed stride; exponential search uses an exponentially growing stride, so it finds the
 * right window in O(log i) probes where i is the answer's index - independent of how long the
 * array is. For a 1,000,000-element array whose answer sits at the end, that is ~10 doublings
 * instead of 500,000 comparisons.
 *
 * <p><b>Precondition: the array must be sorted ascending.</b> Same as Jump Search. The result on
 * an unsorted array is unspecified - it may return -1 for a present value, or an index for an
 * absent one. {@code Runner} sorts before offering a search, and both fuzz suites feed sorted
 * input, so the precondition holds wherever this is called.
 *
 * <p>Pure: takes its inputs, returns an index, and touches neither the array nor any static state.
 * There is no dialog here - {@code Presenter} owns every dialog since Task 13.
 */
public final class ExponentialSearch {

    private ExponentialSearch() {
        // Not instantiable: all-static namespace, same shape as the six algorithm classes.
    }

    /**
     * Index of the first occurrence of {@code target}, or -1 if absent.
     *
     * @param arr    sorted ascending; may be empty
     * @param target the value to find
     * @return an index in [0, arr.length), or -1
     */
    public static int exponentialSearch(int[] arr, int target) {
        int n = arr.length;
        if (n == 0) {
            return -1;
        }
        // The array's first element is outside the doubling loop below, so it needs its own check.
        if (arr[0] == target) {
            return 0;
        }

        // Double until the window overshoots the target or the end of the array.
        // Bound: i < n, and stop as soon as arr[i] exceeds the target - the sorted order means
        // every later index does too.
        int bound = 1;
        while (bound < n && arr[bound] <= target) {
            bound *= 2;
        }

        // The doubling loop's lower edge is bound/2, but duplicates can straddle it: in
        // {1,2,2,2,3} searching for 2, the loop stops with bound=4 and bound/2=2, yet the first 2
        // is at index 1. Walk left while the element before the window is still the target, so
        // the binary search sees the entire run. The array is sorted, so this stops at the first
        // element that differs - at most one scan through the duplicates.
        int lo = Math.min(bound / 2, n - 1);
        while (lo > 0 && arr[lo - 1] == target) {
            lo--;
        }
        return binarySearch(arr, lo, Math.min(bound, n) - 1, target);
    }

    /**
     * Every index at which {@code target} appears, ascending.
     *
     * <p>Kept for shape parity with {@code LinearSearch.linearSearchAll}, so the menu can offer
     * "find all" for every search without special-casing one algorithm.
     *
     * @param arr    sorted ascending; may be empty
     * @param target the value to find
     * @return a list of indices, possibly empty, in ascending order
     */
    public static List<Integer> exponentialSearchAll(int[] arr, int target) {
        int first = exponentialSearch(arr, target);
        if (first < 0) {
            return List.of();
        }

        List<Integer> all = new ArrayList<>();
        // Walk left: the array is sorted, so once arr[i] != target every earlier element is too.
        int i = first;
        while (i >= 0 && arr[i] == target) {
            all.add(i);
            i--;
        }
        Collections.reverse(all);
        // Walk right from the element after the first match.
        i = first + 1;
        while (i < arr.length && arr[i] == target) {
            all.add(i);
            i++;
        }
        return all;
    }

    /**
     * Classic binary search over a closed range, returning the <b>first</b> occurrence.
     *
     * <p>Identical in intent to the binary half of Jump Search, written out here so the two
     * searches do not depend on each other - a shared helper across two algorithm classes would
     * be a third thing to break.
     */
    private static int binarySearch(int[] arr, int lo, int hi, int target) {
        int firstSeen = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (arr[mid] == target) {
                firstSeen = mid;
                hi = mid - 1;   // keep looking left for an earlier occurrence
            } else if (arr[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return firstSeen;
    }
}
