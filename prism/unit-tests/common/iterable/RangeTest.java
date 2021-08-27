package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static common.iterable.PrimitiveReducibleTest.*;
import static org.junit.jupiter.api.Assertions.*;

class RangeTest implements FunctionalPrimitiveIterableTest.OfInt<Range>
{
	static Range asRange(Arguments args)
	{
		Object[] params = args.get();
		return new Range((Integer) params[0], (Integer) params[1], (Integer) params[2]);
	}

	static Stream<Arguments> getEmptyRangeArguments()
	{
		return Stream.of(Arguments.of(0, 0, 1),
				Arguments.of(0, 0, -1),
				Arguments.of(Integer.MAX_VALUE, Integer.MAX_VALUE, 1),
				Arguments.of(Integer.MIN_VALUE, Integer.MIN_VALUE, -1));
	}

	static Stream<Arguments> getSingletonRangeArguments()
	{
		return Stream.of(Arguments.of(0, 1, 1),
				Arguments.of(0, -1, -1),
				Arguments.of(Integer.MAX_VALUE-1, Integer.MAX_VALUE, 1),
				Arguments.of(Integer.MIN_VALUE+1, Integer.MIN_VALUE, -1));
	}

	static Stream<Arguments> getMultitonRangeArguments()
	{
		return Stream.of(Arguments.of(0, 10, 1),
				Arguments.of(0, -10, -1),
				Arguments.of(-10, 10, 3),
				Arguments.of(10, -10, -3),
				Arguments.of(Integer.MIN_VALUE, Integer.MIN_VALUE+20, 7),
				Arguments.of(Integer.MAX_VALUE, Integer.MAX_VALUE-20, -7));
	}

	@Override
	public Range getReducible(int[] arguments)
	{
		throw new RuntimeException("Should not be called");
	}

	@Override
	public Stream<Supplier<Range>> getDuplicatesReducibles()
	{
		return Stream.empty();
	}

	@Override
	public Stream<Supplier<Range>> getEmptyReducibles()
	{
		return getEmptyRangeArguments().map(args -> () -> asRange(args));
	}

	@Override
	public Stream<Supplier<Range>> getSingletonReducibles()
	{
		return getSingletonRangeArguments().map(args -> () -> asRange(args));
	}

	@Override
	public Stream<Supplier<Range>> getMultitonReducibles()
	{
		return getMultitonRangeArguments().map(args -> () -> asRange(args));
	}

	@Override
	public Iterable<Object> getExcluded(Range range)
	{
		List<Object> excluded = new ArrayList<>();
		excluded.addAll(getUniqueObjects());
		// add lower and upper bounds
		range.min().ifPresent(min -> {
			if (min > Integer.MIN_VALUE) {
				excluded.add(min - 1);
				excluded.add(Integer.MIN_VALUE);
			}
		});
		range.max().ifPresent(max -> {
			if (max < Integer.MAX_VALUE) {
				excluded.add(max + 1);
				excluded.add(Integer.MAX_VALUE);
			}
		});
		// add ints between first and last that are no steps
		if (range.isAscending()) {
			for (int i = range.first; i <= range.last; i++) {
				if ((i - range.first) % range.step != 0) {
					excluded.add((Integer) i);
				}
			}
		} else {
			for (int i = range.first; i >= range.last; i--) {
				if ((i - range.first) % range.step != 0) {
					excluded.add((Integer) i);
				}
			}
		}
		return excluded;
	}

	public static void assertEqualsClosedForLoop(int start, int stop, int step, Range actual)
	{
		assertEqualsClosedForLoop(start, stop, step, actual.iterator());
	}

	public static void assertEqualsOpenForLoop(int start, int stop, int step, Range actual)
	{
		assertEqualsOpenForLoop(start, stop, step, actual.iterator());
	}

	public static void assertEqualsClosedForLoop(int start, int stop, int step, PrimitiveIterator.OfInt actual)
	{
		if (step > 0) {
			// closed ascending loop: <=
			for (int i = start; i <= stop; i = Math.addExact(i, step)) {
				assertEquals(i, actual.nextInt());
			}
		} else if (step < 0) {
			// open descending loop: >=
			for (int i = start; i >= stop; i = Math.addExact(i, step)) {
				assertEquals(i, actual.nextInt());
			}
		} else {
			fail("expected step != 0");
		}
		assertFalse(actual.hasNext(), "Expected exhausted iterator");
	}

	public static void assertEqualsOpenForLoop(int start, int stop, int step, PrimitiveIterator.OfInt actual)
	{
		if (step > 0) {
			// open ascending loop: <
			for (int i = start; i < stop; i = Math.addExact(i, step)) {
				assertEquals(i, actual.nextInt());
			}
		} else if (step < 0) {
			// open descending loop: >
			for (int i = start; i > stop; i = Math.addExact(i, step)) {
				assertEquals(i, actual.nextInt());
			}
		} else {
			fail("expected step != 0");
		}
		assertFalse(actual.hasNext(), "Expected exhausted iterator");
	}

	/**
	 * Test method for {@link common.iterable.Range#closed(int)}.
	 */
	@ParameterizedTest
	@MethodSource({"getEmptyRangeArguments", "getSingletonRangeArguments", "getMultitonRangeArguments"})
	void testClosedInt(int start, int stop, int step)
	{
		if (step > 0 && start == 0) {
			// range with positive step width starting at 0
			// adjust stop to maybe be included
			int closed = (step > 0) ? stop - 1 : stop + 1;
			Range actual = Range.closed(closed);
			assertEqualsClosedForLoop(0, closed, 1, actual);
		}
	}

	/**
	 * Test method for {@link common.iterable.Range#closed(int, int)}.
	 */
	@ParameterizedTest
	@MethodSource({"getEmptyRangeArguments", "getSingletonRangeArguments", "getMultitonRangeArguments"})
	void testClosedIntInt(int start, int stop, int step)
	{
		if (step > 0) {
			// range with positive step width
			// adjust stop to maybe be included
			int closed = (step > 0) ? stop - 1 : stop + 1;
			Range actual = Range.closed(start, closed);
			assertEqualsClosedForLoop(start, closed, 1, actual);
		}
	}

	/**
	 * Test method for {@link common.iterable.Range#closed(int, int, int)}.
	 */
	@ParameterizedTest
	@MethodSource({"getEmptyRangeArguments", "getSingletonRangeArguments", "getMultitonRangeArguments"})
	void testClosedIntIntInt(int start, int stop, int step)
	{
		// adjust stop to maybe be included
		int closed = (step > 0) ? stop - 1 : stop + 1;
		Range actual = Range.closed(start, closed, step);
		assertEqualsClosedForLoop(start, closed, step, actual);
	}

	/**
	 * Test method for {@link common.iterable.Range#Range(int)}.
	 */
	@ParameterizedTest
	@MethodSource({"getEmptyRangeArguments", "getSingletonRangeArguments", "getMultitonRangeArguments"})
	void testRangeInt(int start, int stop, int step)
	{
		if (step > 0 && start == 0) {
			// range with positive step width starting at 0
			Range actual = new Range(stop);
			assertEqualsOpenForLoop(0, stop, 1, actual);
		}
	}

	/**
	 * Test method for {@link common.iterable.Range#Range(int, int)}.
	 */
	@ParameterizedTest
	@MethodSource({"getEmptyRangeArguments", "getSingletonRangeArguments", "getMultitonRangeArguments"})
	void testRangeIntInt(int start, int stop, int step)
	{
		if (step > 0) {
			// range with positive step width
			Range actual = new Range(start, stop);
			assertEqualsOpenForLoop(start, stop, 1, actual);
		}
	}

	/**
	 * Test method for {@link common.iterable.Range#Range(int, int, int, boolean)}.
	 */
	@ParameterizedTest
	@MethodSource({"getEmptyRangeArguments", "getSingletonRangeArguments", "getMultitonRangeArguments"})
	void testRangeIntIntInt(int start, int stop, int step)
	{
		Range actual = new Range(start, stop, step);
		assertEqualsOpenForLoop(start, stop, step, actual);
	}

	@Test
	void testRangeStepZero()
	{
		assertThrows(IllegalArgumentException.class, () -> new Range(1, 2, 0));
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	void testIsAscending(Supplier<Range> supplier)
	{
		Range range = supplier.get();
		if (range.isEmpty() || range.isSingleton()) {
			assertTrue(range.isAscending());
		} else if (range.step > 0) {
			assertTrue(range.isAscending());
		} else if(range.step < 0) {
			assertFalse(range.isAscending());
		} else {
			fail("expected step != 0");
		}
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	void testIsSingleton(Supplier<Range> supplier)
	{
		Range range = supplier.get();
		assertTrue(range.count() !=1 ^ range.isSingleton());
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	void testReversed(Supplier<Range> supplier)
	{
		Range range = supplier.get();
		if (range.first == Integer.MIN_VALUE && !range.isEmpty()) {
			// reverse() at min value throws
			assertThrows(ArithmeticException.class, range::reversed);
		} else if (range.first == Integer.MAX_VALUE && !range.isEmpty()) {
			// reverse() at min value throws
			assertThrows(ArithmeticException.class, range::reversed);
		} else {
			assertEqualsClosedForLoop(range.last, range.first, -range.step, range.reversed());
		}
	}

	/**
	 * Test method for {@link common.iterable.Range#toString()}.
	 */
	@ParameterizedTest
	@MethodSource("getReducibles")
	void testToString(Supplier<Range> supplier)
	{
		Range range = supplier.get();
		String expected = "Range.closed(" + range.first + ", " + range.last + ", " + range.step + ")";
		String actual = range.toString();
		assertEquals(expected, actual);
	}

	@Test
	void testEqualsAndHash()
	{
		Range range = Range.closed(-2, 4, 3); // {-2, 1, 4}

		// equal to itself
		assertEquals(range, range);

		// equal to a clone
		Range clone = Range.closed(-2, 4, 3);
		assertEquals(range, clone);
		assertEquals(range.hashCode(), clone.hashCode());

		// not equal to null or other type
		assertFalse(range.equals(null));
		assertFalse(range.equals("no"));

		// not equal to an arbitrary range
		Range otherStart = Range.closed(-5, 4, 3); // {-5, -2, 1, 4}
		assertNotEquals(otherStart, range);
		Range otherStop = Range.closed(-2, 7, 3); // {-2, 1, 4, 7}
		assertNotEquals(otherStop, range);
		Range otherStep = Range.closed(-2, 4, 2); // {-2, 0, 2, 4}
		assertNotEquals(otherStep, range);
	}

	@ParameterizedTest
	@MethodSource({"getEmptyRangeArguments"})
	void testEqualsAndHashEmpty(int start, int stop, int step)
	{
		Range expected = new Range(0);
		Range actual = new Range(start, stop, step);
		assertEquals(expected, actual);
		assertEquals(expected.hashCode(), actual.hashCode());
	}

	@ParameterizedTest
	@MethodSource({"getSingletonRangeArguments"})
	void testEqualsAndHashSingleton(int start, int stop, int step)
	{
		Range expected = new Range(start, stop, step > 0 ? 1 : -1);
		Range actual = new Range(start, stop, step);
		assertEquals(expected, actual);
		assertEquals(expected.hashCode(), actual.hashCode());
	}



	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class RangeIteratorTest implements FunctionalPrimitiveIteratorTest.OfInt<Range.RangeIterator>
	{
		@Override
		public Range.RangeIterator getReducible(int[] arguments)
		{
			throw new RuntimeException("Should not be called");
		}

		@Override
		public Stream<Supplier<Range.RangeIterator>> getDuplicatesReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<Range.RangeIterator>> getEmptyReducibles()
		{
			return getEmptyRangeArguments().map(args -> () -> asRange(args).iterator());
		}

		@Override
		public Stream<Supplier<Range.RangeIterator>> getSingletonReducibles()
		{
			return getSingletonRangeArguments().map(args -> () -> asRange(args).iterator());
		}

		@Override
		public Stream<Supplier<Range.RangeIterator>> getMultitonReducibles()
		{
			return getMultitonRangeArguments().map(args -> () -> asRange(args).iterator());
		}
	}
}
