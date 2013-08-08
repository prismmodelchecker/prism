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

import parser.*;
import parser.visitor.*;
import prism.PrismLangException;
import parser.type.*;
public class ExpressionLiteral extends Expression
{
	Object value; // Value
	String string; // Optionally, keep original string to preserve user formatting

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
	
	/**
	 * Is this expression constant?
	 */
	public boolean isConstant()
	{
		return true;
	}

	@Override
	public boolean isProposition()
	{
		return true;
	}
	
	/**
	 * Evaluate this expression, return result.
	 * Note: assumes that type checking has been done already.
	 */
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		return value;
	}

	@Override
	public boolean returnsSingleValue()
	{
		return true;
	}

	// Methods required for ASTElement:
	
	/**
	 * Visitor method.
	 */
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}
	
	/**
	 * Convert to string.
	 */
	public String toString()
	{
		return string;
	}

	/**
	 * Perform a deep copy.
	 */
	public Expression deepCopy()
	{
		Expression expr = new ExpressionLiteral(type, value, string);
		expr.setPosition(this);
		return expr;
	}
}

//------------------------------------------------------------------------------
