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

import prism.PrismException;
import simulator.*;

public class ExpressionUnaryMinus extends ExpressionUnary
{
	// constructors
	
	public ExpressionUnaryMinus()
	{
		super();
	}
	
	public ExpressionUnaryMinus(Expression e)
	{
		super(e);
	}
	
	// create and return a new expression by renaming

	public Expression rename(RenamedModule rm) throws PrismException
	{
		return new ExpressionUnaryMinus(operand.rename(rm));
	}
	
	// create and return a new expression by expanding constants

	public Expression expandConstants(ConstantList constantList) throws PrismException
	{
		return new ExpressionUnaryMinus(operand.expandConstants(constantList));
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		int t;
		
		// check operand is int or double
		t = operand.getType();
		if (t == Expression.BOOLEAN) {
			throw new PrismException("Type error in expression \"" + toString() + "\"");
		}
		
		// type is same as operand
		setType(t);
	}

	// evaluate
	
	public Object evaluate(Values constantValues, Values varValues) throws PrismException
	{
		Object o;
		
		o = operand.evaluate(constantValues, varValues);
		if (o instanceof Integer) return new Integer(-((Integer)o).intValue());
		else return new Double(-((Double)o).doubleValue());
	}

	/**
	 *	Convert and build simulator expression data structure
	 */
	public long toSimulator(SimulatorEngine sim) throws SimulatorException
	{
		int theType = 0;
		try
		{
			theType = getType();
		}
		catch(PrismException e)
		{
			throw new SimulatorException("Error when creating unary minus operator:\n"+e.getMessage());
		}
		
		if(theType == Expression.DOUBLE)
			return SimulatorEngine.createRealMinus(new ExpressionDouble(0).toSimulator(sim), operand.toSimulator(sim));
		else
			return SimulatorEngine.createNormalMinus(new ExpressionInt(0).toSimulator(sim), operand.toSimulator(sim));
	}

	// convert to string
	
	public String toString()
	{
		return "-" + operand;
	}
}

//------------------------------------------------------------------------------
