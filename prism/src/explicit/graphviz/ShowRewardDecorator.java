//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package explicit.graphviz;

import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;
import explicit.rewards.Rewards;

/**
 * A decorator that attaches rewards
 * to the labels of nodes / edges, according to the
 * given reward structure.
 */
public class ShowRewardDecorator implements Decorator
{
	/** The reward structure */
	private Rewards rewards;
	/** Flag: should zero rewards be output? */
	private boolean showZero;

	/**
	 * Constructor, suppress zero rewards in output.
	 * @param rewards the reward structure
	 */
	public ShowRewardDecorator(explicit.rewards.Rewards rewards)
	{
		this(rewards, false);

	}

	/**
	 * Constructor.
	 * @param rewards the reward structure
	 * @param showZero should zero rewards be output?
	 */
	public ShowRewardDecorator(explicit.rewards.Rewards rewards, boolean showZero)
	{
		this.rewards = rewards;
		this.showZero = showZero;
	}

	/**
	 * Decorate state node by appending state reward to state label.
	 */
	@Override
	public Decoration decorateState(int state, Decoration d)
	{
		double reward = 0;
		if (rewards instanceof MCRewards) {
			reward = ((MCRewards) rewards).getStateReward(state);
		} else if (rewards instanceof MDPRewards) {
			reward = ((MDPRewards) rewards).getStateReward(state);
		}
		if (reward == 0 && !showZero) {
			return d;
		}

		d.labelAddBelow("+" + reward);
		return d;
	}

	/**
	 * Decorate choice edge by appending transition reward to transition label.
	 */
	@Override
	public Decoration decorateTransition(int state, int choice, Decoration d)
	{
		if (!(rewards instanceof MDPRewards)) {
			// transition rewards are only there for MDPRewards
			return d;
		}
		double reward = ((MDPRewards) rewards).getTransitionReward(state, choice);
		if (reward == 0 && !showZero) {
			return d;
		}

		d.labelAddBelow("+" + reward);
		return d;
	}

}
