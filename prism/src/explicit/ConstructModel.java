//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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
import java.util.LinkedList;
import java.util.List;

import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.Expression;
import parser.ast.ModulesFile;
import prism.ModelType;
import prism.Prism;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismPrintStreamLog;
import prism.ProgressDisplay;
import prism.UndefinedConstants;
import simulator.SimulatorEngine;

public class ConstructModel extends PrismComponent
{
	// The simulator engine
	protected SimulatorEngine engine;

	// Options:
	// Find deadlocks during model construction?
	protected boolean findDeadlocks = true;
	// Automatically fix deadlocks?
	protected boolean fixDeadlocks = true;

	// Details of built model
	protected List<State> statesList;

	public ConstructModel(PrismComponent parent, SimulatorEngine engine) throws PrismException
	{
		super(parent);
		this.engine = engine;
	}

	public List<State> getStatesList()
	{
		return statesList;
	}

	public void setFixDeadlocks(boolean b)
	{
		fixDeadlocks = b;
	}

	/**
	 * Build the set of reachable states for a PRISM model language description and return.
	 * @param modulesFile The PRISM model
	 */
	public List<State> computeReachableStates(ModulesFile modulesFile) throws PrismException
	{
		constructModel(modulesFile, true, false);
		return statesList;
	}

	/**
	 * Construct an explicit-state model from a PRISM model language description and return.
	 * @param modulesFile The PRISM model
	 */
	public Model constructModel(ModulesFile modulesFile) throws PrismException
	{
		return constructModel(modulesFile, false, true);
	}

	/**
	 * Construct an explicit-state model from a PRISM model language description and return.
	 * If {@code justReach} is true, no model is built and null is returned;
	 * the set of reachable states can be obtained with {@link #getStatesList()}.
	 * @param modulesFile The PRISM model
	 * @param justReach If true, just build the reachable state set, not the model
	 * @param buildSparse Build a sparse version of the model (if possible)?
	 */
	public Model constructModel(ModulesFile modulesFile, boolean justReach, boolean buildSparse) throws PrismException
	{
		return constructModel(modulesFile, justReach, buildSparse, true);
	}

	/**
	 * Construct an explicit-state model from a PRISM model language description and return.
	 * If {@code justReach} is true, no model is built and null is returned;
	 * the set of reachable states can be obtained with {@link #getStatesList()}.
	 * @param modulesFile The PRISM model
	 * @param justReach If true, just build the reachable state set, not the model
	 * @param buildSparse Build a sparse version of the model (if possible)?
	 * @param distinguishActions True if actions should be attached to distributions (and used to distinguish them)
	 */
	public Model constructModel(ModulesFile modulesFile, boolean justReach, boolean buildSparse, boolean distinguishActions) throws PrismException
	{
		// Model info
		ModelType modelType;
		// State storage
		StateStorage<State> states;
		LinkedList<State> explore;
		State state, stateNew;
		// Explicit model storage
		ModelSimple modelSimple = null;
		DTMCSimple dtmc = null;
		CTMCSimple ctmc = null;
		MDPSimple mdp = null;
		CTMDPSimple ctmdp = null;
		ModelExplicit model = null;
		Distribution distr = null;
		// Misc
		int i, j, nc, nt, src, dest;
		long timer;

		// Get model info
		modelType = modulesFile.getModelType();
		
		// Display a warning if there are unbounded vars
		VarList varList = modulesFile.createVarList();
		if (varList.containsUnboundedVariables())
			mainLog.printWarning("Model contains one or more unbounded variables: model construction may not terminate");

		// Starting reachability...
		mainLog.print("\nComputing reachable states...");
		mainLog.flush();
		ProgressDisplay progress = new ProgressDisplay(mainLog);
		progress.start();
		timer = System.currentTimeMillis();

		// Initialise simulator for this model
		engine.createNewOnTheFlyPath(modulesFile);

		// Create model storage
		if (!justReach) {
			// Create a (simple, mutable) model of the appropriate type
			switch (modelType) {
			case DTMC:
				modelSimple = dtmc = new DTMCSimple();
				break;
			case CTMC:
				modelSimple = ctmc = new CTMCSimple();
				break;
			case MDP:
				modelSimple = mdp = new MDPSimple();
				break;
			case CTMDP:
				modelSimple = ctmdp = new CTMDPSimple();
				break;
			case STPG:
			case SMG:
			case PTA:
				throw new PrismException("Model construction not supported for " + modelType + "s");
			}
		}

		// Initialise states storage
		states = new IndexedSet<State>(true);
		explore = new LinkedList<State>();
		// Add initial state(s) to 'explore'
		// Easy (normal) case: just one initial state
		if (modulesFile.getInitialStates() == null) {
			state = modulesFile.getDefaultInitialState();
			explore.add(state);
		}
		// Otherwise, there may be multiple initial states
		// For now, we handle this is in a very inefficient way
		else {
			Expression init = modulesFile.getInitialStates();
			List<State> allPossStates = varList.getAllStates();
			for (State possState : allPossStates) {
				if (init.evaluateBoolean(modulesFile.getConstantValues(), possState)) {
					explore.add(possState);
				}
			}
		}
		// Copy initial state(s) to 'states' and to the model
		for (State initState : explore) {
			states.add(initState);
			if (!justReach) {
				modelSimple.addState();
				modelSimple.addInitialState(modelSimple.getNumStates() - 1);
			}
		}
		// Explore...
		src = -1;
		while (!explore.isEmpty()) {
			// Pick next state to explore
			// (they are stored in order found so know index is src+1)
			state = explore.removeFirst();
			src++;
			// Use simulator to explore all choices/transitions from this state
			engine.initialisePath(state);
			// Look at each outgoing choice in turn
			nc = engine.getNumChoices();
			for (i = 0; i < nc; i++) {
				// For nondet models, collect transitions in a Distribution
				if (!justReach && modelType.nondeterministic()) {
					distr = new Distribution();
				}
				// Look at each transition in the choice
				nt = engine.getNumTransitions(i);
				for (j = 0; j < nt; j++) {
					stateNew = engine.computeTransitionTarget(i, j);
					// Is this a new state?
					if (states.add(stateNew)) {
						// If so, add to the explore list
						explore.add(stateNew);
						// And to model
						if (!justReach) {
							modelSimple.addState();
						}
					}
					// Get index of state in state set
					dest = states.getIndexOfLastAdd();
					// Add transitions to model
					if (!justReach) {
						switch (modelType) {
						case DTMC:
							dtmc.addToProbability(src, dest, engine.getTransitionProbability(i, j));
							break;
						case CTMC:
							ctmc.addToProbability(src, dest, engine.getTransitionProbability(i, j));
							break;
						case MDP:
						case CTMDP:
							distr.add(dest, engine.getTransitionProbability(i, j));
							break;
						case STPG:
						case SMG:
						case PTA:
							throw new PrismException("Model construction not supported for " + modelType + "s");
						}
					}
				}
				// For nondet models, add collated transition to model 
				if (!justReach) {
					if (modelType == ModelType.MDP) {
						if (distinguishActions) {
							mdp.addActionLabelledChoice(src, distr, engine.getTransitionAction(i, 0));
						} else {
							mdp.addChoice(src, distr);
						}
					} else if (modelType == ModelType.CTMDP) {
						if (distinguishActions) {
							ctmdp.addActionLabelledChoice(src, distr, engine.getTransitionAction(i, 0));
						} else {
							ctmdp.addChoice(src, distr);
						}
					}
				}
			}
			// Print some progress info occasionally
			progress.updateIfReady(src + 1);
		}

		// Finish progress display
		progress.update(src + 1);
		progress.end(" states");

		// Reachability complete
		mainLog.print("Reachable states exploration" + (justReach ? "" : " and model construction"));
		mainLog.println(" done in " + ((System.currentTimeMillis() - timer) / 1000.0) + " secs.");
		//mainLog.println(states);

		// Find/fix deadlocks (if required)
		if (!justReach && findDeadlocks) {
			modelSimple.findDeadlocks(fixDeadlocks);
		}

		boolean sort = true;
		int permut[] = null;

		if (sort) {
			// Sort states and convert set to list
			mainLog.println("Sorting reachable states list...");
			permut = states.buildSortingPermutation();
			statesList = states.toPermutedArrayList(permut);
			//mainLog.println(permut);
		} else {
			statesList = states.toArrayList();
		}
		states.clear();
		states = null;
		//mainLog.println(statesList);

		// Construct new explicit-state model (with correct state ordering)
		if (!justReach) {
			switch (modelType) {
			case DTMC:
				model = sort ? new DTMCSimple(dtmc, permut) : (DTMCSimple) dtmc;
				break;
			case CTMC:
				model = sort ? new CTMCSimple(ctmc, permut) : (CTMCSimple) ctmc;
				break;
			case MDP:
				if (buildSparse) {
					model = sort ? new MDPSparse(mdp, true, permut) : new MDPSparse(mdp);
				} else {
					model = sort ? new MDPSimple(mdp, permut) : mdp;
				}
				break;
			case CTMDP:
				model = sort ? new CTMDPSimple(ctmdp, permut) : mdp;
				break;
			case STPG:
			case SMG:
			case PTA:
				throw new PrismException("Model construction not supported for " + modelType + "s");
			}
			model.setStatesList(statesList);
			model.setConstantValues(new Values(modulesFile.getConstantValues()));
			//mainLog.println("Model: " + model);
		}

		// Discard permutation
		permut = null;

		return model;
	}

	/**
	 * Test method.
	 */
	public static void main(String[] args)
	{
		try {
			// Simple example: parse a PRISM file from a file, construct the model and export to a .tra file
			PrismLog mainLog = new PrismPrintStreamLog(System.out);
			Prism prism = new Prism(mainLog, mainLog);
			ModulesFile modulesFile = prism.parseModelFile(new File(args[0]));
			UndefinedConstants undefinedConstants = new UndefinedConstants(modulesFile, null);
			if (args.length > 2)
				undefinedConstants.defineUsingConstSwitch(args[2]);
			modulesFile.setUndefinedConstants(undefinedConstants.getMFConstantValues());
			ConstructModel constructModel = new ConstructModel(prism, prism.getSimulator());
			Model model = constructModel.constructModel(modulesFile);
			model.exportToPrismExplicitTra(args[1]);
		} catch (FileNotFoundException e) {
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		} catch (PrismException e) {
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
	}
}
