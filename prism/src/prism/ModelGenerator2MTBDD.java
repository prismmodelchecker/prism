//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

import java.util.LinkedList;
import java.util.Vector;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.DeclarationClock;
import parser.ast.DeclarationIntUnbounded;
import parser.ast.DeclarationType;

/**
 * Class to construct a symbolic representation from a ModelGenerator object.
 */
public class ModelGenerator2MTBDD
{
	// Prism stuff
	private Prism prism;
	private PrismLog mainLog;

	// Source model generators
	private ModelGenerator modelGen;
	private RewardGenerator rewardGen;

	// Model info
	private ModelType modelType;
	private VarList varList;
	private int numLabels;
	private int numVars;
	private int numRewardStructs;
	private String[] rewardStructNames;

	// mtbdd stuff

	// dds/dd vars - whole system
	private JDDNode trans; // transition matrix dd
	private JDDNode range; // dd giving range for system
	private JDDNode start; // dd for start state
	private JDDNode reach; // dd for reachable states
	private JDDVars allDDRowVars; // all dd vars (rows)
	private JDDVars allDDColVars; // all dd vars (cols)
	private JDDVars allDDSynchVars; // all dd vars (synchronising actions)
	private JDDVars allDDSchedVars; // all dd vars (scheduling)
	private JDDVars allDDChoiceVars; // all dd vars (internal non-det.)
	private JDDVars allDDNondetVars; // all dd vars (all non-det.)
	// dds/dd vars - modules
	private JDDVars[] moduleDDRowVars; // dd vars for each module (rows)
	private JDDVars[] moduleDDColVars; // dd vars for each module (cols)
	private JDDNode[] moduleRangeDDs; // dd giving range for each module
	private JDDNode[] moduleIdentities; // identity matrix for each module
	// dds/dd vars - variables
	private JDDVars[] varDDRowVars; // dd vars (row/col) for each module variable
	private JDDVars[] varDDColVars;
	private JDDNode[] varRangeDDs; // dd giving range for each module variable
	private JDDNode[] varColRangeDDs; // dd giving range for each module variable (in col vars)
	private JDDNode[] varIdentities; // identity matrix for each module variable
	// dds/dd vars - nondeterminism
	private JDDNode[] ddSynchVars; // individual dd vars for synchronising actions
	private JDDNode[] ddSchedVars; // individual dd vars for scheduling non-det.
	private JDDNode[] ddChoiceVars; // individual dd vars for local non-det.

	private ModelVariablesDD modelVariables;

	// action info
	private Vector<String> synchs; // list of action names
	private JDDNode transActions; // dd for transition action labels (MDPs)
	private Vector<JDDNode> transPerAction; // dds for transition action labels (D/CTMCs)
	// labels
	private JDDNode[] labelsArray;
	// rewards
	private JDDNode[] stateRewardsArray;
	private JDDNode[] transRewardsArray;
	
	private int maxNumChoices = 0;

	public ModelGenerator2MTBDD(Prism prism)
	{
		this.prism = prism;
		mainLog = prism.getMainLog();
	}

	/**
	 * Build a Model corresponding to the passed in model generator.
	 */
	public Model build(ModelGenerator modelGen, RewardGenerator rewardGen) throws PrismException
	{
		this.modelGen = modelGen;
		this.rewardGen = rewardGen;
		modelType = modelGen.getModelType();
		varList = modelGen.createVarList();
		numLabels = modelGen.getNumLabels();
		numVars = varList.getNumVars();
		modelVariables = new ModelVariablesDD();
		numRewardStructs = rewardGen.getNumRewardStructs();
		rewardStructNames = rewardGen.getRewardStructNames().toArray(new String[0]);
		return buildModel();
	}

	/** build model */
	private Model buildModel() throws PrismException
	{
		Model model = null;
		JDDNode tmp, tmp2;
		JDDVars ddv;
		int i;

		// for an mdp, compute the max number of choices in a state
		if (modelType == ModelType.MDP)
			maxNumChoices = 32; // TODO: un-hard-code

		// allocate dd variables
		allocateDDVars();
		sortDDVars();
		sortIdentities();
		sortRanges();

		// construct transition matrix and rewards
		buildTransAndRewards();

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
		// 		if (modelType == ModelType.MDP) mainLog.print(", " + allDDNondetVars.n() + "nd");
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

		int numModules = 1; // just one module
		String moduleNames[] = new String[] { "M" };
		Values constantValues = modelGen.getConstantValues();

		// create new Model object to be returned
		if (modelType == ModelType.DTMC) {
			model = new ProbModel(trans, start, stateRewardsArray, transRewardsArray, rewardStructNames, allDDRowVars, allDDColVars, modelVariables, numModules,
					moduleNames, moduleDDRowVars, moduleDDColVars, numVars, varList, varDDRowVars, varDDColVars, constantValues);
		} else if (modelType == ModelType.MDP) {
			model = new NondetModel(trans, start, stateRewardsArray, transRewardsArray, rewardStructNames, allDDRowVars, allDDColVars, allDDSynchVars,
					allDDSchedVars, allDDChoiceVars, allDDNondetVars, modelVariables, numModules, moduleNames, moduleDDRowVars, moduleDDColVars, numVars, varList,
					varDDRowVars, varDDColVars, constantValues);
		} else if (modelType == ModelType.CTMC) {
			model = new StochModel(trans, start, stateRewardsArray, transRewardsArray, rewardStructNames, allDDRowVars, allDDColVars, modelVariables, numModules,
					moduleNames, moduleDDRowVars, moduleDDColVars, numVars, varList, varDDRowVars, varDDColVars, constantValues);
		}
		// set action info
		// TODO: disable if not required?
		model.setSynchs(synchs);
		if (modelType != ModelType.MDP) {
			model.setTransPerAction((JDDNode[]) transPerAction.toArray(new JDDNode[0]));
		} else {
			model.setTransActions(transActions);
		}

		// no need to do reachability
		model.setReach(reach);
		model.filterReachableStates();

		// Print some info (if extraddinfo flag on)
		if (prism.getExtraDDInfo()) {
			mainLog.print("Reach: " + JDD.GetNumNodes(model.getReach()) + " nodes\n");
		}

		// find any deadlocks
		model.findDeadlocks(prism.getFixDeadlocks());

		// attach labels
		for (int l = 0; l < numLabels; l++) {
			model.addLabelDD(modelGen.getLabelName(l), labelsArray[l]);
		}
		
		// deref spare dds
		JDD.Deref(moduleIdentities[0]);
		JDD.Deref(moduleRangeDDs[0]);
		JDD.DerefArray(varIdentities, numVars);
		JDD.DerefArray(varRangeDDs, numVars);
		JDD.DerefArray(varColRangeDDs, numVars);
		JDD.Deref(range);
		if (modelType == ModelType.MDP) {
			JDD.DerefArray(ddSynchVars, ddSynchVars.length);
			JDD.DerefArray(ddSchedVars, ddSchedVars.length);
			JDD.DerefArray(ddChoiceVars, ddChoiceVars.length);
		}

		return model;
	}

	/**
	 * allocate DD vars for system
	 * i.e. decide on variable ordering and request variables from CUDD
	 */
	private void allocateDDVars() throws PrismNotSupportedException
	{
		JDDNode vr, vc;
		int i, j, n;

		// create arrays/etc. first

		// nondeterministic variables
		if (modelType == ModelType.MDP) {
			ddSynchVars = new JDDNode[0];
			ddSchedVars = new JDDNode[0];
			ddChoiceVars = new JDDNode[maxNumChoices];
		}
		// module variable (row/col) vars
		varDDRowVars = new JDDVars[numVars];
		varDDColVars = new JDDVars[numVars];
		for (i = 0; i < numVars; i++) {
			varDDRowVars[i] = new JDDVars();
			varDDColVars[i] = new JDDVars();
		}

		// now allocate variables

		// allocate nondeterministic variables
		if (modelType == ModelType.MDP) {
			for (i = 0; i < maxNumChoices; i++) {
				ddChoiceVars[i] = modelVariables.allocateVariable("l" + i);
			}
		}

		// allocate dd variables for module variables (i.e. rows/cols)
		// go through all vars in order (incl. global variables)
		// so overall ordering can be specified by ordering in the input file
		for (i = 0; i < numVars; i++) {
			DeclarationType declType = varList.getDeclarationType(i);
			if (declType instanceof DeclarationClock || declType instanceof DeclarationIntUnbounded) {
				throw new PrismNotSupportedException("Cannot build a model that contains a variable with unbounded range (try the explicit engine instead)");
			}
			// get number of dd variables needed
			// (ceiling of log2 of range of variable)
			n = varList.getRangeLogTwo(i);
			// add pairs of variables (row/col)
			for (j = 0; j < n; j++) {
				// new dd row variable
				vr = modelVariables.allocateVariable(varList.getName(i) + "." + j);
				// new dd col variable
				vc = modelVariables.allocateVariable(varList.getName(i) + "'." + j);
				varDDRowVars[i].addVar(vr);
				varDDColVars[i].addVar(vc);
			}
		}
	}

	/**
	 * sort out DD variables and the arrays they are stored in
	 * (more than one copy of most variables is stored)
	 */
	private void sortDDVars()
	{
		int i;

		// put refs for all vars in each module together
		// create arrays
		moduleDDRowVars = new JDDVars[1];
		moduleDDColVars = new JDDVars[1];
		moduleDDRowVars[0] = new JDDVars();
		moduleDDColVars[0] = new JDDVars();
		// go thru all variables
		for (i = 0; i < numVars; i++) {
			moduleDDRowVars[0].copyVarsFrom(varDDRowVars[i]);
			moduleDDColVars[0].copyVarsFrom(varDDColVars[i]);
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
			for (i = 0; i < ddChoiceVars.length; i++) {
				// add to list
				allDDChoiceVars.addVar(ddChoiceVars[i].copy());
				allDDNondetVars.addVar(ddChoiceVars[i].copy());
			}
		}
	}

	/** sort DDs for identities */
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
		moduleIdentities = new JDDNode[1];
		// product of identities for vars in module
		id = JDD.Constant(1);
		for (j = 0; j < numVars; j++) {
			if (varList.getModule(j) == 0) {
				id = JDD.Apply(JDD.TIMES, id, varIdentities[j].copy());
			}
		}
		moduleIdentities[0] = id;
	}

	/** Sort DDs for ranges */
	private void sortRanges()
	{
		int i;

		// initialise range for whole system
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
		moduleRangeDDs = new JDDNode[1];
		// obtain range dd by abstracting from identity matrix
		moduleRangeDDs[0] = JDD.SumAbstract(moduleIdentities[0].copy(), moduleDDColVars[0]);
	}

	/** Construct transition matrix and rewards */
	private void buildTransAndRewards() throws PrismException
	{
		JDDNode elem, tmp;

		// initialise action list
		synchs = new Vector<String>();

		// initialise mtbdds
		trans = JDD.Constant(0);
		if (modelType != ModelType.MDP) {
			transPerAction = new Vector<JDDNode>();
			transPerAction.add(JDD.Constant(0));
		} else {
			transActions = JDD.Constant(0);
		}
		start = JDD.Constant(0); 
		reach = JDD.Constant(0);
		labelsArray = new JDDNode[numLabels];
		for (int l = 0; l < numLabels; l++) {
			labelsArray[l] = JDD.Constant(0);
		}
		stateRewardsArray = new JDDNode[numRewardStructs];
		transRewardsArray = new JDDNode[numRewardStructs];
		for (int r = 0; r < numRewardStructs; r++) {
			stateRewardsArray[r] = JDD.Constant(0);
			transRewardsArray[r] = JDD.Constant(0);
		}
		
		LinkedList<State> explore = new LinkedList<State>();
		// Add initial state(s) to 'explore', update start/reach
		for (State initState : modelGen.getInitialStates()) {
			explore.add(initState);
			JDDNode ddState = encodeState(initState, varDDRowVars);
			start = JDD.Or(start, ddState.copy());
			reach = JDD.Or(reach, ddState);
		}
		// Explore...
		while (!explore.isEmpty()) {
			// Pick next state to explore
			State state = explore.removeFirst();
			JDDNode ddState = encodeState(state, varDDRowVars);
			// Explore all choices/transitions from this state
			modelGen.exploreState(state);
			// Look at each outgoing choice in turn
			int nc = modelGen.getNumChoices();
			if (modelType == ModelType.MDP && nc > maxNumChoices) {
				String msg = "Too many nondeterministic choices (" + nc + ") at state " + state.toString(modelGen);
				msg += ". Maximum is currently hard-coded at " + maxNumChoices;
				throw new PrismException(msg);
			}
			for (int i = 0; i < nc; i++) {
				Object o = modelGen.getChoiceAction(i);
				String a = o == null ? null : o.toString();
				// Look at each transition in the choice
				int nt = modelGen.getNumTransitions(i);
				for (int j = 0; j < nt; j++) {
					State stateNew = modelGen.computeTransitionTarget(i, j);
					double d = modelGen.getTransitionProbability(i, j);
					JDDNode ddStateNew = encodeState(stateNew, varDDRowVars);
					// Is this a new state?
					if (!JDD.AreIntersecting(reach, ddStateNew)) {
						// If so, add to the explore list
						explore.add(stateNew);
						// And to model
						reach = JDD.Or(reach, ddStateNew.copy());
					}
					// Build MTBDD for transition
					elem = JDD.Apply(JDD.TIMES, ddState.copy(), JDD.PermuteVariables(ddStateNew.copy(), allDDRowVars, allDDColVars));
					if (modelType == ModelType.MDP) {
						elem = JDD.Apply(JDD.TIMES, elem, JDD.SetVectorElement(JDD.Constant(0), allDDChoiceVars, i, 1));
					}
					// add it into mtbdds for transition matrix and transition rewards
					trans = JDD.Apply(JDD.PLUS, trans, JDD.Apply(JDD.TIMES, JDD.Constant(d), elem.copy()));
					// look up action name
					int k;
					if (!(a == null || "".equals(a))) {
						k = synchs.indexOf(a);
						// add to list if first time seen 
						if (k == -1) {
							synchs.add(a);
							k = synchs.size() - 1;
						}
						k++;
					} else {
						k = 0;
					}
					/// ...for dtmcs/ctmcs...
					if (modelType != ModelType.MDP) {
						// get (or create) dd for action k
						if (k < transPerAction.size()) {
							tmp = transPerAction.get(k);
						} else {
							tmp = JDD.Constant(0);
							transPerAction.add(tmp);
						}
						// add element to matrix
						tmp = JDD.Apply(JDD.PLUS, tmp, JDD.Apply(JDD.TIMES, JDD.Constant(d), elem.copy()));
						transPerAction.set(k, tmp);
					}
					/// ...for mdps...
					else {
						tmp = JDD.ThereExists(elem.copy(), allDDColVars);
						// use max here because we see multiple transitions for a single choice
						transActions = JDD.Apply(JDD.MAX, transActions, JDD.Apply(JDD.TIMES, JDD.Constant(k), tmp));
					}

					// Add action rewards
					for (int r = 0; r < numRewardStructs; r++) {
						double tr = rewardGen.getStateActionReward(r, state, o);
						transRewardsArray[r] = JDD.Apply(JDD.PLUS, transRewardsArray[r], JDD.Apply(JDD.TIMES, JDD.Constant(tr), elem.copy()));
					}
					
					// deref element dd
					JDD.Deref(elem);
					JDD.Deref(ddStateNew);
				}
				
				// Print some progress info occasionally
				// TODO progress.updateIfReady(src + 1);
			}
			
			for (int l = 0; l < numLabels; l++) {
				if (modelGen.isLabelTrue(l)) {
					labelsArray[l] = JDD.Or(labelsArray[l], ddState.copy());
				}
			}
			
			// Add state rewards
			for (int r = 0; r < numRewardStructs; r++) {
				double sr = rewardGen.getStateReward(r, state);
				stateRewardsArray[r] = JDD.Apply(JDD.PLUS, stateRewardsArray[r], JDD.Apply(JDD.TIMES, JDD.Constant(sr), ddState.copy()));
			}
			
			JDD.Deref(ddState);
		}
	}
	
	/**
	 * Encode a state into a BDD (referencing the result).
	 */
	private JDDNode encodeState(State state, JDDVars[] varDDVars) throws PrismException
	{
		JDDNode res;
		int i, j = 0;
		res = JDD.Constant(1);
		for (i = 0; i < numVars; i++) {
			try {
				j = varList.encodeToInt(i, state.varValues[i]);
			} catch (PrismLangException e) {
				throw new PrismException("Error during JDD encodeState for state value at index " + i);
			}
			res = JDD.Apply(JDD.TIMES, res, JDD.SetVectorElement(JDD.Constant(0), varDDVars[i], j, 1.0));
		}
		return res;
	}
}

//------------------------------------------------------------------------------
