//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Hongyang Qu <hongyang.qu@cs.ox.ac.uk> (University of Oxford)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

import acceptance.AcceptanceOmega;

/**
 * Class to store a deterministic automata of some acceptance type Acceptance.
 * States are 0-indexed integers; class is parameterised by edge labels (Symbol).
 */
public class DA<Symbol, Acceptance extends AcceptanceOmega>
{
	/** AP list */
	private List<String> apList;
	/** Size, i.e. number of states */
	private int size;
	/** Start state (index) */
	private int start;
	/** Edges of DRA */
	private List<List<Edge>> edges;
	/** The acceptance condition (as BitSets) */
	private Acceptance acceptance;

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
	public DA(int size)
	{
		apList = null;
		this.size = size;
		this.start = -1;
		edges = new ArrayList<List<Edge>>(size);
		for (int i = 0; i < size; i++) {
			edges.add(new ArrayList<Edge>());
		}
	}

	public void setAcceptance(Acceptance acceptance)
	{
		this.acceptance = acceptance;
	}

	public Acceptance getAcceptance() {
		return acceptance;
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
	 * Print DRA in DOT format to an output stream.
	 */
	public void printDot(PrintStream out) throws PrismException
	{
		int i;
		out.println("digraph model {");
		for (i = 0; i < size; i++) {
			out.print("	" + i + " [label=\"" + i + " [");
			out.print(acceptance.getSignatureForState(i));
			out.print("]\", shape=");
			if (i == start)
				out.println("doublecircle]");
			else
				out.println("ellipse]");
		}
		for (i = 0; i < size; i++) {
			for (Edge e : edges.get(i)) {
				out.println("	" + i + " -> " + e.dest + " [label=\"" + e.label + "\"]");
			}
		}
		out.println("}");
	}

	// Standard methods

	@Override
	public String toString()
	{
		String s = "";
		int i;
		s += size + " states (start " + start + ")";
		if (apList != null)
			s += ", " + apList.size() + " labels (" + apList + ")";
		s += ":";
		for (i = 0; i < size; i++) {
			for (Edge e : edges.get(i)) {
				s += " " + i + "-" + e.label + "->" + e.dest;
			}
		}
		s += "; " + acceptance.getTypeName() + " acceptance: ";
		s += acceptance;
		return s;
	}

	public String getAutomataType()
	{
		return "D"+acceptance.getTypeAbbreviated()+"A";
	}

	/**
	 * Switch the acceptance condition. This may change the acceptance type,
	 * i.e., a DA<BitSet, AcceptanceRabin> may become a DA<BitSet, AcceptanceStreett>
	 * @param da the automaton
	 * @param newAcceptance the new acceptance condition
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void switchAcceptance(DA da, AcceptanceOmega newAcceptance)
	{
		// as Java generics are only compile time, we can change the AcceptanceType
		da.acceptance = newAcceptance;
	}
}
