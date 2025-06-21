//==============================================================================
//
//	Copyright (c) 2024-
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

package io;

import common.Interval;
import explicit.DTMC;
import explicit.IDTMC;
import explicit.IMDP;
import explicit.LTS;
import explicit.MDP;
import explicit.Model;
import explicit.NondetModel;
import explicit.rewards.Rewards;
import prism.Evaluator;
import prism.ModelInfo;
import prism.Pair;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Base class for model exporter classes.
 */
public abstract class ModelExporter<Value>
{
	/** Evaluator for model (defaults to one for doubles if not provided). */
	@SuppressWarnings("unchecked")
	protected Evaluator<Value> eval = (Evaluator<Value>) Evaluator.forDouble();

	/** Model export options */
	protected ModelExportOptions modelExportOptions;

	// Model annotations

	protected List<Rewards<Value>> rewards = new ArrayList<>();
	protected List<String> rewardNames = new ArrayList<>();
	protected Evaluator<Value> evalRewards = null;

	protected List<BitSet> labels = new ArrayList<>();
	protected List<String> labelNames = new ArrayList<>();

	protected ModelInfo modelInfo;

	/**
	 * Construct a ModelExporter with default export options.
	 */
	public ModelExporter()
	{
		this(new ModelExportOptions());
	}

	/**
	 * Construct a ModelExporter with the specified export options.
	 */
	public ModelExporter(ModelExportOptions modelExportOptions)
	{
		setModelExportOptions(modelExportOptions);
	}

	// Set methods

	/**
	 * Set the evaluator for dealing with model values.
	 */
	public void setEvaluator(Evaluator<Value> eval)
	{
		this.eval = eval;
	}

	/**
	 * Set the export options.
	 */
	public void setModelExportOptions(ModelExportOptions modelExportOptions)
	{
		this.modelExportOptions = modelExportOptions;
	}

	/**
	 * Add an unnamed reward to be exported.
	 */
	public void addReward(Rewards<Value> rew)
	{
		addReward(rew, "");
	}

	/**
	 * Add a reward to be exported.
	 * {@code rewName} can be null or "" if the reward is unnamed.
	 */
	public void addReward(Rewards<Value> rew, String rewName)
	{
		rewards.add(rew);
		rewardNames.add(rewName == null ? "" : rewName);
	}

	/**
	 * Add multiple rewards to be exported.
	 * Strings in {@code rewName} can be null or "" if the reward is unnamed.
	 */
	public void addRewards(List<Rewards<Value>> rews, List<String> rewNames)
	{
		int numRewards = rews.size();
		for (int i = 0; i < numRewards; i++) {
			addReward(rews.get(i), rewNames.get(i));
		}
	}

	/**
	 * Set a separate evaluator for dealing with reward values.
	 * If not set, the evaluator for model values is used.
	 */
	public void setRewardEvaluator(Evaluator<Value> evalRewards)
	{
		this.evalRewards = evalRewards;
	}

	/**
	 * Add a label to be exported.
	 */
	public void addLabel(BitSet label, String labelName)
	{
		labels.add(label);
		labelNames.add(labelName);
	}

	/**
	 * Add multiple labels to be exported.
	 */
	public void addLabels(List<BitSet> labels, List<String> labelNames)
	{
		int numLabels = labels.size();
		for (int i = 0; i < numLabels; i++) {
			addLabel(labels.get(i), labelNames.get(i));
		}
	}

	/**
	 * Provide information about the model,
	 * e.g., for variable/observable annotations.
	 */
	public void setModelInfo(ModelInfo modelInfo)
	{
		this.modelInfo = modelInfo;
	}

	// Get methods

	/**
	 * Get the evaluator for dealing with model values.
	 */
	public Evaluator<Value> getEvaluator()
	{
		return eval;
	}

	/**
	 * Get the export options.
	 */
	public ModelExportOptions getModelExportOptions()
	{
		return modelExportOptions;
	}

	/**
	 * Get the number of rewards to be exported.
	 */
	public int getNumRewards()
	{
		return rewards.size();
	}

	/**
	 * Get the rewards to be exported.
	 */
	public List<Rewards<Value>> getRewards()
	{
		return rewards;
	}

	/**
	 * Get the ith reward to be exported.
	 */
	public Rewards<Value> getReward(int i)
	{
		return rewards.get(i);
	}

	/**
	 * Get the names of the rewards to be exported.
	 * Some names may be empty ("");
	 */
	public List<String> getRewardNames()
	{
		return rewardNames;
	}

	/**
	 * Get the names of the ith reward to be exported.
	 * May be empty ("");
	 */
	public String getRewardName(int i)
	{
		return rewardNames.get(i);
	}

	/**
	 * Get a separate evaluator for dealing with reward values.
	 */
	public Evaluator<Value> getRewardEvaluator()
	{
		return evalRewards == null ? getEvaluator() : evalRewards;
	}

	/**
	 * Get the number of labels to be exported.
	 */
	public int getNumLabels()
	{
		return labels.size();
	}

	/**
	 * Get the labels to be exported.
	 */
	public List<BitSet> getLabels()
	{
		return labels;
	}

	/**
	 * Get the ith label to be exported.
	 */
	public BitSet getLabel(int i)
	{
		return labels.get(i);
	}

	/**
	 * Get the names of the labels to be exported.
	 */
	public List<String> getLabelNames()
	{
		return labelNames;
	}

	/**
	 * Get the name of the ith label to be exported.
	 */
	public String getLabelName(int i)
	{
		return labelNames.get(i);
	}

	/**
	 * Get information about the model,
	 * e.g., for variable/observable annotations.
	 * May be null.
	 */
	public ModelInfo getModelInfo()
	{
		return modelInfo;
	}

	/**
	 * Export a model to a {@link PrismLog}.
	 * @param model The model
	 * @param out Where to export
	 */
	public abstract void exportModel(Model<Value> model, PrismLog out) throws PrismException;

	/**
	 * Export a model to a file.
	 * @param model The model
	 * @param fileOut File to export to
	 */
	public void exportModel(Model<Value> model, File fileOut) throws PrismException
	{
		try (PrismLog out = new PrismFileLog(fileOut.getPath())) {
			exportModel(model, out);
		}
	}

	// Utility functions

	/**
	 * Format a {@code Value} as a string, based on the {@link Evaluator} and {@link ModelExportOptions}.
	 */
	public String formatValue(Value value, Evaluator<Value> theEval)
	{
		return theEval.toStringExport(value, modelExportOptions.getModelPrecision());
	}

	/**
	 * Format a {@code Value} as a string, based on the {@link Evaluator} and {@link ModelExportOptions}.
	 */
	public String formatValue(Value value)
	{
		return eval.toStringExport(value, modelExportOptions.getModelPrecision());
	}

	/**
	 * Get access to a sorted list of transitions for a choice of a state of a model.
	 * Transitions are sorted by successor state and then (if present) action string.
	 * Note that this means duplicate transitions are removed too.
	 * @param model The model
	 * @param s The state
	 * @param j The choice
	 * @param includeActions Whether to include actions
	 */
	@SuppressWarnings("unchecked")
	public <ValueM> Iterable<Transition<Object>> getSortedTransitionsIterator(Model<ValueM> model, int s, int j, boolean includeActions) throws PrismException
	{
		TreeSet<Transition<Object>> sorted = new TreeSet<>(Transition::compareTo);
		// Get action (if attached to choice)
		Object action = null;
		if (model.getModelType().nondeterministic() && includeActions) {
			action = ((NondetModel<ValueM>) model).getAction(s, j);
		}
		// Extract transitions
		if (!model.getModelType().uncertain()) {
			// DTMCs
			if (model instanceof DTMC) {
				Iterator<Map.Entry<Integer, Pair<ValueM, Object>>> iter = ((DTMC<ValueM>) model).getTransitionsAndActionsIterator(s);
				while (iter.hasNext()) {
					Map.Entry<Integer, Pair<ValueM, Object>> e = iter.next();
					if (includeActions) {
						action = e.getValue().second;
					}
					sorted.add((Transition<Object>) new Transition<>(e.getKey(), e.getValue().first, action, model.getEvaluator()));
				}
			}
			// LTS-like (non-probabilistic, nondeterministic) models
			else if (model instanceof LTS) {
				int succ = ((LTS<ValueM>) model).getSuccessor(s, j);
				sorted.add((Transition<Object>) new Transition<>(succ, model.getEvaluator().one(), action, model.getEvaluator()));
			}
			// MDP-like (probabilistic, nondeterministic) models
			else {
				Iterator<Map.Entry<Integer, ValueM>> iter = ((MDP<ValueM>) model).getTransitionsIterator(s, j);
				while (iter.hasNext()) {
					Map.Entry<Integer, ValueM> e = iter.next();
					sorted.add((Transition<Object>) new Transition<>(e.getKey(), e.getValue(), action, model.getEvaluator()));
				}
			}
		}
		// Interval models
		else {
			// IDTMCs
			if (model instanceof IDTMC) {
				Iterator<Map.Entry<Integer, Pair<Interval<ValueM>, Object>>> iter = ((IDTMC<ValueM>) model).getIntervalTransitionsAndActionsIterator(s);
				while (iter.hasNext()) {
					Map.Entry<Integer, Pair<Interval<ValueM>, Object>> e = iter.next();
					if (includeActions) {
						action = e.getValue().second;
					}
					sorted.add((Transition<Object>) (Transition<? extends Object>) new Transition<>(e.getKey(), e.getValue().first, action, ((IDTMC<ValueM>) model).getIntervalEvaluator()));
				}
			}
			// IMDPs
			else {
				Iterator<Map.Entry<Integer, Interval<ValueM>>> iter = ((IMDP<ValueM>) model).getIntervalTransitionsIterator(s, j);
				while (iter.hasNext()) {
					Map.Entry<Integer, Interval<ValueM>> e = iter.next();
					sorted.add((Transition<Object>) (Transition<? extends Object>) new Transition<>(e.getKey(), e.getValue(), action, ((IMDP<ValueM>) model).getIntervalEvaluator()));
				}
			}
		}
		return sorted;
	}

	/**
	 * Get access to a sorted list of transitions rewards for a state of a (Markov chain) model.
	 * Transitions are sorted by successor state and then (if present) action string.
	 * Note that this means duplicate transitions are removed too.
	 * @param model The model
	 * @param s The state
	 * @param includeActions Whether to include actions
	 */
	public <ValueM> Iterable<Transition<Value>> getSortedTransitionRewardsIterator(DTMC<ValueM> model, Rewards<Value> rewards, int s, boolean includeActions)
	{
		TreeSet<Transition<Value>> sorted = new TreeSet<>(Transition::compareTo);
		Object action = null;
		Iterator<Map.Entry<Integer, Pair<ValueM, Object>>> iter = model.getTransitionsAndActionsIterator(s);
		int k = 0;
		while (iter.hasNext()) {
			Map.Entry<Integer, Pair<ValueM, Object>> e = iter.next();
			if (includeActions) {
				action = e.getValue().second;
			}
			sorted.add(new Transition<>(e.getKey(), rewards.getTransitionReward(s, k), action, rewards.getEvaluator()));
			k++;
		}
		return sorted;
	}

	/**
	 * Get a tuple comprising the state rewards for state {@code s}.
	 * @param allRewards The list of rewards
	 * @param s The state
	 */
	protected RewardTuple<Value> getStateRewardTuple(List<Rewards<Value>> allRewards, int s)
	{
		int numRewards = allRewards.size();
		RewardTuple<Value> tuple = new RewardTuple<>(numRewards);
		for (Rewards<Value> rewards : allRewards) {
			tuple.add(rewards.getStateReward(s));
		}
		return tuple;
	}

	/**
	 * Get a tuple comprising the transition rewards for index {@code j} of state {@code s}.
	 * @param allRewards The list of rewards
	 * @param s The state
	 * @param j The choice/transition index
	 */
	protected RewardTuple<Value> getTransitionRewardTuple(List<Rewards<Value>> allRewards, int s, int j)
	{
		int numRewards = allRewards.size();
		RewardTuple<Value> tuple = new RewardTuple<>(numRewards);
		for (Rewards<Value> rewards : allRewards) {
			tuple.add(rewards.getTransitionReward(s, j));
		}
		return tuple;
	}
}
