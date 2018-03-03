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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import common.functions.ObjDoubleFunction;
import common.functions.ObjIntFunction;
import common.functions.ObjLongFunction;

public interface FunctionalIterator<E> extends Iterator<E>
{
	public abstract class FunctionalWrapper<E, I extends Iterator<E>> implements FunctionalIterator<E>
	{
		protected I iterator;

		public FunctionalWrapper(I iterator)
		{
			Objects.requireNonNull(iterator);
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext()
		{
			if(iterator.hasNext()) {
				return true;
			}
			release();
			return false;
		}

		@Override
		public E next()
		{
			return iterator.next();
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action)
		{
			iterator.forEachRemaining(action);
		}

		/**
		 * Unwrap a nested iterator if this instance is a decorating wrapper.
		 * Use to avoid repeated indirections, especially in loops.
		 * 
		 * @return {@code this} instance or the wrapped iterator.
		 */
		@Override
		public I unwrap()
		{
			return iterator;
		}

		protected abstract void release();



		public static class Of<E> extends FunctionalWrapper<E, Iterator<E>>
		{
			public Of(Iterator<E> iterator)
			{
				super(iterator);
			}

			@Override
			protected void release()
			{
				iterator = EmptyIterator.Of();
			}
		}



		public static class OfDouble extends FunctionalWrapper<Double, PrimitiveIterator.OfDouble> implements FunctionalPrimitiveIterator.OfDouble
		{
			public OfDouble(PrimitiveIterator.OfDouble iterator)
			{
				super(iterator);
			}

			@Override
			public double nextDouble()
			{
				return iterator.nextDouble();
			}

			@Override
			public void forEachRemaining(DoubleConsumer action)
			{
				iterator.forEachRemaining(action);
				release();
			}

			@Override
			public <T> T reduce(T identity, ObjDoubleFunction<T, T> accumulator)
			{
				Objects.requireNonNull(accumulator);
				// faster access to local variable than to field
				PrimitiveIterator.OfDouble local = iterator;
				T result                         = identity;
				while(local.hasNext()) {
					result = accumulator.apply(result, local.nextDouble());
				}
				release();
				return result;
			}

			@Override
			public double reduce(double identity, DoubleBinaryOperator accumulator)
			{
				Objects.requireNonNull(accumulator);
				// faster access to local variable than to field
				PrimitiveIterator.OfDouble local = iterator;
				double result                    = identity;
				while(local.hasNext()) {
					result = accumulator.applyAsDouble(result, local.nextDouble());
				}
				release();
				return result;
			}

			@Override
			public PrimitiveIterator.OfDouble unwrap()
			{
				return iterator;
			}

			@Override
			protected void release()
			{
				iterator = EmptyIterator.OfDouble();
			}
		}



		public static class OfInt extends FunctionalWrapper<Integer, PrimitiveIterator.OfInt> implements FunctionalPrimitiveIterator.OfInt
		{
			public OfInt(PrimitiveIterator.OfInt iterator)
			{
				super(iterator);
			}

			@Override
			public int nextInt()
			{
				return iterator.nextInt();
			}

			@Override
			public void forEachRemaining(IntConsumer action)
			{
				iterator.forEachRemaining(action);
				release();
			}

			@Override
			public <T> T reduce(T identity, ObjIntFunction<T, T> accumulator)
			{
				Objects.requireNonNull(accumulator);
				// faster access to local variable than to field
				PrimitiveIterator.OfInt local = iterator;
				T result                      = identity;
				while(local.hasNext()) {
					result = accumulator.apply(result, local.nextInt());
				}
				release();
				return result;
			}

			@Override
			public int reduce(int identity, IntBinaryOperator accumulator)
			{
				Objects.requireNonNull(accumulator);
				// faster access to local variable than to field
				PrimitiveIterator.OfInt local = iterator;
				int result                    = identity;
				while(local.hasNext()) {
					result = accumulator.applyAsInt(result, local.nextInt());
				}
				release();
				return result;
			}

			@Override
			public PrimitiveIterator.OfInt unwrap()
			{
				return iterator;
			}

			@Override
			protected void release()
			{
				iterator = EmptyIterator.OfInt();
			}
		}



		public static class OfLong extends FunctionalWrapper<Long, PrimitiveIterator.OfLong> implements FunctionalPrimitiveIterator.OfLong
		{
			public OfLong(PrimitiveIterator.OfLong iterator)
			{
				super(iterator);
			}

			@Override
			public long nextLong()
			{
				return iterator.nextLong();
			}

			@Override
			public void forEachRemaining(LongConsumer action)
			{
				iterator.forEachRemaining(action);
				release();
			}

			@Override
			public <T> T reduce(T identity, ObjLongFunction<T, T> accumulator)
			{
				Objects.requireNonNull(accumulator);
				// faster access to local variable than to field
				PrimitiveIterator.OfLong local = iterator;
				T result                       = identity;
				while(local.hasNext()) {
					result = accumulator.apply(result, local.nextLong());
				}
				release();
				return result;
			}

			@Override
			public long reduce(long identity, LongBinaryOperator accumulator)
			{
				Objects.requireNonNull(accumulator);
				// faster access to local variable than to field
				PrimitiveIterator.OfLong local = iterator;
				long result                    = identity;
				while(local.hasNext()) {
					result = accumulator.applyAsLong(result, local.nextLong());
				}
				release();
				return result;
			}

			@Override
			public PrimitiveIterator.OfLong unwrap()
			{
				return iterator;
			}

			@Override
			protected void release()
			{
				iterator = EmptyIterator.OfLong();
			}
		}
	}



	public static <E> FunctionalIterator<E> extend(Iterable<E> iterable)
	{
		return extend(iterable.iterator());
	}

	@SuppressWarnings("unchecked")
	public static <E> FunctionalIterator<E> extend(Iterator<E> iterator)
	{
		if (iterator instanceof FunctionalIterator) {
			return (FunctionalIterator<E>) iterator;
		}
		if (iterator instanceof PrimitiveIterator.OfDouble) {
			return (FunctionalIterator<E>) extend((PrimitiveIterator.OfDouble) iterator);
		}
		if (iterator instanceof PrimitiveIterator.OfInt) {
			return (FunctionalIterator<E>) extend((PrimitiveIterator.OfInt) iterator);
		}
		if (iterator instanceof PrimitiveIterator.OfLong) {
			return (FunctionalIterator<E>) extend((PrimitiveIterator.OfLong) iterator);
		}
		return new FunctionalWrapper.Of<>(iterator);
	}

	public static FunctionalPrimitiveIterator.OfDouble extend(PrimitiveIterator.OfDouble iterator)
	{
		if (iterator instanceof FunctionalPrimitiveIterator.OfDouble) {
			return (FunctionalPrimitiveIterator.OfDouble) iterator;
		}
		return new FunctionalWrapper.OfDouble(iterator);
	}

	public static FunctionalPrimitiveIterator.OfInt extend(PrimitiveIterator.OfInt iterator)
	{
		if (iterator instanceof FunctionalPrimitiveIterator.OfInt) {
			return (FunctionalPrimitiveIterator.OfInt) iterator;
		}
		return new FunctionalWrapper.OfInt(iterator);
	}

	public static FunctionalPrimitiveIterator.OfLong extend(PrimitiveIterator.OfLong iterator)
	{
		if (iterator instanceof FunctionalPrimitiveIterator.OfLong) {
			return (FunctionalPrimitiveIterator.OfLong) iterator;
		}
		return new FunctionalWrapper.OfLong(iterator);
	}

	/**
	 * Unwrap a nested iterator if this instance is a decorating wrapper.
	 * Use to avoid repeated indirections, especially in loops.
	 * 
	 * @return {@code this} instance or the wrapped iterator.
	 */
	default Iterator<E> unwrap()
	{
		return this;
	}



	// Transforming Methods

	@SuppressWarnings("unchecked")
	default FunctionalIterator<E> concat(Iterator<? extends E>... iterators)
	{
		if (iterators.length == 0) {
			return this;
		}
		return new ChainedIterator.Of<>(unwrap(), iterators);
	}

	default FunctionalIterator<E> concat(Iterator<Iterator<? extends E>> iterators)
	{
		return new ChainedIterator.Of<>(unwrap(), iterators);
	}

	/**
	 * Obtain filtering iterator for the given iterator,
	 * filtering duplicate elements (via HashSet, requires
	 * that {@code equals()} and {@code hashCode()} are properly
	 * implemented).
	 */
	default FunctionalIterator<E> distinct()
	{
		Set<E> set = new HashSet<>();
		return new FilteringIterator.Of<>(unwrap(), set::add);
	}

	/**
	 * Obtain filtering iterator for the given iterator,
	 * filtering consecutive duplicate elements.
	 */
	default FunctionalIterator<E> dedupe()
	{
		Predicate<E> unseen = new Predicate<E>()
		{
			Object previous = new Object();

			@Override
			public boolean test(E obj)
			{
				if (Objects.equals(previous, obj)) {
					return false;
				}
				previous = obj;
				return true;
			}
		};
		return new FilteringIterator.Of<>(unwrap(), unseen);
	}

	default FunctionalIterator<E> filter(Predicate<? super E> function)
	{
		return new FilteringIterator.Of<>(unwrap(), function);
	}

	default <T> FunctionalIterator<T> flatMap(Function<? super E, ? extends Iterator<? extends T>> function)
	{
		return new ChainedIterator.Of<>(map(function));
	}

	default <T> FunctionalPrimitiveIterator.OfDouble flatMapToDouble(Function<? super E, PrimitiveIterator.OfDouble> function)
	{
		return new ChainedIterator.OfDouble(map(function));
	}

	default <T> FunctionalPrimitiveIterator.OfInt flatMapToInt(Function<? super E, PrimitiveIterator.OfInt> function)
	{
		return new ChainedIterator.OfInt(map(function));
	}

	default <T> FunctionalPrimitiveIterator.OfLong flatMapToLong(Function<? super E, PrimitiveIterator.OfLong> function)
	{
		return new ChainedIterator.OfLong(map(function));
	}

	default <T> FunctionalIterator<T> map(Function<? super E, ? extends T> function)
	{
		return new MappingIterator.From<>(unwrap(), function);
	}

	default FunctionalPrimitiveIterator.OfDouble mapToDouble(ToDoubleFunction<? super E> function)
	{
		return new MappingIterator.ToDouble<>(unwrap(), function);
	}

	default FunctionalPrimitiveIterator.OfInt mapToInt(ToIntFunction<? super E> function)
	{
		return new MappingIterator.ToInt<>(unwrap(), function);
	}

	default FunctionalPrimitiveIterator.OfLong mapToLong(ToLongFunction<? super E> function)
	{
		return new MappingIterator.ToLong<>(unwrap(), function);
	}

	default FunctionalIterator<E> nonNull()
	{
		return new FilteringIterator.Of<>(unwrap(), Objects::nonNull);
	}



	// Accumulations Methods (Consuming)

	default boolean allMatch(Predicate<? super E> predicate)
	{
		return ! anyMatch(predicate.negate());
	}

	default boolean anyMatch(Predicate<? super E> predicate)
	{
		return detect(predicate).isPresent();
	}

	default String asString()
	{
		StringBuffer buffer = new StringBuffer("{");
		if (hasNext()) {
			buffer.append(next());
		}
		forEachRemaining(each -> buffer.append(", ").append(each));
		return buffer.append("}").toString();
	}

	default <C extends Collection<? super E>> C collect(Supplier<? extends C> constructor)
	{
		C collection = constructor.get();
		collect(collection);
		return collection;
	}

	default <C extends Collection<? super E>> C collect(C collection)
	{
		forEachRemaining(collection::add);
//		collectAndCount(collection);
		return collection;
	}

	default E[] collect(E[] array)
	{
		return collect(array, 0);
	}

	default E[] collect(E[] array, int offset)
	{
		collectAndCount(array, offset);
		return array;
	}

	default int collectAndCount(Collection<? super E> collection)
	{
		// avoid redirection in wrappers
		Iterator<E> local = unwrap();
		// exploit arithmetic operator
		int count = 0;
		while (local.hasNext()) {
			collection.add(local.next());
			count++;
		}
		return count;
	}

	default int collectAndCount(E[] array)
	{
		return collectAndCount(array, 0);
	}

	default int collectAndCount(E[] array, int offset)
	{
		// avoid redirection in wrappers
		Iterator<E> local = unwrap();
		// avoid auto-boxing of array index
		int count = offset;
		while (local.hasNext()) {
			array[count++] = local.next();
		}
		return count - offset;
	}

	default boolean contains(Object obj)
	{
		return anyMatch((obj == null) ? Objects::isNull : obj::equals);
	}

	default int count()
	{
		// avoid redirection in wrappers
		Iterator<E> local = unwrap();
		// exploit arithmetic operator
		int count = 0;
		while (local.hasNext()) {
			local.next();
			count++;
		}
		return count;
	}

	default int count(Predicate<? super E> predicate)
	{
		return filter(predicate).count();
	}

	default Optional<E> detect(Predicate<? super E> predicate)
	{
		FunctionalIterator<E> filtered = filter(predicate);
		return filtered.hasNext() ? Optional.of(filtered.next()) : Optional.empty();
	}

	default Optional<E> reduce(BinaryOperator<E> accumulator)
	{
		if (! hasNext()) {
			return Optional.empty();
		}
		return Optional.of(reduce(next(), accumulator));
	}

	default <T> T reduce(T identity, BiFunction<T, ? super E, T> accumulator)
	{
		Objects.requireNonNull(accumulator);
		// avoid redirection in wrappers
		Iterator<E> local = unwrap();
		T result          = identity;
		while(local.hasNext()) {
			result = accumulator.apply(result, local.next());
		}
		return result;
	}
}
