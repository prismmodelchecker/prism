//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
//	* Ludwig Pauly <ludwigpauly@gmail.com> (TU Dresden)
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

package symbolic.build;

import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.Map.Entry;

import io.ExplicitModelImporter;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import parser.Values;
import parser.VarList;
import parser.ast.DeclarationType;
import prism.ModelInfo;
import prism.ModelType;
import prism.Prism;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismNotSupportedException;
import prism.ProgressDisplay;
import prism.RewardGenerator;
import symbolic.model.Model;
import symbolic.model.ModelSymbolic;
import symbolic.model.ModelVariablesDD;
import symbolic.model.NondetModel;
import symbolic.model.ProbModel;
import symbolic.model.StochModel;

/**
 * Class to convert explicit-state file storage of a model to symbolic representation.
 */
public class ExplicitFiles2MTBDD
{
	// Prism stuff
	private Prism prism;
	private PrismLog mainLog;

	// Importer / files to read in from
	private ExplicitModelImporter importer;

	// Model info
	private ModelInfo modelInfo;
	private ModelType modelType;
	private VarList varList;
	private int numVars;
	private int numStates;

	// Explicit storage of states
	private int statesArray[][] = null;

	// Reward info
	private RewardGenerator rewardInfo;

	// mtbdd stuff

	// dds/dd vars - whole system
	private JDDNode trans; // transition matrix dd
	private JDDNode start; // dd for start state
	private JDDNode stateRewards[]; // array of dds for state rewards
	private JDDNode transRewards[]; // array of dds for transition rewards
	private JDDVars allDDRowVars; // all dd vars (rows)
	private JDDVars allDDColVars; // all dd vars (cols)
	private JDDVars allDDNondetVars; // all dd vars (all non-det.)
	// dds/dd vars - variables
	private JDDVars[] varDDRowVars; // dd vars (row/col) for each module variable
	private JDDVars[] varDDColVars;
	// dds/dd vars - nondeterminism

	private ModelVariablesDD modelVariables;
	
	// action info
	private Vector<String> synchs; // list of action names
	private JDDNode transActions; // dd for transition action labels (MDPs)
	private Vector<JDDNode> transPerAction; // dds for transition action labels (D/CTMCs)

	private int maxNumChoices = 0;
	private LinkedHashMap<String, JDDNode> labelsDD;

	// Progress info
	private ProgressDisplay progress;
	private int transitionsImported;

	public ExplicitFiles2MTBDD(Prism prism)
	{
		this.prism = prism;
		mainLog = prism.getMainLog();
	}

	/**
	 * Build a Model corresponding to the passed in states/transitions/labels files.
	 * Variable info and model type is taken from a {@code ModelInfo} object.
	 * The number of states should also be passed in as {@code numStates}.
	 */
	public Model build(ExplicitModelImporter importer) throws PrismException
	{
		this.importer = importer;
		this.modelInfo = importer.getModelInfo();
		modelType = modelInfo.getModelType();
		varList = modelInfo.createVarList();
		numVars = varList.getNumVars();
		this.numStates = importer.getNumStates();
		modelVariables = new ModelVariablesDD();

		rewardInfo = importer.getRewardInfo();

		// Build states list, if info is available
		// (importer can handle case where it is unavailable, but we bypass this)
		if (importer.providesStates()) {
			readStatesFromFile();
		}

		return buildModel();
	}

	/** read info about reachable state space from file and store explicitly */
	private void readStatesFromFile() throws PrismException
	{
		statesArray = new int[numStates][];
		for (int s = 0; s < numStates; s++) {
			statesArray[s] = new int[numVars];
		}
		importer.extractStates((s, i, o) -> statesArray[s][i] = varList.encodeToInt(i, o));
	}

	/** build model */
	private Model buildModel() throws PrismException
	{
		ModelSymbolic model = null;
		JDDNode tmp, tmp2;
		JDDVars ddv;
		int i;

		// for an mdp, compute the max number of choices in a state
		if (modelType == ModelType.MDP) {
			maxNumChoices = importer.computeMaxNumChoices();
		}

		// allocate dd variables
		allocateDDVars();

		// construct transition matrix from file
		buildTrans();

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

		// construct labels and init state info
		buildLabelsAndInitialStates();

		// compute rewards
		buildStateRewards();
		buildTransitionRewards();

		Values constantValues = new Values(); // no constants

		// create new Model object to be returned
		// they need a module name list, so we fake that
		int numModules = 1;
		String moduleNames[] = new String[] { "M" };
		String rewardStructNames[] = (String[]) rewardInfo.getRewardStructNames().toArray(new String[0]);

		if (modelType == ModelType.DTMC) {
			model = new ProbModel(trans, start, allDDRowVars, allDDColVars, modelVariables,
					varList, varDDRowVars, varDDColVars);
		} else if (modelType == ModelType.MDP) {
			model = new NondetModel(trans, start, allDDRowVars, allDDColVars, allDDNondetVars, modelVariables,
					varList, varDDRowVars, varDDColVars);
		} else if (modelType == ModelType.CTMC) {
			model = new StochModel(trans, start, allDDRowVars, allDDColVars, modelVariables,
					varList, varDDRowVars, varDDColVars);
		}
		model.setRewards(stateRewards, transRewards, rewardStructNames);
		model.setConstantValues(constantValues);
		// set action info
		// TODO: disable if not required?
		model.setSynchs(synchs);
		if (modelType != ModelType.MDP) {
			((ProbModel) model).setTransPerAction((JDDNode[]) transPerAction.toArray(new JDDNode[0]));
		} else {
			((NondetModel) model).setTransActions(transActions);
		}

		// do reachability (or not)
		if (prism.getDoReach()) {
			mainLog.print("\nComputing reachable states...\n");
			model.doReachability();
			model.filterReachableStates();
		} else {
			mainLog.print("\nSkipping reachable state computation.\n");
			model.skipReachability();
			model.filterReachableStates();
		}

		// Print some info (if extraddinfo flag on)
		if (prism.getExtraDDInfo()) {
			mainLog.print("Reach: " + JDD.GetNumNodes(model.getReach()) + " nodes\n");
		}

		// find any deadlocks
		model.findDeadlocks(prism.getFixDeadlocks());

		// attach labels
		attachLabels(model);

		// deref spare dds
		if (labelsDD != null) {
			for (JDDNode d : labelsDD.values()) {
				JDD.Deref(d);
			}
		}

		return model;
	}

	/**
	 * allocate DD vars for system
	 * i.e. decide on variable ordering and request variables from CUDD
	 */
	private void allocateDDVars() throws PrismException
	{
		int i, j, n;

		modelVariables = new ModelVariablesDD();

		// create arrays/etc. first

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
			allDDNondetVars = new JDDVars();
			for (i = 0; i < maxNumChoices; i++) {
				allDDNondetVars.addVar(modelVariables.allocateVariable("l" + i));
			}
		}

		// allocate dd variables for module variables (i.e. rows/cols)
		// go through all vars in order (incl. global variables)
		// so overall ordering can be specified by ordering in the input file
		allDDRowVars = new JDDVars();
		allDDColVars = new JDDVars();
		for (i = 0; i < numVars; i++) {
			DeclarationType declType = varList.getDeclarationType(i);
			if (declType.isUnbounded()) {
				throw new PrismNotSupportedException("Cannot build a model that contains a variable with unbounded range (try the explicit engine instead)");
			}
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
			allDDRowVars.copyVarsFrom(varDDRowVars[i]);
			allDDColVars.copyVarsFrom(varDDColVars[i]);
		}
	}

	/** Construct transition matrix from file */
	private void buildTrans() throws PrismException
	{
		// Initial output
		mainLog.print("Importing transitions... ");
		progress = new ProgressDisplay(mainLog);
		progress.start();
		progress.setTotalCount(importer.getNumTransitions());
		transitionsImported = 0;
		// Initialise storage/MTBDDs
		synchs = new Vector<String>();
		trans = JDD.Constant(0);
		if (modelType != ModelType.MDP) {
			transPerAction = new Vector<JDDNode>();
			transPerAction.add(JDD.Constant(0));
		} else {
			transActions = JDD.Constant(0);
		}
		// Import transitions
		if (modelType != ModelType.MDP) {
			importer.extractMCTransitions((s, s2, d, a) -> storeMCTransition(s, s2, d, a));
		} else {
			importer.extractMDPTransitions((s, i, s2, d, a) -> storeMDPTransition(s, i, s2, d, a));
		}
		// Finish progress display
		progress.update(transitionsImported);
		progress.end();
	}

	/**
	 * Stores transRewards in the required format for mtbdd.
	 *
	 * @param s source state index
	 * @param s2 target state index
	 * @param d probability value
	 * @param a action (optional)
	 */
	protected void storeMCTransition(int s, int s2, double d, Object a)
	{
		// construct element of vector mtbdd
		JDDNode elem = encodeStatePair(s, s2);

		// add it into mtbdds for transition matrix
		JDD.Ref(elem);
		trans = JDD.Apply(JDD.PLUS, trans, JDD.Apply(JDD.TIMES, JDD.Constant(d), elem));
		// look up action name
		String aStr = a == null ? "" : a.toString();
		int j = 0;
		if (!("".equals(aStr))) {
			j = synchs.indexOf(aStr);
			// add to list if first time seen
			if (j == -1) {
				synchs.add(aStr);
				j = synchs.size() - 1;
			}
			j++;
		}
		// get (or create) dd for action j
		JDDNode tmp;
		if (j < transPerAction.size()) {
			tmp = transPerAction.get(j);
		} else {
			tmp = JDD.Constant(0);
			transPerAction.add(tmp);
		}
		// add element to matrix
		JDD.Ref(elem);
		tmp = JDD.Apply(JDD.PLUS, tmp, JDD.Apply(JDD.TIMES, JDD.Constant(d), elem));
		transPerAction.set(j, tmp);
		// deref element dd
		JDD.Deref(elem);
		// Print some progress info occasionally
		progress.updateIfReady(++transitionsImported);
	}

	/**
	 * Stores transRewards in the required format for mtbdd.
	 *
	 * @param s source state index
	 * @param i choice index
	 * @param s2 target state index
	 * @param d probability value
	 * @param a action (optional)
	 */
	protected void storeMDPTransition(int s, int i, int s2, double d, Object a)
	{
		// construct element of vector mtbdd
		JDDNode elem = encodeStatePair(s, s2);
		elem = JDD.Apply(JDD.TIMES, elem, JDD.SetVectorElement(JDD.Constant(0), allDDNondetVars, i, 1));

		// add it into mtbdds for transition matrix
		JDD.Ref(elem);
		trans = JDD.Apply(JDD.PLUS, trans, JDD.Apply(JDD.TIMES, JDD.Constant(d), elem));
		// look up action name
		String aStr = a == null ? "" : a.toString();
		int j = 0;
		if (!("".equals(aStr))) {
			j = synchs.indexOf(aStr);
			// add to list if first time seen
			if (j == -1) {
				synchs.add(aStr);
				j = synchs.size() - 1;
			}
			j++;
		}
		JDD.Ref(elem);
		JDDNode tmp = JDD.ThereExists(elem, allDDColVars);
		// use max here because we see multiple transitions for a sinlge choice
		transActions = JDD.Apply(JDD.MAX, transActions, JDD.Apply(JDD.TIMES, JDD.Constant(j), tmp));
		// deref element dd
		JDD.Deref(elem);
		// Print some progress info occasionally
		progress.updateIfReady(++transitionsImported);
	}

	/** Load info on labels and initial states from importer */
	private void buildLabelsAndInitialStates() throws PrismException
	{
		// Initialise BDDs
		start = JDD.Constant(0);
		int numLabels = modelInfo.getNumLabels();
		JDDNode[] labelDDs = new JDDNode[numLabels];
		for (int l = 0; l < numLabels; l++) {
			labelDDs[l] = JDD.Constant(0);
		}
		// Extract info
		importer.extractLabelsAndInitialStates((s, l) -> {
			labelDDs[l] = JDD.Or(labelDDs[l], encodeState(s));
		}, s -> {
			start = JDD.Or(start, encodeState(s));
		});
		if (start == null || start.equals(JDD.ZERO)) {
			throw new PrismException("No initial states found in labels file");
		}
		// Store label map
		labelsDD = new LinkedHashMap<>();
		for (int l = 0; l < numLabels; l++) {
			labelsDD.put(modelInfo.getLabelName(l), labelDDs[l]);
		}
	}

	/** Attach the computed label information to the model */
	private void attachLabels(ModelSymbolic model)
	{
		if (labelsDD == null) {
			return;
		}
		for (Entry<String, JDDNode> e : labelsDD.entrySet()) {
			model.addLabelDD(e.getKey(), e.getValue().copy());
		}
	}

	/**
	 * Load state rewards from the ExplicitFilesRewardGenerator.
	 */
	private void buildStateRewards() throws PrismException
	{
		int numRewardStructs = rewardInfo.getNumRewardStructs();
		stateRewards = new JDDNode[numRewardStructs];
		for (int r = 0; r < numRewardStructs; r++) {
			stateRewards[r] = JDD.Constant(0);
			int finalR = r;
			importer.extractStateRewards(r, (i, d) -> storeStateReward(finalR, i, d));
		}
	}

	/**
	 * Load transition rewards from the ExplicitFilesRewardGenerator.
	 */
	private void buildTransitionRewards() throws PrismException
	{
		int numRewardStructs = rewardInfo.getNumRewardStructs();
		transRewards = new JDDNode[numRewardStructs];
		for (int r = 0; r < numRewardStructs; r++) {
			transRewards[r] = JDD.Constant(0);
			int finalR = r;
			if (!modelType.nondeterministic()) {
				importer.extractMCTransitionRewards(r, (s, s2, d) -> storeMCTransitionReward(finalR, s, s2, d));
			} else {
				importer.extractMDPTransitionRewards(r, (s, i, s2, d) -> storeMDPTransitionReward(finalR, s, i, s2, d));
			}
		}
	}

	/**
	 * Stores stateRewards in the required format for mtbdd.
	 *
	 * @param rewardStructIndex reward structure index
	 * @param s state index
	 * @param d reward value
	 */
	protected void storeStateReward(int rewardStructIndex, int s, double d)
	{
		// Construct element of vector MTBDD
		JDDNode tmp = encodeState(s);
		// Add it into MTBDD for state rewards
		stateRewards[rewardStructIndex] = JDD.Plus(stateRewards[rewardStructIndex], JDD.Times(JDD.Constant(d), tmp));
	}

	/**
	 * Stores transRewards in the required format for mtbdd.
	 *
	 * @param rewardStructIndex reward structure index
	 * @param s source state index
	 * @param s2 target state index
	 * @param d reward value
	 */
	protected void storeMCTransitionReward(int rewardStructIndex, int s, int s2, double d)
	{
		// Construct element of matrix MTBDD
		JDDNode tmp = encodeStatePair(s, s2);
		// Add it into MTBDD for state rewards
		transRewards[rewardStructIndex] = JDD.Plus(transRewards[rewardStructIndex], JDD.Times(JDD.Constant(d), tmp));
	}

	/**
	 * Stores transRewards in the required format for mtbdd.
	 *
	 * @param rewardStructIndex reward structure index
	 * @param s source state index
	 * @param i choice index
	 * @param s2 target state index
	 * @param d reward value
	 */
	protected void storeMDPTransitionReward(int rewardStructIndex, int s, int i, int s2, double d)
	{
		// Construct element of matrix MTBDD
		JDDNode tmp = encodeStatePair(s, s2);
		tmp = JDD.Apply(JDD.TIMES, tmp, JDD.SetVectorElement(JDD.Constant(0), allDDNondetVars, i, 1));
		// Add it into MTBDD for state rewards
		transRewards[rewardStructIndex] = JDD.Plus(transRewards[rewardStructIndex], JDD.Times(JDD.Constant(d), tmp));
	}

	/**
	 * Encode a state index into BDD vars (referencing the result).
	 * @param s state index
	 */
	private JDDNode encodeState(int s)
	{
		JDDNode res;
		// Case where there is no state list...
		if (statesArray == null) {
			res = JDD.SetVectorElement(JDD.Constant(0), varDDRowVars[0], s, 1.0);
		}
		// Case where there is a state list...
		else {
			res = JDD.Constant(1);
			for (int j = 0; j < numVars; j++) {
				res = JDD.Times(res, JDD.SetVectorElement(JDD.Constant(0), varDDRowVars[j], statesArray[s][j], 1));
			}
		}
		return res;
	}

	/**
	 * Encode a state index pair into BDD vars (referencing the result).
	 * @param s source state index
	 * @param s2 target state index
	 */
	private JDDNode encodeStatePair(int s, int s2)
	{
		// Note: could do this with a conjunction of BDDs from two
		// calls to encodeState but this way should be more efficient

		JDDNode res;
		// Case where there is no state list...
		if (statesArray == null) {
			res = JDD.SetMatrixElement(JDD.Constant(0), varDDRowVars[0], varDDColVars[0], s, s2, 1.0);
		}
		// Case where there is a state list...
		else {
			res = JDD.Constant(1);
			for (int i = 0; i < numVars; i++) {
				res = JDD.Apply(JDD.TIMES, res, JDD.SetVectorElement(JDD.Constant(0), varDDRowVars[i], statesArray[s][i], 1.0));
				res = JDD.Apply(JDD.TIMES, res, JDD.SetVectorElement(JDD.Constant(0), varDDColVars[i], statesArray[s2][i], 1.0));
			}
		}
		return res;
	}
}

//------------------------------------------------------------------------------
