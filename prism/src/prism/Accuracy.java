//==============================================================================
//	
//	Copyright (c) 2020-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

/**
 * Class to store information about the accuracy of a model checking result.
 */
public class Accuracy
{
	/**
	 * The possible levels of accuracy. 
	 */
	public enum AccuracyLevel { EXACT, EXACT_FLOATING_POINT, BOUNDED, ESTIMATED_BOUNDED, PROBABLY_BOUNDED };
	
	/**
	 * The level of accuracy provided. 
	 */
	private AccuracyLevel level;

	/**
	 * The error bound between the result and the correct answer.
	 */
	private double errorBound;
	
	/**
	 * The default value for an error bound, if unspecified.
	 */
	public final static double DEFAULT_ERROR_BOUND = 1e-5;
	
	/**
	 * Error bound (absolute) assumed resulting from storage of values using floating point arithmetic.
	 */
	public final static double FLOATING_POINT_ERROR = 1e-12;
	
	/**
	 * The possible types of accuracy: absolute or relative. 
	 */
	public enum AccuracyType { ABSOLUTE, RELATIVE };
	
	/**
	 * The type of accuracy: absolute or relative. 
	 */
	private AccuracyType type;
	
	/**
	 * If appropriate, the probability with which the accuracy is guaranteed
	 */
	private double probability = 1.0;
	
	/**
	 * Create an accuracy of the specified level.
	 * For "bounded" accuracies, {@link Accuracy.DEFAULT_ERROR_BOUND} is assumed.
	 * The error bound is also assumed to be absolute.
	 */
	public Accuracy(AccuracyLevel level)
	{
		this(level, DEFAULT_ERROR_BOUND);
	}
	
	/**
	 * Create an accuracy of the specified level and with the specified error bound.
	 * The error bound is assumed to be absolute. It is ignored for "exact" accuracy levels.   
	 */
	public Accuracy(AccuracyLevel level, double errorBound)
	{
		this(level, errorBound, AccuracyType.ABSOLUTE);
	}
	
	/**
	 * Create an accuracy specification of the specified level, the specified error bound
	 * and the specified accuracy type: absolute (true) or relative (false).
	 * The latter two are ignored for "exact" accuracy levels.   
	 */
	public Accuracy(AccuracyLevel level, double errorBound, boolean absolute)
	{
		this(level, errorBound, absolute ? AccuracyType.ABSOLUTE : AccuracyType.RELATIVE);
	}
	
	/**
	 * Create an accuracy specification of the specified level, the specified error bound
	 * and the specified type: absolute or relative bound.
	 * The latter two are ignored for "exact" accuracy levels.   
	 */
	public Accuracy(AccuracyLevel level, double errorBound, AccuracyType type)
	{
		setLevel(level);
		switch (level) {
		case EXACT:
		case EXACT_FLOATING_POINT:
			setErrorBound(0.0);
			setType(AccuracyType.ABSOLUTE);
			break;
		case BOUNDED:
		case ESTIMATED_BOUNDED:
		case PROBABLY_BOUNDED:
			setErrorBound(errorBound);
			setType(type);
			break;
		}
	}
	
	/**
	 * Set the accuracy level.
	 */
	public void setLevel(AccuracyLevel level)
	{
		this.level = level;
	}
	
	/**
	 * Set the error bound (for non-exact accuracies)
	 * between the result and the correct answer. 
	 */
	public void setErrorBound(double errorBound)
	{
		this.errorBound = errorBound;
	}
	
	/**
	 * Set the probability with which the accuracy is guaranteed
	 * (for the case where the level is {@link AccuracyLevel.PROBABLY_BOUNDED}).
	 */
	public void setProbability(double probability)
	{
		this.probability = probability;
	}
	
	/**
	 * Set the accuracy type (absolute or relative).
	 */
	public void setType(AccuracyType type)
	{
		this.type = type;
	}
	
	/**
	 * Get the accuracy level.
	 */
	public AccuracyLevel getLevel()
	{
		return level;
	}
	
	/**
	 * Get the error bound between the result and the correct answer.
	 * Will return 0.0 for both {@code EXACT} and {@code EXACT_FLOATING_POINT},
	 * i.e., error due to floating point representation is ignored here. 
	 */
	public double getErrorBound()
	{
		switch (level) {
		case EXACT:
		case EXACT_FLOATING_POINT:
			return 0.0;
		case BOUNDED:
		case ESTIMATED_BOUNDED:
		case PROBABLY_BOUNDED:
		default:
			return errorBound;
		}
	}
	
	/**
	 * Get the accuracy type (absolute or relative).
	 */
	public AccuracyType getType()
	{
		return type;
	}
	
	/**
	 * Get a string representation of the accuracy type (absolute or relative).
	 */
	public String getTypeString()
	{
		return type == AccuracyType.RELATIVE ? "relative" : "absolute";
	}
	
	/**
	 * Get the absolute error bound between the (specified) result and the correct answer.
	 */
	public double getAbsoluteErrorBound(double result)
	{
		if (type == AccuracyType.ABSOLUTE) {
			return errorBound;
		} else {
			return Double.isFinite(result) ? (errorBound * Math.abs(result)) : 0.0;
		}
	}
	
	/**
	 * Get the relative error bound between the (specified) result and the correct answer.
	 */
	public double getRelativeErrorBound(double result)
	{
		if (type == AccuracyType.RELATIVE) {
			return errorBound;
		} else {
			return (errorBound == 0.0 || Double.isInfinite(result)) ? 0.0 : errorBound / Math.abs(result);
		}
	}
	
	/**
	 * Get the lower bound for a (specified) result,
	 * factoring in the error bound (and ignoring possible floating point error).
	 */
	public double getResultLowerBound(double result)
	{
		return result - getAbsoluteErrorBound(result);
	}
	
	/**
	 * Get the upper bound for a (specified) result,
	 * factoring in the error bound (and ignoring possible floating point error).
	 */
	public double getResultUpperBound(double result)
	{
		return result + getAbsoluteErrorBound(result);
	}
	
	/**
	 * Get the lower bound for a (specified) result,
	 * factoring in the error bound and, optionally, possible floating point error.
	 */
	public double getResultLowerBound(double result, boolean fpError)
	{
		double fpErrorBound = fpError ? FLOATING_POINT_ERROR : 0.0;
		return result - getAbsoluteErrorBound(result) - fpErrorBound;
	}
	
	/**
	 * Get the upper bound for a (specified) result,
	 * factoring in the error bound and, optionally, possible floating point error.
	 */
	public double getResultUpperBound(double result, boolean fpError)
	{
		double fpErrorBound = fpError ? FLOATING_POINT_ERROR : 0.0;
		return result + getAbsoluteErrorBound(result) + fpErrorBound;
	}
	
	/**
	 * Get the probability with which the accuracy is guaranteed
	 * (for the case where the level is {@link AccuracyLevel.PROBABLY_BOUNDED}; otherwise 1.0).
	 */
	public double getProbability()
	{
		return level == AccuracyLevel.PROBABLY_BOUNDED ? probability : 1.0;
	}
	
	/**
	 * Get a string representation of the accuracy.
	 * The actual result needs to be passed in, in order to
	 * show both absolute and relative accuracy.
	 */
	public String toString(Object result)
	{
		switch (getLevel()) {
		case EXACT:
			return "exact";
		case EXACT_FLOATING_POINT:
			return "exact floating point";
		case BOUNDED:
		case ESTIMATED_BOUNDED:
		case PROBABLY_BOUNDED:
		default:
			// Numerical results 
			if (result instanceof Double) {
				double d = (Double) result;
				String s = "+/- " + getAbsoluteErrorBound(d);
				if (level == AccuracyLevel.ESTIMATED_BOUNDED) {
					s += " estimated";
				} else if (level == AccuracyLevel.PROBABLY_BOUNDED) {
					s += " with probability " + getProbability();
				}
				return s + "; rel err " + getRelativeErrorBound(d);
			}
			// Non-numerical: only really makes sense for "probably"
			else if (level == AccuracyLevel.PROBABLY_BOUNDED) {
				return "with probability " + getProbability();
			}
		}
		// Default to empty: will be ignored
		return "";
	}
	
	@Override
	public String toString()
	{
		switch (getLevel()) {
		case EXACT:
			return "exact";
		case EXACT_FLOATING_POINT:
			return "exact floating point";
		case BOUNDED:
		case ESTIMATED_BOUNDED:
		case PROBABLY_BOUNDED:
		default:
			String s = "+/- " + getErrorBound() + " " + getTypeString();
			if (level == AccuracyLevel.ESTIMATED_BOUNDED) {
				s += " estimated";
			} else if (level == AccuracyLevel.PROBABLY_BOUNDED) {
				s += " with probability " + getProbability();
			}
			return s;
		}
	}
}
