//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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

package param;

/**
 * Reward structure for parametric model.
 * We only consider rewards assigned to a certain nondeterministic choice,
 * because for the properties we consider, state rewards can be stored
 * as equivalent choice rewards.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
final class ParamRewardStruct {
	/** rewards for all choices */
	Function[] rewards;
	/** function factory the functions used for rewards belong to */
	FunctionFactory factory;
	
	/**
	 * Constructs a new reward structure for with given number of choices.
	 * Initially, all rewards will be zero.
	 * 
	 * @param factory function factory to use
	 * @param numChoices number of different rewards
	 */
	ParamRewardStruct(FunctionFactory factory, int numChoices)
	{
		this.rewards = new Function[numChoices];
		this.factory = factory;
		for (int choice = 0; choice < numChoices; choice++) {
			rewards[choice] = factory.getZero();
		}
	}
	
	/**
	 * Constructs a new reward structure which is the copy of another one.
	 * 
	 * @param other original to construct copy of
	 */
	ParamRewardStruct(ParamRewardStruct other)
	{
		this.rewards = new Function[other.rewards.length];
		for (int choice = 0; choice < other.rewards.length; choice++) {
			this.rewards[choice] = other.rewards[choice];
		}
		this.factory = other.factory;
	}
	
	/**
	 * Instantiate reward structure at a given point.
	 * That is, a reward structure is computed in which the corresponding
	 * values have been inserted for each parameter, and which is thus
	 * nonparametric.
	 * 
	 * @param point instantiate for these parameter values
	 * @return non-parametric reward structure instantiated at given point
	 */
	ParamRewardStruct instantiate(Point point)
	{
		ParamRewardStruct result = new ParamRewardStruct(factory, rewards.length);
		for (int choice = 0; choice < rewards.length; choice++) {
			result.rewards[choice] = factory.fromBigRational(this.rewards[choice].evaluate(point));
		}
		result.factory = this.factory;
		
		return result;
	}
	
	/**
	 * Set reward for given choice.
	 * 
	 * @param choice choice to set reward for
	 * @param reward reward to set
	 */
	void setReward(int choice, Function reward)
	{
		rewards[choice] = reward;
	}
	
	/**
	 * Add reward to given choice.
	 * 
	 * @param choice choice to add reward to
	 * @param reward reward to add to existing reward for choice
	 */
	void addReward(int choice, Function reward)
	{
		rewards[choice] = rewards[choice].add(reward);
	}
	
	/**
	 * Get reward for given choice.
	 * 
	 * @param choice choice to get reward of
	 * @return reward for given choice
	 */
	Function getReward(int choice)
	{
		return rewards[choice];
	}
	
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (int state = 0; state < rewards.length; state++) {
			result.append(state);
			result.append(": ");
			result.append(rewards[state]);
			result.append("\n");
		}
		return result.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ParamRewardStruct)) {
			return false;
		}
		ParamRewardStruct other = (ParamRewardStruct) obj;
		if (!this.factory.equals(other.factory)) {
			return false;
		}
		if (this.rewards.length != other.rewards.length) {
			return false;
		}
		for (int choice = 0; choice < rewards.length; choice++) {
			if (!this.rewards[choice].equals(other.rewards[choice])) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		
		hash = factory.hashCode();
		for (int choice = 0; choice < rewards.length; choice++) {
			hash = rewards[choice].hashCode() + (hash << 6) + (hash << 16) - hash;
		}

		return hash;
	}
}
