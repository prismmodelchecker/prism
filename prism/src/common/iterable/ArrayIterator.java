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
 * Abstract base class of efficient Iterators over array slices.
 * Implementations should release the underlying array after iteration.
 *
 * @param <E> type of the array elements
 */
public abstract class ArrayIterator<E> implements FunctionalIterator<E>
{
	protected final int toIndex;
	protected int nextIndex;

	/**
	 * Iterate over all elements in index interval [fromIndex, toIndex).
	 *
	 * @param fromIndex first index, inclusive
	 * @param toIndex last index, exclusive
	 * @param length length of the array
	 */
	protected ArrayIterator(int fromIndex, int toIndex, int length)
	{
		Objects.checkFromToIndex(fromIndex, toIndex, length);
		this.nextIndex = fromIndex;
		this.toIndex   = toIndex;
	}

	@Override
	public boolean hasNext()
	{
		if (nextIndex < toIndex) {
			return true;
		}
		release();
		return false;
	}

	@Override
	public long count()
	{
		return Math.max(0, toIndex - nextIndex);
	}

	/**
	 * Get the index of the next element.
	 */
	public int nextIndex()
	{
		requireNext();
		return nextIndex;
	}

	/**
	 * Release reference to the underlying array.
	 */
	@Override
	public void release()
	{
		nextIndex = toIndex;
	}

	/**
	 * Generic implementation of an Iterator over an array slice.
	 *
	 * @param <E> type of the array elements
	 */
	public static class Of<E> extends ArrayIterator<E>
	{
		/** Placeholder array after Iterator is exhausted */
		public static final Object[] EMPTY_OBJECT = new Object[0];

		protected E[] elements;

		/**
		 * Iterate over all elements.
		 */
		@SafeVarargs
		public Of(E... elements)
		{
			this(elements, 0, elements.length);
		}

		/**
		 * Iterate over all elements in index interval [fromIndex, toIndex).
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
		public E next()
		{
			requireNext();
			return elements[nextIndex++];
		}

		@SuppressWarnings("unchecked")
		@Override
		public void release()
		{
			super.release();
			elements = (E[]) EMPTY_OBJECT;
		}
	}



	/**
	 * Primitive specialisation for {@code double} of an Iterator over an array slice.
	 */
	public static class OfDouble extends ArrayIterator<Double> implements FunctionalPrimitiveIterator.OfDouble
	{
		/** Placeholder array after Iterator is exhausted */
		public static final double[] EMPTY_DOUBLE = new double[0];

		protected double[] elements;

		/**
		 * Iterate over all elements.
		 */
		public OfDouble(double... elements)
		{
			this(elements, 0, elements.length);
		}

		/**
		 * Iterate over all elements in index interval [fromIndex, toIndex).
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
		public double nextDouble()
		{
			requireNext();
			return elements[nextIndex++];
		}

		@Override
		public void release()
		{
			super.release();
			elements = EMPTY_DOUBLE;
		}
	}



	/**
	 * Primitive specialisation for {@code int} of an Iterator over an array slice.
	 */
	public static class OfInt extends ArrayIterator<Integer> implements FunctionalPrimitiveIterator.OfInt
	{
		/** Placeholder array after Iterator is exhausted */
		public static final int[] EMPTY_INT = new int[0];

		protected int[] elements;

		/**
		 * Iterate over all elements.
		 */
		public OfInt(int... elements)
		{
			this(elements, 0, elements.length);
		}

		/**
		 * Iterate over all elements in index interval [fromIndex, toIndex).
		 *
		 * @param fromIndex first index, inclusive
		 * @param toIndex last index, exclusive
		 */
		public OfInt(int[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex, elements.length);
			this.elements = elements;
		}

		@Override
		public int nextInt()
		{
			requireNext();
			return elements[nextIndex++];
		}

		@Override
		public void release()
		{
			super.release();
			elements = EMPTY_INT;
		}
	}



	/**
	 * Primitive specialisation for {@code long} of an Iterator over an array slice.
	 */
	public static class OfLong extends ArrayIterator<Long> implements FunctionalPrimitiveIterator.OfLong
	{
		/** Placeholder array after Iterator is exhausted */
		public static final long[] EMPTY_LONG = new long[0];

		protected long[] elements;

		/**
		 * Iterate over all elements.
		 */
		public OfLong(long... elements)
		{
			this(elements, 0, elements.length);
		}

		/**
		 * Iterate over all elements in index interval [fromIndex, toIndex).
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
		public long nextLong()
		{
			requireNext();
			return elements[nextIndex++];
		}

		@Override
		public void release()
		{
			super.release();
			elements = EMPTY_LONG;
		}
	}
}
