package common.iterable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

import static org.junit.jupiter.api.Assertions.*;

public class Assertions
{
	/**
	 * Assert two double values are equals while assuming -0.0 == +0.0 and NaN == NaN.
	 */
	protected static void assertDoubleEquals(double expected, double actual)
	{
		assertTrue(expected == actual || Double.isNaN(expected) && Double.isNaN(actual));
	}

	public static void assertIterableEquals(PrimitiveIterable.OfDouble expected, PrimitiveIterable.OfDouble actual)
	{
		assertIteratorEquals(expected.iterator(), actual.iterator());
	}

	public static void assertIterableEquals(PrimitiveIterable.OfInt expected, PrimitiveIterable.OfInt actual)
	{
		assertIteratorEquals(expected.iterator(), actual.iterator());
	}

	public static void assertIterableEquals(PrimitiveIterable.OfLong expected, PrimitiveIterable.OfLong actual)
	{
		assertIteratorEquals(expected.iterator(), actual.iterator());
	}

	public static void assertIteratorEquals(Iterator<?> expected, Iterator<?> actual)
	{
		while(expected.hasNext() && actual.hasNext()) {
			assertEquals(expected.next(), actual.next());
		}
		assertFalse(expected.hasNext());
		assertFalse(actual.hasNext());
		assertThrows(NoSuchElementException.class, expected::next);
		assertThrows(NoSuchElementException.class, actual::next);
	}

	public static void assertIteratorEquals(PrimitiveIterator.OfDouble expected, PrimitiveIterator.OfDouble actual)
	{
		while(expected.hasNext() && actual.hasNext()) {
			double exp = expected.nextDouble();
			double act = actual.nextDouble();
			assertDoubleEquals(exp, act);
		}
		assertFalse(expected.hasNext());
		assertFalse(actual.hasNext());
		assertThrows(NoSuchElementException.class, expected::next);
		assertThrows(NoSuchElementException.class, actual::next);
	}

	public static void assertIteratorEquals(PrimitiveIterator.OfInt expected, PrimitiveIterator.OfInt actual)
	{
		while(expected.hasNext() && actual.hasNext()) {
			assertEquals(expected.nextInt(), actual.nextInt());
		}
		assertFalse(expected.hasNext());
		assertFalse(actual.hasNext());
		assertThrows(NoSuchElementException.class, expected::next);
		assertThrows(NoSuchElementException.class, actual::next);
	}

	public static void assertIteratorEquals(PrimitiveIterator.OfLong expected, PrimitiveIterator.OfLong actual)
	{
		while(expected.hasNext() && actual.hasNext()) {
			assertEquals(expected.nextLong(), actual.nextLong());
		}
		assertFalse(expected.hasNext());
		assertFalse(actual.hasNext());
		assertThrows(NoSuchElementException.class, expected::next);
		assertThrows(NoSuchElementException.class, actual::next);
	}

}
