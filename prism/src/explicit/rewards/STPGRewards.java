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

/**
 * Class that provide access to explicit-state rewards for an abstraction STPG.
 * 
 * There are two type of rewards. Ones are on distribution sets and correspond
 * to state rewards in the MDP (SUPPORT FOR THESE IS CURRENTLY NOT IMPLEMENTED).
 * The others are on Distributions and correspond to transition rewards. Because
 * each of the distributions may abstract several concrete distributions, it can
 * have multiple rewards. The number of different rewards for each distribution
 * can be obtained using {@link #getTransitionRewardCount(int, int, int)}, 
 * the rewards itself are then obtained using {@link #getTransitionReward(int, int, int, int)} 
 * 
 * 
 * 
 */
public interface STPGRewards extends Rewards
{
	/**
	 * Returns the reward associated with {@code ds}th distribution for the state {@code s}.
	 */
	public double getDistributionSetReward(int s, int ds);

	/**
	 * Removes all rewards for DistributionSets and Distributions associated with state {@code s}.
	 */
	public void clearRewards(int s);
	
	/**
	 * Returns the number of different rewards associated with {@code d}th distribution of
	 * {@code ds}th distribution set of state {@code s}
	 * 
	 * @param s State
	 * @param ds Distribution set
	 * @param d Distribution
	 * @return Number of different rewards associated with the distribution
	 */
	public int getTransitionRewardCount(int s, int ds, int d);
	
	/**
	 * 
	 * Returns {@code i}th reward of {@code d}th distribution of
	 * {@code ds}th distribution set of state {@code s}
	 * 
	 * @param s State
	 * @param ds Distribution set
	 * @param d Distribution
	 * @param i Index of the reward to return
	 * @return The reward.
	 */
	public double getTransitionReward(int s, int ds, int d, int i); 
}
