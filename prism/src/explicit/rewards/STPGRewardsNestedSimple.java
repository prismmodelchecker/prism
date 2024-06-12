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

import explicit.Model;
import explicit.Product;

/**
 * Variant of STPGRewards storage with support for nested transitions,
 * as found in the STPGAbstrSimple model class. Not currently used.
 */
public class STPGRewardsNestedSimple<Value> extends MDPRewardsSimple<Value> implements STPGRewards<Value>
{
	/** Nested transition rewards */
	protected List<List<List<Value>>> nestedTransRewards;

	/**
	 * Constructor: all zero rewards.
	 * @param numStates Number of states
	 */
	public STPGRewardsNestedSimple(int numStates)
	{
		super(numStates);
		// Initially list is just null (denoting all 0)
		nestedTransRewards = null;
	}

	/**
	 * Constructor: copy MDP rewards, other rewards zero
	 * @param rews MPD rewards to copy
	 */
	public STPGRewardsNestedSimple(MDPRewardsSimple<Value> rews)
	{
		super(rews);
		// Initially list is just null (denoting all 0)
		nestedTransRewards = null;
	}

	// Mutators

	/**
	 * Set the reward for the {@code i},{@code j}th nested transition of state {@code s} to {@code r}.
	 */
	public void setNestedTransitionReward(int s, int i, int j, Value r)
	{
		List<List<Value>> list1;
		List<Value> list2;
		// Nothing to do for zero reward
		if (getEvaluator().isZero(r))
			return;
		// If no rewards array created yet, create it
		if (nestedTransRewards == null) {
			nestedTransRewards = new ArrayList<List<List<Value>>>(numStates);
			for (int k = 0; k < numStates; k++)
				nestedTransRewards.add(null);
		}
		// If no rewards for state s yet, create list1
		if (nestedTransRewards.get(s) == null) {
			list1 = new ArrayList<List<Value>>();
			nestedTransRewards.set(s, list1);
		} else {
			list1 = nestedTransRewards.get(s);
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
			list2 = new ArrayList<Value>();
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
		if (nestedTransRewards != null && nestedTransRewards.size() > s) {
			nestedTransRewards.set(s, null);
		}
	}

	// Accessors

	public Value getNestedTransitionReward(int s, int i, int j)
	{
		List<List<Value>> list1;
		List<Value> list2;
		if (nestedTransRewards == null || (list1 = nestedTransRewards.get(s)) == null)
			return getEvaluator().zero();
		if (list1.size() <= i || (list2 = list1.get(i)) == null)
			return getEvaluator().zero();
		if (list2.size() <= j)
			return getEvaluator().zero();
		return list2.get(j);
	}

	// Converters

	@Override
	public STPGRewards<Value> liftFromModel(Product<?> product)
	{
		// Lift MDP part
		MDPRewardsSimple<Value> rewardsProdMDP = (MDPRewardsSimple<Value>) ((MDPRewardsSimple<Value>) this).liftFromModel(product);
		STPGRewardsNestedSimple<Value> rewardsProd = new STPGRewardsNestedSimple<>(rewardsProdMDP);
		// Lift nested transition rewards
		Model<?> modelProd = product.getProductModel();
		int numStatesProd = modelProd.getNumStates();
		if (nestedTransRewards != null) {
			for (int s = 0; s < numStatesProd; s++) {
				List<List<Value>> list1 = nestedTransRewards.get(product.getModelState(s));
				if (list1 != null) {
					int n1 = list1.size();
					for (int i = 0; i < n1; i++) {
						List<Value> list2 = list1.get(i);
						int n2 = list2.size();
						for (int j = 0; j < n2; j++) {
							rewardsProd.setNestedTransitionReward(s, i, j, list2.get(j));
						}
					}
				}
			}
		}
		return rewardsProd;
	}

	// Other

	@Override
	public MDPRewards<Value> buildMDPRewards()
	{
		return new MDPRewardsSimple<>(this);
	}

	@Override
	public String toString()
	{
		return super.toString() + "; ntr:" + nestedTransRewards;
	}
}
