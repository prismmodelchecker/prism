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

public class ExpressionConstant extends Expression
{
	protected String name;
	
	// Constructors
	
	public ExpressionConstant()
	{
	}
	
	public ExpressionConstant(String n, Type t)
	{
		setType(t);
		name = n;
	}
			
	// Set method
	
	public void setName(String n) 
	{
		name = n;
	}
	
	// Get method
	
	public String getName()
	{
		return name;
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
		Object res = ec.getConstantValue(name);
		if (res == null)
			throw new PrismLangException("Could not evaluate constant", this);

		if (res instanceof BigRational) {
			// Constants can also be BigRational, cast to appropriate type
			// This might lose precision
			BigRational r = (BigRational) res;
			return getType().castFromBigRational(r);
		}
		return res;
	}

	@Override
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException
	{
		Object res = ec.getConstantValue(name);
		if (res == null)
			throw new PrismLangException("Could not evaluate constant", this);

		return BigRational.from(res);
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
		Expression ret = new ExpressionConstant(name, type);
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
		ExpressionConstant other = (ExpressionConstant) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}

//------------------------------------------------------------------------------
