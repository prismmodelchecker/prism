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
import java.awt.*;

public class FontColorSetting extends Setting 
{
    private static FontColorRenderer renderer;
    private static FontColorEditor editor;
    
    static
    {
        renderer = new FontColorRenderer();
        editor = new FontColorEditor();
    }
    
    
    /** Creates a new instance of FontColorSetting */
	public FontColorSetting(String name, FontColorPair value, String comment, SettingOwner owner, boolean editableWhenMultiple)
    {
        super(name, value, comment, owner, editableWhenMultiple);
    }
	
    public FontColorSetting(String name, FontColorPair value, String comment, SettingOwner owner, boolean editableWhenMultiple, FontColorConstraint constraint)
    {
        super(name, value, comment, owner, editableWhenMultiple, constraint);
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
        return FontColorPair.class;
    }
    
    public FontColorPair getFontColorValue()
    {
        return ((FontColorPair)getValue());
    }
    
	public Object parseStringValue(String string) throws SettingException
	{
		try
        {
            StringTokenizer tokens = new StringTokenizer(string, ",");
            String name = tokens.nextToken();
            int size = Integer.parseInt(tokens.nextToken());
            int style = Integer.parseInt(tokens.nextToken());
			int r = Integer.parseInt(tokens.nextToken());
			int g = Integer.parseInt(tokens.nextToken());
			int b = Integer.parseInt(tokens.nextToken());
            
            return new FontColorPair(new Font(name, style, size), new Color(r,g,b));
        }
        catch(NumberFormatException e)
        {
            throw new SettingException("Error when parsing: "+string+" as a FontColorPair value.");
        }
        catch(NoSuchElementException e)
        {
            throw new SettingException("Error when parsing: "+string+" as a FontColorPair value.");
        }
	}
	
	public String toString()
	{
		Color c = getFontColorValue().c;
		Font f = getFontColorValue().f;
		
		return f.getName()+","+f.getSize()+","+f.getStyle()+","+c.getRed()+","+c.getGreen()+","+c.getBlue();
	}
	
}
