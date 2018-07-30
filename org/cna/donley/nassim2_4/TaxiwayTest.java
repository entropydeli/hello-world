package org.cna.donley.nassim2_4;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Test to validate the Taxiway class.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: TaxiwayTest.java 1 2009-09-02 00:00:00EST donley $
 */
public class TaxiwayTest
{
	/**
	 * test of constructors, getters and setters.
	 */
	@Test
	public void testConstructorsNGettersNSetters(){
		int aprt = 2;
		int numCarriers = 2;
		int numEquipTypes = 2;
		int [][][]taxiInTimes = new int[numCarriers][numEquipTypes][2];
		int [][][]taxiOutTimes = new int[numCarriers][numEquipTypes][2];
		for(int i=0;i<numCarriers;i++){
			taxiInTimes[i][0][Nas.Distrib.MEAN.ordinal()] = 7;
			taxiInTimes[i][0][Nas.Distrib.STDDEV.ordinal()] = 2;
			taxiInTimes[i][1][Nas.Distrib.MEAN.ordinal()] = 12;
			taxiInTimes[i][1][Nas.Distrib.STDDEV.ordinal()] = 3;
			taxiOutTimes[i][0][Nas.Distrib.MEAN.ordinal()] = 8;
			taxiOutTimes[i][0][Nas.Distrib.STDDEV.ordinal()] = 3;
			taxiOutTimes[i][1][Nas.Distrib.MEAN.ordinal()] = 22;
			taxiOutTimes[i][1][Nas.Distrib.STDDEV.ordinal()] = 6;
		}
		// Constructor.
		// Taxi times are null.
		Taxiway t1 = new Taxiway(aprt,null,null);
		assertTrue(t1 != null);
		assertTrue(aprt == t1.getAirport());
		assertTrue(null == t1.getTaxiInTimes());
		// Taxi times are not null.
		t1 = new Taxiway(aprt,taxiInTimes,taxiOutTimes);
		assertTrue(t1 != null);
		assertTrue(aprt == t1.getAirport());
		// Get taxi times.
		int[][][] taxiIn = t1.getTaxiInTimes();
		int[][][] taxiOut= t1.getTaxiOutTimes();
		for(int i=0;i<taxiInTimes.length;i++){
			for(int j=0;j<2;j++){
				for(int k=0;k<2;k++){
					assertTrue(taxiInTimes[i][j][k] == taxiIn[i][j][k]);
					assertTrue(taxiOutTimes[i][j][k]== taxiOut[i][j][k]);
				}
			}
		}
		
		// Set and get Neighbors.
		int startTime = 0;
		INode runway = new Runway(aprt,startTime,false,null,null,null,null);
		INode terminal = new Terminal(aprt,null,null);
		t1.setNeighbor(runway);
		t1.setNeighbor(terminal);
		INode[] nn = t1.getNeighbors();
		for(INode node : nn){
			if(node instanceof Runway){
				assertTrue(node == runway);
			} else if(node instanceof Terminal){
				assertTrue(node == terminal);
			} else assertFalse(true);
		}
		
	}
	/**
	 * Test of exception in setNeighbor().  Will accept only
	 * Runway and Terminal objects.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testIllArgExceptionSetNeighbor() {
		INode t1 = new Taxiway(1,null,null);
		INode t2 = new Taxiway(2,null,null);
		t1.setNeighbor(t2);	
	}
	/**
	 * test of receive() method.
	 */
	@Test
	public void testReceive(){
		// Dummy test for now.
		Taxiway t1 = new Taxiway(1,null,null);
		assertTrue(false == t1.receive(null,null));
	}
	/**
	 * test of computeTaxiInTime and computeTaxiOutTime.
	 */
	@Test
	public void testComputeTaxiInNTaxiOutTime(){
		int aprt[] = {32,45};
		Taxiway tn = new Taxiway(aprt[0],null,null);
		int[] itinNum = {21,2};
		int[] legNum = {1,2};
		int[] schedOutTime = {15,6};
		int[] taxiOutTime = {320,-20};
		int[] taxiInTime = {110,-10};
		
		int[][] pars = new int[2][IFlight.Param.values().length];
		for(int i=0;i<2;i++){
			pars[i][IFlight.Param.ITIN_NUM.ordinal()] = itinNum[i];
			pars[i][IFlight.Param.LEG_NUM.ordinal()] = legNum[i];
			pars[i][IFlight.Param.DEP_APRT.ordinal()] = aprt[0];
			pars[i][IFlight.Param.ARR_APRT.ordinal()] = aprt[1];
			pars[i][IFlight.Param.SCHED_OUT_TIME.ordinal()]= schedOutTime[i];
			pars[i][IFlight.Param.TAXI_OUT_TIME.ordinal()] = taxiOutTime[i];
			pars[i][IFlight.Param.TAXI_IN_TIME.ordinal()]  = taxiInTime[i];
		}
    	IFlight[] fs = new Flight[2];
    	for(int i=0;i<2;i++){
    		fs[i] = new Flight(pars[i]);
    	}
    	assertTrue(taxiInTime[0] == tn.computeTaxiInTime(fs[0]));
    	assertTrue(0 == tn.computeTaxiInTime(fs[1]));
    	assertTrue(taxiOutTime[0] == tn.computeTaxiOutTime(fs[0]));
    	assertTrue(0 == tn.computeTaxiOutTime(fs[1]));
    	
	}
}
