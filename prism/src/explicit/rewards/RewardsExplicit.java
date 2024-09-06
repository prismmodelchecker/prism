//==============================================================================
//	
//	Copyright (c) 2020-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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
 * Base class for mutable reward classes.
 */
public abstract class RewardsExplicit<Value> implements Rewards<Value>, MCRewards<Value>, MDPRewards<Value>, STPGRewards<Value>
{
	/** Evaluator for manipulating reward values stored here (of type {@code Value}) */
	@SuppressWarnings("unchecked")
	protected Evaluator<Value> eval = (Evaluator<Value>) Evaluator.forDouble();

	// Mutators

	/**
	 * Set the {@link Evaluator} object for manipulating reward values
	 */
	public void setEvaluator(Evaluator<Value> eval)
	{
		this.eval = eval;
	}

	/**
	 * Set the state reward for state {@code s} to {@code r}.
	 */
	public void setStateReward(int s, Value r)
	{
		throw new UnsupportedOperationException("Setting of state rewards not supported");
	}

	/**
	 * Add {@code r} to the state reward for state {@code s}.
	 */
	public void addToStateReward(int s, Value r)
	{
		setStateReward(s, getEvaluator().add(getStateReward(s), r));
	}

	/**
	 * Set the transition reward with index {@code i} of state {@code s} to {@code r}.
	 * For a nondeterministic model, this refers to the {@code i}th choice,
	 * for a Markov chain like model, it refers to the {@code i}th transition.
	 */
	public void setTransitionReward(int s, int i, Value r)
	{
		throw new UnsupportedOperationException("Setting of transition rewards not supported");
	}

	/**
	 * Add {@code r} to the transition reward with index {@code i} of state {@code s}.
	 * For a nondeterministic model, this refers to the {@code i}th choice,
	 * for a Markov chain like model, it refers to the {@code i}th transition.
	 */
	public void addToTransitionReward(int s, int i, Value r)
	{
		setTransitionReward(s, i, getEvaluator().add(getTransitionReward(s, i), r));
	}

	// Accessors

	@Override
	public Evaluator<Value> getEvaluator()
	{
		return eval;
	}

	@Override
	public abstract RewardsExplicit<Value> liftFromModel(Product<?> product);
}
