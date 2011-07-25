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

public class STPGRewardsSimple implements STPGRewards
{
	/** Number of states */
	protected int numStates;

	protected List<List<Double>> distributionSetRewards;

	protected List<List<List<List<Double>>>> transRewards;

	public STPGRewardsSimple(int numStates)
	{
		this.numStates = numStates;
		// Initially lists are just null (denoting all 0)
		distributionSetRewards = new ArrayList<List<Double>>();
		
		transRewards = new ArrayList<List<List<List<Double>>>>(numStates);
		for (int j = 0; j < numStates; j++)
		{
			transRewards.add(null);
			distributionSetRewards.add(null);
		}
	}

	/**
	 * NOT IMPLEMENTED
	 */
	@Override
	public double getDistributionSetReward(int s, int ds)
	{
		return 0;
	}

	@Override
	public int getTransitionRewardCount(int s, int ds, int d)
	{
		if (transRewards.get(s) == null || transRewards.get(s).get(ds) == null || transRewards.get(s).get(ds).get(d) == null)
			return 0;
		else
			return transRewards.get(s).get(ds).get(d).size();
	}
	
	/**
	 * Adds rewards specified by {@code newRewards} to the rewards associated
	 * with {@code ds}th distribution of state {@code s}. 
	 * 
	 * The rewards are given as a list of lists of doubles, where the
	 * i-th element of {@code newRewards} specifies the rewards to be added
	 * to the (possibly empty) list of rewards associated with
	 * i-th distribution associated with {@code s} and {@code ds}.
	 *  
	 * @param s
	 * @param ds
	 * @param newRewards
	 */
	public void addTransitionRewards(int s, int ds, List<List<Double>> newRewards)
	{	
		if (transRewards.get(s) == null) {	
			List<List<List<Double>>> distTransRewards = new ArrayList<List<List<Double>>>();			
			transRewards.set(s, distTransRewards);
		}
		
		if (transRewards.get(s).size() <= ds) {
			List<List<Double>> lTransRewards = new ArrayList<List<Double>>();			
			transRewards.get(s).add(lTransRewards);
		}
		
		List<List<Double>> dsRewards = transRewards.get(s).get(ds);
		if (dsRewards.size() < newRewards.size())
		{
			for (int i = dsRewards.size(); i < newRewards.size(); i++)
			{
				dsRewards.add(new ArrayList<Double>());
			}
		}
		
		
		for (int i = 0; i < dsRewards.size(); i++)
		{
			dsRewards.get(i).addAll(newRewards.get(i));
		}
	}
	
	
	@Override
	public double getTransitionReward(int s, int ds, int d, int i)
	{
		return this.transRewards.get(s).get(ds).get(d).get(i);
	}
	
	@Override
	public void clearRewards(int s)
	{
		if(this.distributionSetRewards.get(s) != null)
			this.distributionSetRewards.get(s).clear();
		if(this.transRewards.get(s) != null)
			this.transRewards.get(s).clear();
	}
	
	public void addStates(int n)
	{
		this.numStates += n;
		for (int i=0; i<n; i++)
		{
			this.distributionSetRewards.add(null);
			this.transRewards.add(null);
		}
	}

	@Override
	public String toString()
	{
		int i;
		boolean first;
		String s = "";
		first = true;
		s = "[ ";
		for (i = 0; i < numStates; i++) {
			if (first)
				first = false;
			else
				s += ", ";
			s += i + ": " + transRewards.get(i);
		}
		s += " ]";
		return s;
	}
}
