package org.cna.donley.nassim2_4;

import org.cna.donley.jdbc.JDBCManager;
import org.cna.donley.jdbc.SQLDate2;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Class processes the NasSim output flight file(s), and computes the total
 * NAS delay and delays by airport.  Can compare sim output with actuals for
 * a historical day using ASPM data.  This is done using the method
 * {@link #analyzeSimWithActualBaseDays(Properties, boolean, String[])}.  Or,
 * one can compare sim output of one day with others using 
 * {@link #analyzeSimDays(Properties, boolean, String[][])}.  The program
 * parameters are set in a Java Properties, {@link java.util.Properties} file.
 * However, this file allows for computing one day at a time at present.  It 
 * still can be used for multitple days though by just resetting the
 * baseDay and forecastDay params using the Properties.getProperty(String) 
 * method.  See the corresponding JUnit test program for examples.
 * <p>
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: FlightSimOutputAnalyzer.java 1 2009-09-28 00:00:00EST donley $
 */
public class NasSimOutputAnalyzer
{
	/**
	 * The class name.  Need to write it since methods are static.
	 */
	private final static String className = "NasSimOutputAnalyzer";
	
	/** 
	 * Main method to run analyzer.  Can also just call 
	 * {@link #analyzeSimWithActualBaseDays(Properties, boolean, String[])} directly.
	 * @param args  To input object properties.
	 */
	public static void main(String[] args){
		String method = className + ".main()";
		Properties props = new Properties();
		File propsFileNPath = null;
		boolean createTables = false;
		String baseDay = null;  // format "yyyymmdd"
		String forecastDay = null; // same format.
		if(args.length < 2){
			System.err.println(method + 
				": Usage: " + className + 
				" <properties file and path> <createTable <true or false>> "+
				" <base day <YYYYMMDD>>  <forecast day <YYYYMMDD>>");
			System.exit(-1);
		}
		else {
			propsFileNPath = new File(args[0]);
			createTables = Boolean.getBoolean(args[1]);
			baseDay     = args[2];
			forecastDay = args[3];
		}
	
    	if(!readModelInputProperties(propsFileNPath, props)){
    		System.err.println(method + ": error reading properties file. Exit.");
    		System.exit(-1);
    	}
    	
    	// Run analyzer.
    	String[]baseDays = new String[1];
    	baseDays[0] = baseDay;
    	if(!analyzeSimWithActualBaseDays(props,createTables,baseDays)){
    		System.err.println(method + " failure of analyze method.  Exit.");
    		System.exit(-1);
    	}
	}
	
	/**
	 * Analyzes simulation output, compares with actual, historical data and
	 * creates tables of system-wide and airport specific daily delay.
	 * 
	 * @param props  Class properties, including database info.
	 * @param createTables If <code>true</code> then all new tables
	 *   are created and any previous table of the same name is
	 *   destroyed; if <code>false</code>, then just add data
	 *   to tables, which better exist.  If they don't, this
	 *   method will fail.
	 * @param baseDays  Array of base days to run.  Format is "yyyymmdd".
	 * @return <code>true</code> if successful; <code>false</code> if not.
	 */
	public static boolean analyzeSimWithActualBaseDays(Properties props, 
			boolean createTables, String[]baseDays){
		boolean success = true;
		String method = className + ".analyzeSimWithActualBaseDays()";
    	
    	// Where the data is and where it is going.
		// Main path.
		String baseDir = props.getProperty("baseDir");
		String mainSubDir  = props.getProperty("mainSubDir");
		String mainPath = baseDir + File.separator + mainSubDir;
		
		// Get temp table names.
		String flightOutputTable = props.getProperty("flightOutputTable");
		String schedTable = props.getProperty("scheduleInputTable");
		String schedNFlightTable = props.getProperty("schedNFlightOutputTable");
		String aspmFlightDataTable = props.getProperty("aspmFlightDataTable");
		String actNSimTable= props.getProperty("actNSimTable");
		String delayTable  = props.getProperty("delayTable");
		String delayAprtTable = props.getProperty("delayAprtTable");
		String airportGroupsTable= props.getProperty("airportGroupsTable");
		String airportGroup = props.getProperty("airportGroup");
	
    	// SchedInput files.
		String schedSubDir   = props.getProperty("schedSubDir");
		String schedFilePrefix = props.getProperty("schedFilePrefix");
		String schedFileSuffix = props.getProperty("schedFileSuffix");
		File schedFileNPath= null;
		
		// FlightOutput files.
		String flightSubDir   = props.getProperty("flightOutputSubDir");
		String flightFilePrefix = props.getProperty("flightOutputFilePrefix");
		String flightFileSuffix = props.getProperty("flightOutputFileSuffix");
		File flightFileNPath= null;

		// Dump all flight output and schedules data into tables.
		boolean createFnSTables = createTables;
		for(int fIndx=0;fIndx<baseDays.length;fIndx++){
			String baseDay     = baseDays[fIndx];
			String forecastDay = baseDay;
			System.out.println("base day: " + baseDay + ", forecast day: " + forecastDay);
		
		    // Create table of relevant stuff in flights file.
		    flightFileNPath = new File(mainPath + File.separator + flightSubDir +
		        File.separator + flightFilePrefix + forecastDay + "_" + baseDay +
		        flightFileSuffix);
	    	if(!createFlightOutputTable(props, createFnSTables, baseDay, forecastDay,
	    			flightFileNPath, flightOutputTable)){
	    		System.err.println(method + ": Error in createFlightOutputTable. Exit.");
				return false;
	    	}
	    	
	    	// Create table of relevant stuff in schedule input file.
	    	schedFileNPath = new File(mainPath + File.separator + schedSubDir +
				File.separator + schedFilePrefix + 
				forecastDay + "_" + baseDay + schedFileSuffix);
	    	if(!createScheduleTable(props,createFnSTables,baseDay,forecastDay,
	    		schedFileNPath,schedTable)){
	    		System.err.println(method + ": Error in createScheduleTable. Exit.");
				return false;
	    	}
	    	// only create the tables once.
	    	if(createFnSTables)createFnSTables = false;
		}
		if(createTables){
			System.out.println(method + ": created and populated tables: " + flightOutputTable +
				" and " + schedTable);
		} else {
			System.out.println(method + ": populated tables: " + flightOutputTable +
					" and " + schedTable);
		}
		
    	// Join the two tables on the id number.  Always do this.
    	if(!combineSchedNFlightTables(props,schedTable,
    		flightOutputTable,schedNFlightTable)){
    		System.err.println(method + ": Error in combineSchedNFlightTables. Exit.");
			return false;
    	}
		System.out.println(method + ": created table: " + schedNFlightTable);
	    	
    	// With the above table, query the ASPM database, and create
    	// a combo of the two tables, ready for computing delays and
    	// other good stuff.  Always do this.
    	if(!combineActualNSimulationTables(props,aspmFlightDataTable,
    		schedNFlightTable,actNSimTable)){
    		System.err.println(method + ": Error in combineActualNSimulationTables. Exit.");
			return false;
    	}
    	System.out.println(method + ": created combined ASPM and " +
    			"schedNFlight table," + actNSimTable + ".");
	    	
    	// Compute delays for all dates.
    	// Do this every time.
   /*
    	boolean createDelTables = createTables;
		for(int fIndx=0;fIndx<baseDays.length;fIndx++){
			String baseDay     = baseDays[fIndx];
			String forecastDay = baseDay;
	    	// Compute delays and add them to delayTable.
	    	if(!computeDelaysSystemwide(props,createDelTables,baseDay,forecastDay,actNSimTable,
	    		delayTable)){
	    		System.err.println(method + ": Error in computeDelaysSystemwide.");
	    		return false;
	    	}
	    	
	    	if(!computeDelaysByAirport(props,createDelTables,baseDay,forecastDay,
	    		airportGroupsTable,airportGroup,actNSimTable,delayAprtTable)){
	    		System.err.println(method + ": Error in computeDelaysByAirport.");
	    		return false;
	    	}
	    	if(createDelTables)createDelTables = false;
		}
		if(createTables){
			System.out.println(method + ": created and populated " +
				" in tables: " + delayTable + " and " + delayAprtTable);
		}else {
			System.out.println(method + ": populated " +
					" tables: " + delayTable + " and " + delayAprtTable);
		}
   */
 
		return success;
    	
	}// End of analyze.
	
	/**
	 * Analyzes some simulation output for a group of days with the 
	 * intention of comparing output for a base day (or month) with a forecast
	 * day (or month) creates tables of system-wide and airport specific 
	 * daily delay. This method loads the input schedule files and 
	 * flight_output files into tables and combines them.  Then it computes 
	 * sim delays, both system-wide and airport specific, and puts them
	 * into tables.  Any further analysis is done manually on those tables.
	 * Naturally, the NASPAC simulation must have run for the days specified
	 * here.
	 * @param props  Class properties, including database info.
	 * @param createTables If <code>true</code> then all new tables
	 *   are created and any previous table of the same name is
	 *   destroyed; if <code>false</code>, then just add data
	 *   to tables, which better exist.  If they don't, this
	 *   method will fail.
	 * @param baseNForecastDays  Array of days to use.  Each day is specified
	 *   by its base and forecast day, which if it really is a base day are the
	 *   same day.  The base day is the first element of the pair.  
	 *   Format is "yyyymmdd".
	 * @return <code>true</code> if successful; <code>false</code> if not.
	 */
	public static boolean analyzeSimDays(Properties props, 
			boolean createTables, String[][] baseNForecastDays){
		boolean success = true;
		String method = className + ".analyzeSimDays";
    	
    	// Where the data is and where it is going.
		// Main path.
		String baseDir = props.getProperty("baseDir");
		String mainSubDir  = props.getProperty("mainSubDir");
		String mainPath = baseDir + File.separator + mainSubDir;
		
		// Get temp table names.
		String flightOutputTable = props.getProperty("flightOutputTable");
		String schedTable = props.getProperty("scheduleInputTable");
		String schedNFlightTable = props.getProperty("schedNFlightOutputTable");
		String aspmFlightDataTable = props.getProperty("aspmFlightDataTable");
		String actNSimTable= props.getProperty("actNSimTable");
		String delayTable  = props.getProperty("delayTable");
		String delayAprtTable = props.getProperty("delayAprtTable");
		String airportGroupsTable= props.getProperty("airportGroupsTable");
		String airportGroup = props.getProperty("airportGroup");
	
    	// SchedInput files.
		String schedSubDir   = props.getProperty("schedSubDir");
		String schedFilePrefix = props.getProperty("schedFilePrefix");
		String schedFileSuffix = props.getProperty("schedFileSuffix");
		File schedFileNPath= null;
		
		// FlightOutput files.
		String flightSubDir   = props.getProperty("flightOutputSubDir");
		String flightFilePrefix = props.getProperty("flightOutputFilePrefix");
		String flightFileSuffix = props.getProperty("flightOutputFileSuffix");
		File flightFileNPath= null;
		
		// Dump all flight output and schedules data into tables.
		boolean createFnSTables = createTables;
		for(int fIndx=0;fIndx<baseNForecastDays.length;fIndx++){
			String baseDay     = baseNForecastDays[fIndx][0];
			String forecastDay = baseNForecastDays[fIndx][1];
		
		    // Create table of relevant stuff in flights file.
		    flightFileNPath = new File(mainPath + File.separator + flightSubDir +
		        File.separator + flightFilePrefix + forecastDay + flightFileSuffix);
	    	if(!createFlightOutputTable(props, createFnSTables, baseDay, forecastDay,
	    			flightFileNPath, flightOutputTable)){
	    		System.err.println(method + ": Error in createFlightOutputTable. Exit.");
				return false;
	    	}
	    	
	    	// Create table of relevant stuff in schedule input file.
	    	schedFileNPath = new File(mainPath + File.separator + schedSubDir +
				File.separator + schedFilePrefix + 
				forecastDay + "_" + baseDay + schedFileSuffix);
	    	if(!createScheduleTable(props,createFnSTables,baseDay,forecastDay,
	    		schedFileNPath,schedTable)){
	    		System.err.println(method + ": Error in createScheduleTable. Exit.");
	    		return false;
	    	}
	    	// only create the tables once.
	    	if(createFnSTables)createFnSTables = false;
		}
		if(createTables){
			System.out.println(method + ": created and populated tables: " + flightOutputTable +
				" and " + schedTable);
		} else {
			System.out.println(method + ": populated tables: " + flightOutputTable +
					" and " + schedTable);
		}
		
    	// Join the two tables on the id number.
		// Do this every time.
    	if(!combineSchedNFlightTables(props,schedTable,
    		flightOutputTable,schedNFlightTable)){
    		System.err.println(method + ": Error in combineSchedNFlightTables. Exit.");
    		return false;
    	}
		System.out.println(method + ": created table: " + schedNFlightTable);
	    	
    	// With the above table, create a bogus Act + Sim table, so can use
		// the usual methods for computing system delays.
		// Do this every time.
    	if(!createBogusActualNSimulationTables(props,
    		schedNFlightTable,actNSimTable)){
    		System.err.println(method + ": Error in combineActualNSimulationTables. Exit.");
    		return false;
    	}
    	System.out.println(method + ": created bogus actual and " +
    			"schedNFlight table," + actNSimTable + ".");
	    	
    	// Compute delays for all dates.  Create table if necessary.
    	boolean createDelTables = createTables;
		for(int fIndx=0;fIndx<baseNForecastDays.length;fIndx++){
			String baseDay     = baseNForecastDays[fIndx][0];
			String forecastDay = baseNForecastDays[fIndx][1];
	    	// Compute delays and add them to delayTable.
	    	if(!computeDelaysSystemwide(props,createDelTables,baseDay,forecastDay,actNSimTable,
	    		delayTable)){
	    		System.err.println(method + ": Error in computeDelaysSystemwide.");
	    		return false;
	    	}
	    	
	    	if(!computeDelaysByAirport(props,createDelTables,baseDay,forecastDay,
	    		airportGroupsTable,airportGroup,actNSimTable,delayAprtTable)){
	    		System.err.println(method + ": Error in computeDelaysByAirport.");
	    		return false;
	    	}
	    	if(createDelTables)createDelTables = false;
		}
		if(createTables){
			System.out.println(method + ": created and populated " +
				" in tables: " + delayTable + " and " + delayAprtTable);
		}else {
			System.out.println(method + ": populated tables: " +
				delayTable + " and " + delayAprtTable);
		}
 
		return success;
    	
	}// End of analyze.
	
	/**
	 * Creates a table of important fields in an NasSim flightOutput*.csv
	 * output file, which is input to this method.
	 * @param props
	 * @param createTable If <code>true</code> then create the table and 
	 *   and destroy the old one if it exists; if not, then not.
	 * @param baseDay Base date associated with the flights in the output
	 *   file.  Format: yyyymmdd.  Must be consistent with the date given to
	 *   data in the corresponding schedule table created by
	 *   {@link #createScheduleTable(Properties, boolean, String, String, File, String)}.
	 * @param forecastDay Forecast date associated with the flights in the 
	 *   output table.  Format: "yyyymmdd".
	 *   Must be consistent with the date given to data in the corresponding
	 *   flight schedule table create by 
	 *   {@link #createScheduleTable(Properties, boolean, String, String, File, String)}
	 *   below.
	 * @param flightsFileNPath  Name and directory of NASPAC output flights file.
	 * @param flightOutputTable Name of created table.
	 * @return <code>true</code> if successful; <code>false</false> if not.
	 */
	public static boolean createFlightOutputTable(Properties props, 
			boolean createTable, String baseDay, String forecastDay, 
			File flightsFileNPath, String flightOutputTable){
		
		final String method = className + ".createFlightOutputTable()";
		boolean success = true;
		
		// Fields in the table to be created.  For consistency has the same
		// names as the input file with three exceptions.  The "forecast_day" should
		// be the same as the "sim start date." 
		final String tableFields = "(" +
			" base_day DATE, " +
			" forecast_day DATE, " +
			" itinerary_number INTEGER, " +
			" leg_num INTEGER, " +
			" schedule_id INTEGER, " +  // same as the sched id_num.
			" carrier VARCHAR(10), " +
			" etms_ac_type VARCHAR(8)," +
			" dep_aprt VARCHAR(5), " +
			" arr_aprt VARCHAR(5), " +
			" sched_out_time DATE, " +
			" sim_out_time DATE," +
			" calc_off_time DATE," +
			" sim_off_time DATE," +
			" calc_on_time DATE," +
			" sim_on_time DATE," +
			" sched_in_time DATE," +
			" calc_in_time DATE," +
			" sim_in_time DATE," +
			" sim_airborne_time FLOAT, " +
			" turn_time FLOAT," +
			" pushback_time FLOAT," +
			" taxi_out_time FLOAT," + 
			" taxi_in_time FLOAT," + 
			" gate_out_delay FLOAT," +
			" dep_delay FLOAT, " +
			" airborne_delay FLOAT, " +
			" arr_delay FLOAT, " +
			" taxi_in_delay FLOAT, " +
			" dep_fix VARCHAR(8)," +
			" dep_fix_delay FLOAT," +
			" arr_fix VARCHAR(8)," +
			" arr_fix_delay FLOAT" +
		")";
		
    	// Input flights file and path.
    	BufferedReader br = null;
    	Connection conn   = null;
    	
    	try{
    		br = new BufferedReader(new FileReader(flightsFileNPath));
    		
    		// Determine how many rows of good data are in file.
    		// Data is comma separated.  Keep empty entries and
    		// often fields are not populated.
    		String line = null;
    		String[] ss = null;
    		String comma = ",";
    		int nFlightsGood = 0;
    		line = br.readLine();// skip the first line.
			while((line = br.readLine()) != null){
				// Skip comments.
				if(line.charAt(0) != '#'){
					// Make sure flight has an itinerary number,
					// is a non-VFR flight and the scheduled gate out times is 
					// present.
					ss = line.split(comma);
					int fId = Integer.valueOf(ss[2].trim());
					if(!(ss[0].equals("") || fId < 0 || ss[7].equals(""))){
						nFlightsGood++;
					}
				}
			}
			br.close();
			
			// Create table if necessary.
			boolean commitFlag = true;
			boolean succCT = true;
			conn = JDBCManager.openConnection(props, true);

			if(createTable){	
				String dropStr = "drop table " + flightOutputTable;
				String createStr =
					"create table " + flightOutputTable + " " + tableFields;
				succCT = JDBCManager.createDatabaseObject(conn, 
						commitFlag, dropStr, createStr);
				if(!succCT) success = succCT;
			} else {
				// Check if table exists.  Will throw an exception if it fails.
				String selectStr = "select * from " + flightOutputTable + 
					" where rownum=1";
				Statement stmt = conn.createStatement();
				stmt.executeQuery(selectStr);
				JDBCManager.close(stmt);
			}
			
			// Both the insert string and the column types must be
			// consistent with the create table string above.
			int[] colType = 
				JDBCManager.createColTypesFromCreateTableStr(tableFields);
			Object[][] data = new Object[nFlightsGood][colType.length];
			String insertRowStr = 
				JDBCManager.createInsertRowStrForPrepStmt(flightOutputTable,colType.length);
			
			//---------------------------------------------------------
			// loop over all flights.                                --
			//---------------------------------------------------------
    		br = new BufferedReader(new FileReader(flightsFileNPath));
    		
    		String depAirport=null,arrAirport=null,carrier=null;
    		Integer itinNum=null,legNum=null,schedId=null;
    		Float airborneTime = 0.f,turnTime=0.f,pushbackTime=0.f;
    		Float taxiInTime=0.f,taxiOutTime=0.f;
    		Float gateOutDelay=0.f,depDelay=0.f;
    		Float airDelay=0.f,arrDelay=0.f,taxiInDelay=0.f;
    		String etmsAcType=null;
    		String depFix=null,arrFix=null;
    		Float depFixDelay=null,arrFixDelay=null;
    		
    		Timestamp baseDate=null,forecastDate=null;
    		baseDate = (new SQLDate2(baseDay,SQLDate2.Element.dd)).getAsTimestamp();
			forecastDate = (new SQLDate2(forecastDay,SQLDate2.Element.dd)).getAsTimestamp();
			
		    line = br.readLine();// skip the first line.
		    int flight=0;
			while((line = br.readLine()) != null){
				// Skip comments.
				if(line.charAt(0) != '#'){
					
				   	// Extract all elements from the line. Don't remove empty 
					// strings because often fields are not populated.
					ss = line.split(comma);
						
					// Pull out the ones I want. Only take flights
					// that have flown, which at least is indicated by
					// the itinerary number being present. and have
					// scheduled gate out times.
					int fId = Integer.valueOf(ss[2].trim());
					if(!(ss[0].equals("") || fId < 0 || ss[7].equals("") )){
						
						data[flight][0] = baseDate;
						data[flight][1] = forecastDate;
						data[flight][2] = itinNum  = new Integer(ss[0]);
						if(ss[1].equals(""))legNum = null;
						else legNum   = new Integer(ss[1]);
						data[flight][3] = legNum;
						if(ss[2].equals(""))schedId = null;
						else schedId  = new Integer(ss[2]);
						data[flight][4] = schedId;
						data[flight][5] = carrier = ss[3];
						data[flight][6] = etmsAcType = ss[4];
						data[flight][7] = depAirport =ss[5];
						data[flight][8] = arrAirport =ss[6];
						// Get some times. 
						int timeInSecs = 0;
						SQLDate2 date = null;
						for(int i=0;i<9;i++){
							timeInSecs = Integer.valueOf(ss[7+i].trim());
							date = new SQLDate2(baseDate);
							date.addTime(SQLDate2.Element.ss, timeInSecs);
							data[flight][9+i] = date.getAsTimestamp();
						}
						data[flight][18] = airborneTime = new Float(ss[16])/60.f;
						data[flight][19] = turnTime = new Float(ss[17])/60.f;
						data[flight][20] = pushbackTime = new Float(ss[18])/60.f;
						data[flight][21] = taxiOutTime = new Float(ss[19])/60.f;
						data[flight][22] = taxiInTime = new Float(ss[20])/60.f;
						data[flight][23] = gateOutDelay = new Float(ss[21])/60.f;
						data[flight][24] = depDelay= new Float(ss[22])/60.f;
						data[flight][25] = airDelay = new Float(0.f);
						data[flight][26] = arrDelay = new Float(ss[23])/60.f;
						data[flight][27] = taxiInDelay  = new Float(ss[24])/60.f;
 
						data[flight][28] = depFix = ss[25];
						data[flight][29] = depFixDelay = new Float(ss[26])/60.f;
						data[flight][30] = arrFix = ss[27];
						data[flight][31] = arrFixDelay = new Float(ss[28])/60.f;
						
						flight++;
					}
				}
			}	
	    	br.close();
	    	
			// Write to table.  Commit all changes as they happen.
	    	if(succCT){
		    	if(!JDBCManager.populateTable(conn,commitFlag,
		    			flightOutputTable,insertRowStr,data,colType)){
		    		success = false;
		    	}
	    	}
	    	
		}catch(SQLException e){
			e.printStackTrace();
			success = false;
		} catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			success = false;
		} catch(IOException ioe){
			ioe.printStackTrace();
			success = false;
		} finally{ 
			   JDBCManager.close(conn);    	
		}	
		return success;
	}
	
	
	/**
	 * Creates a table of important fields in a simulation schedule*.txt
	 * input file.
	 * @param props
	 * @param createTable If <code>true</code> then create the table and 
	 *   and destroy the old one if it exists; if not, then not.
	  * @param baseDay Base date associated with the flights in the output
	 *   file.  Format: yyyymmdd.  Must be consistent with the date given to
	 *   data in the corresponding schedule table created by
	 *   {@link #createFlightOutputTable(Properties, boolean, String, String, File, String)}.
	 * @param forecastDay Forecast date associated with the flights in the 
	 *   output table.  Format: "yyyymmdd".
	 *   Must be consistent with the date given to data in the corresponding
	 *   flight schedule table create by 
	 *   {@link #createFlightOutputTable(Properties, boolean, String, String, File, String)}
	 *   above.
	 * @param schedFileNPath  Name and directory of NASPAC input
	 *   flight schedule.
	 * @param schedTable Name of created table.
	 * @return <code>true</code> if successful; <code>false</false> if not.
	 */
	public static boolean createScheduleTable(Properties props, 
			boolean createTable, String baseDay, String forecastDay, 
			File schedFileNPath, String schedTable){
		
		final String method = className + ".createScheduleTable()";
		boolean success = true;
		
		final String tableFields = 
			" (" +
			" base_day DATE, " +
			" forecast_day DATE, " +
			" id_num INTEGER, " +
			" act_date DATE," +
			" acid VARCHAR(10), " +
			" flight_index INTEGER, " +
			" out_time DATE, " +  // This is scheduled gate out time.
			" off_time DATE," +
			" on_time DATE," +
			" in_time DATE," +
			" filed_altitude FLOAT," +
			" filed_airspeed FLOAT," +
			" etms_departure_airport VARCHAR(5), " +
			" etms_arrival_airport VARCHAR(5)," +
			" dept_cntry_code INTEGER, " +
			" arr_cntry_code INTEGER, " +
			" etms_aircraft_type VARCHAR(10)," +
			" physical_class VARCHAR(5), " +
			" etms_user_class VARCHAR(5)," +
			" flew_flag INTEGER," +
			" airspace_code INTEGER," +
			" dept_icao_code VARCHAR(10)," +
			" arr_icao_code VARCHAR(10)," +
			" atop_user_class VARCHAR(50)," +
			" bada_type VARCHAR(10)," +
			" bada_type_source VARCHAR(10)," +
			" sched_flag VARCHAR(5) " +
			")";
		
    	// Input flights file and path.
    	BufferedReader br = null;
    	Connection conn   = null;
    	
    	try{
    		br = new BufferedReader(new FileReader(schedFileNPath));
    		
    		// Determine how many rows of good data are in file.
    		// Data is comma separated.  Keep empty entries and
    		// often fields are not populated.  Ignore flights
    		// with a negative id_num since they won't be in the
    		// flight_output file anyways.
    		String line = null;
    		String[] ss = null;
    		String comma = ",";
    		char beginCommentChar='#';
    		int nFlightsGood = 0;
    		// Skip all beginning lines that start with "#".
    		nFlightsGood = 0; // last line read was good.
			while((line = br.readLine()) != null){
				// Skip all comment lines, i.e., that begin with "#".
				if(line.trim().charAt(0)!= beginCommentChar){
					ss = line.split(comma);
					// Make sure flight has a positive id number.
					if(!(Integer.valueOf(ss[0]) < 0)){
						nFlightsGood++;
					}
				}
			}
			br.close();
			
			// Set up table. The id_num links a flight in this file to
			// one, schedule_id, in the flight_output file.
			// In ETMS, a flight is uniquely specified by the act_date, ac_id
			// and flight_index; Thus, we need to get those to make a 
			// comparison with ETMS data, specifically that in AspmFlightData.
			// "out_time" is the scheduled departure (gate-out) time.  It
			// should be the same as the "filed_gate_out_time" in the flight
			// output file.  Also, the dept and arr airports should be the
			// same.  These provide an extra check on the data.
			
			// Create table if necessary.
			boolean commitFlag = true;
			boolean succCT = true;
			conn = JDBCManager.openConnection(props, true);
			if(createTable){
				String dropStr = "drop table " + schedTable;
				String createStr = "create table " + schedTable + tableFields;
				succCT = JDBCManager.createDatabaseObject(conn, 
						commitFlag, dropStr, createStr);
				if(!succCT) success = succCT;
			} else {
				// Check if table exists.  Will throw an exception if it fails.
				String selectStr = "select * from " + schedTable + 
					" where rownum=1";
				Statement stmt = conn.createStatement();
				stmt.executeQuery(selectStr);
				JDBCManager.close(stmt);
			}

			// Get column types and insert string.  Must be consistent with
			// table fields described above.
			int[] colType = 
				JDBCManager.createColTypesFromCreateTableStr(tableFields);
			Object[][] data = new Object[nFlightsGood][colType.length];
			String insertRowStr = 
				JDBCManager.createInsertRowStrForPrepStmt(schedTable,colType.length);
			
			//---------------------------------------------------------
			// loop over all flights.                                --
			//---------------------------------------------------------
    		br = new BufferedReader(new FileReader(schedFileNPath));
    		String acId,depAirport,arrAirport;
    		Integer idNum=null,fltIndx=null,depCntryCode=null,arrCntryCode=null;
    		Timestamp outTime=null,offTime=null,onTime=null,inTime=null;
    		Timestamp actDate=null,baseDate=null,forecastDate=null;
    		Float airspeed=null,altitude=null;
    		String etmsAcType=null,physClass=null,etmsUserClass=null;
    		Integer flewFlag=null,airspaceCode=null;
    		String depIcaoCode=null,arrIcaoCode=null,atopUserClass=null;
    		String badaType=null,badaTypeSource=null,schedFlag=null;
    		
    		baseDate = (new SQLDate2(baseDay,SQLDate2.Element.dd)).getAsTimestamp();
			forecastDate = (new SQLDate2(forecastDay,SQLDate2.Element.dd)).getAsTimestamp();
		    
		    int flight=0;
			while((line = br.readLine()) != null){
				if(line.trim().charAt(0)!= beginCommentChar){
				   	// Extract all elements from the line. Don't remove empty 
					// strings because often fields are not populated.
					ss = line.split(comma);
						
					// Pull out the ones I want. Only take flights
					// with a positive id number since only those are in
					// the flight_output file.  The reason for this is that
					// those with a negative number are added VFR flights that
					// don't have a schedule because they don't travel from one
					// airport to another, e.g., they are helicopters.  As such they
					// increase the demand at airports, yet don't have "delay", so 
					// are not counted.
					if(!(Integer.valueOf(ss[0]) < 0)){
					
						data[flight][0] = baseDate;
						data[flight][1] = forecastDate;
						data[flight][2] = idNum   = new Integer(ss[0]);
						data[flight][3] = actDate = (new SQLDate2(ss[1],SQLDate2.Element.dd)).getAsTimestamp();
						data[flight][4] = acId    = ss[2];
						data[flight][5] = fltIndx = new Integer(ss[3]);
						if(ss[5].equals("")) outTime = null;
						else outTime = (new SQLDate2(ss[5],SQLDate2.Element.ss)).getAsTimestamp();
						data[flight][6] = outTime;
						if(ss[7].equals("")) offTime = null;
						else offTime = (new SQLDate2(ss[7],SQLDate2.Element.ss)).getAsTimestamp();	
						data[flight][7] = offTime;
						if(ss[9].equals("")) onTime = null;
						else onTime  = (new SQLDate2(ss[9],SQLDate2.Element.ss)).getAsTimestamp();
						data[flight][8] = onTime;
						if(ss[11].equals("")) inTime = null;
						else inTime  = (new SQLDate2(ss[11],SQLDate2.Element.ss)).getAsTimestamp();					
						data[flight][9] = inTime;
						if(ss[12].equals("")) altitude = null;
						else altitude= new Float(ss[12]);
						data[flight][10]= altitude;
						if(ss[13].equals("")) airspeed = null;
						else airspeed= new Float(ss[13]);
						data[flight][11]= airspeed;
						data[flight][12] = depAirport=ss[14];
						data[flight][13] = arrAirport=ss[15];
						data[flight][14] = depCntryCode = new Integer(ss[19]);
						data[flight][15] = arrCntryCode = new Integer(ss[23]);
						data[flight][16] = etmsAcType   = ss[24];
						data[flight][17] = physClass    = ss[25];
						data[flight][18] = etmsUserClass= ss[26];
						data[flight][19] = flewFlag     = new Integer(ss[27]);
						data[flight][20] = airspaceCode = new Integer(ss[28]);
						data[flight][21] = depIcaoCode  = ss[29];
						data[flight][22] = arrIcaoCode  = ss[30];
						data[flight][23] = atopUserClass= ss[31];
						data[flight][24] = badaType     = ss[32];
						data[flight][25] = badaTypeSource=ss[33];
						data[flight][26] = schedFlag    = ss[34];
						
						flight++;
					}
				}
			}	
	    	br.close();
	    	
			// Write to table.  Commit all changes as they happen.
	    	if(succCT){
		    	if(!JDBCManager.populateTable(conn,commitFlag,
		    			schedTable,insertRowStr,data,colType)){
		    		success = false;
		    	}
	    	}
	    	
		}catch(SQLException e){
			e.printStackTrace();
			success = false;
		} catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			success = false;
		} catch(IOException ioe){
			ioe.printStackTrace();
			success = false;
		} finally{ 
			   JDBCManager.close(conn);    	
		}	
		return success;
	}
	
	/**
	 * Combines the fields in the input schedule and flight output tables.
	 * Done so can relate sim output variables to specific ETMS flights.
	 * This must be done each time the two input tables have been 
	 * modified.  Thus, must destroy the old table each time.
	 * 
	 * @param props Class properties
	 * @param schedTable  Name of input schedule table.
	 * @param flightOutputTable Flight output table.
	 * @param schedNFlightTable Name of created table.
	 * @return <code>true</code> if successful; <code>false</false> if not.
	 */
	public static boolean combineSchedNFlightTables(Properties props, 
			String schedTable, String flightOutputTable, 
			String schedNFlightTable){
		
		final String method = className + ".combineSchedNFlightTables()";
		boolean success = true;
		
    	Connection conn   = null;
    	
    	try{
    		
    		// Open connection and write to table.  Commit all changes as 
	    	// they happen.
			conn = JDBCManager.openConnection(props, true);
    		boolean commitFlag = true;
    		
    		String dropStr = null, createStr = null;
    		// Create indices to speed up the calc between the two tables.
    		dropStr = "drop index " + schedTable + "_idx";
    		createStr= "create index " + schedTable + "_idx on " +
    			schedTable + " (id_num)";
    		if(!JDBCManager.createDatabaseObject(conn, commitFlag, dropStr, 
    				createStr))success = false;
    		
    		dropStr = "drop index " + flightOutputTable + "_idx";
    		createStr= "create index " + flightOutputTable + "_idx on " +
    			flightOutputTable + " (schedule_id)";
    		if(!JDBCManager.createDatabaseObject(conn, commitFlag, dropStr, 
    				createStr))success = false;
    		
    		dropStr = "drop table " + schedNFlightTable;
    		createStr = "create table " + schedNFlightTable + " as " +
	    		" select a.base_day,a.forecast_day,a.act_date,a.id_num,a.acid, " +
	    		" a.flight_index,a.out_time,a.off_time,a.on_time,a.in_time, " + 
	    		" a.filed_altitude,a.filed_airspeed," +
	    		" a.etms_departure_airport as dep_aprt,a.etms_arrival_airport as arr_aprt," +
	    		" a.dept_cntry_code,a.arr_cntry_code," +
	    		" a.etms_aircraft_type as etms_ac_type,a.physical_class,a.etms_user_class,a.flew_flag," +
	    		" a.airspace_code,a.dept_icao_code,a.arr_icao_code,a.atop_user_class," +
	    		" a.bada_type as bada_ac_type,a.bada_type_source as bada_ac_type_source,a.sched_flag," +
	    		" b.itinerary_number,b.leg_num,b.carrier," +
	    		" b.sim_out_time, b.calc_off_time,b.sim_off_time,b.calc_on_time," +
	    		" b.sim_on_time, b.calc_in_time,b.sim_in_time,b.sim_airborne_time,"+
	    		" b.turn_time,b.pushback_time,b.taxi_out_time,b.taxi_in_time,"+
	    		" b.gate_out_delay, b.dep_delay, b.airborne_delay,b.arr_delay,"+
	    		" b.taxi_in_delay," +
	    		" b.dep_fix,b.dep_fix_delay,b.arr_fix,b.arr_fix_delay " +
	    		" from " +
	    		schedTable + " a inner join " + flightOutputTable + " b " +
	    		" on " +
	    		" a.base_day=b.base_day " +
	    		" and a.forecast_day=b.forecast_day" +
	    		" and a.id_num=b.schedule_id " +
	    		" and a.etms_departure_airport=b.dep_aprt " +
	    		" and a.etms_arrival_airport=b.arr_aprt " +
	    		" and a.etms_aircraft_type=b.etms_ac_type" +
	    		" order by a.base_day,a.forecast_day,a.act_date,a.id_num,a.acid, " +
	    		"   a.flight_index";
    		boolean succ =
    			JDBCManager.createDatabaseObject(conn,true, dropStr,createStr);
    		if(succ == false) success = false;	
	    	
		}catch(SQLException e){
			e.printStackTrace();
			success = false;
		} finally{ 
			   JDBCManager.close(conn);    	
		}	
		return success;
	}
	
	/**
	 * Using data in the AspmFlightData table of ETMS and the 
	 * schedNFlightTable created in this class, this method combines
	 * the tables to give all data to compute both actual
	 * and simulated delays.
	 * 
	 * @param props Class properties
	 * @param aspmFlightTable  Table AspmFlightTable from ETMS.
	 * @param schedNFlightTable Table of info from the NASPAC's input
	 *   schedule and output flight table.
	 * @param actNSimTable Combination of sim, flight and ASPM
	 *   flight data tables.
	 * @return <code>true</code> if successful; <code>false</false> if not.
	 */
	public static boolean combineActualNSimulationTables(Properties props, 
			String aspmFlightTable, String schedNFlightTable, 
			String actNSimTable){
		
		final String method = className + ".combineActualNSimulationTables()";
		boolean success = true;
		
    	Connection conn   = null;
    	
    	try{
    		
    		// Open connection.  Commit all changes as they happen.
			conn = JDBCManager.openConnection(props, true);
    		boolean commitFlag = true;
    		
    		String dropStr = null, createStr = null;
    		
    		// Create a schedNFlightTable that has flight identification
    		// in the form that ASPM wants.  A flight in ASPM is identified
    		// uniquely by the fields, faacarrier, fltno and schoutsec.
    		// faacarrier is the first 3 chars of sNFT.ac_id, fltno is
    		// ac_id w/o the first 3 chars and schoutsec is the scheduled
    		// departure time in seconds GMT from 19800101 00:00:00.
    		String tableNew = schedNFlightTable + "New";
    		dropStr = "drop table " + tableNew;
    		createStr = "create table " + tableNew + " as " +
	    		" select base_day, forecast_day, act_date, itinerary_number as itin_num," +
	    		"  id_num, leg_num, acid as ac_id, flight_index, etms_ac_type, " +
	    		"  dep_aprt, arr_aprt, out_time, sim_out_time, sim_off_time, sim_on_time,"+
	    		"  sim_in_time,gate_out_delay,dep_delay,airborne_delay,arr_delay, " +
	    		"  sim_airborne_time,turn_time,pushback_time,taxi_out_time,taxi_in_time,"+
	    		"  dep_fix,dep_fix_delay,arr_fix,arr_fix_delay," +
	    		"  carrier as faacarrier," + 
	    		"  cast(regexp_substr(acid,'[0-9]+') as int) as fltno, " + 
	    		"  to_number(to_char(airport.convert_to_airport_time(dep_aprt,out_time),'yyyymm')) as dep_yyyymm, " +
	    		"  to_number(to_char(airport.convert_to_airport_time(dep_aprt,out_time),'dd')) as dep_day,"+
	    		"  to_number(to_char(airport.convert_to_airport_time(dep_aprt,out_time),'hh24'))as dep_hour,"+
	    		"  ((out_time - to_date('19800101','yyyymmdd'))*(24*3600)) as fpdepsec," +
	    		"  ((sim_in_time - to_date('19800101','yyyymmdd'))*(24*3600)) as sim_in_time_sec " +
	    		" from " + schedNFlightTable +
	    		"  where regexp_like(substr(acid,1,3),'[A-Z]{3}') " + 
	    		"  order by base_day, forecast_day, act_date, acid ";
    		boolean succ =
    			JDBCManager.createDatabaseObject(conn,true, dropStr,createStr);
    		if(succ == false) success = false;	
    		
    		// Create indices to speed up the calc between the two tables.
    		String tableNewIndex = tableNew + "_indx";
    		dropStr = "drop index " + tableNewIndex;
    		createStr= "create index " + tableNewIndex + " on " +
    			tableNew + " (dep_yyyymm,dep_day,dep_hour,faacarrier,dep_aprt,arr_aprt)";
    		if(!JDBCManager.createDatabaseObject(conn, commitFlag, dropStr, 
    				createStr))success = false;
    		
    		// Combine the above and AspmFlightData table to get delay times.
    		// Match on a hell of a lot of things because I don't trust the flight
    		// number, i.e, fltno, field created from the above.
    		dropStr = "drop table " + actNSimTable;
    		createStr = "create table " + actNSimTable + " as " +
	    		"select a.base_day, a.forecast_day,a.act_date,a.itin_num,a.leg_num," +
	    		" a.id_num, a.ac_id, a.flight_index, a.etms_ac_type, " +
	    		" a.dep_aprt, a.arr_aprt, a.faacarrier, a.out_time, " +
	    		" a.sim_out_time,a.sim_off_time,a.sim_on_time, a.sim_in_time, " +
	    		" a.gate_out_delay, a.dep_delay, a.airborne_delay, " +
	    		" a.arr_delay,a.sim_in_time_sec,a.sim_airborne_time,"+
	    		" a.turn_time,a.pushback_time,a.taxi_out_time,a.taxi_in_time," + 
	    		" a.dep_fix,a.dep_fix_delay,a.arr_fix,a.arr_fix_delay,"+
	    		" b.tailno,b.schoutsec,b.actoutsec,b.actoffsec,b.actonsec," +
	    		" b.actinsec, b.nomto, b.actair, b.fpete, b.nomti, b.dlaedct " +
	    		"from " + tableNew + " a, " + aspmFlightTable + " b " +
	    		" where a.dep_yyyymm=b.dep_yyyymm " +
	    		" and a.dep_day = b.dep_day " +
	    		" and a.dep_hour =b.dep_hour " +
	    		" and a.faacarrier=b.faacarrier " +
	    		" and a.dep_aprt=b.dep_locid " +
	    		" and a.arr_aprt=b.arr_locid " +
	    		" and ((a.fpdepsec + 10) > b.fpdepsec and (a.fpdepsec - 10) < b.fpdepsec) " +
	    		" order by a.base_day, a.forecast_day, a.act_date, a.ac_id, a.flight_index, a.id_num ";
    		succ =
    			JDBCManager.createDatabaseObject(conn,true, dropStr,createStr);
    		if(succ == false) success = false;
    		
		}catch(SQLException e){
			e.printStackTrace();
			success = false;
		} finally{ 
			   JDBCManager.close(conn);    	
		}	
		return success;
	}
    	
	/**
	 * Using data in the schedNFlightTable created in this class, this method
	 * creates a bogus table that includes fields of ASPM 
	 * elements with values set to null (Oracle doesn't allow columns of all 
	 * nulls).  Done so can use the methods below for computing delays.
	 * @param props Class properties
	 * @param schedNFlightTable Table of info from the NASPAC's input
	 *   schedule and output flight table.
	 * @param actNSimTable Combination of sim, flight and ASPM
	 *   flight data tables.
	 * @return <code>true</code> if successful; <code>false</false> if not.
	 */
	public static boolean createBogusActualNSimulationTables(Properties props, 
			String schedNFlightTable, String actNSimTable){
		
		String method = className + ".createBogusActualNSimulationTables()";
		boolean success = true;
		
    	Connection conn   = null;
    	
    	try{
    		
    		// Open connection.  Commit all changes as they happen.
			conn = JDBCManager.openConnection(props, true);
    		boolean commitFlag = true;
    		
    		String dropStr = null, createStr = null;
    		
    		// Create a schedNFlightTable that has flight identification
    		// in the form that ASPM wants.  A flight in ASPM is identified
    		// uniquely by the fields, faacarrier, fltno and schoutsec.
    		// faacarrier is the first 3 chars of sNFT.ac_id, fltno is
    		// ac_id w/o the first 3 chars and schoutsec is the scheduled
    		// departure time in seconds GMT from 19800101 00:00:00.
    		String tableNew = schedNFlightTable + "New";
    		dropStr = "drop table " + tableNew;
    		createStr = "create table " + tableNew + " as " +
	    		" select base_day, forecast_day, act_date, acid as ac_id, flight_index, id_num, " +
	    		"  dept_aprt, arr_aprt, out_time, sim_gate_in_time, " +
	    		"  sim_wheels_off_time, sim_wheels_on_time, sim_gate_out_time, " +
	    		"  pushback_delay,departure_delay,airborne_delay, " +
	    		"  arrival_queueing_delay, gate_in_delay_from_filed, " +
	    		"  substr(acid,1,3) as faacarrier," + 
	    		"  substr(acid,4,7) as fltno, " + 
	    		"  -1 as dep_yyyymm, -1 as dep_day, -1 as dep_hour," +
	    		//"  to_char(faa.convert_to_airport_time(dept_aprt,out_time),'mm'))) as dep_yyyymm, " +
	    		//"  to_number(to_char(faa.convert_to_airport_time(dept_aprt,out_time),'dd')) as dep_day,"+
	    		//"  to_number(to_char(faa.convert_to_airport_time(dept_aprt,out_time),'hh24'))as dep_hour,"+
	    		"  ((out_time - to_date('19800101','yyyymmdd'))*(24*3600)) as schoutsec," +
	    		"  ((sim_gate_in_time - to_date('19800101','yyyymmdd'))*(24*3600)) as sim_gate_in_sec " +
	    		" from " + schedNFlightTable +
	    		"  where regexp_like(substr(acid,1,3),'[A-Z]{3}') " + 
	    		"  order by base_day, forecast_day, act_date, acid ";
    		boolean succ =
    			JDBCManager.createDatabaseObject(conn,true, dropStr,createStr);
    		if(succ == false) success = false;	
    		
    		// Create indices to speed up the calc between the two tables.
    		//String tableNewIndex = tableNew + "_indx";
    		//dropStr = "drop index " + tableNewIndex;
    		//createStr= "create index " + tableNewIndex + " on " +
    		//	tableNew + " (dep_yyyymm,dep_day,dep_hour,faacarrier,dept_aprt,arr_aprt)";
    		//if(!JDBCManager.createDatabaseObject(conn, commitFlag, dropStr, 
    		//		createStr))success = false;
    		
    		// Create a bogus combo of the above and AspmFlightData table.
    		// Populate the ASPM fields with nulls.  Since Oracle doesn't like
    		// columns with all nulls do and end-around. Populating 
    		// all nulls) to make sure the person reading the table knows what is going on.
    		dropStr = "drop table " + actNSimTable;
    		createStr = "create table " + actNSimTable + " as " +
	    		"select a.base_day, a.forecast_day, a.act_date, a.ac_id, " +
	    		" a.flight_index, a.id_num, " +
	    		" a.dept_aprt, a.arr_aprt, a.faacarrier, a.out_time, " +
	    		" a.sim_gate_out_time,a.sim_wheels_off_time, " +
	    		" a.sim_wheels_on_time, a.sim_gate_in_time, " +
	    		" a.pushback_delay, a.departure_delay, a.airborne_delay, " +
	    		" a.arrival_queueing_delay, a.gate_in_delay_from_filed, " +
	    		" a.sim_gate_in_sec, a.schoutsec," +
	    		" (case when 0>1 then 0 else null end) as actinsec," +
	    		" (case when 0>1 then 0 else null end) as actoffsec," +
	    		" (case when 0>1 then 0 else null end) as actoutsec," +
	    		" (case when 0>1 then 0 else null end) as nomto, " +
	    		" (case when 0>1 then 0 else null end) as actair," +
	    		" (case when 0>1 then 0 else null end) as fpete," +
	    		" (case when 0>1 then 0 else null end) as nomti," +
	    		" (case when 0>1 then 0 else null end) as dlaedct" +
	    		" from " + tableNew + " a " + 
	    		" order by a.base_day, a.forecast_day, a.act_date, a.ac_id," +
	    		" a.flight_index, a.id_num ";
    		succ =
    			JDBCManager.createDatabaseObject(conn,true, dropStr,createStr);
    		if(succ == false) success = false;
    		
		}catch(SQLException e){
			e.printStackTrace();
			success = false;
		} finally{ 
			   JDBCManager.close(conn);    	
		}	
		return success;
	}
	
	/**
	 * Computes various systemwide delays using a table that is a combo of the
	 * schedule, flight files and the ASPM flight data table.   
	 * @param props Model properties
	 * @param createTable If <code>true</code> then create the table too; if 
	 *   not then not.
	 * @param baseDay  Base date of delays computed.  Should be in the same format
	 *   as the base_day variable in the actNSimTable, which is: "yyyymmdd".
	 * @param forecastDay Forecast date of delays.  Should be in the same
	 *   format as the forecast_day variable in the actNSimTable, which
	 *   should be "yyyymmdd".
	 * @param actNSimTable Table produced by 
	 * {@link #combineActualNSimulationTables(Properties, String, String, String)}.
	 * @param delayTable Table that contains the delays for various days.
	 * @return <code>true</code> if successful; <code>false</code> if not.
	 */
	public static boolean computeDelaysSystemwide(Properties props, boolean createTable,
			String baseDay, String forecastDay, String actNSimTable, 
			String delayTable){
		
		boolean success = true;
		Connection conn  = null;
		Statement  stmt  = null;
		PreparedStatement pstmt = null;
		ResultSet  rset  = null;
		
		try{
			// Open connection.  Commit all changes as they happen.
			boolean commitFlag = true;
			conn = JDBCManager.openConnection(props, commitFlag);
			
			boolean succCT = true;
			if(createTable){
				String dropStr   = "drop table " + delayTable;
				String createStr = "create table " + delayTable + " (" +
				" base_day DATE, " +
				" forecast_day DATE, " +
				" delay_act FLOAT, " +
				" delay_act_depNair FLOAT, " +
				" delay_sim1 FLOAT, " +
				" delay_sim2 FLOAT, " +
				" delay_sim3 FLOAT, " +
				" delay_sim4 FLOAT) ";
				succCT = JDBCManager.createDatabaseObject(conn, 
						commitFlag, dropStr, createStr);
				if(!succCT) success = succCT;
			} else {
				// Check if table exists.  Will throw an exception if it fails.
				String selectStr = "select * from " + delayTable + 
					" where rownum=1";
				stmt = conn.createStatement();
				stmt.executeQuery(selectStr);
				JDBCManager.close(stmt);
			}
			
    		// Compute delays from this and add to delay table.
			if(succCT){
	    		String selectStr = "select " +
	    		    " base_day, forecast_day, " +
	    			" sum(actinsec/60 - (schoutsec/60 + nomto + fpete + nomti))/count(*) as delay_act, " +
	    			" sum(actair - fpete + actoffsec/60 - actoutsec/60 - nomto)/count(*) as delay_act_depNair, " +
	    			" sum(sim_gate_in_sec/60 - (schoutsec/60 + nomto + fpete + nomti))/count(*) as delay_sim1, " +
	    			" sum(departure_delay + airborne_delay)/count(*) as delay_sim2, " +
	    			" sum(pushback_delay + departure_delay + airborne_delay)/count(*) as delay_sim3, " +
	    			" sum(gate_in_delay_from_filed)/count(*) as delay_sim4 " +
	    			" from " + actNSimTable +
	    			" where base_day=to_date(\'" + baseDay + "\','yyyymmdd')" +
	    			"   and forecast_day=to_date(\'" + forecastDay + "\','yyyymmdd')" +
	    			" group by base_day, forecast_day " +
	    			" order by base_day, forecast_day";
	    		
	    		stmt = conn.createStatement();
	    		rset = stmt.executeQuery(selectStr);
	    		rset.next();
	    		Timestamp baseTs = 
	    			(new SQLDate2(baseDay,SQLDate2.Element.dd)).getAsTimestamp();
	    		Timestamp forecastTs = 
	    			(new SQLDate2(forecastDay,SQLDate2.Element.dd)).getAsTimestamp();
	    		float delay_act = rset.getFloat("delay_act");
	    		float delay_act_depNair = rset.getFloat("delay_act_depNair");
	    		float delay_sim1 = rset.getFloat("delay_sim1");
	    		float delay_sim2 = rset.getFloat("delay_sim2");
	    		float delay_sim3 = rset.getFloat("delay_sim3");
	    		float delay_sim4 = rset.getFloat("delay_sim4");
	
	    		String insertStr = "insert into " + delayTable + 
	    			" values (?,?,?,?,?,?,?,?)";
	    		pstmt = conn.prepareStatement(insertStr);
	    		pstmt.setTimestamp(1, baseTs);
	    		pstmt.setTimestamp(2,forecastTs);
	    		pstmt.setFloat(3, delay_act);
	    		pstmt.setFloat(4, delay_act_depNair);
	    		pstmt.setFloat(5, delay_sim1);
	    		pstmt.setFloat(6, delay_sim2);
	    		pstmt.setFloat(7, delay_sim3);
	    		pstmt.setFloat(8, delay_sim4);
	    		pstmt.executeUpdate();	
			}
		}catch(SQLException e){
			e.printStackTrace();
			success = false;
		} finally{ 
			JDBCManager.close(pstmt);
			JDBCManager.close(stmt);
			JDBCManager.close(conn);    	
		}	
		return success;
	}
	
	/**
	 * Computes various delays using a combo of the schedule, flight files
	 * and the ASPM flight data table.  Same as 
	 * {@link #computeDelaysSystemwide(Properties, boolean, String, String, String, String)} above
	 * except that this method computes delays for each OEP 35 airport, rather
	 * than for the whole system.
	 * @param props Model properties
	 * @param createTable If <code>true</code> then create the table; if 
	 *   not then not.
	 * @param baseDay  Base date of delays computed.  Should be in the same format
	 *   as the base_day variable in the actNSimTable, which is: "yyyymmdd".
	 * @param forecastDay Forecast date of delays.  Should be in the same
	 *   format as the forecast_day variable in the actNSimTable, which
	 *   should be "yyyymmdd".
	 * @param airportGroupsTable Table of airports for various groups, such as
	 *   OEP35.
	 * @param airportGroup  The group of airports we want to consider.  Here,
	 *   this will be OEP35.
	 * @param actNSimTable Table produced by 
	 * {@link #combineActualNSimulationTables(Properties, String, String, String)}.
	 * @param delayAprtTable Table that contains the delays for various days and
	 *   airports
	 * @return <code>true</code> if successful; <code>false</code> if not.
	 */
	public static boolean computeDelaysByAirport(Properties props, 
			boolean createTable, String baseDay, String forecastDay,
			String airportGroupsTable, String airportGroup, 
			String actNSimTable, String delayAprtTable){
		
		boolean success = true;
		Connection conn  = null;
		Statement  stmt  = null;
		PreparedStatement pstmt = null;
		ResultSet  rset  = null;
		
		try{
			// Open connection.  Commit all changes as they happen.
			boolean commitFlag = true;
			conn = JDBCManager.openConnection(props, commitFlag);
			
			boolean succCT = true;
			if(createTable){
				String dropStr   = "drop table " + delayAprtTable;
				String createStr = "create table " + delayAprtTable + " (" +
					" base_day DATE, " +
					" forecast_day DATE, " +
					" arr_aprt VARCHAR(5), " +
					" delay_act FLOAT, " +
					" delay_act_depNair FLOAT, " +
					" delay_act_edctNair FLOAT, " +
					" delay_sim1 FLOAT, " +
					" delay_sim2 FLOAT, " +
					" delay_sim3 FLOAT, " +
					" delay_sim4 FLOAT, " +
					" delay_sim5 FLOAT)";
				succCT = JDBCManager.createDatabaseObject(conn, 
						commitFlag, dropStr, createStr);
				if(!succCT) success = succCT;
			} else {
				// Check if table exists.  Will throw an exception if it fails.
				String selectStr = "select * from " + delayAprtTable + 
					" where rownum=1";
				stmt = conn.createStatement();
				stmt.executeQuery(selectStr);
				JDBCManager.close(stmt);
			}
			
    		// Compute delays from this and add to delay table.
			if(succCT){
	    		String selectStr = "select base_day, forecast_day, arr_aprt, " +
	    			" sum(actinsec/60 - (schoutsec/60 + nomto + fpete + nomti))/count(*) as delay_act, " +
	    			" sum( actair - fpete + actoffsec/60 - actoutsec/60 - nomto)/count(*) as delay_act_depNair, " +
	    			" sum( actair - fpete + dlaedct)/count(*) as delay_act_edctNair, " +
	    			" sum(sim_gate_in_sec/60 - (schoutsec/60 + nomto + fpete + nomti))/count(*) as delay_sim1, " +
	    			" sum(departure_delay + airborne_delay)/count(*) as delay_sim2, " +
	    			" sum(pushback_delay + departure_delay + airborne_delay)/count(*) as delay_sim3, " +
	    			" sum(gate_in_delay_from_filed)/count(*) as delay_sim4, " +
	    			" sum(airborne_delay)/count(*) as delay_sim5 " +
	    			" from " + actNSimTable +
	    			" where trim(arr_aprt) in (select faa from " + airportGroupsTable + " " +
	    			"                          where " + airportGroup + "='Y') "  + 
	    			"   and base_day=to_date(\'" + baseDay + "\','yyyymmdd')" +
	    			"   and forecast_day=to_date(\'" + forecastDay + "\','yyyymmdd')" +
	    			" group by base_day, forecast_day, arr_aprt " +
	    			" order by base_day, forecast_day, arr_aprt ";
	    		
	    		String insertRowStr = "insert into " + delayAprtTable + 
				" values (?,?,?,?,?,?,?,?,?,?,?)";
	    		pstmt = conn.prepareStatement(insertRowStr);
	    		
	    		String arr_aprt=null;
	    		Timestamp day = null;
	    		float delay_act=0.f,delay_act_depNair=0.f,delay_act_edctNair=0.f;
	    		float delay_sim1=0.f;
	    		float delay_sim2=0.f,delay_sim3=0.f,delay_sim4=0.f,delay_sim5=0.f;
	    		
	    		Timestamp baseTs = 
	    			(new SQLDate2(baseDay,SQLDate2.Element.dd)).getAsTimestamp();
	    		Timestamp forecastTs = 
	    			(new SQLDate2(forecastDay,SQLDate2.Element.dd)).getAsTimestamp();
	    		stmt = conn.createStatement();
	    		rset = stmt.executeQuery(selectStr);
	    		while(rset.next()){
		    		arr_aprt = rset.getString("arr_aprt");
		    		delay_act = rset.getFloat("delay_act");
		    		delay_act_depNair  = rset.getFloat("delay_act_depNair");
		    		delay_act_edctNair = rset.getFloat("delay_act_edctNair");
		    		delay_sim1 = rset.getFloat("delay_sim1");
		    		delay_sim2 = rset.getFloat("delay_sim2");
		    		delay_sim3 = rset.getFloat("delay_sim3");
		    		delay_sim4 = rset.getFloat("delay_sim4");
		    		delay_sim5 = rset.getFloat("delay_sim5");
		
		    		pstmt.setTimestamp(1, baseTs);
		    		pstmt.setTimestamp(2, forecastTs);
		    		pstmt.setString(3,arr_aprt);
		    		pstmt.setFloat(4, delay_act);
		    		pstmt.setFloat(5, delay_act_depNair);
		    		pstmt.setFloat(6, delay_act_edctNair);
		    		pstmt.setFloat(7, delay_sim1);
		    		pstmt.setFloat(8, delay_sim2);
		    		pstmt.setFloat(9, delay_sim3);
		    		pstmt.setFloat(10, delay_sim4);
		    		pstmt.setFloat(11,delay_sim5);
		    		pstmt.addBatch();	
	    		}
	    		int[] results = pstmt.executeBatch();
				JDBCManager.close(pstmt);
			
				// check results.
				for(int row=0;row<results.length;row++){
					// A successful call either returns an int > 0 or a value of
					// Statement.SUCCESS_NO_INFO.
					if(!(results[row] == Statement.SUCCESS_NO_INFO ||
							results[row] >= 0)){
						success = false;
						break;
					}
				}
			}
	    	
		}catch(SQLException e){
			e.printStackTrace();
			success = false;
		} finally{ 
			JDBCManager.close(pstmt);
			JDBCManager.close(stmt);
			JDBCManager.close(conn);    	
		}	
		return success;
	}
	
	/**
	 * Create the delay table and destroys one of the same name first
	 * if it exists.   Delay values for a day will be inserted at the 
	 * end of the analyze method.
	 * @param props
	 * @param delayTable
	 * @return <code>true</code> if successful; <code>false</code> if not.
	 */
/*
	public static boolean createDelayTable(Properties props,
		String delayTable){
		
		// Check if table exists.
		boolean success = true;
		Connection conn = null;
		Statement  stmt = null;
		String selectStr = "select * from " + delayTable + " where rownum=1";
		try {
			boolean autoCommit = true;
			conn = JDBCManager.openConnection(props, autoCommit);
			stmt = conn.createStatement();
			try {
				stmt.executeQuery(selectStr);
			} catch(SQLException se){
				// If have an exception, then table doesn't exist (hopefully!)
				// then create it.
				JDBCManager.close(stmt);
				String createStr = "create table " + delayTable + " (" +
					" day DATE, " +
					" delay_act FLOAT, " +
					" delay_act_depNair FLOAT, " +
					" delay_sim1 FLOAT, " +
					" delay_sim2 FLOAT, " +
					" delay_sim3 FLOAT, " +
					" delay_sim4 FLOAT) ";
				stmt = conn.createStatement();
				stmt.execute(createStr);
			}
		} catch (SQLException se2){
			se2.printStackTrace();
			success = false;
		} finally {
			JDBCManager.close(stmt);
			JDBCManager.close(conn);
		}
		return success;		
	}
*/	
	/**
	 * Create the arrival airport specific delay table if it doesn't exist.  
	 * If it does, just return.  Delay values for a day will be inserted at 
	 * the end of the analyze method.
	 * @param props
	 * @param delayAprtTable
	 * @return <code>true</code> if successful; <code>false</code> if not.
	 */
/*
	public static boolean createDelayAprtTable(Properties props,
		String delayAprtTable){
		
		// Check if table exists.
		boolean success = true;
		Connection conn = null;
		Statement  stmt = null;
		String selectStr = "select * from " + delayAprtTable + " where rownum=1";
		try {
			boolean autoCommit = true;
			conn = JDBCManager.openConnection(props, autoCommit);
			stmt = conn.createStatement();
			try {
				stmt.executeQuery(selectStr);
			} catch(SQLException se){
				// If have an exception, then table doesn't exist (hopefully!)
				// then create it.
				JDBCManager.close(stmt);
				String createStr = "create table " + delayAprtTable + " (" +
					" day DATE, " +
					" arr_aprt VARCHAR(5), " +
					" delay_act FLOAT, " +
					" delay_act_depNair FLOAT, " +
					" delay_act_edctNair FLOAT, " +
					" delay_sim1 FLOAT, " +
					" delay_sim2 FLOAT, " +
					" delay_sim3 FLOAT, " +
					" delay_sim4 FLOAT, " +
					" delay_sim5 FLOAT)";
				stmt = conn.createStatement();
				stmt.execute(createStr);
			}
		} catch (SQLException se2){
			se2.printStackTrace();
			success = false;
		} finally {
			JDBCManager.close(stmt);
			JDBCManager.close(conn);
		}
		return success;		
	}
*/
	
	/**
	 * Reads the router input properties from a file.  If needed
	 * properties are not set, i.e., are <code>null</code> the method will 
	 * print a description.  If some properties are used as
	 * booleans, yet are not set to <code>true</code> or <code>false</code>,
	 * then the method will also print a description.  If the method return 
	 * <code>false</code> it is suggested that the calling method cause 
	 * the program to exit.
	 * @param propsFileNPath  The properties file and where it is.
	 * @param props contains model input properties.
	 * @return <code>true</code> if successful; <code>false</code> if not, 
	 * though if it has any problem it will either throw an exception, or
	 * print out an error description.
	 */
	public static boolean readModelInputProperties(File propsFileNPath, 
			Properties props){
		// Check if the properties object is instantiated.  Don't bother
		// with propsFileNPath since the file input stream constructor will 
		// take care of that.
		if(props == null){
			throw new IllegalArgumentException("props object is null");
		}
		
		String method = "readModelInputProperties";
		FileInputStream fis = null; 
    	
    	try{
    		fis = new FileInputStream(propsFileNPath);
    		props.load(fis); 
    	} catch(FileNotFoundException fnfe){
    		fnfe.printStackTrace();
    	} catch(IOException ioe){
    		ioe.printStackTrace();
    	} finally{ 
            try{
               fis.close();
            }catch(IOException e){
               e.printStackTrace();
               System.exit(1);
            }
    	}
    	// Check that all the required properties have been set.
		String[] propertyNames = {"baseDir","mainSubDir","baseDay","forecastDay",
			"dbName","dbUserName","dbUserPassword","dbIPAddress",
			"dbPort","dbDriverClass","dbDriverAlias",
			"createTables","cleanupTables","scheduleInputTable","flightOutputTable",
			"aspmFlightDataTable","airportGroupsTable","airportGroup",
			"schedNFlightOutputTable","delayTable","delayAprtTable",
			"actNSimTable","schedSubDir","schedFilePrefix","schedFileSuffix",
			"flightOutputSubDir","flightOutputFilePrefix","flightOutputFileSuffix"};
			
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
			
		// Make sure createTables and cleanupTables are either "true" or "false."
		// (if is has been set at all of course).
		
		String[] rd= 
			{props.getProperty("cleanupTables"),props.getProperty("createTables")};
		for(int i=0;i<rd.length;i++){
			if(rd != null && 
				!(rd[i].toLowerCase().equals("true") || 
				  rd[i].toLowerCase().equals("false"))){
				System.err.println(method + ": " + rd[i] + " property must be " +
					"either \'true\' or \'false\'.");
				success = false;
			}
		}
    	return success;
	}
	
	/**
	 * Creates a table of important fields in an NasSim flightOutput*.csv
	 * output file, which is input to this method.
	 * @param props
	 * @param createTable If <code>true</code> then create the table and 
	 *   and destroy the old one if it exists; if not, then not.
	 * @param baseDay Base date associated with the flights in the output
	 *   file.  Format: yyyymmdd.  Must be consistent with the date given to
	 *   data in the corresponding schedule table created by
	 *   {@link #createScheduleTable(Properties, boolean, String, String, File, String)}.
	 * @param forecastDay Forecast date associated with the flights in the 
	 *   output table.  Format: "yyyymmdd".
	 *   Must be consistent with the date given to data in the corresponding
	 *   flight schedule table create by 
	 *   {@link #createScheduleTable(Properties, boolean, String, String, File, String)}
	 *   below.
	 * @param flightsFileNPath  Name and directory of NASPAC output flights file.
	 * @param flightOutputTable Name of created table.
	 * @return <code>true</code> if successful; <code>false</false> if not.
	 */
/*
	public static boolean createFlightOutputTableOld(Properties props, 
			boolean createTable, String baseDay, String forecastDay, 
			File flightsFileNPath, String flightOutputTable){
		
		final String method = className + ".createFlightOutputTable()";
		boolean success = true;
		
		// Fields in the table to be created.  For consistency has the same
		// names as the input file with three exceptions.  The "forecast_day" should
		// be the same as the "sim start date." 
		final String tableFields = "(" +
			" base_day DATE, " +
			" forecast_day DATE, " +
			" itinerary_number INTEGER, " +
			" leg_num INTEGER, " +
			" schedule_id INTEGER, " +  // same as the sched id_num.
			" carrier VARCHAR(10), " +
			" etms_ac_type VARCHAR(8)," +
			" departure_airport VARCHAR(5), " +
			" arrival_airport VARCHAR(5), " +
			" filed_gate_out_time DATE, " +
			" calc_out_time DATE," +
			" sim_out_time DATE," +
			" calc_off_time DATE," +
			" sim_off_time DATE," +
			" calc_on_time DATE," +
			" sim_on_time DATE," +
			" filed_gate_in_time DATE," +
			" calc_in_time DATE," +
			" sim_in_time DATE," +
			" sim_airborne_time FLOAT, " +
			" turn_time FLOAT," +
			" turnaround_delay FLOAT," +
			" pushback_delay FLOAT, " +
			" departure_delay FLOAT, " +
			" airborne_delay FLOAT, " +
			" arrival_queue_delay FLOAT, " +
			" taxi_in_delay FLOAT, " +
			" dep_fix VARCHAR(8)," +
			" dep_fix_delay FLOAT," +
			" arr_fix VARCHAR(8)," +
			" arr_fix_delay FLOAT" +
		")";
		
    	// Input flights file and path.
    	BufferedReader br = null;
    	Connection conn   = null;
    	
    	try{
    		br = new BufferedReader(new FileReader(flightsFileNPath));
    		
    		// Determine how many rows of good data are in file.
    		// Data is comma separated.  Keep empty entries and
    		// often fields are not populated.
    		String line = null;
    		String[] ss = null;
    		String comma = ",";
    		int nFlightsGood = 0;
    		line = br.readLine();// skip the first line.
			while((line = br.readLine()) != null){
				// Skip comments.
				if(line.charAt(0) != '#'){
					// Make sure flight has an itinerary number,
					// and is a non-VFR flight,
					// and filed and simulated gate out times are 
					// present.
					ss = line.split(comma);
					int fId = Integer.valueOf(ss[2].trim());
					if(!(ss[0].equals("") || fId < 0 
							|| ss[7].equals("") || ss[8].equals(""))){
						nFlightsGood++;
					}
				}
			}
			br.close();
			
			// Create table if necessary.
			boolean commitFlag = true;
			boolean succCT = true;
			conn = JDBCManager.openConnection(props, true);

			if(createTable){	
				String dropStr = "drop table " + flightOutputTable;
				String createStr =
					"create table " + flightOutputTable + " " + tableFields;
				succCT = JDBCManager.createDatabaseObject(conn, 
						commitFlag, dropStr, createStr);
				if(!succCT) success = succCT;
			} else {
				// Check if table exists.  Will throw an exception if it fails.
				String selectStr = "select * from " + flightOutputTable + 
					" where rownum=1";
				Statement stmt = conn.createStatement();
				stmt.executeQuery(selectStr);
				JDBCManager.close(stmt);
			}
			
			// Both the insert string and the column types must be
			// consistent with the create table string above.
			int[] colType = 
				JDBCManager.createColTypesFromCreateTableStr(tableFields);
			Object[][] data = new Object[nFlightsGood][colType.length];
			String insertRowStr = 
				JDBCManager.createInsertRowStrForPrepStmt(flightOutputTable,colType.length);
			
			//---------------------------------------------------------
			// loop over all flights.                                --
			//---------------------------------------------------------
    		br = new BufferedReader(new FileReader(flightsFileNPath));
    		
    		String depAirport=null,arrAirport=null,carrier=null;
    		Integer itinNum=null,legNum=null,schedId=null;
    		Float airborneTime = 0.f,turnTime=0.f;
    		Float turnaroundDelay=0.f,pushbackDelay=0.f,depDelay=0.f;
    		Float airDelay=0.f,arrQueueDelay=0.f,taxiInDelay=0.f;
    		String etmsAcType=null;
    		String depFix=null,arrFix=null;
    		Float depFixDelay=null,arrFixDelay=null;
    		
    		Timestamp baseDate=null,forecastDate=null;
    		baseDate = (new SQLDate2(baseDay,SQLDate2.Element.dd)).getAsTimestamp();
			forecastDate = (new SQLDate2(forecastDay,SQLDate2.Element.dd)).getAsTimestamp();
			
		    line = br.readLine();// skip the first line.
		    int flight=0;
			while((line = br.readLine()) != null){
				// Skip comments.
				if(line.charAt(0) != '#'){
					
				   	// Extract all elements from the line. Don't remove empty 
					// strings because often fields are not populated.
					ss = line.split(comma);
						
					// Pull out the ones I want. Only take flights
					// that have flown, which at least is indicated by
					// the itinerary number being present. and have
					// filed and sim gate out times.
					int fId = Integer.valueOf(ss[2].trim());
					if(!(ss[0].equals("") || fId < 0 
							|| ss[7].equals("") || ss[8].equals(""))){
						
						data[flight][0] = baseDate;
						data[flight][1] = forecastDate;
						data[flight][2] = itinNum  = new Integer(ss[0]);
						if(ss[1].equals(""))legNum = null;
						else legNum   = new Integer(ss[1]);
						data[flight][3] = legNum;
						if(ss[2].equals(""))schedId = null;
						else schedId  = new Integer(ss[2]);
						data[flight][4] = schedId;
						data[flight][5] = carrier = ss[5];
						data[flight][6] = etmsAcType = ss[6];
						data[flight][7] = depAirport =ss[3];
						data[flight][8] = arrAirport =ss[4];
						// Get some times. 
						int timeInSecs = 0;
						SQLDate2 date = null;
						for(int i=0;i<5;i++){
							timeInSecs = Integer.valueOf(ss[7+i].trim());
							date = new SQLDate2(baseDate);
							date.addTime(SQLDate2.Element.ss, timeInSecs);
							data[flight][9+i] = date.getAsTimestamp();
						}
						data[flight][19] = airborneTime = new Float(ss[12])/60.f;
						for(int i=0;i<5;i++){
							timeInSecs = Integer.valueOf(ss[13+i].trim());
							date = new SQLDate2(baseDate);
							date.addTime(SQLDate2.Element.ss, timeInSecs);
							data[flight][14+i] = date.getAsTimestamp();
						}
						
						data[flight][20] = turnTime = new Float(ss[18])/60.f;
						data[flight][21] = turnaroundDelay = new Float(ss[19])/60.f;
						data[flight][22] = pushbackDelay = new Float(ss[20])/60.f;
						data[flight][23] = depDelay= new Float(ss[21])/60.f;
						data[flight][24] = airDelay = new Float(0.f);
						data[flight][25] = arrQueueDelay = new Float(ss[22])/60.f;
						data[flight][26] = taxiInDelay  = new Float(ss[23])/60.f;
 
						data[flight][27] = depFix = null;
						data[flight][28] = depFixDelay = new Float(0.f);
						data[flight][29] = arrFix = null;
						data[flight][30] = arrFixDelay = new Float(0.f);
						
						flight++;
					}
				}
			}	
	    	br.close();
	    	
			// Write to table.  Commit all changes as they happen.
	    	if(succCT){
		    	if(!JDBCManager.populateTable(conn,commitFlag,
		    			flightOutputTable,insertRowStr,data,colType)){
		    		success = false;
		    	}
	    	}
	    	
		}catch(SQLException e){
			e.printStackTrace();
			success = false;
		} catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			success = false;
		} catch(IOException ioe){
			ioe.printStackTrace();
			success = false;
		} finally{ 
			   JDBCManager.close(conn);    	
		}	
		return success;
	}
*/
}
