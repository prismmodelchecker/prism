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

	public static class OfInt extends FilteringIterable<Integer> implements PrimitiveIterable.OfInt
	{
		private IntPredicate predicate;

		public OfInt(PrimitiveIterable.OfInt iterable, IntPredicate predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}

		@Override
		public PrimitiveIterator.OfInt iterator()
		{
			return new FilteringIterator.OfInt((PrimitiveIterable.OfInt) iterable, predicate);
		}
	}

	public static class OfLong extends FilteringIterable<Long> implements PrimitiveIterable.OfLong
	{
		private LongPredicate predicate;

		public OfLong(PrimitiveIterable.OfLong iterable, LongPredicate predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}

		@Override
		public PrimitiveIterator.OfLong iterator()
		{
			return new FilteringIterator.OfLong((PrimitiveIterable.OfLong) iterable, predicate);
		}
	}

	public static class OfDouble extends FilteringIterable<Double> implements PrimitiveIterable.OfDouble
	{
		private DoublePredicate predicate;

		public OfDouble(PrimitiveIterable.OfDouble iterable, DoublePredicate predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}

		@Override
		public PrimitiveIterator.OfDouble iterator()
		{
			return new FilteringIterator.OfDouble((PrimitiveIterable.OfDouble) iterable, predicate);
		}
	}

	public static PrimitiveIterable.OfInt dedupe(PrimitiveIterable.OfInt iterable)
	{
		return new PrimitiveIterable.OfInt()
		{
			@Override
			public PrimitiveIterator.OfInt iterator()
			{
				return FilteringIterator.dedupe(iterable.iterator());
			}
		};
	}

	public static PrimitiveIterable.OfLong dedupe(PrimitiveIterable.OfLong iterable)
	{
		return new PrimitiveIterable.OfLong()
		{
			@Override
			public PrimitiveIterator.OfLong iterator()
			{
				return FilteringIterator.dedupe(iterable.iterator());
			}
		};
	}

	public static PrimitiveIterable.OfDouble dedupe(PrimitiveIterable.OfDouble iterable)
	{
		return new PrimitiveIterable.OfDouble()
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
