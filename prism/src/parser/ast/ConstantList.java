//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package parser.ast;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import parser.*;
import parser.visitor.*;
import prism.PrismLangException;
import prism.PrismUtils;
import parser.type.*;

/**
 * Class to store list of (defined and undefined) constants
 */
public class ConstantList extends ASTElement
{
	// Name/expression/type triples to define constants
	private Vector<String> names;
	private Vector<Expression> constants; // these can be null, i.e. undefined
	private Vector<Type> types;
	// We also store an ExpressionIdent to match each name.
	// This is to just to provide positional info.
	private Vector<ExpressionIdent> nameIdents;
	
	// Constructor
	
	public ConstantList()
	{
		// initialise
		names = new Vector<String>();
		constants = new Vector<Expression>();
		types = new Vector<Type>();
		nameIdents = new Vector<ExpressionIdent>();
	}
	
	// Set methods
	
	public void addConstant(ExpressionIdent n, Expression c, Type t)
	{
		names.addElement(n.getName());
		constants.addElement(c);
		types.addElement(t);
		nameIdents.addElement(n);
	}
	
	public void setConstant(int i, Expression c)
	{
		constants.setElementAt(c, i);
	}
	
	// Get methods

	public int size()
	{
		return constants.size();
	}

	public String getConstantName(int i)
	{
		return names.elementAt(i);
	}
	
	public Expression getConstant(int i)
	{
		return constants.elementAt(i);
	}
	
	public Type getConstantType(int i)
	{
		return types.elementAt(i);
	}
	
	public ExpressionIdent getConstantNameIdent(int i)
	{
		return nameIdents.elementAt(i);
	}

	/**
	 * Get the index of a constant by its name (returns -1 if it does not exist).
	 */
	public int getConstantIndex(String s)
	{
		return names.indexOf(s);
	}

	/**
	 * Find cyclic dependencies.
	*/
	public void findCycles() throws PrismLangException
	{
		// Create boolean matrix of dependencies
		// (matrix[i][j] is true if constant i contains constant j)
		int n = constants.size();
		boolean matrix[][] = new boolean[n][n];
		for (int i = 0; i < n; i++) {
			Expression e = getConstant(i);
			if (e != null) {
				Vector<String> v = e.getAllConstants();
				for (int j = 0; j < v.size(); j++) {
					int k = getConstantIndex(v.elementAt(j));
					if (k != -1) {
						matrix[i][k] = true;
					}
				}
			}
		}
		// Check for and report dependencies
		int firstCycle = PrismUtils.findCycle(matrix);
		if (firstCycle != -1) {
			String s = "Cyclic dependency in definition of constant \"" + getConstantName(firstCycle) + "\"";
			throw new PrismLangException(s, getConstant(firstCycle));
		}
	}
	
	/**
	 * Get the number of undefined constants in the list.
	 */
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
	
	/**
	 * Get a list of the undefined constants in the list.
	 */
	public Vector<String> getUndefinedConstants()
	{
		int i, n;
		Expression e;
		Vector<String> v;
		
		v = new Vector<String>();
		n = constants.size();
		for (i = 0; i < n; i++) {
			e = getConstant(i);
			if (e == null) {
				v.addElement(getConstantName(i));
			}
		}
		
		return v;
	}
	
	/**
	 * Check if {@code name} is a *defined* constants in the list,
	 * i.e. a constant whose value was *not* left unspecified in the model/property.
	 */
	public boolean isDefinedConstant(String name)
	{
		int i = getConstantIndex(name);
		if (i == -1)
			return false;
		return (getConstant(i) != null);
	}
	
	/**
	 * Set values for *all* undefined constants, evaluate values for *all* constants
	 * and return a Values object with values for *all* constants.
	 * Argument 'someValues' contains values for undefined ones, can be null if all already defined
	 * Argument 'otherValues' contains any other values which may be needed, null if none
	 */
	public Values evaluateConstants(Values someValues, Values otherValues) throws PrismLangException
	{
		return evaluateSomeOrAllConstants(someValues, otherValues, true);
	}
	
	/**
	 * Set values for *some* undefined constants, evaluate values for constants where possible
	 * and return a Values object with values for all constants that could be evaluated.
	 * Argument 'someValues' contains values for undefined ones, can be null if all already defined
	 * Argument 'otherValues' contains any other values which may be needed, null if none
	 */
	public Values evaluateSomeConstants(Values someValues, Values otherValues) throws PrismLangException
	{
		return evaluateSomeOrAllConstants(someValues, otherValues, false);
	}
	
	/**
	 * Set values for *some* or *all* undefined constants, evaluate values for constants where possible
	 * and return a Values object with values for all constants that could be evaluated.
	 * Argument 'someValues' contains values for undefined ones, can be null if all already defined.
	 * Argument 'otherValues' contains any other values which may be needed, null if none.
	 * If argument 'all' is true, an exception is thrown if any undefined constant is not defined.
	 */
	private Values evaluateSomeOrAllConstants(Values someValues, Values otherValues, boolean all) throws PrismLangException
	{
		ConstantList cl;
		Expression e;
		Values allValues;
		int i, j, n, numToEvaluate;
		Type t = null;
		ExpressionIdent s;
		Object val;
		
		// Create new copy of this ConstantList
		// (copy existing constant definitions, add new ones where undefined)
		cl = new ConstantList();
		n = constants.size();
		for (i = 0; i < n; i++) {
			s = getConstantNameIdent(i);
			e = getConstant(i);
			t = getConstantType(i);
			if (e != null) {
				cl.addConstant((ExpressionIdent)s.deepCopy(), e.deepCopy(), t);
			} else {
				// Create new literal expression using values passed in (if possible and needed)
				if (someValues != null && (j = someValues.getIndexOf(s.getName())) != -1) {
					cl.addConstant((ExpressionIdent) s.deepCopy(), new ExpressionLiteral(t, t.castValueTo(someValues.getValue(j))), t);
				} else {
					if (all)
						throw new PrismLangException("No value specified for constant", s);
				}
			}
		}
		numToEvaluate = cl.size();
		
		// Now add constants corresponding to the 'otherValues' argument to the new constant list
		if (otherValues != null) {
			n = otherValues.getNumValues();
			for (i = 0; i < n; i++) {
				Type iType = otherValues.getType(i);
				cl.addConstant(new ExpressionIdent(otherValues.getName(i)), new ExpressionLiteral(iType, iType.castValueTo(otherValues.getValue(i))), iType);
			}
		}
		
		// Go trough and expand definition of each constant
		// (i.e. replace other constant references with their definitions)
		// Note: work with new copy of constant list, don't need to expand 'otherValues' ones.
		for (i = 0; i < numToEvaluate; i++) {
			try {
				e = (Expression)cl.getConstant(i).expandConstants(cl);
				cl.setConstant(i, e);
			} catch (PrismLangException ex) {
				if (all) {
					throw ex;
				} else {
					cl.setConstant(i, null);
				}
			}
		}
		
		// Evaluate constants and store in new Values object (again, ignoring 'otherValues' ones)		
		allValues = new Values();
		for (i = 0; i < numToEvaluate; i++) {
			if (cl.getConstant(i) != null) {
				val = cl.getConstant(i).evaluate(null, otherValues);
				allValues.addValue(cl.getConstantName(i), val);
			}
		}
		
		return allValues;
	}

	/**
	 * Set values for some undefined constants, then partially evaluate values for constants where possible
	 * and return a map from constant names to the Expression representing its value. 
	 * Argument 'someValues' contains values for undefined ones, can be null if all already defined.
	 * Argument 'otherValues' contains any other values which may be needed, null if none.
	 */
	public Map<String,Expression> evaluateConstantsPartially(Values someValues, Values otherValues) throws PrismLangException
	{
		ConstantList cl;
		Expression e;
		int i, j, n, numToEvaluate;
		Type t = null;
		ExpressionIdent s;
		
		// Create new copy of this ConstantList
		// (copy existing constant definitions, add new ones where undefined)
		cl = new ConstantList();
		n = constants.size();
		for (i = 0; i < n; i++) {
			s = getConstantNameIdent(i);
			e = getConstant(i);
			t = getConstantType(i);
			if (e != null) {
				cl.addConstant((ExpressionIdent)s.deepCopy(), e.deepCopy(), t);
			} else {
				// Create new literal expression using values passed in (if possible and needed)
				if (someValues != null && (j = someValues.getIndexOf(s.getName())) != -1) {
					cl.addConstant((ExpressionIdent) s.deepCopy(), new ExpressionLiteral(t, t.castValueTo(someValues.getValue(j))), t);
				}
			}
		}
		numToEvaluate = cl.size();
		
		// Now add constants corresponding to the 'otherValues' argument to the new constant list
		if (otherValues != null) {
			n = otherValues.getNumValues();
			for (i = 0; i < n; i++) {
				Type iType = otherValues.getType(i);
				cl.addConstant(new ExpressionIdent(otherValues.getName(i)), new ExpressionLiteral(iType, iType.castValueTo(otherValues.getValue(i))), iType);
			}
		}
		
		// Go trough and expand definition of each constant
		// (i.e. replace other constant references with their definitions)
		// Note: work with new copy of constant list, don't need to expand 'otherValues' ones.
		for (i = 0; i < numToEvaluate; i++) {
			try {
				e = (Expression)cl.getConstant(i).expandConstants(cl);
				cl.setConstant(i, e);
			} catch (PrismLangException ex) {
				cl.setConstant(i, null);
			}
		}
		
		// Store final expressions for each constant in a map and return
		Map<String,Expression> constExprs = new HashMap<>();
		for (i = 0; i < numToEvaluate; i++) {
			if (cl.getConstant(i) != null) {
				constExprs.put(cl.getConstantName(i), cl.getConstant(i).deepCopy());
			}
		}
		
		return constExprs;
	}

	// Methods required for ASTElement:
	
	/**
	 * Visitor method.
	 */
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}
	
	/**
	 * Convert to string.
	 */
	public String toString()
	{
		String s = "";
		int i, n;
		Expression e;
		
		n = constants.size();
		for (i = 0; i < n; i++) {
			s += "const ";
			s += getConstantType(i).getTypeString() + " ";
			s += getConstantName(i);
			e = getConstant(i);
			if (e != null) {
				s += " = " + e;
			}
			s += ";\n";
		}
		
		return s;
	}
	
	/**
	 * Perform a deep copy.
	 */
	public ASTElement deepCopy()
	{
		int i, n;
		ConstantList ret = new ConstantList();
		n = size();
		for (i = 0; i < n; i++) {
			Expression constantNew = (getConstant(i) == null) ? null : getConstant(i).deepCopy();
			ret.addConstant((ExpressionIdent)getConstantNameIdent(i).deepCopy(), constantNew, getConstantType(i));
		}
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
