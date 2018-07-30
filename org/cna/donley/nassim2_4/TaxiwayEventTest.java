package org.cna.donley.nassim2_4;

import java.util.List;
import java.util.ArrayList;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Test to validate the TaxiwayEvent class.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: TaxiwayEventTest.java 1 2009-09-02 00:00:00EST donley $
 */
public class TaxiwayEventTest
{
	/**
	 * test of constructors, getters and setters.
	 */
	@Test
	public void testConstructorNGettersNSetters(){
		// Terminal
		int aprt = 31;
		int[] carrier = {0,1};
		int[] equipType = {0,1};
		int[][][] nomTaxiTimes = {{{10,1},{7,2}},{{12,2},{6,3}}};
		Taxiway t1 = new Taxiway(aprt,null,null);
		assertTrue(t1 != null);
		
		// Flight(s)
		int[] itinNum = {47,11};
		int[] legNum = {2,1};
		int[] actOutTime = {220,313}; // Actual time out from the gate.
		int[] actOnTime = {410,520};  // Actual landing time.
		
		int[][] pars = new int[2][IFlight.Param.values().length];
		for(int i=0;i<2;i++){
			pars[i][IFlight.Param.ITIN_NUM.ordinal()]    = itinNum[i];
			pars[i][IFlight.Param.LEG_NUM.ordinal()]     = legNum[i];
			pars[i][IFlight.Param.CARRIER.ordinal()]     = carrier[i];
			pars[i][IFlight.Param.EQUIP_TYPE.ordinal()]  = equipType[i];
			pars[i][IFlight.Param.ACT_OUT_TIME.ordinal()]= actOutTime[i];
			pars[i][IFlight.Param.ACT_ON_TIME.ordinal()] = actOnTime[i];
			pars[i][IFlight.Param.TAXI_IN_TIME.ordinal()] =
				nomTaxiTimes[carrier[i]][equipType[i]][Nas.Distrib.MEAN.ordinal()];
			pars[i][IFlight.Param.TAXI_OUT_TIME.ordinal()] =
				nomTaxiTimes[carrier[i]][equipType[i]][Nas.Distrib.MEAN.ordinal()];
		}
    	IFlight[] fs = new Flight[2];
    	for(int i=0;i<2;i++){
    		fs[i] = new Flight(pars[i]);
    	}
    	TaxiwayEvent[] events = new TaxiwayEvent[2];
    	
    	//----------------------------------------------------------------------
    	// A departing and an arriving flight.
    	//----------------------------------------------------------------------
    	events[0] = new TaxiwayEvent(t1,fs[0],IEvent.Cmd.DEP,
    		fs[0].get(IFlight.Param.ACT_OUT_TIME));
    	events[1] = new TaxiwayEvent(t1,fs[1],IEvent.Cmd.ARR,
    		fs[1].get(IFlight.Param.ACT_ON_TIME));

    	// Test output and various getters.
    	assertTrue(actOutTime[0] == events[0].getTime());
    	assertTrue(actOnTime[1] == events[1].getTime());
    	for(int i=0;i<events.length;i++){
			assertTrue(t1 == events[i].getNode());
			assertTrue(fs[i] == events[i].getFlight());
		}
    	assertTrue(IEvent.Cmd.DEP == events[0].getMessage());
    	assertTrue(IEvent.Cmd.ARR == events[1].getMessage());
    	
    	// Test of compareTo().
    	assertTrue(-1 == events[0].compareTo(events[1]));
    	assertTrue(1 == events[1].compareTo(events[0]));
    	assertTrue(0 == events[0].compareTo(events[0]));
    	
    	// Test of equals().
    	assertTrue(events[0].equals(events[0]));
    	assertFalse(events[0].equals(events[1]));
    	
    	// setTime().
    	for(int i=0;i<events.length;i++){
    		events[i].setTime(8);
    		assertTrue(8 == events[i].getTime());
    	}
    	// getType()
    	assertTrue(IEvent.Type.TAXIWAY == events[0].getType());
    	
	}
	/**
	 * Test of exception in constructor.  Will accept only
	 * messages that are arr or dep.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testIllArgExceptionConstructor() {
		IEvent te = new TaxiwayEvent(null,null,IEvent.Cmd.BOGUS,0);
	}
	
	/**
	 * Test of processEvent().  Two tests. One for an arriving flight,
	 * one for a departing flight. 
	 */
	@Test
	public void testProcessEvent(){
		IQueue<IEvent> queue = new EventQueue(); 
		queue.clear();
			
		//----------------------------------------------------------------------
		// Departing flight.
		//----------------------------------------------------------------------
		// Taxiway event and create a runway one in it.
		int aprt = 31;
		int[] carrier = {0,1};
		int[] equipType = {0,1};
		int[][][] nomTaxiTimes = {{{10,1},{7,2}},{{12,2},{6,3}}};
		Taxiway tw = new Taxiway(aprt,null,null);
		Runway rw  = new Runway(aprt,0,false,null,null,null,null);

		assertTrue(rw != null);
		assertTrue(tw != null);
		
		// Flight(s)
		int[] itinNum = {47,11};
		int[] legNum = {2,1};
		int[] actOutTime = {391,266};
		int[] actOnTime = {410,520};  // Actual landing time.
		
		int[][] pars = new int[2][IFlight.Param.values().length];
		for(int i=0;i<2;i++){
			pars[i][IFlight.Param.ITIN_NUM.ordinal()]    = itinNum[i];
			pars[i][IFlight.Param.LEG_NUM.ordinal()]     = legNum[i];
			pars[i][IFlight.Param.CARRIER.ordinal()]     = carrier[i];
			pars[i][IFlight.Param.EQUIP_TYPE.ordinal()]  = equipType[i];
			pars[i][IFlight.Param.ACT_OUT_TIME.ordinal()]= actOutTime[i];
			pars[i][IFlight.Param.ACT_ON_TIME.ordinal()] = actOnTime[i];
			pars[i][IFlight.Param.TAXI_IN_TIME.ordinal()] =
				nomTaxiTimes[carrier[i]][equipType[i]][Nas.Distrib.MEAN.ordinal()];
			pars[i][IFlight.Param.TAXI_OUT_TIME.ordinal()] =
				nomTaxiTimes[carrier[i]][equipType[i]][Nas.Distrib.MEAN.ordinal()];
		}
    	IFlight[] fs = new Flight[2];
    	for(int i=0;i<2;i++){
    		fs[i] = new Flight(pars[i]);
    	}
    	// Create route. Only a runway since are in a taxiway.
    	List<INode> rList = new ArrayList<INode>();
    	rList.add(rw);
    	IRoute[] rte = new IRoute[2];
    	for(int i=0;i<fs.length;i++){
    		rte[i] = new Route(rList);
    		fs[i].setRoute(rte[i]);
    	}
    	
    	// Message
    	IEvent.Cmd message = IEvent.Cmd.DEP;
    	
    	// Create events.
    	TaxiwayEvent[] events = new TaxiwayEvent[2];
    	for(int i=0;i<2;i++){
    		events[i] = new TaxiwayEvent(tw,fs[i],message,
    			fs[i].get(IFlight.Param.ACT_OUT_TIME));	
    	}
    	// Check events (check again?  why not?)
    	for(int i=0;i<events.length;i++){
			assertTrue(actOutTime[i] == events[i].getTime());
			assertTrue(tw == events[i].getNode());
			assertTrue(fs[i] == events[i].getFlight());
			assertTrue(IEvent.Cmd.DEP == events[i].getMessage());
		}
    	// Process events.  Don't bother to add them to the queue.
    	for(int i=0;i<events.length;i++){
    		events[i].processEvent(queue);
    	}
    	// A event process of a departure is to change the calc off time of the
    	// flight and create a new runway event.  Check for those.
    	for(int i=0;i<events.length;i++){
    		IFlight fl = events[i].getFlight();
    		assertTrue(fl.get(IFlight.Param.CALC_OFF_TIME) == actOutTime[i] + 
    			nomTaxiTimes[carrier[i]][equipType[i]][Nas.Distrib.MEAN.ordinal()]);
    	}
    	while(queue.size() > 0){
    		IEvent eve = queue.poll();
    		assertTrue(eve instanceof RunwayEvent);
    		RunwayEvent re = (RunwayEvent)eve;
    		
    		int indx = 0;
    		if(re.getFlight().get(IFlight.Param.ITIN_NUM) == itinNum[1])indx = 1;
    		
    		int tim = actOutTime[indx]+ 
    			nomTaxiTimes[carrier[indx]][equipType[indx]][Nas.Distrib.MEAN.ordinal()];
    		assertTrue(tim == re.getTime());
			assertTrue(fs[indx] == re.getFlight());
			assertTrue(fs[indx].getRoute().getLastNode() == re.getNode());
			assertTrue(IEvent.Cmd.DEP == re.getMessage());	
    	}
    	
    	//---------------------------------------------------------------------
    	// Arriving flights
    	//---------------------------------------------------------------------
    	// Create a taxiway event for the arriving flight, process it and
    	// create a terminal event on output.
    	tw = new Taxiway(aprt,null,null);
		Terminal t1 = new Terminal(aprt,null,null);

		assertTrue(t1 != null);
		assertTrue(tw != null);
		
		// Flight(s)
		for(int i=0;i<2;i++){
			pars[i][IFlight.Param.ITIN_NUM.ordinal()]    = itinNum[i];
			pars[i][IFlight.Param.LEG_NUM.ordinal()]     = legNum[i];
			pars[i][IFlight.Param.CARRIER.ordinal()]     = carrier[i];
			pars[i][IFlight.Param.EQUIP_TYPE.ordinal()]  = equipType[i];
			pars[i][IFlight.Param.ACT_OUT_TIME.ordinal()]= actOutTime[i];
			pars[i][IFlight.Param.ACT_ON_TIME.ordinal()] = actOnTime[i];
			pars[i][IFlight.Param.TAXI_IN_TIME.ordinal()] =
				nomTaxiTimes[carrier[i]][equipType[i]][Nas.Distrib.MEAN.ordinal()];
			pars[i][IFlight.Param.TAXI_OUT_TIME.ordinal()] =
				nomTaxiTimes[carrier[i]][equipType[i]][Nas.Distrib.MEAN.ordinal()];
		}
    	fs = new Flight[2];
    	for(int i=0;i<2;i++){
    		fs[i] = new Flight(pars[i]);
    	}
    	// Create route. Only a terminal since are in a taxiway.
    	rList = new ArrayList<INode>();
    	rList.add(t1);
    	for(int i=0;i<fs.length;i++){
    		rte[i] = new Route(rList);
    		fs[i].setRoute(rte[i]);
    	}
    	// Message
    	message = IEvent.Cmd.ARR;
    	
    	// Create events.
    	events = new TaxiwayEvent[2];
    	for(int i=0;i<2;i++){
    		events[i] = new TaxiwayEvent(tw,fs[i],message,
    			fs[i].get(IFlight.Param.ACT_ON_TIME));
    		
    	}
    	// Check events (check again?  why not?)
    	for(int i=0;i<events.length;i++){
			assertTrue(actOnTime[i] == events[i].getTime());
			assertTrue(tw == events[i].getNode());
			assertTrue(fs[i] == events[i].getFlight());
			assertTrue(IEvent.Cmd.ARR == events[i].getMessage());
		}
    	// Process events.
    	for(int i=0;i<events.length;i++){
    		events[i].processEvent(queue);
    	}
    	// A event process of an arrival is to change the calc in time of the
    	// flight and create a new terminal event.  Check for those.
    	for(int i=0;i<events.length;i++){
    		IFlight fl = events[i].getFlight();
    		assertTrue(fl.get(IFlight.Param.CALC_IN_TIME) == actOnTime[i] + 
    			nomTaxiTimes[carrier[i]][equipType[i]][Nas.Distrib.MEAN.ordinal()]);
    	}
    	while(queue.size() > 0){
    		IEvent eve = queue.poll();
    		assertTrue(eve instanceof TerminalEvent);
    		TerminalEvent te = (TerminalEvent)eve;
    		
    		int indx = 0;
    		if(te.getFlight().get(IFlight.Param.ITIN_NUM) == itinNum[1])indx = 1;
    		
    		int tim = actOnTime[indx]+ 
    			nomTaxiTimes[carrier[indx]][equipType[indx]][Nas.Distrib.MEAN.ordinal()];
    		assertTrue(tim == te.getTime());
    		assertTrue(fs[indx] == te.getFlight());
			assertTrue(fs[indx].getRoute().getLastNode() == te.getNode());
			assertTrue(IEvent.Cmd.ARR == te.getMessage());	
    	}
 
	}
	
}
