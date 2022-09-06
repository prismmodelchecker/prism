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
 * Provides class SafraTreeWalker  */

/** 
 * Walk a SafraTree and invoke the function 
 * <code>void visit(SafraTree& tree, SafraTreeNode *node)</code>
 * of the SafraTreeVisitor on each node of the tree.
 */
public class SafraTreeWalker<Visitor extends SafrasAlgorithm.SafraTreeVisitor> {

	/** The Visitor */
	private Visitor _visitor;

	/** Constructor.
	 *  @param visitor the visitor functor
	 */
	public SafraTreeWalker(Visitor visitor) {
		_visitor = visitor;
	}

	/** 
	 * Walk the tree post-order and call visit() on each node.
	 * @param tree the SafraTree
	 */
	public void walkTreePostOrder(SafraTree tree) {
		if (tree.getRootNode() == null) {return;}
		walkSubTreePostOrder(tree, tree.getRootNode(), true);
	}

	/** 
	 * Walk the subtree rooted at *top post-order and call visit() on each node.
	 * @param tree the SafraTree
	 * @param top the current subroot 
	 * @param visit_top if true, *top is visited too (former default: true)
	 */
	public void walkSubTreePostOrder(SafraTree tree, 
			SafraTreeNode top,
			boolean visit_top) {
		if (top.getChildCount() > 0) {
			for (SafraTreeNode cur_child : top)
				walkSubTreePostOrder(tree, cur_child, true);
		}

		if (visit_top) {
			_visitor.visit(tree, top);
		}
	}
}
