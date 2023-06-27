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
import java.util.List;

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
	 * Helper class for defining the edges.
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
	 * Helper class for defining the transitions.
	 * This data type will denote one of the two:
	 * 1) Several pairs of form (state, interval) denoting the distribution of an uncertain state
	 * 2) Several pairs of form (state, [-1, 1]) denoting the choices in an action state
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
		List<Object> simpleIPOMDP = constructSimpleIPOMDP(ipomdp);
		ArrayList<Integer> uncertainStates = (ArrayList<Integer>) simpleIPOMDP.get(0);
		ArrayList<Integer> actionStates = (ArrayList<Integer>) simpleIPOMDP.get(1);
		Distribution[] transitions = (Distribution[]) simpleIPOMDP.get(2);

		// Return dummy result vector
		ModelCheckerResult res = new ModelCheckerResult();
		int total = ipomdp.getNumStates();
		res.soln = new double[total];
		return res;
	}

	/**
	 * Transform the IPOMDP into a binary/simple IPOMDP.
	 * @param ipomdp The IPOMDP which must be transformed
	 */
	public List<Object> constructSimpleIPOMDP(IPOMDP<Double> ipomdp)
	{
		int numStates = ipomdp.getNumStates();
		int gadget[] = new int[numStates];
		Arrays.fill(gadget, -1);

		int numStatesTransformation = 0;
		for (int s = 0; s < ipomdp.getNumStates(); s++)
			numStatesTransformation = numStatesTransformation + 2 * ipomdp.getNumChoices(s) - 1;

		ArrayList<Integer> uncertainStates = new ArrayList<Integer>();
		ArrayList<Integer> actionStates = new ArrayList<Integer>();
		Distribution[] transitions = new Distribution[numStatesTransformation];

		int index = -1;
		for (int s = 0; s < numStates; s++)
			index = transformState(s, index, gadget, ipomdp, uncertainStates, actionStates, transitions);

		return Arrays.asList(uncertainStates, actionStates, transitions);
	}

	/**
	 * Transform each state of the initial IPODMP into a "gadget" of the binary/simple IPOMDP.
	 * @param state The state which must be transformed into a gadget
	 * @param index The current index denoting the last state created in the binary/simple IPOMDP
	 * @param gadget Array of indices showing for each state in the initial IPOMDP where is the gadget in the binary/simple IPOMDP
	 * @param uncertainStates The uncertain states of the binary/simple IPOMDP will be stored here
	 * @param actionStates The action states of the binary/simple IPOMDP will be stored here
	 * @param transitions The transitions of the binary/simple IPOMDP will be stored here
	 */
	public int transformState(int state, int index, int[] gadget, IPOMDP<Double> ipomdp, ArrayList<Integer> uncertainStates, ArrayList<Integer> actionStates, Distribution[] transitions)
	{
		index = discoverState(state, index, gadget);

		int numChoices = ipomdp.getNumChoices(state);
		for (int dummy = 0; dummy < numChoices - 1; dummy++) {
			int currState = (dummy > 0 ? ++index : gadget[state]);

			actionStates.add(currState);
			Distribution distribution = new Distribution();
			distribution.add(new Edge(index + 1, new Interval<>(-1.0, 1.0)));
			distribution.add(new Edge(index + numChoices, new Interval<>(-1.0, 1.0)));
			transitions[currState] = distribution;
		}

		int index_next = (numChoices == 1 ? index : index + numChoices);
		for (int dummy = 0; dummy < numChoices; dummy++) {
			int currState = (numChoices > 1 ? ++index : gadget[state]);

			uncertainStates.add(currState);
			Distribution distribution = new Distribution();
			Iterator<Map.Entry<Integer, Interval<Double>>> iterator = ipomdp.getTransitionsIterator(state, dummy);
			while (iterator.hasNext()) {
				Map.Entry<Integer, Interval<Double>> elem = iterator.next();
				int successor = elem.getKey();
				Interval<Double> interval = elem.getValue();

				index_next = discoverState(successor, index_next, gadget);
				distribution.add(new Edge(gadget[successor], interval));
			}

			transitions[currState] = distribution;
		}

		return index_next;
	}

	/**
	 * Discover a state in the initial IPOMDP and assign to it an index in the binary/simple IPOMDP.
	 * @param state The state which must be discovered
	 * @param index The current index denoting the last state created in the binary/simple IPOMDP
	 * @param gadget Array of indices showing for each state in the initial IPOMDP where is the gadget in the binary/simple IPOMDP
	 */
	public int discoverState(int state, int index, int[] gadget)
	{
		// Verify whether the state has already been transformed or not
		if (gadget[state] < 0) gadget[state] = ++index;
		return index;
	}
}
