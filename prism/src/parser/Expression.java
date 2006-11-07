//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker
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

// abstract class for prism language expressions

public abstract class Expression
{
	// type constants
	public static final int INT = 1;
	public static final int DOUBLE = 2;
	public static final int BOOLEAN = 3;
	
	// all expressions have a type stored, default to unknown (0)
	protected int type = 0;
	
	// set type
	public void setType(int t) { type = t; }
	
	// get type - if unknown, try and compute first
	public int getType() throws PrismException
	{
		if (type==0) {
			typeCheck();
		}
		
		return type;
	}
	
	// and a function to get at their names
	public static String getTypeString(int i) {
		switch (i) {
			case INT: return "int";
			case DOUBLE: return "double";
			case BOOLEAN: return "bool";
			default: return "(unknown)";
		}
	}
	
	// another useful function telling you which types can be assigned to which others
	public static boolean canAssignTypes(int tl, int tr)
	{
		switch (tl) {
			// boolean can only be assigned boolean
			case Expression.BOOLEAN: return (tr == Expression.BOOLEAN);
			// int can only be assigned int
			case Expression.INT: return (tr == Expression.INT);
			// double can be assigned int or double
			case Expression.DOUBLE: return (tr == Expression.INT || tr == Expression.DOUBLE);
			// should never happen...
			default: return false;
		}
	}
	
	// find all formulas (i.e. locate idents which are formulas)
	// replaces ExpressionIdent objects with ExpressionFormula objects
	// [modifies and returns the expression]
	public abstract Expression findAllFormulas(FormulaList formulaList) throws PrismException;
	
	// get all formulas (put into vector)
	// identifies formulas as those which are ExpressionFormula objects
	// [does nothing to the expression itself]
	public abstract void getAllFormulas(Vector v) throws PrismException;
	
	// expand any formulas
	// [modifies and returns the expression]
	public abstract Expression expandFormulas(FormulaList formulaList) throws PrismException;
	
	// create and return a new expression by renaming
	// [creates a new expression]
	public abstract Expression rename(RenamedModule rm) throws PrismException;
	
	// find all constants (i.e. locate idents which are constants)
	// replaces ExpressionIdent objects with ExpressionConstant objects
	// [modifies and returns the expression]
	public abstract Expression findAllConstants(ConstantList constantList) throws PrismException;
	
	// get all constants (put into vector)
	// identifies constants as those which are ExpressionConstant objects
	// [does nothing to the expression itself]
	public abstract void getAllConstants(Vector v) throws PrismException;
	
	// create and return a new expression by expanding constants
	// [creates a new expression]
	public abstract Expression expandConstants(ConstantList constantList) throws PrismException;

	// find all variables (i.e. locate idents which are variables)
	// replaces ExpressionIdent objects with ExpressionVar objects
	// [modifies and returns the expression]
	public abstract Expression findAllVars(Vector varIdents, Vector varTypes) throws PrismException;

	// get all vars (put into vector)
	// identifies variables as those which are ExpressionVar objects
	// [does nothing to the expression itself]
	public abstract void getAllVars(Vector v) throws PrismException;
	
	// perform type checking
	// (and in doing so compute type)
	// [does nothing to the expression itself]
	public abstract void typeCheck() throws PrismException;

	// is expression constant?
	// [does nothing to the expression itself]
	public abstract boolean isConstant();

	// check expression is ok
	// [does nothing to the expression itself]
	public abstract void check() throws PrismException;
	
	// evaluate
	// [does nothing to the expression itself]
	public abstract Object evaluate(Values constantValues, Values varValues) throws PrismException;

	// evaluate to an int
	// any typing issues cause an exception
	// [does nothing to the expression itself]
	public int evaluateInt(Values constantValues, Values varValues) throws PrismException
	{
		Object o;
		
		o = evaluate(constantValues, varValues);
		
		if (!(o instanceof Integer)) {
			throw new PrismException("Cannot evaluate \"" + toString() + "\" to an integer");
		}
		
		return ((Integer)o).intValue();
	}

	// evaluate to a double
	// any typing issues cause an exception
	// [does nothing to the expression itself]
	public double evaluateDouble(Values constantValues, Values varValues) throws PrismException
	{
		Object o;
		
		o = evaluate(constantValues, varValues);
		
		if (o instanceof Boolean) {
			throw new PrismException("Cannot evaluate \"" + toString() + "\" to a double");
		}
		if (o instanceof Integer) {
			return ((Integer)o).intValue();
		}
		if (o instanceof Double) {
			return ((Double)o).doubleValue();
		}
		
		throw new PrismException("Cannot evaluate \"" + toString() + "\" to a double");
	}

	// evaluate to a boolean
	// any typing issues cause an exception
	// [does nothing to the expression itself]
	public boolean evaluateBoolean(Values constantValues, Values varValues) throws PrismException
	{
		Object o;
		
		o = evaluate(constantValues, varValues);
		
		if (!(o instanceof Boolean)) {
			throw new PrismException("Cannot evaluate \"" + toString() + "\" to a boolean");
		}
		
		return ((Boolean)o).booleanValue();
	}

	/**
	 *	convert and build simulator expression data structure
	 *	Returns an int corresponding to a pointer to the 
	 *	built expression.
	 */
	public abstract int toSimulator(SimulatorEngine sim) throws SimulatorException;

	// get expression as string
	// [does nothing to the expression itself]
	public abstract String toString();

	// get expression tree as string
	// [does nothing to the expression itself]
	public String toTreeString(int indent)
	{
		String s = "";
		int i;
		
		for (i = 0; i < indent; i++) s += " ";
		s += getClass() + " : " + toString() + "\n";
		
		return s;
	}
}

//------------------------------------------------------------------------------
