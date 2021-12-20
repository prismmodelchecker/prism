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

import java.util.Objects;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

/**
 * Abstract base class for Iterables that filter elements by a predicate.
 * Returns only those elements for which the filter predicate evaluates to {@code true}.
 *
 * @param <E> type of the Iterable's elements
 * @param <I> type of the underlying Iterable
 */
public abstract class FilteringIterable<E, I extends Iterable<E>> implements FunctionalIterable<E>
{
	/** The Iterable which elements are filtered */
	protected final I iterable;

	/**
	 * Constructor for a filtering Iterable without a predicate.
	 *
	 * @param iterable an Iterable to be filtered
	 */
	public FilteringIterable(I iterable)
	{
		Objects.requireNonNull(iterable);
		this.iterable = iterable;
	}



	/**
	 * Generic implementation of a filtering Iterable.
	 *
	 * @param <E> type of the Iterable's elements
	 */
	public static class Of<E> extends FilteringIterable<E, Iterable<E>>
	{
		/** The predicate the Iterable uses to filter the elements */
		protected final Predicate<? super E> filter;

		/**
		 * Constructor for an Iterable that filters elements by a predicate.
		 * <p>
		 * Attention! If the predicate is <em>stateful</em>, subsequent iterations may yield different elements.
		 *
		 * @param iterable an Iterable to be filtered
		 * @param predicate a predicate used to filter the elements
		 */
		public Of(Iterable<E> iterable, Predicate<? super E> predicate)
		{
			super(iterable);
			Objects.requireNonNull(predicate);
			this.filter = predicate;
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			return new FilteringIterator.Of<>(iterable.iterator(), filter);
		}
	}



	/**
	 * Primitive specialisation for {@code double} of a filtering Iterable.
	 */
	public static class OfDouble extends FilteringIterable<Double, PrimitiveIterable.OfDouble> implements FunctionalPrimitiveIterable.OfDouble
	{
		/** The predicate the Iterable uses to filter the elements */
		protected final DoublePredicate filter;

		/**
		 * Constructor for an Iterable that filters elements by a predicate.
		 * <p>
		 * Attention! If the predicate is <em>stateful</em>, subsequent iterations may yield different elements.
		 *
		 * @param iterable an Iterable to be filtered
		 * @param predicate a predicate used to filter the elements
		 */
		public OfDouble(PrimitiveIterable.OfDouble iterable, DoublePredicate predicate)
		{
			super(iterable);
			Objects.requireNonNull(predicate);
			this.filter = predicate;
		}

		@Override
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new FilteringIterator.OfDouble(iterable.iterator(), filter);
		}
	}



	/**
	 * Primitive specialisation for {@code int} of a filtering Iterable.
	 */
	public static class OfInt extends FilteringIterable<Integer, PrimitiveIterable.OfInt> implements FunctionalPrimitiveIterable.OfInt
	{
		/** The predicate the Iterable uses to filter the elements */
		protected final IntPredicate filter;

		/**
		 * Constructor for an Iterable that filters elements by a predicate.
		 * <p>
		 * Attention! If the predicate is <em>stateful</em>, subsequent iterations may yield different elements.
		 *
		 * @param iterable an Iterable to be filtered
		 * @param predicate a predicate used to filter the elements
		 */
		public OfInt(PrimitiveIterable.OfInt iterable, IntPredicate predicate)
		{
			super(iterable);
			Objects.requireNonNull(predicate);
			this.filter = predicate;
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return new FilteringIterator.OfInt(iterable.iterator(), filter);
		}
	}



	/**
	 * Primitive specialisation for {@code long} of a filtering Iterable.
	 */
	public static class OfLong extends FilteringIterable<Long, PrimitiveIterable.OfLong> implements FunctionalPrimitiveIterable.OfLong
	{
		/** The predicate the Iterable uses to filter the elements */
		protected final LongPredicate filter;

		/**
		 * Constructor for an Iterable that filters elements by a predicate.
		 * <p>
		 * Attention! If the predicate is <em>stateful</em>, subsequent iterations may yield different elements.
		 *
		 * @param iterable an Iterable to be filtered
		 * @param predicate a predicate used to filter the elements
		 */
		public OfLong(PrimitiveIterable.OfLong iterable, LongPredicate predicate)
		{
			super(iterable);
			Objects.requireNonNull(predicate);
			this.filter = predicate;
		}

		@Override
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return new FilteringIterator.OfLong(iterable.iterator(), filter);
		}
	}
}
