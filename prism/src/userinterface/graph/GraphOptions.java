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

package userinterface.graph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import userinterface.*;

/**
 * This class is to make the new chart package compatible with the old.
 */
public class GraphOptions extends JDialog
{
	private GraphOptionsPanel gop;
	/** Creates a new instance of MultiGraphOptions */
	public GraphOptions(GUIPlugin plugin, Graph theModel, JFrame gui, String title)
	{
		super(gui, title);
		
		gop = new GraphOptionsPanel(plugin, gui, theModel); 
		
		gop.setPreferredSize(new Dimension(400,650));
		
		this.getContentPane().add(gop);
		
		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		this.getContentPane().add(p, BorderLayout.SOUTH);
		
		this.getContentPane().setSize(400,650);
		
		JButton jb = new JButton("Close");
		
		jb.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				gop.stopEditors();
				setVisible(false);
			}
		}
		);
		
		jb.addFocusListener(new FocusListener()
		{
			/**
			 * Invoked when a component gains the keyboard focus.
			 */
			public void focusGained(FocusEvent e){}
			
			/**
			 * Invoked when a component loses the keyboard focus.
			 */
			public void focusLost(FocusEvent e)
			{ 
				//gop.stopEditors();
			}
			
		});
		
		p.add(jb);
		
		
		pack();
		setLocationRelativeTo(getParent()); // centre
		//show();
		setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
	}
	
}
