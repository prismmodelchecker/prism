package common.iterable;

import common.functions.DoubleObjToDoubleFunction;
import common.functions.IntObjToIntFunction;
import common.functions.LongObjToLongFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public interface ReducibleTest<E, T extends Reducible<E, ?>>
{
	default T getAnyReducible()
	{
		Optional<Supplier<T>> any = getReducibles().findAny();
		assert any.isPresent();
		return any.get().get();
	}

	default Stream<Supplier<T>> getReducibles()
	{
		return Stream.concat(Stream.concat(getEmptyReducibles(), getSingletonReducibles()), getMultitonReducibles());
	}

	Stream<Supplier<T>> getDuplicatesReducibles();

	Stream<Supplier<T>> getEmptyReducibles();

	Stream<Supplier<T>> getSingletonReducibles();

	Stream<Supplier<T>> getMultitonReducibles();

	Iterable<Object> getExcluded(T reducible);



	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testCollectArray(Supplier<T> supplier)
	{
		int n = (int) supplier.get().count();
		Object[] expected = new Object[n];
		Range.RangeIterator index = new Range(n).iterator();
		supplier.get().forEach(e -> expected[index.nextInt()] = e);
		Object[] actual = supplier.get().collect((E[]) new Object[n]);  // exploit that E[] is Object[] at runtime
		assertArrayEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testCollectArrayOffset(Supplier<T> supplier)
	{
		int n = (int) supplier.get().count(), offset = 2, tail = 3;
		Object[] expected = new Object[offset + n + tail];
		Range.RangeIterator index = new Range(offset, offset + n).iterator();
		supplier.get().forEach(e -> expected[index.nextInt()] = e);
		Object[] actual = supplier.get().collect((E[]) new Object[offset + n + tail], offset);  // exploit that E[] is Object[] at runtime
		assertArrayEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testCollectCollection(Supplier<T> supplier)
	{
		List<E> expected = new ArrayList<>();
		supplier.get().forEach(expected::add);
		List<E> actual = supplier.get().collect(new ArrayList<>());
		assertIterableEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testCollectSupplier(Supplier<T> supplier)
	{
		List<E> expected = new ArrayList<>();
		supplier.get().forEach(expected::add);
		List<E> actual = supplier.get().collect((Supplier<? extends List<E>>) ArrayList::new);
		assertIterableEquals(expected, actual);
	}

	@Test
	default void testCollect_Null()
	{
		Reducible<E, ?> reducible = getAnyReducible();
		assertThrows(NullPointerException.class, () -> reducible.collect((E[]) null));
		assertThrows(NullPointerException.class, () -> reducible.collect(null, 0));
		assertThrows(NullPointerException.class, () -> reducible.collect((Collection<? super E>) null));
		assertThrows(NullPointerException.class, () -> reducible.collect((Supplier<? extends Collection<? super E>>) null));
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testCollectAndCountArray(Supplier<T> supplier)
	{
		int n = (int) supplier.get().count(), tail = 3;
		Object[] expected = new Object[n + tail];
		Range.RangeIterator index = new Range(n).iterator();
		supplier.get().forEach(e -> expected[index.nextInt()] = e);
		Object[] actual = new Object[n + tail];
		long count = supplier.get().collectAndCount((E[]) actual);  // exploit that E[] is Object[] at runtime
		assertEquals(n, count);
		assertArrayEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testCollectAndCountArrayOffset(Supplier<T> supplier)
	{
		int n = (int) supplier.get().count(), offset = 2, tail = 3;
		Object[] expected = new Object[offset + n + tail];
		Range.RangeIterator index = new Range(offset, offset + n).iterator();
		supplier.get().forEach(e -> expected[index.nextInt()] = e);
		Object[] actual = new Object[offset + n + tail];
		long count = supplier.get().collectAndCount((E[]) actual, offset);  // exploit that E[] is Object[] at runtime
		assertEquals(n, count);
		assertArrayEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testCollectAndCountCollection(Supplier<T> supplier)
	{
		List<E> expected = new ArrayList<>();
		supplier.get().forEach(expected::add);
		List<E> actual = new ArrayList<>();
		long count = supplier.get().collectAndCount(actual);
		assertEquals(expected.size(), count);
		assertIterableEquals(expected, actual);
	}

	@Test
	default void testCollectAndCount_Null()
	{
		Reducible<E, ?> reducible = getAnyReducible();
		assertThrows(NullPointerException.class, () -> reducible.collectAndCount((E[]) null));
		assertThrows(NullPointerException.class, () -> reducible.collectAndCount(null, 0));
		assertThrows(NullPointerException.class, () -> reducible.collectAndCount((Collection<? super E>) null));
	}

	@ParameterizedTest
	@MethodSource({"getReducibles", "getDuplicatesReducibles"})
	default void testCollectDistinct(Supplier<T> supplier)
	{
		Set<E> expected = new HashSet<>();
		supplier.get().forEach(expected::add);
		List<E> actual = supplier.get().collectDistinct().collect(new ArrayList<>());
		assertTrue(expected.containsAll(actual), "actual =< expected");
		assertTrue(actual.containsAll(expected), "actual >= expected");
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testConsume(Supplier<T> supplier)
	{
		// Just test that consume yields the receiver.
		// There is no way to test whether consume does anything beyond this.
		T expected = supplier.get();
		Reducible<E, ?> actual = expected.consume();
		assertSame(expected, actual);
	}

	void testConcat(Supplier<T> supplier);

	@ParameterizedTest
	@MethodSource({"getReducibles", "getDuplicatesReducibles"})
	default void testDistinct(Supplier<T> supplier)
	{
		List<E> expected = new ArrayList<>();
		supplier.get().forEach(e -> {if (! expected.contains(e)) expected.add(e);});
		List<E> actual = supplier.get().distinct().collect(new ArrayList<>());
		assertEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource({"getReducibles", "getDuplicatesReducibles"})
	default void testDedupe(Supplier<T> supplier)
	{
		List<E> expected = new ArrayList<>();
		supplier.get().forEach(e -> {
			Object last = expected.isEmpty() ? new Object() : expected.get(expected.size() - 1);
			if (! Objects.equals(last, e)) {
				expected.add(e);
			}
		});
		List<Object> actual = supplier.get().dedupe().collect(new ArrayList<>());
		assertEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testFilter(Supplier<T> supplier)
	{
		ArrayList<E> expected = new ArrayList<>();
		int n = supplier.get().reduce(0, (int c, E e) -> {
			if (c % 2 == 0) expected.add(e);
			return ++c;
		});
		Range.RangeIterator index = new Range(n).iterator();
		Reducible<E, ?> actual = supplier.get().filter(e -> index.nextInt() % 2 == 0);
		assertIterableEquals(expected, actual.collect(new ArrayList<>()));
	}

	@Test
	default void testFilter_Null()
	{
		Reducible<E, ?> reducible = getAnyReducible();
		assertThrows(NullPointerException.class, () -> reducible.filter(null));
	}

	void testFlatMap(Supplier<T> supplier);

	void testFlatMapToDouble(Supplier<T> supplier);

	void testFlatMapToInt(Supplier<T> supplier);

	void testFlatMapToLong(Supplier<T> supplier);

	void testFlatMapToNull(Supplier<T> supplier);

	void testFlatMap_Null();

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testMap(Supplier<T> supplier)
	{
		String prefix = "Item: ";
		List<String> expected = new ArrayList<>();
		supplier.get().forEach(e -> expected.add(prefix + e));
		Reducible<String, ?> actual = supplier.get().map(e -> prefix + e);
		assertIterableEquals(expected, actual.collect(new ArrayList<>()));
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testMapToDouble(Supplier<T> supplier)
	{
		Range range = new Range((int) supplier.get().count());
		FunctionalIterable<Double> expected = range.map((int i) -> (double) i);
		Range.RangeIterator index = range.iterator();
		PrimitiveReducible.OfDouble<?> actual = supplier.get().mapToDouble(e -> index.next());
		assertIterableEquals(expected, actual.collect(new ArrayList<>()));
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testMapToInt(Supplier<T> supplier)
	{
		Range expected = new Range((int) supplier.get().count());
		Range.RangeIterator index = expected.iterator();
		PrimitiveReducible.OfInt<?> actual = supplier.get().mapToInt(e -> index.next());
		assertIterableEquals(expected, actual.collect(new ArrayList<>()));
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testMapToLong(Supplier<T> supplier)
	{
		Range range = new Range((int) supplier.get().count());
		FunctionalIterable<Long> expected = range.map((int i) -> (long) i);
		Range.RangeIterator index = range.iterator();
		PrimitiveReducible.OfLong<?> actual = supplier.get().mapToLong(e -> index.next());
		assertIterableEquals(expected, actual.collect(new ArrayList<>()));
	}

	@Test
	default void testMap_Null()
	{
		Reducible<E, ?> reducible = getAnyReducible();
		assertThrows(NullPointerException.class, () -> reducible.map(null));
		assertThrows(NullPointerException.class, () -> reducible.mapToDouble(null));
		assertThrows(NullPointerException.class, () -> reducible.mapToInt(null));
		assertThrows(NullPointerException.class, () -> reducible.mapToLong(null));
	}

	void testForEach(Supplier<T> supplier);

	@Test
	default void testForEach_Null()
	{
		Reducible<E, ?> reducible = getAnyReducible();
		assertThrows(NullPointerException.class, () -> reducible.forEach(null));
	}

	@ParameterizedTest
	@MethodSource("getEmptyReducibles")
	default void testIsEmpty_Empty(Supplier<T> supplier)
	{
		assertTrue(supplier.get().isEmpty());
	}

	@ParameterizedTest
	@MethodSource({"getSingletonReducibles", "getMultitonReducibles"})
	default void testIsEmpty_NonEmpty(Supplier<T> supplier)
	{
		assertFalse(supplier.get().isEmpty());
	}

	@ParameterizedTest
	@MethodSource("getEmptyReducibles")
	default void testReduceBinaryOperatorOfE_Empty(Supplier<T> supplier)
	{
		BinaryOperator<E> nop = (res, each) -> null;
		Optional<E> actual = supplier.get().reduce(nop);
		assertEquals(Optional.empty(), actual);
	}

	@ParameterizedTest
	@MethodSource("getSingletonReducibles")
	default void testReduceBinaryOperatorOfE_Singleton(Supplier<T> supplier)
	{
		BinaryOperator<E> nop = (res, each) -> null;
		E expected = supplier.get().detect(e -> true);
		if (expected == null) {
			assertThrows(NullPointerException.class, () -> supplier.get().reduce(nop));
		} else {
			Optional<E> actual = supplier.get().reduce(nop);
			assertTrue(actual.isPresent());
			assertEquals(expected, actual.get());
		}
	}

	@ParameterizedTest
	@MethodSource("getMultitonReducibles")
	default void testReduceBinaryOperatorOfE_Multiton(Supplier<T> supplier)
	{
		ArrayList<E> expected = supplier.get().collect(new ArrayList<>());
		List<E> actual = new ArrayList<>();
		E probe = (E) new Object(); // "unique" value, exploit that E is Object at runtime
		BinaryOperator<E> collect = (res, each) -> {
			if (actual.isEmpty()) {
				actual.add(res);
				actual.add(each);
				return (E) probe;
			} else {
				actual.add(each);
				return res;
			}
		};
		Optional<E> result = supplier.get().reduce(collect);
		assertTrue(result.isPresent());
		assertEquals(probe, result.get());
		assertIterableEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getMultitonReducibles")
	default void testReduceBinary_ResultNull(Supplier<T> supplier)
	{
		Reducible<E, ?> reducible = supplier.get();
		assertThrows(NullPointerException.class, () -> reducible.reduce((res, each) -> null));
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testReduce(Supplier<T> supplier)
	{
		ArrayList<E> expected = supplier.get().collect(new ArrayList<>());
		BiFunction<List<E>, E, List<E>> collect = (seq, each) -> {seq.add(each); return seq;};
		List<E> actual = supplier.get().reduce(new ArrayList<>(), collect);
		assertIterableEquals(expected, actual);
		assertDoesNotThrow(() -> supplier.get().reduce(null, (obj, each) -> null));
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testReduceDouble(Supplier<T> supplier)
	{
		List<E> expected = supplier.get().collect(new ArrayList<>());
		List<E> actual = new ArrayList<>();
		DoubleObjToDoubleFunction<E> collect = (res, each) -> {actual.add(each); return res;};
		double result = supplier.get().reduce(Double.MIN_VALUE, collect);
		assertEquals(Double.MIN_VALUE, result);
		assertIterableEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testReduceInt(Supplier<T> supplier)
	{
		List<E> expected = supplier.get().collect(new ArrayList<>());
		List<E> actual = new ArrayList<>();
		IntObjToIntFunction<E> collect = (res, each) -> {actual.add(each); return res;};
		int result = supplier.get().reduce(Integer.MIN_VALUE, collect);
		assertEquals(Integer.MIN_VALUE, result);
		assertIterableEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testReduceLong(Supplier<T> supplier)
	{
		List<E> expected = supplier.get().collect(new ArrayList<>());
		List<E> actual = new ArrayList<>();
		LongObjToLongFunction<E> collect = (res, each) -> {actual.add(each); return res;};
		long result = supplier.get().reduce(Long.MIN_VALUE, collect);
		assertEquals(Long.MIN_VALUE, result);
		assertIterableEquals(expected, actual);
	}

	@Test
	default void testReduce_Null()
	{
		Reducible<E, ?> reducible = getAnyReducible();
		assertThrows(NullPointerException.class, () -> reducible.reduce(null));
		assertThrows(NullPointerException.class, () -> reducible.reduce(new Object(),null));
		assertThrows(NullPointerException.class, () -> reducible.reduce(0.0, null));
		assertThrows(NullPointerException.class, () -> reducible.reduce(0, (IntObjToIntFunction<? super E>) null));
		assertThrows(NullPointerException.class, () -> reducible.reduce(0L, (LongObjToLongFunction<? super E>) null));
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testNonNull(Supplier<T> supplier)
	{
		List<Object> expected = new ArrayList<>();
		supplier.get().forEach(e -> {if (e != null) expected.add(e);});
		Reducible<E, ?> actual = supplier.get().nonNull();
		assertIterableEquals(expected, actual.collect(new ArrayList<>()));
	}

	@ParameterizedTest
	@MethodSource({"getSingletonReducibles", "getMultitonReducibles"})
	default void testAllMatch(Supplier<T> supplier)
	{
		// match all elements
		assertTrue(supplier.get().allMatch(each -> true), "Expected allMatch() == true");
		// match not all elements
		Predicate<E> matchNotAll = new Predicate<>() {
			// match: no element if singleton, otherwise every odd element
			boolean flag = supplier.get().count() == 1;
			@Override
			public boolean test(E t)
			{
				flag = !flag;
				return flag;
			}
		};
		assertFalse(supplier.get().allMatch(matchNotAll), "Expected allMatch() == false");
	}

	@ParameterizedTest
	@MethodSource("getEmptyReducibles")
	default void testAllMatch_Empty(Supplier<T> supplier)
	{
		Reducible<E, ?> reducible = supplier.get();
		assertTrue(reducible.allMatch(each -> false), "Exepted allMatch() == true if reducible is empty");
	}

	@Test
	default void testAllMatch_Null()
	{
		Reducible<E, ?> reducible = getAnyReducible();
		assertThrows(NullPointerException.class, () -> reducible.allMatch(null));
	}

	@ParameterizedTest
	@MethodSource({"getSingletonReducibles", "getMultitonReducibles"})
	default void testAnyMatch(Supplier<T> supplier)
	{
		// match no element
		assertFalse(supplier.get().anyMatch(each -> false), "Expected anyMatch() == false");
		// match some elements
		Predicate<E> matchSome = new Predicate<>() {
			// match: first element if singleton, otherwise every even element
			boolean flag = supplier.get().count() > 1;
			@Override
			public boolean test(E t)
			{
				flag = !flag;
				return flag;
			}
		};
		assertTrue(supplier.get().anyMatch(matchSome), "Expected anyMatch() == true");
	}

	@ParameterizedTest
	@MethodSource("getEmptyReducibles")
	default void testAnyMatch_Empty(Supplier<T> supplier)
	{
		Reducible<E, ?> reducible = supplier.get();
		assertFalse(reducible.anyMatch(each -> true), "Exepted anyMatch() == false if reducible is empty");
	}

	@Test
	default void testAnyMatch_Null()
	{
		Reducible<E, ?> reducible = getAnyReducible();
		assertThrows(NullPointerException.class, () -> reducible.anyMatch(null));
	}

	@ParameterizedTest
	@MethodSource({"getSingletonReducibles", "getMultitonReducibles"})
	default void testNoneMatch(Supplier<T> supplier)
	{
		// match no element
		assertTrue(supplier.get().noneMatch(each -> false), "Expected noneMatch() == true");
		// match some elements
		Predicate<E> matchSome = new Predicate<>() {
			// match: first element if singleton, otherwise every even element
			boolean flag = supplier.get().count() > 1;
			@Override
			public boolean test(E t)
			{
				flag = !flag;
				return flag;
			}
		};
		assertFalse(supplier.get().noneMatch(matchSome), "Expected noneMatch() == false");
	}

	@ParameterizedTest
	@MethodSource("getEmptyReducibles")
	default void testNoneMatch_Empty(Supplier<T> supplier)
	{
		Reducible<E, ?> reducible = supplier.get();
		assertTrue(reducible.allMatch(each -> false), "Exepted noneMatch() == false if iterator is empty");
	}

	@Test
	default void testNoneMatch_Null()
	{
		Reducible<E, ?> reducible = getAnyReducible();
		assertThrows(NullPointerException.class, () -> reducible.noneMatch(null));
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testAsString(Supplier<T> supplier)
	{
		String expected = "[" + supplier.get().reduce("", (str, each) -> str + (str.isEmpty() ? "" : ", ") + each) + "]";
		String actual = supplier.get().asString();
		assertEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testContains(Supplier<T> supplier)
	{
		ArrayList<E> elements = supplier.get().collect(new ArrayList<>());
		for (E each : elements) {
			assertTrue(supplier.get().contains(each), "Expected contains(" + each + ") == true");
		}
		for (Object each : getExcluded(supplier.get())) {
			assertFalse(supplier.get().contains(each), "Expected contains(" + each + ") == false");
		}
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testCount(Supplier<T> supplier)
	{
		ArrayList<E> elements = supplier.get().collect(new ArrayList<>());
		long expected = elements.size();
		long actual = supplier.get().count();
		assertEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testCountPredicate(Supplier<T> supplier)
	{
		ArrayList<E> elements = supplier.get().collect(new ArrayList<>());
		long expected = (elements.size() + 1) / 2;
		Predicate<E> odd = new Predicate<>() {
			int i = 1;
			@Override
			public boolean test(E t)
			{
				return i++ % 2 == 1;
			}
		};
		long actual = supplier.get().count(odd);
		assertEquals(expected, actual);
	}

	@Test
	default void testCountPredicate_Null()
	{
		Reducible<E, ?> reducible = getAnyReducible();
		assertThrows(NullPointerException.class, () -> reducible.count(null));
	}


	@ParameterizedTest
	@MethodSource("getReducibles")
	default void testDetect_AllFalse(Supplier<T> supplier)
	{
		Reducible<E, ?> reducible = supplier.get();
		assertThrows(NoSuchElementException.class, () -> reducible.detect(each -> false));
	}

	@ParameterizedTest
	@MethodSource("getEmptyReducibles")
	default void testDetect_Empty(Supplier<T> supplier)
	{
		Reducible<E, ?> reducible = supplier.get();
		assertThrows(NoSuchElementException.class, () -> reducible.detect(each -> true));
	}

	@ParameterizedTest
	@MethodSource("getSingletonReducibles")
	default void testDetect_Singleton(Supplier<T> supplier)
	{
		// match first element
		E expected = supplier.get().collect(new ArrayList<>()).get(0);
		E actual = supplier.get().detect(each -> true);
		assertEquals(expected, actual);
	}

	@ParameterizedTest
	@MethodSource("getMultitonReducibles")
	default void testDetect_Multiton(Supplier<T> supplier)
	{
		// match second element
		E expected = supplier.get().collect(new ArrayList<>()).get(1);
		Predicate<E> second = new Predicate<>() {
			boolean flag = true;
			@Override
			public boolean test(E each)
			{
				flag = !flag;
				return flag;
			}
		};
		E actual = supplier.get().detect(second);
		assertEquals(expected, actual);
	}

	@Test
	default void testDetect_Null()
	{
		Reducible<E, ?> reducible = getAnyReducible();
		assertThrows(NullPointerException.class, () -> reducible.detect(null));
	}



	interface Of<E, T extends Reducible<E, ?>> extends ReducibleTest<E, T>
	{
		T getReducible(Object[] objects);

		@Override
		default Stream<Supplier<T>> getDuplicatesReducibles()
		{
			return getDuplicatesArraysOfObject().map(args-> () -> getReducible(args));
		}

		@Override
		default Stream<Supplier<T>> getEmptyReducibles()
		{
			return getEmptyArraysOfObject().map(args-> () -> getReducible(args));
		}

		@Override
		default Stream<Supplier<T>> getSingletonReducibles()
		{
			return getSingletonArraysOfObject().map(args-> () -> getReducible(args));
		}

		@Override
		default Stream<Supplier<T>> getMultitonReducibles()
		{
			return getMultitonArraysOfObject().map(args-> () -> getReducible(args));
		}

		@Override
		default Iterable<Object> getExcluded(T iterable)
		{
			return getUniqueObjects();
		}
	}



	// Test-data sets

	default Stream<Object[]> getArraysOfObject()
	{
		return Stream.concat(Stream.concat(getEmptyArraysOfObject(), getSingletonArraysOfObject()), getMultitonArraysOfObject());
	}

	/* Workaround to pass Object[] as argument */
	default Stream<Arguments> getArraysAsArguments()
	{
		return getArraysOfObject().map(array -> Arguments.of((Object) array));
	}

	default Stream<Object[]> getDuplicatesArraysOfObject()
	{
		return Stream.of(
				new Object[] {null, null,
						"first", "first",
						"second", "second"},
				new Object[] {null, null,
						"first", "first",
						"third", "third",
						"first", "first",
						null, null});
	}

	default Stream<Object[]> getEmptyArraysOfObject()
	{
		return Stream.<Object[]>of(new Object[] {});
	}

	default Stream<Object[]> getSingletonArraysOfObject()
	{
		return Stream.of(new Object[] {"first"},
				new Object[] {null});
	}

	default Stream<Object[]> getMultitonArraysOfObject()
	{
		return Stream.of(new Object[] {"first", "second", "third"},
				new Object[] {null, "first", null, "second", null, "third", null});
	}

	/* Workaround to pass Object[] as argument */
	default Stream<Arguments> getMultitonArraysAsArguments()
	{
		return getMultitonArraysOfObject().map(array -> Arguments.of((Object) array));
	}

	default List<Object> getUniqueObjects()
	{
		return List.of(new Object(), new Object(), new Object());
	}

	default Stream<double[]> getArraysOfDouble()
	{
		return Stream.concat(Stream.concat(getEmptyArraysOfDouble(), getSingletonArraysOfDouble()), getMultitonArraysOfDouble());
	}

	default Stream<double[]> getDuplicatesArraysOfDouble()
	{
		return Stream.of(new double[] {-3.5, -3.5,
						-2.0, -2.0,
						-1.0, -1.0,
						-0.0, +0.0,
						+1.0, +1.0,
						+2.0, +2.0,
						+3.5, +3.5},
				new double[] {Double.NaN, Double.NaN,
						Double.MIN_VALUE, Double.MIN_VALUE,
						Double.MIN_NORMAL, Double.MIN_NORMAL,
						Double.MAX_VALUE, Double.MAX_VALUE,
						Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
						Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
						Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
						Double.MAX_VALUE, Double.MAX_VALUE,
						Double.MIN_NORMAL, Double.MIN_NORMAL,
						Double.MIN_VALUE, Double.MIN_VALUE,
						Double.NaN, Double.NaN});
	}

	default Stream<double[]> getEmptyArraysOfDouble()
	{
		return Stream.of(new double[] {});
	}

	default Stream<double[]> getSingletonArraysOfDouble()
	{
		return Stream.of(new double[] {1.0});
	}

	default Stream<double[]> getMultitonArraysOfDouble()
	{
		return Stream.of(new double[] {-3.5, -2.0, -1.0, 0.0, 1.0, 2.0, 3.5},
				new double[] {Double.NaN,
						Double.MIN_VALUE,
						Double.MIN_NORMAL,
						Double.MAX_VALUE,
						Double.NEGATIVE_INFINITY,
						Double.POSITIVE_INFINITY,});
	}

	default List<Double> getExclusionListOfDouble()
	{
		List<Double> excluded = new ArrayList<>();
		excluded.add(Double.NaN);
		excluded.add(Double.NEGATIVE_INFINITY);
		excluded.add(-100000000.0);
		excluded.add(-10000.0);
		excluded.add(-100.0);
		excluded.add(-10.0);
		excluded.add(-2.0);
		excluded.add(-1.0);
		excluded.add(-0.0);
		excluded.add(+0.0);
		excluded.add(Double.MIN_VALUE);
		excluded.add(Double.MIN_NORMAL);
		excluded.add(1.0);
		excluded.add(2.0);
		excluded.add(10.0);
		excluded.add(100.0);
		excluded.add(10000.0);
		excluded.add(100000000.0);
		excluded.add(Double.MAX_VALUE);
		excluded.add(Double.POSITIVE_INFINITY);
		return excluded;
	}

	default Stream<int[]> getArraysOfInt()
	{
		return Stream.concat(Stream.concat(getEmptyArraysOfInt(), getSingletonArraysOfInt()), getMultitonArraysOfInt());
	}

	default Stream<int[]> getDuplicatesArraysOfInt()
	{
		return Stream.of(new int[] {-3, -3,
						-1, -1,
						-2, -2,
						-0, +0,
						+1, +1,
						+2, +2,
						+3, +3},
				new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE,
						Integer.MAX_VALUE, Integer.MAX_VALUE,
						Integer.MIN_VALUE, Integer.MIN_VALUE});
	}

	default Stream<int[]> getEmptyArraysOfInt()
	{
		return Stream.of(new int[] {});
	}

	default Stream<int[]> getSingletonArraysOfInt()
	{
		return Stream.of(new int[] {1});
	}

	default Stream<int[]> getMultitonArraysOfInt()
	{
		return Stream.of(new int[] {-3, -2, -1, 0, 1, 2, 3},
				new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE});
	}

	default List<Integer> getExclusionListOfInt()
	{
		List<Integer> excluded = new ArrayList<>();
		excluded.add(Integer.MIN_VALUE);
		excluded.add(-100000000);
		excluded.add(-10000);
		excluded.add(-100);
		excluded.add(-10);
		excluded.add(-2);
		excluded.add(-1);
		excluded.add(0);
		excluded.add(1);
		excluded.add(2);
		excluded.add(10);
		excluded.add(100);
		excluded.add(10000);
		excluded.add(100000000);
		excluded.add(Integer.MAX_VALUE);
		return excluded;
	}

	default Stream<long[]> getArraysOfLong()
	{
		return Stream.concat(Stream.concat(getEmptyArraysOfLong(), getSingletonArraysOfLong()), getMultitonArraysOfLong());
	}

	default Stream<long []> getDuplicatesArraysOfLong()
	{
		return Stream.of(new long [] {-3L, -3L,
						-2L, -2L,
						-1L, -1L,
						-0L, +0L,
						+1L, +1L,
						+2L, +2L,
						+3L, +3L},
				new long[] {Long.MIN_VALUE, Long.MIN_VALUE,
						Long.MAX_VALUE, Long.MAX_VALUE,
						Long.MIN_VALUE, Long.MIN_VALUE});
	}

	default Stream<long []> getEmptyArraysOfLong()
	{
		return Stream.of(new long [] {});
	}

	default Stream<long []> getSingletonArraysOfLong()
	{
		return Stream.of(new long [] {1L});
	}

	default Stream<long []> getMultitonArraysOfLong()
	{
		return Stream.of(new long [] {-3L, -2L, -1L, 0L, 1L, 2L, 3L},
				new long[] {Long.MIN_VALUE, Long.MAX_VALUE});
	}

	default List<Long> getExclusionListOfLong()
	{
		List<Long> excluded = new ArrayList<>();
		excluded.add(Long.MIN_VALUE);
		excluded.add(-100000000L);
		excluded.add(-10000L);
		excluded.add(-100L);
		excluded.add(-10L);
		excluded.add(-2L);
		excluded.add(-1L);
		excluded.add(0L);
		excluded.add(1L);
		excluded.add(2L);
		excluded.add(10L);
		excluded.add(100L);
		excluded.add(10000L);
		excluded.add(100000000L);
		excluded.add(Long.MAX_VALUE);
		return excluded;
	}
}
