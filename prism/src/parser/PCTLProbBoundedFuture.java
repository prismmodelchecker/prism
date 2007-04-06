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
import simulator.*;

public class PCTLProbBoundedFuture extends PCTLFormulaUnary
{
	Expression lBound = null; // none if null, i.e. zero
	Expression uBound = null; // none if null, i.e. infinity
	
	// constructor
	
	public PCTLProbBoundedFuture(PCTLFormula f, Expression l, Expression u)
	{
		super(f);
		lBound = l;
		uBound = u;
	}

	public Expression getLowerBound()
	{
		return lBound;
	}

	public Expression getUpperBound()
	{
		return uBound;
	}

	// find all constants (i.e. locate idents which are constants)
	
	public PCTLFormula findAllConstants(ConstantList constantList) throws PrismException
	{
		// call superclass (binary)
		super.findAllConstants(constantList);
		// also do bound expressions
		if (lBound != null) lBound = lBound.findAllConstants(constantList);
		if (uBound != null) uBound = uBound.findAllConstants(constantList);
		
		return this;
	}
	
	// find all variables (i.e. locate idents which are variables)
	
	public PCTLFormula findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		// call superclass (binary)
		super.findAllVars(varIdents, varTypes);
		// also do bound expressions
		if (lBound != null) lBound = lBound.findAllVars(varIdents, varTypes);
		if (uBound != null) uBound = uBound.findAllVars(varIdents, varTypes);
		
		return this;
	}
	
	// type check
	
	public void typeCheck() throws PrismException
	{
		// call superclass (binary)
		super.typeCheck();
		// check any time bounds are of type int/double
		if (lBound != null) if (!(lBound.getType() == Expression.INT || lBound.getType() == Expression.DOUBLE)) {
			throw new PrismException("Time bound \"" + lBound + "\" is the wrong type");
		}
		if (uBound != null) if (!(uBound.getType() == Expression.INT || uBound.getType() == Expression.DOUBLE)) {
			throw new PrismException("Time bound \"" + uBound + "\" is the wrong type");
		}
	}

	// check everything is ok
	
	public void check() throws PrismException
	{
		// call superclass (binary)
		super.check();
		// check any time bounds are ok and constant
		if (lBound != null) {
			lBound.check();
			if (!lBound.isConstant()) {
				throw new PrismException("Time bound \"" + lBound + "\" is not constant");
			}
		}
		if (uBound != null) {
			uBound.check();
			if (!uBound.isConstant()) {
				throw new PrismException("Time bound \"" + uBound + "\" is not constant");
			}
		}
	}

	// check if formula is valid pctl
	
	public void checkValidPCTL() throws PrismException
	{
		// must have upper bound only
		if (lBound != null) throw new PrismException("PCTL bounded future formulas must have bounds of the form \"<=k\"");
		// and it must be an integer
		if (uBound.getType() != Expression.INT) throw new PrismException("Bounds in PCTL bounded future formulas must be of type integer");
	}

	// check if formula is valid csl
	
	public void checkValidCSL() throws PrismException
	{
		// ok
	}

	/**
	 *	Convert and build simulator data structures
	 *	Note: Although the simulator supports ProbBoundedFuture operators, they are 
	 *	only supported if they belong to a top-most Prob formulae, and so are not 
	 *	handled by a toSimulator method.  Therefore, this method will only be called 
	 *	in error and hence throws an exception.
	 */
	public int toSimulator(SimulatorEngine sim ) throws SimulatorException
	{
		throw new SimulatorException("Unexpected Error when loading PCTL Formula into Simulator - BoundedFuture toSimulator should never be called");
	}

	// convert to string
	
	public String toString()
	{
		String s = "";
		
		s += "F";
		if (lBound == null) {
			s += "<=" + uBound;
		}
		else if (uBound == null) {
			s += ">=" + lBound;
		}
		else {
			s += "[" + lBound + "," + uBound + "]";
		}
		s += " " + operand;
		
		return s;
	}
}

//------------------------------------------------------------------------------
