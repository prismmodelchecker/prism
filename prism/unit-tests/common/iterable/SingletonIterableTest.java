package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;

import java.util.function.Supplier;
import java.util.stream.Stream;

interface SingletonIterableTest<E, T extends SingletonIterable<E>> extends FunctionalIterableTest<E,T>
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
	class Of implements SingletonIterableTest<Object,SingletonIterable.Of<Object>>, FunctionalIterableTest.Of<Object,SingletonIterable.Of<Object>>
	{
		@Override
		public SingletonIterable.Of<Object> getReducible(Object[] objects)
		{
			assert objects.length == 1 : "singleton array expected";
			return new SingletonIterable.Of<>(objects[0]);
		}

		@Override
		public Stream<Supplier<SingletonIterable.Of<Object>>> getDuplicatesReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<SingletonIterable.Of<Object>>> getEmptyReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<SingletonIterable.Of<Object>>> getMultitonReducibles()
		{
			return Stream.empty();
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfDouble implements SingletonIterableTest<Double,SingletonIterable.OfDouble>, FunctionalPrimitiveIterableTest.OfDouble<SingletonIterable.OfDouble>
	{
		@Override
		public SingletonIterable.OfDouble getReducible(double[] numbers)
		{
			assert numbers.length == 1 : "singleton array expected";
			return new SingletonIterable.OfDouble(numbers[0]);
		}

		@Override
		public Stream<Supplier<SingletonIterable.OfDouble>> getDuplicatesReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<SingletonIterable.OfDouble>> getEmptyReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<SingletonIterable.OfDouble>> getMultitonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatch_Empty(Supplier<SingletonIterable.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testAllMatchDoublePredicate_Empty(Supplier<SingletonIterable.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testDetectDoublePredicate_Empty(Supplier<SingletonIterable.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testDetectDoublePredicate_Multiton(Supplier<SingletonIterable.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testNoneMatchDoublePredicate_Empty(Supplier<SingletonIterable.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMax_Multiton(Supplier<SingletonIterable.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMin_Multiton(Supplier<SingletonIterable.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Empty(Supplier<SingletonIterable.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Multiton(Supplier<SingletonIterable.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testSum_Empty(Supplier<SingletonIterable.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMin_Empty(Supplier<SingletonIterable.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMax_Empty(Supplier<SingletonIterable.OfDouble> supplier)
		{ /* singletons hold exactly one value */ }
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfInt implements SingletonIterableTest<Integer,SingletonIterable.OfInt>, FunctionalPrimitiveIterableTest.OfInt<SingletonIterable.OfInt>
	{
		@Override
		public SingletonIterable.OfInt getReducible(int[] numbers)
		{
			assert numbers.length == 1 : "singleton array expected";
			return new SingletonIterable.OfInt(numbers[0]);
		}

		@Override
		public Stream<Supplier<SingletonIterable.OfInt>> getDuplicatesReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<SingletonIterable.OfInt>> getEmptyReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<SingletonIterable.OfInt>> getMultitonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatch_Empty(Supplier<SingletonIterable.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testAllMatchIntPredicate_Empty(Supplier<SingletonIterable.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testDetectIntPredicate_Empty(Supplier<SingletonIterable.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testDetectIntPredicate_Multiton(Supplier<SingletonIterable.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMax_Multiton(Supplier<SingletonIterable.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMin_Multiton(Supplier<SingletonIterable.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testNoneMatchIntPredicate_Empty(Supplier<SingletonIterable.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Empty(Supplier<SingletonIterable.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Multiton(Supplier<SingletonIterable.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceIntBinaryOperator_Empty(Supplier<SingletonIterable.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceIntBinaryOperator_Multiton(Supplier<SingletonIterable.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceLongBinaryOperator_Empty(Supplier<SingletonIterable.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceLongBinaryOperator_Multiton(Supplier<SingletonIterable.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMin_Empty(Supplier<SingletonIterable.OfInt> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMax_Empty(Supplier<SingletonIterable.OfInt> supplier)
		{ /* singletons hold exactly one value */ }
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfLong implements SingletonIterableTest<Long,SingletonIterable.OfLong>, FunctionalPrimitiveIterableTest.OfLong<SingletonIterable.OfLong>
	{
		@Override
		public SingletonIterable.OfLong getReducible(long[] numbers)
		{
			assert numbers.length == 1 : "singleton array expected";
			return new SingletonIterable.OfLong(numbers[0]);
		}

		@Override
		public Stream<Supplier<SingletonIterable.OfLong>> getDuplicatesReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<SingletonIterable.OfLong>> getEmptyReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<SingletonIterable.OfLong>> getMultitonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatchLongPredicate_Empty(Supplier<SingletonIterable.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testDetectLongPredicate_Empty(Supplier<SingletonIterable.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testDetectLongPredicate_Multiton(Supplier<SingletonIterable.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMax_Multiton(Supplier<SingletonIterable.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMin_Multiton(Supplier<SingletonIterable.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testNoneMatchLongPredicate_Empty(Supplier<SingletonIterable.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceLongBinaryOperator_Empty(Supplier<SingletonIterable.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testReduceLongBinaryOperator_Multiton(Supplier<SingletonIterable.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMin_Empty(Supplier<SingletonIterable.OfLong> supplier)
		{ /* singletons hold exactly one value */ }

		@Override
		public void testMax_Empty(Supplier<SingletonIterable.OfLong> supplier)
		{ /* singletons hold exactly one value */ }
	}
}
