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

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import prism.PrismUtils;

/**
 * Explicit representation of a probability distribution.
 * Basically, a mapping from (integer-valued) indices to (non-zero, double-valued) probabilities. 
 */
public class Distribution implements Iterable<Entry<Integer, Double>>
{
	private HashMap<Integer, Double> map;

	/**
	 * Create an empty distribution.
	 */
	public Distribution()
	{
		clear();
	}

	/**
	 * Copy constructor.
	 */
	public Distribution(Distribution distr)
	{
		this(distr.iterator());
	}

	/**
	 * Construct a distribution from an iterator over transitions.
	 */
	public Distribution(Iterator<Entry<Integer, Double>> transitions)
	{
		this();
		while (transitions.hasNext()) {
			final Entry<Integer, Double> trans = transitions.next();
			add(trans.getKey(), trans.getValue());
		}
	}

	/**
	 * Construct a distribution from an existing one and an index permutation,
	 * i.e. in which index i becomes index permut[i].
	 * Note: have to build the new distributions from scratch anyway to do this,
	 * so may as well provide this functionality as a constructor.
	 */
	public Distribution(Distribution distr, int permut[])
	{
		this();
		Iterator<Entry<Integer, Double>> i = distr.iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Double> e = i.next();
			add(permut[e.getKey()], e.getValue());
		}
	}

	/**
	 * Clear all entries of the distribution.
	 */
	public void clear()
	{
		map = new HashMap<Integer, Double>();
	}

	/**
	 * Add 'prob' to the probability for index 'j'.
	 * Return boolean indicating whether or not there was already
	 * non-zero probability for this index (i.e. false denotes new transition).
	 */
	public boolean add(int j, double prob)
	{
		Double d = (Double) map.get(j);
		if (d == null) {
			map.put(j, prob);
			return false;
		} else {
			set(j, d + prob);
			return true;
		}
	}

	/**
	 * Set the probability for index 'j' to 'prob'.
	 */
	public void set(int j, double prob)
	{
		if (prob == 0.0)
			map.remove(j);
		else
			map.put(j, prob);
	}

	/**
	 * Get the probability for index j. 
	 */
	public double get(int j)
	{
		Double d;
		d = (Double) map.get(j);
		return d == null ? 0.0 : d.doubleValue();
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
		Iterator<Entry<Integer, Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Double> e = i.next();
			if (!set.get((Integer) e.getKey()))
				return false;
		}
		return true;
	}

	/**
	 * Returns true if at least one index in the support of the distribution is in the set. 
	 */
	public boolean containsOneOf(BitSet set)
	{
		Iterator<Entry<Integer, Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Double> e = i.next();
			if (set.get((Integer) e.getKey()))
				return true;
		}
		return false;
	}

	/**
	 * Get the support of the distribution.
	 */
	public Set<Integer> getSupport()
	{
		return map.keySet();
	}

	/**
	 * Get an iterator over the entries of the map defining the distribution.
	 */
	public Iterator<Entry<Integer, Double>> iterator()
	{
		return map.entrySet().iterator();
	}

	/**
	 * Returns true if the distribution is empty.
	 */
	public boolean isEmpty()
	{
		return map.isEmpty();
	}

	/**
	 * Get the size of the support of the distribution.
	 */
	public int size()
	{
		return map.size();
	}

	/**
	 * Get the mean of the distribution.
	 */
	public double mean()
	{
		double d = 0.0;
		Iterator<Entry<Integer, Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Double> e = i.next();
			d += e.getValue() * e.getKey();
		}
		return d;
	}
	
	/**
	 * Get the variance of the distribution.
	 */
	public double variance()
	{
		double mean = mean();
		double meanSq = 0.0;
		Iterator<Entry<Integer, Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Double> e = i.next();
			meanSq += e.getValue() * e.getKey() * e.getKey();
		}
		return Math.abs(meanSq - mean * mean);
	}
	
	/**
	 * Get the standard deviation of the distribution.
	 */
	public double standardDeviation()
	{
		return Math.sqrt(variance());
	}
	
	/**
	 * Get the relative standard deviation of the distribution,
	 * i.e., as a percentage of the mean.
	 */
	public double standardDeviationRelative()
	{
		return 100.0 * standardDeviation() / mean();
	}
	
	/**
	 * Get the sum of the probabilities in the distribution.
	 */
	public double sum()
	{
		double mean = 0.0;
		Iterator<Entry<Integer, Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Double> e = i.next();
			mean += e.getValue();
		}
		return mean;
	}

	/**
	 * Get the sum of all the probabilities in the distribution except for index j.
	 */
	public double sumAllBut(int j)
	{
		double d = 0.0;
		Iterator<Entry<Integer, Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Double> e = i.next();
			if (e.getKey() != j)
				d += e.getValue();
		}
		return d;
	}

	/**
	 * Create a new distribution, based on a mapping from the indices
	 * used in this distribution to a different set of indices.
	 */
	public Distribution map(int map[])
	{
		Distribution distrNew = new Distribution();
		Iterator<Entry<Integer, Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Double> e = i.next();
			distrNew.add(map[e.getKey()], e.getValue());
		}
		return distrNew;
	}

	@Override
	public boolean equals(Object o)
	{
		Double d1, d2;
		Distribution d = (Distribution) o;
		if (d.size() != size())
			return false;
		Iterator<Entry<Integer, Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Double> e = i.next();
			d1 = e.getValue();
			d2 = d.map.get(e.getKey());
			if (d2 == null || !PrismUtils.doublesAreClose(d1, d2, 1e-12, false))
				return false;
		}
		return true;
	}

	@Override
	public int hashCode()
	{
		// Simple hash code
		return map.size();
	}

	@Override
	public String toString()
	{
		return map.toString();
	}
	
	public String toStringCSV()
	{
		String s = "Value";
		Iterator<Entry<Integer, Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Double> e = i.next();
			s += ", " + e.getKey();
		}
		s += "\nProbability";
		i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Double> e = i.next();
			s += ", " + e.getValue();
		}
		s += "\n";
		return s;
	}
}
