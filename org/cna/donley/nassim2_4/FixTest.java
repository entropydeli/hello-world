package org.cna.donley.nassim2_4;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

/**
 * Test to validate the Fix class.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: FixTest.java 1 2009-11-04 00:00:00EST donley $
 */
public class FixTest
{
	/**
	 * test of constructor and getter and setters, and computeNextTime().
	 */
	@Test
	public void testOne(){
		int name = 23;
		Nas.Ad type = Nas.Ad.ARR;
		int minTimeSpacing = 1030;
		
		Fix f = new Fix(name,type,minTimeSpacing);
		assertTrue(f != null);
		
		assertTrue(name == f.getName());
		assertTrue(type == f.getFixType());
		assertTrue(minTimeSpacing == f.getMinTimeSpacing());
		assertTrue(0 == f.getNextTime());
		
		int time = 143;
		f.computeNextTime(time);
		assertTrue(time + minTimeSpacing == f.getNextTime());
		assertTrue(time == f.getLastTime());
		
		time = 333;
		f.computeNextTime(time);
		assertTrue(time + minTimeSpacing == f.getNextTime());
		assertTrue(time == f.getLastTime());	
	}
	/**
	 * Test of getQueue and getQueueHoldStatus.
	 */
	@Test
	public void testGetQueueNGetQueueHoldStatus(){
		int aprt = 23;
		Nas.Ad type = Nas.Ad.ARR;
		int minTimeSpacing = 1030;
		
		// Check that if all arrays are null, then Runway still creates
		// a non-null object.
		Fix fix = new Fix(aprt,type,minTimeSpacing);
		
		// Queue status and getQueue.
		assertTrue(fix.getQueue() != null);
		assertTrue(fix.getQueueHoldStatus() == false);
		fix.setQueueHoldStatus(true);
		assertTrue(fix.getQueueHoldStatus() == true);
		
		// Flight(s)
		int[] itinNum = {47,11};
		int[] legNum = {2,1};
		int[] actOffTime = {420,313};    // actual on time.
		int[] calcOnTime = {1510,1320};  // calculated out time.
		
		int[][] pars = new int[2][IFlight.Param.values().length];
		for(int i=0;i<2;i++){
			pars[i][IFlight.Param.ITIN_NUM.ordinal()]     = itinNum[i];
			pars[i][IFlight.Param.LEG_NUM.ordinal()]      = legNum[i];
			pars[i][IFlight.Param.CARRIER.ordinal()]      = i;
			pars[i][IFlight.Param.ACT_OFF_TIME.ordinal()] = actOffTime[i];
			pars[i][IFlight.Param.CALC_ON_TIME.ordinal()] = calcOnTime[i];
		}
		IFlight[] fs = new Flight[2];
		IQueue<IFlight> qA = fix.getQueue();
		for(int i=0;i<2;i++){
			fs[i] = new Flight(pars[i]);
			qA.add(fs[i]);
		}
		// Are the flights ordered properly in the queues?
		assertTrue(qA.poll() == fs[1]);
		assertTrue(qA.poll() == fs[0]);		
		
		// Measures correct?
		assertTrue(fix.getMeasure()==IFlight.Param.CALC_ON_TIME);
		
		// Again
		type = Nas.Ad.DEP;
		fix = new Fix(aprt,type,minTimeSpacing);
		assertTrue(fix.getMeasure()==IFlight.Param.ACT_OFF_TIME);
		
	}
	
	/**
	 * test of receive method.  Holder for when it is actually used.
	 */
	public void testReceive(){
		INode fix = new Fix(1,Nas.Ad.DEP,23);
		assertTrue(false == fix.receive(null,null));
	}
	
}
