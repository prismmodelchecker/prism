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
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import parser.State;
import parser.Values;
import parser.VarList;
import prism.ModelGenerator;
import prism.ModelType;
import prism.Prism;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismNotSupportedException;
import prism.PrismPrintStreamLog;
import prism.ProgressDisplay;
import prism.UndefinedConstants;

/**
 * Class to perform explicit-state reachability and model construction.
 * The information about the model to be built is provided via a {@link prism.ModelGenerator} interface.
 * To build a PRISM model, use {@link simulator.ModulesFileModelGenerator}.
 */
public class ConstructModel extends PrismComponent
{
	// Options:

	/** Find deadlocks during model construction? */
	protected boolean findDeadlocks = true;
	/** Automatically fix deadlocks? */
	protected boolean fixDeadlocks = true;
	/** Sort the reachable states before constructing the model? */
	protected boolean sortStates = true;
	/** Build a sparse representation, if possible?
	 *  (e.g. MDPSparse rather than MDPSimple data structure) */
	protected boolean buildSparse = true;
	/** Should actions be attached to distributions (and used to distinguish them)? */
	protected boolean distinguishActions = true;
	/** Should labels be processed and attached to the model? */
	protected boolean attachLabels = true;

	// Details of built model:

	/** Reachable states */
	protected List<State> statesList;

	public ConstructModel(PrismComponent parent) throws PrismException
	{
		super(parent);
	}

	/**
	 * Get the list of states associated with the last model construction performed.  
	 */
	public List<State> getStatesList()
	{
		return statesList;
	}

	/**
	 * Automatically fix deadlocks, if needed?
	 * (by adding self-loops in those states)
	 */
	public void setFixDeadlocks(boolean fixDeadlocks)
	{
		this.fixDeadlocks = fixDeadlocks;
	}

	/**
	 * Sort the reachable states before constructing the model?
	 */
	public void setSortStates(boolean sortStates)
	{
		this.sortStates = sortStates;
	}

	/**
	 * Build a sparse representation, if possible?
	 * (e.g. MDPSparse rather than MDPSimple data structure)
	 */
	public void setBuildSparse(boolean buildSparse)
	{
		this.buildSparse = buildSparse;
	}

	/**
	 * Should actions be attached to distributions (and used to distinguish them)?
	 */
	public void setDistinguishActions(boolean distinguishActions)
	{
		this.distinguishActions = distinguishActions;
	}

	/**
	 * Should labels be processed and attached to the model?
	 */
	public void setAttachLabels(boolean attachLabels)
	{
		this.attachLabels = attachLabels;
	}

	/**
	 * Build the set of reachable states for a model and return it.
	 * @param modelGen The ModelGenerator interface providing the model 
	 */
	public List<State> computeReachableStates(ModelGenerator modelGen) throws PrismException
	{
		constructModel(modelGen, true);
		return getStatesList();
	}

	/**
	 * Construct an explicit-state model and return it.
	 * @param modelGen The ModelGenerator interface providing the model 
	 */
	public Model constructModel(ModelGenerator modelGen) throws PrismException
	{
		return constructModel(modelGen, false);
	}

	/**
	 * Construct an explicit-state model and return it.
	 * If {@code justReach} is true, no model is built and null is returned;
	 * the set of reachable states can be obtained with {@link #getStatesList()}.
	 * @param modelGen The ModelGenerator interface providing the model 
	 * @param justReach If true, just build the reachable state set, not the model
	 */
	public Model constructModel(ModelGenerator modelGen, boolean justReach) throws PrismException
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
		POMDPSimple pomdp = null;
		CTMDPSimple ctmdp = null;
		LTSSimple lts = null;
		ModelExplicit model = null;
		Distribution distr = null;
		// Misc
		int i, j, nc, nt, src, dest;
		long timer;

		// Get model info
		modelType = modelGen.getModelType();
		
		// Display a warning if there are unbounded vars
		VarList varList = modelGen.createVarList();
		if (modelGen.containsUnboundedVariables())
			mainLog.printWarning("Model contains one or more unbounded variables: model construction may not terminate");

		// Starting reachability...
		mainLog.print("\nComputing reachable states...");
		mainLog.flush();
		ProgressDisplay progress = new ProgressDisplay(mainLog);
		progress.start();
		timer = System.currentTimeMillis();

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
			case POMDP:
				modelSimple = pomdp = new POMDPSimple();
				break;
			case CTMDP:
				modelSimple = ctmdp = new CTMDPSimple();
				break;
			case LTS:
				modelSimple = lts = new LTSSimple();
				break;
			case STPG:
			case SMG:
			case PTA:
			case POPTA:
				throw new PrismNotSupportedException("Model construction not supported for " + modelType + "s");
			}
			// Attach variable info
	        ((ModelExplicit) modelSimple).setVarList(varList);
		}

		// Initialise states storage
		states = new IndexedSet<State>(true);
		explore = new LinkedList<State>();
		// Add initial state(s) to 'explore', 'states' and to the model
		for (State initState : modelGen.getInitialStates()) {
			explore.add(initState);
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
			// Explore all choices/transitions from this state
			modelGen.exploreState(state);
			// Look at each outgoing choice in turn
			nc = modelGen.getNumChoices();
			for (i = 0; i < nc; i++) {
				// If required, check for duplicate actions here
				if (modelType.partiallyObservable()) {
					if (((NondetModel) modelSimple).getChoiceByAction(src, modelGen.getChoiceAction(i)) != -1) {
						String act = modelGen.getChoiceAction(i) == null ? "" : modelGen.getChoiceAction(i).toString();
						String err = modelType + " is not allowed duplicate action";
						err += " (\"" + act + "\") in state " + state.toString(modelGen);
						throw new PrismException(err);
					}
				}
				// For nondet models, collect transitions in a Distribution
				if (!justReach && modelType.nondeterministic()) {
					distr = new Distribution();
				}
				// Look at each transition in the choice
				nt = modelGen.getNumTransitions(i);
				for (j = 0; j < nt; j++) {
					stateNew = modelGen.computeTransitionTarget(i, j);
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
							dtmc.addToProbability(src, dest, modelGen.getTransitionProbability(i, j));
							break;
						case CTMC:
							ctmc.addToProbability(src, dest, modelGen.getTransitionProbability(i, j));
							break;
						case MDP:
						case POMDP:
						case CTMDP:
							distr.add(dest, modelGen.getTransitionProbability(i, j));
							break;
						case LTS:
							if (distinguishActions) {
								lts.addActionLabelledTransition(src, dest, modelGen.getChoiceAction(i));
							} else {
								lts.addTransition(src, dest);
							}
							break;
						case STPG:
						case SMG:
						case PTA:
							throw new PrismNotSupportedException("Model construction not supported for " + modelType + "s");
						}
					}
				}
				// For nondet models, add collated transition to model 
				if (!justReach) {
					if (modelType == ModelType.MDP) {
						if (distinguishActions) {
							mdp.addActionLabelledChoice(src, distr, modelGen.getChoiceAction(i));
						} else {
							mdp.addChoice(src, distr);
						}
					} else if (modelType == ModelType.POMDP) {
						if (distinguishActions) {
							pomdp.addActionLabelledChoice(src, distr, modelGen.getChoiceAction(i));
						} else {
							pomdp.addChoice(src, distr);
						}
					} else if (modelType == ModelType.CTMDP) {
						if (distinguishActions) {
							ctmdp.addActionLabelledChoice(src, distr, modelGen.getChoiceAction(i));
						} else {
							ctmdp.addChoice(src, distr);
						}
					}
				}
			}
			// For partially observable models, add observation info to state
			// (do it after transitions are added, since observation actions are checked)
			if (!justReach && modelType == ModelType.POMDP) {
				setStateObservation(modelGen, (POMDPSimple) modelSimple, src, state);
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

		int permut[] = null;

		if (sortStates) {
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

		// Construct new explicit-state model (with correct state ordering, if desired)
		if (!justReach) {
			switch (modelType) {
			case DTMC:
				if (buildSparse) {
					model = sortStates ? new DTMCSparse(dtmc, permut) : new DTMCSparse(dtmc);
				} else {
					model = sortStates ? new DTMCSimple(dtmc, permut) : (DTMCSimple) dtmc;
				}
				break;
			case CTMC:
				model = sortStates ? new CTMCSimple(ctmc, permut) : (CTMCSimple) ctmc;
				break;
			case MDP:
				if (buildSparse) {
					model = sortStates ? new MDPSparse(mdp, true, permut) : new MDPSparse(mdp);
				} else {
					model = sortStates ? new MDPSimple(mdp, permut) : mdp;
				}
				break;
			case POMDP:
				model = sortStates ? new POMDPSimple(pomdp, permut) : pomdp;
				break;
			case CTMDP:
				model = sortStates ? new CTMDPSimple(ctmdp, permut) : ctmdp;
				break;
			case LTS:
				model = sortStates ? new LTSSimple(lts, permut) : lts;
				break;
			case STPG:
			case SMG:
			case PTA:
				throw new PrismNotSupportedException("Model construction not supported for " + modelType + "s");
			}
			model.setStatesList(statesList);
			model.setConstantValues(new Values(modelGen.getConstantValues()));
			//mainLog.println("Model: " + model);
		}

		// Discard permutation
		permut = null;

		if (!justReach && attachLabels)
			attachLabels(modelGen, model);
		
		return model;
	}

	private void setStateObservation(ModelGenerator modelGen, POMDPSimple pomdp, int s, State state) throws PrismException
	{
		// Get observation for the current state
		// An observation is a State containing the value for each observable
		State sObs = modelGen.getObservation(state);
		// Build unobservation for the current state
		// An unobservation is a State containing the value for
		// all variables that are not observable
		int numVars = modelGen.getNumVars();
		int numUnobsVars = numVars - modelGen.getNumObservableVars();
		State sUnobs = new State(numUnobsVars);
		int count = 0;
		for (int i = 0; i < numVars; i++) {
			if (!modelGen.isVarObservable(i)) {
				sUnobs.setValue(count++, state.varValues[i]);
			}
		}
		// Set observation/unobservation
		pomdp.setObservation(s, sObs, sUnobs, modelGen.getObservableNames());
	}
	
	private void attachLabels(ModelGenerator modelGen, ModelExplicit model) throws PrismException
	{
		// Get state info
		List <State> statesList = model.getStatesList();
		int numStates = statesList.size();
		// Create storage for labels
		int numLabels = modelGen.getNumLabels();
		// No need to continue unless this ModelGenerator uses labels
		if (numLabels == 0) return;
		BitSet bitsets[] = new BitSet[numLabels];
		for (int j = 0; j < numLabels; j++) {
			bitsets[j] = new BitSet();
		}
		// Construct bitsets for labels
		for (int i = 0; i < numStates; i++) {
			State state = statesList.get(i);
			modelGen.exploreState(state);
			for (int j = 0; j < numLabels; j++) {
				if (modelGen.isLabelTrue(j)) {
					bitsets[j].set(i);
				}
			}
		}
		// Attach labels/bitsets
		for (int j = 0; j < numLabels; j++) {
			model.addLabel(modelGen.getLabelName(j), bitsets[j]);
		}
	}

	/**
	 * Test method.
	 */
	public static void main(String[] args)
	{
		try {
			// Simple example: parse a PRISM file from a file, construct the model and export to a .tra file
			PrismLog mainLog = new PrismPrintStreamLog(System.out);
			Prism prism = new Prism(mainLog);
			parser.ast.ModulesFile modulesFile = prism.parseModelFile(new File(args[0]));
			UndefinedConstants undefinedConstants = new UndefinedConstants(modulesFile, null);
			if (args.length > 2)
				undefinedConstants.defineUsingConstSwitch(args[2]);
			modulesFile.setUndefinedConstants(undefinedConstants.getMFConstantValues());
			ConstructModel constructModel = new ConstructModel(prism);
			constructModel.setSortStates(true);
			simulator.ModulesFileModelGenerator modelGen = new simulator.ModulesFileModelGenerator(modulesFile, constructModel);
			Model model = constructModel.constructModel(modelGen);
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
