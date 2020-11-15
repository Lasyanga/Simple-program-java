import java.util.Scanner;
public class QueueJava {
   private int arr[];
   private int rear;
   private int front;
   private int capacity;
   private int count;
   
   QueueJava(int size){
       arr = new int[size];
       capacity = size;
       front = 0;
       rear = -1;
       count=0;
   }
   
   public int size(){
       return count;
   }
   
   public Boolean isFull(){
       return (size()==capacity);
   }
   
   public void enqueue(int item){
       if (isFull()){
           System.out.println("Overflow Program terminated.");
           //System.exit(1);
       }
       //insert
       System.out.println("Inserting "+item);
       rear = (rear+1)%capacity;
       arr[rear]=item;
       count++;
    }
   
    public int peek(){
        return arr[front];
    }
    
     public Boolean isEmpty(){
         return (size()==0);
     }
     
     public void dequeue(){
         if (isEmpty()){
           System.out.println("Underflow Program terminated.");
           //System.exit(1);
       }
      System.out.println("Deleting "+arr[front]);
      front = (front+1)%capacity;
      count--;
     }
    
    public static void main(String[] args) {
    	Scanner sc = new Scanner(System.in);
    	int len, elem, x=0;
    	String opt, input;
    	do{
    		System.out.print("Enter the size od queue: ");
        	input = sc.next();
    	}while(!isInteger(input));
    	
    	if(isInteger(input)){
    		len = Integer.parseInt(input);
    		
    		do{
        		System.out.println("The size of the queue is: "+len);
        	       QueueJava q = new QueueJava(len);
        	       System.out.print("\n\tMENU\n1 Enqueue\n2 Dequeue\n3 Peek\n4Exit\n");
        	       
        	       do{
        	    	   System.out.print("Please input the corresponding number of your choose:");
            	       opt = sc.next();
        	       }while(!isInteger(opt));
        	       
        	       switch(Integer.parseInt(opt)){
        	       case 1:
        	    	   do{
        	        	   System.out.print("Enter the element in queue: ");
        	        	   elem = sc.nextInt();
        	        	   q.enqueue(elem);
        	        	   System.out.print("Do you want insert other element?y/n: ");
        	        	   opt = sc.next();
        	           }while(opt.equalsIgnoreCase("y"));
        	    	   break;
        	       case 2:
        	    	   do{
        	    		   q.dequeue();
        	        	   System.out.print("Do you want delete again?y/n: ");
        	        	   opt = sc.next();
        	           }while(opt.equalsIgnoreCase("y"));
        	    	   break;
        	       case 3:
        	    	   do{
        	    		   q.peek();
        	        	   System.out.print("Do you want peek again?y/n: ");
        	        	   opt = sc.next();
        	           }while(opt.equalsIgnoreCase("y"));
        	    	   break;
        	       case 4:
        	    	   System.exit(0);
        	    	   break;
        	    	   
        	       default:
        	    	   System.out.print("Invalid Input...");
        	    	   x = 1;
        	       	
        	       }
        	       do{
        	    	   System.out.print("Do you want to see the Menu?y/n: ");
                	   opt = sc.next();
                	   
                	   if(opt.equalsIgnoreCase("y")){
                		   x = 1;
                	   }else{
                		   x= 0;
                		   System.exit(0);
                	   }
        	       }while(!isString(opt));
        	       
        	}while(x == 1);
    	}
    	
    	
       
    }
    private static boolean isString(String str){
		for(int i = 0; i < str.length(); i++){
			
			if(!Character.isLetter(str.charAt(i))){
				return false;
			}
			
		}
		return true;
	}
    private static boolean isInteger(String num){
		for(int i = 0; i < num.length(); i++){
			
			if(!Character.isDigit(num.charAt(i))){
				return false;
			}
			
		}
		return true;
	}
}