//==============================================================================
//
//	Copyright (c) 2024-
//	Authors:
//	* Dave Parker <david.parker@cs.oc.ac.uk> (University of Oxford)
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

package io;

import prism.Evaluator;
import prism.ModelInfo;
import prism.PrismException;
import prism.RewardGenerator;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ExplicitModelImporter
{
	/**
	 * Does this importer provide info about state definitions?
	 */
	boolean providesStates();

	/**
	 * Does this importer provide info about labels?
	 */
	boolean providesLabels();

	/**
	 * Get a string summarising the source, e.g. the list of files read in.
	 */
	String sourceString();

	/**
	 * Get info about the model.
	 */
	ModelInfo getModelInfo() throws PrismException;

	/**
	 * Get the number of states.
	 */
	int getNumStates() throws PrismException;

	/**
	 * Get the total number of choices (for nondeterministic models).
	 */
	int getNumChoices() throws PrismException;

	/**
	 * Get the total number of transitions.
	 */
	int getNumTransitions() throws PrismException;

	/**
	 * Get a string stating the model type and how it was obtained.
	 */
	String getModelTypeString();

	/**
	 * Get info about the rewards.
	 */
	RewardGenerator<?> getRewardInfo() throws PrismException;

	/**
	 * Extract state definitions (variable values).
	 * Calls {@code storeStateDefn(s, i, o)} for each state s, variable (index) i and variable value o.
	 * @param storeStateDefn Function to be called for each variable value of each state
	 */
	void extractStates(IOUtils.StateDefnConsumer storeStateDefn) throws PrismException;

	/**
	 * Compute the maximum number of choices (in a nondeterministic model).
	 */
	int computeMaxNumChoices() throws PrismException;

	/**
	 * Extract the (Markov chain) transitions from a .tra file.
	 * The transition probabilities/rates are assumed to be of type double.
	 * @param storeTransition Function to be called for each transition
	 */
	default void extractMCTransitions(IOUtils.MCTransitionConsumer<Double> storeTransition) throws PrismException
	{
		extractMCTransitions(storeTransition, Evaluator.forDouble());
	}

	/**
	 * Extract the (Markov chain) transitions from a .tra file.
	 * The transition probabilities/rates are assumed to be of type Value.
	 * @param storeTransition Function to be called for each transition
	 * @param eval Evaluator for Value objects
	 */
	<Value> void extractMCTransitions(IOUtils.MCTransitionConsumer<Value> storeTransition, Evaluator<Value> eval) throws PrismException;

	/**
	 * Extract the (Markov decision process) transitions from a .tra file.
	 * The transition probabilities/rates are assumed to be of type double.
	 * @param storeTransition Function to be called for each transition
	 */
	default void extractMDPTransitions(IOUtils.MDPTransitionConsumer<Double> storeTransition) throws PrismException
	{
		extractMDPTransitions(storeTransition, Evaluator.forDouble());
	}

	/**
	 * Extract the (Markov decision process) transitions from a .tra file.
	 * The transition probabilities/rates are assumed to be of type Value.
	 * @param storeTransition Function to be called for each transition
	 * @param eval Evaluator for Value objects
	 */
	<Value> void extractMDPTransitions(IOUtils.MDPTransitionConsumer<Value> storeTransition, Evaluator<Value> eval) throws PrismException;

	/**
	 * Extract the (labelled transition system) transitions from a .tra file.
	 * @param storeTransition Function to be called for each transition
	 */
	void extractLTSTransitions(IOUtils.LTSTransitionConsumer storeTransition) throws PrismException;

	/**
	 * Extract info about state labellings and initial states.
	 * Calls {@code storeLabel(s, i)} for each state s satisfying label l,
	 * where l is 0-indexed and matches the label list from {@link #getModelInfo()}.
	 * Calls {@code storeInit(s)} for each initial state s.
	 * @param storeLabel Function to be called for each state satisfying a label
	 * @param storeInit Function to be called for each initial stat
	 */
	void extractLabelsAndInitialStates(BiConsumer<Integer, Integer> storeLabel, Consumer<Integer> storeInit) throws PrismException;

	/**
	 * Extract the state rewards for a given reward structure index.
	 * The transition probabilities/rates are assumed to be of type double.
	 * @param rewardIndex Index of reward structure to extract (0-indexed)
	 * @param storeReward Function to be called for each reward
	 */
	default void extractStateRewards(int rewardIndex, BiConsumer<Integer, Double> storeReward) throws PrismException
	{
		extractStateRewards(rewardIndex, storeReward, Evaluator.forDouble());
	}

	/**
	 * Extract the state rewards for a given reward structure index.
	 * The transition probabilities/rates are assumed to be of type Value.
	 * @param rewardIndex Index of reward structure to extract (0-indexed)
	 * @param storeReward Function to be called for each reward
	 * @param eval Evaluator for Value objects
	 */
	<Value> void extractStateRewards(int rewardIndex, BiConsumer<Integer, Value> storeReward, Evaluator<Value> eval) throws PrismException;

	/**
	 * Extract the (Markov chain) transition rewards for a given reward structure index.
	 * The transition probabilities/rates are assumed to be of type double.
	 * @param rewardIndex Index of reward structure to extract (0-indexed)
	 * @param storeReward Function to be called for each reward
	 */
	default void extractMCTransitionRewards(int rewardIndex, IOUtils.TransitionRewardConsumer<Double> storeReward) throws PrismException
	{
		extractMCTransitionRewards(rewardIndex, storeReward, Evaluator.forDouble());
	}

	/**
	 * Extract the (Markov chain) transition rewards for a given reward structure index.
	 * The transition probabilities/rates are assumed to be of type Value.
	 * @param rewardIndex Index of reward structure to extract (0-indexed)
	 * @param storeReward Function to be called for each reward
	 * @param eval Evaluator for Value objects
	 */
	<Value> void extractMCTransitionRewards(int rewardIndex, IOUtils.TransitionRewardConsumer<Value> storeReward, Evaluator<Value> eval) throws PrismException;

	/**
	 * Extract the (Markov decision process) transition rewards for a given reward structure index.
	 * The transition probabilities/rates are assumed to be of type double.
	 * @param rewardIndex Index of reward structure to extract (0-indexed)
	 * @param storeReward Function to be called for each reward
	 */
	default void extractMDPTransitionRewards(int rewardIndex, IOUtils.TransitionStateRewardConsumer<Double> storeReward) throws PrismException
	{
		extractMDPTransitionRewards(rewardIndex, storeReward, Evaluator.forDouble());
	}

	/**
	 * Extract the (Markov decision process) transition rewards for a given reward structure index.
	 * The transition probabilities/rates are assumed to be of type Value.
	 * @param rewardIndex Index of reward structure to extract (0-indexed)
	 * @param storeReward Function to be called for each reward
	 * @param eval Evaluator for Value objects
	 */
	<Value> void extractMDPTransitionRewards(int rewardIndex, IOUtils.TransitionStateRewardConsumer<Value> storeReward, Evaluator<Value> eval) throws PrismException;
}
