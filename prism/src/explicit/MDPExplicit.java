//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Christian von Essen <christian.vonessen@imag.fr> (Verimag, Grenoble)
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

import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismUtils;
import strat.MDStrategy;
import explicit.graphviz.Decorator;

/**
 * Base class for explicit-state representations of an MDP.
 */
public abstract class MDPExplicit extends ModelExplicit implements MDP
{
	// Accessors (for Model)

	@Override
	public ModelType getModelType()
	{
		return ModelType.MDP;
	}

	@Override
	public String infoString()
	{
		String s = "";
		s += numStates + " states (" + getNumInitialStates() + " initial)";
		s += ", " + getNumTransitions() + " transitions";
		s += ", " + getNumChoices() + " choices";
		s += ", dist max/avg = " + getMaxNumChoices() + "/" + PrismUtils.formatDouble2dp(((double) getNumChoices()) / numStates);
		return s;
	}

	@Override
	public String infoStringTable()
	{
		String s = "";
		s += "States:      " + numStates + " (" + getNumInitialStates() + " initial)\n";
		s += "Transitions: " + getNumTransitions() + "\n";
		s += "Choices:     " + getNumChoices() + "\n";
		s += "Max/avg:     " + getMaxNumChoices() + "/" + PrismUtils.formatDouble2dp(((double) getNumChoices()) / numStates) + "\n";
		return s;
	}

	@Override
	public void exportToPrismExplicitTra(PrismLog out)
	{
		int i, j, numChoices;
		Object action;
		TreeMap<Integer, Double> sorted;
		// Output transitions to .tra file
		out.print(numStates + " " + getNumChoices() + " " + getNumTransitions() + "\n");
		sorted = new TreeMap<Integer, Double>();
		for (i = 0; i < numStates; i++) {
			numChoices = getNumChoices(i);
			for (j = 0; j < numChoices; j++) {
				// Extract transitions and sort by destination state index (to match PRISM-exported files)
				Iterator<Map.Entry<Integer, Double>> iter = getTransitionsIterator(i, j);
				while (iter.hasNext()) {
					Map.Entry<Integer, Double> e = iter.next();
					sorted.put(e.getKey(), e.getValue());
				}
				// Print out (sorted) transitions
				for (Map.Entry<Integer, Double> e : sorted.entrySet()) {
					// Note use of PrismUtils.formatDouble to match PRISM-exported files
					out.print(i + " " + j + " " + e.getKey() + " " + PrismUtils.formatDouble(e.getValue()));
					action = getAction(i, j);
					out.print(action == null ? "\n" : (" " + action + "\n"));
				}
				sorted.clear();
			}
		}
	}

	@Override
	public void exportTransitionsToDotFile(int i, PrismLog out, Iterable<explicit.graphviz.Decorator> decorators)
	{
		int j, numChoices;
		String nij;
		Object action;
		numChoices = getNumChoices(i);
		for (j = 0; j < numChoices; j++) {
			action = getAction(i, j);
			nij = "n" + i + "_" + j;
			out.print(i + " -> " + nij + " ");

			explicit.graphviz.Decoration d = new explicit.graphviz.Decoration();
			d.attributes().put("arrowhead", "none");
			d.setLabel(j + (action != null ? ":" + action : ""));

			if (decorators != null) {
				for (Decorator decorator : decorators) {
					d = decorator.decorateTransition(i, j, d);
				}
			}
			out.print(d);
			out.println(";");

			out.print(nij + " [ shape=point,width=0.1,height=0.1,label=\"\" ];\n");

			Iterator<Map.Entry<Integer, Double>> iter = getTransitionsIterator(i, j);
			while (iter.hasNext()) {
				Map.Entry<Integer, Double> e = iter.next();
				out.print(nij + " -> " + e.getKey() + " ");

				d = new explicit.graphviz.Decoration();
				d.setLabel(e.getValue().toString());

				if (decorators != null) {
					for (Decorator decorator : decorators) {
						d = decorator.decorateProbability(i, e.getKey(), j, e.getValue(), d);
					}
				}

				out.print(d);
				out.println(";");
			}
		}
	}

	@Override
	public void exportToDotFileWithStrat(PrismLog out, BitSet mark, int strat[])
	{
		int i, j, numChoices;
		String nij;
		Object action;
		String style;
		out.print("digraph " + getModelType() + " {\nnode [shape=box];\n");
		for (i = 0; i < numStates; i++) {
			if (mark != null && mark.get(i))
				out.print(i + " [style=filled  fillcolor=\"#cccccc\"]\n");
			numChoices = getNumChoices(i);
			for (j = 0; j < numChoices; j++) {
				style = (strat[i] == j) ? ",color=\"#ff0000\",fontcolor=\"#ff0000\"" : "";
				action = getAction(i, j);
				nij = "n" + i + "_" + j;
				out.print(i + " -> " + nij + " [ arrowhead=none,label=\"" + j);
				if (action != null)
					out.print(":" + action);
				out.print("\"" + style + " ];\n");
				out.print(nij + " [ shape=point,height=0.1,label=\"\"" + style + " ];\n");
				Iterator<Map.Entry<Integer, Double>> iter = getTransitionsIterator(i, j);
				while (iter.hasNext()) {
					Map.Entry<Integer, Double> e = iter.next();
					out.print(nij + " -> " + e.getKey() + " [ label=\"" + e.getValue() + "\"" + style + " ];\n");
				}
			}
		}
		out.print("}\n");
	}

	@Override
	public void exportToPrismLanguage(String filename) throws PrismException
	{
		int i, j, numChoices;
		boolean first;
		FileWriter out;
		TreeMap<Integer, Double> sorted;
		Object action;
		try {
			// Output transitions to PRISM language file
			out = new FileWriter(filename);
			out.write(getModelType().keyword() + "\n");
			out.write("module M\nx : [0.." + (numStates - 1) + "];\n");
			sorted = new TreeMap<Integer, Double>();
			for (i = 0; i < numStates; i++) {
				numChoices = getNumChoices(i);
				for (j = 0; j < numChoices; j++) {
					// Extract transitions and sort by destination state index (to match PRISM-exported files)
					Iterator<Map.Entry<Integer, Double>> iter = getTransitionsIterator(i, j);
					while (iter.hasNext()) {
						Map.Entry<Integer, Double> e = iter.next();
						sorted.put(e.getKey(), e.getValue());
					}
					// Print out (sorted) transitions
					action = getAction(i, j);
					out.write(action != null ? ("[" + action + "]") : "[]");
					out.write("x=" + i + "->");
					first = true;
					for (Map.Entry<Integer, Double> e : sorted.entrySet()) {
						if (first)
							first = false;
						else
							out.write("+");
						// Note use of PrismUtils.formatDouble to match PRISM-exported files
						out.write(PrismUtils.formatDouble(e.getValue()) + ":(x'=" + e.getKey() + ")");
					}
					out.write(";\n");
					sorted.clear();
				}
			}
			out.write("endmodule\n");
			out.close();
		} catch (IOException e) {
			throw new PrismException("Could not export " + getModelType() + " to file \"" + filename + "\"" + e);
		}
	}

	// Accessors (for NondetModel)
	
	@Override
	public boolean areAllChoiceActionsUnique()
	{
		HashSet<Object> sActions = new HashSet<Object>();
		for (int s = 0; s < numStates; s++) {
			int n = getNumChoices(s);
			if (n > 1) {
				sActions.clear();
				for (int i = 0; i < n; i++) {
					if (!sActions.add(getAction(s, i))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	// Accessors (for MDP)

	@Override
	public Model constructInducedModel(MDStrategy strat)
	{
		return new DTMCFromMDPAndMDStrategy(this, strat);
	}
}
