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

import prism.ModelGenerator;
import prism.PrismException;

/**
 * Detects deterministic loops in a path though a model.
 * (Currently, only detects single-step loops.)
 */
public class LoopDetector
{
	// Deterministic loop info
	private boolean isLooping;
	private long loopStart;
	private long loopEnd;

	/**
	 * Initialise the loop detector.
	 */
	public void initialise()
	{
		isLooping = false;
		loopStart = loopEnd = -1;
	}
	
	/**
	 * Update loop detector after a step has just been added to the path.
	 */
	public void addStep(Path path, ModelGenerator modelGen)
	
	{
		// If already looping, nothing to do
		if (isLooping) {
			return;
		}
		// Deterministic loops cannot occur in continuous-time models
		if (path.continuousTime()) {
			return;
		}
		// Check transitions from previous step were deterministic
		try {
			if (!modelGen.isDeterministic()) {
				return;
			}
		} catch (PrismException e) {
			// In case of problems, just don't check
			return;
		}
		// Check successive states were identical
		if (path.getPreviousState().equals(path.getCurrentState())) {
			isLooping = true;
			loopStart = path.size() - 1;
			loopEnd = path.size();
		}
	}
	
	/**
	 * Update loop detector after a backtrack within the path has been made.
	 */
	public void backtrack(Path path)
	{
		if (isLooping) {
			if (path.size() < loopEnd) {
				isLooping = false;
				loopStart = loopEnd = -1;
			}
		}
	}
	
	/**
	 * Update loop detector after a prefix of the path has been removed.
	 * @param path Path object (already updated)
	 * @param step Index of old path that is now start of path (i.e. num states removed)
	 */
	public void removePrecedingStates(Path path, int step)
	{
		if (isLooping) {
			if (step > loopStart) {
				isLooping = false;
				loopStart = loopEnd = -1;
			} else {
				loopStart -= step;
				loopEnd -= step;
			}
		}
	}
	
	/**
	 * Does the path contain a deterministic loop?
	 */
	public boolean isLooping()
	{
		return isLooping;
	}
	
	/**
	 * What is the step index of the start of the deterministic loop, if it exists?
	 */
	public long loopStart()
	{
		return loopStart;
	}
	
	/**
	 * What is the step index of the end of the deterministic loop, if it exists?
	 */
	public long loopEnd()
	{
		return loopEnd;
	}
}
