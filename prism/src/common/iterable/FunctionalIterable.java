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

import common.functions.DoubleObjToDoubleFunction;
import common.functions.IntObjToIntFunction;
import common.functions.LongObjToLongFunction;

import java.util.Optional;
import java.util.function.*;

/**
 * A base type that implements the <em>transformation</em> and <em>accumulation</em> operations from {@link Reducible} for {@link Iterable}s.
 * Additionally, it provides the methods {@link FunctionalIterable#flatMap} and its primitive specializations
 * {@link FunctionalIterable#flatMapToDouble}, {@link FunctionalIterable#flatMapToInt} and {@link FunctionalIterable#flatMapToLong}.
 *
 * @param <E> the type of elements returned by this Iterable
 */
public interface FunctionalIterable<E> extends Reducible<E, Iterable<? extends E>>, Iterable<E>
{
	// Bridging methods to Iterator-based code

	/**
	 * Create a FunctionalIterable over the given elements.
	 *
	 * @param elements the elements
	 * @param <E> the type of the elements
	 * @return a FunctionalIterable over the elements
	 */
	@SafeVarargs
	static <E> FunctionalIterable<E> of(E ... elements)
	{
		switch (elements.length) {
			case 0: return EmptyIterable.of();
			case 1: return new SingletonIterable.Of<>(elements[0]);
			default: return new IterableArray.Of<>(elements);
		}
	}

	/**
	 * Create a FunctionalPrimitiveIterable.OfDouble over the given doubles.
	 *
	 * @param numbers the doubles
	 * @return a FunctionalPrimitiveIterable.OfDouble over the doubles
	 */
	static FunctionalPrimitiveIterable.OfDouble ofDouble(double ... numbers)
	{
		switch (numbers.length) {
			case 0: return EmptyIterable.ofDouble();
			case 1: return new SingletonIterable.OfDouble(numbers[0]);
			default: return new IterableArray.OfDouble(numbers);
		}
	}

	/**
	 * Create a FunctionalPrimitiveIterable.OfInt over the given ints.
	 *
	 * @param numbers the ints
	 * @return a FunctionalPrimitiveIterable.OfInt over the ints
	 */
	static FunctionalPrimitiveIterable.OfInt ofInt(int ... numbers)
	{
		switch (numbers.length) {
			case 0: return EmptyIterable.ofInt();
			case 1: return new SingletonIterable.OfInt(numbers[0]);
			default: return new IterableArray.OfInt(numbers);
		}
	}

	/**
	 * Create a FunctionalPrimitiveIterable.OfLong over the given longs.
	 *
	 * @param numbers the longs
	 * @return a FunctionalPrimitiveIterable.OfLong over the longs
	 */
	static FunctionalPrimitiveIterable.OfLong ofLong(long ... numbers)
	{
		switch (numbers.length) {
			case 0: return EmptyIterable.ofLong();
			case 1: return new SingletonIterable.OfLong(numbers[0]);
			default: return new IterableArray.OfLong(numbers);
		}
	}



	// Fundamental methods

	@Override
	FunctionalIterator<E> iterator();

	@Override
	default void forEach(Consumer<? super E> action)
	{
		iterator().forEach(action);
	}

	@Override
	default boolean isEmpty()
	{
		return !iterator().hasNext();
	}



	// Transforming Methods

	@Override
	default FunctionalIterable<E> concat(Iterable<? extends E> iterable)
	{
		return new ChainedIterable.Of<>(new IterableArray.Of<>(this, iterable));
	}

	@Override
	default FunctionalIterable<E> dedupe()
	{
		return (FunctionalIterable<E>) Reducible.super.dedupe();
	}

	@Override
	default FunctionalIterable<E> distinct()
	{
		return (FunctionalIterable<E>) Reducible.super.distinct();
	}

	@Override
	default FunctionalIterable<E> filter(Predicate<? super E> predicate)
	{
		return new FilteringIterable.Of<>(this, predicate);
	}

	/**
	 * Map each element to an Iterable and concatenate the obtained Iterables.
	 *
	 * @param function a function that maps each element to an {@link Iterable}
	 */
	default <T> FunctionalIterable<T> flatMap(Function<? super E, ? extends Iterable<? extends T>> function)
	{
		return new ChainedIterable.Of<>(map(function));
	}

	/**
	 * Map each element to an FunctionalPrimitiveIterable.OfDouble and concatenate the obtained Iterables.
	 *
	 * @param function a function that maps each element to an {@link FunctionalPrimitiveIterable.OfDouble}
	 */
	default FunctionalPrimitiveIterable.OfDouble flatMapToDouble(Function<? super E, PrimitiveIterable.OfDouble> function)
	{
		return new ChainedIterable.OfDouble(map(function));
	}

	/**
	 * Map each element to an FunctionalPrimitiveIterable.OfInt and concatenate the obtained Iterables.
	 *
	 * @param function a function that maps each element to an {@link FunctionalPrimitiveIterable.OfInt}
	 */
	default FunctionalPrimitiveIterable.OfInt flatMapToInt(Function<? super E, PrimitiveIterable.OfInt> function)
	{
		return new ChainedIterable.OfInt(map(function));
	}

	/**
	 * Map each element to an FunctionalPrimitiveIterable.OfLong and concatenate the obtained Iterables.
	 *
	 * @param function a function that maps each element to an {@link FunctionalPrimitiveIterable.OfLong}
	 */
	default FunctionalPrimitiveIterable.OfLong flatMapToLong(Function<? super E, PrimitiveIterable.OfLong> function)
	{
		return new ChainedIterable.OfLong(map(function));
	}

	@Override
	default <T> FunctionalIterable<T> map(Function<? super E, ? extends T> function)
	{
		return new MappingIterable.ObjToObj<>(this, function);
	}

	@Override
	default FunctionalPrimitiveIterable.OfDouble mapToDouble(ToDoubleFunction<? super E> function)
	{
		return new MappingIterable.ObjToDouble<>(this, function);
	}

	@Override
	default FunctionalPrimitiveIterable.OfInt mapToInt(ToIntFunction<? super E> function)
	{
		return new MappingIterable.ObjToInt<>(this, function);
	}

	@Override
	default FunctionalPrimitiveIterable.OfLong mapToLong(ToLongFunction<? super E> function)
	{
		return new MappingIterable.ObjToLong<>(this, function);
	}

	@Override
	default FunctionalIterable<E> nonNull()
	{
		return (FunctionalIterable<E>) Reducible.super.nonNull();
	}



	// Accumulations Methods (Consuming)

	@Override
	default FunctionalIterable<E> consume()
	{
		return (FunctionalIterable<E>) Reducible.super.consume();
	}

	@Override
	default E detect(Predicate<? super E> predicate)
	{
		return iterator().detect(predicate);
	}

	@Override
	default Optional<E> reduce(BinaryOperator<E> accumulator)
	{
		return iterator().reduce(accumulator);
	}

	@Override
	default <T> T reduce(T init, BiFunction<T, ? super E, T> accumulator)
	{
		return iterator().reduce(init, accumulator);
	}

	@Override
	default double reduce(double init, DoubleObjToDoubleFunction<? super E> accumulator)
	{
		return iterator().reduce(init, accumulator);
	}

	@Override
	default int reduce(int init, IntObjToIntFunction<? super E> accumulator)
	{
		return iterator().reduce(init, accumulator);
	}

	@Override
	default long reduce(long init, LongObjToLongFunction<? super E> accumulator)
	{
		return iterator().reduce(init, accumulator);
	}
}
