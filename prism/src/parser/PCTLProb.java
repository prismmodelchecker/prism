//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker, Andrew Hinton
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
import simulator.*;

public class PCTLProb extends PCTLFormulaUnary
{
	String relOp;
	Expression prob;
	PCTLFormula filter;
	boolean filterMinReq;
	boolean filterMaxReq;
	
	// constructor
	
	public PCTLProb(PCTLFormula f, String r, Expression p)
	{
		super(f);
		relOp = r;
		prob = p;
		filter = null;
		filterMinReq = false;
		filterMaxReq = false;
	}

	public void setFilter(PCTLFormula f)
	{
		filter = f;
	}

	public void setFilterMinRequested(boolean b)
	{
		filterMinReq = b;
	}

	public void setFilterMaxRequested(boolean b)
	{
		filterMaxReq = b;
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

	public boolean filterMinRequested()
	{
		return filterMinReq;
	}

	public boolean filterMaxRequested()
	{
		return filterMaxReq;
	}

	public boolean noFilterRequests()
	{
		return !(filterMinReq | filterMaxReq);
	}

	// get "name" of the result of this formula (used for y-axis of any graphs plotted)
	
	public String getResultName()
	{
		// default is just "Result", will be overridden where necessary
		return (prob == null) ? "Probability" : "Result";
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

	// compute the max depth of prob operator nestings
	
	public int computeMaxNested()
	{
		return 1 + super.computeMaxNested();
	}

	// convert to apmc data structures
	
	public int toApmc(Apmc apmc) throws ApmcException
	{
		// register probability
		Double p = null;
		if(prob != null)
		{
			try {
				p = new Double(prob.evaluateDouble(apmc.getConstantValues(), apmc.getVarValues()));
			} catch (PrismException e) {
				throw new ApmcException(e.getMessage());
			}
		}
		apmc.registerProb(relOp, p);
		
		// do conversion of path operator
		return operand.toApmc(apmc);
	}

	/**
	 *	Convert and build simulator data structures
	 *	Note: Although the simulator supports Prob operators, they are only supported
	 *	if they are the top-most formulae, and so are not handled by a toSimulator method.
	 *	Therefore, this method will only be called in error and hence throws an
	 *	exception.
	 */
	public int toSimulator(SimulatorEngine sim ) throws SimulatorException
	{
		throw new SimulatorException("The PRISM Simulator does accept PCTL formulae with nested P operators");
	}

	// convert to string
	
	public String toString()
	{
		String s = "";
		
		s += "P" + relOp;
		s += (prob==null) ? "?" : prob.toString();
		s += " [ " + operand;
		if (filter != null) {
			s += " {" + filter + "}";
			if (filterMinReq) s += "{min}";
			if (filterMaxReq) s += "{max}";
		}
		s += " ]";
		
		return s;
	}
}

//------------------------------------------------------------------------------
