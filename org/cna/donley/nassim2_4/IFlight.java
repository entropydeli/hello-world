package org.cna.donley.nassim2_4;

import java.util.List;

/**
 * An interface for the class that stores data for a single flight.
 * A bunch of getters and setters.  Since will have only one 
 * implementation of this interface as far as I can see, allow the
 * natural ordering of the interface through the extension of the
 * {@link Comparable} interface.  The {@link #equals(IFlight)} method
 * then should be consistent with the {@link #compareTo(IFlight)} one.
 * <p>
 * The constructor of an implementing class creates the object pointer
 * and sets the values of important parameters, such as 
 * {@link IFlight.Param}.ID and {@link IFlight.Param}.LEG_NUM.
 * Once all the flights have been created, then the 
 * {@link #setPrevLeg(IFlight)} and {@link #setNextLeg(IFlight)} methods
 * are run to store references to the neighboring legs, if any, for this
 * flight.  Last, once all NAS nodes have been created, the flight route
 * is created and stored with the {@link #setRoute(IRoute)} method.   
 * <p>
 * All times are in units of the sim timestep unless notes otherwise.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: IFlight.java 1 2009-06-01 00:00:00EST $
 */

public interface IFlight extends Comparable<IFlight>
{ 
	/**
     * Enumeration of flight parameters or variables.  These are all 
     * integers.  All times will initially be w/r to midnight of the first
     * day, but later may be changed to relative to the sim start time.
     * <p>
     * The flight turnaround time is determined by the airport, 
     * airline and type of aircraft.
     */
    public static enum Param{
    	/** Itinerary number.  This is a NASPAC generated number. */
    	ITIN_NUM,
    	/** Leg number.  If there is only one leg, this number will be
    	 * one naturally. */
    	LEG_NUM, 
    	/** Schedule ID.  This is a unique ID to the schedule.  With it, one 
    	 * should be able to get other stuff like the flight index by
    	 * combining the sim results with the original flight schedule. */
    	SCHED_ID, 
    	/** Departure Airport.  Mapping from this number to the actual
    	 * name is given in the main sim class. */
    	DEP_APRT,
    	/** Arrival airport.  Mapping from this number to the actual name
    	 * is given in the main sim class. */
    	ARR_APRT,
    	/** Airline, air carrier. Mapping from this number to the actual
    	 * name is given in the main sim class. */
    	CARRIER,
    	/** Aircraft equipment type.  This will usually be the ETMS type,
    	 * rather than the BADA. */
    	EQUIP_TYPE,
    	/** Scheduled departure time.  Units: sim time steps. */
    	SCHED_OUT_TIME,
    	/** Minimum departure time. Same as sched_dep_time, unless a
    	 * previous leg is late and then it's the minimum of the sched_out_time
    	 * and the sum of the previous leg's in time and its turn time. */
    	MIN_OUT_TIME,
    	/** Actual departure time. */
    	ACT_OUT_TIME,
    	/** Calculated runway off time */
    	CALC_OFF_TIME,
    	/** Actual runway off time */
    	ACT_OFF_TIME,
    	/** Actual (calculated by the TrajectoryModeler) airborne time. */
    	ACT_AIR_TIME,
    	/** Calculated runway on time */
    	CALC_ON_TIME,
    	/** Actual runway on time */
    	ACT_ON_TIME,
    	/** Scheduled in time */
    	SCHED_IN_TIME,
    	/** Calculated gate-in time */
    	CALC_IN_TIME,
    	/**  Actual gate-in time. */
    	ACT_IN_TIME,
    	/** Computed turn-around time. This will depend upon the  
    	 * airport, the airline and the type of plane. */
    	TURN_TIME,
    	/** Computed pushback time.  This will depend upon the airport,
    	 * airline and equipment type of plane. */
    	PUSHBACK_TIME,
    	/** Computed taxi-out time.  This will depend upon the airport,
    	 * airline and equipment type of plane. */
    	TAXI_OUT_TIME,
    	/** Computed taxi-in time.  This will depend upon the airport,
    	 * airline and equipment type of plane. */
    	TAXI_IN_TIME,
    	/** Departure fix. An index that maps to a name using a method in
    	 * the {@link Nas} object. */
    	DEP_FIX,
    	/** Departure fix delay.  Will be the time the plane spends on the
    	 * ground waiting for the departure fix to be available, after
    	 * the runway has become available for takeoff. */
    	DEP_FIX_DELAY,
    	/** Arrival fix. An index that maps to a name using a method in
    	 * the {@link Nas} object. */
    	ARR_FIX,
    	/** Arrival fix delay. */
    	ARR_FIX_DELAY};
    	
    /**
     * Returns the flight parameter as specified by p.  
     * @param p Type of flight parameter.
     * @return parameter as an int, or should be.
     */
    public int get(IFlight.Param p);
    	
	/**
	 * Sets the parameter value.  Is an integer.  This is need to set
	 * values such as times during the running of the sim.
	 * @param p Type of flight parameter
	 * @param value Value of parameter.
	 */
    public void set(IFlight.Param p, int value);
    
    /**
     * Sets the flight route, which is the list of nodes the flight
     * traverses.  Includes taxiways and terminals.
     * @param route  Route of flight as an {@link IRoute} object.
     */
    public void setRoute(IRoute route);

    /**
     * Set the neighboring previous leg of the flight if the flight belongs to a
     * multi-legged itinerary.
     * @param prevLeg The previous leg.  If none, set to <code>null</code>.
     */
    public void setPrevLeg(IFlight prevLeg);
    /**
     * Set the neighboring next leg of the flight if the flight belongs to a
     * multi-legged itinerary.
     * @param nextLeg The next leg of the flight.  If none, set to 
     *   <code>null</code>.
     */
    public void setNextLeg(IFlight nextLeg);
    
    /**
     * Get the previous leg in this flight if any.
     * @return previous leg in flight or <code>null</code> if no leg.
     */
    public IFlight getPrevLeg();
    /**
     * Get the next leg in the flight if any.
     * @return next leg in flight or <code>null</code> if no leg.
     */
    public IFlight getNextLeg();
    
    /**
     * Get the flight route.
     * @return flight route.
     */
    public IRoute getRoute();
    
    /**
     * Bogus compare so that if two desired departure times are the same
     * for two flights, one flight is chosen over the other.
     * @param f
     * @return 1 if this flight has a smaller Id number; -1 if not. If they
     *   have the same id, then checks the leg number.  Returns 0 if they
     *   are determined to be equal.
     */
    public int compareTo(IFlight f);
    
    /**
     * Two flights are equal if they have the same id and leg numbers.  Uses
     * the {@link #compareTo(IFlight)} method for this.
     * @param f
     * @return <code>true</code> if equal; <code>false</code> if not.
     */
    public boolean equals(IFlight f);
    
}
