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

public class ChoiceSetting extends Setting
{
    private static ChoiceRenderer renderer;
    
    
    static
    {
        renderer = new ChoiceRenderer();
    }
    
    
    private String[]values;
    private ChoiceEditor editor; //editor is not static, we need one editor for each object
    
    /** Creates a new instance of ChoiceSetting */
    public ChoiceSetting(String name, String[]values, String value, String comment, SettingOwner owner, boolean editableWhenMultiple)
    {
        super(name, value, comment, owner, editableWhenMultiple);
        editor = new ChoiceEditor(values);
        this.values = values;
    }
	
	public ChoiceSetting(String name, String[]values, String value, String comment, SettingOwner owner, boolean editableWhenMultiple, StringConstraint constraint)
	{
		super(name, value, comment, owner, editableWhenMultiple, constraint);
		editor = new ChoiceEditor(values);
		this.values = values;
	}
    
    public void checkObjectWithConstraints(Object obj) throws SettingException
    {
		super.checkObjectWithConstraints(obj);
        boolean valid = false;
        for(int i = 0; i < values.length; i++)
        {
            if(obj.toString().equals(values[i]))
            {
                valid = true;
                break;
            }
        }
        if(!valid) throw new SettingException("Invalid Setting: "+obj.toString()+ " is not part of the enumeration.");
    }
    
    public SettingEditor getSettingEditor()
    {
        return editor;
    }
    
    public SettingRenderer getSettingRenderer()
    {
        return renderer;
    }
    
    public Class getValueClass()
    {
        return String.class;
    }
    
    public String getStringValue()
    {
        return getValue().toString();
    }
    
    public int getCurrentIndex()
    {
        for(int i = 0; i < values.length; i++)
        {
            if(getValue().toString().equals(values[i]))
                return i;
        }
        return -1;
    }
    
    public void setSelectedIndex(int i) throws SettingException
    {
        setValue(values[i]);
    }
    
	public Object parseStringValue(String string) throws SettingException
	{
		return string;
	}
	
	public String toString()
	{
		return getStringValue();
	}
	
}
