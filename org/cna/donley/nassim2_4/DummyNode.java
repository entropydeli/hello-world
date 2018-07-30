package org.cna.donley.nassim2_4;

/**
 * An implementation of the interface {@link INode} as a dummy node. Used for
 * testing.
 * <p>
 * 
 * @author James Donley &lt;donleyj@cna.org&gt;
 * @version $Id: DummyNode.java 1 2009-12-17 00:00:00EST $
 */

public class DummyNode implements INode
{
 
    /**
     * Default constructor.
     */
    public DummyNode(){}
 
    //-----------------------------------------------------------------------
    // Legacy crap
    //-----------------------------------------------------------------------
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
     * Sets a neighbor of this node. Legacy crap.
     * @param n The neighboring element.
     */
    public void setNeighbor(INode n){};	
    /**
     * Gets the node neighbors as an array.  Legacy crap.
     * @return array of node neighbors.
     */
    public INode[] getNeighbors(){return null;}
    
}
