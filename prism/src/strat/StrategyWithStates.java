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

import java.util.function.Function;

import parser.State;
import prism.PrismException;

/**
 * Base class for implementations of Strategy
 * with additional support to query by State (i.e., StrategyGenerator)
 */
public abstract class StrategyWithStates<Value> implements Strategy<Value>, StrategyGenerator<Value>
{
	// State look-up functionality
	
	/** State lookup function; should return -1 for "not found" */
	protected Function<State,Integer> stateLookUp = null;
	
	/**
	 * Set the state look-up mechanism
	 */
	public void setStateLookUp(Function<State,Integer> stateLookUp)
	{
		this.stateLookUp = stateLookUp;
	}
	
	/**
	 * Get the index of a state, provided as a {@code State} object.
	 * Returns -1 if the state is not known.
	 * Throws an exception if there is no way to look this up.
	 */
	public int getStateIndex(State state) throws PrismException
	{
		if (stateLookUp == null) {
			throw new PrismException("Strategy does not support state look-up");
		}
		return stateLookUp.apply(state);
	}
	
	// Implement StrategyGenerator interface via Strategy methods and state look-up
	
	protected State currentState = null;
	protected int currentStateIndex = -1;
	protected int currentMemory = -1;
	
	@Override
	public StrategyGenerator<Value> initialise(State state)
	{
		currentState = state;
		currentStateIndex = stateLookUp.apply(state);
		currentMemory = getInitialMemory(currentStateIndex);
		return this;
	}

	@Override
	public StrategyGenerator<Value> update(Object action, State state)
	{
		currentState = state;
		currentStateIndex = stateLookUp.apply(state);
		currentMemory = getUpdatedMemory(currentMemory, null, currentStateIndex);
		return this;
	}
	
	@Override
	public StrategyGenerator<Value> reset(State state, int memory)
	{
		currentState = state;
		currentStateIndex = stateLookUp.apply(state);
		currentMemory = memory;
		return this;
	}
	
	@Override
	public Object getCurrentChoiceAction()
	{
		return getChoiceAction(currentStateIndex, currentMemory);
	}
	
	@Override
	public int getCurrentMemory()
	{
		return currentMemory;
	}
}
