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

public class STPGRewardsSimple extends MDPRewardsSimple implements STPGRewards
{
	/** Nested transition rewards (level 1) */
	protected List<List<Double>> nestedTransRewards1;
	/** Nested transition rewards (level 2) */
	protected List<List<List<Double>>> nestedTransRewards2;

	/**
	 * Constructor: all zero rewards.
	 * @param numStates Number of states
	 */
	public STPGRewardsSimple(int numStates)
	{
		super(numStates);
		// Initially lists are just null (denoting all 0)
		nestedTransRewards1 = null;
		nestedTransRewards2 = null;
	}
	
	// Mutators
	
	/**
	 * Set the reward for the {@code i}th nested transition of state {@code s} to {@code r}.
	 */
	public void setNestedTransitionReward(int s, int i, double r)
	{
		List<Double> list;
		// Nothing to do for zero reward
		if (r == 0.0)
			return;
		// If no rewards array created yet, create it
		if (nestedTransRewards1 == null) {
			nestedTransRewards1 = new ArrayList<List<Double>>(numStates);
			for (int j = 0; j < numStates; j++)
				nestedTransRewards1.add(null);
		}
		// If no rewards for state s yet, create list
		if (nestedTransRewards1.get(s) == null) {
			list = new ArrayList<Double>();
			nestedTransRewards1.set(s, list);
		} else {
			list = nestedTransRewards1.get(s);
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

	/**
	 * Set the reward for the {@code i},{@code j}th nested transition of state {@code s} to {@code r}.
	 */
	public void setNestedTransitionReward(int s, int i, int j, double r)
	{
		List<List<Double>> list1;
		List<Double> list2;
		// Nothing to do for zero reward
		if (r == 0.0)
			return;
		// If no rewards array created yet, create it
		if (nestedTransRewards2 == null) {
			nestedTransRewards2 = new ArrayList<List<List<Double>>>(numStates);
			for (int k = 0; k < numStates; k++)
				nestedTransRewards2.add(null);
		}
		// If no rewards for state s yet, create list1
		if (nestedTransRewards2.get(s) == null) {
			list1 = new ArrayList<List<Double>>();
			nestedTransRewards2.set(s, list1);
		} else {
			list1 = nestedTransRewards2.get(s);
		}
		// If list1 not big enough, extend
		int n1 = i - list1.size() + 1;
		if (n1 > 0) {
			for (int k = 0; k < n1; k++) {
				list1.add(null);
			}
		}
		// If no rewards for state s, choice i, create list2
		if (list1.get(i) == null) {
			list2 = new ArrayList<Double>();
			list1.set(i, list2);
		} else {
			list2 = list1.get(i);
		}
		// If list2 not big enough, extend
		int n2 = j - list2.size() + 1;
		if (n2 > 0) {
			for (int k = 0; k < n2; k++) {
				list2.add(null);
			}
		}
		// Set reward
		list2.set(j, r);
	}

	/**
	 * Clear all rewards for state s.
	 */
	public void clearRewards(int s)
	{
		super.clearRewards(s);
		if (nestedTransRewards1 != null && nestedTransRewards1.size() > s) {
			nestedTransRewards1.set(s, null);
		}
		if (nestedTransRewards2 != null && nestedTransRewards2.size() > s) {
			nestedTransRewards2.set(s, null);
		}
	}
	
	// Accessors
	
	@Override
	public double getNestedTransitionReward(int s, int i)
	{
		List<Double> list;
		if (nestedTransRewards1 == null || (list = nestedTransRewards1.get(s)) == null)
			return 0.0;
		if (list.size() <= i)
			return 0.0;
		return list.get(i);
	}
	
	@Override
	public double getNestedTransitionReward(int s, int i, int j)
	{
		List<List<Double>> list1;
		List<Double> list2;
		if (nestedTransRewards2 == null || (list1 = nestedTransRewards2.get(s)) == null)
			return 0.0;
		if (list1.size() <= i || (list2 = list1.get(i)) == null)
			return 0.0;
		if (list2.size() <= j)
			return 0.0;
		return list2.get(j);
	}
	
	@Override
	public String toString()
	{
		return super.toString() + "; ntr1: " + nestedTransRewards1 + "; ntr2:" + nestedTransRewards2;
	}
}
