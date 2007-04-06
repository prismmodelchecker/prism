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

// class to store list of constants

public class ConstantList
{
	private Vector names;
	private Vector constants; // these can be null, i.e. undefined
	private Vector types;
		
	// constructor
	
	public ConstantList()
	{
		// initialise
		names = new Vector();
		constants = new Vector();
		types = new Vector();
	}
	
	// set up methods
	
	public void addConstant(String n, Expression c, int t)
	{
		names.addElement(n);
		constants.addElement(c);
		types.addElement(new Integer(t));
	}
	
	public void setConstant(int i, Expression c)
	{
		constants.setElementAt(c, i);
	}
	
	// accessor methods

	public int size()
	{
		return constants.size();
	}

	public String getConstantName(int i)
	{
		return (String)names.elementAt(i);
	}
	
	public Expression getConstant(int i)
	{
		return (Expression)constants.elementAt(i);
	}
	
	public int getConstantType(int i)
	{
		return ((Integer)types.elementAt(i)).intValue();
	}
	
	// get the index of a constant by its name
	// (returns -1 if it does not exist)
	
	public int getConstantIndex(String s)
	{
		return names.indexOf(s);
	}

	// find all formulas (i.e. locate idents which are formulas)
	// (inside the expressions in this constant list)
	
	public void findAllFormulas(FormulaList formulaList) throws PrismException
	{
		int i, n;
		Expression e;
		
		n = constants.size();
		for (i = 0; i < n; i++) {
			e = getConstant(i);
			// if constant expression is defined
			if (e != null) {
				constants.setElementAt(e.findAllFormulas(formulaList), i);
			}
		}
	}

	// expand any formulas
	
	public void expandFormulas(FormulaList formulaList) throws PrismException
	{
		int i, n;
		Expression e;
		
		n = constants.size();
		for (i = 0; i < n; i++) {
			e = getConstant(i);
			// if constant expression is defined
			if (e != null) {
				constants.setElementAt(e.expandFormulas(formulaList), i);
			}
		}
	}

	// find all constants (i.e. locate idents which are constants)
	// (inside the expressions in this constant list)
	// (note we might be looking for constants from another ConstantList)
	
	public void findAllConstants(ConstantList constantList) throws PrismException
	{
		int i, n;
		Expression e;
		
		n = constants.size();
		for (i = 0; i < n; i++) {
			e = getConstant(i);
			// if constant expression is defined
			if (e != null) {
				constants.setElementAt(e.findAllConstants(constantList), i);
			}
		}
	}

	// find cyclic dependencies
	
	public void findCycles() throws PrismException
	{
		int i, j, k, l, n, firstCycle = -1;
		Vector v;
		boolean matrix[][];
		boolean foundCycle = false;
		Expression e;
		
		// initialise boolean matrix
		n = constants.size();
		matrix = new boolean[n][n];
		for (i = 0; i < n; i++) {
			for (j = 0; j < n; j++) {
				matrix[i][j] = false;
			}
		}
		
		// determine which constants contain which other constants
		// and store this info in boolean matrix
		for (i = 0; i < n; i++) {
			e = getConstant(i);
			if (e != null) {
				v = new Vector();
				e.getAllConstants(v);
				for (j = 0; j < v.size(); j++) {
					k = getConstantIndex((String)v.elementAt(j));
					if (k != -1) {
						matrix[i][k] = true;
					}
				}
			}
		}
		
		// check for dependencies
		// (loop a maximum of n times)
		// (n = max length of possible cycle)
		for (i = 0 ; i < n; i++) {
			// see if there is a cycle yet
			for (j = 0; j < n; j++) {
				if (matrix[j][j]) {
					foundCycle = true;
					firstCycle = j;
					break;
				}
			}
			// if so, stop
			if (foundCycle) break;
			// extend dependencies
			for (j = 0; j < n; j++) {
				for (k = 0; k < n; k++) {
					if (matrix[j][k]) {
						for (l = 0; l < n; l++) {
							matrix[j][l] |= matrix[k][l];
						}
					}
				}
			}
		}
		
		// report dependency
		if (foundCycle) {
			String s = "Cyclic dependency found in constant \"" + getConstantName(firstCycle) + "\"";
			throw new PrismException(s);
		}
	}
	
	// find all variables (i.e. locate idents which are variables)
	// (inside the expressions in this constant list)
	// (there shouldn't be any)
	
	public void findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		int i, n;
		Expression e;
		
		n = constants.size();
		for (i = 0; i < n; i++) {
			e = getConstant(i);
			// if constant expression is defined
			if (e != null) {
				constants.setElementAt(e.findAllVars(varIdents, varTypes), i);
			}
		}
	}

	// check everything is ok
	
	public void check() throws PrismException
	{
		int i, n;
		String s;
		Expression e;
		
		// go thru all constants
		n = constants.size();
		for (i = 0; i < n; i++) {
			e = getConstant(i);
			// if constant expression is defined
			if (e != null) {
				// is expression ok?
				e.check();
				// and is expression constant?
				if (!e.isConstant()) {
					s = "Definition of constant \"" + getConstantName(i) + "\" is not constant";
					throw new PrismException(s);
				}
				// and does it type check ok?
				if (!Expression.canAssignTypes(getConstantType(i), e.getType())) {
					throw new PrismException("Type error in constant \"" + getConstantName(i) + "\"");
				}
			}
		}
	}
	
	// get number of undefined constants
	
	public int getNumUndefined()
	{
		int i, n, res;
		Expression e;
		
		res = 0;
		n = constants.size();
		for (i = 0; i < n; i++) {
			e = getConstant(i);
			if (e == null) {
				res++;
			}
		}
		
		return res;
	}
	
	// get undefined constants
	
	public Vector getUndefinedConstants()
	{
		int i, n;
		Expression e;
		Vector v;
		
		v = new Vector();
		n = constants.size();
		for (i = 0; i < n; i++) {
			e = getConstant(i);
			if (e == null) {
				v.addElement(getConstantName(i));
			}
		}
		
		return v;
	}
	
	// Set values for undefined constants, evaluate all and return.
	// Argument 'someValues' contains values for undefined ones, can be null if all already defined
	// Argument 'otherValues' contains any other values which may be needed, null if none
	
	public Values evaluateConstants(Values someValues, Values otherValues) throws PrismException
	{
		ConstantList cl;
		Expression e;
		Values allValues;
		int i, j, n, t;
		String s;
		
		// create new copy of this ConstantList
		// (copy existing constant definitions, add new ones where undefined)
		cl = new ConstantList();
		n = constants.size();
		for (i = 0; i < n; i++) {
			s = getConstantName(i);
			e = getConstant(i);
			t = getConstantType(i);
			if (e != null) {
				// use existing expression but this is ok because it will be
				// used to create a new one shortly, not directly modified
				cl.addConstant(s, e, t);
			}
			else {
				// create new literal expression using values passed in
				j = someValues.getIndexOf(s);
				if (j == -1) {
					throw new PrismException("No value specified for constant \"" + s + "\"");
				}
				else {
					switch (t) {
						case Expression.INT: cl.addConstant(s, new ExpressionInt(someValues.getIntValue(j)), Expression.INT); break;
						case Expression.DOUBLE: cl.addConstant(s, new ExpressionDouble(someValues.getDoubleValue(j)), Expression.DOUBLE); break;
						case Expression.BOOLEAN:
							if (someValues.getBooleanValue(j)) {
								cl.addConstant(s, new ExpressionTrue(), Expression.BOOLEAN);
							}
							else {
								cl.addConstant(s, new ExpressionFalse(), Expression.BOOLEAN);
							}
							break;
					}
				}
			}
		}
		
		// now add constants corresponding to the 'otherValues' argument to the new constant list
		if (otherValues != null) {
			n = otherValues.getNumValues();
			for (i = 0; i < n; i++) {
				switch (otherValues.getType(i)) {
					case Expression.INT: cl.addConstant(otherValues.getName(i), new ExpressionInt(otherValues.getIntValue(i)), Expression.INT); break;
					case Expression.DOUBLE: cl.addConstant(otherValues.getName(i), new ExpressionDouble(otherValues.getDoubleValue(i)), Expression.DOUBLE); break;
					case Expression.BOOLEAN:
						if (otherValues.getBooleanValue(i)) {
							cl.addConstant(otherValues.getName(i), new ExpressionTrue(), Expression.BOOLEAN);
						}
						else {
							cl.addConstant(otherValues.getName(i), new ExpressionFalse(), Expression.BOOLEAN);
						}
						break;
				}
			}
		}
		
		// go thru and expand definition of each constant
		// (i.e. replace other constant references with their definitions)
		// (working with new copy of constant list)
		// (and ignoring extra constants added on the end which are all defined)		
		n = constants.size();
		for (i = 0; i < n; i++) {
			cl.setConstant(i, cl.getConstant(i).expandConstants(cl));
		}
		
		// evaluate constants and store in new Values object
		// (again, ignoring extra constants added on the end)		
		allValues = new Values();
		n = constants.size();
		for (i = 0; i < n; i++) {
			allValues.addValue(cl.getConstantName(i), cl.getConstant(i).evaluate(null, otherValues));
		}
		
		return allValues;
	}

	// convert to string
	
	public String toString()
	{
		String s = "";
		int i, n;
		Expression e;
		
		n = constants.size();
		for (i = 0; i < n; i++) {
			s += "const ";
			s += Expression.getTypeString(getConstantType(i)) + " ";
			s += getConstantName(i);
			e = getConstant(i);
			if (e != null) {
				s += " = " + e;
			}
			s += ";\n";
		}
		
		return s;
	}
}

//------------------------------------------------------------------------------
