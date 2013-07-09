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

/**
 * Class to store a memoryless deterministic (MD) strategy, as a (Java) array of choice indices.
 */
public class MDStrategyArray extends MDStrategy
{
	private explicit.NondetModel model;
	private int choices[];
	
	/**
	 * Creates an MDStrategyArray from an integer array of choices.
	 * The array may later be modified/delete - take a copy if you want to keep it.
	 */
	public MDStrategyArray(explicit.NondetModel model, int choices[])
	{
		this.model = model;
		this.choices = choices;
	}
	
	@Override
	public int getNumStates()
	{
		return model.getNumStates();
	}
	
	@Override
	public int getChoice(int s)
	{
		return choices[s];
	}
	
	@Override
	public Object getChoiceAction(int s)
	{
		return model.getAction(s, choices[s]);
	}
}
