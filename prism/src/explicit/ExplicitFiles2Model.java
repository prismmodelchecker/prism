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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import common.Interval;
import io.ExplicitModelImporter;
import io.PrismExplicitImporter;
import parser.State;
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
	public Model<Double> build(PrismExplicitImporter modelImporter) throws PrismException
	{
		return build(modelImporter, Evaluator.forDouble());
	}

	/**
	 * Build a Model corresponding to the passed in explicit files importer.
	 * The transition probabilities/rates are assumed to be of type Value.
	 * @param  modelImporter Importer from files
	 * @param eval Evaluator for Value objects
	 */
	public <Value> Model<Value> build(PrismExplicitImporter modelImporter, Evaluator<Value> eval) throws PrismException
	{
		ModelExplicit<Value> model = null;
		ModelInfo modelInfo = modelImporter.getModelInfo();
		switch (modelInfo.getModelType()) {
		case DTMC:
			DTMCSimple<Value> dtmc = new DTMCSimple<>();
			model = dtmc;
			break;
		case CTMC:
			CTMCSimple<Value> ctmc = new CTMCSimple<>();
			model = ctmc;
			break;
		case MDP:
			MDPSimple<Value> mdp = new MDPSimple<>();
			model = mdp;
			break;
		case IDTMC:
			IDTMCSimple<Value> idtmc = new IDTMCSimple<>();
			model = (ModelExplicit<Value>) idtmc;
			break;
		case IMDP:
			IMDPSimple<Value> imdp = new IMDPSimple<>();
			model = (ModelExplicit<Value>) imdp;
			break;
		case CTMDP:
		case LTS:
		case PTA:
		case SMG:
		case STPG:
			throw new PrismNotSupportedException("Currently, importing " + modelInfo.getModelType() + " is not supported");
		}
		if (model == null) {
			throw new PrismException("Could not import " + modelInfo.getModelType());
		}
		model.setEvaluator(eval);
		if (!model.getModelType().uncertain()) {
			model.setEvaluator(eval);
		} else {
			((ModelExplicit<Interval<Value>>) model).setEvaluator(eval.createIntervalEvaluator());
		}
		model.buildFromExplicitImport(modelImporter);

		if (model.getNumStates() == 0) {
			throw new PrismNotSupportedException("Imported model has no states, not supported");
		}

		loadLabelsAndInitialStates(modelImporter, model);
		if (!model.getInitialStates().iterator().hasNext()) {
			throw new PrismException("Imported model has no initial states");
		}

		model.findDeadlocks(fixdl);
		
		if (modelImporter.hasStatesFile()) {
			loadStates(model, modelImporter.getStatesFile(), modelInfo);
		} else {
			// in absence of a statesFile, there is a single variable x
			// in the model, with value corresponding to the state index
			List<State> states = new ArrayList<State>(model.getNumStates());
			for (int i = 0; i < model.getNumStates(); i++) {
				State s = new State(1);
				s.setValue(0, i); // set x = state index
				states.add(s);
			}
			model.setStatesList(states);
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
		modelImporter.extractLabelsAndInitialStates((s, l) -> labelBitSets.get(l).set(s), model::addInitialState);
		// Attach labels to model
		for (int l = 0; l < numLabels; l++) {
			model.addLabel(modelInfo.getLabelName(l), labelBitSets.get(l));
		}
	}

	/** Load the state information, construct the statesList and attach to model */
	private void loadStates(ModelExplicit<?> model, File statesFile, ModelInfo modelInfo) throws PrismException
	{
		int numStates = model.getNumStates();
		List<State> statesList = new ArrayList<State>(numStates);
		for (int i = 0; i < numStates; i++) {
			statesList.add(null);
		}

		String s, ss[];
		int i, j, lineNum = 0;

		int numVars = modelInfo.getNumVars();

		// open file for reading, automatic close when done
		try (BufferedReader in = new BufferedReader(new FileReader(statesFile))) {
			// skip first line
			in.readLine();
			lineNum = 1;
			// read remaining lines
			s = in.readLine();
			lineNum++;
			while (s != null) {
				// skip blank lines
				s = s.trim();
				if (s.length() > 0) {
					// split into two parts
					ss = s.split(":");
					// determine which state this line describes
					i = Integer.parseInt(ss[0]);
					// now split up middle bit and extract var info
					ss = ss[1].substring(ss[1].indexOf('(') + 1, ss[1].indexOf(')')).split(",");

					State state = new State(numVars);
					if (ss.length != numVars)
						throw new PrismException("(wrong number of variable values) ");
					for (j = 0; j < numVars; j++) {
						if (ss[j].equals("true")) {
							state.setValue(j, true);
						} else if (ss[j].equals("false")) {
							state.setValue(j, false);
						} else {
							state.setValue(j, Integer.parseInt(ss[j]));
						}
					}
					if (statesList.get(i) != null)
						throw new PrismException("(duplicated state) ");
					statesList.set(i, state);
				}
				// read next line
				s = in.readLine();
				lineNum++;
			}
			model.setStatesList(statesList);
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + statesFile + "\"");
		} catch (PrismException e) {
			throw new PrismException("Error detected " + e.getMessage() + "at line " + lineNum + " of states file \"" + statesFile + "\"");
		}
	}
}
