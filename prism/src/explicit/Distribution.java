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

import prism.Evaluator;
import simulator.RandomNumberGenerator;

/**
 * Explicit representation of a probability distribution.
 * Basically, a mapping from (integer-valued) indices to (non-zero) probabilities.
 * This is a generic class where probabilities are of type {@code Value}.
 */
public class Distribution<Value> implements Iterable<Entry<Integer, Value>>
{
	/** Mapping from indices to probability values */
	private HashMap<Integer, Value> map;
	
	/** Evaluator for manipulating probability values in the distribution (of type {@code Value}) */
	@SuppressWarnings("unchecked")
	protected Evaluator<Value> eval = (Evaluator<Value>) Evaluator.createForDoubles();;

	/**
	 * Create an empty distribution.
	 * (assumes an Evaluator of type Double, unless set afterwards)
	 */
	public Distribution()
	{
		clear();
	}

	/**
	 * Create an empty distribution.
	 * (with an Evaluator to match the type parameter Value)
	 */
	public Distribution(Evaluator<Value> eval)
	{
		this();
		setEvaluator(eval);
	}

	/**
	 * Copy constructor.
	 * (the Evaluator is also copied)
	 */
	public Distribution(Distribution<Value> distr)
	{
		this(distr.iterator(), distr.getEvaluator());
	}

	/**
	 * Construct a distribution from an iterator over transitions.
	 * (assumes an Evaluator of type Double, unless set afterwards)
	 */
	public Distribution(Iterator<Entry<Integer, Value>> transitions)
	{
		this();
		while (transitions.hasNext()) {
			final Entry<Integer, Value> trans = transitions.next();
			add(trans.getKey(), trans.getValue());
		}
	}

	/**
	 * Construct a distribution from an iterator over transitions.
	 * (with an Evaluator to match the type parameter Value)
	 */
	public Distribution(Iterator<Entry<Integer, Value>> transitions, Evaluator<Value> eval)
	{
		this(transitions);
		setEvaluator(eval);
	}

	/**
	 * Construct a distribution from an existing one and an index permutation,
	 * i.e. in which index i becomes index permut[i].
	 * Note: have to build the new distributions from scratch anyway to do this,
	 * so may as well provide this functionality as a constructor.
	 * (the Evaluator is also copied)
	 */
	public Distribution(Distribution<Value> distr, int permut[])
	{
		this();
		Iterator<Entry<Integer, Value>> i = distr.iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Value> e = i.next();
			add(permut[e.getKey()], e.getValue());
		}
		setEvaluator(distr.getEvaluator());
	}

	/**
	 * Clear all entries of the distribution.
	 */
	public void clear()
	{
		map = new HashMap<Integer, Value>();
	}

	/**
	 * Add 'prob' to the probability for index 'j'.
	 * Return boolean indicating whether or not there was already
	 * non-zero probability for this index (i.e. false denotes new transition).
	 */
	public boolean add(int j, Value prob)
	{
		Value d = map.get(j);
		if (d == null) {
			map.put(j, prob);
			return false;
		} else {
			set(j, eval.add(d, prob));
			return true;
		}
	}

	/**
	 * Set the probability for index 'j' to 'prob'.
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
	 * Get the probability for index j. 
	 */
	public Value get(int j)
	{
		Value d = map.get(j);
		return d == null ? eval.zero() : d;
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
		Iterator<Entry<Integer, Value>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Value> e = i.next();
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
		Iterator<Entry<Integer, Value>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Value> e = i.next();
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
	public Iterator<Entry<Integer, Value>> iterator()
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
	 * Get the mean of the distribution.
	 */
	/*public double mean()
	{
		double d = 0.0;
		Iterator<Entry<Integer, Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Double> e = i.next();
			d += e.getValue() * e.getKey();
		}
		return d;
	}*/
	
	/**
	 * Get the variance of the distribution.
	 */
	/*public double variance()
	{
		double mean = mean();
		double meanSq = 0.0;
		Iterator<Entry<Integer, Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Double> e = i.next();
			meanSq += e.getValue() * e.getKey() * e.getKey();
		}
		return Math.abs(meanSq - mean * mean);
	}*/
	
	/**
	 * Get the standard deviation of the distribution.
	 */
	/*public double standardDeviation()
	{
		return Math.sqrt(variance());
	}*/
	
	/**
	 * Get the relative standard deviation of the distribution,
	 * i.e., as a percentage of the mean.
	 */
	/*public double standardDeviationRelative()
	{
		return 100.0 * standardDeviation() / mean();
	}*/
	
	/**
	 * Get the sum of the probabilities in the distribution.
	 */
	public Value sum()
	{
		Value d = eval.zero();
		Iterator<Entry<Integer, Value>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Value> e = i.next();
			d = eval.add(d, e.getValue());
		}
		return d;
	}

	/**
	 * Get the sum of all the probabilities in the distribution except for index j.
	 */
	public Value sumAllBut(int j)
	{
		Value d = eval.zero();
		Iterator<Entry<Integer, Value>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Value> e = i.next();
			if (e.getKey() != j) {
				d = eval.add(d, e.getValue());
			}
		}
		return d;
	}

	/**
	 * Create a new distribution, based on a mapping from the indices
	 * used in this distribution to a different set of indices.
	 */
	public Distribution<Value> map(int map[])
	{
		Distribution<Value> distrNew = new Distribution<Value>(getEvaluator());
		Iterator<Entry<Integer, Value>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Value> e = i.next();
			distrNew.add(map[e.getKey()], e.getValue());
		}
		return distrNew;
	}

	/**
	 * Set the {@link Evaluator} object for manipulating values in this distribution.
	 */
	public void setEvaluator(Evaluator<Value> eval)
	{
		this.eval = eval;
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
		Value d1, d2;
		@SuppressWarnings("unchecked")
		Distribution<Value> d = (Distribution<Value>) o;
		if (d.size() != size())
			return false;
		Iterator<Entry<Integer, Value>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<Integer, Value> e = i.next();
			d1 = e.getValue();
			d2 = d.map.get(e.getKey());
			if (d2 == null || !eval.equals(d1, d2)) {
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
