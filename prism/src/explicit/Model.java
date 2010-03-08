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

import java.util.*;

import prism.PrismException;

/**
 * Interface for (abstract) classes that provide (read-only) access to an explicit-state model.
 */
public interface Model
{
	// Accessors

	/**
	 * Get the number of states.
	 */
	public int getNumStates();

	/**
	 * Get the number of initial states.
	 */
	public int getNumInitialStates();

	/**
	 * Get iterator over initial state list.
	 */
	public Iterable<Integer> getInitialStates();

	/**
	 * Get the index of the first initial state
	 * (i.e. the one with the lowest index).
	 * Returns -1 if there are no initial states.
	 */
	public int getFirstInitialState();

	/**
	 * Check whether a state is an initial state.
	 */
	public boolean isInitialState(int i);

	/**
	 * Get the total number of transitions in the model.
	 */
	public int getNumTransitions();

	/**
	 * Returns true if state s2 is a successor of state s1.
	 */
	public boolean isSuccessor(int s1, int s2);

	/**
	 * Check if all the successor states of a state are in a set.
	 * @param s: The state to check
	 * @param set: The set to test for inclusion
	 */
	public boolean allSuccessorsInSet(int s, BitSet set);

	/**
	 * Check if any successor states of a state are in a set.
	 * @param s: The state to check
	 * @param set: The set to test for inclusion
	 */
	public boolean someSuccessorsInSet(int s, BitSet set);

	/**
	 * Get the number of nondeterministic choices in state s.
	 */
	public int getNumChoices(int s);

	/**
	 * Checks for deadlocks and throws an exception if any exist.
	 */
	public void checkForDeadlocks() throws PrismException;

	/**
	 * Checks for deadlocks and throws an exception if any exist.
	 * States in 'except' (If non-null) are excluded from the check.
	 */
	public void checkForDeadlocks(BitSet except) throws PrismException;

	/**
	 * Export to explicit format readable by PRISM (i.e. a .tra file, etc.).
	 */
	public void exportToPrismExplicit(String baseFilename) throws PrismException;

	/**
	 * Export to a dot file.
	 */
	public void exportToDotFile(String filename) throws PrismException;

	/**
	 * Export to a dot file, highlighting states in 'mark'.
	 */
	public void exportToDotFile(String filename, BitSet mark) throws PrismException;

	/**
	 * Report info/stats about the model as a string.
	 */
	public String infoString();
}