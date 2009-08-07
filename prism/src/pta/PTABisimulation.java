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

public class PTABisimulation
{
	// Log
	private PrismLog mainLog;
	// Forwards reachability graph
	protected ReachabilityGraph graph;
	// Initial state(s)
	public List<Integer> initialStates;
	// Actual target (symbolic states)
	public BitSet target;

	protected int verbosity;

	// Constructors
	
	public PTABisimulation(PrismLog mainLog, int verbosity)
	{
		this.mainLog = mainLog;
		this.verbosity = verbosity;
	}

	// Set methods for flags/settings, etc.

	public void setLog(PrismLog mainLog)
	{
		this.mainLog = mainLog;
	}

	// Accessors for other info generated during bisimulation

	public BitSet getTarget()
	{
		return target;
	}

	public List<Integer> getInitialStates()
	{
		return initialStates;
	}

	/**
	 * Compute a probabilistic time-abstracting bisimulation from a PTA and target set. 
	 * @param pta: The PTA
	 * @param targetLoc: The set of target locations
	 */
	public MDP computeBisimulation(PTA pta, BitSet targetLocs) throws PrismException
	{
		ForwardsReach forwardsReach;
		MDP mdp;

		// Build forwards reachability graph
		forwardsReach = new ForwardsReach(mainLog);
		graph = forwardsReach.buildForwardsGraph(pta, targetLocs, null);
		
		// Extract initial/target states
		initialStates = forwardsReach.getInitialStates();
		target = forwardsReach.getTarget();
		
		// Display states, graph, etc.
		if (verbosity >= 5) {
			mainLog.println("\nStates: ");
			graph.printStates(mainLog);
			mainLog.println("\nGraph: " + graph);
			mainLog.println("Target states: " + target);
		}

		// Do minimisation
		computeBisimulation(graph, initialStates, target);

		mainLog.println("New graph: " + graph.states.size() + " symbolic states, " + target.cardinality()
				+ " target states");
		HashSet<LocZone> uniq = new HashSet<LocZone>();
		uniq.addAll(graph.states);
		mainLog.println(uniq.size() + " unique");
		
		// Display states, graph, etc.
		if (verbosity >= 5) {
			mainLog.println("\nNew states: ");
			graph.printStates(mainLog);
			mainLog.println("\nNew graph: " + graph);
		}
		
		mainLog.println("\nBuilding MDP...");
		mdp = graph.buildMDP(initialStates);
		mainLog.println("MDP: " + mdp.infoString());
		
		return mdp;
	}
	
	/**
	 * Compute a probabilistic time-abstracting bisimulation from a reachability graph.
	 * TODO: modifies graph, but not others ??? 
	 * @param graph
	 * @param initialStates
	 * @param target
	 */
	public void computeBisimulation(ReachabilityGraph graph, List<Integer> initialStates, BitSet target) throws PrismException
	{
		int i;
		long timer, timerProgress;
		boolean progressDisplayed;
		
		// Store pointer to graph (used in other methods below) 
		this.graph = graph;
		
		// Take copies of initial/target states (will be modified)
		this.initialStates = new ArrayList<Integer>();
		this.initialStates.addAll(initialStates);
		this.target = (BitSet) target.clone();
		
		// Starting bisimulation...
		mainLog.println("\nComputing time-abstracting bisimulation...");
		timer = timerProgress = System.currentTimeMillis();
		progressDisplayed = false;
		
		// Compute validities for all symbolic transitions in the graph
		graph.computeAllValidities();
		
		// Go through symbolic transitions
		boolean changed = true;
		while (changed) {
			changed = false;
			for (i = 0; i < graph.states.size(); i++) {
				if (splitState(i) > 1) {
					changed = true;
				}
				// Print some progress info occasionally
				if (System.currentTimeMillis() - timerProgress > 3000) {
					if (!progressDisplayed) {
						mainLog.print("Number of states so far:");
						progressDisplayed = true;
					}
					mainLog.print(" " + graph.states.size());
					mainLog.flush();
					timerProgress = System.currentTimeMillis();
				}
			}
		}
		
		// Tidy up progress display
		if (progressDisplayed)
			mainLog.println(" " + graph.states.size());

		// Bisimulation complete
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Bisimulation completed in " + (timer / 1000.0) + " secs.");
	}

	protected int splitState(int splitState) throws PrismException
	{
		LocZone lzSplit;
		int[] newStateMap, map;
		int i, numTransitions, numStates, numNewStates;
		ArrayList<DBM> partition;
		ArrayList<SymbolicTransition> sts, stsNew;
		boolean rebuild;
		DBM dbm;
		
		// Get abstract state to split 
		lzSplit = graph.states.get(splitState);
		if (verbosity >= 1)
			mainLog.println("Splitting: #" + splitState + "=" + lzSplit);

		// Get symbolic transition list for this state
		sts = graph.trans.get(splitState);
		
		// Get validity of each outgoing transition from this state.
		// In fact, since guards for multiple transitions are often identical,
		// we only do this for distinct ones, and build a 'map' between them.
		partition = new ArrayList<DBM>();
		numTransitions = sts.size();
		map = new int[numTransitions];
		for (i = 0; i < numTransitions; i++) {
			dbm = (DBM)((DBMList) sts.get(i).valid).list.get(0);
			// See if we already have a copy of this zone z
			map[i] = partition.indexOf(dbm);
			// If not, add to list and store index in map
			if (map[i] == -1) {
				map[i] = partition.size();
				partition.add(dbm);
			}
		}
		
		// Check we actually got a strict split of the partition
		if (partition.size() <= 1) {
			//mainLog.println("Warning: failed to split state #" + splitState + "=" + lzSplit);
			return 1;
		}

		// Update symbolic state set info (graph.states) and store info about indices of new states
		numNewStates = partition.size();
		newStateMap = new int[numNewStates];
		for (i = 0; i < numNewStates; i++) {
			// First new state overwrites one that is being split
			if (i == 0) {
				graph.states.set(splitState, new LocZone(lzSplit.loc, partition.get(i)));
				newStateMap[i] = splitState;
				graph.trans.set(splitState, stsNew = new ArrayList<SymbolicTransition>());
			}
			// Other new states are appended to end of list
			else {
				graph.states.add(new LocZone(lzSplit.loc, partition.get(i)));
				newStateMap[i] = graph.states.size() - 1;
				graph.trans.add(stsNew = new ArrayList<SymbolicTransition>());
			}
		}
		for (i = 0; i < numTransitions; i++) {
			stsNew = graph.trans.get(newStateMap[map[i]]);
			stsNew.add(sts.get(i));
		}

		// Display info
		if (verbosity >= 1) {
			mainLog.println("Splitting: #" + splitState + "=" + lzSplit);
			mainLog.println("into " + numNewStates + ":");
			for (i = 0; i < numNewStates; i++)
				mainLog.println("#" + newStateMap[i] + "=" + partition.get(i));
		}
		if (verbosity >= 5)
			mainLog.println("New states: " + graph.states);

		// Add new states to initial state set if needed
		// Note: we assume any abstract state contains either all/no initial states
		if (initialStates.contains(splitState)) {
			for (i = 1; i < numNewStates; i++) {
				initialStates.add(newStateMap[i]);
			}
		}

		// Rebuild target states
		if (target.get(splitState)) {
			for (i = 1; i < numNewStates; i++) {
				target.set(newStateMap[i]);
			}
		}

		
		// Update symbolic transitions and abstraction
		Set<SymbolicTransition> oldSTs = new LinkedHashSet<SymbolicTransition>();
		Set<SymbolicTransition> newSTs = new LinkedHashSet<SymbolicTransition>();
		// Go through all abstract states
		numStates = graph.states.size();
		for (i = 0; i < numStates; i++) {
			oldSTs.clear();
			newSTs.clear();
			// Do we need to rebuild this state?
			// (i.e. is it a new state or a successor of the split state?)
			rebuild = false;
			// For a new state...
			if (i == splitState || i > numStates - numNewStates) {
				// Split all symbolic transitions from this state
				for (SymbolicTransition st : graph.trans.get(i)) {
					oldSTs.add(st);
					splitSymbolicTransition(i, st, splitState, newStateMap, newSTs);
				}
				rebuild = true;
			}
			// For a successor state
			else {
				// Split symbolic transitions that go to the split state
				for (SymbolicTransition st : graph.trans.get(i)) {
					if (st.hasSuccessor(splitState)) {
						oldSTs.add(st);
						splitSymbolicTransition(i, st, splitState, newStateMap, newSTs);
						rebuild = true;
					}
				}
			}
			// Bail out, if we didn't need to rebuild for this state
			if (!rebuild)
				continue;
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

		if (verbosity >= 5) {
			mainLog.println("New graph: " + graph);
		}

		return numNewStates;
	}

	/**
	 * Split a symbolic transition, based on the division of a symbolic state into several parts.
	 * @param src: The source of the symbolic transition to be split 
	 * @param st: The symbolic transition to be split
	 * @param splitState: The index of the symbolic state that is being split 
	 * @param newStateMap: The indices of the new states
	 * @param newSTs: Where to put the new symbolic transitions
	 */
	private void splitSymbolicTransition(int src, SymbolicTransition st, int splitState, int newStateMap[],
			Set<SymbolicTransition> newSTs)
	{
		// Take a copy of the transition, because we will modify it when analysing it
		SymbolicTransition stNew = new SymbolicTransition(st);
		// Recursively...
		splitSymbolicTransition(src, stNew, splitState, newStateMap, newSTs, 0, st.dests.length);
	}

	private void splitSymbolicTransition(int src, SymbolicTransition st, int splitState, int newStateMap[],
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
			if (st.dests[level] == splitState) {
				int m = newStateMap.length;
				for (int i = 0; i < m; i++) {
					st.dests[level] = newStateMap[i];
					splitSymbolicTransition(src, st, splitState, newStateMap, newSTs, level + 1, n);
				}
				st.dests[level] = splitState;
			} else {
				splitSymbolicTransition(src, st, splitState, newStateMap, newSTs, level + 1, n);
			}
		}
	}
}
