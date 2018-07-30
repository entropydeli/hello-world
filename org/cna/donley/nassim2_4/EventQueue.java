package org.cna.donley.nassim2_4;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.LinkedList;


/**
 * Is the main event queue for the event driven simulation of the NAS network.
 * The queue is a priority one and orders by a {@link IEvent} times.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: EventQueue.java 1 2009-12-17 00:00:00EST $
 */

public class EventQueue implements IQueue<IEvent>
{ 	
	/** name of the class */
	private String className;
	
	/**
     * Some value to initialize the priority queue with.
     */
    private static final int INITIAL_CAPACITY = 10;
    /**
     * This comparator orders events according to their event time
     * The compare method must satisfy certain properties such as 
     * sgn(x,y) = -sgn(y,x).  
     * See Java API notes for those.
     */
    private final Comparator<IEvent> soonestTimeComparator = 
    	new Comparator<IEvent>()
        {
            public int compare(IEvent left, IEvent right){
                return left.compareTo(right);
             }
        };
	/**
	 * Priority queue.  Orders by event time.
	 */
	private final Queue<IEvent> queue = 
    	new PriorityQueue<IEvent>(INITIAL_CAPACITY, soonestTimeComparator);
	
	/** Make default constructor private. */
	public EventQueue(){};
	
	/**
	 * Removes all elements from the queue.
	 */
	public void clear(){
		queue.clear();
	}
	/**
	 * Adds an event to the queue.
	 * @param event
	 */
	public void add(IEvent event){
		queue.add(event);
	}
	/**
	 * Removes the event at the top of queue and returns it.
	 * @return event at top of queue.
	 */
	public IEvent poll(){
		return queue.poll();
	}
	/**
	 * Returns the event at the top of queue w/o removing it from the queue.
	 * @return event at the top of queue w/o removing it from queue
	 */
	public IEvent peek(){
		return queue.peek();
	}
	/**
	 * Returns the number of elements in this queue.
	 * @return number of elements in queue.
	 */
	public int size(){
		return queue.size();
	}
  
}
