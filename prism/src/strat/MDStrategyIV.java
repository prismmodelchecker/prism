//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dv.IntegerVector;
import prism.Model;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismNotSupportedException;

/**
 * Class to store a memoryless deterministic (MD) strategy
 * as an IntegerVector (i.e. stored natively as an array)
 * associated with a sparse/symbolic engine model.
 */
public class MDStrategyIV extends StrategyWithStates<Double> implements MDStrategy<Double>
{
	// Model associated with the strategy
	private Model model;
	// Other model info
	private int numStates;
	private List<String> actions;
	// Array storing MD strategy: *action* index (not choice index) for each state
	private IntegerVector iv;
	
	/**
	 * Create an MDStrategyIV from an IntegerVector.
	 */
	public MDStrategyIV(Model model, IntegerVector iv)
	{
		this.model = model;
		numStates = (int) model.getNumStates();
		actions = model.getSynchs();
		this.iv = iv;
		setStateLookUp(state -> {
			try {
				return model.getReachableStates().getIndexOfState(state);
			} catch (PrismNotSupportedException e) {
				return -1;
			}
		});
	}
	
	@Override
	public Object getChoiceAction(int s, int m)
	{
		int c = iv.getElement(s);
		return c >= 0 ? actions.get(c) : Strategy.UNDEFINED;
	}
	
	@Override
	public int getChoiceIndex(int s, int m)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public UndefinedReason whyUndefined(int s, int m)
	{
		switch (iv.getElement(s)) {
		case -1:
			return UndefinedReason.UNKNOWN;
		case -2:
			return UndefinedReason.ARBITRARY;
		case -3:
			return UndefinedReason.UNREACHABLE;
		default:
			return null;
		}
	}

	@Override
	public int getNumStates()
	{
		return numStates;
	}
	
	@Override
	public void exportInducedModel(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		throw new PrismException("Induced model construction not yet supported for symbolic engines");
	}

	@Override
	public void exportDotFile(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		throw new PrismException("Strategy dot export not yet supported for symbolic engines");
	}
	
	@Override
	public void clear()
	{
		iv.clear();
		iv = null;
	}

	@Override
	public String toString()
	{
		return "[" + IntStream.range(0, getNumStates())
				.mapToObj(s -> s + "=" + getChoiceActionString(s))
				.collect(Collectors.joining(",")) + "]";
	}
}
