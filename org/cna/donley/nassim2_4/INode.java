package org.cna.donley.nassim2_4;

/**
 * An interface of all nodes in the NAS simulation.  The node types
 * as of now are: Terminal, Taxiway and Runway.  Contains info the
 * nodes, holds data and performs some node specific operations.  The
 * actual execution is left to an {@link IEvent} corresponding to the
 * node type.
 * <p>
 * All initialization of the nodes, except creating the NAS network is assumed
 * to be done in the implementing class constructors.  Creation of the
 * network for nodes is done with the {@link #setNeighbor(INode)}
 * method.
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: INode.java 1 2009-08-31 00:00:00EST $
 */

public interface INode
{ 
    /**
     * A way to pass a message to this node.
     * @param sender The sender of the message.
     * @param message The message from the sender.
     * @return <code>true</code> if successful; <code>false</code> if not.
     */
    public boolean receive(Object sender, Object message);
    
    /**
     * Sets a neighbor of this node.  Used when creating the NAS network
     * of nodes and edges.
     * @param n The neighboring element. The type of element is determined
     *   using the "instanceof" operator.
     */
    public void setNeighbor(INode n);
    
    /**
     * Gets the node neighbors as an array.
     * @return array of node neighbors.
     */
    public INode[] getNeighbors();
}
