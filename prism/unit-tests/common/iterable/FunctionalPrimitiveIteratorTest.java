package common.iterable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.function.*;

import static common.iterable.Assertions.assertIterableEquals;
import static common.iterable.Assertions.*;
import static common.iterable.PrimitiveIterable.*;
import static org.junit.jupiter.api.Assertions.*;

public interface FunctionalPrimitiveIteratorTest
{
	interface OfDouble<T extends FunctionalPrimitiveIterator.OfDouble> extends FunctionalIteratorTest<Double,T>, PrimitiveReducibleTest.OfDouble<T>
	{
		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapDoubleToObj(Supplier<T> supplier)
		{
			FunctionalIterator<String> expected = supplier.get().map((DoubleFunction<String>) String::valueOf);
			Iterator<String> actual = supplier.get().flatMap((double d) -> List.of(String.valueOf(d)).iterator());
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapDoubleToDouble(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterator.OfDouble expected = range.iterator().mapToDouble((int i) -> (double) i);
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfDouble actual = supplier.get().flatMapToDouble((double d) -> new SingletonIterator.OfDouble(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapDoubleToInt(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterator.OfInt expected = range.iterator();
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfInt actual = supplier.get().flatMapToInt((double d) -> new SingletonIterator.OfInt(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapDoubleToLong(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterator.OfLong expected = range.iterator().mapToLong((int i) -> (long) i);
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfLong actual = supplier.get().flatMapToLong((double d) -> new SingletonIterator.OfLong(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonReducibles")
		default void testFlatMapDoubleToNull(Supplier<T> supplier)
		{
			assertThrows(NullPointerException.class, () -> supplier.get().flatMap((double d) -> null).consume());
			assertThrows(NullPointerException.class, () -> supplier.get().flatMapToDouble((double d) -> null).consume());
			assertThrows(NullPointerException.class, () -> supplier.get().flatMapToInt((double d) -> null).consume());
			assertThrows(NullPointerException.class, () -> supplier.get().flatMapToLong((double d) -> null).consume());
		}

		@Test
		@Override
		default void testFlatMapOfDouble_Null()
		{
			T iterator = getAnyReducible();
			assertThrows(NullPointerException.class, () -> iterator.flatMap((DoubleFunction<? extends Iterator<?>>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToDouble((DoubleFunction<PrimitiveIterator.OfDouble>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToInt((DoubleFunction<PrimitiveIterator.OfInt>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToLong((DoubleFunction<PrimitiveIterator.OfLong>) null));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		@DisplayName("forEach() yields same sequence as forEachRemaining().")
		@Override
		default void testForEachDoubleConsumer(Supplier<T> supplier)
		{
			List<Double> expected = new ArrayList<>();
			supplier.get().forEachRemaining((DoubleConsumer) expected::add);
			T iterator = supplier.get();
			List<Double> actual = new ArrayList<>();
			iterator.forEach((DoubleConsumer) actual::add);
			assertTrue(iterator.isEmpty());
			assertIterableEquals(unboxDouble(expected), unboxDouble(actual));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		@DisplayName("forEachRemaining() yields same sequence as the underlying iterator.")
		default void testForEachRemainingDoubleConsumer(Supplier<T> supplier)
		{
			List<Double> expected = collectElements(supplier);
			T iterator = supplier.get();
			List<Double> actual = new ArrayList<>();
			iterator.forEachRemaining((DoubleConsumer) actual::add);
			assertTrue(iterator.isEmpty());
			assertIterableEquals(unboxDouble(expected), unboxDouble(actual));
		}

		@Test
		default void testForEachRemainingDoubleConsumer_Null()
		{
			T iterator = getAnyReducible();
			assertThrows(NullPointerException.class, () -> iterator.forEachRemaining((DoubleConsumer) null));
		}

		@Test
		default void testConcatTypes()
		{
			// primitive with boxed signature
			FunctionalIterator<Double> primitive = getAnyReducible();
			assertTrue(getAnyReducible().concat(primitive) instanceof FunctionalPrimitiveIterator.OfDouble);
			// boxed
			FunctionalIterator<Double> boxed = getAnyReducible().map((DoubleFunction<Double>) Double::valueOf);
			assertFalse(getAnyReducible().concat(boxed) instanceof FunctionalPrimitiveIterator.OfDouble);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMin_Consumed(Supplier<T> supplier)
		{
			OptionalDouble expected = OptionalDouble.empty();
			OptionalDouble actual = supplier.get().consume().min();
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMax_Consumed(Supplier<T> supplier)
		{
			OptionalDouble expected = OptionalDouble.empty();
			OptionalDouble actual = supplier.get().consume().max();
			assertEquals(expected, actual);
		}
	}



	interface OfInt<T extends FunctionalPrimitiveIterator.OfInt> extends FunctionalIteratorTest<Integer,T>, PrimitiveReducibleTest.OfInt<T>
	{
		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapIntToObj(Supplier<T> supplier)
		{
			FunctionalIterator<String> expected = supplier.get().map((IntFunction<String>) String::valueOf);
			Iterator<String> actual = supplier.get().flatMap((int i) -> List.of(String.valueOf(i)).iterator());
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapIntToDouble(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterator.OfDouble expected = range.iterator().mapToDouble((int i) -> (double) i);
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfDouble actual = supplier.get().flatMapToDouble((int i) -> new SingletonIterator.OfDouble(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapIntToInt(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterator.OfInt expected = range.iterator();
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfInt actual = supplier.get().flatMapToInt((int i) -> new SingletonIterator.OfInt(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapIntToLong(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterator.OfLong expected = range.iterator().mapToLong((int i) -> (long) i);
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfLong actual = supplier.get().flatMapToLong((int i) -> new SingletonIterator.OfLong(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonReducibles")
		default void testFlatMapIntToNull(Supplier<T> supplier)
		{
			assertThrows(NullPointerException.class, () -> supplier.get().flatMap((int i) -> null).consume());
			assertThrows(NullPointerException.class, () -> supplier.get().flatMapToDouble((int i) -> null).consume());
			assertThrows(NullPointerException.class, () -> supplier.get().flatMapToInt((int i) -> null).consume());
			assertThrows(NullPointerException.class, () -> supplier.get().flatMapToLong((int i) -> null).consume());
		}

		@Test
		default void testFlatMapOfInt_Null()
		{
			T iterator = getAnyReducible();
			assertThrows(NullPointerException.class, () -> iterator.flatMap((IntFunction<? extends Iterator<?>>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToDouble((IntFunction<PrimitiveIterator.OfDouble>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToInt((IntFunction<PrimitiveIterator.OfInt>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToLong((IntFunction<PrimitiveIterator.OfLong>) null));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		@DisplayName("forEach() yields same sequence as the underlying iterator.")
		@Override
		default void testForEachIntConsumer(Supplier<T> supplier)
		{
			List<Integer> expected = new ArrayList<>();
			supplier.get().forEachRemaining((IntConsumer) expected::add);
			T iterator = supplier.get();
			List<Integer> actual = new ArrayList<>();
			iterator.forEach((IntConsumer) actual::add);
			assertTrue(iterator.isEmpty());
			assertIterableEquals(unboxInt(expected), unboxInt(actual));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		@DisplayName("forEachRemaining() yields same sequence as the underlying iterator.")
		default void testForEachRemainingIntConsumer(Supplier<T> supplier)
		{
			List<Integer> expected = collectElements(supplier);
			List<Integer> actual = new ArrayList<>();
			supplier.get().forEachRemaining((IntConsumer) actual::add);
			assertIterableEquals(unboxInt(expected), unboxInt(actual));
		}

		@Test
		default void testForEachRemainingIntConsumer_Null()
		{
			T iterator = getAnyReducible();
			assertThrows(NullPointerException.class, () -> iterator.forEachRemaining((IntConsumer) null));
		}

		@Test
		default void testConcatTypes()
		{
			// primitive with boxed signature
			FunctionalIterator<Integer> primitive = getAnyReducible();
			assertTrue(getAnyReducible().concat(primitive) instanceof FunctionalPrimitiveIterator.OfInt);
			// boxed
			FunctionalIterator<Integer> boxed = getAnyReducible().map((IntFunction<Integer>) Integer::valueOf);
			assertFalse(getAnyReducible().concat(boxed) instanceof FunctionalPrimitiveIterator.OfInt);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMin_Consumed(Supplier<T> supplier)
		{
			OptionalInt expected = OptionalInt.empty();
			OptionalInt actual = supplier.get().consume().min();
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMax_Consumed(Supplier<T> supplier)
		{
			OptionalInt expected = OptionalInt.empty();
			OptionalInt actual = supplier.get().consume().max();
			assertEquals(expected, actual);
		}
	}



	interface OfLong<T extends FunctionalPrimitiveIterator.OfLong> extends FunctionalIteratorTest<Long,T>, PrimitiveReducibleTest.OfLong<T>
	{
		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapLongToObj(Supplier<T> supplier)
		{
			FunctionalIterator<String> expected = supplier.get().map((LongFunction<String>) String::valueOf);
			Iterator<String> actual = supplier.get().flatMap((long l) -> List.of(String.valueOf(l)).iterator());
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapLongToDouble(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterator.OfDouble expected = range.iterator().mapToDouble((int i) -> (double) i);
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfDouble actual = supplier.get().flatMapToDouble((long l) -> new SingletonIterator.OfDouble(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapLongToInt(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterator.OfInt expected = range.iterator();
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfInt actual = supplier.get().flatMapToInt((long l) -> new SingletonIterator.OfInt(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapLongToLong(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterator.OfLong expected = range.iterator().mapToLong((int i) -> (long) i);
			Range.RangeIterator index = range.iterator();
			PrimitiveIterator.OfLong actual = supplier.get().flatMapToLong((long l) -> new SingletonIterator.OfLong(index.next()));
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonReducibles")
		default void testFlatMapLongToNull(Supplier<T> supplier)
		{
			assertThrows(NullPointerException.class, () -> supplier.get().flatMap((long l) -> null).consume());
			assertThrows(NullPointerException.class, () -> supplier.get().flatMapToDouble((long l) -> null).consume());
			assertThrows(NullPointerException.class, () -> supplier.get().flatMapToInt((long l) -> null).consume());
			assertThrows(NullPointerException.class, () -> supplier.get().flatMapToLong((long l) -> null).consume());
		}

		@Test
		default void testFlatMapOfLong_Null()
		{
			T iterator = getAnyReducible();
			assertThrows(NullPointerException.class, () -> iterator.flatMap((LongFunction<? extends Iterator<?>>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToDouble((LongFunction<PrimitiveIterator.OfDouble>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToInt((LongFunction<PrimitiveIterator.OfInt>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToLong((LongFunction<PrimitiveIterator.OfLong>) null));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		@DisplayName("forEach() yields same sequence as the underlying iterator.")
		@Override
		default void testForEachLongConsumer(Supplier<T> supplier)
		{
			List<Long> expected = new ArrayList<>();
			supplier.get().forEachRemaining((LongConsumer) expected::add);
			T iterator = supplier.get();
			List<Long> actual = new ArrayList<>();
			iterator.forEach((LongConsumer) actual::add);
			assertTrue(iterator.isEmpty());
			assertIterableEquals(unboxLong(expected), unboxLong(actual));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		@DisplayName("forEachRemaining() yields same sequence as the underlying iterator.")
		default void testForEachRemainingLongConsumer(Supplier<T> supplier)
		{
			List<Long> expected = collectElements(supplier);
			List<Long> actual = new ArrayList<>();
			supplier.get().forEachRemaining((LongConsumer) actual::add);
			assertIterableEquals(unboxLong(expected), unboxLong(actual));
		}

		@Test
		default void testForEachRemainingLongConsumer_Null()
		{
			T iterator = getAnyReducible();
			assertThrows(NullPointerException.class, () -> iterator.forEachRemaining((LongConsumer) null));
		}

		@Test
		default void testConcatTypes()
		{
			// primitive with boxed signature
			FunctionalIterator<Long> primitive = getAnyReducible();
			assertTrue(getAnyReducible().concat(primitive) instanceof FunctionalPrimitiveIterator.OfLong);
			// boxed
			FunctionalIterator<Long> boxed = getAnyReducible().map((LongFunction<Long>) Long::valueOf);
			assertFalse(getAnyReducible().concat(boxed) instanceof FunctionalPrimitiveIterator.OfLong);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMin_Consumed(Supplier<T> supplier)
		{
			OptionalLong expected = OptionalLong.empty();
			OptionalLong actual = supplier.get().consume().min();
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMax_Consumed(Supplier<T> supplier)
		{
			OptionalLong expected = OptionalLong.empty();
			OptionalLong actual = supplier.get().consume().max();
			assertEquals(expected, actual);
		}
	}
}
