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

import java.io.*;
import java.util.*;

import prism.PrismException;

public class Transition
{
	// Parent PTA
	private PTA parent;
	// Source location
	private int src;
	// Action label ("" = tau)
	private String action;
	// Guard - currently a conjunction of constraints
	private Set<Constraint> guard;
	// Edges
	private int numEdges;
	private ArrayList<Edge> edges;

	/**
	 * Create an empty transition.
	 * @param parent Parent PTA
	 * @param src Source location index
	 * @param action Synchronising action ("" if none)
	 */
	public Transition(PTA parent, int src, String action)
	{
		this.parent = parent;
		this.src = src;
		this.action = action;
		guard = new LinkedHashSet<Constraint>();
		numEdges = 0;
		edges = new ArrayList<Edge>();
	}

	/**
	 * Copy constructor.
	 */
	public Transition(Transition tr)
	{
		this(tr.parent, tr.src, tr.action);
		for (Constraint c : tr.guard) {
			addGuardConstraint(new Constraint(c));
		}
		for (Edge e : tr.edges) {
			addEdge(new Edge(e));
		}
	}
	
	public void addGuardConstraint(Constraint c)
	{
		guard.add(c);
		parent.recomputeMaxClockConstraint(c);
	}

	public Edge addEdge(double prob, int dest)
	{
		Edge e = new Edge(this, prob, dest);
		edges.add(e);
		numEdges++;
		return e;
	}

	public void addEdge(Edge e)
	{
		e.setParent(this);
		edges.add(e);
		numEdges++;
	}
	
	public void setParent(PTA parent)
	{
		this.parent = parent;
	}

	public PTA getParent()
	{
		return parent;
	}

	public String getAction()
	{
		return action;
	}

	public int getSource()
	{
		return src;
	}

	public Iterable<Constraint> getGuardConstraints()
	{
		return guard;
	}

	public int getNumEdges()
	{
		return edges.size();
	}

	public List<Edge> getEdges()
	{
		return edges;
	}

	/**
	 * Perform some basic syntactic checks.
	 */
	public void check() throws PrismException
	{
		// Check edges
		// Also check that  total probability is 1 (ish)
		double prob = 0.0;
		double err = 1e-10;
		for (Edge e : edges) {
			e.check();
			prob += e.getProbability();
		}
		if (prob < 1.0-err || prob > 1.0+err)
			throw new PrismException("Non-1 probability ("+prob+") for PTA transition \""+this+"\"");
	}

	public String toString()
	{
		boolean first;
		String s = "";
		s += parent.getLocationName(src) + " -";
		if (action != null)
			s += action + ":";
		s += Constraint.toStringList(parent, guard);
		s += "-> ";
		first = true;
		for (Edge e : edges) {
			if (first)
				first = false;
			else
				s += " + ";
			s += e;
		}
		return s;
	}

	public void writeToDesFile(Writer out) throws PrismException, IOException
	{
		out.write("\t[\n");
		for (Edge e : edges) {
			e.writeToDesFile(out, ((action == null || "".equals(action)) ? "" : action + "||"), guard);
		}
		out.write("\t]\n");
	}
}
