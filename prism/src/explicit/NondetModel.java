//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

package explicit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.function.IntPredicate;

import common.IterableStateSet;
import explicit.graphviz.Decoration;
import explicit.graphviz.Decorator;
import io.ModelExportOptions;
import prism.PrismException;
import prism.PrismLog;
import strat.MDStrategy;

import static prism.PrismSettings.DEFAULT_EXPORT_MODEL_PRECISION;

/**
 * Interface for (abstract) classes that provide (read-only) access to an explicit-state model with nondeterminism.
 */
public interface NondetModel<Value> extends Model<Value>
{
	// Accessors

	@Override
	default List<Object> findActionsUsed()
	{
		// Find unique actions across all choices
		if (onlyNullActionUsed()) {
			return Collections.singletonList(null);
		}
		LinkedHashSet<Object> actions = new LinkedHashSet<>();
		int numStates = getNumStates();
		for (int s = 0; s < numStates; s++) {
			int numChoices = getNumChoices(s);
			for (int i = 0; i < numChoices; i++) {
				actions.add(getAction(s, i));
			}
		}
		return new ArrayList<>(actions);
	}

	/**
	 * Get the number of nondeterministic choices in state s.
	 */
	int getNumChoices(int s);

	/**
	 * Get the maximum number of nondeterministic choices in any state.
	 */
	default int getMaxNumChoices()
	{
		int maxNumChoices = 0;
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			maxNumChoices = Math.max(maxNumChoices, getNumChoices(state));
		}
		return maxNumChoices;
	}

	/**
	 * Get the total number of nondeterministic choices over all states.
	 */
	default int getNumChoices()
	{
		int numChoices = 0;
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			numChoices += getNumChoices(state);
		}
		return numChoices;
	}

	/**
	 * Get the action label for choice {@code i} of state {@code s}.
	 * The action is null for an unlabelled choice.
	 */
	Object getAction(int s, int i);

	/**
	 * Get the index of the action label for choice {@code i} of state {@code s}.
	 * Indices are into the list given by {@link #getActions()},
	 * which includes null if there are unlabelled choices,
	 * so this method should always return a value >= 0.
	 */
	default int getActionIndex(int s, int i)
	{
		return actionIndex(getAction(s, i));
	}

	/**
	 * Get a list of the actions labelling the choices of state {@code s}.
	 */
	default List<Object> getAvailableActions(int s)
	{
		List<Object> actions = new ArrayList<>();
		int numChoices = getNumChoices(s);
		for (int i = 0; i < numChoices; i++) {
			actions.add(getAction(s, i));
		}
		return actions;
	}

	/**
	 * Get the index of the (first) choice in state {@code s} with action label {@code action}.
	 * Action labels (which are {@link Object}s) are tested for equality using {@link Object#equals(Object)}.
	 * Returns -1 if there is no matching action.
	 */
	default int getChoiceByAction(int s, Object action)
	{
		int numChoices = getNumChoices(s);
		for (int i = 0; i < numChoices; i++) {
			Object a = getAction(s, i);
			if (a == null) {
				if (action == null) {
					return i;
				}
			} else if (a.equals(action)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Do all choices in each state have a unique action label?
	 * <br><br>
	 * NB: "true" does not imply that all choices are labelled,
	 * e.g., an a-labelled choice and an unlabelled one _are_ considered unique;
	 * multiple unlabelled choices are _not_ considered unique.
	 */
	default boolean areAllChoiceActionsUnique()
	{
		int numStates = getNumStates();
		HashSet<Object> sActions = new HashSet<>();
		for (int s = 0; s < numStates; s++) {
			int n = getNumChoices(s);
			if (n > 1) {
				sActions.clear();
				for (int i = 0; i < n; i++) {
					if (!sActions.add(getAction(s, i))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Check that the available actions and their ordering
	 * in states {@code s1} and {@code s2} match, and throw an exception if not.
	 */
	default void checkActionsMatchExactly(int s1, int s2) throws PrismException
	{
		int numChoices = getNumChoices(s1);
		if (numChoices != getNumChoices(s2)) {
			throw new PrismException("Differing actions found in states: " + getAvailableActions(s1) + " vs. " + getAvailableActions(s2));
		}
		for (int i = 0; i < numChoices; i++) {
			Object action1 = getAction(s1, i);
			Object action2 = getAction(s2, i);
			if (action1 == null) {
				if (action2 != null) {
					throw new PrismException("Differing actions found in states: " + getAvailableActions(s1) + " vs. " + getAvailableActions(s2));
				}
			} else {
				if (!action1.equals(action2)) {
					throw new PrismException("Differing actions found in states: " + getAvailableActions(s1) + " vs. " + getAvailableActions(s2));
				}
			}
		}
	}

	/**
	 * Check that the *sets* of available actions in states {@code s1} and {@code s2} match,
	 * and throw an exception if not.
	 */
	default void checkActionsMatch(int s1, int s2) throws PrismException
	{
		// Get and sort action strings for s1
		List<String> s1Actions = new ArrayList<>();
		int numChoices = getNumChoices(s1);
		for (int i = 0; i < numChoices; i++) {
			Object action = getAction(s1, i);
			s1Actions.add(action == null ? "" : action.toString());
		}
		Collections.sort(s1Actions);
		// Get and sort action strings for s2
		List<String> s2Actions = new ArrayList<>();
		numChoices = getNumChoices(s2);
		for (int i = 0; i < numChoices; i++) {
			Object action = getAction(s2, i);
			s2Actions.add(action == null ? "" : action.toString());
		}
		Collections.sort(s2Actions);
		// Check match
		if (!(s1Actions.equals(s2Actions))) {
			throw new PrismException("Differing actions found in states: " + s1Actions + " vs. " + s2Actions);
		}
	}

	/**
	 * Get the number of transitions from choice {@code i} of state {@code s}.
	 */
	int getNumTransitions(int s, int i);

	@Override
	default int getNumTransitions(int s)
	{
		// Re-implement this because the method in the superclass (Model)
		// would not count successors duplicated across choices
		int numTransitions = 0;
		int n = getNumChoices(s);
		for (int i = 0; i < n; i++) {
			numTransitions += getNumTransitions(s, i);
		}
		return numTransitions;
	}

	/**
	 * Check if all the successor states from choice {@code i} of state {@code s} are in the set {@code set}.
	 * @param s The state to check
	 * @param i Choice index
	 * @param set The set to test for inclusion
	 */
	default boolean allSuccessorsInSet(int s, int i, BitSet set)
	{
		return allSuccessorsMatch(s, i, set::get);
	}
	
	/**
	 * Check if some successor state from choice {@code i} of state {@code s} is in the set {@code set}.
	 * @param s The state to check
	 * @param i Choice index
	 * @param set The set to test for inclusion
	 */
	default boolean someSuccessorsInSet(int s, int i, BitSet set)
	{
		return someSuccessorsMatch(s, i, set::get);
	}

	/**
	 * Check the successor states from choice {@code i} of state {@code s}:
	 * Return {@code true} iff all successors are contained in {@code u}
	 * (remain in a "safe" region) and at least one is contained in {@code v}
	 * (can reach the region defined by v).
	 * @param s The state to check
	 * @param i Choice index
	 * @param u The BitSet that all successors have to be in
	 * @param v The BitSet that some successors have to be in
	 */
	default boolean successorsSafeAndCanReach(int s, int i, BitSet u, BitSet v)
	{
		return successorsSafeAndCanReach(s, i, u::get, v::get);
	}

	/**
	 * Check if all the successor states from choice {@code i} of state {@code s} match the predicate.
	 * @param s The state to check
	 * @param i Choice index
	 * @param p The predicate
	 */
	default boolean allSuccessorsMatch(int s, int i, IntPredicate p)
	{
		// the code for this method is equivalent to the following stream expression,
		// but kept explicit for performance
		//
		// return getSuccessors(s,i).stream().allMatch(p);

		SuccessorsIterator it = getSuccessors(s,i);
		while (it.hasNext()) {
			int t = it.nextInt();
			if (!p.test(t))
				return false;
		}
		return true;
	}

	/**
	 * Check if some successor state from choice {@code i} of state {@code s} match the predicate.
	 * @param s The state to check
	 * @param i Choice index
	 * @param p The predicate
	 */
	default boolean someSuccessorsMatch(int s, int i, IntPredicate p)
	{
		// the code for this method is equivalent to the following stream expression,
		// but kept explicit for performance
		//
		// return getSuccessors(s,i).stream().anyMatch(p);

		SuccessorsIterator it = getSuccessors(s, i);
		while (it.hasNext()) {
			int t = it.nextInt();
			if (p.test(t))
				return true;
		}
		return false;
	}

	/**
	 * Check the successor states from choice {@code i} of state {@code s}:
	 * Return {@code true} iff all successors match the predicate {@code u}
	 * (remain in a "safe" region) and at least one matches predicate {@code v}
	 * (can reach the region defined by v).
	 * @param s The state to check
	 * @param i Choice index
	 * @param u The first predicate (all successors have to match)
	 * @param v The second predicate (some successors have to match)
	 */
	default boolean successorsSafeAndCanReach(int s, int i, IntPredicate u, IntPredicate v)
	{
		SuccessorsIterator it = getSuccessors(s, i);
		boolean hadTransitionToV = false;
		while (it.hasNext()) {
			int t = it.nextInt();
			if (!u.test(t))
				return false;
			if (!hadTransitionToV) {
				hadTransitionToV = v.test(t);
			}
		}
		return hadTransitionToV;
	}

	/**
	 * Get an iterator over the successor states from choice {@code i} of state {@code s}.
	 * @param s The state
	 * @param i Choice index
	 */
	default Iterator<Integer> getSuccessorsIterator(int s, int i)
	{
		SuccessorsIterator successors = getSuccessors(s, i);
		return successors.distinct();
	}

	/**
	 * Get a SuccessorsIterator for state s and choice i.
	 * @param s The state
	 * @param i Choice index
	 */
	SuccessorsIterator getSuccessors(int s, int i);

	@Override
	default SuccessorsIterator getSuccessors(final int s)
	{
		return SuccessorsIterator.chain(new Iterator<>() {
			private int choice = 0;
			private final int choices = getNumChoices(s);

			@Override
			public boolean hasNext()
			{
				return choice < choices;
			}

			@Override
			public SuccessorsIterator next()
			{
				return getSuccessors(s, choice++);
			}
		});
	}

	/**
	 * Perform a single step of precomputation algorithm Prob0, i.e., for states i in {@code subset},
	 * set bit i of {@code result} iff, for all/some choices,
	 * there is a transition to a state in {@code u}.
	 * Quantification over choices is determined by {@code forall}.
	 * @param subset Only compute for these states
	 * @param u Set of states {@code u}
	 * @param forall For-all or there-exists (true=for-all, false=there-exists)
	 * @param result Store results here
	 */
	default void prob0step(final BitSet subset, final BitSet u, final boolean forall, final BitSet result)
	{
		for (PrimitiveIterator.OfInt it = new IterableStateSet(subset, getNumStates()).iterator(); it.hasNext();) {
			final int s = it.nextInt();
			boolean b1 = forall; // there exists or for all
			for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
				boolean b2 = someSuccessorsInSet(s, choice, u);
				if (forall) {
					if (!b2) {
						b1 = false;
						break;
					}
				} else {
					if (b2) {
						b1 = true;
						break;
					}
				}
			}
			result.set(s, b1);
		}
	}

	/**
	 * Perform a single step of precomputation algorithm Prob1A, i.e., for states i in {@code subset},
	 * set bit i of {@code result} iff, for all choices,
	 * there is a transition to a state in {@code v} and all transitions go to states in {@code u}.
	 * @param subset Only compute for these states
	 * @param u Set of states {@code u}
	 * @param v Set of states {@code v}
	 * @param result Store results here
	 */
	default void prob1Astep(BitSet subset, BitSet u, BitSet v, BitSet result)
	{
		boolean b1;
		for (PrimitiveIterator.OfInt it = new IterableStateSet(subset, getNumStates()).iterator(); it.hasNext();) {
			final int s = it.nextInt();
			b1 = true;
			for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
				if (!(successorsSafeAndCanReach(s, choice, u, v))) {
					b1 = false;
					break;
				}
			}
			result.set(s, b1);
		}
	}

	/**
	 * Perform a single step of precomputation algorithm Prob1E, i.e., for states i in {@code subset},
	 * set bit i of {@code result} iff, for some choice,
	 * there is a transition to a state in {@code v} and all transitions go to states in {@code u}.
	 * Optionally, store optimal (memoryless) strategy info for 1 states.
	 * @param subset Only compute for these states
	 * @param u Set of states {@code u}
	 * @param v Set of states {@code v}
	 * @param result Store results here
	 * @param strat Storage for (memoryless) strategy choice indices (ignored if null)
	 */
	default void prob1Estep(BitSet subset, BitSet u, BitSet v, BitSet result, int[] strat)
	{
		int stratCh = -1;
		boolean b1;
		for (PrimitiveIterator.OfInt it = new IterableStateSet(subset, getNumStates()).iterator(); it.hasNext();) {
			final int s = it.nextInt();
			b1 = false;
			for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
				if (successorsSafeAndCanReach(s, choice, u, v)) {
					b1 = true;
					// If strategy generation is enabled, remember optimal choice
					if (strat != null)
						stratCh = choice;
					break;
				}
			}
			// If strategy generation is enabled, store optimal choice
			// (only if this the first time we add the state to S^yes)
			if (strat != null & b1 & !result.get(s)) {
				strat[s] = stratCh;
			}
			// Store result
			result.set(s, b1);
		}
	}

	/**
	 * Perform a single step of precomputation algorithm Prob1, i.e., for states i in {@code subset},
	 * set bit i of {@code result} iff, for all/some choices,
	 * there is a transition to a state in {@code v} and all transitions go to states in {@code u}.
	 * Quantification over choices is determined by {@code forall}.
	 * @param subset Only compute for these states
	 * @param u Set of states {@code u}
	 * @param v Set of states {@code v}
	 * @param forall For-all or there-exists (true=for-all, false=there-exists)
	 * @param result Store results here
	 */
	default void prob1step(BitSet subset, BitSet u, BitSet v, boolean forall, BitSet result)
	{
		boolean b1, b2;
		for (PrimitiveIterator.OfInt it = new IterableStateSet(subset, getNumStates()).iterator(); it.hasNext();) {
			final int s = it.nextInt();
			b1 = forall; // there exists or for all
			for (int choice = 0, numChoices = getNumChoices(s); choice < numChoices; choice++) {
				b2 = successorsSafeAndCanReach(s, choice, u, v);
				if (forall) {
					if (!b2) {
						b1 = false;
						break;
					}
				} else {
					if (b2) {
						b1 = true;
						break;
					}
				}
			}
			result.set(s, b1);
		}
	}

	/**
	 * Perform a single step of precomputation algorithm Prob1 for a single state/choice,
	 * i.e., return whether there is a transition to a state in {@code v} and all transitions go to states in {@code u}.
	 * @param s State (row) index
	 * @param i Choice index
	 * @param u Set of states {@code u}
	 * @param v Set of states {@code v}
	 */
	default boolean prob1stepSingle(int s, int i, BitSet u, BitSet v)
	{
		return successorsSafeAndCanReach(s, i, u, v);
	}

	/**
	 * Construct a model that is induced by applying strategy {@code strat} to this model.
	 * Note that the "new" model may be just an implicit (read-only) representation. 
	 * @param strat (Memoryless) strategy to use
	 */
	Model<Value> constructInducedModel(MDStrategy<Value> strat);

	/**
	 * Export to a dot file, highlighting states in 'mark' and choices for a (memoryless) strategy.
	 */
	default void exportToDotFileWithStrat(PrismLog out, BitSet mark, int[] strat) throws PrismException
	{
		exportToDotFileWithStrat(out, mark, strat, DEFAULT_EXPORT_MODEL_PRECISION);
	}

	/**
	 * Export to a dot file, highlighting states in 'mark' and choices for a (memoryless) strategy.
	 *
	 * @param precision number of significant digits >= 1
	 */
	default void exportToDotFileWithStrat(PrismLog out, BitSet mark, int[] strat, int precision) throws PrismException
	{
		List<Decorator> decorators = new ArrayList<>();
		if (mark != null) {
			decorators.add(new Decorator()
			{
				@Override
				public Decoration decorateState(int state, Decoration d)
				{
					if (mark.get(state)) {
						d.attributes().put("style", "filled");
						d.attributes().put("fillcolor", "#cccccc");
					}
					return d;
				}
			});
		}
		if (strat != null) {
			decorators.add(new Decorator()
			{
				@Override
				public Decoration decorateTransition(int state, int choice, Decoration d)
				{
					if (strat[state] == choice) {
						d.attributes().put("color", "#ff0000");
						d.attributes().put("fontcolor", "#ff0000");
					}
					return d;
				}
			});
		}
		exportToDotFile(out, new ModelExportOptions().setModelPrecision(precision), decorators);
	}
}