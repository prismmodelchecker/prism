//==============================================================================
//
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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
import jdd.SanityJDD;
import mtbdd.PrismMTBDD;
import odd.ODDNode;
import odd.ODDUtils;
import parser.Values;
import parser.VarList;
import prism.Prism;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Base class for symbolic (BDD-based) model representations.
 */
public abstract class ModelSymbolic implements Model
{
	// Model info

	// Vars/consts
	/** Model variable info */
	protected VarList varList;
	/** Values of constants (optional) */
	protected Values constantValues;
	// Actions
	/** Number of action labels */
	protected int numSynchs;
	/** Action label names */
	protected List<String> synchs;
	// Rewards
	/** Number of reward structs */
	protected int numRewardStructs;
	/** Reward struct names */
	protected String[] rewardStructNames;
	// Stats
	/** Number of states */
	protected double numStates;
	/** Number of transitions */
	protected double numTransitions;
	/** Nmber of initial states */
	protected double numStartStates;

	// (MT)BDDs and variables

	/** Transition matrix DD */
	protected JDDNode trans;
	/** 0-1 transition matrix BDD underlying graph structure) */
	protected JDDNode trans01;
	/** Initial states BDD */
	protected JDDNode start; //
	/** Reachable states NBDD */
	protected JDDNode reach;
	/** Deadlock states BDD (may have been fixed) */
	protected JDDNode deadlocks;
	/** State rewards MTBDDs */
	protected JDDNode stateRewards[];
	/** Transition rewards MTBDDs */
	protected JDDNode transRewards[];

	/** DD variable info */
	protected ModelVariablesDD modelVariables;
	/** DD vars for each model variable (rows) */
	protected JDDVars[] varDDRowVars;
	/** DD vars for each model variable (columns) */
	protected JDDVars[] varDDColVars;
	/** All DD vars for rows */
	protected JDDVars allDDRowVars;
	/** All DD vars for columns */
	protected JDDVars allDDColVars;

	/** Map from label names to BDDs for sets of satisfying states */
	protected Map<String, JDDNode> labelsDD = new TreeMap<>();

	/** ODD representing reachable states */
	protected ODDNode odd; // odd

	// Constructor

	public ModelSymbolic(JDDNode trans, JDDNode start, JDDVars allDDRowVars, JDDVars allDDColVars, ModelVariablesDD modelVariables,
						 VarList varList, JDDVars[] varDDRowVars, JDDVars[] varDDColVars)
	{
		this.trans = trans;
		this.start = start;
		this.allDDRowVars = allDDRowVars;
		this.allDDColVars = allDDColVars;
		this.modelVariables = modelVariables;
		this.varList = varList;
		this.varDDRowVars = varDDRowVars;
		this.varDDColVars = varDDColVars;

		// calculate 0-1 version of trans
		JDD.Ref(trans);
		trans01 = JDD.GreaterThan(trans, 0);

		// work out number of initial states
		numStartStates = JDD.GetNumMinterms(start, allDDRowVars.n());
	}

	// Mutators

	/**
	 * Set the initial state(s) BDD (but don't re-do reachability).
	 * The old BDD is derefed if set already.
	 */
	public void setStart(JDDNode start)
	{
		if (this.start != null) {
			JDD.Deref(this.start);
		}
		this.start = start;

		// Recompute number of initial states
		numStartStates = JDD.GetNumMinterms(start, allDDRowVars.n());
	}

	/**
	 * Set reachable states BDD (and compute number of states and ODD)
	 */
	public void setReach(JDDNode reach) throws PrismException
	{
		if (this.reach != null)
			JDD.Deref(this.reach);
		this.reach = reach;

		// work out number of reachable states
		numStates = JDD.GetNumMinterms(reach, allDDRowVars.n());

		// build odd, clear old one
		if (odd != null) {
			ODDUtils.ClearODD(odd);
			odd = null;
		}
		odd = ODDUtils.BuildODD(reach, allDDRowVars);
	}

	/**
	 * Reset transition matrix DD.
	 * The old one (if present) will be deref-ed.
	 * Note: Update reachable states and call {@code filterReachableStates}
	 * afterwards to update related information (trans01, odd, etc).
	 * If the reachable states do not change, you probably still want
	 * to call {@link #resetTrans01(JDDNode)}.
	 *
	 * <br>[ STORES: trans, DEREFS: <i>old transition matrix DD</i> ]
	 */
	public void resetTrans(JDDNode trans)
	{
		if (this.trans != null) {
			JDD.Deref(this.trans);
		}
		this.trans = trans;
	}

	/**
	 * Reset 0-1 transition matrix DD.
	 * The old one (if present) will be deref-ed.
	 *
	 * <br>[ STORES: trans01, DEREFS: <i>old 0-1 transition matrix DD</i> ]
	 */
	public void resetTrans01(JDDNode trans01)
	{
		if (this.trans01 != null) {
			JDD.Deref(this.trans01);
		}
		this.trans01 = trans01;
	}

	/**
	 * Set list of action label names.
	 */
	public void setSynchs(List<String> synchs)
	{
		this.synchs = synchs;
		this.numSynchs = synchs.size();
	}

	/**
	 * Stores a JDDNode state set (over the row variables) for the given label.
	 * If the label already exists, the old state set is derefed and overwritten.
	 * <br>
	 * Note that a stored label takes precedence over the on-the-fly calculation
	 * of an ExpressionLabel during model checking.
	 *
	 * <br>[ STORES: labelDD, deref on later call to clear() ]
	 * @param label the label name
	 * @param labelDD the JDDNode with the state set for the label
	 */
	public void addLabelDD(String label, JDDNode labelDD)
	{
		JDDNode old = labelsDD.put(label, labelDD);
		if (old != null) JDD.Deref(old);
	}

	/**
	 * Add a label with corresponding state set, ensuring a unique, non-existing label.
	 * The label will be either "X" or "X_i" where X is the content of the {@code prefix} argument
	 * and i is a non-negative integer.
	 * <br>
	 * Optionally, a set of defined label names can be passed so that those labels
	 * can be avoided. This can be obtained from the model checker via {@code getDefinedLabelNames()}.
	 * <br>
	 * Note that a stored label takes precedence over the on-the-fly calculation
	 * of an ExpressionLabel during model checking.
	 *
	 * <br>[ STORES: labelDD, deref on later call to clear() ]
	 * @param prefix the prefix for the unique label
	 * @param labelDD the JDDNode with the state set for the label
	 * @param definedLabelNames set of names (optional, may be {@code null}) to check for existing labels
	 * @return the generated unique label
	 */
	public String addUniqueLabelDD(String prefix, JDDNode labelDD, Set<String> definedLabelNames)
	{
		String label;
		int i = 0;
		label = prefix;  // first, try without appending _i
		while (true) {
			boolean labelOk = !hasLabelDD(label);  // not directly attached to model
			if (definedLabelNames != null) {
				labelOk &= !definedLabelNames.contains(label);  // not defined
			}

			if (labelOk) {
				break;
			}

			// prepare next label to try
			label = prefix+"_"+i;
			if (i == Integer.MAX_VALUE)
				throw new UnsupportedOperationException("Integer overflow trying to add unique label");

			i++;
		}

		addLabelDD(label, labelDD);
		return label;
	}

	/**
	 * Set all reward info and DDs.
	 *
	 * <br>[ STORES: stateRewards, transRewards, deref on later call to clear() ]
	 */
	public void setRewards(JDDNode[] stateRewards, JDDNode[] transRewards, String[] rewardStructNames)
	{
		this.stateRewards = stateRewards;
		this.transRewards = transRewards;
		this.numRewardStructs = stateRewards.length;
		this.rewardStructNames = rewardStructNames;
	}

	/**
	 * Reset transition rewards DD for reward with index i.
	 *
	 * <br>[ STORES: transRewards, DEREFS: <i>old trans reward DD</i> ]
	 */
	public void resetTransRewards(int i, JDDNode transRewards)
	{
		if (this.transRewards[i] != null) {
			JDD.Deref(this.transRewards[i]);
		}
		this.transRewards[i] = transRewards;
	}

	/**
	 * Reset state rewards DD for reward with index i.
	 *
	 * <br>[ STORES: stateRewards, DEREFS: <i>old state reward DD</i> ]
	 */
	public void resetStateRewards(int i, JDDNode stateRewards)
	{
		if (this.stateRewards[i] != null) {
			JDD.Deref(this.stateRewards[i]);
		}
		this.stateRewards[i] = stateRewards;
	}

	/**
	 * Set the associated (read-only) constant values.
	 */
	public void setConstantValues(Values constantValues)
	{
		this.constantValues = constantValues;
	}

	/**
	 * Compute and store the set of reachable states.
	 * <br/>
	 * Starts reachability computation from the union of {@code seed} and {@start}.
	 * <br/>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public void doReachability() throws PrismException
	{
		// compute reachable states
		setReach(PrismMTBDD.Reachability(trans01, allDDRowVars, allDDColVars, start));
	}

	/**
	 * Compute and store the set of reachable states, where the parameter {@seed}
	 * provides an initial set of states known to be reachable.
	 * <br/>
	 * Starts reachability computation from the union of {@code seed} and {@start}.
	 * <br/>[ REFS: <i>result</i>, DEREFS: seed ]
	 * @param seed set of states (over ddRowVars) that is known to be reachable
	 */
	public void doReachability(JDDNode seed) throws PrismException
	{
		// do sanity check on seed if checking is enabled
		if (SanityJDD.enabled)
			SanityJDD.checkIsStateSet(seed, getAllDDRowVars());

		// S = union of initial states and seed. seed is dereferenced here.
		JDDNode S = JDD.Or(start.copy(), seed);
		// compute and store reachable states
		setReach(PrismMTBDD.Reachability(trans01, allDDRowVars, allDDColVars, S));
		JDD.Deref(S);
	}

	/**
	 * This method allows you to skip the reachability phase.
	 * It is only here for experimental purposes - not general use.
	 */
	public void skipReachability() throws PrismException
	{
		// don't compute reachable states - assume all reachable
		reach = JDD.Constant(1);

		// work out number of reachable states
		numStates = Math.pow(2, allDDRowVars.n());

		// build odd, clear old one
		if (odd != null) {
			ODDUtils.ClearODD(odd);
			odd = null;
		}
		odd = ODDUtils.BuildODD(reach, allDDRowVars);
	}

	/**
	 * Remove non-reachable states from various DDs
	 * (and calculate num transitions)
	 */
	public void filterReachableStates()
	{
		// remove non-reachable states from transition matrix
		JDD.Ref(reach);
		trans = JDD.Apply(JDD.TIMES, reach, trans);
		JDD.Ref(reach);
		JDDNode tmp = JDD.PermuteVariables(reach, allDDRowVars, allDDColVars);
		trans = JDD.Apply(JDD.TIMES, tmp, trans);

		// recalculate 0-1 version of trans
		JDD.Deref(trans01);
		JDD.Ref(trans);
		trans01 = JDD.GreaterThan(trans, 0);

		// remove non-reachable states from state/transition rewards
		for (int i = 0; i < numRewardStructs; i++) {
			// state rewards vector
			JDD.Ref(reach);
			stateRewards[i] = JDD.Apply(JDD.TIMES, reach, stateRewards[i]);
			// transition reward matrix
			JDD.Ref(reach);
			transRewards[i] = JDD.Apply(JDD.TIMES, reach, transRewards[i]);
			JDD.Ref(reach);
			tmp = JDD.PermuteVariables(reach, allDDRowVars, allDDColVars);
			transRewards[i] = JDD.Apply(JDD.TIMES, tmp, transRewards[i]);
		}

		// filter start states, work out number of initial states
		JDD.Ref(reach);
		start = JDD.Apply(JDD.TIMES, reach, start);
		numStartStates = JDD.GetNumMinterms(start, allDDRowVars.n());

		// work out number of transitions
		numTransitions = JDD.GetNumMinterms(trans01, getNumDDVarsInTrans());
	}

	/**
	 * Find all deadlock states and store this information in the model.
	 * If requested (if fix=true) and if needed (i.e. for DTMCs/CTMCs),
	 * fix deadlocks by adding self-loops in these states.
	 * The set of deadlocks (before any possible fixing) can be obtained from {@link #getDeadlocks()}.
	 */
	public abstract void findDeadlocks(boolean fix);

	// Accessors (for prism.Model & symbolic.Model interface)

	@Override
	public int getNumStates()
	{
		return (numStates > Integer.MAX_VALUE) ? -1 : (int) Math.round(numStates);
	}

	@Override
	public int getNumInitialStates()
	{
		return (numStartStates > Integer.MAX_VALUE) ? -1 : (int) Math.round(numStartStates);
	}

	@Override
	public int getNumTransitions()
	{
		return (numTransitions > Integer.MAX_VALUE) ? -1 : (int) Math.round(numTransitions);
	}

	@Override
	public boolean numStatesExceedsInt()
	{
		return numStates > Integer.MAX_VALUE;
	}

	@Override
	public String getNumStatesString()
	{
		return PrismUtils.bigIntToString(numStates);
	}

	@Override
	public String getNumInitialStatesString()
	{
		return PrismUtils.bigIntToString(numStartStates);
	}

	@Override
	public String getNumTransitionsString()
	{
		return PrismUtils.bigIntToString(numTransitions);
	}

	@Override
	public JDDNode getStart()
	{
		return start;
	}

	@Override
	public JDDNode getDeadlocks()
	{
		return deadlocks;
	}

	@Override
	public JDDNode getReach()
	{
		return reach;
	}

	@Override
	public ODDNode getODD()
	{
		return odd;
	}

	@Override
	public JDDNode getTrans()
	{
		return trans;
	}

	@Override
	public JDDNode getTrans01()
	{
		return trans01;
	}

	@Override
	public JDDNode getTransReln()
	{
		return trans01;
	}

	@Override
	public VarList getVarList()
	{
		return varList;
	}

	@Override
	public Values getConstantValues()
	{
		return constantValues;
	}

	@Override
	public List<String> getSynchs()
	{
		return synchs;
	}

	@Override
	public JDDNode getLabelDD(String label)
	{
		return labelsDD.get(label);
	}

	@Override
	public Set<String> getLabels()
	{
		return Collections.unmodifiableSet(labelsDD.keySet());
	}

	@Override
	public int getNumRewardStructs()
	{
		return numRewardStructs;
	}

	@Override
	public JDDNode getStateRewards(int i)
	{
		return (i >= 0 && i < numRewardStructs) ? stateRewards[i] : null;
	}

	@Override
	public JDDNode getStateRewards(String name)
	{
		for (int i = 0; i < numRewardStructs; i++) {
			if (rewardStructNames[i].equals(name)) {
				return stateRewards[i];
			}
		}
		return null;
	}

	@Override
	public JDDNode getTransRewards(int i)
	{
		return (i >= 0 && i < numRewardStructs) ? transRewards[i] : null;
	}

	@Override
	public JDDNode getTransRewards(String name)
	{
		for (int i = 0; i < numRewardStructs; i++) {
			if (rewardStructNames[i].equals(name)) {
				return transRewards[i];
			}
		}
		return null;
	}

	@Override
	public ModelVariablesDD getModelVariables()
	{
		return modelVariables;
	}

	@Override
	public JDDVars getAllDDRowVars()
	{
		return allDDRowVars;
	}

	@Override
	public JDDVars getAllDDColVars()
	{
		return allDDColVars;
	}

	@Override
	public int getNumDDVarsInTrans()
	{
		return getNumDDRowVars() * 2;
	}

	@Override
	public JDDVars[] getVarDDRowVars()
	{
		return varDDRowVars;
	}

	@Override
	public JDDVars[] getVarDDColVars()
	{
		return varDDColVars;
	}

	@Override
	public JDDVars getVarDDRowVars(int i)
	{
		return varDDRowVars[i];
	}

	@Override
	public JDDVars getVarDDColVars(int i)
	{
		return varDDColVars[i];
	}

	// Print and export methods

	@Override
	public void printTransInfo(PrismLog log, boolean extra)
	{
		int i, j, n;

		log.print("States:      " + getNumStatesString() + " (" + getNumInitialStatesString() + " initial)" + "\n");
		log.print("Transitions: " + getNumTransitionsString() + "\n");

		log.println();

		log.print(getTransName() + ": " + JDD.GetInfoString(trans, getNumDDVarsInTrans()));
		log.print(", vars: " + getNumDDRowVars() + "r/" + getNumDDColVars() + "c\n");
		if (extra) {
			log.print("DD vars (r/c):");
			n = allDDRowVars.getNumVars();
			for (i = 0; i < n; i++) {
				j = allDDRowVars.getVarIndex(i);
				log.print(" " + j + ":" + getDDVarNames().get(j));
				j = allDDColVars.getVarIndex(i);
				log.print(" " + j + ":" + getDDVarNames().get(j));
			}
			log.println();
			log.print(getTransName() + " terminals: " + JDD.GetTerminalsAndNumbersString(trans, getNumDDVarsInTrans()) + "\n");
			log.print("Reach: " + JDD.GetNumNodes(reach) + " nodes\n");
			log.print("ODD: " + ODDUtils.GetNumODDNodes() + " nodes\n");
			for (i = 0; i < numRewardStructs; i++) {
				if (stateRewards[i] != null && !stateRewards[i].equals(JDD.ZERO)) {
					log.print("State rewards (" + (i + 1) + (("".equals(rewardStructNames[i])) ? "" : (":\"" + rewardStructNames[i] + "\"")) + "): ");
					log.print(JDD.GetNumNodes(stateRewards[i]) + " nodes (");
					log.print(JDD.GetNumTerminals(stateRewards[i]) + " terminal), ");
					log.print(JDD.GetNumMintermsString(stateRewards[i], getNumDDRowVars()) + " minterms\n");
					if (extra) {
						log.print("State rewards terminals (" + (i + 1) + (("".equals(rewardStructNames[i])) ? "" : (":\"" + rewardStructNames[i] + "\""))
								+ "): ");
						log.print(JDD.GetTerminalsAndNumbersString(stateRewards[i], getNumDDRowVars()) + "\n");
					}
				}
				if (transRewards[i] != null && !transRewards[i].equals(JDD.ZERO)) {
					log.print("Transition rewards (" + (i + 1) + (("".equals(rewardStructNames[i])) ? "" : (":\"" + rewardStructNames[i] + "\"")) + "): ");
					log.print(JDD.GetNumNodes(transRewards[i]) + " nodes (");
					log.print(JDD.GetNumTerminals(transRewards[i]) + " terminal), ");
					log.print(JDD.GetNumMintermsString(transRewards[i], getNumDDVarsInTrans()) + " minterms\n");
					if (extra) {
						log.print("Transition rewards terminals (" + (i + 1) + (("".equals(rewardStructNames[i])) ? "" : (":\"" + rewardStructNames[i] + "\""))
								+ "): ");
						log.print(JDD.GetTerminalsAndNumbersString(transRewards[i], getNumDDVarsInTrans()) + "\n");
					}
				}
			}
		}
	}

	@Override
	public void exportStateRewardsToFile(int r, int exportType, File file, int precision, boolean noexportheaders)
			throws FileNotFoundException, PrismException
	{
		PrismMTBDD.ExportVector(stateRewards[r], "c" + (r + 1), allDDRowVars, odd, exportType, (file == null) ? null : file.getPath(), precision,
				rewardStructNames[r], noexportheaders);
	}

	@Override
	public void exportStates(int exportType, PrismLog log)
	{
		// Print header: list of model vars
		if (exportType == Prism.EXPORT_MATLAB)
			log.print("% ");
		log.print("(");
		int numVars = getNumVars();
		for (int i = 0; i < numVars; i++) {
			log.print(getVarName(i));
			if (i < numVars - 1)
				log.print(",");
		}
		log.println(")");
		if (exportType == Prism.EXPORT_MATLAB)
			log.println("states=[");

		// Print states
		if (exportType != Prism.EXPORT_MATLAB)
			getReachableStates().print(log);
		else
			getReachableStates().printMatlab(log);

		// Print footer
		if (exportType == Prism.EXPORT_MATLAB)
			log.println("];");
	}

	// Other methods

	@Override
	public void clear()
	{
		// Clear the model (deref all DDs and DD variables)
		for (Map.Entry<String, JDDNode> labelDD : labelsDD.entrySet()) {
			JDD.Deref(labelDD.getValue());
		}
		labelsDD.clear();

		if (varDDRowVars != null)
			JDDVars.derefAllArray(varDDRowVars);
		if (varDDColVars != null)
			JDDVars.derefAllArray(varDDColVars);

		allDDRowVars.derefAll();
		allDDColVars.derefAll();
		JDD.Deref(trans);
		JDD.Deref(trans01);
		JDD.Deref(start);
		if (reach != null)
			JDD.Deref(reach);
		if (deadlocks != null)
			JDD.Deref(deadlocks);
		for (int i = 0; i < numRewardStructs; i++) {
			JDD.Deref(stateRewards[i]);
			JDD.Deref(transRewards[i]);
		}

		if (odd != null) {
			// clear ODD
			ODDUtils.ClearODD(odd);
			odd = null;
		}

		if (modelVariables != null)
			modelVariables.clear();
	}

	// Other accessors

	/**
	 * Get the number of states, as a long.
	 */
	public long getNumStatesLong()
	{
		return (numStates > Long.MAX_VALUE) ? -1 : Math.round(numStates);
	}

	/**
	 * Get the number of initial states, as a long.
	 */
	public long getNumInitialStatesLong()
	{
		return (numStartStates > Long.MAX_VALUE) ? -1 : Math.round(numStartStates);
	}

	/**
	 * Get the total number of transitions, as a long.
	 */
	public long getNumTransitionsLong()
	{
		return (numTransitions > Long.MAX_VALUE) ? -1 : Math.round(numTransitions);
	}
}
