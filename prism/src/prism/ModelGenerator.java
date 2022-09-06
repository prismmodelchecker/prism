//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Nishan Kamaleson <nxk249@bham.ac.uk> (University of Birmingham)
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

package prism;

import java.util.Collections;
import java.util.List;

import parser.State;

/**
 * Interface for classes that generate a probabilistic model:
 * given a particular model state (represented as a State object),
 * they provide information about the outgoing transitions from that state.
 */
public interface ModelGenerator extends ModelInfo
{
	/**
	 * Does the model have a single initial state?
	 */
	public default boolean hasSingleInitialState() throws PrismException
	{
		// Default to the case of a single initial state
		return true;
	}
	
	/**
	 * Get the initial states of the model.
	 * The returned list should contain fresh State objects that can be kept/modified. 
	 */
	public default List<State> getInitialStates() throws PrismException
	{
		// Default to the case of a single initial state
		return Collections.singletonList(getInitialState());
	}
	
	/**
	 * Get the initial state of the model, if there is just one,
	 * or the first of the initial states if there are more than one.
	 * The returned State object should be fresh, i.e. can be kept/modified. 
	 */
	public State getInitialState() throws PrismException;
	
	/**
	 * Explore a given state of the model. After a call to this method,
	 * the class should be able to respond to the various methods that are
	 * available to query the outgoing transitions from the current state.
	 * @param exploreState State to explore (generate transition information for)
	 */
	public void exploreState(State exploreState) throws PrismException;

	/**
	 * Get the number of nondeterministic choices in the current state.
	 */
	public int getNumChoices() throws PrismException;

	/**
	 * Get the total number of transitions in the current state.
	 */
	public default int getNumTransitions() throws PrismException
	{
		// Default implementation just extracts from getNumChoices() and getNumTransitions(i) 
		int tot = 0;
		int n = getNumChoices();
		for (int i = 0; i < n; i++) {
			tot += getNumTransitions(i);
		}
		return tot;
	}

	/**
	 * Get the number of transitions in the {@code i}th nondeterministic choice.
	 * @param i Index of the nondeterministic choice
	 */
	public int getNumTransitions(int i) throws PrismException;

	/**
	 * Get the index of the choice containing a transition of a given index.
	 * @param index Index of the transition amongst all transitions
	 */
	public default int getChoiceIndexOfTransition(int index) throws PrismException
	{
		// Default implementation just extracts from getNumChoices() and getNumTransitions(i) 
		int tot = 0;
		int n = getNumChoices();
		for (int i = 0; i < n; i++) {
			tot += getNumTransitions(i);
			if (index < tot) {
				return i;
			}
		}
		// Won't happen if index is valid:
		return -1;
	}
	
	/**
	 * Get the offset of a transition within its containing choice.
	 * @param index Index of the transition amongst all transitions
	 */
	public default int getChoiceOffsetOfTransition(int index) throws PrismException
	{
		// Default implementation just extracts from getNumChoices() and getNumTransitions(i) 
		int tot = 0;
		int n = getNumChoices();
		for (int i = 0; i < n; i++) {
			tot += getNumTransitions(i);
			if (index < tot) {
				return index - (tot - getNumTransitions(i));
			}
		}
		// Won't happen if index is valid:
		return -1;
	}
	
	/**
	 * Get the (total) index of a transition from the index of its containing choice and its offset within it.
	 * @param i Index of the nondeterministic choice
	 * @param offset Index of the transition within the choice
	 */
	public default int getTotalIndexOfTransition(int i, int offset) throws PrismException
	{
		// Default implementation just extracts from getNumChoices() and getNumTransitions(i) 
		int tot = 0;
		for (int j = 0; j < i; j++) {
			tot += getNumTransitions(j);
		}
		return tot + offset;
	}
	
	/**
	 * Is there a deadlock (i.e. no available transitions)?
	 */
	public default boolean isDeadlock() throws PrismException
	{
		return getNumChoices() == 0;
	}

	/**
	 * Get the action label of a transition within a choice, specified by its index/offset.
	 * The label can be any Object, but will often be treated as a string, so it should at least
	 * have a meaningful toString() method implemented. Absence of an action label is denoted by null.
	 * Note: For most types of models, the action label will be the same for all transitions within
	 * the same nondeterministic choice (i.e. for each different value of {@code offset}),
	 * but for Markov chains this may not necessarily be the case.
	 * @param i Index of the nondeterministic choice
	 * @param offset Index of the transition within the choice
	 */
	public Object getTransitionAction(int i, int offset) throws PrismException;

	/**
	 * Get a description for the action label of a choice, specified by its index/offset.
	 * This might be displayed in a representation of a path, e.g. in the simulator UI.
	 * By default this, will be {@code toString()} for {@link #getChoiceAction(int, int)},
	 * but can be customised, e.g. a PRISM model shows "[a]" for a synchronous action a
	 * and "M" for an unlabelled action belonging to a module M.
	 * For unlabelled transitions, this should return "", not null. 
	 * Note: For most types of models, the action label will be the same for all transitions within
	 * the same nondeterministic choice (i.e. for each different value of {@code offset}),
	 * but for Markov chains this may not necessarily be the case.
	 * @param i Index of the nondeterministic choice
	 * @param offset Index of the transition within the choice
	 */
	public default String getTransitionActionString(int i, int offset) throws PrismException
	{
		// Default implementation: use toString on action object 
		Object action = getTransitionAction(i, offset); 
		return action == null ? "" : action.toString();
	}

	/**
	 * Get the action label of a choice, specified by its index.
	 * The label can be any Object, but will often be treated as a string, so it should at least
	 * have a meaningful toString() method implemented. Absence of an action label is denoted by null.
	 * Note: If the model has different actions for different transitions within a choice
	 * (as can be the case for Markov chains), this method returns the action for the first transition.
	 * So, this method is essentially equivalent to {@code getTransitionAction(i, 0)}. 
	 * @param i Index of the nondeterministic choice
	 */
	public default Object getChoiceAction(int i) throws PrismException
	{
		// Default implementation 
		return getTransitionAction(i, 0);
	}

	/**
	 * Get a description for the action label of a choice, specified by its index.
	 * This might be displayed in a representation of a path, e.g. in the simulator UI.
	 * By default this, will be {@code toString()} for {@link #getChoiceAction(int)},
	 * but can be customised, e.g. a PRISM model shows "[a]" for a synchronous action a
	 * and "M" for an unlabelled action belonging to a module M. 
	 * For unlabelled choices, this should return "", not null. 
	 * Note: If the model has different actions for different transitions within a choice
	 * (as can be the case for Markov chains), this method returns the action for the first transition.
	 * So, this method is essentially equivalent to {@code getTransitionActionString(i, 0)}. 
	 * @param i Index of the nondeterministic choice
	 */
	public default String getChoiceActionString(int i) throws PrismException
	{
		// Default implementation: use toString on action object
		Object action = getChoiceAction(i); 
		return action == null ? "" : action.toString();
	}

	/**
	 * Get the probability/rate of a transition within a choice, specified by its index/offset.
	 * @param i Index of the nondeterministic choice
	 * @param offset Index of the transition within the choice
	 */
	public double getTransitionProbability(int i, int offset) throws PrismException;

	/**
	 * Get the sum of probabilities/rates of transitions within a choice, specified by its index.
	 * @param i Index of the nondeterministic choice
	 */
	public default double getChoiceProbabilitySum(int i) throws PrismException
	{
		int numTransitions = getNumTransitions(i);
		double prob = 0;
		for (int offset = 0; offset < numTransitions; offset++) {
			prob += getTransitionProbability(i, offset);
		}
		return prob;
	}

	/**
	 * Get the sum of all probabilities/rates of transitions across choices.
	 */
	public default double getProbabilitySum() throws PrismException
	{
		int numChoices = getNumChoices();
		double prob = 0;
		for (int i = 0; i < numChoices; i++) {
			prob += getChoiceProbabilitySum(i);
		}
		return prob;
	}

	/**
	 * Are the choices deterministic? (i.e. a single probability 1.0 transition)
	 * (will also return true for a continuous-time model matching this
	 * definition, since TransitionList does not know about model type)
	 */
	public default boolean isDeterministic() throws PrismException
	{
		return getNumChoices() == 1 && getNumTransitions(0) == 1 && getTransitionProbability(0, 0) == 1.0;
	}

	/**
	 * Get a string describing the update comprising a transition, specified by its index/offset.
	 * The default implementation is of the form "x'=1, y'=0",
	 * including all variables in the state. An implementation might choose,
	 * for example, to only include variables that are actually updated. 
	 * @param i Index of the nondeterministic choice
	 * @param offset Index of the transition within the choice
	 */
	public default String getTransitionUpdateString(int i, int offset) throws PrismException
	{
		// By default, just build a string showing new values of all
		// variables, regardless of whether they changed or not 
		State nextState = computeTransitionTarget(i, offset);
		String update = "";
		int numVars = getNumVars();
		for (int j = 0; j < numVars; j++) {
			update += ((j > 0) ? ", " : "") + getVarName(j) + "'=" + nextState.varValues[j]; 
		}
		return update;
	}

	/**
	 * Optionally, a more verbose version of {@link #getTransitionUpdateString(int, int)}.
	 * The default implementation just returns the same string.
	 * @param i Index of the nondeterministic choice
	 * @param offset Index of the transition within the choice
	 */
	public default String getTransitionUpdateStringFull(int i, int offset) throws PrismException
	{
		return getTransitionUpdateString(i, offset);
	}

	/**
	 * Get the target (as a new State object) of a transition within a choice, specified by its index/offset.
	 * @param i Index of the nondeterministic choice
	 * @param offset Index of the transition within the choice
	 */
	public State computeTransitionTarget(int i, int offset) throws PrismException;
	
	/**
	 * Is label {@code label} true in the state currently being explored?
	 * @param label The name of the label to check 
	 */
	public default boolean isLabelTrue(String label) throws PrismException
	{
		// Default implementation: Look up label and then check by index
		int i = getLabelIndex(label);
		if (i == -1) {
			throw new PrismException("Label \"" + label + "\" not defined");
		} else {
			return isLabelTrue(i);
		}
	}
	
	/**
	 * Is the {@code i}th label of the model true in the state currently being explored?
	 * @param i The index of the label to check 
	 */
	public default boolean isLabelTrue(int i) throws PrismException
	{
		// No labels by default
		throw new PrismException("Label number \"" + i + "\" not defined");
	}
}
