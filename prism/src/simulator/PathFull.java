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

import java.util.ArrayList;

import parser.*;
import parser.ast.*;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;

/**
 * Stores and manipulates a path though a model.
 * The full path is stored, i.e. all info at all steps.
 * State objects and arrays are copied for storage.
 */
public class PathFull extends Path
{
	// Parent simulator engine
	protected SimulatorEngine engine;
	// Model to which the path corresponds
	protected ModulesFile modulesFile;
	// Does model use continuous time?
	protected boolean continuousTime;
	// Model info/stats
	protected int numRewardStructs;
	
	// The path, i.e. list of states, etc.
	protected ArrayList<Step> steps;
	// The path length (just for convenience; equal to steps.size() - 1)
	protected int size;

	/**
	 * Constructor: creates a new (empty) PathFull object for a specific model.
	 */
	public PathFull(SimulatorEngine engine, ModulesFile modulesFile)
	{
		// Store ptr to engine
		this.engine = engine;
		// Store model and info
		this.modulesFile = modulesFile;
		continuousTime = modulesFile.getModelType().continuousTime();
		numRewardStructs = modulesFile.getNumRewardStructs();
		// Create list to store path
		steps = new ArrayList<Step>(100);
		// Initialise variables
		clear();
	}

	/**
	 * Clear the path.
	 */
	protected void clear()
	{
		steps.clear();
		size = 0;
	}

	// MUTATORS (for Path)

	@Override
	public void initialise(State initialState, double[] initialStateRewards)
	{
		clear();
		// Add new step item to the path
		Step step = new Step();
		steps.add(step);
		// Add (copies of) initial state and state rewards to new step
		step.state = new State(initialState);
		step.stateRewards = initialStateRewards.clone();
		// Set cumulative time/reward (up until entering this state)
		step.timeCumul = 0.0;
		for (int i = 0; i < numRewardStructs; i++) {
			step.rewardsCumul[i] = 0.0;
		}
	}

	/**
	 * Add a step to the path.
	 * The passed in State object and arrays (of rewards) will be copied to store in the path.
	 */
	@Override
	public void addStep(int choice, String action, double[] transitionRewards, State newState, double[] newStateRewards)
	{
		addStep(0.0, choice, action, transitionRewards, newState, newStateRewards);
	}

	/**
	 * Add a timed step to the path.
	 * The passed in State object and arrays (of rewards) will be copied to store in the path.
	 */
	@Override
	public void addStep(double time, int choice, String action, double[] transitionRewards, State newState, double[] newStateRewards)
	{
		Step stepOld, stepNew;
		// Add info to last existing step
		stepOld = steps.get(steps.size() - 1);
		stepOld.time = time;
		stepOld.choice = choice;
		stepOld.action = action;
		stepOld.transitionRewards = transitionRewards.clone();
		// Add new step item to the path
		stepNew = new Step();
		steps.add(stepNew);
		// Add (copies of) new state and state rewards to new step
		stepNew.state = new State(newState);
		stepNew.stateRewards = newStateRewards.clone();
		// Set cumulative time/rewards (up until entering this state)
		stepNew.timeCumul = stepOld.timeCumul + time;
		for (int i = 0; i < numRewardStructs; i++) {
			stepNew.rewardsCumul[i] = stepOld.rewardsCumul[i];
			if (continuousTime)
				stepNew.rewardsCumul[i] += stepOld.stateRewards[i] * time;
			else
				stepNew.rewardsCumul[i] += stepOld.stateRewards[i];
			stepNew.rewardsCumul[i] += transitionRewards[i];
		}
		// Update size too
		size++;
	}

	// MUTATORS (additional)
	
	public void backtrack(int step)
	{
		
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
		return steps.get(steps.size() - 2).state;
	}

	@Override
	public State getCurrentState()
	{
		return steps.get(steps.size() - 1).state;
	}

	@Override
	public double getTotalTime()
	{
		return steps.get(steps.size() - 1).timeCumul;
	}

	@Override
	public double getTimeInPreviousState()
	{
		return steps.get(steps.size() - 2).time;
	}

	@Override
	public double getTotalCumulativeReward(int rsi)
	{
		return steps.get(steps.size() - 1).rewardsCumul[rsi];
	}
	
	@Override
	public double getPreviousStateReward(int rsi)
	{
		return steps.get(steps.size() - 2).stateRewards[rsi];
	}
	
	@Override
	public double getPreviousTransitionReward(int rsi)
	{
		return steps.get(steps.size() - 2).transitionRewards[rsi];
	}
	
	@Override
	public double getCurrentStateReward(int rsi)
	{
		return steps.get(steps.size() - 1).stateRewards[rsi];
	}
	
	// ACCESSORS (additional)

	/**
	 * Get the state at a given step of the path.
	 * @param step Step index (0 = initial state/step of path)
	 */
	public State getState(int step)
	{
		return steps.get(step).state;
	}

	/**
	 * Get a state reward for the state at a given step of the path.
	 * @param step Step index (0 = initial state/step of path)
	 * @param rsi Reward structure index
	 */
	public double getStateReward(int step, int rsi)
	{
		return steps.get(step).stateRewards[rsi];
	}

	/**
	 * Get the total time spent up until entering a given step of the path.
	 * @param step Step index (0 = initial state/step of path)
	 */
	public double getCumulativeTime(int step)
	{
		return steps.get(step).timeCumul;
	}

	/**
	 * Get the total (state and transition) reward accumulated up until entering a given step of the path.
	 * @param step Step index (0 = initial state/step of path)
	 * @param rsi Reward structure index
	 */
	public double getCumulativeReward(int step, int rsi)
	{
		return steps.get(step).rewardsCumul[rsi];
	}

	/**
	 * Get the time spent in a state at a given step of the path.
	 * @param step Step index (0 = initial state/step of path)
	 */
	public double getTime(int step)
	{
		return steps.get(step).time;
	}

	/**
	 * Get the index of the choice taken for a given step.
	 * @param step Step index (0 = initial state/step of path)
	 */
	public int getChoice(int step)
	{
		return steps.get(step).choice;
	}

	/**
	 * Get the action label taken for a given step.
	 * @param step Step index (0 = initial state/step of path)
	 */
	public String getAction(int step)
	{
		return steps.get(step).action;
	}

	/**
	 * Get a transition reward associated with a given step.
	 * @param step Step index (0 = initial state/step of path)
	 * @param rsi Reward structure index
	 */
	public double getTransitionReward(int step, int rsi)
	{
		return steps.get(step).transitionRewards[rsi];
	}

	/**
	 * Export path to a file.
	 * @param log: PrismLog to which the path should be exported to.
	 * @param timeCumul: Show time in cumulative form?
	 * @param colSep: String used to separate columns in display
	 * @param vars: Restrict printing to these variables (indices) and steps which change them (ignore if null)
	 */
	public void exportToLog(PrismLog log, boolean timeCumul, String colSep, ArrayList<Integer> vars) throws PrismException
	{
		int i, j, n, nv;
		double d, t;
		boolean stochastic = (modulesFile.getModelType() == ModelType.CTMC);
		boolean changed;
		int varsNum = 0, varsIndices[] = null;

		if (modulesFile == null) {
			log.flush();
			log.close();
			return;
		}

		// Get sizes
		n = size();
		nv = engine.getNumVariables();

		// if necessary, store info about which vars to display
		if (vars != null) {
			varsNum = vars.size();
			varsIndices = new int[varsNum];
			for (i = 0; i < varsNum; i++)
				varsIndices[i] = vars.get(i);
		}

		// Write header
		log.print("step");
		if (vars == null)
			for (j = 0; j < nv; j++)
				log.print(colSep + engine.getVariableName(j));
		else
			for (j = 0; j < varsNum; j++)
				log.print(colSep + engine.getVariableName(varsIndices[j]));
		if (stochastic)
			log.print(colSep + (timeCumul ? "time" : "time_in_state"));
		if (numRewardStructs == 1) {
			log.print(colSep + "state_reward" + colSep + "transition_reward");
		} else {
			for (j = 0; j < numRewardStructs; j++)
				log.print(colSep + "state_reward" + (j + 1) + colSep + "transition_reward" + (j + 1));
		}
		log.println();

		// Write path
		t = 0.0;
		for (i = 0; i < n; i++) {
			// (if required) see if relevant vars have changed
			if (vars != null && i > 0) {
				changed = false;
				for (j = 0; j < varsNum; j++) {
					if (!getState(i).varValues[j].equals(getState(i - 1).varValues[j]))
						changed = true;
				}
				if (!changed) {
					d = (i < n - 1) ? getTime(i) : 0.0;
					t += d;
					continue;
				}
			}
			// write state index
			log.print(i);
			// write vars
			if (vars == null) {
				for (j = 0; j < nv; j++) {
					log.print(colSep);
					log.print(getState(i).varValues[j]);
				}
			} else {
				for (j = 0; j < varsNum; j++) {
					log.print(colSep);
					log.print(getState(i).varValues[varsIndices[j]]);
				}
			}
			if (stochastic) {
				d = (i < n - 1) ? getTime(i) : 0.0;
				log.print(colSep + (timeCumul ? t : d));
				t += d;
			}
			for (j = 0; j < numRewardStructs; j++) {
				log.print(colSep + ((i < n - 1) ? getStateReward(i, j) : 0.0));
				log.print(colSep + ((i < n - 1) ? getTransitionReward(i, j) : 0.0));
			}
			log.println();
		}

		log.flush();
		log.close();
	}

	@Override
	public String toString()
	{
		int i;
		String s = "";
		for (i = 0; i < size; i++) {
			s += getState(i) + "\n";
		}
		return s;
	}

	/**
	 * Inner class to store info about a single path step.
	 */
	class Step
	{
		public Step()
		{
			stateRewards = new double[numRewardStructs];
			rewardsCumul = new double[numRewardStructs];
			transitionRewards = new double[numRewardStructs];
		}
		// Current state (before transition)
		public State state;
		// State rewards for current state
		public double stateRewards[];
		// Cumulative time spent up until entering this state
		public double timeCumul;
		// Cumulative rewards spent up until entering this state
		public double rewardsCumul[];
		// Time spent in state
		public double time;
		// Index of the choice taken
		public int choice;
		// Action label taken
		public String action;
		// Transition rewards associated with step
		public double transitionRewards[];
	}
}
