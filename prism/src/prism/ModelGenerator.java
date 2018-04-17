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
	 * Get the state that is currently being explored, i.e. the last one for which
	 * {@link #exploreState(State)} was called. Can return null if there is no such state. 
	 */
	public State getExploreState();
	
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
	 * Get the action label of a transition, specified by its index.
	 * The label can be any Object, but will often be treated as a string, so it should at least
	 * have a meaningful toString() method implemented. Absence of an action label is denoted by null.
	 * Note: For most types of models, the action label will be the same for all transitions within
	 * the same nondeterministic choice, so it is better to query the action by choice, not transition.
	 */
	public Object getTransitionAction(int i) throws PrismException;

	/**
	 * Get the action label of a transition within a choice, specified by its index/offset.
	 * The label can be any Object, but will often be treated as a string, so it should at least
	 * have a meaningful toString() method implemented. Absence of an action label is denoted by null.
	 * Note: For most types of models, the action label will be the same for all transitions within
	 * the same nondeterministic choice (i.e. for each different value of {@code offset}),
	 * but for Markov chains this may not necessarily be the case.
	 */
	public Object getTransitionAction(int i, int offset) throws PrismException;

	/**
	 * Get the action label of a choice, specified by its index.
	 * The label can be any Object, but will often be treated as a string, so it should at least
	 * have a meaningful toString() method implemented. Absence of an action label is denoted by null.
	 * Note: If the model has different actions for different transitions within a choice
	 * (as can be the case for Markov chains), this method returns the action for the first transition.
	 * So, this method is essentially equivalent to {@code getTransitionAction(i, 0)}. 
	 */
	public default Object getChoiceAction(int i) throws PrismException
	{
		// Default implementation 
		return getTransitionAction(i, 0);
	}

	/**
	 * Get the probability/rate of a transition within a choice, specified by its index/offset.
	 * @param i Index of the nondeterministic choice
	 * @param offset Index of the transition within the choice
	 */
	public double getTransitionProbability(int i, int offset) throws PrismException;

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
	
	/**
	 * Get the state reward of the {@code r}th reward structure for state {@code state}
	 * ({@code r} is indexed from 0, not from 1 like at the user (property language) level).
	 * @param r The index of the reward structure to use
	 * @param state The state in which to evaluate the rewards 
	 */
	public default double getStateReward(int r, State state) throws PrismException
	{
		// Default reward to 0 (no reward structures by default anyway)
		return 0.0;
	}

	
	/**
	 * Get the state-action reward of the {@code r}th reward structure for state {@code state} and action {@code action}
	 * ({@code r} is indexed from 0, not from 1 like at the user (property language) level).
	 * If a reward structure has no transition rewards, you can indicate this by implementing
	 * the method {@link #rewardStructHasTransitionRewards(int)}, which may improve efficiency
	 * and/or allow use of algorithms/implementations that do not support transition rewards rewards.
	 * @param r The index of the reward structure to use
	 * @param state The state in which to evaluate the rewards 
	 * @param action The outgoing action label 
	 */
	public default double getStateActionReward(int r, State state, Object action) throws PrismException
	{
		// Default reward to 0 (no reward structures by default anyway)
		return 0.0;
	}
}
