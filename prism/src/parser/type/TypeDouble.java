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

import param.BigRational;
import parser.EvaluateContext.EvalMode;
import parser.ast.DeclarationDoubleUnbounded;
import parser.ast.DeclarationType;
import prism.PrismLangException;

public class TypeDouble extends Type 
{
	private static TypeDouble singleton;
	
	static
	{
		singleton = new TypeDouble();
	}
	
	private TypeDouble()
	{		
	}	
	
	public static TypeDouble getInstance()
	{
		return singleton;
	}
	
	// Methods required for Type:
	
	@Override
	public String getTypeString()
	{
		return "double";
	}
	
	@Override
	public boolean isPrimitive()
	{
		return true;
	}
	
	@Override
	public Object defaultValue()
	{
		return 0.0;
	}
	
	@Override
	public DeclarationType defaultDeclarationType()
	{
		return new DeclarationDoubleUnbounded();
	}
	
	@Override
	public boolean canCastTypeTo(Type type)
	{
		return (type instanceof TypeDouble || type instanceof TypeInt);
	}
	
	@Override
	public Number castValueTo(Object value) throws PrismLangException
	{
		// Convert from int to double if needed, ignore eval mode
		if (value instanceof Double) {
			return (Double) value;
		}
		if (value instanceof BigRational) {
			return (BigRational) value;
		}
		if (value instanceof Integer) {
			return Double.valueOf(((Integer) value).intValue());
		}
		if (value instanceof BigInteger) {
			return new BigRational((BigInteger) value);
		}
		else {
			throw new PrismLangException("Can't convert " + value.getClass() + " to type " + getTypeString());
		}
	}

	@Override
	public Number castValueTo(Object value, EvalMode evalMode) throws PrismLangException
	{
		switch (evalMode) {
		// For floating point mode, should be a Double
		case FP:
			// Already a Double - nothing to do
			if (value instanceof Double) {
				return (Double) value;
			}
			// Other possibilities (Integer/BigRational/BigInteger) are all Numbers
			if (value instanceof Number) {
				return ((Number) value).doubleValue();
			}
			throw new PrismLangException("Cannot cast " + value.getClass() + " to " + getTypeString());
		// For exact mode, should be a BigRational
		case EXACT:
			// Already a BigRational - nothing to do
			if (value instanceof BigRational) {
				return (BigRational) value;
			}
			// Other possibilities (Integer/BigRational/BigInteger) need conversion
			if (value instanceof BigInteger) {
				return new BigRational((BigInteger) value);
			}
			if (value instanceof Double) {
				return new BigRational(value.toString());
			}
			if (value instanceof Integer) {
				return new BigRational((Integer) value);
			}
			throw new PrismLangException("Cannot cast " + value.getClass() + " to " + getTypeString());
		default:
			throw new PrismLangException("Unknown evaluation mode " + evalMode);
		}
	}

	// Standard methods:
	
	public boolean equals(Object o)
	{
		return (o instanceof TypeDouble);
	}
	
}
