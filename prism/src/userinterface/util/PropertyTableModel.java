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

package userinterface.util;

import javax.swing.table.*;
import javax.swing.*;
import java.util.*;

@SuppressWarnings("serial")
public class PropertyTableModel extends AbstractTableModel implements Observer
{
	//All of the data
	private ArrayList<PropertyOwner> owners;

	//Current sorted data
	private ArrayList<String> groupNames;
	private ArrayList<Integer> groupStarts;
	private ArrayList<Integer> groupSizes;
	private int currentGroup;

	private DefaultComboBoxModel comboModel;

	private JTable theTable;

	public PropertyTableModel()
	{
		super();
		this.theTable = null;
		groupNames = new ArrayList<String>();
		groupStarts = new ArrayList<Integer>();
		groupSizes = new ArrayList<Integer>();
		owners = new ArrayList<PropertyOwner>();
		comboModel = new DefaultComboBoxModel();
	}

	public void setJTable(JTable tab)
	{
		this.theTable = tab;
	}

	public void setOwners(ArrayList<PropertyOwner> owners)
	{
		this.owners = owners;
		Collections.sort(owners);

		Iterator<PropertyOwner> it = owners.iterator();
		PropertyOwner last = null;
		int currGroupCount = 0;
		String tempName = "";
		groupNames = new ArrayList<String>();
		groupStarts = new ArrayList<Integer>();
		groupSizes = new ArrayList<Integer>();
		int index = 0;

		String ownerList = "";
		while (it.hasNext()) {

			PropertyOwner po = (PropertyOwner) it.next();
			for (int i = 0; i < po.getNumProperties(); i++) {
				po.getProperty(i).addObserver(this);
				po.getProperty(i).setOwningModel(this);
			}
			if (last == null) {
				//this is the first group
				currGroupCount++;
				if (!po.getDescriptor().equals(""))
					ownerList += "\'" + po.getDescriptor() + "\'";
				tempName = po.getClassDescriptor();//+" \'"+po.getDescriptor()+"\'";
				groupStarts.add(new Integer(0));
			} else if (po.getUniquePropertyID() == last.getUniquePropertyID()) {
				//this is for the second or after in the sequence
				currGroupCount++;
				//tempName = ""+currGroupCount+" "+po.getClassDescriptor()+"s";
				if (!po.getClassDescriptor().endsWith("s"))
					tempName = po.getClassDescriptor() + "s";
				if (!po.getDescriptor().equals(""))
					ownerList += ", \'" + po.getDescriptor() + "\'";
			} else {
				//this starts a new group
				tempName += " " + ownerList + "";
				ownerList = "";
				groupNames.add(tempName);
				groupSizes.add(new Integer(currGroupCount));
				currGroupCount = 0;
				currGroupCount++;
				ownerList += po.getDescriptor();
				if (!po.getDescriptor().equals(""))
					tempName = po.getClassDescriptor() + " \'" + po.getDescriptor() + "\'";
				groupStarts.add(new Integer(index));
			}
			last = po;
			index++;
		}
		if (owners.size() != 0) {
			tempName += " " + ownerList + "";
			groupNames.add(tempName);
			groupSizes.add(new Integer(currGroupCount));
		}
		if (currentGroup > owners.size() - 1)
			currentGroup = 0;
		comboModel = new DefaultComboBoxModel(groupNames.toArray());
		fireTableDataChanged();
	}

	public String getGroupName(int i)
	{
		return groupNames.get(i);
	}

	public int getNumGroupNames()
	{
		return groupNames.size();
	}

	public int getRowCount()
	{
		if (groupNames.size() == 0)
			return 0;
		PropertyOwner firstInGroup = owners.get(groupStarts.get(currentGroup).intValue());
		return firstInGroup.getNumProperties();
	}

	public int getColumnCount()
	{
		return 2;
	}

	public String getColumnName(int column)
	{
		if (column == 0)
			return "Property";
		else
			return "Value";
	}

	public Object getValueAt(int row, int column)
	{
		if (column == 0) {
			PropertyOwner firstInGroup = owners.get(groupStarts.get(currentGroup).intValue());
			return firstInGroup.getProperty(row).getName();
		} else {

			//Simple if the selected owner group has only 1 member
			if (getCurrentGroupSize() == 1) {
				PropertyOwner firstInGroup = getOwner(getCurrentGroupStart());
				return firstInGroup.getProperty(row);
			} else {
				ArrayList<SingleProperty> currProps = new ArrayList<SingleProperty>();
				for (int i = getCurrentGroupStart(); i < getCurrentGroupStart() + getCurrentGroupSize(); i++) {
					PropertyOwner prop = getOwner(i);
					currProps.add(prop.getProperty(row));
				}
				MultipleProperty mult = new MultipleProperty(currProps);
				return mult;
			}

		}
	}

	public boolean isCellEditable(int row, int column)
	{
		if (column == 0) {
			return false;
		} else {
			return getSelectedProperty(column).isEnabled();
		}
	}

	public SingleProperty getSelectedProperty(int listIndex)
	{
		if (listIndex < 0)
			return null;
		PropertyOwner firstInGroup = getOwner(getCurrentGroupStart());
		return firstInGroup.getProperty(listIndex);

	}

	public void setValueAt(Object obj, int row, int column)
	{
		try {
			//Simple if the selected owner group has only 1 member
			if (column == 1) {
				if (getCurrentGroupSize() == 1) {
					PropertyOwner firstInGroup = getOwner(getCurrentGroupStart());
					firstInGroup.getProperty(row).setProperty(obj);
				} else {

					for (int i = getCurrentGroupStart(); i < getCurrentGroupStart() + getCurrentGroupSize(); i++) {
						PropertyOwner prop = getOwner(i);
						prop.getProperty(row).setProperty(obj);
					}
				}
			}
		} catch (PropertyException e) {
			//Will have to give a dialog
		}
	}

	/** Getter for property currentGroup.
	 * @return Value of property currentGroup.
	 *
	 */
	public int getCurrentGroup()
	{
		return currentGroup;
	}

	protected int getCurrentGroupSize()
	{
		return groupSizes.get(currentGroup).intValue();
	}

	protected int getCurrentGroupStart()
	{
		return groupStarts.get(currentGroup).intValue();
	}

	protected String getCurrentGroupName()
	{
		return groupNames.get(currentGroup);
	}

	protected PropertyOwner getOwner(int i)
	{
		return owners.get(i);
	}

	public int getNumGroups()
	{
		return groupNames.size();
	}

	/** Setter for property currentGroup.
	 * @param currentGroup New value of property currentGroup.
	 *
	 */
	public void setCurrentGroup(int currentGroup)
	{
		this.currentGroup = currentGroup;
		fireTableDataChanged();
	}

	public static void printArray(ArrayList<?> a)
	{
		System.out.print("(");
		for (int i = 0; i < a.size(); i++)
			System.out.print(a.get(i) + " ");
		//System.out.println(")");
	}

	/**
	 * Getter for property comboModel.
	 * @return Value of property comboModel.
	 */
	public DefaultComboBoxModel getComboModel()
	{
		return comboModel;
	}

	public void update(Observable o, Object arg)
	{
		fireTableDataChanged();
		repaintTable();
	}

	public void repaintTable()
	{
		if (theTable != null)
			theTable.repaint();
	}

}
