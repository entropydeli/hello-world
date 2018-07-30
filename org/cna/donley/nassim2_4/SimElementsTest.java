package org.cna.donley.nassim2_4;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * Test to validate the {@link SimElements} class.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: SimElementsTest.java 1 2009-06-04 00:00:00EST donley $
 */
public class SimElementsTest
{   
    /**
     * Test constructor and getters.
     */
    @Test
	public void testConstructorNGetters()
    {
    	String baseDay = "20080828";
    	String forecastDay = "20090825";
    	IFlight[] flights = new IFlight[10];
    	INode[] nodes = new INode[10];
    	SimElements si = new SimElements(baseDay,forecastDay,flights,nodes);
    	assertTrue(si != null);
    	assertTrue(baseDay.equals(si.getBaseDay()));
    	assertTrue(forecastDay.equals(si.getForecastDay()));
    	assertTrue(flights == si.getFlights());
    	assertTrue(nodes == si.getNodes());  	  	
    }	
}
