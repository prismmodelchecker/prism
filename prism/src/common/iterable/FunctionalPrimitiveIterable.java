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

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.*;

/**
 * A base type for primitive specializations of the methods provided by {@link FunctionalIterable}.
 * Specialized sub interfaces are provided for {@code double}, {@code int} and {@code long}:
 * <ul>
 *     <li>{@link PrimitiveIterable.OfDouble double}</li>
 *     <li>{@link PrimitiveIterable.OfInt int}</li>
 *     <li>{@link PrimitiveIterable.OfLong long}</li>
 * </ul>
 *
 * @param <E>      the type of elements hold by this PrimitiveIterable
 * @param <E_CONS> the type of primitive consumer
 */
public interface FunctionalPrimitiveIterable<E, E_CONS> extends PrimitiveReducible<E, Iterable<? extends E>, E_CONS>, FunctionalIterable<E>, PrimitiveIterable<E, E_CONS>
{
	@Override
	FunctionalPrimitiveIterator<E, E_CONS> iterator();

	@Override
	default void forEach(E_CONS action)
	{
		iterator().forEach(action);
	}


	/**
	 * Specialisation for {@code double} of a FunctionalPrimitiveIterable.
	 */
	interface OfDouble extends FunctionalPrimitiveIterable<Double, DoubleConsumer>, PrimitiveReducible.OfDouble<Iterable<? extends Double>>, PrimitiveIterable.OfDouble
	{
		@Override
		FunctionalPrimitiveIterator.OfDouble iterator();


		// Transforming Methods

		@Override
		default FunctionalIterable<Double> concat(Iterable<? extends Double> iterable)
		{
			if (iterable instanceof PrimitiveIterable.OfDouble) {
				return concat((PrimitiveIterable.OfDouble) iterable);
			}
			return FunctionalPrimitiveIterable.super.concat(iterable);
		}

		/**
		 * Primitive specialisation of {@code concat} for {@code double}.
		 *
		 * @param iterable the {@link PrimitiveIterable.OfDouble} to append
		 * @return an {@link Iterable} that iterates over the elements of the receiver and the argument
		 */
		default FunctionalPrimitiveIterable.OfDouble concat(PrimitiveIterable.OfDouble iterable)
		{
			return new ChainedIterable.OfDouble(new IterableArray.Of<>(this, iterable));
		}

		@Override
		default FunctionalPrimitiveIterable.OfDouble dedupe()
		{
			return (FunctionalPrimitiveIterable.OfDouble) PrimitiveReducible.OfDouble.super.dedupe();
		}

		@Override
		default FunctionalPrimitiveIterable.OfDouble distinct()
		{
			return (FunctionalPrimitiveIterable.OfDouble) PrimitiveReducible.OfDouble.super.distinct();
		}

		@Override
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
		default FunctionalPrimitiveIterable.OfDouble flatMapToDouble(DoubleFunction<PrimitiveIterable.OfDouble> function)
		{
			return new ChainedIterable.OfDouble(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToInt} for {@code double}.
		 *
		 * @see FunctionalIterable#flatMapToInt(java.util.function.Function)
		 */
		default FunctionalPrimitiveIterable.OfInt flatMapToInt(DoubleFunction<PrimitiveIterable.OfInt> function)
		{
			return new ChainedIterable.OfInt(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToLong} for {@code double}.
		 *
		 * @see FunctionalIterable#flatMapToLong(java.util.function.Function)
		 */
		default FunctionalPrimitiveIterable.OfLong flatMapToLong(DoubleFunction<PrimitiveIterable.OfLong> function)
		{
			return new ChainedIterable.OfLong(map(function));
		}

		@Override
		default <T> FunctionalIterable<T> map(DoubleFunction<? extends T> function)
		{
			return new MappingIterable.DoubleToObj<>(this, function);
		}

		@Override
		default FunctionalPrimitiveIterable.OfDouble mapToDouble(DoubleUnaryOperator function)
		{
			return new MappingIterable.DoubleToDouble(this, function);
		}

		@Override
		default FunctionalPrimitiveIterable.OfInt mapToInt(DoubleToIntFunction function)
		{
			return new MappingIterable.DoubleToInt(this, function);
		}

		@Override
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

		@Override
		default FunctionalPrimitiveIterable.OfDouble consume()
		{
			return (FunctionalPrimitiveIterable.OfDouble) PrimitiveReducible.OfDouble.super.consume();
		}

		@Override
		default double detect(DoublePredicate predicate)
		{
			return iterator().detect(predicate);
		}

		@Override
		default OptionalDouble reduce(DoubleBinaryOperator accumulator)
		{
			return iterator().reduce(accumulator);
		}

		@Override
		default <T> T reduce(T init, ObjDoubleFunction<T, T> accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		@Override
		default double reduce(double init, DoubleBinaryOperator accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		@Override
		default int reduce(int init, IntDoubleToIntFunction accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		@Override
		default long reduce(long init, LongDoubleToLongFunction accumulator)
		{
			return iterator().reduce(init, accumulator);
		}
	}


	/**
	 * Specialisation for {@code int} of a FunctionalPrimitiveIterable.
	 */
	interface OfInt extends FunctionalPrimitiveIterable<Integer, IntConsumer>, PrimitiveReducible.OfInt<Iterable<? extends Integer>>, PrimitiveIterable.OfInt
	{
		@Override
		FunctionalPrimitiveIterator.OfInt iterator();


		// Transforming Methods

		@Override
		default FunctionalIterable<Integer> concat(Iterable<? extends Integer> iterable)
		{
			if (iterable instanceof PrimitiveIterable.OfInt) {
				return concat((PrimitiveIterable.OfInt) iterable);
			}
			return FunctionalPrimitiveIterable.super.concat(iterable);
		}

		/**
		 * Primitive specialisation of {@code concat} for {@code int}.
		 *
		 * @param iterable the {@link PrimitiveIterable.OfInt} to append
		 * @return an {@link Iterable} that iterates over the elements of the receiver and the argument
		 */
		default FunctionalPrimitiveIterable.OfInt concat(PrimitiveIterable.OfInt iterable)
		{
			return new ChainedIterable.OfInt(new IterableArray.Of<>(this, iterable));
		}

		@Override
		default FunctionalPrimitiveIterable.OfInt dedupe()
		{
			return (FunctionalPrimitiveIterable.OfInt) PrimitiveReducible.OfInt.super.dedupe();
		}

		@Override
		default FunctionalPrimitiveIterable.OfInt distinct()
		{
			return (FunctionalPrimitiveIterable.OfInt) PrimitiveReducible.OfInt.super.distinct();
		}

		@Override
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
		default FunctionalPrimitiveIterable.OfDouble flatMapToDouble(IntFunction<PrimitiveIterable.OfDouble> function)
		{
			return new ChainedIterable.OfDouble(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToInt} for {@code int}.
		 *
		 * @see FunctionalIterable#flatMapToInt(java.util.function.Function)
		 */
		default FunctionalPrimitiveIterable.OfInt flatMapToInt(IntFunction<PrimitiveIterable.OfInt> function)
		{
			return new ChainedIterable.OfInt(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToLong} for {@code int}.
		 *
		 * @see FunctionalIterable#flatMapToLong(java.util.function.Function)
		 */
		default FunctionalPrimitiveIterable.OfLong flatMapToLong(IntFunction<PrimitiveIterable.OfLong> function)
		{
			return new ChainedIterable.OfLong(map(function));
		}

		@Override
		default <T> FunctionalIterable<T> map(IntFunction<? extends T> function)
		{
			return new MappingIterable.IntToObj<>(this, function);
		}

		@Override
		default FunctionalPrimitiveIterable.OfDouble mapToDouble(IntToDoubleFunction function)
		{
			return new MappingIterable.IntToDouble(this, function);
		}

		@Override
		default FunctionalPrimitiveIterable.OfInt mapToInt(IntUnaryOperator function)
		{
			return new MappingIterable.IntToInt(this, function);
		}

		@Override
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

		@Override
		default FunctionalPrimitiveIterable.OfInt consume()
		{
			return (FunctionalPrimitiveIterable.OfInt) PrimitiveReducible.OfInt.super.consume();
		}

		@Override
		default int detect(IntPredicate predicate)
		{
			return iterator().detect(predicate);
		}

		@Override
		default OptionalDouble reduce(DoubleBinaryOperator accumulator)
		{
			return iterator().reduce(accumulator);
		}

		@Override
		default OptionalInt reduce(IntBinaryOperator accumulator)
		{
			return iterator().reduce(accumulator);
		}

		@Override
		default OptionalLong reduce(LongBinaryOperator accumulator)
		{
			return iterator().reduce(accumulator);
		}

		@Override
		default <T> T reduce(T init, ObjIntFunction<T, T> accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		@Override
		default double reduce(double init, DoubleBinaryOperator accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		@Override
		default int reduce(int init, IntBinaryOperator accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		@Override
		default long reduce(long init, LongBinaryOperator accumulator)
		{
			return iterator().reduce(init, accumulator);
		}
	}


	/**
	 * Specialisation for {@code long} of a FunctionalPrimitiveIterable.
	 */
	interface OfLong extends FunctionalPrimitiveIterable<Long, LongConsumer>, PrimitiveReducible.OfLong<Iterable<? extends Long>>, PrimitiveIterable.OfLong
	{
		@Override
		FunctionalPrimitiveIterator.OfLong iterator();



		// Transforming Methods

		@Override
		default FunctionalIterable<Long> concat(Iterable<? extends Long> iterable)
		{
			if (iterable instanceof PrimitiveIterable.OfLong) {
				return concat((PrimitiveIterable.OfLong) iterable);
			}
			return FunctionalPrimitiveIterable.super.concat(iterable);
		}

		/**
		 * Primitive specialisation of {@code concat} for {@code long}.
		 *
		 * @param iterable the {@link PrimitiveIterable.OfLong} to append
		 * @return an {@link Iterable} that iterates over the elements of the receiver and the argument
		 */
		default FunctionalPrimitiveIterable.OfLong concat(PrimitiveIterable.OfLong iterable)
		{
			return new ChainedIterable.OfLong(new IterableArray.Of<>(this, iterable));
		}

		@Override
		default FunctionalPrimitiveIterable.OfLong dedupe()
		{
			return (FunctionalPrimitiveIterable.OfLong) PrimitiveReducible.OfLong.super.dedupe();
		}
		@Override
		default FunctionalPrimitiveIterable.OfLong distinct()
		{
			return (FunctionalPrimitiveIterable.OfLong) PrimitiveReducible.OfLong.super.distinct();
		}
		@Override
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
		default FunctionalPrimitiveIterable.OfDouble flatMapToDouble(LongFunction<PrimitiveIterable.OfDouble> function)
		{
			return new ChainedIterable.OfDouble(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToInt} for {@code long}.
		 *
		 * @see FunctionalIterable#flatMapToInt(java.util.function.Function)
		 */
		default FunctionalPrimitiveIterable.OfInt flatMapToInt(LongFunction<PrimitiveIterable.OfInt> function)
		{
			return new ChainedIterable.OfInt(map(function));
		}

		/**
		 * Primitive specialisation of {@code flatMapToLong} for {@code long}.
		 *
		 * @see FunctionalIterable#flatMapToLong(java.util.function.Function)
		 */
		default FunctionalPrimitiveIterable.OfLong flatMapToLong(LongFunction<PrimitiveIterable.OfLong> function)
		{
			return new ChainedIterable.OfLong(map(function));
		}

		@Override
		default <T> FunctionalIterable<T> map(LongFunction<? extends T> function)
		{
			return new MappingIterable.LongToObj<>(this, function);
		}

		@Override
		default FunctionalPrimitiveIterable.OfDouble mapToDouble(LongToDoubleFunction function)
		{
			return new MappingIterable.LongToDouble(this, function);
		}

		@Override
		default FunctionalPrimitiveIterable.OfInt mapToInt(LongToIntFunction function)
		{
			return new MappingIterable.LongToInt(this, function);
		}

		@Override
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

		@Override
		default FunctionalPrimitiveIterable.OfLong consume()
		{
			return (FunctionalPrimitiveIterable.OfLong) PrimitiveReducible.OfLong.super.consume();
		}

		@Override
		default long detect(LongPredicate predicate)
		{
			return iterator().detect(predicate);
		}

		@Override
		default OptionalLong reduce(LongBinaryOperator accumulator)
		{
			return iterator().reduce(accumulator);
		}

		@Override
		default <T> T reduce(T init, ObjLongFunction<T, T> accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		@Override
		default double reduce(double init, DoubleLongToDoubleFunction accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		@Override
		default int reduce(int init, IntLongToIntFunction accumulator)
		{
			return iterator().reduce(init, accumulator);
		}

		@Override
		default long reduce(long init, LongBinaryOperator accumulator)
		{
			return iterator().reduce(init, accumulator);
		}
	}
}
