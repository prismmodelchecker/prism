//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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
import java.util.Iterator;
import java.util.stream.Collectors;

import parser.EvaluateContextSubstate;
import parser.State;
import parser.Values;
import parser.VarList;
import parser.type.Type;
import parser.visitor.ASTVisitor;
import prism.PrismLangException;

/**
 * Class to store a single update, i.e. a list of assignments of variables to expressions, e.g. (s'=1)&amp;(x'=x+1)
 */
public class Update extends ASTElement implements Iterable<UpdateElement>
{
	// Individual elements of update
	private ArrayList<UpdateElement> elements;

	// Parent Updates object
	private Updates parent;

	/**
	 * Create an empty update.
	 */
	public Update()
	{
		elements = new ArrayList<>();
	}

	// Set methods

	/**
	 * Add a variable assignment ({@code v}'={@code e}) to this update.
	 * @param v The AST element corresponding to the variable being updated
	 * @param e The expression which will be assigned to the variable
	 */
	public void addElement(ExpressionIdent v, Expression e)
	{
		elements.add(new UpdateElement(v, e));
	}

	/**
	 * Add a variable assignment (encoded as an UpdateElement) to this update.
	 */
	public void addElement(UpdateElement e)
	{
		elements.add(e);
	}

	/**
	 * Set the ith variable assignment (encoded as an UpdateElement) to this update.
	 */
	public void setElement(int i, UpdateElement e)
	{
		elements.set(i, e);
	}

	/**
	 * Set the variable {@code v} for the {@code i}th variable assignment of this update.
	 * @param i The index of the variable assignment within the update
	 * @param v The AST element corresponding to the variable being updated
	 */
	public void setVar(int i, ExpressionIdent v)
	{
		elements.get(i).setVarIdent(v);
	}

	/**
	 * Set the expression {@code e} for the {@code i}th variable assignment of this update.
	 * @param i The index of the variable assignment within the update
	 * @param e The expression which will be assigned to the variable
	 */
	public void setExpression(int i, Expression e)
	{
		elements.get(i).setExpression(e);
	}

	/**
	 * Set the type of the {@code i}th variable assigned to by this update.
	 * @param i The index of the variable assignment within the update
	 * @param t The variable's type
	 */
	public void setType(int i, Type t)
	{
		elements.get(i).setType(t);
	}

	/**
	 * Set the index (wrt the model) of the {@code i}th variable assigned to by this update.
	 * @param i The index of the variable assignment within the update
	 * @param t The index of the variable within the model to which it belongs
	 */
	public void setVarIndex(int i, int index)
	{
		elements.get(i).setVarIndex(index);
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
		return elements.size();
	}

	/** Get the update element (individual assignment) with the given index. */
	public UpdateElement getElement(int index)
	{
		return elements.get(index);
	}
	
	/**
	 * Get the name of the {@code i}th variable in this update.
	 */
	public String getVar(int i)
	{
		return elements.get(i).getVar();
	}

	/**
	 * Get the expression used to update the {@code i}th variable in this update.
	 */
	public Expression getExpression(int i)
	{
		return elements.get(i).getExpression();
	}

	/**
	 * Get the type of the {@code i}th variable in this update.
	 */
	public Type getType(int i)
	{
		return elements.get(i).getType();
	}

	/**
	 * Get the ASTElement corresponding to the {@code i}th variable in this update.
	 */
	public ExpressionIdent getVarIdent(int i)
	{
		return elements.get(i).getVarIdent();
	}

	/**
	 * Get the index (wrt the model) of the {@code i}th variable in this update.
	 */
	public int getVarIndex(int i)
	{
		return elements.get(i).getVarIndex();
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
		Values res = new Values(oldValues);
		update(constantValues, oldValues, res);
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
		for (UpdateElement e : this) {
			e.update(constantValues, oldValues, newValues);
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
		State res = new State(oldState);
		update(oldState, res);
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
		for (UpdateElement e : this) {
			e.update(oldState, newState, exact);
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
		n = elements.size();
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
		State res;
		res = new State(oldState);
		for (UpdateElement e : this) {
			e.checkUpdate(oldState, varList);
		}
		return res;
	}

	// Methods required for ASTElement:

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public ASTElement deepCopy()
	{
		Update ret = new Update();
		for (UpdateElement e : this) {
			ret.addElement(e.deepCopy());
		}
		ret.setPosition(this);
		return ret;
	}
	
	// Other methods:
	
	@Override
	public Iterator<UpdateElement> iterator()
	{
		return elements.iterator();
	}
	
	@Override
	public String toString()
	{
		// Normal case
		if (elements.size() > 0) {
			return elements.stream().map(UpdateElement::toString).collect(Collectors.joining(" & "));
		}
		// Special (empty) case
		else {
			return "true";
		}
	}
}

//------------------------------------------------------------------------------
