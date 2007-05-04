//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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

public class Declaration
{
	String name;		// name
	int type;			// type
	Expression low;		// min value - not for bools
	Expression high;	// max value - not for bools
	Expression start;	// start value
	boolean startSpec;	// was a start value explicitly specified?

	// pointless constructor
	
	public Declaration()
	{
		name = "";
		type = 0;
		low = null;
		high = null;
		start = null;
		startSpec = false;
	}

	// integer variable constructor
	
	public Declaration(String n, Expression l, Expression h, Expression s)
	{
		name = n;
		type = Expression.INT;
		low = l;
		high = h;
		// if no start value given, use low value
		if (s != null) { start = s; startSpec = true; }
		else { start = l; startSpec = false; }
	}

	// boolean variable constructor
	
	public Declaration(String n, Expression s)
	{
		name = n;
		type = Expression.BOOLEAN;
		low = null;
		high = null;
		// if no start value given, default to false
		if (s != null) { start = s; startSpec = true; }
		else { start = new ExpressionFalse(); startSpec = false; }
	}

	// set methods
	
	public void setName(String n)
	{
		name = n;
	}
	
	public void setType(int t)
	{
		type = t;
	}
	
	public void setLow(Expression l)
	{
		low = l;
	}
	
	public void setHigh(Expression h)
	{
		high = h;
	}

	public void setStart(Expression s)
	{
		// if no start value given, use low value
		if (s != null) { start = s; startSpec = true; }
		else { start = low; startSpec = false; }
	}

	// get methods
	
	public String getName()
	{
		return name;
	}
	
	public int getType()
	{
		return type;
	}
	
	public Expression getLow()
	{
		return low;
	}
	
	public Expression getHigh()
	{
		return high;
	}
	
	public Expression getStart()
	{
		return start;
	}
	
	public boolean isStartSpecified()
	{
		return startSpec;
	}

	// find all formulas (i.e. locate idents which are formulas)
	
	public void findAllFormulas(FormulaList formulaList) throws PrismException
	{
		if (low != null) low = low.findAllFormulas(formulaList);
		if (high != null) high = high.findAllFormulas(formulaList);
		start = start.findAllFormulas(formulaList);
	}
		
	// expand any formulas
	
	public void expandFormulas(FormulaList formulaList) throws PrismException
	{
		if (low != null) low = low.expandFormulas(formulaList);
		if (high != null) high = high.expandFormulas(formulaList);
		start = start.expandFormulas(formulaList);
	}
	
	// create and return a new Declaration by renaming

	public Declaration rename(RenamedModule rm) throws PrismException
	{
		String n;
		
		// get new (renamed) variable name
		n = rm.getNewName(name);
		// if it is not renamed, this is an error
		if (n == null) {
			String err = "Definition of module \"" + rm.getName();
			err += "\" must rename variable \"" + name + "\"";
			throw new PrismException(err);
		}
		// create and return new copy
		if (type == Expression.INT) {
			return new Declaration(n, low.rename(rm), high.rename(rm), startSpec ? start.rename(rm) : null);
		}
		else if (type == Expression.BOOLEAN) {
			return new Declaration(n, startSpec ? start.rename(rm) : null);
		}
		else {
			throw new PrismException("Unknown type for variable \"" + name + "\"");
		}
	}
		
	// find all constants (i.e. locate idents which are constants)
	
	public void findAllConstants(ConstantList constantList) throws PrismException
	{
		if (low != null) low = low.findAllConstants(constantList);
		if (high != null) high = high.findAllConstants(constantList);
		start = start.findAllConstants(constantList);
	}
	
	// find all variables (i.e. locate idents which are variables)
	// (there shouldn't be any)
	
	public void findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		if (low != null) low = low.findAllVars(varIdents, varTypes);
		if (high != null) high = high.findAllVars(varIdents, varTypes);
		start = start.findAllVars(varIdents, varTypes);
	}
	
	// check that the declaration is valid
	
	public void check() throws PrismException
	{
		// check low/high/start expressions are ok
		if (low != null) low.check();
		if (high != null) high.check();
		start.check();
		// check low/high/start expressions are constant
		if (low != null) if (!low.isConstant()) {
			throw new PrismException("Minimum value of variable \"" + name + "\" is not constant");
		}
		if (high != null) if (!high.isConstant()) {
			throw new PrismException("Maximum value of variable \"" + name + "\" is not constant");
		}
		if (!start.isConstant()) {
			throw new PrismException("Initial value of variable \"" + name + "\" is not constant");
		}
		// type check low/high/start expressions
		if (low != null) if (!Expression.canAssignTypes(type, low.getType())) {
			throw new PrismException("Type error in minimum value of variable \"" + name + "\"");
		}
		if (high != null) if (!Expression.canAssignTypes(type, high.getType())) {
			throw new PrismException("Type error in maximum value of variable \"" + name + "\"");
		}
		if (!Expression.canAssignTypes(type, start.getType())) {
			throw new PrismException("Type error in initial value of variable \"" + name + "\"");
		}
	}

	// convert to string
	
	public String toString()
	{
		if (type == Expression.INT) {
			return name + " : [" + low + ".." + high + "] init " + start;
		}
		else if (type == Expression.BOOLEAN) {
			return name + " : bool init " + start;
		}
		else return "";
	}
}

//------------------------------------------------------------------------------
