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
 * Explicit representation of a probability distribution over objects of type {@code T}.
 * This class is also generic in terms of the type for probabilities {@code Value}.
 */
public class DistributionOver<Value,T> implements Iterable<Entry<T, Value>>
{
	// Stored as a Distribution (over integer indices) and a mapping from indices to Objects
	private Distribution<Value> distr;
	private Function<Integer, T> objects;
	
	/**
	 * Create from a Distribution (over integer indices) and a mapping from indices to Objects
	 */
	public static <Value,T> DistributionOver<Value,T> create(Distribution<Value> distr, Function<Integer, T> objects)
	{
		DistributionOver<Value,T> d = new DistributionOver<>();
		d.distr = distr;
		d.objects = objects;
		return d;
	}
	
	/**
	 * Get the probability assigned to a value
	 */
	public Value getProbability(T val)
	{
		for (Entry<Integer, Value> elem : distr) {
			T act = objects.apply(elem.getKey());
			if (Objects.equals(act, val)) {
				return elem.getValue();
			}
		}
		return distr.getEvaluator().zero();
	}
	
	/**
	 * Get an iterator over the entries of the map defining the distribution.
	 */
	public Iterator<Entry<T, Value>> iterator()
	{
		return new Iterator<Entry<T, Value>>()
		{
			Iterator<Entry<Integer, Value>> iter = distr.iterator();

			@Override
			public boolean hasNext()
			{
				return iter.hasNext();
			}

			@Override
			public Entry<T, Value> next()
			{
				Entry<Integer, Value> e = iter.next();
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
