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

/**
 * Abstract base class for Iterables ranging over a single element.
 *
 * @param <E> type of the Iterable's elements
 */
public abstract class SingletonIterable<E> implements FunctionalIterable<E>
{
	@Override
	public long count()
	{
		return 1;
	}

	@Override
	public boolean isEmpty()
	{
		return false;
	}

	@Override
	public SingletonIterable<E> dedupe()
	{
		return this;
	}

	@Override
	public SingletonIterable<E> distinct()
	{
		return this;
	}



	/**
	 * Generic implementation of an singleton Iterable.
	 *
	 * @param <E> type of the Iterables's elements
	 */
	public static class Of<E> extends SingletonIterable<E>
	{
		/** The single element */
		protected final E element;

		/**
		 * Constructor for an Iterable ranging over a single element.
		 *
		 * @param element the single element of the Iterable
		 */
		public Of(E theElement)
		{
			element = theElement;
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			return new SingletonIterator.Of<>(element);
		}

		@Override
		public boolean contains(Object o)
		{
			return element == null ? o == null : element.equals(o);
		}
	}



	/**
	 * Primitive specialisation for {@code double} of an singleoton Iterable.
	 */
	public static class OfDouble extends SingletonIterable<Double> implements FunctionalPrimitiveIterable.OfDouble
	{
		/** The single element */
		protected final double element;

		/**
		 * Constructor for an Iterable ranging over a single element.
		 *
		 * @param element the single element of the Iterable
		 */
		public OfDouble(double theElement)
		{
			element = theElement;
		}

		@Override
		public SingletonIterator.OfDouble iterator()
		{
			return new SingletonIterator.OfDouble(element);
		}

		@Override
		public SingletonIterable.OfDouble dedupe()
		{
			return this;
		}

		@Override
		public SingletonIterable.OfDouble distinct()
		{
			return this;
		}

		@Override
		public boolean contains(double d)
		{
			return element == d;
		}
	}



	/**
	 * Primitive specialisation for {@code int} of an singleoton Iterable.
	 */
	public static class OfInt extends SingletonIterable<Integer> implements FunctionalPrimitiveIterable.OfInt
	{
		/** The single element */
		protected final int element;

		/**
		 * Constructor for an Iterable ranging over a single element.
		 *
		 * @param element the single element of the Iterable
		 */
		public OfInt(int theElement)
		{
			element = theElement;
		}

		@Override
		public SingletonIterator.OfInt iterator()
		{
			return new SingletonIterator.OfInt(element);
		}

		@Override
		public SingletonIterable.OfInt dedupe()
		{
			return this;
		}

		@Override
		public SingletonIterable.OfInt distinct()
		{
			return this;
		}

		@Override
		public boolean contains(int i)
		{
			return element == i;
		}
	}



	/**
	 * Primitive specialisation for {@code long} of an singleoton Iterable.
	 */
	public static class OfLong extends SingletonIterable<Long> implements FunctionalPrimitiveIterable.OfLong
	{
		/** The single element */
		protected final long element;

		/**
		 * Constructor for an Iterable ranging over a single element.
		 *
		 * @param element the single element of the Iterable
		 */
		public OfLong(long theElement)
		{
			element = theElement;
		}

		@Override
		public SingletonIterator.OfLong iterator()
		{
			return new SingletonIterator.OfLong(element);
		}

		@Override
		public SingletonIterable.OfLong dedupe()
		{
			return this;
		}

		@Override
		public SingletonIterable.OfLong distinct()
		{
			return this;
		}

		@Override
		public boolean contains(long l)
		{
			return element == l;
		}
	}
}
