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

import parser.*;
import parser.visitor.*;
import prism.PrismLangException;

public class ExpressionProb extends Expression
{
	String relOp = null;
	Expression prob = null;
	PathExpression pathExpression = null;
	Filter filter = null;
	
	// Constructors
	
	public ExpressionProb()
	{
	}
	
	public ExpressionProb(PathExpression e, String r, Expression p)
	{
		pathExpression = e;
		relOp = r;
		prob = p;
	}

	// Set methods
	
	public void setRelOp(String r)
	{
		relOp =r;
	}

	public void setProb(Expression p)
	{
		prob = p;
	}

	public void setPathExpression(PathExpression e)
	{
		pathExpression = e;
	}
	
	public void setFilter(Filter f)
	{
		filter = f;
	}

	// Get methods
	
	public String getRelOp()
	{
		return relOp;
	}
	
	public Expression getProb()
	{
		return prob;
	}

	public PathExpression getPathExpression()
	{
		return pathExpression;
	}
	
	public Filter getFilter()
	{
		return filter;
	}

	public Expression getFilterExpression()
	{
		return filter.getExpression();
	}

	public boolean filterMinRequested()
	{
		return filter.minRequested();
	}

	public boolean filterMaxRequested()
	{
		return filter.maxRequested();
	}

	public boolean noFilterRequests()
	{
		return filter.noRequests();
	}

	// Methods required for Expression:
	
	/**
	 * Is this expression constant?
	 */
	public boolean isConstant()
	{
		return false;
	}

	/**
	 * Evaluate this expression, return result.
	 * Note: assumes that type checking has been done already.
	 */
	public Object evaluate(Values constantValues, Values varValues) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate a P operator without model checking");
	}

	/**
	  * Get "name" of the result of this expression (used for y-axis of any graphs plotted)
	  */
	public String getResultName()
	{
		return (prob == null) ? "Probability" : "Result";
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
		
		s += "P" + relOp;
		s += (prob==null) ? "?" : prob.toString();
		s += " [ " + pathExpression;
		if (filter != null) s += " "+filter;
		s += " ]";
		
		return s;
	}

	/**
	 * Perform a deep copy.
	 */
	public Expression deepCopy()
	{
		ExpressionProb expr = new ExpressionProb(pathExpression.deepCopy(), relOp, prob.deepCopy());
		expr.setFilter((Filter)filter.deepCopy());
		expr.setType(type);
		expr.setPosition(this);
		return expr;
	}
}

//------------------------------------------------------------------------------
