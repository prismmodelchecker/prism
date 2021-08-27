package common.iterable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static common.iterable.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.*;

class FunctionalIterableStaticTest
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
		FunctionalIterable<Object> actual = FunctionalIterable.of(arguments);
		assertIterableEquals(expected, actual);
	}

	@Test
	void testOfTypes()
	{
		assertTrue(FunctionalIterable.of() instanceof EmptyIterable.Of);
		assertTrue(FunctionalIterable.of("first") instanceof SingletonIterable.Of);
	}

	@Test
	void testOf_Null()
	{
		assertThrows(NullPointerException.class, () -> FunctionalIterable.of((Object[]) null));
	}

	@ParameterizedTest
	@MethodSource("getIterablesDouble")
	void testOfDouble(Iterable<Double> iterable)
	{
		FunctionalPrimitiveIterable.OfDouble expected = Reducible.unboxDouble(iterable);
		double[] arguments = new double[Math.toIntExact(expected.count())];
		expected.collect(arguments);
		FunctionalPrimitiveIterable.OfDouble actual = FunctionalIterable.ofDouble(arguments);
		assertIterableEquals(expected, actual);
	}

	@Test
	static void testOfDoubleTypes()
	{
		assertTrue(FunctionalIterable.ofDouble() instanceof EmptyIterable.OfDouble);
		assertTrue(FunctionalIterable.ofDouble(1.0) instanceof SingletonIterable.Of);
	}

	@Test
	void testOfDouble_Null()
	{
		assertThrows(NullPointerException.class, () -> FunctionalIterable.ofDouble((double[]) null));
	}

	@ParameterizedTest
	@MethodSource("getIterablesInt")
	void testOfInt(Iterable<Integer> iterable)
	{
		FunctionalPrimitiveIterable.OfInt expected = Reducible.unboxInt(iterable);
		int[] arguments = new int[Math.toIntExact(expected.count())];
		expected.collect(arguments);
		FunctionalPrimitiveIterable.OfInt actual = FunctionalIterable.ofInt(arguments);
		assertIterableEquals(expected, actual);
	}

	@Test
	void testOfIntTypes()
	{
		assertTrue(FunctionalIterable.ofInt() instanceof EmptyIterable.OfInt);
		assertTrue(FunctionalIterable.ofInt(1) instanceof SingletonIterable.OfInt);
	}

	@Test
	void testOfInt_Null()
	{
		assertThrows(NullPointerException.class, () -> FunctionalIterable.ofInt((int[])null));
	}

	@ParameterizedTest
	@MethodSource("getIterablesLong")
	void testOfLong(Iterable<Long> iterable)
	{
		FunctionalPrimitiveIterable.OfLong expected = Reducible.unboxLong(iterable);
		long[] arguments = new long[Math.toIntExact(expected.count())];
		expected.collect(arguments);
		FunctionalPrimitiveIterable.OfLong actual = FunctionalIterable.ofLong(arguments);
		assertIterableEquals(expected, actual);
	}

	@Test
	void testOfLongTypes()
	{
		assertTrue(FunctionalIterable.ofLong() instanceof EmptyIterable.OfLong);
		assertTrue(FunctionalIterable.ofLong(1L) instanceof SingletonIterable.OfLong);
	}

	@Test
	void testOfLong_Null()
	{
		assertThrows(NullPointerException.class, () -> FunctionalIterable.ofLong((long[]) null));
	}
}
