package org.cna.donley.nassim2_4;

/**
 * A class that implements the {@link IEvent} interface for a taxiway.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: TaxiwayEvent.java 1 2009-08-31 00:00:00EST $
 */

public class TaxiwayEvent implements IEvent
{
	/** The class name. */
	private final String className = "TaxiwayEvent";
	
	/** What object should handle this event. */
	private Taxiway  node =  null;
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
	public TaxiwayEvent(Taxiway where, IFlight who, IEvent.Cmd message, 
		int time){
		// First check if message is one of those allowed by TaxiwayEvent.
		if(!(message == IEvent.Cmd.ARR || message == IEvent.Cmd.DEP)){
			final String method = className;
			throw new IllegalArgumentException(method + ": message is not " +
				"appropriate for a Taxiway.");
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
    	if(message == IEvent.Cmd.DEP){
    		// Add taxi-out time.
    		int calcOffTime = time;
    		int taxiOutTime = node.computeTaxiOutTime(flight);
    		calcOffTime += taxiOutTime;
    		flight.set(IFlight.Param.CALC_OFF_TIME,calcOffTime);
    		
    		// Create a runway event and add it to the queue.
    		// Get runway node.
    		INode n = flight.getRoute().getNextNode();
    		if(n instanceof Runway){
    			Runway rn = (Runway)n;
	    		IEvent newEvent = 
	    			new RunwayEvent(rn,flight,IEvent.Cmd.DEP,calcOffTime);
	    		queue.add(newEvent);
    		}else {
    			final String method = className + ".processEvent()";
    			throw new IllegalArgumentException(method + 
    				" next node for flight, " + flight + ", is not a Runway.");
    		}	
    	} else if(message == IEvent.Cmd.ARR){
    		// Add nominal taxi-in time.
    		int calcInTime = time;
    		int taxiInTime = node.computeTaxiInTime(flight);
    		calcInTime += taxiInTime;
    		flight.set(IFlight.Param.CALC_IN_TIME,calcInTime);
    		
    		// Create a terminal event and add it to the queue.
    		// Get terminal node.
    		INode n = flight.getRoute().getNextNode();
    		if(n instanceof Terminal){
    			Terminal tn = (Terminal)n;
	    		IEvent newEvent = 
	    			new TerminalEvent(tn,flight,IEvent.Cmd.ARR,calcInTime);
	    		queue.add(newEvent);
    		}else {
    			final String method = className + ".processEvent()";
    			throw new IllegalArgumentException(method + 
    				" next node for flight, " + flight + ", is not a Terminal.");
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
     * @param time of the event. 
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
    public IEvent.Type getType(){return IEvent.Type.TAXIWAY;}
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
