//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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

public class STPGRewardsSimple<Value> extends MDPRewardsSimple<Value> implements STPGRewards<Value>
{
	/**
	 * Constructor: all zero rewards.
	 * @param numStates Number of states
	 */
	public STPGRewardsSimple(int numStates)
	{
		super(numStates);
	}

	/**
	 * Constructor: copy MDP rewards
	 * @param rews MPD rewards to copy
	 */
	public STPGRewardsSimple(MDPRewardsSimple<Value> rews)
	{
		super(rews);
	}

	// Converters
	
	@Override
	public STPGRewards<Value> liftFromModel(Product<?> product)
	{
		// Same as for MDPRewardsSimple, but more efficient than calling that code and then copying
		Model<?> modelProd = product.getProductModel();
		int numStatesProd = modelProd.getNumStates();
		STPGRewardsSimple<Value> rewardsProd = new STPGRewardsSimple<>(numStatesProd);
		rewardsProd.setEvaluator(getEvaluator());
		if (stateRewards != null) {
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
	
	// Other

	@Override
	public MDPRewards<Value> buildMDPRewards()
	{
		return new MDPRewardsSimple<>(this);
	}
}
