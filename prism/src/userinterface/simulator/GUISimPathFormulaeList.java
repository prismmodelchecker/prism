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
import userinterface.properties.*;
import simulator.*;

public class GUISimPathFormulaeList extends JList
{

	private GUISimulator guiSim;
	private SimulatorEngine engine;
	private DefaultListModel listModel;

	/** Creates a new instance of GUISimPathFormulaeList */
	public GUISimPathFormulaeList(GUISimulator guiSim)
	{
		this.guiSim = guiSim;
		this.engine = guiSim.getPrism().getSimulator();
		listModel = new DefaultListModel();
		setModel(listModel);

		setCellRenderer(new SimPathFormulaRenderer());
	}

	public void clearList()
	{
		listModel.clear();
	}

	// TODO: cut (subsumed by below)
	public void addRewardFormula(ExpressionReward rew)
	{
		String str = rew.getExpression().toString();

		for (int i = 0; i < listModel.getSize(); i++) {
			if (listModel.getElementAt(i).toString().equals(str))
				return;// if this already is in here, do not add it
		}

		// TODO: re-enable
		long pathPointer = -1;//engine.addExpressionReward(rew);
		if (pathPointer <= 0)
			return;
		int index = -1;//engine.findPathFormulaIndex(pathPointer);

		SimPathFormula form = new SimPathFormula(str, index);
		listModel.addElement(form);
	}

	public void addProperty(Expression prop, PropertiesFile propertiesFile)
	{
		try {
			//String str = prop.getExpression().toString();
			String str = prop.toString();
			for (int i = 0; i < listModel.getSize(); i++) {
				if (listModel.getElementAt(i).toString().equals(str))
					return;// if this already is in here, do not add it
			}
			int index = engine.addProperty(prop, propertiesFile);
			SimPathFormula form = new SimPathFormula(str, index);
			listModel.addElement(form);
		}
		catch (PrismException e) {
			// Silently ignore any problems - just don't add label to list
		}
	}

	class SimPathFormula
	{
		String pathFormula;
		int pathFormulaIndex;

		public SimPathFormula(String pathFormula, int pathFormulaIndex)
		{
			this.pathFormula = pathFormula;
			this.pathFormulaIndex = pathFormulaIndex;
		}

		public String toString()
		{
			return pathFormula;
		}

		public Object getResult()
		{
			return engine.queryProperty(pathFormulaIndex);
		}
	}

	// RENDERERS

	class SimPathFormulaRenderer extends JLabel implements ListCellRenderer
	{
		String lastText;

		public SimPathFormulaRenderer()
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
			SimPathFormula l = (SimPathFormula) value;

			setText(l.toString());

			Object result = l.getResult();

			if (result instanceof Boolean) {
				lastText = ((Boolean) result).booleanValue() ? "True" : "False";
				setIcon(((Boolean) result).booleanValue() ? GUIProperty.IMAGE_TICK : GUIProperty.IMAGE_CROSS);
			} else if (result != null) {
				lastText = result.toString();
				setIcon(GUIProperty.IMAGE_NUMBER);
			} else {
				lastText = "Unknown";
				setIcon(GUIProperty.IMAGE_NOT_DONE);
			}

			setBackground(Color.white);

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
