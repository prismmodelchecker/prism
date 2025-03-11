//==============================================================================
//
//	Copyright (c) 2024-
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
import java.util.Collections;
import java.util.List;

/**
 * Interface for classes that provide some basic (syntactic) information about rewards for a model.
 * In particular, this gives the number of reward structures and their names.
 * It is only necessary to implement {@link #getRewardStructNames()};
 * the others have default implementations that use this method as a basis.
 */
public interface RewardInfo
{
	/**
	 * Get a list of the names of the reward structures.
	 * Unnamed reward structures are allowed and should use name "".
	 * Reward names should be distinct (except if equal to "").
	 */
	public default List<String> getRewardStructNames()
	{
		// No reward structures by default
		return Collections.emptyList();
	}

	/**
	 * Get a list of the strings needed to reference the reward structures,
	 * i.e., "r" for named ones and k for unnamed ones.
	 */
	public default List<String> getRewardStructReferences()
	{
		List<String> refs = new ArrayList<>();
		int numRewards = getNumRewardStructs();
		for (int r = 0; r < numRewards; r++) {
			String name = getRewardStructName(r);
			if ("".equals(name) || name == null) {
				refs.add(Integer.toString(r + 1));
			} else {
				refs.add("\"" + name + "\"");
			}
		}
		return refs;
	}

	/**
	 * Get the number of reward structures.
	 */
	public default int getNumRewardStructs()
	{
		// Default implementation just extracts from getRewardStructNames()
		return getRewardStructNames().size();
	}

	/**
	 * Look up the index of a reward structure by name.
	 * (indexed from 0, not from 1 like at the user (property language) level).
	 * Returns -1 if there is no such reward structure.
	 */
	public default int getRewardStructIndex(String name)
	{
		// Default implementation just extracts from getRewardStructNames()
		return getRewardStructNames().indexOf(name);
	}

	/**
	 * Get the name of the {@code i}th reward structure.
	 * {@code i} should always be between 0 and getNumVars() - 1.
	 */
	public default String getRewardStructName(int i)
	{
		// Default implementation just extracts from getRewardStructNames()
		return getRewardStructNames().get(i);
	}

	/**
	 * Returns true if the {@code r}th reward structure defines state rewards.
	 * ({@code r} is indexed from 0, not from 1 like at the user (property language) level).
	 * If this returns false, the model checker is allowed to ignore them (which may be more efficient).
	 * If using an algorithm or implementation that does not support state rewards,
	 * you may need to return false here (as well as not defining state rewards).
	 */
	public default boolean rewardStructHasStateRewards(int r)
	{
		// By default, assume that any reward structures that do exist may have state rewards
		return true;
	}

	/**
	 * Returns true if the {@code r}th reward structure defines transition rewards.
	 * ({@code r} is indexed from 0, not from 1 like at the user (property language) level).
	 * If this returns false, the model checker is allowed to ignore them (which may be more efficient).
	 * If using an algorithm or implementation that does not support transition rewards,
	 * you may need to return false here (as well as not defining transition rewards).
	 */
	public default boolean rewardStructHasTransitionRewards(int r)
	{
		// By default, assume that any reward structures that do exist may have transition rewards
		return true;
	}
}
