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

import java.util.ArrayList;
import java.util.List;

import parser.State;

/**
 * Abstract class for classes that "display" a simulation path.
 */
public abstract class PathDisplayer
{
	/** Should we display (timed) snapshots, rather than steps? */
	protected boolean showSnapshots = false;
	/** If we are displaying snapshots, how often? */
	protected double snapshotTimeStep = 0.0;
	/** If we are displaying snapshots, when is the next one due? */
	protected double nextTime = 0.0;

	/** Should we show changes in state only, or all steps? (not for snapshot mode) */
	protected boolean showChangesOnly = true;
	
	/** Indices of variables to show (null = all) */
	protected List<Integer> varsToShow = null;

	/** Should we show transition probabilities/rates? */
	protected boolean showProbs = false;
	
	/** Should we display rewards? */
	protected boolean showRewards = false;

	// Getters for config

	/**
	 * Should we display (timed) snapshots, rather than steps?
	 */
	public boolean getShowSnapshots()
	{
		return showSnapshots;
	}

	/**
	 * If we are displaying snapshots, how often?
	 */
	public double getSnapshotTimeStep()
	{
		return snapshotTimeStep;
	}

	/**
	 * Should we show changes in state only, or all steps? (not for snapshot mode)
	 */
	public boolean getShowChangesOnly()
	{
		return showChangesOnly;
	}
	
	/**
	 * Should we display transition probabilities/rates?
	 */
	public boolean getShowProbs()
	{
		return showProbs;
	}
	
	/**
	 * Should we display rewards?
	 */
	public boolean getShowRewards()
	{
		return showRewards;
	}
	
	// Setters for config

	/**
	 * Set to display (timed) snapshots, rather than steps.
	 */
	public void setToShowSteps()
	{
		this.showSnapshots = false;
	}

	/**
	 * Set to display steps, rather than (timed) snapshots.
	 */
	public void setToShowSnapShots(double timeStep)
	{
		this.showSnapshots = true;
		this.snapshotTimeStep = timeStep;
	}

	/**
	 * Set whether we show changes in state only, or all steps (not for snapshot mode).
	 */
	public void setShowChangesOnly(boolean showChangesOnly)
	{
		this.showChangesOnly = showChangesOnly;
	}

	/**
	 * Set (indices of) vars to show values of (null = all).
	 */
	public void setVarsToShow(List<Integer> varsToShow)
	{
		// Take a copy of var index list
		this.varsToShow = varsToShow == null ? null : new ArrayList<Integer>(varsToShow);
	}

	/**
	 * Set whether we show the probability/rate for each transition
	 */
	public void setShowProbs(boolean showProbs)
	{
		this.showProbs = showProbs;
	}

	/**
	 * Set whether we we display rewards.
	 */
	public void setShowRewards(boolean showRewards)
	{
		this.showRewards = showRewards;
	}

	// Methods called by path owner

	public void start(State initialState, double[] initialStateRewards)
	{
		startDisplay(initialState, initialStateRewards);
		if (showSnapshots) {
			nextTime = snapshotTimeStep;
		}
	}

	public void step(double timeSpent, double timeCumul, Object action, double probability, double[] transitionRewards, long newStateIndex, State newState, double[] newStateRewards)
	{
		if (showSnapshots) {
			if (timeCumul < nextTime) {
				return;
			} else {
				while (timeCumul >= nextTime) {
					displaySnapshot(nextTime, newStateIndex, newState, newStateRewards);
					nextTime += snapshotTimeStep;
				}
			}
		} else {
			displayStep(timeSpent, timeCumul, action, probability, transitionRewards, newStateIndex, newState, newStateRewards);
		}
	}

	public void end()
	{
		endDisplay();
	}

	// Display methods to be implemented by subclasses

	/**
	 * Start displaying a path beginning with state {@code initialState}.
	 */
	public abstract void startDisplay(State initialState, double[] initialStateRewards);

	/**
	 * Displaying a step of a path.
	 */
	public abstract void displayStep(double timeSpent, double timeCumul, Object action, double probability, double[] transitionRewards, long newStateIndex, State newState, double[] newStateRewards);

	/**
	 * Displaying a snapshot of a path at a particular time instant.
	 */
	public abstract void displaySnapshot(double timeCumul, long newStateIndex, State newState, double[] newStateRewards);

	/**
	 * Finish displaying a path.
	 */
	public abstract void endDisplay();

	/**
	 * "Close" this displayer (e.g., if it is to a file, close the file).
	 */
	public void close()
	{
		// By default, do nothing
	}
}
