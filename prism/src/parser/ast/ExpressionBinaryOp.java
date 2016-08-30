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
import parser.EvaluateContext;
import parser.type.TypeInt;
import parser.visitor.ASTVisitor;
import prism.PrismLangException;

public class ExpressionBinaryOp extends Expression
{
	// Operator constants
	public static final int IMPLIES = 1;
	public static final int IFF = 2;
	public static final int OR = 3;
	public static final int AND = 4;
	public static final int EQ = 5;
	public static final int NE = 6;
	public static final int GT = 7;
	public static final int GE = 8;
	public static final int LT = 9;
	public static final int LE = 10;
	public static final int PLUS = 11;
	public static final int MINUS = 12;
	public static final int TIMES = 13;
	public static final int DIVIDE = 14;
	// Operator symbols
	public static final String opSymbols[] = { "", "=>", "<=>", "|", "&", "=", "!=", ">", ">=", "<", "<=", "+", "-", "*", "/" };
	// Operator type testers
	public static boolean isLogical(int op) { return op==IMPLIES || op==IFF || op==OR || op==AND; }
	public static boolean isRelOp(int op) { return op==EQ || op==NE || op==GT ||  op==GE || op==LT || op==LE; }
	public static boolean isArithmetic(int op) { return op==PLUS || op==MINUS || op==TIMES ||  op==DIVIDE; }
	
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
		throw new PrismLangException("Unknown binary operator '" + s + "'");
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

	@Override
	public boolean isConstant()
	{
		return operand1.isConstant() && operand2.isConstant();
	}

	@Override
	public boolean isProposition()
	{
		return operand1.isProposition() && operand2.isProposition();
	}

	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		switch (op) {
		case IMPLIES:
			return new Boolean(!operand1.evaluateBoolean(ec) || operand2.evaluateBoolean(ec));
		case IFF:
			return new Boolean(operand1.evaluateBoolean(ec) == operand2.evaluateBoolean(ec));
		case OR:
			return new Boolean(operand1.evaluateBoolean(ec) || operand2.evaluateBoolean(ec));
		case AND:
			return new Boolean(operand1.evaluateBoolean(ec) && operand2.evaluateBoolean(ec));
		case EQ:
			if (operand1.getType() == TypeInt.getInstance() && operand2.getType() == TypeInt.getInstance()) {
				return new Boolean(operand1.evaluateInt(ec) == operand2.evaluateInt(ec));
			} else {
				return new Boolean(operand1.evaluateDouble(ec) == operand2.evaluateDouble(ec));
			}
		case NE:
			if (operand1.getType() == TypeInt.getInstance() && operand2.getType() == TypeInt.getInstance()) {
				return new Boolean(operand1.evaluateInt(ec) != operand2.evaluateInt(ec));
			} else {
				return new Boolean(operand1.evaluateDouble(ec) != operand2.evaluateDouble(ec));
			}
		case GT:
			if (operand1.getType() == TypeInt.getInstance() && operand2.getType() == TypeInt.getInstance()) {
				return new Boolean(operand1.evaluateInt(ec) > operand2.evaluateInt(ec));
			} else {
				return new Boolean(operand1.evaluateDouble(ec) > operand2.evaluateDouble(ec));
			}
		case GE:
			if (operand1.getType() == TypeInt.getInstance() && operand2.getType() == TypeInt.getInstance()) {
				return new Boolean(operand1.evaluateInt(ec) >= operand2.evaluateInt(ec));
			} else {
				return new Boolean(operand1.evaluateDouble(ec) >= operand2.evaluateDouble(ec));
			}
		case LT:
			if (operand1.getType() == TypeInt.getInstance() && operand2.getType() == TypeInt.getInstance()) {
				return new Boolean(operand1.evaluateInt(ec) < operand2.evaluateInt(ec));
			} else {
				return new Boolean(operand1.evaluateDouble(ec) < operand2.evaluateDouble(ec));
			}
		case LE:
			if (operand1.getType() == TypeInt.getInstance() && operand2.getType() == TypeInt.getInstance()) {
				return new Boolean(operand1.evaluateInt(ec) <= operand2.evaluateInt(ec));
			} else {
				return new Boolean(operand1.evaluateDouble(ec) <= operand2.evaluateDouble(ec));
			}
		case PLUS:
			if (operand1.getType() == TypeInt.getInstance() && operand2.getType() == TypeInt.getInstance()) {
				return new Integer(operand1.evaluateInt(ec) + operand2.evaluateInt(ec));
			} else {
				return new Double(operand1.evaluateDouble(ec) + operand2.evaluateDouble(ec));
			}
		case MINUS:
			if (operand1.getType() == TypeInt.getInstance() && operand2.getType() == TypeInt.getInstance()) {
				return new Integer(operand1.evaluateInt(ec) - operand2.evaluateInt(ec));
			} else {
				return new Double(operand1.evaluateDouble(ec) - operand2.evaluateDouble(ec));
			}
		case TIMES:
			if (operand1.getType() == TypeInt.getInstance() && operand2.getType() == TypeInt.getInstance()) {
				return new Integer(operand1.evaluateInt(ec) * operand2.evaluateInt(ec));
			} else {
				return new Double(operand1.evaluateDouble(ec) * operand2.evaluateDouble(ec));
			}
		case DIVIDE:
			return new Double(operand1.evaluateDouble(ec) / operand2.evaluateDouble(ec));
		}
		throw new PrismLangException("Unknown binary operator", this);
	}
	
	@Override
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException
	{
		BigRational v1 = operand1.evaluateExact(ec);
		BigRational v2 = operand2.evaluateExact(ec);

		switch (op) {
		case IMPLIES:
			return BigRational.from(!v1.toBoolean() || v2.toBoolean());
		case IFF:
			return BigRational.from(v1.toBoolean() == v2.toBoolean());
		case OR:
			return BigRational.from(v1.toBoolean() || v2.toBoolean());
		case AND:
			return BigRational.from(v1.toBoolean() && v2.toBoolean());
		case EQ:
			return BigRational.from(v1.equals(v2));
		case NE:
			return BigRational.from(!v1.equals(v2));
		case GT:
			return BigRational.from(v1.compareTo(v2) > 0);
		case GE:
			return BigRational.from(v1.equals(v2) || v1.compareTo(v2) > 0);
		case LT:
			return BigRational.from(v1.compareTo(v2) < 0);
		case LE:
			return BigRational.from(v1.equals(v2) || v1.compareTo(v2) < 0);
		case PLUS:
			return v1.add(v2);
		case MINUS:
			return v1.subtract(v2);
		case TIMES:
			return v1.multiply(v2);
		case DIVIDE:
			return v1.divide(v2);
		}
		throw new PrismLangException("Unknown binary operator", this);
	}


	@Override
	public boolean returnsSingleValue()
	{
		return operand1.returnsSingleValue() && operand2.returnsSingleValue();
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
		ExpressionBinaryOp expr = new ExpressionBinaryOp(op, operand1.deepCopy(), operand2.deepCopy());
		expr.setType(type);
		expr.setPosition(this);
		return expr;
	}

	// Standard methods
	
	@Override
	public String toString()
	{
		return operand1 + opSymbols[op] + operand2;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + op;
		result = prime * result + ((operand1 == null) ? 0 : operand1.hashCode());
		result = prime * result + ((operand2 == null) ? 0 : operand2.hashCode());
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
		ExpressionBinaryOp other = (ExpressionBinaryOp) obj;
		if (op != other.op)
			return false;
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
		return true;
	}
}

// ------------------------------------------------------------------------------
