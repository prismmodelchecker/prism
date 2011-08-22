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

import java.io.File;
import java.util.*;

import parser.State;
import parser.Values;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;

/**
 * Interface for (abstract) classes that provide (read-only) access to an explicit-state model.
 */
public interface Model
{
	// Accessors

	/**
	 * Get the type of this model.
	 */
	public ModelType getModelType();

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
	 * Check whether a state is a "fixed" deadlock, i.e. a state that was
	 * originally a deadlock but has been fixed through the addition of a self-loop,
	 * or a state that is still a deadlock but in a model where this acceptable, e.g. a CTMC.
	 */
	public boolean isFixedDeadlockState(int i);
	
	/**
	 * Get access to an (optional) list of states.
	 */
	public List<State> getStatesList();
	
	/**
	 * Get access to an (optional) list of constant values.
	 */
	public Values getConstantValues();
	
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
	 * @param s The state to check
	 * @param set The set to test for inclusion
	 */
	public boolean allSuccessorsInSet(int s, BitSet set);

	/**
	 * Check if any successor states of a state are in a set.
	 * @param s The state to check
	 * @param set The set to test for inclusion
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
	 * Find all deadlocks and return a BitSet of these states.
	 * If requested (if fix=true), then add self-loops in these states
	 * (and update the "fixed" deadlock information).
	 */
	public BitSet findDeadlocks(boolean fix) throws PrismException;

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
	 * Export transition matrix to explicit format readable by PRISM (i.e. a .tra file).
	 */
	public void exportToPrismExplicitTra(String filename) throws PrismException;

	/**
	 * Export transition matrix to explicit format readable by PRISM (i.e. a .tra file).
	 */
	public void exportToPrismExplicitTra(File file) throws PrismException;
	
	/**
	 * Export transition matrix to explicit format readable by PRISM (i.e. a .tra file).
	 */
	public void exportToPrismExplicitTra(PrismLog log) throws PrismException;
	
	/**
	 * Export to a dot file.
	 */
	public void exportToDotFile(String filename) throws PrismException;

	/**
	 * Export to a dot file, highlighting states in 'mark'.
	 */
	public void exportToDotFile(String filename, BitSet mark) throws PrismException;

	/**
	 * Export to a equivalent PRISM language model description.
	 */
	public void exportToPrismLanguage(String filename) throws PrismException;
	
	/**
	 * Report info/stats about the model as a string.
	 */
	public String infoString();

	/**
	 * Report info/stats about the model, tabulated, as a string.
	 */
	public String infoStringTable();
}