//==============================================================================
//	
//	Copyright (c) 2022-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

package pta;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;

import explicit.IndexedSet;
import explicit.StateStorage;
import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.Expression;
import parser.type.TypeClock;
import prism.ModelGenerator;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import prism.ProgressDisplay;

/**
 * Class to construct a PTA from a {@link prism.ModelGenerator} object.
 */
public class ConstructPTA extends PrismComponent
{
	/**
	 * Constructor.
	 */
	public ConstructPTA(PrismComponent parent) throws PrismException
	{
		super(parent);
	}

	/**
	 * Construct a PTA from a {@link prism.ModelGenerator} object.
	 * This is done somewhat symbolically, in the sense that all reachable locations
	 * are explored, ignoring possible clock values. Invariants, guards and resets
	 * relating to clocks are extracted and stored in the PTA.
	 */
	public PTA constructPTA(ModelGenerator<Double> modelGen) throws PrismException
	{
		// Do a few basic checks on the model
		if (modelGen.getModelType() != ModelType.PTA) {
			throw new PrismLangException("Model is not a PTA");
		}
		// We only support PTAs with a single initial state
		if (!modelGen.hasSingleInitialState()) {
			throw new PrismLangException("Cannot construct PTA models with multiple initial states");
		}
		
		// Extract some info from the model generator
		Values constantValues  = modelGen.getConstantValues();
		VarList varList = modelGen.createVarList();
		
		// Starting reachability...
		mainLog.print("\nComputing reachable locations of " + modelGen.getModelType() + "...");
		mainLog.flush();
		ProgressDisplay progress = new ProgressDisplay(mainLog);
		progress.start();
		long timer = System.currentTimeMillis();

		// Get list of clock (and all) variables
		int numVars = varList.getNumVars();
		ArrayList<String> clocks = new ArrayList<String>();
		ArrayList<String> varNames = new ArrayList<String>();
		for (int i = 0; i < numVars; i++) {
			if (varList.getType(i) instanceof TypeClock) {
				clocks.add(varList.getName(i));
			}
			varNames.add(varList.getName(i));
		}
		
		// Create new PTA and add a clock for each clock variable
		// (note we construct a single global PTA, so no need for action alphabet)
		PTA pta = new PTA(new ArrayList<String>(new Vector<>()));
		for (String clockName : clocks) {
			pta.addClock(clockName);
		}
		
		// Initialise states storage
		StateStorage<State> states = new IndexedSet<State>(true);
		LinkedList<State> explore = new LinkedList<State>();
		// Add initial state(s) to 'explore', 'states' and to the model
		for (State initState : modelGen.getInitialStates()) {
			// We set clock variable values to null
			// (so we can tell if they are reset in transitions)
			for (String clock : clocks) {
				initState.varValues[varList.getIndex(clock)] = null;
			}
			explore.add(initState);
			states.add(initState);
			pta.addLocation(initState);
		}
		// Explore...
		int src = -1;
		while (!explore.isEmpty()) {
			// Pick next state to explore
			// (they are stored in order found so know index is src+1)
			State state = explore.removeFirst();
			src++;
			// Explore all choices/transitions from this state
			modelGen.exploreState(state);
			// Get/store invariant (should be syntactically convex, i.e., conjunction of
			// clock constraints, or true; complain if not)
			Expression invar = modelGen.getClockInvariant();
			if (invar != null) {
				final int srcFinal = src;
				PTAUtils.exprConjToConstraintConsumer(invar, constantValues, pta, c -> { pta.addInvariantCondition(srcFinal, c); });
			}
			// Look at each outgoing choice in turn
			int nc = modelGen.getNumChoices();
			for (int i = 0; i < nc; i++) {
				final Transition tr = pta.addTransition(src, modelGen.getChoiceActionString(i));
				// Get/store guard (should be syntactically convex, i.e., conjunction of
				// clock constraints, or true; complain if not)
				PTAUtils.exprConjToConstraintConsumer(modelGen.getChoiceClockGuard(i), constantValues, pta, c -> { tr.addGuardConstraint(c); });
				// Look at each transition in the choice
				int nt = modelGen.getNumTransitions(i);
				for (int j = 0; j < nt; j++) {
					// Create edge
					Edge edge = tr.addEdge(modelGen.getTransitionProbability(i, j), -1);
					State stateNew = modelGen.computeTransitionTarget(i, j);
					// See which clocks are reset on this edge, and store
					for (String clock : clocks) {
						int index = varList.getIndex(clock);
						Object newClockVal = stateNew.varValues[index];
						if (newClockVal != null) {
							// (was reset to int, but stored as double) 
							int newClockValInt = (int) Math.round((Double) newClockVal);
							edge.addReset(pta.getClockIndex(clock), newClockValInt);
							// Now set clock variable values to null
							// (so we can tell if they are reset in future transitions)
							stateNew.varValues[index] = null;
						}
					}
					// Is this a new state?
					if (states.add(stateNew)) {
						// If so, add to the explore list and model
						explore.add(stateNew);
						pta.addLocation(stateNew);
					}
					// Get index of state in state set (NB: should match return from pta.addLocation)
					int dest = states.getIndexOfLastAdd();
					edge.setDestination(dest);
				}
			}
			 // Print some progress info occasionally
			 progress.updateIfReady(src + 1);
		}
		
		// Finish progress display
		progress.update(src + 1);
		progress.end(" locations");

		// Reachability complete
		mainLog.print("Reachable locations exploration and model construction");
		mainLog.println(" done in " + ((System.currentTimeMillis() - timer) / 1000.0) + " secs.");

		// Pass the list of variable names to the PTA
		pta.setLocationNameVars(varNames);
		
		return pta;
	}
}
