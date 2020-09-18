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
import java.util.Iterator;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
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
 * A base interface for primitive specializations of {@code FunctionalIterator}.
 * Specialized sub interfaces are provided for the types {@link OfInt int}, {@link OfLong long} and {@link OfDouble double}.
 *
 * @param <E> the type of elements returned by this PrimitiveIterator
 * @param <E_CONS> the type of primitive consumer
 *
 * @see FunctionalIterator
 * @see java.util.stream.IntStream DoubleStream
 * @see java.util.stream.IntStream IntStream
 * @see java.util.stream.IntStream LongStream
 */
public interface FunctionalPrimitiveIterator<E, E_CONS> extends FunctionalIterator<E>, PrimitiveIterator<E, E_CONS>
{
	/**
	 * Specialisation for {@code double} of a FunctionalPrimitiveIterator.
	 */
	public interface OfDouble extends FunctionalPrimitiveIterator<Double, DoubleConsumer>, PrimitiveIterator.OfDouble
	{
		// Transforming Methods

		@Override
		default FunctionalIterator<Double> concat(Iterator<? extends Double>... iterators)
		{
			// Assume iterators are PrimitiveIterators
			PrimitiveIterator.OfDouble[] primitives = new PrimitiveIterator.OfDouble[iterators.length];
			for (int i = 0; i < iterators.length; i++) {
				if (! (iterators[i] instanceof PrimitiveIterator.OfDouble)) {
					// Assumption failed, resort to concatenation of generic Iterators
					return FunctionalPrimitiveIterator.super.concat(iterators);
				}
				primitives[i] = (PrimitiveIterator.OfDouble) iterators[i];
			}
			return concat(primitives);
		}

		/**
		 * Primitive specialisation of {@code concat} for {@code double}.
		 *
		 * @param iterators an array of {@link PrimitiveIterator.OfDouble}s to append
		 */
		default FunctionalPrimitiveIterator.OfDouble concat(PrimitiveIterator.OfDouble... iterators)
		{
			if (iterators.length == 0) {
				return this;
			}
			return new ChainedIterator.OfDouble(unwrap(), new ArrayIterator.Of(iterators));
		}

		@Override
		default FunctionalPrimitiveIterator.OfDouble consume()
		{
			forEachRemaining((double each) -> {});
			return this;
		}

		/**
		 * Remove duplicate {@code double}s from the receiver such that only one occurrence is retained.
		 * Duplicates are identified in terms of {@link ==} except for {@code NaN} for which all instances are consider equal, although {@code Double.NaN != Double.NaN}.
		 */
		@Override
		default FunctionalPrimitiveIterator.OfDouble distinct()
		{
			return filter(new Distinct.OfDouble());
		}

		/**
		 * Remove consecutive duplicate {@code double}s such that only the first occurrence in a sequence is retained.
		 * Duplicates are identified in terms of {@link ==} except for {@code NaN} for which all instances are consider equal, although {@code Double.NaN != Double.NaN}.
		 */
		@Override
		default FunctionalPrimitiveIterator.OfDouble dedupe()
		{
			DoublePredicate isFirst = new DoublePredicate()
			{
				boolean start = true;
				double previous = 0.0;

				@Override
				public boolean test(double d)
				{
					if (start) {
						start = false;
					} else if (previous == d) {
						return false;
					} else if (Double.isNaN(previous) && Double.isNaN(d)) {
						// Circumvent Double.NaN != Double.NaN
						return false;
					}
					previous = d;
					return true;
				}
			};
			return filter(isFirst);
		}

		/**
		 * Primitive specialisation of {@code filter} for {@code double}.
		 *
		 * @see FunctionalIterator#filter(java.util.function.Predicate)
		 */
		default FunctionalPrimitiveIterator.OfDouble filter(DoublePredicate predicate)
		{
			return new FilteringIterator.OfDouble(unwrap(), predicate);
		}

		/**
		 * Primitive specialisation of {@code flatMap} for {@code double}.
		 *
		 * @see FunctionalIterator#flatMap(java.util.function.Function)
		 */
		default <T> FunctionalIterator<T> flatMap(DoubleFunction<? extends Iterator<? extends T>> function)
		{
			return new ChainedIterator.Of<>(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToDouble} for {@code double}.
		 *
		 * @see FunctionalIterator#flatMapToDouble(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterator.OfDouble flatMapToDouble(DoubleFunction<PrimitiveIterator.OfDouble> function)
		{
			return new ChainedIterator.OfDouble(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToInt} for {@code double}.
		 *
		 * @see FunctionalIterator#flatMapToInt(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterator.OfInt flatMapToInt(DoubleFunction<PrimitiveIterator.OfInt> function)
		{
			return new ChainedIterator.OfInt(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToLong} for {@code double}.
		 *
		 * @see FunctionalIterator#flatMapToLong(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterator.OfLong flatMapToLong(DoubleFunction<PrimitiveIterator.OfLong> function)
		{
			return new ChainedIterator.OfLong(map(function));
		}

		/**
		 * Primitive specialisation of {@code map} for {@code double}.
		 *
		 * @see FunctionalIterator#map(java.util.function.Function)
		 */
		default <T> FunctionalIterator<T> map(DoubleFunction<? extends T> function)
		{
			return new MappingIterator.DoubleToObj<>(unwrap(), function);
		}

		/**
		 * Primitive specialisation of {@code mapToDouble} for {@code double}.
		 *
		 * @see FunctionalIterator#mapToDouble(java.util.function.ToDoubleFunction)
		 */
		default PrimitiveIterator.OfDouble mapToDouble(DoubleUnaryOperator function)
		{
			return new MappingIterator.DoubleToDouble(unwrap(), function);
		}

		/**
		 * Primitive specialisation of {@code mapToInt} for {@code double}.
		 *
		 * @see FunctionalIterator#mapToInt(java.util.function.ToIntFunction)
		 */
		default PrimitiveIterator.OfInt mapToInt(DoubleToIntFunction function)
		{
			return new MappingIterator.DoubleToInt(unwrap(), function);
		}

		/**
		 * Primitive specialisation of {@code mapToLong} for {@code double}.
		 *
		 * @see FunctionalIterator#mapToLong(java.util.function.ToLongFunction)
		 */
		default PrimitiveIterator.OfLong mapToLong(DoubleToLongFunction function)
		{
			return new MappingIterator.DoubleToLong(unwrap(), function);
		}

		@Override
		default FunctionalPrimitiveIterator.OfDouble nonNull()
		{
			return this;
		}

		@Override
		default PrimitiveIterator.OfDouble unwrap()
		{
			return this;
		}



		// Accumulations Methods (Consuming)

		/**
		 * Primitive specialisation of {@code allMatch} for {@code double}.
		 *
		 * @see FunctionalIterator#allMatch(java.util.function.Predicate)
		 */
		default boolean allMatch(DoublePredicate predicate)
		{
			return !anyMatch(predicate.negate());
		}

		/**
		 * Primitive specialisation of {@code anyMatch} for {@code double}.
		 *
		 * @see FunctionalIterator#anyMatch(java.util.function.Predicate)
		 */
		default boolean anyMatch(DoublePredicate predicate)
		{
			return filter(predicate).hasNext();
		}

		/**
		 * Primitive specialisation of {@code noneMatch} for {@code double}.
		 *
		 * @see FunctionalIterator#noneMatch(java.util.function.Predicate)
		 */
		default boolean noneMatch(DoublePredicate predicate)
		{
			return !anyMatch(predicate);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code double}.
		 *
		 * @see FunctionalIterator#collect(Object[])
		 */
		default double[] collect(double[] array)
		{
			return collect(array, 0);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code double}.
		 *
		 * @see FunctionalIterator#collect(Object[], int)
		 */
		default double[] collect(double[] array, int offset)
		{
			collectAndCount(array, offset);
			return array;
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code double}.
		 *
		 * @see FunctionalIterator#collectAndCount(Object[])
		 */
		default int collectAndCount(double[] array)
		{
			return collectAndCount(array, 0);
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code double}.
		 *
		 * @see FunctionalIterator#collectAndCount(Object[], int)
		 */
		default int collectAndCount(double[] array, int offset)
		{
			Objects.requireNonNull(array);
			int index = reduce(offset, (int i, double e) -> {array[i] = e; return i + 1;});
			return index - offset;
		}

		@Override
		default FunctionalPrimitiveIterable.OfDouble collectDistinct()
		{
			Distinct.OfDouble unseen = new Distinct.OfDouble();
			filter(unseen).consume();
			return unseen.getSeen();
		}

		@Override
		default boolean contains(Object obj)
		{
			if (obj instanceof Double) {
				return contains(((Double) obj).doubleValue());
			}
			release();
			return false;
		}

		/**
		 * Primitive specialisation of {@code contains} for {@code double}.
		 * Inclusion is defined in terms of {@link ==} except for {@code NaN} for which all instances are consider equal, although {@code Double.NaN != Double.NaN}.
		 *
		 * @see FunctionalIterator#contains(Object)
		 */
		default boolean contains(double d)
		{
			return anyMatch(Double.isNaN(d) ? (double e) -> Double.isNaN(e) : (double e) -> e == d);
		}

		@Override
		default long count()
		{
			// avoid auto-boxing
			return reduce(0L, (long c, double d) -> c + 1);
		}

		/**
		 * Primitive specialisation of {@code count} for {@code double}.
		 *
		 * @see FunctionalIterator#count(java.util.function.Predicate)
		 */
		default long count(DoublePredicate predicate)
		{
			return filter(predicate).count();
		}

		/**
		 * Primitive specialisation of {@code detect} for {@code double}.
		 *
		 * @see FunctionalIterator#detect(java.util.function.Predicate)
		 */
		default double detect(DoublePredicate predicate)
		{
			return filter(predicate).nextDouble();
		}

		/**
		 * Find the maximum.
		 *
		 * @return an {@link OptionalDouble} either containing the maximum or being empty if the receiver is empty
		 * @see {@link Math#max(double, double)}.
		 */
		default OptionalDouble max()
		{
			return reduce((DoubleBinaryOperator) Math::max);
		}

		/**
		 * Find the minimum.
		 *
		 * @return an {@link OptionalDouble} either containing the minimum or being empty if the receiver is empty
		 * @see {@link Math#min(double, double)}.
		 */
		default OptionalDouble min()
		{
			return reduce((DoubleBinaryOperator) Math::min);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 *
		 * @see FunctionalIterator#reduce(java.util.function.BinaryOperator)
		 */
		default OptionalDouble reduce(DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (!hasNext()) {
				return OptionalDouble.empty();
			}
			return OptionalDouble.of(reduce(nextDouble(), accumulator));
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 *
		 * @see FunctionalIterator#reduce(Object, java.util.function.BiFunction)
		 */
		default <T> T reduce(T init, ObjDoubleFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfDouble local = unwrap();
			T result = init;
			while (local.hasNext()) {
				result = accumulator.apply(result, local.nextDouble());
			}
			release();
			return result;
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 *
		 * @see FunctionalIterator#reduce(double, common.functions.DoubleObjToDoubleFunction)
		 */
		default double reduce(double init, DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfDouble local = unwrap();
			double result = init;
			while (local.hasNext()) {
				result = accumulator.applyAsDouble(result, local.nextDouble());
			}
			release();
			return result;
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 *
		 * @see FunctionalIterator#reduce(int, common.functions.IntObjToIntFunction)
		 */
		default int reduce(int init, IntDoubleToIntFunction accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfDouble local = unwrap();
			int result = init;
			while (local.hasNext()) {
				result = accumulator.applyAsInt(result, local.nextDouble());
			}
			release();
			return result;
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 *
		 * @see FunctionalIterator#reduce(long, common.functions.LongObjToLongFunction)
		 */
		default long reduce(long init, LongDoubleToLongFunction accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfDouble local = unwrap();
			long result = init;
			while (local.hasNext()) {
				result = accumulator.applyAsLong(result, local.nextDouble());
			}
			release();
			return result;
		}

		/**
		 * Compute the sum over all elements of the receiver.
		 *
		 * @return the sum over all elements or 0 if the receiver is empty
		 */
		default double sum()
		{
			return reduce(0.0, (DoubleBinaryOperator) Double::sum);
		}
	}



	/**
	 * Specialisation for {@code int} of a FunctionalPrimitiveIterator.
	 */
	public interface OfInt extends FunctionalPrimitiveIterator<Integer, IntConsumer>, PrimitiveIterator.OfInt
	{
		// Transforming Methods

		@Override
		default FunctionalIterator<Integer> concat(Iterator<? extends Integer>... iterators)
		{
			// Assume iterators are PrimitiveIterators
			PrimitiveIterator.OfInt[] primitives = new PrimitiveIterator.OfInt[iterators.length];
			for (int i = 0; i < iterators.length; i++) {
				if (! (iterators[i] instanceof PrimitiveIterator.OfInt)) {
					// Assumption failed, resort to concatenation of generic Iterators
					return FunctionalPrimitiveIterator.super.concat(iterators);
				}
				primitives[i] = (PrimitiveIterator.OfInt) iterators[i];
			}
			return concat(primitives);
		}

		/**
		 * Primitive specialisation of {@code concat} for {@code int}.
		 *
		 * @param iterators an array of {@link PrimitiveIterator.OfLong}s to append
		 */
		default FunctionalPrimitiveIterator.OfInt concat(PrimitiveIterator.OfInt... iterators)
		{
			if (iterators.length == 0) {
				return this;
			}
			return new ChainedIterator.OfInt(unwrap(), new ArrayIterator.Of(iterators));
		}

		@Override
		default FunctionalPrimitiveIterator.OfInt consume()
		{
			forEachRemaining((int each) -> {});
			return this;
		}

		/**
		 * Remove duplicate {@code int}s from the receiver such that only one occurrence is retained.
		 * Duplicates are identified in terms of {@code ==}.
		 */
		@Override
		default FunctionalPrimitiveIterator.OfInt distinct()
		{
			return filter(new Distinct.OfInt());
		}

		/**
		 * Remove consecutive duplicate {@code int}s such that only the first occurrence in a sequence is retained.
		 * Duplicates are identified in terms of {@link ==}.
		 */
		@Override
		default FunctionalPrimitiveIterator.OfInt dedupe()
		{
			IntPredicate isFirst = new IntPredicate()
			{
				boolean start = true;
				int previous = 0;

				@Override
				public boolean test(int i)
				{
					if (start) {
						start = false;
					} else if (previous == i) {
						return false;
					}
					previous = i;
					return true;
				}
			};
			return filter(isFirst);
		}

		/**
		 * Primitive specialisation of {@code filter} for {@code int}.
		 *
		 * @see FunctionalIterator#filter(java.util.function.Predicate)
		 */
		default FunctionalPrimitiveIterator.OfInt filter(IntPredicate predicate)
		{
			return new FilteringIterator.OfInt(unwrap(), predicate);
		}

		/**
		 * Primitive specialisation of {@code flatMap} for {@code int}.
		 *
		 * @see FunctionalIterator#flatMap(java.util.function.Function)
		 */
		default <T> FunctionalIterator<T> flatMap(IntFunction<? extends Iterator<? extends T>> function)
		{
			return new ChainedIterator.Of<>(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToDouble} for {@code int}.
		 *
		 * @see FunctionalIterator#flatMapToDouble(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterator.OfDouble flatMapToDouble(IntFunction<PrimitiveIterator.OfDouble> function)
		{
			return new ChainedIterator.OfDouble(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToInt} for {@code int}.
		 *
		 * @see FunctionalIterator#flatMapToInt(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterator.OfInt flatMapToInt(IntFunction<PrimitiveIterator.OfInt> function)
		{
			return new ChainedIterator.OfInt(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToLong} for {@code int}.
		 *
		 * @see FunctionalIterator#flatMapToLong(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterator.OfLong flatMapToLong(IntFunction<PrimitiveIterator.OfLong> function)
		{
			return new ChainedIterator.OfLong(map(function));
		}

		/**
		 * Primitive specialisation of {@code map} for {@code int}.
		 *
		 * @see FunctionalIterator#map(java.util.function.Function)
		 */
		default <T> FunctionalIterator<T> map(IntFunction<? extends T> function)
		{
			return new MappingIterator.IntToObj<>(unwrap(), function);
		}

		/**
		 * Primitive specialisation of {@code mapToDouble} for {@code int}.
		 *
		 * @see FunctionalIterator#mapToDouble(java.util.function.IntToDoubleFunction)
		 */
		default FunctionalPrimitiveIterator.OfDouble mapToDouble(IntToDoubleFunction function)
		{
			return new MappingIterator.IntToDouble(unwrap(), function);
		}

		/**
		 * Primitive specialisation of {@code mapToInt} for {@code int}.
		 *
		 * @see FunctionalIterator#mapToInt(java.util.function.ToIntFunction)
		 */
		default FunctionalPrimitiveIterator.OfInt mapToInt(IntUnaryOperator function)
		{
			return new MappingIterator.IntToInt(unwrap(), function);
		}

		/**
		 * Primitive specialisation of {@code mapToLong} for {@code int}.
		 *
		 * @see FunctionalIterator#mapToLong(java.util.function.ToLongFunction)
		 */
		default FunctionalPrimitiveIterator.OfLong mapToLong(IntToLongFunction function)
		{
			return new MappingIterator.IntToLong(unwrap(), function);
		}

		@Override
		default FunctionalPrimitiveIterator.OfInt nonNull()
		{
			return this;
		}

		@Override
		default PrimitiveIterator.OfInt unwrap()
		{
			return this;
		}



		// Accumulations Methods (Consuming)

		/**
		 * Primitive specialisation of {@code allMatch} for {@code int}.
		 *
		 * @see FunctionalIterator#allMatch(java.util.function.Predicate)
		 */
		default boolean allMatch(IntPredicate predicate)
		{
			return !anyMatch(predicate.negate());
		}

		/**
		 * Primitive specialisation of {@code anyMatch} for {@code int}.
		 *
		 * @see FunctionalIterator#anyMatch(java.util.function.Predicate)
		 */
		default boolean anyMatch(IntPredicate predicate)
		{
			return filter(predicate).hasNext();
		}

		/**
		 * Primitive specialisation of {@code noneMatch} for {@code int}.
		 *
		 * @see FunctionalIterator#noneMatch(java.util.function.Predicate)
		 */
		default boolean noneMatch(IntPredicate predicate)
		{
			return !anyMatch(predicate);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code int}.
		 *
		 * @see FunctionalIterator#collect(Object[])
		 */
		default int[] collect(int[] array)
		{
			return collect(array, 0);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code int}.
		 *
		 * @see FunctionalIterator#collect(Object[], int)
		 */
		default int[] collect(int[] array, int offset)
		{
			collectAndCount(array, offset);
			return array;
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code int} storing the elements in a {@link BitSet}.
		 * This method fails if the receiver yields a negative integer.
		 *
		 * @param indices a {@link BitSet}
		 * @return the BitSet with all elements added to it
		 * @throws IndexOutOfBoundsException if an integer is negative
		 * @see FunctionalIterator#collect(java.util.Collection) FunctionalIterator.collect(Collection)
		 */
		default BitSet collect(BitSet indices)
		{
			unwrap().forEachRemaining((IntConsumer) indices::set);
			release();
			return indices;
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code int}.
		 *
		 * @see FunctionalIterator#collectAndCount(Object[])
		 */
		default int collectAndCount(int[] array)
		{
			return collectAndCount(array, 0);
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code int}.
		 *
		 * @see FunctionalIterator#collectAndCount(Object[])
		 */
		default int collectAndCount(int[] array, int offset)
		{
			Objects.requireNonNull(array);
			int index = reduce(offset, (int i, int e) -> {array[i] = e; return i + 1;});
			return index - offset;
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code int} storing the elements in a {@link BitSet}.
		 * This method fails if the receiver yields a negative integer.
		 *
		 * @param indices a {@link BitSet}
		 * @return the number of elements added to the BitSet
		 * @throws IndexOutOfBoundsException if an integer is negative
		 * @see FunctionalIterator#collectAndCount(java.util.Collection) FunctionalIterator.collectAndCount(Collection)
		 */
		default int collectAndCount(BitSet indices)
		{
			Objects.requireNonNull(indices);
			return reduce(0, (int c, int i) -> {indices.set(i); return c + 1;});
		}

		@Override
		default FunctionalPrimitiveIterable.OfInt collectDistinct()
		{
			Distinct.OfInt unseen = new Distinct.OfInt();
			filter(unseen).consume();
			return unseen.getSeen();
		}

		@Override
		default boolean contains(Object obj)
		{
			if (obj instanceof Integer) {
				return contains(((Integer) obj).intValue());
			}
			release();
			return false;
		}

		/**
		 * Primitive specialisation of {@code contains} for {@code int}.
		 *
		 * @see FunctionalIterator#contains(Object)
		 */
		default boolean contains(int i)
		{
			return anyMatch((int e) -> e == i);
		}

		@Override
		default long count()
		{
			// avoid auto-boxing
			return reduce(0, (long c, long i) -> c + 1);
		}

		/**
		 * Primitive specialisation of {@code count} for {@code int}.
		 *
		 * @see FunctionalIterator#count(java.util.function.Predicate)
		 */
		default long count(IntPredicate predicate)
		{
			return filter(predicate).count();
		}

		/**
		 * Primitive specialisation of {@code detect} for {@code int}.
		 *
		 * @see FunctionalIterator#detect(java.util.function.Predicate)
		 */
		default int detect(IntPredicate predicate)
		{
			return filter(predicate).nextInt();
		}

		/**
		 * Find the maximum.
		 *
		 * @return an {@link OptionalDouble} either containing the maximum or being empty if the receiver is empty
		 * @see {@link Math#max(int, int)}.
		 */
		default OptionalInt max()
		{
			return reduce((IntBinaryOperator) Math::max);
		}

		/**
		 * Find the minimum.
		 *
		 * @return an {@link OptionalDouble} either containing the minimum or being empty if the receiver is empty
		 * @see {@link Math#min(int, int)}.
		 */
		default OptionalInt min()
		{
			return reduce((IntBinaryOperator) Math::min);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 * This methods exploits the <em>widening primitive conversion</em> from {@code int} to {@code double}.
		 *
		 * @see FunctionalIterator#reduce(java.util.function.BinaryOperator)
		 */
		default OptionalDouble reduce(DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (!hasNext()) {
				return OptionalDouble.empty();
			}
			return OptionalDouble.of(reduce(nextInt(), accumulator));
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code int}.
		 *
		 * @see FunctionalIterator#reduce(java.util.function.BinaryOperator)
		 */
		default OptionalInt reduce(IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (!hasNext()) {
				return OptionalInt.empty();
			}
			return OptionalInt.of(reduce(nextInt(), accumulator));
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 * This methods exploits the <em>widening primitive conversion</em> from {@code int} to {@code long}.
		 *
		 * @see FunctionalIterator#reduce(java.util.function.BinaryOperator)
		 */
		default OptionalLong reduce(LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (!hasNext()) {
				return OptionalLong.empty();
			}
			return OptionalLong.of(reduce(nextInt(), accumulator));
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code int}.
		 *
		 * @see FunctionalIterator#reduce(Object, java.util.function.BiFunction)
		 */
		default <T> T reduce(T init, ObjIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			T result = init;
			while (local.hasNext()) {
				result = accumulator.apply(result, local.nextInt());
			}
			release();
			return result;
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code double}.
		 * This methods exploits the <em>widening primitive conversion</em> from {@code int} to {@code double}.
		 *
		 * @see FunctionalIterator#reduce(double, common.functions.DoubleObjToDoubleFunction)
		 */
		default double reduce(double init, DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			double result = init;
			while (local.hasNext()) {
				result = accumulator.applyAsDouble(result, local.nextInt());
			}
			release();
			return result;
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code int}.
		 *
		 * @see FunctionalIterator#reduce(int, common.functions.IntObjToIntFunction)
		 */
		default int reduce(int init, IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			int result = init;
			while (local.hasNext()) {
				result = accumulator.applyAsInt(result, local.nextInt());
			}
			release();
			return result;
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 * This methods exploits the <em>widening primitive conversion</em> from {@code int} to {@code long}.
		 *
		 * @see FunctionalIterator#reduce(long, common.functions.LongObjToLongFunction)
		 */
		default long reduce(long init, LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			long result = init;
			while (local.hasNext()) {
				result = accumulator.applyAsLong(result, local.nextInt());
			}
			release();
			return result;
		}

		/**
		 * Compute the sum over all elements of the receiver.
		 *
		 * @return the sum over all elements or 0 if the receiver is empty
		 */
		default long sum()
		{
			return reduce(0L, (LongBinaryOperator) Long::sum);
		}
	}



	/**
	 * Specialisation for {@code long} of a FunctionalPrimitiveIterator.
	 */
	public interface OfLong extends FunctionalPrimitiveIterator<Long, LongConsumer>, PrimitiveIterator.OfLong
	{
		// Transforming Methods

		@Override
		default FunctionalIterator<Long> concat(Iterator<? extends Long>... iterators)
		{
			// Assume iterators are PrimitiveIterators
			PrimitiveIterator.OfLong[] primitives = new PrimitiveIterator.OfLong[iterators.length];
			for (int i = 0; i < iterators.length; i++) {
				if (! (iterators[i] instanceof PrimitiveIterator.OfLong)) {
					// Assumption failed, resort to concatenation of generic Iterators
					return FunctionalPrimitiveIterator.super.concat(iterators);
				}
				primitives[i] = (PrimitiveIterator.OfLong) iterators[i];
			}
			return concat(primitives);
		}

		/**
		 * Primitive specialisation of {@code concat} for {@code long}.
		 *
		 * @param iterators an array of {@link PrimitiveIterator.OfLong}s to append
		 */
		default FunctionalPrimitiveIterator.OfLong concat(PrimitiveIterator.OfLong... iterators)
		{
			if (iterators.length == 0) {
				return this;
			}
			return new ChainedIterator.OfLong(unwrap(), new ArrayIterator.Of(iterators));
		}

		@Override
		default FunctionalPrimitiveIterator.OfLong consume()
		{
			forEachRemaining((long each) -> {});
			return this;
		}

		/**
		 * Remove duplicate {@code long}s from the receiver such that only one occurrence is retained.
		 * Duplicates are identified in terms of {@code ==}.
		 */
		@Override
		default FunctionalPrimitiveIterator.OfLong distinct()
		{
			return filter(new Distinct.OfLong());
		}

		/**
		 * Remove consecutive duplicate {@code long}s such that only the first occurrence in a sequence is retained.
		 * Duplicates are identified in terms of {@link ==}.
		 */
		@Override
		default FunctionalPrimitiveIterator.OfLong dedupe()
		{
			LongPredicate isFirst = new LongPredicate()
			{
				boolean start = true;
				long previous = 0;

				@Override
				public boolean test(long l)
				{
					if (start) {
						start = false;
					} else if (previous == l) {
						return false;
					}
					previous = l;
					return true;
				}
			};
			return filter(isFirst);
		}

		/**
		 * Primitive specialisation of {@code filter} for {@code long}.
		 *
		 * @see FunctionalIterator#filter(java.util.function.Predicate)
		 */
		default FunctionalPrimitiveIterator.OfLong filter(LongPredicate predicate)
		{
			return new FilteringIterator.OfLong(unwrap(), predicate);
		}

		/**
		 * Primitive specialisation of {@code flatMap} for {@code long}.
		 *
		 * @see FunctionalIterator#flatMap(java.util.function.Function)
		 */
		default <T> FunctionalIterator<T> flatMap(LongFunction<? extends Iterator<? extends T>> function)
		{
			return new ChainedIterator.Of<>(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToDouble} for {@code long}.
		 *
		 * @see FunctionalIterator#flatMapToDouble(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterator.OfDouble flatMapToDouble(LongFunction<PrimitiveIterator.OfDouble> function)
		{
			return new ChainedIterator.OfDouble(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToInt} for {@code long}.
		 *
		 * @see FunctionalIterator#flatMapToInt(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterator.OfInt flatMapToInt(LongFunction<PrimitiveIterator.OfInt> function)
		{
			return new ChainedIterator.OfInt(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToLong} for {@code long}.
		 *
		 * @see FunctionalIterator#flatMapToLong(java.util.function.Function)
		 */
		default <T> FunctionalPrimitiveIterator.OfLong flatMapToLong(LongFunction<PrimitiveIterator.OfLong> function)
		{
			return new ChainedIterator.OfLong(map(function));
		}

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see FunctionalIterator#map(java.util.function.Function)
		 */
		default <T> FunctionalIterator<T> map(LongFunction<? extends T> function)
		{
			return new MappingIterator.LongToObj<>(unwrap(), function);
		}

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see FunctionalIterator#mapToDouble(java.util.function.ToDoubleFunction)
		 */
		default PrimitiveIterator.OfDouble mapToDouble(LongToDoubleFunction function)
		{
			return new MappingIterator.LongToDouble(unwrap(), function);
		}

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see FunctionalIterator#mapToInt(java.util.function.ToIntFunction)
		 */
		default PrimitiveIterator.OfInt mapToInt(LongToIntFunction function)
		{
			return new MappingIterator.LongToInt(unwrap(), function);
		}

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see FunctionalIterator#mapToLong(java.util.function.ToLongFunction)
		 */
		default PrimitiveIterator.OfLong mapToLong(LongUnaryOperator function)
		{
			return new MappingIterator.LongToLong(unwrap(), function);
		}

		@Override
		default FunctionalPrimitiveIterator.OfLong nonNull()
		{
			return this;
		}

		@Override
		default PrimitiveIterator.OfLong unwrap()
		{
			return this;
		}



		// Accumulations Methods (Consuming)

		/**
		 * Primitive specialisation of {@code allMatch} for {@code long}.
		 *
		 * @see FunctionalIterator#allMatch(java.util.function.Predicate)
		 */
		default boolean allMatch(LongPredicate predicate)
		{
			return !anyMatch(predicate.negate());
		}

		/**
		 * Primitive specialisation of {@code anyMatch} for {@code long}.
		 *
		 * @see FunctionalIterator#anyMatch(java.util.function.Predicate)
		 */
		default boolean anyMatch(LongPredicate predicate)
		{
			return filter(predicate).hasNext();
		}

		/**
		 * Primitive specialisation of {@code noneMatch} for {@code long}.
		 *
		 * @see FunctionalIterator#noneMatch(java.util.function.Predicate)
		 */
		default boolean noneMatch(LongPredicate predicate)
		{
			return !anyMatch(predicate);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code long}.
		 *
		 * @see FunctionalIterator#collect(Object[])
		 */
		default long[] collect(long[] array)
		{
			return collect(array, 0);
		}

		/**
		 * Primitive specialisation of {@code collect} for {@code long}.
		 *
		 * @see FunctionalIterator#collect(Object[], int)
		 */
		default long[] collect(long[] array, int offset)
		{
			collectAndCount(array, offset);
			return array;
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code long}.
		 *
		 * @see FunctionalIterator#collectAndCount(Object[])
		 */
		default int collectAndCount(long[] array)
		{
			return collectAndCount(array, 0);
		}

		/**
		 * Primitive specialisation of {@code collectAndCount} for {@code long}.
		 *
		 * @see FunctionalIterator#collectAndCount(Object[], int)
		 */
		default int collectAndCount(long[] array, int offset)
		{
			Objects.requireNonNull(array);
			int index = reduce(offset, (int i, long e) -> {array[i] = e; return i + 1;});
			return index - offset;
		}

		@Override
		default FunctionalPrimitiveIterable.OfLong collectDistinct()
		{
			Distinct.OfLong unseen = new Distinct.OfLong();
			filter(unseen).consume();
			return unseen.getSeen();
		}

		@Override
		default boolean contains(Object obj)
		{
			if (obj instanceof Long) {
				return contains(((Long) obj).longValue());
			}
			release();
			return false;
		}

		/**
		 * Primitive specialisation of {@code contains} for {@code long}.
		 *
		 * @see FunctionalIterator#contains(Object)
		 */
		default boolean contains(long l)
		{
			return anyMatch((long e) -> e == l);
		}

		@Override
		default long count()
		{
			// avoid auto-boxing
			return reduce(0, (long c, long l) -> c + 1);
		}

		/**
		 * Primitive specialisation of {@code count} for {@code long}.
		 *
		 * @see FunctionalIterator#count(java.util.function.Predicate)
		 */
		default long count(LongPredicate predicate)
		{
			return filter(predicate).count();
		}

		/**
		 * Primitive specialisation of {@code detect} for {@code long}.
		 *
		 * @see FunctionalIterator#detect(java.util.function.Predicate)
		 */
		default long detect(LongPredicate predicate)
		{
			return filter(predicate).nextLong();
		}

		/**
		 * Find the maximum.
		 *
		 * @return an {@link OptionalDouble} either containing the maximum or being empty if the receiver is empty
		 * @see {@link Math#max(long, long)}.
		 */
		default OptionalLong max()
		{
			return reduce((LongBinaryOperator) Math::max);
		}

		/**
		 * Find the minimum.
		 *
		 * @return an {@link OptionalDouble} either containing the minimum or being empty if the receiver is empty
		 * @see {@link Math#min(long, long)}.
		 */
		default OptionalLong min()
		{
			return reduce((LongBinaryOperator) Math::min);
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 *
		 * @see FunctionalIterator#reduce(java.util.function.BinaryOperator)
		 */
		default OptionalLong reduce(LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (!hasNext()) {
				return OptionalLong.empty();
			}
			return OptionalLong.of(reduce(nextLong(), accumulator));
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 *
		 * @see FunctionalIterator#reduce(Object, java.util.function.BiFunction)
		 */
		default <T> T reduce(T init, ObjLongFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfLong local = unwrap();
			T result = init;
			while (local.hasNext()) {
				result = accumulator.apply(result, local.nextLong());
			}
			release();
			return result;
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 *
		 * @see FunctionalIterator#reduce(double, common.functions.DoubleObjToDoubleFunction)
		 */
		default double reduce(double init, DoubleLongToDoubleFunction accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfLong local = unwrap();
			double result = init;
			while (local.hasNext()) {
				result = accumulator.applyAsDouble(result, local.nextLong());
			}
			release();
			return result;
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 *
		 * @see FunctionalIterator#reduce(int, common.functions.IntObjToIntFunction)
		 */
		default int reduce(int init, IntLongToIntFunction accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfLong local = unwrap();
			int result = init;
			while (local.hasNext()) {
				result = accumulator.applyAsInt(result, local.nextLong());
			}
			release();
			return result;
		}

		/**
		 * Primitive specialisation of {@code reduce} for {@code long}.
		 *
		 * @see FunctionalIterator#reduce(long, common.functions.LongObjToLongFunction)
		 */
		default long reduce(long init, LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfLong local = unwrap();
			long result = init;
			while (local.hasNext()) {
				result = accumulator.applyAsLong(result, local.nextLong());
			}
			release();
			return result;
		}

		/**
		 * Compute the sum over all elements of the receiver.
		 *
		 * @return the sum over all elements or 0 if the receiver is empty
		 */
		default long sum()
		{
			return reduce(0L, (LongBinaryOperator) Long::sum);
		}
	}
}
