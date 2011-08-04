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

import java.io.*;
import java.util.*;

import parser.*;
import parser.ast.*;
import prism.*;
import simulator.*;

public class ConstructModel
{
	// The simulator engine and a log for output
	private SimulatorEngine engine;
	private PrismLog mainLog;

	// Basic info needed about model
	//	private ModelType modelType;

	// Details of built model
	private List<State> statesList;

	public ConstructModel(SimulatorEngine engine, PrismLog mainLog)
	{
		this.engine = engine;
		this.mainLog = mainLog;
	}

	public List<State> getStatesList()
	{
		return statesList;
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
		return constructModel(modulesFile, false, false, true);
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
	 * @param distinguishActions True if the distributions with different action should be added to the model as separate ones.
	 */
	public Model constructModel(ModulesFile modulesFile, boolean justReach, boolean buildSparse, boolean distinguishActions) throws PrismException
	{
		// Model info
		ModelType modelType;
		// State storage
		IndexedSet<State> states;
		LinkedList<State> explore;
		State state, stateNew;
		// Explicit model storage
		ModelSimple modelSimple = null;
		DTMCSimple dtmc = null;
		CTMCSimple ctmc = null;
		MDPSimple mdp = null;
		CTMDPSimple ctmdp = null;
		Model model = null;
		Distribution distr = null;
		// Misc
		int i, j, k, nc, nt, src, dest;
		long timer, timerProgress;
		boolean fixdl = false;

		// Don't support multiple initial states
		if (modulesFile.getInitialStates() != null) {
			throw new PrismException("Cannot do explicit-state reachability if there are multiple initial states");
		}

		// Starting reachability...
		mainLog.print("\nComputing reachable states...");
		mainLog.flush();
		timer = timerProgress = System.currentTimeMillis();

		// Initialise simulator for this model
		modelType = modulesFile.getModelType();
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
			}
		}

		// Initialise states storage
		states = new IndexedSet<State>(true);
		explore = new LinkedList<State>();
		// Add initial state to lists/model
		if (modulesFile.getInitialStates() != null) {
			throw new PrismException("Explicit model construction does not support multiple initial states");
		}
		state = modulesFile.getDefaultInitialState();
		states.add(state);
		explore.add(state);
		if (!justReach) {
			modelSimple.addState();
			modelSimple.addInitialState(0);
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
				if (!justReach && (modelType == ModelType.MDP || modelType == ModelType.CTMDP)) {
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
						if (!justReach)
							modelSimple.addState();
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
							distr.add(dest, engine.getTransitionProbability(i, j));
							break;
						case CTMDP:
							distr.add(dest, engine.getTransitionProbability(i, j));
							break;
						}
					}
				}
				if (!justReach) {
					if (modelType == ModelType.MDP) {
						if (distinguishActions) {
							k = mdp.addActionLabelledChoice(src, distr, engine.getTransitionAction(i, 0));
							mdp.setAction(src, k, engine.getTransitionAction(i, 0));
						} else {
							k = mdp.addChoice(src, distr);
						}
					} else if (modelType == ModelType.CTMDP) {
						ctmdp.addChoice(src, distr);
					}
				}
			}
			// Print some progress info occasionally
			if (System.currentTimeMillis() - timerProgress > 3000) {
				mainLog.print(" " + (src + 1));
				mainLog.flush();
				timerProgress = System.currentTimeMillis();
			}
		}

		// Finish progress display
		mainLog.println(" " + (src + 1));

		// Reachability complete
		mainLog.print("Reachable states exploration" + (justReach ? "" : " and model construction"));
		mainLog.println(" done in " + ((System.currentTimeMillis() - timer) / 1000.0) + " secs.");
		//mainLog.println(states);

		// Fix deadlocks (if required)
		if (!justReach && fixdl) {
			BitSet deadlocks = modelSimple.findDeadlocks(true);
			if (deadlocks.cardinality() > 0) {
				mainLog.println("Added self-loops in " + deadlocks.cardinality() + " states...");
			}
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
				((ModelSimple) model).statesList = statesList;
				((ModelSimple) model).constantValues = new Values(modulesFile.getConstantValues());
				break;
			case CTMC:
				model = sort ? new CTMCSimple(ctmc, permut) : (CTMCSimple) ctmc;
				((ModelSimple) model).statesList = statesList;
				((ModelSimple) model).constantValues = new Values(modulesFile.getConstantValues());
				break;
			case MDP:
				if (buildSparse) {
					model = sort ? new MDPSparse(mdp, true, permut) : new MDPSparse(mdp);
					((ModelSparse) model).statesList = statesList;
					((ModelSparse) model).constantValues = new Values(modulesFile.getConstantValues());
				} else {
					model = sort ? new MDPSimple(mdp, permut) : mdp;
					((ModelSimple) model).statesList = statesList;
					((ModelSimple) model).constantValues = new Values(modulesFile.getConstantValues());
				}
				break;
			case CTMDP:
				model = sort ? new CTMDPSimple(ctmdp, permut) : mdp;
				((ModelSimple) model).statesList = statesList;
				((ModelSimple) model).constantValues = new Values(modulesFile.getConstantValues());
				break;
			}
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
			ConstructModel constructModel = new ConstructModel(prism.getSimulator(), mainLog);
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
