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
import java.util.Map.Entry;

import jdd.*;
import odd.*;
import mtbdd.*;
import parser.*;
import parser.ast.Declaration;
import parser.ast.DeclarationInt;
import parser.ast.Expression;
import sparse.*;

/*
 * Class for MTBDD-based storage of a PRISM model that is a DTMC.
 */
public class ProbModel implements Model
{
	// model info

	// modules
	protected int numModules; // number of modules
	protected String[] moduleNames; // module names
	// vars/consts
	protected int numVars; // total number of module variables
	protected VarList varList; // list of module variables
	protected long[] gtol; // numbers for use by globalToLocal
	protected Values constantValues; // values of constants
	// actions
	protected int numSynchs; // number of synchronising actions
	protected List<String> synchs; // synchronising action labels
	// rewards
	protected int numRewardStructs; // number of reward structs
	protected String[] rewardStructNames; // reward struct names
	// stats
	protected double numStates; // number of states
	protected double numTransitions; // number of transitions
	protected double numStartStates; // number of initial states

	// mtbdd stuff

	// dds

	protected JDDNode trans; // transition matrix dd
	protected JDDNode trans01; // 0-1 transition matrix dd
	protected JDDNode start; // start state dd
	protected JDDNode reach; // reachable states dd
	protected JDDNode deadlocks; // deadlock states dd (may have been fixed)
	protected JDDNode stateRewards[]; // state rewards dds
	protected JDDNode transRewards[]; // transition rewards dds
	protected JDDNode transActions; // dd for transition action labels (MDPs)
	protected JDDNode transPerAction[]; // dds for transition action labels (D/CTMCs)

	// dd vars
	protected JDDVars[] varDDRowVars; // dd vars for each module variable (rows)
	protected JDDVars[] varDDColVars; // dd vars for each module variable (cols)
	protected JDDVars[] moduleDDRowVars; // dd vars for each module (rows)
	protected JDDVars[] moduleDDColVars; // dd vars for each module (cols)
	protected JDDVars allDDRowVars; // all dd vars (rows)
	protected JDDVars allDDColVars; // all dd vars (cols)

	protected ModelVariablesDD modelVariables;

	/**
	 * A map from label to state set, optionally storing a state set
	 * for a given label directly in the model.
	 */
	protected Map<String, JDDNode> labelsDD = new TreeMap<String, JDDNode>();

	protected ODDNode odd; // odd

	// accessor methods

	// model info

	// type
	public ModelType getModelType()
	{
		return ModelType.DTMC;
	}

	// modules
	public int getNumModules()
	{
		return numModules;
	}

	public String[] getModuleNames()
	{
		return moduleNames;
	}

	public String getModuleName(int i)
	{
		return moduleNames[i];
	}

	// vars
	public int getNumVars()
	{
		return numVars;
	}

	public VarList getVarList()
	{
		return varList;
	}

	public String getVarName(int i)
	{
		return varList.getName(i);
	}

	public int getVarIndex(String n)
	{
		return varList.getIndex(n);
	}

	public int getVarModule(int i)
	{
		return varList.getModule(i);
	}

	public int getVarLow(int i)
	{
		return varList.getLow(i);
	}

	public int getVarHigh(int i)
	{
		return varList.getHigh(i);
	}

	public int getVarRange(int i)
	{
		return varList.getRange(i);
	}

	public Values getConstantValues()
	{
		return constantValues;
	}

	/**
	 * Get list of action label names. 
	 */
	public List<String> getSynchs()
	{
		return synchs;
	}

	// rewards
	public int getNumRewardStructs()
	{
		return numRewardStructs;
	}

	// stats
	public long getNumStates()
	{
		return (numStates > Long.MAX_VALUE) ? -1 : Math.round(numStates);
	}

	public long getNumTransitions()
	{
		return (numTransitions > Long.MAX_VALUE) ? -1 : Math.round(numTransitions);
	}

	public long getNumStartStates()
	{
		return (numStartStates > Long.MAX_VALUE) ? -1 : Math.round(numStartStates);
	}

	// additional methods to get stats as strings
	public String getNumStatesString()
	{
		return PrismUtils.bigIntToString(numStates);
	}

	public String getNumTransitionsString()
	{
		return PrismUtils.bigIntToString(numTransitions);
	}

	public String getNumStartStatesString()
	{
		return PrismUtils.bigIntToString(numStartStates);
	}

	// lists of states
	public StateList getReachableStates()
	{
		return new StateListMTBDD(reach, this);
	}

	@Override
	public StateList getDeadlockStates()
	{
		return new StateListMTBDD(deadlocks, this);
	}

	public StateList getStartStates()
	{
		return new StateListMTBDD(start, this);
	}

	// mtbdd stuff

	public JDDNode getTrans()
	{
		return trans;
	}

	public JDDNode getTrans01()
	{
		return trans01;
	}

	public JDDNode getStart()
	{
		return start;
	}

	public JDDNode getReach()
	{
		return reach;
	}

	@Override
	public JDDNode getTransReln()
	{
		return trans01;
	}

	@Override
	public JDDNode getDeadlocks()
	{
		return deadlocks;
	}

	public JDDNode getStateRewards()
	{
		return getStateRewards(0);
	}

	public JDDNode getStateRewards(int i)
	{
		return (i >= 0 && i < numRewardStructs) ? stateRewards[i] : null;
	}

	public JDDNode getStateRewards(String s)
	{
		for (int i = 0; i < numRewardStructs; i++)
			if (rewardStructNames[i].equals(s))
				return stateRewards[i];
		return null;
	}

	public JDDNode getTransRewards()
	{
		return getTransRewards(0);
	}

	public JDDNode getTransRewards(int i)
	{
		return (i >= 0 && i < numRewardStructs) ? transRewards[i] : null;
	}

	public JDDNode getTransRewards(String s)
	{
		for (int i = 0; i < numRewardStructs; i++)
			if (rewardStructNames[i].equals(s))
				return transRewards[i];
		return null;
	}

	public JDDNode getTransActions()
	{
		return transActions;
	}

	public JDDNode[] getTransPerAction()
	{
		return transPerAction;
	}

	// dd vars
	public JDDVars[] getVarDDRowVars()
	{
		return varDDRowVars;
	}

	public JDDVars[] getVarDDColVars()
	{
		return varDDColVars;
	}

	public JDDVars getVarDDRowVars(int i)
	{
		return varDDRowVars[i];
	}

	public JDDVars getVarDDColVars(int i)
	{
		return varDDColVars[i];
	}

	public JDDVars[] getModuleDDRowVars()
	{
		return moduleDDRowVars;
	}

	public JDDVars[] getModuleDDColVars()
	{
		return moduleDDColVars;
	}

	public JDDVars getModuleDDRowVars(int i)
	{
		return moduleDDRowVars[i];
	}

	public JDDVars getModuleDDColVars(int i)
	{
		return moduleDDColVars[i];
	}

	public JDDVars getAllDDRowVars()
	{
		return allDDRowVars;
	}

	public JDDVars getAllDDColVars()
	{
		return allDDColVars;
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

	// additional useful methods to do with dd vars
	public int getNumDDRowVars()
	{
		return allDDRowVars.n();
	}

	public int getNumDDColVars()
	{
		return allDDColVars.n();
	}

	public int getNumDDVarsInTrans()
	{
		return allDDRowVars.n() * 2;
	}

	public Vector<String> getDDVarNames()
	{
		return modelVariables.getDDVarNames();
	}

	@Override
	public ModelVariablesDD getModelVariables()
	{
		return modelVariables;
	}

	public ODDNode getODD()
	{
		return odd;
	}

	public String getTransName()
	{
		return "Transition matrix";
	}

	public String getTransSymbol()
	{
		return "P";
	}

	// constructor

	public ProbModel(JDDNode tr, JDDNode s, JDDNode sr[], JDDNode trr[], String rsn[], JDDVars arv, JDDVars acv, ModelVariablesDD mvdd, int nm, String[] mn,
			JDDVars[] mrv, JDDVars[] mcv, int nv, VarList vl, JDDVars[] vrv, JDDVars[] vcv, Values cv)
	{
		int i;

		trans = tr;
		start = s;
		deadlocks = null;
		stateRewards = sr;
		transRewards = trr;
		numRewardStructs = stateRewards.length; // which should == transRewards.length
		rewardStructNames = rsn;
		allDDRowVars = arv;
		allDDColVars = acv;
		modelVariables = mvdd;
		numModules = nm;
		moduleNames = mn;
		moduleDDRowVars = mrv;
		moduleDDColVars = mcv;
		numVars = nv;
		varList = vl;
		varDDRowVars = vrv;
		varDDColVars = vcv;
		constantValues = cv;

		// action label info (optional) is initially null
		transActions = null;
		transPerAction = null;

		// compute numbers for globalToLocal converter
		gtol = new long[numVars];
		for (i = 0; i < numVars; i++) {
			gtol[i] = 1l << (varDDRowVars[i].getNumVars());
		}
		for (i = numVars - 2; i >= 0; i--) {
			gtol[i] = gtol[i] * gtol[i + 1];
		}

		// calculate 0-1 version of trans
		JDD.Ref(trans);
		trans01 = JDD.GreaterThan(trans, 0);

		// work out number of initial states
		numStartStates = JDD.GetNumMinterms(start, allDDRowVars.n());
	}

	/**
	 * Set vector of action label names. 
	 */
	public void setSynchs(List<String> synchs)
	{
		this.synchs = synchs;
		this.numSynchs = synchs.size();
	}

	@Override
	public void addLabelDD(String label, JDDNode labelDD)
	{
		JDDNode old = labelsDD.put(label, labelDD);
		if (old != null) JDD.Deref(old);
	}

	@Override
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
	 * Reset transition matrix DD.
	 * Note: Update reachable states and call {@code filterReachableStates}
	 * afterwards to update related information (trans01, odd, etc).
	 *
	 * <br>[ STORES: trans, DEREFS: <i>old transition matrix DD</i> ]
	 */
	public void resetTrans(JDDNode trans)
	{
		if (this.trans != null)
			JDD.Deref(this.trans);
		this.trans = trans;
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

	// do reachability

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

	// this method allows you to skip the reachability phase
	// it is only here for experimental purposes - not general use.

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
	 * Set the initial state(s) BDD (but don't re-do reachability).
	 * Old BDD is derefed if set already
	 */
	public void setStart(JDDNode start)
	{
		if (this.start != null)
			JDD.Deref(this.start);
		this.start = start;
		
		// work out number of initial states
		numStartStates = JDD.GetNumMinterms(start, allDDRowVars.n());
	}

	/**
	 * Set the DD used to store transition action label indices (MDPs).
	 */
	public void setTransActions(JDDNode transActions)
	{
		this.transActions = transActions;
	}

	/**
	 * Set the DDs used to store transition action label indices (D/CTMCs).
	 */
	public void setTransPerAction(JDDNode[] transPerAction)
	{
		this.transPerAction = transPerAction;
	}

	// remove non-reachable states from various dds
	// (and calculate num transitions)

	public void filterReachableStates()
	{
		int i;
		JDDNode tmp;

		// remove non-reachable states from transition matrix
		JDD.Ref(reach);
		trans = JDD.Apply(JDD.TIMES, reach, trans);
		JDD.Ref(reach);
		tmp = JDD.PermuteVariables(reach, allDDRowVars, allDDColVars);
		trans = JDD.Apply(JDD.TIMES, tmp, trans);

		// recalculate 0-1 version of trans
		JDD.Deref(trans01);
		JDD.Ref(trans);
		trans01 = JDD.GreaterThan(trans, 0);

		// remove non-reachable states from state/transition rewards
		for (i = 0; i < stateRewards.length; i++) {
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

		// Action label index info
		if (transActions != null) {
			// transActions just stored per state so only filter rows
			JDD.Ref(reach);
			transActions = JDD.Apply(JDD.TIMES, reach, transActions);
		}
		if (transPerAction != null) {
			// transPerAction stored as matrix so filter both rows/cols
			for (i = 0; i < numSynchs + 1; i++) {
				JDD.Ref(reach);
				transPerAction[i] = JDD.Apply(JDD.TIMES, reach, transPerAction[i]);
				JDD.Ref(reach);
				tmp = JDD.PermuteVariables(reach, allDDRowVars, allDDColVars);
				transPerAction[i] = JDD.Apply(JDD.TIMES, tmp, transPerAction[i]);
			}
		}

		// filter start states, work out number of initial states
		JDD.Ref(reach);
		start = JDD.Apply(JDD.TIMES, reach, start);
		numStartStates = JDD.GetNumMinterms(start, allDDRowVars.n());

		// work out number of transitions
		numTransitions = JDD.GetNumMinterms(trans01, getNumDDVarsInTrans());
	}

	@Override
	public void findDeadlocks(boolean fix)
	{
		// find states with at least one transition
		JDD.Ref(trans01);
		deadlocks = JDD.ThereExists(trans01, allDDColVars);

		// find reachable states with no transitions
		JDD.Ref(reach);
		deadlocks = JDD.And(reach, JDD.Not(deadlocks));
		
		if (fix && !deadlocks.equals(JDD.ZERO)) {
			// remove deadlocks by adding self-loops
			// also update transPerAction info, if present
			JDDNode tmp;
			JDD.Ref(deadlocks);
			tmp = JDD.And(deadlocks, JDD.Identity(allDDRowVars, allDDColVars));
			JDD.Ref(tmp);
			trans = JDD.Apply(JDD.PLUS, trans, tmp);
			JDD.Ref(tmp);
			trans01 = JDD.Apply(JDD.PLUS, trans01, tmp);
			if (transPerAction != null) {
				JDD.Ref(tmp);
				transPerAction[0] = JDD.Apply(JDD.PLUS, transPerAction[0], tmp);
			}
			JDD.Deref(tmp);
			// update model stats
			numTransitions = JDD.GetNumMinterms(trans01, getNumDDVarsInTrans());
		}
	}

	public void printTrans()
	{
		//		JDD.PrintMatrix(trans, allDDRowVars, allDDColVars);
	}

	public void printTrans01()
	{
		//		JDD.PrintMatrix(trans01, allDDRowVars, allDDColVars, JDD.ZERO_ONE);
	}

	public void printTransInfo(PrismLog log)
	{
		printTransInfo(log, false);
	}

	public void printTransInfo(PrismLog log, boolean extra)
	{
		int i, j, n;

		log.print("States:      " + getNumStatesString() + " (" + getNumStartStatesString() + " initial)" + "\n");
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
			if (transPerAction != null) {
				for (i = 0; i < numSynchs + 1; i++) {
					log.print("Action label info (");
					log.print((i == 0 ? "" : synchs.get(i - 1)) + "): ");
					log.println(JDD.GetInfoString(transPerAction[i], getNumDDVarsInTrans()));
				}
			}
			// Don't need to print info for transActions (only stored for MDPs)
		}
	}

	// export transition matrix to a file

	public void exportToFile(int exportType, boolean explicit, File file) throws FileNotFoundException, PrismException
	{
		if (!explicit) {
			PrismMTBDD.ExportMatrix(trans, getTransSymbol(), allDDRowVars, allDDColVars, odd, exportType, (file != null) ? file.getPath() : null);
		} else {
			PrismSparse.ExportMatrix(trans, getTransSymbol(), allDDRowVars, allDDColVars, odd, exportType, (file != null) ? file.getPath() : null);
		}
	}

	@Override
	public void exportStateRewardsToFile(int r, int exportType, File file) throws FileNotFoundException, PrismException
	{
		PrismMTBDD.ExportVector(stateRewards[r], "c" + (r + 1), allDDRowVars, odd, exportType, (file == null) ? null : file.getPath());
	}

	@Deprecated
	public String exportStateRewardsToFile(int exportType, File file) throws FileNotFoundException, PrismException
	{
		// export state rewards vector to a file
		// returns string containing files used if there were more than 1, null otherwise

		if (numRewardStructs == 0)
			throw new PrismException("There are no state rewards to export");
		int i;
		String filename, allFilenames = "";
		for (i = 0; i < numRewardStructs; i++) {
			filename = (file != null) ? file.getPath() : null;
			if (filename != null && numRewardStructs > 1) {
				filename = PrismUtils.addCounterSuffixToFilename(filename, i + 1);
				allFilenames += ((i > 0) ? ", " : "") + filename;
			}
			PrismMTBDD.ExportVector(stateRewards[i], "c" + (i + 1), allDDRowVars, odd, exportType, filename);
		}
		return (allFilenames.length() > 0) ? allFilenames : null;
	}

	@Override
	public void exportTransRewardsToFile(int r, int exportType, boolean ordered, File file) throws FileNotFoundException, PrismException
	{
		if (!ordered) {
			PrismMTBDD.ExportMatrix(transRewards[r], "C" + (r + 1), allDDRowVars, allDDColVars, odd, exportType, (file == null) ? null : file.getPath());
		} else {
			PrismSparse.ExportMatrix(transRewards[r], "C" + (r + 1), allDDRowVars, allDDColVars, odd, exportType, (file == null) ? null : file.getPath());
		}
	}

	@Deprecated
	public String exportTransRewardsToFile(int exportType, boolean explicit, File file) throws FileNotFoundException, PrismException
	{
		// export transition rewards matrix to a file
		// returns string containing files used if there were more than 1, null otherwise

		if (numRewardStructs == 0)
			throw new PrismException("There are no transition rewards to export");
		int i;
		String filename, allFilenames = "";
		for (i = 0; i < numRewardStructs; i++) {
			filename = (file != null) ? file.getPath() : null;
			if (filename != null && numRewardStructs > 1) {
				filename = PrismUtils.addCounterSuffixToFilename(filename, i + 1);
				allFilenames += ((i > 0) ? ", " : "") + filename;
			}
			if (!explicit) {
				PrismMTBDD.ExportMatrix(transRewards[i], "C" + (i + 1), allDDRowVars, allDDColVars, odd, exportType, filename);
			} else {
				PrismSparse.ExportMatrix(transRewards[i], "C" + (i + 1), allDDRowVars, allDDColVars, odd, exportType, filename);
			}
		}
		return (allFilenames.length() > 0) ? allFilenames : null;
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

	// convert global state index to local indices

	public String globalToLocal(long x)
	{
		int i;
		String s = "";

		s += "(";
		for (i = 0; i < numVars - 1; i++) {
			s += ((x / gtol[i + 1]) + varList.getLow(i)) + ",";
			x = x % gtol[i + 1];
		}
		s += (x + varList.getLow(numVars - 1)) + ")";

		return s;
	}

	// convert global state index to local index

	public int globalToLocal(long x, int l)
	{
		int i;

		for (i = 0; i < numVars - 1; i++) {
			if (i == l) {
				return (int) ((x / gtol[i + 1]) + varList.getLow(i));
			} else {
				x = x % gtol[i + 1];
			}
		}

		return (int) (x + varList.getLow(numVars - 1));
	}

	/**
	 * Apply the given model transformation operator to this model
	 * and return the resulting, transformed model.
	 * @param transformation the transformation operator
	 * @return the transformed model (needs to be cleared after use)
	 */
	public ProbModel getTransformed(ProbModelTransformationOperator transformation) throws PrismException
	{
		// New (transformed) model - dds, vars, etc.
		JDDNode newTrans, newStart;
		JDDVars newVarDDRowVars[], newVarDDColVars[];
		JDDVars newAllDDRowVars, newAllDDColVars;
		JDDNode newStateRewards[], newTransRewards[];
		ModelVariablesDD newModelVariables;
		VarList newVarList;
		String extraVar;
		// DRA stuff
		JDDVars extraDDRowVars, extraDDColVars;
		// Misc
		int i, n;
		boolean before;

		// Create a (new, unique) name for the variable that will represent the extra states
		extraVar = transformation.getExtraStateVariableName();
		while (varList.getIndex(extraVar) != -1) {
			extraVar = "_" + extraVar;
		}

		newModelVariables = this.getModelVariables().copy();

		// See how many new dd vars will be needed for the extra variables
		// and whether there is room to put them before rather than after the existing vars
		n = transformation.getExtraStateVariableCount();
		before = newModelVariables.canPrependExtraStateVariable(n);

		extraDDRowVars = new JDDVars();
		extraDDColVars = new JDDVars();
		// Create the new dd state variables
		JDDVars draVars = newModelVariables.allocateExtraStateVariable(n, extraVar, before);

		for (i = 0; i < n; i++) {
			extraDDRowVars.addVar(draVars.getVar(2*i));
			extraDDColVars.addVar(draVars.getVar(2*i+1));
		}

		// notify the transformation operator about the allocated state variables
		transformation.hookExtraStateVariableAllocation(extraDDRowVars.copy(), extraDDColVars.copy());

		// Create/populate new state variable lists
		if (n==0) {
			// no additional state vars, we can just copy everything
			newVarDDRowVars = JDDVars.copyArray(varDDRowVars);
			newVarDDColVars = JDDVars.copyArray(varDDColVars);
			newAllDDRowVars = allDDRowVars.copy();
			newAllDDColVars = allDDColVars.copy();
			newVarList = (VarList) varList.clone();
		} else {
			// insert new variable either before or after the other variables
			newVarDDRowVars = new JDDVars[varDDRowVars.length + 1];
			newVarDDColVars = new JDDVars[varDDRowVars.length + 1];
			newVarDDRowVars[before ? 0 : varDDRowVars.length] = extraDDRowVars.copy();
			newVarDDColVars[before ? 0 : varDDColVars.length] = extraDDColVars.copy();
			for (i = 0; i < varDDRowVars.length; i++) {
				newVarDDRowVars[before ? i + 1 : i] = varDDRowVars[i].copy();
				newVarDDColVars[before ? i + 1 : i] = varDDColVars[i].copy();
			}
			if (before) {
				newAllDDRowVars = extraDDRowVars.copy();
				newAllDDColVars = extraDDColVars.copy();
				newAllDDRowVars.copyVarsFrom(allDDRowVars);
				newAllDDColVars.copyVarsFrom(allDDColVars);
			} else {
				newAllDDRowVars = allDDRowVars.copy();
				newAllDDColVars = allDDColVars.copy();
				newAllDDRowVars.copyVarsFrom(extraDDRowVars);
				newAllDDColVars.copyVarsFrom(extraDDColVars);
			}
			newVarList = (VarList) varList.clone();
			Declaration decl = new Declaration(extraVar, new DeclarationInt(Expression.Int(0), Expression.Int((1 << n) - 1)));
			newVarList.addVar(before ? 0 : varList.getNumVars(), decl, 1, this.getConstantValues());
		}

		// Build transition matrix for transformed model
		newTrans = transformation.getTransformedTrans();

		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(newTrans, newAllDDRowVars, newAllDDColVars);
		}

		// Build set of initial states for transformed model
		newStart = transformation.getTransformedStart();

		if (SanityJDD.enabled) {
			SanityJDD.checkIsStateSet(newStart, newAllDDRowVars);
		}


		// Build transformed reward information
		newStateRewards = new JDDNode[stateRewards.length];
		for (i=0; i < stateRewards.length; i++) {
			newStateRewards[i] = transformation.getTransformedStateReward(stateRewards[i]);

			if (SanityJDD.enabled) {
				SanityJDD.checkIsDDOverVars(newStateRewards[i], newAllDDRowVars);
			}
		}

		newTransRewards = new JDDNode[transRewards.length];
		for (i=0; i < transRewards.length; i++) {
			newTransRewards[i] = transformation.getTransformedTransReward(transRewards[i]);

			if (SanityJDD.enabled) {
				SanityJDD.checkIsDDOverVars(newTransRewards[i], newAllDDRowVars, newAllDDColVars);
			}
		}


		// Create a new model model object to store the transformed model
		ProbModel result = new ProbModel(
				// New transition matrix/start state
				newTrans, newStart,
				// New reward information
				newStateRewards,
				newTransRewards,
				this.rewardStructNames.clone(),
				// New list of all row/col vars
				newAllDDRowVars, newAllDDColVars,
				// New model variables
				newModelVariables,
				// Module info (unchanged)
				this.getNumModules(),
				this.getModuleNames(),
				JDDVars.copyArray(this.getModuleDDRowVars()),
				JDDVars.copyArray(this.getModuleDDColVars()),
				// New var info
				newVarList.getNumVars(), newVarList, newVarDDRowVars, newVarDDColVars,
				// Constants (no change)
				this.getConstantValues());

		// Do reachability/etc. for the new model
		JDDNode S;
		if ( (S = transformation.getReachableStates()) != null) {
			// the transformation operator knows the reachable state set
			result.setReach(S);
		} else if ( (S = transformation.getReachableStateSeed()) != null ) {
			// the transformation operator knows a seed for the reachability computation
			result.doReachability(S);
		} else {
			// otherwise: do standard reachability
			result.doReachability();
		}
		result.filterReachableStates();

		if (!transformation.deadlocksAreFine()) {
			result.findDeadlocks(false);
			if (result.getDeadlockStates().size() > 0) {
				// Assuming original model has no deadlocks, neither should the transformed model
				throw new PrismException("Transformed model has deadlock states");
			}
		}

		// lift labels attached to the model
		for (Entry<String, JDDNode> entry : labelsDD.entrySet()) {
			JDDNode labelStates = entry.getValue();
			JDDNode transformedLabelStates = transformation.getTransformedLabelStates(labelStates, result.getReach());
			result.labelsDD.put(entry.getKey(), transformedLabelStates);
		}

		extraDDRowVars.derefAll();
		extraDDColVars.derefAll();

		return result;
	}

	/**
	 * Convert a BDD (over model row variables) representing a single state to a State object. 
	 */
	public State convertBddToState(JDDNode dd)
	{
		JDDNode ptr;
		int i, n;
		BitSet bits;
		// First convert path through BDD to a bit vector
		ptr = dd;
		n = allDDRowVars.n();
		bits = new BitSet(n);
		for (i = 0; i < n; i++) {
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
	 * Convert a BDD (over model row variables) representing a single state
	 * to a (reachable) state index. 
	 */
	public int convertBddToIndex(JDDNode dd) throws PrismNotSupportedException
	{
		JDDNode ptr;
		ODDNode oddPtr;
		int i, n, index;

		ODDUtils.checkInt(odd, "Cannot convert Bdd to index in model");
		// Traverse BDD and ODD simultaneously to compute index
		ptr = dd;
		oddPtr = odd;
		n = allDDRowVars.n();
		index = 0;
		for (i = 0; i < n; i++) {
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
	 * Clear the model (deref all DDs and DD variables)
	 */
	public void clear()
	{
		for (Entry<String, JDDNode> labelDD : labelsDD.entrySet()) {
			JDD.Deref(labelDD.getValue());
		}
		labelsDD.clear();

		if (varDDRowVars != null)
			JDDVars.derefAllArray(varDDRowVars);
		if (varDDColVars != null)
			JDDVars.derefAllArray(varDDColVars);

		if (moduleDDRowVars != null)
			JDDVars.derefAllArray(moduleDDRowVars);
		if (moduleDDColVars != null)
			JDDVars.derefAllArray(moduleDDColVars);

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
		if (transActions != null)
			JDD.Deref(transActions);
		if (transPerAction != null) {
			for (int i = 0; i < numSynchs + 1; i++) {
				JDD.Deref(transPerAction[i]);
			}
		}

		if (odd != null) {
			// clear ODD
			ODDUtils.ClearODD(odd);
			odd = null;
		}

		if (modelVariables != null)
			modelVariables.clear();
	}

}
