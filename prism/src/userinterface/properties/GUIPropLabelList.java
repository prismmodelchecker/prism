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

package userinterface.properties;
import javax.swing.*;
import parser.*;
import java.util.*;
import javax.swing.table.*;

/**
 *
 * @author  ug60axh
 */
public class GUIPropLabelList extends JTable
{
	
	private PropLabelModel theModel;
	private GUIMultiProperties parent;
	
	/** Creates a new instance of GUIPropConstantList */
	public GUIPropLabelList(GUIMultiProperties parent)
	{
		super();
		this.parent = parent;
		theModel = new PropLabelModel();
		setModel(theModel);
	}
	
	public void correctEditors()
	{
	if(this.getCellEditor() != null)
	{
		getCellEditor().stopCellEditing();
	}
	}
	
	public GUILabel getLabel(int i)
	{
	    return theModel.getLabel(i);
	}
	
	public void newList()
	{
		theModel.newList();
	}
	
	public int getNumLabels()
	{
		return theModel.getNumLabels();
	}
	
	public void addNewLabel()
	{
		theModel.addLabel();
		parent.notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.PROPERTIES_LIST_CHANGED));
	}
	
	public void removeLabel(int index)
	{
		// Stop any editing that i staking place
		// (in case it is on cells we are about to delete)
		CellEditor ce = getCellEditor();
		if (ce != null) {
			ce.cancelCellEditing();
		}
		// do the remove
		theModel.removeLabel(index);
		parent.notifyEventListeners(new GUIPropertiesEvent(GUIPropertiesEvent.PROPERTIES_LIST_CHANGED));
	}
	
	public String getLabelsString()
	{
		return theModel.toString();
	}
	
	public void addPropertiesFile(PropertiesFile pf)
	{
		LabelList ll = pf.getLabelList();
		for(int i = 0; i < ll.size(); i++)
		{
			GUILabel lab = new GUILabel(ll.getLabelName(i), ll.getLabel(i));
			theModel.addLabel(lab);
		}
	}
	
	class PropLabelModel extends AbstractTableModel
	{
		ArrayList labels;
		
		public PropLabelModel()
		{
			labels = new ArrayList();
		}
		
		//ACCESS METHODS
		
		public GUILabel getLabel(int i)
		{
			return (GUILabel)labels.get(i);
		}
		
		public int getRowCount()
		{
			return labels.size();
		}
		
		public int getColumnCount()
		{
			return 2;
		}
		
		public Object getValueAt(int row, int column)
		{
			GUILabel lab = getLabel(row);
			switch(column)
			{
				case 0: return lab.getNameString();
				case 1: return lab.getValueString();
				default: return "";
			}
		}
		
		public String getColumnName(int columnIndex)
		{
			switch(columnIndex)
			{
				case 0: return "Name";
				case 1: return "Definition";
				default: return "";
			}
		}
		
		public int getNumLabels()
		{
			return labels.size();
		}
		
		public String toString()
		{
			String str = "";
			
			for(int i = 0; i < getNumLabels(); i++)
			{
				GUILabel gl = getLabel(i);
				str+=gl.toString()+"\n";
			}
			return str;
		}
		
		//UPDATE METHODS
		
		public void newList()
		{
			labels = new ArrayList();
			fireTableStructureChanged();
		}
		
		public void addLabel()
		{
			labels.add(new GUILabel("L"+labCount, new ExpressionTrue()));
			parent.setModified(true);
			fireTableRowsInserted(labels.size()-1, labels.size()-1);
			labCount++;
		}
		int labCount = 0;
		
		public void removeLabel(int index)
		{
			labels.remove(index);
			parent.setModified(true);
			fireTableRowsDeleted(index,index);
		}
		
		public void addLabel(GUILabel lab)
		{
			labels.add(lab);
			
			fireTableRowsInserted(labels.size()-1, labels.size()-1);
		}
		
		public boolean isCellEditable(int row, int column)
		{
			return true;
		}
		
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
			GUILabel gl = getLabel(rowIndex);
			switch(columnIndex)
			{
				case 0:
				{
					if(!gl.name.equals((String)aValue))
					{
						gl.name = (String)aValue;
						parent.setModified(true);
						parent.resetResults();
						fireTableCellUpdated(rowIndex, columnIndex);
					}
					break;
				}
				case 1:
				{
					//need to parse expression
					String old;
					if(gl.label!=null)
						old = gl.label.toString();
					else old = "";
					if(!old.equals((String)aValue))
					{
						try
						{
							if(((String)aValue).equals("")) gl.label = null;
							else
							{
								Expression e = parent.getPrism().parseSingleExpressionString((String)aValue);
								gl.label = e;
							}
						}
						catch(ParseException e)
						{
							parent.error("Syntax error in expression");
						}
						catch(prism.PrismException e)
						{
							parent.error("Syntax error in expression");
						}
						
						parent.setModified(true);
						parent.resetResults();
						fireTableCellUpdated(rowIndex, columnIndex);
					}
					break;
				}
			}
		}
	}
	
	
	
}
