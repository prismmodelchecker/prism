//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package parser.ast;

import param.BigRational;
import parser.*;
import parser.visitor.*;
import prism.PrismLangException;
import parser.type.*;
public class ExpressionLiteral extends Expression
{
	Object value; // Value
	String string; // Optionally, keep original string to preserve user formatting and allow exact evaluation

	// Constructor
	
	public ExpressionLiteral(Type type, Object value)
	{
		this(type, value, ""+value);
	}

	public ExpressionLiteral(Type type, Object value, String string)
	{
		this.type = type;
		this.value = value;
		this.string = string;
	}

	// Set Methods
	
	public void setValue(Object value)
	{
		this.value = value;
		this.string = ""+value;
	}

	public void setString(String string)
	{
		this.string = string;
	}

	// Get Methods
	
	public Object getValue()
	{
		return value;
	}

	public String getString()
	{
		return string;
	}
	
	// Methods required for Expression:
	
	@Override
	public boolean isConstant()
	{
		return true;
	}

	@Override
	public boolean isProposition()
	{
		return true;
	}
	
	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		if (value instanceof BigRational) {
			// cast from BigRational to appropriate Java data type
			return getType().castFromBigRational((BigRational) value);
		}
		return value;
	}

	@Override
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException
	{
		if (value instanceof BigRational) {
			// if already a BigRational, return that
			return (BigRational) value;
		} else if (value instanceof Boolean) {
			return BigRational.from(value);
		}
		// otherwise, convert from string representation (potentially provides more precision for double literals)
		return BigRational.from(string);
	}

	@Override
	public boolean returnsSingleValue()
	{
		return true;
	}

	// Methods required for ASTElement:
	
	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public Expression deepCopy()
	{
		Expression expr = new ExpressionLiteral(type, value, string);
		expr.setPosition(this);
		return expr;
	}

	// Standard methods
	
	@Override
	public String toString()
	{
		return string;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((string == null) ? 0 : string.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExpressionLiteral other = (ExpressionLiteral) obj;
		if (string == null) {
			if (other.string != null)
				return false;
		} else if (!string.equals(other.string))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}

//------------------------------------------------------------------------------
