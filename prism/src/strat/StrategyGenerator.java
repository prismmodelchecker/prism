//==============================================================================
//	
//	Copyright (c) 2022-
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

package strat;

import parser.State;
import simulator.RandomNumberGenerator;

/**
 * Interface for classes to represent strategies which are queried by State.
 * <br><br>
 * The querying is done interactively: the classes are informed which states are visited,
 * via {@link #initialise(State)} and {@link #update(Object, State)} and the current
 * action to be taken in the latest state is available by calling {@link #getCurrentChoiceAction()}.
 * <br><br>
 * If the strategy has memory, this is kept track of and obtained via {@link #getCurrentMemory()}.
 * The strategy can be reset, in order to query the action to be taken for arbitrary states
 * and arbitrary current memory values, by calling {@link #reset(State, int)}. 
 * <br><br>
 * This is a generic class where {@code Value} should match the accompanying model.
 * This is also needed for probabilities when the strategy is randomised.
 */
public interface StrategyGenerator<Value> extends StrategyInfo<Value>
{
	/**
	 * Initialise the strategy, based on an initial model state.
	 * @param state Initial state of the model
	 */
	public StrategyGenerator<Value> initialise(State state);

	/**
	 * Update the strategy, based on the next step in a model's history.
	 * @param action The action taken in the previous state of the model
	 * @param state The new state of the model
	 */
	public StrategyGenerator<Value> update(Object action, State state);
	
	/**
	 * Reset the strategy to provide action choices for a particular state
	 * and a particular current memory value for the strategy.
	 * @param state The current state of the model
	 * @param memory The current memory for the strategy (-1 if not applicable)
	 */
	public StrategyGenerator<Value> reset(State state, int memory);
	
	/**
	 * Get the action chosen by the strategy in the current state (assuming it is deterministic). 
	 * Returns {@link StrategyInfo#UNDEFINED} if undefined.
	 * For a randomised strategy (and if defined), this method returns
	 * an instance of DistributionOver&lt;Object&gt; instead of Object.
	 */
	public Object getCurrentChoiceAction();
	
	/**
	 * Get the probability with which an action is chosen by the strategy in the current state.
	 */
	public default Value getCurrentChoiceActionProbability(Object act)
	{
		return getChoiceActionProbability(getCurrentChoiceAction(), act);
	}
	
	/**
	 * Is an action chosen by the strategy in the current state?
	 * For a randomised strategy: is the action chosen with positive probability?
	 */
	public default boolean isActionChosenCurrently(Object act)
	{
		return isActionChosen(getCurrentChoiceAction(), act);
	}
	
	/**
	 * Sample an action chosen by the strategy in the current state.
	 * For a deterministic strategy, this returns the (unique) chosen action;
	 * for a randomised strategy, an action is sampled according to the strategy's distribution.
	 * Returns {@link #StrategyInfo.UNDEFINED} if undefined.
	 */
	public default Object sampleCurrentChoiceAction(RandomNumberGenerator rng)
	{
		return sampleChoiceAction(getCurrentChoiceAction(), rng);
	}
	
	/**
	 * Get the current value of the memory for the strategy (-1 if not applicable).
	 */
	public int getCurrentMemory();
	
	/**
	 * Get a description of the current value of the memory for the strategy.
	 * By default, this is a just the integer value of the memory as a string,
	 * but some strategies will provide a more meaningful representation.
	 * Returns "?" if memory is not applicable (or unknown).
	 */
	public default String getCurrentMemoryString()
	{
		return getMemoryString(getCurrentMemory());
	}
}
