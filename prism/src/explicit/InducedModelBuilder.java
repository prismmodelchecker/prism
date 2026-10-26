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

package explicit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import common.Interval;
import explicit.rewards.Rewards;
import explicit.rewards.RewardsSimple;
import parser.State;
import prism.Evaluator;
import prism.ModelType;
import prism.PrismException;
import prism.PrismNotSupportedException;
import strat.Strategy;
import strat.StrategyChoices;
import strat.StrategyExportOptions.InducedModelMode;
import strat.UndefinedChoicePolicy;

/**
 * Shared code for building the model induced by a strategy on a nondeterministic model,
 * used by {@link ConstructInducedModel} (for memoryless strategies) and
 * {@link ConstructStrategyProduct} (for finite-memory strategies, via a product).
 * The caller determines the states of the induced model (and the order they are added);
 * for each one, this class adds the state, copies its labels and state rewards,
 * and adds the choices/transitions picked by the strategy
 * (resolving any undefined choice according to the policy).
 */
class InducedModelBuilder<Value>
{
	/**
	 * Map from a successor state of the original model to a state of the induced model.
	 */
	@FunctionalInterface
	interface SuccessorMap
	{
		/**
		 * Get the index of the state of the induced model for successor {@code s2}
		 * of a choice with action {@code act} (adding the state to the model, if needed).
		 */
		int apply(Object act, int s2) throws PrismException;
	}

	/**
	 * Adds a transition to a (non-nondeterministic) induced model.
	 */
	@FunctionalInterface
	private interface TransitionAdder<T>
	{
		void add(int s, int s2, T prob, Object act);
	}

	/**
	 * A choice of the induced model: one or more (merged) choices of the original model,
	 * picked by the strategy, plus an action label.
	 */
	private static class InducedChoice
	{
		final BitSet choices;
		final Object action;

		InducedChoice(BitSet choices, Object action)
		{
			this.choices = choices;
			this.action = action;
		}
	}

	// Original model and strategy
	private final NondetModel<Value> model;
	private final Strategy<Value> strat;
	// Induced model and its type
	private final ModelSimple<Value> inducedModel;
	private final ModelType inducedModelType;
	// Is the induced model nondeterministic?
	private final boolean nondet;
	// Does the original model have interval probabilities?
	private final boolean interval;
	private final Evaluator<Interval<Value>> intervalEvaluator;
	// Adders for transitions of non-nondeterministic induced models
	private final TransitionAdder<Value> transAdder;
	private final TransitionAdder<Interval<Value>> transAdderIntv;
	// Construction options
	private final InducedModelMode mode;
	private final UndefinedChoicePolicy undefinedChoicePolicy;
	// Description of the construction, for error messages
	private final String description;
	// Labels of the original/induced models (null if not copied)
	private Map<String, BitSet> origLabels = null;
	private Map<String, BitSet> inducedLabels = null;
	// Rewards of the original/induced models (null if not copied)
	private List<Rewards<Value>> origRewardsList = null;
	private List<RewardsSimple<Value>> inducedRewardsList = null;

	/**
	 * Create a builder, which adds states and transitions to an (empty) induced model.
	 * @param model The original model
	 * @param strat The strategy
	 * @param inducedModel The (empty) induced model
	 * @param inducedModelType The type of the induced model
	 * @param mode Mode of construction ("restrict" or "reduce")
	 * @param undefinedChoicePolicy Policy for resolving undefined strategy choices
	 * @param copyLabels Whether to copy labels to the induced model
	 * @param copyRewards Whether to copy rewards to the induced model
	 * @param description Description of the construction, for error messages
	 */
	@SuppressWarnings("unchecked")
	InducedModelBuilder(NondetModel<Value> model, Strategy<Value> strat, ModelSimple<Value> inducedModel, ModelType inducedModelType, InducedModelMode mode,
			UndefinedChoicePolicy undefinedChoicePolicy, boolean copyLabels, boolean copyRewards, String description) throws PrismException
	{
		this.model = model;
		this.strat = strat;
		this.inducedModel = inducedModel;
		this.inducedModelType = inducedModelType;
		this.mode = mode;
		this.undefinedChoicePolicy = undefinedChoicePolicy;
		this.description = description;
		nondet = inducedModelType.nondeterministic();
		interval = model.getModelType() == ModelType.IMDP;
		intervalEvaluator = interval ? ((IntervalModel<Value>) model).getIntervalEvaluator() : null;
		transAdder = inducedModel instanceof DTMCSimple ? ((DTMCSimple<Value>) inducedModel)::addToProbability : null;
		transAdderIntv = inducedModel instanceof IDTMCSimple ? ((IDTMCSimple<Value>) inducedModel)::addToProbability : null;
		// Create empty label bitsets for the induced model, if needed
		if (copyLabels) {
			origLabels = model.getLabelToStatesMap();
			inducedLabels = new LinkedHashMap<>();
			for (String name : origLabels.keySet()) {
				inducedLabels.put(name, new BitSet());
			}
		}
		// Create empty reward structures for the induced model, if needed
		if (copyRewards) {
			int numRewardStructs = model.getNumRewards();
			origRewardsList = new ArrayList<>(numRewardStructs);
			inducedRewardsList = new ArrayList<>(numRewardStructs);
			for (int r = 0; r < numRewardStructs; r++) {
				Rewards<Value> origRews = model.getRewards(r);
				origRewardsList.add(origRews);
				RewardsSimple<Value> inducedRews = new RewardsSimple<>(0);
				inducedRews.setEvaluator(origRews.getEvaluator());
				inducedRewardsList.add(inducedRews);
			}
		}
	}

	/**
	 * Add a state to the induced model, corresponding to state {@code s} of the original model.
	 * Returns the index of the new state.
	 */
	@SuppressWarnings("unchecked")
	int addState(int s)
	{
		switch (inducedModelType) {
		case STPG:
			((STPGSimple<Value>) inducedModel).addState(((STPG<Value>) model).getPlayer(s));
			break;
		default:
			inducedModel.addState();
			break;
		}
		return inducedModel.getNumStates() - 1;
	}

	/**
	 * Copy the labels and state rewards of state {@code s} of the original model
	 * to state {@code sInduced} of the induced model.
	 */
	void copyStateInfo(int s, int sInduced)
	{
		if (origLabels != null) {
			for (Map.Entry<String, BitSet> e : origLabels.entrySet()) {
				if (e.getValue().get(s)) {
					inducedLabels.get(e.getKey()).set(sInduced);
				}
			}
		}
		if (origRewardsList != null) {
			for (int r = 0; r < origRewardsList.size(); r++) {
				Rewards<Value> origRews = origRewardsList.get(r);
				if (origRews.hasStateRewards()) {
					inducedRewardsList.get(r).setStateReward(sInduced, origRews.getStateReward(s));
				}
			}
		}
	}

	/**
	 * Add the choices/transitions for state {@code sInduced} of the induced model,
	 * i.e., those picked by the strategy in state {@code s} of the original model,
	 * with memory {@code m} (-1 for a memoryless strategy).
	 * @param s State of the original model
	 * @param m Strategy memory
	 * @param sInduced State of the induced model
	 * @param succMap Map from successor states of the original model to induced model states
	 */
	@SuppressWarnings("unchecked")
	void addChoices(int s, int m, int sInduced, SuccessorMap succMap) throws PrismException
	{
		// Find the choices picked by the strategy (resolving any undefined choice according to the policy)
		// and the decision used to pick them (used below for probabilities and induced action labels)
		StrategyChoices.Chosen chosen = StrategyChoices.getChosenChoices(model, strat, s, m, undefinedChoicePolicy);
		// For STOP, no choices are picked, so nothing is added: the state has no outgoing
		// transitions/choices, and is marked as a deadlock when deadlocks are found at the end.
		// For LOOP, no choices are picked, but a self-loop is added, as when fixing deadlocks
		// (the state is still marked as a deadlock, as in that case)
		if (chosen.resolution == UndefinedChoicePolicy.Resolution.LOOP) {
			inducedModel.addSelfLoop(sInduced);
			((ModelExplicit<Value>) inducedModel).addDeadlockState(sInduced);
		}
		// Probabilities for the picked choices, if needed: uniform, if all choices are picked
		// for an undefined choice and then merged; or from the decision of a randomised strategy
		boolean all = chosen.resolution == UndefinedChoicePolicy.Resolution.ALL;
		Value allProb = all && !nondet ? uniformProbability(chosen.choices.cardinality()) : null;
		boolean randomised = strat.isRandomised() && chosen.resolution == null;
		int inducedChoice = 0;
		for (InducedChoice ic : groupChoices(s, chosen)) {
			addInducedChoice(s, sInduced, ic, inducedChoice++, chosen.decision, allProb, randomised, succMap);
		}
	}

	/**
	 * Group the choices picked by the strategy into the choices of the induced model, each with an action label.
	 * For a nondeterministic induced model, picked choices with the same induced action label
	 * (see StrategyInfo.getInducedChoiceAction) are merged into one choice (with probabilities,
	 * for a randomised strategy), except that, if all choices are picked for an undefined choice,
	 * they are kept as separate choices. Otherwise, all the picked choices are merged
	 * (picked uniformly at random, if all choices are picked for an undefined choice).
	 */
	private List<InducedChoice> groupChoices(int s, StrategyChoices.Chosen chosen)
	{
		BitSet choices = chosen.choices;
		if (choices.isEmpty()) {
			return Collections.emptyList();
		}
		if (!nondet) {
			return Collections.singletonList(new InducedChoice(choices, null));
		}
		boolean all = chosen.resolution == UndefinedChoicePolicy.Resolution.ALL;
		// Usual case (e.g., a deterministic strategy): a single picked choice
		if (choices.cardinality() == 1) {
			Object act = model.getAction(s, choices.nextSetBit(0));
			return Collections.singletonList(new InducedChoice(choices, all ? act : strat.getInducedChoiceAction(chosen.decision, act, mode)));
		}
		List<InducedChoice> inducedChoices = new ArrayList<>();
		if (all) {
			for (int j = choices.nextSetBit(0); j >= 0; j = choices.nextSetBit(j + 1)) {
				BitSet choice = new BitSet();
				choice.set(j);
				inducedChoices.add(new InducedChoice(choice, model.getAction(s, j)));
			}
		} else {
			Map<Object, BitSet> choicesByAction = new LinkedHashMap<>();
			for (int j = choices.nextSetBit(0); j >= 0; j = choices.nextSetBit(j + 1)) {
				Object act = model.getAction(s, j);
				choicesByAction.computeIfAbsent(strat.getInducedChoiceAction(chosen.decision, act, mode), k -> new BitSet()).set(j);
			}
			for (Map.Entry<Object, BitSet> e : choicesByAction.entrySet()) {
				inducedChoices.add(new InducedChoice(e.getValue(), e.getKey()));
			}
		}
		return inducedChoices;
	}

	/**
	 * Add a choice of the induced model (or, if it is not nondeterministic, its transitions)
	 * for state {@code sInduced}, merging the original model's choices in {@code ic}.
	 * @param s State of the original model
	 * @param sInduced State of the induced model
	 * @param ic The induced choice
	 * @param inducedChoice Index of the induced choice (for transition rewards)
	 * @param decision The decision taken by the strategy
	 * @param allProb Uniform probability for each choice, if needed (null if not)
	 * @param randomised Whether choice probabilities are taken from the (randomised) decision
	 * @param succMap Map from successor states of the original model to induced model states
	 */
	@SuppressWarnings("unchecked")
	private void addInducedChoice(int s, int sInduced, InducedChoice ic, int inducedChoice, Object decision, Value allProb, boolean randomised, SuccessorMap succMap) throws PrismException
	{
		// To build nondeterministic models, store new transitions in a distribution
		Distribution<Value> distr = null;
		Distribution<Interval<Value>> distrIntv = null;
		if (nondet) {
			if (interval) {
				distrIntv = new Distribution<>(intervalEvaluator);
			} else {
				distr = new Distribution<>(model.getEvaluator());
			}
		}
		// Accumulators for transition rewards of chosen choices
		// (transition rewards attach to a whole choice, so choices merged into a single induced
		// choice/state by the strategy need their transition rewards combined too)
		List<Value> transRewAcc = null;
		if (origRewardsList != null) {
			transRewAcc = new ArrayList<>(origRewardsList.size());
			for (int r = 0; r < origRewardsList.size(); r++) {
				transRewAcc.add(origRewardsList.get(r).getEvaluator().zero());
			}
		}
		// Go through the merged choices of the original model
		for (int j = ic.choices.nextSetBit(0); j >= 0; j = ic.choices.nextSetBit(j + 1)) {
			Object act = model.getAction(s, j);
			// Get probability for this choice, if needed
			Value choiceProb = allProb != null ? allProb : randomised ? strat.getChoiceActionProbability(decision, act) : null;
			// Accumulate transition rewards for this choice
			// (weighted by the choice probability, if needed)
			if (transRewAcc != null) {
				for (int r = 0; r < origRewardsList.size(); r++) {
					Rewards<Value> origRews = origRewardsList.get(r);
					if (origRews.hasTransitionRewards()) {
						Evaluator<Value> rewEval = origRews.getEvaluator();
						Value rew = origRews.getTransitionReward(s, j);
						if (choiceProb != null) {
							rew = rewEval.multiply(rew, choiceProb);
						}
						transRewAcc.set(r, rewEval.add(transRewAcc.get(r), rew));
					}
				}
			}
			// Copy the transitions of the original model
			if (interval) {
				Interval<Value> choiceProbIntv = choiceProb == null ? null : new Interval<>(choiceProb, choiceProb);
				copyTransitions(((IMDP<Value>) model).getIntervalTransitionsIterator(s, j), intervalEvaluator, choiceProbIntv, sInduced, act, succMap, distrIntv, transAdderIntv);
			} else if (model instanceof MDP) {
				copyTransitions(((MDP<Value>) model).getTransitionsIterator(s, j), model.getEvaluator(), choiceProb, sInduced, act, succMap, distr, transAdder);
			} else {
				throw new PrismNotSupportedException(description + " not implemented for " + model.getModelType() + "s");
			}
		}
		// Add distribution to nondeterministic model
		if (nondet) {
			if (interval && inducedModel instanceof IMDPSimple) {
				((IMDPSimple<Value>) inducedModel).addActionLabelledChoice(sInduced, distrIntv, ic.action);
			} else if (!interval && inducedModel instanceof MDPSimple) {
				((MDPSimple<Value>) inducedModel).addActionLabelledChoice(sInduced, distr, ic.action);
			} else {
				throw new PrismNotSupportedException(description + " not implemented for " + inducedModelType + "s");
			}
		}
		// Store accumulated transition rewards:
		// for a nondeterministic induced model, attach to the corresponding induced choice for this state;
		// otherwise (induced model has no choices), fold into the induced state reward instead.
		if (transRewAcc != null) {
			for (int r = 0; r < origRewardsList.size(); r++) {
				Value rew = transRewAcc.get(r);
				RewardsSimple<Value> inducedRews = inducedRewardsList.get(r);
				Evaluator<Value> rewEval = origRewardsList.get(r).getEvaluator();
				if (!rewEval.isZero(rew)) {
					if (nondet) {
						inducedRews.setTransitionReward(sInduced, inducedChoice, rew);
					} else {
						inducedRews.addToStateReward(sInduced, rew);
					}
				}
			}
		}
	}

	/**
	 * Copy the transitions of a choice of the original model to the induced model,
	 * scaling their probabilities by {@code choiceProb}, if non-null:
	 * either into the distribution {@code distr} (for a nondeterministic induced model)
	 * or directly into the model, using {@code adder}.
	 * This is generic in the type {@code T} of probabilities (e.g., for intervals).
	 */
	private <T> void copyTransitions(Iterator<Map.Entry<Integer, T>> iter, Evaluator<T> eval, T choiceProb, int sInduced, Object act, SuccessorMap succMap, Distribution<T> distr, TransitionAdder<T> adder) throws PrismException
	{
		if (distr == null && adder == null) {
			throw new PrismNotSupportedException(description + " not implemented for " + inducedModelType + "s");
		}
		while (iter.hasNext()) {
			Map.Entry<Integer, T> e = iter.next();
			T prob = choiceProb == null ? e.getValue() : eval.multiply(e.getValue(), choiceProb);
			int s2 = succMap.apply(act, e.getKey());
			if (distr != null) {
				distr.add(s2, prob);
			} else {
				adder.add(sInduced, s2, prob, act);
			}
		}
	}

	/**
	 * Get the uniform probability 1/n
	 */
	private Value uniformProbability(int n)
	{
		Evaluator<Value> eval = model.getEvaluator();
		Value num = eval.zero();
		for (int j = 0; j < n; j++) {
			num = eval.add(num, eval.one());
		}
		return eval.divide(eval.one(), num);
	}

	/**
	 * Finish the construction of the induced model (deadlocks, states list, labels, rewards)
	 * and return it.
	 * @param statesList States list for the induced model (null if none)
	 */
	Model<Value> finish(List<State> statesList) throws PrismException
	{
		inducedModel.findDeadlocks(false);
		if (statesList != null) {
			inducedModel.setStatesList(statesList);
		}
		if (inducedLabels != null) {
			for (Map.Entry<String, BitSet> e : inducedLabels.entrySet()) {
				inducedModel.addLabel(e.getKey(), e.getValue());
			}
		}
		if (inducedRewardsList != null) {
			for (int r = 0; r < inducedRewardsList.size(); r++) {
				inducedModel.addRewards(model.getRewardName(r), model.getRewardPosition(r), inducedRewardsList.get(r));
			}
		}
		return inducedModel;
	}
}
