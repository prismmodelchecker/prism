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
		// Construct the binary/simple version of our IPOMDP
		List<Object> simpleIPOMDP = constructSimpleIPOMDP(ipomdp);
		ArrayList<Integer> uncertainStates = (ArrayList<Integer>) simpleIPOMDP.get(0);
		ArrayList<Integer> actionStates = (ArrayList<Integer>) simpleIPOMDP.get(1);
		Distribution[] transitions = (Distribution[]) simpleIPOMDP.get(2);
		int[] observations = (int[]) simpleIPOMDP.get(3);

		// LP with Gurobi
		try {
			// Number of states in the simple IPOMDP
			int n = uncertainStates.size() + actionStates.size();

			// Set up the environment for Gurobi
			GRBEnv env = new GRBEnv("gurobi.log");
			GRBModel model = new GRBModel(env);

			// Create the variables for the probabilities
			GRBVar probVars[] = new GRBVar[n];
			for (int s = 0; s < n; s++) {
				probVars[s] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "p" + s);
			}

			// Create the variables for the policy
			GRBVar policyVars[] = new GRBVar[2 * n];

			// Handle the policy variables for the uncertain states
			// There will be exactly one such variable for each state
			for (int s : uncertainStates) {
				policyVars[2 * s] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "policy" + s + "a");

				// The policy must be a valid distribution
				GRBLinExpr validDistribution = new GRBLinExpr();
				validDistribution.addTerm(1.0, policyVars[2 * s]);
				model.addConstr(validDistribution, GRB.EQUAL, 1.0, "policyUncertainState" + s);
			}

			// Handle the policy variables for the action states
			// There will be exactly two such variables for each state
			for (int s : actionStates) {
				policyVars[2 * s] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "policy" + s + "a");
				policyVars[2 * s + 1] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "policy" + s + "b");

				// The policy must be a valid distribution
				GRBLinExpr validDistribution = new GRBLinExpr();
				validDistribution.addTerm(1.0, policyVars[2 * s]);
				validDistribution.addTerm(1.0, policyVars[2 * s + 1]);
				model.addConstr(validDistribution, GRB.EQUAL, 1.0, "policyActionState" + s);
				
				// Strictly greater than zero
				for (int k = 0; k <= 1; k++) {
					double nonZero = 1e-9;
					GRBLinExpr underlyingGraph = new GRBLinExpr();
					underlyingGraph.addTerm(1.0, policyVars[2 * s + k]);
					model.addConstr(underlyingGraph, GRB.GREATER_EQUAL, nonZero, "underlyingGraph" + (s + k));
				}
			}

			// Optimise the model
			model.optimize();

			// Dispose of model and environment
			model.dispose();
			env.dispose();
		} catch (GRBException e) {
			throw new PrismException("Error solving LP: " +  e.getMessage());
		}

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
		List<Object> supportStructure = determineSupport(ipomdp);

		ArrayList<Integer> uncertainStates = (ArrayList<Integer>) supportStructure.get(0);
		ArrayList<Integer> actionStates = (ArrayList<Integer>) supportStructure.get(1);
		Distribution[] transitions = (Distribution[]) supportStructure.get(2);
		int[] observations = determineObservations(ipomdp, supportStructure);

		return Arrays.asList(uncertainStates, actionStates, transitions, observations);
	}

	/**
	 * Determine the support graph of the binary/simple IPOMDP (i.e. states, transitions).
	 * @param ipomdp The IPOMDP which must be transformed
	 */
	public List<Object> determineSupport(IPOMDP<Double> ipomdp)
	{
		int numStates = ipomdp.getNumStates();

		// Find the exact number of states of the binary/simple variant
		int numStatesAfterProcess = 0;
		for (int s = 0; s < ipomdp.getNumStates(); s++)
			numStatesAfterProcess = numStatesAfterProcess + 2 * ipomdp.getNumChoices(s) - 1;

		// Function which maps each state of the initial IPOMDP to the "gadget" in the binary/simple variant
		int gadget[] = new int[numStates];
		Arrays.fill(gadget, -1);

		// List of states showing the order in which states were created in the binary/simple variant
		ArrayList<Integer> traversal = new ArrayList<>();

		// Representation of the support graph
		ArrayList<Integer> uncertainStates = new ArrayList<Integer>();
		ArrayList<Integer> actionStates = new ArrayList<Integer>();
		Distribution[] transitions = new Distribution[numStatesAfterProcess];

		int lastStateAdded = -1;
		for (int state = 0; state < ipomdp.getNumStates(); state++) {
			if (gadget[state] < 0) gadget[state] = ++lastStateAdded;

			int numChoices = ipomdp.getNumChoices(state);
			for (int dummy = 0; dummy < numChoices - 1; dummy++) {
				int currState = (dummy == 0 ? gadget[state]: ++lastStateAdded);
				traversal.add(currState);

				actionStates.add(currState);
				Distribution distribution = new Distribution();
				distribution.add(new Edge(lastStateAdded + 1, new Interval<>(-1.0, 1.0)));
				distribution.add(new Edge(lastStateAdded + numChoices, new Interval<>(-1.0, 1.0)));
				transitions[currState] = distribution;
			}

			int lastStateAddedFuture = (numChoices == 1 ? lastStateAdded : lastStateAdded + numChoices);
			for (int choice = 0; choice < numChoices; choice++) {
				int currState = (numChoices == 1 ? gadget[state] : ++lastStateAdded);
				traversal.add(currState);

				uncertainStates.add(currState);
				Distribution distribution = new Distribution();
				Iterator<Map.Entry<Integer, Interval<Double>>> iterator = ipomdp.getTransitionsIterator(state, choice);
				while (iterator.hasNext()) {
					Map.Entry<Integer, Interval<Double>> elem = iterator.next();
					int successor = elem.getKey();
					Interval<Double> interval = elem.getValue();

					if (gadget[successor] < 0) gadget[successor] = ++lastStateAddedFuture;
					distribution.add(new Edge(gadget[successor], interval));
				}

				transitions[currState] = distribution;
			}

			lastStateAdded = lastStateAddedFuture;
		}

		return Arrays.asList(uncertainStates, actionStates, transitions, traversal, gadget);
	}

	/**
	 * Determine the observations of the binary/simple IPOMDP.
	 * @param ipomdp The IPOMDP which must be transformed
	 * @param supportStructure The support graph of the binary/simple IPOMDP
	 */
	public int[] determineObservations(IPOMDP<Double> ipomdp, List<Object> supportStructure)
	{
		ArrayList<Integer> traversal = (ArrayList<Integer>) supportStructure.get(3);
		int[] gadget = (int[]) supportStructure.get(4);

		int numStates = ipomdp.getNumStates();
		int numStatesAfterProcess = traversal.size();

		// Inverse (partial) function of the gadget function
		int[] gadget_inv = new int[numStatesAfterProcess];
		Arrays.fill(gadget_inv, -1);
		for (int s = 0; s < numStates; s++)
			gadget_inv[ gadget[s] ] = s;

		// Function which maps each observation of the initial IPOMDP to some observation in the new one
		int[] freshObservations = new int[numStates];
		Arrays.fill(freshObservations, -1);

		// Array of observations for the states in the binary/simple variant
		int[] observations = new int[numStatesAfterProcess];

		int lastObservationAdded = -1;
		int indexObservation = -1;
		for (int i = 0; i < traversal.size(); i++) {
			int state = traversal.get(i);

			// If the current state in the binary/simple variant is not the head of any gadget
			if (gadget_inv[state] < 0) {
				lastObservationAdded = Math.max(lastObservationAdded, indexObservation);
				observations[state] = indexObservation++;
				continue;
			}

			int initialState = gadget_inv[state];
			int initialObservation = ipomdp.getObservation(initialState);
			if (freshObservations[initialObservation] < 0)
				freshObservations[initialObservation] = ++lastObservationAdded;

			indexObservation = freshObservations[initialObservation];
			observations[state] = indexObservation++;
		}

		return observations;
	}
}
