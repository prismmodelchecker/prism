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
 * Classes that store and manipulate a path though a model.
 */
public abstract class Path
{
	// MUTATORS
	
	/**
	 * Initialise the path with an initial state and rewards.
	 * Note: State object and array will be copied, not stored directly.
	 */
	public abstract void initialise(State initialState, double[] initialStateRewards);

	/**
	 * Add a step to the path.
	 * Note: State object and arrays will be copied, not stored directly.
	 */
	public abstract void addStep(int choice, int actionIndex, double probability, double[] transRewards, State newState, double[] newStateRewards, TransitionList transitionList);

	/**
	 * Add a timed step to the path.
	 * Note: State object and arrays will be copied, not stored directly.
	 */
	public abstract void addStep(double time, int choice, int actionIndex, double probability, double[] transRewards, State newState, double[] newStateRewards, TransitionList transitionList);

	// ACCESSORS

	/**
	 * Check whether this path includes continuous-time information, e.g. delays for a CTMC.
	 */
	public abstract boolean continuousTime();

	/**
	 * Get the size of the path (number of steps; or number of states - 1).
	 */
	public abstract long size();

	/**
	 * Get the previous state, i.e. the penultimate state of the current path.
	 */
	public abstract State getPreviousState();

	/**
	 * Get the current state, i.e. the current final state of the path.
	 */
	public abstract State getCurrentState();

	/**
	 * Get the index i of the action taken in the previous step.
	 * If i>0, then i-1 is the index of an action label (0-indexed)
	 * If i<0, then -i-1 is the index of a module (0-indexed)
	 */
	public abstract int getPreviousModuleOrActionIndex();

	/**
	 * Get a string describing the action/module of the previous step.
	 */
	public abstract String getPreviousModuleOrAction();
	
	/**
	 * Get the probability or rate associated wuth the previous step.
	 */
	public abstract double getPreviousProbability();
	
	/**
	 * Get the total time elapsed so far (where zero time has been spent in the current (final) state).
	 * For discrete-time models, this is just the number of steps (but returned as a double).
	 */
	public abstract double getTotalTime();
	
	/**
	 * For paths with continuous-time info, get the time spent in the previous state.
	 */
	public abstract double getTimeInPreviousState();
	
	/**
	 * Get the total reward accumulated so far
	 * (includes reward for previous transition but no state reward for current (final) state).
	 * @param rsi Reward structure index
	 */
	public abstract double getTotalCumulativeReward(int rsi);
	
	/**
	 * Get the state reward for the previous state.
	 * (For continuous-time models, need to multiply this by time spent in the state.)
	 * @param rsi Reward structure index
	 */
	public abstract double getPreviousStateReward(int rsi);
	
	/**
	 * Get the state rewards for the previous state.
	 * (For continuous-time models, need to multiply these by time spent in the state.)
	 */
	public abstract double[] getPreviousStateRewards();
	
	/**
	 * Get the transition reward for the transition between the previous and current states.
	 * @param rsi Reward structure index
	 */
	public abstract double getPreviousTransitionReward(int rsi);

	/**
	 * Get the transition rewards for the transition between the previous and current states.
	 */
	public abstract double[] getPreviousTransitionRewards();

	/**
	 * Get the state reward for the current state.
	 * (For continuous-time models, need to multiply this by time spent in the state.)
	 * @param rsi Reward structure index
	 */
	public abstract double getCurrentStateReward(int rsi);
	
	/**
	 * Get the state rewards for the current state.
	 * (For continuous-time models, need to multiply these by time spent in the state.)
	 */
	public abstract double[] getCurrentStateRewards();
	
	/**
	 * Does the path contain a deterministic loop?
	 */
	public abstract boolean isLooping();
	
	/**
	 * What is the step index of the start of the deterministic loop, if it exists?
	 */
	public abstract long loopStart();
	
	/**
	 * What is the step index of the end of the deterministic loop, if it exists?
	 */
	public abstract long loopEnd();
}
