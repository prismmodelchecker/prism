//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

import java.util.Vector;
import java.util.HashSet;
import java.util.Iterator;

import jdd.*;
import parser.*;
import parser.ast.*;

// class to translate a modules description file into an MTBDD model

public class Modules2MTBDD
{
	// Prism object
	private Prism prism;
	
	// StateModelChecker for expression -> MTBDD conversion
	private StateModelChecker expr2mtbdd;
	
	// logs
	private PrismLog mainLog;		// main log

	// ModulesFile object to store syntax tree from parser
	private ModulesFile modulesFile;
	
	// model info
	
	// type
	private ModelType modelType;				// model type (dtmc/mdp/ctmc)
	// modules
	private int numModules;			// number of modules
	private String[] moduleNames;	// module names
	// vars/constants
	private int numVars;			// total number of module variables
	private VarList varList;		// list of module variables
	private Values constantValues;	// values of constants
	// synch info
	private int numSynchs;			// number of synchronisations
	private Vector<String> synchs;			// synchronisations
	// rewards
	private int numRewardStructs;		// number of reward structs
	private String[] rewardStructNames;	// reward struct names
	
	// mtbdd stuff
	
	// dds/dd vars - whole system
	private JDDNode trans;				// transition matrix dd
	private JDDNode range;				// dd giving range for system
	private JDDNode start;				// dd for start state
	private JDDNode stateRewards[];		// dds for state rewards
	private JDDNode transRewards[];		// dds for transition rewards
	private JDDNode transActions;	// dd for transition action labels (MDPs)
	private JDDNode transPerAction[];	// dds for transitions for each action (D/CTMCs)
	private JDDNode transInd;	// dds for independent bits of trans
	private JDDNode transSynch[];	// dds for synch action parts of trans
	private JDDVars allDDRowVars;		// all dd vars (rows)
	private JDDVars allDDColVars;		// all dd vars (cols)
	private JDDVars allDDSynchVars;		// all dd vars (synchronising actions)
	private JDDVars allDDSchedVars;		// all dd vars (scheduling)
	private JDDVars allDDChoiceVars;	// all dd vars (internal non-det.)
	private JDDVars allDDNondetVars;	// all dd vars (all non-det.)
	// dds/dd vars - globals
	private JDDVars globalDDRowVars;	// dd vars for all globals (rows)
	private JDDVars globalDDColVars;	// dd vars for all globals (cols)
	// dds/dd vars - modules
	private JDDVars[] moduleDDRowVars;	// dd vars for each module (rows)
	private JDDVars[] moduleDDColVars;	// dd vars for each module (cols)
	private JDDNode[] moduleRangeDDs;	// dd giving range for each module
	private JDDNode[] moduleIdentities;	// identity matrix for each module
	// dds/dd vars - variables
	private JDDVars[] varDDRowVars;		// dd vars (row/col) for each module variable
	private JDDVars[] varDDColVars;
	private JDDNode[] varRangeDDs;		// dd giving range for each module variable
	private JDDNode[] varColRangeDDs;	// dd giving range for each module variable (in col vars)
	private JDDNode[] varIdentities;	// identity matrix for each module variable
	// dds/dd vars - nondeterminism
	private JDDNode[] ddSynchVars;		// individual dd vars for synchronising actions
	private JDDNode[] ddSchedVars;		// individual dd vars for scheduling non-det.
	private JDDNode[] ddChoiceVars;		// individual dd vars for local non-det.

	private ModelVariablesDD modelVariables;
	
	// flags for keeping track of which variables have been used
	private boolean[] varsUsed;
	
	// symmetry info
	private boolean doSymmetry;			// use symmetry reduction
	private JDDNode symm; 				// dd of symmetric states
	private JDDNode nonSymms[];			// dds of non-(i,i+1)-symmetric states (i=0...numSymmModules-1)
	private int numModulesBeforeSymm;	// number of modules in the PRISM file before the symmetric ones
	private int numModulesAfterSymm;	// number of modules in the PRISM file after the symmetric ones
	private int numSymmModules;			// number of symmetric components
	
	// hidden option - do we also store each part of the transition matrix separately? (now defunct)
	private boolean storeTransParts = false; 
	// hidden option - do we also store action info for the transition matrix? (supersedes the above)
	private boolean storeTransActions = true; 
	
	// data structure used to store mtbdds and related info
	// for some component of the whole model

	private class ComponentDDs
	{
		public JDDNode guards;		// bdd for guards
		public JDDNode trans;		// mtbdd for transitions
		public JDDNode rewards[];	// mtbdd for rewards
		public int min; 			// min index of dd vars used for local nondeterminism
		public int max; 			// max index of dd vars used for local nondeterminism
		public ComponentDDs()
		{
			rewards = new JDDNode[modulesFile.getNumRewardStructs()];
		}
	}
	
	// data structure used to store mtbdds and related info
	// for some part of the system definition

	private class SystemDDs
	{
		public ComponentDDs ind;		// for independent bit (i.e. tau action)
		public ComponentDDs[] synchs;	// for each synchronising action
		public JDDNode id;	 			// mtbdd for identity matrix
		public HashSet<String> allSynchs;		// set of all synchs used (syntactic)
		public SystemDDs(int n)
		{
			synchs = new ComponentDDs[n];
			allSynchs = new HashSet<String>();
		}
	}
	
	// constructor
	
	public Modules2MTBDD(Prism p, ModulesFile mf)
	{
		prism = p;
		mainLog = p.getMainLog();
		modulesFile = mf;
		// get symmetry reduction info
		String s = prism.getSettings().getString(PrismSettings.PRISM_SYMM_RED_PARAMS);
		doSymmetry = !(s == null || s == "");
	}
	
	@SuppressWarnings("unchecked") // for clone of vector in translate()

	// main method - translate
	public Model translate() throws PrismException
	{
		Model model = null;
		JDDNode tmp, tmp2;
		JDDVars ddv;
		int i;
		
		// get variable info from ModulesFile
		varList = modulesFile.createVarList();
		if (modulesFile.containsUnboundedVariables())
			throw new PrismNotSupportedException("Cannot build a model that contains a variable with unbounded range (try the explicit engine instead)");
		numVars = varList.getNumVars();
		constantValues = modulesFile.getConstantValues();
		
		// get basic system info
		modelType = modulesFile.getModelType();
		moduleNames = modulesFile.getModuleNames();
		numModules = modulesFile.getNumModules();
		synchs = modulesFile.getSynchs();
		numSynchs = synchs.size();
		
		// check model type is supported
		if (!(modelType == ModelType.DTMC || modelType == ModelType.MDP || modelType == ModelType.CTMC)) {
			throw new PrismException("Symbolic construction of " + modelType + "s not supported");
		}
		
		// allocate dd variables
		allocateDDVars();
		sortDDVars();
		sortIdentities();
		sortRanges();
		
		// create stripped-down StateModelChecker for expression to MTBDD conversions
		expr2mtbdd = new StateModelChecker(prism, varList, allDDRowVars, varDDRowVars, constantValues);
		
		// translate modules file into dd
		translateModules();
		
		// get rid of any nondet dd variables not needed
		if (modelType == ModelType.MDP) {
			tmp = JDD.GetSupport(trans);
			tmp = JDD.ThereExists(tmp, allDDRowVars);
			tmp = JDD.ThereExists(tmp, allDDColVars);
			tmp2 = tmp;
			ddv = new JDDVars();
			while (!tmp2.equals(JDD.ONE)) {
				ddv.addVar(JDD.Var(tmp2.getIndex()));
				tmp2 = tmp2.getThen();
			}
			JDD.Deref(tmp);
			allDDNondetVars.derefAll();
			allDDNondetVars = ddv;
		}
		
// 		// print dd variables actually used (support of trans)
// 		mainLog.print("\nMTBDD variables used (" + allDDRowVars.n() + "r, " + allDDRowVars.n() + "c");
// 		if (type == ModulesFile.NONDETERMINISTIC) mainLog.print(", " + allDDNondetVars.n() + "nd");
// 		mainLog.print("):");
// 		tmp = JDD.GetSupport(trans);
// 		tmp2 = tmp;
// 		while (!tmp2.isConstant()) {
// 			//mainLog.print(" " + tmp2.getIndex() + ":" + ddVarNames.elementAt(tmp2.getIndex()));
// 			mainLog.print(" " + ddVarNames.elementAt(tmp2.getIndex()));
// 			tmp2 = tmp2.getThen();
// 		}
// 		mainLog.println();
// 		JDD.Deref(tmp);
		
		// Print some info (if extraddinfo flag on)
		if (prism.getExtraDDInfo()) {
			mainLog.print("Transition matrix (pre-reachability): ");
			mainLog.print(JDD.GetNumNodes(trans) + " nodes (");
			mainLog.print(JDD.GetNumTerminals(trans) + " terminal)\n");
		}
		
		// build bdd for initial state(s)
		buildInitialStates();
		
		// store reward struct names
		rewardStructNames = new String[numRewardStructs];
		for (i = 0; i < numRewardStructs; i++) {
			rewardStructNames[i] = modulesFile.getRewardStruct(i).getName();
		}
		
		// create new Model object to be returned
		if (modelType == ModelType.DTMC) {
			model = new ProbModel(trans, start, stateRewards, transRewards, rewardStructNames, allDDRowVars, allDDColVars, modelVariables,
						   numModules, moduleNames, moduleDDRowVars, moduleDDColVars,
						   numVars, varList, varDDRowVars, varDDColVars, constantValues);
		}
		else if (modelType == ModelType.MDP) {
			model = new NondetModel(trans, start, stateRewards, transRewards, rewardStructNames, allDDRowVars, allDDColVars,
						     allDDSynchVars, allDDSchedVars, allDDChoiceVars, allDDNondetVars, modelVariables,
						     numModules, moduleNames, moduleDDRowVars, moduleDDColVars,
						     numVars, varList, varDDRowVars, varDDColVars, constantValues);
		}
		else if (modelType == ModelType.CTMC) {
			model = new StochModel(trans, start, stateRewards, transRewards, rewardStructNames, allDDRowVars, allDDColVars, modelVariables,
						    numModules, moduleNames, moduleDDRowVars, moduleDDColVars,
						    numVars, varList, varDDRowVars, varDDColVars, constantValues);
		}
		
		// We also store a copy of the list of action label names
		model.setSynchs((Vector<String>)synchs.clone());
		
		// For MDPs, we also store the DDs used to construct the part
		// of the transition matrix that corresponds to each action
		if (modelType == ModelType.MDP && storeTransParts) {
			((NondetModel)model).setTransInd(transInd);
			((NondetModel)model).setTransSynch(transSynch);
		}
		
		// If required, we also store info about action labels
		if (storeTransActions) {
			// Note: one of these will be null, depending on model type
			// but this is fine: null = none stored.
			model.setTransActions(transActions);
			model.setTransPerAction(transPerAction);
		}
		
		// do reachability (or not)
		if (prism.getDoReach()) {
			mainLog.print("\nComputing reachable states...\n");
			model.doReachability();
			model.filterReachableStates();
		}
		else {
			mainLog.print("\nSkipping reachable state computation.\n");
			model.skipReachability();
			model.filterReachableStates();
		}
		
		// Print some info (if extraddinfo flag on)
		if (prism.getExtraDDInfo()) {
			mainLog.print("Reach: " + JDD.GetNumNodes(model.getReach()) + " nodes\n");
		}
		
		// symmetrification
		if (doSymmetry) doSymmetry(model);
		
		// find/fix any deadlocks
		model.findDeadlocks(prism.getFixDeadlocks());
		
		// deref spare dds
		globalDDRowVars.derefAll();
		globalDDColVars.derefAll();
		JDD.DerefArray(moduleIdentities, numModules);
		JDD.DerefArray(moduleRangeDDs, numModules);
		JDD.DerefArray(varIdentities, numVars);
		JDD.DerefArray(varRangeDDs, numVars);
		JDD.DerefArray(varColRangeDDs, numVars);
		JDD.Deref(range);
		if (modelType == ModelType.MDP) {
			JDD.DerefArray(ddSynchVars, ddSynchVars.length);
			JDD.DerefArray(ddSchedVars, ddSchedVars.length);
			JDD.DerefArray(ddChoiceVars, ddChoiceVars.length);
		}
		if (doSymmetry) {
			JDD.Deref(symm);
			JDD.DerefArray(nonSymms, numSymmModules-1);
		}
		
		expr2mtbdd.clearDummyModel();
		
		return model;
	}
	
	// allocate DD vars for system
	// i.e. decide on variable ordering and request variables from CUDD
			
	private void allocateDDVars()
	{
		int i, j, m, n, last;
		
		modelVariables = new ModelVariablesDD();
		
		switch (prism.getOrdering()) {
		
		case 1:
		// ordering: (a ... a) (s ... s) (l ... l) (r c ... r c)
		
			modelVariables.preallocateExtraActionVariables(prism.getSettings().getInteger(PrismSettings.PRISM_DD_EXTRA_ACTION_VARS));

			// create arrays/etc. first
			
			// nondeterministic variables
			if (modelType == ModelType.MDP) {
				// synchronizing action variables
				ddSynchVars = new JDDNode[numSynchs];
				// sched nondet vars
				ddSchedVars = new JDDNode[numModules];
				// local nondet vars
				// max num needed = total num of commands in all modules + num of modules
				// (actually, might need more for complex parallel compositions? hmmm...)
				m = numModules;
				for (i = 0; i < numModules; i++) {
					m += modulesFile.getModule(i).getNumCommands();
				}
				ddChoiceVars = new JDDNode[m];
			}
			// module variable (row/col) vars
			varDDRowVars = new JDDVars[numVars];
			varDDColVars = new JDDVars[numVars];
			for (i = 0; i < numVars; i++) {
				varDDRowVars[i] = new JDDVars();
				varDDColVars[i] = new JDDVars();
			}
			
			// now allocate variables

			// allocate synchronizing action variables
			if (modelType == ModelType.MDP) {
				// allocate vars
				for (i = 0; i < numSynchs; i++) {
					ddSynchVars[i] = modelVariables.allocateVariable(synchs.elementAt(i)+".a");
				}
			}
		
			// allocate scheduling nondet dd variables
			if (modelType == ModelType.MDP) {
				// allocate vars
				for (i = 0; i < numModules; i++) {
					ddSchedVars[i] = modelVariables.allocateVariable(moduleNames[i] + ".s");
				}
			}
			
			// allocate internal nondet choice dd variables
			if (modelType == ModelType.MDP) {
				m = ddChoiceVars.length;
				for (i = 0; i < m; i++) {
					ddChoiceVars[i] = modelVariables.allocateVariable("l" + i);
				}
			}
			
			// create a gap in the dd variables
			// this allows to prepend additional row/col vars, e.g. for constructing
			// a product model when doing LTL model checking
			modelVariables.preallocateExtraStateVariables(prism.getSettings().getInteger(PrismSettings.PRISM_DD_EXTRA_STATE_VARS));

			
			// allocate dd variables for module variables (i.e. rows/cols)
			// go through all vars in order (incl. global variables)
			// so overall ordering can be specified by ordering in the input file
			for (i = 0; i < numVars; i++) {
				// get number of dd variables needed
				// (ceiling of log2 of range of variable)
				n = varList.getRangeLogTwo(i);
				// add pairs of variables (row/col)
				for (j = 0; j < n; j++) {
					// new dd row variable
					varDDRowVars[i].addVar(modelVariables.allocateVariable(varList.getName(i) + "." + j));
					// new dd col variable
					varDDColVars[i].addVar(modelVariables.allocateVariable(varList.getName(i) + "'." + j));
				}
			}

			break;
			
		case 2:
		// ordering: (a ... a) (l ... l) (s r c ... r c) (s r c ... r c) ...
	
			modelVariables.preallocateExtraActionVariables(prism.getSettings().getInteger(PrismSettings.PRISM_DD_EXTRA_ACTION_VARS));

			// create arrays/etc. first
			
			// nondeterministic variables
			if (modelType == ModelType.MDP) {
				// synchronizing action variables
				ddSynchVars = new JDDNode[numSynchs];
				// sched nondet vars
				ddSchedVars = new JDDNode[numModules];
				// local nondet vars: num = total num of commands in all modules + num of modules
				m = numModules;
				for (i = 0; i < numModules; i++) {
					m += modulesFile.getModule(i).getNumCommands();
				}
				ddChoiceVars = new JDDNode[m];
			}
			// module variable (row/col) vars
			varDDRowVars = new JDDVars[numVars];
			varDDColVars = new JDDVars[numVars];
			for (i = 0; i < numVars; i++) {
				varDDRowVars[i] = new JDDVars();
				varDDColVars[i] = new JDDVars();
			}
			
			// now allocate variables
			
			// allocate synchronizing action variables
			if (modelType == ModelType.MDP) {
				for (i = 0; i < numSynchs; i++) {
					ddSynchVars[i] = modelVariables.allocateVariable(synchs.elementAt(i)+".a");
				}
			}

			// allocate internal nondet choice dd variables
			if (modelType == ModelType.MDP) {
				m = ddChoiceVars.length;
				for (i = 0; i < m; i++) {
					ddChoiceVars[i] = modelVariables.allocateVariable("l" + i);
				}
			}

			// TODO: For the other variable order (-o1, used for sparse/hybrid by default,
			// see above), we preallocate a certain number of state variables.
			// For consistency, it would make sense to do the same here. However,
			// we should first do some testing to see if this negatively impacts
			// performance.

			// go through all vars in order (incl. global variables)
			// so overall ordering can be specified by ordering in the input file
			// use 'last' to detect when starting a new module
			last = -1; // globals are -1
			for (i = 0; i < numVars; i++) {
				// if at the start of a module's variables
				// and model is an mdp...
				if ((modelType == ModelType.MDP) && (last != varList.getModule(i))) {
					// add scheduling dd var(s) (may do multiple ones here if modules have no vars)
					for (j = last+1; j <= varList.getModule(i); j++) {
						ddSchedVars[j] = modelVariables.allocateVariable(moduleNames[j] + ".s");
					}
					// change 'last'
					last = varList.getModule(i);
				}
				// now add row/col dd vars for the variable
				// get number of dd variables needed
				// (ceiling of log2 of range of variable)
				n = varList.getRangeLogTwo(i);
				// add pairs of variables (row/col)
				for (j = 0; j < n; j++) {
					varDDRowVars[i].addVar(modelVariables.allocateVariable(varList.getName(i) + "." + j));
					varDDColVars[i].addVar(modelVariables.allocateVariable(varList.getName(i) + "'." + j));
				}
			}
			// add any remaining scheduling dd var(s) (happens if some modules have no vars)
			if (modelType == ModelType.MDP) for (j = last+1; j <numModules; j++) {
				ddSchedVars[j] = modelVariables.allocateVariable(moduleNames[j] + ".s");
			}
			break;
			
		default:
			mainLog.printWarning("Invalid MTBDD ordering selected - it's all going to go wrong.");
			break;
		}
		
		// print out all mtbdd variables allocated
//		mainLog.print("\nMTBDD variables:");
//		for (i = 0; i < ddVarNames.size(); i++) {
//			mainLog.print(" (" + i + ")" + ddVarNames.elementAt(i));
//		}
//		mainLog.println();
	}

	// sort out DD variables and the arrays they are stored in
	// (more than one copy of most variables is stored)
			
	private void sortDDVars()
	{
		int i, m;
		
		// put refs for all globals and all vars in each module together
		// create arrays
		globalDDRowVars = new JDDVars();
		globalDDColVars = new JDDVars();
		moduleDDRowVars = new JDDVars[numModules];
		moduleDDColVars = new JDDVars[numModules];
		for (i = 0; i < numModules; i++) {
			moduleDDRowVars[i] = new JDDVars();
			moduleDDColVars[i] = new JDDVars();
		}
		// go thru all variables
		for (i = 0; i < numVars; i++) {
			// check which module it belongs to
			m = varList.getModule(i);
			// if global...
			if (m == -1) {
				globalDDRowVars.copyVarsFrom(varDDRowVars[i]);
				globalDDColVars.copyVarsFrom(varDDColVars[i]);
			}
			// otherwise...
			else {
				moduleDDRowVars[m].copyVarsFrom(varDDRowVars[i]);
				moduleDDColVars[m].copyVarsFrom(varDDColVars[i]);
			}
		}
		
		// put refs for all vars in whole system together
		// create arrays
		allDDRowVars = new JDDVars();
		allDDColVars = new JDDVars();
		if (modelType == ModelType.MDP) {
			allDDSynchVars = new JDDVars();
			allDDSchedVars = new JDDVars();
			allDDChoiceVars = new JDDVars();
			allDDNondetVars = new JDDVars();
		}
		// go thru all variables
		for (i = 0; i < numVars; i++) {
			// add to list
			allDDRowVars.copyVarsFrom(varDDRowVars[i]);
			allDDColVars.copyVarsFrom(varDDColVars[i]);
		}
		if (modelType == ModelType.MDP) {
			// go thru all syncronising action vars
			for (i = 0; i < ddSynchVars.length; i++) {
				// add to list
				allDDSynchVars.addVar(ddSynchVars[i].copy());
				allDDNondetVars.addVar(ddSynchVars[i].copy());
			}
			// go thru all scheduler nondet vars
			for (i = 0; i < ddSchedVars.length; i++) {
				// add to list
				allDDSchedVars.addVar(ddSchedVars[i].copy());
				allDDNondetVars.addVar(ddSchedVars[i].copy());
			}
			// go thru all local nondet vars
			for (i = 0; i < ddChoiceVars.length; i++) {
				// add to list
				allDDChoiceVars.addVar(ddChoiceVars[i].copy());
				allDDNondetVars.addVar(ddChoiceVars[i].copy());
			}
		}
	}
	
	// sort DDs for identities
	
	private void sortIdentities()
	{
		int i, j;
		JDDNode id;
		
		// variable identities
		varIdentities = new JDDNode[numVars];
		for (i = 0; i < numVars; i++) {
			// set each element of the identity matrix
			id = JDD.Constant(0);
			for (j = 0; j < varList.getRange(i); j++) {
				id = JDD.SetMatrixElement(id, varDDRowVars[i], varDDColVars[i], j, j, 1);
			}
			varIdentities[i] = id;
		}
		// module identities
		moduleIdentities = new JDDNode[numModules];
		for (i = 0; i < numModules; i++) {
			// product of identities for vars in module
			id = JDD.Constant(1);
			for (j = 0; j < numVars; j++) {
				if (varList.getModule(j) == i) {
					id = JDD.Apply(JDD.TIMES, id, varIdentities[j].copy());
				}
			}
			moduleIdentities[i] = id;
		}
	}

	// Sort DDs for ranges
	
	private void sortRanges()
	{
		int i;
		
		// initialise raneg for whole system
		range = JDD.Constant(1);
		
		// variable ranges		
		varRangeDDs = new JDDNode[numVars];
		varColRangeDDs = new JDDNode[numVars];
		for (i = 0; i < numVars; i++) {
			// obtain range dd by abstracting from identity matrix
			varRangeDDs[i] = JDD.SumAbstract(varIdentities[i].copy(), varDDColVars[i]);
			// obtain range dd by abstracting from identity matrix
			varColRangeDDs[i] = JDD.SumAbstract(varIdentities[i].copy(), varDDRowVars[i]);
			// build up range for whole system as we go
			range = JDD.Apply(JDD.TIMES, range, varRangeDDs[i].copy());
		}
		// module ranges
		moduleRangeDDs = new JDDNode[numModules];
		for (i = 0; i < numModules; i++) {
			// obtain range dd by abstracting from identity matrix
			moduleRangeDDs[i] = JDD.SumAbstract(moduleIdentities[i].copy(), moduleDDColVars[i]);
		}
	}

	// translate modules decription to dds
	
	private void translateModules() throws PrismException
	{
		SystemFullParallel sys;
		JDDNode tmp;
		int i;
		
		varsUsed = new boolean[numVars];
		
		if (modulesFile.getSystemDefn() == null) {
			sys = new SystemFullParallel();
			for (i = 0; i < numModules; i++) {
				sys.addOperand(new SystemModule(moduleNames[i]));
			}
			translateSystemDefn(sys);
		}
		else {
			translateSystemDefn(modulesFile.getSystemDefn());
		}
		
//		if (type == ModulesFile.PROBABILISTIC) {
//			// divide each row by number of modules
//			trans = JDD.Apply(JDD.DIVIDE, trans, JDD.Constant(numModules));
//		}
		
		// for dtmcs, need to normalise each row to remove local nondeterminism
		if (modelType == ModelType.DTMC) {
			// divide each row by row sum
			tmp = JDD.SumAbstract(trans.copy(), allDDColVars);
			trans = JDD.Apply(JDD.DIVIDE, trans, tmp);
		}
	}

	// build system according to composition expression
	
	private void translateSystemDefn(SystemDefn sys) throws PrismException
	{
		SystemDDs sysDDs;
		JDDNode tmp, v;
		int i, j, n, max;
		int[] synchMin;
		
		// initialise some values for synchMin
		// (stores min indices of dd vars to use for local nondet)
		synchMin = new int[numSynchs];
		for (i = 0; i < numSynchs; i++) {
			synchMin[i] = 0;
		}
		
		// build system recursively (descend parse tree)
		sysDDs = translateSystemDefnRec(sys, synchMin);
		
		// for the nondeterministic case, add extra mtbdd variables to encode nondeterminism
		if (modelType == ModelType.MDP) {
			// need to make sure all parts have the same number of dd variables for nondeterminism
			// so we don't generate lots of extra nondeterministic choices
			// first compute max number of variables used
			max = sysDDs.ind.max;
			for (i = 0; i < numSynchs; i++) {
				if (sysDDs.synchs[i].max > max) {
					max = sysDDs.synchs[i].max;
				}
			}
			// check independent bit has this many variables
			if (max > sysDDs.ind.max) {
				tmp = JDD.Constant(1);
				for (i = sysDDs.ind.max; i < max; i++) {
					v = ddChoiceVars[ddChoiceVars.length-i-1];
					JDD.Ref(v);
					tmp = JDD.And(tmp, JDD.Not(v));
				}
				sysDDs.ind.trans = JDD.Apply(JDD.TIMES, sysDDs.ind.trans, tmp);
				//JDD.Ref(tmp);
				//sysDDs.ind.rewards = JDD.Apply(JDD.TIMES, sysDDs.ind.rewards, tmp);
				sysDDs.ind.max = max;
			}
			// check each synchronous bit has this many variables
			for (i = 0; i < numSynchs; i++) {
				if (max > sysDDs.synchs[i].max) {
					tmp = JDD.Constant(1);
					for (j = sysDDs.synchs[i].max; j < max; j++) {
						v = ddChoiceVars[ddChoiceVars.length-j-1];
						JDD.Ref(v);
						tmp = JDD.And(tmp, JDD.Not(v));
					}
					sysDDs.synchs[i].trans = JDD.Apply(JDD.TIMES, sysDDs.synchs[i].trans, tmp);
					//JDD.Ref(tmp);
					//sysDDs.synchs[i].rewards = JDD.Apply(JDD.TIMES, sysDDs.synchs[i].rewards, tmp);
					sysDDs.synchs[i].max = max;
				}
			}
			// now add in new mtbdd variables to distinguish between actions
			// independent bit
			tmp = JDD.Constant(1);
			for (i = 0; i < numSynchs; i++) {
				tmp = JDD.And(tmp, JDD.Not(ddSynchVars[i].copy()));
			}
			sysDDs.ind.trans = JDD.Apply(JDD.TIMES, tmp, sysDDs.ind.trans);
			//JDD.Ref(tmp);
			//transRewards = JDD.Apply(JDD.TIMES, tmp, sysDDs.ind.rewards);
			// synchronous bits
			for (i = 0; i < numSynchs; i++) {
				tmp = JDD.Constant(1);
				for (j = 0; j < numSynchs; j++) {
					if (j == i) {
						tmp = JDD.And(tmp, ddSynchVars[j].copy());
					}
					else {
						tmp = JDD.And(tmp, JDD.Not(ddSynchVars[j].copy()));
					}
				}
				sysDDs.synchs[i].trans = JDD.Apply(JDD.TIMES, tmp, sysDDs.synchs[i].trans);
				//JDD.Ref(tmp);
				//transRewards = JDD.Apply(JDD.PLUS, transRewards, JDD.Apply(JDD.TIMES, tmp, sysDDs.synchs[i].rewards));
			}
		}
		
		// build state and transition rewards
		computeRewards(sysDDs);
		
		// now, for all model types, transition matrix can be built by summing over all actions
		// also build transition rewards at the same time
		n = modulesFile.getNumRewardStructs();
		trans = sysDDs.ind.trans.copy();
		for (j = 0; j < n; j++) {
			transRewards[j] = sysDDs.ind.rewards[j];
		}
		for (i = 0; i < numSynchs; i++) {
			trans = JDD.Apply(JDD.PLUS, trans, sysDDs.synchs[i].trans.copy());
			for (j = 0; j < n; j++) {
				transRewards[j] = JDD.Apply(JDD.PLUS, transRewards[j], sysDDs.synchs[i].rewards[j]);
			}
		}
		// For D/CTMCs, final rewards are scaled by dividing by total prob/rate for each transition
		// (when individual transition rewards are computed, they are multiplied by individual probs/rates).
		// Need to do this (for D/CTMCs) because transition prob/rate can be the sum of values from
		// several different actions; this gives us the "expected" reward for each transition.
		// (Note, for MDPs, nondeterministic choices are always kept separate so this never occurs.)
		if (modelType != ModelType.MDP) {
			n = modulesFile.getNumRewardStructs();
			for (j = 0; j < n; j++) {
				transRewards[j] = JDD.Apply(JDD.DIVIDE, transRewards[j], trans.copy());
			}
		}
		
		// For MDPs, we take a copy of the DDs used to construct the part
		// of the transition matrix that corresponds to each action
		if (modelType == ModelType.MDP && storeTransParts) {
			transInd = JDD.ThereExists(JDD.GreaterThan(sysDDs.ind.trans.copy(), 0), allDDColVars);
			transSynch = new JDDNode[numSynchs];
			for (i = 0; i < numSynchs; i++) {
				transSynch[i] = JDD.ThereExists(JDD.GreaterThan(sysDDs.synchs[i].trans.copy(), 0), allDDColVars);
			}
		}
		
		// If required, we also build MTBDD(s) to store the action labels for each transition.
		// The indexing of actions is as follows:
		// independent ("tau", non-action-labelled) transitions have index 0;
		// action-labelled transitions are 1-indexed using the ordering from the model file,
		// i.e. adding 1 to the list of actions from modulesFile.getSynchs().
		// What is actually stored differs for each model type.
		// For MDPs, we just store the action (index) for each state and nondet choice
		// (as an MTBDD 'transActions' over allDDRowVars and allDDNondetVars, with terminals giving index).  
		// For D/CTMCs, we have store to store a copy of the transition matrix for each action
		// (as numSynchs+1 MTBDDs 'transPerAction' over allDDRowVars/allDDColVars, with terminals giving prob/rate)  
		// because one global transition can come from several different actions.
		if (storeTransActions) {
			// Initialise storage to null so we know what we have used
			transActions = null;
			transPerAction = null;
			switch (modelType) {
			case MDP:
				transActions = JDD.Constant(0);
				// Don't need to store info for independent (action-less) transitions
				// as they are encoded as 0 anyway
				//JDD.Ref(sysDDs.ind.trans);
				//tmp = JDD.ThereExists(JDD.GreaterThan(sysDDs.ind.trans, 0), allDDColVars);
				//transActions = JDD.Apply(JDD.PLUS, transActions, JDD.Apply(JDD.TIMES, tmp, JDD.Constant(1)));
				for (i = 0; i < numSynchs; i++) {
					tmp = JDD.ThereExists(JDD.GreaterThan(sysDDs.synchs[i].trans.copy(), 0), allDDColVars);
					transActions = JDD.Apply(JDD.PLUS, transActions, JDD.Apply(JDD.TIMES, tmp, JDD.Constant(1+i)));
				}
				break;
			case DTMC:
			case CTMC:
				// Just reference DDs and copy them to new array
				transPerAction = new JDDNode[numSynchs + 1];
				transPerAction[0] = sysDDs.ind.trans.copy();
				for (i = 0; i < numSynchs; i++) {
					transPerAction[i + 1] = sysDDs.synchs[i].trans.copy();
				}
				break;
			}
		}
		
		// deref bits of ComponentDD objects - we don't need them any more
		JDD.Deref(sysDDs.ind.guards);
		JDD.Deref(sysDDs.ind.trans);
		for (i = 0; i < numSynchs; i++) {
			JDD.Deref(sysDDs.synchs[i].guards);
			JDD.Deref(sysDDs.synchs[i].trans);
		}
		JDD.Deref(sysDDs.id);
	}

	// recursive part of system composition (descend parse tree)
	
	private SystemDDs translateSystemDefnRec(SystemDefn sys, int[] synchMin) throws PrismException
	{
		SystemDDs sysDDs;
		
		// determine type of current parse tree node
		// and pass to relevant method
		if (sys instanceof SystemModule) {
			sysDDs = translateSystemModule((SystemModule)sys, synchMin);
		}
		else if (sys instanceof SystemBrackets) {
			sysDDs = translateSystemDefnRec(((SystemBrackets)sys).getOperand(), synchMin);
		}
		else if (sys instanceof SystemFullParallel) {
			sysDDs = translateSystemFullParallel((SystemFullParallel)sys, synchMin);
		}
		else if (sys instanceof SystemInterleaved) {
			sysDDs = translateSystemInterleaved((SystemInterleaved)sys, synchMin);
		}
		else if (sys instanceof SystemParallel) {
			sysDDs = translateSystemParallel((SystemParallel)sys, synchMin);
		}
		else if (sys instanceof SystemHide) {
			sysDDs = translateSystemHide((SystemHide)sys, synchMin);
		}
		else if (sys instanceof SystemRename) {
			sysDDs = translateSystemRename((SystemRename)sys, synchMin);
		}
		else if (sys instanceof SystemReference) {
			String name = ((SystemReference) sys).getName();
			SystemDefn sysRef = modulesFile.getSystemDefnByName(name);
			if (sysRef == null)
				throw new PrismLangException("Reference to system " + sys + " which does not exist", sys);
			sysDDs = translateSystemDefnRec(sysRef, synchMin);
		}
		else {
			throw new PrismLangException("Unknown operator in model construction", sys);
		}
		
		return sysDDs;
	}

	// system composition (module)
	
	private SystemDDs translateSystemModule(SystemModule sys, int[] synchMin) throws PrismException
	{
		SystemDDs sysDDs;
		parser.ast.Module module;
		String synch;
		int i, m;
		
		// create object to store result
		sysDDs = new SystemDDs(numSynchs);
		
		// determine which module it is
		m = modulesFile.getModuleIndex(sys.getName());
		module = modulesFile.getModule(m);
		
		// build mtbdd for independent bit
		sysDDs.ind = translateModule(m, module, "", 0);
		// build mtbdd for each synchronising action
		for (i = 0; i < numSynchs; i++) {
			synch = synchs.elementAt(i);
			sysDDs.synchs[i] = translateModule(m, module, synch, synchMin[i]);
		}
		// store identity matrix
		sysDDs.id = moduleIdentities[m].copy();
		
		// store synchs used
		sysDDs.allSynchs.addAll(module.getAllSynchs());
		
		return sysDDs;
	}

	// system composition (full parallel)
	
	private SystemDDs translateSystemFullParallel(SystemFullParallel sys, int[] synchMin) throws PrismException
	{
		SystemDDs sysDDs1, sysDDs2, sysDDs;
		int[] newSynchMin;
		int i, j;
		
		// construct mtbdds for first operand
		sysDDs = translateSystemDefnRec(sys.getOperand(0), synchMin);
		
		// loop through all other operands in the parallel operator
		for (i = 1; i < sys.getNumOperands(); i++) {
		
			// change min to max for potentially synchronising actions
			// store this in new array - old one may still be used elsewhere
			newSynchMin = new int[numSynchs];
			for (j = 0; j < numSynchs; j++) {
				if (sysDDs.allSynchs.contains(synchs.get(j))) {
					newSynchMin[j] = sysDDs.synchs[j].max;
				}
				else {
					newSynchMin[j] = synchMin[j];
				}
			}
			
			// construct mtbdds for next operand
			sysDDs2 = translateSystemDefnRec(sys.getOperand(i), newSynchMin);
			// move sysDDs (operands composed so far) into sysDDs1
			sysDDs1 = sysDDs;
			// we are going to combine sysDDs1 and sysDDs2 and put the result into sysDDs
			sysDDs = new SystemDDs(numSynchs);
			
			// combine mtbdds for independent bit
			sysDDs.ind = translateNonSynchronising(sysDDs1.ind, sysDDs2.ind, sysDDs1.id, sysDDs2.id);
			
			// combine mtbdds for each synchronising action
			for (j = 0; j < numSynchs; j++) {
				// if one operand does not use this action,
				// do asynchronous parallel composition
				if ((sysDDs1.allSynchs.contains(synchs.get(j))?1:0) + (sysDDs2.allSynchs.contains(synchs.get(j))?1:0) == 1) {
					sysDDs.synchs[j] = translateNonSynchronising(sysDDs1.synchs[j], sysDDs2.synchs[j], sysDDs1.id, sysDDs2.id);
				}
				else {
					sysDDs.synchs[j] = translateSynchronising(sysDDs1.synchs[j], sysDDs2.synchs[j]);
				}
			}
			
			// compute identity
			sysDDs.id = JDD.Apply(JDD.TIMES, sysDDs1.id, sysDDs2.id);
			
			// combine lists of synchs
			sysDDs.allSynchs.addAll(sysDDs1.allSynchs);
			sysDDs.allSynchs.addAll(sysDDs2.allSynchs);
		}
		
		return sysDDs;
	}

	// system composition (interleaved)
		
	private SystemDDs translateSystemInterleaved(SystemInterleaved sys, int[] synchMin) throws PrismException
	{
		SystemDDs sysDDs1, sysDDs2, sysDDs;
		int i, j;
	
		// construct mtbdds for first operand
		sysDDs = translateSystemDefnRec(sys.getOperand(0), synchMin);
		
		// loop through all other operands in the parallel operator
		for (i = 1; i < sys.getNumOperands(); i++) {
		
			// construct mtbdds for next operand
			sysDDs2 = translateSystemDefnRec(sys.getOperand(i), synchMin);
			// move sysDDs (operands composed so far) into sysDDs1
			sysDDs1 = sysDDs;
			// we are going to combine sysDDs1 and sysDDs2 and put the result into sysDDs
			sysDDs = new SystemDDs(numSynchs);
			
			// combine mtbdds for independent bit
			sysDDs.ind = translateNonSynchronising(sysDDs1.ind, sysDDs2.ind, sysDDs1.id, sysDDs2.id);
			
			// combine mtbdds for each synchronising action
			for (j = 0; j < numSynchs; j++) {
				sysDDs.synchs[j] = translateNonSynchronising(sysDDs1.synchs[j], sysDDs2.synchs[j], sysDDs1.id, sysDDs2.id);
			}
			
			// compute identity
			sysDDs.id = JDD.Apply(JDD.TIMES, sysDDs1.id, sysDDs2.id);
			
			// combine lists of synchs
			sysDDs.allSynchs.addAll(sysDDs1.allSynchs);
			sysDDs.allSynchs.addAll(sysDDs2.allSynchs);
		}
		
		return sysDDs;
	}

	// system composition (parallel over actions)
	
	private SystemDDs translateSystemParallel(SystemParallel sys, int[] synchMin) throws PrismException
	{
		SystemDDs sysDDs1, sysDDs2, sysDDs;
		boolean[] synchBool;
		int[] newSynchMin;
		int i;
		
		// go thru all synchronising actions and decide if we will synchronise on each one
		synchBool = new boolean[numSynchs];
		for (i = 0; i < numSynchs; i++) {
			synchBool[i] = sys.containsAction(synchs.elementAt(i));
		}
		
		// construct mtbdds for first operand
		sysDDs1 = translateSystemDefnRec(sys.getOperand1(), synchMin);
		
		// change min to max for synchronising actions
		// store this in new array - old one may still be used elsewhere
		newSynchMin = new int[numSynchs];
		for (i = 0; i < numSynchs; i++) {
			if (synchBool[i]) {
				newSynchMin[i] = sysDDs1.synchs[i].max;
			}
			else {
				newSynchMin[i] = synchMin[i];
			}
		}
		
		// construct mtbdds for second operand
		sysDDs2 = translateSystemDefnRec(sys.getOperand2(), newSynchMin);
		
		// create object to store mtbdds
		sysDDs = new SystemDDs(numSynchs);
		
		// combine mtbdds for independent bit
		sysDDs.ind = translateNonSynchronising(sysDDs1.ind, sysDDs2.ind, sysDDs1.id, sysDDs2.id);
		
		// combine mtbdds for each synchronising action
		for (i = 0; i < numSynchs; i++) {
			if (synchBool[i]) {
				sysDDs.synchs[i] = translateSynchronising(sysDDs1.synchs[i], sysDDs2.synchs[i]);
			}
			else {
				sysDDs.synchs[i] = translateNonSynchronising(sysDDs1.synchs[i], sysDDs2.synchs[i], sysDDs1.id, sysDDs2.id);
			}
		}
		
		// combine mtbdds for identity matrices
		sysDDs.id = JDD.Apply(JDD.TIMES, sysDDs1.id, sysDDs2.id);
		
		// combine lists of synchs
		sysDDs.allSynchs.addAll(sysDDs1.allSynchs);
		sysDDs.allSynchs.addAll(sysDDs2.allSynchs);
		
		return sysDDs;
	}
	
	// system composition (hide)
	
	private SystemDDs translateSystemHide(SystemHide sys, int[] synchMin) throws PrismException
	{
		SystemDDs sysDDs1, sysDDs;
		int[] newSynchMin;
		int i;
		
		// reset synchMin to 0 for actions to be hidden
		// store this in new array - old one may still be used elsewhere
		newSynchMin = new int[numSynchs];
		for (i = 0; i < numSynchs; i++) {
			if (sys.containsAction(synchs.elementAt(i))) {
				newSynchMin[i] = 0;
			}
			else {
				newSynchMin[i] = synchMin[i];
			}
		}
		
		// construct mtbdds for operand
		sysDDs1 = translateSystemDefnRec(sys.getOperand(), newSynchMin);
		
		// create object to store mtbdds for result
		sysDDs = new SystemDDs(numSynchs);
		
		// copy across independent bit
		sysDDs.ind = sysDDs1.ind;
		
		// go thru all synchronising actions
		for (i = 0; i < numSynchs; i++) {
			
			// if the action is in the set to be hidden, hide it...
			// note that it doesn't matter if an action is included more than once in the set
			// (although this would be picked up during the syntax check anyway)
			if (sys.containsAction(synchs.elementAt(i))) {
				
				// move these transitions into the independent bit
				sysDDs.ind = combineComponentDDs(sysDDs.ind, sysDDs1.synchs[i]);
				
				// create empty mtbdd for action
				sysDDs.synchs[i] = new ComponentDDs();
				sysDDs.synchs[i].guards = JDD.Constant(0);
				sysDDs.synchs[i].trans = JDD.Constant(0);
				//sysDDs.synchs[i].rewards = JDD.Constant(0);
				sysDDs.synchs[i].min = 0;
				sysDDs.synchs[i].max = 0;
			}
			// otherwise just copy it across
			else {
				sysDDs.synchs[i] = sysDDs1.synchs[i];
			}
		}
		
		// copy identity too
		sysDDs.id = sysDDs1.id;
		
		// modify list of synchs
		sysDDs.allSynchs.addAll(sysDDs1.allSynchs);
		for (i = 0; i < sys.getNumActions(); i++) {
			sysDDs.allSynchs.remove(sys.getAction(i));
		}
		
		return sysDDs;
	}

	// system composition (rename)
	
	private SystemDDs translateSystemRename(SystemRename sys, int[] synchMin) throws PrismException
	{
		SystemDDs sysDDs1, sysDDs;
		int[] newSynchMin;
		int i, j;
		String s;
		Iterator<String> iter;
		
		// swap some values in synchMin due to renaming
		// store this in new array - old one may still be used elsewhere
		newSynchMin = new int[numSynchs];
		for (i = 0; i < numSynchs; i++) {
			// find out what this action is renamed to
			// (this may be itself, i.e. it's not renamed)
			s = sys.getNewName(synchs.elementAt(i));
			j = synchs.indexOf(s);
			if (j == -1) {
				throw new PrismLangException("Invalid action name \"" + s + "\" in renaming", sys);
			}
			newSynchMin[i] = synchMin[j];
		}
		
		// construct mtbdds for operand
		sysDDs1 = translateSystemDefnRec(sys.getOperand(), newSynchMin);
		
		// create object to store mtbdds for result
		sysDDs = new SystemDDs(numSynchs);
		
		// copy across independent bit
		sysDDs.ind = sysDDs1.ind;
		
		// initially there are no mtbdds in result
		for (i = 0; i < numSynchs; i++) {
			sysDDs.synchs[i] = new ComponentDDs();
			sysDDs.synchs[i].guards = JDD.Constant(0);
			sysDDs.synchs[i].trans = JDD.Constant(0);
			//sysDDs.synchs[i].rewards = JDD.Constant(0);
			sysDDs.synchs[i].min = 0;
			sysDDs.synchs[i].max = 0;
		}
		
		// go thru all synchronising actions
		for (i = 0; i < numSynchs; i++) {
		
			// find out what this action is renamed to
			// (this may be itself, i.e. it's not renamed)
			// then add it to result
			s = sys.getNewName(synchs.elementAt(i));
			j = synchs.indexOf(s);
			if (j == -1) {
				throw new PrismLangException("Invalid action name \"" + s + "\" in renaming", sys);
			}
			sysDDs.synchs[j] = combineComponentDDs(sysDDs.synchs[j], sysDDs1.synchs[i]);
		}
		
		// copy identity too
		sysDDs.id = sysDDs1.id;
		
		// modify list of synchs
		iter = sysDDs1.allSynchs.iterator();
		while (iter.hasNext()) {
			sysDDs.allSynchs.add(sys.getNewName(iter.next()));
		}
		
		return sysDDs;
	}

	private ComponentDDs translateSynchronising(ComponentDDs compDDs1, ComponentDDs compDDs2) throws PrismException
	{
		ComponentDDs compDDs;
		
		// create object to store result
		compDDs = new ComponentDDs();
		
		// combine parts synchronously
		// first guards
		JDD.Ref(compDDs1.guards);
		JDD.Ref(compDDs2.guards);
		compDDs.guards = JDD.And(compDDs1.guards, compDDs2.guards);
		// then transitions
		JDD.Ref(compDDs1.trans);
		JDD.Ref(compDDs2.trans);
		compDDs.trans = JDD.Apply(JDD.TIMES, compDDs1.trans, compDDs2.trans);
		// then transition rewards
		//JDD.Ref(compDDs2.trans);
		//compDDs1.rewards = JDD.Apply(JDD.TIMES, compDDs1.rewards, JDD.GreaterThan(compDDs2.trans, 0));
		//JDD.Ref(compDDs1.trans);
		//compDDs2.rewards = JDD.Apply(JDD.TIMES, compDDs2.rewards, JDD.GreaterThan(compDDs1.trans, 0));
		//JDD.Ref(compDDs1.rewards);
		//JDD.Ref(compDDs2.rewards);
		//compDDs.rewards = JDD.Apply(JDD.PLUS, compDDs1.rewards, compDDs2.rewards);
		// compute new min/max
		compDDs.min = (compDDs1.min < compDDs2.min) ? compDDs1.min : compDDs2.min;
		compDDs.max = (compDDs1.max > compDDs2.max) ? compDDs1.max : compDDs2.max;
		
		// deref old stuff
		JDD.Deref(compDDs1.guards);
		JDD.Deref(compDDs2.guards);
		JDD.Deref(compDDs1.trans);
		JDD.Deref(compDDs2.trans);
		//JDD.Deref(compDDs1.rewards);
		//JDD.Deref(compDDs2.rewards);
		
		return compDDs;
	}

	private ComponentDDs translateNonSynchronising(ComponentDDs compDDs1, ComponentDDs compDDs2, JDDNode id1, JDDNode id2) throws PrismException
	{
		ComponentDDs compDDs;
		
		// add identities to mtbdds for transitions
		JDD.Ref(id2);
		compDDs1.trans = JDD.Apply(JDD.TIMES, compDDs1.trans, id2);
		JDD.Ref(id1);
		compDDs2.trans = JDD.Apply(JDD.TIMES, compDDs2.trans, id1);
		// add identities to mtbdds for transition rewards
		//JDD.Ref(id2);
		//compDDs1.rewards = JDD.Apply(JDD.TIMES, compDDs1.rewards, id2);
		//JDD.Ref(id1);
		//compDDs2.rewards = JDD.Apply(JDD.TIMES, compDDs2.rewards, id1);
		
		// then combine...
		compDDs = combineComponentDDs(compDDs1, compDDs2);
		
		return compDDs;
	}

	private ComponentDDs combineComponentDDs(ComponentDDs compDDs1, ComponentDDs compDDs2) throws PrismException
	{
		ComponentDDs compDDs;
		JDDNode tmp, v;
		int i;
		
		// create object to store result
		compDDs = new ComponentDDs();
		
		// if no nondeterminism - just add
		if (modelType != ModelType.MDP) {
			compDDs.guards = JDD.Or(compDDs1.guards, compDDs2.guards);
			compDDs.trans = JDD.Apply(JDD.PLUS, compDDs1.trans, compDDs2.trans);
			//compDDs.rewards = JDD.Apply(JDD.PLUS, compDDs1.rewards, compDDs2.rewards);
			compDDs.min = 0;
			compDDs.max = 0;
		}
		// if there's nondeterminism, but one part is empty, it's also easy
		else if (compDDs1.trans.equals(JDD.ZERO)) {
			JDD.Deref(compDDs1.guards);
			compDDs.guards = compDDs2.guards;
			JDD.Deref(compDDs1.trans);
			compDDs.trans = compDDs2.trans;
			//JDD.Deref(compDDs1.rewards);
			//compDDs.rewards = compDDs2.rewards;
			compDDs.min = compDDs2.min;
			compDDs.max = compDDs2.max;
		}
		else if (compDDs2.trans.equals(JDD.ZERO)) {
			JDD.Deref(compDDs2.guards);
			compDDs.guards = compDDs1.guards;
			JDD.Deref(compDDs2.trans);
			compDDs.trans = compDDs1.trans;
			//JDD.Deref(compDDs2.rewards);
			//compDDs.rewards = compDDs1.rewards;
			compDDs.min = compDDs1.min;
			compDDs.max = compDDs1.max;
		}
		// otherwise, it's a bit more complicated...
		else {
			// make sure two bits have the same number of dd variables for nondeterminism
			// (so we don't generate lots of extra nondeterministic choices)
			if (compDDs1.max > compDDs2.max) {
				tmp = JDD.Constant(1);
				for (i = compDDs2.max; i < compDDs1.max; i++) {
					v = ddChoiceVars[ddChoiceVars.length-i-1];
					JDD.Ref(v);
					tmp = JDD.And(tmp, JDD.Not(v));
				}
				compDDs2.trans = JDD.Apply(JDD.TIMES, compDDs2.trans, tmp);
				//JDD.Ref(tmp);
				//compDDs2.rewards = JDD.Apply(JDD.TIMES, compDDs2.rewards, tmp);
				compDDs2.max = compDDs1.max;
			}
			else if (compDDs2.max > compDDs1.max) {
				tmp = JDD.Constant(1);
				for (i = compDDs1.max; i < compDDs2.max; i++) {
					v = ddChoiceVars[ddChoiceVars.length-i-1];
					JDD.Ref(v);
					tmp = JDD.And(tmp, JDD.Not(v));
				}
				compDDs1.trans = JDD.Apply(JDD.TIMES, compDDs1.trans, tmp);
				//JDD.Ref(tmp);
				//compDDs1.rewards = JDD.Apply(JDD.TIMES, compDDs1.rewards, tmp);
				compDDs1.max = compDDs2.max;
			}
			// and then combine
			if (ddChoiceVars.length-compDDs1.max-1 < 0)
				throw new PrismException("Insufficient BDD variables allocated for nondeterminism - please report this as a bug. Thank you");
			v = ddChoiceVars[ddChoiceVars.length-compDDs1.max-1];
			compDDs.guards = JDD.Or(compDDs1.guards, compDDs2.guards);
			JDD.Ref(v);
			compDDs.trans = JDD.ITE(v, compDDs2.trans, compDDs1.trans);
			//JDD.Ref(v);
			//compDDs.rewards = JDD.ITE(v, compDDs2.rewards, compDDs1.rewards);
			compDDs.min = compDDs1.min;
			compDDs.max = compDDs1.max+1;
		}
		
		return compDDs;
	}
	
	// translate a single module to a dd
	// for a given synchronizing action ("" = none)
	
	private ComponentDDs translateModule(int m, parser.ast.Module module, String synch, int synchMin) throws PrismException
	{
		ComponentDDs compDDs;
		JDDNode guardDDs[], upDDs[], tmp;
		Command command;
		int l, numCommands;
		double dmin = 0, dmax = 0;
		boolean match;
		
		// get number of commands and set up arrays accordingly
		numCommands = module.getNumCommands();
		guardDDs = new JDDNode[numCommands];
		upDDs = new JDDNode[numCommands];
		//rewDDs = new JDDNode[numCommands];
		
		// translate guard/updates for each command of the module
		for (l = 0; l < numCommands; l++) {
			command = module.getCommand(l);
			// check if command matches requested synch
			match = false;
			if (synch == "") {
				if (command.getSynch() == "") match = true;
			}
			else {
				if (command.getSynch().equals(synch)) match = true;
			}
			// if so translate
			if (match) {
				// translate guard
				guardDDs[l] = translateExpression(command.getGuard());
				JDD.Ref(range);
				guardDDs[l] = JDD.Apply(JDD.TIMES, guardDDs[l], range);
				// check for false guard
				if (guardDDs[l].equals(JDD.ZERO)) {
					// display a warning (unless guard is "false", in which case was probably intentional
					if (!Expression.isFalse(command.getGuard())) {
						String s = "Guard for command " + (l+1) + " of module \"" + module.getName() + "\" is never satisfied.";
						mainLog.printWarning(s);
					}
					// no point bothering to compute the mtbdds for the update
					// if the guard is never satisfied
					upDDs[l] = JDD.Constant(0);
					//rewDDs[l] = JDD.Constant(0);
				}
				else {
					// translate updates and do some checks on probs/rates
					upDDs[l] = translateUpdates(m, l, command.getUpdates(), (command.getSynch()=="")?false:true, guardDDs[l]);
					JDD.Ref(guardDDs[l]);
					upDDs[l] = JDD.Apply(JDD.TIMES, upDDs[l], guardDDs[l]);
					// are all probs/rates non-negative?
					dmin = JDD.FindMin(upDDs[l]);
					if (dmin < 0) {
						String s = (modelType == ModelType.CTMC) ? "Rates" : "Probabilities";
						s += " in command " + (l+1) + " of module \"" + module.getName() + "\" are negative";
						s += " (" + dmin + ") for some states.\n";
						s += "Perhaps the guard needs to be strengthened";
						throw new PrismLangException(s, command);
					}
					// only do remaining checks if 'doprobchecks' flag is set
					if (prism.getDoProbChecks()) {
						// sum probs/rates in updates
						JDD.Ref(upDDs[l]);
						tmp = JDD.SumAbstract(upDDs[l], moduleDDColVars[m]);
						tmp = JDD.SumAbstract(tmp, globalDDColVars);
						// put 1s in for sums which are not covered by this guard
						JDD.Ref(guardDDs[l]);
						tmp = JDD.ITE(guardDDs[l], tmp, JDD.Constant(1));
						// compute min/max sums
						dmin = JDD.FindMin(tmp);
						dmax = JDD.FindMax(tmp);
						// check sums for NaNs (note how to check if x=NaN i.e. x!=x)
						if (dmin != dmin || dmax != dmax) {
							JDD.Deref(tmp);
							String s = (modelType == ModelType.CTMC) ? "Rates" : "Probabilities";
							s += " in command " + (l+1) + " of module \"" + module.getName() + "\" have errors (NaN) for some states. ";
							s += "Check for zeros in divide or modulo operations. ";
							s += "Perhaps the guard needs to be strengthened";
							throw new PrismLangException(s, command);
						}
						// check min sums - 1 (ish) for dtmcs/mdps, 0 for ctmcs
						if (modelType != ModelType.CTMC && dmin < 1-prism.getSumRoundOff()) {
							JDD.Deref(tmp);
							String s = "Probabilities in command " + (l+1) + " of module \"" + module.getName() + "\" sum to less than one";
							s += " (e.g. " + dmin + ") for some states. ";
							s += "Perhaps some of the updates give out-of-range values. ";
							s += "One possible solution is to strengthen the guard";
							throw new PrismLangException(s, command);
						}
						if (modelType == ModelType.CTMC && dmin <= 0) {
							JDD.Deref(tmp);
							// note can't sum to less than zero - already checked for negative rates above
							String s = "Rates in command " + (l+1) + " of module \"" + module.getName() + "\" sum to zero for some states. ";
							s += "Perhaps some of the updates give out-of-range values. ";
							s += "One possible solution is to strengthen the guard";
							throw new PrismLangException(s, command);
						}
						// check max sums - 1 (ish) for dtmcs/mdps, infinity for ctmcs
						if (modelType != ModelType.CTMC && dmax > 1+prism.getSumRoundOff()) {
							JDD.Deref(tmp);
							String s = "Probabilities in command " + (l+1) + " of module \"" + module.getName() + "\" sum to more than one";
							s += " (e.g. " + dmax + ") for some states. ";
							s += "Perhaps the guard needs to be strengthened";
							throw new PrismLangException(s, command);
						}
						if (modelType == ModelType.CTMC && dmax == Double.POSITIVE_INFINITY) {
							JDD.Deref(tmp);
							String s = "Rates in command " + (l+1) + " of module \"" + module.getName() + "\" sum to infinity for some states. ";
							s += "Perhaps the guard needs to be strengthened";
							throw new PrismLangException(s, command);
						}
						JDD.Deref(tmp);
					}
					// translate reward, if present
					// if (command.getReward() != null) {
					// 	tmp = translateExpression(command.getReward());
					// 	JDD.Ref(upDDs[l]);
					// 	rewDDs[l] = JDD.Apply(JDD.TIMES, tmp, JDD.GreaterThan(upDDs[l], 0));
					// 	// are all rewards non-negative?
					// if ((d = JDD.FindMin(rewDDs[l])) < 0) {
					// 	String s = "Rewards in command " + (l+1) + " of module \"" + module.getName() + "\" are negative";
					// 	s += " (" + d + ") for some states. ";
					// 	s += "Perhaps the guard needs to be strengthened";
					// 	throw new PrismException(s);
					// }
					// } else {
					// 	rewDDs[l] = JDD.Constant(0);
					// }
				}
			}
			// otherwise use 0
			else {
				guardDDs[l] = JDD.Constant(0);
				upDDs[l] = JDD.Constant(0);
				//rewDDs[l] = JDD.Constant(0);
			}
		}
		
		// combine guard/updates dds for each command
		if (modelType == ModelType.DTMC) {
			compDDs = combineCommandsProb(m, numCommands, guardDDs, upDDs);
		}
		else if (modelType == ModelType.MDP) {
			compDDs = combineCommandsNondet(m, numCommands, guardDDs, upDDs, synchMin);
		}
		else if (modelType == ModelType.CTMC) {
			compDDs = combineCommandsStoch(m, numCommands, guardDDs, upDDs);
		}
		else {
			 throw new PrismException("Unknown model type");
		}
		
		// deref guards/updates
		JDD.DerefArray(guardDDs, numCommands);
		JDD.DerefArray(upDDs, numCommands);
		//JDD.DerefArray(rewDDs, numCommands);
		
		return compDDs;
	}
	
	// go thru guard/updates dds for all commands of a prob. module and combine
	// also check for any guard overlaps, etc...
	
	private ComponentDDs combineCommandsProb(int m, int numCommands, JDDNode guardDDs[], JDDNode upDDs[])
	{
		ComponentDDs compDDs;
		int i;
		JDDNode covered, transDD, tmp;
		//JDDNode rewardsDD;
		
		// create object to return result
		compDDs = new ComponentDDs();
		
		// use 'transDD' to build up MTBDD for transitions
		transDD = JDD.Constant(0);
		// use 'transDD' to build up MTBDD for rewards
		//rewardsDD = JDD.Constant(0);
		// use 'covered' to track states covered by guards
		covered = JDD.Constant(0);
		// loop thru commands...
		for (i = 0; i < numCommands; i++) {
			// do nothing if guard is empty
			if (guardDDs[i].equals(JDD.ZERO)) {
				continue;
			}
			// check if command overlaps with previous ones
			JDD.Ref(guardDDs[i]);
			JDD.Ref(covered);
			tmp = JDD.And(guardDDs[i], covered);
			if (!(tmp.equals(JDD.ZERO))) {
				// if so, output a warning (but carry on regardless)
				mainLog.printWarning("Guard for command " + (i+1) + " of module \""
					+ moduleNames[m] + "\" overlaps with previous commands.");
			}
			JDD.Deref(tmp);
			// add this command's guard to 'covered'
			JDD.Ref(guardDDs[i]);
			covered = JDD.Or(covered, guardDDs[i]);
			// add transitions
			JDD.Ref(guardDDs[i]);
			JDD.Ref(upDDs[i]);
			transDD = JDD.Apply(JDD.PLUS, transDD, JDD.Apply(JDD.TIMES, guardDDs[i], upDDs[i]));
			// add rewards
			//JDD.Ref(guardDDs[i]);
			//JDD.Ref(rewDDs[i]);
			//rewardsDD = JDD.Apply(JDD.PLUS, rewardsDD, JDD.Apply(JDD.TIMES, guardDDs[i], rewDDs[i]));
		}
		
		// store result
		compDDs.guards = covered;
		compDDs.trans = transDD;
		//compDDs.rewards = rewardsDD;
		compDDs.min = 0;
		compDDs.max = 0;
		
		return compDDs;
	}

	// go thru guard/updates dds for all commands of a stoch. module and combine
	
	private ComponentDDs combineCommandsStoch(int m, int numCommands, JDDNode guardDDs[], JDDNode upDDs[])
	{
		ComponentDDs compDDs;
		int i;
		JDDNode covered, transDD;
		//JDDNode rewardsDD;
		
		// create object to return result
		compDDs = new ComponentDDs();
		
		// use 'transDD 'to build up MTBDD for transitions
		transDD = JDD.Constant(0);
		// use 'transDD 'to build up MTBDD for rewards
		//rewardsDD = JDD.Constant(0);
		// use 'covered' to track states covered by guards
		covered = JDD.Constant(0);
		
		// loop thru commands...
		for (i = 0; i < numCommands; i++) {
			// do nothing if guard is empty
			if (guardDDs[i].equals(JDD.ZERO)) {
				continue;
			}
			// add this command's guard to 'covered'
			JDD.Ref(guardDDs[i]);
			covered = JDD.Or(covered, guardDDs[i]);
			// add transitions
			JDD.Ref(guardDDs[i]);
			JDD.Ref(upDDs[i]);
			transDD = JDD.Apply(JDD.PLUS, transDD, JDD.Apply(JDD.TIMES, guardDDs[i], upDDs[i]));
			// add rewards
			//JDD.Ref(guardDDs[i]);
			//JDD.Ref(rewDDs[i]);
			//rewardsDD = JDD.Apply(JDD.PLUS, rewardsDD, JDD.Apply(JDD.TIMES, guardDDs[i], rewDDs[i]));
		}
		
		// store result
		compDDs.guards = covered;
		compDDs.trans = transDD;
		//compDDs.rewards = rewardsDD;
		compDDs.min = 0;
		compDDs.max = 0;
		
		return compDDs;
	}

	// go thru guard/updates dds for all commands of a non-det. module,
	// work out guard overlaps and sort out non determinism accordingly
	// (non recursive version)
	
	private ComponentDDs combineCommandsNondet(int m, int numCommands, JDDNode guardDDs[], JDDNode upDDs[], int synchMin) throws PrismException
	{
		ComponentDDs compDDs;
		int i, j, k, maxChoices, numDDChoiceVarsUsed;
		JDDNode covered, transDD, overlaps, equalsi, tmp, tmp2, tmp3;
		//JDDNode rewardsDD;
		JDDNode[] transDDbits, frees;
		//JDDNode[] rewardsDDbits;
		JDDVars ddChoiceVarsUsed;
		
		// create object to return result
		compDDs = new ComponentDDs();
		
		// use 'transDD' to build up MTBDD for transitions
		transDD = JDD.Constant(0);
		// use 'transDD' to build up MTBDD for rewards
		//rewardsDD = JDD.Constant(0);
		// use 'covered' to track states covered by guards
		covered = JDD.Constant(0);
		
		// find overlaps in guards by adding them all up
		overlaps = JDD.Constant(0);
		for (i = 0; i < numCommands; i++) {
			JDD.Ref(guardDDs[i]);
			overlaps = JDD.Apply(JDD.PLUS, overlaps, guardDDs[i]);
			// compute bdd of all guards at same time
			JDD.Ref(guardDDs[i]);
			covered = JDD.Or(covered, guardDDs[i]);
		}
		
		// find the max number of overlaps
		// (i.e. max number of nondet. choices)
		maxChoices = (int)Math.round(JDD.FindMax(overlaps));
		
		// if all the guards were false, we're done already
		if (maxChoices == 0) {
			compDDs.guards = covered;
			compDDs.trans = transDD;
			//compDDs.rewards = rewardsDD;
			compDDs.min = synchMin;
			compDDs.max = synchMin;
			JDD.Deref(overlaps);
			return compDDs;
		}
		
		// likewise, if there are no overlaps, it's also pretty easy
		if (maxChoices == 1) {
			// add up dds for all commands
			for (i = 0; i < numCommands; i++) {
				// add up transitions
				JDD.Ref(guardDDs[i]);
				JDD.Ref(upDDs[i]);
				transDD = JDD.Apply(JDD.PLUS, transDD, JDD.Apply(JDD.TIMES, guardDDs[i], upDDs[i]));
				// add up rewards
				//JDD.Ref(guardDDs[i]);
				//JDD.Ref(rewDDs[i]);
				//rewardsDD = JDD.Apply(JDD.PLUS, rewardsDD, JDD.Apply(JDD.TIMES, guardDDs[i], rewDDs[i]));
			}
			compDDs.guards = covered;
			compDDs.trans = transDD;
			//compDDs.rewards = rewardsDD;
			compDDs.min = synchMin;
			compDDs.max = synchMin;
			JDD.Deref(overlaps);	
			return compDDs;
		}
		
		// otherwise, it's a bit more complicated...
		
		// first, calculate how many dd vars will be needed
		numDDChoiceVarsUsed = (int)Math.ceil(PrismUtils.log2(maxChoices));
		
		// select the variables we will use and put them in a JDDVars
		ddChoiceVarsUsed = new JDDVars();
		for (i = 0; i < numDDChoiceVarsUsed; i++) {
			if (ddChoiceVars.length-synchMin-numDDChoiceVarsUsed+i < 0)
				throw new PrismException("Insufficient BDD variables allocated for nondeterminism - please report this as a bug. Thank you.");
			ddChoiceVarsUsed.addVar(ddChoiceVars[ddChoiceVars.length-synchMin-numDDChoiceVarsUsed+i]);
		}
		
		// for each i (i = 1 ... max number of nondet. choices)
		for (i = 1; i <= maxChoices; i++) {
			
			// find sections of state space
			// which have exactly i nondet. choices in this module
			JDD.Ref(overlaps);
			equalsi = JDD.Equals(overlaps, (double)i);
			// if there aren't any for this i, skip the iteration
			if (equalsi.equals(JDD.ZERO)) {
				JDD.Deref(equalsi);
				continue;
			}
			
			// create arrays of size i to store dds
			transDDbits = new JDDNode[i];
			//rewardsDDbits = new JDDNode[i];
			frees = new JDDNode[i];
			for (j = 0; j < i; j++) {
				transDDbits[j] = JDD.Constant(0);
				//rewardsDDbits[j] = JDD.Constant(0);
				frees[j] = equalsi;
				JDD.Ref(equalsi);
			}
			
			// go thru each command of the module...
			for (j = 0; j < numCommands; j++) {
				
				// see if this command's guard overlaps with 'equalsi'
				JDD.Ref(guardDDs[j]);
				JDD.Ref(equalsi);
				tmp = JDD.And(guardDDs[j], equalsi);
				// if it does...
				if (!tmp.equals(JDD.ZERO)) {
					
					// split it up into nondet. choices as necessary
					
					JDD.Ref(tmp);
					tmp2 = tmp;
					
					// for each possible nondet. choice (1...i) involved...
					for (k = 0; k < i; k ++) {
						// see how much of the command can go in nondet. choice k
						JDD.Ref(tmp2);
						JDD.Ref(frees[k]);
						tmp3 = JDD.And(tmp2, frees[k]);
						// if some will fit in...
						if (!tmp3.equals(JDD.ZERO)) {
							JDD.Ref(tmp3);
							frees[k] = JDD.And(frees[k], JDD.Not(tmp3));
							JDD.Ref(tmp3);
							JDD.Ref(upDDs[j]);
							transDDbits[k] = JDD.Apply(JDD.PLUS, transDDbits[k], JDD.Apply(JDD.TIMES, tmp3, upDDs[j]));
							//JDD.Ref(tmp3);
							//JDD.Ref(rewDDs[j]);
							//rewardsDDbits[k] = JDD.Apply(JDD.PLUS, rewardsDDbits[k], JDD.Apply(JDD.TIMES, tmp3, rewDDs[j]));
						}
						// take out the bit just put in this choice
						tmp2 = JDD.And(tmp2, JDD.Not(tmp3));
						if (tmp2.equals(JDD.ZERO)) {
							break;
						}
					}
					JDD.Deref(tmp2);
				}
				JDD.Deref(tmp);
			}
			
			// now add the nondet. choices for this value of i
			for (j = 0; j < i; j++) {
				tmp = JDD.SetVectorElement(JDD.Constant(0), ddChoiceVarsUsed, j, 1);
				transDD = JDD.Apply(JDD.PLUS, transDD, JDD.Apply(JDD.TIMES, tmp, transDDbits[j]));
				//JDD.Ref(tmp);
				//rewardsDD = JDD.Apply(JDD.PLUS, rewardsDD, JDD.Apply(JDD.TIMES, tmp, rewardsDDbits[j]));
				JDD.Deref(frees[j]);
			}
			
			// take the i bits out of 'overlaps'
			overlaps = JDD.Apply(JDD.TIMES, overlaps, JDD.Not(equalsi));
		}
		JDD.Deref(overlaps);
		
		// store result
		compDDs.guards = covered;
		compDDs.trans = transDD;
		//compDDs.rewards = rewardsDD;
		compDDs.min = synchMin;
		compDDs.max = synchMin + numDDChoiceVarsUsed;
		
		return compDDs;
	}

	// translate the updates part of a command

	private JDDNode translateUpdates(int m, int l, Updates u, boolean synch, JDDNode guard) throws PrismException
	{
		int i, n;
		Expression p;
		JDDNode dd, udd, pdd;
		boolean warned;
		String msg;
		
		// sum up over possible updates
		dd = JDD.Constant(0);
		n = u.getNumUpdates();
		for (i = 0; i < n; i++) {
			// translate a single update
			udd = translateUpdate(m, u.getUpdate(i), synch, guard);
			// check for zero update
			warned = false;
			if (udd.equals(JDD.ZERO)) {
				warned = true;
				// Use a PrismLangException to get line numbers displayed
				msg = "Update " + (i+1) + " of command " + (l+1);
				msg += " of module \"" + moduleNames[m] + "\" doesn't do anything";
				mainLog.printWarning(new PrismLangException(msg, u.getUpdate(i)).getMessage());
			}
			// multiply by probability/rate
			p = u.getProbability(i);
			if (p == null) p = Expression.Double(1.0);
			pdd = translateExpression(p);
			udd = JDD.Apply(JDD.TIMES, udd, pdd);
			// check (again) for zero update
			if (!warned && udd.equals(JDD.ZERO)) {
				// Use a PrismLangException to get line numbers displayed
				msg = "Update " + (i+1) + " of command " + (l+1);
				msg += " of module \"" + moduleNames[m] + "\" doesn't do anything";
				mainLog.printWarning(new PrismLangException(msg, u.getUpdate(i)).getMessage());
			}
			dd = JDD.Apply(JDD.PLUS, dd, udd);
		}
		
		return dd;
	}

	// translate an update
	
	private JDDNode translateUpdate(int m, Update c, boolean synch, JDDNode guard) throws PrismException
	{
		int i, j, n, v, l, h;
		String s;
		JDDNode dd, tmp1, tmp2, cl;
		
		// clear varsUsed flag array to indicate no vars used yet
		for (i = 0; i < numVars; i++) {	
			varsUsed[i] = false;
		}
		// take product of clauses
		dd = JDD.Constant(1);
		n = c.getNumElements();
		for (i = 0; i < n; i++) {
		
			// get variable
			s = c.getVar(i);
			v = varList.getIndex(s);
			if (v == -1) {
				throw new PrismLangException("Unknown variable \"" + s + "\" in update", c.getVarIdent(i));
			}
			varsUsed[v] = true;
			// check if the variable to be modified is valid
			// (i.e. belongs to this module or is global)
			if (varList.getModule(v) != -1 && varList.getModule(v) != m) {
				throw new PrismLangException("Cannot modify variable \""+s+"\" from module \""+moduleNames[m]+"\"", c.getVarIdent(i));
			}
			// print out a warning if this update is in a command with a synchronising
			// action AND it modifies a global variable
			if (varList.getModule(v) == -1 && synch) {
				throw new PrismLangException("Synchronous command cannot modify global variable", c.getVarIdent(i));
			}
			// get some info on the variable
			l = varList.getLow(v);
			h = varList.getHigh(v);
			// create dd
			tmp1 = JDD.Constant(0);
			for (j = l; j <= h; j++) {
				tmp1 = JDD.SetVectorElement(tmp1, varDDColVars[v], j-l, j);
			}
			tmp2 = translateExpression(c.getExpression(i));
			JDD.Ref(guard);
			tmp2 = JDD.Apply(JDD.TIMES, tmp2, guard);
			cl = JDD.Apply(JDD.EQUALS, tmp1, tmp2);
			JDD.Ref(guard);
			cl = JDD.Apply(JDD.TIMES, cl, guard);
			// filter out bits not in range
			JDD.Ref(varColRangeDDs[v]);
			cl = JDD.Apply(JDD.TIMES, cl, varColRangeDDs[v]);
			JDD.Ref(range);
			cl = JDD.Apply(JDD.TIMES, cl, range);
			dd = JDD.Apply(JDD.TIMES, dd, cl);
		}
		// if a variable from this module or a global variable
		// does not appear in this update assume it does not change value
		// so multiply by its identity matrix
		for (i = 0; i < numVars; i++) {	
			if ((varList.getModule(i) == m || varList.getModule(i) == -1) && !varsUsed[i]) {
				JDD.Ref(varIdentities[i]);
				dd = JDD.Apply(JDD.TIMES, dd, varIdentities[i]);
			}
		}
		
		return dd;
	}

	// translate an arbitrary expression
	
	private JDDNode translateExpression(Expression e) throws PrismException
	{
		// pass this work onto the Expression2MTBDD object
		// states of interest = JDD.ONE = true = all possible states
		return expr2mtbdd.checkExpressionDD(e, JDD.ONE.copy());
	}

	// build state and transition rewards
	
	private void computeRewards(SystemDDs sysDDs) throws PrismException
	{
		RewardStruct rs;
		int i, j, k, n;
		double d;
		String synch, s;
		JDDNode rewards, states, item;
		ComponentDDs compDDs;
		
		// how many reward structures?
		numRewardStructs = modulesFile.getNumRewardStructs();
		
		// initially rewards zero
		stateRewards = new JDDNode[numRewardStructs];
		transRewards = new JDDNode[numRewardStructs];
		for (j = 0; j < numRewardStructs; j++) {
			stateRewards[j] = JDD.Constant(0);
			sysDDs.ind.rewards[j] = JDD.Constant(0);
			for (i = 0; i < numSynchs; i++) sysDDs.synchs[i].rewards[j] = JDD.Constant(0);
		}
		
		// for each reward structure...
		for (j = 0; j < numRewardStructs; j++) {
			
			// get reward struct
			rs = modulesFile.getRewardStruct(j);
			
			// work through list of items in reward struct
			n = rs.getNumItems();
			for (i = 0; i < n; i++) {
				
				// translate states predicate and reward expression
				states = translateExpression(rs.getStates(i));
				rewards = translateExpression(rs.getReward(i));
				
				// first case: item corresponds to state rewards
				synch = rs.getSynch(i);
				if (synch == null) {
					// restrict rewards to relevant states
					item = JDD.Apply(JDD.TIMES, states, rewards);
					// check for infinite/NaN/negative rewards
					double dmin = JDD.FindMin(item);
					double dmax = JDD.FindMax(item);
					if (!Double.isFinite(dmin)) {
						s = "Reward structure item contains non-finite rewards (" + dmin + ").";
						s += "\nNote that these may correspond to states which are unreachable.";
						s += "\nIf this is the case, try strengthening the predicate.";
						throw new PrismLangException(s, rs.getRewardStructItem(i));
					}
					if (!Double.isFinite(dmax)) {
						s = "Reward structure item contains non-finite rewards (" + dmax + ").";
						s += "\nNote that these may correspond to states which are unreachable.";
						s += "\nIf this is the case, try strengthening the predicate.";
						throw new PrismLangException(s, rs.getRewardStructItem(i));
					}
					if (dmin < 0) {
						s = "Reward structure item contains negative rewards (" + dmin + ").";
						s += "\nNote that these may correspond to states which are unreachable.";
						s += "\nIf this is the case, try strengthening the predicate.";
						throw new PrismLangException(s, rs.getRewardStructItem(i));
					}
					// add to state rewards
					stateRewards[j] = JDD.Apply(JDD.PLUS, stateRewards[j], item);
				}
				
				// second case: item corresponds to transition rewards
				else {
					// work out which (if any) action this is for
					if ("".equals(synch)) {
						compDDs = sysDDs.ind;
					} else if ((k = synchs.indexOf(synch)) != -1) {
						compDDs = sysDDs.synchs[k];
					} else {
						throw new PrismLangException("Invalid action name \"" + synch + "\" in reward structure item", rs.getRewardStructItem(i));
					}
					// identify corresponding transitions
					// (for dtmcs/ctmcs, keep actual values - need to weight rewards; for mdps just store 0/1)
					JDD.Ref(compDDs.trans);
					if (modelType == ModelType.MDP) {
						item = JDD.GreaterThan(compDDs.trans, 0);
					} else {
						item = compDDs.trans;
					}
					// restrict to relevant states
					item = JDD.Apply(JDD.TIMES, item, states);
					// multiply by reward values
					item = JDD.Apply(JDD.TIMES, item, rewards);
					// check for infinite/NaN/negative rewards
					double dmin = JDD.FindMin(item);
					double dmax = JDD.FindMax(item);
					if (!Double.isFinite(dmin)) {
						s = "Reward structure item contains non-finite rewards (" + dmin + ").";
						s += "\nNote that these may correspond to states which are unreachable.";
						s += "\nIf this is the case, try strengthening the predicate.";
						throw new PrismLangException(s, rs.getRewardStructItem(i));
					}
					if (!Double.isFinite(dmax)) {
						s = "Reward structure item contains non-finite rewards (" + dmax + ").";
						s += "\nNote that these may correspond to states which are unreachable.";
						s += "\nIf this is the case, try strengthening the predicate.";
						throw new PrismLangException(s, rs.getRewardStructItem(i));
					}
					if (dmin < 0) {
						s = "Reward structure item contains negative rewards (" + dmin + ").";
						s += "\nNote that these may correspond to states which are unreachable.";
						s += "\nIf this is the case, try strengthening the predicate.";
						throw new PrismLangException(s, rs.getRewardStructItem(i));
					}
					// add result to rewards
					compDDs.rewards[j] = JDD.Apply(JDD.PLUS, compDDs.rewards[j], item);
				}
			}
		}
	}
	
	// calculate dd for initial state(s)
	
	private void buildInitialStates() throws PrismException
	{
		int i;
		JDDNode tmp;
		
		// first, handle case where multiple initial states specified with init...endinit
		if (modulesFile.getInitialStates() != null) {
			start = translateExpression(modulesFile.getInitialStates());
			JDD.Ref(range);
			start = JDD.And(start, range);
			if (start.equals(JDD.ZERO)) throw new PrismLangException("No initial states: \"init\" construct evaluates to false", modulesFile.getInitialStates());
		}
		// second, handle case where initial state determined by init values for variables
		else {
			start = JDD.Constant(1);
			for (i = 0; i < numVars; i++) {
				Object startObj = modulesFile.getVarDeclaration(i).getStartOrDefault().evaluate(constantValues);
				try {
					int startInt = varList.encodeToInt(i, startObj);
					tmp = JDD.SetVectorElement(JDD.Constant(0), varDDRowVars[i], startInt, 1);
					start = JDD.And(start, tmp);
				} catch (PrismLangException e) {
					// attach initial value spec for better error reporting
					e.setASTElement(modulesFile.getVarDeclaration(i).getStart());
					throw e;
				}
			}
		}
	}
	
	// symmetrification
	
	private void doSymmetry(Model model) throws PrismException
	{
		JDDNode tmp, transNew, reach, trans, transRewards[];
		int i, j, k, numSwaps;
		boolean done;
		long clock;
		String ss[];
		
		// parse symmetry reduction parameters
		ss = prism.getSettings().getString(PrismSettings.PRISM_SYMM_RED_PARAMS).split(" ");
		if (ss.length != 2) throw new PrismException ("Invalid parameters for symmetry reduction");
		try {
			numModulesBeforeSymm = Integer.parseInt(ss[0].trim());
			numModulesAfterSymm = Integer.parseInt(ss[1].trim());
		}
		catch (NumberFormatException e) {
			throw new PrismException("Invalid parameters for symmetry reduction");
		}

		clock = System.currentTimeMillis();
		
		// get a copies of model (MT)BDDs
		reach = model.getReach();
		JDD.Ref(reach);
		trans =  model.getTrans();
		JDD.Ref(trans);
		transRewards = new JDDNode[numRewardStructs];
		for (i = 0; i < numRewardStructs; i++) {
			transRewards[i] =  model.getTransRewards(i);
			JDD.Ref(transRewards[i]);
		}
		
		mainLog.print("\nApplying symmetry reduction...\n");
		
		//identifySymmetricModules();
		numSymmModules = numModules - (numModulesBeforeSymm + numModulesAfterSymm);
		computeSymmetryFilters(reach);
		
		// compute number of local states
// 		JDD.Ref(reach);
// 		tmp = reach;
// 		for (i = 0; i < numModules; i++) {
// 			if (i != numModulesBeforeSymm) tmp = JDD.ThereExists(tmp, moduleDDRowVars[i]);
// 		}
// 		tmp = JDD.ThereExists(tmp, globalDDRowVars);
// 		mainLog.println("Local states: " + (int)JDD.GetNumMinterms(tmp, moduleDDRowVars[numModulesBeforeSymm].n()));
// 		JDD.Deref(tmp);
		
		//ODDNode odd = ODDUtils.BuildODD(reach, allDDRowVars);
		//try {sparse.PrismSparse.NondetExport(trans, allDDRowVars, allDDColVars, allDDNondetVars, odd, Prism.EXPORT_PLAIN, "trans-full.tra"); } catch (FileNotFoundException e) {}
		
		mainLog.println("\nNumber of states before before symmetry reduction: " + model.getNumStatesString());
		mainLog.println("DD sizes before symmetry reduction:");
		
		// trans - rows
		mainLog.print("trans: ");
		mainLog.println(JDD.GetInfoString(trans, (modelType==ModelType.MDP)?(allDDRowVars.n()*2+allDDNondetVars.n()):(allDDRowVars.n()*2)));
		JDD.Ref(symm);
		trans = JDD.Apply(JDD.TIMES, trans, symm);
		//mainLog.print("trans (symm): ");
		//mainLog.println(JDD.GetInfoString(trans, (type==ModulesFile.NONDETERMINISTIC)?(allDDRowVars.n()*2+allDDNondetVars.n()):(allDDRowVars.n()*2)));
		
		// trans rewards - rows
		for (k = 0; k < numRewardStructs; k++) {
			mainLog.print("transrew["+k+"]: ");
			mainLog.println(JDD.GetInfoString(transRewards[k], (modelType==ModelType.MDP)?(allDDRowVars.n()*2+allDDNondetVars.n()):(allDDRowVars.n()*2)));
			JDD.Ref(symm);
			transRewards[k] = JDD.Apply(JDD.TIMES, transRewards[k], symm);
			//mainLog.print("transrew["+k+"] (symm): ");
			//mainLog.println(JDD.GetInfoString(transRewards[k], (type==ModulesFile.NONDETERMINISTIC)?(allDDRowVars.n()*2+allDDNondetVars.n()):(allDDRowVars.n()*2)));
		}
		
		mainLog.println("Starting quicksort...");
		done = false;
		numSwaps = 0;
		for (i = numSymmModules; i > 1 && !done; i--) {
			// store trans from previous iter
			JDD.Ref(trans);
			transNew = trans;
			for (j = 0; j < i-1; j++) {
				
				// are there any states where j+1>j+2?
				if (nonSymms[j].equals(JDD.ZERO)) continue;
				
				// identify offending block in trans
				JDD.Ref(transNew);
				JDD.Ref(nonSymms[j]);
				tmp = JDD.Apply(JDD.TIMES, transNew, JDD.PermuteVariables(nonSymms[j], allDDRowVars, allDDColVars));
				//mainLog.print("bad block: ");
				//mainLog.println(JDD.GetInfoString(tmp, (type==ModulesFile.NONDETERMINISTIC)?(allDDRowVars.n()*2+allDDNondetVars.n()):(allDDRowVars.n()*2)));
				
				if (tmp.equals(JDD.ZERO)) { JDD.Deref(tmp); continue; }
				numSwaps++;
				mainLog.println("Iteration "+(numSymmModules-i+1)+"."+(j+1));
				
				// swap
				tmp = JDD.SwapVariables(tmp, moduleDDColVars[numModulesBeforeSymm+j], moduleDDColVars[numModulesBeforeSymm+j+1]);
				//mainLog.print("bad block (swapped): ");
				//mainLog.println(JDD.GetInfoString(tmp, (type==ModulesFile.NONDETERMINISTIC)?(allDDRowVars.n()*2+allDDNondetVars.n()):(allDDRowVars.n()*2)));
				
				// insert swapped block
				JDD.Ref(nonSymms[j]);
				JDD.Ref(tmp);
				transNew = JDD.ITE(JDD.PermuteVariables(nonSymms[j], allDDRowVars, allDDColVars), JDD.Constant(0), JDD.Apply(JDD.PLUS, transNew, tmp));
				//mainLog.print("trans (symm): ");
				//mainLog.println(JDD.GetInfoString(transNew, (type==ModulesFile.NONDETERMINISTIC)?(allDDRowVars.n()*2+allDDNondetVars.n()):(allDDRowVars.n()*2)));
				JDD.Deref(tmp);
				
				for (k = 0; k < numRewardStructs; k++) {
					// identify offending block in trans rewards
					JDD.Ref(transRewards[k]);
					JDD.Ref(nonSymms[j]);
					tmp = JDD.Apply(JDD.TIMES, transRewards[k], JDD.PermuteVariables(nonSymms[j], allDDRowVars, allDDColVars));
					//mainLog.print("bad block: ");
					//mainLog.println(JDD.GetInfoString(tmp, (type==ModulesFile.NONDETERMINISTIC)?(allDDRowVars.n()*2+allDDNondetVars.n()):(allDDRowVars.n()*2)));
					
					// swap
					tmp = JDD.SwapVariables(tmp, moduleDDColVars[numModulesBeforeSymm+j], moduleDDColVars[numModulesBeforeSymm+j+1]);
					//mainLog.print("bad block (swapped): ");
					//mainLog.println(JDD.GetInfoString(tmp, (type==ModulesFile.NONDETERMINISTIC)?(allDDRowVars.n()*2+allDDNondetVars.n()):(allDDRowVars.n()*2)));
					
					// insert swapped block
					JDD.Ref(nonSymms[j]);
					JDD.Ref(tmp);
					transRewards[k] = JDD.ITE(JDD.PermuteVariables(nonSymms[j], allDDRowVars, allDDColVars), JDD.Constant(0), JDD.Apply(JDD.PLUS, transRewards[k], tmp));
					//mainLog.print("transrew["+k+"] (symm): ");
					//mainLog.println(JDD.GetInfoString(transRewards[k], (type==ModulesFile.NONDETERMINISTIC)?(allDDRowVars.n()*2+allDDNondetVars.n()):(allDDRowVars.n()*2)));
					JDD.Deref(tmp);
				}
			}
			
			if (transNew.equals(trans)) {
				done = true;
			}
			JDD.Deref(trans);
			trans = transNew;
		}
		
		// reset (MT)BDDs in model
		model.resetTrans(trans);
		for (i = 0; i < numRewardStructs; i++) {
			model.resetTransRewards(i, transRewards[i]);
		}
		
		// reset reach bdd, etc.
		JDD.Ref(symm);
		reach = JDD.And(reach, symm);
		
		model.setReach(reach);
		model.filterReachableStates();
		
		clock = System.currentTimeMillis() - clock;
		mainLog.println("Symmetry complete: " + (numSymmModules-i) + " iterations, " + numSwaps + " swaps, " + clock/1000.0 + " seconds");
	}

	private void computeSymmetryFilters(JDDNode reach) throws PrismException
	{
		int i;
		JDDNode tmp;
		
		// array for non-symmetric parts
		nonSymms = new JDDNode[numSymmModules-1];
		// dd for all symmetric states
		JDD.Ref(reach);
		symm = reach;
		// loop through symmetric module pairs
		for (i = 0; i < numSymmModules-1; i++) {
			// (locally) symmetric states, i.e. where i+1 <= i+2
			tmp = JDD.VariablesLessThanEquals(moduleDDRowVars[numModulesBeforeSymm+i], moduleDDRowVars[numModulesBeforeSymm+i+1]);
			// non-(locally)-symmetric states
			JDD.Ref(tmp);
			JDD.Ref(reach);
			nonSymms[i] = JDD.And(JDD.Not(tmp), reach);
			// all symmetric states
			symm = JDD.And(symm, tmp);
		}
	}

	// old version of computeSymmetryFilters()
	/*private void computeSymmetryFilters() throws PrismException
	{
		int i, j, k, n;
		String varNames[][] = null;
		JDDNode tmp;
		Expression expr, exprTmp;
		
		// get var names for each symm module
		n = modulesFile.getModule(numModulesBeforeSymm).getNumDeclarations();
		varNames = new String[numModules][];
		for (i = numModulesBeforeSymm; i < numModulesBeforeSymm+numSymmModules; i++) {
			varNames[i-numModulesBeforeSymm] = new String[n];
			j = 0;
			while (j < numVars && varList.getModule(j) != i) j++;
			for (k = 0; k < n; k++) {
				varNames[i-numModulesBeforeSymm][k] = varList.getName(j+k);
			}
		}
		
		// array for non-symmetric parts
		nonSymms = new JDDNode[numSymmModules-1];
		// dd for all symmetric states
		JDD.Ref(reach);
		symm = reach;
		// loop through symmetric module pairs
		for (i = 0; i < numSymmModules-1; i++) {
			// expression for (locally) symmetric states, i.e. where i+1 <= i+2
			expr = new ExpressionTrue();
			for (j = varNames[0].length-1; j >= 0 ; j--) {
				exprTmp = new ExpressionAnd();
				((ExpressionAnd)exprTmp).addOperand(new ExpressionBrackets(new ExpressionRelOp(new ExpressionVar(varNames[i][j], 0), "=", new ExpressionVar(varNames[i+1][j], 0))));
				((ExpressionAnd)exprTmp).addOperand(new ExpressionBrackets(expr));
				expr = exprTmp;
				exprTmp = new ExpressionOr();
				((ExpressionOr)exprTmp).addOperand(new ExpressionBrackets(new ExpressionRelOp(new ExpressionVar(varNames[i][j], 0), "<", new ExpressionVar(varNames[i+1][j], 0))));
				((ExpressionOr)exprTmp).addOperand(expr);
				expr = exprTmp;
			}
			mainLog.println(expr);
			// bdd for (locally) symmetric states, i.e. where i+1 <= i+2
			tmp = expr2mtbdd.translateExpression(expr);
			// non-(locally)-symmetric states
			JDD.Ref(tmp);
			JDD.Ref(reach);
			nonSymms[i] = JDD.And(JDD.Not(tmp), reach);
			// all symmetric states
			symm = JDD.And(symm, tmp);
		}
	}*/
}

//------------------------------------------------------------------------------
