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
 * Explicit-state storage of just state rewards (as an array).
 */
public class StateRewardsArray implements MCRewards, MDPRewards
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
		for (int i = 0; i < numStates; i++)
			stateRewards[i] = 0.0;
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
	
	@Override
	public double getTransitionReward(int s, int i)
	{
		return 0.0;
	}
}
