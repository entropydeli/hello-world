package org.cna.donley.nassim2_4;

import java.util.List;

/**
 * An implementation of the {@link IFlight} interface.  It stores data for a
 * single flight.  Has a bunch of getters and setters.
 * <p>
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: Flight.java 1 2009-06-01 00:00:00EST $
 */

public class Flight implements IFlight
{ 
	/**
	 * All params of flight except for neighboring legs.  What variables
	 * are in this array are given in {@link IFlight.Param}.
	 */
	private int[] params;
	
	/**
	 * Previous leg of flight.  Could be null.
	 */
	private IFlight prevLeg;
	/**
	 * Next leg of flight.  Could be null.
	 */
	private IFlight nextLeg;
	
	/**
	 * Flight route.
	 */
	private IRoute flightRoute;
	
	
	/** 
	 * Private default constructor.
	 */
	private Flight(){}
	
	/**
	 * Constructor.  At least the parameters for the flight ID and leg number
	 *   should be set here, but gives no warning if they aren't.  Throws an 
	 *   exception of the input params array is not the right length.
	 * @param params
	 */
	public Flight(int[] params){
		this.params = new int[IFlight.Param.values().length];
		if(params.length != this.params.length){
			final String method = this.getClass().getName();
			throw new IllegalArgumentException(method + ": input param " +
				"array has incorrect length.");
		}
		for(int i=0;i<IFlight.Param.values().length;i++){
			this.params[i] = params[i];
		}
		this.prevLeg = null;
		this.nextLeg = null;
		this.flightRoute = null;
	}
	
    /**
     * Returns the flight parameter as specified by p.  
     * @param p Type of flight parameter.
     * @return parameter as an int, or should be.
     */
    public int get(IFlight.Param p){
    	return params[p.ordinal()];
    }
    	
	/**
	 * Sets the parameter value.  Is an integer.
	 * @param p Type of flight parameter
	 * @param value Value of parameter.
	 */
    public void set(Param p, int value){
    	params[p.ordinal()] = value;
    }
    /**
     * Sets the flight route, which is the list of nodes the flight
     * traverses.  Includes taxiways and terminals.
     * @param route  Route of flight as an {@link IRoute} object.
     */
    public void setRoute(IRoute route){
    	flightRoute = route;
    }
    
    /**
     * Set the neighboring previous leg of the flight if the flight belongs to a
     * multi-legged itinerary.
     * @param prevLeg The previous leg.  If none, set to <code>null</code>.
     */
    public void setPrevLeg(IFlight prevLeg){
    	this.prevLeg = prevLeg;
    }
    /**
     * Set the neighboring next leg of the flight if the flight belongs to a
     * multi-legged itinerary.
     * @param nextLeg The next leg of the flight.  If none, set to 
     *   <code>null</code>.
     */
    public void setNextLeg(IFlight nextLeg){
    	this.nextLeg = nextLeg;
    }
    /**
     * Get the previous leg in this flight if any.
     * @return previous leg in flight or <code>null</code> if no leg.
     */
    public IFlight getPrevLeg(){
    	return prevLeg;
    }
    /**
     * Get the next leg in the flight if any.
     * @return next leg in flight or <code>null</code> if no leg.
     */
    public IFlight getNextLeg(){
    	return nextLeg;
    }  
    /**
     * Get the flight route.
     * @return flight route.
     */
    public IRoute getRoute(){
    	return flightRoute;
    }
    
    /**
     * Bogus compare so that if two desired departure times are the same
     * for two flights, one flight is chosen over the other.
     * @param f
     * @return -1 if this flight has a smaller intinerary number; 1 if a larger.
     *  If they have the same itin number, then checks the leg number.  
     *  Return 0 if they are determined to have the same itin and leg numbers.
     */
    public int compareTo(IFlight f){
    	if(params[IFlight.Param.ITIN_NUM.ordinal()] < 
    	    f.get(IFlight.Param.ITIN_NUM)) return -1;
    	else if(params[IFlight.Param.ITIN_NUM.ordinal()] >
    		f.get(IFlight.Param.ITIN_NUM)) return 1;
    	else {
    		// Have the same flight ID.  Check leg number.
        	if(params[IFlight.Param.LEG_NUM.ordinal()] < 
        		f.get(IFlight.Param.LEG_NUM))return -1;
        	else if(params[IFlight.Param.LEG_NUM.ordinal()] > 
        		f.get(IFlight.Param.LEG_NUM))return 1;
        	else return 0;
 	    }
    }
    /**
     * Two flights are equal if they have the same id and leg numbers.  Uses
     * the {@link #compareTo(IFlight)} method for this.
     * @param f
     * @return <code>true</code> if equal; <code>false</code> if not.
     */
    public boolean equals(IFlight f){
    	if(compareTo(f) == 0)return true;
    	else return false;
    }
    
}
