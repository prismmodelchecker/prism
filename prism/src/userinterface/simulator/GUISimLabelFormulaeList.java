//==============================================================================
//
//	Copyright (c) 2004-2005, Andrew Hinton
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

import javax.swing.*;
import userinterface.properties.*;
import simulator.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import parser.*;
import prism.*;

/**
 *
 * @author  ug60axh
 */
public class GUISimLabelFormulaeList extends JList
{
    
	private static final Color background = new Color(202,225, 255);
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
    
	public void addLabel(GUILabel label, ModulesFile mf)
	{
	
	
		//create the expression in the simulator
		try
		{
	   
			Expression expr = label.label.findAllVars(mf.getVarNames(),mf.getVarTypes());
			int exprPointer = expr.toSimulator(engine);
	    
			int index = SimulatorEngine.loadProposition(exprPointer);
	    
			SimLabel sl = new SimLabel(label.getNameString(), index);
			//System.out.println("adding "+label.getNameString());
			listModel.addElement(sl);
			//System.out.println("added");
		}
		catch(SimulatorException e)
		{
			//System.out.println("exception "+e);
			e.printStackTrace();
		}
		catch(PrismException e)
		{
			//System.out.println("exception "+e);
		}
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
			return SimulatorEngine.queryProposition(formulaIndex);
		}
	
		public int getResult(int step)
		{
			return SimulatorEngine.queryProposition(formulaIndex, step);
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
			return SimulatorEngine.queryIsInitial();
		}
	
		public int getResult(int step)
		{
			return SimulatorEngine.queryIsInitial(step);
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
			return SimulatorEngine.queryIsDeadlock();
		}
	
		public int getResult(int step)
		{
			return SimulatorEngine.queryIsDeadlock(step);
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
	
		public Component getListCellRendererComponent
			(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			setBorder(new BottomBorder());
			SimLabel l = (SimLabel)value;
	    
	   
			setText(l.toString());
			if(!sim.isOldUpdate())
			{
		
				if(l.getResult() == 1)
				{
					lastText = "True";
					setIcon(GUIProperty.IMAGE_TICK);
				}
				else if(l.getResult() == 0)
				{
					lastText = "False";
					setIcon(GUIProperty.IMAGE_CROSS);
				}
				else
				{
					lastText = "Unknown";
					setIcon(GUIProperty.IMAGE_NOT_DONE);
				}
			}
			else
			{
				if(l.getResult(sim.getOldUpdateStep()) == 1)
				{
					lastText = "True";
					setIcon(GUIProperty.IMAGE_TICK);
				}
				else if(l.getResult(sim.getOldUpdateStep()) == 0)
				{
					lastText = "False";
					setIcon(GUIProperty.IMAGE_CROSS);
				}
				else
				{
					lastText = "Unknown";
					setIcon(GUIProperty.IMAGE_NOT_DONE);
				}
			}
	    
			if(isSelected)
			{
				setBackground(background);
                
			}
			else
			{
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
			return new Insets(0,0,0,0);
		}
	
		public boolean isBorderOpaque()
		{
			return true;
		}
	
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
		{
			g.setColor(Color.lightGray);
			g.drawLine(x,(y+height-1), (x+width), (y+height-1));
	    
		}
	}
    
}
