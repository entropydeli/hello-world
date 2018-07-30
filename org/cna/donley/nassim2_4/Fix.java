package org.cna.donley.nassim2_4;

/**
 * An implementation of the interface {@link INode} for an arrival or
 * departure gate fix. "Fix" denotes a fixed point in space that a plane
 * must fly over when departing from or arriving to the airport vicinity.
 * <p>
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: Fix.java 1 2009-11-04 00:00:00EST $
 */

public class Fix implements INode
{
	/**
	 * Name of fix.  Is an integer which then maps to a real name using a method
	 * in the {@link Nas} object.
	 */
	private int nameAsInt;
	/**
	 * Type of fix.  Arrival or departure.
	 */
	private Nas.Ad type;
	/**
	 * Minimum time between planes traversing this fix.  Units: seconds.
	 */
	private int minTimeSpacing;
	/**
	 * Next time a plane is allowed to traverse this fix.  If a plane
	 * traverses the fix at time t, then the next time is t + min_time_spacing.
	 * Units: seconds.
	 */
	private int nextTime = 0;
	/**
	 * Last time a plane is traversed this fix.  Units: seconds.
	 */
	private int lastTime = 0;
	/**
     * Measures used to order flights in the fix queue.
     */
    private IFlight.Param measure = null;
	/**
     * Fix queue}.
     */
    private IQueue<IFlight> queue = null;
    /**
     * Status of holds on queue.  If <code>true</code> then there
     * is a {@link FixEvent} to be executed when the hold time is up.  This
     * flag is used to prevent more than one event having to be created
     * to "release" the hold.
     */
    private boolean queueHoldStatus = false;
    
    /**
     * Make default constructor private.
     */
    private Fix(){}
    
    /**
     * @param nameAsInt  Index that maps to a fix name using the appropriate
     *   {@link Nas} method. 
     * @param type Type of fix.  Arrival or departure.
     * @param minTimeSpacing  Minimum time between planes traversing this fix.
     *   Units: seconds.  
     */
    public Fix(int nameAsInt, Nas.Ad type, int minTimeSpacing){
    	this.nameAsInt = nameAsInt;
    	this.type = type;
    	this.minTimeSpacing = minTimeSpacing;
    	nextTime = 0;
    	lastTime = -Integer.MAX_VALUE;
    	
    	// Create fix queue and associated objects. 
    	if(type == Nas.Ad.DEP){
    		measure= IFlight.Param.ACT_OFF_TIME;
    	} else {
    		measure = IFlight.Param.CALC_ON_TIME;
    	}
    	queue = new FlightQueue(measure);
    	queueHoldStatus = false;// false at first obviously.
    } 
    /**
     * Get a fix queue.  Departure or arrival.
     * @return fix queue.
     */
    public IQueue<IFlight> getQueue(){
    	return queue;	
    }
    /**
     * Get the hold status of the queue.  If <code>true</code> then some
     * event has been created, which when executed at the hold end time, w
     * ill access the queue.
     * @return status of hold on events associated with this fix.
     */
    public boolean getQueueHoldStatus(){
    	return queueHoldStatus;
    }
    /**
     * Sets the hold status of the queue.
     * @param status If <code>true</code>, then 
     */
    public void setQueueHoldStatus( boolean status){
    	queueHoldStatus = status;
    }
    /**
     * Gives the name of the fix as an index.  The name can then be obtained
     * by mapping the index using a method in the {@link Nas} object.
     * @return name of fix as an integer index.
     */
    public int getName(){return nameAsInt;}
    
    /**
     * Gets the next time a plane can traverse this fix.
     * @return Next time a plane can traverse this fix.
     */
    public int getNextTime(){return nextTime;}
    /**
     * Gets the last time a plane traversed this fix.
     * @return Last time a plane traversed this fix.
     */
    public int getLastTime(){return lastTime;}
    /**
     * Get type of fix.
     * @return type of fix. 
     */
    public Nas.Ad getFixType(){return type;}
    /**
     * Computes the minimum time a plane is allowed to traverse this
     * fix, assuming the last time was the input value.
     * @param time The last time a plane traversed this fix.  Units:
     *   seconds.
     */
    public void computeNextTime(int time){
    	lastTime = time;
    	nextTime = lastTime + minTimeSpacing;
    }
    
    //--------------------------------------------------------------------------
    // Methods for testing only
    //--------------------------------------------------------------------------
    /**
     * Get the min time spacing between aircraft.
     * @return min time spacing.
     */
    public int getMinTimeSpacing(){return minTimeSpacing;}
    /**
     * @return Measure used to order flights in queue.
     */
    public IFlight.Param getMeasure(){return measure;}
    
    //-----------------------------------------------------------------------
    // Legacy crap
    //-----------------------------------------------------------------------
    /**
     * Used to send a message to the node. Does nothing right now.
     * @param sender The sender of the message.
     * @param message What the node should do.
     * @return <code>true</code> if accepted; <code>false</code> if not.
     */
    public synchronized boolean receive(Object sender, Object message){
    	return false;
    }
    /**
     * Sets a neighbor of this node. Legacy crap.
     * @param n The neighboring element.
     */
    public void setNeighbor(INode n){};	
    /**
     * Gets the node neighbors as an array.  Legacy crap.
     * @return array of node neighbors.
     */
    public INode[] getNeighbors(){return null;}
    
}
