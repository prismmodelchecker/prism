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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import common.iterable.FunctionalIterable;
import common.iterable.FunctionalIterator;
import common.iterable.Reducible;
import prism.Evaluator;
import simulator.RandomNumberGenerator;

/**
 * Explicit representation of a probability distribution.
 * Basically, a mapping from (integer-valued) indices to (non-zero) probabilities.
 * This is a generic class where probabilities are of type {@code Value}.
 */
public class Distribution<Value> implements FunctionalIterable<Entry<Integer, Value>>
{
	/** Mapping from indices to probability values */
	protected final HashMap<Integer, Value> map;
	
	/** Evaluator for manipulating probability values in the distribution (of type {@code Value}) */
	protected final Evaluator<Value> eval;

	/**
	 * Create an empty distribution
	 * (assumes an Evaluator of type Double, unless set afterwards).
	 * This constructor is deprecated: it is preferable to either use the
	 * static creation methods such as {@link #ofDouble()}, or pass an
	 * evaluator, e.g., with {@link #Distribution(Evaluator)}.
	 */
	@Deprecated
	public Distribution()
	{
		this((Evaluator<Value>) Evaluator.forDouble());
	}

	/**
	 * Create an empty distribution.
	 * (with an Evaluator to match the type parameter Value)
	 */
	public Distribution(Evaluator<Value> eval)
	{
		this.eval = eval;
		this.map = new HashMap<>();
	}

	/**
	 * Construct a distribution from an iterator over transitions
	 * (with an Evaluator to match the type parameter Value).
	 */
	public Distribution(Iterator<Entry<Integer, Value>> transitions, Evaluator<Value> eval)
	{
		this(eval);
		// use #add to ensure probabilities sum up for any duplicated indices
		transitions.forEachRemaining(t -> add(t.getKey(), t.getValue()));
	}

	/**
	 * Copy constructor
	 * (the Evaluator is also copied).
	 */
	public Distribution(Distribution<Value> distr)
	{
		this(distr.getEvaluator());
		// use Map#put since for each index we get only one probability
		distr.forEach(t -> map.put(t.getKey(), t.getValue()));
	}

	/**
	 * Construct a distribution from an existing one and an index permutation,
	 * i.e. in which index i becomes index permut[i]
	 * (the Evaluator is also copied).
	 * Note: have to build the new distributions from scratch anyway to do this,
	 * so may as well provide this functionality as a constructor.
	 */
	public Distribution(Distribution<Value> distr, int permut[])
	{
		this(distr.getEvaluator());
		// use #add to ensure probabilities sum up for each index
		distr.forEach(t -> add(permut[t.getKey()], t.getValue()));
	}

	/**
	 * Construct an empty distribution
	 * assuming an Evaluator of type Double.
	 */
	public static Distribution<Double> ofDouble()
	{
		return new Distribution<>(Evaluator.forDouble());
	}

	/**
	 * Construct a distribution from an iterator over transitions
	 * assuming an Evaluator of type Double.
	 */
	public static Distribution<Double> ofDouble(Iterator<Entry<Integer, Double>> transitions)
	{
		return new Distribution<Double>(transitions, Evaluator.forDouble());
	}

	/**
	 * Clear all entries of the distribution.
	 */
	public void clear()
	{
		map.clear();
	}

	/**
	 * Add non-negative {@code prob} to the probability for index {@code j}.
	 * Return boolean {@code true} if no new transition is created,
	 * i.e., {@code false} indicates a new transition with prob > 0.
	 *
	 * @return {@code true} iff p(j) != 0 || prob == 0
	 */
	public boolean add(int j, Value prob)
	{
		if (eval.isZero(prob)) {
			return true;
		}
		Value sum = map.merge(j, prob, eval::add);
		return sum != prob;

	}

	/**
	 * Set the probability for index {@code j} to {@code prob}.
	 */
	public void set(int j, Value prob)
	{
		if (eval.isZero(prob)) {
			map.remove(j);
		} else {
			map.put(j, prob);
		}
	}

	/**
	 * Get the probability for index {@code j}.
	 */
	public Value get(int j)
	{
		return map.getOrDefault(j, eval.zero());
	}

	/**
	 * Returns true if index {@code j} is in the support of the distribution.
	 */
	public boolean contains(int j)
	{
		return map.containsKey(j);
	}

	/**
	 * Returns true if all indices in the support of the distribution are in the set. 
	 */
	public boolean isSubsetOf(BitSet set)
	{
		return Reducible.extend(getSupport()).allMatch(set::get);
	}

	/**
	 * Returns true if at least one index in the support of the distribution is in the set. 
	 */
	public boolean containsOneOf(BitSet set)
	{
		return Reducible.extend(getSupport()).anyMatch(set::get);
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
	public FunctionalIterator<Entry<Integer, Value>> iterator()
	{
		return Reducible.extend(map.entrySet().iterator());
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
	 * Sample an index at random from the distribution.
	 * Returns -1 if the distribution is empty.
	 */
	public int sample()
	{
		return getValueByProbabilitySum(Math.random());
	}
	
	/**
	 * Sample an index at random from the distribution, using the specified RandomNumberGenerator.
	 * Returns -1 if the distribution is empty.
	 */
	public int sample(RandomNumberGenerator rng)
	{
		return getValueByProbabilitySum(rng.randomUnifDouble());
	}
	
	/**
	 * Get the first index for which the sum of probabilities of that and all prior indices exceeds x.
	 * Returns -1 if the distribution is empty.
	 * @param x Probability sum
	 */
	private int getValueByProbabilitySum(double x)
	{
		if (isEmpty()) {
			return -1;
		}
		Iterator<Entry<Integer, Value>> i = iterator();
		Map.Entry<Integer, Value> e = null;
		Value tot = eval.zero();
		while (x >= eval.toDouble(tot) && i.hasNext()) {
			e = i.next();
			tot = eval.add(tot, e.getValue());
		}
		return e.getKey();
	}

	/**
	 * Get the sum of the probabilities in the distribution.
	 */
	public Value sum()
	{
		return map(Entry::getValue).reduce(eval.zero(), eval::add);
	}

	/**
	 * Get the sum of all the probabilities in the distribution except for index j.
	 */
	public Value sumAllBut(int j)
	{
		return filter(t -> t.getKey() != j).map(Entry::getValue).reduce(eval.zero(), eval::add);
	}

	/**
	 * Create a new distribution, based on a mapping from the indices
	 * used in this distribution to a different set of indices.
	 */
	public Distribution<Value> map(int map[])
	{
		return new Distribution<Value>(this, map);
	}

	/**
	 * Get an Evaluator for the probability values stored in this distribution.
	 * This is need, for example, to compute probability sums, check for equality to 0/1, etc.
	 * By default, this is initialised to an evaluator for the (usual) case when Value is Double.
	 */
	public Evaluator<Value> getEvaluator()
	{
		return eval;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (! (o instanceof Distribution)) {
			return false;
		}
		// Check elements of distribution using evaluator equals method
		HashMap<Integer,Value> oMap = ((Distribution<Value>) o).map;
		if (map.size() != oMap.size()) {
			return false;
		}
		for (Map.Entry<Integer,Value> entry : map.entrySet()) {
			Integer key = entry.getKey();
			Value value = entry.getValue(); // We assume nothing maps to null
			Value oValue = oMap.get(key);
			if (oValue == null || !getEvaluator().equals(value, oValue)) {
				return false;
			}
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
		Iterator<Entry<Integer, Value>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Value> e = i.next();
			s += ", " + e.getKey();
		}
		s += "\nProbability";
		i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Value> e = i.next();
			s += ", " + e.getValue();
		}
		s += "\n";
		return s;
	}
}
