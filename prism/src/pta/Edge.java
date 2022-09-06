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

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import prism.PrismException;

public class Edge
{
	// Parent transition;
	private Transition parent;
	// Probability
	private double prob;
	// Destination location
	private int dest;
	// Resets
	private HashMap<Integer, Integer> resets;

	/**
	 * Create an empty edge.
	 * @param parent Parent transition
	 * @param prob Probability
	 * @param dest destination location
	 */
	public Edge(Transition parent, double prob, int dest)
	{
		this.parent = parent;
		this.prob = prob;
		this.dest = dest;
		resets = new HashMap<Integer, Integer>();
	}

	/**
	 * Copy constructor.
	 */
	public Edge(Edge edge)
	{
		this(edge.parent, edge.prob, edge.dest);
		for (Map.Entry<Integer, Integer> e : edge.resets.entrySet()) {
			addReset(e.getKey(), e.getValue());
		}
	}
	
	public void setProb(double prob)
	{
		this.prob = prob;
	}

	public void setDestination(int dest)
	{
		this.dest = dest;
	}

	public void setParent(Transition parent)
	{
		this.parent = parent;
	}

	public void addReset(int clock)
	{
		addReset(clock, 0);
	}

	public void addReset(int clock, int val)
	{
		resets.put(clock, val);
	}

	public Transition getParent()
	{
		return parent;
	}

	public int getDestination()
	{
		return dest;
	}

	public double getProbability()
	{
		return prob;
	}

	public Iterable<Map.Entry<Integer, Integer>> getResets()
	{
		return resets.entrySet();
	}

	/**
	 * Perform some basic syntactic checks.
	 */
	public void check() throws PrismException
	{
	}

	public String toString()
	{
		boolean first = true;
		String s = "";
		s += prob + " : {";
		for (Map.Entry<Integer, Integer> e : resets.entrySet()) {
			if (first)
				first = false;
			else
				s += ",";
			s += parent.getParent().getClockName(e.getKey());
			s += "=" + e.getValue();
		}
		s += "}" + parent.getParent().getLocationName(dest);
		return s;
	}

	public void writeToDesFile(Writer out, String actionPrefix, Iterable<Constraint> guard) throws PrismException,
			IOException
	{
		boolean first;
		PTA pta = parent.getParent();
		out.write("\t" + actionPrefix + "tran  ");
		out.write(pta.getLocationName(dest).toString().replace(':', '_') + "; ");
		out.write(Constraint.toStringList(pta, guard));
		out.write("; ");
		first = true;
		for (Map.Entry<Integer, Integer> e : resets.entrySet()) {
			if (first)
				first = false;
			else
				out.write(",");
			out.write(pta.getClockName(e.getKey()) + "=" + e.getValue());
		}
		if (first)
			out.write("null");
		out.write("; " + prob + "\n");
	}
}
