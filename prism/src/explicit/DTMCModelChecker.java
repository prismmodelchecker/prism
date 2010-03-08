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

import java.util.*;

import prism.*;

/**
 * Explicit-state model checker for discrete-time Markov chains (DTMCs).
 */
public class DTMCModelChecker extends ModelChecker
{
	// Model checking functions

	/**
	 * Prob0 precomputation algorithm.
	 */
	public BitSet prob0(DTMC dtmc, BitSet target)
	{
		int i, n, iters;
		boolean b2;
		BitSet u, soln;
		boolean u_done;
		long timer;

		// Start precomputation
		timer = System.currentTimeMillis();
		mainLog.println("Starting Prob0...");

		// Special case: no target states
		if (target.cardinality() == 0) {
			soln = new BitSet(dtmc.getNumStates());
			soln.set(0, dtmc.getNumStates());
			return soln;
		}

		// Initialise vectors
		n = dtmc.getNumStates();
		u = (BitSet) target.clone();
		soln = new BitSet(n);

		// Fixed point loop
		iters = 0;
		u_done = false;
		// Least fixed point - should start from 0 but we optimise by
		// starting from 'target' (see above) thus bypassing first iteration 
		while (!u_done) {
			iters++;
			for (i = 0; i < n; i++) {
				// Need either that i is a target state
				// or some transition goes to u
				b2 = target.get(i) || dtmc.someSuccessorsInSet(i, u);
				soln.set(i, b2);
			}

			// Check termination
			u_done = soln.equals(u);

			// u = soln
			u.clear();
			u.or(soln);
		}

		// Negate
		u.flip(0, n);

		// Finished precomputation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Prob0");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		return u;
	}

	/**
	 * Prob1 precomputation algorithm.
	 */
	public BitSet prob1(DTMC dtmc, BitSet target)
	{
		int i, n, iters;
		boolean b2;
		BitSet u, v, soln;
		boolean u_done, v_done;
		long timer;

		// Start precomputation
		timer = System.currentTimeMillis();
		mainLog.println("Starting Prob1...");

		// Special case: no target states
		if (target.cardinality() == 0) {
			return new BitSet(dtmc.getNumStates());
		}

		// Initialise vectors
		n = dtmc.getNumStates();
		u = new BitSet(n);
		v = new BitSet(n);
		soln = new BitSet(n);

		// Nested fixed point loop
		iters = 0;
		u_done = false;
		// Greatest fixed point
		u.set(0, n);
		while (!u_done) {
			v_done = false;
			// Least fixed point
			v.clear();
			while (!v_done) {
				iters++;
				for (i = 0; i < n; i++) {
					// Need either that i is a target state
					// or all transitions are to u states and some transition goes to v
					b2 = target.get(i) || (dtmc.allSuccessorsInSet(i, u) && dtmc.someSuccessorsInSet(i, v));
					soln.set(i, b2);
				}

				// Check termination (inner)
				v_done = soln.equals(v);

				// v = soln
				v.clear();
				v.or(soln);
			}

			// Check termination (outer)
			u_done = v.equals(u);

			// u = v
			u.clear();
			u.or(v);
		}

		// Finished precomputation
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Prob1");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		return u;
	}

	/**
	 * Compute probabilistic reachability.
	 * @param dtmc: The DTMC
	 * @param target: Target states
	 */
	public ModelCheckerResult probReach(DTMC dtmc, BitSet target) throws PrismException
	{
		return probReach(dtmc, target, null, null);
	}

	/**
	 * Compute probabilistic reachability.
	 * @param dtmc: The DTMC
	 * @param target: Target states
	 * @param init: Optionally, an initial solution vector for value iteration 
	 * @param known: Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	public ModelCheckerResult probReach(DTMC dtmc, BitSet target, double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet no, yes;
		int i, n, numYes, numNo;
		long timer, timerProb0, timerProb1;

		// Start probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("Starting probabilistic reachability...");

		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		dtmc.checkForDeadlocks(target);

		// Store num states
		n = dtmc.getNumStates();

		// Optimise by enlarging target set (if more info is available)
		if (init != null && known != null) {
			BitSet targetNew = new BitSet(n);
			for (i = 0; i < n; i++) {
				targetNew.set(i, target.get(i) || (known.get(i) && init[i] == 1.0));
			}
			target = targetNew;
		}

		// Precomputation
		timerProb0 = System.currentTimeMillis();
		if (precomp && prob0) {
			no = prob0(dtmc, target);
		} else {
			no = new BitSet();
		}
		timerProb0 = System.currentTimeMillis() - timerProb0;
		timerProb1 = System.currentTimeMillis();
		if (precomp && prob1) {
			yes = prob1(dtmc, target);
		} else {
			yes = (BitSet) target.clone();
		}
		timerProb1 = System.currentTimeMillis() - timerProb1;

		// Print results of precomputation
		numYes = yes.cardinality();
		numNo = no.cardinality();
		mainLog.println("yes=" + numYes + ", no=" + numNo + ", maybe=" + (n - (numYes + numNo)));

		// Compute probabilities
		switch (solnMethod) {
		case VALUE_ITERATION:
			res = probReachValIter(dtmc, no, yes, init, known);
			break;
		default:
			throw new PrismException("Unknown DTMC solution method " + solnMethod);
		}

		// Finished probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Probabilistic reachability took " + timer / 1000.0 + " seconds.");

		// Update time taken
		res.timeTaken = timer / 1000.0;
		res.timeProb0 = timerProb0 / 1000.0;
		res.timePre = (timerProb0 + timerProb1) / 1000.0;

		return res;
	}

	/**
	 * Compute probabilistic reachability using value iteration.
	 * @param dtmc: The DTMC
	 * @param no: Probability 0 states
	 * @param yes: Probability 1 states
	 * @param init: Optionally, an initial solution vector for value iteration 
	 * @param known: Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	protected ModelCheckerResult probReachValIter(DTMC dtmc, BitSet no, BitSet yes, double init[], BitSet known)
			throws PrismException
	{
		ModelCheckerResult res;
		BitSet unknown;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[], initVal;
		boolean done;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting value iteration...");

		// Store num states
		n = dtmc.getNumStates();

		// Create solution vector(s)
		soln = new double[n];
		soln2 = (init == null) ? new double[n] : init;

		// Initialise solution vectors. Use (where available) the following in order of preference:
		// (1) exact answer, if already known; (2) 1.0/0.0 if in yes/no; (3) passed in initial value; (4) initVal
		// where initVal is 0.0 or 1.0, depending on whether we converge from below/above. 
		initVal = (valIterDir == ValIterDir.BELOW) ? 0.0 : 1.0;
		if (init != null) {
			if (known != null) {
				for (i = 0; i < n; i++)
					soln[i] = soln2[i] = known.get(i) ? init[i] : yes.get(i) ? 1.0 : no.get(i) ? 0.0 : init[i];
			} else {
				for (i = 0; i < n; i++)
					soln[i] = soln2[i] = yes.get(i) ? 1.0 : no.get(i) ? 0.0 : init[i];
			}
		} else {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = yes.get(i) ? 1.0 : no.get(i) ? 0.0 : initVal;
		}

		// Determine set of states actually need to compute values for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(yes);
		unknown.andNot(no);
		if (known != null)
			unknown.andNot(known);

		// Start iterations
		iters = 0;
		done = false;
		while (!done && iters < maxIters) {
			iters++;
			// Matrix-vector multiply
			dtmc.mvMult(soln, soln2, unknown, false);
			// Check termination
			done = PrismUtils.doublesAreClose(soln, soln2, termCritParam, termCrit == TermCrit.ABSOLUTE);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished value iteration
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Value iteration");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Compute bounded probabilistic reachability.
	 * @param dtmc: The DTMC
	 * @param target: Target states
	 * @param k: Bound
	 * @param init: Initial solution vector - pass null for default
	 * @param results: Optional array of size b+1 to store (init state) results for each step (null if unused)
	 */
	public ModelCheckerResult probReachBounded(DTMC dtmc, BitSet target, int k) throws PrismException
	{
		return probReachBounded(dtmc, target, k, null, null);
	}

	/**
	 * Compute bounded probabilistic reachability.
	 * @param dtmc: The DTMC
	 * @param target: Target states
	 * @param k: Bound
	 * @param init: Initial solution vector - pass null for default
	 * @param results: Optional array of size b+1 to store (init state) results for each step (null if unused)
	 */
	public ModelCheckerResult probReachBounded(DTMC dtmc, BitSet target, int k, double init[], double results[])
			throws PrismException
	{
		ModelCheckerResult res = null;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		long timer;

		// Start bounded probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("Starting bounded probabilistic reachability...");

		// Store num states
		n = dtmc.getNumStates();

		// Create solution vector(s)
		soln = new double[n];
		soln2 = (init == null) ? new double[n] : init;

		// Initialise solution vectors. Use passed in initial vector, if present
		if (init != null) {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = target.get(i) ? 1.0 : init[i];
		} else {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = target.get(i) ? 1.0 : 0.0;
		}
		// Store intermediate results if required
		// (compute min/max value over initial states for first step)
		if (results != null) {
			// TODO: whether this is min or max should be specified somehow
			results[0] = Utils.minMaxOverArraySubset(soln2, dtmc.getInitialStates(), true);
		}

		// Start iterations
		iters = 0;
		while (iters < k) {

			iters++;
			// Matrix-vector multiply
			dtmc.mvMult(soln, soln2, target, true);
			// Store intermediate results if required
			// (compute min/max value over initial states for this step)
			if (results != null) {
				// TODO: whether this is min or max should be specified somehow
				results[iters] = Utils.minMaxOverArraySubset(soln2, dtmc.getInitialStates(), true);
			}
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished bounded probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Bounded probabilistic reachability");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.lastSoln = soln2;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		res.timePre = 0.0;
		return res;
	}

	/**
	 * Compute expected reachability.
	 * @param dtmc: The DTMC
	 * @param target: Target states
	 * @param init: Optionally, an initial solution vector for value iteration 
	 * @param known: Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	public ModelCheckerResult expReach(DTMC dtmc, BitSet target) throws PrismException
	{
		return expReach(dtmc, target, null, null);
	}

	/**
	 * Compute expected reachability.
	 * @param dtmc: The DTMC
	 * @param target: Target states
	 * @param init: Optionally, an initial solution vector for value iteration 
	 * @param known: Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	public ModelCheckerResult expReach(DTMC dtmc, BitSet target, double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet inf;
		int i, n, numTarget, numInf;
		long timer, timerProb1;

		// Start expected reachability
		timer = System.currentTimeMillis();
		mainLog.println("Starting expected reachability...");

		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		dtmc.checkForDeadlocks(target);

		// Store num states
		n = dtmc.getNumStates();

		// Optimise by enlarging target set (if more info is available)
		if (init != null && known != null) {
			BitSet targetNew = new BitSet(n);
			for (i = 0; i < n; i++) {
				targetNew.set(i, target.get(i) || (known.get(i) && init[i] == 0.0));
			}
			target = targetNew;
		}

		// Precomputation (not optional)
		timerProb1 = System.currentTimeMillis();
		inf = prob1(dtmc, target);
		inf.flip(0, n);
		timerProb1 = System.currentTimeMillis() - timerProb1;

		// Print results of precomputation
		numTarget = target.cardinality();
		numInf = inf.cardinality();
		mainLog.println("target=" + numTarget + ", inf=" + numInf + ", rest=" + (n - (numTarget + numInf)));

		// Compute rewards
		switch (solnMethod) {
		case VALUE_ITERATION:
			res = expReachValIter(dtmc, target, inf, init, known);
			break;
		default:
			throw new PrismException("Unknown DTMC solution method " + solnMethod);
		}

		// Finished expected reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Expected reachability took " + timer / 1000.0 + " seconds.");

		// Update time taken
		res.timeTaken = timer / 1000.0;
		res.timePre = timerProb1 / 1000.0;

		return res;
	}

	/**
	 * Compute expected reachability using value iteration.
	 * @param dtmc: The DTMC
	 * @param target: Target states
	 * @param inf: States for which reward is infinite
	 * @param init: Optionally, an initial solution vector for value iteration 
	 * @param known: Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.
	 */
	protected ModelCheckerResult expReachValIter(DTMC dtmc, BitSet target, BitSet inf, double init[], BitSet known)
			throws PrismException
	{
		ModelCheckerResult res;
		BitSet unknown;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		boolean done;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting value iteration...");

		// Store num states
		n = dtmc.getNumStates();

		// Create solution vector(s)
		soln = new double[n];
		soln2 = (init == null) ? new double[n] : init;

		// Initialise solution vectors. Use (where available) the following in order of preference:
		// (1) exact answer, if already known; (2) 0.0/infinity if in target/inf; (3) passed in initial value; (4) 0.0
		if (init != null) {
			if (known != null) {
				for (i = 0; i < n; i++)
					soln[i] = soln2[i] = known.get(i) ? init[i] : target.get(i) ? 0.0
							: inf.get(i) ? Double.POSITIVE_INFINITY : init[i];
			} else {
				for (i = 0; i < n; i++)
					soln[i] = soln2[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : init[i];
			}
		} else {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : 0.0;
		}

		// Determine set of states actually need to compute values for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(target);
		unknown.andNot(inf);
		if (known != null)
			unknown.andNot(known);

		// Start iterations
		iters = 0;
		done = false;
		while (!done && iters < maxIters) {
			//mainLog.println(soln);
			iters++;
			// Matrix-vector multiply
			dtmc.mvMultRew(soln, soln2, unknown, false);
			// Check termination
			done = PrismUtils.doublesAreClose(soln, soln2, termCritParam, termCrit == TermCrit.ABSOLUTE);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished value iteration
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Value iteration");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Simple test program.
	 */
	public static void main(String args[])
	{
		DTMCModelChecker mc;
		DTMCSimple dtmc;
		ModelCheckerResult res;
		BitSet target;
		Map<String, BitSet> labels;
		try {
			mc = new DTMCModelChecker();
			dtmc = new DTMCSimple();
			dtmc.buildFromPrismExplicit(args[0]);
			//System.out.println(dtmc);
			labels = mc.loadLabelsFile(args[1]);
			//System.out.println(labels);
			target = labels.get(args[2]);
			if (target == null)
				throw new PrismException("Unknown label \"" + args[2] + "\"");
			for (int i = 3; i < args.length; i++) {
				if (args[i].equals("-nopre"))
					mc.setPrecomp(false);
			}
			res = mc.probReach(dtmc, target);
			System.out.println(res.soln[0]);
		} catch (PrismException e) {
			System.out.println(e);
		}
	}
}
