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

package simulator;

import java.util.*;

import parser.*;
import parser.ast.*;
import prism.*;

public class Updater
{
	// Parent simulator/prism
	protected SimulatorEngine simulator;
	protected Prism prism;

	// Model to which the path corresponds
	protected ModulesFile modulesFile;
	protected ModelType modelType;
	protected int numModules;
	// Synchronising action info
	protected Vector<String> synchs;
	protected int numSynchs;
	// Model info/stats
	protected int numRewardStructs;

	// Temp storage
	protected List<List<List<Updates>>> updateLists;
	protected BitSet enabledSynchs;
	protected BitSet enabledModules[];

	// TODO: apply optimiseForFast or assume called?
	public Updater(SimulatorEngine simulator, ModulesFile modulesFile)
	{
		this.simulator = simulator;
		prism = simulator.getPrism();
		this.modulesFile = modulesFile;
		modelType = modulesFile.getModelType();
		numModules = modulesFile.getNumModules();
		synchs = modulesFile.getSynchs();
		numSynchs = synchs.size();
		numRewardStructs = modulesFile.getNumRewardStructs();
		updateLists = new ArrayList<List<List<Updates>>>(numModules);
		for (int i = 0; i < numModules; i++) {
			updateLists.add(new ArrayList<List<Updates>>(numSynchs + 1));
			for (int j = 0; j < numSynchs + 1; j++) {
				updateLists.get(i).add(new ArrayList<Updates>());
			}
		}
		enabledSynchs = new BitSet(numSynchs + 1);
		enabledModules = new BitSet[numSynchs + 1];
		for (int i = 0; i < numSynchs + 1; i++) {
			enabledModules[i] = new BitSet(numModules);
		}
	}

	/**
	 * Determine the set of outgoing transitions from state 'state' and store in 'transitionList'.
	 * @param state State from which to explore
	 * @param transitionList TransitionList object in which to store result
	 */
	public void calculateTransitions(State state, TransitionList transitionList) throws PrismException
	{
		Module module;
		ChoiceListFlexi ch, prod;
		int i, j, n, n2, n3;
		double p;

		System.out.println("Calc updates for " + state);
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
		for (i = 0; i < numModules; i++) {
			calculateUpdatesForModule(i, state);
		}
		System.out.println("updateLists: " + updateLists);

		// Combination of updates depends on model type
		switch (modelType) {

		case DTMC:
		case CTMC:
			ch = new ChoiceListFlexi();
			n = 0;
			// Independent choices for each (enabled) module
			for (i = enabledModules[0].nextSetBit(0); i >= 0; i = enabledModules[0].nextSetBit(i + 1)) {
				for (Updates ups : updateLists.get(i).get(0)) {
					/*processUpdatesAndAddToSum(ups, state, ch);
					n++;*/
					transitionList.add(processUpdatesAndCreateNewChoice(ups, state));
				}
			}
			// Add synchronous transitions to list
			for (i = enabledSynchs.nextSetBit(1); i >= 0; i = enabledSynchs.nextSetBit(i + 1)) {
				prod = null;
				for (j = enabledModules[i].nextSetBit(0); j >= 0; j = enabledModules[i].nextSetBit(j + 1)) {
					// TODO: Case where module blocks (CHECK COUNT?)
					// Case where there is 1 choice
					if (updateLists.get(j).get(i).size() == 1) {
						// Case where this is the first Updates added
						if (prod == null) {
							for (Updates ups : updateLists.get(j).get(i)) {
								prod = processUpdatesAndCreateNewChoice(ups, state);
							}
						} else {
							processUpdatesAndAddToProduct(updateLists.get(j).get(i).get(0), state, prod);
						}
					}
					// Case where there are multiple choices
					else {
						// TODO
						throw new PrismLangException("Don't handle local nondet yet");
					}
					System.out.println("prod" + j + ": " + prod);
				}
				transitionList.add(prod);
				n++;
			}

			/*if (n > 1)
				ch.scaleProbabilitiesBy(1.0 / n);
			transitionList.add(ch);*/
			break;

		case MDP:
			/*
			// Add independent transitions to list 
			for (i = 0; i < numModules; i++) {
				for (Updates ups : updateLists.get(i).get(0)) {
					transitionList.add(calculateTransitionsForUpdates(ups, state));
				}
			}
			// Add synchronous transitions to list
			for (i = 0; i < numSynchs; i++) {
				prod = new ArrayList<Updates>();
				for (j = 0; j < numModules; j++) {
					// Skip modules not using this action
					if (!modulesFile.getModule(j).usesSynch(synchs.get(i)))
						continue;
					// Case where module blocks
					if (updateLists.get(j).get(i).size() == 0) {
						prod.clear();
						break;
					}
					// Case where this is the first Updates added
					if (prod.isEmpty()) {
						for (Updates ups : updateLists.get(j).get(i)) {
							// TODO: avoid so much copying for efficiency?
							prod.add((Updates) ups.deepCopy());
						}
					}
					// Case where there is 1 choice
					else if (updateLists.get(j).get(i).size() == 1) {
					}
					// Case where there are multiple choices
					else {
						// TODO
						//for (Choice tr : choiceLists.get(j).get(i)) {
					}
				}
				for (Updates ups : prod) {
					transitionList.add(calculateTransitionsForUpdates(ups, state));
				}
			}*/
			break;

		default:
			throw new PrismException("Unhandled model type \"" + modelType + "\"");
		}

		// For DTMCs, need to randomise

		System.out.println(transitionList);

	}

	private ChoiceListFlexi processUpdatesAndCreateNewChoice(Updates ups, State state) throws PrismLangException
	{
		// TODO: use sum function?
		ChoiceListFlexi ch;
		List<Update> list;
		int i, n;
		double p, sum;

		ch = new ChoiceListFlexi();
		n = ups.getNumUpdates();
		sum = 0;
		for (i = 0; i < n; i++) {
			p = ups.getProbabilityInState(i, state);
			sum += p;
			list = new ArrayList<Update>();
			list.add(ups.getUpdate(i));
			ch.add("", p, list, ups.getParent());
		}
		// Check distribution sums to 1 (if required)
		if (modelType.choicesSumToOne() && Math.abs(sum - 1) > prism.getSumRoundOff()) {
			throw new PrismLangException("Probabilities sum to " + sum + " in state " + state.toString(modulesFile), ups);
		}

		return ch;
	}

	private void processUpdatesAndAddToSum(Updates ups, State state, ChoiceListFlexi ch) throws PrismLangException
	{
		List<Update> list;
		int i, n;
		double p, sum;

		n = ups.getNumUpdates();
		sum = 0;
		for (i = 0; i < n; i++) {
			p = ups.getProbabilityInState(i, state);
			sum += p;
			list = new ArrayList<Update>();
			list.add(ups.getUpdate(i));
			ch.add("", p, list, ups.getParent());
		}
		// Check distribution sums to 1 (if required)
		if (modelType.choicesSumToOne() && Math.abs(sum - 1) > prism.getSumRoundOff()) {
			throw new PrismLangException("Probabilities sum to " + sum + " in state " + state);
		}

	}

	private void processUpdatesAndAddToProduct(Updates ups, State state, ChoiceListFlexi ch) throws PrismLangException
	{
		ChoiceListFlexi chNew;
		chNew = processUpdatesAndCreateNewChoice(ups, state);
		ch.productWith(chNew);
	}

	// Model exploration methods (e.g. for simulation)

	/**
	 * Determine the enabled updates for the 'm'th module from (global) state 'state'.
	 * Update information in ... TODO
	 * @param m The module index
	 * @param state State from which to explore
	 * @param ... 
	 * TODO
	 */
	private void calculateUpdatesForModule(int m, State state) throws PrismLangException
	{
		Module module;
		Command command;
		int i, j, n;
		String s;

		module = modulesFile.getModule(m);
		n = module.getNumCommands();
		for (i = 0; i < n; i++) {
			command = module.getCommand(i);
			if (command.getGuard().evaluateBoolean(state)) {
				s = command.getSynch();
				j = ("".equals(s)) ? -1 : synchs.indexOf(s);
				updateLists.get(m).get(j + 1).add(command.getUpdates());
				enabledSynchs.set(j + 1);
				enabledModules[j + 1].set(m);
			}
		}
	}

	/*private Choice calculateTransitionsForUpdates(Updates ups, State state) throws PrismLangException
	{
		int i, n;
		State newState;

		n = ups.getNumUpdates();
		if (n == 1) {
			ChoiceSingleton chSingle = null;
			chSingle = new ChoiceSingleton();
			newState = calculateTransitionsForUpdate(ups.getUpdate(0), state);
			chSingle.setTarget(newState);
			chSingle.setProbability(ups.getProbabilityInState(0, state));
			return chSingle;
		} else {
			ChoiceList chList;
			chList = new ChoiceList(n);
			for (i = 0; i < n; i++) {
				newState = calculateTransitionsForUpdate(ups.getUpdate(i), state);
				chList.addTarget(newState);
				chList.addProbability(ups.getProbabilityInState(i, state));
			}
			return chList;
		}
	}*/

	// TODO: do we really need to evaluate dest State at this point?
	// maybe just store pointer to Update object for efficiency
	private State calculateTransitionsForUpdate(Update up, State state) throws PrismLangException
	{
		State newState;
		int i, n;

		// Copy current state, then apply updates
		newState = new State(state);
		n = up.getNumElements();
		for (i = 0; i < n; i++) {
			newState.varValues[up.getVarIndex(i)] = up.getExpression(i).evaluate(state);
		}

		return newState;
	}

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

}
