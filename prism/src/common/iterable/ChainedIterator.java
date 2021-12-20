//==============================================================================
//	
//	Copyright (c) 2015-
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
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;

/**
 * Abstract base class implementing an Iterator that chains a sequence of Iterators.
 * Returns all the elements of the first Iterator, then the elements of the
 * second Iterator and so on.
 * <p>
 * The calls to {@code next()} of the underlying Iterator happen on-the-fly,
 * i.e., only when {@code next()} is called for this Iterator.
 * <p>
 * Implementations should release the underlying Iterators after iteration.
 * <p>
 * This Iterator does not support the {@code remove()} method, even if the underlying
 * Iterators support it.
 *
 * @param <E> type of the {@link Iterator}'s elements
 * @param <I> common super type of {@link FunctionalIterator} for all Iterators in the chain
 */
public abstract class ChainedIterator<E, I extends FunctionalIterator<E>> implements FunctionalIterator<E>
{
	/** The Iterator over the sequence of Iterators that are chained */
	protected FunctionalIterator<I> iterators;
	/** The current Iterator in the sequence of Iterators */
	protected I current;

	/**
	 * Constructor for an Iterator that chains Iterators provided in an Iterator.
	 *
	 * @param iterators an Iterator over Iterators to be chained
	 */
	public ChainedIterator(Iterator<? extends Iterator<E>>iterators)
	{
		this(EmptyIterator.of(), iterators);
	}

	/**
	 * Constructor for chaining an Iterator and a number of Iterators provided in an Iterator.
	 *
	 * @param iterator  an Iterator to prepend the chain
	 * @param iterators an Iterator over Iterators to be chained
	 */
	@SuppressWarnings({ "unchecked"})
	public ChainedIterator(Iterator<E> iterator, Iterator<? extends Iterator<E>> iterators)
	{
		Objects.requireNonNull(iterator);
		Objects.requireNonNull(iterators);
		this.current   = (I) Reducible.extend(iterator);
		this.iterators = (FunctionalIterator<I>) Reducible.extend(iterators).map(Reducible::extend);
		hasNext();
	}

	@Override
	public boolean hasNext()
	{
		if (current.hasNext()) {
			// the current iterator has another element
			return true;
		}

		// the current iterator has no more elements,
		// search for the next iterator that as an element
		while (iterators.hasNext()) {
			// consider the next iterator
			current = iterators.next();
			if (current.hasNext()) {
				// iterator has element, keep current and return true
				return true;
			}
		}
		// there are no more iterators / elements
		release();
		return false;
	}

	@Override
	public long count()
	{
		long count = current.count() + iterators.mapToLong(FunctionalIterator::count).sum();
		release();
		return count;
	}

	@Override
	public void release()
	{
		iterators = EmptyIterator.of();
	}



	/**
	 * Generic implementation of a chained Iterator.
	 *
	 * @param <E> type of the Iterator's elements
	 */
	public static class Of<E> extends ChainedIterator<E, FunctionalIterator<E>>
	{
		/**
		 * Constructor for an Iterator that chains Iterators provided in an Iterator.
		 *
		 * @param iterators an Iterator over Iterators to be chained
		 */
		@SuppressWarnings("unchecked")
		public Of(Iterator<? extends Iterator<? extends E>> iterators)
		{
			super((Iterator<? extends Iterator<E>>) iterators);
		}

		/**
		 * Constructor for chaining an Iterator and a number of Iterators provided in an Iterator.
		 *
		 * @param iterator  an Iterator to prepend the chain
		 * @param iterators an Iterator over Iterators to be chained
		 */
		@SuppressWarnings("unchecked")
		public Of(Iterator<? extends E> iterator, Iterator<? extends Iterator<? extends E>> iterators)
		{
			super((Iterator<E>) iterator, (Iterator<? extends Iterator<E>>) iterators);
		}

		@Override
		public E next()
		{
			requireNext();
			return current.next();
		}

		@Override
		public boolean contains(Object obj)
		{
			boolean contains = current.contains(obj) || iterators.anyMatch(it -> it.contains(obj));
			release();
			return contains;
		}

		@Override
		public void release()
		{
			super.release();
			current   = EmptyIterator.of();
		}
	}



	/**
	 * Primitive specialisation for {@code double} of a chained Iterator.
	 */
	public static class OfDouble extends ChainedIterator<Double, FunctionalPrimitiveIterator.OfDouble> implements FunctionalPrimitiveIterator.OfDouble
	{
		/**
		 * Constructor for an Iterator that chains Iterators provided in an Iterator.
		 *
		 * @param iterators an Iterator over Iterators to be chained
		 */
		public OfDouble(Iterator<? extends PrimitiveIterator.OfDouble> iterators)
		{
			super(iterators);
		}

		/**
		 * Constructor for chaining an Iterator and a number of Iterators provided in an Iterator.
		 *
		 * @param iterator  an Iterator to prepend the chain
		 * @param iterators an Iterator over Iterators to be chained
		 */
		public OfDouble(PrimitiveIterator.OfDouble iterator, Iterator<? extends PrimitiveIterator.OfDouble> iterators)
		{
			super(iterator, iterators);
		}

		@Override
		public double nextDouble()
		{
			requireNext();
			return current.nextDouble();
		}

		@Override
		public boolean contains(double d)
		{
			boolean result = current.contains(d) || iterators.anyMatch(it -> it.contains(d));
			release();
			return result;
		}

		@Override
		public OptionalDouble max()
		{
			if (!hasNext()) {
				return OptionalDouble.empty();
			}
			double max = current.max().getAsDouble();
			max = iterators.reduce(max, (m, it) -> Math.max(m, it.max().orElse(m)));
			release();
			return OptionalDouble.of(max);
		}

		@Override
		public OptionalDouble min()
		{
			if (!hasNext()) {
				return OptionalDouble.empty();
			}
			double min = current.min().getAsDouble();
			min = iterators.reduce(min, (m, it) -> Math.min(m, it.min().orElse(m)));
			release();
			return OptionalDouble.of(min);
		}

		@Override
		public double sum()
		{
			double sum = iterators.reduce(current.sum(), (s, it) -> s + it.sum());
			release();
			return sum;
		}

		@Override
		public void release()
		{
			super.release();
			current   = EmptyIterator.ofDouble();
		}
	}



	/**
	 * Primitive specialisation for {@code int} of a chained Iterator.
	 */
	public static class OfInt extends ChainedIterator<Integer, FunctionalPrimitiveIterator.OfInt> implements FunctionalPrimitiveIterator.OfInt
	{
		/**
		 * Constructor for an Iterator that chains Iterators provided in an Iterator.
		 *
		 * @param iterators an Iterator over Iterators to be chained
		 */
		public OfInt(Iterator<? extends PrimitiveIterator.OfInt> iterators)
		{
			super(iterators);
		}

		/**
		 * Constructor for chaining an Iterator and a number of Iterators provided in an Iterator.
		 *
		 * @param iterator  an Iterator to prepend the chain
		 * @param iterators an Iterator over Iterators to be chained
		 */
		public OfInt(PrimitiveIterator.OfInt iterator, Iterator<? extends PrimitiveIterator.OfInt> iterators)
		{
			super(iterator, iterators);
		}

		@Override
		public int nextInt()
		{
			requireNext();
			return current.nextInt();
		}

		@Override
		public boolean contains(int i)
		{
			boolean result = current.contains(i) || iterators.anyMatch(it -> it.contains(i));
			release();
			return result;
		}

		@Override
		public OptionalInt max()
		{
			if (!hasNext()) {
				return OptionalInt.empty();
			}
			int max = current.max().getAsInt();
			max = iterators.reduce(max, (int m, FunctionalPrimitiveIterator.OfInt it) -> Math.max(m, it.max().orElse(m)));
			release();
			return OptionalInt.of(max);
		}

		@Override
		public OptionalInt min()
		{
			if (!hasNext()) {
				return OptionalInt.empty();
			}
			int min = current.min().getAsInt();
			min = iterators.reduce(min, (int m, FunctionalPrimitiveIterator.OfInt it) -> Math.min(m, it.min().orElse(m)));
			release();
			return OptionalInt.of(min);
		}

		@Override
		public long sum()
		{
			long sum = iterators.reduce(current.sum(), (long s, FunctionalPrimitiveIterator.OfInt it) -> s + it.sum());
			release();
			return sum;
		}

		@Override
		public void release()
		{
			super.release();
			current   = EmptyIterator.ofInt();
		}
	}



	/**
	 * Primitive specialisation for {@code long} of a chained iterator.
	 */
	public static class OfLong extends ChainedIterator<Long, FunctionalPrimitiveIterator.OfLong> implements FunctionalPrimitiveIterator.OfLong
	{
		/**
		 * Constructor for an Iterator that chains Iterators provided in an Iterator.
		 *
		 * @param iterators an Iterator over Iterators to be chained
		 */
		public OfLong(Iterator<? extends PrimitiveIterator.OfLong> iterators)
		{
			super(iterators);
		}

		/**
		 * Constructor for chaining an Iterator and a number of Iterators provided in an Iterator.
		 *
		 * @param iterator  an Iterator to prepend the chain
		 * @param iterators an Iterator over Iterators to be chained
		 */
		public OfLong(PrimitiveIterator.OfLong iterator, Iterator<? extends PrimitiveIterator.OfLong> iterators)
		{
			super(iterator, iterators);
		}

		@Override
		public long nextLong()
		{
			requireNext();
			return current.nextLong();
		}

		@Override
		public boolean contains(long l)
		{
			boolean result = current.contains(l) || iterators.anyMatch(it -> it.contains(l));
			release();
			return result;
		}

		@Override
		public OptionalLong max()
		{
			if (!hasNext()) {
				return OptionalLong.empty();
			}
			long max = current.max().getAsLong();
			max = iterators.reduce(max, (long m, FunctionalPrimitiveIterator.OfLong it) -> Math.max(m, it.max().orElse(m)));
			release();
			return OptionalLong.of(max);
		}

		@Override
		public OptionalLong min()
		{
			if (!hasNext()) {
				return OptionalLong.empty();
			}
			long min = current.min().getAsLong();
			min = iterators.reduce(min, (long m, FunctionalPrimitiveIterator.OfLong it) -> Math.min(m, it.min().orElse(m)));
			release();
			return OptionalLong.of(min);
		}

		@Override
		public long sum()
		{
			long sum = iterators.reduce(current.sum(), (long s, FunctionalPrimitiveIterator.OfLong it) -> s + it.sum());
			release();
			return sum;
		}

		@Override
		public void release()
		{
			super.release();
			current   = EmptyIterator.ofLong();
		}
	}
}
