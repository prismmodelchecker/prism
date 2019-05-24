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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;

import javax.swing.CellEditor;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import parser.ast.LabelList;
import parser.ast.PropertiesFile;
import prism.PrismException;

public class GUIPropLabelList extends JTable
{
	
	private PropLabelModel theModel;
	private GUIMultiProperties parent;
	
	/** Creates a new instance of GUIPropLabelList */
	public GUIPropLabelList(GUIMultiProperties parent)
	{
		super();
		this.parent = parent;
		theModel = new PropLabelModel();
		setModel(theModel);
		try { setDefaultRenderer(Class.forName("java.lang.Object"), new TheCellRenderer()); } catch (ClassNotFoundException e) {}
	}
	
	/** Override set font to update row heights at same time */
	public void setFont(Font font)
	{
		super.setFont(font);
		setRowHeight(getFontMetrics(font).getHeight() + 4);
	}

	public void correctEditors()
	{
		if(this.getCellEditor() != null)
		{
			getCellEditor().stopCellEditing();
		}
	}
	
	public void newList()
	{
		theModel.newList();
	}
	
	public int getNumLabels()
	{
		return theModel.getNumLabels();
	}
	
	public GUILabel getLabel(int i)
	{
	    return theModel.getLabel(i);
	}
	
	public void addNewLabel()
	{
		theModel.addLabel();
		theModel.validateLabels();
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
		theModel.validateLabels();
	}
	
	public void addPropertiesFile(PropertiesFile pf)
	{
		LabelList ll = pf.getLabelList();
		int i, n;
		n = ll.size();
		for(i = 0; i < n; i++)
		{
			GUILabel lab = new GUILabel(parent, ll.getLabelName(i), ll.getLabel(i).toString());
			theModel.addLabel(lab);
		}
		theModel.validateLabels();
	}
	
	/** Validate the label list
	    NB: Don't call it "validate()" to avoid overwriting Swing methods */
	
	public void validateLabels()
	{
		theModel.validateLabels();
	}
	
	/** Is the label list valid? */
	
	public boolean isPropLabelListValid()
	{
		if (theModel.error != null) return false;
		int i, n;
		n = theModel.getNumLabels();
		for(i = 0; i < n; i++) {
			if (!theModel.getLabel(i).isParseable()) return false;
		}
		return true;
	}
	
	/** Return string representing PRISM code for all labels in this list */
	
	public String getLabelsString()
	{
		return theModel.toString();
	}
	
	/** Return string representing PRISM code for all valid Labels in this list */
	
	public String getValidLabelsString()
	{
		return theModel.validToString();
	}
	
	/* Table model for the label list */
	
	class PropLabelModel extends AbstractTableModel
	{
		ArrayList labels; // The list of labels
		int labCount; // Counter used to generate new default names for labels
		Exception error; // Any error that occurs when all labels are parsed together
		
		public PropLabelModel()
		{
			labels = new ArrayList();
			labCount = 0;
			error = null;
		}
		
		/* Acesssor methods */
		
		public int getNumLabels()
		{
			return labels.size();
		}
		
		public GUILabel getLabel(int i)
		{
			return (GUILabel)labels.get(i);
		}
		
		public Exception getError()
		{
			return error;
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
		
		public boolean isCellEditable(int row, int column)
		{
			return true;
		}
		
		/** Return string representing PRISM code for all labels in this table model */
		
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
		
		/** Return string representing PRISM code for all valid labels in this table model */
		
		public String validToString()
		{
			if (error != null) return "";
			else return parseableToString();
		}
		
		/** Return string representing PRISM code for all parseable labels in this table model */
		
		public String parseableToString()
		{
			int i, n;
			GUILabel gl;
			String str = "";
			n = getNumLabels();
			for(i = 0; i < n; i++) {
				gl = getLabel(i);
				if (gl.isParseable()) str+=gl.toString()+"\n";
			}
			return str;
		}
		
		/* Methods to modify table model */
		
		public void newList()
		{
			labels = new ArrayList();
			fireTableStructureChanged();
			parent.labelListChanged();
		}
		
		/** Add a new (default-valued) label to the list.
		    You should call validateLabels() after calling this. */
		
		public void addLabel()
		{
			addLabel(new GUILabel(parent, "L"+labCount, "true"));
			labCount++;
		}
		
		/** Add a new label to the list.
		    You should call validateLabels() after calling this. */
		
		public void addLabel(GUILabel lab)
		{
			lab.parse();
			labels.add(lab);
			fireTableRowsInserted(labels.size()-1, labels.size()-1);
			parent.labelListChanged();
		}
		
		/** Remove a label from the list.
		    You should call validateLabels() after calling this. */
		
		public void removeLabel(int index)
		{
			labels.remove(index);
			fireTableRowsDeleted(index,index);
			parent.labelListChanged();
		}
		
		/** This is called directly by the GUI (when the table is edited) */
		
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
						gl.parse();
						validateLabels();
						parent.labelListChanged();
					}
					break;
				}
				case 1:
				{
					if(!gl.label.equals((String)aValue))
					{
						gl.label = (String)aValue;
						gl.parse();
						validateLabels();
						parent.labelListChanged();
					}
					break;
				}
			}
		}
		
		/** Validate the label list
		    NB: Don't call it "validate()" to avoid overwriting Swing methods */
		
		public void validateLabels()
		{
			try {
				error = null;
				parent.getPrism().parsePropertiesString(parent.getConstantsString()+"\n"+parseableToString());
			}
			catch (PrismException e) {
				error = e;
			}
			fireTableDataChanged();
		}
	}
	
	class TheCellRenderer extends DefaultTableCellRenderer
	{
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) 
		{
			Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			GUILabel gl = theModel.getLabel(row);
			// If the label has a parse error, show that
			if (!gl.isParseable()) {
				setBackground(isSelected ? parent.getSelectionColor() : parent.getWarningColor());
				setForeground(Color.red);
				setToolTipText(gl.parseError.toString());
			}
			// If not but there is a "global" error for the whole list, show that
			else if (theModel.error != null) {
				setBackground(isSelected ? parent.getSelectionColor() : parent.getWarningColor());
				setForeground(Color.red);
				setToolTipText(theModel.error.toString());
			}
			// Otherwise everything is fine
			else {
				setBackground(isSelected ? parent.getSelectionColor() : Color.white);
				setForeground(Color.black);
				setToolTipText(gl.toString());
			}
			return cell;
		}
	}
}
