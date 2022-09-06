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
import java.util.*;

public class SingleProperty extends Observable 
{
    protected String name;
    protected Object property;
    protected boolean multiline;
    protected String description;
    protected String comment;
    protected boolean editableWhenMultiple;
    protected PropertyOwner owner;
    protected boolean enabled;
    
    /** Creates a new instance of SingleProperty */
    public SingleProperty(PropertyOwner owner, String name, Object property)
    {
        this(owner, name, property, "", false, "");
    }
    
    public SingleProperty(PropertyOwner owner,String name, Object property, String description)
    {
        this(owner,name, property, description, false, "");
    }
    
    public SingleProperty(PropertyOwner owner,String name, Object property, boolean multiline)
    {
        this(owner,name, property, "", multiline, "");
    }
    
    public SingleProperty(PropertyOwner owner,String name, Object property, String description, boolean multiline, String comment)
    {
        setOwner(owner);
        setName(name);
        try
        {
        setProperty(property);
        }
        catch(PropertyException e)
        {
            property = new String("INVALID"); //default if the constructor is wrong
        }
        setDescription(description);
        setMultiline(multiline);
        setEditableWhenMultiple(true);
        setComment(comment);
        setEnabled(true);
    }
    
    /**
     * Getter for property name.
     * @return Value of property name.
     */
    public java.lang.String getName()
    {
        return name;
    }
    
    /**
     * Setter for property name.
     * @param name New value of property name.
     */
    public void setName(java.lang.String name)
    {
        this.name = name;
        setChanged();
        notifyObservers(null);
    }
    
    /**
     * Getter for property property.
     * @return Value of property property.
     */
    public java.lang.Object getProperty()
    {
        return property;
    }
    
    /**
     * Setter for property property.
     * @param property New value of property property.
     */
    public void setProperty(java.lang.Object property) throws PropertyException
    {
        setProperty(property, true);
    }
    
    public void setProperty(Object property, boolean notifyObservers) throws PropertyException
    {
		//System.out.println("Setting property "+property.toString());
        if(property == null) return; //protection
		if(property.toString().equals("...")) return;
        this.property = property;
        if(notifyObservers)
        {
            setChanged();
            notifyObservers(null);
        }
    }
    
    /**
     * Getter for property multiline.
     * @return Value of property multiline.
     */
    public boolean isMultiline()
    {
        return multiline;
    }
    
    /**
     * Setter for property multiline.
     * @param multiline New value of property multiline.
     */
    public void setMultiline(boolean multiline)
    {
            this.multiline = multiline;
            setChanged();
            notifyObservers(null);
    }
    
    /**
     * Getter for property description.
     * @return Value of property description.
     */
    public java.lang.String getDescription()
    {
        return description;
    }
    
    /**
     * Setter for property description.
     * @param description New value of property description.
     */
    public void setDescription(java.lang.String description)
    {
        this.description = description;
        setChanged();
        notifyObservers(null);
    }
    
    /** Getter for property editableWhenMultiple.
     * @return Value of property editableWhenMultiple.
     *
     */
    public boolean isEditableWhenMultiple()
    {
        return editableWhenMultiple;
    }
    
    /** Setter for property editableWhenMultiple.
     * @param editableWhenMultiple New value of property editableWhenMultiple.
     *
     */
    public void setEditableWhenMultiple(boolean editableWhenMultiple)
    {
        this.editableWhenMultiple = editableWhenMultiple;
        setChanged();
        notifyObservers(null);
    }
    
    /** Getter for property comment.
     * @return Value of property comment.
     *
     */
    public java.lang.String getComment()
    {
        return comment;
    }
    
    /** Setter for property comment.
     * @param comment New value of property comment.
     *
     */
    public void setComment(java.lang.String comment)
    {
        this.comment = comment;
        setChanged();
        notifyObservers(null);
    }
    
    public String toString()
    {
        return property.toString();
    }
    
    
    // This can be overridden in subclasses to provide a different renderer if required
    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column)
    {
        return null;
    }
    
    public Component getTableCellRendererComponentMulti(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column, boolean allTheSame)
    {
        return null;
    }
    
    //WILL NEED EDITOR TOO
    
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) 
    {
        return null;
    }
    
    public Component getTableCellEditorComponentMulti(JTable table, Object value, boolean isSelected, int row, int column, boolean allTheSame) 
    {
        return null;
    }
    
    /** Getter for property owner.
     * @return Value of property owner.
     *
     */
    public userinterface.util.PropertyOwner getOwner()
    {
        return owner;
    }
    
    /** Setter for property owner.
     * @param owner New value of property owner.
     *
     */
    public void setOwner(userinterface.util.PropertyOwner owner)
    {
        this.owner = owner;
    }
    
    /** Getter for property enabled.
     * @return Value of property enabled.
     *
     */
    public boolean isEnabled()
    {
        return enabled;
    }
    
    /** Setter for property enabled.
     * @param enabled New value of property enabled.
     *
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
		if(ptm != null)ptm.repaintTable();
    }
	private PropertyTableModel ptm;

	public void setOwningModel(PropertyTableModel ptm)
	{
		this.ptm = ptm;
	}
    
}
