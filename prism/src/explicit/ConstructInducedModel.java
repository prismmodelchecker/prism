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
import strat.MDStrategy;
import strat.StrategyExportOptions.InducedModelMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
	 * Construct the model induced by a memoryless deterministic strategy on a nondeterministic model
	 * @param model The model
	 * @param strat The strategy
	 * @return The induced model
	 */
	@SuppressWarnings("unchecked")
	public <Value> Model<Value> constructInducedModel(NondetModel<Value> model, MDStrategy<Value> strat) throws PrismException
	{
		// Determine type of induced model
		ModelType modelType = model.getModelType();
		ModelType inducedModelType = null;
		if (mode == InducedModelMode.REDUCE) {
			switch (modelType) {
				case MDP:
				case POMDP:
				case STPG:
					inducedModelType = ModelType.DTMC;
					break;
				case IMDP:
					inducedModelType = ModelType.IDTMC;
					break;
				default:
					throw new PrismNotSupportedException("Induced model construction not supported for " + modelType + "s");
			}
		} else {
			inducedModelType = modelType;
		}

		// Create a (simple, mutable) model of the appropriate type
		ModelSimple<Value> inducedModel = (ModelSimple<Value>) ModelSimple.forModelType(inducedModelType);
		
		// Attach evaluator and copy variable info
		((ModelExplicit<Value>) inducedModel).setEvaluator(model.getEvaluator());
		((ModelExplicit<Value>) inducedModel).setVarList(model.getVarList());

		// Now do the actual induced model construction
		// This is a separate method so that we can alter the model type if needed,
		// e.g. construct an IMDP<Value> product as one over an MDP<Interval<Value>>
		switch (modelType) {
			case IMDP:
				inducedModelType = (mode == InducedModelMode.REDUCE) ? ModelType.DTMC : ModelType.MDP;
				return doConstructInducedModel(ModelType.MDP, inducedModelType, inducedModel, model, strat);
			default:
				return doConstructInducedModel(modelType, inducedModelType, inducedModel, model, strat);
		}
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
	public <Value> Model<Value> doConstructInducedModel(ModelType modelType, ModelType inducedModelType, ModelSimple<Value> inducedModel, NondetModel<Value> model, MDStrategy<Value> strat) throws PrismException
	{
		// Create new states list if needed
		List<State> inducedStatesList = model.getStatesList();
		if (reachOnly && inducedStatesList != null) {
			inducedStatesList = new ArrayList<>();
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
					// Extract strategy decision (if it is undefined, just pick the first choice)
					int j = strat.getChoiceIndex(s);
					if (j < 0) {
						j = 0;
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

			// Go through transitions of original model
			// (for chosen strategy decision; if it is undefined, just pick the first choice)
			int j = strat.getChoiceIndex(s);
			if (j < 0) {
				j = 0;
			}
			Iterator<Map.Entry<Integer, Value>> iter;
			switch (modelType) {
				case MDP:
					iter = ((MDP<Value>) model).getTransitionsIterator(s, j);
					break;
				case POMDP:
					iter = ((POMDP<Value>) model).getTransitionsIterator(s, j);
					break;
				case STPG:
					iter = ((STPG<Value>) model).getTransitionsIterator(s, j);
					break;
				default:
					throw new PrismNotSupportedException("Induced model construction not implemented for " + modelType + "s");
			}
			Distribution<Value> prodDistr = null;
			if (inducedModelType.nondeterministic()) {
				prodDistr = new Distribution<>(model.getEvaluator());
			}
			while (iter.hasNext()) {
				Map.Entry<Integer, Value> e = iter.next();
				int s_2 = e.getKey();
				Value prob = e.getValue();
				// Add transition to model
				switch (inducedModelType) {
					case DTMC:
						((DTMCSimple<Value>) inducedModel).setProbability(map[s], map[s_2], prob);
						break;
					case MDP:
					case POMDP:
					case STPG:
						prodDistr.set(map[s_2], prob);
						break;
					default:
						throw new PrismNotSupportedException("Induced model construction not implemented for " + modelType + "s");
				}
			}
			switch (inducedModelType) {
				case MDP:
					((MDPSimple<Value>) inducedModel).addActionLabelledChoice(map[s], prodDistr, ((MDP<Value>) model).getAction(s, j));
					break;
				case POMDP:
					((POMDPSimple<Value>) inducedModel).addActionLabelledChoice(map[s], prodDistr, ((POMDP<Value>) model).getAction(s, j));
					break;
				case STPG:
					((STPGSimple<Value>) inducedModel).addActionLabelledChoice(map[s], prodDistr, ((STPG<Value>) model).getAction(s, j));
					break;
				default:
					break;
			}
		}

		inducedModel.findDeadlocks(false);

		if (inducedStatesList != null) {
			inducedModel.setStatesList(inducedStatesList);
		}

		return inducedModel;
	}
}
