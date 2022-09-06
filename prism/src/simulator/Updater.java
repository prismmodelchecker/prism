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
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import parser.State;
import parser.VarList;
import parser.ast.Command;
import parser.ast.Module;
import parser.ast.ModulesFile;
import parser.ast.RewardStruct;
import parser.ast.Update;
import parser.ast.Updates;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismSettings;

public class Updater extends PrismComponent
{
	// Settings:
	// Do we check that probabilities sum to 1?
	protected boolean doProbChecks = true;
	// The precision to which we check probabilities sum to 1
	protected double sumRoundOff = 1e-5;
	
	// Info on model being explored
	protected ModulesFile modulesFile;
	protected ModelType modelType;
	protected int numModules;
	protected VarList varList;
	// Synchronising action info
	protected Vector<String> synchs;
	protected int numSynchs;
	protected int synchModuleCounts[];
	// Model info/stats
	protected int numRewardStructs;

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

	public Updater(ModulesFile modulesFile, VarList varList)
	{
		this(modulesFile, varList, null);
	}
	
	public Updater(ModulesFile modulesFile, VarList varList, PrismComponent parent)
	{
		// Store some settings
		doProbChecks = parent.getSettings().getBoolean(PrismSettings.PRISM_DO_PROB_CHECKS);
		sumRoundOff = parent.getSettings().getDouble(PrismSettings.PRISM_SUM_ROUND_OFF);
		
		// Get info from model
		this.modulesFile = modulesFile;
		modelType = modulesFile.getModelType();
		numModules = modulesFile.getNumModules();
		synchs = modulesFile.getSynchs();
		numSynchs = synchs.size();
		numRewardStructs = modulesFile.getNumRewardStructs();
		this.varList = varList;

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
	}

	/**
	 * Set the precision to which we check that probabilities sum to 1.
	 */
	public void setSumRoundOff(double sumRoundOff)
	{
		this.sumRoundOff = sumRoundOff;
	}

	/**
	 * Get the precision to which we check that probabilities sum to 1.
	 */
	public double getSumRoundOff()
	{
		return sumRoundOff;
	}

	/**
	 * Determine the set of outgoing transitions from state 'state' and store in 'transitionList'.
	 * @param state State from which to explore
	 * @param transitionList TransitionList object in which to store result
	 */
	public void calculateTransitions(State state, TransitionList transitionList) throws PrismException
	{
		List<ChoiceListFlexi> chs;
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

		// Calculate the available updates for each module/action
		// (update information in updateLists, enabledSynchs and enabledModules)
		for (i = 0; i < numModules; i++) {
			calculateUpdatesForModule(i, state);
		}
		//System.out.println("updateLists: " + updateLists);

		// Add independent transitions for each (enabled) module to list
		for (i = enabledModules[0].nextSetBit(0); i >= 0; i = enabledModules[0].nextSetBit(i + 1)) {
			for (Updates ups : updateLists.get(i).get(0)) {
				ChoiceListFlexi ch = processUpdatesAndCreateNewChoice(-(i + 1), ups, state);
				if (ch.size() > 0)
					transitionList.add(ch);
			}
		}
		// Add synchronous transitions to list
		chs = new ArrayList<ChoiceListFlexi>();
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
						ChoiceListFlexi ch = processUpdatesAndCreateNewChoice(i, ups, state);
						if (ch.size() > 0)
							chs.add(ch);
					}
					// Case where there are existing Choices
					else {
						// Product with all existing choices
						for (ChoiceListFlexi ch : chs) {
							processUpdatesAndAddToProduct(ups, state, ch);
						}
					}
				}
				// Case where there are multiple Updates (i.e. local nondeterminism)
				else {
					// Case where there are no existing choices
					if (chs.size() == 0) {
						for (Updates ups : updateLists.get(j).get(i)) {
							ChoiceListFlexi ch = processUpdatesAndCreateNewChoice(i, ups, state);
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
								chs.add(new ChoiceListFlexi(chs.get(l)));
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
			for (ChoiceListFlexi ch : chs) {
				transitionList.add(ch);
			}
		}
		
		// For a DTMC, we need to normalise across all transitions
		// This is partly to handle "local nondeterminism"
		// and also to handle any dubious trickery done by disabling probability checks
		if (modelType == ModelType.DTMC) {
			double probSum = transitionList.getProbabilitySum();
			transitionList.scaleProbabilitiesBy(1.0 / probSum);
		}
	
		// Check validity of the computed transitions
		// (not needed currently)
		//transitionList.checkValid(modelType);
		
		// Check for errors (e.g. overflows) in the computed transitions
		//transitionList.checkForErrors(state, varList);
		
		//System.out.println(transitionList);
	}

	/**
	 * Calculate the state rewards for a given state.
	 * @param state The state to compute rewards for
	 * @param store An array in which to store the rewards
	 */
	public void calculateStateRewards(State state, double[] store) throws PrismLangException
	{
		int i, j, n;
		double d;
		RewardStruct rw;
		for (i = 0; i < numRewardStructs; i++) {
			rw = modulesFile.getRewardStruct(i);
			n = rw.getNumItems();
			d = 0.0;
			for (j = 0; j < n; j++) {
				if (!rw.getRewardStructItem(j).isTransitionReward())
					if (rw.getStates(j).evaluateBoolean(state))
						d += rw.getReward(j).evaluateDouble(state);
			}
			store[i] = d;
		}
	}

	/**
	 * Calculate the transition rewards for a given state and outgoing choice.
	 * @param state The state to compute rewards for
	 * @param ch The choice from the state to compute rewards for
	 * @param store An array in which to store the rewards
	 */
	public void calculateTransitionRewards(State state, Choice ch, double[] store) throws PrismLangException
	{
		int i, j, n;
		double d;
		RewardStruct rw;
		for (i = 0; i < numRewardStructs; i++) {
			rw = modulesFile.getRewardStruct(i);
			n = rw.getNumItems();
			d = 0.0;
			for (j = 0; j < n; j++) {
				if (rw.getRewardStructItem(j).isTransitionReward())
					if (rw.getRewardStructItem(j).getSynchIndex() == Math.max(0, ch.getModuleOrActionIndex()))
						if (rw.getStates(j).evaluateBoolean(state))
							d += rw.getReward(j).evaluateDouble(state);
			}
			store[i] = d;
		}
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
		Module module;
		Command command;
		int i, j, n;

		module = modulesFile.getModule(m);
		n = module.getNumCommands();
		for (i = 0; i < n; i++) {
			command = module.getCommand(i);
			if (command.getGuard().evaluateBoolean(state)) {
				j = command.getSynchIndex();
				updateLists.get(m).get(j).add(command.getUpdates());
				enabledSynchs.set(j);
				enabledModules[j].set(m);
			}
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
	private ChoiceListFlexi processUpdatesAndCreateNewChoice(int moduleOrActionIndex, Updates ups, State state) throws PrismLangException
	{
		ChoiceListFlexi ch;
		List<Update> list;
		int i, n;
		double p, sum;

		// Create choice and add all info
		ch = new ChoiceListFlexi();
		ch.setModuleOrActionIndex(moduleOrActionIndex);
		n = ups.getNumUpdates();
		sum = 0;
		for (i = 0; i < n; i++) {
			// Compute probability/rate
			p = ups.getProbabilityInState(i, state);
			// Check for negative/NaN probabilities/rates
			if (Double.isNaN(p) || p < 0) {
				String s = modelType.choicesSumToOne() ? "Probability" : "Rate";
				s += " is invalid (" + p + ") in state " + state.toString(modulesFile);
				// Note: we indicate error in whole Updates object because the offending
				// probability expression has probably been simplified from original form.
				throw new PrismLangException(s, ups);
			}
			// Skip transitions with zero probability/rate
			if (p == 0)
				continue;
			sum += p;
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
		if (doProbChecks && ch.size() > 0 && modelType.choicesSumToOne() && Math.abs(sum - 1) > sumRoundOff) {
			throw new PrismLangException("Probabilities sum to " + sum + " in state " + state.toString(modulesFile), ups);
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
	private void processUpdatesAndAddToProduct(Updates ups, State state, ChoiceListFlexi ch) throws PrismLangException
	{
		// Create new choice (action index is 0 - not needed)
		ChoiceListFlexi chNew = processUpdatesAndCreateNewChoice(0, ups, state);
		// Build product with existing
		ch.productWith(chNew);
	}
}
