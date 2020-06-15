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

import java.util.BitSet;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;

import common.iterable.FilteringIterable.DistinctIterable;

public interface FunctionalPrimitiveIterable<E, E_CONS> extends FunctionalIterable<E>
{
	@Override
	public FunctionalPrimitiveIterator<E, E_CONS> iterator();

	@Override
	default boolean isEmpty()
	{
		return !iterator().hasNext();
	}

	default FunctionalIterable<E> boxed()
	{
		return this;
	}

	default void forEach(E_CONS action)
	{
		iterator().forEachRemaining(action);
	}

	default FunctionalPrimitiveIterable<E, E_CONS> nonNull()
	{
		return this;
	}


	/**
	 * Iterable for a PrimitiveIterator.OfDouble
	 */
	public interface IterableDouble extends Iterable<Double>, FunctionalPrimitiveIterable<Double, DoubleConsumer>
	{
		@Override
		FunctionalPrimitiveIterator.OfDouble iterator();

		// Transforming Methods

		default IterableDouble concat(IterableDouble... iterables)
		{
			switch (iterables.length) {
			case 0:
				return this;
			case 1:
				return new ChainedIterable.OfDouble(this, iterables[0]);
			default:
				return new ChainedIterable.OfDouble(this, new ChainedIterable.OfDouble(iterables));
			}
		}

		@Override
		default IterableDouble distinct()
		{
			return new DistinctIterable.Of<>(this).mapToDouble(Double::doubleValue);
		}

		@Override
		default IterableDouble dedupe()
		{
			return new IterableDouble()
			{
				@Override
				public FunctionalPrimitiveIterator.OfDouble iterator()
				{
					return IterableDouble.this.iterator().dedupe();
				}
			};
		}

		default IterableDouble filter(DoublePredicate predicate)
		{
			return new FilteringIterable.OfDouble(this, predicate);
		}

		default <T> FunctionalIterable<T> flatMap(DoubleFunction<? extends Iterable<? extends T>> function)
		{
			return new ChainedIterable.Of<>(map(function));
		}

		default <T> IterableDouble flatMapToDouble(DoubleFunction<IterableDouble> function)
		{
			return new ChainedIterable.OfDouble(map(function));
		}

		default <T> IterableInt flatMapToInt(DoubleFunction<IterableInt> function)
		{
			return new ChainedIterable.OfInt(map(function));
		}

		default <T> IterableLong flatMapToLong(DoubleFunction<IterableLong> function)
		{
			return new ChainedIterable.OfLong(map(function));
		}

		default <T> FunctionalIterable<T> map(DoubleFunction<? extends T> function)
		{
			return new MappingIterable.FromDouble<>(this, function);
		}

		default IterableDouble mapToDouble(DoubleUnaryOperator function)
		{
			return new MappingIterable.FromDoubleToDouble(this, function);
		}

		default IterableInt mapToInt(DoubleToIntFunction function)
		{
			return new MappingIterable.FromDoubleToInt(this, function);
		}

		default IterableLong mapToLong(DoubleToLongFunction function)
		{
			return new MappingIterable.FromDoubleToLong(this, function);
		}

		default IterableDouble nonNull()
		{
			return this;
		}



		// Accumulations Methods (Consuming)

		default boolean allMatch(DoublePredicate predicate)
		{
			return iterator().allMatch(predicate);
		}

		default boolean anyMatch(DoublePredicate predicate)
		{
			return iterator().anyMatch(predicate);
		}

		default double[] collect(double[] array)
		{
			return iterator().collect(array);
		}

		default double[] collect(double[] array, int offset)
		{
			return iterator().collect(array, offset);
		}

		default int collectAndCount(double[] array)
		{
			return iterator().collectAndCount(array);
		}

		default int collectAndCount(double[] array, int offset)
		{
			return iterator().collectAndCount(array, offset);
		}

		default boolean contains(double d)
		{
			return iterator().contains(d);
		}

		default OptionalDouble detect(DoublePredicate predicate)
		{
			return iterator().detect(predicate);
		}

		default OptionalDouble max()
		{
			return iterator().max();
		}

		default OptionalDouble min()
		{
			return iterator().min();
		}

		default OptionalDouble reduce(DoubleBinaryOperator accumulator)
		{
			return iterator().reduce(accumulator);
		}

		default double reduce(double identity, DoubleBinaryOperator accumulator)
		{
			return iterator().reduce(identity, accumulator);
		}

		default double sum()
		{
			return iterator().sum();
		}
	}



	/**
	 * Iterable for a PrimitiveIterator.OfInt
	 */
	public interface IterableInt extends Iterable<Integer>, FunctionalPrimitiveIterable<Integer, IntConsumer>
	{
		@Override
		public FunctionalPrimitiveIterator.OfInt iterator();

		// Transforming Methods

		default IterableInt concat(IterableInt... iterables)
		{
			switch (iterables.length) {
			case 0:
				return this;
			case 1:
				return new ChainedIterable.OfInt(this, iterables[0]);
			default:
				return new ChainedIterable.OfInt(this, new ChainedIterable.OfInt(iterables));
			}
		}

		@Override
		default IterableInt distinct()
		{
			return new DistinctIterable.OfInt(this);
		}

		@Override
		default IterableInt dedupe()
		{
			return new IterableInt()
			{
				@Override
				public FunctionalPrimitiveIterator.OfInt iterator()
				{
					return IterableInt.this.iterator().dedupe();
				}
			};
		}

		default IterableInt filter(IntPredicate predicate)
		{
			return new FilteringIterable.OfInt(this, predicate);
		}

		default <T> FunctionalIterable<T> flatMap(IntFunction<? extends Iterable<? extends T>> function)
		{
			return new ChainedIterable.Of<>(map(function));
		}

		default <T> IterableDouble flatMapToDouble(IntFunction<IterableDouble> function)
		{
			return new ChainedIterable.OfDouble(map(function));
		}

		default <T> IterableInt flatMapToInt(IntFunction<IterableInt> function)
		{
			return new ChainedIterable.OfInt(map(function));
		}

		default <T> IterableLong flatMapToLong(IntFunction<IterableLong> function)
		{
			return new ChainedIterable.OfLong(map(function));
		}

		default <T> FunctionalIterable<T> map(IntFunction<? extends T> function)
		{
			return new MappingIterable.FromInt<>(this, function);
		}

		default IterableDouble mapToDouble(IntToDoubleFunction function)
		{
			return new MappingIterable.FromIntToDouble(this, function);
		}

		default IterableInt mapToInt(IntUnaryOperator function)
		{
			return new MappingIterable.FromIntToInt(this, function);
		}

		default IterableLong mapToLong(IntToLongFunction function)
		{
			return new MappingIterable.FromIntToLong(this, function);
		}

		default IterableInt nonNull()
		{
			return this;
		}


		// Accumulations Methods (Consuming)

		default boolean allMatch(IntPredicate predicate)
		{
			return iterator().allMatch(predicate);
		}

		default boolean anyMatch(IntPredicate predicate)
		{
			return iterator().anyMatch(predicate);
		}

		default int[] collect(int[] array)
		{
			return iterator().collect(array);
		}

		default int[] collect(int[] array, int offset)
		{
			return iterator().collect(array, offset);
		}

		default BitSet collect(BitSet indices)
		{
			return iterator().collect(indices);
		}

		default int collectAndCount(int[] array)
		{
			return iterator().collectAndCount(array);
		}

		default int collectAndCount(int[] array, int offset)
		{
			return iterator().collectAndCount(array, offset);
		}

		default int collectAndCount(BitSet indices)
		{
			return iterator().collectAndCount(indices);
		}

		default boolean contains(int i)
		{
			return iterator().contains(i);
		}

		default OptionalInt detect(IntPredicate predicate)
		{
			return iterator().detect(predicate);
		}

		default OptionalInt max()
		{
			return iterator().max();
		}

		default OptionalInt min()
		{
			return iterator().min();
		}

		default OptionalInt reduce(IntBinaryOperator accumulator)
		{
			return iterator().reduce(accumulator);
		}

		default int reduce(int identity, IntBinaryOperator accumulator)
		{
			return iterator().reduce(identity, accumulator);
		}

		default int sum()
		{
			return iterator().sum();
		}
	}



	/**
	 * Iterable for a PrimitiveIterator.OfLong
	 */
	public interface IterableLong extends Iterable<Long>, FunctionalPrimitiveIterable<Long, LongConsumer>
	{
		@Override
		public FunctionalPrimitiveIterator.OfLong iterator();

		// Transforming Methods

		default IterableLong concat(IterableLong... iterables)
		{
			switch (iterables.length) {
			case 0:
				return this;
			case 1:
				return new ChainedIterable.OfLong(this, iterables[0]);
			default:
				return new ChainedIterable.OfLong(this, new ChainedIterable.OfLong(iterables));
			}
		}

		@Override
		default IterableLong distinct()
		{
			return new DistinctIterable.Of<>(this).mapToLong(Long::longValue);
		}

		@Override
		default IterableLong dedupe()
		{
			return new IterableLong()
			{
				@Override
				public FunctionalPrimitiveIterator.OfLong iterator()
				{
					return IterableLong.this.iterator().dedupe();
				}
			};
		}

		default IterableLong filter(LongPredicate predicate)
		{
			return new FilteringIterable.OfLong(this, predicate);
		}

		default <T> FunctionalIterable<T> flatMap(LongFunction<? extends Iterable<? extends T>> function)
		{
			return new ChainedIterable.Of<>(map(function));
		}

		default <T> IterableDouble flatMapToDouble(LongFunction<IterableDouble> function)
		{
			return new ChainedIterable.OfDouble(map(function));
		}

		default <T> IterableInt flatMapToInt(LongFunction<IterableInt> function)
		{
			return new ChainedIterable.OfInt(map(function));
		}

		default <T> IterableLong flatMapToLong(LongFunction<IterableLong> function)
		{
			return new ChainedIterable.OfLong(map(function));
		}

		default <T> FunctionalIterable<T> map(LongFunction<? extends T> function)
		{
			return new MappingIterable.FromLong<>(this, function);
		}

		default IterableDouble mapToDouble(LongToDoubleFunction function)
		{
			return new MappingIterable.FromLongToDouble(this, function);
		}

		default IterableInt mapToInt(LongToIntFunction function)
		{
			return new MappingIterable.FromLongToInt(this, function);
		}

		default IterableLong mapToLong(LongUnaryOperator function)
		{
			return new MappingIterable.FromLongToLong(this, function);
		}

		default IterableLong nonNull()
		{
			return this;
		}



		// Accumulations Methods (Consuming)

		default boolean allMatch(LongPredicate predicate)
		{
			return iterator().allMatch(predicate);
		}

		default boolean anyMatch(LongPredicate predicate)
		{
			return iterator().anyMatch(predicate);
		}

		default long[] collect(long[] array)
		{
			return iterator().collect(array);
		}

		default long[] collect(long[] array, int offset)
		{
			return iterator().collect(array, offset);
		}

		default int collectAndCount(long[] array)
		{
			return iterator().collectAndCount(array);
		}

		default int collectAndCount(long[] array, int offset)
		{
			return iterator().collectAndCount(array, offset);
		}

		default boolean contains(long l)
		{
			return iterator().contains(l);
		}

		default OptionalLong detect(LongPredicate predicate)
		{
			return iterator().detect(predicate);
		}

		default OptionalLong max()
		{
			return iterator().max();
		}

		default OptionalLong min()
		{
			return iterator().min();
		}

		default OptionalLong reduce(LongBinaryOperator accumulator)
		{
			return iterator().reduce(accumulator);
		}

		default long reduce(long identity, LongBinaryOperator accumulator)
		{
			return iterator().reduce(identity, accumulator);
		}

		default long sum()
		{
			return iterator().sum();
		}
	}
}
