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

import explicit.Product;
import prism.Evaluator;

/**
 * Interface implemented by all reward classes.
 */
public interface Rewards<Value>
{
	/**
	 * Get an Evaluator for the reward values stored in this class.
	 * This is needed, for example, to compute sums, check for equality to 0/1, etc.
	 * A default implementation provides an evaluator for the (usual) case when Value is Double.
	 */
	@SuppressWarnings("unchecked")
	default Evaluator<Value> getEvaluator()
	{
		return (Evaluator<Value>) Evaluator.forDouble();
	}

	/**
	 * Returns true if this reward structure has state rewards.
	 * This method can sometimes return true even if there are none,
	 * but a value of false always indicates that none are present.
	 */
	default boolean hasStateRewards()
	{
		// Safe to assume true by default
		return true;
	}

	/**
	 * Returns true if this reward structure has transition rewards.
	 * This method can sometimes return true even if there are none,
	 * but a value of false always indicates that none are present.
	 */
	default boolean hasTransitionRewards()
	{
		// Safe to assume true by default
		return true;
	}

	/**
	 * Get the state reward for state {@code s}.
	 */
	default Value getStateReward(int s)
	{
		// Default implementation assumes all zero rewards
		return getEvaluator().zero();
	}

	/**
	 * Get the transition reward with index {@code i} from state {@code s}.
	 * For a nondeterministic model, this refers to the {@code i}th choice,
	 * for a Markov chain like model, it refers to the {@code i}th transition.
	 */
	default Value getTransitionReward(int s, int i)
	{
		// Default implementation assumes all zero rewards
		return getEvaluator().zero();
	}

	/**
	 * Create a new reward structure that lifts this one such that it is defined over states of a
	 * model that is a product of the one that this reward structure is defined over.
	 */
	Rewards<Value> liftFromModel(Product<?> product);
}
