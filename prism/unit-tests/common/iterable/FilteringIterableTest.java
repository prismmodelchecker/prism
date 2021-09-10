package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;

import static common.iterable.Assertions.assertIterableEquals;
import static common.iterable.PrimitiveIterable.*;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

interface FilteringIterableTest
{
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Of implements FilteringIterableTest, FunctionalIterableTest.Of<Object,FilteringIterable.Of<Object>>
	{
		@Override
		public FilteringIterable.Of<Object> getReducible(Object[] objects)
		{
			return new FilteringIterable.Of<>(FunctionalIterable.of(objects), e -> true);
		}

		@ParameterizedTest
		@MethodSource("getArraysAsArguments")
		void testOf(Object[] objects)
		{
			ArrayList<Object> expected = new ArrayList<>();
			int c = 0;
			for (Object each : objects) {
				if (c++ % 2 == 0) {
					expected.add(each);
				}
			}
			Iterable<Object> iterable = Arrays.asList(objects);
			FunctionalIterable<Object> actual = new FilteringIterable.Of<>(iterable, expected::contains);
			assertIterableEquals(expected, actual);
		}

		@Test
		public void testOf_Null()
		{
			EmptyIterable.Of<Object> iterable = EmptyIterable.of();
			assertThrows(NullPointerException.class, () -> new FilteringIterable.Of<>(null, e -> true));
			assertThrows(NullPointerException.class, () -> new FilteringIterable.Of<>(iterable, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfDouble implements FilteringIterableTest, FunctionalPrimitiveIterableTest.OfDouble<FilteringIterable.OfDouble>
	{
		@Override
		public FilteringIterable.OfDouble getReducible(double[] numbers)
		{
			return new FilteringIterable.OfDouble(FunctionalIterable.ofDouble(numbers), e -> true);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		public void testOfDouble(double[] numbers)
		{
			ArrayList<Double> expected = new ArrayList<>();
			int c = 0;
			for (double d : numbers) {
				if (c++ % 2 == 0) {
					expected.add(d);
				}
			}
			IterableArray.OfDouble iterable = new IterableArray.OfDouble(numbers);
			FunctionalPrimitiveIterable.OfDouble actual = new FilteringIterable.OfDouble(iterable, expected::contains);
			assertIterableEquals(unboxDouble(expected), actual);
		}

		@Test
		public void testOfDouble_Null()
		{
			EmptyIterable.OfDouble iterable = EmptyIterable.ofDouble();
			assertThrows(NullPointerException.class, () -> new FilteringIterable.OfDouble(null, e -> true));
			assertThrows(NullPointerException.class, () -> new FilteringIterable.OfDouble(iterable, null));
		}
	}




	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfInt implements FilteringIterableTest, FunctionalPrimitiveIterableTest.OfInt<FilteringIterable.OfInt>
	{
		@Override
		public FilteringIterable.OfInt getReducible(int[] numbers)
		{
			return new FilteringIterable.OfInt(FunctionalIterable.ofInt(numbers), e -> true);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		public void testOfInt(int[] numbers)
		{
			ArrayList<Integer> expected = new ArrayList<>();
			int c = 0;
			for (int i : numbers) {
				if (c++ % 2 == 0) {
					expected.add(i);
				}
			}
			IterableArray.OfInt iterable = new IterableArray.OfInt(numbers);
			FunctionalPrimitiveIterable.OfInt actual = new FilteringIterable.OfInt(iterable, expected::contains);
			assertIterableEquals(unboxInt(expected), actual);
		}

		@Test
		public void testOfInt_Null()
		{
			EmptyIterable.OfInt iterable = EmptyIterable.ofInt();
			assertThrows(NullPointerException.class, () -> new FilteringIterable.OfInt(null, e -> true));
			assertThrows(NullPointerException.class, () -> new FilteringIterable.OfInt(iterable, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfLong implements FilteringIterableTest, FunctionalPrimitiveIterableTest.OfLong<FilteringIterable.OfLong>
	{
		@Override
		public FilteringIterable.OfLong getReducible(long[] numbers)
		{
			return new FilteringIterable.OfLong(FunctionalIterable.ofLong(numbers), e -> true);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		public void testOfLong(long[] numbers)
		{
			ArrayList<Long> expected = new ArrayList<>();
			int c = 0;
			for (Long i : numbers) {
				if (c++ % 2 == 0) {
					expected.add(i);
				}
			}
			IterableArray.OfLong iterable = new IterableArray.OfLong(numbers);
			FunctionalPrimitiveIterable.OfLong actual = new FilteringIterable.OfLong(iterable, expected::contains);
			assertIterableEquals(unboxLong(expected), actual);
		}

		@Test
		public void testOfLong_Null()
		{
			EmptyIterable.OfLong iterable = EmptyIterable.ofLong();
			assertThrows(NullPointerException.class, () -> new FilteringIterable.OfLong(null, e -> true));
			assertThrows(NullPointerException.class, () -> new FilteringIterable.OfLong(iterable, null));
		}
	}
}
