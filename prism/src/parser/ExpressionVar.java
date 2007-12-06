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

public class ExpressionVar extends Expression
{
	String name;
	
	// constructors
	
	public ExpressionVar()
	{
	}
	
	public ExpressionVar(String n, int t)
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
		// n/a
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

		return new ExpressionVar(name, type);
	}
		
	// find all constants (i.e. locate idents which are constants)
	
	public Expression findAllConstants(ConstantList constantList) throws PrismException
	{
		// n/a
		return this;
	}

	// get all constants (put into vector)
	
	public void getAllConstants(Vector v) throws PrismException
	{
		// n/a
	}

	// create and return a new expression by expanding constants

	public Expression expandConstants(ConstantList constantList) throws PrismException
	{
		return new ExpressionVar(name, type);
	}

	// find all variables (i.e. locate idents which are variables)
	
	public Expression findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		// n/a: ExpressionVar objects are never created until _after_ the findAllVars phase
		return this;
	}
	
	// get all vars (put into vector)
	
	public void getAllVars(Vector v) throws PrismException
	{
		if (!v.contains(name)) {
			v.addElement(name);
		}
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
		return false;
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
		
		if (varValues == null) {
			throw new PrismException("Could not evaluate variable \"" + name +  "\"");
		}
		
		i = varValues.getIndexOf(name);
		if (i == -1) {
			throw new PrismException("Could not evaluate variable \"" + name +  "\"");
		}
		
		return varValues.getValue(i);
	}

	/**
	 *	Convert and build simulator expression data structure
	 */
	public long toSimulator(SimulatorEngine sim) throws SimulatorException
	{
		//precondition: variable indices have 
		//already been populated in simulator 
		//engine
		int varIndex = sim.getIndexOfVar(name); 
		int theType = 0;
		try
		{
			theType = getType();
		}
		catch(PrismException e)
		{
			throw new SimulatorException("Error when creating variable: "+name+"\n"+e.getMessage());
		}
		switch(theType)
		{
			case Expression.INT: return SimulatorEngine.createIntegerVar(varIndex);
			case Expression.BOOLEAN: return SimulatorEngine.createBooleanVar(varIndex);
			default: throw new SimulatorException("type error when creating variable: "+name);
		}
	}

	// convert to string
	
	public String toString()
	{
		return name;
	}
}

//------------------------------------------------------------------------------
