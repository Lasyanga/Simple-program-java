import java.util.Arrays;


public class Array {
	private int length, element[] = null, copy[] = null;
	private String elem=" ";

	public Array(int length) {
		this.length = length;
		element = new int [length];
	}
	
	public int getLength(){
		return length;
	}
	
	public void setElement(int index, int element){
		this.element[index] = element;
	}
	
	public String getElement(){
		elem = " ";
		for(int i : element)
			elem += String.valueOf(i+" ");	
		
		return elem;
	}
	
	public void setCopy(){
		copy = element.clone();
	}
	
	public int[] getCopy(){
		return copy;
	}
	
	public int[] getsorted(){
		int sorted []= copy.clone();
		Arrays.sort(sorted);
		return sorted;
	}
	
	

}
