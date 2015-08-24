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

import java.util.List;

import parser.EvaluateContext;
import parser.visitor.ASTVisitor;
import prism.PrismLangException;

/**
 * Class to represent ATL &lt;&lt;.&gt;&gt; and [[.]] operators, i.e. quantification over strategies
 * ("there exists a strategy" or "for all strategies").
 */
public class ExpressionStrategy extends Expression
{
	/** "There exists a strategy" (true) or "for all strategies" (false) */
	protected boolean thereExists = false;
	
	/** Coalition info (for game models) */
	protected Coalition coalition = new Coalition(); 
	
	/** Child expression */
	protected Expression expression = null;

	// Constructors

	public ExpressionStrategy()
	{
	}

	public ExpressionStrategy(boolean thereExists)
	{
		this.thereExists = thereExists;
	}

	public ExpressionStrategy(boolean thereExists, Expression expression)
	{
		this.thereExists = thereExists;
		this.expression = expression;
	}

	// Set methods

	public void setThereExists(boolean thereExists)
	{
		this.thereExists = thereExists;
	}
	
	public void setCoalitionAllPlayers()
	{
		this.coalition.setAllPlayers();
	}

	public void setCoalition(List<String> coalition)
	{
		this.coalition.setPlayers(coalition);
	}

	public void setExpression(Expression expression)
	{
		this.expression = expression;
	}

	// Get methods

	public boolean isThereExists()
	{
		return thereExists;
	}

	/**
	 * Get a string ""&lt;&lt;&gt;&gt;"" or "[[]]" indicating type of quantification.
	 */
	public String getOperatorString()
	{
		return thereExists ? "<<>>" : "[[]]";
	}

	public Coalition getCoalition()
	{
		return coalition;
	}
	
	public boolean coalitionIsAllPlayers()
	{
		return coalition.isAllPlayers();
	}
	
	public List<String> getCoalitionPlayers()
	{
		return coalition.getPlayers();
	}
	
	public Expression getExpression()
	{
		return expression;
	}

	// Methods required for Expression:

	@Override
	public boolean isConstant()
	{
		return false;
	}

	@Override
	public boolean isProposition()
	{
		return false;
	}

	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate a " + getOperatorString() + " operator without a model");
	}

	@Override
	public String getResultName()
	{
		return expression.getResultName();
	}

	@Override
	public boolean returnsSingleValue()
	{
		return false;
	}

	// Methods required for ASTElement:

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public Expression deepCopy()
	{
		ExpressionStrategy expr = new ExpressionStrategy();
		expr.setThereExists(isThereExists());
		expr.coalition = new Coalition(coalition);
		expr.setExpression(expression == null ? null : expression.deepCopy());
		expr.setType(type);
		expr.setPosition(this);
		return expr;
	}

	// Standard methods

	@Override
	public String toString()
	{
		String s = "";
		s += (thereExists ? "<<" : "[[");
		s += coalition;
		s += (thereExists ? ">>" : "]]");
		s += " " + expression.toString();
		return s;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((coalition == null) ? 0 : coalition.hashCode());
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
		result = prime * result + (thereExists ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExpressionStrategy other = (ExpressionStrategy) obj;
		if (coalition == null) {
			if (other.coalition != null)
				return false;
		} else if (!coalition.equals(other.coalition))
			return false;
		if (expression == null) {
			if (other.expression != null)
				return false;
		} else if (!expression.equals(other.expression))
			return false;
		if (thereExists != other.thereExists)
			return false;
		return true;
	}
}
