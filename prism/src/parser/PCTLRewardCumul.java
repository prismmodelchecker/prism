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

public class PCTLRewardCumul extends PCTLFormula
{
	Expression bound;

	// constructor
	
	public PCTLRewardCumul(Expression b)
	{
		bound = b;
	}

	public Expression getBound()
	{
		return bound;
	}

	// find all constants (i.e. locate idents which are constants)
	
	public PCTLFormula findAllConstants(ConstantList constantList) throws PrismException
	{
		// do bound
		if (bound != null) bound = bound.findAllConstants(constantList);
		
		return this;
	}

	// find all variables (i.e. locate idents which are variables)
	
	public PCTLFormula findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		// do bound
		if (bound != null) bound = bound.findAllVars(varIdents, varTypes);
		
		return this;
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		// check bound is int/double
		if (bound != null) if (!(bound.getType() == Expression.INT || bound.getType() == Expression.DOUBLE)) {
			throw new PrismException("Bound \"" + bound + "\" is the wrong type");
		}
		// set type
		setType(Expression.BOOLEAN);
	}

	// check everything is ok
	
	public void check() throws PrismException
	{
		// check bound is ok and is constant
		if (bound != null) {
			bound.check();
			if (!bound.isConstant()) {
				throw new PrismException("Bound \"" + bound + "\" is not constant");
			}
		}
	}

	// check if formula is valid pctl
	
	public void checkValidPCTL() throws PrismException
	{
		// bound must be an integer
		if (bound.getType() != Expression.INT) throw new PrismException("Bounds in PCTL cumulative reward formulas must be of type integer");
	}

	// check if formula is valid csl
	
	public void checkValidCSL() throws PrismException
	{
		// ok
	}

	/**
	 *	Convert and build simulator data structures
	 *	Note: Although the simulator supports Reward operators, they are 
	 *	only supported if they belong to a top-most Reward formulae, and so are not 
	 *	handled by a toSimulator method.  Therefore, this method will only be called 
	 *	in error and hence throws an exception.
	 */
	public int toSimulator(SimulatorEngine sim ) throws SimulatorException
	{
		throw new SimulatorException("Unexpected error when loading PCTL formula into simulator - rewards method should never be called");
	}

	// convert to string
	
	public String toString()
	{
		return "C<=" + bound;
	}
}
