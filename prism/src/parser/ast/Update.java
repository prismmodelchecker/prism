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

import java.util.ArrayList;
import java.util.BitSet;

import param.BigRational;
import parser.*;
import parser.type.*;
import parser.visitor.*;
import prism.PrismLangException;

/**
 * Class to store a single update, i.e. a list of assignments of variables to expressions, e.g. (s'=1)&amp;(x'=x+1)
 */
public class Update extends ASTElement
{
	// Lists of variable/expression pairs (and types)
	private ArrayList<String> vars;
	private ArrayList<Expression> exprs;
	private ArrayList<Type> types;
	// We also store an ExpressionIdent to match each variable.
	// This is to just to provide positional info.
	private ArrayList<ExpressionIdent> varIdents;
	// The indices of each variable in the model to which it belongs
	private ArrayList<Integer> indices;
	// Parent Updates object
	private Updates parent;

	/**
	 * Create an empty update.
	 */
	public Update()
	{
		vars = new ArrayList<String>();
		exprs = new ArrayList<Expression>();
		types = new ArrayList<Type>();
		varIdents = new ArrayList<ExpressionIdent>();
		indices = new ArrayList<Integer>();
	}

	// Set methods

	/**
	 * Add a variable assignment ({@code v}'={@code e}) to this update.
	 * @param v The AST element corresponding to the variable being updated
	 * @param e The expression which will be assigned to the variable
	 */
	public void addElement(ExpressionIdent v, Expression e)
	{
		vars.add(v.getName());
		exprs.add(e);
		types.add(null); // Type currently unknown
		varIdents.add(v);
		indices.add(-1); // Index currently unknown
	}

	/**
	 * Set the variable {@code v} for the {@code i}th variable assignment of this update.
	 * @param i The index of the variable assignment within the update
	 * @param v The AST element corresponding to the variable being updated
	 */
	public void setVar(int i, ExpressionIdent v)
	{
		vars.set(i, v.getName());
		varIdents.set(i, v);
	}

	/**
	 * Set the expression {@code e} for the {@code i}th variable assignment of this update.
	 * @param i The index of the variable assignment within the update
	 * @param e The expression which will be assigned to the variable
	 */
	public void setExpression(int i, Expression e)
	{
		exprs.set(i, e);
	}

	/**
	 * Set the type of the {@code i}th variable assigned to by this update.
	 * @param i The index of the variable assignment within the update
	 * @param t The variable's type
	 */
	public void setType(int i, Type t)
	{
		types.set(i, t);
	}

	/**
	 * Set the index (wrt the model) of the {@code i}th variable assigned to by this update.
	 * @param i The index of the variable assignment within the update
	 * @param t The index of the variable within the model to which it belongs
	 */
	public void setVarIndex(int i, int index)
	{
		indices.set(i, index);
	}

	/**
	 * Set the {@link parser.ast.Updates} object containing this update.
	 */
	public void setParent(Updates u)
	{
		parent = u;
	}

	// Get methods

	/**
	 * Get the number of variables assigned values by this update.
	 */
	public int getNumElements()
	{
		return vars.size();
	}

	/**
	 * Get the name of the {@code i}th variable in this update.
	 */
	public String getVar(int i)
	{
		return vars.get(i);
	}

	/**
	 * Get the expression used to update the {@code i}th variable in this update.
	 */
	public Expression getExpression(int i)
	{
		return exprs.get(i);
	}

	/**
	 * Get the type of the {@code i}th variable in this update.
	 */
	public Type getType(int i)
	{
		return types.get(i);
	}

	/**
	 * Get the ASTElement corresponding to the {@code i}th variable in this update.
	 */
	public ExpressionIdent getVarIdent(int i)
	{
		return varIdents.get(i);
	}

	/**
	 * Get the index (wrt the model) of the {@code i}th variable in this update.
	 */
	public int getVarIndex(int i)
	{
		return indices.get(i);
	}

	/**
	 * Get the {@link parser.ast.Updates} object containing this update.
	 */
	public Updates getParent()
	{
		return parent;
	}

	/**
	 * Execute this update, based on variable values specified as a Values object,
	 * returning the result as a new Values object copied from the existing one.
	 * Values of any constants should also be provided.
	 * @param constantValues Values for constants
	 * @param oldValues Variable values in current state
	 */
	public Values update(Values constantValues, Values oldValues) throws PrismLangException
	{
		int i, n;
		Values res;
		res = new Values(oldValues);
		n = exprs.size();
		for (i = 0; i < n; i++) {
			res.setValue(getVar(i), getExpression(i).evaluate(constantValues, oldValues));
		}
		return res;
	}

	/**
	 * Execute this update, based on variable values specified as a Values object,
	 * applying changes in variables to a second Values object. 
	 * Values of any constants should also be provided.
	 * @param constantValues Values for constants
	 * @param oldValues Variable values in current state
	 * @param newValues Values object to apply changes to
	 */
	public void update(Values constantValues, Values oldValues, Values newValues) throws PrismLangException
	{
		int i, n;
		n = exprs.size();
		for (i = 0; i < n; i++) {
			newValues.setValue(getVar(i), getExpression(i).evaluate(constantValues, oldValues));
		}
	}

	/**
	 * Execute this update, based on variable values specified as a State object,
	 * returning the result as a new State object copied from the existing one.
	 * It is assumed that any constants have already been defined.
	 * @param oldState Variable values in current state
	 */
	public State update(State oldState) throws PrismLangException
	{
		int i, n;
		State res;
		res = new State(oldState);
		n = exprs.size();
		for (i = 0; i < n; i++) {
			res.setValue(getVarIndex(i), getExpression(i).evaluate(oldState));
		}
		return res;
	}

	/**
	 * Execute this update, based on variable values specified as a State object.
	 * Apply changes in variables to a provided copy of the State object.
	 * (i.e. oldState and newState should be equal when passed in) 
	 * It is assumed that any constants have already been defined.
	 * <br>
	 * Arithmetic expressions are evaluated using the default evaluate (i.e., not using exact arithmetic)
	 * @param oldState Variable values in current state
	 * @param newState State object to apply changes to
	 */
	public void update(State oldState, State newState) throws PrismLangException
	{
		update(oldState, newState, false);
	}

	/**
	 * Execute this update, based on variable values specified as a State object.
	 * Apply changes in variables to a provided copy of the State object.
	 * (i.e. oldState and newState should be equal when passed in)
	 * It is assumed that any constants have already been defined.
	 * @param oldState Variable values in current state
	 * @param newState State object to apply changes to
	 * @param exact evaluate arithmetic expressions exactly?
	 */
	public void update(State oldState, State newState, boolean exact) throws PrismLangException
	{
		int i, n;
		n = exprs.size();
		for (i = 0; i < n; i++) {
			Object newValue;
			if (exact) {
				BigRational r = getExpression(i).evaluateExact(oldState);
				// cast to Java data type
				newValue = getExpression(i).getType().castFromBigRational(r);
			} else {
				newValue = getExpression(i).evaluate(oldState);
			}
			newState.setValue(getVarIndex(i), newValue);
		}
	}

	/**
	 * Execute this update, based on variable values specified as a State object.
	 * Apply changes in variables to a provided copy of the State object.
	 * (i.e. oldState and newState should be equal when passed in.) 
	 * Both State objects represent only a subset of the total set of variables,
	 * with this subset being defined by the mapping varMap.
	 * Only variables in this subset are updated.
	 * But if doing so requires old values for variables outside the subset, this will cause an exception. 
	 * It is assumed that any constants have already been defined.
	 * @param oldState Variable values in current state
	 * @param newState State object to apply changes to
	 * @param varMap A mapping from indices (over all variables) to the subset (-1 if not in subset). 
	 */
	public void updatePartially(State oldState, State newState, int[] varMap) throws PrismLangException
	{
		int i, j, n;
		n = exprs.size();
		for (i = 0; i < n; i++) {
			j = varMap[getVarIndex(i)];
			if (j != -1) {
				newState.setValue(j, getExpression(i).evaluate(new EvaluateContextSubstate(oldState, varMap)));
			}
		}
	}

	/**
	 * Check whether this update (from a particular state) would cause any errors, mainly variable overflows.
	 * Variable ranges are specified in the passed in VarList.
	 * Throws an exception if such an error occurs.
	 */
	public State checkUpdate(State oldState, VarList varList) throws PrismLangException
	{
		int i, n, valNew;
		State res;
		res = new State(oldState);
		n = exprs.size();
		for (i = 0; i < n; i++) {
			valNew = varList.encodeToInt(i, getExpression(i).evaluate(oldState));
			if (valNew < varList.getLow(i) || valNew > varList.getHigh(i))
				throw new PrismLangException("Value of variable " + getVar(i) + " overflows", getExpression(i));
		}
		return res;
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
		int i, n;
		String s = "";

		n = exprs.size();
		// normal case
		if (n > 0) {
			for (i = 0; i < n - 1; i++) {
				s = s + "(" + vars.get(i) + "'=" + exprs.get(i) + ") & ";
			}
			s = s + "(" + vars.get(n - 1) + "'=" + exprs.get(n - 1) + ")";
		}
		// special (empty) case
		else {
			s = "true";
		}

		return s;
	}

	/**
	 * Perform a deep copy.
	 */
	public ASTElement deepCopy()
	{
		int i, n;
		Update ret = new Update();
		n = getNumElements();
		for (i = 0; i < n; i++) {
			ret.addElement((ExpressionIdent) getVarIdent(i).deepCopy(), getExpression(i).deepCopy());
			ret.setType(i, getType(i));
			ret.setVarIndex(i, getVarIndex(i));
		}
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
