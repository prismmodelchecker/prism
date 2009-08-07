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
import java.util.Map.Entry;

import prism.PrismUtils;

public class Distribution implements Iterable<Entry<Integer,Double>>
{
	private HashMap<Integer,Double> map;

	public Distribution()
	{
		clear();
	}
	
	public void clear()
	{
		map = new HashMap<Integer,Double>();
	}
	
	public void add(int j, double prob)
	{
		Double d = (Double) map.get(j);
		if (d == null)
			map.put(j, prob);
		else
			map.put(j, d + prob);
	}

	public void set(int j, double prob)
	{
		if (prob == 0.0)
			map.remove(j);
		map.put(j, prob);
	}

	public double get(int j)
	{
		Double d;
		d = (Double) map.get(j);
		return d==null ? 0.0 : d.doubleValue();
	}
	
	/**
	 * Returns true if index j is in the support of the distribution. 
	 */
	public boolean contains(int j)
	{
		return map.get(j) != null;
	}
	
	/**
	 * Returns true if all indices in the support of the distribution are in the set. 
	 */
	public boolean isSubsetOf(BitSet set)
	{
		Iterator<Entry<Integer,Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer,Double> e = i.next();
			if (!set.get((Integer) e.getKey()))
				return false;
		}
		return true;
	}
	
	public Iterator<Entry<Integer,Double>> iterator()
	{
		return map.entrySet().iterator();
	}

	public boolean isEmpty()
	{
		return map.isEmpty();
	}
	
	public int size()
	{
		return map.size();
	}
	
	public double sum()
	{
		double d = 0.0;
		Iterator<Entry<Integer,Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer,Double> e = i.next();
			d += e.getValue();
		}
		return d;
	}
	
	public double sumAllBut(int j)
	{
		double d = 0.0;
		Iterator<Entry<Integer,Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer,Double> e = i.next();
			if (e.getKey() != j)
				d += e.getValue();
		}
		return d;
	}
	
	public boolean equals(Object o)
	{
		Double d1, d2;
		Distribution d = (Distribution) o;
		Iterator<Entry<Integer,Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer,Double> e = i.next();
			d1 = e.getValue();
			d2 = d.map.get(e.getKey());
			if (d2 == null || !PrismUtils.doublesAreClose(d1, d2, 1e-12, false))
				return false;
		}
		return true;
	}

	public int hashCode()
	{
		// Simple hash code
		return map.size();
	}

	public String toString()
	{
		return "" + map;
	}
}
