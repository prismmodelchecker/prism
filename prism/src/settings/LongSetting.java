//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

public class LongSetting extends Setting 
{
    private static LongRenderer renderer;
    private static LongEditor editor;

    /** Creates a new instance of LongSetting */
	public LongSetting(String name, Long value, String comment, SettingOwner owner, boolean editableWhenMultiple)
    {
        super(name, value, comment, owner, editableWhenMultiple);
    }
	
    public LongSetting(String name, Long value, String comment, SettingOwner owner, boolean editableWhenMultiple, NumericConstraint constraint)
    {
        super(name, value, comment, owner, editableWhenMultiple, constraint);
    }
    
    public SettingEditor getSettingEditor()
    {
        if (editor == null) {
            editor = new LongEditor();
        }
        return editor;
    }
    
    public SettingRenderer getSettingRenderer()
    {
        if (renderer == null) {
            renderer = new LongRenderer();
        }
        return renderer;
    }
    
    public Class getValueClass()
    {
        return Long.class;
    }
    
    public long getLongValue()
    {
        return ((Long)getValue()).longValue();
    }
    
	public Object parseStringValue(String string) throws SettingException
	{
		try
		{
			return new Long(string);
		}
		catch(NumberFormatException e)
		{
			throw new SettingException("Error when parsing: "+string+" as a Long value.");
		}
	}
	
	public String toString()
	{
		return ""+getLongValue();
	}
	
}
