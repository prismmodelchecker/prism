//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <steffen.maercker@tu-dresden.de> (TU Dresden)
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

import common.functions.DoubleLongToDoubleFunction;
import common.functions.IntDoubleToIntFunction;
import common.functions.IntLongToIntFunction;
import common.functions.LongDoubleToLongFunction;
import common.functions.ObjDoubleFunction;
import common.functions.ObjIntFunction;
import common.functions.ObjLongFunction;

/**
 * A base interface for primitive specializations of {@code FunctionalIterable}.
 * Specialized sub interfaces are provided for the types {@link OfInt int}, {@link OfLong long} and {@link OfDouble double}.
 *
 * @param <E> the type of elements hold by this PrimitiveIterable
 * @param <E_CONS> the type of primitive consumer
 *
 * @see FunctionalIterable
 * @see FunctionalPrimitiveIterator
 */
public interface FunctionalPrimitiveIterable<E, E_CONS> extends FunctionalIterable<E>, PrimitiveIterable<E, E_CONS>
{
	@Override
	FunctionalPrimitiveIterator<E, E_CONS> iterator();



	/**
	 * Specialisation for {@code double} of a FunctionalPrimitiveIterable.
	 */
	interface OfDouble extends FunctionalPrimitiveIterable<Double, DoubleConsumer>, PrimitiveIterable.OfDouble
	{
		@Override
		FunctionalPrimitiveIterator.OfDouble iterator();



		// Transforming Methods

		@SuppressWarnings("unchecked")
		@Override
		default FunctionalIterable<Double> concat(Iterable<? extends Double>... iterables)
		{
			if (iterables instanceof PrimitiveIterable.OfDouble[]) {
				return concat((PrimitiveIterable.OfDouble[]) iterables);
			}
			return FunctionalPrimitiveIterable.super.concat(iterables);
		}

		/**
		 * Primitive specialisation of {@code concat} for {@code double}.
		 *
		 * @param iterables an array of {@link OfDouble}s to append
		 */
		default FunctionalPrimitiveIterable.OfDouble concat(PrimitiveIterable.OfDouble... iterables)
		{
			switch (iterables.length) {
			case 0:
				return this;
			case 1:
				return new ChainedIterable.OfDouble(new IterableArray.Of<>(this, iterables[0]));
			default:
				PrimitiveIterable.OfDouble tail = new ChainedIterable.OfDouble(new IterableArray.Of<>(iterables));
				return new ChainedIterable.OfDouble(new IterableArray.Of(this, tail));
			}
		}

		/**
		 * Remove duplicate {@code double}s from the receiver such that only one occurrence is retained.
		 * Duplicates are identified in terms of {@link ==} except for {@code NaN} for which all instances are consider equal, although {@code Double.NaN != Double.NaN}.
		 */
		@Override
		default FunctionalPrimitiveIterable.OfDouble distinct()
		{
			return filter(new Distinct.OfDouble());
		}

		/**
		 * Remove consecutive duplicate {@code double}s such that only the first occurrence in a sequence is retained.
		 * Duplicates are identified in terms of {@link ==} except for {@code NaN} for which all instances are consider equal, although {@code Double.NaN != Double.NaN}.
		 */
		@Override
		default FunctionalPrimitiveIterable.OfDouble dedupe()
		{
			return new FunctionalPrimitiveIterable.OfDouble()
			{
				@Override
				public FunctionalPrimitiveIterator.OfDouble iterator()
				{
					return FunctionalPrimitiveIterable.OfDouble.this.iterator().dedupe();
				}
			};
		}

		/**
		 * Primitive specialisation of {@code filter} for {@code double}.
		 *
		 * @see FunctionalIterable#filter(java.util.function.Predicate)
		 */
		default FunctionalPrimitiveIterable.OfDouble filter(DoublePredicate predicate)
		{
			return new FilteringIterable.OfDouble(this, predicate);
		}

		/**
		 * Primitive specialisation of {@code flatMap} for {@code double}.
		 *
		 * @see FunctionalIterable#flatMap(java.util.function.Function)
		 */
		default <T> FunctionalIterable<T> flatMap(DoubleFunction<? extends Iterable<? extends T>> function)
		{
			return new ChainedIterable.Of<>(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToDouble} for {@code double}.
		 *
		 * @see FunctionalIterable#flatMapToDouble(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterable.OfDouble flatMapToDouble(DoubleFunction<PrimitiveIterable.OfDouble> function)
		{
			return new ChainedIterable.OfDouble(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToInt} for {@code double}.
		 *
		 * @see FunctionalIterable#flatMapToInt(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterable.OfInt flatMapToInt(DoubleFunction<PrimitiveIterable.OfInt> function)
		{
			return new ChainedIterable.OfInt(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToLong} for {@code double}.
		 *
		 * @see FunctionalIterable#flatMapToLong(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterable.OfLong flatMapToLong(DoubleFunction<PrimitiveIterable.OfLong> function)
		{
			return new ChainedIterable.OfLong(map(function));
		}

		/**
		 * Primitive specialisation of {@code map} for {@code double}.
		 *
		 * @see FunctionalIterable#map(java.util.function.Function)
		 */
		default <T> FunctionalIterable<T> map(DoubleFunction<? extends T> function)
		{
			return new MappingIterable.DoubleToObj<>(this, function);
		}

		/**
		 * Primitive specialisation of {@code mapToDouble} for {@code double}.
		 *
		 * @see FunctionalIterable#mapToDouble(java.util.function.ToDoubleFunction)
		 */
		default FunctionalPrimitiveIterable.OfDouble mapToDouble(DoubleUnaryOperator function)
		{
			return new MappingIterable.DoubleToDouble(this, function);
		}

		/**
		 * Primitive specialisation of {@code mapToInt} for {@code double}.
		 *
		 * @see FunctionalIterable#mapToInt(java.util.function.ToIntFunction)
		 */
		default FunctionalPrimitiveIterable.OfInt mapToInt(DoubleToIntFunction function)
		{
			return new MappingIterable.DoubleToInt(this, function);
		}

		/**
		 * Primitive specialisation of {@code mapToLong} for {@code double}.
		 *
		 * @see FunctionalIterable#mapToLong(java.util.function.ToLongFunction)
		 */
		default FunctionalPrimitiveIterable.OfLong mapToLong(DoubleToLongFunction function)
		{
			return new MappingIterable.DoubleToLong(this, function);
		}

		@Override
		default FunctionalPrimitiveIterable.OfDouble nonNull()
		{
			return this;
		}



		// Accumulations Methods (Consuming)

		/**
		 * Primitive specialisation of {@code allMatch} for {@code double}.
		 *
		 * @see FunctionalIterable#allMatch(java.util.function.Predicate)
		 */
		default boolean allMatch(DoublePredicate predicate)
		{
			return iterator().allMatch(predicate);
		}

		/**
		 * Primitive specialisation of {@code anyMatch} for {@code double}.
		 *
		 * @see FunctionalIterable#anyMatch(java.util.function.Predicate)
		 */
		default boolean anyMatch(DoublePredicate predicate)
		{
			return iterator().anyMatch(predicate);
		}

		/**
		 * Primitive specialisation of {@code noneMatch} for {@code double}.
		 *
		 * @see FunctionalIterable#noneMatch(java.util.function.Predicate)
		 */
		default boolean noneMatch(DoublePredicate predicate)
		{
			return iterator().noneMatch(predicate);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code double}.
		 *
		 * @see FunctionalIterable#collect(Object[])
		 */
		default double[] collect(double[] array)
		{
			return iterator().collect(array);
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code double}.
		 *
		 * @see FunctionalIterable#collectAndCount(Object[])
		 */
		default double[] collect(double[] array, int offset)
		{
			return iterator().collect(array, offset);
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code double}.
		 *
		 * @see FunctionalIterable#collectAndCount(Object[])
		 */
		default int collectAndCount(double[] array)
		{
			return iterator().collectAndCount(array);
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code double}.
		 *
		 * @see FunctionalIterable#collectAndCount(Object[], int)
		 */
		default int collectAndCount(double[] array, int offset)
		{
			return iterator().collectAndCount(array, offset);
		}

		@Override
		default FunctionalPrimitiveIterable.OfDouble collectDistinct()
		{
			return iterator().collectDistinct();
		}

		/**
		 * Primitive specialisation of {@code contains} for {@code double}.
		 * Inclusion is defined in terms of {@link ==} except for {@code NaN} for which all instances are consider equal, although {@code Double.NaN != Double.NaN}.
		 *
		 * @see FunctionalIterable#contains(Object)
		 */
		default boolean contains(double d)
		{
			return iterator().contains(d);
		}

		/**
		 * Primitive specialisation of {@code count} for {@code double}.
		 *
		 * @see FunctionalIterable#count(java.util.function.Predicate)
		 */
		default long count(DoublePredicate predicate)
		{
			return filter(predicate).count();
		}

		/**
		 * Primitive specialisation of {@code detect} for {@code double}.
		 *
		 * @see FunctionalIterable#detect(java.util.function.Predicate)
		 */
		default double detect(DoublePredicate predicate)
		{
			return iterator().detect(predicate);
		}

		/**
		 * Find the maximum.
		 *
		 * @return an {@link OptionalDouble} either containing the maximum or being empty if the receiver is empty
		 * @see {@link Math#max(double, double)}.
		 */
		default OptionalDouble max()
		{
			return iterator().max();
		}

		/**
		 * Find the minimum.
		 *
		 * @return an {@link OptionalDouble} either containing the minimum or being empty if the receiver is empty
		 * @see {@link Math#min(double, double)}.
		 */
		default OptionalDouble min()
		{
			return iterator().min();
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 *
		 * @see FunctionalIterable#reduce(java.util.function.BinaryOperator)
		 */
		default OptionalDouble reduce(DoubleBinaryOperator accumulator)
		{
			return iterator().reduce(accumulator);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 *
		 * @see FunctionalIterable#reduce(Object, java.util.function.BiFunction)
		 */
		default <T> T reduce(T init, ObjDoubleFunction<T, T> accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 *
		 * @see FunctionalIterable#reduce(double, common.functions.DoubleObjToDoubleFunction)
		 */
		default double reduce(double init, DoubleBinaryOperator accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 *
		 * @see FunctionalIterable#reduce(int, common.functions.IntObjToIntFunction)
		 */
		default int reduce(int init, IntDoubleToIntFunction accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 *
		 * @see FunctionalIterable#reduce(long, common.functions.LongObjToLongFunction)
		 */
		default long reduce(long init, LongDoubleToLongFunction accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		/**
		 * Compute the sum over all elements of the receiver.
		 *
		 * @return the sum over all elements or 0 if the receiver is empty
		 */
		default double sum()
		{
			return iterator().sum();
		}
	}



	/**
	 * Specialisation for {@code int} of a FunctionalPrimitiveIterable.
	 */
	interface OfInt extends FunctionalPrimitiveIterable<Integer, IntConsumer>, PrimitiveIterable.OfInt
	{
		@Override
		FunctionalPrimitiveIterator.OfInt iterator();



		// Transforming Methods

		@SuppressWarnings("unchecked")
		@Override
		default FunctionalIterable<Integer> concat(Iterable<? extends Integer>... iterables)
		{
			if (iterables instanceof PrimitiveIterable.OfInt[]) {
				return concat((PrimitiveIterable.OfInt[]) iterables);
			}
			return FunctionalPrimitiveIterable.super.concat(iterables);
		}

		/**
		 * Primitive specialisation of {@code concat} for {@code int}.
		 *
		 * @param iterables an array of {@link OfInt}s to append
		 */
		default FunctionalPrimitiveIterable.OfInt concat(PrimitiveIterable.OfInt... iterables)
		{
			switch (iterables.length) {
			case 0:
				return this;
			case 1:
				return new ChainedIterable.OfInt(new IterableArray.Of(this, iterables[0]));
			default:
				PrimitiveIterable.OfInt tail = new ChainedIterable.OfInt(new IterableArray.Of<>(iterables));
				return new ChainedIterable.OfInt(new IterableArray.Of(this, tail));
			}
		}

		/**
		 * Remove duplicate {@code int}s from the receiver such that only one occurrence is retained.
		 * Duplicates are identified in terms of {@code ==}.
		 */
		@Override
		default FunctionalPrimitiveIterable.OfInt distinct()
		{
			return filter(new Distinct.OfInt());
		}

		/**
		 * Remove consecutive duplicate {@code int}s such that only the first occurrence in a sequence is retained.
		 * Duplicates are identified in terms of {@link ==}.
		 */
		@Override
		default FunctionalPrimitiveIterable.OfInt dedupe()
		{
			return new FunctionalPrimitiveIterable.OfInt()
			{
				@Override
				public FunctionalPrimitiveIterator.OfInt iterator()
				{
					return FunctionalPrimitiveIterable.OfInt.this.iterator().dedupe();
				}
			};
		}

		/**
		 * Primitive specialisation of {@code filter} for {@code int}.
		 *
		 * @see FunctionalIterable#filter(java.util.function.Predicate)
		 */
		default FunctionalPrimitiveIterable.OfInt filter(IntPredicate predicate)
		{
			return new FilteringIterable.OfInt(this, predicate);
		}

		/**
		 * Primitive specialisation of {@code flatMap} for {@code int}.
		 *
		 * @see FunctionalIterable#flatMap(java.util.function.Function)
		 */
		default <T> FunctionalIterable<T> flatMap(IntFunction<? extends Iterable<? extends T>> function)
		{
			return new ChainedIterable.Of<>(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToDouble} for {@code int}.
		 *
		 * @see FunctionalIterable#flatMapToDouble(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterable.OfDouble flatMapToDouble(IntFunction<PrimitiveIterable.OfDouble> function)
		{
			return new ChainedIterable.OfDouble(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToInt} for {@code int}.
		 *
		 * @see FunctionalIterable#flatMapToInt(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterable.OfInt flatMapToInt(IntFunction<PrimitiveIterable.OfInt> function)
		{
			return new ChainedIterable.OfInt(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToLong} for {@code int}.
		 *
		 * @see FunctionalIterable#flatMapToLong(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterable.OfLong flatMapToLong(IntFunction<PrimitiveIterable.OfLong> function)
		{
			return new ChainedIterable.OfLong(map(function));
		}

		/**
		 * Primitive specialisation of {@code map} for {@code int}.
		 *
		 * @see FunctionalIterable#map(java.util.function.Function)
		 */
		default <T> FunctionalIterable<T> map(IntFunction<? extends T> function)
		{
			return new MappingIterable.IntToObj<>(this, function);
		}

		/**
		 * Primitive specialisation of {@code mapToDouble} for {@code int}.
		 *
		 * @see FunctionalIterable#mapToDouble(java.util.function.IntToDoubleFunction)
		 */
		default FunctionalPrimitiveIterable.OfDouble mapToDouble(IntToDoubleFunction function)
		{
			return new MappingIterable.IntToDouble(this, function);
		}

		/**
		 * Primitive specialisation of {@code mapToInt} for {@code int}.
		 *
		 * @see FunctionalIterable#mapToInt(java.util.function.ToIntFunction)
		 */
		default FunctionalPrimitiveIterable.OfInt mapToInt(IntUnaryOperator function)
		{
			return new MappingIterable.IntToInt(this, function);
		}

		/**
		 * Primitive specialisation of {@code mapToLong} for {@code int}.
		 *
		 * @see FunctionalIterable#mapToLong(java.util.function.ToLongFunction)
		 */
		default FunctionalPrimitiveIterable.OfLong mapToLong(IntToLongFunction function)
		{
			return new MappingIterable.IntToLong(this, function);
		}

		@Override
		default FunctionalPrimitiveIterable.OfInt nonNull()
		{
			return this;
		}



		// Accumulations Methods (Consuming)

		/**
		 * Primitive specialisation of {@code allMatch} for {@code int}.
		 *
		 * @see FunctionalIterable#allMatch(java.util.function.Predicate)
		 */
		default boolean allMatch(IntPredicate predicate)
		{
			return iterator().allMatch(predicate);
		}

		/**
		 * Primitive specialisation of {@code anyMatch} for {@code int}.
		 *
		 * @see FunctionalIterable#anyMatch(java.util.function.Predicate)
		 */
		default boolean anyMatch(IntPredicate predicate)
		{
			return iterator().anyMatch(predicate);
		}

		/**
		 * Primitive specialisation of {@code noneMatch} for {@code int}.
		 *
		 * @see FunctionalIterable#noneMatch(java.util.function.Predicate)
		 */
		default boolean noneMatch(IntPredicate predicate)
		{
			return iterator().noneMatch(predicate);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code int}.
		 *
		 * @see FunctionalIterable#collect(Object[])
		 */
		default int[] collect(int[] array)
		{
			return iterator().collect(array);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code int}.
		 *
		 * @see FunctionalIterable#collect(Object[], int)
		 */
		default int[] collect(int[] array, int offset)
		{
			return iterator().collect(array, offset);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code int} storing the elements in a {@link BitSet}.
		 * This method fails if the receiver yields a negative integer.
		 *
		 * @param indices a {@link BitSet}
		 * @return the BitSet with all elements added to it
		 * @throws IndexOutOfBoundsException if an integer is negative
		 * @see FunctionalIterable#collect(java.util.Collection) FunctionalIterable.collect(Collection)
		 */
		default BitSet collect(BitSet indices)
		{
			return iterator().collect(indices);
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code int}.
		 *
		 * @see FunctionalIterable#collectAndCount(Object[])
		 */
		default int collectAndCount(int[] array)
		{
			return iterator().collectAndCount(array);
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code int}.
		 *
		 * @see FunctionalIterable#collectAndCount(Object[], int)
		 */
		default int collectAndCount(int[] array, int offset)
		{
			return iterator().collectAndCount(array, offset);
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code int} storing the elements in a {@link BitSet}.
		 * This method fails if the receiver yields a negative integer.
		 *
		 * @param indices a {@link BitSet}
		 * @return the number of elements added to the BitSet
		 * @throws IndexOutOfBoundsException if an integer is negative
		 * @see FunctionalIterable#collectAndCount(java.util.Collection) FunctionalIterable.collectAndCount(Collection)
		 */
		default int collectAndCount(BitSet indices)
		{
			return iterator().collectAndCount(indices);
		}

		@Override
		default FunctionalPrimitiveIterable.OfInt collectDistinct()
		{
			return iterator().collectDistinct();
		}

		/**
		 * Primitive specialisation of {@code contains} for {@code int}.
		 *
		 * @see FunctionalIterable#contains(Object)
		 */
		default boolean contains(int i)
		{
			return iterator().contains(i);
		}

		/**
		 * Primitive specialisation of {@code count} for {@code int}.
		 *
		 * @see FunctionalIterable#count(java.util.function.Predicate)
		 */
		default long count(IntPredicate predicate)
		{
			return filter(predicate).count();
		}

		/**
		 * Primitive specialisation of {@code detect} for {@code int}.
		 *
		 * @see FunctionalIterable#detect(java.util.function.Predicate)
		 */
		default int detect(IntPredicate predicate)
		{
			return iterator().detect(predicate);
		}

		/**
		 * Find the maximum.
		 *
		 * @return an {@link OptionalDouble} either containing the maximum or being empty if the receiver is empty
		 * @see {@link Math#max(int, int)}.
		 */
		default OptionalInt max()
		{
			return iterator().max();
		}

		/**
		 * Find the minimum.
		 *
		 * @return an {@link OptionalDouble} either containing the minimum or being empty if the receiver is empty
		 * @see {@link Math#min(int, int)}.
		 */
		default OptionalInt min()
		{
			return iterator().min();
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 * This methods exploits the <em>widening primitive conversion</em> from {@code int} to {@code double}.
		 *
		 * @see FunctionalIterable#reduce(java.util.function.BinaryOperator)
		 */
		default OptionalDouble reduce(DoubleBinaryOperator accumulator)
		{
			return iterator().reduce(accumulator);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code int}.
		 *
		 * @see FunctionalIterable#reduce(java.util.function.BinaryOperator)
		 */
		default OptionalInt reduce(IntBinaryOperator accumulator)
		{
			return iterator().reduce(accumulator);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 * This methods exploits the <em>widening primitive conversion</em> from {@code int} to {@code long}.
		 *
		 * @see FunctionalIterable#reduce(java.util.function.BinaryOperator)
		 */
		default OptionalLong reduce(LongBinaryOperator accumulator)
		{
			return iterator().reduce(accumulator);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code int}.
		 *
		 * @see FunctionalIterable#reduce(Object, java.util.function.BiFunction)
		 */
		default <T> T reduce(T init, ObjIntFunction<T, T> accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 * This methods exploits the <em>widening primitive conversion</em> from {@code int} to {@code double}.
		 *
		 * @see FunctionalIterable#reduce(double, common.functions.DoubleObjToDoubleFunction)
		 */
		default double reduce(long init, DoubleBinaryOperator accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code int}.
		 *
		 * @see FunctionalIterable#reduce(int, common.functions.IntObjToIntFunction)
		 */
		default int reduce(int init, IntBinaryOperator accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 * This methods exploits the <em>widening primitive conversion</em> from {@code int} to {@code long}.
		 *
		 * @see FunctionalIterable#reduce(long, common.functions.LongObjToLongFunction)
		 */
		default long reduce(long init, LongBinaryOperator accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		/**
		 * Compute the sum over all elements of the receiver.
		 *
		 * @return the sum over all elements or 0 if the receiver is empty
		 */
		default long sum()
		{
			return iterator().sum();
		}
	}



	/**
	 * Specialisation for {@code long} of a FunctionalPrimitiveIterable.
	 */
	interface OfLong extends FunctionalPrimitiveIterable<Long, LongConsumer>, PrimitiveIterable.OfLong
	{
		@Override
		FunctionalPrimitiveIterator.OfLong iterator();



		// Transforming Methods

		@SuppressWarnings("unchecked")
		@Override
		default FunctionalIterable<Long> concat(Iterable<? extends Long>... iterables)
		{
			if (iterables instanceof PrimitiveIterable.OfLong[]) {
				return concat((PrimitiveIterable.OfLong[]) iterables);
			}
			return FunctionalPrimitiveIterable.super.concat(iterables);
		}

		/**
		 * Primitive specialisation of {@code concat} for {@code long}.
		 *
		 * @param iterables an array of {@link OfLong}s to append
		 */
		default FunctionalPrimitiveIterable.OfLong concat(PrimitiveIterable.OfLong... iterables)
		{
			switch (iterables.length) {
			case 0:
				return this;
			case 1:
				return new ChainedIterable.OfLong(new IterableArray.Of<>(this, iterables[0]));
			default:
				PrimitiveIterable.OfLong tail = new ChainedIterable.OfLong(new IterableArray.Of<>(iterables));
				return new ChainedIterable.OfLong(new IterableArray.Of(this, tail));
			}
		}

		/**
		 * Remove duplicate {@code long}s from the receiver such that only one occurrence is retained.
		 * Duplicates are identified in terms of {@code ==}.
		 */
		@Override
		default FunctionalPrimitiveIterable.OfLong distinct()
		{
			return filter(new Distinct.OfLong());
		}

		/**
		 * Remove consecutive duplicate {@code long}s such that only the first occurrence in a sequence is retained.
		 * Duplicates are identified in terms of {@link ==}.
		 */
		@Override
		default FunctionalPrimitiveIterable.OfLong dedupe()
		{
			return new FunctionalPrimitiveIterable.OfLong()
			{
				@Override
				public FunctionalPrimitiveIterator.OfLong iterator()
				{
					return FunctionalPrimitiveIterable.OfLong.this.iterator().dedupe();
				}
			};
		}

		/**
		 * Primitive specialisation of {@code filter} for {@code long}.
		 *
		 * @see FunctionalIterable#filter(java.util.function.Predicate)
		 */
		default FunctionalPrimitiveIterable.OfLong filter(LongPredicate predicate)
		{
			return new FilteringIterable.OfLong(this, predicate);
		}

		/**
		 * Primitive specialisation of {@code flatMap} for {@code long}.
		 *
		 * @see FunctionalIterable#flatMap(java.util.function.Function)
		 */
		default <T> FunctionalIterable<T> flatMap(LongFunction<? extends Iterable<? extends T>> function)
		{
			return new ChainedIterable.Of<>(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToDouble} for {@code long}.
		 *
		 * @see FunctionalIterable#flatMapToDouble(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterable.OfDouble flatMapToDouble(LongFunction<PrimitiveIterable.OfDouble> function)
		{
			return new ChainedIterable.OfDouble(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToInt} for {@code long}.
		 *
		 * @see FunctionalIterable#flatMapToInt(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterable.OfInt flatMapToInt(LongFunction<PrimitiveIterable.OfInt> function)
		{
			return new ChainedIterable.OfInt(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToLong} for {@code long}.
		 *
		 * @see FunctionalIterable#flatMapToLong(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterable.OfLong flatMapToLong(LongFunction<PrimitiveIterable.OfLong> function)
		{
			return new ChainedIterable.OfLong(map(function));
		}

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see FunctionalIterable#map(java.util.function.Function)
		 */
		default <T> FunctionalIterable<T> map(LongFunction<? extends T> function)
		{
			return new MappingIterable.LongToObj<>(this, function);
		}

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see FunctionalIterable#mapToDouble(java.util.function.ToDoubleFunction)
		 */
		default FunctionalPrimitiveIterable.OfDouble mapToDouble(LongToDoubleFunction function)
		{
			return new MappingIterable.LongToDouble(this, function);
		}

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see FunctionalIterable#mapToInt(java.util.function.ToIntFunction)
		 */
		default FunctionalPrimitiveIterable.OfInt mapToInt(LongToIntFunction function)
		{
			return new MappingIterable.LongToInt(this, function);
		}

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see FunctionalIterable#mapToLong(java.util.function.ToLongFunction)
		 */
		default FunctionalPrimitiveIterable.OfLong mapToLong(LongUnaryOperator function)
		{
			return new MappingIterable.LongToLong(this, function);
		}

		@Override
		default FunctionalPrimitiveIterable.OfLong nonNull()
		{
			return this;
		}



		// Accumulations Methods (Consuming)

		/**
		 * Primitive specialisation of {@code allMatch} for {@code long}.
		 *
		 * @see FunctionalIterable#allMatch(java.util.function.Predicate)
		 */
		default boolean allMatch(LongPredicate predicate)
		{
			return iterator().allMatch(predicate);
		}

		/**
		 * Primitive specialisation of {@code anyMatch} for {@code long}.
		 *
		 * @see FunctionalIterable#anyMatch(java.util.function.Predicate)
		 */
		default boolean anyMatch(LongPredicate predicate)
		{
			return iterator().anyMatch(predicate);
		}

		/**
		 * Primitive specialisation of {@code noneMatch} for {@code long}.
		 *
		 * @see FunctionalIterable#noneMatch(java.util.function.Predicate)
		 */
		default boolean noneMatch(LongPredicate predicate)
		{
			return iterator().noneMatch(predicate);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code long}.
		 *
		 * @see FunctionalIterable#collect(Object[])
		 */
		default long[] collect(long[] array)
		{
			return iterator().collect(array);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code long}.
		 *
		 * @see FunctionalIterable#collect(Object[], int)
		 */
		default long[] collect(long[] array, int offset)
		{
			return iterator().collect(array, offset);
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code long}.
		 *
		 * @see FunctionalIterable#collectAndCount(Object[])
		 */
		default int collectAndCount(long[] array)
		{
			return iterator().collectAndCount(array);
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code long}.
		 *
		 * @see FunctionalIterable#collectAndCount(Object[], int)
		 */
		default int collectAndCount(long[] array, int offset)
		{
			return iterator().collectAndCount(array, offset);
		}

		@Override
		default FunctionalPrimitiveIterable.OfLong collectDistinct()
		{
			return iterator().collectDistinct();
		}

		/**
		 * Primitive specialisation of {@code contains} for {@code long}.
		 *
		 * @see FunctionalIterable#contains(Object)
		 */
		default boolean contains(long l)
		{
			return iterator().contains(l);
		}

		/**
		 * Primitive specialisation of {@code count} for {@code long}.
		 *
		 * @see FunctionalIterable#count(java.util.function.Predicate)
		 */
		default long count(LongPredicate predicate)
		{
			return filter(predicate).count();
		}

		/**
		 * Primitive specialisation of {@code detect} for {@code long}.
		 *
		 * @see FunctionalIterable#detect(java.util.function.Predicate)
		 */
		default long detect(LongPredicate predicate)
		{
			return iterator().detect(predicate);
		}

		/**
		 * Find the maximum.
		 *
		 * @return an {@link OptionalDouble} either containing the maximum or being empty if the receiver is empty
		 * @see {@link Math#max(long, long)}.
		 */
		default OptionalLong max()
		{
			return iterator().max();
		}

		/**
		 * Find the minimum.
		 *
		 * @return an {@link OptionalDouble} either containing the minimum or being empty if the receiver is empty
		 * @see {@link Math#min(long, long)}.
		 */
		default OptionalLong min()
		{
			return iterator().min();
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 *
		 * @see FunctionalIterable#reduce(java.util.function.BinaryOperator)
		 */
		default OptionalLong reduce(LongBinaryOperator accumulator)
		{
			return iterator().reduce(accumulator);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 *
		 * @see FunctionalIterable#reduce(Object, java.util.function.BiFunction)
		 */
		default <T> T reduce(T init, ObjLongFunction<T, T> accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 *
		 * @see FunctionalIterable#reduce(double, common.functions.DoubleObjToDoubleFunction)
		 */
		default double reduce(double init, DoubleLongToDoubleFunction accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 *
		 * @see FunctionalIterable#reduce(int, common.functions.IntObjToIntFunction)
		 */
		default int reduce(int init, IntLongToIntFunction accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 *
		 * @see FunctionalIterable#reduce(long, common.functions.LongObjToLongFunction)
		 */
		default long reduce(long init, LongBinaryOperator accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		/**
		 * Compute the sum over all elements of the receiver.
		 *
		 * @return the sum over all elements or 0 if the receiver is empty
		 */
		default long sum()
		{
			return iterator().sum();
		}
	}
}
