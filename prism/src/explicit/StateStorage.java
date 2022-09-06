//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Mateusz Ujma <mateusz.ujma@cs.ox.ac.uk> (University of Oxford)
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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
 * Interface for storing a set of objects of type T.
 * Typically used for storing states during reachability.
 */
public interface StateStorage<T>
{
	public int get(T t);
	
	public boolean add(T state);
	
	public void clear();

	public boolean contains(T state);

	public int getIndexOfLastAdd();

	public boolean isEmpty();

	/**
	 * Get the number of objects stored in the set.
	 */
	public int size();

	/**
	 * Get access to the underlying set of map entries. 
	 */
	public Set<Map.Entry<T, Integer>> getEntrySet();
	
	/**
	 * Create an ArrayList of the states, ordered by index.
	 */
	public ArrayList<T> toArrayList();

	/**
	 * Create an ArrayList of the states, ordered by index, storing in the passed in list.
	 * @param list An empty ArrayList in which to store the result.
	 */
	public void toArrayList(ArrayList<T> list);
	
	/**
	 * Create an ArrayList of the states, ordered by permuted index.
	 * Index in new list is permut[old_index].
	 * @param permut Permutation to apply
	 */
	public ArrayList<T> toPermutedArrayList(int permut[]);

	/**
	 * Create an ArrayList of the states, ordered by permuted index, storing in the passed in list.
	 * Index in new list is permut[old_index].
	 * @param permut Permutation to apply
	 * @param list An empty ArrayList in which to store the result.
	 */
	public void toPermutedArrayList(int permut[], ArrayList<T> list);
	
	/**
	 * Build sort permutation. Assuming this was built as a sorted set,
	 * this returns a permutation (integer array) mapping current indices
	 * to new indices under the sorting order.
	 */
	public int[] buildSortingPermutation();
}
