package org.cna.donley.nassim2_4;

/**
 * A class that implements the {@link IEvent} interface as a dummy.  Used for
 * testing.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: DummyEvent.java 1 2009-12-17 00:00:00EST $
 */

public class DummyEvent implements IEvent
{
	/** The class name. */
	private final String className = "DummyEvent";
	/** What object should handle this event. */
	private INode  node =  null;
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
	public DummyEvent(INode where, IFlight who, IEvent.Cmd message, 
		int time){
		this.node = where;
		this.flight = who;
		this.message = message;	
		this.time = time;
	}
	/**
	 * Executes the event.
	 */
    public void processEvent(IQueue<IEvent> mainQueue){};
    
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
    public IEvent.Type getType(){return null;}
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
