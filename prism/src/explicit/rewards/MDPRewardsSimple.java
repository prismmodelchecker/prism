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

/**
 * Simple explicit-state storage of rewards for an MDP.
 * Like the related class MDPSimple, this is not especially efficient, but mutable (in terms of size).
 */
public class MDPRewardsSimple implements MDPRewards
{
	/** Number of state */
	protected int numStates;
	/** State rewards **/
	protected List<Double> stateRewards;
	/** Transition rewards **/
	protected List<List<Double>> transRewards;
	
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
	
	// Mutators
	
	/**
	 * Set the reward for choice {@code i} of state {@code s} to {@code r}.
	 */
	public void setTransitionReward(int s, int i, double r)
	{
		List<Double> list;
		// If no rewards array created yet, create it
		if (transRewards == null) {
			transRewards = new ArrayList<List<Double>>(numStates);
			for (int j = 0; j < numStates; j++)
				transRewards.add(null);
		}
		// If no rewards for state i yet, create list
		if (transRewards.get(s) == null) {
			list = new ArrayList<Double>();
			transRewards.set(s, list);
		} else {
			list = transRewards.get(s);
		}
		// If list not big enough, extend
		int n = i - list.size() + 1;
		if (n > 0) {
			for (int j = 0; j < n; j++) {
				list.add(0.0);
			}
		}
		// Set reward
		list.set(i, r);
	}

	// Accessors
	
	@Override
	public double getStateReward(int s)
	{
		return stateRewards.get(s);
	}
	
	@Override
	public double getTransitionReward(int s, int i)
	{
		List<Double> list;
		if (transRewards == null || (list = transRewards.get(s)) == null)
			return 0.0;
		if (list.size() <= i)
			return 0.0;
		return list.get(i);
	}
	
	@Override
	public String toString()
	{
		return "st: " + this.stateRewards + "; tr:" + this.transRewards;
	}
}
