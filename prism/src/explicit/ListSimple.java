//==============================================================================
//
//	Copyright (c) 2024-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

package explicit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A list of values. Mutable, but not especially efficient.
 * Storage is minimised by only storing the list if needed.
 * This partly depends on the definition of a "zero" value (null by default).
 * The actual size of the list is not known/stored.
 */
public class ListSimple<E>
{
	/**
	 * The values, stored as a list.
	 * Can be null, implying all zero.
	 **/
	protected List<E> values;

	/** Value for "zero" entries (defaults to null) */
	protected E zero = null;

	/** Test if a value is "zero" */
	protected Predicate<E> isZero = Objects::isNull;

	/**
	 * Constructor: zero list
	 */
	public ListSimple()
	{
		this(null, Objects::isNull);
	}

	/**
	 * Constructor: zero list
	 * @param zero Zero value
	 * @param isZero Test for zero
	 */
	public ListSimple(E zero, Predicate<E> isZero)
	{
		// Initially list is just null (denoting all zero)
		values = null;
		setZero(zero, isZero);
	}

	/**
	 * Copy constructor
	 * @param list ListSimple to copy
	 */
	public ListSimple(ListSimple<E> list)
	{
		values = list.values == null ? null : new ArrayList<>(list.values);
		setZero(list.zero, list.isZero);
	}

	/**
	 * Copy constructor with a value map, which is applied to values from the copied list.
	 * A new zero and zero test is provided since it is likely different.
	 * @param list ListSimple to copy
	 * @param valMap The value map
	 * @param zero Zero value
	 * @param isZero Test for zero
	 */
	public <T> ListSimple(ListSimple<T> list, Function<? super T, ? extends E> valMap, E zero, Predicate<E> isZero)
	{
		if (list.values == null) {
			values = null;
		} else {
			values = new ArrayList<>(list.values.size());
			for (T value : list.values) {
				values.add(valMap.apply(value));
			}
		}
		setZero(zero, isZero);
	}

	/**
	 * Copy constructor with index permutation,
	 * i.e., in which index {@code i} becomes index {@code permut[i]}.
	 * @param list ListSimple to copy
	 * @param permut Index permutation
	 */
	public ListSimple(ListSimple<E> list, int permut[])
	{
		if (list.values == null) {
			values = null;
		} else {
			int n = permut.length;
			values = new ArrayList<>(n);
			values.addAll(Collections.nCopies(n, null));
			for (int i = 0; i < n; i++) {
				values.set(permut[i], list.values.get(i));
			}
		}
		setZero(list.zero, list.isZero);
	}

	/**
	 * Copy constructor with index permutation,
	 * i.e., in which index {@code i} becomes index {@code permut[i]},
	 * and a value map, which is applied to values from the copied list.
	 * A new zero and zero test is provided since it is likely different.
	 * @param list ListSimple to copy
	 * @param permut Index permutation
	 * @param valMap The value map
	 * @param zero Zero value
	 * @param isZero Test for zero
	 */
	public <T> ListSimple(ListSimple<T> list, int permut[], Function<? super T, ? extends E> valMap, E zero, Predicate<E> isZero)
	{
		if (list.values == null) {
			values = null;
		} else {
			int n = permut.length;
			values = new ArrayList<>(n);
			values.addAll(Collections.nCopies(n, null));
			for (int i = 0; i < n; i++) {
				values.set(permut[i], valMap.apply(list.values.get(i)));
			}
		}
		setZero(zero, isZero);
	}

	/**
	 * Set the "zero" value for the list, and a test for it.
	 * @param zero Zero value
	 * @param isZero Test for zero
	 */
	public void setZero(E zero, Predicate<E> isZero)
	{
		this.zero = zero;
		this.isZero = isZero;
	}

	/**
	 * Set the value for index {@code i} to {@code value}.
	 */
	public void setValue(int i, E value)
	{
		// Create main list if not done yet
		if (values == null) {
			if (isZero.test(value)) {
				return;
			}
			values = new ArrayList<>(i + 1);
		}
		// Expand main list up to index i if needed,
		// storing zero for newly added items
		if (i >= values.size()) {
			if (isZero.test(value)) {
				return;
			}
			int n = i - values.size() + 1;
			for (int i2 = 0; i2 < n; i2++) {
				values.add(zero);
			}
		}
		// Store the value
		values.set(i, value);
	}

	/**
	 * Get the value for index {@code i}, {@code j}.
	 */
	public E getValue(int i)
	{
		return (values == null || i >= values.size()) ? zero : values.get(i);
	}

	/**
	 * Returns true if the list is all zero.
	 * BUT: This is done via a quick check. for efficiency.
	 * If it returns true, the list is definitely all zero,
	 * but the converse is not necessarily true.
	 */
	public boolean allZero()
	{
		return values == null;
	}

	@Override
	public String toString()
	{
		return values == null ? "[]" : values.toString();
	}
}
