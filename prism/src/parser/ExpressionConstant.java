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

public class ExpressionConstant extends Expression
{
	String name;
	
	// constructors
	
	public ExpressionConstant()
	{
	}
	
	public ExpressionConstant(String n, int t)
	{
		setType(t);
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
		// n/a: ExpressionConstant objects are never created until _after_ the findAllFormulas phase
		return this;
	}
	
	// get all formulas (put into vector)
	
	public void getAllFormulas(Vector v) throws PrismException
	{
		// n/a
		return;
	}

	// expand any formulas
	
	public Expression expandFormulas(FormulaList formulaList) throws PrismException
	{
		// n/a
		return this;
	}
		
	// create and return a new expression by renaming

	public Expression rename(RenamedModule rm) throws PrismException
	{
		// these aren't created until after renaming so this should never be called
		// but just in case...

		return new ExpressionConstant(name, type);
	}
		
	// find all constants (i.e. locate idents which are constants)
	
	public Expression findAllConstants(ConstantList constantList) throws PrismException
	{
		// n/a: ExpressionConstant objects are never created until _after_ the findAllConstants phase
		return this;
	}
	
	// get all constants (put into vector)
	
	public void getAllConstants(Vector v) throws PrismException
	{
		if (!v.contains(name)) {
			v.addElement(name);
		}
	}

	// create and return a new expression by expanding constants

	public Expression expandConstants(ConstantList constantList) throws PrismException
	{
		int i;
		Expression e;
		
		// see if identifier corresponds to a constant
		i = constantList.getConstantIndex(name);
		if (i != -1) {
			// if so, replace it with the corresponding expression
			e = constantList.getConstant(i);
			// but also recursively expand that
			e = e.expandConstants(constantList);
			// put in brackets so precedence is preserved
			// (for display purposes only)
			e = new ExpressionBrackets(e);
			// return replacement expression
			return e;
		}
		else {
			// error (should never happen)
			throw new PrismException("Constant \"" + name + "\" undefined");
		}
	}

	// find all variables (i.e. locate idents which are variables)
	
	public Expression findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		// n/a
		return this;
	}

	// get all vars (put into vector)
	
	public void getAllVars(Vector v) throws PrismException
	{
		// n/a
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		// always ok
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
		int i;
		
		if (constantValues == null) {
			throw new PrismException("Could not evaluate constant \"" + name +  "\"");
		}
		
		i = constantValues.getIndexOf(name);
		if (i == -1) {
			throw new PrismException("Could not evaluate constant \"" + name +  "\"");
		}
		
		return constantValues.getValue(i);
	}

	/**
	 *	Convert and build simulator expression data structure
	 */
	public int toSimulator(SimulatorEngine sim) throws SimulatorException
	{
		
		int i = sim.getConstants().getIndexOf(name);
		Object o;
		if (i == -1)
		{
			i = sim.getPropertyConstants().getIndexOf(name);
			if(i == -1)
				throw new SimulatorException("Could not evaluate constant \"" + name +  "\"");
			else 
				o = sim.getPropertyConstants().getValue(i);
		}
		else
			o = sim.getConstants().getValue(i);
		double dval;
		int ival;
		boolean bval;
		if(o instanceof Double)
		{
			dval = ((Double)o).doubleValue();
			ival = (int)Math.round(dval);
			bval = (ival==0) ? false : true;
		}
		else if(o instanceof Integer)
		{
			ival = ((Integer)o).intValue();
			dval = ival;
			bval = (ival==0) ? false : true;
		}
		else if(o instanceof Boolean)
		{
			bval = ((Boolean)o).booleanValue();
			ival = (bval) ? 1 : 0;
			dval = ival;
		}
		else //should never happen
		{
			bval = false;
			ival = 0;
			dval = 0.0;
		}
		Expression expr;
		switch (type)
		{
			case Expression.INT: expr = new ExpressionInt(ival); break;
			case Expression.DOUBLE: expr = new ExpressionDouble(dval); break;
			case Expression.BOOLEAN: expr = (bval) ? (Expression)new ExpressionTrue() : (Expression)new ExpressionFalse(); break;
			default: expr = null;
		}
		// and then call toSimulator on that
		return expr.toSimulator(sim);
	}

	// convert to string
	
	public String toString()
	{
		return name;
	}
}

//------------------------------------------------------------------------------
