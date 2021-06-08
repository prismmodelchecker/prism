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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntPredicate;

import prism.PrismLog;
import strat.MDStrategy;

/**
 * Interface for (abstract) classes that provide (read-only) access to an explicit-state model with nondeterminism.
 */
public interface NondetModel extends Model
{
	// Accessors

	/**
	 * Get the number of nondeterministic choices in state s.
	 */
	public int getNumChoices(int s);

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
	 * Get the action label (if any) for choice {@code i} of state {@code s}.
	 */
	public Object getAction(int s, int i);

	/**
	 * Get a list of the actions labelling the choices of state {@code s}.
	 */
	public default List<Object> getAvailableActions(int s)
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
	 * Action labels (which are {@link Object}s) are tested for equality using {@link Object#equals()}.
	 * Returns -1 if there is no matching action.
	 */
	public default int getChoiceByAction(int s, Object action)
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
	 * Do all choices in in each state have a unique action label?
	 * <br><br>
	 * NB: "true" does not imply that all choices are labelled,
	 * e.g., an a-labelled choice and an unlabelled one _are_ considered unique;
	 * multiple unlabelled choices are _not_ considered unique.
	 */
	public default boolean areAllChoiceActionsUnique()
	{
		int numStates = getNumStates();
		HashSet<Object> sActions = new HashSet<Object>();
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
	 * Get the number of transitions from choice {@code i} of state {@code s}.
	 */
	public int getNumTransitions(int s, int i);

	@Override
	public default int getNumTransitions(int s)
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
	public default boolean allSuccessorsInSet(int s, int i, BitSet set)
	{
		return allSuccessorsMatch(s, i, set::get);
	}
	
	/**
	 * Check if some successor state from choice {@code i} of state {@code s} is in the set {@code set}.
	 * @param s The state to check
	 * @param i Choice index
	 * @param set The set to test for inclusion
	 */
	public default boolean someSuccessorsInSet(int s, int i, BitSet set)
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
	public default boolean successorsSafeAndCanReach(int s, int i, BitSet u, BitSet v)
	{
		return successorsSafeAndCanReach(s, i, u::get, v::get);
	}

	/**
	 * Check if all the successor states from choice {@code i} of state {@code s} match the predicate.
	 * @param s The state to check
	 * @param i Choice index
	 * @param p The predicate
	 */
	public default boolean allSuccessorsMatch(int s, int i, IntPredicate p)
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
	public default boolean someSuccessorsMatch(int s, int i, IntPredicate p)
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
	public default boolean successorsSafeAndCanReach(int s, int i, IntPredicate u, IntPredicate v)
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
	public default Iterator<Integer> getSuccessorsIterator(int s, int i)
	{
		SuccessorsIterator successors = getSuccessors(s, i);
		return successors.distinct();
	}

	/**
	 * Get a SuccessorsIterator for state s and choice i.
	 * @param s The state
	 * @param i Choice index
	 */
	public SuccessorsIterator getSuccessors(int s, int i);

	@Override
	public default SuccessorsIterator getSuccessors(final int s)
	{
		return SuccessorsIterator.chain(new Iterator<SuccessorsIterator>() {
			private int choice = 0;
			private int choices = getNumChoices(s);

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
	 * Construct a model that is induced by applying strategy {@code strat} to this model.
	 * Note that the "new" model may be just an implicit (read-only) representation. 
	 * @param strat (Memoryless) strategy to use
	 */
	public Model constructInducedModel(MDStrategy strat);

	/**
	 * Export to a dot file, highlighting states in 'mark' and choices for a (memoryless) strategy.
	 */
	public void exportToDotFileWithStrat(PrismLog out, BitSet mark, int strat[]);
}