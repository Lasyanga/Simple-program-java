import javax.swing.JOptionPane;


public class SelectionSort {
	private Array arr;
	private Runner quiano;
	private int selection [], min, temp, flag = 0;
	private String unSorted = " ", element=" ";

	public SelectionSort(int array[]) {
		for(int i : array){
			unSorted += String.valueOf(i+" ");
		}
		this.selection = array.clone();
		
		Sorting();
		selectionGUI();
	}
	
	public void selectionGUI(){
		JOptionPane.showMessageDialog(null, "Unsorted element: "+unSorted+"\nSelection Sort Process:\n" +
				""+getSelectionProcess(),"Selection Sort",1 );
		
		//Display menu
		Runner.Menu();
	}
	
	public void Sorting(){
		for(int i = 0; i < selection.length; i++){
			min = i;
			for(int j = i+1; j < selection.length; j++){
				if(selection[j] < selection[min]){
					min = j;
				}
			}
			temp = selection[min];
			selection[min] = selection[i];
			selection[i] = temp;
			getSelectionProcess();
			if(i == selection.length -1){
				flag = 1;
			}
		}
		
	}
	
	public String getSelectionProcess(){
		if(flag == 1){
			element +="Sorted element: ";
			for(int k : selection)
				element += String.valueOf(k+" ");
		}else{
			for(int k : selection)
				element += String.valueOf(k+" ");
			element+="\n";
		}
		return element;
	}
	
	public int[] sorted(){		
		return selection;
	}
		

}
