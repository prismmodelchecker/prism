//==============================================================================
//	
//	Copyright (c) 2019-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
// 	* Ludwig Pauly <ludwigpauly@gmail.com> (TU Dresden)
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

package prism;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * This class extends ExplicitFilesRewardGenerator for the explicit engine.
 */
public class ExplicitFilesRewardGenerator4Explicit extends ExplicitFilesRewardGenerator
{
	protected double[][] stateRewards; // state reward structures

	public ExplicitFilesRewardGenerator4Explicit(PrismComponent parent, List<File> stateRewardsFiles, int numStates) throws PrismException
	{
		super(parent, stateRewardsFiles, numStates);
		stateRewards = new double[getNumRewardStructs()][];
	}

	public ExplicitFilesRewardGenerator4Explicit(PrismComponent parent, File stateRewardsFiles, int numStates) throws PrismException
	{
		this(parent, Collections.singletonList(stateRewardsFiles), numStates);
	}

	/**
	 * Stores the state rewards in the required format for explicit.
	 *
	 * @param rewardStructIndex reward structure index
	 * @param i state index
	 * @param d reward value
	 */
	protected void storeReward(int rewardStructIndex, int i, double d)
	{
		stateRewards[rewardStructIndex][i] = d;
	}

	/**
	 * Lazily load rewards from file when requested.
	 *
	 * @param r The index of the reward structure to use
	 * @param s The index of the state in which to evaluate the rewards
	 * @return state reward
	 * @throws PrismException if an error occurs during reward extraction
	 */
	@Override
	public Double getStateReward(int r, int s) throws PrismException
	{
		if (stateRewards[r] == null) {
			stateRewards[r] = new double[numStates];
			extractStateRewards(r);
		}
		return stateRewards[r][s];
	}
}
