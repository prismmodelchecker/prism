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

import prism.PrismLangException;

public class TypeClock extends Type 
{
	private static TypeClock singleton;
	
	static
	{
		singleton = new TypeClock();
	}
	
	private TypeClock()
	{		
	}	
	
	public static TypeClock getInstance()
	{
		return singleton;
	}
	
	// Methods required for Type:
	
	@Override
	public String getTypeString()
	{
		return "clock";
	}
	
	@Override
	public boolean isPrimitive()
	{
		return true;
	}
	
	@Override
	public Object defaultValue()
	{
		return 0;
	}
	
	@Override
	public boolean canAssign(Type type)
	{
		return (type instanceof TypeClock);
	}
	
	@Override
	public Double castValueTo(Object value) throws PrismLangException
	{
		if (value instanceof Double)
			return (Double) value;
		if (value instanceof Integer)
			return ((Double) value).doubleValue();
		else
			throw new PrismLangException("Can't convert " + value.getClass() + " to type " + getTypeString());
	}

	// Standard methods:
	
	public boolean equals(Object o)
	{
		return (o instanceof TypeClock);
	}
}
