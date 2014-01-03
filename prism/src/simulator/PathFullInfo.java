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

import parser.State;

/**
 * Interface for classes that provide (read) access to a path through a model.
 */
public interface PathFullInfo
{
	/**
	 * Get the size of the path (number of steps; or number of states - 1).
	 */
	public abstract long size();
	
	/**
	 * Get the state at a given step of the path.
	 * @param step Step index (0 = initial state/step of path)
	 */
	public abstract State getState(int step);

	/**
	 * Get a state reward for the state at a given step of the path.
	 * If no reward info is stored ({@link #hasRewardInfo()} is false), returns 0.0. 
	 * @param step Step index (0 = initial state/step of path)
	 * @param rsi Reward structure index
	 */
	public abstract double getStateReward(int step, int rsi);

	/**
	 * Get the total time spent up until entering a given step of the path.
	 * If no time info is stored ({@link #hasTimeInfo()} is false), returns 0.0. 
	 * @param step Step index (0 = initial state/step of path)
	 */
	public abstract double getCumulativeTime(int step);

	/**
	 * Get the total (state and transition) reward accumulated up until entering a given step of the path.
	 * If no reward info is stored ({@link #hasRewardInfo()} is false), returns 0.0. 
	 * @param step Step index (0 = initial state/step of path)
	 * @param rsi Reward structure index
	 */
	public abstract double getCumulativeReward(int step, int rsi);

	/**
	 * Get the time spent in a state at a given step of the path.
	 * If no time info is stored ({@link #hasTimeInfo()} is false), returns 0.0. 
	 * @param step Step index (0 = initial state/step of path)
	 */
	public abstract double getTime(int step);

	/**
	 * Get the index of the choice taken for a given step.
	 * If no choice info is stored ({@link #hasChoiceInfo()} is false), returns 0. 
	 * @param step Step index (0 = initial state/step of path)
	 */
	public abstract int getChoice(int step);

	/**
	 * Get the index i of the action taken for a given step.
	 * If i>0, then i-1 is the index of an action label (0-indexed)
	 * If i<0, then -i-1 is the index of a module (0-indexed)
	 * If no action info is stored ({@link #hasActionInfo()} is false), returns 0. 
	 * @param step Step index (0 = initial state/step of path)
	 */
	public abstract int getModuleOrActionIndex(int step);

	/**
	 * Get a string describing the action/module of a given step.
	 * If no action info is stored ({@link #hasActionInfo()} is false), returns "". 
	 * @param step Step index (0 = initial state/step of path)
	 */
	public abstract String getModuleOrAction(int step);

	/**
	 * Get a transition reward associated with a given step.
	 * If no reward info is stored ({@link #hasRewardInfo()} is false), returns 0.0. 
	 * @param step Step index (0 = initial state/step of path)
	 * @param rsi Reward structure index
	 */
	public abstract double getTransitionReward(int step, int rsi);
	
	/**
	 * Does the path contain a deterministic loop?
	 * If no loop info is stored ({@link #hasLoopInfo()} is false), returns false. 
	 */
	public abstract boolean isLooping();
	
	/**
	 * What is the step index of the start of the deterministic loop, if it exists?
	 * If no loop info is stored ({@link #hasLoopInfo()} is false), returns 0. 
	 */
	public abstract long loopStart();
	
	/**
	 * What is the step index of the end of the deterministic loop, if it exists?
	 * If no loop info is stored ({@link #hasLoopInfo()} is false), returns 0. 
	 */
	public abstract long loopEnd();
	
	/**
	 * Does this object store information about rewards?
	 */
	public abstract boolean hasRewardInfo();
	
	/**
	 * Does this object store information about which choices were taken?
	 */
	public abstract boolean hasChoiceInfo();
	
	/**
	 * Does this object store information about rewards?
	 */
	public abstract boolean hasActionInfo();
	
	/**
	 * Does this object store information about time elapse (for continuous-time models)?
	 */
	public abstract boolean hasTimeInfo();
	
	/**
	 * Does this object store information about loops in the path?
	 */
	public abstract boolean hasLoopInfo();
}
