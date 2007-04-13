//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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

import javax.swing.*;
import userinterface.properties.*;
import simulator.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import parser.*;

/**
 *
 * @author  ug60axh
 */
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
    
	public void addInitial()
	{
		listModel.addElement("init");
	}
    
	public void addDeadlock()
	{
		listModel.addElement("deadlock");
	}
    
	public void addRewardFormula(PCTLReward rew)
	{
		String str = rew.getOperand().toString();
        
		for(int i = 0; i < listModel.getSize(); i++)
		{
			if(listModel.getElementAt(i).toString().equals(str))
				return;//if this already is in here, do not add it
		}
        
		long pathPointer = engine.addPCTLRewardFormula(rew);
		if(pathPointer <=0) return;
		int index = engine.findPathFormulaIndex(pathPointer);
	
        
		SimPathFormula form = new SimPathFormula(rew.getOperand().toString(), index);
		listModel.addElement(form);
	}
    
	public void addProbFormula(PCTLProb prob)
	{
		String str = prob.getOperand().toString();
        
		for(int i = 0; i < listModel.getSize(); i++)
		{
			if(listModel.getElementAt(i).toString().equals(str))
				return;//if this already is in here, do not add it
		}
	
		long pathPointer = engine.addPCTLProbFormula(prob);
		if(pathPointer <=0) return;
		//System.out.println("probPointer = "+pathPointer);
		int index = engine.findPathFormulaIndex(pathPointer);
		//System.out.println("probindex = "+index);
        
		SimPathFormula form = new SimPathFormula(prob.getOperand().toString(), index);
		listModel.addElement(form);
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
	
		public int getResult()
		{
			return engine.queryPathFormula(pathFormulaIndex);
		}
	
		public double getResultNumeric()
		{
			return engine.queryPathFormulaNumeric(pathFormulaIndex);
		}
        
        
	
	
	}
    
    
	//RENDERERS
    
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
	
		public Component getListCellRendererComponent
			(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			setBorder(new BottomBorder());
			SimPathFormula l = (SimPathFormula)value;
	    
	   
			setText(l.toString());
	    
			int result = l.getResult();
		
			if(result == 1)
			{
				lastText = "True";
				setIcon(GUIProperty.IMAGE_TICK);
			}
			else if(result == 0)
			{
				lastText = "False";
				setIcon(GUIProperty.IMAGE_CROSS);
			}
			else if(result == 2)
			{
				lastText = ""+l.getResultNumeric();
				setIcon(GUIProperty.IMAGE_NUMBER);
			}
			else
			{
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
