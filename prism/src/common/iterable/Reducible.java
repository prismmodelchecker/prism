package common.iterable;

import common.functions.DoubleObjToDoubleFunction;
import common.functions.IntObjToIntFunction;
import common.functions.LongObjToLongFunction;

import java.util.*;
import java.util.function.*;

/**
 * A {@link Reducible} provides <em>transformation</em> and <em>accumulation</em> operations similar to {@link java.util.stream.Stream Stream}.
 * It serves as a bridge between Iterator-based code and the functional API provided by Java streams.
 * The operations are implemented and specialized in {@link FunctionalIterator} and {@link FunctionalIterable}.
 * The following example illustrates is usage.
 *
 * <pre>
 * List&lt;String>  allFibonacci = List.of("0", "1", "1", "2", "3", "5", "8", "13");
 * List&lt;Integer> oddFibonacci = Reducible.extend(allFibonacci)
 *                                   .mapToInt(Integer::parseInt)
 *                                   .filter((int i) -> i % 2 != 0)
 *                                   .collect(ArrayList::new);
 * </pre>
 * The code computes the odd Fibonacci numbers from a {@code List} of {@code String} representations:
 * <ol>
 * <li>Extend an ordinary Iterable (the {@code List}) with the methods of this interface
 * <li>Map the String representation of each number to its respective {@code int} value
 * <li>Filter all odd numbers
 * <li>Collect the filtered numbers in a new {@code ArrayList}
 * </ol>
 * Please note that {@link Reducible#mapToInt} and {@link Reducible#filter} only wrap the underlying Reducible.
 * The actual computation happens on-the-fly when {@code collect} is called.
 *
 * <p><em>Transformation operations</em> (known as <a href="package-summary.html#StreamOps">intermediate operations</a> on streams)
 * <em>transform the sequence of elements</em>, e.g., by filtering with a predicate or by mapping elements using a function.
 * All transformation operations are defined in this interface with one exception:
 * {@code flatMap} and its primitive specializations {@code flatMapToDouble}, {@code flatMapToInt} and {@code flatMapToLong}
 * have to be defined in the implementors of this interface, as Java's generic type system is not flexible enough to define it here.
 *
 * <p><em>Accumulation operations</em> (known as <a href="package-summary.html#StreamOps">terminal operations</a> on streams)
 * <em>consume the elements</em> and <em>compute a result</em>, e.g., by testing whether all elements match a predicate or by collecting all elements in an array.
 * The most general accumulation operation is {@link Reducible#reduce(Object, BiFunction) reduce}.
 * To enable an efficient treatment of the primitive types {@code double}, {@code int} and {@code long}, reduce is overloaded.
 * Please note that implementors of the interface may consume no elements if the result of an accumulation can be computed directly.
 *
 * @param <E> the type of elements returned by this Reducible
 * @param <E_CAT> the type of Reducibles accepted by {@link Reducible#concat concat}
 *ø
 * @see java.util.stream.Stream Stream
 */
public interface Reducible<E, E_CAT>
{
	// Bridging methods to Iterator-based code

	/**
	 * Extend an Iterable with the methods provided by FunctionalIterable.
	 * Answer the argument if it already implements {@code FunctionalIterable}.
	 *
	 * @param iterable the {@link Iterable} to extend
	 * @return a {@link FunctionalIterable} that is either the argument or an adaptor on the argument
	 */
	@SuppressWarnings("unchecked")
	static <E> FunctionalIterable<E> extend(Iterable<E> iterable)
	{
		if (iterable instanceof FunctionalIterable) {
			return (FunctionalIterable<E>) iterable;
		}
		if (iterable instanceof PrimitiveIterable.OfDouble) {
			return (FunctionalIterable<E>) extend((PrimitiveIterable.OfDouble) iterable);
		}
		if (iterable instanceof PrimitiveIterable.OfInt) {
			return (FunctionalIterable<E>) extend((PrimitiveIterable.OfInt) iterable);
		}
		if (iterable instanceof PrimitiveIterable.OfLong) {
			return (FunctionalIterable<E>) extend((PrimitiveIterable.OfLong) iterable);
		}
		return new IterableAdaptor.Of<>(iterable);

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
	static <E> FunctionalIterator<E> extend(Iterator<E> iterator)
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
	 * Extend a PrimitiveIterable.OfDouble with the methods provided by FunctionalPrimitiveIterable.
	 * Answer the argument if it already implements {@code FunctionalIterable}.
	 *
	 * @param iterable the {@link PrimitiveIterable.OfDouble} to extend
	 * @return a {@link FunctionalPrimitiveIterable.OfDouble} that is either the argument or an adaptor on the argument
	 */
	static FunctionalPrimitiveIterable.OfDouble extend(PrimitiveIterable.OfDouble iterable)
	{
		if (iterable instanceof FunctionalPrimitiveIterable.OfDouble) {
			return (FunctionalPrimitiveIterable.OfDouble) iterable;
		}
		return new IterableAdaptor.OfDouble(iterable);
	}

	/**
	 * Extend a PrimitiveIterator.OfDouble with the methods provided by FunctionalPrimitiveIterator.
	 * Answer the argument if it already implements {@code FunctionalPrimitiveIterator.OfDouble}.
	 *
	 * @param iterator the {@link PrimitiveIterator.OfDouble} to extend
	 * @return a {@link FunctionalPrimitiveIterator.OfDouble} that is either the argument or an adaptor on the argument
	 */
	static FunctionalPrimitiveIterator.OfDouble extend(PrimitiveIterator.OfDouble iterator)
	{
		if (iterator instanceof FunctionalPrimitiveIterator.OfDouble) {
			return (FunctionalPrimitiveIterator.OfDouble) iterator;
		}
		return new IteratorAdaptor.OfDouble(iterator);
	}

	/**
	 * Extend a PrimitiveIterable.OfInt with the methods provided by FunctionalPrimitiveIterable.
	 * Answer the argument if it already implements {@code FunctionalPrimitiveIterable}.
	 *
	 * @param iterable the {@link PrimitiveIterable.OfInt} to extend
	 * @return a {@link FunctionalPrimitiveIterable.OfDouble} that is either the argument or an adaptor on the argument
	 */
	static FunctionalPrimitiveIterable.OfInt extend(PrimitiveIterable.OfInt iterable)
	{
		if (iterable instanceof FunctionalPrimitiveIterable.OfInt) {
			return (FunctionalPrimitiveIterable.OfInt) iterable;
		}
		return new IterableAdaptor.OfInt(iterable);
	}

	/**
	 * Extend a PrimitiveIterator.OfInt with the methods provided by FunctionalPrimitiveIterator.
	 * Answer the argument if it already implements {@code FunctionalPrimitiveIterator.OfInt}.
	 *
	 * @param iterator the {@link PrimitiveIterator.OfInt} to extend
	 * @return a {@link FunctionalPrimitiveIterator.OfInt} that is either the argument or an adaptor on the argument
	 */
	static FunctionalPrimitiveIterator.OfInt extend(PrimitiveIterator.OfInt iterator)
	{
		if (iterator instanceof FunctionalPrimitiveIterator.OfInt) {
			return (FunctionalPrimitiveIterator.OfInt) iterator;
		}
		return new IteratorAdaptor.OfInt(iterator);
	}

	/**
	 * Extend a PrimitiveIterable.OfLong with the methods provided by FunctionalPrimitiveIterable.
	 * Answer the argument if it already implements {@code FunctionalPrimitiveIterable}.
	 *
	 * @param iterable the {@link PrimitiveIterable.OfLong} to extend
	 * @return a {@link FunctionalPrimitiveIterable.OfLong} that is either the argument or an adaptor on the argument
	 */
	static FunctionalPrimitiveIterable.OfLong extend(PrimitiveIterable.OfLong iterable)
	{
		if (iterable instanceof FunctionalPrimitiveIterable.OfLong) {
			return (FunctionalPrimitiveIterable.OfLong) iterable;
		}
		return new IterableAdaptor.OfLong(iterable);
	}

	/**
	 * Extend a PrimitiveIterator.OfLong with the methods provided by FunctionalPrimitiveIterator.
	 * Answer the argument if it already implements {@code FunctionalPrimitiveIterator.OfLong}.
	 *
	 * @param iterator the {@link PrimitiveIterator.OfLong} to extend
	 * @return a {@link FunctionalPrimitiveIterator.OfLong} that is either the argument or an adaptor on the argument
	 */
	static FunctionalPrimitiveIterator.OfLong extend(PrimitiveIterator.OfLong iterator)
	{
		if (iterator instanceof FunctionalPrimitiveIterator.OfLong) {
			return (FunctionalPrimitiveIterator.OfLong) iterator;
		}
		return new IteratorAdaptor.OfLong(iterator);
	}

	/**
	 * Concatenate a sequence of Iterables.
	 * The returned FunctionalIterable iterates over the elements in the order of the argument sequence.
	 *
	 * @param iterables the {@link Iterable}s to concatenate
	 * @return a {@link FunctionalIterable} that iterates over the elements of each Iterable
	 */
	static <E> FunctionalIterable<E> concat(Iterable<? extends Iterable<? extends E>> iterables)
	{
		return new ChainedIterable.Of(iterables);
	}

	/**
	 * Concatenate a sequence of Iterators.
	 * The returned FunctionalIterator iterates over the elements in the order of the argument sequence.
	 *
	 * @param iterators the {@link Iterator}s to concatenate
	 * @return a {@link FunctionalIterator} that iterates over the elements of each Iterator
	 */
	static <E> FunctionalIterator<E> concat(Iterator<? extends Iterator<? extends E>> iterators)
	{
		return new ChainedIterator.Of(iterators);
	}

	/**
	 * Primitive specialisation of {@code concat} for {@code double}.
	 *
	 * @param iterables the {@link PrimitiveIterable.OfDouble}s to concatenate
	 * @return a {@link FunctionalPrimitiveIterable.OfDouble} that iterates over the elements of each Iterable
	 */
	static FunctionalPrimitiveIterable.OfDouble concatDouble(Iterable<? extends PrimitiveIterable.OfDouble> iterables)
	{
		return new ChainedIterable.OfDouble(iterables);
	}

	/**
	 * Primitive specialisation of {@code concat} for {@code double}.
	 *
	 * @param iterators the {@link PrimitiveIterator.OfDouble}s to concatenate
	 * @return a {@link FunctionalPrimitiveIterator.OfDouble} that iterates over the elements of each Iterator
	 */
	static FunctionalPrimitiveIterator.OfDouble concatDouble(Iterator<? extends PrimitiveIterator.OfDouble> iterators)
	{
		return new ChainedIterator.OfDouble(iterators);
	}

	/**
	 * Primitive specialisation of {@code concat} for {@code int}.
	 *
	 * @param iterables the {@link PrimitiveIterable.OfInt}s to concatenate
	 * @return a {@link FunctionalPrimitiveIterable.OfInt} that iterates over the elements of each Iterable
	 */
	static FunctionalPrimitiveIterable.OfInt concatInt(Iterable<? extends PrimitiveIterable.OfInt> iterables)
	{
		return new ChainedIterable.OfInt(iterables);
	}

	/**
	 * Primitive specialisation of {@code concat} for {@code int}.
	 *
	 * @param iterators the {@link PrimitiveIterator.OfInt}s to concatenate
	 * @return a {@link FunctionalPrimitiveIterator.OfInt} that iterates over the elements of each Iterator
	 */
	static FunctionalPrimitiveIterator.OfInt concatInt(Iterator<? extends PrimitiveIterator.OfInt> iterators)
	{
		return new ChainedIterator.OfInt(iterators);
	}

	/**
	 * Primitive specialisation of {@code concat} for {@code long}.
	 *
	 * @param iterables the {@link PrimitiveIterable.OfLong}s to concatenate
	 * @return a {@link FunctionalPrimitiveIterable.OfLong} that iterates over the elements of each Iterable
	 */
	static FunctionalPrimitiveIterable.OfLong concatLong(Iterable<? extends PrimitiveIterable.OfLong> iterables)
	{
		return new ChainedIterable.OfLong(iterables);
	}

	/**
	 * Primitive specialisation of {@code concat} for {@code long}.
	 *
	 * @param iterators the {@link PrimitiveIterator.OfLong}s to concatenate
	 * @return a {@link FunctionalPrimitiveIterator.OfLong} that iterates over the elements of each Iterator
	 */
	static FunctionalPrimitiveIterator.OfLong concatLong(Iterator<? extends PrimitiveIterator.OfLong> iterators)
	{
		return new ChainedIterator.OfLong(iterators);
	}

	/**
	 * Convert an Iterable&lt;Double&gt; to a FunctionalPrimitiveIterable.OfDouble by unboxing each element.
	 * If the argument's Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterable if it already implements of FunctionalPrimitiveIterable.OfDouble.
	 *
	 * @param iterable the {@link Iterable}&lt;Double&gt; to extend
	 * @return a {@link FunctionalPrimitiveIterable.OfDouble} that is either the argument or an adaptor on the argument
	 */
	static FunctionalPrimitiveIterable.OfDouble unboxDouble(Iterable<Double> iterable)
	{
		if (iterable instanceof FunctionalPrimitiveIterable.OfDouble) {
			return (FunctionalPrimitiveIterable.OfDouble) iterable;
		}
		return extend(PrimitiveIterable.unboxDouble(iterable));
	}

	/**
	 * Convert an Iterator&lt;Double&gt; to a FunctionalPrimitiveIterator.OfDouble by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterator if it already implements of FunctionalPrimitiveIterator.OfDouble.
	 *
	 * @param iterator the {@link Iterator}&lt;Double&gt; to unbox
	 * @return a {@link FunctionalPrimitiveIterator.OfDouble} that is either the argument or an unboxing iterator
	 */
	static FunctionalPrimitiveIterator.OfDouble unboxDouble(Iterator<Double> iterator)
	{
		if (iterator instanceof PrimitiveIterator.OfDouble) {
			return extend((PrimitiveIterator.OfDouble) iterator);
		}
		return extend(PrimitiveIterable.unboxDouble(iterator));
	}

	/**
	 * Convert an Iterable&lt;Integer&gt; to a FunctionalPrimitiveIterable.OfInt by unboxing each element.
	 * If the argument's Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterable if it already implements of FunctionalPrimitiveIterable.OfInt.
	 *
	 * @param iterable the {@link Iterable}&lt;Integer&gt; to extend
	 * @return a {@link FunctionalPrimitiveIterable.OfInt} that is either the argument or an adaptor on the argument
	 */
	static FunctionalPrimitiveIterable.OfInt unboxInt(Iterable<Integer> iterable)
	{
		if (iterable instanceof FunctionalPrimitiveIterable.OfInt) {
			return (FunctionalPrimitiveIterable.OfInt) iterable;
		}
		return extend(PrimitiveIterable.unboxInt(iterable));
	}

	/**
	 * Convert an Iterator&lt;Integer&gt; to a FunctionalPrimitiveIterator.OfInt by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterator if it already implements of FunctionalPrimitiveIterator.OfInt.
	 *
	 * @param iterator the {@link Iterator}&lt;Integer&gt; to unbox
	 * @return a {@link FunctionalPrimitiveIterator.OfInt} that is either the argument or an unboxing iterator
	 */
	static FunctionalPrimitiveIterator.OfInt unboxInt(Iterator<Integer> iterator)
	{
		if (iterator instanceof PrimitiveIterator.OfInt) {
			return extend((PrimitiveIterator.OfInt) iterator);
		}
		return extend(PrimitiveIterable.unboxInt(iterator));
	}

	/**
	 * Convert an Iterable&lt;Long&gt; to a FunctionalPrimitiveIterable.OfLong by unboxing each element.
	 * If the argument's Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterable if it already implements of FunctionalPrimitiveIterable.OfLong.
	 *
	 * @param iterable the {@link Iterable}&lt;Long&gt; to extend
	 * @return a {@link FunctionalPrimitiveIterable.OfLong} that is either the argument or an adaptor on the argument
	 */
	static FunctionalPrimitiveIterable.OfLong unboxLong(Iterable<Long> iterable)
	{
		if (iterable instanceof FunctionalPrimitiveIterable.OfLong) {
			return (FunctionalPrimitiveIterable.OfLong) iterable;
		}
		return extend(PrimitiveIterable.unboxLong(iterable));
	}

	/**
	 * Convert an Iterator&lt;Long&gt; to a FunctionalPrimitiveIterator.OfLong by unboxing each element.
	 * If the Iterator yields {@code null}, a {@link NullPointerException} is thrown when iterating.
	 * Answer the Iterator if it already implements of FunctionalPrimitiveIterator.OfLong.
	 *
	 * @param iterator the {@link Iterator}&lt;Long&gt; to unbox
	 * @return a {@link FunctionalPrimitiveIterator.OfLong} that is either the argument or an unboxing iterator
	 */
	static FunctionalPrimitiveIterator.OfLong unboxLong(Iterator<Long> iterator)
	{
		if (iterator instanceof PrimitiveIterator.OfLong) {
			return extend((PrimitiveIterator.OfLong) iterator);
		}
		return extend(PrimitiveIterable.unboxLong(iterator));
	}



	// Fundamental methods

	/**
	 * Perform an action with each of the receivers elements.
	 *
	 * @param action the action to be performed with each element
	 * @see Iterable#forEach(Consumer)
	 */
	void forEach(Consumer<? super E> action);

	/**
	 * Test whether the receiver is empty.
	 *
	 * @return {@code true} iff the receiver is empty
	 */
	boolean isEmpty();



	// Transforming Methods

	/**
	 * Concatenate the receiver and the argument.
	 * The returned Reducible first iterates over the elements of the receiver and then over the elements of the argument.
	 *
	 * @param reducible the {@link Reducible} to append
	 * @return a {@link Reducible} that iterates over the elements of the receiver and the argument
	 */
	Reducible<E,E_CAT> concat(E_CAT reducible);

	/**
	 * Remove consecutive duplicate elements such that only the first occurrence in a sequence is retained.
	 * Duplicates are identified in terms of {@link Object#equals}.
	 */
	default Reducible<E,E_CAT> dedupe()
	{
		Predicate<E> isFirst = new Predicate<>()
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
	 * Remove duplicate elements from the receiver such that only one occurrence is retained.
	 * Duplicates are identified in terms of {@link Object#equals} which requires a proper implementation of {@link Object#hashCode}.
	 */
	default Reducible<E,E_CAT> distinct()
	{
		return filter(new Distinct.Of<>());
	}

	/**
	 * Filter the receiver by a predicate, i.e., only elements that match the predicate are kept.
	 *
	 * @param predicate a predicate evaluating to {@code true} for the elements to keep
	 */
	Reducible<E,E_CAT> filter(Predicate<? super E> predicate);

	/**
	 * Map each element.
	 *
	 * @param function a mapping function that is applied to each element
	 */
	<T> Reducible<T,?> map(Function<? super E, ? extends T> function);

	/**
	 * Map each element to a {@code double}.
	 *
	 * @param function a mapping function that is applied to each element
	 */
	PrimitiveReducible.OfDouble<?> mapToDouble(ToDoubleFunction<? super E> function);

	/**
	 * Map each element to an {@code int}.
	 *
	 * @param function a mapping function that is applied to each element
	 */
	PrimitiveReducible.OfInt<?> mapToInt(ToIntFunction<? super E> function);

	/**
	 * Map each element to a {@code long}.
	 *
	 * @param function a mapping function that is applied to each element
	 */
	PrimitiveReducible.OfLong<?> mapToLong(ToLongFunction<? super E> function);

	/**
	 * Remove all {@code null} elements.
	 */
	default Reducible<E,E_CAT> nonNull()
	{
		return filter(Objects::nonNull);
	}



	// Accumulations Methods (Consuming)

	/**
	 * Consume this Reducible, i.e., iterate over all its elements.
	 */
	default Reducible<E,E_CAT> consume()
	{
		forEach((E each) -> {});
		return this;
	}

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
		return ! filter(predicate).isEmpty();
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
		StringBuilder builder = new StringBuilder("[");
		forEach(each -> {
			if (builder.length() > 1) {
				builder.append(", ");
			}
			builder.append(each);
		});
		return builder.append("]").toString();
	}

	/**
	 * Add each element to a newly instantiated {@code Collection} supplied by the argument using {@link Collection#add(Object)}.
	 *
	 * @param constructor a function yielding a new Collection
	 * @return the new Collection with all elements added to it
	 * @see java.util.stream.Stream#collect(Supplier, java.util.function.BiConsumer, java.util.function.BiConsumer)
	 */
	default <C extends Collection<? super E>> C collect(Supplier<? extends C> constructor)
	{
		Objects.requireNonNull(constructor);
		C collection = constructor.get();
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
		forEach(collection::add);
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
		return reduce(0L, (long c, E e) -> {collection.add(e); return c + 1;});
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
		int index = reduce(offset, (int i, E e) -> {array[i] = e; return i + 1;});
		return index - offset;
	}

	/**
	 * Collect distinct elements from the receiver such that only one occurrence of each element is retained.
	 * Duplicates are identified in terms of {@link Object#equals} which requires a proper implementation of {@link Object#hashCode}.
	 * The order of elements in the result may differ from the order in the receiver.
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
	 * @param obj an object to be found in the receiver
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
		return reduce(0L, (long c, E e) -> c + 1);
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
	 * @return the first a matching element
	 * @throws NoSuchElementException if no matching element was found
	 */
	E detect(Predicate<? super E> predicate);

	/**
	 * Reduce the receiver with an accumulator similar to {@code fold}.
	 * The first element serves as initial value for the reduction of the remaining elements.
	 *
	 * @param accumulator a {@link BinaryOperator} taking an intermediate result as first argument and an element as second argument
	 * @return an {@link Optional} either containing the result of the evaluation of the accumulator with the last element or being empty if the receiver is empty.
	 * @throws NullPointerException if the result of the reduction is {@code null}
	 * @see FunctionalIterator#reduce(Object, BiFunction)
	 */
	Optional<E> reduce(BinaryOperator<E> accumulator);

	/**
	 * Reduce the receiver with an accumulator and an initial value.
	 * First, the accumulator is evaluated with the initial value and the first element.
	 * Then it is evaluated with each subsequent element and the result of the previous evaluation.
	 *
	 * @param init an initial value for the reduction
	 * @param accumulator a {@link BiFunction} taking an intermediate result as first argument and an element as second argument
	 * @return the result of the evaluation of the accumulator with the last element
	 */
	<T> T reduce(T init, BiFunction<T, ? super E, T> accumulator);

	/**
	 * Primitive specialisation of {@code reduce} for values of type {@code double}.
	 *
	 * @see FunctionalIterator#reduce(Object, BiFunction)
	 */
	double reduce(double init, DoubleObjToDoubleFunction<? super E> accumulator);

	/**
	 * Primitive specialisation of {@code reduce} for values of type {@code int}.
	 *
	 * @see FunctionalIterator#reduce(Object, BiFunction)
	 */
	int reduce(int init, IntObjToIntFunction<? super E> accumulator);

	/**
	 * Primitive specialisation of {@code reduce} for values of type {@code long}.
	 *
	 * @see FunctionalIterator#reduce(Object, BiFunction)
	 */
	long reduce(long init, LongObjToLongFunction<? super E> accumulator);
}
