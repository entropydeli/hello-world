package org.cna.donley.nassim2_4;

import java.util.Queue;

/**
 * A class that implements the {@link IEvent} interface for a terminal.
 * Is more than weakly coupled to the {@link Terminal} class.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: TerminalEvent.java 1 2009-08-31 00:00:00EST $
 */

public class TerminalEvent implements IEvent
{
	/** The class name. */
	private final String className = "TerminalEvent";
	
	/** What object should handle this event. */
	private Terminal  node =  null;
	/** What flight should be handled by this event. */
	private IFlight flight = null;
	/** How the flight should be handled. */
	private IEvent.Cmd message = null;
	/** time of event. */
	private int time = -1;
	
	/**
	 * Constructor.
	 * @param where What node should handle event. 
	 * @param who What flight is to be handled.
	 * @param message What should be done.
	 * @param time Time of event.
	 */
	public TerminalEvent(Terminal where, IFlight who, IEvent.Cmd message, 
		int time){
		// First check if message is one of those allowed by TerminalEvent.
		if(!(message == IEvent.Cmd.ARR || message == IEvent.Cmd.DEP)){
			final String method = className;
			throw new IllegalArgumentException(method + ": message is not " +
				"appropriate for a Terminal.");
		}
		this.node = where;
		this.flight = who;
		this.message = message;	
		this.time = time;
	}
	/**
	 * Executes the event.
	 * @param queue Queue from which event originated.  Will be used
	 *   to put any events created from this one.
	 */
    public void processEvent(IQueue<IEvent> queue){
    	// Constructor forces the message to be either DEP or ARR.
    	if(message == IEvent.Cmd.DEP){
    		// Just let it go.  Note that if this the first leg
    		// of the flight, the pushback time should be included
    		// in this event time.
    		flight.set(IFlight.Param.ACT_OUT_TIME,time);

    		// Create a taxiway event and add it to queue.
    		Taxiway tn = null;
    		INode n = flight.getRoute().getNextNode();
    		if(n instanceof Taxiway){
    			tn = (Taxiway)n;
    		}else {
    			final String method = className + ".processEvent()";
    			throw new IllegalArgumentException(method + 
    				" next node for flight, " + flight + ", is not a Taxiway.");
    		}
    		IEvent newEvent = 
    			new TaxiwayEvent(tn,flight,IEvent.Cmd.DEP,time);
    		queue.add(newEvent);
    	} else if(message == IEvent.Cmd.ARR){
    		// park the flight.  The event time should be the calced in time.
    		flight.set(IFlight.Param.ACT_IN_TIME,time);
    		
    		// release next leg if there is one.
    		IFlight farr2 = null;
    		if((farr2 = flight.getNextLeg()) != null){
    			// Get soonest out time for this new flight.
    			int minOutTime = time + node.computeTurnTime(flight);
    			farr2.set(IFlight.Param.MIN_OUT_TIME, minOutTime);
    			
    			// Get departure time if neglect turnaround.
    			int schNPushTime = farr2.get(IFlight.Param.SCHED_OUT_TIME) +
    				node.computePushbackTime(farr2);
    			
    			// Actual out time will be Max(min_out_time, sched + push time).
    			int outTime = 0;
    			if(minOutTime > schNPushTime){
    				outTime = minOutTime;
    				//System.out.println("minOutTime: " + minOutTime + " schNPushtime: " + schNPushTime);
    			}
    			else outTime = schNPushTime;
    			
    			// create event for new departing flight.
    			INode n = farr2.getRoute().getNextNode();
    			Terminal tt = null;
    			if(n instanceof Terminal){
        			tt = (Terminal)n;
        		}else {
        			final String method = className + ".processEvent()";
        			throw new IllegalArgumentException(method + 
        				" next node for flight, " + flight + ", is not a Terminal.");
        		}
    			IEvent newEvent = 
    				new TerminalEvent(tt,farr2,IEvent.Cmd.DEP,outTime);
    			queue.add(newEvent);
    		}
    	}else {
    		// This exception should never be reached as long as the
    		// Constructor does the same check.  Keep it in though just
    		// in case.
    		final String method = className + ".processEvent()";
    		throw new IllegalArgumentException(method + ": Bad command.");
    	}
    }
    
    /**
     * @return the time of the event. 
     */
    public int getTime(){return time;}
    /**
     * @param time of the event. 
     */
    public void setTime(int time){this.time = time;}
    /**
     * @return Event node.
     */
    public INode getNode(){return node;}
    /**
     * @return Event flight.
     */
    public IFlight getFlight(){return flight;}
    /**
     * @return Event message.
     */
    public IEvent.Cmd getMessage(){return message;}
    /**
     * @return Type of event.
     */
    public IEvent.Type getType(){return IEvent.Type.TERMINAL;}
  
    /**
     * Compares to events to see which occurs sooner.  Is used to order events
     * in a priority queue.
     * @param e Another event.
     * @return -1 if this event has a smaller time than e ; 1 if not. If they
     *   have the same time, then looks at the itinerary numbers.  If the
     *   flight for this event has a smaller itin number it return -1; if
     *   it is larger, then -1, if they have the same (impossible since only
     *   one leg can fly at a time) it returns 0.
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
    
 
}
