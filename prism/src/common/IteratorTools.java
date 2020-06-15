//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
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

import common.iterable.FunctionalIterator;
import common.iterable.MappingIterator;

/**
 * Convenience methods for performing operations on Iterators / Iterables
 */
public class IteratorTools
{
	/**
	 * Boolean AND-function: Are all elements true?
	 */
	public static boolean and(final Iterator<Boolean> booleans)
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
	public static boolean or(final Iterator<Boolean> booleans)
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
	public static int count(final Iterable<?> iterable)
	{
		return FunctionalIterator.extend(iterable).count();
	}

	/**
	 * Count the number of elements
	 */
	public static int count(final Iterator<?> iterator)
	{
		return FunctionalIterator.extend(iterator).count();
	}

	/**
	 * Count the number of elements
	 */
	public static <T> int count(final Iterable<T> iterable, final Predicate<? super T> predicate)
	{
		return FunctionalIterator.extend(iterable).count(predicate);
	}

	/**
	 * Count the number of elements
	 */
	public static <T> int count(final Iterator<T> iterator, final Predicate<? super T> predicate)
	{
		return FunctionalIterator.extend(iterator).count(predicate);
	}

	/**
	 * Maximum over iterator elements
	 */
	public static OptionalDouble max(final OfDouble numbers)
	{
		return FunctionalIterator.extend(numbers).max();
	}

	/**
	 * Maximum over iterator elements
	 */
	public static OptionalInt max(final OfInt numbers)
	{
		return FunctionalIterator.extend(numbers).max();
	}

	/**
	 * Maximum over iterator elements
	 */
	public static OptionalLong max(final OfLong numbers)
	{
		return FunctionalIterator.extend(numbers).max();
	}

	/**
	 * Maximum over non-null iterator elements
	 */
	public static OptionalDouble maxDouble(final Iterator<Double> numbers)
	{
		return MappingIterator.toDouble(FunctionalIterator.extend(numbers).nonNull()).max();
	}

	/**
	 * Maximum over non-null iterator elements
	 */
	public static OptionalInt maxInt(final Iterator<Integer> numbers)
	{
		return MappingIterator.toInt(FunctionalIterator.extend(numbers).nonNull()).max();
	}

	/**
	 * Maximum over non-null iterator elements
	 */
	public static OptionalLong maxLong(final Iterator<Long> numbers)
	{
		return MappingIterator.toLong(FunctionalIterator.extend(numbers).nonNull()).max();
	}

	/**
	 * Minimum over iterator elements
	 */
	public static OptionalDouble min(final OfDouble numbers)
	{
		return FunctionalIterator.extend(numbers).min();
	}

	/**
	 * Minimum over iterator elements
	 */
	public static OptionalInt min(final OfInt numbers)
	{
		return FunctionalIterator.extend(numbers).min();
	}

	/**
	 * Minimum over iterator elements
	 */
	public static OptionalLong min(final OfLong numbers)
	{
		return FunctionalIterator.extend(numbers).min();
	}

	/**
	 * Minimum over non-null iterator elements
	 */
	public static OptionalDouble minDouble(final Iterator<Double> numbers)
	{
		return MappingIterator.toDouble(FunctionalIterator.extend(numbers).nonNull()).min();
	}

	/**
	 * Minimum over non-null iterator elements
	 */
	public static OptionalInt minInt(final Iterator<Integer> numbers)
	{
		return MappingIterator.toInt(FunctionalIterator.extend(numbers).nonNull()).min();
	}

	/**
	 * Minimum over non-null iterator elements
	 */
	public static OptionalLong minLong(final Iterator<Long> numbers)
	{
		return MappingIterator.toLong(FunctionalIterator.extend(numbers).nonNull()).min();
	}

	/**
	 * Sum over iterator elements
	 */
	public static double sum(final OfDouble numbers)
	{
		return FunctionalIterator.extend(numbers).sum();
	}

	/**
	 * Sum over iterator elements
	 */
	public static int sum(final OfInt numbers)
	{
		return FunctionalIterator.extend(numbers).sum();
	}

	/**
	 * Sum over iterator elements
	 */
	public static long sum(final OfLong numbers)
	{
		return FunctionalIterator.extend(numbers).sum();
	}

	/**
	 * Sum over non-null iterator elements
	 */
	public static double sumDouble(final Iterator<Double> numbers)
	{
		return MappingIterator.toDouble(FunctionalIterator.extend(numbers).nonNull()).sum();
	}

	/**
	 * Sum over non-null iterator elements
	 */
	public static int sumInt(final Iterator<Integer> numbers)
	{
		return MappingIterator.toInt(FunctionalIterator.extend(numbers).nonNull()).sum();
	}

	/**
	 * Sum over non-null iterator elements
	 */
	public static long sumLong(final Iterator<Long> numbers)
	{
		return MappingIterator.toLong(FunctionalIterator.extend(numbers).nonNull()).sum();
	}

	/**
	 * Create String "name = {e_1, e_2}".
	 * 
	 * @param name     name of a variable
	 * @param iterator the iterator to be printed
	 */
	public static <T> void printIterator(final String name, final Iterator<T> iterator)
	{
		System.out.print(name + " = ");
		System.out.print(FunctionalIterator.extend(iterator).asString());
		System.out.println();
	}
}
