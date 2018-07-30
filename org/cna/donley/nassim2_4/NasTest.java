package org.cna.donley.nassim2_4;

import org.junit.Test;
import java.util.Random;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Test to validate the methods in the Nas class.
 *  Uses JUnit 4.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: NasTest.java 1 2009-06-05 00:00:00EST donley $
 */
public class NasTest
{
	/** 
	 *  Tests of all methods in this class.  The reason for this
	 *  is that the instance variables can't be reset without 
	 *  setter methods and I have not created them.  So it would
	 *  always be the same instance parameters anyways.
	 */
	@Test
	public void testAllMethods(){
		String[] indxToAprtMap = {"ALB","LAX","DTW","KJFK","EGLL"};
		String[] indxToCarrierMap={"COA","AAL","NWA","DLA"};
		String[] indxToEquipTypeMap = {"B733","A320","B732"};
		String[][] indxToFixMap = {{"FJS","W8XW","MARES","DOCI"},
				{"XCV","I3CP","IOU"}};
		
		// Instantiated properly?
		assertTrue(Nas.getInstance() == null);
		Nas.createInstance(
			indxToAprtMap,indxToCarrierMap,indxToEquipTypeMap,indxToFixMap);
		assertTrue(Nas.getInstance() != null);
		
		// Map of Airport names to indices and vice versa.
		int numAprts = Nas.getInstance().getNumAirports();
		assertTrue(numAprts == indxToAprtMap.length);
		for(int i=0;i<indxToAprtMap.length;i++){
			assertTrue(indxToAprtMap[i].equals(
				Nas.getInstance().getAirportFromIndex(i)));
			assertTrue(i == 
				Nas.getInstance().getAirportIndex(indxToAprtMap[i]));
		}
		// Check if it returns -1 if the name isn't in the list.
		assertTrue(-1 == Nas.getInstance().getAirportIndex("YEAH!!"));
		// Check if return null if the index is out of range.
		assertTrue(null == Nas.getInstance().getAirportFromIndex(-1));
		assertTrue(null == 
			Nas.getInstance().getAirportFromIndex(indxToAprtMap.length));
		
		// Map of carrier names to indices and vice versa.
		int numCars = Nas.getInstance().getNumCarriers();
		assertTrue(numCars == indxToCarrierMap.length);
		for(int i=0;i<indxToCarrierMap.length;i++){
			assertTrue(indxToCarrierMap[i].equals(
				Nas.getInstance().getCarrierFromIndex(i)));
			assertTrue(i == 
				Nas.getInstance().getCarrierIndex(indxToCarrierMap[i]));
		}
		// Check if it returns -1 if the name isn't in the list.
		assertTrue(-1 == Nas.getInstance().getCarrierIndex("YOH!!"));
		// Check if return null if the index is out of range.
		assertTrue(null == Nas.getInstance().getCarrierFromIndex(-1));
		assertTrue(null == 
			Nas.getInstance().getCarrierFromIndex(indxToCarrierMap.length));
		
		// Map of aircraft names to indices and vice versa.
		int numEquip = Nas.getInstance().getNumEquipTypes();
		assertTrue(numEquip == indxToEquipTypeMap.length);
		for(int i=0;i<indxToEquipTypeMap.length;i++){
			assertTrue(indxToEquipTypeMap[i].equals(
				Nas.getInstance().getEquipTypeFromIndex(i)));
			assertTrue(i == 
				Nas.getInstance().getEquipTypeIndex(indxToEquipTypeMap[i]));
		}
		// Check if it returns -1 if the name isn't in the list.
		assertTrue(-1 == Nas.getInstance().getEquipTypeIndex("EEE!!"));
		// Check if return null if the index is out of range.
		assertTrue(null == Nas.getInstance().getEquipTypeFromIndex(-1));
		assertTrue(null == 
			Nas.getInstance().getEquipTypeFromIndex(indxToEquipTypeMap.length));
		
		// MC indices.  Explicit test of getMcIndex() and implicit test of
		// createMcToIndxMap().
		Nas.Mc[] vals = Nas.Mc.values();
		for(int i=0;i<vals.length;i++){
			assertTrue(vals[i].ordinal()==
				Nas.getInstance().getMcIndex(vals[i].toString()));
		}
		// Check if it returns -1 if the name isn't in the list.
		assertTrue(-1 == Nas.getInstance().getMcIndex("YEAH!"));
		
		// Map of fix names to indices and vice versa.
		int numDepFix = Nas.getInstance().getNumFixes(Nas.Ad.DEP);
		int numArrFix = Nas.getInstance().getNumFixes(Nas.Ad.ARR);
		assertTrue(numDepFix == indxToFixMap[Nas.Ad.DEP.ordinal()].length);
		assertTrue(numArrFix == indxToFixMap[Nas.Ad.ARR.ordinal()].length);
		Nas.Ad[] ad = {Nas.Ad.DEP,Nas.Ad.ARR};
		for(int j=0;j<ad.length;j++){
			for(int i=0;i<indxToFixMap[ad[j].ordinal()].length;i++){
				assertTrue(indxToFixMap[ad[j].ordinal()][i].equals(
					Nas.getInstance().getFixNameFromIndex(ad[j],i)));
				assertTrue(i == Nas.getInstance().
					getFixIndex(ad[j],indxToFixMap[ad[j].ordinal()][i]));
			}
		}
		// Check if it returns -1 if the name isn't in the list.
		// and check if return null if the index is out of range.
		for(int i=0;i<ad.length;i++){
			assertTrue(-1 == Nas.getInstance().getFixIndex(ad[i],"EEE!!"));
			assertTrue(null == Nas.getInstance().getFixNameFromIndex(ad[i],-1));
			assertTrue(null == Nas.getInstance().
				getFixNameFromIndex(ad[i],indxToFixMap[ad[i].ordinal()].length));
		}
		
		// Random class instance.
		Random ran = Nas.getInstance().getRandom();
		assertTrue(ran != null);
		// Generate number from normal distribution.
		//for(int i=0;i<100;i++){
		//	System.out.println(i + " " + ran.nextGaussian());
		//}
		
		// Destroy method and create it again.
		Nas.destroyInstance();
		assertTrue(Nas.getInstance() == null);
		Nas.createInstance(indxToAprtMap,indxToCarrierMap,indxToEquipTypeMap,
			indxToFixMap);
		assertTrue(Nas.getInstance() != null);
		
		
    }
	
}
