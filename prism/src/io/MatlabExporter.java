//==============================================================================
//
//	Copyright (c) 2024-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

package io;

import explicit.DTMC;
import explicit.LTS;
import explicit.MDP;
import explicit.Model;
import explicit.NondetModel;
import explicit.PartiallyObservableModel;
import explicit.SuccessorsIterator;
import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;
import parser.State;
import parser.VarList;
import prism.Evaluator;
import io.ModelExportOptions;
import prism.ModelInfo;
import prism.ModelType;
import prism.Pair;
import prism.Prism;
import prism.PrismException;
import prism.PrismLog;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Class to manage export of built models to Matlab format.
 */
public class MatlabExporter<Value> extends Exporter<Value>
{
	public MatlabExporter()
	{
		super();
	}

	public MatlabExporter(ModelExportOptions modelExportOptions)
	{
		super(modelExportOptions);
	}

	/**
	 * Export the states for a model.
	 * @param model The model
	 * @param varList The VarList for the model
	 * @param out Where to export
	 */
	public void exportStates(Model<Value> model, VarList varList, PrismLog out) throws PrismException
	{
		List<State> statesList = model.getStatesList();
		if (statesList == null)
			return;

		// Print header: list of model vars
		out.print("% (");
		int numVars = varList.getNumVars();
		for (int i = 0; i < numVars; i++) {
			out.print(varList.getName(i));
			if (i < numVars - 1)
				out.print(",");
		}
		out.println(")");

		// Print states
		out.println("states=[");
		int numStates = statesList.size();
		for (int i = 0; i < numStates; i++) {
			out.println(statesList.get(i).toStringNoParentheses());
		}

		// Print footer
		out.println("];");
	}

	/**
	 * Export the observations for a (partially observable) model.
	 * @param model The model
	 * @param modelInfo The ModelInfo for the model
	 * @param out Where to export
	 */
	public void exportObservations(PartiallyObservableModel<Value> model, ModelInfo modelInfo, PrismLog out) throws PrismException
	{
		List<State> observationsList =  model.getObservationsList();

		// Print header: list of observables
		out.print("% (");
		int numObservables = modelInfo.getNumObservables();
		for (int i = 0; i < numObservables; i++) {
			out.print(modelInfo.getObservableName(i));
			if (i < numObservables - 1)
				out.print(",");
		}
		out.println(")");

		// Print states + observations
		out.println("obs=[");
		int numObservations = model.getNumObservations();
		for (int i = 0; i < numObservations; i++) {
			out.println(observationsList.get(i).toStringNoParentheses());
		}

		// Print footer
		out.println("];");
	}

	/**
	 * Export a set of labels and the states that satisfy them.
	 * @param model The model
	 * @param labelNames The names of the labels to export
	 * @param labelStates The states that satisfy each label, specified as a BitSet
	 * @param out Where to export
	 */
	public void exportLabels(Model<Value> model, List<String> labelNames, List<BitSet> labelStates, PrismLog out) throws PrismException
	{
		// Get model info and exportOptions
		setEvaluator(model.getEvaluator());
		int numStates = model.getNumStates();

		// Print list of labels
		String varName = "l";
		int numLabels = labelNames.size();
		for (int s = 0; s < numLabels; s++) {
			out.println(varName + "_" + labelNames.get(s) + "=sparse(" + numStates + ",1);");
		}
		out.println();

		// Go through states and print satisfying label indices for each one
		for (int s = 0; s < numStates; s++) {
			boolean first = true;
			for (int i = 0; i < numLabels; i++) {
				if (labelStates.get(i).get(s)) {
					out.println(varName + "_" + labelNames.get(i) + "(" + (s + 1) + ")=1;");
				}
			}
		}
	}
}
