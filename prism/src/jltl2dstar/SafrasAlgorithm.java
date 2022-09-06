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
 * Provides an implementation of the transition function of Safra's algorithm. 
 */
import java.util.Vector;

import jltl2ba.APElement;
import jltl2ba.MyBitSet;
import prism.PrismException;

/**
 * A class which calculates the transition function of Safra's algorithm.
 * You have to provide at least the final states of the NBA, the other
 * values have to be set, if the corresponding option is set.
 */
public class SafrasAlgorithm {

	private Options_Safra _options;
	private NBAAnalysis _nba_analysis;
	private NBA _nba;
	private int _NODES;

	private Vector<MyBitSet> _next;

	/** Caching the STVisitor_reorder_children, as it's initialization is complex. */
	private STVReorderChildren stv_reorder;

	/**
	 * Constructor
	 * @param nba The NBA
	 * @param options The options for Safra's algorithm
	 */
	public SafrasAlgorithm(NBA nba, Options_Safra options) throws PrismException {
		_options = options; 
		_nba_analysis = new NBAAnalysis(nba);
		_nba = nba;
		
		if (_nba.getFailIfDisjoint() && _nba_analysis.isNBADisjoint()) {
			throw new PrismException("The NBA generated for the LTL formula was discovered to be disjoint,\n"
			                       + "i.e., some states were not reachable from the initial state. This likely\n"
			                       + "indicates a problem in the translation. Please report the formula to the\n"
			                       + "PRISM developers");
		}
		
		_NODES = 2 * nba.getStateCount();
		stv_reorder = null; 
		_next = new Vector<MyBitSet>();
		_next.setSize(nba.getStateCount());
	}

	
	// typedef SafraTreeTemplate_ptr result_t;
	// typedef SafraTree_ptr state_t;

	public SafraTreeTemplate delta(SafraTree tree, APElement elem) throws PrismException {
		return process(tree, elem);
	}

	public SafraTree getStartState() {
		SafraTree start = new SafraTree(_NODES);
		if (_nba.getStartState() != null) {
			start.getRootNode().getLabeling().set(_nba.getStartState().getName());
		}

		return start;
	}

	public void prepareAcceptance(RabinAcceptance acceptance) {
		acceptance.newAcceptancePairs(_NODES);
	}

	public boolean checkEmpty() {
		if (_nba.size() == 0 || _nba.getStartState() == null) {
			return true;
		}
		return false;
	}

	/** 
	 * Get the next Safra tree using the
	 * transition function as described by Safra.
	 * @param tree the original tree
	 * @param elem the edge label
	 * @return a SafraTreeTemplate containing the new tree and 
	 *         bookkeeping which states were created/deleted.
	 */
	public SafraTreeTemplate process(SafraTree tree, APElement elem) {
		// Make copy of original tree
		SafraTree cur = new SafraTree(tree);
		SafraTreeTemplate tree_template = new SafraTreeTemplate(cur);
		// System.out.print("Init: "); tree_template.getState().print(System.out); System.out.println();

		STVResetFinalFlag stv_reset_flag = new STVResetFinalFlag();
		cur.walkTreePostOrder(stv_reset_flag);
		// System.out.print("Reset: "); tree_template.getState().print(System.out); System.out.println();
		
		STVCheckFinalSet stv_final = new STVCheckFinalSet(_nba_analysis.getFinalStates(), tree_template);
		cur.walkTreePostOrder(stv_final);
		// System.out.print("Final: "); tree_template.getState().print(System.out); System.out.println();

		STVPowerset stv_powerset = new STVPowerset(_nba, elem);
		cur.walkTreePostOrder(stv_powerset);
		// System.out.print("Powerset: "); tree_template.getState().print(System.out); System.out.println();


		/*
		 * Optimization: ACCEPTING_TRUE_LOOPS
		 */
		if (_options.opt_accloop) {
			if (cur.getRootNode() != null) {
				SafraTreeNode root = cur.getRootNode();

				if (_nba_analysis.getStatesWithAcceptingTrueLoops().intersects(root.getLabeling())) {
					// True Loop
					STVRemoveSubtree stv_remove = new STVRemoveSubtree(tree_template);
					cur.walkChildrenPostOrder(stv_remove, root);

					root.getLabeling().clear();

					int canonical_true_loop = _nba_analysis.getStatesWithAcceptingTrueLoops().nextSetBit(0);
					root.getLabeling().set(canonical_true_loop);
					root.setFinalFlag(true);

					// System.out.print("Accloop: "); tree_template.getState().print(System.out); System.out.println();
					return tree_template;
				}
			}
		}

		STVCheckChildrenHorizontal stv_horizontal = new STVCheckChildrenHorizontal();
		cur.walkTreePostOrder(stv_horizontal);
		// System.out.print("Horizontal: "); tree_template.getState().print(System.out); System.out.println();
		
		STVRemoveEmpty stv_empty = new STVRemoveEmpty(tree_template);
		cur.walkTreePostOrder(stv_empty);
		// System.out.print("Empty: "); tree_template.getState().print(System.out); System.out.println();

		STVCheckChildrenVertical stv_vertical = new STVCheckChildrenVertical(tree_template);
		cur.walkTreePostOrder(stv_vertical);
		// System.out.print("Vertical: "); tree_template.getState().print(System.out); System.out.println();

		/*
		 * Optimization: REORDER
		 */
		if (_options.opt_reorder) {
			if (stv_reorder == null) {
				stv_reorder = new STVReorderChildren(_nba_analysis.getReachability(), cur.getNodeMax());
			}

			cur.walkTreePostOrder(stv_reorder);
		}
		// System.out.print("Reorder: "); tree_template.getState().print(System.out); System.out.println();

		/*
		 * Optimization: ALL SUCCESSORS ARE ACCEPTING
		 */
		if (_options.opt_accsucc) {
			STVCheckForFinalSucc stv_succ = new STVCheckForFinalSucc(_nba_analysis.getStatesWithAllSuccAccepting(), tree_template);

			cur.walkTreePostOrder(stv_succ);
		}
		// System.out.print("Accsucc: "); tree_template.getState().print(System.out); System.out.println();
		return tree_template;
	}

	/** Visitors */
	public interface SafraTreeVisitor {
		public void visit(SafraTree tree, SafraTreeNode node);
	}
	
	/**
	 * A Safra tree visitor that modifies the
	 * children so that all children have
	 * disjoint labels
	 */
	public class STVCheckChildrenHorizontal implements SafraTreeVisitor {

		/** Node visitor */
		public void visit(SafraTree tree, SafraTreeNode node) {
			if (node.getChildCount() <= 1) {
				return;
			}

			MyBitSet already_seen = new MyBitSet();	// FIXME: although it's not used, hmph warnings
			boolean first = true;
			for (SafraTreeNode cur_child : node) {
				if (first) {
					already_seen = (MyBitSet) cur_child.getLabeling().clone();
					first = false;
				} else {
					MyBitSet current = cur_child.getLabeling();

					MyBitSet intersection = (MyBitSet) already_seen.clone(); // make copy
					if (intersection.intersects(current)) {
						// There are some labels, which occur in older brothers,
						// remove them from current node and its children
						STVSubstractLabeling stv_sub = new STVSubstractLabeling(intersection);
						tree.walkSubTreePostOrder(stv_sub, cur_child);
					}
					already_seen.or(current);
				}
			}
		}
	}

	/**
	 * A Safra tree visitor that ensures that 
	 * the union of the labels of the children
	 * are a proper subset of the label of the
	 * parents. Otherwise, the children are
	 * removed and the final flag is set on
	 * the tree node.
	 */
	public class STVCheckChildrenVertical implements SafraTreeVisitor {

		private SafraTreeTemplate _tree_template;

		public STVCheckChildrenVertical(SafraTreeTemplate tree_template) { 
			_tree_template = tree_template;
		}

		/** Node visitor */
		public void visit(SafraTree tree, SafraTreeNode node) {
			if (node.getChildCount() == 0) {return;}

			MyBitSet labeling_union = new MyBitSet();
			for (SafraTreeNode child : node) {
				labeling_union.or(child.getLabeling());
			}

			if (labeling_union.equals(node.getLabeling())) {
				// The union of the labelings of the children is exactly the 
				// same as the labeling of the parent ->
				//  remove children
				STVRemoveSubtree stv_remove = new STVRemoveSubtree(_tree_template);
				tree.walkChildrenPostOrder(stv_remove, node);

				node.setFinalFlag(true);
			}
		}
	}

	/**
	 * Safra tree visitor that creates a new child node if
	 * the label of the node and the set of final states in the
	 * NBA intersect.
	 */
	public class STVCheckFinalSet implements SafraTreeVisitor {

		private MyBitSet _final_states;
		private SafraTreeTemplate _tree_template;


		/**
		 * Constructor.
		 * @param final_states the states that are accepting (final) in the NBA
		 * @param tree_template the tree template to keep track of new nodes
		 */
		public STVCheckFinalSet(MyBitSet final_states, SafraTreeTemplate tree_template) {

			_final_states = final_states;
			_tree_template = tree_template;
		}

		public void visit(SafraTree tree, SafraTreeNode node) {
			if (_final_states.intersects(node.getLabeling())) {
				MyBitSet q_and_f = (MyBitSet) _final_states.clone();
				q_and_f.and(node.getLabeling());

				SafraTreeNode new_child = tree.newNode();
				node.addAsYoungestChild(new_child);

				_tree_template.setRenameable(new_child.getID(), true);

				new_child.setLabeling(q_and_f);
			}
		}
	}

	/**
	 * A Safra tree visitor that checks if all
	 * the successor states in the NBA of the label
	 * are accepting. If this is the case, all
	 * children are removed, and the final flag is set.
	 */
	public class STVCheckForFinalSucc implements SafraTreeVisitor {
		private boolean _success;
		private MyBitSet _nba_states_with_all_succ_final;
		private SafraTreeTemplate _tree_template;


		/** 
		 * Constructor 
		 * @param nba_states_with_all_succ_final A BitSet with the indizes of the
		 *                                       NBA states that only have accepting (final)
		 *                                       successors.
		 * @param tree_template                  SafraTreeTemplate to keep track of removed nodes
		 */
		public STVCheckForFinalSucc(MyBitSet nba_states_with_all_succ_final, SafraTreeTemplate tree_template) {
			_success = false; 
			_nba_states_with_all_succ_final = nba_states_with_all_succ_final;
			_tree_template = tree_template;
		}

		/** Returns true if the condition was triggered. */
		public boolean wasSuccessful() {return _success;}

		/** Node visitor */
		public void visit(SafraTree tree, SafraTreeNode node) {

			boolean all_final = true;
			for (int i = node.getLabeling().nextSetBit(0); i >= 0; i = node.getLabeling().nextSetBit(i+1)) {
				if (! _nba_states_with_all_succ_final.get(i)) {
					all_final = false;
					break;
				}
			}
			if (all_final) {
				// remove all children of node & set final flag
				STVRemoveSubtree stv_remove = new STVRemoveSubtree(_tree_template);
				tree.walkChildrenPostOrder(stv_remove, node);
				node.setFinalFlag(true);
				_success=true;
			}
		}
	}

	/**
	 * Safra tree visitor that performs the powerset construction
	 * on the label of the Safra tree node.
	 */
	public class STVPowerset implements SafraTreeVisitor {

		private NBA _nba;
		private APElement _elem;

		/**
		 * Constructor.
		 */
		public STVPowerset(NBA nba, APElement elem) {
			_nba = nba;
			_elem = elem;
		}

		/** Node visitor */  
		public void visit(SafraTree tree, SafraTreeNode node) {
			MyBitSet old_labeling = node.getLabeling();
			MyBitSet new_labeling = new MyBitSet(old_labeling.size());
			for (int i = old_labeling.nextSetBit(0); i >= 0; i = old_labeling.nextSetBit(i+1)) {
				new_labeling.or(_nba.get(i).getEdge(_elem));
			}
			node.setLabeling(new_labeling);
		}
	}

	/**
	 * A Safra tree visitor that removes tree nodes
	 * with empty labels.
	 */
	public class STVRemoveEmpty implements SafraTreeVisitor {

		private	SafraTreeTemplate _tree_template;

		public STVRemoveEmpty(SafraTreeTemplate tree_template) {
			_tree_template = tree_template;
		}

		/** Node visitor */
		public void visit(SafraTree tree, SafraTreeNode node) {
			if (node.getLabeling().isEmpty()) {
				int id = node.getID();
				if (_tree_template.isRenameable(id)) {
					// this node was created recently, so we only delete it in
					// renameableNames, but don't mark it in restrictedNodes
					_tree_template.setRenameable(id, false);
				} else {
					_tree_template.setRestricted(id, true);
				}

				tree.remove(node);
			}
		}
	}

	/**
	 * A Safra tree visitor that removes all 
	 * children of the node.
	 */
	public class STVRemoveSubtree implements SafraTreeVisitor {

		private SafraTreeTemplate _tree_template;

		public STVRemoveSubtree(SafraTreeTemplate tree_template) {
			_tree_template = tree_template;	
		}

		/** Node visitor */
		public void visit(SafraTree tree, SafraTreeNode node) {
			int id = node.getID();
			if (_tree_template.isRenameable(id)) {
				// this node was created recently, so we only delete it from
				// the renameableNames, but don't mark it in restrictedNames
				_tree_template.setRenameable(id, false);
			} else {
				_tree_template.setRestricted(id, true);
			}

			tree.remove(node);
		}
	}

	/**
	 * Safra tree visitor that attempts
	 * to reorder the independant children 
	 * into a canonical order.
	 * Two children are independet if
	 * their is no state that is reachable by 
	 * states in both labels.
	 */
	public class STVReorderChildren implements SafraTreeVisitor {

		private Vector<MyBitSet> _nba_reachability;
		private MyBitSet[] _node_reachability;
		private int[] _node_order;

		/**
		 * Constructor
		 * nba_reachability A vector of BitSets (state index -> BitSet) of states
		 *                  in the NBA that are reachable from a state.
		 * N                the maximum number of nodes in the Safra tree
		 */
		public STVReorderChildren(Vector<MyBitSet> nba_reachability, int N) {
			_nba_reachability = nba_reachability;
			_node_order = new int[N];
			_node_reachability = new MyBitSet[N];
			for (int i = 0; i < N; i++) {
				_node_reachability[i] = new MyBitSet();
			}
		}

		/** Node visitor */
		public void visit(SafraTree tree, SafraTreeNode node) {
			if (node.getChildCount() <= 1) {return;}

			int i = 0;
			for (SafraTreeNode child : node) {
				MyBitSet reachable_this = _node_reachability[child.getID()];
				reachable_this.clear();
				_node_order[child.getID()] = i++;

				MyBitSet label_this = child.getLabeling();
				for (int setbit = label_this.nextSetBit(0); setbit >= 0; setbit = label_this.nextSetBit(setbit+1)) {
					reachable_this.or(_nba_reachability.get(setbit));
				}
				//      std::cerr << "reachability_this: "<<reachable_this << std::endl; 
			}

			// reorder...
			//    std::cerr << "Sorting!" << std::endl;

			// Bubble sort, ough!
			boolean finished = false;
			while (!finished) {
				finished=true;

				for (SafraTreeNode a = node.getOldestChild();
				a != null && a.getYoungerBrother() != null;
				a = a.getYoungerBrother()) {

					SafraTreeNode b = a.getYoungerBrother();

					MyBitSet reach_a = _node_reachability[a.getID()];
					MyBitSet reach_b = _node_reachability[b.getID()];

					if (reach_a.intersects(reach_b)) {
						// a and b are not independant...
						// --> keep relative order...
						assert(_node_order[a.getID()] < _node_order[b.getID()]);
					} else {
						// a and b are independant...
						if (a.getLabeling().compareTo(b.getLabeling()) >= 0) {
							// swap
							node.swapChildren(a,b);
							a=b;
							finished=false;	    
						}
					}
				}
			}
		}
	}

	/**
	 * Safra tree visitor that resets the final flag on the Safra tree node.
	 */
	public class STVResetFinalFlag implements SafraTreeVisitor {
		
		/** Node visitor */
		public void visit(SafraTree tree, SafraTreeNode node) {
			node.setFinalFlag(false);
		}
	}
	
	/**
	 * A Safra tree visitor that subtracts (minus operator) a BitSet from
	 * the label of the tree node.
	 */
	public class STVSubstractLabeling implements SafraTreeVisitor {

		private MyBitSet _bitset;

		public STVSubstractLabeling(MyBitSet bitset) {
			_bitset = bitset;
		}
		
		public void visit(SafraTree tree, SafraTreeNode node) {
			node.getLabeling().andNot(_bitset);
		}  
	}
}
