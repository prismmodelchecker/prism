//==============================================================================
//	
//	Copyright (c) 2018-
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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;

import common.IteratorTools;

/**
 * This class provides static methods to construct
 * the Cartesian product of a set of iterables.
 * The implementation computes the tuples on the fly for space efficiency.
 * It relies on an efficient implementation of {@code flatMap}.
 * <p>
 * Since Stream::flatMap preallocates memory for the complete stream,
 * we have to use the alternative implementation from FunctionalIterable.
 * </p> 
 */
public class CartesianProduct
{
	/**
	 * Variant of the Cartesian product with {@code Object} as common super-type of the tuple/iterable elements.
	 * Elements of the product are distinct and safe to store/use elsewhere.
	 *
	 * @param iterables iterables to build Cartesian product from
	 * @see #of(Iterable)
	 */
	public static FunctionalIterable<Object[]> of(Iterable<?> ... iterables)
	{
		return of(new IterableArray.Of<>(iterables));
	}

	/**
	 * Typesafe generic variant of the Cartesian product.
	 * Elements of the product are distinct and safe to store/use elsewhere.
	 *
	 * @param type      common super-type of the tuple/iterable elements
	 * @param iterables iterables to build Cartesian product from
	 * @see #of(Iterable)
	 */
	@SafeVarargs
	public static <E> FunctionalIterable<E[]> of(Class<E> type, Iterable<? extends E> ... iterables)
	{
		return of(type, new IterableArray.Of<>(iterables));
	}

	/**
	 * Variant of the Cartesian product with {@code Object} as common super-type of the tuple/iterable elements.
	 * Elements of the product are distinct and safe to store/use elsewhere.
	 *
	 * @param iterables iterables to build Cartesian product from
	 * @see #of(Iterable)
	 */
	public static FunctionalIterable<Object[]> of(Iterable<Iterable<?>> iterables)
	{
		return mutableOf(iterables).map(t -> Arrays.copyOf(t, t.length));
	}

	/**
	 * Typesafe generic variant of the Cartesian product.
	 * Elements of the product are distinct and safe to store/use elsewhere.
	 *
	 * @param type      common super-type of the tuple/iterable elements
	 * @param iterables iterables to build Cartesian product from
	 * @see #of(Iterable)
	 */
	public static <E> FunctionalIterable<E[]> of(Class<E> type, Iterable<Iterable<? extends E>> iterables)
	{
		return mutableOf(type, iterables).map(t -> Arrays.copyOf(t, t.length));
	}

	/**
	 * Variant of the Cartesian product with {@code Object} as common super-type of the tuple/iterable elements.
	 * Elements of the product must be copied if stored/used elsewhere.
	 *
	 * @param type      common super-type of the tuple/iterable elements
	 * @param iterables iterables to build Cartesian product from
	 * @see #of(Iterable)
	 */
	public static FunctionalIterable<Object[]> mutableOf(Iterable<?> ... iterables)
	{
		return of(new IterableArray.Of<>(iterables));
	}

	/**
	 * Typesafe generic variant of the Cartesian product.
	 * Elements of the product must be copied if stored/used elsewhere.
	 *
	 * @param type      common super-type of the tuple/iterable elements
	 * @param iterables iterables to build Cartesian product from
	 * @see #mutableOf(Iterator, Object[], int)
	 */
	@SafeVarargs
	public static <E> FunctionalIterable<E[]> mutableOf(Class<E> type, Iterable<? extends E> ... iterables)
	{
		return mutableOf(type, new IterableArray.Of<>(iterables));
	}

	/**
	 * Variant of the Cartesian product with {@code Object} as common super-type of the tuple/iterable elements.
	 * Elements of the product must be copied if stored/used elsewhere.
	 *
	 * @param iterables iterables to build Cartesian product from
	 * @see #mutableOf(Iterator, Object[], int);
	 */
	public static FunctionalIterable<Object[]> mutableOf(Iterable<Iterable<?>> iterables)
	{
		return mutableOf(Object.class, iterables);
	}

	/**
	 * Typesafe generic variant of the Cartesian product.
	 * Elements of the product must be copied if stored/used elsewhere.
	 *
	 * @param type      common super-type of the tuple/iterable elements
	 * @param iterables iterables to build Cartesian product from
	 * @see #mutableOf(Iterator, Object[], int);
	 */
	public static <E> FunctionalIterable<E[]> mutableOf(Class<E> type, Iterable<Iterable<? extends E>> iterables)
	{
		int arity = Math.toIntExact(Reducible.extend(iterables).count());
		@SuppressWarnings("unchecked")
		E[] tuple = (E[]) Array.newInstance(type, arity);
		return mutableOf(iterables.iterator(), tuple, 0);
	}

	/**
	 * Build Cartesian product recursively over the provided iterables.
	 * For efficiency, the same tuple instance is reused for the elements of the result.
	 * Hence, an element has to be copied if it is stored/used elsewhere.
	 *
	 * @param iterables iterables to build Cartesian product from
	 * @param tuple     the tuple instance to be used
	 * @param pos       the current tuple position
	 * @return iterable of Cartesian product
	 */
	protected static <T> FunctionalIterable<T[]> mutableOf(Iterator<Iterable<? extends T>> iterables, T[] tuple, int pos)
	{
		// no iterable -> empty product
		if (!iterables.hasNext()) {
			return EmptyIterable.of();
		}
		assert pos < tuple.length : "Require slot in tuple for next element.";
		FunctionalIterable<? extends T> elements = Reducible.extend(iterables.next());
		// one iterable -> map elements to tuples
		if (!iterables.hasNext()) {
			return elements.map(e -> {tuple[pos] = e; return tuple;});
		}
		// more than one iterable: recurse
		FunctionalIterable<T[]> tuples = mutableOf(iterables, tuple, pos + 1);
		// elements x tuples
		return elements.flatMap(e -> {tuple[pos] = e; return tuples;});
	}

	/**
	 * Test method.
	 *
	 * @param args command-line arguments (ignored)
	 */
	public static void main(final String[] args)
	{
		// declare type of function explicitly to circumvent class format error due to faulty type inference
		Function<Object[], String> toString = Arrays::toString;

		FunctionalIterable<? extends Object[]> product = of();
		IteratorTools.printIterator("empty product  ", product.iterator());
		System.out.println();

		product = of(Arrays.asList(1, 2, 3));
		IteratorTools.printIterator("single product ", product.map(toString).iterator());
		System.out.println();

		product = of(Arrays.asList(1, 2, 3), Arrays.asList('a', 'b'));
		IteratorTools.printIterator("binary product ", product.map(toString).iterator());
		System.out.println();

		product = of(Arrays.asList(1, 2, 3), Arrays.asList('a', 'b'), Arrays.asList(0.5));
		IteratorTools.printIterator("ternary product", product.map(toString).iterator());
		System.out.println();

		product = mutableOf(Arrays.asList(1, 2, 3), Arrays.asList('a', 'b'), Arrays.asList(0.5));
		IteratorTools.printIterator("ternary product", product.map(toString).iterator());
		System.out.println();
	}
}
