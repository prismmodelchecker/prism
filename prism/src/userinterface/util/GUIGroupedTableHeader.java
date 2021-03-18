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

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

/**
 * An object to represent a grouped TableHeader. It extends JTableHeader, but actually consists of two separate
 * JTableHeaders. Some of this could be tidied up, but it is build more or less to Swings concepts.
 */
@SuppressWarnings("serial")
public class GUIGroupedTableHeader extends JTableHeader implements TableColumnModelListener, MouseListener
{
	private JTableHeader bottomHeader;
	private JTableHeader topHeader;
	private GUIGroupedTableModel tableModel;
    
	/**
	 * Creates a new GUIGroupedTableHeader.
	 * @param model The column model that is the basis of this table header (must be grouped).
	 */
	public GUIGroupedTableHeader(GUIGroupedTableColumnModel model, GUIGroupedTableModel tableModel, GUIGroupedTable table)
	{
		super();
		removeAll();
		
		this.tableModel = tableModel;
						
		topHeader = new JTableHeader(model.getGroupTableColumnModel());
		topHeader.setResizingAllowed(false);
		topHeader.setReorderingAllowed(false);
		
		final TableCellRenderer renderer = topHeader.getDefaultRenderer();
		topHeader.setDefaultRenderer(new TableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) 
			{
				Component component = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				component.setFont(topHeader.getFont().deriveFont(Font.BOLD));
				return component;
			}
		});
		
		bottomHeader = new JTableHeader(model);
		bottomHeader.setReorderingAllowed(false);
		
		topHeader.setTable(table);
		bottomHeader.setTable(table);
		
		setLayout(new GridLayout(2,1));
					
		add(topHeader);
		add(bottomHeader);		
		
		model.updateGroups();
					
		Component[] cs = this.getComponents();
		for (int i = 0; i < cs.length; i++) 
		{
		    cs[i].addMouseListener(this);
		}
		
		topHeader.addMouseMotionListener(new MouseMotionAdapter()
		{
			private TableColumn lastColumn;
			
			public void mouseMoved(MouseEvent e) 
			{			
				bottomHeader.setToolTipText(null);
				
				TableColumn currentColumn;
				
				int column = topHeader.getColumnModel().getColumnIndexAtX(e.getX());
	    
	            if (column >= 0) 
	            {
	                currentColumn = topHeader.getColumnModel().getColumn(column);
	                
	                if (currentColumn != lastColumn) 
		            {	         
	                	topHeader.setToolTipText((GUIGroupedTableHeader.this).tableModel.getGroupToolTip(column));
		                lastColumn = currentColumn;
		            }	               
	            }	            
			}
		});
		bottomHeader.addMouseMotionListener(new MouseMotionAdapter()
		{
			private TableColumn lastColumn;
			
			public void mouseMoved(MouseEvent e) 
			{			
				topHeader.setToolTipText(null);
				
				TableColumn currentColumn;
				
				int column = bottomHeader.getColumnModel().getColumnIndexAtX(e.getX());
	    
	            if (column >= 0) 
	            {
	                currentColumn = bottomHeader.getColumnModel().getColumn(column);
	                
	                if (currentColumn != lastColumn) 
		            {	         
	                	bottomHeader.setToolTipText((GUIGroupedTableHeader.this).tableModel.getColumnToolTip(column));
		                lastColumn = currentColumn;
		            }	               
	            }	            
			}
		});
		
		//topHeader.setToolTipText(null);
		//bottomHeader.setToolTipText(null);
	}
	
	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(bottomHeader.getPreferredSize().width, bottomHeader.getPreferredSize().height * 2);
	}
	
	@Override
	public Dimension getMinimumSize()
	{
		return new Dimension(bottomHeader.getMinimumSize().width, bottomHeader.getMinimumSize().height * 2);
	}
	
	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(bottomHeader.getMaximumSize().width, bottomHeader.getMaximumSize().height * 2);
	}
	
	/**
	 * A fairly ugly hack to prevent a CellRenderer to be added to this container. More elegant solutions are welcome.
	 * @see java.awt.Container#addImpl(java.awt.Component, java.lang.Object, int)
	 */
	@Override
    protected void addImpl(Component comp, Object constraints, int index) {
	    // TODO Auto-generated method stub
		if (this.getComponents().length < 2)
		{
			super.addImpl(comp, constraints, index);
		}
    }

	/**
	 * Overwritten to never allow reordering.
	 * @see javax.swing.table.JTableHeader#setReorderingAllowed(boolean)
	 */
	@Override
	public void setReorderingAllowed(boolean reorderingAllowed) {			
		super.setReorderingAllowed(false);
	}

	/**
	 * Overwritten to always return the resizing column of the bottom JTableHeader.
	 * @see javax.swing.table.JTableHeader#getResizingColumn()
	 */
	@Override
	public TableColumn getResizingColumn() 
	{
		return bottomHeader.getResizingColumn();			
	}
	
    /** 
     * Overwritten to give the JTableHeaders a change to resize as well.
     * @see javax.swing.table.JTableHeader#resizeAndRepaint()
     */
    /*@Override
    public void resizeAndRepaint() 
    {   	
    	if (topHeader != null && bottomHeader != null)
    	{
    		this.doLayout();
    	}   
    	
	    super.resizeAndRepaint();	   
    }*/
   
	/**
	 * Set the column model.
	 * @param columnModel The grouped column model to use.
	 */
	public void setColumnModel(GUIGroupedTableColumnModel columnModel) {
	    
    	topHeader.setColumnModel(columnModel.getGroupTableColumnModel());
    	bottomHeader.setColumnModel(columnModel);   	
    	    	
    	this.resizeAndRepaint();
    	
    	columnModel.updateGroups();
	    super.setColumnModel(columnModel);
    }

	/** 
	 * Overwritten to catch events of child components.
	 */
	public void mouseClicked(MouseEvent e) {
		this.dispatchEvent(new MouseEvent(this, e.getID(), e.getWhen(), e.getModifiers(), e.getX(), e.getY() + ((Component)e.getSource()).getBounds().y, e.getClickCount(), e.isPopupTrigger()));
    }

	/** 
	 * Overwritten to catch events of child components.
	 */
	public void mouseEntered(MouseEvent e) {
		this.dispatchEvent(new MouseEvent(this, e.getID(), e.getWhen(), e.getModifiers(), e.getX(), e.getY() + ((Component)e.getSource()).getBounds().y, e.getClickCount(), e.isPopupTrigger()));    
	}

	/** 
	 * Overwritten to catch events of child components.
	 */
	public void mouseExited(MouseEvent e) {
		this.dispatchEvent(new MouseEvent(this, e.getID(), e.getWhen(), e.getModifiers(), e.getX(), e.getY() + ((Component)e.getSource()).getBounds().y, e.getClickCount(), e.isPopupTrigger()));    
	}

	/** 
	 * Overwritten to catch events of child components.
	 */
	public void mousePressed(MouseEvent e) {
		this.dispatchEvent(new MouseEvent(this, e.getID(), e.getWhen(), e.getModifiers(), e.getX(), e.getY() + ((Component)e.getSource()).getBounds().y, e.getClickCount(), e.isPopupTrigger()));    
	}

	/** 
	 * Overwritten to catch events of child components.
	 */
	public void mouseReleased(MouseEvent e) {
		this.dispatchEvent(new MouseEvent(this, e.getID(), e.getWhen(), e.getModifiers(), e.getX(), e.getY() + ((Component)e.getSource()).getBounds().y, e.getClickCount(), e.isPopupTrigger()));    
	}
}
