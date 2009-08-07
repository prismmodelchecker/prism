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

package abstraction;

import java.util.*;
import explicit.*;

public class AbstractRefine
{
	protected double epsilonRefine = 1e-4; // abs
	protected double epsilonSolve = 1e-6; // rel
	protected double epsilonDouble = 1e-12; // abs

	protected PRISMAbstraction abstr = null;
	protected AbstractMDP amdp = null;
	protected GameModelChecker gmc = null;
	
	// Flags/parameters
	protected boolean optimise;
	protected String refinementMethod;
	
	public boolean phi2[];

	public int totalIters;

	protected int property = -1; // -2 = expected reachability, -1 = probabilistic reachability, >=0 = bounded
									// reachability
	protected int refinementMapping[];
	
	/* Main method - create a class instance and call run() */

	public static void main(String args[])
	{
		new AbstractRefine().run(args);
	}

	/* Run method - do abstraction refinement loop */

	public void run(String args[])
	{
		int i, j;
		boolean done;
		String s;
		long totalTimer;
		long initTime, buildTime, solnTime, refineTime;

		// start timer
		totalTimer = System.currentTimeMillis();

		initTime = 0;
		buildTime = 0;
		solnTime = 0;
		refineTime = 0;
		
		// Locate and construct abstraction class from name in args[0]
		Class<?> abstrClass;
		try {
			abstrClass = Class.forName(args[0].replace("/", "."));
			abstr = (PRISMAbstraction)abstrClass.newInstance();
		} catch (ClassNotFoundException e) {
			System.err.println(e);
			System.exit(1);
		} catch (IllegalAccessException e) {
			System.err.println(e);
			System.exit(1);
		} catch (InstantiationException e) {
			System.err.println(e);
			System.exit(1);
		}
		
		// Configure abstraction class using model details in args[1] 
		s = abstr.modelArgs(args[1]);
		if (s != null) {
			System.out.println(usage());
			System.out.println("where <model_details> are: " + s);
			System.exit(1);
		}
		
		// Create game model checker
		gmc = new GameModelChecker();
		
		// Parse command-line arguments
		args(args);
		
		// Build initial abstract MDP
		amdp = new AbstractMDP();
		amdp.buildAbstractStateSpace(abstr);
		initTime = amdp.getLastTimer();

		i = 0;
		done = false;
		totalIters = 0;

		while (!done) {

			System.out.println("\nBuilding abstract MDP (#" + i + ")...");
			amdp.buildAbstractMDP(abstr);
			buildTime += amdp.getLastTimer();
			buildPhi2();
			System.out.println("\nModel checking (#" + i + ")...");
			gmc.lbStrat = new HashSet[amdp.nAbstract];
			gmc.ubStrat = new HashSet[amdp.nAbstract];
			for (j = 0; j < amdp.nAbstract; j++) {
				gmc.lbStrat[j] = new HashSet();
				gmc.ubStrat[j] = new HashSet();
			}
			if (property == -2) {
				gmc.expectedReachability(amdp, phi2, true, abstr.computeMinExp(), refinementMapping); // min max (default)
				solnTime += gmc.getLastTimer();
				totalIters += gmc.getLastIters();
				gmc.expectedReachability(amdp, phi2, false, abstr.computeMinExp(), refinementMapping); // max max (default)
				solnTime += gmc.getLastTimer();
				totalIters += gmc.getLastIters();
			} else {
				gmc.probabilisticReachability(amdp, phi2, true, abstr.computeMinProbs(), refinementMapping); // min min (default)
				solnTime += gmc.getLastTimer();
				totalIters += gmc.getLastIters();
				gmc.probabilisticReachability(amdp, phi2, false, abstr.computeMinProbs(), refinementMapping); // max min (default)
				solnTime += gmc.getLastTimer();
				totalIters += gmc.getLastIters();
			}
			// System.out.print("lb:"); for (j = 0; j < nAbstract; j++) System.out.print(" "+gmc.lbSoln[j]);
			// System.out.println();
			// System.out.print("ub:"); for (j = 0; j < nAbstract; j++) System.out.print(" "+gmc.ubSoln[j]);
			// System.out.println();
			// System.out.print("gmc.lbStrat:"); for (j = 0; j < nAbstract; j++) System.out.print(" "+gmc.lbStrat[j]);
			// System.out.println();
			// System.out.print("gmc.ubStrat:"); for (j = 0; j < nAbstract; j++) System.out.print(" "+gmc.ubStrat[j]);
			// System.out.println();
			System.out.print("Result interval for initial state: "
					+ abstr.processResult(property, gmc.lbSoln[amdp.initialAbstractState]) + ","
					+ abstr.processResult(property, gmc.ubSoln[amdp.initialAbstractState]));
			System.out.println(" (diff = "
					+ Math.abs(abstr.processResult(property, gmc.lbSoln[amdp.initialAbstractState])
							- abstr.processResult(property, gmc.ubSoln[amdp.initialAbstractState])) + ")");

			if (!veryCloseRel(gmc.ubSoln[amdp.initialAbstractState], gmc.lbSoln[amdp.initialAbstractState], epsilonRefine)) {
				// if (!veryCloseAbs(gmc.ubSoln[amdp.initialAbstractState], gmc.lbSoln[amdp.initialAbstractState],
				// epsilonRefine)) {
				i++;
				System.out.println("\nRefining (#" + i + ")...");
				boolean res = refine(refinementMethod);
				refineTime += gmc.getLastTimer();
				if (!res) {
					done = true;
					i--;
				}
			} else {
				done = true;
			}

		}

		// stop timer
		totalTimer = System.currentTimeMillis() - totalTimer;

		System.out.println("\nTotal of " + i + " refinements");
		System.out.println("\nFinal abstract MDP has " + amdp.nAbstract + " states and " + amdp.nnzAbstract
				+ " transitions");
		System.out.println("\nFinal result interval for initial state: "
				+ abstr.processResult(property, gmc.lbSoln[amdp.initialAbstractState]) + ","
				+ abstr.processResult(property, gmc.ubSoln[amdp.initialAbstractState]));
		System.out.println("\nTotal time for whole process: " + totalTimer / 1000.0 + " seconds.");
		System.out.println("\nTime breakdown: init " + initTime / 1000.0 + " seconds, build " + buildTime / 1000.0
				+ " seconds, soln " + solnTime / 1000.0 + " seconds, refine " + refineTime / 1000.0 + " seconds.");
		System.out.println("\nTotal iterations for whole process: " + totalIters);

		//amdp.printMapping(abstr);
	}

	/* Parse command-line args */

	public void args(String args[])
	{
		if (args.length < 3) {
			System.out.println(usage());
			System.exit(1);
		}
		property = Integer.parseInt(args[2]);
		refinementMethod = (args.length > 3) ? args[3] : null;
		for (int i = 3; i < args.length; i++) {
			if (args[i].equals("-opt"))
				optimise = gmc.optimise = true;
			else if (args[i].equals("-noopt"))
				optimise = gmc.optimise = false;
		}
	}

	/* Usage string */

	public String usage()
	{
		return "Usage: java "+getClass().getName()+" <abstr_class> <model_details> <property>";
	}

	public void buildPhi2()
	{
		int i;
		phi2 = new boolean[amdp.nAbstract];
		for (i = 0; i < amdp.nAbstract; i++)
			phi2[i] = false;
		for (i = 0; i < amdp.nConcrete; i++)
			if (amdp.target[i])
				phi2[amdp.mapping[i]] = true;
	}

	// Perform refinement, return true if refined successfully

	public boolean refine(String refinementMethod)
	{
		if (refinementMethod == null)
			return false;
		if (refinementMethod.equals("10"))
			return refine(1, 0);
		else if (refinementMethod.equals("11"))
			return refine(1, 1);
		else if (refinementMethod.equals("20"))
			return refine(2, 0);
		else if (refinementMethod.equals("21"))
			return refine(2, 1);
		return false;
	}

	// whichStates: 0 = single max diff, 1 = all max diff, 2 = all where diff > 0
	// howSplit: 0 = choose single lower/upper bound strategies, 1 = ...

	public boolean refine(int whichStates, int howSplit)
	{
		HashSet set, splitStates, strategySets, inter, l_minus, u_minus;
		Iterator it1, it2;
		int i, n, r, r_lb, r_ub, newStates = 0;
		double maxDiff;
		Integer ii;
		List list;
		long timer;
		
		// Start timer
		timer = System.currentTimeMillis();

		// If necessary, find find max diff amongst potential split states
		maxDiff = 0.0;
		r = -1;
		if (whichStates <= 1) {
			System.out.print("Potential split states:");
			for (i = 0; i < amdp.nAbstract; i++) {
				if (gmc.ubSoln[i] - gmc.lbSoln[i] < maxDiff)
					continue;
				set = new HashSet();
				set.addAll(gmc.lbStrat[i]);
				set.addAll(gmc.ubStrat[i]);
				if (set.size() > 1) {
					maxDiff = gmc.ubSoln[i] - gmc.lbSoln[i];
					r = i;
					System.out.print(" " + i);
				}
			}
			System.out.println();
			if (r > -1)
				System.out.println("Potential split state max diff: " + maxDiff);
			else {
				System.out.println("No potential split states.");
				return false;
			}
		}

		// Choose states to split
		splitStates = new HashSet();
		switch (whichStates) {
		case 0:
			splitStates.add(r);
			break;
		case 1:
			for (i = 0; i < amdp.nAbstract; i++) {
				// set = new HashSet();
				// set.addAll(gmc.lbStrat[i]);
				// set.addAll(gmc.ubStrat[i]);
				// if (set.size() > 1) splitStates.add(i);

				// If there's only one choice in this player 1 state, can't be split
				if (amdp.mdp.get(i).size() == 1 || !veryCloseAbs(gmc.ubSoln[i] - gmc.lbSoln[i], maxDiff, epsilonDouble)) {
					if (optimise)
						gmc.status[i] = 2;
					continue;
				}

				splitStates.add(i);
			}
			break;
		case 2:
			for (i = 0; i < amdp.nAbstract; i++) {
				// set = new HashSet();
				// set.addAll(gmc.lbStrat[i]);
				// set.addAll(gmc.ubStrat[i]);
				// if (set.size() > 1 && !veryClose(gmc.ubSoln[i], gmc.lbSoln[i], epsilonDouble)) splitStates.add(i);

				// If there's only one choice in this player 1 state, can't be split
				if (amdp.mdp.get(i).size() == 1)
					continue;
				if (veryClose(gmc.ubSoln[i], gmc.lbSoln[i], epsilonDouble)) {
					if (optimise)
						gmc.status[i] = 2;
					continue;
				}

				splitStates.add(i);
			}
			break;
		}
		// System.out.println("Split states: "+splitStates);

		// Split the states; this is done in 2 passes:
		// - one which just counts the number of new abstract states
		// - the second which actually does the split
		strategySets = new HashSet();
		for (int round = 1; round <= 2; round++) {

			// create array to store (backwards) refinement map
			if (round == 2) {
				refinementMapping = new int[amdp.nAbstract + newStates];
				for (i = 0; i < amdp.nAbstract + newStates; i++)
					refinementMapping[i] = i;
			}

			newStates = 0;
			it1 = splitStates.iterator();
			while (it1.hasNext()) {
				// Which state (r) to be split
				r = (Integer) it1.next();
				// Start with empty set of stratedy sets
				strategySets.clear();
				if (howSplit == 0) {
					// Remove interection of lb/ub strategy sets from larger
					// If sets were identical, move one element across to ensure both non-empty
					if (gmc.lbStrat[r].size() > gmc.ubStrat[r].size()) {
						gmc.lbStrat[r].removeAll(gmc.ubStrat[r]);
						if (gmc.lbStrat[r].size() == 0) {
							ii = (Integer) gmc.ubStrat[r].iterator().next();
							gmc.lbStrat[r].add(ii);
							gmc.ubStrat[r].remove(ii);
						}
					} else {
						gmc.ubStrat[r].removeAll(gmc.lbStrat[r]);
						if (gmc.ubStrat[r].size() == 0) {
							ii = (Integer) gmc.lbStrat[r].iterator().next();
							gmc.ubStrat[r].add(ii);
							gmc.lbStrat[r].remove(ii);
						}
					}
					// Pick first of each set
					r_lb = (!gmc.lbStrat[r].isEmpty()) ? (Integer) gmc.lbStrat[r].iterator().next() : -1;
					r_ub = (!gmc.ubStrat[r].isEmpty()) ? (Integer) gmc.ubStrat[r].iterator().next() : -1;
					if (r_ub != -1) {
						set = new HashSet();
						set.add(r_ub);
						strategySets.add(set);
					}
					if (r_lb != -1) {
						set = new HashSet();
						set.add(r_lb);
						strategySets.add(set);
					}
				} else {
					inter = new HashSet();
					l_minus = new HashSet();
					u_minus = new HashSet();
					it2 = gmc.lbStrat[r].iterator();
					while (it2.hasNext()) {
						i = (Integer) it2.next();
						if (gmc.ubStrat[r].contains(i))
							inter.add(i);
						else
							l_minus.add(i);
					}
					it2 = gmc.ubStrat[r].iterator();
					while (it2.hasNext()) {
						i = (Integer) it2.next();
						if (gmc.lbStrat[r].contains(i))
							inter.add(i);
						else
							u_minus.add(i);
					}
					// System.out.println("lb: "+gmc.lbStrat[r]+" ub: "+gmc.ubStrat[r]+" inter: "+inter+"
					// interval="+gmc.lbSoln[r]+"-"+gmc.ubSoln[r]);
					if (inter.size() > 0)
						strategySets.add(inter);
					if (l_minus.size() > 0)
						strategySets.add(l_minus);
					if (u_minus.size() > 0)
						strategySets.add(u_minus);
				}
				// Split (or not)
				if (round == 1) {
					newStates += splitCount(r, strategySets);
				} else {
					newStates += split(r, strategySets);
				}
			}
		}

		// Stop timer
		timer = System.currentTimeMillis() - timer;
		System.out.println("Refinement completed in " + timer / 1000.0 + " seconds (" + newStates + " states added)");

		return true;
	}

	// Return number of new states that would be added on a split

	public int splitCount(int r, HashSet strategySets)
	{
		List state;
		Iterator it1;
		boolean reuse;
		int i, n, count, newStates;

		state = amdp.mdp.get(r);
		it1 = strategySets.iterator();
		n = strategySets.size();
		i = 0;
		count = 0;
		newStates = 0;
		reuse = false;
		while (it1.hasNext()) {
			count += ((HashSet) it1.next()).size();
			if (i == n - 1 && count == state.size())
				reuse = true;
			if (!reuse)
				newStates++;
			i++;
		}
		return newStates;
	}

	// Split a state, return number of new states added

	public int split(int r, HashSet strategySets)
	{
		HashSet set, oneStrategySet;
		List state, states;
		Iterator it1, it2, it3;
		int i, j, k, n, count, newStates;
		boolean reuse;
		List list;

		int r_lb = -1;
		int r_ub = 1;

		// System.out.println("Splitting state "+r+" (strats="+strategySets+")");

		state = amdp.mdp.get(r);
		states = amdp.mdpStates.get(r);

		// Go through each set of strategies (i.e. each new partition of state r)
		it1 = strategySets.iterator();
		n = strategySets.size();
		i = 0;
		count = 0;
		newStates = 0;
		reuse = false;
		while (it1.hasNext()) {
			oneStrategySet = (HashSet) it1.next();
			// Keep running count of number of strategies and, on the last set,
			// work out if we will reuse state r
			count += oneStrategySet.size();
			// System.out.println("# "+i+"/"+n+": "+count+" (="+state.size()+"?)");
			if (i == n - 1 && count == state.size())
				reuse = true;
			if (!reuse) {
				// System.out.print("New state "+amdp.nAbstract+": ");
				refinementMapping[amdp.nAbstract] = r;
				// System.out.print("* "+amdp.nAbstract+" - > "+r);
				newStates++;
				// For each strategy (i.e. element of player 1 state) in the set
				it2 = oneStrategySet.iterator();
				while (it2.hasNext()) {
					j = (Integer) it2.next();
					// For each concrete state corresponding to that strategy
					it3 = ((HashSet) states.get(j)).iterator();
					while (it3.hasNext()) {
						k = ((Integer) it3.next()).intValue();
						// System.out.print(" "+k);
						amdp.mapping[k] = amdp.nAbstract;
						if (k == amdp.initialConcreteState)
							amdp.initialAbstractState = amdp.nAbstract;
					}
				}
				// System.out.println();
				amdp.nAbstract++;
			}
			i++;
		}

		return newStates;
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
