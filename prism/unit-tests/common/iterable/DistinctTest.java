package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public interface DistinctTest<E, T extends Distinct<E>>
{
	T getDistinct();

	boolean test(T distinct, E element);

	Stream<FunctionalIterable<E>> getElements();

	@ParameterizedTest
	@MethodSource("getElements")
	default void testGetSeen(FunctionalIterable<E> elements)
	{
		T distinct = getDistinct();
		assertTrue(distinct.getSeen().isEmpty());
		for (E e : elements) {
			test(distinct, e);
			test(distinct, e);
		}
		FunctionalIterable<E> seen = distinct.getSeen();
		for (E e : elements) {
			assertTrue(seen.contains(e));
		}
		for (E e : seen) {
			assertTrue(elements.contains(e));
		}
	}

	@ParameterizedTest
	@MethodSource("getElements")
	default void testTest(FunctionalIterable<E> elements)
	{
		T distinct = getDistinct();
		for (E e : elements) {
			assertTrue(test(distinct, e));
			assertFalse(test(distinct, e));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Of implements DistinctTest<String, Distinct.Of<String>>
	{
		@Override
		public Distinct.Of<String> getDistinct()
		{
			return new Distinct.Of<>();
		}

		@Override
		public boolean test(Distinct.Of<String> distinct, String element)
		{
			return distinct.test(element);
		}

		@Override
		public Stream<FunctionalIterable<String>> getElements()
		{
			return Stream.of(new IterableArray.Of<>("first", "second", "third"));
		}
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfDouble implements DistinctTest<Double, Distinct.OfDouble>
	{
		@Override
		public Distinct.OfDouble getDistinct()
		{
			return new Distinct.OfDouble();
		}

		@Override
		public boolean test(Distinct.OfDouble distinct, Double element)
		{
			return distinct.test(element);
		}

		@Override
		public Stream<FunctionalIterable<Double>> getElements()
		{
			return Stream.of(new IterableArray.Of<>(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN, 0.0, 1.0, 2.0, 3.0));
		}

		@Test
		public void testTest_PositiveZero()
		{
			Distinct.OfDouble distinct = getDistinct();
			assertTrue(distinct.test(+0.0));
			assertFalse(distinct.test(+0.0));
			assertFalse(distinct.test(-0.0));
		}

		@Test
		public void testTest_NegativeZero()
		{
			Distinct.OfDouble distinct = getDistinct();
			assertTrue(distinct.test(+0.0));
			assertFalse(distinct.test(-0.0));
			assertFalse(distinct.test(+0.0));
		}
	}

	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfInt implements DistinctTest<Integer, Distinct.OfInt>
	{
		@Override
		public Distinct.OfInt getDistinct()
		{
			return new Distinct.OfInt();
		}

		@Override
		public boolean test(Distinct.OfInt distinct, Integer element)
		{
			return distinct.test(element);
		}

		@Override
		public Stream<FunctionalIterable<Integer>> getElements()
		{
			return Stream.of(new IterableArray.Of<>(1, 2, 3));
		}
	}

	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfLong implements DistinctTest<Long, Distinct.OfLong>
	{
		@Override
		public Distinct.OfLong getDistinct()
		{
			return new Distinct.OfLong();
		}

		@Override
		public boolean test(Distinct.OfLong distinct, Long element)
		{
			return distinct.test(element);
		}

		@Override
		public Stream<FunctionalIterable<Long>> getElements()
		{
			return Stream.of(new IterableArray.Of<>(1L, 2L, 3L));
		}
	}
}
