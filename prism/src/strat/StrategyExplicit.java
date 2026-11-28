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

import java.util.function.Consumer;

import explicit.ConstructStrategyProduct;
import explicit.InducedStateMap;
import explicit.Model;
import explicit.NondetModel;
import io.ModelExportFormat;
import prism.Evaluator;
import prism.PrismException;
import prism.PrismLog;

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
