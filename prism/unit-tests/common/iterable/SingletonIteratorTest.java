package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;

import java.util.function.Supplier;
import java.util.stream.Stream;

interface SingletonIteratorTest<E, T extends SingletonIterator<E>> extends FunctionalIteratorTest<E,T>
{
	@Override
	default void testAllMatch_Empty(Supplier<T> supplier)
	{ /* singletons hold exactly one value */ }

	@Override
	default void testAnyMatch_Empty(Supplier<T> supplier)
	{ /* singletons hold exactly one value */ }

	@Override
	default void testDetect_Empty(Supplier<T> supplier)
	{ /* singletons hold exactly one value */ }

	@Override
	default void testDetect_Multiton(Supplier<T> supplier)
	{ /* singletons hold exactly one value */ }

	@Override
	default void testNoneMatch_Empty(Supplier<T> supplier)
	{ /* singletons hold exactly one value */ }

	@Override
	default void testIsEmpty_Empty(Supplier<T> supplier)
	{ /* singletons hold exactly one value */ }

	@Override
	default void testReduceBinary_ResultNull(Supplier<T> supplier)
	{ /* singletons hold exactly one value */}

	@Override
	default void testReduceBinaryOperatorOfE_Empty(Supplier<T> supplier)
	{ /* singletons hold exactly one value */ }

	@Override
	default void testReduceBinaryOperatorOfE_Multiton(Supplier<T> supplier)
	{ /* singletons hold exactly one value */ }



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Of implements SingletonIteratorTest<Object,SingletonIterator.Of<Object>>, FunctionalIteratorTest.Of<Object,SingletonIterator.Of<Object>>
	{
		@Override
		public SingletonIterator.Of<Object> getReducible(Object[] objects)
		{
			assert objects.length == 1 : "singleton array expected";
			return new SingletonIterator.Of<>(objects[0]);
		}

		@Override
		public Stream<Supplier<SingletonIterator.Of<Object>>> getDuplicatesReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<SingletonIterator.Of<Object>>> getEmptyReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<SingletonIterator.Of<Object>>> getMultitonReducibles()
		{
			return Stream.empty();
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfDouble implements SingletonIteratorTest<Double,SingletonIterator.OfDouble>, FunctionalPrimitiveIteratorTest.OfDouble<SingletonIterator.OfDouble>
	{
		@Override
		public SingletonIterator.OfDouble getReducible(double[] numbers)
		{
			assert numbers.length == 1 : "singleton array expected";
			return new SingletonIterator.OfDouble(numbers[0]);
		}

		@Override
		public Stream<Supplier<SingletonIterator.OfDouble>> getDuplicatesReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<SingletonIterator.OfDouble>> getEmptyReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<SingletonIterator.OfDouble>> getMultitonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatch_Empty(Supplier<SingletonIterator.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testAllMatchDoublePredicate_Empty(Supplier<SingletonIterator.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testDetectDoublePredicate_Empty(Supplier<SingletonIterator.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testDetectDoublePredicate_Multiton(Supplier<SingletonIterator.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testNoneMatchDoublePredicate_Empty(Supplier<SingletonIterator.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMax_Multiton(Supplier<SingletonIterator.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMin_Multiton(Supplier<SingletonIterator.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Empty(Supplier<SingletonIterator.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Multiton(Supplier<SingletonIterator.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testSum_Empty(Supplier<SingletonIterator.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMin_Empty(Supplier<SingletonIterator.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMax_Empty(Supplier<SingletonIterator.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfInt implements SingletonIteratorTest<Integer,SingletonIterator.OfInt>, FunctionalPrimitiveIteratorTest.OfInt<SingletonIterator.OfInt>
	{
		@Override
		public SingletonIterator.OfInt getReducible(int[] numbers)
		{
			assert numbers.length == 1 : "singleton array expected";
			return new SingletonIterator.OfInt(numbers[0]);
		}

		@Override
		public Stream<Supplier<SingletonIterator.OfInt>> getDuplicatesReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<SingletonIterator.OfInt>> getEmptyReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<SingletonIterator.OfInt>> getMultitonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatch_Empty(Supplier<SingletonIterator.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testAllMatchIntPredicate_Empty(Supplier<SingletonIterator.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testDetectIntPredicate_Empty(Supplier<SingletonIterator.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testDetectIntPredicate_Multiton(Supplier<SingletonIterator.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMax_Multiton(Supplier<SingletonIterator.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMin_Multiton(Supplier<SingletonIterator.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testNoneMatchIntPredicate_Empty(Supplier<SingletonIterator.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Empty(Supplier<SingletonIterator.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Multiton(Supplier<SingletonIterator.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceIntBinaryOperator_Empty(Supplier<SingletonIterator.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceIntBinaryOperator_Multiton(Supplier<SingletonIterator.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceLongBinaryOperator_Empty(Supplier<SingletonIterator.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceLongBinaryOperator_Multiton(Supplier<SingletonIterator.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMin_Empty(Supplier<SingletonIterator.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMax_Empty(Supplier<SingletonIterator.OfInt> supplier)
		{ /* singletons hold exactly one value */ }
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfLong implements SingletonIteratorTest<Long,SingletonIterator.OfLong>, FunctionalPrimitiveIteratorTest.OfLong<SingletonIterator.OfLong>
	{
		@Override
		public SingletonIterator.OfLong getReducible(long[] numbers)
		{
			assert numbers.length == 1 : "singleton array expected";
			return new SingletonIterator.OfLong(numbers[0]);
		}

		@Override
		public Stream<Supplier<SingletonIterator.OfLong>> getDuplicatesReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<SingletonIterator.OfLong>> getEmptyReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<SingletonIterator.OfLong>> getMultitonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatchLongPredicate_Empty(Supplier<SingletonIterator.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testDetectLongPredicate_Empty(Supplier<SingletonIterator.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testDetectLongPredicate_Multiton(Supplier<SingletonIterator.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMax_Multiton(Supplier<SingletonIterator.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMin_Multiton(Supplier<SingletonIterator.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testNoneMatchLongPredicate_Empty(Supplier<SingletonIterator.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceLongBinaryOperator_Empty(Supplier<SingletonIterator.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceLongBinaryOperator_Multiton(Supplier<SingletonIterator.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMin_Empty(Supplier<SingletonIterator.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMax_Empty(Supplier<SingletonIterator.OfLong> supplier)
		{ /* singletons hold exactly one value */ }
	}
}
