//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

import java.util.*;

/**
 * Class storing an indexed set of objects of type T.
 * Typically used for storing state space during reachability.
 */
public class IndexedSet<T> implements StateStorage<T>
{
	protected Map<T, Integer> set;
	protected int indexOfLastAdd;

	public IndexedSet()
	{
		this(false);
	}

	public IndexedSet(boolean sorted)
	{
		indexOfLastAdd = -1;
		set = sorted ? new TreeMap<T, Integer>() : new HashMap<T, Integer>();
	}
	
	public IndexedSet(Comparator<T> comparator)
	{
		indexOfLastAdd = -1;
		set = new TreeMap<T, Integer>(comparator);
	}

	@Override
	public void clear()
	{
		set.clear();
	}
	
	@Override
	public boolean add(T state)
	{
		Integer i = set.get(state);
		if (i != null) {
			indexOfLastAdd = i;
			return false;
		} else {
			indexOfLastAdd = set.size();
			set.put(state, set.size());
			return true;
		}
	}

	@Override
	public boolean contains(T state)
	{
			return set.get(state) != null;
	}

	@Override
	public int getIndexOfLastAdd()
	{
		return indexOfLastAdd;
	}

	@Override
	public boolean isEmpty()
	{
		return set.isEmpty();
	}

	/**
	 * Get the number of objects stored in the set.
	 */
	@Override
	public int size()
	{
		return set.size();
	}

	/**
	 * Get access to the underlying set of map entries. 
	 */
	@Override
	public Set<Map.Entry<T, Integer>> getEntrySet()
	{
		return set.entrySet();
	}
	
	/**
	 * Create an ArrayList of the states, ordered by index.
	 */
	@Override
	public ArrayList<T> toArrayList()
	{
		ArrayList<T> list = new ArrayList<T>(set.size());
		toArrayList(list);
		return list;
	}

	/**
	 * Create an ArrayList of the states, ordered by index, storing in the passed in list.
	 * @param list An empty ArrayList in which to store the result.
	 */
	@Override
	public void toArrayList(ArrayList<T> list)
	{
		int i, n;

		n = set.size();
		for (i = 0; i < n ; i++)
			list.add(null);
		for (Map.Entry<T, Integer> e : set.entrySet()) {
			list.set(e.getValue(), e.getKey());
		}
	}
	
	/**
	 * Create an ArrayList of the states, ordered by permuted index.
	 * Index in new list is permut[old_index].
	 * @param permut Permutation to apply
	 */
	@Override
	public ArrayList<T> toPermutedArrayList(int permut[])
	{
		ArrayList<T> list = new ArrayList<T>(set.size());
		toPermutedArrayList(permut, list);
		return list;
	}

	/**
	 * Create an ArrayList of the states, ordered by permuted index, storing in the passed in list.
	 * Index in new list is permut[old_index].
	 * @param permut Permutation to apply
	 * @param list An empty ArrayList in which to store the result.
	 */
	@Override
	public void toPermutedArrayList(int permut[], ArrayList<T> list)
	{
		int i, n;

		n = set.size();
		for (i = 0; i < n ; i++)
			list.add(null);
		for (Map.Entry<T, Integer> e : set.entrySet()) {
			list.set(permut[e.getValue()], e.getKey());
		}
	}
	
	/**
	 * Build sort permutation. Assuming this was built as a sorted set,
	 * this returns a permutation (integer array) mapping current indices
	 * to new indices under the sorting order.
	 */
	@Override
	public int[] buildSortingPermutation()
	{
		int i, n;
		int perm[];
		
		n = set.size();
		perm = new int[n];
		i = 0;
		for (Map.Entry<T, Integer> e : set.entrySet()) {
			perm[e.getValue()] = i++;
		}
		
		return perm;
	}
	
	@Override
	public String toString()
	{
		return set.toString();
	}

	@Override
	public int get(T t)
	{
		return set.get(t);
	}
}
