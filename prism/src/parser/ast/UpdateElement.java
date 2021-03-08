//==============================================================================
//	
//	Copyright (c) 2018
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

import param.BigRational;
import parser.State;
import parser.Values;
import parser.VarList;
import parser.type.Type;
import parser.visitor.ASTVisitor;
import prism.PrismLangException;

/**
 * Class to store a single element of an Update, i.e. a single assignment (e.g. s'=1)
 */
public class UpdateElement extends ASTElement
{
	/** The variable that is assigned to */
	private String var;
	/** The expression for the assignment value */
	private Expression expr;
	/** The type of the assignment (initially empty / unknown) */
	private Type type;
	/** The identifier expression for the variable (for position information, etc) */
	private ExpressionIdent ident;
	/** The variable index (initially unknown, i.e., -1) */
	private int index;

	/** Constructor */
	public UpdateElement(ExpressionIdent v, Expression e)
	{
		var = v.getName();
		expr = e;
		type = null; // Type currently unknown
		ident = v;
		index = -1; // Index currently unknown
	}

	/** Shallow copy constructor */
	public UpdateElement(UpdateElement other)
	{
		var = other.var;
		expr = other.expr;
		type = other.type;
		ident = other.ident;
		index = other.index;
	}

	// Getters

	/** Get the name of the variable that is the assignment target */
	public String getVar()
	{
		return var;
	}

	/** Get the update expression */
	public Expression getExpression()
	{
		return expr;
	}

	/** Get the type of the update */
	public Type getType()
	{
		return type;
	}

	/** Get the ExpressionIdent corresponding to the variable name (for position information) */
	public ExpressionIdent getVarIdent()
	{
		return ident;
	}

	/** Set the name of the variable that is the assignment target */
	public void setVar(String var)
	{
		this.var = var;
	}

	/** Get the variable index for the variable that is the assignment target */
	public int getVarIndex()
	{
		return index;
	}

	// Setters
	
	/** Set the update expression */
	public void setExpression(Expression expr)
	{
		this.expr = expr;
	}

	/** Set the type of the update */
	public void setType(Type type)
	{
		this.type = type;
	}

	/** Set the ExpressionIdent corresponding to the variable name (for position information) */
	public void setVarIdent(ExpressionIdent ident)
	{
		this.ident = ident;
		this.var = ident.getName();
	}

	/** Set the variable index for the variable that is the assignment target */
	public void setVarIndex(int index)
	{
		this.index = index;
	}

	/**
	 * Execute this update element, based on variable values specified as a Values object,
	 * applying changes in variables to a second Values object. 
	 * Values of any constants should also be provided.
	 * @param constantValues Values for constants
	 * @param oldValues Variable values in current state
	 * @param newValues Values object to apply changes to
	 */
	public void update(Values constantValues, Values oldValues, Values newValues) throws PrismLangException
	{
		newValues.setValue(var, expr.evaluate(constantValues, oldValues));
	}

	/**
	 * Execute this update element, based on variable values specified as a State object.
	 * It is assumed that any constants have already been defined.
	 * @param oldState Variable values in current state
	 * @param newState State object to apply changes to
	 */
	public void update(State oldState, State newState) throws PrismLangException
	{
		update(oldState, newState, false);
	}

	/**
	 * Execute this update element, based on variable values specified as a State object.
	 * It is assumed that any constants have already been defined.
	 * @param oldState Variable values in current state
	 * @param newState State object to apply changes to
	 * @param exact evaluate arithmetic expressions exactly?
	 */
	public void update(State oldState, State newState, boolean exact) throws PrismLangException
	{
		Object newValue;
		if (exact) {
			BigRational r = expr.evaluateExact(oldState);
			// cast to Java data type
			newValue = expr.getType().castFromBigRational(r);
		} else {
			newValue = expr.evaluate(oldState);
		}
		newState.setValue(index, newValue);
	}
	
	/**
	 * Check whether this update (from a particular state) would cause any errors, mainly variable overflows.
	 * Variable ranges are specified in the passed in VarList.
	 * Throws an exception if such an error occurs.
	 */
	public void checkUpdate(State oldState, VarList varList) throws PrismLangException
	{
		int valNew;
		valNew = varList.encodeToInt(index, expr.evaluate(oldState));
		if (valNew < varList.getLow(index) || valNew > varList.getHigh(index))
			throw new PrismLangException("Value of variable " + var + " overflows", expr);
	}


	// Methods required for ASTElement:

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public UpdateElement deepCopy()
	{
		UpdateElement result = new UpdateElement((ExpressionIdent)ident.deepCopy(), expr.deepCopy());
		result.type = type;
		result.index = index;
		return result;
	}
	
	// Other methods:
	
	@Override
	public String toString()
	{
		return "(" + getVar() + "'=" + getExpression() + ")";		
	}
}
