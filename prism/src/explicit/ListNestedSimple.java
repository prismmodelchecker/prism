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
 * A list of lists of values. Mutable, but not especially efficient.
 * Storage is minimised by only storing the list (and sublists) as needed.
 * This partly depends on the definition of a "zero" value (null by default).
 * The actual size of the list and sublists is not known/stored.
 */
public class ListNestedSimple<E>
{
	/**
	 * The values, stored as a list of lists.
	 * Can be null, implying all zero. So can sublists.
	 **/
	protected List<List<E>> values;

	/** Value for "zero" entries (defaults to null) */
	protected E zero = null;

	/** Test if a value is "zero" */
	protected Predicate<E> isZero = Objects::isNull;

	/**
	 * Constructor: zero list
	 */
	public ListNestedSimple()
	{
		this(null, Objects::isNull);
	}

	/**
	 * Constructor: zero list
	 * @param zero Zero value
	 * @param isZero Test for zero
	 */
	public ListNestedSimple(E zero, Predicate<E> isZero)
	{
		// Initially list is just null (denoting all zero)
		values = null;
		setZero(zero, isZero);
	}

	/**
	 * Copy constructor
	 * @param listn ListNestedSimple to copy
	 */
	public ListNestedSimple(ListNestedSimple<E> listn)
	{
		if (listn.values == null) {
			values = null;
		} else {
			int n = listn.values.size();
			values = new ArrayList<>(n);
			for (List<E> list : listn.values) {
				values.add(list == null ? null : new ArrayList<>(list));
			}
		}
		setZero(listn.zero, listn.isZero);
	}

	/**
	 * Copy constructor with a value map, which is applied to values from the copied list.
	 * A new zero and zero test is provided since it is likely different.
	 * @param listn ListNestedSimple to copy
	 * @param valMap The value map
	 * @param zero Zero value
	 * @param isZero Test for zero
	 */
	public <T> ListNestedSimple(ListNestedSimple<T> listn, Function<? super T, ? extends E> valMap, E zero, Predicate<E> isZero)
	{
		if (listn.values == null) {
			values = null;
		} else {
			int n = listn.values.size();
			values = new ArrayList<>(n);
			for (List<T> list : listn.values) {
				if (list == null) {
					values.add(null);
				} else {
					List<E> list2 = new ArrayList<>(list.size());
					values.add(list2);
					for (T value : list) {
						list2.add(valMap.apply(value));
					}
				}
			}
		}
		setZero(zero, isZero);
	}

	/**
	 * Copy constructor with index permutation,
	 * i.e., in which index {@code i} becomes index {@code permut[i]}.
	 * @param listn ListNestedSimple to copy
	 * @param permut Index permutation
	 */
	public ListNestedSimple(ListNestedSimple<E> listn, int permut[])
	{
		if (listn.values == null) {
			values = null;
		} else {
			int n = permut.length;
			values = new ArrayList<>(n);
			values.addAll(Collections.nCopies(n, null));
			for (int i = 0; i < n; i++) {
				List<E> list = listn.getSubList(i);
				values.set(permut[i], list == null ? null : new ArrayList<>(list));
			}
		}
		setZero(listn.zero, listn.isZero);
	}

	/**
	 * Copy constructor with index permutation,
	 * i.e., in which index {@code i} becomes index {@code permut[i]},
	 * and a value map, which is applied to values from the copied list.
	 * A new zero and zero test is provided since it is likely different.
	 * @param listn ListNestedSimple to copy
	 * @param permut Index permutation
	 * @param valMap The value map
	 * @param zero Zero value
	 * @param isZero Test for zero
	 */
	public <T> ListNestedSimple(ListNestedSimple<T> listn, int permut[], Function<? super T, ? extends E> valMap, E zero, Predicate<E> isZero)
	{
		if (listn.values == null) {
			values = null;
		} else {
			int n = permut.length;
			values = new ArrayList<>(n);
			values.addAll(Collections.nCopies(n, null));
			for (int i = 0; i < n; i++) {
				List<T> list = listn.getSubList(i);
				if (list == null) {
					values.set(permut[i], null);
				} else {
					List<E> list2 = new ArrayList<>(list.size());
					values.set(permut[i], list2);
					for (T value : list) {
						list2.add(valMap.apply(value));
					}
				}
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
	 * Set the value for index {@code i}, {@code j} to {@code value}.
	 */
	public void setValue(int i, int j, E value)
	{
		// Create main list if not done yet
		if (values == null) {
			if (isZero.test(value)) {
				return;
			}
			values = new ArrayList<>();
		}
		// Expand main list up to index i if needed,
		// storing null for newly added items
		if (i >= values.size()) {
			if (isZero.test(value)) {
				return;
			}
			values.addAll(Collections.nCopies(i - values.size() + 1, null));
			int n = i - values.size() + 1;
		}
		// Create sublist for index i if needed
		List<E> list;
		if ((list = values.get(i)) == null) {
			if (isZero.test(value)) {
				return;
			}
			values.set(i, (list = new ArrayList<>()));
		}
		// Expand sublist up to index j if needed,
		// storing zero for newly added items
		if (j >= list.size()) {
			if (isZero.test(value)) {
				return;
			}
			int n = j - list.size() + 1;
			for (int j2 = 0; j2 < n; j2++) {
				list.add(zero);
			}
		}
		// Store the value
		list.set(j, value);
	}

	/**
	 * Set the list at index {@code i}, copy the list from index {code i2} of another {@link ListNestedSimple}.
	 * @param i Index to set
	 * @param listn ListNestedSimple to copy from
	 * @param i2 Index to copy from
	 */
	public void copyFrom(int i, ListNestedSimple<E> listn, int i2)
	{
		// Create main list if not done yet
		if (values == null) {
			values = new ArrayList<>();
		}
		// Expand main list up to index i if needed,
		// storing null for newly added items
		if (i >= values.size()) {
			values.addAll(Collections.nCopies(i - values.size() + 1, null));
		}
		// Copy list
		List<E> list = listn.getSubList(i2);
		values.set(i, list == null ? null : new ArrayList<>(list));
	}

	/**
	 * Clear the list at index {@code i}.
	 * @param i Index
	 */
	public void clear(int i)
	{
		if (values != null && values.size() > i && values.get(i) != null) {
			values.set(i, null);
		}
	}

	/**
	 * Get the value for index {@code i}, {@code j}.
	 */
	public E getValue(int i, int j)
	{
		List<E> list;
		if (values == null || i >= values.size() || (list = values.get(i)) == null)
			return zero;
		if (j >= list.size())
			return zero;
		return list.get(j);
	}

	/**
	 * Get the sublist for index {@code i}.
	 */
	protected List<E> getSubList(int i)
	{
		return (values == null || i >= values.size()) ? null : values.get(i);
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
