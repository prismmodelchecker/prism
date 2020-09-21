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

import common.functions.DoubleObjToDoubleFunction;
import common.functions.IntObjToIntFunction;
import common.functions.LongObjToLongFunction;
import common.iterable.FunctionalPrimitiveIterable.IterableDouble;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterable.IterableLong;
import common.iterable.MappingIterable.ObjToDouble;
import common.iterable.MappingIterable.ObjToInt;
import common.iterable.MappingIterable.ObjToLong;

/**
 * An {@link Iterable} that provides <em>transformation</em> and <em>accumulation</em> operations similar to {@link java.util.stream.Stream Stream}.
 * It servers as a bridge between Iterator-based code and the functional API provided by Java streams.
 * The following example illustrates is usage.
 *
 * <pre>
 * List&lt;String>  allFibonacci = List.of("0", "1", "1", "2", "3", "5", "8", "13");
 * List&lt;Integer> oddFibonacci = FunctionalIterable.extend(AllFibonacci)
 *                                   .mapToInt(Integer::parseInt)
 *                                   .filter((int i) -> i % 2 != 0)
 *                                   .collect(ArrayList::new);
 * </pre>
 * The code computes the odd Fibonacci numbers from a {@code List} of {@code String} representations:
 * <ol>
 * <li>Extend {@code List} with the methods of this interface.
 * <li>Map the String representations to their respective {@code int} values
 * <li>Filter all odd numbers
 * <li>Collect the numbers in a new {@code ArrayList}
 * </ol>
 * Please note that the {@code mapToInt} and {@code} only wrap the underlying Iterable.
 * The actual computation happens on-the-fly when {@code collect} is called (or when an Iterator is obtained and consumed).
 *
 * <p><em>Transformation operations</em> (known as <a href="package-summary.html#StreamOps">intermediate operations</a> on streams)
 * <em>transform the sequence of elements</em>, e.g., by filtering with a predicate or by mapping elements using a function.
 *
 * <p><em>Accumulation operations</em> (known as <a href="package-summary.html#StreamOps">terminal operations</a> on streams)
 * <em>consume the elements</em> and <em>compute a result</em>, e.g., by testing whether all elements match a predicate or by collection all elements in an array.
 * The most general accumulation operation is {@link FunctionalIterable#reduce(Object, BinaryOperator) reduce}.
 * To enable an efficient treatment of the primitive types {@code double}, {@code int} and {@code long}, reduce is overloaded.
 * Please note that implementors of the interface may not consume any elements if the result of an accumulation can be computed directly.
 *
 * <p>The default implementations of this interface just wrap an Iterable such that the actual transformation or accumulation is performed by a corresponding Iterator.
 * This implies that an underlying Iterable is not modified.
 *
 * @param <E> the type of elements returned by this Iterable
 * @see java.util.stream.Stream Stream
 * @see FunctionalIterator
 */
public interface FunctionalIterable<E> extends Iterable<E>
{
	/**
	 * Extend an Iterable with the methods provided by FunctionalIterable.
	 * Answer the argument if it already implements {@code FunctionalIterable}.
	 *
	 * @param iterable the {@link Iterable} to extend
	 * @return a {@link FunctionalIterable} that is either the argument or an adaptor on the argument
	 */
	public static <E> FunctionalIterable<E> extend(Iterable<E> iterable)
	{
		if (iterable instanceof FunctionalIterable) {
			return (FunctionalIterable<E>) iterable;
		}
		return new IterableAdaptor<E>(iterable);
	}

	/**
	 * Convert an Iterable&lt;Double&gt; to a IterableDouble by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterable if it already implements of IterableDouble.
	 *
	 * @param iterable the {@link Iterable}&lt;Double&gt; to extend
	 * @return a {@link IterableDouble} that is either the argument or an adaptor on the argument
	 */
	public static IterableDouble unboxDouble(Iterable<Double> iterable)
	{
		if (iterable instanceof IterableDouble) {
			return (IterableDouble) iterable;
		}
		return new ObjToDouble<>(iterable, Double::doubleValue);
	}

	/**
	 * Convert an Iterable&lt;Integer&gt; to a IterableInt by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterable if it already implements of IterableInt.
	 *
	 * @param iterable the {@link Iterable}&lt;Integer&gt; to extend
	 * @return a {@link IterableInt} that is either the argument or an adaptor on the argument
	 */
	public static IterableInt unboxInt(Iterable<Integer> iterable)
	{
		if (iterable instanceof IterableInt) {
			return (IterableInt) iterable;
		}
		return new ObjToInt<>(iterable, Integer::intValue);
	}

	/**
	 * Convert an Iterable&lt;Long&gt; to a IterableLong by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterable if it already implements of IterableLong.
	 *
	 * @param iterable the {@link Iterable}&lt;Long&gt; to extend
	 * @return a {@link IterableLong} that is either the argument or an adaptor on the argument
	 */
	public static IterableLong unboxLong(Iterable<Long> iterable)
	{
		if (iterable instanceof IterableLong) {
			return (IterableLong) iterable;
		}
		return new ObjToLong<>(iterable, Long::longValue);
	}

	@Override
	FunctionalIterator<E> iterator();

	@Override
	default void forEach(Consumer<? super E> action)
	{
		iterator().forEachRemaining(action);
	}

	/**
	 * Test whether the receiver is empty.
	 *
	 * @return {@code true} iff the receiver is empty
	 */
	default boolean isEmpty()
	{
		return !iterator().hasNext();
	}



	// Transforming Methods

	/**
	 * Concatenate this Iterable and the arguments.
	 * The Iterator of the returned Iterable first iterates over the elements of the receiver and then over the elements of each argument.
	 *
	 * @param iterables an array of {@link Iterable}s to append
	 * @return an {@link Iterable} over the elements of the receiver and the arguments
	 */
	@SuppressWarnings("unchecked")
	default FunctionalIterable<E> concat(Iterable<? extends E>... iterables)
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

	/**
	 * Remove duplicate elements from the receiver such that only one occurrence is retained.
	 * Duplicates are identified in terms of {@link Object#equals} which requires a proper implementation of {@link Object#hashCode}.
	 */
	default FunctionalIterable<E> distinct()
	{
		return filter(new Distinct.Of<>());
	}

	/**
	 * Remove consecutive duplicate elements such that only the first occurrence in a sequence is retained.
	 * Duplicates are identified in terms of {@link Object#equals}.
	 */
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

	/**
	 * Filter the receiver by a predicate, i.e., only elements that match the predicate are kept.
	 *
	 * @param predicate a predicate evaluating to {@code true} for the elements to keep
	 */
	default FunctionalIterable<E> filter(Predicate<? super E> function)
	{
		return new FilteringIterable.Of<>(this, function);
	}

	/**
	 * Map each element to an Iterable and concatenate the obtained Iterables.
	 *
	 * @param function a function that maps each element to an {@link Iterable}
	 */
	default <T> FunctionalIterable<T> flatMap(Function<? super E, ? extends Iterable<? extends T>> function)
	{
		return new ChainedIterable.Of<T>(map(function));
	}

	/**
	 * Map each element to an IterableDouble and concatenate the obtained Iterables.
	 *
	 * @param function a function that maps each element to an {@link IterableDouble}
	 */
	default <T> IterableDouble flatMapToDouble(Function<? super E, IterableDouble> function)
	{
		return new ChainedIterable.OfDouble(map(function));
	}

	/**
	 * Map each element to an IterableInt and concatenate the obtained Iterables.
	 *
	 * @param function a function that maps each element to an {@link IterableInt}
	 */
	default <T> IterableInt flatMapToInt(Function<? super E, IterableInt> function)
	{
		return new ChainedIterable.OfInt(map(function));
	}

	/**
	 * Map each element to an IterableLong and concatenate the obtained Iterables.
	 *
	 * @param function a function that maps each element to an {@link IterableLong}
	 */
	default <T> IterableLong flatMapToLong(Function<? super E, IterableLong> function)
	{
		return new ChainedIterable.OfLong(map(function));
	}

	/**
	 * Map each element.
	 *
	 * @param function a mapping function that is applied to each element
	 */
	default <T> FunctionalIterable<T> map(Function<? super E, ? extends T> function)
	{
		return new MappingIterable.ObjToObj<>(this, function);
	}

	/**
	 * Map each element to a {@code double}.
	 *
	 * @param function a mapping function that is applied to each element
	 */
	default IterableDouble mapToDouble(ToDoubleFunction<? super E> function)
	{
		return new MappingIterable.ObjToDouble<>(this, function);
	}

	/**
	 * Map each element to a {@code int}.
	 *
	 * @param function a mapping function that is applied to each element
	 */
	default IterableInt mapToInt(ToIntFunction<? super E> function)
	{
		return new MappingIterable.ObjToInt<>(this, function);
	}

	/**
	 * Map each element to a {@code long}.
	 *
	 * @param function a mapping function that is applied to each element
	 */
	default IterableLong mapToLong(ToLongFunction<? super E> function)
	{
		return new MappingIterable.ObjToLong<>(this, function);
	}

	/**
	 * Remove all {@code null} elements.
	 */
	default Iterable<E> nonNull()
	{
		return new FilteringIterable.Of<>(this, Objects::nonNull);
	}



	// Accumulations Methods (Consuming)

	/**
	 * Test whether each element matches a predicate.
	 *
	 * @param predicate a {@link Predicate} to test the element against
	 * @return {@code true} iff the predicate evaluates to {@code true} for each element
	 */
	default boolean allMatch(Predicate<? super E> predicate)
	{
		return iterator().allMatch(predicate);
	}

	/**
	 * Test whether any element matches a predicate.
	 * Return {@code true} after the first match without testing the remaining elements.
	 *
	 * @param predicate a {@link Predicate} to test the elements against
	 * @return {@code true} iff the predicate evaluates to {@code true} for any element
	 */
	default boolean anyMatch(Predicate<? super E> predicate)
	{
		return iterator().anyMatch(predicate);
	}

	/**
	 * Test whether no element matches a predicate.
	 * Return {@code false} after the first match without testing the remaining elements.
	 *
	 * @param predicate a {@link Predicate} to test the elements against
	 * @return {@code false} iff the predicate evaluates to {@code true} for any element
	 */
	default boolean noneMatch(Predicate<? super E> predicate)
	{
		return iterator().noneMatch(predicate);
	}

	/**
	 * Build an array-like string that lists the string representation {@link Object#toString} of each element.
	 *
	 * @return a String of the form "[e_1, e_2, ... , e_n]"
	 */
	default String asString()
	{
		return iterator().asString();
	}

	/**
	 * Add each element to a newly instantiated {@code Collection} supplied by the argument using {@link Collection#add(Object)}.
	 *
	 * @param supplier a function yielding a new Collection
	 * @return the new Collection with all elements added to it
	 * @see java.util.stream.Stream#collect(Supplier, java.util.function.BiConsumer, java.util.function.BiConsumer) Stream.collect(Supplier, BiConsumer, BiConsumer)
	 */
	default <C extends Collection<? super E>> C collect(Supplier<? extends C> constructor)
	{
		return iterator().collect(constructor);
	}

	/**
	 * Add each element to the argument {@code Collection} using {@link Collection#add(Object)}.
	 *
	 * @param collection a {@link Collection}
	 * @return the Collection with all elements added to it
	 */
	default <C extends Collection<? super E>> C collect(C collection)
	{
		return iterator().collect(collection);
	}

	/**
	 * Store each element in an array starting at index 0.
	 *
	 * @param array an Array of the receiver's element type
	 * @return the array with all elements stored in it
	 */
	default E[] collect(E[] array)
	{
		return iterator().collect(array);
	}

	/**
	 * Store each element in an array starting at index {@code offset}.
	 *
	 * @param array an Array of the receiver's element type
	 * @param offset an index (inclusive) from which on the elements are stored
	 * @return the array with all elements stored in it
	 */
	default E[] collect(E[] array, int offset)
	{
		return iterator().collect(array, offset);
	}

	/**
	 * Add each element to the argument {@code Collection} using {@link Collection#add(Object)} and count the number of elements added.
	 *
	 * @param collection a {@link Collection}
	 * @return the number of elements added to the Collection
	 */
	default long collectAndCount(Collection<? super E> collection)
	{
		return iterator().collectAndCount(collection);
	}

	/**
	 * Store each element in an array starting at index 0 and count the number of elements stored.
	 *
	 * @param array an Array of the receiver's element type
	 * @return the number of elements stored in the array
	 */
	default int collectAndCount(E[] array)
	{
		return iterator().collectAndCount(array);
	}

	/**
	 * Store each element in an array starting at index {@code offset} and count the number of elements stored.
	 *
	 * @param array an Array of the receiver's element type
	 * @param offset an index (inclusive) from which on the elements are stored
	 * @return the number of elements stored in the array
	 */
	default int collectAndCount(E[] array, int offset)
	{
		return iterator().collectAndCount(array, offset);
	}

	/**
	 * Collect distinct elements from the receiver such that only one occurrence of each element is retained.
	 * Duplicates are identified in terms of {@link Object#equals} which requires a proper implementation of {@link Object#hashCode}.
	 *
	 * @return a {@link FunctionalIterable} that contains each distinct of the receiver exactly once
	 * @see FunctionalIterable#distinct()
	 */
	default FunctionalIterable<E> collectDistinct()
	{
		return iterator().collectDistinct();
	}

	/**
	 * Test in terms of {@link Object#equals} whether an object (or {@code null}) is one of the receiver's elements.
	 *
	 * @param obj
	 * @return {@code true} if the argument is contained in the receiver
	 */
	default boolean contains(Object obj)
	{
		return iterator().contains(obj);
	}

	/**
	 * Count the number of elements in the receiver.
	 *
	 * @return the number of elements
	 */
	default long count()
	{
		return iterator().count();
	}

	/**
	 * Count the number of elements that match a predicate.
	 *
	 * @param predicate a {@link Predicate} evaluating to {@code true} for the elements to count
	 * @return the number of elements matching the predicate
	 */
	default long count(Predicate<? super E> predicate)
	{
		return iterator().count(predicate);
	}

	/**
	 * Find the first element that matches a predicate.
	 *
	 * @param predicate a {@link Predicate} evaluating to {@code true} for the element to return
	 * @return an {@link Optional} either containing a matching element or being empty if no matching element was found
	 */
	default Optional<E> detect(Predicate<? super E> predicate)
	{
		return iterator().detect(predicate);
	}

	/**
	 * Reduce the receiver with an accumulator similar to {@code fold}.
	 * The first element serves as initial value for the reduction of the remaining elements.
	 *
	 * @param accumulator
	 * @return an {@link Optional} either containing the result of the evaluation of the accumulator with the last element or being empty if the receiver is empty.
	 * @see FunctionalIterable#reduce(Object, BiFunction)
	 */
	default Optional<E> reduce(BinaryOperator<E> accumulator)
	{
		return iterator().reduce(accumulator);
	}

	/**
	 * Reduce the receiver with an accumulator and an initial value.
	 * First, the accumulator is evaluated with the initial value and the first element.
	 * Then it is evaluated with each subsequent element and the result of the previous evaluation.
	 *
	 * @param init an initial value for the reduction
	 * @param accumulator a {@link BiFunction} taking an intermediate result as first argument and an element as second argument
	 * @return the result of the evaluation of the accumulator with the last element
	 */
	default <T> T reduce(T init, BiFunction<T, ? super E, T> accumulator)
	{
		return iterator().reduce(init, accumulator);
	}

	/**
	 * Primitive specialisation of {@code reduce} for values of type {@code double}.
	 *
	 * @see FunctionalIterable#reduce(Object, BiFunction)
	 */
	default double reduce(double init, DoubleObjToDoubleFunction<? super E> accumulator)
	{
		return iterator().reduce(init, accumulator);
	}

	/**
	 * Primitive specialisation of {@code reduce} for values of type {@code int}.
	 *
	 * @see FunctionalIterable#reduce(Object, BiFunction)
	 */
	default int reduce(int init, IntObjToIntFunction<? super E> accumulator)
	{
		return iterator().reduce(init, accumulator);
	}

	/**
	 * Primitive specialisation of {@code reduce} for values of type {@code long}.
	 *
	 * @see FunctionalIterable#reduce(Object, BiFunction)
	 */
	default long reduce(long init, LongObjToLongFunction<? super E> accumulator)
	{
		return iterator().reduce(init, accumulator);
	}
}
