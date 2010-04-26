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
	private ModulesFile modulesFile;
	private ModelType modelType;
	private Values initialState;

	public ConstructModel(SimulatorEngine engine, PrismLog mainLog)
	{
		this.engine = engine;
		this.mainLog = mainLog;
	}

	public Model construct(ModulesFile modulesFile, Values initialState) throws PrismException
	{
		this.modulesFile = modulesFile;
		modelType = modulesFile.getModelType();
		this.initialState = initialState;

		int i, j, nc, nt, src, dest;
		IndexedSet<State> states;
		LinkedList<State> explore;
		State state, stateNew;

		ModelSimple model = null;
		DTMCSimple dtmc = null;
		CTMCSimple ctmc = null;
		MDPSimple mdp = null;
		Distribution distr = null;

		// Initialise simulator for this model
		engine.createNewOnTheFlyPath(modulesFile, null);

		// Create a (simple, mutable) model of the appropriate type
		switch (modelType) {
		case DTMC:
			model = dtmc = new DTMCSimple();
			break;
		case CTMC:
			model = ctmc = new CTMCSimple();
			break;
		case MDP:
			model = mdp = new MDPSimple();
			break;
		}

		// Initialise states storage
		states = new IndexedSet<State>(true);
		explore = new LinkedList<State>();
		// Add initial state to lists/model
		state = new State(modulesFile.getInitialValues());
		states.add(state);
		explore.add(state);
		model.addState();
		// Explore...
		src = -1;
		while (!explore.isEmpty()) {
			// Pick next state to explore
			// (they are stored in order found so know index is src+1)
			state = explore.removeFirst();
			src++;
			// Use simulator to explore all choices/transitions from this state
			engine.initialisePath(state);
			nc = engine.getNumChoices();
			for (i = 0; i < nc; i++) {
				if (modelType == ModelType.MDP) {
					distr = new Distribution();
				}
				nt = engine.getNumTransitions(i);
				for (j = 0; j < nt; j++) {
					stateNew = engine.computeTransitionTarget(i, j);
					// Is this a new state?
					if (states.add(stateNew)) {
						// If so, add to the explore list
						explore.add(stateNew);
						// And to model
						model.addState();
					}
					// Get index of state in state set
					dest = states.getIndexOfLastAdd();
					// Add transitions to model
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
					}
				}
				if (modelType == ModelType.MDP) {
					mdp.addChoice(src, distr);
				}
			}
		}

		//graph.states = states.toArrayList();

		int permut[] = states.buildSortingPermutation();
		mainLog.println(permut);

		mainLog.println("Model: " + model);

		switch (modelType) {
		case DTMC:
			model = dtmc = new DTMCSimple(dtmc, permut);
			break;
		case CTMC:
			model = ctmc = new CTMCSimple(ctmc, permut);
			break;
		case MDP:
			model = mdp = new MDPSimple(mdp, permut);
			break;
		}

		mainLog.println(states.size() + " states: " + states);
		mainLog.println("Model: " + model);

		BitSet deadlocks = model.findDeadlocks(true);
		if (deadlocks.cardinality() > 0) {
			mainLog.println("Adding self-loops in " + deadlocks.cardinality() + " states...");
		}

		return model;
	}

	/**
	 * Test method.
	 */
	public static void main(String[] args)
	{
		try {
			PrismLog mainLog = new PrismPrintStreamLog(System.out);
			Prism prism = new Prism(mainLog, mainLog);
			ModulesFile modulesFile = prism.parseModelFile(new File(args[0]));
			UndefinedConstants undefinedConstants = new UndefinedConstants(modulesFile, null);
			if (args.length > 2)
				undefinedConstants.defineUsingConstSwitch(args[2]);
			modulesFile.setUndefinedConstants(undefinedConstants.getMFConstantValues());
			ConstructModel constructModel = new ConstructModel(prism.getSimulator(), mainLog);
			Model model = constructModel.construct(modulesFile, modulesFile.getInitialValues());
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
