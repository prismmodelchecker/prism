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

package simulator;

import parser.*;

/**
 * Represents a prefix of an existing PathFull object.
 * Used by Samplers (which expect a Path) to iterate over a whole path. 
 */
public class PathFullPrefix extends Path
{
	// Path of which this a prefix
	private PathFull pathFull;
	// Length (number of states) in the prefix
	private int prefixLength;
	
	/**
	 * Constructor: create from an existing PathFull object and a prefix length.
	 */
	public PathFullPrefix(PathFull pathFull, int prefixLength)
	{
		this.pathFull = pathFull;
		this.prefixLength = prefixLength;
	}

	// MUTATORS (for Path)

	@Override
	public void initialise(State initialState, double[] initialStateRewards)
	{
		// Do nothing (we are not allowed to modify the underlying PathFull)
	}

	@Override
	public void addStep(int choice, int moduleOrActionIndex, double probability, double[] transitionRewards, State newState, double[] newStateRewards, TransitionList transitionList)
	{
		// Do nothing (we are not allowed to modify the underlying PathFull)
	}

	@Override
	public void addStep(double time, int choice, int moduleOrActionIndex, double probability, double[] transitionRewards, State newState, double[] newStateRewards, TransitionList transitionList)
	{
		// Do nothing (we are not allowed to modify the underlying PathFull)
	}

	// MUTATORS (additional)
	
	public void setPrefixLength(int prefixLength)
	{
		this.prefixLength = prefixLength;
	}
	
	// ACCESSORS (for Path)

	@Override
	public boolean continuousTime()
	{
		return pathFull.continuousTime();
	}
	
	@Override
	public long size()
	{
		return prefixLength;
	}

	@Override
	public State getPreviousState()
	{
		return pathFull.getState(prefixLength - 1);
	}

	@Override
	public State getCurrentState()
	{
		return pathFull.getState(prefixLength);
	}

	@Override
	public int getPreviousModuleOrActionIndex()
	{
		return pathFull.getModuleOrActionIndex(prefixLength - 1);
	}

	@Override
	public String getPreviousModuleOrAction()
	{
		return pathFull.getModuleOrAction(prefixLength - 1);
	}
	
	@Override
	public double getPreviousProbability()
	{
		return pathFull.getProbability(prefixLength - 1);
	}

	@Override
	public double getTotalTime()
	{
		return pathFull.getCumulativeTime(prefixLength);
	}

	@Override
	public double getTimeInPreviousState()
	{
		return pathFull.getTime(prefixLength - 1);
	}

	@Override
	public double getTotalCumulativeReward(int rsi)
	{
		return pathFull.getCumulativeReward(prefixLength, rsi);
	}
	
	@Override
	public double getPreviousStateReward(int rsi)
	{
		return pathFull.getStateReward(prefixLength - 1, rsi);
	}
	
	@Override
	public double[] getPreviousStateRewards()
	{
		return pathFull.getStateRewards(prefixLength - 1);
	}
	
	@Override
	public double getPreviousTransitionReward(int rsi)
	{
		return pathFull.getTransitionReward(prefixLength - 1, rsi);
	}
	
	@Override
	public double[] getPreviousTransitionRewards()
	{
		return pathFull.getTransitionRewards(prefixLength - 1);
	}
	
	@Override
	public double getCurrentStateReward(int rsi)
	{
		return pathFull.getStateReward(prefixLength, rsi);
	}
	
	@Override
	public double[] getCurrentStateRewards()
	{
		return pathFull.getStateRewards(prefixLength);
	}
	
	@Override
	public boolean isLooping()
	{
		return pathFull.isLooping() && pathFull.loopEnd() < prefixLength;
	}
	
	@Override
	public long loopStart()
	{
		return isLooping() ? pathFull.loopStart() : -1;
	}
	
	@Override
	public long loopEnd()
	{
		return isLooping() ? pathFull.loopEnd() : -1;
	}
}
