//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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

package parser;

import java.util.Vector;

public class RenamedModule
{
	private String name;
	private String baseModule;
	private Vector oldNames;
	private Vector newNames;
	
	public RenamedModule(String n, String b)
	{
		name = n;
		baseModule = b;
		oldNames = new Vector();
		newNames = new Vector();
	}
		
	public void setName(String n)
	{
		name = n;
	}

	public void setBaseModule(String b)
	{
		baseModule = b;
	}
	
	public String getName()
	{
		return name;
	}

	public String getBaseModule()
	{
		return baseModule;
	}

	public void addRename(String s1, String s2)
	{
		oldNames.addElement(s1);
		newNames.addElement(s2);
	}
		
	public String getNewName(String s)
	{
		int i = oldNames.indexOf(s);
		
		if (i == -1) {
			return null;
		}
		
		return (String)newNames.elementAt(i);
	}
		
	public String getOldName(String s)
	{
		int i = newNames.indexOf(s);
		
		if (i == -1) {
			return null;
		}
		
		return (String)oldNames.elementAt(i);
	}
		
	public String toString()
	{
		String s = "";
		int i;
		
		s = s + "module " + name + " = " + baseModule + " [";
		for (i = 0; i < oldNames.size() - 1; i++) {
			s = s + oldNames.elementAt(i) + " = " + newNames.elementAt(i) + ", ";
		}
		i = oldNames.size() - 1;
		s = s + oldNames.elementAt(i) + " = " + newNames.elementAt(i) + "] endmodule";
		
		return s;
	}
}

//------------------------------------------------------------------------------
