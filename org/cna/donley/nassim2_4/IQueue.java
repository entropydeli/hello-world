package org.cna.donley.nassim2_4;


/**
 * An interface for queues used in Nassim, and simulation
 * of a National Airspace (NAS) network.  Methods here are essentially those
 * in {@link java.util.PriorityQueue}.  Are doing this instead of
 * {@link java.util.Queue} because there is a lot of crap in that that
 * needs to be implemented.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: IQueue.java 1 2009-12-03 00:00:00EST $
 * @param <E> Generic type.
 */

public interface IQueue<E>
{ 	
	/** 
	 * Removes all elements from the queue.
	 */
	public void clear();
	/**
	 * Adds a t to the queue.
	 * @param t
	 */
	public void add(E t);
	/**
	 * Removes the t at the top of queue and returns it.
	 * @return t at top of queue.
	 */
	public E poll();
	/**
	 * Returns the t at the top of queue w/o removing it from the queue.
	 * @return t at the top of queue.
	 */
	public E peek();
	/**
	 * Returns the number of elements in this queue.
	 * @return number of elements in queue.
	 */
	public int size();
  
}
