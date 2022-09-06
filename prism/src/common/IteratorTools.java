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
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

import common.iterable.FilteringIterator;
import common.iterable.MappingIterator;

import java.util.PrimitiveIterator.OfDouble;
import java.util.PrimitiveIterator.OfInt;
import java.util.PrimitiveIterator.OfLong;

/** Convenience methods for performing operations on Iterators / Iterables */
public class IteratorTools
{
	/** Count the number of elements */
	public static int count(final Iterable<?> iterable)
	{
		return count(iterable.iterator());
	}

	/** Count the number of elements */
	public static int count(final Iterator<?> iterator)
	{
		if (iterator instanceof OfInt) {
			return count((OfInt) iterator);
		}
		if (iterator instanceof OfLong) {
			return count((OfLong) iterator);
		}
		if (iterator instanceof OfDouble) {
			return count((OfDouble) iterator);
		}

		int count=0;
		for (; iterator.hasNext(); iterator.next()) {
			count++;
		}
		return count;
	}

	/** Count the number of elements */
	public static int count(final OfInt iterator)
	{
		// call nextInt to avoid unnecessary boxing
		int count=0;
		for (; iterator.hasNext(); iterator.nextInt()) {
			count++;
		}
		return count;
	}

	/** Count the number of elements */
	public static int count(final OfLong iterator)
	{
		// call nextLong to avoid unnecessary boxing
		int count=0;
		for (; iterator.hasNext(); iterator.nextLong()) {
			count++;
		}
		return count;
	}

	/** Count the number of elements */
	public static int count(final OfDouble iterator)
	{
		// call nextDouble to avoid unnecessary boxing
		int count=0;
		for (; iterator.hasNext(); iterator.nextDouble()) {
			count++;
		}
		return count;
	}

	/** Count the number of elements matching the predicate */
	public static <T> int count(final Iterable<T> iterable, final Predicate<? super T> predicate)
	{
		return count(iterable.iterator(), predicate);
	}

	/** Count the number of elements matching the predicate */
	public static <T> int count(final Iterator<T> iterator, final Predicate<? super T> predicate)
	{
		if (iterator instanceof OfInt && predicate instanceof IntPredicate) {
			return count(new FilteringIterator.OfInt((OfInt) iterator, (IntPredicate) predicate));
		}
		if (iterator instanceof OfLong && predicate instanceof LongPredicate) {
			return count(new FilteringIterator.OfLong((OfLong) iterator, (LongPredicate) predicate));
		}
		if (iterator instanceof OfDouble && predicate instanceof DoublePredicate) {
			return count(new FilteringIterator.OfDouble((OfDouble) iterator, (DoublePredicate) predicate));
		}

		return count(new FilteringIterator.Of<>(iterator, predicate));
	}

	/** Sum over iterator elements */
	public static int sumInt(final Iterator<Integer> numbers)
	{
		return sum(MappingIterator.toInt(numbers));
	}

	/** Sum over iterator elements */
	public static int sum(final OfInt numbers)
	{
		int sum = 0;
		while (numbers.hasNext()) {
			sum += numbers.nextInt();
		}
		return sum;
	}

	/** Sum over iterator elements */
	public static long sumLong(final Iterator<Long> numbers)
	{
		return sum(MappingIterator.toLong(numbers));
	}

	/** Sum over iterator elements */
	public static long sum(final OfLong numbers)
	{
		long sum = 0;
		while (numbers.hasNext()) {
			sum += numbers.nextLong();
		}
		return sum;
	}

	/** Sum over iterator elements */
	public static double sumDouble(final Iterator<Double> numbers)
	{
		return sum(MappingIterator.toDouble(numbers));
	}

	/** Sum over iterator elements */
	public static double sum(final OfDouble numbers)
	{
		double sum = 0;
		while (numbers.hasNext()) {
			sum += numbers.nextDouble();
		}
		return sum;
	}

}
