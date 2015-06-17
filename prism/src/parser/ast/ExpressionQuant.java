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

import parser.Values;
import prism.OpRelOpBound;
import prism.PrismException;

/**
 * Abstract class for representing "quantitative" operators (P,R,S),
 * i.e., a superclass of ExpressionProb, ExpressionReward, ExpressionSS.
 */
public abstract class ExpressionQuant extends Expression
{
	/** Optional "mode" to specify variants of the P/R/S operator */
	protected String mode = null;
	/** The attached relational operator (e.g. "<" in "P<0.1"). */
	protected RelOp relOp = null;
	/** The attached (probability/reward) bound, as an expression (e.g. "p" in "P<p"). Null if absent (e.g. "P=?"). */
	protected Expression bound = null;
	/** The main operand of the operator (e.g. "F target=true" in "P<0.1[F target=true]. */
	protected Expression expression = null;
	/** Optional "old-style" filter. This is just for display purposes since
	  *  the parser creates an (invisible) new-style filter around this expression. */
	protected Filter filter = null;

	// Set methods

	/**
	 * Set the (optional) "mode" for this operator.
	 */
	public void setMode(String mode)
	{
		this.mode = mode;
	}

	/**
	 * Set the attached relational operator (e.g. "<" in "P<0.1").
	 * Uses the enum {@link RelOp}. For example: {@code setRelOp(RelOp.GT);}
	 */
	public void setRelOp(RelOp relOp)
	{
		this.relOp = relOp;
	}

	/**
	 * Set the attached relational operator (e.g. "<" in "P<0.1").
	 * The operator is passed as a string, e.g. "<" or ">=".
	 */
	public void setRelOp(String relOpString)
	{
		relOp = RelOp.parseSymbol(relOpString);
	}

	/**
	 * Set the attached bound, as an expression (e.g. "p" in "P<p"). Should be null if absent (e.g. "P=?").
	 */
	public void setBound(Expression bound)
	{
		this.bound = bound;
	}

	/**
	 * Set the main operand of the operator (e.g. "F target=true" in "P<0.1[F target=true].
	 */
	public void setExpression(Expression expression)
	{
		this.expression = expression;
	}

	/**
	 * Set the optional "old-style" filter. This is just for display purposes since
	 * the parser creates an (invisible) new-style filter around this expression.
	 */
	public void setFilter(Filter f)
	{
		filter = f;
	}

	// Get methods

	/**
	 * Get the (optional) "mode" for this operator.
	 */
	public String getMode()
	{
		return mode;
	}

	/**
	 * Get a string representing the model as a suffix for the operator.
	 */
	public String getModeString()
	{
		return mode == null ? "" : "(" + mode + ")";
	}

	/**
	 * Get the attached relational operator (e.g. "<" in "P<0.1"), as a {@link RelOp}.
	 */
	public RelOp getRelOp()
	{
		return relOp;
	}

	/**
	 * Get the attached bound, as an expression (e.g. "p" in "P<p"). Should be null if absent (e.g. "P=?").
	 */
	public Expression getBound()
	{
		return bound;
	}

	/**
	 * Get the main operand of the operator (e.g. "F target=true" in "P<0.1[F target=true].
	 */
	public Expression getExpression()
	{
		return expression;
	}

	/**
	 * Get an object storing info about the attached relational operator and bound, after evaluating the bound to a double.
	 * For example "<0.1" in "P<p" where p=0.5 in {@code constantValues}.
	 * Does some checks, e.g., throws an exception if a probability is not in the range [0,1]
	 * 
	 * @param constantValues Values for constants in order to evaluate any bound
	 */
	public abstract OpRelOpBound getRelopBoundInfo(Values constantValues) throws PrismException;

	/**
	 * Get the optional "old-style" filter. This is just for display purposes since
	 * the parser creates an (invisible) new-style filter around this expression.
	 */
	public Filter getFilter()
	{
		return filter;
	}
}

//------------------------------------------------------------------------------
