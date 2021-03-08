//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
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

package explicit.modelviews;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.PrimitiveIterator;

import explicit.DTMCFromMDPAndMDStrategy;
import explicit.Distribution;
import explicit.MDP;
import explicit.Model;
import explicit.SuccessorsIterator;
import strat.MDStrategy;

/**
 * Base class for an MDP view, i.e., a virtual MDP that is obtained
 * by mapping from some other model on-the-fly.
 * <br>
 * The main job of sub-classes is to provide an appropriate
 * getTransitionsIterator() method. Several other methods, providing
 * meta-data on the model have to be provided as well. For examples,
 * see the sub-classes contained in this package.
 */
public abstract class MDPView extends ModelView implements MDP, Cloneable
{
	public MDPView()
	{
		super();
	}

	public MDPView(final MDPView model)
	{
		super(model);
	}



	//--- Object ---

	@Override
	public String toString()
	{
		String s = "[ ";
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			if (state > 0)
				s += ", ";
			s += state + ": ";
			s += "[";
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				if (choice > 0)
					s += ",";
				final Object action = getAction(state, choice);
				if (action != null)
					s += action + ":";
				s += new Distribution(getTransitionsIterator(state, choice));
			}
			s += "]";
		}
		s += " ]";
		return s;
	}

	//--- NondetModel ---

	@Override
	public SuccessorsIterator getSuccessors(final int state, final int choice)
	{
		final Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice);

		return SuccessorsIterator.from(new PrimitiveIterator.OfInt() {
			public boolean hasNext() {return transitions.hasNext();}
			public int nextInt() {return transitions.next().getKey();}
		}, true);
	}

	@Override
	public Model constructInducedModel(final MDStrategy strat)
	{
		return new DTMCFromMDPAndMDStrategy(this, strat);
	}
}
