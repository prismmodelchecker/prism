//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package explicit.graphviz;

import java.util.List;

import parser.State;

/**
 * Decorator which show the state variable valuation for a given state.
 */
public class ShowStatesDecorator implements Decorator
{
	/** The state list, i.e., the variable valuation information for each state index */
	private List<State> stateList;

	/**
	 * Constructor.
	 * @param stateList the variable valuation information for each state index
	 */
	public ShowStatesDecorator(List<State> stateList)
	{
		this.stateList = stateList;
	}

	/** Decorate state label by appending the variable information */
	public Decoration decorateState(int state, Decoration d)
	{
		d.labelAddBelow(stateList.get(state).toString());
		return d;
	}
}
