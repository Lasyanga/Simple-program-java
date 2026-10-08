package algorithms;

import java.util.Scanner;

/**
 * A bounded linear queue, plus the console menu that drives it.
 *
 * <p><b>Task 21 separated the two.</b> {@link #enqueue(int)} and {@link #dequeue()} used to print
 * "Inserting 5" / "Underflow Program terminated." as side effects of storing or removing; they now
 * report their outcome in their return value and {@link #main} does the printing. Same move as the
 * algorithms' dialogs in Tasks 6-13, and the reason {@code QueueJavaTest} can assert on the queue's
 * state without the demo's narration landing in the test output.
 *
 * <p>The bounds guards themselves are Task 4's work: both used to print a warning and fall
 * straight through, so an overflow still inserted and an underflow drove {@code count} to -1,
 * after which the queue reported neither empty nor full. They are covered by {@code QueueJavaTest}.
 *
 * <p><b>Known defect, out of scope here:</b> {@code main} constructs {@code new QueueJava(len)}
 * <i>inside</i> the outer menu loop, so every trip back to the menu discards the queue. A user can
 * never successfully dequeue or peek from the console — those paths only run empty. The structure
 * is fine; only the demo loses state. {@code CircularQueue} builds its queue outside the loop and
 * does not have this problem.
 */
public class QueueJava {
   private int arr[];
   private int rear;
   private int front;
   private int capacity;
   private int count;

   QueueJava(int size){
       if(size < 1){
           throw new IllegalArgumentException("Queue size must be at least 1, was "+size);
       }
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

   /**
    * Stores {@code item} at the rear.
    *
    * @return true if stored, false if the queue was full
    */
   public boolean enqueue(int item){
       if (isFull()){
           return false;
       }
       rear = (rear+1)%capacity;
       arr[rear]=item;
       count++;
       return true;
    }

    public int peek(){
        return arr[front];
    }

     public Boolean isEmpty(){
         return (size()==0);
     }

     /**
      * Removes and returns the front element.
      *
      * @return the element that was at the front, or -1 if the queue was empty. As with
      *         {@code CircularQueue}, -1 is the sentinel the 2019 code used to signal emptiness;
      *         {@code main} checks {@link #isEmpty()} first, so it never has to tell the two apart.
      */
     public int dequeue(){
         if (isEmpty()){
           return -1;
       }
       int removed = arr[front];
       front = (front+1)%capacity;
       count--;
       return removed;
     }

    public static void main(String[] args) {
    	Scanner sc = new Scanner(System.in);
    	int len, elem, x=0;
    	String opt, input;
    	do{
    		System.out.print("Enter the size of queue: ");
        	input = sc.next();
    	}while(!Validator.isInt(input));

    	if(Validator.isInt(input)){
    		len = Integer.parseInt(input);
      if(len < 1){
          System.out.println("The size of the queue must be at least 1.");
          return;
      }

    		do{
        		System.out.println("The size of the queue is: "+len);
        	        // Rebuilt every visit: state does not survive a trip back to the menu. That is
        	        // the pre-existing defect documented in this class's javadoc.
        	        QueueJava q = new QueueJava(len);
        	        System.out.print("\n\tMENU\n1 Enqueue\n2 Dequeue\n3 Peek\n4 Exit\n");

        	       do{
        	    	   System.out.print("Please enter the number of your choice:");
            	       opt = sc.next();
        	       }while(!Validator.isInt(opt));

        	       switch(Integer.parseInt(opt)){
        	       case 1:
        	    	   do{
        	        	   System.out.print("Enter the element to enqueue: ");
        	        	   elem = sc.nextInt();
        	        	   if(q.enqueue(elem)){
        	        		   System.out.println("Inserting "+elem);
        	        	   }else{
        	        		   System.out.println("Overflow Program terminated.");
        	        	   }
        	        	   System.out.print("Do you want to insert another element? (y/n): ");
        	        	   opt = sc.next();
        	           }while(opt.equalsIgnoreCase("y"));
        	    	   break;
        	       case 2:
        	    	   do{
        	    		   if(q.isEmpty()){
        	    			   System.out.println("Underflow Program terminated.");
        	    		   }else{
        	    			   System.out.println("Deleting "+q.dequeue());
        	    		   }
        	        	   System.out.print("Do you want to delete again? (y/n): ");
        	        	   opt = sc.next();
        	           }while(opt.equalsIgnoreCase("y"));
        	    	   break;
        	       case 3:
        	    	   do{
        	        	   q.peek();
        	        	   System.out.print("Do you want to peek again? (y/n): ");
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
        	       }while(!Validator.isLetters(opt));

        	}while(x == 1);
    	}


    }
}
