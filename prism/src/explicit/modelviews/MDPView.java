//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package explicit.modelviews;

import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import common.IteratorTools;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismUtils;
import strat.MDStrategy;
import explicit.DTMCFromMDPAndMDStrategy;
import explicit.Distribution;
import explicit.MDP;
import explicit.Model;
import explicit.SuccessorsIterator;
import explicit.graphviz.Decorator;

/**
 * Base class for an MDP view, i.e., a virtual MDP that is obtained
 * by mapping from some other model on-the-fly.
 * <br>
 * The main job of sub-classes is to provide an appropriate
 * getTransitionsIterator() method. Several other methods, providing
 * meta-data on the model have to be provided as well. For examples,
 * see the sub-classes contained in this package.
 */
public abstract class MDPView extends ModelView implements MDP, Cloneable
{
	public MDPView()
	{
		super();
	}

	public MDPView(final MDPView model)
	{
		super(model);
	}



	//--- Object ---

	@Override
	public String toString()
	{
		String s = "[ ";
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			if (state > 0)
				s += ", ";
			s += state + ": ";
			s += "[";
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				if (choice > 0)
					s += ",";
				final Object action = getAction(state, choice);
				if (action != null)
					s += action + ":";
				s += new Distribution(getTransitionsIterator(state, choice));
			}
			s += "]";
		}
		s += " ]";
		return s;
	}



	//--- Model ---

	@Override
	public ModelType getModelType()
	{
		return ModelType.MDP;
	}

	@Override
	public int getNumTransitions()
	{
		int numTransitions = 0;
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++)
				numTransitions += getNumTransitions(state, choice);
		}
		return numTransitions;
	}

	@Override
	public void exportToPrismExplicitTra(final PrismLog out)
	{
		final int numStates = getNumStates();
		// Output transitions to .tra file
		out.print(numStates + " " + getNumChoices() + " " + getNumTransitions() + "\n");
		final TreeMap<Integer, Double> sorted = new TreeMap<>();
		for (int state = 0; state < numStates; state++) {
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				// Extract transitions and sort by destination state index (to match PRISM-exported files)
				for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
					final Entry<Integer, Double> trans = transitions.next();
					sorted.put(trans.getKey(), trans.getValue());
				}
				// Print out (sorted) transitions
				for (Entry<Integer, Double> e : sorted.entrySet()) {
					// Note use of PrismUtils.formatDouble to match PRISM-exported files
					out.print(state + " " + choice + " " + e.getKey() + " " + PrismUtils.formatDouble(e.getValue()));
					final Object action = getAction(state, choice);
					out.print(action == null ? "\n" : (" " + action + "\n"));
				}
				sorted.clear();
			}
		}
	}

	@Override
	public void exportToPrismLanguage(final String filename) throws PrismException
	{
		try (FileWriter out = new FileWriter(filename)) {
			// Output transitions to PRISM language file
			out.write(getModelType().keyword() + "\n");
			final int numStates = getNumStates();
			out.write("module M\nx : [0.." + (numStates - 1) + "];\n");
			final TreeMap<Integer, Double> sorted = new TreeMap<Integer, Double>();
			for (int state = 0; state < numStates; state++) {
				for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
					// Extract transitions and sort by destination state index (to match PRISM-exported files)
					for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
						final Entry<Integer, Double> trans = transitions.next();
						sorted.put(trans.getKey(), trans.getValue());
					}
					// Print out (sorted) transitions
					final Object action = getAction(state, choice);
					out.write(action != null ? ("[" + action + "]") : "[]");
					out.write("x=" + state + "->");
					boolean first = true;
					for (Entry<Integer, Double> e : sorted.entrySet()) {
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
		} catch (IOException e) {
			throw new PrismException("Could not export " + getModelType() + " to file \"" + filename + "\"" + e);
		}
	}

	@Override
	public String infoString()
	{
		final int numStates = getNumStates();
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
		final int numStates = getNumStates();
		String s = "";
		s += "States:      " + numStates + " (" + getNumInitialStates() + " initial)\n";
		s += "Transitions: " + getNumTransitions() + "\n";
		s += "Choices:     " + getNumChoices() + "\n";
		s += "Max/avg:     " + getMaxNumChoices() + "/" + PrismUtils.formatDouble2dp(((double) getNumChoices()) / numStates) + "\n";
		return s;
	}

	//--- NondetModel ---

	@Override
	public int getNumChoices()
	{
		int numChoices = 0;
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			numChoices += getNumChoices(state);
		}
		return numChoices;
	}

	@Override
	public int getMaxNumChoices()
	{
		int maxNumChoices = 0;
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			maxNumChoices = Math.max(maxNumChoices, getNumChoices(state));
		}
		return maxNumChoices;
	}

	@Override
	public boolean areAllChoiceActionsUnique()
	{
		final HashSet<Object> actions = new HashSet<Object>();
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			final int numChoices = getNumChoices(state);
			if (numChoices <= 1) {
				continue;
			}
			actions.clear();
			for (int choice = 0; choice < numChoices; choice++) {
				if (!actions.add(getAction(state, choice))) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public int getNumTransitions(final int state, final int choice)
	{
		return IteratorTools.count(getTransitionsIterator(state, choice));
	}

	@Override
	public SuccessorsIterator getSuccessors(final int state, final int choice)
	{
		final Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice);

		return SuccessorsIterator.from(new PrimitiveIterator.OfInt() {
			public boolean hasNext() {return transitions.hasNext();}
			public int nextInt() {return transitions.next().getKey();}
		}, true);
	}

	@Override
	public Model constructInducedModel(final MDStrategy strat)
	{
		return new DTMCFromMDPAndMDStrategy(this, strat);
	}

	@Override
	public void exportToDotFileWithStrat(final PrismLog out, final BitSet mark, final int[] strat)
	{
		out.print("digraph " + getModelType() + " {\nnode [shape=box];\n");
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			if (mark != null && mark.get(state))
				out.print(state + " [style=filled  fillcolor=\"#cccccc\"]\n");
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				final String style = (strat[state] == choice) ? ",color=\"#ff0000\",fontcolor=\"#ff0000\"" : "";
				final Object action = getAction(state, choice);
				final String nij = "n" + state + "_" + choice;
				out.print(state + " -> " + nij + " [ arrowhead=none,label=\"" + choice);
				if (action != null) {
					out.print(":" + action);
				}
				out.print("\"" + style + " ];\n");
				out.print(nij + " [ shape=point,height=0.1,label=\"\"" + style + " ];\n");
				for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
					Entry<Integer, Double> trans = transitions.next();
					out.print(nij + " -> " + trans.getKey() + " [ label=\"" + trans.getValue() + "\"" + style + " ];\n");
				}
			}
		}
		out.print("}\n");
	}

	//--- ModelView ---

	/**
	 * @see explicit.MDPExplicit#exportTransitionsToDotFile(int, PrismLog) MDPExplicit
	 **/
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
}
