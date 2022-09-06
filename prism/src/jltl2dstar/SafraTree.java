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

/** 
 * A Safra tree, an ordered tree of SafraTreeNodes.
 */
public class SafraTree implements NBA2DAState {

	/** The maximum number of nodes */
	private int MAX_NODES;
	/** An array to store the nodes */
	private Vector<SafraTreeNode> _nodes;

	/** 
	 * Constructor.
	 * @param N the maximum number of nodes.
	 */
	public SafraTree(int N) {
		MAX_NODES = (N == 0 ? 1 : N);
		_nodes = new Vector<SafraTreeNode>(MAX_NODES);
		_nodes.setSize(MAX_NODES);

		// create root-node
		newNode(0);
	}

	/** Copy constructor. */
	public SafraTree(SafraTree other) {
		MAX_NODES = other.MAX_NODES;

		_nodes = new Vector<SafraTreeNode>(MAX_NODES);
		_nodes.setSize(MAX_NODES);
		for (int i = 0; i < MAX_NODES; i++) {
			if (other._nodes.get(i) != null) {
				newNode(i);
				_nodes.get(i).setLabeling((MyBitSet) other._nodes.get(i).getLabeling().clone());
				_nodes.get(i).setFinalFlag(other._nodes.get(i).hasFinalFlag());
			}
		}

		copySubTree(_nodes.get(0), other._nodes.get(0));
	}
	
	/** Get the root node of the tree. */
	public SafraTreeNode getRootNode() {return _nodes.get(0);}

	/** Create a new node. The name is the next free node name. */
	public SafraTreeNode newNode() {
		if (_nodes.indexOf(null) != -1)
			return newNode(_nodes.indexOf(null));	// FIXME: hmm, inconsistent with newNode(int)
		else return null;
	}

	/** Create a new node with name <i>id</i>. */
	public SafraTreeNode newNode(int id) {
		assert(id < MAX_NODES);
		assert(_nodes.get(id) == null);

		_nodes.set(id,new SafraTreeNode(id));

		return _nodes.get(id);
	}

	/** 
	 * Remove a SafraTreeNode from the tree, 
	 * the node can have no children.
	 */
	public void remove(SafraTreeNode node) {
		assert(_nodes.get(node.getID()) == node);
		remove(node.getID());
	}

	/** 
	 * Remove the SafraTreeNode <i>id</i> from the tree,
	 * the node can have no children.
	 */
	public void remove(int id) {
		assert(id >= 0 && id < MAX_NODES);

		_nodes.get(id).removeFromTree();
		_nodes.set(id, null);
	}


	/**
	 * Remove all children of the SafraTreeNode <i>id</i>.
	 */
	public void removeAllChildren(int id) {
		assert(id < MAX_NODES);

		SafraTreeNode n = _nodes.get(id);
		SafraTreeNode child;
		while ((child = n.getOldestChild()) != null) {
			removeAllChildren(child.getID());
			remove(child.getID());
		}
	}

	/** 
	 * Walk the tree post-order, calling the function 
	 * void visit(SafraTree& tree, SafraTreeNode *node) 
	 * in the SafraTreeVisitor on each node.
	 */
	public <V extends SafrasAlgorithm.SafraTreeVisitor> void walkTreePostOrder(V visitor) {
		SafraTreeWalker<V> stw = new SafraTreeWalker<V>(visitor);
		stw.walkTreePostOrder(this);
	}

	/** 
	 * Walk the subtree rooted under node *top post-order, 
	 * calling the function void visit(SafraTree& tree, SafraTreeNode *node) 
	 * in the SafraTreeVisitor on each node.
	 */
	public <V extends SafrasAlgorithm.SafraTreeVisitor> void walkSubTreePostOrder(V visitor, SafraTreeNode top) {
		SafraTreeWalker<V> stw = new SafraTreeWalker<V>(visitor);
		stw.walkSubTreePostOrder(this, top, true);
	}

	/** 
	 * Walk the subtree rooted under node *top (only the children, not *top itself) 
	 * post-order, calling the function void visit(SafraTree& tree, SafraTreeNode *node) 
	 * in the SafraTreeVisitor on each node.
	 */
	public <V extends SafrasAlgorithm.SafraTreeVisitor> void walkChildrenPostOrder(V visitor, SafraTreeNode top) {
		SafraTreeWalker<V> stw = new SafraTreeWalker<V>(visitor);
		stw.walkSubTreePostOrder(this, top, false); // = don't visit top
	}


	/**
	 * Calculate the height of the tree.
	 */
	public int treeHeight() {
		if (getRootNode() != null) {
			return getRootNode().treeHeight();
		}
		return 0;
	}


	/**
	 * Calculate the width of the tree.
	 */
	public int treeWidth() {
		if (getRootNode() != null) {
			return getRootNode().treeWidth();
		}
		return 0;
	}

	/**
	 * Equality operator.
	 */
	public boolean equals(SafraTree other) {
		if (other.MAX_NODES != MAX_NODES) {return false;}
		return _nodes.equals(other._nodes);
	}

	public boolean equals(Object other) {
		if (other instanceof SafraTree)
			return this.equals((SafraTree) other);
		else return false;
	}
		
	/**
	 * Checks equality when ignoring the node names.
	 */
	public boolean structural_equal_to(SafraTree other) {
		if (other.MAX_NODES!=MAX_NODES) {return false;}

		SafraTreeNode this_root = this.getRootNode();
		SafraTreeNode other_root = other.getRootNode();

		if (this_root == null || other_root == null) {
			// return true if both are 0
			return (this_root==other_root);
		}

		return this_root.structuralEquals(other_root);
	}

	/**
	 * Less-than operator when ignoring the node names.
	 */
	public boolean structural_less_than(SafraTree other) {
		if (other.MAX_NODES<MAX_NODES) {return true;}

		SafraTreeNode this_root = this.getRootNode();
		SafraTreeNode other_root = other.getRootNode();

		if (this_root == null) {
			if (other_root!= null) {
				return true;
			} else {
				return false;
			}
		} else { // this_root !=0 
			if (other_root == null) {return false;}

			return this_root.structuralLessThan(other_root);
		}
	}

	/**
	 * Less-than operator
	 */
	public boolean lessThan(SafraTree other) {
		if (MAX_NODES < other.MAX_NODES) {return true;}

		for (int i = 0; i < MAX_NODES; i++) {
			if (_nodes.get(i) == null && other._nodes.get(i) == null) {
				;
			} else if (_nodes.get(i) == null) {
				return true;
			} else if (other._nodes.get(i) == null) {
				return false;
			} else {
				if (_nodes.get(i).lessThan(other._nodes.get(i))) {
					return true;
				} else if (_nodes.get(i).equals(other._nodes.get(i))) {
					;
				} else {
					return false;
				}
			}
		}
		return false;
	}

	/** Get the maximum number of nodes. */
	public int getNodeMax() {return MAX_NODES;}

	/** Get SafraTreeNode with index <i>i</i>*/
	public SafraTreeNode get(int i) {
		return _nodes.get(i);
	}

	public void set(int i, SafraTreeNode node) {
		_nodes.set(i, node);
	}

	/** Print the SafraTree on an output stream. */
	public void print(PrintStream out) {
		if (getRootNode() == null) {
			out.println("<empty>");
		} else {
			printSubTree(out, 0, getRootNode());
		}
	}

	/** Returns a string representation of the SafraTree */
	// public String toString() {
	// std::ostringstream buf;
	// buf << *this;
	// return buf.str();
	// }

	/** Returns a string representation in HTML of the SafraTree */
	public String toHTML() {
		if (getRootNode() == null) {
			return "<TABLE><TR><TD>[empty]</TD></TR></TABLE>";
		} else {
			return getRootNode().toHTMLString();
		}
	}

	/**
	 * Calculate a hash value using HashFunction
	 * @param hashfunction the HashFunction
	 * @param only_structure ignore the nameing of the nodes
	 */
	// template <class HashFunction>
	// public void hashCode(HashFunction& hashfunction,
	// 		bool only_structure=false) {
	// SafraTreeNode* root=getRootNode();
	// 
	// 		if (root!=0) {
	// 			root->hashCode(hashfunction, only_structure);
	// 		}
	// }
	
	public int hashCode() {
		if (getRootNode() != null)
			return getRootNode().hashCode();
		else return 0;
	}

	/**
	 * Generate the appropriate acceptance signature for Rabin Acceptance for this tree  
	 */
	public void generateAcceptance(AcceptanceForState acceptance) {
		for (int i = 0; i < getNodeMax(); i++) {
			SafraTreeNode stn = this.get(i);
			if (stn == null) {
				acceptance.addTo_U(i);
			} else {
				if (stn.hasFinalFlag()) {
					acceptance.addTo_L(i);
				}
			}
		}
	}

	public void generateAcceptance(RabinSignature acceptance) {
		acceptance.setSize(getNodeMax());
		for (int i = 0; i < getNodeMax(); i++) {
			SafraTreeNode  stn = this.get(i);
			if (stn == null) {
				acceptance.setColor(i, RabinAcceptance.RabinColor.RABIN_RED);
			} else {
				if (stn.hasFinalFlag()) {
					acceptance.setColor(i, RabinAcceptance.RabinColor.RABIN_GREEN);
				} else {
					acceptance.setColor(i, RabinAcceptance.RabinColor.RABIN_WHITE);
				}
			}
		}
	}

	public RabinSignature generateAcceptance() {
		RabinSignature s = new RabinSignature(getNodeMax());
		generateAcceptance(s);
		return s;
	}


	/**
	 * Copy the subtree (the children) of *other
	 * to *top, becoming the children of *top
	 */
	private void copySubTree(SafraTreeNode top, SafraTreeNode other) {
		if (other == null) {return;}

		for (SafraTreeNode child : other) {
			SafraTreeNode n = _nodes.get(child.getID());
			top.addAsYoungestChild(n);
			copySubTree(n, child);
		}
	}

	/**
	 * Print the subtree rooted at node *top to the output stream
	 * @param out the output stream
	 * @param prefix the number of spaces ' ' in front of each node
	 * @param top the current tree sub root
	 */
	private void printSubTree(PrintStream out, int prefix, SafraTreeNode top) {
		for (int i = 0; i < prefix; i++) {
			out.print(" ");
		}
		top.print(out);
		out.println();

		for (SafraTreeNode child : top) {
			printSubTree(out, prefix+1, child);
		}
	}
}
