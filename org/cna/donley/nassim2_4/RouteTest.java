package org.cna.donley.nassim2_4;

import java.util.List;
import java.util.ArrayList;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

/**
 * Test to validate the Route class.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: RouteTest.java 1 2009-11-03 00:00:00EST donley $
 */
public class RouteTest
{   
    /**
     * Test constructors and getters.
     */
    @Test
	public void testOne()
    {
    	// Create route as a list. Just use the Terminal class to create
    	// the nodes.
    	int nNodes = 6;
    	INode[] nodes = new INode[nNodes];
    	for(int i=0;i<nodes.length;i++){
    		nodes[i] = new Terminal(0,null,null);
    	}
 
    	List<INode> rList = new ArrayList<INode>();
    	for(int i=0;i<nodes.length;i++){
    		rList.add(nodes[i]);
    	}
    	IRoute route = new Route(rList);
    	assertTrue(route != null);
    	
    	// Test of getRoute().
    	List<INode> r2 = route.getNodes();
    	assertTrue(r2 != null);
    	for(int i=0;i<rList.size();i++){
    		assertTrue(r2.get(i) == rList.get(i));
    	}	
    	// Test of getNextNode, getLastNode and getNextNodeWithoutUpdate.
    	assertTrue(null == route.getLastNode());
		assertTrue(rList.get(0) == route.getNextNode());
    	for(int i=1;i<rList.size();i++){
    		assertTrue(rList.get(i-1) == route.getLastNode());
    		assertTrue(rList.get(i) == route.getNextNodeNoUpdatePtr());
    		assertTrue(rList.get(i) == route.getNextNodeNoUpdatePtr());
    		assertTrue(rList.get(i) == route.getNextNode());
    		assertFalse(rList.get(i) == route.getNextNodeNoUpdatePtr());
    	}
    	assertTrue(rList.get(rList.size()-1) == route.getLastNode());
    	assertTrue(null == route.getNextNode());
    	assertTrue(null == route.getNextNode());
    	
    	
    	// Test of addNode().
    	route = new Route(rList);
    	INode newNode = new Taxiway(0,null,null);
    	route.addNode(newNode);
    	r2 = route.getNodes();
    	// Should be the last node on the route.
    	assertTrue(newNode == r2.get(r2.size()-1));
    	
    }
	
}
