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
import explicit.NondetModel;
import explicit.Product;
import prism.Evaluator;

import java.util.function.Function;

/**
 * Simple explicit-state storage of rewards for a model.
 * Like the related class *Simple classes, this is not especially efficient, but mutable.
 */
public class RewardsSimple<Value> extends RewardsExplicit<Value>
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
		numStates = rews.numStates;
		stateRewards = new ListSimple<>(rews.stateRewards);
		transRewards = new ListNestedSimple<>(rews.transRewards);
		setEvaluator(rews.getEvaluator());
	}

	/**
	 * Copy constructor, mapping reward values using the provided function.
	 * Since the type changes (T -> Value), an Evaluator for Value must be given.
	 * @param rews Rewards to copy
	 * @param model Associated model (needed for sizes)
	 * @param rewMap Reward value map
	 * @param eval Evaluator for Value
	 */
	public <T> RewardsSimple(Rewards<T> rews, Model<?> model, Function<? super T, ? extends Value> rewMap, Evaluator<Value> eval)
	{
		numStates = model.getNumStates();
		stateRewards = new ListSimple<>(eval.zero(), eval::isZero);
		if (rews.hasStateRewards()) {
			for (int s = numStates - 1; s >= 0; s--) {
				stateRewards.setValue(s, rewMap.apply(rews.getStateReward(s)));
			}
		}
		transRewards = new ListNestedSimple<>(eval.zero(), eval::isZero);
		if (rews.hasTransitionRewards()) {
			for (int s = numStates - 1; s >= 0; s--) {
				int n;
				if (model.getModelType().nondeterministic()) {
					n = ((NondetModel<?>) model).getNumChoices(s);
				} else {
					n = model.getNumTransitions(s);
				}
				for (int j = n; j >= 0; j--) {
					transRewards.setValue(s, j, rewMap.apply(rews.getTransitionReward(s, j)));
				}
			}
		}
		setEvaluator(eval);
	}

	// Mutators

	@Override
	public void setEvaluator(Evaluator<Value> eval)
	{
		super.setEvaluator(eval);
		stateRewards.setZero(getEvaluator().zero(), getEvaluator()::isZero);
		transRewards.setZero(getEvaluator().zero(), getEvaluator()::isZero);
	}

	@Override
	public void setStateReward(int s, Value r)
	{
		stateRewards.setValue(s, r);
	}

	@Override
	public void setTransitionReward(int s, int i, Value r)
	{
		transRewards.setValue(s, i, r);
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
		return "st: " + this.stateRewards + "; tr: " + this.transRewards;
	}
}
