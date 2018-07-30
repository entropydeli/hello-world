package org.cna.donley.nassim2_4;


/**
 * An implementation of the interface {@link INode} for an airport 
 * taxiway.  It hold info on the taxiways.  There is assumed to be no
 * limit on the ability to handle flights, so there are no waiting times
 * for flights to enter the taxiway.  A wait time would occur if there must
 * be a minimum distance between planes, for example.
 * <p>
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: Taxiway.java 1 2009-08-31 00:00:00EST $
 */

public class Taxiway implements INode
{	
	/**
	 * Airport it belongs to.
	 */
	private int airport = 0;
      
    /**
     *  Neighbor terminal node.
     */
    private INode terminalNode = null;
    /**
     *  Neighbor runway node.
     */
    private INode runwayNode = null; 
    /**
     * Mean and standard deviation of the taxi-in times.  First element is the air 
     * carrier.  The second is the equipment type. The 3rd is 
     * either the mean [0] or std dev [1] of the distribution.  Units: seconds
     */
    private int[][][] taxiInTimes  = null;
    /**
     * Mean and standard deviation of the taxi-out times.  First element is the air 
     * carrier.  The second is the equipment type. The 3rd is 
     * either the mean [0] or std dev [1] of the distribution.  Units: seconds
     */
    private int[][][] taxiOutTimes  = null;
  
    /**
     * Private default constructor.
     */
    private Taxiway(){};
    
    /**
     * Constructor.  Initially the taxiway queues are empty.
     * @param aprt Airport this taxiway belongs to.  The neighboring nodes
     *   are set by the {@link #setNeighbor(INode n)}
     *   method.
     * @param taxiInTimes 3D array of taxi-in times for this 
     *   airport.  First element is air carrier and the second is the 
     *   equipment type.  The 3rd element is
     *   the mean [0] or std dev [1] of the distribution.
     *   This info can also be obtained from the {@link IFlight} object,
     *   so could be <code>null</code>.  Units: seconds. 
     * @param taxiOutTimes 3D array of taxi-out times for this 
     *   airport.  First element is air carrier and the second is the 
     *   equipment type.  The 3rd element is
     *   the mean [0] or std dev [1] of the distribution.
     *   This info can also be obtained from the {@link IFlight} object,
     *   so could be <code>null</code>.  Units: seconds.  
     */
    public Taxiway(int aprt, int[][][] taxiInTimes, int[][][] taxiOutTimes){ 
    	airport = aprt;
    	this.taxiInTimes = taxiInTimes;
    	this.taxiOutTimes = taxiOutTimes;
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
     * @param n The neighboring element. The type of the element is
     *   determined by using the "instanceof" operator.
     */
    public void setNeighbor(INode n){
    	// Only neighbors are the terminal and runway.
    	if(n instanceof Terminal){
    		terminalNode = n;
    	} else if(n instanceof Runway){
    		runwayNode = n;
    	} else {
    		final String method = this.getClass().getName() + ".setNeighbor()";
    		throw new IllegalArgumentException(method + ": node type: " +
    			n.getClass().getName() + " is not a neighbor of a taxiway.");
    	}
    }
    /**
     * Gets the node neighbors as an array.
     * @return array of node neighbors.
     */
    public INode[] getNeighbors(){
    	INode[] nn = new INode[2];
    	nn[0] = runwayNode;
    	nn[1] = terminalNode;
    	return nn;
    }
    
    /**
     * Computes the taxi-out time for a flight in or out of this airport.
     * It will depend upon the airport, the airline and the type of
     * aircraft.  At present, just uses the taxi-out time set during
     * the sim initialization, obtained from the flight object.
     * <p>
     * @param flight
     * @return taxi-out time of aircraft. Units: seconds.
     */
    public int computeTaxiOutTime(IFlight flight){
    	int taxiTime = flight.get(IFlight.Param.TAXI_OUT_TIME);
    	if(taxiTime > 0) return taxiTime;
    	else return 0;	
    }  
    /**
     * Computes the taxi-in time for a flight in or out of this airport.
     * It will depend upon the airport, the airline and the type of
     * aircraft.  At present, just uses the taxi-in time set during
     * the sim initialization, obtained from the flight object.
     * <p>
     * @param flight
     * @return taxi-in time of aircraft. Units: seconds.
     */
    public int computeTaxiInTime(IFlight flight){
    	int taxiTime = flight.get(IFlight.Param.TAXI_IN_TIME);
    	if(taxiTime > 0) return taxiTime;
    	else return 0;	
    }  
    
    //--------------------------------------------------------------------
    // Various getters and setters.  Just used for testing.
    //--------------------------------------------------------------------
    /**
     * Gets the airport index assoicated with this node.
     * @return airport index
     */
    public int getAirport(){return airport;}
    /**
     * Gets the taxi-in times for this taxiway.  First element is
     * the air carrier type as an index; the second is the equipment type.
     * The 3rd index denotes either the mean [0] or the std dev [1] of the 
     * distribution.
     * @return taxi-in times.  Units: seconds.
     */
    public int[][][] getTaxiInTimes(){
    	return taxiInTimes;   	
    }
    /**
     * Gets the taxi-out times for this taxiway.  First element is
     * the air carrier type as an index; the second is the equipment type.
     * The 3rd index denotes either the mean [0] or the std dev [1] of the 
     * distribution.
     * @return taxi-out times.  Units: seconds.
     */
    public int[][][] getTaxiOutTimes(){
    	return taxiOutTimes;   	
    }
      
}
