//==============================================================================
//	
//	Copyright (c) 2020-
//	Authors:
//	* Steffen Maercker <steffen.maercker@tu-dresden.de> (TU Dresden)
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

package common.iterable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

/**
 * Mutable predicate that tells whether it encounters an element the first time or not.
 *
 * @param <E> the type of the objects this predicate tests
 */
public abstract class Distinct<E>
{
	protected final Set<E> seen = new HashSet<>();

	/**
	 * Answer whether the element has not been seen yet.
	 *
	 * @param element the element to be tested
	 * @return true iff the element has not been seen yet
	 */
	protected boolean isUnseen(E element)
	{
		return seen.add(element);
	}

	/**
	 * Answer the elements that have already been seen.
	 */
	public abstract FunctionalIterable<E> getSeen();



	/**
	 * Mutable predicate for {@code Object} that tells whether it encounters an element the first time or not.
	 * Elements are identified in terms of {@link Object#equals(Object)} which requires to be accompanied by a matching {@link Object#hashCode()} implementation.
	 * This implementation uses a {@code HashSet} to store the elements it has already seen.
	 *
	 * @param <E> the type of the objects this predicate tests
	 */
	public static class Of<E> extends Distinct<E> implements Predicate<E>
	{
		@Override
		public boolean test(E element)
		{
			return super.isUnseen(element);
		}

		@Override
		public FunctionalIterable<E> getSeen()
		{
			return Reducible.extend(seen);
		}

	}



	/**
	 * Mutable predicate for {@code double} that tells whether it encounters an element the first time or not.
	 * Elements are identified in terms of {@link ==} except for {@code NaN} for which all instances are consider equal, although {@code Double.NaN != Double.NaN}.
	 * This implementation uses a {@code HashSet} to store the elements it has already seen.
	 */
	public static class OfDouble extends Distinct<Double> implements DoublePredicate
	{
		boolean zeroSeen = false;

		@Override
		public boolean test(double value)
		{
			if (value == 0.0) {
				// Circumvent HashSet considering +0.0 != -0.0
				if (zeroSeen) {
					return false;
				} else {
					zeroSeen = true;
					seen.add(value);
					return true;
				}
			}
			// HashSet considers two Double.NaN instances to be equal
			return super.isUnseen(value);
		}

		@Override
		public FunctionalPrimitiveIterable.OfDouble getSeen()
		{
			return Reducible.unboxDouble(seen);
		}
	}



	/**
	 * Mutable predicate for {@code int} that tells whether it encounters an element the first time or not.
	 * Elements are identified in terms of {@link ==}.
	 * This implementation uses a {@code HashSet} to store the elements it has already seen.
	 */
	public static class OfInt extends Distinct<Integer> implements IntPredicate
	{
		@Override
		public boolean test(int value)
		{
			return super.isUnseen(value);
		}

		@Override
		public FunctionalPrimitiveIterable.OfInt getSeen()
		{
			return Reducible.unboxInt(seen);
		}
	}



	/**
	 * Mutable predicate for {@code long} that tells whether it encounters an element the first time or not.
	 * Elements are identified in terms of {@link ==}.
	 * This implementation uses a {@code HashSet} to store the elements it has already seen.
	 */
	public static class OfLong extends Distinct<Long> implements LongPredicate
	{
		@Override
		public boolean test(long value)
		{
			return super.isUnseen(value);
		}

		@Override
		public FunctionalPrimitiveIterable.OfLong getSeen()
		{
			return Reducible.unboxLong(seen);
		}
	}
}
