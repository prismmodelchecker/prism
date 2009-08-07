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

package explicit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import prism.PrismException;

public abstract class Model
{
	// Number of states
	public int numStates;
	// Initial states
	public List<Integer> initialStates; // TODO: should be a (linkedhash?) set really
	
	/**
	 * Initialise (set up any data structures used by all models).
	 */
	public void initialise(int numStates)
	{
		this.numStates = numStates;
		initialStates = new ArrayList<Integer>();
	}
	
	/**
	 * Get the number of states.
	 */
	public int getNumStates()
	{
		return numStates;
	}
	
	/**
	 * Get the number of initial states.
	 */
	public int getNumInitialStates()
	{
		return initialStates.size();
	}
	
	/**
	 * Get iterator over initial state list.
	 */
	public Iterable<Integer> getInitialStates()
	{
		return initialStates;
	}
	
	/**
	 * Get the index of the first initial state
	 * (i.e. the one with the lowest index).
	 * Returns -1 if there are no initial states.
	 */
	public int getFirstInitialState()
	{
		return initialStates.isEmpty() ? -1 : initialStates.get(0);
	}
	
	/**
	 * Check whether a state is an initial state.
	 */
	public boolean isInitialState(int i)
	{
		return initialStates.contains(i);
	}
	
	public abstract String infoString();

	public abstract void clearState(int i);
	
	public abstract int addState();

	public abstract void addStates(int numToAdd);

	/**
	 * Add a state to the list of initial states.
	 */
	public void addInitialState(int i)
	{
		initialStates.add(i);
	}
	
	/**
	 * Set a constant reward for all transitions
	 */
	public abstract void setConstantTransitionReward(double r);
	
	/**
	 * Get the number of nondeterministic choices in state s.
	 */
	public abstract int getNumChoices(int s);
	
	/**
	 * Returns true if state s2 is a successor of state s1.
	 */
	public abstract boolean isSuccessor(int s1, int s2);
	
	/**
	 * Checks for deadlocks and throws an exception if any exist.
	 */
	public void checkForDeadlocks() throws PrismException
	{
		checkForDeadlocks(null);
	}
	
	/**
	 * Checks for deadlocks and throws an exception if any exist.
	 * States in 'except' (If non-null) are excluded from the check.
	 */
	public abstract void checkForDeadlocks(BitSet except) throws PrismException;
	
	/**
	 * Build (anew) from a list of transitions exported explicitly by PRISM (i.e. a .tra file).
	 */
	public abstract void buildFromPrismExplicit(String filename) throws PrismException;

	/**
	 * Export to explicit format readable by PRISM (i.e. a .tra file, etc.).
	 */
	public abstract void exportToPrismExplicit(String baseFilename) throws PrismException;

	public abstract void exportToDotFile(String filename) throws PrismException;

	public abstract void exportToDotFile(String filename, BitSet mark) throws PrismException;
}