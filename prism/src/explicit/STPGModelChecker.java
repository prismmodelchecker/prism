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
 * Explicit-state model checker for two-player stochastic games (STPGs).
 */
public class STPGModelChecker extends StateModelChecker
{
	// Model checking functions

	/**
	 * Prob0 precomputation algorithm.
	 */
	public BitSet prob0(STPG stpg, BitSet target, boolean min1, boolean min2)
	{
		int i, k, n, iters;
		boolean b1, b2, b3;
		BitSet u, soln;
		boolean u_done;
		long timer;

		// Start precomputation
		timer = System.currentTimeMillis();
		mainLog.println("Starting Prob0 (" + (min1 ? "min" : "max") + (min2 ? "min" : "max") + ")...");

		// Special case: no target states
		if (target.cardinality() == 0) {
			soln = new BitSet(stpg.numStates);
			soln.set(0, stpg.numStates);
			return soln;
		}

		// Initialise vectors
		n = stpg.numStates;
		u = new BitSet(n);
		soln = new BitSet(n);

		// Fixed point loop
		iters = 0;
		u_done = false;
		// Least fixed point
		u.clear();
		while (!u_done) {
			iters++;
			for (i = 0; i < n; i++) {
				// First see if this state is a target state
				// (in which case, can skip rest of fixed point function evaluation)
				b1 = target.get(i);
				if (!b1) {
					b1 = min1; // there exists or for all player 1 choices
					for (DistributionSet distrs : stpg.getChoices(i)) {
						b2 = min2; // there exists or for all player 2 choices
						for (Distribution distr : distrs) {
							b3 = false; // some transition goes to u
							for (Map.Entry<Integer, Double> e : distr) {
								k = (Integer) e.getKey();
								if (u.get(k)) {
									b3 = true;
									continue;
								}
							}
							if (min2) {
								if (!b3)
									b2 = false;
							} else {
								if (b3)
									b2 = true;
							}
						}
						if (min1) {
							if (!b2)
								b1 = false;
						} else {
							if (b2)
								b1 = true;
						}
					}
				}
				soln.set(i, b1);
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
		mainLog.print("Prob0 (" + (min1 ? "min" : "max") + (min2 ? "min" : "max") + ")");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		return u;
	}

	/**
	 * Prob1 precomputation algorithm.
	 */
	public BitSet prob1(STPG stpg, BitSet target, boolean min1, boolean min2)
	{
		int i, k, n, iters;
		boolean b1, b2, b3, b4, b5;
		BitSet u, v, soln;
		boolean u_done, v_done;
		long timer;

		// Start precomputation
		timer = System.currentTimeMillis();
		mainLog.println("Starting Prob1 (" + (min1 ? "min" : "max") + (min2 ? "min" : "max") + ")...");

		// Special case: no target states
		if (target.cardinality() == 0) {
			return new BitSet(stpg.numStates);
		}

		// Initialise vectors
		n = stpg.numStates;
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
					b1 = target.get(i);
					if (!b1) {
						b1 = min1; // there exists or for all player 1 choices
						for (DistributionSet distrs : stpg.getChoices(i)) {
							b2 = min2; // there exists or for all player 2 choices
							for (Distribution distr : distrs) {
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
								if (min2) {
									if (!b5)
										b2 = false;
								} else {
									if (b5)
										b2 = true;
								}
							}
							if (min1) {
								if (!b2)
									b1 = false;
							} else {
								if (b2)
									b1 = true;
							}
						}
					}
					soln.set(i, b1);
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
		mainLog.print("Prob1 (" + (min1 ? "min" : "max") + (min2 ? "min" : "max") + ")");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		return u;
	}

	/**
	 * Compute probabilistic reachability.
	 * @param stpg: The STPG
	 * @param target: Target states
	 * @param min1: Min or max probabilities for player 1 (true=lower bound, false=upper bound)
	 * @param min2: Min or max probabilities for player 2 (true=min, false=max)
	 */
	public ModelCheckerResult probReach(STPG stpg, BitSet target, boolean min1, boolean min2) throws PrismException
	{
		return probReach(stpg, target, min1, min2, null, null);
	}

	/**
	 * Compute probabilistic reachability.
	 * @param stpg: The STPG
	 * @param target: Target states
	 * @param min1: Min or max probabilities for player 1 (true=lower bound, false=upper bound)
	 * @param min2: Min or max probabilities for player 2 (true=min, false=max)
	 * @param init: Optionally, an initial solution vector for value iteration 
	 * @param known: Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	public ModelCheckerResult probReach(STPG stpg, BitSet target, boolean min1, boolean min2, double init[],
			BitSet known) throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet no, yes;
		int i, n, numYes, numNo;
		long timer, timerProb0, timerProb1;

		// Check for some unsupported combinations
		if (solnMethod == SolnMethod.VALUE_ITERATION && valIterDir == ValIterDir.ABOVE && !(precomp && prob0)) {
			throw new PrismException("Precomputation (Prob0) must be enabled for value iteration from above");
		}

		// Start probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("Starting probabilistic reachability...");

		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		stpg.checkForDeadlocks(target);

		// Store num states
		n = stpg.numStates;
		
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
			no = prob0(stpg, target, min1, min2);
		} else {
			no = new BitSet();
		}
		timerProb0 = System.currentTimeMillis() - timerProb0;
		timerProb1 = System.currentTimeMillis();
		if (precomp && prob1) {
			yes = prob1(stpg, target, min1, min2);
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
			res = probReachValIter(stpg, no, yes, min1, min2, init, known);
			break;
		default:
			throw new PrismException("Unknown STPG solution method " + solnMethod);
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
	 * @param stpg: The STPG
	 * @param no: Probability 0 states
	 * @param yes: Probability 1 states
	 * @param min1: Min or max probabilities for player 1 (true=lower bound, false=upper bound)
	 * @param min2: Min or max probabilities for player 2 (true=min, false=max)
	 * @param init: Optionally, an initial solution vector for value iteration 
	 * @param known: Optionally, a set of states for which the exact answer is known
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.  
	 */
	protected ModelCheckerResult probReachValIter(STPG stpg, BitSet no, BitSet yes, boolean min1, boolean min2,
			double init[], BitSet known) throws PrismException
	{
		ModelCheckerResult res = null;
		BitSet unknown;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[], initVal;
		boolean done;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		mainLog.println("Starting value iteration (" + (min1 ? "min" : "max") + (min2 ? "min" : "max") + ")...");

		// Store num states
		n = stpg.numStates;

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
		i = -1;
		done = false;
		while (!done && iters < maxIters) {
			iters++;
			// Matrix-vector multiply and min/max ops
			stpg.mvMultMinMax(soln, min1, min2, soln2, unknown, false);
			// Check termination
			done = PrismUtils.doublesAreClose(soln, soln2, termCritParam, termCrit == TermCrit.ABSOLUTE);
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Print vector (for debugging)
		//mainLog.println(soln);

		// Finished value iteration
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Value iteration (" + (min1 ? "min" : "max") + (min2 ? "min" : "max") + ")");
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
	 * (More precisely, list of indices of player 1 choices resulting in min/max.)
	 * (Note: indices are guaranteed to be sorted in ascending order.)
	 * @param stpg: The STPG
	 * @param state: The state to generate strategy info for
	 * @param target: The set of target states to reach
	 * @param min1: Min or max probabilities for player 1 (true=min, false=max)
	 * @param min:1 Min or max probabilities for player 2 (true=min, false=max)
	 * @param lastSoln: Vector of probabilities from which to recompute in one iteration 
	 */
	public List<Integer> probReachStrategy(STPG stpg, int state, BitSet target, boolean min1, boolean min2,
			double lastSoln[]) throws PrismException
	{
		double val = stpg.mvMultMinMaxSingle(state, lastSoln, min1, min2);
		return stpg.mvMultMinMaxSingleChoices(state, lastSoln, min1, min2, val);
	}

	/**
	 * Compute bounded probabilistic reachability.
	 * @param stpg: The STPG
	 * @param target: Target states
	 * @param b: Bound
	 * @param min1: Min or max probabilities for player 1 (true=lower bound, false=upper bound)
	 * @param min2: Min or max probabilities for player 2 (true=min, false=max)
	 * @param init: Initial solution vector - pass null for default
	 * @param results: Optional array of size b+1 to store (init state) results for each step (null if unused)
	 */
	public ModelCheckerResult probReachBounded(STPG stpg, BitSet target, int b, boolean min1, boolean min2,
			double init[], double results[]) throws PrismException
	{
		ModelCheckerResult res = null;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		long timer;

		// Start bounded probabilistic reachability
		timer = System.currentTimeMillis();
		mainLog.println("Starting bounded probabilistic reachability...");

		// Store num states
		n = stpg.numStates;

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
			results[0] = Utils.minMaxOverArraySubset(soln2, stpg.initialStates, min2);
		}

		// Start iterations
		iters = 0;
		while (iters < b) {
			iters++;
			// Matrix-vector multiply and min/max ops
			stpg.mvMultMinMax(soln, min1, min2, soln2, target, true);
			// Store intermediate results if required
			// (compute min/max value over initial states for this step)
			if (results != null) {
				results[iters] = Utils.minMaxOverArraySubset(soln2, stpg.initialStates, min2);
			}
			// Swap vectors for next iter
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Print vector (for debugging)
		//mainLog.println(soln);

		// Finished bounded probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Bounded probabilistic reachability (" + (min1 ? "min" : "max") + (min2 ? "min" : "max") + ")");
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

	// Expected reachability (value iteration)
	// min1: true=lower bound, false=upper bound
	// min2: true=minimum rewards, false=maximum rewards

	/*public void expReach(STPG stpg, BitSet target, boolean min1, boolean min2, int refinementMapping[])
	{
		int i, j, k, n, iters;
		double d, prob, soln[], soln2[], tmpsoln[], minmax1, minmax2;
		BitSet inf;
		Set<Integer> strat[];
		boolean first1, first2, done;
		long timer;

		// Start value iteration
		timer = System.currentTimeMillis();
		log.println("Starting expected reachability...");

		// Store num states
		n = stpg.numStates;

		strat = null;


		// Precomputation
		inf = prob1(stpg, target, !min1, !min2, null, null);
		inf.flip(0, n);

		// Find index of initial state
		// init = statesList.indexOf(getInitialAbstractState());

		// Initialise solution vectors
		// For optimised version, use (where available) the following in order of preference:
		// (1) correct answer based on target/prob1 (inf if not in prob1, 0 if in target)
		// (2) exact answers if known
		// (3) lower bound using values from last time if possible
		// (4) 0.0
		soln = new double[n];
		soln2 = new double[n];
		if (optimise) {
			// When computing upper bounds
			if (!min1)
				for (i = 0; i < n; i++)
					soln[i] = soln2[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY
							: (status[i] == 2) ? ubSoln[refinementMapping[i]] : lbSoln[i];
			// When computing lower bounds (not for the first time) (here, options (2) and (3) from above coincide)
			else if (min1 && refinementMapping != null)
				for (i = 0; i < n; i++)
					soln[i] = soln2[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY
							: lbSoln[refinementMapping[i]];
			// When computing lower bounds (for the first time)
			else
				for (i = 0; i < n; i++)
					soln[i] = soln2[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : 0.0;
		} else
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : 0.0;

		iters = 0;
		done = false;
		while (iters < maxIters) {
			iters++;
			for (i = 0; i < n; i++) {
				// For "inf" states (reward = inf) there is no need to do anything
				// Likewise for states where we already know the exact answer
				if (target.get(i) || inf.get(i) || (optimise && status[i] == 2))
					continue;
				j = -1;
				minmax1 = 0;
				first1 = true;
				for (DistributionSet distrs : stpg.steps.get(i)) {
					j++;
					minmax2 = 0;
					first2 = true;
					for (Distribution distr : distrs) {
						d = 1.0;
						for (Map.Entry<Integer, Double> e : distr) {
							k = (Integer) e.getKey();
							prob = (Double) e.getValue();
							d += prob * (done ? soln2[k] : soln[k]);
						}
						if (min2) {
							if (first2 | d < minmax2)
								minmax2 = d;
						} else {
							if (first2 | d > minmax2)
								minmax2 = d;
						}
						first2 = false;
					}
					// For a normal iteration, check whether we have exceeded min/max so far
					if (!done) {
						if (first1 || (min1 && minmax2 < minmax1) || (!min1 && minmax2 > minmax1))
							minmax1 = minmax2;
					}
					// For the last iteration, store strategy info
					// (Note that here and also above soln/soln2 have been swapped around - we want to redo previous
					// iteration)
					else {
						if (veryCloseAbs(minmax2, soln[i], epsilonDouble)) {
							strat[i].add(j);
						}
					}
					first1 = false;
				}
				if (!done)
					soln2[i] = minmax1;
			}

			// Stop the loop if this is the very last iteration
			if (done) {
				break;
			}

			// log.print(iters+": "); for (i = 0; i < n; i++) log.print(soln2[i]+" ");
			// log.println();
			// log.print(iters+": "); log.println(soln2[0]+" ");
			// if (iters%100==0) log.print(iters+" ");

			// Check termination
			done = true;
			for (i = 0; i < n; i++) {
				if (!veryCloseRel(soln2[i], soln[i], termCritParam)) {
					done = false;
					break;
				}
			}

			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Print result for initial state
		log.println((min1 ? "min" : "max") + (min2 ? "min" : "max") + " = " + soln[stpg.initialState] + " ("
				+ (iters - 1) + " iters)");

		// for (i = 0; i < n; i++) log.print(soln[k] + " "); log.println();

		// Finished value iteration
		timer = System.currentTimeMillis() - timer;
		log.println("Expected reachability took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Store results
		// if (min1) { lbSoln = soln; lbSoln2 = soln2; lbStrat = strat; }
		// else { ubSoln = soln; ubSoln2 = soln2; ubStrat = strat; }
		// Store results
		if (min1) {
			lbSoln = soln;
			lbSoln2 = soln2;
		} else {
			ubSoln = soln;
			ubSoln2 = soln2;
		}
	}*/


	/**
	 * Simple test program.
	 */
	public static void main(String args[])
	{
		STPGModelChecker mc;
		STPG stpg;
		ModelCheckerResult res;
		BitSet target;
		Map<String, BitSet> labels;
		boolean min1 = true, min2 = true;
		try {
			mc = new STPGModelChecker();
			stpg = new STPG();
			stpg.buildFromPrismExplicit(args[0]);
			//System.out.println(stpg);
			labels = mc.loadLabelsFile(args[1]);
			//System.out.println(labels);
			target = labels.get(args[2]);
			if (target == null)
				throw new PrismException("Unknown label \"" + args[2] + "\"");
			for (int i  =3; i < args.length; i++) {
				if (args[i].equals("-minmin")) {
					min1 =true;
					min2 = true;
				} else if (args[i].equals("-maxmin")) {
					min1 =false;
					min2 = true;
				} else if (args[i].equals("-minmax")) {
					min1 =true;
					min2 = false;
				} else if (args[i].equals("-maxmax")) {
					min1 =false;
					min2 = false;
				}
			}
			//stpg.exportToDotFile("stpg.dot", target);
			//stpg.exportToPrismExplicit("stpg");
			res = mc.probReach(stpg, target, min1, min2);
			System.out.println(res.soln[0]);
		} catch (PrismException e) {
			System.out.println(e);
		}
	}
}
