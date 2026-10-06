import javax.swing.JOptionPane;


public class LinearSearch {
	private static int arr[];
	private static String element;
	private static String input, position=" ";
	private static Runner quiano;

	public LinearSearch(int array[],String element) {
		this.arr = array.clone();
		this.element = element;
		linearSearchGUI();
		
	}
	
	public static void linearSearchGUI(){
		try{
			do{
				input=JOptionPane.showInputDialog(null,"Unsorted element: "+element+"\n" +
						position+
						"\nEnter the you want to Search:"
						,"Linear Search",1);
				// Cancel makes showInputDialog return null. intOnly reports null as
				// invalid rather than throwing, so without this check the condition
				// below re-prompts forever and the user is trapped in this dialog.
				// Going back to the menu is what the accidental NPE used to achieve.
				if(input == null){
					quiano.Menu();
					return;
				}
			}while(!quiano.intOnly(input));
			
			int opt = Integer.parseInt(input);
			
			if(opt == JOptionPane.CANCEL_OPTION){			
				quiano.Menu();
			}else{
			
			if(quiano.intOnly(input)){
				int search = Integer.parseInt(input);
				Searching(search);			
			}
			linearSearchGUI();
			}
			
		}catch(Exception e){
			
		}
		
	}
	
	public static String Searching(int search){
		int i =0, flag =0;
		position =" ";
		position+=search+" is @ index: ";
		while(i < arr.length){
			if(arr[i]== search){
				position+=i+" ";
				flag = 1;
			}
			i++;
		}
		if(flag == 1){
			linearSearchGUI();
		}else{
			position =" ";
			position+="Element "+search+" is not found.";
			
		}
		return position;
	}

}
