package common.iterable;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.PrimitiveIterator;

import static common.iterable.Assertions.assertIteratorEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PrimitiveIterableTest
{
	@Test
	public void testUnboxDoubleIterable()
	{
		Iterable<Double> numbers = List.of(0.0, 1.0, 2.0);
		PrimitiveIterable.OfDouble actual = PrimitiveIterable.unboxDouble(numbers);
		PrimitiveIterator.OfDouble expected = PrimitiveIterable.unboxDouble(numbers.iterator());
		assertIteratorEquals(expected, actual.iterator());
	}

	@Test
	public void testUnboxDoubleIterator()
	{
		Iterable<Double> numbers = List.of(0.0, 1.0, 2.0);
		PrimitiveIterator.OfDouble actual = PrimitiveIterable.unboxDouble(numbers).iterator();
		for (double d : numbers) {
			assertEquals(d, actual.nextDouble());
		}
	}

	@Test
	public void testUnboxDoubleIterator_NullValues()
	{
		Iterable<Double> numbers = Arrays.asList(new Double[] {null});
		PrimitiveIterator.OfDouble actual = PrimitiveIterable.unboxDouble(numbers).iterator();
		assertThrows(NullPointerException.class, actual::nextDouble);
	}

	@Test
	public void testUnboxDouble_Null()
	{
		assertThrows(NullPointerException.class, () -> PrimitiveIterable.unboxDouble((Iterator<Double>) null));
		assertThrows(NullPointerException.class, () -> PrimitiveIterable.unboxDouble((Iterable<Double>) null));
	}

	@Test
	public void testUnboxIntIterable()
	{
		Iterable<Integer> numbers = List.of(0, 1, 2);
		PrimitiveIterable.OfInt actual = PrimitiveIterable.unboxInt(numbers);
		PrimitiveIterator.OfInt expected = PrimitiveIterable.unboxInt(numbers.iterator());
		assertIteratorEquals(expected, actual.iterator());
	}

	@Test
	public void testUnboxIntIterator()
	{
		Iterable<Integer> numbers = List.of(0, 1, 2);
		PrimitiveIterator.OfInt actual = PrimitiveIterable.unboxInt(numbers).iterator();
		for (int i : numbers) {
			assertEquals(i, actual.nextInt());
		}
	}

	@Test
	public void testUnboxIntIterator_NullValues()
	{
		Iterable<Integer> numbers = Arrays.asList(new Integer[] {null});
		PrimitiveIterator.OfInt actual = PrimitiveIterable.unboxInt(numbers).iterator();
		assertThrows(NullPointerException.class, actual::nextInt);
	}

	@Test
	public void testUnboxInt_Null()
	{
		assertThrows(NullPointerException.class, () -> PrimitiveIterable.unboxInt((Iterator<Integer>) null));
		assertThrows(NullPointerException.class, () -> PrimitiveIterable.unboxInt((Iterable<Integer>) null));
	}

	@Test
	public void testLongDoubleIterable()
	{
		Iterable<Long> numbers = List.of(0L, 1L, 2L);
		PrimitiveIterable.OfLong actual = PrimitiveIterable.unboxLong(numbers);
		PrimitiveIterator.OfLong expected = PrimitiveIterable.unboxLong(numbers.iterator());
		assertIteratorEquals(expected, actual.iterator());
	}

	@Test
	public void testUnboxLongIterator()
	{
		Iterable<Long> numbers = List.of(0L, 1L, 2L);
		PrimitiveIterator.OfLong actual = PrimitiveIterable.unboxLong(numbers).iterator();
		for (long l : numbers) {
			assertEquals(l, actual.nextLong());
		}
	}

	@Test
	public void testUnboxLongIterator_NullValues()
	{
		Iterable<Long> numbers = Arrays.asList(new Long[] {null});
		PrimitiveIterator.OfLong actual = PrimitiveIterable.unboxLong(numbers).iterator();
		assertThrows(NullPointerException.class, actual::nextLong);
	}

	@Test
	public void testUnboxLong_Null()
	{
		assertThrows(NullPointerException.class, () -> PrimitiveIterable.unboxLong((Iterator<Long>) null));
		assertThrows(NullPointerException.class, () -> PrimitiveIterable.unboxLong((Iterable<Long>) null));
	}
}
