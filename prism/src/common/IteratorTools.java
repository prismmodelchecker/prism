//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <steffen.maercker@tu-dresden.de> (TU Dresden)
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

package common;

import java.util.Iterator;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator.OfDouble;
import java.util.PrimitiveIterator.OfInt;
import java.util.PrimitiveIterator.OfLong;
import java.util.function.Predicate;

import common.iterable.Reducible;

/**
 * Convenience methods for performing operations on Iterators / Iterables
 */
public class IteratorTools
{
	/**
	 * Boolean AND-function: Are all elements true?
	 */
	public static boolean and(Iterator<Boolean> booleans)
	{
		while (booleans.hasNext()) {
			if (!booleans.next()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Boolean OR-function: Is at least one elements true?
	 */
	public static boolean or(Iterator<Boolean> booleans)
	{
		while (booleans.hasNext()) {
			if (booleans.next()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Count the number of elements
	 */
	public static long count(Iterable<?> iterable)
	{
		return Reducible.extend(iterable).count();
	}

	/**
	 * Count the number of elements
	 */
	public static long count(Iterator<?> iterator)
	{
		return Reducible.extend(iterator).count();
	}

	/**
	 * Count the number of elements
	 */
	public static <T> long count(Iterable<T> iterable, Predicate<? super T> predicate)
	{
		return Reducible.extend(iterable).count(predicate);
	}

	/**
	 * Count the number of elements
	 */
	public static <T> long count(Iterator<T> iterator, Predicate<? super T> predicate)
	{
		return Reducible.extend(iterator).count(predicate);
	}

	/**
	 * Maximum over iterator elements
	 */
	public static OptionalDouble max(OfDouble numbers)
	{
		return Reducible.extend(numbers).max();
	}

	/**
	 * Maximum over iterator elements
	 */
	public static OptionalInt max(OfInt numbers)
	{
		return Reducible.extend(numbers).max();
	}

	/**
	 * Maximum over iterator elements
	 */
	public static OptionalLong max(OfLong numbers)
	{
		return Reducible.extend(numbers).max();
	}

	/**
	 * Maximum over non-null iterator elements
	 */
	public static OptionalDouble maxDouble(Iterator<Double> numbers)
	{
		return Reducible.unboxDouble(Reducible.extend(numbers).nonNull()).max();
	}

	/**
	 * Maximum over non-null iterator elements
	 */
	public static OptionalInt maxInt(Iterator<Integer> numbers)
	{
		return Reducible.unboxInt(Reducible.extend(numbers).nonNull()).max();
	}

	/**
	 * Maximum over non-null iterator elements
	 */
	public static OptionalLong maxLong(Iterator<Long> numbers)
	{
		return Reducible.unboxLong(Reducible.extend(numbers).nonNull()).max();
	}

	/**
	 * Minimum over iterator elements
	 */
	public static OptionalDouble min(OfDouble numbers)
	{
		return Reducible.extend(numbers).min();
	}

	/**
	 * Minimum over iterator elements
	 */
	public static OptionalInt min(OfInt numbers)
	{
		return Reducible.extend(numbers).min();
	}

	/**
	 * Minimum over iterator elements
	 */
	public static OptionalLong min(OfLong numbers)
	{
		return Reducible.extend(numbers).min();
	}

	/**
	 * Minimum over non-null iterator elements
	 */
	public static OptionalDouble minDouble(Iterator<Double> numbers)
	{
		return Reducible.unboxDouble(Reducible.extend(numbers).nonNull()).min();
	}

	/**
	 * Minimum over non-null iterator elements
	 */
	public static OptionalInt minInt(Iterator<Integer> numbers)
	{
		return Reducible.unboxInt(Reducible.extend(numbers).nonNull()).min();
	}

	/**
	 * Minimum over non-null iterator elements
	 */
	public static OptionalLong minLong(Iterator<Long> numbers)
	{
		return Reducible.unboxLong(Reducible.extend(numbers).nonNull()).min();
	}

	/**
	 * Sum over iterator elements
	 */
	public static double sum(OfDouble numbers)
	{
		return Reducible.extend(numbers).sum();
	}

	/**
	 * Sum over iterator elements
	 */
	public static long sum(OfInt numbers)
	{
		return Reducible.extend(numbers).sum();
	}

	/**
	 * Sum over iterator elements
	 */
	public static long sum(OfLong numbers)
	{
		return Reducible.extend(numbers).sum();
	}

	/**
	 * Sum over non-null iterator elements
	 */
	public static double sumDouble(Iterator<Double> numbers)
	{
		return Reducible.unboxDouble(Reducible.extend(numbers).nonNull()).sum();
	}

	/**
	 * Sum over non-null iterator elements
	 */
	public static long sumInt(Iterator<Integer> numbers)
	{
		return Reducible.unboxInt(Reducible.extend(numbers).nonNull()).sum();
	}

	/**
	 * Sum over non-null iterator elements
	 */
	public static long sumLong(Iterator<Long> numbers)
	{
		return Reducible.unboxLong(Reducible.extend(numbers).nonNull()).sum();
	}

	/**
	 * Create String "name = {e_1, e_2}".
	 * 
	 * @param name     name of a variable
	 * @param iterator the iterator to be printed
	 */
	public static <T> void printIterator(String name, Iterator<T> iterator)
	{
		System.out.print(name + " = ");
		System.out.print(Reducible.extend(iterator).asString());
		System.out.println();
	}
}
