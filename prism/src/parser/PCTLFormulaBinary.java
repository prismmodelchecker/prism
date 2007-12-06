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

package parser;

import java.util.Vector;

import prism.PrismException;

public abstract class PCTLFormulaBinary extends PCTLFormula
{
	// pair of operands
	
	protected PCTLFormula operand1;
	protected PCTLFormula operand2;
	
	// constructors
	
	public PCTLFormulaBinary()
	{
	}
	
	public PCTLFormulaBinary(PCTLFormula e1, PCTLFormula e2)
	{
		operand1 = e1;
		operand2 = e2;
	}
	
	// set methods
	
	public void setOperand1(PCTLFormula e1)
	{
		operand1 = e1;
	}
	
	public void setOperand2(PCTLFormula e2)
	{
		operand2 = e2;
	}

	// get methods
	
	public PCTLFormula getOperand1()
	{
		return operand1;
	}
	
	public PCTLFormula getOperand2()
	{
		return operand2;
	}

	// check all labels (make sure the referred labels exist)
	
	public void checkLabelIdents(LabelList labelList) throws PrismException
	{
		operand1.checkLabelIdents(labelList);
		operand2.checkLabelIdents(labelList);
	}

	// find all formulas (i.e. locate idents which are formulas)
	
	public PCTLFormula findAllFormulas(FormulaList formulaList) throws PrismException
	{
		operand1 = operand1.findAllFormulas(formulaList);
		operand2 = operand2.findAllFormulas(formulaList);
		
		return this;
	}
	
	// expand any formulas
	
	public PCTLFormula expandFormulas(FormulaList formulaList) throws PrismException
	{
		operand1 = operand1.expandFormulas(formulaList);
		operand2 = operand2.expandFormulas(formulaList);
		
		return this;
	}
		
	// find all constants (i.e. locate idents which are constants)
	
	public PCTLFormula findAllConstants(ConstantList constantList) throws PrismException
	{
		operand1 = operand1.findAllConstants(constantList);
		operand2 = operand2.findAllConstants(constantList);
		
		return this;
	}
	
	// find all variables (i.e. locate idents which are variables)
	
	public PCTLFormula findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		operand1 = operand1.findAllVars(varIdents, varTypes);
		operand2 = operand2.findAllVars(varIdents, varTypes);
		
		return this;
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		// make sure operands are boolean
		if (operand1.getType() != Expression.BOOLEAN) {
			throw new PrismException("Type error in formula \"" + toString() + "\"");
		}
		if (operand2.getType() != Expression.BOOLEAN) {
			throw new PrismException("Type error in formula \"" + toString() + "\"");
		}
		
		// set type
		setType(Expression.BOOLEAN);
	}

	// check everything is ok
	
	public void check() throws PrismException
	{
		operand1.check();
		operand2.check();
	}
	
	// check if formula is valid pctl
	
	public void checkValidPCTL() throws PrismException
	{
		operand1.checkValidPCTL();
		operand2.checkValidPCTL();
	}
	
	// check if formula is valid csl
	
	public void checkValidCSL() throws PrismException
	{
		operand1.checkValidCSL();
		operand2.checkValidCSL();
	}

	// compute the max depth of prob operator nestings
	
	public int computeMaxNested()
	{
		return Math.max(operand1.computeMaxNested(), operand2.computeMaxNested());
	}

	// get parse tree as string
	
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
