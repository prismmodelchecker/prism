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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import parser.State;
import parser.ast.*;
import prism.ModelType;
import prism.Prism;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismPrintStreamLog;
import prism.UndefinedConstants;

public class QuantAbstractRefineExample extends QuantAbstractRefine
{
	// Flags/settings
	protected boolean rebuildImmed = false; // Rebuild split states immediately

	// Concrete model
	protected ModelSimple modelConcrete;
	protected ModulesFile modulesFile;
	protected int nConcrete; // Number of (concrete) states
	protected BitSet initialConcrete; // Initial (concrete) states
	protected BitSet targetConcrete; // Target (concrete) states
	protected String targetLabel; // PRISM label denoting target states

	// Abstraction info
	// Map from concrete to abstract states
	protected int concreteToAbstract[];
	// Map from abstract P1 choices to concrete state sets
	protected List<List<Set<Integer>>> abstractToConcrete;

	/**
	 * Default constructor.
	 */
	public QuantAbstractRefineExample(PrismComponent parent) throws PrismException
	{
		super(parent);
	}

	// Implementation of initialise() for abstraction-refinement loop; see superclass for details 

	@Override
	protected void initialise() throws PrismException
	{
		Expression targetExpr;
		List<State> statesList;
		DistributionSet set;
		List<Set<Integer>> list;
		boolean isTarget, isInitial, existsInitial, existsTargetAndInitial, existsRest;
		int i, c, a, j, nAbstract;

		// Build concrete model
		mainLog.println("Concrete " + modelType + ": " + modelConcrete.infoString());
		nConcrete = modelConcrete.getNumStates();

		// Get initial (concrete) states
		initialConcrete = new BitSet(nConcrete);
		for (int in: modelConcrete.getInitialStates())
			initialConcrete.set(in, 1);
		
		// Get target (concrete) states
		statesList = modelConcrete.getStatesList();
		i = modulesFile.getLabelList().getLabelIndex(targetLabel);
		if (i == -1l)
			throw new PrismException("Unknown label \"" + targetLabel + "\"");
		targetExpr = modulesFile.getLabelList().getLabel(i);
		targetConcrete = new BitSet(nConcrete);
		for (i = 0; i < nConcrete; i++) {
			targetConcrete.set(i, targetExpr.evaluateBoolean(statesList.get(i)));
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
		case MDP:
			abstraction = new STPGAbstrSimple(nAbstract);
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
			case MDP:
				set = buildAbstractDistributionSet(c, (MDPSimple) modelConcrete, (STPG) abstraction);
				j = ((STPGAbstrSimple) abstraction).addDistributionSet(a, set);
				break;
			default:
				throw new PrismException("Cannot handle model type " + modelType);
			}
			list = abstractToConcrete.get(a);
			if (j >= list.size())
				list.add(new HashSet<Integer>(1));
			list.get(j).add(c);
		}
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

	@Override
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

	@Override
	protected void rebuildAbstraction(Set<Integer> rebuildStates) throws PrismException
	{
		for (int a : rebuildStates) {
			rebuildAbstractionState(a);
		}
	}

	protected void rebuildAbstractionState(int i) throws PrismException
	{
		List<Set<Integer>> list, listNew;
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
				case MDP:
					set = buildAbstractDistributionSet(c, (MDPSimple) modelConcrete, (STPG) abstraction);
					j = ((STPGAbstrSimple) abstraction).addDistributionSet(a, set);
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

	// Override this to also print out concrete model details at the end
	@Override
	protected void printFinalSummary(String initAbstractionInfo, boolean canRefine)
	{
		mainLog.println("\nConcrete " + modelType + ": " + modelConcrete.infoString());
		super.printFinalSummary(initAbstractionInfo, canRefine);
	}

	public static void main(String args[])
	{
		// Parse command line args
		if (args.length < 2) {
			System.err.println("Usage: java ... <PRISM model> <target label>");
			System.exit(1);
		}
		try {
			// Load/parse a PRISM model description
			PrismLog mainLog = new PrismPrintStreamLog(System.out);
			Prism prism = new Prism(mainLog, mainLog);
			ModulesFile modulesFile = prism.parseModelFile(new File(args[0]));
			UndefinedConstants undefinedConstants = new UndefinedConstants(modulesFile, null);
			undefinedConstants.defineUsingConstSwitch("");
			modulesFile.setUndefinedConstants(undefinedConstants.getMFConstantValues());
			modulesFile = (ModulesFile) modulesFile.deepCopy().expandConstants(modulesFile.getConstantList());
			
			// Build the model (explicit-state reachability) 
			ConstructModel constructModel = new ConstructModel(prism, prism.getSimulator());
			ModelSimple model = (ModelSimple) constructModel.constructModel(modulesFile, false, false);
			model.exportToPrismExplicitTra(args[1]);
			
			// Create/initialise abstraction-refinement engine
			QuantAbstractRefineExample abstractRefine = new QuantAbstractRefineExample(prism);
			abstractRefine.setModelType(ModelType.MDP);
			abstractRefine.setPropertyType(PropertyType.PROB_REACH);
			abstractRefine.sanityChecks = true;
			abstractRefine.modelConcrete = model;
			abstractRefine.modulesFile = modulesFile;
			abstractRefine.targetLabel = args[1];
			
			// Do abstraction-refinement
			abstractRefine.printSettings();
			boolean min = true;
			abstractRefine.abstractRefine(min);
			
		} catch (FileNotFoundException e) {
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		} catch (PrismException e) {
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
	}
}
