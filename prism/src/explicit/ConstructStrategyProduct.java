//==============================================================================
//	
//	Copyright (c) 2022-
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

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import common.Interval;
import explicit.rewards.Rewards;
import explicit.rewards.RewardsSimple;
import parser.State;
import parser.VarList;
import parser.ast.Declaration;
import parser.ast.DeclarationInt;
import parser.ast.Expression;
import prism.Evaluator;
import prism.ModelType;
import prism.PrismException;
import prism.PrismNotSupportedException;
import strat.Strategy;
import strat.StrategyChoices;
import strat.StrategyExportOptions;
import strat.UndefinedChoicePolicy;

/**
 * Construct the product model induced by a finite-memory strategy on a nondeterministic model
 */
public class ConstructStrategyProduct
{
	/**
	 * The "mode" of construction:
	 * "restrict" (same model type but restrict to selected action choices); or
	 * "reduce" (change mode type by removing nondeterminism)
	 */
	private StrategyExportOptions.InducedModelMode mode = StrategyExportOptions.InducedModelMode.RESTRICT;

	/**
	 * Whether to copy labels from the original model to the product model
	 */
	private boolean copyLabels = true;

	/**
	 * Whether to copy rewards from the original model to the product model
	 */
	private boolean copyRewards = true;

	/**
	 * Policy for resolving undefined strategy choices
	 */
	private UndefinedChoicePolicy undefinedChoicePolicy = new UndefinedChoicePolicy();

	/**
	 * Construct a ConstructStrategyProduct with default options.
	 */
	public ConstructStrategyProduct()
	{
	}

	/**
	 * Construct a ConstructStrategyProduct, configured from the mode/labels/rewards
	 * settings of the passed in {@link StrategyExportOptions}.
	 */
	public ConstructStrategyProduct(StrategyExportOptions options)
	{
		setMode(options.getMode());
		setCopyLabels(options.getInducedModelExportOptions().getShowLabels());
		setCopyRewards(options.getInducedModelExportOptions().getShowRewards());
		setUndefinedChoicePolicy(options.getUndefinedChoicePolicy());
	}

	/**
	 * Set the "mode" of construction:
	 * "restrict" (same model type but restrict to selected action choices); or
	 * "reduce" (change mode type by removing nondeterminism)
	 */
	public ConstructStrategyProduct setMode(StrategyExportOptions.InducedModelMode mode)
	{
		this.mode = mode;
		return this;
	}

	/**
	 * Set whether to copy labels from the original model to the product model,
	 * restricted/reindexed to the product model's states.
	 */
	public ConstructStrategyProduct setCopyLabels(boolean copyLabels)
	{
		this.copyLabels = copyLabels;
		return this;
	}

	/**
	 * Set whether to copy rewards from the original model to the product model,
	 * restricted/reindexed to the product model's states.
	 */
	public ConstructStrategyProduct setCopyRewards(boolean copyRewards)
	{
		this.copyRewards = copyRewards;
		return this;
	}

	/**
	 * Set the policy for resolving undefined strategy choices.
	 */
	public ConstructStrategyProduct setUndefinedChoicePolicy(UndefinedChoicePolicy undefinedChoicePolicy)
	{
		this.undefinedChoicePolicy = undefinedChoicePolicy;
		return this;
	}

	/**
	 * Construct the product model induced by a finite-memory strategy on a nondeterministic model
	 * @param model The model
	 * @param strat The strategy
	 * @return The product model
	 */
	@SuppressWarnings("unchecked")
	public <Value> Model<Value> constructProductModel(NondetModel<Value> model, Strategy<Value> strat) throws PrismException
	{
		// This is for finite-memory strategies
		if (!strat.hasMemory()) {
			throw new PrismException("Product construction is for finite-memory strategies");
		}
		
		// If the model has a VarList, we will create a new one
		VarList newVarList = null;
		if (model.getVarList() != null) {
			VarList varList = model.getVarList();
			// Create a (new, unique) name for the variable that will represent memory states
			String memVar = "_mem";
			while (varList.getIndex(memVar) != -1) {
				memVar = "_" + memVar;
			}
			newVarList = (VarList) varList.clone();
			// NB: if the strategy has only one memory state, we add an extra dummy state
			Declaration decl = new Declaration(memVar, new DeclarationInt(Expression.Int(0), Expression.Int(Math.max(strat.getMemorySize() - 1, 1))));
			newVarList.addVarAtStart(decl, 1);
		}

		// Determine type of induced model
		ModelType modelType = model.getModelType();
		ModelType productModelType = strat.getInducedModelType(mode);
		if (productModelType == null) {
			throw new PrismNotSupportedException("Product construction not supported for " + modelType + "s");
		}

		// Create a (simple, mutable) model of the appropriate type
		ModelSimple<Value> prodModel = (ModelSimple<Value>) ModelSimple.forModelType(productModelType);

		// Attach evaluator and variable info
		((ModelExplicit<Value>) prodModel).setEvaluator(model.getEvaluator());
		if (prodModel instanceof IntervalModelExplicit) {
			((IntervalModelExplicit<Value>) prodModel).setIntervalEvaluator(((IntervalModel<Value>) model).getIntervalEvaluator());
		}
		((ModelExplicit<Value>) prodModel).setVarList(newVarList);

		// Now do the actual product model construction
		return doConstructProductModel(modelType, productModelType, prodModel, model, strat);
	}
	
	/**
	 * Do the main part of the construction of the product model induced
	 * by a finite-memory strategy on a nondeterministic model,
	 * inserting states and transitions into the provided ModelSimple object.
	 * @param modelType The type of the original model
	 * @param productModelType The type of the product model
	 * @param prodModel The (empty) product model
	 * @param model The model
	 * @param strat The strategy
	 * @return The product model
	 */
	@SuppressWarnings("unchecked")
	public <Value> Model<Value> doConstructProductModel(ModelType modelType, ModelType productModelType, ModelSimple<Value> prodModel, NondetModel<Value> model, Strategy<Value> strat) throws PrismException
	{
		int modelNumStates = model.getNumStates();
		int memSize = strat.getMemorySize();
		int prodNumStates;
		List<State> prodStatesList, memStatesList;

		// Check size limits for this product construction approach
		try {
			prodNumStates = Math.multiplyExact(modelNumStates, memSize);
		} catch (ArithmeticException e) {
			throw new PrismException("Size of product state space of model and strategy is too large for explicit engine");
		}

		// Create empty label bitsets for the product model, if needed
		Map<String, BitSet> origLabels = null;
		Map<String, BitSet> inducedLabels = null;
		if (copyLabels) {
			origLabels = model.getLabelToStatesMap();
			inducedLabels = new LinkedHashMap<>();
			for (String name : origLabels.keySet()) {
				inducedLabels.put(name, new BitSet());
			}
		}

		// Create empty reward structures for the product model, if needed
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

		// Encoding:
		// each state s' = <s, q> = s * memSize + q
		// s(s') = s' / memSize
		// q(s') = s' % memSize

		// Initialise state info storage
		LinkedList<Point> queue = new LinkedList<Point>();
		BitSet visited = new BitSet(prodNumStates);
		int map[] = new int[prodNumStates];
		Arrays.fill(map, -1);
		if (model.getStatesList() != null) {
			prodStatesList = new ArrayList<State>();
			memStatesList = new ArrayList<State>(memSize);
			for (int i = 0; i < memSize; i++) {
				memStatesList.add(new State(1).setValue(0, i));
			}
		} else {
			prodStatesList = null;
			memStatesList = null;
		}

		// Code to get the index for a new product state for model successor state s_2
		// (or initial state s_2 if init==true)
		// assuming the current memory status is q_1 and the action taken is act
		// (q_1 and act are ignored if init==true)
		NewStateMap newStateMap = (init, q_1, act, s_2) -> {
			// Find corresponding memory value
			int q_2 = -1;
			if (init) {
				q_2 = strat.getInitialMemory(s_2);
			} else {
				q_2 = strat.getUpdatedMemory(q_1, act, s_2);
			}
			if (q_2 < 0) {
				throw new PrismException("The memory status is unknown (state " + s_2 + ")");
			}
			// Add state/transition to model
			if (!visited.get(s_2 * memSize + q_2) && map[s_2 * memSize + q_2] == -1) {
				queue.add(new Point(s_2, q_2));
				switch (productModelType) {
					case STPG:
						((STPGSimple<Value>) prodModel).addState(((STPG<Value>) model).getPlayer(s_2));
						break;
					default:
						prodModel.addState();
						break;
				}
				map[s_2 * memSize + q_2] = prodModel.getNumStates() - 1;
				if (prodStatesList != null) {
					// Store state information for the product
					// (memory first, since the memory variable is added at the start of the VarList)
					prodStatesList.add(new State(memStatesList.get(q_2), model.getStatesList().get(s_2)));
				}
			}
			return map[s_2 * memSize + q_2];
		};

		// Get initial states
		for (int s_0 : model.getInitialStates()) {
			int map_0 = newStateMap.apply(true, -1, null, s_0);
			prodModel.addInitialState(map_0);
		}

		// Explore product
		while (!queue.isEmpty()) {
			Point p = queue.pop();
			int s_1 = p.x;
			int q_1 = p.y;
			visited.set(s_1 * memSize + q_1);
			int map_1 = map[s_1 * memSize + q_1];

			if (copyLabels) {
				for (Map.Entry<String, BitSet> e : origLabels.entrySet()) {
					if (e.getValue().get(s_1)) {
						inducedLabels.get(e.getKey()).set(map_1);
					}
				}
			}
			// Copy state rewards
			if (copyRewards) {
				for (int r = 0; r < origRewardsList.size(); r++) {
					Rewards<Value> origRews = origRewardsList.get(r);
					if (origRews.hasStateRewards()) {
						inducedRewardsList.get(r).setStateReward(map_1, origRews.getStateReward(s_1));
					}
				}
			}

			int numChoices =  model.getNumChoices(s_1);
			// Find the choices picked by the strategy (resolving any undefined choice according to the policy)
			// and the decision used to pick them (used below for probabilities and induced action labels)
			StrategyChoices.Chosen chosen = StrategyChoices.getChosenChoices(model, strat, s_1, q_1, undefinedChoicePolicy);
			Object decision = chosen.decision;
			// Group the picked choices into the choices of the product model, each with an action label.
			// For a nondeterministic product model, picked choices with the same induced action label
			// (see StrategyInfo.getInducedChoiceAction) are merged into one choice (with probabilities,
			// for a randomised strategy), except that, if all choices are picked for an undefined choice,
			// they are kept as separate choices. Otherwise, all the picked choices are merged
			// (picked uniformly at random, if all choices are picked for an undefined choice).
			boolean all = chosen.resolution == UndefinedChoicePolicy.Resolution.ALL;
			List<BitSet> groups = new ArrayList<>();
			List<Object> groupActions = new ArrayList<>();
			if (productModelType.nondeterministic()) {
				Map<Object, BitSet> groupsByAction = new LinkedHashMap<>();
				for (int j = chosen.choices.nextSetBit(0); j >= 0; j = chosen.choices.nextSetBit(j + 1)) {
					Object act = model.getAction(s_1, j);
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
			if (all && !productModelType.nondeterministic()) {
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
				prodModel.addSelfLoop(map_1);
				((ModelExplicit<Value>) prodModel).addDeadlockState(map_1);
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
				if (productModelType.nondeterministic()) {
					if (modelType != ModelType.IMDP) {
						prodDistr = new Distribution<>(model.getEvaluator());
					} else {
						prodDistrIntv = new Distribution<>(((IMDP<Value>) model).getIntervalEvaluator());
					}
				}
				// Go through choices from state s_1 in original model
				for (int j = 0; j < numChoices; j++) {
					Object act = model.getAction(s_1, j);
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
								Value rew = origRews.getTransitionReward(s_1, j);
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
						iter = ((MDP<Value>) model).getTransitionsIterator(s_1, j);
						break;
					case POMDP:
						iter = ((POMDP<Value>) model).getTransitionsIterator(s_1, j);
						break;
					case IMDP:
						iterIntv = ((IMDP<Value>) model).getIntervalTransitionsIterator(s_1, j);
						break;
					case STPG:
						iter = ((STPG<Value>) model).getTransitionsIterator(s_1, j);
						break;
					default:
						throw new PrismNotSupportedException("Product construction not implemented for " + modelType + "s");
					}
					if (modelType != ModelType.IMDP) {
						while (iter.hasNext()) {
							Map.Entry<Integer, Value> e = iter.next();
							int s_2 = e.getKey();
							Value prob = e.getValue();
							if (choiceProb != null) {
								prob = model.getEvaluator().multiply(prob, choiceProb);
							}
							int map_2 = newStateMap.apply(false, q_1, act, s_2);

							switch (productModelType) {
								case DTMC:
									((DTMCSimple<Value>) prodModel).addToProbability(map_1, map_2, prob, act);
									break;
								case MDP:
								case POMDP:
								case STPG:
									prodDistr.add(map_2, prob);
									break;
								default:
									throw new PrismNotSupportedException("Product construction not implemented for " + modelType + "s");
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
							int map_2 = newStateMap.apply(false, q_1, act, s_2);
							// Add transition to model
							switch (productModelType) {
								case IDTMC:
									((IDTMCSimple<Value>) prodModel).addToProbability(map_1, map_2, prob, act);
									break;
								case IMDP:
									prodDistrIntv.add(map_2, prob);
									break;
								default:
									throw new PrismNotSupportedException("Induced model construction not implemented for " + modelType + "s");
							}
						}
					}
				}
				// Add distribution to nondeterministic model
				if (productModelType.nondeterministic()) {
					switch (productModelType) {
					case MDP:
						((MDPSimple<Value>) prodModel).addActionLabelledChoice(map_1, prodDistr, inducedAction);
						break;
					case IMDP:
						((IMDPSimple<Value>) prodModel).addActionLabelledChoice(map_1, prodDistrIntv, inducedAction);
						break;
					case POMDP:
						((POMDPSimple<Value>) prodModel).addActionLabelledChoice(map_1, prodDistr, inducedAction);
						break;
					case STPG:
						((STPGSimple<Value>) prodModel).addActionLabelledChoice(map_1, prodDistr, inducedAction);
						break;
					default:
						break;
					}
				}
				// Store accumulated transition rewards:
				// for a nondeterministic product model, attach to the corresponding induced choice for this state;
				// otherwise (product model has no choices), fold into the induced state reward instead.
				if (copyRewards) {
					for (int r = 0; r < origRewardsList.size(); r++) {
						Value rew = transRewAcc.get(r);
						RewardsSimple<Value> inducedRews = inducedRewardsList.get(r);
						Evaluator<Value> rewEval = origRewardsList.get(r).getEvaluator();
						if (!rewEval.isZero(rew)) {
							if (productModelType.nondeterministic()) {
								inducedRews.setTransitionReward(map_1, inducedChoice, rew);
							} else {
								inducedRews.addToStateReward(map_1, rew);
							}
						}
					}
				}
				inducedChoice++;
			}
		}

		prodModel.findDeadlocks(false);

		if (prodStatesList != null) {
			prodModel.setStatesList(prodStatesList);
		}

		if (copyLabels) {
			for (Map.Entry<String, BitSet> e : inducedLabels.entrySet()) {
				prodModel.addLabel(e.getKey(), e.getValue());
			}
		}

		if (copyRewards) {
			for (int r = 0; r < origRewardsList.size(); r++) {
				prodModel.addRewards(model.getRewardName(r), model.getRewardPosition(r), inducedRewardsList.get(r));
			}
		}

		return prodModel;
	}

	@FunctionalInterface
	interface NewStateMap
	{
		int apply(boolean init, int q_1, Object act, int s_2) throws PrismException;
	};
}
