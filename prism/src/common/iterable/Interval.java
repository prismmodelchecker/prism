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

import java.util.OptionalInt;

import common.IteratorTools;

/**
 * An Iterable that yields all ints from an interval with a given step-width.
 */
public class Interval implements FunctionalPrimitiveIterable.OfInt
{
	protected final int lowerBound;
	protected final int upperBound;
	protected final int step;

	/**
	 * Constructor for an interval [0, upperBound) with step-width 1.
	 *
	 * @param upperBound last {@code int}, exclusive
	 */
	public Interval(int upperBound)
	{
		this(0, upperBound);
	}

	/**
	 * Constructor for an interval [lowerBound, upperBound) with step-width 1.
	 *
	 * @param lowerBound first {@code int}, inclusive
	 * @param upperBound last {@code int}, exclusive
	 */
	public Interval(int lowerBound, int upperBound)
	{
		this(lowerBound, upperBound, 1);
	}

	/**
	 * Constructor for an interval [lowerBound, upperBound) with a custom step-width.
	 *
	 * @param lowerBound first {@code int}, inclusive
	 * @param upperBound last {@code int}, exclusive
	 * @param step a positive {@code int}
	 */
	public Interval(int lowerBound, int upperBound, int step)
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

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "(" + lowerBound + ", " + upperBound + ", " + step + ")";
	}



	/**
	 * An Iterator that yields all ints from an interval with a given step-width.
	 */
	public static class IntervalIterator implements FunctionalPrimitiveIterator.OfInt
	{
		protected final int upperBound;
		protected final int step;
		protected int next;

		/**
		 * Constructor for an Iterator over an interval [lowerBound, upperBound) with a custom step-width.
		 *
		 * @param lowerBound first {@code int}, inclusive
		 * @param upperBound last {@code int}, exclusive
		 * @param step a positive {@code int}
		 */
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
			requireNext();
			int current = next;
			next        = next + step;
			return current;
		}

		@Override
		public boolean contains(int i)
		{
			boolean result = (i >= next) && (i < upperBound) && ((i - next) % step) == 0;
			next = upperBound;
			return result;
		}

		@Override
		public long count()
		{
			if (next >= upperBound) {
				return 0;
			}
			long result = ((long)upperBound - (long)next - 1) / step + 1;
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
		public long sum()
		{
			if (next >= upperBound) {
				return 0;
			}
			// Sn = a + (a+d) + (a+2d) + ... + (a+(n-1)d)
			// Sn = n(2a+(n-1)d)/2 
			long count = count();
			long result = count * (2*next + (count-1)*step)/2;
			next = upperBound;
			return result;
		}
	}



	/**
	 * Simple test method
	 */
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

		interval = new Interval(-5, -3);
		IteratorTools.printIterator("Interval(-5, -3)", interval.iterator());
		System.out.println("count = "+interval.count());
		System.out.println("sum   = "+interval.sum());
}
}
