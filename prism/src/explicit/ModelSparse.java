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
import prism.PrismFileLog;
import prism.PrismLog;

/**
 * Base class sparse matrix-based (non-mutable) explicit-state model representations
 */
public abstract class ModelSparse implements Model
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
	// State info
	protected List<State> statesList;
	// Constant info
	protected Values constantValues;
	
	// Mutators
	
	/**
	 * Initialise: create new model with fixed number of states.
	 */
	protected void initialise(int numStates)
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
	}

	@Override
	public void exportToPrismExplicitTra(String filename) throws PrismException
	{
		exportToPrismExplicitTra(new PrismFileLog(filename));
	}

	@Override
	public void exportToPrismExplicitTra(File file) throws PrismException
	{
		exportToPrismExplicitTra(new PrismFileLog(file.getPath()));
	}

	@Override
	public abstract void exportToPrismExplicitTra(PrismLog log) throws PrismException;

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