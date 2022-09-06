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

public class DoubleSetting extends Setting 
{
    private static DoubleRenderer renderer;
    private static DoubleEditor editor;

    /** Creates a new instance of DoubleSetting */
    public DoubleSetting(String name, Double value, String comment, SettingOwner owner, boolean editableWhenMultiple, NumericConstraint constraint)
    {
        super(name, value, comment, owner, editableWhenMultiple, constraint);
    }
    
    public DoubleSetting(String name, Double value, String comment, SettingOwner owner, boolean editableWhenMultiple)
    {
        super(name, value, comment, owner, editableWhenMultiple);
    }
    
    public SettingEditor getSettingEditor()
    {
        if (editor == null) {
            editor = new DoubleEditor();
        }
        return editor;
    }
    
    public SettingRenderer getSettingRenderer()
    {
        if (renderer == null) {
            renderer = new DoubleRenderer();
        }
        return renderer;
    }
    
    public Class getValueClass()
    {
        return Double.class;
    }
    
    public double getDoubleValue()
    {
        return ((Double)getValue()).doubleValue();
    }
    
	public Object parseStringValue(String string) throws SettingException
	{
		try
		{
			return new Double(string);
		}
		catch(NumberFormatException e)
		{
			throw new SettingException("Error when parsing: "+string+" as a Double value.");
		}
	}
	
	public String toString()
	{
		return ""+getDoubleValue();
	}
	
}
