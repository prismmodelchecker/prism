//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Mark Kattenbelt <mxk@cs.bham.uc.uk> (University of Birmingham)
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

import javax.swing.*;
import javax.swing.table.*;

import java.util.*;


/**
 * @author mxk
 * A table object that is capable of grouping headers, effectively by means of two JTableHeader
 * Objects on top of each other.
 */
public class GUIGroupedTable extends JTable 
{
	
	/**
	 * Creates a new GUIGroupedTable.
	 * @param tableModel The (grouped) table data model on which to base this table. 
	 */
	public GUIGroupedTable(GUIGroupedTableModel tableModel)
	{
		super(tableModel);		
	}
	
	/**
	 * The default TableColumnModel Object (and the only valid one) is a GUIGroupedColumnModel Object.
	 * @see javax.swing.JTable#createDefaultColumnModel()
	 */
	@Override
    protected TableColumnModel createDefaultColumnModel() 
	{	    
	    return new GUIGroupedTableColumnModel();
    }

	
	/** 
	 * Given a GUIGroupedTableModel, this function creates a GUIGroupedColumnModel.
	 * This function is called by JTable when the structure of the table changes, such that
	 * it is not necessary to worry about the header.
	 * @see javax.swing.JTable#createDefaultColumnsFromModel()
	 */
	@Override
    public void createDefaultColumnsFromModel() 
	{			
		GUIGroupedTableColumnModel groupedColumnModel = (GUIGroupedTableColumnModel)columnModel;
		
		groupedColumnModel.clear();	
				
		if (dataModel != null && dataModel instanceof GUIGroupedTableModel && columnModel instanceof GUIGroupedTableColumnModel)
		{			
			GUIGroupedTableModel groupedDataModel = (GUIGroupedTableModel)dataModel;
						
			int group = 0;
			int element = 0;
			
			// Add a group every iteration
			while (group < groupedDataModel.getGroupCount() && element < groupedDataModel.getColumnCount())
			{								
				ArrayList groupElements = new ArrayList();
				
				// Add an element every iteration
				while (groupedDataModel.getLastColumnOfGroup(group) >= element)
				{
					TableColumn elementColumn = new TableColumn(element);
					elementColumn.setHeaderValue(groupedDataModel.getColumnName(element));
					groupElements.add(elementColumn);
					element++;
				}
				
				TableColumn groupColumn = new TableColumn(group);
				groupColumn.setHeaderValue(groupedDataModel.getGroupName(group));
				group++;
				
				groupedColumnModel.addColumnGroup(groupColumn, groupElements);
			}		
		}		
    }

	/**
	 * Creates a default table header from the current column model.
	 * Also called by JTable such that there is no need to worry about headers once you implemented
	 * GUIGroupedTableModel.
	 * @see javax.swing.JTable#createDefaultTableHeader()
	 */
	@Override
    protected JTableHeader createDefaultTableHeader() {
		
		if (columnModel != null && columnModel instanceof GUIGroupedTableColumnModel)
		{
			return new GUIGroupedTableHeader((GUIGroupedTableColumnModel)columnModel, (GUIGroupedTableModel)this.getModel());
		}
	    return new GUIGroupedTableHeader(new GUIGroupedTableColumnModel(), (GUIGroupedTableModel)this.getModel());
    }
}

