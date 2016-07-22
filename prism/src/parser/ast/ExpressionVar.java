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
	
	@Override
	public boolean isConstant()
	{
		return false;
	}

	@Override
	public boolean isProposition()
	{
		return true;
	}
	
	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		Object res = ec.getVarValue(name, index);
		if (res == null)
			throw new PrismLangException("Could not evaluate variable", this);
		return res;
	}

	@Override
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException
	{
		Object res = ec.getVarValue(name, index);
		if (res == null)
			throw new PrismLangException("Could not evaluate variable", this);
		return BigRational.from(res);
	}

	@Override
	public boolean returnsSingleValue()
	{
		return false;
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
		ExpressionVar expr = new ExpressionVar(name, type);
		expr.setIndex(index);
		expr.setPosition(this);
		return expr;
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
		result = prime * result + index;
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
		ExpressionVar other = (ExpressionVar) obj;
		if (index != other.index)
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
