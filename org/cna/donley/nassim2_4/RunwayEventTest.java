package org.cna.donley.nassim2_4;

import java.util.List;
import java.util.ArrayList;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Test to validate the RunwayEvent class.  With processEvent()
 * need to handle a number of cases, deps with computed and called rates,
 * deps with a delay (which creates a {@link HoldEvent}), deps with no
 * delay.  And ditto for arrivals.  So, that's 2x2x2 = 8 different cases. 
 * I am only doing two here.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: RunwayEventTest.java 1 2009-09-09 00:00:00EST donley $
 */
public class RunwayEventTest
{
	/**
	 * Create a bogus array of quarter hour acceptance rates.
	 * @return 2D array of rates, first element is the qtr hour
	 *   index measured w/r to some time, the first index being at
	 *   the quarter hour
	 */
	private int[][] createCalledRates(){
		int numQtrhour = 33*4; // num
		int[][] rates = new int[numQtrhour][2];
		for(int i=0;i<rates.length;i++){
			rates[i][Nas.Ad.DEP.ordinal()] = i;
			rates[i][Nas.Ad.ARR.ordinal()] = i+1;
		}
		return rates;
	}
	/**
	 * Creates a bogus pareto curve array for an airport.
	 * @return 3D pareto array. First element: MC condition, second element:
	 *   index of point on curve, third element: x or y index, x is DEP and
	 *   y is ARR, but it depends on the {@link Nas.Ad} enum.
	 *   Units: number per second.  I know it should be n/sec, but...
	 */
	private double[][][] createParetoCurves(){
		// As numbers per hour.
		double[][] paretoV = 
		{{0.,61.6},{35.9,60.0},{36.8,57.2},{41.9,50.4},{70.4,0.}};
		double[][] paretoI =
		{{0.,51.6},{25.9,50.0},{26.8,47.2},{31.9,40.4},{60.4,0.}};
		double[][] paretoM =
			{{0.,45.6},{20.9,44.0},{21.8,42.2},{26.9,35.4},{55.4,0.}};
		double[][][] pareto = new double[3][][];
		pareto[Nas.Mc.VMC.ordinal()] = paretoV;
		pareto[Nas.Mc.IMC.ordinal()] = paretoI;
		pareto[Nas.Mc.MVMC.ordinal()]= paretoM;
		// Convert to numbers per quarter hour.
		int qtrhrsPerHr = 4;
		for(int i=0;i<pareto.length;i++){
			for(int j=0;j<pareto[i].length;j++){
				for(int k=0;k<pareto[i][j].length;k++){
					pareto[i][j][k] /= qtrhrsPerHr;
				}
			}
		}
		return pareto;	
	}
	/**
	 * Creates bogus array of change capacities.
	 * @return 2D array of change capacities. First element: time of change
	 *   in seconds.  Second element: type of meteorological condition.
	 */
	private int[][] createChangeCapacities(){
		int[][] changeCap = {{0,Nas.Mc.VMC.ordinal()},
				{50,Nas.Mc.IMC.ordinal()},{700,Nas.Mc.MVMC.ordinal()},
				{1600,Nas.Mc.VMC.ordinal()}};
		return changeCap;
	}
	/**
	 * Creates a bogus array of estimated runway on and off times for flights
	 * coming into an airport.
	 * @return 
	 */
	private int[][] createEstOnNOffTimes(){
		int[][] times = new int[2][100];
		for(int j=0;j<times[0].length;j++){
			times[Nas.Ad.ARR.ordinal()][j] = 120*j;
			times[Nas.Ad.DEP.ordinal()][j] = 240*j;
		}
		return times;		
	}
	
	/**
	 * test of constructors, getters and setters.
	 */
	@Test
	public void testConstructorNGettersNSetters(){
		int aprt = 31;
		int[] carrier = {1,2};
		int startTime = 0;
		// Non-null input arrays.
		boolean computeRatesFlag = true;
		double[][][] pareto = createParetoCurves();
		int[][] calledRates = createCalledRates();
		int[][] estOnNOffTimes = createEstOnNOffTimes();
		int[][] changeCap = createChangeCapacities();
		
		Runway rw = new Runway(aprt,startTime,computeRatesFlag,
				calledRates,pareto,changeCap,estOnNOffTimes);
		assertTrue(rw != null);
		
		// Flight(s)
		int[] itinNum = {47,11};
		int[] legNum = {2,1};
		int[] calcOnTime = {220,313}; // calculate on time.
		int[] calcOffTime = {310,520};  // calculated out time.
		
		int[][] pars = new int[2][IFlight.Param.values().length];
		for(int i=0;i<2;i++){
			pars[i][IFlight.Param.ITIN_NUM.ordinal()]     = itinNum[i];
			pars[i][IFlight.Param.LEG_NUM.ordinal()]      = legNum[i];
			pars[i][IFlight.Param.CARRIER.ordinal()]      = carrier[i];
			pars[i][IFlight.Param.CALC_ON_TIME.ordinal()] = calcOnTime[i];
			pars[i][IFlight.Param.CALC_OFF_TIME.ordinal()]= calcOffTime[i];
		}
    	IFlight[] fs = new Flight[2];
    	for(int i=0;i<2;i++){
    		fs[i] = new Flight(pars[i]);
    	}
    	IEvent[] events = new RunwayEvent[2];
    	
    	//----------------------------------------------------------------------
    	// A departing and an arriving flight.
    	//----------------------------------------------------------------------
    	events[0] = new RunwayEvent(rw,fs[0],IEvent.Cmd.DEP,
    		fs[0].get(IFlight.Param.CALC_OFF_TIME));
    	events[1] = new RunwayEvent(rw,fs[1],IEvent.Cmd.ARR,
    		fs[1].get(IFlight.Param.CALC_ON_TIME));

    	// Test output and various getters.
    	// getTime
    	assertTrue(calcOffTime[0] == events[0].getTime());
    	assertTrue(calcOnTime[1] == events[1].getTime());
    	// getNode and getFlight
    	for(int i=0;i<events.length;i++){
			assertTrue(rw == events[i].getNode());
			assertTrue(fs[i] == ((RunwayEvent)events[i]).getFlight());
		}
    	// getMessage()
    	assertTrue(IEvent.Cmd.DEP == events[0].getMessage());
    	assertTrue(IEvent.Cmd.ARR == events[1].getMessage());
    	// getType()
    	for(IEvent eve : events){
    		assertTrue(IEvent.Type.RUNWAY == eve.getType());
    	}
    	// getFlight()
    	for(int i=0;i<fs.length;i++){
    		assertTrue(fs[i] == events[i].getFlight());
    	}
    	
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
    	
	}
	/**
	 * Test of exception in constructor.  Will accept only
	 * messages that are arr or dep.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testIllArgExceptionConstructor() {
		IEvent re = new RunwayEvent(null,null,IEvent.Cmd.BOGUS,0);
	}
	
	/**
	 * Test of processEvent().  Two tests. One for an arriving flight,
	 * one for a departing flight. 
	 */
	@Test
	public void testProcessEvent(){
		
		//----------------------------------------------------------------------
		// Departing flight. Using pareto.
		//----------------------------------------------------------------------
		// Have three flights that are dumped into the runway queue.  At
		// the end three FixEvent events should be in the main queue, spacing
		// in time determined by the pareto values.  Have them all go to the
		// same fix for convenience.
		int aprt = 31;
		int startTime = 0;
		// Non-null input arrays.
		boolean computeRatesFlag = true;
		double[][][] pareto = createParetoCurves();
		int[][] calledRates = createCalledRates();
		int[][] estOnNOffTimes = createEstOnNOffTimes();
		int[][] changeCap = createChangeCapacities();
		
		Runway rw = new Runway(aprt,startTime,computeRatesFlag,
				calledRates,pareto,changeCap,estOnNOffTimes);
		DummyNode dn = new DummyNode();
		assertTrue(rw != null);
		assertTrue(dn != null);
		
		// Flight(s)
		int nFlights = 3;
		int[] itinNum = {1,2,4};
		int[] legNum = {1,1,2};
		int[] carrier = {1,2,3};
		int[] calcOffTime = {300,410,330};  // calculated out time.
		
		int[][] pars = new int[nFlights][IFlight.Param.values().length];
		for(int i=0;i<nFlights;i++){
			pars[i][IFlight.Param.ITIN_NUM.ordinal()]     = itinNum[i];
			pars[i][IFlight.Param.LEG_NUM.ordinal()]      = legNum[i];
			pars[i][IFlight.Param.CARRIER.ordinal()]      = carrier[i];
			pars[i][IFlight.Param.CALC_OFF_TIME.ordinal()]= calcOffTime[i];
		}
    	IFlight[] fs = new Flight[nFlights];
    	for(int i=0;i<nFlights;i++){
    		fs[i] = new Flight(pars[i]);
    	}
    	// Create routes.  Flights go to a DummyNode.
    	List<INode> rList = null;
    	IRoute rt = null;
    	for(int i=0;i<nFlights;i++){
    		rList = new ArrayList<INode>();
    		rList.add(dn);
    		rt = new Route(rList);
    		fs[i].setRoute(rt);
    	}
    	
    	// Create runway events.
    	IEvent[] events = new RunwayEvent[nFlights];
    	for(int i=0;i<nFlights;i++){
	    	events[i] = new RunwayEvent(rw,fs[i],IEvent.Cmd.DEP,
	    		fs[i].get(IFlight.Param.CALC_OFF_TIME));
    	}
    	
    	// Check events (check again?  why not?)
    	for(int i=0;i<events.length;i++){
			assertTrue(calcOffTime[i] == events[i].getTime());
			assertTrue(rw == events[i].getNode());
			assertTrue(fs[i] == events[i].getFlight());
			assertTrue(IEvent.Cmd.DEP == events[i].getMessage());
		}
    	//Add events to queue.
    	IQueue<IEvent> queue = new EventQueue();
    	for(int i=0;i<events.length;i++){
    		queue.add(events[i]);
    	}
    	// Process events.  Dummy events do nothing. Flights go to DummyNode 
    	// objects.  Save dummy objects.
    	List<IEvent> dums = new ArrayList<IEvent>();
    	IEvent ev = null;
    	while((ev = queue.poll()) != null){
    		ev.processEvent(queue);
    		if(ev instanceof DummyEvent){
    			dums.add(ev);
    		}
    	}
    	
    	// The first flight should just be let go.  The other two should
    	// have their off times changed.
    	assertTrue(events[0].getFlight().get(IFlight.Param.ACT_OFF_TIME) ==
    		calcOffTime[0]);
    	assertTrue(events[2].getFlight().get(IFlight.Param.ACT_OFF_TIME) ==
    		calcOffTime[0]+150);
    	assertTrue(events[1].getFlight().get(IFlight.Param.ACT_OFF_TIME) ==
    		calcOffTime[0]+300);
    	
    	// Look at DummyNode events.
    	assertTrue(dums.size() == nFlights);
    	for(int i=0;i<dums.size();i++){
    		IEvent eve = dums.get(i);
    		assertTrue(eve instanceof DummyEvent);
			assertTrue(IEvent.Cmd.DEP == eve.getMessage());	
    		IFlight fl = eve.getFlight();
    		assertTrue(eve.getTime()==fl.get(IFlight.Param.ACT_OFF_TIME));
    		assertTrue(fl.getRoute().getLastNode() == eve.getNode());
    	}
    	
    	//---------------------------------------------------------------------
    	// Arriving flights.  Use Computed rates and have delay.
    	//---------------------------------------------------------------------
    	computeRatesFlag = false;
		rw = new Runway(aprt,startTime,computeRatesFlag,
				calledRates,pareto,changeCap,estOnNOffTimes);
		assertTrue(rw != null);
		
		// Flight(s)
		int[] actOnTime = {1400,1500,1420};
		for(int i=0;i<nFlights;i++){
			pars[i][IFlight.Param.ITIN_NUM.ordinal()]     = itinNum[i];
			pars[i][IFlight.Param.LEG_NUM.ordinal()]      = legNum[i];
			pars[i][IFlight.Param.CARRIER.ordinal()]      = carrier[i];
			pars[i][IFlight.Param.ACT_ON_TIME.ordinal()]  = actOnTime[i];
		}
    	fs = new Flight[nFlights];
    	for(int i=0;i<nFlights;i++){
    		fs[i] = new Flight(pars[i]);
    	}
    	// Create route.  For arrivals only need to have the Taxiway.
    	dn = new DummyNode();
    	for(int i=0;i<nFlights;i++){
    		rList = new ArrayList<INode>();
    		rList.add(dn);
    		rt = new Route(rList);
    		fs[i].setRoute(rt);
    	}
    	
    	// Create events.
    	events = new RunwayEvent[nFlights];
    	for(int i=0;i<events.length;i++){
    		events[i] = new RunwayEvent(rw,fs[i],IEvent.Cmd.ARR,
    			fs[i].get(IFlight.Param.ACT_ON_TIME));
    	}	
    	
    	// Check events (check again?  why not?)
    	for(int i=0;i<events.length;i++){
			assertTrue(actOnTime[i] == events[i].getTime());
			assertTrue(rw == events[i].getNode());
			assertTrue(fs[i] == events[i].getFlight());
			assertTrue(IEvent.Cmd.ARR == events[i].getMessage());
		}
    	//Add events to queue.
    	queue.clear();
    	for(int i=0;i<events.length;i++){
    		queue.add(events[i]);
    	}
    	// Process events.  Dummy events do nothing. Flights go to DummyNode 
    	// objects.  Save dummy objects.
    	dums = new ArrayList<IEvent>();
    	ev = null;
    	while((ev = queue.poll()) != null){
    		ev.processEvent(queue);
    		if(ev instanceof DummyEvent){
    			dums.add(ev);
    		}
    	}
    	
    	// The first flight should just be landed.  The other two should
    	// have their on times changed.
    	assertTrue(events[0].getFlight().get(IFlight.Param.ACT_ON_TIME) ==
    		actOnTime[0]);
    	assertTrue(events[2].getFlight().get(IFlight.Param.ACT_ON_TIME) ==
    		actOnTime[0]+450);
    	assertTrue(events[1].getFlight().get(IFlight.Param.ACT_ON_TIME) ==
    		actOnTime[0]+750);

    	while(queue.size() != 0){
    		IEvent eve = queue.poll();
    		assertTrue(eve instanceof DummyEvent);
			assertTrue(IEvent.Cmd.ARR == eve.getMessage());	
    		IFlight fl = eve.getFlight();
    		int tim = fl.get(IFlight.Param.ACT_ON_TIME); 
    		assertTrue(tim == eve.getTime());
    		assertTrue(tim == fl.get(IFlight.Param.ACT_ON_TIME));
    		assertTrue(fl.getRoute().getLastNode() == eve.getNode());
    	}
	}
	
}
