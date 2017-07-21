//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

/**
 * Base class for filtering iterators,
 * static helpers for common filtering task (deduping).
 */
public abstract class FilteringIterator<T> implements Iterator<T>
{
	protected final Iterator<T> iterator;
	protected boolean hasNext;

	public static <T> Iterator<T> nonNull(Iterable<T> iterable)
	{
		return nonNull(iterable.iterator());
	}

	public static <T> Iterator<T> nonNull(Iterator<T> iterator)
	{
		if (iterator instanceof PrimitiveIterator) {
			return iterator;
		}
		return new FilteringIterator.Of<>(iterator, Objects::nonNull);
	}

	public FilteringIterator(final Iterable<T> iterable)
	{
		this(iterable.iterator());
	}

	public FilteringIterator(final Iterator<T> iterator)
	{
		this.iterator = iterator;
	}

	@Override
	public boolean hasNext()
	{
		return hasNext;
	}

	protected void requireNext()
	{
		if (!hasNext) {
			throw new NoSuchElementException();
		}
	}

	/**
	 * Obtain filtering iterator for the given iterator,
	 * filtering duplicate elements (via HashSet, requires
	 * that {@code equals()} and {@code hashCode()} are properly
	 * implemented).
	 */
	public static <T> Iterator<T> dedupe(final Iterator<T> iterator)
	{
		final Set<T> elements = new HashSet<>();
		return new FilteringIterator.Of<>(iterator, (Predicate<T>) elements::add);
	}

	/**
	 * Obtain filtering iterator for the given primitive int iterator,
	 * filtering duplicate elements (via HashSet).
	 */
	public static OfInt dedupe(final PrimitiveIterator.OfInt iterator)
	{
		// TODO: use BitSet? Evaluate performance in practice...
		final Set<Integer> elements = new HashSet<>();
		return new FilteringIterator.OfInt(iterator, (IntPredicate) elements::add);
	}

	/**
	 * Obtain filtering iterator for the given primitive long iterator,
	 * filtering duplicate elements (via HashSet).
	 */
	public static OfLong dedupe(final PrimitiveIterator.OfLong iterator)
	{
		final Set<Long> elements = new HashSet<>();
		return new FilteringIterator.OfLong(iterator, (LongPredicate) elements::add);
	}

	/**
	 * Obtain filtering iterator for the given primitive double iterator,
	 * filtering duplicate elements (via HashSet).
	 */
	public static OfDouble dedupe(final PrimitiveIterator.OfDouble iterator)
	{
		final Set<Double> elements = new HashSet<>();
		return new FilteringIterator.OfDouble(iterator, (DoublePredicate) elements::add);
	}

	public static class Of<T> extends FilteringIterator<T>
	{
		protected final Predicate<? super T> predicate;
		private T next;

		public Of(Iterable<T> iterable, Predicate<? super T> predicate)
		{
			this(iterable.iterator(), predicate);
		}

		public Of(Iterator<T> iterator, Predicate<? super T> predicate)
		{
			super(iterator);
			this.predicate = predicate;
			seekNext();
		}

		@Override
		public T next()
		{
			requireNext();
			T current = next;
			seekNext();
			return current;
		}

		private void seekNext()
		{
			while (iterator.hasNext()) {
				next = iterator.next();
				if (predicate.test(next)) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
			next = null;
		}
	}

	public static class OfInt extends FilteringIterator<Integer> implements PrimitiveIterator.OfInt
	{
		protected final IntPredicate predicate;
		private int next;

		public OfInt(IterableInt iterable, IntPredicate predicate)
		{
			this(iterable.iterator(), predicate);
		}

		public OfInt(PrimitiveIterator.OfInt iterator, IntPredicate predicate)
		{
			super(iterator);
			this.predicate = predicate;
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

		private void seekNext()
		{
			while (iterator.hasNext()) {
				next = ((PrimitiveIterator.OfInt) iterator).nextInt();
				if (predicate.test(next)) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}
	}

	public static class OfLong extends FilteringIterator<Long> implements PrimitiveIterator.OfLong
	{
		protected final LongPredicate predicate;
		private long next;

		public OfLong(IterableLong iterable, LongPredicate predicate)
		{
			this(iterable.iterator(), predicate);
		}

		public OfLong(PrimitiveIterator.OfLong iterator, LongPredicate predicate)
		{
			super(iterator);
			this.predicate = predicate;
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

		private void seekNext()
		{
			while (iterator.hasNext()) {
				next = ((PrimitiveIterator.OfLong) iterator).nextLong();
				if (predicate.test(next)) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}
	}

	public static class OfDouble extends FilteringIterator<Double> implements PrimitiveIterator.OfDouble
	{
		protected final DoublePredicate predicate;
		private double next;

		public OfDouble(IterableDouble iterable, DoublePredicate predicate)
		{
			this(iterable.iterator(), predicate);
		}

		public OfDouble(PrimitiveIterator.OfDouble iterator, DoublePredicate predicate)
		{
			super(iterator);
			this.predicate = predicate;
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

		private void seekNext()
		{
			while (iterator.hasNext()) {
				next = ((PrimitiveIterator.OfDouble) iterator).nextDouble();
				if (predicate.test(next)) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}
	}
}
