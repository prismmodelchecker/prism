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

package prism;

import java.io.*;
import java.util.*;

import jdd.*;
import odd.*;
import parser.*;

public interface Model
{
	ModelType getModelType();

	int getNumModules();
	String[] getModuleNames();
	String getModuleName(int i);
	
	int getNumVars();
	VarList getVarList();
	String getVarName(int i);
	int getVarIndex(String n);
	int getVarModule(int i);
	int getVarLow(int i);
	int getVarHigh(int i);
	int getVarRange(int i);
	Values getConstantValues();
	List<String> getSynchs();

	/**
	 * Returns the JDDNode for the state set (over the row variables)
	 * associated with the given label.
	 *
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 * @param label the label
	 * @returns JDDNode for the label, {@code null} if none is stored
	 */
	JDDNode getLabelDD(String label);

	/**
	 * Returns true if a JDDNode state set for the given label
	 * is stored in the model.
	 */
	default boolean hasLabelDD(String label)
	{
		return getLabels().contains(label);
	}

	/**
	 * Get the (read-only) set of label names
	 * that are (optionally) stored in the model.
	 * Returns an empty set if there are none.
	 */
	Set<String> getLabels();

	/**
	 * Add a label with corresponding state set, ensuring a unique, non-existing label.
	 * The label will be either "X" or "X_i" where X is the content of the {@code prefix} argument
	 * and i is a non-negative integer.
	 * <br>
	 * Optionally, a set of defined label names can be passed so that those labels
	 * can be avoided. This can be obtained from the model checker via {@code getDefinedLabelNames()}.
	 * <br>
	 * Note that a stored label takes precedence over the on-the-fly calculation
	 * of an ExpressionLabel, cf. {@link explicit.StateModelChecker#checkExpressionLabel}
	 * <br>[ STORES: labelDD, deref on later call to clear() ]
	 *
	 * @param prefix the prefix for the unique label
	 * @param labelDD the JDDNode with the state set for the label
	 * @param definedLabelNames set of names (optional, may be {@code null}) to check for existing labels
	 * @return the generated unique label
	 */
	public String addUniqueLabelDD(String prefix, JDDNode labelDD, Set<String> definedLabelNames);

	String globalToLocal(long x);
	int globalToLocal(long x, int l);
	State convertBddToState(JDDNode dd);
	int convertBddToIndex(JDDNode dd);

	StateList getReachableStates();
	
	/**
	 * Get a StateList storing the set of states that are/were deadlocks.
	 * (Such states may have been fixed at build-time by adding self-loops)
	 */
	StateList getDeadlockStates();
	
	StateList getStartStates();
	int getNumRewardStructs();
	long getNumStates();
	long getNumTransitions();
	long getNumStartStates();
	String getNumStatesString();
	String getNumTransitionsString();
	String getNumStartStatesString();

	JDDNode getTrans();
	JDDNode getTrans01();
	JDDNode getStart();
	JDDNode getReach();
	
	/**
	 * Get a BDD storing the underlying transition relation of the model.
	 * If it is not stored already, it will be computed.
	 */
	JDDNode getTransReln();
	
	/**
	 * Get a BDD storing the set of states that are/were deadlocks.
	 * (Such states may have been fixed at build-time by adding self-loops)
	 */
	JDDNode getDeadlocks();
	
	JDDNode getStateRewards();
	JDDNode getStateRewards(int i);
	JDDNode getStateRewards(String s);
	JDDNode getTransRewards();
	JDDNode getTransRewards(int i);
	JDDNode getTransRewards(String s);
	JDDNode getTransActions();
	JDDNode[] getTransPerAction();
	JDDVars[] getVarDDRowVars();
	JDDVars[] getVarDDColVars();
	JDDVars getVarDDRowVars(int i);
	JDDVars getVarDDColVars(int i);
	JDDVars[] getModuleDDRowVars();
	JDDVars[] getModuleDDColVars();
	JDDVars getModuleDDRowVars(int i);
	JDDVars getModuleDDColVars(int i);
	JDDVars getAllDDRowVars();
	JDDVars getAllDDColVars();
	int getNumDDRowVars();
	int getNumDDColVars();
	int getNumDDVarsInTrans();
	Vector<String> getDDVarNames();

	/** Get the information about the model variables */
	ModelVariablesDD getModelVariables();

	ODDNode getODD();

	void setSynchs(List<String> synchs);

	/**
	 * Stores a JDDNode state set (over the row variables)
	 * for the given label.<br>
	 * If the label already exists, the old state set is dereferenced
	 * and overwritten.
	 * <br>
	 * Note that a stored label takes precedence over the on-the-fly calculation
	 * of an ExpressionLabel, cf. {@link prism.StateModelChecker#checkExpressionLabel}
	 *
	 * <br>[ STORES: labelDD, deref on later call to clear() ]
	 * @param label the label name
	 * @param labelDD the JDDNode with the state set for the label
	*/
	void addLabelDD(String label, JDDNode labelDD);

	void resetTrans(JDDNode trans);
	void resetTransRewards(int i, JDDNode transRewards);
	void doReachability() throws PrismException;
	void skipReachability() throws PrismException;
	void setReach(JDDNode reach) throws PrismException;
	void setTransActions(JDDNode transActions); // MDPs only
	void setTransPerAction(JDDNode[] transPerAction); // D/CTMCs only
	void filterReachableStates();
	
	/**
	 * Find all deadlock states and store this information in the model.
	 * If requested (if fix=true) and if needed (i.e. for DTMCs/CTMCs),
	 * fix deadlocks by adding self-loops in these states.
	 * The set of deadlocks (before any possible fixing) can be obtained from {@link #getDeadlocks()}.
	 */
	void findDeadlocks(boolean fix);
	
	void printTrans();
	void printTrans01();
	public void printTransInfo(PrismLog log);
	public void printTransInfo(PrismLog log, boolean extra);
	void exportToFile(int exportType, boolean explicit, File file) throws FileNotFoundException, PrismException;
	
	/**
	 * Export (non-zero) state rewards for one reward structure of the model.
	 * @param r Index of reward structure to export (0-indexed)
	 * @param exportType The format in which to export
	 * @param file File to export to (if null, print to the log instead)
	 */
	void exportStateRewardsToFile(int r, int exportType, File file) throws FileNotFoundException, PrismException;
	
	@Deprecated
	String exportStateRewardsToFile(int exportType, File file) throws FileNotFoundException, PrismException;
	
	/**
	 * Export (non-zero) transition rewards for one reward structure of the model.
	 * @param r Index of reward structure to export (0-indexed)
	 * @param exportType The format in which to export
	 * @param ordered Do the entries need to be printed in order?
	 * @param file File to export to (if null, print to the log instead)
	 */
	void exportTransRewardsToFile(int r, int exportType, boolean ordered, File file) throws FileNotFoundException, PrismException;

	@Deprecated
	String exportTransRewardsToFile(int exportType, boolean explicit, File file) throws FileNotFoundException, PrismException;

	void exportStates(int exportType, PrismLog log);

	void clear();
}

//------------------------------------------------------------------------------
