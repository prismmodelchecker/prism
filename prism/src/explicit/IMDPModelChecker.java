//==============================================================================
//	
//	Copyright (c) 2020-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

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

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBQuadExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import com.gurobi.gurobi.GRBConstr;
import lpsolve.LpSolve;
import lpsolve.LpSolveException;

import explicit.trigger.*;


/**
 * Explicit-state model checker for interval Markov decision prcoesses (IMDPs).
 */
public class IMDPModelChecker extends ProbModelChecker
{
	// MDPModelChecker in order to use e.g. precomputation algorithms
	protected MDPModelChecker mcMDP = null; 
	
	/**
	 * Create a new IMDPModelChecker, inherit basic state from parent (unless null).
	 */
	public IMDPModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);
		mcMDP = new MDPModelChecker(this);
		mcMDP.inheritSettings(this);
	}

	// Model checking functions
	
	@Override
	@SuppressWarnings("unchecked")
	protected StateValues checkProbPathFormulaCosafeLTL(Model<?> model, Expression expr, boolean qual, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// Build product of IMDP and DFA for the LTL formula, and do any required exports
		LTLModelChecker mcLtl = new LTLModelChecker(this);
		LTLModelChecker.LTLProduct<IMDP<Double>> product = mcLtl.constructDFAProductForCosafetyProbLTL(this, (IMDP<Double>) model, expr, statesOfInterest);
		doProductExports(product);
		
		// Find accepting states + compute reachability probabilities
		BitSet acc = ((AcceptanceReach)product.getAcceptance()).getGoalStates();
		mainLog.println("\nComputing reachability probabilities...");
		IMDPModelChecker mcProduct = new IMDPModelChecker(this);
		mcProduct.inheritSettings(this);
		ModelCheckerResult res = mcProduct.computeReachProbs(product.getProductModel(), acc, minMax);
		StateValues probsProduct = StateValues.createFromArrayResult(res, product.getProductModel());

		// Output vector over product, if required
		if (getExportProductVector()) {
				mainLog.println("\nExporting product solution vector matrix to file \"" + getExportProductVectorFilename() + "\"...");
				PrismFileLog out = new PrismFileLog(getExportProductVectorFilename());
				probsProduct.print(out, false, false, false, false);
				out.close();
		}

		// If a strategy was generated, lift it to the product and store
		if (res.strat != null) {
			Strategy<Double> stratProduct = new FMDStrategyProduct<>(product, (MDStrategy<Double>) res.strat);
			result.setStrategy(stratProduct);
		}
		
		// Mapping probabilities in the original model
		StateValues probs = product.projectToOriginalModel(probsProduct);
		probsProduct.clear();

		return probs;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected StateValues checkRewardCoSafeLTL(Model<?> model, Rewards<?> modelRewards, Expression expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// Build product of IMDP and DFA for the LTL formula, convert rewards and do any required exports
		LTLModelChecker mcLtl = new LTLModelChecker(this);
		LTLModelChecker.LTLProduct<IMDP<Double>> product = mcLtl.constructDFAProductForCosafetyReward(this, (IMDP<Double>) model, expr, statesOfInterest);
		MDPRewards<Double> productRewards = ((MDPRewards<Double>) modelRewards).liftFromModel(product);
		doProductExports(product);

		// Find accepting states + compute reachability rewards
		BitSet acc = ((AcceptanceReach)product.getAcceptance()).getGoalStates();
		mainLog.println("\nComputing reachability rewards...");
		IMDPModelChecker mcProduct = new IMDPModelChecker(this);
		mcProduct.inheritSettings(this);
		ModelCheckerResult res = mcProduct.computeReachRewards(product.getProductModel(), productRewards, acc, minMax);
		StateValues rewardsProduct = StateValues.createFromArrayResult(res, product.getProductModel());
		
		// Output vector over product, if required
		if (getExportProductVector()) {
				mainLog.println("\nExporting product solution vector matrix to file \"" + getExportProductVectorFilename() + "\"...");
				PrismFileLog out = new PrismFileLog(getExportProductVectorFilename());
				rewardsProduct.print(out, false, false, false, false);
				out.close();
		}

		// If a strategy was generated, lift it to the product and store
		if (res.strat != null) {
			Strategy<Double> stratProduct = new FMDStrategyProduct<>(product, (MDStrategy<Double>) res.strat);
			result.setStrategy(stratProduct);
		}
		
		// Mapping rewards in the original model
		StateValues rewards = product.projectToOriginalModel(rewardsProduct);
		rewardsProduct.clear();
		
		return rewards;
	}
	
	// Numerical computation functions

	/**
	 * Compute next-state probabilities.
	 * i.e. compute the probability of being in a state in {@code target} in the next step.
	 * @param imdp The IMDP
	 * @param target Target states
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeNextProbs(IMDP<Double> imdp, BitSet target, MinMax minMax) throws PrismException
	{
		long timer = System.currentTimeMillis();

		// Check for any zero lower probability bounds (not supported
		// since this approach assumes the graph structure remains static)
		imdp.checkLowerBoundsArePositive();
		
		// Store num states
		int n = imdp.getNumStates();
		PrimitiveIterator.OfInt statesAll = new IterableStateSet(n).iterator();

		// Create/initialise solution vector(s)
		double[] soln = Utils.bitsetToDoubleArray(target, n);
		double[] soln2 = new double[n];

		// Next-step probabilities
		imdp.mvMultUnc(soln, minMax, soln2, statesAll, null);

		// Return results
		ModelCheckerResult res = new ModelCheckerResult();
		res.accuracy = AccuracyFactory.boundedNumericalIterations();
		res.soln = soln2;
		res.numIters = 1;
		timer = System.currentTimeMillis() - timer;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Compute bounded reachability probabilities.
	 * i.e. compute the probability of reaching a state in {@code target} within k steps.
	 * @param imdp The IMDP
	 * @param target Target states
	 * @param k Bound
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeBoundedReachProbs(IMDP<Double> imdp, BitSet target, int k, MinMax minMax) throws PrismException
	{
		return computeBoundedUntilProbs(imdp, null, target, k, minMax);
	}

	/**
	 * Compute bounded until probabilities.
	 * i.e. compute the probability of reaching a state in {@code target},
	 * within k steps, and while remaining in states in {@code remain}.
	 * @param imdp The IMDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param k Bound
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeBoundedUntilProbs(IMDP<Double> imdp, BitSet remain, BitSet target, int k, MinMax minMax) throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet unknown;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		long timer;
		int strat[] = null;
		FMDStrategyStep<?> fmdStrat = null;

		// Start bounded probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting bounded probabilistic reachability...");

		// Check for any zero lower probability bounds (not supported
		// since this approach assumes the graph structure remains static)
		imdp.checkLowerBoundsArePositive();
		
		// Store num states
		n = imdp.getNumStates();

		// Create solution vector(s)
		soln = new double[n];
		soln2 = new double[n];

		// If required, create/initialise strategy storage
		// Set choices to -1, denoting unknown
		// (except for target states, which are -2, denoting arbitrary)
		if (genStrat) {
			strat = new int[n];
			for (i = 0; i < n; i++) {
				strat[i] = target.get(i) ? -2 : -1;
			}
			fmdStrat = new FMDStrategyStep<>(imdp, k);
		}

		// Initialise solution vectors.
		for (i = 0; i < n; i++)
			soln[i] = soln2[i] = target.get(i) ? 1.0 : 0.0;

		// Determine set of states actually need to perform computation for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(target);
		if (remain != null)
			unknown.and(remain);
		IntSet unknownStates = IntSet.asIntSet(unknown);

		// Start iterations
		iters = 0;
		while (iters < k) {
			iters++;
			// Matrix-vector multiply and min/max ops
			imdp.mvMultUnc(soln, minMax, soln2, unknownStates.iterator(), strat);
			if (genStrat) {
				fmdStrat.setStepChoices(k - iters, strat);
			}
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished bounded probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Bounded probabilistic reachability");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.lastSoln = soln2;
		res.accuracy = AccuracyFactory.boundedNumericalIterations();
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		res.timePre = 0.0;
		if (genStrat) {
			res.strat = fmdStrat;
		}
		return res;
	}
	
	/**
	 * Compute reachability probabilities.
	 * i.e. compute the probability of reaching a state in {@code target}.
	 * @param imdp The IMDP
	 * @param target Target states
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeReachProbs(IMDP<Double> imdp, BitSet target, MinMax minMax) throws PrismException
	{
		return computeReachProbs(imdp, null, target, minMax);
	}

	/**
	 * Compute until probabilities.
	 * i.e. compute the probability of reaching a state in {@code target}.
	 * @param imdp The IMDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeUntilProbs(IMDP<Double> imdp, BitSet remain, BitSet target, MinMax minMax) throws PrismException
	{
		return computeReachProbs(imdp, remain, target, minMax);
	}

	/**
	 * Compute reachability/until probabilities.
	 * i.e. compute the probability of reaching a state in {@code target},
	 * while remaining in those in {@code remain}.
	 * @param imdp The IMDP
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeReachProbs(IMDP<Double> imdp, BitSet remain, BitSet target, MinMax minMax) throws PrismException
	{
		int strat[] = null;
		// Switch to a supported method, if necessary
		IMDPSolnMethod imdpSolnMethod = this.imdpSolnMethod;
		switch (imdpSolnMethod)
		{
		case VALUE_ITERATION:
		case GAUSS_SEIDEL:
			break; // supported
		default:
			imdpSolnMethod = IMDPSolnMethod.GAUSS_SEIDEL;
			mainLog.printWarning("Switching to solution method \"" + imdpSolnMethod.fullName() + "\"");
		}

		// Start probabilistic reachability
		long timer = System.currentTimeMillis();
		mainLog.println("\nStarting probabilistic reachability...");

		// Check for any zero lower probability bounds (not supported
		// since this approach assumes the graph structure remains static)
		imdp.checkLowerBoundsArePositive();
		
		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		imdp.checkForDeadlocks(target);

		// Store num states
		int n = imdp.getNumStates();

		// If required, create/initialise strategy storage
		// Set choices to -1, denoting unknown
		// (except for target states, which are -2, denoting arbitrary)
		if (genStrat) {
			strat = new int[n];
			for (int i = 0; i < n; i++) {
				strat[i] = target.get(i) ? -2 : -1;
			}
		}

		// Precomputation
		BitSet no, yes;
		if (precomp && prob0) {
			no = mcMDP.prob0(imdp, remain, target, minMax.isMin(), strat);
		} else {
			no = new BitSet();
		}
		if (precomp && prob1) {
			yes = mcMDP.prob1(imdp, remain, target, minMax.isMin(), strat);
		} else {
			yes = (BitSet) target.clone();
		}

		// Print results of precomputation
		int numYes = yes.cardinality();
		int numNo = no.cardinality();
		mainLog.println("target=" + target.cardinality() + ", yes=" + numYes + ", no=" + numNo + ", maybe=" + (n - (numYes + numNo)));

		// If still required, store strategy for no/yes (0/1) states.
		// This is just for the cases max=0 and min=1, where arbitrary choices suffice (denoted by -2)
		if (genStrat) {
			if (minMax.isMin()) {
				for (int i = yes.nextSetBit(0); i >= 0; i = yes.nextSetBit(i + 1)) {
					if (!target.get(i))
						strat[i] = -2;
				}
			} else {
				for (int i = no.nextSetBit(0); i >= 0; i = no.nextSetBit(i + 1)) {
					strat[i] = -2;
				}
			}
		}

		// Start value iteration
		timer = System.currentTimeMillis();
		String sMinMax = minMax.isMin() ? "min" : "max";
		sMinMax += minMax.isMinUnc() ? "min" : "max";
		mainLog.println("Starting value iteration (" + sMinMax + ")...");

		// Store num states
		n = imdp.getNumStates();

		// Initialise solution vectors
		double[] init = new double[n];
		for (int i = 0; i < n; i++)
			init[i] = yes.get(i) ? 1.0 : no.get(i) ? 0.0 : 0.0;

		// Determine set of states actually need to compute values for
		BitSet unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(yes);
		unknown.andNot(no);

		// Compute probabilities (if needed)
		ModelCheckerResult res;
		if (numYes + numNo < n) {
			IterationMethod iterationMethod = null;
			switch (imdpSolnMethod) {
			case VALUE_ITERATION:
				iterationMethod = new IterationMethodPower(termCrit == TermCrit.ABSOLUTE, termCritParam);
				break;
			case GAUSS_SEIDEL:
				iterationMethod = new IterationMethodGS(termCrit == TermCrit.ABSOLUTE, termCritParam, false);
				break;
			default:
				throw new PrismException("Unknown solution method " + imdpSolnMethod.fullName());
			}
			IterationMethod.IterationValIter iterationReachProbs = iterationMethod.forMvMultMinMaxUnc(imdp, minMax, strat);
			iterationReachProbs.init(init);
			IntSet unknownStates = IntSet.asIntSet(unknown);
			String description = sMinMax + ", with " + iterationMethod.getDescriptionShort();
			res = iterationMethod.doValueIteration(this, description, iterationReachProbs, unknownStates, timer, null);
		} else {
			res = new ModelCheckerResult();
			res.soln = Utils.bitsetToDoubleArray(yes, n);
			res.accuracy = AccuracyFactory.doublesFromQualitative();
		}
		
		// Store strategy
		if (genStrat) {
			res.strat = new MDStrategyArray<>(imdp, strat);
		}
		
		// Finished probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Probabilistic reachability took " + timer / 1000.0 + " seconds.");

		// Update time taken
		res.timeTaken = timer / 1000.0;

		return res;
	}

	/**
	 * Compute expected reachability rewards.
	 * i.e. compute the min/max reward accumulated to reach a state in {@code target}.
	 * @param imdp The IMDP
	 * @param imdpRewards The rewards
	 * @param target Target states
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeReachRewards(IMDP<Double> imdp, MDPRewards<Double> imdpRewards, BitSet target, MinMax minMax) throws PrismException
	{
		int strat[] = null;
		// Switch to a supported method, if necessary
		IMDPSolnMethod imdpSolnMethod = this.imdpSolnMethod;
		switch (imdpSolnMethod)
		{
		case VALUE_ITERATION:
		case GAUSS_SEIDEL:
			break; // supported
		default:
			imdpSolnMethod = IMDPSolnMethod.GAUSS_SEIDEL;
			mainLog.printWarning("Switching to solution method \"" + imdpSolnMethod.fullName() + "\"");
		}

		// Start probabilistic reachability
		long timer = System.currentTimeMillis();
		mainLog.println("\nStarting expected reachability...");

		// Check for any zero lower probability bounds (not supported
		// since this approach assumes the graph structure remains static)
		imdp.checkLowerBoundsArePositive();
		
		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		imdp.checkForDeadlocks(target);

		// Store num states
		int n = imdp.getNumStates();

		// If required, create/initialise strategy storage
		// Set choices to -1, denoting unknown
		// (except for target states, which are -2, denoting arbitrary)
		if (genStrat) {
			strat = new int[n];
			for (int i = 0; i < n; i++) {
				strat[i] = target.get(i) ? -2 : -1;
			}
		}
		
		// Precomputation (not optional)
		BitSet inf = mcMDP.prob1(imdp, null, target, !minMax.isMin(), strat);
		inf.flip(0, n);

		// Print results of precomputation
		int numTarget = target.cardinality();
		int numInf = inf.cardinality();
		mainLog.println("target=" + numTarget + ", inf=" + numInf + ", rest=" + (n - (numTarget + numInf)));

		// If required, generate strategy for "inf" states.
		if (genStrat) {
			if (minMax.isMin()) {
				// If min reward is infinite, all choices give infinity
				// So the choice can be arbitrary, denoted by -2; 
				for (int i = inf.nextSetBit(0); i >= 0; i = inf.nextSetBit(i + 1)) {
					strat[i] = -2;
				}
			} else {
				// If max reward is infinite, there is at least one choice giving infinity.
				// So we pick, for all "inf" states, the first choice for which some transitions stays in "inf".
				for (int i = inf.nextSetBit(0); i >= 0; i = inf.nextSetBit(i + 1)) {
					int numChoices = imdp.getNumChoices(i);
					for (int k = 0; k < numChoices; k++) {
						if (imdp.someSuccessorsInSet(i, k, inf)) {
							strat[i] = k;
							continue;
						}
					}
				}
			}
		}

		// Start value iteration
		timer = System.currentTimeMillis();
		String sMinMax = minMax.isMin() ? "min" : "max";
		sMinMax += minMax.isMinUnc() ? "min" : "max";
		mainLog.println("Starting value iteration (" + sMinMax + ")...");

		// Store num states
		n = imdp.getNumStates();

		// Initialise solution vectors
		double[] init = new double[n];
		for (int i = 0; i < n; i++)
			init[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : 0.0;

		// Determine set of states actually need to compute values for
		BitSet unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(target);
		unknown.andNot(inf);

		// Compute probabilities (if needed)
		ModelCheckerResult res;
		if (numTarget + numInf < n) {
			IterationMethod iterationMethod = null;
			switch (imdpSolnMethod) {
			case VALUE_ITERATION:
				iterationMethod = new IterationMethodPower(termCrit == TermCrit.ABSOLUTE, termCritParam);
				break;
			case GAUSS_SEIDEL:
				iterationMethod = new IterationMethodGS(termCrit == TermCrit.ABSOLUTE, termCritParam, false);
				break;
			default:
				throw new PrismException("Unknown solution method " + imdpSolnMethod.fullName());
			}
			IterationMethod.IterationValIter iterationReachProbs = iterationMethod.forMvMultRewMinMaxUnc(imdp, imdpRewards, minMax, strat);
			iterationReachProbs.init(init);
			IntSet unknownStates = IntSet.asIntSet(unknown);
			String description = sMinMax + ", with " + iterationMethod.getDescriptionShort();
			res = iterationMethod.doValueIteration(this, description, iterationReachProbs, unknownStates, timer, null);
		} else {
			res = new ModelCheckerResult();
			res.soln = Utils.bitsetToDoubleArray(inf, n, Double.POSITIVE_INFINITY);
			res.accuracy = AccuracyFactory.doublesFromQualitative();
		}
		
		// Store strategy
		if (genStrat) {
			res.strat = new MDStrategyArray<>(imdp, strat);
		}
		
		// Finished probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Probabilistic reachability took " + timer / 1000.0 + " seconds.");

		// Update time taken
		res.timeTaken = timer / 1000.0;

		return res;
	}

	/**
	 * Compute expected reachability rewards.
	 * i.e. compute the min/max reward accumulated to reach a state in {@code target}.
	 * @param imdp The IMDP
	 * @param imdpRewards The rewards
	 * @param bound The bound
	 */
	public ModelCheckerResult computeMultiStrategy(IMDP<Double> imdp, MDPRewards<Double> mdpRewards, double bound) throws PrismException
	{
		boolean value =false;
		boolean min =false;
		boolean robust=true;
		if (value){
			if (min){
				return computeMultiStrategyValueMin(imdp, mdpRewards, bound, robust);
			} else {
				return computeMultiStrategyValueMax(imdp, mdpRewards, bound, robust);
			}
			
		} else {
			if (min){
				return computeMultiStrategyProbMax(imdp, mdpRewards, bound, robust);
			} else {
				return computeMultiStrategyProbMax(imdp, mdpRewards, bound, robust);
			}
			
		}
	}

	/**
	 * Compute permissive strategy that satisfy a specified bound -> Rmin >= bound.
	 * i.e. compute the probability of being in a state in {@code target} in the next step.
	 * @param imdp The IMDP
	 * @param mdpRewards Target states
	 * @param bound limitation bound
	 * @param robust true -> robust or false -> optimistic
	 */

	public ModelCheckerResult computeMultiStrategyValueMin(IMDP<Double> imdp, MDPRewards<Double> mdpRewards, double bound, boolean robust) throws PrismException
	{
		double[] soln = null;
		
		// Start solution
		long timer = System.currentTimeMillis();
		mainLog.println("Starting linear programming (min)...");
		
		int n = imdp.getNumStates();
		System.out.println(n);
		int sInit = imdp.getFirstInitialState();

		double BIG = 10000.0;
		System.out.println(imdp.getLabelStates("goal"));
		BitSet targetS = imdp.getLabelStates("goal");
		System.out.println("Hi: " + imdp.getInitialStates());
		IntSet targetStates = IntSet.asIntSet(targetS);
		PrimitiveIterator.OfInt states = targetStates.iterator();
		int numTarget = 0;
		while(states.hasNext()){
			final int s = states.nextInt();
			numTarget += 1;
		}
		int[] target = new int[numTarget];
		states = targetStates.iterator();
		int counter = 0;
		while(states.hasNext()){
			final int s = states.nextInt();
			target[counter] = s;
			counter += 1;
		}
		if (robust==true){
			mainLog.println("Calcualting multi Strategy under the assumption of worst transition per objective: ");
			try {
				// Initialise MILP solver
				GRBEnv env = new GRBEnv("gurobi.log");
				env.set(GRB.IntParam.OutputFlag, 1);
				GRBModel m = new GRBModel(env);
				Map<Integer,GRBVar> v;
				Map<String,GRBVar> eta, lam;
				Map<String,Map<Integer,GRBVar>> uplus, umin;

				// Value-function
				v = new HashMap<>();
				for (int s = 0; s <n; s++){
					v.put(s, m.addVar(0.0, GRB.INFINITY, 0, GRB.CONTINUOUS, "mu["+s+"]"));
				}
				for (int i = 0; i < target.length; i++){
					m.addConstr(v.get(target[i]),GRB.EQUAL,0.0,"v-target");
				}
				
				// action-variable Binary + Dualization
				eta = new HashMap<>();
				lam = new HashMap<>();
				uplus = new HashMap<>();
				umin = new HashMap<>();
				for (int s = 0; s <n; s++){
					int numChoices = imdp.getNumChoices(s);
					for (int c = 0; c < numChoices; c++){
						String sc = s + "-" + c;
						eta.put(sc, m.addVar(0.0,1.0,0.0,GRB.BINARY,"eta"+"-"+sc));
						lam.put(sc, m.addVar(-BIG,BIG,0,GRB.CONTINUOUS,"lam"+"-"+sc));
						uplus.put(sc, new HashMap<>());
						umin.put(sc, new HashMap<>());
						Iterator<Map.Entry<Integer, Interval<Double>>> iter = imdp.getTransitionsIterator(s, c);
						while(iter.hasNext()){
							Map.Entry<Integer, Interval<Double>> e = iter.next();
							int t = e.getKey();
							Interval<Double> transition_val = e.getValue();
							String key = sc + "->" + t;
							GRBVar up = m.addVar(0,BIG,0,GRB.CONTINUOUS,"uplus["+key+"]");
							GRBVar um = m.addVar(0,BIG,0,GRB.CONTINUOUS,"umin["+key+"]");
							uplus.get(sc).put(t, up);
							umin.get(sc).put(t, um);
							GRBLinExpr allowed = new GRBLinExpr();
							allowed.addTerm(BIG, eta.get(sc));
							m.addConstr(uplus.get(sc).get(t), GRB.GREATER_EQUAL, 0.0,"uPlusLow-"+sc+"-"+t );
							m.addConstr(umin.get(sc).get(t), GRB.GREATER_EQUAL, 0.0,"uMinLow-"+sc+"-"+t );
							m.addConstr(uplus.get(sc).get(t), GRB.LESS_EQUAL, allowed,"uPlusHigh-"+sc+"-"+t);
							m.addConstr(umin.get(sc).get(t), GRB.LESS_EQUAL, allowed,"uMinHigh-"+sc+"-"+t);
							GRBLinExpr dualExpr = new GRBLinExpr();
							dualExpr.addTerm(1.0, lam.get(sc));
							dualExpr.addTerm(1.0, uplus.get(sc).get(t));
							dualExpr.addTerm(-1.0, umin.get(sc).get(t));
							m.addConstr(dualExpr, GRB.GREATER_EQUAL, v.get(t), "dualFeas_"+sc+"_"+t);
						}
					}
				}
				
				// Action per state Constraint (multi >= 1.0, deter == 1.0)
				for (int s = 0; s < n; s++) {
					GRBLinExpr expr = new GRBLinExpr();
					for (int c = 0; c < imdp.getNumChoices(s); c++)
						expr.addTerm(1.0, eta.get(s+"-"+c));
					m.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "actSel-"+s);
				}
				
				//Pre-bell
				Map<String,GRBVar> phi = new HashMap<>();

				for (int s = 0; s < n; s++) {
					for (int c = 0; c < imdp.getNumChoices(s); c++) {
						String sc = s + "-" + c;
						GRBVar etaVar = eta.get(sc), lamVar = lam.get(sc);
						GRBVar phiVar = m.addVar(-BIG, BIG, 0.0, GRB.CONTINUOUS, "phi-" + sc);
						phi.put(sc, phiVar);
						// 1) φ ≤ λMore actions
						m.addConstr(phiVar, GRB.LESS_EQUAL, lamVar, "philem1-" + sc);

						// 2) φ ≤ BIG * η  → build RHS as a LinExpr
						GRBLinExpr rhs2 = new GRBLinExpr();
						rhs2.addTerm(BIG, etaVar);
						m.addConstr(phiVar, GRB.LESS_EQUAL, rhs2, "philem2-" + sc);
						// 3) φ ≥ λ − BIG*(1−η)
						GRBLinExpr c3 = new GRBLinExpr();
						c3.addTerm(1.0,  phiVar);
						c3.addTerm(-1.0, lamVar);
						c3.addConstant(BIG);
						c3.addTerm(-BIG, etaVar);
						m.addConstr(c3, GRB.GREATER_EQUAL, 0.0, "philem3-" + sc);
					}
				}
				
				
				
				m.update();


				// Bellman constraint
				for (int s = 0; s < n; s++) {
					int numChoices = imdp.getNumChoices(s);
					for (int c = 0; c < numChoices; c++) {
						String sc = s + "-" + c;
						GRBLinExpr expr = new GRBLinExpr();
						expr.addConstant(mdpRewards.getTransitionReward(s,c));

						// add phi = λ·η
						expr.addTerm(1.0, phi.get(sc));

						// add ∑[hi·uplus – lo·umin]
						GRBLinExpr ins = new GRBLinExpr();
						Iterator<Map.Entry<Integer,Interval<Double>>> it = imdp.getTransitionsIterator(s,c);
						while (it.hasNext()) {
							Map.Entry<Integer,Interval<Double>> e = it.next();
							int t = e.getKey();
							double lo = e.getValue().getLower(), hi = e.getValue().getUpper();
							ins.addTerm( hi, uplus.get(sc).get(t) );
							ins.addTerm(-lo,  umin .get(sc).get(t) );
						}
						expr.add(ins);


						expr.addConstant(-BIG);
						expr.addTerm( BIG, eta.get(sc) );


						// post v[s] ≥ expr

						m.addConstr( v.get(s), GRB.GREATER_EQUAL, expr, "bell-"+sc );
						
					}
				}

				// Bound for v[0];

				m.addConstr(v.get(sInit), GRB.LESS_EQUAL, bound, "multi-bound");
				m.update();

				// Objective
				GRBLinExpr pen = new GRBLinExpr();
				for (int s = 0; s < n; s++){
					int numChoices = imdp.getNumChoices(s);
					for (int c = 0; c < numChoices; c++){
						String sc = s +"-"+c;
						pen.addConstant(1.0);
						pen.addTerm(-1.0, eta.get(sc));
					}
				} 
				// set the model’s objective to pen, minimize it
				
				int numVars   = m.get(GRB.IntAttr.NumVars);
				int numConstr = m.get(GRB.IntAttr.NumConstrs);
				int numIntVars  = m.get(GRB.IntAttr.NumIntVars);   // counts both BINARY and INTEGER types
				int numContVars = numVars - numIntVars;
				mainLog.println("Continuous Variables = " + numContVars + ", Integer Variables = "+ numIntVars + ", Constraints = " + numConstr);
				System.out.println("Continuous Variables = " + numContVars + ", Integer Variables = "+ numIntVars + ", Constraints = " + numConstr);
				// now optimize
				m.setObjective(pen, GRB.MINIMIZE);
				m.setCallback(new trigger(m, 3600));
				m.write("gurobi.lp");
				long timerGurobi = System.currentTimeMillis();
				m.optimize();
				long afterGurobi = System.currentTimeMillis();
				long timeTake = afterGurobi - timerGurobi;
				mainLog.println("Gurobi Solve time: " + timeTake);

				
				if (m.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
					System.out.println("Model infeasible—computing IIS...");
					m.computeIIS();
					for (GRBConstr c : m.getConstrs()) {
						if (c.get(GRB.IntAttr.IISConstr) == 1) {
							System.out.println("IIS: " + c.get(GRB.StringAttr.ConstrName));
						}
					}
					// Optionally write the IIS to a file:
					m.write("model.IIS");
				}
				System.out.printf("v[0]: %.4f%n", v.get(sInit).get(GRB.DoubleAttr.X));

				soln = new double[n];
				for (int s = 0; s < n; s++) {
					if (s==0){
						System.out.println(v.get(s).get(GRB.StringAttr.VarName) + " = "+v.get(s).get(GRB.DoubleAttr.X));
					}
					
					
					soln[s] = v.get(s).get(GRB.DoubleAttr.X);
				}

				for (int s = 0; s < n; s++) {
					mainLog.println("state: " + s + ", value: " + soln[s]);
				}

				int isMIP   = m.get(GRB.IntAttr.IsMIP);    // 1 => has integer vars
				int isQP    = m.get(GRB.IntAttr.IsQP);     // 1 => quadratic objective
				int isQCP   = m.get(GRB.IntAttr.IsQCP);    // 1 => quadratic constraints
				int numQC   = m.get(GRB.IntAttr.NumQConstrs); // # quadratic constraints
				System.out.printf("IsMIP=%d IsQP=%d IsQCP=%d NumQConstrs=%d%n",isMIP, isQP, isQCP, numQC);


				mainLog.println("Multi Strategy to Obtain at most under worst assumption of adversary " + bound);

				// How many solutions are in the pool?
				int solCount = m.get(GRB.IntAttr.SolCount);

				// Loop over each solution k=0…solCount−1

				for (int s = 0; s < n; s++) {
					mainLog.print(s + ":");
					int numChoices = imdp.getNumChoices(s);
					for (int i = 0; i < numChoices; i++) {
						String sc = s +"-"+i;
						if (eta.get(sc).get(GRB.DoubleAttr.X) > 0) {
							
							mainLog.print(" " + imdp.getAction(s, i));
						}
					}
					mainLog.println();
				}
				int permC = 0;
				for (int s = 0; s < n; s++) {
					//System.out.println(v.get(s).get(GRB.StringAttr.VarName) + " = "+v.get(s).get(GRB.DoubleAttr.X));
					int numChoices = imdp.getNumChoices(s);
					
					if (numChoices == 1){
						continue;
					} else {
						int tmC = 0;
						for (int i = 0; i < numChoices; i++) {
							String sc = s +"-"+i;
							if (eta.get(sc).get(GRB.DoubleAttr.X) > 0) {
								tmC += 1;
							}
						}
						if (tmC > 1){
							permC += 1;
						}
					}
				}
				mainLog.println("Number of Permissive States: " + permC);

				int reachable = 0;
				for (int s = 0; s < n; s++) {
					if (soln[s] > 0.0){
						if (soln[s] <= bound){
							int numChoices = imdp.getNumChoices(s);
							if (numChoices == 1){
								continue;
							} else {
								for (int i = 0; i < numChoices; i++) {
									reachable += 1;
								}
							}
						}
						
						
					}
				}

				mainLog.println("Total number of reachable permissable state-action pairs: " + reachable);
				

				reachable = 0;
				for (int s = 0; s < n; s++) {
					if (soln[s] >= 0.0){
						if (soln[s] <= bound){
							int numChoices = imdp.getNumChoices(s);
						
							for (int i = 0; i < numChoices; i++) {
								reachable += 1;
							}
						}
						
						
						
					}
				}

				mainLog.println("Total number of reachable state-action pairs: " + reachable);
				// Clean up
				m.dispose();
				env.dispose();
			} catch (GRBException e) {
				throw new PrismException("Error solving LP: " +e.getMessage());
			}
	} else {

		// Multi-strategy under best assumption
		mainLog.println("Calcualting multi Strategy under the assumption of best transition per objective: ");
		double tol = 1e-9;

		try {
				 // create environment & model
			 	System.out.println("Im here");
				GRBEnv env = new GRBEnv("gurobi.log");
				env.set(GRB.IntParam.OutputFlag, 0);
				env.set(GRB.DoubleParam.TimeLimit, 3600.0);   // 60-second limit 
				env.set(GRB.IntParam.Threads,   0);        // (optional) use all cores
				env.set(GRB.IntParam.OutputFlag, 1);
				env.set(GRB.IntParam.Cuts, 2);
				GRBModel m = new GRBModel(env);
				

				// --- 1) Value‐function vars v[s] ---
				Map<Integer,GRBVar> v = new HashMap<>();
				for (int s = 0; s <n; s++){
					v.put(s, m.addVar(0.0, GRB.INFINITY, 0, GRB.CONTINUOUS, "mu["+s+"]"));
				}
				for (int i = 0; i < target.length; i++){
					m.addConstr(v.get(target[i]),GRB.EQUAL,0.0,"v-target");
				}
				m.update();

				// --- 2) Binary η and dual vars lam (α), uplus (β), umin (γ) ---
				Map<String,GRBVar> eta = new HashMap<>();
				Map<String,Map<Integer,GRBVar>> delta = new HashMap<>();

				
				for (int s = 0; s <n; s ++){
					int numC = imdp.getNumChoices(s);
					for (int c = 0; c < numC; c++) {
						String sc = s + "-" + c;
						eta.put(sc, m.addVar(0.0,1.0,0.0,GRB.BINARY,"eta"+"-"+sc));
						// collect successors + bounds, and create delta vars
						List<Integer> tList = new ArrayList<>();
						List<Double> loList = new ArrayList<>(), hiList = new ArrayList<>();
						Iterator<Map.Entry<Integer, Interval<Double>>> it = imdp.getTransitionsIterator(s, c);
						List<Interval<Double>> li = new ArrayList<>();
						while (it.hasNext()) {
							Map.Entry<Integer, Interval<Double>> e = it.next();
							int t = e.getKey();
							Interval<Double> w = e.getValue();
							li.add(w);
							tList.add(t);
						}

						System.out.println("tList: " + tList.toString());
						for (int i = 0; i < li.size(); i++) {
							Interval<Double> w = li.get(i);
							double lo = Math.min(w.getLower(), w.getUpper()); // normalize just in case
							double hi = Math.max(w.getLower(), w.getUpper());
							loList.add(lo);
							hiList.add(hi);
						}


						

						// Enumerate all vertices via "n-1 at bounds + 1 free"


						List<Map<Integer,Double>> result = new ArrayList<>();
						Set<String> seen = new HashSet<>();
						for (int j = 0; j < tList.size(); j++) {
							int freeState = tList.get(j);
							List<Map<Integer,Double>> verts =
								findExtremeCombos(tList, loList, hiList, tol, freeState); // <-- new overload
							for (Map<Integer,Double> combo : verts) {
								String key = comboKey(combo, tList);
								if (seen.add(key)) result.add(combo);  // keep only novel vertices
							}
						}

						mainLog.println("State: " + s + ", Action: " + c);

						int count = 0;
						for (Map<Integer,Double> combo : result) {
							StringBuilder sb = new StringBuilder();
							GRBLinExpr expr = new GRBLinExpr();
							expr.addConstant(mdpRewards.getTransitionReward(s,c));
							for (int i = 0; i < tList.size(); i++) {
								int t = tList.get(i);
								double p = combo.get(t);
								if (i > 0) sb.append(", ");
								sb.append("state ").append(t).append(": ")
								.append(String.format(Locale.US, "%.9f", p));
								expr.addTerm(p, v.get(t));
							}
							// indicator-style big-M for the chosen (s,c)
							expr.addConstant(-BIG);
							expr.addTerm(+BIG, eta.get(sc));
							m.addConstr(v.get(s), GRB.GREATER_EQUAL, expr,
										"state-"+s+"-choice-"+c+"-combination-"+count);

							mainLog.println(sb.toString());
							count++;
						}
					}
				}

				// --- 3) Action‐selection: sum η ≥ 1 at each state ---
				for (int s = 0; s < n; s++) {
					GRBLinExpr expr = new GRBLinExpr();
					for (int c = 0; c < imdp.getNumChoices(s); c++)
						expr.addTerm(1.0, eta.get(s+"-"+c));
					m.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "actSel-"+s);
				}


				// 3g) Bellman 
				m.addConstr(v.get(sInit), GRB.LESS_EQUAL, bound, "multi-bound");
				m.update();



				// 5) objective:  minimize sum(1−η)  ≡  minimize (   #actions  − sum η  )
				GRBLinExpr pen = new GRBLinExpr();

				for (int s = 0; s < n; s++){
					int numChoices = imdp.getNumChoices(s);
					for (int c = 0; c < numChoices; c++){
						String sc = s +"-"+c;
						pen.addConstant(1.0);
						pen.addTerm(-1.0, eta.get(sc));
					}
				}

				m.setObjective(pen, GRB.MINIMIZE);
				
				// set the model’s objective to pen, minimize it

				/* GRBLinExpr pen = new GRBLinExpr();
				pen.addTerm(1.0, v.get(sInit));
				m.setObjective(pen, GRB.MINIMIZE); */
				int numVars   = m.get(GRB.IntAttr.NumVars);
				int numConstr = m.get(GRB.IntAttr.NumConstrs);
				int numIntVars  = m.get(GRB.IntAttr.NumIntVars);   // counts both BINARY and INTEGER types
				int numContVars = numVars - numIntVars;
				mainLog.println("Continuous Variables = " + numContVars + ", Integer Variables = "+ numIntVars + ", Constraints = " + numConstr);
				System.out.println("Continuous Variables = " + numContVars + ", Integer Variables = "+ numIntVars + ", Constraints = " + numConstr);

				m.setCallback(new trigger(m, 3600.0));
				long timerGurobi = System.currentTimeMillis();
				m.optimize();
				long afterGurobi = System.currentTimeMillis();
				long timeTake = afterGurobi - timerGurobi;
				mainLog.println("Gurobi Solve time: " + timeTake);

				//System.out.printf("v[0]: %.4f%n", v.get(0).get(GRB.DoubleAttr.X));
				if (m.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
					System.out.println("Model infeasible—computing IIS...");
					m.computeIIS();
					for (GRBConstr c : m.getConstrs()) {
						if (c.get(GRB.IntAttr.IISConstr) == 1) {
							System.out.println("IIS: " + c.get(GRB.StringAttr.ConstrName));
						}
					}
					// Optionally write the IIS to a file:
					m.write("model.IIS");
				}
				int status = m.get(GRB.IntAttr.Status);
				System.out.println("Gurobi Status = " + status);

				soln = new double[n];
				for (int s = 0; s < n; s++) {
					soln[s] = v.get(s).get(GRB.DoubleAttr.X);
					mainLog.println("state: " + s + ", value: " + soln[s]);
				}

				mainLog.println("Multi Strategy to Obtain at least under worst assumption of adversary" + bound);
				

				// How many solutions are in the pool?
				int solCount = m.get(GRB.IntAttr.SolCount);

				// Loop over each solution k=0…solCount−1

				for (int s = 0; s < n; s++) {
					mainLog.print(s + ":");
					int numChoices = imdp.getNumChoices(s);
					for (int i = 0; i < numChoices; i++) {
						String sc = s +"-"+i;
						if (eta.get(sc).get(GRB.DoubleAttr.X) > 0) {
							mainLog.print(" " + imdp.getAction(s, i));
						}
					}
					mainLog.println();
				}
				int permC = 0;
				for (int s = 0; s < n; s++) {
					//System.out.println(v.get(s).get(GRB.StringAttr.VarName) + " = "+v.get(s).get(GRB.DoubleAttr.X));
					int numChoices = imdp.getNumChoices(s);
					
					if (numChoices == 1){
						continue;
					} else {
						int tmC = 0;
						for (int i = 0; i < numChoices; i++) {
							String sc = s +"-"+i;
							if (eta.get(sc).get(GRB.DoubleAttr.X) > 0) {
								tmC += 1;
							}
						}
						if (tmC > 1){
							permC += 1;
						}
					}
				}
				mainLog.println("Number of Permissive States: " + permC);
				mainLog.println("Value at inital state " + v.get(sInit).get(GRB.DoubleAttr.X));

				mainLog.println("Number of Permissive States: " + permC);

				int reachable = 0;
				for (int s = 0; s < n; s++) {
					if (soln[s] > 0.0){
						if (soln[s] < BIG){
							int numChoices = imdp.getNumChoices(s);
							if (numChoices == 1){
								continue;
							} else {
								for (int i = 0; i < numChoices; i++) {
									reachable += 1;
								}
							}
						}
						
						
					}
				}

				mainLog.println("Total number of reachable permissable state-action pairs: " + reachable);
				

				reachable = 0;
				for (int s = 0; s < n; s++) {
					if (soln[s] > 0.0){
						if (soln[s] <= BIG){
							int numChoices = imdp.getNumChoices(s);
						
							for (int i = 0; i < numChoices; i++) {
								reachable += 1;
							}
						}
						
						
						
					}
				}

				mainLog.println("Total number of reachable state-action pairs: " + reachable);
				
				m.dispose();
				env.dispose();
			} catch (GRBException e) {
				throw new PrismException("Error solving LP: " +e.getMessage());
			}
		
	}
		long time = (System.currentTimeMillis() - timer);
		mainLog.println("It took "+time + " milli-seconds to finish synthesize the permissive strategy");

		// Return results
		ModelCheckerResult res = new ModelCheckerResult();
//		res.accuracy = AccuracyFactory.boundedNumericalIterations();
		res.soln = soln;
//		res.timeTaken = timer / 1000.0;
		return res;
	}

	
	/**
	 * Compute permissive strategy that satisfy a specified bound -> Rmax <= bound.
	 * i.e. compute the probability of being in a state in {@code target} in the next step.
	 * @param imdp The IMDP
	 * @param mdpRewards Target states
	 * @param bound limitation bound
	 * @param robust true -> robust or false -> optimistic
	 */
	public ModelCheckerResult computeMultiStrategyValueMax(IMDP<Double> imdp, MDPRewards<Double> mdpRewards, double bound, boolean robust) throws PrismException
	{
		double[] soln = null;
		
		// Start solution
		long timer = System.currentTimeMillis();
		mainLog.println("Starting linear programming (min)...");
		
		int n = imdp.getNumStates();
		System.out.println(n);
		int sInit = imdp.getFirstInitialState();
		//double BIG = 1000.00;
		double BIG =100.000;
		System.out.println(imdp.getLabelStates("goal"));
		BitSet targetS = imdp.getLabelStates("goal");
		System.out.println(imdp.getInitialStates());
		IntSet targetStates = IntSet.asIntSet(targetS);
		PrimitiveIterator.OfInt states = targetStates.iterator();
		int numTarget = 0;
		while(states.hasNext()){
			final int s = states.nextInt();
			numTarget += 1;
		}
		int[] target = new int[numTarget];
		states = targetStates.iterator();
		int counter = 0;
		while(states.hasNext()){
			final int s = states.nextInt();
			target[counter] = s;
			counter += 1;
		}
		System.out.println(target[0]);
		
		

		if (robust==true){
			mainLog.println("Calcualting multi Strategy under the assumption of worst transition per objective: ");
			try {
				// Initialise MILP solver
				GRBEnv env = new GRBEnv("gurobi.log");
				//env.set(GRB.DoubleParam.TimeLimit, 3600.0);   // 60-second limit 
				//env.set(GRB.IntParam.Threads,   0);        // (optional) use all cores
				//env.set(GRB.IntParam.OutputFlag, 1);
				/* env.set(GRB.IntParam.Cuts, 2);
				
				env.set(GRB.DoubleParam.MIPGap,1e-2); */
				//env.set(GRB.IntParam.MIPFocus, 2);
				env.set(GRB.IntParam.Cuts, 2);
				env.set(GRB.DoubleParam.MIPGap,1e-1);
				GRBModel m = new GRBModel(env);
				Map<Integer,GRBVar> v;
				Map<String,GRBVar> eta, lam;
				Map<String,Map<Integer,GRBVar>> uplus, umin;

				// Value-function
				v = new HashMap<>();
				for (int s = 0; s <n; s++){
					v.put(s, m.addVar(0.0, BIG, 0, GRB.CONTINUOUS, "mu["+s+"]"));
				}
				for (int i = 0; i < target.length; i++){
					m.addConstr(v.get(target[i]),GRB.EQUAL,0.0,"v-target");
				}
				
				// action-variable Binary + Dualization
				eta = new HashMap<>();
				lam = new HashMap<>();
				uplus = new HashMap<>();
				umin = new HashMap<>();
				for (int s = 0; s <n; s++){
					int numChoices = imdp.getNumChoices(s);
					for (int c = 0; c < numChoices; c++){
						String sc = s + "-" + c;
						eta.put(sc, m.addVar(0.0,1.0,0.0,GRB.BINARY,"eta"+"-"+sc));
						lam.put(sc, m.addVar(-BIG,BIG,0,GRB.CONTINUOUS,"lam"+"-"+sc));
						/* GRBLinExpr lamAU = new GRBLinExpr();
						lamAU.addTerm(BIG, eta.get(sc));
						GRBLinExpr lamAD = new GRBLinExpr();
						lamAD.addTerm(-BIG, eta.get(sc));
						m.addConstr(lam.get(sc), GRB.LESS_EQUAL, lamAU, "lamU["+sc+"]");
						m.addConstr(lam.get(sc), GRB.GREATER_EQUAL, lamAD, "lamD["+sc+"]"); */
						uplus.put(sc, new HashMap<>());
						umin.put(sc, new HashMap<>());
						Iterator<Map.Entry<Integer, Interval<Double>>> iter = imdp.getTransitionsIterator(s, c);
						while(iter.hasNext()){
							Map.Entry<Integer, Interval<Double>> e = iter.next();
							int t = e.getKey();
							Interval<Double> transition_val = e.getValue();
							String key = sc + "->" + t;
							GRBVar up = m.addVar(0,BIG,0,GRB.CONTINUOUS,"uplus["+key+"]");
							GRBVar um = m.addVar(0,BIG,0,GRB.CONTINUOUS,"umin["+key+"]");
							uplus.get(sc).put(t, up);
							umin.get(sc).put(t, um);
							/* GRBLinExpr allowed = new GRBLinExpr();
							allowed.addTerm(BIG, eta.get(sc));*/
							m.addConstr(uplus.get(sc).get(t), GRB.GREATER_EQUAL, 0.0,"uPlusLow-"+sc+"-"+t );
							m.addConstr(umin.get(sc).get(t), GRB.GREATER_EQUAL, 0.0,"uMinLow-"+sc+"-"+t );
							// if ηₛₐ == 1 then uplusₛₐₜ == 0
							GRBLinExpr allowed = new GRBLinExpr();
							allowed.addConstant(BIG);
							allowed.addTerm(-BIG, eta.get(sc));
							m.addConstr(uplus.get(sc).get(t), GRB.LESS_EQUAL, allowed,"uPlusHigh-"+sc+"-"+t);
							m.addConstr(umin.get(sc).get(t), GRB.LESS_EQUAL, allowed,"uMinHigh-"+sc+"-"+t);
							GRBLinExpr dualExpr = new GRBLinExpr();
							dualExpr.addTerm(1.0, lam.get(sc));
							dualExpr.addTerm(-1.0, uplus.get(sc).get(t));
							dualExpr.addTerm(+1.0, umin.get(sc).get(t));
							m.addConstr(dualExpr, GRB.LESS_EQUAL, v.get(t), "dualFeas_"+sc+"_"+t);
						}
					}
				}
				
				// Action per state Constraint (multi >= 1.0, deter == 1.0)
				for (int s = 0; s < n; s++) {
					GRBLinExpr expr = new GRBLinExpr();
					for (int c = 0; c < imdp.getNumChoices(s); c++)
						expr.addTerm(1.0, eta.get(s+"-"+c));
					m.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "actSel-"+s);
				}
				
				
				//Pre-bell
				Map<String,GRBVar> phi = new HashMap<>();
				for (int s = 0; s < n; s++) {
					for (int c = 0; c < imdp.getNumChoices(s); c++) {
						String sc = s + "-" + c;
						GRBVar etaVar = eta.get(sc), lamVar = lam.get(sc);
						GRBVar phiVar = m.addVar(-BIG, BIG, 0.0, GRB.CONTINUOUS, "phi-" + sc);
						phi.put(sc, phiVar);

						// 3) φ <= λ + BIG*(1−η)
						GRBLinExpr c3 = new GRBLinExpr();
						c3.addTerm(1.0,  phiVar);
						c3.addTerm(-1.0, lamVar);
						c3.addConstant(-BIG);
						c3.addTerm(+BIG, etaVar);
						m.addConstr(c3, GRB.LESS_EQUAL, 0.0, "philem3-" + sc);
					}
				}
				
				
				m.update();


				// Bellman constraint
				for (int s = 0; s < n; s++) {
					int numChoices = imdp.getNumChoices(s);
					for (int c = 0; c < numChoices; c++) {
						String sc = s + "-" + c;
						GRBLinExpr expr = new GRBLinExpr();
						expr.addConstant(mdpRewards.getTransitionReward(s,c));

						// add phi = λ·η
						expr.addTerm(1.0, phi.get(sc));

						// add ∑[hi·uplus – lo·umin]
						GRBLinExpr ins = new GRBLinExpr();
						Iterator<Map.Entry<Integer,Interval<Double>>> it = imdp.getTransitionsIterator(s,c);
						while (it.hasNext()) {
							Map.Entry<Integer,Interval<Double>> e = it.next();
							int t = e.getKey();
							double lo = e.getValue().getLower(), hi = e.getValue().getUpper();
							ins.addTerm(-hi, uplus.get(sc).get(t) );
							ins.addTerm(+lo,  umin .get(sc).get(t) );
						}
						expr.add(ins);
						expr.addConstant(+BIG);
						expr.addTerm(-BIG, eta.get(sc) );


						// post v[s] ≥ expr
						m.addConstr( v.get(s), GRB.LESS_EQUAL, expr, "bell-"+sc );
						
						
					}
				}

				// Bound for v[0];

				m.addConstr(v.get(sInit), GRB.GREATER_EQUAL, bound, "multi-bound");
				
				m.update();
				double eps = 0.01;
				GRBLinExpr tieB = new GRBLinExpr();
				for (int s = 0; s <n; s++){
					int numChoices = imdp.getNumChoices(s);
					tieB.addTerm(eps,v.get(s));
				}
				// Objective
				GRBLinExpr pen = new GRBLinExpr();
				for (int s = 0; s < n; s++){
					int numChoices = imdp.getNumChoices(s);
					for (int c = 0; c < numChoices; c++){
						String sc = s +"-"+c;
						pen.addConstant(1.0);
						pen.addTerm(-1.0, eta.get(sc));
					}
				}

				GRBLinExpr obj = new GRBLinExpr();
				obj.add(pen);
				obj.add(tieB);
				System.out.println("Hello");
				m.setObjective(pen, GRB.MINIMIZE);

				
				// set the model’s objective to pen, minimize it
				/* GRBLinExpr pen = new GRBLinExpr();
				pen.addTerm(1.0, v.get(sInit));
				m.setObjective(pen, GRB.MAXIMIZE); */
				int numVars   = m.get(GRB.IntAttr.NumVars);
				int numConstr = m.get(GRB.IntAttr.NumConstrs);
				int numIntVars  = m.get(GRB.IntAttr.NumIntVars);   // counts both BINARY and INTEGER types
				int numContVars = numVars - numIntVars;
				mainLog.println("Continuous Variables = " + numContVars + ", Integer Variables = "+ numIntVars + ", Constraints = " + numConstr);
				System.out.println("Continuous Variables = " + numContVars + ", Integer Variables = "+ numIntVars + ", Constraints = " + numConstr);

				m.setCallback(new trigger(m, 3600));
				// now optimize
				m.optimize();
				if (m.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
					System.out.println("Model infeasible—computing IIS...");
					m.computeIIS();
					for (GRBConstr c : m.getConstrs()) {
						if (c.get(GRB.IntAttr.IISConstr) == 1) {
							System.out.println("IIS: " + c.get(GRB.StringAttr.ConstrName));
						}
					}
					// Optionally write the IIS to a file:
					m.write("model.IIS");
				}
				System.out.printf("v[0]: %.4f%n", v.get(sInit).get(GRB.DoubleAttr.X));

				soln = new double[n];
				for (int s = 0; s < n; s++) {
					System.out.println(v.get(s).get(GRB.StringAttr.VarName) + " = "+v.get(s).get(GRB.DoubleAttr.X));
					soln[s] = v.get(s).get(GRB.DoubleAttr.X);
				}

				mainLog.println("Multi Strategy to Obtain at least under worst assumption of adversary" + bound);
				

				// How many solutions are in the pool?
				int solCount = m.get(GRB.IntAttr.SolCount);

				// Loop over each solution k=0…solCount−1

				for (int s = 0; s < n; s++) {
					mainLog.print(s + ":");
					int numChoices = imdp.getNumChoices(s);
					for (int i = 0; i < numChoices; i++) {
						String sc = s +"-"+i;
						if (eta.get(sc).get(GRB.DoubleAttr.X) > 0) {
							mainLog.print(" " + imdp.getAction(s, i));
						}
					}
					mainLog.println();
				}

				// Clean up
				m.dispose();
				env.dispose();
			} catch (GRBException e) {
				throw new PrismException("Error solving LP: " +e.getMessage());
			}
	} else {

		System.out.println("Hi");
		// Multi-strategy under best assumption
		mainLog.println("Calcualting multi Strategy under the assumption of best transition per objective: ");
		
			try {
				 // create environment & model
				GRBEnv env = new GRBEnv("gurobi.log");
				//env.set(GRB.DoubleParam.TimeLimit, 3600.0);   // 60-second limit 
				env.set(GRB.IntParam.Threads,   0);        // (optional) use all cores
				env.set(GRB.IntParam.OutputFlag, 1);
				/* env.set(GRB.IntParam.Cuts, 2);
				env.set(GRB.DoubleParam.MIPGap,1e-2); */
				GRBModel m = new GRBModel(env);

				// --- 1) Value‐function vars v[s] ---
				Map<Integer,GRBVar> v = new HashMap<>();
				for (int s = 0; s < n; s++) {
					v.put(s, m.addVar(0.0,BIG, 0.0, GRB.CONTINUOUS, "mu["+s+"]"));
				}
				for (int i = 0; i < numTarget; i++) {
					m.addConstr(v.get(target[i]), GRB.EQUAL, 0.0, "v-target-"+target[i]);
				}
				m.update();

				// --- 2) Binary η and dual vars lam (α), uplus (β), umin (γ) ---
				Map<String,GRBVar> eta = new HashMap<>();
				Map<String,Map<Integer,GRBVar>> delta = new HashMap<>();

				for (int s = 0; s < n; s++) {
					int numC = imdp.getNumChoices(s);
					for (int c = 0; c < numC; c++) {
						String sc = s + "-" + c;
						eta.put(sc, m.addVar(0.0, 1.0, 0.0, GRB.BINARY, "eta-"+sc));
						delta.put(sc, new HashMap<>());
						Iterator<Map.Entry<Integer,Interval<Double>>> it = imdp.getTransitionsIterator(s,c);
						while (it.hasNext()) {
							Map.Entry<Integer,Interval<Double>> e = it.next();
							int t = e.getKey();
							Interval<Double> transition_val = e.getValue();
							String key = sc+"->"+t;
							GRBVar up = m.addVar(0.0, 1.0, 0.0,GRB.CONTINUOUS, "delta-"+key);
							delta.get(sc).put(t, up);
							GRBLinExpr allowedUp = new GRBLinExpr();
							GRBLinExpr allowedLow = new GRBLinExpr();
							allowedUp.addTerm(transition_val.getUpper(), eta.get(sc));
							allowedLow.addTerm(transition_val.getLower(), eta.get(sc));
							m.addConstr(delta.get(sc).get(t), GRB.LESS_EQUAL, allowedUp, "upperBound");
							m.addConstr(delta.get(sc).get(t), GRB.GREATER_EQUAL, allowedLow, "LowerBound");
						}
						GRBLinExpr norm = new GRBLinExpr();
						for (int t : delta.get(sc).keySet())
							norm.addTerm(1.0, delta.get(sc).get(t));
						m.addConstr(norm, GRB.EQUAL, eta.get(sc), "norm-"+sc);
					}
				}

				// --- 3) Action‐selection: sum η ≥ 1 at each state ---
				for (int s = 0; s < n; s++) {
					GRBLinExpr expr = new GRBLinExpr();
					for (int c = 0; c < imdp.getNumChoices(s); c++)
						expr.addTerm(1.0, eta.get(s+"-"+c));
					m.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "actSel-"+s);
				}


				// 3g) Bellman 
				for (int s = 0; s < n; s++) {
					int numChoices = imdp.getNumChoices(s);
					for (int c = 0; c < numChoices; c++) {
						String sc = s + "-" + c;
						GRBQuadExpr rhs = new GRBQuadExpr();
						rhs.addConstant(mdpRewards.getTransitionReward(s,c));
						
						Iterator<Map.Entry<Integer,Interval<Double>>> it = imdp.getTransitionsIterator(s,c);
						GRBQuadExpr qexpr = new GRBQuadExpr();
						while (it.hasNext()) {
							Map.Entry<Integer,Interval<Double>> e = it.next();
							int t = e.getKey();
							qexpr.addTerm(1.0,delta.get(sc).get(t),v.get(t));
						}

						rhs.add(qexpr);

						// add big‑M disable term
						rhs.addConstant(+BIG);
						rhs.addTerm(-BIG, eta.get(sc) );
						

						// post v[s] ≥ expr

						m.addQConstr(v.get(s), GRB.LESS_EQUAL, rhs, "bell-"+sc );
						
						
					}
				}

				m.addConstr(v.get(sInit), GRB.GREATER_EQUAL, bound, "prefBound");
				
				
				m.update();

				// 5) objective:  minimize sum(1−η)  ≡  minimize (   #actions  − sum η  )
				GRBLinExpr pen = new GRBLinExpr();

				for (int s = 0; s < n; s++){
					int numChoices = imdp.getNumChoices(s);
					for (int c = 0; c < numChoices; c++){
						String sc = s +"-"+c;
						pen.addConstant(1.0);
						pen.addTerm(-1.0, eta.get(sc));
					}
				}
				
				// set the model’s objective to pen, minimize it

				m.setObjective(pen, GRB.MINIMIZE);
				/* GRBLinExpr pen = new GRBLinExpr();
				pen.addTerm(1.0, v.get(sInit));
				m.setObjective(pen, GRB.MAXIMIZE); */
				int numVars   = m.get(GRB.IntAttr.NumVars);
				int numConstr = m.get(GRB.IntAttr.NumConstrs);
				int numIntVars  = m.get(GRB.IntAttr.NumIntVars);   // counts both BINARY and INTEGER types
				int numContVars = numVars - numIntVars;
				mainLog.println("Continuous Variables = " + numContVars + ", Integer Variables = "+ numIntVars + ", Constraints = " + numConstr);
				m.setCallback(new trigger(m, 300.0));
				
				m.optimize();
				//System.out.printf("v[0]: %.4f%n", v.get(0).get(GRB.DoubleAttr.X));
				if (m.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
					System.out.println("Model infeasible—computing IIS...");
					m.computeIIS();
					for (GRBConstr c : m.getConstrs()) {
						if (c.get(GRB.IntAttr.IISConstr) == 1) {
							System.out.println("IIS: " + c.get(GRB.StringAttr.ConstrName));
						}
					}
					// Optionally write the IIS to a file:
					m.write("model.IIS");
				}
				int status = m.get(GRB.IntAttr.Status);
				System.out.println("Gurobi Status = " + status);

				soln = new double[n];
				for (int s = 0; s < n; s++) {
					System.out.println(v.get(s).get(GRB.StringAttr.VarName) + " = "+v.get(s).get(GRB.DoubleAttr.X));
					soln[s] = v.get(s).get(GRB.DoubleAttr.X);
				}

				mainLog.println("Multi Strategy to Obtain at least under worst assumption of adversary" + bound);
				

				// How many solutions are in the pool?
				int solCount = m.get(GRB.IntAttr.SolCount);

				// Loop over each solution k=0…solCount−1

				for (int s = 0; s < n; s++) {
					mainLog.print(s + ":");
					int numChoices = imdp.getNumChoices(s);
					for (int i = 0; i < numChoices; i++) {
						String sc = s +"-"+i;
						if (eta.get(sc).get(GRB.DoubleAttr.X) > 0) {
							mainLog.print(" " + imdp.getAction(s, i));
						}
					}
					mainLog.println();
				}
				m.dispose();
				env.dispose();
			} catch (GRBException e) {
				throw new PrismException("Error solving LP: " +e.getMessage());
			}
	}
		long time = (System.currentTimeMillis() - timer);
		mainLog.println("It took "+time + " milli-seconds to finish synthesize the permissive strategy");

		// Return results
		ModelCheckerResult res = new ModelCheckerResult();
//		res.accuracy = AccuracyFactory.boundedNumericalIterations();
		res.soln = soln;
//		res.timeTaken = timer / 1000.0;
		return res;
	}

	public ModelCheckerResult computeMultiStrategyProbMax(IMDP<Double> imdp, MDPRewards<Double> mdpRewards, double bound, boolean robust) throws PrismException
	{
		double[] soln = null;
		
		// Start solution
		long timer = System.currentTimeMillis();
		mainLog.println("Starting linear programming (min)...");
		
		
		int n = imdp.getNumStates();
		System.out.println(n);
		int sInit = imdp.getFirstInitialState();
		
		double BIG = 1.01;
		BitSet no = imdp.getLabelStates("Crash");
		BitSet targetS = imdp.getLabelStates("goal");
		imdp.checkForDeadlocks();
		no.andNot(targetS);
		//targetS.andNot(no);
		BitSet everything = new BitSet();
		everything.set(0, n);
		//everything.andNot(no);
		IntSet targetStates = IntSet.asIntSet(targetS);
		IntSet eS = IntSet.asIntSet(everything);
		PrimitiveIterator.OfInt Tstates = targetStates.iterator();
		int numTarget = 0;
		while(Tstates.hasNext()){
			final int s = Tstates.nextInt();
			numTarget += 1;
		}
		int[] target = new int[numTarget];
		Tstates = targetStates.iterator();
		int counter = 0;
		while(Tstates.hasNext()){
			final int s = Tstates.nextInt();
			target[counter] = s;
			counter += 1;
		}
		PrimitiveIterator.OfInt states = eS.iterator();
		IntSet nStates = IntSet.asIntSet(no);
		PrimitiveIterator.OfInt ns = nStates.iterator();
		System.out.println("sInit = " + sInit);
		System.out.println("Crash states = " + no);
		System.out.println("Goal states  = " + targetS);


		if (robust==true){
			mainLog.println("Calcualting multi Strategy under the assumption of worst transition per objective: ");
			try {
				// Initialise MILP solver
				GRBEnv env = new GRBEnv("gurobi.log");
				//env.set(GRB.DoubleParam.TimeLimit, 10800.0);   // 60-second limit 
				env.set(GRB.IntParam.Threads,   0);        // (optional) use all cores
				env.set(GRB.IntParam.OutputFlag, 1);
				/*
				env.set(GRB.IntParam.MIRCuts,      2);
				env.set(GRB.IntParam.CoverCuts,    2);
				env.set(GRB.IntParam.FlowCoverCuts,2);
				env.set(GRB.IntParam.GUBCoverCuts, 2);
				env.set(GRB.IntParam.ImpliedCuts,  2); */
							
				GRBModel m = new GRBModel(env);
				Map<Integer,GRBVar> v;
				Map<String,GRBVar> eta, lam;
				Map<String,Map<Integer,GRBVar>> uplus, umin;

				// Value-function
				v = new HashMap<>();
				for (int s = 0; s <n; s++){
					v.put(s, m.addVar(0.0, 1.0, 0, GRB.CONTINUOUS, "mu["+s+"]"));
				}
				Tstates = targetStates.iterator();
				while(Tstates.hasNext()){
					final int s = Tstates.nextInt();
					m.addConstr(v.get(s),GRB.EQUAL,1.0,"v-target");
				}
				
				while(ns.hasNext()){
					final int s = ns.nextInt();
					//v.put(s, m.addVar(0.0, BIG, 0, GRB.CONTINUOUS, "mu["+s+"]"));
					m.addConstr(v.get(s),GRB.EQUAL,0.0,"v-no");
				}
				
				// action-variable Binary + Dualization
				eta = new HashMap<>();
				lam = new HashMap<>();
				uplus = new HashMap<>();
				umin = new HashMap<>();
				states = eS.iterator();
				while(states.hasNext()){
					final int s = states.nextInt();
					int numChoices = imdp.getNumChoices(s);
					for (int c = 0; c < numChoices; c++){
						String sc = s + "-" + c;
						eta.put(sc, m.addVar(0.0,1.0,0.0,GRB.BINARY,"eta"+"-"+sc));
						lam.put(sc, m.addVar(-1.0,1.0,0,GRB.CONTINUOUS,"lam"+"-"+sc));
						GRBLinExpr lamAU = new GRBLinExpr();
						lamAU.addTerm(BIG, eta.get(sc));
						GRBLinExpr lamAD = new GRBLinExpr();
						lamAD.addTerm(-BIG, eta.get(sc));
						m.addConstr(lam.get(sc), GRB.LESS_EQUAL, lamAU, "lamU["+sc+"]");
						m.addConstr(lam.get(sc), GRB.GREATER_EQUAL, lamAD, "lamD["+sc+"]");
						uplus.put(sc, new HashMap<>());
						umin.put(sc, new HashMap<>());
						Iterator<Map.Entry<Integer, Interval<Double>>> iter = imdp.getTransitionsIterator(s, c);
						while(iter.hasNext()){
							Map.Entry<Integer, Interval<Double>> e = iter.next();
							int t = e.getKey();
							Interval<Double> transition_val = e.getValue();
							String key = sc + "->" + t;
							GRBVar up = m.addVar(0,1.01,0,GRB.CONTINUOUS,"uplus["+key+"]");
							GRBVar um = m.addVar(0,1.01,0,GRB.CONTINUOUS,"umin["+key+"]");
							uplus.get(sc).put(t, up);
							umin.get(sc).put(t, um);
							GRBLinExpr allowed = new GRBLinExpr();
							//allowed.addConstant(BIG);
							allowed.addTerm(BIG, eta.get(sc));
							m.addConstr(uplus.get(sc).get(t), GRB.GREATER_EQUAL, 0.0,"uPlusLow-"+sc+"-"+t );
							m.addConstr(umin.get(sc).get(t), GRB.GREATER_EQUAL, 0.0,"uMinLow-"+sc+"-"+t );
							m.addConstr(uplus.get(sc).get(t), GRB.LESS_EQUAL, allowed,"uPlusHigh-"+sc+"-"+t);
							m.addConstr(umin.get(sc).get(t), GRB.LESS_EQUAL, allowed,"uMinHigh-"+sc+"-"+t);
							GRBLinExpr dualExpr = new GRBLinExpr();
							dualExpr.addTerm(1.0, lam.get(sc));
							dualExpr.addTerm(-1.0, uplus.get(sc).get(t));
							dualExpr.addTerm(+1.0, umin.get(sc).get(t));
							m.addConstr(dualExpr, GRB.LESS_EQUAL, v.get(t), "dualFeas_"+sc+"_"+t);
						}
					}
				}
				states = eS.iterator();
				while(states.hasNext()) {
					final int s = states.nextInt();
					GRBLinExpr expr = new GRBLinExpr();
					for (int c = 0; c < imdp.getNumChoices(s); c++)
						expr.addTerm(1.0, eta.get(s+"-"+c));
					m.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "actSel-"+s);
				}
				

				double LAM_MIN = -1.0;
				double LAM_MAX = 1.0;
				//Pre-bell
				Map<String,GRBVar> phi = new HashMap<>();
				states = eS.iterator();
				while(states.hasNext()) {
					final int s = states.nextInt();
					for (int c = 0; c < imdp.getNumChoices(s); c++) {
						String sc   = s + "-" + c;
						GRBVar etaVar = eta.get(sc), lamVar = lam.get(sc);

						// φ in the same bounds as λ
						GRBVar phiVar = m.addVar(LAM_MIN, LAM_MAX, 0.0, GRB.CONTINUOUS, "phi-"+sc);
						phi.put(sc, phiVar);

						// McCormick: exact for binary η and bounded λ
						// 1) φ ≥ L * η
						GRBLinExpr mc1 = new GRBLinExpr();
						mc1.addTerm(LAM_MIN, etaVar);
						m.addConstr(phiVar, GRB.GREATER_EQUAL, mc1, "mc1-"+sc);

						// 2) φ ≤ U * η
						GRBLinExpr mc2 = new GRBLinExpr();
						mc2.addTerm(LAM_MAX, etaVar);
						m.addConstr(phiVar, GRB.LESS_EQUAL, mc2, "mc2-"+sc);

						// 3) φ ≥ λ − U*(1−η)
						GRBLinExpr mc3 = new GRBLinExpr();
						mc3.addTerm(1.0, lamVar);
						mc3.addConstant(-LAM_MAX);
						mc3.addTerm(LAM_MAX, etaVar);
						m.addConstr(phiVar, GRB.GREATER_EQUAL, mc3, "mc3-"+sc);

						// 4) φ ≤ λ − L*(1−η)
						GRBLinExpr mc4 = new GRBLinExpr();
						mc4.addTerm(1.0, lamVar);
						mc4.addConstant(-LAM_MIN);
						mc4.addTerm(LAM_MIN, etaVar);
						m.addConstr(phiVar, GRB.LESS_EQUAL, mc4, "mc4-"+sc);
					}
				}
				
				
				m.update();


				// Bellman constraint
				states = eS.iterator();
				while(states.hasNext()) {
					final int s = states.nextInt();
					int numChoices = imdp.getNumChoices(s);
					for (int c = 0; c < numChoices; c++) {
						String sc = s + "-" + c;
						GRBLinExpr expr = new GRBLinExpr();
						//expr.addConstant(mdpRewards.getTransitionReward(s,c));

						// add phi = λ·η
						expr.addTerm(1.0, phi.get(sc));

						// add ∑[hi·uplus – lo·umin]
						GRBLinExpr ins = new GRBLinExpr();
						Iterator<Map.Entry<Integer,Interval<Double>>> it = imdp.getTransitionsIterator(s,c);
						while (it.hasNext()) {
							Map.Entry<Integer,Interval<Double>> e = it.next();
							int t = e.getKey();
							double lo = e.getValue().getLower(), hi = e.getValue().getUpper();
							ins.addTerm(-hi, uplus.get(sc).get(t) );
							ins.addTerm(+lo,  umin .get(sc).get(t) );
						}
						expr.add(ins);

						expr.addConstant(+BIG);
						expr.addTerm(-BIG, eta.get(sc) );


						// post v[s] ≥ expr
						m.addConstr( v.get(s), GRB.LESS_EQUAL, expr, "bell-"+sc );
						
						
					}
				}

				// Bound for v[0];

				m.addConstr(v.get(sInit), GRB.GREATER_EQUAL, bound, "multi-bound");
				
				m.update();

				// Objective
				GRBLinExpr pen = new GRBLinExpr();
				for (int s = 0; s < n; s++){
					int numChoices = imdp.getNumChoices(s);
					for (int c = 0; c < numChoices; c++){
						String sc = s +"-"+c;
						pen.addConstant(1.0);
						pen.addTerm(-1.0, eta.get(sc));
					}
				}
				m.setObjective(pen, GRB.MINIMIZE);
				// set the model’s objective to pen, minimize it
				/* GRBLinExpr pen = new GRBLinExpr();
				pen.addTerm(1.0, v.get(sInit));
				m.setObjective(pen, GRB.MAXIMIZE); */
				m.setCallback(new trigger(m, 3600.0));
				// now optimize
				int numVars   = m.get(GRB.IntAttr.NumVars);
				int numConstr = m.get(GRB.IntAttr.NumConstrs);
				int numIntVars  = m.get(GRB.IntAttr.NumIntVars);   // counts both BINARY and INTEGER types
				int numContVars = numVars - numIntVars;
				mainLog.println("Continuous Variables = " + numContVars + ", Integer Variables = "+ numIntVars + ", Constraints = " + numConstr);
				System.out.println("Continuous Variables = " + numContVars + ", Integer Variables = "+ numIntVars + ", Constraints = " + numConstr);
				m.write("gurobi.lp");

				long timerGurobi = System.currentTimeMillis();
				m.optimize();
				long afterGurobi = System.currentTimeMillis();
				long timeTake = afterGurobi - timerGurobi;
				mainLog.println("Gurobi solve time: " + timeTake);
				//System.out.printf("v[0]: %.4f%n", v.get(0).get(GRB.DoubleAttr.X));
				if (m.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
					System.out.println("Model infeasible—computing IIS...");
					m.computeIIS();
					for (GRBConstr c : m.getConstrs()) {
						if (c.get(GRB.IntAttr.IISConstr) == 1) {
							System.out.println("IIS: " + c.get(GRB.StringAttr.ConstrName));
						}
					}
					// Optionally write the IIS to a file:
					m.write("model.IIS");
				}
				System.out.printf("v[0]: %.4f%n", v.get(sInit).get(GRB.DoubleAttr.X));

				soln = new double[n];
				for (int s = 0; s < n; s++) {
					soln[s] = v.get(s).get(GRB.DoubleAttr.X);
					mainLog.println("state: " + s + ", value: " + soln[s]);
				}
				

				mainLog.println("Multi Strategy to Obtain at least under worst assumption of adversary" + bound);
				

				// How many solutions are in the pool?
				int solCount = m.get(GRB.IntAttr.SolCount);

				// Loop over each solution k=0…solCount−1

				for (int s = 0; s < n; s++) {
					mainLog.print(s + ":");
					int numChoices = imdp.getNumChoices(s);
					for (int i = 0; i < numChoices; i++) {
						String sc = s +"-"+i;
						if (eta.get(sc).get(GRB.DoubleAttr.X) > 0) {
							mainLog.print(" " + imdp.getAction(s, i));
						}
					}
					mainLog.println();
				}
				int permC = 0;
				for (int s = 0; s < n; s++) {
					//System.out.println(v.get(s).get(GRB.StringAttr.VarName) + " = "+v.get(s).get(GRB.DoubleAttr.X));
					int numChoices = imdp.getNumChoices(s);
					
					if (numChoices == 1){
						continue;
					} else {
						int tmC = 0;
						for (int i = 0; i < numChoices; i++) {
							String sc = s +"-"+i;
							if (eta.get(sc).get(GRB.DoubleAttr.X) > 0) {
								tmC += 1;
							}
						}
						if (tmC > 1){
							permC += 1;
						}
					}
				}
				mainLog.println("Number of Permissive States: " + permC);
				// Clean up
				int reachable = 0;
				for (int s = 0; s < n; s++) {
					if (soln[s] > 0.0){
						if (soln[s] < 1.0){
							int numChoices = imdp.getNumChoices(s);
							if (numChoices == 1){
								continue;
							} else {
								for (int i = 0; i < numChoices; i++) {
									reachable += 1;
								}
							}
						}
						
						
					}
				}

				mainLog.println("Total number of reachable permissable state-action pairs: " + reachable);
				

				reachable = 0;
				for (int s = 0; s < n; s++) {
					if (soln[s] > 0.0){
						if (soln[s] <= 1.0){
							int numChoices = imdp.getNumChoices(s);
						
							for (int i = 0; i < numChoices; i++) {
								reachable += 1;
							}
						} 
					}
				}

				mainLog.println("Total number of reachable state-action pairs: " + reachable);
				m.dispose();
				env.dispose();
			} catch (GRBException e) {
				throw new PrismException("Error solving LP: " +e.getMessage());
			}
	} else {

		// Multi-strategy under best assumption
		mainLog.println("Calcualting multi Strategy under the assumption of best transition per objective: ");
		double tol = 1e-9;
		
			try {
				 // create environment & model
			 	System.out.println("Im here");
				GRBEnv env = new GRBEnv("gurobi.log");
				env.set(GRB.IntParam.OutputFlag, 0);
				env.set(GRB.DoubleParam.TimeLimit, 3600.0);   // 60-second limit 
				env.set(GRB.IntParam.Threads,   0);        // (optional) use all cores
				env.set(GRB.IntParam.OutputFlag, 1);
				env.set(GRB.IntParam.Cuts, 2);
				GRBModel m = new GRBModel(env);
				

				// --- 1) Value‐function vars v[s] ---
				Map<Integer,GRBVar> v = new HashMap<>();
				for (int s = 0; s < n; s++) {
					v.put(s, m.addVar(0.0,1.0, 0.0, GRB.CONTINUOUS, "mu["+s+"]"));
				}
				/* for (int i = 0; i < numTarget; i++) {
					m.addConstr(v.get(target[i]), GRB.EQUAL, 1.0, "v-target-"+target[i]);
				} */
				Tstates = targetStates.iterator();
				while(Tstates.hasNext()){
					final int s = Tstates.nextInt();
					m.addConstr(v.get(s),GRB.EQUAL,1.0,"v-target");
				}
				
				while(ns.hasNext()){
					final int s = ns.nextInt();
					v.put(s, m.addVar(0.0, 1.0, 0, GRB.CONTINUOUS, "mu["+s+"]"));
					m.addConstr(v.get(s),GRB.EQUAL,0.0,"v-no");
				}
				m.update();

				// --- 2) Binary η and dual vars lam (α), uplus (β), umin (γ) ---
				Map<String,GRBVar> eta = new HashMap<>();
				Map<String,Map<Integer,GRBVar>> delta = new HashMap<>();

				states = eS.iterator();
				
				while(states.hasNext()){
					final int s = states.nextInt();
					int numC = imdp.getNumChoices(s);
					for (int c = 0; c < numC; c++) {
						String sc = s + "-" + c;
						eta.put(sc, m.addVar(0.0,1.0,0.0,GRB.BINARY,"eta"+"-"+sc));
						// collect successors + bounds, and create delta vars
						List<Integer> tList = new ArrayList<>();
						List<Double> loList = new ArrayList<>(), hiList = new ArrayList<>();
						Iterator<Map.Entry<Integer, Interval<Double>>> it = imdp.getTransitionsIterator(s, c);
						List<Interval<Double>> li = new ArrayList<>();
						while (it.hasNext()) {
							Map.Entry<Integer, Interval<Double>> e = it.next();
							int t = e.getKey();
							Interval<Double> w = e.getValue();
							li.add(w);
							tList.add(t);
						}

						System.out.println("tList: " + tList.toString());
						for (int i = 0; i < li.size(); i++) {
							Interval<Double> w = li.get(i);
							double lo = Math.min(w.getLower(), w.getUpper()); // normalize just in case
							double hi = Math.max(w.getLower(), w.getUpper());
							loList.add(lo);
							hiList.add(hi);
						}


						

						// Enumerate all vertices via "n-1 at bounds + 1 free"


						List<Map<Integer,Double>> result = new ArrayList<>();
						Set<String> seen = new HashSet<>();
						for (int j = 0; j < tList.size(); j++) {
							int freeState = tList.get(j);
							List<Map<Integer,Double>> verts =
								findExtremeCombos(tList, loList, hiList, tol, freeState); // <-- new overload
							for (Map<Integer,Double> combo : verts) {
								String key = comboKey(combo, tList);
								if (seen.add(key)) result.add(combo);  // keep only novel vertices
							}
						}

						int count = 0;
						for (Map<Integer,Double> combo : result) {
							StringBuilder sb = new StringBuilder();
							GRBLinExpr expr = new GRBLinExpr();
							for (int i = 0; i < tList.size(); i++) {
								int t = tList.get(i);
								double p = combo.get(t);
								if (i > 0) sb.append(", ");
								sb.append("state ").append(t).append(": ")
								.append(String.format(Locale.US, "%.9f", p));
								expr.addTerm(p, v.get(t));
							}
							// indicator-style big-M for the chosen (s,c)
							expr.addConstant(+BIG);
							expr.addTerm(-BIG, eta.get(sc));
							m.addConstr(v.get(s), GRB.LESS_EQUAL, expr,
										"state-"+s+"-choice-"+c+"-combination-"+count);

							count++;
						}
					}
				}

				// --- 3) Action‐selection: sum η ≥ 1 at each state ---
				states = eS.iterator();
				while(states.hasNext()) {
					final int s = states.nextInt();
					GRBLinExpr expr = new GRBLinExpr();
					for (int c = 0; c < imdp.getNumChoices(s); c++)
						expr.addTerm(1.0, eta.get(s+"-"+c));
					m.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "actSel-"+s);
				}


				// 3g) Bellman 
				m.addConstr(v.get(sInit), GRB.GREATER_EQUAL, bound, "multi-bound");
				m.update();



				// 5) objective:  minimize sum(1−η)  ≡  minimize (   #actions  − sum η  )
				GRBLinExpr pen = new GRBLinExpr();

				for (int s = 0; s < n; s++){
					int numChoices = imdp.getNumChoices(s);
					for (int c = 0; c < numChoices; c++){
						String sc = s +"-"+c;
						pen.addConstant(1.0);
						pen.addTerm(-1.0, eta.get(sc));
					}
				}

				m.setObjective(pen, GRB.MINIMIZE);
				
				// set the model’s objective to pen, minimize it

				m.setObjective(pen, GRB.MINIMIZE);
				/*GRBLinExpr pen = new GRBLinExpr();
				pen.addTerm(1.0, v.get(sInit));
				m.setObjective(pen, GRB.MAXIMIZE);*/
				int numVars   = m.get(GRB.IntAttr.NumVars);
				int numConstr = m.get(GRB.IntAttr.NumConstrs);
				int numIntVars  = m.get(GRB.IntAttr.NumIntVars);   // counts both BINARY and INTEGER types
				int numContVars = numVars - numIntVars;
				mainLog.println("Continuous Variables = " + numContVars + ", Integer Variables = "+ numIntVars + ", Constraints = " + numConstr);
				System.out.println("Continuous Variables = " + numContVars + ", Integer Variables = "+ numIntVars + ", Constraints = " + numConstr);

				m.setCallback(new trigger(m, 3600.0));
				m.write("gurobi.lp");

				long timerGurobi = System.currentTimeMillis();
				m.optimize();
				long afterGurobi = System.currentTimeMillis();
				long timeTake = afterGurobi - timerGurobi;
				mainLog.println("Gurobi Solve time: " + timeTake);

				//System.out.printf("v[0]: %.4f%n", v.get(0).get(GRB.DoubleAttr.X));
				if (m.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
					System.out.println("Model infeasible—computing IIS...");
					m.computeIIS();
					for (GRBConstr c : m.getConstrs()) {
						if (c.get(GRB.IntAttr.IISConstr) == 1) {
							System.out.println("IIS: " + c.get(GRB.StringAttr.ConstrName));
						}
					}
					// Optionally write the IIS to a file:
					m.write("model.IIS");
				}
				int status = m.get(GRB.IntAttr.Status);
				System.out.println("Gurobi Status = " + status);

				soln = new double[n];
				for (int s = 0; s < n; s++) {
					soln[s] = v.get(s).get(GRB.DoubleAttr.X);
					mainLog.println("state: " + s + ", value: " + soln[s]);
				}

				mainLog.println("Multi Strategy to Obtain at least under worst assumption of adversary" + bound);
				

				// How many solutions are in the pool?
				int solCount = m.get(GRB.IntAttr.SolCount);

				// Loop over each solution k=0…solCount−1

				
				for (int s = 0; s < n; s++) {
					mainLog.print(s + ":");
					int numChoices = imdp.getNumChoices(s);
					for (int i = 0; i < numChoices; i++) {
						String sc = s +"-"+i;
						if (eta.get(sc).get(GRB.DoubleAttr.X) > 0) {
							mainLog.print(" " + imdp.getAction(s, i));
						}
					}
					mainLog.println();
				}
				int permC = 0;
				for (int s = 0; s < n; s++) {
					//System.out.println(v.get(s).get(GRB.StringAttr.VarName) + " = "+v.get(s).get(GRB.DoubleAttr.X));
					int numChoices = imdp.getNumChoices(s);
					
					if (numChoices == 1){
						continue;
					} else {
						int tmC = 0;
						for (int i = 0; i < numChoices; i++) {
							String sc = s +"-"+i;
							if (eta.get(sc).get(GRB.DoubleAttr.X) > 0) {
								tmC += 1;
							}
						}
						if (tmC > 1){
							permC += 1;
						}
					}
				}
				mainLog.println("Number of Permissive States: " + permC);
				mainLog.println("Value at inital state " + v.get(sInit).get(GRB.DoubleAttr.X));

				int reachable = 0;
				int reachable_state = 0;
				for (int s = 0; s < n; s++) {
					if (soln[s] > 0.0){
						if (soln[s] < 1.0){
							reachable_state += 1;
							int numChoices = imdp.getNumChoices(s);
							if (numChoices == 1){
								continue;
							} else {
								for (int i = 0; i < numChoices; i++) {
									reachable += 1;
								}
							}
						}
						
						
					}
				}

				mainLog.println("Total number of reachable permissable state-action pairs: " + reachable);
				mainLog.println("Reachable States: " + reachable_state);
				

				reachable = 0;
				for (int s = 0; s < n; s++) {
					if (soln[s] > 0.0){
						if (soln[s] <= 1.0){
							int numChoices = imdp.getNumChoices(s);
						
							for (int i = 0; i < numChoices; i++) {
								reachable += 1;
							}
						}
						
						
						
					}
				}

				mainLog.println("Total number of reachable state-action pairs: " + reachable);
				m.dispose();
				env.dispose();
			} catch (GRBException e) {
				throw new PrismException("Error solving LP: " +e.getMessage());
			}
	}
		long time = (System.currentTimeMillis() - timer);
		mainLog.println("It took "+time + " milli-seconds to finish synthesize the permissive strategy");

		// Return results
		ModelCheckerResult res = new ModelCheckerResult();
//		res.accuracy = AccuracyFactory.boundedNumericalIterations();
		res.soln = soln;
//		res.timeTaken = timer / 1000.0;
		return res;
	}

	/* public static List<Map<Integer, Double>> findExtremeCombos(
			List<Integer> tList,
			List<Double>  loList,
			List<Double>  hiList,
			double tol
	) {
		final int n = tList.size();
		if (loList.size() != n || hiList.size() != n) {
			throw new IllegalArgumentException("tList, loList, hiList must have same length");
		}

		// Copy into arrays for speed; normalize (lo<=hi).
		final int[]    t  = new int[n];
		final double[] lo = new double[n];
		final double[] hi = new double[n];
		for (int i = 0; i < n; i++) {
			t[i]  = tList.get(i);
			double a = loList.get(i), b = hiList.get(i);
			lo[i] = Math.min(a, b);
			hi[i] = Math.max(a, b);
		}

		// Suffix MIN/MAX over remaining indices i..n-1 for pruning.
		final double[] minSuf = new double[n + 1];
		final double[] maxSuf = new double[n + 1];
		for (int i = n - 1; i >= 0; --i) {
			minSuf[i] = minSuf[i + 1] + Math.min(lo[i], hi[i]);
			maxSuf[i] = maxSuf[i + 1] + Math.max(lo[i], hi[i]);
		}

		// Global feasibility check.
		if (1.0 < minSuf[0] - tol || 1.0 > maxSuf[0] + tol) {
			return Collections.emptyList();
		}

		final List<Map<Integer, Double>> out = new ArrayList<>();
		final int[] choice = new int[n]; // 0 => lo[i], 1 => hi[i]

		// DFS with branch-and-bound.
		class DFS {
			void go(int i, double sum) {
				if (i == n) {
					if (Math.abs(sum - 1.0) <= tol) {
						// Build { t -> chosen value } for this combination.
						Map<Integer, Double> combo = new LinkedHashMap<>(n);
						for (int k = 0; k < n; k++) {
							combo.put(t[k], choice[k] == 0 ? lo[k] : hi[k]);
						}
						out.add(combo);
					}
					return;
				}
				double low  = sum + minSuf[i];
				double high = sum + maxSuf[i];
				if (1.0 < low - tol || 1.0 > high + tol) return; // prune

				choice[i] = 0; go(i + 1, sum + lo[i]); // pick lower
				choice[i] = 1; go(i + 1, sum + hi[i]); // pick upper
			}
		}
		new DFS().go(0, 0.0);
		return out;
	} */

		public static List<Map<Integer, Double>> findExtremeCombos(
        List<Integer> tList,
        List<Double>  loList,
        List<Double>  hiList,
        double tol,
        int freeState // the state id that gets the dumped remainder
			) {
				final int n = tList.size();
				System.out.println("n = " + n);
				System.out.println(loList.size());
				System.out.println(hiList.size());
				if (loList.size() != n || hiList.size() != n) {
					throw new IllegalArgumentException("tList, loList, hiList must have same length");
				}

				// copy + normalize
				final int[]    t  = new int[n];
				final double[] lo = new double[n];
				final double[] hi = new double[n];
				for (int i = 0; i < n; i++) {
					t[i]  = tList.get(i);
					double a = loList.get(i), b = hiList.get(i);
					lo[i] = Math.min(a, b);
					hi[i] = Math.max(a, b);
				}

				// locate free index by state id
				int jTmp = -1;
				for (int i = 0; i < n; i++) if (t[i] == freeState) { jTmp = i; break; }
				if (jTmp < 0) throw new IllegalArgumentException("freeState not found in tList");

				// **** make all captured locals final ****
				final int freeIdx = jTmp;
				final int nLocal  = n;

				// build arrays for the other (n-1) indices
				final int m = nLocal - 1;
				final int[]    tO  = new int[m];
				final double[] loO = new double[m];
				final double[] hiO = new double[m];
				for (int i = 0, k = 0; i < nLocal; i++) {
					if (i == freeIdx) continue;
					tO[k]  = t[i];
					loO[k] = lo[i];
					hiO[k] = hi[i];
					k++;
				}
				final double loJ = lo[freeIdx], hiJ = hi[freeIdx];
				final int    tJ  = t[freeIdx];

				// suffix bounds over "others"
				final double[] minSuf = new double[m + 1];
				final double[] maxSuf = new double[m + 1];
				for (int i = m - 1; i >= 0; --i) {
					minSuf[i] = minSuf[i + 1] + Math.min(loO[i], hiO[i]);
					maxSuf[i] = maxSuf[i + 1] + Math.max(loO[i], hiO[i]);
				}

				// feasible sum range for others so that p_free is within its interval
				final double sMinFeas = 1.0 - hiJ;
				final double sMaxFeas = 1.0 - loJ;
				if (maxSuf[0] + tol < sMinFeas || minSuf[0] - tol > sMaxFeas) {
					return Collections.emptyList();
				}

				final List<Map<Integer, Double>> out = new ArrayList<>();
				final int[] choice = new int[m]; // 0->lo, 1->hi

				class DFS {
					void go(int i, double sumOthers) {
						if (i == m) {
							final double pJ = 1.0 - sumOthers;
							if (pJ >= loJ - tol && pJ <= hiJ + tol) {
								Map<Integer, Double> combo = new LinkedHashMap<>(nLocal);
								int k = 0;
								for (int idx = 0; idx < nLocal; idx++) {
									if (idx == freeIdx) {
										combo.put(tJ, pJ);
									} else {
										double v = (choice[k] == 0 ? loO[k] : hiO[k]);
										combo.put(tO[k], v);
										k++;
									}
								}
								out.add(combo);
							}
							return;
						}
						double low  = sumOthers + minSuf[i];
						double high = sumOthers + maxSuf[i];
						if (high + tol < sMinFeas || low - tol > sMaxFeas) return;

						choice[i] = 0; go(i + 1, sumOthers + loO[i]);
						choice[i] = 1; go(i + 1, sumOthers + hiO[i]);
					}
				}
				new DFS().go(0, 0.0);
				return out;
			}

			// Helper to de-dupe combos (round to ~1e-12 to be safe)
			static String comboKey(Map<Integer,Double> combo, List<Integer> tOrder) {
				StringBuilder sb = new StringBuilder();
				for (int tt : tOrder) {
					double p = combo.get(tt);
					sb.append(tt).append(':')
					.append(String.format(Locale.US, "%.12f", p))
					.append('|');
				}
				return sb.toString();
			}



	
	/**
	 * Simple test program.
	 */
	public static void main(String args[])
	{
		try {
			IMDPModelChecker mc = new IMDPModelChecker(null);
			Evaluator<Interval<Double>> eval = Evaluator.forDoubleInterval();
			IMDPSimple<Double> imdp = new IMDPSimple<>();
			imdp.setEvaluator(eval);
			imdp.addState();
			imdp.addState();
			imdp.addState();
			imdp.addInitialState(0);
			Distribution<Interval<Double>> distr;
			distr = new Distribution<>(eval);
			distr.add(1, new Interval<Double>(0.2, 0.4));
			distr.add(2, new Interval<Double>(0.6, 0.8));
			imdp.addActionLabelledChoice(0, distr, "a");
			distr = new Distribution<>(eval);
			distr.add(1, new Interval<Double>(0.1, 0.3));
			distr.add(2, new Interval<Double>(0.7, 0.9));
			imdp.addActionLabelledChoice(0, distr, "b");
			imdp.findDeadlocks(true);
			imdp.exportToDotFile("imdp.dot");
			BitSet target = new BitSet();
			target.set(2);
			ModelCheckerResult res;
			res = mc.computeReachProbs(imdp, target, MinMax.min().setMinUnc(true));
			System.out.println("minmin: " + res.soln[0]);
			res = mc.computeReachProbs(imdp, target, MinMax.min().setMinUnc(false));
			System.out.println("minmax: " + res.soln[0]);
			res = mc.computeReachProbs(imdp, target, MinMax.max().setMinUnc(true));
			System.out.println("maxmin: " + res.soln[0]);
			res = mc.computeReachProbs(imdp, target, MinMax.max().setMinUnc(false));
			System.out.println("maxmax: " + res.soln[0]);
		} catch (PrismException e) {
			System.out.println(e);
		}
	}
}
