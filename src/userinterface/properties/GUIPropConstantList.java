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
		
		JComboBox typeChooser = new JComboBox();
		typeChooser.addItem(new String("int"));
		typeChooser.addItem(new String("double"));
		typeChooser.addItem(new String("bool"));
		TableCellEditor typeEditor = new DefaultCellEditor(typeChooser);
		TableColumn column = getColumnModel().getColumn(1);
		column.setCellEditor(typeEditor);
		
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
		JComboBox typeChooser = new JComboBox();
		typeChooser.addItem(new String("int"));
		typeChooser.addItem(new String("double"));
		typeChooser.addItem(new String("bool"));
		TableCellEditor typeEditor = new DefaultCellEditor(typeChooser);
		TableColumn column = getColumnModel().getColumn(1);
		column.setCellEditor(typeEditor);
	}
	
	public int getNumConstants()
	{
		return theModel.getNumConstants();
	}
	
	public void addNewConstant()
	{
		theModel.addConstant();
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
	}
	
	public String getConstantsString()
	{
		return theModel.toString();
	}
	
	public void addPropertiesFile(PropertiesFile pf)
	{
		ConstantList cl = pf.getConstantList();
		for(int i = 0; i < cl.size(); i++)
		{
			//System.out.println("adding a constant");
			GUIConstant con = new GUIConstant(cl.getConstantName(i), cl.getConstant(i), cl.getConstantType(i));
			theModel.addConstant(con);
		}
	}
	
	class PropConstantModel extends AbstractTableModel
	{
		ArrayList constants;
		
		public PropConstantModel()
		{
			constants = new ArrayList();
		}
		
		public GUIConstant getConstant(int i)
		{
			return (GUIConstant)constants.get(i);
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
				case 1:
				{
					switch(con.type)
					{
						case GUIConstant.INT: return "int";
						case GUIConstant.BOOL: return "bool";
						case GUIConstant.DOUBLE: return "double";
						default: return "";
					}
				}
				case 2:
				{
					Expression e = con.constant;
					if(e == null) return "";
					else return con.constant;
				}
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
		
		public int getNumConstants()
		{
			return constants.size();
		}
		
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
		
		public void newList()
		{
			constants = new ArrayList();
			fireTableStructureChanged();
		}
		
		public void addConstant()
		{
			constants.add(new GUIConstant("C"+conCount,  null,  GUIConstant.INT));
			
			parent.setModified(true);
			fireTableRowsInserted(constants.size()-1, constants.size()-1);
			conCount++;
		}
		int conCount = 0;
		
		public void removeConstant(int index)
		{
			constants.remove(index);
			parent.setModified(true);
			fireTableRowsDeleted(index,index);
		}
		
		public void addConstant(GUIConstant cons)
		{
			constants.add(cons);
			
			fireTableRowsInserted(constants.size()-1, constants.size()-1);
		}
		
		public boolean isCellEditable(int row, int column)
		{
			return true;
		}
		
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
						parent.setModified(true);
						parent.resetResults();
						fireTableCellUpdated(rowIndex, columnIndex);
					}
					break;
				}
				case 1:
				{
					//System.out.println("value = "+aValue);
					int newType = GUIConstant.INT;
					if(((String)aValue).equals("double"))newType = GUIConstant.DOUBLE;
					else if(((String)aValue).equals("bool"))newType = GUIConstant.BOOL;
					if(newType != gc.type) // if there was a change
					{
						gc.type = newType;
						gc.constant = null;
						parent.setModified(true);
						parent.resetResults();
						fireTableCellUpdated(rowIndex, columnIndex);
						fireTableCellUpdated(rowIndex, columnIndex+1);
					}
					break;
				}
				case 2:
				{
					String old;
					
					if(gc.constant != null)old = gc.constant.toString();
					else old = "";
					if(!old.equals((String)aValue))
					{
						//need to parse expression
						try
						{
							if(((String)aValue).equals("")) gc.constant = null;
							else
							{
								Expression e = parent.getPrism().parseSingleExpressionString((String)aValue);
								gc.constant = e;
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
	
	class GUIConstant
	{
		public static final int INT = Expression.INT;
		public static final int BOOL = Expression.BOOLEAN;
		public static final int DOUBLE = Expression.DOUBLE;
		
		String name;
		Expression constant;
		int type;
		
		public GUIConstant(String name, Expression constant, int type)
		{
			this.name = name;
			this.constant = constant;
			this.type = type;
		}
		
		public String toString()
		{
			return "const "+getTypeString()+" "+name+getValueString()+";";
		}
		
		public String getTypeString()
		{
			switch(type)
			{
				case(INT):	return "int";
				case(DOUBLE): return "double";
				case(BOOL):   return "bool";
				default:	  return "";
			}
		}
		
		public String getValueString()
		{
			if(constant == null)//if undefined
			{
				return "";
			}
			else return " = "+constant.toString(); //if defined
		}
	}
	
}
