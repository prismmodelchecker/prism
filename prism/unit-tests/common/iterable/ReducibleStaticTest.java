package common.iterable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static common.iterable.Assertions.assertIterableEquals;
import static common.iterable.Assertions.assertIteratorEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.*;

class ReducibleStaticTest
{
	static Stream<Iterable<Object>> getIterables()
	{
		return Stream.of(Collections.singleton(null),
		                 Collections.emptyList(),
		                 Collections.singleton("one"),
		                 Arrays.asList("one", "two", "three"));
	}

	static <T> Stream<Iterable<T>> getIterablesNull()
	{
		return Stream.of(Collections.singleton(null));
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
	@DisplayName("extend() yields same sequence as the underlying iterable.")
	void testExtendIterable(Iterable<?> iterable)
	{
		FunctionalIterable<?> actual = Reducible.extend(iterable);
		assertIterableEquals(iterable, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	@DisplayName("extend() yields same sequence as the underlying iterator.")
	void testExtendIterator(Iterable<?> iterable)
	{
		Iterator<?> expected = iterable.iterator();
		FunctionalIterator<?> actual = Reducible.extend(iterable.iterator());
		assertIteratorEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	@DisplayName("extend() does not extend a FunctionalIterable.")
	void testExtendFunctionalIterable(Iterable<?> iterable)
	{
		Iterable<?> expected = Reducible.extend(iterable);
		FunctionalIterable<?> actual = Reducible.extend(expected);
		assertSame(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterables")
	@DisplayName("extend() does not extend a FunctionalIterator.")
	void testExtendFunctionalIterator(Iterable<?> iterable)
	{
		Iterator<?> expected = Reducible.extend(iterable.iterator());
		FunctionalIterator<?> actual = Reducible.extend(expected);
		assertSame(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterablesDouble")
	@DisplayName("extend() does not extend a FunctionalPrimitiveIterable.OfDouble.")
	void testExtendFunctionalPrimitiveIterableOfDouble(Iterable<Double> iterable)
	{
		PrimitiveIterable.OfDouble expected = Reducible.extend(PrimitiveIterable.unboxDouble(iterable));
		FunctionalIterable<Double> functional = Reducible.extend(expected);
		assertSame(expected, functional);
	}

	@ParameterizedTest
	@MethodSource("getIterablesDouble")
	@DisplayName("extend() does not extend a FunctionalPrimitiveIterator.OfDouble.")
	void testExtendFunctionalPrimitiveIteratorOfDouble(Iterable<Double> iterable)
	{
		PrimitiveIterator.OfDouble expected = Reducible.extend(PrimitiveIterable.unboxDouble(iterable.iterator()));
		FunctionalIterator<Double> actual = Reducible.extend(expected);
		assertSame(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterablesInt")
	@DisplayName("extend() does not extend a FunctionalPrimitiveIterable.OfInt.")
	void testExtendFunctionalPrimitiveIterableOfInt(Iterable<Integer> iterable)
	{
		PrimitiveIterable.OfInt expected = Reducible.extend(PrimitiveIterable.unboxInt(iterable));
		FunctionalIterable<Integer> actual = Reducible.extend(expected);
		assertSame(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterablesInt")
	@DisplayName("extend() does not extend a FunctionalPrimitiveIterator.OfInt.")
	void testExtendFunctionalPrimitiveIteratorOfInt(Iterable<Integer> iterable)
	{
		PrimitiveIterator.OfInt expected = Reducible.extend(PrimitiveIterable.unboxInt(iterable.iterator()));
		FunctionalIterator<Integer> actual = Reducible.extend(expected);
		assertSame(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterablesLong")
	@DisplayName("extend() does not extend a FunctionalPrimitiveIterable.OfLong.")
	void testExtendFunctionalPrimitiveIterableOfLong(Iterable<Long> iterable)
	{
		PrimitiveIterable.OfLong expected = Reducible.extend(PrimitiveIterable.unboxLong(iterable));
		FunctionalIterable<Long> actual = Reducible.extend(expected);
		assertSame(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterablesLong")
	@DisplayName("extend() does not extend a FunctionalPrimitiveIterator.OfLong.")
	void testExtendFunctionalPrimitiveIteratorOfLong(Iterable<Long> iterable)
	{
		PrimitiveIterator.OfLong expected = Reducible.extend(PrimitiveIterable.unboxLong(iterable).iterator());
		FunctionalIterator<Long> actual = Reducible.extend(expected);
		assertSame(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterablesDouble")
	@DisplayName("extend() extends OfDouble to FunctionalPrimitiveIterable.")
	void testExtendPrimitiveIterableOfDouble(Iterable<Double> iterable)
	{
		Iterable<Double> primitive = PrimitiveIterable.unboxDouble(iterable);
		FunctionalIterable<Double> actual = Reducible.extend(primitive);
		assertTrue(actual instanceof FunctionalPrimitiveIterable);
	}

	@ParameterizedTest
	@MethodSource("getIterablesDouble")
	@DisplayName("extend() extends OfDouble to FunctionalPrimitiveIterator.")
	void testExtendPrimitiveIteratorOfDouble(Iterable<Double> iterable)
	{
		Iterator<Double> primitive = PrimitiveIterable.unboxDouble(iterable).iterator();
		FunctionalIterator<Double> actual = Reducible.extend(primitive);
		assertTrue(actual instanceof FunctionalPrimitiveIterator);
	}

	@ParameterizedTest
	@MethodSource("getIterablesInt")
	@DisplayName("extend() extends OfInt to FunctionalPrimitiveIterable.")
	void testExtendPrimitiveIterableOfInt(Iterable<Integer> iterable)
	{
		Iterable<Integer> primitive = PrimitiveIterable.unboxInt(iterable);
		FunctionalIterable<Integer> actual = Reducible.extend(primitive);
		assertTrue(actual instanceof FunctionalPrimitiveIterable);
	}

	@ParameterizedTest
	@MethodSource("getIterablesInt")
	@DisplayName("extend() extends OfInt to FunctionalPrimitiveIterator.")
	void testExtendPrimitiveIteratorOfInt(Iterable<Integer> iterable)
	{
		Iterator<Integer> primitive = PrimitiveIterable.unboxInt(iterable.iterator());
		FunctionalIterator<Integer> actual = Reducible.extend(primitive);
		assertTrue(actual instanceof FunctionalPrimitiveIterator);
	}

	@ParameterizedTest
	@MethodSource("getIterablesLong")
	@DisplayName("extend() extends OfLong to FunctionalPrimitiveIterable.")
	void testExtendPrimitiveIterableOfLong(Iterable<Long> iterable)
	{
		Iterable<Long> primitive = PrimitiveIterable.unboxLong(iterable);
		FunctionalIterable<Long> actual = Reducible.extend(primitive);
		assertTrue(actual instanceof FunctionalPrimitiveIterable);
	}

	@ParameterizedTest
	@MethodSource("getIterablesLong")
	@DisplayName("extend() extends OfLong to FunctionalPrimitiveIterator.")
	void testExtendPrimitiveIteratorOfLong(Iterable<Long> iterable)
	{
		Iterator<Long> primitive = PrimitiveIterable.unboxLong(iterable).iterator();
		FunctionalIterator<Long> actual = Reducible.extend(primitive);
		assertTrue(actual instanceof FunctionalPrimitiveIterator);
	}

	@Test
	@DisplayName("extend() with null throws NullPointerException.")
	void testExtendIterable_Null()
	{
		assertThrows(NullPointerException.class, () -> Reducible.extend((Iterable<?>) null));
	}

	@Test
	@DisplayName("extend() with null throws NullPointerException.")
	void testExtendIterator_Null()
	{
		assertThrows(NullPointerException.class, () -> Reducible.extend((Iterator<?>) null));
	}

	@ParameterizedTest
	@MethodSource("getIterablesDouble")
	@DisplayName("unboxDouble() yields same sequence as the underlying iterable.")
	void testUnboxDoubleIterable(Iterable<Double> iterable)
	{
		PrimitiveIterable.OfDouble expected = PrimitiveIterable.unboxDouble(iterable);
		FunctionalPrimitiveIterable.OfDouble actual = Reducible.unboxDouble(iterable);
		assertIterableEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterablesDouble")
	@DisplayName("unboxDouble() yields same sequence as the underlying iterator.")
	void testUnboxDoubleIterator(Iterable<Double> iterable)
	{
		PrimitiveIterator.OfDouble expected = PrimitiveIterable.unboxDouble(iterable.iterator());
		FunctionalPrimitiveIterator.OfDouble actual = Reducible.unboxDouble(iterable.iterator());
		assertIteratorEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterablesDouble")
	@DisplayName("unboxDouble() does not unbox a FunctionalPrimitiveIterable.OfDouble.")
	void testUnboxDoubleIterableOfDouble(Iterable<Double> iterable)
	{
		PrimitiveIterable.OfDouble expected = Reducible.extend(PrimitiveIterable.unboxDouble(iterable));
		FunctionalPrimitiveIterable.OfDouble actual = Reducible.unboxDouble(expected);
		assertSame(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterablesDouble")
	@DisplayName("unboxDouble() does not unbox a FunctionalPrimitiveIterator.OfDouble.")
	void testUnboxDoubleIteratorOfDouble(Iterable<Double> iterable)
	{
		PrimitiveIterator.OfDouble expected = Reducible.extend(PrimitiveIterable.unboxDouble(iterable.iterator()));
		FunctionalPrimitiveIterator.OfDouble actual = Reducible.unboxDouble(expected);
		assertSame(expected, actual);
	}

	@Test
	@DisplayName("unboxDouble() with null throws NullPointerException.")
	void testUnboxDoubleIterable_Null()
	{
		assertThrows(NullPointerException.class, () -> Reducible.unboxDouble((Iterable<Double>) null));
	}

	@Test
	@DisplayName("unboxDouble() with null throws NullPointerException.")
	void testUnboxDoubleIterator_Null()
	{
		assertThrows(NullPointerException.class, () -> Reducible.unboxDouble((Iterator<Double>) null));
	}

	@ParameterizedTest
	@MethodSource("getIterablesNull")
	@DisplayName("unboxDouble() with an Iterable containing null throws NullPointerException.")
	void testUnboxDoubleIterable_NullValues(Iterable<Double> iterable)
	{
		FunctionalPrimitiveIterable.OfDouble primitive = Reducible.unboxDouble(iterable);
		assertThrows(NullPointerException.class, primitive::consume);
	}

	@ParameterizedTest
	@MethodSource("getIterablesNull")
	@DisplayName("unboxDouble() with an Iterator containing null throws NullPointerException.")
	void testUnboxDoubleIterator_NullValues(Iterable<Double> iterable)
	{
		FunctionalPrimitiveIterator.OfDouble primitive = Reducible.unboxDouble(iterable.iterator());
		assertThrows(NullPointerException.class, primitive::consume);
	}

	@ParameterizedTest
	@MethodSource("getIterablesInt")
	@DisplayName("unboxInt() yields same sequence as the underlying iterable.")
	void testUnboxIntIterable(Iterable<Integer> iterable)
	{
		PrimitiveIterable.OfInt expected = PrimitiveIterable.unboxInt(iterable);
		FunctionalPrimitiveIterable.OfInt actual = Reducible.unboxInt(iterable);
		assertIterableEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterablesInt")
	@DisplayName("unboxInt() yields same sequence as the underlying iterator.")
	void testUnboxIntIterator(Iterable<Integer> iterable)
	{
		PrimitiveIterator.OfInt expected = PrimitiveIterable.unboxInt(iterable.iterator());
		FunctionalPrimitiveIterator.OfInt actual = Reducible.unboxInt(iterable.iterator());
		assertIteratorEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterablesInt")
	@DisplayName("unboxInt() does not unbox a FunctionalPrimitiveIterable.OfInt.")
	void testUnboxIntIterableOfInt(Iterable<Integer> iterable)
	{
		PrimitiveIterable.OfInt expected = Reducible.extend(PrimitiveIterable.unboxInt(iterable));
		FunctionalPrimitiveIterable.OfInt actual = Reducible.unboxInt(expected);
		assertSame(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterablesInt")
	@DisplayName("unboxInt() does not unbox a FunctionalPrimitiveIterator.OfInt.")
	void testUnboxIntIteratorOfInt(Iterable<Integer> iterable)
	{
		PrimitiveIterator.OfInt expected = Reducible.extend(PrimitiveIterable.unboxInt(iterable.iterator()));
		FunctionalPrimitiveIterator.OfInt actual = Reducible.unboxInt(expected);
		assertSame(expected, actual);
	}

	@Test
	@DisplayName("unboxInt() with null throws NullPointerException.")
	void testUnboxIntIterable_Null()
	{
		assertThrows(NullPointerException.class, () -> Reducible.unboxInt((Iterable<Integer>) null));
	}

	@Test
	@DisplayName("unboxInt() with null throws NullPointerException.")
	void testUnboxIntIterator_Null()
	{
		assertThrows(NullPointerException.class, () -> Reducible.unboxInt((Iterator<Integer>) null));
	}

	@ParameterizedTest
	@MethodSource("getIterablesNull")
	@DisplayName("unboxInt() with an Iterable containing null throws NullPointerException.")
	void testUnboxIntIterable_NullValues(Iterable<Integer> iterable)
	{
		FunctionalPrimitiveIterable.OfInt primitive = Reducible.unboxInt(iterable);
		assertThrows(NullPointerException.class, primitive::consume);
	}

	@ParameterizedTest
	@MethodSource("getIterablesNull")
	@DisplayName("unboxInt() with an Iterator containing null throws NullPointerException.")
	void testUnboxIntIterator_NullValues(Iterable<Integer> iterable)
	{
		FunctionalPrimitiveIterator.OfInt primitive = Reducible.unboxInt(iterable.iterator());
		assertThrows(NullPointerException.class, primitive::consume);
	}

	@ParameterizedTest
	@MethodSource("getIterablesLong")
	@DisplayName("unboxLong() yields same sequence as the underlying iterable.")
	void testUnboxLongIterable(Iterable<Long> iterable)
	{
		PrimitiveIterable.OfLong expected = PrimitiveIterable.unboxLong(iterable);
		FunctionalPrimitiveIterable.OfLong actual = Reducible.unboxLong(iterable);
		assertIterableEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterablesLong")
	@DisplayName("unboxLong() yields same sequence as the underlying iterator.")
	void testUnboxLong(Iterable<Long> iterable)
	{
		PrimitiveIterator.OfLong expected = PrimitiveIterable.unboxLong(iterable.iterator());
		FunctionalPrimitiveIterator.OfLong actual = Reducible.unboxLong(iterable.iterator());
		assertIteratorEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterablesLong")
	@DisplayName("unboxLong() does not unbox a FunctionalPrimitiveIterable.OfDouble.")
	void testUnboxLongIterableOfLong(Iterable<Long> iterable)
	{
		PrimitiveIterable.OfLong expected = Reducible.extend(PrimitiveIterable.unboxLong(iterable));
		FunctionalPrimitiveIterable.OfLong actual = Reducible.unboxLong(expected);
		assertSame(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getIterablesLong")
	@DisplayName("unboxLong() does not unbox a FunctionalPrimitiveIterator.OfDouble.")
	void testUnboxLongIteratorOfLong(Iterable<Long> iterable)
	{
		PrimitiveIterator.OfLong expected = Reducible.extend(PrimitiveIterable.unboxLong(iterable.iterator()));
		FunctionalPrimitiveIterator.OfLong actual = Reducible.unboxLong(expected);
		assertSame(expected, actual);
	}

	@Test
	@DisplayName("unboxLong() with null throws NullPointerException.")
	void testUnboxLongIterable_Null()
	{
		assertThrows(NullPointerException.class, () -> Reducible.unboxLong((Iterable<Long>) null));
	}

	@Test
	@DisplayName("unboxLong() with null throws NullPointerException.")
	void testUnboxLongIterator_Null()
	{
		assertThrows(NullPointerException.class, () -> Reducible.unboxLong((Iterator<Long>) null));
	}

	@ParameterizedTest
	@MethodSource("getIterablesNull")
	@DisplayName("unboxInt() with an Iterable containing null throws NullPointerException.")
	void testUnboxLongIterable_NullValues(Iterable<Long> iterable)
	{
		FunctionalPrimitiveIterable.OfLong primitive = Reducible.unboxLong(iterable);
		assertThrows(NullPointerException.class, primitive::consume);
	}

	@ParameterizedTest
	@MethodSource("getIterablesNull")
	@DisplayName("unboxInt() with an Iterator containing null throws NullPointerException.")
	void testUnboxLongIterator_NullValues(Iterable<Long> iterable)
	{
		FunctionalPrimitiveIterator.OfLong primitive = Reducible.unboxLong(iterable.iterator());
		assertThrows(NullPointerException.class, primitive::consume);
	}
}
