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
import parser.EvaluateContext;
import parser.EvaluateContext.EvalMode;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import parser.visitor.ASTVisitor;
import parser.visitor.DeepCopy;
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
	public static final int POW = 15;
	// Operator symbols
	public static final String opSymbols[] = { "", "=>", "<=>", "|", "&", "=", "!=", ">", ">=", "<", "<=", "+", "-", "*", "/", "^" };
	// Operator type testers
	public static boolean isLogical(int op) { return op==IMPLIES || op==IFF || op==OR || op==AND; }
	public static boolean isRelOp(int op) { return op==EQ || op==NE || op==GT ||  op==GE || op==LT || op==LE; }
	public static boolean isArithmetic(int op) { return op==PLUS || op==MINUS || op==TIMES || op==DIVIDE || op== POW; }
	
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
		switch (op) {
			// Short-circuit evaluation
			case IMPLIES:
				return !((boolean) eval1) || ((boolean) operand2.evaluate(ec));
			case OR:
				return ((boolean) eval1) || ((boolean) operand2.evaluate(ec));
			case AND:
				return ((boolean) eval1) && ((boolean) operand2.evaluate(ec));
			// No short-circuit evaluation
			default:
				Object eval2 = operand2.evaluate(ec);
				return apply(eval1, eval2, ec.getEvaluationMode());
		}
	}
	
	/**
	 * Apply this binary operator instance to the arguments provided
	 */
	public Object apply(Object eval1, Object eval2, EvalMode evalMode) throws PrismLangException
	{
		switch (op) {

		// Boolean operators (any eval mode)
		case IMPLIES:
			return !((boolean) eval1) || ((boolean) eval2);
		case IFF:
			return ((boolean) eval1) == ((boolean) eval2);
		case OR:
			return ((boolean) eval1) || ((boolean) eval2);
		case AND:
			return ((boolean) eval1) && ((boolean) eval2);

		// (In)equality (any eval mode)
		case EQ:
		case NE:
			// Cast arguments to the same type if needed,
			// before testing using equals() on the resulting Objects
			if (!operand1.getType().equals(operand2.getType())) {
				if (operand1.getType().canCastTypeTo(operand2.getType())) {
					eval1 = operand1.getType().castValueTo(eval1);
					eval2 = operand1.getType().castValueTo(eval2);
				} else if (operand2.getType().canCastTypeTo(operand1.getType())) {
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
			Object eval1D = TypeDouble.getInstance().castValueTo(eval1, evalMode);
			Object eval2D = TypeDouble.getInstance().castValueTo(eval2, evalMode);
			switch (evalMode) {
			case FP:
				return ((double) eval1D) / ((double) eval2D);
			case EXACT:
				return ((BigRational) eval1D).divide(((BigRational) eval2D));
			default:
				throw new PrismLangException("Unknown evaluation mode " + evalMode);
			}

		// Division (reuse code for pow())
		case POW:
			return ExpressionFunc.applyPow(getType(), eval1, eval2, evalMode);

		// Other numerical (relations/arithmetic) - mix of doubles/ints
		default:
			try {
				// Two ints
				if (operand1.getType() == TypeInt.getInstance() && operand2.getType() == TypeInt.getInstance()) {
					switch (evalMode) {
					case FP:
						int i1 = (int) eval1;
						int i2 = (int) eval2;
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
					case EXACT:
						BigInteger bi1 = (BigInteger) eval1;
						BigInteger bi2 = (BigInteger) eval2;
						switch (op) {
						case GT:
							return bi1.compareTo(bi2) > 0;
						case GE:
							return bi1.compareTo(bi2) >= 0;
						case LT:
							return bi1.compareTo(bi2) < 0;
						case LE:
							return bi1.compareTo(bi2) <= 0;
						case PLUS:
							return bi1.add(bi2);
						case MINUS:
							return bi1.subtract(bi2);
						case TIMES:
							return bi1.multiply(bi2);
						}
					default:
						throw new PrismLangException("Unknown evaluation mode " + evalMode);
					}
				}
				// One or more arguments doubles - convert all to doubles
				else {
					eval1D = TypeDouble.getInstance().castValueTo(eval1, evalMode);
					eval2D = TypeDouble.getInstance().castValueTo(eval2, evalMode);
					switch (evalMode) {
					case FP:
						double d1 = (double) TypeDouble.getInstance().castValueTo(eval1, EvalMode.FP);
						double d2 = (double) TypeDouble.getInstance().castValueTo(eval2, EvalMode.FP);
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
						default:
							throw new PrismLangException("Unknown binary operator", this);
						}
					case EXACT:
						BigRational br1 = (BigRational) TypeDouble.getInstance().castValueTo(eval1, EvalMode.EXACT);
						BigRational br2 = (BigRational) TypeDouble.getInstance().castValueTo(eval2, EvalMode.EXACT);
						switch (op) {
						case GT:
							return br1.compareTo(br2) > 0;
						case GE:
							return br1.compareTo(br2) >= 0;
						case LT:
							return br1.compareTo(br2) < 0;
						case LE:
							return br1.compareTo(br2) <= 0;
						case PLUS:
							return br1.add(br2);
						case MINUS:
							return br1.subtract(br2);
						case TIMES:
							return br1.multiply(br2);
						default:
							throw new PrismLangException("Unknown binary operator", this);
						}
					default:
						throw new PrismLangException("Unknown evaluation mode " + evalMode);
					}
				}
			} catch (ArithmeticException e) {
				throw new PrismLangException(e.getMessage(), this);
			}
		}
	}
	
	@Override
	public boolean returnsSingleValue()
	{
		return operand1.returnsSingleValue() && operand2.returnsSingleValue();
	}

	@Override
	public Precedence getPrecedence()
	{
		switch (op) {
			case IMPLIES:
				return Precedence.IMPLIES;
			case IFF:
				return Precedence.IFF;
			case OR:
				return Precedence.OR;
			case AND:
				return Precedence.AND;
			case EQ:
			case NE:
				return Precedence.EQUALITY;
			case GT:
			case GE:
			case LT:
			case LE:
				return Precedence.RELOP;
			case PLUS:
			case MINUS:
				return Precedence.PLUS_MINUS;
			case TIMES:
			case DIVIDE:
				return Precedence.TIMES_DIVIDE;
			case POW:
				return Precedence.POW;
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
	public ExpressionBinaryOp deepCopy(DeepCopy copier) throws PrismLangException
	{
		operand1 = copier.copy(operand1);
		operand2 = copier.copy(operand2);

		return this;
	}

	@Override
	public ExpressionBinaryOp clone()
	{
		return (ExpressionBinaryOp) super.clone();
	}

	// Standard methods
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		if (op == IMPLIES || op == EQ || op == NE) {
			// => is a (right-associative) non-commutative binary operator
			// Don't treat = and != as associative since types may vary
			builder.append(Expression.toStringPrecLeq(operand1, this));
		} else {
			// Others are commutative (or left-associative)
			builder.append(Expression.toStringPrecLt(operand1, this));
		}
		builder.append(opSymbols[op]);
		if (op == MINUS || op == DIVIDE || op == EQ || op == NE) {
			// - and / are (left-associative) non-commutative binary operators
			// Don't treat = and != as associative since types may vary
			builder.append(Expression.toStringPrecLeq(operand2, this));
		} else {
			// Others are commutative (or right-associative)
			builder.append(Expression.toStringPrecLt(operand2, this));
		}
		return builder.toString();
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
