import javax.swing.JOptionPane;


public class Runner {
	private static String length = " ", insElem, input ="";;
	private static int size , count = 0, element;
	private static Array arr;
	private static LinearSearch linearSearch;
	private static JumpSearch jump;
	
	public Runner(){
		GUI();
	}
	
	
	public void GUI(){
		try{
			do{
				length = JOptionPane.showInputDialog(null, "Professor: Nikka Salvador\n" +
						"Submitted By: Herminigildo Jr. Quiano\n\n" +
						"Welcome!!\n" +
						"This program show diff.\n" +
						"Sorting and Searching Method.\n\n" +
						"Please input the length of your array:",
						"Sorting and Searching", 1);
				
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
					insElem = JOptionPane.showInputDialog(null, 
							"Please insert the Element["+count+"]:",
							"Sorting and Searching",1);
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
			JOptionPane.showMessageDialog(null, "Bye.. bye..\n(@~__~)", "Message from Cowboy",1);
		}
		
	}
	
	public static void Menu(){
	
		do{
			input = JOptionPane.showInputDialog(null, "Length of your Array: "+arr.getLength()+"\n" +
					"Element you input: "+arr.getElement()+"\n\n" +
							"[1]Bubble Sort\n" +
							"[2]Insertion Sort\n" +
							"[3]Selection Sort\n" +
							"[4]Merge Sort\n" +
							"[5]Quick\n" +
							"[6]Linear Search\n" +
							"[7]Exponential Search\n" +
							"[8]Jump Search\n" +
							"[9]Exit", "Menu", JOptionPane.INFORMATION_MESSAGE);
			
			
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
					
				case 6:;
					linearSearch = new LinearSearch(arr.getCopy(), arr.getElement());
					break;
					
				case 7:
					JOptionPane.showMessageDialog(null, "Exponential Search is not implemented yet.",
						"Message", 1);
					break;
				
				case 8:
					jump = new JumpSearch(arr.getsorted());
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
	 * <p>Temporary home for this dialog. Task 13 collapses every xxxGUI method and this helper
	 * into a single Presenter; until then the extracted sorts display through here so the menu
	 * keeps working. Cases 2-5 still show their own dialogs from inside their algorithm classes.
	 *
	 * @param title window and heading text, e.g. "Bubble Sort"
	 * @param trace one array state per swap, in order; may be empty if the input was sorted
	 * @param result the final sorted array
	 */
	private static void showSortTrace(String title, java.util.List<int[]> trace, int[] result){
		StringBuilder steps = new StringBuilder();
		for(int[] state : trace){
			for(int i = 0; i < state.length; i++){
				steps.append(state[i]).append("   ");
			}
			steps.append("\n");
		}
		JOptionPane.showMessageDialog(null, "Unsorted element: "+arr.getElement()+"\n"+
				title+" Process:\n"+steps+"\nSorted Element: "+java.util.Arrays.toString(result),
				title, 1);
	}

	public static void main(String []args){
		new Runner();
	}

}
