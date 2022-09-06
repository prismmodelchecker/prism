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

public class ExpressionFormula extends Expression
{
	protected String name;
	protected Expression definition;
	
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
	
	@Override
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
	
	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		// Should only be called (if at all) after definition has been set
		if (definition == null)
			throw new PrismLangException("Could not evaluate formula", this);
		else
			return definition.evaluate(ec);
	}

	@Override
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException
	{
		// Should only be called (if at all) after definition has been set
		if (definition == null)
			throw new PrismLangException("Could not evaluate formula", this);
		else
			return definition.evaluateExact(ec);
	}

	@Override
	public boolean returnsSingleValue()
	{
		// Unless defined, don't know so err on the side of caution
		return definition == null ? false : definition.returnsSingleValue();
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
		ExpressionFormula ret = new ExpressionFormula(name);
		ret.setDefinition(definition == null ? null : definition.deepCopy());
		ret.setPosition(this);
		return ret;
	}

	// Standard methods
	
	@Override
	public String toString()
	{
		return name;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((definition == null) ? 0 : definition.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		ExpressionFormula other = (ExpressionFormula) obj;
		if (definition == null) {
			if (other.definition != null)
				return false;
		} else if (!definition.equals(other.definition))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}

//------------------------------------------------------------------------------
