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
import prism.Evaluator;
import io.ModelExportOptions;
import prism.ModelType;
import prism.Pair;
import prism.PrismLog;

import java.util.Iterator;
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
}
