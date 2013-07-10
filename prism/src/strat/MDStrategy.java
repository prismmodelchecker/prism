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

import prism.PrismLog;

/**
 * Classes to store memoryless deterministic (MD) strategies.
 */
public abstract class MDStrategy implements Strategy
{
	/**
	 * Get the number of states of the model associated with this strategy. 
	 */
	public abstract int getNumStates();

	/**
	 * Get the index of the choice taken in state s.
	 * The index is defined with respect to a particular model, stored locally.
	 * Other possible values: -1 (unknown), -2 (arbitrary), -3 (unreachable)
	 */
	public abstract int getChoice(int s);

	/**
	 * Get the action taken in state s.
	 */
	public abstract Object getChoiceAction(int s);

	@Override
	public void exportActions(PrismLog out)
	{
		int n = getNumStates();
		for (int s = 0; s < n; s++) {
			// Only print actions for reachable states
			if (getChoice(s) != -3)
				out.println(s + ":" + getChoiceAction(s));
		}
	}
}
