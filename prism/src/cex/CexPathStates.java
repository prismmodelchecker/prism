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

import parser.State;
import simulator.PathFullInfo;;

/**
 * Class to store a counterexample/witness comprising a single path,
 * represented as a list of states, stored as State objects. 
 */
public class CexPathStates implements PathFullInfo
{
	protected prism.Model model;
	protected ArrayList<State> states;

	/**
	 * Construct empty path.
	 */
	public CexPathStates(prism.Model model)
	{
		this.model = model;
		states = new ArrayList<State>();
	}

	/**
	 * Add a state to the path (will be stored, not copied).
	 */
	public void addState(State state)
	{
		states.add(state);
	}

	/**
	 * Clear the counterexample.
	 */
	public void clear()
	{
		states = null;
	}

	// ACCESSORS (for PathFullInfo)
	
	@Override
	public long size()
	{
		return states.size() - 1;
	}
	
	@Override
	public State getState(int step)
	{
		return states.get(step);
	}
	
	@Override
	public double getStateReward(int step, int rsi)
	{
		return 0.0;
	}

	@Override
	public double getCumulativeTime(int step)
	{
		return 0.0;
	}

	@Override
	public double getCumulativeReward(int step, int rsi)
	{
		return 0.0;
	}

	@Override
	public double getTime(int step)
	{
		return 0.0;
	}

	@Override
	public int getChoice(int step)
	{
		return 0;
	}

	@Override
	public int getModuleOrActionIndex(int step)
	{
		return 0;
	}

	@Override
	public String getModuleOrAction(int step)
	{
		return "";
	}

	@Override
	public double getTransitionReward(int step, int rsi)
	{
		return 0.0;
	}
	
	@Override
	public boolean isLooping()
	{
		return false;
	}
	
	@Override
	public long loopStart()
	{
		return 0;
	}
	
	@Override
	public long loopEnd()
	{
		return 0;
	}
	
	@Override
	public boolean hasRewardInfo()
	{
		return false;
	}
	
	@Override
	public boolean hasChoiceInfo()
	{
		return false;
	}
	
	@Override
	public boolean hasActionInfo()
	{
		return false;
	}
	
	@Override
	public boolean hasTimeInfo()
	{
		return false;
	}
	
	@Override
	public boolean hasLoopInfo()
	{
		return false;
	}
	
	// Standard method
	
	@Override
	public String toString()
	{
		State state;
		int i, n;
		String s = "";
		n = states.size();
		for (i = 0; i < n; i++) { 
			state = states.get(i);
			s += state.toString();
			if (i < n - 1)
				s += "\n";
		}
		return s;
	}
}
