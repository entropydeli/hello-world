package org.cna.donley.nassim2_4;

import java.util.List;
import java.util.ArrayList;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

/**
 * Test to validate the Flight class.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: FlightTest.java 1 2009-06-04 00:00:00EST donley $
 */
public class FlightTest
{   
    /**
     * Test constructors and getters.
     */
    @Test
	public void testOne()
    {
    	
    	// Constructors, setters and getters.
    	int flID = 27;
    	int aprt = 1;
    	int[] pars = new int[IFlight.Param.values().length];
    	for(int i=0;i<pars.length;i++){
    		if(i==IFlight.Param.SCHED_ID.ordinal())pars[i] = flID;
    		if(i==IFlight.Param.LEG_NUM.ordinal())pars[i] = 3;
    		if(i==IFlight.Param.DEP_APRT.ordinal())pars[i] = aprt;
    		if(i==IFlight.Param.ARR_APRT.ordinal())pars[i] = aprt;
    		else pars[i] = i;
    	}
    	IFlight f    = new Flight(pars);
    	assertFalse(f==null);
    	IFlight.Param[] p = IFlight.Param.values();
    	for(int i=0;i<p.length;i++){
    		assertTrue(pars[i] == f.get(p[i]));
    	}
    	
    	pars[IFlight.Param.LEG_NUM.ordinal()] = 2;
    	IFlight pLeg = new Flight(pars);
    	pars[IFlight.Param.LEG_NUM.ordinal()] = 4;
    	IFlight nLeg = new Flight(pars);
    	f.setPrevLeg(pLeg);
    	f.setNextLeg(nLeg);
    	assertTrue(pLeg == f.getPrevLeg());
    	assertTrue(nLeg == f.getNextLeg());
    	
    	// Test of setRoute() and getRoute();
    	INode arrRunway = new Runway(0,0,false,null,null,null,null);
    	INode depRunway = new Runway(1,0,false,null,null,null,null);
    	List<INode> route = new ArrayList<INode>();
    	route.add(depRunway);
    	route.add(arrRunway);
    	IRoute rr = new Route(route);
    	f.setRoute(rr);
    	assertTrue(depRunway == f.getRoute().getNextNode());
    	assertTrue(arrRunway == f.getRoute().getNextNode());
    	List<INode> r2 = f.getRoute().getNodes();
    	for(int i=0;i<route.size();i++){
    		assertTrue(route.get(i) == r2.get(i));
    	}
    	
    	
    	// Set flight variable.
    	IFlight f4 = new Flight(pars);
    	IFlight.Param[] p3 = IFlight.Param.values();
    	for(int i=0;i<p.length;i++){
    		f4.set(p3[i], -i*i);
    		assertTrue(f4.get(p3[i]) == -i*i);
    	}
    	
    	// compareTo() and equals().
    	// Have diff ID's.
    	pars[IFlight.Param.SCHED_ID.ordinal()] = 1;
    	pars[IFlight.Param.LEG_NUM.ordinal()] = 1;
    	f.set(IFlight.Param.SCHED_ID, 1);
    	f.set(IFlight.Param.LEG_NUM,1);
    	int[] pars2 = new int[IFlight.Param.values().length];
    	pars2[IFlight.Param.SCHED_ID.ordinal()] = 21;
    	pars2[IFlight.Param.LEG_NUM.ordinal()] = 2;
    	IFlight f2 = new Flight(pars2);
    	assertTrue(f.compareTo(f2)==-1);
    	assertTrue(f2.compareTo(f)==1);
    	assertTrue(f.compareTo(f)==0);
    	assertTrue(f.equals(f));
    	assertFalse(f.equals(f2));
    	// Have the same ID, but different leg numbers.
    	int[] pars3 = new int[IFlight.Param.values().length];
    	pars3[IFlight.Param.SCHED_ID.ordinal()] = 21;
    	pars3[IFlight.Param.LEG_NUM.ordinal()] = 1;
    	IFlight f3 = new Flight(pars3);
    	assertTrue(f2.compareTo(f3)==1);
    	assertTrue(f3.compareTo(f2)==-1);
    	assertTrue(f2.compareTo(f2)==0);
    	assertTrue(f3.equals(f3));
    	assertFalse(f3.equals(f2));	   	
    }
    
    /**
	 * Test of exception in Flight constructor if the input params
	 * array doesn't have the correct length.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testIllArgException() {
		int[] pars = new int[24];
		for(int i=0;i<pars.length;i++)pars[i] = i;
		IFlight f = new Flight(pars);
	}
	
}
