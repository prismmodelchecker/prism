//==============================================================================
//	
//	Copyright (c) 2020-
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import parser.visitor.ASTVisitor;
import prism.PrismLangException;

/**
 * A list of variables that are observable
 */
public class ObservableVars extends ASTElement
{
	/**
	 * List of variables (stored as AST elements referencing them)
	 */
	private List<Expression> vars;
	
	// Constructor
	
	/**
	 * Create a new (empty) observable variables list.
	 */
	public ObservableVars()
	{
		vars = new ArrayList<>();
	}

	// Mutators
	
	/**
	 * Add a variable to the list.
	 * It is specified via the AST element from the model description,
	 * e.g. an ExpressionIdent, for improved error reporting.
	 */
	public void addVar(Expression exprVar)
	{
		vars.add(exprVar);
	}
	
	/**
	 * Add a variable to the list, specified by its name.
	 */
	public void addVar(String varName)
	{
		addVar(new ExpressionIdent(varName));
	}
	
	/**
	 * Set the {@code i}th variable in the list.
	 * It is specified via the AST element from the model description,
	 * e.g. an ExpressionIdent, for improved error reporting.
	 */
	public void setVar(int i, Expression exprVar)
	{
		vars.set(i, exprVar);
	}
	
	// Accessors
	
	/**
	 * Get the number of variables in the list.
	 */
	public int getNumVars()
	{
		return vars.size();
	}
	
	/**
	 * Get the {@code i}th variable in the list.
	 * Returned as an AST element referring to it.
	 */
	public Expression getVar(int i)
	{
		return vars.get(i);
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
		String s = "observables";
		if (!vars.isEmpty()) {
			s += vars.stream().map(e -> e.toString()).collect(Collectors.joining(", ", " ", "")) + "";
		}
		s += " endobservables";
		return s;
	}
	
	/**
	 * Perform a deep copy.
	 */
	public ObservableVars deepCopy()
	{
		ObservableVars ret = new ObservableVars();
		for (Expression var : vars) {
			ret.addVar(var.deepCopy());
		}
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
