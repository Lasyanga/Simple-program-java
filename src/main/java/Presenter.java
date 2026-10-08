import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

/**
 * Every {@code JOptionPane} call in the application.
 *
 * <p>Until Task 13 there were three files that opened dialogs: {@link Runner}, {@link LinearSearch}
 * and {@link JumpSearch}. The searches opened their own because each was written as one class that
 * both computed and prompted, and nothing separated the two concerns until now. That coupling was
 * the reason no algorithm could be tested without a display — {@code Searching(int)} had a
 * {@code JOptionPane} call in the middle of its loop.
 *
 * <p><b>This class builds dialogs; it does not decide anything.</b> It never validates input, never
 * branches on what the user typed, and never chooses what happens next — {@link Runner} owns all of
 * that and calls in here to put a window on screen. Keeping the split that way means a change to
 * wording or layout cannot accidentally become a change in control flow, and
 * {@code ArchitectureTest} enforces it: this must be the only file in the source tree that mentions
 * {@code JOptionPane}, and no algorithm class may name this one either.
 *
 * <p>The trace formatter in {@link #showSortTrace} does use loops, over the states it was handed.
 * Those are layout, not control flow — there is no branch and no decision in them.
 *
 * <p>Wording and message ordering were preserved exactly through Task 13, which relocated these
 * strings without revising them. Task 16 then corrected the ones that were wrong to a reader:
 * {@code "Enter the you want to Search:"} became {@code "Enter the value you want to search for:"},
 * and menu option 5 read {@code [5]Quick} while the dialog it opened was titled {@code Quick Sort} —
 * the only option whose label did not name its window. Both are asserted by
 * {@code UserFacingTextTest}.
 *
 * <p><b>Still deliberately ungrammatical.</b> {@code "This program show diff."} is left exactly as
 * the 2019 author wrote it. It is not a typo one can correct without guessing what was meant, and a
 * confident rewrite would be inventing intent rather than fixing a spelling mistake. The author's
 * voice is otherwise intact too: "Welcome!!", "Bye.. bye..", "Message from Cowboy".
 */
public final class Presenter {

	private Presenter(){
		// Presentational helpers only. Nothing here needs an instance, and making the class
		// final plus a private constructor is what lets ArchitectureTest assert it is a namespace.
	}

	/**
	 * Asks for the array length, on the welcome screen.
	 *
	 * @return the typed text, or null if the user pressed Cancel
	 */
	public static String askLength(){
		return JOptionPane.showInputDialog(null,
				"Professor: Nikka Salvador\n" +
				"Submitted By: Herminigildo Jr. Quiano\n\n" +
				"Welcome!!\n" +
				"This program show diff.\n" +
				"Sorting and Searching Method.\n\n" +
				"Please input the length of your array:",
				"Sorting and Searching", 1);
	}

	/**
	 * Asks for one element of the array.
	 *
	 * @param count the index being filled, zero-based, shown in the prompt
	 * @return the typed text, or null if the user pressed Cancel
	 */
	public static String askElement(int count){
		return JOptionPane.showInputDialog(null,
				"Please insert the Element["+count+"]:",
				"Sorting and Searching", 1);
	}

	/**
	 * Shows the menu, listing what was typed.
	 *
	 * @param length the array length, as built
	 * @param element the array as typed, for display only
	 * @return the chosen option, or null if the user pressed Cancel
	 */
	public static String askMenu(int length, String element){
		return JOptionPane.showInputDialog(null,
				"Length of your Array: "+length+"\n" +
				"Element you input: "+element+"\n\n" +
						"[1]Bubble Sort\n" +
						"[2]Insertion Sort\n" +
						"[3]Selection Sort\n" +
						"[4]Merge Sort\n" +
						"[5]Quick Sort\n" +
						"[6]Linear Search\n" +
						"[7]Exponential Search\n" +
						"[8]Jump Search\n" +
						"[9]Exit", "Menu", JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Shows a sort's intermediate states and its final result.
	 *
	 * <p>The {@code trace} unit differs per algorithm — one state per swap for bubble, per insertion
	 * for insertion sort, per partition for quicksort — so the caller supplies the list and this
	 * method only lays it out. Three spaces between values, and a trailing blank line per state, are
	 * the 2019 spacing and are load-bearing for the traces' readability.
	 *
	 * @param title window and heading text, e.g. "Bubble Sort"
	 * @param unsorted the array as typed, for display only
	 * @param trace one array state per step, in order; may be empty if the input was sorted
	 * @param result the final sorted array
	 */
	public static void showSortTrace(String title, String unsorted, List<int[]> trace, int[] result){
		StringBuilder steps = new StringBuilder();
		for(int[] state : trace){
			for(int i = 0; i < state.length; i++){
				steps.append(state[i]).append("   ");
			}
			steps.append("\n");
		}
		JOptionPane.showMessageDialog(null,
				"Unsorted element: "+unsorted+"\n"+
				title+" Process:\n"+steps+"\nSorted Element: "+Arrays.toString(result),
				title, 1);
	}

	/**
	 * Asks for a value to search for, showing whatever the last search found above the input line.
	 *
	 * <p>Shared by both searches, which differ only in the heading word and the window title: linear
	 * search says "Unsorted element", jump search says "Sorted element". Both repeat
	 * "Enter the you want to Search:" unchanged.
	 *
	 * <p><b>No validation here.</b> A non-numeric or overflowing reply comes back as typed, and it
	 * is {@link Runner}'s job to decide whether to ask again. Keeping the loop out of this class is
	 * what stops "re-ask" from turning into "re-ask forever" by accident.
	 *
	 * @param title window text, "Linear Search" or "Jump Search"
	 * @param heading "Unsorted" or "Sorted", the qualifier on the element line
	 * @param elements the array as displayed, leading and trailing spaces included
	 * @param position the previous result line, or a single space before the first search
	 * @return the typed text, or null if the user pressed Cancel
	 */
	public static String askSearchKey(String title, String heading, String elements, String position){
		return JOptionPane.showInputDialog(null,
				heading + " element: " + elements + "\n" + position
						+ "\nEnter the value you want to search for:",
				title, JOptionPane.QUESTION_MESSAGE);
	}

	/** Says an option exists in the menu but does nothing yet. Option 7, pending Task 19. */
	public static void showNotImplemented(){
		JOptionPane.showMessageDialog(null, "Exponential Search is not implemented yet.",
				"Message", 1);
	}

	/**
	 * The app's visible symptom of having died. Every exception in {@link Runner#GUI()} ends here.
	 */
	public static void showGoodbye(){
		JOptionPane.showMessageDialog(null, "Bye.. bye..\n(@~__~)", "Message from Cowboy", 1);
	}
}