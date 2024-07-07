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
