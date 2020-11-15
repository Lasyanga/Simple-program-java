import javax.swing.JOptionPane;

public class Quicksort {
	private Runner quiano;
	private String unSorted = " ", element=" ", sorted=" ";
	private int quick[];

	public Quicksort(int array[]) {
		quick = array.clone();
		int len = quick.length;
		int i = 0;
		while(i < quick.length){
			unSorted+= String.valueOf(quick[i]+" ");
			i++;
		}
		quickRecursion(quick, 0, len-1);
		sorted +="\nSorted element: ";
		for(int x = 1; x < quick.length+1; x++){
			sorted+=String.valueOf(x+" ");
		}
		QuickSortGUI();
			
			
	}
	
	public void QuickSortGUI(){
		JOptionPane.showMessageDialog(null, "Unsorted element: "+unSorted+"\nQuick Sort Process:\n"+
				""+getProcess()+sorted,"Quick Sort",1 );
		
		//Display menu
		quiano.Menu();
	}
	
	public int partion(int arr[],int low, int high){
		int pivot = arr[(low+high)/2];

		while(low < high){
			while(arr[low] < pivot){
				low++;
			}
			while(arr[high] > pivot){
				high --;
			}
			if (low <= high) {
				int temp = arr[low];
				arr[low] = arr[high];
				arr[high] = temp;
				
				low ++;
				high--;
			}
		}

		return low;

	}

	public void quickRecursion(int arr[], int low, int high){
		int pi = partion(arr, low, high);

		if (low < pi-1) {
			quickRecursion(arr, low, pi-1);
			for(int i =low; i <= pi-1;i++){
				element+=  quick[i]+" ";
			}
			element +="\n";
			
		}
		if (pi < high) {
			quickRecursion(arr, pi, high);
			for(int i =pi; i <= high;i++){
				element += quick[i]+" ";
			}
			element +="\n";
		}
	}
	
	public String getProcess(){
		
		return element;
	}
	

}
