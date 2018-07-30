package org.cna.donley.nassim2_4;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Test to validate the Terminal class.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: TerminalTest.java 1 2009-09-01 00:00:00EST donley $
 */
public class TerminalTest
{
	/**
	 * test of constructors, getters and setters.
	 */
	@Test
	public void testConstructorNGettersNSetters(){
		int aprt = 2;
		
		// Null turn and pushback times.
		Terminal t1 = new Terminal(aprt,null,null);
		assertTrue(t1 != null);
		// Check getAirport().
		assertTrue(aprt == t1.getAirport());
		// Set and get Neighbors.
		INode taxiway = new Taxiway(1,null,null);
		t1.setNeighbor(taxiway);
		INode[] nn = t1.getNeighbors();
		for(INode node : nn){
			if(node instanceof Taxiway){
				assertTrue(node == taxiway);
			} else assertFalse(true);
		}
		assertTrue(null == t1.getTurnTimes());
		assertTrue(null == t1.getPushbackTimes());
		
		// Non-null turn and pushback times.
		int numCarriers = 2;
		int numEquipTypes = 2;
		int [][][]turnTimes = new int[numEquipTypes][numCarriers][2];
		int [][][]pushbackTimes = new int[numCarriers][numEquipTypes][2];
		for(int i=0;i<numCarriers;i++){
			turnTimes[i][0][Nas.Distrib.MEAN.ordinal()] = 7;
			turnTimes[i][0][Nas.Distrib.STDDEV.ordinal()] = 2;
			turnTimes[i][1][Nas.Distrib.MEAN.ordinal()] = 12;
			turnTimes[i][1][Nas.Distrib.STDDEV.ordinal()] = 3;
			pushbackTimes[i][0][Nas.Distrib.MEAN.ordinal()] = 8;
			pushbackTimes[i][0][Nas.Distrib.STDDEV.ordinal()] = 3;
			pushbackTimes[i][1][Nas.Distrib.MEAN.ordinal()] = 22;
			pushbackTimes[i][1][Nas.Distrib.STDDEV.ordinal()] = 6;
		}
		// Null turn and pushback times.
		t1 = new Terminal(aprt,turnTimes,pushbackTimes);
		assertTrue(t1 != null);
		int[][][] tTimes = t1.getTurnTimes();
		int[][][] pTimes = t1.getPushbackTimes();
		for(int i=0;i<numCarriers;i++){
			for(int j=0;j<numEquipTypes;j++){
				for(int k=0;k<2;k++){
					assertTrue(tTimes[i][j][k] == turnTimes[i][j][k]);
					assertTrue(pTimes[i][j][k] == pushbackTimes[i][j][k]);
				}
			}
		}
	}
	/**
	 * Test of exception in setNeighbor().  Will accept only
	 * Taxiway objects.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testIllArgExceptionSetNeighbor() {
		INode t1 = new Terminal(1,null,null);
		INode r1 = new Runway(1,0,false,null,null,null,null);
		t1.setNeighbor(r1);	
	}
	/**
	 * Test of createEventsForDepartingFlights()
	 */
	@Test
	public void testCreateEventsForDepartingFlights(){
		int aprt = 1;
		Terminal tn = new Terminal(aprt,null,null);
		int[] itinNum = {21,2};
		int[] legNum = {1,2};
		int[] schedOutTime = {1500,600};
		int[] pushbackTime = {-24,-100};
		
		
		int[][] pars = new int[2][IFlight.Param.values().length];
		for(int i=0;i<2;i++){
			pars[i][IFlight.Param.ITIN_NUM.ordinal()]= itinNum[i];
			pars[i][IFlight.Param.LEG_NUM.ordinal()] = legNum[i];
			pars[i][IFlight.Param.SCHED_OUT_TIME.ordinal()]= schedOutTime[i];
			pars[i][IFlight.Param.PUSHBACK_TIME.ordinal()] = pushbackTime[i];
		}
    	IFlight[] fs = new Flight[2];
    	for(int i=0;i<2;i++){
    		fs[i] = new Flight(pars[i]);
    	}
 
		IEvent[] events = tn.createEventsForDepartingFlights(fs);
		assertTrue(events !=  null);
		for(int i=0;i<events.length;i++){
			assertTrue(schedOutTime[i]+pushbackTime[i] == events[i].getTime());
			assertTrue(tn == ((TerminalEvent)events[i]).getNode());
			assertTrue(fs[i] == ((TerminalEvent)events[i]).getFlight());
			assertTrue(IEvent.Cmd.DEP ==
				((TerminalEvent)events[i]).getMessage());
		}
		// Check that if flights is null, then events is null also.
		events = tn.createEventsForDepartingFlights(null);
		assertTrue(events == null);
	}
	/**
	 * test of computeTurnTime and computePushbackTime.
	 */
	@Test
	public void testComputeTurnNPushbackTime(){
		int aprt[] = {32,45};
		Terminal tn = new Terminal(aprt[0],null,null);
		int[] itinNum = {21,2};
		int[] legNum = {1,2};
		int[] schedOutTime = {15,6};
		int[] turnTime = {320,-20};
		int[] pushTime = {-23,41};
		
		int[][] pars = new int[2][IFlight.Param.values().length];
		for(int i=0;i<2;i++){
			pars[i][IFlight.Param.ITIN_NUM.ordinal()] = itinNum[i];
			pars[i][IFlight.Param.LEG_NUM.ordinal()] = legNum[i];
			pars[i][IFlight.Param.DEP_APRT.ordinal()] = aprt[0];
			pars[i][IFlight.Param.ARR_APRT.ordinal()] = aprt[1];
			pars[i][IFlight.Param.SCHED_OUT_TIME.ordinal()] = schedOutTime[i];
			pars[i][IFlight.Param.TURN_TIME.ordinal()] = turnTime[i];
			pars[i][IFlight.Param.PUSHBACK_TIME.ordinal()] = pushTime[i];
		}
    	IFlight[] fs = new Flight[2];
    	for(int i=0;i<2;i++){
    		fs[i] = new Flight(pars[i]);
    	}
    	assertTrue(turnTime[0] == tn.computeTurnTime(fs[0]));
    	assertTrue(0 == tn.computeTurnTime(fs[1]));
    	for(int i=0;i<fs.length;i++){
    		assertTrue(pushTime[i] == tn.computePushbackTime(fs[i]));
    	}
	}
	/**
	 * Test of exception in computTurnTime(). Flight arrival or departure
	 * airport must be the same as the terminal's.
	 */
/*
	@Test(expected=IllegalArgumentException.class)
	public void testIllArgExceptionComputeTurnTime() {
		int aprt = 32;
		int[] aprtWrong = {11,12};
		Terminal tn = new Terminal(aprt,null,null);
		int[] turnTime = {320,451};
		
		int[][] pars = new int[2][IFlight.Param.values().length];
		for(int i=0;i<2;i++){
			pars[i][IFlight.Param.DEP_APRT.ordinal()] = aprtWrong[0];
			pars[i][IFlight.Param.ARR_APRT.ordinal()] = aprtWrong[1];
		}
    	IFlight[] fs = new Flight[2];
    	for(int i=0;i<2;i++){
    		fs[i] = new Flight(pars[i]);
    	}
    	for(int i=0;i<fs.length;i++){
    		assertTrue(turnTime[i] == tn.computeTurnTime(fs[i]));
    	}
	}
*/
	
}
