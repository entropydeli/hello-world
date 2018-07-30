package org.cna.donley.nassim2_4;

import static org.cna.donley.utils.Constants.EPS;

import java.util.List;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

/**
 * Test to validate the Runway class.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: RunwayTest.java 1 2009-09-03 00:00:00EST donley $
 */
public class RunwayTest
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
	 * test of constructor and getter and setters.
	 */
	@Test
	public void testConstructorNSettersNGetters(){
		int aprt = 23;
		int startTime = 0;
		
		// Check that if all arrays are null, then Runway still creates
		// a non-null object.
		Runway rw = new Runway(aprt,0,false,null,null,null,null);
		
		// Non-null input arrays.
		boolean computeRatesFlag = true;
		double[][][] pareto = createParetoCurves();
		int[][] calledRates = createCalledRates();
		int[][] estOnNOffTimes = createEstOnNOffTimes();
		int[][] changeCap = createChangeCapacities();
		
		rw = new Runway(aprt,startTime,computeRatesFlag,
				calledRates,pareto,changeCap,estOnNOffTimes);
		assertTrue(rw != null);
		// get aprt
		assertTrue(aprt == rw.getAirport());
		// get last indx changed used.
		assertTrue(0 == rw.getLastIndxChangeUsed());
		// get computeRates flag
		assertTrue(computeRatesFlag == rw.getComputeRatesFlag());
		// set and get last slot open time.
		rw.setLastTime(Nas.Ad.ARR,38);
		rw.setLastTime(Nas.Ad.DEP,56);
		assertTrue(38 == rw.getLastTime(Nas.Ad.ARR));
		assertTrue(56 == rw.getLastTime(Nas.Ad.DEP));
		// set and get next slot open time.
		rw.setNextTime(Nas.Ad.ARR,22);
		rw.setNextTime(Nas.Ad.DEP,25);
		assertTrue(22 == rw.getNextTime(Nas.Ad.ARR));
		assertTrue(25 == rw.getNextTime(Nas.Ad.DEP));
		
		// set and get neighbors.
		INode tw = new Taxiway(aprt,null,null);
		rw.setNeighbor(tw);
		tw.setNeighbor(rw);
		
		// Get data arrays.
		double[][][] par = rw.getParetoCurves();
		assertTrue(par != null);
		assertTrue(par == pareto);
		for(int i=0;i<pareto.length;i++){
			for(int j=0;j<pareto[i].length;j++){
				for(int k=0;k<pareto[i][j].length;k++){
					assertEquals(pareto[i][j][k],par[i][j][k],EPS);
				}
			}
		}
		int[][] changeOut = rw.getChangeCapacities();
		assertTrue(changeOut != null);
		assertTrue(changeOut == changeCap);
		for(int i=0;i<changeCap.length;i++){
			for(int j=0;j<changeCap[i].length;j++){
				assertTrue(changeCap[i][j] == changeOut[i][j]);
			}
		}
		int[][] cRates = rw.getCalledRates();
		assertTrue(cRates != null);
		assertTrue(cRates == calledRates);
		for(int i=0;i<cRates.length;i++){
			for(int j=0;j<cRates[i].length;j++){
				assertTrue(cRates[i][j] == calledRates[i][j]);
			}
		}
		int[][] estOOTimes = rw.getEstOnNOffTimes();
		assertTrue(estOOTimes != null);
		assertTrue(estOOTimes == estOnNOffTimes);
		for(int i=0;i<estOOTimes.length;i++){
			for(int j=0;j<estOOTimes[i].length;j++){
				assertTrue(estOOTimes[i][j] == estOnNOffTimes[i][j]);
			}
		}
			
	}
	
	/**
	 * Test of getQueue and getQueueHoldStatus.
	 */
	@Test
	public void testGetQueueNGetQueueHoldStatus(){
		int aprt = 23;
		
		// Check that if all arrays are null, then Runway still creates
		// a non-null object.
		Runway rw = new Runway(aprt,0,false,null,null,null,null);
		
		// Queue status and getQueue.
		assertTrue(rw.getQueue(Nas.Ad.DEP) != null);
		assertTrue(rw.getQueue(Nas.Ad.ARR) != null);
		assertTrue(rw.getQueueHoldStatus(Nas.Ad.DEP) == false);
		assertTrue(rw.getQueueHoldStatus(Nas.Ad.ARR) == false);
		rw.setQueueHoldStatus(Nas.Ad.DEP,true);
		rw.setQueueHoldStatus(Nas.Ad.ARR,true);
		assertTrue(rw.getQueueHoldStatus(Nas.Ad.DEP) == true);
		assertTrue(rw.getQueueHoldStatus(Nas.Ad.ARR) == true);
		
		// Flight(s)
		int[] itinNum = {47,11};
		int[] legNum = {2,1};
		int[] actOnTime = {420,313};    // actual on time.
		int[] calcOffTime = {310,520};  // calculated out time.
		
		int[][] pars = new int[2][IFlight.Param.values().length];
		for(int i=0;i<2;i++){
			pars[i][IFlight.Param.ITIN_NUM.ordinal()]     = itinNum[i];
			pars[i][IFlight.Param.LEG_NUM.ordinal()]      = legNum[i];
			pars[i][IFlight.Param.CARRIER.ordinal()]      = i;
			pars[i][IFlight.Param.ACT_ON_TIME.ordinal()]  = actOnTime[i];
			pars[i][IFlight.Param.CALC_OFF_TIME.ordinal()]= calcOffTime[i];
		}
		IFlight[] fs = new Flight[2];
		IQueue<IFlight> qD = rw.getQueue(Nas.Ad.DEP);
		IQueue<IFlight> qA = rw.getQueue(Nas.Ad.ARR);
		for(int i=0;i<2;i++){
			fs[i] = new Flight(pars[i]);
			qD.add(fs[i]);
			qA.add(fs[i]);
		}
		// Are the flights ordered properly in the queues?
		assertTrue(qD.poll() == fs[0]);
		assertTrue(qD.poll() == fs[1]);	
		assertTrue(qA.poll() == fs[1]);
		assertTrue(qA.poll() == fs[0]);		
		
		// Measures correct?
		assertTrue(rw.getMeasure(Nas.Ad.DEP)==IFlight.Param.CALC_OFF_TIME);
		assertTrue(rw.getMeasure(Nas.Ad.ARR)==IFlight.Param.ACT_ON_TIME);
		
	}
	/**
	 * Test of exception in setNeighbor.  Will accept only
	 * Nodes that are taxiway.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testIllArgExceptionSetNeighbor() {
		INode rw = new Runway(1,0,false,null,null,null,null);
		INode tw = new Terminal(1,null,null);
		rw.setNeighbor(tw);
	}
	
	/**
	 * test of receive method.  Holder for when it is actually used.
	 */
	public void testReceive(){
		INode rw = new Runway(1,0,false,null,null,null,null);
		assertTrue(false == rw.receive(null,null));
	}
	
	/**
	 * Test of computeCurrentWeatherConditions.
	 */
	@Test
	public void testComputeCurrentWeatherConditions(){
		int aprt = 23;
		int[][] changeCap = {{0,Nas.Mc.VMC.ordinal()},
				{5,Nas.Mc.IMC.ordinal()},{22,Nas.Mc.MVMC.ordinal()},
				{37,Nas.Mc.IMC.ordinal()}};
		boolean computeRatesFlag = true;
		
		Runway rw = 
			new Runway(aprt,0,computeRatesFlag,null,null,changeCap,null);
		assertTrue(rw != null);
		
		// Test before the first change, somewhere in between and after the last
		// Note that the lasIndxChangeUsed marker is updated after every call
		// so one needs to order the calling times properly.
		int time = 1;
		int mc = rw.computeCurrentWeatherCondition(time);
		assertTrue(mc == Nas.Mc.VMC.ordinal());
		time = 24;
		mc = rw.computeCurrentWeatherCondition(time);
		assertTrue(mc == Nas.Mc.MVMC.ordinal());
		time = 1000;
		mc = rw.computeCurrentWeatherCondition(time);
		assertTrue(mc == Nas.Mc.IMC.ordinal());	
	}
	/**
	 * Test of computeNumWantDepNArrNextQtrhour.
	 */
	@Test
	public void testComputeNumWantDepNArrNextQtrhour(){
		int[][] estTimes = new int[2][100];
		for(int j=0;j<estTimes[0].length;j++){
			estTimes[Nas.Ad.ARR.ordinal()][j] = 120*j;
			estTimes[Nas.Ad.DEP.ordinal()][j] = 240*j;
		}
		
		int time = 60;
		int[]num = Runway.computeNumWantDepNArrNextQtrhour(time,estTimes);
		assertTrue(num[Nas.Ad.DEP.ordinal()] == 3);
		assertTrue(num[Nas.Ad.ARR.ordinal()] == 7);
		
		time = 622;
		num = Runway.computeNumWantDepNArrNextQtrhour(time,estTimes);
		assertTrue(num[Nas.Ad.DEP.ordinal()] == 4);
		assertTrue(num[Nas.Ad.ARR.ordinal()] == 7);	
	}
	/**
	 * test of computeOptimalAcceptRatesPerQtrhour. 
	 */
	@Test
	public void testComputeOptimalAcceptRatesPerQtrhour(){
		// These numbers are all per hour, but it doesn't matter because,
		// well, they are just numbers.
		double[][] pareto = 
			{{0.,61.6},{35.9,60.0},{36.8,57.2},{41.9,50.4},{70.4,0.}};
		int numArr = 73;
		int numDep = 45;
		double[] rates = new double[2];
		rates = Runway.computeOptimalAcceptRatesPerQtrhour(numDep,numArr,pareto);
		// Good rates should have a slope close numArr/numDep.
		assertEquals(rates[1]/rates[0],numArr/(double)numDep,5.e-2);
		assertEquals(rates[0],36.35,1.e-5);
		assertEquals(rates[1],58.60,1.e-5);
		
		numArr = 0;
		numDep = 80;
		rates = Runway.computeOptimalAcceptRatesPerQtrhour(numDep,numArr,pareto);
		assertEquals(rates[0],70.4,1.e-5);
		assertEquals(rates[1],0.0,1.e-5);
		
		numArr = 80;
		numDep = 0;
		rates = Runway.computeOptimalAcceptRatesPerQtrhour(numDep,numArr,pareto);
		assertEquals(rates[0],0.0,1.e-5);
		assertEquals(rates[1],61.6,1.e-5);
		
		numArr = 0;
		numDep = 0;
		rates = Runway.computeOptimalAcceptRatesPerQtrhour(numDep,numArr,pareto);
		assertEquals(rates[0],47.6,1.e-5);
		assertEquals(rates[1],40.32,1.e-5);
		
		// Special case: the pareto curve has a perfectly horizontal or vertical
		// segment.  In this case will choose the point that gives the
		// total max number of sched arrival and departures.
		double[][] pareto2 = {{0.,10.0},{5.0,10.0},{5.0,0.0}};
		numArr = 20;
		numDep = 20;
		rates = Runway.computeOptimalAcceptRatesPerQtrhour(numDep,numArr,pareto2);
		assertEquals(rates[0],5.0,1.e-5);
		assertEquals(rates[1],10.0,1.e-5);
		
		numArr = 30;
		numDep = 10;
		rates = Runway.computeOptimalAcceptRatesPerQtrhour(numDep,numArr,pareto2);
		assertEquals(rates[0],5.0,1.e-5);
		assertEquals(rates[1],10.0,1.e-5);
		
	
	}
	
	/**
	 * test of computeOptimalAcceptRatesPerQtrhourSimple.
	 */
	@Test
	public void testComputeOptimalAcceptRatesPerQtrhourSimple(){
		// These numbers are all per hour, but it doesn't matter because,
		// well, they are just numbers.
		double[][] pareto = 
			{{0.,61.6},{35.9,60.0},{36.8,57.2},{41.9,50.4},{70.4,0.}};
		int numArr = 73;
		int numDep = 45;
		double[] rates = new double[2];
		rates = Runway.computeOptimalAcceptRatesPerQtrhourSimple(numDep,numArr,pareto);
		// Good rates should have a slope close numArr/numDep.
		assertTrue(rates[1]/rates[0] >= numArr/(double)numDep);
		for(int i=0;i<rates.length;i++){
			assertEquals(rates[i],pareto[1][i],1.e-5);
		}
		
		numArr = 0;
		numDep = 80;
		rates = Runway.computeOptimalAcceptRatesPerQtrhourSimple(numDep,numArr,pareto);
		for(int i=0;i<rates.length;i++){
			assertEquals(rates[i],pareto[3][i],1.e-5);
		}
		
		numArr = 80;
		numDep = 0;
		rates = Runway.computeOptimalAcceptRatesPerQtrhourSimple(numDep,numArr,pareto);
		for(int i=0;i<rates.length;i++){
			assertEquals(rates[i],pareto[1][i],1.e-5);
		}
		
		numArr = 0;
		numDep = 0;
		rates = Runway.computeOptimalAcceptRatesPerQtrhourSimple(numDep,numArr,pareto);
		for(int i=0;i<rates.length;i++){
			assertEquals(rates[i],pareto[3][i],1.e-5);
		}
		
		// Special case: the pareto curve has a perfectly horizontal or vertical
		// segment.  In this case will choose the point that gives the
		// total max number of sched arrival and departures.
		double[][] pareto2 = {{0.,10.0},{5.0,10.0},{5.0,0.0}};
		numArr = 20;
		numDep = 20;
		rates = Runway.computeOptimalAcceptRatesPerQtrhourSimple(numDep,numArr,pareto2);
		for(int i=0;i<rates.length;i++){
			assertEquals(rates[i],pareto2[1][i],1.e-5);
		}
		
		numArr = 30;
		numDep = 10;
		rates = Runway.computeOptimalAcceptRatesPerQtrhourSimple(numDep,numArr,pareto2);
		for(int i=0;i<rates.length;i++){
			assertEquals(rates[i],pareto2[1][i],1.e-5);
		}
		
	
	}
	/**
	 * test of computeAcceptRaterPerQtrhour.
	 */
	@Test
	public void testComputeAcceptRatesPerQtrhour(){
		int aprt = 23;
		int startTime =  0;
		// Non-null input arrays.
		boolean computeRatesFlag = true;
		double[][][] pareto = createParetoCurves();
		int[][] estOnNOffTimes = createEstOnNOffTimes();
		int[][] changeCap = createChangeCapacities();
		
		Runway rw = new Runway(aprt,startTime,computeRatesFlag,
				null,pareto,changeCap,estOnNOffTimes);
		assertTrue(rw != null);
		
		int time = 60;
		int[] rates = rw.computeAcceptRatesPerQtrhour(time);
		assertTrue(rates!=null);
		assertEquals(rates[Nas.Ad.DEP.ordinal()],6,EPS);
		assertEquals(rates[Nas.Ad.ARR.ordinal()],13,EPS);
		
		time = 800;
		rates = rw.computeAcceptRatesPerQtrhour(time);
		assertTrue(rates != null);
		assertEquals(rates[Nas.Ad.DEP.ordinal()],5,1.e-4);
		assertEquals(rates[Nas.Ad.ARR.ordinal()],11,EPS);
		
	}
	
	/**
	 *  Test of computeAcceptTimeDiffsFromPareto().
	 */
	@Test
	public void testComputeAcceptTimeDiffsFromPareto(){
		int aprt = 23;
		int startTime =  0;
		// Non-null input arrays.
		boolean computeRatesFlag = true;
		double[][][] pareto = createParetoCurves();
		int[][] estOnNOffTimes = createEstOnNOffTimes();
		int[][] changeCap = createChangeCapacities();
		
		Runway rw = new Runway(aprt,startTime,computeRatesFlag,
				null,pareto,changeCap,estOnNOffTimes);
		assertTrue(rw != null);
		
		int qtrhourInSecs = 15*60;
		int time = 60;
		int[] minTimeDiffs = null;
		minTimeDiffs = rw.computeAcceptTimeDiffsFromPareto(time);
		assertTrue(minTimeDiffs != null);
		assertTrue(minTimeDiffs[Nas.Ad.DEP.ordinal()]==qtrhourInSecs/6);
		assertTrue(minTimeDiffs[Nas.Ad.ARR.ordinal()]==qtrhourInSecs/13);
		
		time = 800;
		minTimeDiffs = rw.computeAcceptTimeDiffsFromPareto(time);
		assertTrue(minTimeDiffs != null);
		assertTrue(minTimeDiffs[Nas.Ad.DEP.ordinal()]==qtrhourInSecs/5);
		assertTrue(minTimeDiffs[Nas.Ad.ARR.ordinal()]==qtrhourInSecs/11);
		
		// Now the case in which any of the arrays are null, so it is 
		// impossible to compute it.  In that case, time diff should be 
		// effectively infinite.
		rw = new Runway(aprt,startTime,computeRatesFlag,null,null,null,null);
		assertTrue(rw != null);
		
		time = 60;
		minTimeDiffs = rw.computeAcceptTimeDiffsFromPareto(time);
		assertTrue(minTimeDiffs != null);
		assertEquals(minTimeDiffs[Nas.Ad.DEP.ordinal()],0.,EPS);
		assertEquals(minTimeDiffs[Nas.Ad.ARR.ordinal()],0.,EPS);
	
	}
	
	/**
	 * Test of computeAcceptTimesDiffsFromCalledRates.
	 */
	@Test
	public void testComputeAcceptTimeDiffsFromCalledRates(){
		int aprt = 23;
		int startTime =  0;
		// Non-null input arrays.
		boolean computeRatesFlag = false;
		int[][] calledRates = createCalledRates();
		
		Runway rw = new Runway(aprt,startTime,computeRatesFlag,
				calledRates,null,null,null);
		assertTrue(rw != null);
	 	
	 	int time = 60;
	 	int[] minTimeDiffs = null;
		minTimeDiffs = rw.computeAcceptTimeDiffsFromCalledRates(time);
		assertTrue(minTimeDiffs != null);
		assertTrue(minTimeDiffs[Nas.Ad.DEP.ordinal()]==Integer.MAX_VALUE);
		assertTrue(minTimeDiffs[Nas.Ad.ARR.ordinal()]==900);
		
		time = 3000;
		minTimeDiffs = rw.computeAcceptTimeDiffsFromCalledRates(time);
		assertTrue(minTimeDiffs != null);
		assertTrue(minTimeDiffs[Nas.Ad.DEP.ordinal()]==300);
		assertTrue(minTimeDiffs[Nas.Ad.ARR.ordinal()]==225);
		
		// Case for which the time is beyond that for which have data.  Then
		// should use data from the last time.
		time = 60*60*100; // 100 hours.
		minTimeDiffs = rw.computeAcceptTimeDiffsFromCalledRates(time);
		assertTrue(minTimeDiffs != null);
		int qtrhourInSecs = 15*60;
		int[] tims = {qtrhourInSecs/(33*4-1),qtrhourInSecs/(33*4)};
		assertTrue(minTimeDiffs[Nas.Ad.DEP.ordinal()]==tims[0]);
		assertTrue(minTimeDiffs[Nas.Ad.ARR.ordinal()]==tims[1]);
		
		// Now the case in which any of the arrays are null, so it is 
		// impossible to compute it.  In that case, time diff should be 
		// 0.
		rw = new Runway(aprt,startTime,computeRatesFlag,null,null,null,null);
		assertTrue(rw != null);
		
		time = 60;
		minTimeDiffs = rw.computeAcceptTimeDiffsFromCalledRates(time);
		assertTrue(minTimeDiffs != null);
		assertEquals(minTimeDiffs[Nas.Ad.DEP.ordinal()],0.,EPS);
		assertEquals(minTimeDiffs[Nas.Ad.ARR.ordinal()],0.,EPS);
		
		// Now the case in which the called rates are zero to the end. 
		// Then, if the time wanted is passed the last data point, then
		// the minTimeDiff will be a week.
		for(int i=0;i<calledRates.length;i++){
			for(int j=0;j<calledRates[i].length;j++){
				calledRates[i][j] = 0;
			}
		}
		rw = new Runway(aprt,startTime,computeRatesFlag,calledRates,null,null,null);
		assertTrue(rw != null);
		int oneWeek = 7*24*60*60;
		time = 2*24*60*60;
		minTimeDiffs = rw.computeAcceptTimeDiffsFromCalledRates(time);
		assertTrue(minTimeDiffs != null);
		assertTrue(minTimeDiffs[Nas.Ad.DEP.ordinal()] == oneWeek);
		assertTrue(minTimeDiffs[Nas.Ad.ARR.ordinal()] == oneWeek);
		
		
		
	}
	/**
	 * Test of computeNextTime().
	 */
	@Test
	public void testComputeNextTime(){
		boolean success = false;
		int time = 0;
		int qtrhourInSecs = 15*60;
		
		//--------------------------------------------------------
		// Called rates.
		//--------------------------------------------------------
		int aprt = 23;
		int startTime =  0;
		// Non-null input arrays.
		boolean computeRatesFlag = false;
		int[][] calledRates = createCalledRates();
		
		Runway rw = new Runway(aprt,startTime,computeRatesFlag,
				calledRates,null,null,null);
		assertTrue(rw != null);
		
		time = 60;
		success = rw.computeNextTime(Nas.Ad.DEP, time);
		assertTrue(success);
		assertTrue(1800 == rw.getNextTime(Nas.Ad.DEP));
		
		time = 900;
		success = rw.computeNextTime(Nas.Ad.DEP, time);
		assertTrue(success);
		assertTrue(1800 == rw.getNextTime(Nas.Ad.DEP));	
		
		time = 1000;
		success = rw.computeNextTime(Nas.Ad.DEP, time);
		assertTrue(success);
		assertTrue(1900 == rw.getNextTime(Nas.Ad.DEP));	
		
		time = 450;
		success = rw.computeNextTime(Nas.Ad.ARR, time);
		assertTrue(success);
		assertTrue(1350 == rw.getNextTime(Nas.Ad.ARR));	
		
		//---------------------------------------------------------------
		// Pareto curves.
		//---------------------------------------------------------------
		computeRatesFlag = true;
		double[][][] pareto = createParetoCurves();
		int[][] estOnNOffTimes = createEstOnNOffTimes();
		int[][] changeCap = createChangeCapacities();
		
		rw = new Runway(aprt,startTime,computeRatesFlag,
				null,pareto,changeCap,estOnNOffTimes);
		assertTrue(rw != null);
		
		time = 60;
		success = rw.computeNextTime(Nas.Ad.DEP, time);
		assertTrue(success);
		assertTrue(210 == rw.getNextTime(Nas.Ad.DEP));
		
		time = 1560;
		success = rw.computeNextTime(Nas.Ad.ARR, time);
		assertTrue(success);
		assertTrue(1641 == rw.getNextTime(Nas.Ad.ARR));
		
	}
	
	/**
	 * Test of computeAcceptRatesPerTimestep. Notice that the input qtr hour
	 * rates are really integers.  The algorithm is such is that if the rates
	 * are integers, one should always get that number by summing all the
	 * counts from the bins for each timestep.  If they aren't integers,
	 * the number is sometimes one less, depending on the value of the 
	 * remainder.
	 */
/*
	@Test
	public void testComputeAcceptRatesPerTimestep(){
		double[] rates = {41.0, 53.0};
		int numTimesteps = 1;
		int[][] ratesT = null;
		int[][] ratesTAct = {{41,53}};
		ratesT = 
			Runway.computeAcceptRatesPerTimestep(rates,numTimesteps);
		for(int i=0;i<numTimesteps;i++){
			assertTrue(ratesT[i][0] == ratesTAct[i][0]);
			assertTrue(ratesT[i][1] == ratesTAct[i][1]);
		}
		
		numTimesteps = 2;
		int[][] ratesTAct2 = {{21,27},{20,26}};
		ratesT = 
			Runway.computeAcceptRatesPerTimestep(rates,numTimesteps);
		for(int i=0;i<numTimesteps;i++){
			assertTrue(ratesT[i][0] == ratesTAct2[i][0]);
			assertTrue(ratesT[i][1] == ratesTAct2[i][1]);
		}
		
		numTimesteps = 4;
		int[][] ratesTAct4 = {{11,14},{10,13},{10,13},{10,13}};
		ratesT = 
			Runway.computeAcceptRatesPerTimestep(rates,numTimesteps);
		for(int i=0;i<numTimesteps;i++){
			assertTrue(ratesT[i][0] == ratesTAct4[i][0]);
			assertTrue(ratesT[i][1] == ratesTAct4[i][1]);
		}
		
		numTimesteps = 9;
		int[][] ratesTAct9 = {{5,6},{4,6},{5,6},{4,6},{5,6},{4,6},{5,6},{5,6},{4,5}};
		ratesT = 
			Runway.computeAcceptRatesPerTimestep(rates,numTimesteps);
		int[] sum = new int[2];
		for(int i=0;i<numTimesteps;i++){
			sum[0] += ratesT[i][0];
			sum[1] += ratesT[i][1];
		}
		assertTrue(sum[0] == 41);
		assertTrue(sum[1] == 53);
		for(int i=0;i<numTimesteps;i++){
			assertTrue(ratesT[i][0] == ratesTAct9[i][0]);
			assertTrue(ratesT[i][1] == ratesTAct9[i][1]);
		}
		
		numTimesteps = 15;
		int[][] ratesTAct15 = {{3,4},{3,3},{3,4},{2,3},{3,4},{3,3},{3,4},{2,3},{3,4},
				{3,3},{3,4},{2,3},{3,4},{3,4},{2,3}};
		ratesT = 
			Runway.computeAcceptRatesPerTimestep(rates,numTimesteps);
		sum[0] = 0;
		sum[1] = 0;
		for(int i=0;i<numTimesteps;i++){
			sum[0] += ratesT[i][0];
			sum[1] += ratesT[i][1];
		}
		assertTrue(sum[0] == 41);
		assertTrue(sum[1] == 53);
		for(int i=0;i<numTimesteps;i++){
			assertTrue(ratesT[i][0] == ratesTAct15[i][0]);
			assertTrue(ratesT[i][1] == ratesTAct15[i][1]);
		}
		
		// Max number of timesteps in a quarter hour.
		numTimesteps = 15*60;
		ratesT = 
			Runway.computeAcceptRatesPerTimestep(rates,numTimesteps);
		sum[0] = 0;
		sum[1] = 0;
		for(int i=0;i<numTimesteps;i++){
			sum[0] += ratesT[i][0];
			sum[1] += ratesT[i][1];
		}
		assertTrue(sum[0] == 41);
		assertTrue(sum[1] == 53);
	}
*/
	
}
