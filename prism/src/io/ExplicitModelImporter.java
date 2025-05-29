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

import parser.ast.DeclarationInt;
import parser.ast.DeclarationType;
import parser.ast.Expression;
import parser.type.Type;
import parser.type.TypeInt;
import prism.Evaluator;
import prism.ModelInfo;
import prism.PrismException;
import prism.RewardInfo;

import java.util.BitSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Base class for importers from explicit model sources.
 */
public abstract class ExplicitModelImporter
{
	// Importer config or cached information

	/** How transition rewards are indexed */
	public enum TransitionRewardIndexing {
		OFFSET, // Offset within rewards for state/choice
		STATE // Index of successor state
	};
	protected TransitionRewardIndexing transitionRewardIndexing = TransitionRewardIndexing.OFFSET;

	/** Should deadlocks be detected and fixed (by adding a self-loop) on import? */
	protected boolean fixdl;

	/**
	 * Specify whether should deadlocks be detected and fixed (by adding a self-loop) on import
	 */
	public void setFixDeadlocks(boolean fixdl)
	{
		this.fixdl = fixdl;
	}

	/**
	 * Specify how transition rewards should be supplied when extracted.
	 */
	public void setTransitionRewardIndexing(TransitionRewardIndexing transitionRewardIndexing)
	{
		this.transitionRewardIndexing = transitionRewardIndexing;
	}

	/**
	 * Access provide to a model (usually constructed earlier using this importer)
	 * which can be looked up to identify transition info (usually for extracting rewards).
	 */
	protected explicit.Model modelLookup;

	/**
	 * Provide access to a model (usually constructed earlier using this importer)
	 * which can be looked up to identify transition info (usually for extracting rewards).
	 */
	public void setModel(explicit.Model modelLookup)
	{
		this.modelLookup = modelLookup;
	}

	// Methods to be implemented by an importer

	/**
	 * Does this importer provide info about state definitions?
	 */
	public abstract boolean providesStates();

	/**
	 * Does this importer provide info about labels?
	 */
	public abstract boolean providesLabels();

	/**
	 * Get a string summarising the source, e.g. the list of files read in.
	 */
	public abstract String sourceString();

	/**
	 * Get info about the model (type, variables, labels, etc.).
	 * If {@link #providesLabels()} returns false, this will report zero labels.
	 * If {@link #providesStates()} returns false, this will report a single
	 * integer-valued variable with name {@link #defaultVariableName()}.
	 */
	public abstract ModelInfo getModelInfo() throws PrismException;

	/**
	 * Get the number of states.
	 */
	public abstract int getNumStates() throws PrismException;

	/**
	 * Get the total number of choices (for nondeterministic models).
	 */
	public abstract int getNumChoices() throws PrismException;

	/**
	 * Get the total number of transitions.
	 */
	public abstract int getNumTransitions() throws PrismException;

	/**
	 * Get the indices of the states which are/were deadlocks.
	 */
	public abstract BitSet getDeadlockStates() throws PrismException;

	/**
	 * Get the number of states which are/were deadlocks.
	 */
	public abstract int getNumDeadlockStates() throws PrismException;

	/**
	 * Get a string stating the model type and how it was obtained.
	 */
	public String getModelTypeString() throws PrismException
	{
		// By default, just return the type, no explanation
		return getModelInfo().getModelType().toString();
	}

	/**
	 * Get info about the rewards.
	 */
	public abstract RewardInfo getRewardInfo() throws PrismException;

	/**
	 * Extract state definitions (variable values).
	 * Calls {@code storeStateDefn(s, i, o)} for each state s, variable (index) i and variable value o.
	 * If {@link #providesStates()} returns false, this should report a single
	 * integer-valued variable range between 0 and {@link #getNumStates()} - 1.
	 * @param storeStateDefn Function to be called for each variable value of each state
	 */
	public void extractStates(IOUtils.StateDefnConsumer storeStateDefn) throws PrismException
	{
		// Default implementation - assume one integer variable
		int numStates = getNumStates();
		for (int s = 0; s < numStates; s++) {
			storeStateDefn.accept(s, 0, s);
		}
	}

	/**
	 * Compute the maximum number of choices (in a nondeterministic model).
	 */
	public abstract int computeMaxNumChoices() throws PrismException;

	/**
	 * Extract the (Markov chain) transitions.
	 * The transition probabilities/rates are assumed to be of type double.
	 * @param storeTransition Function to be called for each transition
	 */
	public void extractMCTransitions(IOUtils.MCTransitionConsumer<Double> storeTransition) throws PrismException
	{
		extractMCTransitions(storeTransition, Evaluator.forDouble());
	}

	/**
	 * Extract the (Markov chain) transitions.
	 * The transition probabilities/rates are assumed to be of type Value.
	 * @param storeTransition Function to be called for each transition
	 * @param eval Evaluator for Value objects
	 */
	public abstract <Value> void extractMCTransitions(IOUtils.MCTransitionConsumer<Value> storeTransition, Evaluator<Value> eval) throws PrismException;

	/**
	 * Extract the (Markov decision process) transitions.
	 * The transition probabilities/rates are assumed to be of type double.
	 * @param storeTransition Function to be called for each transition
	 */
	public void extractMDPTransitions(IOUtils.MDPTransitionConsumer<Double> storeTransition) throws PrismException
	{
		extractMDPTransitions(storeTransition, Evaluator.forDouble());
	}

	/**
	 * Extract the (Markov decision process) transitions.
	 * The transition probabilities/rates are assumed to be of type Value.
	 * @param storeTransition Function to be called for each transition
	 * @param eval Evaluator for Value objects
	 */
	public abstract <Value> void extractMDPTransitions(IOUtils.MDPTransitionConsumer<Value> storeTransition, Evaluator<Value> eval) throws PrismException;

	/**
	 * Extract the (labelled transition system) transitions.
	 * @param storeTransition Function to be called for each transition
	 */
	public abstract void extractLTSTransitions(IOUtils.LTSTransitionConsumer storeTransition) throws PrismException;

	/**
	 * Extract info about state labellings and initial states.
	 * Calls {@code storeLabel(s, i)} for each state s satisfying label l,
	 * where l is 0-indexed and matches the label list from {@link #getModelInfo()}.
	 * Calls {@code storeInit(s)} for each initial state s.
	 * Any "deadlock" labels are ignored.
	 * @param storeLabel Function to be called for each state satisfying a label
	 * @param storeInit Function to be called for each initial stat
	 */
	public void extractLabelsAndInitialStates(BiConsumer<Integer, Integer> storeLabel, Consumer<Integer> storeInit) throws PrismException
	{
		extractLabelsAndInitialStates(storeLabel, storeInit, null);
	}

	/**
	 * Extract info about state labellings and initial states.
	 * Calls {@code storeLabel(s, i)} for each state s satisfying label l,
	 * where l is 0-indexed and matches the label list from {@link #getModelInfo()}.
	 * Calls {@code storeInit(s)} for each initial state s.
	 * If {@code storeDeadlock} is non-null, it will be called for each state labelled
	 * with "deadlock", regardless of whether it has been detected as a deadlock and/or fixed.
	 * @param storeLabel Function to be called for each state satisfying a label
	 * @param storeInit Function to be called for each initial state
	 * @param storeDeadlock Function to be called for each state marked as a deadlock
	 */
	public abstract void extractLabelsAndInitialStates(BiConsumer<Integer, Integer> storeLabel, Consumer<Integer> storeInit, Consumer<Integer> storeDeadlock) throws PrismException;

	/**
	 * Extract the state rewards for a given reward structure index.
	 * The transition probabilities/rates are assumed to be of type double.
	 * @param rewardIndex Index of reward structure to extract (0-indexed)
	 * @param storeReward Function to be called for each reward
	 */
	public void extractStateRewards(int rewardIndex, BiConsumer<Integer, Double> storeReward) throws PrismException
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
	public abstract <Value> void extractStateRewards(int rewardIndex, BiConsumer<Integer, Value> storeReward, Evaluator<Value> eval) throws PrismException;

	/**
	 * Extract the (Markov chain) transition rewards for a given reward structure index.
	 * These are supplied as tuples (s,i,v) where s is the (source) state and v is the reward value.
	 * The index i is either the offset within the state or the index of the successor state,
	 * depending on what has been specified with {@link #setTransitionRewardIndexing(TransitionRewardIndexing)}.
	 * The reward values are assumed to be of type double.
	 * @param rewardIndex Index of reward structure to extract (0-indexed)
	 * @param storeReward Function to be called for each reward
	 */
	public void extractMCTransitionRewards(int rewardIndex, IOUtils.TransitionRewardConsumer<Double> storeReward) throws PrismException
	{
		extractMCTransitionRewards(rewardIndex, storeReward, Evaluator.forDouble());
	}

	/**
	 * Extract the (Markov chain) transition rewards for a given reward structure index.
	 * These are supplied as tuples (s,i,v) where s is the (source) state and v is the reward value.
	 * The index i is either the offset within the state or the index of the successor state,
	 * depending on what has been specified with {@link #setTransitionRewardIndexing(TransitionRewardIndexing)}.
	 * The reward values are assumed to be of type Value.
	 * @param rewardIndex Index of reward structure to extract (0-indexed)
	 * @param storeReward Function to be called for each reward
	 * @param eval Evaluator for Value objects
	 */
	public abstract <Value> void extractMCTransitionRewards(int rewardIndex, IOUtils.TransitionRewardConsumer<Value> storeReward, Evaluator<Value> eval) throws PrismException;

	/**
	 * Extract the (Markov decision process) transition rewards for a given reward structure index.
	 * These are supplied as tuples (s,i,v) where s is the (source) state,
	 * i is the choice index and v is the reward value.
	 * The reward values are assumed to be of type double.
	 * @param rewardIndex Index of reward structure to extract (0-indexed)
	 * @param storeReward Function to be called for each reward
	 */
	public void extractMDPTransitionRewards(int rewardIndex, IOUtils.TransitionRewardConsumer<Double> storeReward) throws PrismException
	{
		extractMDPTransitionRewards(rewardIndex, storeReward, Evaluator.forDouble());
	}

	/**
	 * Extract the (Markov decision process) transition rewards for a given reward structure index.
	 * These are supplied as tuples (s,i,v) where s is the (source) state,
	 * i is the choice index and v is the reward value.
	 * The reward values are assumed to be of type Value.
	 * @param rewardIndex Index of reward structure to extract (0-indexed)
	 * @param storeReward Function to be called for each reward
	 * @param eval Evaluator for Value objects
	 */
	public abstract <Value> void extractMDPTransitionRewards(int rewardIndex, IOUtils.TransitionRewardConsumer<Value> storeReward, Evaluator<Value> eval) throws PrismException;

	// Defaults for a single variable name when none is specified

	/**
	 * Get the default name for the (single) variable when none is specified.
	 * By default, this is "x".
	 */
	public String defaultVariableName()
	{
		return "x";
	}

	/**
	 * Get the default type for the (single) variable when none is specified.
	 * By default, this is {@code int}.
	 */
	public Type defaultVariableType()
	{
		return TypeInt.getInstance();
	}

	/**
	 * Get the default type declaration for the (single) variable when none is specified.
	 * By default, this is {@code [0..(numStates-1)]}.
	 */
	public DeclarationType defaultVariableDeclarationType() throws PrismException
	{
		return new DeclarationInt(Expression.Int(0), Expression.Int(getNumStates() - 1));
	}
}
