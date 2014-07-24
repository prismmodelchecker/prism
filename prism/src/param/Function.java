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

/**
 * Represents a rational function.
 * Allows for the usual operations (addition, multiplication, etc.) which
 * can be applied to rational functions, resulting in another rational
 * function. It also implements the special operator {@code star}, which
 * represents to value 1/(1-fn). Although this operation could be implemented
 * by standard operations, implementations of this class might choose a more
 * efficient solution, as this operation will be used quite often. Each
 * {@code Function} has a link to a corresponding {@code FunctionFactory},
 * which maintains objects shared by all rational functions with which it
 * can be combined by mathematical operations.
 * 
 * Functions shall be created using either the corresponding
 * {@code FunctionFactory} or by mathematical operations. They are immutable
 * objects, and cannot be changed after their creation.
 * 
 * In addition to rational functions, an object of this type can also represent
 * the special value not-a-number, negative and positive infinity.
 * 
 * It is assumed that the signums of the denominators of the rational function
 * the objects of this class represent do not change within the parameter
 * range specified in the corresponding {@code FunctionFactory}.
 * 
 * @see FunctionFactory
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
public abstract class Function extends StateValue
{
	/** function factory for this function */
	protected FunctionFactory factory;

	/**
	 * Creates a new function.
	 * For internal use.
	 * 
	 * @param factory factory used for this function
	 */
	protected Function(FunctionFactory factory)
	{
		this.factory = factory;
	}

	/**
	 * Adds {@code other} to this function.
	 * 
	 * @param other function to add to this function
	 * @return sum of {@code} this and {@other}
	 */
	abstract Function add(Function other);

	/**
	 * Negates this rational function.
	 * 
	 * @return negated rational function.
	 */
	abstract Function negate();

	/**
	 * Multiplies {@code other} with this function.
	 * 
	 * @param other function to multiply with this function
	 * @return product of {@code} this and {@other}
	 */
	abstract Function multiply(Function other);

	/**
	 * Divides this function by {@code other}.
	 * 
	 * @param other function to divide this function by
	 * @return {@code this} divided by {@other}
	 */
	abstract Function divide(Function other);

	/**
	 * Performs the {@code star} operation with this function.
	 * The value of the result is equal to 1/(1-{@code this}).
	 * It might however be represented in a different way than the
	 * function which would be obtained if this result were to be computed
	 * using subtraction and division.
	 * 
	 * @return result of star operation
	 */
	abstract Function star();

	/**
	 * Returns a simplified version for constraint checking.
	 * The function returned shall be a polynomial which is above/equal to
	 * zero for the same parameter valuations as the original function.
	 * It might evaluate to different values otherwise. This function is
	 * intended to prepare input to constraint solvers, which sometimes
	 * cannot handle division and also otherwise perform better when applied
	 * to less complex functions. 
	 * 
	 * @return simplified form for constraint checking
	 */
	abstract Function toConstraint();

	/**
	 * Evaluate this function at a given point.
	 * The {@code point} represents an evaluation of the parameters, with
	 * values assigned to parameters according to their order as given in the
	 * {@code FunctionFactory}.
	 * 
	 * @param point parameter evaluation to evaluate
	 * @param cancel whether result shall be enforced to be coprime
	 * @return value at the given parameter evaluation
	 */
	abstract BigRational evaluate(Point point, boolean cancel);

	/**
	 * Returns a BigRational representing the same number as this object.
	 * Only works of this function is actually a rational number. Otherwise,
	 * a numeric runtime exception might result.
	 * 
	 * @return BigRational representation of this function
	 */
	abstract BigRational asBigRational();

	/**
	 * Returns true iff this function represents not-a-number.
	 * @return true iff this function represents not-a-number
	 */
	abstract boolean isNaN();

	/**
	 * Returns true iff this function represents positive infinity.
	 * @return true iff this function represents positive infinity
	 */
	abstract boolean isInf();

	/**
	 * Returns true iff this function represents negative infinity.
	 * @return true iff this function represents negative infinity
	 */
	abstract boolean isMInf();

	/**
	 * Returns true iff this function represents the number one.
	 * @return true iff this function represents the number one 
	 */
	abstract boolean isOne();

	/**
	 * Returns true iff this function represents the number zero.
	 * @return true iff this function represents the number zero
	 */
	abstract boolean isZero();

	/**
	 * Multiplies {@code byNumber} with this function.
	 * 
	 * @param number to multiply with this function
	 * @return product of {@code} this and {@byNumber}
	 */
	public Function multiply(int byNumber)
	{
		Function byFunction = factory.fromLong(byNumber);
		return multiply(byFunction);
	}

	/**
	 * Divides this function by {@code byNumber}.
	 * 
	 * @param number to divide this function by
	 * @return this function divided by {@code byNumber}
	 */
	public Function divide(int byNumber)
	{
		Function byFunction = factory.fromLong(byNumber);
		return divide(byFunction);
	}

	/**
	 * Returns the {@code FunctionFactory} of this function.
	 * 
	 * @return {@code FunctionFactory} of this function
	 */
	public FunctionFactory getFactory()
	{
		return factory;
	}

	/**
	 * Aubtracts {@code other} from this function.
	 * 
	 * @param other function to subtract from this function
	 * @return this function minus {@coce other}
	 */
	public Function subtract(Function other)
	{
		return add(other.negate());
	}

	/**
	 * Evaluate this function at a given point.
	 * The {@code point} represents an evaluation of the parameters, with
	 * values assigned to parameters according to their order as given in the
	 * {@code FunctionFactory}. The result will be a coprime representation
	 * of a rational number.
	 * 
	 * @param point parameter evaluation to evaluate
	 * @return value at the given parameter evaluation
	 */
	public BigRational evaluate(Point point)
	{
		return evaluate(point, true);
	}

	/**
	 * Checks whether this function is {@code >= 0} / {@code >0} at the given point.
	 * 
	 * @param point point to check function at
	 * @param strict true for strictly larger to zero, false for larger or equal
	 * @return true iff value at {@code point} is (strictly) larger than zero
	 */
	boolean check(Point point, boolean strict)
	{
		BigRational value = evaluate(point, false);
		int compare = value.signum();
		return strict ? (compare > 0) : (compare >= 0);
	}
}
