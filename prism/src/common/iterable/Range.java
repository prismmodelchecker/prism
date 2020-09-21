package common.iterable;

import java.util.OptionalInt;

import common.IteratorTools;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterator.OfInt;

/**
 * An Iterable that yields all integers between two values, first and last (both inclusive).
 * The step width can be customized and defaults to 1.
 * <p>
 * If {@code step > 0} then the sequence is <br>
 * <em>ascending</em> (first, first+1, ... , last).<br>
 * otherwise<br>
 * <em>descending</em> (first, first-1, ... , last).<br>
 */
public class Range implements IterableInt
{
	protected final int first; // inclusive
	protected final int last;  // inclusive
	protected final int step;

	/**
	 * Factory method for a range that includes the last element.
	 *
	 * @param bound last {@code int}, inclusive
	 */
	public static Range closed(int bound)
	{
		return closed(0, bound, 1);
	}

	/**
	 * Factory method for a range that includes the last element.
	 *
	 * @param first first {@code int}, inclusive
	 * @param bound last {@code int}, inclusive
	 */
	public static Range closed(int first, int bound)
	{
		return closed(first, bound, 1);
	}

	/**
	 * Factory method for a range that includes the last element if it is a step.
	 *
	 * @param first first {@code int}, inclusive
	 * @param bound last {@code int}, inclusive
	 * @param step a positive {@code int}
	 */
	public static Range closed(int first, int bound, int step)
	{
		return new Range(first, bound, step, true);
	}



	/**
	 * Constructor for a range from {@code 0} (inclusive) to {@code last} (exclusive)
	 * with step width 1.
	 *
	 * @param bound last {@code int}, exclusive
	 */
	public Range(int bound)
	{
		this(0, bound);
	}

	/**
	 * Constructor for a range from {@code first} (inclusive) to {@code last} (exclusive)
	 * with step width 1.
	 *
	 * @param first first {@code int}, inclusive
	 * @param bound last {@code int}, exclusive
	 */
	public Range(int first, int bound)
	{
		this(first, bound, 1);
	}

	/**
	 * Constructor for a range from {@code 0} (inclusive) to {@code last} (exclusive)
	 * with step width {@code step}.
	 *
	 * @param first first {@code int}, inclusive
	 * @param bound last {@code int}, exclusive
	 * @param step a positive {@code int}
	 */
	public Range(int first, int bound, int step)
	{
		this(first, bound, step, false);
	}

	/**
	 * Constructor for a range from {@code 0} (inclusive) to {@code last} (inclusive or exclusive)
	 * with step width {@code step}.
	 * If {@step > 0} the range is ascending and otherwise descending.
	 *
	 * @param first first {@code int}, inclusive
	 * @param bound last {@code int}, inclusive iff {@code closed == true}
	 * @param step an {@code int != 0}
	 * @param closed flag whether {@code last} is inclusive ({@code true}) or not ({@code false})
	 */
	public Range(int first, int bound, int step, boolean closed)
	{
		if (step == 0) {
			throw new IllegalArgumentException("Expected: step != 0");
		}
		// convert to inclusive bound
		if (!closed) {
			bound = (step > 0) ? bound - 1 : bound + 1;
		}
		// normalize parameters
		long distance = (long) bound - (long) first;
		if ((step > 0 && distance < 0) || (step < 0 && distance > 0)) {
			// empty range
			this.first = 0;
			this.last  = -1;
			this.step  = 1;
		} else {
			// non-empty range: calculate last element
			this.first = first;
			this.last  = (int) (first + (distance / step) * step);
			this.step  = step;
		}
	}

	/**
	 * Is this range in ascending order?
	 */
	public boolean isAscending()
	{
		return step > 0;
	}

	/**
	 * Return a new range with all elements in reverse order.
	 */
	public Range reversed()
	{
		return new Range(last, first, -step, true);
	}

	@Override
	public RangeIterator iterator()
	{
		if (isAscending()) {
			return new AscendingRangeIterator();
		} else {
			return new DescendingRangeIterator();
		}
	}

	@Override
	public boolean isEmpty()
	{
		return isAscending() ? first > last : first < last;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + ".closed(" + first + ", " + last + ", " + step + ")";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		if (isEmpty()) {
			return result;
		}
		result = prime * result + first;
		result = prime * result + last;
		result = prime * result + step;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Range)) {
			return false;
		}
		Range other = (Range) obj;
		if (first != other.first) {
			return false;
		}
		if (last != other.last) {
			return false;
		}
		if (step != other.step) {
			return false;
		}
		return true;
	}



	/**
	 * A abstract base class for an Iterator from {@code first} (inclusive) and {@code last} (exclusive) with a custom step width.
	 */
	protected abstract class RangeIterator implements OfInt
	{
		protected int next;

		/**
		 * Constructor for a range Iterator.
		 */
		public RangeIterator()
		{
			this.next = first;
		}

		@Override
		public int nextInt()
		{
			requireNext();
			int current = next;
			next += step;
			return current;
		}

		@Override
		public long count()
		{
			if (!hasNext()) {
				return 0;
			}
			long count = ((long) last - (long) next) / step + 1;
			release();
			return count;
		}

		@Override
		public long sum()
		{
			if (!hasNext()) {
				return 0;
			}
			// S(1..n) = k + (k+s) + (k+2s) + ... + (k+(n-1)s)
			// S(1..n) = n(2k + (n-1)s) / 2
			int k = next; // store next, since count() exhausts the iterator
			long count = count();
			long sum = count * (2 * k + (count - 1) * step) / 2;
			release();
			return sum;
		}

		@Override
		public void release()
		{
			next = last + step;
		}
	}



	/**
	 * Ascending Iterator from {@code first} (inclusive) and {@code last} (inclusive) with a custom step width.
	 */
	protected class AscendingRangeIterator extends RangeIterator
	{
		/**
		 * Constructor for an ascending Iterator.
		 */
		public AscendingRangeIterator()
		{
			super();
			if (step <= 0) {
				throw new IllegalArgumentException("Expected: step > 0");
			}
			if (last + step < last) {
				throw new ArithmeticException("integer overflow");
			}
		}

		@Override
		public boolean hasNext()
		{
			return next <= last;
		}

		@Override
		public boolean contains(int i)
		{
			// Is in interval?
			boolean inInterval = (i >= next) && (i <= last);
			// Is a step? Mind potential overflow!
			boolean contains = inInterval && (((long) i - (long) next) % step) == 0;
			release();
			return contains;
		}

		@Override
		public OptionalInt max()
		{
			if (!hasNext()) {
				return OptionalInt.empty();
			}
			OptionalInt max = OptionalInt.of(last);
			release();
			return max;
		}

		@Override
		public OptionalInt min()
		{
			if (!hasNext()) {
				return OptionalInt.empty();
			}
			OptionalInt min = OptionalInt.of(next);
			release();
			return min;
		}
	}



	/**
	 * Descending Iterator from {@code first} (inclusive) and {@code last} (inclusive) with a custom step width.
	 */
	protected class DescendingRangeIterator extends RangeIterator
	{
		/**
		 * Constructor for an descending Iterator.
		 */
		public DescendingRangeIterator()
		{
			super();
			if (step >= 0) {
				throw new IllegalArgumentException("Expected: step < 0");
			}
			if (last + step > last) {
				throw new ArithmeticException("integer underflow");
			}
		}

		@Override
		public boolean hasNext()
		{
			return next >= last;
		}

		@Override
		public boolean contains(int i)
		{
			// Is in interval?
			boolean inInterval = (i <= next) && (i >= last);
			// Is a step? Mind potential overflow!
			boolean contains = inInterval && (((long) i - (long) next) % step) == 0;
			release();
			return contains;
		}

		@Override
		public OptionalInt max()
		{
			if (!hasNext()) {
				return OptionalInt.empty();
			}
			OptionalInt max = OptionalInt.of(next);
			release();
			return max;
		}

		@Override
		public OptionalInt min()
		{
			if (!hasNext()) {
				return OptionalInt.empty();
			}
			OptionalInt min = OptionalInt.of(last);
			release();
			return min;		}
	}



	/**
	 * Simple test method
	 */
	public static void main(final String[] args)
	{
		Range range = new Range(-3, 5);
		IteratorTools.printIterator(range.toString(), range.iterator());
		System.out.println("min   = "+range.min());
		System.out.println("max   = "+range.max());
		System.out.println("count = "+range.count());
		System.out.println("sum   = "+range.sum());
		System.out.println();

		range = new Range(5, -3, -1);
		IteratorTools.printIterator(range.toString(), range.iterator());
		System.out.println("min   = "+range.min());
		System.out.println("max   = "+range.max());
		System.out.println("count = "+range.count());
		System.out.println("sum   = "+range.sum());
		System.out.println();

		range = new Range(-3, 3, 2);
		IteratorTools.printIterator(range.toString(), range.iterator());
		System.out.println("min   = "+range.min());
		System.out.println("max   = "+range.max());
		System.out.println("count = "+range.count());
		System.out.println("sum   = "+range.sum());
		System.out.println();

		range = new Range(3, -3, -2);
		IteratorTools.printIterator(range.toString(), range.iterator());
		System.out.println("min   = "+range.min());
		System.out.println("max   = "+range.max());
		System.out.println("count = "+range.count());
		System.out.println("sum   = "+range.sum());
		System.out.println();

		range = new Range(-3, 5, 3);
		IteratorTools.printIterator(range.toString(), range.iterator());
		System.out.println("min   = "+range.min());
		System.out.println("max   = "+range.max());
		System.out.println("count = "+range.count());
		System.out.println("sum   = "+range.sum());
		System.out.println();

		range = new Range(3, -5, -3);
		IteratorTools.printIterator(range.toString(), range.iterator());
		System.out.println("min   = "+range.min());
		System.out.println("max   = "+range.max());
		System.out.println("count = "+range.count());
		System.out.println("sum   = "+range.sum());
		System.out.println();

		range = new Range(-3, -3);
		IteratorTools.printIterator(range.toString(), range.iterator());
		System.out.println("min   = "+range.min());
		System.out.println("max   = "+range.max());
		System.out.println("count = "+range.count());
		System.out.println("sum   = "+range.sum());
	}
}
