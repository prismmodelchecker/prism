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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import explicit.Distribution;
import explicit.MDPSimple;
import explicit.rewards.MDPRewardsSimple;
import prism.PrismException;
import prism.PrismPrintStreamLog;

/**
 * Tests for {@link PrismExplicitExporter#exportTransRewards}.
 *
 * The nondet/MDP branch must bound its per-state choice loop with the
 * per-state {@code getNumChoices(s)}, not the model-wide, summed-over-all-states
 * {@code getNumChoices()}. Using the model-wide count lets the loop visit choice
 * indices that don't exist for a given state whenever an earlier/other state has
 * more choices. The {@link explicit.rewards.Rewards} API does not validate that
 * rewards are only ever set for a state's real choices, so a reward accidentally
 * (or via some other bug) recorded at such an out-of-range index would previously
 * be picked up, and the subsequent {@code NondetModel.getNumTransitions(s, j)}
 * call (e.g. {@code MDPSimple.getNumTransitions}) then indexes past the end of
 * that state's own choice list and throws.
 */
public class PrismExplicitExporterTest
{
	@Test
	void exportTransRewardsUsesPerStateChoiceCount() throws PrismException
	{
		// MDP with 3 states and a differing number of choices per state:
		// state 0 -> 2 choices, state 1 -> 1 choice, state 2 -> 3 choices.
		// Model-wide total (NondetModel.getNumChoices()) is 2 + 1 + 3 = 6,
		// which exceeds the per-state count for states 0 and 1.
		MDPSimple<Double> mdp = new MDPSimple<>(3);
		mdp.addChoice(0, singleton(1)); // state 0, choice 0 -> state 1
		mdp.addChoice(0, singleton(2)); // state 0, choice 1 -> state 2
		mdp.addChoice(1, singleton(0)); // state 1, choice 0 -> state 0
		mdp.addChoice(2, singleton(0)); // state 2, choice 0 -> state 0
		mdp.addChoice(2, singleton(1)); // state 2, choice 1 -> state 1
		mdp.addChoice(2, singleton(2)); // state 2, choice 2 -> state 2

		MDPRewardsSimple<Double> rewards = new MDPRewardsSimple<>(3);
		rewards.setTransitionReward(0, 0, 1.5);
		rewards.setTransitionReward(0, 1, 2.5);
		rewards.setTransitionReward(1, 0, 3.5);
		rewards.setTransitionReward(2, 0, 4.5);
		rewards.setTransitionReward(2, 1, 5.5);
		rewards.setTransitionReward(2, 2, 6.5);
		// A stale/out-of-range reward for state 1, choice 3: state 1 only has a
		// single real choice (index 0). Nothing in the Rewards API prevents this
		// from being recorded, so it must be safely ignored on export rather than
		// crash or leak into the output.
		rewards.setTransitionReward(1, 3, 9.5);

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		PrismPrintStreamLog log = new PrismPrintStreamLog(new PrintStream(bytes));
		PrismExplicitExporter<Double> exporter = new PrismExplicitExporter<>(new ModelExportOptions().setPrintHeaders(false));

		exporter.exportTransRewards(mdp, rewards, "", log);
		log.flush();

		List<String> lines = new ArrayList<>();
		for (String line : bytes.toString().split("\n")) {
			if (!line.isEmpty()) {
				lines.add(line);
			}
		}

		// Header line: numStates numChoices(model-wide) numNonZeroRewardTransitions.
		// The model-wide choice count (6) is correct here regardless of the fix,
		// but the count of non-zero reward transitions must only reflect the 6
		// real per-state choices, not the stale out-of-range entry.
		assertEquals("3 6 6", lines.get(0));

		List<String> expected = List.of(
				"0 0 1 1.5",
				"0 1 2 2.5",
				"1 0 0 3.5",
				"2 0 0 4.5",
				"2 1 1 5.5",
				"2 2 2 6.5");
		assertEquals(expected, lines.subList(1, lines.size()));
	}

	private static Distribution<Double> singleton(int target)
	{
		Distribution<Double> distr = Distribution.ofDouble();
		distr.add(target, 1.0);
		return distr;
	}
}
