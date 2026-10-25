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
import java.util.Set;
import java.util.stream.Collectors;

import explicit.DistributionOver;
import prism.Evaluator;
import prism.ModelType;
import prism.PrismNotSupportedException;
import simulator.RandomNumberGenerator;
import strat.StrategyExportOptions.InducedModelMode;

/**
 * Super-interface for Strategy and StrategyGenerator interfaces.
 * Constants, enums and methods for basic information about strategies. 
 * This is a generic class where {@code Value} is the type of
 * probabilities when the strategy is randomised.
 * <br><br>
 * Strategy decisions are action-based: a decision is an action label
 * (or, for a randomised strategy, a distribution over action labels),
 * and the choices of a model that a decision picks are those whose
 * action labels match it (see {@link #isActionChosen(Object, Object)}).
 * This allows strategies to be applied to representations of a model
 * other than the one from which they were generated (e.g., in the simulator),
 * where choices may be ordered differently. It assumes that, in each state
 * where a decision is made, the actions picked identify choices uniquely,
 * i.e., that no two (different) choices in the state have the same action label.
 * Where this is not the case, the decision is ambiguous and should be reported
 * as such by code that applies the strategy, rather than resolved arbitrarily.
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
	
	/**
	 * Possible reasons for a choice being undefined by a strategy.
	 * When strategy choices are stored as (non-negative) choice indices in an array,
	 * these are represented by the negative integers given by {@link #getCode()}.
	 */
	public enum UndefinedReason {
		/** No information is available about the choice (in a state where it matters, this indicates an issue) */
		UNKNOWN(-1),
		/** Any choice is fine: all are equally good (but the objective is not yet decided) */
		ARBITRARY(-2),
		/** The state (and memory, if applicable) cannot be reached under the strategy */
		UNREACHABLE(-3),
		/** The objective has been satisfied (e.g., a target has been reached), so later choices do not matter */
		SATISFIED(-4),
		/** The objective can no longer be satisfied, so later choices do not matter */
		UNSATISFIABLE(-5),
		/** The step bound of the objective has been passed, so later choices do not matter */
		HORIZON(-6);

		private final int code;

		private UndefinedReason(int code)
		{
			this.code = code;
		}

		/**
		 * Get the (negative) integer used to represent this reason
		 * when strategy choice indices are stored in an array.
		 */
		public int getCode()
		{
			return code;
		}

		/**
		 * Get the reason represented by a strategy choice index stored in an array,
		 * if it is negative (UNKNOWN if not recognised), or null if it is non-negative.
		 */
		public static UndefinedReason fromCode(int choice)
		{
			if (choice >= 0) {
				return null;
			}
			for (UndefinedReason reason : values()) {
				if (reason.code == choice) {
					return reason;
				}
			}
			return UNKNOWN;
		}
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
	 * Returns {@link StrategyInfo#UNDEFINED} if undefined.
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
	 * Project an action of the model to which this strategy is applied onto the part of it
	 * that is decided by this strategy. By default, this is the identity, but it can be overridden,
	 * e.g., for a strategy that only decides the actions of some players in a concurrent game,
	 * where actions are joint actions.
	 * <br><br>
	 * This is used when the strategy is undefined in a state but the first choice is to be used
	 * (see {@link UndefinedChoicePolicy.Resolution#FIRST}): the projected action of the first choice
	 * is then treated as the strategy's decision, leaving any parts of the action that are not
	 * decided by the strategy (e.g., other players' actions) undecided.
	 * @param act An action of the model
	 */
	public default Object projectAction(Object act)
	{
		return act;
	}

	/**
	 * Get the action label for a choice in the model induced by this strategy,
	 * given the action of a choice in the original model and the decision taken by
	 * this strategy (which picks that choice, with at least some probability).
	 * When building an induced model that remains nondeterministic,
	 * the choices picked by a decision in a state are grouped by this label:
	 * picked choices with the same label are merged into one choice of the induced model.
	 * <br><br>
	 * For a deterministic strategy, the label is unchanged.
	 * For a randomised strategy, the label combines the actions in the support
	 * of the decision, in the order of the model's choices, e.g., "a_b"
	 * (a decision whose support is a single action keeps that action).
	 * Strategies that only decide part of an action (e.g., for some players of a game)
	 * can override this, e.g., to give one induced choice per action of the remaining players.
	 * @param decision The decision taken by the strategy
	 * @param act The action of the choice in the original model
	 * @param mode Mode of induced model construction ("restrict" or "reduce")
	 */
	public default Object getInducedChoiceAction(Object decision, Object act, InducedModelMode mode)
	{
		if (decision == UNDEFINED) {
			return null;
		}
		if (decision instanceof DistributionOver) {
			return combineActionSupport(((DistributionOver<?, ?>) decision).getSupport());
		} else {
			return decision;
		}
	}

	/**
	 * Convert the support of a randomised decision (a set of actions) into a single action:
	 * either the action itself, for a singleton support,
	 * or the actions converted to strings, joined with "_", e.g., "a_b".
	 */
	public static Object combineActionSupport(Set<?> actionSupport)
	{
		if (actionSupport.size() == 1) {
			return actionSupport.iterator().next();
		} else {
			return actionSupport.stream().map(a -> Objects.toString(a, "")).collect(Collectors.joining("_"));
		}
	}

	/**
	 * Get the action label for a choice in the model induced by this strategy.
	 * @deprecated Use {@link #getInducedChoiceAction(Object, Object, InducedModelMode)}.
	 */
	@Deprecated
	public default Object getInducedAction(Object decision, Object act)
	{
		return getInducedChoiceAction(decision, act, InducedModelMode.RESTRICT);
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
	 * Get the type of model induced by this strategy when applied to a type of model.
	 * Returns null if the model type cannot be deduced.
	 * @param modelType The type of model to which the strategy will be applied
	 * @param numPlayers The number of players in the model (if relevant)
	 * @param mode Mode of induced model construction ("restrict" or "reduce")
	 */
	public default ModelType getInducedModelType(ModelType modelType, int numPlayers, InducedModelMode mode)
	{
		ModelType inducedModelType = null;
		if (mode == InducedModelMode.REDUCE) {
			switch (modelType) {
				case MDP:
				case POMDP:
				case STPG:
					inducedModelType = ModelType.DTMC;
					break;
				case IMDP:
					inducedModelType = ModelType.IDTMC;
					break;
			}
		} else {
			inducedModelType = modelType;
		}
		return inducedModelType;
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
