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
import java.util.ArrayList;
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
 * Class for MTBDD-based storage of a PRISM model that is an MDP.
 */
public class NondetModel extends ProbModel
{
	// Extra info
	protected double numChoices; // number of choices

	// Extra dd stuff
	protected JDDNode nondetMask; // mask for nondeterministic choices
	protected JDDVars allDDSynchVars; // synch actions dd vars
	protected JDDVars allDDSchedVars; // scheduler dd vars
	protected JDDVars allDDChoiceVars; // local nondet choice dd vars
	protected JDDVars allDDNondetVars; // all nondet dd vars (union of two above)
	protected JDDNode transInd; // BDD for independent part of trans
	protected JDDNode transSynch[]; // BDD for parts of trans from each action
	protected JDDNode transReln; // BDD for the transition relation (no action encoding)

	// accessor methods

	@Override
	public JDDNode getTransReln()
	{
		// First, compute the transition relation if it is not there
		if (transReln == null) {
			JDD.Ref(trans01);
			transReln = JDD.ThereExists(trans01, allDDNondetVars);
		}
		return transReln;
	}

	// type
	public ModelType getModelType()
	{
		return ModelType.MDP;
	}

	public long getNumChoices()
	{
		return (numChoices > Long.MAX_VALUE) ? -1 : Math.round(numChoices);
	}

	public String getNumChoicesString()
	{
		return PrismUtils.bigIntToString(numChoices);
	}

	public JDDNode getNondetMask()
	{
		return nondetMask;
	}

	public JDDVars getAllDDSynchVars()
	{
		return allDDSynchVars;
	}

	public JDDVars getAllDDSchedVars()
	{
		return allDDSchedVars;
	}

	public JDDVars getAllDDChoiceVars()
	{
		return allDDChoiceVars;
	}

	public JDDVars getAllDDNondetVars()
	{
		return allDDNondetVars;
	}

	public JDDNode getTransInd()
	{
		return transInd;
	}

	public JDDNode[] getTransSynch()
	{
		return transSynch;
	}

	// additional useful methods to do with dd vars
	public int getNumDDNondetVars()
	{
		return allDDNondetVars.n();
	}

	public int getNumDDVarsInTrans()
	{
		return allDDRowVars.n() * 2 + allDDNondetVars.n();
	}

	public String getTransName()
	{
		return "Transition matrix";
	}

	public String getTransSymbol()
	{
		return "S";
	}

	/**
	 * Do all choices in in each state have a unique action label?
	 */
	public boolean areAllChoiceActionsUnique()
	{
		// Action labels
		for (int i = 0; i < numSynchs; i++) {
			JDD.Ref(transActions);
			JDDNode tmp = JDD.Equals(transActions, i + 1);
			tmp = JDD.SumAbstract(tmp, allDDNondetVars);
			double max = JDD.FindMax(tmp);
			JDD.Deref(tmp);
			if (max > 1)
				return false;
		}
		// Unlabelled choices
		JDD.Ref(reach);
		JDD.Ref(transActions);
		JDD.Ref(nondetMask);
		JDDNode tmp = JDD.And(reach, JDD.And(JDD.LessThanEquals(transActions, 0), JDD.Not(nondetMask)));
		tmp = JDD.SumAbstract(tmp, allDDNondetVars);
		double max = JDD.FindMax(tmp); 
		JDD.Deref(tmp);
		if (max > 1)
			return false;
		
		return true;
	}
	
	// set methods for things not set up in constructor

	public void setTransInd(JDDNode transInd)
	{
		this.transInd = transInd;
	}

	public void setTransSynch(JDDNode[] transSynch)
	{
		this.transSynch = transSynch;
	}

	// constructor

	public NondetModel(JDDNode tr, JDDNode s, JDDNode sr[], JDDNode trr[], String rsn[], JDDVars arv, JDDVars acv, JDDVars asyv, JDDVars asv, JDDVars achv,
			JDDVars andv, ModelVariablesDD mvdd, int nm, String[] mn, JDDVars[] mrv, JDDVars[] mcv, int nv, VarList vl, JDDVars[] vrv, JDDVars[] vcv, Values cv)
	{
		super(tr, s, sr, trr, rsn, arv, acv, mvdd, nm, mn, mrv, mcv, nv, vl, vrv, vcv, cv);

		allDDSynchVars = asyv;
		allDDSchedVars = asv;
		allDDChoiceVars = achv;
		allDDNondetVars = andv;

		transInd = null;
		transSynch = null;
		transReln = null;
	}

	// do reachability

	public void doReachability() throws PrismException
	{
		JDDNode tmp;

		// remove any nondeterminism
		JDD.Ref(trans01);
		tmp = JDD.MaxAbstract(trans01, allDDNondetVars);

		// compute reachable states
		JDDNode reachable = PrismMTBDD.Reachability(tmp, allDDRowVars, allDDColVars, start);
		JDD.Deref(tmp);

		// set the reachable states, compute numStates, create the ODD, etc
		setReach(reachable);
	}

	/**
	 * Compute and store the set of reachable states,
	 * where the parameter {@seed} provides an initial set of states
	 * known to be reachable.
	 * <br/>
	 * Starts reachability computation from the union of {@code seed} and {@start}.
	 * <br/>[ REFS: <i>result</i>, DEREFS: seed ]
	 * @param seed set of states (over ddRowVars) that is known to be reachable
	 */
	public void doReachability(JDDNode seed) throws PrismException
	{
		JDDNode tmp;

		// do sanity check on seed if checking is enabled
		if (SanityJDD.enabled)
			SanityJDD.checkIsStateSet(seed, getAllDDRowVars());

		// remove any nondeterminism from the 0/1-transition function
		JDD.Ref(trans01);
		tmp = JDD.MaxAbstract(trans01, allDDNondetVars);

		// S = union of initial states and seed. seed is dereferenced here.
		JDDNode S = JDD.Or(start.copy(), seed);

		// compute reachable states
		JDDNode reachable = PrismMTBDD.Reachability(tmp, allDDRowVars, allDDColVars, S);
		JDD.Deref(tmp);
		JDD.Deref(S);

		// set the reachable states, compute numStates, create the ODD, etc
		setReach(reachable);
	}

	// remove non-reachable states from various dds
	// (and calculate num transitions, etc.)
	// (and build mask)

	public void filterReachableStates()
	{
		super.filterReachableStates();

		// also filter transInd/tranSynch DDs (if necessary)
		if (transInd != null) {
			JDD.Ref(reach);
			transInd = JDD.Apply(JDD.TIMES, reach, transInd);
			for (int i = 0; i < numSynchs; i++) {
				JDD.Ref(reach);
				transSynch[i] = JDD.Apply(JDD.TIMES, reach, transSynch[i]);
			}
		}
		// also filter transReln DD (if necessary)
		if (transReln != null) {
			JDD.Ref(reach);
			transReln = JDD.Apply(JDD.TIMES, reach, transReln);
		}
		
		// build mask for nondeterminstic choices
		// (and work out number of choices)
		JDD.Ref(trans01);
		JDD.Ref(reach);
		if (this.nondetMask != null)
			JDD.Deref(this.nondetMask);
		// nb: this assumes that there are no deadlock states
		nondetMask = JDD.And(JDD.ThereExists(trans01, allDDColVars), reach);
		numChoices = JDD.GetNumMinterms(nondetMask, getNumDDRowVars() + getNumDDNondetVars());
		JDD.Ref(reach);
		nondetMask = JDD.And(JDD.Not(nondetMask), reach);
	}

	@Override
	public void findDeadlocks(boolean fix)
	{
		// find states with at least one transition
		JDD.Ref(trans01);
		deadlocks = JDD.ThereExists(trans01, allDDColVars);
		deadlocks = JDD.ThereExists(deadlocks, allDDNondetVars);

		// find reachable states with no transitions
		JDD.Ref(reach);
		deadlocks = JDD.And(reach, JDD.Not(deadlocks));
		
		if (fix && !deadlocks.equals(JDD.ZERO)) {
			// remove deadlocks by adding self-loops to trans
			// (also update transInd (if necessary) at same time)
			// (note: we don't need to update transActions since
			//  action-less transitions are encoded as 0 anyway)
			// (note: would need to update transPerAction[0]
			//  but this is not stored for MDPs)
			JDDNode tmp;
			JDD.Ref(deadlocks);
			tmp = JDD.SetVectorElement(JDD.Constant(0), allDDNondetVars, 0, 1);
			tmp = JDD.And(tmp, JDD.Identity(allDDRowVars, allDDColVars));
			tmp = JDD.And(deadlocks, tmp);
			JDD.Ref(tmp);
			trans = JDD.Apply(JDD.PLUS, trans, tmp);
			JDD.Ref(tmp);
			trans01 = JDD.Apply(JDD.PLUS, trans01, tmp);
			if (transInd != null) {
				JDD.Ref(tmp);
				transInd = JDD.Or(transInd, JDD.ThereExists(tmp, allDDColVars));
			}
			JDD.Deref(tmp);
			// recompute transReln (if needed)
			if (transReln != null) {
				JDD.Deref(transReln);
				JDD.Ref(trans01);
				transReln = JDD.ThereExists(trans01, allDDNondetVars);
			}
			// update transition count
			numTransitions = JDD.GetNumMinterms(trans01, getNumDDVarsInTrans());
			// re-build mask for nondeterminstic choices
			// (and work out number of choices)
			JDD.Ref(trans01);
			JDD.Ref(reach);
			if (this.nondetMask != null)
				JDD.Deref(this.nondetMask);
			nondetMask = JDD.And(JDD.ThereExists(trans01, allDDColVars), reach);
			numChoices = JDD.GetNumMinterms(nondetMask, getNumDDRowVars() + getNumDDNondetVars());
			JDD.Ref(reach);
			nondetMask = JDD.And(JDD.Not(nondetMask), reach);
		}
	}

	public void printTransInfo(PrismLog log, boolean extra)
	{
		int i, j, n;

		log.print("States:      " + getNumStatesString() + " (" + getNumStartStatesString() + " initial)" + "\n");
		log.print("Transitions: " + getNumTransitionsString() + "\n");
		log.print("Choices:     " + getNumChoicesString() + "\n");

		log.println();

		log.print(getTransName() + ": " + JDD.GetInfoString(trans, getNumDDVarsInTrans()));
		log.print(", vars: " + getNumDDRowVars() + "r/" + getNumDDColVars() + "c/" + getNumDDNondetVars() + "nd\n");
		if (extra) {
			log.print("DD vars (nd):");
			n = allDDNondetVars.getNumVars();
			for (i = 0; i < n; i++) {
				j = allDDNondetVars.getVarIndex(i);
				log.print(" " + j + ":" + getDDVarNames().get(j));
			}
			log.println();
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
			log.print("Mask: " + JDD.GetNumNodes(nondetMask) + " nodes, ");
			log.print(JDD.GetNumMintermsString(nondetMask, getNumDDRowVars() + getNumDDNondetVars()) + " minterms\n");

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
			if (transActions != null && !transActions.equals(JDD.ZERO)) {
				log.print("Action label indices: ");
				log.print(JDD.GetNumNodes(transActions) + " nodes (");
				log.print(JDD.GetNumTerminals(transActions) + " terminal)\n");
			}
			// Don't need to print info for transPerAction (not stored for MDPs)
		}
	}

	// export transition matrix to a file

	public void exportToFile(int exportType, boolean explicit, File file) throws FileNotFoundException, PrismException
	{
		if (!explicit) {
			// can only do explicit (sparse matrix based) export for mdps
		} else {
			PrismSparse.ExportMDP(trans, transActions, getSynchs(), getTransSymbol(), allDDRowVars, allDDColVars, allDDNondetVars, odd, exportType,
					(file != null) ? file.getPath() : null);
		}
	}

	@Override
	public void exportTransRewardsToFile(int r, int exportType, boolean ordered, File file) throws FileNotFoundException, PrismException
	{
		if (!ordered) {
			// can only do explicit (sparse matrix based) export for mdps
		} else {
			PrismSparse.ExportSubMDP(trans, transRewards[r], "C" + (r + 1), allDDRowVars, allDDColVars, allDDNondetVars, odd, exportType, (file == null) ? null : file.getPath());
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
				// can only do explicit (sparse matrix based) export for mdps
			} else {
				PrismSparse.ExportSubMDP(trans, transRewards[i], "C" + (i + 1), allDDRowVars, allDDColVars, allDDNondetVars, odd, exportType, filename);
			}
		}
		return (allFilenames.length() > 0) ? allFilenames : null;
	}

	// clear up (deref all dds, dd vars)

	public void clear()
	{
		super.clear();
		allDDSynchVars.derefAll();
		allDDSchedVars.derefAll();
		allDDChoiceVars.derefAll();
		allDDNondetVars.derefAll();
		JDD.Deref(nondetMask);
		if (transInd != null)
			JDD.Deref(transInd);
		if (transSynch != null)
			for (int i = 0; i < numSynchs; i++) {
				JDD.Deref(transSynch[i]);
			}
		if (transReln != null)
			JDD.Deref(transReln);
	}

	/**
	 * Apply the given model transformation operator to this model
	 * and return the resulting, transformed model.
	 * @param transformation the information about the transformation
	 * @return the transformed model (needs to be cleared after use)
	 */
	public NondetModel getTransformed(NondetModelTransformationOperator transformation) throws PrismException
	{
		// New (transformed) model - dds, vars, etc.
		JDDNode newTrans, newStart;
		JDDVars newVarDDRowVars[], newVarDDColVars[];
		JDDVars newAllDDRowVars, newAllDDColVars;
		JDDVars newAllDDChoiceVars, newAllDDNondetVars;
		JDDNode newStateRewards[], newTransRewards[];
		ModelVariablesDD newModelVariables;
		VarList newVarList;
		String extraStateVar, extraActionVar;
		// extra variable stuff
		JDDVars extraDDRowVars, extraDDColVars, extraActionVars;
		// Misc
		int i, nStateVars, nActionVars;
		boolean before;

		// Create a (new, unique) name for the variable that will represent extra states
		extraStateVar = transformation.getExtraStateVariableName();
		while (varList.getIndex(extraStateVar) != -1) {
			extraStateVar = "_" + extraStateVar;
		}

		// Create a (new, unique) name for the variable that will represent extra actions
		extraActionVar = transformation.getExtraActionVariableName();
		while (varList.getIndex(extraActionVar) != -1) {
			extraActionVar = "_" + extraActionVar;
		}

		newModelVariables = this.getModelVariables().copy();

		// See how many new dd vars will be needed for extra state variables
		// and whether there is room to put them before rather than after the existing vars
		nStateVars = transformation.getExtraStateVariableCount();
		before = newModelVariables.canPrependExtraStateVariable(nStateVars);

		extraDDRowVars = new JDDVars();
		extraDDColVars = new JDDVars();
		// Create the new dd state variables
		JDDVars extraStateVars = newModelVariables.allocateExtraStateVariable(nStateVars, extraStateVar, before);

		for (i = 0; i < nStateVars; i++) {
			extraDDRowVars.addVar(extraStateVars.getVar(2*i));
			extraDDColVars.addVar(extraStateVars.getVar(2*i+1));
		}

		// notify the transformation about the allocated state variables
		transformation.hookExtraStateVariableAllocation(extraDDRowVars.copy(), extraDDColVars.copy());

		// allocate action vars
		nActionVars = transformation.getExtraActionVariableCount();
		extraActionVars = newModelVariables.allocateExtraActionVariable(nActionVars, extraActionVar);

		// notify the transformation about the allocated action variables
		transformation.hookExtraActionVariableAllocation(extraActionVars.copy());

		// Generate new allDDChoiceVars and newAllDDNondetVars
		newAllDDChoiceVars = new JDDVars();
		newAllDDChoiceVars.copyVarsFrom(extraActionVars);
		newAllDDChoiceVars.copyVarsFrom(this.allDDChoiceVars);

		newAllDDNondetVars = new JDDVars();
		newAllDDNondetVars.copyVarsFrom(extraActionVars);
		newAllDDNondetVars.copyVarsFrom(this.allDDNondetVars);


		// Create/populate new state variable lists
		if (nStateVars == 0) {
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
			Declaration decl = new Declaration(extraStateVar, new DeclarationInt(Expression.Int(0), Expression.Int((1 << nStateVars) - 1)));
			newVarList.addVar(before ? 0 : varList.getNumVars(), decl, 1, this.getConstantValues());
		}

		// Build transition matrix for transformed model
		newTrans = transformation.getTransformedTrans();

		if (SanityJDD.enabled) {
			SanityJDD.checkIsDDOverVars(newTrans, newAllDDRowVars, newAllDDColVars, newAllDDNondetVars);
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
				SanityJDD.checkIsDDOverVars(newTransRewards[i], newAllDDRowVars, newAllDDColVars, newAllDDNondetVars);
			}
		}

		// Create a new model model object to store the product model
		NondetModel result = new NondetModel(
		// New transition matrix/start state
				newTrans, newStart,
				// New reward information
				newStateRewards,
				newTransRewards,
				this.rewardStructNames.clone(),
				// New list of all row/col vars
				newAllDDRowVars, newAllDDColVars,
				// Nondet variables (unchanged)
				this.getAllDDSchedVars().copy(),
				this.getAllDDSynchVars().copy(),
				newAllDDChoiceVars,
				newAllDDNondetVars,
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

		// Set new transActions
		result.setTransActions(transformation.getTransformedTransActions());

		// Also need to copy set of action label strings
		result.setSynchs(new ArrayList<String>(this.getSynchs()));


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
		extraActionVars.derefAll();

		return result;
	}

	/**
	 * Apply the given model transformation, specified as a
	 * ProbModelTransformationOperator, on this model
	 * and return the resulting, transformed model.
	 * <br>
	 * This convenience method does the transformation via a
	 * NondetModelTransformationOperation that is constructed on-the-fly
	 * from the given transformation. For this to work properly,
	 * the getTrans method has to preserve the action information,
	 * i.e., simply augment the state space in some way.
	 * <br>
	 * As an example, a simple product construction with a deterministic
	 * automaton would work fine, as that does not change the structure
	 * of the transitions.
	 *
	 * @param transformation the transformation operator
	 * @return the transformed model (needs to be cleared after use)
	 */
	public NondetModel getTransformed(final ProbModelTransformationOperator transformation) throws PrismException
	{
		NondetModelTransformationOperator ndTransformation = new NondetModelTransformationOperator(this) {
			@Override
			public int getExtraStateVariableCount()
			{
				return transformation.getExtraStateVariableCount();
			}

			@Override
			public int getExtraActionVariableCount()
			{
				// we don't change the nondeterministic choices
				return 0;
			}

			@Override
			public JDDNode getTransformedTrans() throws PrismException
			{
				// pass through
				return transformation.getTransformedTrans();
			}

			@Override
			public JDDNode getTransformedStart() throws PrismException
			{
				// pass through
				return transformation.getTransformedStart();
			}

			@Override
			public String getExtraStateVariableName()
			{
				// pass through
				return transformation.getExtraStateVariableName();
			}

			@Override
			public void hookExtraStateVariableAllocation(JDDVars extraRowVars, JDDVars extraColVars)
			{
				// pass through
				transformation.hookExtraStateVariableAllocation(extraRowVars, extraColVars);
			}

			@Override
			public void hookExtraActionVariableAllocation(JDDVars extraActionVars)
			{
				if (extraActionVars.n() != 0) {
					throw new RuntimeException("NondetModel.getTransformed(ProbModelTransformation) has not requested action variables");
				}
			}

			@Override
			public JDDNode getTransformedStateReward(JDDNode oldReward) throws PrismException
			{
				// pass through
				return transformation.getTransformedStateReward(oldReward);
			}

			@Override
			public JDDNode getTransformedTransReward(JDDNode oldReward) throws PrismException
			{
				// pass through
				return transformation.getTransformedTransReward(oldReward);
			}
		};

		// do transformation with the NondetModelTransformationOperator
		return getTransformed(ndTransformation);
	}

}

//------------------------------------------------------------------------------
