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
 * Class to manage export of built models to PRISM's explicit file formats.
 */
public class PrismExplicitExporter<Value> extends Exporter<Value>
{
	public PrismExplicitExporter()
	{
		super();
	}

	public PrismExplicitExporter(ModelExportOptions modelExportOptions)
	{
		super(modelExportOptions);
	}

	/**
	 * Export a model in PRISM's .tra format.
	 * @param model Model to export
	 * @param out PrismLog to export to
	 */
	public void exportTransitions(Model<Value> model, PrismLog out)
	{
		// Get model info and exportOptions
		setEvaluator(model.getEvaluator());
		ModelType modelType = model.getModelType();
		boolean exportActions = modelType.nondeterministic();

		// Output .tra file file header
		int numStates = model.getNumStates();
		out.print(numStates);
		if (modelType.nondeterministic()) {
			out.print(" " + ((NondetModel) model).getNumChoices());
		}
		out.print(" " + model.getNumTransitions());
		if (modelType.partiallyObservable()) {
			out.print(" " + ((PartiallyObservableModel) model).getNumObservations());
		}
		out.print("\n");

		// Output transitions in .tra format
		// Iterate through states
		for (int s = 0; s < numStates; s++) {
			int numChoices = 1;
			if (modelType.nondeterministic()) {
				numChoices = ((NondetModel<Value>) model).getNumChoices(s);
			}
			// Iterate through choices
			for (int j = 0; j < numChoices; j++) {
				// Print out (sorted) transitions
				for (Transition<Value> transition : getSortedTransitionsIterator(model, s, j, exportActions)) {
					out.print(s);
					if (modelType.nondeterministic()) {
						out.print(" " + j);
					}
					out.print(" " + transition.target);
					if (modelType.isProbabilistic()) {
						out.print(" " + formatValue(transition.probability));
					}
					if (modelType.partiallyObservable()) {
						out.print(" " + ((PartiallyObservableModel) model).getObservation(transition.target));
					}
					if (transition.action != null && !"".equals(transition.action)) {
						out.print(" " + transition.action);
					}
					out.print("\n");
				}
			}
		}
	}

	/**
	 * Export (non-zero) state rewards from an MCRewards object.
	 * @param model The model
	 * @param mcRewards The rewards
	 * @param exportType The format in which to export
	 * @param out Where to export
	 * @param precision number of significant digits >= 1
	 */
	public void exportMCStateRewards(Model<Value> model, MCRewards<Value> mcRewards, String rewardStructName, PrismLog out) throws PrismException
	{
		// Get model info and exportOptions
		setEvaluator(model.getEvaluator());
		Evaluator<Value> evalRewards = mcRewards.getEvaluator();
		boolean noexportheaders = !getModelExportOptions().getPrintHeaders();

		int numStates = model.getNumStates();
		int nonZeroRews = 0;
		for (int s = 0; s < numStates; s++) {
			Value d = mcRewards.getStateReward(s);
			if (!evalRewards.isZero(d)) {
				nonZeroRews++;
			}
		}
		printStateRewardsHeader(out, rewardStructName, noexportheaders);
		out.println(numStates + " " + nonZeroRews);
		for (int s = 0; s < numStates; s++) {
			Value d = mcRewards.getStateReward(s);
			if (!evalRewards.isZero(d)) {
				out.println(s + " " + formatValue(d, evalRewards));
			}
		}
	}

	/**
	 * Export (non-zero) state rewards from an MDPRewards object.
	 * @param model The model
	 * @param mdpRewards The rewards
	 * @param exportType The format in which to export
	 * @param out Where to export
	 * @param precision number of significant digits >= 1
	 */
	public void exportMDPStateRewards(Model<Value> model, MDPRewards<Value> mdpRewards, String rewardStructName, PrismLog out) throws PrismException
	{
		// Get model info and exportOptions
		setEvaluator(model.getEvaluator());
		Evaluator<Value> evalRewards = mdpRewards.getEvaluator();
		boolean noexportheaders = !getModelExportOptions().getPrintHeaders();

		int numStates = model.getNumStates();
		int nonZeroRews = 0;
		for (int s = 0; s < numStates; s++) {
			Value d = mdpRewards.getStateReward(s);
			if (!evalRewards.isZero(d)) {
				nonZeroRews++;
			}
		}
		printStateRewardsHeader(out, rewardStructName, noexportheaders);
		out.println(numStates + " " + nonZeroRews);
		for (int s = 0; s < numStates; s++) {
			Value d = mdpRewards.getStateReward(s);
			if (!evalRewards.isZero(d)) {
				out.println(s + " " + formatValue(d, evalRewards));
			}
		}
	}

	/**
	 * Print header to srew file, when not disabled.
	 * Header format with optional reward struct name:
	 * <pre>
	 *   # Reward structure &lt;double-quoted-name&gt;
	 *   # State rewards
	 * </pre>
	 * where &lt;double-quoted-name&gt; ("<name>") is omitted if the reward structure is not named.
	 *
	 * @param r index of the reward structure
	 * @param out print target
	 * @param noexportheaders disable export of the header
	 */
	public void printStateRewardsHeader(PrismLog out, String rewardStructName, boolean noexportheaders)
	{
		if (noexportheaders) {
			return;
		}
		out.print("# Reward structure");
		if (!"".equals(rewardStructName)) {
			out.print(" \"" + rewardStructName + "\"");
		}
		out.println("\n# State rewards");
	}

	/**
	 * Export (non-zero) transition rewards from an MDPRewards object.
	 * @param model The model
	 * @param mdpRewards The rewards
	 * @param exportType The format in which to export
	 * @param out Where to export
	 * @param precision number of significant digits >= 1
	 */
	public void exportMDPTransRewards(NondetModel<Value> model, MDPRewards<Value> mdpRewards, String rewardStructName, PrismLog out) throws PrismException
	{
		// Get model info and exportOptions
		setEvaluator(model.getEvaluator());
		Evaluator<Value> evalRewards = mdpRewards.getEvaluator();
		boolean noexportheaders = !getModelExportOptions().getPrintHeaders();

		int numStates = model.getNumStates();
		int numChoicesAll = model.getNumChoices();
		int nonZeroRews = 0;
		for (int s = 0; s < numStates; s++) {
			int numChoices = model.getNumChoices();
			for (int i = 0; i < numChoices; i++) {
				Value d = mdpRewards.getTransitionReward(s, i);
				if (!evalRewards.isZero(d)) {
					nonZeroRews += model.getNumTransitions(s, i);;
				}
			}
		}
		printTransRewardsHeader(out, rewardStructName, noexportheaders);
		out.println(numStates + " " + numChoicesAll + " " + nonZeroRews);
		for (int s = 0; s < numStates; s++) {
			int numChoices = model.getNumChoices();
			for (int i = 0; i < numChoices; i++) {
				Value d = mdpRewards.getTransitionReward(s, i);
				if (!evalRewards.isZero(d)) {
					int numTransitions = model.getNumTransitions(s, i);
					for (SuccessorsIterator succ = model.getSuccessors(s, i); succ.hasNext();) {
						int j = succ.nextInt();
						out.println(s + " " + i + " " + j + " " + formatValue(d, evalRewards));
					}
				}
			}
		}
	}

	/**
	 * Print header to trew file, when not disabled.
	 * Header format with optional reward struct name:
	 * <pre>
	 *   # Reward structure &lt;double-quoted-name&gt;
	 *   # State rewards
	 * </pre>
	 * where &lt;double-quoted-name&gt; ("<name>") is omitted if the reward structure is not named.
	 *
	 * @param r index of the reward structure
	 * @param out print target
	 * @param noexportheaders disable export of the header
	 */
	public void printTransRewardsHeader(PrismLog out, String rewardStructName, boolean noexportheaders)
	{
		if (noexportheaders) {
			return;
		}
		out.print("# Reward structure");
		if (!"".equals(rewardStructName)) {
			out.print(" \"" + rewardStructName + "\"");
		}
		out.println("\n# Transition rewards");
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
		out.print("(");
		int numVars = varList.getNumVars();
		for (int i = 0; i < numVars; i++) {
			out.print(varList.getName(i));
			if (i < numVars - 1)
				out.print(",");
		}
		out.println(")");

		// Print states
		int numStates = statesList.size();
		for (int i = 0; i < numStates; i++) {
			out.println(i + ":" + statesList.get(i).toString());
		}
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
		int numLabels = labelNames.size();
		for (int s = 0; s < numLabels; s++) {
			out.print((s > 0 ? " " : "") + s + "=\"" + labelNames.get(s) + "\"");
		}
		out.println();

		// Go through states and print satisfying label indices for each one
		for (int s = 0; s < numStates; s++) {
			boolean first = true;
			for (int i = 0; i < numLabels; i++) {
				if (labelStates.get(i).get(s)) {
					if (first) {
						out.print(s + ":");
						first = false;
					}
					out.print(" " + i);
				}
			}
			if (!first) {
				out.println();
			}
		}
	}
}
