//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <steffen.maercker@tu-dresden.de> (TU Dresden)
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

import java.util.Objects;

/**
 * Abstract base class of efficient Iterables over array slices.
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
	 * @param length length of the array
	 */
	protected IterableArray(int fromIndex, int toIndex, int length)
	{
		Objects.checkFromToIndex(fromIndex, toIndex, length);
		this.fromIndex = fromIndex;
		this.toIndex   = toIndex;
	}

	@Override
	public long count()
	{
		return size();
	}

	@Override
	public abstract ArrayIterator<E> iterator();

	/**
	 * Get the number of elements.
	 */
	public int size()
	{
		return Math.max(0, toIndex - fromIndex);
	}



	/**
	 * Generic implementation of an Iterable over an array slice.
	 *
	 * @param <E> type of the array elements
	 */
	public static class Of<E> extends IterableArray<E>
	{
		protected final E[] elements;

		/**
		 * Iterable slice of array over all elements.
		 */
		@SafeVarargs
		public Of(E... elements)
		{
			this(elements,0, elements.length);
		}

		/**
		 * Iterable slice of array in index interval [fromIndex, toIndex).
		 *
		 * @param fromIndex first index, inclusive
		 * @param toIndex last index, exclusive
		 */
		public Of(E[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex, elements.length);
			this.elements = elements;
		}

		@Override
		public ArrayIterator.Of<E> iterator()
		{
			return new ArrayIterator.Of<>(elements, fromIndex, toIndex);
		}
	}



	/**
	 * Primitive specialisation for {@code double} of an Iterable over an array slice.
	 */
	public static class OfInt extends IterableArray<Integer> implements FunctionalPrimitiveIterable.OfInt
	{
		protected final int[] elements;

		/**
		 * Iterable slice of array over all elements.
		 */
		public OfInt(int... elements)
		{
			this(elements, 0, elements.length);
		}

		public OfInt(int[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex, elements.length);
			this.elements = elements;
		}

		@Override
		public ArrayIterator.OfInt iterator()
		{
			return new ArrayIterator.OfInt(elements, fromIndex, toIndex);
		}
	}



	/**
	 * Primitive specialisation for {@code int} of an Iterable over an array slice.
	 */
	public static class OfDouble extends IterableArray<Double> implements FunctionalPrimitiveIterable.OfDouble
	{
		protected final double[] elements;

		/**
		 * Iterable slice of array over all elements.
		 */
		public OfDouble(double... elements)
		{
			this(elements,0, elements.length);
		}

		/**
		 * Iterable slice of array in index interval [fromIndex, toIndex).
		 *
		 * @param fromIndex first index, inclusive
		 * @param toIndex last index, exclusive
		 */
		public OfDouble(double[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex, elements.length);
			this.elements = elements;
		}

		@Override
		public ArrayIterator.OfDouble iterator()
		{
			return new ArrayIterator.OfDouble(elements, fromIndex, toIndex);
		}
	}



	/**
	 * Primitive specialisation for {@code long} of an Iterable over an array slice.
	 */
	public static class OfLong extends IterableArray<Long> implements FunctionalPrimitiveIterable.OfLong
	{
		protected final long[] elements;

		/**
		 * Iterable slice of array over all elements.
		 */
		public OfLong(long... elements)
		{
			this(elements,0, elements.length);
		}

		/**
		 * Iterable slice of array in index interval [fromIndex, toIndex).
		 *
		 * @param fromIndex first index, inclusive
		 * @param toIndex last index, exclusive
		 */
		public OfLong(long[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex, elements.length);
			this.elements = elements;
		}

		@Override
		public ArrayIterator.OfLong iterator()
		{
			return new ArrayIterator.OfLong(elements, fromIndex, toIndex);
		}
	}
}
