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

public class ExpressionFormula extends Expression
{
	String name;
	
	// constructors
	
	public ExpressionFormula()
	{
	}
	
	public ExpressionFormula(String n)
	{
		name = n;
	}
			
	// set method
	
	public void setName(String n) 
	{
		name = n;
	}
	
	// get method
	
	public String getName()
	{
		return name;
	}
		
	// find all formulas (i.e. locate idents which are formulas)
	
	public Expression findAllFormulas(FormulaList formulaList) throws PrismException
	{
		// n/a: ExpressionFormula objects are never created until _after_ the findAllFormulas phase
		return this;
	}
	
	// get all formulas (put into vector)
	
	public void getAllFormulas(Vector v) throws PrismException
	{
		if (!v.contains(name)) {
			v.addElement(name);
		}
	}

	// expand any formulas
	
	public Expression expandFormulas(FormulaList formulaList) throws PrismException
	{
		int i;
		Expression e;
		
		// see if identifier corresponds to a formula
		i = formulaList.getFormulaIndex(name);
		if (i != -1) {
			// if so, replace it with the corresponding expression
			e = formulaList.getFormula(i);
			// but also recursively expand that
			e = e.expandFormulas(formulaList);
			// put in brackets so precedence is preserved
			// (for display purposes only)
			e = new ExpressionBrackets(e);
			// return replacement expression
			return e;
		}
		else {
			// error (should never happen)
			throw new PrismException("Formula \"" + name + "\" undefined");
		}
	}
	
	// create and return a new expression by renaming

	public Expression rename(RenamedModule rm) throws PrismException
	{
		// formulas are expanded before renaming so this should never be called
		// but just in case...

		return new ExpressionFormula(name);
	}
		
	// find all constants (i.e. locate idents which are constants)
	
	public Expression findAllConstants(ConstantList constantList) throws PrismException
	{
		// n/a - ExpressionFormula objects should have been expanded by the time this is called
		return this;
	}

	// get all constants (put into vector)
	
	public void getAllConstants(Vector v) throws PrismException
	{
		// n/a - ExpressionFormula objects should have been expanded by the time this is called
	}

	// create and return a new expression by expanding constants

	public Expression expandConstants(ConstantList constantList) throws PrismException
	{
		// formulas are expanded before constants so this should never be called
		// but just in case...

		return new ExpressionFormula(name);
	}

	// find all variables (i.e. locate idents which are variables)
	
	public Expression findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		// n/a - ExpressionFormula objects should have been expanded by the time this is called
		return this;
	}

	// get all vars (put into vector)
	
	public void getAllVars(Vector v) throws PrismException
	{
		// n/a - ExpressionFormula objects should have been expanded by the time this is called
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		// should never happpen
		throw new PrismException("Cannot determine type of a formula");
	}

	// is expression constant?
	
	public boolean isConstant()
	{
		// don't know - err on the side of caution
		return false;
	}

	// check expression is ok
	
	public void check() throws PrismException
	{
		// error - this should have been expanded by now
		throw new PrismException("Unexpanded formula \"" + name + "\"");
	}

	// evaluate
	
	public Object evaluate(Values constantValues, Values varValues) throws PrismException
	{
		// this should never be called
		throw new PrismException("Could not evaluate formula \"" + name +  "\"");
	}

	/**
	 *	Convert and build simulator expression data structure
	 */
	public long toSimulator(SimulatorEngine sim) throws SimulatorException
	{
		// this should never be called - this should have been expanded by now
		throw new SimulatorException("Unexpanded formula \"" + name + "\"");
	}

	// convert to string
	
	public String toString()
	{
		return name;
	}
}

//------------------------------------------------------------------------------
