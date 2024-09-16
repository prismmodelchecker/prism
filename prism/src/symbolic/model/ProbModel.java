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
import parser.Values;
import parser.VarList;
import parser.ast.Declaration;
import parser.ast.DeclarationInt;
import parser.ast.Expression;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;
import sparse.PrismSparse;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

/**
 * Class for symbolic (BDD-based) representation of a DTMC.
 */
public class ProbModel extends ModelSymbolic
{
	/** Optionally, a separate transition matrix MTBDD for each action label */
	protected JDDNode transPerAction[];

	// Constructor

	public ProbModel(JDDNode trans, JDDNode start, JDDVars allDDRowVars, JDDVars allDDColVars, ModelVariablesDD modelVariables,
					 VarList varList, JDDVars[] varDDRowVars, JDDVars[] varDDColVars)
	{
		super(trans, start, allDDRowVars, allDDColVars, modelVariables, varList, varDDRowVars, varDDColVars);
	}

	// Mutators

	/**
	 * Set the transition matrix MTBDDs for each action label
	 */
	public void setTransPerAction(JDDNode[] transPerAction)
	{
		this.transPerAction = transPerAction;
	}

	@Override
	public void filterReachableStates()
	{
		super.filterReachableStates();
		// Action label index info
		if (transPerAction != null) {
			// transPerAction stored as matrix so filter both rows/cols
			for (int i = 0; i < numSynchs + 1; i++) {
				JDD.Ref(reach);
				transPerAction[i] = JDD.Apply(JDD.TIMES, reach, transPerAction[i]);
				JDD.Ref(reach);
				JDDNode tmp = JDD.PermuteVariables(reach, allDDRowVars, allDDColVars);
				transPerAction[i] = JDD.Apply(JDD.TIMES, tmp, transPerAction[i]);
			}
		}
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

	// Accessors (for Model)

	@Override
	public ModelType getModelType()
	{
		return ModelType.DTMC;
	}

	@Override
	public void printTransInfo(PrismLog log, boolean extra)
	{
		super.printTransInfo(log, extra);
		if (extra) {
			if (transPerAction != null) {
				for (int i = 0; i < numSynchs + 1; i++) {
					log.print("Action label info (");
					log.print((i == 0 ? "" : synchs.get(i - 1)) + "): ");
					log.println(JDD.GetInfoString(transPerAction[i], getNumDDVarsInTrans()));
				}
			}
		}
	}

	@Override
	public void exportToFile(int exportType, boolean explicit, File file, int precision) throws FileNotFoundException, PrismException
	{
		if (!explicit) {
			PrismMTBDD.ExportMatrix(trans, getTransSymbol(), allDDRowVars, allDDColVars, odd, exportType, (file != null) ? file.getPath() : null, precision, null, true);
		} else {
			PrismSparse.ExportMatrix(trans, getTransSymbol(), allDDRowVars, allDDColVars, odd, exportType, (file != null) ? file.getPath() : null, precision, null, true);
		}
	}

	@Override
	public void exportTransRewardsToFile(int r, int exportType, boolean ordered, File file, int precision, boolean noexportheaders) throws FileNotFoundException, PrismException
	{
		if (!ordered) {
			PrismMTBDD.ExportMatrix(transRewards[r], "C" + (r + 1), allDDRowVars, allDDColVars, odd, exportType, (file == null) ? null : file.getPath(), precision, rewardStructNames[r], noexportheaders);
		} else {
			PrismSparse.ExportMatrix(transRewards[r], "C" + (r + 1), allDDRowVars, allDDColVars, odd, exportType, (file == null) ? null : file.getPath(), precision, rewardStructNames[r], noexportheaders);
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
		return "P";
	}

	@Override
	public void clear()
	{
		super.clear();
		if (transPerAction != null) {
			for (int i = 0; i < numSynchs + 1; i++) {
				JDD.Deref(transPerAction[i]);
			}
		}
	}

	// Accessors (for DTMCs)

	/**
	 * Get the transition matrix MTBDDs for each action label
	 */
	public JDDNode[] getTransPerAction()
	{
		return transPerAction;
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
		while (varList.exists(extraVar)) {
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
			if (before) {
				newVarList.addVarAtStart(decl, 1);
			} else {
				newVarList.addVar(decl, 1);
			}
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
				// New list of all row/col vars
				newAllDDRowVars, newAllDDColVars,
				// New model variables
				newModelVariables,
				// New var info
				newVarList, newVarDDRowVars, newVarDDColVars);
		// New reward information
		result.setRewards(newStateRewards, newTransRewards, this.rewardStructNames.clone());
		// Constants (no change)
		result.setConstantValues(this.getConstantValues());

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
		for (Map.Entry<String, JDDNode> entry : labelsDD.entrySet()) {
			JDDNode labelStates = entry.getValue();
			JDDNode transformedLabelStates = transformation.getTransformedLabelStates(labelStates, result.getReach());
			result.labelsDD.put(entry.getKey(), transformedLabelStates);
		}

		extraDDRowVars.derefAll();
		extraDDColVars.derefAll();

		return result;
	}
}
