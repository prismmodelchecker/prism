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

import java.math.BigInteger;

import parser.EvaluateContext.EvalMode;
import parser.ast.DeclarationIntUnbounded;
import parser.ast.DeclarationType;
import prism.PrismLangException;

public class TypeInt extends Type 
{
	private static TypeInt singleton;
	
	static
	{
		singleton = new TypeInt();
	}
	
	private TypeInt()
	{		
	}
	
	public static TypeInt getInstance()
	{
		return singleton;
	}
	
	// Methods required for Type:
	
	@Override
	public String getTypeString()
	{
		return "int";
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
	public DeclarationType defaultDeclarationType()
	{
		return new DeclarationIntUnbounded();
	}
	
	@Override
	public boolean canCastTypeTo(Type type)
	{
		return (type instanceof TypeInt);
	}
	
	@Override
	public Number castValueTo(Object value) throws PrismLangException
	{
		if (value instanceof Integer) {
			return (Integer) value;
		} else if (value instanceof BigInteger) {
			return (BigInteger) value;
		} else {
			throw new PrismLangException("Can't convert " + value.getClass() + " to type " + getTypeString());
		}
	}

	@Override
	public Number castValueTo(Object value, EvalMode evalMode) throws PrismLangException
	{
		switch (evalMode) {
		// For floating point mode, should be an Integer
		case FP:
			if (value instanceof Integer) {
				return (Integer) value;
			} else if (value instanceof BigInteger) {
				return ((BigInteger) value).intValue();
			}
			throw new PrismLangException("Cannot convert " + value.getClass() + " to " + getTypeString());
		// For exact mode, should be a BigInteger
		case EXACT:
			if (value instanceof BigInteger) {
				return (BigInteger) value;
			} else if (value instanceof Integer) {
				return BigInteger.valueOf((Integer) value);
			}
			throw new PrismLangException("Cannot convert " + value.getClass() + " to " + getTypeString());
		default:
			throw new PrismLangException("Unknown evaluation mode " + evalMode);
		}
	}

	// Standard methods:
	
	public boolean equals(Object o)
	{
		return (o instanceof TypeInt);
	}
}
