//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
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
import java.util.Objects;
import java.util.Optional;
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
 * Base class for an iterator that ranges over a single element,
 * static helpers for common primitive iterators.
 */
public abstract class SingletonIterator<E> implements FunctionalIterator<E>
{
	protected abstract void release();



	public static class Of<E> extends SingletonIterator<E>
	{
		protected Optional<E> element;

		public Of(E element)
		{
			this.element = Optional.of(element);
		}

		@Override
		public boolean hasNext()
		{
			return element.isPresent();
		}

		@Override
		public E next()
		{
			E next = element.get();
			release();
			return next;
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action)
		{
			element.ifPresent(action);
			release();
		}

		@Override
		public int collectAndCount(Collection<? super E> collection)
		{
			if (element.isPresent()) {
				collection.add(element.get());
				release();
				return 1;
			}
			return 0;
		}

		@Override
		public int collectAndCount(E[] array, int offset)
		{
			if (element.isPresent()) {
				array[offset] = element.get();
				release();
				return 1;
			}
			return 0;
		}

		@Override
		public boolean contains(Object obj)
		{
			return element.isPresent() && element.get().equals(obj);
		}

		@Override
		public int count()
		{
			return element.isPresent() ? 1 : 0;
		}

		@Override
		public <T> T reduce(T identity, BiFunction<T, ? super E, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = element.isPresent() ? accumulator.apply(identity, element.get()) : identity;
			release();
			return result;
		}

		@Override
		protected void release()
		{
			element = Optional.empty();
		}
	}



	public static class OfDouble extends SingletonIterator<Double> implements FunctionalPrimitiveIterator.OfDouble
	{
		protected OptionalDouble element;
	
		public OfDouble(double element)
		{
			this.element = OptionalDouble.of(element);
		}
	
		@Override
		public boolean hasNext()
		{
			return element.isPresent();
		}
	
		@Override
		public double nextDouble()
		{
			double next = element.getAsDouble();
			release();
			return next;
		}

		@Override
		public void forEachRemaining(DoubleConsumer action)
		{
			element.ifPresent(action);
			release();
		}

		@Override
		public int collectAndCount(Collection<? super Double> collection)
		{
			if (element.isPresent()) {
				collection.add(element.getAsDouble());
				release();
				return 1;
			}
			return 0;
		}

		@Override
		public int collectAndCount(Double[] array, int offset)
		{
			if (element.isPresent()) {
				array[offset] = element.getAsDouble();
				release();
				return 1;
			}
			return 0;
		}

		@Override
		public int collectAndCount(double[] array, int offset)
		{
			if (element.isPresent()) {
				array[offset] = element.getAsDouble();
				release();
				return 1;
			}
			return 0;
		}

		@Override
		public boolean contains(double i)
		{
			return element.isPresent() && element.getAsDouble() == i;
		}

		@Override
		public int count()
		{
			return element.isPresent() ? 1 : 0;
		}

		@Override
		public OptionalDouble max()
		{
			return element;

		}

		@Override
		public OptionalDouble min()
		{
			return element;
		}

		@Override
		public <T> T reduce(T identity, BiFunction<T, ? super Double, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = element.isPresent() ? accumulator.apply(identity, element.getAsDouble()) : identity;
			release();
			return result;
		}

		@Override
		public <T> T reduce(T identity, ObjDoubleFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = element.isPresent() ? accumulator.apply(identity, element.getAsDouble()) : identity;
			release();
			return result;
		}

		@Override
		public double reduce(double identity, DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			double result = element.isPresent() ? accumulator.applyAsDouble(identity, element.getAsDouble()) : identity;
			release();
			return result;
		}

		@Override
		public double sum()
		{
			return element.orElse(0.0);
		}

		@Override
		protected void release()
		{
			element = OptionalDouble.empty();
		}
	}



	public static class OfInt extends SingletonIterator<Integer> implements FunctionalPrimitiveIterator.OfInt
	{
		protected OptionalInt element;

		public OfInt(int element)
		{
			this.element = OptionalInt.of(element);
		}

		@Override
		public boolean hasNext()
		{
			return element.isPresent();
		}

		@Override
		public int nextInt()
		{
			int next = element.getAsInt();
			release();
			return next;
		}

		@Override
		public void forEachRemaining(IntConsumer action)
		{
			element.ifPresent(action);
			release();
		}

		@Override
		public int collectAndCount(Collection<? super Integer> collection)
		{
			if (element.isPresent()) {
				collection.add(element.getAsInt());
				release();
				return 1;
			}
			return 0;
		}

		@Override
		public int collectAndCount(Integer[] array, int offset)
		{
			if (element.isPresent()) {
				array[offset] = element.getAsInt();
				release();
				return 1;
			}
			return 0;
		}

		@Override
		public int collectAndCount(int[] array, int offset)
		{
			if (element.isPresent()) {
				array[offset] = element.getAsInt();
				release();
				return 1;
			}
			return 0;
		}

		@Override
		public boolean contains(int i)
		{
			return element.isPresent() && element.getAsInt() == i;
		}

		@Override
		public int count()
		{
			return element.isPresent() ? 1 : 0;
		}

		@Override
		public OptionalInt max()
		{
			return element;

		}

		@Override
		public OptionalInt min()
		{
			return element;
		}

		@Override
		public <T> T reduce(T identity, BiFunction<T, ? super Integer, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = element.isPresent() ? accumulator.apply(identity, element.getAsInt()) : identity;
			release();
			return result;
		}

		@Override
		public <T> T reduce(T identity, ObjIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = element.isPresent() ? accumulator.apply(identity, element.getAsInt()) : identity;
			release();
			return result;
		}

		@Override
		public int reduce(int identity, IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			int result = element.isPresent() ? accumulator.applyAsInt(identity, element.getAsInt()) : identity;
			release();
			return result;
		}

		@Override
		public int sum()
		{
			return element.orElse(0);
		}

		@Override
		protected void release()
		{
			element = OptionalInt.empty();
		}
	}



	public static class OfLong extends SingletonIterator<Long> implements FunctionalPrimitiveIterator.OfLong
	{
		protected OptionalLong element;

		public OfLong(Long element)
		{
			this.element = OptionalLong.of(element);
		}

		@Override
		public boolean hasNext()
		{
			return element.isPresent();
		}

		@Override
		public long nextLong()
		{
			long next = element.getAsLong();
			release();
			return next;
		}

		@Override
		public void forEachRemaining(LongConsumer action)
		{
			element.ifPresent(action);
			release();
		}

		@Override
		public int collectAndCount(Collection<? super Long> collection)
		{
			if (element.isPresent()) {
				collection.add(element.getAsLong());
				release();
				return 1;
			}
			return 0;
		}

		@Override
		public int collectAndCount(Long[] array, int offset)
		{
			if (element.isPresent()) {
				array[offset] = element.getAsLong();
				release();
				return 1;
			}
			return 0;
		}

		@Override
		public int collectAndCount(long[] array, int offset)
		{
			if (element.isPresent()) {
				array[offset] = element.getAsLong();
				release();
				return 1;
			}
			return 0;
		}

		@Override
		public boolean contains(long l)
		{
			return element.isPresent() && element.getAsLong() == l;
		}

		@Override
		public int count()
		{
			return element.isPresent() ? 1 : 0;
		}

		@Override
		public OptionalLong max()
		{
			return element;

		}

		@Override
		public OptionalLong min()
		{
			return element;
		}

		@Override
		public <T> T reduce(T identity, BiFunction<T, ? super Long, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = element.isPresent() ? accumulator.apply(identity, element.getAsLong()) : identity;
			release();
			return result;
		}

		@Override
		public <T> T reduce(T identity, ObjLongFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = element.isPresent() ? accumulator.apply(identity, element.getAsLong()) : identity;
			release();
			return result;
		}


		@Override
		public long reduce(long identity, LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			long result = element.isPresent() ? accumulator.applyAsLong(identity, element.getAsLong()) : identity;
			release();
			return result;
		}

		@Override
		public long sum()
		{
			return element.orElse(0);
		}

		@Override
		protected void release()
		{
			element = OptionalLong.empty();
		}
	}
}