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

package explicit.rewards;

import java.util.ArrayList;
import java.util.List;

import explicit.Model;
import explicit.Product;

/**
 * Simple explicit-state storage of rewards for an MDP.
 * Like the related class MDPSimple, this is not especially efficient, but mutable (in terms of size).
 */
public class MDPRewardsSimple implements MDPRewards
{
	/** Number of states */
	protected int numStates;
	/** State rewards */
	protected List<Double> stateRewards;
	/** Transition rewards */
	protected List<List<Double>> transRewards;

	/**
	 * Constructor: all zero rewards.
	 * @param numStates Number of states
	 */
	public MDPRewardsSimple(int numStates)
	{
		this.numStates = numStates;
		// Initially lists are just null (denoting all 0)
		stateRewards = null;
		transRewards = null;
	}

	/**
	 * Copy constructor
	 * @param rews Rewards to copy
	 */
	public MDPRewardsSimple(MDPRewardsSimple rews)
	{
		numStates = rews.numStates;
		if (rews.stateRewards == null) {
			stateRewards = null;
		} else {
			stateRewards = new ArrayList<Double>(numStates);
			for (int i = 0; i < numStates; i++) {
				stateRewards.add(rews.stateRewards.get(i));
			}
		}
		if (rews.transRewards == null) {
			transRewards = null;
		} else {
			transRewards = new ArrayList<List<Double>>(numStates);
			for (int i = 0; i < numStates; i++) {
				List<Double> list = rews.transRewards.get(i);
				if (list == null) {
					transRewards.add(null);
				} else {
					int n = list.size();
					List<Double> list2 = new ArrayList<Double>(n);
					transRewards.add(list2);
					for (int j = 0; j < n; j++) {
						list2.add(list.get(j));
					}
				}
			}
		}
	}

	// Mutators

	/**
	 * Set the state reward for state {@code s} to {@code r}.
	 */
	public void setStateReward(int s, double r)
	{
		// If no rewards array created yet, create it
		if (stateRewards == null) {
			stateRewards = new ArrayList<Double>(numStates);
			for (int j = 0; j < numStates; j++)
				stateRewards.add(0.0);
		}
		// Set reward
		stateRewards.set(s, r);
	}

	/**
	 * Add {@code r} to the state reward for state {@code s}.
	 */
	public void addToStateReward(int s, double r)
	{
		setStateReward(s, getStateReward(s) + r);
	}

	/**
	 * Set the transition reward for choice {@code i} of state {@code s} to {@code r}.
	 */
	public void setTransitionReward(int s, int i, double r)
	{
		List<Double> list;
		// If no rewards array created yet, create it
		if (transRewards == null) {
			transRewards = new ArrayList<List<Double>>(numStates);
			for (int j = 0; j < numStates; j++)
				transRewards.add(null);
		}
		// If no rewards for state s yet, create list
		if (transRewards.get(s) == null) {
			list = new ArrayList<Double>();
			transRewards.set(s, list);
		} else {
			list = transRewards.get(s);
		}
		// If list not big enough, extend
		int n = i - list.size() + 1;
		if (n > 0) {
			for (int j = 0; j < n; j++) {
				list.add(0.0);
			}
		}
		// Set reward
		list.set(i, r);
	}

	/**
	 * Add {@code r} to the transition reward for choice {@code i} of state {@code s}.
	 */
	public void addToTransitionReward(int s, int i, double r)
	{
		setTransitionReward(s, i, getTransitionReward(s, i) + r);
	}

	/**
	 * Clear all rewards for state s.
	 */
	public void clearRewards(int s)
	{
		setStateReward(s, 0.0);
		if (transRewards != null && transRewards.size() > s) {
			transRewards.set(s, null);
		}
	}

	// Accessors

	@Override
	public double getStateReward(int s)
	{
		if (stateRewards == null)
			return 0.0;
		return stateRewards.get(s);
	}

	@Override
	public double getTransitionReward(int s, int i)
	{
		List<Double> list;
		if (transRewards == null || (list = transRewards.get(s)) == null)
			return 0.0;
		if (list.size() <= i)
			return 0.0;
		return list.get(i);
	}

	// Converters
	
	@Override
	public MDPRewards liftFromModel(Product<? extends Model> product)
	{
		Model modelProd = product.getProductModel();
		int numStatesProd = modelProd.getNumStates();		
		MDPRewardsSimple rewardsProd = new MDPRewardsSimple(numStatesProd);
		if (stateRewards != null) {
			for (int s = 0; s < numStatesProd; s++) {
				rewardsProd.setStateReward(s, stateRewards.get(product.getModelState(s)));
			}
		}
		if (transRewards != null) {
			for (int s = 0; s < numStatesProd; s++) {
				List<Double> list = transRewards.get(product.getModelState(s));
				if (list != null) {
					int numChoices = list.size();
					for (int i = 0; i < numChoices; i++) {
						rewardsProd.setTransitionReward(s, i, list.get(i));
					}
				}
			}
		}
		return rewardsProd;
	}
	
	@Override
	public String toString()
	{
		return "st: " + this.stateRewards + "; tr:" + this.transRewards;
	}

	@Override
	public boolean hasTransitionRewards()
	{
		return transRewards != null;
	}
}
