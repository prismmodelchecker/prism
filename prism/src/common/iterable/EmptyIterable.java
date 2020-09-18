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

/**
 * Abstract base class for empty Iterables.
 * The four natural implementations (generic, double, int, and long)
 * are implemented as Singletons and their instances accessible by a static method.
 *
 * @param <E> type of the Iterable's elements
 */
public abstract class EmptyIterable<E> implements FunctionalIterable<E>
{
	/**
	 * Get unique instance for elements of a generic type.
	 *
	 * @param <E> type of the Iterable's elements
	 */
	@SuppressWarnings("unchecked")
	public static <E> Of<E> of()
	{
		return (Of<E>) Of.OF;
	}

	/**
	 * Get unique instance for {@code double} elements.
	 */
	public static OfDouble ofDouble()
	{
		return OfDouble.OF_DOUBLE;
	}

	/**
	 * Get unique instance for {@code int} elements.
	 */
	public static OfInt ofInt()
	{
		return OfInt.OF_INT;
	}

	/**
	 * Get unique instance for {@code long} elements.
	 */
	public static OfLong ofLong()
	{
		return OfLong.OF_LONG;
	}



	/**
	 * Generic implementation of an empty Iterable as Singleton.
	 *
	 * @param <E> type of the Iterable's elements
	 */
	public static class Of<E> extends EmptyIterable<E>
	{
		/** Singleton instance elements of a generic type */
		private static final Of<?> OF = new Of<>();

		/**
		 * Private constructor for the Singleton instance.
		 */
		private Of() {}

		@Override
		public EmptyIterator.Of<E> iterator()
		{
			return EmptyIterator.of();
		}
	}



	/**
	 * Primitive specialisation for {@code double} of an empty Iterable as Singleton.
	 */
	public static class OfDouble extends EmptyIterable<Double> implements FunctionalPrimitiveIterable.OfDouble
	{
		/** Singleton instance for {@code double} elements */
		private static final EmptyIterable.OfDouble OF_DOUBLE = new EmptyIterable.OfDouble();

		/**
		 * Private constructor for the Singleton instance.
		 */
		private OfDouble() {}
	
		@Override
		public EmptyIterator.OfDouble iterator()
		{
			return EmptyIterator.ofDouble();
		}
	}



	/**
	 * Primitive specialisation for {@code int} of an empty Iterable as Singleton.
	 */
	public static class OfInt extends EmptyIterable<Integer> implements FunctionalPrimitiveIterable.OfInt
	{
		/** Singleton instance for {@code int} elements */
		private static final EmptyIterable.OfInt OF_INT = new EmptyIterable.OfInt();

		/**
		 * Private constructor for the Singleton instance.
		 */
		private OfInt() {}

		@Override
		public EmptyIterator.OfInt iterator()
		{
			return EmptyIterator.ofInt();
		}
	}



	/**
	 * Primitive specialisation for {@code long} of an empty Iterable as Singleton.
	 */
	public static class OfLong extends EmptyIterable<Long> implements FunctionalPrimitiveIterable.OfLong
	{
		/** Singleton instance for {@code long} elements*/
		private static final EmptyIterable.OfLong OF_LONG = new EmptyIterable.OfLong();

		/**
		 * Private constructor for the Singleton instance.
		 */
		private OfLong() {}

		@Override
		public EmptyIterator.OfLong iterator()
		{
			return EmptyIterator.ofLong();
		}
	}
}