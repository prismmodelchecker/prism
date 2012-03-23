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

package prism;

import java.util.*;

import jdd.*;
import parser.*;
import parser.ast.*;
import explicit.*;

/**
 * Class to convert explicit-state representation of a model to a symbolic one.
 */
public class ExplicitModel2MTBDD
{
	// prism
	private Prism prism;

	// logs
	private PrismLog mainLog; // main log
	private PrismLog techLog; // tech log

	// Explicit-state model
	private explicit.Model modelExpl;

	// ModulesFile object, essentially just to store variable info
	private ModulesFile modulesFile;

	// model info

	// type
	private ModelType modelType; // model type (dtmc/mdp/ctmc.)
	// vars info
	private int numVars; // total number of variables
	private VarList varList; // VarList object to store all var info 
	// explicit storage of states
	private int numStates = 0;
	private List<State> statesList = null;

	// mtbdd stuff

	// dds/dd vars - whole system
	private JDDNode trans; // transition matrix dd
	private JDDNode start; // dd for start state
	private JDDNode stateRewards; // dd of state rewards
	private JDDNode transRewards; // dd of transition rewards
	private JDDVars allDDRowVars; // all dd vars (rows)
	private JDDVars allDDColVars; // all dd vars (cols)
	private JDDVars allDDSynchVars; // all dd vars (synchronising actions)
	private JDDVars allDDSchedVars; // all dd vars (scheduling)
	private JDDVars allDDChoiceVars; // all dd vars (internal non-det.)
	private JDDVars allDDNondetVars; // all dd vars (all non-det.)
	// dds/dd vars - modules
	private JDDVars[] moduleDDRowVars; // dd vars for each module (rows)
	private JDDVars[] moduleDDColVars; // dd vars for each module (cols)
	// dds/dd vars - variables
	private JDDVars[] varDDRowVars; // dd vars (row/col) for each module variable
	private JDDVars[] varDDColVars;
	// dds/dd vars - nondeterminism
	private JDDNode[] ddSynchVars; // individual dd vars for synchronising actions
	private JDDNode[] ddSchedVars; // individual dd vars for scheduling non-det.
	private JDDNode[] ddChoiceVars; // individual dd vars for local non-det.
	// names for all dd vars used
	private Vector<String> ddVarNames;
	// action info
	private Vector<String> synchs; // list of action names
	private JDDNode transActions; // dd for transition action labels (MDPs)
	private Vector<JDDNode> transPerAction; // dds for transition action labels (D/CTMCs)
	// max num choices (MDP only)
	private int maxNumChoices = 0;

	// constructor

	public ExplicitModel2MTBDD(Prism prism)
	{
		this.prism = prism;
		mainLog = prism.getMainLog();
		techLog = prism.getTechLog();
	}

	// Build model

	/**
	 * Construct a symbolic model from an explicit state one.  
	 * If provided, {@code statesList} is used to encode states using model variables,
	 * in which case a corresponding {@code ModulesFile} must be provided for variable
	 * details (and e.g. module names). If both are null, a single variable x is assumed. 
	 */
	public Model buildModel(explicit.Model modelExpl, List<State> statesList, ModulesFile modulesFile, boolean doReach) throws PrismException
	{
		Model model = null;
		JDDNode tmp, tmp2;
		JDDVars ddv;
		int i;

		// Store model/type/states
		this.modelExpl = modelExpl;
		this.modelType = modelExpl.getModelType();
		this.numStates = modelExpl.getNumStates();
		this.statesList = statesList;

		// Store modules files or create dummy one if needed
		if (statesList != null) {
			this.modulesFile = modulesFile;
		} else {
			this.modulesFile = modulesFile = new ModulesFile();
			Module m = new Module("M");
			Declaration d = new Declaration("x", new DeclarationInt(Expression.Int(0), Expression.Int(numStates - 1)));
			d.setStart(Expression.Int(0));
			m.addDeclaration(d);
			modulesFile.addModule(m);
			modulesFile.tidyUp();
		}

		// Get variable info from ModulesFile
		varList = modulesFile.createVarList();
		numVars = varList.getNumVars();

		// For an mdp, compute the max number of choices in a state
		if (modelType == ModelType.MDP)
			maxNumChoices = ((MDPSimple) modelExpl).getMaxNumChoices();

		// Allocate dd variables
		allocateDDVars();
		sortDDVars();

		// Construct transition matrix from file
		buildTrans(modelExpl);

		// Get rid of any nondet dd variables not needed
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

		// Calculate BDD for initial state
		buildInit();

		// Compute state rewards
		computeStateRewards();

		int numModules = 1; // just one module
		String moduleNames[] = modulesFile.getModuleNames(); // whose name is stored here
		Values constantValues = new Values(); // no constants

		/*JDDNode stateRewardsArray[] = new JDDNode[1];
		stateRewardsArray[0] = stateRewards;
		JDDNode transRewardsArray[] = new JDDNode[1];
		transRewardsArray[0] = transRewards;
		String rewardStructNames[] = new String[1];
		rewardStructNames[0] = "";*/
		JDDNode stateRewardsArray[] = new JDDNode[0];
		JDDNode transRewardsArray[] = new JDDNode[0];
		String rewardStructNames[] = new String[0];

		// Create new Model object to be returned
		if (modelType == ModelType.DTMC) {
			model = new ProbModel(trans, start, stateRewardsArray, transRewardsArray, rewardStructNames, allDDRowVars, allDDColVars, ddVarNames, numModules,
					moduleNames, moduleDDRowVars, moduleDDColVars, numVars, varList, varDDRowVars, varDDColVars, constantValues);
		} else if (modelType == ModelType.MDP) {
			model = new NondetModel(trans, start, stateRewardsArray, transRewardsArray, rewardStructNames, allDDRowVars, allDDColVars, allDDSynchVars,
					allDDSchedVars, allDDChoiceVars, allDDNondetVars, ddVarNames, numModules, moduleNames, moduleDDRowVars, moduleDDColVars, numVars, varList,
					varDDRowVars, varDDColVars, constantValues);
		} else if (modelType == ModelType.CTMC) {
			model = new StochModel(trans, start, stateRewardsArray, transRewardsArray, rewardStructNames, allDDRowVars, allDDColVars, ddVarNames, numModules,
					moduleNames, moduleDDRowVars, moduleDDColVars, numVars, varList, varDDRowVars, varDDColVars, constantValues);
		}
		// Set action info
		// TODO: disable if not required?
		model.setSynchs(synchs);
		if (modelType != ModelType.MDP) {
			model.setTransPerAction((JDDNode[]) transPerAction.toArray(new JDDNode[0]));
		} else {
			model.setTransActions(transActions);
		}

		// Do reachability (if required)
		if (doReach) {
			mainLog.print("\nComputing reachable states...\n");
			model.doReachability();
			model.filterReachableStates();
		} else {
			// If not, assume any non-empty row/column is a reachable state
			JDD.Ref(trans);
			tmp = JDD.GreaterThan(trans, 0);
			if (modelType == ModelType.MDP)
				tmp = JDD.ThereExists(tmp, allDDNondetVars);
			JDD.Ref(tmp);
			tmp2 = JDD.ThereExists(tmp, allDDRowVars);
			tmp2 = JDD.SwapVariables(tmp2, allDDColVars, allDDRowVars);
			tmp = JDD.ThereExists(tmp, allDDColVars);
			tmp = JDD.Or(tmp, tmp2);
			model.setReach(tmp);
			model.filterReachableStates();
		}

		// Print some info (if extraddinfo flag on)
		if (prism.getExtraDDInfo()) {
			mainLog.print("Reach: " + JDD.GetNumNodes(model.getReach()) + " nodes\n");
		}

		// Find any deadlocks
		model.findDeadlocks(prism.getFixDeadlocks());

		// Deref spare dds
		if (modelType == ModelType.MDP) {
			for (i = 0; i < ddSynchVars.length; i++) {
				JDD.Deref(ddSynchVars[i]);
			}
			for (i = 0; i < ddSchedVars.length; i++) {
				JDD.Deref(ddSchedVars[i]);
			}
			for (i = 0; i < ddChoiceVars.length; i++) {
				JDD.Deref(ddChoiceVars[i]);
			}
		}

		return model;
	}

	// allocate DD vars for system
	// i.e. decide on variable ordering and request variables from CUDD

	private void allocateDDVars()
	{
		JDDNode v, vr, vc;
		int i, j, n;
		int ddVarsUsed = 0;
		ddVarNames = new Vector<String>();

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
				v = JDD.Var(ddVarsUsed++);
				ddChoiceVars[i] = v;
				ddVarNames.add("l" + i);
			}
		}

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
				vr = JDD.Var(ddVarsUsed++);
				// new dd col variable
				vc = JDD.Var(ddVarsUsed++);
				varDDRowVars[i].addVar(vr);
				varDDColVars[i].addVar(vc);
				// add names to list
				ddVarNames.add(varList.getName(i) + "." + j);
				ddVarNames.add(varList.getName(i) + "'." + j);
			}
		}
	}

	// sort out DD variables and the arrays they are stored in
	// (more than one copy of most variables is stored)

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
			varDDRowVars[i].refAll();
			varDDColVars[i].refAll();
			moduleDDRowVars[0].addVars(varDDRowVars[i]);
			moduleDDColVars[0].addVars(varDDColVars[i]);
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
			varDDRowVars[i].refAll();
			varDDColVars[i].refAll();
			allDDRowVars.addVars(varDDRowVars[i]);
			allDDColVars.addVars(varDDColVars[i]);
		}
		if (modelType == ModelType.MDP) {
			for (i = 0; i < ddChoiceVars.length; i++) {
				// add to list
				JDD.Ref(ddChoiceVars[i]);
				JDD.Ref(ddChoiceVars[i]);
				allDDChoiceVars.addVar(ddChoiceVars[i]);
				allDDNondetVars.addVar(ddChoiceVars[i]);
			}
		}
	}

	/**
	 * Construct (symbolic) transition matrix from explicit-state model. 
	 */
	private void buildTrans(explicit.Model modelExpl) throws PrismException
	{
		String a;
		int j, r, c, k = 0;
		int nnz, count;
		double d;
		JDDNode elem, tmp;
		ProgressDisplay progress;

		mainLog.print("Converting to MTBDD: ");

		// Initialise action list
		synchs = new Vector<String>();
		// Initialise mtbdds
		trans = JDD.Constant(0);
		transRewards = JDD.Constant(0);
		if (modelType != ModelType.MDP) {
			transPerAction = new Vector<JDDNode>();
			transPerAction.add(JDD.Constant(0));
		} else {
			transActions = JDD.Constant(0);
		}
		// Initialise transition counter/progress display
		nnz = modelExpl.getNumTransitions();
		count = 0;
		progress = new ProgressDisplay(mainLog);
		progress.setTotalCount(nnz);
		progress.start();
		// Case for DTMCs/CTMCs...
		if (modelType == ModelType.DTMC || modelType == ModelType.CTMC) {
			DTMC dtmc = (DTMC) modelExpl;
			for (r = 0; r < numStates; r++) {
				Iterator<Map.Entry<Integer, Double>> iter = dtmc.getTransitionsIterator(r);
				while (iter.hasNext()) {
					Map.Entry<Integer, Double> e = iter.next();
					c = (Integer) e.getKey();
					d = (Double) e.getValue();
					a = "";
					// construct element of matrix mtbdd
					elem = encodeStatePair(r, c);
					// add it into mtbdds for transition matrix and transition rewards
					JDD.Ref(elem);
					trans = JDD.Apply(JDD.PLUS, trans, JDD.Apply(JDD.TIMES, JDD.Constant(d), elem));
					// look up action name
					if (!("".equals(a))) {
						j = synchs.indexOf(a);
						// add to list if first time seen 
						if (j == -1) {
							synchs.add(a);
							j = synchs.size() - 1;
						}
						j++;
					} else {
						j = 0;
					}
					// get (or create) dd for action j
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
					progress.updateIfReady(++count);
				}
			}
		}
		// Case for MDPs...
		else if (modelType == ModelType.MDP) {
			MDP mdp = (MDP) modelExpl;
			for (r = 0; r < numStates; r++) {
				int nc = mdp.getNumChoices(r);
				for (k = 0; k < nc; k++) {
					Iterator<Map.Entry<Integer, Double>> iter = mdp.getTransitionsIterator(r, k);
					while (iter.hasNext()) {
						Map.Entry<Integer, Double> e = iter.next();
						c = (Integer) e.getKey();
						d = (Double) e.getValue();
						a = "";
						// construct element of matrix mtbdd
						elem = JDD.SetVectorElement(JDD.Constant(0), allDDChoiceVars, k, 1.0);
						elem = JDD.Apply(JDD.TIMES, elem, encodeStatePair(r, c));
						// add it into mtbdds for transition matrix and transition rewards
						JDD.Ref(elem);
						trans = JDD.Apply(JDD.PLUS, trans, JDD.Apply(JDD.TIMES, JDD.Constant(d), elem));
						// look up action name
						if (!("".equals(a))) {
							j = synchs.indexOf(a);
							// add to list if first time seen 
							if (j == -1) {
								synchs.add(a);
								j = synchs.size() - 1;
							}
							j++;
						} else {
							j = 0;
						}
						JDD.Ref(elem);
						tmp = JDD.ThereExists(elem, allDDColVars);
						// use max here because we see multiple transitions for a single choice
						transActions = JDD.Apply(JDD.MAX, transActions, JDD.Apply(JDD.TIMES, JDD.Constant(j), tmp));
						// deref element dd
						JDD.Deref(elem);
						// Print some progress info occasionally
						progress.updateIfReady(++count);
					}
				}
			}
		} else {
			throw new PrismException("Unknown model type");
		}
		// Tidy up progress display
		progress.update(nnz);
		progress.end();
	}

	// Build BDD for initial state(s)

	private void buildInit() throws PrismException
	{
		start = JDD.Constant(0);
		for (int r : modelExpl.getInitialStates()) {
			start = JDD.Or(start, encodeState(r));
		}
	}

	// read info about state rewards from states file

	public void computeStateRewards() throws PrismException
	{
		/*BufferedReader in;
		String s, ss[];
		int i, j, lineNum = 0;
		double d;
		JDDNode tmp;

		// initialise mtbdd
		stateRewards = JDD.Constant(0);

		if (statesFile == null)
			return;

		try {
			// open file for reading
			in = new BufferedReader(new FileReader(statesFile));
			// skip first line
			in.readLine();
			lineNum = 1;
			// read remaining lines
			s = in.readLine();
			lineNum++;
			while (s != null) {
				// skip blank lines
				s = s.trim();
				if (s.length() > 0) {
					// split into two/three parts
					ss = s.split(":");
					// determine which state this line describes
					i = Integer.parseInt(ss[0]);
					// if there is a state reward...
					ss = ss[1].split("=");
					if (ss.length == 2) {
						// determine value
						d = Double.parseDouble(ss[1]);
						// construct element of vector mtbdd
						tmp = encodeState(i);
						// add it into mtbdd for state rewards
						stateRewards = JDD.Apply(JDD.PLUS, stateRewards, JDD.Apply(JDD.TIMES, JDD.Constant(d), tmp));
					}
				}
				// read next line
				s = in.readLine();
				lineNum++;
			}
			// close file
			in.close();
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + statesFile + "\"");
		} catch (NumberFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of states file \"" + statesFile + "\"");
		}*/
	}

	/**
	 * Encode a state index into BDD vars (referencing the result).
	 * @param s State index
	 */
	private JDDNode encodeState(int s)
	{
		JDDNode res;
		int i, j = 0;
		// Case where there is no state list...
		if (statesList == null) {
			res = JDD.SetVectorElement(JDD.Constant(0), varDDRowVars[0], s, 1.0);
		}
		// Case where there is a state list...
		else {
			res = JDD.Constant(1);
			for (i = 0; i < numVars; i++) {
				try {
					j = varList.encodeToInt(i, statesList.get(s).varValues[i]);
				} catch (PrismLangException e) {
					// Won't happen
				}
				res = JDD.Apply(JDD.TIMES, res, JDD.SetVectorElement(JDD.Constant(0), varDDRowVars[i], j, 1.0));
			}
		}
		return res;
	}

	/**
	 * Encode a state index pair into BDD vars (referencing the result).
	 * @param r State row index
	 * @param c State column index
	 */
	private JDDNode encodeStatePair(int r, int c)
	{
		JDDNode res;
		int i, j = 0, k = 0;

		// Note: could do this with a conjunction of BDDs from two
		// calls to encodeState but this way should be more efficient 

		// Case where there is no state list...
		if (statesList == null) {
			res = JDD.SetMatrixElement(JDD.Constant(0), varDDRowVars[0], varDDColVars[0], r, c, 1.0);
		}
		// Case where there is a state list...
		else {
			res = JDD.Constant(1);
			for (i = 0; i < numVars; i++) {
				try {
					j = varList.encodeToInt(i, statesList.get(r).varValues[i]);
					k = varList.encodeToInt(i, statesList.get(c).varValues[i]);
				} catch (PrismLangException e) {
					// Won't happen
				}
				res = JDD.Apply(JDD.TIMES, res, JDD.SetVectorElement(JDD.Constant(0), varDDRowVars[i], j, 1.0));
				res = JDD.Apply(JDD.TIMES, res, JDD.SetVectorElement(JDD.Constant(0), varDDColVars[i], k, 1.0));
			}
		}
		return res;
	}
}

//------------------------------------------------------------------------------
