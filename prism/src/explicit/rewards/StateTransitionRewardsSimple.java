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

import java.util.ArrayList;
import java.util.List;

/**
 * Explicit-state storage of state and transition rewards (mutable).
 */
public class StateTransitionRewardsSimple extends StateRewardsSimple
{
	private List<List<Double>> transRewards;
	
	public StateTransitionRewardsSimple(int numStates)
	{
		super(numStates);
		this.transRewards = new ArrayList<List<Double>>();
		for(int i = 0; i < numStates; i++)
		{
			this.transRewards.add(new ArrayList<Double>());
		}
	}
	
	/**
	 * Increase the number of states by {@code numStates}
	 * 
	 * @param numStates Number of newly added states
	 */
	public void addStates(int numStates)
	{
		for(int i = 0; i < numStates; i++)
		{
			this.transRewards.add(new ArrayList<Double>());
		}		
	}
	
	/**
	 * Set the reward of choice {@code c} of state {@code s} to {@code r}.
	 * 
	 * The number of states added so far must be at least {@code s+1}.
	 * 
	 * @param s State
	 * @param c Choice (Transition)
	 * @param r Reward
	 */
	public void setTransitionReward(int s, int c, double r)
	{
		int n = s - transRewards.get(s).size() + 1;
		if (n > 0) {
			for (int j = 0; j < n; j++) {
				transRewards.get(s).add(0.0);
			}
		}
		transRewards.get(s).set(c, r);
	}
	
	@Override
	public double getTransitionReward(int s, int i)
	{
		return transRewards.get(s).get(i);
	}
	
	public String toString()
	{
		return "rews: " + stateRewards + "; rewt: " + transRewards;
	}
}
