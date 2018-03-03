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
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import common.iterable.FilteringIterable.DistinctIterable;
import common.iterable.FunctionalPrimitiveIterable.IterableDouble;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterable.IterableLong;

public interface FunctionalIterable<E> extends Iterable<E>
{
	public class FunctionalWrapper<E, I extends Iterable<E>> implements FunctionalIterable<E>
	{
		protected final I iterable;

		public FunctionalWrapper(I iterable)
		{
			Objects.requireNonNull(iterable);
			this.iterable = iterable;
		}

		@Override
		public FunctionalIterator<E> iterator()
		{
			return FunctionalIterator.extend(iterable.iterator());
		}

		@Override
		public void forEach(Consumer<? super E> action)
		{
			iterable.forEach(action);
		}
	}



	public static <E> FunctionalIterable<E> extend(Iterable<E> iterable)
	{
		if (iterable instanceof FunctionalIterable) {
			return (FunctionalIterable<E>) iterable;
		}
		return new FunctionalWrapper<E, Iterable<E>>(iterable);
	}



	@Override
	FunctionalIterator<E> iterator();

	@Override
	default void forEach(Consumer<? super E> action)
	{
		iterator().forEachRemaining(action);
	}

	// Testing
	default boolean isEmpty()
	{
		return iterator().hasNext();
	}



	// Transforming Methods

	@SuppressWarnings("unchecked")
	default FunctionalIterable<E> chain(Iterable<? extends E>... iterables)
	{
		switch (iterables.length) {
		case 0:
			return this;
		case 1:
			return new ChainedIterable.Of<>(this, iterables[0]);
		default:
			return new ChainedIterable.Of<>(this, new ChainedIterable.Of<>(iterables));
		}
	}

	default FunctionalIterable<E> chain(Iterable<Iterable<? extends E>> iterables)
	{
		return new ChainedIterable.Of<>(this, new ChainedIterable.Of<>(iterables));
	}

	default FunctionalIterable<E> distinct()
	{
		return new DistinctIterable.Of<>(this);
	}

	default FunctionalIterable<E> dedupe()
	{
		return new FunctionalIterable<E>()
		{
			@Override
			public FunctionalIterator<E> iterator()
			{
				return FunctionalIterable.this.iterator().dedupe();
			}
		};
	}

	default FunctionalIterable<E> filter(Predicate<? super E> function)
	{
		return new FilteringIterable.Of<>(this, function);
	}

	default <T> FunctionalIterable<T> flatMap(Function<? super E, ? extends Iterable<? extends T>> function)
	{
		return new ChainedIterable.Of<T>(map(function));
	}

	default <T> IterableDouble flatMapToDouble(Function<? super E, IterableDouble> function)
	{
		return new ChainedIterable.OfDouble(map(function));
	}

	default <T> IterableInt flatMapToInt(Function<? super E, IterableInt> function)
	{
		return new ChainedIterable.OfInt(map(function));
	}

	default <T> IterableLong flatMapToLong(Function<? super E, IterableLong> function)
	{
		return new ChainedIterable.OfLong(map(function));
	}

	default <T> FunctionalIterable<T> map(Function<? super E, ? extends T> function)
	{
		return new MappingIterable.From<>(this, function);
	}

	default IterableDouble mapToDouble(ToDoubleFunction<? super E> function)
	{
		return new MappingIterable.ToDouble<>(this, function);
	}

	default IterableInt mapToInt(ToIntFunction<? super E> function)
	{
		return new MappingIterable.ToInt<>(this, function);
	}

	default IterableLong mapToLong(ToLongFunction<? super E> function)
	{
		return new MappingIterable.ToLong<>(this, function);
	}

	default Iterable<E> nonNull()
	{
		return new FilteringIterable.Of<>(this, Objects::nonNull);
	}



	// Accumulations Methods

	default boolean allMatch(Predicate<? super E> predicate)
	{
		return iterator().allMatch(predicate);
	}

	default boolean anyMatch(Predicate<? super E> predicate)
	{
		return iterator().anyMatch(predicate);
	}

	default String asString()
	{
		return iterator().asString();
	}

	default <C extends Collection<? super E>> C collect(Supplier<? extends C> constructor)
	{
		return iterator().collect(constructor);
	}

	default <C extends Collection<? super E>> C collect(C collection)
	{
		return iterator().collect(collection);
	}

	default E[] collect(E[] array)
	{
		return iterator().collect(array);
	}

	default E[] collect(E[] array, int offset)
	{
		return iterator().collect(array, offset);
	}

	default int collectAndCount(Collection<? super E> collection)
	{
		return iterator().collectAndCount(collection);
	}

	default int collectAndCount(E[] array)
	{
		return iterator().collectAndCount(array);
	}

	default int collectAndCount(E[] array, int offset)
	{
		return iterator().collectAndCount(array, offset);
	}

	default boolean contains(Object obj)
	{
		return iterator().contains(obj);
	}

	default int count()
	{
		return iterator().count();
	}

	default int count(Predicate<? super E> predicate)
	{
		return iterator().count(predicate);
	}

	default Optional<E> detect(Predicate<? super E> predicate)
	{
		return iterator().detect(predicate);
	}

	default Optional<E> reduce(BinaryOperator<E> accumulator)
	{
		return iterator().reduce(accumulator);
	}

	default <T> T reduce(T identity, BiFunction<T, ? super E, T> accumulator)
	{
		return iterator().reduce(identity, accumulator);
	}
}
