import javax.swing.JOptionPane;


public class MergeSort {
	private Runner quiano;
	private String unSorted =" ", element = " ", sorted =" ";
	private int merge[], length, temparr[];
	int count = -1, x = 0, y;
	int round = 0;
	

	public MergeSort(int array[], int length) {
		for(int i: array){
			unSorted += String.valueOf(i+" ");
		}
		
		
		this.merge =  array.clone();
		this.length = length;
		this.temparr = new int[length];

		Sorting(0, length-1);
		mergeGUI();
		
	}
	
	public void mergeGUI(){
		JOptionPane.showMessageDialog(null, "Unsorted element: "+unSorted+"\nMerge Sort Process:\n"+
				""+getMergeProcess()+sorted(),"Merge Sort",1 );
		
		//Display menu
		quiano.Menu();
	}
	
	public void Sorting(int lowIndex, int highIndex){
		x = 1;

		if(lowIndex < highIndex){
			int middle = lowIndex + (highIndex - lowIndex)/2;

			for(int i = lowIndex; i <= middle; i++){
				element+= merge[i]+" ";
				if(i == middle){
					++count;
					if(count > 0){
						round = i + 1;
						if(round < middle){
							round = i;
							x = 0;
							y = lowIndex;
						}
					}
				}
			}
			
			
			
			getMergeProcess();


			Sorting(lowIndex, middle);
			

			Sorting(middle+1, highIndex);

			Merge(lowIndex, middle, highIndex);
		}
	}
	
	public void Merge(int lowIndex, int middle, int highIndex){
		for(int i = lowIndex; i <= highIndex; i++){
			temparr[i] = merge[i];
		}
		
		int i = lowIndex;
		int j = middle + 1;
		int k = lowIndex;
		
		while(i <= middle && j <= highIndex){
			if(temparr[i] <= temparr[j]){
				merge[k] = temparr[i];
				i++;
			}else{
				merge[k] = temparr[j];
				j++;
			}
			k++;
		}
		
		while(i <= middle){
			merge[k] = temparr[i];
			i++;
			k++;
		}
	}
	
	public String getMergeProcess(){
		if(count > 0){
			if(x == 0){
				element +="\n";
				element += merge[y]+" ";
			}
			if(x == 1){
				element += "   ";
				element += merge[round]+" ";
			}
		}

		element +="\n";
		return element;
	}
	
	public String sorted(){
		
		
		sorted +="Sorted element: ";	
		for(int i : merge){
			sorted += String.valueOf(i + " ");
		}
		return sorted;	
	}

}
