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

public class ForwardsReach
{
	// Log for output
	protected PrismLog mainLog;

	// PTA target info
	protected BitSet targetLocs;
	protected Constraint targetConstraint;

	// Extra information associated with reachability graph
	protected BitSet target; // Bit set specifying target states
	protected List<Integer> initialStates; // Initial states

	// Constructors

	public ForwardsReach()
	{
		this(new PrismPrintStreamLog(System.out));
	}

	public ForwardsReach(PrismLog log)
	{
		this.mainLog = log;
	}

	// Accessors for other info generated during construction of reachability graph

	public BitSet getTarget()
	{
		return target;
	}

	public List<Integer> getInitialStates()
	{
		return initialStates;
	}

	/**
	 * Build a reachability graph based on a forwards PTA traversal.
	 * All zones in the symbolic states in the graph are guaranteed to be convex (i.e. DBMs).
	 * The set of target states found and the (singleton) set of initial states are stored separately
	 * and can be obtained subsequently using getTarget() and getInitialStates().
	 */
	public ReachabilityGraph buildForwardsGraph(PTA pta, BitSet targetLocs, Constraint targetConstraint)
			throws PrismException
	{
		return buildForwardsGraphFormats10(pta, targetLocs, targetConstraint);
		//return buildForwardsGraphFormats09(pta, targetLocs, targetConstraint);
	}
	
	/**
	 * Implementation of {@link #buildForwardsGraph} using FORMATS'10 definition.
	 */
	private ReachabilityGraph buildForwardsGraphFormats10(PTA pta, BitSet targetLocs, Constraint targetConstraint)
	throws PrismException
	{
		LocZone init, lz, lz2;
		LinkedList<LocZone> X;
		StateStorage<LocZone> Yset;
		//LocZoneSetOld Zset;
		ReachabilityGraph graph;
		int src, dest, count, dests[];
		boolean canDiverge;
		long timer;

		// Store target info
		this.targetLocs = targetLocs;
		this.targetConstraint = targetConstraint;

		// Starting reachability...
		mainLog.print("\nBuilding forwards reachability graph...");
		mainLog.flush();
		ProgressDisplay progress = new ProgressDisplay(mainLog);
		progress.start();
		timer = System.currentTimeMillis();

		// Re-compute max clock constraint value if required
		if (targetConstraint != null)
			pta.recomputeMaxClockConstraint(targetConstraint);
		
		// Initialise data structures
		graph = new ReachabilityGraph(pta);
		Yset = new IndexedSet<LocZone>();
		X = new LinkedList<LocZone>();
		target = new BitSet();

		// Build initial symbolic state (NB: assume initial location = 0)
		init = new LocZone(0, DBM.createZero(pta));

		// Reachability loop
		Yset.add(init);
		X.add(init);
		src = -1;
		// While there are unexplored symbolic states (in X)...
		while (!X.isEmpty()) {
			// Pick next state to explore
			// X is a list containing states in order found
			// (so we know index of lz is src+1)
			lz = X.removeFirst();
			src++;
			// Compute timed post for this zone (NB: do this before checking if target)
			lz = lz.deepCopy();
			lz.tPost(pta);
			// Is this a target state? (If so, don't explore)
			if (targetLocs.get(lz.loc) && (targetConstraint == null || lz.zone.isSatisfied(targetConstraint))) {
				target.set(src);
				// Add null for this state (no need to store info)
				graph.addState();
				continue;
			}
			// Check if time can diverge in this state
			// (note we already did tPost above)
			canDiverge = lz.zone.allClocksAreUnbounded();
			// Explore this symbolic state
			// First, check for one possible cause of timelock:
			// no PTA transitions and not possible for time to diverge
			if (!canDiverge && pta.getTransitions(lz.loc).size() == 0) {
				throw new PrismException("Timelock (no transitions) in PTA at location " + pta.getLocationNameString(lz.loc));
			}
			// Add current state to reachability graph
			graph.addState();
			// For unbounded case, add a special self-loop transition to model divergence
			if (canDiverge) {
				dests = new int[1];
				dests[0] = src;
				Transition trNew = new Transition(pta, lz.loc, "_diverge");
				trNew.addEdge(1.0, lz.loc);
				graph.addTransition(src, trNew, dests, null);
			}
			// And for the non-unbounded case, need to do a check for time-locks
			else {
				Zone zone;
				NCZone ncZone;
				// Build union of tPre of each guard
				ncZone = DBMList.createFalse(pta);
				for (Transition transition : pta.getTransitions(lz.loc)) {
					zone = DBM.createFromConstraints(pta, transition.getGuardConstraints());
					zone.down();
					ncZone.union(zone);
				}
				// Make sure tPost of this zone is not bigger (tPost done above)
				// (i.e. intersection with complement of union is empty)
				ncZone.complement();
				ncZone.intersect(lz.zone);
				if (!ncZone.isEmpty()) {
					String s = "Timelock in PTA at location " + pta.getLocationNameString(lz.loc);
					s += " when " + ncZone.getAZone();
					throw new PrismException(s);
				}
			}
			// For each outgoing transition...
			for (Transition transition : pta.getTransitions(lz.loc)) {
				dests = new int[transition.getNumEdges()];
				boolean enabled = false;
				boolean unenabled = false;
				Edge unenabledEdge = null;
				count = 0;
				for (Edge edge : transition.getEdges()) {
					// Do "discrete post" for this edge
					// (followed by c-closure)
					lz2 = lz.deepCopy();
					lz2.dPost(edge);
					lz2.cClosure(pta);
					// If non-empty, create edge, also adding state to X if new 
					if (!lz2.zone.isEmpty()) {
						if (Yset.add(lz2)) {
							X.add(lz2);
						}
						dest = Yset.getIndexOfLastAdd();
						enabled = true;
						dests[count] = dest;
					} else {
						unenabled = true;
						// Store first unenabled edge
						unenabledEdge = (unenabledEdge == null) ? edge : unenabledEdge;
						dests[count] = -1;
					}
					count++;
				}
				if (enabled) {
					if (unenabled) {
						String s = "Badly formed PTA at location " + pta.getLocationNameString(lz.loc) + " when " + lz.zone;
						s += ": \"" + transition.getAction() + "\"-labelled transition to ";
						s += pta.getLocationNameString(unenabledEdge.getDestination());
						s += " leads to state where invariant is not satisfied";
						throw new PrismException(s);
					}
					graph.addTransition(src, transition, dests, null);
				}
			}
			// Check for another possible cause of timelock:
			// no PTA transitions *enabled* and not possible for time to diverge
			// (NB: This should be defunct now because of earlier timelock check)
			// (NB2: Strictly speaking, don't need to check canDiverge - if it was
			// true, we would have added a loop transition that is definitely enabled)
			if (!canDiverge && graph.trans.get(src).size() == 0) {
				String s = "Timelock in PTA (no enabled transitions) at location " + pta.getLocationNameString(lz.loc);
				s += " when " + lz.zone;
				throw new PrismException(s);
			}
			// Print some progress info occasionally
			if (progress.ready())
				progress.update(Yset.size());
		}

		// Tidy up progress display
		progress.update(Yset.size());
		progress.end(" states");

		// Convert state set to ArrayList and store
		graph.states = Yset.toArrayList();

		// Always have a single initial state 0 after this construction
		initialStates = new ArrayList<Integer>();
		initialStates.add(0);

		// Reachability complete
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Graph constructed in " + (timer / 1000.0) + " secs.");
		mainLog.print("Graph: " + graph.states.size() + " symbolic states");
		mainLog.println(" (" + initialStates.size() + " initial, " + target.cardinality() + " target)");

		// Print a warning if there are no target states
		if (target.cardinality() == 0)
			mainLog.printWarning("There are no target states.");

		return graph;
	}
	
	/**
	 * Implementation of {@link #buildForwardsGraph} using FORMATS'09 definition.
	 * This should not be used any more since changes/improvements to e.g. timelock detection
	 * have been implemented under the assumption that the default (FORMATS'10) algorithm is used.
	 */
	@SuppressWarnings("unused")
	private ReachabilityGraph buildForwardsGraphFormats09(PTA pta, BitSet targetLocs, Constraint targetConstraint)
	throws PrismException
	{
		Zone z;
		LocZone init, lz, lz2;
		LinkedList<LocZone> Y;
		StateStorage<LocZone> Zset;
		//LocZoneSetOld Zset;
		ReachabilityGraph graph;
		int cMax;
		int src, dest, count;
		long timer, timerProgress;
		boolean progressDisplayed;

		// Store target info
		this.targetLocs = targetLocs;
		this.targetConstraint = targetConstraint;

		// Starting reachability...
		mainLog.println("\nBuilding forwards reachability graph...");
		timer = timerProgress = System.currentTimeMillis();
		progressDisplayed = false;

		// Compute max clock constraint value
		if (targetConstraint != null)
			pta.recomputeMaxClockConstraint(targetConstraint);
		cMax = pta.getMaxClockConstraint();

		// Initialise data structures
		graph = new ReachabilityGraph(pta);
		//Zset = new LocZoneSetOld();
		Zset = new IndexedSet<LocZone>();
		Y = new LinkedList<LocZone>();
		target = new BitSet();

		// Build initial symbolic state (NB: assume initial location = 0)
		z = DBM.createZero(pta);
		init = new LocZone(0, z);
		// And do tpost/c-closure
		init.tPost(pta);
		init.cClosure(pta);

		// Reachability loop
		Zset.add(init);
		Y.add(init);
		src = -1;
		// While there are unexplored symbolic states (in Y)...
		while (!Y.isEmpty()) {
			// Pick next state to explore
			// Y is a list containing states in order found
			// (so we know index of lz is src)
			lz = Y.removeFirst();
			src++;
			// Is this a target state?
			if (targetLocs.get(lz.loc) && (targetConstraint == null || lz.zone.isSatisfied(targetConstraint))) {
				target.set(src);
				// Add null for this state (no need to store info)
				graph.addState();
				continue;
			}
			// Otherwise, explore this symbolic state
			// First, check there is at least one transition
			// (don't want deadlocks in non-target states)
			if (pta.getTransitions(lz.loc).size() == 0) {
				throw new PrismException("PTA deadlocks in location " + pta.getLocationNameString(lz.loc));
			}
			// For each outgoing transition...
			graph.addState();
			for (Transition transition : pta.getTransitions(lz.loc)) {
				int[] dests = new int[transition.getNumEdges()];
				boolean enabled = false;
				boolean unenabled = false;
				count = 0;
				for (Edge edge : transition.getEdges()) {
					// Build "post" zone for this edge
					// (dpost, then tpost, then c-closure) 
					lz2 = lz.deepCopy();
					lz2.dPost(edge);
					lz2.tPost(pta);
					lz2.cClosure(pta);
					// If non-empty, create edge, also adding state to Y if new 
					if (!lz2.zone.isEmpty()) {
						if (Zset.add(lz2)) {
							Y.add(lz2);
						}
						dest = Zset.getIndexOfLastAdd();
						enabled = true;
						dests[count] = dest;
					} else {
						unenabled = true;
						dests[count] = -1;
					}
					count++;
				}
				if (enabled) {
					if (unenabled)
						throw new PrismException("Badly formed PTA: state " + src);
					graph.addTransition(src, transition, dests, null);
				}
			}
			// Print some progress info occasionally
			if (System.currentTimeMillis() - timerProgress > 3000) {
				if (!progressDisplayed) {
					mainLog.print("Number of states so far:");
					progressDisplayed = true;
				}
				mainLog.print(" " + Zset.size());
				mainLog.flush();
				timerProgress = System.currentTimeMillis();
			}
		}

		// Tidy up progress display
		if (progressDisplayed)
			mainLog.println(" " + Zset.size());

		// Convert state set to ArrayList and store
		graph.states = Zset.toArrayList();

		// Always have a single initial state 0 after this construction
		initialStates = new ArrayList<Integer>();
		initialStates.add(0);

		// Reachability complete
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Graph constructed in " + (timer / 1000.0) + " secs.");
		mainLog.print("Graph: " + graph.states.size() + " symbolic states");
		mainLog.println("), " + target.cardinality() + " target states");

		// Print a warning if there are no target states
		if (target.cardinality() == 0)
			mainLog.printWarning("There are no target states.");

		return graph;
	}
}
