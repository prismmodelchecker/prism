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

import java.util.BitSet;
import java.util.PrimitiveIterator;

import acceptance.AcceptanceReach;
import common.IntSet;
import common.IterableStateSet;
import explicit.rewards.MCRewards;
import explicit.rewards.Rewards;
import parser.ast.Expression;
import prism.AccuracyFactory;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismFileLog;

/**
 * Explicit-state model checker for interval discrete-time Markov chains (IDTMCs).
 */
public class IDTMCModelChecker extends ProbModelChecker
{
	// DTMCModelChecker in order to use e.g. precomputation algorithms
	protected DTMCModelChecker mcDTMC = null; 
	
	/**
	 * Create a new IDTMCModelChecker, inherit basic state from parent (unless null).
	 */
	public IDTMCModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);
		mcDTMC = new DTMCModelChecker(this);
		mcDTMC.inheritSettings(this);
	}

	// Model checking functions
	
	@Override
	@SuppressWarnings("unchecked")
	protected StateValues checkProbPathFormulaCosafeLTL(Model<?> model, Expression expr, boolean qual, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		// Build product of IMC and DFA for the LTL formula, and do any required exports
		LTLModelChecker mcLtl = new LTLModelChecker(this);
		LTLModelChecker.LTLProduct<IDTMC<Double>> product = mcLtl.constructDFAProductForCosafetyProbLTL(this, (IDTMC<Double>) model, expr, statesOfInterest);
		doProductExports(product);
		
		// Find accepting states + compute reachability probabilities
		BitSet acc = ((AcceptanceReach)product.getAcceptance()).getGoalStates();
		mainLog.println("\nComputing reachability probabilities...");
		IDTMCModelChecker mcProduct = new IDTMCModelChecker(this);
		mcProduct.inheritSettings(this);
		ModelCheckerResult res = mcProduct.computeReachProbs(product.getProductModel(), acc, minMax);
		StateValues probsProduct = StateValues.createFromDoubleArrayResult(res, product.getProductModel());

		// Output vector over product, if required
		if (getExportProductVector()) {
				mainLog.println("\nExporting product solution vector matrix to file \"" + getExportProductVectorFilename() + "\"...");
				PrismFileLog out = new PrismFileLog(getExportProductVectorFilename());
				probsProduct.print(out, false, false, false, false);
				out.close();
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
		// Build product of IMC and DFA for the LTL formula, convert rewards and do any required exports
		LTLModelChecker mcLtl = new LTLModelChecker(this);
		LTLModelChecker.LTLProduct<IDTMC<Double>> product = mcLtl.constructDFAProductForCosafetyReward(this, (IDTMC<Double>) model, expr, statesOfInterest);
		MCRewards<Double> productRewards = ((MCRewards<Double>) modelRewards).liftFromModel(product);
		doProductExports(product);

		// Find accepting states + compute reachability rewards
		BitSet acc = ((AcceptanceReach)product.getAcceptance()).getGoalStates();
		mainLog.println("\nComputing reachability rewards...");
		IDTMCModelChecker mcProduct = new IDTMCModelChecker(this);
		mcProduct.inheritSettings(this);
		ModelCheckerResult res = mcProduct.computeReachRewards(product.getProductModel(), productRewards, acc, minMax);
		StateValues rewardsProduct = StateValues.createFromDoubleArrayResult(res, product.getProductModel());
		
		// Output vector over product, if required
		if (getExportProductVector()) {
				mainLog.println("\nExporting product solution vector matrix to file \"" + getExportProductVectorFilename() + "\"...");
				PrismFileLog out = new PrismFileLog(getExportProductVectorFilename());
				rewardsProduct.print(out, false, false, false, false);
				out.close();
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
	 * @param idtmc The IDTMC
	 * @param target Target states
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeNextProbs(IDTMC<Double> idtmc, BitSet target, MinMax minMax) throws PrismException
	{
		long timer = System.currentTimeMillis();

		// Check for any zero lower probability bounds (not supported
		// since this approach assumes the graph structure remains static)
		idtmc.checkLowerBoundsArePositive();
		
		// Store num states
		int n = idtmc.getNumStates();
		PrimitiveIterator.OfInt statesAll = new IterableStateSet(n).iterator();

		// Create/initialise solution vector(s)
		double[] soln = Utils.bitsetToDoubleArray(target, n);
		double[] soln2 = new double[n];

		// Next-step probabilities
		idtmc.mvMultUnc(soln, minMax, soln2, statesAll);

		// Return results
		ModelCheckerResult res = new ModelCheckerResult();
		res.soln = soln2;
		res.accuracy = AccuracyFactory.boundedNumericalIterations();
		res.numIters = 1;
		timer = System.currentTimeMillis() - timer;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Compute bounded reachability probabilities.
	 * i.e. compute the probability of reaching a state in {@code target} within k steps.
	 * @param idtmc The IDTMC
	 * @param target Target states
	 * @param k Bound
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeBoundedReachProbs(IDTMC<Double> idtmc, BitSet target, int k, MinMax minMax) throws PrismException
	{
		return computeBoundedUntilProbs(idtmc, null, target, k, minMax);
	}

	/**
	 * Compute bounded until probabilities.
	 * i.e. compute the probability of reaching a state in {@code target},
	 * within k steps, and while remaining in states in {@code remain}.
	 * @param idtmc The IDTMC
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param k Bound
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeBoundedUntilProbs(IDTMC<Double> idtmc, BitSet remain, BitSet target, int k, MinMax minMax) throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet unknown;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		long timer;

		// Start bounded probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting bounded probabilistic reachability...");

		// Check for any zero lower probability bounds (not supported
		// since this approach assumes the graph structure remains static)
		idtmc.checkLowerBoundsArePositive();
		
		// Store num states
		n = idtmc.getNumStates();

		// Create solution vector(s)
		soln = new double[n];
		soln2 = new double[n];

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
			idtmc.mvMultUnc(soln, minMax, soln2, unknownStates.iterator());
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
		return res;
	}
	
	/**
	 * Compute reachability probabilities.
	 * i.e. compute the probability of reaching a state in {@code target}.
	 * @param idtmc The IDTMC
	 * @param target Target states
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeReachProbs(IDTMC<Double> idtmc, BitSet target, MinMax minMax) throws PrismException
	{
		return computeReachProbs(idtmc, null, target, minMax);
	}

	/**
	 * Compute until probabilities.
	 * i.e. compute the probability of reaching a state in {@code target}.
	 * @param idtmc The IDTMC
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeUntilProbs(IDTMC<Double> idtmc, BitSet remain, BitSet target, MinMax minMax) throws PrismException
	{
		return computeReachProbs(idtmc, remain, target, minMax);
	}

	/**
	 * Compute reachability/until probabilities.
	 * i.e. compute the probability of reaching a state in {@code target},
	 * while remaining in those in {@code remain}.
	 * @param idtmc The IDTMC
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeReachProbs(IDTMC<Double> idtmc, BitSet remain, BitSet target, MinMax minMax) throws PrismException
	{
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
		idtmc.checkLowerBoundsArePositive();
		
		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		idtmc.checkForDeadlocks(target);

		// Store num states
		int n = idtmc.getNumStates();

		// Precomputation
		BitSet no, yes;
		PredecessorRelation pre = null;
		if (precomp && (prob0 || prob1) && preRel) {
			pre = idtmc.getPredecessorRelation(this, true);
		}
		if (precomp && prob0) {
			if (preRel) {
				no = mcDTMC.prob0(idtmc, remain, target, pre);
			} else {
				no = mcDTMC.prob0(idtmc, remain, target);
			}
		} else {
			no = new BitSet();
		}
		if (precomp && prob1) {
			if (preRel) {
				yes = mcDTMC.prob1(idtmc, remain, target, pre);
			} else {
				yes = mcDTMC.prob1(idtmc, remain, target);
			}
		} else {
			yes = (BitSet) target.clone();
		}

		// Print results of precomputation
		int numYes = yes.cardinality();
		int numNo = no.cardinality();
		mainLog.println("target=" + target.cardinality() + ", yes=" + numYes + ", no=" + numNo + ", maybe=" + (n - (numYes + numNo)));

		// Start value iteration
		timer = System.currentTimeMillis();
		String sMinMax = minMax.isMinUnc() ? "min" : "max";
		mainLog.println("Starting value iteration (" + sMinMax + ")...");

		// Store num states
		n = idtmc.getNumStates();

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
			IterationMethod.IterationValIter iterationReachProbs = iterationMethod.forMvMultMinMaxUnc(idtmc, minMax);
			iterationReachProbs.init(init);
			IntSet unknownStates = IntSet.asIntSet(unknown);
			String description = sMinMax + ", with " + iterationMethod.getDescriptionShort();
			res = iterationMethod.doValueIteration(this, description, iterationReachProbs, unknownStates, timer, null);
		} else {
			res = new ModelCheckerResult();
			res.soln = Utils.bitsetToDoubleArray(yes, n);
			res.accuracy = AccuracyFactory.doublesFromQualitative();
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
	 * @param idtmc The IDTMC
	 * @param imcRewards The rewards
	 * @param target Target states
	 * @param minMax Min/max info
	 */
	public ModelCheckerResult computeReachRewards(IDTMC<Double> idtmc, MCRewards<Double> imcRewards, BitSet target, MinMax minMax) throws PrismException
	{
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
		idtmc.checkLowerBoundsArePositive();
		
		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		idtmc.checkForDeadlocks(target);

		// Store num states
		int n = idtmc.getNumStates();

		// Precomputation (not optional)
		BitSet inf;
		if (preRel) {
			// prob1 via predecessor relation
			PredecessorRelation pre = idtmc.getPredecessorRelation(this, true);
			inf = mcDTMC.prob1(idtmc, null, target, pre);
		} else {
			// prob1 via fixed-point algorithm
			inf = mcDTMC.prob1(idtmc, null, target);
		}
		inf.flip(0, n);

		// Print results of precomputation
		int numTarget = target.cardinality();
		int numInf = inf.cardinality();
		mainLog.println("target=" + numTarget + ", inf=" + numInf + ", rest=" + (n - (numTarget + numInf)));

		// Start value iteration
		timer = System.currentTimeMillis();
		String sMinMax = minMax.isMinUnc() ? "min" : "max";
		mainLog.println("Starting value iteration (" + sMinMax + ")...");

		// Store num states
		n = idtmc.getNumStates();

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
			IterationMethod.IterationValIter iterationReachProbs = iterationMethod.forMvMultRewMinMaxUnc(idtmc, imcRewards, minMax);
			iterationReachProbs.init(init);
			IntSet unknownStates = IntSet.asIntSet(unknown);
			String description = sMinMax + ", with " + iterationMethod.getDescriptionShort();
			res = iterationMethod.doValueIteration(this, description, iterationReachProbs, unknownStates, timer, null);
		} else {
			res = new ModelCheckerResult();
			res.soln = Utils.bitsetToDoubleArray(inf, n, Double.POSITIVE_INFINITY);
			res.accuracy = AccuracyFactory.doublesFromQualitative();
		}
		
		// Finished probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Probabilistic reachability took " + timer / 1000.0 + " seconds.");

		// Update time taken
		res.timeTaken = timer / 1000.0;

		return res;
	}
}
