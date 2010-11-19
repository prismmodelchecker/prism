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

import explicit.*;
import prism.*;

/**
 * Class to store a forwards reachability graph for a PTA.
 */
public class ReachabilityGraph
{
	// PTA
	PTA pta;
	// List of symbolic states (Z)
	List<LocZone> states;
	// List of symbolic transitions (grouped by source location)
	// (thus source is not stored explicitly in the symbolic transition)
	ArrayList<ArrayList<SymbolicTransition>> trans;

	/**
	 * Construct empty reachability graph.
	 * @param pta PTA associated with this graph.
	 */
	public ReachabilityGraph(PTA pta)
	{
		this.pta = pta;
		states = null;
		trans = new ArrayList<ArrayList<SymbolicTransition>>();
	}

	public void addState()
	{
		trans.add(new ArrayList<SymbolicTransition>());
	}

	public void copyState(int i)
	{
		ArrayList<SymbolicTransition> listOld, listNew;
		listOld = trans.get(i);
		listNew = new ArrayList<SymbolicTransition>(listOld.size());
		for (SymbolicTransition g : listOld)
			listNew.add(new SymbolicTransition(g));
		trans.add(listNew);
	}

	/**
	 * Add a new symbolic transition to the graph.
	 * @param src Source state index
	 * @param tr Corresponding transition in the PTA
	 * @param dests List of destinations
	 * @param valid Validity condition (usually left null and computed later)
	 */
	public void addTransition(int src, Transition tr, int dests[], Zone valid)
	{
		trans.get(src).add(new SymbolicTransition(tr, dests, valid));
	}

	/**
	 * Returns true if state s2 is a successor of state s1
	 * (i.e. if there is a symbolic transition with s1 as source and s2 in destinations).
	 */
	public boolean isSuccessor(int s1, int s2)
	{
		for (SymbolicTransition st : trans.get(s1)) {
			if (st.hasSuccessor(s2))
				return true;
		}
		return false;
	}

	/**
	 * Compute the validity conditions for all symbolic transitions in the graph.
	 */
	public void computeAllValidities()
	{
		int i, n;
		ArrayList<SymbolicTransition> list;
		n = trans.size();
		for (i = 0; i < n; i++) {
			list = trans.get(i);
			for (SymbolicTransition st : list) {
				st.valid = computeValidity(i, st.tr, st.dests);
			}
		}
	}

	/**
	 * Print the list of symbolic states to a log.
	 */
	public void printStates(PrismLog log)
	{
		int i = 0;
		for (LocZone lz : states) {
			if (i > 0)
				log.print(" ");
			log.print("#" + (i++) + ":" + lz);
		}
	}
	
	@Override
	public String toString()
	{
		int i, n;
		boolean first;
		String s = "";
		n = trans.size();
		first = true;
		s = "[ ";
		for (i = 0; i < n; i++) {
			if (first)
				first = false;
			else
				s += ", ";
			s += i + ":" + trans.get(i);
		}
		s += " ]";
		return s;
	}

	/**
	 * Compute the validity condition for a symbolic transition
	 * @param src The index of the source state of the symbolic transition 
	 * @param tr The PTA transition corresponding to the symbolic transition
	 * @param dests The list of destination state indices of the symbolic transition
	 */
	public Zone computeValidity(int src, Transition tr, int[] dests)
	{
		Zone z = new DBMList(DBM.createTrue(pta));
		int count = 0;
		for (Edge edge : tr.getEdges()) {
			LocZone lz2 = states.get(dests[count]).deepCopy();
			lz2.dPre(edge);
			z.intersect(lz2.zone);
			count++;
		}
		z.down();
		z.intersect(states.get(src).zone);

		return z;
	}
	
	/**
	 * Build an MDP from this forwards reachability graph.
	 * The set of initial states should also be specified.
	 */
	public MDP buildMDP(List<Integer> initialStates) throws PrismException
	{
		Distribution distr;
		int numStates, src, count, dest;
		MDPSimple mdp;

		// Building MDP...
		mdp = new MDPSimple();

		// Add all states
		mdp.addStates(states.size());

		// For each symbolic state...
		numStates = states.size(); 
		for (src = 0; src < numStates; src++) {
			// And for each outgoing transition in PTA...
			for (SymbolicTransition st : trans.get(src)) {
				distr = new Distribution();
				count = -1;
				for (Edge edge : st.tr.getEdges()) {
					count++;
					dest = st.dests[count];
					distr.add(dest, edge.getProbability());
				}
				if (!distr.isEmpty())
					mdp.addChoice(src, distr);
			}
		}

		// Set initial states
		for (int i : initialStates) {
			mdp.addInitialState(i);
		}

		return mdp;
	}
}
