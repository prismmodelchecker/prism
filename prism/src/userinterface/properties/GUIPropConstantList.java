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
import parser.type.*;
import prism.*;

public class GUIPropConstantList extends JTable
{
	
	private PropConstantModel theModel;
	private GUIMultiProperties parent;
	
	/** Creates a new instance of GUIPropConstantList */
	public GUIPropConstantList(GUIMultiProperties parent)
	{
		super();
		this.parent = parent;
		theModel = new PropConstantModel();
		setModel(theModel);
		setEditorAndRenderer();
	}
	
	/** Override set font to update row heights at same time */
	public void setFont(Font font)
	{
		super.setFont(font);
		setRowHeight(getFontMetrics(font).getHeight() + 4);
	}

	protected void setEditorAndRenderer()
	{
		JComboBox typeChooser = new JComboBox();
		typeChooser.addItem(new String("int"));
		typeChooser.addItem(new String("double"));
		typeChooser.addItem(new String("bool"));
		TableCellEditor typeEditor = new DefaultCellEditor(typeChooser);
		TableColumn column = getColumnModel().getColumn(1);
		column.setCellEditor(typeEditor);
		try { setDefaultRenderer(Class.forName("java.lang.Object"), new TheCellRenderer()); } catch (ClassNotFoundException e) {}
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
		setEditorAndRenderer();
	}
	
	public int getNumConstants()
	{
		return theModel.getNumConstants();
	}
	
	public void addNewConstant()
	{
		theModel.addConstant();
		theModel.validateConstants();
	}
	
	public void removeConstant(int index)
	{
		// Stop any editing that i staking place
		// (in case it is on cells we are about to delete)
		CellEditor ce = getCellEditor();
		if (ce != null) {
			ce.cancelCellEditing();
		}
		// do the remove
		theModel.removeConstant(index);
		theModel.validateConstants();
	}
	
	public void addPropertiesFile(PropertiesFile pf)
	{
		ConstantList cl = pf.getConstantList();
		int i, n;
		Expression expr;
		n = cl.size();
		for(i = 0; i < n; i++)
		{
			expr = cl.getConstant(i);
			theModel.addConstant(new GUIConstant(parent, cl.getConstantName(i), (expr==null)?"":expr.toString(), cl.getConstantType(i)));
		}
		//  Don't validate until all constants added (might be dependent on each other)
		theModel.validateConstants();
	}
	
	/** Validate the constant list
	    NB: Don't call it "validate()" to avoid overwriting Swing methods */
	
	public void validateConstants()
	{
		theModel.validateConstants();
	}
	
	/** Is the constant list valid? */	
	// Changed: isValid is a method of java.awt.Component
	public boolean isConstantListValid()
	{
		if (theModel.error != null) return false;
		int i, n;
		n = theModel.getNumConstants();
		for(i = 0; i < n; i++) {
			if (!theModel.getConstant(i).isParseable()) return false;
		}
		return true;
	}
	
	/** Return string representing PRISM code for all constants in this list */
	
	public String getConstantsString()
	{
		return theModel.toString();
	}
	
	/** Return string representing PRISM code for all valid constants in this list */
	
	public String getValidConstantsString()
	{
		return theModel.validToString();
	}
	
	/* Table model for the constant list */
	
	class PropConstantModel extends AbstractTableModel
	{
		ArrayList constants; // The list of constants
		int conCount; // Counter used to generate new default names for constants
		Exception error; // Any error that occurs when all constants are parsed together
		
		public PropConstantModel()
		{
			constants = new ArrayList();
			conCount = 0;
			error = null;
		}
		
		/* Acesssor methods */
		
		public int getNumConstants()
		{
			return constants.size();
		}
		
		public GUIConstant getConstant(int i)
		{
			return (GUIConstant)constants.get(i);
		}
		
		public Exception getError()
		{
			return error;
		}
		
		public int getRowCount()
		{
			return constants.size();
		}
		
		public int getColumnCount()
		{
			return 3;
		}
		
		public Object getValueAt(int row, int column)
		{
			GUIConstant con = getConstant(row);
			switch(column)
			{
				case 0: return con.name;
				case 1: return con.type.getTypeString();
				case 2: return con.constant;
				default: return "";
			}
		}
		
		public String getColumnName(int columnIndex)
		{
			switch(columnIndex)
			{
				case 0: return "Name";
				case 1: return "Type";
				case 2: return "Value";
				default: return "";
			}
		}
		
		public boolean isCellEditable(int row, int column)
		{
			return true;
		}
		
		/** Return string representing PRISM code for all constants in this table model */
		
		public String toString()
		{
			String str = "";
			
			for(int i = 0; i < getNumConstants(); i++)
			{
				GUIConstant gc = getConstant(i);
				str+=gc.toString()+"\n";
			}
			return str;
		}
		
		/** Return string representing PRISM code for all valid constants in this table model */
		
		public String validToString()
		{
			if (error != null) return "";
			else return parseableToString();
		}
		
		/** Return string representing PRISM code for all parseable constants in this table model */
		
		public String parseableToString()
		{
			int i, n;
			GUIConstant gc;
			String str = "";
			n = getNumConstants();
			for(i = 0; i < n; i++) {
				gc = getConstant(i);
				if (gc.isParseable()) str+=gc.toString()+"\n";
			}
			return str;
		}
		
		/* Methods to modify table model */
		
		public void newList()
		{
			constants = new ArrayList();
			fireTableStructureChanged();
			parent.constantListChanged();
		}
		
		/** Add a new (default-valued) constant to the list.
		    You should call validateConstants() after calling this. */
		
		public void addConstant()
		{
			addConstant(new GUIConstant(parent, "C"+conCount, "", TypeInt.getInstance()));
			conCount++;
		}
		
		/** Add a new constant to the list.
		    You should call validateConstants() after calling this. */
		
		public void addConstant(GUIConstant cons)
		{
			cons.parse();
			constants.add(cons);
			fireTableRowsInserted(constants.size()-1, constants.size()-1);
			parent.constantListChanged();
		}
		
		/** Remove a constant from the list.
		    You should call validateConstants() after calling this. */
		
		public void removeConstant(int index)
		{
			constants.remove(index);
			fireTableRowsDeleted(index,index);
			parent.constantListChanged();
		}
		
		/** This is called directly by the GUI (when the table is edited) */
		
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
			GUIConstant gc = getConstant(rowIndex);
			switch(columnIndex)
			{
				case 0:
				{
					if(!gc.name.equals((String)aValue))
					{
						gc.name = (String)aValue;
						gc.parse();
						validateConstants();
						parent.constantListChanged();
					}
					break;
				}
				case 1:
				{
					Type newType = TypeInt.getInstance();
					if(((String)aValue).equals("double"))newType = TypeDouble.getInstance();
					else if(((String)aValue).equals("bool"))newType = TypeBool.getInstance();
					if(newType != gc.type) // if there was a change
					{
						gc.type = newType;
						gc.parse();
						validateConstants();
						parent.constantListChanged();
					}
					break;
				}
				case 2:
				{
					if (!gc.constant.equals((String)aValue))
					{
						gc.constant = (String)aValue;
						gc.parse();
						validateConstants();
						parent.constantListChanged();
					}
					break;
				}
			}
		}
		
		/** Validate the constant list
		    NB: Don't call it "validate()" to avoid overwriting Swing methods */
		
		public void validateConstants()
		{
			try {
				error = null;
				parent.getPrism().parsePropertiesString(parent.getParsedModel(), parseableToString());
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
			GUIConstant gc = theModel.getConstant(row);
			// If the constant has a parse error, show that
			if (!gc.isParseable()) {
				setBackground(isSelected ? parent.getSelectionColor() : parent.getWarningColor());
				setForeground(Color.red);
				setToolTipText(gc.parseError.toString());
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
				setToolTipText(gc.toString());
			}
			return cell;
		}
	}
}
