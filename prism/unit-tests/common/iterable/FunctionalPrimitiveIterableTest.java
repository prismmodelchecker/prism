package common.iterable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.function.*;

import static common.iterable.Assertions.assertIterableEquals;
import static common.iterable.PrimitiveIterable.*;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.*;

public interface FunctionalPrimitiveIterableTest
{
	interface OfDouble<T extends FunctionalPrimitiveIterable.OfDouble> extends FunctionalIterableTest<Double,T>, PrimitiveReducibleTest.OfDouble<T>
	{
		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapDoubleToObj(Supplier<T> supplier)
		{
			FunctionalIterable<String> expected = supplier.get().map((DoubleFunction<String>) String::valueOf);
			Iterable<String> actual = supplier.get().flatMap((double d) -> List.of(String.valueOf(d)));
			assertIterableEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapDoubleToDouble(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterable.OfDouble expected = range.mapToDouble((int i) -> (double) i);
			Range.RangeIterator index = range.iterator();
			PrimitiveIterable.OfDouble actual = supplier.get().flatMapToDouble((double d) -> new SingletonIterable.OfDouble(index.next()));
			assertIterableEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapDoubleToInt(Supplier<T> supplier)
		{
			Range expected = new Range((int) supplier.get().count());
			Range.RangeIterator index = expected.iterator();
			PrimitiveIterable.OfInt actual = supplier.get().flatMapToInt((double d) -> new SingletonIterable.OfInt(index.next()));
			assertIterableEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapDoubleToLong(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterable.OfLong expected = range.mapToLong((int i) -> (long) i);
			Range.RangeIterator index = range.iterator();
			PrimitiveIterable.OfLong actual = supplier.get().flatMapToLong((double d) -> new SingletonIterable.OfLong(index.next()));
			assertIterableEquals(expected, actual);
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
		default void testFlatMapOfDouble_Null()
		{
			T iterator = getAnyReducible();
			assertThrows(NullPointerException.class, () -> iterator.flatMap((DoubleFunction<? extends Iterable<?>>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToDouble((DoubleFunction<PrimitiveIterable.OfDouble>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToInt((DoubleFunction<PrimitiveIterable.OfInt>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToLong((DoubleFunction<PrimitiveIterable.OfLong>) null));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		@DisplayName("forEach() yields same sequence as the underlying iterator.")
		@Override
		default void testForEachDoubleConsumer(Supplier<T> supplier)
		{
			T expected = supplier.get();
			List<Double> actual = new ArrayList<>();
			supplier.get().forEach((DoubleConsumer) actual::add);
			assertIterableEquals(unboxDouble(expected), unboxDouble(actual));
		}

		@Test
		default void testConcatTypes()
		{
			// primitive with boxed signature
			FunctionalIterable<Double> primitive = getAnyReducible();
			assertTrue(getAnyReducible().concat(primitive) instanceof FunctionalPrimitiveIterable.OfDouble);
			// boxed
			FunctionalIterable<Double> boxed = getAnyReducible().map((DoubleFunction<Double>) Double::valueOf);
			assertFalse(getAnyReducible().concat(boxed) instanceof FunctionalPrimitiveIterable.OfDouble);
		}
	}



	interface OfInt<T extends FunctionalPrimitiveIterable.OfInt> extends FunctionalIterableTest<Integer,T>, PrimitiveReducibleTest.OfInt<T>
	{
		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapIntToObj(Supplier<T> supplier)
		{
			FunctionalIterable<String> expected = supplier.get().map((IntFunction<String>) String::valueOf);
			FunctionalIterable<String> actual = supplier.get().flatMap((int i) -> List.of(String.valueOf(i)));
			assertIterableEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapIntToDouble(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterable.OfDouble expected = range.mapToDouble((int i) -> (double) i);
			Range.RangeIterator index = range.iterator();
			PrimitiveIterable.OfDouble actual = supplier.get().flatMapToDouble((int i) -> new SingletonIterable.OfDouble(index.next()));
			assertIterableEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapIntToInt(Supplier<T> supplier)
		{
			Range expected = new Range((int) supplier.get().count());
			Range.RangeIterator index = expected.iterator();
			PrimitiveIterable.OfInt actual = supplier.get().flatMapToInt((int i) -> new SingletonIterable.OfInt(index.next()));
			assertIterableEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapIntToLong(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterable.OfLong expected = range.mapToLong((int i) -> (long) i);
			Range.RangeIterator index = range.iterator();
			PrimitiveIterable.OfLong actual = supplier.get().flatMapToLong((int i) -> new SingletonIterable.OfLong(index.next()));
			assertIterableEquals(expected, actual);
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
			assertThrows(NullPointerException.class, () -> iterator.flatMap((IntFunction<? extends Iterable<?>>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToDouble((IntFunction<PrimitiveIterable.OfDouble>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToInt((IntFunction<PrimitiveIterable.OfInt>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToLong((IntFunction<PrimitiveIterable.OfLong>) null));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		@DisplayName("forEach() yields same sequence as the underlying iterator.")
		@Override
		default void testForEachIntConsumer(Supplier<T> supplier)
		{
			T expected = supplier.get();
			List<Integer> actual = new ArrayList<>();
			supplier.get().forEach((IntConsumer) actual::add);
			assertIterableEquals(unboxInt(expected), unboxInt(actual));
		}

		@Test
		default void testConcatTypes()
		{
			// primitive with boxed signature
			FunctionalIterable<Integer> primitive = getAnyReducible();
			assertTrue(getAnyReducible().concat(primitive) instanceof FunctionalPrimitiveIterable.OfInt);
			// boxed
			FunctionalIterable<Integer> boxed = getAnyReducible().map((IntFunction<Integer>) Integer::valueOf);
			assertFalse(getAnyReducible().concat(boxed) instanceof FunctionalPrimitiveIterable.OfInt);
		}
	}



	interface OfLong<T extends FunctionalPrimitiveIterable.OfLong> extends FunctionalIterableTest<Long,T>, PrimitiveReducibleTest.OfLong<T>
	{
		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapLongToObj(Supplier<T> supplier)
		{
			FunctionalIterable<String> expected = supplier.get().map((LongFunction<String>) String::valueOf);
			FunctionalIterable<String> actual = supplier.get().flatMap((long l) -> List.of(String.valueOf(l)));
			assertIterableEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapLongToDouble(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterable.OfDouble expected = range.mapToDouble((int i) -> (double) i);
			Range.RangeIterator index = range.iterator();
			PrimitiveIterable.OfDouble actual = supplier.get().flatMapToDouble((long l) -> new SingletonIterable.OfDouble(index.next()));
			assertIterableEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapLongToInt(Supplier<T> supplier)
		{
			Range expected = new Range((int) supplier.get().count());
			Range.RangeIterator index = expected.iterator();
			PrimitiveIterable.OfInt actual = supplier.get().flatMapToInt((long l) -> new SingletonIterable.OfInt(index.next()));
			assertIterableEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFlatMapLongToLong(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterable.OfLong expected = range.mapToLong((int i) -> (long) i);
			Range.RangeIterator index = range.iterator();
			PrimitiveIterable.OfLong actual = supplier.get().flatMapToLong((long l) -> new SingletonIterable.OfLong(index.next()));
			assertIterableEquals(expected, actual);
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
			assertThrows(NullPointerException.class, () -> iterator.flatMap((LongFunction<? extends Iterable<?>>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToDouble((LongFunction<PrimitiveIterable.OfDouble>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToInt((LongFunction<PrimitiveIterable.OfInt>) null));
			assertThrows(NullPointerException.class, () -> iterator.flatMapToLong((LongFunction<PrimitiveIterable.OfLong>) null));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		@DisplayName("forEach() yields same sequence as the underlying iterator.")
		@Override
		default void testForEachLongConsumer(Supplier<T> supplier)
		{
			T expected = supplier.get();
			List<Long> actual = new ArrayList<>();
			supplier.get().forEach((LongConsumer) actual::add);
			assertIterableEquals(unboxLong(expected), unboxLong(actual));
		}

		@Test
		default void testConcatTypes()
		{
			// primitive with boxed signature
			FunctionalIterable<Long> primitive = getAnyReducible();
			assertTrue(getAnyReducible().concat(primitive) instanceof FunctionalPrimitiveIterable.OfLong);
			// boxed
			FunctionalIterable<Long> boxed = getAnyReducible().map((LongFunction<Long>) Long::valueOf);
			assertFalse(getAnyReducible().concat(boxed) instanceof FunctionalPrimitiveIterable.OfLong);
		}
	}
}
