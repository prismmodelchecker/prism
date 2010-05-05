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
	// Parent simulator engine
	protected SimulatorEngine engine;
	// Model to which the path corresponds
	protected ModulesFile modulesFile;
	// Does model use continuous time?
	protected boolean continuousTime;
	// Model info/stats
	protected int numRewardStructs;

	// Path info required to implement Path abstract class:
	protected int size;
	protected State previousState;
	protected State currentState;
	protected double totalTime;
	double timeInPreviousState;
	protected double totalRewards[];
	protected double previousStateRewards[];
	protected double previousTransitionRewards[];
	protected double currentStateRewards[];
	
	/**
	 * Constructor: creates a new (empty) PathOnTheFly object for a specific model.
	 */
	public PathOnTheFly(SimulatorEngine engine, ModulesFile modulesFile)
	{
		// Store ptr to engine
		this.engine = engine;
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
	}

	@Override
	public void addStep(int choice, String action, double[] transRewards, State newState, double[] newStateRewards)
	{
		addStep(0, choice, action, transRewards, newState, newStateRewards);
	}

	@Override
	public void addStep(double time, int choice, String action, double[] transRewards, State newState, double[] newStateRewards)
	{
		size++;
		previousState.copy(currentState);
		currentState.copy(newState);
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
	}

	// ACCESSORS (for Path)

	@Override
	public int size()
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
	public double getTimeSoFar()
	{
		return totalTime;
	}

	@Override
	public double getTimeInPreviousState()
	{
		return timeInPreviousState;
	}

	@Override
	public double getRewardCumulatedSoFar(int index)
	{
		return totalRewards[index];
	}
	
	@Override
	public double getPreviousStateReward(int index)
	{
		return previousStateRewards[index];
	}
	
	@Override
	public double getPreviousTransitionReward(int index)
	{
		return previousTransitionRewards[index];
	}
	
	@Override
	public double getCurrentStateReward(int index)
	{
		return currentStateRewards[index];
	}
}
