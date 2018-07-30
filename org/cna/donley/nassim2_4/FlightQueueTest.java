package org.cna.donley.nassim2_4;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

/**
 * Test to validate the FlightQueue class.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: FlightQueueTest.java 1 2009-12-08 00:00:00EST donley $
 */
public class FlightQueueTest
{
	/**
	 * test of constructor and getter and setters, and computeNextTime().
	 */
	@Test
	public void testOne(){
		
		IFlight.Param measure = IFlight.Param.ACT_OUT_TIME;
		int[] itinNum = {0,1,2};
    	int[] legNum = {1,2,1};
    	int[] outTime = {123,89,110};
    	IFlight[] fl = new IFlight[3];
    	int[] pars = new int[IFlight.Param.values().length];
    	for(int j=0;j<fl.length;j++){
	    	for(int i=0;i<pars.length;i++){
	    		if(i==IFlight.Param.ITIN_NUM.ordinal())pars[i]= itinNum[j];
	    		if(i==IFlight.Param.LEG_NUM.ordinal())pars[i] = legNum[j];
	    		if(i==IFlight.Param.ACT_OUT_TIME.ordinal())pars[i] = outTime[j];
	    		else pars[i] = i;
	    	}
	    	fl[j] = new Flight(pars);
	    	assertTrue(fl[j] != null);
    	}
    	
    	// Constructor as a priority queue.
    	IQueue<IFlight> queue = new FlightQueue(measure);
    	assertTrue(queue != null);	
    	// add()
    	for(IFlight f : fl){
    		queue.add(f);
    	}
    	// clear().
    	queue.clear();
    	// size()
    	assertTrue(queue.size() == 0);
    	for(IFlight f : fl){
    		queue.add(f);
    	}
    	// size()
    	assertTrue(queue.size() == 3);
    	// peek().  Top of queue.
    	assertTrue(queue.peek() == fl[1]);
    	// poll();
    	assertTrue(queue.poll() == fl[1]);
    	assertTrue(queue.poll() == fl[2]);
    	assertTrue(queue.poll() == fl[0]); 
    	assertTrue(queue.poll() == null);  
    	
    	// Constructor as a linked list.  Should return the flights
    	// in the order that they are added.
    	queue = new FlightQueue(null);
    	assertTrue(queue != null);	
    	// add()
    	for(IFlight f : fl){
    		queue.add(f);
    	}
    	// size()
    	assertTrue(queue.size() == 3);
    	// peek().  Top of queue.
    	assertTrue(queue.peek() == fl[0]);
    	// poll();
    	assertTrue(queue.poll() == fl[0]);
    	assertTrue(queue.poll() == fl[1]);
    	assertTrue(queue.poll() == fl[2]); 
    	assertTrue(queue.poll() == null);  
	}
}
