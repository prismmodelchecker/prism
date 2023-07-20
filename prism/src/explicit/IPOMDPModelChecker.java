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

import java.lang.reflect.Array;
import java.util.*;

import acceptance.AcceptanceReach;
import common.IntSet;
import common.Interval;
import common.IterableStateSet;
import explicit.rewards.MDPRewards;
import explicit.rewards.Rewards;
import org.apache.logging.log4j.core.util.ArrayUtils;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.platform.console.shadow.picocli.CommandLine;
import parser.State;
import parser.ast.Expression;
import prism.*;
import strat.FMDStrategyStep;
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
	 * Parameters of the algorithm are stored within this class.
	 */
	private class Parameters {
		public double penaltyWeight;
		public double trustRegion;
		public double regionChangeFactor;
		public double regionThreshold;

		public Parameters(double penaltyWeight, double trustRegion, double regionChangeFactor, double regionThreshold) {
			this.penaltyWeight = penaltyWeight;
			this.trustRegion = trustRegion;
			this.regionChangeFactor = regionChangeFactor;
			this.regionThreshold = regionThreshold;
		}
	}

	private class TransformIntoSimpleIPOMDP {
		private SimpleIPOMDP simpleIPOMDP;
		private IPOMDP<Double> initialIPOMDP;
		private int[] gadget;
		private ArrayList<Integer> traversal;

		public TransformIntoSimpleIPOMDP(IPOMDP<Double> initialIPOMDP) {
			this.initialIPOMDP = initialIPOMDP;
			this.simpleIPOMDP = new SimpleIPOMDP();
			determineSupportGraph(initialIPOMDP);
			determineObservations(initialIPOMDP);

		}

		public int[] getGadgetMapping() {
			return this.gadget;
		}

		public ArrayList<Integer> getOrderOfTraversal() {
			return this.traversal;
		}

		public SimpleIPOMDP getSimpleIPOMDP() {
			return this.simpleIPOMDP;
		}

		public IPOMDP<Double> getInitialIPOMDP() {
			return this.initialIPOMDP;
		}

		public BitSet computeNewTargetGivenInitial(BitSet initialTarget) {
			BitSet target = new BitSet();
			int indexOfGoalState = initialTarget.nextSetBit(0);
			while (indexOfGoalState >= 0) {
				target.set(gadget[indexOfGoalState]);
				indexOfGoalState = initialTarget.nextSetBit(indexOfGoalState + 1);
			}

			return target;
		}

		private void determineSupportGraph(IPOMDP<Double> ipomdp) {
			int numStates = ipomdp.getNumStates();

			// Find the exact number of states of the binary/simple variant
			int numStatesAfterProcess = 0;
			for (int s = 0; s < ipomdp.getNumStates(); s++)
				numStatesAfterProcess = numStatesAfterProcess + 2 * ipomdp.getNumChoices(s) - 1;

			// Function which maps each state of the initial IPOMDP to the "gadget" in the binary/simple variant
			gadget = new int[numStates];
			Arrays.fill(gadget, -1);

			// List of states showing the order in which states were created in the binary/simple variant
			 traversal = new ArrayList<>();

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

			simpleIPOMDP.uncertainStates = uncertainStates;
			simpleIPOMDP.actionStates = actionStates;
			simpleIPOMDP.transitions = transitions;
		}

		private void determineObservations(IPOMDP<Double> ipomdp) {
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

			simpleIPOMDP.observations = observations;
		}
	}

	private class SimpleIPOMDP {
		public ArrayList<Integer> uncertainStates;
		public ArrayList<Integer> actionStates;
		public Distribution[] transitions;
		public int[] observations;

		public SimpleIPOMDP() {}

		public SimpleIPOMDP(ArrayList<Integer> uncertainStates, ArrayList<Integer> actionStates, Distribution[] transitions, int[] observations) {
			this.uncertainStates = uncertainStates;
			this.actionStates = actionStates;
			this.transitions = transitions;
			this.observations = observations;
		}

		public Integer getNumStates() {
			return uncertainStates.size() + actionStates.size();
		}
	}

	private class SpecificationDetails {

		private BitSet target;
		MinMax minMax;

		public SpecificationDetails(BitSet target, MinMax minMax) {
			this.target = target;
			this.minMax = minMax;
		}
	}

	static private class InducedDTMCFromIPOMDPAndPolicy {
		static public double[] ComputeReachabilityProbabilities(SimpleIPOMDP simpleIPOMDP, double[] policy, SpecificationDetails specification) throws PrismException {
			IDTMCSimple<Double> inducedIDTMC = createInducedIDTMC(simpleIPOMDP, policy);
			return computeReachProbs(inducedIDTMC, specification);
		}

		static private IDTMCSimple<Double> createInducedIDTMC(SimpleIPOMDP simpleIPOMDP, double[] policy) throws PrismException
		{
			// Create explicit IDTMC
			IDTMCSimple<Double> inducedIDTMC = new IDTMCSimple<>(simpleIPOMDP.getNumStates());

			// Add transitions from uncertain states
			for (int state : simpleIPOMDP.uncertainStates)
				for (Edge<Integer, Interval<Double>> transition : simpleIPOMDP.transitions[state]) {
					int successor = transition.state;
					Interval<Double> interval = transition.interval;

					inducedIDTMC.setProbability(state, successor, interval);
				}

			// Add transitions from action states
			for (int state : simpleIPOMDP.actionStates)
				for (int k = 0; k <= 1; k++) {
					int successor = simpleIPOMDP.transitions[state].get(k).state;
					Interval<Double> interval = new Interval<>(policy[2 * state + k], policy[2 * state + k]);

					inducedIDTMC.setProbability(state, successor, interval);
				}

			return inducedIDTMC;
		}

		static private double[] computeReachProbs(IDTMCSimple<Double> IDTMC, SpecificationDetails specification) throws PrismException
		{
			IDTMCModelChecker modelChecker = new IDTMCModelChecker(null);
			modelChecker.inheritSettings(modelChecker);
			modelChecker.setLog(new PrismDevNullLog());
			modelChecker.setMaxIters(100);
			modelChecker.setErrorOnNonConverge(false);

			ModelCheckerResult res = modelChecker.computeReachProbs(IDTMC, specification.target, specification.minMax);
			return res.soln;
		}
	}

	static private class VariableHandler {
		static public double[] getProbabilitiesForInitialIPOMDP(Variables mainVariables, TransformIntoSimpleIPOMDP transformationProcess)
		{
			int[] gadget = transformationProcess.getGadgetMapping();
			double[] initialProb = new double[gadget.length];
			for (int i = 0; i < gadget.length; i++)
				initialProb[i] = mainVariables.prob[ gadget[i] ];
			return initialProb;
		}

		static public void initialiseVariables(Variables mainVariables, SimpleIPOMDP simpleIPOMDP, SpecificationDetails simpleSpecification) throws PrismException
		{
			int numStates = simpleIPOMDP.getNumStates();

			double[] policy = new double[2 * numStates];
			for (int s : simpleIPOMDP.uncertainStates) {
				policy[2 * s] = 1.0;
			}

			for (int s : simpleIPOMDP.actionStates) {
				policy[2 * s] = 0.5;
				policy[2 * s + 1] = 0.5;
			}

			double[] prob = InducedDTMCFromIPOMDPAndPolicy.ComputeReachabilityProbabilities(simpleIPOMDP, policy, simpleSpecification);
			mainVariables.policy = policy;
			mainVariables.prob = prob;
		}
	}

	private class Variables {
		public double[] policy;
		public double[] prob;

		public Variables() {}

		public Variables(double[] policy, double[] prob) {
			this.policy = policy;
			this.prob = prob;
		}
	}

	private class GurobiLinearProgramming {

		private SimpleIPOMDP simpleIPOMDP;
		private SpecificationDetails specification;
		private Variables mainVariables;
		private Parameters parameters;
		private GRBEnv env;
		private GRBModel model;
		private GRBVar probVars[];
		private GRBVar policyVars[];
		private GRBVar penaltyVars[];

		public GurobiLinearProgramming(SimpleIPOMDP simpleIPOMDP, SpecificationDetails specification, Variables mainVariables, Parameters parameters) throws GRBException {
			this.simpleIPOMDP = simpleIPOMDP;
			this.specification = specification;
			this.mainVariables = mainVariables;
			this.parameters = parameters;
			initialiseModelAndVariables(simpleIPOMDP);
		}

		private void initialiseModelAndVariables(SimpleIPOMDP simpleIPOMDP) throws GRBException {
			int n = simpleIPOMDP.getNumStates();
			this.env = new GRBEnv("gurobi.log");
			this.model = new GRBModel(env);
			this.probVars = new GRBVar[n];
			this.policyVars = new GRBVar[2 * n];
			this.penaltyVars = new GRBVar[n];

			// Handle the probability variables
			for (int s = 0; s < n; s++) {
				probVars[s] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "p" + s);
			}

			// Handle the policy variables for the uncertain states
			// There will be exactly one such variable for each state
			for (int s : simpleIPOMDP.uncertainStates) {
				policyVars[2 * s] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "policy" + s + "a");
			}

			// Handle the policy variables for the action states
			// There will be exactly two such variables for each state
			for (int s : simpleIPOMDP.actionStates) {
				policyVars[2 * s] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "policy" + s + "a");
				policyVars[2 * s + 1] = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "policy" + s + "b");
			}

			// Create the penalty variables
			for (int s = 0; s < n; s++) {
				penaltyVars[s] = model.addVar(0.0, 1e9, 0.0, GRB.CONTINUOUS, "penalty" + s);
			}
		}

		public Variables solveAndGetOptimalVariables() throws GRBException, PrismException
		{
			// Add constraints about the policy
			policyMustBeObservationBased();
			policyMustPreserveUnderlyingGraph();
			policyMustBeValidDistribution();

			// Add constraints about the IPOMDP itself
			addConstraintsForGoalStates();
			addConstraintsForActionStates();
			addConstraintsForUncertainStates();
			addConstraintsForTrustRegions();

			// Add main objective
			addObjective();

			// Optimise the model
			model.optimize();

			// Extract optimal policy
			double[] policy = extractOptimalPolicy();

			// Extract optimal probabilities
			double[] prob = InducedDTMCFromIPOMDPAndPolicy.ComputeReachabilityProbabilities(simpleIPOMDP, policy, specification);

			// Dispose of model and environment
			model.dispose();
			env.dispose();

			return new Variables(policy, prob);
		}

		public double[] extractOptimalPolicy() throws GRBException
		{
			double[] optimalPolicy = new double[policyVars.length];
			for (int state : simpleIPOMDP.uncertainStates) {
				optimalPolicy[2 * state] = policyVars[2 * state].get(GRB.DoubleAttr.X);
			}

			for (int state : simpleIPOMDP.actionStates)
				for (int k = 0; k <= 1; k++) {
					optimalPolicy[2 * state + k] = policyVars[2 * state + k].get(GRB.DoubleAttr.X);
				}

			return optimalPolicy;
		}

		public void addObjective() throws GRBException
		{
			GRBLinExpr obj = new GRBLinExpr();
			obj.addTerm(1.0, probVars[0]);
			for (int state : simpleIPOMDP.actionStates) {
				obj.addTerm(-parameters.penaltyWeight, penaltyVars[state]);
			}

			model.setObjective(obj, GRB.MAXIMIZE);
		}

		public void addConstraintsForActionStates() throws GRBException
		{
			for (int state : simpleIPOMDP.actionStates) {
				// If current state is part of the target set then skip
				if (specification.target.get(state)) continue;

				GRBLinExpr constraint = new GRBLinExpr();
				constraint.addTerm(-1.0, probVars[state]);
				constraint.addTerm(1.0, penaltyVars[state]);

				double RHS = 0.0;
				for (int k = 0; k <= 1; k++) {
					int successor = simpleIPOMDP.transitions[state].get(k).state;

					constraint.addTerm(mainVariables.policy[2 * state + k], probVars[successor]);
					constraint.addTerm(mainVariables.prob[successor], policyVars[2 * state + k]);
					RHS = RHS + mainVariables.policy[2 * state + k] * mainVariables.prob[successor];
				}

				model.addConstr(constraint, GRB.GREATER_EQUAL, RHS, "actionState" + state);
			}
		}

		public void addConstraintsForGoalStates() throws GRBException
		{
			BitSet target = specification.target;
			int indexOfGoalState = target.nextSetBit(0);
			while (indexOfGoalState >= 0) {
				GRBLinExpr constraint = new GRBLinExpr();
				constraint.addTerm(1.0, probVars[indexOfGoalState]);
				model.addConstr(constraint, GRB.EQUAL, 1.0, "goalState" + indexOfGoalState);

				indexOfGoalState = target.nextSetBit(indexOfGoalState + 1);
			}
		}

		public void addConstraintsForTrustRegions() throws GRBException
		{
			int numStates = simpleIPOMDP.getNumStates();
			double[] prob = mainVariables.prob;
			double[] policy = mainVariables.policy;
			double trustRegion = parameters.trustRegion + 1;

			for (int s = 0; s < numStates; s++) {
				GRBLinExpr firstConstraint = new GRBLinExpr();
				firstConstraint.addTerm(1.0, probVars[s]);
				model.addConstr(firstConstraint, GRB.GREATER_EQUAL, prob[s] / trustRegion, "trustRegionProbLeft" + s);

				GRBLinExpr secondConstraint = new GRBLinExpr();
				secondConstraint.addTerm(1.0, probVars[s]);
				model.addConstr(secondConstraint, GRB.LESS_EQUAL, prob[s] * trustRegion, "trustRegionProbRight" + s);
			}

			for (int s : simpleIPOMDP.actionStates)
				for (int k = 0; k <= 1; k++) {
					GRBLinExpr firstConstraint = new GRBLinExpr();
					firstConstraint.addTerm(1.0, policyVars[2 * s + k]);
					model.addConstr(firstConstraint, GRB.GREATER_EQUAL, policy[2 * s + k] / trustRegion, "trustRegionPolicyLeft" + s);

					GRBLinExpr secondConstraint = new GRBLinExpr();
					secondConstraint.addTerm(1.0, policyVars[2 * s + k]);
					model.addConstr(secondConstraint, GRB.LESS_EQUAL, policy[2 * s + k] * trustRegion, "trustRegionPolicyRight" + s);
				}
		}

		public void addConstraintsForUncertainStates() throws GRBException
		{
			// We have the constraints a(i) <= u(i) <= b(i) and sum_i u(i) = 1
			// We encode these into the equation C * u + g >= 0
			for (int s : simpleIPOMDP.uncertainStates) {
				// If current state is part of the target set then skip
				if (specification.target.get(s)) continue;

				int n = simpleIPOMDP.transitions[s].size();
				int m = 2 * n + 2;

				// Keep track of the successor states
				ArrayList<Integer> successors = new ArrayList<>();

				// Create vector g
				double[] g = new double[m];

				// Populate successors and determine vector g
				for (int i = 0; i < n; i++) {
					Edge<Integer, Interval<Double>> transition = simpleIPOMDP.transitions[s].get(i);

					successors.add(transition.state);
					g[2 * i] = -transition.interval.getLower();
					g[2 * i + 1] = transition.interval.getUpper();
				}
				g[2 * n] = -1.0;
				g[2 * n + 1] = 1.0;

				// Create the dual variables
				GRBVar dualVars[] = new GRBVar[m];
				for (int i = 0; i < m; i++) {
					dualVars[i] = model.addVar(0.0, 1e9, 0.0, GRB.CONTINUOUS, "dual" + s + "," + i);
				}

				// Introduce the probability inequality
				GRBLinExpr inequality = new GRBLinExpr();
				inequality.addTerm(-1.0, probVars[s]);
				for (int i = 0; i < m; i++) {
					inequality.addTerm(g[i], dualVars[i]);
				}
				model.addConstr(inequality, GRB.GREATER_EQUAL, 0.0, "dualizationInequality" + s);

				// Introduce the dualization constraints
				for (int i = 0; i < n; i++) {
					int successor = successors.get(i);
					GRBLinExpr constraint = new GRBLinExpr();
					constraint.addTerm(1.0, probVars[successor]);

					constraint.addTerm(1.0, dualVars[2 * i]);
					constraint.addTerm(-1.0, dualVars[2 * i + 1]);

					constraint.addTerm(1.0, dualVars[2 * n]);
					constraint.addTerm(-1.0, dualVars[2 * n + 1]);

					model.addConstr(constraint, GRB.EQUAL, 0.0, "dualizationConstraint" + s + "," + i);
				}
			}
		}

		public void policyMustPreserveUnderlyingGraph() throws GRBException
		{
			double smallValue = 1e-5;

			for (int s : simpleIPOMDP.uncertainStates) {
				GRBLinExpr underlyingGraph = new GRBLinExpr();
				underlyingGraph.addTerm(1.0, policyVars[2 * s]);
				model.addConstr(underlyingGraph, GRB.GREATER_EQUAL, smallValue, "underlyingGraph" + s);
			}

			for (int s : simpleIPOMDP.actionStates)
				for (int k = 0; k <= 1; k++) {
					GRBLinExpr underlyingGraph = new GRBLinExpr();
					underlyingGraph.addTerm(1.0, policyVars[2 * s + k]);
					model.addConstr(underlyingGraph, GRB.GREATER_EQUAL, smallValue, "underlyingGraph" + (s + k));
				}
		}

		public void policyMustBeValidDistribution() throws GRBException
		{
			for (int s : simpleIPOMDP.uncertainStates) {
				GRBLinExpr validDistribution = new GRBLinExpr();
				validDistribution.addTerm(1.0, policyVars[2 * s]);
				model.addConstr(validDistribution, GRB.EQUAL, 1.0, "policyUncertainState" + s);
			}

			for (int s : simpleIPOMDP.actionStates) {
				GRBLinExpr validDistribution = new GRBLinExpr();
				validDistribution.addTerm(1.0, policyVars[2 * s]);
				validDistribution.addTerm(1.0, policyVars[2 * s + 1]);
				model.addConstr(validDistribution, GRB.EQUAL, 1.0, "policyActionState" + s);
			}
		}

		public void policyMustBeObservationBased() throws GRBException
		{
			int n = simpleIPOMDP.getNumStates();
			int[] leaderOfObservation = new int[n];
			for (int s = n - 1; s >= 0; s--)
				leaderOfObservation[ simpleIPOMDP.observations[s] ] = s;

			for (int s : simpleIPOMDP.uncertainStates) {
				int idx = leaderOfObservation[ simpleIPOMDP.observations[s] ];
				GRBLinExpr observationBased = new GRBLinExpr();
				observationBased.addTerm(1.0, policyVars[2 * s]);
				model.addConstr(observationBased, GRB.EQUAL, policyVars[2 * idx], "policyObservation" + s);
			}

			for (int s : simpleIPOMDP.actionStates)
				for (int k = 0; k <= 1; k++) {
					int idx = leaderOfObservation[ simpleIPOMDP.observations[s] ];
					GRBLinExpr observationBased = new GRBLinExpr();
					observationBased.addTerm(1.0, policyVars[2 * s + k]);
					model.addConstr(observationBased, GRB.EQUAL, policyVars[2 * idx + k], "policyObservation" + s);
				}
		}
	}

	private class Edge <X, Y> {
		public final X state;
		public final Y interval;
		public Edge(X state, Y interval) {
			this.state = state;
			this.interval= interval;
		}
	}

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
		// Construct the binary/simple version of the IPOMDP
		TransformIntoSimpleIPOMDP transformationProcess = new TransformIntoSimpleIPOMDP(ipomdp);
		SimpleIPOMDP simpleIPOMDP = transformationProcess.getSimpleIPOMDP();

		// Compute specification in the binary/simple version of the IPOMDP
		SpecificationDetails simpleSpecification = new SpecificationDetails(transformationProcess.computeNewTargetGivenInitial(target), minMax);

		// Initialise parameters
		Parameters parameters = new Parameters(1e4, 1.5, 1.5, 1e-4);

		// Initialise policy and therefore reaching probability vector
		Variables variablesOld = new Variables();
		VariableHandler.initialiseVariables(variablesOld, simpleIPOMDP, simpleSpecification);

		double objectiveOld = 0.0;
		while (parameters.trustRegion > parameters.regionThreshold) {
			Variables variablesNew;
			// Solve the Linear Programming
			try {
				GurobiLinearProgramming gurobiLP = new GurobiLinearProgramming(simpleIPOMDP, simpleSpecification, variablesOld, parameters);
				variablesNew = gurobiLP.solveAndGetOptimalVariables();
			} catch (GRBException e) {
				throw new PrismException("Error solving LP: " +  e.getMessage());
			}

			double objectiveNew = variablesNew.prob[0];
			if (objectiveNew > objectiveOld) {
				variablesOld = variablesNew;
				objectiveOld = objectiveNew;
				parameters.trustRegion = parameters.trustRegion * parameters.regionChangeFactor;
			} else {
				parameters.trustRegion = parameters.trustRegion / parameters.regionChangeFactor;
			}
		}

		// Return result vector
		ModelCheckerResult res = new ModelCheckerResult();
		res.soln = VariableHandler.getProbabilitiesForInitialIPOMDP(variablesOld, transformationProcess);
		return res;
	}
}
