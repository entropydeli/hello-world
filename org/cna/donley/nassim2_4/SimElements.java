package org.cna.donley.nassim2_4;

/**
 * A container that holds the sim elements, nodes and flights.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: SimElements.java 1 2009-08-06 00:00:00EST $
 */

public class SimElements implements ISimElements
{
	/** base day of sim */
	private String baseDay = null;
	/** forecast day of sim */
	private String forecastDay = null;
	/** array of flights */
	private IFlight[] flights = null;
	/** array of NAS nodes */
	private INode[] nodes = null;
	/**
	 * Constructor.
	 * @param baseDay
	 * @param forecastDay
	 * @param flights
	 * @param nodes
	 */
    public SimElements(String baseDay, String forecastDay,
    	IFlight[] flights, INode[] nodes){
    	this.baseDay = baseDay;
    	this.forecastDay = forecastDay;
    	this.flights = flights;
    	this.nodes = nodes;
    }
    /** 
	 *  Returns the base start day of the simulation.  Usually the sim
     *   will start at midnight of the start day which is independent of
     *   the times in the flight files.  However, it is possible for the
     *   initializer to ignore this and use, say, the departure time of
     *   the first flight of the day.  For NASPAC, the change capacity
     *   file in pre-output/change has times w/r to midnight, while the
     *   one in sim-data/change has times w/r to the first flight 
     *   departure time (I think).  Since I will be using the pre-output
     *   change, must use this variable with NASPAC.
     *   @return base day of simulation.
	 */
	public String getBaseDay(){return baseDay;}
	/**
	 * Date sim is really supposed to start, but since
     *  all data is in terms of the base day, this is just used to find
     *  files, such as the desired flight schedule.
     *  @return forecast day of simulation.
     */
	public String getForecastDay(){return forecastDay;}
    /**
     * Returns the flights array.
     * @return {@link IFlight} array.
     */
    public IFlight[] getFlights(){return flights;}
    /**
     * Returns the nodes array.
     * @return {@link INode} array.
     */
    public INode[] getNodes(){return nodes;}
}
