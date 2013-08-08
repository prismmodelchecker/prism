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

public class ExpressionVar extends Expression
{
	// Variable name
	private String name;
	// The index of the variable in the model to which it belongs
	private int index;
	
	// Constructors
	
	public ExpressionVar(String n, Type t)
	{
		setType(t);
		name = n;
		index = -1;
	}
			
	// Set method
	
	public void setName(String n) 
	{
		name = n;
	}
	
	public void setIndex(int i) 
	{
		index = i;
	}
	
	// Get method
	
	public String getName()
	{
		return name;
	}
	
	public int getIndex()
	{
		return index;
	}
	
	// Methods required for Expression:
	
	/**
	 * Is this expression constant?
	 */
	public boolean isConstant()
	{
		return false;
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
		Object res = ec.getVarValue(name, index);
		if (res == null)
			throw new PrismLangException("Could not evaluate variable", this);
		return res;
	}
	
	@Override
	public boolean returnsSingleValue()
	{
		return false;
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
		return name;
	}

	/**
	 * Perform a deep copy.
	 */
	public Expression deepCopy()
	{
		ExpressionVar expr = new ExpressionVar(name, type);
		expr.setIndex(index);
		expr.setPosition(this);
		return expr;
	}
}

//------------------------------------------------------------------------------
