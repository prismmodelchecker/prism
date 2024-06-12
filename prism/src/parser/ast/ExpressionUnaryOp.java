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

import java.math.BigInteger;

import param.BigRational;
import parser.*;
import parser.EvaluateContext.EvalMode;
import parser.visitor.*;
import prism.PrismLangException;
import parser.type.*;

public class ExpressionUnaryOp extends Expression
{
	// Operator constants
	public static final int NOT = 1;
	public static final int MINUS = 2;
	public static final int PARENTH = 3;
	// Operator symbols
	public static final String opSymbols[] = { "", "!", "-", "()" };

	// Operator
	protected int op = 0;
	// Operand
	protected Expression operand = null;

	// Constructors

	public ExpressionUnaryOp()
	{
	}

	public ExpressionUnaryOp(int op, Expression operand)
	{
		this.operand = operand;
		this.op = op;
	}

	// Set methods

	public void setOperator(int i)
	{
		op = i;
	}

	/**
	 * Set the operator from the operator symbol.
	 */
	public void setOperator(String s) throws PrismLangException
	{
		for (int i = 1; i < opSymbols.length; i++) {
			if (opSymbols[i].equals(s)) {
				setOperator(i);
				return;
			}
		}
		throw new PrismLangException("Unknown unary operator '" + s + "'");
	}

	public void setOperand(Expression e)
	{
		operand = e;
	}

	// Get methods

	public int getOperator()
	{
		return op;
	}

	public String getOperatorSymbol()
	{
		return opSymbols[op];
	}

	public Expression getOperand()
	{
		return operand;
	}

	// Methods required for Expression:

	@Override
	public boolean isConstant()
	{
		return operand.isConstant();
	}

	@Override
	public boolean isProposition()
	{
		return operand.isProposition();
	}
	
	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		Object eval = operand.evaluate(ec);
		return apply(eval, ec.getEvaluationMode());
	}

	/**
	 * Apply this unary operator instance to the argument provided
	 */
	public Object apply(Object eval, EvalMode evalMode) throws PrismLangException
	{
		switch (op) {
		case NOT:
			return !((Boolean) getType().castValueTo(eval));
		case MINUS:
			switch (evalMode) {
			case FP:
				try {
					if (getType() instanceof TypeInt) {
						int i = (int) TypeInt.getInstance().castValueTo(eval);
						return Math.negateExact(i);
					} else {
						double d = (double) TypeDouble.getInstance().castValueTo(eval);
						return -d;
					}
				} catch (ArithmeticException e) {
					throw new PrismLangException(e.getMessage(), this);
				}
			case EXACT:
				if (getType() instanceof TypeInt) {
					BigInteger i = (BigInteger) TypeInt.getInstance().castValueTo(eval);
					return i.negate();
				} else {
					BigRational d = (BigRational) TypeDouble.getInstance().castValueTo(eval);
					return d.negate();
				}
			default:
				throw new PrismLangException("Unknown evaluation mode " + evalMode);
			}
		case PARENTH:
			return eval;
		}
		throw new PrismLangException("Unknown unary operator", this);
	}
	
	@Override
	public boolean returnsSingleValue()
	{
		return operand.returnsSingleValue();
	}

	@Override
	public Precedence getPrecedence()
	{
		switch (op) {
			case NOT:
				return Precedence.NOT;
			case MINUS:
				return Precedence.UNARY_MINUS;
			case PARENTH:
				return Precedence.BASIC;
			default:
				return null;
		}
	}

	// Methods required for ASTElement:

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public ExpressionUnaryOp deepCopy(DeepCopy copier) throws PrismLangException
	{
		operand = copier.copy(operand);

		return this;
	}

	@Override
	public ExpressionUnaryOp clone()
	{
		return (ExpressionUnaryOp) super.clone();
	}

	// Standard methods

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		if (op == PARENTH) {
			builder.append("(");
		} else {
			builder.append(opSymbols[op]);
		}
		builder.append(Expression.toStringPrecLt(operand, this));
		if (op == PARENTH) {
			builder.append(")");
		}
		return builder.toString();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + op;
		result = prime * result + ((operand == null) ? 0 : operand.hashCode());
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
		ExpressionUnaryOp other = (ExpressionUnaryOp) obj;
		if (op != other.op)
			return false;
		if (operand == null) {
			if (other.operand != null)
				return false;
		} else if (!operand.equals(other.operand))
			return false;
		return true;
	}
}

// ------------------------------------------------------------------------------
