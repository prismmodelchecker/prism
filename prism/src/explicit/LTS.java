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
import java.util.Iterator;

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
		throw new UnsupportedOperationException();
	}

	@Override
	default void exportTransitionsToDotFile(int s, PrismLog out, Iterable<explicit.graphviz.Decorator> decorators)
	{
		for (Iterator<Integer> it = getSuccessorsIterator(s); it.hasNext(); ) {
			Integer successor = it.next();
			// we ignore decorators here
			out.println(s + " -> " + successor + ";");
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
}
