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

public class SingleLineStringSetting extends Setting
{
    private static SingleLineStringRenderer renderer;
    private static SingleLineStringEditor editor;
    
    /** Creates a new instance of SingleLineStringSetting */
	public SingleLineStringSetting(String name, String value, String comment, SettingOwner owner, boolean editableWhenMultiple, StringConstraint constraint)
    {
        super(name, value, comment, owner, editableWhenMultiple, constraint);
    }
	
    public SingleLineStringSetting(String name, String value, String comment, SettingOwner owner, boolean editableWhenMultiple)
    {
        super(name, value, comment, owner, editableWhenMultiple);
    }
    
    public SettingEditor getSettingEditor()
    {
        if (editor == null) {
            editor = new SingleLineStringEditor();
        }
        return editor;
    }
    
    public SettingRenderer getSettingRenderer()
    {
        if (renderer == null) {
            renderer = new SingleLineStringRenderer();
        }
        return renderer;
    }
    
    public Class getValueClass()
    {
        return String.class;
    }
    
    public void checkObjectWithConstraints(Object obj) throws SettingException
    {
        super.checkObjectWithConstraints(obj);
		
		if(obj.toString().lastIndexOf('\n') != -1) //search for newline breaks.
			throw new SettingException("Single line settings cannot contain newline breaks.");
    }
    
    public String getStringValue()
    {
        return getValue().toString();
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
