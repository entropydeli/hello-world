package org.cna.donley.nassim2_4;

import java.util.Queue;

/**
 * A class that implements the {@link IEvent} interface for a runway.
 * It is assumed here that all runways have at least one departure and
 * arrival fix associated with them.  Thus, on departure, a runway always
 * sends the flight to a fix and on arrival the runway gets the flight
 * from an arrival fix.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: RunwayEvent.java 1 2009-08-31 00:00:00EST $
 */

public class RunwayEvent implements IEvent
{
	/** The class name. */
	private final String className = "RunwayEvent";
	
	/** The number of seconds in a quarter hour.  All times in this
     *  simulation are in seconds, so this value will be used to determine
     *  when to compute the acceptance rates. Units: seconds.
     */
    private final int qtrhourInSecs = 15*60;
	
	/** What object should handle this event. */
	private Runway  node =  null;
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
	public RunwayEvent(Runway where, IFlight who, IEvent.Cmd message, 
		int time){
		// First check if message is one of those allowed by TaxiwayEvent.
		if(!(message == IEvent.Cmd.ARR || message == IEvent.Cmd.DEP)){
			final String method = className;
			throw new IllegalArgumentException(method + ": message is not " +
				"appropriate for a Runway.");
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
	 * the flight till that time.  One may also need to hold other flights
	 * in the queue.  Call a {@link HoldEvent} to do that.
	 * @param mainQueue Queue from which event originated.  Will be used
	 *   to put any events created from this one.
	 */
    public void processEvent(IQueue<IEvent> mainQueue){
    	
    	if(message == IEvent.Cmd.DEP){
    		// Departure event.
    		
    		// Add flight to departure queue if it is non-null.  A null
    		// flight indicates that this is a hold runway event.
    		IQueue<IFlight> qq = node.getQueue(Nas.Ad.DEP);
    		if(flight != null){
	    		qq.add(flight);
    		}
    		
    		// A flight must be in the queue for a RunwayEvent to be executed.
    		// In other words, a runway hold event should only be executed if
    		// a runway queue has anything in it.
    			
    		IFlight fTop =  null;
    		int nextOffTime = node.getNextTime(Nas.Ad.DEP);
    		if(time >= nextOffTime){
    			// Slot is available. Set queue hold status to false.
    			node.setQueueHoldStatus(Nas.Ad.DEP,false);
    			
    			// Then can get top of queue. Update flight and runway objects.
    			fTop = qq.poll();
    			int offTime = time;
    			fTop.set(IFlight.Param.ACT_OFF_TIME, offTime);
    			node.setLastTime(Nas.Ad.DEP, offTime);
    			// Compute next off time for runway.
    			node.computeNextTime(Nas.Ad.DEP,offTime);
    			
    			// Create next event for this flight.  Compute arrival time for
    			// the next node.  The next node is always a fix.
    			INode n = fTop.getRoute().getNextNode();
    			if(n instanceof Fix){
    				// Assumes that delays at a fix only affect other planes
    				// going to the same fix.  There is no delay between the 
    				// runway and fix queues, so the event time is the present 
    				// act_off_time. This time will be overridden if there are 
    				// fix delays.
    				Fix ff = (Fix)n;
	        		IEvent newEvent = 
	        			new FixEvent(ff,fTop,IEvent.Cmd.DEP,offTime);
	        		mainQueue.add(newEvent);
    			}
	        	else if(n instanceof DummyNode){
	        		// For testing.
    				DummyNode dn = (DummyNode)n;
	        		IEvent newEvent = 
	        			new DummyEvent(dn,fTop,IEvent.Cmd.DEP,offTime);
	        		mainQueue.add(newEvent);
        		}else {
        			final String method = className + ".processEvent()";
        			throw new IllegalArgumentException(method + 
        				" next node for flight, " + fTop + ", is not a Fix" +
        				" or DummyNode.");
        		}	
    		} 

			// If the queue still has elements in it and a "hold" runway event
    		// hasn't been generated for this runway, then do it.  The hold
    		// event will be executed at the next allowed runway off time.
			if(qq.size() > 0 && node.getQueueHoldStatus(Nas.Ad.DEP)==false){
				node.setQueueHoldStatus(Nas.Ad.DEP,true);
				nextOffTime = node.getNextTime(Nas.Ad.DEP);
    			IEvent runwayEvent = 
    				new RunwayEvent(node,null,IEvent.Cmd.DEP,nextOffTime);
    			mainQueue.add(runwayEvent);
			}
    		
    	} else if(message == IEvent.Cmd.ARR){
    		// Arrival Event.
    		
    		// Add flight to arrival queue if it is non-null.  A null
    		// flight indicates that this is a hold runway event.
    		IQueue<IFlight> qq = node.getQueue(Nas.Ad.ARR);
    		if(flight != null){
	    		qq.add(flight);
    		}
    		
    		IFlight fTop = null;
    		int nextOnTime = node.getNextTime(Nas.Ad.ARR);
    		if(time >= nextOnTime){
    			// Slot is available. Set queue hold status to false.
    			node.setQueueHoldStatus(Nas.Ad.ARR,false);
    			
    			// Then can get top of queue. Update flight and runway objects
    			// and act_on_time.
    			fTop = qq.poll();
    			int onTime = time;
    			fTop.set(IFlight.Param.ACT_ON_TIME, onTime);
    			node.setLastTime(Nas.Ad.ARR, onTime);
    			// Compute next on time for runway.
    			node.computeNextTime(Nas.Ad.ARR,onTime);
    			
    			// Create next event for this flight.  This is a taxiway event.
        		// Find Taxiway neighbor of this node.
    			INode n = fTop.getRoute().getNextNode();
        		if(n instanceof Taxiway){
            		Taxiway tn = (Taxiway)n;
	        		IEvent newEvent = 
	        			new TaxiwayEvent(tn,fTop,IEvent.Cmd.ARR,onTime);
	        		mainQueue.add(newEvent);
        		}
        		else if(n instanceof DummyNode){
	        		// For testing.
    				DummyNode dn = (DummyNode)n;
	        		IEvent newEvent = 
	        			new DummyEvent(dn,fTop,IEvent.Cmd.ARR,onTime);
	        		mainQueue.add(newEvent);
        		}else{
        			final String method = className + ".processEvent()";
        			throw new IllegalArgumentException(method + 
        				" next node for flight, " + fTop + ", is not a " +
        				"Taxiway or DummyNode");
        		}
    			
    		} 
    		
    		// If the queue still has elements in it and a "hold" runway event
    		// hasn't been generated for this runway, then do it.  The hold
    		// event will be executed at the next allowed runway off time.
			if(qq.size() > 0 && node.getQueueHoldStatus(Nas.Ad.ARR)==false){
				node.setQueueHoldStatus(Nas.Ad.ARR,true);
				nextOnTime = node.getNextTime(Nas.Ad.ARR);
    			IEvent runwayEvent = 
    				new RunwayEvent(node,null,IEvent.Cmd.ARR,nextOnTime);
    			mainQueue.add(runwayEvent);
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
    public IEvent.Type getType(){return IEvent.Type.RUNWAY;}
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
