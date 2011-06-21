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

import parser.State;
import parser.Values;
import prism.ModelType;
import prism.PrismException;

/**
 * Base class for simple explicit-state model representations.
 */
public abstract class ModelSimple implements Model
{
	// Number of states
	protected int numStates;
	// Initial states
	protected List<Integer> initialStates; // TODO: should be a (linkedhash?) set really
	/**
	 * States with deadlocks that have been "fixed", i.e. a state that was
	 * originally a deadlock but has been fixed through the addition of a self-loop,
	 * or a state that is still a deadlock but in a model where this acceptable, e.g. a CTMC.
	 */
	protected TreeSet<Integer> deadlocksFixed;
	// State info (read only, just a pointer)
	protected List<State> statesList;
	// Constant info (read only, just a pointer)
	protected Values constantValues;

	// Mutators

	/**
	 * Copy data from another ModelSimple (used by superclass copy constructors).
	 * Assumes that this has already been initialise()ed.
	 */
	public void copyFrom(ModelSimple model)
	{
		numStates = model.numStates;
		for (int in : model.initialStates) {
			addInitialState(in);
		}
		// Shallow copy of read-only stuff
		statesList = model.statesList;
		constantValues = model.constantValues;
	}

	/**
	 * Copy data from another ModelSimple and a state index permutation,
	 * i.e. state index i becomes index permut[i]
	 * (used by superclass copy constructors).
	 * Assumes that this has already been initialise()ed.
	 * Pointer to states list is NOT copied (since now wrong).
	 */
	public void copyFrom(ModelSimple model, int permut[])
	{
		numStates = model.numStates;
		for (int in : model.initialStates) {
			addInitialState(permut[in]);
		}
		// Shallow copy of (some) read-only stuff
		// (i.e. info that is not broken by permute)
		statesList = null;
		constantValues = model.constantValues;
	}

	/**
	 * Initialise: create new model with fixed number of states.
	 */
	public void initialise(int numStates)
	{
		this.numStates = numStates;
		initialStates = new ArrayList<Integer>();
		deadlocksFixed = new TreeSet<Integer>();
		statesList = null;
	}

	/**
	 * Add a state to the list of initial states.
	 */
	public void addInitialState(int i)
	{
		initialStates.add(i);
	}

	/**
	 * Add a state to the list of "fixed" deadlock states.
	 */
	public void addFixedDeadlockState(int i)
	{
		deadlocksFixed.add(i);
	}

	/**
	 * Clear all information for a state (i.e. remove all transitions).
	 */
	public abstract void clearState(int i);

	/**
	 * Add a new state and return its index.
	 */
	public abstract int addState();

	/**
	 * Add multiple new states.
	 */
	public abstract void addStates(int numToAdd);

	/**
	 * Build (anew) from a list of transitions exported explicitly by PRISM (i.e. a .tra file).
	 */
	public abstract void buildFromPrismExplicit(String filename) throws PrismException;

	// Accessors (for Model interface)

	public abstract ModelType getModelType();

	public int getNumStates()
	{
		return numStates;
	}

	public int getNumInitialStates()
	{
		return initialStates.size();
	}

	public Iterable<Integer> getInitialStates()
	{
		return initialStates;
	}

	public int getFirstInitialState()
	{
		return initialStates.isEmpty() ? -1 : initialStates.get(0);
	}

	public boolean isInitialState(int i)
	{
		return initialStates.contains(i);
	}

	@Override
	public boolean isFixedDeadlockState(int i)
	{
		return deadlocksFixed.contains(i);
	}
	
	public List<State> getStatesList()
	{
		return statesList;
	}

	public Values getConstantValues()
	{
		return constantValues;
	}

	public abstract int getNumTransitions();

	public abstract boolean isSuccessor(int s1, int s2);

	public abstract int getNumChoices(int s);

	public void checkForDeadlocks() throws PrismException
	{
		checkForDeadlocks(null);
	}

	public abstract void checkForDeadlocks(BitSet except) throws PrismException;

	@Override
	public void exportToPrismExplicit(String baseFilename) throws PrismException
	{
		// Default implementation - just output .tra file
		// (some models might override this)
		exportToPrismExplicitTra(baseFilename + ".tra");
		// TODO: Also output transition rewards to .trew file, etc.
	}

	public abstract void exportToPrismExplicitTra(String filename) throws PrismException;

	public void exportToDotFile(String filename) throws PrismException
	{
		exportToDotFile(filename, null);
	}

	public abstract void exportToDotFile(String filename, BitSet mark) throws PrismException;

	public abstract void exportToPrismLanguage(String filename) throws PrismException;

	public abstract String infoString();

	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof ModelSimple))
			return false;
		ModelSimple model = (ModelSimple) o;
		if (numStates != model.numStates)
			return false;
		if (!initialStates.equals(model.initialStates))
			return false;
		return true;
	}
}