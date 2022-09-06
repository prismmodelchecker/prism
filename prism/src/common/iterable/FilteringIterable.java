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

import java.util.Iterator;
import java.util.PrimitiveIterator;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

/**
 * Base class for Iterables around FilteringIterators,
 * static constructors for deduplicating entries. */
public abstract class FilteringIterable<T> implements Iterable<T>
{
	protected final Iterable<T> iterable;

	public FilteringIterable(final Iterable<T> iterable)
	{
		this.iterable = iterable;
	}

	public static class Of<T> extends FilteringIterable<T>
	{
		private Predicate<? super T> predicate;

		public Of(Iterable<T> iterable, Predicate<? super T> predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}

		@Override
		public Iterator<T> iterator()
		{
			return new FilteringIterator.Of<>(iterable, predicate);
		}
	}

	public static class OfInt extends FilteringIterable<Integer> implements IterableInt
	{
		private IntPredicate predicate;

		public OfInt(IterableInt iterable, IntPredicate predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}

		@Override
		public PrimitiveIterator.OfInt iterator()
		{
			return new FilteringIterator.OfInt((IterableInt) iterable, predicate);
		}
	}

	public static class OfLong extends FilteringIterable<Long> implements IterableLong
	{
		private LongPredicate predicate;

		public OfLong(IterableLong iterable, LongPredicate predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}

		@Override
		public PrimitiveIterator.OfLong iterator()
		{
			return new FilteringIterator.OfLong((IterableLong) iterable, predicate);
		}
	}

	public static class OfDouble extends FilteringIterable<Double> implements IterableDouble
	{
		private DoublePredicate predicate;

		public OfDouble(IterableDouble iterable, DoublePredicate predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}

		@Override
		public PrimitiveIterator.OfDouble iterator()
		{
			return new FilteringIterator.OfDouble((IterableDouble) iterable, predicate);
		}
	}

	public static IterableInt dedupe(IterableInt iterable)
	{
		return new IterableInt()
		{
			@Override
			public PrimitiveIterator.OfInt iterator()
			{
				return FilteringIterator.dedupe(iterable.iterator());
			}
		};
	}

	public static IterableLong dedupe(IterableLong iterable)
	{
		return new IterableLong()
		{
			@Override
			public PrimitiveIterator.OfLong iterator()
			{
				return FilteringIterator.dedupe(iterable.iterator());
			}
		};
	}

	public static IterableDouble dedupe(IterableDouble iterable)
	{
		return new IterableDouble()
		{
			@Override
			public PrimitiveIterator.OfDouble iterator()
			{
				return FilteringIterator.dedupe(iterable.iterator());
			}
		};
	}

	public static <T> Iterable<T> dedupe(Iterable<T> iterable)
	{
		return new Iterable<T>()
		{
			@Override
			public Iterator<T> iterator()
			{
				return FilteringIterator.dedupe(iterable.iterator());
			}
		};
	}
}
