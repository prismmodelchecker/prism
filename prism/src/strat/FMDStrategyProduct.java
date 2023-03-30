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

import explicit.ConstructStrategyProduct;
import explicit.Model;
import explicit.NondetModel;
import explicit.Product;
import explicit.SuccessorsIterator;
import prism.PrismException;
import prism.PrismLog;

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
	 * Creates an MDStrategyArray from a memoryless Strategy and an explicit engine product model.
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
		// Inefficient lookup for now, since product states not sorted
		int n = product.getProductModel().getNumStates();
		for (int i = 0; i < n; i++) {
			if (product.getModelState(i) == s && product.getAutomatonState(i) == m) {
				return strat.getChoiceAction(i);
			}
		}
		return Strategy.UNDEFINED;
	}
	
	@Override
	public int getChoiceIndex(int s, int m)
	{
		// Inefficient lookup for now, since product states not sorted
		int n = product.getProductModel().getNumStates();
		for (int i = 0; i < n; i++) {
			if (product.getModelState(i) == s && product.getAutomatonState(i) == m) {
				return strat.getChoiceIndex(i);
			}
		}
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
		// Inefficient lookup for now: extract from product
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
		// Inefficient lookup for now: extract from product
		int n = product.getProductModel().getNumStates();
		for (int i = 0; i < n; i++) {
			if (product.getAutomatonState(i) == m) {
				for (SuccessorsIterator succ = product.getProductModel().getSuccessors(i); succ.hasNext(); ) {
					int j = succ.nextInt();
					if (product.getModelState(j) == sNext) {
						return product.getAutomatonState(j);
					}
				}
			}
		}
		return -1;
	}
	
	@Override
	public void exportActions(PrismLog out)
	{
		int n = product.getProductModel().getNumStates();
		for (int i = 0; i < n; i++) {
			int s = product.getModelState(i);
			int m = product.getAutomatonState(i);
			Object act = strat.getChoiceAction(i);
			if (act != UNDEFINED) {
				out.println(s + "," + m + ":" + act);
			}
		}
	}

	@Override
	public void exportIndices(PrismLog out)
	{
		int n = product.getProductModel().getNumStates();
		for (int i = 0; i < n; i++) {
			int s = product.getModelState(i);
			int m = product.getAutomatonState(i);
			out.println(s + "," + m + ":" + strat.getChoiceIndex(i));
		}
	}

	@Override
	public void exportInducedModel(PrismLog out, int precision) throws PrismException
	{
		ConstructStrategyProduct csp = new ConstructStrategyProduct();
		Model<Value> prodModel = csp.constructProductModel(model, this);
		prodModel.exportToPrismExplicitTra(out, precision);
	}

	@Override
	public void exportDotFile(PrismLog out, int precision) throws PrismException
	{
		ConstructStrategyProduct csp = new ConstructStrategyProduct();
		Model<Value> prodModel = csp.constructProductModel(model, this);
		prodModel.exportToDotFile(out, null, true, precision);
	}

	@Override
	public void clear()
	{
		strat.clear();
	}
}
