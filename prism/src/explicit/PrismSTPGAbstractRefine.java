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

import common.IterableStateSet;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismNotSupportedException;

public class PrismSTPGAbstractRefine extends QuantAbstractRefine
{
	// Inputs
	protected String traFile; // Model file for concrete model (PRISM explicit output)
	protected String labFile; // Labels file for concrete model (PRISM explicit output)
	protected String rewsFile; // State rewards file for concrete model (PRISM explicit output)
	protected String rewtFile; // Transition rewards file for concrete model (PRISM explicit output)
	protected String targetLabel; // PRISM label denoting target stares

	// Flags/settings
	protected boolean exact = false; // Do model checking on the full concrete model?
	protected boolean exactCheck = false; // Use exact result to check A-R result? (or just skip A-R?)
	protected boolean rebuildImmed = false; // Rebuild split states immediately

	// Concrete model
	protected ModelSimple modelConcrete;
	protected int nConcrete; // Number of (concrete) states
	protected BitSet initialConcrete; // Initial (concrete) states
	protected BitSet targetConcrete; // Target (concrete) states
	protected double exactInit; // Exact result for concrete model

	// Abstraction info
	// Map from concrete to abstract states
	protected int concreteToAbstract[];
	// Map from abstract P1 choices to concrete state sets
	protected List<List<Set<Integer>>> abstractToConcrete;

	/**
	 * Default constructor.
	 */
	public PrismSTPGAbstractRefine(PrismComponent parent) throws PrismException
	{
		super(parent);
	}

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
		mainLog.println("Building concrete " + (buildEmbeddedDtmc ? "(embedded) " : "") + modelType + "...");
		switch (modelType) {
		case DTMC:
			modelConcrete = buildEmbeddedDtmc ? new CTMCSimple() : new DTMCSimple();
			break;
		case CTMC:
			modelConcrete = new CTMCSimple();
			break;
		case MDP:
			modelConcrete = new MDPSimple();
			break;
		default:
			throw new PrismNotSupportedException("Cannot handle model type " + modelType);
		}
		modelConcrete.buildFromPrismExplicit(traFile);
		if (buildEmbeddedDtmc) {
			// TODO: do more efficiently (straight from tra file?)
			mainLog.println("Concrete " + "CTMC" + ": " + modelConcrete.infoString());
			//mainLog.println(modelConcrete);
			modelConcrete = ((CTMCSimple) modelConcrete).buildEmbeddedDTMC();
			//mainLog.println(modelConcrete);
		}
		//if (propertyType == PropertyType.EXP_REACH)
		//modelConcrete.buildTransitionRewardsFromFile(rewtFile);
		//modelConcrete.setConstantTransitionReward(1.0);
		mainLog.println("Concrete " + modelType + ": " + modelConcrete.infoString());
		nConcrete = modelConcrete.getNumStates();

		// For a CTMC and time-bounded properties, we need to uniformise
		if (modelType == ModelType.CTMC) {
			if (propertyType != PropertyType.PROB_REACH) {
				((CTMCSimple) modelConcrete).uniformise(((CTMCSimple) modelConcrete).getDefaultUniformisationRate());
			}
		}

		// Get initial/target (concrete) states
		labels = StateModelChecker.loadLabelsFile(labFile);
		initialConcrete = labels.get("init");
		targetConcrete = labels.get(targetLabel);
		if (targetConcrete == null)
			throw new PrismException("Unknown label \"" + targetLabel + "\"");

		// set the initial states from the set initialConcrete
		for (int state : new IterableStateSet(initialConcrete, modelConcrete.getNumStates())) {
			modelConcrete.addInitialState(state);
		}

		// If the 'exact' flag is set, just do model checking on the concrete model (no abstraction)
		if (exact) {
			doExactModelChecking();
			if (!exactCheck)
				throw new PrismException("Terminated early after exact verification");
		}

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

		if (verbosity >= 10) {
			mainLog.print("Initial concreteToAbstract: ");
			mainLog.println(concreteToAbstract);
		}

		// Create (empty) abstraction and store initial states info
		nAbstract = existsRest ? 3 : 2;
		switch (modelType) {
		case DTMC:
			abstraction = new MDPSimple(nAbstract);
			break;
		case CTMC:
			abstraction = new CTMDPSimple(nAbstract);
			// TODO: ((CTMDP) abstraction).unif = ((CTMCSimple) modelConcrete).unif;
			break;
		case MDP:
			abstraction = new STPGAbstrSimple(nAbstract);
			break;
		default:
			throw new PrismNotSupportedException("Cannot handle model type " + modelType);
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
				distr = buildAbstractDistribution(c, (DTMCSimple) modelConcrete);
				j = ((MDPSimple) abstraction).addChoice(a, distr);
				//((MDPSimple) abstraction).setTransitionReward(a, j, ((DTMC) modelConcrete).getTransitionReward(c));
				break;
			case CTMC:
				distr = buildAbstractDistribution(c, (CTMCSimple) modelConcrete);
				j = ((CTMDPSimple) abstraction).addChoice(a, distr);
				break;
			case MDP:
				set = buildAbstractDistributionSet(c, (MDPSimple) modelConcrete, (STPG) abstraction);
				j = ((STPGAbstrSimple) abstraction).addDistributionSet(a, set);
				break;
			default:
				throw new PrismNotSupportedException("Cannot handle model type " + modelType);
			}
			list = abstractToConcrete.get(a);
			if (j >= list.size())
				list.add(new HashSet<Integer>(1));
			list.get(j).add(c);
		}
	}

	/**
	 * Abstract a concrete state c of a DTMC ready to add to an MDP state.
	 */
	protected Distribution buildAbstractDistribution(int c, DTMCSimple dtmc)
	{
		return dtmc.getTransitions(c).map(concreteToAbstract);
	}

	/**
	 * Abstract a concrete state c of an MDP ready to add to an STPG state.
	 */
	protected DistributionSet buildAbstractDistributionSet(int c, MDPSimple mdp, STPG stpg)
	{
		DistributionSet set = ((STPGAbstrSimple) stpg).newDistributionSet(null);
		for (Distribution distr : mdp.getChoices(c)) {
			set.add(distr.map(concreteToAbstract));
		}
		return set;
	}

	// Implementation of splitState(...) for abstraction-refinement loop; see superclass for details 

	protected int splitState(int splitState, List<List<Integer>> choiceLists, Set<Integer> rebuiltStates,
			Set<Integer> rebuildStates) throws PrismException
	{
		List<Set<Integer>> list, listNew;
		Set<Integer> concreteStates, concreteStatesNew;
		int i, a, nAbstract, numNewStates;

		if (verbosity >= 1)
			mainLog.println("Splitting: #" + splitState);

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

		if (verbosity >= 10) {
			mainLog.print("New concreteToAbstract: ");
			mainLog.println(concreteToAbstract);
		}

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
			if (i == splitState || abstraction.isSuccessor(i, splitState)) {
				if (rebuildImmed) {
					rebuildAbstractionState(i);
					rebuiltStates.add(i);
				} else {
					rebuildStates.add(i);
				}
			}
		}
		for (i = 1; i < numNewStates; i++) {
			if (rebuildImmed) {
				rebuildAbstractionState(nAbstract + i - 1);
				rebuiltStates.add(i);
			} else {
				rebuildStates.add(nAbstract + i - 1);
			}
		}

		return numNewStates;
	}

	// Implementation of rebuildAbstraction(...) for abstraction-refinement loop; see superclass for details 

	protected void rebuildAbstraction(Set<Integer> rebuildStates) throws PrismException
	{
		for (int a : rebuildStates) {
			rebuildAbstractionState(a);
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
				if (a != i)
					throw new PrismException("Oops");
				switch (modelType) {
				case DTMC:
					distr = buildAbstractDistribution(c, (DTMCSimple) modelConcrete);
					j = ((MDPSimple) abstraction).addChoice(a, distr);
					//((MDPSimple) abstraction).setTransitionReward(a, j, ((DTMC) modelConcrete).getTransitionReward(c));
					break;
				case CTMC:
					distr = buildAbstractDistribution(c, (CTMCSimple) modelConcrete);
					j = ((CTMDPSimple) abstraction).addChoice(a, distr);
					// TODO: recompute unif?
					break;
				case MDP:
					set = buildAbstractDistributionSet(c, (MDPSimple) modelConcrete, (STPG) abstraction);
					j = ((STPGAbstrSimple) abstraction).addDistributionSet(a, set);
					break;
				default:
					throw new PrismNotSupportedException("Cannot handle model type " + modelType);
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
		ModelCheckerResult res = null;
		switch (modelType) {
		case DTMC:
			DTMCModelChecker mcDtmc = new DTMCModelChecker(null);
			mcDtmc.inheritSettings(mcOptions);
			switch (propertyType) {
			case PROB_REACH:
				res = mcDtmc.computeReachProbs((DTMC) modelConcrete, targetConcrete);
				break;
			case PROB_REACH_BOUNDED:
				res = mcDtmc.computeBoundedReachProbs((DTMC) modelConcrete, targetConcrete, reachBound);
				break;
			case EXP_REACH:
				break;
			}
			break;
		case CTMC:
			CTMCModelChecker mcCtmc = new CTMCModelChecker(null);
			mcCtmc.inheritSettings(mcOptions);
			switch (propertyType) {
			/*case PROB_REACH:
				res = mcDtmc.probReach((DTMC) modelConcrete, targetConcrete);
				break;
			case PROB_REACH_BOUNDED:
				res = mcDtmc.probReachBounded((DTMC) modelConcrete, targetConcrete, reachBound);
				break;
			case EXP_REACH:
				break;*/
			}
			break;
		case MDP:
			MDPModelChecker mcMdp = new MDPModelChecker(null);
			mcMdp.inheritSettings(mcOptions);
			switch (propertyType) {
			case PROB_REACH:
				res = mcMdp.computeReachProbs((MDP) modelConcrete, targetConcrete, min);
				break;
			case PROB_REACH_BOUNDED:
				res = mcMdp.computeBoundedReachProbs((MDP) modelConcrete, targetConcrete, reachBound, min);
				break;
			case EXP_REACH:
				break;
			}
			break;
		}

		// Unhandled cases
		if (res == null) {
			String s = "Cannot do exact model checking for";
			s += " model type " + modelType + " and property type " + propertyType;
			throw new PrismException(s);
		}

		// Display results for all initial states
		mainLog.print("Results for initial state(s):");
		for (int j : modelConcrete.getInitialStates()) {
			mainLog.print(" " + res.soln[j]);
		}
		mainLog.println();

		// Pick min/max value over all initial states
		exactInit = min ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
		for (int j : modelConcrete.getInitialStates()) {
			if (min) {
				exactInit = Math.min(exactInit, res.soln[j]);
			} else {
				exactInit = Math.max(exactInit, res.soln[j]);
			}
		}
	}

	// Override this to also print out concrete model details at the end
	@Override
	protected void printFinalSummary(String initAbstractionInfo, boolean canRefine)
	{
		mainLog.println("\nConcrete " + modelType + ": " + modelConcrete.infoString());
		super.printFinalSummary(initAbstractionInfo, canRefine);
		mainLog.print("Exact (concrete) result: " + exactInit);
		mainLog.println(" (diff = " + Math.abs(exactInit - ((lbInit + ubInit) / 2)) + ")");
	}

	public static void main(String args[])
	{
		PrismSTPGAbstractRefine abstractRefine;
		boolean min = false;
		ArrayList<String> nonSwitches;
		String s, sw, sOpt, filenameBase;
		int i, j;

		try {
			// Create abstraction-refinement engine
			abstractRefine = new PrismSTPGAbstractRefine(null);
			abstractRefine.sanityChecks = true;

			// Parse command line args
			if (args.length < 3) {
				System.err.println("Usage: java ... [options] <tra file> <lab file> <target label>");
				System.exit(1);
			}
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
					} else if (sw.equals("exact")) {
						abstractRefine.exact = true;
					} else if (sw.equals("exactcheck")) {
						abstractRefine.exact = true;
						abstractRefine.exactCheck = true;
					} else if (sw.equals("rebuild") && sOpt.equals("immed")) {
						abstractRefine.rebuildImmed = true;
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
