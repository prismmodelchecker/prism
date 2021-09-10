package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;

import java.util.function.Supplier;
import java.util.stream.Stream;

interface EmptyIteratorTest<E, T extends EmptyIterator<E>> extends FunctionalIteratorTest<E,T>
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
	class Of implements EmptyIteratorTest<Object,EmptyIterator.Of<Object>>, FunctionalIteratorTest.Of<Object,EmptyIterator.Of<Object>>
	{
		@Override
		public EmptyIterator.Of<Object> getReducible(Object[] objects)
		{
			assert objects.length == 0 : "empty array expected";
			return EmptyIterator.of();
		}

		@Override
		public Stream<Supplier<EmptyIterator.Of<Object>>> getDuplicatesReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<EmptyIterator.Of<Object>>> getSingletonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<EmptyIterator.Of<Object>>> getMultitonReducibles()
		{
			return Stream.empty();
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfDouble implements EmptyIteratorTest<Double,EmptyIterator.OfDouble>, FunctionalPrimitiveIteratorTest.OfDouble<EmptyIterator.OfDouble>
	{
		@Override
		public EmptyIterator.OfDouble getReducible(double[] numbers)
		{
			assert numbers.length == 0 : "empty array expected";
			return EmptyIterator.ofDouble();
		}

		@Override
		public Stream<Supplier<EmptyIterator.OfDouble>> getDuplicatesReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<EmptyIterator.OfDouble>> getSingletonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<EmptyIterator.OfDouble>> getMultitonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatchDoublePredicate(Supplier<EmptyIterator.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testAnyMatchDoublePredicate(Supplier<EmptyIterator.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testDetectDoublePredicate_Multiton(Supplier<EmptyIterator.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testDetectDoublePredicate_Singleton(Supplier<EmptyIterator.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testFlatMapDoubleToNull(Supplier<EmptyIterator.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMax_Singleton(Supplier<EmptyIterator.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMax_Multiton(Supplier<EmptyIterator.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMin_Singleton(Supplier<EmptyIterator.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMin_Multiton(Supplier<EmptyIterator.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testNoneMatchDoublePredicate(Supplier<EmptyIterator.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Singleton(Supplier<EmptyIterator.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Multiton(Supplier<EmptyIterator.OfDouble> supplier)
		{ /* empty reducibles hold no value */ }
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfInt implements EmptyIteratorTest<Integer,EmptyIterator.OfInt>, FunctionalPrimitiveIteratorTest.OfInt<EmptyIterator.OfInt>
	{
		@Override
		public EmptyIterator.OfInt getReducible(int[] numbers)
		{
			assert numbers.length == 0 : "empty array expected";
			return EmptyIterator.ofInt();
		}

		@Override
		public Stream<Supplier<EmptyIterator.OfInt>> getDuplicatesReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<EmptyIterator.OfInt>> getSingletonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<EmptyIterator.OfInt>> getMultitonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatchIntPredicate(Supplier<EmptyIterator.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testAnyMatchIntPredicate(Supplier<EmptyIterator.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testDetectIntPredicate_Multiton(Supplier<EmptyIterator.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testDetectIntPredicate_Singleton(Supplier<EmptyIterator.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testFlatMapIntToNull(Supplier<EmptyIterator.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMax_Singleton(Supplier<EmptyIterator.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMax_Multiton(Supplier<EmptyIterator.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMin_Singleton(Supplier<EmptyIterator.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMin_Multiton(Supplier<EmptyIterator.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testNoneMatchIntPredicate(Supplier<EmptyIterator.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Singleton(Supplier<EmptyIterator.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceDoubleBinaryOperator_Multiton(Supplier<EmptyIterator.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceIntBinaryOperator_Singleton(Supplier<EmptyIterator.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceIntBinaryOperator_Multiton(Supplier<EmptyIterator.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceLongBinaryOperator_Singleton(Supplier<EmptyIterator.OfInt> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceLongBinaryOperator_Multiton(Supplier<EmptyIterator.OfInt> supplier)
		{ /* empty reducibles hold no value */ }
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfLong implements EmptyIteratorTest<Long,EmptyIterator.OfLong>, FunctionalPrimitiveIteratorTest.OfLong<EmptyIterator.OfLong>
	{
		@Override
		public EmptyIterator.OfLong getReducible(long[] numbers)
		{
			assert numbers.length == 0 : "empty array expected";
			return EmptyIterator.ofLong();
		}

		@Override
		public Stream<Supplier<EmptyIterator.OfLong>> getDuplicatesReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<EmptyIterator.OfLong>> getSingletonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public Stream<Supplier<EmptyIterator.OfLong>> getMultitonReducibles()
		{
			return Stream.empty();
		}

		@Override
		public void testAllMatchLongPredicate(Supplier<EmptyIterator.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testAnyMatch(Supplier<EmptyIterator.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testAnyMatchLongPredicate(Supplier<EmptyIterator.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testDetectLongPredicate_Multiton(Supplier<EmptyIterator.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testDetectLongPredicate_Singleton(Supplier<EmptyIterator.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testFlatMapLongToNull(Supplier<EmptyIterator.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMax_Singleton(Supplier<EmptyIterator.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMax_Multiton(Supplier<EmptyIterator.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMin_Singleton(Supplier<EmptyIterator.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testMin_Multiton(Supplier<EmptyIterator.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testNoneMatchLongPredicate(Supplier<EmptyIterator.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceLongBinaryOperator_Singleton(Supplier<EmptyIterator.OfLong> supplier)
		{ /* empty reducibles hold no value */ }

		@Override
		public void testReduceLongBinaryOperator_Multiton(Supplier<EmptyIterator.OfLong> supplier)
		{ /* empty reducibles hold no value */ }
	}
}