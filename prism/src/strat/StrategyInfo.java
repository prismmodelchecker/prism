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

import java.util.Objects;

import explicit.DistributionOver;
import prism.Evaluator;
import simulator.RandomNumberGenerator;

/**
 * Super-interface for Strategy and StrategyGenerator interfaces.
 * Constants, enums and methods for basic information about strategies. 
 * This is a generic class where {@code Value} is the type of
 * probabilities when the strategy is randomised.
 */
public interface StrategyInfo<Value>
{
	// Constants / enums
	
	/** Strategy classes in terms of memory used */
	public enum Memory {
		NONE, FINITE, INFINITE;
	};
	
	/** Action returned when choice is undefined by strategy */
	public static final Object UNDEFINED = "?";
	
	/** Possible reasons for a choice being undefined */
	public enum UndefinedReason {
		UNKNOWN, ARBITRARY, UNREACHABLE;
	};
	
	// Methods
	
	/**
	 * Strategy class in terms of memory use
	 */
	public default Memory memory()
	{
		// Default to memoryless; override for other cases
		return Memory.NONE;
	}
	
	/**
	 * Does the strategy use memory?
	 */
	public default boolean hasMemory()
	{
		return memory() != Memory.NONE;
	}
	
	/**
	 * Does the strategy use randomisation for action selection?
	 */
	public default boolean isRandomised()
	{
		// Default to deterministic
		return false;
	}

	/**
	 * Get the probability with which an action is chosen by the strategy,
	 * extracting this information from a decision taken by the strategy,
	 * as returned by getChoiceAction().
	 * @param decision The decision taken by the strategy
	 * @param act The action to check
	 */
	@SuppressWarnings("unchecked")
	public default Value getChoiceActionProbability(Object decision, Object act)
	{
		if (decision instanceof DistributionOver) {
			return ((DistributionOver<Value,Object>) decision).getProbability(act);
		} else {
			return Objects.equals(act, decision) ? getEvaluator().one() : getEvaluator().zero();
		}
	}
	
	/**
	 * Is an action chosen by the strategy,
	 * extracting this information from a decision taken by the strategy,
	 * as returned by getChoiceAction()?
	 * For a randomised strategy: is the action chosen with positive probability?
	 * @param decision The decision taken by the strategy
	 * @param act The action to check
	 */
	@SuppressWarnings("unchecked")
	public default boolean isActionChosen(Object decision, Object act)
	{
		if (decision instanceof DistributionOver) {
			// Randomised strategy: check positive probability
			Value prob = ((DistributionOver<Value,Object>) decision).getProbability(act);
			return getEvaluator().gt(prob, getEvaluator().zero());
		} else {
			// Deterministic strategy: check equality (including nulls)
			return Objects.equals(act, decision);
		}
	}
	
	/**
	 * Sample an action chosen by the strategy,
	 * extracting this information from a decision taken by the strategy,
	 * as returned by getChoiceAction().
	 * For a deterministic strategy, this returns the (unique) chosen action;
	 * for a randomised strategy, an action is sampled according to the strategy's distribution.
	 * Returns {@link #StrategyInfo.UNDEFINED} if undefined.
	 * @param decision The decision taken by the strategy
	 * @param rng Random number generator
	 */
	@SuppressWarnings("unchecked")
	public default Object sampleChoiceAction(Object decision, RandomNumberGenerator rng)
	{
		if (decision == UNDEFINED) {
			return decision;
		}
		if (decision instanceof DistributionOver) {
			// Randomised strategy: sample from distribution
			return ((DistributionOver<Value,Object>) decision).sample(rng);
		} else {
			// Deterministic strategy: return unique action choice
			return decision;
		}
	}
	
	/**
	 * For a strategy with memory, get a description of a given memory value.
	 * By default, this is a just the integer value of the memory as a string,
	 * but some strategies will provide a more meaningful representation.
	 * Returns "?" if memory is not applicable (or unknown).
	 * @param m Memory value to look up
	 */
    public default String getMemoryString(int m)
    {
		return m == -1 ? "?" : Integer.toString(m);
    }
    
	/**
	 * Get an Evaluator for the probability values returned when this strategy is randomised.
	 * By default, this is an evaluator for the (usual) case when Value is Double.
	 */
	@SuppressWarnings("unchecked")
	public default Evaluator<Value> getEvaluator()
	{
		return (Evaluator<Value>) Evaluator.forDouble();
	}	
}
