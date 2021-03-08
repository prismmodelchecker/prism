//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

import java.util.BitSet;

import explicit.graphviz.Decorator;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;

/**
 * Interface for classes that provide (read) access to an explicit-state labelled transition system (LTS).
 */
public interface LTS extends NondetModel
{
	// Accessors (for Model) - default implementations
	
	@Override
	default ModelType getModelType()
	{
		return ModelType.LTS;
	}

	@Override
	default void exportToPrismExplicitTra(PrismLog out)
	{
		// Output transitions to .tra file
		int numStates = getNumStates();
		out.print(numStates + " " + getNumChoices() + "\n");
		for (int i = 0; i < numStates; i++) {
			int numChoices = getNumChoices(i);
			for (int j = 0; j < numChoices; j++) {
				out.print(i + " " + j + " " + getSuccessor(i, j));
				Object action = getAction(i, j);
				out.print(action == null ? "\n" : (" " + action + "\n"));
			}
		}
	}

	@Override
	default void exportTransitionsToDotFile(int i, PrismLog out, Iterable<explicit.graphviz.Decorator> decorators)
	{
		// Iterate through outgoing transitions (i.e. choices) for this state
		int numChoices = getNumChoices(i);
		for (int j = 0; j < numChoices; j++) {
			Object action = getAction(i, j);
			// Print a new dot file line for the arrow for this transition
			out.print(i + " -> " + getSuccessor(i, j));
			// Annotate this with the choice index/action 
			explicit.graphviz.Decoration d = new explicit.graphviz.Decoration();
			d.setLabel(j + (action != null ? ":" + action : ""));
			// Apply any other decorators requested
			if (decorators != null) {
				for (Decorator decorator : decorators) {
					d = decorator.decorateTransition(i, j, d);
				}
			}
			// Append to the dot file line
			out.println(" " + d.toString() + ";");
		}
	}

	@Override
	default void exportToPrismLanguage(String filename) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	default String infoString()
	{
		String s = "";
		s += getNumStates() + " states (" + getNumInitialStates() + " initial)";
		s += ", " + getNumTransitions() + " transitions";
		return s;
	}

	@Override
	default String infoStringTable()
	{
		String s = "";
		s += "States:      " + getNumStates() + " (" + getNumInitialStates() + " initial)\n";
		s += "Transitions: " + getNumTransitions() + "\n";
		return s;
	}

	// Accessors (for NondetModel) - default implementations
	
	@Override
	default void exportToDotFileWithStrat(PrismLog out, BitSet mark, int[] strat)
	{
		throw new UnsupportedOperationException();
	}
	
	// Accessors
	
	/**
	 * Get the successor state for the {@code i}th choice/transition from state {@code s}.
	 */
	public int getSuccessor(int s, int i);
}
