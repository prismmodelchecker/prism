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
import java.util.Vector;

import jdd.*;
import odd.*;
import mtbdd.*;
import parser.*;
import sparse.*;

/*
 * Class to store a PRISM model which is a DTMC
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
	protected JDDNode deadlocks; // deadlock states dd
	protected JDDNode fixdl; // fixed deadlock states dd
	protected JDDNode stateRewards[]; // state rewards dds
	protected JDDNode transRewards[]; // transition rewards dds
	// dd vars
	protected JDDVars[] varDDRowVars; // dd vars for each module variable (rows)
	protected JDDVars[] varDDColVars; // dd vars for each module variable (cols)
	protected JDDVars[] moduleDDRowVars; // dd vars for each module (rows)
	protected JDDVars[] moduleDDColVars; // dd vars for each module (cols)
	protected JDDVars allDDRowVars; // all dd vars (rows)
	protected JDDVars allDDColVars; // all dd vars (cols)
	// names for all dd vars used
	protected Vector<String> ddVarNames;

	protected ODDNode odd; // odd

	// accessor methods

	// model info

	// type
	public int getType()
	{
		return Model.DTMC;
	}

	public String getTypeString()
	{
		return "Probabilistic (DTMC)"; // TODO: Change this after regression testing
		//return "DTMC";
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

	public JDDNode getDeadlocks()
	{
		return deadlocks;
	}

	public JDDNode getFixedDeadlocks()
	{
		return fixdl;
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
		return ddVarNames;
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

	public ProbModel(JDDNode tr, JDDNode s, JDDNode sr[], JDDNode trr[], String rsn[], JDDVars arv, JDDVars acv,
			Vector<String> ddvn, int nm, String[] mn, JDDVars[] mrv, JDDVars[] mcv, int nv, VarList vl, JDDVars[] vrv,
			JDDVars[] vcv, Values cv)
	{
		int i;

		trans = tr;
		start = s;
		fixdl = JDD.Constant(0);
		stateRewards = sr;
		transRewards = trr;
		numRewardStructs = stateRewards.length; // which should == transRewards.length
		rewardStructNames = rsn;
		allDDRowVars = arv;
		allDDColVars = acv;
		ddVarNames = ddvn;
		numModules = nm;
		moduleNames = mn;
		moduleDDRowVars = mrv;
		moduleDDColVars = mcv;
		numVars = nv;
		varList = vl;
		varDDRowVars = vrv;
		varDDColVars = vcv;
		constantValues = cv;

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

	// do reachability

	public void doReachability()
	{
		doReachability(false);
	}

	public void doReachability(boolean extraReachInfo)
	{
		// compute reachable states
		reach = PrismMTBDD.Reachability(trans01, allDDRowVars, allDDColVars, start, extraReachInfo ? 1 : 0);

		// work out number of reachable states
		numStates = JDD.GetNumMinterms(reach, allDDRowVars.n());

		// build odd
		odd = ODDUtils.BuildODD(reach, allDDRowVars);
	}

	// this method allows you to skip the reachability phase
	// it is only here for experimental purposes - not general use.

	public void skipReachability()
	{
		// don't compute reachable states - assume all reachable
		reach = JDD.Constant(1);

		// work out number of reachable states
		numStates = Math.pow(2, allDDRowVars.n());

		// build odd
		odd = ODDUtils.BuildODD(reach, allDDRowVars);
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
		
		// work out number of transitions
		numTransitions = JDD.GetNumMinterms(trans01, getNumDDVarsInTrans());
	}

	// identify any deadlock states

	public void findDeadlocks()
	{
		// find states with at least one transition
		JDD.Ref(trans01);
		deadlocks = JDD.ThereExists(trans01, allDDColVars);

		// find reachable states with no transitions
		JDD.Ref(reach);
		deadlocks = JDD.And(reach, JDD.Not(deadlocks));
	}

	// remove deadlocks by adding self-loops

	public void fixDeadlocks()
	{
		JDDNode tmp;

		if (!deadlocks.equals(JDD.ZERO)) {
			// remove deadlocks by adding self-loops
			JDD.Ref(deadlocks);
			tmp = JDD.And(deadlocks, JDD.Identity(allDDRowVars, allDDColVars));
			JDD.Ref(tmp);
			trans = JDD.Apply(JDD.PLUS, trans, tmp);
			trans01 = JDD.Apply(JDD.PLUS, trans01, tmp);
			// update lists of deadlocks
			JDD.Deref(fixdl);
			fixdl = deadlocks;
			deadlocks = JDD.Constant(0);
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
				log.print(" " + j + ":" + ddVarNames.get(j));
				j = allDDColVars.getVarIndex(i);
				log.print(" " + j + ":" + ddVarNames.get(j));
			}
			log.println();
			log.print(getTransName() + " terminals: " + JDD.GetTerminalsAndNumbersString(trans, getNumDDVarsInTrans())
					+ "\n");
			log.print("Reach: " + JDD.GetNumNodes(reach) + " nodes\n");
			log.print("ODD: " + ODDUtils.GetNumODDNodes() + " nodes\n");
		}
		for (i = 0; i < numRewardStructs; i++) {
			if (stateRewards[i] != null && !stateRewards[i].equals(JDD.ZERO)) {
				log.print("State rewards (" + (i + 1)
						+ (("".equals(rewardStructNames[i])) ? "" : (":\"" + rewardStructNames[i] + "\"")) + "): ");
				log.print(JDD.GetNumNodes(stateRewards[i]) + " nodes (");
				log.print(JDD.GetNumTerminals(stateRewards[i]) + " terminal), ");
				log.print(JDD.GetNumMintermsString(stateRewards[i], getNumDDRowVars()) + " minterms\n");
				if (extra) {
					log.print("State rewards terminals (" + (i + 1)
							+ (("".equals(rewardStructNames[i])) ? "" : (":\"" + rewardStructNames[i] + "\"")) + "): ");
					log.print(JDD.GetTerminalsAndNumbersString(stateRewards[i], getNumDDRowVars()) + "\n");
				}
			}
			if (transRewards[i] != null && !transRewards[i].equals(JDD.ZERO)) {
				log.print("Transition rewards (" + (i + 1)
						+ (("".equals(rewardStructNames[i])) ? "" : (":\"" + rewardStructNames[i] + "\"")) + "): ");
				log.print(JDD.GetNumNodes(transRewards[i]) + " nodes (");
				log.print(JDD.GetNumTerminals(transRewards[i]) + " terminal), ");
				log.print(JDD.GetNumMintermsString(transRewards[i], getNumDDVarsInTrans()) + " minterms\n");
				if (extra) {
					log.print("Transition rewards terminals (" + (i + 1)
							+ (("".equals(rewardStructNames[i])) ? "" : (":\"" + rewardStructNames[i] + "\"")) + "): ");
					log.print(JDD.GetTerminalsAndNumbersString(transRewards[i], getNumDDVarsInTrans()) + "\n");
				}
			}
		}
	}

	// export transition matrix to a file

	public void exportToFile(int exportType, boolean explicit, File file) throws FileNotFoundException
	{
		if (!explicit) {
			PrismMTBDD.ExportMatrix(trans, getTransSymbol(), allDDRowVars, allDDColVars, odd, exportType,
					(file != null) ? file.getPath() : null);
		} else {
			PrismSparse.ExportMatrix(trans, getTransSymbol(), allDDRowVars, allDDColVars, odd, exportType,
					(file != null) ? file.getPath() : null);
		}
	}

	// export state rewards vector to a file

	// returns string containing files used if there were more than 1, null otherwise

	public String exportStateRewardsToFile(int exportType, File file) throws FileNotFoundException, PrismException
	{
		if (numRewardStructs == 0)
			throw new PrismException("There are no state rewards to export");
		int i;
		String filename, allFilenames = "";
		for (i = 0; i < numRewardStructs; i++) {
			filename = (file != null) ? file.getPath() : null;
			if (filename != null && numRewardStructs > 1) {
				filename = PrismUtils.addCounterSuffixToFilename(filename, i);
				allFilenames += ((i > 0) ? ", " : "") + filename;
			}
			PrismMTBDD.ExportVector(stateRewards[i], "c" + i, allDDRowVars, odd, exportType, filename);
		}
		return (allFilenames.length() > 0) ? allFilenames : null;
	}

	// export transition rewards matrix to a file

	// returns string containing files used if there were more than 1, null otherwise

	public String exportTransRewardsToFile(int exportType, boolean explicit, File file) throws FileNotFoundException,
			PrismException
	{
		if (numRewardStructs == 0)
			throw new PrismException("There are no transition rewards to export");
		int i;
		String filename, allFilenames = "";
		for (i = 0; i < numRewardStructs; i++) {
			filename = (file != null) ? file.getPath() : null;
			if (filename != null && numRewardStructs > 1) {
				filename = PrismUtils.addCounterSuffixToFilename(filename, i);
				allFilenames += ((i > 0) ? ", " : "") + filename;
			}
			if (!explicit) {
				PrismMTBDD
						.ExportMatrix(transRewards[i], "C" + i, allDDRowVars, allDDColVars, odd, exportType, filename);
			} else {
				PrismSparse.ExportMatrix(transRewards[i], "C" + i, allDDRowVars, allDDColVars, odd, exportType,
						filename);
			}
		}
		return (allFilenames.length() > 0) ? allFilenames : null;
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

	// clear up (deref all dds, dd vars)

	public void clear()
	{
		for (int i = 0; i < numVars; i++) {
			varDDRowVars[i].derefAll();
			varDDColVars[i].derefAll();
		}
		for (int i = 0; i < numModules; i++) {
			moduleDDRowVars[i].derefAll();
			moduleDDColVars[i].derefAll();
		}
		allDDRowVars.derefAll();
		allDDColVars.derefAll();
		JDD.Deref(trans);
		JDD.Deref(trans01);
		JDD.Deref(start);
		if (reach != null) JDD.Deref(reach);
		if (deadlocks != null) JDD.Deref(deadlocks);
		JDD.Deref(fixdl);
		for (int i = 0; i < numRewardStructs; i++) {
			JDD.Deref(stateRewards[i]);
			JDD.Deref(transRewards[i]);
		}
	}
}
