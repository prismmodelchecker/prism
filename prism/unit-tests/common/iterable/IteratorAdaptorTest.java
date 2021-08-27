package common.iterable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;

import static common.iterable.Assertions.assertIteratorEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IteratorAdaptorTest
{
	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class Of implements FunctionalIteratorTest.Of<Object,IteratorAdaptor.Of<Object>>
	{
		@Override
		public IteratorAdaptor.Of<Object> getReducible(Object[] objects)
		{
			return new IteratorAdaptor.Of<>(Arrays.asList(objects).iterator());
		}

		@ParameterizedTest
		@MethodSource("getArraysAsArguments")
		@DisplayName("Adaptor yields same sequence as the underlying iterator.")
		public void testOf(Object[] objects)
		{
			List<Object> iterable = Arrays.asList(objects);
			Iterator<Object> expected = iterable.iterator();
			IteratorAdaptor.Of<Object> actual = new IteratorAdaptor.Of<>(iterable.iterator());
			assertIteratorEquals(expected, actual);
		}

		@Test
		@DisplayName("Adapter on null throws NullPointerException.")
		public void testOf_Null()
		{
			assertThrows(NullPointerException.class, () -> new IteratorAdaptor.Of<>(null));
		}

		@ParameterizedTest
		@MethodSource("getArraysAsArguments")
		@DisplayName("unwrap() answers the underlying iterator.")
		public void testUnwrap(Object[] objects)
		{
			Iterator<Object> expected = Arrays.asList(objects).iterator();
			Iterator<Object> actual = new IteratorAdaptor.Of<>(expected).unwrap();
			assertSame(expected, actual);
		}
	}



	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class OfDouble implements FunctionalPrimitiveIteratorTest.OfDouble<IteratorAdaptor.OfDouble>
	{
		@Override
		public IteratorAdaptor.OfDouble getReducible(double[] numbers)
		{
			PrimitiveIterable.OfDouble iterable = asNonFunctionalIterable(numbers);
			return new IteratorAdaptor.OfDouble(iterable.iterator());
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		@DisplayName("Adaptor yields same sequence as the underlying iterator.")
		public void testOfDouble(double[] numbers)
		{
			PrimitiveIterable.OfDouble expected = asNonFunctionalIterable(numbers);
			IteratorAdaptor.OfDouble actual = new IteratorAdaptor.OfDouble(expected.iterator());
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		@DisplayName("Adapter on null throws NullPointerException.")
		public void testOfDouble_Null()
		{
			assertThrows(NullPointerException.class, () -> new IteratorAdaptor.OfDouble(null));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		@DisplayName("unwrap() answers the underlying iterator.")
		public void testUnwrap(double[] numbers)
		{
			PrimitiveIterator.OfDouble expected = asNonFunctionalIterable(numbers).iterator();
			PrimitiveIterator.OfDouble actual = new IteratorAdaptor.OfDouble(expected).unwrap();
			assertSame(expected, actual);
		}

		private PrimitiveIterable.OfDouble asNonFunctionalIterable(double[] numbers)
		{
			List<Double> boxed = FunctionalIterator.ofDouble(numbers).collect(new ArrayList<>());
			return PrimitiveIterable.unboxDouble(boxed);
		}
	}



	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class OfInt implements FunctionalPrimitiveIteratorTest.OfInt<IteratorAdaptor.OfInt>
	{
		@Override
		public IteratorAdaptor.OfInt getReducible(int[] numbers)
		{
			PrimitiveIterable.OfInt iterable = asNonFunctionalIterable(numbers);
			return new IteratorAdaptor.OfInt(iterable.iterator());
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		@DisplayName("Adaptor yields same sequence as the underlying iterator.")
		public void testOfInt(int[] numbers)
		{
			PrimitiveIterable.OfInt expected = asNonFunctionalIterable(numbers);
			IteratorAdaptor.OfInt actual = new IteratorAdaptor.OfInt(expected.iterator());
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		@DisplayName("Adapter on null throws NullPointerException.")
		public void testOfInt_Null()
		{
			assertThrows(NullPointerException.class, () -> new IteratorAdaptor.OfInt(null));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		@DisplayName("unwrap() answers the underlying iterator.")
		public void testUnwrap(int[] numbers)
		{
			PrimitiveIterator.OfInt expected = asNonFunctionalIterable(numbers).iterator();
			PrimitiveIterator.OfInt actual = new IteratorAdaptor.OfInt(expected).unwrap();
			assertSame(expected, actual);
		}

		private PrimitiveIterable.OfInt asNonFunctionalIterable(int[] numbers)
		{
			List<Integer> boxed = FunctionalIterator.ofInt(numbers).collect(new ArrayList<>());
			return PrimitiveIterable.unboxInt(boxed);
		}
	}



	@Nested
	@TestInstance(Lifecycle.PER_CLASS)
	class OfLong implements FunctionalPrimitiveIteratorTest.OfLong<IteratorAdaptor.OfLong>
	{
		@Override
		public IteratorAdaptor.OfLong getReducible(long[] numbers)
		{
			PrimitiveIterable.OfLong iterable = asNonFunctionalIterable(numbers);
			return new IteratorAdaptor.OfLong(iterable.iterator());
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		@DisplayName("Adaptor yields same sequence as the underlying iterator.")
		public void testOfLong(long[] numbers)
		{
			PrimitiveIterable.OfLong expected = asNonFunctionalIterable(numbers);
			IteratorAdaptor.OfLong actual = new IteratorAdaptor.OfLong(expected.iterator());
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		@DisplayName("Adapter on null throws NullPointerException.")
		public void testOfLong_Null()
		{
			assertThrows(NullPointerException.class, () -> new IteratorAdaptor.OfLong(null));
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		@DisplayName("unwrap() answers the underlying iterator.")
		public void testUnwrap(long[] numbers)
		{
			PrimitiveIterator.OfLong expected = asNonFunctionalIterable(numbers).iterator();
			PrimitiveIterator.OfLong actual = new IteratorAdaptor.OfLong(expected).unwrap();
			assertSame(expected, actual);
		}

		private PrimitiveIterable.OfLong asNonFunctionalIterable(long[] numbers)
		{
			List<Long> boxed = FunctionalIterator.ofLong(numbers).collect(new ArrayList<>());
			return PrimitiveIterable.unboxLong(boxed);
		}
	}
}
