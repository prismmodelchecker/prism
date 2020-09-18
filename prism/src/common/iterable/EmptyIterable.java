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

import common.iterable.FunctionalPrimitiveIterable.IterableDouble;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterable.IterableLong;

/**
 * Abstract base class for empty Iterables.
 * The four natural implementations (generic, double, int, and long)
 * are implemented as Singletons and their instances accessible by a static method.
 *
 * @param <E> type of the Iterable's elements
 */
public abstract class EmptyIterable<E> implements FunctionalIterable<E>
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
	 * @param <E> type of the Iterable's elements
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



	/**
	 * Generic implementation of an empty Iterable as Singleton.
	 *
	 * @param <E> type of the Iterable's elements
	 */
	public static class Of<E> extends EmptyIterable<E>
	{
		/**
		 * Private constructor for the Singleton instance.
		 */
		private Of() {};

		@Override
		public EmptyIterator.Of<E> iterator()
		{
			return EmptyIterator.Of();
		}
	}



	/**
	 * Primitive specialisation for {@code double} of an empty Iterable as Singleton.
	 */
	public static class OfDouble extends EmptyIterable<Double> implements IterableDouble
	{
		/**
		 * Private constructor for the Singleton instance.
		 */
		private OfDouble() {};
	
		@Override
		public EmptyIterator.OfDouble iterator()
		{
			return EmptyIterator.OfDouble();
		}
	}



	/**
	 * Primitive specialisation for {@code int} of an empty Iterable as Singleton.
	 */
	public static class OfInt extends EmptyIterable<Integer> implements IterableInt
	{
		/**
		 * Private constructor for the Singleton instance.
		 */
		private OfInt() {};

		@Override
		public EmptyIterator.OfInt iterator()
		{
			return EmptyIterator.OfInt();
		}
	}



	/**
	 * Primitive specialisation for {@code long} of an empty Iterable as Singleton.
	 */
	public static class OfLong extends EmptyIterable<Long> implements IterableLong
	{
		/**
		 * Private constructor for the Singleton instance.
		 */
		private OfLong() {};

		@Override
		public EmptyIterator.OfLong iterator()
		{
			return EmptyIterator.OfLong();
		}
	}
}