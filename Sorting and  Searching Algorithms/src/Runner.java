import javax.swing.JOptionPane;


public class Runner {
	private static String length = " ", insElem, input ="";;
	private static int size , count = 0, element, sort = 0;
	private static Array arr;
	private static BubbleSort bubble;
	private static InsertionSort insertion;
	private static SelectionSort selection;
	private static MergeSort merge;
	private static Quicksort quick;
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
				
				if(intOnly(length)){					
					size = Integer.parseInt(length);
					arr = new Array(size);
					
					do{
						do{
							insElem = JOptionPane.showInputDialog(null, 
									"Please insert the Element["+count+"]:",
									"Sorting and Searching",1);
							if(intOnly(insElem)){
									element = Integer.parseInt(insElem);
									arr.setElement(count, element);
									count++;
								}
						}while(!intOnly(insElem));						
					}while(count < size);
					
					arr.setCopy();
					Menu();
				}
			}while(!intOnly(length));
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
			
			
			if(intOnly(input)){
				switch(Integer.parseInt(input)){
				case 1:
					sort +=1;
					bubble = new BubbleSort(arr.getCopy());
					break;
					
				case 2:
					insertion = new InsertionSort(arr.getCopy());
					sort += 1;
					break;
					
				case 3:
					sort += 1;
					selection = new SelectionSort(arr.getCopy());
					break;
					
				case 4:
					sort += 1;
					merge =new MergeSort(arr.getCopy(), arr.getLength());
					break;
					
				case 5:
					quick = new Quicksort(arr.getCopy());
					sort += 1;
					break;
					
				case 6:;
					linearSearch = new LinearSearch(arr.getCopy(), arr.getElement());
					break;
					
				case 7:
					if(sort == 0){
						JOptionPane.showMessageDialog(null, "You Must sort the array element\n" +
								" in able to perform this algorithm.","Message", 1);
						Menu();
					}else{
					//	expo = new ExponentialSearch(arr.getsorted());
					}
					break;
					
				case 8:
					if(sort == 0){
						JOptionPane.showMessageDialog(null, "You Must sort the array element\n" +
								" in able to perform this algorithm.","Message", 1);
						Menu();
					}else{
						jump = new JumpSearch(arr.getsorted());
					}
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
		for(int i = 0; i < str.length(); i++){
			if(!Character.isDigit(str.charAt(i))){
				return false;
			}
		}
		return true;
	}
	
	public static void main(String []args){
		new Runner();
	}

}
