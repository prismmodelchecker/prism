//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package cex;

import java.util.ArrayList;

import jdd.JDD;
import jdd.JDDNode;
import parser.State;

/**
 * Class to store a single-path counterexample, as a list of BDDs.
 */
public class CexPathAsBDDs
{
	protected prism.Model model;
	protected ArrayList<JDDNode> states;

	/**
	 * Construct empty path.
	 */
	public CexPathAsBDDs(prism.Model model)
	{
		this.model = model;
		states = new ArrayList<JDDNode>();
	}

	/**
	 * Add a state to the path (as a BDD, which will be stored and Ref'ed).
	 */
	public void addState(JDDNode state)
	{
		JDD.Ref(state);
		states.add(state);
	}

	/**
	 * Clear the counterexample.
	 */
	public void clear()
	{
		for (JDDNode dd : states) {
			JDD.Deref(dd);
		}
	}

	@Override
	public String toString()
	{
		State state;
		int i, n;
		String s = "";
		n = states.size();
		for (i = 0; i < n; i++) { 
			state = model.convertBddToState(states.get(i));
			s += state.toString();
			if (i < n - 1)
				s += "\n";
		}
		return s;
	}
}
