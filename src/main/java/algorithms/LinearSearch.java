package algorithms;

/**
 * Linear search.
 *
 * <p><b>Pure: no fields, no dialogs, no callbacks, no reference to {@link Runner}.</b> The search was
 * authored rather than extracted in Task 11 — {@code Searching(int)} had been the whole
 * implementation, and it was impure three ways: it read the static field {@code arr}, it rebuilt the
 * static {@code position} string on every call, and on a hit it opened a modal dialog <i>before</i>
 * returning. Its return value therefore reached a caller only after the user had been re-prompted.
 * There was no pure method underneath to lift out.
 *
 * <p><b>Duplicate keys: every match is reported.</b> The 2019 dialog listed all matching indices in
 * one string, so that behaviour is kept deliberately. First-match-only would have been tidier and is
 * exposed as a separate method, but silently changing what a user sees is not a refactor's call to
 * make. Both forms are named for what they do, so the choice is visible in the API rather than a
 * side effect of where the loop stopped.
 *
 * <p><b>The cancelled-search bug.</b> The old dialog ended with
 * {@code if (searched == JOptionPane.CANCEL_OPTION) return to menu;}. {@code CANCEL_OPTION} is
 * **2**, not -1, so typing {@code 2} to search for the number 2 was read as "the user cancelled" and
 * the app returned to the menu without searching. Every other digit searched normally, which is why it
 * survived a casual try. {@code CLOSED_OPTION} is the -1. Cancellation is in any case signalled by a
 * {@code null} return from {@code showInputDialog}, never by a value — so the fix was to delete the
 * comparison rather than to repair it.
 *
 * <p>The dialog that used to live here moved to {@link Presenter} in Task 13, along with the
 * {@code quiano} field and the re-prompt recursion. This class is now a namespace, matching the five
 * sorts: private constructor so nothing can be instantiated, and nothing to instantiate it for.
 */
public final class LinearSearch {

	private LinearSearch(){
		// Nothing to construct. Instantiation is what forced this algorithm to be welded to a
		// dialog in 2019, so the constructor is closed rather than merely unused.
	}

	/**
	 * The first index of {@code key}, or -1 if it is not present.
	 *
	 * @param input values to scan; never modified
	 * @param key the value to look for
	 * @return the lowest matching index, or -1
	 */
	public static int linearSearch(int[] input, int key){
		for(int i = 0; i < input.length; i++){
			if(input[i] == key){
				return i;
			}
		}
		return -1;
	}

	/**
	 * Every index holding {@code key}, in ascending order.
	 *
	 * <p>Empty when the key is absent. This is the behaviour the 2019 dialog had, kept on purpose;
	 * see the class comment.
	 *
	 * @param input values to scan; never modified
	 * @param key the value to look for
	 * @return a new array of matching indices, never null
	 */
	public static int[] linearSearchAll(int[] input, int key){
		java.util.List<Integer> hits = new java.util.ArrayList<>();
		for(int i = 0; i < input.length; i++){
			if(input[i] == key){
				hits.add(i);
			}
		}
		int[] indices = new int[hits.size()];
		for(int i = 0; i < indices.length; i++){
			indices[i] = hits.get(i);
		}
		return indices;
	}
}
