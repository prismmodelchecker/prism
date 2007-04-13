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

public class PCTLRewardInst extends PCTLFormula
{
	Expression time;

	// constructor
	
	public PCTLRewardInst(Expression t)
	{
		time = t;
	}

	public Expression getTime()
	{
		return time;
	}

	// find all constants (i.e. locate idents which are constants)
	
	public PCTLFormula findAllConstants(ConstantList constantList) throws PrismException
	{
		// do time
		if (time != null) time = time.findAllConstants(constantList);
		
		return this;
	}

	// find all variables (i.e. locate idents which are variables)
	
	public PCTLFormula findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		// do time
		if (time != null) time = time.findAllVars(varIdents, varTypes);
		
		return this;
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		// check time is integer/double
		if (time != null) if (time.getType() == Expression.BOOLEAN) {
			throw new PrismException("Time \"" + time + "\" must be an integer or a double");
		}
		// set type
		setType(Expression.BOOLEAN);
	}

	// check everything is ok
	
	public void check() throws PrismException
	{
		// check time is ok and is constant
		if (time != null) {
			time.check();
			if (!time.isConstant()) {
				throw new PrismException("Time \"" + time + "\" is not constant");
			}
		}
	}

	/**
	 *	Convert and build simulator data structures
	 *	Note: Although the simulator supports Reward operators, they are 
	 *	only supported if they belong to a top-most Reward formulae, and so are not 
	 *	handled by a toSimulator method.  Therefore, this method will only be called 
	 *	in error and hence throws an exception.
	 */
	public long toSimulator(SimulatorEngine sim ) throws SimulatorException
	{
		throw new SimulatorException("Unexpected error when loading PCTL formula into simulator - rewards method should never be called");
	}

	// convert to string
	
	public String toString()
	{
		return "I=" + time;
	}
}
