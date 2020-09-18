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

import common.functions.*;

import java.util.*;
import java.util.function.*;

/**
 * A base type for primitive specializations of the methods provided by {@link FunctionalIterator}.
 * Specialized sub interfaces are provided for {@code double}, {@code int} and {@code long}:
 * <ul>
 *     <li>{@link PrimitiveIterator.OfDouble double}</li>
 *     <li>{@link PrimitiveIterator.OfInt int}</li>
 *     <li>{@link PrimitiveIterator.OfLong long}</li>
 * </ul>
 *
 * @param <E> the type of elements returned by this PrimitiveIterator
 * @param <E_CONS> the type of primitive consumer
 */
public interface FunctionalPrimitiveIterator<E, E_CONS> extends PrimitiveReducible<E, Iterator<? extends E>, E_CONS>,FunctionalIterator<E>, PrimitiveIterator<E, E_CONS>
{
	@Override
	default void forEach(E_CONS action)
	{
		unwrap().forEachRemaining(action);
		release();
	}

	@Override
	PrimitiveIterator<E, E_CONS> unwrap();



	/**
	 * Specialisation for {@code double} of a FunctionalPrimitiveIterator.
	 */
	interface OfDouble extends FunctionalPrimitiveIterator<Double, DoubleConsumer>, PrimitiveReducible.OfDouble<Iterator<? extends Double>>, PrimitiveIterator.OfDouble
	{
		@Override
		default PrimitiveIterator.OfDouble unwrap()
		{
			return this;
		}



		// Transforming Methods

		@Override
		default FunctionalIterator<Double> concat(Iterator<? extends Double> iterator)
		{
			if (iterator instanceof PrimitiveIterator.OfDouble) {
				return concat((PrimitiveIterator.OfDouble) iterator);
			}
			return FunctionalPrimitiveIterator.super.concat(iterator);
		}

		/**
		 * Primitive specialisation of {@code concat} for {@code double}.
		 *
		 * @param iterator the {@link PrimitiveIterator.OfDouble} to append
		 * @return an {@link Iterator} that iterates over the elements of the receiver and the argument
		 */
		default FunctionalPrimitiveIterator.OfDouble concat(PrimitiveIterator.OfDouble iterator)
		{
			return new ChainedIterator.OfDouble(unwrap(), new ArrayIterator.Of<>(iterator));
		}

		@Override
		default FunctionalPrimitiveIterator.OfDouble dedupe()
		{
			return (FunctionalPrimitiveIterator.OfDouble) PrimitiveReducible.OfDouble.super.dedupe();
		}

		@Override
		default FunctionalPrimitiveIterator.OfDouble distinct()
		{
			return (FunctionalPrimitiveIterator.OfDouble) PrimitiveReducible.OfDouble.super.distinct();
		}

		@Override
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
		default FunctionalPrimitiveIterator.OfDouble flatMapToDouble(DoubleFunction<PrimitiveIterator.OfDouble> function)
		{
			return new ChainedIterator.OfDouble(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToInt} for {@code double}.
		 *
		 * @see FunctionalIterator#flatMapToInt(java.util.function.Function)
		 */
		default FunctionalPrimitiveIterator.OfInt flatMapToInt(DoubleFunction<PrimitiveIterator.OfInt> function)
		{
			return new ChainedIterator.OfInt(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToLong} for {@code double}.
		 *
		 * @see FunctionalIterator#flatMapToLong(java.util.function.Function)
		 */
		default FunctionalPrimitiveIterator.OfLong flatMapToLong(DoubleFunction<PrimitiveIterator.OfLong> function)
		{
			return new ChainedIterator.OfLong(map(function));
		}

		@Override
		default <T> FunctionalIterator<T> map(DoubleFunction<? extends T> function)
		{
			return new MappingIterator.DoubleToObj<>(unwrap(), function);
		}

		@Override
		default FunctionalPrimitiveIterator.OfDouble mapToDouble(DoubleUnaryOperator function)
		{
			return new MappingIterator.DoubleToDouble(unwrap(), function);
		}

		@Override
		default FunctionalPrimitiveIterator.OfInt mapToInt(DoubleToIntFunction function)
		{
			return new MappingIterator.DoubleToInt(unwrap(), function);
		}

		@Override
		default FunctionalPrimitiveIterator.OfLong mapToLong(DoubleToLongFunction function)
		{
			return new MappingIterator.DoubleToLong(unwrap(), function);
		}

		@Override
		default FunctionalPrimitiveIterator.OfDouble nonNull()
		{
			return this;
		}



		// Accumulations Methods (Consuming)

		@Override
		default FunctionalPrimitiveIterator.OfDouble consume()
		{
			return (FunctionalPrimitiveIterator.OfDouble) PrimitiveReducible.OfDouble.super.consume();
		}

		@Override
		default boolean contains(Object obj)
		{
			boolean found = PrimitiveReducible.OfDouble.super.contains(obj);
			release();
			return found;
		}

		@Override
		default double detect(DoublePredicate predicate)
		{
			return filter(predicate).nextDouble();
		}

		@Override
		default OptionalDouble reduce(DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (! hasNext()) {
				return OptionalDouble.empty();
			}
			return OptionalDouble.of(reduce(nextDouble(), accumulator));
		}

		@Override
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

		@Override
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

		@Override
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

		@Override
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
	}



	/**
	 * Specialisation for {@code int} of a FunctionalPrimitiveIterator.
	 */
	interface OfInt extends FunctionalPrimitiveIterator<Integer, IntConsumer>, PrimitiveReducible.OfInt<Iterator<? extends Integer>>, PrimitiveIterator.OfInt
	{
		@Override
		default PrimitiveIterator.OfInt unwrap()
		{
			return this;
		}



		// Transforming Methods

		@Override
		default FunctionalIterator<Integer> concat(Iterator<? extends Integer> iterator)
		{
			if (iterator instanceof PrimitiveIterator.OfInt) {
				return concat((PrimitiveIterator.OfInt) iterator);
			}
			return FunctionalPrimitiveIterator.super.concat(iterator);
		}

		/**
		 * Primitive specialisation of {@code concat} for {@code int}.
		 *
		 * @param iterator the {@link PrimitiveIterator.OfInt} to append
		 * @return an {@link Iterator} that iterates over the elements of the receiver and the argument
		 */
		default FunctionalPrimitiveIterator.OfInt concat(PrimitiveIterator.OfInt iterator)
		{
			return new ChainedIterator.OfInt(unwrap(), new ArrayIterator.Of<>(iterator));
		}

		@Override
		default FunctionalPrimitiveIterator.OfInt dedupe()
		{
			return (FunctionalPrimitiveIterator.OfInt) PrimitiveReducible.OfInt.super.dedupe();
		}

		@Override
		default FunctionalPrimitiveIterator.OfInt distinct()
		{
			return (FunctionalPrimitiveIterator.OfInt) PrimitiveReducible.OfInt.super.distinct();
		}

		@Override
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
		default FunctionalPrimitiveIterator.OfDouble flatMapToDouble(IntFunction<PrimitiveIterator.OfDouble> function)
		{
			return new ChainedIterator.OfDouble(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToInt} for {@code int}.
		 *
		 * @see FunctionalIterator#flatMapToInt(java.util.function.Function)
		 */
		default FunctionalPrimitiveIterator.OfInt flatMapToInt(IntFunction<PrimitiveIterator.OfInt> function)
		{
			return new ChainedIterator.OfInt(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToLong} for {@code int}.
		 *
		 * @see FunctionalIterator#flatMapToLong(java.util.function.Function)
		 */
		default FunctionalPrimitiveIterator.OfLong flatMapToLong(IntFunction<PrimitiveIterator.OfLong> function)
		{
			return new ChainedIterator.OfLong(map(function));
		}

		@Override
		default <T> FunctionalIterator<T> map(IntFunction<? extends T> function)
		{
			return new MappingIterator.IntToObj<>(unwrap(), function);
		}

		@Override
		default FunctionalPrimitiveIterator.OfDouble mapToDouble(IntToDoubleFunction function)
		{
			return new MappingIterator.IntToDouble(unwrap(), function);
		}

		@Override
		default FunctionalPrimitiveIterator.OfInt mapToInt(IntUnaryOperator function)
		{
			return new MappingIterator.IntToInt(unwrap(), function);
		}

		/**
		 * Primitive specialisation of {@code mapToLong} for {@code int}.
		 *
		 * @see FunctionalIterator#mapToLong(java.util.function.ToLongFunction)
		 */
		@Override
		default FunctionalPrimitiveIterator.OfLong mapToLong(IntToLongFunction function)
		{
			return new MappingIterator.IntToLong(unwrap(), function);
		}

		@Override
		default FunctionalPrimitiveIterator.OfInt nonNull()
		{
			return this;
		}



		// Accumulations Methods (Consuming)

		@Override
		default FunctionalPrimitiveIterator.OfInt consume()
		{
			return (FunctionalPrimitiveIterator.OfInt) PrimitiveReducible.OfInt.super.consume();
		}

		@Override
		default boolean contains(Object obj)
		{
			boolean found = PrimitiveReducible.OfInt.super.contains(obj);
			release();
			return found;
		}

		@Override
		default int detect(IntPredicate predicate)
		{
			return filter(predicate).nextInt();
		}

		@Override
		default OptionalDouble reduce(DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (! hasNext()) {
				return OptionalDouble.empty();
			}
			return OptionalDouble.of(reduce(nextInt(), accumulator));
		}

		@Override
		default OptionalInt reduce(IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (! hasNext()) {
				return OptionalInt.empty();
			}
			return OptionalInt.of(reduce(nextInt(), accumulator));
		}

		@Override
		default OptionalLong reduce(LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (! hasNext()) {
				return OptionalLong.empty();
			}
			return OptionalLong.of(reduce(nextInt(), accumulator));
		}

		@Override
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

		@Override
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

		@Override
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

		@Override
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
	}



	/**
	 * Specialisation for {@code long} of a FunctionalPrimitiveIterator.
	 */
	interface OfLong extends FunctionalPrimitiveIterator<Long, LongConsumer>, PrimitiveReducible.OfLong<Iterator<? extends Long>>, PrimitiveIterator.OfLong
	{
		@Override
		default PrimitiveIterator.OfLong unwrap()
		{
			return this;
		}



		// Transforming Methods

		@Override
		default FunctionalIterator<Long> concat(Iterator<? extends Long> iterator)
		{
			if (iterator instanceof PrimitiveIterator.OfLong) {
				return concat((PrimitiveIterator.OfLong) iterator);
			}
			return FunctionalPrimitiveIterator.super.concat(iterator);
		}

		/**
		 * Primitive specialisation of {@code concat} for {@code long}.
		 *
		 * @param iterator the {@link PrimitiveIterator.OfLong} to append
		 * @return an {@link Iterator} that iterates over the elements of the receiver and the argument
		 */
		default FunctionalPrimitiveIterator.OfLong concat(PrimitiveIterator.OfLong iterator)
		{
			return new ChainedIterator.OfLong(unwrap(), new ArrayIterator.Of<>(iterator));
		}

		@Override
		default FunctionalPrimitiveIterator.OfLong dedupe()
		{
			return (FunctionalPrimitiveIterator.OfLong) PrimitiveReducible.OfLong.super.dedupe();
		}

		@Override
		default FunctionalPrimitiveIterator.OfLong distinct()
		{
			return (FunctionalPrimitiveIterator.OfLong) PrimitiveReducible.OfLong.super.distinct();
		}

		@Override
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
		default FunctionalPrimitiveIterator.OfDouble flatMapToDouble(LongFunction<PrimitiveIterator.OfDouble> function)
		{
			return new ChainedIterator.OfDouble(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToInt} for {@code long}.
		 *
		 * @see FunctionalIterator#flatMapToInt(java.util.function.Function)
		 */
		default FunctionalPrimitiveIterator.OfInt flatMapToInt(LongFunction<PrimitiveIterator.OfInt> function)
		{
			return new ChainedIterator.OfInt(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToLong} for {@code long}.
		 *
		 * @see FunctionalIterator#flatMapToLong(java.util.function.Function)
		 */
		default FunctionalPrimitiveIterator.OfLong flatMapToLong(LongFunction<PrimitiveIterator.OfLong> function)
		{
			return new ChainedIterator.OfLong(map(function));
		}

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see FunctionalIterator#map(java.util.function.Function)
		 */
		@Override
		default <T> FunctionalIterator<T> map(LongFunction<? extends T> function)
		{
			return new MappingIterator.LongToObj<>(unwrap(), function);
		}

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see FunctionalIterator#mapToDouble(java.util.function.ToDoubleFunction)
		 */
		@Override
		default FunctionalPrimitiveIterator.OfDouble mapToDouble(LongToDoubleFunction function)
		{
			return new MappingIterator.LongToDouble(unwrap(), function);
		}

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see FunctionalIterator#mapToInt(java.util.function.ToIntFunction)
		 */
		@Override
		default FunctionalPrimitiveIterator.OfInt mapToInt(LongToIntFunction function)
		{
			return new MappingIterator.LongToInt(unwrap(), function);
		}

		/**
		 * Primitive specialisation of {@code map} for {@code long}.
		 *
		 * @see FunctionalIterator#mapToLong(java.util.function.ToLongFunction)
		 */
		@Override
		default FunctionalPrimitiveIterator.OfLong mapToLong(LongUnaryOperator function)
		{
			return new MappingIterator.LongToLong(unwrap(), function);
		}

		@Override
		default FunctionalPrimitiveIterator.OfLong nonNull()
		{
			return this;
		}



		// Accumulations Methods (Consuming)

		@Override
		default FunctionalPrimitiveIterator.OfLong consume()
		{
			return (FunctionalPrimitiveIterator.OfLong) PrimitiveReducible.OfLong.super.consume();
		}

		@Override
		default boolean contains(Object obj)
		{
			boolean found = PrimitiveReducible.OfLong.super.contains(obj);
			release();
			return found;
		}

		@Override
		default long detect(LongPredicate predicate)
		{
			return filter(predicate).nextLong();
		}

		default OptionalLong reduce(LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (! hasNext()) {
				return OptionalLong.empty();
			}
			return OptionalLong.of(reduce(nextLong(), accumulator));
		}

		@Override
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

		@Override
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

		@Override
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

		@Override
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
	}
}
