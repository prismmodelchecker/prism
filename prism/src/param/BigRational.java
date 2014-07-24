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

/**
 * Provides a class to store big rational numbers.
 * Nominator and denominator of a number stored using this class are not
 * necessarily coprime. However, cancellation is applied by default.
 * The special values infinity (INF), minus infinity (MINF) and not a number
 * (NAN)are provided. For them, the usual rules apply (INF * INF = INF,
 * MINF&INF=MINF, etc.), with the exception that INF+MINF=0, INF-INF=0, etc
 * rather than NAN.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
public final class BigRational implements Comparable<BigRational>
{
	/** the BigInteger "-1" */
	private final static BigInteger BMONE = BigInteger.ONE.negate();
	/** the BigInteger "2" */
	private final static BigInteger BITWO = new BigInteger("2");
	/** the BigInteger "10" */
	private final static BigInteger BITEN = new BigInteger("10");

	/** the BigRational "1" */
	final static BigRational ONE = new BigRational(BigInteger.ONE);
	/** the BigRational "-1" */
	final static BigRational MONE = new BigRational(BigInteger.ONE);
	/** the BigRational "0" */
	final static BigRational ZERO = new BigRational(BigInteger.ZERO);
	/** the BigRational "infinity" */
	final static BigRational INF = new BigRational(BigInteger.ONE, BigInteger.ZERO);
	/** the BigRational "-infinity" */
	final static BigRational MINF = new BigRational(BMONE, BigInteger.ZERO);
	/** the BigRational "not a number" */
	final static BigRational NAN = new BigRational(BigInteger.ZERO, BigInteger.ZERO);

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
				num = BigInteger.ZERO;
				den = BigInteger.ONE;
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
		}
		BigInteger num;
		BigInteger den;
		string = string.trim();
		int slashIdx = string.indexOf('/');
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
			num = new BigInteger((negate ? "-" : "") + noDotCoeff);
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
			num = new BigInteger(string.substring(0, slashIdx));
			den = new BigInteger(string.substring(slashIdx + 1, string.length()));
			BigRational r = cancel(num, den);
			this.num = r.num;
			this.den = r.den;
			return;
		}
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
}
