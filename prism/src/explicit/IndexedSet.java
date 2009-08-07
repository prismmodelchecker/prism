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
public class IndexedSet<T>
{
	private HashMap<T, Integer> set;
	private int indexOfLastAdd;

	public IndexedSet()
	{
		set = new HashMap<T, Integer>();
		indexOfLastAdd = -1;
	}

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

	public boolean contains(T state)
	{
			return set.get(state) != null;
	}

	public int getIndexOfLastAdd()
	{
		return indexOfLastAdd;
	}

	public boolean isEmpty()
	{
		return set.isEmpty();
	}

	/**
	 * Get the number of objects stored in the set.
	 */
	public int size()
	{
		return set.size();
	}

	/**
	 * Create an ArrayList of the states, ordered by index.
	 */
	public ArrayList<T> toArrayList()
	{
		ArrayList<T> list = new ArrayList<T>(set.size());
		toArrayList(list);
		return list;
	}

	/**
	 * Create an ArrayList of the states, ordered by index, storing in the passed in list.
	 * @param storeHere: An empty ArrayList in which to store the result.
	 */
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
	
	@Override
	public String toString()
	{
		return set.toString();
	}
}
