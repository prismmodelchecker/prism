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

/**
 * Explicit-state storage of just state rewards (mutable).
 */
public class StateRewardsSimple extends StateRewards
{
	/** Arraylist of state rewards **/
	protected ArrayList<Double> stateRewards;

	/**
	 * Constructor: all zero rewards.
	 */
	public StateRewardsSimple()
	{
		stateRewards = new ArrayList<Double>();
	}

	/**
	 * Copy constructor
	 * @param rews Rewards to copy
	 */
	public StateRewardsSimple(StateRewardsSimple rews)
	{
		if (rews.stateRewards == null) {
			stateRewards = null;
		} else {
			int n = rews.stateRewards.size();
			stateRewards = new ArrayList<Double>(n);
			for (int i = 0; i < n; i++) {
				stateRewards.add(rews.stateRewards.get(i));
			}
		}
	}

	// Mutators

	/**
	 * Set the reward for state {@code s} to {@code r}.
	 */
	public void setStateReward(int s, double r)
	{
		if (r == 0.0 && s >= stateRewards.size())
			return;
		// If list not big enough, extend
		int n = s - stateRewards.size() + 1;
		if (n > 0) {
			for (int j = 0; j < n; j++) {
				stateRewards.add(0.0);
			}
		}
		// Set reward
		stateRewards.set(s, r);
	}

	// Accessors

	@Override
	public double getStateReward(int s)
	{
		try {
			return stateRewards.get(s);
		} catch (ArrayIndexOutOfBoundsException e) {
			return 0.0;
		}
	}

	// Other

	@Override
	public StateRewardsSimple deepCopy()
	{
		return new StateRewardsSimple(this);
	}
}
