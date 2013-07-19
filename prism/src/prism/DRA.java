//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Hongyang Qu <hongyang.qu@cs.ox.ac.uk> (University of Oxford)
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

package prism;

import java.io.PrintStream;
import java.util.*;

/**
 * Class to store a deterministic Rabin automata (DRA).
 * States are 0-indexed integers; class is parameterised by edge labels (Symbol).
 */
public class DRA<Symbol>
{
	/** AP list */
	private List<String> apList;
	/** Size, i.e. number of states */
	private int size;
	/** Start state (index) */
	private int start;
	/** Edges of DRA */
	private List<List<Edge>> edges;
	/** Sets L_i of acceptance condition pairs (L_i,K_i), stored as BitSets */
	private List<BitSet> acceptanceL;
	/** Sets K_i of acceptance condition pairs (L_i,K_i), stored as BitSets */
	private List<BitSet> acceptanceK;

	/** Local class to represent DRA edge */
	class Edge
	{
		private Symbol label;
		private int dest;

		public Edge(Symbol label, int dest)
		{
			this.label = label;
			this.dest = dest;
		}
	}

	/**
	 * Construct a DRA of fixed size (i.e. fixed number of states).
	 */
	public DRA(int size)
	{
		apList = null;
		this.size = size;
		this.start = -1;
		edges = new ArrayList<List<Edge>>(size);
		for (int i = 0; i < size; i++) {
			edges.add(new ArrayList<Edge>());
		}
		acceptanceL = new ArrayList<BitSet>();
		acceptanceK = new ArrayList<BitSet>();
	}

	// TODO: finish/tidy this
	public void setAPList(List<String> apList)
	{
		this.apList = apList;
	}
	
	public List<String> getAPList()
	{
		return apList;
	}
	
	// Mutators

	/**
	 * Set the start state (index)
	 */
	public void setStartState(int start)
	{
		this.start = start;
	}

	/**
	 * Add an edge
	 */
	public void addEdge(int src, Symbol label, int dest)
	{
		edges.get(src).add(new Edge(label, dest));
	}

	/**
	 * Add an acceptance pair (L_i,K_i)
	 */
	public void addAcceptancePair(BitSet l, BitSet k)
	{
		acceptanceL.add(l);
		acceptanceK.add(k);
	}

	// Accessors

	/**
	 * Get the size (number of states).
	 */
	public int size()
	{
		return size;
	}

	/**
	 * Get the start state (index)
	 */
	public int getStartState()
	{
		return start;
	}

	/**
	 * Get the number of edges from state i
	 */
	public int getNumEdges(int i)
	{
		return edges.get(i).size();
	}

	/**
	 * Get the destination of edge j from state i
	 */
	public int getEdgeDest(int i, int j)
	{
		return edges.get(i).get(j).dest;
	}

	/**
	 * Get the label of edge j from state i.
	 */
	public Symbol getEdgeLabel(int i, int j)
	{
		return edges.get(i).get(j).label;
	}

	/**
	 * Get the destination of the edge from state i with label lab.
	 * Returns -1 if no such edge is found.
	 */
	public int getEdgeDestByLabel(int i, Symbol lab)
	{
		for (Edge e : edges.get(i))
			if (e.label.equals(lab))
				return e.dest;
		return -1;
	}

	/**
	 * Get the number of pairs (L_i,K_i) in the acceptance condition.
	 */
	public int getNumAcceptancePairs()
	{
		return acceptanceL.size();
	}

	/**
	 * Get the states in set L_i for acceptance condition pair (L_i,K_i).
	 * @param Pair index (0-indexed)
	 */
	public BitSet getAcceptanceL(int i)
	{
		return acceptanceL.get(i);
	}

	/**
	 * Get the states in set K_i for acceptance condition pair (L_i,K_i).
	 * @param Pair index (0-indexed)
	 */
	public BitSet getAcceptanceK(int i)
	{
		return acceptanceK.get(i);
	}

	/**
	 * Print DRA in DOT format to an output stream.
	 */
	public void printDot(PrintStream out) throws PrismException
	{
		int i, j, n;
		out.println("digraph model {");
		for (i = 0; i < size; i++) {
			if (i == start)
				out.println("	" + i + " [label=\"" + i + "\", shape=ellipse]");
			else {
				boolean isAcceptance = false;
				n = getNumAcceptancePairs();
				for (j = 0; j < n; j++) {
					if (acceptanceK.get(j).get(i)) {
						out.println("	" + i + " [label=\"" + i + "\", shape=doublecircle]");
						isAcceptance = true;
						break;
					} else if (acceptanceL.get(j).get(i)) {
						out.println("	" + i + " [label=\"" + i + "\", shape=box]");
						isAcceptance = true;
						break;
					}
				}
				if (!isAcceptance)
					out.println("	" + i + " [label=\"" + i + "\", shape=circle]");
			}
		}
		for (i = 0; i < size; i++) {
			for (Edge e : edges.get(i)) {
				out.println("	" + i + " -> " + e.dest + " [label=\"" + e.label + "\"]");
			}
		}
	}

	// Standard methods

	@Override
	public String toString()
	{
		String s = "";
		int i, n;
		s += size + " states (start " + start + ")";
		if (apList != null)
			s += ", " + apList.size() + " labels";
 		s += ":";
		for (i = 0; i < size; i++) {
			for (Edge e : edges.get(i)) {
				s += " " + i + "-" + e.label + "->" + e.dest;
			}
		}
		n = acceptanceL.size();
		s += "; " + n + " acceptance pairs:";
		for (i = 0; i < n; i++) {
			s += " (" + acceptanceL.get(i) + "," + acceptanceK.get(i) + ")";
		}
		return s;
	}
}
