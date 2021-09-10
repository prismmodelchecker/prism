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

import java.util.*;
import java.util.function.*;

/**
 * A base type that implements the <em>transformation</em> and <em>accumulation</em> operations from {@link Reducible} for {@link Iterator}s.
 * Additionally, it provides the methods {@link FunctionalIterator#flatMap} and its primitive specializations
 * {@link FunctionalIterator#flatMapToDouble}, {@link FunctionalIterator#flatMapToInt} and {@link FunctionalIterator#flatMapToLong}.
 *
 * @param <E> the type of elements returned by this Iterator
 */
public interface FunctionalIterator<E> extends Reducible<E, Iterator<? extends E>>, Iterator<E>
{
	// Bridging methods to Iterator-based code

	/**
	 * Create a FunctionalIterator over the given elements.
	 *
	 * @param elements the elements
	 * @param <E> the type of the elements
	 * @return a FunctionalIterator over the elements
	 */
	@SafeVarargs
	static <E> FunctionalIterator<E> of(E ... elements)
	{
		switch (elements.length) {
			case 0: return EmptyIterator.of();
			case 1: return new SingletonIterator.Of<>(elements[0]);
			default: return new ArrayIterator.Of<>(elements);
		}
	}

	/**
	 * Create a FunctionalPrimitiveIterator.OfDouble over the given doubles.
	 *
	 * @param numbers the doubles
	 * @return a FunctionalPrimitiveIterator.OfDouble over the doubles
	 */
	static FunctionalPrimitiveIterator.OfDouble ofDouble(double ... numbers)
	{
		switch (numbers.length) {
			case 0: return EmptyIterator.ofDouble();
			case 1: return new SingletonIterator.OfDouble(numbers[0]);
			default: return new ArrayIterator.OfDouble(numbers);
		}
	}

	/**
	 * Create a FunctionalPrimitiveIterator.OfInt over the given ints.
	 *
	 * @param numbers the ints
	 * @return a FunctionalPrimitiveIterator.OfInt over the ints
	 */
	static FunctionalPrimitiveIterator.OfInt ofInt(int ... numbers)
	{
		switch (numbers.length) {
			case 0: return EmptyIterator.ofInt();
			case 1: return new SingletonIterator.OfInt(numbers[0]);
			default: return new ArrayIterator.OfInt(numbers);
		}
	}

	/**
	 * Create a FunctionalPrimitiveIterator.OfLong over the given longs.
	 *
	 * @param numbers the longs
	 * @return a FunctionalPrimitiveIterator.OfLong over the longs
	 */
	static FunctionalPrimitiveIterator.OfLong ofLong(long ... numbers)
	{
		switch (numbers.length) {
			case 0: return EmptyIterator.ofLong();
			case 1: return new SingletonIterator.OfLong(numbers[0]);
			default: return new ArrayIterator.OfLong(numbers);
		}
	}



	// Fundamental methods

	@Override
	default void forEach(Consumer<? super E> action)
	{
		unwrap().forEachRemaining(action);
		release();
	}

	@Override
	default boolean isEmpty()
	{
		return ! hasNext();
	}

	/**
	 * Unwrap a nested iterator if this instance is an adapting wrapper.
	 * Use to avoid repeated indirections, especially in loops.
	 * 
	 * @return {@code this} instance
	 */
	default Iterator<E> unwrap()
	{
		return this;
	}

	/**
	 * Release resources such as wrapped Iterators making this Iterator empty.
	 * Should be called internally after an Iterator is exhausted.
	 */
	default void release()
	{
		// no-op
	}

	/**
	 * Check that there is a next element, throw if not.
	 *
	 * @throws NoSuchElementException if Iterator is exhausted
	 */
	default void requireNext()
	{
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
	}



	// Transforming Methods

	@Override
	default FunctionalIterator<E> concat(Iterator<? extends E> iterator)
	{
		return new ChainedIterator.Of<>(unwrap(), new ArrayIterator.Of<>(iterator));
	}

	@Override
	default FunctionalIterator<E> dedupe()
	{
		return (FunctionalIterator<E>) Reducible.super.dedupe();
	}

	@Override
	default FunctionalIterator<E> distinct()
	{
		return (FunctionalIterator<E>) Reducible.super.distinct();
	}

	@Override
	default FunctionalIterator<E> filter(Predicate<? super E> predicate)
	{
		return new FilteringIterator.Of<>(unwrap(), predicate);
	}

	/**
	 * Map each element to an Iterator and concatenate the obtained Iterators.
	 *
	 * @param function a function that maps each element to an {@link Iterator}
	 */
	default <T> FunctionalIterator<T> flatMap(Function<? super E, ? extends Iterator<? extends T>> function)
	{
		return new ChainedIterator.Of<>(map(function));
	}

	/**
	 * Map each element to a PrimitiveIterator over {@code double} and concatenate the obtained Iterators.
	 *
	 * @param function a function that maps each element to an {@link PrimitiveIterator.OfDouble}
	 */
	default FunctionalPrimitiveIterator.OfDouble flatMapToDouble(Function<? super E, PrimitiveIterator.OfDouble> function)
	{
		return new ChainedIterator.OfDouble(map(function));
	}

	/**
	 * Map each element to a PrimitiveIterator over {@code int} and concatenate the obtained Iterators.
	 *
	 * @param function a function that maps each element to an {@link PrimitiveIterator.OfInt}
	 */
	default FunctionalPrimitiveIterator.OfInt flatMapToInt(Function<? super E, PrimitiveIterator.OfInt> function)
	{
		return new ChainedIterator.OfInt(map(function));
	}

	/**
	 * Map each element to a PrimitiveIterator over {@code long} and concatenate the obtained Iterators.
	 *
	 * @param function a function that maps each element to an {@link PrimitiveIterator.OfLong}
	 */
	default FunctionalPrimitiveIterator.OfLong flatMapToLong(Function<? super E, PrimitiveIterator.OfLong> function)
	{
		return new ChainedIterator.OfLong(map(function));
	}

	@Override
	default <T> FunctionalIterator<T> map(Function<? super E, ? extends T> function)
	{
		return new MappingIterator.ObjToObj<>(unwrap(), function);
	}

	@Override
	default FunctionalPrimitiveIterator.OfDouble mapToDouble(ToDoubleFunction<? super E> function)
	{
		return new MappingIterator.ObjToDouble<>(unwrap(), function);
	}

	@Override
	default FunctionalPrimitiveIterator.OfInt mapToInt(ToIntFunction<? super E> function)
	{
		return new MappingIterator.ObjToInt<>(unwrap(), function);
	}
	@Override
	default FunctionalPrimitiveIterator.OfLong mapToLong(ToLongFunction<? super E> function)
	{
		return new MappingIterator.ObjToLong<>(unwrap(), function);
	}

	@Override
	default FunctionalIterator<E> nonNull()
	{
		return (FunctionalIterator<E>) Reducible.super.nonNull();
	}



	// Accumulations Methods (Consuming)

	@Override
	default FunctionalIterator<E> consume()
	{
		return (FunctionalIterator<E>) Reducible.super.consume();
	}

	@Override
	default E detect(Predicate<? super E> predicate)
	{
		return filter(predicate).next();
	}

	@Override
	default Optional<E> reduce(BinaryOperator<E> accumulator)
	{
		Objects.requireNonNull(accumulator);
		if (! hasNext()) {
			return Optional.empty();
		}
		return Optional.of(reduce(next(), accumulator));
	}

	@Override
	default <T> T reduce(T init, BiFunction<T, ? super E, T> accumulator)
	{
		Objects.requireNonNull(accumulator);
		// avoid redirection in wrappers
		Iterator<E> local = unwrap();
		T result  = init;
		while (local.hasNext()) {
			result = accumulator.apply(result, local.next());
		}
		release();
		return result;
	}

	@Override
	default double reduce(double init, DoubleObjToDoubleFunction<? super E> accumulator)
	{
		Objects.requireNonNull(accumulator);
		// avoid redirection in wrappers
		Iterator<E> local = unwrap();
		double result = init;
		while (local.hasNext()) {
			result = accumulator.applyAsDouble(result, local.next());
		}
		release();
		return result;
	}

	@Override
	default int reduce(int init, IntObjToIntFunction<? super E> accumulator)
	{
		Objects.requireNonNull(accumulator);
		// avoid redirection in wrappers
		Iterator<E> local = unwrap();
		int result = init;
		while (local.hasNext()) {
			result = accumulator.applyAsInt(result, local.next());
		}
		release();
		return result;
	}

	@Override
	default long reduce(long init, LongObjToLongFunction<? super E> accumulator)
	{
		Objects.requireNonNull(accumulator);
		// avoid redirection in wrappers
		Iterator<E> local = unwrap();
		long result = init;
		while (local.hasNext()) {
			result = accumulator.applyAsLong(result, local.next());
		}
		release();
		return result;
	}
}
