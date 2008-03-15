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

import javax.swing.*;
import javax.swing.tree.*;


/** This is the GUI end of the strucuted log.  It contains the JTree which displays
 * the information.  The model is used for an interface for this log.
 */
public class GUIVisualLogger extends javax.swing.JPanel
{
    
    //Attributes
    
    //NON-GUI
    private int thing = 0;
    private Thread adder;
    private GUIVisualLogModel theModel;
    
    //GUI COMPONENTS
    
    private JScrollPane visualLogScroller;
    private JTree theLogTree;
    
    
    //Constructor
    
    /** Creates a new VisualLogger component. */    
    public GUIVisualLogger(GUIVisualLogModel theModel)
    {
        this.theModel = theModel;
	initComponents();
	addANode();
    }
    
    //Access Methods
    
    /** Returns the VisualLogModel associated with this component.
     * @return the model for the structured log.
     */    
    public GUIVisualLogModel getTheModel()
    {
	return theModel;
    }
    
    //Update Methods
    
    /** Intended to create a root node. */    
    public void addANode()
    {
	
	DefaultMutableTreeNode rooter = (DefaultMutableTreeNode)theModel.getTheModel().getRoot();
	for (int i = 0; i < rooter.getChildCount(); i++)
	{
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode)rooter.getChildAt(i);
	    theLogTree.expandPath(new TreePath(node.getPath()));
	}
	
    }
    
    //Constructor helper methods
    
    private void initComponents()
    {
	
	visualLogScroller = new javax.swing.JScrollPane();
	{
	    //theModel = new GUIVisualLogModel();
	    theLogTree = new javax.swing.JTree(theModel.getTheModel());
	}
	visualLogScroller.setViewportView(theLogTree);
	
	setLayout(new java.awt.BorderLayout());
	add(visualLogScroller, java.awt.BorderLayout.CENTER);
	
    }
    
    
    
    
    
    
    
    
}
