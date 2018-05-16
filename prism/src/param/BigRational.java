//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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

package param;

import java.math.BigInteger;

import prism.PrismLangException;

/**
 * Provides a class to store big rational numbers.
 * Numerator and denominator of a number stored using this class are not
 * necessarily coprime. However, cancellation is applied by default.
 * The special values infinity (INF), minus infinity (MINF) and not a number
 * (NAN)are provided. For them, the usual rules apply (INF * INF = INF,
 * MINF&INF=MINF, etc.), with the exception that INF+MINF=0, INF-INF=0, etc
 * rather than NAN.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
public final class BigRational extends Number implements Comparable<BigRational>
{
	/** Serial version for serialisation */
	private static final long serialVersionUID = 8273185089413305187L;

	/** the BigInteger "-1" */
	private final static BigInteger BMONE = BigInteger.ONE.negate();
	/** the BigInteger "2" */
	private final static BigInteger BITWO = new BigInteger("2");
	/** the BigInteger "10" */
	private final static BigInteger BITEN = new BigInteger("10");

	/** the BigRational "1" */
	public final static BigRational ONE = new BigRational(BigInteger.ONE);
	/** the BigRational "2" */
	public final static BigRational TWO = new BigRational(BigInteger.valueOf(2));
	/** the BigRational "-1" */
	public final static BigRational MONE = new BigRational(BigInteger.ONE).negate();
	/** the BigRational "0" */
	public final static BigRational ZERO = new BigRational(BigInteger.ZERO);
	/** the BigRational "1/2" */
	public final static BigRational HALF = ONE.divide(TWO);
	/** the BigRational "infinity" */
	public final static BigRational INF = new BigRational(BigInteger.ONE, BigInteger.ZERO);
	/** the BigRational "-infinity" */
	public final static BigRational MINF = new BigRational(BMONE, BigInteger.ZERO);
	/** the BigRational "not a number" */
	public final static BigRational NAN = new BigRational(BigInteger.ZERO, BigInteger.ZERO);

	/** numerator */
	private BigInteger num;
	/** denominator */
	private BigInteger den;

	// constructors

	/**
	 * Creates a new BigRational with value 0.
	 */
	public BigRational()
	{
		this.num = BigInteger.ZERO;
		this.den = BigInteger.ONE;
	}

	/**
	 * Creates a new BigRational with value {@code num}.
	 * 
	 * @param num value of new rational as an integer value
	 */
	public BigRational(BigInteger num)
	{
		this.num = num;
		this.den = BigInteger.ONE;
	}

	/**
	 * Creates a new BigRational with value {@code num} / {@code den}.
	 * Cancellation of {@code num} and {@code den} is applied.
	 * 
	 * @param num numerator of this BigRational
	 * @param den denominator of this BigRational
	 */
	public BigRational(BigInteger num, BigInteger den)
	{
		this(num, den, true);
	}

	/**
	 * Creates a new BigRational with value {@code num} / {@code den}.
	 * Whether cancellation between {@code num} and {@code den} is applied depends
	 * on {@code cancel}.
	 * 
	 * @param num numerator of this BigRational
	 * @param den denominator of this BigRational
	 * @param cancel true to ensure resulting BigRational is coprime
	 */
	public BigRational(BigInteger num, BigInteger den, boolean cancel)
	{
		if (den.equals(BigInteger.ZERO)) {
			int cmp = num.compareTo(BigInteger.ZERO);
			switch (cmp) {
			case -1:
				num = BMONE;
				break;
			case 1:
				num = BigInteger.ONE;
				break;
			}
		}
		if (cancel) {
			if (num.equals(BigInteger.ZERO)) {
				if (!den.equals(BigInteger.ZERO)) {
					// not NaN (= 0/0), so this is a real zero:
					// normalise by setting denominator to 1
					num = BigInteger.ZERO;
					den = BigInteger.ONE;
				}
			} else {
				BigInteger gcd = num.gcd(den);
				num = num.divide(gcd);
				den = den.divide(gcd);
				if (den.signum() == -1) {
					num = num.negate();
					den = den.negate();
				}
			}
		}
		this.num = num;
		this.den = den;
	}

	/**
	 * Creates a new BigRational with value {@code num} / {@code den}.
	 * Cancellation of {@code num} and {@code den} is applied.
	 * 
	 * @param num numerator of this BigRational
	 * @param den denominator of this BigRational
	 */
	public BigRational(long num, long den)
	{
		this(new BigInteger(Long.toString(num)), new BigInteger(Long.toString(den)));
	}

	/**
	 * Creates a new BigRational with value {@code num}.
	 * 
	 * @param num value of new rational as an integer value
	 */
	public BigRational(long num)
	{
		this(num, 1);
	}

	/**
	 * Creates a new BigRational from string {@code string}.
	 * Formats supported are num / den where num and den are integers,
	 * and scientific notation.
	 * 
	 * @param string string to create BigRational from
	 */
	public BigRational(String string)
	{
		if (string.equals("Infinity") || string.equals("Inf")) {
			this.num = new BigInteger("1");
			this.den = new BigInteger("0");
			return;
		} else if (string.equals("-Infinity") || string.equals("-Inf")) {
			this.num = new BigInteger("-1");
			this.den = new BigInteger("0");
			return;
		} else if (string.equals("NaN")) {
			this.num = new BigInteger("0");
			this.den = new BigInteger("0");
			return;
		}
		string = string.trim();
		int slashIdx = string.lastIndexOf('/');
		if (slashIdx < 0) {
			// decimal point notation
			Double.parseDouble(string); // ensures correctness of format
			boolean negate = false;
			if (string.charAt(0) == '-') {
				negate = true;
				string = string.substring(1);
			}
			int ePos = string.indexOf("e");
			if (ePos < 0) {
				ePos = string.indexOf("E");
			}
			String coefficient = string.substring(0, ePos >= 0 ? ePos : string.length());
			int dotIdx = coefficient.indexOf('.');
			int expo = 0;
			String noDotCoeff;
			if (dotIdx >= 0) {
				noDotCoeff = coefficient.substring(0, dotIdx);
				noDotCoeff += coefficient.substring(dotIdx + 1);
				expo = -coefficient.substring(dotIdx + 1).length();
			} else {
				noDotCoeff = coefficient;
			}
			if (ePos >= 0) {
				String eStr = string.substring(ePos + 1);
				int eInt = Integer.parseInt(eStr);
				expo += eInt;
			}
			BigInteger num = new BigInteger((negate ? "-" : "") + noDotCoeff);
			BigInteger den;
			BigInteger ten = BITEN;
			if (expo == 0) {
				den = BigInteger.ONE;
			} else if (expo > 0) {
				den = BigInteger.ONE;
				num = num.multiply(ten.pow(expo));
			} else { // expo < 0
				den = ten.pow(-expo);
			}
			BigRational result = new BigRational(num, den, true);
			this.num = result.num;
			this.den = result.den;
		} else {
			// fractional
			if (slashIdx == 0 || slashIdx == string.length()-1) {
				throw new NumberFormatException("Illegal fraction syntax");
			}
			// because we use lastIndexOf, we obtain left-associativity,
			// i.e. a/b/c is interpreted as (a/b)/c
			BigRational num = new BigRational(string.substring(0, slashIdx));
			BigRational den = new BigRational(string.substring(slashIdx + 1, string.length()));
			BigRational r = num.divide(den);
			this.num = r.num;
			this.den = r.den;
			return;
		}
	}

	/**
	 * Construct a BigRational from the given object.
	 * Throws an IllegalArgumentException if there is no
	 * known conversion.
	 */
	public static BigRational from(Object value)
	{
		if (value instanceof BigRational) {
			BigRational v = (BigRational)value;
			return new BigRational(v.num, v.den);
		} else if (value instanceof Integer) {
			return new BigRational((int) value);
		} else if (value instanceof Long) {
			return new BigRational((long) value);
		} else if (value instanceof Boolean) {
			boolean v = (Boolean)value;
			return new BigRational(v ? 1 : 0);
		} else if (value instanceof Double) {
			// TODO: ? might be imprecise, perhaps there
			// is a way to get the full precision?
			return new BigRational(((Double)value).toString());
		} else if (value instanceof String) {
			return new BigRational((String)value);
		}
		throw new IllegalArgumentException("Can not convert from " + value.getClass() + " to BigRational");
	}

	// helper functions

	/**
	 * Negates this number.
	 * Negation of INF, MINF are as usual, negation of NAN is NAN.
	 * 
	 * @return negated BigRational
	 */
	public BigRational negate()
	{
		return new BigRational(num.negate(), den, false);
	}

	/**
	 * Convert to coprime BigRational.
	 * 
	 * @return coprime rational with the same value as this object
	 */
	public BigRational cancel()
	{
		return new BigRational(this.num, this.den, true);
	}

	/**
	 * Creates a new BigRational with value {@code num} / {@code den}.
	 * Makes sure that {@code num} and {@code den} are coprime.
	 * To be used  
	 * 
	 * @param num numerator of new BigRational
	 * @param den denominator of new BigRational
	 * @return BigRational with value {@code num} / {@code den}
	 */
	private static BigRational cancel(BigInteger num, BigInteger den)
	{
		return new BigRational(num, den);
	}

	// operations

	public BigRational add(BigRational other, boolean cancel)
	{
		if (this.isNaN() || other.isNaN()) {
			return NAN;
		}
		if (this.isInf() || other.isInf()) {
			if (this.isMInf() || other.isMInf()) {
				return ZERO;
			}
			return INF;
		}
		if (this.isMInf() || other.isMInf()) {
			return MINF;
		}
		BigInteger num = this.num.multiply(other.den).add(other.num.multiply(this.den));
		BigInteger den = this.den.multiply(other.den);
		return new BigRational(num, den, cancel);
	}

	public BigRational add(BigRational other)
	{
		return add(other, true);
	}

	public BigRational subtract(BigRational other)
	{
		if (this.isNaN() || other.isNaN()) {
			return NAN;
		}
		if ((this.isInf() && other.isInf()) || (this.isMInf() && other.isMInf())) {
			return ZERO;
		}
		if (this.isInf()) {
			return INF;
		}
		if (this.isMInf()) {
			return MINF;
		}
		if (other.isInf()) {
			return INF;
		}
		if (other.isMInf()) {
			return MINF;
		}
		BigInteger num = this.num.multiply(other.den).subtract(other.num.multiply(this.den));
		BigInteger den = this.den.multiply(other.den);
		return new BigRational(num, den);
	}

	/**
	 * Multiply this BigRational with {@code other}.
	 * 
	 * @param other BigRational to multiply with
	 * @return result of the multiplication
	 */
	public BigRational multiply(BigRational other, boolean cancel)
	{
		if (this.isNaN() || other.isNaN()) {
			return NAN;
		}
		if (this.isZero() || other.isZero()) {
			return ZERO;
		}
		if (this.isInf() || other.isInf()) {
			return this.signum() * other.signum() == 1 ? INF : MINF;
		}
		BigInteger num = this.num.multiply(other.num);
		BigInteger den = this.den.multiply(other.den);
		return new BigRational(num, den, cancel);
	}

	public BigRational multiply(BigRational other)
	{
		return multiply(other, true);
	}

	/**
	 * Multiply this BigRational with {@code other}.
	 * 
	 * @param other long to multiply with
	 * @param cancel whether ensure result rational is comprime
	 * @return result of the multiplication
	 */
	public BigRational multiply(long other, boolean cancel)
	{
		return multiply(new BigRational(other), cancel);
	}

	/**
	 * Multiply this BigRational with {@code other}.
	 * Ensures result rational is coprime
	 * 
	 * @param other long to multiply with
	 * @return result of the multiplication
	 */
	public BigRational multiply(long other)
	{
		return multiply(other, true);
	}

	/**
	 * Multiply this BigRational by {@code other}.
	 * 
	 * @param other long to divide by
	 * @param cancel whether ensure result rational is comprime
	 * @return result of the division
	 */
	public BigRational divide(BigRational other, boolean cancel)
	{
		if (other.isInf() || other.isMInf()) {
			return NAN;
		}
		BigRational inverseOther = new BigRational(other.den, other.num, cancel);
		return multiply(inverseOther, cancel);
	}

	/**
	 * Divides this BigRational with {@code other}.
	 * Ensures result rational is coprime
	 * 
	 * @param other long to divide by
	 * @return result of the division
	 */
	public BigRational divide(BigRational other)
	{
		return divide(other, true);
	}

	/**
	 * Divides this BigRational with {@code other}.
	 * Ensures result rational is coprime
	 * 
	 * @param other long to divide by
	 * @return result of the division
	 */
	public BigRational divide(long other)
	{
		return divide(new BigRational(other));
	}

	/**
	 * Returns the signum function of this BigInteger.
	 * 
	 * @return -1, 0 or 1 as the value of this BigInteger is negative, zero or positive.
	 */
	public int signum()
	{
		if (isInf()) return 1;
		if (isMInf()) return -1;
		return num.signum() * den.signum();
	}

	/**
	 * Returns a BigRational whose value is {@code this} to the {@code exponent}.
	 * 
	 * @param exponent exponent to which this number is raised
	 * @return {@code this} to the {@code exponent}
	 */
	public BigRational pow(int exponent)
	{
		BigInteger num;
		BigInteger den;
		if (exponent == 0) {
			return ONE;
		} else if (exponent > 0) {
			num = this.num.pow(exponent);
			den = this.den.pow(exponent);
		} else { // exponent < 0
			exponent = -exponent;
			num = this.den.pow(exponent);
			den = this.num.pow(exponent);
		}
		return new BigRational(num, den, false);
	}

	/**
	 * Compares {@code this} to {@code obj}.
	 * Returns true iff {@code obj} is a BigRational which represents
	 * the same rational number as {@code this}.
	 * 
	 * @param obj object to compare to
	 * @return true iff {@code obj} is a BigRational which represents the same rational number as {@code this}.
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof BigRational)) {
			return false;
		}
		BigRational other = (BigRational) obj;
		if (isNaN()) {
			return other.isNaN();
		}
		if (isInf()) {
			return other.isInf();
		}
		if (isMInf()) {
			return other.isMInf();
		}
		return this.num.equals(other.num) && this.den.equals(other.den);
	}

	/**
	 * Returns a hash code for this object.
	 * 
	 * @return has code
	 */
	@Override
	public int hashCode()
	{
		return 37 * num.hashCode() + den.hashCode();
	}

	/**
	 * Returns a double approximation of value represented by this BigRational.
	 * There are no guarantees on preciseness currently, so this function
	 * should be mainly used for plotting graphs, etc. Can return
	 * {@code Double.NaN}, {@code Double.POSITIVE_INFINITY},
	 * {@code Double.NEGATIVE_INFINITY} in case this BigRational represents
	 * not-a-number, positive infinity or negative infinitey respectively.
	 * 
	 * @return a double approximation of the rational represented by this object
	 */
	public double doubleValue()
	{
		if (isNaN()) {
			return Double.NaN;
		} else if (isInf()) {
			return Double.POSITIVE_INFINITY;
		} else if (isMInf()) {
			return Double.NEGATIVE_INFINITY;
		} else if (isOne()) {
			return 1.0;
		} else if (isZero()) {
			return 0.0;
		}
		BigInteger shiftedNum;
		int signum = num.signum() * den.signum();
		BigInteger posNum = (num.signum() == 1) ? num : num.negate();
		BigInteger posDen = (den.signum() == 1) ? den : den.negate();
		shiftedNum = posNum.shiftLeft(55);
		BigInteger div = shiftedNum.divide(posDen);
		if (shiftedNum.mod(posDen).multiply(BITWO).compareTo(posDen) == 1) {
			div = div.add(BigInteger.ONE);
		}
		return signum * div.doubleValue() / Math.pow(2.0, 55);
	}

	/**
	 * Returns the value of the specified number as an {@code int},
	 * which may involve rounding or truncation.
	 * <br>
	 * Note: In contrast to the standard Number.intValue() behaviour,
	 * this implementation throws an Arithmetic exception if the underlying
	 * rational number is not an integer or if representing as an {@code int}
	 * overflows.
	 * <br>
	 * Positive and negative infinity are mapped to Integer.MAX_VALUE and Integer.MIN_VALUE,
	 * respectively, NaN is mapped to 0 (per the Java Language Specification).
	 *
	 * @return  the numeric value represented by this object after conversion
	 *          to type {@code int}.
	 */
	@Override
	public int intValue()
	{
		if (isSpecial()) {
			if (isInf()) return Integer.MAX_VALUE;
			if (isMInf()) return Integer.MIN_VALUE;
			if (isNaN()) return 0;  // per Java Language Specification
		}

		// TODO JK: In case of fraction / overflow, this method should not throw an
		// exception but return some imprecise result. We are conservative here.
		// In the future, it may make sense to have an intValueExact (similar to BigInteger)
		if (!isInteger()) {
			throw new ArithmeticException("Can not convert fractional number to int");
		}
		int value = getNum().intValue();
		if (!getNum().equals(new BigInteger(Integer.toString(value)))) {
			throw new ArithmeticException("Can not convert BigInteger to int, value " + this + " out of range");
		}
		return value;
	}

	/**
	 * Returns the value of the specified number as a {@code long},
	 * which may involve rounding or truncation.
	 * <br>
	 * Note: In contrast to the standard Number.longValue() behaviour,
	 * this implementation throws an Arithmetic exception if the underlying
	 * rational number is not an integer or if representing as a {@code long}
	 * overflows.
	 * <br>
	 * Positive and negative infinity are mapped to Long.MAX_VALUE and Long.MIN_VALUE,
	 * respectively, NaN is mapped to 0 (per the Java Language Specification).
	 *
	 * @return  the numeric value represented by this object after conversion
	 *          to type {@code int}.
	 */
	@Override
	public long longValue()
	{
		if (isSpecial()) {
			if (isInf()) return Long.MAX_VALUE;
			if (isMInf()) return Long.MIN_VALUE;
			if (isNaN()) return 0;  // per Java Language Specification
		}

		// TODO JK: In case of fraction / overflow, this method should not throw an
		// exception but return some imprecise result. We are conservative here. In the future,
		// it may make sense to have an intValueExact (similar to BigInteger)
		if (!isInteger()) {
			throw new ArithmeticException("Can not convert fractional number to long");
		}
		long value = getNum().longValue();
		if (!getNum().equals(new BigInteger(Long.toString(value)))) {
			throw new ArithmeticException("Can not convert BigInteger to long, value " + this + " out of range");
		}
		return value;
	}

	@Override
	public float floatValue()
	{
		// TODO JK: Better precision?
		return (float)doubleValue();
	}

	/**
	 * Returns a string representation of this BigRational.
	 * 
	 * @return string representation of this rational number 
	 */
	@Override
	public String toString()
	{
		if (isNaN()) {
			return "NaN";
		} else if (isInf()) {
			return "Inf";
		} else if (isMInf()) {
			return "-Inf";
		} else if (den.equals(BigInteger.ONE)) {
			return num.toString();
		} else {
			return num + "/" + den;
		}
	}

	/**
	 * Compares this BigRational to another BigRational other.
	 * 
	 * @return -1, 0 or 1 as this BigRational is numerically less than, equal to, or greater than {@code other}.
	 */
	@Override
	public int compareTo(BigRational other)
	{
		if (this.isInf()) {
			if (other.isInf()) {
				return 0;
			} else {
				return 1;
			}
		}
		if (this.isMInf()) {
			if (other.isMInf()) {
				return 0;
			} else {
				return -1;
			}
		}
		return this.num.multiply(other.den).compareTo(other.num.multiply(this.den));
	}

	/**
	 * Compares this BigRational to the long other.
	 * 
	 * @return -1, 0 or 1 as this BigRational is numerically less than, equal to, or greater than {@code other}.
	 */
	public int compareTo(long i)
	{
		return this.compareTo(new BigRational(i));
	}

	/** Returns true if this number is less than the other number */
	public boolean lessThan(BigRational other)
	{
		return this.compareTo(other) < 0;
	}

	/** Returns true if this number is less than or equal the other number */
	public boolean lessThanEquals(BigRational other)
	{
		return this.compareTo(other) <= 0;
	}

	/** Returns true if this number is greater than the other number */
	public boolean greaterThan(BigRational other)
	{
		return this.compareTo(other) > 0;
	}

	/** Returns true if this number is greater than or equal the other number */
	public boolean greaterThanEquals(BigRational other)
	{
		return this.compareTo(other) >= 0;
	}

	/**
	 * Return numerator of this BigRational as a BigInteger.
	 * 
	 * @return numerator of this BigRational as a BigInteger
	 */
	public java.math.BigInteger getNum()
	{
		return num;
	}

	/**
	 * Return denominator of this BigRational as a BigInteger.
	 * 
	 * @return denominator of this BigRational as a BigInteger
	 */
	public java.math.BigInteger getDen()
	{
		return den;
	}

	/**
	 * Return absolute value of this BigRational.
	 * 
	 * @return absolute value of this BigRational
	 */
	public BigRational abs()
	{
		if (num.signum() == -1) {
			return new BigRational(num.negate(), den);
		} else {
			return this;
		}
	}

	/**
	 * Return ceil(value), i.e., the smallest integer >= value.
	 * @throws PrismLangException for special values (NaN, infinity)
	 */
	public BigRational ceil() throws PrismLangException
	{
		if (isSpecial()) {
			throw new PrismLangException("Can not compute ceil of " + this);
		}

		BigInteger[] divideAndRemainder = getNum().divideAndRemainder(getDen());

		switch (divideAndRemainder[1].compareTo(BigInteger.ZERO)) {
		case 0:   // no remainder
		case -1:  // negative remainder: value was negative, so we ignore the remainder
			return new BigRational(divideAndRemainder[0]);
		case 1:   // positive remainder: return next-largest integer
			return new BigRational(divideAndRemainder[0].add(BigInteger.ONE));
		default:
			throw new IllegalStateException("Should not be reached");
		}
	}

	/**
	 * Return floor(value), i.e., the largest integer <= value.
	 * @throws PrismLangException for special values (NaN, infinity)
	 */
	public BigRational floor() throws PrismLangException
	{
		if (isSpecial()) {
			throw new PrismLangException("Can not compute floor of " + this);
		}

		BigInteger[] divideAndRemainder = getNum().divideAndRemainder(getDen());
		switch (divideAndRemainder[1].compareTo(BigInteger.ZERO)) {
		case 0:   // no remainder
		case 1:   // positive remainder: value was positive, so we ignore the remainder
			return new BigRational(divideAndRemainder[0]);
		case -1:  // negative remainder: value was negative, return next-smallest integer
			return new BigRational(divideAndRemainder[0].subtract(BigInteger.ONE));
		default:
			throw new IllegalStateException("Should not be reached");
		}
	}

	/**
	 * Return round(value), i.e., the integer closest to value with
	 * ties rounding towards positive infinity.
	 * @throws PrismLangException for special values (NaN, infinity)
	 */
	public BigRational round() throws PrismLangException
	{
		if (isSpecial()) {
			throw new PrismLangException("Can not compute round of " + this);
		}

		return this.add(HALF).floor();
	}

	/**
	 * Returns larger value of {@code this} and {@code other}.
	 * 
	 * @param other rational number to compare to
	 * @return {@code other} if {@code other} this is larger than {@code this} and {@code this} otherwise
	 */
	public BigRational max(BigRational other)
	{
		if (this.compareTo(other) >= 0) {
			return this;
		} else {
			return other;
		}
	}

	/**
	 * Returns smaller value of {@code this} and {@code other}.
	 * 
	 * @param other rational number to compare to
	 * @return {@code other} if {@code other} this is smaller than {@code this} and {@code this} otherwise
	 */
	public BigRational min(BigRational other)
	{
		if (this.compareTo(other) <= 0) {
			return this;
		} else {
			return other;
		}
	}

	/**
	 * Returns true iff this BigRational represents the number zero.
	 * 
	 * @return true iff this BigRational represents the number zero
	 */
	public boolean isZero()
	{
		return num.equals(BigInteger.ZERO) && den.equals(BigInteger.ONE);
	}

	/**
	 * Returns true iff this BigRational represents the number one.
	 * 
	 * @return true iff this BigRational represents the number one
	 */
	public boolean isOne()
	{
		return num.equals(BigInteger.ONE) && den.equals(BigInteger.ONE);
	}

	/**
	 * Returns true iff this BigRational represents the special value not-a-number.
	 * 
	 * @return true iff this BigRational represents the special value not-a-number"
	 */
	public boolean isNaN()
	{
		return num.equals(BigInteger.ZERO) && den.equals(BigInteger.ZERO);
	}

	/**
	 * Returns true iff this BigRational represents positive infinity.
	 * 
	 * @return true iff this BigRational represents positive infinity
	 */
	public boolean isInf()
	{
		return num.equals(BigInteger.ONE) && den.equals(BigInteger.ZERO);
	}

	/**
	 * Returns true iff this BigRational represents negative infinity.
	 * 
	 * @return true iff this BigRational represents negative infinity
	 */
	public boolean isMInf()
	{
		return num.equals(BMONE) && den.equals(BigInteger.ZERO);
	}

	/**
	 * Returns true iff this object represents a true rational number.
	 * This excludes the values for not-a-number as well as positive
	 * and negative infinity.
	 * 
	 * @return true iff this object represents a true rational number
	 */
	public boolean isRational()
	{
		return !isNaN() && !isInf() && !isMInf();
	}

	/**
	 * Returns true iff this object represents an integer, i.e.,
	 * is not not-a-number, positive, or negative infinity and
	 * the denominator is 1.
	 *
	 * @return true iff this object represents an integer
	 */
	public boolean isInteger()
	{
		return isRational() && getDen().equals(BigInteger.ONE);
	}

	/**
	 * Returns true iff this value represents a special number.
	 * This is the case if this is either not-a-number, positive or
	 * negative infinity.
	 * 
	 * @return true iff this object represents a special number
	 */
	public boolean isSpecial()
	{
		return isNaN() || isInf() || isMInf();
	}

	/**
	 * Returns true if this value equals 1 or false if this value
	 * equals 0. In all other cases, a PrismLangException is thrown.
	 */
	public boolean toBoolean() throws PrismLangException
	{
		if (isOne())
			return true;
		if (isZero())
			return false;
		throw new PrismLangException("Conversion from BigRational to Boolean not possible, invalid value: " + this);
	}

	/**
	 * Return an approximate String representation (via conversion to double).
	 * If the conversion is imprecise, the result string is prefixed by '~'.
	 */
	public String toApproximateString()
	{
		String result = Double.toString(doubleValue());
		if (new BigRational(result).equals(this)) {
			// round-trip did not lose precision
			return result;
		}
		// only approximate
		return "~" + result;

	}

	/**
	 * Returns the int representation of this value,
	 * if this value is an integer and if the integer can
	 * be represented by an int variable.
	 * In all other cases, a PrismLangException is thrown.
	 */
	public int toInt() throws PrismLangException
	{
		if (!isInteger()) {
			throw new PrismLangException("Can not convert fractional number to int");
		}
		int value = getNum().intValue();
		if (!getNum().equals(new BigInteger(Integer.toString(value)))) {
			throw new PrismLangException("Can not convert BigInteger to int, value out of range");
		}
		return value;
	}
}
