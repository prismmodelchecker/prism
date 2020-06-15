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

import java.util.Collection;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;

import common.IteratorTools;
import common.functions.ObjIntFunction;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;

public class Interval implements IterableInt
{
	public static class IntervalIterator implements FunctionalPrimitiveIterator.OfInt
	{
		protected final int upperBound;
		protected final int step;
		protected int next;

		public IntervalIterator(int first, int upperBound, int step)
		{
			assert step > 0 : "positive step width expected";
			this.upperBound = upperBound;
			this.step       = step;
			this.next       = first;
		}

		@Override
		public boolean hasNext()
		{
			return next < upperBound;
		}

		@Override
		public int nextInt()
		{
			int current = next;
			next        = next + step;
			return current;
		}

		@Override
		public void forEachRemaining(IntConsumer action)
		{
			while (next < upperBound) {
				action.accept(next);
				next += step;
			}
		}

		@Override
		public int collectAndCount(Collection<? super Integer> collection)
		{
			int count = 0;
			while (next < upperBound) {
				count++;
				collection.add(next);
				next += step;
			}
			return count;
		}

		@Override
		public int collectAndCount(Integer[] array, int offset)
		{
			int count = offset;
			while (next < upperBound) {
				array[count++] = next;
				next += step;
			}
			return count - offset;
		}

		@Override
		public int collectAndCount(int[] array, int offset)
		{
			int count = offset;
			while (next < upperBound) {
				array[count++] = next;
				next += step;
			}
			return count - offset;
		}

		@Override
		public boolean contains(int i)
		{
			boolean result = (i >= next) && (i < upperBound) && ((i - next) % step) == 0;
			next = upperBound;
			return result;
		}

		@Override
		public int count()
		{
			if (next >= upperBound) {
				return 0;
			}
			int result = (upperBound - next - 1) / step + 1;
			next = upperBound;
			return result;
		}

		@Override
		public OptionalInt max()
		{
			OptionalInt result = (next >= upperBound) ? OptionalInt.empty() : OptionalInt.of(upperBound);
			next = upperBound;
			return result;
		}

		@Override
		public OptionalInt min()
		{
			OptionalInt result = (next >= upperBound) ? OptionalInt.empty() : OptionalInt.of(next);
			next = upperBound;
			return result;
		}

		@Override
		public <T> T reduce(T identity, BiFunction<T, ? super Integer, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (next < upperBound) {
				result = accumulator.apply(result, next);
				next  += step;
			}
			return result;
		}

		@Override
		public <T> T reduce(T identity, ObjIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (next < upperBound) {
				result = accumulator.apply(result, next);
				next  += step;
			}
			return result;
		}

		@Override
		public int reduce(int identity, IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			int result = identity;
			while (next < upperBound) {
				result = accumulator.applyAsInt(result, next);
				next  += step;
			}
			return result;
		}

		@Override
		public int sum()
		{
			if (next >= upperBound) {
				return 0;
			}
			// Sn = a + (a+d) + (a+2d) + ... + (a+(n-1)d)
			// Sn = n(2a+(n-1)d)/2 
			int count = count();
			int result = count * (2*next + (count-1)*step)/2;
			next = upperBound;
			return result;
		}
	}

	protected final int lowerBound;
	protected final int upperBound;
	protected final int step;

	public Interval(int upperBound)
	{
		this(0, upperBound);
	}

	public Interval(final int lowerBound, final int upperBound)
	{
		this(lowerBound, upperBound, 1);
	}

	public Interval(final int lowerBound, final int upperBound, final int step)
	{
		assert step > 0 : "positive step width expected";
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.step = step;
	}

	@Override
	public IntervalIterator iterator()
	{
		return new IntervalIterator(lowerBound, upperBound, step);
	}



	public String toString()
	{
		return getClass().getSimpleName() + "(" + lowerBound + ", " + upperBound + ", " + step + ")";
	}

	public static void main(final String[] args)
	{
		Interval interval = new Interval(-3, 5);
		IteratorTools.printIterator("Interval(-3, 5, 1)", interval.iterator());
		System.out.println("count = "+interval.count());
		System.out.println("sum   = "+interval.sum());

		interval = new Interval(-3, 3, 2);
		IteratorTools.printIterator("Interval(-3, 3, 2)", interval.iterator());
		System.out.println("count = "+interval.count());
		System.out.println("sum   = "+interval.sum());

		interval = new Interval(-3, 5, 3);
		IteratorTools.printIterator("Interval(-3, 5, 3)", interval.iterator());
		System.out.println("count = "+interval.count());
		System.out.println("sum   = "+interval.sum());

		interval = new Interval(-3, -3);
		IteratorTools.printIterator("Interval(-3, -3)", interval.iterator());
		System.out.println("count = "+interval.count());
		System.out.println("sum   = "+interval.sum());

	}
}