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

package common.iterable;

import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Abstract base class for empty Iterators.
 * The four natural implementations (generic, double, int, and long)
 * are implemented as Singletons and their instances accessible by a static method.
 *
 * @param <E> type of the Iterator's elements
 */
public abstract class EmptyIterator<E> implements FunctionalIterator<E>
{
	/** Singleton instance elements of a generic type */
	private static final Of<?>    OF        = new Of<>();
	/** Singleton instance for {@code double} elements */
	private static final OfDouble OF_DOUBLE = new OfDouble();
	/** Singleton instance for {@code int} elements */
	private static final OfInt    OF_INT    = new OfInt();
	/** Singleton instance for {@code long} elements*/
	private static final OfLong   OF_LONG   = new OfLong();

	/**
	 * Get unique instance for elements of a generic type.
	 *
	 * @param <E> type of the Iterator's elements
	 */
	@SuppressWarnings("unchecked")
	public static <E> Of<E> Of()
	{
		return (Of<E>) OF;
	}

	/**
	 * Get unique instance for {@code double} elements.
	 */
	public static OfDouble OfDouble()
	{
		return OF_DOUBLE;
	}

	/**
	 * Get unique instance for {@code int} elements.
	 */
	public static OfInt OfInt()
	{
		return OF_INT;
	}

	/**
	 * Get unique instance for {@code long} elements.
	 */
	public static OfLong OfLong()
	{
		return OF_LONG;
	}

	@Override
	public boolean hasNext()
	{
		return false;
	}

	@Override
	public EmptyIterator<E> dedupe()
	{
		return this;
	}

	@Override
	public EmptyIterator<E> distinct()
	{
		return this;
	}



	/**
	 * Generic implementation of an empty Iterator as Singleton.
	 *
	 * @param <E> type of the Iterator's elements
	 */
	public static class Of<E> extends EmptyIterator<E>
	{
		/**
		 * Private constructor for the Singleton instance.
		 */
		private Of() {};

		@Override
		public E next()
		{
			throw new NoSuchElementException();
		}
	}



	/**
	 * Primitive specialisation for {@code double} of an empty Iterator as Singleton.
	 */
	public static class OfDouble extends EmptyIterator<Double> implements FunctionalPrimitiveIterator.OfDouble
	{
		/**
		 * Private constructor for the Singleton instance.
		 */
		private OfDouble() {};

		@Override
		public double nextDouble()
		{
			throw new NoSuchElementException();
		}

		@Override
		public EmptyIterator.OfDouble dedupe()
		{
			return this;
		}

		@Override
		public EmptyIterator.OfDouble distinct()
		{
			return this;
		}

		@Override
		public OptionalDouble max()
		{
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
			return 0.0;
		}
	}



	/**
	 * Primitive specialisation for {@code int} of an empty Iterator as Singleton.
	 */
	public static class OfInt extends EmptyIterator<Integer> implements FunctionalPrimitiveIterator.OfInt
	{
		/**
		 * Private constructor for the Singleton instance.
		 */
		private OfInt() {};

		@Override
		public int nextInt()
		{
			throw new NoSuchElementException();
		}

		@Override
		public EmptyIterator.OfInt dedupe()
		{
			return this;
		}

		@Override
		public EmptyIterator.OfInt distinct()
		{
			return this;
		}

		@Override
		public OptionalInt max()
		{
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
			return 0;
		}
	}



	/**
	 * Primitive specialisation for {@code long} of an empty Iterator as Singleton.
	 */
	public static class OfLong extends EmptyIterator<Long> implements FunctionalPrimitiveIterator.OfLong
	{
		/**
		 * Private constructor for the Singleton instance.
		 */
		private OfLong() {};

		@Override
		public long nextLong()
		{
			throw new NoSuchElementException();
		}

		@Override
		public EmptyIterator.OfLong dedupe()
		{
			return this;
		}

		@Override
		public EmptyIterator.OfLong distinct()
		{
			return this;
		}

		@Override
		public OptionalLong max()
		{
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
			return 0;
		}
	}
}
