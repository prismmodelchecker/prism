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
	 * Determine which choices in state {@code s} of a model (where the current memory
	 * of the strategy, if applicable, is {@code m}) are picked by a strategy.
	 * Strategy decisions are action-based, so this is done by matching the
	 * action labels of the choices against the strategy's decision
	 * (see {@link StrategyInfo#isActionChosen(Object, Object)}).
	 * If the strategy is undefined in the state, any choice is fine,
	 * so the choice(s) with the same action as the first one are used.
	 * <br><br>
	 * A decision is ambiguous if it picks several choices with the same action label,
	 * since it is then unclear which of them is meant. For a (defined) decision,
	 * an exception is thrown in this case; for an undefined one, only the first such
	 * choice is used.
	 * @param model The model
	 * @param strat The strategy
	 * @param s The state
	 * @param m The current strategy memory (-1 if not applicable)
	 * @return The indices of the picked choices
	 */
	public static <Value> BitSet getChosenChoices(NondetModel<Value> model, Strategy<Value> strat, int s, int m) throws PrismException
	{
		BitSet chosen = new BitSet();
		int numChoices = model.getNumChoices(s);
		if (numChoices == 0) {
			return chosen;
		}
		Object decision = strat.getChoiceAction(s, m);
		boolean undefined = decision == StrategyInfo.UNDEFINED;
		if (undefined) {
			decision = model.getAction(s, 0);
		}
		Set<Object> chosenActions = new HashSet<>();
		for (int j = 0; j < numChoices; j++) {
			Object act = model.getAction(s, j);
			if (!strat.isActionChosen(decision, act)) {
				continue;
			}
			if (!chosenActions.add(act)) {
				if (undefined) {
					continue;
				}
				List<State> statesList = model.getStatesList();
				String stateString = statesList != null ? statesList.get(s).toString() : Integer.toString(s);
				if (m != -1) {
					stateString += " (strategy memory " + strat.getMemoryString(m) + ")";
				}
				throw new PrismException("Strategy is ambiguous in state " + stateString + ": action \"" + (act == null ? "" : act)
						+ "\" labels several choices, so it is not clear which is picked (action labels need to be unique in each state)");
			}
			chosen.set(j);
		}
		return chosen;
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
