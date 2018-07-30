package org.cna.donley.nassim2_4;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

import org.cna.donley.jdbc.SQLDate2;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

/**
 * Test to validate the NaspacBridge class.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: NaspacBridgeTest.java 1 2009-07-28 00:00:00EST donley $
 */
public class NaspacBridgeTest
{   
	
	static String rootPath = 
			//C:\\Documents and Settings\\James CTR Donley";
			"/Users/jdonley/workspaces/FAA/TestData";
	/**
	 * Extracts the carrier name from a string.
	 */
	@Test
	public void testExtractCarrierName(){
		String str = "A347";
		String car = null;
		car = NaspacBridge.extractCarrierName(str);
		assertTrue(car.equals("A"));
		str = "423";
		car = NaspacBridge.extractCarrierName(str);
		assertTrue(car == null);
		str = "AB";
		car = NaspacBridge.extractCarrierName(str);
		assertTrue(car.equals("AB"));
		str = "A";
		car = NaspacBridge.extractCarrierName(str);
		assertTrue(car.equals("A"));
		str = "ABCX12";
		car = NaspacBridge.extractCarrierName(str);
		assertTrue(car.equals("ABC"));
		
	}
    /**
     * Test constructors and readPropertiesFile() method (which is called
     * by the constructor.
     */
    @Test
	public void testConstructor()
    {
    	// Constructor.
    	Properties props = new Properties();
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NaspacBridgeTest.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
		assertTrue(true == 
			NaspacBridge.readModelInputProperties(propsFileNPath, props));
		
    	NaspacBridge nb = new NaspacBridge(propsFileNPath);
    	assertTrue(nb != null);
    }
    
    /**
     * 	Test of readBaseNForecastDays()
     */
    @Test
    public void testReadBaseNForecastDays(){
    	
    	// Read properties.
		Properties props = new Properties();
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NaspacBridgeTest.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
		assertTrue(true == 
			NaspacBridge.readModelInputProperties(propsFileNPath, props));
		
		// Get flight file name.
		String baseDir = props.getProperty("baseDir");
		String scenario = props.getProperty("scenario");
		String flightSchedSubDir = props.getProperty("flightSchedSubDir");
		String dir = baseDir + File.separator + scenario + File.separator +
			flightSchedSubDir;
		String flightSchedFilePrefix = 
			props.getProperty("flightSchedFilePrefix");
		String[] days = null;
		days = NaspacBridge.readBaseNForecastDays(
				dir, flightSchedFilePrefix);
		assertTrue(days != null);
		assertTrue(days[0].equals("20080820"));
		assertTrue(days[1].equals("20080820"));
    }
    
    /**
     * Test of determineAirportsCarriersBadaTypesFromFlights() method.
     */
    @Test
    public void testDetermineAirportsCarriersBadaTypesFromFlights(){
    	// Read properties.
    	Properties props = new Properties();
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NaspacBridgeTest.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
    	assertTrue(true == 
    		NaspacBridge.readModelInputProperties(propsFileNPath, props));
    	
    	// Get flight file name.
    	String baseDir = props.getProperty("baseDir");
		String scenario = props.getProperty("scenario");
		String aircraftInterSubDir = props.getProperty("aircraftInterSubDir");
		String findCrossingsFileSuffix = 
			props.getProperty("findCrossingsFileSuffix");
		String vfrFlightsFileSuffix = 
			props.getProperty("vfrFlightsFileSuffix");
		String fcFile = scenario + findCrossingsFileSuffix;
		String vfrFile = scenario + vfrFlightsFileSuffix;
		File fcFileNPath = new File(baseDir + File.separator +
			scenario + File.separator + aircraftInterSubDir + File.separator +
			fcFile);
		File vfrFileNPath = new File(baseDir + File.separator +
				scenario + File.separator + aircraftInterSubDir + File.separator +
				vfrFile);
    	
    	List<String> aprtList = new ArrayList<String>();
    	List<String> carrierList = new ArrayList<String>();
    	List<String> badaList = new ArrayList<String>();
    	boolean success =
    		NaspacBridge.determineAirportsCarriersBadaTypesFromFlights(
    			fcFileNPath, vfrFileNPath, aprtList, 
    			carrierList, badaList);
    	assertTrue(success);
    	assertTrue(aprtList.size() == 2563);
    	assertTrue(carrierList.size() == 794);
    	assertTrue(badaList.size() == 82);
    }
    /**
     * Test of readAprtCarrierEquipTypeFromFlightSched() method.
     */
    @Test
    public void testReadAprtCarrierEquipTypeFromFlightSched(){
    	// Read properties.
    	Properties props = new Properties();
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NaspacBridgeTest.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
    	assertTrue(true == 
    		NaspacBridge.readModelInputProperties(propsFileNPath, props));
    	
    	// Get flight sched file name and dir.
    	String baseDir = props.getProperty("baseDir");
		String scenario = props.getProperty("scenario");
		String flightSchedSubDir = props.getProperty("flightSchedSubDir");
		String flightSchedDir = baseDir + File.separator + scenario + File.separator +
			flightSchedSubDir;
		String flightSchedFilePrefix =props.getProperty("flightSchedFilePrefix");
		String flightSchedFileSuffix =props.getProperty("flightSchedFileSuffix");
		String[] days = null;
		days = NaspacBridge.readBaseNForecastDays(flightSchedDir, 
				flightSchedFilePrefix);
		assertTrue(days!=null);
		String baseDay = days[0];
		String forecastDay = days[1];
		String flightSchedFile = flightSchedFilePrefix + baseDay + "_" +
			forecastDay + flightSchedFileSuffix;
		File flightSchedFileNPath = new File(flightSchedDir + File.separator +
				flightSchedFile);
		
		// Read flight sched and get some flight info.
    	List<String> aprtList = new ArrayList<String>();
    	List<String> carrierList = new ArrayList<String>();
    	List<String> badaEquipList = new ArrayList<String>();
    	List<String> etmsEquipList = new ArrayList<String>();
    	boolean success = NaspacBridge.readAprtCarrierEquipTypeFromFlightSched(
    		flightSchedFileNPath, aprtList, carrierList, etmsEquipList, 
    		badaEquipList);
    	assertTrue(success);
    	assertTrue(aprtList.size() == 2643);
    	assertTrue(carrierList.size() == 1039);
    	assertTrue(etmsEquipList.size() == 465);
    	assertTrue(badaEquipList.size() == 82);
    }
    /**
     * Test of readFixNamesFromFile() method.
     */
    @Test
    public void testReadFixNamesNFixDelaysFromFile(){
    	// Read properties.
    	Properties props = new Properties();
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NaspacBridgeTest.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
    	assertTrue(true == 
    		NaspacBridge.readModelInputProperties(propsFileNPath, props));
    	
    	//------------------------------------------------------------------------
    	// Fix names
    	//------------------------------------------------------------------------
    	// Get flight sched file name and dir.
    	String baseDir = props.getProperty("baseDir");
		String scenario = props.getProperty("scenario");
		String interSubDir = props.getProperty("aircraftInterSubDir");
		String fcSchedDir = baseDir + File.separator + scenario + File.separator +
			interSubDir;
		String fcFile = scenario + props.getProperty("findCrossingsFileSuffix");
		File fcFileNPath = new File(fcSchedDir + File.separator + fcFile);
		
		// Read flight sched and get some flight info.
    	List<String> depFixList = new ArrayList<String>();
    	List<String> arrFixList = new ArrayList<String>();
    	
    	boolean success = NaspacBridge.readFixNamesFromFCFile(
    			fcFileNPath,depFixList,arrFixList);
    	assertTrue(success);
    	assertTrue(depFixList.size() == 64);
    	assertTrue(arrFixList.size() == 50);
    	
    	//-------------------------------------------------------------------------
    	// Fix delays
    	//-------------------------------------------------------------------------
    	// Nas object.  Need only fix to index map.  Rest can be bogus.
    	String[] indxToAprtMap = {"ABQ","DTW","JFK"};
    	String[] indxToCarrierMap = {"AAL","COA","SWA"};
    	String[] indxToEquipTypeMap = {"A319","B732","B757"};
    	String[][] indxToFixMap = new String[2][1];
    	indxToFixMap[Nas.Ad.DEP.ordinal()] = 
    		depFixList.toArray(indxToFixMap[Nas.Ad.DEP.ordinal()]);
    	indxToFixMap[Nas.Ad.ARR.ordinal()] = 
    		arrFixList.toArray(indxToFixMap[Nas.Ad.ARR.ordinal()]);
    	Nas.destroyInstance();
		Nas.createInstance(indxToAprtMap,indxToCarrierMap,indxToEquipTypeMap,
			indxToFixMap);
		Nas nas = Nas.getInstance();
    	
    	// Get file names and dirs.
		String fixDelaySubDir = props.getProperty("fixDelaySubDir");
		String depFixDelayFile = props.getProperty("depFixDelayFile");
		String arrFixDelayFile = props.getProperty("arrFixDelayFile");
		String fixDelayPath = baseDir + File.separator + scenario + 
			File.separator + fixDelaySubDir;
		File depFixDelayFileNPath = 
			new File(fixDelayPath + File.separator + depFixDelayFile);
		File arrFixDelayFileNPath = 
			new File(fixDelayPath + File.separator + arrFixDelayFile);
		int[][] fixDelays = NaspacBridge.readFixDelaysFromFile(nas,
			depFixDelayFileNPath,arrFixDelayFileNPath);
		assertTrue(fixDelays != null);
		assertTrue(fixDelays[Nas.Ad.DEP.ordinal()].length == depFixList.size());
		assertTrue(fixDelays[Nas.Ad.ARR.ordinal()].length == arrFixList.size());
		
		// Individual checks
		double fixD = 
			fixDelays[Nas.Ad.DEP.ordinal()][nas.getFixIndex(Nas.Ad.DEP, "HAMME")];
		assertTrue(fixD == (int)(1.50*60.));
		fixD = fixDelays[Nas.Ad.ARR.ordinal()][nas.getFixIndex(Nas.Ad.ARR, "SHOOZ")];
		assertTrue(fixD == (int)(1.13*60.));
    }
    
    /**
     * Test of createRatesFlagsForAprts
     */
    @Test
    public void testCreateRatesFlagsForAprts(){
		
		// Create Nas instance.
		String[] indxToAprtMap = {"ALB","LAX","DTW","LGA","JFK","EGLL"};
		String[] indxToCarrierMap={"COA","AAL","NWA","DLA"};
		String[] indxToBadaMap = {"B733","A320","B732"};
		Nas.destroyInstance();
		Nas.createInstance(indxToAprtMap,indxToCarrierMap,indxToBadaMap,null);
		Nas nas = Nas.getInstance();
		
		// Get list of airports for which to compute acceptance rates.
		// All airports.
		String computeRates = "All";
		boolean[] computeRatesAprts = null;
		computeRatesAprts = 
			NaspacBridge.createRatesFlagsForAprts(nas, computeRates);
		for(int i=0;i<computeRatesAprts.length;i++){
			assertTrue(computeRatesAprts[i]);
		}
		// None.
		computeRates = "None";
		computeRatesAprts = 
			NaspacBridge.createRatesFlagsForAprts(nas, computeRates);
		for(int i=0;i<computeRatesAprts.length;i++){
			assertFalse(computeRatesAprts[i]);
		}
		// Some
		computeRates="LGA, JFK";
		computeRatesAprts = 
			NaspacBridge.createRatesFlagsForAprts(nas, computeRates);
		for(int i=0;i<computeRatesAprts.length;i++){
			if(i == nas.getAirportIndex("LGA") ||
			   i == nas.getAirportIndex("JFK")){
				assertTrue(computeRatesAprts[i]);
			}
			else assertFalse(computeRatesAprts[i]);
		}
    }
    
    /**
     * Test of readChangeCapacities() and readParetoCurves() methods.
     */
    @Test
    public void testReadChangeCapacitiesNParetoCurves(){
    	// Read properties.
    	Properties props = new Properties();
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NaspacBridgeTest.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
    	assertTrue(true == 
    		NaspacBridge.readModelInputProperties(propsFileNPath, props));
    	
    	String baseDir = props.getProperty("baseDir");
		String scenario = props.getProperty("scenario");
		
		String baseDay = props.getProperty("baseDay");
		
		String findCrossingsFileSuffix = props.getProperty("findCrossingsFileSuffix");
		String vfrFlightsFileSuffix = props.getProperty("vfrFlightsFileSuffix");
		String findCrossingsFile = scenario + findCrossingsFileSuffix;
		String vfrFlightsFile = scenario + vfrFlightsFileSuffix;
		String aircraftInterSubDir = props.getProperty("aircraftInterSubDir");
		String aircraftInterDir = baseDir + File.separator + scenario +
			File.separator + aircraftInterSubDir;
		File findCrossingsFileNPath = new File(aircraftInterDir + 
			File.separator + findCrossingsFile);
		File vfrFlightsFileNPath = new File(aircraftInterDir + 
			File.separator + vfrFlightsFile);
		
		// Get the list of airports, air carriers and aircraft types.
		// These will be all the airports to be modeled and the other stuff
		// will be used to classify flights.  Needed to create the Nas 
		// object.
		String[] indxToAprtMap = new String[1];
		String[] indxToCarrierMap = new String[1];
		String[] indxToBadaMap = new String[1];
		List<String> aprtList = new ArrayList<String>();
		List<String> carrierList = new ArrayList<String>();
		List<String> badaList = new ArrayList<String>();
		boolean success = NaspacBridge.determineAirportsCarriersBadaTypesFromFlights(
				findCrossingsFileNPath,vfrFlightsFileNPath,
				aprtList,carrierList,badaList);
		assertTrue(success);
		indxToAprtMap = aprtList.toArray(indxToAprtMap);
		indxToCarrierMap = carrierList.toArray(indxToCarrierMap);
		indxToBadaMap = badaList.toArray(indxToBadaMap);
		
		// With airports and delt create instance of Nas class.
		Nas.destroyInstance();
		Nas.createInstance(indxToAprtMap,indxToCarrierMap,
			indxToBadaMap,null);
		Nas nas = Nas.getInstance();
		
		//--------------------------------------------------------------
		// TEST of readChangeCapacities().
		//--------------------------------------------------------------
		// Open and read the NASPAC airport capacity file to get
		// change airport conditions for all airports.  Here, the
		// first element is the airport, the second the time w/r to
		String changeCapSubDir = props.getProperty("changeCapSubDir");
		String changeCapPrefix = props.getProperty("changeCapPrefix");
		String changeFile = changeCapPrefix + baseDay;
		File changeCapFile = new File(baseDir + File.separator +
			scenario + File.separator + changeCapSubDir + File.separator +
			changeFile);
		int[][][] changeCapacities = 
			NaspacBridge.readChangeCapacities(nas,changeCapFile);	
		assertTrue(changeCapacities != null);
		
		// Look at individual cases.
		String aprt = "DSM";
		int[][] changeCapsAct = {{527,0},{714,2},{774,0}};
		int aprtIndx = nas.getAirportIndex(aprt);
		int[][] changeCaps = changeCapacities[aprtIndx];
		for(int i=0;i<changeCapsAct.length;i++){
			for(int j=0;j<2;j++){
				assertTrue(changeCapsAct[i][j]==changeCaps[i][j]);
			}
		}
		aprt = "PIE";
		int[][] changeCapsAct2 = {{527,2},{574,0},{612,2},{783,0},{871,2},
			{903,0},{1073,2},{1133,0},{1173,2},{1180,0},{1370,2},{1396,0}};
		aprtIndx = nas.getAirportIndex(aprt);
		changeCaps = changeCapacities[aprtIndx];
		for(int i=0;i<changeCapsAct2.length;i++){
			for(int j=0;j<2;j++){
				assertTrue(changeCapsAct2[i][j]==changeCaps[i][j]);
			}
		}
		
		
		// Compute the total number of entries in changeCaps.
		int count = 0;
		for(int i=0;i<changeCapacities.length;i++){
			if(changeCapacities[i] != null){
				for(int j=0;j<changeCapacities[i].length;j++){
					// If the time is greater than 0 then count it.
					// It better be.
					if(changeCapacities[i][j][0] > 0) count++;
				}
			}
		}
		BufferedReader br = null;
		int nAp = 0;
		try {
			br = new BufferedReader(new FileReader(changeCapFile));
			String line = null;
			while((line = br.readLine()) != null){
				if(line.length() > 6 && line.substring(0,2).equals("AP")){
					nAp++;
				}
			}
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			assertTrue(false);
		}catch(IOException ioe){
			ioe.printStackTrace();
			assertTrue(false);
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		assertTrue(count == nAp);
		
		//--------------------------------------------------------------
		// TEST of readParetoCurves().
		//--------------------------------------------------------------
		String paretoSubDir = props.getProperty("paretoSubDir");
		String paretoFileName = props.getProperty("paretoFile");
		File paretoFile =  new File(baseDir + File.separator + scenario
			+ File.separator + paretoSubDir + File.separator + paretoFileName);
		// Get pareto curves.  Need the aprt to index map in {@link Nas}.
		double[][][][] paretoCurves = 
			NaspacBridge.readParetoCurves(nas,paretoFile);
		assertTrue(paretoCurves != null);
		// Count how many pareto curves are not null.  There should be 110 here.
		count = 0;
		for(int i=0;i<paretoCurves.length;i++){
			if(paretoCurves[i] != null){
				count ++;
			}
		}
		assertTrue(count == 110);
		
		// Check curves for an airport.
		aprt = "ATL";
		aprtIndx = nas.getAirportIndex(aprt);
		String mc = "MVMC";
		int mcIndx = nas.getMcIndex(mc);
		double[][] curves = {{0.0,121.4},{100.5,121.4},{112.8,111.9},
			{125.8,86.6},{125.8,0.0}};
		// Divide by 4 to get qtr hour rates.
		for(int i=0;i<curves.length;i++){
			for(int j=0;j<curves[i].length;j++)curves[i][j] /= 4.;
		}
		
		for(int i=0;i<curves.length;i++){
			for(int j=0;j<2;j++){
				assertTrue(curves[i][j]==paretoCurves[aprtIndx][mcIndx][i][j]);
			}
		}	
		aprt = "VNY";
		aprtIndx = nas.getAirportIndex(aprt);
		mc = "MVMC";
		mcIndx = nas.getMcIndex(mc);
		double[][] curves2 = {{0.0,43.0},{43.0,43.0},{58.0,18.0},{58.0,0.0}};
		// Divide by 4 to get qtr hour rates.
		for(int i=0;i<curves2.length;i++){
			for(int j=0;j<curves2[i].length;j++)curves2[i][j] /= 4.;
		}
		
		for(int i=0;i<curves2.length;i++){
			for(int j=0;j<2;j++){
				assertTrue(curves2[i][j]==paretoCurves[aprtIndx][mcIndx][i][j]);
			}
		}
    }
    
    /**
     * Test of readModeledAirports().
     */
    @Test
    public void testReadModeledAirports(){
    	// Read properties.
    	Properties props = new Properties();
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NaspacBridgeTest.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
    	assertTrue(true == 
    		NaspacBridge.readModelInputProperties(propsFileNPath, props));
    	
    	String baseDir = props.getProperty("baseDir");
		String scenario = props.getProperty("scenario");
		
		// Flight schedule stuff
		String flightSchedSubDir = props.getProperty("flightSchedSubDir");
		String flightSchedDir = baseDir + File.separator + scenario + 
			File.separator + flightSchedSubDir;
		String flightSchedFilePrefix = props.getProperty("flightSchedFilePrefix");
		String flightSchedFileSuffix = props.getProperty("flightSchedFileSuffix");
		String[] days = NaspacBridge.readBaseNForecastDays(
				flightSchedDir,flightSchedFilePrefix);
		String baseDay = days[0];
		String forecastDay = days[1];
		String flightSchedFile = flightSchedFilePrefix + baseDay + "_" +
			forecastDay + flightSchedFileSuffix;
		File flightSchedFileNPath = new File(flightSchedDir + File.separator +
				flightSchedFile);
		
		// Get the list of airports, air carriers and aircraft types.
		// These will be all the airports to be modeled and the other stuff
		// will be used to classify flights.  Needed to create the Nas 
		// object.
		String[] indxToAprtMap = new String[1];
		String[] indxToCarrierMap = new String[1];
		String[] indxToBadaMap = new String[1];
		String[] indxToEtmsEquipMap = new String[1];
		List<String> aprtList = new ArrayList<String>();
		List<String> carrierList = new ArrayList<String>();
		List<String> badaEquipList = new ArrayList<String>();
		List<String> etmsEquipList = new ArrayList<String>();
		boolean success = NaspacBridge.readAprtCarrierEquipTypeFromFlightSched(
				flightSchedFileNPath, aprtList, carrierList, etmsEquipList, 
				badaEquipList);
		assertTrue(success);
		indxToAprtMap = aprtList.toArray(indxToAprtMap);
		indxToCarrierMap = carrierList.toArray(indxToCarrierMap);
		indxToBadaMap = badaEquipList.toArray(indxToBadaMap);
		indxToEtmsEquipMap = etmsEquipList.toArray(indxToEtmsEquipMap);
		
		// With airports and other stuff create instance of Nas class.
		Nas.destroyInstance();
		Nas.createInstance(indxToAprtMap,indxToCarrierMap,
			indxToEtmsEquipMap,null);
		Nas nas = Nas.getInstance();
		
		//-----------------------------------------------------------------
		// Test of readModeledAirports().
		//-----------------------------------------------------------------
		String airportsFile = props.getProperty("modeledAirportsFile");
		String airportsSubDir = props.getProperty("modeledAirportsSubDir");
		File aprtsFileNPath = new File(baseDir + File.separator + airportsSubDir+
			File.separator + airportsFile);
		
		int[] modeledAprts = null;
		modeledAprts = NaspacBridge.readModeledAirports(nas,aprtsFileNPath);
		assertTrue(modeledAprts != null);
		assertTrue(modeledAprts.length == 35);
		assertTrue(modeledAprts[0] == nas.getAirportIndex("ATL"));
		assertTrue(modeledAprts[modeledAprts.length-1] == nas.getAirportIndex("TPA"));
	
    }
    /**
     * Test of pareAirports method.
     */
    @Test
    public void testPareAirports(){
    	int[] aprts = {0,1,2,3,4,5};
    	int[] modeledAprts = {0,2,4};
    	int[][][] calledRates = new int[aprts.length][1][];
    	int[][][] changeCaps = new int[aprts.length][1][];
    	double[][][][] paretoCurves = new double[aprts.length][1][][];
    	
    	// Case 1.  No change.
    	int[] mAprts = {-1};
    	boolean success = NaspacBridge.pareAirports(
    			mAprts, calledRates, changeCaps, paretoCurves);
    	assertTrue(success);
    	for(int i=0;i<aprts.length;i++){
    		assertTrue(calledRates[i] != null);
    		assertTrue(changeCaps[i] != null);
    		assertTrue(paretoCurves[i] != null);
    	}
    	
    	// Case 2. No change again.
    	success = NaspacBridge.pareAirports(
    			null, calledRates, changeCaps, paretoCurves);
    	assertTrue(success);
    	for(int i=0;i<aprts.length;i++){
    		assertTrue(calledRates[i] != null);
    		assertTrue(changeCaps[i] != null);
    		assertTrue(paretoCurves[i] != null);
    	}
    	// Case 3. Pare airports.
    	success = NaspacBridge.pareAirports(
    			modeledAprts, calledRates, changeCaps, paretoCurves);
    	assertTrue(success);
    	for(int i=0;i<aprts.length;i++){
    		if(i%2 == 0){
	    		assertTrue(calledRates[i] != null);
	    		assertTrue(changeCaps[i] != null);
	    		assertTrue(paretoCurves[i] != null);
    		} else {
    			assertTrue(calledRates[i] == null);
	    		assertTrue(changeCaps[i] == null);
	    		assertTrue(paretoCurves[i] == null);	
    		}
    	}	
    }
    
    /**
     * Test of readNominalTaxiTimes() method.
     */
 /*
    @Test
    public void testReadNominalTaxiTimes(){
    	// Read properties.
    	Properties props = new Properties();
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NaspacBridgeTest.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
    	assertTrue(true == 
    		NaspacBridge.readModelInputProperties(propsFileNPath, props));	
    	
    	String baseDir = props.getProperty("baseDir");
		String scenario = props.getProperty("scenario");
		
		String baseDay = props.getProperty("baseDay");
		String forecastDay = props.getProperty("forecastDay");
		
		String findCrossingsFileSuffix = props.getProperty("findCrossingsFileSuffix");
		String vfrFlightsFileSuffix = props.getProperty("vfrFlightsFileSuffix");
		String findCrossingsFile = scenario + findCrossingsFileSuffix;
		String vfrFlightsFile = scenario + vfrFlightsFileSuffix;
		String aircraftInterSubDir = props.getProperty("aircraftInterSubDir");
		String aircraftInterDir = baseDir + File.separator + scenario +
			File.separator + aircraftInterSubDir;
		File findCrossingsFileNPath = new File(aircraftInterDir + 
			File.separator + findCrossingsFile);
		File vfrFlightsFileNPath = new File(aircraftInterDir + 
			File.separator + vfrFlightsFile);
		
		// Get the list of airports, air carriers and aircraft types.
		// These will be all the airports to be modeled and the other stuff
		// will be used to classify flights.  Needed to create the Nas 
		// object.
		String[] indxToAprtMap = new String[1];
		String[] indxToCarrierMap = new String[1];
		String[] indxToBadaMap = new String[1];
		List<String> aprtList = new ArrayList<String>();
		List<String> carrierList = new ArrayList<String>();
		List<String> badaList = new ArrayList<String>();
		boolean success = NaspacBridge.determineAirportsCarriersBadaTypesFromFlights(
				findCrossingsFileNPath,vfrFlightsFileNPath,
				aprtList,carrierList,badaList);
		assertTrue(success);
		indxToAprtMap = aprtList.toArray(indxToAprtMap);
		indxToCarrierMap = carrierList.toArray(indxToCarrierMap);
		indxToBadaMap = badaList.toArray(indxToBadaMap);
		
		// With airports and delt create instance of Nas class.
		Nas.destroyInstance();
		Nas.createInstance(indxToAprtMap,indxToCarrierMap,indxToBadaMap);
		Nas nas = Nas.getInstance();
    	
		String taxiTimesDir = baseDir + File.separator +
			props.getProperty("taxiTimesSubDir");
		String taxiTimesFilePrefix = props.getProperty("taxiTimesFilePrefix");
		String taxiTimesFileSuffix = props.getProperty("taxiTimesFileSuffix");
		String year = baseDay.substring(0,4);
		String taxiTimesFile = taxiTimesFilePrefix + year + taxiTimesFileSuffix;
		File taxiTimesFileNPath = new File(taxiTimesDir + File.separator +
			taxiTimesFile);
    	int[][][] nomTaxiTimes = 
    		NaspacBridge.readNominalTaxiTimes(nas,baseDay,taxiTimesFileNPath);
    	assertTrue(nomTaxiTimes != null);
    	// Check some airports.
    	String aprt = "TPA";
    	String carrier = "AAL";
    	int aprtIndx = nas.getAirportIndex(aprt);
    	int carrierIndx = nas.getCarrierIndex(carrier);
    	int[] times = new int[2];
    	int minInSecs = 60;
    	times[Nas.Ad.DEP.ordinal()] = (int)(10.6*minInSecs + 0.5);
    	times[Nas.Ad.ARR.ordinal()] = (int)(3.8*minInSecs + 0.5);
    	for(int i=0;i<2;i++){
    		assertTrue(times[i] == nomTaxiTimes[aprtIndx][carrierIndx][i]);
    	}
    	carrier = "ZZZ";
    	// All carriers.  Have for quarter 3 except FFT.
    	String[] carsAll = {"AAL","ACA","COA","COM","DAL","FDX","JBU","NWA",
    		"SWA","TRS","UAL","USA"};
    	aprtIndx = nas.getAirportIndex(aprt);
    	carrierIndx = nas.getCarrierIndex(carrier);
    	times[Nas.Ad.DEP.ordinal()] = (int)(9.8*minInSecs + 0.5); 
    	times[Nas.Ad.ARR.ordinal()] = (int)(4.1*minInSecs + 0.5);
    	
    	for(int i=0;i<nas.getNumCarriers();i++){
    		// All carriers except those listed in carsAll above.
    		success = false;
    		for(String car : carsAll){
    			if(i == nas.getCarrierIndex(car)){
    				success = true;
    				break;
    			}
    		}
    		if(success == false){
	    		for(int j=0;j<2;j++){
	    			assertTrue(times[j] == nomTaxiTimes[aprtIndx][i][j]);
	    		}
    		}
    	}
    	Nas.destroyInstance();
    }
  */ 
    /**
     * Test of readAspmAcceptanceRates() method.
     */
    @Test
    public void testReadAspmAcceptanceRates(){
    	// Read properties.
    	Properties props = new Properties();
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NaspacBridgeTest.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
    	assertTrue(true == 
    		NaspacBridge.readModelInputProperties(propsFileNPath, props));	
    	
    	String baseDir = props.getProperty("baseDir");
		String scenario = props.getProperty("scenario");
		
		String baseDay = props.getProperty("baseDay");
		String forecastDay = props.getProperty("forecastDay");
		
		String findCrossingsFileSuffix = props.getProperty("findCrossingsFileSuffix");
		String vfrFlightsFileSuffix = props.getProperty("vfrFlightsFileSuffix");
		String findCrossingsFile = scenario + findCrossingsFileSuffix;
		String vfrFlightsFile = scenario + vfrFlightsFileSuffix;
		String aircraftInterSubDir = props.getProperty("aircraftInterSubDir");
		String aircraftInterDir = baseDir + File.separator + scenario +
			File.separator + aircraftInterSubDir;
		File findCrossingsFileNPath = new File(aircraftInterDir + 
			File.separator + findCrossingsFile);
		File vfrFlightsFileNPath = new File(aircraftInterDir + 
			File.separator + vfrFlightsFile);
		
		// Get the list of airports, air carriers and aircraft types.
		// These will be all the airports to be modeled and the other stuff
		// will be used to classify flights.  Needed to create the Nas 
		// object.
		String[] indxToAprtMap = new String[1];
		String[] indxToCarrierMap = new String[1];
		String[] indxToBadaMap = new String[1];
		List<String> aprtList = new ArrayList<String>();
		List<String> carrierList = new ArrayList<String>();
		List<String> badaList = new ArrayList<String>();
		boolean success = NaspacBridge.determineAirportsCarriersBadaTypesFromFlights(
				findCrossingsFileNPath,vfrFlightsFileNPath,
				aprtList,carrierList,badaList);
		assertTrue(success);
		indxToAprtMap = aprtList.toArray(indxToAprtMap);
		indxToCarrierMap = carrierList.toArray(indxToCarrierMap);
		indxToBadaMap = badaList.toArray(indxToBadaMap);
		
		// With airports and delt create instance of Nas class.
		Nas.destroyInstance();
		Nas.createInstance(indxToAprtMap,indxToCarrierMap,indxToBadaMap,null);
		Nas nas = Nas.getInstance();
    	
		String acceptRatesSubDir = 
			props.getProperty("aspmAcceptRatesSubDir");
		String acceptRatesFilePrefix = 
			props.getProperty("aspmAcceptRatesFilePrefix");
		String acceptRatesFileSuffix = 
			props.getProperty("aspmAcceptRatesFileSuffix");
		String acceptRatesFile = acceptRatesFilePrefix + baseDay +
			acceptRatesFileSuffix;
		
		File acceptRatesFileNPath = new File(baseDir + File.separator +
			acceptRatesSubDir + File.separator + acceptRatesFile);
		int[][][] calledRates = NaspacBridge.readAspmAcceptanceRates(nas,
			(new SQLDate2(baseDay,SQLDate2.Element.dd)),acceptRatesFileNPath);
		assertTrue(calledRates != null);
		
		// Count how many non-null rates.
		int nAprts = 0;
		for(int i=0;i<nas.getNumAirports();i++){
			if(calledRates[i] != null)nAprts++;
		}
		assertTrue(nAprts == 77);
		
    	// Check some airports.  Have qtr hour rates upon output.
    	String aprt = "TPA";
    	int aprtIndx = nas.getAirportIndex(aprt);
    	int[] rates = new int[2];
    	int indx = 0;
    	rates[Nas.Ad.DEP.ordinal()] = 13;
    	rates[Nas.Ad.ARR.ordinal()] = 17;
    	for(int i=0;i<2;i++){
    		assertTrue(rates[i] == calledRates[aprtIndx][indx][i]);
    	}
    	aprt = "VNY";
    	aprtIndx = nas.getAirportIndex(aprt);
    	indx = 132; // end
    	rates[Nas.Ad.DEP.ordinal()] = 11;
    	rates[Nas.Ad.ARR.ordinal()] = 9;
    	for(int i=0;i<2;i++){
    		assertTrue(rates[i] == calledRates[aprtIndx][indx][i]);
    	}
    	aprt = "JFK";
    	aprtIndx = nas.getAirportIndex(aprt);
    	indx = 12; // somewhere
    	rates[Nas.Ad.DEP.ordinal()] = 11;
    	rates[Nas.Ad.ARR.ordinal()] = 9;
    	for(int i=0;i<2;i++){
    		assertTrue(rates[i] == calledRates[aprtIndx][indx][i]);
    	}   	
    }
    /**
     * Test of computeEstimatedOnNOffTimes.
     */
    @Test
    public void testComputeEstimatedOnNOffTimes(){
    	// Create bogus Nas instance.
		String[] indxToAprtMap =
			{"ABQ","LAX","DTW","LGA","JFK","EGLL","ANC","ORD"};
		String[] indxToCarrierMap={"COA","AAL","NWA","DLA"};
		String[] indxToBadaMap = {"B733","A320","B732"};
		Nas.destroyInstance();
		Nas.createInstance(indxToAprtMap,indxToCarrierMap,indxToBadaMap,null);
		Nas nas = Nas.getInstance();
		
		// Create bogus nomTaxiTimes array.
		int nAprts = nas.getNumAirports();
		int nCarriers = nas.getNumCarriers();
		int[][][] nomTaxiTimes = new int[nAprts][nCarriers][2];
		for(int i=0;i<nAprts;i++){
			for(int j=0;j<nCarriers;j++){
				nomTaxiTimes[i][j][Nas.Ad.DEP.ordinal()] = 1100;
				nomTaxiTimes[i][j][Nas.Ad.ARR.ordinal()] = 897;// used if
					// the flight has no dep airport.
			}
		}
		// Create some bogus flights.
		int[] pars = new int[IFlight.Param.values().length];
		for(int i=0;i<pars.length;i++){
			pars[i] = 10*i;
		}
		IFlight[] fls = new IFlight[7];
		String[][] aprts = {{"ABQ","LAX"},{"ABQ","DTW"},{"LAX","DTW"},
				{"JFK","EGLL"},{"LGA","DTW"},{"ANC","ANC"},{"ORD","ORD"}};
		String[] carriers = {"AAL","COA"};
		int car = 0;
		int schedOutTime = 100;
		int schedInTime = 88;
		int airTime = 450;
		for(int i=0;i<fls.length;i++){
			int depAprt = nas.getAirportIndex(aprts[i][Nas.Ad.DEP.ordinal()]);
			int arrAprt = nas.getAirportIndex(aprts[i][Nas.Ad.ARR.ordinal()]);
			pars[IFlight.Param.DEP_APRT.ordinal()] = depAprt;
			pars[IFlight.Param.ARR_APRT.ordinal()] = arrAprt;
			pars[IFlight.Param.SCHED_OUT_TIME.ordinal()] = schedOutTime;
			pars[IFlight.Param.ACT_AIR_TIME.ordinal()] = airTime;
			pars[IFlight.Param.SCHED_IN_TIME.ordinal()] = schedInTime;
			if(i%2==0)car = nas.getCarrierIndex(carriers[0]);
			else car = nas.getCarrierIndex(carriers[1]);
			pars[IFlight.Param.CARRIER.ordinal()] = car;
			fls[i] = new Flight(pars);
		}
		// Two flights that are sources or sinks.
		fls[fls.length-1].set(IFlight.Param.DEP_APRT, -1);
		fls[fls.length-2].set(IFlight.Param.ARR_APRT, -1);
		
		// Compute estimated on and off times for these flights.
		// First index is airport, second index is arrival or departure
		// and the last index numbers of flight from that airport.
	    int[][][] estOnNOffTime = 
	    	NaspacBridge.computeEstimatedOnNOffTimes(nas,nomTaxiTimes,fls);
	    // Check if number of flights in each bin is correct.
	    assertTrue(2 == estOnNOffTime[nas.getAirportIndex("ABQ")][Nas.Ad.DEP.ordinal()].length);
	    assertTrue(null == estOnNOffTime[nas.getAirportIndex("ABQ")][Nas.Ad.ARR.ordinal()]);
	    assertTrue(null == estOnNOffTime[nas.getAirportIndex("DTW")][Nas.Ad.DEP.ordinal()]);
	    assertTrue(3 == estOnNOffTime[nas.getAirportIndex("DTW")][Nas.Ad.ARR.ordinal()].length);
	    assertTrue(1 == estOnNOffTime[nas.getAirportIndex("LAX")][Nas.Ad.DEP.ordinal()].length);
	    assertTrue(1 == estOnNOffTime[nas.getAirportIndex("LAX")][Nas.Ad.ARR.ordinal()].length);
	    assertTrue(1 == estOnNOffTime[nas.getAirportIndex("ANC")][Nas.Ad.DEP.ordinal()].length);
	    assertTrue(null == estOnNOffTime[nas.getAirportIndex("ANC")][Nas.Ad.ARR.ordinal()]);
	    assertTrue(null == estOnNOffTime[nas.getAirportIndex("ORD")][Nas.Ad.DEP.ordinal()]);
	    assertTrue(1 == estOnNOffTime[nas.getAirportIndex("ORD")][Nas.Ad.ARR.ordinal()].length);
	    // Check times. Airports are in alphabetical order, except for the last
	    // two.
	    int time = 0;
	    for(int i=0;i<nAprts;i++){
	    	for(int j=0;j<2;j++){
	    		if(estOnNOffTime[i][j] != null){
		    		for(int k=0;k<estOnNOffTime[i][j].length;k++){
		    			// taxi times are independent of carrier and arr or dep
		    			// in this test.
		    			if(i < nAprts-2){
			    			if(j == Nas.Ad.DEP.ordinal()){
			    				time = schedOutTime + nomTaxiTimes[i][0][Nas.Ad.DEP.ordinal()];
			    				assertTrue(time == estOnNOffTime[i][j][k]);                                   
			    			}else {
			    				time = schedOutTime + nomTaxiTimes[i][0][Nas.Ad.DEP.ordinal()] +
			    					airTime;
			    				assertTrue(time == estOnNOffTime[i][j][k]);     
			    			}
		    			} else if(i == 6){
		    				// ANC and no arrival airport.
		    				if(j == Nas.Ad.DEP.ordinal()){
			    				time = schedOutTime + nomTaxiTimes[i][0][Nas.Ad.DEP.ordinal()];
			    				assertTrue(time == estOnNOffTime[i][j][k]);                                   
			    			}
		    			} else if(i==7){
		    				// ORD and has no dep airport.
		    				if(j == Nas.Ad.ARR.ordinal()){
			    				time = schedInTime - nomTaxiTimes[i][0][Nas.Ad.ARR.ordinal()];
			    				assertTrue(time == estOnNOffTime[i][j][k]);                                   
			    			}
		    			}
		    		}
	    		}
	    	}
	    }
	    //--------------------------------------------------------------------
	    // Special cases.
	    //--------------------------------------------------------------------
	    // Create bogus nomTaxiTimes array.
		nAprts = nas.getNumAirports();
		nCarriers = nas.getNumCarriers();
		nomTaxiTimes = new int[nAprts][nCarriers][2];
		for(int i=0;i<nAprts;i++){
			for(int j=0;j<nCarriers;j++){
				for(int k=0;k<2;k++){
					nomTaxiTimes[i][j][k] = 1+i+j+k;
				}
			}
		}
		
		// Compute estimated on and off times for these flights.
		// First index is airport, second index is arrival or departure
		// and the last index numbers of flight from that airport.
	    estOnNOffTime = 
	    	NaspacBridge.computeEstimatedOnNOffTimes(nas,nomTaxiTimes,fls);
	    int aprt = nas.getAirportIndex("ABQ");
	    assertTrue(estOnNOffTime[aprt][Nas.Ad.DEP.ordinal()][0] == 101);
	    assertTrue(estOnNOffTime[aprt][Nas.Ad.DEP.ordinal()][1] == 102);
	    aprt = nas.getAirportIndex("LAX");
	    assertTrue(estOnNOffTime[aprt][Nas.Ad.DEP.ordinal()][0] == 103);
	    assertTrue(estOnNOffTime[aprt][Nas.Ad.ARR.ordinal()][0] == 552);
	    
	    // Carriers don't exist.
	    for(int i=0;i<fls.length;i++){
	    	fls[i].set(IFlight.Param.CARRIER,-1);
	    }
	    estOnNOffTime = 
	    	NaspacBridge.computeEstimatedOnNOffTimes(nas,nomTaxiTimes,fls);
	    aprt = nas.getAirportIndex("ABQ");
	    assertTrue(estOnNOffTime[aprt][Nas.Ad.DEP.ordinal()][0] == 100);
	    assertTrue(estOnNOffTime[aprt][Nas.Ad.DEP.ordinal()][1] == 100);
	    aprt = nas.getAirportIndex("LAX");
	    assertTrue(estOnNOffTime[aprt][Nas.Ad.DEP.ordinal()][0] == 100);
	    assertTrue(estOnNOffTime[aprt][Nas.Ad.ARR.ordinal()][0] == 550);
	    // Set carriers back.
	    for(int i=0;i<fls.length;i++){
		    if(i%2==0)car = nas.getCarrierIndex(carriers[0]);
			else car = nas.getCarrierIndex(carriers[1]);
			fls[i].set(IFlight.Param.CARRIER,car); 
	    }
	    
	    //--------------------------------------------------------------------
	    // More special cases.  This time nomTaxiTimes for airport is null.
	    //--------------------------------------------------------------------
		nAprts = nas.getNumAirports();
		nomTaxiTimes = new int[nAprts][][];
		
		// Compute estimated on and off times for these flights.
		// First index is airport, second index is arrival or departure
		// and the last index numbers of flight from that airport.
	    estOnNOffTime = 
	    	NaspacBridge.computeEstimatedOnNOffTimes(nas,nomTaxiTimes,fls);
	    aprt = nas.getAirportIndex("ABQ");
	    assertTrue(estOnNOffTime[aprt][Nas.Ad.DEP.ordinal()][0] == 100);
	    assertTrue(estOnNOffTime[aprt][Nas.Ad.DEP.ordinal()][1] == 100);
	    aprt = nas.getAirportIndex("LAX");
	    assertTrue(estOnNOffTime[aprt][Nas.Ad.DEP.ordinal()][0] == 100);
	    assertTrue(estOnNOffTime[aprt][Nas.Ad.ARR.ordinal()][0] == 550);
	    	
    }
    /**
     * Test of readFlightsFromFCnVFRFiles(), addSchedIdsToFlights() and
     *   addEtmsEquipTypeToFlights() methods.
     */
    @Test
    public void testReadFlightsFromFCnVFRFilesNVariousMethodsToAddStuffToFlights(){
    	// Read properties.
    	Properties props = new Properties();
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NaspacBridgeTest.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
    	assertTrue(true == 
    		NaspacBridge.readModelInputProperties(propsFileNPath, props));
    	
    	String baseDir = props.getProperty("baseDir");
		String scenario = props.getProperty("scenario");
		
		// Flight schedule stuff
		String flightSchedSubDir = props.getProperty("flightSchedSubDir");
		String flightSchedDir = baseDir + File.separator + scenario + 
			File.separator + flightSchedSubDir;
		String flightSchedFilePrefix = props.getProperty("flightSchedFilePrefix");
		String flightSchedFileSuffix = props.getProperty("flightSchedFileSuffix");
		String[] days = NaspacBridge.readBaseNForecastDays(
				flightSchedDir,flightSchedFilePrefix);
		String baseDay = days[0];
		String forecastDay = days[1];
		String flightSchedFile = flightSchedFilePrefix + baseDay + "_" +
			forecastDay + flightSchedFileSuffix;
		File flightSchedFileNPath = new File(flightSchedDir + File.separator +
				flightSchedFile);
		
		// Find crossings and vfr flights stuff.
		String findCrossingsFileSuffix = props.getProperty("findCrossingsFileSuffix");
		String vfrFlightsFileSuffix = props.getProperty("vfrFlightsFileSuffix");
		String findCrossingsFile = scenario + findCrossingsFileSuffix;
		String vfrFlightsFile = scenario + vfrFlightsFileSuffix;
		String aircraftInterSubDir = props.getProperty("aircraftInterSubDir");
		String aircraftInterDir = baseDir + File.separator + scenario +
			File.separator + aircraftInterSubDir;
		File findCrossingsFileNPath = new File(aircraftInterDir + 
			File.separator + findCrossingsFile);
		File vfrFlightsFileNPath = new File(aircraftInterDir + 
			File.separator + vfrFlightsFile);
		
		// Get the list of airports, air carriers and aircraft types.
		// These will be all the airports to be modeled and the other stuff
		// will be used to classify flights.  Needed to create the Nas 
		// object.
		String[] indxToAprtMap = new String[1];
		String[] indxToCarrierMap = new String[1];
		String[] indxToEtmsEquipMap = new String[1];
		String[] indxToBadaEquipMap = new String[1];
		String[][] indxToFixMap = new String[2][1];
		List<String> aprtList = new ArrayList<String>();
		List<String> carrierList = new ArrayList<String>();
		List<String> etmsEquipList = new ArrayList<String>();
		List<String> badaEquipList = new ArrayList<String>();
		List<String> depFixList = new ArrayList<String>();
		List<String> arrFixList = new ArrayList<String>();
		
		//boolean success = NaspacBridge.determineAirportsCarriersBadaTypesFromFlights(
		//		findCrossingsFileNPath,vfrFlightsFileNPath,
		//		aprtList,carrierList,badaList);
		boolean success = NaspacBridge.readAprtCarrierEquipTypeFromFlightSched(
			flightSchedFileNPath, aprtList, carrierList, etmsEquipList, 
			badaEquipList);
		assertTrue(success);
		indxToAprtMap = aprtList.toArray(indxToAprtMap);
		indxToCarrierMap = carrierList.toArray(indxToCarrierMap);
		indxToEtmsEquipMap = etmsEquipList.toArray(indxToEtmsEquipMap);
		indxToBadaEquipMap = badaEquipList.toArray(indxToBadaEquipMap);
		
		// Get fix names from FC file because if they aren't in there, then
		// they aren't needed.
		success = NaspacBridge.readFixNamesFromFCFile(
				findCrossingsFileNPath,depFixList,arrFixList);
		assertTrue(success);	
		indxToFixMap[Nas.Ad.DEP.ordinal()] = 
			depFixList.toArray(indxToFixMap[Nas.Ad.DEP.ordinal()]);
		indxToFixMap[Nas.Ad.ARR.ordinal()] = 
			arrFixList.toArray(indxToFixMap[Nas.Ad.ARR.ordinal()]);
		
		// With airports and delt create instance of Nas class.
		Nas.destroyInstance();
		Nas.createInstance(
			indxToAprtMap,indxToCarrierMap,indxToEtmsEquipMap,indxToFixMap);
		Nas nas = Nas.getInstance();
    	
		//----------------------------------------------------------------------
		// readFlightsFromFCnVFRFiles
		//----------------------------------------------------------------------
    	IFlight[] flights = 
    		NaspacBridge.readFlightsFromFCnVFRFiles(nas, 
    				findCrossingsFileNPath, vfrFlightsFileNPath);
    	
    	assertTrue(flights != null);
    	assertTrue(flights.length == 53720);
    	
    	// Have the flight legs been set?
    	int legNum = 0;
    	for(int i=0;i<flights.length;i++){
    		legNum = flights[i].get(IFlight.Param.LEG_NUM);
    		if(legNum > 1){
    			IFlight f = flights[i].getPrevLeg();
    			assertTrue(f != null);
    			assertTrue(f.getNextLeg() == flights[i]);
    		}
    	}
    	
    	// Individual flights.
    	IFlight f1 = flights[0];
    	assertTrue(1274 == nas.getAirportIndex("JFK"));
    	assertTrue(1843 == nas.getAirportIndex("OMDB"));
    	assertTrue(914 == nas.getCarrierIndex("UAE"));
    	assertTrue(74 == nas.getEquipTypeIndex("B744"));
    	assertTrue(37 == nas.getFixIndex(Nas.Ad.DEP, "MARES"));
    	assertTrue(0 == nas.getFixIndex(Nas.Ad.ARR, "????"));
    	IFlight f2 = flights[1];
    	assertTrue(1411 == nas.getAirportIndex("LIRF"));
    	assertTrue(1571 == nas.getAirportIndex("MIA"));
    	assertTrue(56 == nas.getCarrierIndex("AZA"));
    	assertTrue(80 == nas.getEquipTypeIndex("B772"));
    	assertTrue(0 == nas.getFixIndex(Nas.Ad.DEP, "????"));
    	assertTrue(18 == nas.getFixIndex(Nas.Ad.ARR, "EEONS"));
    	IFlight flast = flights[flights.length-1];
    	assertTrue(2317 == nas.getAirportIndex("TCM"));
    	assertTrue(2271 == nas.getAirportIndex("SUU"));
    	assertTrue(293 == nas.getCarrierIndex("E"));
    	assertTrue(57 == nas.getEquipTypeIndex("B703"));
    	assertTrue(0 == nas.getFixIndex(Nas.Ad.DEP, "????"));
    	assertTrue(0 == nas.getFixIndex(Nas.Ad.ARR, "????"));
    	int MIN_IN_SECS = 60; // conversion from Naspac time in mins to secs.
    	int[] p1Act =  {1,1,-1,1274,1843,914,74,
    			1620*MIN_IN_SECS,1620*MIN_IN_SECS,0,0,0,694*MIN_IN_SECS,0,0,
    			2385*MIN_IN_SECS,0,0,30*MIN_IN_SECS,0,0,0,37,0,0,0};
    	int[] p2Act =  {2,1,-1,1411,1571,56,80,
    			1885*MIN_IN_SECS,1885*MIN_IN_SECS,0,0,0,662*MIN_IN_SECS,0,0,
    			2535*MIN_IN_SECS,0,0,30*MIN_IN_SECS,0,0,0,0,0,18,0};
    	int[] plastAct={31977,1,-1,2317,2271,293,57,
    			1419*MIN_IN_SECS,1419*MIN_IN_SECS,0,0,0,79*MIN_IN_SECS,0,0,
    			1521*MIN_IN_SECS,0,0,30*MIN_IN_SECS,0,0,0,0,0,0,0};
    	IFlight.Param[] vals = IFlight.Param.values();
    	assertTrue(p1Act.length == vals.length);
    	assertTrue(plastAct.length == vals.length);
    	for(int i=0;i<p1Act.length;i++){
    		assertTrue(p1Act[vals[i].ordinal()] == f1.get(vals[i]));
    		assertTrue(p2Act[vals[i].ordinal()] == f2.get(vals[i]));
    		assertTrue(plastAct[vals[i].ordinal()] == flast.get(vals[i]));
    	}
   	
    	//----------------------------------------------------------------------
		// addIdsToFlights
		//----------------------------------------------------------------------
    	// Add flight ID to flights.  Use the .MAP.Itinerary output
		// file from the TrajectoryModeler.  Needless to say, the flight
    	// itin and leg nums must already be included.
		String itin2FlightMapFileSuffix = 
			props.getProperty("itin2FlightMapFileSuffix");
		File itin2FlightMapFileNPath = new File(aircraftInterDir +
			File.separator + scenario + itin2FlightMapFileSuffix);
		success = NaspacBridge.addSchedIdsToFlights(itin2FlightMapFileNPath,flights);
		assertTrue(success);
		int schId = flights[0].get(IFlight.Param.SCHED_ID);
		assertTrue(schId == 29307);
		schId = flights[flights.length-1].get(IFlight.Param.SCHED_ID);
		assertTrue(schId == 3);
		
		//----------------------------------------------------------------------
		// addEtmsEquipTypeToFlights
		//----------------------------------------------------------------------
    	// Add flight ID to flights.  Use the .MAP.Itinerary output
		// file from the TrajectoryModeler.  Needless to say, the flight
    	// itin and leg nums must already be included.
		success = NaspacBridge.addEtmsEquipTypeToFlights(nas, 
				flightSchedFileNPath, flights);
		assertTrue(success);
		// itin 1, leg 1 => flight ID 29307 and ETMS equip Type A380.
		int indx = 0;
		schId = flights[indx].get(IFlight.Param.SCHED_ID);
		int equipTypeIndx = flights[indx].get(IFlight.Param.EQUIP_TYPE);
		String equipType = nas.getEquipTypeFromIndex(equipTypeIndx);
		assertTrue(schId == 29307);
		assertTrue(equipType.equals("A380"));
		// itin 8, leg 2 => flight ID 52881 and ETMS equip Type B773.
		indx = 11;
		schId = flights[indx].get(IFlight.Param.SCHED_ID);
		equipTypeIndx = flights[indx].get(IFlight.Param.EQUIP_TYPE);
		equipType = nas.getEquipTypeFromIndex(equipTypeIndx);
		assertTrue(schId == 52881);
		assertTrue(equipType.equals("B773"));
		// itin 31977, leg 1 => flight ID 3 and ETMS equip Type K35R.
		indx = flights.length-1;
		schId = flights[indx].get(IFlight.Param.SCHED_ID);
		equipTypeIndx = flights[indx].get(IFlight.Param.EQUIP_TYPE);
		equipType = nas.getEquipTypeFromIndex(equipTypeIndx);
		assertTrue(schId == 3);
		assertTrue(equipType.equals("K35R"));
		
		//----------------------------------------------------------------------
		// readParamTimesNAddToFlights
		//----------------------------------------------------------------------
		// Read turn times file and add them to flight array.
		String turnTimesFilePrefix = props.getProperty("turnTimesFilePrefix");
		String paramTimesFileSuffix = props.getProperty("paramTimesFileSuffix");
		String turnTimesFile = turnTimesFilePrefix + baseDay.substring(0,4) +
			paramTimesFileSuffix;
		String paramTimesSubDir = props.getProperty("paramTimesSubDir");
		File paramTimesFileNPath = new File(baseDir + File.separator +
			paramTimesSubDir + File.separator + turnTimesFile);
		// Read equip type map file.
		String equipTypeMapFile = props.getProperty("equipTypeMapFile");
		File equipTypeMapFileNPath = new File(baseDir + File.separator +
			paramTimesSubDir + File.separator + equipTypeMapFile);
		int paramColIndx = 4;// turnaround column
		IFlight.Param paramName = IFlight.Param.TURN_TIME;
		int nDef = NaspacBridge.readParamTimesNAddToFlights(
						nas,paramName,paramTimesFileNPath,
						equipTypeMapFileNPath,paramColIndx,flights);
		assertTrue(nDef == 26605);
		
		// Read pushback times and add to array.
		String pushTimesFilePrefix = 
			props.getProperty("pushbackTimesFilePrefix");
		String pushTimesFile = pushTimesFilePrefix + baseDay.substring(0,4) +
			paramTimesFileSuffix;
		paramTimesFileNPath = new File(baseDir + File.separator +
				paramTimesSubDir + File.separator + pushTimesFile);
		paramColIndx = 3;// pushback column
		paramName = IFlight.Param.PUSHBACK_TIME;
		nDef = NaspacBridge.readParamTimesNAddToFlights(
						nas,paramName,paramTimesFileNPath,
						equipTypeMapFileNPath,paramColIndx,flights);
		assertTrue(nDef == 15455);
		
		// Flight 2 AZA, MIA, B772, tTime = 3837
		IFlight fl = flights[2];
		int carIndx = fl.get(IFlight.Param.CARRIER);
		assertTrue("AZA".equals(nas.getCarrierFromIndex(carIndx)));
		int aprtIndx = fl.get(IFlight.Param.DEP_APRT);
		assertTrue("MIA".equals(nas.getAirportFromIndex(aprtIndx)));
		equipTypeIndx = fl.get(IFlight.Param.EQUIP_TYPE);
		assertTrue("B772".equals(nas.getEquipTypeFromIndex(equipTypeIndx)));
		assertTrue(3268 == fl.get(IFlight.Param.TURN_TIME));
		assertTrue(-357 == fl.get(IFlight.Param.PUSHBACK_TIME));
		
		// Flight 21 COA, TPA, B735, tTime = 2939
		fl = flights[21];
		carIndx = fl.get(IFlight.Param.CARRIER);
		assertTrue("COA".equals(nas.getCarrierFromIndex(carIndx)));
		aprtIndx = fl.get(IFlight.Param.DEP_APRT);
		assertTrue("TPA".equals(nas.getAirportFromIndex(aprtIndx)));
		equipTypeIndx = fl.get(IFlight.Param.EQUIP_TYPE);
		assertTrue("B735".equals(nas.getEquipTypeFromIndex(equipTypeIndx)));
		assertTrue(2039 == fl.get(IFlight.Param.TURN_TIME));
		assertTrue(-623 == fl.get(IFlight.Param.PUSHBACK_TIME));
		
		// Flight end E, TCM, K35R, tTime = 2081
		fl = flights[flights.length-1];
		carIndx = fl.get(IFlight.Param.CARRIER);
		assertTrue("E".equals(nas.getCarrierFromIndex(carIndx)));
		aprtIndx = fl.get(IFlight.Param.DEP_APRT);
		assertTrue("TCM".equals(nas.getAirportFromIndex(aprtIndx)));
		equipTypeIndx = fl.get(IFlight.Param.EQUIP_TYPE);
		assertTrue("K35R".equals(nas.getEquipTypeFromIndex(equipTypeIndx)));
		assertTrue(1732 == fl.get(IFlight.Param.TURN_TIME));
		assertTrue(-78 == fl.get(IFlight.Param.PUSHBACK_TIME));
		
    } 
    
    /**
     * Test of addSchedIdsToFlights done already in the above.
     */
    @Test
    public void testAddSchedIdsToFlights(){
    	assertTrue(true);
    }
    /**
     * Test of addEtmsEquipTypeToFlights done already in the above.
     */
    @Test
    public void testAddEtmsEquipTypeToFlights(){
    	assertTrue(true);
    }
    /**
     * Test of readTurnTimesNAddToFlights done already in the above.
     */
    @Test
    public void testReadTurnTimesNAddToFlights(){
    	assertTrue(true);
    }
    
    /**
     * test of createFlightRoutes() method.
     */
    @Test
    public void testCreateFlightRoutes(){
    	int[] aprts = {0,1};
    	int[] depF = {0,1};
    	int[] arrF = {0,1};
    	// Terminals,taxiways and runways.
    	INode[] terminals = new Terminal[2];
    	INode[] taxiways  = new Taxiway[2];
    	INode[] runways = new Runway[2];
    	INode[] depFixes = new Fix[2];
    	INode[] arrFixes = new Fix[2];
    	for(int i=0;i<2;i++){
    		terminals[i]= new Terminal(aprts[i],null,null);
    		taxiways[i] = new Taxiway(aprts[i],null,null);
    		runways[i]  = new Runway(aprts[i],0,false,null,null,null,null);
    		depFixes[i] = new Fix(depF[i],Nas.Ad.DEP,0);
    		arrFixes[i] = new Fix(arrF[i],Nas.Ad.ARR,0);	
    	}
    	
    	// Flights
    	IFlight[] flights = new Flight[4];
    	int[] pars = new int[IFlight.Param.values().length];
		for(int i=0;i<pars.length;i++){
			pars[i] = -1;
		}
		for(int i=0;i<flights.length;i++){
			if(i%2 == 0){
				pars[IFlight.Param.DEP_APRT.ordinal()] = aprts[0];
				pars[IFlight.Param.ARR_APRT.ordinal()] = aprts[1];
				pars[IFlight.Param.DEP_FIX.ordinal()] = depF[0];
				pars[IFlight.Param.ARR_FIX.ordinal()] = arrF[0];
			} else {
				pars[IFlight.Param.DEP_APRT.ordinal()] = aprts[1];
				pars[IFlight.Param.ARR_APRT.ordinal()] = aprts[0];
				pars[IFlight.Param.DEP_FIX.ordinal()] = depF[1];
				pars[IFlight.Param.ARR_FIX.ordinal()] = arrF[1];
			}
			flights[i] = new Flight(pars);
		}
		// Create routes from flights and runways.
    	assertTrue(NaspacBridge.createFlightRoutes(flights,
    		terminals,taxiways,runways,depFixes,arrFixes));
    	
    	// Test.
    	for(int i=0;i<flights.length;i++){
    		IRoute rte = flights[i].getRoute();
    		if(i%2 == 0){
    			assertTrue(rte.getNextNode() == terminals[0]);
    			assertTrue(rte.getNextNode() == taxiways[0]);
    			assertTrue(rte.getNextNode() == runways[0]);
    			assertTrue(rte.getNextNode() == depFixes[0]);
    			assertTrue(rte.getNextNode() == arrFixes[0]);
    			assertTrue(rte.getNextNode() == runways[1]);
    			assertTrue(rte.getNextNode() == taxiways[1]);
    			assertTrue(rte.getNextNode() == terminals[1]);
    		}else {
    			assertTrue(rte.getNextNode() == terminals[1]);
    			assertTrue(rte.getNextNode() == taxiways[1]);
    			assertTrue(rte.getNextNode() == runways[1]);
    			assertTrue(rte.getNextNode() == depFixes[1]);
    			assertTrue(rte.getNextNode() == arrFixes[1]);
    			assertTrue(rte.getNextNode() == runways[0]);
    			assertTrue(rte.getNextNode() == taxiways[0]);
    			assertTrue(rte.getNextNode() == terminals[0]);
    		}
    	}
    	
    }
    
    /**
     * Test of readNominalTaxiTimes() method.
     */
    @Test
    public void testReadNominalTaxiTimes(){
    	// Read properties.
    	Properties props = new Properties();
		String rootPath = "C:\\Documents and Settings\\James CTR Donley";
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NaspacBridgeTest.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
    	assertTrue(true == 
    		NaspacBridge.readModelInputProperties(propsFileNPath, props));	
    	
    	String baseDir = props.getProperty("baseDir");
		String scenario = props.getProperty("scenario");
		
		String baseDay = props.getProperty("baseDay");
		String forecastDay = props.getProperty("forecastDay");
		
		String findCrossingsFileSuffix = props.getProperty("findCrossingsFileSuffix");
		String vfrFlightsFileSuffix = props.getProperty("vfrFlightsFileSuffix");
		String findCrossingsFile = scenario + findCrossingsFileSuffix;
		String vfrFlightsFile = scenario + vfrFlightsFileSuffix;
		String aircraftInterSubDir = props.getProperty("aircraftInterSubDir");
		String aircraftInterDir = baseDir + File.separator + scenario +
			File.separator + aircraftInterSubDir;
		File findCrossingsFileNPath = new File(aircraftInterDir + 
			File.separator + findCrossingsFile);
		File vfrFlightsFileNPath = new File(aircraftInterDir + 
			File.separator + vfrFlightsFile);
		
		// Get the list of airports, air carriers and aircraft types.
		// These will be all the airports to be modeled and the other stuff
		// will be used to classify flights.  Needed to create the Nas 
		// object.
		String[] indxToAprtMap = new String[1];
		String[] indxToCarrierMap = new String[1];
		String[] indxToBadaMap = new String[1];
		List<String> aprtList = new ArrayList<String>();
		List<String> carrierList = new ArrayList<String>();
		List<String> badaList = new ArrayList<String>();
		boolean success = NaspacBridge.determineAirportsCarriersBadaTypesFromFlights(
				findCrossingsFileNPath,vfrFlightsFileNPath,
				aprtList,carrierList,badaList);
		assertTrue(success);
		indxToAprtMap = aprtList.toArray(indxToAprtMap);
		indxToCarrierMap = carrierList.toArray(indxToCarrierMap);
		indxToBadaMap = badaList.toArray(indxToBadaMap);
		
		// With airports and delt create instance of Nas class.
		Nas.destroyInstance();
		Nas.createInstance(indxToAprtMap,indxToCarrierMap,indxToBadaMap,null);
		Nas nas = Nas.getInstance();
    	
		String taxiTimesDir = baseDir + File.separator +
			props.getProperty("taxiTimesSubDir");
		String taxiTimesFilePrefix = props.getProperty("taxiTimesFilePrefix");
		String taxiTimesFileSuffix = props.getProperty("taxiTimesFileSuffix");
		String year = baseDay.substring(0,4);
		String taxiTimesFile = taxiTimesFilePrefix + year + taxiTimesFileSuffix;
		File taxiTimesFileNPath = new File(taxiTimesDir + File.separator +
			taxiTimesFile);
    	int[][][] nomTaxiTimes = 
    		NaspacBridge.readNominalTaxiTimes(nas,baseDay,taxiTimesFileNPath);
    	assertTrue(nomTaxiTimes != null);
    	// Check some airports.
    	String aprt = "TPA";
    	String carrier = "AAL";
    	int aprtIndx = nas.getAirportIndex(aprt);
    	int carrierIndx = nas.getCarrierIndex(carrier);
    	int[] times = new int[2];
    	int minInSecs = 60;
    	times[Nas.Ad.DEP.ordinal()] = (int)(10.6*minInSecs + 0.5);
    	times[Nas.Ad.ARR.ordinal()] = (int)(3.8*minInSecs + 0.5);
    	for(int i=0;i<2;i++){
    		assertTrue(times[i] == nomTaxiTimes[aprtIndx][carrierIndx][i]);
    	}
    	carrier = "ZZZ";
    	// All carriers.  Have for quarter 3 except FFT.
    	String[] carsAll = {"AAL","ACA","COA","COM","DAL","FDX","JBU","NWA",
    		"SWA","TRS","UAL","USA"};
    	aprtIndx = nas.getAirportIndex(aprt);
    	carrierIndx = nas.getCarrierIndex(carrier);
    	times[Nas.Ad.DEP.ordinal()] = (int)(9.8*minInSecs + 0.5); 
    	times[Nas.Ad.ARR.ordinal()] = (int)(4.1*minInSecs + 0.5);
    	
    	for(int i=0;i<nas.getNumCarriers();i++){
    		// All carriers except those listed in carsAll above.
    		success = false;
    		for(String car : carsAll){
    			if(i == nas.getCarrierIndex(car)){
    				success = true;
    				break;
    			}
    		}
    		if(success == false){
	    		for(int j=0;j<2;j++){
	    			assertTrue(times[j] == nomTaxiTimes[aprtIndx][i][j]);
	    		}
    		}
    	}
    }
    
    /**
     * Test initialize().  The big kahuna.
     */
    @Test
	public void testInitialize()
    {
    	// Constructor.
    	Properties props = new Properties();
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NaspacBridgeTest.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
		assertTrue(true == 
			NaspacBridge.readModelInputProperties(propsFileNPath, props));
		
    	NaspacBridge nb = new NaspacBridge(propsFileNPath);
    	assertTrue(nb != null);
    	
    	ISimElements se = nb.initialize();
    	assertTrue(se != null);
    	
    	// Check if the correct number of flights are present and they are
    	// all non-null.
    	IFlight[] fls = se.getFlights();
    	int nFlights = fls.length;
    	assertTrue(nFlights == 53720);
    	for(IFlight f : fls){
    		assertTrue(f != null);
    	}
    	// Check if the correct number of nodes are present and they are
    	// all non-null.
    	INode[] nodes = se.getNodes();
    	int nNodes = nodes.length;
    	Nas nas = Nas.getInstance();
    	int nAprts = nas.getNumAirports();
    	int nDepFix = nas.getNumFixes(Nas.Ad.DEP);
    	int nArrFix = nas.getNumFixes(Nas.Ad.ARR);
    	assertTrue(nNodes == 3*nAprts + nDepFix + nArrFix);
    	for(INode n : nodes){
    		assertTrue(n != null);
    	}
    	String bDay = se.getBaseDay();
    	assertTrue(bDay.equals("20080820"));
    	String fDay = se.getForecastDay();
    	assertTrue(fDay.equals("20080820"));
    }
    
    
    
    /**
	 * Test of exception.
	 */
	//@Test(expected=IllegalArgumentException.class)
	//public void testIllArgException() {	
	//}

	 /**
     * Test of determineAirportsFromFlights() method.
     */
 /*
    @Test
    public void testDetermineAirportsFromFlights(){
    	// Read properties.
    	Properties props = new Properties();
		String rootPath = "C:\\Documents and Settings\\James CTR Donley";
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NaspacBridgeTest.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
    	assertTrue(true == 
    		NaspacBridge.readModelInputProperties(propsFileNPath, props));
    	
    	// Get flight file name.
    	String baseDir = props.getProperty("baseDir");
		String scenario = props.getProperty("scenario");
		String baseDay  = props.getProperty("baseDay");
		String forecastDay = props.getProperty("forecastDay");
		String flightSchedPrefix = props.getProperty("flightSchedFilePrefix");
		String flightSchedSuffix = props.getProperty("flightSchedFileSuffix");
		String flightSchedFile = flightSchedPrefix + baseDay + "_" + 
			forecastDay + flightSchedSuffix;
		String flightSchedSubDir = props.getProperty("flightSchedSubDir");
		File flightSchedFileNPath = new File(baseDir + File.separator +
			scenario + File.separator + flightSchedSubDir + File.separator +
			flightSchedFile);
    	
    	String[] indxToAprtMap = null;
    	indxToAprtMap = 
    		NaspacBridge.determineAirportsFromFlights(flightSchedFileNPath);
    	assertTrue(indxToAprtMap != null);
    	assertTrue(indxToAprtMap.length == 2776);
    }
 */
	 /**
     * Test of determineAirportsFromAircraftFile() method.
     */
  /*
    @Test
    public void testDetermineAirportsFromAircraftFile(){
    	// Read properties.
    	Properties props = new Properties();
		String rootPath = "C:\\Documents and Settings\\James CTR Donley";
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NaspacBridgeTest.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
    	assertTrue(true == 
    		NaspacBridge.readModelInputProperties(propsFileNPath, props));
    	
    	// Get flight file name.
    	String baseDir = props.getProperty("baseDir");
		String scenario = props.getProperty("scenario");
		String aircraftFilePrefix = props.getProperty("aircraftFilePrefix");
		String aircraftFile = aircraftFilePrefix + scenario;
		String aircraftFileSubDir = props.getProperty("aircraftFileSubDir");
		File aircraftFileNPath = new File(baseDir + File.separator +
			scenario + File.separator + aircraftFileSubDir + File.separator +
			aircraftFile);
    	
    	String[] indxToAprtMap = null;
    	indxToAprtMap = 
    		NaspacBridge.determineAirportsFromAircraftFile(aircraftFileNPath);
    	assertTrue(indxToAprtMap != null);
    	assertTrue(indxToAprtMap.length == 2776);
    }
 */  
	
}
