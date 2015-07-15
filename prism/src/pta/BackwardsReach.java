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

public class BackwardsReach extends PrismComponent
{
	/**
	 * Create a new MDPModelChecker, inherit basic state from parent (unless null).
	 */
	public BackwardsReach(PrismComponent parent) throws PrismException
	{
		super(parent);
	}

	/**
	 * Compute the min/max probability of reaching a target in a PTA, from the initial state,
	 * using the backwards reachability method.
	 * @param pta The PTA
	 * @param targetLocs Target locations
	 * @param targetConstraint Target timing contraint
	 * @param min Min or max probabilities (true=min, false=max)
	 */
	public double computeProbabilisticReachability(PTA pta, BitSet targetLocs, boolean min) throws PrismException
	{
		if (min)
			throw new PrismException("Backwards reachability does not yet support minmum probabilities");

		// Build backwards graph
		BackwardsReachabilityGraph graph = buildBackwardsGraph(pta, targetLocs);

		// Build corresponding MDP
		mainLog.print("Building MDP... ");
		mainLog.flush();
		MDP mdp = graph.buildMDP(pta);
		mainLog.println(mdp.infoString());
		//mdp.exportToDotFile("mdp.dot", target);

		// Compute reachability probs on MDP
		MDPModelChecker mc = new MDPModelChecker(this);
		mc.setPrecomp(false);
		ModelCheckerResult res = mc.computeReachProbs(mdp, graph.getTarget(), false);

		// Result is max prob over all initial states
		return Math.max(0.0, Utils.minMaxOverArraySubset(res.soln, graph.getInitialStates(), false));
	}

	/**
	 * Build a backwards reachability graph, from a PTA object.
	 */
	public BackwardsReachabilityGraph buildBackwardsGraph(PTA pta, BitSet targetLocs) throws PrismException
	{
		LocZone lz;
		LinkedList<LocZone> explore; // (Waiting)
		IndexedSet<LocZone> found; // (Visited)
		List<LocZone> states;
		BackwardsReachabilityGraph graph;
		int src, dest, count;
		long timer;

		mainLog.print("\nBuilding backwards reachability graph...");
		mainLog.flush();
		ProgressDisplay progress = new ProgressDisplay(mainLog);
		progress.start();
		timer = System.currentTimeMillis();

		// Initialise data structures
		graph = new BackwardsReachabilityGraph();
		found = new IndexedSet<LocZone>();
		explore = new LinkedList<LocZone>();
		graph.states = states = new ArrayList<LocZone>();

		// Add target states
		count = 0;
		for (int loc = targetLocs.nextSetBit(0); loc >= 0; loc = targetLocs.nextSetBit(loc + 1)) {
			Zone zTarget = DBM.createTrue(pta);
			zTarget.addConstraints(pta.getInvariantConstraints(loc));
			LocZone lzTarget = new LocZone(loc, zTarget);
			found.add(lzTarget);
			explore.add(lzTarget);
			states.add(lzTarget);
			graph.addState(pta.getTransitions(loc));
			graph.addTargetState(count++);
		}

		// Reachability loop
		dest = -1;
		// While there are unexplored symbolic states...
		while (!explore.isEmpty()) {
			// Pick next state to explore
			// NB: States are added in order found so we know index of lz is dest+1
			lz = explore.removeFirst();
			dest++;
			//mainLog.println("Exploring: " + dest + ":" + lz);

			// Time predecessor (same for all incoming edges)
			lz = lz.deepCopy();
			lz.tPre(pta);

			// For each incoming transition...
			// TODO: make more efficient
			for (int ii = 0; ii < pta.getNumLocations(); ii++) {
				if (targetLocs.get(ii))
					continue;
				int iTrans = -1;
				for (Transition tr : pta.getTransitions(ii)) {
					iTrans++;
					int numEdges = tr.getNumEdges();
					for (int iEdge = 0; iEdge < numEdges; iEdge++) {
						Edge edge = tr.getEdges().get(iEdge);
						//mainLog.println(edge);
						if (edge.getDestination() != lz.loc)
							continue;
						LocZone lzSrc = lz.deepCopy();
						lzSrc.dPre(edge);
						// If predecessor state is non-empty
						if (!lzSrc.zone.isEmpty()) {
							// add state
							if (found.add(lzSrc)) {
								//mainLog.println("Added " + Yset.getIndexOfLastAdd() + ":" + lzSrc);
								explore.add(lzSrc);
								states.add(lzSrc);
								graph.addState(pta.getTransitions(lzSrc.loc));
							}
							// end add state
							src = found.getIndexOfLastAdd();
							//mainLog.println("D += " + src + ":" + lz2 + "->" + dest + ":...");
							graph.addTransition(src, iTrans, iEdge, dest);
							//mainLog.println(src + ":" + graph.getList(src));

							// For each state that lzTmp intersects lzSrc
							int numStatesSoFar = states.size();
							for (int src2 = 0; src2 < numStatesSoFar; src2++) {
								// TODO: if any edges in graph...
								LocZone lzTmp = states.get(src2);
								if (src2 != src && lzTmp.loc == lzSrc.loc) {
									Zone zTmp = lzTmp.zone.deepCopy();
									zTmp.intersect(lzSrc.zone);
									//mainLog.println("intersect with " + st + ":" + lzTmp + ": " + zTmp);
									if (!zTmp.isEmpty()) {
										// For all reachability graph edges from lzTmp...
										int numTrans2 = graph.getList(src2).size();
										for (int iTrans2 = 0; iTrans2 < numTrans2; iTrans2++) {
											List<List<Integer>> edges2 = graph.getList(src2).get(iTrans2);
											int numEdges2 = edges2.size();
											if (numEdges2 < 2) {
												//mainLog.println("SKIP");
												continue;
											}
											for (int iEdge2 = 0; iEdge2 < numEdges2; iEdge2++) {
												List<Integer> dests2 = edges2.get(iEdge2);
												int numDests2 = dests2.size();
												for (int iDest2 = 0; iDest2 < numDests2; iDest2++) {
													int dest2 = dests2.get(iDest2);
													// Edge (src2, iTrans2, iEdge2, dest2)

													// add state

													LocZone lz3 = new LocZone(lzSrc.loc, zTmp);
													if (found.add(lz3)) {
														//mainLog.println("Added2 " + Yset.getIndexOfLastAdd() + ":" + lz3);
														explore.add(lz3);
														states.add(lz3);
														graph.addState(pta.getTransitions(lz3.loc));
													} else {
														//mainLog.println("Reusing " + Yset.getIndexOfLastAdd() + ":" + lz3);
													}
													// end add state
													int src3 = found.getIndexOfLastAdd();
													//mainLog.println("^D += " + src3 + ":" + lz3 + "->" + dest + ":...");
													graph.addTransition(src3, iTrans, iEdge, dest);
													//mainLog.println(src3 + ":" + graph.getList(src3));
													//mainLog.println("^D += " + src3 + ":" + lz3 + "->" + dest2 + ":...");
													graph.addTransition(src3, iTrans2, iEdge2, dest2);
													//mainLog.println(src3 + ":" + graph.getList(src3));
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			// Print some progress info occasionally
			if (progress.ready())
				progress.update(found.size());
		}

		// Tidy up progress display
		progress.update(found.size());
		progress.end(" states");

		// Determine which are initial states
		// (NB: assume initial location = 0)
		int numStates = states.size();
		for (int st = 0; st < numStates; st++) {
			if (states.get(st).loc == 0) {
				Zone z = states.get(st).zone.deepCopy();
				z.down();
				if (z.includes(DBM.createZero(pta)))
					graph.addInitialState(st);
			}
		}

		// Reachability complete
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Graph constructed in " + (timer / 1000.0) + " secs.");
		mainLog.print("Graph: " + graph.states.size() + " symbolic states");
		mainLog.println(" (" + graph.getInitialStates().size() + " initial, " + graph.getTarget().cardinality() + " target)");

		return graph;
	}
}
