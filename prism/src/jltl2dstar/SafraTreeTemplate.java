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

import jltl2ba.MyBitSet;

/** @file
 * Provides the class SafraTreeTemplate
 */

/**
 * A SafraTreeTemplate consists of a SafraTree and two BitSets,
 * one containing the names of nodes that may be renamed and on
 * containing the names that are not allowed in the tree.
 */
public class SafraTreeTemplate implements NBA2DAResult<SafraTree> {

	private SafraTree _safraTree;
	private MyBitSet _renameableNames;
	private MyBitSet _restrictedNames;

	/**
	 * Constructor.
	 * @param safraTree the SafraTree
	 */
	public SafraTreeTemplate(SafraTree safraTree)
	{
		_safraTree = safraTree;
		_renameableNames = new MyBitSet();
		_restrictedNames = new MyBitSet();
	}

	/** Get the SafraTree */
	public SafraTree getSafraTree() {return _safraTree;}

	/** Get the SafraTree */
	public SafraTree getState() {return _safraTree;}

	/** Get the names of nodes that may be renamed. */
	public MyBitSet renameableNames() {return _renameableNames;}

	/** Get the names that can are not allowed to be used in the Safra tree */
	public MyBitSet restrictedNames() {return _restrictedNames;}

	/** Set the 'renameable' flag for a name (former flag default: true) */
	public void setRenameable(int name, boolean flag) { _renameableNames.set(name, flag); }

	/** Get the 'renameable' flag for a name */
	public boolean isRenameable(int name) { return _renameableNames.get(name); }

	/** Set the 'restricted' flag for a name (former flag default: true) */
	public void setRestricted(int name, boolean flag) { _restrictedNames.set(name, flag); }

	/** Get the 'restricted' flag for a name */
	public boolean isRestricted(int name) { return _restrictedNames.get(name); }


	/**
	 * Return true if this tree (taking into account the renameableNames and the restrictedNames) 
	 * can be renamed to match the SafraTree other.
	 * Can only be called for trees that are structural_equal!!!
	 */
	public boolean matches(SafraTree other) {
		SafraTreeNode this_root = _safraTree.getRootNode();
		SafraTreeNode other_root = other.getRootNode();

		if (this_root == null || other_root == null) {
			assert(this_root == null && other_root == null);
			return true;
		}

		return matches(this_root, other_root);
	}


	/**
	 * Compare two subtrees to see if they match (taking into account the renameableNames
	 * and the restrictedNames).
	 */
	private boolean matches(SafraTreeNode this_node, SafraTreeNode other_node) {
		assert(this_node != null && other_node != null);

		if (this_node == null || other_node == null) {
			return false;
		}

		if (!renameableNames().get(this_node.getID())) {
			// this is not a new node, so we require a perfect match..
			if (other_node.getID() != this_node.getID()) {
				return false;
			}
		} else {
			// we are flexible with the id, as long as the id wasn't removed
			//  in the tree
			if (restrictedNames().get(other_node.getID())) {
				return false;
			}
		}

		assert(this_node.getLabeling().equals(other_node.getLabeling()));
		assert(this_node.hasFinalFlag() == other_node.hasFinalFlag());

		// this node looks good, now the children
		SafraTreeNode this_child = this_node.getOldestChild();
		SafraTreeNode other_child = other_node.getOldestChild();

		while (this_child != null && other_child != null) {
			if (!matches(this_child, other_child)) {
				return false;
			}

			this_child = this_child.getYoungerBrother();
			other_child = other_child.getYoungerBrother();
		}
		assert(this_child == null && other_child == null);

		return true;
	}
}
