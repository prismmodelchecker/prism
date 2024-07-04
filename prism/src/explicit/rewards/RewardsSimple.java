//==============================================================================
//
//	Copyright (c) 2024-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

import explicit.ListNestedSimple;
import explicit.ListSimple;
import explicit.Model;
import explicit.Product;

/**
 * Simple explicit-state storage of rewards for a model.
 * Like the related class *Simple classes, this is not especially efficient, but mutable.
 */
public class RewardsSimple<Value> extends RewardsExplicit<Value> implements MDPRewards<Value>, MCRewards<Value>
{
	/** Number of states */
	protected int numStates;
	/** State rewards */
	protected ListSimple<Value> stateRewards;
	/** Transition rewards */
	protected ListNestedSimple<Value> transRewards;

	/**
	 * Constructor: all zero rewards.
	 * @param numStates Number of states
	 */
	public RewardsSimple(int numStates)
	{
		this.numStates = numStates;
		stateRewards = new ListSimple<>(getEvaluator().zero(), getEvaluator()::isZero);
		transRewards = new ListNestedSimple<>(getEvaluator().zero(), getEvaluator()::isZero);
	}

	/**
	 * Copy constructor
	 * @param rews RewardsSimple to copy
	 */
	public RewardsSimple(RewardsSimple<Value> rews)
	{
		setEvaluator(rews.getEvaluator());
		numStates = rews.numStates;
		stateRewards = new ListSimple<>(rews.stateRewards);
		transRewards = new ListNestedSimple<>(rews.transRewards);
	}

	// Mutators

	/**
	 * Set the state reward for state {@code s} to {@code r}.
	 */
	public void setStateReward(int s, Value r)
	{
		stateRewards.setValue(s, r);
	}

	/**
	 * Add {@code r} to the state reward for state {@code s}.
	 */
	public void addToStateReward(int s, Value r)
	{
		setStateReward(s, getEvaluator().add(getStateReward(s), r));
	}

	/**
	 * Set the transition reward with index {@code i} of state {@code s} to {@code r}.
	 */
	public void setTransitionReward(int s, int i, Value r)
	{
		transRewards.setValue(s, i, r);
	}

	/**
	 * Add {@code r} to the transition reward with index {@code i} of state {@code s}.
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
		transRewards.clear(s);
	}

	// Accessors

	@Override
	public boolean hasStateRewards()
	{
		return !stateRewards.allZero();
	}

	@Override
	public boolean hasTransitionRewards()
	{
		return !transRewards.allZero();
	}

	@Override
	public Value getStateReward(int s)
	{
		return stateRewards.getValue(s);
	}

	@Override
	public Value getTransitionReward(int s, int i)
	{
		return transRewards.getValue(s, i);
	}

	@Override
	public RewardsSimple<Value> liftFromModel(Product<?> product)
	{
		Model<?> modelProd = product.getProductModel();
		int numStatesProd = modelProd.getNumStates();
		RewardsSimple<Value> rewardsProd = new RewardsSimple<>(numStatesProd);
		rewardsProd.setEvaluator(getEvaluator());
		if (!stateRewards.allZero()) {
			for (int s = 0; s < numStatesProd; s++) {
				rewardsProd.setStateReward(s, stateRewards.getValue(product.getModelState(s)));
			}
		}
		if (!transRewards.allZero()) {
			for (int s = 0; s < numStatesProd; s++) {
				rewardsProd.transRewards.copyFrom(s, transRewards, product.getModelState(s));
			}
		}
		return rewardsProd;
	}

	@Override
	public String toString()
	{
		return "st: " + this.stateRewards + "; tr:" + this.transRewards;
	}
}
