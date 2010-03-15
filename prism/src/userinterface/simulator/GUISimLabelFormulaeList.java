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
import prism.PrismLangException;
import userinterface.properties.*;
import simulator.*;

/**
 *
 * @author  ug60axh
 */
public class GUISimLabelFormulaeList extends JList
{
	private static final Color background = new Color(202, 225, 255);
	private GUISimulator sim;
	private SimulatorEngine engine;
	private DefaultListModel listModel;

	/** Creates a new instance of GUISimLabelFormulaeList */
	public GUISimLabelFormulaeList(GUISimulator sim)
	{
		this.sim = sim;
		this.engine = sim.getPrism().getSimulator();
		listModel = new DefaultListModel();
		setModel(listModel);

		setCellRenderer(new SimLabelRenderer());
	}

	public void addLabel(String name, Expression expr, ModulesFile mf)
	{
		int index = engine.addLabel(expr);
		SimLabel sl = new SimLabel(name, index);
		listModel.addElement(sl);
	}

	public void addDeadlockAndInit()
	{
		listModel.addElement(new InitSimLabel());
		listModel.addElement(new DeadlockSimLabel());
	}

	public void clearLabels()
	{
		listModel.clear();
	}

	class SimLabel
	{
		String formula;
		int formulaIndex;

		public SimLabel(String formula, int formulaIndex)
		{
			this.formula = formula;
			this.formulaIndex = formulaIndex;
		}

		public String toString()
		{
			return formula;
		}

		public int getResult()
		{
			try {
				boolean b = engine.queryLabel(formulaIndex);
				return b ? 1 : 0;
			} catch (PrismLangException e) {
				return -1;
			}
		}

		public int getResult(int step)
		{
			try {
				boolean b = engine.queryLabel(formulaIndex, step);
				return b ? 1 : 0;
			} catch (PrismLangException e) {
				return -1;
			}
		}
	}

	class InitSimLabel extends SimLabel
	{
		public InitSimLabel()
		{
			super("init", 0);
		}

		public int getResult()
		{
			return engine.queryIsInitial();
		}

		public int getResult(int step)
		{
			return engine.queryIsInitial(step);
		}
	}

	class DeadlockSimLabel extends SimLabel
	{
		public DeadlockSimLabel()
		{
			super("deadlock", 0);
		}

		public int getResult()
		{
			return engine.queryIsDeadlock();
		}

		public int getResult(int step)
		{
			return engine.queryIsDeadlock(step);
		}
	}

	//RENDERERS

	class SimLabelRenderer extends JLabel implements ListCellRenderer
	{
		String lastText;

		public SimLabelRenderer()
		{
			setOpaque(true);
			lastText = "Unknown";
		}

		public String getToolTipText()
		{
			return lastText;
		}

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus)
		{
			setBorder(new BottomBorder());
			SimLabel l = (SimLabel) value;

			setText(l.toString());
			if (!sim.isOldUpdate()) {

				if (l.getResult() == 1) {
					lastText = "True";
					setIcon(GUIProperty.IMAGE_TICK);
				} else if (l.getResult() == 0) {
					lastText = "False";
					setIcon(GUIProperty.IMAGE_CROSS);
				} else {
					lastText = "Unknown";
					setIcon(GUIProperty.IMAGE_NOT_DONE);
				}
			} else {
				if (l.getResult(sim.getOldUpdateStep()) == 1) {
					lastText = "True";
					setIcon(GUIProperty.IMAGE_TICK);
				} else if (l.getResult(sim.getOldUpdateStep()) == 0) {
					lastText = "False";
					setIcon(GUIProperty.IMAGE_CROSS);
				} else {
					lastText = "Unknown";
					setIcon(GUIProperty.IMAGE_NOT_DONE);
				}
			}

			if (isSelected) {
				setBackground(background);

			} else {
				setBackground(Color.white);
			}

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
