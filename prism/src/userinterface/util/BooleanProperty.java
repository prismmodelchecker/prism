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

import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;
import java.awt.event.*;

public class BooleanProperty extends SingleProperty
{
    JPanel pan = new JPanel();
    JCheckBox renderer = new JCheckBox();
    
    /** Creates a new instance of BooleanProperty */
    public BooleanProperty(PropertyOwner owner,String name, boolean property)
    {
        this(owner, name, property, "");
    }
    
    public BooleanProperty(PropertyOwner owner,String name, boolean property, String comment)
    {
        super(owner, name, new Boolean(property), "", false, comment);
        //FlowLayout fl = new FlowLayout(FlowLayout.CENTER, 0, 0);
        pan.setLayout(new GridBagLayout());
        renderer.setBorder(null);
        pan.add(renderer);
    }
    
    public void setBoolValue(boolean property)
    {
        try
        {
            setProperty(new Boolean(property));
        }
        catch(PropertyException e)
        {
            //This will NEVER happen! (hopefully!)
        }
    }
    
    public boolean getBoolValue()
    {
        return ((Boolean)getProperty()).booleanValue();
    }
    
    public void setProperty(Object property) throws PropertyException
    {
        //System.out.println("setting bool property");
        if(property instanceof Boolean)
            super.setProperty(property);
        else if(property instanceof String)
        {
            if(property.toString().equals("true") || property.toString().equals("false"))
                super.setProperty(new Boolean(Boolean.getBoolean((String)property)));
            return;
            
        }
        else
        {
            throw new PropertyException("Value must be of type Boolean");
        }
        
    }

	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);

		if(renderer!=null)renderer.setEnabled(enabled);
	}
    
    
    public Component getTableCellRendererComponent(JTable table, Object value,
    boolean isSelected, boolean hasFocus, int row, int column)
    {
        renderer.setOpaque(true);
        renderer.setSelected(getBoolValue());
        if (isSelected)
        {
            renderer.setForeground(table.getSelectionForeground());
            renderer.setBackground(table.getSelectionBackground());
        }
        else
        {
            renderer.setForeground(table.getForeground());
            renderer.setBackground(table.getBackground());
        }
        
        if (hasFocus)
        {
            pan.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
            if (table.isCellEditable(row, column))
            {
                pan.setForeground( UIManager.getColor("Table.focusCellForeground") );
                pan.setBackground( UIManager.getColor("Table.focusCellBackground") );
            }
        } 
        else
        {
            pan.setBorder(new EmptyBorder(0, 2, 2, 1));
            pan.setForeground( UIManager.getColor("Table.focusCellForeground") );
                pan.setBackground( UIManager.getColor("Table.focusCellBackground") );
        }
        
        if(!isEnabled())
        {
            pan.setBackground(Color.lightGray);
            renderer.setEnabled(false);
        }
        else
        {
            renderer.setEnabled(true);
        }
        
        return pan;
    }
    
    public Component getTableCellRendererComponentMulti(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column, boolean allTheSame)
    {
        
        if(allTheSame) 
            return getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        renderer.setOpaque(true);
        renderer.setSelected(getBoolValue());
        if (isSelected)
        {
            
            renderer.setBackground(table.getSelectionBackground());
        }
        else
        {
            
            renderer.setBackground(table.getBackground());
        }
        
        if (hasFocus)
        {
            pan.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
            if (table.isCellEditable(row, column))
            {
                pan.setForeground( UIManager.getColor("Table.focusCellForeground") );
                pan.setBackground( new Color(240,240,240) );
            }
        } 
        else
        {
            pan.setBorder(new EmptyBorder(0, 2, 2, 1));
            pan.setForeground( UIManager.getColor("Table.focusCellForeground") );
            pan.setBackground( new Color(240,240,240) );
        }
        
        //renderer.setEnabled(false);
        
        
        return pan;
    }
    
    //THIS WILL NEED TO OVERRIDE THE EDITOR
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) 
    {
        renderer.setSelected(getBoolValue());
        pan.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
        
                pan.setForeground( UIManager.getColor("Table.focusCellForeground") );
                pan.setBackground( UIManager.getColor("Table.focusCellBackground") );
           if(!isEnabled())
        {
            pan.setBackground(Color.lightGray);
            renderer.setEnabled(false);
        }
        else
        {
            renderer.setEnabled(true);
        }
        return pan;
    }
    
    public Component getTableCellEditorComponentMulti(JTable table, Object value, boolean isSelected, int row, int column, boolean allTheSame) 
    {
        if(allTheSame)
        {
            return getTableCellEditorComponent(table, value, isSelected, row, column);
        }
        //renderer.setSelected(getBoolValue());
        pan.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
        
                pan.setForeground( UIManager.getColor("Table.focusCellForeground") );
                pan.setBackground( new Color(240,240,240));
            if(!isEnabled())
        {
            pan.setBackground(Color.lightGray);
            renderer.setEnabled(false);
        }
        else
        {
            renderer.setEnabled(true);
        }
        return pan;
    }
    
    public Boolean getEditorValue()
    {
        
        return new Boolean(renderer.isSelected()); 
    }
    
    public void addListenerToEditor(ActionListener e)
    {
        renderer.addActionListener(e);
    }
    
    public void removeListenerFromEditor(ActionListener e)
    {
        renderer.removeActionListener(e);
    }
    
    
}
