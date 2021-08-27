package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

import static common.iterable.Assertions.assertIteratorEquals;
import static org.junit.jupiter.api.Assertions.*;

interface ArrayIteratorTest
{
	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testNextIndex(Supplier<? extends ArrayIterator<?>> supplier)
	{
		ArrayIterator<?> iterator = supplier.get();
		for (int i=0; iterator.hasNext(); i++) {
			assertEquals(i, iterator.nextIndex());
			iterator.next();
		}
		assertThrows(NoSuchElementException.class, iterator::nextIndex);
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Of implements ArrayIteratorTest, FunctionalIteratorTest.Of<Object,ArrayIterator.Of<Object>>
	{
		@Override
		public ArrayIterator.Of<Object> getReducible(Object[] objects)
		{
			return new ArrayIterator.Of<>(objects);
		}

		@ParameterizedTest
		@MethodSource("getArraysAsArguments")
		public void testOfArray(Object[] array)
		{
			ArrayIterator.Of<Object> iterator = new ArrayIterator.Of<>(array);
			Object[] actual = iterator.collect(new Object[array.length]);
			assertArrayEquals(array, actual);
		}

		@ParameterizedTest
		@MethodSource("getArraysAsArguments")
		public void testOfArrayIntInt_All(Object[] array)
		{
			ArrayIterator.Of<Object> expected = new ArrayIterator.Of<>(array);
			ArrayIterator.Of<Object> actual = new ArrayIterator.Of<>(array, 0, array.length);
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource({"getMultitonArraysAsArguments"})
		public void testOfArrayIntInt_Range(Object[] array)
		{
			FunctionalIterable<Object> expected = new Range(1, array.length - 1).map((int i) -> array[i]);
			ArrayIterator.Of<Object> actual = new ArrayIterator.Of<>(array, 1, array.length - 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOf_Errors()
		{
			Optional<Object[]> any = getMultitonArraysOfObject().findAny();
			assert any.isPresent();
			Object[] array = any.get();
			int length = array.length;
			assertThrows(NullPointerException.class, () -> new ArrayIterator.Of<>((Object[]) null));
			assertThrows(NullPointerException.class, () -> new ArrayIterator.Of<>(null, 0, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.Of<>(array, -1, -1));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.Of<>(array, -1, length));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.Of<>(array, 1, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.Of<>(array, 0, length+1));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.Of<>(array, length+1, length+1));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfDouble implements ArrayIteratorTest, FunctionalPrimitiveIteratorTest.OfDouble<ArrayIterator.OfDouble>
	{
		@Override
		public ArrayIterator.OfDouble getReducible(double[] numbers)
		{
			return new ArrayIterator.OfDouble(numbers);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfDouble", "getSingletonArraysOfDouble", "getMultitonArraysOfDouble"})
		public void testOfDouble(double[] array)
		{
			ArrayIterator.OfDouble iterator = new ArrayIterator.OfDouble(array);
			double[] actual = iterator.collect(new double[array.length]);
			assertArrayEquals(array, actual);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfDouble", "getSingletonArraysOfDouble", "getMultitonArraysOfDouble"})
		public void testOfDoubleArrayIntInt_All(double[] array)
		{
			ArrayIterator.OfDouble expected = new ArrayIterator.OfDouble(array);
			ArrayIterator.OfDouble actual = new ArrayIterator.OfDouble(array, 0, array.length);
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource({"getMultitonArraysOfDouble"})
		public void testOfDoubleArrayIntInt_Range(double[] array)
		{
			FunctionalPrimitiveIterable.OfDouble expected = new Range(1, array.length - 1).mapToDouble((int i) -> array[i]);
			ArrayIterator.OfDouble actual = new ArrayIterator.OfDouble(array, 1, array.length - 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOfDouble_Errors()
		{
			Optional<double[]> any = getMultitonArraysOfDouble().findAny();
			assert any.isPresent();
			double[] array = any.get();
			int length = array.length;
			assertThrows(NullPointerException.class, () -> new ArrayIterator.OfDouble((double[]) null));
			assertThrows(NullPointerException.class, () -> new ArrayIterator.OfDouble(null, 0, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfDouble(array, -1, -1));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfDouble(array, -1, length));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfDouble(array, 1, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfDouble(array, 0, length+1));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfDouble(array, length+1, length+1));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfInt implements ArrayIteratorTest, FunctionalPrimitiveIteratorTest.OfInt<ArrayIterator.OfInt>
	{
		@Override
		public ArrayIterator.OfInt getReducible(int[] numbers)
		{
			return new ArrayIterator.OfInt(numbers);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfInt", "getSingletonArraysOfInt", "getMultitonArraysOfInt"})
		public void testOfInt(int[] array)
		{
			ArrayIterator.OfInt iterator = new ArrayIterator.OfInt(array);
			int[] actual = iterator.collect(new int[array.length]);
			assertArrayEquals(array, actual);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfInt", "getSingletonArraysOfInt", "getMultitonArraysOfInt"})
		public void testOfIntArrayIntInt_All(int[] array)
		{
			ArrayIterator.OfInt expected = new ArrayIterator.OfInt(array);
			ArrayIterator.OfInt actual = new ArrayIterator.OfInt(array, 0, array.length);
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource({"getMultitonArraysOfInt"})
		public void testOfIntArrayIntInt_Range(int[] array)
		{
			FunctionalPrimitiveIterable.OfInt expected = new Range(1, array.length - 1).mapToInt((int i) -> array[i]);
			ArrayIterator.OfInt actual = new ArrayIterator.OfInt(array, 1, array.length - 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOfInt_Errors()
		{
			Optional<int[]> any = getMultitonArraysOfInt().findAny();
			assert any.isPresent();
			int[] array = any.get();
			int length = array.length;
			assertThrows(NullPointerException.class, () -> new ArrayIterator.OfInt((int[]) null));
			assertThrows(NullPointerException.class, () -> new ArrayIterator.OfInt(null, 0, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfInt(array, -1, -1));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfInt(array, -1, length));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfInt(array, 1, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfInt(array, 0, length+1));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfInt(array, length+1, length+1));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfLong implements ArrayIteratorTest, FunctionalPrimitiveIteratorTest.OfLong<ArrayIterator.OfLong>
	{
		@Override
		public ArrayIterator.OfLong getReducible(long[] numbers)
		{
			return new ArrayIterator.OfLong(numbers);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfLong", "getSingletonArraysOfLong", "getMultitonArraysOfLong"})
		public void testOfLong(long[] array)
		{
			ArrayIterator.OfLong iterator = new ArrayIterator.OfLong(array);
			long[] actual = iterator.collect(new long[array.length]);
			assertArrayEquals(array, actual);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfLong", "getSingletonArraysOfLong", "getMultitonArraysOfLong"})
		public void testOfLongArrayIntInt_All(long[] array)
		{
			ArrayIterator.OfLong expected = new ArrayIterator.OfLong(array);
			ArrayIterator.OfLong actual = new ArrayIterator.OfLong(array, 0, array.length);
			assertIteratorEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource({"getMultitonArraysOfLong"})
		public void testOfLongArrayIntInt_Range(long[] array)
		{
			FunctionalPrimitiveIterable.OfLong expected = new Range(1, array.length - 1).mapToLong((int i) -> array[i]);
			ArrayIterator.OfLong actual = new ArrayIterator.OfLong(array, 1, array.length - 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOfLong_Errors()
		{
			Optional<long[]> any = getMultitonArraysOfLong().findAny();
			assert any.isPresent();
			long[] array = any.get();
			int length = array.length;
			assertThrows(NullPointerException.class, () -> new ArrayIterator.OfLong((long[]) null));
			assertThrows(NullPointerException.class, () -> new ArrayIterator.OfLong(null, 0, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfLong(array, -1, -1));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfLong(array, -1, length));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfLong(array, 1, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfLong(array, 0, length+1));
			assertThrows(IndexOutOfBoundsException.class, () -> new ArrayIterator.OfLong(array, length+1, length+1));
		}
	}
}