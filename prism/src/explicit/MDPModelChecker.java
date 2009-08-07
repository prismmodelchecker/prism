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
 * Explicit-state model checker for Markov decision processes (MDPs).
 */
public class MDPModelChecker extends ModelChecker
{
	// Model checking functions

	/**
	 * Prob1 precomputation algorithm.
	 */
	public BitSet prob1(MDP mdp, BitSet target, boolean min)
	{
		int i, k, n, iters;
		boolean b2, b3, b4, b5;
		BitSet u, v, soln;
		boolean u_done, v_done;
		long timer;

		// Start precomputation
		timer = System.currentTimeMillis();
		mainLog.println("Starting Prob1 (" + (min ? "min" : "max") + ")...");

		// Special case: no target states
		if (target.cardinality() == 0) {
			return new BitSet(mdp.numStates);
		}

		// Initialise vectors
		n = mdp.numStates;
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
					// First see if this state is a target state
					// (in which case, can skip rest of fixed point function evaluation)
					b2 = target.get(i);
					if (!b2) {
						b2 = min; // there exists or for all choices
						for (Distribution distr : mdp.steps.get(i)) {
							b3 = true; // all transitions are to u states
							b4 = false; // some transition goes to v
							for (Map.Entry<Integer, Double> e : distr) {
								k = (Integer) e.getKey();
								if (!u.get(k))
									b3 = false;
								if (v.get(k))
									b4 = true;
							}
							b5 = (b3 && b4);
							if (min) {
								if (!b5)
									b2 = false;
							} else {
								if (b5)
									b2 = true;
							}
						}
					}
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
		mainLog.print("Prob1 (" + (min ? "min" : "max") + ")");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		return u;
	}

	/**
	 * Compute probabilistic reachability.
	 * @param mdp: The MDP
	 * @param target: Target states
	 * @param min: Min or max probabilities (true=min, false=max)
	 */
	public ModelCheckerResult probReach(MDP mdp, BitSet target, boolean min) throws PrismException
	{
		return probReach(mdp, target, min, null, null);
	}

	/**
	 * Compute probabilistic reachability.
	 * @param mdp: The MDP
	 * @param target: Target states
	 * @param min: Min or max probabilities (true=min, false=max)
	 * @param: init: Optionally, an initial solution vector for value iteration 
	 * @param: known: Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	public ModelCheckerResult probReach(MDP mdp, BitSet target, boolean min, double init[], BitSet known)
			throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet no, yes;
		int n, numYes, numNo;
		long timer, timerProb0, timerProb1;

		// Start probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("Starting probabilistic reachability...");

		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		mdp.checkForDeadlocks(target);

		// Store num states
		n = mdp.numStates;

		// Recompute target (if we have more info available)
		// TODO

		// Precomputation
		//TODO: prob0
		timerProb0 = System.currentTimeMillis();
		timerProb0 = System.currentTimeMillis() - timerProb0;
		timerProb1 = System.currentTimeMillis();
		no = new BitSet();
		if (precomp && prob1) {
			yes = prob1(mdp, target, min);
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
			res = probReachValIter(mdp, no, yes, min, init, known);
			break;
		default:
			throw new PrismException("Unknown MDP solution method " + solnMethod);
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
	 * @param mdp: The MDP
	 * @param no: Probability 0 states
	 * @param yes: Probability 1 states
	 * @param min: Min or max probabilities (true=min, false=max)
	 * @param: init: Optionally, an initial solution vector for value iteration 
	 * @param: known: Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	protected ModelCheckerResult probReachValIter(MDP mdp, BitSet no, BitSet yes, boolean min, double init[],
			BitSet known) throws PrismException
	{
		ModelCheckerResult res;
		BitSet unknown;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[], initVal;
		boolean done;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting value iteration (" + (min ? "min" : "max") + ")...");

		// Store num states
		n = mdp.numStates;

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
			// Matrix-vector multiply and min/max ops
			mdp.mvMultMinMax(soln, min, soln2, unknown, false);
			// Check termination
			done = PrismUtils.doublesAreClose(soln, soln2, termCritParam, termCrit == TermCrit.ABSOLUTE);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished value iteration
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Value iteration (" + (min ? "min" : "max") + ")");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Construct strategy information for min/max reachability probabilities.
	 * (More precisely, list of indices of choices resulting in min/max.)
	 * (Note: indices are guaranteed to be sorted in ascending order.)
	 * @param mdp: The MDP
	 * @param state: The state to generate strategy info for
	 * @param target: The set of target states to reach
	 * @param min: Min or max probabilities (true=min, false=max)
	 * @param lastSoln: Vector of values from which to recompute in one iteration 
	 */
	public List<Integer> probReachStrategy(MDP mdp, int state, BitSet target, boolean min, double lastSoln[])
			throws PrismException
	{
		double val = mdp.mvMultMinMaxSingle(state, lastSoln, min);
		return mdp.mvMultMinMaxSingleChoices(state, lastSoln, min, val);
	}

	/**
	 * Compute bounded probabilistic reachability.
	 * @param mdp: The MDP
	 * @param target: Target states
	 * @param k: Bound
	 * @param min: Min or max probabilities for (true=min, false=max)
	 * @param init: Initial solution vector - pass null for default
	 * @param results: Optional array of size b+1 to store (init state) results for each step (null if unused)
	 */
	public ModelCheckerResult probReachBounded(MDP mdp, BitSet target, int k, boolean min, double init[],
			double results[]) throws PrismException
	{
		ModelCheckerResult res = null;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		long timer;

		// Start bounded probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("Starting bounded probabilistic reachability...");

		// Store num states
		n = mdp.numStates;

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
			results[0] = Utils.minMaxOverArraySubset(soln2, mdp.getInitialStates(), true);
		}

		// Start iterations
		iters = 0;
		while (iters < k) {

			// Print vector (for debugging)
			mainLog.println(soln);

			iters++;
			// Matrix-vector multiply and min/max ops
			mdp.mvMultMinMax(soln, min, soln2, target, true);
			// Store intermediate results if required
			// (compute min/max value over initial states for this step)
			if (results != null) {
				// TODO: whether this is min or max should be specified somehow
				results[iters] = Utils.minMaxOverArraySubset(soln2, mdp.getInitialStates(), true);
			}
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Print vector (for debugging)
		mainLog.println(soln);

		// Finished bounded probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Probabilistic bounded reachability (" + (min ? "min" : "max") + ")");
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
	 * @param mdp: The MDP
	 * @param target: Target states
	 * @param min: Min or max rewards (true=min, false=max)
	 * @param: init: Optionally, an initial solution vector for value iteration 
	 * @param: known: Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	public ModelCheckerResult expReach(MDP mdp, BitSet target, boolean min) throws PrismException
	{
		return expReach(mdp, target, min, null, null);
	}

	/**
	 * Compute expected reachability.
	 * @param mdp: The MDP
	 * @param target: Target states
	 * @param min: Min or max rewards (true=min, false=max)
	 * @param: init: Optionally, an initial solution vector for value iteration 
	 * @param: known: Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	public ModelCheckerResult expReach(MDP mdp, BitSet target, boolean min, double init[], BitSet known)
			throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet inf;
		int n, numTarget, numInf;
		long timer, timerProb1;

		// Start expected reachability
		timer = System.currentTimeMillis();
		mainLog.println("Starting expected reachability...");

		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		mdp.checkForDeadlocks(target);

		// Store num states
		n = mdp.numStates;

		// Recompute target (if we have more info available)
		// TODO ?

		// Precomputation (not optional)
		timerProb1 = System.currentTimeMillis();
		inf = prob1(mdp, target, !min);
		inf.flip(0, n);
		timerProb1 = System.currentTimeMillis() - timerProb1;

		// Print results of precomputation
		numTarget = target.cardinality();
		numInf = inf.cardinality();
		mainLog.println("target=" + numTarget + ", inf=" + numInf + ", rest=" + (n - (numTarget + numInf)));

		// Compute rewards
		switch (solnMethod) {
		case VALUE_ITERATION:
			res = expReachValIter(mdp, target, inf, min, init, known);
			break;
		default:
			throw new PrismException("Unknown MDP solution method " + solnMethod);
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
	 * @param mdp: The MDP
	 * @param target: Target states
	 * @param inf: States for which reward is infinite
	 * @param min: Min or max rewards (true=min, false=max)
	 * @param: init: Optionally, an initial solution vector for value iteration 
	 * @param: known: Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.
	 */
	protected ModelCheckerResult expReachValIter(MDP mdp, BitSet target, BitSet inf, boolean min, double init[],
			BitSet known) throws PrismException
	{
		ModelCheckerResult res;
		BitSet unknown;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		boolean done;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting value iteration (" + (min ? "min" : "max") + ")...");

		// Store num states
		n = mdp.numStates;

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
			// Matrix-vector multiply and min/max ops
			mdp.mvMultRewMinMax(soln, min, soln2, unknown, false);
			// Check termination
			done = PrismUtils.doublesAreClose(soln, soln2, termCritParam, termCrit == TermCrit.ABSOLUTE);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished value iteration
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Value iteration (" + (min ? "min" : "max") + ")");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		return res;
	}

	/**
	 * Construct strategy information for min/max expected reachability.
	 * (More precisely, list of indices of choices resulting in min/max.)
	 * (Note: indices are guaranteed to be sorted in ascending order.)
	 * @param mdp: The MDP
	 * @param state: The state to generate strategy info for
	 * @param target: The set of target states to reach
	 * @param min: Min or max rewards (true=min, false=max)
	 * @param lastSoln: Vector of values from which to recompute in one iteration 
	 */
	public List<Integer> expReachStrategy(MDP mdp, int state, BitSet target, boolean min, double lastSoln[])
			throws PrismException
	{
		double val = mdp.mvMultRewMinMaxSingle(state, lastSoln, min);
		return mdp.mvMultRewMinMaxSingleChoices(state, lastSoln, min, val);
	}

	/**
	 * Simple test program.
	 */
	public static void main(String args[])
	{
		MDPModelChecker mc;
		MDP mdp;
		ModelCheckerResult res;
		BitSet target;
		Map<String, BitSet> labels;
		try {
			mc = new MDPModelChecker();
			mdp = new MDP();
			mdp.buildFromPrismExplicit(args[0]);
			//System.out.println(mdp);
			labels = mc.loadLabelsFile(args[1]);
			//System.out.println(labels);
			target = labels.get(args[2]);
			if (target == null)
				throw new PrismException("Unknown label \"" + args[2] + "\"");
			res = mc.probReach(mdp, target, true);
			System.out.println(res.soln[0]);
		} catch (PrismException e) {
			System.out.println(e);
		}
	}
}
