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

package userinterface.properties;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;

import java.awt.*;
import java.util.*;

@SuppressWarnings("serial")
public class ConstantPickerList extends JPanel implements Scrollable
{
	private ArrayList<ConstantLine> rows;
	private JPanel nextPanel;

	/** Creates a new instance of ConstantPickerList */
	public ConstantPickerList()
	{
		setLayout(new BorderLayout());
		nextPanel = new JPanel();
		nextPanel.setLayout(new BorderLayout());
		add(nextPanel, BorderLayout.CENTER);
		rows = new ArrayList<ConstantLine>();
	}

	public void addConstant(ConstantLine pl)
	{
		rows.add(pl);
		JPanel containPl = new JPanel();
		containPl.setLayout(new BorderLayout());
		containPl.setBorder(new CompoundBorder(pl.new BottomBorder(), new LineBorder(Color.white, 2)));
		containPl.add(pl, BorderLayout.CENTER);
		nextPanel.add(containPl, BorderLayout.NORTH);
		JPanel np = new JPanel();
		np.setLayout(new BorderLayout());
		nextPanel.add(np, BorderLayout.CENTER);
		nextPanel = np;
	}

	public Dimension getPreferredScrollableViewportSize()
	{
		return getPreferredSize();
	}

	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return 20;
	}

	public boolean getScrollableTracksViewportHeight()
	{
		return false;
	}

	public boolean getScrollableTracksViewportWidth()
	{
		return true;
	}

	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return 40;
	}

	public int getNumConstants()
	{
		return rows.size();
	}

	public ConstantLine getConstantLine(int i)
	{
		return rows.get(i);
	}

}
