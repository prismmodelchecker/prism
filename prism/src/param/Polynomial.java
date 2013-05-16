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
import java.util.ArrayList;
import java.util.HashMap;

// TODO terms should be sorted. will become necessary if a Function is
// implemented which directly uses objects of this class to store
// rational functions

/**
 * A polynomial exressed as a sum of terms.
 * Once a polynomial is fully constructed, it is immutable and cannot
 * be changed anymore. 
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
final class Polynomial {
	/** number of variables in this polynomial */ 
	private int numVariables;
	/** coefficients of this polynomial. */
	private BigInteger[] coefficients;
	/** exponents of each term. for each term, there are {@code numVariables}
	 * entries in exponents in this array, followed by the entries for
	 * the next term (if any). */
	private int[] exponents;
	private HashMap<Point,BigRational> pointsSeen;
	/** current size of the polynomial. used during its construction. */
	private int size;
	
	/**
	 * Constructs a new polynomial.
	 * 
	 * @param numVariables number of variables the polynomial will have
	 * @param numTerms final number of terms after construction
	 */
	Polynomial(int numVariables, int numTerms)
	{
		coefficients = new BigInteger[numTerms];
		exponents = new int[numTerms * numVariables];
		this.numVariables = numVariables;
		this.pointsSeen = new HashMap<Point,BigRational>();
		this.size = 0;
	}
	
	/**
	 * Adds a term to a polynomial.
	 * To be used during its construction.
	 * 
	 * @param coefficient
	 * @param monomial
	 */
	void addTerm(BigInteger coefficient, ArrayList<Integer> monomial)
	{
		coefficients[size] = coefficient;
		for (int i = 0; i < monomial.size(); i++) {
			exponents[numVariables * size + i] = monomial.get(i);
		}
		size++;
	}
	
	/**
	 * Checks whether this polynomial is equal to the given object.
	 * For this to hold, {@code obj} must be a polynomial, and it
	 * must be the same polynomial as this polynomial.
	 * 
	 * @param obj object to compare against
	 * @return true iff {@code obj} is a polynomial which equals this polynomial
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Polynomial)) {
			return false;
		}
		
		Polynomial other = (Polynomial) obj;
		if (this.numVariables != other.numVariables) {
			return false;
		}
		if (this.coefficients.length != other.coefficients.length) {
			return false;
		}
		for (int i = 0; i < this.coefficients.length; i++) {
			if (!this.coefficients[i].equals(other.coefficients[i])) {
				return false;
			}
		}
		for (int i = 0; i < this.exponents.length; i++) {
			if (this.exponents[i] != other.exponents[i]) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Returns a hashcode for this polynomial.
	 * 
	 * @return hash code for this polynomial
	 */
	@Override
	public int hashCode() {
		int hash = numVariables;
		
		for (int i = 0; i < exponents.length; i++) {
			hash = exponents[i] + (hash << 6) + (hash << 16) - hash;
		}
		
		for (int i = 0; i < coefficients.length; i++) {
			hash = coefficients[i].hashCode() + (hash << 6) + (hash << 16) - hash;
		}

		return hash;
	}

	/**
	 * Returns a string representation of the polynomial.
	 * 
	 * @return string representation of the polynomial
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (int term = 0; term < coefficients.length; term++) {
			BigInteger coeff = coefficients[term];
			
			if (coeff.signum() > -1) {
				if (term != 0) {
					builder.append(" + ");
				}
				builder.append(coeff);
			} else {
				if (term == 0) {
					builder.append("-");
				} else {
					builder.append(" - ");
				}
				builder.append(coeff.negate());
			}

			builder.append("*");
			for (int var = 0; var < numVariables; var++) {
				int power = exponents[numVariables * term + var];
				builder.append("x");
				builder.append(var);
				builder.append("^");
				builder.append(power);
				if (var < numVariables - 1) {
					builder.append("*");
				}
			}
		}
		return builder.toString();
	}

	/**
	 * Evaluates a polynomial at a given point.
	 * {@code point} must have the same dimension as the number of variables
	 * for this to work correctly. {@code cancel} specifies whether the
	 * result is to be brought into coprime form.
	 * 
	 * @param point point to evaluate polynomial at
	 * @param cancel whether to make result coprime
	 * @return value of polynomial at given point
	 */
	BigRational evaluate(Point point, boolean cancel)
	{
		long time = System.currentTimeMillis();
		BigRational result = pointsSeen.get(point);
		if (result != null) {
			if (!cancel) {
				time = System.currentTimeMillis() - time;
				return result;
			} else {
				time = System.currentTimeMillis() - time;
				return result.cancel();
			}
		}
		result = BigRational.ZERO;
		
		for (int coeffNr = 0; coeffNr < coefficients.length; coeffNr++) {
			BigRational coeffVal = new BigRational(coefficients[coeffNr]);
			for (int var = 0; var < numVariables; var++) {
				BigRational exp = point.getDimension(var).pow(exponents[coeffNr * numVariables + var]);
				coeffVal = coeffVal.multiply(exp, cancel);				
			}
			
			result = result.add(coeffVal, cancel);
		}
		pointsSeen.put(point, result);
		time = System.currentTimeMillis() - time;
		return result;
	}
	
	/**
	 * Evaluates a polynomial at a given point.
	 * {@code point} must have the same dimension as the number of variables
	 * for this to work correctly. Result is brought into coprime form.
	 * 
	 * @param point point to evaluate polynomial at
	 * @return value of polynomial at given point
	 */
	BigRational evaluate(Point point)
	{
		return evaluate(point, true);
	}

	/**
	 * Checks whether the value of this polynomial is (strictly) greater zero. 
	 * 
	 * @param point point to evaluate polynomial at
	 * @param strict true for strictly greater zero, greater or equal else
	 * @return true if value at given {@code point} is (strictly) greater zero
	 */
	boolean check(Point point, boolean strict)
	{
		BigRational value = evaluate(point, false);
		int compare = value.signum();
		return strict ? (compare > 0) : (compare >= 0);
	}
}
