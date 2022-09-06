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

package pta;

import java.util.*;

public class LocZoneSet
{
	private HashMap<LocZone, Integer> set;
	private int indexOfLastAdd;

	public LocZoneSet()
	{
		set = new HashMap<LocZone, Integer>();
		indexOfLastAdd = -1;
	}

	public boolean add(int loc, Zone z)
	{
		return add(new LocZone(loc, z));
	}

	public boolean add(LocZone lz)
	{
		Integer i = set.get(lz);
		if (i != null) {
			indexOfLastAdd = i;
			return false;
		} else {
			indexOfLastAdd = set.size();
			set.put(lz, set.size());
			return true;
		}
	}

	public boolean contains(LocZone lz)
	{
			return set.get(lz) != null;
	}

	public int getIndexOfLastAdd()
	{
		return indexOfLastAdd;
	}

	public boolean isEmpty()
	{
		return set.isEmpty();
	}

	public int size()
	{
		return set.size();
	}

	public ArrayList<LocZone> toArrayList()
	{
		int i, n;

		n = set.size();
		ArrayList<LocZone> list = new ArrayList<LocZone>(n);
		for (i = 0; i < n ; i++)
			list.add(null);
		for (Map.Entry<LocZone, Integer> e : set.entrySet()) {
			list.set(e.getValue(), e.getKey());
		}
		return list;
	}

	public String toString()
	{
		String s = "";
		s += set;
		return s;
	}
}
