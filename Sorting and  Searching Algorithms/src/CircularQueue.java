import java.util.Scanner;



public class CircularQueue {

    private int items[];
    private int front;
    private int rear;
    private int size=100;
    
    CircularQueue(){
        front = -1;
        rear = -1;
        items = new int[size];
    }
    
    public boolean isFull(){
        if (front == 0 && rear == size-1){
            return true;
        }
        if (front == rear+1){
            return true;
        }
        return false;
    }
    
    public boolean isEmpty(){
        if(front==-1) return true;
        else return false;
    }
    
    public void enqueue(int element){
        if (isFull()){
            System.out.println("Queue is full");
        }else{
            if (front==-1)
            front = 0;
            rear = (rear+1)%size;     
            items[rear] = element;
            System.out.println("Inserted "+element);    
        }
    }
    
    public int dequeue(){
        int element;
        if (isEmpty()){
            System.out.println("Queue is empty");
            return -1;
        }else{
            element = items[front];
            if (front==rear){
                front = -1;
                rear = -1;
            }else{
                front = (front+1)%size;
            }
            return (element);
        }
    }
    
    public void display(){
        int i;
        if (isEmpty()){
            System.out.println("Queue is empty");
        }else{
            System.out.println("Front ->"+(front+1));
            System.out.print("Items:");
            for (i=front; i!=rear ; i=(i+1)%size){
                System.out.print("\t"+items[i]);
            }
            System.out.print("\t"+items[i]);
            System.out.println("\nRear ->"+(rear+1));
        }
    }
    
    
    public static void main(String[] args) {
    	CircularQueue cq = new CircularQueue();
    	Scanner sc = new Scanner(System.in);
    	int len, elem, x=0;
    	String opt, input;
    	
    		do{
    			
        	       System.out.print("\n\tMENU\n1 Enqueue\n2 Dequeue\n3 Display\n4Exit\n");
        	       
        	       do{
        	    	   System.out.print("Please input the corresponding number of your choose:");
            	       opt = sc.next();
        	       }while(!isInteger(opt));
        	       
        	       switch(Integer.parseInt(opt)){
        	       case 1:
        	    	   do{
        	        	   System.out.print("Enter the element in queue: ");
        	        	   elem = sc.nextInt();
        	        	   cq.enqueue(elem);
        	        	   System.out.print("Do you want insert other element?y/n: ");
        	        	   opt = sc.next();
        	           }while(opt.equalsIgnoreCase("y"));
        	    	   break;
        	       case 2:
        	    	   do{
        	    		   cq.dequeue();
        	        	   System.out.print("Do you want delete again?y/n: ");
        	        	   opt = sc.next();
        	           }while(opt.equalsIgnoreCase("y"));
        	    	   break;
        	       case 3:
        	    	  
        	    		   cq.display();
        	        	   
        	           
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
        

    	
    	
        /*CircularQueue cq = new CircularQueue();
        //cq.dequeue();
        cq.enqueue(5);
        cq.enqueue(1);
        cq.enqueue(2);
        cq.enqueue(3);
        cq.enqueue(4);
        cq.enqueue(9);

        cq.display();
        
        cq.dequeue();
        
        cq.display();*/
        
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