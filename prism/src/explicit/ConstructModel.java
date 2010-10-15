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

		IndexedSet<State> states;
		LinkedList<State> explore;
		State state, stateNew;
		LabelList ll;

		ModelSimple model = null;
		DTMCSimple dtmc = null;
		CTMCSimple ctmc = null;
		MDPSimple mdp = null;
		Distribution distr = null;

		int i, j, k, nc, nt, nl, src, dest;
		boolean progressDisplayed;
		long timer, timerProgress;

		// Starting reachability...
		mainLog.println("\nExploring reachable state space...");
		timer = timerProgress = System.currentTimeMillis();
		progressDisplayed = false;

		// Initialise simulator for this model
		engine.createNewOnTheFlyPath(modulesFile);
		
		// Go through labels: create Bitsets and add to simulator
		ll = modulesFile.getLabelList();
		nl = ll.size();
		ArrayList<BitSet> labels = new ArrayList<BitSet>(nl);
		for (i = 0; i < nl; i++) {
			engine.addLabel(ll.getLabel(i));
			labels.add(new BitSet());
		}

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
			// TODO: remove this deadlock check (nc=0) hack
			for (k = 0; k < nl; k++) {
				if (nc == 0 && engine.queryLabel(k)) {
					labels.get(k).set(src);
				}
			}
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
			// Print some progress info occasionally
			if (System.currentTimeMillis() - timerProgress > 3000) {
				if (!progressDisplayed) {
					mainLog.print("Number of states so far:");
					progressDisplayed = true;
				}
				mainLog.print(" " + (src + 1));
				mainLog.flush();
				timerProgress = System.currentTimeMillis();
			}
		}

		// Tidy up progress display
		if (progressDisplayed)
			mainLog.println(" " + (src + 1));

		// Reachability complete
		mainLog.println("State exploration done in " + ((System.currentTimeMillis() - timer) / 1000.0) + " secs.");
		mainLog.println("States: " + (src + 1));

		//graph.states = states.toArrayList();

		int permut[] = states.buildSortingPermutation();
		//mainLog.println(permut);

		//mainLog.println("Model: " + model);

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

		//mainLog.println(states.size() + " states: " + states);
		//mainLog.println("Model: " + model);

		BitSet deadlocks = model.findDeadlocks(true);
		if (deadlocks.cardinality() > 0) {
			mainLog.println("Adding self-loops in " + deadlocks.cardinality() + " states...");
		}

		// Model construction complete
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Model construction done in " + (timer / 1000.0) + " secs.");
		
		// TODO: clean me up
		for (i = 0; i < nl; i++) {
			mainLog.println(labels.get(i));
			/*ll.getLabel(i);
			j = 0;
			for (Map.Entry<State, Integer> e : states.getEntrySet()) {
				mainLog.println(j + ":" + permut[j] + ":" + e.getValue() + " - ");
				j++;
			}*/
		}
		
		((CTMCSimple)model).exportToPrismLanguage("ctmc.sm");
		
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
