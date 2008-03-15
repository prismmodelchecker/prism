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

import parser.*;
import parser.visitor.*;
import prism.PrismLangException;

public class Update extends ASTElement
{
	// Lists of variable/expression pairs (and types)
	private ArrayList<String> vars;
	private ArrayList<Expression> exprs;
	private ArrayList<Integer> types;
	// We also store an ExpressionIdent to match each variable.
	// This is to just to provide positional info.
	private ArrayList<ExpressionIdent> varIdents;
	// Parent Updates object
	private Updates parent;
	
	// Constructor
	
	public Update()
	{
		vars = new ArrayList<String>();
		exprs = new ArrayList<Expression>();
		types = new ArrayList<Integer>();
		varIdents = new ArrayList<ExpressionIdent>();
	}
	
	// Set methods
	
	public void addElement(ExpressionIdent v, Expression e)
	{
		vars.add(v.getName());
		exprs.add(e);
		types.add(0); // Type currently unknown
		varIdents.add(v);
	}
	
	public void setVar(int i, ExpressionIdent v)
	{
		vars.set(i, v.getName());
		varIdents.set(i, v);
	}
	
	public void setExpression(int i, Expression e)
	{
		exprs.set(i, e);
	}
	
	public void setType(int i, int t)
	{
		types.set(i, t);
	}
	
	public void setParent(Updates u)
	{
		parent = u;
	}
	
	// Get methods
	
	public int getNumElements()
	{
		return vars.size();
	}
	
	public String getVar(int i)
	{
		return vars.get(i);
	}
	
	public Expression getExpression(int i)
	{
		return exprs.get(i);
	}
	
	public int getType(int i)
	{
		return types.get(i);
	}
	
	public ExpressionIdent getVarIdent(int i)
	{
		return varIdents.get(i);
	}
	
	public Updates getParent()
	{
		return parent;
	}

	// create new Values object according to this update
	
	public Values update(Values constantValues, Values oldValues) throws PrismLangException
	{
		int i, n;
		Values res;
		
		res = new Values();
		n = exprs.size();
		for (i = 0; i < n; i++) {
			res.setValue(getVar(i), getExpression(i).evaluate(constantValues, oldValues));
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
			for (i = 0; i < n-1; i++) {
				s = s + "(" + vars.get(i) + "'=" + exprs.get(i) + ") & ";
			}
			s = s + "(" + vars.get(n-1) + "'=" + exprs.get(n-1) + ")";
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
			ret.addElement((ExpressionIdent)getVarIdent(i).deepCopy(), getExpression(i).deepCopy());
			ret.setType(i, getType(i));
		}
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
