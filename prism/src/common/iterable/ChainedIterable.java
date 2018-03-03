//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
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
import common.iterable.FunctionalPrimitiveIterable.IterableDouble;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterable.IterableLong;

public abstract class ChainedIterable<E, I extends Iterable<E>> implements FunctionalIterable<E>
{
	protected final FunctionalIterable<? extends I> iterables;

	@SafeVarargs
	public ChainedIterable(I... iterables)
	{
		this(new IterableArray.Of<>(iterables));
	}

	public ChainedIterable(Iterable<? extends I> iterables)
	{
		Objects.requireNonNull(iterables);
		this.iterables = FunctionalIterable.extend(iterables);
	}



	public static class Of<E> extends ChainedIterable<E, Iterable<E>>
	{
		@SafeVarargs
		@SuppressWarnings("unchecked")
		public Of(Iterable<? extends E>... iterables)
		{
			super((Iterable<E>[])iterables);
		}

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



	public static class OfDouble extends ChainedIterable<Double, IterableDouble> implements IterableDouble
	{
		@SafeVarargs
		public OfDouble(IterableDouble... iterables)
		{
			super(iterables);
		}
	
		public OfDouble(Iterable<IterableDouble> iterables)
		{
			super(iterables);
		}
	
		@Override
		public FunctionalPrimitiveIterator.OfDouble iterator()
		{
			return new ChainedIterator.OfDouble(iterables.map(IterableDouble::iterator).iterator());
		}
	}



	public static class OfInt extends ChainedIterable<Integer, IterableInt> implements IterableInt
	{
		@SafeVarargs
		public OfInt(IterableInt... iterables)
		{
			super(iterables);
		}

		public OfInt(Iterable<IterableInt> iterables)
		{
			super(iterables);
		}

		@Override
		public FunctionalPrimitiveIterator.OfInt iterator()
		{
			return new ChainedIterator.OfInt(iterables.map(IterableInt::iterator).iterator());
		}
	}



	public static class OfLong extends ChainedIterable<Long, IterableLong> implements IterableLong
	{
		@SafeVarargs
		public OfLong(IterableLong... iterables)
		{
			super(iterables);
		}

		public OfLong(Iterable<IterableLong> iterables)
		{
			super(iterables);
		}

		@Override
		public FunctionalPrimitiveIterator.OfLong iterator()
		{
			return new ChainedIterator.OfLong(iterables.map(IterableLong::iterator).iterator());
		}
	}



	public static void main(final String[] args)
	{
		final List<Integer> l1 = Arrays.asList(new Integer[] { 1, 2, 3 });
		final List<Integer> l2 = Arrays.asList(new Integer[] { 4, 5, 6 });
		final FunctionalIterable<Integer> chain1 = new ChainedIterable.Of<Integer>(l1, l2);
		IteratorTools.printIterator("chain1", chain1.iterator());
		System.out.println("max    = " + chain1.reduce(Math::max));

		final Interval i1 = new Interval(1, 4);
		final Interval i2 = new Interval(5, 10, 2);
		final IterableInt chain2 = new ChainedIterable.OfInt(i1, i2);
		IteratorTools.printIterator("chain2", chain2.iterator());
		System.out.println("max    = " + chain2.reduce((IntBinaryOperator) Math::max));

		final IterableArray.OfInt a2 = new IterableArray.OfInt(6, 8, 10);
		final IterableInt chain3 = new ChainedIterable.OfInt(i1, a2);
		IteratorTools.printIterator("chain3", chain3.iterator());
		System.out.println("max    = " + chain3.reduce((IntBinaryOperator) Math::max));
	}
}