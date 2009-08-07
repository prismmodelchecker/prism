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

import java.io.*;
import java.util.*;
import abstraction.*;

public class GameModelChecker
{
	protected double epsilonRefine = 1e-4; // abs
	protected double epsilonSolve = 1e-6; // rel
	protected double epsilonDouble = 1e-12; // abs

	/*
	 * protected int nConcrete; protected int nAbstract; protected int nnzConcrete; protected int nnzAbstract; protected
	 * int mapping[] = null; protected int initialConcreteState; protected int initialAbstractState; protected boolean
	 * target[]; protected boolean phi2[]; protected TreeSet states; protected List statesList; protected ArrayList<List>
	 * mdp; protected ArrayList<List> mdpStates;
	 */

	protected int property = -1; // -2 = expected reachability, -1 = probabilistic reachability, >=0 = bounded
									// reachability

	public double lbSoln[], lbSoln2[];
	public double ubSoln[], ubSoln2[];
	public HashSet lbStrat[], ubStrat[];
	public char status[] = null;
	
	protected long timer;
	protected int lastIters;
	public boolean optimise = true;

	public long getLastTimer()
	{
		return timer;
	}
	
	public long getLastIters()
	{
		return lastIters;
	}
	
	public boolean[] prob1(AbstractMDP amdp, boolean phi2[], boolean min1, boolean min2, int refinementMapping[], int property)
	{
		HashSet distrs;
		Distribution distr;
		List list;
		Iterator it1, it2, it3;
		int i, j, k, n, iters, init;
		boolean b1, b2, b3, b4, b5, u[], v[], soln[];
		boolean u_done, v_done;
		long localTimer;

		// Start timer,
		localTimer = System.currentTimeMillis();

		// Initialise vectors
		u = new boolean[amdp.nAbstract];
		v = new boolean[amdp.nAbstract];
		soln = new boolean[amdp.nAbstract];

		System.out.println("Starting Prob1" + (min2 ? "A" : "E") + "(" + (min1 ? "min" : "max") + ")...");

		iters = 0;
		u_done = false;
		for (i = 0; i < amdp.nAbstract; i++)
			u[i] = true; // greatest fixpoint
		while (iters < 100000) {
			v_done = false;
			for (i = 0; i < amdp.nAbstract; i++)
				v[i] = false; // least fixpoint
			while (!v_done && iters < 100000) {
				iters++;
				for (i = 0; i < amdp.nAbstract; i++) {
					// First see if this state is a target state (in which case, can skip rest of fixpoint function
					// evaluation)
					// For optimised version, reuse info about states which were prob 1 last time
					// (not applicable on last iter (for which u_done=true) since have to build strategy
					if (!u_done) {
						// Optimised version (have to do things differently depending on whether
						// prob1 is being called from probabilistic reachability or expected reachability)
						// Note also: need to make sure this is not the first iter if using last iter
						if (optimise && property == -1) {
							// When computing lower bounds for prob reach (for the first time)
							if (min1 && refinementMapping == null)
								b1 = phi2[i];
							// When computing lower bounds for prob reach (not for the first time)
							else if (min1 && refinementMapping != null)
								b1 = (lbSoln[refinementMapping[i]] == 1.0);
							// When computing upper bounds for prob reach
							else
								b1 = (lbSoln[i] == 1.0);
						} else if (optimise && property == -2) {
							// When computing lower bounds for exp reach (but upper for prob1) (not for the first time)
							// ub(prob)=1 <= lb_last(prob)=1 <= ub_last(exp)<inf
							if (min1 && refinementMapping != null)
								b1 = !Double.isInfinite(ubSoln[refinementMapping[i]]);
							// When computing upper bounds for exp reach (but lower for prob1) (for the first time)
							// lb(prob)=1 <= lb_last(prob)=1 <= ub_last(exp)<inf
							else if (!min1 && refinementMapping != null)
								b1 = !Double.isInfinite(ubSoln[refinementMapping[i]]);
							// Otherwise
							else
								b1 = phi2[i];
						}
						// Non-optimised version - just look at phi2
						else {
							b1 = phi2[i];
						}
					} else
						b1 = phi2[i];
					// If not a target state, do rest of fixpoint function
					// (again, not applicable on last iter (for which u_done=true) since have to build strategy
					if (!b1 || u_done) {
						list = amdp.mdp.get(i);
						it1 = list.iterator();
						j = -1;
						b1 = min1; // there exists or for all player 1 choices
						while (it1.hasNext()) {
							distrs = (HashSet) it1.next();
							j++;
							it2 = distrs.iterator();
							b2 = min2; // there exists or for all player 2 choices
							while (it2.hasNext()) {
								b3 = true; // all transitions are to u states
								b4 = false; // some transition goes to v
								distr = (Distribution) it2.next();
								it3 = distr.iterator();
								while (it3.hasNext()) {
									Map.Entry e = (Map.Entry) it3.next();
									k = (Integer) e.getKey();
									if (!u[k])
										b3 = false;
									if (v[k])
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
							// For a normal iteration, check the there-exists/for-all
							if (!u_done) {
								if (min1) {
									if (!b2)
										b1 = false;
								} else {
									if (b2)
										b1 = true;
								}
							}
							// For the last iteration, store strategy info
							else {
								if (property == -1) {
									if (!min1) {
										// if (b2) { System.out.println("XX: "+i+" "+j); ubStrat[i].add(j);
										// System.out.println(ubStrat[i]); }
										if (b2) {
											ubStrat[i].add(j);
										}
									}
								} else if (property == -2) {
									if (min1) {
										if (!b2) {
											ubStrat[i].add(j);
										}
									}
								}
							}
						}
					}
					if (!u_done)
						soln[i] = b1;
				}

				// Stop the (inner) loop if this is the very last iteration
				if (u_done) {
					break;
				}

				// Check termination (inner)
				v_done = true;
				for (i = 0; i < amdp.nAbstract; i++) {
					if (soln[i] != v[i]) {
						v_done = false;
						break;
					}
				}

				for (i = 0; i < amdp.nAbstract; i++)
					v[i] = soln[i];
			}

			// Stop the (outer) loop if this is the very last iteration
			if (u_done) {
				break;
			}

			// Check termination (outer)
			u_done = true;
			for (i = 0; i < amdp.nAbstract; i++) {
				if (v[i] != u[i]) {
					u_done = false;
					break;
				}
			}

			for (i = 0; i < amdp.nAbstract; i++)
				u[i] = v[i];
		}

		// Stop timer
		localTimer = System.currentTimeMillis() - localTimer;
		System.out.println("Prob1" + (min2 ? "A" : "E") + "(" + (min1 ? "min" : "max") + ") took " + localTimer
				/ 1000.0 + " seconds.");

		return u;
	}

	// Probabilistic reachability (value iteration)
	// min1: true=lower bound, false=upper bound
	// min2: true=minimum probabilities, false = maximum probabilities

	public void probabilisticReachability(AbstractMDP amdp, boolean phi2[], boolean min1, boolean min2, int refinementMapping[])
	{
		HashSet distrs;
		Distribution distr;
		List list;
		Iterator it1, it2, it3;
		Integer ii;
		int i, j, k, n, iters, init;
		double d, prob, soln[], soln2[], tmpsoln[], minmax1, minmax2;
		boolean yes[];
		boolean first1, first2, done;

		// Start timer
		timer = System.currentTimeMillis();

		System.out.println("Starting probabilistic reachability...");

		// Create a new vector to store "status" for each state ("done", "prob=1", etc.)
		// If there is an existing vector (from previous iter), use that to initialise
		// (Only do this when computing lower bound, which is always done before computing upper bound)
		if (optimise) {
			if (min1) {
				char tmpStatus[] = new char[amdp.nAbstract];
				if (status != null)
					for (i = 0; i < amdp.nAbstract; i++)
						tmpStatus[i] = status[refinementMapping[i]];
				else
					for (i = 0; i < amdp.nAbstract; i++)
						tmpStatus[i] = 0;
				status = tmpStatus;
			}
			// System.out.print("Status:"); for (i = 0; i < amdp.nAbstract; i++) System.out.print(" "+(int)status[i]);
			// System.out.println();
		}

		// Precomputation
		if (property == -1)
			yes = prob1(amdp, phi2, min1, min2, refinementMapping, -1);
		else {
			yes = new boolean[amdp.nAbstract];
			for (i = 0; i < amdp.nAbstract; i++)
				yes[i] = phi2[i];
		}

		// Find index of initial state
		// init = statesList.indexOf(getInitialAbstractState());

		// Initialise solution vectors
		// For optimised version, use (where available) the following in order of preference:
		// (1) 1.0 if in prob1/yes, (2) exact answers if known, or (3) lower bound using values from last time if
		// possible (4) 0.0
		soln = new double[amdp.nAbstract];
		soln2 = new double[amdp.nAbstract];
		if (optimise) {
			// When computing upper bounds
			if (!min1)
				for (i = 0; i < amdp.nAbstract; i++)
					soln[i] = soln2[i] = yes[i] ? 1.0 : (status[i] == 2) ? ubSoln[refinementMapping[i]] : lbSoln[i];
			// When computing lower bounds (not for the first time) (here, options (2) and (3) from above coincide)
			else if (min1 && refinementMapping != null)
				for (i = 0; i < amdp.nAbstract; i++)
					soln[i] = soln2[i] = yes[i] ? 1.0 : lbSoln[refinementMapping[i]];
			// When computing lower bounds (for the first time)
			else
				for (i = 0; i < amdp.nAbstract; i++)
					soln[i] = soln2[i] = yes[i] ? 1.0 : 0.0;
		} else
			for (i = 0; i < amdp.nAbstract; i++)
				soln[i] = soln2[i] = yes[i] ? 1.0 : 0.0;

		iters = 0;
		done = false;
		while (iters < 10000000) {
			iters++;
			for (i = 0; i < amdp.nAbstract; i++) {
				// For "yes" states (prob = 1) there is no need to do anything
				// Likewise for states where we already know the exact answer
				if (yes[i] || (optimise && status[i] == 2))
					continue;
				// Otherwise do normal value iteration
				list = amdp.mdp.get(i);
				it1 = list.iterator();
				j = -1;
				minmax1 = 0;
				first1 = true;
				while (it1.hasNext()) {
					distrs = (HashSet) it1.next();
					j++;
					it2 = distrs.iterator();
					minmax2 = 0;
					first2 = true;
					while (it2.hasNext()) {
						d = 0.0;
						distr = (Distribution) it2.next();
						it3 = distr.iterator();
						while (it3.hasNext()) {
							Map.Entry e = (Map.Entry) it3.next();
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
							(min1 ? lbStrat : ubStrat)[i].add(j);
						}
					}
					// if (first1) { minmax1 = minmax2; System.out.println("# "+i+"="+j+"("+minmax1+")"); }
					// else if (veryClose(minmax1, minmax2)) { System.out.println("# "+i+"+="+j+"("+minmax1+")"); }
					// else if ((min1 && minmax2 < minmax1) || (!min1 && minmax2 > minmax1)) { minmax1 = minmax2;
					// System.out.println("# "+i+"="+j+"("+minmax1+")"); }
					first1 = false;
				}
				if (!done)
					soln2[i] = minmax1;
			}

			if (property > -1)
				System.out.print(" " + soln2[amdp.initialAbstractState]);

			// Stop the loop if this is the very last iteration
			if (done) {
				break;
			}

			// System.out.print(iters+": "); for (i = 0; i < amdp.nAbstract; i++) System.out.print(soln2[i]+" ");
			// System.out.println();
			// System.out.print(iters+": "); System.out.println(soln2[0]+" ");
			// if (iters%100==0) System.out.print(iters+" ");
			// if (property > -1) System.out.println("# " + (iters) + " " + (min1?"min":"max") + (min2?"min":"max") + "
			// = " + soln2[init]);

			// Check termination
			// Bounded
			if (property > -1) {
				done = (iters >= property);
			}
			// Unbounded
			else {
				done = true;
				for (i = 0; i < amdp.nAbstract; i++) {
					if (!veryCloseRel(soln2[i], soln[i], epsilonSolve)) {
						// System.out.println("* "+i+":"+soln[i]+","+soln2[i]);
						done = false;
						break;
					}
					// else System.out.println("# "+i+":"+soln[i]+","+soln2[i]);
				}
			}

			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		if (property > -1)
			System.out.println();

		// Print result for initial state
		System.out.println((min1 ? "min" : "max") + (min2 ? "min" : "max") + " = " + soln[amdp.initialAbstractState]
				+ " (" + (iters - 1) + " iters)");

		// for (i = 0; i < amdp.nAbstract; i++) System.out.print(soln[k] + " "); System.out.println();

		// Stop timer
		timer = System.currentTimeMillis() - timer;
		System.out.println("Probabilistic reachability took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Store results
		if (min1) {
			lbSoln = soln;
			lbSoln2 = soln2;
		} else {
			ubSoln = soln;
			ubSoln2 = soln2;
		}
		lastIters = iters;
	}

	// Expected reachability (value iteration)
	// min1: true=lower bound, false=upper bound
	// min2: true=minimum rewards, false = maximum rewards

	public void expectedReachability(AbstractMDP amdp, boolean phi2[], boolean min1, boolean min2, int refinementMapping[])
	{
		HashSet distrs;
		Distribution distr;
		List list;
		Iterator it1, it2, it3;
		Integer ii;
		int i, j, k, n, iters, init;
		double d, prob, soln[], soln2[], tmpsoln[], minmax1, minmax2;
		boolean inf[];
		boolean first1, first2, done;

		// Start timer
		timer = System.currentTimeMillis();

		System.out.println("Starting expected reachability...");

		// Create a new vector to store "status" for each state ("done", "prob=1", etc.)
		// If there is an existing vector (from previous iter), use that to initialise
		// (Only do this when computing lower bound, which is always done before computing upper bound)
		if (optimise) {
			if (min1) {
				char tmpStatus[] = new char[amdp.nAbstract];
				if (status != null)
					for (i = 0; i < amdp.nAbstract; i++)
						tmpStatus[i] = status[refinementMapping[i]];
				else
					for (i = 0; i < amdp.nAbstract; i++)
						tmpStatus[i] = 0;
				status = tmpStatus;
			}
			// System.out.print("Status:"); for (i = 0; i < amdp.nAbstract; i++) System.out.print(" "+(int)status[i]);
			// System.out.println();
		}

		// Precomputation
		inf = prob1(amdp, phi2, !min1, !min2, refinementMapping, -2);
		for (i = 0; i < amdp.nAbstract; i++)
			inf[i] = !inf[i];

		// Find index of initial state
		// init = statesList.indexOf(getInitialAbstractState());

		// Initialise solution vectors
		// For optimised version, use (where available) the following in order of preference:
		// (1) correct answer based on phi2/prob1 (inf if not in prob1, 0 if in phi2)
		// (2) exact answers if known
		// (3) lower bound using values from last time if possible
		// (4) 0.0
		soln = new double[amdp.nAbstract];
		soln2 = new double[amdp.nAbstract];
		if (optimise) {
			// When computing upper bounds
			if (!min1)
				for (i = 0; i < amdp.nAbstract; i++)
					soln[i] = soln2[i] = phi2[i] ? 0.0 : inf[i] ? Double.POSITIVE_INFINITY
							: (status[i] == 2) ? ubSoln[refinementMapping[i]] : lbSoln[i];
			// When computing lower bounds (not for the first time) (here, options (2) and (3) from above coincide)
			else if (min1 && refinementMapping != null)
				for (i = 0; i < amdp.nAbstract; i++)
					soln[i] = soln2[i] = phi2[i] ? 0.0 : inf[i] ? Double.POSITIVE_INFINITY
							: lbSoln[refinementMapping[i]];
			// When computing lower bounds (for the first time)
			else
				for (i = 0; i < amdp.nAbstract; i++)
					soln[i] = soln2[i] = phi2[i] ? 0.0 : inf[i] ? Double.POSITIVE_INFINITY : 0.0;
		} else
			for (i = 0; i < amdp.nAbstract; i++)
				soln[i] = soln2[i] = phi2[i] ? 0.0 : inf[i] ? Double.POSITIVE_INFINITY : 0.0;

		iters = 0;
		done = false;
		while (iters < 10000000) {
			iters++;
			for (i = 0; i < amdp.nAbstract; i++) {
				// For "inf" states (reward = inf) there is no need to do anything
				// Likewise for states where we already know the exact answer
				if (phi2[i] || inf[i] || (optimise && status[i] == 2))
					continue;
				list = amdp.mdp.get(i);
				it1 = list.iterator();
				j = -1;
				minmax1 = 0;
				first1 = true;
				while (it1.hasNext()) {
					distrs = (HashSet) it1.next();
					j++;
					it2 = distrs.iterator();
					minmax2 = 0;
					first2 = true;
					while (it2.hasNext()) {
						d = 1.0;
						distr = (Distribution) it2.next();
						it3 = distr.iterator();
						while (it3.hasNext()) {
							Map.Entry e = (Map.Entry) it3.next();
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
							(min1 ? lbStrat : ubStrat)[i].add(j);
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

			// System.out.print(iters+": "); for (i = 0; i < amdp.nAbstract; i++) System.out.print(soln2[i]+" ");
			// System.out.println();
			// System.out.print(iters+": "); System.out.println(soln2[0]+" ");
			// if (iters%100==0) System.out.print(iters+" ");
			// if (property > -1) System.out.println("# " + (iters) + " " + (min1?"min":"max") + (min2?"min":"max") + "
			// = " + soln2[init]);

			// Check termination
			done = true;
			for (i = 0; i < amdp.nAbstract; i++) {
				if (!veryCloseRel(soln2[i], soln[i], epsilonSolve)) {
					done = false;
					break;
				}
			}

			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Print result for initial state
		System.out.println((min1 ? "min" : "max") + (min2 ? "min" : "max") + " = " + soln[amdp.initialAbstractState]
				+ " (" + (iters - 1) + " iters)");

		// for (i = 0; i < amdp.nAbstract; i++) System.out.print(soln[k] + " "); System.out.println();

		// Stop timer
		timer = System.currentTimeMillis() - timer;
		System.out.println("Expected reachability took " + iters + " iters and " + timer / 1000.0 + " seconds.");

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
		lastIters = iters;
	}

	public static boolean veryClose(double d1, double d2, double epsilon)
	{
		return veryCloseAbs(d1, d2, epsilon);
	}

	public static boolean veryCloseAbs(double d1, double d2, double epsilon)
	{
		if (Double.isInfinite(d1)) {
			if (Double.isInfinite(d2))
				return (d1 > 0) == (d2 > 0);
			else
				return false;
		}
		double diff = Math.abs(d1 - d2);
		return (diff < epsilon);
	}

	public static boolean veryCloseRel(double d1, double d2, double epsilon)
	{
		if (Double.isInfinite(d1)) {
			if (Double.isInfinite(d2))
				return (d1 > 0) == (d2 > 0);
			else
				return false;
		}
		double diff = Math.abs(d1 - d2);
		double min = Math.min(d1, d2);
		if (min != 0)
			return (diff / min < epsilon);
		return (diff < epsilon);
	}


}
