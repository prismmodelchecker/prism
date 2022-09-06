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

import explicit.Model;
import explicit.Product;

/**
 * Explicit-state storage of just state rewards (as an array).
 */
public class StateRewardsArray extends StateRewards
{
	/** Array of state rewards **/
	protected double stateRewards[] = null;
	
	/**
	 * Constructor: all zero rewards.
	 * @param numStates Number of states
	 */
	public StateRewardsArray(int numStates)
	{
		stateRewards = new double[numStates];
		for (int i = 0; i < numStates; i++) {
			stateRewards[i] = 0.0;
		}
	}
	
	/**
	 * Copy constructor
	 * @param rews Rewards to copy
	 */
	public StateRewardsArray(StateRewardsArray rews)
	{
		int numStates= rews.stateRewards.length;
		stateRewards = new double[numStates];
		for (int i = 0; i < numStates; i++) {
			stateRewards[i] = rews.stateRewards[i];
		}
	}

	// Mutators
	
	/**
	 * Set the reward for state {@code s} to {@code r}.
	 */
	public void setStateReward(int s, double r)
	{
		stateRewards[s] = r;
	}
	
	/**
	 * Add {@code r} to the state reward for state {@code s} .
	 */
	public void addToStateReward(int s, double r)
	{
		stateRewards[s] += r;
	}
	
	// Accessors
	
	@Override
	public double getStateReward(int s)
	{
		return stateRewards[s];
	}
	
	// Converters
	
	@Override
	public StateRewards liftFromModel(Product<? extends Model> product)
	{
		Model modelProd = product.getProductModel();
		int numStatesProd = modelProd.getNumStates();
		StateRewardsArray rewardsProd = new StateRewardsArray(numStatesProd);
		for (int s = 0; s < numStatesProd; s++) {
			rewardsProd.setStateReward(s, stateRewards[product.getModelState(s)]);
		}
		return rewardsProd;
	}
	
	// Other

	@Override
	public StateRewardsArray deepCopy()
	{
		return new StateRewardsArray(this);
	}
}
