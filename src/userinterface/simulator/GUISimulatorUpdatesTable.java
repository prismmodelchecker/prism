//==============================================================================
//	
//	Copyright (c) 2004-2005, Andrew Hinton
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

package userinterface.simulator;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.event.*;

import parser.*;

/**
 *
 * @author  ug60axh
 */
public class GUISimulatorUpdatesTable extends JTable implements ListSelectionListener
{
    public static Color [] DISTRIBUTION_COLOURS = { new Color(255,255,255),     //white
						    new Color(253,255,201) };     //yellow
						    /*new Color(224,255,224),     //green
						    new Color(255,227,255),     //pink
						    new Color(255,234,199),     //orange
						    new Color(209,217,255),     //blue
						    new Color(226,199,255),     //purple
						    new Color(212,255,255)} ;*/   //cyan
    private GUISimulator.UpdateTableModel utm;
    
    private UpdateHeaderListModel headerModel;
    private JList header;
    
    private GUISimulator sim;
    
    /** Creates a new instance of GUISimulatorUpdatesTable */
    public GUISimulatorUpdatesTable(GUISimulator.UpdateTableModel utm, GUISimulator sim)
    {
	super(utm);
	this.sim = sim;
	this.utm = utm;
	
	this.getSelectionModel().addListSelectionListener(this);
	
	setColumnSelectionAllowed(false);
	getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
	
	headerModel = new UpdateHeaderListModel();
		JList rowHeader = new JList(headerModel);
	
		rowHeader.setBackground(new JPanel().getBackground());
	
		rowHeader.setFixedCellWidth(15);
	
		rowHeader.setFixedCellHeight(getRowHeight());
		//+ getRowMargin());
		//getIntercellSpacing().height);
		rowHeader.setCellRenderer(new UpdateHeaderRenderer(this));
	
		this.header = rowHeader;
	
	setDefaultRenderer(Object.class, new UpdateTableRenderer());
	
	
	setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    }
    
    public void valueChanged(ListSelectionEvent e)
    {
	if(headerModel != null)
	headerModel.updateHeader();
	repaint();
    }
    
    public JList getUpdateRowHeader()
    {
	return header;
    }
    
    class UpdateTableRenderer implements TableCellRenderer 
    {
	JTextField renderer;
	
	
	public UpdateTableRenderer()
	{
	    renderer = new JTextField("");
	}
	public Component getTableCellRendererComponent(JTable table, Object value, 
	boolean isSelected, boolean hasFocus, int row, int column)
	{
	    renderer.setText(value.toString());
	    
	    int dist;
	    
	    if(sim.getModulesFile().getType() == ModulesFile.STOCHASTIC)
		dist = 0;
	    else dist = utm.getProbabilityDistributionOf(row);
	    
	    Color c = DISTRIBUTION_COLOURS[dist%2];
	    
	   
	    if(isSelected)
	    {
		Color newCol = new Color(c.getRed()-20, c.getGreen()-20, c.getBlue());
		if(utm.oldUpdate)
		{
		    newCol = new Color(newCol.getRed()-7, newCol.getGreen()-7, newCol.getBlue()-7);
		    renderer.setBackground(newCol);
		}
		else
		{
		    renderer.setBackground(newCol);
		}
	    }
	    else
	    {
		if(utm.oldUpdate)
		{
		    Color newCol = new Color(c.getRed()-7, c.getGreen()-7, c.getBlue()-7);
		    renderer.setBackground(newCol);
		}
		else
		    renderer.setBackground(c);
	    }
	    
	    renderer.setBorder(new EmptyBorder(1, 1, 1, 1));
	    return renderer;
	}
    }
    
    
    
    
	class UpdateHeaderRenderer extends JButton implements ListCellRenderer
	{
	
		boolean selected;
		ImageIcon selectedIcon;
	
		UpdateHeaderRenderer(JTable table)
		{
		    selected = false;
			/*JTableHeader header = table.getTableHeader();
			 setOpaque(true);
			 setBorder(UIManager.getBorder("TableHeader.cellBorder"));
			 setHorizontalAlignment(CENTER);
			 setForeground(header.getForeground());
			 setBackground(header.getBackground());
			 setFont(header.getFont());*/
		    setBorder(null);
		    selectedIcon = new javax.swing.ImageIcon(getClass().getResource("/images/smallItemSelected.gif"));
		}
	
		public Component getListCellRendererComponent( JList list,
			Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			
			selected = getSelectedRow() == index;
			
			if(selected)
			{
			    setIcon(selectedIcon);
			}
			else
			{
			    setIcon(null);
			}
			
			return this;
		}
	
		
	}
    
	class UpdateHeaderListModel extends AbstractListModel
	{
	
		public Object getElementAt(int index)
		{
			return "";
		}
	
		public int getSize()
		{
			return utm.getRowCount();
		}
	
		public void updateHeader()
		{
			fireContentsChanged(this, 0, utm.getRowCount());
			
			//System.out.println("The tables width is "+getWidth());
		}
	
	}
}
