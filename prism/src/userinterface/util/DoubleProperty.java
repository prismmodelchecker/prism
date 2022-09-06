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

public class DoubleProperty extends SingleProperty
{
    /** Creates a new instance of DoubleProperty */
    public DoubleProperty(PropertyOwner owner,String name, double property)
    {
        this(owner, name, property, "");
    }
    
    public DoubleProperty(PropertyOwner owner,String name, double property, String comment)
    {
        super(owner,name, new Double(property), "", false, comment);
    }
    
    public void setValue(double property)
    {
        setValue(property, true);
    }
    
    public void setValue(double property, boolean notifyObservers)
    {
        try
        {
            setProperty(new Double(property), notifyObservers);
        }
        catch(PropertyException e)
        {
            //This will NEVER happen! (hopefully!)
        }
    }
    
    public double getValue()
    {
        return ((Double)getProperty()).doubleValue();
    }
    
    public void setProperty(Object property) throws PropertyException
    {
        setProperty(property, true);
    }
    
    public void setProperty(Object property, boolean notifyObservers) throws PropertyException
    {
        if(property instanceof Double)
            super.setProperty(property, notifyObservers);
        else if(property instanceof String)
        {
            try
            {
            
                super.setProperty(new Double(Double.parseDouble((String)property)), notifyObservers);
                return;
            }
            catch(NumberFormatException e)
            {
                throw new PropertyException("The format of this number is incorrect: "+(String)property);
            }
        }
        else 
        {
            throw new PropertyException("Value must be a real number");
        }
        
    }

	
    
   /* public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column)
    {
        return new DefaultTableCellRenderer().getTableCellRendererComponent
        (table, value, isSelected, hasFocus, row, column);
    }*/
    
    //THIS WILL NEED TO OVERRIDE THE EDITOR
    
}
