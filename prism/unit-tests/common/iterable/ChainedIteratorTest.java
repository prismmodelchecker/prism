package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static common.iterable.Assertions.assertIteratorEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

interface ChainedIteratorTest
{
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Of implements ChainedIteratorTest, FunctionalIteratorTest.Of<Object,ChainedIterator.Of<Object>>
	{
		@Override
		public ChainedIterator.Of<Object> getReducible(Object[] objects)
		{
			throw new RuntimeException("Should not be called");
		}

		protected Stream<FunctionalIterable<Iterable<Object>>> split(Object[] objects)
		{
			// 1. whole sequence
			FunctionalIterable<Iterable<Object>> complete = FunctionalIterable.of(Arrays.asList(objects));
			// 2. split & pad with empty
			List<Object> empty = Collections.emptyList();
			int l1 = objects.length / 2;
			Object[] first = new Object[l1];
			System.arraycopy(objects, 0, first, 0, l1);
			int l2 = objects.length - l1;
			Object[] second = new Object[l2];
			System.arraycopy(objects, l1, second, 0, l2);
			FunctionalIterable<Iterable<Object>>  chunks = FunctionalIterable.of(empty, Arrays.asList(first), empty, Arrays.asList(second), empty);
			return Stream.of(complete, chunks);
		}

		public Stream<FunctionalIterable<Iterable<Object>>> getChains()
		{
			return getArraysOfObject().flatMap(this::split);
		}

		@Override
		public Stream<Supplier<ChainedIterator.Of<Object>>> getDuplicatesReducibles()
		{
			Stream<FunctionalIterable<Iterable<Object>>> splits = getDuplicatesArraysOfObject().flatMap(this::split);
			return splits.map(args -> () -> new ChainedIterator.Of<>(args.map(Iterable::iterator).iterator()));
		}

		@Override
		public Stream<Supplier<ChainedIterator.Of<Object>>> getEmptyReducibles()
		{
			Stream<FunctionalIterable<Iterable<Object>>> splits = getEmptyArraysOfObject().flatMap(this::split);
			return splits.map(args -> () -> new ChainedIterator.Of<>(args.map(Iterable::iterator).iterator()));
		}

		@Override
		public Stream<Supplier<ChainedIterator.Of<Object>>> getSingletonReducibles()
		{
			Stream<FunctionalIterable<Iterable<Object>>> splits = getSingletonArraysOfObject().flatMap(this::split);
			return splits.map(args -> () -> new ChainedIterator.Of<>(args.map(Iterable::iterator).iterator()));
		}

		@Override
		public Stream<Supplier<ChainedIterator.Of<Object>>> getMultitonReducibles()
		{
			Stream<FunctionalIterable<Iterable<Object>>> splits = getMultitonArraysOfObject().flatMap(this::split);
			return splits.map(args -> () -> new ChainedIterator.Of<>(args.map(Iterable::iterator).iterator()));
		}

		@ParameterizedTest
		@MethodSource("getChains")
		public void testOf(FunctionalIterable<Iterable<Object>> chain)
		{
			List<Object> expected = new ArrayList<>();
			for (Iterable<?> iterable : chain) {
				for (Object each : iterable) {
					expected.add(each);
				}
			}
			FunctionalIterator<Iterator<Object>> iterators = Reducible.extend(chain.iterator()).map(Iterable::iterator);
			ChainedIterator.Of<Object> actual = new ChainedIterator.Of<>(iterators);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOf_Null()
		{
			assertThrows(NullPointerException.class, () -> new ChainedIterator.Of<>(null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfDouble implements ChainedIteratorTest, FunctionalPrimitiveIteratorTest.OfDouble<ChainedIterator.OfDouble>
	{
		@Override
		public ChainedIterator.OfDouble getReducible(double[] numbers)
		{
			throw new RuntimeException("Should not be called");
		}

		protected Stream<FunctionalIterable<PrimitiveIterable.OfDouble>> split(double[] numbers)
		{
			// 1. whole sequence
			FunctionalIterable<PrimitiveIterable.OfDouble> complete = FunctionalIterable.of(new IterableArray.OfDouble(numbers));
			// 2. split & pad with empty
			EmptyIterable.OfDouble empty = EmptyIterable.ofDouble();
			int l1 = numbers.length / 2;
			double[] first = new double[l1];
			System.arraycopy(numbers, 0, first, 0, l1);
			int l2 = numbers.length - l1;
			double[] second = new double[l2];
			System.arraycopy(numbers, l1, second, 0, l2);
			FunctionalIterable<PrimitiveIterable.OfDouble>  chunks = FunctionalIterable.of(empty, new IterableArray.OfDouble(first), empty, new IterableArray.OfDouble(second), empty);
			return Stream.of(complete, chunks);
		}

		public Stream<FunctionalIterable<PrimitiveIterable.OfDouble>> getChains()
		{
			return getArraysOfDouble().flatMap(this::split);
		}

		@Override
		public Stream<Supplier<ChainedIterator.OfDouble>> getDuplicatesReducibles()
		{
			Stream<FunctionalIterable<PrimitiveIterable.OfDouble>> splits = getDuplicatesArraysOfDouble().flatMap(this::split);
			return splits.map(args -> () -> new ChainedIterator.OfDouble(args.map(PrimitiveIterable.OfDouble::iterator).iterator()));
		}

		@Override
		public Stream<Supplier<ChainedIterator.OfDouble>> getEmptyReducibles()
		{
			Stream<FunctionalIterable<PrimitiveIterable.OfDouble>> splits = getEmptyArraysOfDouble().flatMap(this::split);
			return splits.map(args -> () -> new ChainedIterator.OfDouble(args.map(PrimitiveIterable.OfDouble::iterator).iterator()));
		}

		@Override
		public Stream<Supplier<ChainedIterator.OfDouble>> getSingletonReducibles()
		{
			Stream<FunctionalIterable<PrimitiveIterable.OfDouble>> splits = getSingletonArraysOfDouble().flatMap(this::split);
			return splits.map(args -> () -> new ChainedIterator.OfDouble(args.map(PrimitiveIterable.OfDouble::iterator).iterator()));
		}

		@Override
		public Stream<Supplier<ChainedIterator.OfDouble>> getMultitonReducibles()
		{
			Stream<FunctionalIterable<PrimitiveIterable.OfDouble>> splits = getMultitonArraysOfDouble().flatMap(this::split);
			return splits.map(args -> () -> new ChainedIterator.OfDouble(args.map(PrimitiveIterable.OfDouble::iterator).iterator()));
		}

		@ParameterizedTest
		@MethodSource("getChains")
		public void testOf(FunctionalIterable<PrimitiveIterable.OfDouble> chain)
		{
			List<Double> expected = new ArrayList<>();
			for (Iterable<Double> iterable : chain) {
				for (Double each : iterable) {
					expected.add(each);
				}
			}
			FunctionalIterator<PrimitiveIterator.OfDouble> iterators = Reducible.extend(chain.iterator()).map(PrimitiveIterable.OfDouble::iterator);
			ChainedIterator.OfDouble actual = new ChainedIterator.OfDouble(iterators);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOfDouble_Null()
		{
			assertThrows(NullPointerException.class, () -> new ChainedIterator.OfDouble(null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfInt implements ChainedIteratorTest, FunctionalPrimitiveIteratorTest.OfInt<ChainedIterator.OfInt>
	{
		@Override
		public ChainedIterator.OfInt getReducible(int[] numbers)
		{
			throw new RuntimeException("Should not be called");
		}

		protected Stream<FunctionalIterable<PrimitiveIterable.OfInt>> split(int[] numbers)
		{
			// 1. whole sequence
			FunctionalIterable<PrimitiveIterable.OfInt> complete = FunctionalIterable.of(new IterableArray.OfInt(numbers));
			// 2. split & pad with empty
			EmptyIterable.OfInt empty = EmptyIterable.ofInt();
			int l1 = numbers.length / 2;
			int[] first = new int[l1];
			System.arraycopy(numbers, 0, first, 0, l1);
			int l2 = numbers.length - l1;
			int[] second = new int[l2];
			System.arraycopy(numbers, l1, second, 0, l2);
			FunctionalIterable<PrimitiveIterable.OfInt>  chunks = FunctionalIterable.of(empty, new IterableArray.OfInt(first), empty, new IterableArray.OfInt(second), empty);
			return Stream.of(complete, chunks);
		}

		public Stream<FunctionalIterable<PrimitiveIterable.OfInt>> getChains()
		{
			return getArraysOfInt().flatMap(this::split);
		}

		@Override
		public Stream<Supplier<ChainedIterator.OfInt>> getDuplicatesReducibles()
		{
			Stream<FunctionalIterable<PrimitiveIterable.OfInt>> splits = getDuplicatesArraysOfInt().flatMap(this::split);
			return splits.map(args -> () -> new ChainedIterator.OfInt(args.map(PrimitiveIterable.OfInt::iterator).iterator()));
		}

		@Override
		public Stream<Supplier<ChainedIterator.OfInt>> getEmptyReducibles()
		{
			Stream<FunctionalIterable<PrimitiveIterable.OfInt>> splits = getEmptyArraysOfInt().flatMap(this::split);
			return splits.map(args -> () -> new ChainedIterator.OfInt(args.map(PrimitiveIterable.OfInt::iterator).iterator()));
		}

		@Override
		public Stream<Supplier<ChainedIterator.OfInt>> getSingletonReducibles()
		{
			Stream<FunctionalIterable<PrimitiveIterable.OfInt>> splits = getSingletonArraysOfInt().flatMap(this::split);
			return splits.map(args -> () -> new ChainedIterator.OfInt(args.map(PrimitiveIterable.OfInt::iterator).iterator()));
		}

		@Override
		public Stream<Supplier<ChainedIterator.OfInt>> getMultitonReducibles()
		{
			Stream<FunctionalIterable<PrimitiveIterable.OfInt>> splits = getMultitonArraysOfInt().flatMap(this::split);
			return splits.map(args -> () -> new ChainedIterator.OfInt(args.map(PrimitiveIterable.OfInt::iterator).iterator()));
		}

		@ParameterizedTest
		@MethodSource("getChains")
		public void testOf(FunctionalIterable<PrimitiveIterable.OfInt> chain)
		{
			List<Integer> expected = new ArrayList<>();
			for (Iterable<Integer> iterable : chain) {
				for (Integer each : iterable) {
					expected.add(each);
				}
			}
			FunctionalIterator<PrimitiveIterator.OfInt> iterators = Reducible.extend(chain.iterator()).map(PrimitiveIterable.OfInt::iterator);
			ChainedIterator.OfInt actual = new ChainedIterator.OfInt(iterators);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOfDouble_Null()
		{
			assertThrows(NullPointerException.class, () -> new ChainedIterator.OfInt(null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class OfLong implements ChainedIteratorTest, FunctionalPrimitiveIteratorTest.OfLong<ChainedIterator.OfLong>
	{
		@Override
		public ChainedIterator.OfLong getReducible(long[] numbers)
		{
			throw new RuntimeException("Should not be called");
		}

		protected Stream<FunctionalIterable<PrimitiveIterable.OfLong>> split(long[] numbers)
		{
			// 1. whole sequence
			FunctionalIterable<PrimitiveIterable.OfLong> complete = FunctionalIterable.of(new IterableArray.OfLong(numbers));
			// 2. split & pad with empty
			EmptyIterable.OfLong empty = EmptyIterable.ofLong();
			int l1 = numbers.length / 2;
			long[] first = new long[l1];
			System.arraycopy(numbers, 0, first, 0, l1);
			int l2 = numbers.length - l1;
			long[] second = new long[l2];
			System.arraycopy(numbers, l1, second, 0, l2);
			FunctionalIterable<PrimitiveIterable.OfLong>  chunks = FunctionalIterable.of(empty, new IterableArray.OfLong(first), empty, new IterableArray.OfLong(second), empty);
			return Stream.of(complete, chunks);
		}

		public Stream<Iterable<PrimitiveIterable.OfLong>> getChains()
		{
			return getArraysOfLong().flatMap(this::split);
		}

		@Override
		public Stream<Supplier<ChainedIterator.OfLong>> getDuplicatesReducibles()
		{
			Stream<FunctionalIterable<PrimitiveIterable.OfLong>> splits = getDuplicatesArraysOfLong().flatMap(this::split);
			return splits.map(args -> () -> new ChainedIterator.OfLong(args.map(PrimitiveIterable.OfLong::iterator).iterator()));
		}

		@Override
		public Stream<Supplier<ChainedIterator.OfLong>> getEmptyReducibles()
		{
			Stream<FunctionalIterable<PrimitiveIterable.OfLong>> splits = getEmptyArraysOfLong().flatMap(this::split);
			return splits.map(args -> () -> new ChainedIterator.OfLong(args.map(PrimitiveIterable.OfLong::iterator).iterator()));
		}

		@Override
		public Stream<Supplier<ChainedIterator.OfLong>> getSingletonReducibles()
		{
			Stream<FunctionalIterable<PrimitiveIterable.OfLong>> splits = getSingletonArraysOfLong().flatMap(this::split);
			return splits.map(args -> () -> new ChainedIterator.OfLong(args.map(PrimitiveIterable.OfLong::iterator).iterator()));
		}

		@Override
		public Stream<Supplier<ChainedIterator.OfLong>> getMultitonReducibles()
		{
			Stream<FunctionalIterable<PrimitiveIterable.OfLong>> splits = getMultitonArraysOfLong().flatMap(this::split);
			return splits.map(args -> () -> new ChainedIterator.OfLong(args.map(PrimitiveIterable.OfLong::iterator).iterator()));
		}

		@ParameterizedTest
		@MethodSource("getChains")
		public void testOf(FunctionalIterable<PrimitiveIterable.OfLong> chain)
		{
			List<Long> expected = new ArrayList<>();
			for (Iterable<Long> iterable : chain) {
				for (Long each : iterable) {
					expected.add(each);
				}
			}
			FunctionalIterator<PrimitiveIterator.OfLong> iterators = Reducible.extend(chain.iterator()).map(PrimitiveIterable.OfLong::iterator);
			ChainedIterator.OfLong actual = new ChainedIterator.OfLong(iterators);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOfDouble_Null()
		{
			assertThrows(NullPointerException.class, () -> new ChainedIterator.OfLong(null));
		}
	}
}
