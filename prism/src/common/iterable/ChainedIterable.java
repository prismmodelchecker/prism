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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntBinaryOperator;

import common.IteratorTools;

/**
 * Abstract base class for Iterables that chain a sequence of Iterables.
 *
 * @param <E> type of the Iterator's elements
 * @param <I> type of the underlying Iterables
 */
public abstract class ChainedIterable<E, I extends Iterable<E>> implements FunctionalIterable<E>
{
	/** The Iterable over the sequence of Iterables that are chained */
	protected final FunctionalIterable<? extends I> iterables;

	/**
	 * Constructor for an Iterable that chains Iterables provided in an Iterable.
	 *
	 * @param iterables an Iterable of Iterables to be chained
	 */
	public ChainedIterable(Iterable<? extends I> iterables)
	{
		Objects.requireNonNull(iterables);
		this.iterables = FunctionalIterable.extend(iterables);
	}



	/**
	 * Generic implementation of a chained Iterable.
	 *
	 * @param <E> type of the Iterable's elements
	 */
	public static class Of<E> extends ChainedIterable<E, Iterable<E>>
	{
		/**
		 * Constructor for an Iterable that chains Iterables provided in an Iterable.
		 *
		 * @param iterables an Iterable of Iterables to be chained
		 */
		@SuppressWarnings("unchecked")
		public Of(Iterable<? extends Iterable<? extends E>> iterables)
		{
			super((Iterable<? extends Iterable<E>>) iterables);
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			return new ChainedIterator.Of<>(iterables.map(Iterable::iterator).iterator());
		}
	}



	/**
	 * Primitive specialisation for {@code double} of a chained Iterable.
	 */
	public static class OfDouble extends ChainedIterable<Double, PrimitiveIterable.OfDouble> implements FunctionalPrimitiveIterable.OfDouble
	{
		/**
		 * Constructor for an Iterable that chains Iterables provided in an Iterable.
		 *
		 * @param iterables an Iterable of Iterables to be chained
		 */
		public OfDouble(Iterable<? extends PrimitiveIterable.OfDouble> iterables)
		{
			super(iterables);
		}

		@Override
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new ChainedIterator.OfDouble(iterables.map(PrimitiveIterable.OfDouble::iterator).iterator());
		}
	}



	/**
	 * Primitive specialisation for {@code int} of a chained Iterable.
	 */
	public static class OfInt extends ChainedIterable<Integer, PrimitiveIterable.OfInt> implements FunctionalPrimitiveIterable.OfInt
	{
		/**
		 * Constructor for an Iterable that chains Iterables provided in an Iterable.
		 *
		 * @param iterables an Iterable of Iterables to be chained
		 */
		public OfInt(Iterable<? extends PrimitiveIterable.OfInt> iterables)
		{
			super(iterables);
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return new ChainedIterator.OfInt(iterables.map(PrimitiveIterable.OfInt::iterator).iterator());
		}
	}



	/**
	 * Primitive specialisation for {@code long} of a chained Iterable.
	 */
	public static class OfLong extends ChainedIterable<Long, PrimitiveIterable.OfLong> implements FunctionalPrimitiveIterable.OfLong
	{
		/**
		 * Constructor for an Iterable that chains Iterables provided in an Iterable.
		 *
		 * @param iterables an Iterable of Iterables to be chained
		 */
		public OfLong(Iterable<? extends PrimitiveIterable.OfLong> iterables)
		{
			super(iterables);
		}

		@Override
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return new ChainedIterator.OfLong(iterables.map(PrimitiveIterable.OfLong::iterator).iterator());
		}
	}



	/**
	 * Simple test method
	 */
	public static void main(final String[] args)
	{
		final List<Integer> l1 = Arrays.asList(new Integer[] { 1, 2, 3 });
		final List<Integer> l2 = Arrays.asList(new Integer[] { 4, 5, 6 });
		final FunctionalIterable<Integer> chain1 = new ChainedIterable.Of<Integer>(new IterableArray.Of(l1, l2));
		IteratorTools.printIterator("chain1", chain1.iterator());
		System.out.println("max    = " + chain1.reduce(Math::max));

		final Range i1 = new Range(1, 4);
		final Range i2 = new Range(5, 10, 2);
		final ChainedIterable.OfInt chain2 = new ChainedIterable.OfInt(new IterableArray.Of(i1, i2));
		IteratorTools.printIterator("chain2", chain2.iterator());
		System.out.println("max    = " + chain2.reduce((IntBinaryOperator) Math::max));

		final IterableArray.OfInt a2 = new IterableArray.OfInt(6, 8, 10);
		final ChainedIterable.OfInt chain3 = new ChainedIterable.OfInt(new IterableArray.Of(i1, a2));
		IteratorTools.printIterator("chain3", chain3.iterator());
		System.out.println("max    = " + chain3.reduce((IntBinaryOperator) Math::max));
	}
}