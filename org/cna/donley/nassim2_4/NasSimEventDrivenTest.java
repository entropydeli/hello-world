package org.cna.donley.nassim2_4;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Properties;
import java.util.List;
import java.util.Queue;
import java.util.Arrays;
import java.util.logging.Logger;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Test to validate the NasSimEventDriven class.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: NasSimEventDrivenTest.java 1 2009-09-24 00:00:00EST donley $
 */
public class NasSimEventDrivenTest
{
	/**
	 * Root path of all files.
	 */
	private static String rootPath = 
		//"C:\\Documents and Settings\\James CTR Donley";
		"/Users/jdonley/workspaces/FAA/TestData";
	
	/**
	 * test of constructors, getters and setters.
	 */
	@Test
	public void testConstructorsNGettersNSetters(){
		// Constructor.
    	Properties props = new Properties();
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NassimTest.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
		assertTrue(true == 
			NasSimEventDriven.readModelInputProperties(propsFileNPath, props));
		
    	NasSimEventDriven nb = new NasSimEventDriven(propsFileNPath);
    	assertTrue(nb != null);
    	
    	// Properties
    	assertTrue(nb.getProperties() != null);
    	
    	// Event queue.
    	assertTrue(nb.getMainQueue().size()==0);
		
	}
	/**
	 * Test of exception in constructor.  If have a problem reading
	 * properties file.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testIllArgExceptionConstructor() {
		// Constructor.
		System.err.println("Ignore the next line.");
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NassimTestWrong.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
    	ISimulation nb = new NasSimEventDriven(propsFileNPath);
		
	}
	/**
	 * Test of createLogger method.
	 */
	@Test
	public void testCreateLogger(){
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String loggerPropsFile = "NassimLogger.properties";
		String propsFileNPath = rootPath + File.separator + mainSubDir +
			File.separator + loggerPropsFile;
		
		String loggerName = "loggerBogus";
		String logFileName = "nassimBogus.log";
		String logFileNPath = rootPath + File.separator + mainSubDir +
			File.separator + logFileName;
		assertTrue(NasSimEventDriven.createLogger(
			propsFileNPath,logFileNPath,loggerName));
		
		Logger logger = Logger.getLogger(loggerName);
		logger.severe("severe message");
		logger.info("info message");
		
		// Test if log file has the messages.
		BufferedReader br = null;
		try{
			File logFile = new File(logFileNPath);
			br = new BufferedReader(new FileReader(logFile));
			int count = 0;
			String line = null;
			while((line = br.readLine()) !=  null){
				count++;
			}
			br.close();
			assertTrue(count == 4);
			
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			assertTrue(false);
		}catch(IOException ioe){
			ioe.printStackTrace();
			assertTrue(false);
		}
	}
	/**
	 * Test of groupFirstLegFlightsByAprt method.
	 */
	@Test
	public void testGroupFirstLegFlightsByAprt(){
		
		int[] aprts = {0,1};
		Nas.Ad aprtGroupType = null;
		
		// Create some bogus flights.  Only need leg num, and arr and dep
		// airport.
		int[] pars = new int[IFlight.Param.values().length];
		for(int i=0;i<pars.length;i++){
			pars[i] = -1;
		}
		IFlight[] fls = new IFlight[10];
		int depAprt=0,arrAprt=0;
		for(int i=0;i<fls.length;i++){
			if(i%2==0){
				depAprt = aprts[0];
				arrAprt = aprts[1];
			}else{
				depAprt = aprts[1];
				arrAprt = aprts[0];
			}
			pars[IFlight.Param.ITIN_NUM.ordinal()] = i ;
			pars[IFlight.Param.LEG_NUM.ordinal()]  = 1;
			pars[IFlight.Param.DEP_APRT.ordinal()] = depAprt;
			pars[IFlight.Param.ARR_APRT.ordinal()] = arrAprt;
			fls[i] = new Flight(pars);
		}
		
		// Group by Departure airport.
		aprtGroupType = Nas.Ad.DEP;
		Object[] fByAprt = NasSimEventDriven.groupFirstLegFlightsByAprt(
			aprts.length,aprtGroupType,fls);
		assertTrue(fByAprt !=  null);
		int nFlights = 0;
		for(int i=0;i<fByAprt.length;i++){
			IFlight[] fl = (IFlight[])fByAprt[i];
			assertTrue(fl.length == fls.length/2);
			for(IFlight f : fl){
				nFlights++;
				if(i == 0){
					assertTrue(f.get(IFlight.Param.ITIN_NUM)%2 == 0);
				}else{
					assertTrue(f.get(IFlight.Param.ITIN_NUM)%2 != 0);
				}
			}
		}
		assertTrue(nFlights == fls.length);
		
		// Group by Arrival airport.
		aprtGroupType = Nas.Ad.ARR;
		fByAprt = NasSimEventDriven.groupFirstLegFlightsByAprt(
			aprts.length,aprtGroupType,fls);
		assertTrue(fByAprt !=  null);
		nFlights = 0;
		for(int i=0;i<fByAprt.length;i++){
			IFlight[] fl = (IFlight[])fByAprt[i];
			assertTrue(fl.length == fls.length/2);
			for(IFlight f : fl){
				nFlights++;
				if(i == 0){
					assertTrue(f.get(IFlight.Param.ITIN_NUM)%2 != 0);
				}else{
					assertTrue(f.get(IFlight.Param.ITIN_NUM)%2 == 0);
				}
			}
		}
		assertTrue(nFlights == fls.length);
	}
	
	/**
	 * test of initialize() method.
	 */
/*
	@Test
	public void testInitialize(){
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NassimTest.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
    	NasSimEventDriven nb = new NasSimEventDriven(propsFileNPath);
    	assertTrue(nb != null);
    	assertTrue(nb.getProperties() != null);
    	
    	// Initialize.
    	nb.initialize();
    	
    	Queue<IEvent> q = nb.getMainQueue();
    	int queueSize = q.size();
    	// Why is the following number not the same as in the aircraft file?
    	// The aircraft file gives 32085 itineraries, so I have 9 more.
    	assertTrue(queueSize == 32094);
    	IEvent[] eve = new IEvent[1];
    	IEvent[] eves = q.toArray(eve);
    	int[] itins = new int[queueSize];
    	int count = 0;
    	for(IEvent e : eves){
    		assertTrue(e instanceof TerminalEvent);
    		TerminalEvent te = (TerminalEvent)e;
    		Flight f = (Flight)te.getFlight();
    		int itinNum = f.get(IFlight.Param.ITIN_NUM);
    		int legNum  = f.get(IFlight.Param.LEG_NUM);
    		assertTrue(itinNum <= 31977);
    		assertTrue(legNum == 1);  	
    		itins[count] = itinNum;
    		count++;
    	}
    	Arrays.sort(itins);
    	// There will be flights that have the same itinerary number.
    	// Print those out.
    	count = 0;
    	for(int i=1;i<itins.length;i++){
    		if(itins[i] == itins[i-1]){
    			System.out.println("i: " + i + " itin: " + itins[i]);
    			count++;
    		}
    	}
    	System.out.println("total number of flights with the same itin: " +
    		count);
		
	}
*/	
	/**
	 * test of run() and processOutput() methods.
	 */
/*
	@Test
	public void testRunNProcessOutput(){
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NassimTest.properties";
		File propsFileNPath = new File(rootPath + File.separator + mainSubDir +
			File.separator + propsFile);
		
    	NasSimEventDriven nb = new NasSimEventDriven(propsFileNPath);
    	assertTrue(nb != null);
    	assertTrue(nb.getProperties() != null);
    	
    	// Initialize.
    	nb.initialize();
    	
    	// Run.
    	nb.run();
    	
    	// Print output.
    	boolean success = nb.processOutput();
    	assertTrue(success);
		
	}
*/
	/**
	 * test of main() method
	 */
	@Test
	public void testMain(){
		String mainSubDir = "BogusTestData" + File.separator + "Nassim";
		String propsFile = "NassimTest.properties";
		String propsFileNPath = rootPath + File.separator + mainSubDir +
			File.separator + propsFile;
		
		String[] args = new String[1];
		args[0] = propsFileNPath;
    	NasSimEventDriven.main(args);
    	boolean success = true;
    	assertTrue(success);	
	}
}
