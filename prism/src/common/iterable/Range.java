package common.iterable;

import java.util.OptionalInt;

/**
 * An Iterable that yields all integers between two values, first and last (both inclusive).
 * The step width can be customized and defaults to 1.
 * <p>
 * If {@code step > 0} then the sequence is <br>
 * <em>ascending</em> (start, start+1, ... , stop).<br>
 * otherwise<br>
 * <em>descending</em> (start, start-1, ... , stop).<br>
 */
public class Range implements FunctionalPrimitiveIterable.OfInt
{
	protected final int first; // inclusive
	protected final int last;  // inclusive
	protected final int step;

	/**
	 * Factory method for a range from {@code 0} to {@code stop} (both inclusive)
	 * with step width {@code 1}.
	 *
	 * @param stop last {@code int}, inclusive
	 * @throws ArithmeticException if enumerating the Range would cause an integer over- or underflow
	 */
	public static Range closed(int stop)
	{
		return closed(0, stop, 1);
	}

	/**
	 * Factory method for a range from {@code start} to {@code stop} (both inclusive)
	 * with step width {@code 1}.
	 *
	 * @param start first {@code int}, inclusive
	 * @param stop last {@code int}, inclusive
	 * @throws ArithmeticException if enumerating the Range would cause an integer over- or underflow
	 */
	public static Range closed(int start, int stop)
	{
		return closed(start, stop, 1);
	}

	/**
	 * Factory method for a range from {@code start} (inclusive) to {@code stop} (inclusive it it is a step)
	 * with step width {@code step}.
	 *
	 * @param start first {@code int}, inclusive
	 * @param stop last {@code int}, inclusive if it is a step
	 * @param step an {@code int != 0} giving the step width
	 * @throws ArithmeticException if enumerating the Range would cause an integer over- or underflow
	 */
	public static Range closed(int start, int stop, int step)
	{
		return new Range(start, stop, step, true);
	}



	/**
	 * Constructor for a range from {@code 0} (inclusive) to {@code stop} (exclusive)
	 * with step width 1.
	 *
	 * @param stop last {@code int}, exclusive
	 * @throws ArithmeticException if enumerating the Range would cause an integer over- or underflow
	 */
	public Range(int stop)
	{
		this(0, stop);
	}

	/**
	 * Constructor for a range from {@code start} (inclusive) to {@code stop} (exclusive)
	 * with step width 1.
	 *
	 * @param start first {@code int}, inclusive
	 * @param stop last {@code int}, exclusive
	 * @throws ArithmeticException if enumerating the Range would cause an integer over- or underflow
	 */
	public Range(int start, int stop)
	{
		this(start, stop, 1);
	}

	/**
	 * Constructor for a range from {@code start} (inclusive) to {@code stop} (exclusive)
	 * with step width {@code step}.
	 *
	 * @param start first {@code int}, inclusive
	 * @param stop last {@code int}, exclusive
	 * @param step an {@code int != 0} giving the step width
	 * @throws ArithmeticException if enumerating the Range would cause an integer over- or underflow
	 */
	public Range(int start, int stop, int step)
	{
		this(start, stop, step, false);
	}

	/**
	 * Constructor for a range from {@code start} (inclusive) to {@code stop} (inclusive or exclusive)
	 * with step width {@code step}.
	 * If {@code step > 0} the range is ascending and otherwise descending.
	 *
	 * @param start first {@code int}, inclusive
	 * @param stop last {@code int}, inclusive if it is a step and {@code closed == true}
	 * @param step an {@code int != 0}
	 * @param closed flag whether {@code stop} is inclusive ({@code true}) or not ({@code false})
	 * @throws ArithmeticException if enumerating the Range would cause an integer over- or underflow
	 */
	private Range(int start, int stop, int step, boolean closed)
	{
		if (step == 0) {
			throw new IllegalArgumentException("Expected: step != 0");
		}
		// convert to inclusive bound
		if (!closed) {
			stop = Math.addExact(stop, (step > 0) ? -1 : +1);
		}
		// normalize parameters
		long distance = (long) stop - (long) start;
		if ((step > 0 && distance < 0) || (step < 0 && distance > 0)) {
			// empty range
			this.first = 0;
			this.last = -1;
			this.step = 1;
		} else {
			// non-empty range
			this.first = start;
			// calculate last element
			this.last = (int) (start + (distance / step) * step);
			// set step = 1 for singleton ranges
			this.step = (this.first == this.last) ? 1 : step;
		}
		// check for over- and underflows
		checkForOverAndUnderflows();
	}

	/**
	 * Check that the iteration does not over- or underflow at Integer.MAX_VALUE or Integer.MIN_VALUE.
	 */
	private void checkForOverAndUnderflows()
	{
		if (isAscending()) {
			if (last + step < last) {
				throw new ArithmeticException("integer overflow");
			}
		} else {
			if (last + step > last) {
				throw new ArithmeticException("integer underflow");
			}
		}
	}

	/**
	 * Answer whether the receiver is in ascending order.
	 * Empty and singleton ranges are ascending by default.
	 */
	public boolean isAscending()
	{
		return step > 0;
	}

	/**
	 * Answer whether the receiver contains exactly one number.
	 */
	public boolean isSingleton()
	{
		return first == last;
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
		// Empty ranges are ascending by default.
		return isAscending() && first > last;
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
	public abstract class RangeIterator implements FunctionalPrimitiveIterator.OfInt
	{
		protected int next;

		/**
		 * Constructor for a range Iterator.
		 */
		public RangeIterator()
		{
			assert step != 0 : "Expected: step != 0";
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
			long k = next; // store next, since count() exhausts the iterator
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
			assert step > 0 : "Expected: step > 0";
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
			assert step < 0 : "Expected: step < 0";
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
			return min;
		}
	}
}
