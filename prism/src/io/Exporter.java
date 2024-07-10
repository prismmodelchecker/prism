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

import explicit.DTMC;
import explicit.LTS;
import explicit.MDP;
import explicit.Model;
import explicit.NondetModel;
import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;
import explicit.rewards.Rewards;
import prism.Evaluator;
import prism.Pair;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Base class for exporter classes.
 */
public class Exporter<Value>
{
	/** Evaluator for model (defaults to oen for doubles if not provided). */
	protected Evaluator<Value> eval = (Evaluator<Value>) Evaluator.forDouble();

	/** Model export options */
	protected ModelExportOptions modelExportOptions;

	public Exporter()
	{
		this(new ModelExportOptions());
	}

	public Exporter(ModelExportOptions modelExportOptions)
	{
		setModelExportOptions(modelExportOptions);
	}

	public void setEvaluator(Evaluator<Value> eval)
	{
		this.eval = eval;
	}

	public void setModelExportOptions(ModelExportOptions modelExportOptions)
	{
		this.modelExportOptions = modelExportOptions;
	}

	public Evaluator<Value> getEvaluator()
	{
		return eval;
	}

	public ModelExportOptions getModelExportOptions()
	{
		return modelExportOptions;
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
	public Iterable<Transition<Value>> getSortedTransitionsIterator(Model<Value> model, int s, int j, boolean includeActions)
	{
		TreeSet<Transition<Value>> sorted = new TreeSet<>(Transition::compareTo);
		Object action = null;
		if (model.getModelType().nondeterministic() && includeActions) {
			action = ((NondetModel<Value>) model).getAction(s, j);
		}
		if (model instanceof DTMC) {
			Iterator<Map.Entry<Integer, Pair<Value, Object>>> iter = ((DTMC<Value>) model).getTransitionsAndActionsIterator(s);
			while (iter.hasNext()) {
				Map.Entry<Integer, Pair<Value, Object>> e = iter.next();
				if (includeActions) {
					action = e.getValue().second;
				}
				sorted.add(new Transition<>(e.getKey(), e.getValue().first, action));
			}
		} else if (model instanceof LTS) {
			int succ = ((LTS<Value>) model).getSuccessor(s, j);
			sorted.add(new Transition<>(succ, eval.one(), action));
		} else {
			Iterator<Map.Entry<Integer, Value>> iter = ((MDP<Value>) model).getTransitionsIterator(s, j);
			while (iter.hasNext()) {
				Map.Entry<Integer, Value> e = iter.next();
				sorted.add(new Transition<>(e.getKey(), e.getValue(), action));
			}
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
		for (int r = 0; r < numRewards; r++) {
			Rewards<Value> rewards = allRewards.get(r);
			if (rewards instanceof MCRewards) {
				tuple.add(((MCRewards<Value>) rewards).getStateReward(s));
			} else if (rewards instanceof MDPRewards) {
				tuple.add(((MDPRewards<Value>) rewards).getStateReward(s));
			}
		}
		return tuple;
	}

	/**
	 * Get a tuple comprising the transition rewards for choice {@code j} of state {@code s}.
	 * @param allRewards The list of rewards
	 * @param s The state
	 * @param j The choice
	 */
	protected RewardTuple<Value> getTransitionRewardTuple(List<Rewards<Value>> allRewards, int s, int j)
	{
		int numRewards = allRewards.size();
		RewardTuple<Value> tuple = new RewardTuple<>(numRewards);
		for (int r = 0; r < numRewards; r++) {
			Rewards<Value> rewards = allRewards.get(r);
			if (rewards instanceof MDPRewards) {
				tuple.add(((MDPRewards<Value>) rewards).getTransitionReward(s, j));
			}
		}
		return tuple;
	}
}
