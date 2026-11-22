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

import parser.State;
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
	 * States of the original model to start from (null: its initial states)
	 */
	private BitSet initialStates = null;

	/**
	 * Whether to store, for each state of the induced model, the corresponding original state
	 */
	private boolean storeStateMap = false;

	/**
	 * For the last induced model constructed (if storeStateMap is true):
	 * the original state for each induced state
	 */
	private int[] origStates = null;

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
		setInitialStates(options.getInitialStates());
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
	 * Set the states of the original model from which to construct the induced model
	 * (which become its initial states), instead of the model's initial states.
	 * Null means the initial states of the original model (the default).
	 */
	public ConstructInducedModel setInitialStates(BitSet initialStates)
	{
		this.initialStates = initialStates;
		return this;
	}

	/**
	 * Set whether to store, for each state of the induced model, the corresponding state
	 * of the original model, available afterwards via {@link #getOriginalState(int)}.
	 * This is off by default; it needs one int per induced state.
	 */
	public ConstructInducedModel setStoreStateMap(boolean storeStateMap)
	{
		this.storeStateMap = storeStateMap;
		return this;
	}

	/**
	 * Get the state of the original model corresponding to state {@code inducedState}
	 * of the last induced model constructed. This needs {@link #setStoreStateMap(boolean)}
	 * to have been set to true before construction.
	 */
	public int getOriginalState(int inducedState)
	{
		if (origStates == null) {
			throw new IllegalStateException("State map not stored for induced model");
		}
		return origStates[inducedState];
	}

	/**
	 * Get the map from states of the last induced model constructed to states of the original
	 * model, or null if it was not stored (see {@link #setStoreStateMap(boolean)}).
	 */
	public InducedStateMap getStateMap()
	{
		return origStates == null ? null : new InducedStateMap(origStates, null);
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
		InducedModelBuilder<Value> builder = new InducedModelBuilder<>(model, strat, inducedModel, inducedModelType, mode, undefinedChoicePolicy, copyLabels, copyRewards, "Induced model construction");
		origStates = null;

		// Create new states list if needed
		List<State> inducedStatesList = model.getStatesList();
		if (reachOnly && inducedStatesList != null) {
			inducedStatesList = new ArrayList<>();
		}

		// Initially create an array with 0s for reachable state indices and -1s for unreachable ones
		int numStates = model.getNumStates();
		int[] map = new int[numStates];
		int numInducedStates = numStates;
		if (reachOnly) {
			Arrays.fill(map, -1);
			BitSet explore = new BitSet();
			// Get initial states
			for (int is : getInitialStates(model)) {
				map[is] = 0;
				explore.set(is);
			}

			// Compute reachable states (and store 0s in map)
			while (!explore.isEmpty()) {
				for (int s = explore.nextSetBit(0); s >= 0; s = explore.nextSetBit(s + 1)) {
					explore.set(s, false);
					// Find the choices picked by the strategy
					BitSet chosen = StrategyChoices.getChosenChoices(model, strat, s, -1, undefinedChoicePolicy).choices;
					// Go through transitions of those choices from state s in original model
					for (int j = chosen.nextSetBit(0); j >= 0; j = chosen.nextSetBit(j + 1)) {
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
			numInducedStates = count;
		} else {
			// Skip reachability
			for (int s = 0; s < numStates; s++) {
				map[s] = s;
			}
		}

		// Store the original state of each induced state, if required
		// (the number of induced states is known by now)
		if (storeStateMap) {
			builder.setStoreStateMap(false, numInducedStates);
		}

		// Iterate through reachable states to create new model
		for (int s = 0; s < numStates; s++) {
			if (map[s] == -1) {
				continue;
			}
			// Add state to model, with its labels/rewards and the choices/transitions picked by the strategy
			builder.addState(s);
			if (initialStates == null ? model.isInitialState(s) : initialStates.get(s)) {
				inducedModel.addInitialState(map[s]);
			}
			if (reachOnly && inducedStatesList != null) {
				inducedStatesList.add(model.getStatesList().get(s));
			}
			builder.copyStateInfo(s, map[s]);
			builder.addChoices(s, -1, map[s], (act, s2) -> map[s2]);
		}

		Model<Value> res = builder.finish(inducedStatesList);
		origStates = builder.getOriginalStates();
		return res;
	}

	/**
	 * Get the states of the original model to start construction from.
	 */
	private Iterable<Integer> getInitialStates(Model<?> model)
	{
		return initialStates == null ? model.getInitialStates() : () -> initialStates.stream().iterator();
	}
}
