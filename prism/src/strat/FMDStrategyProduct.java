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

import explicit.ConstructInducedModel;
import explicit.ConstructStrategyProduct;
import explicit.Model;
import explicit.NondetModel;
import explicit.Product;
import explicit.SuccessorsIterator;
import parser.State;
import prism.PrismException;
import prism.PrismLog;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class to store finite-memory deterministic (FMD) strategies
 * using a memoryless Strategy and an explicit engine product model.
 */
public class FMDStrategyProduct<Value> extends StrategyExplicit<Value>
{
	// Product model associated with the strategy
	private Product<?> product;
	// Memoryless strategy over product model
	private MDStrategy<Value> strat;
	
	/**
	 * Creates an FMDStrategyProduct from a memoryless Strategy and an explicit engine product model.
	 */
	@SuppressWarnings("unchecked")
	public FMDStrategyProduct(Product<?> product, MDStrategy<Value> strat)
	{
		super((NondetModel<Value>) product.getOriginalModel());
		this.product = product;
		this.strat = strat;
	}

	@Override
	public Memory memory()
	{
		return Memory.FINITE;
	}
	
	@Override
	public Object getChoiceAction(int s, int m)
	{
		// Find a matching state in the product and look up the strategy for it
		int i = findMatchingProductState(s, m);
		return i == -1 ? Strategy.UNDEFINED : strat.getChoiceAction(i);
	}
	
	@Override
	public int getChoiceIndex(int s, int m)
	{
		// Find a matching state in the product and look up strategy for it
		int i = findMatchingProductState(s, m);
		return i == -1 ? -1 : strat.getChoiceIndex(i);
	}
	
	/**
	 * Find the index of a state (s,m) in the product, if there is one
	 */
	private int findMatchingProductState(int s, int m)
	{
		// If memory value is unknown, there are no matching states
		if (m == -1) {
			return -1;
		}
		// Look for a matching state in the product
		// (inefficiently, since they are not sorted)
		int n = product.getProductModel().getNumStates();
		for (int i = 0; i < n; i++) {
			if (product.getModelState(i) == s && product.getAutomatonState(i) == m) {
				return i;
			}
		}
		// No match
		return -1;
	}
	
	@Override
	public int getMemorySize()
	{
		return product.getAutomatonSize();
	}
	
	@Override
	public int getInitialMemory(int sInit)
	{
		// We don't have access to the original automaton, so we
		// look for an initial state in the product with this model state
		// (inefficiently, since they are not sorted)
		for (int i : product.getProductModel().getInitialStates()) {
			if (product.getModelState(i) == sInit) {
				return product.getAutomatonState(i);
			}
		}
		return -1;
	}
	
	@Override
	public int getUpdatedMemory(int m, Object action, int sNext)
	{
		// If the current memory is unknown, we don't know how to update it
		if (m == -1) {
			return -1;
		}
		// We don't have access to the original automaton, so we
		// look for a matching transition in the product and find the memory update
		// (inefficiently, since they are not sorted)
		int n = product.getProductModel().getNumStates();
		for (int i = 0; i < n; i++) {
			if (product.getAutomatonState(i) == m) {
				int j = findMatchingMemoryUpdate(i, sNext);
				if (j != -1) {
					return j;
				}
			}
		}
		return -1;
	}
	
	/**
	 * Find a transition in the i-th state of the product whose destination
	 * has model state sNext, if there is one, and return the automaton state
	 */
	private int findMatchingMemoryUpdate(int i, int sNext)
	{
		for (SuccessorsIterator succ = product.getProductModel().getSuccessors(i); succ.hasNext(); ) {
			int j = succ.nextInt();
			if (product.getModelState(j) == sNext) {
				return product.getAutomatonState(j);
			}
		}
		// No match
		return -1;
	}
	
	@Override
	public void exportActions(PrismLog out, StrategyExportOptions options)
	{
		List<State> states = model.getStatesList();
		boolean showStates = options.getShowStates() && states != null;
		int n = product.getProductModel().getNumStates();
		for (int i = 0; i < n; i++) {
			int s = product.getModelState(i);
			int m = product.getAutomatonState(i);
			Object act = strat.getChoiceAction(i);
			if (act != UNDEFINED) {
				out.println((showStates ? states.get(s) : s) + "," + m + "=" + (act == null ? "" : act.toString()));
			}
		}
	}

	@Override
	public void exportIndices(PrismLog out, StrategyExportOptions options)
	{
		int n = product.getProductModel().getNumStates();
		for (int i = 0; i < n; i++) {
			int s = product.getModelState(i);
			int m = product.getAutomatonState(i);
			out.println(s + "," + m + "=" + strat.getChoiceIndex(i));
		}
	}

	@Override
	public void exportInducedModel(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		// If restricting to reachable states, construct product afresh
		if (options.getReachOnly()) {
			ConstructStrategyProduct csp = new ConstructStrategyProduct();
			csp.setMode(options.getMode());
			Model<Value> prodModel = csp.constructProductModel(model, this);
			prodModel.exportToPrismExplicitTra(out, options.getModelPrecision());
		}
		// Otherwise, just export MD strategy, unmodified
		else {
			strat.exportInducedModel(out, options);
		}
	}

	@Override
	public void exportDotFile(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		ConstructStrategyProduct csp = new ConstructStrategyProduct();
		csp.setMode(options.getMode());
		Model<Value> prodModel = csp.constructProductModel(model, this);
		prodModel.exportToDotFile(out, null, options.getShowStates(), options.getModelPrecision());
	}

	@Override
	public void clear()
	{
		strat.clear();
	}

	@Override
	public String toString()
	{
		return "[" + IntStream.range(0, getNumStates())
				.mapToObj(s -> IntStream.range(0, getMemorySize())
				.mapToObj(m -> s + "," + m + "=" + getChoiceActionString(s, m))
				.collect(Collectors.joining(",")))
				.collect(Collectors.joining(",")) + "]";
	}
}
