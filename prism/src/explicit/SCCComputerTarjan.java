//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Christian von Essen <christian.vonessen@imag.fr> (Verimag, Grenoble)
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package explicit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.IntPredicate;

import prism.PrismComponent;
import prism.PrismException;

/**
 * Tarjan's SCC algorithm operating on a Model object.
 */
public class SCCComputerTarjan extends SCCComputer
{
	/* The model to compute (B)SCCs for */
	private Model model;
	/* Number of nodes (model states) */
	private int numNodes;

	/* Next index to give to a node */
	private int index = 0;
	/* Stack of nodes */
	private List<Integer> stack = new LinkedList<Integer>();
	/* List of nodes in the graph. Invariant: {@code nodeList.get(i).id == i} */
	private ArrayList<Node> nodeList;
	/* Nodes currently on the stack. */
	private BitSet onStack;
	/** Should we filter trivial SCCs? */
	private boolean filterTrivialSCCs;
	private IntPredicate restrict;

	/**
	 * Build (B)SCC computer for a given model.
	 */
	public SCCComputerTarjan(PrismComponent parent, Model model, SCCConsumer consumer) throws PrismException
	{
		super(parent, consumer);
		this.model = model;
		this.numNodes = model.getNumStates();
		this.nodeList = new ArrayList<Node>(numNodes);
		for (int i = 0; i < numNodes; i++) {
			nodeList.add(new Node(i));
		}
		onStack = new BitSet();
	}

	// Methods for SCCComputer interface

	@Override
	public void computeSCCs(boolean filterTrivialSCCs, IntPredicate restrict) throws PrismException
	{
		this.filterTrivialSCCs = filterTrivialSCCs;
		consumer.notifyStart(model);
		this.restrict = restrict;
		tarjan();
		consumer.notifyDone();
	}

	// SCC Computation

	/**
	 * Execute Tarjan's algorithm. Determine maximal strongly connected components
	 * (SCCS) for the graph of the model and stored in {@code sccs}.
	 */
	public void tarjan() throws PrismException
	{
		for (int i = 0; i < numNodes; i++) {
			if (restrict != null && !restrict.test(i))
				continue; // skip state if not one of the relevant states
			if (nodeList.get(i).lowlink == -1)
				tarjan(i);
		}

	}

	private void tarjan(int i) throws PrismException
	{
		final Node v = nodeList.get(i);
		v.index = index;
		v.lowlink = index;
		index++;
		stack.add(0, i);
		onStack.set(i);

		boolean hadSelfloop = false;
		SuccessorsIterator it = model.getSuccessors(i);
		while (it.hasNext()) {
			int e = it.nextInt();

			if (e == i) {
				hadSelfloop = true;
				continue;
			}

			if (restrict != null && !restrict.test(e)) {
				continue; // ignore edge to state that is not relevant
			}

			Node n = nodeList.get(e);
			if (n.index == -1) {
				tarjan(e);
				v.lowlink = Math.min(v.lowlink, n.lowlink);
			} else if (onStack.get(e)) {
				v.lowlink = Math.min(v.lowlink, n.index);
			}
		}
		if (v.lowlink == v.index) {
			// this is a singleton SCC if the top of the stack equals i
			boolean singletonSCC = (stack.get(0) == i);
			if (singletonSCC && filterTrivialSCCs) {
				if (!hadSelfloop) { // singleton SCC & no selfloop -> trivial
					stack.remove(0);
					onStack.set(i, false);
					return;
				}
			}

			int n;
			consumer.notifyStartSCC();
			do {
				n = stack.remove(0);
				onStack.set(n, false);
				consumer.notifyStateInSCC(n);
			} while (n != i);
			consumer.notifyEndSCC();
		}
	}

	/**
	 * A small class wrapping a node.
	 * It carries extra information necessary for Tarjan's algorithm.
	 */
	protected static class Node
	{
		public int lowlink = -1;
		public int index = -1;
		public int id;

		public Node(int id)
		{
			this.id = id;
		}
	}
}
