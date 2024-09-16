//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package symbolic.model;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import odd.ODDNode;
import odd.ODDUtils;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismNotSupportedException;
import symbolic.states.StateList;
import symbolic.states.StateListMTBDD;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import static prism.PrismSettings.DEFAULT_EXPORT_MODEL_PRECISION;

/**
 * Interface for classes that store models symbolically, as (MT)BDDs.
 * For clarity, this interface provides read-only access to the model.
 * See {@link ModelSymbolic} for the writeable base class.
 */
public interface Model extends prism.Model<Double>
{
	// Accessors (basic model info)

	/**
	 * Get a BDD storing the set of initial states.
	 */
	JDDNode getStart();

	/**
	 * Get a StateList storing the set of initial states.
	 */
	default StateList getStartStates()
	{
		return new StateListMTBDD(getStart(), this);
	}

	/**
	 * Get a BDD storing the set of states that are/were deadlocks.
	 * (Such states may have been fixed at build-time by adding self-loops)
	 */
	JDDNode getDeadlocks();

	/**
	 * Get a StateList storing the set of states that are/were deadlocks.
	 * (Such states may have been fixed at build-time by adding self-loops)
	 */
	default StateList getDeadlockStates()
	{
		return new StateListMTBDD(getDeadlocks(), this);
	}

	/**
	 * Get a BDD storing the set of reachable states.
	 */
	JDDNode getReach();

	/**
	 * Get an ODD storing the set of reachable states.
	 */
	ODDNode getODD();

	/**
	 * Get a StateList storing the set of reachable states.
	 */
	default StateList getReachableStates()
	{
		return new StateListMTBDD(getReach(), this);
	}

	/**
	 * Get an (MT)BDD representing the transition function/matrix.
	 */
	JDDNode getTrans();

	/**
	 * Get a BDD representing the underlying transition graph.
	 */
	JDDNode getTrans01();

	/**
	 * Get a BDD storing the underlying transition relation of the model.
	 * If it is not stored already, it will be computed.
	 */
	JDDNode getTransReln();

	/**
	 * Get access to the VarList (optionally stored).
	 */
	VarList getVarList();

	/**
	 * Get access to a list of constant values (optionally stored).
	 */
	Values getConstantValues();

	/**
	 * Get the list of action labels for the model.
	 */
	List<String> getSynchs();

	/**
	 * Get a BDD (over row variables) for the states satisfying a label.
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 * @param label the label name
	 * @return JDDNode for the label, {@code null} if none is stored
	 */
	JDDNode getLabelDD(String label);

	/**
	 * Is a BDD state set stored for this label?
	 */
	default boolean hasLabelDD(String label)
	{
		return getLabels().contains(label);
	}

	/**
	 * Get the (read-only) set of label names that are (optionally) stored in the model.
	 * Returns an empty set if there are none.
	 */
	Set<String> getLabels();

	/**
	 * Get the number of reward structures.
	 */
	int getNumRewardStructs();

	/**
	 * Get an MTBDD for the state rewards for the {@code i}th reward structure.
	 */
	JDDNode getStateRewards(int i);

	/**
	 * Get an MTBDD for the state rewards for the named reward structure.
	 */
	JDDNode getStateRewards(String name);

	/**
	 * Get an MTBDD for the state rewards for the first reward structure.
	 */
	default JDDNode getStateRewards()
	{
		return getStateRewards(0);
	}

	/**
	 * Get an MTBDD for the transition rewards for the {@code i}th reward structure.
	 */
	JDDNode getTransRewards(int i);

	/**
	 * Get an MTBDD for the transition rewards for the named reward structure.
	 */
	JDDNode getTransRewards(String name);

	/**
	 * Get an MTBDD for the transition rewards for the first reward structure.
	 */
	default JDDNode getTransRewards()
	{
		return getTransRewards(0);
	}

	// Accessors (DD variables)

	/**
	 * Get the information about the model's DD variables
	 */
	ModelVariablesDD getModelVariables();

	/**
	 * Get the names of the model's DD variables
	 */
	default Vector<String> getDDVarNames()
	{
		return getModelVariables().getDDVarNames();
	}

	/**
	 * Get the DD variables for rows (source states) of the model
	 */
	JDDVars getAllDDRowVars();

	/**
	 * Get the DD variables for column (destination states) of the model
	 */
	JDDVars getAllDDColVars();

	/**
	 * Get the number of DD variables for rows (source states) of the model
	 */
	default int getNumDDRowVars()
	{
		return getAllDDRowVars().n();
	}

	/**
	 * Get the number DD variables for column (destination states) of the model
	 */
	default int getNumDDColVars()
	{
		return getAllDDColVars().n();
	}

	/**
	 * Get the number of DD variables used to represent the transition function/matrix of the model
	 */
	int getNumDDVarsInTrans();

	/**
	 * Get the DD variables for rows (source states) for each of the model's variables
	 */
	JDDVars[] getVarDDRowVars();

	/**
	 * Get the DD variables for column (destination states) for each of the model's variables
	 */
	JDDVars[] getVarDDColVars();

	/**
	 * Get the DD variables for rows (source states) for the {@code i}th model variable
	 */
	JDDVars getVarDDRowVars(int i);

	/**
	 * Get the DD variables for column (destination states) for the {@code i}th model variable
	 */
	JDDVars getVarDDColVars(int i);

	// Print and export methods

	/**
	 * Print basic information about the model to a log.
	 */
	default void printTransInfo(PrismLog log)
	{
		printTransInfo(log, false);
	}

	/**
	 * Print basic (and, optionally, more detailed) information about the model to a log.
	 */
	void printTransInfo(PrismLog log, boolean extra);

	/**
	 * Export the transition function/matrix.
	 * @param exportType The format in which to export
	 * @param explicit Whether to order by state
	 * @param file File to export to (if null, print to the log instead)
	 */
	default void exportToFile(int exportType, boolean explicit, File file) throws FileNotFoundException, PrismException
	{
		exportToFile(exportType, explicit, file, DEFAULT_EXPORT_MODEL_PRECISION);
	}

	/**
	 * Export the transition function/matrix.
	 * @param exportType The format in which to export
	 * @param explicit Whether to order by state
	 * @param file File to export to (if null, print to the log instead)
	 * @param precision Model export precision (number of significant digits, >= 1)
	 */
	void exportToFile(int exportType, boolean explicit, File file, int precision) throws FileNotFoundException, PrismException;

	/**
	 * Export (non-zero) state rewards for one reward structure of the model.
	 * @param r Index of reward structure to export (0-indexed)
	 * @param exportType The format in which to export
	 * @param file File to export to (if null, print to the log instead)
	 */
	default void exportStateRewardsToFile(int r, int exportType, File file) throws FileNotFoundException, PrismException
	{
		exportStateRewardsToFile(r, exportType, file, DEFAULT_EXPORT_MODEL_PRECISION, false);
	}

	/**
	 * Export (non-zero) state rewards for one reward structure of the model.
	 * @param r Index of reward structure to export (0-indexed)
	 * @param exportType The format in which to export
	 * @param file File to export to (if null, print to the log instead)
	 * @param precision Model export precision (number of significant digits, >= 1)
	 * @param noexportheaders disables export headers for srew files
	 */
	void exportStateRewardsToFile(int r, int exportType, File file, int precision, boolean noexportheaders) throws FileNotFoundException, PrismException;

	/**
	 * Export (non-zero) transition rewards for one reward structure of the model.
	 * @param r Index of reward structure to export (0-indexed)
	 * @param exportType The format in which to export
	 * @param ordered Do the entries need to be printed in order?
	 * @param file File to export to (if null, print to the log instead)
	 */
	default void exportTransRewardsToFile(int r, int exportType, boolean ordered, File file) throws FileNotFoundException, PrismException
	{
		exportTransRewardsToFile(r, exportType, ordered, file, DEFAULT_EXPORT_MODEL_PRECISION, false);
	}

	/**
	 * Export (non-zero) transition rewards for one reward structure of the model.
	 * @param r Index of reward structure to export (0-indexed)
	 * @param exportType The format in which to export
	 * @param ordered Do the entries need to be printed in order?
	 * @param file File to export to (if null, print to the log instead)
	 * @param precision number of significant digits >= 1
	 * @param noexportheaders disables export headers for trew files
	 */
	void exportTransRewardsToFile(int r, int exportType, boolean ordered, File file, int precision, boolean noexportheaders) throws FileNotFoundException, PrismException;

	/**
	 * Export the list of reachable states of the model.
	 * @param exportType The format in which to export
	 * @param log Where to export
	 */
	void exportStates(int exportType, PrismLog log);

	// Other methods

	/**
	 * Convert a BDD (over row variables) representing a single state
	 * to a corresponding {@link State} object.
	 * @param dd 0/1-MTBDD, representing a single state
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	default State convertBddToState(JDDNode dd)
	{
		return convertBddToState(dd, getAllDDRowVars(), getVarList());
	}

	/**
	 * Convert a BDD (over the given row variables, encoding variables according to the VarList)
	 * representing a single state to a corresponding {@link State} object.
	 * @param dd 0/1-MTBDD, representing a single state
	 * @param allDDRowVars the list of row variables
	 * @param varList the VarList, specifying the encoding of the individual state variables
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public static State convertBddToState(JDDNode dd, JDDVars allDDRowVars, VarList varList)
	{
		// First convert path through BDD to a bit vector
		JDDNode ptr = dd;
		int n = allDDRowVars.n();
		BitSet bits = new BitSet(n);
		for (int i = 0; i < n; i++) {
			if (ptr.getIndex() > allDDRowVars.getVarIndex(i)) {
			} else if (!ptr.getElse().equals(JDD.ZERO)) {
				ptr = ptr.getElse();
			} else {
				bits.set(i, true);
				ptr = ptr.getThen();
			}
		}
		// Then convert to State object
		return varList.convertBitSetToState(bits);
	}

	/**
	 * Convert a BDD (over row variables) representing a single state
	 * to an index into the list of reachable states.
	 */
	default int convertBddToIndex(JDDNode dd) throws PrismNotSupportedException
	{
		ODDNode odd = getODD();
		ODDUtils.checkInt(odd, "Cannot convert Bdd to index in model");
		// Traverse BDD and ODD simultaneously to compute index
		JDDVars allDDRowVars = getAllDDRowVars();
		JDDNode ptr = dd;
		ODDNode oddPtr = odd;
		int n = allDDRowVars.n();
		int index = 0;
		for (int i = 0; i < n; i++) {
			if (ptr.getIndex() > allDDRowVars.getVarIndex(i)) {
				oddPtr = oddPtr.getElse();
			} else if (!ptr.getElse().equals(JDD.ZERO)) {
				ptr = ptr.getElse();
				oddPtr = oddPtr.getElse();
			} else {
				ptr = ptr.getThen();
				index += oddPtr.getEOff();
				oddPtr = oddPtr.getThen();
			}
		}
		return index;
	}

	/**
	 * Get the name of the transition function/matrix (e.g. "Transition matrix")
	 */
	String getTransName();

	/**
	 * Get the symbolic of the transition function/matrix when exporting (e.g. "P")
	 */
	String getTransSymbol();

	/**
	 * Can be called once the model is finished with, e.g. to clear local storage.
	 */
	void clear();

	// Convenience methods for getting variable info

	/**
	 * Get the number of model variables
	 */
	default int getNumVars()
	{
		return getVarList().getNumVars();
	}

	/**
	 * Get the name of the {@code i}th model variable
	 */
	default String getVarName(int i)
	{
		return getVarList().getName(i);
	}

	/**
	 * Get the index of model variable {@code name}
	 */
	default int getVarIndex(String name)
	{
		return getVarList().getIndex(name);
	}

	/**
	 * Get the low value of the {@code i}th model variable
	 */
	default int getVarLow(int i)
	{
		return getVarList().getLow(i);
	}

	/**
	 * Get the high value of the {@code i}th model variable
	 */
	default int getVarHigh(int i)
	{
		return getVarList().getHigh(i);
	}

	/**
	 * Get the range of the {@code i}th model variable
	 */
	default int getVarRange(int i)
	{
		return getVarList().getRange(i);
	}
}

//------------------------------------------------------------------------------
