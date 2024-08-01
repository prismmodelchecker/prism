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

package symbolic.model;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import jdd.SanityJDD;
import mtbdd.PrismMTBDD;
import parser.Values;
import parser.VarList;
import parser.ast.Declaration;
import parser.ast.DeclarationInt;
import parser.ast.Expression;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismUtils;
import sparse.PrismSparse;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map.Entry;

/**
 * Class for symbolic (BDD-based) representation of an MDP.
 */
public class NondetModel extends ModelSymbolic
{
	// Extra (MT)BDDs and variables

	/** BDD (over row and nondet variables) representing a "mask"
	 * for the nondeterministic choices, with a 0 where there is a
	 * choice present, and a 1 where there is none.
	 * Used for minimising over nondeterminism. */
	protected JDDNode nondetMask;
	/** DD variables used to represent nondeterminism */
	protected JDDVars allDDNondetVars;
	/** BDD (over row and nondet variables) for choices that not action-labelled */
	protected JDDNode transInd;
	/** BDD (over row and nondet variables) for choices labelled with each action */
	protected JDDNode transSynch[];
	/** BDD for the transition relation (just state pairs) */
	protected JDDNode transReln;
	/** MTBDDs (over row and nondet DD variables) used to store the action
	 * for each choice, as a 1-indexed action index for each state-choice pair. */
	protected JDDNode transActions;

	// Extra statistics

	/** Total number of choices */
	protected double numChoices;

	// Constructor

	public NondetModel(JDDNode trans, JDDNode start, JDDVars allDDRowVars, JDDVars allDDColVars, JDDVars allDDNondetVars, ModelVariablesDD modelVariables,
					 VarList varList, JDDVars[] varDDRowVars, JDDVars[] varDDColVars)
	{
		super(trans, start, allDDRowVars, allDDColVars, modelVariables, varList, varDDRowVars, varDDColVars);
		this.allDDNondetVars = allDDNondetVars;
	}

	// Mutators

	/**
	 * Set the MTBDDs (over row and nondet DD variables) used to store the action
	 * for each choice, as a 1-indexed action index for each state-choice pair.
	 */
	public void setTransActions(JDDNode transActions)
	{
		this.transActions = transActions;
	}

	/**
	 * Set the BDD (over row and nondet variables) for choices that not action-labelled
	 */
	public void setTransInd(JDDNode transInd)
	{
		this.transInd = transInd;
	}

	/**
	 * Set the BDDs (over row and nondet variables) for choices labelled with each action
	 */
	public void setTransSynch(JDDNode[] transSynch)
	{
		this.transSynch = transSynch;
	}

	@Override
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

	@Override
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

	@Override
	public void filterReachableStates()
	{
		// remove non-reachable states from various dds
		// (and calculate num transitions, etc.)
		// (and build mask)

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
		// Action label index info
		if (transActions != null) {
			// transActions just stored per state so only filter rows
			JDD.Ref(reach);
			transActions = JDD.Apply(JDD.TIMES, reach, transActions);
		}
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

	// Accessors (for Model)

	@Override
	public ModelType getModelType()
	{
		return ModelType.MDP;
	}

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

	@Override
	public int getNumDDVarsInTrans()
	{
		return allDDRowVars.n() * 2 + allDDNondetVars.n();
	}

	@Override
	public void printTransInfo(PrismLog log, boolean extra)
	{
		super.printTransInfo(log, extra);
		if (extra) {
			if (transActions != null) {
				log.print("Action label info (");
				log.println(JDD.GetInfoString(transActions, getNumDDVarsInTrans()));
			}
		}
	}

	@Override
	public void exportToFile(int exportType, boolean explicit, File file, int precision) throws FileNotFoundException, PrismException
	{
		if (!explicit) {
			// can only do explicit (sparse matrix based) export for mdps
		} else {
			PrismSparse.ExportMDP(trans, transActions, getSynchs(), getTransSymbol(), allDDRowVars, allDDColVars, allDDNondetVars, odd, exportType,
					(file != null) ? file.getPath() : null, precision);
		}
	}

	@Override
	public void exportTransRewardsToFile(int r, int exportType, boolean ordered, File file, int precision, boolean noexportheaders) throws FileNotFoundException, PrismException
	{
		if (!ordered) {
			// can only do explicit (sparse matrix based) export for mdps
		} else {
			PrismSparse.ExportSubMDP(trans, transRewards[r], "C" + (r + 1), allDDRowVars, allDDColVars, allDDNondetVars, odd, exportType, (file == null) ? null : file.getPath(), precision, rewardStructNames[r], noexportheaders);
		}
	}

	@Override
	public String getTransName()
	{
		return "Transition matrix";
	}

	@Override
	public String getTransSymbol()
	{
		return "S";
	}

	@Override
	public void clear()
	{
		super.clear();
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
		if (transActions != null) {
			JDD.Deref(transActions);
		}
	}

	// Accessors (for MDPs)

	/**
	 * Get the total number of choices.
	 */
	public long getNumChoices()
	{
		return (numChoices > Long.MAX_VALUE) ? -1 : Math.round(numChoices);
	}

	/**
	 * Get the total number of choices as a string.
	 */
	public String getNumChoicesString()
	{
		return PrismUtils.bigIntToString(numChoices);
	}

	/**
	 * Get a BDD (over row and nondet variables) representing a "mask"
	 * for the nondeterministic choices, with a 0 where there is a
	 * choice present, and a 1 where there is none.
	 * Used for minimising over nondeterminism.
	 */
	public JDDNode getNondetMask()
	{
		return nondetMask;
	}

	/**
	 * Get (all) the DD variables used to represent nondeterminism.
	 */
	public JDDVars getAllDDNondetVars()
	{
		return allDDNondetVars;
	}

	/**
	 * Get the total number of DD variables used to represent nondeterminism.
	 */
	public int getNumDDNondetVars()
	{
		return allDDNondetVars.n();
	}

	/**
	 * Get the BDD (over row and nondet variables) for choices that not action-labelled
	 */
	public JDDNode getTransInd()
	{
		return transInd;
	}

	/**
	 * Set the BDDs (over row and nondet variables) for choices labelled with each action
	 */
	public JDDNode[] getTransSynch()
	{
		return transSynch;
	}

	/**
	 * Get the MTBDDs (over row and nondet DD variables) used to store the action
	 * for each choice, as a 1-indexed action index for each state-choice pair.
	 */
	public JDDNode getTransActions()
	{
		return transActions;
	}

	/**
	 * Do all choices in each state have a unique action label?
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
		JDDVars newAllDDNondetVars;
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
		while (varList.exists(extraStateVar)) {
			extraStateVar = "_" + extraStateVar;
		}

		// Create a (new, unique) name for the variable that will represent extra actions
		extraActionVar = transformation.getExtraActionVariableName();
		while (varList.exists(extraActionVar)) {
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

		// Generate new newAllDDNondetVars
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
			if (before) {
				newVarList.addVarAtStart(decl, 1);
			} else {
				newVarList.addVar(decl, 1);
			}
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
				// New list of all row/col vars
				newAllDDRowVars, newAllDDColVars,
				// Nondet variables (unchanged)
				newAllDDNondetVars,
				// New model variables
				newModelVariables,
				// New var info
				newVarList, newVarDDRowVars, newVarDDColVars);
		// New reward information
		result.setRewards(newStateRewards, newTransRewards, this.rewardStructNames.clone());
		// Constants (no change)
		result.setConstantValues(this.getConstantValues());

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

