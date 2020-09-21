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

import common.functions.ObjDoubleFunction;
import common.functions.ObjIntFunction;
import common.functions.ObjLongFunction;
import common.functions.primitive.DoubleLongToDoubleFunction;
import common.functions.primitive.IntDoubleToIntFunction;
import common.functions.primitive.IntLongToIntFunction;
import common.functions.primitive.LongDoubleToLongFunction;
import common.iterable.FunctionalPrimitiveIterator.OfDouble;
import common.iterable.FunctionalPrimitiveIterator.OfInt;
import common.iterable.FunctionalPrimitiveIterator.OfLong;

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
public interface FunctionalPrimitiveIterable<E, E_CONS> extends FunctionalIterable<E>
{
	@Override
	FunctionalPrimitiveIterator<E, E_CONS> iterator();

	/**
	 * Perform the given action for each element of the receiver.
	 *
	 * @see Iterable#forEach(java.util.function.Consumer)
	 */
	default void forEach(E_CONS action)
	{
		iterator().forEachRemaining(action);
	}



	/**
	 * Specialisation for {@code double} of a FunctionalPrimitiveIterable.
	 */
	public interface IterableDouble extends Iterable<Double>, FunctionalPrimitiveIterable<Double, DoubleConsumer>
	{
		@Override
		FunctionalPrimitiveIterator.OfDouble iterator();



		// Transforming Methods

		@SuppressWarnings("unchecked")
		@Override
		default FunctionalIterable<Double> concat(Iterable<? extends Double>... iterables)
		{
			if (iterables instanceof IterableDouble[]) {
				return concat((IterableDouble[]) iterables);
			}
			return FunctionalPrimitiveIterable.super.concat(iterables);
		}

		/**
		 * Primitive specialisation of {@code concat} for {@code double}.
		 *
		 * @param iterables an array of {@link IterableDouble}s to append
		 */
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

		/**
		 * Remove duplicate {@code double}s from the receiver such that only one occurrence is retained.
		 * Duplicates are identified in terms of {@link ==} except for {@code NaN} for which all instances are consider equal, although {@code Double.NaN != Double.NaN}.
		 */
		@Override
		default IterableDouble distinct()
		{
			return filter(new Distinct.OfDouble());
		}

		/**
		 * Remove consecutive duplicate {@code double}s such that only the first occurrence in a sequence is retained.
		 * Duplicates are identified in terms of {@link ==} except for {@code NaN} for which all instances are consider equal, although {@code Double.NaN != Double.NaN}.
		 */
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

		/**
		 * Primitive specialisation of {@code filter} for {@code double}.
		 *
		 * @see FunctionalIterable#filter(java.util.function.Predicate)
		 */
		default IterableDouble filter(DoublePredicate predicate)
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
		default <T> IterableDouble flatMapToDouble(DoubleFunction<IterableDouble> function)
		{
			return new ChainedIterable.OfDouble(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToInt} for {@code double}.
		 *
		 * @see FunctionalIterable#flatMapToInt(java.util.function.Function)
		 */
		default <T> IterableInt flatMapToInt(DoubleFunction<IterableInt> function)
		{
			return new ChainedIterable.OfInt(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToLong} for {@code double}.
		 *
		 * @see FunctionalIterable#flatMapToLong(java.util.function.Function)
		 */
		default <T> IterableLong flatMapToLong(DoubleFunction<IterableLong> function)
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
		default IterableDouble mapToDouble(DoubleUnaryOperator function)
		{
			return new MappingIterable.DoubleToDouble(this, function);
		}

		/**
		 * Primitive specialisation of {@code mapToInt} for {@code double}.
		 *
		 * @see FunctionalIterable#mapToInt(java.util.function.ToIntFunction)
		 */
		default IterableInt mapToInt(DoubleToIntFunction function)
		{
			return new MappingIterable.DoubleToInt(this, function);
		}

		/**
		 * Primitive specialisation of {@code mapToLong} for {@code double}.
		 *
		 * @see FunctionalIterable#mapToLong(java.util.function.ToLongFunction)
		 */
		default IterableLong mapToLong(DoubleToLongFunction function)
		{
			return new MappingIterable.DoubleToLong(this, function);
		}

		@Override
		default IterableDouble nonNull()
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
		default IterableDouble collectDistinct()
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
		default OptionalDouble detect(DoublePredicate predicate)
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
	public interface IterableInt extends Iterable<Integer>, FunctionalPrimitiveIterable<Integer, IntConsumer>
	{
		@Override
		FunctionalPrimitiveIterator.OfInt iterator();



		// Transforming Methods

		@SuppressWarnings("unchecked")
		@Override
		default FunctionalIterable<Integer> concat(Iterable<? extends Integer>... iterables)
		{
			if (iterables instanceof IterableInt[]) {
				return concat((IterableInt[]) iterables);
			}
			return FunctionalPrimitiveIterable.super.concat(iterables);
		}

		/**
		 * Primitive specialisation of {@code concat} for {@code int}.
		 *
		 * @param iterables an array of {@link IterableInt}s to append
		 */
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

		/**
		 * Remove duplicate {@code int}s from the receiver such that only one occurrence is retained.
		 * Duplicates are identified in terms of {@code ==}.
		 */
		@Override
		default IterableInt distinct()
		{
			return filter(new Distinct.OfInt());
		}

		/**
		 * Remove consecutive duplicate {@code int}s such that only the first occurrence in a sequence is retained.
		 * Duplicates are identified in terms of {@link ==}.
		 */
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

		/**
		 * Primitive specialisation of {@code filter} for {@code int}.
		 *
		 * @see FunctionalIterable#filter(java.util.function.Predicate)
		 */
		default IterableInt filter(IntPredicate predicate)
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
		default <T> IterableDouble flatMapToDouble(IntFunction<IterableDouble> function)
		{
			return new ChainedIterable.OfDouble(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToInt} for {@code int}.
		 *
		 * @see FunctionalIterable#flatMapToInt(java.util.function.Function)
		 */
		default <T> IterableInt flatMapToInt(IntFunction<IterableInt> function)
		{
			return new ChainedIterable.OfInt(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToLong} for {@code int}.
		 *
		 * @see FunctionalIterable#flatMapToLong(java.util.function.Function)
		 */
		default <T> IterableLong flatMapToLong(IntFunction<IterableLong> function)
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
		default IterableDouble mapToDouble(IntToDoubleFunction function)
		{
			return new MappingIterable.IntToDouble(this, function);
		}

		/**
		 * Primitive specialisation of {@code mapToInt} for {@code int}.
		 *
		 * @see FunctionalIterable#mapToInt(java.util.function.ToIntFunction)
		 */
		default IterableInt mapToInt(IntUnaryOperator function)
		{
			return new MappingIterable.IntToInt(this, function);
		}

		/**
		 * Primitive specialisation of {@code mapToLong} for {@code int}.
		 *
		 * @see FunctionalIterable#mapToLong(java.util.function.ToLongFunction)
		 */
		default IterableLong mapToLong(IntToLongFunction function)
		{
			return new MappingIterable.IntToLong(this, function);
		}

		@Override
		default IterableInt nonNull()
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
		default IterableInt collectDistinct()
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
		default OptionalInt detect(IntPredicate predicate)
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
	public interface IterableLong extends Iterable<Long>, FunctionalPrimitiveIterable<Long, LongConsumer>
	{
		@Override
		FunctionalPrimitiveIterator.OfLong iterator();



		// Transforming Methods

		@SuppressWarnings("unchecked")
		@Override
		default FunctionalIterable<Long> concat(Iterable<? extends Long>... iterables)
		{
			if (iterables instanceof IterableLong[]) {
				return concat((IterableLong[]) iterables);
			}
			return FunctionalPrimitiveIterable.super.concat(iterables);
		}

		/**
		 * Primitive specialisation of {@code concat} for {@code long}.
		 *
		 * @param iterables an array of {@link IterableLong}s to append
		 */
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

		/**
		 * Remove duplicate {@code long}s from the receiver such that only one occurrence is retained.
		 * Duplicates are identified in terms of {@code ==}.
		 */
		@Override
		default IterableLong distinct()
		{
			return filter(new Distinct.OfLong());
		}

		/**
		 * Remove consecutive duplicate {@code long}s such that only the first occurrence in a sequence is retained.
		 * Duplicates are identified in terms of {@link ==}.
		 */
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

		/**
		 * Primitive specialisation of {@code filter} for {@code long}.
		 *
		 * @see FunctionalIterable#filter(java.util.function.Predicate)
		 */
		default IterableLong filter(LongPredicate predicate)
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
		default <T> IterableDouble flatMapToDouble(LongFunction<IterableDouble> function)
		{
			return new ChainedIterable.OfDouble(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToInt} for {@code long}.
		 *
		 * @see FunctionalIterable#flatMapToInt(java.util.function.Function)
		 */
		default <T> IterableInt flatMapToInt(LongFunction<IterableInt> function)
		{
			return new ChainedIterable.OfInt(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToLong} for {@code long}.
		 *
		 * @see FunctionalIterable#flatMapToLong(java.util.function.Function)
		 */
		default <T> IterableLong flatMapToLong(LongFunction<IterableLong> function)
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
		default IterableDouble mapToDouble(LongToDoubleFunction function)
		{
			return new MappingIterable.LongToDouble(this, function);
		}

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see FunctionalIterable#mapToInt(java.util.function.ToIntFunction)
		 */
		default IterableInt mapToInt(LongToIntFunction function)
		{
			return new MappingIterable.LongToInt(this, function);
		}

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see FunctionalIterable#mapToLong(java.util.function.ToLongFunction)
		 */
		default IterableLong mapToLong(LongUnaryOperator function)
		{
			return new MappingIterable.LongToLong(this, function);
		}

		@Override
		default IterableLong nonNull()
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
		default IterableLong collectDistinct()
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
		default OptionalLong detect(LongPredicate predicate)
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
