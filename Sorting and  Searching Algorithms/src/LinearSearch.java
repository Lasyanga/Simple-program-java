import javax.swing.JOptionPane;

/**
 * Linear search, over a swing dialog written in 2019.
 *
 * <p><b>This task authored the search rather than extracting one.</b> {@code Searching(int)} was the
 * whole implementation, and it was impure three ways: it read the static field {@code arr}, it
 * rebuilt the static {@code position} string on every call, and on a hit it opened a modal dialog
 * <i>before</i> returning. Its return value therefore reached a caller only after the user had
 * already been re-prompted. There was no pure method underneath to lift out.
 *
 * <p>What survives is two pure static methods and one dialog. The search is four lines long and
 * no longer touches a static.
 *
 * <p><b>Duplicate keys: every match is reported.</b> The 2019 dialog listed all matching indices
 * in one string, so that behaviour is kept deliberately. First-match-only would have been tidier
 * and is exposed as a separate method, but silently changing what a user sees is not a
 * refactor's call to make. Both forms are named for what they do, so the choice is visible in the
 * API rather than a side effect of where the loop stopped.
 *
 * <p><b>The cancelled-search bug.</b> The old dialog ended with
 * {@code if (searched == JOptionPane.CANCEL_OPTION) return to menu;}. {@code CANCEL_OPTION} is
 * **2**, not -1, so typing {@code 2} to search for the number 2 was read as "the user cancelled"
 * and the app returned to the menu without searching. Every other digit searched normally, which
 * is why it survived a casual try. {@code CLOSED_OPTION} is the -1. Cancellation is in any case
 * signalled by a {@code null} return from {@code showInputDialog}, never by a value - so the fix
 * was to delete the comparison rather than to repair it.
 *
 * <p>{@link #quiano} is the last vestige of the null-{@code Runner} pattern and goes in Task 13,
 * which moves every dialog to one presenter. It is never assigned; {@code Menu()} is static, which
 * is the only reason this compiles. Do not "fix" the null by assigning {@code new Runner()} - that
 * re-enters {@code GUI()} and restarts the input dialog.
 */
public class LinearSearch {

	private static String element;
	private static String position = " ";
	private static Runner quiano;

	/**
	 * @param array the values to search; cloned, and never modified
	 * @param element the array as typed, for display only
	 */
	public LinearSearch(int[] array, String element){
		this.element = element;
		searching(array.clone());
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

	/**
	 * Prompts for a value until one is given or the user backs out, then reports every match.
	 *
	 * <p>The loop condition is {@code isSearchable}, which is {@code intOnly} - true only for a
	 * bare run of digits that fits in an {@code int}. A null return means Cancel and leaves.
	 */
	private static void searching(int[] array){
		String input;
		while(true){
			input = JOptionPane.showInputDialog(null,
					"Unsorted element: " + element + "\n" + position
							+ "\nEnter the you want to Search:",
					"Linear Search", JOptionPane.QUESTION_MESSAGE);

			// Cancel. showInputDialog returns null, which is the only cancellation signal there is.
			if(input == null){
				quiano.Menu();
				return;
			}
			if(isSearchable(input)){
				break;
			}
		}

		int search = Integer.parseInt(input);
		int[] hits = linearSearchAll(array, search);

		if(hits.length == 0){
			position = "Element " + search + " is not found.";
		}else{
			StringBuilder found = new StringBuilder();
			for(int hit : hits){
				found.append(hit).append(' ');
			}
			position = search + " is @ index: " + found.toString().stripTrailing();
		}

		// Back to the same prompt, which now shows the result above the input line.
		searching(array);
	}

	/** Whether text from the dialog is usable as a search key. */
	private static boolean isSearchable(String input){
		return Runner.intOnly(input);
	}
}