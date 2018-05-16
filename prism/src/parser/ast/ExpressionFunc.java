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
		switch (code) {
		case MIN:
			return evaluateMin(ec);
		case MAX:
			return evaluateMax(ec);
		case FLOOR:
			return evaluateFloor(ec);
		case CEIL:
			return evaluateCeil(ec);
		case ROUND:
			return evaluateRound(ec);
		case POW:
			return evaluatePow(ec);
		case MOD:
			return evaluateMod(ec);
		case LOG:
			return evaluateLog(ec);
		}
		throw new PrismLangException("Unknown function \"" + name + "\"", this);
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

	
	private Object evaluateMin(EvaluateContext ec) throws PrismLangException
	{
		int i, j, n, iMin;
		double d, dMin;

		if (type instanceof TypeInt) {
			iMin = getOperand(0).evaluateInt(ec);
			n = getNumOperands();
			for (i = 1; i < n; i++) {
				j = getOperand(i).evaluateInt(ec);
				iMin = (j < iMin) ? j : iMin;
			}
			return new Integer(iMin);
		} else {
			dMin = getOperand(0).evaluateDouble(ec);
			n = getNumOperands();
			for (i = 1; i < n; i++) {
				d = getOperand(i).evaluateDouble(ec);
				dMin = (d < dMin) ? d : dMin;
			}
			return new Double(dMin);
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

	private Object evaluateMax(EvaluateContext ec) throws PrismLangException
	{
		int i, j, n, iMax;
		double d, dMax;

		if (type instanceof TypeInt) {
			iMax = getOperand(0).evaluateInt(ec);
			n = getNumOperands();
			for (i = 1; i < n; i++) {
				j = getOperand(i).evaluateInt(ec);
				iMax = (j > iMax) ? j : iMax;
			}
			return new Integer(iMax);
		} else {
			dMax = getOperand(0).evaluateDouble(ec);
			n = getNumOperands();
			for (i = 1; i < n; i++) {
				d = getOperand(i).evaluateDouble(ec);
				dMax = (d > dMax) ? d : dMax;
			}
			return new Double(dMax);
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

	public Integer evaluateFloor(EvaluateContext ec) throws PrismLangException
	{
		try {
			return evaluateFloor(getOperand(0).evaluateDouble(ec));
		} catch (PrismLangException e) {
			e.setASTElement(this);
			throw e;
		}
	}

	public static int evaluateFloor(double arg) throws PrismLangException
	{
		// Check for NaN or +/-inf, otherwise possible errors lost in cast to int
		if (Double.isNaN(arg) || Double.isInfinite(arg))
			throw new PrismLangException("Cannot take floor() of " + arg);
		return (int) Math.floor(arg);
	}

	public Integer evaluateCeil(EvaluateContext ec) throws PrismLangException
	{
		try {
			return evaluateCeil(getOperand(0).evaluateDouble(ec));
		} catch (PrismLangException e) {
			e.setASTElement(this);
			throw e;
		}
	}

	public static int evaluateCeil(double arg) throws PrismLangException
	{
		// Check for NaN or +/-inf, otherwise possible errors lost in cast to int
		if (Double.isNaN(arg) || Double.isInfinite(arg))
			throw new PrismLangException("Cannot take ceil() of " + arg);
		return (int) Math.ceil(arg);
	}

	public Integer evaluateRound(EvaluateContext ec) throws PrismLangException
	{
		try {
			return evaluateRound(getOperand(0).evaluateDouble(ec));
		} catch (PrismLangException e) {
			e.setASTElement(this);
			throw e;
		}
	}

	public static int evaluateRound(double arg) throws PrismLangException
	{
		// Check for NaN, otherwise possible errors lost in cast to int
		if (Double.isNaN(arg))
			throw new PrismLangException("Cannot take round() of " + arg);
		return (int) Math.round(arg);
	}

	public BigRational evaluateFloorExact(EvaluateContext ec) throws PrismLangException
	{
		return getOperand(0).evaluateExact(ec).floor();
	}

	public BigRational evaluateCeilExact(EvaluateContext ec) throws PrismLangException
	{
		return getOperand(0).evaluateExact(ec).ceil();
	}

	public BigRational evaluateRoundExact(EvaluateContext ec) throws PrismLangException
	{
		return getOperand(0).evaluateExact(ec).round();
	}

	public Object evaluatePow(EvaluateContext ec) throws PrismLangException
	{
		try {
			if (type instanceof TypeInt) {
				return new Integer(evaluatePowInt(getOperand(0).evaluateInt(ec), getOperand(1).evaluateInt(ec)));
			} else {
				return new Double(evaluatePowDouble(getOperand(0).evaluateDouble(ec), getOperand(1).evaluateDouble(ec)));
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
		double res = Math.pow(base, exp);
		// Check for overflow
		if (res > Integer.MAX_VALUE)
			throw new PrismLangException("Overflow evaluating integer power");
		// Check for underflow
		if (res < Integer.MIN_VALUE)
			throw new PrismLangException("Underflow evaluating integer power");
		return (int) res;
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

	public Object evaluateMod(EvaluateContext ec) throws PrismLangException
	{
		try {
			return new Integer(evaluateMod(getOperand(0).evaluateInt(ec), getOperand(1).evaluateInt(ec)));
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

	public Object evaluateLog(EvaluateContext ec) throws PrismLangException
	{
		try {
			return new Double(evaluateLog(getOperand(0).evaluateDouble(ec), getOperand(1).evaluateDouble(ec)));
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
