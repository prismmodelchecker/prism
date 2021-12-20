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

import java.util.Iterator;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

/**
 * Abstract base class implementing an Iterator that filters elements by a predicate.
 * Returns only those elements for which the filter predicate evaluates to {@code true}.
 * <p>
 * The calls to {@code next()} of the underlying Iterator happen on-the-fly,
 * i.e., only when {@code next()} is called for this Iterator.
 * <p>
 * Implementations should release the underlying Iterator after iteration.
 * <p>
 * This Iterator does not support the {@code remove()} method, even if the underlying
 * Iterator support it.
 *
 * @param <E> type of the Iterator's elements
 * @param <I> type of the underlying Iterator
 */
public abstract class FilteringIterator<E, I extends Iterator<E>> implements FunctionalIterator<E>
{
	/** The Iterator which elements are filtered */
	protected I iterator;
	/** A flag indicating whether another element exists */
	protected boolean hasNext;

	/**
	 * Constructor for a FilteringIterator without a predicate.
	 *
	 * @param iterator an Iterator to be filtered
	 */
	public FilteringIterator(I iterator)
	{
		Objects.requireNonNull(iterator);
		this.iterator = iterator;
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
	 * Seek and store the next element for which the filter evaluates to {@code true}.
	 */
	protected abstract void seekNext();




	/**
	 * Generic implementation of a FilteringIterator.
	 *
	 * @param <E> type of the Iterable's elements
	 */
	public static class Of<E> extends FilteringIterator<E, Iterator<E>>
	{
		/** The predicate the Iterator uses to filter the elements */
		protected final Predicate<E> filter;
		/** The next element for which the filter predicates evaluates to {@code true} */
		protected E next;

		/**
		 * Constructor for an Iterator that filters elements by a predicate.
		 *
		 * @param iterator an Iterator to be filtered
		 * @param predicate a predicate used to filter the elements
		 */
		@SuppressWarnings("unchecked")
		public Of(Iterator<E> iterator, Predicate<? super E> predicate)
		{
			super(iterator);
			Objects.requireNonNull(predicate);
			this.filter = (Predicate<E>) predicate;
			seekNext();
		}

		@Override
		public E next()
		{
			requireNext();
			E current = next;
			seekNext();
			return current;
		}

		@Override
		public void release()
		{
			super.release();
			iterator = EmptyIterator.of();
			next     = null;
		}

		@Override
		protected void seekNext()
		{
			while (iterator.hasNext()) {
				next = iterator.next();
				if (filter.test(next)) {
					hasNext = true;
					return;
				}
			}
			release();
		}
	}



	/**
	 * Primitive specialisation for {@code double} of a FilteringIterator.
	 */
	public static class OfDouble extends FilteringIterator<Double, PrimitiveIterator.OfDouble> implements FunctionalPrimitiveIterator.OfDouble
	{
		/** The predicate the Iterator uses to filter the elements */
		protected final DoublePredicate filter;
		/** The next element for which the filter predicates evaluates to {@code true} */
		protected double next;

		/**
		 * Constructor for an Iterator that filters elements by a predicate.
		 *
		 * @param iterator an Iterator to be filtered
		 * @param predicate a predicate used to filter the elements
		 */
		public OfDouble(PrimitiveIterator.OfDouble iterator, DoublePredicate predicate)
		{
			super(iterator);
			Objects.requireNonNull(predicate);
			this.filter = predicate;
			seekNext();
		}

		@Override
		public double nextDouble()
		{
			requireNext();
			double current = next;
			seekNext();
			return current;
		}

		@Override
		public void release()
		{
			super.release();
			iterator = EmptyIterator.ofDouble();
			next     = 0.0;
		}

		@Override
		protected void seekNext()
		{
			while (iterator.hasNext()) {
				next = iterator.nextDouble();
				if (filter.test(next)) {
					hasNext = true;
					return;
				}
			}
			release();
		}
	}



	/**
	 * Primitive specialisation for {@code int} of a FilteringIterator.
	 */
	public static class OfInt extends FilteringIterator<Integer, PrimitiveIterator.OfInt> implements FunctionalPrimitiveIterator.OfInt
	{
		/** The predicate the Iterator uses to filter the elements */
		protected final IntPredicate filter;
		/** The next element for which the filter predicates evaluates to {@code true} */
		protected int next;

		/**
		 * Constructor for an Iterator that filters elements by a predicate.
		 *
		 * @param iterator an Iterator to be filtered
		 * @param predicate a predicate used to filter the elements
		 */
		public OfInt(PrimitiveIterator.OfInt iterator, IntPredicate predicate)
		{
			super(iterator);
			Objects.requireNonNull(predicate);
			this.filter = predicate;
			seekNext();
		}

		@Override
		public int nextInt()
		{
			requireNext();
			int current = next;
			seekNext();
			return current;
		}

		@Override
		public void release()
		{
			super.release();
			iterator = EmptyIterator.ofInt();
			next     = 0;
		}

		@Override
		protected void seekNext()
		{
			while (iterator.hasNext()) {
				next = iterator.nextInt();
				if (filter.test(next)) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}
	}



	/**
	 * Primitive specialisation for {@code long} of a FilteringIterator.
	 */
	public static class OfLong extends FilteringIterator<Long, PrimitiveIterator.OfLong> implements FunctionalPrimitiveIterator.OfLong
	{
		/** The predicate the Iterator uses to filter the elements */
		protected final LongPredicate filter;
		/** The next element for which the filter predicates evaluates to {@code true} */
		protected long next;

		/**
		 * Constructor for an Iterator that filters elements by a predicate.
		 *
		 * @param iterator an Iterator to be filtered
		 * @param predicate a predicate used to filter the elements
		 */
		public OfLong(PrimitiveIterator.OfLong iterator, LongPredicate predicate)
		{
			super(iterator);
			Objects.requireNonNull(predicate);
			this.filter = predicate;
			seekNext();
		}

		@Override
		public long nextLong()
		{
			requireNext();
			long current = next;
			seekNext();
			return current;
		}

		@Override
		public void release()
		{
			super.release();
			iterator = EmptyIterator.ofLong();
			next     = 0;
		}

		@Override
		protected void seekNext()
		{
			while (iterator.hasNext()) {
				next = iterator.nextLong();
				if (filter.test(next)) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}
	}
}
