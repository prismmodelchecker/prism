//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

import java.util.function.Function;

import explicit.Model;
import prism.Evaluator;

/**
 * Explicit-state storage of just state rewards (mutable).
 */
public class StateRewardsSimple<Value> extends RewardsSimple<Value>
{
	/**
	 * Constructor: all zero rewards.
	 */
	public StateRewardsSimple()
	{
		super(0);
	}

	/**
	 * Copy constructor
	 * @param rews Rewards to copy
	 */
	public StateRewardsSimple(StateRewardsSimple<Value> rews)
	{
		super(rews);
	}

	/**
	 * Copy constructor.
	 * @param rews Rewards to copy
	 * @param model Associated model (needed for sizes)
	 */
	public StateRewardsSimple(StateRewardsSimple<Value> rews, Model<?> model)
	{
		this(rews, model, r -> r);
	}

	/**
	 * Copy constructor, mapping reward values using the provided function.
	 * Since the type changes (T -> Value), an Evaluator for Value must be given.
	 * @param rews Rewards to copy
	 * @param model Associated model (needed for sizes)
	 * @param rewMap Reward value map
	 */
	public StateRewardsSimple(StateRewardsSimple<Value> rews, Model<?> model, Function<? super Value, ? extends Value> rewMap)
	{
		this(rews, model, rewMap, rews.getEvaluator());
	}

	/**
	 * Copy constructor, mapping reward values using the provided function.
	 * Since the type changes (T -> Value), an Evaluator for Value must be given.
	 * @param rews Rewards to copy
	 * @param model Associated model (needed for sizes)
	 * @param rewMap Reward value map
	 * @param eval Evaluator for Value
	 */
	public <T> StateRewardsSimple(StateRewardsSimple<T> rews, Model<?> model, Function<? super T, ? extends Value> rewMap, Evaluator<Value> eval)
	{
		super(rews, model, rewMap, eval);
	}

	@Override
	public void setTransitionReward(int s, int i, Value r)
	{
		throw new UnsupportedOperationException("StateRewardsSimple does not support transition rewards");
	}

	@Override
	public boolean hasTransitionRewards()
	{
		// Only state rewards
		return false;
	}
}
