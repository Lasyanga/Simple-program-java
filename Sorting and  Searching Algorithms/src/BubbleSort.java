import javax.swing.JOptionPane;


public class BubbleSort {
	private Runner quiano;
	private int bubble[], temp, flag;
	private String element =" ",dispElement= " ";

	public BubbleSort(int []array) {
		bubble = array.clone();
		for(int i: array){
			dispElement += String.valueOf(i+" ");
		}
		Sorting();
		displayGUI();
	}

	public void displayGUI(){
		JOptionPane.showMessageDialog(null, "Unsorted element: "+dispElement+"\nBubble Sort Process:\n" +
				""+getBubbleSort(),"Bubble Sort",1 );
		quiano.Menu();
	}
	
	public String getBubbleSort(){
		if(flag == 0){
			element+="Sorted Element: ";
			for(int i = 0; i < bubble.length; i++)
				element += String.valueOf(bubble[i]+"   ");						
			element+=" ";
		}else{
			for(int i = 0; i < bubble.length; i++)
				element += String.valueOf(bubble[i]+"   ");
			element+="\n";
		}
		return element;
	}
	
	public void Sorting(){
		for(int i =0; i < bubble.length; i++){
			flag = 0;
			for(int j = 0; j < bubble.length-1-i; j++){
				if(bubble[j] > bubble[j+1]){
					temp = bubble[j];
					bubble[j] = bubble[j+1];
					bubble[j+1] = temp;
					flag = 1;
					getBubbleSort();
				}
			}
			if(flag == 0){
				break;
			}
		}
	}
	
	public int[] getSortedBubble(){
		return bubble;
	}
	
	
}
