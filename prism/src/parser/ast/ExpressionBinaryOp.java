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

public class ExpressionBinaryOp extends Expression
{
	// Operator constants
	public static final int IMPLIES = 1;
	public static final int OR = 2;
	public static final int AND = 3;
	public static final int EQ = 4;
	public static final int NE = 5;
	public static final int GT = 6;
	public static final int GE = 7;
	public static final int LT = 8;
	public static final int LE = 9;
	public static final int PLUS = 10;
	public static final int MINUS = 11;
	public static final int TIMES = 12;
	public static final int DIVIDE = 13;
	// Operator symbols
	public static final String opSymbols[] = { "", "=>", "|", "&", "=", "!=", ">", ">=", "<", "<=", "+", "-", "*", "/" };

	// Operator
	protected int op = 0;
	// Pair of operands
	protected Expression operand1 = null;
	protected Expression operand2 = null;

	// Constructors

	public ExpressionBinaryOp()
	{
	}

	public ExpressionBinaryOp(int op, Expression operand1, Expression operand2)
	{
		this.operand1 = operand1;
		this.operand2 = operand2;
		this.op = op;
	}

	// Set methods

	public void setOperator(int i)
	{
		op = i;
	}

	public void setOperand1(Expression e1)
	{
		operand1 = e1;
	}

	public void setOperand2(Expression e2)
	{
		operand2 = e2;
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

	public Expression getOperand1()
	{
		return operand1;
	}

	public Expression getOperand2()
	{
		return operand2;
	}

	// Methods required for Expression:

	/**
	 * Is this expression constant?
	 */
	public boolean isConstant()
	{
		return operand1.isConstant() && operand2.isConstant();
	}

	/**
	 * Evaluate this expression, return result. Note: assumes that type checking
	 * has been done already.
	 */
	public Object evaluate(Values constantValues, Values varValues) throws PrismLangException
	{
		switch (op) {
		case IMPLIES:
			return new Boolean(!operand1.evaluateBoolean(constantValues, varValues)
					|| operand2.evaluateBoolean(constantValues, varValues));
		case OR:
			return new Boolean(operand1.evaluateBoolean(constantValues, varValues)
					|| operand2.evaluateBoolean(constantValues, varValues));
		case AND:
			return new Boolean(operand1.evaluateBoolean(constantValues, varValues)
					&& operand2.evaluateBoolean(constantValues, varValues));
		case EQ:
			if (operand1.getType() == Expression.INT && operand2.getType() == Expression.INT) {
				return new Boolean(operand1.evaluateInt(constantValues, varValues) == operand2.evaluateInt(
						constantValues, varValues));
			} else {
				return new Boolean(operand1.evaluateDouble(constantValues, varValues) == operand2.evaluateDouble(
						constantValues, varValues));
			}
		case NE:
			if (operand1.getType() == Expression.INT && operand2.getType() == Expression.INT) {
				return new Boolean(operand1.evaluateInt(constantValues, varValues) != operand2.evaluateInt(
						constantValues, varValues));
			} else {
				return new Boolean(operand1.evaluateDouble(constantValues, varValues) != operand2.evaluateDouble(
						constantValues, varValues));
			}
		case GT:
			if (operand1.getType() == Expression.INT && operand2.getType() == Expression.INT) {
				return new Boolean(operand1.evaluateInt(constantValues, varValues) > operand2.evaluateInt(
						constantValues, varValues));
			} else {
				return new Boolean(operand1.evaluateDouble(constantValues, varValues) > operand2.evaluateDouble(
						constantValues, varValues));
			}
		case GE:
			if (operand1.getType() == Expression.INT && operand2.getType() == Expression.INT) {
				return new Boolean(operand1.evaluateInt(constantValues, varValues) >= operand2.evaluateInt(
						constantValues, varValues));
			} else {
				return new Boolean(operand1.evaluateDouble(constantValues, varValues) >= operand2.evaluateDouble(
						constantValues, varValues));
			}
		case LT:
			if (operand1.getType() == Expression.INT && operand2.getType() == Expression.INT) {
				return new Boolean(operand1.evaluateInt(constantValues, varValues) < operand2.evaluateInt(
						constantValues, varValues));
			} else {
				return new Boolean(operand1.evaluateDouble(constantValues, varValues) < operand2.evaluateDouble(
						constantValues, varValues));
			}
		case LE:
			if (operand1.getType() == Expression.INT && operand2.getType() == Expression.INT) {
				return new Boolean(operand1.evaluateInt(constantValues, varValues) <= operand2.evaluateInt(
						constantValues, varValues));
			} else {
				return new Boolean(operand1.evaluateDouble(constantValues, varValues) <= operand2.evaluateDouble(
						constantValues, varValues));
			}
		case PLUS:
			if (operand1.getType() == Expression.INT && operand2.getType() == Expression.INT) {
				return new Integer(operand1.evaluateInt(constantValues, varValues)
						+ operand2.evaluateInt(constantValues, varValues));
			} else {
				return new Double(operand1.evaluateDouble(constantValues, varValues)
						+ operand2.evaluateDouble(constantValues, varValues));
			}
		case MINUS:
			if (operand1.getType() == Expression.INT && operand2.getType() == Expression.INT) {
				return new Integer(operand1.evaluateInt(constantValues, varValues)
						- operand2.evaluateInt(constantValues, varValues));
			} else {
				return new Double(operand1.evaluateDouble(constantValues, varValues)
						- operand2.evaluateDouble(constantValues, varValues));
			}
		case TIMES:
			if (operand1.getType() == Expression.INT && operand2.getType() == Expression.INT) {
				return new Integer(operand1.evaluateInt(constantValues, varValues)
						* operand2.evaluateInt(constantValues, varValues));
			} else {
				return new Double(operand1.evaluateDouble(constantValues, varValues)
						* operand2.evaluateDouble(constantValues, varValues));
			}
		case DIVIDE:
			return new Double(operand1.evaluateDouble(constantValues, varValues)
					/ operand2.evaluateDouble(constantValues, varValues));
		}
		throw new PrismLangException("Unknown binary operator", this);
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
		return operand1 + opSymbols[op] + operand2;
	}

	/**
	 * Perform a deep copy.
	 */
	public Expression deepCopy()
	{
		ExpressionBinaryOp expr = new ExpressionBinaryOp(op, operand1.deepCopy(), operand2.deepCopy());
		expr.setType(type);
		expr.setPosition(this);
		return expr;
	}
}

// ------------------------------------------------------------------------------
