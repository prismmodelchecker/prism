//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
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

package explicit.graphviz;

import java.util.BitSet;

/**
 * A Decorator is called from the DOT output routines and
 * can modify the decoration that is attached at the
 * given node or edge in the GraphViz file.
 * <br>
 * Default implementations are provided that simply pass
 * through the decoration given as input, without any modification.
 */
public interface Decorator
{
	/**
	 * Called to allow modification of the decoration
	 * attached to a state.
	 * @param state the state index in the model
	 * @param d the current decoration
	 * @return an updated decoration (may return {@code d} and modify it)
	 */
	default Decoration decorateState(int state, Decoration d)
	{
		return d;
	}

	/**
	 * Called to allow modification of the decoration
	 * attached to non-deterministic transition choice.
	 * @param from the state index in the model of the originating state
	 * @param choice the choice index
	 * @param d the current decoration
	 * @return an updated decoration (may return {@code d} and modify it)
	 */
	default Decoration decorateTransition(int from, int choice, Decoration d)
	{
		return d;
	}

	/**
	 * Called to allow modification of the decoration
	 * attached to a probabilistic transition for a non-deterministic choice.
	 * @param from the state index in the model of the originating state
	 * @param to the state index in the model of the target state
	 * @param choice the choice index
	 * @param probability the probability (e.g., a Double, param.BigRational, param.Function)
	 * @param d the current decoration
	 * @return an updated decoration (may return {@code d} and modify it)
	 */
	default Decoration decorateProbability(int from, int to, int choice, Object probability, Decoration d)
	{
		return d;
	}

	/**
	 * Called to allow modification of the decoration
	 * attached to a probabilistic transition in a model without non-deterministic choices (e.g., DTMC, CTMC).
	 * @param from the state index in the model of the originating state
	 * @param to the state index in the model of the target state
	 * @param probability the probability (e.g., a Double, param.BigRational, param.Function)
	 * @param d the current decoration
	 * @return an updated decoration (may return {@code d} and modify it)
	 */
	default Decoration decorateProbability(int from, int to, Object probability, Decoration d)
	{
		return d;
	}

	/**
	 * Called to allow modification of the decoration
	 * attached to an automaton edge.
	 * @param from the state index in the automaton of the originating state
	 * @param to the state index in the automaton of the target state
	 * @param apElement a BitSet representing the current element of the alphabet (2^AP)
	 * @param d the current decoration
	 * @return an updated decoration (may return {@code d} and modify it)
	 */
	default Decoration decorateAutomatonEdge(int from, int to, BitSet apElement, Decoration d)
	{
		return d;
	}

}
