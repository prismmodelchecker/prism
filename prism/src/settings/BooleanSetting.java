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

public class BooleanSetting extends Setting 
{
    private static BooleanRenderer renderer;
    private static BooleanEditor editor;

    /** Creates a new instance of BooleanSetting */
    public BooleanSetting(String name, Boolean value, String comment, SettingOwner owner, boolean editableWhenMultiple)
    {
        super(name, value, comment, owner, editableWhenMultiple);
    }
	
	public BooleanSetting(String name, Boolean value, String comment, SettingOwner owner, boolean editableWhenMultiple, BooleanConstraint constraint)
	{
		super(name, value, comment, owner, editableWhenMultiple, constraint);
	}
	
    public SettingEditor getSettingEditor()
    {
        if (editor == null) {
            editor = new BooleanEditor();
        }
        return editor;
    }
    
    public SettingRenderer getSettingRenderer()
    {
        if (renderer == null) {
            renderer = new BooleanRenderer();
        }
        return renderer;
    }
    
    public Class getValueClass()
    {
        return Boolean.class;
    }
    
    public boolean getBooleanValue()
    {
        return ((Boolean)getValue()).booleanValue();
    }
    
	public Object parseStringValue(String string) throws SettingException
	{
		if(string.equals("true")) return new Boolean(true);
		else if(string.equals("false")) return new Boolean(false);
		else throw new SettingException("Error when parsing: "+string+" as a Boolean value.");
	}
	
	public String toString()
	{
		if(getBooleanValue()) return "true";
		else return "false";
	}
	
}
