//==============================================================================
//
//	Copyright (c) 2004-2005, Andrew Hinton
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

package userinterface.properties;

import parser.*;

/**
 *
 * @author  ug60axh
 */
public class GUILabel
{
    public String name;
    public Expression label;
    
    public GUILabel(String name, Expression label)
    {
	this.name = name;
	this.label = label;
    }
    
    public String toString()
    {
	return "label \""+getNameString()+"\" = "+getValueString()+";";
    }
    
    public String getNameString()
    {
	return name;
    }
    
    public String getValueString()
    {
	if(label == null) return "";
	else return label.toString();
    }
}
