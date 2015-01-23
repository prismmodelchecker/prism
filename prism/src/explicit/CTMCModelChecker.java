//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

import java.io.File;
import java.util.*;

import explicit.rewards.MCRewards;
import explicit.rewards.StateRewardsArray;
import parser.ast.*;
import parser.type.*;
import prism.*;

/**
 * Explicit-state model checker for continuous-time Markov chains (CTMCs).
 * 
 * This uses various bits of functionality of DTMCModelChecker, so we inherit from that.
 * (This way DTMCModelChecker picks up any options set on this one.) 
 */
public class CTMCModelChecker extends DTMCModelChecker
{
	/**
	 * Create a new CTMCModelChecker, inherit basic state from parent (unless null).
	 */
	public CTMCModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);
	}
	
	// Model checking functions

	@Override
	protected StateValues checkProbPathFormulaLTL(Model model, Expression expr, boolean qual, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		mainLog.println("Building embedded DTMC...");
		DTMC dtmcEmb = ((CTMC)model).buildImplicitEmbeddedDTMC();
		
		// use superclass (DTMCModelChecker) method on the embedded DTMC
		return super.checkProbPathFormulaLTL(dtmcEmb, expr, qual, minMax, statesOfInterest);
	}
	
	@Override
	protected StateValues checkProbBoundedUntil(Model model, ExpressionTemporal expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
	{
		double lTime, uTime; // time bounds
		Expression exprTmp;
		BitSet b1, b2, tmp;
		StateValues probs = null;
		ModelCheckerResult tmpRes = null, res = null;

		// get info from bounded until

		// lower bound is 0 if not specified
		// (i.e. if until is of form U<=t)
		exprTmp = expr.getLowerBound();
		if (exprTmp != null) {
			lTime = exprTmp.evaluateDouble(constantValues);
			if (lTime < 0) {
				throw new PrismException("Invalid lower bound " + lTime + " in time-bounded until formula");
			}
		} else {
			lTime = 0;
		}
		// upper bound is -1 if not specified
		// (i.e. if until is of form U>=t)
		exprTmp = expr.getUpperBound();
		if (exprTmp != null) {
			uTime = exprTmp.evaluateDouble(constantValues);
			if (uTime < 0 || (uTime == 0 && expr.upperBoundIsStrict())) {
				String bound = (expr.upperBoundIsStrict() ? "<" : "<=") + uTime;
				throw new PrismException("Invalid upper bound " + bound + " in time-bounded until formula");
			}
			if (uTime < lTime) {
				throw new PrismException("Upper bound must exceed lower bound in time-bounded until formula");
			}
		} else {
			uTime = -1;
		}

		// model check operands first for all states
		b1 = checkExpression(model, expr.getOperand1(), null).getBitSet();
		b2 = checkExpression(model, expr.getOperand2(), null).getBitSet();

		// compute probabilities

		// a trivial case: "U<=0"
		if (lTime == 0 && uTime == 0) {
			// prob is 1 in b2 states, 0 otherwise
			probs = StateValues.createFromBitSetAsDoubles(b2, model);
		} else {

			// break down into different cases to compute probabilities

			// >= lTime
			if (uTime == -1) {
				// check for special case of lTime == 0, this is actually an unbounded until
				if (lTime == 0) {
					// compute probs
					res = computeUntilProbs((DTMC) model, b1, b2);
					probs = StateValues.createFromDoubleArray(res.soln, model);
				} else {
					// compute unbounded until probs
					tmpRes = computeUntilProbs((DTMC) model, b1, b2);
					// compute bounded until probs
					res = computeTransientBackwardsProbs((CTMC) model, b1, b1, lTime, tmpRes.soln);
					probs = StateValues.createFromDoubleArray(res.soln, model);
				}
			}
			// <= uTime
			else if (lTime == 0) {
				// nb: uTime != 0 since would be caught above (trivial case)
				b1.andNot(b2);
				res = computeTransientBackwardsProbs((CTMC) model, b2, b1, uTime, null);
				probs = StateValues.createFromDoubleArray(res.soln, model);
				// set values to exactly 1 for target (b2) states
				// (these are computed inexactly during uniformisation)
				int n = model.getNumStates();
				for (int i = 0; i < n; i++) {
					if (b2.get(i))
						probs.setDoubleValue(i, 1);
				}
			}
			// [lTime,uTime] (including where lTime == uTime)
			else {
				tmp = (BitSet) b1.clone();
				tmp.andNot(b2);
				tmpRes = computeTransientBackwardsProbs((CTMC) model, b2, tmp, uTime - lTime, null);
				res = computeTransientBackwardsProbs((CTMC) model, b1, b1, lTime, tmpRes.soln);
				probs = StateValues.createFromDoubleArray(res.soln, model);
			}
		}

		return probs;
	}

	// Steady-state/transient probability computation

	/**
	 * Compute transient probability distribution (forwards).
	 * Start from initial state (or uniform distribution over multiple initial states).
	 */
	public StateValues doTransient(CTMC ctmc, double time) throws PrismException
	{
		return doTransient(ctmc, time, (StateValues) null);
	}

	/**
	 * Compute transient probability distribution (forwards).
	 * Optionally, use the passed in file initDistFile to give the initial probability distribution (time 0).
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 * @param ctmc The CTMC
	 * @param t Time point
	 * @param initDistFile File containing initial distribution
	 */
	public StateValues doTransient(CTMC ctmc, double t, File initDistFile) throws PrismException
	{
		StateValues initDist = readDistributionFromFile(initDistFile, ctmc);
		return doTransient(ctmc, t, initDist);
	}

	/**
	 * Compute transient probability distribution (forwards).
	 * Optionally, use the passed in vector initDist as the initial probability distribution (time 0).
	 * If null, start from initial state (or uniform distribution over multiple initial states).
	 * For reasons of efficiency, when a vector is passed in, it will be trampled over,
	 * so if you wanted it, take a copy. 
	 * @param ctmc The CTMC
	 * @param t Time point
	 * @param initDist Initial distribution (will be overwritten)
	 */
	public StateValues doTransient(CTMC ctmc, double t, StateValues initDist) throws PrismException
	{
		ModelCheckerResult res = null;
		StateValues initDistNew = null, probs = null;

		// Build initial distribution (if not specified)
		if (initDist == null) {
			initDistNew = new StateValues(TypeDouble.getInstance(), new Double(0.0), ctmc);
			double initVal = 1.0 / ctmc.getNumInitialStates();
			for (int in : ctmc.getInitialStates()) {
				initDistNew.setDoubleValue(in, initVal);
			}
		} else {
			initDistNew = initDist;
		}

		// Compute transient probabilities
		res = computeTransientProbs(ctmc, t, initDistNew.getDoubleArray());
		probs = StateValues.createFromDoubleArray(res.soln, ctmc);

		return probs;
	}

	// Numerical computation functions

	/**
	 * Compute next=state probabilities.
	 * i.e. compute the probability of being in a state in {@code target} in the next step.
	 * @param ctmc The CTMC
	 * @param target Target states
	 */
	public ModelCheckerResult computeNextProbs(CTMC ctmc, BitSet target) throws PrismException
	{
		mainLog.println("Building embedded DTMC...");
		DTMC dtmcEmb = ctmc.buildImplicitEmbeddedDTMC();
		return super.computeNextProbs(dtmcEmb, target);
	}

	/**
	 * Compute reachability/until probabilities.
	 * i.e. compute the probability of reaching a state in {@code target},
	 * while remaining in those in @{code remain}.
	 * @param ctmc The CTMC
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param init Optionally, an initial solution vector (may be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	public ModelCheckerResult computeReachProbs(CTMC ctmc, BitSet remain, BitSet target, double init[], BitSet known) throws PrismException
	{
		mainLog.println("Building embedded DTMC...");
		DTMC dtmcEmb = ctmc.buildImplicitEmbeddedDTMC();
		return super.computeReachProbs(dtmcEmb, remain, target, init, known);
	}

	/**
	 * Compute time-bounded reachability probabilities,
	 * i.e. compute the probability of reaching a state in {@code target} within time {@code t}.
	 * @param ctmc The CTMC
	 * @param target Target states
	 * @param t Time bound
	 */
	public ModelCheckerResult computeTimeBoundedReachProbs(CTMC ctmc, BitSet target, double t) throws PrismException
	{
		return computeTimeBoundedUntilProbs(ctmc, null, target, t);
	}

	/**
	 * Compute time-bounded until probabilities,
	 * i.e. compute the probability of reaching a state in {@code target},
	 * within time {@code t}, and while remaining in states in {@code remain}.
	 * @param ctmc The CTMC
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param t Time bound
	 */
	public ModelCheckerResult computeTimeBoundedUntilProbs(CTMC ctmc, BitSet remain, BitSet target, double t) throws PrismException
	{
		BitSet nonAbs = null;
		if (remain != null) {
			nonAbs = (BitSet) remain.clone();
			nonAbs.andNot(target);
		}
		ModelCheckerResult res = computeTransientBackwardsProbs(ctmc, target, nonAbs, t, null);
		// Set values to exactly 1 for target states
		// (these are computed inexactly during uniformisation)
		int n = ctmc.getNumStates();
		for (int i = 0; i < n; i++) {
			if (target.get(i))
				res.soln[i] = 1.0;
		}
		return res;
	}

	/**
	 * Perform transient probability computation, as required for (e.g. CSL) model checking.
	 * Compute, for each state, the sum over {@code target} states
	 * of the probability of being in that state at time {@code t}
	 * multiplied by the corresponding probability in the vector {@code multProbs},
	 * assuming that all states *not* in {@code nonAbs} are made absorbing.
	 * If {@code multProbs} is null, it is assumed to be all 1s.
	 * @param ctmc The CTMC
	 * @param target Target states
	 * @param nonAbs States *not* to be made absorbing (optional: null means "all")
	 * @param t Time bound
	 * @param multProbs Multiplication vector (optional: null means all 1s)
	 */
	public ModelCheckerResult computeTransientBackwardsProbs(CTMC ctmc, BitSet target, BitSet nonAbs, double t, double multProbs[]) throws PrismException
	{
		ModelCheckerResult res = null;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[], sum[];
		DTMC dtmc;
		long timer;
		// Fox-Glynn stuff
		FoxGlynn fg;
		int left, right;
		double q, qt, acc, weights[], totalWeight;

		// Optimisations: If (nonAbs is empty or t = 0) and multProbs is null, this is easy.
		if (((nonAbs != null && nonAbs.isEmpty()) || (t == 0)) && multProbs == null) {
			res = new ModelCheckerResult();
			res.soln = Utils.bitsetToDoubleArray(target, ctmc.getNumStates());
			return res;
		}

		// Start backwards transient computation
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting backwards transient probability computation...");

		// Store num states
		n = ctmc.getNumStates();

		// Get uniformisation rate; do Fox-Glynn
		q = ctmc.getDefaultUniformisationRate(nonAbs);
		qt = q * t;
		mainLog.println("\nUniformisation: q.t = " + q + " x " + t + " = " + qt);
		acc = termCritParam / 8.0;
		fg = new FoxGlynn(qt, 1e-300, 1e+300, acc);
		left = fg.getLeftTruncationPoint();
		right = fg.getRightTruncationPoint();
		if (right < 0) {
			throw new PrismException("Overflow in Fox-Glynn computation (time bound too big?)");
		}
		weights = fg.getWeights();
		totalWeight = fg.getTotalWeight();
		for (i = left; i <= right; i++) {
			weights[i - left] /= totalWeight;
		}
		mainLog.println("Fox-Glynn (" + acc + "): left = " + left + ", right = " + right);

		// Build (implicit) uniformised DTMC
		dtmc = ctmc.buildImplicitUniformisedDTMC(q);

		// Create solution vector(s)
		soln = new double[n];
		soln2 = new double[n];
		sum = new double[n];

		// Initialise solution vectors.
		// Vectors soln/soln2 are 1 for target states, or multProbs[i] if supplied.
		// Vector sum is all zeros (done by array creation).
		if (multProbs != null) {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = target.get(i) ? multProbs[i] : 0.0;
		} else {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = target.get(i) ? 1.0 : 0.0;
		}

		// If necessary, do 0th element of summation (doesn't require any matrix powers)
		if (left == 0)
			for (i = 0; i < n; i++)
				sum[i] += weights[0] * soln[i];

		// Start iterations
		iters = 1;
		while (iters <= right) {
			// Matrix-vector multiply
			dtmc.mvMult(soln, soln2, nonAbs, false);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
			// Add to sum
			if (iters >= left) {
				for (i = 0; i < n; i++)
					sum[i] += weights[iters - left] * soln[i];
			}
			iters++;
		}

		// Finished backwards transient computation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Backwards transient probability computation");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = sum;
		res.lastSoln = soln2;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		res.timePre = 0.0;
		return res;
	}

	/**
	 * Perform cumulative reward computation.
	 * Compute, for each state of {@ctmc}, the expected rewards accumulated until {@code t}
	 * when starting in this state and using reward structure {@code mcRewards}.
	 * @param ctmc The CTMC
	 * @param mcRewards The rewards
	 * @param t Time bound
	 */
	public ModelCheckerResult computeCumulativeRewards(CTMC ctmc, MCRewards mcRewards, double t) throws PrismException
	{
		ModelCheckerResult res = null;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[], sum[];
		long timer;
		// Fox-Glynn stuff
		FoxGlynn fg;
		int left, right;
		double q, qt, acc, weights[], totalWeight;

		// Optimisation: If t = 0, this is easy.
		if (t == 0) {
			res = new ModelCheckerResult();
			res.soln = new double[ctmc.getNumStates()];
			return res;
		}

		// Start backwards transient computation
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting backwards cumulative rewards computation...");

		// Store num states
		n = ctmc.getNumStates();

		// Get uniformisation rate; do Fox-Glynn
		q = ctmc.getDefaultUniformisationRate();
		qt = q * t;
		mainLog.println("\nUniformisation: q.t = " + q + " x " + t + " = " + qt);
		acc = termCritParam / 8.0;
		fg = new FoxGlynn(qt, 1e-300, 1e+300, acc);
		left = fg.getLeftTruncationPoint();
		right = fg.getRightTruncationPoint();
		if (right < 0) {
			throw new PrismException("Overflow in Fox-Glynn computation (time bound too big?)");
		}
		weights = fg.getWeights();
		totalWeight = fg.getTotalWeight();
		for (i = left; i <= right; i++) {
			weights[i - left] /= totalWeight;
		}

		// modify the poisson probabilities to what we need for this computation
		// first make the kth value equal to the sum of the values for 0...k
		for (i = left+1; i <= right; i++) {
			weights[i - left] += weights[i - 1 - left];
		}
		// then subtract from 1 and divide by uniformisation constant (q) to give mixed poisson probabilities
		for (i = left; i <= right; i++) {
			weights[i - left] = (1 - weights[i - left]) / q;
		}
		mainLog.println("Fox-Glynn (" + acc + "): left = " + left + ", right = " + right);

		// Build (implicit) uniformised DTMC
		DTMC dtmcUnif = ctmc.buildImplicitUniformisedDTMC(q);

		// Create solution vector(s)
		soln = new double[n];
		soln2 = new double[n];

		// Initialise solution vectors.
		for (i = 0; i < n; i++)
			soln[i] = mcRewards.getStateReward(i);

		// do 0th element of summation (doesn't require any matrix powers)
		sum = new double[n];
		if (left == 0) {
			for (i = 0; i < n; i++)
				sum[i] += weights[0] * soln[i];
		} else {
			for (i = 0; i < n; i++)
				sum[i] += soln[i] / q;
		}

		// Start iterations
		iters = 1;
		while (iters <= right) {
			// Matrix-vector multiply
			dtmcUnif.mvMult(soln, soln2, null, false);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
			// Add to sum
			if (iters >= left) {
				for (i = 0; i < n; i++)
					sum[i] += weights[iters - left] * soln[i];
			} else {
				for (i = 0; i < n; i++)
					sum[i] += soln[i] / q;
			}
			iters++;
		}

		// Finished backwards transient computation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Backwards transient cumulative rewards computation");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = sum;
		res.lastSoln = soln2;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		res.timePre = 0.0;
		return res;
	}

	/**
	 * Perform instantaneous reward computation.
	 * Compute, for each state of {@ctmc}, the expected rewards at time {@code t}
	 * when starting in this state and using reward structure {@code mcRewards}.
	 * @param ctmc The CTMC
	 * @param mcRewards The rewards
	 * @param t Time bound
	 */
	@Override
	public ModelCheckerResult computeInstantaneousRewards(DTMC dtmc, MCRewards mcRewards, double t) throws PrismException
	{
		ModelCheckerResult res = null;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[], sum[];
		long timer;
		// Fox-Glynn stuff
		FoxGlynn fg;
		int left, right;
		double q, qt, acc, weights[], totalWeight;

		// Store num states
		n = dtmc.getNumStates();

		// Optimisation: If t = 0, this is easy.
		if (t == 0) {
			res = new ModelCheckerResult();
			res.soln = new double[dtmc.getNumStates()];
			for (i = 0; i < n; i++)
				res.soln[i] = mcRewards.getStateReward(i);
			return res;
		}

		// Start backwards transient computation
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting backwards instantaneous rewards computation...");

		CTMC ctmc = (CTMC) dtmc;

		// Get uniformisation rate; do Fox-Glynn
		q = ctmc.getDefaultUniformisationRate();
		qt = q * t;
		mainLog.println("\nUniformisation: q.t = " + q + " x " + t + " = " + qt);
		acc = termCritParam / 8.0;
		fg = new FoxGlynn(qt, 1e-300, 1e+300, acc);
		left = fg.getLeftTruncationPoint();
		right = fg.getRightTruncationPoint();
		if (right < 0) {
			throw new PrismException("Overflow in Fox-Glynn computation (time bound too big?)");
		}
		weights = fg.getWeights();
		totalWeight = fg.getTotalWeight();
		for (i = left; i <= right; i++) {
			weights[i - left] /= totalWeight;
		}

		mainLog.println("Fox-Glynn (" + acc + "): left = " + left + ", right = " + right);

		// Build (implicit) uniformised DTMC
		dtmc = ctmc.buildImplicitUniformisedDTMC(q);

		// Create solution vector(s)
		soln = new double[n];
		soln2 = new double[n];

		// Initialise solution vectors.
		for (i = 0; i < n; i++)
			soln[i] = mcRewards.getStateReward(i);

		// do 0th element of summation (doesn't require any matrix powers)
		sum = new double[n];
		if (left == 0)
			for (i = 0; i < n; i++)
				sum[i] += weights[0] * soln[i];

		// Start iterations
		iters = 1;
		while (iters <= right) {
			// Matrix-vector multiply
			dtmc.mvMult(soln, soln2, null, false);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
			// Add to sum
			if (iters >= left) {
				for (i = 0; i < n; i++)
					sum[i] += weights[iters - left] * soln[i];
			}
			iters++;
		}

		// Finished backwards transient computation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Backwards transient instantaneous rewards computation");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = sum;
		res.lastSoln = soln2;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		res.timePre = 0.0;
		return res;
	}

	/**
	 * Compute expected reachability rewards.
	 * @param ctmc The CTMC
	 * @param mcRewards The rewards
	 * @param target Target states
	 */
	public ModelCheckerResult computeReachRewards(CTMC ctmc, MCRewards mcRewards, BitSet target) throws PrismException
	{
		int i, n;
		// Build embedded DTMC
		mainLog.println("Building embedded DTMC...");
		DTMC dtmcEmb = ctmc.buildImplicitEmbeddedDTMC();
		// Convert rewards
		n = ctmc.getNumStates();
		StateRewardsArray rewEmb = new StateRewardsArray(n);
		for (i = 0; i < n; i++) {
			rewEmb.setStateReward(i, mcRewards.getStateReward(i) / ctmc.getExitRate(i));
		}
		// Do computation on DTMC
		return super.computeReachRewards(dtmcEmb, rewEmb, target);
	}

	/**
	 * Compute transient probabilities.
	 * i.e. compute the probability of being in each state at time {@code t},
	 * assuming the initial distribution {@code initDist}. 
	 * For space efficiency, the initial distribution vector will be modified and values over-written,  
	 * so if you wanted it, take a copy. 
	 * @param ctmc The CTMC
	 * @param t Time point
	 * @param initDist Initial distribution (will be overwritten)
	 */
	public ModelCheckerResult computeTransientProbs(CTMC ctmc, double t, double initDist[]) throws PrismException
	{
		ModelCheckerResult res = null;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[], sum[];
		DTMC dtmc;
		long timer;
		// Fox-Glynn stuff
		FoxGlynn fg;
		int left, right;
		double q, qt, acc, weights[], totalWeight;

		// Start bounded probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting transient probability computation...");

		// Store num states
		n = ctmc.getNumStates();

		// Get uniformisation rate; do Fox-Glynn
		q = ctmc.getDefaultUniformisationRate();
		qt = q * t;
		mainLog.println("\nUniformisation: q.t = " + q + " x " + t + " = " + qt);
		termCritParam = 1e-6;
		acc = termCritParam / 8.0;
		fg = new FoxGlynn(qt, 1e-300, 1e+300, acc);
		left = fg.getLeftTruncationPoint();
		right = fg.getRightTruncationPoint();
		if (right < 0) {
			throw new PrismException("Overflow in Fox-Glynn computation (time bound too big?)");
		}
		weights = fg.getWeights();
		totalWeight = fg.getTotalWeight();
		for (i = left; i <= right; i++) {
			weights[i - left] /= totalWeight;
		}
		mainLog.println("Fox-Glynn (" + acc + "): left = " + left + ", right = " + right);

		// Build (implicit) uniformised DTMC
		dtmc = ctmc.buildImplicitUniformisedDTMC(q);

		// Create solution vector(s)
		// For soln, we just use init (since we are free to modify this vector)
		soln = initDist;
		soln2 = new double[n];
		sum = new double[n];

		// Initialise solution vectors
		// (don't need to do soln2 since will be immediately overwritten)
		for (i = 0; i < n; i++)
			sum[i] = 0.0;

		// If necessary, do 0th element of summation (doesn't require any matrix powers)
		if (left == 0)
			for (i = 0; i < n; i++)
				sum[i] += weights[0] * soln[i];

		// Start iterations
		iters = 1;
		while (iters <= right) {
			// Matrix-vector multiply
			dtmc.vmMult(soln, soln2);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
			// Add to sum
			if (iters >= left) {
				for (i = 0; i < n; i++)
					sum[i] += weights[iters - left] * soln[i];
			}
			iters++;
		}

		// Finished bounded probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Transient probability computation");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = sum;
		res.lastSoln = soln2;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		res.timePre = 0.0;
		return res;
	}

	/**
	 * Simple test program.
	 */
	public static void main(String args[])
	{
		CTMCModelChecker mc;
		CTMCSimple ctmc;
		ModelCheckerResult res;
		BitSet target;
		Map<String, BitSet> labels;
		try {
			mc = new CTMCModelChecker(null);
			ctmc = new CTMCSimple();
			ctmc.buildFromPrismExplicit(args[0]);
			//System.out.println(dtmc);
			labels = mc.loadLabelsFile(args[1]);
			//System.out.println(labels);
			target = labels.get(args[2]);
			if (target == null)
				throw new PrismException("Unknown label \"" + args[2] + "\"");
			for (int i = 4; i < args.length; i++) {
				if (args[i].equals("-nopre"))
					mc.setPrecomp(false);
			}
			res = mc.computeTimeBoundedReachProbs(ctmc, target, Double.parseDouble(args[3]));
			System.out.println(res.soln[0]);
		} catch (PrismException e) {
			System.out.println(e);
		}
	}
}
