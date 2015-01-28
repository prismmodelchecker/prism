//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package prism;

import parser.Values;
import parser.ast.ExpressionTemporal;

/**
 * Canonical representation of an integer bound, with strict/non-strict lower and upper bound.
 */
public class IntegerBound
{
	/** The lowest integer that is included in the bound. {@code null} for "no bound". */
	private Integer lowest;
	/** The highest integer that is included in the bound. {@code null} for "no bound". */
	private Integer highest;

	/** Create new bounds.
	 * @param lower: The lower bound, {@code null} represents "no lower bound"
	 * @param lower_strict: Is the lower bound strict (> lower) or non-strict (>= lower)
	 * @param upper: The upper bound, {@code null} represents "no upper bound"
	 * @param upper_strict: Is the upper bound strict (< upper) or non-strict (<= upper)
	 */
	public IntegerBound(Integer lower, boolean lower_strict, Integer upper, boolean upper_strict)
	{
		// normalize
		if (lower_strict && lower != null) {
			lowest = new Integer(lower+1);
		} else {
			lowest = lower;
		}
		if (upper_strict && upper != null) {
			highest = new Integer(upper-1);
		} else {
			highest = upper;
		}
	}

	/**
	 * Extract the bounds from an {@code ExpressionTemporal} expression
	 * and create the corresponding {@code IntegerBound}.
	 *
	 * Expects that constants for the upper and lower bounds have already been resolved.
	 *
	 * If {@code check} is {@code true}, throws an exception for negative or empty bounds.
	 *
	 * @param expression the expression
	 * @param check check for non-negative bounds / non-emptiness?
	 * @return the {@code IntegerBound} for the expression
	 * @throws PrismException
	 */
	public static IntegerBound fromExpressionTemporal(ExpressionTemporal expression, boolean check) throws PrismException
	{
		return fromExpressionTemporal(expression, null, check);
	}

	/**
	 * Extract the bounds from an {@code ExpressionTemporal} expression
	 * and create the corresponding {@code IntegerBound}, resolving constants
	 * via {@code constantValues}.
	 *
	 * If {@code check} is {@code true}, throws an exception for negative or empty bounds.
	 *
	 * @param expression the expression
	 * @param values the values for constants (may be {@code null})
	 * @param check check for non-negative bounds / non-emptiness?
	 * @return the {@code IntegerBound} for the expression
	 * @throws PrismException
	 */
	public static IntegerBound fromExpressionTemporal(ExpressionTemporal expression, Values constantValues, boolean check) throws PrismException
	{
		IntegerBound bounds =  new IntegerBound(expression.getLowerBound() == null ? null : expression.getLowerBound().evaluateInt(constantValues),
		                                          expression.lowerBoundIsStrict(),
		                                          expression.getUpperBound() == null ? null : expression.getUpperBound().evaluateInt(constantValues),
		                                          expression.upperBoundIsStrict());

		if (check) {
			if (bounds.hasNegativeBound()) {
				throw new PrismException("Negative bound in "+expression.toString());
			}
			if (bounds.isEmpty()) {
				throw new PrismException("Empty bound in "+expression.toString());
			}
		}

		return bounds;
	}

	/** Returns {@code true} if there exists some lower bound. */
	public boolean hasLowerBound()
	{
		return lowest != null;
	}

	/** Returns {@code true} if there exists some upper bound. */
	public boolean hasUpperBound()
	{
		return highest != null;
	}

	/**
	 * Returns the lowest integer included in the bounds. {@code null} = "no bound".
	 * Assumes that {@code isEmpty()} is {@code false}.
	 **/
	public Integer getLowestInteger()
	{
		return lowest;
	}

	/**
	 * Returns the highest integer included in the bounds. {@code null} = "no bound".
	 * Assumes that {@code isEmpty} is {@code false}.
	 **/
	public Integer getHighestInteger()
	{
		return highest;
	}

	/** Returns {@code true} if the lower or upper bound is negative. */
	public boolean hasNegativeBound()
	{
		if (lowest != null && lowest < 0 ||
		    highest != null && highest < 0) {
			return true;
		}
		return false;
	}

	/** Returns {@code true} if the lower bound is higher than the upper bound. */
	public boolean isEmpty()
	{
		if (hasLowerBound() && hasUpperBound()) return getLowestInteger()>getHighestInteger();
		return false;
	}

	/** Returns true if {@code value} is in the bounds. */
	public boolean isInBounds(int value)
	{
		if (lowest!=null) {
			if (value >= lowest) {
				// continue
			} else {
				return false;
			}
		}

		if (highest!=null) {
			if (value <= highest) {
				return true;
			} else {
				return false;
			}
		}

		// no bound (upper=lower=null)
		return true;
	}

	/** Get the maximal interesting value, i.e., the value v such that
	 *  for _all_ i>=v either isInBound(i)=true or isInBound(i)=false */
	public int getMaximalInterestingValue()
	{
		int max = 0;

		if (isEmpty()) return 0;

		if (lowest != null) {
			max = lowest;
		}

		if (highest != null) {
			max = highest + 1;
		}

		return max;
	}

	/** String representation */
	public String toString()
	{
		if (hasLowerBound()) {
			if (hasUpperBound()) {
				if (getLowestInteger().equals(getHighestInteger())) {
					return "="+getLowestInteger();
				}
				return "["+getLowestInteger()+","+getHighestInteger()+"]";
			} else {
				return ">="+getLowestInteger();
			}
		} else {
			if (hasUpperBound()) {
				return "<="+getHighestInteger();
			} else {
				return "";
			}
		}
	}
	
	public static void main(String args[])
	{
		System.out.println(new IntegerBound(1, true, 3, false));
	}
}
