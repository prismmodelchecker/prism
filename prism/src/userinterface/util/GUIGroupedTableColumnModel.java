//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

import java.util.*;
import javax.swing.event.*;
import javax.swing.table.*;

/**
 * The TableColumnModel with the addition of Groups. A group is a set of columns. The set of columns 
 * belonging to each group are disjoint and non-empty (!). 
 */
@SuppressWarnings("serial")
public class GUIGroupedTableColumnModel extends DefaultTableColumnModel implements TableColumnModelListener
{
	//private DefaultTableColumnModel elementTableColumnModel;
	private DefaultTableColumnModel groupTableColumnModel;

	// The nth element is the index of the last element in the nth group.
	private ArrayList<Integer> lastColumn;

	/**
	 * Creates a new GUIGroupedTableModel with no groups.
	 */
	public GUIGroupedTableColumnModel()
	{
		lastColumn = new ArrayList<Integer>();

		//this.elementTableColumnModel = new DefaultTableColumnModel();
		this.groupTableColumnModel = new DefaultTableColumnModel();
	}

	/**
	 * Returns The columns of all groups in terms of a DefaultTableColumnModel.
	 * @return The columns of all groups in terms of a DefaultTableColumnModel.
	 */
	public DefaultTableColumnModel getGroupTableColumnModel()
	{
		return groupTableColumnModel;
	}

	/**
	 * Adds a group to this grouped column model.
	 * @param groupColumn A TableColumn representing all of the group.
	 * @param elementColumns An ArrayList of TableColumns representing the columns of this group in the model.
	 */
	public void addColumnGroup(TableColumn groupColumn, ArrayList<TableColumn> elementColumns)
	{
		groupTableColumnModel.addColumn(groupColumn);

		for (int i = 0; i < elementColumns.size(); i++) {
			this.addColumn(elementColumns.get(i));
		}

		lastColumn.add(new Integer(this.getColumnCount() - 1));
		updateGroups();
	}

	/**
	 * A function that updates the widths of the group columns. 
	 */
	public void updateGroups()
	{
		int group = 0;
		int groupWidth = 0;

		for (int i = 0; i < this.getColumnCount(); i++) {
			groupWidth += this.getColumn(i).getWidth();

			// If this is the last column of a group.
			if (i == lastColumn.get(group).intValue()) {
				while (group < groupTableColumnModel.getColumnCount() && i == lastColumn.get(group).intValue()) {
					groupTableColumnModel.getColumn(group).setWidth(groupWidth);

					groupWidth = 0;
					group++;
				}
			}
		}
	}

	public void columnAdded(TableColumnModelEvent e)
	{
	}

	public void columnMarginChanged(ChangeEvent e)
	{
	}

	public void columnMoved(TableColumnModelEvent e)
	{
	}

	public void columnRemoved(TableColumnModelEvent e)
	{
	}

	public void columnSelectionChanged(ListSelectionEvent e)
	{
	}

	/**
	 * If something changed in the columns of the table, then adjust the group widths.
	 */
	@Override
	protected void fireColumnMarginChanged()
	{
		// Size must have changed then.
		updateGroups();
		super.fireColumnMarginChanged();
	}

	/** 
	 * Empties this GUIGroupedTableColumnModel.
	 */
	public void clear()
	{
		while (this.getColumnCount() > 0) {
			this.removeColumn(this.getColumn(0));
		}

		while (groupTableColumnModel.getColumnCount() > 0) {
			groupTableColumnModel.removeColumn(groupTableColumnModel.getColumn(0));
		}

		this.lastColumn.clear();
	}
}
