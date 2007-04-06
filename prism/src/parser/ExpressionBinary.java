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

public abstract class ExpressionBinary extends Expression
{
	// pair of operands
	
	protected Expression operand1;
	protected Expression operand2;
	
	// constructors
	
	public ExpressionBinary()
	{
	}
	
	public ExpressionBinary(Expression e1, Expression e2)
	{
		operand1 = e1;
		operand2 = e2;
	}
	
	// set methods
	
	public void setOperand1(Expression e1)
	{
		operand1 = e1;
	}
	
	public void setOperand2(Expression e2)
	{
		operand2 = e2;
	}
	
	// get methods
	
	public Expression getOperand1()
	{
		return operand1;
	}
	
	public Expression getOperand2()
	{
		return operand2;
	}
	
	// find all formulas (i.e. locate idents which are formulas)
	
	public Expression findAllFormulas(FormulaList formulaList) throws PrismException
	{
		operand1 = operand1.findAllFormulas(formulaList);
		operand2 = operand2.findAllFormulas(formulaList);
		
		return this;
	}
	
	// get all formulas (put into vector)
	
	public void getAllFormulas(Vector v) throws PrismException
	{
		operand1.getAllFormulas(v);
		operand2.getAllFormulas(v);
	}

	// expand any formulas
	
	public Expression expandFormulas(FormulaList formulaList) throws PrismException
	{
		operand1 = operand1.expandFormulas(formulaList);
		operand2 = operand2.expandFormulas(formulaList);
		
		return this;
	}
		
	// find all constants (i.e. locate idents which are constants)
	
	public Expression findAllConstants(ConstantList constantList) throws PrismException
	{
		operand1 = operand1.findAllConstants(constantList);
		operand2 = operand2.findAllConstants(constantList);
		
		return this;
	}
	
	// get all constants (put into vector)
	
	public void getAllConstants(Vector v) throws PrismException
	{
		operand1.getAllConstants(v);
		operand2.getAllConstants(v);
	}

	// find all variables (i.e. locate idents which are variables)
	
	public Expression findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		operand1 = operand1.findAllVars(varIdents, varTypes);
		operand2 = operand2.findAllVars(varIdents, varTypes);
		
		return this;
	}

	// get all vars (put into vector)
	
	public void getAllVars(Vector v) throws PrismException
	{
		operand1.getAllVars(v);
		operand2.getAllVars(v);
	}

	// is expression constant?
	
	public boolean isConstant()
	{
		if (!operand1.isConstant()) return false;
		if (!operand2.isConstant()) return false;
		
		return true;
	}
	
	// check expression is ok
	
	public void check() throws PrismException
	{
		operand1.check();
		operand2.check();
	}

	// get expression tree as string
	
	public String toTreeString(int indent)
	{
		String s;
		
		s = super.toTreeString(indent);
		s += operand1.toTreeString(indent+1);
		s += operand2.toTreeString(indent+1);
		
		return s;
	}
}

//------------------------------------------------------------------------------
