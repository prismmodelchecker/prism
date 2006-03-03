//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker
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

import prism.PrismException;

public class SystemModule extends SystemDefn
{
	String name;
	
	// constructors
	
	public SystemModule()
	{
	}
	
	public SystemModule(String n)
	{
		name = n;
	}
	
	// set method
	
	public void setName(String n)
	{
		name = n;
	}
	
	// get method
	
	public String getName()
	{
		return name;
	}
		
	// get list of all modules appearing (recursively)
	
	public void getModules(Vector v)
	{
		v.addElement(name);
	}

	// get list of all synchronising actions _introduced_ (recursively)

	public void getSynchs(Vector v)
	{
		// do nothing
	}
	
	// check
	
	public void check(Vector synchs) throws PrismException
	{
		// do nothing
	}
	
	// convert to string
	
	public String toString()
	{
		return name;
	}
}

//------------------------------------------------------------------------------
