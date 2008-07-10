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

import java.util.Iterator;
import java.io.PrintStream;

import jltl2ba.MyBitSet;

/** @file
 * Provides class SafraTreeNode.
 */

/**
 * A node in a SafraTree.
 * Each node has: <br>
 * - a name/id (integer)<br>
 * - a final flag (bool)<br>
 * - a parent (NULL for root node)<br>
 * - an older brother (NULL if node is oldest)<br>
 * - a younger brother (NULL if node is youngest)<br>
 * - an oldestChild (NULL if node has no children<br>
 * - a youngestChild (NULL if node has no children<br>
 * - a label (BitSet) for the powerset 
 */
public class SafraTreeNode implements Iterable<SafraTreeNode> {

	/** The node name */
	private int _id;

	/** The label of the node (powerset) */
	private MyBitSet _labeling;

	/** The final flag */
	private boolean _final_flag;

	/** The parent node */
	private SafraTreeNode _parent;

	/** The older brother */
	private SafraTreeNode _olderBrother;

	/** The younger brother */
	private SafraTreeNode _youngerBrother;

	/** The oldest child */
	private SafraTreeNode _oldestChild;

	/** The youngest child */
	private SafraTreeNode _youngestChild;

	/** The number of children */
	private int _childCount;

	/** 
	 * Constructor 
	 * @param id the name of the node
	 */
	public SafraTreeNode(int id) {
		_id = id;
		_final_flag = false;
		_parent = null;
		_olderBrother = null;
		_youngerBrother = null;
		_oldestChild = null;
		_youngestChild = null;
		_childCount = 0;
		_labeling = new MyBitSet();
	}

	/** Get the name of the node*/
	public int getID() {return _id;}

	/** Get the final flag */
	public boolean hasFinalFlag() {return _final_flag;}

	/** Get the number of children. */
	public int getChildCount() {return _childCount;}

	/** Get the labeling */
	public MyBitSet getLabeling() {return _labeling;}

	/** 
	 * Get the older brother.
	 * @return the older brother, or NULL if node is oldest child.
	 */
	public SafraTreeNode getOlderBrother() {return _olderBrother;}

	/** 
	 * Get the younger brother.
	 * @return the younger brother, or NULL if node is youngest child.
	 */
	public SafraTreeNode getYoungerBrother() {return _youngerBrother;}

	/** 
	 * Get the oldest child.
	 * @return the oldest child, or NULL if node has no children
	 */
	public SafraTreeNode getOldestChild() {return _oldestChild;}

	/** 
	 * Get the youngest child.
	 * @return the youngest child, or NULL if node has no children
	 */
	public SafraTreeNode getYoungestChild() {return _youngestChild;}

	/** 
	 * Get the parent of the node.
	 * @return the parent, or NULL if node has no parent (is root node)
	 */
	public SafraTreeNode getParent() {return _parent;}

	/** 
	 * Set the final flag.
	 * @param finalFlag the value
	 */
	public void setFinalFlag(boolean finalFlag) {_final_flag=finalFlag;}
	
	/** 
	 * Set the labeling 
	 * @param labeling the new labeling
	 */
	public void setLabeling(MyBitSet labeling) {_labeling=labeling;}

	private boolean null_or_eq(SafraTreeNode a, SafraTreeNode b) {
		return (a == null && b == null) || ((a != null && b != null) && (a.getID() == b.getID()));
	}

	/** Equality operator. Does not do a deep compare */
	public boolean equals(SafraTreeNode other) {
		if (_id != other._id) {return false;}
		if (_final_flag != other._final_flag) {return false;}
		if (_childCount != other._childCount) {return false;}
		if (!_labeling.equals(other._labeling)) {return false;}
		if (!null_or_eq(_parent, other._parent)) {return false;}
		if (!null_or_eq(_olderBrother, other._olderBrother)) {return false;}
		if (!null_or_eq(_youngerBrother, other._youngerBrother)) {return false;}
		if (!null_or_eq(_oldestChild, other._oldestChild)) {return false;}
		if (!null_or_eq(_youngestChild, other._youngestChild)) {return false;}

		return true;
	}
	
	public boolean equals(Object other) {
		if (other instanceof SafraTreeNode) {
			return this.equals((SafraTreeNode) other);
		}
		else return false;
	}

	/** 
	 * Equality operator ignoring the name of the nodes, doing a deep compare
	 * (checks that all children are also structurally equal.
	 */
	public boolean structuralEquals(SafraTreeNode other) {
		if (!(_final_flag == other._final_flag)) {return false;}
		if (!(_childCount == other._childCount)) {return false;}
		if (!(_labeling.equals(other._labeling))) {return false;}

		if (_childCount > 0) {
			SafraTreeNode this_child = getOldestChild();
			SafraTreeNode other_child = other.getOldestChild();

			do {
				if (!this_child.structuralEquals(other_child)) {
					return false;
				}

				this_child = this_child.getYoungerBrother();
				other_child = other_child.getYoungerBrother();
			} while (this_child != null && other_child != null);

			assert(this_child == null && other_child == null);
		}
		return true;
	}

	private int null_or_cmpID(SafraTreeNode a, SafraTreeNode b) {
		if (a == null) {
			if (b == null) return 0;
			else return -1;
		}
		else {
			if (b == null) return 1;
			return a.getID() - b.getID();
		}
	}
	/** Less-than operator. Does not do deep compare */
	public boolean lessThan(SafraTreeNode other) {
		
		if (_id < other._id) return true;
		if (!_final_flag && other._final_flag) return true;
		if (_childCount < other._childCount) return true;
		if (_labeling.compareTo(other._labeling) < 0) return true;
		
		if (null_or_cmpID(_parent, other._parent) != 0)
			return (null_or_cmpID(_parent, other._parent) < 0);
		if (null_or_cmpID(_olderBrother, other._olderBrother) != 0)
			return (null_or_cmpID(_olderBrother, other._olderBrother) < 0);
		if (null_or_cmpID(_youngerBrother, other._youngerBrother) != 0)
			return (null_or_cmpID(_youngerBrother, other._youngerBrother) < 0);
		if (null_or_cmpID(_oldestChild, other._oldestChild) != 0)
			return (null_or_cmpID(_oldestChild, other._oldestChild) < 0);
		if (null_or_cmpID(_youngestChild, other._youngestChild) != 0)
			return (null_or_cmpID(_youngestChild, other._youngestChild) < 0);
		return false;
	}

	/** 
	 * Less-than operator ignoring the name of the nodes, doing a deep compare
	 * (applies recursively on the children).
	 */
	public boolean structuralLessThan(SafraTreeNode other) {
		return (this.compareTo(other) < 0);
	}
	
	/** Do a structural comparison */
	public int compareTo(SafraTreeNode other) {
		int cmp;
		
		if (_final_flag ^ other._final_flag)
			return (_final_flag ? 1 : -1);
		
		cmp = _childCount - other._childCount;
		if (cmp != 0) {return cmp;}
		
		cmp = _labeling.compareTo(other._labeling);
		if (cmp != 0) {return cmp;}
		
		// if we are here, this and other have the same number of children
		if (_childCount > 0) {
			SafraTreeNode this_child = getOldestChild();
			SafraTreeNode other_child = other.getOldestChild();

			do {
				cmp = this_child.compareTo(other_child);
				if (cmp != 0) {return cmp;}

				this_child = this_child.getYoungerBrother();
				other_child = other_child.getYoungerBrother();
			} while (this_child != null && other_child != null);

			// assert that there was really the same number of children
			assert(this_child == null && other_child == null);
		}

		// when we are here, all children were equal
		return 0;
	}

	/** Add a node as the youngest child */
	public void addAsYoungestChild(SafraTreeNode other) {
		assert(other != null);
		assert(other.getParent() == null);
		assert(other.getOlderBrother() == null);
		assert(other.getYoungerBrother() == null);

		if (_youngestChild != null) {
			assert(_youngestChild._youngerBrother == null);
			_youngestChild._youngerBrother = other;
			other._olderBrother = _youngestChild;
		}

		other._parent = this;
		_youngestChild = other;
		if (_oldestChild == null) {
			_oldestChild = other;
		}
		_childCount++;
	}

	/** Add a node as the oldest child */
	public void addAsOldestChild(SafraTreeNode other) {
		assert(other.getParent() == null);
		assert(other.getOlderBrother() == null);
		assert(other.getYoungerBrother() == null);

		if (_oldestChild != null) {
			assert(_oldestChild._olderBrother == null);
			_oldestChild._olderBrother = other;
			other._youngerBrother = _oldestChild;
		}

		other._parent = this;
		_oldestChild = other;
		if (_youngestChild == null) {
			_youngestChild = other;
		}
		_childCount++;
	}

	/** Remove this node from the tree (relink siblings). The node is not allowed 
	 * to have children! */
	public void removeFromTree() {
		assert(_childCount == 0);

		if (_parent == null) {
			// Root-Node or already removed from tree, nothing to do
			return;
		}

		// Relink siblings
		if (_olderBrother != null) {
			_olderBrother._youngerBrother = _youngerBrother;
		}
		if (_youngerBrother != null) {
			_youngerBrother._olderBrother = _olderBrother;
		}

		// Relink child-pointers in _parent
		if (_parent._oldestChild == this) {
			// this node is oldest child
			_parent._oldestChild = this._youngerBrother;
		}

		if (_parent._youngestChild == this) {
			// this node is youngest child
			_parent._youngestChild = this._olderBrother;
		}

		_parent._childCount--;

		_youngerBrother = null;
		_olderBrother = null;
		_parent = null;
	}

	/**
	 * Swap the places of two child nodes 
	 */
	public void swapChildren(SafraTreeNode a, SafraTreeNode b) {
		assert(a.getParent() == b.getParent() && a.getParent() == this);

		if (a == b) {return;}

		if (_oldestChild == a) {
			_oldestChild = b;
		} else if (_oldestChild == b) {
			_oldestChild = a;
		}

		if (_youngestChild == a) {
			_youngestChild = b;
		} else if (_youngestChild == b) {
			_youngestChild = a;
		}

		SafraTreeNode a_left = a._olderBrother;
		SafraTreeNode b_left = b._olderBrother;
		SafraTreeNode a_right = a._youngerBrother;
		SafraTreeNode b_right = b._youngerBrother;

		if (a_left != null) {a_left._youngerBrother = b;}
		if (b_left != null) {b_left._youngerBrother = a;}
		if (a_right != null) {a_right._olderBrother = b;}
		if (b_right != null) {b_right._olderBrother = a;}

		a._olderBrother = b_left;
		a._youngerBrother = b_right;
		b._olderBrother = a_left;
		b._youngerBrother = a_right;

		if (a_right == b) {
			// a & b are direct neighbours, a to the left of b
			a._olderBrother = b;
			b._youngerBrother = a;
		} else if (b_right == a) {
			// a & b are direct neighbours, b to the left of a
			a._youngerBrother = b;
			b._olderBrother = a;
		}
	}

	public Iterator<SafraTreeNode> iterator() {
		return new SafraTreeNodeIterator(this);
	}
	
	public static class SafraTreeNodeIterator implements Iterator<SafraTreeNode> {

		private SafraTreeNode _current;
		
		public SafraTreeNodeIterator(SafraTreeNode node) {
			_current = node.getOldestChild();
		}
		
		public SafraTreeNode next() {
			SafraTreeNode tmp = _current;
			_current = _current.getYoungerBrother();
			return tmp;
		}
		
		public boolean hasNext() {
			return (_current != null);
		}
		
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	/** Calculate the height of the subtree rooted at this node. */
	public int treeHeight() {
		int height = 0;

		if (getChildCount() > 0) {
			for (SafraTreeNode cur_child : this) {
				int child_height = cur_child.treeHeight();
				if (child_height > height) {
					height = child_height;
				}
			}
		}
		return height + 1;
	}

	/** Calculate the width of the subtree rooted at this node. */
	public int treeWidth() {
		int width = 0;

		if (getChildCount() > 0) {
			for (SafraTreeNode cur_child : this) {
				width += cur_child.treeWidth();
			}
		} 
		else width = 1;

		return width;
	}

	/** 
	 * Calculate a hashvalue using HashFunction for this node.
	 * @param hashfunction the HashFunction functor
	 * @param only_structure Ignore naming of the nodes?
	 */
	/* public void hashCode(HashFunction hashfunction,	boolean only_structure) {
		if (!only_structure) {
			hashfunction.hash(getID());
		}

		getLabeling().hashCode(hashfunction);
		hashfunction.hash(hasFinalFlag());

		if (getChildCount () >0) {
			for (SafraTreeNode c : this) { 
				c.hashCode(hashfunction, only_structure);
			}
		}
	}*/
	
	public int hashCode() {
		int hash = 1;
		hash += (hasFinalFlag() ? 1 : 0);
		hash = hash * 31 + getLabeling().hashCode();
		if (getChildCount () > 0) {
			for (SafraTreeNode c : this) { 
				hash = hash * 31 + c.hashCode();
			}
		}
		return hash;
	}
	
	/**
	 * Print node to output stream
	 */
	public void print(PrintStream out) {
		out.print(_id + " ");
		_labeling.print(out);
		if (_final_flag) {out.print(" !");}
	}

	/** Print HTML version of this node to output stream */
	public void toHTML(PrintStream out) {
		out.print(this.toHTMLString());
	}
	
	public String toHTMLString() {
		StringBuffer buf = new StringBuffer("<TABLE><TR>");
		
		if (getChildCount() <= 1) {
			buf.append("<TD>");
		} else {
			buf.append("<TD COLSPAN=\"");
			buf.append(getChildCount());
			buf.append("\">");
		}

		buf.append(getID());
		buf.append(" ");
		buf.append(_labeling);
		if (_final_flag) {buf.append("!");}
		buf.append("</TD></TR>");
		if (getChildCount() > 0) {
			buf.append("<TR>");
			for (SafraTreeNode child : this) {
				buf.append("<TD>");
				buf.append(child.toHTMLString());
				buf.append("</TD>");
			}
			buf.append("</TR>");
		}
		buf.append("</TABLE>");
		
		return buf.toString();
	}

}