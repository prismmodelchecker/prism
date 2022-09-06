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
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PrimitiveIterator;
import java.util.TreeMap;
import java.util.function.IntFunction;

import common.IteratorTools;
import common.IterableStateSet;
import common.iterable.MappingIterator;
import explicit.DTMC;
import explicit.Distribution;
import explicit.SuccessorsIterator;
import explicit.graphviz.Decorator;
import prism.ModelType;
import prism.Pair;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismUtils;

/**
 * Base class for a DTMC view, i.e., a virtual DTMC that is obtained
 * by mapping from some other model on-the-fly.
 * <br>
 * The main job of sub-classes is to provide an appropriate
 * getTransitionsIterator() method. Several other methods, providing
 * meta-data on the model have to be provided as well. For examples,
 * see the sub-classes contained in this package.
 */
public abstract class DTMCView extends ModelView implements DTMC, Cloneable
{
	public DTMCView()
	{
		super();
	}

	public DTMCView(final ModelView model)
	{
		super(model);
	}



	//--- Object ---

	@Override
	public String toString()
	{
		final IntFunction<Entry<Integer, Distribution>> getDistribution = new IntFunction<Entry<Integer, Distribution>>()
		{
			@Override
			public final Entry<Integer, Distribution> apply(final int state)
			{
				final Distribution distribution = new Distribution(getTransitionsIterator(state));
				return new AbstractMap.SimpleImmutableEntry<>(state, distribution);
			}
		};
		String s = "trans: [ ";
		final IterableStateSet states = new IterableStateSet(getNumStates());
		final Iterator<Entry<Integer, Distribution>> distributions = new MappingIterator.FromInt<>(states, getDistribution);
		while (distributions.hasNext()) {
			final Entry<Integer, Distribution> dist = distributions.next();
			s += dist.getKey() + ": " + dist.getValue();
			if (distributions.hasNext()) {
				s += ", ";
			}
		}
		return s + " ]";
	}



	//--- Model ---

	@Override
	public ModelType getModelType()
	{
		return ModelType.DTMC;
	}

	@Override
	public int getNumTransitions()
	{
		int numTransitions = 0;
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			numTransitions += getNumTransitions(state);
		}
		return numTransitions;
	}

	@Override
	public SuccessorsIterator getSuccessors(final int state)
	{
		final Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state);

		return SuccessorsIterator.from(new PrimitiveIterator.OfInt() {
			public boolean hasNext() {return transitions.hasNext();}
			public int nextInt() {return transitions.next().getKey();}
		}, true);
	}

	@Override
	public void exportToPrismExplicitTra(final PrismLog log)
	{
		// Output transitions to .tra file
		log.print(getNumStates() + " " + getNumTransitions() + "\n");
		final TreeMap<Integer, Double> sorted = new TreeMap<>();
		for (int state = 0, max = getNumStates(); state < max; state++) {
			// Extract transitions and sort by destination state index (to match PRISM-exported files)
			for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state); transitions.hasNext();) {
				final Entry<Integer, Double> transition = transitions.next();
				sorted.put(transition.getKey(), transition.getValue());
			}
			// Print out (sorted) transitions
			for (Entry<Integer, Double> transition : sorted.entrySet()) {
				// Note use of PrismUtils.formatDouble to match PRISM-exported files
				log.print(state + " " + transition.getKey() + " " + PrismUtils.formatDouble(transition.getValue()) + "\n");
			}
			sorted.clear();
		}
	}

	@Override
	public void exportToPrismLanguage(final String filename) throws PrismException
	{
		try (FileWriter out = new FileWriter(filename)) {
			out.write(getModelType().keyword() + "\n");
			out.write("module M\nx : [0.." + (getNumStates() - 1) + "];\n");
			final TreeMap<Integer, Double> sorted = new TreeMap<Integer, Double>();
			for (int state = 0, max = getNumStates(); state < max; state++) {
				// Extract transitions and sort by destination state index (to match PRISM-exported files)
				for (Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state); transitions.hasNext();) {
					final Entry<Integer, Double> transition = transitions.next();
					sorted.put(transition.getKey(), transition.getValue());
				}
				// Print out (sorted) transitions
				out.write("[]x=" + state + "->");
				boolean first = true;
				for (Entry<Integer, Double> transition : sorted.entrySet()) {
					if (first)
						first = false;
					else
						out.write("+");
					// Note use of PrismUtils.formatDouble to match PRISM-exported files
					out.write(PrismUtils.formatDouble(transition.getValue()) + ":(x'=" + transition.getKey() + ")");
				}
				out.write(";\n");
				sorted.clear();
			}
			out.write("endmodule\n");
		} catch (IOException e) {
			throw new PrismException("Could not export " + getModelType() + " to file \"" + filename + "\"" + e);
		}
	}

	@Override
	public String infoString()
	{
		String s = "";
		s += getNumStates() + " states (" + getNumInitialStates() + " initial)";
		s += ", " + getNumTransitions() + " transitions";
		return s;
	}

	@Override
	public String infoStringTable()
	{
		String s = "";
		s += "States:      " + getNumStates() + " (" + getNumInitialStates() + " initial)\n";
		s += "Transitions: " + getNumTransitions() + "\n";
		return s;
	}



	//--- DTMC ---

	@Override
	public int getNumTransitions(final int state)
	{
		return IteratorTools.count(getTransitionsIterator(state));
	}

	public static Entry<Integer, Pair<Double, Object>> attachAction(final Entry<Integer, Double> transition, final Object action)
	{
		final Integer state = transition.getKey();
		final Double probability = transition.getValue();
		return new AbstractMap.SimpleImmutableEntry<>(state, new Pair<>(probability, action));
	}

	@Override
	public Iterator<Entry<Integer, Pair<Double, Object>>> getTransitionsAndActionsIterator(final int state)
	{
		final Iterator<Entry<Integer, Double>> transitions = getTransitionsIterator(state);
		return new MappingIterator.From<>(transitions, transition -> attachAction(transition, null));
	}


	//--- ModelView ---

	/**
	 * @see explicit.DTMCExplicit#exportTransitionsToDotFile(int, PrismLog) DTMCExplicit
	 **/
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
}
