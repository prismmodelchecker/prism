//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package parser.type;

import java.util.*;

public class TypeArray extends Type 
{
	private static Map<Type, TypeArray> singletons;
	
	static
	{
		singletons = new HashMap<Type, TypeArray>();
	}
	
	private Type subType;
	
	public TypeArray(Type subType)
	{
		this.subType = subType;
	}

	public Type getSubType() 
	{
		return subType;
	}

	public void setSubType(Type subType) 
	{
		this.subType = subType;
	}
	
	public boolean equals(Object o)
	{
		if (o instanceof TypeArray)
		{
			TypeArray oa = (TypeArray)o;
			return (subType.equals(oa.getSubType()));
		}
		
		return false;
	}
	
	public String getTypeString()
	{
		return "array of " + subType.getTypeString();
	}
	
	public static TypeArray getInstance(Type subType)
	{
		if (!singletons.containsKey(subType))
			singletons.put(subType, new TypeArray(subType));
			
		return singletons.get(subType);
	}
}
