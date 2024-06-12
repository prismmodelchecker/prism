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
 * Explicit-state storage of just state rewards.
 */
public abstract class StateRewards<Value> extends RewardsExplicit<Value> implements MCRewards<Value>, MDPRewards<Value>, STPGRewards<Value>
{
	/**
	 * Get the state reward for state {@code s}.
	 */
	public abstract Value getStateReward(int s);
	
	@Override
	public Value getTransitionReward(int s, int i)
	{
		return getEvaluator().zero();
	}
	
	@Override
	public MDPRewards<Value> buildMDPRewards()
	{
		return deepCopy();
	}
	
	@Override
	public abstract StateRewards<Value> liftFromModel(Product<?> product);
	
	/**
	 * Perform a deep copy.
	 */
	public abstract StateRewards<Value> deepCopy();

	@Override
	public boolean hasTransitionRewards()
	{
		// only state rewards
		return false;
	}
}
