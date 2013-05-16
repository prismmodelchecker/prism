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

final class ParamRewardStruct {
	Function[] rewards;
	FunctionFactory factory;
	
	ParamRewardStruct(FunctionFactory factory, int numChoices)
	{
		this.rewards = new Function[numChoices];
		this.factory = factory;
		for (int choice = 0; choice < numChoices; choice++) {
			rewards[choice] = factory.getZero();
		}
	}
	
	ParamRewardStruct(ParamRewardStruct other)
	{
		this.rewards = new Function[other.rewards.length];
		for (int choice = 0; choice < other.rewards.length; choice++) {
			this.rewards[choice] = other.rewards[choice];
		}
		this.factory = other.factory;
	}
	
	ParamRewardStruct instantiate(Point point)
	{
		ParamRewardStruct result = new ParamRewardStruct(factory, rewards.length);
		for (int choice = 0; choice < rewards.length; choice++) {
			result.rewards[choice] = factory.fromBigRational(this.rewards[choice].evaluate(point));
		}
		result.factory = this.factory;
		
		return result;
	}
	
	void setReward(int choice, Function reward)
	{
		rewards[choice] = reward;
	}
	
	void addReward(int choice, Function reward)
	{
		rewards[choice] = rewards[choice].add(reward);
	}
	
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
