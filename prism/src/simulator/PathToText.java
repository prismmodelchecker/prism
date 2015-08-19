//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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
import parser.ast.ModulesFile;
import prism.PrismFileLog;
import prism.PrismLog;

/**
 * Class to display a simulation path in text form, sending to a PrismLog.
 */
public class PathToText extends PathDisplayer
{
	/** Log to display path to */
	private PrismLog log;

	// Model info
	private ModulesFile modulesFile;
	private int numVars;
	private int numRewardStructs;
	private boolean contTime;

	// Config
	private boolean showTimeCumul = true;
	private boolean showTimeSpent = false;
	private String colSep = " ";

	// Displayer state
	/** Is the next column the first? */
	private boolean firstCol;
	/** Last state */
	private State lastState;
	/** Last state rewards */
	private double[] lastStateRewards;
	/** Did the displayed info change in the current step? */
	private boolean changed;

	/**
	 * Construct a {@link PathToText} object
	 * @param log Log to output path to
	 * @param modulesFile Model associated with path
	 */
	public PathToText(PrismLog log, ModulesFile modulesFile)
	{
		this.log = log;
		this.modulesFile = modulesFile;

		// Get model info
		numVars = modulesFile.getNumVars();
		numRewardStructs = modulesFile.getNumRewardStructs();
		contTime = modulesFile.getModelType().continuousTime();
	}

	// Setters

	/**
	 * Set whether we show cumulative time (for continuous models only)
	 */
	public void setShowTimeCumul(boolean showTimeCumul)
	{
		this.showTimeCumul = showTimeCumul;
	}

	/**
	 * Set whether we show time spent in each state (for continuous models only)
	 */
	public void setShowTimeSpent(boolean showTimeSpent)
	{
		this.showTimeSpent = showTimeSpent;
	}

	/**
	 * Set the column separator for the table of path contents
	 */
	public void setColSep(String colSep)
	{
		this.colSep = colSep;
	}

	// Display methods

	@Override
	public void startDisplay(State initialState, double[] initialStateRewards)
	{
		int j;

		// Display header
		firstCol = true;
		if (!getShowSnapshots()) {
			log.print(getColSep() + "action");
			if (showProbs)
				log.print(getColSep() + "probability");
		}
		log.print(getColSep() + "step");
		if (contTime && showTimeCumul)
			log.print(getColSep() + "time");
		if (varsToShow == null)
			for (j = 0; j < numVars; j++)
				log.print(getColSep() + modulesFile.getVarName(j));
		else
			for (int v : varsToShow)
				log.print(getColSep() + modulesFile.getVarName(v));
		if (getShowRewards()) {
			if (numRewardStructs == 1) {
				log.print(getColSep() + "state_reward");
				if (!getShowSnapshots())
					log.print(getColSep() + "transition_reward");
			} else {
				for (j = 0; j < numRewardStructs; j++) {
					log.print(getColSep() + "state_reward" + (j + 1));
					if (!getShowSnapshots())
						log.print(getColSep() + "transition_reward" + (j + 1));
				}
			}
		}
		if (!getShowSnapshots()) {
			if (contTime && showTimeSpent)
				log.print(getColSep() + "time_in_state");
		}
		log.println();

		// Display initial step
		changed = true;
		firstCol = true;
		if (!getShowSnapshots()) {
			log.print(getColSep() + "-");
			if (showProbs)
				log.print(getColSep() + "-");
		}
		log.print(getColSep() + "0");
		if (contTime && showTimeCumul)
			log.print(getColSep() + "0.0");
		lastState = new State(initialState.varValues.length);
		displayState(initialState);
		if (getShowRewards()) {
			lastStateRewards = explicit.Utils.cloneDoubleArray(initialStateRewards);
		}
		if (getShowSnapshots()) {
			log.println();
		}
	}

	@Override
	public void displayStep(double timeSpent, double timeCumul, Object action, double probability, double[] transitionRewards, long newStateIndex, State newState, double[] newStateRewards)
	{
		if (!showChangesOnly || changed) {
			// display rewards for last state
			if (getShowRewards()) {
				for (int j = 0; j < numRewardStructs; j++) {
					log.print(getColSep() + lastStateRewards[j]);
					log.print(getColSep() + transitionRewards[j]);
				}
			}
			// display time spent in state
			if (contTime && showTimeSpent)
				log.print(getColSep() + timeSpent);
			log.println();
		}

		// if required, check whether the info to be displayed changed
		if (showChangesOnly) {
			changed = stateChanged(lastState, newState) || rewardsChanged(lastStateRewards, newStateRewards);
			if (!changed)
				return;
		}

		firstCol = true;

		// display action
		log.print(getColSep() + action);
		// display probability/rate
		if (showProbs)
			log.print(getColSep() + probability);
		// display state index
		log.print(getColSep() + newStateIndex);
		// display cumulative time
		if (contTime && showTimeCumul)
			log.print(getColSep() + timeCumul);
		// display state
		displayState(newState);
		// store state rewards
		if (getShowRewards()) {
			explicit.Utils.copyDoubleArray(newStateRewards, lastStateRewards);
		}
	}

	@Override
	public void displaySnapshot(double timeCumul, long newStateIndex, State newState, double[] newStateRewards)
	{
		firstCol = true;

		// display state index
		log.print(getColSep() + newStateIndex);
		// display cumulative time
		if (contTime && showTimeCumul)
			log.print(getColSep() + timeCumul);
		// display state
		displayState(newState);
		// display state rewards
		if (getShowRewards()) {
			for (int j = 0; j < numRewardStructs; j++) {
				log.print(getColSep() + newStateRewards[j]);
			}
		}
		log.println();
	}

	private void displayState(State state)
	{
		int j;
		if (varsToShow == null) {
			for (j = 0; j < numVars; j++) {
				log.print(getColSep());
				log.print(state.varValues[j]);
			}
		} else {
			for (int v : varsToShow) {
				log.print(getColSep());
				log.print(state.varValues[v]);
			}
		}
		lastState.copy(state);
	}

	private String getColSep()
	{
		if (firstCol) {
			firstCol = false;
			return "";
		} else {
			return colSep;
		}
	}

	private boolean stateChanged(State lastState, State newState)
	{
		if (varsToShow == null) {
			for (int j = 0; j < numVars; j++) {
				if (!newState.varValues[j].equals(lastState.varValues[j])) {
					return true;
				}
			}
		} else {
			for (int v : varsToShow) {
				if (!newState.varValues[v].equals(lastState.varValues[v])) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean rewardsChanged(double[] lastStateRewards, double[] newStateRewards)
	{
		if (!showRewards)
			return false;
		for (int j = 0; j < numRewardStructs; j++) {
			if (newStateRewards[j] != lastStateRewards[j]) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void endDisplay()
	{
		if (!getShowSnapshots() && (!showChangesOnly || changed)) {
			// display state rewards for last state
			// (transition rewards unknown because no outgoing transition)
			if (getShowRewards()) {
				for (int j = 0; j < numRewardStructs; j++) {
					log.print(getColSep() + lastStateRewards[j]);
					log.print(getColSep() + "?");
				}
			}
			// display (zero) time spent in state
			if (contTime && showTimeSpent)
				log.print(getColSep() + 0.0);
			log.println();
		}
	}
	
	@Override
	public void close()
	{
		if (log instanceof PrismFileLog) {
			log.close();
		}
	}
}
