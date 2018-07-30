package org.cna.donley.nassim2_4;

import java.util.Queue;

/**
 * A class that implements the {@link IEvent} interface for a fix.  It is 
 * assumed that all runways have at least one arrival and departure fix
 * associated with them.  Thus, in the absence of sectors, a route is
 * {term,taxi,run,fix,fix,run,taxi,term}.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: FixEvent.java 1 2009-11-04 00:00:00EST $

 */

public class FixEvent implements IEvent
{
	/** The class name. */
	private final String className = "FixEvent";
	
	/** What object should handle this event. */
	private Fix  node =  null;
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
	 * @param time Time of event.  Should be the actual out time or
	 *   actual on time depending on if the flight is a departure 
	 *   or arrival.
	 */
	public FixEvent(Fix where, IFlight who, IEvent.Cmd message, 
		int time){
		// First check if message is one of those allowed by TaxiwayEvent.
		if(!(message == IEvent.Cmd.ARR || message == IEvent.Cmd.DEP)){
			final String method = className;
			throw new IllegalArgumentException(method + ": message is not " +
				"appropriate for a Fix.");
		}
		this.node = where;
		this.flight = who;
		this.message = message;	
		this.time = time;
	}
	/**
	 * Executes the event.  Assumes the next off or on time has
	 * been computed in the last call to this or was initialized.
	 * Thus, need only check if the event time is equal to or past
	 * the next time.  If so, just go (and then compute the next time
	 * a flight can arr/dep).  If not, then need to hold
	 * the flight till that time.
	 * @param mainQueue From which event originated.  Will be used
	 *   to put any events created from this object.
	 */
    public void processEvent(IQueue<IEvent> mainQueue){
    	
    	if(message == IEvent.Cmd.DEP){
    		// Departure event.
    		
    		// Add flight to departure queue if it is non-null.  A null
    		// flight indicates that this is a hold runway event.
    		IQueue<IFlight> qq = node.getQueue();
    		if(flight != null){
	    		qq.add(flight);
    		}
    		
    		// A flight must be in the queue for a FixEvent to be executed.
    		// In other words, a fix hold event should only be executed if
    		// a fix queue has anything in it.
    			
    		int nextTime = node.getNextTime();
    		if(time >= nextTime){
    			
    			// Slot is available. Set queue hold status to false.
    			node.setQueueHoldStatus(false);
    			
    			// Then can get top of queue. Update flight and fix objects.
    			IFlight fTop = qq.poll();
    			// Set fix delay and override runway off time.
				int fixDelay = time - fTop.get(IFlight.Param.ACT_OFF_TIME);
				fTop.set(IFlight.Param.DEP_FIX_DELAY, fixDelay);
				fTop.set(IFlight.Param.ACT_OFF_TIME, time);
				
				// Compute next off time for fix.
				node.computeNextTime(time);
    			
    			// Create next event for this flight.  Compute arrival time for
    			// the next node.  The next node is always a fix.
    			INode n = fTop.getRoute().getNextNode();
    			if(n instanceof Fix){	
					Fix fix = (Fix)n;
	        		//int calcArrTime = flight.get(IFlight.Param.ACT_OFF_TIME) +
	        		//	flight.get(IFlight.Param.ACT_AIR_TIME);
					int calcArrTime = 
						time + fTop.get(IFlight.Param.ACT_AIR_TIME);
	        		fTop.set(IFlight.Param.CALC_ON_TIME,calcArrTime);
	        		IEvent newEvent = 
	        			new FixEvent(fix,fTop,IEvent.Cmd.ARR,calcArrTime);
	        		mainQueue.add(newEvent);
    			}
	        	else if(n instanceof DummyNode){
	        		// For testing.
    				DummyNode dn = (DummyNode)n;
    				//int calcArrTime = flight.get(IFlight.Param.ACT_OFF_TIME) +
	        		//	flight.get(IFlight.Param.ACT_AIR_TIME);
					int calcArrTime = 
						time + fTop.get(IFlight.Param.ACT_AIR_TIME);
	        		fTop.set(IFlight.Param.CALC_ON_TIME,calcArrTime);
	        		IEvent newEvent = 
	        			new DummyEvent(dn,fTop,IEvent.Cmd.ARR,calcArrTime);
	        		mainQueue.add(newEvent);
        		}else {
        			final String method = className + ".processEvent()";
        			throw new IllegalArgumentException(method + 
        				" next node for flight, " + flight + ", is not a Fix.");
        		}	
    		} 

			// If the queue still has elements in it and a "hold" fix event
    		// hasn't been generated for this fix, then do it.  The hold
    		// event will be executed at the next allowed fix throughput time.
			if(qq.size() > 0 && node.getQueueHoldStatus()==false){
				node.setQueueHoldStatus(true);
				nextTime = node.getNextTime();
    			IEvent fixEvent = 
    				new FixEvent(node,null,IEvent.Cmd.DEP,nextTime);
    			mainQueue.add(fixEvent);
			}
    		
    	} else if(message == IEvent.Cmd.ARR){
    		// Arrival Event.
    		
    		// Add flight to arrival queue if it is non-null.  A null
    		// flight indicates that this is a hold runway event.
    		IQueue<IFlight> qq = node.getQueue();
    		if(flight != null){
	    		qq.add(flight);
    		}
    		
    		int nextTime = node.getNextTime();
    		if(time >= nextTime){
    			// Slot is available. Set queue hold status to false.
    			node.setQueueHoldStatus(false);
    			
    			// Then can get top of queue.
    			IFlight fTop = qq.poll();
    			// Then can just land, contingent on the arrival runway being 
    			// open that is.  Compute arr fix delay.
    			int fixDelay = time - fTop.get(IFlight.Param.CALC_ON_TIME);
    			fTop.set(IFlight.Param.ARR_FIX_DELAY,fixDelay);
    			// Also set act_on_time even though it will be overridden in
    			// RunwayEvent.
    			fTop.set(IFlight.Param.ACT_ON_TIME, time);
    			
    			// Compute next open time for arrival fix.
    			node.computeNextTime(time);
    			
    			// Create next event for this flight.  This is a runway event.
        		// Find Taxiway neighbor of this node.
    			INode n = fTop.getRoute().getNextNode();
        		if(n instanceof Runway){
        			Runway rn = (Runway)n;
	        		IEvent newEvent = 
	        			new RunwayEvent(rn,fTop,IEvent.Cmd.ARR,time);
	        		mainQueue.add(newEvent);
        		}
        		else if(n instanceof DummyNode){
	        		// For testing.
    				DummyNode dn = (DummyNode)n;
	        		IEvent newEvent = 
	        			new DummyEvent(dn,fTop,IEvent.Cmd.ARR,time);
	        		mainQueue.add(newEvent);
        		}else{
        			final String method = className + ".processEvent()";
        			throw new IllegalArgumentException(method + 
        				" next node for flight, " + fTop + ", is not a Runway.");
        		}
    			
    		} 
    		
    		// If the queue still has elements in it and a "hold" runway event
    		// hasn't been generated for this runway, then do it.  The hold
    		// event will be executed at the next allowed runway off time.
			if(qq.size() > 0 && node.getQueueHoldStatus()==false){
				node.setQueueHoldStatus(true);
				nextTime = node.getNextTime();
    			IEvent fixEvent = 
    				new FixEvent(node,null,IEvent.Cmd.ARR,nextTime);
    			mainQueue.add(fixEvent);
			}
    		
    	}else {
    		final String method = className + ".processEvent()";
    		throw new IllegalArgumentException(method + ": Bad command.");
    	}
    }
    
    /**
     * @return the time of the event. 
     */
    public int getTime(){return time;}
    /**
     * @param time of event.
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
     * @return Type of event.
     */
    public IEvent.Type getType(){return IEvent.Type.FIX;}
    /**
     * @return Event flight.
     */
    public IFlight getFlight(){return flight;}
  
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
