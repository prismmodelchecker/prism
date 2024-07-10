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

import explicit.Model;
import explicit.NondetModel;
import explicit.PartiallyObservableModel;
import explicit.graphviz.Decorator;
import prism.ModelType;
import prism.PrismLog;

/**
 * Class to manage export of built models to Dot format.
 */
public class DotExporter<Value> extends Exporter<Value>
{
	public DotExporter()
	{
		super();
	}

	public DotExporter(ModelExportOptions modelExportOptions)
	{
		super(modelExportOptions);
	}

	/**
	 * Export a model in Dot format.
	 * @param model Model to export
	 * @param out PrismLog to export to
	 * @param decorators Any Dot decorators to add (ignored if null)
	 */
	public void exportModel(Model<Value> model, PrismLog out, Iterable<explicit.graphviz.Decorator> decorators)
	{
		// Get model info and exportOptions
		setEvaluator(model.getEvaluator());
		ModelType modelType = model.getModelType();
		int numStates = model.getNumStates();
		// By default, we only show actions for nondeterministic models
		boolean showActions = modelExportOptions.getShowActions(modelType.nondeterministic());

		// Get default Dot formatting info
		explicit.graphviz.Decoration defaults = new explicit.graphviz.Decoration();
		defaults.attributes().put("shape", "box");

		// Output header
		out.println("digraph " + modelType + " {");
		out.println("node " + defaults + ";");

		// Output transitions in Dot format
		// Iterate through states
		for (int s = 0; s < numStates; s++) {
			// Set up Dot Decoration
			explicit.graphviz.Decoration d = new explicit.graphviz.Decoration(defaults);
			d.setLabel(Integer.toString(s));
			if (modelExportOptions.getShowStates()) {
				if (modelType.partiallyObservable()) {
					d = new explicit.graphviz.ShowStatesDecorator(model.getStatesList(), ((PartiallyObservableModel<Value>) model)::getObservationAsState).decorateState(s, d);
				} else {
					d = new explicit.graphviz.ShowStatesDecorator(model.getStatesList()).decorateState(s, d);
				}
			}
			if (decorators != null) {
				for (Decorator decorator : decorators) {
					d = decorator.decorateState(s, d);
				}
			}
			// Output state details
			String nodeSrc = Integer.toString(s);
			String nodeMid = Integer.toString(s);
			String decoration = d.toString();
			out.println(nodeSrc + " " + decoration + ";");

			// Iterate through choices
			int numChoices = 1;
			if (modelType.nondeterministic()) {
				numChoices = ((NondetModel<Value>) model).getNumChoices(s);
			}
			for (int j = 0; j < numChoices; j++) {

				// For nondeterministic models, display the choice
				if (modelType.nondeterministic() && modelType.isProbabilistic()) {
					// Print a new dot file line for the initial line fragment for this choice
					nodeMid = "n" + s + "_" + j;
					out.print(nodeSrc + " -> " + nodeMid + " ");
					// Annotate this with the choice index/action
					explicit.graphviz.Decoration d2 = new explicit.graphviz.Decoration();
					d2.attributes().put("arrowhead", "none");
					if (showActions) {
						Object action = ((NondetModel<Value>) model).getAction(s, j);
						d2.setLabel(j + (action != null ? ":" + action : ""));
					}
					// Apply any other decorators requested
					if (decorators != null) {
						for (Decorator decorator : decorators) {
							d2 = decorator.decorateTransition(s, j, d2);
						}
					}
					// Append to the dot file line
					out.println(" " + d2.toString() + ";");
					// Print a new dot file line for the point where this choice branches
					out.print(nodeMid + " [ shape=point,width=0.1,height=0.1,label=\"\" ];\n");
				}

				// Print out (sorted) transitions
				for (Transition<Value> transition : getSortedTransitionsIterator(model, s, j, showActions)) {
					// Print a new Dot file line for the arrow for this transition
					out.print(nodeMid + " -> " + transition.target);
					// Annotate this arrow with the probability
					explicit.graphviz.Decoration d3 = new explicit.graphviz.Decoration();
					if (modelType.isProbabilistic()) {
						d3.setLabel(formatValue(transition.value));
					} else {
						Object action = ((NondetModel<Value>) model).getAction(s, j);
						d3.setLabel(j + (action != null ? ":" + action : ""));
					}
					// Apply any other decorators requested
					if (decorators != null) {
						for (Decorator decorator : decorators) {
							d3 = decorator.decorateProbability(s, transition.target, transition.value, d3);
						}
					}
					// Append to the Dot file line for this transition
					out.println(" " + d3.toString() + ";");
				}
			}
		}

		// Output footer
		out.print("}\n");
	}
}
