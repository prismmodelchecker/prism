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

public abstract class PCTLFormulaUnary extends PCTLFormula
{
	// single operand
	
	protected PCTLFormula operand;
	
	// constructors
	
	public PCTLFormulaUnary()
	{
	}
	
	public PCTLFormulaUnary(PCTLFormula e)
	{
		operand = e;
	}
	
	// set method
	
	public void setOperand(PCTLFormula e)
	{
		operand = e;
	}
	
	// get method
	
	public PCTLFormula getOperand()
	{
		return operand;
	}

	// check all labels (make sure the referred labels exist)
	
	public void checkLabelIdents(LabelList labelList) throws PrismException
	{
		operand.checkLabelIdents(labelList);
	}

	// find all formulas (i.e. locate idents which are formulas)
	
	public PCTLFormula findAllFormulas(FormulaList formulaList) throws PrismException
	{
		operand = operand.findAllFormulas(formulaList);
		
		return this;
	}
	
	// expand any formulas
	
	public PCTLFormula expandFormulas(FormulaList formulaList) throws PrismException
	{
		operand = operand.expandFormulas(formulaList);
		
		return this;
	}
		
	// find all constants (i.e. locate idents which are constants)
	
	public PCTLFormula findAllConstants(ConstantList constantList) throws PrismException
	{
		operand = operand.findAllConstants(constantList);
		
		return this;
	}
	
	// find all variables (i.e. locate idents which are variables)
	
	public PCTLFormula findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		operand = operand.findAllVars(varIdents, varTypes);
		
		return this;
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		// make sure operand is boolean
		if (operand.getType() != Expression.BOOLEAN) {
			throw new PrismException("Type error in formula \"" + toString() + "\"");
		}
		
		// set type
		setType(Expression.BOOLEAN);
	}

	// check everything is ok
	
	public void check() throws PrismException
	{
		operand.check();
	}
	
	// check if formula is valid pctl
	
	public void checkValidPCTL() throws PrismException
	{
		operand.checkValidPCTL();
	}
	
	// check if formula is valid csl
	
	public void checkValidCSL() throws PrismException
	{
		operand.checkValidCSL();
	}

	// compute the max depth of prob operator nestings
	
	public int computeMaxNested()
	{
		return operand.computeMaxNested();
	}

	// get parse tree as string
	
	public String toTreeString(int indent)
	{
		String s;
		
		s = super.toTreeString(indent);
		s += operand.toTreeString(indent+1);
		
		return s;
	}
}

//------------------------------------------------------------------------------
