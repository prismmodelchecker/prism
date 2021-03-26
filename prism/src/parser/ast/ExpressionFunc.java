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

import java.util.ArrayList;

import common.SafeCast;
import param.BigRational;
import parser.*;
import parser.visitor.*;
import prism.PrismLangException;
import prism.PrismUtils;
import parser.type.*;

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
	public static final int minArities[] = { 2, 2, 1, 1, 1, 2, 2, 2, 1 };
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
		int n = getNumOperands();
		Object[] eval = new Object[n];
		for (int i = 0; i < n; i++) {
			eval[i] = getOperand(i).evaluate(ec);
		}
		return apply(eval);
	}

	@Override
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException
	{
		switch (code) {
		case MIN:
			return evaluateMinExact(ec);
		case MAX:
			return evaluateMaxExact(ec);
		case FLOOR:
			return evaluateFloorExact(ec);
		case CEIL:
			return evaluateCeilExact(ec);
		case ROUND:
			return evaluateRoundExact(ec);
		case POW:
			return evaluatePowExact(ec);
		case MOD:
			return evaluateModExact(ec);
		case LOG:
			return evaluateLogExact(ec);
		}
		throw new PrismLangException("Unknown function \"" + name + "\"", this);
	}
	
	/**
	 * Apply this function instance to the arguments provided
	 */
	public Object apply(Object[] eval) throws PrismLangException
	{
		switch (code) {
		case MIN:
			return applyMin(eval);
		case MAX:
			return applyMax(eval);
		case FLOOR:
			return applyFloor(eval[0]);
		case CEIL:
			return applyCeil(eval[0]);
		case ROUND:
			return applyRound(eval[0]);
		case POW:
			return applyPow(eval[0], eval[1]);
		case MOD:
			return applyMod(eval[0], eval[1]);
		case LOG:
			return applyLog(eval[0], eval[1]);
		}
		throw new PrismLangException("Unknown function \"" + name + "\"", this);
	}
	
	/**
	 * Apply this (unary) function instance to the argument provided
	 */
	public Object applyUnary(Object eval) throws PrismLangException
	{
		switch (code) {
		case FLOOR:
			return applyFloor(eval);
		case CEIL:
			return applyCeil(eval);
		case ROUND:
			return applyRound(eval);
		}
		throw new PrismLangException("Unknown unary function \"" + name + "\"", this);
	}
	
	/**
	 * Apply this (binary, or n-ary) function instance to the argument provided
	 */
	public Object applyBinary(Object eval1, Object eval2) throws PrismLangException
	{
		switch (code) {
		case MIN:
			return applyMinBinary(eval1, eval2);
		case MAX:
			return applyMaxBinary(eval1, eval2);
		case POW:
			return applyPow(eval1, eval2);
		case MOD:
			return applyMod(eval1, eval2);
		case LOG:
			return applyLog(eval1, eval2);
		}
		throw new PrismLangException("Unknown binary function \"" + name + "\"", this);
	}
	
	/**
	 * Apply this (min) function instance to the arguments provided
	 */
	private Object applyMin(Object[] eval) throws PrismLangException
	{
		int n = eval.length;
		// All arguments ints
		if (getType() instanceof TypeInt) {
			int iMin = (int) TypeInt.getInstance().castValueTo(eval[0]);
			for (int i = 1; i < n; i++) {
				int j = (int) TypeInt.getInstance().castValueTo(eval[i]);
				iMin = (j < iMin) ? j : iMin;
			}
			return iMin;
		}
		// Arguments mix of ints and doubles - convert to doubles
		else {
			double dMin = (double) TypeDouble.getInstance().castValueTo(eval[0]);
			for (int i = 1; i < n; i++) {
				double d = (double) TypeDouble.getInstance().castValueTo(eval[i]);
				dMin = (d < dMin) ? d : dMin;
			}
			return dMin;
		}
	}

	/**
	 * Apply this (min) function instance to the arguments provided
	 */
	private Object applyMinBinary(Object eval1, Object eval2) throws PrismLangException
	{
		// All arguments ints
		if (getType() instanceof TypeInt) {
			int i1 = (int) TypeInt.getInstance().castValueTo(eval1);
			int i2 = (int) TypeInt.getInstance().castValueTo(eval2);
			return Math.min(i1, i2);
		}
		// Arguments mix of ints and doubles - convert to doubles
		else {
			double d1 = (double) TypeDouble.getInstance().castValueTo(eval1);
			double d2 = (double) TypeDouble.getInstance().castValueTo(eval2);
			return Math.min(d1, d2);
		}
	}

	private BigRational evaluateMinExact(EvaluateContext ec) throws PrismLangException
	{
		BigRational min;
	
		min = getOperand(0).evaluateExact(ec);
		for (int i = 1, n = getNumOperands(); i < n; i++) {
			min = min.min(getOperand(i).evaluateExact(ec));
		}
		return min;
	}

	/**
	 * Apply this (max) function instance to the arguments provided
	 */
	private Object applyMax(Object[] eval) throws PrismLangException
	{
		int n = eval.length;
		// All arguments ints
		if (getType() instanceof TypeInt) {
			int iMax = (int) TypeInt.getInstance().castValueTo(eval[0]);
			for (int i = 1; i < n; i++) {
				int j = (int) TypeInt.getInstance().castValueTo(eval[i]);
				iMax = (j > iMax) ? j : iMax;
			}
			return iMax;
		}
		// Arguments mix of ints and doubles - convert to doubles
		else {
			double dMax = (double) TypeDouble.getInstance().castValueTo(eval[0]);
			for (int i = 1; i < n; i++) {
				double d = (double) TypeDouble.getInstance().castValueTo(eval[i]);
				dMax = (d > dMax) ? d : dMax;
			}
			return dMax;
		}
	}

	/**
	 * Apply this (max) function instance to the arguments provided
	 */
	private Object applyMaxBinary(Object eval1, Object eval2) throws PrismLangException
	{
		// All arguments ints
		if (getType() instanceof TypeInt) {
			int i1 = (int) TypeInt.getInstance().castValueTo(eval1);
			int i2 = (int) TypeInt.getInstance().castValueTo(eval2);
			return Math.max(i1, i2);
		}
		// Arguments mix of ints and doubles - convert to doubles
		else {
			double d1 = (double) TypeDouble.getInstance().castValueTo(eval1);
			double d2 = (double) TypeDouble.getInstance().castValueTo(eval2);
			return Math.max(d1, d2);
		}
	}

	private BigRational evaluateMaxExact(EvaluateContext ec) throws PrismLangException
	{
		BigRational max;

		max = getOperand(0).evaluateExact(ec);
		for (int i = 1, n = getNumOperands(); i < n; i++) {
			max = max.max(getOperand(i).evaluateExact(ec));
		}
		return max;
	}

	/**
	 * Apply this (floor) function instance to the argument provided
	 */
	private Object applyFloor(Object eval) throws PrismLangException
	{
		try {
			return evaluateFloor((double) TypeDouble.getInstance().castValueTo(eval));
		} catch (PrismLangException e) {
			e.setASTElement(this);
			throw e;
		}
	}

	public static int evaluateFloor(double arg) throws PrismLangException
	{
		try {
			return SafeCast.toIntExact(Math.floor(arg));
		} catch (ArithmeticException e) {
			throw new PrismLangException("Cannot take floor() of " + arg + ": " + e.getMessage());
		}
	}

	public BigRational evaluateFloorExact(EvaluateContext ec) throws PrismLangException
	{
		return getOperand(0).evaluateExact(ec).floor();
	}

	/**
	 * Apply this (ceil) function instance to the argument provided
	 */
	private Object applyCeil(Object eval) throws PrismLangException
	{
		try {
			return evaluateCeil((double) TypeDouble.getInstance().castValueTo(eval));
		} catch (PrismLangException e) {
			e.setASTElement(this);
			throw e;
		}
	}

	public static int evaluateCeil(double arg) throws PrismLangException
	{
		try {
			return SafeCast.toIntExact(Math.ceil(arg));
		} catch (ArithmeticException e) {
			throw new PrismLangException("Cannot take ceil() of " + arg + ": " + e.getMessage());
		}
	}

	public BigRational evaluateCeilExact(EvaluateContext ec) throws PrismLangException
	{
		return getOperand(0).evaluateExact(ec).ceil();
	}

	/**
	 * Apply this (round) function instance to the argument provided
	 */
	private Integer applyRound(Object eval) throws PrismLangException
	{
		try {
			return evaluateRound((double) TypeDouble.getInstance().castValueTo(eval));
		} catch (PrismLangException e) {
			e.setASTElement(this);
			throw e;
		}
	}

	public static int evaluateRound(double arg) throws PrismLangException
	{
		try {
			return SafeCast.toIntExact(Math.round(arg));
		} catch (ArithmeticException e) {
			throw new PrismLangException("Cannot take round() of " + arg + ": " + e.getMessage());
		}
	}

	public BigRational evaluateRoundExact(EvaluateContext ec) throws PrismLangException
	{
		return getOperand(0).evaluateExact(ec).round();
	}

	/**
	 * Apply this (pow) function instance to the arguments provided
	 */
	private Object applyPow(Object eval1, Object eval2) throws PrismLangException
	{
		try {
			// All arguments ints
			if (getType() instanceof TypeInt) {
				int i1 = (int) TypeInt.getInstance().castValueTo(eval1);
				int i2 = (int) TypeInt.getInstance().castValueTo(eval2);
				return evaluatePowInt(i1, i2);
			}
			// Arguments mix of ints and doubles - convert to doubles
			else {
				double d1 = (double) TypeDouble.getInstance().castValueTo(eval1);
				double d2 = (double) TypeDouble.getInstance().castValueTo(eval2);
				return evaluatePowDouble(d1, d2);
			}
		} catch (PrismLangException e) {
			e.setASTElement(this);
			throw e;
		}
	}

	public static int evaluatePowInt(int base, int exp) throws PrismLangException
	{
		// Not allowed to do e.g. pow(2,-2) because of typing (should be pow(2.0,-2) instead)
		if (exp < 0)
			throw new PrismLangException("Negative exponent not allowed for integer power");
		try {
			return SafeCast.toIntExact(Math.pow(base, exp));
		} catch (ArithmeticException e) {
			throw new PrismLangException("Overflow evaluating integer power: " + e.getMessage());
		}
	}

	public static double evaluatePowDouble(double base, double exp) throws PrismLangException
	{
		return Math.pow(base, exp);
	}

	public BigRational evaluatePowExact(EvaluateContext ec) throws PrismLangException
	{
		BigRational base = getOperand(0).evaluateExact(ec);
		BigRational exp = getOperand(1).evaluateExact(ec);

		try {
			int expInt = exp.toInt();
			return base.pow(expInt);
		} catch (PrismLangException e) {
			throw new PrismLangException("Can not compute pow exactly, as there is a problem with the exponent: " + e.getMessage(), this);
		}
	}

	/**
	 * Apply this (mod) function instance to the arguments provided
	 */
	private Object applyMod(Object eval1, Object eval2) throws PrismLangException
	{
		try {
			int i1 = (int) TypeInt.getInstance().castValueTo(eval1);
			int i2 = (int) TypeInt.getInstance().castValueTo(eval2);
			return evaluateMod(i1, i2);
		} catch (PrismLangException e) {
			e.setASTElement(this);
			throw e;
		}
	}

	public static int evaluateMod(int i, int j) throws PrismLangException
	{
		// Non-positive divisor not allowed 
		if (j <= 0)
			throw new PrismLangException("Attempt to compute modulo with non-positive divisor");
		// Take care of negative case (% is remainder, not modulo)
		int rem = i % j;
		return (rem < 0) ? rem + j : rem;
	}

	public BigRational evaluateModExact(EvaluateContext ec) throws PrismLangException
	{
		BigRational a = getOperand(0).evaluateExact(ec);
		BigRational b = getOperand(1).evaluateExact(ec);

		if (!a.isInteger() && !b.isInteger()) {
			throw new PrismLangException("Can not compute mod for non-integer arguments", this);
		}
		return new BigRational(a.getNum().mod(b.getNum()));
	}

	/**
	 * Apply this (log) function instance to the arguments provided
	 */
	private Object applyLog(Object eval1, Object eval2) throws PrismLangException
	{
		try {
			double d1 = (int) TypeDouble.getInstance().castValueTo(eval1);
			double d2 = (int) TypeDouble.getInstance().castValueTo(eval2);
			return evaluateLog(d1, d2);
		} catch (PrismLangException e) {
			e.setASTElement(this);
			throw e;
		}
	}

	public static double evaluateLog(double x, double b) throws PrismLangException
	{
		return PrismUtils.log(x, b);
	}

	public BigRational evaluateLogExact(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Currently, can not compute log exactly", this);
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
	public Expression deepCopy()
	{
		int i, n;
		ExpressionFunc e;

		e = new ExpressionFunc(name);
		e.setOldStyle(oldStyle);
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			e.addOperand((Expression) getOperand(i).deepCopy());
		}
		e.setType(type);
		e.setPosition(this);

		return e;
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
