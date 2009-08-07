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

package explicit;

import java.util.*;

import prism.ModelType;
import prism.PrismException;

public class PrismSTPGAbstractRefine extends STPGAbstractRefine
{
	// Inputs
	protected String traFile; // Model file for concrete model (PRISM explicit output)
	protected String labFile; // Labels file for concrete model (PRISM explicit output)
	protected String rewsFile; // State rewards file for concrete model (PRISM explicit output)
	protected String rewtFile; // Transition rewards file for concrete model (PRISM explicit output)
	protected String targetLabel; // PRISM label denoting target stares

	// Flags/settings
	protected boolean exact = false; // Just do model checking on the full concrete model

	// Concrete model
	protected Model modelConcrete;
	protected int nConcrete; // Number of (concrete) states
	protected BitSet initialConcrete; // Initial (concrete) states
	protected BitSet targetConcrete; // Target (concrete) states

	// Abstraction info
	// Map from concrete to abstract states
	protected int concreteToAbstract[];
	// Map from abstract P1 choices to concrete state sets
	protected List<List<Set<Integer>>> abstractToConcrete;

	// Implementation of initialise() for abstraction-refinement loop; see superclass for details 

	protected void initialise() throws PrismException
	{
		Map<String, BitSet> labels;
		DistributionSet set;
		Distribution distr;
		List<Set<Integer>> list;
		boolean isTarget, isInitial, existsInitial, existsTargetAndInitial, existsRest;
		int c, a, j, nAbstract;

		// Build concrete model
		mainLog.println("Building concrete " + modelType + "...");
		switch (modelType) {
		case DTMC:
			modelConcrete = new DTMC();
			break;
		case CTMC:
			modelConcrete = new CTMC();
			break;
		case MDP:
			modelConcrete = new MDP();
			break;
		default:
			throw new PrismException("Cannot handle model type " + modelType);
		}
		modelConcrete.buildFromPrismExplicit(traFile);
		//if (propertyType == PropertyType.EXP_REACH)
		//modelConcrete.buildTransitionRewardsFromFile(rewtFile);
		modelConcrete.setConstantTransitionReward(1.0);
		mainLog.println("Concrete " + modelType + ": " + modelConcrete.infoString());
		nConcrete = modelConcrete.getNumStates();

		// For a CTMC, we need to uniformise
		if (modelType == ModelType.CTMC) {
			((CTMC) modelConcrete).uniformise();
		}

		// Get initial/target (concrete) states
		labels = new ModelChecker().loadLabelsFile(labFile);
		initialConcrete = labels.get("init");
		targetConcrete = labels.get(targetLabel);
		if (targetConcrete == null)
			throw new PrismException("Unknown label \"" + targetLabel + "\"");

		// If the 'exact' flag is set, just do model checking on the concrete model (no abstraction)
		if (exact)
			doExactModelChecking();

		// Build a mapping between concrete/abstract states
		// Initial abstract states: 0: initial, 1: target, 2:rest
		concreteToAbstract = new int[nConcrete];
		existsInitial = existsTargetAndInitial = existsRest = false;
		for (c = 0; c < nConcrete; c++) {
			isTarget = targetConcrete.get(c);
			isInitial = initialConcrete.get(c);
			existsInitial |= (!isTarget && isInitial);
			existsTargetAndInitial |= (isTarget && isInitial);
			existsRest |= (!isTarget && !isInitial);
			concreteToAbstract[c] = isTarget ? 1 : isInitial ? 0 : 2;
		}
		if (!existsInitial)
			throw new PrismException("No non-target initial states");

		// Create (empty) abstraction and store initial states info
		nAbstract = existsRest ? 3 : 2;
		switch (modelType) {
		case DTMC:
			abstraction = new MDP(nAbstract);
			break;
		case CTMC:
			abstraction = new CTMDP(nAbstract);
			((CTMDP) abstraction).unif = ((CTMC) modelConcrete).unif;
			break;
		case MDP:
			abstraction = new STPG(nAbstract);
			break;
		default:
			throw new PrismException("Cannot handle model type " + modelType);
		}
		abstraction.addInitialState(0);
		if (existsTargetAndInitial)
			abstraction.addInitialState(1);

		// Build target set
		target = new BitSet(nAbstract);
		target.set(1);

		// Construct initial abstraction.
		// Simultaneously, build abstractToConcrete,
		// which records which concrete states correspond to each game choice.
		abstractToConcrete = new ArrayList<List<Set<Integer>>>(nAbstract);
		for (a = 0; a < nAbstract; a++)
			abstractToConcrete.add(new ArrayList<Set<Integer>>());
		for (c = 0; c < nConcrete; c++) {
			a = concreteToAbstract[c];
			switch (modelType) {
			case DTMC:
				distr = buildAbstractDistribution(c, (DTMC) modelConcrete);
				j = ((MDP) abstraction).addDistribution(a, distr);
				((MDP) abstraction).setTransitionReward(a, j, ((DTMC) modelConcrete).getTransitionReward(c));
				break;
			case CTMC:
				distr = buildAbstractDistribution(c, (CTMC) modelConcrete);
				j = ((CTMDP) abstraction).addDistribution(a, distr);
				break;
			case MDP:
				set = buildAbstractDistributionSet(c, (MDP) modelConcrete, (STPG) abstraction);
				j = ((STPG) abstraction).addDistributionSet(a, set);
				break;
			default:
				throw new PrismException("Cannot handle model type " + modelType);
			}
			list = abstractToConcrete.get(a);
			if (j >= list.size())
				list.add(new HashSet<Integer>(1));
			list.get(j).add(c);
		}
		//mainLog.println(abstraction);
	}

	/**
	 * Abstract a concrete state c of a DTMC ready to add to an MDP state.
	 */
	protected Distribution buildAbstractDistribution(int c, DTMC dtmc)
	{
		Distribution distrNew;
		int k;
		double prob;

		distrNew = new Distribution();
		for (Map.Entry<Integer, Double> e : dtmc.trans.get(c)) {
			k = (Integer) e.getKey();
			prob = (Double) e.getValue();
			distrNew.add(concreteToAbstract[k], prob);
		}

		return distrNew;
	}

	/**
	 * Abstract a concrete state c of an MDP ready to add to an STPG state.
	 */
	protected DistributionSet buildAbstractDistributionSet(int c, MDP mdp, STPG stpg)
	{
		Distribution distrNew;
		DistributionSet set;
		int k;
		double prob;

		set = ((STPG) stpg).newDistributionSet(null);
		for (Distribution distr : mdp.steps.get(c)) {
			distrNew = new Distribution();
			for (Map.Entry<Integer, Double> e : distr) {
				k = (Integer) e.getKey();
				prob = (Double) e.getValue();
				distrNew.add(concreteToAbstract[k], prob);
			}
			set.add(distrNew);
		}

		return set;
	}

	// Implementation of splitState(...) for abstraction-refinement loop; see superclass for details 

	protected int splitState(int splitState, List<List<Integer>> choiceLists, Set<Integer> rebuildStates)
			throws PrismException
	{
		List<Set<Integer>> list, listNew;
		Set<Integer> concreteStates, concreteStatesNew;
		int i, a, nAbstract, numNewStates;

		if (verbosity >= 1)
			mainLog.println("Splitting: #" + splitState);

		// TODO: this is tmp
		Set<Integer> rebuildStatesTmp = new HashSet<Integer>();

		// Add an element to the list of choices
		// corresponding to all remaining choices
		addRemainderIntoChoiceLists(splitState, choiceLists);

		// Do split...
		nAbstract = abstraction.getNumStates();
		numNewStates = choiceLists.size();
		list = abstractToConcrete.get(splitState);
		i = 0;
		for (List<Integer> choiceList : choiceLists) {
			// Build...
			listNew = new ArrayList<Set<Integer>>(1);
			concreteStatesNew = new HashSet<Integer>();
			listNew.add(concreteStatesNew);
			// Compute index 'a' of new abstract state
			// (first one reuses splitState, rest go on the end)
			// Also add new list to abstractToConcrete
			if (i == 0) {
				a = splitState;
				abstractToConcrete.set(a, listNew);
			} else {
				a = nAbstract + i - 1;
				abstractToConcrete.add(listNew);
			}
			rebuildStates.add(a); // TODO; and more!
			rebuildStatesTmp.add(a);
			//log.println(choiceList);
			for (int j : choiceList) {
				concreteStates = list.get(j);
				for (int c : concreteStates) {
					//log.println(c);
					concreteToAbstract[c] = a;
					concreteStatesNew.add(c);
				}
			}
			i++;
		}
		/*log.print("concreteToAbstract:");
		for (i = 0; i < nConcrete; i++)
			log.print(" " + i + "=" + concreteToAbstract[i]);
		log.println();*/

		// TODO: who should do this?
		// Add new states to the abstraction
		abstraction.addStates(numNewStates - 1);
		// Add new states to initial state set if needed
		// Note: we assume any abstract state contains either all/no initial states
		if (abstraction.isInitialState(splitState)) {
			for (i = 1; i < numNewStates; i++) {
				abstraction.addInitialState(nAbstract + i - 1);
			}
		}

		for (i = 0; i < nAbstract; i++) {
			if (i == splitState || abstraction.isSuccessor(splitState, i))
				rebuildAbstractionState(i);
		}
		for (i = 1; i < numNewStates; i++) {
			rebuildAbstractionState(nAbstract + i - 1);
		}

		return numNewStates;
	}

	// Implementation of rebuildAbstraction(...) for abstraction-refinement loop; see superclass for details 

	protected void rebuildAbstraction(Set<Integer> rebuildStates) throws PrismException
	{
		int i;

		//TODO: just rebuildStates?
		//		for (int a : rebuildStates) {
		for (i = 0; i < abstraction.getNumStates(); i++) {
			rebuildAbstractionState(i);
		}
	}

	protected void rebuildAbstractionState(int i) throws PrismException
	{
		List<Set<Integer>> list, listNew;
		Distribution distr;
		DistributionSet set;
		int j, a;

		list = abstractToConcrete.get(i);
		listNew = new ArrayList<Set<Integer>>();
		abstraction.clearState(i);
		for (Set<Integer> concreteStates : list) {
			for (int c : concreteStates) {
				a = concreteToAbstract[c];
				// ASSERT: a = i ???
				switch (modelType) {
				case DTMC:
					distr = buildAbstractDistribution(c, (DTMC) modelConcrete);
					j = ((MDP) abstraction).addDistribution(a, distr);
					((MDP) abstraction).setTransitionReward(a, j, ((DTMC) modelConcrete).getTransitionReward(c));
					break;
				case CTMC:
					distr = buildAbstractDistribution(c, (CTMC) modelConcrete);
					j = ((CTMDP) abstraction).addDistribution(a, distr);
					// TODO: recompute unif?
					break;
				case MDP:
					set = buildAbstractDistributionSet(c, (MDP) modelConcrete, (STPG) abstraction);
					j = ((STPG) abstraction).addDistributionSet(a, set);
					break;
				default:
					throw new PrismException("Cannot handle model type " + modelType);
				}
				if (j >= listNew.size())
					listNew.add(new HashSet<Integer>(1));
				listNew.get(j).add(c);
				//TODO: if (initialConcrete.get(c))
				//TODO: 	stpg.initialStates.add(a);
			}
		}
		abstractToConcrete.set(i, listNew);
	}

	/**
	 * Just do model checking on the concrete model (no abstraction)
	 */
	public void doExactModelChecking() throws PrismException
	{
		switch (modelType) {
		case DTMC:
			break;
		case CTMC:
			break;
		case MDP:
			break;
		default:
			String s = "Cannot do exact model checking for";
			s += " model type " + modelType + " and property type " + propertyType;
			throw new PrismException(s);
		}
	}

	public static void main(String args[])
	{
		PrismSTPGAbstractRefine abstractRefine;
		boolean min = false;
		ArrayList<String> nonSwitches;
		String s, sw, sOpt, filenameBase;
		int i, j;

		// Create abstraction-refinement engine
		abstractRefine = new PrismSTPGAbstractRefine();
		abstractRefine.sanityChecks = true;

		// Parse command line args
		if (args.length < 3) {
			System.err.println("Usage: java ... [options] <tra file> <lab file> <target label>");
			System.exit(1);
		}
		try {
			nonSwitches = new ArrayList<String>();
			for (i = 0; i < args.length; i++) {
				s = args[i];
				// Process a non-switch
				if (s.charAt(0) != '-') {
					nonSwitches.add(s);
				}
				// Process a switch
				else {
					s = s.substring(1);
					// Break switch up into parts if contains =
					if ((j = s.indexOf('=')) != -1) {
						sOpt = s.substring(j + 1);
						sw = s.substring(0, j);
					} else {
						sOpt = null;
						sw = s;
					}
					// Local options
					if (sw.equals("min")) {
						min = true;
					} else if (sw.equals("max")) {
						min = false;
					} else if (sw.equals("dtmc")) {
						abstractRefine.setModelType(ModelType.DTMC);
					} else if (sw.equals("ctmc")) {
						abstractRefine.setModelType(ModelType.CTMC);
					} else if (sw.equals("mdp")) {
						abstractRefine.setModelType(ModelType.MDP);
					} else if (sw.equals("probreach")) {
						abstractRefine.setPropertyType(PropertyType.PROB_REACH);
					} else if (sw.equals("expreach")) {
						abstractRefine.setPropertyType(PropertyType.EXP_REACH);
					} else if (sw.equals("probreachbnd")) {
						abstractRefine.setPropertyType(PropertyType.PROB_REACH_BOUNDED);
						if (sOpt != null) {
							abstractRefine.setReachBound(Integer.parseInt(sOpt));
						}
					}

					// Otherwise, try passing to abstraction-refinement engine
					else {
						abstractRefine.parseOption(s);
					}
				}
			}
			// Store params
			filenameBase = nonSwitches.get(0);
			abstractRefine.targetLabel = nonSwitches.get(1);
			abstractRefine.traFile = filenameBase + ".tra";
			abstractRefine.labFile = filenameBase + ".lab";
			abstractRefine.rewsFile = filenameBase + ".rews";
			abstractRefine.rewtFile = filenameBase + ".rewt";
			// Display command-line args and settings:
			System.out.print("Command:");
			for (i = 0; i < args.length; i++)
				System.out.print(" " + args[i]);
			System.out.println();
			abstractRefine.printSettings();
			// Go...
			abstractRefine.abstractRefine(min);
		} catch (PrismException e) {
			System.out.println("Error: " + e.getMessage() + ".");
			System.exit(1);
		}
	}
}
