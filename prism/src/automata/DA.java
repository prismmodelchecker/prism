//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Hongyang Qu <hongyang.qu@cs.ox.ac.uk> (University of Oxford)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//	* David Mueller <david.mueller@tcs.inf.tu-dresden.de> (TU Dresden)
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

package automata;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import jltl2ba.APElement;
import jltl2ba.APElementIterator;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismPrintStreamLog;
import acceptance.AcceptanceOmega;
import acceptance.AcceptanceRabin;

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
	 * Returns true if the automaton has an edge for {@code src} and {@label}.
	 */
	public boolean hasEdge(int src, Symbol label)
	{
		return edges.get(src).contains(label);
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
	 * Print the automaton in Dot format to an output stream.
	 */
	public void printDot(PrintStream out) throws PrismException
	{
		printDot(new PrismPrintStreamLog(out));
	}

	/**
	 * Print automaton in Dot format to a PrismLog
	 */
	public void printDot(PrismLog out) throws PrismException
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
	
	/**
	 * Print the DRA in ltl2dstar v2 format to the output stream.
	 * @param out the output stream 
	 */
	public static void printLtl2dstar(DA<BitSet, AcceptanceRabin> dra, PrintStream out) throws PrismException {
		AcceptanceRabin acceptance = dra.getAcceptance();

		if (dra.getStartState() < 0) {
			// No start state! 
			throw new PrismException("No start state in DA!");
		}

		out.println("DRA v2 explicit");
		out.println("States: " + dra.size());
		out.println("Acceptance-Pairs: " + acceptance.size());
		out.println("Start: " + dra.getStartState());

		// Enumerate APSet
		out.print("AP: " + dra.getAPList().size());
		for (String ap : dra.getAPList()) {
			out.print(" \"" + ap + "\"");
		}
		out.println();

		out.println("---");

		for (int i_state = 0; i_state < dra.size(); i_state++) {
			out.println("State: " + i_state);

			out.print("Acc-Sig:");
			for (int pair = 0; pair < acceptance.size(); pair++) {
				if (acceptance.get(pair).getL().get(i_state)) {
					out.print(" -"+pair);
				} else if (acceptance.get(pair).getK().get(i_state)) {
					out.print(" +"+pair);
				}
			}
			out.println();
			
			APElementIterator it = new APElementIterator(dra.apList.size());
			while (it.hasNext()) {
				APElement edge = it.next();
				out.println(dra.getEdgeDestByLabel(i_state, edge));
			}
		}
	}

	/**
	 * Print automaton to a PrismLog in a specified format ("dot" or "txt").
	 */
	public void print(PrismLog out, String type) throws PrismException
	{
		switch (type) {
		case "txt":
			out.println(toString());
			break;
		case "dot":
			printDot(out);
			break;
		}
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
	 * i.e., a DA&lt;BitSet, AcceptanceRabin&gt; may become a DA&lt;BitSet, AcceptanceStreett&gt;
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
