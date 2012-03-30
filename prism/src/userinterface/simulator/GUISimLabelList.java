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

package userinterface.simulator;

import java.awt.*;
import javax.swing.*;

import parser.ast.*;
import prism.PrismException;
import prism.PrismLangException;
import userinterface.properties.*;
import simulator.*;

/**
 * List of labels in the simulator GUI.
 */
public class GUISimLabelList extends JList
{
	// Default serial ID
	private static final long serialVersionUID = 1L;
	// Background colour of selected list items
	private static final Color background = new Color(202, 225, 255);
	// Pointers to simulator and GUI
	private GUISimulator sim;
	private SimulatorEngine engine;
	// The list of labels
	private DefaultListModel listModel;

	/**
	 * Create a new instance of GUISimLabelList
	 */
	public GUISimLabelList(GUISimulator sim)
	{
		this.sim = sim;
		this.engine = sim.getPrism().getSimulator();
		listModel = new DefaultListModel();
		setModel(listModel);
		setCellRenderer(new SimLabelRenderer());
	}

	/**
	 * Clear the list of labels.
	 */
	public void clearLabels()
	{
		listModel.clear();
	}

	/**
	 * Add a (model file) label to the list.
	 * Any required formulas/labels will be in the associated model, already in the simulator.
	 */
	public void addModelLabel(String name, Expression expr)
	{
		try {
			int index = engine.addLabel(expr);
			SimLabel sl = new SimLabel(name, index);
			listModel.addElement(sl);
		}
		catch (PrismLangException e) {
			// Silently ignore any problems - just don't add label to list
		}
	}

	/**
	 * Add a (properties file) label to the list.
	 * The associated properties file is also (optionally) passed in so that
	 * any required formulas/labels (not in the model file) can be obtained.
	 */
	public void addPropertyLabel(String name, Expression expr, PropertiesFile pf)
	{
		try {
			int index = engine.addLabel(expr, pf);
			SimLabel sl = new SimLabel(name, index);
			listModel.addElement(sl);
		}
		catch (PrismLangException e) {
			// Silently ignore any problems - just don't add label to list
		}
	}

	/**
	 * Add the special deadlock/init labels to the list.
	 */
	public void addDeadlockAndInit()
	{
		listModel.addElement(new InitSimLabel());
		listModel.addElement(new DeadlockSimLabel());
	}

	/**
	 * Class to store a normal label, that has been loaded into the simulator.
	 */
	class SimLabel
	{
		// Label name
		private String name;
		// Index of the label in the simulator engine
		private int index;

		public SimLabel(String name, int index)
		{
			this.name = name;
			this.index = index;
		}

		public String toString()
		{
			return name;
		}

		/**
		 * Get the value of the label in the current state of the simulator.
		 * 1 denotes true, 0 false, and -1 error/unknown
		 */
		public int getResult()
		{
			try {
				boolean b = engine.queryLabel(index);
				return b ? 1 : 0;
			} catch (PrismLangException e) {
				return -1;
			}
		}

		/**
		 * Get the value of the label in a particular step of the current simulator path.
		 * 1 denotes true, 0 false, and -1 error/unknown
		 */
		public int getResult(int step)
		{
			try {
				return engine.queryLabel(index, step) ? 1 : 0;
			} catch (PrismLangException e) {
				return -1;
			}
		}
	}

	/**
	 * Class to store the special "init" label.
	 */
	class InitSimLabel extends SimLabel
	{
		public InitSimLabel()
		{
			super("init", 0);
		}

		@Override
		public int getResult()
		{
			try {
				return engine.queryIsInitial() ? 1 : 0;
			} catch (PrismLangException e) {
				return -1;
			}
		}

		@Override
		public int getResult(int step)
		{
			try {
				return engine.queryIsInitial(step) ? 1 : 0;
			} catch (PrismLangException e) {
				return -1;
			}
		}
	}

	/**
	 * Class to store the special "deadlock" label.
	 */
	class DeadlockSimLabel extends SimLabel
	{
		public DeadlockSimLabel()
		{
			super("deadlock", 0);
		}

		@Override
		public int getResult()
		{
			try {
				return engine.queryIsDeadlock() ? 1 : 0;
			} catch (PrismException e) {
				return -1;
			}
		}

		@Override
		public int getResult(int step)
		{
			try {
				return engine.queryIsDeadlock(step) ? 1 : 0;
			} catch (PrismException e) {
				return -1;
			}
		}
	}

	// RENDERERS

	class SimLabelRenderer extends JLabel implements ListCellRenderer
	{
		// Default serial ID
		private static final long serialVersionUID = 1L;
		// Tooltip text
		private String text;

		public SimLabelRenderer()
		{
			setOpaque(true);
			text = "Unknown";
		}

		public String getToolTipText()
		{
			return text;
		}

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			setBorder(new BottomBorder());
			// Get label
			SimLabel l = (SimLabel) value;
			setText(l.toString());
			// Extract value of label (either for current state or an earlier path step).
			int val = sim.isOldUpdate() ? l.getResult(sim.getOldUpdateStep()) : l.getResult();
			switch (val) {
			case 1:
				text = "True";
				setIcon(GUIProperty.IMAGE_TICK);
				break;
			case 0:
				text = "False";
				setIcon(GUIProperty.IMAGE_CROSS);
				break;
			default:
				text = "Unknown";
				setIcon(GUIProperty.IMAGE_NOT_DONE);
				break;
			}
			// Set BG colour
			setBackground(isSelected ? background : Color.white);
			repaint();
			return this;
		}

	}

	class BottomBorder implements javax.swing.border.Border
	{
		public Insets getBorderInsets(Component c)
		{
			return new Insets(0, 0, 0, 0);
		}

		public boolean isBorderOpaque()
		{
			return true;
		}

		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
		{
			g.setColor(Color.lightGray);
			g.drawLine(x, (y + height - 1), (x + width), (y + height - 1));
		}
	}
}
