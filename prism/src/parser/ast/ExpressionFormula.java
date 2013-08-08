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

public class ExpressionFormula extends Expression
{
	String name;
	Expression definition;
	
	// Constructors
	
	public ExpressionFormula()
	{
	}
	
	public ExpressionFormula(String n)
	{
		name = n;
		definition = null;
	}
			
	// Set method
	
	public void setName(String n) 
	{
		name = n;
	}
	
	public void setDefinition(Expression definition) 
	{
		this.definition = definition;
	}
	
	// Get method
	
	public String getName()
	{
		return name;
	}
	
	public Expression getDefinition()
	{
		return definition;
	}
	
	// Methods required for Expression:
	
	/**
	 * Is this expression constant?
	 */
	public boolean isConstant()
	{
		// Unless defined, don't know so err on the side of caution
		return definition == null ? false : definition.isConstant();
	}

	@Override
	public boolean isProposition()
	{
		// Unless defined, don't know so err on the side of caution
		return definition == null ? false : definition.isProposition();
	}
	
	/**
	 * Evaluate this expression, return result.
	 * Note: assumes that type checking has been done already.
	 */
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		// Should only be called (if at all) after definition has been set
		if (definition == null)
			throw new PrismLangException("Could not evaluate formula", this);
		else
			return definition.evaluate(ec);
	}

	@Override
	public boolean returnsSingleValue()
	{
		// Unless defined, don't know so err on the side of caution
		return definition == null ? false : definition.returnsSingleValue();
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
		ExpressionFormula ret = new ExpressionFormula(name);
		ret.setDefinition(definition == null ? null : definition.deepCopy());
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
