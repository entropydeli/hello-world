package org.cna.donley.nassim2_4;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Class runs all test for the Nassim2_4 project.  Uses JUnit 4.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: AllTestsNassim2_4.java 2367 2009-11-02 00:00:00EST donley $
 */
@RunWith(value=Suite.class)
@Suite.SuiteClasses(value={SimElementsTest.class,FlightTest.class, 
		RunwayTest.class, TerminalTest.class, TaxiwayTest.class,
		FixTest.class,RouteTest.class,NasTest.class,RunwayEventTest.class,
		TerminalEventTest.class,TaxiwayEventTest.class,HoldEventTest.class,
		FixEventTest.class,NaspacBridgeTest.class,
		NasSimEventDrivenTest.class})

public class AllTestsNassim2_4 {
	
	private String testName = "AllTests for Nassim2_4.";
	/**
	 * There is no "main" for JUnit 4.  Assumes the IDE has built a runner for 
	 * JUnit.  If using via command line, then? 
	 */
	public AllTestsNassim2_4(){
		System.out.println("Running" + testName);
	}	
}
