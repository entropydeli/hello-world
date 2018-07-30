package org.cna.donley.nassim2_4;

/**
 * This is an interface for classes that initialize the NAS simulation.
 * These classes should use data to create all the {@link IFlight} and
 * {@link INode} objects.  This includes setting up the NAS network
 * by linking the nodes and flights.  It is separate from the 
 * {@link ISimulation} class because the implementation of this 
 * interface can vary a lot. For example, it does not create the initial
 * events for the first leg of the departing flights.  That is done
 * in the implementation of {@link ISimulation}.
 * <p>
 * The class to this must also create the {@link Nas} singleton, including
 * various maps such as the airport-to-index map.  I mean, if not here,
 * then where?
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: ISimInitializer.java 1 2009-06-16 00:00:00EST $
 */

public interface ISimInitializer
{	
	/**
	 * This method initializes the simulation, giving arrays of flights,
	 * NAS network nodes and initial events upon output.  It should also create
	 * the {@link Nas} singleton class.
     * Last, it obtains the sim base and forecast days, the forecast day being
     * the start of the simulation.
	 * @return Object of class that implements the {@link ISimElements}
	 *   interface, which contains flights and NAS network nodes of
	 *   type {@link IFlight} and {@link INode}, respectively, and
	 *   the sim base and forecast days.
	 */
	public ISimElements initialize();
  
}
