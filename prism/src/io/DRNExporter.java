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

import explicit.CTMC;
import explicit.Model;
import explicit.NondetModel;
import explicit.PartiallyObservableModel;
import explicit.rewards.Rewards;
import prism.Evaluator;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismUtils;
import prism.RewardGenerator;

import java.util.BitSet;
import java.util.List;

/**
 * Class to manage export of built models to Storm's DRN file format.
 */
public class DRNExporter<Value> extends ModelExporter<Value>
{
	/**
	 * Construct a DRNExporter with default export options.
	 */
	public DRNExporter()
	{
		super();
	}

	/**
	 * Construct a DRNExporter with the specified export options.
	 */
	public DRNExporter(ModelExportOptions modelExportOptions)
	{
		super(modelExportOptions);
	}

	@Override
	public void exportModel(Model<Value> model, PrismLog out) throws PrismException
	{
		// Get model info and options
		setEvaluator(model.getEvaluator());
		Evaluator<Value> evalRewards = getRewardEvaluator();
		ModelType modelType = model.getModelType();
		int numRewards = getNumRewards();
		int numLabels = getNumLabels();
		int numStates = model.getNumStates();
		// By default, we only show actions for nondeterministic models
		boolean showActions = modelExportOptions.getShowActions(modelType.nondeterministic());

		// Output header
		out.println("// Exported by prism");
		out.println("// Original model type: " + modelType);
		out.println("@type: " + modelType);

		// No parameters
		out.println("@parameters");
		out.println();

		// Output reward structure info
		out.println("@reward_models");
		out.println(String.join(" ", PrismUtils.listReversed(getRewardNames())));

		// Output model stats
		out.println("@nr_states");
		out.println(model.getNumStates());
		out.println("@nr_choices");
		if (modelType.nondeterministic()) {
			out.println(((NondetModel<Value>) model).getNumChoices());
		} else {
			out.println(model.getNumStates());
		}

		// Output states and transitions
		out.println("@model");

		// Iterate through states
		for (int s = 0; s < numStates; s++) {

			// Output state info
			out.print("state " + s);
			if (modelType.partiallyObservable()) {
				out.print(" {" + ((PartiallyObservableModel<Value>) model).getObservation(s) +"}");
			}
			if (modelType.continuousTime()) {
				out.print(" !" + ((CTMC<Value>) model).getExitRate(s));
			}
			if (numRewards > 0) {
				out.print(" " + getStateRewardTuple(getRewards(), s).toStringReversed(e -> formatValue(e, evalRewards), ", "));
			}
			for (int i = 0; i < numLabels; i++) {
				if (getLabel(i).get(s)) {
					out.print(" " + getLabelName(i));
				}
			}
			out.println();

			// Iterate through choices
			int numChoices = 1;
			if (modelType.nondeterministic()) {
				numChoices = ((NondetModel<Value>) model).getNumChoices(s);
			}
			for (int j = 0; j < numChoices; j++) {
				out.print("\taction ");
				if (modelType.nondeterministic() && showActions) {
					Object action = ((NondetModel<Value>) model).getAction(s, j);
					out.print(action != null ? action : "__NOLABEL__");
				} else {
					out.print(j);
				}
				if (numRewards > 0) {
					out.print(" " + getTransitionRewardTuple(getRewards(), s, j).toStringReversed(e -> formatValue(e, evalRewards), ", "));
				}
				out.println();
				// Print out (sorted) transitions
				for (Transition<?> transition : getSortedTransitionsIterator(model, s, j, showActions)) {
					out.println("\t\t" + transition.target + " : " + transition.toString(modelExportOptions));
				}
			}
		}
	}
}
