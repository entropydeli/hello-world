package org.cna.donley.nassim2_4;

import org.cna.donley.jdbc.SQLDate2;
import org.cna.donley.utils.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;


/**
 * This is an implementation of the {@link ISimInitializer} interface.
 * I call this a bridge because it links the output of the NASPAC
 * preprocessor, mostly the TrajectoryModeler and ChangeFileGenerator 
 * classes.  The idea behind this is that the sim shouldn't care where 
 * it gets its initial data, nor the specifics of getting it, so all that
 * is done here. 
 * <p>
 * Anyways, this class creates the flights and NAS nodes from output of the
 * NASPAC preprocessor.  I have tried to avoid using data as formatted
 * for the NASPAC Core simulation (written in Simscript) as that is 
 * hard to read.  The preferences here are for output in the 
 * scenario subdirectory pre-output and if the data isn't there then
 * use data from the pre-data subdirectory.  Most of the flight
 * info is obtained from the forecast.txt.Itinerary.BadaReMap file, where
 * "forecast.txt" is the original flight schedule input to NASPAC.  
 * Additional info such as taxi times are obtained straight from the
 * ETMS database.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: NaspacBridge.java 1 2009-06-01 00:00:00EST $
 */

public class NaspacBridge implements ISimInitializer
{
	/**
	 * Properties of this specific bridge.  Contains info about where
	 * various input data files are, the desired sim time (to be consistent
	 * with NASPAC), etc.
	 */
	private Properties props = null;
	
	/**
	 * Name of class.
	 */
	private static String className = null;
	
	/**
	 * Constructor.  Takes a properties file of data specific to this
	 * bridge and stores it in the local properties file.
	 * @param naspacPropsFileNPath File and path of the Naspac bridge
	 *   properties file.
	 */
	public NaspacBridge(File naspacPropsFileNPath){
		className = this.getClass().getName();
		props = new Properties();
		if(!readModelInputProperties(naspacPropsFileNPath,props)){
			final String method = this.getClass().getName();
			throw new IllegalArgumentException(method + ": problem reading " +
				"properties file: " + naspacPropsFileNPath);			
		}
	}
	
	/**
	 * This method initializes the simulation using the preprocessor output
	 * of NASPAC as the starting point.  Need the following files from 
	 * NASPAC: Airport Capacity, Change Capacity, Airport Pareto Curves, 
	 * Itin/Leg to Flight ID Map, the find crossings trajectory file and
	 * the VFR flights file.  Last, get base and forecast days from the
	 * original flight schedule file.
	 * @return An object that implements the interface {@link ISimElements}
	 *   The object contains an array of {@link IFlight} flights, one 
	 *   of {@link INode} NAS nodes, and the base and forecast simulation days.
	 *   They are set up to reflect the NAS that the NASPAC core uses, 
	 *   though right now only airport nodes are considered.
	 */
	public ISimElements initialize(){
		final String method = className + ".initialize()";
		String baseDay = null, forecastDay = null;
		IFlight[] flights = null;
		
		String baseDir = props.getProperty("baseDir");
		String scenario = props.getProperty("scenario");
		
		//--------------------------------------------------------------------
		// Get base and forecast days from name of flight schedule file.
		//--------------------------------------------------------------------
		String flightSchedSubDir = props.getProperty("flightSchedSubDir");
		String flightSchedDir = baseDir + File.separator + scenario + 
			File.separator + flightSchedSubDir;
		String flightSchedFilePrefix = props.getProperty("flightSchedFilePrefix");
		String flightSchedFileSuffix = props.getProperty("flightSchedFileSuffix");
		String[] days = NaspacBridge.readBaseNForecastDays(
				flightSchedDir,flightSchedFilePrefix);
		if(days == null){
			final String callMethod = className + ".readBaseNForecaseDays()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		baseDay = days[0];
		forecastDay = days[1];
		props.setProperty("baseDay", baseDay);
		props.setProperty("forecastDay", forecastDay);
		
		//---------------------------------------------------------------------
		// Create Nas singleton.
		//---------------------------------------------------------------------
		//Read pre-processor output flight files to get flight and other info.
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
		String flightSchedFile = flightSchedFilePrefix + baseDay + "_" +
			forecastDay + flightSchedFileSuffix;
		File flightSchedFileNPath = new File(flightSchedDir + File.separator +
			flightSchedFile);
		
		// Get the list of airports, air carriers, etms aircraft types and
		// arrival and departure fixes.
		// These will be all the airports to be modeled and the other stuff
		// will be used to classify flights.  Needed to create the Nas 
		// object. Use ETMS instead of BADA because Dan argues that BADA
		// maps to ETMS types can possibly change over time, while ETMS types
		// do not.
		String[] indxToAprtMap = new String[1];
		String[] indxToCarrierMap = new String[1];
		String[] indxToEquipTypeMap = new String[1];
		String[][] indxToFixMap = new String[2][1];
		List<String> aprtList = new ArrayList<String>();
		List<String> carrierList = new ArrayList<String>();
		List<String> etmsEquipList = new ArrayList<String>();
		List<String> depFixList = new ArrayList<String>();
		List<String> arrFixList = new ArrayList<String>();
		List<String> badaEquipList = new ArrayList<String>();
		
		if(!readAprtCarrierEquipTypeFromFlightSched(flightSchedFileNPath,
			aprtList, carrierList,etmsEquipList,badaEquipList)){
			final String callMethod = className + 
				".readAprtCarrierEquipTypeFromFlightSched()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		indxToAprtMap      = aprtList.toArray(indxToAprtMap);
		indxToCarrierMap   = carrierList.toArray(indxToCarrierMap);
		indxToEquipTypeMap = etmsEquipList.toArray(indxToEquipTypeMap);
		
		// Get fix names from FC file because if they aren't in there, then
		// they aren't needed.
		if(!readFixNamesFromFCFile(findCrossingsFileNPath,depFixList,arrFixList)){
			final String callMethod = className + ".readFixNamesFromFCFile()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		indxToFixMap[Nas.Ad.DEP.ordinal()] = 
			depFixList.toArray(indxToFixMap[Nas.Ad.DEP.ordinal()]);
		indxToFixMap[Nas.Ad.ARR.ordinal()] = 
			arrFixList.toArray(indxToFixMap[Nas.Ad.ARR.ordinal()]);
		
		
		// With airports and other crap create instance of Nas class.
		Nas.destroyInstance();
		Nas.createInstance(indxToAprtMap,indxToCarrierMap,indxToEquipTypeMap,
			indxToFixMap);
		Nas nas = Nas.getInstance();
		
		//---------------------------------------------------------------------
		// Create flight, i.e., {@link IFlight}, elements and populate them.
		//---------------------------------------------------------------------
		// Open and read the flights files. Are using the find crossings
		// and VFR flights file from the preprocessor output.
		if((flights = readFlightsFromFCnVFRFiles(nas, 
			findCrossingsFileNPath, vfrFlightsFileNPath)) == null){
			final String callMethod = className +  
				".readFlightsFromFCnVFRFiles()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		
		// Add flight ID to flights.  Use the .MAP.Itinerary output
		// file from the TrajectoryModeler.
		String itin2FlightMapFileSuffix = 
			props.getProperty("itin2FlightMapFileSuffix");
		File itin2FlightMapFileNPath = new File(aircraftInterDir +
			File.separator + scenario + itin2FlightMapFileSuffix);
		if(!addSchedIdsToFlights(itin2FlightMapFileNPath,flights)){
			final String callMethod = className +  
				".addSchedIdsToFlights()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		
		// Add ETMS equipment types to flight objects.
		if(!addEtmsEquipTypeToFlights(nas,flightSchedFileNPath,flights)){
			final String callMethod = className + 
				".addEtmsEquipTypeToFlights()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		
		//----------------------------------------------------------------------
		// Read turnaround, pushback, taxi-in and taxi-out data.
		//----------------------------------------------------------------------
		// And add them to the flight array.
		String turnTimesFilePrefix = props.getProperty("turnTimesFilePrefix");
		String pushTimesFilePrefix = props.getProperty("pushbackTimesFilePrefix");
		String taxiInTimesFilePrefix =props.getProperty("taxiInTimesFilePrefix");
		String taxiOutTimesFilePrefix=props.getProperty("taxiOutTimesFilePrefix");
		String paramTimesFileSuffix = props.getProperty("paramTimesFileSuffix");
		String paramTimesSubDir = props.getProperty("paramTimesSubDir");
		// Read equip type map file.
		String equipTypeMapFile = props.getProperty("equipTypeMapFile");
		File equipTypeMapFileNPath = new File(baseDir + File.separator +
			paramTimesSubDir + File.separator + equipTypeMapFile);
		
		// Turnaround data
		String paramTimesFile = turnTimesFilePrefix + baseDay.substring(0,4) +
			paramTimesFileSuffix;
		File paramTimesFileNPath = new File(baseDir + File.separator +
			paramTimesSubDir + File.separator + paramTimesFile);
		int paramColIndx = 4;// turnaround column
		IFlight.Param paramName = IFlight.Param.TURN_TIME;
		int nDef = NaspacBridge.readParamTimesNAddToFlights(
						nas,paramName,paramTimesFileNPath,
						equipTypeMapFileNPath,paramColIndx,flights);
		if(nDef < 0){
			final String callMethod = className + 
				".readParamTimesNAddToFlights()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		int avg = 0;
		for(int i=0;i<flights.length;i++){
			avg += flights[i].get(IFlight.Param.TURN_TIME);
		}
		avg /= flights.length;
		System.out.println(method + ": turnTimeAvg: " + avg);
		// Pushback data.
		paramTimesFile = pushTimesFilePrefix + baseDay.substring(0,4) +
			paramTimesFileSuffix;
		paramTimesFileNPath = new File(baseDir + File.separator +
				paramTimesSubDir + File.separator + paramTimesFile);
		paramColIndx = 3;// pushback column
		paramName = IFlight.Param.PUSHBACK_TIME;
		nDef = NaspacBridge.readParamTimesNAddToFlights(
							nas,paramName,paramTimesFileNPath,
							equipTypeMapFileNPath,paramColIndx,flights);
		if(nDef < 0){
			final String callMethod = className + 
				".readParamTimesNAddToFlights()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		avg = 0;
		for(int i=0;i<flights.length;i++){
			avg += flights[i].get(IFlight.Param.PUSHBACK_TIME);
		}
		avg /= flights.length;
		System.out.println(method + ": pushbackTimeAvg: " + avg);
		// Taxi-In data.
		paramTimesFile = taxiInTimesFilePrefix + baseDay.substring(0,4) +
		paramTimesFileSuffix;
		paramTimesFileNPath = new File(baseDir + File.separator +
			paramTimesSubDir + File.separator + paramTimesFile);
		paramColIndx = 1;// taxi-in column
		paramName = IFlight.Param.TAXI_IN_TIME;
		nDef = NaspacBridge.readParamTimesNAddToFlights(
						nas,paramName,paramTimesFileNPath,
						equipTypeMapFileNPath,paramColIndx,flights);
		if(nDef < 0){
			final String callMethod = className + 
				".readParamTimesNAddToFlights()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		avg = 0;
		for(int i=0;i<flights.length;i++){
			avg += flights[i].get(IFlight.Param.TAXI_IN_TIME);
		}
		avg /= flights.length;
		System.out.println(method + ": taxiInTimeAvg: " + avg);
		// Taxi-out data.
		paramTimesFile = taxiOutTimesFilePrefix + baseDay.substring(0,4) +
		paramTimesFileSuffix;
		paramTimesFileNPath = new File(baseDir + File.separator +
			paramTimesSubDir + File.separator + paramTimesFile);
		paramColIndx = 2;// pushback column
		paramName = IFlight.Param.TAXI_OUT_TIME;
		nDef = NaspacBridge.readParamTimesNAddToFlights(
						nas,paramName,paramTimesFileNPath,
						equipTypeMapFileNPath,paramColIndx,flights);
		if(nDef < 0){
			final String callMethod = className + 
				".readParamTimesNAddToFlights()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		avg = 0;
		for(int i=0;i<flights.length;i++){
			avg += flights[i].get(IFlight.Param.TAXI_OUT_TIME);
		}
		avg /= flights.length;
		System.out.println(method + ": taxiOutTimeAvg: " + avg);
		// Add routes to flights once network nodes have been created.
		// See below.
		
		//---------------------------------------------------------------------
		// Read Pareto and change capacity files.
		//---------------------------------------------------------------------
		// Read the pareto file. It will implicitly contain the
		// airports to be modeled, so also determine airports to be
		// modeled.
		String paretoFileSubDir = props.getProperty("paretoSubDir");
		String paretoFileName = props.getProperty("paretoFile");
		File paretoFile =  new File(baseDir + File.separator + scenario +
			File.separator + paretoFileSubDir + File.separator + paretoFileName);
		// Get pareto curves.  Need the aprt to index map in {@link Nas}.
		// This should be the number per qtr hour.
		double[][][][] paretoCurves = readParetoCurves(nas,paretoFile);	
		if(paretoCurves == null){
			final String callMethod = className + ".readParetoCurves()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		
		// Open and read the NASPAC airport capacity file to get
		// change airport conditions for all airports.  Here, the
		// first element is the airport, the second the time w/r to
		String changeCapPrefix = props.getProperty("changeCapPrefix");
		String changeCapSubDir = props.getProperty("changeCapSubDir");
		String changeCapFile = changeCapPrefix + baseDay;
		File changeCapFileNPath = new File(baseDir + File.separator + scenario +
			File.separator + changeCapSubDir + File.separator + changeCapFile);
		int[][][] changeCapacities = 
			readChangeCapacities(nas,changeCapFileNPath);	
		if(changeCapacities == null){
			final String callMethod = className + 
				".readChangeCapacities)";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		
		//------------------------------------------------------------------
		// Read historical airport acceptance rates.
		//------------------------------------------------------------------
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
		// Called acceptance rates.  Output should be the number per qtrhour.
		int[][][] calledRates = readAspmAcceptanceRates(nas,
			(new SQLDate2(baseDay,SQLDate2.Element.dd)),acceptRatesFileNPath);
		if(calledRates == null){
			final String callMethod = className +  
				".readAspmAcceptanceRates()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		
		// Pareto or historical? Create flags that tell the runways
		// which method to use to estimate acceptance rates.
		String computeRatesAprts = props.getProperty("computeAcceptRatesAirports");
		boolean[] computeRates = 
			createRatesFlagsForAprts(nas,computeRatesAprts);
		if(computeRates == null){
			final String callMethod = className + 
				".createRatesFlagsForAprts()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		
		//------------------------------------------------------------------
		// Pare airport info. Determine which airports actually should be modeled.
		//------------------------------------------------------------------
		String airportsFile = props.getProperty("modeledAirportsFile");
		String airportsSubDir = props.getProperty("modeledAirportsSubDir");
		File aprtsFileNPath = new File(baseDir + File.separator + airportsSubDir+
			File.separator + airportsFile);
		int[] modeledAprts =
			readModeledAirports(nas,aprtsFileNPath);
		if(modeledAprts == null){
			final String callMethod = className + 
				".readModeledAirports()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		if(!pareAirports(modeledAprts,calledRates,changeCapacities,paretoCurves)){
			final String callMethod = className + 
				".pareAirports()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		
		//------------------------------------------------------------------
		// Read fix delay values from files.
		//------------------------------------------------------------------
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
		if(fixDelays == null){
			final String callMethod = className + ".readFixDelays()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
			
		//------------------------------------------------------------------
		// Create network nodes.
		//------------------------------------------------------------------
		// First, read in nominal taxi times for the airports and carriers.  If 
		// an airport has no taxi info, the element is set to null.
		// It is [aprt][carrier][taxi-in/taxi-out]
		String taxiTimesDir = baseDir + File.separator +
			props.getProperty("taxiTimesSubDir");
		String taxiTimesFilePrefix = props.getProperty("taxiTimesFilePrefix");
		String taxiTimesFileSuffix = props.getProperty("taxiTimesFileSuffix");
		String year = baseDay.substring(0,4);
		String taxiTimesFile = taxiTimesFilePrefix + year + taxiTimesFileSuffix;
		File taxiTimesFileNPath = new File(taxiTimesDir + File.separator +
			taxiTimesFile);
		int[][][] nomTaxiTimes = 
			readNominalTaxiTimes(nas,baseDay,taxiTimesFileNPath);
		if(nomTaxiTimes == null){
			final String callMethod = className + 
				".readNominalTaxiTimes()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		
		// Now, estimate runway on and off times for all flights.  Will be used 
		// by the Runway nodes to determine acceptance rates if change 
		// capacities and pareto curves are used.
		int[][][] estOnNOffTimes = computeEstimatedOnNOffTimes(nas, 
		    nomTaxiTimes,flights); 
		if(estOnNOffTimes == null){
			final String callMethod = className + 
				".computeEstimateOnNOffTimes()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		
		// Create network nodes.
		int numAprts = indxToAprtMap.length;
		INode[] runways   = new Runway[numAprts];
		INode[] taxiways  = new Taxiway[numAprts];
		INode[] terminals = new Terminal[numAprts];
		int startTime = 0; // Naspac times are w/r to midnight of the start date.
		for(int i=0;i<numAprts;i++){
			runways[i] = new Runway(i,startTime,computeRates[i],calledRates[i],
				paretoCurves[i],changeCapacities[i],estOnNOffTimes[i]);
			taxiways[i] = new Taxiway(i,null,null);// taxi-in&out data is in flights.
			terminals[i]= new Terminal(i,null,null);// turn & push time info is
			                                        // in flights.
		}
		int numDepFixes = nas.getNumFixes(Nas.Ad.DEP);
		int numArrFixes = nas.getNumFixes(Nas.Ad.ARR);
		INode[] depFixes = new Fix[numDepFixes];
		INode[] arrFixes = new Fix[numArrFixes];
		int fixDelay = 0;
		for(int i=0;i<nas.getNumFixes(Nas.Ad.DEP);i++){
			fixDelay = fixDelays[Nas.Ad.DEP.ordinal()][i];
			depFixes[i] = new Fix(i,Nas.Ad.DEP,fixDelay);
		}
		for(int i=0;i<nas.getNumFixes(Nas.Ad.ARR);i++){
			fixDelay = fixDelays[Nas.Ad.ARR.ordinal()][i];
			arrFixes[i] = new Fix(i,Nas.Ad.ARR,fixDelay);	
		}
		
		// Put nodes into a single array.
		int nodTot = runways.length + terminals.length + taxiways.length +
			depFixes.length + arrFixes.length;
		INode[] nod = new INode[nodTot];
		int count = 0,countOld=0;
		for(int i=0;i<runways.length;i++){
			nod[i] = runways[i];
			count++;
		}
		countOld = count;
		for(int i=0;i<terminals.length;i++){
			nod[i+countOld] = terminals[i];
			count++;
		}
		countOld = count;
		for(int i=0;i<taxiways.length;i++){
			nod[i+countOld] = taxiways[i];
			count++;
		}
		countOld = count;
		for(int i=0;i<depFixes.length;i++){
			nod[i+countOld] = depFixes[i];
			count++;
		}
		countOld = count;
		for(int i=0;i<arrFixes.length;i++){
			nod[i+countOld] = arrFixes[i];
			count++;
		}
		
		//-----------------------------------------------------------------
		// Create routes for flights based upon network nodes.
		//-----------------------------------------------------------------
		// No sectors, so just terminals, taxiways, runways and fixes.
		if(!createFlightRoutes(flights,terminals,taxiways,runways,depFixes,
				arrFixes)){
			final String callMethod = className + 
				".createFlightRoutes()";
			System.err.println(method + ": " + callMethod + " failed.");
			return null;
		}
		
		// Create Sim Element box and return it.
		// Events are not contained because they are destroyed later in the
		// Sim.
		ISimElements ie = new SimElements(baseDay,forecastDay,flights,nod);
		return ie;
	}
	
	/**
	 * Pares the airports to be modeled.  Input list of airports, as indices,
	 * to model.  Set to null any calledRates, changeCaps or paretoCurves
	 * if they aren't on the list.  If the modeledAprts array is null or
	 * the first element is -1, then don't do anything.  This method is
	 * used if want only a known subset of airports, such as OEP 35, to be
	 * modeled.
	 * @param modeledAprts Array of airports to be modeled.  If null or
	 *   the only element has value -1, then pare airports.
	 * @param calledRates Called rates array. First element is aprt index.
	 * @param changeCaps Change caps array. First element is aprt indx.
	 * @param paretoCurves  Pareto curve array. First element is aprt indx.
	 * @return <code>true</code> if successful; <code>false</code> if not.
	 */
	public static boolean pareAirports(int[] modeledAprts,
		int[][][] calledRates, int[][][] changeCaps,
		double[][][][] paretoCurves){
		
		// If want to model all airports then do nothing.
		if(modeledAprts == null || modeledAprts[0] < 0)return true;
		
		// Set to null any called rates for airports that are not modeled.
		boolean success = false;
		int count = 0;
		for(int i=0;i<calledRates.length;i++){
			success = false;
			count = 0;
			while(!success && count < modeledAprts.length){
				if(i == modeledAprts[count]){
					success = true;
					break;
				} else count++;
			}
			if(!success)calledRates[i] = null;
		}
		
		// Set to null any pareto curves for airports that are not modeled.
		success = false;
		count = 0;
		for(int i=0;i<paretoCurves.length;i++){
			success = false;
			count = 0;
			while(!success && count < modeledAprts.length){
				if(i == modeledAprts[count]){
					success = true;
					break;
				} else count++;
			}
			if(!success)paretoCurves[i] = null;
		}
		// Set to null any change capacities for airports that are not modeled.
		success = false;
		count = 0;
		for(int i=0;i<changeCaps.length;i++){
			success = false;
			count = 0;
			while(!success && count < modeledAprts.length){
				if(i == modeledAprts[count]){
					success = true;
					break;
				} else count++;
			}
			if(!success)changeCaps[i] = null;
		}
		return true;
	}
	
	/**
     * Takes all the flights that are to arrive or depart at this airport and 
     * creates an ordered array of their predicted runway on (arrival) and
     * off (departure) times.  Will be used to predict the number of arrivals 
     * and departures in a qtr hour epoch so as to determine the acceptance rates.  
     * <p>
     * @param nas Instance of {@link Nas} singleton.
     * @param nomTaxiTimes Nominal taxi times for each airport and carriers.
     * @param flights Array of flights
     * @return Array of arrival times, ordered by time. First element is
     *   the airport index, the second element denotes
     *   either arrival or departure and the last is a number of index of
     *   the list of times for that flight.  Times are in ascending order for
     *   each airport and type.
     */
    public static int[][][] computeEstimatedOnNOffTimes(Nas nas, 
    	int[][][] nomTaxiTimes, IFlight[] flights){ 
    	int numAprts = nas.getNumAirports();
    	int[][][] predictedOnNOffTimes = new int[numAprts][2][];
    
    	// First get how many flights by airport and type.
    	int arrAprt = -1, depAprt = -1;
    	int[][] numFlights = new int[numAprts][2];
    	for(int i=0;i<numFlights.length;i++){
    		numFlights[i][0] = 0;
    		numFlights[i][1] = 0;
    	}
    	for(int i=0;i<flights.length;i++){
    		arrAprt = flights[i].get(IFlight.Param.ARR_APRT);
    		depAprt = flights[i].get(IFlight.Param.DEP_APRT);
    		
    		// VFR flights may not have both an arrival and departure airport
    		// so check for that.  This should not be true, but leave code in.
    		if(arrAprt >= 0)numFlights[arrAprt][Nas.Ad.ARR.ordinal()]++;
    		if(depAprt >= 0)numFlights[depAprt][Nas.Ad.DEP.ordinal()]++;	
    	}
    	// Create arrays.
    	for(int i=0;i<numAprts;i++){
    		for(int j=0;j<2;j++){
    			if(numFlights[i][j] > 0){
    				predictedOnNOffTimes[i][j] = new int[numFlights[i][j]];
    			} else predictedOnNOffTimes[i][j] = null;
    		}
    	}
    	// Populate arrays.  Need to estimate the arrival and departure times,
    	// so need the nomTaxiTimes
    	int[][] count = new int[numAprts][2];
    	for(int i=0;i<count.length;i++){
    		for(int j=0;j<count[i].length;j++) count[i][j] = 0;
    	}
    	int carrier=0,schedOutTime=0,airborneTime=0,taxiOutTime=0;
    	int schedInTime=0,taxiInTime=0;
    	for(int i=0;i<flights.length;i++){
    		// Departure
    		depAprt = flights[i].get(IFlight.Param.DEP_APRT);
    		if(depAprt >= 0){
    			// Only consider flights that indeed have a departure airport.
	    		schedOutTime = flights[i].get(IFlight.Param.SCHED_OUT_TIME);
	    		carrier      = flights[i].get(IFlight.Param.CARRIER);
	    		// Account for case in which taxi times info is not available 
	    		// for the airport or the flight carrier is unknown.
	    		if(nomTaxiTimes[depAprt] == null || carrier < 0) taxiOutTime = 0;
	    		else{
	    			taxiOutTime  = nomTaxiTimes[depAprt][carrier][Nas.Ad.DEP.ordinal()];
	    		}
	    		predictedOnNOffTimes[depAprt][Nas.Ad.DEP.ordinal()]
	    		     [count[depAprt][Nas.Ad.DEP.ordinal()]] = 
	    		    	 schedOutTime + taxiOutTime;
	    		count[depAprt][Nas.Ad.DEP.ordinal()]++;
    		}
    		// Arrival
    		arrAprt = flights[i].get(IFlight.Param.ARR_APRT);
    		if(arrAprt >= 0){
    			// Only consider flights that have an arrival airport.
    			if(depAprt >= 0){
		    		airborneTime = flights[i].get(IFlight.Param.ACT_AIR_TIME);
		    		predictedOnNOffTimes[arrAprt][Nas.Ad.ARR.ordinal()]
		    		     [count[arrAprt][Nas.Ad.ARR.ordinal()]] = 
		    		    	 schedOutTime + taxiOutTime + airborneTime;
		    		count[arrAprt][Nas.Ad.ARR.ordinal()]++;
    			}else {
    				// Has no departure airport, so compute back from the
    				// sched arrival time.
    				schedInTime = flights[i].get(IFlight.Param.SCHED_IN_TIME);
    				carrier = flights[i].get(IFlight.Param.CARRIER);
    				// Account for case in which taxi times info is not available 
    	    		// for airport or the carrier is not known.
    				if(nomTaxiTimes[arrAprt] == null || carrier < 0)taxiInTime = 0;
    				else taxiInTime = 
    	    			nomTaxiTimes[arrAprt][carrier][Nas.Ad.ARR.ordinal()];
		    		predictedOnNOffTimes[arrAprt][Nas.Ad.ARR.ordinal()]
		    		     [count[arrAprt][Nas.Ad.ARR.ordinal()]] = 
		    		    	 schedInTime - taxiInTime;
		    		count[arrAprt][Nas.Ad.ARR.ordinal()]++;
    			}
    		}
    	}
    	// Sort by times.
    	for(int i=0;i<numAprts;i++){
    		if(predictedOnNOffTimes[i][Nas.Ad.DEP.ordinal()] != null){
    			Arrays.sort(predictedOnNOffTimes[i][Nas.Ad.DEP.ordinal()]);
    		}
    		if(predictedOnNOffTimes[i][Nas.Ad.ARR.ordinal()] != null){
    			Arrays.sort(predictedOnNOffTimes[i][Nas.Ad.ARR.ordinal()]);
    		}
    	}
    	
    	return predictedOnNOffTimes; 	
    }
	
	/**
	 * Creates an array of flags that tells each airport whether to use
	 * historical acceptance rates or compute them using the change capacities
	 * and Pareto curves.  If the airport is not listed on the input string,
	 * it is assumed that one wants to use historical rates.
	 * @param nas Nas singleton
	 * @param computeRatesAirports A list of airports for which one is to use
	 *   compute the acceptance rate rather than use the historical rates or
	 *   setting them to infinity. The list is comma demarcated.  If there is 
	 *   only one element, then a value of "All" or "None" tells the method to
	 *   have all or none of the airports to compute the acceptance rate.
	 * @return Array of flags that tell each airport whether to use historical
	 *   acceptance rates or compute them using the change capacities and
	 *   Pareto curves.
	 */
	public static boolean[] createRatesFlagsForAprts(Nas nas, 
		String computeRatesAirports){
		boolean[] computeRates = new boolean[nas.getNumAirports()]; 
		String comma = ",";
		String[] computeRatesAprts = computeRatesAirports.split(comma);
		for(int i=0;i<computeRatesAprts.length;i++)computeRatesAprts[i].trim();
		
		// See if all or none of the airports are to have computed
		// acceptance rates.
		if(computeRatesAprts.length == 1){
			if(computeRatesAprts[0].toLowerCase().equals("all")){
				for(int i=0;i<computeRates.length;i++){
					computeRates[i] = true;
				}
				return computeRates;
			} else if(computeRatesAprts[0].toLowerCase().equals("none")){
				for(int i=0;i<computeRates.length;i++){
					computeRates[i] = false;
				}
				return computeRates;
			}
		}
		// Have a list of specific airports then.
		for(int i=0;i<computeRates.length;i++){
			// set false by default.
			computeRates[i] = false;
		}
		for(String aprt : computeRatesAprts){
			int aprtIndx = nas.getAirportIndex(aprt.trim());
			computeRates[aprtIndx] = true;
		}
		return computeRates;	
	}
	
	/**
	 * Searches the directory for a file with the given prefix.  Then takes
	 * the file name and extracts the base and forecast days from it.
	 * @param dir Directory to search for file name.
	 * @param flightSchedFilePrefix Prefix of desired flight schedule file.
	 * @return base and forecast days as a 2D array, the 0th element being the
	 *   base day.  If unsuccessful, <code>null</code> is returned.
	 */
	public static String[] readBaseNForecastDays(String dir, 
			String flightSchedFilePrefix){
		String[] days = new String[2];
		File dirF = new File(dir);
		if(!dirF.exists())return null;
		String[] list = dirF.list();
		for(int i=0;i<list.length;i++){
			// See if prefix of file is the same as above.
			// Get the first one.
			if(list[i].startsWith(flightSchedFilePrefix)){
				int startBD = flightSchedFilePrefix.length();
				int endBD = startBD+8;
				int endFD = endBD+1+8;
				String baseDay = list[i].substring(startBD,endBD);
				String forecastDay = list[i].substring(endBD+1,endFD);
				days[0] = baseDay;
				days[1] = forecastDay;
				return days;
			}
		}
		return null;
	}
	
	/**
	 * Read nominal taxi times from a file.  These values are created by 
	 * Akira Kondo, FAA.  A taxi time 
	 * depends on the year, season, airport and air carrier.   That is it.  He 
	 * presently covers 77 airports and up to 20 carriers per airport.  If 
	 * a value is not set by him, ASPM sets the value to SQL null? I will 
	 * assume so. However, he is not sure if his numbers are completely 
	 * implemented by ASPM. The ones read here are from data received from him 
	 * directly.
	 * <p>
	 * The input file is assumed to have the following format: Lines starting
	 * with "#" are comments. Each real line is comma demarcated.  One has the form:
	 * "aprt,carrier,season,TO_Unimpeded,TO_Average,TO_Median,TO_10th_Percentile,
	 * Ti_Unimpeded, etc., where aprt and carrier are strings and
	 * the season is an int and the times are in minutes.  The carrier "ZZZ" denotes
	 * an average over all the data for an airport.  If a carrier is not
	 * listed, its values are set to ZZZ's.  It is further
	 * assumed that the data is ordered by airport.  If not, then TROUBLE.  
	 * Also, the method does not check for redundant entries.
	 * <p> The output is a 3D array of taxi times. The first entry is the 
	 * airport, the airport names being stored in the {@link Nas} object.
	 * The second entry is the air carrier name, the carrier names are also
	 * stored in the {@link Nas} object.  The last element is the index of
	 * the type of taxi time, 0 for taxi-in and 1 for taxi-out.  
	 * <p>
	 * If an airport has no data, that array element is <code>null</code>.  So
	 * there will be 77*nCarriers*2 ~ 80K matrix elements.  Doable.
	 * @param nas Nas class instance.  Used to get airport to index mapping
	 *   and carrier to index mapping.
	 * @param day Base calendar day of the simulation.  It's the base
	 *   day because taxi time data is historical.  Units: yyyymmdd 
	 * @param nomTaxiTimesFileNPath name and path to file.
	 * @return 3D array of taxi-times, the first element being airport, the
	 *   2nd being air carrier and the 3rd being an index, 0 for taxi-in 
	 *   and 1 for taxi-out. Units: seconds.
	 */
	public static int[][][] readNominalTaxiTimes(Nas nas, String day,
		File nomTaxiTimesFileNPath){
		final String method = className + ".readNominalTaxiTimes()";
		int[][][] times = null;
		
		// Get "season."  Kondo's seasons start in December rather than
		// January. However, ignore that and assume season = quarter.
		SQLDate2 date = new SQLDate2(day,SQLDate2.Element.dd);
		int qtr = date.computeFiscalQuarter();
		// Adjust to calendar year.
		if(qtr == 1)qtr = 4;
		else qtr--;
		
		BufferedReader br = null;
		String line = null, aprt = null, carrier = null;
		String[] ss = null;
		String comma = ",";
		int aprtIndx = -1, season = -1, carrierIndx = -1;
		int lastAprtIndx = 0;
		List<String> aprtData = new ArrayList<String>();
		try{
			
			//-----------------------------------------------------------------
			// Input data checks.
			//-----------------------------------------------------------------
			// Make some checks of data. All entries in lines are present 
			// and there is a ZZZ carrier for each airport listed.  Last, all
			// taxi time data is positive.
			
			// Skip all lines at the beginning that begin with "#", i.e.,
			// are comments.
			br = new BufferedReader(new FileReader(nomTaxiTimesFileNPath));
			int nSkip = 0;
			while((line = br.readLine()) != null){
				if(line.trim().charAt(0) != '#')break;
				nSkip++;
			}
			br.close();
			br = new BufferedReader(new FileReader(nomTaxiTimesFileNPath));
			for(int i=0;i<nSkip;i++)br.readLine();
			
			int aprtCount = 0,zzzCount=0; 
			lastAprtIndx = Integer.MAX_VALUE;
			while((line = br.readLine()) != null){
				ss = line.split(comma);
				// Correct length?
				if(ss.length != 11){
					throw new IllegalArgumentException(method + ": line in" +
						" input file, " + nomTaxiTimesFileNPath + " has wrong " +
						" number of entries.");
				}
				// Count airports and default entries.
				aprt = ss[0].trim();
				carrier = ss[1].trim();
				aprtIndx = nas.getAirportIndex(aprt);
				carrierIndx = nas.getCarrierIndex(carrier);
				if(aprtIndx > -1 && aprtIndx != lastAprtIndx)aprtCount++;
				if(carrier.equals("ZZZ"))zzzCount++;
				// Check for valid times.
				double tt = 0.;
				for(int i=3;i<ss.length;i++){
					tt = Double.valueOf(ss[i].trim());
					if(tt < 0.){
						throw new IllegalArgumentException(method + ": have a " +
							"negative time value in file "+nomTaxiTimesFileNPath);
					}
				}
				lastAprtIndx = aprtIndx;
			}
			br.close();
			if(aprtCount != zzzCount){
				throw new IllegalArgumentException(method + ": number of ZZZ " +
					" entries not equal to the number of airports in file " +
					nomTaxiTimesFileNPath);		
			}
			
			//-------------------------------------------------------------------
			// Extract data from file.
			//-------------------------------------------------------------------
			// Create array of possible airports.  Only will have data
			// for a subset.
			int numAprts = nas.getNumAirports();
			times = new int[numAprts][][];
			for(int i=0;i<times.length;i++)times[i] = null;
			int numCarriers = nas.getNumCarriers();
			
			// Read file and collect data for the proper season.
			// Always include carrier "ZZZ" as that is an average.
			br = new BufferedReader(new FileReader(nomTaxiTimesFileNPath));
			for(int i=0;i<nSkip;i++) br.readLine();
			
			aprtCount = 0; 
			lastAprtIndx = Integer.MAX_VALUE;
			while((line = br.readLine()) != null){
				ss = line.split(comma);
				aprt = ss[0].trim();
				carrier = ss[1].trim();
				season = Integer.valueOf(ss[2].trim());
				aprtIndx = nas.getAirportIndex(aprt);
				
				// Is airport in the Nas map and is the data for the right 
				// season or the carrier is ZZZ?
				if(aprtIndx > -1 && (season == qtr || carrier.equals("ZZZ"))) {
					aprtData.add(line);
				}
				// Create time array for this airport while we are at it
				if(aprtIndx > -1 && aprtIndx != lastAprtIndx){
					times[aprtIndx] = new int[numCarriers][2];
				}
				lastAprtIndx = aprtIndx;
			}
			br.close();
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return null;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		
		// Now populate array with this data for the season.
		// ZZZ values are excluded for now.
		// Round times to the nearest integer.
		double minInSecs = 60;
		int taxiOut=0,taxiIn=0; 
		for(String lin : aprtData){
			ss = lin.split(comma);
			aprt    = ss[0].trim();
			carrier = ss[1].trim();
			carrierIndx = nas.getCarrierIndex(carrier);
			if(!carrier.equals("ZZZ") && carrierIndx > -1){
				aprtIndx = nas.getAirportIndex(aprt);
				// Get times, convert to sim timesteps and round.
				taxiOut = (int)(Double.valueOf(ss[3].trim())*minInSecs + 0.5);
				taxiIn  = (int)(Double.valueOf(ss[7].trim())*minInSecs + 0.5);	
				times[aprtIndx][carrierIndx][Nas.Ad.ARR.ordinal()] = taxiIn;
				times[aprtIndx][carrierIndx][Nas.Ad.DEP.ordinal()]= taxiOut;
			}	
		}
		// Now add in ZZZ values for carriers for which there is no data.
		for(String lin : aprtData){
			ss = lin.split(comma);
			aprt    = ss[0].trim();
			carrier = ss[1].trim();
			if(carrier.equals("ZZZ")){
				aprtIndx = nas.getAirportIndex(aprt);
				taxiOut = (int)(Double.valueOf(ss[3].trim())*minInSecs + 0.5);
				taxiIn  = (int)(Double.valueOf(ss[7].trim())*minInSecs + 0.5);	
				// Populate all zero taxi times with average values.
				for(int i=0;i<nas.getNumCarriers();i++){
					// Might have cases in which the taxiIn is set but the out is 
					// not and vice versa, so treat them separately.
					if(times[aprtIndx][i][Nas.Ad.ARR.ordinal()] <= 0){
						times[aprtIndx][i][Nas.Ad.ARR.ordinal()] = taxiIn;
					}
					if(times[aprtIndx][i][Nas.Ad.DEP.ordinal()] <= 0){
						times[aprtIndx][i][Nas.Ad.DEP.ordinal()]= taxiOut;	
					}
				}	
			}	
		}	
		return times;
	}

	/**
	 * Method loads the airport acceptance rate data from a file.  The
	 * format of the file is assumed to be: airport, date(GMT), ADR, AAR.
	 * Comments on the input file are demarcated with a "#".  Method yields
	 * and output array. The first element of this array is an index
	 * for the airport, the mapping given in the {@link Nas} object.
	 * The second element is the index of the quarter hour, with index
	 * 0 being the quarter hour of the startTime, usually midnight on
	 * the start day, and the last index is the quarter hour of the end of the
	 * simulation, usually 9am on the day after the start day.  The
	 * last element indicates either departure rate, ADR, or arrival 
	 *  rate, AAR, the index being determined by the {@link Nas.Ad} enum.
	 * These rates are the number of aircraft in that qtr hour epoch. If an
	 * airport has no data, that array element is <code>null</code>.
	 * <p>
	 * It is assumed that the input rates are in terms of number per hour, rather
	 * than quarter hour.  So to get the quarter hour rate, one must divide by
	 * 4.
	 * 
	 * @param nas Nas class instance.  Used to get airport to index mapping
	 *   and carrier to index mapping.
	 * @param baseDay Base day of the simulation.  It is assumed that all times
	 *   in the simulation are with respect to midnight of the base day,
	 *   i.e., 00:00:00.
	 * @param acceptRatesFileNPath name and path to file.
	 * @return 3D array of called rates.  The first element is an index
	 *   for the airport, the mapping given in the {@link Nas} object.
	 *   The second element is the index of the quarter hour, with index
	 *   0 being the quarter hour of the startTime, usually midnight on
	 *   the start day, and the last index is the quarter hour of the end of the
	 *   simulation, usually 9am on the day after the start day.  The
	 *   last element indicates either departure rate, ADR, or arrival 
	 *   rate, AAR, the index being determined by the {@link Nas.Ad} enum.
	 *   These rates are the number of aircraft in that qtr hour epoch.
	 *   If an error occurs, <code>null</code> is returned.
	 */
	public static int[][][] readAspmAcceptanceRates(Nas nas, 
		SQLDate2 baseDay, File acceptRatesFileNPath){
		final String method = className + ".readHistoricalAcceptanceRates()";
		
		int[][][] rates = new int[nas.getNumAirports()][][];
		// null out all array elements.  Any airport for which there is
		// no data will have a null array.
		for(int i=0;i<rates.length;i++)rates[i] = null;
		
		BufferedReader br = null;
		try{
			//-----------------------------------------------------------------
			// Do checks on data and set up arrays.
			//-----------------------------------------------------------------
			// Do some checks, but mostly just rely on natural exceptions to catch 
			// things.
			br = new BufferedReader(new FileReader(acceptRatesFileNPath));
			String line = null;
			int nSkip = 0;
			while((line = br.readLine()) != null){
				if(line.trim().charAt(0) != '#')break;
				nSkip++;
			}
			br.close();
			br = new BufferedReader(new FileReader(acceptRatesFileNPath));
			for(int i=0;i<nSkip;i++)br.readLine();
			
			// Read data for the first airport to get number of times.
			// Read data for other airports to make sure they all have
			// the same number of times.
			String aprt = null;
			String[] ss = null;
			String comma = ",";
			int aprtIndx = -1;
			int lastAprtIndx = Integer.MAX_VALUE;
			int aprtCount = 0,timeCount=0,count=0;
			int firstAprtIndx = -1;
			boolean firstAprtFound = false;
			while((line = br.readLine()) != null){
				
				ss = line.split(comma);
				aprt = ss[0].trim();
				aprtIndx = nas.getAirportIndex(aprt);
				
				// Do calc for first airport
				if(firstAprtFound == false){
					firstAprtFound = true;
					firstAprtIndx = aprtIndx;
				}
				if(aprtIndx == firstAprtIndx){
					timeCount++;
				}
				// Count airports.
				if(aprtIndx != lastAprtIndx){
					lastAprtIndx = aprtIndx;
					aprtCount++;
				}
				// Count all lines.
				count++;
			}
			br.close();
			if(aprtCount*timeCount != count){
				throw new IllegalArgumentException(method + ": total number of " +
					" times not equal to the num aprts * num times from first " +
					" airport in file: " + acceptRatesFileNPath);
			}
			
			//-------------------------------------------------------------------
			// Extract data from file.
			//-------------------------------------------------------------------
			br = new BufferedReader(new FileReader(acceptRatesFileNPath));
			for(int i=0;i<nSkip;i++) br.readLine();
			
			aprtCount = 0; 
			lastAprtIndx = Integer.MAX_VALUE;
			while((line = br.readLine()) != null){
				ss = line.split(comma);
				aprt = ss[0].trim();
				aprtIndx = nas.getAirportIndex(aprt);
				
				// Check for new airport.
				if(aprtIndx != lastAprtIndx){
					lastAprtIndx = aprtIndx;
					rates[aprtIndx] = new int[timeCount][2];
					// reset array count.
					count = 0;
					// Check if start time for data is correct. 
					SQLDate2 date = 
						new SQLDate2(ss[1].trim(),SQLDate2.Element.ss);
					if(!date.equals(baseDay)){
						throw new IllegalArgumentException(method + ": starting " +
							"time for airport: " + aprt + ", is not the base day " +
							"time: " + baseDay);
					}
				}
				rates[aprtIndx][count][Nas.Ad.DEP.ordinal()] = 
					Integer.valueOf(ss[2].trim())/4;
				rates[aprtIndx][count][Nas.Ad.ARR.ordinal()] = 
					Integer.valueOf(ss[3].trim())/4;
				                
				count++;
			}
			br.close();
				
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return null;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return rates;
	}
	
	
	/**
	 * Using knowledge of the arrival and departure airports and the
	 * NAS nodes, the method creates a route for each flight.  It is assumed
	 * here that the order of the Nodes in the arrays is the same
	 * as the mapping of airports to indices.  The ordering of the nodes
	 * in the route is from the time of encounter, e.g., the departure
	 * airport is before the arrival airport.
	 * @param flights Array of flights.
	 * @param terminals Array of terminals.
	 * @param taxiways Array of taxiways.
	 * @param runways Array of runways.
	 * @param depFixes Array of departure fixes.
	 * @param arrFixes Array of arrival fixes.
	 * @return <code>true</code> if successful; <code>false</code> if not. 
	 */
	public static boolean createFlightRoutes(IFlight[] flights,
		INode[] terminals, INode[] taxiways, INode[] runways,
		INode[] depFixes, INode[] arrFixes){
		for(int i=0;i<flights.length;i++){
			int depAprt = flights[i].get(IFlight.Param.DEP_APRT);
			int arrAprt = flights[i].get(IFlight.Param.ARR_APRT);
			List<INode> rl = new ArrayList<INode>();
			rl.add(terminals[depAprt]);
			rl.add(taxiways[depAprt]);
			rl.add(runways[depAprt]);
			int depFix = flights[i].get(IFlight.Param.DEP_FIX);
			int arrFix = flights[i].get(IFlight.Param.ARR_FIX);
			if(arrFix < 0){
				System.out.println("yeha");
			}
			rl.add(depFixes[depFix]);
			rl.add(arrFixes[arrFix]);
			rl.add(runways[arrAprt]);
			rl.add(taxiways[arrAprt]);
			rl.add(terminals[arrAprt]);
			IRoute rt = new Route(rl);
			flights[i].setRoute(rt);
		}	
		return true;	
	}
	/**
	 * Reads the list of flights to get airports, air carriers and
	 * aircraft that need to be 
	 * modeled.  Input files are the find crossings and vfr flights
	 * files in the NASPAC scenario pre-output/aircraft/intermediate 
	 * sub-directory.  The find crossings file contains trajectory data
	 * for IFR flights, and incidentally contains other info (that we
	 * want here).  The VFR flight file contain naturally info on VFR
	 * flights.  I could have obtained the same info from a single file,
	 * the aircraft file, but as an input to the NASPAC core I'm not
	 * sure how long it will be around.  Also, the two files I am 
	 * using I am also using to create the {@link Flight} objects.
	 * <p>
	 * Note that the source/sink default airport "????" is already in the
	 * list because the VFR flight schedule is used.
	 * 
	 * @param findCrossingsFileNPath File of IFR flight trajectories. From the
	 *   NASPAC pre-output/aircraft/intermediate scenario directory.
	 * @param vfrFlightsFileNPath File of VFR flights.  From the NASPAC
	 *   pre-output/aircraft/intermediate
	 * @param aprtList List of airport names.  Probably 3 letter as that is
	 *   what NASPAC produces at present. Output.
	 * @param carrierList List of airlines, air carriers. Output.
	 * @param badaList List of aircraft types. Output.
	 * @return <code>true</code> if successful; <code>false</code> if not.
	 */
	public static boolean determineAirportsCarriersBadaTypesFromFlights(
		File findCrossingsFileNPath, File vfrFlightsFileNPath,
		List<String> aprtList, List<String> carrierList, 
		List<String> badaList){
		if(aprtList == null || carrierList == null || badaList == null){
			final String method = className + 
				".determineAirportsCarriersBadaTypesFromFlights()";
			throw new IllegalArgumentException(method + ": an input list is" +
				" null.  Please initialize it.");
		}
		aprtList.clear();
		carrierList.clear();
		badaList.clear();

		final int headerLineOneLength = 20;
		//String[] aprts = new String[1];
		BufferedReader br = null;
		try{
			// IFR flights.
			br = new BufferedReader(new FileReader(findCrossingsFileNPath));
			String line = null;
			String depAprt=null,arrAprt=null,carrier=null,aircraft=null;
			while((line = br.readLine()) != null){
				// Skip all trajectory points.
				String begin = line.substring(0,2);
				if(!(begin.equals("RP") || begin.equals("DF") ||
					 begin.equals("AF"))){
					// read header to get carrier and aircraft type.
					if(line.length() <= headerLineOneLength){
						// read header to get carrier and aircraft type.
						carrier = line.substring(5,8).trim();
						carrierList.add(carrier);
						aircraft= line.substring(8,12);
						badaList.add(aircraft);
					}
					else {
						// read other stuff to get airports.
						depAprt = line.substring(5,9).trim();
						aprtList.add(depAprt);
						arrAprt = line.substring(10,15).trim();
						aprtList.add(arrAprt);
					}
				}
			}
			br.close();
		
			// VFR flights.  No carriers or aircraft types in VFR.
			br = new BufferedReader(new FileReader(vfrFlightsFileNPath));
			while((line = br.readLine()) != null){
				depAprt = line.substring(43,47).trim();
				aprtList.add(depAprt);
				arrAprt = line.substring(53,57).trim();
				aprtList.add(arrAprt);
			}
			br.close();
			
			// Sort the array by alphabetical order and remove redundant
			// entries.  This could be done better maybe...
			// Airports.
			String[] aprtsHold = new String[1];
			aprtsHold = aprtList.toArray(aprtsHold);
			Arrays.sort(aprtsHold);//Natural ordering of Strings is alpha.
			aprtList.clear();
			String aprtLast = "";
			for(int i=0;i<aprtsHold.length;i++){
				if(!aprtsHold[i].equals(aprtLast)){
					aprtList.add(aprtsHold[i]);
				    aprtLast = aprtsHold[i];
				}
			}
			//aprts = aprtList.toArray(aprts);
			
			// Carriers.
			String[] carsHold = new String[1];
			carsHold = carrierList.toArray(carsHold);
			Arrays.sort(carsHold);//Natural ordering of Strings is alpha.
			carrierList.clear();
			String carsLast = "";
			for(int i=0;i<carsHold.length;i++){
				if(!carsHold[i].equals(carsLast)){
					carrierList.add(carsHold[i]);
				    carsLast = carsHold[i];
				}
			}
			//cars = carrierList.toArray(cars);
			
			// Aircraft types.
			String[] acrftHold = new String[1];
			acrftHold = badaList.toArray(acrftHold);
			Arrays.sort(acrftHold);//Natural ordering of Strings is alpha.
			badaList.clear();
			String acrftLast = "";
			for(int i=0;i<acrftHold.length;i++){
				if(!acrftHold[i].equals(acrftLast)){
					badaList.add(acrftHold[i]);
				    acrftLast = acrftHold[i];
				}
			}
			//bada = badaList.toArray(bada);
			
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return false;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return false;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return true;
	}

	/**
	 * Creates the flight objects and populates them with starting data.
	 * Uses outputs from the TrajectoryModeler which are two files:
	 * one of trajectory data for IFR flights, the other of VFR flight data.
	 * (These two files are combined in NASPAC to produce the aircraft file.)
	 * Both of these files are in the pre-output/aircraft/intermediate 
	 * sub-directory of the scenario directory.  The format of both files 
	 * is given in the NASPAC class, bonn.naspac.NASPACFlightPlanClass.  
	 * <p>
	 * Uses the BADA type for the aircrat equipment type.  If one wants to
	 * use the ETMS equipment type instead, that must be changed later.
	 * @param nas Instance of Nas class.
	 * @param findCrossingFileNPath File name and path of IFR flight 
	 *  trajectories.
	 * @param vfrFlightsFileNPath File name and path of VFR flights.
	 * @return Array of flight objects.
	 */
	public static IFlight[] readFlightsFromFCnVFRFiles(Nas nas, 
		File findCrossingFileNPath, File vfrFlightsFileNPath){
		IFlight[] flights = null;
		final int headerLineOneLength = 17;
		// Naspac times from these two files are in minutes.  Need to
		// convert to seconds.
		final int MIN_IN_SECS = 60;
		
		BufferedReader br = null;
		try{
			// Find how many flights there are. Then create array.
			// Read FC file.
			br = new BufferedReader(new FileReader(findCrossingFileNPath));
			int nFlights = 0, numLegs = 0;
			String line = null;
			while((line = br.readLine()) != null){
				// Read only flight first header lines.  It is the short line.
				// May have errors in traj lines so also line must start with a 
				// number.
				if(line.length() <= headerLineOneLength && 
						line.substring(0,1).matches("[0-9]")){
					numLegs = Integer.valueOf(line.substring(15,line.length()).trim());
					nFlights += numLegs;
				}
			}
			br.close();
			// Read VFR file.  Just count the number of lines.
			br = new BufferedReader(new FileReader(vfrFlightsFileNPath));
			while((line = br.readLine()) != null){
				nFlights++;
			}
			br.close();
			flights = new Flight[nFlights];
			
			//-------------------------------------------------------------
			// Load flights from FC file.
			//-------------------------------------------------------------
			// Read FC file.
			br = new BufferedReader(new FileReader(findCrossingFileNPath));
			int itinNum=0,leg=1,airborneTime=0;
			int schDepTime=0,schArrTime=0,depAprt=0,arrAprt=0;
			String carrierStr=null,badaTypeStr=null;
			String depAprtStr=null,arrAprtStr=null;
			int carrier=0,equipType=0;
			
			int count = 0;
			while((line = br.readLine()) != null){
				String begin = line.trim().substring(0,2);
				
				// Skip all trajectory points that are not fixes. These begin 
				// with RP.
				if(!begin.equals("RP")){
					
					// Get leg info.
					if(!(begin.equals("DF") || begin.equals("AF"))){
						// Look for header of flight.
						if(line.length() <= headerLineOneLength){
							
							// NASPAC itinerary number, carrier, equipment BADA
							// type, turnaround and enroute (?) categories and
							// number of flight legs.
							itinNum = Integer.valueOf(line.substring(0,5));
							carrierStr = 
								NaspacBridge.extractCarrierName(line.substring(5,8).trim());
							carrier = nas.getCarrierIndex(carrierStr);
							badaTypeStr = line.substring(8,12);
							equipType = nas.getEquipTypeIndex(badaTypeStr);
							//turnaroundCat = Integer.valueOf(line.substring(12,13));
							//enrouteTimeCat= Integer.valueOf(line.substring(13,14));
							numLegs = Integer.valueOf(line.substring(15,line.length()).trim());
							leg = 1;
						} else {
							// must be a leg. Know its itin and leg numbers.
							depAprtStr = line.substring(5,9).trim();
							depAprt = nas.getAirportIndex(depAprtStr);
							arrAprtStr = line.substring(10,15).trim();
							arrAprt = nas.getAirportIndex(arrAprtStr);
							// If an airport isn't on the map throw an exception because it should
							// be.
							if(depAprt < 0 || arrAprt < 0){
								final String method = className + ".readFlightsFromFCnVFRFiles()";
								throw new IllegalArgumentException(method + ": dep or arr" +
									" airport doesn't exist in the map");
							}
							schDepTime = 
								MIN_IN_SECS*Integer.valueOf(line.substring(32,36).trim());
							schArrTime = 
								MIN_IN_SECS*Integer.valueOf(line.substring(54,58).trim());
							airborneTime = 
								MIN_IN_SECS*Integer.valueOf(line.substring(64,68).trim());
	
							// Create flight object.
							int[] pars = new int[IFlight.Param.values().length];
							for(int i=0;i<pars.length;i++)pars[i] = 0;
							pars[IFlight.Param.ITIN_NUM.ordinal()] = itinNum;
							pars[IFlight.Param.LEG_NUM.ordinal()]  = leg;
							pars[IFlight.Param.SCHED_ID.ordinal()]       =
								-1;// for now.  Will fill in with mapping.
							pars[IFlight.Param.DEP_APRT.ordinal()] = depAprt;
							pars[IFlight.Param.ARR_APRT.ordinal()] = arrAprt;
							pars[IFlight.Param.CARRIER.ordinal()] = carrier;
							pars[IFlight.Param.EQUIP_TYPE.ordinal()] = equipType;
							pars[IFlight.Param.SCHED_OUT_TIME.ordinal()] = 
								schDepTime;
							pars[IFlight.Param.MIN_OUT_TIME.ordinal()] = 
								schDepTime;
							pars[IFlight.Param.SCHED_IN_TIME.ordinal()]  = 
								schArrTime;
							pars[IFlight.Param.ACT_AIR_TIME.ordinal()] = 
								airborneTime;
							// Default fix names.
							pars[IFlight.Param.DEP_FIX.ordinal()] = 0;
							pars[IFlight.Param.ARR_FIX.ordinal()] = 0;
							
							// Default aircraft turnaround time. Will be corrected
							// later at the Terminal.
							pars[IFlight.Param.TURN_TIME.ordinal()] = 
								 turnTime(carrier,arrAprt,equipType);
								
							flights[count] = new Flight(pars);
							
							// Add neighboring flight legs.
							if(leg > 1){
								flights[count].setPrevLeg(flights[count-1]);
								flights[count-1].setNextLeg(flights[count]);
							}
							
							count++;
							leg++;		
						}
					} else {
						// Add Fixes.
						String[] ss = null;
						String whiteSpace = " ", fix = null;
						ss = StringUtils.splitLine(line, whiteSpace);
						fix = ss[0].substring(2,ss[0].length()).trim();
						if(begin.equals("DF")){
							int fixIndx = nas.getFixIndex(Nas.Ad.DEP, fix);
							flights[count-1].set(IFlight.Param.DEP_FIX, fixIndx);
						}else {
							int fixIndx = nas.getFixIndex(Nas.Ad.ARR, fix);
							flights[count-1].set(IFlight.Param.ARR_FIX, fixIndx);
						}
					}
				}// End !RP
			}// End while
			br.close();
			
			//-------------------------------------------------------------------
			// Load flights from the VFR file.
			//-------------------------------------------------------------------
			// Read VFR file.  Just count the number of lines.
			br = new BufferedReader(new FileReader(vfrFlightsFileNPath));
			while((line = br.readLine()) != null){
				itinNum = Integer.valueOf(line.substring(9,14).trim());
				numLegs  = 1;
				leg = 1;
				schDepTime = 
					MIN_IN_SECS*Integer.valueOf(line.substring(26,30).trim());
				schArrTime = 
					MIN_IN_SECS*Integer.valueOf(line.substring(30,34).trim());
				
				depAprtStr = line.substring(43,47).trim();
				depAprt = nas.getAirportIndex(depAprtStr);
				arrAprtStr = line.substring(53,57).trim();
				arrAprt = nas.getAirportIndex(arrAprtStr);
				// If an airport isn't on the map throw an exception because it should
				// be.
				if(depAprt < 0 || arrAprt < 0){
					final String method = className + ".readFlightsFromFCnVFRFiles()";
					throw new IllegalArgumentException(method + ": dep or arr " +
						" does not exist in the map");
				}
				
				// not used so just it to -1.
				int turnaroundTime = -1;
				// Unknown carrier and equipment type.
				carrier = -1;
				equipType = -1;
			
				// Create flight object.
				int[] pars = new int[IFlight.Param.values().length];
				for(int i=0;i<pars.length;i++)pars[i] = 0;
				pars[IFlight.Param.ITIN_NUM.ordinal()] = itinNum;
				pars[IFlight.Param.LEG_NUM.ordinal()]  = leg;
				pars[IFlight.Param.SCHED_ID.ordinal()]       =
					-1;// for now.  Will fill in with mapping.
				pars[IFlight.Param.DEP_APRT.ordinal()] = depAprt;
				pars[IFlight.Param.ARR_APRT.ordinal()] = arrAprt;
				pars[IFlight.Param.CARRIER.ordinal()] = carrier;
				pars[IFlight.Param.EQUIP_TYPE.ordinal()] = equipType;
				pars[IFlight.Param.SCHED_OUT_TIME.ordinal()] = 
					schDepTime;
				pars[IFlight.Param.MIN_OUT_TIME.ordinal()] = 
					schDepTime;
				pars[IFlight.Param.SCHED_IN_TIME.ordinal()]  = 
					schArrTime;
				pars[IFlight.Param.TURN_TIME.ordinal()] = turnaroundTime;
				// No airborne time on VFR flights, so approximate it.
				pars[IFlight.Param.ACT_AIR_TIME.ordinal()] = 
					schArrTime - schDepTime;
				// Default fix names.
				pars[IFlight.Param.DEP_FIX.ordinal()] = 0;
				pars[IFlight.Param.ARR_FIX.ordinal()] = 0;
				
				flights[count] = new Flight(pars);
				count++;
			}
			br.close();
			
			// Sort flights by their ordering as determined by the
			// {@link IFlight.compareTo(IFlight)} method.
			Arrays.sort(flights);
					
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return null;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return flights;
	}
	
	/**
	 * Adds the schedule ID to the {@link IFlight} objects.  
	 * Uses the itinerary/leg to flight ID map file in the NASPAC scenario
	 * sub-directory pre-output/aircraft/intermediate to do this.  Includes
	 * both IFR and VFR flights.
	 * <p>
	 * Throws an {@link IllegalArgumentException} if the map is not 
	 * consistent with the flight object ordering, or if the number
	 * of entries in the map is not the same as in the flight object.
	 * @param itin2FlightsMapFileNPath File and path to intinerary 2
	 *  flight ID map file.
	 * @param flights Flight objects.
	 * @return <code>true</code> if successful; <code>false</code> if not.
	 */
	public static boolean addSchedIdsToFlights(File itin2FlightsMapFileNPath,
		IFlight[] flights){
		final String method = className + ".addIdsToFlights()";
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(itin2FlightsMapFileNPath));
			int nSkip = 1; // Skip first 1 line.
			for(int i=0;i<nSkip;i++) br.readLine();
			int nFlights = 0, count;
			nFlights = flights.length;
			
			String line = null;
			String[] ss = null;
			String comma = ",";
			int itinNum=0,legNum=0,flId=0,iNum=0,lNum=0,id=0;
			String ifrVfr =  null;
			// Read lines in file.  Might have crap at end so end looping when
			// the count exceeds the number of flights.
			count = 0;
			while((line = br.readLine()) != null && count < flights.length){
				// The flights are ordered by itinerary and leg number,
				// and so is the map file, so just cycle through them.
				ss = line.split(comma);
				itinNum = Integer.valueOf(ss[1]);
				legNum  = Integer.valueOf(ss[2]);
				flId    = Integer.valueOf(ss[3]);
				ifrVfr  = ss[4].trim();
				
				// There are cases in which an IFR has been split into
				// two VFR ones.  Then need to assign the same ID to both.
				if(ifrVfr.length() < 5 || ifrVfr.substring(5,6).equals("1")){
					// Regular
					flights[count].set(IFlight.Param.SCHED_ID, flId);
					iNum = flights[count].get(IFlight.Param.ITIN_NUM);
					lNum = flights[count].get(IFlight.Param.LEG_NUM);
					count++;
				} else {
					// Split IFR flight to two VFR's.
					id = Integer.valueOf(-9991 + "" + flId);
					flights[count].set(IFlight.Param.SCHED_ID, id);
					id = Integer.valueOf((-9992 + "" + flId).trim());
					flights[count+1].set(IFlight.Param.SCHED_ID, id);
					iNum = flights[count].get(IFlight.Param.ITIN_NUM);
					lNum = flights[count].get(IFlight.Param.LEG_NUM);
					count += 2;
				}
				// Test.
				if(itinNum != iNum || legNum != lNum){
					throw new IllegalArgumentException(method + 
						": Inconsistent inputs: itin and leg numbers don't "+
						" match flight ID for flight: " + flId);
				}
			}
			br.close();
			
			// Another check.
			if(nFlights != count){
				throw new IllegalArgumentException(method + ": number of " +
					"flight objects in not the same as the number of map entries.");
			}		
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return false;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return false;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return true;
	}
	/**
	 * Adds the ETMS aircraft equipment type to the flight {@link IFlight} 
	 * objects.  Get the equip type from the input schedule, and uses the
	 * flight ID to add the equipment type to a flight object.  Needless
	 * to say the flight ID in the flight object must be set before this
	 * method is called.
	 * @param nas Instance of {@link Nas} singleton.  Used to get index of 
	 *   equipment types.
	 * @param flightSchedFileNPath File and path to flight 
	 *   schedule.GenerateItineraries.BadaRemap file.
	 * @param flights Flight objects.
	 * @return <code>true</code> if successful; <code>false</code> if not.
	 */
	public static boolean addEtmsEquipTypeToFlights(Nas nas, 
		File flightSchedFileNPath, IFlight[] flights){
		final String method = className + ".addEtmsEquipTypeToFlights()";
		
		BufferedReader br = null;
		try{
			
			// Read file and store all equip types in a map between those
			// and the flight ID's.
			br = new BufferedReader(new FileReader(flightSchedFileNPath));
			int nSkip = 2; // Skip first 2 lines.
			for(int i=0;i<nSkip;i++) br.readLine();
			
			String line = null;
			String[] ss = null;
			String comma = ",";
			Integer flId = null;
			String etmsEquipType =  null;
			Map<Integer,String> flIdToEquipMap = new HashMap<Integer,String>();
			while((line = br.readLine()) != null){
				ss = line.split(comma);
				flId    = Integer.valueOf(ss[2].trim());
				etmsEquipType  = ss[26].trim();
				flIdToEquipMap.put(flId, etmsEquipType);
			}
			
			// Add equip types to flights.
			Integer idNum = null;
			int equipTypeIndx = 0;
			for(int i=0;i<flights.length;i++){
				idNum = new Integer(flights[i].get(IFlight.Param.SCHED_ID));
				etmsEquipType = flIdToEquipMap.get(idNum);
				equipTypeIndx = nas.getEquipTypeIndex(etmsEquipType);
				flights[i].set(IFlight.Param.EQUIP_TYPE, equipTypeIndx);
			}
			br.close();	
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return false;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return false;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return true;
	}
	/**
	 * Reads the pareto curve file to for each of the modeled airports
	 * (being modeled means it has a known pareto curve).  The first
	 * element is the airport index, the map from index to airport
	 * is determined by the index-to-airport map in the {@link Nas} class.
	 * The second element is the type of airport weather condition, e.g.,
	 * VMC, IMC.  The third is number of the Dep-Arr point on the curve 
	 * and the last is which coordinate of the point, x, Dep, 0 or 
	 * y, Arr, 1.
	 * <p>
	 * Will throw an exception of the number of file entries is not consistent
	 * with the number of airports in the file.  In other words, if the
	 * airport capacity is being modelled, then all MC conditions must be
	 * modelled.
	 * <p> It is assumed that the input values are number per hour, so this
	 * method divides by four to get the number per quarter hour.
	 * @param nas Instance of {@link Nas} object.
	 * @param paretoFile File of pareto curves. From the NASPAC 
	 *   pre-output/change scenario directory.
	 * @return array of pareto curves or <code>null</code> if there is a
	 *   problem with the pareto file.  The first element is the airport, the
	 *   second is type of mc condition (VMC, etc.), the 3rd is point index
	 *   on the curve and the fourth is the D, i.e., x or A, i.e., y value.
	 *   The output values are quarter hour rates, i.e., number per quarter
	 *   hour epoch.
	 */
	public static double[][][][] readParetoCurves(Nas nas, File paretoFile){
		int nAprts = nas.getNumAirports();
		double[][][][] curves = new double[nAprts][][][];
		
		// null out curves.  Is done so that a null value indicates
		// that the curve is not present.  Yes, I know that Java does
		// this automatically, but I'm making sure.
		for(int i=0;i<nAprts;i++){
			curves[i] = null;
		}
		
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(paretoFile));
			String line = null;
			String[] ss = null;
			String whiteSpace = "\\s+";
			
			// Skip first 4 lines;
			int nSkip = 4;
			for(int i=0;i<nSkip;i++) br.readLine();

			// Now count lines to see how many there are.
			// If line count is not consistent with number of airports,
			// then return null.  It is assumed that if an airport 
			// has a missing capacity curve, then a default was loaded
			// by the ChangeFileGenerator class.
			int count = 0;
			while((line = br.readLine()) != null){
				count++;
			}
			br.close();
			if(count%(Nas.Mc.values().length) != 0){
				final String method = className + ".readParetoCurves()";
				throw new IllegalArgumentException(method + ": number of" +
					" lines in pareto file isn't consistent with the number "+
					" of airports in the file.");
			}
			
			// Now read file for real.
			br = new BufferedReader(new FileReader(paretoFile));
			for(int i=0;i<nSkip;i++) br.readLine();
			String aprtCond = null, aprt = null;
			int numPts = 0;
			int lastAprtIndx = Integer.MAX_VALUE;
			while((line = br.readLine()) != null){
				ss = StringUtils.splitLine(line,whiteSpace);
				aprt = ss[0];
				aprtCond = ss[1];
				numPts = Integer.valueOf(ss[2].trim());
				double[][] cs  = new double[numPts][2];
				int indx = 0;
				for(int i=3;i<3+2*numPts;i+=2){
					cs[indx][Nas.Ad.ARR.ordinal()] = Double.valueOf(ss[i].trim())/4.;
					cs[indx][Nas.Ad.DEP.ordinal()] = Double.valueOf(ss[i+1].trim())/4.;	
					indx++;
				}
				// Add it to the airport if the airport is in the list.
				// If not, then we don't care about that airport.
				int indxAprt = -1;
				if((indxAprt = nas.getAirportIndex(aprt)) >= 0){
					if(indxAprt != lastAprtIndx){
						// create array if this is the first pass at the airport.
						curves[indxAprt] = new double[Nas.Mc.values().length][][];
						lastAprtIndx = indxAprt;
					}
					curves[indxAprt][nas.getMcIndex(aprtCond)] = cs;
				}
			}
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return null;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return curves;
	}
	
	/**
	 * Reads in a list of airports to model.  This list is not necessarily the
	 * same as those for which have called rates or pareto curves.  After this
	 * list is read, any airport not on the list will have its called rates,
	 * pareto curves and airport conditions set to <code>null</code> to indicate
	 * that the airport is not modeled.
	 * @param nas Instance of {@link Nas} object.
	 * @param airportsFileNPath  Name and path to file of airports to be modeled. 
	 *   Could be aspm77, but usually it's oep35.  If the file has one entry that
	 *   is "All", then all airports which have data are modeled.  Input file
	 *   format has a single airport to a line, with comments starting with "#".
	 * @return array of airport indices that are to be modeled.  Map from index
	 *   to indice is given in the {@link Nas} singleton.  If all airports 
	 *   are to be modeled (for which have data), then this array has one 
	 *   entry with a value of -1.  If an error occurs, then array is
	 *   <code>null</code>.
	 */
	public static int[] readModeledAirports(Nas nas, File airportsFileNPath){
		int[] modeledAprts = null;
		
		BufferedReader br = null;
		List<String> aprtsList = new ArrayList<String>();
		try{
			br = new BufferedReader(new FileReader(airportsFileNPath));
			String line = null;
			while((line = br.readLine()) != null){
				// Add entries to list.  Skip all comments and blank lines.
				if(!line.trim().equals("") && line.trim().charAt(0)!= '#'){
					aprtsList.add(line.trim());	
				}
			}
			br.close();
				
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return null;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}
		
		// Special case: first aprt is "AllAprts".
		if(aprtsList.get(0).toLowerCase().equals("allaprts")){
			modeledAprts = new int[1];
			modeledAprts[0] = -1;
		}else{
			// First check of all aprts are in the Nas list.
			// If one isn't, then remove it.
			for(int i=0;i<aprtsList.size();i++){
				int indx = nas.getAirportIndex(aprtsList.get(i));
				if(indx < 0)aprtsList.remove(i);
			}
			// Now create airport list with remaining ones.
			modeledAprts = new int[aprtsList.size()];
			for(int i=0;i<aprtsList.size();i++){
				int indx = nas.getAirportIndex(aprtsList.get(i));
				modeledAprts[i] = indx;
			}
		}
		return modeledAprts;
	}
	
	/**
	 * Computes the estimated turn-around time for a particular aircraft 
	 * flight.  In general this will depend upon the airport, the 
	 * aircraft ID and the plane type.  All are integers and the mapping
	 * of these indices to names is given in the {@link Nas} class. However,
	 * only the default turnaround time is loaded here.  More specific ones
	 * will be computed at the terminal.
	 * @param carrier  Air carrier, e.g., "AAL".
	 * @param airport  Airport, e.g., "ABQ".
	 * @param equipType  Aircraft equipment type according to ETMS, e.g.,
	 *   "A380".
	 * @return turn-around time.  Units: seconds
	 */
	public static int turnTime(int carrier, int airport, int equipType){
		// Just give a default value for now.  D. Robinson says that the
		// average turn time for an airline aircraft is 30 minutes.
		return 30*60; 
	}
	/**
	 * For the specified {@link IFlight.Param} parameter, i.e., turnTime,
	 * pushbackTime, taxiOutTime or taxiInTime, reads in a file and sets that
	 * parameter value for each {@link IFlight} object. 
	 * Reads in times from a file, grabs a flight and computes the
	 * param time for the flight.  Does this even though a flight leg may not
	 * need that time.  These param times may be modified by the
	 * {@link INode} objects later or not.  The format of an input file line
	 * is assumed to have the form: Carrier_IATA,Carrier_ICAO,Airport,EquipType,
	 * Time,StdDev.  Here, Carrier_ICAO is the 3 letter air carrier 
	 * designation.  Airport is a designation consistent with the input
	 * to the NASPAC core which is 3 letter for US airports for now. EquipType
	 * is the ETMS equipment type as a number.  The map from this number to 
	 * the actual equip type name is given in another file.  Carrier, Airport
	 * and Equipment Type should be as those defined in the {@link Nas} object. 
	 * The distribution of param times is assumed to be normal and so the values 
	 * TurnTime and StdDev completely specify it.  Time units for these are 
	 * minutes.
	 * <p>  Throws an {@link IllegalArgumentException} if the {@link IFlight.Param} 
	 * name is not one of the ones covered.
	 * @param nas Instance of {@link Nas} singleton.
	 * @param paramName Name of the parameter to be set in the {@link IFlight}
	 *   object.  Is one of {@link IFlight.Param}.
	 * @param paramTimesFileNPath Name and path to turn times file.
	 * @param equipTypeMapFileNPath File of maps from indices used by the
	 *   param time file.  This is specific to the format of the input
	 *   file and so is read in here as opposed to separately.  This map
	 *   is not used outside this method.
	 * @param paramColIndex Column in the equipType map file that corresponds to the
	 *   values for the parameter considered here.  The 1st column is 0, etc.
	 * @param flights Flight array.
	 * @return number of flights with param times set using default values, or -1
	 *   if a failure occurs.
	 */
	public static int readParamTimesNAddToFlights(
		Nas nas, IFlight.Param paramName, File paramTimesFileNPath, 
		File equipTypeMapFileNPath, int paramColIndex, IFlight[] flights){
		int nDef = 0;
		
		BufferedReader br = null;
		try{
			
			//------------------------------------------------------------------
			// Read in equip map
			//------------------------------------------------------------------
			// There may be more than one equip name with the same index, but 
			// that is okay.
			Map<String,Integer> equipMap = new HashMap<String,Integer>();
			br = new BufferedReader(new FileReader(equipTypeMapFileNPath));
			String line = null;
			String[] ss = null;
			String comma = ",", equipTypeStr = null;
			Integer index = null;
			while((line = br.readLine()) != null){
				// ignore comments.
				if(line.charAt(0) != '#'){
					ss = line.split(comma);
					equipTypeStr = ss[0].trim();
					index = Integer.valueOf(ss[paramColIndex].trim());
					equipMap.put(equipTypeStr,index);
				}
			}
			br.close();
			
			//------------------------------------------------------------------
			// Read in param times.
			//------------------------------------------------------------------
			br = new BufferedReader(new FileReader(paramTimesFileNPath));
			// A Hash table of turn time specifiers.
			Map<String,String[]> map = new HashMap<String,String[]>();
			
			// Read all lines from file and put them in the hash map.
			// Use a concatenation of the three specifiers as the hash key.
			String carStr=null,aprtStr=null,key=null;
			while((line = br.readLine()) != null){
				// skip comments.
				if(line.charAt(0) != '#'){
					// Put line in list.
					ss = line.split(comma);
					carStr       = ss[1].trim();
					aprtStr      = ss[2].trim();
					if(ss[3].trim().equals(""))index=-1;
					else index = Integer.valueOf(ss[3].trim());
					// Use concatanated names as key. Key always has something 
					// in it because of the "-1".
					key = carStr + aprtStr + index;
					map.put(key,ss);
				}
			}
			br.close();
			
			// Now for each flight grab the appropriate data.
			int aprtIndx=0,carIndx=0,equipTypeIndx=0,tTime=0;
			double mean = 0., stdDev = 0.,z=0.;
			IFlight fl = null;
			for(int i=0;i<flights.length;i++){
				fl = flights[i];
				carIndx  = fl.get(IFlight.Param.CARRIER);
				aprtIndx = fl.get(IFlight.Param.DEP_APRT);
				equipTypeIndx = fl.get(IFlight.Param.EQUIP_TYPE);
				
				// Convert to actual names.  If the field type doesn't exist, then
				// the index will be -1 and out of range.  Account for that.
				carStr = nas.getCarrierFromIndex(carIndx);
				if(carStr == null)carStr = "";
				aprtStr = nas.getAirportFromIndex(aprtIndx);
				if(aprtStr == null)aprtStr = "";
				equipTypeStr = nas.getEquipTypeFromIndex(equipTypeIndx);
				if(equipTypeStr == null)index = -1;
				else {
					if(equipMap.get(equipTypeStr)==null)index = -1;
					else index = equipMap.get(equipTypeStr);
				}
				
				// From equip, aprt and carrier, figure out the correct
				// numbers to use.
				if(paramName == IFlight.Param.TURN_TIME){
					// Order for turn times is:
					//  1) equip, aprt, carrier
					//  2) equip, aprt
					//  3) equip.
					key = carStr + aprtStr + index;
					ss = map.get(key);
					if(ss == null){
						// Try again.
						key = aprtStr+index;
						ss = map.get(key);
						if(ss == null){
							// Try again.
							key = "" + index;
							ss = map.get(key);
							if(ss == null){
								// equip type doesn't exist in the map.
								key = "-1";
								ss = map.get(key);
							}
							if(key.equals("-1")){
								nDef++;
							}
						}	
					}
				}else if(paramName == IFlight.Param.PUSHBACK_TIME || 
					paramName == IFlight.Param.TAXI_IN_TIME ||
					paramName == IFlight.Param.TAXI_OUT_TIME){
					// Order for pushback, taxi-in or taxi-out times is:
					//  1) aprt, carrier, equip
					//  2) aprt, carrier
					//  3) aprt.
					key = carStr + aprtStr + index;
					ss = map.get(key);
					if(ss == null){
						// Try again.
						key = carStr + aprtStr + "-1";
						ss = map.get(key);
						if(ss == null){
							// Try again.
							key = aprtStr + "-1";
							ss = map.get(key);
							if(ss == null){
								// aprt doesn't exist in the map.
								key = "-1";
								ss = map.get(key);
							}
							if(key.equals("-1")){
								nDef++;
							}
						}	
					}
				} else {
					final String method = className + ".readParamTimesNAddToFlights";
					throw new IllegalArgumentException(method + ": param type, " +
						paramName + ", not covered by this method.");
				}
				
				// Compute time.
				mean = Double.valueOf(ss[4].trim());
				stdDev=Double.valueOf(ss[5].trim());
				z = nas.getRandom().nextGaussian();
				// Get time.  Don't adjust if the time is unphysical as that
				// will be very rare.  For example, turnaround and taxi times
				// must be positive obviously yet the equation below does
				// not enforce that positivity.
				tTime = (int)((z*stdDev + mean)*60. + 0.5);
				fl.set(paramName, tTime);	

			}	
				
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return -1;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return -1;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}		
		return nDef;	
	}
	
	/**
	 * Reads the flight schedule to get airports, air carriers and
	 * ETMS and BADA aircraft equipment types that _may_ need to be modeled.  
	 * Emphasis is put on the _may_ because many flights will be dropped from 
	 * the flight list for various reasons, the main being that many VFR flights
	 * are irrelevant.  Input file is the input flight schedule or its
	 * modified version.
	 * <p>
	 * The airport "????" is added to the list.  It is the source and sink aiport
	 * for VFR flights.
	 * @param flightSchedFileNPath Input file of flights to the simulation.
	 * @param aprtList List of airport names.  Probably 3 letter as that is
	 *   what NASPAC produces at present. Output.
	 * @param carrierList ICAO list of airlines, air carriers. Output.
	 * @param etmsEquipList List of ETMS aircraft types. Output.
	 * @param badaEquipList List of BADA aircraft types. Output.
	 * @return <code>true</code> if successful; <code>false</code> if not.
	 */
	public static boolean readAprtCarrierEquipTypeFromFlightSched(
		File flightSchedFileNPath, List<String> aprtList, 
		List<String> carrierList, List<String>etmsEquipList,
		List<String> badaEquipList){
		if(aprtList == null || carrierList == null || 
			etmsEquipList ==  null || badaEquipList == null){
			final String method = className + 
				".determineAirportsCarriersBadaTypesFromFlights()";
			throw new IllegalArgumentException(method + ": an input list is" +
				" null.  Please initialize it.");
		}
		aprtList.clear();
		carrierList.clear();
		etmsEquipList.clear();
		badaEquipList.clear();

		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(flightSchedFileNPath));
			
			String line = null;
			String depAprt=null,arrAprt=null,carrier=null;
			String etmsEquipType=null,badaEquipType=null;
			String[] ss = null;
			String comma = ",", str = null;
			while((line = br.readLine()) != null){
				// Skip all comments.
				if(line.trim().charAt(0) != '#')
				{
					ss = line.split(comma);
					carrier = extractCarrierName(ss[2].trim());
					depAprt = ss[14].trim();
					arrAprt = ss[15].trim();
					etmsEquipType = ss[24].trim();
					badaEquipType= ss[32].trim();
					carrierList.add(carrier);
					aprtList.add(depAprt);
					aprtList.add(arrAprt);	
					etmsEquipList.add(etmsEquipType);
					badaEquipList.add(badaEquipType);
				}
			}
			br.close();
			
			// Add source/sink airport ???? to list.
			String sourceSinkAprt = "????";
			aprtList.add(sourceSinkAprt);
			
			// Sort the arrays by alphabetical order and remove redundant
			// entries.  This could be done better maybe...
			// Airports.
			String[] aprtsHold = new String[1];
			aprtsHold = aprtList.toArray(aprtsHold);
			Arrays.sort(aprtsHold);//Natural ordering of Strings is alpha.
			aprtList.clear();
			String aprtLast = "";
			for(int i=0;i<aprtsHold.length;i++){
				if(!aprtsHold[i].equals(aprtLast)){
					aprtList.add(aprtsHold[i]);
				    aprtLast = aprtsHold[i];
				}
			}
			//aprts = aprtList.toArray(aprts);
			
			// Carriers.
			String[] carsHold = new String[1];
			carsHold = carrierList.toArray(carsHold);
			Arrays.sort(carsHold);//Natural ordering of Strings is alpha.
			carrierList.clear();
			String carsLast = "";
			for(int i=0;i<carsHold.length;i++){
				if(!carsHold[i].equals(carsLast)){
					carrierList.add(carsHold[i]);
				    carsLast = carsHold[i];
				}
			}
			//cars = carrierList.toArray(cars);
			
			// ETMS aircraft types.
			String[] acrftHold = new String[1];
			acrftHold = etmsEquipList.toArray(acrftHold);
			Arrays.sort(acrftHold);//Natural ordering of Strings is alpha.
			etmsEquipList.clear();
			String acrftLast = "";
			for(int i=0;i<acrftHold.length;i++){
				if(!acrftHold[i].equals(acrftLast)){
					etmsEquipList.add(acrftHold[i]);
				    acrftLast = acrftHold[i];
				}
			}
			
			// Aircraft BADA types.
			acrftHold = new String[1];
			acrftHold = badaEquipList.toArray(acrftHold);
			Arrays.sort(acrftHold);//Natural ordering of Strings is alpha.
			badaEquipList.clear();
			acrftLast = "";
			for(int i=0;i<acrftHold.length;i++){
				if(!acrftHold[i].equals(acrftLast)){
					badaEquipList.add(acrftHold[i]);
				    acrftLast = acrftHold[i];
				}
			}
			
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return false;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return false;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return true;
	}
	/**
	 * Reads the names of departure and arrival fixes from the find
	 * crossings flight file. Adds an "unknown" fix name in case some
	 * runways don't have explicit fixes.  Want all runways to have them.
	 * @param findCrossingFileNPath File name and path of IFR flight 
	 *  trajectories.
	 * @param depFixList  List of departure fix names.  Output.
	 * @param arrFixList  List of arrival fix names.  Output.
	 * @return <code>true</code> if successful; <code>false</code> if not.
	 */
	public static boolean readFixNamesFromFCFile(File findCrossingFileNPath, 
		List<String> depFixList, List<String> arrFixList){
		
		BufferedReader br = null;
		try{
			// Add bogus fix name.
			String unknownFix = "????";
			depFixList.add(unknownFix);
			arrFixList.add(unknownFix);
			
			// Read FC file.
			br = new BufferedReader(new FileReader(findCrossingFileNPath));
			String line = null, begin = null, fix = null;
			String[] ss = null;
			String whiteSpace = " ";
			while((line = br.readLine()) != null){
				begin = line.substring(0,2);
				if(begin.equals("DF")){
					ss = StringUtils.splitLine(line, whiteSpace);
					fix = ss[0].substring(2,ss[0].length()).trim();
					depFixList.add(fix);
				}else if(begin.equals("AF")){
					ss = StringUtils.splitLine(line, whiteSpace);
					fix = ss[0].substring(2,ss[0].length()).trim();
					arrFixList.add(fix);
				}
			}
			br.close();
			
			// Sort the arrays by alphabetical order and remove redundant
			// entries.  This could be done better maybe...
			String[] fixHold = new String[1];
			fixHold = depFixList.toArray(fixHold);
			Arrays.sort(fixHold);//Natural ordering of Strings is alpha.
			depFixList.clear();
			String fixLast = "";
			for(int i=0;i<fixHold.length;i++){
				if(!fixHold[i].equals(fixLast)){
					depFixList.add(fixHold[i]);
				    fixLast = fixHold[i];
				}
			}
			fixHold = new String[1];
			fixHold = arrFixList.toArray(fixHold);
			Arrays.sort(fixHold);//Natural ordering of Strings is alpha.
			arrFixList.clear();
			fixLast = "";
			for(int i=0;i<fixHold.length;i++){
				if(!fixHold[i].equals(fixLast)){
					arrFixList.add(fixHold[i]);
				    fixLast = fixHold[i];
				}
			}					
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return false;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return false;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return true;
	}
	/**
	 * Read the values of the fix delays from two files.  These right now are
	 * the same files for the NASPAC sim core, e.g., dep_fix.2010caps.
	 * @param nas Nas object used to get names of fixes used in this sim run.
	 * @param depFixDelayFileNPath  Name and path to file of departure fix delay
	 *   values.
	 * @param arrFixDelayFileNPath  Name and path to file of arrival fix delay
	 *   values.
	 * @return 2D array of 
	 */
	public static int[][] readFixDelaysFromFile(Nas nas,
		File depFixDelayFileNPath, File arrFixDelayFileNPath){
		
		int[][] fixDelays = new int[2][];
		int nDepFix = nas.getNumFixes(Nas.Ad.DEP);
		int nArrFix = nas.getNumFixes(Nas.Ad.ARR);
		fixDelays[Nas.Ad.DEP.ordinal()] = new int[nDepFix];
		fixDelays[Nas.Ad.ARR.ordinal()] = new int[nArrFix];	
		
		BufferedReader br = null;
		try{
			// Read dep fix values.
			br = new BufferedReader(new FileReader(depFixDelayFileNPath));
			// Skip first six lines.
			int nSkip = 6;
			for(int i=0;i<nSkip;i++) br.readLine();
			
			// Read dep delay fix file.
			String line = null, fixName = null;
			String[] ss = null;
			String whiteSpace = " ";
			int fixIndx = -1, delay = -1;
			int count = 0;
			while((line = br.readLine()) != null){
				ss = StringUtils.splitLine(line, whiteSpace);
				fixName = ss[0].trim();
				fixIndx = nas.getFixIndex(Nas.Ad.DEP, fixName);
				if(fixIndx >= 0){
					delay = (int)(Double.valueOf(ss[1].trim())*60.);
					fixDelays[Nas.Ad.DEP.ordinal()][fixIndx] = delay;
					count++;
				}
			}
			br.close();
			if(count != nDepFix-1){
				// The -1 comes from the first fix being the default one which 
				// has 0 delay.
				final String method = className + ".readFixDelaysFromFile()";
				throw new IllegalArgumentException(method + ": some dep fixes are" +
					" not listed in the file, " + depFixDelayFileNPath);
			}
			
			// Read arr fix values.
			br = new BufferedReader(new FileReader(arrFixDelayFileNPath));
			// Skip first six lines.
			nSkip = 4;
			for(int i=0;i<nSkip;i++) br.readLine();
			// Read dep delay fix file.
			count = 0;
			while((line = br.readLine()) != null){
				ss = StringUtils.splitLine(line, whiteSpace);
				fixName = ss[0].trim();
				fixIndx = nas.getFixIndex(Nas.Ad.ARR, fixName);
				if(fixIndx >= 0){
					delay = (int)(Double.valueOf(ss[1].trim())*60.);
					fixDelays[Nas.Ad.ARR.ordinal()][fixIndx] = delay;
					count++;
				}
			}
			br.close();
			if(count != nArrFix-1){
				// The -1 comes from the first fix being the default one which 
				// has 0 delay.
				final String method = className + ".readFixDelaysFromFile()";
				throw new IllegalArgumentException(method + ": some arr fixes are" +
					" not listed in the file, " + arrFixDelayFileNPath);
			}
						
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return null;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return fixDelays;
	}
	
	/**
	 * Extracts the carrier name from a string.  Assumes an input string
	 * of the form "A(n)...", where A(n) is some letter sequence of length n.
	 * The maximum carrier name length is assumed to be three.
	 * @param str Input string
	 * @return Leading string of characters or <code>null</code> if string does
	 *   not start with a letter.
	 */
	public static String extractCarrierName(String str){
		StringBuffer sb = new StringBuffer();
		boolean success = false;
		String substr = null;
		int count = 0;
		while(!success && count < str.length() && count < 3){
			substr = str.substring(count,count+1);
			if(substr.matches("[a-zA-Z]")){
				sb.append(substr);
				count++;
			} else {
				success = true;
			}
		}
		if(sb.length() == 0) return null;
		else return sb.toString();
	}
	
	/**
	 * Reads the change capacity file that gives the weather conditions
	 * for all important airports.  The file is in the pre-output/change 
	 * subdirectory of the scenario.  Returns an array.  If an airport isn't 
	 * listed, this array will return <code>null</code> for those airport
	 * caps. The first element is the index of the airport, the second
	 * is an index of the change, the number of them depending on how
	 * many change reports were for this airport.  The last index is either
	 * the time of the report [0] or the weather condition, e.g., VMC as
	 * and index [1].  For example, if the airport is 10 and this is
	 * the 7th report and one wants the weather condition, then one
	 * asks for the [10][7-1=6][1] element. 
	 * @param nas Instance of the {@link Nas} object.
	 * @param changeCapFileNPath
	 * @return Array of change capacities or <code>null</code> if there is
	 *   an error.
	 */
	public static int[][][] readChangeCapacities(Nas nas, 
		File changeCapFileNPath){
		if(nas == null){
			final String method = className + ".readChangeCapacities()";
			throw new IllegalArgumentException(method + ": input Nas object"
				+ " is null.");
		}
		int numAprts = nas.getNumAirports();
		int[][][] changeCaps = new int[numAprts][][];
		
		BufferedReader br = null;
		try {
			// First open the file and count how many entries for each 
			// airport.
			br = new BufferedReader(new FileReader(changeCapFileNPath));
			int[] numEntries = new int[numAprts];
			String line = null;
			String aprt = null;
			while((line = br.readLine()) != null){
				if(line.length() > 4 && line.substring(0,2).equals("AP")){
					aprt = line.substring(6,10).trim();
					numEntries[nas.getAirportIndex(aprt)]++;
				}	
			}
			br.close();
			for(int i=0;i<numAprts;i++){
				int num = numEntries[i];
				if(num > 0) changeCaps[i] = new int[num][2];
				else changeCaps[i] = null;
			}
			// Read file again.
			for(int i=0;i<numEntries.length;i++){
				numEntries[i] = 0;
			}
			br = new BufferedReader(new FileReader(changeCapFileNPath));
			String ac = null, tStr = null;
			int aprtIndx=0,count=0,time=0,acIndx = 0;;
			while((line = br.readLine()) != null){
				
				if(line.length() > 6 && line.substring(0,2).equals("AP")){
					aprt = line.substring(6,10).trim();
					ac   = line.substring(10,line.length()).trim();
					aprtIndx = nas.getAirportIndex(aprt);
					acIndx = nas.getMcIndex(ac);
					count = numEntries[aprtIndx];
					
					changeCaps[aprtIndx][count][0] = time ;
					changeCaps[aprtIndx][count][1] = acIndx;
					numEntries[aprtIndx]++;                
				}else if(line.length() > 6 && line.substring(0,6).equals("CHANGE")){
					tStr = line.substring(7,line.length());
					time=Integer.valueOf(tStr.substring(0,tStr.length()-1));
				}
			}
			
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return null;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return changeCaps;	
	}
	
	/**
	 * Reads the model input properties from a file.  The input properties
	 * are those that are needed to create the bridge.  If needed
	 * properties are not set, i.e., are <code>null</code> then method will 
	 * print a description.  If some properties are used as
	 * booleans, yet are not set to <code>true</code> or <code>false</code>,
	 * then the method will also print a description.  If the method returns  
	 * <code>false</code> it is suggested that the calling method cause 
	 * the program to exit.
	 * @param propsFileNPath  The properties file and where it is.
	 * @param props contains model input properties.
	 * @return <code>true</code> if successful; <code>false</code> if not, 
	 *   though if it has any problem it will either throw an exception, or
	 *   print out an error description.
	 */
	public static boolean readModelInputProperties(File propsFileNPath, 
			Properties props){
		final String method = className + ".readModelInputProperties()";
		
		// Check if the properties object is instantiated. 
		if(props == null){
			System.err.println(method + " :props object is null");
			return false;
		}
	
		FileInputStream fis = null; 
    	try{
    		fis = new FileInputStream(propsFileNPath);
    		props.load(fis); 
    	} catch(FileNotFoundException fnfe){
    		fnfe.printStackTrace();
    		return false;
    	} catch(IOException ioe){
    		ioe.printStackTrace();
    		return false;
    	} finally{ 
            try{
               fis.close();
            }catch(IOException e){
               e.printStackTrace();
               return false;
            }
    	}
    	// Check that all the required properties have been set.
		String[] propertyNames = {"baseDir","baseDay",
			"forecastDay","scenario","aircraftInterSubDir",
			"flightSchedSubDir","flightSchedFilePrefix",
			"findCrossingsFileSuffix","vfrFlightsFileSuffix",
			"itin2FlightMapFileSuffix",
			"aircraftFileSubDir","aircraftFilePrefix",
			"changeCapSubDir","changeCapPrefix","paretoSubDir","paretoFile",
			//"turnTimesFilePrefix","paramTimesFileSuffix","paramTimesSubDir",
			//"taxiInTimesFilePrefix","taxiOutTimesFilePrefix","pushbackTimesFilePrefix",
			//"equipTypeMapFile",
			"taxiTimesSubDir","taxiTimesFilePrefix","taxiTimesFileSuffix",
			"computeAcceptRatesAirports" //,
			//"modeledAirportsFile","modeledAirportsSubDir","fixDelaySubDir","depFixDelayFile",
			//"arrFixDelayFile"
			};
			
		// If the property is not in the file at all, it will be null, and if
		// is not present but set it be an empty string.  For either, print a message.
		boolean success = true;
		String prop = null;
		for(int i=0;i<propertyNames.length;i++){
			prop = props.getProperty(propertyNames[i]);
			if(prop == null){
				success = false;
				System.err.println("Warning: " + method + ": " + propertyNames[i] + " is null.");
			}
			else if(prop.equals("")){
				success = false;
				System.err.println("Warning: " + method + ": " + propertyNames[i] + " is not set.");
			}
		}
			
		// Make sure createTables and cleanupTables is either "true" or "false"
		// (if they have been set at all of course).
		/*
		String createTables = 
			(props.getProperty("createTables"));
		if(createTables != null && 
			!(createTables.toLowerCase().equals("true") || 
			  createTables.toLowerCase().equals("false"))){
			System.err.println(method + ": createTables property must be " +
				"either \'true\' or \'false\'.");
			success = false;
		}
		*/
    	return success;
	}
	/**
	 * Reads the list of flights to get airports that need to be 
	 * modeled.  Input files are the find crossings and vfr flights
	 * files in the NASPAC scenario pre-output/aircraft/intermediate 
	 * sub-directory.  The find crossings file contains trajectory data
	 * for IFR flights, and incidentally contains other info (that we
	 * want here).  The VFR flight file contain naturally info on VFR
	 * flights.  I could have obtained the same info from a single file,
	 * the aircraft file, but as an input to the NASPAC core I'm not
	 * sure how long it will be around.  Also, the two files I am 
	 * using I am also using to create the {@link Flight} objects.
	 * @param findCrossingsFileNPath File of IFR flight trajectories. From the
	 *   NASPAC pre-output/aircraft/intermediate scenario directory.
	 * @param vfrFlightsFileNPath File of VFR flights.  From the NASPAC
	 *   pre-output/aircraft/intermediate
	 * @return Array of airport names.  Probably 3 letter as that is
	 *   what NASPAC produces at present.
	 */
/*
	public static String[] determineAirportsFromFlights(
		File findCrossingsFileNPath, File vfrFlightsFileNPath){
		final int headerLineOneLength = 20;
		String[] aprts = new String[1];
		BufferedReader br = null;
		try{
			// IFR flights.
			br = new BufferedReader(new FileReader(findCrossingsFileNPath));
			String line = null;
			List<String> list = new ArrayList<String>();
			String depAprt=null,arrAprt=null;
			while((line = br.readLine()) != null){
				// Skip all trajectory points.
				String begin = line.substring(0,2);
				if(!(begin.equals("RP") || begin.equals("DF") ||
					 begin.equals("AF"))){
					// Skip headers.
					if(line.length() > headerLineOneLength){
						depAprt = line.substring(5,9).trim();
						list.add(depAprt);
						arrAprt = line.substring(10,14).trim();
						list.add(arrAprt);
					}
				}
			}
			br.close();
			
			// VFR flights.
			br = new BufferedReader(new FileReader(vfrFlightsFileNPath));
			while((line = br.readLine()) != null){
				depAprt = line.substring(44,48).trim();
				list.add(depAprt);
				arrAprt = line.substring(55,59).trim();
				list.add(arrAprt);
			}
			br.close();
			
			// Sort the array by alphabetical order and remove redundant
			// entries.  This could be done better maybe...
			String[] aprtsHold = new String[1];
			aprtsHold = list.toArray(aprtsHold);
			Arrays.sort(aprtsHold);//Natural ordering of Strings is alpha.
			list.clear();
			String aprtLast = "";
			for(int i=0;i<aprtsHold.length;i++){
				if(!aprtsHold[i].equals(aprtLast)){
					list.add(aprtsHold[i]);
				    aprtLast = aprtsHold[i];
				}
			}
			aprts = list.toArray(aprts);
			
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return null;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return aprts;
	}
*/		
	/**
	 * Adds the airborne times to the flight objects.  These are in
	 * the find crossings input file.  This is in the 
	 * pre-output/aircraft/intermediate subdirectory of the scenario.
	 * The format of the find crossing input file is given in the NASPAC
	 * class bonn.naspac.NASPACFlightPlanClass.
	 * @param nas Instance of Nas class.
	 * @param flights Array of flight objects.
	 * @param findCrossingFileNPath File name and path to file.
	 * @return <code>true</code> if successful; <code>false</code> if not.
	 */
/*
	public boolean addAirborneTimes(Nas nas, IFlight[] flights, 
		File findCrossingFileNPath){
		final int HEADER_LINE_ONE_LENGTH = 17;
		
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(findCrossingFileNPath));
			String line = null;
			int numLegs = 0,itinNum = 0, leg = 1;
			int airTime = 0;
			while((line = br.readLine()) != null){
				// Skip all trajectory points.
				String begin = line.substring(0,2);
				if(!(begin.equals("RP") || begin.equals("DF") ||
					 begin.equals("AF"))){
					// Get leg info.
					
					// Look for header of flight.
					if(line.length() == HEADER_LINE_ONE_LENGTH){
						// NASPAC itinerary number.
						itinNum = Integer.valueOf(line.substring(0,5));
						// last two chars are the number of flight legs.
						numLegs = Integer.valueOf(line.substring(15,17).trim());
						leg = 1;
					} else {
						// must be a leg. Know its itin and leg numbers.
						airTime = Integer.valueOf(
							line.substring(64,68).trim();
						// Add to flight object.
						int indx = nas.flightIndexFromItinNLeg(itinNum,leg);
						flights[indx].set(IFlight.Param.ACT_AIR_TIME, airTime);
						
						leg++;		
					}
				}
			}
			
			
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return false;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return false;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return true;
	}
*/
	/**
	 * Reads the pareto curve file to get airports that have modeled
	 * capacities.  Any other airport will be assumed to have infinite 
	 * capacity.  It is assumed the pareto file has curves ordered by
	 * airport name. Further, the airport name is 3 letters and is the
	 * first characters on a line.  File format as is in the
	 * NASPAC scenario pre-output/change directory.
	 * @param paretoFile File of pareto curves. From the NASPAC 
	 *   pre-output/change scenario directory.
	 * @return Array of airport names.  Probably 3 letter as that is
	 *   what NASPAC produces at present.
	 */
/*
	public String[] determineAirportsFromPareto(File paretoFile){
		final int MAX_APRT_NAME_SIZE = 4;
		String aprt = null, aprtLast = null;
		String[] aprts = new String[1];
		List<String> list = new ArrayList<String>();
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(paretoFile));
			String line = null;
			// Skip first 4 lines;
			int nSkip = 4;
			for(int i=0;i<nSkip;i++) br.readLine();
			
			aprtLast = "";
			while((line = br.readLine()) != null){
				aprt = line.substring(0,MAX_APRT_NAME_SIZE).trim();
				if(!aprt.equals(aprtLast)){
					list.add(aprt);
					aprtLast = aprt;
				}
			}
			aprts = list.toArray(aprts);
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return null;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return aprts;
	}
*/
	/**
	 * Reads the list of flights to get airports that need to be 
	 * modelled.  File format is as in the NASPAC scenario pre-data/aircraft
	 * directory.  The reason for using this file as opposed to the
	 * aircraft file in pre-output/aircraft/intermediate is that it is 
	 * easier to read.  In this file, an airport name is always 
	 * preceded by "XAP", where "X" is a number and "AP" denotes airport.
	 * So, just look for that pattern.
	 * @param acFile NASPAC aircraft file.  The input file of flights to
	 *   NASPAC.  From the NASPAC pre-output/aircraft/intermediate scenario 
	 *   directory.
	 * @return Array of airport names.  3 or 4 letter, depending upon what
	 *   is in the aircraft file.
	 */
/*
	public static String[] determineAirportsFromAircraftFile(File acFile){
		String[] aprts = new String[1];
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(acFile));
			String line = null;
			// Skip first 4 lines;
			int nSkip = 4;
			for(int i=0;i<nSkip;i++) br.readLine();
			
			// Now want to read each line and pull out the arr and dep
			// aprts.
			List<String> list = new ArrayList<String>();
			String[] ss = null;
			String regex = "[0-9]AP[A-Z](3)";// Match on XAPyyy, where 
				// X is any integer and yyy are 3 characters.
			String aprt = null;
			while((line = br.readLine()) != null){
				ss = line.split(regex);
				for(int i=1;i<ss.length;i++){
					aprt = ss[i-1].substring(0,4).trim();
					if(!aprt.equals(""))list.add(aprt);
				}	
			}
			
			// Sort the array by alphabetical order and remove redundant
			// entries.  This could be done better maybe...
			String[] aprtsHold = new String[1];
			aprtsHold = list.toArray(aprtsHold);
			Arrays.sort(aprtsHold);//Natural ordering of Strings is alpha.
			list.clear();
			String aprtLast = "";
			for(int i=0;i<aprtsHold.length;i++){
				if(!aprtsHold[i].equals(aprtLast)){
					list.add(aprtsHold[i]);
				    aprtLast = aprtsHold[i];
				}
			}
			aprts = list.toArray(aprts);
			
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return null;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return aprts;
	}
*/	
	/**
	 * Reads the NASPAC aircraft file and loads up into the flight array
	 * as much info about the flights as can be obtained.  File format is 
	 * as in the NASPAC scenario pre-output/aircraft/intermediate directory,
	 * file name "aircraft.final.X", where X is the scenario name.
	 * @param nas Instance of {@link Nas} object.
	 * @param aircraftFile File of flights.  Each flight has 
	 *   multiple legs.  From the NASPAC pre-output/aircraft/intermediate
	 *   sub-directory of scenario.
	 * @return flights Array of flights or <code>null</code> if method
	 *   fails for some reason.
	 */
/*
	public static IFlight[] readAircraftFile(Nas nas, File aircraftFile){ 
		IFlight[] flights = null;
		
		final int acRecHeadLen = 21;
		final int flRecHeadLen = 20;
		
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(aircraftFile));
			String line = null;
			
			// Find how many flights there are. Then create array.
			int nSkip = 4; // Skip first 4 lines.
			for(int i=0;i<nSkip;i++) br.readLine();
			int count = 0;
			while((line = br.readLine()) != null){
				count++;
			}
			br.close();
			flights = new Flight[count];
			
			// Now want to read each line and pull out various stuff.
			br = new BufferedReader(new FileReader(aircraftFile));
			for(int i=0;i<nSkip;i++) br.readLine(); // skip first 2 lines.
			
			String[] ss = null;
			String comma = ",";
			int itinNum=0, numLegs=0, legNum=0,idNum=0;
			int ttCat = 0;
			Timestamp schedOutTime = null, schedInTime = null;
			String acId = null, badaType = null, depAprtStr = null;
			String acRecHead = null;
			count = 0;
			while((line = br.readLine()) != null){
				// pull out aircraft record stuff.
				acRecHead = line.substring(0,acRecHeadLen);
				itinNum = Integer.valueOf(acRecHead.substring(9,14));
				ttCat   = Integer.valueOf(acRecHead.substring(17,18));
				numLegs = Integer.valueOf(acRecHead.substring(19,21));
				
				// pull out flight leg records.
				for(int i=0;i<numLegs;i++){
					flRecHead = line.substring
					
				}
				
				
				ss = line.split(comma);
				itinNum = Integer.valueOf(ss[0]);
				legNum  = Integer.valueOf(ss[1]);
				idNum   = Integer.valueOf(ss[2]);
				acId    = ss[4];
				schedOutTime = 
					
				outDiff = 
				depAprtStr = ss[14].trim();
				depAprt = nas.getAirportIndex(depAprtStr);
				
				int[] pars = new int[IFlight.Param.values().length];
				for(int i=0;i<pars.length;i++)pars[i] = 0;
				pars[IFlight.Param.ITIN_NUM.ordinal()] = itinNum;
				pars[IFlight.Param.LEG_NUM.ordinal()]  = legNum;
				pars[IFlight.Param.ID.ordinal()]       = idNum;
				pars[IFlight.Param.DEP_APRT.ordinal()]= depAprt;
				pars[IFlight.Param.ARR_APRT.ordinal()] = arrAprt;
				pars[IFlight.Param.SCHED_OUT_TIME.ordinal()] = outDiff;
				pars[IFlight.Param.SCHED_IN_TIME.ordinal()]  = inDiff;
				int turnTime = turnTime(depAprtStr,acId,badaType);
				pars[IFlight.Param.TURN_TIME.ordinal()] = turnTime;
				
				flights[count] = new Flight(pars);
				
				// Add neighboring flight legs.
				if(legNum > 1){
					flights[count].setPrevLeg(flights[count-1]);
					flights[count-1].setNextLeg(flights[count]);
				}
				count++;
			}
			// Sort, so that it is ordered by itin and leg number.
			Arrays.sort(flights);
			
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return null;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return flights;
	}
*/
	/**
	 * Reads the flight schedule file and loads up into the flight array
	 * as much info about the flights as can be obtained.  Make sure
	 * one is using the flight file with itineraries built-in.  File format is as in the
	 * NASPAC scenario pre-date/aircraft directory.
	 * @param nas Instance of {@link Nas} object.
	 * @param startDay  The first day that the sim will run.  Any flight
	 *   in the file that has a scheduled departure time before midnight
	 *   (in GMT) of the first day will be ignored.  This is used to
	 *   adjust all times w/r to it.
	 * @param flightFile File of flights. From the NASPAC 
	 *   pre-output/change scenario directory.
	 * @return flights Array of flights or <code>null</code> if method
	 *   fails for some reason.
	 */
/*
	public static IFlight[] readFlights(Nas nas, SQLDate startDay, 
		File flightFile){ 
		IFlight[] flights = null;
		
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(flightFile));
			String line = null;
			
			// Find how many flights there are. Then create array.
			int nSkip = 2; // Skip first 2 lines.
			for(int i=0;i<nSkip;i++) br.readLine();
			int count = 0;
			while((line = br.readLine()) != null){
				count++;
			}
			br.close();
			flights = new Flight[count];
			
			// Now want to read each line and pull out various stuff.
			br = new BufferedReader(new FileReader(flightFile));
			for(int i=0;i<nSkip;i++) br.readLine(); // skip first 2 lines.
			
			Timestamp startTime = startDay.getAsTimestamp();
			String[] ss = null;
			String comma = ",";
			int arrAprt=0,depAprt=0,itinNum=0,legNum=0,idNum=0;
			int outDiff=0,inDiff=0;
			Timestamp schedOutTime = null, schedInTime = null;
			String acId = null, badaType = null, depAprtStr = null;
			long simTimeMs = 
				nas.getDelt()*1000L;// Sim timestep in milliseconds.
			count = 0;
			while((line = br.readLine()) != null){
				ss = line.split(comma);
				itinNum = Integer.valueOf(ss[0]);
				legNum  = Integer.valueOf(ss[1]);
				idNum   = Integer.valueOf(ss[2]);
				acId    = ss[4];
				schedOutTime = 
					(new SQLDate(ss[7],SQLDate.Element.ss)).getAsTimestamp();
				outDiff = (int)
					((schedOutTime.getTime() - startTime.getTime())/simTimeMs);
				depAprtStr = ss[14].trim();
				depAprt = nas.getAirportIndex(depAprtStr);
				// If the flight has no sched in time or arr aprt, then it is
				// a VFR flight, so set the values to -1.
				if(!ss[13].equals("")){
					schedInTime  = 
						(new SQLDate(ss[13],SQLDate.Element.ss)).getAsTimestamp();
					inDiff = (int)
						((schedInTime.getTime() - startTime.getTime())/simTimeMs);
				} else inDiff = -1;
				
				if(!ss[15].equals("")){
					arrAprt = nas.getAirportIndex(ss[15].trim());
				} else arrAprt = -1;
				
				int[] pars = new int[IFlight.Param.values().length];
				for(int i=0;i<pars.length;i++)pars[i] = 0;
				pars[IFlight.Param.ITIN_NUM.ordinal()] = itinNum;
				pars[IFlight.Param.LEG_NUM.ordinal()]  = legNum;
				pars[IFlight.Param.ID.ordinal()]       = idNum;
				pars[IFlight.Param.DEP_APRT.ordinal()]= depAprt;
				pars[IFlight.Param.ARR_APRT.ordinal()] = arrAprt;
				pars[IFlight.Param.SCHED_OUT_TIME.ordinal()] = outDiff;
				pars[IFlight.Param.SCHED_IN_TIME.ordinal()]  = inDiff;
				int turnTime = turnTime(depAprtStr,acId,badaType);
				pars[IFlight.Param.TURN_TIME.ordinal()] = turnTime;
				
				flights[count] = new Flight(pars);
				
				// Add neighboring flight legs.
				if(legNum > 1){
					flights[count].setPrevLeg(flights[count-1]);
					flights[count-1].setNextLeg(flights[count]);
				}
				count++;
			}
			// Sort, so that it is ordered by itin and leg number.
			Arrays.sort(flights);
			
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return null;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return flights;
	}
*/	
	/**
	 * Reads the list of flights to get airports that need to be 
	 * modelled.  File format is as in the NASPAC scenario pre-data/aircraft
	 * directory.  The reason for using this file as opposed to the
	 * aircraft file in pre-output/aircraft/intermediate is that it is 
	 * easier to read.
	 * @param flightFile File of flights. From the NASPAC 
	 *   pre-output/change scenario directory.
	 * @return Array of airport names.  Probably 3 letter as that is
	 *   what NASPAC produces at present.
	 */
/*
	public static String[] determineAirportsFromFlightsOld(File flightFile){
		String[] aprts = new String[1];
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(flightFile));
			String line = null;
			// Skip first 2 lines;
			int nSkip = 2;
			for(int i=0;i<nSkip;i++) br.readLine();
			
			// Now want to read each line and pull out the arr and dep
			// aprts.
			List<String> list = new ArrayList<String>();
			String[] ss = null;
			String comma = ",";
			String arrAprt = null, depAprt = null;
			while((line = br.readLine()) != null){
				ss = line.split(comma);
				depAprt = ss[16].trim();
				arrAprt = ss[17].trim();
				if(!depAprt.equals(""))list.add(depAprt);
				if(!arrAprt.equals(""))list.add(arrAprt);
			}
			// Add "unknown" airport, which for NASPAC is "????".
			list.add("????");
			
			// Sort the array by alphabetical order and remove redundant
			// entries.  This could be done better maybe...
			String[] aprtsHold = new String[1];
			aprtsHold = list.toArray(aprtsHold);
			Arrays.sort(aprtsHold);//Natural ordering of Strings is alpha.
			list.clear();
			String aprtLast = "";
			for(int i=0;i<aprtsHold.length;i++){
				if(!aprtsHold[i].equals(aprtLast)){
					list.add(aprtsHold[i]);
				    aprtLast = aprtsHold[i];
				}
			}
			aprts = list.toArray(aprts);
			
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return null;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return aprts;
	}
*/
	/**
	 * Reads the list of flights to get airports, air carriers and
	 * aircraft that need to be 
	 * modeled.  Input files are the find crossings and vfr flights
	 * files in the NASPAC scenario pre-output/aircraft/intermediate 
	 * sub-directory.  The find crossings file contains trajectory data
	 * for IFR flights, and incidentally contains other info (that we
	 * want here).  The VFR flight file contain naturally info on VFR
	 * flights.  I could have obtained the same info from a single file,
	 * the aircraft file, but as an input to the NASPAC core I'm not
	 * sure how long it will be around.  Also, the two files I am 
	 * using I am also using to create the {@link Flight} objects.
	 * @param findCrossingsFileNPath File of IFR flight trajectories. From the
	 *   NASPAC pre-output/aircraft/intermediate scenario directory.
	 * @param vfrFlightsFileNPath File of VFR flights.  From the NASPAC
	 *   pre-output/aircraft/intermediate
	 * @param aprtList List of airport names.  Probably 3 letter as that is
	 *   what NASPAC produces at present. Output.
	 * @param carrierList List of airlines, air carriers. Output.
	 * @param badaList List of aircraft types. Output.
	 * @return <code>true</code> if successful; <code>false</code> if not.
	 */
/*
	public static boolean determineAirportsCarriersBadaTypesFromFlights(
		File findCrossingsFileNPath, File vfrFlightsFileNPath,
		List<String> aprtList, List<String> carrierList, 
		List<String> badaList){
		if(aprtList == null || carrierList == null || badaList == null){
			final String method = className + 
				".determineAirportsCarriersBadaTypesFromFlights()";
			throw new IllegalArgumentException(method + ": an input list is" +
				" null.  Please initialize it.");
		}
		aprtList.clear();
		carrierList.clear();
		badaList.clear();

		final int headerLineOneLength = 20;
		//String[] aprts = new String[1];
		BufferedReader br = null;
		try{
			// IFR flights.
			br = new BufferedReader(new FileReader(findCrossingsFileNPath));
			String line = null;
			String depAprt=null,arrAprt=null,carrier=null,aircraft=null;
			while((line = br.readLine()) != null){
				// Skip all trajectory points.
				String begin = line.substring(0,2);
				if(!(begin.equals("RP") || begin.equals("DF") ||
					 begin.equals("AF"))){
					// read header to get carrier and aircraft type.
					if(line.length() <= headerLineOneLength){
						// read header to get carrier and aircraft type.
						carrier = line.substring(5,8).trim();
						carrierList.add(carrier);
						aircraft= line.substring(8,12);
						badaList.add(aircraft);
					}
					else {
						// read other stuff to get airports.
						depAprt = line.substring(5,9).trim();
						aprtList.add(depAprt);
						arrAprt = line.substring(10,15).trim();
						aprtList.add(arrAprt);
					}
				}
			}
			br.close();
		
			// VFR flights.  No carriers or aircraft types in VFR.
			br = new BufferedReader(new FileReader(vfrFlightsFileNPath));
			while((line = br.readLine()) != null){
				depAprt = line.substring(43,47).trim();
				aprtList.add(depAprt);
				arrAprt = line.substring(53,57).trim();
				aprtList.add(arrAprt);
			}
			br.close();
			
			// Sort the array by alphabetical order and remove redundant
			// entries.  This could be done better maybe...
			// Airports.
			String[] aprtsHold = new String[1];
			aprtsHold = aprtList.toArray(aprtsHold);
			Arrays.sort(aprtsHold);//Natural ordering of Strings is alpha.
			aprtList.clear();
			String aprtLast = "";
			for(int i=0;i<aprtsHold.length;i++){
				if(!aprtsHold[i].equals(aprtLast)){
					aprtList.add(aprtsHold[i]);
				    aprtLast = aprtsHold[i];
				}
			}
			//aprts = aprtList.toArray(aprts);
			
			// Carriers.
			String[] carsHold = new String[1];
			carsHold = carrierList.toArray(carsHold);
			Arrays.sort(carsHold);//Natural ordering of Strings is alpha.
			carrierList.clear();
			String carsLast = "";
			for(int i=0;i<carsHold.length;i++){
				if(!carsHold[i].equals(carsLast)){
					carrierList.add(carsHold[i]);
				    carsLast = carsHold[i];
				}
			}
			//cars = carrierList.toArray(cars);
			
			// Aircraft types.
			String[] acrftHold = new String[1];
			acrftHold = badaList.toArray(acrftHold);
			Arrays.sort(acrftHold);//Natural ordering of Strings is alpha.
			badaList.clear();
			String acrftLast = "";
			for(int i=0;i<acrftHold.length;i++){
				if(!acrftHold[i].equals(acrftLast)){
					badaList.add(acrftHold[i]);
				    acrftLast = acrftHold[i];
				}
			}
			//bada = badaList.toArray(bada);
			
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return false;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return false;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		return true;
	}
*/
	/**
	 * Read nominal taxi times from a file.  These values are created by 
	 * Akira Kondo, FAA.  A taxi time 
	 * depends on the year, season, airport and air carrier.   That is it.  He 
	 * presently covers 77 airports and up to 20 carriers per airport.  If 
	 * a value is not set by him, ASPM sets the value to SQL null? I will 
	 * assume so. However, he is not sure if his numbers are completely 
	 * implemented by ASPM. The ones read here are from data received from him 
	 * directly.
	 * <p>
	 * The input file is assumed to have the following format: Lines starting
	 * with "#" are comments. Each real line is comma demarcated.  One has the form:
	 * "aprt,carrier,season,TO_Unimpeded,TO_Average,TO_Median,TO_10th_Percentile,
	 * Ti_Unimpeded, etc., where aprt and carrier are strings and
	 * the season is an int and the times are in minutes.  The carrier "ZZZ" denotes
	 * an average over all the data for an airport.  If a carrier is not
	 * listed, its values are set to ZZZ's.  It is further
	 * assumed that the data is ordered by airport.  If not, then TROUBLE.  
	 * Also, the method does not check for redundant entries.
	 * <p> The output is a 3D array of taxi times. The first entry is the 
	 * airport, the airport names being stored in the {@link Nas} object.
	 * The second entry is the air carrier name, the carrier names are also
	 * stored in the {@link Nas} object.  The last element is the index of
	 * the type of taxi time, 0 for taxi-in and 1 for taxi-out.  
	 * <p>
	 * If an airport has no data, that array element is <code>null</code>.  So
	 * there will be 77*nCarriers*2 ~ 80K matrix elements.  Doable.
	 * @param nas Nas class instance.  Used to get airport to index mapping
	 *   and carrier to index mapping.
	 * @param day Base calendar day of the simulation.  It's the base
	 *   day because taxi time data is historical.  Units: yyyymmdd 
	 * @param nomTaxiTimesFileNPath name and path to file.
	 * @return 3D array of taxi-times, the first element being airport, the
	 *   2nd being air carrier and the 3rd being an index, 0 for taxi-in 
	 *   and 1 for taxi-out. Units: seconds.
	 */
/*
	public static int[][][] readNominalTaxiTimes(Nas nas, String day,
		File nomTaxiTimesFileNPath){
		final String method = className + ".readNominalTaxiTimes()";
		int[][][] times = null;
		
		// Get "season."  Kondo's seasons start in December rather than
		// January. However, ignore that and assume season = quarter.
		SQLDate2 date = new SQLDate2(day,SQLDate2.Element.dd);
		int qtr = date.computeFiscalQuarter();
		// Adjust to calendar year.
		if(qtr == 1)qtr = 4;
		else qtr--;
		
		BufferedReader br = null;
		String line = null, aprt = null, carrier = null;
		String[] ss = null;
		String comma = ",";
		int aprtIndx = -1, season = -1, carrierIndx = -1;
		int lastAprtIndx = 0;
		List<String> aprtData = new ArrayList<String>();
		try{
			
			//-----------------------------------------------------------------
			// Input data checks.
			//-----------------------------------------------------------------
			// Make some checks of data. All entries in lines are present 
			// and there is a ZZZ carrier for each airport listed.  Last, all
			// taxi time data is positive.
			
			// Skip all lines at the beginning that begin with "#", i.e.,
			// are comments.
			br = new BufferedReader(new FileReader(nomTaxiTimesFileNPath));
			int nSkip = 0;
			while((line = br.readLine()) != null){
				if(line.trim().charAt(0) != '#')break;
				nSkip++;
			}
			br.close();
			br = new BufferedReader(new FileReader(nomTaxiTimesFileNPath));
			for(int i=0;i<nSkip;i++)br.readLine();
			
			int aprtCount = 0,zzzCount=0; 
			lastAprtIndx = Integer.MAX_VALUE;
			while((line = br.readLine()) != null){
				ss = line.split(comma);
				// Correct length?
				if(ss.length != 11){
					throw new IllegalArgumentException(method + ": line in" +
						" input file, " + nomTaxiTimesFileNPath + " has wrong " +
						" number of entries.");
				}
				// Count airports and default entries.
				aprt = ss[0].trim();
				carrier = ss[1].trim();
				aprtIndx = nas.getAirportIndex(aprt);
				carrierIndx = nas.getCarrierIndex(carrier);
				if(aprtIndx > -1 && aprtIndx != lastAprtIndx)aprtCount++;
				if(carrier.equals("ZZZ"))zzzCount++;
				// Check for valid times.
				double tt = 0.;
				for(int i=3;i<ss.length;i++){
					tt = Double.valueOf(ss[i].trim());
					if(tt < 0.){
						throw new IllegalArgumentException(method + ": have a " +
							"negative time value in file "+nomTaxiTimesFileNPath);
					}
				}
				lastAprtIndx = aprtIndx;
			}
			br.close();
			if(aprtCount != zzzCount){
				throw new IllegalArgumentException(method + ": number of ZZZ " +
					" entries not equal to the number of airports in file " +
					nomTaxiTimesFileNPath);		
			}
			
			//-------------------------------------------------------------------
			// Extract data from file.
			//-------------------------------------------------------------------
			// Create array of possible airports.  Only will have data
			// for a subset.
			int numAprts = nas.getNumAirports();
			times = new int[numAprts][][];
			for(int i=0;i<times.length;i++)times[i] = null;
			int numCarriers = nas.getNumCarriers();
			
			// Read file and collect data for the proper season.
			// Always include carrier "ZZZ" as that is an average.
			br = new BufferedReader(new FileReader(nomTaxiTimesFileNPath));
			for(int i=0;i<nSkip;i++) br.readLine();
			
			aprtCount = 0; 
			lastAprtIndx = Integer.MAX_VALUE;
			while((line = br.readLine()) != null){
				ss = line.split(comma);
				aprt = ss[0].trim();
				carrier = ss[1].trim();
				season = Integer.valueOf(ss[2].trim());
				aprtIndx = nas.getAirportIndex(aprt);
				
				// Is airport in the Nas map and is the data for the right 
				// season or the carrier is ZZZ?
				if(aprtIndx > -1 && (season == qtr || carrier.equals("ZZZ"))) {
					aprtData.add(line);
				}
				// Create time array for this airport while we are at it
				if(aprtIndx > -1 && aprtIndx != lastAprtIndx){
					times[aprtIndx] = new int[numCarriers][2];
				}
				lastAprtIndx = aprtIndx;
			}
			br.close();
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			return null;
		}catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}finally {
			try{
				br.close();
			}catch(IOException ioe){}
		}	
		
		// Now populate array with this data for the season.
		// ZZZ values are excluded for now.
		// Round times to the nearest integer.
		double minInSecs = 60;
		int taxiOut=0,taxiIn=0; 
		for(String lin : aprtData){
			ss = lin.split(comma);
			aprt    = ss[0].trim();
			carrier = ss[1].trim();
			carrierIndx = nas.getCarrierIndex(carrier);
			if(!carrier.equals("ZZZ") && carrierIndx > -1){
				aprtIndx = nas.getAirportIndex(aprt);
				// Get times, convert to sim timesteps and round.
				taxiOut = (int)(Double.valueOf(ss[3].trim())*minInSecs + 0.5);
				taxiIn  = (int)(Double.valueOf(ss[7].trim())*minInSecs + 0.5);	
				times[aprtIndx][carrierIndx][Nas.Ad.ARR.ordinal()] = taxiIn;
				times[aprtIndx][carrierIndx][Nas.Ad.DEP.ordinal()]= taxiOut;
			}	
		}
		// Now add in ZZZ values for carriers for which there is no data.
		for(String lin : aprtData){
			ss = lin.split(comma);
			aprt    = ss[0].trim();
			carrier = ss[1].trim();
			if(carrier.equals("ZZZ")){
				aprtIndx = nas.getAirportIndex(aprt);
				taxiOut = (int)(Double.valueOf(ss[3].trim())*minInSecs + 0.5);
				taxiIn  = (int)(Double.valueOf(ss[7].trim())*minInSecs + 0.5);	
				// Populate all zero taxi times with average values.
				for(int i=0;i<nas.getNumCarriers();i++){
					// Might have cases in which the taxiIn is set but the out is 
					// not and vice versa, so treat them separately.
					if(times[aprtIndx][i][Nas.Ad.ARR.ordinal()] <= 0){
						times[aprtIndx][i][Nas.Ad.ARR.ordinal()] = taxiIn;
					}
					if(times[aprtIndx][i][Nas.Ad.DEP.ordinal()] <= 0){
						times[aprtIndx][i][Nas.Ad.DEP.ordinal()]= taxiOut;	
					}
				}	
			}	
		}	
		return times;
	}
*/
    
}
