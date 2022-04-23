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

import parser.EvaluateContext.EvalMode;
import parser.ast.DeclarationType;
import prism.PrismLangException;

public abstract class Type 
{
	/**
	 * Returns the string denoting this type, e.g. "int", "bool".
	 */
	public abstract String getTypeString();
	
	/**
	 * Is this a primitive type (bool, int, etc.)?
	 */
	public boolean isPrimitive()
	{
		// Assume true by default; override if not
		return true;
	}
	
	/**
	 * Returns the default value for this type, assuming no initialisation specified.
	 */
	public Object defaultValue()
	{
		// Play safe: assume null
		return null;
	}
	
	/**
	 * Returns an appropriate DeclarationType object for this type, assuming no info about bounds, etc.
	 */
	public DeclarationType defaultDeclarationType() throws PrismLangException
	{
		// Not implemented by default
		throw new PrismLangException("Cannot create a DeclarationType for type " + getTypeString());
	}
	
	/**
	 * Returns true iff a value of type {@code type} can be cast to a value of this type.
	 */
	public boolean canCastTypeTo(Type type)
	{
		// Play safe: assume not possible, unless explicitly overridden.
		return false;
	}
	
	/**
	 * Make sure that a value, stored as an Object (Integer, Boolean, etc.)
	 * is the correct kind of Object for this type.
	 * Basically, implement some implicit casts (e.g. from type int to double).
	 * The evaluation mode is not changed (e.g. when casting  int to double,
	 * the conversion could be either Integer -> Double or BigInteger -> BigRational).
	 * This should only only work for combinations of types that satisfy {@code #canAssign(Type)}.
	 * If not, an exception is thrown (but such problems should have been caught earlier by type checking)
	 */
	public Object castValueTo(Object value) throws PrismLangException
	{
		// Play safe: assume error unless explicitly overridden.
		throw new PrismLangException("Cannot cast a value to type " + getTypeString());
	}

	/**
	 * Make sure that a value, stored as an Object (Integer, Boolean, etc.),
	 * is the correct kind of Object for this type, and a given evaluation mode.
	 * E.g. a "double" is stored as a Double for floating point mode (EvalMode.FP)
	 * but a BigRational for exact mode (EvalMode.EXACT).
	 * Basically, implement some implicit casts (e.g. from type int to double)
	 * and some conversions between evaluation modes (e.g. BigRational to Double).
	 * This should only only work for combinations of types that satisfy {@code #canAssign(Type)}.
	 * If not, an exception is thrown (but such problems should have been caught earlier by type checking)
	 */
	public Object castValueTo(Object value, EvalMode evalMode) throws PrismLangException
	{
		// Play safe: assume error unless explicitly overridden.
		throw new PrismLangException("Cannot cast a value to type " + getTypeString());
	}
	
	@Override
	public String toString()
	{
		return getTypeString();
	}
}
