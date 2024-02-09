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

import common.SafeCast;
import param.BigRational;
import parser.EvaluateContext;
import parser.EvaluateContext.EvalMode;
import parser.type.Type;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import parser.visitor.ASTVisitor;
import parser.visitor.DeepCopy;
import prism.PrismLangException;
import prism.PrismUtils;

import java.math.BigInteger;
import java.util.ArrayList;

public class ExpressionFunc extends Expression
{
	// Built-in function name constants
	public static final int MIN = 0;
	public static final int MAX = 1;
	public static final int FLOOR = 2;
	public static final int CEIL = 3;
	public static final int ROUND = 4;
	public static final int POW = 5;
	public static final int MOD = 6;
	public static final int LOG = 7;
	public static final int MULTI = 8;
	// Built-in function names
	public static final String names[] = { "min", "max", "floor", "ceil", "round", "pow", "mod", "log", "multi"};
	// Min/max function arities
	public static final int minArities[] = { 1, 1, 1, 1, 1, 2, 2, 2, 1 };
	public static final int maxArities[] = { -1, -1, 1, 1, 1, 2, 2, 2, -1 };

	// Function name
	private String name = "";
	private int code = -1;
	// Operands
	private ArrayList<Expression> operands;
	// Was function written in old style notation (using "func" keyword)?
	private boolean oldStyle = false;

	// Constructors

	public ExpressionFunc()
	{
		operands = new ArrayList<Expression>();
	}

	public ExpressionFunc(String name)
	{
		setName(name);
		operands = new ArrayList<Expression>();
	}

	// Set methods

	public void setName(String s)
	{
		int i, n;
		// Set string
		name = s;
		// Determine and set code
		n = names.length;
		code = -1;
		for (i = 0; i < n; i++) {
			if (s.equals(names[i])) {
				code = i;
				break;
			}
		}
	}

	public void addOperand(Expression e)
	{
		operands.add(e);
	}

	public void setOperand(int i, Expression e)
	{
		operands.set(i, e);
	}

	public void setOldStyle(boolean b)
	{
		oldStyle = b;
	}

	// Get methods

	public String getName()
	{
		return name;
	}

	public int getNameCode()
	{
		return code;
	}

	public int getNumOperands()
	{
		return operands.size();
	}

	public Expression getOperand(int i)
	{
		return operands.get(i);
	}

	public boolean getOldStyle()
	{
		return oldStyle;
	}

	public int getMinArity()
	{
		return code == -1 ? Integer.MAX_VALUE : minArities[code];
	}

	public int getMaxArity()
	{
		return code == -1 ? -1 : maxArities[code];
	}

	// Methods required for Expression:

	@Override
	public boolean isConstant()
	{
		int i, n;
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			if (!getOperand(i).isConstant())
				return false;
		}
		return true;
	}

	@Override
	public boolean isProposition()
	{
		int i, n;
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			if (!getOperand(i).isProposition())
				return false;
		}
		return true;
	}
	
	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		try {
			int n = getNumOperands();
			Object[] eval = new Object[n];
			for (int i = 0; i < n; i++) {
				eval[i] = getOperand(i).evaluate(ec);
			}
			return apply(eval, ec.getEvaluationMode());
		} catch (PrismLangException e) {
			e.setASTElement(this);
			throw e;
		}
	}

	/**
	 * Apply this function instance to the arguments provided.
	 * The arguments are assumed to be the correct kinds of Objects for their type
	 * (as returned by {@link Type#castValueTo(Object, EvalMode)}).
	 */
	public Object apply(Object[] eval, EvalMode evalMode) throws PrismLangException
	{
		switch (code) {
		case MIN:
			return applyMin(eval, evalMode);
		case MAX:
			return applyMax(eval, evalMode);
		case FLOOR:
			return applyFloor(eval[0], evalMode);
		case CEIL:
			return applyCeil(eval[0], evalMode);
		case ROUND:
			return applyRound(eval[0], evalMode);
		case POW:
			return applyPow(eval[0], eval[1], evalMode);
		case MOD:
			return applyMod(eval[0], eval[1], evalMode);
		case LOG:
			return applyLog(eval[0], eval[1], evalMode);
		}
		throw new PrismLangException("Unknown function \"" + name + "\"", this);
	}
	
	/**
	 * Apply this (unary) function instance to the argument provided.
	 * The arguments are assumed to be the correct kinds of Objects for their type
	 * (as returned by {@link Type#castValueTo(Object, EvalMode)}).
	 */
	public Object applyUnary(Object eval, EvalMode evalMode) throws PrismLangException
	{
		switch (code) {
		case FLOOR:
			return applyFloor(eval, evalMode);
		case CEIL:
			return applyCeil(eval, evalMode);
		case ROUND:
			return applyRound(eval, evalMode);
		}
		throw new PrismLangException("Unknown unary function \"" + name + "\"", this);
	}
	
	/**
	 * Apply this (binary, or n-ary) function instance to the argument provided.
	 * The arguments are assumed to be the correct kinds of Objects for their type
	 * (as returned by {@link Type#castValueTo(Object, EvalMode)}).
	 */
	public Object applyBinary(Object eval1, Object eval2, EvalMode evalMode) throws PrismLangException
	{
		switch (code) {
		case MIN:
			return applyMinBinary(eval1, eval2, evalMode);
		case MAX:
			return applyMaxBinary(eval1, eval2, evalMode);
		case POW:
			return applyPow(eval1, eval2, evalMode);
		case MOD:
			return applyMod(eval1, eval2, evalMode);
		case LOG:
			return applyLog(eval1, eval2, evalMode);
		}
		throw new PrismLangException("Unknown binary function \"" + name + "\"", this);
	}
	
	/**
	 * Apply this (min) function instance to the arguments provided.
	 * The arguments are assumed to be the correct kinds of Objects for their type
	 * (as returned by {@link Type#castValueTo(Object, EvalMode)}).
	 */
	private Object applyMin(Object[] eval, EvalMode evalMode) throws PrismLangException
	{
		int n = eval.length;
		// All arguments ints
		if (getType() instanceof TypeInt) {
			switch (evalMode) {
			case FP:
				int iMin = (int) eval[0];
				for (int i = 1; i < n; i++) {
					iMin = Math.min(iMin, (int) eval[i]);
				}
				return iMin;
			case EXACT:
				BigInteger biMin = (BigInteger) eval[0];
				for (int i = 1; i < n; i++) {
					biMin = biMin.min((BigInteger) eval[i]);
				}
				return biMin;
			default:
				throw new PrismLangException("Unknown evaluation mode " + evalMode);
			}
		}
		// One or more arguments doubles - convert all to doubles
		else {
			switch (evalMode) {
			case FP:
				double dMin = (double) TypeDouble.getInstance().castValueTo(eval[0], evalMode);
				for (int i = 1; i < n; i++) {
					dMin = Math.min(dMin, (double) TypeDouble.getInstance().castValueTo(eval[i], evalMode));
				}
				return dMin;
			case EXACT:
				BigRational brMin = (BigRational) TypeDouble.getInstance().castValueTo(eval[0], evalMode);
				for (int i = 1; i < n; i++) {
					brMin = brMin.min((BigRational) TypeDouble.getInstance().castValueTo(eval[i], evalMode));
				}
				return brMin;
			default:
				throw new PrismLangException("Unknown evaluation mode " + evalMode);
			}
		}
	}

	/**
	 * Apply this (min) function instance to the argument provided.
	 * The argument is assumed to be the correct kind of Object for its type
	 * (as returned by {@link Type#castValueTo(Object, EvalMode)}).
	 */
	private Object applyMinBinary(Object eval1, Object eval2, EvalMode evalMode) throws PrismLangException
	{
		// All arguments ints
		if (getType() instanceof TypeInt) {
			switch (evalMode) {
			case FP:
				return Math.min((int) eval1, (int) eval2);
			case EXACT:
				return ((BigInteger) eval1).min((BigInteger) eval2);
			default:
				throw new PrismLangException("Unknown evaluation mode " + evalMode);
			}
		}
		// One or more arguments doubles - convert all to doubles
		else {
			Object eval1D = TypeDouble.getInstance().castValueTo(eval1, evalMode);
			Object eval2D = TypeDouble.getInstance().castValueTo(eval2, evalMode);
			switch (evalMode) {
			case FP:
				return Math.min((double) eval1D, (double) eval2D);
			case EXACT:
				return ((BigRational) eval1D).min((BigRational) eval2D);
			default:
				throw new PrismLangException("Unknown evaluation mode " + evalMode);
			}
		}
	}

	/**
	 * Apply this (max) function instance to the arguments provided.
	 * The arguments are assumed to be the correct kinds of Objects for their type
	 * (as returned by {@link Type#castValueTo(Object, EvalMode)}).
	 */
	private Object applyMax(Object[] eval, EvalMode evalMode) throws PrismLangException
	{
		int n = eval.length;
		// All arguments ints
		if (getType() instanceof TypeInt) {
			switch (evalMode) {
			case FP:
				int iMax = (int) eval[0];
				for (int i = 1; i < n; i++) {
					iMax = Math.max(iMax, (int) eval[i]);
				}
				return iMax;
			case EXACT:
				BigInteger biMax = (BigInteger) eval[0];
				for (int i = 1; i < n; i++) {
					biMax = biMax.max((BigInteger) eval[i]);
				}
				return biMax;
			default:
				throw new PrismLangException("Unknown evaluation mode " + evalMode);
			}
		}
		// One or more arguments doubles - convert all to doubles
		else {
			switch (evalMode) {
			case FP:
				double dMax = (double) TypeDouble.getInstance().castValueTo(eval[0], evalMode);
				for (int i = 1; i < n; i++) {
					dMax = Math.max(dMax, (double) TypeDouble.getInstance().castValueTo(eval[i], evalMode));
				}
				return dMax;
			case EXACT:
				BigRational brMax = (BigRational) TypeDouble.getInstance().castValueTo(eval[0], evalMode);
				for (int i = 1; i < n; i++) {
					brMax = brMax.max((BigRational) TypeDouble.getInstance().castValueTo(eval[i], evalMode));
				}
				return brMax;
			default:
				throw new PrismLangException("Unknown evaluation mode " + evalMode);
			}
		}
	}

	/**
	 * Apply this (max) function instance to the argument provided.
	 * The argument is assumed to be the correct kind of Object for its type
	 * (as returned by {@link Type#castValueTo(Object, EvalMode)}).
	 */
	private Object applyMaxBinary(Object eval1, Object eval2, EvalMode evalMode) throws PrismLangException
	{
		// All arguments ints
		if (getType() instanceof TypeInt) {
			switch (evalMode) {
			case FP:
				return Math.max((int) eval1, (int) eval2);
			case EXACT:
				return ((BigInteger) eval1).max((BigInteger) eval2);
			default:
				throw new PrismLangException("Unknown evaluation mode " + evalMode);
			}
		}
		// One or more arguments doubles - convert all to doubles
		else {
			Object eval1D = TypeDouble.getInstance().castValueTo(eval1, evalMode);
			Object eval2D = TypeDouble.getInstance().castValueTo(eval2, evalMode);
			switch (evalMode) {
			case FP:
				return Math.max((double) eval1D, (double) eval2D);
			case EXACT:
				return ((BigRational) eval1D).max((BigRational) eval2D);
			default:
				throw new PrismLangException("Unknown evaluation mode " + evalMode);
			}
		}
	}

	/**
	 * Apply this (floor) function instance to the argument provided.
	 * The argument is assumed to be the correct kind of Object for its type
	 * (as returned by {@link Type#castValueTo(Object, EvalMode)}).
	 */
	private Object applyFloor(Object eval, EvalMode evalMode) throws PrismLangException
	{
		try {
			// Double argument so may need to cast to double first
			Object evalD = TypeDouble.getInstance().castValueTo(eval, evalMode);
			switch (evalMode) {
			case FP:
				return SafeCast.toIntExact(Math.floor((double) evalD));
			case EXACT:
				return ((BigRational) evalD).floor().bigIntegerValue();
			default:
				throw new PrismLangException("Unknown evaluation mode " + evalMode);
			}
		} catch (ArithmeticException e) {
			throw new PrismLangException("Error evaluating " + getName() + ":" + e.getMessage(), this);
		}
	}

	/**
	 * Apply this (ceil) function instance to the argument provided.
	 * The argument is assumed to be the correct kind of Object for its type
	 * (as returned by {@link Type#castValueTo(Object, EvalMode)}).
	 */
	private Object applyCeil(Object eval, EvalMode evalMode) throws PrismLangException
	{
		try {
			// Double argument so may need to cast to double first
			Object evalD = TypeDouble.getInstance().castValueTo(eval, evalMode);
			switch (evalMode) {
			case FP:
				return SafeCast.toIntExact(Math.ceil((double) evalD));
			case EXACT:
				return ((BigRational) evalD).ceil().bigIntegerValue();
			default:
				throw new PrismLangException("Unknown evaluation mode " + evalMode);
			}
		} catch (ArithmeticException e) {
			throw new PrismLangException("Error evaluating " + getName() + ":" + e.getMessage(), this);
		}
	}

	/**
	 * Apply this (round) function instance to the argument provided.
	 * The argument is assumed to be the correct kind of Object for its type
	 * (as returned by {@link Type#castValueTo(Object, EvalMode)}).
	 */
	private Object applyRound(Object eval, EvalMode evalMode) throws PrismLangException
	{
		try {
			// Double argument so may need to cast to double first
			Object evalD = TypeDouble.getInstance().castValueTo(eval, evalMode);
			switch (evalMode) {
			case FP:
				return SafeCast.toIntExact(Math.round((double) evalD));
			case EXACT:
				return ((BigRational) evalD).round().bigIntegerValue();
			default:
				throw new PrismLangException("Unknown evaluation mode " + evalMode);
			}
		} catch (ArithmeticException e) {
			throw new PrismLangException("Error evaluating " + getName() + ":" + e.getMessage(), this);
		}
	}

	/**
	 * Apply this (pow) function instance to the arguments provided
	 * The arguments are assumed to be the correct kinds of Objects for their type
	 * (as returned by {@link Type#castValueTo(Object, EvalMode)}).
	 */
	private Object applyPow(Object eval1, Object eval2, EvalMode evalMode) throws PrismLangException
	{
		try {
			// The apply code is in a separate static method for re-use elsewhere
			return applyPow(getType(), eval1, eval2, evalMode);
		} catch (PrismLangException e) {
			throw new PrismLangException(e.getMessage(), this);
		}
	}

	/**
	 * Apply a (pow) function instance of the specified type to the arguments provided
	 * The arguments are assumed to be the correct kinds of Objects for their type
	 * (as returned by {@link Type#castValueTo(Object, EvalMode)}).
	 */
	public static Object applyPow(Type type, Object eval1, Object eval2, EvalMode evalMode) throws PrismLangException
	{
		// All arguments ints
		if (type instanceof TypeInt) {
			switch (evalMode) {
			case FP:
				int iBase = (int) eval1;
				int iExp = (int) eval2;
				// Not allowed to do e.g. pow(2,-2) because of typing (should be pow(2.0,-2) instead)
				if (iExp < 0)
					throw new PrismLangException("Negative exponent not allowed for integer power");
				try {
					return SafeCast.toIntExact(Math.pow(iBase, iExp));
				} catch (ArithmeticException e) {
					throw new PrismLangException("Overflow evaluating integer power: " + e.getMessage());
				}
			case EXACT:
				BigInteger biBase = (BigInteger) eval1;
				BigInteger biExp = (BigInteger) eval2;
				// Not allowed to do e.g. pow(2,-2) because of typing (should be pow(2.0,-2) instead)
				if (biExp.compareTo(BigInteger.ZERO) < 0)
					throw new PrismLangException("Negative exponent not allowed for integer power");
				try {
					return biBase.pow(biExp.intValue());
				} catch (ArithmeticException e) {
					throw new PrismLangException("Cannot compute pow exactly, as there is a problem with the exponent: " + e.getMessage());
				}
			default:
				throw new PrismLangException("Unknown evaluation mode " + evalMode);
			}
		}
		// One or more arguments doubles - convert all to doubles
		else {
			Object base = TypeDouble.getInstance().castValueTo(eval1);
			Object exp = TypeDouble.getInstance().castValueTo(eval2);
			switch (evalMode) {
			case FP:
				return Math.pow((double) base, (double) exp);
			case EXACT:
				if (((BigRational) exp).isInteger()) {
					return ((BigRational) base).pow(((BigRational) exp).toInt());
				} else {
					throw new PrismLangException("Cannot compute fractional powers exactly");
				}
			default:
				throw new PrismLangException("Unknown evaluation mode " + evalMode);
			}
		}
	}

	/**
	 * Apply this (mod) function instance to the arguments provided.
	 * The arguments are assumed to be the correct kinds of Objects for their type
	 * (as returned by {@link Type#castValueTo(Object, EvalMode)}).
	 */
	private Object applyMod(Object eval1, Object eval2, EvalMode evalMode) throws PrismLangException
	{
		// Both arguments are integers
		switch (evalMode) {
		case FP:
			int i1 = (int) eval1;
			int i2 = (int) eval2;
			// Non-positive divisor not allowed 
			if (i2 <= 0) {
				throw new PrismLangException("Attempt to compute modulo with non-positive divisor", this);
			}
			// Take care of negative case (% is remainder, not modulo)
			int rem = i1 % i2;
			return (rem < 0) ? rem + i2 : rem;
		case EXACT:
			BigInteger bi1 = (BigInteger) eval1;
			BigInteger bi2 = (BigInteger) eval2;
			// Non-positive divisor not allowed 
			if (bi2.compareTo(BigInteger.ZERO) <= 0) {
				throw new PrismLangException("Attempt to compute modulo with non-positive divisor");
			}
			return bi1.mod(bi2);
		default:
			throw new PrismLangException("Unknown evaluation mode " + evalMode);
		}
	}

	/**
	 * Apply this (log) function instance to the arguments provided.
	 * The arguments are assumed to be the correct kinds of Objects for their type
	 * (as returned by {@link Type#castValueTo(Object, EvalMode)}).
	 */
	private Object applyLog(Object eval1, Object eval2, EvalMode evalMode) throws PrismLangException
	{
		// Double arguments so may need to cast to double first
		Object x = TypeDouble.getInstance().castValueTo(eval1, evalMode);
		Object b = TypeDouble.getInstance().castValueTo(eval2, evalMode);
		switch (evalMode) {
		case FP:
			// Type will be double; so evaluate both operands and cast to doubles
			return PrismUtils.log((double) x, (double) b);
		case EXACT:
			throw new PrismLangException("Currently, cannot compute log exactly", this);
		default:
			throw new PrismLangException("Unknown evaluation mode " + evalMode);
		}
	}

	@Override
	public boolean returnsSingleValue()
	{
		int i, n;
		// Otherwise, true iff all operands true
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			if (!getOperand(i).returnsSingleValue())
				return false;
		}
		return true;
	}

	// Methods required for ASTElement:

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public ExpressionFunc deepCopy(DeepCopy copier) throws PrismLangException
	{
		copier.copyAll(operands);

		return this;
	}


	@SuppressWarnings("unchecked")
	@Override
	public ExpressionFunc clone()
	{
		ExpressionFunc clone = (ExpressionFunc) super.clone();

		clone.operands = (ArrayList<Expression>) operands.clone();

		return clone;
	}
	
	// Standard methods
	
	@Override
	public String toString()
	{
		int i, n;
		String s = "";
		boolean first = true;

		if (!oldStyle)
			s += name + "(";
		else
			s += "func(" + name + ", ";
		n = operands.size();
		for (i = 0; i < n; i++) {
			if (!first)
				s += ", ";
			else
				first = false;
			s = s + getOperand(i);
		}
		s += ")";

		return s;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + code;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (oldStyle ? 1231 : 1237);
		result = prime * result + ((operands == null) ? 0 : operands.hashCode());
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
		ExpressionFunc other = (ExpressionFunc) obj;
		if (code != other.code)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (oldStyle != other.oldStyle)
			return false;
		if (operands == null) {
			if (other.operands != null)
				return false;
		} else if (!operands.equals(other.operands))
			return false;
		return true;
	}
}

// ------------------------------------------------------------------------------
