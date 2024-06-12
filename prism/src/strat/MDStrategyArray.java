//==============================================================================
//	
//	Copyright (c) 2002-
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

import explicit.ConstructInducedModel;
import explicit.Model;
import explicit.NondetModel;
import parser.State;
import prism.PrismException;
import prism.PrismLog;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class to store a memoryless deterministic (MD) strategy
 * as a (Java) array of choice indices associated with an explicit engine model.
 */
public class MDStrategyArray<Value> extends StrategyExplicit<Value> implements MDStrategy<Value>
{
	// Index of choice taken in each state (wrt model above)
	// Other possible values: -1 (unknown), -2 (arbitrary), -3 (unreachable)
	private int choices[];

	/**
	 * Creates an MDStrategyArray from an integer array of choices.
	 * The array may later be modified/delete - take a copy if you want to keep it.
	 */
	public MDStrategyArray(NondetModel<Value> model, int choices[])
	{
		super(model);
		this.choices = choices;
	}

	@Override
	public Object getChoiceAction(int s, int m)
	{
		int c = choices[s];
		return c >= 0 ? model.getAction(s, c) : Strategy.UNDEFINED;
	}

	@Override
	public int getChoiceIndex(int s, int m)
	{
		return choices[s];
	}

	@Override
	public UndefinedReason whyUndefined(int s, int m)
	{
		switch (choices[s]) {
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
	public void exportActions(PrismLog out, StrategyExportOptions options)
	{
		List<State> states = model.getStatesList();
		boolean showStates = options.getShowStates() && states != null;
		int n = getNumStates();
		for (int s = 0; s < n; s++) {
			if (isChoiceDefined(s)) {
				out.println((showStates ? states.get(s) : s) + "=" + getChoiceActionString(s));
			}
		}
	}

	@Override
	public void exportInducedModel(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		ConstructInducedModel cim = new ConstructInducedModel();
		cim.setMode(options.getMode()).setReachOnly(options.getReachOnly());
		Model<Value> inducedModel = cim.constructInducedModel(model, this);
		inducedModel.exportToPrismExplicitTra(out, options.getModelPrecision());

		// NB: for reach=false (and MDPs), it would be slightly cheaper to use:
		//Model<Value> inducedModel = model.constructInducedModel(this);
	}

	@Override
	public void exportDotFile(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		ConstructInducedModel cim = new ConstructInducedModel();
		cim.setMode(options.getMode()).setReachOnly(options.getReachOnly());
		Model<Value> inducedModel = cim.constructInducedModel(model, this);
		inducedModel.exportToDotFile(out, null, options.getShowStates(), options.getModelPrecision());
	}

	@Override
	public void clear()
	{
		choices = null;
	}

	@Override
	public String toString()
	{
		return "[" + IntStream.range(0, getNumStates())
				.mapToObj(s -> s + "=" + getChoiceActionString(s))
				.collect(Collectors.joining(",")) + "]";
	}
}
