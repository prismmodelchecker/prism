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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfDouble;
import java.util.PrimitiveIterator.OfInt;
import java.util.PrimitiveIterator.OfLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import common.functions.ObjDoubleFunction;
import common.functions.ObjIntFunction;
import common.functions.ObjLongFunction;

/**
 * Helpers for mapping Iterators to another Iterator, performing some mapping on the elements
 */
public abstract class MappingIterator<S, E, I extends Iterator<S>> implements FunctionalIterator<E>
{
	protected I iterator;

	public MappingIterator(I iterator)
	{
		Objects.requireNonNull(iterator);
		this.iterator = iterator;
	}

	@Override
	public boolean hasNext()
	{
		if (iterator.hasNext()) {
			return true;
		}
		release();
		return false;
	}

	@Override
	public void remove()
	{
		iterator.remove();
	}

	protected abstract void release();

	protected void requireNext()
	{
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
	}

	public static FunctionalPrimitiveIterator.OfDouble toDouble(Iterator<Double> iterator)
	{
		if (iterator instanceof PrimitiveIterator.OfDouble) {
			return FunctionalIterator.extend((PrimitiveIterator.OfDouble) iterator);
		}
		return new ToDouble<>(iterator, Double::doubleValue);
	}

	public static FunctionalPrimitiveIterator.OfInt toInt(Iterator<Integer> iterator)
	{
		if (iterator instanceof PrimitiveIterator.OfInt) {
			return FunctionalIterator.extend((PrimitiveIterator.OfInt) iterator);
		}
		return new ToInt<>(iterator, Integer::intValue);
	}

	public static FunctionalPrimitiveIterator.OfLong toLong(Iterator<Long> iterator)
	{
		if (iterator instanceof PrimitiveIterator.OfLong) {
			return FunctionalIterator.extend((PrimitiveIterator.OfLong) iterator);
		}
		return new ToLong<>(iterator, Long::longValue);
	}



	/**
	 * Map an Iterator<S> to an Iterator<E> using the given mapping function.
	 */
	public static class From<S, E> extends MappingIterator<S, E, Iterator<S>>
	{
		protected final Function<? super S, ? extends E> function;

		public From(Iterator<S> iterator, Function<? super S, ? extends E> function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public E next()
		{
			requireNext();
			return function.apply(iterator.next());
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action)
		{
			Objects.requireNonNull(action);
			iterator.forEachRemaining(each -> action.accept(function.apply(each)));
			release();
		}

		@Override
		public int count()
		{
			if (iterator instanceof FunctionalIterator) {
				return ((FunctionalIterator<?>) iterator).count();
			}
			// do not use reduce to avoid auto-boxing of count variable
			// exploit arithmetic operator
			int count = 0;
			while (iterator.hasNext()) {
				iterator.next();
				count++;
			}
			release();
			return count;
		}

		@Override
		public <T> T reduce(T identity, BiFunction<T, ? super E, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			if (iterator instanceof FunctionalIterator) {
				result = ((FunctionalIterator<S>) iterator).reduce(result, (r, e) -> accumulator.apply(r, function.apply(e)));
			} else {
				while (iterator.hasNext()) {
					result = accumulator.apply(result, function.apply(iterator.next()));
				}
			}
			release();
			return result;
		}

		@Override
		protected void release()
		{
			iterator = EmptyIterator.Of();
		}
	}



	/**
	 * Map an Iterator<E> to an FunctionalPrimitiveIterator.OfDouble using the given mapping function.
	 */
	public static class ToDouble<E> extends MappingIterator<E, Double, Iterator<E>> implements FunctionalPrimitiveIterator.OfDouble
	{
		protected ToDoubleFunction<? super E> function;

		public ToDouble(Iterator<E> iterator, ToDoubleFunction<? super E> function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public double nextDouble()
		{
			requireNext();
			return function.applyAsDouble(iterator.next());
		}

		@Override
		public void forEachRemaining(DoubleConsumer action)
		{
			Objects.requireNonNull(action);
			iterator.forEachRemaining(each -> action.accept(function.applyAsDouble(each)));
			release();
		}

		@Override
		public boolean contains(double d)
		{
			while (iterator.hasNext()) {
				if (function.applyAsDouble(iterator.next()) == d) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int count()
		{
			if (iterator instanceof FunctionalIterator) {
				return ((FunctionalIterator<?>) iterator).count();
			}
			// do not use reduce to avoid auto-boxing of count variable
			// exploit arithmetic operator
			int count = 0;
			while (iterator.hasNext()) {
				iterator.next();
				count++;
			}
			release();
			return count;
		}

		@Override
		public <T> T reduce(T identity, ObjDoubleFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (iterator.hasNext()) {
				result = accumulator.apply(result, function.applyAsDouble(iterator.next()));
			}
			release();
			return result;
		}

		@Override
		public double reduce(double identity, DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			double result = identity;
			while (iterator.hasNext()) {
				result = accumulator.applyAsDouble(result, function.applyAsDouble(iterator.next()));
			}
			release();
			return result;
		}

		@Override
		protected void release()
		{
			iterator = EmptyIterator.Of();
		}
	}


	/**
	 * Map an Iterator<E> to an FunctionalPrimitiveIterator.OfInt using the given mapping function.
	 */
	public static class ToInt<E> extends MappingIterator<E, Integer, Iterator<E>> implements FunctionalPrimitiveIterator.OfInt
	{
		protected ToIntFunction<? super E> function;

		public ToInt(Iterator<E> iterator, ToIntFunction<? super E> function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public int nextInt()
		{
			requireNext();
			return function.applyAsInt(iterator.next());
		}

		@Override
		public void forEachRemaining(IntConsumer action)
		{
			Objects.requireNonNull(action);
			iterator.forEachRemaining(each -> action.accept(function.applyAsInt(each)));
			release();
		}

		@Override
		public boolean contains(int i)
		{
			while (iterator.hasNext()) {
				if (function.applyAsInt(iterator.next()) == i) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int count()
		{
			if (iterator instanceof FunctionalIterator) {
				return ((FunctionalIterator<?>) iterator).count();
			}
			// do not use reduce to avoid auto-boxing of count variable
			// exploit arithmetic operator
			int count = 0;
			while (iterator.hasNext()) {
				iterator.next();
				count++;
			}
			release();
			return count;
		}

		@Override
		public <T> T reduce(T identity, ObjIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (iterator.hasNext()) {
				result = accumulator.apply(result, function.applyAsInt(iterator.next()));
			}
			release();
			return result;
		}

		@Override
		public int reduce(int identity, IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			int result = identity;
			while (iterator.hasNext()) {
				result = accumulator.applyAsInt(result, function.applyAsInt(iterator.next()));
			}
			release();
			return result;
		}

		@Override
		protected void release()
		{
			iterator = EmptyIterator.Of();
		}
	}



	/**
	 * Map an Iterator<E> to an FunctionalPrimitiveIterator.OfLong using the given mapping function.
	 */
	public static class ToLong<E> extends MappingIterator<E, Long, Iterator<E>> implements FunctionalPrimitiveIterator.OfLong
	{
		protected ToLongFunction<? super E> function;

		public ToLong(Iterator<E> iterator, ToLongFunction<? super E> function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public long nextLong()
		{
			requireNext();
			return function.applyAsLong(iterator.next());
		}

		@Override
		public void forEachRemaining(LongConsumer action)
		{
			Objects.requireNonNull(action);
			iterator.forEachRemaining(each -> action.accept(function.applyAsLong(each)));
			release();
		}

		@Override
		public boolean contains(long l)
		{
			while (iterator.hasNext()) {
				if (function.applyAsLong(iterator.next()) == l) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int count()
		{
			if (iterator instanceof FunctionalIterator) {
				return ((FunctionalIterator<?>) iterator).count();
			}
			// do not use reduce to avoid auto-boxing of count variable
			// exploit arithmetic operator
			int count = 0;
			while (iterator.hasNext()) {
				iterator.next();
				count++;
			}
			release();
			return count;
		}

		@Override
		public <T> T reduce(T identity, ObjLongFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (iterator.hasNext()) {
				result = accumulator.apply(result, function.applyAsLong(iterator.next()));
			}
			release();
			return result;
		}

		@Override
		public long reduce(long identity, LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			long result = identity;
			while (iterator.hasNext()) {
				result = accumulator.applyAsLong(result, function.applyAsLong(iterator.next()));
			}
			release();
			return result;
		}

		@Override
		protected void release()
		{
			iterator = EmptyIterator.Of();
		}
	}



	/**
	 * Map an PrimitiveIterator.OfDouble to an Iterator<E> using the given mapping function.
	 */
	public static class FromDouble<E> extends MappingIterator<Double, E, PrimitiveIterator.OfDouble>
	{
		protected DoubleFunction<? extends E> function;

		public FromDouble(OfDouble iterator, DoubleFunction<? extends E> function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public E next()
		{
			requireNext();
			return function.apply(iterator.nextDouble());
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action)
		{
			Objects.requireNonNull(action);
			iterator.forEachRemaining((double each) -> action.accept(function.apply(each)));
			release();
		}

		@Override
		public int count()
		{
			if (iterator instanceof FunctionalPrimitiveIterator) {
				return ((FunctionalPrimitiveIterator.OfDouble) iterator).count();
			}
			// do not use reduce to avoid auto-boxing of count variable
			// exploit arithmetic operator
			int count = 0;
			while (iterator.hasNext()) {
				iterator.nextDouble();
				count++;
			}
			release();
			return count;
		}

		@Override
		protected void release()
		{
			iterator = EmptyIterator.OfDouble();
		}
	}



	/**
	 * Map an PrimitiveIterator.OfDouble to an FunctionalPrimitiveIterator.OfDouble using the given mapping function.
	 */
	public static class FromDoubleToDouble extends MappingIterator<Double, Double, PrimitiveIterator.OfDouble> implements FunctionalPrimitiveIterator.OfDouble
	{
		protected DoubleUnaryOperator function;

		public FromDoubleToDouble(PrimitiveIterator.OfDouble iterator, DoubleUnaryOperator function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public double nextDouble()
		{
			requireNext();
			return function.applyAsDouble(iterator.nextDouble());
		}

		@Override
		public void forEachRemaining(DoubleConsumer action)
		{
			Objects.requireNonNull(action);
			iterator.forEachRemaining((double each) -> action.accept(function.applyAsDouble(each)));
			release();
		}

		@Override
		public boolean contains(double d)
		{
			while (iterator.hasNext()) {
				if (function.applyAsDouble(iterator.next()) == d) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int count()
		{
			if (iterator instanceof FunctionalPrimitiveIterator) {
				return ((FunctionalPrimitiveIterator.OfDouble) iterator).count();
			}
			// do not use reduce to avoid auto-boxing of count variable
			// exploit arithmetic operator
			int count = 0;
			while (iterator.hasNext()) {
				iterator.nextDouble();
				count++;
			}
			release();
			return count;
		}

		@Override
		public <T> T reduce(T identity, ObjDoubleFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (iterator.hasNext()) {
				result = accumulator.apply(result, function.applyAsDouble(iterator.nextDouble()));
			}
			release();
			return result;
		}

		@Override
		public double reduce(double identity, DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			double result = identity;
			while (iterator.hasNext()) {
				result = accumulator.applyAsDouble(result, function.applyAsDouble(iterator.nextDouble()));
			}
			release();
			return result;
		}

		@Override
		protected void release()
		{
			iterator = EmptyIterator.OfDouble();
		}
	}



	/**
	 * Map an PrimitiveIterator.OfDouble to an FunctionalPrimitiveIterator.OfInt using the given mapping function.
	 */
	public static class FromDoubleToInt extends MappingIterator<Double, Integer, PrimitiveIterator.OfDouble> implements FunctionalPrimitiveIterator.OfInt
	{
		protected DoubleToIntFunction function;

		public FromDoubleToInt(PrimitiveIterator.OfDouble iterator, DoubleToIntFunction function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public int nextInt()
		{
			requireNext();
			return function.applyAsInt(iterator.nextDouble());
		}

		@Override
		public void forEachRemaining(IntConsumer action)
		{
			Objects.requireNonNull(action);
			iterator.forEachRemaining((double each) -> action.accept(function.applyAsInt(each)));
			release();
		}

		@Override
		public boolean contains(int i)
		{
			while (iterator.hasNext()) {
				if (function.applyAsInt(iterator.next()) == i) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int count()
		{
			if (iterator instanceof FunctionalPrimitiveIterator) {
				return ((FunctionalPrimitiveIterator.OfDouble) iterator).count();
			}
			// do not use reduce to avoid auto-boxing of count variable
			// exploit arithmetic operator
			int count = 0;
			while (iterator.hasNext()) {
				iterator.nextDouble();
				count++;
			}
			release();
			return count;
		}

		@Override
		public <T> T reduce(T identity, ObjIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (iterator.hasNext()) {
				result = accumulator.apply(result, function.applyAsInt(iterator.nextDouble()));
			}
			release();
			return result;
		}

		@Override
		public int reduce(int identity, IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			int result = identity;
			while (iterator.hasNext()) {
				result = accumulator.applyAsInt(result, function.applyAsInt(iterator.nextDouble()));
			}
			release();
			return result;
		}

		@Override
		protected void release()
		{
			iterator = EmptyIterator.OfDouble();
		}
	}



	/**
	 * Map an PrimitiveIterator.OfDouble to an FunctionalPrimitiveIterator.OfLong using the given mapping function.
	 */
	public static class FromDoubleToLong extends MappingIterator<Double, Long, PrimitiveIterator.OfDouble> implements FunctionalPrimitiveIterator.OfLong
	{
		protected DoubleToLongFunction function;

		public FromDoubleToLong(PrimitiveIterator.OfDouble iterator, DoubleToLongFunction function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public long nextLong()
		{
			requireNext();
			return function.applyAsLong(iterator.nextDouble());
		}

		@Override
		public void forEachRemaining(LongConsumer action)
		{
			Objects.requireNonNull(action);
			iterator.forEachRemaining((double each) -> action.accept(function.applyAsLong(each)));
			release();
		}

		@Override
		public boolean contains(long l)
		{
			while (iterator.hasNext()) {
				if (function.applyAsLong(iterator.next()) == l) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int count()
		{
			if (iterator instanceof FunctionalPrimitiveIterator) {
				return ((FunctionalPrimitiveIterator.OfDouble) iterator).count();
			}
			// do not use reduce to avoid auto-boxing of count variable
			// exploit arithmetic operator
			int count = 0;
			while (iterator.hasNext()) {
				iterator.nextDouble();
				count++;
			}
			release();
			return count;
		}

		@Override
		public <T> T reduce(T identity, ObjLongFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (iterator.hasNext()) {
				result = accumulator.apply(result, function.applyAsLong(iterator.nextDouble()));
			}
			release();
			return result;
		}

		@Override
		public long reduce(long identity, LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			long result = identity;
			while (iterator.hasNext()) {
				result = accumulator.applyAsLong(result, function.applyAsLong(iterator.nextDouble()));
			}
			release();
			return result;
		}

		@Override
		protected void release()
		{
			iterator = EmptyIterator.OfDouble();
		}
	}



	/**
	 * Map an PrimitiveIterator.OfInt to an Iterator<E> using the given mapping function.
	 */
	public static class FromInt<E> extends MappingIterator<Integer, E, PrimitiveIterator.OfInt>
	{
		protected IntFunction<? extends E> function;

		public FromInt(OfInt iterator, IntFunction<? extends E> function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public E next()
		{
			requireNext();
			return function.apply(iterator.nextInt());
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action)
		{
			Objects.requireNonNull(action);
			iterator.forEachRemaining((int each) -> action.accept(function.apply(each)));
			release();
		}

		@Override
		public int count()
		{
			if (iterator instanceof FunctionalPrimitiveIterator) {
				return ((FunctionalPrimitiveIterator.OfInt) iterator).count();
			}
			// do not use reduce to avoid auto-boxing of count variable
			// exploit arithmetic operator
			int count = 0;
			while (iterator.hasNext()) {
				iterator.nextInt();
				count++;
			}
			release();
			return count;
		}

		@Override
		protected void release()
		{
			iterator = EmptyIterator.OfInt();
		}
	}



	/**
	 * Map an PrimitiveIterator.OfInt to an FunctionalPrimitiveIterator.OfDouble using the given mapping function.
	 */
	public static class FromIntToDouble extends MappingIterator<Integer, Double, PrimitiveIterator.OfInt> implements FunctionalPrimitiveIterator.OfDouble
	{
		protected IntToDoubleFunction function;
	
		public FromIntToDouble(PrimitiveIterator.OfInt iterator, IntToDoubleFunction function)
		{
			super(iterator);
			this.function = function;
		}
	
		@Override
		public double nextDouble()
		{
			requireNext();
			return function.applyAsDouble(iterator.nextInt());
		}

		@Override
		public void forEachRemaining(DoubleConsumer action)
		{
			Objects.requireNonNull(action);
			iterator.forEachRemaining((int each) -> action.accept(function.applyAsDouble(each)));
			release();
		}

		@Override
		public boolean contains(double d)
		{
			while (iterator.hasNext()) {
				if (function.applyAsDouble(iterator.next()) == d) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int count()
		{
			if (iterator instanceof FunctionalPrimitiveIterator) {
				return ((FunctionalPrimitiveIterator.OfInt) iterator).count();
			}
			// do not use reduce to avoid auto-boxing of count variable
			// exploit arithmetic operator
			int count = 0;
			while (iterator.hasNext()) {
				iterator.nextInt();
				count++;
			}
			release();
			return count;
		}

		@Override
		public <T> T reduce(T identity, ObjDoubleFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (iterator.hasNext()) {
				result = accumulator.apply(result, function.applyAsDouble(iterator.nextInt()));
			}
			release();
			return result;
		}

		@Override
		public double reduce(double identity, DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			double result = identity;
			while (iterator.hasNext()) {
				result = accumulator.applyAsDouble(result, function.applyAsDouble(iterator.nextInt()));
			}
			release();
			return result;
		}

		@Override
		protected void release()
		{
			iterator = EmptyIterator.OfInt();
		}
	}



	/**
	 * Map an PrimitiveIterator.OfInt to an FunctionalPrimitiveIterator.OfInt using the given mapping function.
	 */
	public static class FromIntToInt extends MappingIterator<Integer, Integer, PrimitiveIterator.OfInt> implements FunctionalPrimitiveIterator.OfInt
	{
		protected IntUnaryOperator function;

		public FromIntToInt(PrimitiveIterator.OfInt iterator, IntUnaryOperator function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public int nextInt()
		{
			requireNext();
			return function.applyAsInt(iterator.nextInt());
		}

		@Override
		public void forEachRemaining(IntConsumer action)
		{
			Objects.requireNonNull(action);
			iterator.forEachRemaining((int each) -> action.accept(function.applyAsInt(each)));
			release();
		}

		@Override
		public boolean contains(int i)
		{
			while (iterator.hasNext()) {
				if (function.applyAsInt(iterator.next()) == i) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int count()
		{
			if (iterator instanceof FunctionalPrimitiveIterator) {
				return ((FunctionalPrimitiveIterator.OfInt) iterator).count();
			}
			// do not use reduce to avoid auto-boxing of count variable
			// exploit arithmetic operator
			int count = 0;
			while (iterator.hasNext()) {
				iterator.nextInt();
				count++;
			}
			release();
			return count;
		}

		@Override
		public <T> T reduce(T identity, ObjIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (iterator.hasNext()) {
				result = accumulator.apply(result, function.applyAsInt(iterator.nextInt()));
			}
			release();
			return result;
		}

		@Override
		public int reduce(int identity, IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			int result = identity;
			while (iterator.hasNext()) {
				result = accumulator.applyAsInt(result, function.applyAsInt(iterator.nextInt()));
			}
			release();
			return result;
		}

		@Override
		protected void release()
		{
			iterator = EmptyIterator.OfInt();
		}
	}



	/**
	 * Map an PrimitiveIterator.OfInt to an FunctionalPrimitiveIterator.OfLong using the given mapping function.
	 */
	public static class FromIntToLong extends MappingIterator<Integer, Long, PrimitiveIterator.OfInt> implements FunctionalPrimitiveIterator.OfLong
	{
		protected IntToLongFunction function;

		public FromIntToLong(PrimitiveIterator.OfInt iterator, IntToLongFunction function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public long nextLong()
		{
			requireNext();
			return function.applyAsLong(iterator.nextInt());
		}

		@Override
		public void forEachRemaining(LongConsumer action)
		{
			Objects.requireNonNull(action);
			iterator.forEachRemaining((int each) -> action.accept(function.applyAsLong(each)));
			release();
		}

		@Override
		public boolean contains(long l)
		{
			while (iterator.hasNext()) {
				if (function.applyAsLong(iterator.next()) == l) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int count()
		{
			if (iterator instanceof FunctionalPrimitiveIterator) {
				return ((FunctionalPrimitiveIterator.OfInt) iterator).count();
			}
			// do not use reduce to avoid auto-boxing of count variable
			// exploit arithmetic operator
			int count = 0;
			while (iterator.hasNext()) {
				iterator.nextInt();
				count++;
			}
			release();
			return count;
		}

		@Override
		public <T> T reduce(T identity, ObjLongFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (iterator.hasNext()) {
				result = accumulator.apply(result, function.applyAsLong(iterator.nextInt()));
			}
			release();
			return result;
		}

		@Override
		public long reduce(long identity, LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			long result = identity;
			while (iterator.hasNext()) {
				result = accumulator.applyAsLong(result, function.applyAsLong(iterator.nextInt()));
			}
			release();
			return result;
		}

		@Override
		protected void release()
		{
			iterator = EmptyIterator.OfInt();
		}
	}



	/**
	 * Map an PrimitiveIterator.OfLong to an Iterator<E> using the given mapping function.
	 */
	public static class FromLong<E> extends MappingIterator<Long, E, PrimitiveIterator.OfLong>
	{
		protected LongFunction<? extends E> function;

		public FromLong(OfLong iterator, LongFunction<? extends E> function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public E next()
		{
			requireNext();
			return function.apply(iterator.nextLong());
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action)
		{
			Objects.requireNonNull(action);
			iterator.forEachRemaining((long each) -> action.accept(function.apply(each)));
			release();
		}

		@Override
		public int count()
		{
			if (iterator instanceof FunctionalPrimitiveIterator) {
				return ((FunctionalPrimitiveIterator.OfLong) iterator).count();
			}
			// do not use reduce to avoid auto-boxing of count variable
			// exploit arithmetic operator
			int count = 0;
			while (iterator.hasNext()) {
				iterator.nextLong();
				count++;
			}
			release();
			return count;
		}

		@Override
		protected void release()
		{
			iterator = EmptyIterator.OfLong();
		}
	}



	/**
	 * Map an PrimitiveIterator.OfLong to an FunctionalPrimitiveIterator.OfDouble using the given mapping function.
	 */
	public static class FromLongToDouble extends MappingIterator<Long, Double, PrimitiveIterator.OfLong> implements FunctionalPrimitiveIterator.OfDouble
	{
		protected LongToDoubleFunction function;
	
		public FromLongToDouble(PrimitiveIterator.OfLong iterator, LongToDoubleFunction function)
		{
			super(iterator);
			this.function = function;
		}
	
		@Override
		public double nextDouble()
		{
			requireNext();
			return function.applyAsDouble(iterator.nextLong());
		}
	
		@Override
		public void forEachRemaining(DoubleConsumer action)
		{
			Objects.requireNonNull(action);
			iterator.forEachRemaining((long each) -> action.accept(function.applyAsDouble(each)));
			release();
		}
	
		@Override
		public boolean contains(double d)
		{
			while (iterator.hasNext()) {
				if (function.applyAsDouble(iterator.next()) == d) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int count()
		{
			if (iterator instanceof FunctionalPrimitiveIterator) {
				return ((FunctionalPrimitiveIterator.OfLong) iterator).count();
			}
			// do not use reduce to avoid auto-boxing of count variable
			// exploit arithmetic operator
			int count = 0;
			while (iterator.hasNext()) {
				iterator.nextLong();
				count++;
			}
			release();
			return count;
		}

		@Override
		public <T> T reduce(T identity, ObjDoubleFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (iterator.hasNext()) {
				result = accumulator.apply(result, function.applyAsDouble(iterator.nextLong()));
			}
			release();
			return result;
		}

		@Override
		public double reduce(double identity, DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			double result = identity;
			while (iterator.hasNext()) {
				result = accumulator.applyAsDouble(result, function.applyAsDouble(iterator.nextLong()));
			}
			release();
			return result;
		}

		@Override
		protected void release()
		{
			iterator = EmptyIterator.OfLong();
		}
	}



	/**
	 * Map an PrimitiveIterator.OfLong to an FunctionalPrimitiveIterator.OfInt using the given mapping function.
	 */
	public static class FromLongToInt extends MappingIterator<Long, Integer, PrimitiveIterator.OfLong> implements FunctionalPrimitiveIterator.OfInt
	{
		protected LongToIntFunction function;

		public FromLongToInt(PrimitiveIterator.OfLong iterator, LongToIntFunction function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public int nextInt()
		{
			requireNext();
			return function.applyAsInt(iterator.nextLong());
		}

		@Override
		public void forEachRemaining(IntConsumer action)
		{
			Objects.requireNonNull(action);
			iterator.forEachRemaining((long each) -> action.accept(function.applyAsInt(each)));
			release();
		}

		@Override
		public boolean contains(int i)
		{
			while (iterator.hasNext()) {
				if (function.applyAsInt(iterator.next()) == i) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int count()
		{
			if (iterator instanceof FunctionalPrimitiveIterator) {
				return ((FunctionalPrimitiveIterator.OfLong) iterator).count();
			}
			// do not use reduce to avoid auto-boxing of count variable
			// exploit arithmetic operator
			int count = 0;
			while (iterator.hasNext()) {
				iterator.nextLong();
				count++;
			}
			release();
			return count;
		}

		@Override
		public <T> T reduce(T identity, ObjIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (iterator.hasNext()) {
				result = accumulator.apply(result, function.applyAsInt(iterator.nextLong()));
			}
			release();
			return result;
		}

		@Override
		public int reduce(int identity, IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			int result = identity;
			while (iterator.hasNext()) {
				result = accumulator.applyAsInt(result, function.applyAsInt(iterator.nextLong()));
			}
			release();
			return result;
		}

		@Override
		protected void release()
		{
			iterator = EmptyIterator.OfLong();
		}
	}



	/**
	 * Map an PrimitiveIterator.OfLong to an FunctionalPrimitiveIterator.OfLong using the given mapping function.
	 */
	public static class FromLongToLong extends MappingIterator<Long, Long, PrimitiveIterator.OfLong> implements FunctionalPrimitiveIterator.OfLong
	{
		protected LongUnaryOperator function;

		public FromLongToLong(PrimitiveIterator.OfLong iterator, LongUnaryOperator function)
		{
			super(iterator);
			this.function = function;
		}

		@Override
		public long nextLong()
		{
			requireNext();
			return function.applyAsLong(iterator.nextLong());
		}

		@Override
		public void forEachRemaining(LongConsumer action)
		{
			Objects.requireNonNull(action);
			iterator.forEachRemaining((long each) -> action.accept(function.applyAsLong(each)));
			release();
		}

		@Override
		public boolean contains(long l)
		{
			while (iterator.hasNext()) {
				if (function.applyAsLong(iterator.next()) == l) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int count()
		{
			if (iterator instanceof FunctionalPrimitiveIterator) {
				return ((FunctionalPrimitiveIterator.OfLong) iterator).count();
			}
			// do not use reduce to avoid auto-boxing of count variable
			// exploit arithmetic operator
			int count = 0;
			while (iterator.hasNext()) {
				iterator.nextLong();
				count++;
			}
			release();
			return count;
		}

		@Override
		public <T> T reduce(T identity, ObjLongFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			while (iterator.hasNext()) {
				result = accumulator.apply(result, function.applyAsLong(iterator.nextLong()));
			}
			release();
			return result;
		}

		@Override
		public long reduce(long identity, LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			long result = identity;
			while (iterator.hasNext()) {
				result = accumulator.applyAsLong(result, function.applyAsLong(iterator.nextLong()));
			}
			release();
			return result;
		}

		@Override
		protected void release()
		{
			iterator = EmptyIterator.OfLong();
		}
	}
}