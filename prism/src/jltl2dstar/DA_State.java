/*
 * This file is part of a Java port of the program ltl2dstar
 * (http://www.ltl2dstar.de/) for PRISM (http://www.prismmodelchecker.org/)
 * Copyright (C) 2005-2007 Joachim Klein <j.klein@ltl2dstar.de>
 * Copyright (c) 2007 Carlos Bederian
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as 
 *  published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jltl2dstar;

import java.util.HashMap;

import jltl2ba.APElement;

/**
 * A state of a deterministic omega-automaton.
 * For a description of the template parameters, see class DA.
 */
	
public class DA_State {

	/** The automaton of which this state is a part. */
	private	DA _graph;
	
	private int _name; 

	/** The edges */
	private HashMap<APElement, DA_State> _edges;

	/** A description */
	private String _description;

	/** 
	 * Constructor.
	 * @param graph The automaton (DA) that contains this state.
	 */
	public DA_State(DA graph)
	{
		_graph = graph;
		_edges = new HashMap<APElement, DA_State>();
		_description = "";
	}
  
	/** Get the EdgeContainer to access the edges. */
	public HashMap<APElement, DA_State> edges()
	{
		return _edges;
	}
  
	/** Get the name (index) of this state. */
	public int getName() 
	{
		// return _graph.getIndexForState(this);
		return _name;
	}

	/** Set the name (index) of this state. */
	public void setName(int name) 
	{
		// return _graph.getIndexForState(this);
		_name = name;
	}

	/** Print the name of the state on an output stream. */
	public String toString() 
	{
		return Integer.toString(getName());
	}
 
	/** Set an description for the state */
	public void setDescription(String s) 
	{
		_description = s;
	}

	/**
	 * Get an description for the state (previously set using setDescription()).
	 * Should only be called after verifying that the state hasDescription()
	 * @return a const string ref to the description
	 */
	public String getDescription() 
	{
		return _description;
	}

	/**
	 * Check whether the state has a description.
	 */
	public boolean hasDescription() 
	{
		return _description != "";
	}

	/**
	 * Checks if all transitions originating in this state
	 * leed back to itself. 
	 */
	public boolean hasOnlySelfLoop() 
	{
		for (DA_State dest : edges().values())
		{
			if (dest != this) {
				return false;
			}
		}
		return true;
	}

	/** Get the AcceptanceForState access functor for this state */
	public AcceptanceForState acceptance() {
		AcceptanceForState acc = new AcceptanceForState(_graph.acceptance(), this.getName());
		return acc;
	}
}
