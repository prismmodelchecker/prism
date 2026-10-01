//==============================================================================
//
//	Copyright (c) 2026-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import explicit.NondetModel;
import parser.State;
import prism.PrismException;
import strat.StrategyInfo.UndefinedReason;

/**
 * Utility methods for working out which choices of a model are picked by a strategy.
 * <br><br>
 * Strategy decisions are action-based (see {@link StrategyInfo}), so this is done
 * by matching action labels, which assumes that the actions picked identify choices
 * uniquely; cases where they do not (i.e., where a decision is ambiguous) are detected.
 */
public class StrategyChoices
{
	/**
	 * The choices picked by a strategy in a state (see {@link StrategyChoices#getChosenChoices}).
	 */
	public static class Chosen
	{
		/** Indices of the picked choices */
		public final BitSet choices;
		/** How an undefined decision was resolved (null if the decision was defined) */
		public final UndefinedChoicePolicy.Resolution resolution;
		/**
		 * The decision used to pick the choices: the strategy's decision, if defined;
		 * for an undefined decision resolved as FIRST, the action of the first choice;
		 * and null otherwise (other resolutions, or no choices)
		 */
		public final Object decision;

		public Chosen(BitSet choices, UndefinedChoicePolicy.Resolution resolution, Object decision)
		{
			this.choices = choices;
			this.resolution = resolution;
			this.decision = decision;
		}
	}

	/**
	 * Determine which choices in state {@code s} of a model (where the current memory
	 * of the strategy, if applicable, is {@code m}) are picked by a strategy.
	 * Strategy decisions are action-based, so this is done by matching the
	 * action labels of the choices against the strategy's decision
	 * (see {@link StrategyInfo#isActionChosen(Object, Object)}).
	 * <br><br>
	 * If the strategy is undefined in the state, this is resolved according to
	 * {@code policy}, based on the reason (see {@link Strategy#whyUndefined(int, int)}):
	 * STOP and LOOP pick no choices (for the latter, a self-loop should be added); FIRST picks the choice(s) with the same action as the first one
	 * (only the first of these, if there are several); ALL picks all choices;
	 * and ERROR throws an exception.
	 * <br><br>
	 * A (defined) decision is ambiguous if it picks several choices with the same action label,
	 * since it is then unclear which of them is meant: an exception is thrown in this case.
	 * @param model The model
	 * @param strat The strategy
	 * @param s The state
	 * @param m The current strategy memory (-1 if not applicable)
	 * @param policy Policy for resolving undefined choices
	 */
	public static <Value> Chosen getChosenChoices(NondetModel<Value> model, Strategy<Value> strat, int s, int m, UndefinedChoicePolicy policy) throws PrismException
	{
		BitSet chosen = new BitSet();
		int numChoices = model.getNumChoices(s);
		if (numChoices == 0) {
			return new Chosen(chosen, null, null);
		}
		Object decision = strat.getChoiceAction(s, m);
		UndefinedChoicePolicy.Resolution resolution = null;
		if (decision == StrategyInfo.UNDEFINED) {
			UndefinedReason why = strat.whyUndefined(s, m);
			resolution = policy.get(why);
			switch (resolution) {
			case STOP:
			case LOOP:
				return new Chosen(chosen, resolution, null);
			case ALL:
				chosen.set(0, numChoices);
				return new Chosen(chosen, resolution, null);
			case ERROR:
				throw new PrismException("Strategy choice is undefined in state " + stateString(model, strat, s, m) + " (reason: "
						+ (why == null ? UndefinedReason.UNKNOWN : why).toString().toLowerCase() + ")");
			case FIRST:
			default:
				decision = strat.projectAction(model.getAction(s, 0));
				break;
			}
		}
		Set<Object> chosenActions = new HashSet<>();
		for (int j = 0; j < numChoices; j++) {
			Object act = model.getAction(s, j);
			if (!strat.isActionChosen(decision, act)) {
				continue;
			}
			if (!chosenActions.add(act)) {
				if (resolution != null) {
					continue;
				}
				throw new PrismException("Strategy is ambiguous in state " + stateString(model, strat, s, m) + ": action \"" + (act == null ? "" : act)
						+ "\" labels several choices, so it is not clear which is picked (action labels need to be unique in each state)");
			}
			chosen.set(j);
		}
		return new Chosen(chosen, resolution, decision);
	}

	/**
	 * Get a string describing state s (and strategy memory m, if applicable), for error messages
	 */
	private static String stateString(NondetModel<?> model, Strategy<?> strat, int s, int m)
	{
		List<State> statesList = model.getStatesList();
		String stateString = statesList != null ? statesList.get(s).toString() : Integer.toString(s);
		if (m != -1) {
			stateString += " (strategy memory " + strat.getMemoryString(m) + ")";
		}
		return stateString;
	}

	/**
	 * Find the states of a model in which the decisions of a memoryless strategy
	 * are ambiguous, i.e., in which an action picked by the strategy labels several choices
	 * (see {@link #getChosenChoices}). States where the strategy is undefined are ignored.
	 * @param model The model
	 * @param strat The (memoryless) strategy
	 * @return The ambiguous states
	 */
	public static BitSet findAmbiguousStates(NondetModel<?> model, Strategy<?> strat)
	{
		BitSet ambiguous = new BitSet();
		if (model.areAllChoiceActionsUnique()) {
			return ambiguous;
		}
		int numStates = model.getNumStates();
		for (int s = 0; s < numStates; s++) {
			Object decision = strat.getChoiceAction(s, -1);
			if (decision == StrategyInfo.UNDEFINED) {
				continue;
			}
			Set<Object> chosenActions = new HashSet<>();
			int numChoices = model.getNumChoices(s);
			for (int j = 0; j < numChoices; j++) {
				Object act = model.getAction(s, j);
				if (strat.isActionChosen(decision, act) && !chosenActions.add(act)) {
					ambiguous.set(s);
					break;
				}
			}
		}
		return ambiguous;
	}
}
