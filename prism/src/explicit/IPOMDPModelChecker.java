//==============================================================================
//
//	Copyright (c) 2023-
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

package explicit;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.ArrayList;
import java.util.Arrays;

import acceptance.AcceptanceReach;
import common.IntSet;
import common.Interval;
import common.IterableStateSet;
import explicit.rewards.MDPRewards;
import explicit.rewards.Rewards;
import parser.ast.Expression;
import prism.AccuracyFactory;
import prism.Evaluator;
import prism.PrismComponent;
import prism.PrismException;
import strat.FMDStrategyStep;
import prism.PrismFileLog;
import strat.FMDStrategyProduct;
import strat.MDStrategy;
import strat.MDStrategyArray;
import strat.Strategy;
import gurobi.*;

/**
 * Explicit-state model checker for interval Markov decision prcoesses (IMDPs).
 */
public class IPOMDPModelChecker extends ProbModelChecker
{
	/**
	 * Helper class for defining an edge
	 */
	private class Edge<X, Y> {
		public final X state;
		public final Y interval;
		public Edge(X state, Y interval) {
			this.state = state;
			this.interval= interval;
		}
	}

	/**
	 * Helper class for the complex type
	 * This is either one of the two:
	 * 1) Distribution found in an uncertain state
	 * 2) Action choices found in an action state
	 */
	private class Distribution extends ArrayList<Edge<Integer, Interval<Double>>> {

	}

	/**
	 * Create a new IPOMDPModelChecker, inherit basic state from parent (unless null).
	 */
	public IPOMDPModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);
	}

	/**
	 * Compute until probabilities.
	 * i.e. compute the probability of reaching a state in {@code target}.
	 * @param ipomdp The IPOMDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeUntilProbs(IPOMDP<Double> ipomdp, BitSet remain, BitSet target, MinMax minMax) throws PrismException
	{
		return computeReachProbs(ipomdp, remain, target, minMax);
	}

	/**
	 * Compute reachability/until probabilities.
	 * i.e. compute the probability of reaching a state in {@code target},
	 * while remaining in those in {@code remain}.
	 * @param ipomdp The IPOMDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeReachProbs(IPOMDP<Double> ipomdp, BitSet remain, BitSet target, MinMax minMax) throws PrismException
	{
		int n = 0;
		for (int s = 0; s < ipomdp.getNumStates(); s++)
			n = n + 2 * ipomdp.getNumChoices(s) - 1;

		ArrayList<Integer> uncertainStates = new ArrayList<Integer>();
		ArrayList<Integer> actionStates = new ArrayList<Integer>();
		Distribution[] transitions = new Distribution[n];
		constructSimpleIPOMDP(ipomdp, uncertainStates, actionStates, transitions);

		// Return dummy result vector
		ModelCheckerResult res = new ModelCheckerResult();
		int total = ipomdp.getNumStates();
		res.soln = new double[total];
		return res;
	}

	/**
	 * Transform the IPOMDP into a binary/simple IPOMDP.
	 * @param ipomdp The IPOMDP which is transformed
	 * @param uncertainStates The uncertain states of the binary/simple IPOMDP will be stored here
	 * @param actionStates The action states of the binary/simple IPOMDP will be stored here
	 * @param transitions The transitions of the binary/simple IPOMDP will be stored here
	 */
	public void constructSimpleIPOMDP(IPOMDP<Double> ipomdp, ArrayList<Integer> uncertainStates, ArrayList<Integer> actionStates, Distribution[] transitions)
	{
		int numStates = ipomdp.getNumStates();
		int gadget[] = new int[numStates];
		Arrays.fill(gadget, -1);

		int index = -1;
		for (int s = 0; s < numStates; s++)
			index = transformState(s, index, gadget, ipomdp, uncertainStates, actionStates, transitions);
	}

	/**
	 * Transform each state of the initial IPODMP into a "gadget" of the binary/simple IPOMDP (binary/simple)
	 * @param s The state which is transformed into a gadget
	 * @param index The current index denoting the last state created in the binary/simple IPOMDP
	 * @param gadget Array of indices showing for each state in the initial IPOMDP where is the gadget in the binary/simple IPOMDP
	 */
	public int transformState(int s, int index, int[] gadget, IPOMDP<Double> ipomdp, ArrayList<Integer> uncertainStates, ArrayList<Integer> actionStates, Distribution[] transitions)
	{
		index = discoverState(s, index, gadget);

		int numChoices = ipomdp.getNumChoices(s);
		if (numChoices == 1) {
			uncertainStates.add(gadget[s]);

			Iterator<Map.Entry<Integer, Interval<Double>>> iterator = ipomdp.getTransitionsIterator(s, 0);
			Distribution distribution = new Distribution();
			while (iterator.hasNext()) {
				Map.Entry<Integer, Interval<Double>> elem = iterator.next();
				int successor = elem.getKey();
				Interval<Double> interval = elem.getValue();

				index = discoverState(successor, index, gadget);
				distribution.add(new Edge(gadget[successor], interval));
			}

			transitions[ gadget[s] ] = distribution;
		} else {
			for (int dummy = 0; dummy < numChoices - 1; dummy++) {
				int currState = gadget[s];
				if (dummy > 0) currState = ++index;
				actionStates.add(currState);

				Distribution distribution = new Distribution();
				distribution.add(new Edge(index + 1, new Interval<>(-2.0, 2.0)));
				distribution.add(new Edge(index + numChoices, new Interval<>(-1.0, 1.0)));
				transitions[currState] = distribution;
			}

			for (int dummy = 0; dummy < numChoices; dummy++) {
				int currState = ++index;
				uncertainStates.add(currState);

				Iterator<Map.Entry<Integer, Interval<Double>>> iterator = ipomdp.getTransitionsIterator(s, dummy);
				Distribution distribution = new Distribution();
				while (iterator.hasNext()) {
					Map.Entry<Integer, Interval<Double>> elem = iterator.next();
					int successor = elem.getKey();
					Interval<Double> interval = elem.getValue();

					index = discoverState(successor, index, gadget);
					distribution.add(new Edge(gadget[successor], interval));
				}

				transitions[currState] = distribution;
			}
		}

		return index;
	}

	/**
	 * Discover a state in the initial IPOMDP and assign to it an index in the binary/simple IPOMDP
	 * @param s The state which must be discovered
	 * @param index The current index denoting the last state created in the binary/simple IPOMDP
	 * @param gadget Array of indices showing for each state in the initial IPOMDP where is the gadget in the binary/simple IPOMDP
	 */
	public int discoverState(int s, int index, int[] gadget)
	{
		// Verify whether the state has already been transformed or not
		if (gadget[s] < 0) gadget[s] = ++index;
		return index;
	}
}
