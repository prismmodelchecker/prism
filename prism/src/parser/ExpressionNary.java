//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker
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
import apmc.*;

public abstract class ExpressionNary extends Expression
{
	// vector of operands
	
	protected Vector operands;
	
	// constructor
	
	public ExpressionNary()
	{
		operands = new Vector();
	}
	
	// set methods
	
	public void addOperand(Expression e)
	{
		operands.addElement(e);
	}
		
	public void setOperand(int i, Expression e)
	{
		operands.setElementAt(e, i);
	}
			
	// get methods
	
	public int getNumOperands()
	{
		return operands.size();
	}
	
	public Expression getOperand(int i)
	{
		return (Expression)operands.elementAt(i);
	}
		
	// find all formulas (i.e. locate idents which are formulas)
	
	public Expression findAllFormulas(FormulaList formulaList) throws PrismException
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			setOperand(i, getOperand(i).findAllFormulas(formulaList));
		}
		
		return this;
	}
	
	// get all formulas (put into vector)
	
	public void getAllFormulas(Vector v) throws PrismException
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			getOperand(i).getAllFormulas(v);
		}
	}

	// expand any formulas
	
	public Expression expandFormulas(FormulaList formulaList) throws PrismException
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			setOperand(i, getOperand(i).expandFormulas(formulaList));
		}
		
		return this;
	}
	
	// find all constants (i.e. locate idents which are constants)
	
	public Expression findAllConstants(ConstantList constantList) throws PrismException
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			setOperand(i, getOperand(i).findAllConstants(constantList));
		}
		
		return this;
	}
	
	// get all constants (put into vector)
	
	public void getAllConstants(Vector v) throws PrismException
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			getOperand(i).getAllConstants(v);
		}
	}

	// find all variables (i.e. locate idents which are variables)
	
	public Expression findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			setOperand(i, getOperand(i).findAllVars(varIdents, varTypes));
		}
		
		return this;
	}

	// get all vars (put into vector)
	
	public void getAllVars(Vector v) throws PrismException
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			getOperand(i).getAllVars(v);
		}
	}

	// is expression constant?
	
	public boolean isConstant()
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			if (!getOperand(i).isConstant()) return false;
		}
		
		return true;
	}
	
	// check expression is ok
	
	public void check() throws PrismException
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			getOperand(i).check();
		}
	}

	// convert to apmc data structures
	
	public int toApmc(Apmc apmc) throws ApmcException
	{
	    throw new ApmcException("Unknown Nary operand");
	}

	// get expression tree as string
	
	public String toTreeString(int indent)
	{
		String s;
		int i, n;
		
		s = super.toTreeString(indent);
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			s += getOperand(i).toTreeString(indent+1);
		}
		
		return s;
	}
}

//------------------------------------------------------------------------------
