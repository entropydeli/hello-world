package org.cna.donley.nassim2_4;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

/**
 * Test to validate the EventQueue class.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: EventQueueTest.java 1 2009-12-17 00:00:00EST donley $
 */
public class EventQueueTest
{
	/**
	 * test of constructor and getter and setters, and computeNextTime().
	 */
	@Test
	public void testOne(){
		
		EventQueue queue = new EventQueue();
		assertTrue(queue != null);
		
		int nEvents = 3;
		int[] times = {732, 450, 823};
		IEvent[] eves = new IEvent[nEvents];
		for(int i=0;i<eves.length;i++){
			eves[i] = new DummyEvent(null,null,null,times[i]);
			queue.add(eves[i]);
		}
		// clear() and size().
		queue.clear();
		assertTrue(queue.size() == 0);
		
		// non-empty queue
		for(int i=0;i<eves.length;i++){
			queue.add(eves[i]);
		}
    	// size()
    	assertTrue(queue.size() == nEvents);
    	// peek().  Top of queue.
    	assertTrue(queue.peek() == eves[1]);
    	// poll();
    	assertTrue(queue.poll() == eves[1]);
    	assertTrue(queue.poll() == eves[0]);
    	assertTrue(queue.poll() == eves[2]); 
    	assertTrue(queue.poll() == null);  
	}
}
