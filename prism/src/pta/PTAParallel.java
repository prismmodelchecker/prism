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

import prism.PrismException;
import explicit.IndexedSet;
import explicit.StateStorage;

/**
 * Class to perform the parallel composition of PTAs.
 */
public class PTAParallel
{
	// All states
	private StateStorage<IndexPair> states;
	// States to be explored
	private LinkedList<IndexPair> explore;
	// Component PTAs
	private PTA pta1;
	private PTA pta2;
	// Resulting PTA (parallel composition)
	private PTA par;
	
	/**
	 * Construct the parallel composition of two PTAs.
	 */
	public PTA compose(PTA pta1, PTA pta2)
	{
		Set<String> alpha1, alpha2, alpha1only, alpha2only, sync;
		Transition transition;
		Edge edge;
		double prob;
		IndexPair state;
		int src, dest;

		// Store PTAs locally and create new one to store parallel composition
		this.pta1 = pta1;
		this.pta2 = pta2;
		par = new PTA();

		// New set of clocks is union of sets for two PTAs
		for (String s : pta1.clockNames) {
			par.getOrAddClock(s);
		}
		for (String s : pta2.clockNames) {
			par.getOrAddClock(s);
		}

		// Get alphabets, compute intersection etc.
		alpha1 = pta1.getAlphabet();
		alpha2 = pta2.getAlphabet();
		//System.out.println("alpha1: " + alpha1);
		//System.out.println("alpha2: " + alpha2);
		sync = new LinkedHashSet<String>();
		alpha1only = new LinkedHashSet<String>();
		alpha2only = new LinkedHashSet<String>();
		for (String a : alpha1) {
			if (!("".equals(a)) && alpha2.contains(a)) {
				sync.add(a);
			} else {
				alpha1only.add(a);
			}
		}
		for (String a : alpha2) {
			if (!alpha1.contains(a)) {
				alpha2only.add(a);
			}
		}
		// Explicitly add tau to action lists
		alpha1only.add("");
		alpha2only.add("");
		//System.out.println("alpha1only: " + alpha1only);
		//System.out.println("alpha2only: " + alpha2only);
		//System.out.println("sync: " + sync);

		// Initialise states storage
		states = new IndexedSet<IndexPair>();
		explore = new LinkedList<IndexPair>();
		// Add initial location
		addState(0, 0);
		src = -1;
		while (!explore.isEmpty()) {
			// Pick next state to explore
			// (they are stored in order found so know index is src+1)
			state = explore.removeFirst();
			src++;
			// Go through asynchronous transitions of PTA 1
			for (String a : alpha1only) {
				for (Transition transition1 : pta1.getTransitionsByAction(state.i1, a)) {
					// Create new transition
					transition = par.addTransition(src, a);
					// Copy guard
					for (Constraint c : transition1.getGuardConstraints())
						transition.addGuardConstraint(c.deepCopy().renameClocks(pta1, par));
					// Combine edges
					for (Edge edge1 : transition1.getEdges()) {
						prob = edge1.getProbability();
						dest = addState(edge1.getDestination(), state.i2);
						edge = transition.addEdge(prob, dest);
						// Copy resets
						for (Map.Entry<Integer, Integer> e : edge1.getResets()) {
							edge.addReset(PTA.renameClock(pta1, par, e.getKey()), e.getValue());
						}
					}
				}
			}
			// Go through asynchronous transitions of PTA 2
			for (String a : alpha2only) {
				for (Transition transition2 : pta2.getTransitionsByAction(state.i2, a)) {
					// Create new transition
					transition = par.addTransition(src, a);
					// Copy guard
					for (Constraint c : transition2.getGuardConstraints())
						transition.addGuardConstraint(c.deepCopy().renameClocks(pta2, par));
					// Combine edges
					for (Edge edge2 : transition2.getEdges()) {
						prob = edge2.getProbability();
						dest = addState(state.i1, edge2.getDestination());
						edge = transition.addEdge(prob, dest);
						// Copy resets
						for (Map.Entry<Integer, Integer> e : edge2.getResets()) {
							edge.addReset(PTA.renameClock(pta2, par, e.getKey()), e.getValue());
						}
					}
				}
			}
			// Go through synchronous transitions
			for (String a : sync) {
				for (Transition transition1 : pta1.getTransitionsByAction(state.i1, a)) {
					for (Transition transition2 : pta2.getTransitionsByAction(state.i2, a)) {
						// Create new transition
						transition = par.addTransition(src, a);
						// Guard is conjunction of guards
						for (Constraint c : transition1.getGuardConstraints())
							transition.addGuardConstraint(c.deepCopy().renameClocks(pta1, par));
						for (Constraint c : transition2.getGuardConstraints())
							transition.addGuardConstraint(c.deepCopy().renameClocks(pta2, par));
						// Combine edges
						for (Edge edge1 : transition1.getEdges()) {
							for (Edge edge2 : transition2.getEdges()) {
								prob = edge1.getProbability() * edge2.getProbability();
								dest = addState(edge1.getDestination(), edge2.getDestination());
								edge = transition.addEdge(prob, dest);
								// Reset set is union of reset sets
								for (Map.Entry<Integer, Integer> e : edge1.getResets()) {
									edge.addReset(PTA.renameClock(pta1, par, e.getKey()), e.getValue());
								}
								for (Map.Entry<Integer, Integer> e : edge2.getResets()) {
									edge.addReset(PTA.renameClock(pta2, par, e.getKey()), e.getValue());
								}
							}
						}
					}
				}
			}
		}
		
		return par;
	}

	/**
	 * Helper function. Add a new state, if it does not already exist, and returns its index.
	 * @param loc1: Index of location of PTA 1
	 * @param loc2: Index of location of PTA 2
	 */
	private int addState(int loc1, int loc2)
	{
		// See if this is a new state
		IndexPair stateNew = new IndexPair(loc1, loc2);
		if (states.add(stateNew)) {
			// If so, add to the explore list
			explore.add(stateNew);
			// Get index of state in state set
			int locNew = states.getIndexOfLastAdd();
			// And add a location to the PTA
			par.addLocation(PTA.combineLocationNames(pta1.getLocationName(loc1), pta2.getLocationName(loc2)));
			// Invariant is conjunction of two invariants
			for (Constraint c : pta1.getInvariantConstraints(loc1))
				par.addInvariantCondition(locNew, c.deepCopy().renameClocks(pta1, par));
			for (Constraint c : pta2.getInvariantConstraints(loc2))
				par.addInvariantCondition(locNew, c.deepCopy().renameClocks(pta2, par));
		}
		// Return index of state in state set 
		return states.getIndexOfLastAdd();
	}
	
	/**
	 * Class to store a pair of indices.
	 */
	class IndexPair
	{
		int i1;
		int i2;

		public IndexPair(int i1, int i2)
		{
			this.i1 = i1;
			this.i2 = i2;
		}
		
		public int hashCode()
		{
			// Simple hash code
			return i1;
		}
		
		public boolean equals(Object o)
		{
			IndexPair ip;
			if (o == null) return false;
			try {
				ip = (IndexPair) o;
			} catch (ClassCastException e) {
				return false;
			}
			return i1 == ip.i1 && i2 == ip.i2;
		}
		
		public String toString()
		{
			return i1 +"," + i2; 
		}
	}

	/**
	 * Main method to test
	 *  (read from multiple des files, output result to a des file)
	 */ 
	public static void main(String args[])
	{
		PTA pta, pta2;
		int i;

		if (args.length < 1) {
			System.err.println("Usage: java ... <des files>");
			System.exit(1);
		}
		try {
			// Build PTA
			System.out.println("Building PTA from \"" + args[0] + "\"");
			pta = PTA.buildPTAFromDesFile(args[0]);
			System.out.println(pta.infoString());
			//System.out.println(pta);
			for (i = 1; i < args.length; i++) {
				System.out.println("Building PTA from \"" + args[i] + "\"");
				pta2 = PTA.buildPTAFromDesFile(args[i]);
				System.out.println(pta2.infoString());
				//System.out.println(pta2);
				pta = new PTAParallel().compose(pta, pta2);
				//System.out.println(pta);
			}
			System.out.println("Final PTA: " + pta.infoString());
			System.out.println(pta);
			pta.check();
			pta.writeToDesFile("par.des");
			// Parse target location
		} catch (PrismException e) {
			System.err.println("Error: " + e.getMessage());
		}
	}
}
