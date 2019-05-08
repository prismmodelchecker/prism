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
import parser.visitor.ASTVisitor;
import parser.visitor.DeepCopy;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;

public class ExpressionSS extends ExpressionQuant
{
	// Constructors

	public ExpressionSS()
	{
	}

	public ExpressionSS(Expression expression, String relOpString, Expression p)
	{
		setExpression(expression);
		setRelOp(relOpString);
		setBound(p);
	}

	// Set methods

	/**
	 * Set the probability bound. Equivalent to {@code setBound(p)}.
	 */
	public void setProb(Expression p)
	{
		setBound(p);
	}

	// Get methods

	/**
	 * Get the probability bound. Equivalent to {@code getBound()}.
	 */
	public Expression getProb()
	{
		return getBound();
	}

	/**
	 * Get info about the operator and bound.
	 * Does some checks, e.g., throws an exception if probability is out of range.
	 * @param constantValues Values for constants in order to evaluate any bound
	 */
	@Override
	public OpRelOpBound getRelopBoundInfo(Values constantValues) throws PrismException
	{
		if (getBound() != null) {
			double boundValue = getBound().evaluateDouble(constantValues);
			if (boundValue < 0 || boundValue > 1)
				throw new PrismException("Invalid probability bound " + boundValue + " in P operator");
			return new OpRelOpBound("S", getRelOp(), boundValue);
		} else {
			return new OpRelOpBound("S", getRelOp(), null);
		}
	}

	// Methods required for Expression:

	@Override
	public String getResultName()
	{
		return (getBound() == null) ? "Probability" : "Result";
	}

	// Methods required for ASTElement:

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public ExpressionSS deepCopy(DeepCopy copier) throws PrismLangException
	{
		return (ExpressionSS) super.deepCopy(copier);
	}

	@Override
	public ExpressionSS clone()
	{
		return (ExpressionSS) super.clone();
	}

	// Standard methods

	@Override
	protected String operatorToString()
	{
		return "S" + getModifierString();
	}

	@Override
	protected String bodyToString()
	{
		String filter = getFilter() == null ? "" : " " + getFilter();
		return getExpression() + filter;
	}
}

//------------------------------------------------------------------------------
