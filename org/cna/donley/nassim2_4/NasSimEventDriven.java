package org.cna.donley.nassim2_4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Properties;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Implements the {@link ISimulation} interface for an event driven simulation
 * of a National Airspace (NAS) network.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: NasSimEventDriven.java 1 2009-08-28 00:00:00EST $
 */

public class NasSimEventDriven implements ISimulation 
{ 	
	/** name of the class */
	private static String className;
	
	/**
	 * Simulation properties object.
	 */
	private Properties props;
	
	/**
	 * Holds the simulation elements.
	 */
	private ISimElements simElements;

	/**
	 * Priority queue.  Orders by event time.
	 */
	private final IQueue<IEvent> eventQueue = new EventQueue();
	
	/**
	 * Simulation time.  Records the time of the latest event.
	 */
	private int time = 0;
	
	/**
	 * Set up loggers.
	 */
	private static String loggerName = "nassimLogger";
	
	/** 
	 * Main method to run class.
	 * @param args  To input object properties.
	 */
	public static void main(String[] args){
		String classN = "org.cna.donley.nassim2.NasSimEventDriven";
		String method = classN + ".main()";
		File propsFileNPath = null;
		if(args.length < 1){
			System.err.println("Usage: " + classN + 
				" <properties file and path> ");
			System.exit(-1);
		}
		else {
			propsFileNPath = new File(args[0]);
			if(!propsFileNPath.exists()){
				System.out.println(method + ": input properties file, " +
					propsFileNPath + ", can't be found.");
				System.exit(-1);
			}
			ISimulation nassim = new NasSimEventDriven(propsFileNPath);
			nassim.startSimulation();
		}
	}
	
	/**
	 * Constructor
	 * @param propsFileNPath Name and path to simulation properties file.
	 */
	public NasSimEventDriven(File propsFileNPath){
		className = this.getClass().getName();
		props = new Properties();
		// Read properties file.
		if(!readModelInputProperties(propsFileNPath,props)){
			final String method = this.getClass().getName();
			throw new IllegalArgumentException(method + ": problem reading " +
				"properties file: " + propsFileNPath);			
		}
		
		// Set up logger.
		String baseDir = props.getProperty("baseDir");
		String subDir = props.getProperty("subDir");
		String loggerPropsFile = 
			props.getProperty("loggingPropsFile");
		String logFileName = props.getProperty("logFileName");
		String loggerPropsFileNPath = baseDir + File.separator +
			subDir + File.separator + loggerPropsFile;
		String logFileNPath = baseDir + File.separator + subDir +
			File.separator + logFileName;
		if(!createLogger(loggerPropsFileNPath,logFileNPath,loggerName)){
			final String method = this.getClass().getName();
			throw new IllegalArgumentException(method + ": creation of " +
				"logger failed.");
		}
	}
	
	/**
	 * Sets up and creates the logger for this class.
	 * @param loggerPropsFile
	 * @param logFileName
	 * @param loggerName  Name of logger. Can be accessed globally using
	 *   this name.
	 * @return <code>true</code> if successful; <code>false</code> if not.
	 */
	public static boolean createLogger(String loggerPropsFile,String logFileName,
		String loggerName){
		// Get a logger; the logger is automatically created if
		// it doesn't already exist.  Note that the name
		// denotes the logger, so it is global in that sense.
		Logger logger = Logger.getLogger(loggerName);
		
		try {
	    	  File loggerPropsFileNPath = new File(loggerPropsFile);
	    	  InputStream is = new FileInputStream(loggerPropsFileNPath);
	    	  LogManager lm = LogManager.getLogManager();
	    	  lm.readConfiguration(is);
	    	  
	    	  // Create an new file handler
			  boolean append = false;
			  FileHandler fileHandler = new FileHandler(logFileName, append);
			  // Add to the desired logger
			  logger.addHandler(fileHandler); 
			  // Writing error logs.
			  // Create a console handler
			  ConsoleHandler errorHandler = new ConsoleHandler();
			  // Add to logger
			  logger.addHandler(errorHandler);
			  
  	    } catch (FileNotFoundException fnfe){
        	  fnfe.printStackTrace();
        	  return false;
        } catch (IOException e) {
        	  e.printStackTrace();
        	  return false;
        }
        return true;	
	}
	
	/**
	 * Starts the simulation.  This includes initializing the simulation,
	 * running it and producing output.
	 */
	public void startSimulation(){
		// Init
		initialize();
		// Run.
		run();
		// Process Output
		processOutput();
	}
	/**
	 * Initializes the simulation.  Reads in data from files, creates
	 * sim elements such as the node network and flight objects. It also
	 * creates the initial events and adds these events to queue.
	 */
	public void initialize(){
		// Read naspac preprocessor and other file stuff and
		// create Nas singleton.  Returns an object that contains
		// all the flights and network nodes.
		String baseDir = props.getProperty("baseDir");
		String subDir = props.getProperty("subDir");
		String bridgePropsFile = props.getProperty("bridgePropsFile");
		String bridgeType = props.getProperty("bridgeType");
		ISimInitializer si = null;
		
		if(bridgeType.toLowerCase().equals("naspac")){
			File naspacPropsFileNPath = new File(baseDir + File.separator +
				subDir + File.separator + bridgePropsFile);
			si = new NaspacBridge(naspacPropsFileNPath);
			simElements = si.initialize();
		} else{
			final String method = className + ".initialize()";
			throw new IllegalArgumentException(method + ": bridge type, " +
				bridgeType + ", has not yet been implemented");
		}
		
		// Grab the {@link Nas} singleton.
		Nas nas = Nas.getInstance();
		
		// Create events for all first leg departing flights and
		// add them to the queue.
		IFlight[] flights = simElements.getFlights();
		for(IFlight fl : flights){
			// Only first legs.
			if(fl.getPrevLeg() == null){
				INode te = fl.getRoute().getNextNode();
				if(te instanceof Terminal ){
					// Create Terminal event for this flight.
					IEvent eve = 
						((Terminal)te).createEventForFirstLegDepartingFlight(fl);
					eventQueue.add(eve);
				}else {
					final String method = className + ".initialize()";
					throw new IllegalArgumentException(method + ": first node, " +
						te + ", of flight, " + fl + " is not a Terminal node.");
				}
			}
		}
	}
	
	/**
	 * Runs the simulation by processing events as they exit the
	 * queue.
	 */
    public void run(){
    	Logger logger = Logger.getLogger(loggerName);
    	while(eventQueue.size()>0){
    		// Extract event and process it.
    		IEvent nextEvent = eventQueue.poll();
    		time = nextEvent.getTime();
    		logger.info("event time: " + time);
    		nextEvent.processEvent(eventQueue);
    		
    		//if(time == 131796){
    		//	System.out.println("yeah");
    		//}
    		//System.out.flush();
    	}
    }
    /**
     * Produces output, usually to a file, after the sim is run.
     * @return <code>true</code> if successful; <code>false</code> if not.
     */
    public boolean processOutput(){ 
    	boolean success = true;
    	String baseDir = props.getProperty("baseDir");
    	String subDir = props.getProperty("subDir");
    	String outputDir = props.getProperty("outputDir");
    	
    	String baseDay = simElements.getBaseDay();
		String forecastDay = simElements.getForecastDay();
		String flightOutputFile = props.getProperty("flightOutputFilePrefix") + 
			"_" + forecastDay + "_" + baseDay + 
			props.getProperty("flightOutputFileSuffix");
    	File flightOutputFileNPath = new File(baseDir + File.separator +
    		subDir + File.separator + outputDir + File.separator +
    		flightOutputFile);
    	
    	PrintWriter pw = null;
    	Nas nas = Nas.getInstance();
    	
    	try{
    		pw = new PrintWriter(flightOutputFileNPath);
    		
    		// File header.
    		pw.println("#");
    		pw.println("#Flight output for base day, " + baseDay + ", and forecast day, "+
    			forecastDay);
    		pw.println("# line format:");
    		String lineFormat = "#itin_num,leg_num,sched_id,carrier,equip_type," +
    			"dep_aprt,arr_aprt,sched_out_time,act_out_time,calc_off_time," +
    			"act_off_time,calc_on_time,act_on_time,sched_in_time,calc_in_time," +
    			"act_in_time,act_air_time,turn_time,pushback_time,taxi_out_time," +
    			"taxi_in_time,gate_out_delay,dep_delay,arr_delay,taxi_in_delay," +
    			"dep_fix,dep_fix_delay,arr_fix,arr_fix_delay";
    		pw.println(lineFormat);
    		
    		IFlight[] flights = simElements.getFlights();
    		int itinNum=0,legNum=0,schedId=0,depAprt=0,arrAprt=0;
    		int carrier=0,equipType=0,schedOutTime=0,actOutTime=0;
    		int calcOffTime=0,actOffTime=0,actAirTime=0,calcOnTime=0,actOnTime=0;
    		int schedInTime=0,calcInTime=0,actInTime=0,turnTime=0;
    		int pushbackTime=0,taxiOutTime=0,taxiInTime=0;
    		int depFix=0,depFixDelay=0,arrFix=0,arrFixDelay=0;
    		String depAprtStr=null,arrAprtStr=null,carrierStr=null,equipTypeStr=null;
    		String depFixStr=null,arrFixStr=null;
    		int gateOutDelay=0,depDelay=0,arrDelay=0,taxiInDelay=0;
    		int goDAvg=0,dDAvg=0,aDAvg=0,tIDAvg=0,dfDAvg=0,afDAvg=0;
    		int count = 0;
    		for(IFlight f : flights){
    			
    			// Flight properties.
    			itinNum = f.get(IFlight.Param.ITIN_NUM);
    			legNum  = f.get(IFlight.Param.LEG_NUM);
    			schedId= f.get(IFlight.Param.SCHED_ID);
    			depAprt = f.get(IFlight.Param.DEP_APRT);
    			arrAprt = f.get(IFlight.Param.ARR_APRT);
    			carrier = f.get(IFlight.Param.CARRIER);
    			equipType=f.get(IFlight.Param.EQUIP_TYPE);
    			depFix   = f.get(IFlight.Param.DEP_FIX);
    			arrFix   = f.get(IFlight.Param.ARR_FIX);
    			if(depAprt >= 0) depAprtStr = nas.getAirportFromIndex(depAprt);
    			else depAprtStr = "";
    			if(arrAprt >= 0) arrAprtStr = nas.getAirportFromIndex(arrAprt);
    			else arrAprtStr = "";
    			if(carrier >= 0) carrierStr = nas.getCarrierFromIndex(carrier);
    			else carrierStr = "";
    			if(equipType >= 0) equipTypeStr = 
    				nas.getEquipTypeFromIndex(equipType);
    			else equipTypeStr = "";
    			if(depFix >= 0) depFixStr = nas.getFixNameFromIndex(Nas.Ad.DEP, depFix);
    			else depFixStr = "";
    			if(arrFix >= 0) arrFixStr = nas.getFixNameFromIndex(Nas.Ad.ARR, arrFix);
    			else arrFixStr = "";
    			
    			// Flight times.
    			schedOutTime = f.get(IFlight.Param.SCHED_OUT_TIME);
    			actOutTime   = f.get(IFlight.Param.ACT_OUT_TIME);
    			calcOffTime  = f.get(IFlight.Param.CALC_OFF_TIME);
    			actOffTime   = f.get(IFlight.Param.ACT_OFF_TIME);
    			actAirTime   = f.get(IFlight.Param.ACT_AIR_TIME);
    			calcOnTime   = f.get(IFlight.Param.CALC_ON_TIME);
    			actOnTime    = f.get(IFlight.Param.ACT_ON_TIME);
    			schedInTime  = f.get(IFlight.Param.SCHED_IN_TIME);
    			calcInTime   = f.get(IFlight.Param.CALC_IN_TIME);
    			actInTime    = f.get(IFlight.Param.ACT_IN_TIME);
    			turnTime     = f.get(IFlight.Param.TURN_TIME);
    			pushbackTime = f.get(IFlight.Param.PUSHBACK_TIME);
    			taxiOutTime  = f.get(IFlight.Param.TAXI_OUT_TIME);
    			taxiInTime   = f.get(IFlight.Param.TAXI_IN_TIME);
    			depFixDelay  = f.get(IFlight.Param.DEP_FIX_DELAY);
    			arrFixDelay  = f.get(IFlight.Param.ARR_FIX_DELAY);
    			
    			gateOutDelay   = actOutTime - schedOutTime;
    			depDelay       = actOffTime - calcOffTime;
    			arrDelay       = actOnTime - calcOnTime;
    			taxiInDelay    = actInTime - calcInTime;
    			
    			String com = ",";
    			pw.println(itinNum + com + legNum + com + schedId + com +
    				carrierStr + com + equipTypeStr + com + depAprtStr + com + 
    				arrAprtStr + com + schedOutTime + com + actOutTime + com + 
    				calcOffTime + com + actOffTime + com + calcOnTime + com + 
    				actOnTime + com + schedInTime + com + calcInTime + com + 
    				actInTime + com + actAirTime + com + turnTime + com + 
    				pushbackTime + com + taxiOutTime + com + taxiInTime + com +
    				gateOutDelay + com + depDelay + com + arrDelay + com +
    				taxiInDelay + com + depFixStr + com + depFixDelay + com + 
    				arrFixStr + com + arrFixDelay);	
    			
    			// Average only non-VFR flights.
    			if(schedId >= 0){
	    			goDAvg += gateOutDelay;
	    			dDAvg += depDelay;
	    			aDAvg += arrDelay;
	    			tIDAvg+= taxiInDelay;
	    			dfDAvg += depFixDelay;
	    			afDAvg += arrFixDelay;
	    			count++;
    			}
    		}
    		
    		goDAvg /= count;
    		dDAvg  /= count;
    		aDAvg  /= count;
    		tIDAvg /= count;
    		dfDAvg /= count;
    		afDAvg /= count;
    		int totalDelay = goDAvg + dDAvg + dfDAvg + afDAvg + aDAvg + tIDAvg;
    		String msg1 = "gateOutDelayAvg: " + goDAvg + ", depDelayAvg: " +
    			dDAvg + ", arrDelayAvg: " + aDAvg;
    		String msg2 = "taxiInDelayAvg: " + tIDAvg + ", totalDelay: " +
				totalDelay + ", depFixDelayAvg: " + dfDAvg + 
				", arrFixDelayAvg: " + afDAvg;
    		System.out.println(msg1);
    		System.out.println(msg2);
    		Logger logger = Logger.getLogger(loggerName);
    		logger.info(msg1);
    		logger.info(msg2);
    
    	}catch(FileNotFoundException fnfe){
    		fnfe.printStackTrace();
    		success = false;
    	}finally {
    		pw.close();
    	}
    	return success;
    }
    
    /**
     * Adds an {@link IEvent} object to the queue.
     * @param e Event to add to the queue.
     */
    public void pushEvent(IEvent e){
    	eventQueue.add(e);
    }
    
    /**
	 * Groups the first leg flights by departure airport or arrival airport.   
	 * Only first legs are considered because info on other legs is contained in 
	 * the first leg.  The returned array will list airports by 
	 * index, the index being the same as for the mapping of the airport to an 
	 * index as done in {@link Nas}.  The second index denotes the ith flight.
	 * <p>
	 * Java does not have arrays of lists and kludging some created a strange
	 * problem.  Thus, I just use straight arrays below.
	 * @param numAprts Number of airports.  Usually obtained from the {@link Nas}
	 *   object.
	 * @param aprtGroupType Type of airport grouping, departure or arrival.  
	 * @param flights Array of flights.
	 * @return 2D array of {@link IFlight} objects, the first element 
	 *   corresponds to the ith airport, either departing or arriving.  The 
	 *   second element denotes the flight.
	 */
	public static IFlight[][] groupFirstLegFlightsByAprt(int numAprts,
		Nas.Ad aprtGroupType, IFlight[] flights){
		IFlight[][] fByAprt = new IFlight[numAprts][];
		
		// Count how many flights per airport.
		int[] nFlights = new int[numAprts];
		for(int i=0;i<numAprts;i++){
			nFlights[i] = 0;
		}
		int depAprt = 0, arrAprt = 0, leg = -1;
		for(int i=0;i<flights.length;i++){
			leg = flights[i].get(IFlight.Param.LEG_NUM);
			if(leg == 1){
				if(aprtGroupType.equals(Nas.Ad.DEP)){
					depAprt = flights[i].get(IFlight.Param.DEP_APRT);
					nFlights[depAprt]++;
				}else {
					arrAprt = flights[i].get(IFlight.Param.ARR_APRT);
					nFlights[arrAprt]++;
				}
			}
		}
		for(int i=0;i<numAprts;i++){
			if(nFlights[i] == 0)fByAprt[i] = null;
			else fByAprt[i] = new IFlight[nFlights[i]];
		}
		
		// Zero nFlights and use as count variable.
		for(int i=0;i<numAprts;i++)nFlights[i] = 0;
		// Populate arrays.
		for(int i=0;i<flights.length;i++){
			leg = flights[i].get(IFlight.Param.LEG_NUM);
			if(leg == 1){
				if(aprtGroupType.equals(Nas.Ad.DEP)){
					depAprt = flights[i].get(IFlight.Param.DEP_APRT);
					fByAprt[depAprt][nFlights[depAprt]] = flights[i];
					nFlights[depAprt]++;
				}else {
					arrAprt = flights[i].get(IFlight.Param.ARR_APRT);
					fByAprt[arrAprt][nFlights[arrAprt]] = flights[i];
					nFlights[arrAprt]++;
				}
			}
		}			
		return fByAprt;	
	}
	
	/**
	 * Reads the sim input properties from a file.  The input properties
	 * are those that are needed to create the sim, which may include
	 * other properties files.  If needed
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
		String method = className + ".readModelInputProperties()";
		
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
		String[] propertyNames = {"baseDir","subDir","baseDay",
			"bridgeType","bridgePropsFile","outputDir","flightOutputFilePrefix",
			"flightOutputFileSuffix","loggingPropsFile","logFileName"};
			
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
	
	//--------------------------------------------------------------------------
	// Methods for testing.
	//--------------------------------------------------------------------------
	/**
	 * Returns sim props object.  Used for testing.
	 * @return Simulation properties object.
	 */
	public Properties getProperties(){return props;}
	/**
	 * Returns sim main event queue.
	 * @return sim main event queue
	 */
	public IQueue<IEvent> getMainQueue(){return eventQueue;}
	
	/**
	 * Groups the first leg flights by departure airport or arrival airport.   
	 * Only first legs are considered because info on other legs is contained in 
	 * the first leg.  The returned array will list airports by 
	 * index, the index being the same as for the mapping of the airport to an 
	 * index as done in {@link Nas}.
	 * <p>
	 * This doesn't work for some reason.  I suspect the lack of hard typing
	 * on the lists, but I am not sure.
	 * <p>
	 * @param numAprts Number of airports.  Usually obtained from the {@link Nas}
	 *   object.
	 * @param aprtGroupType Type of airport grouping, departure or arrival.  
	 * @param flights Array of flights.
	 * @return Array of {@link IFlight} arrays, each array corresponds to
	 *   one of departing flights from the ith airport...or...arriving airport.
	 *   But for the latter only flights which have no departing airport are
	 *   considered.
	 */
/*
	public static Object[] groupFirstLegFlightsByAprtOld(int numAprts,
		Nas.Ad aprtGroupType, IFlight[] flights){
		Object[] fByAprt = new Object[numAprts];
		for(int i=0;i<numAprts;i++){
			fByAprt[i] = new ArrayList<IFlight>();
		}
		int depAprt = 0, arrAprt = 0, leg = -1;
		for(int i=0;i<flights.length;i++){
			leg = flights[i].get(IFlight.Param.LEG_NUM);
			if(leg == 1){
				if(aprtGroupType.equals(Nas.Ad.DEP)){
					depAprt = flights[i].get(IFlight.Param.DEP_APRT);
					((List<IFlight>)fByAprt[depAprt]).add(flights[i]);
				}else {
					arrAprt = flights[i].get(IFlight.Param.ARR_APRT);
					((List<IFlight>)fByAprt[arrAprt]).add(flights[i]);
				}
			}
		}
		// Convert lists to IFlight arrays.
		IFlight[] fl = new IFlight[1];
	//	int nF = 0;
		for(int i=0;i<numAprts;i++){
	//		nF += ((List<IFlight>)fByAprt[i]).size();
			fByAprt[i] = ((List<IFlight>)fByAprt[i]).toArray(fl);
		}
		return fByAprt;	
	}
*/
	
  
}
