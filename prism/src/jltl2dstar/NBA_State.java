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

import java.util.*;

import jltl2ba.APElement;
import jltl2ba.APSet;
import jltl2ba.MyBitSet;
import jltl2ba.MyBitSet.MyBitSetIterator;


/** @file
 * Provides class NBA_State for storing a state of a nondeterministic B�chi automaton.
 */

/**
 * A state of a deterministic omega-automaton.
 * For a description of the template parameters, see class NBA.
 */
public class NBA_State implements Iterable<Map.Entry<APElement,MyBitSet>> {

	 /** The automaton */
	private NBA _graph;

	/** Is this state accepting */
	boolean _isFinal;

	/** A description. */
	String _description;

	/** The EdgeManager*/
	EdgeManager _edge_manager;

	
  /**
   * Constructor.
   * @param graph The automaton (NBA) that contains this state.
   */
	public NBA_State(NBA graph)
	{
		_graph = graph;
		_isFinal = false;
		_edge_manager = new EdgeManager(this, graph.getAPSet());
		_description = "";
	}


	/** 
	 * Add an edge from this state to the other state
	 * @param label the label for the edge
	 * @param state the target state 
	 */
	public void addEdge(APElement label, NBA_State state) {
		_edge_manager.addEdge(label, state);
	}


	/** 
	 * Add edge(s) from this state to the other state
	 * @param monom an APMonom for the label(s)
	 * @param to_state the target state
	 */
	public void addEdge(APMonom monom, NBA_State to_state) {
		_edge_manager.addEdge(monom, to_state);
	}
	
	/** 
	 * Add edge(s) from this state to the other state
	 * @param monom an APMonom for the label(s)
	 * @param to_state the target state
	 */
	public void addEdges(APElement monom, MyBitSet to_states) {
		_edge_manager.addEdges(monom, to_states);
	}

	/** 
	 * Get the target states of the labeled edge.
	 * @return a pointer to a BitSet with the indizes of the target states.
	 */
	public MyBitSet getEdge(APElement label) {
		return _edge_manager.getEdge(label);
	}

	/** Get the name (index) of this state. */
	public int getName() {
		return _graph.getIndexForState(this);
	}

	// FIXME: operator<<

	/** Is this state accepting (final)? */
	public boolean isFinal()
	{
		return _isFinal;
	}
  
	/** Set the value of the final flag for this state */
	public void setFinal(boolean f) {
		_isFinal=f;
		_graph.getFinalStates().set(_graph.getIndexForState(this), f);
	}

	// FIXME: ForEachSuccessor
  	// FIXME: edge_to_bitset_iterator
  	// FIXME: successor_iterator
  
	/** Set the description for this state. */
	public void setDescription(String s) {
		_description = s;
	}
	
	/** Get the description for this state. */
	String getDescription() {
		return _description;
	}

	/** Check if this state has a description. */
	public boolean hasDescription() {
		return (_description.length() != 0);
	}

	/** Get the automaton owning this state. */
	public NBA getGraph()
	{
		return _graph;
	}
	
	public Iterator<Map.Entry<APElement,MyBitSet>> iterator()
	{
		return _edge_manager.getEdgeContainer().entrySet().iterator();
	}
	
	public Iterator<Integer> successorIterator()
	{	
		MyBitSet successors = new MyBitSet(_graph.getStateCount());
		for (MyBitSet dest : _edge_manager.getEdgeContainer().values()) {
			successors.or(dest);
		}
		return new MyBitSet.MyBitSetIterator(successors);
	}
	
	/** The EdgeManager for the NBA_State */
	public static class EdgeManager {

		/** The state owning this EdgeManager */
		private NBA_State _state;
		/** The EdgeContainer */
		private HashMap<APElement, MyBitSet> _container;
		private int _apset_size;

		/**
		 * Constructor.
		 * @param state the NBA_State owning this EdgeManager
		 * @param apset the underlying APSet   
		 */
		public EdgeManager(NBA_State state, APSet apset) 
		{
			_apset_size = apset.size();
			_state = state;
			_container = new HashMap<APElement, MyBitSet>();
		}

		/** Get the target states */
		public MyBitSet getEdge(APElement label) {
			if (_container.get(label) == null)
				_container.put(label, new MyBitSet(_apset_size));
			return _container.get(label);
		}

		public void addEdges(APElement label, MyBitSet to)
		{
			_container.put(label, to);
		}

		/** Add an edge. */
		public void addEdge(APElement label, NBA_State state) {
			if (_container.get(label) == null)
				_container.put(label, new MyBitSet(_apset_size));
			_container.get(label).set(state.getName());
		}

		/** Add an edge. */
		public void addEdge(APMonom label, NBA_State state) {
			APSet ap_set = _state.getGraph().getAPSet();
			
			for (Iterator<APElement> it = label.APElementIterator(ap_set); it.hasNext(); ) {
				APElement cur = it.next();
				addEdge(cur, state);
				// System.out.println("State " + _state.getName() + " added edge to " + state.getName() + " through " + cur);
			}
		}

		/** Get the EdgeContainer. */
		public HashMap<APElement, MyBitSet> getEdgeContainer() {
			return _container;
		}
	}
 }
