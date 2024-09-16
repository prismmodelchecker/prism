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

import explicit.NondetModel;
import prism.Evaluator;

/**
 * Simple explicit-state storage of rewards for an MDP.
 * Like the related class MDPSimple, this is not especially efficient, but mutable (in terms of size).
 * This is no longer needed - just use {@link RewardsSimple}.
 */
public class MDPRewardsSimple<Value> extends RewardsSimple<Value>
{
	/**
	 * Constructor: all zero rewards.
	 * @param numStates Number of states
	 */
	public MDPRewardsSimple(int numStates)
	{
		super(numStates);
	}

	/**
	 * Copy constructor
	 * @param rews Rewards to copy
	 */
	public MDPRewardsSimple(MDPRewardsSimple<Value> rews)
	{
		super(rews);
	}

	/**
	 * Copy constructor.
	 * @param rews Rewards to copy
	 * @param model Associated model (needed for sizes)
	 */
	public MDPRewardsSimple(MDPRewards<Value> rews, NondetModel<?> model)
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
	public MDPRewardsSimple(MDPRewards<Value> rews, NondetModel<?> model, Function<? super Value, ? extends Value> rewMap)
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
	public <T> MDPRewardsSimple(MDPRewards<T> rews, NondetModel<?> model, Function<? super T, ? extends Value> rewMap, Evaluator<Value> eval)
	{
		super(rews, model, rewMap, eval);
	}
}
