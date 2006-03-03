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

// note: although this makes no difference to the meaning
// of the expression, it means we can keep the user's
// original bracketting for display purposes

public class ExpressionBrackets extends ExpressionUnary
{
	// constructors
	
	public ExpressionBrackets()
	{
	}

	public ExpressionBrackets(Expression e)
	{
		super(e);
	}

	// create and return a new expression by renaming

	public Expression rename(RenamedModule rm) throws PrismException
	{
		return new ExpressionBrackets(operand.rename(rm));
	}

	// create and return a new expression by expanding constants

	public Expression expandConstants(ConstantList constantList) throws PrismException
	{
		return new ExpressionBrackets(operand.expandConstants(constantList));
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		setType(operand.getType());
	}

	// evaluate
	
	public Object evaluate(Values constantValues, Values varValues) throws PrismException
	{
		return operand.evaluate(constantValues, varValues);
	}

	// convert to apmc data structures
	
	public int toApmc(Apmc apmc) throws ApmcException
	{
		return operand.toApmc(apmc);
	}

	/**
	 *	Convert and build simulator expression data structure
	 */
	public int toSimulator(SimulatorEngine sim) throws SimulatorException
	{
		return operand.toSimulator(sim); //don't need to store any bracketing
	}

	// convert to string
		
	public String toString()
	{
		return "(" + operand + ")";
	}
}

//------------------------------------------------------------------------------
