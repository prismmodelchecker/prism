//==============================================================================
//
//	Copyright (c) 2025-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Simple mutable storage of basic info about rewards of a model.
 * Implements {@link RewardInfo}, but only provides info, not actual rewards.
 */
public class BasicRewardInfo implements RewardInfo
{
	/** Reward names */
	private final List<String> rewardNameList = new ArrayList<>();
	/** Which rewards have state rewards */
	private final BitSet hasStateRewards = new BitSet();
	/** Which rewards have transition rewards */
	private final BitSet hasTransitionRewards = new BitSet();

	/**
	 * Add a reward structure to the list.
	 * Use "" for {@code name} if it is unnamed.
	 * Assumes that it has state/transition rewards for now.
	 */
	public void addReward(String name)
	{
		rewardNameList.add(name);
		hasStateRewards.set(rewardNameList.size() - 1);
		hasTransitionRewards.set(rewardNameList.size() - 1);
	}

	/**
	 * Specify whether reward structure r has state rewards.
	 * @param r Index of reward structure (indexed from 0).
	 * @param b Whether or not it has state rewards
	 */
	public void setHasStateRewards(int r, boolean b)
	{
		hasStateRewards.set(r, b);
	}

	/**
	 * Specify whether reward structure r has transition rewards.
	 * @param r Index of reward structure (indexed from 0).
	 * @param b Whether or not it has transition rewards
	 */
	public void setHasTransitionRewards(int r, boolean b)
	{
		hasTransitionRewards.set(r, b);
	}

	// Methods to implement RewardGenerator

	@Override
	public List<String> getRewardStructNames()
	{
		return rewardNameList;
	}

	@Override
	public int getNumRewardStructs()
	{
		return rewardNameList.size();
	}

	@Override
	public boolean rewardStructHasStateRewards(int r)
	{
		return hasStateRewards.get(r);
	}

	@Override
	public boolean rewardStructHasTransitionRewards(int r)
	{
		return hasTransitionRewards.get(r);
	}
}
