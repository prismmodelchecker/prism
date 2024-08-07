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

import java.util.Collections;
import java.util.List;

import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;
import explicit.rewards.Rewards;

/**
 * A decorator that attaches state rewards
 * to the labels of nodes, according to the
 * given reward structure(s).
 */
public class ShowStateRewardsDecorator<Value> implements Decorator
{
	/** List of state reward structures */
	private List<Rewards<Value>> rewards;
	/** Output state rewards even if they are all zero for a state? */
	private boolean showAllZero;

	/** Constructor, single reward structure */
	public ShowStateRewardsDecorator(Rewards<Value> rewards, boolean showZero)
	{
		this(Collections.singletonList(rewards), showZero);
	}

	/** Constructor, multiple reward structures */
	public ShowStateRewardsDecorator(List<Rewards<Value>> rewards, boolean showAllZero)
	{
		this.rewards = rewards;
		this.showAllZero = showAllZero;
	}

	/** Attach state rewards for the given state */
	@Override
	public Decoration decorateState(int state, Decoration d)
	{
		boolean allZero = true;

		for (Rewards<Value> rew : rewards) {
			Value reward = rew.getStateReward(state);
			if (!rew.getEvaluator().isZero(reward)) {
				allZero = false;
				break;
			}
		}
		if (allZero && !showAllZero)
			return d;

		String values = "";
		for (Rewards<Value> rew : rewards) {
			Value reward = rew.getStateReward(state);
			if (!values.isEmpty())
				values += ",";
			values += reward;
		}

		d.labelAddBelow(values);
		return d;
	}
}
