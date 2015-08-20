/*
 * This file is part of a Java port of the program ltl2dstar
 * (http://www.ltl2dstar.de/) for PRISM (http://www.prismmodelchecker.org/)
 * Copyright (C) 2005-2007 Joachim Klein <j.klein@ltl2dstar.de>
 * Copyright (c) 2007 Carlos Bederian
 * Copyright (c) 2011- Hongyang Qu
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

import java.io.PrintStream;
import java.util.*;

import jltl2ba.APElement;
import jltl2ba.APSet;
import prism.PrismException;
import prism.PrismNotSupportedException;

public class DA {

	/**
	 * A class representing a deterministic omega automaton.
	 * <p>
	 * The template parameters:<br>
	 * Label: the type of the labeling on the edges<br>
	 * EdgeContainer: a template (taking the DA_State class as parameter)
	 *                providing an EdgeContainer for holding the edges in 
	 *                the states.<br>
	 * </p>
	 * <p>
	 *  Each state is identified by an index.<br>
	 *  There exists one start state.<br>
	 *  There exists an acceptance condition <br>
	 *  The DA is <i>compact</i>, if there are no holes in the indexes of the
	 *  states and the acceptance condition is compact as well.
	 * </p>
	 */

	/** The storage index for the states. */
	private Vector<DA_State> _index;

	/** The underlying APset. */
	private APSet _ap_set;

	/** The start state. */
	private DA_State _start_state;

	/** Flag to mark that the automaton is compact. */
	private boolean _is_compact;

	/** A comment */
	private String _comment;

	/** The acceptance condition for this automaton. */
	private RabinAcceptance _acceptance;

	/**
	 * Constructor.
	 * @param ap_set the underlying APSet.
	 */
	public DA(APSet ap_set)
	{
		_ap_set = ap_set; 
		_start_state = null; 
		_is_compact = true;
		_comment = "";
		_index = new Vector<DA_State>();
		_acceptance = new RabinAcceptance();
	}
	
	public static DA newInstance(APSet ap_set) {
		return new DA(ap_set);
	}

	/**
	 * Create a new state.
	 * @return a pointer to the new state.
	 */
	public DA_State newState()
	{
		DA_State state = new DA_State(this);
		int name = _index.size();
		state.setName(name);
		_index.add(state);
		_acceptance.addState(name);
		return state;
	}

	/** Make this automaton into an never accepting automaton */
	public void constructEmpty() {
		DA_State n = newState();
		setStartState(n);

		for (Iterator<APElement> it = getAPSet().elementIterator(); it.hasNext(); ){
			n.edges().put(it.next(), n);
		}
	}

	/** The number of states in the automaton.*/
	public int size()
	{
		return _index.size();
	}

	/** The type of an iterator over the states */
	public Iterator<DA_State> iterator()
	{
		return _index.iterator();
	}

	/**
	 * Array index operator, get the state with index i.
	 */
	public DA_State get(int i)
	{
		return _index.get(i);
	}

	/**
	 * Get the size of the underlying APSet.
	 */
	public int getAPSize()
	{
		return _ap_set.size();
	}

	/**
	 * Get a const reference to the underlying APSet.
	 */
	public APSet getAPSet()
	{
		return _ap_set;
	}

	/**
	 * Switch the APSet to another with the same number of APs.
	 */
	public void switchAPSet(APSet new_apset) throws PrismException
	{
		if (new_apset.size() != _ap_set.size()) {
			throw new PrismException("New APSet has to have the same size as the old APSet!");
		}
		_ap_set=new_apset;
	}

	/**
	 * Get the index for a state.
	 */
	public int getIndexForState(DA_State state)
	{
		return _index.indexOf(state);
	}

	/** Set the start state. */
	public void setStartState(DA_State state)
	{
		_start_state=state;
	}

	/**
	 * Get the start state.
	 * @return the start state, or NULL if it wasn't set.
	 */
	public DA_State getStartState()
	{
		return _start_state;
	}

	/** Checks if the automaton is compact. */
	public boolean isCompact()
	{
		return _is_compact && acceptance().isCompact();
	}

	/** Set a comment for the automaton. */
	public void setComment(String comment)
	{
		_comment=comment;
	}

	/** Get the comment for the automaton. */
	public String getComment()
	{
		return _comment;
	}

	/** Return reference to the acceptance condition for this automaton.
	 * @return reference to the acceptance condition
	 */
	public RabinAcceptance acceptance()
	{
		return _acceptance;
	}

	/**
	 * Reorder states and acceptance conditions so that
	 * the automaton becomes compact.
	 */
	public void makeCompact() throws PrismException {
		acceptance().makeCompact();

		if (!_is_compact) {
			int i, j;
			boolean moved = false;

			Vector<Integer> mapping = new Vector<Integer>(_index.size());
			for (i = 0, j = 0; i < _index.size(); i++) {
				if (_index.get(i) != null) {
					mapping.set(i, new Integer(j));
					if (j != i) {
						_index.set(j, _index.get(i));
						_index.set(i, null);
						moved = true;
					}
					j++;
				}
				else {
					while (_index.get(i) == null && i < _index.size()) {
						i++;
					}
					if (i < _index.size())
						break;
				}
			}

			if (moved) {
				_index.setSize(j);
				acceptance().moveStates(mapping);
			}
			_is_compact=true;
		}
	}


	/**
	 * Print the DA in v2 format to the output stream.
	 * This functions expects that the DA is compact.
	 * @param da_type a string specifying the type of automaton ("DRA", "DSA").
	 * @param out the output stream 
	 */
	public void	print(String da_type, PrintStream out) throws PrismException {
		// Ensure that this DA is compact...
		if (!this.isCompact()) {
			throw new PrismException("DA is not compact!");
		}

		if (this.getStartState() == null) {
			// No start state! 
			throw new PrismException("No start state in DA!");
		}

		out.println(da_type+" v2 explicit");
		
		if (_comment != "") {
			out.println("Comment: \"" + _comment + "\"");
		}
		out.println("States: " + _index.size());
		_acceptance.outputAcceptanceHeader(out);

		int start_state = this.getStartState().getName();
		out.println("Start: " + start_state);

		// Enumerate APSet
		out.print("AP: " + getAPSize());
		for (int ap_i = 0; ap_i < getAPSize(); ap_i++) {
			out.print(" \"" + getAPSet().getAP(ap_i) + "\"");
		}
		out.println();

		out.println("---");

		for (int i_state = 0; i_state < _index.size(); i_state++) {
			DA_State cur_state = _index.get(i_state);
			out.print("State: " + i_state);
			if (cur_state.hasDescription()) {
				out.print(" \"" + cur_state.getDescription() + "\"");
			}
			out.println();

			_acceptance.outputAcceptanceForState(out, i_state);
			
			Iterator<APElement> it = _ap_set.elementIterator();
			while (it.hasNext()) {
				APElement e = it.next();
				DA_State to = cur_state.edges().get(e);
				out.println(to.getName());
			}
		}
	}

	/**
	 * Print the DA in HOA format to the output stream.
	 * This functions expects that the DA is compact.
	 * @param da_type a string specifying the type of automaton ("DRA", "DSA").
	 * @param out the output stream
	 */
	public void printHOA(String da_type, PrintStream out) throws PrismException {
		if (!da_type.equals("DRA")) throw new PrismNotSupportedException("HOA printing for "+da_type+" currently not supported");

		out.println("HOA: v1");
		out.println("States: "+size());
		_ap_set.print_hoa(out);
		out.println("Start: "+getStartState().getName());
		_acceptance.outputAcceptanceHeaderHOA(out);
		out.println("properties: trans-labels explicit-labels state-acc no-univ-branch deterministic");
		out.println("--BODY--");
		for (DA_State state : _index) {
			out.print("State: "+state.getName()+ " ");  // id
			_acceptance.outputAcceptanceForStateHOA(out, state.getName());

			for (Map.Entry<APElement, DA_State> edge : state.edges().entrySet()) {
				APElement label = edge.getKey();
				String labelString = "["+label.toStringHOA(_ap_set.size())+"]";
				DA_State to = edge.getValue();
				out.print(labelString);
				out.print(" ");
				out.println(to);
			}
		}
		out.println("--END--");
	}

	/**
	 * Print the DA in dot format to the output stream.
	 * This functions expects that the DA is compact.
	 * @param da_type a string specifying the type of automaton ("DRA", "DSA").
	 * @param out the output stream 
	 */
	public void	printDot(String da_type, PrintStream out) throws PrismException {
		// Ensure that this DA is compact...
		if (!this.isCompact()) {
			throw new PrismException("DA is not compact!");
		}

		if (this.getStartState() == null) {
			// No start state! 
			throw new PrismException("No start state in DA!");
		}

		int start_state = this.getStartState().getName();

		out.println("digraph model {");
		for (int i_state = 0; i_state < _index.size(); i_state++) {
			if(i_state == start_state)
				out.println("	" + i_state + " [label=\"" + i_state + "\", shape=ellipse]");
			else {
				boolean isAcceptance = false;
				for (int ap_i = 0; ap_i < _acceptance.size(); ap_i++) {
					if(_acceptance.isStateInAcceptance_L(ap_i, i_state)) {
						out.println("	" + i_state + " [label=\"" + i_state + "\", shape=doublecircle]");
						isAcceptance = true;
						break;
					} else if(_acceptance.isStateInAcceptance_U(ap_i, i_state)) {
						out.println("	" + i_state + " [label=\"" + i_state + "\", shape=box]");
						isAcceptance = true;
						break;
					}
				}
				if(!isAcceptance)
					out.println("	" + i_state + " [label=\"" + i_state + "\", shape=circle]");
			}
		}
		for (int i_state = 0; i_state < _index.size(); i_state++) {
			DA_State cur_state = _index.get(i_state);
			for (Map.Entry<APElement, DA_State> transition : cur_state.edges().entrySet()) {
				out.println("	" + i_state + " -> " + transition.getValue().getName() + 
						" [label=\"" + transition.getKey().toString(_ap_set, true) + "\"]");
			}
		}
		out.println("}");
		
	}
}
