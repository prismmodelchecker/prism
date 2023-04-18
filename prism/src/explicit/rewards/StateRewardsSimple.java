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

import explicit.Model;
import explicit.Product;

/**
 * Explicit-state storage of just state rewards (mutable).
 */
public class StateRewardsSimple<Value> extends StateRewards<Value>
{
	/** State rewards **/
	protected ArrayList<Value> stateRewards;

	/**
	 * Constructor: all zero rewards.
	 */
	public StateRewardsSimple()
	{
		// Empty list (denoting all 0)
		stateRewards = new ArrayList<Value>();
	}

	/**
	 * Copy constructor
	 * @param rews Rewards to copy
	 */
	public StateRewardsSimple(StateRewardsSimple<Value> rews)
	{
		setEvaluator(rews.getEvaluator());
		if (rews.stateRewards == null) {
			stateRewards = null;
		} else {
			stateRewards = new ArrayList<>(rews.stateRewards);
		}
	}

	// Mutators

	/**
	 * Set the reward for state {@code s} to {@code r}.
	 */
	public void setStateReward(int s, Value r)
	{
		if (getEvaluator().isZero(r) && s >= stateRewards.size()) {
			return;
		}
		// If list not big enough, extend
		int n = s - stateRewards.size() + 1;
		if (n > 0) {
			for (int j = 0; j < n; j++) {
				stateRewards.add(getEvaluator().zero());
			}
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

	// Accessors

	@Override
	public Value getStateReward(int s)
	{
		try {
			return stateRewards.get(s);
		} catch (IndexOutOfBoundsException e) {
			return getEvaluator().zero();
		}
	}

	// Converters
	
	@Override
	public StateRewards<Value> liftFromModel(Product<?> product)
	{
		Model<?> modelProd = product.getProductModel();
		int numStatesProd = modelProd.getNumStates();
		StateRewardsSimple<Value> rewardsProd = new StateRewardsSimple<>();
		rewardsProd.setEvaluator(getEvaluator());
		for (int s = 0; s < numStatesProd; s++) {
			rewardsProd.setStateReward(s, getStateReward(product.getModelState(s)));
		}
		return rewardsProd;
	}
	
	// Other

	@Override
	public StateRewardsSimple<Value> deepCopy()
	{
		return new StateRewardsSimple<>(this);
	}
}
