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
import prism.PrismException;
import java.io.PrintStream;

import jltl2ba.APElement;
import jltl2ba.APSet;
import jltl2ba.MyBitSet;


/** @file
 * Provides class NBA to store a nondeterministic B�chi automaton.
 */

/**
 * A nondeterministic B�chi automaton.
 * See class DA for description of template parameters.
 */

public class NBA implements Iterable<NBA_State> {

	/** Number of states */
	  private int _state_count;
	  
	  /** Storage for the states */
	  private Vector<NBA_State> _index;

	  /** The underlying APSet */
	  private APSet _apset;
	  
	  /** The start states */
	  private NBA_State _start_state;

	  /** The states that are accepting (final) */
	  private MyBitSet _final_states;

	  /**
	   * Flag, telling whether to fail later on if the NBA is discovered
	   * to be disjoint, as this is indicative of a malfunctioning
	   * NBA generator.
	   */
	  private boolean _fail_if_disjoint = false;
	
	/**
	 * Constructor.
	 * @param apset The underlying APSet
	 */
	public NBA (APSet apset)
	{
		_state_count = 0;
		_start_state = null;
		_apset = apset;
		_index = new Vector<NBA_State>();
		_final_states = new MyBitSet();
	}
	
	/**
	 * Add a new state.
	 * @return a pointer to the newly generated state
	 */
	public NBA_State newState()
	{
		_state_count++;
		NBA_State state = new NBA_State(this);
	  
		_index.add(state);
		return state;
	}

	/** Get number of states. */
	public int size()
	{
		return _index.size();
	}

	//FIXME: ref_iterator
  
	/** Array index operator, get the state with index i. */
	public NBA_State get(int i)
	{
		return _index.get(i);
	}

	/** Get the size of the underlying APSet. */
	public int getAPSize()
	{
		return _apset.size();
	}

	/** Get a const reference to the underlying APSet. */
	public APSet getAPSet()
	{
		return _apset;
	}

	/** Switch the APSet to another with the same number of APs. */
	public void switchAPSet(APSet new_apset) throws PrismException
	{
		if (new_apset.size() != _apset.size()) {
			throw new PrismException("New APSet has to have the same size as the old APSet!");
		}
		_apset = new_apset;
	}

	/** Get the index for a state. */
	public int getIndexForState(NBA_State state)
	{
		return _index.indexOf(state);
	}

	/** Set the start state. */
	public void setStartState(NBA_State state)
	{
		_start_state = state;
	}

	/**
	 * Get the start state.
	 * @return the start state, or NULL if it wasn't set.
	 */
	public NBA_State getStartState()
	{
		return _start_state;
	}

	/** Get the set of final (accepting) states in the NBA */
	public MyBitSet getFinalStates()
	{
		return _final_states;
	}

	// public MyBitSet calculateFinalTrueLoops(SCCs sccs);

	/** 
	 * Remove states from the set of accepting (final) states when this is redundant.
	 * @param sccs the SCCs of the NBA
	 */
	public void removeRedundantFinalStates(SCCs sccs) {
		for (int scc = 0; scc < sccs.countSCCs(); ++scc) {
			if (sccs.get(scc).cardinality() == 1) {
				int state_id = sccs.get(scc).nextSetBit(0);
				NBA_State state = this.get(state_id);

				if (state.isFinal()) {
					if (!sccs.stateIsReachable(state_id, state_id)) {
						// The state is final and has no self-loop
						//  -> the final flag is redundant
						state.setFinal(false);
					}
				}
			}
		}
	}
	
	/**
	 * Checks if the NBA is deterministic (every edge has at most one target state).
	 */
	public boolean isDeterministic() {
		for (NBA_State state : _index) {
			for (Map.Entry<APElement, MyBitSet> edge : state) {
				if (edge.getValue().cardinality() > 1) {
					return false;
				}
			}
		}
		return true;
	}
	
	public NBA product_automaton(NBA nba_2) {
		
		NBA nba_1 = this;
		
		NBA product_nba = new NBA(nba_1.getAPSet());
	  
		APSet apset = nba_1.getAPSet();

		for (int s_1 = 0; s_1 < nba_1.size(); s_1++) {
			for (int s_2 = 0; s_2 < nba_2.size(); s_2++) {
				for (int copy = 0; copy < 2; copy++) {
					int s_r = product_nba.nba_i_newState();
					int to_copy = copy;
		
					if (copy == 0 && nba_1.get(s_1).isFinal()) {
						to_copy=1;
					}
					if (copy == 1 && nba_2.get(s_2).isFinal()) {
						product_nba.get(s_r).setFinal(true);
						to_copy = 0;
					}
		
					APElement label = new APElement(apset.size());
					for (int i = 0; i < (1<<apset.size()); i++) {
						MyBitSet to_s1 = nba_1.get(s_1).getEdge(label);
						MyBitSet to_s2 = nba_2.get(s_2).getEdge(label);
						MyBitSet to_set = new MyBitSet();
						for (int it_e_1 = to_s1.nextSetBit(0); it_e_1 != -1; it_e_1 = to_s1.nextSetBit(it_e_1)) {
							for (int it_e_2 = to_s2.nextSetBit(0); it_e_2 != -1; it_e_2 = to_s2.nextSetBit(it_e_2)) {
								int to = 2 * (it_e_1 * nba_2.size() + it_e_2) + to_copy;
								to_set.set(to);
							}
						}
						product_nba.get(s_r).addEdges(label, to_set);
						label.increment();
					}
				}
			}
		}
		int start_1 = nba_1.getStartState().getName();
		int start_2 = nba_2.getStartState().getName();
		product_nba.setStartState(product_nba.get(start_1 * nba_2.size() + start_2));
	  
		return product_nba;
	}

	/**
	 * Print the NBA on the output stream.
	 */
	public void print(PrintStream out) {
		for (NBA_State state : _index){
			out.print("State " + state.getName());
			if (getStartState() == state) {
				out.print(" *");
			}
			if (state.isFinal()) {
				out.print(" !");
			}
			out.println();

			for (Map.Entry<APElement, MyBitSet> edge : state) {
				APElement label = edge.getKey();
				MyBitSet to_states = edge.getValue();
				out.println(" " + label.toString(getAPSet(),true) + " -> " + to_states.toString());
			}
		}
	}

	
	
	// public void print_lbtt(std::ostream& out);
	// public void print_dot(std::ostream& out);

	/** Return number of states. */
	public int getStateCount()
	{
		return _state_count;
	}
	
	/** Set fail_if_disjoint flag */
	public void setFailIfDisjoint(boolean value)
	{
		_fail_if_disjoint = value;
	}
	
	/** Get fail_if_disjoint flag */
	public boolean getFailIfDisjoint()
	{
		return _fail_if_disjoint;
	}
	
	/** 
	 * Create a new state.
	 * @return the index of the new state
	 */
	public int nba_i_newState()
	{ 
		return newState().getName();
	}

	/**
	 * Add an edge from state <i>from</i> to state <i>to</i>
	 * for the edges covered by the APMonom.
	 * @param from the index of the 'from' state
	 * @param m the APMonom
	 * @param to the index of the 'to' state
	 */
	public void nba_i_addEdge(int from, APMonom m, int to)
	{
		this.get(from).addEdge(m, this.get(to));
	}

	/**
	 * Get the underlying APSet 
	 * @return a const pointer to the APSet
	 */
	public APSet nba_i_getAPSet()
	{
		return getAPSet();
	}

	/** 
	 * Set the final flag (accepting) for a state.
	 * @param state the state index
	 * @param final the flag
	 */
	public void nba_i_setFinal(int state, boolean f)
	{
		this.get(state).setFinal(f);
	}

	/**
	 * Set the state as the start state.
	 * @param state the state index
	 */
	public void nba_i_setStartState(int state)
	{
		setStartState(this.get(state));
	}
	
	public Iterator<NBA_State> iterator()
	{
		return _index.iterator();
	}
}
