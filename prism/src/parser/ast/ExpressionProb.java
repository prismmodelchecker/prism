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
import prism.PrismLangException;

public class ExpressionProb extends ExpressionQuant
{
	// Constructors

	public ExpressionProb()
	{
	}

	public ExpressionProb(Expression expression, String relOpString, Expression p)
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
	 * Get a string describing the type of P operator, e.g. "P=?" or "P&lt;p".
	 */
	public String getTypeOfPOperator()
	{
		String s = "";
		s += "P" + getRelOp();
		s += (getBound() == null) ? "?" : "p";
		return s;
	}

	@Override
	public OpRelOpBound getRelopBoundInfo(Values constantValues) throws PrismLangException
	{
		if (getBound() != null) {
			double boundVal = getBound().evaluateDouble(constantValues);
			if (boundVal < 0 || boundVal > 1)
				throw new PrismLangException("Invalid probability bound " + boundVal + " in P operator");
			return new OpRelOpBound("P", getRelOp(), boundVal);
		} else {
			return new OpRelOpBound("P", getRelOp(), null);
		}
	}

	// Methods required for Expression:

	@Override
	public String getResultName()
	{
		if (getBound() != null)
			return "Result";
		else if (getRelOp() == RelOp.MIN)
			return "Minimum probability";
		else if (getRelOp() == RelOp.MAX)
			return "Maximum probability";
		else
			return "Probability";
	}

	// Methods required for ASTElement:

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public ExpressionProb deepCopy(DeepCopy copier) throws PrismLangException
	{
		return (ExpressionProb) super.deepCopy(copier);
	}

	@Override
	public ExpressionProb clone()
	{
		return (ExpressionProb) super.clone();
	}

	// Standard methods

	@Override
	protected String operatorToString()
	{
		return "P" + getModifierString();
	}

	@Override
	protected String bodyToString()
	{
		String filter = getFilter() == null ? "" : " " + getFilter();
		return getExpression() + filter;
	}
}

//------------------------------------------------------------------------------
