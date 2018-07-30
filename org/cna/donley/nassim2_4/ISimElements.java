package org.cna.donley.nassim2_4;

/**
 * An interface to a container that holds the sim elements, 
 * nodes and flights.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: ISimElements.java 1 2009-08-11 00:00:00EST $
 */

public interface ISimElements
{
	
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
	public String getBaseDay();
	/**
	 * Date sim is really supposed to start, but since
     *  all data is in terms of the base day, this is just used to find
     *  files, such as the desired flight schedule.
     *  @return forecast day of simulation.
     */
	public String getForecastDay();
    /**
     * Returns the flights array.
     * @return {@link IFlight} array.
     */
    public IFlight[] getFlights();
    /**
     * Returns the nodes array.
     * @return {@link INode} array.
     */
    public INode[] getNodes();
}
