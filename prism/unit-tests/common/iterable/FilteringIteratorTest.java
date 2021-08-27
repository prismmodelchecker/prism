package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import static common.iterable.Assertions.assertIteratorEquals;
import static common.iterable.PrimitiveIterable.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

interface FilteringIteratorTest
{
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Of implements FilteringIteratorTest, FunctionalIteratorTest.Of<Object,FilteringIterator.Of<Object>>
	{
		@Override
		public FilteringIterator.Of<Object> getReducible(Object[] objects)
		{
			return new FilteringIterator.Of<>(FunctionalIterator.of(objects), e -> true);
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
			Iterator<Object> iterator = Arrays.asList(objects).iterator();
			FunctionalIterator<Object> actual = new FilteringIterator.Of<>(iterator, expected::contains);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOf_Null()
		{
			EmptyIterator.Of<Object> iterator = EmptyIterator.of();
			assertThrows(NullPointerException.class, () -> new FilteringIterator.Of<>(null, e -> true));
			assertThrows(NullPointerException.class, () -> new FilteringIterator.Of<>(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfDouble implements FilteringIteratorTest, FunctionalPrimitiveIteratorTest.OfDouble<FilteringIterator.OfDouble>
	{
		@Override
		public FilteringIterator.OfDouble getReducible(double[] numbers)
		{
			return new FilteringIterator.OfDouble(FunctionalIterator.ofDouble(numbers), e -> true);
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
			ArrayIterator.OfDouble iterator = new ArrayIterator.OfDouble(numbers);
			FunctionalPrimitiveIterator.OfDouble actual = new FilteringIterator.OfDouble(iterator, expected::contains);
			assertIteratorEquals(unboxDouble(expected.iterator()), actual);
		}

		@Test
		public void testOfDouble_Null()
		{
			EmptyIterator.OfDouble iterator = EmptyIterator.ofDouble();
			assertThrows(NullPointerException.class, () -> new FilteringIterator.OfDouble(null, e -> true));
			assertThrows(NullPointerException.class, () -> new FilteringIterator.OfDouble(iterator, null));
		}
	}




	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfInt implements FilteringIteratorTest, FunctionalPrimitiveIteratorTest.OfInt<FilteringIterator.OfInt>
	{
		@Override
		public FilteringIterator.OfInt getReducible(int[] numbers)
		{
			return new FilteringIterator.OfInt(FunctionalIterator.ofInt(numbers), e -> true);
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
			ArrayIterator.OfInt iterator = new ArrayIterator.OfInt(numbers);
			FunctionalPrimitiveIterator.OfInt actual = new FilteringIterator.OfInt(iterator, expected::contains);
			assertIteratorEquals(unboxInt(expected.iterator()), actual);
		}

		@Test
		public void testOfInt_Null()
		{
			EmptyIterator.OfInt iterator = EmptyIterator.ofInt();
			assertThrows(NullPointerException.class, () -> new FilteringIterator.OfInt(null, e -> true));
			assertThrows(NullPointerException.class, () -> new FilteringIterator.OfInt(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfLong implements FilteringIteratorTest, FunctionalPrimitiveIteratorTest.OfLong<FilteringIterator.OfLong>
	{
		@Override
		public FilteringIterator.OfLong getReducible(long[] numbers)
		{
			return new FilteringIterator.OfLong(FunctionalIterator.ofLong(numbers), e -> true);
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
			ArrayIterator.OfLong iterator = new ArrayIterator.OfLong(numbers);
			FunctionalPrimitiveIterator.OfLong actual = new FilteringIterator.OfLong(iterator, expected::contains);
			assertIteratorEquals(unboxLong(expected.iterator()), actual);
		}

		@Test
		public void testOfLong_Null()
		{
			EmptyIterator.OfLong iterator = EmptyIterator.ofLong();
			assertThrows(NullPointerException.class, () -> new FilteringIterator.OfLong(null, e -> true));
			assertThrows(NullPointerException.class, () -> new FilteringIterator.OfLong(iterator, null));
		}
	}
}
