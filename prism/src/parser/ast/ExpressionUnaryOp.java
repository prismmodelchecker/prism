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

import param.BigRational;
import parser.*;
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
		switch (op) {
		case NOT:
			return new Boolean(!operand.evaluateBoolean(ec));
		case MINUS:
			if (type instanceof TypeInt) {
				return new Integer(-operand.evaluateInt(ec));
			} else {
				return new Double(-operand.evaluateDouble(ec));
			}
		case PARENTH:
			return operand.evaluate(ec);
		}
		throw new PrismLangException("Unknown unary operator", this);
	}

	@Override
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException
	{
		switch (op) {
		case NOT:
			return BigRational.from(!operand.evaluateExact(ec).toBoolean());
		case MINUS:
			return operand.evaluateExact().negate();
		case PARENTH:
			return operand.evaluateExact(ec);
		}
		throw new PrismLangException("Unknown unary operator", this);
	}

	@Override
	public boolean returnsSingleValue()
	{
		return operand.returnsSingleValue();
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
		ExpressionUnaryOp expr = new ExpressionUnaryOp(op, operand.deepCopy());
		expr.setType(type);
		expr.setPosition(this);
		return expr;
	}

	// Standard methods

	@Override
	public String toString()
	{
		if (op == PARENTH)
			return "(" + operand + ")";
		else
			return opSymbols[op] + operand;
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
