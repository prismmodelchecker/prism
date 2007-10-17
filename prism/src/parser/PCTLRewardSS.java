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

public class PCTLRewardSS extends PCTLFormula
{
	// constructor
	
	public PCTLRewardSS()
	{
	}

	// find all formulas (i.e. locate idents which are formulas)
	
	public PCTLFormula findAllFormulas(FormulaList formulaList) throws PrismException
	{
		return this;
	}
	
	// expand any formulas
	
	public PCTLFormula expandFormulas(FormulaList formulaList) throws PrismException
	{
		return this;
	}
		
	// find all constants (i.e. locate idents which are constants)
	
	public PCTLFormula findAllConstants(ConstantList constantList) throws PrismException
	{
		return this;
	}

	// find all variables (i.e. locate idents which are variables)
	
	public PCTLFormula findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		return this;
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		// set type
		setType(Expression.BOOLEAN);
	}

	// check if formula is valid pctl - no!
	
	public void checkValidPCTL() throws PrismException
	{
		throw new PrismException("PCTL formulas cannot contain the steady-state reward operator");
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
		return "S";
	}
}
