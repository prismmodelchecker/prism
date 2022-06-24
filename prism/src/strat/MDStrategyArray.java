//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Aistis Simaitis <aistis.aimaitis@cs.ox.ac.uk> (University of Oxford)
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

import explicit.Model;
import explicit.NondetModel;
import prism.PrismLog;

/**
 * Class to store a memoryless deterministic (MD) strategy
 * as a (Java) array of choice indices associated with an explicit engine model.
 */
public class MDStrategyArray extends StrategyExplicit implements MDStrategy
{
	// Index of choice taken in each state (wrt model above)
	// Other possible values: -1 (unknown), -2 (arbitrary), -3 (unreachable)
	private int choices[];

	/**
	 * Creates an MDStrategyArray from an integer array of choices.
	 * The array may later be modified/delete - take a copy if you want to keep it.
	 */
	public MDStrategyArray(NondetModel model, int choices[])
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
	public void exportInducedModel(PrismLog out, int precision)
	{
		Model dtmcInd = model.constructInducedModel(this);
		dtmcInd.exportToPrismExplicitTra(out, precision);
	}

	@Override
	public void exportDotFile(PrismLog out, int precision)
	{
		// For now, we export just the reduced (induced) model
		Model dtmcInd = model.constructInducedModel(this);
		dtmcInd.exportToDotFile(out, null, true, precision);
		//model.exportToDotFileWithStrat(out, null, choices, precision);
	}

	@Override
	public void clear()
	{
		choices = null;
	}
}
