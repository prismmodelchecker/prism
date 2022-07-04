//==============================================================================
//	
//	Copyright (c) 2022-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Function;

import simulator.RandomNumberGenerator;

/**
 * Explicit representation of a probability distribution over objects of type T.
 */
public class DistributionOver<T> implements Iterable<Entry<T, Double>>
{
	// Stored as a Distribution (over integer indices) and a mapping from indices to Objects
	private Distribution distr;
	private Function<Integer, T> objects;
	
	/**
	 * Create from a Distribution (over integer indices) and a mapping from indices to Objects
	 */
	public static <T> DistributionOver<T> create(Distribution distr, Function<Integer, T> objects)
	{
		DistributionOver<T> d = new DistributionOver<>();
		d.distr = distr;
		d.objects = objects;
		return d;
	}
	
	/**
	 * Get the probability assigned to a value
	 */
	public double getProbability(T val)
	{
		for (Entry<Integer, Double> elem : distr) {
			T act = objects.apply(elem.getKey());
			if (Objects.equals(act, val)) {
				return elem.getValue();
			}
		}
		return 0;
	}
	
	/**
	 * Get an iterator over the entries of the map defining the distribution.
	 */
	public Iterator<Entry<T, Double>> iterator()
	{
		return new Iterator<Entry<T, Double>>()
		{
			Iterator<Entry<Integer, Double>> iter = distr.iterator();

			@Override
			public boolean hasNext()
			{
				return iter.hasNext();
			}

			@Override
			public Entry<T, Double> next()
			{
				Entry<Integer, Double> e = iter.next();
				return new AbstractMap.SimpleImmutableEntry<>(objects.apply(e.getKey()), e.getValue());
			}
		};
	}

	/**
	 * Sample an index at random from the distribution.
	 * Returns null if the distribution is empty.
	 */
	public T sample()
	{
		int i = distr.sample();
		return i == -1 ? null : objects.apply(i);
	}
	
	/**
	 * Sample an index at random from the distribution, using the specified RandomNumberGenerator.
	 * Returns null if the distribution is empty.
	 */
	public T sample(RandomNumberGenerator rng)
	{
		int i = distr.sample(rng);
		return i == -1 ? null : objects.apply(i);
	}
}
