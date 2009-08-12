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

package pta;

import java.util.*;

import prism.*;
import explicit.*;

public class PTAExpected
{
	// Prism object
	private Prism prism;
	// Log
	private PrismLog mainLog;
	// PTA and associated info
	protected PTA pta = null;
	protected BitSet targetLocs;
	protected int numClocks;
	// Forwards reachability graph
	protected ReachabilityGraph graph;
	// Initial state(s)
	protected List<Integer> initialStates;
	// Actual target (symbolic states)
	protected BitSet target;

	protected int verbosity = 1;

	/**
	 * Constructor.
	 */
	public PTAExpected(Prism prism)
	{
		this.prism = prism;
		mainLog = prism.getMainLog();
	}

	// Set methods for flags/settings, etc.

	public void setLog(PrismLog mainLog)
	{
		this.mainLog = mainLog;
	}

	/**
	 * Main method: do model checking.
	 */
	public double check(PTA pta, BitSet targetLocs, boolean min) throws PrismException
	{
		// Store PTA/target info
		this.pta = pta;
		this.targetLocs = targetLocs;

		ForwardsReach forwardsReach;
		int src, numStates, i, j;
		long timer;
		int d1, d2;
		Zone z, z1, z2;
		LocZone lz1, lz2;

		numClocks = pta.getNumClocks();
		// Build forwards reachability graph
		forwardsReach = new ForwardsReach(mainLog);
		graph = forwardsReach.buildForwardsGraph(pta, targetLocs, null);
		mainLog.println(pta);
		mainLog.println(graph.states);
		mainLog.println(graph);
		// Extract initial/target states
		initialStates = forwardsReach.getInitialStates();
		target = forwardsReach.getTarget();

		// Compute validities for all symbolic transitions in the graph
		graph.computeAllValidities();

		PTABisimulation ptaBisim = new PTABisimulation(mainLog, verbosity);
		ptaBisim.computeBisimulation(graph, initialStates, target);
		initialStates = ptaBisim.getInitialStates();
		target = ptaBisim.getTarget();

		/*PTAAbstractRefine ptaAR;
		ptaAR = new PTAAbstractRefine();
		String arOptions = prism.getSettings().getString(PrismSettings.PRISM_AR_OPTIONS);
		ptaAR.setLog(mainLog);
		ptaAR.parseOptions(arOptions.split(","));
		ptaAR.forwardsReachAbstractRefine(pta, targetLocs, null, !min);
		graph = ptaAR.graph;
		mainLog.println(graph);
		mainLog.println(graph.states);
		for (LocZone lz : graph.states) {
			mainLog.println(lz + ": "+lz.zone.storageInfo());
		}*/

		if (false) {
			// Go through symbolic transitions
			boolean changed = true;
			SymbolicTransition removeMe = null;
			while (changed) {
				changed = false;
				numStates = graph.states.size();
				for (i = 0; i < numStates; i++) {
					for (SymbolicTransition st : graph.trans.get(i)) {
						//mainLog.println(i + ": " + st);
						lz1 = graph.states.get(i);
						z1 = lz1.zone;
						z2 = st.valid;
						//boolean same = equalMinMaxForZone(z1, z2, min);
						boolean same = z1.equals(z2);
						if (!same) {
							//mainLog.println(st.tr);
							mainLog.println("Diff for:" + i + ": " + lz1);
							mainLog.println("Valid: " + z2);
							mainLog.println(z1.storageInfo());
							mainLog.println(z2.storageInfo());
							printMinMaxForZone(z1, min);
							printMinMaxForZone(z2, min);
							mainLog.println("Splitting " + i + "(st=" + st + ")");
							graph.states.add(new LocZone(lz1.loc, st.valid.deepCopy()));
							int newState = graph.states.size() - 1;
							graph.copyState(i);
							//throw new PrismException("Clock min/max different");
							splitSymbolicTransitionBackwards(i, newState);
							removeMe = st;
							changed = true;
							break;
						}
					}
					if (changed) {
						graph.trans.get(i).remove(removeMe);
						mainLog.println(graph.states);
						mainLog.println(graph);
						break;
					}
				}
			}
		}

		// Build MDP
		MDP mdp = buildMDPWithRewards(graph, initialStates, min);
		mainLog.println(mdp);
		mainLog.println(mdp.getInitialStates());
		MDPModelChecker mdpMc = new MDPModelChecker();
		mdpMc.setLog(mainLog);
		ModelCheckerResult res = mdpMc.expReach(mdp, target, min);
		mainLog.println(res.soln);
		mainLog.println(explicit.Utils.minOverArraySubset(res.soln, mdp.getInitialStates()));
		mainLog.println(explicit.Utils.maxOverArraySubset(res.soln, mdp.getInitialStates()));

		throw new PrismException("Not implemented yet");
	}

	private void splitSymbolicTransitionBackwards(int splitState, int newState)
	{
		int i, numStates;

		// Update symbolic transitions and abstraction
		Set<SymbolicTransition> oldSTs = new LinkedHashSet<SymbolicTransition>();
		Set<SymbolicTransition> newSTs = new LinkedHashSet<SymbolicTransition>();
		// Go through all abstract states
		numStates = graph.states.size();
		for (i = 0; i < numStates; i++) {
			oldSTs.clear();
			newSTs.clear();
			// For a new state...
			if (i == splitState || i == newState) {
				// Split all symbolic transitions from this state
				for (SymbolicTransition st : graph.trans.get(i)) {
					oldSTs.add(st);
					splitSymbolicTransitionBackwards(i, st, splitState, newState, newSTs);
				}
			}
			// For a successor state
			else {
				// Refine symbolic transitions that go to the split state
				for (SymbolicTransition st : graph.trans.get(i)) {
					if (st.hasSuccessor(splitState)) {
						oldSTs.add(st);
						splitSymbolicTransitionBackwards(i, st, splitState, newState, newSTs);
					}
				}
			}
			// Now, actually modify the graph
			// (didn't do this on the fly because don't went to change
			// the list that we are iterating over)
			for (SymbolicTransition st : oldSTs)
				graph.trans.get(i).remove(st);
			for (SymbolicTransition st : newSTs)
				graph.trans.get(i).add(st);
			if ((verbosity >= 1) && !oldSTs.isEmpty()) {
				mainLog.print("Replacing symbolic transitions: " + i + ":" + oldSTs);
				mainLog.println(" with: " + i + ":" + newSTs);
			}
		}
	}

	private void splitSymbolicTransitionBackwards(int src, SymbolicTransition st, int splitState, int newState,
			Set<SymbolicTransition> newSTs)
	{
		// Take a copy of the transition, because we will modify it when analysing it
		SymbolicTransition stNew = new SymbolicTransition(st);
		// Recursively...
		splitSymbolicTransitionBackwards(src, stNew, splitState, newState, newSTs, 0, st.dests.length);
	}

	private void splitSymbolicTransitionBackwards(int src, SymbolicTransition st, int refineState, int newState,
			Set<SymbolicTransition> newSTs, int level, int n)
	{
		if (level == n) {
			Zone valid = graph.computeValidity(src, st.tr, st.dests);
			if (!valid.isEmpty()) {
				SymbolicTransition stNew = new SymbolicTransition(st);
				stNew.valid = valid;
				newSTs.add(stNew);
			}
		} else {
			if (st.dests[level] == refineState) {
				splitSymbolicTransitionBackwards(src, st, refineState, newState, newSTs, level + 1, n);
				st.dests[level] = newState;
				splitSymbolicTransitionBackwards(src, st, refineState, newState, newSTs, level + 1, n);
				st.dests[level] = refineState;
			} else {
				splitSymbolicTransitionBackwards(src, st, refineState, newState, newSTs, level + 1, n);
			}
		}
	}

	/**
	 * Check that minimum (or maximum) value for a clock in a zone.
	 * Note: clocks are indexed from 1.
	 */
	protected int getMinMaxForZone(Zone z, int x, boolean min) throws PrismException
	{
		if (z instanceof DBMList) {
			if (((DBMList) z).list.size() > 1)
				throw new PrismException("Can't compute min/max of non-convex zone");
			z = ((DBMList) z).list.get(0);
		}
		return min ? ((DBM) z).getMin(x) : ((DBM) z).getMax(x);
	}

	/**
	 * Check that minimum (or maximum) clock values are all the same for two zones. 
	 */
	protected boolean equalMinMaxForZone(Zone z1, Zone z2, boolean min) throws PrismException
	{
		for (int i = 1; i < numClocks + 1; i++) {
			if (getMinMaxForZone(z1, i, min) != getMinMaxForZone(z2, i, min))
				return false;
		}
		return true;
	}

	protected void printMinMaxForZone(Zone z, boolean min) throws PrismException
	{
		if (z instanceof DBMList) {
			if (((DBMList) z).list.size() > 1)
				throw new PrismException("Can't compute min/max of non-convex zone");
			z = ((DBMList) z).list.get(0);
		}
		for (int j = 1; j < numClocks + 1; j++) {
			if (j > 1)
				mainLog.print(",");
			mainLog.print(min ? ((DBM) z).getMin(j) : ((DBM) z).getMax(j));
		}
		mainLog.println();
	}

	/**
	 * Build an MDP from a forwards reachability graph, with rewards encoded.
	 */
	protected MDP buildMDPWithRewards(ReachabilityGraph graph, List<Integer> initialStates, boolean min)
			throws PrismException
	{
		Distribution distr;
		int src, count, dest, choice, lzRew, rew, i, j;
		double rewSum;
		long timer;
		MDP mdp;
		int someClock = 1;

		// Building MDP...
		mainLog.println("\nBuilding MDP...");
		timer = System.currentTimeMillis();
		mdp = new MDP();

		// Add all states, including a new initial state
		mdp.addStates(graph.states.size() + 1);

		// For each symbolic state...
		src = 0;
		for (LocZone lz : graph.states) {
			mainLog.println("lz: " + lz);
			// Check there is at least one enabled transition
			// (don't want deadlocks in non-target states)
			if (graph.trans.get(src).size() == 0 && !target.get(src)) {
				throw new PrismException("MDP has deadlock in symbolic state " + lz);
			}
			// And for each outgoing transition in PTA...
			for (SymbolicTransition st : graph.trans.get(src)) {
				// Build distribution
				distr = new Distribution();
				count = -1;
				for (Edge edge : st.tr.getEdges()) {
					count++;
					dest = st.dests[count];
					distr.add(dest, edge.getProbability());
				}
				// Skip if distribution is empty 
				if (distr.isEmpty())
					continue;
				// Add distribution
				choice = mdp.addChoice(src, distr);
				// Compute reward
				if (min) {
					rewSum = 0.0;
					rew = -1;
					DBM dbm = DBM.createFromConstraints(pta, st.tr.getGuardConstraints());
					mainLog.println(dbm);
					for (i = 1; i < numClocks + 1; i++) {
						j = getMinMaxForZone(dbm, i, true);
						j -= getMinMaxForZone(lz.zone, i, true);
						mainLog.println(j);
						if (i == 1 || j > rew)
							rew = j;
						mainLog.println(" " + rew);
					}
					rewSum = rew;
					mainLog.println("# " + rew);
				} else {
					rewSum = 0.0;
					count = -1;
					for (Edge edge : st.tr.getEdges()) {
						count++;
						dest = st.dests[count];
						distr.add(dest, edge.getProbability());
						mainLog.println("lz" + count + ": " + graph.states.get(dest));

						lzRew = -1;
						rew = -1;
						int last = -1;

						for (someClock = 1; someClock < numClocks + 1; someClock++) {

							lzRew = getMinMaxForZone(graph.states.get(dest).zone, someClock, min);
							mainLog.println(lzRew);
							if (lzRew > 10000)
								throw new PrismException("stop");
							LocZone lz2 = lz.deepCopy();
							lz2.dSuc(edge);
							mainLog.println("post: " + lz2);
							rew = getMinMaxForZone(lz2.zone, someClock, min);
							mainLog.println(rew);

							mainLog.println(edge.getProbability() + " * (" + lzRew + "-" + rew + ")");
							mainLog.println("# " + (lzRew - rew));

							if (someClock > 1) {
								if (lzRew - rew != last)
									throw new PrismException("stop");
							}
							last = lzRew - rew;

						}

						rewSum += edge.getProbability() * (lzRew - rew);
						mainLog.println("= " + (edge.getProbability() * (lzRew - rew)));
						mainLog.println("resSum = " + rewSum);
					}
					count++;

				}
				// Set reward
				mdp.setTransitionReward(src, choice, rewSum);
			}
			src++;
		}

		// Create a new initial state to encode incoming reward
		src = mdp.getNumStates() - 1;
		// PTAs only have one initial state but better check...
		if (initialStates.size() != 1)
			throw new PrismException("PTA cannot have multiple initial states");
		// Add transition + reward
		distr = new Distribution();
		distr.add(initialStates.get(0), 1.0);
		mdp.addChoice(src, distr);
		if (!min)
			mdp.setTransitionReward(src, 0, getMinMaxForZone(graph.states.get(initialStates.get(0)).zone, someClock,
					min));
		mdp.addInitialState(src);

		// MDP construction done
		timer = System.currentTimeMillis() - timer;
		mainLog.println("MDP constructed in " + (timer / 1000.0) + " secs.");
		mainLog.println("MDP: " + mdp.infoString());

		return mdp;
	}
}
