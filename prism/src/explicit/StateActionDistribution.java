//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import prism.PrismUtils;

/**
 * Stores a distributions over a state-action pairs.
 */
public class StateActionDistribution implements Iterable<Entry<StateAction,Double>>
{
	private HashMap<StateAction,Double> map;
	
	/**
	 * Create an empty distribution.
	 */
	public StateActionDistribution()
	{
		clear();
	}

	/**
	 * Copy constructor.
	 */
	public StateActionDistribution(StateActionDistribution distr)
	{
		this();
		Iterator<Entry<StateAction,Double>> i = distr.iterator();
		while (i.hasNext()) {
			Map.Entry<StateAction,Double> e = i.next();
			add(e.getKey(), e.getValue());
		}
	}

	/**
	 * Construct a distribution from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 * Note: have to build the new distributions from scratch anyway to do this,
	 * so may as well provide this functionality as a constructor.
	 */
	public StateActionDistribution(StateActionDistribution distr, int permut[])
	{
		this();
		Iterator<Entry<StateAction,Double>> i = distr.iterator();
		while (i.hasNext()) {
			Map.Entry<StateAction,Double> e = i.next();
			StateAction oldStateAction = e.getKey();
			StateAction newStateAction = new StateAction(permut[oldStateAction.getState()], oldStateAction.getAction());
			add(newStateAction, e.getValue());
		}
	}

	/**
	 * Get probability for state state/action 0.
	 * 
	 * @param state state to get probability of
	 * @return probability
	 */
	public double get(int state)
	{
		return get(state, null);
	}
	
	/**
	 * Get the probability for state/action pair 
	 */
	public double get(int state, Object action)
	{
		StateAction entry = new StateAction(state,  action);
		return get(entry);
	}

	/**
	 * Get the probability for given entry
	 */
	public double get(StateAction entry)
	{
		Double d;
		d = (Double) map.get(entry);
		return d==null ? 0.0 : d.doubleValue();		
	}
	
	@Override
	public Iterator<Entry<StateAction, Double>> iterator()
	{
		return map.entrySet().iterator();
	}
	
	/**
	 * Clear all entries of the distribution.
	 */
	public void clear()
	{
		map = new HashMap<StateAction,Double>();
	}
	
	public boolean add(int state, double prob)
	{
		return add(state, null, prob);
	}
	
	/**
	 * Add 'prob' to the probability for given state and action.
	 * Return boolean indicating whether or not there was already
	 * non-zero probability for this state (i.e. false denotes new transition).
	 */
	public boolean add(int state, Object action, double prob)
	{
		StateAction entry = new StateAction(state, action);
		return add(entry, prob);
	}
	
	public boolean add(StateAction entry, double prob)
	{
		Double d = (Double) map.get(entry);
		if (d == null) {
			map.put(entry, prob);
			return false;
		} else {
			set(entry, d + prob);
			return true;
		}		
	}
	
	/**
	 * Set the probability for state/action to prob.
	 */
	public void set(int state, Object action, double prob)
	{
		StateAction entry = new StateAction(state, action);
		set(entry, prob);
	}

	public void set(int state, double prob) {
		set(state, null, prob);
	}
	
	public void set(StateAction entry, double prob)
	{
		if (prob == 0.0)
			map.remove(entry);
		else
			map.put(entry, prob);
	}
	
	/**
	 * Returns true if index j is in the support of the distribution. 
	 */
	public boolean contains(int state, Object action)
	{
		StateAction entry = new StateAction(state, action);
		return map.get(entry) != null;
	}
	
	public boolean contains(int state)
	{
		return contains(state, null);
	}
	
	/**
	 * Returns true if all states in the support of the distribution are in the set. 
	 */
	public boolean isSubsetOf(BitSet set)
	{
		Iterator<Entry<StateAction,Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<StateAction,Double> e = i.next();
			if (!set.get(((StateAction) e.getKey()).getState()))
				return false;
		}
		return true;
	}
	
	/**
	 * Get the support of the distribution.
	 */
	public Set<Integer> getSupport()
	{
		HashSet<Integer> support = new HashSet<Integer>();
		for (StateAction stateAction : map.keySet()) {
			support.add(stateAction.getState());
		}
		return support;
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
	 * Get the sum of the probabilities in the distribution.
	 */
	public double sum()
	{
		double d = 0.0;
		Iterator<Entry<StateAction,Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<StateAction,Double> e = i.next();
			d += e.getValue();
		}
		return d;
	}
	
	/**
	 * Get the sum of all the probabilities in the distribution except for state j.
	 */
	public double sumAllBut(int j)
	{
		double d = 0.0;
		Iterator<Entry<StateAction,Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<StateAction,Double> e = i.next();
			if (e.getKey().getState() != j)
				d += e.getValue();
		}
		return d;
	}
	
	/**
	 * Create a new distribution, based on a mapping from the state indices
	 * used in this distribution to a different set  of state indices.
	 */
	public StateActionDistribution map(int map[])
	{
		StateActionDistribution distrNew = new StateActionDistribution();
		Iterator<Entry<StateAction,Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<StateAction,Double> e = i.next();
			StateAction oldStateAction = e.getKey();
			StateAction newStateAction = new StateAction(map[oldStateAction.getState()], oldStateAction.getAction());
			distrNew.add(newStateAction, e.getValue());
		}
		return distrNew;
	}
	
	@Override
	public boolean equals(Object o)
	{
		Double d1, d2;
		StateActionDistribution d = (StateActionDistribution) o;
		if (d.size() != size())
			return false;
		Iterator<Entry<StateAction,Double>> i = iterator();
		while (i.hasNext()) {
			Map.Entry<StateAction,Double> e = i.next();
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

	public boolean containsOneOf(BitSet set)
	{
		for (StateAction stateAction : map.keySet()) {
			if (set.get(stateAction.getState()));
		}
		return false;
	}

	public Distribution toDistribution()
	{
		Distribution result = new Distribution();
		for (Entry<StateAction, Double> entry : map.entrySet()) {
			result.add(entry.getKey().getState(), entry.getValue());
		}
		
		return result;
	}
}
