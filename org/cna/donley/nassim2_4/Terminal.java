package org.cna.donley.nassim2_4;


/**
 * An implementation of the interface {@link INode} for an airport 
 * terminal.  This and the {@link TerminalEvent} class are more than
 * weakly coupled.
 * <p>
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: Terminal.java 1 2009-08-31 00:00:00EST $
 */

public class Terminal implements INode
{
	/**
	 * Airport index to which this terminal belongs.
	 */
	private int airport = -1;

    /**
     * Neighboring node.
     */
    private INode taxiwayNode = null;
    
    /**
     * Array of turnaround times.  First element is equip type, second is
     * carrier.  The 3rd element is mean and standard deviation of the
     * distribution, given by {@link Nas.Distrib}. 
     * Units: seconds.
     */
    private int[][][] turnTimes;
    /**
     * Array of pushback times.  First element is carrier, second is
     * equip type.  The 3rd element is mean and standard deviation of the
     * distribution, given by {@link Nas.Distrib}. 
     * Units: seconds.
     */
    private int[][][] pushbackTimes;
    
    /**
     * Make default constructor private.
     */
    private Terminal(){};
    
    /**
     * Constructor.  Neighboring node is set in the 
     *  {@link #setNeighbor(INode)} method.  Departing flights are turned
     * into sim events separately using the
     * {@link #createEventsForDepartingFlights(IFlight[])} method.  This
     * constructor is used if want a weaker coupling between nodes and flights.
     * @param aprt Airport index of which this terminal belongs.
     * @param turnTimes  Array of turn times, first element is the equip type,
     *   second is carrier and third is distribution values according to
     *   {@link Nas.Distrib}.  If value is set in the {@link IFlight} objects,
     *   then can set this arry to <code>null</code>.
     * @param pushbackTimes  Array of pushback times, first element is the 
     *   carrier, the second is the equip type, and the third is distribution
     *   values according to {@link Nas.Distrib}.  If value is set in the
     *   {@link IFlight} objects, can set this array to <code>null<code>.
     */
    public Terminal(int aprt, int[][][] turnTimes, int[][][] pushbackTimes){
    	airport = aprt;
    	this.turnTimes = turnTimes;
    	this.pushbackTimes = pushbackTimes;
    }
    
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
     * Sets a neighbor of this node.  Used when creating the NAS network
     * of nodes and edges.
     * @param n The neighboring element.
     */
    public void setNeighbor(INode n){
    	// Only neighbor now is a taxiway.
    	if(n instanceof Taxiway){
    		taxiwayNode = n;
    	} else {
    		final String method = this.getClass().getName()+".setNeighbor()";
    		throw new IllegalArgumentException(method + ": node type: " +
    			n.getClass().getName() + " is not a neighbor of a terminal.");
    	}
    } 
    
    /**
     * Gets the node neighbors as an array.
     * @return array of node neighbors.
     */
    public INode[] getNeighbors(){
    	INode[] nn = new INode[1];
    	nn[0] = taxiwayNode;
    	return nn;
    }
    
    /**
     * Gets the airport index associated with this node.
     * @return airport index
     */
    public int getAirport(){return airport;}
    /**
     * Get array of turnaround times.  First element is equip type, second is
     * carrier.  The 3rd element is mean and standard deviation of the
     * distribution, given by {@link Nas.Distrib}. 
     * Units: seconds.
     * @return turn times array.
     */
    public int[][][] getTurnTimes(){return turnTimes;}
    /**
     * Get array of pushback times.  First element is carrier, second is
     * equip type.  The 3rd element is mean and standard deviation of the
     * distribution, given by {@link Nas.Distrib}. 
     * Units: seconds.
     * @return pushback times array.
     */
    public int[][][] getPushbackTimes(){return pushbackTimes;}
    
    /**
     * Creates events for all departing flights associated with this terminal
     * These events will later be loaded into an event queue. These flights 
     * should be only of the first leg of flights that depart from this 
     * terminal.  Info on other legs is contained in the first leg.  Needless
     * to say this method shouldn't be called until after the scheduled
     * out time for the flights have been set.
     * <p> 
     * This is not a part of the constructor because I want to keep the
     * event driven part of the simulation outside of the creation of the
     * flights and nodes.  It is normally called by the 
     * {@link ISimulation#initialize()} method.
     * Note that the event time is the sum of the sch_out_time and pushback_time.
     * @param flights Flights to load in queue.
     * @return Array of Terminal events of first legs of departing flights.
     *   If there are no flights to add, then the method returns 
     *   <code>null</code>.
     */
    public IEvent[] createEventsForDepartingFlights(IFlight[] flights){
    	// Do if some flights are present.
    	if(flights != null && flights[0] != null){
    		IEvent[] events = new IEvent[flights.length];
	    	for(int i=0;i<flights.length;i++){
	    		events[i] = createEventForFirstLegDepartingFlight(flights[i]);
	    	}
	    	return events;
    	} else return null;
    }
    /**
     * Creates an event for a departing flight associated with this terminal.
     * This flight should be only the first leg of flights that depart
     * from this terminal.  Info on other legs is contained in the first leg
     * and they are handled elsewhere. 
     * Needless to say this method shouldn't be called until after the 
     * scheduled out time for the flight has been set.
     * <p> 
     * This is not a part of the constructor because I want to keep the
     * event driven part of the simulation outside of the creation of the
     * flights and nodes.  It is normally called by the 
     * {@link ISimulation#initialize()} method.
     * Note that the event time is the sum of the sch_out_time and pushback_time.
     * @param flight Flight for which to create event.
     * @return Terminal event of first legs of departing flights.
     *   If there are no flights to add, then the method returns 
     *   <code>null</code>.
     */
    public IEvent createEventForFirstLegDepartingFlight(IFlight flight){
		int time = flight.get(IFlight.Param.SCHED_OUT_TIME) +
			computePushbackTime(flight);
		IEvent eve  = 
			new TerminalEvent(this,flight,IEvent.Cmd.DEP,time);
	    return eve;
    }
    
    /**
     * Computes the turn-around time for a flight in or out of this airport.
     * It will depend upon the airport, the airline and the type of
     * aircraft.  At present, just uses the turnaround time set during
     * the sim initialization, obtained from the flight object.
     * <p>
     * @param flight
     * @return turnaround time of aircraft. Units: seconds.
     */
    public int computeTurnTime(IFlight flight){
    	int turnTime = flight.get(IFlight.Param.TURN_TIME);
    	if(turnTime > 0) return turnTime;
    	else return 0;	
    }  
    /**
     * Computes the pushback relative time for a flight in or out of this airport.
     * It will depend upon the airport, the airline and the type of
     * aircraft.  At present, just uses the pushback time set during
     * the sim initialization, obtained from the flight object.  This probably
     * is the time, relative to the scheduled out time, that the first leg
     * of a flight departs on average.  Is usually negative.
     * <p>
     * @param flight
     * @return pushback relative time of aircraft. Units: seconds.
     */
    public int computePushbackTime(IFlight flight){
    	return flight.get(IFlight.Param.PUSHBACK_TIME);
    }  
}
