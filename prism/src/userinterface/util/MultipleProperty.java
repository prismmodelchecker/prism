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

import java.util.*;
import java.awt.*;
import javax.swing.*;

/**
 * Represents more than one single property when it is selected.
 */
public class MultipleProperty
{
    private ArrayList<SingleProperty> properties;
    
    /** Creates a new instance of MultipleProperty */
    public MultipleProperty(ArrayList<SingleProperty> properties)
    {
        this.properties = properties;
    }
    
    public boolean isMultiline()
    {
        return getProperty(0).isMultiline();
    }
    
    public SingleProperty getProperty(int i)
    {
        return properties.get(i);
    }
    
    public int getNumProperties()
    {
        return properties.size();
    }
    
    public boolean allTheSame()
    {
        String value = getProperty(0).toString();
        
        for(int i = 1; i < getNumProperties(); i++)
        {
            String temp = getProperty(i).toString();
            if(!temp.equals(value))
            {
                return false;
            }
        }
        return true;
    }
    
    public String toString()
    {
        
        String value = getProperty(0).toString();
        
        for(int i = 1; i < getNumProperties(); i++)
        {
            String temp = getProperty(i).toString();
            if(!temp.equals(value))
            {
                value = "...";
                break;
            }
        }
        return value;
    }
    
    public Component getTableCellRendererComponentMulti(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column)
    {
        return getProperty(0).getTableCellRendererComponentMulti(table, value, isSelected, hasFocus, row, column, allTheSame());
    }
    
    /**
     * Getter for property properties.
     * @return Value of property properties.
     */
    public ArrayList<SingleProperty> getProperties()
    {
        return properties;
    }
    
    
    
}
