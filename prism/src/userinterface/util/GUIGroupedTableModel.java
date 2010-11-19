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

import javax.swing.table.TableModel;

/**
 * An interface which extends {@link TableModel} such that also group information can 
 * be extracted automatically. Note that groups are always non-empty.
 */
public interface GUIGroupedTableModel extends TableModel
{
	/**
	 * @return The number of groups in this model.
	 */
	public int getGroupCount();
	
	/**
	 * Retrieves the name of a group.
	 * @param groupIndex The index of a group.
	 * @return The name of the nth group, where n = groupIndex.
	 * @see #getColumnName(int columnIndex). 
	 */
	public String getGroupName(int groupIndex);
		
	/**
	 * Retrieves the index of the last `normal' column in a group.
	 * @param groupIndex The index of a group.
	 * @return The index of the last column in this group.
	 */
	public int getLastColumnOfGroup(int groupIndex);	
	
	/**
	 * Retrieves the tooltip to be set for a group.
	 * @param groupIndex The index of a group.
	 * @return The tooltip text for a group, or null.
	 */
	public String getGroupToolTip(int groupIndex);

	/**
	 * Retrieves the tooltip to be set for a column.
	 * @param columnIndex The index of a column.
	 * @return The tooltip text for a column, or null.
	 */
	public String getColumnToolTip(int columnIndex)	;
}
