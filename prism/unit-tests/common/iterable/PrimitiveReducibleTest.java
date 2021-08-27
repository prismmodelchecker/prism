package common.iterable;

import common.functions.DoubleLongToDoubleFunction;
import common.functions.IntDoubleToIntFunction;
import common.functions.IntLongToIntFunction;
import common.functions.LongDoubleToLongFunction;
import common.functions.ObjDoubleFunction;
import common.functions.ObjIntFunction;
import common.functions.ObjLongFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import static common.iterable.Assertions.assertIterableEquals;
import static common.iterable.Assertions.*;
import static common.iterable.PrimitiveIterable.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public interface PrimitiveReducibleTest
{
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	interface OfDouble<T extends PrimitiveReducible.OfDouble<?>> extends ReducibleTest<Double, T>
	{
		@Override
		default Stream<Supplier<T>> getDuplicatesReducibles()
		{
			return getDuplicatesArraysOfDouble().map(args-> () -> getReducible(args));
		}

		@Override
		default Stream<Supplier<T>> getEmptyReducibles()
		{
			return getEmptyArraysOfDouble().map(args-> () -> getReducible(args));
		}

		@Override
		default Stream<Supplier<T>> getSingletonReducibles()
		{
			return getSingletonArraysOfDouble().map(args-> () -> getReducible(args));
		}

		@Override
		default Stream<Supplier<T>> getMultitonReducibles()
		{
			return getMultitonArraysOfDouble().map(args-> () -> getReducible(args));
		}

		T getReducible(double[] arguments);

		@Override
		default Iterable<Object> getExcluded(T reducible)
		{
			List<Double> candidates = getExclusionListOfDouble();
			reducible.forEach((double d) -> {
				candidates.remove(d);
				if (d == 0.0) {
					candidates.remove(-1 * d);
				}
			});
			List<Object> excluded = new ArrayList<>();
			excluded.addAll(getUniqueObjects());
			excluded.addAll(candidates);
			return excluded;
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testCollectDoubleArray(Supplier<T> supplier)
		{
			int n = (int) supplier.get().count();
			double[] expected = new double[n];
			Range.RangeIterator index = new Range(n).iterator();
			supplier.get().forEach((double d) -> expected[index.nextInt()] = d);
			double[] actual = supplier.get().collect(new double[n]);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testCollectDoubleArrayOffset(Supplier<T> supplier)
		{
			int n = (int) supplier.get().count(), offset = 2, tail = 3;
			double[] expected = new double[offset + n + tail];
			Range.RangeIterator index = new Range(offset, offset + n).iterator();
			supplier.get().forEach((double d) -> expected[index.nextInt()] = d);
			double[] actual = supplier.get().collect(new double[offset + n + tail], offset);
			assertArrayEquals(expected, actual);
		}

		@Test
		default void testCollectOfDouble_Null()
		{
			PrimitiveReducible.OfDouble<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.collect((double[]) null));
			assertThrows(NullPointerException.class, () -> reducible.collect((double[]) null, 0));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testCollectAndCountDoubleArray(Supplier<T> supplier)
		{
			int n = (int) supplier.get().count(), tail = 3;
			double[] expected = new double[n + tail];
			Range.RangeIterator index = new Range(n).iterator();
			supplier.get().forEach((double d) -> expected[index.nextInt()] = d);
			double[] actual = new double[n + tail];
			long count = supplier.get().collectAndCount(actual);
			assertEquals(n, count);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testCollectAndCountDoubleArrayOffset(Supplier<T> supplier)
		{
			int n = (int) supplier.get().count(), offset = 2, tail = 3;
			double[] expected = new double[offset + n + tail];
			Range.RangeIterator index = new Range(offset, offset + n).iterator();
			supplier.get().forEach((double d) -> expected[index.nextInt()] = d);
			double[] actual = new double[offset + n + tail];
			long count = supplier.get().collectAndCount(actual, offset);
			assertEquals(n, count);
			assertArrayEquals(expected, actual);
		}

		@Test
		default void testCollectAndCountOfDouble_Null()
		{
			PrimitiveReducible.OfDouble<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.collectAndCount((double[]) null));
			assertThrows(NullPointerException.class, () -> reducible.collectAndCount((double[]) null, 0));
		}

		@ParameterizedTest
		@MethodSource({"getReducibles", "getDuplicatesReducibles"})
		@Override
		default void testCollectDistinct(Supplier<T> supplier)
		{
			Set<Double> expected = new HashSet<>();
			supplier.get().forEach((double d) -> {
				if (!expected.contains(d) && !(d == 0.0 && expected.contains(-1.0 * d))) {
					expected.add(d);
				}
			});
			List<Double> actual = supplier.get().collectDistinct().collect(new ArrayList<>());
			assertTrue(expected.containsAll(actual), "actual =< expected");
			assertTrue(actual.containsAll(expected), "actual >= expected");

		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFilterDouble(Supplier<T> supplier)
		{
			ArrayList<Double> expected = new ArrayList<>();
			supplier.get().forEach((double d) -> {
				if (d % 2 == 0) {
					expected.add(d);
				}
			});
			PrimitiveReducible.OfDouble<?> actual = supplier.get().filter((double d) -> d % 2 == 0);
			assertIterableEquals(unboxDouble(expected), unboxDouble(actual.collect(new ArrayList<>())));
		}

		@Test
		default void testFilterOfDouble_Null()
		{
			PrimitiveReducible.OfDouble<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.filter((DoublePredicate) null));
		}

		void testFlatMapDoubleToObj(Supplier<T> supplier);

		void testFlatMapDoubleToDouble(Supplier<T> supplier);

		void testFlatMapDoubleToInt(Supplier<T> supplier);

		void testFlatMapDoubleToLong(Supplier<T> supplier);

		void testFlatMapDoubleToNull(Supplier<T> supplier);

		void testFlatMapOfDouble_Null();

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMapDoubleToObj(Supplier<T> supplier)
		{
			String prefix = "Item: ";
			List<String> expected = new ArrayList<>();
			supplier.get().forEach((double d) -> expected.add(prefix + d));
			Reducible<String, ?> actual = supplier.get().map((double d) -> prefix + d);
			assertIterableEquals(expected, actual.collect(new ArrayList<>()));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMapDoubleToDouble(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterable.OfDouble expected = unboxDouble(range.map((int i) -> (double) i));
			Range.RangeIterator index = range.iterator();
			PrimitiveReducible.OfDouble<?> actual = supplier.get().mapToDouble((double d) -> index.next());
			assertIterableEquals(expected, unboxDouble(actual.collect(new ArrayList<>())));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMapDoubleToInt(Supplier<T> supplier)
		{
			Range expected = new Range((int) supplier.get().count());
			Range.RangeIterator index = expected.iterator();
			PrimitiveReducible.OfInt<?> actual = supplier.get().mapToInt((double d) -> index.next());
			assertIterableEquals(expected, unboxInt(actual.collect(new ArrayList<>())));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMapDoubleToLong(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterable.OfLong expected = unboxLong(range.map((int i) -> (long) i));
			Range.RangeIterator index = range.iterator();
			PrimitiveReducible.OfLong<?> actual = supplier.get().mapToLong((double d) -> index.next());
			assertIterableEquals(expected, unboxLong(actual.collect(new ArrayList<>())));
		}

		@Test
		default void testMapOfDouble_Null()
		{
			PrimitiveReducible.OfDouble<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.map((DoubleFunction<?>) null));
			assertThrows(NullPointerException.class, () -> reducible.mapToDouble((DoubleUnaryOperator) null));
			assertThrows(NullPointerException.class, () -> reducible.mapToInt((DoubleToIntFunction) null));
			assertThrows(NullPointerException.class, () -> reducible.mapToLong((DoubleToLongFunction) null));
		}

		void testForEachDoubleConsumer(Supplier<T> supplier);

		@Test
		default void testForEachDoubleConsumer_Null()
		{
			PrimitiveReducible.OfDouble<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.forEach((DoubleConsumer) null));
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testReduceDoubleBinaryOperator_Empty(Supplier<T> supplier)
		{
			DoubleBinaryOperator dummy = (res, each) -> Double.MIN_VALUE;
			OptionalDouble actual = supplier.get().reduce(dummy);
			assertEquals(OptionalDouble.empty(), actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonReducibles")
		default void testReduceDoubleBinaryOperator_Singleton(Supplier<T> supplier)
		{
			DoubleBinaryOperator dummy = (res, each) -> Double.MIN_VALUE;
			double expected = supplier.get().detect((double d) -> true);
			OptionalDouble actual = supplier.get().reduce(dummy);
			assertTrue(actual.isPresent());
			assertEquals(expected, actual.getAsDouble());
		}

		@ParameterizedTest
		@MethodSource("getMultitonReducibles")
		default void testReduceDoubleBinaryOperator_Multiton(Supplier<T> supplier)
		{
			List<Double> expected = supplier.get().collect(new ArrayList<>());
			List<Double> actual = new ArrayList<>();
			double probe = -31; // "unique" value
			DoubleBinaryOperator collect = (res, each) -> {
				if (actual.isEmpty()) {
					actual.add(res);
					actual.add(each);
					return probe;
				} else {
					actual.add(each);
					return res;
				}
			};
			OptionalDouble result = supplier.get().reduce(collect);
			assertTrue(result.isPresent());
			assertEquals(probe, result.getAsDouble());
			assertIterableEquals(unboxDouble(expected), unboxDouble(actual));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testReduceIntDoubleToIntFunction(Supplier<T> supplier)
		{
			List<Double> expected = supplier.get().collect(new ArrayList<>());
			int init = Integer.MIN_VALUE;
			List<Double> actual = new ArrayList<>();
			IntDoubleToIntFunction collect = (res, each) -> {actual.add(each); return res;};
			int result = supplier.get().reduce(init, collect);
			assertEquals(init, result);
			assertIterableEquals(unboxDouble(expected), unboxDouble(actual));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testReduceLongDoubleToLongFunction(Supplier<T> supplier)
		{
			List<Double> expected = supplier.get().collect(new ArrayList<>());
			long init = Long.MIN_VALUE;
			List<Double> actual = new ArrayList<>();
			LongDoubleToLongFunction collect = (res, each) -> {actual.add(each); return res;};
			long result = supplier.get().reduce(init, collect);
			assertEquals(init, result);
			assertIterableEquals(unboxDouble(expected), unboxDouble(actual));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testReduceObjDouble(Supplier<T> supplier)
		{
			List<Double> expected = supplier.get().collect(new ArrayList<>());
			ObjDoubleFunction<List<Double>, List<Double>> collect = (seq, each) -> {seq.add(each); return seq;};
			List<Double> actual = supplier.get().reduce(new ArrayList<>(), collect);
			assertIterableEquals(unboxDouble(expected), unboxDouble(actual));
			assertDoesNotThrow(() -> supplier.get().reduce(null, (Object obj, double each) -> null));
		}

		@Test
		default void testReduceOfDouble_Null()
		{
			PrimitiveReducible.OfDouble<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.reduce((DoubleBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> reducible.reduce(new Object(), (ObjDoubleFunction<Object, Object>) null));
			assertThrows(NullPointerException.class, () -> reducible.reduce(0.0, (DoubleBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> reducible.reduce(0, (IntDoubleToIntFunction) null));
			assertThrows(NullPointerException.class, () -> reducible.reduce(0L, (LongDoubleToLongFunction) null));
		}

		void testConcatTypes();

		@ParameterizedTest
		@MethodSource({"getReducibles", "getDuplicatesReducibles"})
		@Override
		default void testDistinct(Supplier<T> supplier)
		{
			List<Double> expected = new ArrayList<>();
			supplier.get().forEach((double d) -> {
				if (!expected.contains(d) && !(d == 0.0 && expected.contains(-1.0 * d))) {
					expected.add(d);
				}
			});
			List<Double> actual = supplier.get().distinct().collect(new ArrayList<>());
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource({"getReducibles", "getDuplicatesReducibles"})
		default void testDedupe(Supplier<T> supplier)
		{
			List<Double> expected = new ArrayList<>();
			supplier.get().forEach((double d) -> {
				if (expected.isEmpty()) {
					expected.add(d);
				} else {
					double last = expected.get(expected.size() - 1);
					if (last != d && !(Double.isNaN(last) && Double.isNaN(d))) {
						expected.add(d);
					}
				}
			});
			List<Double> actual = supplier.get().dedupe().collect(new ArrayList<>());
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource({"getSingletonReducibles", "getMultitonReducibles"})
		default void testAllMatchDoublePredicate(Supplier<T> supplier)
		{
			// match all elements
			assertTrue(supplier.get().allMatch((double each) -> true), "Expected allMatch() == true");
			// match not all elements
			DoublePredicate matchNotAll = new DoublePredicate() {
				// match: no element if singleton, otherwise every odd element
				boolean flag = supplier.get().count() == 1;
				@Override
				public boolean test(double d)
				{
					flag = !flag;
					return flag;
				}
			};
			assertFalse(supplier.get().allMatch(matchNotAll), "Expected allMatch() == false");
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testAllMatchDoublePredicate_Empty(Supplier<T> supplier)
		{
			PrimitiveReducible.OfDouble<?> reducible = supplier.get();
			assertTrue(reducible.allMatch((double each) -> false), "Exepted allMatch() == true if iterator is empty");
		}

		@Test
		default void testAllMatchDoublePredicate_Null()
		{
			PrimitiveReducible.OfDouble<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.allMatch((DoublePredicate) null));
		}

		@ParameterizedTest
		@MethodSource({"getSingletonReducibles", "getMultitonReducibles"})
		default void testAnyMatchDoublePredicate(Supplier<T> supplier)
		{
			// match no element
			assertFalse(supplier.get().anyMatch((double each) -> false), "Expected anyMatch() == false");
			// match some elements
			DoublePredicate matchSome = new DoublePredicate() {
				// match: first element if singleton, otherwise every even element
				boolean flag = supplier.get().count() > 1;
				@Override
				public boolean test(double d)
				{
					flag = !flag;
					return flag;
				}
			};
			assertTrue(supplier.get().anyMatch(matchSome), "Expected anyMatch() == true");
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testAnyMatchDoublePredicate_Empty(Supplier<T> supplier)
		{
			PrimitiveReducible.OfDouble<?> reducible = supplier.get();
			assertTrue(reducible.allMatch((double each) -> true), "Exepted anyMatch() == false if iterator is empty");
		}

		@Test
		default void testAnyMatchDoublePredicate_Null()
		{
			PrimitiveReducible.OfDouble<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.anyMatch((DoublePredicate) null));
		}

		@ParameterizedTest
		@MethodSource({"getSingletonReducibles", "getMultitonReducibles"})
		default void testNoneMatchDoublePredicate(Supplier<T> supplier)
		{
			// match no element
			assertTrue(supplier.get().noneMatch((double each) -> false), "Expected noneMatch() == true");
			// match some elements
			DoublePredicate matchSome = new DoublePredicate() {
				// match: first element if singleton, otherwise every even element
				boolean flag = supplier.get().count() > 1;
				@Override
				public boolean test(double d)
				{
					flag = !flag;
					return flag;
				}
			};
			assertFalse(supplier.get().noneMatch(matchSome), "Expected noneMatch() == false");
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testNoneMatchDoublePredicate_Empty(Supplier<T> supplier)
		{
			PrimitiveReducible.OfDouble<?> reducible = supplier.get();
			assertTrue(reducible.allMatch((double each) -> false), "Exepted noneMatch() == false if iterator is empty");
		}

		@Test
		default void testNoneMatchDoublePredicate_Null()
		{
			PrimitiveReducible.OfDouble<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.noneMatch((DoublePredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		@Override
		default void testContains(Supplier<T> supplier)
		{
			assertFalse(supplier.get().contains(null));
			testContainsDouble(supplier);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testContainsDouble(Supplier<T> supplier)
		{
			List<Double> numbers = supplier.get().collect(new ArrayList<>());
			for (Double each : numbers) { // boxed double to trigger contains(Double d)
				assertTrue(supplier.get().contains(each), "Expected contains(" + each + ") == true");
				if (each == 0.0) {
					assertTrue(supplier.get().contains(-1.0 * each), "Expected contains(" + (-1.0 * each) + ") == true");
				}
			}
			for (Object each : getExcluded(supplier.get())) { // boxed double to trigger contains(Double d)
				assertFalse(supplier.get().contains(each), "Expected contains(" + each + ") == false");
			}
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testCountDoublePredicate(Supplier<T> supplier)
		{
			long[] expected = new long[] {0L};
			supplier.get().forEach((double d) -> {
				if (d % 2 == 1) {
					expected[0]++;
				}
			});
			DoublePredicate odd = d -> d % 2 == 1;
			long actual = supplier.get().count(odd);
			assertEquals(expected[0], actual);
		}

		@Test
		default void testCountDoublePredicate_Null()
		{
			PrimitiveReducible.OfDouble<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.count((DoublePredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testDetectDoublePredicate_AllFalse(Supplier<T> supplier)
		{
			PrimitiveReducible.OfDouble<?> reducible = supplier.get();
			assertThrows(NoSuchElementException.class, () -> reducible.detect((double each) -> false));
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testDetectDoublePredicate_Empty(Supplier<T> supplier)
		{
			PrimitiveReducible.OfDouble<?> reducible = supplier.get();
			assertThrows(NoSuchElementException.class, () -> reducible.detect((double each) -> true));
		}

		@ParameterizedTest
		@MethodSource("getSingletonReducibles")
		default void testDetectDoublePredicate_Singleton(Supplier<T> supplier)
		{
			// match first element
			double expected = supplier.get().collect(new ArrayList<>()).get(0);
			double actual = supplier.get().detect((double each) -> true);
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getMultitonReducibles")
		default void testDetectDoublePredicate_Multiton(Supplier<T> supplier)
		{
			// match second element
			double expected = supplier.get().collect(new ArrayList<>()).get(1);
			DoublePredicate second = new DoublePredicate() {
				boolean flag = true;
				@Override
				public boolean test(double d)
				{
					flag = !flag;
					return flag;
				}
			};
			double actual = supplier.get().detect(second);
			assertEquals(expected, actual);
		}

		@Test
		default void testDetectDoublePredicate_Null()
		{
			PrimitiveReducible.OfDouble<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.detect((DoublePredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testMin_Empty(Supplier<T> supplier)
		{
			OptionalDouble expected = OptionalDouble.empty();
			OptionalDouble actual = supplier.get().min();
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonReducibles")
		default void testMin_Singleton(Supplier<T> supplier)
		{
			OptionalDouble expected = OptionalDouble.of(supplier.get().detect((double d) -> true));
			OptionalDouble actual = supplier.get().min();
			assertTrue(actual.isPresent());
			assertDoubleEquals(expected.getAsDouble(), actual.getAsDouble());
		}

		@ParameterizedTest
		@MethodSource("getMultitonReducibles")
		default void testMin_Multiton(Supplier<T> supplier)
		{
			double[] expected = new double[] {supplier.get().detect((double d) -> true)};
			supplier.get().forEach((double d) -> expected[0] = Math.min(expected[0], d));
			OptionalDouble actual = supplier.get().min();
			assertTrue(actual.isPresent());
			assertDoubleEquals(expected[0], actual.getAsDouble());
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testMax_Empty(Supplier<T> supplier)
		{
			OptionalDouble expected = OptionalDouble.empty();
			OptionalDouble actual = supplier.get().max();
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonReducibles")
		default void testMax_Singleton(Supplier<T> supplier)
		{
			OptionalDouble expected = OptionalDouble.of(supplier.get().detect((double d) -> true));
			OptionalDouble actual = supplier.get().max();
			assertTrue(actual.isPresent());
			assertDoubleEquals(expected.getAsDouble(), actual.getAsDouble());
		}

		@ParameterizedTest
		@MethodSource("getMultitonReducibles")
		default void testMax_Multiton(Supplier<T> supplier)
		{
			double[] expected = new double[] {supplier.get().detect((double d) -> true)};
			supplier.get().forEach((double d) -> expected[0] = Math.max(expected[0], d));
			OptionalDouble actual = supplier.get().max();
			assertTrue(actual.isPresent());
			assertDoubleEquals(expected[0], actual.getAsDouble());
		}

		@ParameterizedTest
		@MethodSource({"getReducibles"})
		default void testSum(Supplier<T> supplier)
		{
			double[] expected = new double[] {0.0};
			supplier.get().forEach((double d) -> expected[0] += d);
			double actual = supplier.get().sum();
			assertEquals(expected[0], actual, 1e-15);
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testSum_Empty(Supplier<T> supplier)
		{
			double expected = 0.0;
			double actual = supplier.get().sum();
			assertDoubleEquals(expected, actual);
		}
	}



	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	interface OfInt<T extends PrimitiveReducible.OfInt<?>> extends ReducibleTest<Integer, T>
	{
		@Override
		default Stream<Supplier<T>> getDuplicatesReducibles()
		{
			return getDuplicatesArraysOfInt().map(args-> () -> getReducible(args));
		}

		@Override
		default Stream<Supplier<T>> getEmptyReducibles()
		{
			return getEmptyArraysOfInt().map(args-> () -> getReducible(args));
		}

		@Override
		default Stream<Supplier<T>> getSingletonReducibles()
		{
			return getSingletonArraysOfInt().map(args-> () -> getReducible(args));
		}

		@Override
		default Stream<Supplier<T>> getMultitonReducibles()
		{
			return getMultitonArraysOfInt().map(args-> () -> getReducible(args));
		}

		T getReducible(int[] arguments);

		@Override
		default Iterable<Object> getExcluded(T reducible)
		{
			List<Integer> candidates = getExclusionListOfInt();
			reducible.forEach((Consumer<Integer>) candidates::remove);
			List<Object> excluded = new ArrayList<>();
			excluded.addAll(getUniqueObjects());
			excluded.addAll(candidates);
			return excluded;
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testCollectIntArray(Supplier<T> supplier)
		{
			int n = (int) supplier.get().count();
			int[] expected = new int[n];
			Range.RangeIterator index = new Range(n).iterator();
			supplier.get().forEach((int e) -> expected[index.nextInt()] = e);
			int[] actual = supplier.get().collect(new int[n]);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testCollectIntArrayOffset(Supplier<T> supplier)
		{
			int n = (int) supplier.get().count(), offset = 2, tail = 3;
			int[] expected = new int[offset + n + tail];
			Range.RangeIterator index = new Range(offset, offset + n).iterator();
			supplier.get().forEach((int i) -> expected[index.nextInt()] = i);
			int[] actual = supplier.get().collect(new int[offset + n + tail], offset);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testCollectBitSet(Supplier<T> supplier)
		{
			BitSet expected = new BitSet();
			supplier.get().forEach((int i) -> {if (i >= 0) expected.set(i);});
			BitSet actual = supplier.get().filter((int i) -> i >= 0).collect(new BitSet());
			assertEquals(expected, actual);
		}

		@Test
		default void testCollectOfInt_Null()
		{
			PrimitiveReducible.OfInt<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.collect((int[]) null));
			assertThrows(NullPointerException.class, () -> reducible.collect((int[]) null, 0));
			assertThrows(NullPointerException.class, () -> reducible.collect((BitSet) null));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testCollectAndCountIntArray(Supplier<T> supplier)
		{
			int n = (int) supplier.get().count(), tail = 3;
			int[] expected = new int[n + tail];
			Range.RangeIterator index = new Range(n).iterator();
			supplier.get().forEach((int i) -> expected[index.nextInt()] = i);
			int[] actual = new int[n + tail];
			long count = supplier.get().collectAndCount(actual);
			assertEquals(n, count);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testCollectAndCountIntArrayOffset(Supplier<T> supplier)
		{
			int n = (int) supplier.get().count(), offset = 2, tail = 3;
			int[] expected = new int[offset + n + tail];
			Range.RangeIterator index = new Range(offset, offset + n).iterator();
			supplier.get().forEach((int i) -> expected[index.nextInt()] = i);
			int[] actual = new int[offset + n + tail];
			long count = supplier.get().collectAndCount(actual, offset);
			assertEquals(n, count);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testCollectAndCountBitSet(Supplier<T> supplier)
		{
			int n = (int) supplier.get().filter((int i) -> i >= 0).count();
			BitSet expected = new BitSet();
			supplier.get().forEach((int i) -> {if (i >= 0) expected.set(i);});
			BitSet actual = new BitSet();
			long count = supplier.get().filter((int i) -> i >= 0).collectAndCount(actual);
			assertEquals(n, count);
			assertEquals(expected, actual);
		}

		@Test
		default void testCollectAndCountOfInt_Null()
		{
			PrimitiveReducible.OfInt<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.collectAndCount((int[]) null));
			assertThrows(NullPointerException.class, () -> reducible.collectAndCount((int[]) null, 0));
			assertThrows(NullPointerException.class, () -> reducible.collectAndCount((BitSet) null));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFilterInt(Supplier<T> supplier)
		{
			ArrayList<Integer> expected = new ArrayList<>();
			supplier.get().forEach((int i) -> {
				if (i % 2 == 0) {
					expected.add(i);
				}
			});
			PrimitiveReducible.OfInt<?> actual = supplier.get().filter((int i) -> i % 2 == 0);
			assertIterableEquals(unboxInt(expected), unboxInt(actual.collect(new ArrayList<>())));
		}

		@Test
		default void testFilterOfInt_Null()
		{
			PrimitiveReducible.OfInt<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.filter((IntPredicate) null));
		}

		void testFlatMapIntToObj(Supplier<T> supplier);

		void testFlatMapIntToDouble(Supplier<T> supplier);

		void testFlatMapIntToInt(Supplier<T> supplier);

		void testFlatMapIntToLong(Supplier<T> supplier);

		void testFlatMapIntToNull(Supplier<T> supplier);

		void testFlatMapOfInt_Null();

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMapIntToObj(Supplier<T> supplier)
		{
			String prefix = "Item: ";
			List<String> expected = new ArrayList<>();
			supplier.get().forEach((int i) -> expected.add(prefix + i));
			Reducible<String, ?> actual = supplier.get().map((int i) -> prefix + i);
			assertIterableEquals(expected, actual.collect(new ArrayList<>()));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMapIntToDouble(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterable.OfDouble expected = unboxDouble(range.map((int i) -> (double) i));
			Range.RangeIterator index = range.iterator();
			PrimitiveReducible.OfDouble<?> actual = supplier.get().mapToDouble((int i) -> index.next());
			assertIterableEquals(expected, unboxDouble(actual.collect(new ArrayList<>())));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMapIntToInt(Supplier<T> supplier)
		{
			Range expected = new Range((int) supplier.get().count());
			Range.RangeIterator index = expected.iterator();
			PrimitiveReducible.OfInt<?> actual = supplier.get().mapToInt((int i) -> index.next());
			assertIterableEquals(expected, unboxInt(actual.collect(new ArrayList<>())));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMapToLong(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterable.OfLong expected = unboxLong(range.map((int i) -> (long) i));
			Range.RangeIterator index = range.iterator();
			PrimitiveReducible.OfLong<?> actual = supplier.get().mapToLong((int i) -> index.next());
			assertIterableEquals(expected, unboxLong(actual.collect(new ArrayList<>())));
		}

		@Test
		default void testMapOfInt_Null()
		{
			PrimitiveReducible.OfInt<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.map((IntFunction<?>) null));
			assertThrows(NullPointerException.class, () -> reducible.mapToDouble((IntToDoubleFunction) null));
			assertThrows(NullPointerException.class, () -> reducible.mapToInt((IntUnaryOperator) null));
			assertThrows(NullPointerException.class, () -> reducible.mapToLong((IntToLongFunction) null));
		}

		void testForEachIntConsumer(Supplier<T> supplier);

		@Test
		default void testForEachIntConsumer_Null()
		{
			PrimitiveReducible.OfInt<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.forEach((IntConsumer) null));
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testReduceIntBinaryOperator_Empty(Supplier<T> supplier)
		{
			IntBinaryOperator dummy = (res, each) -> Integer.MIN_VALUE;
			OptionalInt actual = supplier.get().reduce(dummy);
			assertEquals(OptionalInt.empty(), actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonReducibles")
		default void testReduceIntBinaryOperator_Singleton(Supplier<T> supplier)
		{
			IntBinaryOperator dummy = (res, each) -> Integer.MIN_VALUE;
			int expected = supplier.get().detect((int i) -> true);
			OptionalInt actual = supplier.get().reduce(dummy);
			assertTrue(actual.isPresent());
			assertEquals(expected, actual.getAsInt());
		}

		@ParameterizedTest
		@MethodSource("getMultitonReducibles")
		default void testReduceIntBinaryOperator_Multiton(Supplier<T> supplier)
		{
			List<Integer> expected = supplier.get().collect(new ArrayList<>());
			List<Integer> actual = new ArrayList<>();
			int probe = -31; // "unique" value
			IntBinaryOperator collect = (res, each) -> {
				if (actual.isEmpty()) {
					actual.add(res);
					actual.add(each);
					return probe;
				} else {
					actual.add(each);
					return res;
				}
			};
			OptionalInt result = supplier.get().reduce(collect);
			assertTrue(result.isPresent());
			assertEquals(probe, result.getAsInt());
			assertIterableEquals(unboxInt(expected), unboxInt(actual));
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testReduceDoubleBinaryOperator_Empty(Supplier<T> supplier)
		{
			DoubleBinaryOperator dummy = (res, each) -> Double.MIN_VALUE;
			OptionalDouble actual = supplier.get().reduce(dummy);
			assertEquals(OptionalDouble.empty(), actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonReducibles")
		default void testReduceDoubleBinaryOperator_Singleton(Supplier<T> supplier)
		{
			DoubleBinaryOperator dummy = (res, each) -> Double.MIN_VALUE;
			double expected = supplier.get().detect((int i) -> true);
			OptionalDouble actual = supplier.get().reduce(dummy);
			assertTrue(actual.isPresent());
			assertEquals(expected, actual.getAsDouble());
		}

		@ParameterizedTest
		@MethodSource("getMultitonReducibles")
		default void testReduceDoubleBinaryOperator_Multiton(Supplier<T> supplier)
		{
			List<Double> expected = supplier.get().mapToDouble((int i) -> (double) i).collect(new ArrayList<>());
			List<Double> actual = new ArrayList<>();
			double probe = -31; // "unique" value
			DoubleBinaryOperator collect = (res, each) -> {
				if (actual.isEmpty()) {
					actual.add(res);
					actual.add(each);
					return probe;
				} else {
					actual.add(each);
					return res;
				}
			};
			OptionalDouble result = supplier.get().reduce(collect);
			assertTrue(result.isPresent());
			assertEquals(probe, result.getAsDouble());
			assertIterableEquals(unboxDouble(expected), unboxDouble(actual));
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testReduceLongBinaryOperator_Empty(Supplier<T> supplier)
		{
			LongBinaryOperator dummy = (res, each) -> Long.MIN_VALUE;
			OptionalLong actual = supplier.get().reduce(dummy);
			assertEquals(OptionalLong.empty(), actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonReducibles")
		default void testReduceLongBinaryOperator_Singleton(Supplier<T> supplier)
		{
			LongBinaryOperator dummy = (res, each) -> Long.MIN_VALUE;
			long expected = supplier.get().detect((int i) -> true);
			OptionalLong actual = supplier.get().reduce(dummy);
			assertTrue(actual.isPresent());
			assertEquals(expected, actual.getAsLong());
		}

		@ParameterizedTest
		@MethodSource("getMultitonReducibles")
		default void testReduceLongBinaryOperator_Multiton(Supplier<T> supplier)
		{
			List<Long> expected = supplier.get().mapToLong((int i) -> (long) i).collect(new ArrayList<>());
			List<Long> actual = new ArrayList<>();
			long probe = -31; // "unique" value
			LongBinaryOperator collect = (res, each) -> {
				if (actual.isEmpty()) {
					actual.add(res);
					actual.add(each);
					return probe;
				} else {
					actual.add(each);
					return res;
				}
			};
			OptionalLong result = supplier.get().reduce(collect);
			assertTrue(result.isPresent());
			assertEquals(probe, result.getAsLong());
			assertIterableEquals(unboxLong(expected), unboxLong(actual));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testReduceObjIntFunction(Supplier<T> supplier)
		{
			List<Integer> expected = supplier.get().collect(new ArrayList<>());
			ObjIntFunction<List<Integer>, List<Integer>> collect = (seq, each) -> {seq.add(each); return seq;};
			List<Integer> actual = supplier.get().reduce(new ArrayList<>(), collect);
			assertIterableEquals(unboxInt(expected), unboxInt(actual));
			assertDoesNotThrow(() -> supplier.get().reduce(null, (Object obj, int each) -> null));
		}

		@Test
		default void testReduceOfInt_Null()
		{
			PrimitiveReducible.OfInt<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.reduce((DoubleBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> reducible.reduce((IntBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> reducible.reduce((LongBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> reducible.reduce(new Object(), (ObjIntFunction<Object, Object>) null));
			assertThrows(NullPointerException.class, () -> reducible.reduce(0.0, (DoubleBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> reducible.reduce(0, (IntBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> reducible.reduce(0L, (LongBinaryOperator) null));
		}

		void testConcatTypes();

		@ParameterizedTest
		@MethodSource({"getSingletonReducibles", "getMultitonReducibles"})
		default void testAllMatchIntPredicate(Supplier<T> supplier)
		{
			// match all elements
			assertTrue(supplier.get().allMatch((int each) -> true), "Expected allMatch() == true");
			// match not all elements
			IntPredicate matchNotAll = new IntPredicate() {
				// match: no element if singleton, otherwise every odd element
				boolean flag = supplier.get().count() == 1;
				@Override
				public boolean test(int i)
				{
					flag = !flag;
					return flag;
				}
			};
			assertFalse(supplier.get().allMatch(matchNotAll), "Expected allMatch() == false");
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testAllMatchIntPredicate_Empty(Supplier<T> supplier)
		{
			PrimitiveReducible.OfInt<?> reducible = supplier.get();
			assertTrue(reducible.allMatch((int each) -> false), "Exepted allMatch() == true if iterator is empty");
		}

		@Test
		default void testAllMatchIntPredicate_Null()
		{
			PrimitiveReducible.OfInt<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.allMatch((IntPredicate) null));
		}

		@ParameterizedTest
		@MethodSource({"getSingletonReducibles", "getMultitonReducibles"})
		default void testAnyMatchIntPredicate(Supplier<T> supplier)
		{
			// match no element
			assertFalse(supplier.get().anyMatch((int each) -> false), "Expected anyMatch() == false");
			// match some elements
			IntPredicate matchSome = new IntPredicate() {
				// match: first element if singleton, otherwise every even element
				boolean flag = supplier.get().count() > 1;
				@Override
				public boolean test(int i)
				{
					flag = !flag;
					return flag;
				}
			};
			assertTrue(supplier.get().anyMatch(matchSome), "Expected anyMatch() == true");
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testAnyMatchIntPredicate_Empty(Supplier<T> supplier)
		{
			PrimitiveReducible.OfInt<?> reducible = supplier.get();
			assertTrue(reducible.allMatch((int each) -> true), "Exepted anyMatch() == false if iterator is empty");
		}

		@Test
		default void testAnyMatchIntPredicate_Null()
		{
			PrimitiveReducible.OfInt<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.anyMatch((IntPredicate) null));
		}

		@ParameterizedTest
		@MethodSource({"getSingletonReducibles", "getMultitonReducibles"})
		default void testNoneMatchIntPredicate(Supplier<T> supplier)
		{
			// match no element
			assertTrue(supplier.get().noneMatch((int each) -> false), "Expected noneMatch() == true");
			// match some elements
			IntPredicate matchSome = new IntPredicate() {
				// match: first element if singleton, otherwise every even element
				boolean flag = supplier.get().count() > 1;
				@Override
				public boolean test(int i)
				{
					flag = !flag;
					return flag;
				}
			};
			assertFalse(supplier.get().noneMatch(matchSome), "Expected noneMatch() == false");
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testNoneMatchIntPredicate_Empty(Supplier<T> supplier)
		{
			PrimitiveReducible.OfInt<?> reducible = supplier.get();
			assertTrue(reducible.allMatch((int each) -> false), "Exepted noneMatch() == false if iterator is empty");
		}

		@Test
		default void testNoneMatchIntPredicate_Null()
		{
			PrimitiveReducible.OfInt<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.noneMatch((IntPredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		@Override
		default void testContains(Supplier<T> supplier)
		{
			assertFalse(supplier.get().contains(null), "Expected contains(null) == false");
			testContainsInt(supplier);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testContainsInt(Supplier<T> supplier)
		{
			List<Integer> numbers = supplier.get().collect(new ArrayList<>());
			for (Integer each : numbers) { // boxed int to trigger contains(Integer i)
				assertTrue(supplier.get().contains(each), "Expected contains(" + each + ") == true");
			}
			for (Object each : getExcluded(supplier.get())) { // boxed int to trigger contains(Integer i)
				assertFalse(supplier.get().contains(each), "Expected contains(" + each + ") == false");
			}
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testCountIntPredicate(Supplier<T> supplier)
		{
			long[] expected = new long[] {0L};
			supplier.get().forEach((int i) -> {
				if (i % 2 == 1) {
					expected[0]++;
				}
			});
			IntPredicate odd = i -> i % 2 == 1;
			long actual = supplier.get().count(odd);
			assertEquals(expected[0], actual);
		}

		@Test
		default void testCountIntPredicate_Null()
		{
			PrimitiveReducible.OfInt<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.count((IntPredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testDetectIntPredicate(Supplier<T> supplier)
		{
			PrimitiveReducible.OfInt<?> reducible = supplier.get();
			assertThrows(NoSuchElementException.class, () -> reducible.detect((int each) -> false));
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testDetectIntPredicate_Empty(Supplier<T> supplier)
		{
			PrimitiveReducible.OfInt<?> reducible = supplier.get();
			assertThrows(NoSuchElementException.class, () -> reducible.detect((int each) -> true));
		}

		@ParameterizedTest
		@MethodSource("getSingletonReducibles")
		default void testDetectIntPredicate_Singleton(Supplier<T> supplier)
		{
			// match first element
			int expected = supplier.get().collect(new ArrayList<>()).get(0);
			int actual = supplier.get().detect((int each) -> true);
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getMultitonReducibles")
		default void testDetectIntPredicate_Multiton(Supplier<T> supplier)
		{
			// match second element
			int expected = supplier.get().collect(new ArrayList<>()).get(1);
			IntPredicate second = new IntPredicate() {
				boolean flag = true;
				@Override
				public boolean test(int i)
				{
					flag = !flag;
					return flag;
				}
			};
			int actual = supplier.get().detect(second);
			assertEquals(expected, actual);
		}

		@Test
		default void testDetectIntPredicate_Null()
		{
			PrimitiveReducible.OfInt<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.detect((IntPredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testMin_Empty(Supplier<T> supplier)
		{
			OptionalInt expected = OptionalInt.empty();
			OptionalInt actual = supplier.get().min();
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonReducibles")
		default void testMin_Singleton(Supplier<T> supplier)
		{
			OptionalInt expected = OptionalInt.of(supplier.get().detect((int i) -> true));
			OptionalInt actual = supplier.get().min();
			assertTrue(actual.isPresent());
			assertEquals(expected.getAsInt(), actual.getAsInt());
		}

		@ParameterizedTest
		@MethodSource("getMultitonReducibles")
		default void testMin_Multiton(Supplier<T> supplier)
		{
			int[] expected = new int[] {supplier.get().detect((int i) -> true)};
			supplier.get().forEach((int i) -> expected[0] = Math.min(expected[0], i));
			OptionalInt actual = supplier.get().min();
			assertTrue(actual.isPresent());
			assertEquals(expected[0], actual.getAsInt());
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testMax_Empty(Supplier<T> supplier)
		{
			OptionalInt expected = OptionalInt.empty();
			OptionalInt actual = supplier.get().max();
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonReducibles")
		default void testMax_Singleton(Supplier<T> supplier)
		{
			OptionalInt expected = OptionalInt.of(supplier.get().detect((int i) -> true));
			OptionalInt actual = supplier.get().max();
			assertTrue(actual.isPresent());
			assertEquals(expected.getAsInt(), actual.getAsInt());
		}

		@ParameterizedTest
		@MethodSource("getMultitonReducibles")
		default void testMax_Multiton(Supplier<T> supplier)
		{
			int[] expected = new int[] {supplier.get().detect((int i) -> true)};
			supplier.get().forEach((int i) -> expected[0] = Math.max(expected[0], i));
			OptionalInt actual = supplier.get().max();
			assertTrue(actual.isPresent());
			assertEquals(expected[0], actual.getAsInt());
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testSum(Supplier<T> supplier)
		{
			long[] expected = new long[] {0L};
			supplier.get().forEach((int i) -> expected[0] += i);
			long actual = supplier.get().sum();
			assertEquals(expected[0], actual);
		}
	}



	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	interface OfLong<T extends PrimitiveReducible.OfLong<?>> extends ReducibleTest<Long, T>
	{
		@Override
		default Stream<Supplier<T>> getDuplicatesReducibles()
		{
			return getDuplicatesArraysOfLong().map(args-> () -> getReducible(args));
		}

		@Override
		default Stream<Supplier<T>> getEmptyReducibles()
		{
			return getEmptyArraysOfLong().map(args-> () -> getReducible(args));
		}

		@Override
		default Stream<Supplier<T>> getSingletonReducibles()
		{
			return getSingletonArraysOfLong().map(args-> () -> getReducible(args));
		}

		@Override
		default Stream<Supplier<T>> getMultitonReducibles()
		{
			return getMultitonArraysOfLong().map(args-> () -> getReducible(args));
		}

		T getReducible(long[] numbers);

		@Override
		default Iterable<Object> getExcluded(T reducible)
		{
			List<Long> candidates = getExclusionListOfLong();
			reducible.forEach((Consumer<Long>) candidates::remove);
			List<Object> excluded = new ArrayList<>();
			excluded.addAll(getUniqueObjects());
			excluded.addAll(candidates);
			return excluded;
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testCollectLongArray(Supplier<T> supplier)
		{
			int n = (int) supplier.get().count();
			long[] expected = new long[n];
			Range.RangeIterator index = new Range(n).iterator();
			supplier.get().forEach((long l) -> expected[index.nextInt()] = l);
			long[] actual = supplier.get().collect(new long[n]);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testCollectLongArrayOffset(Supplier<T> supplier)
		{
			int n = (int) supplier.get().count(), offset = 2, tail = 3;
			long[] expected = new long[offset + n + tail];
			Range.RangeIterator index = new Range(offset, offset + n).iterator();
			supplier.get().forEach((long l) -> expected[index.nextInt()] = l);
			long[] actual = supplier.get().collect(new long[offset + n + tail], offset);
			assertArrayEquals(expected, actual);
		}

		@Test
		default void testCollectOfLong_Null()
		{
			PrimitiveReducible.OfLong<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.collect((long[]) null));
			assertThrows(NullPointerException.class, () -> reducible.collect((long[]) null, 0));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testCollectAndCountLongArray(Supplier<T> supplier)
		{
			int n = (int) supplier.get().count(), tail = 3;
			long[] expected = new long[n + tail];
			Range.RangeIterator index = new Range(n).iterator();
			supplier.get().forEach((long l) -> expected[index.nextInt()] = l);
			long[] actual = new long[n + tail];
			long count = supplier.get().collectAndCount(actual);
			assertEquals(n, count);
			assertArrayEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testCollectAndCountArrayOffset(Supplier<T> supplier)
		{
			int n = (int) supplier.get().count(), offset = 2, tail = 3;
			long[] expected = new long[offset + n + tail];
			Range.RangeIterator index = new Range(offset, offset + n).iterator();
			supplier.get().forEach((long l) -> expected[index.nextInt()] = l);
			long[] actual = new long[offset + n + tail];
			long count = supplier.get().collectAndCount(actual, offset);
			assertEquals(n, count);
			assertArrayEquals(expected, actual);
		}

		@Test
		default void testCollectAndCountOfLong_Null()
		{
			PrimitiveReducible.OfLong<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.collectAndCount((long[]) null));
			assertThrows(NullPointerException.class, () -> reducible.collectAndCount((long[]) null, 0));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testFilterLong(Supplier<T> supplier)
		{
			ArrayList<Long> expected = new ArrayList<>();
			supplier.get().forEach((long l) -> {
				if (l % 2 == 0) {
					expected.add(l);
				}
			});
			PrimitiveReducible.OfLong<?> actual = supplier.get().filter((long l) -> l % 2 == 0);
			assertIterableEquals(unboxLong(expected), unboxLong(actual.collect(new ArrayList<>())));
		}

		@Test
		default void testFilterOfLong_Null()
		{
			PrimitiveReducible.OfLong<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.filter((LongPredicate) null));
		}

		void testFlatMapLongToObj(Supplier<T> supplier);

		void testFlatMapLongToDouble(Supplier<T> supplier);

		void testFlatMapLongToInt(Supplier<T> supplier);

		void testFlatMapLongToLong(Supplier<T> supplier);

		void testFlatMapLongToNull(Supplier<T> supplier);

		void testFlatMapOfLong_Null();

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMapLongToObj(Supplier<T> supplier)
		{
			String prefix = "Item: ";
			List<String> expected = new ArrayList<>();
			supplier.get().forEach((long l) -> expected.add(prefix + l));
			Reducible<String, ?> actual = supplier.get().map((long l) -> prefix + l);
			assertIterableEquals(expected, actual.collect(new ArrayList<>()));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMapLongToDouble(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterable.OfDouble expected = unboxDouble(range.map((int i) -> (double) i));
			Range.RangeIterator index = range.iterator();
			PrimitiveReducible.OfDouble<?> actual = supplier.get().mapToDouble((long l) -> index.next());
			assertIterableEquals(expected, unboxDouble(actual.collect(new ArrayList<>())));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMapLongToInt(Supplier<T> supplier)
		{
			Range expected = new Range((int) supplier.get().count());
			Range.RangeIterator index = expected.iterator();
			PrimitiveReducible.OfInt<?> actual = supplier.get().mapToInt((long l) -> index.next());
			assertIterableEquals(expected, unboxInt(actual.collect(new ArrayList<>())));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testMapToLong(Supplier<T> supplier)
		{
			Range range = new Range((int) supplier.get().count());
			PrimitiveIterable.OfLong expected = unboxLong(range.map((int i) -> (long) i));
			Range.RangeIterator index = range.iterator();
			PrimitiveReducible.OfLong<?> actual = supplier.get().mapToLong((long l) -> index.next());
			assertIterableEquals(expected, unboxLong(actual.collect(new ArrayList<>())));
		}

		@Test
		default void testMapOfLong_Null()
		{
			PrimitiveReducible.OfLong<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.map((LongFunction<?>) null));
			assertThrows(NullPointerException.class, () -> reducible.mapToDouble((LongToDoubleFunction) null));
			assertThrows(NullPointerException.class, () -> reducible.mapToInt((LongToIntFunction) null));
			assertThrows(NullPointerException.class, () -> reducible.mapToLong((LongUnaryOperator) null));
		}

		void testForEachLongConsumer(Supplier<T> supplier);

		@Test
		default void testForEachLongConsumer_Null()
		{
			PrimitiveReducible.OfLong<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.forEach((LongConsumer) null));
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testReduceLongBinaryOperator_Empty(Supplier<T> supplier)
		{
			LongBinaryOperator dummy = (res, each) -> Long.MIN_VALUE;
			OptionalLong actual = supplier.get().reduce(dummy);
			assertEquals(OptionalLong.empty(), actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonReducibles")
		default void testReduceLongBinaryOperator_Singleton(Supplier<T> supplier)
		{
			LongBinaryOperator dummy = (res, each) -> Long.MIN_VALUE;
			long expected = supplier.get().detect((long l) -> true);
			OptionalLong actual = supplier.get().reduce(dummy);
			assertTrue(actual.isPresent());
			assertEquals(expected, actual.getAsLong());
		}

		@ParameterizedTest
		@MethodSource("getMultitonReducibles")
		default void testReduceLongBinaryOperator_Multiton(Supplier<T> supplier)
		{
			List<Long> expected = supplier.get().collect(new ArrayList<>());
			List<Long> actual = new ArrayList<>();
			long probe = -31; // "unique" value
			LongBinaryOperator collect = (res, each) -> {
				if (actual.isEmpty()) {
					actual.add(res);
					actual.add(each);
					return probe;
				} else {
					actual.add(each);
					return res;
				}
			};
			OptionalLong result = supplier.get().reduce(collect);
			assertTrue(result.isPresent());
			assertEquals(probe, result.getAsLong());
			assertIterableEquals(unboxLong(expected), unboxLong(actual));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testReduceDoubleLongToDoubleFunction(Supplier<T> supplier)
		{
			List<Long> expected = supplier.get().collect(new ArrayList<>());
			double init = Double.MIN_VALUE;
			List<Long> actual = new ArrayList<>();
			DoubleLongToDoubleFunction collect = (res, each) -> {actual.add(each); return res;};
			double result = supplier.get().reduce(init, collect);
			assertEquals(init, result);
			assertIterableEquals(unboxLong(expected), unboxLong(actual));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testReduceIntLongToIntFunction(Supplier<T> supplier)
		{
			List<Long> expected = supplier.get().collect(new ArrayList<>());
			int init = Integer.MIN_VALUE;
			List<Long> actual = new ArrayList<>();
			IntLongToIntFunction collect = (res, each) -> {actual.add(each); return res;};
			int result = supplier.get().reduce(init, collect);
			assertEquals(init, result);
			assertIterableEquals(unboxLong(expected), unboxLong(actual));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testReduceObjLong(Supplier<T> supplier)
		{
			List<Long> expected = supplier.get().collect(new ArrayList<>());
			ObjLongFunction<List<Long>, List<Long>> collect = (seq, each) -> {seq.add(each); return seq;};
			List<Long> actual = supplier.get().reduce(new ArrayList<>(), collect);
			assertIterableEquals(unboxLong(expected), unboxLong(actual));
			assertDoesNotThrow(() -> supplier.get().reduce(null, (Object obj, long each) -> null));
		}

		@Test
		default void testReduceOfLong_Null()
		{
			PrimitiveReducible.OfLong<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.reduce((LongBinaryOperator) null));
			assertThrows(NullPointerException.class, () -> reducible.reduce(new Object(), (ObjLongFunction<Object, Object>) null));
			assertThrows(NullPointerException.class, () -> reducible.reduce(0.0, (DoubleLongToDoubleFunction) null));
			assertThrows(NullPointerException.class, () -> reducible.reduce(0, (IntLongToIntFunction) null));
			assertThrows(NullPointerException.class, () -> reducible.reduce(0L, (LongBinaryOperator) null));
		}

		void testConcatTypes();

		@ParameterizedTest
		@MethodSource({"getSingletonReducibles", "getMultitonReducibles"})
		default void testAllMatchLongPredicate(Supplier<T> supplier)
		{
			// match all elements
			assertTrue(supplier.get().allMatch((long each) -> true), "Expected allMatch() == true");
			// match not all elements
			LongPredicate matchNotAll = new LongPredicate() {
				// match: no element if singleton, otherwise every odd element
				boolean flag = supplier.get().count() == 1;
				@Override
				public boolean test(long i)
				{
					flag = !flag;
					return flag;
				}
			};
			assertFalse(supplier.get().allMatch(matchNotAll), "Expected allMatch() == false");
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testAllMatchLongPredicate_Empty(Supplier<T> supplier)
		{
			PrimitiveReducible.OfLong<?> reducible = supplier.get();
			assertTrue(reducible.allMatch((long each) -> false), "Exepted allMatch() == true if iterator is empty");
		}

		@Test
		default void testAllMatchLongPredicate_Null()
		{
			PrimitiveReducible.OfLong<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.allMatch((LongPredicate) null));
		}

		@ParameterizedTest
		@MethodSource({"getSingletonReducibles", "getMultitonReducibles"})
		default void testAnyMatchLongPredicate(Supplier<T> supplier)
		{
			// match no element
			assertFalse(supplier.get().anyMatch((long each) -> false), "Expected anyMatch() == false");
			// match some elements
			LongPredicate matchSome = new LongPredicate() {
				// match: first element if singleton, otherwise every even element
				boolean flag = supplier.get().count() > 1;
				@Override
				public boolean test(long i)
				{
					flag = !flag;
					return flag;
				}
			};
			assertTrue(supplier.get().anyMatch(matchSome), "Expected anyMatch() == true");
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testAnyMatchLongPredicate_Empty(Supplier<T> supplier)
		{
			PrimitiveReducible.OfLong<?> reducible = supplier.get();
			assertTrue(reducible.allMatch((long each) -> true), "Exepted anyMatch() == false if iterator is empty");
		}

		@Test
		default void testAnyMatchLongPredicate_Null()
		{
			PrimitiveReducible.OfLong<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.anyMatch((LongPredicate) null));
		}

		@ParameterizedTest
		@MethodSource({"getSingletonReducibles", "getMultitonReducibles"})
		default void testNoneMatchLongPredicate(Supplier<T> supplier)
		{
			// match no element
			assertTrue(supplier.get().noneMatch((long each) -> false), "Expected noneMatch() == true");
			// match some elements
			LongPredicate matchSome = new LongPredicate() {
				// match: first element if singleton, otherwise every even element
				boolean flag = supplier.get().count() > 1;
				@Override
				public boolean test(long i)
				{
					flag = !flag;
					return flag;
				}
			};
			assertFalse(supplier.get().noneMatch(matchSome), "Expected noneMatch() == false");
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testNoneMatchLongPredicate_Empty(Supplier<T> supplier)
		{
			PrimitiveReducible.OfLong<?> reducible = supplier.get();
			assertTrue(reducible.allMatch((long each) -> false), "Exepted noneMatch() == false if iterator is empty");
		}

		@Test
		default void testNoneMatchLongPredicate_Null()
		{
			PrimitiveReducible.OfLong<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.noneMatch((LongPredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		@Override
		default void testContains(Supplier<T> supplier)
		{
			assertFalse(supplier.get().contains(null), "Expected contains(null) == false");
			testContainsLong(supplier);
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testContainsLong(Supplier<T> supplier)
		{
			List<Long> numbers = supplier.get().collect(new ArrayList<>());
			for (Long each : numbers) { // boxed long to trigger contains(Long l)
				assertTrue(supplier.get().contains(each), "Expected contains(" + each + ") == true");
			}
			for (Object each : getExcluded(supplier.get())) { // boxed long to trigger contains(Long l)
				assertFalse(supplier.get().contains(each), "Expected contains(" + each + ") == false");
			}
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testCountLongPredicate(Supplier<T> supplier)
		{
			long[] expected = new long[] {0L};
			supplier.get().forEach((long l) -> {
				if (l % 2 == 1) {
					expected[0]++;
				}
			});
			LongPredicate odd = l -> l % 2 == 1;
			long actual = supplier.get().count(odd);
			assertEquals(expected[0], actual);
		}

		@Test
		default void testCountLongPredicate_Null()
		{
			PrimitiveReducible.OfLong<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.count((LongPredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testDetectLongPredicate(Supplier<T> supplier)
		{
			PrimitiveReducible.OfLong<?> reducible = supplier.get();
			assertThrows(NoSuchElementException.class, () -> reducible.detect((long each) -> false));
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testDetectLongPredicate_Empty(Supplier<T> supplier)
		{
			PrimitiveReducible.OfLong<?> reducible = supplier.get();
			assertThrows(NoSuchElementException.class, () -> reducible.detect((long each) -> true));
		}

		@ParameterizedTest
		@MethodSource("getSingletonReducibles")
		default void testDetectLongPredicate_Singleton(Supplier<T> supplier)
		{
			// match first element
			long expected = supplier.get().collect(new ArrayList<>()).get(0);
			long actual = supplier.get().detect((long each) -> true);
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getMultitonReducibles")
		default void testDetectLongPredicate_Multiton(Supplier<T> supplier)
		{
			// match second element
			long expected = supplier.get().collect(new ArrayList<>()).get(1);
			LongPredicate second = new LongPredicate() {
				boolean flag = true;
				@Override
				public boolean test(long l)
				{
					flag = !flag;
					return flag;
				}
			};
			long actual = supplier.get().detect(second);
			assertEquals(expected, actual);
		}

		@Test
		default void testDetectLongPredicate_Null()
		{
			PrimitiveReducible.OfLong<?> reducible = getAnyReducible();
			assertThrows(NullPointerException.class, () -> reducible.detect((LongPredicate) null));
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testMin_Empty(Supplier<T> supplier)
		{
			OptionalLong expected = OptionalLong.empty();
			OptionalLong actual = supplier.get().min();
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonReducibles")
		default void testMin_Singleton(Supplier<T> supplier)
		{
			OptionalLong expected = OptionalLong.of(supplier.get().detect((long l) -> true));
			OptionalLong actual = supplier.get().min();
			assertTrue(actual.isPresent());
			assertEquals(expected.getAsLong(), actual.getAsLong());
		}

		@ParameterizedTest
		@MethodSource("getMultitonReducibles")
		default void testMin_Multiton(Supplier<T> supplier)
		{
			long[] expected = new long[] {supplier.get().detect((long l) -> true)};
			supplier.get().forEach((long l) -> expected[0] = Math.min(expected[0], l));
			OptionalLong actual = supplier.get().min();
			assertTrue(actual.isPresent());
			assertEquals(expected[0], actual.getAsLong());
		}

		@ParameterizedTest
		@MethodSource("getEmptyReducibles")
		default void testMax_Empty(Supplier<T> supplier)
		{
			OptionalLong expected = OptionalLong.empty();
			OptionalLong actual = supplier.get().max();
			assertEquals(expected, actual);
		}

		@ParameterizedTest
		@MethodSource("getSingletonReducibles")
		default void testMax_Singleton(Supplier<T> supplier)
		{
			OptionalLong expected = OptionalLong.of(supplier.get().detect((long l) -> true));
			OptionalLong actual = supplier.get().max();
			assertTrue(actual.isPresent());
			assertEquals(expected.getAsLong(), actual.getAsLong());
		}

		@ParameterizedTest
		@MethodSource("getMultitonReducibles")
		default void testMax_Multiton(Supplier<T> supplier)
		{
			long[] expected = new long[] {supplier.get().detect((long l) -> true)};
			supplier.get().forEach((long l) -> expected[0] = Math.max(expected[0], l));
			OptionalLong actual = supplier.get().max();
			assertTrue(actual.isPresent());
			assertEquals(expected[0], actual.getAsLong());
		}

		@ParameterizedTest
		@MethodSource("getReducibles")
		default void testSum(Supplier<T> supplier)
		{
			long[] expected = new long[] {0L};
			supplier.get().forEach((long l) -> expected[0] += l);
			long actual = supplier.get().sum();
			assertEquals(expected[0], actual);
		}
	}
}
