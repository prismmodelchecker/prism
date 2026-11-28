//==============================================================================
//	
//	Copyright (c) 2022-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

package strat;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import explicit.ConstructStrategyProduct;
import explicit.InducedStateMap;
import explicit.Model;
import explicit.NondetModel;
import io.ModelExportFormat;
import parser.State;
import prism.Evaluator;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismNotSupportedException;

/**
 * Base class for implementations of Strategy associated with an explicit engine model.
 */
public abstract class StrategyExplicit<Value> extends StrategyWithStates<Value>
{
	// Model associated with the strategy
	protected NondetModel<Value> model;

	public StrategyExplicit(NondetModel<Value> model)
	{
		this.model = model;
		if (model.getStatesList() != null) {
			setStateLookUp(state -> model.getStatesList().indexOf(state));
		} else {
			setStateLookUp(state -> -1);
		}
	}

	@Override
	public NondetModel<Value> getModel()
	{
		return model;
	}

	@Override
	public Evaluator<Value> getEvaluator()
	{
		return model.getEvaluator();
	}

	@Override
	public Model<Value> constructInducedModel(StrategyExportOptions options) throws PrismException
	{
		return constructInducedModel(options, null);
	}

	/**
	 * Construct the model induced by this strategy.
	 * If {@code stateMapConsumer} is non-null, the map from states of the induced model
	 * to states of the original model (and strategy memory) is also stored
	 * and then passed to it.
	 */
	public abstract Model<Value> constructInducedModel(StrategyExportOptions options, Consumer<InducedStateMap> stateMapConsumer) throws PrismException;

	/**
	 * Construct the model induced by this strategy, for one with (finite) memory,
	 * as a product of the model and the strategy's memory.
	 * Strategies with memory can use this for {@link #constructInducedModel(StrategyExportOptions, Consumer)}.
	 */
	protected Model<Value> constructInducedModelWithMemory(StrategyExportOptions options, Consumer<InducedStateMap> stateMapConsumer) throws PrismException
	{
		ConstructStrategyProduct csp = new ConstructStrategyProduct(options);
		csp.setStoreStateMap(stateMapConsumer != null);
		Model<Value> prodModel = csp.constructProductModel(model, this);
		if (stateMapConsumer != null) {
			stateMapConsumer.accept(csp.getStateMap());
		}
		return prodModel;
	}

	/**
	 * Get a string representing the choice index (or, for a randomised strategy,
	 * the distribution over choice indices) of the strategy in the state index s
	 * and where the current memory of the strategy (if applicable) is m,
	 * as used for {@link #exportIndices(PrismLog, StrategyExportOptions)}.
	 * The choice should be defined. By default, this is {@link #getChoiceIndex(int, int)},
	 * and an exception is thrown if the strategy does not give one.
	 */
	public String getChoiceIndexString(int s, int m) throws PrismException
	{
		int i = getChoiceIndex(s, m);
		if (i < 0) {
			throw new PrismNotSupportedException("Strategy does not give choice indices");
		}
		return Integer.toString(i);
	}

	/**
	 * Export the strategy's decisions, for one with (finite) memory, as {@code state,memory=decision},
	 * for each pair of a state and a memory value that can occur together (on some path from
	 * an initial state, whatever the choices), ordered by state and then memory.
	 * Decisions are actions or, if {@code indices} is true, choice indices
	 * (see {@link #getChoiceIndexString(int, int)}).
	 * Strategies with memory can use this for {@link #exportActions(PrismLog, StrategyExportOptions)}
	 * and {@link #exportIndices(PrismLog, StrategyExportOptions)}.
	 */
	protected void exportWithMemory(PrismLog out, StrategyExportOptions options, boolean indices) throws PrismException
	{
		List<State> states = model.getStatesList();
		boolean showStates = options.getShowStates() && states != null;
		int memSize = getMemorySize();
		BitSet pairs = findStateMemoryPairs();
		for (int i = pairs.nextSetBit(0); i >= 0; i = pairs.nextSetBit(i + 1)) {
			int s = i / memSize;
			int m = i % memSize;
			String stateString = (showStates ? states.get(s) : Integer.toString(s)) + "," + getMemoryString(m);
			if (isChoiceDefined(s, m)) {
				out.println(stateString + "=" + (indices ? getChoiceIndexString(s, m) : getChoiceActionString(s, m)));
			} else if (options.getShowUndefined()) {
				out.println(stateString + "=" + getUndefinedChoiceString(s, m));
			}
		}
	}

	/**
	 * Find the pairs of a state s and memory value m (encoded as s * memSize + m)
	 * that can occur together, i.e., on some path from an initial state (whatever the choices).
	 */
	private BitSet findStateMemoryPairs() throws PrismException
	{
		int memSize = getMemorySize();
		try {
			Math.multiplyExact(model.getNumStates(), memSize);
		} catch (ArithmeticException e) {
			throw new PrismException("Too many pairs of states and strategy memory values to export");
		}
		BitSet pairs = new BitSet();
		List<Integer> explore = new ArrayList<>();
		for (int s : model.getInitialStates()) {
			int i = s * memSize + checkMemory(getInitialMemory(s), memSize, s);
			if (!pairs.get(i)) {
				pairs.set(i);
				explore.add(i);
			}
		}
		while (!explore.isEmpty()) {
			int i = explore.remove(explore.size() - 1);
			int s = i / memSize;
			int m = i % memSize;
			int numChoices = model.getNumChoices(s);
			for (int j = 0; j < numChoices; j++) {
				Object act = model.getAction(s, j);
				for (Iterator<Integer> it = model.getSuccessorsIterator(s, j); it.hasNext(); ) {
					int t = it.next();
					int iNew = t * memSize + checkMemory(getUpdatedMemory(m, act, t), memSize, t);
					if (!pairs.get(iNew)) {
						pairs.set(iNew);
						explore.add(iNew);
					}
				}
			}
		}
		return pairs;
	}

	/**
	 * Check that memory value m, on entering state s, is known and in range.
	 */
	private static int checkMemory(int m, int memSize, int s) throws PrismException
	{
		if (m < 0) {
			throw new PrismException("The memory status is unknown (state " + s + ")");
		}
		if (m >= memSize) {
			throw new PrismException("Invalid strategy memory value " + m + " (state " + s + ")");
		}
		return m;
	}

	@Override
	public void exportInducedModel(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		constructInducedModel(options).export(out, options.getInducedModelExportOptions());
	}

	@Override
	public void exportDotFile(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		// Usually, this has been set up so we can treat it an induced model export
		// in Dot format; if so, just delegate to the induced model export method
		if (options.getInducedModelExportOptions() != null && options.getInducedModelExportOptions().getFormat() == ModelExportFormat.DOT) {
			exportInducedModel(out, options);
		}
		// Otherwise call Dot export directly, passing a few options
		else {
			constructInducedModel(options).exportToDotFile(out, null, options.getShowStates(), options.getModelPrecision());
		}
	}
}
