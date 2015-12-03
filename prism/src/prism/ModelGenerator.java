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

import java.util.List;

import parser.State;
import parser.Values;
import parser.VarList;

/**
 * Interface for classes that generate a probabilistic model:
 * given a particular model state (represented as a State object),
 * they provide information about the outgoing transitions from that state.
 */
public interface ModelGenerator extends ModelInfo
{
	/**
	 * Set values for *some* undefined constants.
	 * If there are no undefined constants, {@code someValues} can be null.
	 * Undefined constants can be subsequently redefined to different values with the same method.
	 * The current constant values (if set) are available via {@link #getConstantValues()}.
	 */
	public void setSomeUndefinedConstants(Values someValues) throws PrismException;

	/**
	 * Get access to the values for all constants in the model, including the 
	 * undefined constants set previously via the method {@link #setUndefinedConstants(Values)}.
	 * Until they are set for the first time, this method returns null.  
	 */
	public Values getConstantValues();

	/**
	 * Does the model have a single initial state?
	 */
	public boolean hasSingleInitialState() throws PrismException;
	
	/**
	 * Get the initial states of the model.
	 * The returned list should contain fresh State objects that can be kept/modified. 
	 */
	public List<State> getInitialStates() throws PrismException;
	
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
	public int getNumTransitions() throws PrismException;

	/**
	 * Get the number of transitions in the {@code i}th nondeterministic choice.
	 * @param i Index of the nondeterministic choice
	 */
	public int getNumTransitions(int i) throws PrismException;

	/**
	 * Get the action label of a choice, specified by its index.
	 * The label can be any Object, but will often be treated as a string, so it should at least
	 * have a meaningful toString() method implemented. Absence of an action label is denoted by null.
	 * Note: If the model has different actions for different transitions within a choice
	 * (as can be the case for Markov chains), this method returns the action for the first transition.
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
	public boolean isLabelTrue(String label) throws PrismException;
	
	/**
	 * Is the {@code i}th label of the model true in the state currently being explored?
	 * @param i The index of the label to check 
	 */
	public boolean isLabelTrue(int i) throws PrismException;
	
	/**
	 * Get the state reward for state {@code state} using the reward structure with index {@code index}.
	 */
	public double getStateReward(int index, State state) throws PrismException;
	
	// TODO: can we remove this?
	public VarList createVarList();
}
