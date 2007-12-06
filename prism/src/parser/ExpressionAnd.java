//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

public class ExpressionAnd extends ExpressionNary
{
	// create and return a new expression by renaming

	public Expression rename(RenamedModule rm) throws PrismException
	{
		int i, n;
		ExpressionAnd e;
		
		e = new ExpressionAnd();		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			e.addOperand(getOperand(i).rename(rm));
		}
		
		return e;
	}
	
	// create and return a new expression by expanding constants

	public Expression expandConstants(ConstantList constantList) throws PrismException
	{
		int i, n;
		ExpressionAnd e;
		
		e = new ExpressionAnd();		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			e.addOperand(getOperand(i).expandConstants(constantList));
		}
		
		return e;
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		int i, n;
		
		// make sure that all operands are booleans
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			if (getOperand(i).getType() != Expression.BOOLEAN) {
				throw new PrismException("Type error in expression \"" + toString() + "\"");
			}
		}
		
		// result is always boolean
		setType(Expression.BOOLEAN);
	}

	// evaluate
	
	public Object evaluate(Values constantValues, Values varValues) throws PrismException
	{
		int i, n;
		
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			if (!getOperand(i).evaluateBoolean(constantValues, varValues)) {
				return new Boolean(false);
			}
		}
		
		return new Boolean(true);
	}

	/**
	 *	Convert and build AND expression into simulator
	 */
	public long toSimulator(SimulatorEngine sim) throws SimulatorException
	{
		int n = getNumOperands();
		if( n < 1 )
			throw new SimulatorException("Expression \"" + toString() + "\" has zero operands");
		long[] exprs = new long[n];
		
		//Collect operands pointers in an array
		for (int i = 0; i < n; i++) 
			exprs[i] = getOperand(i).toSimulator(sim);
		
		return SimulatorEngine.createAnd(exprs);
	}

	// convert to string
	
	public String toString()
	{
		int i, n;
		String s = "";
		
		n = getNumOperands();
		for (i = 0; i < n-1; i++) {
			s = s + getOperand(i) + "&";
		}
		if (n > 0) {
			s = s + getOperand(n-1);
		}
		
		return s;
	}
}

//------------------------------------------------------------------------------
