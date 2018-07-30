package org.cna.donley.nassim2_4;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Test to validate the HoldEvent class.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: HoldEventTest.java 1 2009-09-08 00:00:00EST donley $
 */
public class HoldEventTest
{
	
	/**
	 * test of constructors, getters and setters.
	 */
	@Test
	public void testConstructorNGettersNSettersNProcessEvent(){
		// Make up some bogus events.
		int num = 20;
		int[] aprt = {23,11};
		INode[] node = new INode[2];
		IEvent[] events = new IEvent[num];
		
		// Flights.
		int[] calcOffTime = {520,310,414,289};  // calculated out time.
		IFlight[] fl = new IFlight[num];
		int[][] pars = new int[4][IFlight.Param.values().length];
		for(int i=0;i<pars.length;i++){
			pars[i][IFlight.Param.ITIN_NUM.ordinal()]     = 1;
			pars[i][IFlight.Param.LEG_NUM.ordinal()]      = 1;
			pars[i][IFlight.Param.CALC_OFF_TIME.ordinal()]= calcOffTime[i];
		}
    	for(int i=0;i<fl.length;i++){
    		if(i%2 == 0)fl[i] = new Flight(pars[0]);
    		if(i%3 == 0)fl[i] = new Flight(pars[1]);
    		if(i%4 == 0)fl[i] = new Flight(pars[2]);
    		else fl[i] = new Flight(pars[3]);	
    	}
		
    	IQueue<IEvent> eventQueue = new EventQueue();
		eventQueue.clear();
		
		// Create nodes,
		node[0] = new Runway(aprt[0],0,false,null,null,null,null);
		node[1] = new Taxiway(aprt[0],null,null);
		// Create events and add to queue.
		int time = 0;
		for(int i=0;i<events.length/2;i++){
			time = 10*i;
			if(i%2 == 0){ events[i] = 
				new RunwayEvent((Runway)node[0],fl[i],IEvent.Cmd.DEP,time);
			}
			else { events[i] = 
				new RunwayEvent((Runway)node[0],fl[i],IEvent.Cmd.ARR,time);
			}
			eventQueue.add(events[i]);
		}
		for(int i=events.length/2;i<events.length;i++){
			time = 20*i;
			if(i%2 == 0){ events[i] = 
				new TaxiwayEvent((Taxiway)node[1],fl[i],IEvent.Cmd.DEP,time);
			}
			else { events[i] = 
				new TaxiwayEvent((Taxiway)node[1],fl[i],IEvent.Cmd.ARR,time);
			}
			eventQueue.add(events[i]);
		}
		
		// Hold departing flights at airport 23 for 2 minutes.
		int timeStart = 16;
		int timeHold = 120;
		IFlight.Param crit = IFlight.Param.CALC_OFF_TIME;
		IEvent holdEvent = new HoldEvent(IEvent.Type.RUNWAY,node[0],
			IEvent.Cmd.HOLD_DEP,crit,timeStart,timeHold);
		
		// Getters and setters.
		assertTrue(holdEvent.getNode()==node[0]);
		assertTrue(holdEvent.getMessage() == IEvent.Cmd.HOLD_DEP);
		assertTrue(holdEvent.getType() == IEvent.Type.HOLD);
		assertTrue(((HoldEvent)holdEvent).getTypeToHold() == IEvent.Type.RUNWAY);
		assertTrue(holdEvent.getTime() == timeStart);
		holdEvent.setTime(8);
		assertTrue(holdEvent.getTime() == 8);
		assertTrue(holdEvent.getFlight() == null);
		holdEvent.setTime(timeStart);
		assertTrue(((HoldEvent)holdEvent).getTimeHold() == timeHold);

		assertTrue(((HoldEvent)holdEvent).getCriterion() == crit);
		
		// getFlight.
		assertTrue(null == holdEvent.getFlight());
		
		// compareTo.
		IEvent h2 = new HoldEvent(IEvent.Type.RUNWAY,node[0],
				IEvent.Cmd.HOLD_DEP,crit,timeStart+15,timeHold-10);
		assertTrue(holdEvent.compareTo(holdEvent)==0);
		assertTrue(holdEvent.compareTo(h2) == -1);
		assertTrue(h2.compareTo(holdEvent) == 1);
		
		// equals.  who cares?
		
		// Now check if all those events are indeed delayed.
		holdEvent.processEvent(eventQueue);
		assertTrue(eventQueue.size() == events.length);// All events are still there!
		
		List<IEvent> list = new ArrayList<IEvent>();
		int count = 0;
		IEvent eve = null;
		while((eve = eventQueue.poll()) != null){
			// Look at departing flights of the runway node.
			if(eve.getNode() == node[0] && 
				eve.getMessage() == IEvent.Cmd.DEP){
				// Should be no flights between the timeStart and timeHold times
				if(eve.getTime() >= timeStart){ 
					if(eve.getTime() < timeHold){
						assertFalse(true);
					}else {
						// then time must be changed.
						assertTrue(eve.getTime() == timeHold + count);
						list.add(eve);
						count++;
					}
				} else {
					// time has not changed.
					assertTrue(eve.getTime()%10 == 0);
				}
			} else {
				// Other nodes or arriving flight at the chosen runway node.
				// Time is not changed.
				assertTrue(eve.getTime()%10 == 0);
			}		
		}
		assertTrue(count == 4);
		
		// Now see if they are ordered properly, i.e, by their computed
		// off time.
		int tOld = -1;
		for(IEvent ev : list){
			assertTrue(ev.getFlight().get(crit) >= tOld);
			tOld = ev.getFlight().get(crit);
		}
				
		//---------------------------------------------------------------------
		// Case 2: Have a flight that is after the hold time, but close enough 
		// so that it must be moved back too.
		//---------------------------------------------------------------------
		// Here, change hold time to 79, so that that event at 80 will need
		// to be held too even though it's after the hold time.
		
		// Create events.  Same as before.
		time = 0;
		for(int i=0;i<events.length/2;i++){
			time = 10*i;
			if(i%2 == 0){ events[i] = 
				new RunwayEvent((Runway)node[0],fl[i],IEvent.Cmd.DEP,time);
			}
			else { events[i] = 
				new RunwayEvent((Runway)node[0],fl[i],IEvent.Cmd.ARR,time);
			}
			eventQueue.add(events[i]);
		}
		for(int i=events.length/2;i<events.length;i++){
			time = 20*i;
			if(i%2 == 0){ events[i] = 
				new TaxiwayEvent((Taxiway)node[1],fl[i],IEvent.Cmd.DEP,time);
			}
			else { events[i] = 
				new TaxiwayEvent((Taxiway)node[1],fl[i],IEvent.Cmd.ARR,time);
			}
			eventQueue.add(events[i]);
		}
		eventQueue.clear();
		for(IEvent ev : events){
			eventQueue.add(ev);
		}
		
		// Hold departing flights at airport 23 for 59 secs.
		timeStart = 16;
		timeHold = 59;
		holdEvent = new HoldEvent(IEvent.Type.RUNWAY,node[0],
			IEvent.Cmd.HOLD_DEP,crit,timeStart,timeHold);	
		
		// Now check if all those events are indeed delayed.
		holdEvent.processEvent(eventQueue);
		assertTrue(eventQueue.size() == events.length);// All events are still there!
		
		count = 0;
		int actCount = 3;
		list.clear();
		while((eve = eventQueue.poll()) != null){
			// Look at departing flights of the runway node.
			if(eve.getNode() == node[0] && 
				eve.getMessage() == IEvent.Cmd.DEP){
				// Should be no flights between the timeStart and timeHold times
				time = eve.getTime();
				if(time >= timeStart && time < timeHold+actCount){ 
					if(time < timeHold){
						assertFalse(true);
					}else {
						// then time must be changed.
						assertTrue(eve.getTime() == timeHold + count);
						list.add(eve);
						count++;
					}
				} else {
					// time has not changed.
					assertTrue(eve.getTime()%10 == 0);
				}
			} else {
				// Other nodes or arriving flight at the chosen runway node.
				// Time is not changed.
				assertTrue(eve.getTime()%10 == 0);
			}		
		}
		assertTrue(count == actCount);
		// Now see if they are ordered properly, i.e, by their computed
		// off time.
		tOld = -1;
		for(IEvent ev : list){
			assertTrue(ev.getFlight().get(crit) >= tOld);
			tOld = ev.getFlight().get(crit);
		}
	
	}
	/**
	 * Test of exception in constructor.  Will accept only
	 * messages that are arr or dep.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testIllArgExceptionConstructor() {
		IEvent te = new HoldEvent(null,null,IEvent.Cmd.BOGUS,
			IFlight.Param.CALC_ON_TIME,0,0);
	}
	
}
