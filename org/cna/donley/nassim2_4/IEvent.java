package org.cna.donley.nassim2_4;

/**
 * An interface for a simulation event class.  An event has a time it is
 * supposed to be executed, a node for which the event is to operate on,
 * and a message which is what the event is supposed to do.  The node
 * could be <code>null</code>.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: IEvent.java 1 2009-08-28 00:00:00EST $
 */

public interface IEvent extends Comparable<IEvent>
{ 	
	/**
	 * An enum of possible event messages. Most are common to all events,
	 * so just put it here.
	 */
	public enum Cmd {
		/** A departing flight. */
		DEP,
		/** An arriving flight. */
		ARR,
		/** Hold departing flights.  Used by {@link HoldEvent}. */
		HOLD_DEP,
		/** Hold arriving flights.  Used by {@link HoldEvent}. */
		HOLD_ARR,
		/** Bogus message. */
		BOGUS;
	}
	/**
	 * An enum of possible types of events.  
	 */
	public enum Type {
		/** Event associated with a termnal */
		TERMINAL,
		/** Event associated with a taxiway */
		TAXIWAY,
		/** Event associated with a runway */
		RUNWAY,
		/** Event associated with an arrival or departure fix. */
		FIX,
		/** A hold event. */
		HOLD;
	}
	
	/**
	 * Executes the event.
	 * @param queue The queue from which this event came.
	 */
    public void processEvent(IQueue<IEvent> queue);
    
    /**
     * @return the time of the event. 
     */
    public int getTime();
    /**
     * Set the time of the event.
     * @param time of the event. 
     */
    public void setTime(int time);
    /**
     * @return Node event is supposed to operate on.
     */
    public INode getNode();
    /**
     * @return Event message.
     */
    public IEvent.Cmd getMessage();
    /**
     * @return Type of event.
     */
    public IEvent.Type getType();
    /**
     * @return Flight associated with event or <code>null</code> if there is 
     *   none.
     */
    public IFlight getFlight();
  
    /**
     * Compares to events to see which occurs sooner.  Is used to order events
     * in a priority queue.
     * @param e Another event.
     * @return -1 if this event has a smaller time than e ; 1 if not. If they
     *   have the same time, then returns zero.
     */
    public int compareTo(IEvent e);
    
    /**
     * Two events are equal if they have the same time.  Uses
     * the {@link #compareTo(IEvent)} method for this.
     * @param e Another event.
     * @return <code>true</code> if equal; <code>false</code> if not.
     */
    public boolean equals(IEvent e);
    
}
