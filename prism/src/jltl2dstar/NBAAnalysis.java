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

/** @file
 * Provides class NBAAnalysis for performing analysis on non-deterministic B�chi automata.
 */

import java.util.*;

import jltl2ba.APElementIterator;
import jltl2ba.MyBitSet;


public class NBAAnalysis {

	/** 
	 * Perform (and cache) analysis for a given NBA.
	 */
	private NBA _nba;

	/** Information about the SCCs of the NBA (cached) */
	private SCCs _sccs;
	/** Information about the states where all the successor states are accepting (cached) */
	private MyBitSet _allSuccAccepting;
	/** Information about the states that have an accepting true self-loop (cached) */
	private MyBitSet _accepting_true_loops;
	/** Information about the reachability of states (cached) */
	private Vector<MyBitSet> _reachability;


	/** Constructor.
	 * @param nba the NBA to be analyzed
	 */
	public NBAAnalysis(NBA nba) {
		_sccs = null;
		_allSuccAccepting = null;
		_accepting_true_loops = null;
		_nba = nba;
	}						

	/** Get the SCCs for the NBA 
	 * @return the SCCs
	 */
	public SCCs getSCCs() {
		if (_sccs == null) {
			_sccs = new SCCs();
			GraphAlgorithms.calculateSCCs(_nba, _sccs, true);
		}
		return _sccs;
	}

	/** Get the states for which all successor states are accepting.
	 * @return BitSet with the information
	 */
	public MyBitSet getStatesWithAllSuccAccepting() {
		if (_allSuccAccepting == null) {
			calculateStatesWithAllSuccAccepting();
		}
		return _allSuccAccepting;
	}

	/** Get the states with accepting true self loops
	 * @return BitSet with the information
	 */
	public MyBitSet getStatesWithAcceptingTrueLoops() {
		if (_accepting_true_loops == null) {
			calculateAcceptingTrueLoops();
		}
		return _accepting_true_loops;
	}


	/** Checks to see if NBA has only accepting (final) states.
	 * @return true iff all states are accepting
	 */
	public boolean areAllStatesFinal() {
		for (NBA_State state : _nba) {
			if (!state.isFinal()) {
				return false;
			}
		}
		return true;
	}

	/** Get the accepting states from the NBA
	 * @return BitSet with the information
	 */
	public MyBitSet getFinalStates() {
		return _nba.getFinalStates();
	}

	/**
	 * Returns true if the NBA is disjoint, i.e., there are states
	 * that are not reachable from the initial state
	 */
	public boolean isNBADisjoint() {
		return getSCCs().getGraphIsDisjoint();
	}
	
	/** Get the reachability analysis for the NBA
	 * @return vector of BitSets representing the set of state which are reachable from a given state.
	 */
	public Vector<MyBitSet> getReachability() {
		if (_reachability == null) {
			_reachability = getSCCs().getReachabilityForAllStates();
		}

		return _reachability;
	}


	/** Check if the NBA is empty.
	 * @return true iff the NBA has no accepting run.
	 */
	public boolean emptinessCheck() {
		SCCs sccs = getSCCs();

		for (int scc = 0; scc < sccs.countSCCs(); ++scc) {
			MyBitSet states_in_scc = sccs.get(scc);

			// check to see if there is an accepting state in this SCC
			for (int state = states_in_scc.nextSetBit(0); state >= 0; state = states_in_scc.nextSetBit(state+1)) {
				if (_nba.get(state).isFinal()) {
					// check to see if this SCC is a trivial SCC (can't reach itself)

					if (states_in_scc.cardinality() == 1) {
						// there is only one state in this scc ...

						if (sccs.stateIsReachable(state,state) == false) {
							// ... and it doesn't loop to itself
							// -> can not guarantee accepting run
							continue;
						}
					}

					// if we are here, the SCC has more than 1 state or 
					// exactly one self-looping state
					//  -> accepting run

					// check that SCC can be reached from initial state
					assert(_nba.getStartState() != null);
					if (sccs.stateIsReachable(_nba.getStartState().getName(), state)) {
						return false;
					}
					continue;
				}
			}
		}
		return true;
	}


	/** 
	 * Calculates BitSet which specifies which states in the NBA 
	 * only have accepting successors.
	 */
	private void calculateStatesWithAllSuccAccepting() {
		_allSuccAccepting = new MyBitSet();
		MyBitSet result = _allSuccAccepting;
		SCCs sccs = getSCCs();

		MyBitSet scc_all_final = new MyBitSet(sccs.countSCCs());

		for (int sccIndex = sccs.countSCCs(); sccIndex > 0; --sccIndex) {
			// go backward in topological order...
			int scc = sccs.topologicalOrder().get(sccIndex-1);

			MyBitSet states_in_scc = sccs.get(scc);

			// check to see if all states in this SCC are final
			scc_all_final.set(scc);
			for (int it = states_in_scc.nextSetBit(0); it >= 0; it = states_in_scc.nextSetBit(it+1)) {
				if (!_nba.get(it).isFinal()) {
					scc_all_final.clear(scc);
					break;
				}
			}

			boolean might_be_final = false;
			if (!scc_all_final.get(scc)) {
				if (states_in_scc.cardinality() == 1) {
					// there is only one state in this scc ...
					int state = states_in_scc.nextSetBit(0);

					if (!sccs.stateIsReachable(state,state)) {
						// ... and it doesn't loop to itself
						might_be_final = true;
					}
				}
			}

			if (scc_all_final.get(scc) || might_be_final) {
				// Check to see if all successors are final...
				boolean all_successors_are_final = true;
				MyBitSet scc_succ = sccs.successors(scc);
				for (int it = scc_succ.nextSetBit(0); it >= 0; it = scc_succ.nextSetBit(it+1)) {
					if (!scc_all_final.get(it)) {
						all_successors_are_final = false;
						break;
					}
				}

				if (all_successors_are_final) {
					// Add all states in this SCC to the result-set
					result.or(states_in_scc);

					if (might_be_final) {
						scc_all_final.set(scc);
					}
				}
			}
		}
	}

	/** 
	 * Calculate the set of states that are accepting and have a true self loop.
	 */
	private void calculateAcceptingTrueLoops() {
		_accepting_true_loops = new MyBitSet();
		SCCs sccs = getSCCs();

		for (int scc = 0; scc < sccs.countSCCs(); ++scc) {
			if (sccs.get(scc).cardinality() == 1) {
				int state_id = sccs.get(scc).nextSetBit(0);
				NBA_State state = _nba.get(state_id);

				if (!state.isFinal()) {
					// not final, consider next
					continue;
				}

				if (!sccs.successors(scc).isEmpty()) {
					// there are edges leaving this state, consider next
					continue;
				}

				boolean no_empty_to = true;
				if (sccs.stateIsReachable(state_id, state_id)) {
					// state has at least one self-loop
					// we have to check that there is no edge with empty To
					for (APElementIterator it = new APElementIterator(_nba.getAPSize()); it.hasNext(); ) {
						// FIXME: this fills the state with null edges because of getEdge() sideeffects
						if (state.getEdge(it.next()).isEmpty()) {
							// not all edges lead back to the state...
							no_empty_to = false;
							break;
						}
					}

					if (no_empty_to) {
						// When we are here the state is a final true loop
						_accepting_true_loops.set(state_id);
						//	  std::cerr << "True Loop: " << state_id << std::endl;
					}
				}
			}
		}
	}
}