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
import parser.ast.*;

/**
 * Stores and manipulates a path though a model.
 * Minimal info is stored - just enough to allow checking of properties.
 */
public class PathOnTheFly extends Path
{
	// Model to which the path corresponds
	protected ModulesFile modulesFile;
	// Does model use continuous time?
	protected boolean continuousTime;
	// Model info/stats
	protected int numRewardStructs;

	// Path info required to implement Path abstract class:
	protected long size;
	protected State previousState;
	protected State currentState;
	protected int previousModuleOrActionIndex;
	protected double previousProbability;
	protected double totalTime;
	double timeInPreviousState;
	protected double totalRewards[];
	protected double previousStateRewards[];
	protected double previousTransitionRewards[];
	protected double currentStateRewards[];
	
	// Loop detector for path
	protected LoopDetector loopDet;

	/**
	 * Constructor: creates a new (empty) PathOnTheFly object for a specific model.
	 */
	public PathOnTheFly(ModulesFile modulesFile)
	{
		// Store model and info
		this.modulesFile = modulesFile;
		continuousTime = modulesFile.getModelType().continuousTime();
		numRewardStructs = modulesFile.getNumRewardStructs();
		// Create State objects for current/previous state
		previousState = new State(modulesFile.getNumVars());
		currentState = new State(modulesFile.getNumVars());
		// Create arrays to store totals
		totalRewards = new double[numRewardStructs];
		previousStateRewards = new double[numRewardStructs];
		previousTransitionRewards = new double[numRewardStructs];
		currentStateRewards = new double[numRewardStructs];
		// Initialise path info
		clear();
		// Create loop detector
		loopDet = new LoopDetector();
	}

	/**
	 * Clear the path.
	 */
	protected void clear()
	{
		// Initialise all path info
		size = 0;
		previousState.clear();
		currentState.clear();
		totalTime = 0.0;
		timeInPreviousState = 0.0;
		for (int i = 0; i < numRewardStructs; i++) {
			totalRewards[i] = 0.0;
			previousStateRewards[i] = 0.0;
			previousTransitionRewards[i] = 0.0;
			currentStateRewards[i] = 0.0;
		}
	}

	// MUTATORS (for Path)

	@Override
	public void initialise(State initialState, double[] initialStateRewards)
	{
		clear();
		currentState.copy(initialState);
		for (int i = 0; i < numRewardStructs; i++) {
			currentStateRewards[i] = initialStateRewards[i];
		}
		// Initialise loop detector
		loopDet.initialise();
	}

	@Override
	public void addStep(int choice, int moduleOrActionIndex, double probability, double[] transRewards, State newState, double[] newStateRewards, TransitionList transitionList)
	{
		addStep(1.0, choice, moduleOrActionIndex, probability, transRewards, newState, newStateRewards, transitionList);
	}

	@Override
	public void addStep(double time, int choice, int moduleOrActionIndex, double probability, double[] transRewards, State newState, double[] newStateRewards, TransitionList transitionList)
	{
		size++;
		previousState.copy(currentState);
		currentState.copy(newState);
		previousModuleOrActionIndex = moduleOrActionIndex;
		previousProbability = probability;
		totalTime += time;
		timeInPreviousState = time;
		for (int i = 0; i < numRewardStructs; i++) {
			if (continuousTime) {
				totalRewards[i] += currentStateRewards[i] * time + transRewards[i];
			} else {
				totalRewards[i] += currentStateRewards[i] + transRewards[i];
			}
			previousStateRewards[i] = currentStateRewards[i];
			previousTransitionRewards[i] = transRewards[i];
			currentStateRewards[i] = newStateRewards[i];
		}
		// Update loop detector
		loopDet.addStep(this, transitionList);
	}

	// ACCESSORS (for Path)

	@Override
	public boolean continuousTime()
	{
		return continuousTime;
	}
	
	@Override
	public long size()
	{
		return size;
	}

	@Override
	public State getPreviousState()
	{
		return previousState;
	}

	@Override
	public State getCurrentState()
	{
		return currentState;
	}

	@Override
	public int getPreviousModuleOrActionIndex()
	{
		return previousModuleOrActionIndex;
	}

	@Override
	public String getPreviousModuleOrAction()
	{
		int i = getPreviousModuleOrActionIndex();
		if (i < 0)
			return modulesFile.getModuleName(-i - 1);
		else if (i > 0)
			return "[" + modulesFile.getSynchs().get(i - 1) + "]";
		else
			return "?";
	}

	@Override
	public double getPreviousProbability()
	{
		return previousProbability;
	}

	@Override
	public double getTotalTime()
	{
		return totalTime;
	}

	@Override
	public double getTimeInPreviousState()
	{
		return timeInPreviousState;
	}

	@Override
	public double getTotalCumulativeReward(int rsi)
	{
		return totalRewards[rsi];
	}
	
	@Override
	public double getPreviousStateReward(int rsi)
	{
		return previousStateRewards[rsi];
	}
	
	@Override
	public double[] getPreviousStateRewards()
	{
		return previousStateRewards;
	}
	
	@Override
	public double getPreviousTransitionReward(int rsi)
	{
		return previousTransitionRewards[rsi];
	}
	
	@Override
	public double[] getPreviousTransitionRewards()
	{
		return previousTransitionRewards;
	}
	
	@Override
	public double getCurrentStateReward(int rsi)
	{
		return currentStateRewards[rsi];
	}
	
	@Override
	public double[] getCurrentStateRewards()
	{
		return currentStateRewards;
	}
	
	@Override
	public boolean isLooping()
	{
		return loopDet.isLooping();
	}
	
	@Override
	public long loopStart()
	{
		return loopDet.loopStart();
	}
	
	@Override
	public long loopEnd()
	{
		return loopDet.loopEnd();
	}
}
