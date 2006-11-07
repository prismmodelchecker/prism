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

public class ExpressionIdent extends Expression
{
	String name;
	
	// constructors
	
	public ExpressionIdent()
	{
	}
	
	public ExpressionIdent(String n)
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
		int i;
		Expression e;
		
		// see if identifier corresponds to a formula
		i = formulaList.getFormulaIndex(name);
		if (i != -1) {
			// if so, replace it with an ExpressionFormula object
			return new ExpressionFormula(name);
		}
		else {
			// if not, leave it unchanged
			return this;
		}
	}
	
	// get all formulas (put into vector)
	
	public void getAllFormulas(Vector v) throws PrismException
	{
		// n/a - if this was a formula, it will have been replaced with an ExpressionFormula by now
	}

	// expand any formulas
	
	public Expression expandFormulas(FormulaList formulaList) throws PrismException
	{
		// n/a - if this was a formula, it will have been replaced with an ExpressionFormula by now
		return this;
	}
	
	// create and return a new expression by renaming

	public Expression rename(RenamedModule rm) throws PrismException
	{
		String s;
		
		// change name if it has been renamed
		s = rm.getNewName(name);
		s = (s == null) ? name : s;
		
		return new ExpressionIdent(s);
	}
	
	// find all constants (i.e. locate idents which are constants)
	
	public Expression findAllConstants(ConstantList constantList) throws PrismException
	{
		int i;
		Expression e;
		
		// see if identifier corresponds to a constant
		i = constantList.getConstantIndex(name);
		if (i != -1) {
			// if so, replace it with an ExpressionConstant object
			return new ExpressionConstant(name, constantList.getConstantType(i));
		}
		else {
			// if not, leave it unchanged
			return this;
		}
	}

	// get all constants (put into vector)
	
	public void getAllConstants(Vector v) throws PrismException
	{
		// n/a - if this was a constant, it will have been replaced with an ExpressionConstant by now
	}

	// create and return a new expression by expanding constants

	public Expression expandConstants(ConstantList constantList) throws PrismException
	{
		// nb: constants should have been turned into ExpressionConstants by now
		return new ExpressionIdent(name);
	}

	// find all variables (i.e. locate idents which are variables)
	
	public Expression findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		int i;
		
		// if identifier corresponds to a variable
		i = varIdents.indexOf(name);
		if (i != -1) {
			// replace it with an ExpressionVar object
			return new ExpressionVar(name, ((Integer)varTypes.elementAt(i)).intValue());
		}
		// if not, leave it unchanged
		else {
			return this;
		}
	}
	
	// get all vars (put into vector)
	
	public void getAllVars(Vector v) throws PrismException
	{
		// n/a - if this was a variable, it will have been replaced with an ExpressionVar by now
	}

	// is expression constant?
	
	public boolean isConstant()
	{
		// don't know - err on the side of caution
		return false;
	}
	
	// type check
	
	public void typeCheck() throws PrismException
	{
		// should never happpen
		throw new PrismException("Cannot determine type of undeclared identifier");
	}

	// check expression is ok
	
	public void check() throws PrismException
	{
		// by the time the expression is checked, this should
		// have been converted to an ExpressionVar/ExpressionConstant/...
		throw new PrismException("Undeclared identifier \"" + name + "\"");
	}

	// evaluate
	
	public Object evaluate(Values constantValues, Values varValues) throws PrismException
	{
		// this should never be called - this should
		// have been converted to an ExpressionVar/ExpressionConstant/...
		throw new PrismException("Could not evaluate identifier \"" + name +  "\"");
	}

	/**
	 *	convert and build simulator expression data structure
	 */
	public int toSimulator(SimulatorEngine sim) throws SimulatorException
	{
		throw new SimulatorException("Undeclared identifier \"" + name +  "\"");
	}

	// convert to string
	
	public String toString()
	{
		return name;
	}
}

//------------------------------------------------------------------------------
