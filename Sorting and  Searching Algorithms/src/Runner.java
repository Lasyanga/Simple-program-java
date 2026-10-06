/**
 * The application's entry point and its control flow.
 *
 * <p>Until Task 13 this class, {@link LinearSearch} and {@link JumpSearch} each opened their own
 * dialogs. Three files meant three slightly different ways to prompt, and a search could not be
 * tested without a display. Every {@code JOptionPane} call now lives in {@link Presenter}; this
 * class decides <i>when</i> to show one, and {@link Presenter} decides only what it looks like.
 *
 * <p>Two control-flow oddities here are deliberate leftovers, both scheduled for Task 14:
 * <ul>
 *   <li>{@link #Menu()} is recursive — it re-invokes itself at the bottom of its loop and again in
 *       the {@code default} branch — so the menu advances by nesting rather than iterating.
 *   <li>Cancel at the menu calls {@link System#exit(int)} to match option 9, because there is
 *       nowhere to return to. Once the recursion becomes a loop this becomes a plain return.
 * </ul>
 * Neither was touched here: Task 14 rewrites both together, and changing either on its own would
 * leave the menu harder to follow than either endpoint.
 */
public class Runner {

	private static String length = " ", insElem, input = "";
	private static int size, count = 0, element;
	private static Array arr;

	public Runner(){
		GUI();
	}

	public void GUI(){
		try{
			do{
				length = Presenter.askLength();

				// Cancel makes showInputDialog return null, and the digit validator
				// used to throw on it. There is no screen above this prompt to go
				// back to, so leave quietly instead of handing null to intOnly.
				if(length == null){
					return;
				}
			}while(!isValidLength(length));

			size = Integer.parseInt(length);
			arr = new Array(size);

			do{
				do{
					insElem = Presenter.askElement(count);

					// Cancel here abandons a half-filled array, so leave rather than
					// loop forever on a prompt the validator will never accept.
					if(insElem == null){
						return;
					}
					if(intOnly(insElem)){
						element = Integer.parseInt(insElem);
						arr.setElement(count, element);
						count++;
					}
				}while(!intOnly(insElem));
			}while(count < size);

			arr.setCopy();
			Menu();
		}catch(Exception e){
			System.out.print(e);
			Presenter.showGoodbye();
		}
	}

	public static void Menu(){

		do{
			input = Presenter.askMenu(arr.getLength(), arr.getElement());

			// Cancel makes showInputDialog return null. Without this check the
			// validator says "invalid", the loop falls through to the recursive
			// Menu() call at the bottom, and the prompt reappears forever, so the
			// user could not leave. Exiting matches menu option 9; Task 14 replaces
			// the recursion with a real loop, where this becomes a plain return.
			if(input == null){
				System.exit(0);
			}
			if(intOnly(input)){
				switch(Integer.parseInt(input)){
				case 1:
					showSortTrace("Bubble Sort", BubbleSort.bubbleSortTrace(arr.getCopy()),
							BubbleSort.bubbleSort(arr.getCopy()));
					break;

				case 2:
					showSortTrace("Insertion Sort", InsertionSort.insertionSortTrace(arr.getCopy()),
							InsertionSort.insertionSort(arr.getCopy()));
					break;

				case 3:
					showSortTrace("Selection Sort", SelectionSort.selectionSortTrace(arr.getCopy()),
							SelectionSort.selectionSort(arr.getCopy()));
					break;

				case 4:
					showSortTrace("Merge Sort", MergeSort.mergeSortTrace(arr.getCopy()),
							MergeSort.mergeSort(arr.getCopy()));
					break;

				case 5:
					showSortTrace("Quick Sort", Quicksort.quickSortTrace(arr.getCopy()),
							Quicksort.quickSort(arr.getCopy()));
					break;

				case 6:
					searchLinear(arr.getCopy(), arr.getElement());
					break;

				case 7:
					Presenter.showNotImplemented();
					break;

				case 8:
					searchJump(arr.getsorted());
					break;

				case 9:
					System.exit(0);
					break;

				default:
					Menu();
				}
			}
			Menu();
		}while(!intOnly(input));
	}

	/**
	 * Repeatedly searches the array until the user leaves.
	 *
	 * <p>Was {@code LinearSearch.Searching(int)} before Task 13, and reached the search by recursion:
	 * search, then call itself again to re-prompt. The recursion is now a loop. The visible sequence
	 * of dialogs is identical either way — a loop and a tail call produce the same prompts in the same
	 * order — but the stack no longer grows by one frame per search, which mattered here because a
	 * user who searched fifty times would have recursed fifty times deep.
	 *
	 * @param array the values to search, as typed; never modified
	 * @param typed the array as typed, for display only
	 */
	private static void searchLinear(int[] array, String typed){
		String position = " ";
		while(true){
			String key = askSearchKey("Linear Search", "Unsorted", typed, position);

			// Cancel. There is nowhere above this dialog to return to but the menu, and the
			// caller's loop shows it again, so leaving the loop is the whole cancel path.
			if(key == null){
				return;
			}

			int search = Integer.parseInt(key);
			int[] hits = LinearSearch.linearSearchAll(array, search);
			if(hits.length == 0){
				position = "Element " + search + " is not found.";
			}else{
				StringBuilder found = new StringBuilder();
				for(int hit : hits){
					found.append(hit).append(' ');
				}
				position = search + " is @ index: " + found.toString().stripTrailing();
			}
		}
	}

	/**
	 * Repeatedly searches the sorted array until the user leaves. The loop counterpart of the
	 * recursion {@code JumpSearch.searching(int)} used before Task 13.
	 *
	 * @param sorted the values to search, already sorted; never modified
	 */
	private static void searchJump(int[] sorted){
		String sortedText = asElements(sorted);
		String position = " ";
		while(true){
			String key = askSearchKey("Jump Search", "Sorted", sortedText, position);
			if(key == null){
				return;
			}

			int search = Integer.parseInt(key);
			int found = JumpSearch.jumpSearch(sorted, search);
			if(found == -1){
				position = "Element " + search + " is not found.";
			}else{
				position = "Element @ index: " + found;
			}
		}
	}

	/**
	 * Asks for a search key, re-asking until one is usable.
	 *
	 * <p>The validation loop lives here rather than in {@link Presenter} on purpose. {@code intOnly}
	 * reports a null as invalid, so a cancel handled inside the loop would re-ask forever and trap the
	 * user in the dialog — a bug this project shipped once already, in the same two searches. Null is
	 * returned straight through instead.
	 *
	 * @param title window text, "Linear Search" or "Jump Search"
	 * @param heading "Unsorted" or "Sorted", the qualifier on the element line
	 * @param elements the array as displayed
	 * @param position the previous result line, shown above the input line
	 * @return a usable key, or null if the user cancelled
	 */
	private static String askSearchKey(String title, String heading, String elements, String position){
		while(true){
			String reply = Presenter.askSearchKey(title, heading, elements, position);
			if(reply == null || intOnly(reply)){
				return reply;
			}
		}
	}

	/**
	 * Formats values the way the dialogs display them: a leading space, then each value and a space.
	 *
	 * <p>Identical to {@link Array#getElement()}, which cannot be reused here because that method
	 * formats the array as <i>typed</i> while jump search displays the <i>sorted</i> copy. Matching
	 * the format rather than sharing the code keeps the string in one place per array.
	 */
	private static String asElements(int[] values){
		StringBuilder out = new StringBuilder(" ");
		for(int value : values){
			out.append(value).append(' ');
		}
		return out.toString();
	}

	public static boolean intOnly(String str){
		if(str == null || str.isEmpty()){
			return false;
		}
		for(int i = 0; i < str.length(); i++){
			if(!Character.isDigit(str.charAt(i))){
				return false;
			}
		}
		try{
			Integer.parseInt(str);
			return true;
		}catch(NumberFormatException e){
			// More digits than an int can hold. Every caller hands this
			// method's result straight to Integer.parseInt, so refusing
			// the value here is what stops a long enough run of digits
			// from throwing at the element prompt or the menu switch.
			return false;
		}
	}


	/**
	 * Whether the text typed at the array-length prompt is usable.
	 *
	 * The element-entry loop in {@link #GUI()} is a
	 * {@code do { do { ... } while (...) } while (count < size)}, so its body runs at
	 * least once whatever the length is. A length of 0 therefore made the app demand one
	 * element and write it into a zero-length array, throwing
	 * ArrayIndexOutOfBoundsException and ending the session. Refusing 0 up front is what
	 * removes that crash.
	 *
	 * @param str text straight from the dialog; may be null when the user pressed Cancel
	 * @return true only for a bare run of digits denoting at least 1
	 */
	public static boolean isValidLength(String str){
		if(!intOnly(str)){
			return false;
		}
		// intOnly already proved the string parses, so this cannot throw.
		return Integer.parseInt(str) >= 1;
	}

	/**
	 * Shows a sort's intermediate states and its final result.
	 *
	 * <p>Kept as a method on this class rather than inlined at five call sites only because all five
	 * pass the same array: {@code arr.getElement()} is the display string and cannot be read here.
	 * {@link Presenter#showSortTrace} does the drawing.
	 */
	private static void showSortTrace(String title, java.util.List<int[]> trace, int[] result){
		Presenter.showSortTrace(title, arr.getElement(), trace, result);
	}

	public static void main(String []args){
		new Runner();
	}

}
