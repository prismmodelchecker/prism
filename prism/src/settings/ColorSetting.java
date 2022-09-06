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

import java.awt.*;
import java.util.*;

public class ColorSetting extends Setting
{
	private static ColorRenderer renderer;
	private static ColorEditor editor;

	/** Creates a new instance of ColorSetting */
	public ColorSetting(String name, Color value, String comment, SettingOwner owner, boolean editableWhenMultiple)
	{
		super(name, value, comment, owner, editableWhenMultiple);
	}
	
	public ColorSetting(String name, Color value, String comment, SettingOwner owner, boolean editableWhenMultiple, ColorConstraint constraint)
	{
		super(name, value, comment, owner, editableWhenMultiple, constraint);
	}
	
	
	public SettingEditor getSettingEditor()
	{
		if (editor == null) {
			editor = new ColorEditor();
		}
		return editor;
	}
	
	public SettingRenderer getSettingRenderer()
	{
		if (renderer == null) {
			renderer = new ColorRenderer();
		}
		return renderer;
	}
	
	public Class getValueClass()
	{
		return Color.class;
	}
	
	public Color getColorValue()
	{
		return (Color)getValue();
	}
	
	/**
	 *	Parses strings of the form: "r,g,b" e.g. "255,0,255"
	 */
	public Object parseStringValue(String string) throws SettingException
	{
		try
		{
			int r,g,b;
			StringTokenizer tokens = new StringTokenizer(string, ",");
			r = Integer.parseInt(tokens.nextToken());
			g = Integer.parseInt(tokens.nextToken());
			b = Integer.parseInt(tokens.nextToken());
			return new Color(r,g,b);
		}
		catch(NumberFormatException e)
		{
			throw new SettingException("Error when parsing: "+string+" as a Color value.");
		}
		catch(NoSuchElementException e)
		{
			throw new SettingException("Error when parsing: "+string+" as a Color value.");
		}
	}
	
	public String toString()
	{
		Color c = getColorValue();
		return c.getRed()+","+c.getGreen()+","+c.getBlue();
	}
	
}
