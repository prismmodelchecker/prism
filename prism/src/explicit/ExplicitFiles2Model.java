//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package explicit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import common.Interval;
import io.ExplicitModelImporter;
import parser.EvaluateContext;
import parser.State;
import parser.VarList;
import prism.Evaluator;
import prism.ModelInfo;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;

/**
 * Class to convert explicit-state file storage of a model to a model of the explicit engine.
 */
public class ExplicitFiles2Model extends PrismComponent
{
	// Should deadlocks be fixed (by adding a self-loop) when detected?
	private boolean fixdl;

	// Label bitsets
	private List<BitSet> labelBitSets;
	
	/** Constructor */
	public ExplicitFiles2Model(PrismComponent parent)
	{
		super(parent);
		if (settings != null) {
			setFixDeadlocks(settings.getBoolean(PrismSettings.PRISM_FIX_DEADLOCKS));
		}
	}

	/**
	 * Are deadlocks fixed (by adding a self-loop) when detected?
	 */
	public boolean getFixDeadlocks()
	{
		return fixdl;
	}

	/**
	 * Should deadlocks be fixed (by adding a self-loop) when detected?
	 */
	public void setFixDeadlocks(boolean fixdl)
	{
		this.fixdl = fixdl;
	}

	/**
	 * Build a Model corresponding to the passed in explicit files importer.
	 * The transition probabilities/rates are assumed to be of type double.
	 * @param  modelImporter Importer from files
	 */
	public Model<Double> build(ExplicitModelImporter modelImporter) throws PrismException
	{
		return build(modelImporter, Evaluator.forDouble());
	}

	/**
	 * Build a Model corresponding to the passed in explicit files importer.
	 * The transition probabilities/rates are assumed to be of type Value.
	 * @param  modelImporter Importer from files
	 * @param eval Evaluator for Value objects
	 */
	public <Value> Model<Value> build(ExplicitModelImporter modelImporter, Evaluator<Value> eval) throws PrismException
	{
		// Check model is defined as doubles
		if (modelImporter.modelIsExact() && !eval.exact()) {
			throw new PrismException("Cannot import an exact model unless in exact mode");
		}

		modelImporter.setFixDeadlocks(fixdl);
		ModelExplicit<Value> model = null;
		ModelInfo modelInfo = modelImporter.getModelInfo();
		boolean isDbl = eval.one() instanceof Double;
		switch (modelInfo.getModelType()) {
		case DTMC:
			DTMC<Value> dtmc = isDbl ? (DTMC<Value>) new DTMCSparse() : new DTMCSimple<>();
			model = (ModelExplicit<Value>) dtmc;
			break;
		case CTMC:
			CTMCSimple<Value> ctmc = new CTMCSimple<>();
			model = ctmc;
			break;
		case MDP:
			MDP<Value> mdp = isDbl ? (MDP<Value>) new MDPSparse() : new MDPSimple<>();
			model = (ModelExplicit<Value>) mdp;
			break;
		case POMDP:
			POMDP<Value> pomdp = new POMDPSimple<>();
			model = (ModelExplicit<Value>) pomdp;
			break;
		case IDTMC:
			IDTMCSimple<Value> idtmc = new IDTMCSimple<>();
			model = (ModelExplicit<Value>) idtmc;
			break;
		case IMDP:
			IMDPSimple<Value> imdp = new IMDPSimple<>();
			model = (ModelExplicit<Value>) imdp;
			break;
		case IPOMDP:
			IPOMDPSimple<Value> ipomdp = new IPOMDPSimple<>();
			model = (ModelExplicit<Value>) ipomdp;
			break;
		case LTS:
			LTS<Value> lts = new LTSSimple<>();
			model = (ModelExplicit<Value>) lts;
			break;
		case CTMDP:
		case PTA:
		case SMG:
		case STPG:
			throw new PrismNotSupportedException("Currently, importing " + modelInfo.getModelType() + " is not supported");
		}
		if (model == null) {
			throw new PrismException("Could not import " + modelInfo.getModelType());
		}
		model.setEvaluator(eval);
		if (model instanceof IntervalModelExplicit) {
			((IntervalModelExplicit<Value>) model).setIntervalEvaluator(eval.createIntervalEvaluator());
		}
		List<Object> actions = modelInfo.getActions();
		if (actions != null) {
			model.setActions(actions);
		}
		model.buildFromExplicitImport(modelImporter);

		if (model.getNumStates() == 0) {
			throw new PrismNotSupportedException("Imported model has no states, not supported");
		}

		loadLabelsAndInitialStates(modelImporter, model);
		if (!model.getInitialStates().iterator().hasNext()) {
			throw new PrismException("Imported model has no initial states");
		}
		BitSet deadlocks = modelImporter.getDeadlockStates();
		for (int s = deadlocks.nextSetBit(0); s >= 0; s = deadlocks.nextSetBit(s + 1)) {
			model.addDeadlockState(s);
		}

		loadStates(modelImporter, model);
		if (model.getModelType().partiallyObservable()) {
			loadObservationDefinitions(modelImporter, (PartiallyObservableModel<Value>) model);
		}

		return model;
	}

	/**
	 * Load the label information and attach to the model.
	 * The "init" label states become the initial states of the model.
	 * The "deadlock" label is ignored - this info is recomputed.
	 */
	private void loadLabelsAndInitialStates(ExplicitModelImporter modelImporter, ModelExplicit<?> model) throws PrismException
	{
		// Create BitSets to store label info
		ModelInfo modelInfo = modelImporter.getModelInfo();
		int numLabels = modelInfo.getNumLabels();
		labelBitSets = new ArrayList<>(numLabels);
		for (int l = 0; l < numLabels; l++) {
			labelBitSets.add(new BitSet());
		}
		// Extract info
		modelImporter.extractLabelsAndInitialStates((s, l) -> labelBitSets.get(l).set(s), model::addInitialState, model::addDeadlockState);
		// Attach labels to model
		for (int l = 0; l < numLabels; l++) {
			model.addLabel(modelInfo.getLabelName(l), labelBitSets.get(l));
		}
	}

	/**
	 * Load the state information, construct the statesList and attach to model
	 */
	private void loadStates(ExplicitModelImporter modelImporter, ModelExplicit<?> model) throws PrismException
	{
		int numStates = model.getNumStates();
		int numVars = modelImporter.getModelInfo().getNumVars();
		List<State> statesList = new ArrayList<>(numStates);
		ModelInfo modelInfo = modelImporter.getModelInfo();
		EvaluateContext.EvalMode evalMode = model.getEvaluator().evalMode();
		for (int i = 0; i < numStates; i++) {
			statesList.add(new State(numVars));
		}
		modelImporter.extractStates(
				(s, i, v) -> statesList.get(s).setValue(i, modelInfo.getVarType(i).castValueTo(v, evalMode))
		);
		model.setStatesList(statesList);
	}

	/**
	 * Load the observation information, and store in model
	 */
	private void loadObservationDefinitions(ExplicitModelImporter modelImporter, PartiallyObservableModel<?> model) throws PrismException
	{
		int numObservations = model.getNumObservations();
		int numObservables = modelImporter.getModelInfo().getNumObservables();
		List<State> observationsList = new ArrayList<>(numObservations);
		for (int i = 0; i < numObservations; i++) {
			observationsList.add(new State(numObservables));
		}
		modelImporter.extractObservationDefinitions((o, i, v) -> observationsList.get(o).setValue(i, v));
		model.setObservationsList(observationsList);
		List<State> statesList = model.getStatesList();
		if (statesList != null) {
			model.setUnobservationsList(new ArrayList<>(statesList));
		} else {
			throw new PrismException("Can't load observation definitions without a states list");
		}
	}
}
