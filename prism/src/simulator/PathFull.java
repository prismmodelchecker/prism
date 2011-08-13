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
import prism.PrismException;
import prism.PrismLog;

/**
 * Stores and manipulates a path though a model.
 * The full path is stored, i.e. all info at all steps.
 * State objects and arrays are copied for storage.
 */
public class PathFull extends Path implements PathFullInfo
{
	// Model to which the path corresponds
	private ModulesFile modulesFile;
	// Does model use continuous time?
	private boolean continuousTime;
	// Model info/stats
	private int numRewardStructs;
	
	// The path, i.e. list of states, etc.
	private ArrayList<Step> steps;
	// The path length (just for convenience; equal to steps.size() - 1)
	private int size;
	
	// Loop detector for path
	protected LoopDetector loopDet;

	/**
	 * Constructor: creates a new (empty) PathFull object for a specific model.
	 */
	public PathFull(ModulesFile modulesFile)
	{
		// Store model and info
		this.modulesFile = modulesFile;
		continuousTime = modulesFile.getModelType().continuousTime();
		numRewardStructs = modulesFile.getNumRewardStructs();
		// Create list to store path
		steps = new ArrayList<Step>(100);
		// Initialise variables
		clear();
		// Create loop detector
		loopDet = new LoopDetector();
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
		// Initialise loop detector
		loopDet.initialise();
	}

	@Override
	public void addStep(int choice, int moduleOrActionIndex, double[] transitionRewards, State newState, double[] newStateRewards, TransitionList transitionList)
	{
		addStep(0.0, choice, moduleOrActionIndex, transitionRewards, newState, newStateRewards, transitionList);
	}

	@Override
	public void addStep(double time, int choice, int moduleOrActionIndex, double[] transitionRewards, State newState, double[] newStateRewards, TransitionList transitionList)
	{
		Step stepOld, stepNew;
		// Add info to last existing step
		stepOld = steps.get(steps.size() - 1);
		stepOld.time = time;
		stepOld.choice = choice;
		stepOld.moduleOrActionIndex = moduleOrActionIndex;
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
		// Update loop detector
		loopDet.addStep(this, transitionList);
	}

	// MUTATORS (additional)
	
	/**
	 * Backtrack to a particular step within the current path.
	 * @param step The step of the path to backtrack to (step >= 0)
	 */
	public void backtrack(int step)
	{
		int i, n;
		// Remove steps after index 'step'
		n = steps.size() - 1;
		for (i = n; i > step; i--)
			steps.remove(i);
		// Update info in last step of path
		Step last = steps.get(steps.size() - 1);
		last.time = 0.0;
		last.choice = -1;
		last.moduleOrActionIndex = 0;
		for (i = 0; i < numRewardStructs; i++)
			last.transitionRewards[i] = 0.0;
		// Update size too
		size = step;
		// Update loop detector
		loopDet.backtrack(this);
	}
	
	/**
	 * Remove the prefix of the current path up to the given path step.
	 * Index step should be >=0 and <= the total path size. 
	 * @param step The step before which states will be removed.
	 */
	public void removePrecedingStates(int step)
	{
		int i, j, numKeep, sizeOld;
		double timeCumul, rewardsCumul[];
		
		// Ignore trivial case
		if (step == 0)
			return;
		// Get cumulative time/reward for index 'step'
		timeCumul = getCumulativeTime(step);
		rewardsCumul = new double[numRewardStructs];
		for (j = 0; j < numRewardStructs; j++)
			rewardsCumul[j] = getCumulativeReward(step, j);
		// Move later steps of path 'step' places forward 
		// and subtract time/reward as appropriate 
		numKeep = steps.size() - step;
		for (i = 0; i < numKeep; i++) {
			Step tmp = steps.get(i + step);
			tmp.timeCumul -= timeCumul;
			for (j = 0; j < numRewardStructs; j++)
				tmp.rewardsCumul[j] -= rewardsCumul[j];
			steps.set(i, tmp);
		}
		// Remove steps after index 'step'
		sizeOld = steps.size();
		for (i = sizeOld - 1; i >= numKeep; i--)
			steps.remove(i);
		// Update size too
		size = steps.size() - 1;
		// Update loop detector
		loopDet.removePrecedingStates(this, step);
	}
	
	// ACCESSORS (for Path (and some of PathFullInfo))

	@Override
	public boolean continuousTime()
	{
		return continuousTime;
	}
	
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
	
	@Override
	public boolean isLooping()
	{
		return loopDet.isLooping();
	}
	
	@Override
	public int loopStart()
	{
		return loopDet.loopStart();
	}
	
	@Override
	public int loopEnd()
	{
		return loopDet.loopEnd();
	}
	
	// ACCESSORS (for PathFullInfo)

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
	 * Get the index i of the action taken for a given step.
	 * If i>0, then i-1 is the index of an action label (0-indexed)
	 * If i<0, then -i-1 is the index of a module (0-indexed)
	 * @param step Step index (0 = initial state/step of path)
	 */
	public int getModuleOrActionIndex(int step)
	{
		return steps.get(step).moduleOrActionIndex;
	}

	/**
	 * Get a string describing the action/module of a given step.
	 * @param step Step index (0 = initial state/step of path)
	 */
	public String getModuleOrAction(int step)
	{
		int i = steps.get(step).moduleOrActionIndex;
		if (i < 0)
			return modulesFile.getModuleName(-i - 1);
		else if (i > 0)
			return "[" + modulesFile.getSynchs().get(i - 1) + "]";
		else
			return "?";
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

	// Other methods
	
	/**
	 * Export path to a file.
	 * @param log PrismLog to which the path should be exported to.
	 * @param timeCumul Show time in cumulative form?
	 * @param colSep String used to separate columns in display
	 * @param vars Restrict printing to these variables (indices) and steps which change them (ignore if null)
	 */
	public void exportToLog(PrismLog log, boolean timeCumul, String colSep, ArrayList<Integer> vars) throws PrismException
	{
		int i, j, n, nv;
		double d, t;
		boolean contTime = modulesFile.getModelType().continuousTime();
		boolean changed;
		int varsNum = 0, varsIndices[] = null;

		if (modulesFile == null) {
			log.flush();
			log.close();
			return;
		}

		// Get sizes
		n = size();
		nv = modulesFile.getNumVars();

		// if necessary, store info about which vars to display
		if (vars != null) {
			varsNum = vars.size();
			varsIndices = new int[varsNum];
			for (i = 0; i < varsNum; i++)
				varsIndices[i] = vars.get(i);
		}

		// Write header
		log.print("action");
		log.print(colSep + "step");
		if (contTime)
			log.print(colSep + (timeCumul ? "time" : "time_in_state"));
		if (vars == null)
			for (j = 0; j < nv; j++)
				log.print(colSep + modulesFile.getVarName(j));
		else
			for (j = 0; j < varsNum; j++)
				log.print(colSep + modulesFile.getVarName(varsIndices[j]));
		if (numRewardStructs == 1) {
			log.print(colSep + "state_reward" + colSep + "transition_reward");
		} else {
			for (j = 0; j < numRewardStructs; j++)
				log.print(colSep + "state_reward" + (j + 1) + colSep + "transition_reward" + (j + 1));
		}
		log.println();

		// Write path
		t = 0.0;
		for (i = 0; i <= n; i++) {
			// (if required) see if relevant vars have changed
			if (vars != null && i > 0) {
				changed = false;
				for (j = 0; j < varsNum; j++) {
					if (!getState(i).varValues[varsIndices[j]].equals(getState(i - 1).varValues[varsIndices[j]]))
						changed = true;
				}
				if (!changed) {
					d = (i < n - 1) ? getTime(i) : 0.0;
					t += d;
					continue;
				}
			}
			// write action
			log.print(i == 0 ? "-" : getModuleOrAction(i - 1));
			// write state index
			log.print(colSep);
			log.print(i);
			// print time (if continuous time)
			if (contTime) {
				d = (i < n - 1) ? getTime(i) : 0.0;
				log.print(colSep + (timeCumul ? t : d));
				t += d;
			}
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
			// write rewards
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
			// Set (unknown) defaults and initialise arrays
			state = null;
			stateRewards = new double[numRewardStructs];
			timeCumul = 0.0;
			rewardsCumul = new double[numRewardStructs];
			time = 0.0;
			choice = -1;
			moduleOrActionIndex = 0;
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
		// Action label taken (i.e. the index in the model's list of all actions).
		// This is 1-indexed, with 0 denoting an independent ("tau"-labelled) command.
		public int moduleOrActionIndex;
		// Transition rewards associated with step
		public double transitionRewards[];
	}
}
