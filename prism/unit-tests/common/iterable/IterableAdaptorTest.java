package common.iterable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;

import static common.iterable.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IterableAdaptorTest
{
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Of implements FunctionalIterableTest.Of<Object,IterableAdaptor.Of<Object>>
	{
		@Override
		public IterableAdaptor.Of<Object> getReducible(Object[] objects)
		{
			return new IterableAdaptor.Of<>(Arrays.asList(objects));
		}

		@ParameterizedTest
		@MethodSource("getArraysAsArguments")
		@DisplayName("Adaptor yields same sequence as the underlying iterator.")
		public void testOf(Object[] objects)
		{
			List<Object> expected = Arrays.asList(objects);
			IterableAdaptor.Of<Object> actual = new IterableAdaptor.Of<>(expected);
			assertIterableEquals(expected, actual);
		}

		@Test
		@DisplayName("Adapter on null throws NullPointerException.")
		public void testOf_Null()
		{
			assertThrows(NullPointerException.class, () -> new IterableAdaptor.Of<>(null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfDouble implements FunctionalPrimitiveIterableTest.OfDouble<IterableAdaptor.OfDouble>
	{
		@Override
		public IterableAdaptor.OfDouble getReducible(double[] numbers)
		{
			PrimitiveIterable.OfDouble iterable = asNonFunctionalIterable(numbers);
			return new IterableAdaptor.OfDouble(iterable);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		@DisplayName("Adaptor yields same sequence as the underlying iterator.")
		public void testOfDouble(double[] numbers)
		{
			PrimitiveIterable.OfDouble expected = asNonFunctionalIterable(numbers);
			IterableAdaptor.OfDouble actual = new IterableAdaptor.OfDouble(expected);
			assertIterableEquals(expected, actual);
		}

		@Test
		@DisplayName("Adapter on null throws NullPointerException.")
		public void testOfDouble_Null()
		{
			assertThrows(NullPointerException.class, () -> new IterableAdaptor.OfDouble(null));
		}

		private PrimitiveIterable.OfDouble asNonFunctionalIterable(double[] numbers)
		{
			List<Double> boxed = FunctionalIterator.ofDouble(numbers).collect(new ArrayList<>());
			return PrimitiveIterable.unboxDouble(boxed);
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfInt implements FunctionalPrimitiveIterableTest.OfInt<IterableAdaptor.OfInt>
	{
		@Override
		public IterableAdaptor.OfInt getReducible(int[] numbers)
		{
			PrimitiveIterable.OfInt iterable = asNonFunctionalIterable(numbers);
			return new IterableAdaptor.OfInt(iterable);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		@DisplayName("Adaptor yields same sequence as the underlying iterator.")
		public void testOfInt(int[] numbers)
		{
			PrimitiveIterable.OfInt expected = asNonFunctionalIterable(numbers);
			IterableAdaptor.OfInt actual = new IterableAdaptor.OfInt(expected);
			assertIterableEquals(expected, actual);
		}

		@Test
		@DisplayName("Adapter on null throws NullPointerException.")
		public void testOfInt_Null()
		{
			assertThrows(NullPointerException.class, () -> new IterableAdaptor.OfInt(null));
		}

		private PrimitiveIterable.OfInt asNonFunctionalIterable(int[] numbers)
		{
			List<Integer> boxed = FunctionalIterator.ofInt(numbers).collect(new ArrayList<>());
			return PrimitiveIterable.unboxInt(boxed);
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfLong implements FunctionalPrimitiveIterableTest.OfLong<IterableAdaptor.OfLong>
	{
		@Override
		public IterableAdaptor.OfLong getReducible(long[] numbers)
		{
			PrimitiveIterable.OfLong iterable = asNonFunctionalIterable(numbers);
			return new IterableAdaptor.OfLong(iterable);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		@DisplayName("Adaptor yields same sequence as the underlying iterator.")
		public void testOfLong(long[] numbers)
		{
			PrimitiveIterable.OfLong expected = asNonFunctionalIterable(numbers);
			IterableAdaptor.OfLong actual = new IterableAdaptor.OfLong(expected);
			assertIterableEquals(expected, actual);
		}

		@Test
		@DisplayName("Adapter on null throws NullPointerException.")
		public void testOfLong_Null()
		{
			assertThrows(NullPointerException.class, () -> new IterableAdaptor.OfLong(null));
		}

		private PrimitiveIterable.OfLong asNonFunctionalIterable(long[] numbers)
		{
			List<Long> boxed = FunctionalIterator.ofLong(numbers).collect(new ArrayList<>());
			return PrimitiveIterable.unboxLong(boxed);
		}
	}
}
