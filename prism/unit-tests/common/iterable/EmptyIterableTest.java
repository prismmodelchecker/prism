package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;

import java.util.function.Supplier;
import java.util.stream.Stream;

interface EmptyIterableTest<E, T extends EmptyIterable<E>> extends FunctionalIterableTest<E,T>
{
	@Override
	default void testAllMatch(Supplier<T> supplier)
	{ /* empty reducibles hold no value */ }

	@Override
	default void testAnyMatch(Supplier<T> supplier)
	{ /* empty reducibles hold no value */ }

	@Override
	default void testDetect_Multiton(Supplier<T> supplier)
	{ /* empty reducibles hold no value */ }

	@Override
	default void testDetect_Singleton(Supplier<T> supplier)
	{ /* empty reducibles hold no value */ }

	@Override
	default void testFlatMapToNull(Supplier<T> supplier)
	{ /* empty reducibles hold no value */ }

	@Override
	default void testNoneMatch(Supplier<T> supplier)
	{ /* empty reducibles hold no value */ }

	@Override
	default void testIsEmpty_NonEmpty(Supplier<T> supplier)
	{ /* empty reducibles hold no value */ }

	@Override
	default void testReduceBinaryOperatorOfE_Singleton(Supplier<T> supplier)
	{ /* empty reducibles hold no value */ }

	@Override
	default void testReduceBinaryOperatorOfE_Multiton(Supplier<T> supplier)
	{ /* empty reducibles hold no value */ }

	@Override
	default void testReduceBinary_ResultNull(Supplier<T> supplier)
	{ /* empty reducibles hold no value */ }



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Of implements EmptyIterableTest<Object,EmptyIterable.Of<Object>>, FunctionalIterableTest.Of<Object,EmptyIterable.Of<Object>>
	{
		@Override
		public EmptyIterable.Of<Object> getReducible(Object[] objects)
		{
			assert objects.length == 0 : "empty array expected";
			return EmptyIterable.of();
		}

		@Override
		public Stream<Supplier<EmptyIterable.Of<Object>>> getDuplicatesReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<EmptyIterable.Of<Object>>> getSingletonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<EmptyIterable.Of<Object>>> getMultitonReducibles()
		{
			return Stream.empty();
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfDouble implements EmptyIterableTest<Double,EmptyIterable.OfDouble>, FunctionalPrimitiveIterableTest.OfDouble<EmptyIterable.OfDouble>
	{
		@Override
		public EmptyIterable.OfDouble getReducible(double[] numbers)
		{
			assert numbers.length == 0 : "empty array expected";
			return EmptyIterable.ofDouble();
		}

		@Override
		public Stream<Supplier<EmptyIterable.OfDouble>> getDuplicatesReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<EmptyIterable.OfDouble>> getSingletonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<EmptyIterable.OfDouble>> getMultitonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatchDoublePredicate(Supplier<EmptyIterable.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testAnyMatchDoublePredicate(Supplier<EmptyIterable.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testDetectDoublePredicate_Multiton(Supplier<EmptyIterable.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testDetectDoublePredicate_Singleton(Supplier<EmptyIterable.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testFlatMapDoubleToNull(Supplier<EmptyIterable.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMax_Singleton(Supplier<EmptyIterable.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMax_Multiton(Supplier<EmptyIterable.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMin_Singleton(Supplier<EmptyIterable.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMin_Multiton(Supplier<EmptyIterable.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testNoneMatchDoublePredicate(Supplier<EmptyIterable.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Singleton(Supplier<EmptyIterable.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Multiton(Supplier<EmptyIterable.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfInt implements EmptyIterableTest<Integer,EmptyIterable.OfInt>, FunctionalPrimitiveIterableTest.OfInt<EmptyIterable.OfInt>
	{
		@Override
		public EmptyIterable.OfInt getReducible(int[] numbers)
		{
			assert numbers.length == 0 : "empty array expected";
			return EmptyIterable.ofInt();
		}

		@Override
		public Stream<Supplier<EmptyIterable.OfInt>> getDuplicatesReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<EmptyIterable.OfInt>> getSingletonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<EmptyIterable.OfInt>> getMultitonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatchIntPredicate(Supplier<EmptyIterable.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testAnyMatchIntPredicate(Supplier<EmptyIterable.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testDetectIntPredicate_Multiton(Supplier<EmptyIterable.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testDetectIntPredicate_Singleton(Supplier<EmptyIterable.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testFlatMapIntToNull(Supplier<EmptyIterable.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMax_Singleton(Supplier<EmptyIterable.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMax_Multiton(Supplier<EmptyIterable.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMin_Singleton(Supplier<EmptyIterable.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMin_Multiton(Supplier<EmptyIterable.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testNoneMatchIntPredicate(Supplier<EmptyIterable.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Singleton(Supplier<EmptyIterable.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Multiton(Supplier<EmptyIterable.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceIntBinaryOperator_Singleton(Supplier<EmptyIterable.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceIntBinaryOperator_Multiton(Supplier<EmptyIterable.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceLongBinaryOperator_Singleton(Supplier<EmptyIterable.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceLongBinaryOperator_Multiton(Supplier<EmptyIterable.OfInt> supplier)
		{ /* empty reducibles hold no value */ }
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfLong implements EmptyIterableTest<Long,EmptyIterable.OfLong>, FunctionalPrimitiveIterableTest.OfLong<EmptyIterable.OfLong>
	{
		@Override
		public EmptyIterable.OfLong getReducible(long[] numbers)
		{
			assert numbers.length == 0 : "empty array expected";
			return EmptyIterable.ofLong();
		}

		@Override
		public Stream<Supplier<EmptyIterable.OfLong>> getDuplicatesReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<EmptyIterable.OfLong>> getSingletonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<EmptyIterable.OfLong>> getMultitonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatchLongPredicate(Supplier<EmptyIterable.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testAnyMatch(Supplier<EmptyIterable.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testAnyMatchLongPredicate(Supplier<EmptyIterable.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testDetectLongPredicate_Multiton(Supplier<EmptyIterable.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testDetectLongPredicate_Singleton(Supplier<EmptyIterable.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testFlatMapLongToNull(Supplier<EmptyIterable.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMax_Singleton(Supplier<EmptyIterable.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMax_Multiton(Supplier<EmptyIterable.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMin_Singleton(Supplier<EmptyIterable.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMin_Multiton(Supplier<EmptyIterable.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testNoneMatchLongPredicate(Supplier<EmptyIterable.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceLongBinaryOperator_Singleton(Supplier<EmptyIterable.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceLongBinaryOperator_Multiton(Supplier<EmptyIterable.OfLong> supplier)
		{ /* empty reducibles hold no value */ }
	}
}