package org.cna.donley.nassim2_4;

import java.util.List;
import java.util.ArrayList;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Test to validate the TerminalEvent class.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: TerminalEventTest.java 1 2009-09-01 00:00:00EST donley $
 */
public class TerminalEventTest
{
	/**
	 * test of constructors, getters and setters.
	 */
	@Test
	public void testConstructorNGettersNSetters(){
		// Terminal
		int aprt = 31;
		Terminal t1 = new Terminal(aprt,null,null);
		assertTrue(t1 != null);
		// Flight(s)
		int[] itinNum = {47,11};
		int[] legNum = {2,1};
		int[] schedOutTime = {1391,466};
		
		int[][] pars = new int[2][IFlight.Param.values().length];
		for(int i=0;i<2;i++){
			pars[i][IFlight.Param.ITIN_NUM.ordinal()] = itinNum[i];
			pars[i][IFlight.Param.LEG_NUM.ordinal()] = legNum[i];
			pars[i][IFlight.Param.SCHED_OUT_TIME.ordinal()] = schedOutTime[i];
		}
    	IFlight[] fs = new Flight[2];
    	for(int i=0;i<2;i++){
    		fs[i] = new Flight(pars[i]);
    	}
    	// Message
    	IEvent.Cmd message = IEvent.Cmd.DEP;
    	IEvent[] events = new IEvent[2];
    	for(int i=0;i<2;i++){
    		events[i] = new TerminalEvent(t1,fs[i],message,
    			fs[i].get(IFlight.Param.SCHED_OUT_TIME));
    	}
    	// Test output.  Includes test of getTime() method.
    	for(int i=0;i<events.length;i++){
			assertTrue(schedOutTime[i] == events[i].getTime());
			assertTrue(t1 == ((TerminalEvent)events[i]).getNode());
			assertTrue(fs[i] == ((TerminalEvent)events[i]).getFlight());
			assertTrue(IEvent.Cmd.DEP ==
				((TerminalEvent)events[i]).getMessage());
		}
    	
    	// Test of compareTo().
    	assertTrue(1 == events[0].compareTo(events[1]));
    	assertTrue(-1 == events[1].compareTo(events[0]));
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
    	assertTrue(IEvent.Type.TERMINAL == events[0].getType());
    	
	}
	/**
	 * Test of exception in constructor.  Will accept only
	 * messages that are arr or dep.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testIllArgExceptionConstructor() {
		IEvent te = new TerminalEvent(null,null,IEvent.Cmd.BOGUS,0);
	}
	
	/**
	 * Test of processEvent().  Two tests. One for an arriving flight,
	 * one for a departing flight. 
	 */
	@Test
	public void testProcessEvent(){
		
		//----------------------------------------------------------------------
		// Departing flight.
		//----------------------------------------------------------------------
		// Terminal
		int aprt = 31;
		Terminal t1 = new Terminal(aprt,null,null);
		Taxiway tw = new Taxiway(aprt,null,null);
		assertTrue(t1 != null);
		assertTrue(tw != null);
		
		// Flight(s)
		int[] itinNum = {47,11};
		int[] legNum = {2,1};
		int[] schedOutTime = {1391,466};
		
		int[][] pars = new int[2][IFlight.Param.values().length];
		for(int i=0;i<2;i++){
			pars[i][IFlight.Param.ITIN_NUM.ordinal()] = itinNum[i];
			pars[i][IFlight.Param.LEG_NUM.ordinal()] = legNum[i];
			pars[i][IFlight.Param.SCHED_OUT_TIME.ordinal()] = schedOutTime[i];
		}
    	IFlight[] fs = new Flight[2];
    	for(int i=0;i<2;i++){
    		fs[i] = new Flight(pars[i]);
    	}
    	// Create routes.  Add only taxiways to routes because the
    	// Terminal will already have been sampled
    	List<INode> nList = new ArrayList<INode>();
    	nList.add(tw);
    	IRoute r1 = new Route(nList);
    	fs[0].setRoute(r1);
    	nList.clear();
    	nList.add(tw);
    	IRoute r2 = new Route(nList);
    	fs[1].setRoute(r2);
    	
    	// Message
    	IEvent.Cmd message = IEvent.Cmd.DEP;
    	
    	// Create events.
    	TerminalEvent[] events = new TerminalEvent[2];
    	for(int i=0;i<2;i++){
    		events[i] = new TerminalEvent(t1,fs[i],message,
    			fs[i].get(IFlight.Param.SCHED_OUT_TIME));
    		
    	}
    	// Check events (check again?  why not?)
    	for(int i=0;i<events.length;i++){
			assertTrue(schedOutTime[i] == events[i].getTime());
			assertTrue(t1 == ((TerminalEvent)events[i]).getNode());
			assertTrue(fs[i] == ((TerminalEvent)events[i]).getFlight());
			assertTrue(IEvent.Cmd.DEP ==
				((TerminalEvent)events[i]).getMessage());
		}
    	// Process events.
    	IQueue<IEvent> queue = new EventQueue();
    	for(int i=0;i<events.length;i++){
    		events[i].processEvent(queue);
    	}
    	// A event process of a departure is to change the act out time of the
    	// flight and create a new taxiway event.  Check for those.
    	for(int i=0;i<events.length;i++){
    		IFlight fl = events[i].getFlight();
    		assertTrue(fl.get(IFlight.Param.ACT_OUT_TIME) == 
    			fl.get(IFlight.Param.SCHED_OUT_TIME));
    	}
    	while(queue.size() > 0){
    		IEvent eve = queue.poll();
    		assertTrue(eve instanceof TaxiwayEvent);
    		TaxiwayEvent te = (TaxiwayEvent)eve;
    		
    		int indx = 0;
    		if(te.getFlight().get(IFlight.Param.ITIN_NUM) == itinNum[1])indx = 1;
    			
    		assertTrue(schedOutTime[indx] == te.getTime());
			assertTrue(fs[indx].getRoute().getLastNode() == te.getNode());
			assertTrue(fs[indx] == te.getFlight());
			assertTrue(IEvent.Cmd.DEP == te.getMessage());	
    	}
    	
    	//---------------------------------------------------------------------
    	// Arriving flights
    	//---------------------------------------------------------------------
    	// Have two flights of the same itinerary. One is arriving and the
    	// other will depart when the first has arrived.
    	// Case 1: computed minimum out time for second leg is larger than
    	// 		the sched dep time + pushback time of that flight.
    	itinNum[0]=11;
    	itinNum[1]=11;
    	legNum[0]=1;
    	legNum[1]=2;
    	schedOutTime[0]=220;
    	schedOutTime[1]=421;
    	int[] calcInTime = {391,0};
    	int[] turnTime = {45,23};
    	int[] pushTime = {-21,-33};
    	pars = new int[2][IFlight.Param.values().length];
		for(int i=0;i<2;i++){
			pars[i][IFlight.Param.DEP_APRT.ordinal()]       = aprt;
			pars[i][IFlight.Param.ITIN_NUM.ordinal()]       = itinNum[i];
			pars[i][IFlight.Param.LEG_NUM.ordinal()]        = legNum[i];
			pars[i][IFlight.Param.SCHED_OUT_TIME.ordinal()] = schedOutTime[i];
			pars[i][IFlight.Param.CALC_IN_TIME.ordinal()]   = calcInTime[i];
			pars[i][IFlight.Param.TURN_TIME.ordinal()]      = turnTime[i];	
			pars[i][IFlight.Param.PUSHBACK_TIME.ordinal()]  = pushTime[i];
		}
    	fs = new Flight[2];
    	for(int i=0;i<2;i++){
    		fs[i] = new Flight(pars[i]);
    	}
    	fs[0].setNextLeg(fs[1]);
    	fs[1].setPrevLeg(fs[0]);
    	// Set routes.
    	nList = new ArrayList<INode>();
    	// Empty route because it has arrived.
    	r1 = new Route(nList);
    	fs[0].setRoute(r1);
    	// New route because it will be executed.
    	nList.add(t1);
    	nList.add(tw);
    	r2 = new Route(nList);
    	fs[1].setRoute(r2);
    	
    	// Message
    	message = IEvent.Cmd.ARR;
    	
    	// Create event for first flight, which is arriving.
    	TerminalEvent eve = null;
    	eve = new TerminalEvent(t1,fs[0],message,
    			fs[0].get(IFlight.Param.CALC_IN_TIME));
    		
    	// Check events (check again?  why not?)
		assertTrue(calcInTime[0] == eve.getTime());
		assertTrue(t1 == ((TerminalEvent)eve).getNode());
		assertTrue(fs[0] == ((TerminalEvent)eve).getFlight());
		assertTrue(IEvent.Cmd.ARR ==((TerminalEvent)eve).getMessage());
    	// Process events.
		queue.clear();
    	eve.processEvent(queue);

    	// An event process of an arrival is to change the actual in time of the
    	// flight.  Then if the flight has another leg, generate a departing
    	// TerminalEvent for that leg and add it to the queue.
    	// Is flight parked?
		IFlight fl = eve.getFlight();
		assertTrue(fl.get(IFlight.Param.CALC_IN_TIME) == 
			fl.get(IFlight.Param.ACT_IN_TIME));

		// Is next leg event created properly?
		IEvent ev = queue.poll();
		assertTrue(ev instanceof TerminalEvent);
		TerminalEvent te = (TerminalEvent)ev;
		int indx = 1;
		assertTrue(fs[indx] == te.getFlight());
		assertTrue(te.getFlight().get(IFlight.Param.ITIN_NUM) == itinNum[indx]);
		assertTrue(fs[indx].getRoute().getLastNode() == te.getNode());
		assertTrue(IEvent.Cmd.DEP == te.getMessage());
		// minimum out time should be the previous flight's in time + that
		// flight's turntime.
		int minOutTime = fs[0].get(IFlight.Param.ACT_IN_TIME) +
			 fs[0].get(IFlight.Param.TURN_TIME);
		assertTrue(minOutTime == fs[1].get(IFlight.Param.MIN_OUT_TIME));
		// Compare with sch + push time. 
		int schNPushTime = fs[1].get(IFlight.Param.SCHED_OUT_TIME) +
			fs[1].get(IFlight.Param.PUSHBACK_TIME);
		if(minOutTime > schNPushTime)assertTrue(te.getTime() == minOutTime);
		else assertTrue(te.getTime() == schNPushTime);
		
		// Case 2: computed minimum out time for second leg is smaller than
    	// 		the sched dep + push time of that flight.
		fs[1].set(IFlight.Param.SCHED_OUT_TIME, 500);
		//      Need to put Terminal back into route since want to process it
		//      as a new departing leg.
		// New route because it will be executed.
    	nList.add(t1);
    	nList.add(tw);
    	r2 = new Route(nList);
    	fs[1].setRoute(r2);
    	// Process event for fs[0].
		queue.clear();
		eve.processEvent(queue);
		ev = queue.poll();
		te = (TerminalEvent)ev;
		schNPushTime = fs[1].get(IFlight.Param.SCHED_OUT_TIME) +
			fs[1].get(IFlight.Param.PUSHBACK_TIME);
		assertTrue(schNPushTime == te.getTime());	
 
	}
	
}
