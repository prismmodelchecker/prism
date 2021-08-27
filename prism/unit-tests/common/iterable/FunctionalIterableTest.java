package common.iterable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.function.Supplier;

import static common.iterable.PrimitiveIterable.*;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.*;

public interface FunctionalIterableTest<E, T extends FunctionalIterable<E>> extends ReducibleTest<E, T>
{
	@ParameterizedTest
	@MethodSource({"getReducibles"})
	@Override
	default void testConcat(Supplier<T> supplier)
	{
		ArrayList<Object> expected = supplier.get().collect(new ArrayList<>());
		supplier.get().collect(expected);
		FunctionalIterable<E> actual = supplier.get().concat(supplier.get());
		assertIterableEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	@Override
	default void testFlatMap(Supplier<T> supplier)
	{
		FunctionalIterable<String> expected = supplier.get().map(String::valueOf);
		FunctionalIterable<String> actual = supplier.get().flatMap(e -> new SingletonIterable.Of<>(String.valueOf(e)));
		assertIterableEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	@Override
	default void testFlatMapToDouble(Supplier<T> supplier)
	{
		Range range = new Range((int) supplier.get().count());
		PrimitiveIterable.OfDouble expected = unboxDouble(range.map((int i) -> (double) i));
		Range.RangeIterator index = range.iterator();
		PrimitiveIterable.OfDouble actual = supplier.get().flatMapToDouble(e -> new SingletonIterable.OfDouble(index.next()));
		assertIterableEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	@Override
	default void testFlatMapToInt(Supplier<T> supplier)
	{
		Range expected = new Range((int) supplier.get().count());
		Range.RangeIterator index = expected.iterator();
		PrimitiveIterable.OfInt actual = supplier.get().flatMapToInt(e -> new SingletonIterable.OfInt(index.next()));
		assertIterableEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	@Override
	default void testFlatMapToLong(Supplier<T> supplier)
	{
		Range range = new Range((int) supplier.get().count());
		PrimitiveIterable.OfLong expected = unboxLong(range.map((int i) -> (long) i));
		Range.RangeIterator index = range.iterator();
		PrimitiveIterable.OfLong actual = supplier.get().flatMapToLong(e -> new SingletonIterable.OfLong(index.next()));
		assertIterableEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getSingletonReducibles")
	@Override
	default void testFlatMapToNull(Supplier<T> supplier)
	{
		assertThrows(NullPointerException.class, () -> supplier.get().flatMap(e -> null).consume());
		assertThrows(NullPointerException.class, () -> supplier.get().flatMapToDouble(e -> null).consume());
		assertThrows(NullPointerException.class, () -> supplier.get().flatMapToInt(e -> null).consume());
		assertThrows(NullPointerException.class, () -> supplier.get().flatMapToLong(e -> null).consume());
	}

	@Test
	@Override
	default void testFlatMap_Null()
	{
		FunctionalIterable<E> iterator = getAnyReducible();
		assertThrows(NullPointerException.class, () -> iterator.flatMap(null));
		assertThrows(NullPointerException.class, () -> iterator.flatMapToDouble(null));
		assertThrows(NullPointerException.class, () -> iterator.flatMapToInt(null));
		assertThrows(NullPointerException.class, () -> iterator.flatMapToLong(null));
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	@DisplayName("forEach yields same sequence as the underlying iterator.")
	@Override
	default void testForEach(Supplier<T> supplier)
	{
		T expected = supplier.get();
		List<Object> actual = new ArrayList<>();
		supplier.get().forEach(actual::add);
		assertIterableEquals(expected, actual);
	}



	interface Of<E, T extends FunctionalIterable<E>> extends FunctionalIterableTest<E, T>, ReducibleTest.Of<E, T>
	{
	}
}
