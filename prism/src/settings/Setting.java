//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

package settings;

import java.util.*;

public abstract class Setting
{
    private String key;
    private String name;
    private String comment;
    private Object value;
    private boolean editableWhenMultiple;
    private boolean enabled;
    private ArrayList constraints;
	private String version;
    
    private SettingOwner owner;
    
    public Setting(String name, Object value, String comment, SettingOwner owner, boolean editableWhenMultiple, SettingConstraint constraint)
    {
        this.name = name;
        this.key = name; //you should set this to something better though.
        this.value = value;
        this.comment = comment;
        this.owner = owner;
        this.editableWhenMultiple = editableWhenMultiple;
        this.enabled = true;
		this.version = null;
        
        constraints = new ArrayList();
        if(constraint != null) addConstraint(constraint);
    }
    
    public Setting(String name, Object value, String comment, SettingOwner owner, SettingConstraint constraint)
    {
        this(name, value, comment, owner, false, constraint);
    }
    
    public Setting(String name, Object value, String comment, SettingOwner owner, boolean editableWhenMultiple)
    {
        this(name, value, comment, owner, editableWhenMultiple, null);
    }
    
    public Setting(String name, Object value, String comment, SettingOwner owner)
    {
        this(name, value, comment, owner, false, null);
    }
    
    public void addConstraint(SettingConstraint constraint)
    {
        if(constraint != null) constraints.add(constraint);
    }
    
    public void removeConstraint(SettingConstraint constraint)
    {
        constraints.remove(constraint);
    }
    
    public int getNumConstraints()
    {
        return constraints.size();
    }
    
    public SettingConstraint getConstraint(int i)
    {
        return (SettingConstraint)constraints.get(i);
    }
    
	public void setVersion(String s)
	{
		version = s;
	}
	
    /**
     *  Return the name of this setting
     */
    public String getName()
    {
        return name;
    }
    
    /**
     *  Return a comment or description of this setting
     */
    public String getComment()
    {
        return comment;
    }
    
    /**
     *  Returns the owner of this setting
     */
    public SettingOwner getOwner()
    {
        return owner;
    }
    
    /**
     *  Should this property be editable if it has been selected along with
     *  others of the same name. 
     */
    public boolean isEditableWhenMultiple()
    {
        return editableWhenMultiple;
    }
    
    /**
     *  Returns the renderer responsible for this setting
     */
    public abstract SettingRenderer getSettingRenderer();
    
    /**
     *  Returns the editor for this setting
     */
    public abstract SettingEditor getSettingEditor();
    
    /**
     *  This method can be overridden if necessary, but do call super(obj)
     */
    public void checkObjectWithConstraints(Object obj) throws SettingException
    {
        for(int i = 0; i < getNumConstraints(); i++)
        {
            getConstraint(i).checkValue(obj);
        }
    }
    
    /**
     *  Returns the value of this setting
     */
    public Object getValue()
    {
        return value;
    }
    
    /**
     *  Returns the class type of the value being stored
     */
    public abstract Class getValueClass();
    
    /**
     *  Sets the value, with some type checking.  A set is expected to be done
     *  by the SettingOwner and so the owner notifySettingChanged method is not
     *  called.  This will cause only the renderer to display the new value.
     */
    public void setValue(Object obj) throws SettingException
    {
        SettingEditor editor = getSettingEditor();
        if(editor != null) editor.stopEditing();
        
        if(obj.getClass().isAssignableFrom(getValueClass())) //do a check that the object is in the correct format
        {
            //System.out.println("checking constraints");
            checkObjectWithConstraints(obj);
            this.value = obj;
            SettingDisplay display = getOwner().getDisplay();
            if(display != null) display.redisplaySetting(this);
        }
        else
        {
            throw new SettingException("Invalid type for property: "+name);
        }
    }
    
    /**
     *  Sets the value, with some type checking.  The edit is expected to be
     *  done either by one of the editors, or by an object that is not the
     *  SettingOwner.  Therefore, the SettingOwner will be notified and the
     *  renderer will display the new value.
     */
    public void editValue(Object obj) throws SettingException
    {
        SettingEditor editor = getSettingEditor();
        if(editor != null) editor.stopEditing();
    
        if(obj.getClass().isAssignableFrom(this.getValueClass()))
        {
            checkObjectWithConstraints(obj);
            this.value = obj;
            SettingDisplay display = getOwner().getDisplay();
            if(display != null) display.redisplaySetting(this);
            getOwner().notifySettingChanged(this);
        }
        else
        {
            throw new SettingException("Invalid type for property: "+name);
        }
    }
    
    public void setEnabled(boolean enable)
    {
        this.enabled = enable;
        getSettingEditor().stopEditing();
        
    }
    
    public boolean isEnabled()
    {
        return enabled;
    }
    
    public void setKey(String key)
    {
        this.key = key;
    }
    
    public String getKey()
    {
        return key;
    }
	
	public String getVersion()
	{
		return version;
	}

	/**
	 *	Must be overridden to define what this setting should be written like to
	 *	a file, so that it can be read in again
	 */
	public abstract String toString();
    
	/**
	 *	Must be overridden to return an object of the appropriate type, which is
	 *	a parsed version of the string.
	 */
	public abstract Object parseStringValue(String string) throws SettingException;
    
}
