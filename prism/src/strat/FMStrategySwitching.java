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

package strat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import explicit.InducedStateMap;
import explicit.Model;
import explicit.NondetModel;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismNotSupportedException;

/**
 * A finite-memory strategy that switches between memoryless strategies according to its memory:
 * the memory ({@link StrategyMemory}) is updated as usual, and each memory value has a memoryless
 * strategy whose decisions are used while the memory has that value (or none, in which case
 * the decisions are undefined). For example, a strategy that follows one strategy until
 * it takes some choice, and then another one forever, has two memory values.
 * <br><br>
 * Where the strategy for a memory value leaves a choice undefined (or there is none),
 * the reason given is the memory's, if any (see {@link StrategyMemory#whyUndefined(int)}),
 * or else that of the strategy for the memory value.
 */
public class FMStrategySwitching<Value> extends StrategyExplicit<Value>
{
	/** The memory */
	protected StrategyMemory memory;
	/** For each memory value, the (memoryless) strategy used, or null if decisions are undefined */
	protected List<StrategyExplicit<Value>> strategies;

	/**
	 * Create a strategy switching between (memoryless) strategies according to its memory.
	 * @param model The model
	 * @param memory The memory
	 * @param strategies For each memory value, the (memoryless) strategy used, or null if decisions are undefined
	 */
	public FMStrategySwitching(NondetModel<Value> model, StrategyMemory memory, List<? extends StrategyExplicit<Value>> strategies)
	{
		super(model);
		if (memory.size() < 1) {
			throw new IllegalArgumentException("There should be at least one memory value");
		}
		if (strategies.size() != memory.size()) {
			throw new IllegalArgumentException("There should be a strategy (or null) for each memory value");
		}
		for (Strategy<Value> strat : strategies) {
			if (strat != null && strat.hasMemory()) {
				throw new IllegalArgumentException("Strategies switched between must be memoryless");
			}
			if (strat != null && strat.getModel() != model) {
				throw new IllegalArgumentException("Strategies switched between must be for the same model");
			}
		}
		this.memory = memory;
		this.strategies = new ArrayList<>(strategies);
	}

	/**
	 * Get the memory of this strategy.
	 */
	public StrategyMemory getMemory()
	{
		return memory;
	}

	/**
	 * Get the strategy used with memory value m (null if decisions are undefined).
	 */
	public StrategyExplicit<Value> getStrategy(int m)
	{
		return strategies.get(m);
	}

	// Memory

	@Override
	public Memory memory()
	{
		return Memory.FINITE;
	}

	@Override
	public int getMemorySize()
	{
		return memory.size();
	}

	@Override
	public int getInitialMemory(int sInit)
	{
		return memory.initial(sInit);
	}

	@Override
	public int getUpdatedMemory(int m, Object action, int sNext)
	{
		// (memory not known: stays unknown)
		return m < 0 ? -1 : memory.update(m, action, sNext);
	}

	@Override
	public String getMemoryString(int m)
	{
		return m < 0 ? "?" : memory.toString(m);
	}

	// Decisions

	@Override
	public boolean isRandomised()
	{
		return strategies.stream().anyMatch(strat -> strat != null && strat.isRandomised());
	}

	@Override
	public Object getChoiceAction(int s, int m)
	{
		// (memory not known: no decision)
		Strategy<Value> strat = m < 0 ? null : strategies.get(m);
		return strat == null ? UNDEFINED : strat.getChoiceAction(s, -1);
	}

	@Override
	public int getChoiceIndex(int s, int m)
	{
		Strategy<Value> strat = m < 0 ? null : strategies.get(m);
		return strat == null ? whyUndefined(s, m).getCode() : strat.getChoiceIndex(s, -1);
	}

	@Override
	public UndefinedReason whyUndefined(int s, int m)
	{
		if (m < 0) {
			return UndefinedReason.UNKNOWN;
		}
		// Defined if the strategy for the memory value defines it
		Strategy<Value> strat = strategies.get(m);
		UndefinedReason whyStrat = strat == null ? UndefinedReason.UNKNOWN : strat.whyUndefined(s, -1);
		if (whyStrat == null) {
			return null;
		}
		// Otherwise, the memory's reason, if any, or else the strategy's
		UndefinedReason whyMemory = memory.whyUndefined(m);
		return whyMemory != null ? whyMemory : whyStrat;
	}

	// Induced models and export

	@Override
	public Model<Value> constructInducedModel(StrategyExportOptions options, Consumer<InducedStateMap> stateMapConsumer) throws PrismException
	{
		return constructInducedModelWithMemory(options, stateMapConsumer);
	}

	@Override
	public void exportActions(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		exportWithMemory(out, options, false);
	}

	@Override
	public void exportIndices(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		exportWithMemory(out, options, true);
	}

	@Override
	public String getChoiceIndexString(int s, int m) throws PrismException
	{
		StrategyExplicit<Value> strat = m < 0 ? null : strategies.get(m);
		if (strat == null) {
			throw new PrismNotSupportedException("No choice index for an undefined choice");
		}
		return strat.getChoiceIndexString(s, -1);
	}

	@Override
	public void clear()
	{
		for (Strategy<Value> strat : strategies) {
			if (strat != null) {
				strat.clear();
			}
		}
	}

	@Override
	public String toString()
	{
		return "[" + memory.size() + " memory values: " + strategies + "]";
	}
}
