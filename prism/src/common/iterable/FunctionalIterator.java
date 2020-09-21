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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.PrimitiveIterator;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import common.functions.DoubleObjToDoubleFunction;
import common.functions.IntObjToIntFunction;
import common.functions.LongObjToLongFunction;
import common.iterable.MappingIterator.ObjToDouble;
import common.iterable.MappingIterator.ObjToInt;
import common.iterable.MappingIterator.ObjToLong;

/**
 * An {@link Iterator} that provides <em>transformation</em> and <em>accumulation</em> operations similar to {@link java.util.stream.Stream Stream}.
 * It servers as a bridge between Iterator-based code and the functional API provided by Java streams.
 * The following example illustrates is usage.
 *
 * <pre>
 * List&lt;String>  allFibonacci = List.of("0", "1", "1", "2", "3", "5", "8", "13");
 * List&lt;Integer> oddFibonacci = FunctionalIterator.extend(AllFibonacci)
 *                                   .mapToInt(Integer::parseInt)
 *                                   .filter((int i) -> i % 2 != 0)
 *                                   .collect(ArrayList::new);
 * </pre>
 * The code computes the odd Fibonacci numbers from a {@code List} of {@code String} representations:
 * <ol>
 * <li>Extend an ordinary Iterator obtained from {@code List} with the methods of this interface.
 * <li>Map the String representations to their respective {@code int} values
 * <li>Filter all odd numbers
 * <li>Collect the numbers in a new {@code ArrayList}
 * </ol>
 * Please note that the {@code mapToInt} and {@code} only wrap the underlying Iterator.
 * The actual computation happens on-the-fly when {@code collect} is called.
 *
 * <p><em>Transformation operations</em> (known as <a href="package-summary.html#StreamOps">intermediate operations</a> on streams)
 * <em>transform the sequence of elements</em>, e.g., by filtering with a predicate or by mapping elements using a function.
 *
 * <p><em>Accumulation operations</em> (known as <a href="package-summary.html#StreamOps">terminal operations</a> on streams)
 * <em>consume the elements</em> and <em>compute a result</em>, e.g., by testing whether all elements match a predicate or by collection all elements in an array.
 * The most general accumulation operation is {@link FunctionalIterator#reduce(Object, BinaryOperator) reduce}.
 * To enable an efficient treatment of the primitive types {@code double}, {@code int} and {@code long}, reduce is overloaded.
 * Please note that implementors of the interface may not consume any elements if the result of an accumulation can be computed directly.
 *
 * <p>The methods {@link FunctionalIterator#unwrap() unwrap}, {@link FunctionalIterator#release() release} and {@link FunctionalIterator#requireNext() requireNext}
 * are helper methods for implementors of the interface.
 * They should not be called by client code.
 *
 * @param <E> the type of elements returned by this Iterator
 * @see java.util.stream.Stream Stream
 */
public interface FunctionalIterator<E> extends Iterator<E>
{
	/**
	 * Extend the Iterator of an Iterable with the methods provided by FunctionalIterator.
	 * If the Iterator is a PrimitiveIterator, the extension is a {@code FunctionalPrimitiveIterator}.
	 * Answer the argument if it already implements of {@code FunctionalIterator}.
	 *
	 * @param iterable the {@link Iterable} providing the {@link Iterator} to extend
	 * @return a {@link FunctionalIterator} that is either the Iterable's Iterator or an adaptor on the Iterator
	 */
	public static <E> FunctionalIterator<E> extend(Iterable<E> iterable)
	{
		return extend(iterable.iterator());
	}

	/**
	 * Extend an Iterator with the methods provided by FunctionalIterator.
	 * If the Iterator is a PrimitiveIterator, the extension is a {@code FunctionalPrimitiveIterator}.
	 * Answer the argument if it already implements {@code FunctionalIterator}.
	 *
	 * @param iterator the {@link Iterator} to extend
	 * @return a {@link FunctionalIterator} that is either the argument or an adaptor on the argument
	 */
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
		return new IteratorAdaptor.Of<>(iterator);
	}

	/**
	 * Extend a PrimitiveIterator.OfDouble with the methods provided by FunctionalPrimitiveIterator.
	 * Answer the argument if it already implements {@code FunctionalPrimitiveIterator.OfDouble}.
	 *
	 * @param iterator the {@link PrimitiveIterator.OfDouble} to extend
	 * @return a {@link FunctionalPrimitiveIterator.OfDouble} that is either the argument or an adaptor on the argument
	 */
	public static FunctionalPrimitiveIterator.OfDouble extend(PrimitiveIterator.OfDouble iterator)
	{
		if (iterator instanceof FunctionalPrimitiveIterator.OfDouble) {
			return (FunctionalPrimitiveIterator.OfDouble) iterator;
		}
		return new IteratorAdaptor.OfDouble(iterator);
	}

	/**
	 * Extend a PrimitiveIterator.OfInt with the methods provided by FunctionalPrimitiveIterator.
	 * Answer the argument if it already implements {@code FunctionalPrimitiveIterator.OfInt}.
	 *
	 * @param iterator the {@link PrimitiveIterator.OfInt} to extend
	 * @return a {@link FunctionalPrimitiveIterator.OfInt} that is either the argument or an adaptor on the argument
	 */
	public static FunctionalPrimitiveIterator.OfInt extend(PrimitiveIterator.OfInt iterator)
	{
		if (iterator instanceof FunctionalPrimitiveIterator.OfInt) {
			return (FunctionalPrimitiveIterator.OfInt) iterator;
		}
		return new IteratorAdaptor.OfInt(iterator);
	}

	/**
	 * Extend a PrimitiveIterator.OfLong with the methods provided by FunctionalPrimitiveIterator.
	 * Answer the argument if it already implements {@code FunctionalPrimitiveIterator.OfLong}.
	 *
	 * @param iterator the {@link PrimitiveIterator.OfLong} to extend
	 * @return a {@link FunctionalPrimitiveIterator.OfLong} that is either the argument or an adaptor on the argument
	 */
	public static FunctionalPrimitiveIterator.OfLong extend(PrimitiveIterator.OfLong iterator)
	{
		if (iterator instanceof FunctionalPrimitiveIterator.OfLong) {
			return (FunctionalPrimitiveIterator.OfLong) iterator;
		}
		return new IteratorAdaptor.OfLong(iterator);
	}

	/**
	 * Convert the Iterator of an Iterable&lt;Double&gt; to a FunctionalPrimitiveItator.OfDouble by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterable's Iterator if it already implements of FunctionalPrimitiveIterator.OfDouble.
	 *
	 * @param iterable the {@link Iterable} providing the {@link Iterator} to unbox
	 * @return a {@link FunctionalPrimitiveIterator.OfDouble} that is either the argument's iterator or an unboxing iterator
	 */
	public static FunctionalPrimitiveIterator.OfDouble unboxDouble(Iterable<Double> iterable)
	{
		return unboxDouble(iterable.iterator());
	}

	/**
	 * Convert an Iterator&lt;Double&gt; to a FunctionalPrimitiveItator.OfDouble by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterator if it already implements of FunctionalPrimitiveIterator.OfDouble.
	 *
	 * @param iterator the {@link Iterator}&lt;Double&gt; to unbox
	 * @return a {@link FunctionalPrimitiveIterator.OfDouble} that is either the argument or an unboxing iterator
	 */
	public static FunctionalPrimitiveIterator.OfDouble unboxDouble(Iterator<Double> iterator)
	{
		if (iterator instanceof PrimitiveIterator.OfDouble) {
			return FunctionalIterator.extend((PrimitiveIterator.OfDouble) iterator);
		}
		return new ObjToDouble<>(iterator, Double::doubleValue);
	}

	/**
	 * Convert the Iterator of an Iterable&lt;Integer&gt; to a FunctionalPrimitiveItator.OfInt by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterable's Iterator if it already implements of FunctionalPrimitiveIterator.OfInt.
	 *
	 * @param iterable the {@link Iterable} providing the {@link Iterator} to unbox
	 * @return a {@link FunctionalPrimitiveIterator.OfInt} that is either the argument's iterator or an unboxing iterator
	 */
	public static FunctionalPrimitiveIterator.OfInt unboxInt(Iterable<Integer> iterable)
	{
		return unboxInt(iterable.iterator());
	}

	/**
	 * Convert an Iterator&lt;Integer&gt; to a FunctionalPrimitiveItator.OfInt by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterator if it already implements of FunctionalPrimitiveIterator.OfInt.
	 *
	 * @param iterator the {@link Iterator}&lt;Integer&gt; to unbox
	 * @return a {@link FunctionalPrimitiveIterator.OfInt} that is either the argument or an unboxing iterator
	 */
	public static FunctionalPrimitiveIterator.OfInt unboxInt(Iterator<Integer> iterator)
	{
		if (iterator instanceof PrimitiveIterator.OfInt) {
			return FunctionalIterator.extend((PrimitiveIterator.OfInt) iterator);
		}
		return new ObjToInt<>(iterator, Integer::intValue);
	}

	/**
	 * Convert the Iterator of an Iterable&lt;Long&gt; to a FunctionalPrimitiveItator.OfLong by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterable's Iterator if it already implements of FunctionalPrimitiveIterator.OfLong.
	 *
	 * @param iterable the {@link Iterable} providing the {@link Iterator} to unbox
	 * @return a {@link FunctionalPrimitiveIterator.OfLong} that is either the argument's iterator or an unboxing iterator
	 */
	public static FunctionalPrimitiveIterator.OfLong unboxLong(Iterable<Long> iterable)
	{

		return unboxLong(iterable.iterator());
	}

	/**
	 * Convert an Iterator&lt;Long&gt; to a FunctionalPrimitiveItator.OfLong by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterator if it already implements of FunctionalPrimitiveIterator.OfLong.
	 *
	 * @param iterator the {@link Iterator}&lt;Long&gt; to unbox
	 * @return a {@link FunctionalPrimitiveIterator.OfLong} that is either the argument or an unboxing iterator
	 */
	public static FunctionalPrimitiveIterator.OfLong unboxLong(Iterator<Long> iterator)
	{
		if (iterator instanceof PrimitiveIterator.OfLong) {
			return FunctionalIterator.extend((PrimitiveIterator.OfLong) iterator);
		}
		return new ObjToLong<>(iterator, Long::longValue);
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

	/**
	 * Concatenate this Iterator and the arguments.
	 * The returned Iterator first iterates over the elements of the receiver and then over the elements of each argument.
	 *
	 * @param iterators an array of {@link Iterator}s to append
	 * @return an {@link Iterator} that iterates over the elements of the receiver and the arguments
	 */
	@SuppressWarnings("unchecked")
	default FunctionalIterator<E> concat(Iterator<? extends E>... iterators)
	{
		Objects.requireNonNull(iterators);
		if (iterators.length == 0) {
			return this;
		}
		return new ChainedIterator.Of<>(unwrap(), iterators);
	}

	/**
	 * Consume this Iterator to its end.
	 */
	default FunctionalIterator<E> consume()
	{
		return reduce(this, (FunctionalIterator<E> iter, E e) -> iter);
	}

	/**
	 * Remove duplicate elements from the receiver such that only one occurrence is retained.
	 * Duplicates are identified in terms of {@link Object#equals} which requires a proper implementation of {@link Object#hashCode}.
	 */
	default FunctionalIterator<E> distinct()
	{
		return filter(new Distinct.Of<>());
	}

	/**
	 * Remove consecutive duplicate elements such that only the first occurrence in a sequence is retained.
	 * Duplicates are identified in terms of {@link Object#equals}.
	 */
	default FunctionalIterator<E> dedupe()
	{
		Predicate<E> isFirst = new Predicate<E>()
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
		return filter(isFirst);
	}

	/**
	 * Filter the receiver by a predicate, i.e., only elements that match the predicate are kept.
	 *
	 * @param predicate a predicate evaluating to {@code true} for the elements to keep
	 */
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
	default <T> FunctionalPrimitiveIterator.OfDouble flatMapToDouble(Function<? super E, PrimitiveIterator.OfDouble> function)
	{
		return new ChainedIterator.OfDouble(map(function));
	}

	/**
	 * Map each element to a PrimitiveIterator over {@code int} and concatenate the obtained Iterators.
	 *
	 * @param function a function that maps each element to an {@link PrimitiveIterator.OfInt}
	 */
	default <T> FunctionalPrimitiveIterator.OfInt flatMapToInt(Function<? super E, PrimitiveIterator.OfInt> function)
	{
		return new ChainedIterator.OfInt(map(function));
	}

	/**
	 * Map each element to a PrimitiveIterator over {@code long} and concatenate the obtained Iterators.
	 *
	 * @param function a function that maps each element to an {@link PrimitiveIterator.OfLong}
	 */
	default <T> FunctionalPrimitiveIterator.OfLong flatMapToLong(Function<? super E, PrimitiveIterator.OfLong> function)
	{
		return new ChainedIterator.OfLong(map(function));
	}

	/**
	 * Map each element.
	 *
	 * @param function a mapping function that is applied to each element
	 */
	default <T> FunctionalIterator<T> map(Function<? super E, ? extends T> function)
	{
		return new MappingIterator.ObjToObj<>(unwrap(), function);
	}

	/**
	 * Map each element to a {@code double}.
	 *
	 * @param function a mapping function that is applied to each element
	 */
	default FunctionalPrimitiveIterator.OfDouble mapToDouble(ToDoubleFunction<? super E> function)
	{
		return new MappingIterator.ObjToDouble<>(unwrap(), function);
	}

	/**
	 * Map each element to an {@code int}.
	 *
	 * @param function a mapping function that is applied to each element
	 */
	default FunctionalPrimitiveIterator.OfInt mapToInt(ToIntFunction<? super E> function)
	{
		return new MappingIterator.ObjToInt<>(unwrap(), function);
	}

	/**
	 * Map each element to a {@code long}.
	 *
	 * @param function a mapping function that is applied to each element
	 */
	default FunctionalPrimitiveIterator.OfLong mapToLong(ToLongFunction<? super E> function)
	{
		return new MappingIterator.ObjToLong<>(unwrap(), function);
	}

	/**
	 * Remove all {@code null} elements.
	 */
	default FunctionalIterator<E> nonNull()
	{
		return filter(Objects::nonNull);
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
		return !anyMatch(predicate.negate());
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
		return detect(predicate).isPresent();
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
		return !anyMatch(predicate);
	}

	/**
	 * Build an array-like string that lists the string representation {@link Object#toString} of each element.
	 *
	 * @return a String of the form "[e_1, e_2, ... , e_n]"
	 */
	default String asString()
	{
		StringBuffer buffer = new StringBuffer("[");
		if (hasNext()) {
			buffer.append(next());
		}
		unwrap().forEachRemaining(each -> buffer.append(", ").append(each));
		release();
		return buffer.append("]").toString();
	}

	/**
	 * Add each element to a newly instantiated {@code Collection} supplied by the argument using {@link Collection#add(Object)}.
	 *
	 * @param supplier a function yielding a new Collection
	 * @return the new Collection with all elements added to it
	 * @see java.util.stream.Stream#collect(Supplier, java.util.function.BiConsumer, java.util.function.BiConsumer) Stream.collect(Supplier, BiConsumer, BiConsumer)
	 */
	default <C extends Collection<? super E>> C collect(Supplier<? extends C> supplier)
	{
		Objects.requireNonNull(supplier);
		C collection = supplier.get();
		collect(collection);
		return collection;
	}

	/**
	 * Add each element to the argument {@code Collection} using {@link Collection#add(Object)}.
	 *
	 * @param collection a {@link Collection}
	 * @return the Collection with all elements added to it
	 */
	default <C extends Collection<? super E>> C collect(C collection)
	{
		Objects.requireNonNull(collection);
		unwrap().forEachRemaining(collection::add);
		release();
		return collection;
	}

	/**
	 * Store each element in an array starting at index 0.
	 *
	 * @param array an Array of the receiver's element type
	 * @return the array with all elements stored in it
	 */
	default E[] collect(E[] array)
	{
		return collect(array, 0);
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
		collectAndCount(array, offset);
		return array;
	}

	/**
	 * Add each element to the argument {@code Collection} using {@link Collection#add(Object)} and count the number of elements added.
	 *
	 * @param collection a {@link Collection}
	 * @return the number of elements added to the Collection
	 */
	default long collectAndCount(Collection<? super E> collection)
	{
		Objects.requireNonNull(collection);
		return reduce(0L, (long c, E e) -> {collection.add(e); return c++;});
	}

	/**
	 * Store each element in an array starting at index 0 and count the number of elements stored.
	 *
	 * @param array an Array of the receiver's element type
	 * @return the number of elements stored in the array
	 */
	default int collectAndCount(E[] array)
	{
		return collectAndCount(array, 0);
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
		Objects.requireNonNull(array);
		int index = reduce(offset, (int i, E e) -> {array[i] = e; return i++;});
		return index - offset;
	}

	/**
	 * Collect distinct elements from the receiver such that only one occurrence of each element is retained.
	 * Duplicates are identified in terms of {@link Object#equals} which requires a proper implementation of {@link Object#hashCode}.
	 *
	 * @return a {@link FunctionalIterable} that contains each distinct of the receiver exactly once
	 * @see FunctionalIterator#distinct()
	 */
	default FunctionalIterable<E> collectDistinct()
	{
		Distinct.Of<E> unseen = new Distinct.Of<>();
		filter(unseen).consume();
		return unseen.getSeen();
	}

	/**
	 * Test in terms of {@link Object#equals} whether an object (or {@code null}) is one of the receiver's elements.
	 *
	 * @param obj
	 * @return {@code true} if the argument is contained in the receiver
	 */
	default boolean contains(Object obj)
	{
		return anyMatch((obj == null) ? Objects::isNull : obj::equals);
	}

	/**
	 * Count the number of elements in the receiver.
	 *
	 * @return the number of elements
	 */
	default long count()
	{
		return reduce(0L, (long c, E e) -> c++);
	}

	/**
	 * Count the number of elements that match a predicate.
	 *
	 * @param predicate a {@link Predicate} evaluating to {@code true} for the elements to count
	 * @return the number of elements matching the predicate
	 */
	default long count(Predicate<? super E> predicate)
	{
		return filter(predicate).count();
	}

	/**
	 * Find the first element that matches a predicate.
	 *
	 * @param predicate a {@link Predicate} evaluating to {@code true} for the element to return
	 * @return an {@link Optional} either containing a matching element or being empty if no matching element was found
	 */
	default Optional<E> detect(Predicate<? super E> predicate)
	{
		FunctionalIterator<E> filtered = filter(predicate);
		Optional<E> result = filtered.hasNext() ? Optional.of(filtered.next()) : Optional.empty();
		release();
		return result;
	}

	/**
	 * Reduce the receiver with an accumulator similar to {@code fold}.
	 * The first element serves as initial value for the reduction of the remaining elements.
	 *
	 * @param accumulator
	 * @return an {@link Optional} either containing the result of the evaluation of the accumulator with the last element or being empty if the receiver is empty.
	 * @see FunctionalIterator#reduce(Object, BiFunction)
	 */
	default Optional<E> reduce(BinaryOperator<E> accumulator)
	{
		Objects.requireNonNull(accumulator);
		if (!hasNext()) {
			return Optional.empty();
		}
		return Optional.of(reduce(next(), accumulator));
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

	/**
	 * Primitive specialisation of {@code reduce} for values of type {@code double}.
	 *
	 * @see FunctionalIterator#reduce(Object, BiFunction)
	 */
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

	/**
	 * Primitive specialisation of {@code reduce} for values of type {@code int}.
	 *
	 * @see FunctionalIterator#reduce(Object, BiFunction)
	 */
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

	/**
	 * Primitive specialisation of {@code reduce} for values of type {@code long}.
	 *
	 * @see FunctionalIterator#reduce(Object, BiFunction)
	 */
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
