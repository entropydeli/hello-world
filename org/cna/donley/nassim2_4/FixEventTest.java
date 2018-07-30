package org.cna.donley.nassim2_4;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Test to validate the FixEvent class.  With processEvent()
 * need to handle a number of cases, deps with a delay 
 * (which creates a {@link HoldEvent}), deps with no
 * delay.  And ditto for arrivals.  So, that's 2x2 = 4 different cases. 
 * I am only doing two here.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: FixEventTest.java 1 2009-11-05 00:00:00EST donley $
 */
public class FixEventTest
{
	/**
	 * test of constructors, getters and setters.
	 */
	@Test
	public void testConstructorNGettersNSetters(){
		int aprt = 31;
		int name = 11;
		int minTimeSpacing = 180;
		
		Fix ff = new Fix(name,Nas.Ad.DEP,minTimeSpacing);
		assertTrue(ff != null);
		
		// Flight(s)
		int[] itinNum = {47,11};
		int[] legNum = {2,1};
		int[] calcOnTime = {220,313}; // calculate on time.
		int[] calcOffTime = {310,520};  // calculated out time.
		int[] airborneTime = {1200,600};
		
		int[][] pars = new int[2][IFlight.Param.values().length];
		for(int i=0;i<2;i++){
			pars[i][IFlight.Param.ITIN_NUM.ordinal()]     = itinNum[i];
			pars[i][IFlight.Param.LEG_NUM.ordinal()]      = legNum[i];
			pars[i][IFlight.Param.CALC_ON_TIME.ordinal()] = calcOnTime[i];
			pars[i][IFlight.Param.ACT_OFF_TIME.ordinal()] = calcOffTime[i];
			pars[i][IFlight.Param.ACT_AIR_TIME.ordinal()] = airborneTime[i];
		}
    	IFlight[] fs = new Flight[2];
    	for(int i=0;i<2;i++){
    		fs[i] = new Flight(pars[i]);
    	}
    	IEvent[] events = new FixEvent[2];
    	
    	//----------------------------------------------------------------------
    	// A departing and an arriving flight.
    	//----------------------------------------------------------------------
    	events[0] = new FixEvent(ff,fs[0],IEvent.Cmd.DEP,
    		fs[0].get(IFlight.Param.ACT_OFF_TIME));
    	events[1] = new FixEvent(ff,fs[1],IEvent.Cmd.ARR,
    		fs[1].get(IFlight.Param.CALC_ON_TIME));

    	// Test output and various getters.
    	// getTime
    	assertTrue(calcOffTime[0] == events[0].getTime());
    	assertTrue(calcOnTime[1] == events[1].getTime());
    	// getNode and getFlight
    	for(int i=0;i<events.length;i++){
			assertTrue(ff == events[i].getNode());
			assertTrue(fs[i] == ((FixEvent)events[i]).getFlight());
		}
    	// getMessage()
    	assertTrue(IEvent.Cmd.DEP == events[0].getMessage());
    	assertTrue(IEvent.Cmd.ARR == events[1].getMessage());
    	// getType()
    	for(IEvent eve : events){
    		assertTrue(IEvent.Type.FIX == eve.getType());
    	}
    	// getFlight()
    	for(int i=0;i<fs.length;i++){
    		assertTrue(fs[i] == events[i].getFlight());
    	}
    	
    	// setTime().
    	for(int i=0;i<events.length;i++){
    		events[i].setTime(8);
    		assertTrue(8 == events[i].getTime());
    	}
    	
	}
	/**
	 * 
	 */
	@Test
	public void testCompareToNEquals(){
    	// Create events.
    	IEvent[] events = new FixEvent[2];
    	int[] times = {100,200};
    	for(int i=0;i<events.length;i++){
    		events[i] = new FixEvent(null,null,IEvent.Cmd.ARR,times[i]);
    	}
    	// Test of compareTo() with diff events times.
    	assertTrue(-1 == events[0].compareTo(events[1]));
    	assertTrue(1 == events[1].compareTo(events[0]));
    	assertTrue(0 == events[0].compareTo(events[0]));
    	assertTrue(!events[0].equals(events[1]));
    	// Now same event times.
    	for(int i=0;i<events.length;i++){
    		events[i] = new FixEvent(null,null,IEvent.Cmd.DEP,120);
    	}
    	assertTrue(0 == events[0].compareTo(events[1]));
    	assertTrue(0 == events[1].compareTo(events[0]));
    	
    	assertTrue(events[0].equals(events[1]));
		
	}
	/**
	 * Test of exception in constructor.  Will accept only
	 * messages that are arr or dep.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testIllArgExceptionConstructor() {
		IEvent fe = new FixEvent(null,null,IEvent.Cmd.BOGUS,0);
	}
	
	/**
	 * Test of processEvent().  Two tests. One for an arriving flight,
	 * one for a departing flight. 
	 */
	@Test
	public void testProcessEvent(){
		IQueue<IEvent> queue = new EventQueue();
		
		//----------------------------------------------------------------------
		// Departing flight. From Dep Fix to DummyNode.
		//----------------------------------------------------------------------
		// Here, have three flights that go through the same fix, one after
		// another.  The first flight is not delayed by the fix, but the
		// others are.
		int aprt = 31;
		int nameDepFix = 11, nameArrFix = 23;
		int minTimeSpacing = 240;
		
		Fix fd = new Fix(nameDepFix,Nas.Ad.DEP,minTimeSpacing);
		DummyNode dn = new DummyNode();
		assertTrue(fd != null);
		assertTrue(dn != null);
		
		// Flight(s)
		int nFlights = 3;
		int[] itinNum = {47,11,12};
		int[] legNum = {2,1,1};
		int[] actOffTime = {310,520,412};  // calculated out time.
		int[] airborneTime = {1200,1180,880};
		
		int[][] pars = new int[nFlights][IFlight.Param.values().length];
		for(int i=0;i<nFlights;i++){
			pars[i][IFlight.Param.ITIN_NUM.ordinal()]     = itinNum[i];
			pars[i][IFlight.Param.LEG_NUM.ordinal()]      = legNum[i];
			pars[i][IFlight.Param.ACT_OFF_TIME.ordinal()] = actOffTime[i];
			pars[i][IFlight.Param.ACT_AIR_TIME.ordinal()] = airborneTime[i];
		}
    	IFlight[] fs = new Flight[nFlights];
    	for(int i=0;i<nFlights;i++){
    		fs[i] = new Flight(pars[i]);
    	}
    	
    	// Create routes.  Only have arrival fix objects in route because
    	// have no sectors.  Create routes.  For simplicity, let flights go 
    	// to a DummyNode.
    	List<INode> rList = null;
    	IRoute rt = null;
    	for(int i=0;i<nFlights;i++){
    		rList = new ArrayList<INode>();
    		rList.add(dn);
    		rt = new Route(rList);
    		fs[i].setRoute(rt);
    	}
    	
    	// Create fix events.
    	IEvent[] events = new FixEvent[nFlights];
    	for(int i=0;i<events.length;i++){
	    	events[i] = new FixEvent(fd,fs[i],IEvent.Cmd.DEP,
	    		fs[i].get(IFlight.Param.ACT_OFF_TIME));
    	}
  	
    	// Check events (check again?  why not?)
    	for(int i=0;i<events.length;i++){
			assertTrue(actOffTime[i] == events[i].getTime());
			assertTrue(fd == events[i].getNode());
			assertTrue(fs[i] == events[i].getFlight());
			assertTrue(IEvent.Cmd.DEP == events[i].getMessage());
		}
    	// Process events. There may be a hold event in between, so need to 
    	// handle that too.
    	for(int i=0;i<events.length;i++){
    		queue.add(events[i]);
    	}
    	// Process events.  Dummy events do nothing. Flights go to DummyNode 
    	// objects.  Save dummy objects.
    	List<IEvent> dums = new ArrayList<IEvent>();
    	IEvent eve = null;
    	while((eve = queue.poll()) != null){
    		eve.processEvent(queue);
    		if(eve instanceof DummyEvent){
    			dums.add(eve);
    		}
    	}
    	
    	// An event process of a departure is to change the actual off time of the
    	// flight if there is a fix delay and create a new arrival fix event for
    	// the flight. Second flight will be delayed. 
    	int[] delay = {0,2*minTimeSpacing,minTimeSpacing};
    	for(int i=0;i<nFlights;i++){
			assertTrue(events[i].getFlight().get(IFlight.Param.ACT_OFF_TIME) 
				== actOffTime[0] + delay[i]);
			assertTrue(events[i].getFlight().get(IFlight.Param.DEP_FIX_DELAY) 
				== actOffTime[0] - actOffTime[i] + delay[i]);
    	}
		
  
		assertTrue(dums.size() == nFlights);
    	for(int i=0;i<dums.size();i++){
    		eve = dums.get(i);
    		assertTrue(eve instanceof DummyEvent);
			assertTrue(IEvent.Cmd.ARR == eve.getMessage());	
    		IFlight fl = eve.getFlight();
    		int airTime = fl.get(IFlight.Param.ACT_AIR_TIME);
    		int tim = fl.get(IFlight.Param.ACT_OFF_TIME) + airTime;
    		assertTrue(tim == eve.getTime());
    		assertTrue(tim == fl.get(IFlight.Param.CALC_ON_TIME));
    		assertTrue(fl.getRoute().getLastNode() == eve.getNode());// 
    	}
    	
    	//---------------------------------------------------------------------
    	// Arriving flights.  From Arr Fix to DummyNode.
    	//---------------------------------------------------------------------
    	// Here, have two flights that go through the same fix, one after
		// another.  The second flight is delayed by the fix.
		Fix fa = new Fix(nameArrFix,Nas.Ad.ARR,minTimeSpacing);
		
		// Flight(s)
		int[] calcOnTime = {1322,1532,1400};
		for(int i=0;i<nFlights;i++){
			pars[i][IFlight.Param.ITIN_NUM.ordinal()]     = itinNum[i];
			pars[i][IFlight.Param.LEG_NUM.ordinal()]      = legNum[i];
			pars[i][IFlight.Param.CALC_ON_TIME.ordinal()] = calcOnTime[i];
		}
    	fs = new Flight[nFlights];
    	for(int i=0;i<nFlights;i++){
    		fs[i] = new Flight(pars[i]);
    	}
    	// Create routes.  Only need Runway objects in route. For simplicity, 
    	// let flights go to a DummyNode.
    	for(int i=0;i<nFlights;i++){
    		rList = new ArrayList<INode>();
    		rList.add(dn);
    		rt = new Route(rList);
    		fs[i].setRoute(rt);
    	}
    	
    	// Create events. A fix event creates a runway one.
    	events = new FixEvent[nFlights];
    	for(int i=0;i<events.length;i++){
    		events[i] = new FixEvent(fa,fs[i],IEvent.Cmd.ARR,
    			fs[i].get(IFlight.Param.CALC_ON_TIME));
    	}	
    	
    	// Check events (check again?  why not?)
    	for(int i=0;i<events.length;i++){
			assertTrue(fs[i].get(IFlight.Param.CALC_ON_TIME) == 
				events[i].getTime());
			assertTrue(fa == events[i].getNode());
			assertTrue(fs[i] == events[i].getFlight());
			assertTrue(IEvent.Cmd.ARR == events[i].getMessage());
		}
    	// Process events. There may be a hold event in between, so need to 
    	// handle that too.
    	queue.clear();
    	for(int i=0;i<events.length;i++){
    		queue.add(events[i]);
    	}
    	// Process events.  Dummy events do nothing. Flights go to DummyNode 
    	// objects.  Save dummy objects.
    	dums.clear();
    	while((eve = queue.poll()) != null){
    		eve.processEvent(queue);
    		if(eve instanceof DummyEvent){
    			dums.add(eve);
    		}
    	}
    	// An event process of a departure is to change the actual off time of the
    	// flight if there is a fix delay and create a new arrival fix event for
    	// the flight. Second flight will be delayed. 
    	int[] aDelay = {0,2*minTimeSpacing,minTimeSpacing};
    	for(int i=0;i<nFlights;i++){
			assertTrue(events[i].getFlight().get(IFlight.Param.ACT_ON_TIME) 
				== calcOnTime[0] + aDelay[i]);
			assertTrue(events[i].getFlight().get(IFlight.Param.ARR_FIX_DELAY) 
				== calcOnTime[0] - calcOnTime[i] + aDelay[i]);
    	}
		
		assertTrue(dums.size() == nFlights);
    	for(int i=0;i<dums.size();i++){
    		eve = dums.get(i);
    		assertTrue(eve instanceof DummyEvent);
			assertTrue(IEvent.Cmd.ARR == eve.getMessage());	
    		IFlight fl = eve.getFlight();
    		int tim = fl.get(IFlight.Param.ACT_ON_TIME);
    		assertTrue(tim == eve.getTime());
    		assertTrue(fl.getRoute().getLastNode() == eve.getNode());// 
    	}
 
	}
	
}
