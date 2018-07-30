package org.cna.donley.nassim2_4;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Stores general info about the NAS, including maps of names to indexes.  
 * None of these quantities, once set, are changed.  Enums
 * are used throughout the simulation, but the other methods are used only 
 * during sim initialization and output.  Is a bag of miscellaneous stuff.  
 * Is a singleton class, so it has no interface.  Could have stored the
 * map stuff more elegantly, but Java does not allow array of Generics
 * such as maps and lists.  Thus, each map is treated separately.  Oh, well.
 * <p>
 * This follows the Singleton pattern, which means there will
 * exist only one instance of this class.  It is instantiated with a
 * call to {@link #createInstance(String[],String[],String[],String[][])}.
 * Any subsequent call must first retrieve the class instance using the
 * method {@link #getInstance()}.  There is no way to reset the singleton 
 * instance parameters: one needs to destroy the instance as a whole
 * using {@link #destroyInstance()}.
 * <p>
 * Note that all times within the simulation are in seconds.  Input data
 * must then be converted to seconds before being used naturally.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: Nas.java 1 2009-06-04 00:00:00EST $
 */

public class Nas
{ 		
	/**
	 * Airport meteorological condition types.  Used to map these types
	 * to indices through Type.ordinal().
	 */
	public static enum Mc {
		/** Visual meteorological conditions. */
		VMC,
		/** Instrument meteorological conditions. */
		IMC,
		/** Medium meteorolological conditions. */
		MVMC};
		
	/**
	 * Arrival and departure enum.  Use as an index to keep things straight.
	 */
	public static enum Ad {
		/** Departure */
		DEP,
		/** Arrival */
		ARR};
		
	/**
	* Mean and standard deviation of a probability distribution.
	*/
	public static enum Distrib {
		/** Mean.  Units: seconds if time. */
		MEAN,
		/** Standard Deviation.  Units: seconds if time */
		STDDEV};

		
	 /**
     * Stores single instance of class.
     */
    private static Nas instance = null; 
	
	/**
	 * Maps airport code to an integer.  Integer value is used in the core
	 * sim to reference an airport.
	 */
	private Map<String,Integer> airportToIndexMap = null;
	/**
	 * The reverse of the airportToIndexMap.
	 */
	private String[] indexToAirportMap = null;
	
	/**
	 * Maps air carrier code to an integer.  Integer value is used in the 
	 * core sim to reference an air carrier.
	 */
	private Map<String,Integer> carrierToIndexMap = null;
	/**
	 * The reverse of the carrierToIndexMap.
	 */
	private String[] indexToCarrierMap = null;
	
	/**
	 * Maps aircraft equipment type to an integer.  Integer value is used 
	 * in the core sim to reference an aircraft type.  The equipment type
	 * is almost always the ETMS one, rather than the BADA.
	 */
	private Map<String,Integer> equipTypeToIndexMap = null;
	/**
	 * The reverse of the etmsEquipTypeToIndexMap.
	 */
	private String[] indexToEquipTypeMap = null;
	
	/**
	 * Maps a departure fix name to an index.
	 */
	private Map<String, Integer> depFixToIndexMap = null;
	/**
	 * Maps an arrival fix name to an index.
	 */
	private Map<String, Integer> arrFixToIndexMap = null;
	/**
	 * The reverse of the depFixToIndexMap and arrFixToIndexMap.
	 */
	private String[][] indexToFixMap = null;
	
	/**
	 * A map from the name of a airport condition, such as VMC, as given
	 * by the {@link Nas.Mc} enum above to an array index.  For example,
	 * if the MC condition is IMC, this will be the same index as given
	 * by the {@link #Nas.Mc.IMC.ordinal()} method.
	 */
	private Map<String,Integer> mcToIndxMap = null;
	
	/**
	 * Seed for the initialization of the Random class.
	 */
	private final long seed = 214748364;
	/**
	 * Instance of the {@link Random} class.  Will be used to generate
	 * samples from a normal distribution, among other methods in that 
	 * class.
	 */
	private final Random random = new Random(seed);
	
	/**
	 * Constructor is private for a singleton.
	 */
	private Nas(){}
	
	/**
     * Creates an instance of the class and populates it with the
     * parameters below.  Method is synchronized so two threads can not
     * access it at the same time.
     * @param indxToAprtMap  Array of all airport names for this NAS network.
     * @param indxToCarrierMap Array of all air carrier names for this NAS 
     *   network.
     * @param indxToEquipTypeMap Array of all aircraft names for this NAS
     *   network.
     * @param indxToFixMap Array of dep and arr fix names.  If have no fixes
     *   then this array can be <code>null</code>.
     */
    public static synchronized void createInstance( 
    	String[] indxToAprtMap, String[] indxToCarrierMap, 
    	String[] indxToEquipTypeMap, String[][] indxToFixMap){
    	if(instance == null){
    		instance = new Nas();	
    		instance.indexToAirportMap = indxToAprtMap;
    		instance.createAirportToIndexMap();
    		instance.indexToCarrierMap = indxToCarrierMap;
    		instance.createCarrierToIndexMap();
    		instance.indexToEquipTypeMap = indxToEquipTypeMap;
    		instance.createEquipTypeToIndexMap();
    		instance.createMcToIndxMap();
    		if(indxToFixMap != null){
	    		instance.indexToFixMap = indxToFixMap;
	    		instance.createFixToIndexMaps();
    		}
    	} else {
    		// Just print a warning that the instance has already been
    		// created.
    		final String method = 
    			instance.getClass().getName() + ".createInstance()";
    		System.err.println(method + ": Warning: instance has already " +
    			" been created, so this call does nothing.");
    	}
    }
    
    /**
     * Destroys the instance, so it can be re-initialized again.  Done so
     * that in the same process the object can be re-set.
     */
    public static synchronized void destroyInstance(){
    	instance = null;
    }
    
    /**
     * Returns the single instance of the class.
     * @return instance of class.  If the class has not been instantiated,
     *   this method returns <code>null</code>.
     */
    public static Nas getInstance(){
    	return instance;
    }
	
	/**
	 * Returns the airport index as an int.  If the airport is not in
	 * the map it returns -1.
	 * @param aprt Airport
	 * @return airport index as an int.
	 */
	public int getAirportIndex(String aprt){
		Integer indx = airportToIndexMap.get(aprt);
		if(indx != null) return indx.intValue();
		else return -1;
	}
	/**
	 * Returns the carrier index as an int.  If the carrier is not in
	 * the map it returns -1.
	 * @param ac carrier
	 * @return air carrier index as an int.
	 */
	public int getCarrierIndex(String ac){
		Integer indx = carrierToIndexMap.get(ac);
		if(indx != null) return indx.intValue();
		else return -1;
	}
	/**
	 * Returns the aircraft equipment type index as an int.  If the aircraft is 
	 * not in the map it returns -1.
	 * @param et Equipment type, e.g, "CRJ2".
	 * @return aircraft type index as an int.
	 */
	public int getEquipTypeIndex(String et){
		Integer indx = equipTypeToIndexMap.get(et);
		if(indx != null) return indx.intValue();
		else return -1;
	}
	/**
	 * Gets the fix index from its name and type.
	 * @param type Type of fix.  Dep or Arr.
	 * @param fixName Name of fix.
	 * @return fix index.
	 */
	public int getFixIndex(Nas.Ad type, String fixName){
		Integer indx = null;
		if(type == Nas.Ad.DEP){
			indx = depFixToIndexMap.get(fixName);
		} else {
			indx = arrFixToIndexMap.get(fixName);
		}
		if(indx != null) return indx.intValue();
		else return -1;		
	}
	/**
	 * Returns the airport name as a string, knowing the index associated
	 * with that airport. If the index is out of range, returns null.
	 * @param i Airport index.
	 * @return airport name or <code>null</code> if index is out of range.
	 */
	public String getAirportFromIndex(int i){
		if(i < 0 || i > indexToAirportMap.length - 1)return null;
		return indexToAirportMap[i];
	}
	/**
	 * Returns the air carrier name as a string, knowing the index associated
	 * with that carrier.
	 * @param i carrier index.
	 * @return carrier name or <code>null</code> if index is out or range.
	 */
	public String getCarrierFromIndex(int i){
		if(i < 0 || i > indexToCarrierMap.length - 1)return null;
		return indexToCarrierMap[i];
	}
	/**
	 * Returns the aircraft equipment type name as a string, knowing the index 
	 * associated with that aircraft.
	 * @param i equip type index.
	 * @return equip type name or <code>null</code> if index is out of range.
	 */
	public String getEquipTypeFromIndex(int i){
		if(i < 0 || i > indexToEquipTypeMap.length - 1)return null;
		return indexToEquipTypeMap[i];
	}
	/**
	 * Returns the fix name from the fix type and index.
	 * @param type Fix type.  Dep or Arr.
	 * @param i Index.
	 * @return Fix name from index and type.
	 */
	public String getFixNameFromIndex(Nas.Ad type, int i){
		if(i < 0 || i > indexToFixMap[type.ordinal()].length - 1)return null;
		return indexToFixMap[type.ordinal()][i];
	}
	/**
	 * Returns the number of airports in the index to airport map and
	 * vice versa.
	 * @return number of airports.
	 */
	public int getNumAirports(){
		return indexToAirportMap.length;
	}
	/**
	 * Returns the number of air carriers in the index to carrier map and
	 * vice versa.
	 * @return number of air carriers.
	 */
	public int getNumCarriers(){
		return indexToCarrierMap.length;
	}
	/**
	 * Returns the number of aircraft types in the index to equip type
	 * map and vice versa.
	 * @return number of aircraft equipment types.
	 */
	public int getNumEquipTypes(){
		return indexToEquipTypeMap.length;
	}
	/**
	 * Returns the number of dep or arr fixes.
	 * @param type Type of fix.  Arrival or departure.
	 * @return number of fixes.
	 */
	public int getNumFixes(Nas.Ad type){
		return indexToFixMap[type.ordinal()].length;
	}
	/**
	 * Returns the index corresponding to the meteorological condition
	 * name.  For example, if mcName is "IMC", then the method will 
	 * return an integer equal to Mc.IMC.ordinal().
	 * If the name is not in the map, then it returns -1.
	 * @param mcName Meterological condition as a name.  Should be one
	 *   of those in the {@link Mc} enum.
	 * @return index corresponding to the MC name.
	 */
	public int getMcIndex(String mcName){
		Integer indx = mcToIndxMap.get(mcName);
		if(indx != null) return indx.intValue();
		else return -1;
	}

	/**
	 * Return the instance of the {@link Random} class used to generate
	 * random numbers.
	 * @return instance of the {@link Random} class.
	 */
	public Random getRandom(){return random;}
	
    /**
     * Creates the map from an airport name to an index.  The index will
     * be used by the simulation to keep track of airports.
     */
    private void createAirportToIndexMap(){
    	airportToIndexMap = new HashMap<String,Integer>();
    	for(int i=0;i<indexToAirportMap.length;i++){
    		airportToIndexMap.put(indexToAirportMap[i], (new Integer(i)));
    	}  	
    }
    /**
     * Creates the map from an air carrier name to an index.  The index will
     * be used by the simulation to keep track of carriers.
     */
    private void createCarrierToIndexMap(){
    	carrierToIndexMap = new HashMap<String,Integer>();
    	for(int i=0;i<indexToCarrierMap.length;i++){
    		carrierToIndexMap.put(indexToCarrierMap[i], (new Integer(i)));
    	}  	
    }
    /**
     * Creates the map from an aircraft equipment name to an index.  The index 
     * will be used by the simulation to keep track of aircraft types.
     */
    private void createEquipTypeToIndexMap(){
    	equipTypeToIndexMap = new HashMap<String,Integer>();
    	for(int i=0;i<indexToEquipTypeMap.length;i++){
    		equipTypeToIndexMap.put(indexToEquipTypeMap[i], (new Integer(i)));
    	}  	
    }
    /**
     * Creates a map from the name of a meteorlogical condition, e.g., 
     * MVMC, to an array index.  This index will be the same as that
     * given by the method, e.g., {@link #Nas.Mc.MVMC.ordinal()}.
     */
    private void createMcToIndxMap(){
    	mcToIndxMap = new HashMap<String,Integer>();
    	Mc[] mcs = Mc.values();
    	for(int i=0;i<mcs.length;i++){
    		mcToIndxMap.put(mcs[i].toString(), 
    				(new Integer(mcs[i].ordinal())));
    	}
    }
    /**
     * Creates the maps from a dep or arr fix name to an index. The index
     * will be used to keep track of fix names.
     */
    private void createFixToIndexMaps(){
    	depFixToIndexMap = new HashMap<String,Integer>();
    	arrFixToIndexMap = new HashMap<String,Integer>();
    	for(int i=0;i<indexToFixMap[Nas.Ad.DEP.ordinal()].length;i++){
    		depFixToIndexMap.
    			put(indexToFixMap[Nas.Ad.DEP.ordinal()][i],(new Integer(i)));
    	} 
    	for(int i=0;i<indexToFixMap[Nas.Ad.ARR.ordinal()].length;i++){
    		arrFixToIndexMap.
    			put(indexToFixMap[Nas.Ad.ARR.ordinal()][i],(new Integer(i)));
    	} 
    }
    
    /**
	 * Sets the sim timestep.  Only done in this class.  Checks if the 
	 * number of sim timesteps in a quarter hour is a whole number.  Doesn't
	 * allow timesteps that are negative, larger than a quarter hour, or
	 * such that a quarter hour is not a whole number in timesteps.
	 * Units: seconds.
	 * @param dt The sim timstep 
	 */
/*
	private void setDelt(int dt){
		if(dt <= 0){
			final String method = this.getClass().getName() + ".setDelt()";
			throw new IllegalArgumentException(method + ": Sim timestep is: " +
				dt + " which is less than 1");
		}
		// Don't allow timesteps that are greater than a quarter hour.
		if(dt > 15*60){
			final String method = this.getClass().getName() + ".setDelt()";
			throw new IllegalArgumentException(method + ": Sim timestep is: " +
				dt + " larger than a quarter hour.  Please reduce.");
		}
		// Does the time increment give a whole number of sim timesteps in a
		// a quarter hour?  This is important as the qtr hour is used by
		// the {@link Runway} class to compute acceptance rates.
		if(!wholeNumSimTimestepsInQtrhour(dt)){
			final String method = this.getClass().getName() + ".setDelt()";
    		throw new IllegalArgumentException(method + ": Invalid sim " +
    			" timestep.  Does not give a whole number of sim timesteps in "+
    			" a qtrhour.  Try again. ");
		}
		delt = dt;
	}
*/
	/**
	 * Gets the sim timestep. Units: seconds. 
	 * @return sim timestep;
	 */
 /*
	public int getDelt(){
		return delt;
	}
  */
	/**
	 * Inserts a single airport into the airport/index map.
	 * @param str
	 * @param i
	 */
	/*
	public void addAirportToMap(String str, int i){
		airportToIndexMap.put(str,(new Integer(i)));
	}
	*/
    /**
	 * Elements of the NAS model.  Are either nodes or edges.  Just
	 * have nodes for now.
	 */
	//public static enum Element{
	//	/** Airport taxiway */
	//	TAXIWAY,
	//	/** Airport terminal */
	//	TERMINAL,
	//	/** Airport runway */
	//	RUNWAY,
	//	/** Airport or route fix point */
	//	FIX,
	//	/** Airspace sector */
	//	SECTOR};
    
    /**
     * Determines if a quarter hour in terms of sim timesteps is a whole
     * number.  
     * @param delt The timestep to check.
     * @return <code>true</code> if yes; <code>false</code> if not.
     */
/*
    private boolean wholeNumSimTimestepsInQtrhour(int delt){
    	double numD = 15.*60./(double)delt;
    	int numInt =  (int)numD;
    	if(abs(numD -(double)numInt) > Constants.EPS) return false;
    	else return true; 	
    }
 */
    
}
