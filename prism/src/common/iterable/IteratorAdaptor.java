//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <steffen.maercker@tu-dresden.de> (TU Dresden)
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
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * Abstract base class of adaptors that extend non-functional Iterators with the methods provided by {@link FunctionalIterator}.
 * Implementations should release the underlying Iterator after iteration.
 *
 * @param <E> type of the {@link Iterator}'s elements
 * @param <I> type of the Iterator to extend
 */
public abstract class IteratorAdaptor<E, I extends Iterator<E>> implements FunctionalIterator<E>
{
	/** the Iterator that is extended */
	protected I iterator;

	/**
	 * Generic constructor that wraps an Iterator.
	 *
	 * @param iterator the {@link Iterator} to be extended
	 */
	public IteratorAdaptor(I iterator)
	{
		Objects.requireNonNull(iterator);
		this.iterator = iterator;
	}

	@Override
	public boolean hasNext()
	{
		if (iterator.hasNext()) {
			return true;
		}
		release();
		return false;
	}

	@Override
	public E next()
	{
		return iterator.next();
	}

	@Override
	public void forEachRemaining(Consumer<? super E> action)
	{
		iterator.forEachRemaining(action);
		release();
	}

	/**
	 * Unwrap a nested iterator if this instance is an adaptor wrapping an Iterator.
	 * Use to avoid repeated indirections, especially in loops.
	 *
	 * @return the wrapped iterator.
	 */
	@Override
	public I unwrap()
	{
		return iterator;
	}



	/**
	 * Generic implementation of an {@link IteratorAdaptor}.
	 *
	 * @param <E> type of the Iterator's elements
	 */
	public static class Of<E> extends IteratorAdaptor<E, Iterator<E>>
	{
		public Of(Iterator<E> iterator)
		{
			super(iterator);
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.of();
		}
	}



	/**
	 * Primitive specialisation for {@code double} of an {@link IteratorAdaptor}.
	 */
	public static class OfDouble extends IteratorAdaptor<Double, PrimitiveIterator.OfDouble> implements FunctionalPrimitiveIterator.OfDouble
	{
		/**
		 * Constructor that wraps a PrimitiveIterator.OfDouble.
		 *
		 * @param iterator the {@link PrimitiveIterator.OfDouble} to be extended
		 */
		public OfDouble(PrimitiveIterator.OfDouble iterator)
		{
			super(iterator);
		}

		@Override
		public double nextDouble()
		{
			return iterator.nextDouble();
		}

		@Override
		public void forEachRemaining(DoubleConsumer action)
		{
			iterator.forEachRemaining(action);
			release();
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.ofDouble();
		}
	}



	/**
	 * Primitive specialisation for {@code int} of an {@link IteratorAdaptor}.
	 */
	public static class OfInt extends IteratorAdaptor<Integer, PrimitiveIterator.OfInt> implements FunctionalPrimitiveIterator.OfInt
	{
		/**
		 * Constructor that wraps a PrimitiveIterator.OfInt.
		 *
		 * @param iterator the {@link PrimitiveIterator.OfInt} to be extended
		 */
		public OfInt(PrimitiveIterator.OfInt iterator)
		{
			super(iterator);
		}

		@Override
		public int nextInt()
		{
			return iterator.nextInt();
		}

		@Override
		public void forEachRemaining(IntConsumer action)
		{
			iterator.forEachRemaining(action);
			release();
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.ofInt();
		}
	}



	/**
	 * Primitive specialisation for {@code long} of an {@link IteratorAdaptor}.
	 */
	public static class OfLong extends IteratorAdaptor<Long, PrimitiveIterator.OfLong> implements FunctionalPrimitiveIterator.OfLong
	{
		/**
		 * Constructor that wraps a PrimitiveIterator.OfLong.
		 *
		 * @param iterator the {@link PrimitiveIterator.OfLong} to be extended
		 */
		public OfLong(PrimitiveIterator.OfLong iterator)
		{
			super(iterator);
		}

		@Override
		public long nextLong()
		{
			return iterator.nextLong();
		}

		@Override
		public void forEachRemaining(LongConsumer action)
		{
			iterator.forEachRemaining(action);
			release();
		}

		@Override
		public void release()
		{
			iterator = EmptyIterator.ofLong();
		}
	}
}
