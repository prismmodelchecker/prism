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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import parser.State;
import parser.Values;
import parser.VarList;
import prism.ModelType;
import prism.Prism;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;

/**
 * Base class for explicit-state model representations.
 */
public abstract class ModelExplicit implements Model
{
	// Basic model information

	/** Number of states */
	protected int numStates;
	/** Which states are initial states */
	protected List<Integer> initialStates; // TODO: should be a (linkedhash?) set really
	/** States that are/were deadlocks. Where requested and where appropriate (DTMCs/MDPs),
	 * these states may have been fixed at build time by adding self-loops. */
	protected TreeSet<Integer> deadlocks;

	// Additional, optional information associated with the model

	/** (Optionally) information about the states of this model,
	 * i.e. the State object corresponding to each state index. */
	protected List<State> statesList;
	/** (Optionally) a list of values for constants associated with this model. */
	protected Values constantValues;
	/** (Optionally) some labels (atomic propositions) associated with the model,
	 * represented as a String->BitSet mapping from their names to the states that satisfy them. */
	protected Map<String, BitSet> labels = new TreeMap<String, BitSet>();

	// Mutators

	/**
	 * Copy data from another ModelExplicit (used by superclass copy constructors).
	 * Assumes that this has already been initialise()ed.
	 */
	public void copyFrom(ModelExplicit model)
	{
		numStates = model.numStates;
		for (int in : model.initialStates) {
			addInitialState(in);
		}
		for (int dl : model.deadlocks) {
			addDeadlockState(dl);
		}
		// Shallow copy of read-only stuff
		statesList = model.statesList;
		constantValues = model.constantValues;
		labels = model.labels;
	}

	/**
	 * Copy data from another ModelExplicit and a state index permutation,
	 * i.e. state index i becomes index permut[i]
	 * (used by superclass copy constructors).
	 * Assumes that this has already been initialise()ed.
	 * Pointer to states list is NOT copied (since now wrong).
	 */
	public void copyFrom(ModelExplicit model, int permut[])
	{
		numStates = model.numStates;
		for (int in : model.initialStates) {
			addInitialState(permut[in]);
		}
		for (int dl : model.deadlocks) {
			addDeadlockState(permut[dl]);
		}
		// Shallow copy of (some) read-only stuff
		// (i.e. info that is not broken by permute)
		statesList = null;
		constantValues = model.constantValues;
		labels.clear();
	}

	/**
	 * Initialise: create new model with fixed number of states.
	 */
	public void initialise(int numStates)
	{
		this.numStates = numStates;
		initialStates = new ArrayList<Integer>();
		deadlocks = new TreeSet<Integer>();
		statesList = null;
		constantValues = null;
		labels = new TreeMap<String, BitSet>();
	}

	/**
	 * Add a state to the list of initial states.
	 */
	public void addInitialState(int i)
	{
		initialStates.add(i);
	}

	/**
	 * Add a state to the list of deadlock states.
	 */
	public void addDeadlockState(int i)
	{
		deadlocks.add(i);
	}

	/**
	 * Build (anew) from a list of transitions exported explicitly by PRISM (i.e. a .tra file).
	 */
	public abstract void buildFromPrismExplicit(String filename) throws PrismException;

	/**
	 * Set the associated (read-only) state list.
	 */
	public void setStatesList(List<State> statesList)
	{
		this.statesList = statesList;
	}

	/**
	 * Set the associated (read-only) constant values.
	 */
	public void setConstantValues(Values constantValues)
	{
		this.constantValues = constantValues;
	}

	/**
	 * Adds a label and the set the states that satisfy it.
	 * Any existing label with the same name is overwritten.
	 * @param name The name of the label
	 * @param states The states that satisyy the label 
	 */
	public void addLabel(String name, BitSet states)
	{
		labels.put(name, states);
	}

	// Accessors (for Model interface)

	@Override
	public abstract ModelType getModelType();

	@Override
	public int getNumStates()
	{
		return numStates;
	}

	@Override
	public int getNumInitialStates()
	{
		return initialStates.size();
	}

	@Override
	public Iterable<Integer> getInitialStates()
	{
		return initialStates;
	}

	@Override
	public int getFirstInitialState()
	{
		return initialStates.isEmpty() ? -1 : initialStates.get(0);
	}

	@Override
	public boolean isInitialState(int i)
	{
		return initialStates.contains(i);
	}

	@Override
	public int getNumDeadlockStates()
	{
		return deadlocks.size();
	}

	@Override
	public Iterable<Integer> getDeadlockStates()
	{
		return deadlocks;
	}

	@Override
	public StateValues getDeadlockStatesList()
	{
		BitSet bs = new BitSet();
		for (int dl : deadlocks) {
			bs.set(dl);
		}

		return StateValues.createFromBitSet(bs, this);
	}

	@Override
	public int getFirstDeadlockState()
	{
		return deadlocks.isEmpty() ? -1 : deadlocks.first();
	}

	@Override
	public boolean isDeadlockState(int i)
	{
		return deadlocks.contains(i);
	}

	@Override
	public List<State> getStatesList()
	{
		return statesList;
	}

	@Override
	public Values getConstantValues()
	{
		return constantValues;
	}

	@Override
	public BitSet getLabelStates(String name)
	{
		return labels.get(name);
	}

	@Override
	public Set<String> getLabels()
	{
		return labels.keySet();
	}
	
	@Override
	public abstract int getNumTransitions();

	@Override
	public abstract boolean isSuccessor(int s1, int s2);

	@Override
	public void checkForDeadlocks() throws PrismException
	{
		checkForDeadlocks(null);
	}

	@Override
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
		exportToPrismExplicitTra(PrismFileLog.create(filename));
	}

	@Override
	public void exportToPrismExplicitTra(File file) throws PrismException
	{
		exportToPrismExplicitTra(PrismFileLog.create(file.getPath()));
	}

	@Override
	public abstract void exportToPrismExplicitTra(PrismLog out);

	@Override
	public void exportToDotFile(String filename) throws PrismException
	{
		exportToDotFile(PrismFileLog.create(filename), null);
	}

	@Override
	public void exportToDotFile(String filename, BitSet mark) throws PrismException
	{
		exportToDotFile(PrismFileLog.create(filename), mark);
	}

	@Override
	public void exportToDotFile(PrismLog out)
	{
		exportToDotFile(out, null, false);
	}

	@Override
	public void exportToDotFile(PrismLog out, BitSet mark)
	{
		exportToDotFile(out, mark, false);
	}

	@Override
	public void exportToDotFile(PrismLog out, BitSet mark, boolean showStates)
	{
		int i;
		// Header
		out.print("digraph " + getModelType() + " {\nsize=\"8,5\"\nnode [shape=box];\n");
		for (i = 0; i < numStates; i++) {
			// Style for each state
			if (mark != null && mark.get(i))
				out.print(i + " [style=filled  fillcolor=\"#cccccc\"]\n");
			// Transitions for state i
			exportTransitionsToDotFile(i, out);
		}
		// Append state info (if required)
		if (showStates) {
			List<State> states = getStatesList();
			if (states != null) {
				for (i = 0; i < numStates; i++) {
					out.print(i + " [label=\"" + i + "\\n" + states.get(i) + "\"]\n");
				}
			}
		}
		// Footer
		out.print("}\n");
	}

	/**
	 * Export the transitions from state {@code i} in Dot format to {@code out}.
	 * @param i State index
	 * @param out PrismLog for output
	 */
	protected abstract void exportTransitionsToDotFile(int i, PrismLog out);

	@Override
	public abstract void exportToPrismLanguage(String filename) throws PrismException;

	@Override
	public void exportStates(int exportType, VarList varList, PrismLog log) throws PrismException
	{
		if (statesList == null)
			return;

		// Print header: list of model vars
		if (exportType == Prism.EXPORT_MATLAB)
			log.print("% ");
		log.print("(");
		int numVars = varList.getNumVars();
		for (int i = 0; i < numVars; i++) {
			log.print(varList.getName(i));
			if (i < numVars - 1)
				log.print(",");
		}
		log.println(")");
		if (exportType == Prism.EXPORT_MATLAB)
			log.println("states=[");

		// Print states
		int numStates = statesList.size();
		for (int i = 0; i < numStates; i++) {
			if (exportType != Prism.EXPORT_MATLAB)
				log.println(i + ":" + statesList.get(i).toString());
			else
				log.println(statesList.get(i).toStringNoParentheses());
		}

		// Print footer
		if (exportType == Prism.EXPORT_MATLAB)
			log.println("];");
	}

	@Override
	public abstract String infoString();

	@Override
	public abstract String infoStringTable();

	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof ModelExplicit))
			return false;
		ModelExplicit model = (ModelExplicit) o;
		if (numStates != model.numStates)
			return false;
		if (!initialStates.equals(model.initialStates))
			return false;
		return true;
	}
}