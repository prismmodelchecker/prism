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

public class ExpressionTrue extends Expression
{
	// constructor
	
	public ExpressionTrue()
	{
		setType(Expression.BOOLEAN);
	}

	// find all formulas (i.e. locate idents which are formulas)
	
	public Expression findAllFormulas(FormulaList formulaList) throws PrismException
	{
		return this;
	}

	// get all formulas (put into vector)
	
	public void getAllFormulas(Vector v) throws PrismException
	{
	}

	// expand any formulas
	
	public Expression expandFormulas(FormulaList formulaList)
	{
		return this;
	}
	
	// create and return a new expression by renaming

	public Expression rename(RenamedModule rm) throws PrismException
	{
		return new ExpressionTrue();
	}
	
	// find all constants (i.e. locate idents which are constants)
	
	public Expression findAllConstants(ConstantList constantList) throws PrismException
	{
		return this;
	}

	// get all constants (put into vector)
	
	public void getAllConstants(Vector v) throws PrismException
	{
	}

	// create and return a new expression by expanding constants

	public Expression expandConstants(ConstantList constantList) throws PrismException
	{
		return new ExpressionTrue();
	}

	// find all variables (i.e. locate idents which are variables)
	
	public Expression findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		return this;
	}
	
	// get all vars (put into vector)
	
	public void getAllVars(Vector v) throws PrismException
	{
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		// never any probs
		return;
	}

	// is expression constant?
	
	public boolean isConstant()
	{
		return true;
	}
	
	// check expression is ok
	
	public void check() throws PrismException
	{
		// it's ok
		return;
	}
	
	// evaluate
	
	public Object evaluate(Values constantValues, Values varValues) throws PrismException
	{
		return new Boolean(true);
	}

	/**
	 *	Convert and build simulator expression data structure
	 */
	public int toSimulator(SimulatorEngine sim) throws SimulatorException
	{
		return SimulatorEngine.createBoolean(true);
	}

	// convert to string
	
	public String toString()
	{
		return "true";
	}
}

//------------------------------------------------------------------------------
