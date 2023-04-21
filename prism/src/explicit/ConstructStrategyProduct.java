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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import parser.State;
import parser.VarList;
import parser.ast.Declaration;
import parser.ast.DeclarationInt;
import parser.ast.Expression;
import prism.ModelType;
import prism.PrismException;
import prism.PrismNotSupportedException;
import strat.Strategy;
import strat.StrategyExportOptions;
import strat.StrategyInfo;

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
			Declaration decl = new Declaration(memVar, new DeclarationInt(Expression.Int(0), Expression.Int(strat.getMemorySize())));
			newVarList.addVar(0, decl, 1, model.getConstantValues());
		}

		// Determine type of induced model
		ModelType modelType = model.getModelType();
		ModelType productModelType = null;
		if (mode == StrategyExportOptions.InducedModelMode.REDUCE) {
			switch (modelType) {
				case MDP:
				case POMDP:
				case STPG:
					productModelType = ModelType.DTMC;
					break;
				case IMDP:
					productModelType = ModelType.IDTMC;
					break;
				default:
					throw new PrismNotSupportedException("Product construction not supported for " + modelType + "s");
			}
		} else {
			productModelType = modelType;
		}

		// Create a (simple, mutable) model of the appropriate type
		ModelSimple<Value> prodModel = null;
		switch (productModelType) {
		case DTMC:
			prodModel = new DTMCSimple<>();
			break;
		case MDP:
			prodModel = new MDPSimple<>();
			break;
		case POMDP:
			prodModel = new POMDPSimple<>();
			break;
		case IDTMC:
			prodModel = (ModelSimple<Value>) new IDTMCSimple<>();
			break;
		case IMDP:
			prodModel = (ModelSimple<Value>) new IMDPSimple<>();
			break;
		case STPG:
			prodModel = new STPGSimple<>();
			break;
		default:
			throw new PrismNotSupportedException("Product construction not supported for " + modelType + "s");
		}
		// Attach evaluator and variable info
		((ModelExplicit<Value>) prodModel).setEvaluator(model.getEvaluator());
		((ModelExplicit<Value>) prodModel).setVarList(newVarList);

		// Now do the actual product model construction
		// This is a separate method so that we can alter the model type if needed,
		// e.g. construct an IMDP<Value> product as one over an MDP<Interval<Value>>
		switch (modelType) {
		case IMDP:
			productModelType = (mode == StrategyExportOptions.InducedModelMode.REDUCE) ? ModelType.DTMC : ModelType.MDP;
			return doConstructProductModel(ModelType.MDP, productModelType, prodModel, model, strat);
		default:
			return doConstructProductModel(modelType, productModelType, prodModel, model, strat);
		}
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
		int s_1, s_2, q_1, q_2;
		List<State> prodStatesList = null, memStatesList = null;
		Value stratChoiceProb = model.getEvaluator().one();

		// Check size limits for this product construction approach
		try {
			prodNumStates = Math.multiplyExact(modelNumStates, memSize);
		} catch (ArithmeticException e) {
			throw new PrismException("Size of product state space of model and strategy is too large for explicit engine");
		}
		
		// Encoding: 
		// each state s' = <s, q> = s * memSize + q
		// s(s') = s' / memSize
		// q(s') = s' % memSize

		// Initialise state info storage
		LinkedList<Point> queue = new LinkedList<Point>();
		int map[] = new int[prodNumStates];
		Arrays.fill(map, -1);
		if (model.getStatesList() != null) {
			prodStatesList = new ArrayList<State>();
			memStatesList = new ArrayList<State>(memSize);
			for (int i = 0; i < memSize; i++) {
				memStatesList.add(new State(1).setValue(0, i));
			}
		}

		// Get initial states
		for (int s_0 : model.getInitialStates()) {
			// Find corresponding initial memory
			int q_0 = strat.getInitialMemory(s_0);
			if (q_0 < 0) {
				throw new PrismException("The memory status is unknown (state " + s_0 + ")");
			}
			// Add (initial) state to product
			queue.add(new Point(s_0, q_0));
			switch (productModelType) {
			case STPG:
				((STPGSimple<Value>) prodModel).addState(((STPG<Value>) model).getPlayer(s_0));
				break;
			default:
				prodModel.addState();
			break;
			}
			prodModel.addInitialState(prodModel.getNumStates() - 1);
			map[s_0 * memSize + q_0] = prodModel.getNumStates() - 1;
			if (prodStatesList != null) {
				// Store state information for the product
				prodStatesList.add(new State(model.getStatesList().get(s_0), memStatesList.get(q_0)));
			}
		}

		// Product states
		BitSet visited = new BitSet(prodNumStates);
		while (!queue.isEmpty()) {
			Point p = queue.pop();
			s_1 = p.x;
			q_1 = p.y;
			visited.set(s_1 * memSize + q_1);
			int numChoices =  model.getNumChoices(s_1);
			// Extract strategy decision
			Object decision = strat.getChoiceAction(s_1, q_1);
			// If it is undefined, just pick the first one
			if (decision == StrategyInfo.UNDEFINED && numChoices > 0) {
				decision = model.getAction(s_1, 0);
			}
			// Go through transitions from state s_1 in original model
			for (int j = 0; j < numChoices; j++) {
				Object act = model.getAction(s_1, j);
				// Skip choices not picked by the strategy
				if (!strat.isActionChosen(decision, act)) {
					continue;
				}
				if (strat.isRandomised()) {
					stratChoiceProb = strat.getChoiceActionProbability(decision, act);
				}
				Iterator<Map.Entry<Integer, Value>> iter;
				switch (modelType) {
				case DTMC:
					iter = ((DTMC<Value>) model).getTransitionsIterator(s_1);
					break;
				case MDP:
					iter = ((MDP<Value>) model).getTransitionsIterator(s_1, j);
					break;
				case POMDP:
					iter = ((POMDP<Value>) model).getTransitionsIterator(s_1, j);
					break;
				case STPG:
					iter = ((STPG<Value>) model).getTransitionsIterator(s_1, j);
					break;
				default:
					throw new PrismNotSupportedException("Product construction not implemented for " + modelType + "s");
				}
				Distribution<Value> prodDistr = null;
				if (productModelType.nondeterministic()) {
					prodDistr = new Distribution<>(model.getEvaluator());
				}
				while (iter.hasNext()) {
					Map.Entry<Integer, Value> e = iter.next();
					s_2 = e.getKey();
					Value prob = e.getValue();
					if (strat.isRandomised()) {
						prob = model.getEvaluator().multiply(prob, stratChoiceProb);
					}
					// Find corresponding memory update
					q_2 = strat.getUpdatedMemory(q_1, model.getAction(s_1, j), s_2);
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
							prodStatesList.add(new State(model.getStatesList().get(s_2), memStatesList.get(q_2)));
						}
					}
					switch (productModelType) {
					case DTMC:
						((DTMCSimple<Value>) prodModel).setProbability(map[s_1 * memSize + q_1], map[s_2 * memSize + q_2], prob);
						break;
					case MDP:
					case POMDP:
					case STPG:
						prodDistr.set(map[s_2 * memSize + q_2], prob);
						break;
					default:
						throw new PrismNotSupportedException("Product construction not implemented for " + modelType + "s");
					}
				}
				switch (productModelType) {
				case MDP:
					((MDPSimple<Value>) prodModel).addActionLabelledChoice(map[s_1 * memSize + q_1], prodDistr, ((MDP<Value>) model).getAction(s_1, j));
					break;
				case POMDP:
					((POMDPSimple<Value>) prodModel).addActionLabelledChoice(map[s_1 * memSize + q_1], prodDistr, ((POMDP<Value>) model).getAction(s_1, j));
					break;
				case STPG:
					((STPGSimple<Value>) prodModel).addActionLabelledChoice(map[s_1 * memSize + q_1], prodDistr, ((STPG<Value>) model).getAction(s_1, j));
					break;
				default:
					break;
				}
			}
		}
		
		prodModel.findDeadlocks(false);

		if (prodStatesList != null) {
			prodModel.setStatesList(prodStatesList);
		}
		
		return prodModel;
	}
}
