package common.iterable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static common.iterable.Assertions.assertIteratorEquals;
import static org.junit.jupiter.api.Assertions.*;

class FunctionalIteratorStaticTest
{
	static Stream<Iterable<Object>> getIterables()
	{
		return Stream.of(Collections.singleton(null),
				Collections.emptyList(),
				Collections.singleton("one"),
				Arrays.asList("one", "two", "three"));
	}

	static Stream<Iterable<Double>> getIterablesDouble()
	{
		return Stream.of(Collections.emptyList(),
				Collections.singleton(1.0),
				Arrays.asList(1.0, 2.0, 3.0));
	}

	static Stream<Iterable<Integer>> getIterablesInt()
	{
		return Stream.of(Collections.emptyList(),
				Collections.singleton(1),
				Arrays.asList(1, 2, 3));
	}

	static Stream<Iterable<Long>> getIterablesLong()
	{
		return Stream.of(Collections.emptyList(),
				Collections.singleton(1L),
				Arrays.asList(1L, 2L, 3L));
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	void testOf(Iterable<Object> iterable)
	{
		FunctionalIterable<Object> expected = Reducible.extend(iterable);
		Object[] arguments = new Object[Math.toIntExact(expected.count())];
		expected.collect(arguments);
		FunctionalIterator<Object> actual = FunctionalIterator.of(arguments);
		assertIteratorEquals(expected.iterator(), actual);
	}

	@Test
	void testOfTypes()
	{
		assertTrue(FunctionalIterator.of() instanceof EmptyIterator.Of);
		assertTrue(FunctionalIterator.of("first") instanceof SingletonIterator.Of);
	}

	@Test
	void testOf_Null()
	{
		assertThrows(NullPointerException.class, () -> FunctionalIterator.of((Object[]) null));
	}

	@ParameterizedTest
	@MethodSource("getIterablesDouble")
	void testOfDouble(Iterable<Double> iterable)
	{
		FunctionalPrimitiveIterable.OfDouble expected = Reducible.unboxDouble(iterable);
		double[] arguments = new double[Math.toIntExact(expected.count())];
		expected.collect(arguments);
		FunctionalPrimitiveIterator.OfDouble actual = FunctionalIterator.ofDouble(arguments);
		assertIteratorEquals(expected.iterator(), actual);
	}

	@Test
	static void testOfDoubleTypes()
	{
		assertTrue(FunctionalIterator.ofDouble() instanceof EmptyIterator.OfDouble);
		assertTrue(FunctionalIterator.ofDouble(1.0) instanceof SingletonIterator.Of);
	}

	@Test
	void testOfDouble_Null()
	{
		assertThrows(NullPointerException.class, () -> FunctionalIterator.ofDouble((double[]) null));
	}

	@ParameterizedTest
	@MethodSource("getIterablesInt")
	void testOfInt(Iterable<Integer> iterable)
	{
		FunctionalPrimitiveIterable.OfInt expected = Reducible.unboxInt(iterable);
		int[] arguments = new int[Math.toIntExact(expected.count())];
		expected.collect(arguments);
		FunctionalPrimitiveIterator.OfInt actual = FunctionalIterator.ofInt(arguments);
		assertIteratorEquals(expected.iterator(), actual);
	}

	@Test
	void testOfIntTypes()
	{
		assertTrue(FunctionalIterator.ofInt() instanceof EmptyIterator.OfInt);
		assertTrue(FunctionalIterator.ofInt(1) instanceof SingletonIterator.OfInt);
	}

	@Test
	void testOfInt_Null()
	{
		assertThrows(NullPointerException.class, () -> FunctionalIterator.ofInt((int[]) null));
	}

	@ParameterizedTest
	@MethodSource("getIterablesLong")
	void testOfLong(Iterable<Long> iterable)
	{
		FunctionalPrimitiveIterable.OfLong expected = Reducible.unboxLong(iterable);
		long[] arguments = new long[Math.toIntExact(expected.count())];
		expected.collect(arguments);
		FunctionalPrimitiveIterator.OfLong actual = FunctionalIterator.ofLong(arguments);
		assertIteratorEquals(expected.iterator(), actual);
	}

	@Test
	void testOfLongTypes()
	{
		assertTrue(FunctionalIterator.ofLong() instanceof EmptyIterator.OfLong);
		assertTrue(FunctionalIterator.ofLong(1L) instanceof SingletonIterator.OfLong);
	}

	@Test
	void testOfLong_Null()
	{
		assertThrows(NullPointerException.class, () -> FunctionalIterator.ofLong((long[]) null));
	}
}
