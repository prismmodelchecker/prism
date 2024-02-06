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

import parser.EvaluateContext;
import parser.EvaluateContext.EvalMode;
import parser.type.TypeBool;
import parser.visitor.ASTVisitor;
import parser.visitor.DeepCopy;
import prism.PrismLangException;

public class ExpressionITE extends Expression
{
	// Operands
	protected Expression operand1 = null; // condition
	protected Expression operand2 = null; // then
	protected Expression operand3 = null; // else

	// Constructor

	public ExpressionITE(Expression c, Expression t, Expression e)
	{
		operand1 = c;
		operand2 = t;
		operand3 = e;
	}

	// Set methods

	public void setOperand1(Expression e)
	{
		operand1 = e;
	}

	public void setOperand2(Expression e)
	{
		operand2 = e;
	}

	public void setOperand3(Expression e)
	{
		operand3 = e;
	}

	// Get methods

	public Expression getOperand1()
	{
		return operand1;
	}

	public Expression getOperand2()
	{
		return operand2;
	}

	public Expression getOperand3()
	{
		return operand3;
	}

	// Methods required for Expression:

	@Override
	public boolean isConstant()
	{
		return operand1.isConstant() && operand2.isConstant() && operand3.isConstant();
	}

	@Override
	public boolean isProposition()
	{
		return operand1.isProposition() && operand2.isProposition() && operand3.isProposition();
	}
	
	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		// Note that we don't use apply(...) because we want short-circuiting
		Object eval1 = operand1.evaluate(ec);
		boolean b = TypeBool.getInstance().castValueTo(eval1);
		return getType().castValueTo(b ? operand2.evaluate(ec) : operand3.evaluate(ec), ec.getEvaluationMode());
	}

	/**
	 * Apply this ITE operator instance to the arguments provided
	 */
	public Object apply(Object eval1, Object eval2, Object eval3, EvalMode evalMode) throws PrismLangException
	{
		boolean b = TypeBool.getInstance().castValueTo(eval1);
		return getType().castValueTo(b ? eval2 : eval3, evalMode);
	}
	
	@Override
	public boolean returnsSingleValue()
	{
		return operand1.returnsSingleValue() && operand2.returnsSingleValue() && operand3.returnsSingleValue();
	}

	@Override
	public Precedence getPrecedence()
	{
		return Precedence.ITE;
	}

	// Methods required for ASTElement:

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public ExpressionITE deepCopy(DeepCopy copier) throws PrismLangException
	{
		operand1 = copier.copy(operand1);
		operand2 = copier.copy(operand2);
		operand3 = copier.copy(operand3);

		return this;
	}

	@Override
	public ExpressionITE clone()
	{
		return (ExpressionITE) super.clone();
	}

	// Standard methods
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		// ? is a (right-associative) non-commutative operator
		builder.append(Expression.toStringPrecLeq(operand1, this));
		builder.append("?");
		builder.append(Expression.toStringPrecLeq(operand2, this));
		builder.append(":");
		builder.append(Expression.toStringPrecLt(operand3, this));
		return builder.toString();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((operand1 == null) ? 0 : operand1.hashCode());
		result = prime * result + ((operand2 == null) ? 0 : operand2.hashCode());
		result = prime * result + ((operand3 == null) ? 0 : operand3.hashCode());
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
		ExpressionITE other = (ExpressionITE) obj;
		if (operand1 == null) {
			if (other.operand1 != null)
				return false;
		} else if (!operand1.equals(other.operand1))
			return false;
		if (operand2 == null) {
			if (other.operand2 != null)
				return false;
		} else if (!operand2.equals(other.operand2))
			return false;
		if (operand3 == null) {
			if (other.operand3 != null)
				return false;
		} else if (!operand3.equals(other.operand3))
			return false;
		return true;
	}
}

// ------------------------------------------------------------------------------
