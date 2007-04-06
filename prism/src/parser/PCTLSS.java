//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
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
import simulator.*;

public class PCTLSS extends PCTLFormulaUnary
{
	String relOp;
	Expression prob;
	PCTLFormula filter;
	
	// constructor
	
	public PCTLSS(PCTLFormula f, String r, Expression p)
	{
		super(f);
		relOp = r;
		prob = p;
		filter = null;
	}

	public void setFilter(PCTLFormula f)
	{
		filter = f;
	}

	public String getRelOp()
	{
		return relOp;
	}
	
	public Expression getProb()
	{
		return prob;
	}

	public PCTLFormula getFilter()
	{
		return filter;
	}

	// check all labels (make sure the referred labels exist)
	
	public void checkLabelIdents(LabelList labelList) throws PrismException
	{
		// call superclass (unary)
		super.checkLabelIdents(labelList);
		// also do filter
		if (filter != null) filter.checkLabelIdents(labelList);
	}

	// find all constants (i.e. locate idents which are constants)
	
	public PCTLFormula findAllConstants(ConstantList constantList) throws PrismException
	{
		// call superclass (unary)
		super.findAllConstants(constantList);
		// also do prob expression
		if (prob != null) prob = prob.findAllConstants(constantList);
		// also do filter
		if (filter != null) filter = filter.findAllConstants(constantList);
		
		return this;
	}
	
	// find all variables (i.e. locate idents which are variables)
	
	public PCTLFormula findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		// call superclass (unary)
		super.findAllVars(varIdents, varTypes);
		// also do prob expression
		if (prob != null) prob = prob.findAllVars(varIdents, varTypes);
		// also do filter
		if (filter != null) filter = filter.findAllVars(varIdents, varTypes);
		
		return this;
	}
	
	// type check
	
	public void typeCheck() throws PrismException
	{
		// call superclass (unary)
		super.typeCheck();
		// check prob is double
		if (prob != null) if (!Expression.canAssignTypes(Expression.DOUBLE, prob.getType())) {
			throw new PrismException("Probability \"" + prob + "\" is the wrong type");
		}
		// check filter is boolean
		if (filter != null) if (filter.getType() != Expression.BOOLEAN) {
			throw new PrismException("Expression \"" + filter + "\" must be Boolean");
		}
		// set type if necesary (default of Boolean already assigned by superclass)
		if (prob == null) setType(Expression.DOUBLE);
	}

	// check everything is ok
	
	public void check() throws PrismException
	{
		// call superclass (unary)
		super.check();
		// check prob is ok and is constant
		if (prob != null) {
			prob.check();
			if (!prob.isConstant()) {
				throw new PrismException("Probability \"" + prob + "\" is not constant");
			}
		}
		// check filter
		if (filter != null) filter.check();
	}

	// check if formula is valid pctl - no!
	
	public void checkValidPCTL() throws PrismException
	{
		throw new PrismException("PCTL formulas cannot contain the steady-state operator");
	}

	// check if formula is valid csl - yes
	
	public void checkValidCSL() throws PrismException
	{
		// ok
	}

	// compute the max depth of prob operator nestings
	
	public int computeMaxNested()
	{
		return 1 + super.computeMaxNested();
	}

	/**
	 *	Convert and build simulator data structures
	 */
	public int toSimulator(SimulatorEngine sim ) throws SimulatorException
	{
		throw new SimulatorException("The PRISM Simulator does accept PCTL formulae with SS operators");
	}

	// convert to string
	
	public String toString()
	{
		String s = "";
		
		s += "S" + relOp;
		s += (prob==null) ? "?" : prob.toString();
		s += " [ " + operand;
		if (filter != null) s+= " {" + filter + "}";
		s += " ]";
		
		return s;
	}
}

//------------------------------------------------------------------------------
