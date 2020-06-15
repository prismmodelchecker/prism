//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Marcus Daum <marcus.daum@ivi.fraunhofer.de> (Frauenhofer Institut)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package common.iterable;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;

import common.functions.ObjDoubleFunction;
import common.functions.ObjIntFunction;
import common.functions.ObjLongFunction;

/**
 * Abstract base class of efficient iterators over array slices.
 * Implementations should release the underlying array after iteration.
 *
 * @param <E> type of the array elements
 */
public abstract class ArrayIterator<E> implements FunctionalIterator<E>
{
	protected final int toIndex;
	protected int nextIndex;

	/**
	 * Iterate over all elements in index interval [0, toIndex).
	 *
	 * @param toIndex last index, exclusively
	 */
	public ArrayIterator(int toIndex)
	{
		this(0, toIndex);
	}

	/**
	 * Iterate over all elements in index interval [fromIndex, toIndex).
	 *
	 * @param fromIndex first index, inclusive
	 * @param toIndex last index, exclusive
	 */
	public ArrayIterator(int fromIndex, int toIndex)
	{
		if (fromIndex < 0) {
			throw new IllegalArgumentException("non-negative fromIndex expected, got: " + fromIndex);
		}
		this.nextIndex = fromIndex;
		this.toIndex   = toIndex;
	}

	@Override
	public boolean hasNext()
	{
		if (nextIndex < toIndex) {
			return true;
		}
		release();
		return false;
	}

	public int nextIndex()
	{
		requireNext();
		return nextIndex;
	}

	public int count()
	{
		return toIndex - nextIndex;
	}

	protected void requireNext()
	{
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
	}

	protected void release()
	{
		nextIndex = toIndex;
	}



	/**
	 * Generic implementation of an iterator over an array slice.
	 *
	 * @param <E> type of the array elements
	 */
	public static class Of<E> extends ArrayIterator<E>
	{
		public static final Object[] EMPTY_OBJECT = new Object[0];

		protected E[] elements;

		@SafeVarargs
		public Of(E... elements)
		{
			this(elements, 0, elements.length);
		}

		public Of(E[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex);
			Objects.requireNonNull(elements);
			this.elements = elements;
		}

		@Override
		public E next()
		{
			requireNext();
			return elements[nextIndex++];
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action)
		{
			Objects.requireNonNull(action);
			while (nextIndex < toIndex) {
				action.accept(elements[nextIndex++]);
			}
			release();
		}

		@Override
		public int collectAndCount(Collection<? super E> collection)
		{
			int count = 0;
			while (nextIndex < toIndex) {
				count++;
				collection.add(elements[nextIndex++]);
			}
			release();
			return count;
		}

		@Override
		public int collectAndCount(E[] array, int offset)
		{
			int count = offset;
			while (nextIndex < toIndex) {
				array[count++] = elements[nextIndex++];
			}
			release();
			return count - offset;
		}

		@Override
		public <T> T reduce(T identity, BiFunction<T, ? super E, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (nextIndex < toIndex) {
				result = accumulator.apply(result, elements[nextIndex++]);
			}
			release();
			return result;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void release()
		{
			super.release();
			elements = (E[]) EMPTY_OBJECT;
		}
	}



	/**
	 * Primitive specialisation for {@code double} of an iterator over an array slice.
	 */
	public static class OfDouble extends ArrayIterator<Double> implements FunctionalPrimitiveIterator.OfDouble
	{
		public static final double[] EMPTY_DOUBLE = new double[0];

		protected double[] elements;

		@SafeVarargs
		public OfDouble(double... elements)
		{
			this(elements, 0, elements.length);
		}

		public OfDouble(double[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex);
			Objects.requireNonNull(elements);
			this.elements = elements;
		}

		@Override
		public double nextDouble()
		{
			requireNext();
			return elements[nextIndex++];
		}

		@Override
		public void forEachRemaining(DoubleConsumer action)
		{
			Objects.requireNonNull(action);
			while (nextIndex < toIndex) {
				action.accept(elements[nextIndex++]);
			}
			release();
		}

		@Override
		public int collectAndCount(Collection<? super Double> collection)
		{
			int count = 0;
			while (nextIndex < toIndex) {
				count++;
				collection.add(elements[nextIndex++]);
			}
			release();
			return count;
		}

		@Override
		public int collectAndCount(Double[] array, int offset)
		{
			int count = offset;
			while (nextIndex < toIndex) {
				array[count++] = elements[nextIndex++];
			}
			release();
			return count - offset;
		}

		@Override
		public int collectAndCount(double[] array, int offset)
		{
			int count = offset;
			while (nextIndex < toIndex) {
				array[count++] = elements[nextIndex++];
			}
			release();
			return count - offset;
		}

		@Override
		public boolean contains(double d)
		{
			while (nextIndex < toIndex) {
				if (d == elements[nextIndex++]) {
					return true;
				}
			}
			return false;
		}

		@Override
		public OptionalDouble max()
		{
			if (! hasNext()) {
				return OptionalDouble.empty();
			}
			double max = elements[nextIndex++];
			while (nextIndex < toIndex) {
				double next = elements[nextIndex++];
				max      = next > max ? next : max;
			}
			release();
			return OptionalDouble.of(max);
		}

		@Override
		public OptionalDouble min()
		{
			if (! hasNext()) {
				return OptionalDouble.empty();
			}
			double min = elements[nextIndex++];
			while (nextIndex < toIndex) {
				double next = elements[nextIndex++];
				min      = next < min ? next : min;
			}
			release();
			return OptionalDouble.of(min);
		}

		@Override
		public <T> T reduce(T identity, BiFunction<T, ? super Double, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (nextIndex < toIndex) {
				result = accumulator.apply(result, elements[nextIndex++]);
			}
			release();
			return result;
		}

		@Override
		public <T> T reduce(T identity, ObjDoubleFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (nextIndex < toIndex) {
				result = accumulator.apply(result, elements[nextIndex++]);
			}
			release();
			return result;
		}

		@Override
		public double reduce(double identity, DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			double result = identity;
			while (nextIndex < toIndex) {
				result = accumulator.applyAsDouble(result, elements[nextIndex++]);
			}
			release();
			return result;
		}

		@Override
		public double sum()
		{
			double sum = 0;
			while (nextIndex < toIndex) {
				sum += elements[nextIndex++];
			}
			release();
			return sum;
		}

		@Override
		protected void release()
		{
			super.release();
			elements = EMPTY_DOUBLE;
		}
	}



	/**
	 * Primitive specialisation for {@code int} of an iterator over an array slice.
	 */
	public static class OfInt extends ArrayIterator<Integer> implements FunctionalPrimitiveIterator.OfInt
	{
		public static final int[] EMPTY_INT = new int[0];

		protected int[] elements;

		@SafeVarargs
		public OfInt(int... elements)
		{
			this(elements, 0, elements.length);
		}

		public OfInt(int[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex);
			Objects.requireNonNull(elements);
			this.elements = elements;
		}

		@Override
		public int nextInt()
		{
			requireNext();
			return elements[nextIndex++];
		}

		@Override
		public void forEachRemaining(IntConsumer action)
		{
			Objects.requireNonNull(action);
			while (nextIndex < toIndex) {
				action.accept(elements[nextIndex++]);
			}
			release();
		}

		@Override
		public int collectAndCount(Collection<? super Integer> collection)
		{
			int count = 0;
			while (nextIndex < toIndex) {
				count++;
				collection.add(elements[nextIndex++]);
			}
			release();
			return count;
		}

		@Override
		public int collectAndCount(Integer[] array, int offset)
		{
			int count = offset;
			while (nextIndex < toIndex) {
				array[count++] = elements[nextIndex++];
			}
			release();
			return count - offset;
		}

		@Override
		public int collectAndCount(int[] array, int offset)
		{
			int count = offset;
			while (nextIndex < toIndex) {
				array[count++] = elements[nextIndex++];
			}
			release();
			return count - offset;
		}

		@Override
		public boolean contains(int i)
		{
			while (nextIndex < toIndex) {
				if (i == elements[nextIndex++]) {
					return true;
				}
			}
			return false;
		}

		@Override
		public OptionalInt max()
		{
			if (! hasNext()) {
				return OptionalInt.empty();
			}
			int max = elements[nextIndex++];
			while (nextIndex < toIndex) {
				int next = elements[nextIndex++];
				max      = next > max ? next : max;
			}
			release();
			return OptionalInt.of(max);
		}

		@Override
		public OptionalInt min()
		{
			if (! hasNext()) {
				return OptionalInt.empty();
			}
			int min = elements[nextIndex++];
			while (nextIndex < toIndex) {
				int next = elements[nextIndex++];
				min      = next < min ? next : min;
			}
			release();
			return OptionalInt.of(min);
		}

		@Override
		public <T> T reduce(T identity, BiFunction<T, ? super Integer, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (nextIndex < toIndex) {
				result = accumulator.apply(result, elements[nextIndex++]);
			}
			release();
			return result;
		}

		@Override
		public <T> T reduce(T identity, ObjIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (nextIndex < toIndex) {
				result = accumulator.apply(result, elements[nextIndex++]);
			}
			release();
			return result;
		}

		@Override
		public int reduce(int identity, IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			int result = identity;
			while (nextIndex < toIndex) {
				result = accumulator.applyAsInt(result, elements[nextIndex++]);
			}
			release();
			return result;
		}

		@Override
		public int sum()
		{
			int sum = 0;
			while (nextIndex < toIndex) {
				sum += elements[nextIndex++];
			}
			release();
			return sum;
		}

		@Override
		protected void release()
		{
			super.release();
			elements = EMPTY_INT;
		}
	}



	/**
	 * Primitive specialisation for {@code long} of an iterator over an array slice.
	 */
	public static class OfLong extends ArrayIterator<Long> implements FunctionalPrimitiveIterator.OfLong
	{
		public static final long[] EMPTY_LONG = new long[0];

		protected long[] elements;

		@SafeVarargs
		public OfLong(long... elements)
		{
			this(elements, 0, elements.length);
		}

		public OfLong(long[] elements, int fromIndex, int toIndex)
		{
			super(fromIndex, toIndex);
			Objects.requireNonNull(elements);
			this.elements = elements;
		}

		@Override
		public long nextLong()
		{
			requireNext();
			return elements[nextIndex++];
		}

		@Override
		public void forEachRemaining(LongConsumer action)
		{
			Objects.requireNonNull(action);
			while (nextIndex < toIndex) {
				action.accept(elements[nextIndex++]);
			}
			release();
		}

		@Override
		public int collectAndCount(Collection<? super Long> collection)
		{
			int count = 0;
			while (nextIndex < toIndex) {
				count++;
				collection.add(elements[nextIndex++]);
			}
			release();
			return count;
		}

		@Override
		public int collectAndCount(Long[] array, int offset)
		{
			int count = offset;
			while (nextIndex < toIndex) {
				array[count++] = elements[nextIndex++];
			}
			release();
			return count - offset;
		}

		@Override
		public int collectAndCount(long[] array, int offset)
		{
			int count = offset;
			while (nextIndex < toIndex) {
				array[count++] = elements[nextIndex++];
			}
			release();
			return count - offset;
		}

		@Override
		public boolean contains(long l)
		{
			while (nextIndex < toIndex) {
				if (l == elements[nextIndex++]) {
					return true;
				}
			}
			return false;
		}

		@Override
		public OptionalLong max()
		{
			if (! hasNext()) {
				return OptionalLong.empty();
			}
			long max = elements[nextIndex++];
			while (nextIndex < toIndex) {
				long next = elements[nextIndex++];
				max      = next > max ? next : max;
			}
			release();
			return OptionalLong.of(max);
		}

		@Override
		public OptionalLong min()
		{
			if (! hasNext()) {
				return OptionalLong.empty();
			}
			long min = elements[nextIndex++];
			while (nextIndex < toIndex) {
				long next = elements[nextIndex++];
				min      = next < min ? next : min;
			}
			release();
			return OptionalLong.of(min);
		}

		@Override
		public <T> T reduce(T identity, BiFunction<T, ? super Long, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (nextIndex < toIndex) {
				result = accumulator.apply(result, elements[nextIndex++]);
			}
			release();
			return result;
		}

		@Override
		public <T> T reduce(T identity, ObjLongFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (nextIndex < toIndex) {
				result = accumulator.apply(result, elements[nextIndex++]);
			}
			release();
			return result;
		}

		@Override
		public long reduce(long identity, LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			long result = identity;
			while (nextIndex < toIndex) {
				result = accumulator.applyAsLong(result, elements[nextIndex++]);
			}
			release();
			return result;
		}

		@Override
		public long sum()
		{
			long sum = 0;
			while (nextIndex < toIndex) {
				sum += elements[nextIndex++];
			}
			release();
			return sum;
		}

		@Override
		protected void release()
		{
			super.release();
			elements = EMPTY_LONG;
		}
	}
}
