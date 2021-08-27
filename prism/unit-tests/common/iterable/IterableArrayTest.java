package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;

import static common.iterable.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

interface IterableArrayTest
{
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Of implements IterableArrayTest, FunctionalIterableTest.Of<Object,IterableArray.Of<Object>>
	{
		@Override
		public IterableArray.Of<Object> getReducible(Object[] objects)
		{
			return new IterableArray.Of<>(objects);
		}

		@ParameterizedTest
		@MethodSource("getArraysAsArguments")
		public void testOfArray(Object[] array)
		{
			IterableArray.Of<Object> iterator = new IterableArray.Of<>(array);
			Object[] actual = iterator.collect(new Object[array.length]);
			assertArrayEquals(array, actual);
		}

		@ParameterizedTest
		@MethodSource("getArraysAsArguments")
		public void testOfArrayIntInt_All(Object[] array)
		{
			IterableArray.Of<Object> expected = new IterableArray.Of<>(array);
			IterableArray.Of<Object> actual = new IterableArray.Of<>(array, 0, array.length);
			assertIterableEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource({"getMultitonArraysAsArguments"})
		public void testOfArrayIntInt_Range(Object[] array)
		{
			FunctionalIterable<Object> expected = new Range(1, array.length - 1).map((int i) -> array[i]);
			IterableArray.Of<Object> actual = new IterableArray.Of<>(array, 1, array.length - 1);
			assertIterableEquals(expected, actual);
		}

		@Test
		public void testOf_Errors()
		{
			Optional<Object[]> any = getMultitonArraysOfObject().findAny();
			assert any.isPresent();
			Object[] array = any.get();
			int length = array.length;
			assertThrows(NullPointerException.class, () -> new IterableArray.Of<>((Object[]) null));
			assertThrows(NullPointerException.class, () -> new IterableArray.Of<>(null, 0, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.Of<>(array, -1, -1));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.Of<>(array, -1, length));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.Of<>(array, 1, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.Of<>(array, 0, length+1));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.Of<>(array, length+1, length+1));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfDouble implements IterableArrayTest, FunctionalPrimitiveIterableTest.OfDouble<IterableArray.OfDouble>
	{
		@Override
		public IterableArray.OfDouble getReducible(double[] numbers)
		{
			return new IterableArray.OfDouble(numbers);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfDouble", "getSingletonArraysOfDouble", "getMultitonArraysOfDouble"})
		public void testOfDouble(double[] array)
		{
			IterableArray.OfDouble iterator = new IterableArray.OfDouble(array);
			double[] actual = iterator.collect(new double[array.length]);
			assertArrayEquals(array, actual);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfDouble", "getSingletonArraysOfDouble", "getMultitonArraysOfDouble"})
		public void testOfDoubleArrayIntInt_All(double[] array)
		{
			IterableArray.OfDouble expected = new IterableArray.OfDouble(array);
			IterableArray.OfDouble actual = new IterableArray.OfDouble(array, 0, array.length);
			assertIterableEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource({"getMultitonArraysOfDouble"})
		public void testOfDoubleArrayIntInt_Range(double[] array)
		{
			FunctionalPrimitiveIterable.OfDouble expected = new Range(1, array.length - 1).mapToDouble((int i) -> array[i]);
			IterableArray.OfDouble actual = new IterableArray.OfDouble(array, 1, array.length - 1);
			assertIterableEquals(expected, actual);
		}

		@Test
		public void testOfDouble_Errors()
		{
			Optional<double[]> any = getMultitonArraysOfDouble().findAny();
			assert any.isPresent();
			double[] array = any.get();
			int length = array.length;
			assertThrows(NullPointerException.class, () -> new IterableArray.OfDouble((double[]) null));
			assertThrows(NullPointerException.class, () -> new IterableArray.OfDouble(null, 0, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.OfDouble(array, -1, -1));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.OfDouble(array, -1, length));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.OfDouble(array, 1, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.OfDouble(array, 0, length+1));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.OfDouble(array, length+1, length+1));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfInt implements IterableArrayTest, FunctionalPrimitiveIterableTest.OfInt<IterableArray.OfInt>
	{
		@Override
		public IterableArray.OfInt getReducible(int[] numbers)
		{
			return new IterableArray.OfInt(numbers);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfInt", "getSingletonArraysOfInt", "getMultitonArraysOfInt"})
		public void testOfInt(int[] array)
		{
			IterableArray.OfInt iterator = new IterableArray.OfInt(array);
			int[] actual = iterator.collect(new int[array.length]);
			assertArrayEquals(array, actual);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfInt", "getSingletonArraysOfInt", "getMultitonArraysOfInt"})
		public void testOfIntArrayIntInt_All(int[] array)
		{
			IterableArray.OfInt expected = new IterableArray.OfInt(array);
			IterableArray.OfInt actual = new IterableArray.OfInt(array, 0, array.length);
			assertIterableEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource({"getMultitonArraysOfInt"})
		public void testOfIntArrayIntInt_Range(int[] array)
		{
			FunctionalPrimitiveIterable.OfInt expected = new Range(1, array.length - 1).mapToInt((int i) -> array[i]);
			IterableArray.OfInt actual = new IterableArray.OfInt(array, 1, array.length - 1);
			assertIterableEquals(expected, actual);
		}

		@Test
		public void testOfInt_Errors()
		{
			Optional<int[]> any = getMultitonArraysOfInt().findAny();
			assert any.isPresent();
			int[] array = any.get();
			int length = array.length;
			assertThrows(NullPointerException.class, () -> new IterableArray.OfInt((int[]) null));
			assertThrows(NullPointerException.class, () -> new IterableArray.OfInt(null, 0, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.OfInt(array, -1, -1));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.OfInt(array, -1, length));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.OfInt(array, 1, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.OfInt(array, 0, length+1));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.OfInt(array, length+1, length+1));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfLong implements IterableArrayTest, FunctionalPrimitiveIterableTest.OfLong<IterableArray.OfLong>
	{
		@Override
		public IterableArray.OfLong getReducible(long[] numbers)
		{
			return new IterableArray.OfLong(numbers);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfLong", "getSingletonArraysOfLong", "getMultitonArraysOfLong"})
		public void testOfLong(long[] array)
		{
			IterableArray.OfLong iterator = new IterableArray.OfLong(array);
			long[] actual = iterator.collect(new long[array.length]);
			assertArrayEquals(array, actual);
		}

		@ParameterizedTest
		@MethodSource({"getEmptyArraysOfLong", "getSingletonArraysOfLong", "getMultitonArraysOfLong"})
		public void testOfLongArrayIntInt_All(long[] array)
		{
			IterableArray.OfLong expected = new IterableArray.OfLong(array);
			IterableArray.OfLong actual = new IterableArray.OfLong(array, 0, array.length);
			assertIterableEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource({"getMultitonArraysOfLong"})
		public void testOfLongArrayIntInt_Range(long[] array)
		{
			FunctionalPrimitiveIterable.OfLong expected = new Range(1, array.length - 1).mapToLong((int i) -> array[i]);
			IterableArray.OfLong actual = new IterableArray.OfLong(array, 1, array.length - 1);
			assertIterableEquals(expected, actual);
		}

		@Test
		public void testOfLong_Errors()
		{
			Optional<long[]> any = getMultitonArraysOfLong().findAny();
			assert any.isPresent();
			long[] array = any.get();
			int length = array.length;
			assertThrows(NullPointerException.class, () -> new IterableArray.OfLong((long[]) null));
			assertThrows(NullPointerException.class, () -> new IterableArray.OfLong(null, 0, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.OfLong(array, -1, -1));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.OfLong(array, -1, length));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.OfLong(array, 1, 0));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.OfLong(array, 0, length+1));
			assertThrows(IndexOutOfBoundsException.class, () -> new IterableArray.OfLong(array, length+1, length+1));
		}
	}
}