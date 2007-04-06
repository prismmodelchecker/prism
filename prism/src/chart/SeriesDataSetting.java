//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
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

package chart;

import settings.*;

/**
 *
 * @author Andrew Hinton
 */
public class SeriesDataSetting extends Setting 
{
    private static SeriesDataRenderer renderer;
    private static SeriesDataEditor editor;
    
    static
    {
	renderer = new SeriesDataRenderer();
	editor = new SeriesDataEditor();
    }
    
    /** Creates a new instance of SeriesDataSetting */
    public SeriesDataSetting(String name, GraphList value, String comment, SettingOwner owner, boolean editableWhenMultiple)
    {
	super(name, value, comment, owner, editableWhenMultiple);
    }
    
    public void checkObjectWithConstraints(Object obj)
    {
    }
    
    public Class getValueClass()
    {
	return GraphList.class;
    }
    
    public SettingRenderer getSettingRenderer()
    {
	return renderer;
    }
    
    public SettingEditor getSettingEditor()
    {
	return editor;
    }
    
    public GraphList getGraphListValue()
    {
	return (GraphList)getValue();
    }
    
	public Object parseStringValue(String string) throws SettingException
	{
		return "boohoo";
	}
	
	public String toString()
	{
		return "boohoo";
	}
	
}
