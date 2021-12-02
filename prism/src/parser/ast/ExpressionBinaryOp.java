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
import parser.type.TypeDouble;
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
		Object eval1 = operand1.evaluate(ec);
		Object eval2 = operand2.evaluate(ec);
		return apply(eval1, eval2);
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

	/**
	 * Apply this binary operator instance to the arguments provided
	 */
	public Object apply(Object eval1, Object eval2) throws PrismLangException
	{
		switch (op) {

		// Boolean operators
		case IMPLIES:
			return !((Boolean) getType().castValueTo(eval1)) || ((Boolean) getType().castValueTo(eval2));
		case IFF:
			return ((Boolean) getType().castValueTo(eval1)) == ((Boolean) getType().castValueTo(eval2));
		case OR:
			return ((Boolean) getType().castValueTo(eval1)) || ((Boolean) getType().castValueTo(eval2));
		case AND:
			return ((Boolean) getType().castValueTo(eval1)) && ((Boolean) getType().castValueTo(eval2));

		// (In)equality
		case EQ:
		case NE:
			// Cast arguments to the same type if needed,
			// before testing using equals() on the resulting Objects
			if (!operand1.getType().equals(operand2.getType())) {
				if (operand1.getType().canAssign(operand2.getType())) {
					eval1 = operand1.getType().castValueTo(eval1);
					eval2 = operand1.getType().castValueTo(eval2);
				} else if (operand2.getType().canAssign(operand1.getType())) {
					eval1 = operand2.getType().castValueTo(eval1);
					eval2 = operand2.getType().castValueTo(eval2);
				} else {
					throw new PrismLangException("Cannot apply " + getOperatorSymbol() + " to " + operand1.getType() + " and " + operand2.getType(), this);
				}
			}
			return op == EQ ? eval1.equals(eval2) : !eval1.equals(eval2);

		// Division (always evaluates to a double)
		case DIVIDE:
			// Type will always be double; so cast both to doubles
			eval1 = getType().castValueTo(eval1);
			eval2 = getType().castValueTo(eval2);
			return ((Double) eval1) / ((Double) eval2);

		// Other numerical (relations/arithmetic) - mix of doubles/ints
		default:
			try {
				// Two ints
				if (operand1.getType() == TypeInt.getInstance() && operand2.getType() == TypeInt.getInstance()) {
					int i1 = (Integer) TypeInt.getInstance().castValueTo(eval1);
					int i2 = (Integer) TypeInt.getInstance().castValueTo(eval2);
					switch (op) {
					case GT:
						return i1 > i2;
					case GE:
						return i1 >= i2;
					case LT:
						return i1 < i2;
					case LE:
						return i1 <= i2;
					case PLUS:
						return Math.addExact(i1, i2);
					case MINUS:
						return Math.subtractExact(i1, i2);
					case TIMES:
						return Math.multiplyExact(i1, i2);
					}
				}
				// Two doubles or one double + one int: cast both to doubles
				else {
					double d1 = (double) TypeDouble.getInstance().castValueTo(eval1);
					double d2 = (double) TypeDouble.getInstance().castValueTo(eval2);
					switch (op) {
					case GT:
						return d1 > d2;
					case GE:
						return d1 >= d2;
					case LT:
						return d1 < d2;
					case LE:
						return d1 <= d2;
					case PLUS:
						return d1 + d2;
					case MINUS:
						return d1 - d2;
					case TIMES:
						return d1 * d2;
					}
				}
			} catch (ArithmeticException e) {
				throw new PrismLangException(e.getMessage(), this);
			}
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
