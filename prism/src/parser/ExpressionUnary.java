//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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

package parser;

import java.util.Vector;

import prism.PrismException;

public abstract class ExpressionUnary extends Expression
{
	// single operand
	
	protected Expression operand;
	
	// constructors
	
	public ExpressionUnary()
	{
	}
	
	public ExpressionUnary(Expression e)
	{
		operand = e;
	}
	
	// set method
	
	public void setOperand(Expression e)
	{
		operand = e;
	}
	
	// get method
	
	public Expression getOperand()
	{
		return operand;
	}
	
	// find all formulas (i.e. locate idents which are formulas)
	
	public Expression findAllFormulas(FormulaList formulaList) throws PrismException
	{
		operand = operand.findAllFormulas(formulaList);
		
		return this;
	}
	
	// get all formulas (put into vector)
	
	public void getAllFormulas(Vector v) throws PrismException
	{
		operand.getAllFormulas(v);
	}

	// expand any formulas
	
	public Expression expandFormulas(FormulaList formulaList) throws PrismException
	{
		operand = operand.expandFormulas(formulaList);
		
		return this;
	}
		
	// find all constants (i.e. locate idents which are constants)
	
	public Expression findAllConstants(ConstantList constantList) throws PrismException
	{
		operand = operand.findAllConstants(constantList);
		
		return this;
	}
	
	// get all constants (put into vector)
	
	public void getAllConstants(Vector v) throws PrismException
	{
		operand.getAllConstants(v);
	}

	// find all variables (i.e. locate idents which are variables)
	
	public Expression findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		operand = operand.findAllVars(varIdents, varTypes);
		
		return this;
	}

	// get all vars (put into vector)
	
	public void getAllVars(Vector v) throws PrismException
	{
		operand.getAllVars(v);
	}
	
	// is expression constant?
	
	public boolean isConstant()
	{
		return operand.isConstant();
	}
	
	// check expression is ok
	
	public void check() throws PrismException
	{
		operand.check();
	}

	// get expression tree as string
	
	public String toTreeString(int indent)
	{
		String s;
		
		s = super.toTreeString(indent);
		s += operand.toTreeString(indent+1);
		
		return s;
	}
}

//------------------------------------------------------------------------------
