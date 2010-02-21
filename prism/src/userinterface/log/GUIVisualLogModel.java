//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package userinterface.log;

import java.util.Stack;
import java.util.StringTokenizer;
import javax.swing.tree.*;
import javax.swing.SwingUtilities;
import prism.PrismLog;

/** This is the model for the structured log it contains the following interface:
 * <ul>
 *    <li>Text can be added to the log using the println(int, String) method as
 * well as the print(int, String) method.  The int is the mode of the text.  The
 * five different modes are:
 *        <ul>
 *            <li>Normal
 *            <li>Timing
 *            <li>Memory
 *            <li>Error
 *            <li>Details
 *        </ul>
 *        and it is intended that the bitwise constants are used to set the mode.
 * There is also a newLine() method for a carriage return.
 *    <li>As the structured log is a tree, we need some way of creating children.
 * The methods for controlling this are:
 *        <ul>
 *            <li>indent()    - Increases the level by 1.
 *            <li>unindent()  - Decreases the level by 1.
 *            <li>setIndent(int i) - Sets the indent to a given level(assuming that
 * 0 <= i <= currentLevel+1.
 *        </ul>
 *    <li>The idea of there being different "types" of output to the log, is so
 * that the user can turn on or off different types of output.  This can be done
 * with the enableType(int i) and the disableType(int i) methods. It is intended
 * that the integer i is a bitwise value so say you want to enable Error's and
 * Memory, but disable Timing and Details you would type:
 *        <code>
 *              enableType(VisualLogModel.ERROR | VisualLogModel.MEMORY);
 *              disableType(VisualLogModel.DETAILS | VisualLogModel.TIMING);
 *        </code>
 * </ul>
 */
public class GUIVisualLogModel extends PrismLog
{
    
    //Attributes
    
    // Bitwise Types for Output
    /** bitwise constant */    
    public static final int NORMAL  = 1;
    /** bitwise constant */    
    public static final int TIMING  = 2;
    /** bitwise constant */    
    public static final int MEMORY  = 4;
    /** bitwise constant */    
    public static final int ERROR   = 8;
    /** bitwise constant */    
    public static final int DETAILS = 16;

    // Current types enabled for output
    private int enabled;

    //Root node, this is never modified externally, and is not meant to be visible
    private DefaultMutableTreeNode root;

    //Current node which is being worked on
    private DefaultMutableTreeNode current;

    //Buffer for String output
    private String buffer;

    //Stack for storing the ancestor nodes of the current indentation point
    private Stack theStack;

    //current indentation point
    private int currentIndent;

    //The Tree Model
    private DefaultTreeModel theModel;

    //Constructor
    
    /** Creates a new VisualLogModel setting the tree to an empty root. */    
    public GUIVisualLogModel()
    {
	root = new DefaultMutableTreeNode("Root");
	theModel = new DefaultTreeModel(root);
	currentIndent = 0;
	theStack = new Stack();
	theStack.push(root);
	buffer = "";
	current = null;
	enabled = NORMAL | TIMING | MEMORY | ERROR | DETAILS; // initially all types are enabled
    }
    
    /** Has the effect of increasing the indent by one, and all children are of the current node which is pushed onto stack*/
    public void indent()
    {
	try
	    {
		theStack.push(current);
		currentIndent++;
		current = null;
	    }
	catch(NullPointerException e) // caught if the indentation is illegal as no nodes have been added to current level
	    {
		System.err.println("Attempt to indent failed because no nodes on current level.");
	    }
    }
    
    /** Has the effect of decreasing the indent by one, all subsequent nodes are added to the grandparent of previous current node*/
    public void unIndent()
    {
	try
	    {
		if(currentIndent>0)
		    {
			//System.out.println(currentIndent);
			theStack.pop();
			currentIndent--;
			current = null;
		    }
		else
		    {
			//System.err.println("Cannot unIndent top level");
		    }
	    }
	catch(Exception e)
	    {
		//System.err.println(e.getMessage());
	    }
    }

    /** Sets the indent level quicker.  Assumption that 0 <= ind <= currentIndent + 1
     * @param ind the new indentation level
     */
    public void setIndent(int ind)
    {
	if(ind <= currentIndent+1)
	    {
	        if(ind == currentIndent+1) indent();
		else if(ind == currentIndent) return;
		else if(ind >= 0) //    0 >= ind > currentIndent
		    {
			for(int i = currentIndent; i >= ind+1; i--)
			    {
				unIndent();
			    }
		    }
	    }
    }

    /** Access method to return the current indent.
     * @return the current indent level.
     */
    public int getIndent()
    {
	return currentIndent;
    }

    /** Enables the type for output
     * @param type a bitwise integer representing which type/s should be enabled.
     */
    public void enableType(int type)
    {
	enabled = enabled|type;
    }

    /** Disabled the type for output
     * @param type a bitwise integer representing which type/s should be disabled.
     */
    public void disableType(int type)
    {
	enabled = enabled&(~type);
    }

    /** "Prints" the given string with the given type and starts on the next node
     * @param type bitwise type of the output
     * @param s the output string
     */
    public void println(int type, String s)
    {
	print(type, s);
	newLine();
    }

    /** "Prints" the given string with the given type but continues with the same node
     * @param type bitwise type of the output
     * @param s the output string
     */
    public void print(int type, String s)
    {
	if((enabled&type)!=0) buffer+=s;
    }

    /**A Carriage return*/
    public void newLine()
    {
	if(!buffer.equals(""))
	    {
		StringTokenizer tok = new StringTokenizer(buffer, "\n");
		int noOfTokens = tok.countTokens();
		for (int i = 0; i < noOfTokens; i++)
		    {
			String str = tok.nextToken();
			current = new DefaultMutableTreeNode(str);
			
			DefaultMutableTreeNode parent = (DefaultMutableTreeNode)theStack.peek();
		
			SwingUtilities.invokeLater(new AddLogEntry(current,parent));
		    }
	    }
	buffer = "";
	
    }
	
    /** Access method to return the DefaultTreeModel associated with this tree
     * @return the DefaultTreeModel associated with this log.
     */
    public DefaultTreeModel getTheModel()
    {
	return theModel;
    }

    // Methods required by PrismLog abstract class
    
    public boolean ready()
    {
        return true;
    }
    public long getFilePointer()
    {
        return -1;
    }
    
	public void flush()
	{
	}

    public void close()
    {
    }
    
    public void print(long l)
    {
        print(NORMAL, ""+l);
    }
    
    public void print(int i)
    {
        print(NORMAL, ""+i);
    }
    
    public void print(boolean b)
    {
        print(NORMAL, ""+b);
    }
    
    public void print(char c)
    {
        print(NORMAL, ""+c);
    }
    
    public void print(float f)
    {
        print(NORMAL, ""+f);
    }
    
    public void print(double d)
    {
        print(NORMAL, ""+d);
    }
    
    public void print(Object obj)
    {
        print(NORMAL, ""+obj);
    }
    
    public void print(String s)
    {
        print(NORMAL, s);
    }
    
    public void print(double d[])
    {
         print(NORMAL,""+d);
    }
    
    public void println()
    {
        println(NORMAL, "");
    }
        
    /** Because the entries have to be called from Threads, the invokeLater method is
     * used to add entries using this Thread.
     */    
    class AddLogEntry extends Thread
    {
	private DefaultMutableTreeNode node;
	private DefaultMutableTreeNode parentOfNode;
	/** Creates a new AddLogEntry.  The entry is described as a node and the nodes
	 * parent
	 * @param node the node the be added
	 * @param parentOfNode the parent of the node to be added
	 */	
	public AddLogEntry(DefaultMutableTreeNode node, DefaultMutableTreeNode parentOfNode) 
	{
	    this.node = node;
	    this.parentOfNode = parentOfNode;
	}
	/** Overridden superclass run() method for thread of execution to add nodes to the
	 * tree.
	 */	
	public void run()
	{
	    theModel.insertNodeInto(node, parentOfNode, parentOfNode.getChildCount());
	}
    }
	    
		

    
}
    
