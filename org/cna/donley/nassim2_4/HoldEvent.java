package org.cna.donley.nassim2_4;

import java.util.List;
import java.util.ArrayList;


/**
 * A class that implements the {@link IEvent} interface for a flight hold,
 * usually at the runway.  The purpose of this event is to hold flights in
 * the queue, yet keep their order intact.  So it modifies events already
 * present in the queue.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: HoldEvent.java 1 2009-09-08 00:00:00EST $
 */

public class HoldEvent implements IEvent
{
	/** The class name. */
	private final String className = "HoldEvent";

	/** Type of event to look for so can hold them. */
	private IEvent.Type typeToHold = null;
	/** Where the events to be handled should refer to. */
	private INode node =  null;
	/** How the flight should be handled. */
	private IEvent.Cmd message = null;
	/** What criterion to use to order the flights selected to be held. */
	private IFlight.Param criterion = null;
	/** Hold all flights with event times beginning at this time */
	private int time = -1;
	/** Hold all flights till this time. */
	private int timeHold = -1;
	
	/**
	 * Constructor.
	 * @param typeToHold Type of event to be monitored.
	 * @param where Node which event refers to.
	 * @param message What should be done. Often the event class will do 
	 *   mulitple tasks, such as handling both arrivals and departures.  This
	 *   variable specifies which one.
	 * @param criterion What criterion to use to order the flights that should
	 *  be held.  For example, if it is the departure runway, then want to order
	 *  the flights on the runway by their computed departure time, CALC_DEP_TIME.
	 * @param timeStart Hold all flights with event times on or after this time.
	 * @param timeHold Hold all flights till this time.
	 */
	public HoldEvent(IEvent.Type typeToHold, INode where, IEvent.Cmd message, 
		IFlight.Param criterion, int timeStart, int timeHold){
		// First check if message is one of those allowed by HoldEvent.
		if(!(message == IEvent.Cmd.HOLD_ARR || message == IEvent.Cmd.HOLD_DEP)){
			final String method = className;
			throw new IllegalArgumentException(method + ": message is not " +
				"appropriate for a " + className);
		}
		// Don't allow holding of HoldEvents themselves.
		if(typeToHold == IEvent.Type.HOLD){
			final String method = className;
			throw new IllegalArgumentException(method + ": Not able to hold " +
			" HoldEvent's themselves. I mean really: what is the point?");
		}
		this.typeToHold = typeToHold;
		this.node      = where;
		this.message   = message;	
		this.criterion = criterion;
		this.time      = timeStart;
		this.timeHold  = timeHold;
	}
	/**
	 * Executes the event.  Holds flights in the given queue.
	 * @param queue Queue from which event originated.  Will be used
	 *   to put any events created from this one.
	 */
    public void processEvent(IQueue<IEvent> queue){
    	// List of events that need to delay times.
    	List<IEvent> list = new ArrayList<IEvent>();
    	// List of events to be put back in the queue.
    	List<IEvent> putbackList = new ArrayList<IEvent>();
    	
    	// Figure out what type of node event message we are looking for.
    	IEvent.Cmd nodeMsg = null;
    	if(message == IEvent.Cmd.HOLD_DEP){
    		nodeMsg = IEvent.Cmd.DEP;
    	} else {
    		nodeMsg = IEvent.Cmd.ARR;
    	}
    	
		// Get all events that satisfy criteria for whatever event we are
    	// holding for, e.g., a {@link RunwayEvent}.
		IEvent eve = null;
		boolean success = false;
		int extraTime=0, count=0;
		boolean firstPass = true;
		while(count > 0 || firstPass){
			// Need to add to hold list iteratively as holding back flights
			// could cause one to push back other events even though they
			// in principle occur after the hold time.
			
			if(firstPass == true)firstPass = false;
			success = false;
			count = 0;

			while(!success && (eve = queue.poll()) != null){
				// Check if the event time is past the hold time plus a buffer.
				// Include flights that are up to this extra time after the 
				// hold time since we need to have space for the flights we are
				// pushing back, so may need to move later flights back to 
				// create the space.
				if(eve.getTime() > timeHold + extraTime){
					// Then all possible events have been found for this hold time.
					// Add this event back into the original queue rather than
					// the putbackList.  This is done in case the count variable
					// is not zero, so will be moving through the queue again.  If
					// are, need this last event back in the original queue.  It
					// might be the event that is held once extraTime is increased.
					queue.add(eve);
					success = true;
				} else {
					// Time is after the desired timeStart and event is of type
					// RunwayEvent?
					if(eve.getTime() >= time && eve.getType() == typeToHold){
	    				// Get all events associated with the proper runway and
	    				// with the right node message then.
	    				if(eve.getNode() == node && eve.getMessage().equals(nodeMsg)){
	    					list.add(eve);
	    					count++;
	    				}else putbackList.add(eve);
					}else putbackList.add(eve);
				}
			}
			extraTime += count;
    	}
		// First, put back events in the queue that aren't delayed.
		for(IEvent e : putbackList){
			queue.add(e);
		}
		putbackList.clear();
		
		// Sort the list to hold by the ordering criterion.
		// Bubble sort.
		boolean swapped = true;
		while(swapped && list.size() > 1){
			swapped = false;
			for(int i=0;i<list.size()-1;i++){
				int t1 = list.get(i).getFlight().get(criterion);
				int t2 = list.get(i+1).getFlight().get(criterion);
				if(t1 > t2){
					IEvent h = list.get(i);
					list.set(i,list.get(i+1));
					list.set(i+1,h);
					swapped = true;
				}
			}
		}
		
		// Now for the list of flights with times between the
		// desired times, delay their times.
		int time = timeHold;
		for(IEvent ie : list){
			ie.setTime(time);
			queue.add(ie);
			time++;// add one second each time as that is the min time
				   // to order them.	
		}
		list.clear();// not necessary, but whatever.
    }
    
    /**
     * @return The time of the event.  Units: seconds.
     */
    public int getTime(){return time;}
    /**
     * @param time The time of the event.  Units: seconds.
     */
    public void setTime(int time){this.time = time;}
    /**
     * @return Event node.
     */
    public INode getNode(){return node;}
    /**
     * @return Event message.
     */
    public IEvent.Cmd getMessage(){return message;}
    /**
     * @return event type.
     */
    public IEvent.Type getType(){return IEvent.Type.HOLD;}
    /**
     * @return type of event to hold.
     */
    public IEvent.Type getTypeToHold(){return typeToHold;}
    /**
     * @return Flight associated with event or <code>null</code> if there is 
     *   none.  Here there is no flight.
     */
    public IFlight getFlight(){return null;}
  
    /**
     * Compares to events to see which occurs sooner.  Is used to order events
     * in a priority queue.
     * @param e Another event.
     * @return -1 if this event has a smaller time than e ; 1 if not. If they
     *   have the same time, then returns zero.
     */
    public int compareTo(IEvent e){
    	if(time < e.getTime())return -1;
    	else if(time > e.getTime())return 1;
    	return 0;
    }
    
    /**
     * Two events are equal if they have the same time.  Uses
     * the {@link #compareTo(IEvent)} method for this.
     * @param e Another event.
     * @return <code>true</code> if equal; <code>false</code> if not.
     */
    public boolean equals(IEvent e){
    	if(compareTo(e)==0)return true;
    	return false;
    }
    
    //-----------------------------------------------------------------------
    // Getters for testing.
    //-----------------------------------------------------------------------
    /**
     * @return time to hold events to. Units: seconds.
     */
    public int getTimeHold(){return timeHold;}
    /**
     * @return Criterion to order held flights.
     */
    public IFlight.Param getCriterion(){return criterion;}
    
}
