//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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


/**
 * Explicit-state representation of a DTMC rewards structure, constructed (implicitly)
 * from an MDP rewards structure and a memoryless deterministic strategy, specified as an array of integer indices.
 * This class is read-only: most of data is pointers to other model info.
 */
public class MCRewardsFromMDPRewards implements MCRewards
{
	// MDP rewards
	protected MDPRewards mdpRewards;
	// Strategy (array of choice indices; -1 denotes no choice)
	protected int strat[];

	/**
	 * Constructor: create from MDP rewards and memoryless adversary.
	 */
	public MCRewardsFromMDPRewards(MDPRewards mdpRewards, int strat[])
	{
		this.mdpRewards = mdpRewards;
		this.strat = strat;
	}

	@Override
	public double getStateReward(int s)
	{
		// For now, state/transition rewards from MDP are both put into state reward
		// This works fine for cumulative rewards, but not instantaneous ones
		return mdpRewards.getStateReward(s) + mdpRewards.getTransitionReward(s, strat[s]);
	}
}
