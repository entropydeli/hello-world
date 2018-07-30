package org.cna.donley.nassim2_4;

/**
 * An interface for an generic simulation.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: ISimulation.java 1 2009-08-28 00:00:00EST $
 */

public interface ISimulation 
{ 
	/**
	 * Starts the simulation.  This includes initializing the simulation,
	 * running it and producing output.
	 */
	public void startSimulation();
	
	/**
	 * Initializes the simulation.
	 */
	public void initialize();
	
	/**
	 * Executes the events in the queue.
	 */
    public void run();
    
    /**
     * Produces output, usually to a file, after the sim is run.
     * @return <code>true</code> if successful; <code>false</code> if not.
     */
    public boolean processOutput();
  
}
