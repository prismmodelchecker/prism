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

package simulator;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import parser.EvaluateContextState;
import parser.State;
import parser.VarList;
import parser.ast.Command;
import parser.ast.Expression;
import parser.ast.Module;
import parser.ast.ModulesFile;
import parser.ast.Update;
import parser.ast.Updates;
import parser.type.TypeClock;
import prism.Evaluator;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismSettings;

public class Updater<Value> extends PrismComponent
{
	// Evaluator for values/states
	public Evaluator<Value> eval;
	
	// Evaluation context for expressions
	protected EvaluateContextState ec;
	
	// Settings:
	// Do we check that probabilities sum to 1?
	protected boolean doProbChecks = true;
	
	// Info on model being explored
	protected ModulesFile modulesFile;
	protected ModelType modelType;
	protected int numModules;
	protected VarList varList;
	// Synchronising action info
	protected List<String> synchs;
	protected int numSynchs;
	protected int synchModuleCounts[];
	// Model info/stats
	protected int numRewardStructs;
	// For real-time models, info about which vars are clocks (bitset over variable indices)
	protected BitSet clockVars;
	
	// Temporary storage:

	// Element i,j of updateLists is a list of the updates from module i labelled with action j
	// (where j=0 denotes independent, otherwise 1-indexed action label)
	protected List<List<List<Updates>>> updateLists;
	// Bit j of enabledSynchs is set iff action j is currently enabled
	// (where j=0 denotes independent, otherwise 1-indexed action label)
	protected BitSet enabledSynchs;
	// Element j of enabledModules is a BitSet showing modules which enable action j
	// (where j=0 denotes independent, otherwise 1-indexed action label)
	protected BitSet enabledModules[];
	// For real-time models, the guards over clock variables attached to (some) Updates
	protected Map<Updates,Expression> clockGuards;

	public Updater(ModulesFile modulesFile, VarList varList, Evaluator<Value> eval)
	{
		this(modulesFile, varList, eval, null);
	}
	
	public Updater(ModulesFile modulesFile, VarList varList, Evaluator<Value> eval, PrismComponent parent)
	{
		// Store some settings
		doProbChecks = parent.getSettings().getBoolean(PrismSettings.PRISM_DO_PROB_CHECKS);
		
		// Get info from model
		this.modulesFile = modulesFile;
		modelType = modulesFile.getModelType();
		numModules = modulesFile.getNumModules();
		synchs = modulesFile.getSynchs();
		numSynchs = synchs.size();
		numRewardStructs = modulesFile.getNumRewardStructs();
		this.varList = varList;

		// Store evaluator
		this.eval = eval;
		
		// Create evaluate context for re-use
		ec = new EvaluateContextState(modulesFile.getConstantValues(), new State(modulesFile.getNumVars()));
		ec.setEvaluationMode(eval.evalMode());
		
		// For real-time models, store info about which vars are clocks
		if (modelType.realTime()) {
			int numVars = varList.getNumVars();
			clockVars = new BitSet();
			for (int v = 0; v < numVars; v++) {
				if (varList.getType(v) instanceof TypeClock) {
					clockVars.set(v);
				}
			}
		}

		// Compute count of number of modules using each synch action
		// First, compute and cache the synch actions for each of the modules
		List<HashSet<String>> synchsPerModule = new ArrayList<HashSet<String>>(numModules);
		for (int i = 0; i < numModules; i++) {
			synchsPerModule.add(new HashSet<String>(modulesFile.getModule(i).getAllSynchs()));
		}
		// Second, do the counting
		synchModuleCounts = new int[numSynchs];
		for (int j = 0; j < numSynchs; j++) {
			synchModuleCounts[j] = 0;
			String s = synchs.get(j);
			for (int i = 0; i < numModules; i++) {
				if (synchsPerModule.get(i).contains(s))
					synchModuleCounts[j]++;
			}
		}

		// Build lists/bitsets for later use
		updateLists = new ArrayList<List<List<Updates>>>(numModules);
		for (int i = 0; i < numModules; i++) {
			updateLists.add(new ArrayList<List<Updates>>(numSynchs + 1));
			for (int j = 0; j < numSynchs + 1; j++) {
				updateLists.get(i).add(new ArrayList<Updates>());
			}
		}
		enabledSynchs = new BitSet(numSynchs + 1);
		enabledModules = new BitSet[numSynchs + 1];
		for (int j = 0; j < numSynchs + 1; j++) {
			enabledModules[j] = new BitSet(numModules);
		}
		clockGuards = new HashMap<Updates, Expression>();
	}

	/**
	 * Determine the set of outgoing transitions from state 'state' and store in 'transitionList'.
	 * @param state State from which to explore
	 * @param transitionList TransitionList object in which to store result
	 */
	public void calculateTransitions(State state, TransitionList<Value> transitionList) throws PrismException
	{
		List<ChoiceListFlexi<Value>> chs;
		int i, j, k, l, n, count;

		// Clear lists/bitsets
		transitionList.clear();
		for (i = 0; i < numModules; i++) {
			for (j = 0; j < numSynchs + 1; j++) {
				updateLists.get(i).get(j).clear();
			}
		}
		enabledSynchs.clear();
		for (i = 0; i < numSynchs + 1; i++) {
			enabledModules[i].clear();
		}
		clockGuards.clear();

		// Calculate the available updates for each module/action
		// (update information in updateLists, clockGuards, enabledSynchs and enabledModules)
		for (i = 0; i < numModules; i++) {
			calculateUpdatesForModule(i, state);
		}
		//System.out.println("updateLists: " + updateLists);

		// Add independent transitions for each (enabled) module to list
		for (i = enabledModules[0].nextSetBit(0); i >= 0; i = enabledModules[0].nextSetBit(i + 1)) {
			for (Updates ups : updateLists.get(i).get(0)) {
				ChoiceListFlexi<Value> ch = processUpdatesAndCreateNewChoice(-(i + 1), ups, state);
				if (ch.size() > 0)
					transitionList.add(ch);
			}
		}
		// Add synchronous transitions to list
		chs = new ArrayList<ChoiceListFlexi<Value>>();
		for (i = enabledSynchs.nextSetBit(1); i >= 0; i = enabledSynchs.nextSetBit(i + 1)) {
			chs.clear();
			// Check counts to see if this action is blocked by some module
			if (enabledModules[i].cardinality() < synchModuleCounts[i - 1])
				continue;
			// If not, proceed...
			for (j = enabledModules[i].nextSetBit(0); j >= 0; j = enabledModules[i].nextSetBit(j + 1)) {
				count = updateLists.get(j).get(i).size();
				// Case where there is only 1 Updates for this module
				if (count == 1) {
					Updates ups = updateLists.get(j).get(i).get(0);
					// Case where this is the first Choice created
					if (chs.size() == 0) {
						ChoiceListFlexi<Value> ch = processUpdatesAndCreateNewChoice(i, ups, state);
						if (ch.size() > 0)
							chs.add(ch);
					}
					// Case where there are existing Choices
					else {
						// Product with all existing choices
						for (ChoiceListFlexi<Value> ch : chs) {
							processUpdatesAndAddToProduct(ups, state, ch);
						}
					}
				}
				// Case where there are multiple Updates (i.e. local nondeterminism)
				else {
					// Case where there are no existing choices
					if (chs.size() == 0) {
						for (Updates ups : updateLists.get(j).get(i)) {
							ChoiceListFlexi<Value> ch = processUpdatesAndCreateNewChoice(i, ups, state);
							if (ch.size() > 0)
								chs.add(ch);
						}
					}
					// Case where there are existing Choices
					else {
						// Duplicate (count-1 copies of) current Choice list
						n = chs.size();
						for (k = 0; k < count - 1; k++)
							for (l = 0; l < n; l++)
								chs.add(new ChoiceListFlexi<Value>(chs.get(l)));
						// Products with existing choices
						for (k = 0; k < count; k++) {
							Updates ups = updateLists.get(j).get(i).get(k);
							for (l = 0; l < n; l++) {
								processUpdatesAndAddToProduct(ups, state, chs.get(k * n + l));
							}
						}
					}
				}
			}
			// Add all new choices to transition list
			for (ChoiceListFlexi<Value> ch : chs) {
				transitionList.add(ch);
			}
		}
		
		// For a DTMC, we need to normalise across all transitions
		// This is partly to handle "local nondeterminism"
		// and also to handle any dubious trickery done by disabling probability checks
		if (modelType == ModelType.DTMC) {
			Value probSum = transitionList.getProbabilitySum();
			transitionList.scaleProbabilitiesBy(eval.divide(eval.one(), probSum));
		}
	
		// Check validity of the computed transitions
		// (not needed currently)
		//transitionList.checkValid(modelType);
		
		// Check for errors (e.g. overflows) in the computed transitions
		//transitionList.checkForErrors(state, varList);
		
		//System.out.println(transitionList);
	}
	
	// Private helpers
	
	/**
	 * Determine the enabled updates for the 'm'th module from (global) state 'state'.
	 * Update information in updateLists, enabledSynchs and enabledModules.
	 * @param m The module index
	 * @param state State from which to explore
	 */
	protected void calculateUpdatesForModule(int m, State state) throws PrismLangException
	{
		Module module = modulesFile.getModule(m);
		int n = module.getNumCommands();
		for (int i = 0; i < n; i++) {
			Command command = module.getCommand(i);
			// See if the guard is satisfied
			boolean guardSat = false;
			Expression clockGuard = null;
			// For real-time models, we only evaluate in terms of non-clock vars, and store any clock guard
			if (modelType.realTime()) {
				State stateNoClocks = new State(state);
				for (int v = clockVars.nextSetBit(0); v >= 0; v = clockVars.nextSetBit(v + 1)) {
					stateNoClocks.varValues[v] = null;
				}
				clockGuard = command.getGuard().deepCopy();
				clockGuard = (Expression) clockGuard.evaluatePartially(ec.setState(stateNoClocks)).simplify();
				if (!Expression.isFalse(clockGuard)) {
					guardSat = true;
				}
			} else {
				guardSat = command.getGuard().evaluateBoolean(ec.setState(state));
			}
			// If the command is enabled, update stored info
			if (guardSat) {
				int j = command.getSynchIndex();
				updateLists.get(m).get(j).add(command.getUpdates());
				enabledSynchs.set(j);
				enabledModules[j].set(m);
				if (modelType.realTime()) {
					clockGuards.put(command.getUpdates(), clockGuard);
				}
			}
		}
	}

	/**
	 * Evaluate the probability (or rate) of the ith update, in the context of a state.
	 * If the probability is not specified in the update, it is assumed to be 1.
	 */
	protected Value getProbabilityInState(Updates ups, int i, State state) throws PrismLangException
	{
		Expression p = ups.getProbability(i);
		if (p == null) {
			return eval.one();
		} else {
			return eval.evaluate(p, state);
		}
	}
	
	/**
	 * Create a new Choice object (currently ChoiceListFlexi) based on an Updates object
	 * and a (global) state. Check for negative probabilities/rates and, if appropriate,
	 * check probabilities sum to 1 too.
	 * @param moduleOrActionIndex Module/action for the choice, encoded as an integer (see Choice)
	 * @param ups The Updates object 
	 * @param state Global state
	 */
	private ChoiceListFlexi<Value> processUpdatesAndCreateNewChoice(int moduleOrActionIndex, Updates ups, State state) throws PrismLangException
	{
		ChoiceListFlexi<Value> ch;
		List<Update> list;
		int i, n;
		Value p, sum;

		// Create choice and add all info
		ch = new ChoiceListFlexi<Value>(eval);
		ch.setModuleOrActionIndex(moduleOrActionIndex);
		n = ups.getNumUpdates();
		sum = eval.zero();
		for (i = 0; i < n; i++) {
			// Compute probability/rate
			p = getProbabilityInState(ups, i, state);
			// Check that probabilities/rates are finite (non-infinite, non-NaN) and non-negative
			// We omit the check in symbolic (parametric) cases - too expensive
			// Note: we indicate errors in whole Updates object because the offending
			// probability expression has probably been simplified from original form.
			if (!eval.isSymbolic()) {
				if (!eval.isFinite(p)) {
					String msg = modelType.probabilityOrRate() + " is not finite in state " + state.toString(modulesFile);
					throw new PrismLangException(msg, ups);
				}
				if (!eval.geq(p, eval.zero())) {
					String msg = modelType.probabilityOrRate() + " is negative in state " + state.toString(modulesFile);
					throw new PrismLangException(msg, ups);
				}
			}
			// Skip transitions with zero probability/rate
			if (eval.isZero(p))
				continue;
			sum  = eval.add(sum, p);
			list = new ArrayList<Update>();
			list.add(ups.getUpdate(i));
			ch.add(p, list);
		}
		// For now, PRISM treats empty (all zero probs/rates) distributions as an error.
		// Later, when errors in symbolic model construction are improved, this might be relaxed.
		if (ch.size() == 0) {
			String msg = modelType.probabilityOrRate();
			msg += (ups.getNumUpdates() > 1) ? " values sum to " : " is ";
			msg += "zero for updates in state " + state.toString(modulesFile);
			throw new PrismLangException(msg, ups);
		}
		// Check distribution sums to 1 (if required, and if is non-empty)
		// As above, we omit the check in symbolic (parametric) cases - too expensive
		if (doProbChecks && ch.size() > 0 && modelType.choicesSumToOne() && !eval.isSymbolic()) {
			try {
				eval.checkProbabilitySum(sum);
			} catch (PrismException e) {
				throw new PrismLangException(e.getMessage() + " in state " + state.toString(modulesFile), ups);
			}
		}
		if (modelType.realTime() && clockGuards.containsKey(ups)) {
			ch.setClockGuard(clockGuards.get(ups));
		}
		return ch;
	}

	/**
	 * Create a new Choice object (currently ChoiceListFlexi) based on the product
	 * of an existing ChoiceListFlexi and an Updates object, for some (global) state.
	 * If appropriate, check probabilities sum to 1 too.
	 * @param ups The Updates object 
	 * @param state Global state
	 * @param ch The existing Choices object
	 */
	private void processUpdatesAndAddToProduct(Updates ups, State state, ChoiceListFlexi<Value> ch) throws PrismLangException
	{
		// Create new choice (action index is 0 - not needed)
		ChoiceListFlexi<Value> chNew = processUpdatesAndCreateNewChoice(0, ups, state);
		// Build product with existing
		ch.productWith(chNew);
	}
}
