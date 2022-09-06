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
import java.util.List;

import param.BigRational;
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
	
	/** Child expression(s) */
	protected List<Expression> operands = new ArrayList<Expression>();
	
	/** Is there just a single operand (P/R operator)? If not, the operand list will be parenthesised. **/
	protected boolean singleOperand = false;
	
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
		operands.add(expression);
		singleOperand = true;
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

	public void setSingleOperand(Expression expression)
	{
		operands.clear();
		operands.add(expression);
		singleOperand = true;
	}

	public void addOperand(Expression e)
	{
		operands.add(e);
	}

	public void setOperand(int i, Expression e)
	{
		operands.set(i, e);
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
	
	public boolean hasSingleOperand()
	{
		return singleOperand;
	}
	
	public int getNumOperands()
	{
		return operands.size();
	}

	public Expression getOperand(int i)
	{
		return operands.get(i);
	}

	public List<Expression> getOperands()
	{
		return operands;
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
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate a " + getOperatorString() + " operator without a model");
	}

	/*@Override
	public String getResultName()
	{
		return expression.getResultName();
	}*/

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
		for (Expression operand : operands) {
			expr.addOperand((Expression) operand.deepCopy());
		}
		expr.singleOperand = singleOperand;
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
		s += (thereExists ? ">> " : "]] ");
		if (singleOperand) {
			s += operands.get(0);
		} else {
			s += "(";
			boolean first = true;
			for (Expression operand : operands) {
				if (!first)
					s += ", ";
				else
					first = false;
				s = s + operand;
			}
			s += ")";
		}
		return s;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((coalition == null) ? 0 : coalition.hashCode());
		result = prime * result + ((operands == null) ? 0 : operands.hashCode());
		result = prime * result + (singleOperand ? 1231 : 1237);
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
		if (operands == null) {
			if (other.operands != null)
				return false;
		} else if (!operands.equals(other.operands))
			return false;
		if (singleOperand != other.singleOperand)
			return false;
		if (thereExists != other.thereExists)
			return false;
		return true;
	}
}
