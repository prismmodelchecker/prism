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

import parser.visitor.*;
import prism.PrismLangException;

/**
 * Old-style filter, as embedded in P/R/S operator.
 */
public class Filter extends ASTElement
{
	private Expression expr = null;
	// Either "min" or "max", or neither or both.
	// In the latter two cases, this means "state" or "range"
	private boolean minReq = false;
	private boolean maxReq = false;

	// Constructor
	
	public Filter(Expression expr)
	{
		this.expr = expr;
	}

	// Set methods
	
	public void setExpression(Expression expr)
	{
		this.expr = expr;
	}

	public void setMinRequested(boolean b)
	{
		minReq = b;
	}

	public void setMaxRequested(boolean b)
	{
		maxReq = b;
	}
	
	// Get methods
	
	public Expression getExpression()
	{
		return expr;
	}

	public boolean minRequested()
	{
		return minReq;
	}

	public boolean maxRequested()
	{
		return maxReq;
	}

	/**
	 * Get (as a string) the operator for this filter
	 * (as need to construct an ExpressionFilter object)
	 */
	public String getFilterOpString()
	{
		if (minReq) {
			return maxReq ? "range" : "min";
		} else {
			return maxReq ? "max" : "state";
		}
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
		s += "{" + expr + "}";
		if (minReq) s += "{min}";
		if (maxReq) s += "{max}";
		return s;
	}
	
	/**
	 * Perform a deep copy.
	 */
	public ASTElement deepCopy()
	{
		Filter ret = new Filter(expr.deepCopy());
		ret.setMinRequested(minReq);
		ret.setMaxRequested(maxReq);
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
