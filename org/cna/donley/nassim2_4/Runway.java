package org.cna.donley.nassim2_4;

import org.cna.donley.utils.Constants;
import static java.lang.Math.abs;

/**
 * An implementation of the interface {@link INode} for an airport 
 * runway(s).  The runway uses airport capacity info.  This could be the actual
 * called acceptance rates for each timestep delt.  Or it could be the 
 * pareto curves and the airport conditions (translated to capacity changes).
 * <p>
 * Acceptance rates are computed each quarter hour no matter what the sim
 * timestep is.  This means that the timestep better be smaller than 15 min.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: Runway.java 1 2009-06-02 00:00:00EST $
 */

public class Runway implements INode
{
	/**
	 * Airport to which the runway belongs.
	 */
	private int airport = 0;
	/**
	 * Start time of the simulation.  Units: seconds.
	 */
	private int startTime = 0;
    
    /** The number of seconds in a quarter hour.  All times in this
     *  simulation are in seconds, so this value will be used to determine
     *  when to compute the acceptance rates. Units: seconds.
     */
    private final int qtrhourInSecs = 15*60;
    
    /**
     * Neighboring Node.  Any other NAS element info, such as the next queue 
     * for a departing flight, will be obtained from the flight itself.
     */
    private INode taxiwayNode = null;
    
    /**
     * Use the known historical acceptance rates if true.  If not, use the change
     * capacities and pareto curves to compute the acceptance rates.
     */
    private boolean computeRates = false;
    /**
     * Historical acceptance rates for this runway and day.  The first element
     * is an index specifying the quarter hour after the sim start date,
     * midnight being element 0.  The second element is the number per quarter 
     * hour, arrival or departure, with the index being determined by
     * {@link Nas.Ad}.
     */
    private int[][] calledRates = null;
    
    /**
     * Curves for all possible conditions, whatever they
     *  are.  Which one is chosen depends on the contents of the change
     *  capacity array.  Format is: first index denotes the airport condition
     *  as an integer.  The mapping from IMC, VMC, etc., to an integer
     *  is given in the {@link Nas.Mc} enum.  The second index is an (x,y) 
     *  point on the curve.  The last index denotes the dimension, either 
     *  0 for x or Departure and 1 for y or Arrival, but the index is
     *  determined by the {@link Nas.Ad} enum.
     *  Units: the values for the number rates are w/r to a single sim
     *  timestep.  Any conversion must have been done during initialization.
     */
    private double[][][] paretoCurve = null;
    
    /**
    *   Queue of airport conditions.  Gives numbers
    *   usually only if the condition has changed; thus the name.  First
    *   element is the time of the change in sim time, e.g., 482 (in units
    *   of delt) and what weather condition the airport now has, e.g.,
    *   IMC, MVMC, etc.  These latter are represented as integers
    *   from the {@link Nas.Mc} enum. So, [0][0] is the time of the first
    *   entry and [0][1] is the meteorological condition of the first entry.  
    *   Note that both are set only once here since the weather is supposedly
    *   known.  Assume they must be ordered from soonest to latest when 
    *   initialized.
    */
    private int[][] changeCapacity = null;
    
    /**
     * Index of element in {@link #changeCapacity} that was used the last
     * time.  Is used so that methods will not need to cycle through
     * the whole {@link #changeCapacity} array each time they want to get the
     * current MC condition.  
     */
    private int lastIndxChangeUsed = 0;
    /**
     * Array that holds the values of the estimated runway on and off times for
     * all the flights that go through this airport. First element is the index
     * corresponding to arrival or departure and the second index denotes
     * a listing of the times.  It is assumed these arrays are ordered by
     * time.  Units: seconds.
     */
    private int[][] estOnNOffTimes = null;
    
    /**
     * Time of last allowed runway off or on time. Units: seconds from some 
     * reference.
     */
    private int[] lastTime = null;
    
    /**
     * Time of next allowed runway off or on time.  It will computed
     * at the "last" time. Units: seconds from some reference.
     */
    private int[] nextTime = null;
    
    /**
     * Measures used to order flights in the queues.
     */
    private IFlight.Param[] measures = null;
    
    /**
     * Departure and arrival queues, the index being from (@link Nas.Ad}.
     */
    private IQueue<IFlight>[] queues = null;
    /**
     * Status of holds on queues.  If <code>true</code> then there
     * is a {@link RunwayEvent} to be executed when the hold time is up.  This
     * flag is used to prevent more than one event having to be created
     * to "release" the hold.
     */
    private boolean[] queueHoldStatus = null;
    
    /**
     * Make default constructor private.
     */
    private Runway(){}
    
    /**
     * @param aprt Airport to which the runway belongs.  The {@link Nas} class
     *   should have a map from index to airport name.
     * @param startTime Start time of the simulation.  Could be zero or could
     *   be an absolute time. (NASPAC it is zero)  But it must be the reference.
     *   time for which all the other times are relative to.  Units: seconds.
     *   I don't see how this could be anything but w/r to midnight of the start
     *   day, but leave it in anyways.
     * @param computeRates If <code>true</code>, use the historical acceptance
     *  rates for this runway; if <code>false</code> use the change capacities
     *  and pareto curves to compute the rates.
     * @param calledRates Array of know historical quarter hour acceptance rates.
     *   Is used if the computeRates flag is <code>false</code>.  However,
     *   if this array is <code>null</code>, the capacities will be set to be
     *   effectively infinite.
     * @param paretoCurve Curves for all possible conditions, whatever they
     *   are.  Which one is chosen depends on the contents of the change
     *   capacity array.  It is assumed that the curves points are ordered
     *   as {d0,a0}, {d1,a1},..., where d and a refer to dep and arr,
     *   respectively, and the pairs are ordered such that d_i+1 > d_i 
	 *   for all i.  It is also assumed the numbers are w/r to a quarter hour.
     * @param changeCapacity  Array of airport conditions.  Gives numbers
     *   usually only if the condition has changed; thus the name.  First
     *   element is the time of the change in sim time, e.g., 482 (in units
     *   of delt) and what weather condition the airport now has, e.g.,
     *   IMC, MVMC, etc.  These latter are represented as integers
     *   from the {@link Nas.Mc} enum.  Note that both are set only once
     *   here since the weather is supposedly known.  It is assumed that
     *   the array elements are ordered by time.  If the airport capacities
     *   are not being modeled, this array is <code>null</code>.  If so,
     *   the capacity will be set to infinite if called rates aren't used.
     *  @param estOnNOffTimes 2D array of estimated departure and arrival times.
     *    First element is the type, on or off, given by the enum
     *    {@link Nas.Ad} and the second is a number index.  It is assumed the
     *    times are in ascending order.  If the acceptance rates are not
     *    being computed this array can be <code>null</code>.
     *     
     */
    public Runway(int aprt, int startTime, boolean computeRates, 
    	int[][] calledRates, double[][][] paretoCurve, int[][] changeCapacity,
    	int[][] estOnNOffTimes){
    	airport = aprt;
    	this.startTime     = startTime;
    	this.computeRates= computeRates;
    	this.calledRates   = calledRates;
    	//if(computeRates == false && calledRates == null){
    	//	final String method = this.getClass().getName();
    	//	throw new IllegalArgumentException(method + ": called Rates flag "+
    	//		"is set true yet calledRates array is null.");
    	//}
    	this.estOnNOffTimes = estOnNOffTimes;
    	this.paretoCurve   = paretoCurve;
    	this.changeCapacity= changeCapacity;
    	lastIndxChangeUsed = 0; // Must be zero!
    	// Initialize last acceptance times.  Set default to -1 so will know
    	// it is the initial value.
    	lastTime = new int[2];
    	for(int i=0;i<lastTime.length;i++)lastTime[i] = -1;
    	// Initialize next acceptance times.  Set default to startTime 
    	// as that will be the first time off.
    	nextTime = new int[2];
    	for(int i=0;i<nextTime.length;i++)nextTime[i] = startTime;
    	
    	// Create runway queues and associated objects. 
    	int nQs = 2;
    	measures = new IFlight.Param[nQs];
    	queues = new FlightQueue[nQs];
    	queueHoldStatus = new boolean[nQs];
    	measures[Nas.Ad.DEP.ordinal()] = IFlight.Param.CALC_OFF_TIME;
    	measures[Nas.Ad.ARR.ordinal()] = IFlight.Param.ACT_ON_TIME;
    	for(int i=0;i<nQs;i++){
    		queues[i] = new FlightQueue(measures[i]);
    		queueHoldStatus[i] = false;// false at first obviously.
    	}
    	
    }  
    
    /**
     * Get a runway queue.  Departure or arrival.
     * @param type Type of queue wanted.
     * @return runway queue.
     */
    public IQueue<IFlight> getQueue(Nas.Ad type){
    	return queues[type.ordinal()];	
    }
    /**
     * Get the hold status of the queue.  If <code>true</code> then some
     * event has been created, which when executed at the hold end time, w
     * ill access the queue.
     * @param type Type of queue.
     * @return status of hold on events associated with this runway.
     */
    public boolean getQueueHoldStatus(Nas.Ad type){
    	return queueHoldStatus[type.ordinal()];
    }
    /**
     * Sets the hold status of the queue.
     * @param type Type of queue.
     * @param status If <code>true</code>, then 
     */
    public void setQueueHoldStatus(Nas.Ad type, boolean status){
    	queueHoldStatus[type.ordinal()] = status;
    }
    
    /**
     * Used to send a message to the node. Does nothing right now.
     * @param sender The sender of the message.
     * @param message What the node should do.
     * @return <code>true</code> if accepted; <code>false</code> if not.
     */
    public synchronized boolean receive(Object sender, Object message){
    	return false;
    }
    
    /**
     * Sets a neighbor of this node.  Used when creating the NAS network
     * of nodes and edges.
     * @param n The neighboring element.
     */
    public void setNeighbor(INode n){
    	// Only neighbor right now is the taxiway.
    	if(n instanceof Taxiway){
    		taxiwayNode = n;
    	} else {
    		final String method = this.getClass().getName()+".setNeighbor()";
    		throw new IllegalArgumentException(method + ": node type: " +
    			n.getClass().getName() + " is not a neighbor of a runway.");
    	}
    }
    /**
     * Gets the node neighbors as an array.
     * @return array of node neighbors.
     */
    public INode[] getNeighbors(){
    	INode[] nn = new INode[1];
    	nn[0] = taxiwayNode;
    	return nn;
    }
 
    /**
     * Sets the last time a plane tookoff or landed.
     * @param aOrD Arrival or Departure? 
     * @param time Time of the last arrival or departure, which one
     *   depending upon the aOrD value.
     */
    public void setLastTime(Nas.Ad aOrD, int time){
    	lastTime[aOrD.ordinal()] = time;
    }
    /**
     * Gets the last time a plane tookoff or landed, which one depending
     *  upon the aOrD value.
     * @param aOrD Arrival or departure?
     * @return Last time a plan arrived or departed, which
     *   one depending upon the aOrD value.
     */
    public int getLastTime(Nas.Ad aOrD){return lastTime[aOrD.ordinal()];}
    /**
     * Sets the next future time a plane tookoff or landed.
     * @param aOrD Arrival or Departure? 
     * @param time Time of the next arrival or departure, which one
     *   depending upon the aOrD value.
     */
    public void setNextTime(Nas.Ad aOrD, int time){
    	nextTime[aOrD.ordinal()] = time;
    }
    /**
     * Gets the next time a plane can takeoff or land, which one depending
     *  upon the aOrD value.
     * @param aOrD Arrival or departure?
     * @return Next time a plan will be allowed to arrive or depart, which
     *   one depending upon the aOrD value.
     */
    public int getNextTime(Nas.Ad aOrD){return nextTime[aOrD.ordinal()];}
    
    /**
     * Computes the next on or off time from this time.  This is done by computing the
     * acceptance times for the present qtr hour epoch.  They will be either
     * a qtr hour or less or infinite.  If infinite, go to the next epoch
     * and continue until find an epoch for which the time is less than infinite.
     * If one runs out of entries in the called rates, then are out of luck.
     * 
     * @param type Type of operation, arrival or departure.
     * @param time The last time an operation occured.  Or should be.
     * @return <code>true</code> if successful; <code>false</code> if not.
     */
    public boolean computeNextTime(Nas.Ad type, int time){
    	int[] acceptTimeDiffs = null;
    	if(computeRates){
    		// Compute using Pareto. Pareto always gives a non-zero rate.
    		acceptTimeDiffs = 
				computeAcceptTimeDiffsFromPareto(time);
			if(acceptTimeDiffs == null)return false;
			else {
				int nextTime = time + acceptTimeDiffs[type.ordinal()];
				setNextTime(type,nextTime);
			}
			return true;
    	} else {
    		
//    if(time == 103629){
//    	System.out.println("yeah");
//    }
    		// Compute using called rates.  These could be give an
    		// infinite time diff.  If so, need to go to future epochs 
    		// to find a finite one.  If can't, then are out of luck.
    		// This algorithm could be improved...
    		acceptTimeDiffs = computeAcceptTimeDiffsFromCalledRates(time);
    		if(acceptTimeDiffs == null)return false;
    		int timeFromStart = time - startTime;
    		int tt = timeFromStart - timeFromStart%qtrhourInSecs;
    		int count = 0;
   
    		while(acceptTimeDiffs[type.ordinal()] == Integer.MAX_VALUE){
    			tt += qtrhourInSecs; // start of an epoch.
    			count++;
    			acceptTimeDiffs = 
    				computeAcceptTimeDiffsFromCalledRates(tt);
    			if(acceptTimeDiffs == null)return false;	
    		}
    		// Have found an epoch with non-zero called rates.
    		int nextTime = 0;
    		if(count == 0)nextTime = time + acceptTimeDiffs[type.ordinal()];
    		else nextTime = tt + acceptTimeDiffs[type.ordinal()];
    		setNextTime(type,nextTime);
    		return true;
    	}
    }
    
    /**
     * Computes the acceptance time differences using the pareto curve and
     * change capacities.
     * @param time Start of a qtrhour epoch when one wants these time diffs.
     *   Units: seconds.
     * @return Array of minimum times for acceptance of the next arr or dep
     *   flight.  Units: seconds.
     */
    public int[] computeAcceptTimeDiffsFromPareto(int time){
    	int[] minTimeDiff = new int[2];
    	
    	// Compute qtr hour acceptance rates from pareto
    	int[] rates = null;
    	if((rates = computeAcceptRatesPerQtrhour(time)) == null)return null;
    	
    	// Convert these max number of flights that can be accepted
    	// in the next qtrhour to a min time between flights.
    	for(int i=0;i<rates.length;i++){
    		if(rates[i] <= 0.){
    			minTimeDiff[i] = Integer.MAX_VALUE;
    		}
    		else {
    			// Round rates to nearest integer.
    			minTimeDiff[i] = 
    				qtrhourInSecs/rates[i];
    		}
    	}
    	return minTimeDiff;   	
    }
    
    /**
     * Computes the acceptance time differences using from the called rates.
     * If the calledRates array is null, returns 0 for all times.  If the time
     * is outside of the span for which have called rate data, then the
     * last rate values are used.  This is similar to what is done if the
     * acceptance rates are calculated using the weather condition changes.
     * <p> 
     * It is assumed that the values in the called rate array are the rates
     * per quarter hour epoch.
     * @param time Start of a qtrhour epoch when one wants these time diffs.
     *   Units: seconds.
     * @return Array of minimum time differences for landing or takeoff, i.e., 
     *   for acceptance of arr or dep flights.
     */
    public int[] computeAcceptTimeDiffsFromCalledRates(int time){
    	int[] minTimeDiff = new int[2];

    	// Determine which qtrhour epoch this is relative to the start time.
    	int epoch = (time - startTime)/qtrhourInSecs;
    	if(calledRates == null){
    		for(int i=0;i<minTimeDiff.length;i++){
    			minTimeDiff[i] = 0;	
    		}
    		return minTimeDiff;
    	}
    	
    	// If the epoch is after for which have data, use the last data.
    	int epochOrig = epoch;
    	if(epoch > calledRates.length -1)epoch = calledRates.length-1;	
    	
    	for(int i=0;i<calledRates[epoch].length;i++){
    		if(calledRates[epoch][i] > 0){
    			// If the called rates are non-zero.
    			minTimeDiff[i] = qtrhourInSecs/calledRates[epoch][i];
    		} else if(epochOrig < calledRates.length-1){
    			// If the rates are zero, but the original epoch is before
    			// the last epoch for which one has data, then set the
    			// min time diff to eff inf; hopefully a future epoch will
    			// have a non-zero called rate.
    			minTimeDiff[i] = Integer.MAX_VALUE;
    		}else {
    			// Rates are zero and have run out of capacity data. In 
    			// this case, set the min time diff to 1 week to show that
    			// this flight had trouble, yet the sim completed.
    			minTimeDiff[i] = 7*24*4*qtrhourInSecs;
    		}
    	}
    	return minTimeDiff;  	
    }
    
    /**
     * Computes the qtrhour acceptance rates of the sim using Pareto curves.
     * Right now the algorithm below is presumably just what NASPAC and 
     * Bonn's JNet do.  
     * Namely, they make a guess for flights that will want to arrive and 
     * depart in the next 15 minutes, calling them N_arr and N_dep.  Can 
     * represent the point(N_dep,N_arr) in the Dep-Arr (x-y) plane.  A ray 
     * from the origin through this point will cross the D-A pareto curve.
     * The crossing point gives the acceptance rates for this quarter hour.  
     * As John Gulding and Gerald Shapiro have explained to me, this is a rough 
     * approximation as many airports treat arrivals more favorably than
     * departures because, e.g., it's easy to hold planes on the ground as
     * opposed to in the air.   On the other hand, such behavior depends
     * on ability of the airport to hold planes on the ground and so can
     * vary quite a bit from airport to airport. 
     * @param time Present sim time.
     * @return 2D Array of acceptance rates for this time.
     */
    public int[] computeAcceptRatesPerQtrhour(int time){
    	int[] rates = new int[2];
    	
    	// If the change capacity, pareto curve or estOnNOffTimes array is null,
    	// then the airport capacities are not being modeled, so set the acceptance
    	// rates to effectively infinity.
    	if(changeCapacity == null || paretoCurve == null 
    		|| estOnNOffTimes == null){
    		for(int i=0;i<rates.length;i++){
    			rates[i] = Integer.MAX_VALUE;
    		}
    		return rates;
    	}
    	// If it isn't then need to compute the acceptance rates.

    	// Get most current weather condition.
    	int mc = computeCurrentWeatherCondition(time);
    	
    	// Get pareto curve using airport condition.
    	double[][] pareto = paretoCurve[mc];
    	
    	// Get number of planes that will want to depart or land in the next
    	// quarter hour epoch.
    	int[] num = computeNumWantDepNArrNextQtrhour(time,estOnNOffTimes);
    	
    	// With the number of deps and arrs in the next 15 minutes, and
    	// the appropriate airport pareto curve, determine the best
    	// acceptance rates.
    	double[] ratesD = null;
    	ratesD = computeOptimalAcceptRatesPerQtrhour(
    		num[Nas.Ad.DEP.ordinal()],num[Nas.Ad.ARR.ordinal()],pareto);
    	// Use simple algorithm as suggested by Gerald.
    	//ratesD = computeOptimalAcceptRatesPerQtrhourSimple(num[Nas.Ad.DEP.ordinal()],
    	//		num[Nas.Ad.ARR.ordinal()],pareto);
    	// Convert to integers;
    	for(int i=0;i<rates.length;i++){
    		rates[i] = (int)(ratesD[i] + 0.5);
    	}
    	return rates;
    }
    
    /**
     * Computes the qtrhour acceptance rates of the sim using Pareto curves.
     * Right now the algorithm below is presumably just what NASPAC and 
     * Bonn's JNet do.  
     * Namely, they make a guess for flights that will want to arrive and 
     * depart in the next 15 minutes, calling them N_arr and N_dep.  Can 
     * represent the point(N_dep,N_arr) in the Dep-Arr (x-y) plane.  A ray 
     * from the origin through this point will cross the D-A pareto curve.
     * The crossing point gives the acceptance rates for this quarter hour.  
     * As John Gulding and Gerald Shapiro have explained to me, this is a rough 
     * approximation as many airports treat arrivals more favorably than
     * departures because, e.g., it's easy to hold planes on the ground as
     * opposed to in the air.   On the other hand, such behavior depends
     * on ability of the airport to hold planes on the ground and so can
     * vary quite a bit from airport to airport. 
     * @param time Present sim time.
     * @return 2D Array of acceptance rates for this time.
     */
    public int[] computeAcceptRatesPerQtrhourBogus(int time){
    	int[] rates = new int[2];
    	
    	// If the change capacity, pareto curve or estOnNOffTimes array is null,
    	// then the airport capacities are not being modeled, so set the acceptance
    	// rates to effectively infinity.
    	if(changeCapacity == null || paretoCurve == null 
    		|| estOnNOffTimes == null){
    		for(int i=0;i<rates.length;i++){
    			rates[i] = Integer.MAX_VALUE;
    		}
    		return rates;
    	}
    	// If it isn't then need to compute the acceptance rates.

    	// Get most current weather condition.
    	int mc = computeCurrentWeatherCondition(time);
    	
    	// Get pareto curve using airport condition.
    	double[][] pareto = paretoCurve[mc];
    	
    	// Get number of planes that will want to depart or land in the next
    	// quarter hour epoch.
    	int[] num = computeNumWantDepNArrNextQtrhour(time,estOnNOffTimes);
    	
    	// With the number of deps and arrs in the next 15 minutes, and
    	// the appropriate airport pareto curve, determine the best
    	// acceptance rates.
    	double[] ratesD = null;
    	ratesD = computeOptimalAcceptRatesPerQtrhour(
    		num[Nas.Ad.DEP.ordinal()],num[Nas.Ad.ARR.ordinal()],pareto);
    	
    	// Normalize by called rates if available.
    	if(calledRates != null){
    		int epoch = (time - startTime)/qtrhourInSecs;
    		int[] ratesCalled = null;
    		if(epoch < calledRates.length)ratesCalled = calledRates[epoch];
    		else ratesCalled = calledRates[calledRates.length-1];
    		
    		int sumC = ratesCalled[0] + ratesCalled[1];
    		double sumD = ratesD[0] + ratesD[1];
    		for(int i=0;i<ratesD.length;i++){
    			ratesD[i] = sumC/sumD*ratesD[i];
    		}
    	}
    	
    	// Convert to integers;
    	for(int i=0;i<rates.length;i++){
    		rates[i] = (int)(ratesD[i] + 0.5);
    	}
    	return rates;
    }
    
    //-----------------------------------------------------------------------
    // Methods that are in principle private, but are left public for testing.
    //----------------------------------------------------------------------- 
    /**
     * Computes the most current weather condition.  Uses the
     * changeCapacity array and the marker lastIndxChangeUsed.  The
     * latter marker is updated upon exit.  Thus, this method is NOT re-entrant.
     * It assumes that the changeCapacity array has an entry with a time
     * that is before the current sim time.  If not, it takes the first
     * entry of the change capacity for the current one.
     * @param time Simulation time as an integer.
     * @return Meteorological condition as an integer.  The mapping to
     *   the name is given by the {@link Nas.Mc} enum.
     */
    public int computeCurrentWeatherCondition(int time){
	    boolean success = false;
	    int indx = 0;
		//int indx = lastIndxChangeUsed;   // MC of last current entry.
		int mc = changeCapacity[indx][1];// Default in case no entry is before the
			                             // current time.
		while(!success && indx<changeCapacity.length){
			if(changeCapacity[indx][0] <= time){
				mc = changeCapacity[indx][1];
				//lastIndxChangeUsed = indx;
				indx++;
			} else success = true;
		}
		return mc;
    }
    /**
     * Estimates the number of flights that want to depart or land in the
     * next quarter hour from the present time.  It is assumed that the
     * class has an array of estimated runway on and off times for all flights
     * coming into this airport during the simulation.  Further, it is 
     * assumed that the estimated times are in ascending order in the array.
     * @param time Current time. Units: seconds.
     * @param estOOTimes Array of estimated runway on and off times for all flights
     *   due to arrive at or depart from this airport in the simulation.
     * @return array of number of flights that want to depart or arrive in the
     *   next quarter hour epoch.  Is a 2-D array with the elements defined
     *   by the {@link Nas.Ad} enum.
     */
    public static int[] computeNumWantDepNArrNextQtrhour(int time, 
    	int[][] estOOTimes){
    	int qtrhourSecs = 15*60;
		int[] num = {0,0};
		int timeEnd = time + qtrhourSecs;
	
		// Arrivals
		for(int indx=0;indx<estOOTimes[Nas.Ad.ARR.ordinal()].length;indx++){
			if(estOOTimes[Nas.Ad.ARR.ordinal()][indx] >= timeEnd)break;
			if(estOOTimes[Nas.Ad.ARR.ordinal()][indx] >= time){
				num[Nas.Ad.ARR.ordinal()]++;
			}
		}
		// Departures.
		for(int indx=0;indx<estOOTimes[Nas.Ad.DEP.ordinal()].length;indx++){
			if(estOOTimes[Nas.Ad.DEP.ordinal()][indx] >= timeEnd)break;
			if(estOOTimes[Nas.Ad.DEP.ordinal()][indx] >= time){
				num[Nas.Ad.DEP.ordinal()]++;
			}
		}
		return num;
    }
    
    /**
     * Computes the optimal qtrhour acceptance rates knowing the predicted number
     * of planes wanting to depart and land in the next quarter hour.  It is a
     * straight implementation of the "optimal" number obtained by finding 
     * the point on the curve at which the ray of (numDep,numArr) intersects.
     * However, an exception is made if an endpoint of the curve is chosen.  If so,
     * the next point is chosen instead.
     * <p>
     * It is assumed that the input pareto curves have rates in terms of number
     * per qtr hour.
     * @param numDep Number of planes predicted to depart in the next 
     *   quarter hour.
     * @param numArr Number of planes predicted to arrive in the next
     *   quarter hour.
     * @param pareto Pareto curve for this time period.
     * @return Qtrhour rates as an array. First element is dep and second is arr.
     *   or <code>null</code> if the method fails for some reason.
     */
    public static double[] computeOptimalAcceptRatesPerQtrhour(
    		int numDep, int numArr, double[][] pareto){
    	double[] rates = new double[2];
    	
    	// Special cases.
    	if(numArr == 0 || numDep == 0){
    		if(numDep != 0){
    			for(int i=0;i<rates.length;i++){
    				rates[i]= pareto[pareto.length-1][i];
    			}
    			return rates;
    		}
    		else if(numArr != 0){
    			for(int i=0;i<rates.length;i++){
    				rates[i]= pareto[0][i];
    			}
    			return rates;
    		}
    		else {
    			// Both are zero.  Then it doesn't matter what the value
    			// is so just set it to be the medium.
    			numArr = 1;
    			numDep = 1;
    		}
    	}
    	double nArrDepSlope = (double)numArr/(double)numDep;
    	
    	// Get pareto curve point slopes.
    	double[] slope = new double[pareto.length];
    	for(int i=0;i<slope.length;i++){
    		if(pareto[i][Nas.Ad.DEP.ordinal()] < Constants.EPS){
    			slope[i] = Double.MAX_VALUE;
    		} else {
    			slope[i] = pareto[i][Nas.Ad.ARR.ordinal()]/
    				pareto[i][Nas.Ad.DEP.ordinal()];// y over x, Arr over Dep.
    		}
    	}
    	// Find the slope values that bracket the N_arr/N_dep slope.
    	// Assumes that the pareto points are ordered w/r to x, i.e., dep axis.
    	int indx = 1;// skip first x=0 (D=0) element as it has already been
    		         // covered above (numDep = 0).
    	boolean success = false;
    	int iMin=0,iMax=0;
    	while(!success && indx < slope.length){
    		if(slope[indx] < nArrDepSlope){
    			iMin = indx-1;
    			iMax = indx;
    			success = true;
    		}
    		indx++;
    	}
    	
    	//--------------------------------------------------------------------------
    	// Special cases.
    	//--------------------------------------------------------------------------
    	// Ray crosses an end segment and pareto is almost completely horizontal
    	// or vertical there.
    	if(iMin == 0 && abs(pareto[iMin][Nas.Ad.ARR.ordinal()]- 
    		pareto[iMax][Nas.Ad.ARR.ordinal()]) < 0.5){
    		// End segment near D=0 is almost horizonal.
    		// Take iMax values as they give most departures.
    		for(int i=0;i<pareto[iMax].length;i++){
    			rates[i] = pareto[iMax][i];
    		}
    		return rates;
    	} else if(iMax == pareto.length - 1 && 
    		abs(pareto[iMin][Nas.Ad.DEP.ordinal()] - 
    			pareto[iMax][Nas.Ad.DEP.ordinal()]) < 0.5){
    		// End segment near D=D_max is almost vertical.
    		// Take iMin values as they give most arrivals.
    		for(int i=0;i<pareto[iMin].length;i++){
    			rates[i] = pareto[iMin][i];
    		}
    		return rates;
    	}
    	
    	//--------------------------------------------------------------------------
    	// Usual case:
    	//--------------------------------------------------------------------------
    	// Find the point on the line between the two pareto points that
    	// bracket the arr/dep ray that has the same slope as the arr/dep ray.
    	// Find line between the two points. y = a*x + b.
    	// a = (y2-y1)/(x2-x1).  
    	double aa = (pareto[iMax][Nas.Ad.ARR.ordinal()] - 
    				 pareto[iMin][Nas.Ad.ARR.ordinal()])/
    			    (pareto[iMax][Nas.Ad.DEP.ordinal()] - 
    			     pareto[iMin][Nas.Ad.DEP.ordinal()]);
    	double bb = pareto[iMax][1] - aa*pareto[iMax][0];
    	int nSteps = 10;
    	double delx = (pareto[iMax][Nas.Ad.DEP.ordinal()] - 
    				   pareto[iMin][Nas.Ad.DEP.ordinal()])/nSteps;
    	double xx = pareto[iMin][Nas.Ad.DEP.ordinal()], yy = 0.;
    	success = false;
    	for(int i=0;i<nSteps;i++){
    		yy = aa*xx + bb;
    		if(yy/xx < nArrDepSlope){
    			rates[Nas.Ad.DEP.ordinal()] = xx;
    			rates[Nas.Ad.ARR.ordinal()] = yy;
    			success = true;
    			break;
    		}
    		xx += delx;	
    	}
    	return rates; 	 
    }
    
    /**
     * Computes the optimal acceptance rates knowing the predicted number of
     * planes wanting to depart and land in the next quarter hour.  Does
     * a simple implementation to find "optimal" number of arrivals and
     * departures.  This is similar (identical?) to what NASPAC does.  Here,
     * the arr/dep slope is computed.  Then the slopes of each pareto point
     * is computed.  The slope that is closest to the computed one, but 
     * _larger_ is taken to be the optimal.
     * <p>
     * THIS METHOD NEEDS WORK!!!!
     * @param numDep Number of planes predicted to depart in the next 
     *   quarter hour.
     * @param numArr Number of planes predicted to arrive in the next
     *   quarter hour.
     * @param pareto Pareto curve for this time period.
     * @return Rates as an array. First element is dep and second is arr.
     *   or <code>null</code> if the method fails for some reason.
     */
    public static double[] computeOptimalAcceptRatesPerQtrhourSimple(
    		int numDep, int numArr, double[][] pareto){
    	double[] rates = new double[2];
    	
    	// Special cases.
    	if(numArr == 0 || numDep == 0){
    		if(numDep != 0){
    			// Next to last point unless the pareto curve has
    			// only two points.
    			for(int i=0;i<rates.length;i++){
    				if(pareto.length > 2)rates[i]= pareto[pareto.length-2][i];
    				else rates[i] = pareto[pareto.length-1][i];
    			}
    			return rates;
    		}
    		else if(numArr != 0){
    			// Next to first point.
    			for(int i=0;i<rates.length;i++){
    				rates[i]= pareto[1][i];
    			}
    			return rates;
    		}
    		else {
    			// Both are zero.  Then it doesn't matter what the value
    			// is so just set it to be the medium.
    			numArr = 1;
    			numDep = 1;
    		}
    	}
    	double nArrDepSlope = (double)numArr/(double)numDep;
    	
    	// Get pareto curve point slopes.
    	double[] slope = new double[pareto.length];
    	for(int i=0;i<slope.length;i++){
    		if(pareto[i][Nas.Ad.DEP.ordinal()] < Constants.EPS){
    			slope[i] = Double.MAX_VALUE;
    		} else {
    			slope[i] = pareto[i][Nas.Ad.ARR.ordinal()]/
    				pareto[i][Nas.Ad.DEP.ordinal()];// y over x, Arr over Dep.
    		}
    	}
    	// Find the slope values that bracket the N_arr/N_dep slope.
    	// Assumes that the pareto points are ordered w/r to x, i.e., dep axis.
    	int indx = 1;// skip first x=0 (D=0) element as it has already been
    		         // covered above (numDep = 0).
    	boolean success = false;
    	int iMin=0,iMax=0;
    	while(!success && indx < slope.length){
    		if(slope[indx] < nArrDepSlope){
    			iMin = indx-1;
    			iMax = indx;
    			success = true;
    		}
    		indx++;
    	}
    	
    	//--------------------------------------------------------------------------
    	// Usual case.
    	//--------------------------------------------------------------------------
    	// Find the point on the pareto that is closest to the arr/dep slope
    	// yet larger.  However, if iMin = 0, then choose i = 1.
    	if(iMin == 0){
    		for(int i=0;i<pareto[1].length;i++){
    			rates[i] = pareto[1][i];
    		}
    	}else {
    		for(int i=0;i<pareto[iMin].length;i++){
    			rates[i] = pareto[iMin][i];
    		}
    	}
    	return rates;
    }
    
    /**
     * Computes the optimal qtrhour acceptance rates knowing the predicted number
     * of planes wanting to depart and land in the next quarter hour.  It is a
     * straight implementation of the "optimal" number obtained by finding 
     * the point on the curve at which the ray of (numDep,numArr) intersects.
     * This algorithm will probably need improvement later.
     * <p>
     * It is assumed that the input pareto curves have rates in terms of number
     * per qtr hour.
     * @param numDep Number of planes predicted to depart in the next 
     *   quarter hour.
     * @param numArr Number of planes predicted to arrive in the next
     *   quarter hour.
     * @param pareto Pareto curve for this time period.
     * @return Qtrhour rates as an array. First element is dep and second is arr.
     *   or <code>null</code> if the method fails for some reason.
     */
 /*
    public static double[] computeOptimalAcceptRatesPerQtrhour(
    		int numDep, int numArr, double[][] pareto){
    	double[] rates = new double[2];
    	
    	// Special cases.
    	if(numArr == 0 || numDep == 0){
    		if(numDep != 0){
    			for(int i=0;i<rates.length;i++){
    				rates[i]= pareto[pareto.length-1][i];
    			}
    			return rates;
    		}
    		else if(numArr != 0){
    			for(int i=0;i<rates.length;i++){
    				rates[i]= pareto[0][i];
    			}
    			return rates;
    		}
    		else {
    			// Both are zero.  Then it doesn't matter what the value
    			// is so just set it to be the medium.
    			numArr = 1;
    			numDep = 1;
    		}
    	}
    	double nArrDepSlope = (double)numArr/(double)numDep;
    	
    	// Get pareto curve point slopes.
    	double[] slope = new double[pareto.length];
    	for(int i=0;i<slope.length;i++){
    		if(pareto[i][Nas.Ad.DEP.ordinal()] < Constants.EPS){
    			slope[i] = Double.MAX_VALUE;
    		} else {
    			slope[i] = pareto[i][Nas.Ad.ARR.ordinal()]/
    				pareto[i][Nas.Ad.DEP.ordinal()];// y over x, Arr over Dep.
    		}
    	}
    	// Find the slope values that bracket the N_arr/N_dep slope.
    	// Assumes that the pareto points are ordered w/r to x, i.e., dep axis.
    	int indx = 1;// skip first x=0 (D=0) element as it has already been
    		         // covered above (numDep = 0).
    	boolean success = false;
    	int iMin=0,iMax=0;
    	while(!success && indx < slope.length){
    		if(slope[indx] < nArrDepSlope){
    			iMin = indx-1;
    			iMax = indx;
    			success = true;
    		}
    		indx++;
    	}
    	
    	//--------------------------------------------------------------------------
    	// Usual case:
    	//--------------------------------------------------------------------------
    	// Find the point on the line between the two pareto points that
    	// bracket the arr/dep ray that has the same slope as the arr/dep ray.
    	// Find line between the two points. y = a*x + b.
    	// a = (y2-y1)/(x2-x1).  
    	double aa = (pareto[iMax][Nas.Ad.ARR.ordinal()] - 
    				 pareto[iMin][Nas.Ad.ARR.ordinal()])/
    			    (pareto[iMax][Nas.Ad.DEP.ordinal()] - 
    			     pareto[iMin][Nas.Ad.DEP.ordinal()]);
    	double bb = pareto[iMax][1] - aa*pareto[iMax][0];
    	int nSteps = 10;
    	double delx = (pareto[iMax][Nas.Ad.DEP.ordinal()] - 
    				   pareto[iMin][Nas.Ad.DEP.ordinal()])/nSteps;
    	double xx = pareto[iMin][Nas.Ad.DEP.ordinal()], yy = 0.;
    	success = false;
    	for(int i=0;i<nSteps;i++){
    		yy = aa*xx + bb;
    		if(yy/xx < nArrDepSlope){
    			rates[Nas.Ad.DEP.ordinal()] = xx;
    			rates[Nas.Ad.ARR.ordinal()] = yy;
    			success = true;
    			break;
    		}
    		xx += delx;	
    	}
    	return rates; 	 
    }
*/    
    //-----------------------------------------------------------
    // Some getters used for testing.
    //-----------------------------------------------------------
    /**
     * @return airport as an index.
     */
    public int getAirport(){return airport;}
    /**
     * @return Pareto curve array.
     */
    public double[][][] getParetoCurves(){return paretoCurve;}
    /**
     * @return Change capacity array.
     */
    public int[][] getChangeCapacities(){return changeCapacity;}
    /**
     * @return lastIndxChangeUsed index.
     */
    public int getLastIndxChangeUsed(){return lastIndxChangeUsed;} 
    /**
     * @return Array of estimated runway on and off times.  Units: seconds.
     */
    public int[][] getEstOnNOffTimes(){return estOnNOffTimes;}
    /**
     * @return Array of qtr hour called rates.  Units: number/qtrhour.
     */
    public int[][] getCalledRates(){return calledRates;}
    /**
     * @return the computeRates flag.  False if using called rates; true
     *   if are computing them from the change capacities and pareto curve. 
     */
    public boolean getComputeRatesFlag(){return computeRates;}
    /**
     * @param type Type of measure.  Departure or arrival.
     * @return Measure used to order flights in queue.
     */
    public IFlight.Param getMeasure(Nas.Ad type){
    	return measures[type.ordinal()];
    }
    
    /**
     * Takes AAR and ADR for a quarter hour epoch and spreads them over
     * all sim timestep bins for that epoch.  Since the number of timesteps
     * may not be an integral value of the AAR and ADR rates possibly, 
     * attempts to spread out evenly the values.  Nothing fancy here. 
     * This could be better.
     * @param rates Acceptance rates for the total epoch.  AAR and ADR
     *   for a quater hour. [0] = ADR, [1] = AAR.
     * @param numTs Number of sim timesteps in an epoch.
     * @return array of arrival and departure acceptance rates for each
     *   sim timestep.  Note that they are integers.
     */
 /*
    public static int[][] computeAcceptRatesPerTimestep(double[] rates,
			int numTs){
    	int[][] aRates = new int[numTs][2];
    	double ar = 0.;
    	int arInt = 0;
    	double arRes = 0.;
    	int[] flucs = new int[numTs]; // Stores fluctuations about the average.
    	
    	for(int i=0;i<2;i++){
    		for(int j=0;j<flucs.length;j++)flucs[j] = 0;
    		
    		ar = rates[i]/(double)numTs;
    		arInt = (int)ar;
    	    arRes = ar - arInt;
    	    // Cases: arRes <= 0.5 and > 0.5.
    	    // If arRes <= 0.5, then do nothing; if > 0.5, then
    	    // change arRes to 1 - arRes, compute the add array and
    	    // then take the complement.
    	    boolean flip = false;
    	    if(arRes > 0.5){
    	    	arRes = 1. - arRes;
    	    	flip = true;
    	    }
  
	    	// Then less than half or half of the bins will need an
	    	// extra 1 above the average.
	    	
	    	// First, populate bins with baseline rates.
	    	for(int j=0;j<aRates.length;j++){
	    		aRates[j][i] = arInt;
	    	}
	    	
	    	// Figure out how many bins to skip on average.  
	    	// Will add a 1 every nSkip bins, but every nSkipExc times,
	    	// will add a 1 at the nSkip+1 bin.  Say, nSkipDub was 2.25,
	    	// then would add a 1 every 2 bins except the fourth time when
	    	// would add at the next 2+1 = 3 bin.
	    	// nSkip[] = {2 2 2 3 2 2 2 3 2 2 2 3}, etc.
	    	double nSkipDub = 1./arRes;
	    	int nSkipBase = (int)(nSkipDub+1.e-6); // baseline number to skip.
	    	double nSkipRes = nSkipDub - nSkipBase;
	    	int nSkipExc = 0;
	    	int[] nSkip = null;
	    	if(nSkipRes <= 0.5){
	    		nSkipExc = (int)(1./nSkipRes + 1.e-6); 
	    		if(nSkipExc > 15*60) nSkipExc = 15*60+1; // prevent silly large arrays.
		    	if(nSkipExc > 0){
			    	nSkip = new int[nSkipExc];
			    	for(int j=0;j<nSkip.length-1;j++){
			    		nSkip[j] = nSkipBase;
			    	}
			    	nSkip[nSkip.length-1]=nSkipBase+1;
		    	} else {
		    		nSkip = new int[1];
		    		nSkip[0] = nSkipBase;
		    	}
	    	}else {
	    		nSkipExc = (int)(1./(1.-nSkipRes) + 1.e-6); 
	    		if(nSkipExc > 15*60)nSkipExc = 15*60+1;// prevent silly large arrays.
		    	if(nSkipExc > 0){
			    	nSkip = new int[nSkipExc];
			    	for(int j=0;j<nSkip.length-1;j++){
			    		nSkip[j] = nSkipBase+1;
			    	}
			    	nSkip[nSkip.length-1]=nSkipBase;
		    	} else {
		    		nSkip = new int[1];
		    		nSkip[0] = nSkipBase;
		    	}	
	    	}
	    	
	    	if(nSkip[0] <= numTs){
	    		int cnt = -1;
    	    	for(int j=0;j<numTs;j+=nSkip[cnt]){
    	    		flucs[j]++;
    	    		cnt++;
    	    		if(cnt >= nSkip.length)cnt=0;
    	    	}
	    	} 
	    	
	    	// Now add fluctuations to aRates.  If flip = false, then just
	    	// add.  If not, need to take complement of fluctuations.
	    	if(!flip){
	    		for(int j=0;j<numTs;j++){
	    			aRates[j][i] += flucs[j];
	    		}
	    	} else {
	    		for(int j=0;j<numTs;j++){
	    			if(flucs[j] == 0){
	    				aRates[j][i]++; 
	    			}
	    		}
	    		// Shift all rates down 1.
	    		int[] aRatesOld = new int[numTs];
	    		for(int j=0;j<numTs;j++)aRatesOld[j] = aRates[j][i];
	    		aRates[numTs-1][i] = aRatesOld[0];
	    		for(int j=0;j<numTs-1;j++)aRates[j][i] = aRatesOld[j+1];
	    	}	    
    	}
    	return aRates;
    	
    }
 */  
    /**
     * Takes all the flights that are to arrive or depart at this airport and 
     * creates an ordered array of their predicted runway on (arrival) and
     * off (departure) times.  Will be used to predict the number of arrivals 
     * and departures in a qtr hour epoch so as to determine the acceptance rates.  
     * <p>
     * This does not work yet !!!!!!!!!!!!!!
     * @param flightsDep Array of flights departing from this airport.
     * @param flightsArr Array of flights arriving at this airport.
     * @return Array of arrival times, ordered by time.
     */
 /*
    public int[][] computePredictedOnNOffTimes(IFlight[] flightsDep, 
    		IFlight[] flightsArr){
    	int[][] predictedOnNOffTimes = new int[2][];
    	predictedOnNOffTimes[Nas.Ad.DEP.ordinal()] = new int[flightsDep.length];
    	predictedOnNOffTimes[Nas.Ad.ARR.ordinal()] = new int[flightsArr.length];
    	
    	for(int i=0;i<predictedOnNOffTimes[Nas.Ad.DEP.ordinal()].length;i++){
    		predictedOnNOffTimes[Nas.Ad.DEP.ordinal()][i] = 
    			flightsDep[i].get(IFlight.Param.SCHED_OUT_TIME);
    	}
    	Arrays.sort(predictedOnNOffTimes[Nas.Ad.DEP.ordinal()]);
    	for(int i=0;i<predictedOnNOffTimes[Nas.Ad.ARR.ordinal()].length;i++){
    		predictedOnNOffTimes[Nas.Ad.ARR.ordinal()][i] = 
    			flightsDep[i].get(IFlight.Param.SCHED_IN_TIME);
    	}
    	Arrays.sort(predictedOnNOffTimes[Nas.Ad.ARR.ordinal()]);
    	
    	return predictedOnNOffTimes; 	
    }
*/
    /**
     * Creates events for all arriving flights associated with this runway.
     * These events will later be loaded into an event queue. These flights 
     * should be only flights that have no departing airport, i.e., they 
     * are flights to a sink.  This is an awkward method as it needs to
     * ask the Taxiway node for info, but whatever.
     * <p> 
     * This is not a part of the constructor because I want to keep the
     * event driven part of the simulation outside of the creation of the
     * flights and nodes.  It is normally called by the 
     * {@link ISimulation#initialize()} method.
     * @param flights Flights to load in queue.
     * @return Array of Runway events of arriving flights that have
     *   no departure airports.  If there are no flights to add, then the 
     *   method returns <code>null</code>.
     */
 /*
    public IEvent[] createEventsForArrivingFlights(IFlight[] flights){
    	// Do if some flights are present.
    	if(flights != null && flights[0] != null){
    		IEvent[] events = new IEvent[flights.length];
	    	for(int i=0;i<flights.length;i++){
	    		// These will all be arriving flights!
	    		int time = 0;
	    		int schedInTime = flights[i].get(IFlight.Param.SCHED_IN_TIME);
	    		int carrier = flights[i].get(IFlight.Param.CARRIER);
	    		int[][] taxiTimes = ((Taxiway)taxiwayNode).getNomTaxiTimes();
	    		if(taxiTimes != null && carrier >= 0){
	    			// if there is taxi time info for this airport and the
	    			// carrier exists, then get the taxi time. 
	    			// estimated arrival time is the sched in time minus the
	    			// taxi time.
	    			time = schedInTime - taxiTimes[carrier][Nas.Ad.ARR.ordinal()];
	    		}else time = schedInTime;
	    		events[i]  = 
	    			new RunwayEvent(this,flights[i],IEvent.Cmd.ARR,time);
	    	}
	    	return events;
    	} else return null;
    }
*/
    
}
