

import java.util.StringTokenizer;

import javax.swing.JOptionPane;


public class JumpSearch {
	
	private static Runner quiano;
	private static StringTokenizer st;
	private static String element=" ", input=" ", position=" ";
	private static int jump[], len, step, index, inpt[] = new int[2];
	
	
	public JumpSearch(int[] array) {
		JumpSearch.jump = array.clone();
		element =" ";
		for(int i: array){
			element +=i+" ";
		}
		JumpsearchGUI();
		
	}
	
	public static void JumpsearchGUI(){
		try{
			int i = -1;
			do{				
				input=JOptionPane.showInputDialog(null,"Sorted element:"+element+"\n" +
						position+
						"\nEnter the you want to Search:[element][interval]"
						,"Jump Search",1);
				
			}while(!Runner.intOnly(st.nextToken()));
			
			int opt = Integer.parseInt(input);
			
			if(opt == JOptionPane.CANCEL_OPTION){			
				Runner.Menu();
			}else{
				int search = Integer.parseInt(input);
				index = jumpSearch(jump, search);
				position+="Element @ index: "+index;
			JumpsearchGUI();
			}
			
		}catch(Exception e){
			
		}
		
	}	
	public static int jumpSearch(int[] array, int key){ 
		len = array.length;

		step = (int)Math.floor(Math.sqrt(len)); 

		int prev = 0; 
		while (array[Math.min(step, len)-1] < key){ 
			prev = step; 
			step += (int)Math.floor(Math.sqrt(len)); 
			if (prev >= len) 
				return -1; 
		} 
		
		while (array[prev] < key){ 
			prev++; 			
			if (prev == Math.min(step, len)) 
				return -1; 
		} 		
		if (array[prev] == key) 
			return prev; 

		return -1; 
	}

	
}
