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

import parser.State;
import parser.ast.ModulesFile;
import prism.PrismException;
import prism.PrismLog;
import userinterface.graph.Graph;

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
	public void addStep(int choice, int moduleOrActionIndex, double probability, double[] transitionRewards, State newState, double[] newStateRewards,
			TransitionList transitionList)
	{
		addStep(1.0, choice, moduleOrActionIndex, probability, transitionRewards, newState, newStateRewards, transitionList);
	}

	@Override
	public void addStep(double time, int choice, int moduleOrActionIndex, double probability, double[] transitionRewards, State newState,
			double[] newStateRewards, TransitionList transitionList)
	{
		Step stepOld, stepNew;
		// Add info to last existing step
		stepOld = steps.get(steps.size() - 1);
		stepOld.time = time;
		stepOld.choice = choice;
		stepOld.moduleOrActionIndex = moduleOrActionIndex;
		stepOld.probability = probability;
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
		last.probability = 0.0;
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
	public long size()
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
	public int getPreviousModuleOrActionIndex()
	{
		return steps.get(steps.size() - 2).moduleOrActionIndex;
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
		return steps.get(steps.size() - 2).probability;
	}

	@Override
	public double getTotalTime()
	{
		return size < 1 ? 0.0 : steps.get(steps.size() - 1).timeCumul;
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
	public double[] getPreviousStateRewards()
	{
		return steps.get(steps.size() - 2).stateRewards;
	}

	@Override
	public double getPreviousTransitionReward(int rsi)
	{
		return steps.get(steps.size() - 2).transitionRewards[rsi];
	}

	@Override
	public double[] getPreviousTransitionRewards()
	{
		return steps.get(steps.size() - 2).transitionRewards;
	}

	@Override
	public double getCurrentStateReward(int rsi)
	{
		return steps.get(steps.size() - 1).stateRewards[rsi];
	}

	@Override
	public double[] getCurrentStateRewards()
	{
		return steps.get(steps.size() - 1).stateRewards;
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

	// ACCESSORS (for PathFullInfo)

	@Override
	public State getState(int step)
	{
		return steps.get(step).state;
	}

	@Override
	public double getStateReward(int step, int rsi)
	{
		return steps.get(step).stateRewards[rsi];
	}

	/**
	 * Get an array of state rewards for the state at a given step of the path.
	 * @param step Step index (0 = initial state/step of path)
	 */
	protected double[] getStateRewards(int step)
	{
		return steps.get(step).stateRewards;
	}

	@Override
	public double getCumulativeTime(int step)
	{
		return steps.get(step).timeCumul;
	}

	@Override
	public double getCumulativeReward(int step, int rsi)
	{
		return steps.get(step).rewardsCumul[rsi];
	}

	@Override
	public double getTime(int step)
	{
		return steps.get(step).time;
	}

	@Override
	public int getChoice(int step)
	{
		return steps.get(step).choice;
	}

	@Override
	public int getModuleOrActionIndex(int step)
	{
		return steps.get(step).moduleOrActionIndex;
	}

	@Override
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
	 * Get the probability or rate associated with a given step.
	 * @param step Step index (0 = initial state/step of path)
	 */
	public double getProbability(int step)
	{
		return steps.get(steps.size() - 2).probability;
	}

	@Override
	public double getTransitionReward(int step, int rsi)
	{
		return steps.get(step).transitionRewards[rsi];
	}

	/**
	 * Get an array of transitions reward associated with a given step.
	 * @param step Step index (0 = initial state/step of path)
	 */
	protected double[] getTransitionRewards(int step)
	{
		return steps.get(step).transitionRewards;
	}

	@Override
	public boolean hasRewardInfo()
	{
		return true;
	}

	@Override
	public boolean hasChoiceInfo()
	{
		return true;
	}

	@Override
	public boolean hasActionInfo()
	{
		return true;
	}

	@Override
	public boolean hasTimeInfo()
	{
		return true;
	}

	@Override
	public boolean hasLoopInfo()
	{
		return true;
	}

	// Other methods

	/**
	 * Pass the path to a PathDisplayer object.
	 * @param displayer The PathDisplayer
	 */
	public void display(PathDisplayer displayer) throws PrismException
	{
		// In the absence of model info, do nothing
		if (modulesFile == null) {
			return;
		}
		// Display path
		displayer.start(getState(0), getStateRewards(0));
		// Get length (non-on-the-fly paths will never exceed length Integer.MAX_VALUE) 
		long nLong = size();
		if (nLong > Integer.MAX_VALUE)
			throw new PrismException("PathFull cannot deal with paths over length " + Integer.MAX_VALUE);
		int n = (int) nLong;
		// Loop
		for (int i = 1; i <= n; i++) {
			displayer.step(getTime(i - 1), getCumulativeTime(i), getModuleOrAction(i - 1), getProbability(i - 1), getTransitionRewards(i), i, getState(i),
					getStateRewards(i));
		}
		displayer.end();
	}

	/**
	 * Pass the path to a PathDisplayer object, running in a new thread. 
	 * @param displayer The PathDisplayer
	 */
	public void displayThreaded(PathDisplayer displayer) throws PrismException
	{
		// In the absence of model info, do nothing
		if (modulesFile == null) {
			return;
		}
		// Display path
		new DisplayThread(displayer).start();
	}

	/**
	 * Export path to a PrismLog (e.g. file, stdout).
	 * @param log PrismLog to which the path should be exported to.
	 * @param showTimeCumul Show time in cumulative form?
	 * @param colSep String used to separate columns in display
	 * @param vars Restrict printing to these variables (indices) and steps which change them (ignore if null)
	 */
	public void exportToLog(PrismLog log, boolean showTimeCumul, String colSep, ArrayList<Integer> vars) throws PrismException
	{
		exportToLog(log, showTimeCumul, false, colSep, vars);
	}

	/**
	 * Export path to a PrismLog (e.g. file, stdout).
	 * @param log PrismLog to which the path should be exported to.
	 * @param showTimeCumul Show time in cumulative form?
	 * @param showRewards Show rewards?
	 * @param colSep String used to separate columns in display
	 * @param vars Restrict printing to these variables (indices) and steps which change them (ignore if null)
	 */
	public void exportToLog(PrismLog log, boolean showTimeCumul, boolean showRewards, String colSep, ArrayList<Integer> vars) throws PrismException
	{
		PathToText displayer = new PathToText(log, modulesFile);
		displayer.setShowTimeCumul(showTimeCumul);
		displayer.setColSep(colSep);
		displayer.setVarsToShow(vars);
		displayer.setShowRewards(showRewards);
		display(displayer);
	}

	/**
	 * Plot path on a graph.
	 * @param graphModel Graph on which to plot path
	 */
	public void plotOnGraph(Graph graphModel) throws PrismException
	{
		PathToGraph displayer = new PathToGraph(graphModel, modulesFile);
		displayThreaded(displayer);
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
			probability = 0.0;
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
		// Probability or rate of step
		public double probability;
		// Transition rewards associated with step
		public double transitionRewards[];
	}

	class DisplayThread extends Thread
	{
		private PathDisplayer displayer = null;

		public DisplayThread(PathDisplayer displayer)
		{
			this.displayer = displayer;
		}

		public void run()
		{
			try {
				display(displayer);
			} catch (PrismException e) {
				// Just ignore problems
			}
		}
	}
}
