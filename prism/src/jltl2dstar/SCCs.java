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

import java.util.Vector;
import java.io.PrintStream;

import jltl2ba.MyBitSet;


public class SCCs {

	/**
	 * A class for storing information about the Strongly Connected Components (SCCs)
	 * of a graph
	 */
	public Vector<MyBitSet> _sccs;
	public Vector<Integer> _state_to_scc;
	public Vector<MyBitSet> _dag;
	public Vector<Integer> _topological_order;
	public Vector<MyBitSet> _reachability;
	public boolean _graph_is_disjoint;


	/** Constructor */
	public SCCs() {
		_sccs = new Vector<MyBitSet>();
		_state_to_scc = new Vector<Integer>();
		_dag = new Vector<MyBitSet>();
		_topological_order = new Vector<Integer>();
		_reachability = new Vector<MyBitSet>();
		_graph_is_disjoint = false;
	}
	
	/** Get the states that are in SCC scc_index */
	public MyBitSet get(int scc_index) {
		return _sccs.get(scc_index);
	}

	/** Get the number of SCCs */
	public int countSCCs() {return _sccs.size();}

	/** Get the SCC index for state */
	public int state2scc(int state) {
		return _state_to_scc.get(state);
	}

	/** Get a vector with a topological order of the states*/
	Vector<Integer> topologicalOrder() {
		return _topological_order;
	}

	/** Get a set of SCCs that are successors of the SCC scc_index */
	MyBitSet successors(int scc_index) {
		return _dag.get(scc_index);
	}

	/** Return true, if state_to is reachable from state_from */
	public boolean stateIsReachable(int state_from, int state_to) {
		return isReachable(state2scc(state_from), state2scc(state_to));
	}

	/** Return true, if SCC scc_to is reachable from SCC_fromom */
	public boolean isReachable(int scc_from, int scc_to) {
		return _reachability.get(scc_from).get(scc_to);
	}

	/** Print the SCCs on the output stream */
	public void print(PrintStream out) {
		out.println("SCC:");

		for (int i = 0; i < this.countSCCs(); i++) {
			int scc_i = this._topological_order.get(i);

			out.println(scc_i + " : " + get(scc_i));
		}
	}

	/** Get a vector of BitSets with reachability information 
	 * (state -> reachable_states)
	 */
	public Vector<MyBitSet> getReachabilityForAllStates() {
		Vector<MyBitSet> v = new Vector<MyBitSet>();
		v.setSize(_state_to_scc.size());

		for (int i = 0; i < _state_to_scc.size(); ++i) {
			int scc = state2scc(i);
			MyBitSet reachable_sccs = _reachability.get(scc);

			MyBitSet reachable_states = new MyBitSet();
			for (Integer it : reachable_sccs) {
				// union with all states from the reachable scc
				reachable_states.or(_sccs.get(it));
			}      

			v.set(i, reachable_states);

			//      std::cerr << "from "<<i<<": "<<reachable_states<<std::endl;
		}
		return v;
	}

	//  friend class GraphAlgorithms::SCC_DFS;

	/** Add a new SCC */
	public int addSCC(MyBitSet scc) {
		_sccs.add(scc);
		return _sccs.size() - 1;
	}

	/** Set the SCC for a state */
	public void setState2SCC(int state, int scc) {
		if (_state_to_scc.size() <= state) {
			_state_to_scc.setSize(state + 1);
		}
		_state_to_scc.set(state, new Integer(scc));
	}

	/** Set flag that the graph was discovered to be disjoint */
	public void setGraphIsDisjoint() {
		_graph_is_disjoint = true;
	}
	
	/** Get flag wether the graph was discovered to be disjoint */
	public boolean getGraphIsDisjoint() {
		return _graph_is_disjoint;
	}
}
