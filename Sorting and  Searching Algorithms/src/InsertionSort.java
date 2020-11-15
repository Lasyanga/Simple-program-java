import javax.swing.JOptionPane;


public class InsertionSort {
    private Runner quiano;
	private String unSorted =" ", element =" ";
	private int insertion [], temp, x, flag = 0;
	
	public InsertionSort(int array[]){
		insertion = array.clone();
		
		for(int i : array){
			unSorted += String.valueOf(i+" ");
		}
		Sorting();
		insertionGUI();
	}

	public void insertionGUI(){
		JOptionPane.showMessageDialog(null, "Unsorted element: "+unSorted+"\nInsertion Sort Process:\n" +
				""+getInsertionProcess(),"Insetion Sort",1 );
		
		//Display menu
		quiano.Menu();
	}
	
	public void Sorting(){
		for(int i = 1; i < insertion.length; i++){
			temp  = insertion[i];
			x = i;
			while(x > 0 && insertion[x-1] > temp){
				insertion[x] = insertion[x-1];
				x -= 1;
			}
			insertion[x] = temp;
			getInsertionProcess();
			if(i == insertion.length -1){
				flag = 1;
			}
		}
		
	}
	
	public String getInsertionProcess(){
		if(flag == 1){
			element +="Sorted element: ";
			for(int j : insertion)
				element += String.valueOf(j+"   ");
				
			element += "\n";
		}else{
			for(int j : insertion)
				element += String.valueOf(j+"   ");
				
			element += "\n";
		}		
		return element;
	}
	
	public int[] getSortedInsertion(){
		return insertion;
	}

}
