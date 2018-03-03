//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Marcus Daum <marcus.daum@ivi.fraunhofer.de> (Frauenhofer Institut)
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

import common.iterable.FunctionalPrimitiveIterable.IterableDouble;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterable.IterableLong;

/**
 * Abstract base class of efficient iterables over array slices.
 *
 * @param <E> type of the array elements
 */
public abstract class IterableArray<E> implements FunctionalIterable<E>
{
	protected final int fromIndex;
	protected final int toIndex;

	/**
	 * Iterable slice of array in index interval [fromIndex, toIndex).
	 *
	 * @param fromIndex first index, inclusive
	 * @param toIndex last index, exclusive
	 */
	public IterableArray(int fromIndex, int toIndex)
	{
		if (fromIndex < 0) {
			throw new IllegalArgumentException("non-negative fromIndex expected, got: " + fromIndex);
		}
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
	}

	public int size()
	{
		return Math.max(0, toIndex - fromIndex);
	}



	/**
	 * Generic implementation of an iterable over an array slice.
	 *
	 * @param <E> type of the array elements
	 */
	public static class Of<E> extends IterableArray<E>
	{
		protected final E[] elements;

		@SafeVarargs
		public Of(E... elements)
		{
			super(0, elements.length);
			this.elements = elements;
		}

		public Of(E[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex);
			this.elements = elements;
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			return new ArrayIterator.Of<>(elements, fromIndex, toIndex);
		}
	}



	/**
	 * Primitive specialisation for {@code double} of an iterable over an array slice.
	 */
	public static class OfInt extends IterableArray<Integer> implements IterableInt
	{
		protected final int[] elements;

		@SafeVarargs
		public OfInt(int... elements)
		{
			super(0, elements.length);
			this.elements = elements;
		}

		public OfInt(int[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex);
			this.elements = elements;
		}

		@Override
		public ArrayIterator.OfInt iterator()
		{
			return new ArrayIterator.OfInt(elements, fromIndex, toIndex);
		}
	}



	/**
	 * Primitive specialisation for {@code int} of an iterable over an array slice.
	 */
	public static class OfDouble extends IterableArray<Double> implements IterableDouble
	{
		protected final double[] elements;

		@SafeVarargs
		public OfDouble(double... elements)
		{
			super(0, elements.length);
			this.elements = elements;
		}

		public OfDouble(double[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex);
			this.elements = elements;
		}

		@Override
		public ArrayIterator.OfDouble iterator()
		{
			return new ArrayIterator.OfDouble(elements, fromIndex, toIndex);
		}
	}



	/**
	 * Primitive specialisation for {@code long} of an iterable over an array slice.
	 */
	public static class OfLong extends IterableArray<Long> implements IterableLong
	{
		protected final long[] elements;

		@SafeVarargs
		public OfLong(long... elements)
		{
			super(0, elements.length);
			this.elements = elements;
		}

		public OfLong(long[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex);
			this.elements = elements;
		}

		@Override
		public ArrayIterator.OfLong iterator()
		{
			return new ArrayIterator.OfLong(elements, fromIndex, toIndex);
		}
	}
}
