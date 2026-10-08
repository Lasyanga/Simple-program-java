package algorithms;

import java.util.Scanner;


/**
 * A fixed-capacity circular (ring) buffer, plus the console menu that drives it.
 *
 * <p><b>Task 21 separated the two.</b> The queue operations — {@link #enqueue(int)},
 * {@link #dequeue()}, {@link #display()}, {@link #isFull()}, {@link #isEmpty()} — no longer write
 * to {@code System.out}; they return their result and {@link #main} prints it. That is the same
 * move Tasks 6-13 made for the algorithms, which used to open their own {@code JOptionPane}:
 * presentation belongs to the caller, not to the data structure. It is what makes the structure
 * testable without a console attached — see {@code CircularQueueTest}.
 *
 * <p>Before this, {@code enqueue} printed "Inserted 5" as a side effect of storing 5, so a test
 * asserting the queue's contents also emitted the demo's narration.
 *
 * <p>Note that unlike {@code QueueJava}, this queue is constructed <b>once</b>, outside the menu
 * loop, so items survive a trip back to the menu. {@code QueueJava} rebuilds itself on every
 * visit, which is why its successful dequeue and peek paths are unreachable from the console.
 */
public class CircularQueue {

    private int items[];
    private int front;
    private int rear;
    private int size=100;

    CircularQueue(){
        this(100);
    }

    /**
     * @param size capacity; must be at least 1
     */
    CircularQueue(int size){
        if(size < 1){
            throw new IllegalArgumentException("Queue size must be at least 1, was "+size);
        }
        this.size = size;
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

    /**
     * Stores {@code element} at the rear.
     *
     * @return true if stored, false if the queue was full
     */
    public boolean enqueue(int element){
        if (isFull()){
            return false;
        }
        if (front==-1)
        front = 0;
        rear = (rear+1)%size;
        items[rear] = element;
        return true;
    }

    /**
     * Removes and returns the front element.
     *
     * @return the element that was at the front, or -1 if the queue was empty. The -1 sentinel
     *         is the 2019 contract, kept unchanged; callers that must distinguish "empty" from
     *         "stored -1" should test {@link #isEmpty()} first, which is what {@code main} does.
     */
    public int dequeue(){
        int element;
        if (isEmpty()){
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

    /**
     * The queue as the demo prints it: a front marker, the items, and a rear marker.
     *
     * @return the formatted block, including its trailing newline; never null. "Queue is empty"
     *         plus a newline when there is nothing to show.
     */
    public String display(){
        int i;
        if (isEmpty()){
            return "Queue is empty\n";
        }
        StringBuilder out = new StringBuilder();
        out.append("Front ->").append(front+1).append('\n');
        out.append("Items:");
        for (i=front; i!=rear ; i=(i+1)%size){
            out.append('\t').append(items[i]);
        }
        out.append('\t').append(items[i]);
        out.append('\n').append("Rear ->").append(rear+1).append('\n');
        return out.toString();
    }


    public static void main(String[] args) {
    	CircularQueue cq = new CircularQueue();
    	Scanner sc = new Scanner(System.in);
    	int len, elem, x=0;
    	String opt, input;

    		do{

        	       System.out.print("\n\tMENU\n1 Enqueue\n2 Dequeue\n3 Display\n4 Exit\n");

        	       do{
        	    	   System.out.print("Please enter the number of your choice:");
            	       opt = sc.next();
        	       }while(!Validator.isInt(opt));

        	       switch(Integer.parseInt(opt)){
        	       case 1:
        	    	   do{
        	        	   System.out.print("Enter the element to enqueue: ");
        	        	   elem = sc.nextInt();
        	        	   if(cq.enqueue(elem)){
        	        		   System.out.println("Inserted "+elem);
        	        	   }else{
        	        		   System.out.println("Queue is full");
        	        	   }
        	        	   System.out.print("Do you want to insert another element? (y/n): ");
        	        	   opt = sc.next();
        	           }while(opt.equalsIgnoreCase("y"));
        	    	   break;
        	       case 2:
        	    	   do{
        	    		   if(cq.isEmpty()){
        	    			   System.out.println("Queue is empty");
        	    		   }else{
        	    			   cq.dequeue();
        	    		   }
        	        	   System.out.print("Do you want to delete again? (y/n): ");
        	        	   opt = sc.next();
        	           }while(opt.equalsIgnoreCase("y"));
        	    	   break;
        	       case 3:

        	    		   System.out.print(cq.display());


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
