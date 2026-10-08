package algorithms;

/**
 * The application's entry point and its control flow.
 *
 * <p>Until Task 13 this class, {@link LinearSearch} and {@link JumpSearch} each opened their own
 * dialogs. Three files meant three slightly different ways to prompt, and a search could not be
 * tested without a display. Every {@code JOptionPane} call now lives in {@link Presenter}; this
 * class decides <i>when</i> to show one, and {@link Presenter} decides only what it looks like.
 *
 * <p>No loop in this class advances by recursion. It used to: {@code Menu()} called itself at the
 * bottom of its {@code do/while} and again from its {@code default} branch, which made that
 * {@code while}'s condition unreachable and grew the stack by a frame per menu click. Task 14 made it
 * one loop, and Task 13 had already done the same for the two search dialogs.
 *
 * <p>{@code Menu()} returns rather than exiting the process. Cancel and option 9 both end the loop and
 * hand back to {@link #main}, which owns process teardown — see the note there for why that
 * separation is not merely tidiness: returning from {@code main} takes about 1.3s longer to shut
 * down than {@link System#exit(int)} does.
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
				// back to, so leave quietly instead of handing null to Validator.isInt.
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
					if(Validator.isInt(insElem)){
						element = Integer.parseInt(insElem);
						arr.setElement(count, element);
						count++;
					}
				}while(!Validator.isInt(insElem));
			}while(count < size);

			arr.setCopy();
			Menu();
		}catch(Exception e){
			System.out.print(e);
			Presenter.showGoodbye();
		}
	}

	/**
	 * Shows the menu until the user leaves.
	 *
	 * <p>Was recursive until Task 14. The 2019 version ended with an unconditional {@code Menu()} at
	 * the bottom of its {@code do/while}, plus another in the {@code default} branch, so the
	 * {@code while}'s own condition was dead: the recursive call never returned, because every path
	 * either recursed deeper or called {@code System.exit}. Each menu click cost a stack frame, and a
	 * user navigating for a while grew the stack without bound.
	 *
	 * <p>Now one loop. Private because {@link #GUI()} is its only caller — once the search dialogs
	 * stopped calling back into it in Task 13, nothing outside this class needed it.
	 */
	private static void Menu(){

		// An input the validator rejects — a blank field, letters, or a run of digits too long for
		// an int — falls out of the if below and returns to the top of the loop, re-asking. That is
		// what the old unconditional Menu() call did, and what its default branch's Menu() did.
		while(true){
			input = Presenter.askMenu(arr.getLength(), arr.getElement());

			// Cancel makes showInputDialog return null. The 2019 code called System.exit(0)
			// here to match option 9. It now returns instead, which is the honest control-flow
			// signal: this loop is over. Process teardown is main's business, and main does it
			// immediately - see the note there, because returning is NOT equivalent to exiting.
			if(input == null){
				return;
			}
			if(Validator.isInt(input)){
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
					showSortTrace("Quick Sort", QuickSort.quickSortTrace(arr.getCopy()),
							QuickSort.quickSort(arr.getCopy()));
					break;

				case 6:
					searchLinear(arr.getCopy(), arr.getElement());
					break;

				case 7:
					searchExponential(arr.getsorted());
					break;

				case 8:
					searchJump(arr.getsorted());
					break;

				case 9:
					// The user asked to quit. Same as Cancel: the loop is over, and main ends
					// the process.
					return;

				default:
					// A number with no case above it, such as 0 or 10. Falling out of the switch
					// returns to the top of the loop and re-asks, which is what the old default
					// branch's Menu() call did.
				}
			}
		}
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
	 * Repeatedly searches the sorted array with exponential search until the user leaves.
	 *
	 * <p>Task 19. Option 7 showed "not implemented yet" since Task 5 replaced the empty 2019
	 * branch; the commented-out call it replaced had never run. Same shape as {@code searchJump}:
	 * the array is already sorted, so the dialog shows the sorted values and the loop returns to
	 * the menu on Cancel.
	 *
	 * @param sorted the values to search, already sorted; never modified
	 */
	private static void searchExponential(int[] sorted){
		String sortedText = asElements(sorted);
		String position = " ";
		while(true){
			String key = askSearchKey("Exponential Search", "Sorted", sortedText, position);
			if(key == null){
				return;
			}

			int search = Integer.parseInt(key);
			int found = ExponentialSearch.exponentialSearch(sorted, search);
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
	 * <p>The validation loop lives here rather than in {@link Presenter} on purpose. {@link Validator#isInt}
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
			if(reply == null || Validator.isInt(reply)){
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
		if(!Validator.isInt(str)){
			return false;
		}
		// Validator.isInt already proved the string parses, so this cannot throw.
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

	/**
 * Starts the app, then ends the process.
 *
 * <p>The explicit {@link System#exit(int)} is not ceremony. Returning from {@code main} looks
 * equivalent and is not: Swing's AWT threads are non-daemon and wind down through an auto-shutdown
 * timer, so the JVM lingers. Measured on this machine, from the last dialog being answered to the
 * shutdown hook firing — {@code System.exit} at ~7ms, a bare return at ~1310ms. No window is visible
 * during that gap, but the app takes over a second to disappear, which a user pressing Exit reads as
 * a hang.
 *
 * <p>So control flow and process lifetime are separated deliberately: {@link #Menu()} returns to say
 * "the loop is over", and this method decides what that means for the process. Task 14 removed the
 * {@code System.exit} calls from inside the menu for exactly this reason - they were control flow
 * masquerading as process management.
 */
public static void main(String []args){
		new Runner();
		System.exit(0);
	}

}
