package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;

import static common.iterable.Assertions.assertIteratorEquals;
import static common.iterable.PrimitiveIterable.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

interface MappingIteratorTest
{
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ObjToObj implements MappingIteratorTest, FunctionalIteratorTest.Of<Object,MappingIterator.ObjToObj<Object,Object>>
	{
		@Override
		public MappingIterator.ObjToObj<Object, Object> getReducible(Object[] objects)
		{
			Map<Object, Object> lookup = new LinkedHashMap<>();
			for (int i=0, length=objects.length; i<length; i++) {
				lookup.put(objects[i], objects[length-1-i]);
			}
			return new MappingIterator.ObjToObj<>(lookup.keySet().iterator(), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysAsArguments")
		void testOf(Object[] objects)
		{
			ArrayList<String> expected = new ArrayList<>();
			for (Object each : objects) {
				expected.add(Objects.toString(each));
			}
			Iterator<Object> iterator = new ArrayIterator.Of<>(objects);
			Iterator<String> actual = new MappingIterator.ObjToObj<>(iterator, Objects::toString);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testOf_Null()
		{
			Iterator<Object> iterator = EmptyIterator.of();
			assertThrows(NullPointerException.class, () -> new MappingIterator.ObjToObj<>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.ObjToObj<>(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ObjToDouble implements MappingIteratorTest, FunctionalPrimitiveIteratorTest.OfDouble<MappingIterator.ObjToDouble<Double>>
	{
		@Override
		public MappingIterator.ObjToDouble<Double> getReducible(double[] numbers)
		{
			List<Double> boxed = FunctionalIterator.ofDouble(numbers).collect(new ArrayList<>());
			return new MappingIterator.ObjToDouble<>(boxed.iterator(), Double::doubleValue);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		public void testObjToDouble(double[] numbers)
		{
			ArrayList<Double> expected = new ArrayList<>();
			for (double d : numbers) {
				expected.add(d);
			}
			Iterator<Double> iterator = expected.iterator();
			FunctionalPrimitiveIterator.OfDouble actual = new MappingIterator.ObjToDouble<>(iterator, each -> each);
			assertIteratorEquals(unboxDouble(expected.iterator()), actual);
		}

		@Test
		public void testObjToDouble_Null()
		{
			Iterator<Double> iterator = EmptyIterator.of();
			assertThrows(NullPointerException.class, () -> new MappingIterator.ObjToDouble<Double>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.ObjToDouble<>(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ObjToInt implements MappingIteratorTest, FunctionalPrimitiveIteratorTest.OfInt<MappingIterator.ObjToInt<Integer>>
	{
		@Override
		public MappingIterator.ObjToInt<Integer> getReducible(int[] numbers)
		{
			List<Integer> boxed = FunctionalIterator.ofInt(numbers).collect(new ArrayList<>());
			return new MappingIterator.ObjToInt<>(boxed.iterator(), Integer::intValue);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		public void testObjToInt(int[] numbers)
		{
			ArrayList<Integer> expected = new ArrayList<>();
			for (int i : numbers) {
				expected.add(i);
			}
			Iterator<Integer> iterator = expected.iterator();
			FunctionalPrimitiveIterator.OfInt actual = new MappingIterator.ObjToInt<>(iterator, each -> each);
			assertIteratorEquals(unboxInt(expected.iterator()), actual);
		}

		@Test
		public void testObjToInt_Null()
		{
			Iterator<Integer> iterator = EmptyIterator.of();
			assertThrows(NullPointerException.class, () -> new MappingIterator.ObjToInt<Integer>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.ObjToInt<>(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ObjToLong implements MappingIteratorTest, FunctionalPrimitiveIteratorTest.OfLong<MappingIterator.ObjToLong<Long>>
	{
		@Override
		public MappingIterator.ObjToLong<Long> getReducible(long[] numbers)
		{
			List<Long> boxed = FunctionalIterator.ofLong(numbers).collect(new ArrayList<>());
			return new MappingIterator.ObjToLong<>(boxed.iterator(), Long::longValue);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		public void testObjToLong(long[] numbers)
		{
			ArrayList<Long> expected = new ArrayList<>();
			for (long l : numbers) {
				expected.add(l);
			}
			Iterator<Long> iterator = expected.iterator();
			FunctionalPrimitiveIterator.OfLong actual = new MappingIterator.ObjToLong<>(iterator, each -> each);
			assertIteratorEquals(unboxLong(expected.iterator()), actual);
		}

		@Test
		public void testObjToLong_Null()
		{
			Iterator<Long> iterator = EmptyIterator.of();
			assertThrows(NullPointerException.class, () -> new MappingIterator.ObjToLong<Long>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.ObjToLong<>(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class DoubleToObj implements MappingIteratorTest, FunctionalIteratorTest.Of<Object,MappingIterator.DoubleToObj<Object>>
	{
		@Override
		public MappingIterator.DoubleToObj<Object> getReducible(Object[] objects)
		{
			Map<Double, Object> lookup = new LinkedHashMap<>();
			FunctionalIterator.of(objects).reduce(1.5, (d, e) -> {lookup.put(d, e); return d + 1;});
			return new MappingIterator.DoubleToObj<>(unboxDouble(lookup.keySet().iterator()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		public void testDoubleToObj(double[] numbers)
		{
			ArrayList<String> expected = new ArrayList<>();
			for (double d : numbers) {
				expected.add(Objects.toString(d));
			}
			ArrayIterator.OfDouble iterator = new ArrayIterator.OfDouble(numbers);
			Iterator<String> actual = new MappingIterator.DoubleToObj<>(iterator, Objects::toString);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testDoubleToObj_Null()
		{
			PrimitiveIterator.OfDouble iterator = EmptyIterator.ofDouble();
			assertThrows(NullPointerException.class, () -> new MappingIterator.DoubleToObj<>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.DoubleToObj<>(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class DoubleToDouble implements MappingIteratorTest, FunctionalPrimitiveIteratorTest.OfDouble<MappingIterator.DoubleToDouble>
	{
		@Override
		public MappingIterator.DoubleToDouble getReducible(double[] numbers)
		{
			Map<Double, Double> lookup = new LinkedHashMap<>();
			FunctionalIterator.ofDouble(numbers).reduce(1.5, (double d, double e) -> {lookup.put(d, e); return d + 1;});
			return new MappingIterator.DoubleToDouble(unboxDouble(lookup.keySet().iterator()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		public void testDoubleToDouble(double[] numbers)
		{
			ArrayList<Double> expected = new ArrayList<>();
			for (double d : numbers) {
				expected.add(d + 1.0);
			}
			ArrayIterator.OfDouble iterator = new ArrayIterator.OfDouble(numbers);
			PrimitiveIterator.OfDouble actual = new MappingIterator.DoubleToDouble(iterator, d -> d + 1.0);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testDoubleToDouble_Null()
		{
			PrimitiveIterator.OfDouble iterator = EmptyIterator.ofDouble();
			assertThrows(NullPointerException.class, () -> new MappingIterator.DoubleToDouble(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.DoubleToDouble(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class DoubleToInt implements MappingIteratorTest, FunctionalPrimitiveIteratorTest.OfInt<MappingIterator.DoubleToInt>
	{
		@Override
		public MappingIterator.DoubleToInt getReducible(int[] numbers)
		{
			Map<Double, Integer> lookup = new LinkedHashMap<>();
			FunctionalIterator.ofInt(numbers).reduce(1.5, (Double d, Integer e) -> {lookup.put(d, e); return d + 1;});
			return new MappingIterator.DoubleToInt(unboxDouble(lookup.keySet().iterator()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		public void testDoubleToInt(double[] numbers)
		{
			ArrayList<Integer> expected = new ArrayList<>();
			for (double d : numbers) {
				expected.add((int) d + 1);
			}
			ArrayIterator.OfDouble iterator = new ArrayIterator.OfDouble(numbers);
			PrimitiveIterator.OfInt actual = new MappingIterator.DoubleToInt(iterator, d -> (int) d + 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testDoubleToInt_Null()
		{
			PrimitiveIterator.OfDouble iterator = EmptyIterator.ofDouble();
			assertThrows(NullPointerException.class, () -> new MappingIterator.DoubleToInt(null, each -> (int) each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.DoubleToInt(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class DoubleToLong implements MappingIteratorTest, FunctionalPrimitiveIteratorTest.OfLong<MappingIterator.DoubleToLong>
	{
		@Override
		public MappingIterator.DoubleToLong getReducible(long[] numbers)
		{
			Map<Double, Long> lookup = new LinkedHashMap<>();
			FunctionalIterator.ofLong(numbers).reduce(1.5, (double d, long e) -> {lookup.put(d, e); return d + 1;});
			return new MappingIterator.DoubleToLong(unboxDouble(lookup.keySet().iterator()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		public void testDoubleToLong(double[] numbers)
		{
			ArrayList<Long> expected = new ArrayList<>();
			for (double d : numbers) {
				expected.add((long) d + 1);
			}
			ArrayIterator.OfDouble iterator = new ArrayIterator.OfDouble(numbers);
			PrimitiveIterator.OfLong actual = new MappingIterator.DoubleToLong(iterator, d -> (long) d + 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testDoubleToLong_Null()
		{
			PrimitiveIterator.OfDouble iterator = EmptyIterator.ofDouble();
			assertThrows(NullPointerException.class, () -> new MappingIterator.DoubleToLong(null, each -> (long) each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.DoubleToLong(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class IntToObj implements MappingIteratorTest, FunctionalIteratorTest.Of<Object,MappingIterator.IntToObj<Object>>
	{
		@Override
		public MappingIterator.IntToObj<Object> getReducible(Object[] objects)
		{
			Map<Integer, Object> lookup = new LinkedHashMap<>();
			FunctionalIterator.of(objects).reduce(1, (int i, Object e) -> {lookup.put(i, e); return i + 1;});
			return new MappingIterator.IntToObj<>(unboxInt(lookup.keySet().iterator()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		public void testIntToObj(int[] numbers)
		{
			ArrayList<String> expected = new ArrayList<>();
			for (int i : numbers) {
				expected.add(Objects.toString(i));
			}
			ArrayIterator.OfInt iterator = new ArrayIterator.OfInt(numbers);
			Iterator<String> actual = new MappingIterator.IntToObj<>(iterator, Objects::toString);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testIntToObj_Null()
		{
			PrimitiveIterator.OfInt iterator = EmptyIterator.ofInt();
			assertThrows(NullPointerException.class, () -> new MappingIterator.IntToObj<>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.IntToObj<>(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class IntToDouble implements MappingIteratorTest, FunctionalPrimitiveIteratorTest.OfDouble<MappingIterator.IntToDouble>
	{
		@Override
		public MappingIterator.IntToDouble getReducible(double[] numbers)
		{
			Map<Integer, Double> lookup = new LinkedHashMap<>();
			FunctionalIterator.ofDouble(numbers).reduce(1, (int i, double e) -> {lookup.put(i, e); return i + 1;});
			return new MappingIterator.IntToDouble(unboxInt(lookup.keySet().iterator()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		public void testIntToDouble(int[] numbers)
		{
			ArrayList<Double> expected = new ArrayList<>();
			for (int i : numbers) {
				expected.add(i + 1.0);
			}
			ArrayIterator.OfInt iterator = new ArrayIterator.OfInt(numbers);
			PrimitiveIterator.OfDouble actual = new MappingIterator.IntToDouble(iterator, i -> i + 1.0);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testIntToDouble_Null()
		{
			PrimitiveIterator.OfInt iterator = EmptyIterator.ofInt();
			assertThrows(NullPointerException.class, () -> new MappingIterator.IntToDouble(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.IntToDouble(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class IntToInt implements MappingIteratorTest, FunctionalPrimitiveIteratorTest.OfInt<MappingIterator.IntToInt>
	{
		@Override
		public MappingIterator.IntToInt getReducible(int[] numbers)
		{
			Map<Integer, Integer> lookup = new LinkedHashMap<>();
			FunctionalIterator.ofInt(numbers).reduce(1, (int i, int e) -> {lookup.put(i, e); return i + 1;});
			return new MappingIterator.IntToInt(unboxInt(lookup.keySet().iterator()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		public void testIntToInt(int[] numbers)
		{
			ArrayList<Integer> expected = new ArrayList<>();
			for (int i : numbers) {
				expected.add(i + 1);
			}
			ArrayIterator.OfInt iterator = new ArrayIterator.OfInt(numbers);
			PrimitiveIterator.OfInt actual = new MappingIterator.IntToInt(iterator, i -> i + 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testIntToInt_Null()
		{
			PrimitiveIterator.OfInt iterator = EmptyIterator.ofInt();
			assertThrows(NullPointerException.class, () -> new MappingIterator.IntToInt(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.IntToInt(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class IntToLong implements MappingIteratorTest, FunctionalPrimitiveIteratorTest.OfLong<MappingIterator.IntToLong>
	{
		@Override
		public MappingIterator.IntToLong getReducible(long[] numbers)
		{
			Map<Integer, Long> lookup = new LinkedHashMap<>();
			FunctionalIterator.ofLong(numbers).reduce(1, (Integer i, Long e) -> {lookup.put(i, e); return i + 1;});
			return new MappingIterator.IntToLong(unboxInt(lookup.keySet().iterator()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		public void testIntToLong(int[] numbers)
		{
			ArrayList<Long> expected = new ArrayList<>();
			for (int i : numbers) {
				expected.add((long) i + 1);
			}
			ArrayIterator.OfInt iterator = new ArrayIterator.OfInt(numbers);
			PrimitiveIterator.OfLong actual = new MappingIterator.IntToLong(iterator, i -> (long) i + 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testIntToLong_Null()
		{
			PrimitiveIterator.OfInt iterator = EmptyIterator.ofInt();
			assertThrows(NullPointerException.class, () -> new MappingIterator.IntToLong(null, each -> (long) each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.IntToLong(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LongToObj implements MappingIteratorTest, FunctionalIteratorTest.Of<Object,MappingIterator.LongToObj<Object>>
	{
		@Override
		public MappingIterator.LongToObj<Object> getReducible(Object[] objects)
		{
			Map<Long, Object> lookup = new LinkedHashMap<>();
			FunctionalIterator.of(objects).reduce(1L, (long l, Object e) -> {lookup.put(l, e); return l + 1;});
			return new MappingIterator.LongToObj<>(unboxLong(lookup.keySet().iterator()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		public void testLongToObj(long[] numbers)
		{
			ArrayList<String> expected = new ArrayList<>();
			for (long l : numbers) {
				expected.add(Objects.toString(l));
			}
			ArrayIterator.OfLong iterator = new ArrayIterator.OfLong(numbers);
			Iterator<String> actual = new MappingIterator.LongToObj<>(iterator, Objects::toString);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testLongToObj_Null()
		{
			PrimitiveIterator.OfLong iterator = EmptyIterator.ofLong();
			assertThrows(NullPointerException.class, () -> new MappingIterator.LongToObj<>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.LongToObj<>(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LongToDouble implements MappingIteratorTest, FunctionalPrimitiveIteratorTest.OfDouble<MappingIterator.LongToDouble>
	{
		@Override
		public MappingIterator.LongToDouble getReducible(double[] numbers)
		{
			Map<Long, Double> lookup = new LinkedHashMap<>();
			FunctionalIterator.ofDouble(numbers).reduce(1L, (long l, double e) -> {lookup.put(l, e); return l + 1;});
			return new MappingIterator.LongToDouble(unboxLong(lookup.keySet().iterator()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		public void testLongToDouble(long[] numbers)
		{
			ArrayList<Double> expected = new ArrayList<>();
			for (long l : numbers) {
				expected.add(l + 1.0);
			}
			ArrayIterator.OfLong iterator = new ArrayIterator.OfLong(numbers);
			PrimitiveIterator.OfDouble actual = new MappingIterator.LongToDouble(iterator, l -> l + 1.0);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testLongToDouble_Null()
		{
			PrimitiveIterator.OfLong iterator = EmptyIterator.ofLong();
			assertThrows(NullPointerException.class, () -> new MappingIterator.LongToDouble(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.LongToDouble(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LongToInt implements MappingIteratorTest, FunctionalPrimitiveIteratorTest.OfInt<MappingIterator.LongToInt>
	{
		@Override
		public MappingIterator.LongToInt getReducible(int[] numbers)
		{
			Map<Long, Integer> lookup = new LinkedHashMap<>();
			FunctionalIterator.ofInt(numbers).reduce(1L, (Long l, Integer e) -> {lookup.put(l, e); return l + 1;});
			return new MappingIterator.LongToInt(unboxLong(lookup.keySet().iterator()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		public void testLongToInt(long[] numbers)
		{
			ArrayList<Integer> expected = new ArrayList<>();
			for (long l : numbers) {
				expected.add((int) l + 1);
			}
			ArrayIterator.OfLong iterator = new ArrayIterator.OfLong(numbers);
			PrimitiveIterator.OfInt actual = new MappingIterator.LongToInt(iterator, l -> (int) l + 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testLongToInt_Null()
		{
			PrimitiveIterator.OfLong iterator = EmptyIterator.ofLong();
			assertThrows(NullPointerException.class, () -> new MappingIterator.LongToInt(null, each -> (int) each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.LongToInt(iterator, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LongToLong implements MappingIteratorTest, FunctionalPrimitiveIteratorTest.OfLong<MappingIterator.LongToLong>
	{
		@Override
		public MappingIterator.LongToLong getReducible(long[] numbers)
		{
			Map<Long, Long> lookup = new LinkedHashMap<>();
			FunctionalIterator.ofLong(numbers).reduce(1L, (long l, long e) -> {lookup.put(l, e); return l + 1;});
			return new MappingIterator.LongToLong(unboxLong(lookup.keySet().iterator()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		public void testLongToLong(long[] numbers)
		{
			ArrayList<Long> expected = new ArrayList<>();
			for (long l : numbers) {
				expected.add(l + 1);
			}
			ArrayIterator.OfLong iterator = new ArrayIterator.OfLong(numbers);
			PrimitiveIterator.OfLong actual = new MappingIterator.LongToLong(iterator, l -> l + 1);
			assertIteratorEquals(expected.iterator(), actual);
		}

		@Test
		public void testLongToLong_Null()
		{
			PrimitiveIterator.OfLong iterator = EmptyIterator.ofLong();
			assertThrows(NullPointerException.class, () -> new MappingIterator.LongToLong(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterator.LongToLong(iterator, null));
		}
	}
}
