package org.cna.donley.nassim2_4;

import org.cna.donley.jdbc.JDBCManager;
import org.cna.donley.jdbc.SQLDate2;
import org.cna.donley.utils.Constants;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Test to validate the NasSimOutputAnalyzer class.  Uses JUnit 4.
 * The tests should be run in consecutive order as some results, namely the
 * counting of the table rows as a check will yield a failed test.  However,
 * if you don't care, then whatever.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: NasSimOutputAnalyzerTest.java 1 2009-09-28 00:00:00EST donley $
 */
public final class NasSimOutputAnalyzerTest
{
	
	/**
	 * Small number.
	 */
	private double EPS = 1.e-10;
		
	/** test readModelInputProperties */
    @Test
	public void testReadModelInputProperties()
    {	
    	boolean success = true;
    	Properties props = new Properties();

    	String path = "..\\..\\BogusTestData\\Nassim";
    	String fileName = "NasSimOutputAnalyzerTest.properties";
    	File propsFileNPath = new File(path + File.separator + fileName);
    	success = 
    		NasSimOutputAnalyzer.readModelInputProperties(propsFileNPath, props);
    	assertTrue(success);
    }
    
    /**
     * Test createFlightOutputTable.
     */
    @Test
    public void testCreateFlightOutputTable(){
    	
    	Properties props = new Properties();

    	String path = "..\\..\\BogusTestData\\Nassim";
    	String fileName = "NasSimOutputAnalyzerTest.properties";
    	File propsFileNPath = new File(path + File.separator + fileName);
    	boolean success = 
    		NasSimOutputAnalyzer.readModelInputProperties(propsFileNPath, props);
    	assertTrue(success);
    	
    	// Load the database (oracle jdbc) driver.
        assertTrue(JDBCManager.loadClass(props.getProperty("dbDriverClass")));
    	
    	// Dates wanted.  Year, months, and day.  Day is either an integer of
    	// "all".
        String baseDay = props.getProperty("baseDay");
        String forecastDay = props.getProperty("forecastDay");
    	
    	// Where the data is and where it is going.
		// Main path.
		String baseDir = props.getProperty("baseDir");
		String mainSubDir  = props.getProperty("mainSubDir");
		String mainPath = baseDir + File.separator + mainSubDir;
		
		// Get temp table names.
		boolean createTables = 
			Boolean.parseBoolean(props.getProperty("createTables"));
		String flightOutputTable = props.getProperty("flightOutputTable");
		
		// FlightOutput files.
		String flightSubDir   = props.getProperty("flightOutputSubDir");
		String flightFilePrefix = props.getProperty("flightOutputFilePrefix");
		String flightFileSuffix = props.getProperty("flightOutputFileSuffix");
		File flightFileNPath= null;
		
    	flightFileNPath = new File(mainPath + File.separator + flightSubDir +
    		File.separator + flightFilePrefix + baseDay + "_" + forecastDay + flightFileSuffix);
    	
    	// Create table.
	    props.setProperty("createTables","true");
	    createTables = Boolean.parseBoolean(props.getProperty("createTables"));
    	success = NasSimOutputAnalyzer.createFlightOutputTable(props, 
				createTables, baseDay, forecastDay, flightFileNPath,flightOutputTable);
		assertTrue(success);
		
		// Correct number of rows?
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        try{
        	String selectStr = "select count(*) as num_rows from " + 
        		flightOutputTable;
        	conn = JDBCManager.openConnection(props, true);
        	stmt = conn.createStatement();
        	rset = stmt.executeQuery(selectStr);
        	rset.next();
        	int numRows = rset.getInt("num_rows");
        	assertTrue(numRows == 48671);
        	
        	// Check a whole row.
        	selectStr = "select * from " + flightOutputTable + 
        	  " where schedule_id=37863";
        	rset = stmt.executeQuery(selectStr);
        	rset.next();
        	assertTrue("20080820 00:00:00".equals(
        			(new SQLDate2(rset.getTimestamp("base_day"))).toString()));
        	assertTrue("20080820 00:00:00".equals(
        			(new SQLDate2(rset.getTimestamp("forecast_day"))).toString()));
        	assertTrue(12==rset.getInt("itinerary_number"));
        	assertTrue(1==rset.getInt("leg_num"));
        	assertTrue("COA".equals(rset.getString("carrier")));
        	assertTrue("B735".equals(rset.getString("etms_ac_type")));
        	assertTrue("SAT".equals(rset.getString("dep_aprt")));
        	assertTrue("IAH".equals(rset.getString("arr_aprt")));
        	assertTrue("20080820 16:58:00".equals(
        			(new SQLDate2(rset.getTimestamp("sched_out_time"))).toString()));
        	assertTrue("20080820 16:52:50".equals(
        			(new SQLDate2(rset.getTimestamp("sim_out_time"))).toString()));
        	assertTrue("20080820 17:02:18".equals(
        			(new SQLDate2(rset.getTimestamp("calc_off_time"))).toString()));
        	assertTrue("20080820 17:02:18".equals(
        			(new SQLDate2(rset.getTimestamp("sim_off_time"))).toString()));
        	assertTrue("20080820 17:33:18".equals(
        			(new SQLDate2(rset.getTimestamp("calc_on_time"))).toString()));
        	assertTrue("20080820 17:34:07".equals(
        			(new SQLDate2(rset.getTimestamp("sim_on_time"))).toString()));
        	assertTrue("20080820 17:59:00".equals(
        			(new SQLDate2(rset.getTimestamp("sched_in_time"))).toString()));
        	assertTrue("20080820 17:37:57".equals(
        			(new SQLDate2(rset.getTimestamp("calc_in_time"))).toString()));
        	assertTrue("20080820 17:37:57".equals(
        			(new SQLDate2(rset.getTimestamp("sim_in_time"))).toString()));
        	assertEquals(31.0,rset.getFloat("sim_airborne_time"),1.e-4);
        	assertEquals(31.533333,rset.getFloat("turn_time"),1.e-4);
        	assertEquals(-5.166666,rset.getFloat("pushback_time"),1.e-4);
        	assertEquals(9.466666,rset.getFloat("taxi_out_time"),1.e-4);
        	assertEquals(3.833333,rset.getFloat("taxi_in_time"),1.e-4);
        	assertEquals(-5.166666,rset.getFloat("gate_out_delay"),1.e-4);
        	assertEquals(0.0,rset.getFloat("dep_delay"),1.e-4);
        	assertEquals(0.0,rset.getFloat("airborne_delay"),1.e-4);
        	assertEquals(0.8166666,rset.getFloat("arr_delay"),1.e-4);
        	assertEquals(0.0,rset.getFloat("taxi_in_delay"),1.e-4);
        	assertTrue("????".equals(rset.getString("dep_fix")));
        	assertTrue(0.0 == rset.getFloat("dep_fix_delay"));
        	assertTrue("????".equals(rset.getString("arr_fix")));
        	assertTrue(0.0 == rset.getFloat("arr_fix_delay"));
        	
        }catch(SQLException e){
        	e.printStackTrace();
        	assertTrue(false);
        }finally{
        	JDBCManager.close(rset);
        	JDBCManager.close(stmt);
        	JDBCManager.close(conn);
        }       
		
    }   
    /**
     * Test createScheduleTable.
     */
    @Test
    public void testCreateScheduleTable(){
    	
     	Properties props = new Properties();

    	String path = "..\\..\\BogusTestData\\Nassim";
    	String fileName = "NasSimOutputAnalyzerTest.properties";
    	File propsFileNPath = new File(path + File.separator + fileName);
    	boolean success = 
    		NasSimOutputAnalyzer.readModelInputProperties(propsFileNPath, props);
    	assertTrue(success);
    	
    	// Load the database (oracle jdbc) driver.
        assertTrue(JDBCManager.loadClass(props.getProperty("dbDriverClass")));
    	
    	// Dates wanted.  Year, months, and day.  Day is either an integer of
    	// "all".
        String baseDay = props.getProperty("baseDay");
        String forecastDay = props.getProperty("forecastDay");
    	
    	// Where the data is and where it is going.
		// Main path.
		String baseDir = props.getProperty("baseDir");
		String mainSubDir  = props.getProperty("mainSubDir");
		String mainPath = baseDir + File.separator + mainSubDir;
		
		// Get temp table names.
		String schedTable = props.getProperty("scheduleInputTable");
		
		// FlightOutput files.
		String schedSubDir   = props.getProperty("schedSubDir");
		String schedFilePrefix = props.getProperty("schedFilePrefix");
		String schedFileSuffix = props.getProperty("schedFileSuffix");
		File schedFileNPath= null;
		
    	schedFileNPath = new File(mainPath + File.separator + schedSubDir +
    		File.separator + schedFilePrefix + forecastDay + "_" +
    		baseDay + schedFileSuffix);
    	
    	props.setProperty("createTables","true");
	    boolean createTables = 
	    	Boolean.parseBoolean(props.getProperty("createTables"));
		success = NasSimOutputAnalyzer.createScheduleTable(props, 
				createTables,baseDay,forecastDay,schedFileNPath,schedTable);
		assertTrue(success);
		
		// Correct number of rows?
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        try{
        	String selectStr = "select count(*) as num_rows from " + 
        		schedTable;
        	conn = JDBCManager.openConnection(props, true);
        	stmt = conn.createStatement();
        	rset = stmt.executeQuery(selectStr);
        	rset.next();
        	int numRows = rset.getInt("num_rows");
        	assertTrue(numRows == 50693);
        	
        	// Check a whole row.
        	selectStr = "select * from " + schedTable + 
        	  " where id_num=2";
        	rset = stmt.executeQuery(selectStr);
        	rset.next();
        	assertTrue("20080820 00:00:00".equals(
        			(new SQLDate2(rset.getTimestamp("base_day"))).toString()));
        	assertTrue("20080820 00:00:00".equals(
        			(new SQLDate2(rset.getTimestamp("forecast_day"))).toString()));
        	assertTrue("20080819 00:00:00".equals(
        			(new SQLDate2(rset.getTimestamp("act_date"))).toString()));
        	assertTrue("DEUCE51".equals(rset.getString("acid")));
        	assertTrue(43456 == rset.getInt("flight_index"));
        	assertTrue("20080821 00:48:36".equals(
        			(new SQLDate2(rset.getTimestamp("out_time"))).toString()));
        	assertTrue("20080821 01:00:00".equals(
        			(new SQLDate2(rset.getTimestamp("off_time"))).toString()));
        	assertTrue("20080820 23:32:00".equals(
        			(new SQLDate2(rset.getTimestamp("on_time"))).toString()));
        	assertTrue("20080820 23:37:06".equals(
        			(new SQLDate2(rset.getTimestamp("in_time"))).toString()));
        	assertTrue(20.0f == rset.getFloat("filed_altitude"));
        	assertTrue(219.0f == rset.getFloat("filed_airspeed"));
        	assertTrue("POB".equals(rset.getString("etms_departure_airport")));
        	assertTrue("POB".equals(rset.getString("etms_arrival_airport")));
        	assertTrue(1 == rset.getInt("dept_cntry_code"));
        	assertTrue(1 == rset.getInt("arr_cntry_code"));
        	assertTrue("C130".equals(rset.getString("etms_aircraft_type")));
        	assertTrue("T".equals(rset.getString("physical_class")));
        	assertTrue("M".equals(rset.getString("etms_user_class")));
        	assertTrue(2 == rset.getInt("flew_flag"));
        	assertTrue(-1 == rset.getInt("airspace_code"));
        	assertTrue("KPOB".equals(rset.getString("dept_icao_code")));
        	assertTrue("KPOB".equals(rset.getString("arr_icao_code")));
        	assertTrue("D Military".equals(rset.getString("atop_user_class")));
        	assertTrue("C130".equals(rset.getString("bada_type")));
        	assertTrue("ATOP".equals(rset.getString("bada_type_source")));
        	assertTrue("N".equals(rset.getString("sched_flag")));
        	
        }catch(SQLException e){
        	e.printStackTrace();
        	assertTrue(false);
        }finally{
        	JDBCManager.close(rset);
        	JDBCManager.close(stmt);
        	JDBCManager.close(conn);
        }       		
    }
    /**
     * Test createScheNFlightTable.
     */
    @Test
    public void testCreateSchedNFlightTable(){
    	
    	Properties props = new Properties();

    	String path = "..\\..\\BogusTestData\\Nassim";
    	String fileName = "NasSimOutputAnalyzerTest.properties";
    	File propsFileNPath = new File(path + File.separator + fileName);
    	boolean success = 
    		NasSimOutputAnalyzer.readModelInputProperties(propsFileNPath, props);
    	assertTrue(success);
    	
    	// Load the database (oracle jdbc) driver.
        assertTrue(JDBCManager.loadClass(props.getProperty("dbDriverClass")));
    	
		// Get temp table names.
        String flightOutputTable = props.getProperty("flightOutputTable");
		String schedTable = props.getProperty("scheduleInputTable");
		String schedNFlightTable = props.getProperty("schedNFlightOutputTable");
    	
		success = NasSimOutputAnalyzer.combineSchedNFlightTables(
				props, schedTable, flightOutputTable, schedNFlightTable);
		assertTrue(success);
		
		// Correct number of rows?
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        try{
        	String selectStr = "select count(*) as num_rows from " + 
        		schedNFlightTable;
        	conn = JDBCManager.openConnection(props, true);
        	stmt = conn.createStatement();
        	rset = stmt.executeQuery(selectStr);
        	rset.next();
        	int numRows = rset.getInt("num_rows");
        	assertTrue(numRows == 48557);
        }catch(SQLException e){
        	e.printStackTrace();
        	assertTrue(false);
        }finally{
        	JDBCManager.close(rset);
        	JDBCManager.close(stmt);
        	JDBCManager.close(conn);
        }       		
    }

    /**
     * Test combineActualNSimulationTables
     */
    @Test
    public void testCombineActualNSimulationTables(){
    	
    	Properties props = new Properties();

    	String path = "..\\..\\BogusTestData\\Nassim";
    	String fileName = "NasSimOutputAnalyzerTest.properties";
    	File propsFileNPath = new File(path + File.separator + fileName);
    	boolean success = 
    		NasSimOutputAnalyzer.readModelInputProperties(propsFileNPath, props);
    	assertTrue(success);
    	
    	// Load the database (oracle jdbc) driver.
        assertTrue(JDBCManager.loadClass(props.getProperty("dbDriverClass")));
    	
		// Get table names.
		String schedNFlightTable = props.getProperty("schedNFlightOutputTable");
    	String aspmFlightDataTable = props.getProperty("aspmFlightDataTable");
		String actNSimTable = props.getProperty("actNSimTable");
		
		success = 
			NasSimOutputAnalyzer.combineActualNSimulationTables(props, 
					aspmFlightDataTable, schedNFlightTable, actNSimTable);
		assertTrue(success);
		
		// Correct number of rows?
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        try{
        	String selectStr = "select count(*) as num_rows from " + 
    		  schedNFlightTable + "New";
	    	conn = JDBCManager.openConnection(props, true);
	    	stmt = conn.createStatement();
	    	rset = stmt.executeQuery(selectStr);
	    	rset.next();
	    	int numRows = rset.getInt("num_rows");
	    	assertTrue(numRows == 37587);
	    	
        	selectStr = "select count(*) as num_rows from " + 
        		actNSimTable;
        	conn = JDBCManager.openConnection(props, true);
        	stmt = conn.createStatement();
        	rset = stmt.executeQuery(selectStr);
        	rset.next();
        	numRows = rset.getInt("num_rows");
        	assertTrue(numRows == 23215);
        }catch(SQLException e){
        	e.printStackTrace();
        	assertTrue(false);
        }finally{
        	JDBCManager.close(rset);
        	JDBCManager.close(stmt);
        	JDBCManager.close(conn);
        }       		
    } 

    /**
     * Test createBogusActualNSimulationTables
     */
/*
    @Test
    public void testCreateBogusActualNSimulationTables(){
    	
    	Properties props = new Properties();

    	String path = "..\\..\\BogusTestData\\Nassim";
    	String fileName = "NasSimOutputAnalyzerBogus.properties";
    	String propsFileNPath = path + File.separator + fileName;
    	boolean success = 
    		NasSimOutputAnalyzer.readModelInputProperties(propsFileNPath, props);
    	assertTrue(success);
    	
    	// Load the database (oracle jdbc) driver.
        assertTrue(JDBCManager.loadClass(props.getProperty("dbDriverClass")));
    	
		// Get table names.
		String schedNFlightTable = props.getProperty("schedNFlightOutputTable");
		String actNSimTable = props.getProperty("actNSimTable");
		
		success = 
			NasSimOutputAnalyzer.createBogusActualNSimulationTables(props, 
					schedNFlightTable, actNSimTable);
		assertTrue(success);
		
		// Correct number of rows?
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        try{
        	String selectStr = "select count(*) as num_rows from " + 
    		  schedNFlightTable + "New";
	    	conn = JDBCManager.openConnection(props, true);
	    	stmt = conn.createStatement();
	    	rset = stmt.executeQuery(selectStr);
	    	rset.next();
	    	int numRows = rset.getInt("num_rows");
	    	assertTrue(numRows == 38126);
	    	
        	selectStr = "select count(*) as num_rows from " + 
        		actNSimTable;
        	conn = JDBCManager.openConnection(props, true);
        	stmt = conn.createStatement();
        	rset = stmt.executeQuery(selectStr);
        	rset.next();
        	numRows = rset.getInt("num_rows");
        	assertTrue(numRows == 38126);
        }catch(SQLException e){
        	e.printStackTrace();
        	assertTrue(false);
        }finally{
        	JDBCManager.close(rset);
        	JDBCManager.close(stmt);
        	JDBCManager.close(conn);
        }       		
    }
 */
    /**
     * Test of computeDelaysSystemwide.
     */
 /*
    @Test
    public void testComputeDelaysSystemwide(){
    	Properties props = new Properties();
    	String path = "..\\..\\BogusTestData\\Nassim";
    	String fileName = "NasSimOutputAnalyzerBogus.properties";
    	File propsFileNPath = new File(path + File.separator + fileName);
    	boolean success = 
    		NasSimOutputAnalyzer.readModelInputProperties(propsFileNPath, props);
    	assertTrue(success);
    	
    	// Load the database (oracle jdbc) driver.
        assertTrue(JDBCManager.loadClass(props.getProperty("dbDriverClass")));
    	
        String baseDay = props.getProperty("baseDay");
        String forecastDay = props.getProperty("forecastDay");
        String actNSimTable = props.getProperty("actNSimTable");
        String delayTable = props.getProperty("delayTable");
        boolean createTable = true;
        success = NasSimOutputAnalyzer.computeDelaysSystemwide(props, 
        		createTable, baseDay, forecastDay, actNSimTable, delayTable);
        assertTrue(success);
        // TRY IT AGAIN.  Should just create double the entries.
        createTable = false;
        success = NasSimOutputAnalyzer.computeDelaysSystemwide(props, 
        		createTable, baseDay, forecastDay, actNSimTable, delayTable);
        assertTrue(success);
        
        // Correct number of rows?
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        try{
        	String selectStr = "select count(*) as num_rows from " + 
    		  delayTable;
	    	conn = JDBCManager.openConnection(props, true);
	    	stmt = conn.createStatement();
	    	rset = stmt.executeQuery(selectStr);
	    	rset.next();
	    	int numRows = rset.getInt("num_rows");
	    	assertTrue(numRows == 2);
	    	
        }catch(SQLException e){
        	e.printStackTrace();
        	assertTrue(false);
        }finally{
        	JDBCManager.close(rset);
        	JDBCManager.close(stmt);
        	JDBCManager.close(conn);
        }       		
    }
*/
    /**
     * Test of computeDelaysByAirport.
     */
 /*
    @Test
    public void testComputeDelaysByAirport(){
    	Properties props = new Properties();
    	String path = "..\\..\\BogusTestData\\Nassim";
    	String fileName = "NasSimOutputAnalyzerBogus.properties";
    	File propsFileNPath = new File(path + File.separator + fileName);
    	boolean success = 
    		NasSimOutputAnalyzer.readModelInputProperties(propsFileNPath, props);
    	assertTrue(success);
    	
    	// Load the database (oracle jdbc) driver.
        assertTrue(JDBCManager.loadClass(props.getProperty("dbDriverClass")));
    	
        String baseDay = props.getProperty("baseDay");
        String forecastDay = props.getProperty("forecastDay");
        String actNSimTable = props.getProperty("actNSimTable");
        String delayAprtTable = props.getProperty("delayAprtTable");
        String airportGroupsTable = props.getProperty("airportGroupsTable");
        String airportGroup = props.getProperty("airportGroup");
        
        boolean createTable = true;
        success = NasSimOutputAnalyzer.computeDelaysByAirport(props, 
        		createTable, baseDay, forecastDay, airportGroupsTable, 
        		airportGroup, actNSimTable, delayAprtTable);
        assertTrue(success);
        // try it again. Should just duplicate the entries.
        createTable = false;
        success = NasSimOutputAnalyzer.computeDelaysByAirport(props,
        		createTable, baseDay, forecastDay, airportGroupsTable, 
        		airportGroup, actNSimTable, delayAprtTable);
        assertTrue(success);
        
     // Correct number of rows?
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        try{
        	String selectStr = "select count(*) as num_rows from " + 
    		  delayAprtTable;
	    	conn = JDBCManager.openConnection(props, true);
	    	stmt = conn.createStatement();
	    	rset = stmt.executeQuery(selectStr);
	    	rset.next();
	    	int numRows = rset.getInt("num_rows");
	    	assertTrue(numRows == 70);
	    	
        }catch(SQLException e){
        	e.printStackTrace();
        	assertTrue(false);
        }finally{
        	JDBCManager.close(rset);
        	JDBCManager.close(stmt);
        	JDBCManager.close(conn);
        }       		
    } 
 */  
    /**
     * Test of analyzeSimWithActualBaseDays().
     */
 /*
    @Test
    public void testAnalyzeSimWithActualBaseDays(){
    	Properties props = new Properties();

    	String path = "..\\..\\BogusTestData\\Nassim";
    	String fileName = "NasSimOutputAnalyzerTest.properties";
    	File propsFileNPath = new File(path + File.separator + fileName);
    	boolean success = 
    		NasSimOutputAnalyzer.readModelInputProperties(propsFileNPath, props);
    	assertTrue(success);
    	
    	// Load the database (oracle jdbc) driver.
        assertTrue(JDBCManager.loadClass(props.getProperty("dbDriverClass")));
 
        String[] baseDays = {"20080820","20080821"};
        // 200807 days
//        String[] baseDays = {"20080701","20080702","20080704","20080705","20080706",
//	        "20080707","20080710","20080711","20080712","20080714","20080715",
//	        "20080717","20080718","20080719","20080720",
//	    	"20080721","20080724","20080725","20080726","20080727","20080728",
//	    	"20080729","20080730"};
        
        // 200808 days
      //  String[] baseDays = {
      //	"20080801","20080802","20080803","20080804","20080805","20080806",
      //	"20080809","20080812","20080813","20080814","20080815","20080816",
      //	"20080818","20080819","20080820","20080821","20080822","20080823",
      //	"20080824","20080825","20080827","20080830","20080831"};

        boolean createTables = true;
	    success = NasSimOutputAnalyzer.analyzeSimWithActualBaseDays(props,
	    		createTables, baseDays);
	    assertTrue(success);
    }
*/
    /**
     * Test of analyzeSimDays().
     */
 /*
    @Test
    public void testAnalyzeSimDays(){
    	Properties props = new Properties();

    	String path = "..\\..\\BogusTestData\\Nassim";
    	String fileName = "NasSimOutputAnalyzerTest.properties";
    	File propsFileNPath = new File(path + File.separator + fileName);
    	boolean success = 
    		NasSimOutputAnalyzer.readModelInputProperties(propsFileNPath, props);
    	assertTrue(success);
    	
    	// Load the database (oracle jdbc) driver.
        assertTrue(JDBCManager.loadClass(props.getProperty("dbDriverClass")));
 
        String[] baseDays = {"20080820"};
        
        String[][] baseNForecastDays = new String[baseDays.length][2];
        boolean createTables;
        
        // Populate tables with Base day data.
        for(int i=0;i<baseNForecastDays.length;i++){
        	baseNForecastDays[i][0] = baseDays[i];
        	baseNForecastDays[i][1] = baseDays[i];
        }
        createTables = true;
	    success = NasSimOutputAnalyzer.analyzeSimDays(props,
	    		createTables, baseNForecastDays);
	    assertTrue(success);
    }  
*/
    /**
     * Test of analyzeSimDays(), but really it is used to check stuff.
     */
 /*
    @Test
    public void testAnalyzeSimDays(){
    	Properties props = new Properties();

    	String path = "..\\..\\BogusTestData\\Nassim";
    	String fileName = "NasSimOutputAnalyzerTurnTime.properties";
 //   	String path = "..\\..\\proj\\NASPAC";
 //   	String fileName = "NasSimOutputAnalyzer.properties";
    	File propsFileNPath = new File(path + File.separator + fileName);
    	boolean success = 
    		NasSimOutputAnalyzer.readModelInputProperties(propsFileNPath, props);
    	assertTrue(success);
    	
    	// Load the database (oracle jdbc) driver.
        assertTrue(JDBCManager.loadClass(props.getProperty("dbDriverClass")));
 
        String[] baseDays = {"20080801"};
        
        String[][] baseNForecastDays = new String[baseDays.length][2];
        boolean createTables;
        
        // Populate tables with Base day data.
        for(int i=0;i<baseNForecastDays.length;i++){
        	baseNForecastDays[i][0] = baseDays[i];
        	baseNForecastDays[i][1] = baseDays[i];
        }
        createTables = true;
	    success = NasSimOutputAnalyzer.analyzeSimDays(props,
	    		createTables, baseNForecastDays);
	    assertTrue(success);
    } 
 */ 
    /**
     * Test of analyzeSimWithActualBaseDays(), but really it is a production run.
     */
 /*
    @Test
    public void testAnalyzeSimWithActualBaseDaysProd2008XX(){
    	Properties props = new Properties();
    	
    	// Root path of all files.
        String mainPath = 
        	"C:\\Documents and Settings\\James CTR Donley\\proj\\Nassim";
        
        // Get base days.
        int[] daysSkip = {33}; // need to include at least one day.
        int year = 2008;
        int month = 10;
        String[][] baseNForecastDays = computeBaseNForecastDays(
            	year,year,month,daysSkip);
        int len = baseNForecastDays.length;
     //   int len = 5;
        String[] baseDays = new String[len];
        for(int i=0;i<len;i++){
        	baseDays[i] = baseNForecastDays[i][0];
        }
        boolean createTables;
        
        //------------------------------------
        // Do run for AnS2008XX.
        //------------------------------------
    	String fileName = "NasSimOutputAnalyzerAnS200810.properties";
       	File propsFileNPath = new File(mainPath + File.separator + fileName);
       	boolean success = 
       		NasSimOutputAnalyzer.readModelInputProperties(propsFileNPath, props);
       	assertTrue(success);
       	// Load the database (oracle jdbc) driver.
           assertTrue(JDBCManager.loadClass(props.getProperty("dbDriverClass")));
       
        createTables = true;
	    success = NasSimOutputAnalyzer.analyzeSimWithActualBaseDays(
	    		props,createTables, baseDays);
	    assertTrue(success);
    }
 */ 
    /**
     * Test of analyzeSimDays(), but really it is a production 
     * run for 2008/08 and 2009/08.
     */
 /*
    @Test
    public void testAnalyzeSimDaysProd200808(){
    	Properties props = new Properties();
    	String path = "..\\..\\BogusTestData\\Nassim";

  
        String[] baseDays = {"20080801","20080802","20080803","20080804",
        	"20080805","20080806","20080809","20080812","20080813","20080814",
        	"20080815","20080816","20080818","20080819","20080820","20080821",
	    	"20080822","20080823","20080824","20080825","20080826","20080827",
	    	"20080830","20080831"};
        String[] forecastDays = {"20090731","20090801","20090802","20090803",
            "20090804","20090805","20090808","20090811","20090812","20090813",
            "20090814","20090815","20090817","20090818","20090819","20090820",
            "20090821","20090822","20090823","20090824","20090825","20090826",
            "20090829","20090830"};
    

    	//String[] baseDays = {"20080801","20080802"};
    	//String[] forecastDays = {"20090731","20090801"};  
   
    	
        String[][] baseNForecastDays = new String[baseDays.length][2];
        boolean createTables;
        
        //------------------------------------
        // Do run for SnS200807.
        //------------------------------------
    	String fileName = "NasSimOutputAnalyzerSnS200808.properties";
       	String propsFileNPath = path + File.separator + fileName;
       	boolean success = 
       		NasSimOutputAnalyzer.readModelInputProperties(propsFileNPath, props);
       	assertTrue(success);
       	// Load the database (oracle jdbc) driver.
           assertTrue(JDBCManager.loadClass(props.getProperty("dbDriverClass")));
        // Populate tables with Base day data.
        for(int i=0;i<baseNForecastDays.length;i++){
        	baseNForecastDays[i][0] = baseDays[i];
        	baseNForecastDays[i][1] = baseDays[i];
        }
        createTables = true;
	    success = NasSimOutputAnalyzer.analyzeSimDays(props,
	    		createTables, baseNForecastDays);
	    assertTrue(success);
	    
	    //------------------------------
	    // Do run for SnS200907.
	    //------------------------------
    	fileName = "NasSimOutputAnalyzerSnS200908.properties";
       	propsFileNPath = path + File.separator + fileName;
       	success = 
       		NasSimOutputAnalyzer.readModelInputProperties(propsFileNPath, props);
       	assertTrue(success);
       	// Load the database (oracle jdbc) driver.
           assertTrue(JDBCManager.loadClass(props.getProperty("dbDriverClass")));
	   
	    // Populate tables with forecast day data.
        for(int i=0;i<baseNForecastDays.length;i++){
        	baseNForecastDays[i][0] = baseDays[i];
        	baseNForecastDays[i][1] = forecastDays[i];
        }
        createTables = false;
	    success = NasSimOutputAnalyzer.analyzeSimDays(props,
	    		createTables, baseNForecastDays);
	    assertTrue(success);
    }
*/
    
    /**
     * Right now, both the base and forecast days are the same.
     * @param yearBase Base year.
     * @param yearForecast Forecast year.
     * @param month Month
     * @param daysSkip Array of days in month to skip.
     * @return 2D array of base and forecast days.
     */
    public String[][] computeBaseNForecastDays(
    	int yearBase, int yearForecast ,int month, int[] daysSkip){
	   
	    List<String> bDays = new ArrayList<String>();
	    List<String> fDays = new ArrayList<String>();
	    int day = 1;
	    SQLDate2 date = new SQLDate2(yearBase,month,day,0,0,0);
	    while(SQLDate2.checkDayOfMonth(yearBase,month,day)){
	    	boolean skip = false;
	    	for(int j=0;j<daysSkip.length;j++){
	    		if(day == daysSkip[j]){
	    			skip = true;
	    			break;
	    		}
	    	}
	    	if(!skip){
	    		bDays.add(date.toString(SQLDate2.Element.dd));
	    		fDays.add(date.toString(SQLDate2.Element.dd));
	    	}
	    	date.addTime(SQLDate2.Element.dd, 1);
	    	day++;
	    }
	    String[][] baseNForecastDays = new String[bDays.size()][2];
	    for(int i=0;i<bDays.size();i++){
	    	baseNForecastDays[i][0] = bDays.get(i);
	    	baseNForecastDays[i][1] = fDays.get(i);
	    }
	    return baseNForecastDays;
    }

}
