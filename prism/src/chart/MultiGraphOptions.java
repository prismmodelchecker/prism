//==============================================================================
//
//	Copyright (c) 2002-2004, Andrew Hinton, Dave Parker
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

package chart;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * This class is to make the new chart package compatible with the old.
 * @author  ug60axh
 */
public class MultiGraphOptions extends JDialog
{
	private GraphOptionsPanel gop;
	/** Creates a new instance of MultiGraphOptions */
	public MultiGraphOptions(MultiGraphModel theModel, JFrame gui, String title)
	{
		super(gui, title);
		
		gop = new GraphOptionsPanel(gui, theModel); 
		
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
				hide();
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
		//show();
		setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
	}
	
}
