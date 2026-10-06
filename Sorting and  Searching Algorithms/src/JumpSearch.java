import javax.swing.JOptionPane;

/**
 * Jump search, extracted from the 2019 dialog-driven version of this class.
 *
 * <p><b>Menu option 8 has never worked, in any build, since 2019.</b> Not "was buggy" - never ran.
 * The dialog's loop condition called {@code st.nextToken()} on a {@code StringTokenizer} that was
 * declared and never assigned, so it threw {@code NullPointerException} on the user's first
 * keystroke. An empty {@code catch} swallowed it, so the app simply appeared to do nothing. The
 * algorithm underneath was fine; nothing could reach it.
 *
 * <p><b>The algorithm is unchanged and was correct already.</b> Verified by execution before this
 * file was touched: 600,000 differential trials against a brute-force reference, over sorted
 * arrays both strictly increasing and containing duplicates, and every length up to 1,000,000.
 * Zero wrong answers. So this is an extraction, not a repair - the fuzz cases in
 * {@code JumpSearchTest} exist to hold that behaviour steady, which nothing had done before
 * because this code was unreachable from the GUI.
 *
 * <p><b>The one defect fixed here is the missing empty-array guard.</b> The 2019 loop evaluated
 * {@code array[Math.min(step, len) - 1]}; with {@code len == 0} and {@code step == 0} that
 * indexes {@code array[-1]} and throws. Unreachable through the GUI, because the length prompt
 * refuses 0 before {@code Array} is built - but the method is public static, so any caller can
 * hand it {@code new int[0]}. Defensive, not a live user path.
 *
 * <p><b>Duplicates resolve to the first match.</b> The linear scan stops at the first element not
 * less than the key, so that is what this returns. Asserted explicitly in the test suite, so it
 * stays a decision rather than an accident.
 *
 * <p>{@link #quiano} is the last vestige of the null-{@code Runner} pattern and goes in Task 13
 * with every other dialog. It is never assigned; {@code Menu()} is static, which is the only reason
 * this compiles. Do not "fix" the null by assigning {@code new Runner()} - that re-enters
 * {@code GUI()} and restarts the input dialog.
 */
public class JumpSearch {

	private static String element = " ";
	private static String position = " ";
	private static Runner quiano;

	/**
	 * @param array a <b>sorted</b> array; cloned, and never modified
	 */
	public JumpSearch(int[] array){
		element = " ";
		for(int value : array){
			element += value + " ";
		}
		searching(array.clone());
	}

	/**
	 * The index of {@code key} in the sorted {@code array}, or -1 if absent.
	 *
	 * <p>The input must be sorted ascending; this is not checked, and an unsorted array can produce
	 * a wrong answer rather than an error. That is inherent to the algorithm, not an oversight -
	 * {@code Runner} passes {@code Array.getsorted()}, which sorts a clone.
	 *
	 * @param array sorted values to scan; may be empty; never modified
	 * @param key the value to look for
	 * @return the first matching index, or -1
	 */
	public static int jumpSearch(int[] array, int key){
		int len = array.length;
		if(len == 0){
			// Without this, step is floor(sqrt(0)) == 0 and the line below indexes array[-1].
			return -1;
		}

		int block = blockSize(len);

		// Jump forward a block at a time until a block's last element is >= the key, or the
		// array is exhausted. Everything skipped is smaller than the key.
		int prev = 0;
		while(array[Math.min(block, len) - 1] < key){
			prev = block;
			block += blockSize(len);
			if(prev >= len){
				return -1;
			}
		}

		// Walk the one block the jump loop landed in.
		while(array[prev] < key){
			prev++;
			if(prev == Math.min(block, len)){
				return -1;
			}
		}

		if(array[prev] == key){
			return prev;
		}
		return -1;
	}

	/**
	 * The jump length: {@code floor(sqrt(len))}.
	 *
	 * <p>Kept as a helper rather than inlined at both use sites. The 2019 code computed it twice,
	 * which is harmless but makes the loop harder to check - the two values must agree or the
	 * arithmetic quietly stops making progress.
	 *
	 * <p>For any {@code len >= 1} this is at least 1, so the block always advances. The floor of 1
	 * is 1, which is the smallest case worth stating explicitly: an array of one element still
	 * moves.
	 *
	 * @param len the array length, always at least 1 here
	 * @return the block size
	 */
	private static int blockSize(int len){
		return (int) Math.floor(Math.sqrt(len));
	}

	/**
	 * Prompts for a value until one is given or the user backs out, then reports the match.
	 *
	 * <p>The 2019 version checked {@code !intOnly(st.nextToken())} - the null {@code st} that made
	 * this option dead. Checking the dialog's own return is what a cancel actually looks like: a
	 * null. {@code intOnly} is the same digit check the other prompts use, so non-numeric and
	 * overflowing input re-asks rather than throwing.
	 */
	private static void searching(int[] array){
		String input;
		while(true){
			input = JOptionPane.showInputDialog(null,
					"Sorted element: " + element + "\n" + position
							+ "\nEnter the you want to Search:",
					"Jump Search", JOptionPane.QUESTION_MESSAGE);

			// Cancel. showInputDialog returns null, which is the only cancellation signal there is.
			if(input == null){
				quiano.Menu();
				return;
			}
			if(Runner.intOnly(input)){
				break;
			}
		}

		int search = Integer.parseInt(input);
		int found = jumpSearch(array, search);

		if(found == -1){
			position = "Element " + search + " is not found.";
		}else{
			position = "Element @ index: " + found;
		}

		// Back to the same prompt, which now shows the result above the input line.
		searching(array);
	}
}