package org.cna.donley.nassim2_4;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.LinkedList;


/**
 * Implements the {@link IQueue} interface for an event driven simulation
 * of a National Airspace (NAS) network.  The queue orders by a {@link IFlight}
 * measure supplied by the user.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: FlightQueue.java 1 2009-12-03 00:00:00EST $
 */

public class FlightQueue implements IQueue<IFlight>
{ 	
	/** name of the class */
	private String className;
	
	/** measure to use to order the flights. */
	private IFlight.Param measure;
	/**
     * Some value to initialize the priority queue with.
     */
    private final int INITIAL_CAPACITY = 10;
    /**
     * This comparator orders events according to the size of their measure,
     * a time of some sort.  The compare method must satisfy certain properties 
     * such as sgn(x,y) = -sgn(y,x). 
     * See Java API notes for those.
     */
    private final Comparator<IFlight> comparator = 
    	new Comparator<IFlight>()
        {
            public int compare(IFlight left, IFlight right){
            	// Best to use the {@link IFlight.compareTo} method, as that
            	// is consistent with its equals() method; however, can't do
            	// that here.  On the other hand, there will be no case in which
            	// a.equals(b), yet compare(a,b) != 0, so I think we are safe.
            	if(left.get(measure) < right.get(measure)) return -1;
            	else if(left.get(measure) > right.get(measure)) return 1;
            	else return 0;
             }
        };
	/**
	 * Priority queue.  Orders flights according to the measure above; however,
	 * if that measure is <code>null</code> then it implements a FIFO queue.
	 */
	private Queue<IFlight> queue;
	
	/** Make default constructor private. */
	private FlightQueue(){};
	
	/**
	 * Constructor.
	 * @param measure to use by which to order flights in a priority queue.  
	 *   If <code>null</code>, then a FIFO queue as a linked list is 
	 *   implemented instead.
	 */
	public FlightQueue(IFlight.Param measure){
		className = this.getClass().getName();
		this.measure = measure;
		if(measure != null){
			queue = new PriorityQueue<IFlight>(INITIAL_CAPACITY, comparator);
		}else {
			queue = new LinkedList<IFlight>();
		}
	}
	/** 
	 * Removes all elements from the queue.
	 */
	public void clear(){
		queue.clear();
	}
	/**
	 * Adds a flight to the queue.
	 * @param flight
	 */
	public void add(IFlight flight){
		queue.add(flight);
	}
	/**
	 * Removes the flight at the top of queue and returns it.
	 * @return flight at top of queue.
	 */
	public IFlight poll(){
		return queue.poll();
	}
	/**
	 * Returns the flight at the top of queue w/o removing it from the queue.
	 * @return flight at the top of queue.
	 */
	public IFlight peek(){
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
