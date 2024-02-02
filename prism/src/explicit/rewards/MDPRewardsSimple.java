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
import java.util.function.Function;

import explicit.Model;
import explicit.NondetModel;
import explicit.Product;
import prism.Evaluator;

/**
 * Simple explicit-state storage of rewards for an MDP.
 * Like the related class MDPSimple, this is not especially efficient, but mutable (in terms of size).
 */
public class MDPRewardsSimple<Value> extends RewardsExplicit<Value> implements MDPRewards<Value>
{
	/** Number of states */
	protected int numStates;
	/** State rewards */
	protected List<Value> stateRewards;
	/** Transition rewards */
	protected List<List<Value>> transRewards;

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
	public MDPRewardsSimple(MDPRewardsSimple<Value> rews)
	{
		setEvaluator(rews.getEvaluator());
		numStates = rews.numStates;
		if (rews.stateRewards == null) {
			stateRewards = null;
		} else {
			stateRewards = new ArrayList<Value>(numStates);
			for (int i = 0; i < numStates; i++) {
				stateRewards.add(rews.stateRewards.get(i));
			}
		}
		if (rews.transRewards == null) {
			transRewards = null;
		} else {
			transRewards = new ArrayList<List<Value>>(numStates);
			for (int i = 0; i < numStates; i++) {
				List<Value> list = rews.transRewards.get(i);
				if (list == null) {
					transRewards.add(null);
				} else {
					int n = list.size();
					List<Value> list2 = new ArrayList<>(n);
					transRewards.add(list2);
					for (int j = 0; j < n; j++) {
						list2.add(list.get(j));
					}
				}
			}
		}
	}

	/**
	 * Copy constructor.
	 * @param rews Rewards to copy
	 * @param model Associated model (needed for sizes)
	 */
	public MDPRewardsSimple(MDPRewards<Value> rews, NondetModel<?> model)
	{
		this(rews, model, r -> r);
	}

	/**
	 * Copy constructor, mapping reward values using the provided function.
	 * Since the type changes (T -> Value), an Evaluator for Value must be given.
	 * @param rews Rewards to copy
	 * @param model Associated model (needed for sizes)
	 * @param rewMap Reward value map
	 */
	public MDPRewardsSimple(MDPRewards<Value> rews, NondetModel<?> model, Function<? super Value, ? extends Value> rewMap)
	{
		this(rews, model, rewMap, rews.getEvaluator());
	}

	/**
	 * Copy constructor, mapping reward values using the provided function.
	 * Since the type changes (T -> Value), an Evaluator for Value must be given.
	 * @param rews Rewards to copy
	 * @param model Associated model (needed for sizes)
	 * @param rewMap Reward value map
	 * @param eval Evaluator for Value
	 */
	public <T> MDPRewardsSimple(MDPRewards<T> rews, NondetModel<?> model, Function<? super T, ? extends Value> rewMap, Evaluator<Value> eval)
	{
		setEvaluator(eval);
		numStates = model.getNumStates();
		if (rews.hasStateRewards()) {
			stateRewards = new ArrayList<Value>(numStates);
			for (int i = 0; i < numStates; i++) {
				stateRewards.add(rewMap.apply(rews.getStateReward(i)));
			}
		}
		if (rews.hasTransitionRewards()) {
			transRewards = new ArrayList<>(numStates);
			for (int i = 0; i < numStates; i++) {
				int numChoices = model.getNumChoices(i);
				List<Value> list = new ArrayList<>(numChoices);
				transRewards.add(list);
				for (int j = 0; j < numChoices; j++) {
					list.add(rewMap.apply(rews.getTransitionReward(i, j)));
				}
			}
		}
	}

	// Mutators

	/**
	 * Set the state reward for state {@code s} to {@code r}.
	 */
	public void setStateReward(int s, Value r)
	{
		// If no rewards array created yet, create it
		if (stateRewards == null) {
			stateRewards = new ArrayList<Value>(numStates);
			for (int j = 0; j < numStates; j++)
				stateRewards.add(getEvaluator().zero());
		}
		// Set reward
		stateRewards.set(s, r);
	}

	/**
	 * Add {@code r} to the state reward for state {@code s}.
	 */
	public void addToStateReward(int s, Value r)
	{
		setStateReward(s, getEvaluator().add(getStateReward(s), r));
	}

	/**
	 * Set the transition reward for choice {@code i} of state {@code s} to {@code r}.
	 */
	public void setTransitionReward(int s, int i, Value r)
	{
		List<Value> list;
		// If no rewards array created yet, create it
		if (transRewards == null) {
			transRewards = new ArrayList<List<Value>>(numStates);
			for (int j = 0; j < numStates; j++)
				transRewards.add(null);
		}
		// If no rewards for state s yet, create list
		if (transRewards.get(s) == null) {
			list = new ArrayList<Value>();
			transRewards.set(s, list);
		} else {
			list = transRewards.get(s);
		}
		// If list not big enough, extend
		int n = i - list.size() + 1;
		if (n > 0) {
			for (int j = 0; j < n; j++) {
				list.add(getEvaluator().zero());
			}
		}
		// Set reward
		list.set(i, r);
	}

	/**
	 * Add {@code r} to the transition reward for choice {@code i} of state {@code s}.
	 */
	public void addToTransitionReward(int s, int i, Value r)
	{
		setTransitionReward(s, i, getEvaluator().add(getTransitionReward(s, i), r));
	}

	/**
	 * Clear all rewards for state s.
	 */
	public void clearRewards(int s)
	{
		setStateReward(s, getEvaluator().zero());
		if (transRewards != null && transRewards.size() > s) {
			transRewards.set(s, null);
		}
	}

	// Accessors

	@Override
	public Value getStateReward(int s)
	{
		if (stateRewards == null)
			return getEvaluator().zero();
		return stateRewards.get(s);
	}

	@Override
	public Value getTransitionReward(int s, int i)
	{
		List<Value> list;
		if (transRewards == null || (list = transRewards.get(s)) == null)
			return getEvaluator().zero();
		if (list.size() <= i)
			return getEvaluator().zero();
		return list.get(i);
	}

	// Converters
	
	@Override
	public MDPRewards<Value> liftFromModel(Product<?> product)
	{
		Model<?> modelProd = product.getProductModel();
		int numStatesProd = modelProd.getNumStates();		
		MDPRewardsSimple<Value> rewardsProd = new MDPRewardsSimple<>(numStatesProd);
		rewardsProd.setEvaluator(getEvaluator());
		if (stateRewards != null) {
			for (int s = 0; s < numStatesProd; s++) {
				rewardsProd.setStateReward(s, stateRewards.get(product.getModelState(s)));
			}
		}
		if (transRewards != null) {
			for (int s = 0; s < numStatesProd; s++) {
				List<Value> list = transRewards.get(product.getModelState(s));
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
	public boolean hasStateRewards()
	{
		return stateRewards != null;
	}

	@Override
	public boolean hasTransitionRewards()
	{
		return transRewards != null;
	}
}
