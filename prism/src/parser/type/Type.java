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

import param.BigRational;
import prism.PrismLangException;

public abstract class Type 
{
	/**
	 * Returns the string denoting this type, e.g. "int", "bool".
	 */
	public abstract String getTypeString();
	
	/**
	 * Returns the default value for this type, assuming no initialisation specified.
	 */
	public Object defaultValue()
	{
		// Play safe: assume null
		return null;
	}
	
	/**
	 * Returns true iff a variable of this type can be assigned a value that is of type {@code type}. 
	 */
	public boolean canAssign(Type type)
	{
		// Play safe: assume not possible, unless explicitly overridden.
		return false;
	}
	
	/**
	 * Make sure that a value, stored as an Object (Integer, Boolean, etc.) is of this type.
	 * Basically, implement some implicit casts, e.g. Integer -> Double.
	 * This should only only work for combinations of types that satisfy {@code #canAssign(Type)}.
	 * If not, an exception is thrown (but such problems should have been caught earlier by type checking)
	 */
	public Object castValueTo(Object value) throws PrismLangException
	{
		// Play safe: assume error unless explicitly overridden.
		throw new PrismLangException("Cannot cast a value to type " + getTypeString());
	}

	/**
	 * Cast a BigRational value to the Java data type (Boolean, Integer, Double, ...)
	 * corresponding to this type.
	 * <br>
	 * For boolean and integer, this throws an exception if the value can not be
	 * precisely represented by the Java data type; for double, loss of precision
	 * is expected and does not raise an exception.
	 */
	public Object castFromBigRational(BigRational value) throws PrismLangException
	{
		// Play safe: assume error unless explicitly overridden.
		throw new PrismLangException("Cannot cast rational number to type " + getTypeString());
	}

	@Override
	public String toString()
	{
		return getTypeString();
	}
}
