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

import java.util.BitSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

import common.IterableBitSet;
import common.iterable.FunctionalPrimitiveIterable.IterableDouble;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterable.IterableLong;

/**
 * Base class for Iterables around FilteringIterators,
 * static constructors for deduplicating entries.
 */
public abstract class FilteringIterable<E, I extends Iterable<E>> implements FunctionalIterable<E>
{
	protected final I iterable;

	public FilteringIterable(I iterable)
	{
		Objects.requireNonNull(iterable);
		this.iterable = iterable;
	}

	public static <T> FunctionalIterable<T> dedupe(Iterable<T> iterable)
	{
		return new DedupedIterable.Of<>(iterable);
	}

	public static IterableDouble dedupe(IterableDouble iterable)
	{
		return new DedupedIterable.Of<>(iterable).mapToDouble(Double::doubleValue);
	}

	public static IterableInt dedupe(IterableInt iterable)
	{
		return new DedupedIterable.OfInt(iterable);
	}

	public static IterableLong dedupe(IterableLong iterable)
	{
		return new DedupedIterable.Of<>(iterable).mapToLong(Long::longValue);
	}

	public static <T> FunctionalIterable<T> dedupeCons(Iterable<T> iterable)
	{
		return new FunctionalIterable<T>()
		{
			@Override
			public FunctionalIterator<T> iterator()
			{
				return FilteringIterator.dedupeCons(iterable.iterator());
			}
		};
	}

	public static IterableDouble dedupeCons(IterableDouble iterable)
	{
		return new IterableDouble()
		{
			@Override
			public FunctionalPrimitiveIterator.OfDouble iterator()
			{
				return FilteringIterator.dedupeCons(iterable.iterator());
			}
		};
	}

	public static IterableInt dedupeCons(IterableInt iterable)
	{
		return new IterableInt()
		{
			@Override
			public FunctionalPrimitiveIterator.OfInt iterator()
			{
				return FilteringIterator.dedupeCons(iterable.iterator());
			}
		};
	}

	public static IterableLong dedupeCons(IterableLong iterable)
	{
		return new IterableLong()
		{
			@Override
			public FunctionalPrimitiveIterator.OfLong iterator()
			{
				return FilteringIterator.dedupeCons(iterable.iterator());
			}
		};
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> FunctionalIterable<T> nonNull(Iterable<T> iterable)
	{
		if (iterable instanceof FunctionalPrimitiveIterable) {
			return (FunctionalPrimitiveIterable) iterable;
		}
		return new FilteringIterable.Of<>(iterable, Objects::nonNull);
	}



	public static abstract class DedupedIterable<E, I extends FunctionalIterable<E>> implements FunctionalIterable<E>
	{
		protected I source;
		protected I deduped;
	
		@SuppressWarnings("unchecked")
		public DedupedIterable(Iterable<E> source)
		{
			this.source  = (I) FunctionalIterable.extend(source);
			this.deduped = null;
		}
	
	
	
		public static class Of<E> extends DedupedIterable<E, FunctionalIterable<E>>
		{
			public Of(Iterable<E> source)
			{
				super(source);
			}
	
			@Override
			public FunctionalIterator<E> iterator()
			{
				if (source == null) {
					return deduped.iterator();
				}
				Set<E> set                 = new HashSet<E>();
				FunctionalIterable<E> iter = source.filter(set::add);
				deduped                    = FunctionalIterable.extend(set);
				source                     = null;
				return iter.iterator();
			}
		}
	
	
	
		public static class OfInt extends DedupedIterable<Integer, IterableInt> implements IterableInt
		{
			public OfInt(IterableInt source)
			{
				super(source);
			}
	
			@Override
			public FunctionalPrimitiveIterator.OfInt iterator()
			{
				if (source == null) {
					deduped.iterator();
				}
				BitSet bits          = new BitSet();
				IntPredicate set     = (int i) -> {if (bits.get(i)) return false; else bits.set(i); return true;};
				IterableInt filtered = source.filter(set);
				deduped              = IterableBitSet.getSetBits(bits);
				source               = null;
				return FunctionalIterator.extend(filtered.iterator());
			}
		}
	}



	public static class Of<E> extends FilteringIterable<E, Iterable<E>>
	{
		protected final Predicate<? super E> predicate;

		public Of(Iterable<E> iterable, Predicate<? super E> predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			return new FilteringIterator.Of<>(iterable.iterator(), predicate);
		}
	}



	public static class OfDouble extends FilteringIterable<Double, IterableDouble> implements IterableDouble
	{
		protected final DoublePredicate predicate;
	
		public OfDouble(IterableDouble iterable, DoublePredicate predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}
	
		@Override
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new FilteringIterator.OfDouble(iterable.iterator(), predicate);
		}
	}



	public static class OfInt extends FilteringIterable<Integer, IterableInt> implements IterableInt
	{
		protected final IntPredicate predicate;

		public OfInt(IterableInt iterable, IntPredicate predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return new FilteringIterator.OfInt(iterable.iterator(), predicate);
		}
	}



	public static class OfLong extends FilteringIterable<Long, IterableLong> implements IterableLong
	{
		protected final LongPredicate predicate;

		public OfLong(IterableLong iterable, LongPredicate predicate)
		{
			super(iterable);
			this.predicate = predicate;
		}

		@Override
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return new FilteringIterator.OfLong(iterable.iterator(), predicate);
		}
	}
}
