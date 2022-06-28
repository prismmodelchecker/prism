//==============================================================================
//	
//	Copyright (c) 2016-
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

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Abstract base class for Iterators ranging over a single element.
 *
 * @param <E> type of the Iterator's elements
 */
public abstract class SingletonIterator<E> implements FunctionalIterator<E>
{
	protected boolean hasNext = true;

	@Override
	public SingletonIterator<E> dedupe()
	{
		return this;
	}

	@Override
	public SingletonIterator<E> distinct()
	{
		return this;
	}

	@Override
	public boolean hasNext()
	{
		return hasNext;
	}

	@Override
	public void release()
	{
		hasNext = false;
	}


	/**
	 * Generic implementation of an singleton Iterator.
	 *
	 * @param <E> type of the Iterator's elements
	 */
	public static class Of<E> extends SingletonIterator<E>
	{
		/** The single element */
		protected E element;

		/**
		 * Constructor for an Iterator ranging over a single element.
		 *
		 * @param element the single element of the Iterator
		 */
		public Of(E element)
		{
			this.element = element;
		}

		@Override
		public E next()
		{
			requireNext();
			E next = element;
			release();
			return next;
		}

		@Override
		public void release()
		{
			super.release();
			element = null;
		}
	}



	/**
	 * Primitive specialisation for {@code double} of an singleoton Iterator.
	 */
	public static class OfDouble extends SingletonIterator<Double> implements FunctionalPrimitiveIterator.OfDouble
	{
		/** The single element */
		protected final double element;

		/**
		 * Constructor for an Iterator ranging over a single element.
		 *
		 * @param element the single element of the Iterator
		 */
		public OfDouble(double element)
		{
			this.element = element;
		}

		@Override
		public double nextDouble()
		{
			requireNext();
			release();
			return element;
		}

		@Override
		public SingletonIterator.OfDouble dedupe()
		{
			return this;
		}

		@Override
		public SingletonIterator.OfDouble distinct()
		{
			return this;
		}

		@Override
		public OptionalDouble max()
		{
			if (hasNext) {
				release();
				return OptionalDouble.of(element);
			}
			return OptionalDouble.empty();
		}

		@Override
		public OptionalDouble min()
		{
			return max();
		}

		@Override
		public double sum()
		{
			return hasNext ? element : 0.0;
		}
	}



	/**
	 * Primitive specialisation for {@code int} of an singleoton Iterator.
	 */
	public static class OfInt extends SingletonIterator<Integer> implements FunctionalPrimitiveIterator.OfInt
	{
		/** The single element */
		protected final int element;

		/**
		 * Constructor for an Iterator ranging over a single element.
		 *
		 * @param element the single element of the Iterator
		 */
		public OfInt(int element)
		{
			this.element = element;
		}

		@Override
		public int nextInt()
		{
			requireNext();
			release();
			return element;
		}

		@Override
		public SingletonIterator.OfInt dedupe()
		{
			return this;
		}

		@Override
		public SingletonIterator.OfInt distinct()
		{
			return this;
		}

		@Override
		public OptionalInt max()
		{
			if (hasNext) {
				release();
				return OptionalInt.of(element);
			}
			return OptionalInt.empty();
		}

		@Override
		public OptionalInt min()
		{
			return max();
		}

		@Override
		public long sum()
		{
			return hasNext ? element : 0L;
		}
	}



	/**
	 * Primitive specialisation for {@code long} of an singleoton Iterator.
	 */
	public static class OfLong extends SingletonIterator<Long> implements FunctionalPrimitiveIterator.OfLong
	{
		/** The single element */
		protected final long element;

		/**
		 * Constructor for an Iterator ranging over a single element.
		 *
		 * @param element the single element of the Iterator
		 */
		public OfLong(long element)
		{
			this.element = element;
		}

		@Override
		public long nextLong()
		{
			requireNext();
			release();
			return element;
		}

		@Override
		public SingletonIterator.OfLong dedupe()
		{
			return this;
		}

		@Override
		public SingletonIterator.OfLong distinct()
		{
			return this;
		}

		@Override
		public OptionalLong max()
		{
			if (hasNext) {
				release();
				return OptionalLong.of(element);
			}
			return OptionalLong.empty();
		}

		@Override
		public OptionalLong min()
		{
			return max();
		}

		@Override
		public long sum()
		{
			return hasNext ? element : 0L;
		}
	}
}