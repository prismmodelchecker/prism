//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import explicit.graphviz.Decorator;
import prism.ModelType;
import prism.Pair;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismUtils;

/**
 * Base class for explicit-state representations of a DTMC.
 */
public abstract class DTMCExplicit extends ModelExplicit implements DTMC
{
	// Accessors (for Model)
	
	@Override
	public ModelType getModelType()
	{
		return ModelType.DTMC;
	}

	@Override
	public void exportToPrismExplicitTra(PrismLog out)
	{
		int i;
		TreeMap<Integer, Pair<Double, Object>> sorted;
		// Output transitions to .tra file
		out.print(numStates + " " + getNumTransitions() + "\n");
		sorted = new TreeMap<Integer, Pair<Double, Object>>();
		for (i = 0; i < numStates; i++) {
			// Extract transitions and sort by destination state index (to match PRISM-exported files)
			Iterator<Map.Entry<Integer,Pair<Double, Object>>> iter = getTransitionsAndActionsIterator(i);
			while (iter.hasNext()) {
				Map.Entry<Integer, Pair<Double, Object>> e = iter.next();
				sorted.put(e.getKey(), e.getValue());
			}
			// Print out (sorted) transitions
			for (Map.Entry<Integer, Pair<Double, Object>> e : sorted.entrySet()) {
				// Note use of PrismUtils.formatDouble to match PRISM-exported files
				out.print(i + " " + e.getKey() + " " + PrismUtils.formatDouble(e.getValue().first));
				Object action = e.getValue().second; 
				if (action != null && !"".equals(action))
					out.print(" " + action);
				out.print("\n");
			}
			sorted.clear();
		}
	}

	@Override
	public void exportTransitionsToDotFile(int i, PrismLog out, Iterable<explicit.graphviz.Decorator> decorators)
	{
		Iterator<Map.Entry<Integer, Double>> iter = getTransitionsIterator(i);
		while (iter.hasNext()) {
			Map.Entry<Integer, Double> e = iter.next();
			out.print(i + " -> " + e.getKey());

			explicit.graphviz.Decoration d = new explicit.graphviz.Decoration();
			d.setLabel(e.getValue().toString());
			if (decorators != null) {
				for (Decorator decorator : decorators) {
					d = decorator.decorateProbability(i, e.getKey(), e.getValue(), d);
				}
			}

			out.println(d.toString());
		}
	}

	@Override
	public void exportToPrismLanguage(String filename) throws PrismException
	{
		int i;
		boolean first;
		FileWriter out;
		TreeMap<Integer, Double> sorted;
		try {
			// Output transitions to PRISM language file
			out = new FileWriter(filename);
			out.write(getModelType().keyword() + "\n");
			out.write("module M\nx : [0.." + (numStates - 1) + "];\n");
			sorted = new TreeMap<Integer, Double>();
			for (i = 0; i < numStates; i++) {
				// Extract transitions and sort by destination state index (to match PRISM-exported files)
				Iterator<Map.Entry<Integer, Double>> iter = getTransitionsIterator(i);
				while (iter.hasNext()) {
					Map.Entry<Integer, Double> e = iter.next();
					sorted.put(e.getKey(), e.getValue());
				}
				// Print out (sorted) transitions
				out.write("[]x=" + i + "->");
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
			out.write("endmodule\n");
			out.close();
		} catch (IOException e) {
			throw new PrismException("Could not export " + getModelType() + " to file \"" + filename + "\"" + e);
		}
	}
	
	// Accessors (for DTMC)
	
	@Override
	public Iterator<Entry<Integer, Pair<Double, Object>>> getTransitionsAndActionsIterator(int s)
	{
		// Default implementation: extend iterator, setting all actions to null
		return new AddDefaultActionToTransitionsIterator(getTransitionsIterator(s), null);
	}

	public class AddDefaultActionToTransitionsIterator implements Iterator<Map.Entry<Integer, Pair<Double, Object>>>
	{
		private Iterator<Entry<Integer, Double>> transIter;
		private Object defaultAction;
		private Entry<Integer, Double> next;

		public AddDefaultActionToTransitionsIterator(Iterator<Entry<Integer, Double>> transIter, Object defaultAction)
		{
			this.transIter = transIter;
			this.defaultAction = defaultAction;
		}

		@Override
		public Entry<Integer, Pair<Double, Object>> next()
		{
			next = transIter.next();
			final Integer state = next.getKey();
			final Double probability = next.getValue();
			return new AbstractMap.SimpleImmutableEntry<>(state, new Pair<>(probability, defaultAction));
		}

		@Override
		public boolean hasNext()
		{
			return transIter.hasNext();
		}

		@Override
		public void remove()
		{
			// Do nothing: read-only
		}
	}
}
