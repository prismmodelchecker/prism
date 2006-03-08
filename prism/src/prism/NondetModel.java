//==============================================================================
//	
//	Copyright (c) 2002-2006, Dave Parker
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

// class to store an instance of a concurrent probabilistic model (mtbdds)

public class NondetModel implements Model
{
	// model info
	
	// type
	private static final String type = "Nondeterministic (MDP)";
	// modules
	private int numModules;				// number of modules
	private String[] moduleNames;		// module names
	// vars/consts
	private int numVars;				// total number of module variables
	private VarList varList;			// list of module variables
	private long[] gtol;				// numbers for use by globalToLocal
	private Values constantValues;		// values of constants
	// stats
	private double numStates;			// number of states
	private double numTransitions;		// number of transitions
	private double numChoices;			// number of choices
	private double numStartStates;		// number of initial states
	// reachable state list
	private StateListMTBDD reachStateList = null;
	// deadlock state list
	private StateListMTBDD deadlockStateList = null;
	// initial state list
	private StateListMTBDD startStateList = null;

	// mtbdd stuff

	// dds
	private JDDNode trans;			// transition matrix dd
	private JDDNode trans01;		// 0-1 transition matrix dd
	private JDDNode start;			// start state dd
	private JDDNode reach;			// reachable states dd
	private JDDNode deadlocks;		// deadlock states dd
	private JDDNode fixdl;			// fixed deadlock states dd
	private JDDNode nondetMask;		// mask for nondeterministic choices
	private JDDNode stateRewards;	// state rewards dd
	private JDDNode transRewards;	// transition rewards dd
	// dd vars
	private JDDVars[] varDDRowVars;		// dd vars for each module variable (rows)
	private JDDVars[] varDDColVars;		// dd vars for each module variable (cols)
	private JDDVars[] moduleDDRowVars;	// dd vars for each module (rows)
	private JDDVars[] moduleDDColVars;	// dd vars for each module (cols)
	private JDDVars allDDRowVars;		// all dd vars (rows)
	private JDDVars allDDColVars;		// all dd vars (cols)
	private JDDVars allDDSynchVars;		// synch actions dd vars
	private JDDVars allDDSchedVars;		// scheduler dd vars
	private JDDVars allDDChoiceVars;	// local nondet choice dd vars
	private JDDVars allDDNondetVars;	// all nondet dd vars (union of two above)
	// names for all dd vars used
	private Vector ddVarNames;
	
	private ODDNode odd;			// odd

	// accessor methods
	
	// model info
	
	// type
	public String getType()				{ return type; }
	// modules
	public int getNumModules()			{ return numModules; }
	public String[] getModuleNames()		{ return moduleNames; }
	public String getModuleName(int i)		{ return moduleNames[i]; }
	// vars
	public int getNumVars()				{ return numVars; }
	public VarList getVarList()			{ return varList; }
	public String getVarName(int i)			{ return varList.getName(i); }
	public int getVarIndex(String n)		{ return varList.getIndex(n); }
	public int getVarModule(int i)			{ return varList.getModule(i); }
	public int getVarLow(int i)			{ return varList.getLow(i); }
	public int getVarHigh(int i)			{ return varList.getHigh(i); }
	public int getVarRange(int i)			{ return varList.getRange(i); }
	public Values getConstantValues()		{ return constantValues; }
	// stats
	public long getNumStates()			{ return (numStates>Long.MAX_VALUE) ? -1 : (long)numStates; }
	public long getNumTransitions()			{ return (numTransitions>Long.MAX_VALUE) ? -1 : (long)numTransitions; }
	public long getNumChoices()			{ return (numChoices>Long.MAX_VALUE) ? -1 : (long)numChoices; }
	public long getNumStartStates()			{ return (numStartStates>Long.MAX_VALUE) ? -1 : (long)numStartStates; }
	// additional methods to get stats as strings
	public String getNumStatesString()		{ return PrismUtils.bigIntToString(numStates); }
	public String getNumTransitionsString()		{ return PrismUtils.bigIntToString(numTransitions); }
	public String getNumChoicesString()		{ return PrismUtils.bigIntToString(numChoices); }
	public String getNumStartStatesString()		{ return PrismUtils.bigIntToString(numStartStates); }
	// lists of states
	public StateList getReachableStates()		{ return reachStateList; }
	public StateList getDeadlockStates()		{ return deadlockStateList; }
	public StateList getStartStates()		{ return startStateList; }
	
	// mtbdd stuff
	
	// dds
	public JDDNode getTrans()			{ return trans; }
	public JDDNode getTrans01()			{ return trans01; }
	public JDDNode getStart()			{ return start; }
	public JDDNode getReach()		 	{ return reach; }
	public JDDNode getDeadlocks()		 	{ return deadlocks; }
	public JDDNode getFixedDeadlocks()		 	{ return fixdl; }
	public JDDNode getNondetMask()			{ return nondetMask; }
	public JDDNode getStateRewards()		{ return stateRewards; }
	public JDDNode getTransRewards()		{ return transRewards; }
	// dd vars
	public JDDVars[] getVarDDRowVars()		{ return varDDRowVars; }
	public JDDVars[] getVarDDColVars()		{ return varDDColVars; }
	public JDDVars getVarDDRowVars(int i)		{ return varDDRowVars[i]; }
	public JDDVars getVarDDColVars(int i)		{ return varDDColVars[i]; }
	public JDDVars[] getModuleDDRowVars()		{ return moduleDDRowVars; }
	public JDDVars[] getModuleDDColVars()		{ return moduleDDColVars; }
	public JDDVars getModuleDDRowVars(int i)	{ return moduleDDRowVars[i]; }
	public JDDVars getModuleDDColVars(int i)	{ return moduleDDColVars[i]; }
	public JDDVars getAllDDRowVars()		{ return allDDRowVars; }
	public JDDVars getAllDDColVars()		{ return allDDColVars; }
	public JDDVars getAllDDSchedVars()		{ return allDDSchedVars; }
	public JDDVars getAllDDChoiceVars()		{ return allDDChoiceVars; }
	public JDDVars getAllDDNondetVars()		{ return allDDNondetVars; }
	// additional useful methods to do with dd vars
	public int getNumDDRowVars()			{ return allDDRowVars.n(); }
	public int getNumDDColVars()			{ return allDDColVars.n(); }
	public int getNumDDNondetVars()			{ return allDDNondetVars.n(); }
	public int getNumDDVarsInTrans()		{ return allDDRowVars.n()*2+allDDNondetVars.n(); }
	public Vector getDDVarNames()			{ return ddVarNames; }
	
	public ODDNode getODD()				{ return odd; }
	
	// constructor
	
	public NondetModel(JDDNode tr, JDDNode tr01, JDDNode s, JDDNode r, JDDNode dl, JDDNode sr, JDDNode trr, JDDVars arv, JDDVars acv,
				JDDVars asyv,  JDDVars asv, JDDVars achv, JDDVars andv, Vector ddvn,
				int nm, String[] mn, JDDVars[] mrv, JDDVars[] mcv,
				int nv, VarList vl, JDDVars[] vrv, JDDVars[] vcv, Values cv)
	{
		int i;
		double d;
		
		trans = tr;
		trans01 = tr01;
		start = s;
		reach = r;
		deadlocks = dl;
		fixdl = JDD.Constant(0);
		stateRewards = sr;
		transRewards = trr;
		allDDRowVars = arv;
		allDDColVars = acv;
		allDDSynchVars = asyv;
		allDDSchedVars = asv;
		allDDChoiceVars = achv;
		allDDNondetVars = andv;
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
		for (i = numVars-2; i >= 0; i--) {
			gtol[i] = gtol[i] * gtol[i+1];
		}
		
		// build mask for nondeterminstic choices
		JDD.Ref(trans01);
		JDD.Ref(reach);
		// nb: this assumes that there are no deadlock states
		nondetMask = JDD.And(JDD.Not(JDD.ThereExists(trans01, allDDColVars)), reach);
		
		// work out number of states
		numStates = JDD.GetNumMinterms(reach, allDDRowVars.n());
		numStartStates = JDD.GetNumMinterms(start, allDDRowVars.n());
		
		// work out number of transitions
		numTransitions = JDD.GetNumMinterms(trans01, allDDRowVars.n()*2+allDDNondetVars.n());		
		
		// work out number of choices
		d = JDD.GetNumMinterms(nondetMask,  getNumDDRowVars()+getNumDDNondetVars());
		numChoices = ((Math.pow(2, getNumDDNondetVars())) * numStates) - d;
				
		// build odd
		odd = ODDUtils.BuildODD(reach, allDDRowVars);
		
		// store reachable states in a StateList
		reachStateList = new StateListMTBDD(reach, this);
		
		// store deadlock states in a StateList
		deadlockStateList = new StateListMTBDD(deadlocks, this);
		
		// store initial states in a StateList
		startStateList = new StateListMTBDD(start, this);
	}
	
	public void fixDeadlocks()
	{
		JDDNode tmp;
		double d;
		
		if (!deadlocks.equals(JDD.ZERO)) {
			// remove deadlocks by adding self-loops
			JDD.Ref(deadlocks);
			tmp = JDD.SetVectorElement(JDD.Constant(0), allDDNondetVars, 0, 1);
			tmp = JDD.And(tmp, JDD.Identity(allDDRowVars, allDDColVars));
			tmp = JDD.And(deadlocks, tmp);
			JDD.Ref(tmp);
			trans = JDD.Apply(JDD.PLUS, trans, tmp);
			trans01 = JDD.Apply(JDD.PLUS, trans01, tmp);
			// update lists of deadlocks
			JDD.Deref(fixdl);
			fixdl = deadlocks;
			deadlocks = JDD.Constant(0);
			deadlockStateList = new StateListMTBDD(deadlocks, this);
			// update mask
			JDD.Deref(nondetMask);
			JDD.Ref(trans01);
			JDD.Ref(reach);
			nondetMask = JDD.And(JDD.Not(JDD.ThereExists(trans01, allDDColVars)), reach);
			// update model stats
			numTransitions = JDD.GetNumMinterms(trans01, getNumDDVarsInTrans());		
			d = JDD.GetNumMinterms(nondetMask, getNumDDRowVars()+getNumDDNondetVars());
			numChoices = ((Math.pow(2, getNumDDNondetVars())) * numStates) - d;
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
		log.print("States:      " + getNumStatesString() + " (" + getNumStartStatesString() + " initial)" + "\n");
		log.print("Transitions: " + getNumTransitionsString() + "\n");
		log.print("Choices:     " + getNumChoicesString() + "\n");
		
		log.println();
		
		log.print("Transition matrix: ");
		log.print(JDD.GetNumNodes(trans) + " nodes (");
		log.print(JDD.GetNumTerminals(trans) + " terminal), ");
		log.print(JDD.GetNumMintermsString(trans, getNumDDVarsInTrans()) + " minterms, ");
		log.print("vars: " + getNumDDRowVars() + "r/" + getNumDDColVars() + "c/" + getNumDDNondetVars() + "nd\n");
		
		//log.print("ODD: " + ODDUtils.GetNumODDNodes() + " nodes\n");
		
		//log.print("Mask: " + JDD.GetNumNodes(nondetMask) + " nodes\n");
		
		if (stateRewards != null && !stateRewards.equals(JDD.ZERO)) {
			log.print("State rewards: ");
			log.print(JDD.GetNumNodes(stateRewards) + " nodes (");
			log.print(JDD.GetNumTerminals(stateRewards) + " terminal), ");
			log.print(JDD.GetNumMintermsString(stateRewards, getNumDDRowVars()) + " minterms\n");
		}
		if (transRewards != null && !transRewards.equals(JDD.ZERO)) {
			log.print("Transition rewards: ");
			log.print(JDD.GetNumNodes(transRewards) + " nodes (");
			log.print(JDD.GetNumTerminals(transRewards) + " terminal), ");
			log.print(JDD.GetNumMintermsString(transRewards, getNumDDVarsInTrans()) + " minterms\n");
		}
	}

	// export transition matrix to a file
	
	public void exportToFile(int exportType, boolean explicit, File file) throws FileNotFoundException
	{
		if (!explicit) {
			// can only do explicit (sparse matrix based) export for mdps
		}
		else {
			PrismSparse.ExportMDP(trans, "S", allDDRowVars, allDDColVars, allDDNondetVars, odd, exportType, (file != null)?file.getPath():null);
		}
	}

	// export state rewards vector to a file
	
	public void exportStateRewardsToFile(int exportType, File file) throws FileNotFoundException, PrismException
	{
		if (stateRewards == null || stateRewards.equals(JDD.ZERO)) throw new PrismException("There are no state rewards to export");
		
		PrismMTBDD.ExportVector(stateRewards, "c", allDDRowVars, odd, exportType, (file != null)?file.getPath():null);
	}

	// export transition rewards matrix to a file
	
	public void exportTransRewardsToFile(int exportType, boolean explicit, File file) throws FileNotFoundException, PrismException
	{
		if (transRewards == null || transRewards.equals(JDD.ZERO)) throw new PrismException("There are no transition rewards to export");
		
		if (!explicit) {
			// can only do explicit (sparse matrix based) export for mdps
		}
		else {
			PrismSparse.ExportSubMDP(trans, transRewards, "C", allDDRowVars, allDDColVars, allDDNondetVars, odd, exportType, (file != null)?file.getPath():null);
		}
	}

	// convert global state index to local indices
	
	public String globalToLocal(long x)
	{
		int i;
		String s = "";
		
		s += "(";
		for (i = 0; i < numVars-1; i++) {
			s += ((x/gtol[i+1]) + varList.getLow(i)) + ",";
			x = x % gtol[i+1];
		}
		s += (x + varList.getLow(numVars-1)) + ")";
		
		return s;
	}

	// convert global state index to local index
	
	public int globalToLocal(long x, int l)
	{
		int i;
		
		for (i = 0; i < numVars-1; i++) {
			if (i == l) {
				return (int)((x/gtol[i+1]) + varList.getLow(i));
			}
			else {
				x = x % gtol[i+1];
			}
		}
		
		return (int)(x + varList.getLow(numVars-1));
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
		allDDSynchVars.derefAll();
		allDDSchedVars.derefAll();
		allDDChoiceVars.derefAll();
		allDDNondetVars.derefAll();
		JDD.Deref(trans);
		JDD.Deref(trans01);
		JDD.Deref(start);
		JDD.Deref(reach);
		JDD.Deref(deadlocks);
		JDD.Deref(fixdl);
		JDD.Deref(nondetMask);
		JDD.Deref(stateRewards);
		JDD.Deref(transRewards);
	}
}

//------------------------------------------------------------------------------
