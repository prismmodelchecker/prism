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

/**
 * Explicit storage of constant game rewards.
 */
public class STPGRewardsConstant implements STPGRewards
{
	private double dsReward;
	private double transReward;
	
	public STPGRewardsConstant(double dsReward, double transReward)
	{
		this.dsReward = dsReward;
		this.transReward = transReward;
	}

	@Override
	public double getDistributionSetReward(int s, int d)
	{
		return this.dsReward;
	}

	@Override
	public int getTransitionRewardCount(int s, int ds, int d)
	{
		return 1;
	}

	@Override
	public double getTransitionReward(int s, int d, int t, int i)
	{
		return this.transReward;
	}
	
	@Override
	public void clearRewards(int s)
	{
		//do nothing
		return;
	}

}
