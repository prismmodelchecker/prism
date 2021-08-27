package common.iterable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;

import static common.iterable.Assertions.assertIterableEquals;
import static common.iterable.PrimitiveIterable.*;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

interface MappingIterableTest
{
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ObjToObj implements MappingIterableTest, FunctionalIterableTest.Of<Object,MappingIterable.ObjToObj<Object,Object>>
	{
		@Override
		public MappingIterable.ObjToObj<Object, Object> getReducible(Object[] objects)
		{
			Map<Object, Object> lookup = new LinkedHashMap<>();
			for (int i=0, length=objects.length; i<length; i++) {
				lookup.put(objects[i], objects[length-1-i]);
			}
			return new MappingIterable.ObjToObj<>(lookup.keySet(), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysAsArguments")
		void testOf(Object[] objects)
		{
			ArrayList<String> expected = new ArrayList<>();
			for (Object each : objects) {
				expected.add(Objects.toString(each));
			}
			Iterable<Object> iterable = new IterableArray.Of<>(objects);
			Iterable<String> actual = new MappingIterable.ObjToObj<>(iterable, Objects::toString);
			assertIterableEquals(expected, actual);
		}

		@Test
		public void testOf_Null()
		{
			EmptyIterable.Of<Object> iterable = EmptyIterable.of();
			assertThrows(NullPointerException.class, () -> new MappingIterable.ObjToObj<>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterable.ObjToObj<>(iterable, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ObjToDouble implements MappingIterableTest, FunctionalPrimitiveIterableTest.OfDouble<MappingIterable.ObjToDouble<Double>>
	{
		@Override
		public MappingIterable.ObjToDouble<Double> getReducible(double[] numbers)
		{
			List<Double> boxed = FunctionalIterable.ofDouble(numbers).collect(new ArrayList<>());
			return new MappingIterable.ObjToDouble<>(boxed, Double::doubleValue);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		public void testObjToDouble(double[] numbers)
		{
			ArrayList<Double> expected = new ArrayList<>();
			for (double d : numbers) {
				expected.add(d);
			}
			FunctionalPrimitiveIterable.OfDouble actual = new MappingIterable.ObjToDouble<>(expected, each -> each);
			assertIterableEquals(unboxDouble(expected), actual);
		}

		@Test
		public void testObjToDouble_Null()
		{
			EmptyIterable.Of<Double> iterable = EmptyIterable.of();
			assertThrows(NullPointerException.class, () -> new MappingIterable.ObjToDouble<Double>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterable.ObjToDouble<>(iterable, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ObjToInt implements MappingIterableTest, FunctionalPrimitiveIterableTest.OfInt<MappingIterable.ObjToInt<Integer>>
	{
		@Override
		public MappingIterable.ObjToInt<Integer> getReducible(int[] numbers)
		{
			List<Integer> boxed = FunctionalIterable.ofInt(numbers).collect(new ArrayList<>());
			return new MappingIterable.ObjToInt<>(boxed, Integer::intValue);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		public void testObjToInt(int[] numbers)
		{
			ArrayList<Integer> expected = new ArrayList<>();
			for (int i : numbers) {
				expected.add(i);
			}
			FunctionalPrimitiveIterable.OfInt actual = new MappingIterable.ObjToInt<>(expected, each -> each);
			assertIterableEquals(unboxInt(expected), actual);
		}

		@Test
		public void testObjToInt_Null()
		{
			EmptyIterable.Of<Integer> iterable = EmptyIterable.of();
			assertThrows(NullPointerException.class, () -> new MappingIterable.ObjToInt<Integer>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterable.ObjToInt<>(iterable, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ObjToLong implements MappingIterableTest, FunctionalPrimitiveIterableTest.OfLong<MappingIterable.ObjToLong<Long>>
	{
		@Override
		public MappingIterable.ObjToLong<Long> getReducible(long[] numbers)
		{
			List<Long> boxed = FunctionalIterable.ofLong(numbers).collect(new ArrayList<>());
			return new MappingIterable.ObjToLong<>(boxed, Long::longValue);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		public void testObjToLong(long[] numbers)
		{
			ArrayList<Long> expected = new ArrayList<>();
			for (long l : numbers) {
				expected.add(l);
			}
			FunctionalPrimitiveIterable.OfLong actual = new MappingIterable.ObjToLong<>(expected, each -> each);
			assertIterableEquals(unboxLong(expected), actual);
		}

		@Test
		public void testObjToLong_Null()
		{
			EmptyIterable.Of<Long> iterable = EmptyIterable.of();
			assertThrows(NullPointerException.class, () -> new MappingIterable.ObjToLong<Long>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterable.ObjToLong<>(iterable, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class DoubleToObj implements MappingIterableTest, FunctionalIterableTest.Of<Object,MappingIterable.DoubleToObj<Object>>
	{
		@Override
		public MappingIterable.DoubleToObj<Object> getReducible(Object[] objects)
		{
			Map<Double, Object> lookup = new LinkedHashMap<>();
			FunctionalIterable.of(objects).reduce(1.5, (d, e) -> {lookup.put(d, e); return d + 1;});
			return new MappingIterable.DoubleToObj<>(unboxDouble(lookup.keySet()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		public void testDoubleToObj(double[] numbers)
		{
			ArrayList<String> expected = new ArrayList<>();
			for (double d : numbers) {
				expected.add(Objects.toString(d));
			}
			IterableArray.OfDouble iterable = new IterableArray.OfDouble(numbers);
			Iterable<String> actual = new MappingIterable.DoubleToObj<>(iterable, Objects::toString);
			assertIterableEquals(expected, actual);
		}

		@Test
		public void testDoubleToObj_Null()
		{
			EmptyIterable.OfDouble iterable = EmptyIterable.ofDouble();
			assertThrows(NullPointerException.class, () -> new MappingIterable.DoubleToObj<>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterable.DoubleToObj<>(iterable, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class DoubleToDouble implements MappingIterableTest, FunctionalPrimitiveIterableTest.OfDouble<MappingIterable.DoubleToDouble>
	{
		@Override
		public MappingIterable.DoubleToDouble getReducible(double[] numbers)
		{
			Map<Double, Double> lookup = new LinkedHashMap<>();
			FunctionalIterable.ofDouble(numbers).reduce(1.5, (double d, double e) -> {lookup.put(d, e); return d + 1;});
			return new MappingIterable.DoubleToDouble(unboxDouble(lookup.keySet()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		public void testDoubleToDouble(double[] numbers)
		{
			ArrayList<Double> expected = new ArrayList<>();
			for (double d : numbers) {
				expected.add(d + 1.0);
			}
			IterableArray.OfDouble iterable = new IterableArray.OfDouble(numbers);
			PrimitiveIterable.OfDouble actual = new MappingIterable.DoubleToDouble(iterable, d -> d + 1.0);
			assertIterableEquals(unboxDouble(expected), actual);
		}

		@Test
		public void testDoubleToDouble_Null()
		{
			EmptyIterable.OfDouble iterable = EmptyIterable.ofDouble();
			assertThrows(NullPointerException.class, () -> new MappingIterable.DoubleToDouble(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterable.DoubleToDouble(iterable, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class DoubleToInt implements MappingIterableTest, FunctionalPrimitiveIterableTest.OfInt<MappingIterable.DoubleToInt>
	{
		@Override
		public MappingIterable.DoubleToInt getReducible(int[] numbers)
		{
			Map<Double, Integer> lookup = new LinkedHashMap<>();
			FunctionalIterable.ofInt(numbers).reduce(1.5, (Double d, Integer e) -> {lookup.put(d, e); return d + 1;});
			return new MappingIterable.DoubleToInt(unboxDouble(lookup.keySet()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		public void testDoubleToInt(double[] numbers)
		{
			ArrayList<Integer> expected = new ArrayList<>();
			for (double d : numbers) {
				expected.add((int) d + 1);
			}
			IterableArray.OfDouble iterable = new IterableArray.OfDouble(numbers);
			PrimitiveIterable.OfInt actual = new MappingIterable.DoubleToInt(iterable, d -> (int) d + 1);
			assertIterableEquals(unboxInt(expected), actual);
		}

		@Test
		public void testDoubleToInt_Null()
		{
			EmptyIterable.OfDouble iterable = EmptyIterable.ofDouble();
			assertThrows(NullPointerException.class, () -> new MappingIterable.DoubleToInt(null, each -> (int) each));
			assertThrows(NullPointerException.class, () -> new MappingIterable.DoubleToInt(iterable, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class DoubleToLong implements MappingIterableTest, FunctionalPrimitiveIterableTest.OfLong<MappingIterable.DoubleToLong>
	{
		@Override
		public MappingIterable.DoubleToLong getReducible(long[] numbers)
		{
			Map<Double, Long> lookup = new LinkedHashMap<>();
			FunctionalIterable.ofLong(numbers).reduce(1.5, (double d, long e) -> {lookup.put(d, e); return d + 1;});
			return new MappingIterable.DoubleToLong(unboxDouble(lookup.keySet()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfDouble")
		public void testDoubleToLong(double[] numbers)
		{
			ArrayList<Long> expected = new ArrayList<>();
			for (double d : numbers) {
				expected.add((long) d + 1);
			}
			IterableArray.OfDouble iterable = new IterableArray.OfDouble(numbers);
			PrimitiveIterable.OfLong actual = new MappingIterable.DoubleToLong(iterable, d -> (long) d + 1);
			assertIterableEquals(unboxLong(expected), actual);
		}

		@Test
		public void testDoubleToLong_Null()
		{
			EmptyIterable.OfDouble iterable = EmptyIterable.ofDouble();
			assertThrows(NullPointerException.class, () -> new MappingIterable.DoubleToLong(null, each -> (long) each));
			assertThrows(NullPointerException.class, () -> new MappingIterable.DoubleToLong(iterable, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class IntToObj implements MappingIterableTest, FunctionalIterableTest.Of<Object,MappingIterable.IntToObj<Object>>
	{
		@Override
		public MappingIterable.IntToObj<Object> getReducible(Object[] objects)
		{
			Map<Integer, Object> lookup = new LinkedHashMap<>();
			FunctionalIterable.of(objects).reduce(1, (int i, Object e) -> {lookup.put(i, e); return i + 1;});
			return new MappingIterable.IntToObj<>(unboxInt(lookup.keySet()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		public void testIntToObj(int[] numbers)
		{
			ArrayList<String> expected = new ArrayList<>();
			for (int i : numbers) {
				expected.add(Objects.toString(i));
			}
			IterableArray.OfInt iterable = new IterableArray.OfInt(numbers);
			Iterable<String> actual = new MappingIterable.IntToObj<>(iterable, Objects::toString);
			assertIterableEquals(expected, actual);
		}

		@Test
		public void testIntToObj_Null()
		{
			EmptyIterable.OfInt iterable = EmptyIterable.ofInt();
			assertThrows(NullPointerException.class, () -> new MappingIterable.IntToObj<>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterable.IntToObj<>(iterable, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class IntToDouble implements MappingIterableTest, FunctionalPrimitiveIterableTest.OfDouble<MappingIterable.IntToDouble>
	{
		@Override
		public MappingIterable.IntToDouble getReducible(double[] numbers)
		{
			Map<Integer, Double> lookup = new LinkedHashMap<>();
			FunctionalIterable.ofDouble(numbers).reduce(1, (int i, double e) -> {lookup.put(i, e); return i + 1;});
			return new MappingIterable.IntToDouble(unboxInt(lookup.keySet()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		public void testIntToDouble(int[] numbers)
		{
			ArrayList<Double> expected = new ArrayList<>();
			for (int i : numbers) {
				expected.add(i + 1.0);
			}
			IterableArray.OfInt iterable = new IterableArray.OfInt(numbers);
			PrimitiveIterable.OfDouble actual = new MappingIterable.IntToDouble(iterable, i -> i + 1.0);
			assertIterableEquals(unboxDouble(expected), actual);
		}

		@Test
		public void testIntToDouble_Null()
		{
			EmptyIterable.OfInt iterable = EmptyIterable.ofInt();
			assertThrows(NullPointerException.class, () -> new MappingIterable.IntToDouble(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterable.IntToDouble(iterable, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class IntToInt implements MappingIterableTest, FunctionalPrimitiveIterableTest.OfInt<MappingIterable.IntToInt>
	{
		@Override
		public MappingIterable.IntToInt getReducible(int[] numbers)
		{
			Map<Integer, Integer> lookup = new LinkedHashMap<>();
			FunctionalIterable.ofInt(numbers).reduce(1, (int i, int e) -> {lookup.put(i, e); return i + 1;});
			return new MappingIterable.IntToInt(unboxInt(lookup.keySet()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		public void testIntToInt(int[] numbers)
		{
			ArrayList<Integer> expected = new ArrayList<>();
			for (int i : numbers) {
				expected.add(i + 1);
			}
			IterableArray.OfInt iterable = new IterableArray.OfInt(numbers);
			PrimitiveIterable.OfInt actual = new MappingIterable.IntToInt(iterable, i -> i + 1);
			assertIterableEquals(unboxInt(expected), actual);
		}

		@Test
		public void testIntToInt_Null()
		{
			EmptyIterable.OfInt iterable = EmptyIterable.ofInt();
			assertThrows(NullPointerException.class, () -> new MappingIterable.IntToInt(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterable.IntToInt(iterable, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class IntToLong implements MappingIterableTest, FunctionalPrimitiveIterableTest.OfLong<MappingIterable.IntToLong>
	{
		@Override
		public MappingIterable.IntToLong getReducible(long[] numbers)
		{
			Map<Integer, Long> lookup = new LinkedHashMap<>();
			FunctionalIterable.ofLong(numbers).reduce(1, (Integer i, Long e) -> {lookup.put(i, e); return i + 1;});
			return new MappingIterable.IntToLong(unboxInt(lookup.keySet()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfInt")
		public void testIntToLong(int[] numbers)
		{
			ArrayList<Long> expected = new ArrayList<>();
			for (int i : numbers) {
				expected.add((long) i + 1);
			}
			IterableArray.OfInt iterable = new IterableArray.OfInt(numbers);
			PrimitiveIterable.OfLong actual = new MappingIterable.IntToLong(iterable, i -> (long) i + 1);
			assertIterableEquals(unboxLong(expected), actual);
		}

		@Test
		public void testIntToLong_Null()
		{
			EmptyIterable.OfInt iterable = EmptyIterable.ofInt();
			assertThrows(NullPointerException.class, () -> new MappingIterable.IntToLong(null, each -> (long) each));
			assertThrows(NullPointerException.class, () -> new MappingIterable.IntToLong(iterable, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LongToObj implements MappingIterableTest, FunctionalIterableTest.Of<Object,MappingIterable.LongToObj<Object>>
	{
		@Override
		public MappingIterable.LongToObj<Object> getReducible(Object[] objects)
		{
			Map<Long, Object> lookup = new LinkedHashMap<>();
			FunctionalIterable.of(objects).reduce(1L, (long l, Object e) -> {lookup.put(l, e); return l + 1;});
			return new MappingIterable.LongToObj<>(unboxLong(lookup.keySet()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		public void testLongToObj(long[] numbers)
		{
			ArrayList<String> expected = new ArrayList<>();
			for (long l : numbers) {
				expected.add(Objects.toString(l));
			}
			IterableArray.OfLong iterable = new IterableArray.OfLong(numbers);
			Iterable<String> actual = new MappingIterable.LongToObj<>(iterable, Objects::toString);
			assertIterableEquals(expected, actual);
		}

		@Test
		public void testLongToObj_Null()
		{
			EmptyIterable.OfLong iterable = EmptyIterable.ofLong();
			assertThrows(NullPointerException.class, () -> new MappingIterable.LongToObj<>(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterable.LongToObj<>(iterable, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LongToDouble implements MappingIterableTest, FunctionalPrimitiveIterableTest.OfDouble<MappingIterable.LongToDouble>
	{
		@Override
		public MappingIterable.LongToDouble getReducible(double[] numbers)
		{
			Map<Long, Double> lookup = new LinkedHashMap<>();
			FunctionalIterable.ofDouble(numbers).reduce(1L, (long l, double e) -> {lookup.put(l, e); return l + 1;});
			return new MappingIterable.LongToDouble(unboxLong(lookup.keySet()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		public void testLongToDouble(long[] numbers)
		{
			ArrayList<Double> expected = new ArrayList<>();
			for (long l : numbers) {
				expected.add(l + 1.0);
			}
			IterableArray.OfLong iterable = new IterableArray.OfLong(numbers);
			PrimitiveIterable.OfDouble actual = new MappingIterable.LongToDouble(iterable, l -> l + 1.0);
			assertIterableEquals(unboxDouble(expected), actual);
		}

		@Test
		public void testLongToDouble_Null()
		{
			EmptyIterable.OfLong iterable = EmptyIterable.ofLong();
			assertThrows(NullPointerException.class, () -> new MappingIterable.LongToDouble(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterable.LongToDouble(iterable, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LongToInt implements MappingIterableTest, FunctionalPrimitiveIterableTest.OfInt<MappingIterable.LongToInt>
	{
		@Override
		public MappingIterable.LongToInt getReducible(int[] numbers)
		{
			Map<Long, Integer> lookup = new LinkedHashMap<>();
			FunctionalIterable.ofInt(numbers).reduce(1L, (Long l, Integer e) -> {lookup.put(l, e); return l + 1;});
			return new MappingIterable.LongToInt(unboxLong(lookup.keySet()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		public void testLongToInt(long[] numbers)
		{
			ArrayList<Integer> expected = new ArrayList<>();
			for (long l : numbers) {
				expected.add((int) l + 1);
			}
			IterableArray.OfLong iterable = new IterableArray.OfLong(numbers);
			PrimitiveIterable.OfInt actual = new MappingIterable.LongToInt(iterable, l -> (int) l + 1);
			assertIterableEquals(unboxInt(expected), actual);
		}

		@Test
		public void testLongToInt_Null()
		{
			EmptyIterable.OfLong iterable = EmptyIterable.ofLong();
			assertThrows(NullPointerException.class, () -> new MappingIterable.LongToInt(null, each -> (int) each));
			assertThrows(NullPointerException.class, () -> new MappingIterable.LongToInt(iterable, null));
		}
	}



	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LongToLong implements MappingIterableTest, FunctionalPrimitiveIterableTest.OfLong<MappingIterable.LongToLong>
	{
		@Override
		public MappingIterable.LongToLong getReducible(long[] numbers)
		{
			Map<Long, Long> lookup = new LinkedHashMap<>();
			FunctionalIterable.ofLong(numbers).reduce(1L, (long l, long e) -> {lookup.put(l, e); return l + 1;});
			return new MappingIterable.LongToLong(unboxLong(lookup.keySet()), lookup::get);
		}

		@ParameterizedTest
		@MethodSource("getArraysOfLong")
		public void testLongToLong(long[] numbers)
		{
			ArrayList<Long> expected = new ArrayList<>();
			for (long l : numbers) {
				expected.add(l + 1);
			}
			IterableArray.OfLong iterable = new IterableArray.OfLong(numbers);
			PrimitiveIterable.OfLong actual = new MappingIterable.LongToLong(iterable, l -> l + 1);
			assertIterableEquals(unboxLong(expected), actual);
		}

		@Test
		public void testLongToLong_Null()
		{
			EmptyIterable.OfLong iterable = EmptyIterable.ofLong();
			assertThrows(NullPointerException.class, () -> new MappingIterable.LongToLong(null, each -> each));
			assertThrows(NullPointerException.class, () -> new MappingIterable.LongToLong(iterable, null));
		}
	}
}
