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

import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * An adaptor that extends non-functional Iterables with the methods provided by {@link FunctionalIterable}.
 *
 * @param <E> type of the {@link Iterable}'s elements
 * @param <I> type of the Iterable to extend
 */
public abstract class IterableAdaptor<E, I extends Iterable<E>> implements FunctionalIterable<E>
{
	/** the Iterable that is extended */
	protected final I iterable;

	/**
	 * Generic constructor that wraps an Iterable.
	 *
	 * @param iterable the {@link Iterable} to be extended
	 */
	public IterableAdaptor(I iterable)
	{
		Objects.requireNonNull(iterable);
		this.iterable = iterable;
	}

	@Override
	public void forEach(Consumer<? super E> action)
	{
		iterable.forEach(action);
	}



	public static class Of<E> extends IterableAdaptor<E, Iterable<E>>
	{
		/**
		 * Generic constructor that wraps an Iterable.
		 *
		 * @param iterable the {@link Iterable} to be extended
		 */
		public Of(Iterable<E> iterable)
		{
			super(iterable);
		}

		@Override
		public IteratorAdaptor.Of <E> iterator()
		{
			return new IteratorAdaptor.Of<>(iterable.iterator());
		}
	}



	public static class OfDouble extends IterableAdaptor<Double, PrimitiveIterable.OfDouble> implements FunctionalPrimitiveIterable.OfDouble
	{
		/**
		 * Constructor that wraps a PrimitiveIterator.OfDouble.
		 *
		 * @param iterator the {@link PrimitiveIterator.OfDouble} to be extended
		 */
		public OfDouble(PrimitiveIterable.OfDouble iterator)
		{
			super(iterator);
		}

		@Override
		public IteratorAdaptor.OfDouble iterator()
		{
			return new IteratorAdaptor.OfDouble(iterable.iterator());
		}

		@Override
		public void forEach(DoubleConsumer action)
		{
			iterable.forEach(action);
		}
	}



	public static class OfInt extends IterableAdaptor<Integer, PrimitiveIterable.OfInt> implements FunctionalPrimitiveIterable.OfInt
	{
		/**
		 * Constructor that wraps a PrimitiveIterator.OfInt.
		 *
		 * @param iterator the {@link PrimitiveIterator.OfLong} to be extended
		 */
		public OfInt(PrimitiveIterable.OfInt iterator)
		{
			super(iterator);
		}

		@Override
		public IteratorAdaptor.OfInt iterator()
		{
			return new IteratorAdaptor.OfInt(iterable.iterator());
		}

		@Override
		public void forEach(IntConsumer action)
		{
			iterable.forEach(action);
		}
}



	public static class OfLong extends IterableAdaptor<Long, PrimitiveIterable.OfLong> implements FunctionalPrimitiveIterable.OfLong
	{
		/**
		 * Constructor that wraps a PrimitiveIterator.OfLong.
		 *
		 * @param iterator the {@link PrimitiveIterator.OfLong} to be extended
		 */
		public OfLong(PrimitiveIterable.OfLong iterator)
		{
			super(iterator);
		}

		@Override
		public IteratorAdaptor.OfLong iterator()
		{
			return new IteratorAdaptor.OfLong(iterable.iterator());
		}

		@Override
		public void forEach(LongConsumer action)
		{
			iterable.forEach(action);
		}
	}
}
