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

import java.util.*; 
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

import parser.ast.*;
import prism.*;

public class GUIExperimentTable extends JTable
{
	private static final long serialVersionUID = 1L;
	
	private ExperimentTableModel expModel;
	private GUIMultiProperties guiProps;
	
	/** Creates a new instance of GUIExperimentPanel */
	public GUIExperimentTable(GUIMultiProperties guiProps)
	{
		super();
		this.guiProps = guiProps;
		initComponents();
		TableColumn col = getColumnModel().getColumn(2);
		col.setCellRenderer(new ProgressBarRenderer());
	}
	
	/** Override set font to update row heights at same time */
	public void setFont(Font font)
	{
		super.setFont(font);
		setRowHeight(getFontMetrics(font).getHeight() + 4);
	}

	//UPDATE METHODS
	
	public void deleteSelected()
	{
		while(getSelectedRowCount() > 0)
		{
			int row = this.getSelectedRow();
			removeExperiment(row);
		}
	}
	
	public int newExperiment(PropertiesFile propFile, UndefinedConstants cons, boolean useSimulation)//propFile only contains 1 con
	{
		GUIExperiment ge = new GUIExperiment(this, guiProps, propFile, cons, useSimulation); 
		return expModel.addExperiment(ge);
	}
	
	public void removeExperiment(int i)
	{
		expModel.removeExperiment(i);
	}
	
	public void startExperiment(int i)
	{
		expModel.getExperiment(i).startExperiment();
	}
		
	public void progressChanged()
	{
		repaint();
	}
	
	public void stop()
	{
		expModel.stop();
	}
	
	//ACCESS METHODS
	
	public int getNumExperiments()
	{
		return expModel.getNumExperiments();
	}
	
	public GUIExperiment getExperiment(int i)
	{
		return expModel.getExperiment(i);
	}
	
	private void initComponents()
	{
		expModel = new ExperimentTableModel();
		setModel(expModel);
	}

	class ExperimentTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		
		ArrayList<GUIExperiment> experiments;
		
		public ExperimentTableModel()
		{
			experiments = new ArrayList<GUIExperiment>();
		}
		
		public String getColumnName(int index)
		{
			switch(index)
			{
				case 0: return "Property";
				case 1: return "Defined Constants";
				case 2: return "Progress";
				case 3: return "Status";
				case 4: return "Method";
				default: return "";
			}
		}
		
		public int getColumnCount()
		{
			return 5;
		}
		
		public int getRowCount()
		{
			return experiments.size();
		}
		
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			GUIExperiment ge = getExperiment(rowIndex);
			switch(columnIndex)
			{
				case 0: return ge.getPropertyString();
				case 1: return ge.getDefinedConstantsString();
				case 2:
					{
					JProgressBar b = new JProgressBar();
					// default case
					if (ge.getTotalIterations() > 0) 
					{
						b.setMaximum(ge.getTotalIterations());
						b.setValue(ge.getCurrentIterations());
						b.setStringPainted(true);
						b.setBackground(Color.white);
						int percent = (int)((double)ge.getCurrentIterations()/(double)ge.getTotalIterations()*100.0);
						b.setString(""+ge.getCurrentIterations()+"/"+ge.getTotalIterations()+" ("+percent+"%)");
					}
						// special case where there are 0 iterations
					else 
					{
						b.setMaximum(1);
						b.setValue(1);
						b.setStringPainted(true);
						b.setBackground(Color.white);
						b.setString("0/0 (100%)");
					}
					return b;
				}
				case 3:
					{
					if (!ge.isFinished()) return "Running";
					else return (ge.getCurrentIterations() < ge.getTotalIterations()) ? "Stopped" : "Done";
				}
				case 4:
					{
					if(ge.isUseSimulation()) return "Simulation";
					else return "Verification";
				}
				default: return "";
			}
		}
		
		public GUIExperiment getExperiment(int i)
		{
			return experiments.get(i);
		}
		
		public int getNumExperiments()
		{
			return experiments.size();
		}
		
		public int addExperiment(GUIExperiment e)
		{
			experiments.add(e);
			int i = experiments.size()-1;
			fireTableRowsInserted(i, i);
			scrollRectToVisible(getCellRect(i, 0, true));
			return experiments.indexOf(e);
		}
		
		public void removeExperiment(int i)
		{
			GUIExperiment ge = getExperiment(i);
			ge.clear();
			experiments.remove(i);
			fireTableRowsDeleted(i,i);
		}
		
		public void stop()
		{
			for(int i = 0; i < getNumExperiments(); i++)
			{
				getExperiment(i).stop();
			}
		}
		
		public Class getColumnClass(int i)
		{
			switch(i)
			{
				case 0: 
				case 1: return String.class;
				case 2: return JProgressBar.class;
				case 3: return String.class;
				default: return Object.class;
			}
		}
	}

	class ProgressBarRenderer implements TableCellRenderer
	{
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			if(value instanceof JProgressBar)
			{
				JProgressBar b = new JProgressBar();
				if(isSelected)
				{
					b.setBackground(getSelectionBackground());
				}
				else
				{
					b.setBackground(getBackground());
				}
				return (JProgressBar)value;
			}
			else return new JPanel();
		}
	}
}
