//==============================================================================
//
//	Copyright (c) 2025-
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

import java.util.ArrayList;
import java.util.List;
import java.util.PrimitiveIterator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import explicit.Distribution;
import explicit.MDPSimple;

/**
 * Tests for the per-choice iterators of {@link ModelAccess} (see {@link ModelAccessIterators}),
 * in particular for models containing states with no choices (deadlocks).
 */
public class ModelAccessIteratorsTest
{
	/**
	 * MDP with 5 states, where states 0, 2 and 4 have no choices (deadlocks),
	 * state 1 has 2 choices (with 1 and 2 transitions) and state 3 has 1 choice (with 1 transition).
	 * Choices are labelled with actions "a", "b", "c", respectively.
	 */
	private static MDPSimple<Double> buildMDPWithDeadlocks()
	{
		MDPSimple<Double> mdp = new MDPSimple<>(5);
		mdp.addActionLabelledChoice(1, distribution(new int[] {3}, new double[] {1.0}), "a");
		mdp.addActionLabelledChoice(1, distribution(new int[] {0, 2}, new double[] {0.5, 0.5}), "b");
		mdp.addActionLabelledChoice(3, distribution(new int[] {4}, new double[] {1.0}), "c");
		mdp.addInitialState(1);
		return mdp;
	}

	@Test
	void choiceIteratorsSkipStatesWithNoChoices()
	{
		ModelAccess<Double> modelAccess = ModelAccess.wrap(buildMDPWithDeadlocks());
		assertEquals(List.of(0, 0, 2, 2, 3, 3), toList(modelAccess.getStateChoiceOffsets()));
		assertEquals(List.of(0, 1, 3, 4), toList(modelAccess.getChoiceTransitionOffsets()));
		List<String> actionStrings = modelAccess.getActionStrings();
		List<String> actions = new ArrayList<>();
		for (int i : toList(modelAccess.getChoiceActionIndices())) {
			actions.add(actionStrings.get(i));
		}
		assertEquals(List.of("a", "b", "c"), actions);
	}

	private static Distribution<Double> distribution(int[] succs, double[] probs)
	{
		Distribution<Double> distr = Distribution.ofDouble();
		for (int k = 0; k < succs.length; k++) {
			distr.add(succs[k], probs[k]);
		}
		return distr;
	}

	private static List<Integer> toList(PrimitiveIterator.OfInt it)
	{
		List<Integer> list = new ArrayList<>();
		while (it.hasNext()) {
			list.add(it.nextInt());
		}
		return list;
	}
}
