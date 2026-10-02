//==============================================================================
//
//	Copyright (c) 2023-
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
import strat.StrategyExportOptions;
import strat.StrategyExportOptions.InducedModelMode;
import strat.UndefinedChoicePolicy;

import java.util.*;

/**
 * Construct the model induced by a memoryless deterministic strategy on a nondeterministic model
 */
public class ConstructInducedModel
{
	/**
	 * The "mode" of construction:
	 * "restrict" (same model type but restrict to selected action choices); or
	 * "reduce" (change mode type by removing nondeterminism)
	 */
	private InducedModelMode mode = InducedModelMode.RESTRICT;

	/**
	 * Whether to restrict strategy/model to reachable states
	 */
	private boolean reachOnly = true;

	/**
	 * Whether to copy labels from the original model to the induced model
	 */
	private boolean copyLabels = true;

	/**
	 * Whether to copy rewards from the original model to the induced model
	 */
	private boolean copyRewards = true;

	/**
	 * Policy for resolving undefined strategy choices
	 */
	private UndefinedChoicePolicy undefinedChoicePolicy = new UndefinedChoicePolicy();

	/**
	 * Construct a ConstructInducedModel with default options.
	 */
	public ConstructInducedModel()
	{
	}

	/**
	 * Construct a ConstructInducedModel, configured from the mode/reachOnly/labels/rewards
	 * settings of the passed in {@link StrategyExportOptions}.
	 */
	public ConstructInducedModel(StrategyExportOptions options)
	{
		setMode(options.getMode());
		setReachOnly(options.getReachOnly());
		setCopyLabels(options.getInducedModelExportOptions().getShowLabels());
		setCopyRewards(options.getInducedModelExportOptions().getShowRewards());
		setUndefinedChoicePolicy(options.getUndefinedChoicePolicy());
	}

	/**
	 * Set the "mode" of construction:
	 * "restrict" (same model type but restrict to selected action choices); or
	 * "reduce" (change mode type by removing nondeterminism)
	 */
	public ConstructInducedModel setMode(InducedModelMode mode)
	{
		this.mode = mode;
		return this;
	}

	/**
	 * Set whether to restrict strategy/model to reachable states
	 */
	public ConstructInducedModel setReachOnly(boolean reachOnly)
	{
		this.reachOnly = reachOnly;
		return this;
	}

	/**
	 * Set whether to copy labels from the original model to the induced model,
	 * restricted/reindexed to the induced model's states.
	 */
	public ConstructInducedModel setCopyLabels(boolean copyLabels)
	{
		this.copyLabels = copyLabels;
		return this;
	}

	/**
	 * Set whether to copy rewards from the original model to the induced model,
	 * restricted/reindexed to the induced model's states.
	 */
	public ConstructInducedModel setCopyRewards(boolean copyRewards)
	{
		this.copyRewards = copyRewards;
		return this;
	}

	/**
	 * Set the policy for resolving undefined strategy choices.
	 */
	public ConstructInducedModel setUndefinedChoicePolicy(UndefinedChoicePolicy undefinedChoicePolicy)
	{
		this.undefinedChoicePolicy = undefinedChoicePolicy;
		return this;
	}

	/**
	 * Construct the model induced by a memoryless deterministic strategy on a nondeterministic model
	 * @param model The model
	 * @param strat The strategy
	 * @return The induced model
	 */
	@SuppressWarnings("unchecked")
	public <Value> Model<Value> constructInducedModel(NondetModel<Value> model, Strategy<Value> strat) throws PrismException
	{
		// This is for memoryless strategies
		if (strat.hasMemory()) {
			throw new PrismException("Induced model construction is for memoryless strategies");
		}

		// Determine type of induced model
		ModelType modelType = model.getModelType();
		ModelType inducedModelType = strat.getInducedModelType(mode);
		if (inducedModelType == null) {
			throw new PrismNotSupportedException("Induced model construction not supported for " + modelType + "s");
		}

		// Create a (simple, mutable) model of the appropriate type
		ModelSimple<Value> inducedModel = (ModelSimple<Value>) ModelSimple.forModelType(inducedModelType);
		
		// Attach evaluator and copy variable info
		((ModelExplicit<Value>) inducedModel).setEvaluator(model.getEvaluator());
		if (inducedModel instanceof IntervalModelExplicit) {
			((IntervalModelExplicit<Value>) inducedModel).setIntervalEvaluator(((IntervalModel<Value>) model).getIntervalEvaluator());
		}
		((ModelExplicit<Value>) inducedModel).setVarList(model.getVarList());

		// Now do the actual induced model construction
		return doConstructInducedModel(modelType, inducedModelType, inducedModel, model, strat);
	}

	/**
	 * Do the main part of the construction of the model induced
	 * by a memoryless deterministic strategy on a nondeterministic model,
	 * inserting states and transitions into the provided ModelSimple object.
	 * @param modelType The type of the original model
	 * @param inducedModelType The type of the induced model
	 * @param inducedModel The (empty) induced model
	 * @param model The model
	 * @param strat The strategy
	 * @return The product model
	 */
	@SuppressWarnings("unchecked")
	public <Value> Model<Value> doConstructInducedModel(ModelType modelType, ModelType inducedModelType, ModelSimple<Value> inducedModel, NondetModel<Value> model, Strategy<Value> strat) throws PrismException
	{
		// Create new states list if needed
		List<State> inducedStatesList = model.getStatesList();
		if (reachOnly && inducedStatesList != null) {
			inducedStatesList = new ArrayList<>();
		}

		// Create empty label bitsets for the induced model, if needed
		Map<String, BitSet> origLabels = null;
		Map<String, BitSet> inducedLabels = null;
		if (copyLabels) {
			origLabels = model.getLabelToStatesMap();
			inducedLabels = new LinkedHashMap<>();
			for (String name : origLabels.keySet()) {
				inducedLabels.put(name, new BitSet());
			}
		}

		// Create empty reward structures for the induced model, if needed
		List<Rewards<Value>> origRewardsList = null;
		List<RewardsSimple<Value>> inducedRewardsList = null;
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

		// Initially create an array with 0s for reachable state indices and -1s for unreachable ones
		int numStates = model.getNumStates();
		int[] map = new int[numStates];
		if (reachOnly) {
			Arrays.fill(map, -1);
			BitSet explore = new BitSet();
			// Get initial states
			for (int is : model.getInitialStates()) {
				map[is] = 0;
				explore.set(is);
			}

			// Compute reachable states (and store 0s in map)
			while (!explore.isEmpty()) {
				for (int s = explore.nextSetBit(0); s >= 0; s = explore.nextSetBit(s + 1)) {
					explore.set(s, false);
					int numChoices =  model.getNumChoices(s);
					// Find the choices picked by the strategy
					BitSet chosen = StrategyChoices.getChosenChoices(model, strat, s, -1, undefinedChoicePolicy).choices;
					// Go through transitions from state s in original model
					for (int j = 0; j < numChoices; j++) {
						// Skip choices not picked by the strategy
						if (!chosen.get(j)) {
							continue;
						}
						for (Iterator<Integer> it = model.getSuccessorsIterator(s, j); it.hasNext(); ) {
							int dest = it.next();
							if (map[dest] == -1) {
								map[dest] = 0;
								explore.set(dest);
							}
						}
					}
				}
			}
			// Then populate map with indices
			int count = 0;
			for (int s = 0; s < numStates; s++) {
				if (map[s] != -1) {
					map[s] = count++;
				}
			}
		} else {
			// Skip reachability
			for (int s = 0; s < numStates; s++) {
				map[s] = s;
			}
		}

		// Iterate through reachable states to create new model
		for (int s = 0; s < numStates; s++) {
			if (map[s] == -1) {
				continue;
			}

			// Add state to model
			switch (inducedModelType) {
				case STPG:
					((STPGSimple<Value>) inducedModel).addState(((STPG<Value>) model).getPlayer(s));
					break;
				default:
					inducedModel.addState();
					break;
			}
			if (model.isInitialState(s)) {
				inducedModel.addInitialState(map[s]);
			}
			if (reachOnly && inducedStatesList != null) {
				inducedStatesList.add(model.getStatesList().get(s));
			}
			if (copyLabels) {
				for (Map.Entry<String, BitSet> e : origLabels.entrySet()) {
					if (e.getValue().get(s)) {
						inducedLabels.get(e.getKey()).set(map[s]);
					}
				}
			}
			// Copy state rewards
			if (copyRewards) {
				for (int r = 0; r < origRewardsList.size(); r++) {
					Rewards<Value> origRews = origRewardsList.get(r);
					if (origRews.hasStateRewards()) {
						inducedRewardsList.get(r).setStateReward(map[s], origRews.getStateReward(s));
					}
				}
			}

			int numChoices =  model.getNumChoices(s);
			// Find the choices picked by the strategy (resolving any undefined choice according to the policy)
			// and the decision used to pick them (used below for probabilities and induced action labels)
			StrategyChoices.Chosen chosen = StrategyChoices.getChosenChoices(model, strat, s, -1, undefinedChoicePolicy);
			Object decision = chosen.decision;
			// Group the picked choices into the choices of the induced model, each with an action label.
			// For a nondeterministic induced model, picked choices with the same induced action label
			// (see StrategyInfo.getInducedChoiceAction) are merged into one choice (with probabilities,
			// for a randomised strategy), except that, if all choices are picked for an undefined choice,
			// they are kept as separate choices. Otherwise, all the picked choices are merged
			// (picked uniformly at random, if all choices are picked for an undefined choice).
			boolean all = chosen.resolution == UndefinedChoicePolicy.Resolution.ALL;
			List<BitSet> groups = new ArrayList<>();
			List<Object> groupActions = new ArrayList<>();
			if (inducedModelType.nondeterministic()) {
				Map<Object, BitSet> groupsByAction = new LinkedHashMap<>();
				for (int j = chosen.choices.nextSetBit(0); j >= 0; j = chosen.choices.nextSetBit(j + 1)) {
					Object act = model.getAction(s, j);
					if (all) {
						BitSet group = new BitSet();
						group.set(j);
						groups.add(group);
						groupActions.add(act);
					} else {
						groupsByAction.computeIfAbsent(strat.getInducedChoiceAction(decision, act, mode), k -> new BitSet()).set(j);
					}
				}
				for (Map.Entry<Object, BitSet> e : groupsByAction.entrySet()) {
					groups.add(e.getValue());
					groupActions.add(e.getKey());
				}
			} else if (!chosen.choices.isEmpty()) {
				groups.add(chosen.choices);
				groupActions.add(null);
			}
			Value allProb = null;
			if (all && !inducedModelType.nondeterministic()) {
				Evaluator<Value> eval = model.getEvaluator();
				Value numChosen = eval.zero();
				for (int j = 0; j < chosen.choices.cardinality(); j++) {
					numChosen = eval.add(numChosen, eval.one());
				}
				allProb = eval.divide(eval.one(), numChosen);
			}
			// For STOP, no choices are picked, so nothing is added: the state has no outgoing
			// transitions/choices, and is marked as a deadlock when deadlocks are found at the end.
			// For LOOP, no choices are picked, but a self-loop is added, as when fixing deadlocks
			// (the state is still marked as a deadlock, as in that case)
			if (chosen.resolution == UndefinedChoicePolicy.Resolution.LOOP) {
				inducedModel.addSelfLoop(map[s]);
				((ModelExplicit<Value>) inducedModel).addDeadlockState(map[s]);
			}
			int inducedChoice = 0;
			for (int g = 0; g < groups.size(); g++) {
				BitSet group = groups.get(g);
				// Accumulators for transition rewards of chosen choices
				// (transition rewards attach to a whole choice, so choices merged into a single induced
				// choice/state by the strategy need their transition rewards combined too)
				List<Value> transRewAcc = null;
				if (copyRewards) {
					transRewAcc = new ArrayList<>(origRewardsList.size());
					for (int r = 0; r < origRewardsList.size(); r++) {
						transRewAcc.add(origRewardsList.get(r).getEvaluator().zero());
					}
				}
				// To build nondeterministic models, store new transitions in a distribution
				Object inducedAction = groupActions.get(g);
				Distribution<Value> prodDistr = null;
				Distribution<Interval<Value>> prodDistrIntv = null;
				if (inducedModelType.nondeterministic()) {
					if (modelType != ModelType.IMDP) {
						prodDistr = new Distribution<>(model.getEvaluator());
					} else {
						prodDistrIntv = new Distribution<>(((IMDP<Value>) model).getIntervalEvaluator());
					}
				}
				// Go through choices from state s in original model
				for (int j = 0; j < numChoices; j++) {
					Object act = model.getAction(s, j);
					// Skip choices not picked by the strategy
					if (!group.get(j)) {
						continue;
					}
					// Get probability for this choice, if needed (uniform for all choices; or from a randomised strategy)
					Value choiceProb = null;
					if (allProb != null) {
						choiceProb = allProb;
					} else if (strat.isRandomised() && chosen.resolution == null) {
						choiceProb = strat.getChoiceActionProbability(decision, act);
					}
					// Accumulate transition rewards for this choice
					// (weighted by the choice probability, if needed)
					if (copyRewards) {
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
					// Go through transitions of original model
					Iterator<Map.Entry<Integer, Value>> iter = null;
					Iterator<Map.Entry<Integer, Interval<Value>>> iterIntv = null;
					switch (modelType) {
						case MDP:
							iter = ((MDP<Value>) model).getTransitionsIterator(s, j);
							break;
						case POMDP:
							iter = ((POMDP<Value>) model).getTransitionsIterator(s, j);
							break;
						case IMDP:
							iterIntv = ((IMDP<Value>) model).getIntervalTransitionsIterator(s, j);
							break;
						case STPG:
							iter = ((STPG<Value>) model).getTransitionsIterator(s, j);
							break;
						default:
							throw new PrismNotSupportedException("Induced model construction not implemented for " + modelType + "s");
					}
					if (modelType != ModelType.IMDP) {
						while (iter.hasNext()) {
							Map.Entry<Integer, Value> e = iter.next();
							int s_2 = e.getKey();
							Value prob = e.getValue();
							if (choiceProb != null) {
								prob = model.getEvaluator().multiply(prob, choiceProb);
							}
							// Add transition to model
							switch (inducedModelType) {
								case DTMC:
									((DTMCSimple<Value>) inducedModel).addToProbability(map[s], map[s_2], prob, act);
									break;
								case MDP:
								case POMDP:
								case STPG:
									prodDistr.add(map[s_2], prob);
									break;
								default:
									throw new PrismNotSupportedException("Induced model construction not implemented for " + modelType + "s");
							}
						}
					} else {
						while (iterIntv.hasNext()) {
							Map.Entry<Integer, Interval<Value>> e = iterIntv.next();
							int s_2 = e.getKey();
							Interval<Value> prob = e.getValue();
							if (choiceProb != null) {
								prob = ((IMDP<Value>) model).getIntervalEvaluator().multiply(prob, new Interval<>(choiceProb, choiceProb));
							}
							// Add transition to model
							switch (inducedModelType) {
								case IDTMC:
									((IDTMCSimple<Value>) inducedModel).addToProbability(map[s], map[s_2], prob, act);
									break;
								case IMDP:
									prodDistrIntv.add(map[s_2], prob);
									break;
								default:
									throw new PrismNotSupportedException("Induced model construction not implemented for " + modelType + "s");
							}
						}
					}
				}
				// Add distribution to nondeterministic model
				if (inducedModelType.nondeterministic()) {
					switch (inducedModelType) {
						case MDP:
							((MDPSimple<Value>) inducedModel).addActionLabelledChoice(map[s], prodDistr, inducedAction);
							break;
						case POMDP:
							((POMDPSimple<Value>) inducedModel).addActionLabelledChoice(map[s], prodDistr, inducedAction);
							break;
						case IMDP:
							((IMDPSimple<Value>) inducedModel).addActionLabelledChoice(map[s], prodDistrIntv, inducedAction);
							break;
						case STPG:
							((STPGSimple<Value>) inducedModel).addActionLabelledChoice(map[s], prodDistr, inducedAction);
							break;
						default:
							break;
					}
				}
				// Store accumulated transition rewards:
				// for a nondeterministic induced model, attach to the corresponding induced choice for this state;
				// otherwise (induced model has no choices), fold into the induced state reward instead.
				if (copyRewards) {
					for (int r = 0; r < origRewardsList.size(); r++) {
						Value rew = transRewAcc.get(r);
						RewardsSimple<Value> inducedRews = inducedRewardsList.get(r);
						Evaluator<Value> rewEval = origRewardsList.get(r).getEvaluator();
						if (!rewEval.isZero(rew)) {
							if (inducedModelType.nondeterministic()) {
								inducedRews.setTransitionReward(map[s], inducedChoice, rew);
							} else {
								inducedRews.addToStateReward(map[s], rew);
							}
						}
					}
				}
				inducedChoice++;
			}
		}

		inducedModel.findDeadlocks(false);

		if (inducedStatesList != null) {
			inducedModel.setStatesList(inducedStatesList);
		}

		if (copyLabels) {
			for (Map.Entry<String, BitSet> e : inducedLabels.entrySet()) {
				inducedModel.addLabel(e.getKey(), e.getValue());
			}
		}

		if (copyRewards) {
			for (int r = 0; r < origRewardsList.size(); r++) {
				inducedModel.addRewards(model.getRewardName(r), model.getRewardPosition(r), inducedRewardsList.get(r));
			}
		}

		return inducedModel;
	}
}
