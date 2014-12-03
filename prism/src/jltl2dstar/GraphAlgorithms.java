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
import java.util.Stack;
import java.util.Iterator;

import jltl2ba.MyBitSet;

/** @file
 * Provides algorithms to be used on graphs (automata), notably the calculation
 * of Strongly Connected Components (SCCs).
 */

public class GraphAlgorithms {
	
	/** 
	 * Calculate the SCCs for Graph graph and save in result.
	 * disjoint default = false 
	 */
	public static void calculateSCCs(NBA graph, SCCs result, boolean disjoint) {
		SCC_DFS scc_dfs = new SCC_DFS(graph, result);
		scc_dfs.calculate(disjoint);
	}
	
	public static class SCC_DFS {
		
		public static class SCC_DFS_Data {
			/** A class for saving DFS state information */
			public int dfs_nr;
			public int root_index;
			public boolean inComponent;
		}

		/** The graph */
		private NBA _graph;

		/** The SCCs */
		private SCCs _result;

		/** The current DFS number */
		private int current_dfs_nr;

		/** The DFS stack */
		private Stack<Integer> _stack;

		/** The SCC_DFS_Data for every state (state index -> DFS_DATA) */
		private Vector<SCC_DFS_Data> _dfs_data;

		/** The current scc number */
		private int scc_nr;

		/** Constructor */
		public SCC_DFS(NBA graph, SCCs result) {
			_graph = graph;
			_result = result;
			_stack = new Stack<Integer>();
			_dfs_data = new Vector<SCC_DFS_Data>();
		}

		/** Calculate the SCCs*/
		public void calculate(boolean disjoint) {
			current_dfs_nr = 0;
			_dfs_data.clear();
			// Ensure there are as many entries as there are graph-states
			_dfs_data.setSize(_graph.size());
			scc_nr = 0;

			NBA_State start_state = _graph.getStartState();
			if (start_state == null) {
				return;
			}

			int start_idx = start_state.getName();
			visit(start_idx);
			if (disjoint) {
				// The Graph may be disjoint -> restart DFS on every not yet visited state 
				for (int v = 0; v < _graph.size(); ++v) {
					if (_dfs_data.get(v) == null) {
						// not yet visited, i.e., not reachable from the start state
						_result.setGraphIsDisjoint();
						visit(v);
					}
				}
			}

			calculateDAG();
		}

		/** Visit a state (perform DFS) */
		private void visit(int v) {
			SCC_DFS_Data sdd = new SCC_DFS_Data();
			sdd.dfs_nr = current_dfs_nr++;
			sdd.root_index = v;
			sdd.inComponent = false;

			_stack.push(v);
			_dfs_data.set(v, sdd);

			for (Iterator<Integer> it = _graph.get(v).successorIterator(); it.hasNext(); ) {
				int w = it.next();

				if (_dfs_data.get(w) == null) {
					// not yet visited
					visit(w);
				}

				SCC_DFS_Data sdd_w = _dfs_data.get(w);
				if (sdd_w.inComponent == false) {
					int dfs_nr_root_v = _dfs_data.get(sdd.root_index).dfs_nr;
					int dfs_nr_root_w = _dfs_data.get(sdd_w.root_index).dfs_nr;

					if (dfs_nr_root_v > dfs_nr_root_w) {
						sdd.root_index = sdd_w.root_index;
					}
				}
			}

			if (sdd.root_index == v) {
				MyBitSet set = new MyBitSet();

				int w;
				do {
					w = _stack.pop();

					set.set(w);
					_result.setState2SCC(w, scc_nr);

					SCC_DFS_Data sdd_w = _dfs_data.get(w);
					sdd_w.inComponent=true;
				} while (w != v);

				scc_nr = _result.addSCC(set) + 1;
			}
		}

		/** Calculate the Directed Acyclical Graph (DAG) */
		private void calculateDAG() {
			_result._dag.clear();
			_result._dag.setSize(_result.countSCCs());
			_result._reachability.setSize(_result.countSCCs());

			int[] in_degree = new int[_result.countSCCs()];

			for (int scc = 0; scc < _result.countSCCs(); ++scc) {
				// Init
				_result._dag.set(scc, new MyBitSet());
				_result._reachability.set(scc, new MyBitSet());
				
				MyBitSet states_in_scc = _result.get(scc);

				for (int from_state = states_in_scc.nextSetBit(0); from_state >= 0; from_state = states_in_scc.nextSetBit(from_state + 1)) {
					for (Iterator<Integer> succ_it = _graph.get(from_state).successorIterator(); succ_it.hasNext(); ) {
						int to_state = succ_it.next();
						int to_scc = _result.state2scc(to_state);

						if (to_scc != scc) {
							// Only successor in the DAG if not the same scc
							if (!_result._dag.get(scc).get(to_scc)) {
								// This SCC is a new successor, increment in_degree
								in_degree[to_scc]++;
								_result._dag.get(scc).set(to_scc);
							}
						}

						// Reachability
						_result._reachability.get(scc).set(to_scc);
					}
				}
			}

			boolean progress = true;
			int cnt = 0;
			_result._topological_order.clear();
			_result._topological_order.setSize(_result.countSCCs());

			int[] sort = new int[_result.countSCCs()];
			while (progress) {
				progress=false;

				for (int scc = 0; scc < _result.countSCCs(); ++scc) {
					if (in_degree[scc] == 0) {
						sort[scc] = cnt++;
						progress = true;
						in_degree[scc] = -1;

						for (Integer scc_to : _result._dag.get(scc)) {
							in_degree[scc_to]--;
						}
					}
				}
			}

			for (int i = 0; i < _result.countSCCs(); i++) {
				_result._topological_order.set(sort[i], i);
			}


			// traverse SCCs in reverse topological order
			for (int i = _result.countSCCs(); i > 0; --i) {
				int cur_scc = _result._topological_order.get(i-1);

				MyBitSet reaches = _result._reachability.get(cur_scc);

				for (Integer scc_to : _result._dag.get(cur_scc)) {
					reaches.or(_result._reachability.get(scc_to));
				}
			}
		}
	}

}
