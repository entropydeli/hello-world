package org.cna.donley.nassim2_4;

import java.util.List;
import java.util.ArrayList;

/**
 * An implementation of the {@link IRoute} interface.  A route class stores 
 * route data for a single flight.  Has a bunch of getters and setters.
 * At present the route is accessed either by pulling out all nodes
 * or just one.  If just one, then it will be the "next" or "last" nodes.
 * In that manner, the nodes are accessed in sequence and that is
 * how the flight progresses from beginning to end.  There is no
 * mechanism at present for one to reset the "next" node, so once
 * you access it using {@link #getNextNode()} you change the
 * "next" node pointer forever. However, using {@link #getLastNode()}
 * does not change the "next" node pointer.
 * <p>
 * Any setting of the route is done in the constructor, though one
 * can add nodes to the end using the {@link #addNode(INode)} method.
 * <p>
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: Route.java 1 2009-11-03 00:00:00EST $
 */

public class Route implements IRoute
{ 
	/**
	 * Pointer to next node in list.  The flight should currently then be at
	 * currentNode = nextNode - 1.
	 */
	private int nextNode;
	/**
	 * List of nodes along route.
	 */
	private List<INode> routeList;
	/**
	 * Make default private.  Can always just enter in a null route
	 * in the constructor below and then add it in later.
	 */
	private Route(){};
	
	/**
	 * Constructor.  Creates the flight route and sets the "next" 
	 * node to the beginning of the list.  Will be a list of {@link INode}
	 * objects.  If the flight route needs to be modified one
     * needs to create a whole new route object using the constructor.
	 * @param routeList of flight as a list of {@link INode} objects.  Can
	 *   be <code>null</code>, but if so sets up an empty route list. This
	 *   is done so the other methods won't throw exceptions.
	 */
	public Route(List<INode> routeList){
		if(routeList == null) this.routeList = new ArrayList<INode>();
		else this.routeList = routeList;
    	nextNode = 0;
	}
	
    /**
     * Add a node to the route.  Will add it to the end.  Does not
     * change the current node pointer.
     * @param node
     */
    public void addNode(INode node){
    	this.routeList.add(node);	
    }
    
    /**
     * Gets the "next" node of the route, i.e., the next node after that
     * last one that has been accessed.  Once that is done the
     * pointer to the next node is set to the node after that.
     * @return Next node of route or <code>null</code> if route is
     *  done being accessed, i.e., the flight has ended and the next
     *  node pointer points to a non-existent node.
     */
    public INode getNextNode(){
    	if(nextNode >= routeList.size()) return null;
    	INode n = routeList.get(nextNode);
    	if(n != null){
    		nextNode++;
    	}
    	return n;
    }
    /**
     * Gets the "next" node of the route, i.e., the next node after that
     * last one that has been accessed. In contrast to {@link #getNextNode()},
     * however, it does not update the "next node" pointer.  Thus,
     * this method is reentrant.
     * @return Next node of route or <code>null</code> if route is
     *  done being accessed, i.e., the flight has ended and the next
     *  node pointer points to a non-existent node.
     */
    public INode getNextNodeNoUpdatePtr(){
    	if(nextNode >= routeList.size()) return null;
    	return routeList.get(nextNode);
    }
    /**
     * Gets the "last" node that has been accessed using the 
     * {@link #getNextNode()} method.  Does not update the pointer
     * to the next node.  Is used primarily for consistency checks,
     * e.g., the current node that is calling the flight to get the
     * route to get the next node the flight must go to is indeed
     * the current node as specified by the route.
     * @return Last node accessed from the route or <code>null</code>
     *   if there was no last node.
     */
    public INode getLastNode(){
    	if(nextNode < 1) return null;
    	else return routeList.get(nextNode-1); 	
    }
    
    /**
     * Retrieves the flight route as a list of nodes.
     * @return Flight route as a list.
     */
    public List<INode> getNodes(){
    	return routeList;
    }
    
}
