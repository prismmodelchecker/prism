package common.iterable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.function.Supplier;

import static common.iterable.Assertions.assertIteratorEquals;
import static common.iterable.PrimitiveIterable.*;
import static org.junit.jupiter.api.Assertions.*;

public interface FunctionalIteratorTest<E, T extends FunctionalIterator<E>> extends ReducibleTest<E, T>
{
	/**
	 * Collect elements from an iterator without using method from Reducible.
	 *
	 * @param supplier the supplier yielding the iterator
	 * @return a {@link List} of the iterator elements
	 */
	default List<E> collectElements(Supplier<T> supplier)
	{
		ArrayList<E> elements = new ArrayList<>();
		for (Iterator<E> iterator = supplier.get(); iterator.hasNext();) {
			elements.add(iterator.next());
		}
		return elements;
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	@DisplayName("release() empties the iterator.")
	default void testRelease(Supplier<T> supplier)
	{
		FunctionalIterator<?> iterator = supplier.get();
		iterator.release();
		assertFalse(iterator.hasNext(), "Expected no next element after release()");
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testUnwrap(Supplier<T> supplier)
	{
		FunctionalIterator<?> iterator = supplier.get();
		assertIteratorEquals(supplier.get(), iterator.unwrap());
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testRequireNext(Supplier<T> supplier)
	{
		FunctionalIterator<E> iterator = supplier.get().consume();
		assertThrows(NoSuchElementException.class, iterator::requireNext);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	@Override
	default void testConsume(Supplier<T> supplier)
	{
		ReducibleTest.super.testConsume(supplier);
		// Just test that the iterator is empty after calling #consume.
		// There is no way to test whether consume does anything beyond this.
		FunctionalIterator<E> iterator = supplier.get().consume();
		assertTrue(iterator.isEmpty(), "Expected empty iterator after consume()");
		assertFalse(iterator.hasNext(), "Expected no next element after consume()");
	}

	@ParameterizedTest
	@MethodSource({"getReducibles"})
	@Override
	default void testConcat(Supplier<T> supplier)
	{
		ArrayList<Object> expected = supplier.get().collect(new ArrayList<>());
		supplier.get().collect(expected);
		FunctionalIterator<E> actual = supplier.get().concat(supplier.get());
		assertIteratorEquals(expected.iterator(), actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	@Override
	default void testFlatMap(Supplier<T> supplier)
	{
		FunctionalIterator<String> expected = supplier.get().map(String::valueOf);
		FunctionalIterator<String> actual = supplier.get().flatMap(e -> new SingletonIterator.Of<>(String.valueOf(e)));
		assertIteratorEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	@Override
	default void testFlatMapToDouble(Supplier<T> supplier)
	{
		Range range = new Range((int) supplier.get().count());
		PrimitiveIterator.OfDouble expected = unboxDouble(range.iterator().map((int i) -> (double) i));
		Range.RangeIterator index = range.iterator();
		PrimitiveIterator.OfDouble actual = supplier.get().flatMapToDouble(e -> new SingletonIterator.OfDouble(index.next()));
		assertIteratorEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	@Override
	default void testFlatMapToInt(Supplier<T> supplier)
	{
		Range range = new Range((int) supplier.get().count());
		PrimitiveIterator.OfInt expected = range.iterator();
		Range.RangeIterator index = range.iterator();
		PrimitiveIterator.OfInt actual = supplier.get().flatMapToInt(e -> new SingletonIterator.OfInt(index.next()));
		assertIteratorEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	@Override
	default void testFlatMapToLong(Supplier<T> supplier)
	{
		Range range = new Range((int) supplier.get().count());
		PrimitiveIterator.OfLong expected = unboxLong(range.iterator().map((int i) -> (long) i));
		Range.RangeIterator index = range.iterator();
		PrimitiveIterator.OfLong actual = supplier.get().flatMapToLong(e -> new SingletonIterator.OfLong(index.next()));
		assertIteratorEquals(expected, actual);
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
		FunctionalIterator<E> iterator = getAnyReducible();
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
		List<E> expected = collectElements(supplier);
		List<Object> actual = new ArrayList<>();
		supplier.get().forEach(actual::add);
		assertIterableEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	@DisplayName("forEachRemaining() yields same sequence as the underlying iterator.")
	default void testForEachRemaining(Supplier<T> supplier)
	{
		List<E> expected = collectElements(supplier);
		List<Object> actual = new ArrayList<>();
		supplier.get().forEachRemaining(actual::add);
		assertIterableEquals(expected, actual);
	}

	@Test
	default void testForEachRemaining_Null()
	{
		FunctionalIterator<E> iterator = getAnyReducible();
		assertThrows(NullPointerException.class, () -> iterator.forEachRemaining(null));
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testIsEmpty(Supplier<T> supplier)
	{
		T iterator = supplier.get();
		assertTrue(iterator.isEmpty() ^ iterator.hasNext());
	}



	interface Of<E, T extends FunctionalIterator<E>> extends FunctionalIteratorTest<E, T>, ReducibleTest.Of<E, T>
	{
	}
}
