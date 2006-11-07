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
import simulator.*;

public class ExpressionITE extends ExpressionNary
{
	// specialised constructor (this always has three operands)
	
	public ExpressionITE(Expression c, Expression t, Expression e)
	{
		super();
		addOperand(c);
		addOperand(t);
		addOperand(e);
	}

	// create and return a new expression by renaming
	
	public Expression rename(RenamedModule rm) throws PrismException
	{
		ExpressionITE e;
		
		e = new ExpressionITE(getOperand(0).rename(rm), getOperand(1).rename(rm), getOperand(2).rename(rm));
		
		return e;
	}

	// create and return a new expression by expanding constants
	
	public Expression expandConstants(ConstantList constantList) throws PrismException
	{
		ExpressionITE e;
		
		e = new ExpressionITE(getOperand(0).expandConstants(constantList), getOperand(1).expandConstants(constantList), getOperand(2).expandConstants(constantList));
		
		return e;
	}

	// check expression is ok
	
	public void check() throws PrismException
	{
		if (getNumOperands() != 3) {
			throw new PrismException("If-then-else (\"ite\") operator must have exactly three operands");
		}
		super.check();
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		int t1, t2, t3;
		
		t1 = getOperand(0).getType();
		t2 = getOperand(1).getType();
		t3 = getOperand(2).getType();
		
		// first operand must be boolean
		if (t1 != Expression.BOOLEAN) {
			throw new PrismException("Type error in expression \"" + toString() + "\"");
		}
		// if second or third operand is boolean, both must be
		if (t2==Expression.BOOLEAN) {
			if (t3!=Expression.BOOLEAN) {
				throw new PrismException("Type error in expression \"" + toString() + "\"");
			}
		}
		if (t3==Expression.BOOLEAN) {
			if (t2!=Expression.BOOLEAN) {
				throw new PrismException("Type error in expression \"" + toString() + "\"");
			}
		}
		
		// set type
		if (t2 == Expression.BOOLEAN) setType(Expression.BOOLEAN);
		else if (t2 == Expression.DOUBLE || t3 == Expression.DOUBLE) setType(Expression.DOUBLE);
		else setType(Expression.INT);
	}

	// evaluate
	
	public Object evaluate(Values constantValues, Values varValues) throws PrismException
	{
		if (getOperand(0).evaluateBoolean(constantValues, varValues)) {
			return getOperand(1).evaluate(constantValues, varValues);
		} else {
			return getOperand(2).evaluate(constantValues, varValues);
		}
	}

	/**
	 *	Convert and build simulator expression data structure
	 */
	public int toSimulator(SimulatorEngine sim) throws SimulatorException
	{
		try
		{
			if(getType() == Expression.DOUBLE)
				return SimulatorEngine.createRealIte(
					getOperand(0).toSimulator(sim), 
					getOperand(1).toSimulator(sim), 
					getOperand(2).toSimulator(sim));
			else
				return SimulatorEngine.createIte(
					getOperand(0).toSimulator(sim), 
					getOperand(1).toSimulator(sim), 
					getOperand(2).toSimulator(sim));
		}
		catch(PrismException e)
		{
			throw new SimulatorException(e.getMessage());
		}
	}

	// convert to string
	
	public String toString()
	{
		String s;
		
		s =  getOperand(0) + " ? " + getOperand(1) + " : " + getOperand(2);
		
		return s;
	}
}

//------------------------------------------------------------------------------
