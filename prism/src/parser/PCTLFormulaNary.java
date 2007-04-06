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

public abstract class PCTLFormulaNary extends PCTLFormula
{
	// vector of operands
	
	protected Vector operands;
	
	// constructor
	
	public PCTLFormulaNary()
	{
		operands = new Vector();
	}
	
	// set methods
	
	public void addOperand(PCTLFormula e)
	{
		operands.addElement(e);
	}
		
	public void setOperand(int i, PCTLFormula e)
	{
		operands.setElementAt(e, i);
	}
			
	// get methods
	
	public int getNumOperands()
	{
		return operands.size();
	}
	
	public PCTLFormula getOperand(int i)
	{
		return (PCTLFormula)operands.elementAt(i);
	}

	// check all labels (make sure the referred labels exist)
	
	public void checkLabelIdents(LabelList labelList) throws PrismException
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			getOperand(i).checkLabelIdents(labelList);
		}
	}

	// find all constants (i.e. locate idents which are constants)
	
	public PCTLFormula findAllConstants(ConstantList constantList) throws PrismException
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			setOperand(i, getOperand(i).findAllConstants(constantList));
		}
		
		return this;
	}
	
	// find all variables (i.e. locate idents which are variables)
	
	public PCTLFormula findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			setOperand(i, getOperand(i).findAllVars(varIdents, varTypes));
		}
		
		return this;
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		int i, n;
		
		// make sure operands are boolean
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			if (getOperand(i).getType() != Expression.BOOLEAN) {
				throw new PrismException("Type error in formula \"" + toString() + "\"");
			}
		}
		
		// set type
		setType(Expression.BOOLEAN);
	}

	// check everything is ok
	
	public void check() throws PrismException
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			getOperand(i).check();
		}
	}
	
	// check if formula is valid pctl
	
	public void checkValidPCTL() throws PrismException
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			getOperand(i).checkValidPCTL();
		}
	}

	// check if formula is valid csl
	
	public void checkValidCSL() throws PrismException
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			getOperand(i).checkValidCSL();
		}
	}

	// compute the max depth of prob operator nestings
	
	public int computeMaxNested()
	{
		int i, n, r = 0;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			r = Math.max(r, getOperand(i).computeMaxNested());
		}
		
		return r;
	}

	// get parse tree as string
	
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
