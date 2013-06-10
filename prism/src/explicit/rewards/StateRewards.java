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
 * Explicit-state storage of just state rewards.
 */
public abstract class StateRewards implements MCRewards, MDPRewards, STPGRewards
{
	/**
	 * Get the state reward for state {@code s}.
	 */
	public abstract double getStateReward(int s);
	
	@Override
	public double getTransitionReward(int s, int i)
	{
		return 0.0;
	}
	
	@Override
	public double getNestedTransitionReward(int s, int i, int j)
	{
		return 0.0;
	}
	
	@Override
	public MDPRewards buildMDPRewards()
	{
		return deepCopy();
	}
	
	/**
	 * Perform a deep copy.
	 */
	public abstract StateRewards deepCopy();
}
