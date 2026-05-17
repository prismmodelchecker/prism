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

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.IntPredicate;

import prism.PrismComponent;
import prism.PrismException;

/**
 * Tarjan's SCC algorithm operating on a Model object.
 * Uses an iterative (non-recursive) DFS to avoid stack overflow on large models,
 * and primitive int arrays throughout to minimise object allocation.
 * The variable-depth stacks (DFS frame stack and Tarjan SCC stack) start small
 * and grow on demand, so the only O(numStates) allocations are the two discovery
 * index arrays that are intrinsic to the algorithm.
 */
public class SCCComputerTarjan extends SCCComputer
{
	/* The model to compute (B)SCCs for */
	private Model<?> model;
	/* Number of nodes (model states) */
	private int numNodes;

	/* Next discovery index to assign */
	private int index = 0;
	/* Per-node discovery index; -1 means unvisited */
	private int[] nodeIndex;
	/* Per-node lowlink value */
	private int[] lowlink;
	/* Nodes currently on the Tarjan SCC stack */
	private BitSet onStack;
	/* Should we filter trivial SCCs? */
	private boolean filterTrivialSCCs;
	private IntPredicate restrict;

	/** Initial capacity for the dynamically-sized stacks */
	private static final int INITIAL_CAPACITY = 64;

	/**
	 * Build (B)SCC computer for a given model.
	 */
	public SCCComputerTarjan(PrismComponent parent, Model<?> model, SCCConsumer consumer) throws PrismException
	{
		super(parent, consumer);
		this.model = model;
		this.numNodes = model.getNumStates();
		this.nodeIndex = new int[numNodes];
		this.lowlink = new int[numNodes];
		Arrays.fill(nodeIndex, -1);
		this.onStack = new BitSet(numNodes);
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
	 * Execute Tarjan's algorithm iteratively.
	 * Uses an explicit frame stack instead of JVM recursion, so that large
	 * models with long chains do not cause a StackOverflowError.
	 * Both the DFS frame stack and the Tarjan SCC stack grow on demand
	 * (doubling up to numNodes) so no large upfront allocation is needed.
	 */
	private void tarjan() throws PrismException
	{
		int initCap = (numNodes == 0) ? 1 : Math.min(numNodes, INITIAL_CAPACITY);

		// Tarjan SCC path stack: contains nodes whose SCC membership is not yet determined.
		// Grows on demand up to numNodes (each node is pushed at most once).
		int tarjanCap = initCap;
		int[] tarjanStack = new int[tarjanCap];
		int tarjanStackTop = 0;

		// Explicit DFS call stack.  Each frame records:
		//   frameNode[]        - the node v being explored
		//   frameIter[]        - stateful successor iterator for v (resumes from where we left off)
		//   frameHadSelfloop[] - whether a self-loop on v has been seen
		// Depth is bounded by the longest DFS path, which is at most numNodes.
		int frameCap = initCap;
		int[] frameNode = new int[frameCap];
		SuccessorsIterator[] frameIter = new SuccessorsIterator[frameCap];
		boolean[] frameHadSelfloop = new boolean[frameCap];
		int frameTop = 0;

		for (int startNode = 0; startNode < numNodes; startNode++) {
			if (restrict != null && !restrict.test(startNode))
				continue;
			if (nodeIndex[startNode] != -1)
				continue;

			// Initialise startNode and push its frame
			nodeIndex[startNode] = index;
			lowlink[startNode] = index++;
			if (tarjanStackTop == tarjanCap) {
				tarjanCap = Math.min(numNodes, tarjanCap * 2);
				tarjanStack = Arrays.copyOf(tarjanStack, tarjanCap);
			}
			tarjanStack[tarjanStackTop++] = startNode;
			onStack.set(startNode);
			if (frameTop == frameCap) {
				frameCap = Math.min(numNodes, frameCap * 2);
				frameNode = Arrays.copyOf(frameNode, frameCap);
				frameIter = Arrays.copyOf(frameIter, frameCap);
				frameHadSelfloop = Arrays.copyOf(frameHadSelfloop, frameCap);
			}
			frameNode[frameTop] = startNode;
			frameIter[frameTop] = model.getSuccessors(startNode);
			frameHadSelfloop[frameTop] = false;
			frameTop++;

			while (frameTop > 0) {
				int fi = frameTop - 1;
				int v = frameNode[fi];
				SuccessorsIterator it = frameIter[fi];

				// Advance through successors of v until we push a new frame or exhaust them
				boolean pushed = false;
				while (it.hasNext()) {
					int e = it.nextInt();
					if (e == v) {
						frameHadSelfloop[fi] = true;
						continue;
					}
					if (restrict != null && !restrict.test(e))
						continue;

					if (nodeIndex[e] == -1) {
						// Unvisited: push new frame for e (simulates recursive call)
						nodeIndex[e] = index;
						lowlink[e] = index++;
						if (tarjanStackTop == tarjanCap) {
							tarjanCap = Math.min(numNodes, tarjanCap * 2);
							tarjanStack = Arrays.copyOf(tarjanStack, tarjanCap);
						}
						tarjanStack[tarjanStackTop++] = e;
						onStack.set(e);
						if (frameTop == frameCap) {
							frameCap = Math.min(numNodes, frameCap * 2);
							frameNode = Arrays.copyOf(frameNode, frameCap);
							frameIter = Arrays.copyOf(frameIter, frameCap);
							frameHadSelfloop = Arrays.copyOf(frameHadSelfloop, frameCap);
						}
						frameNode[frameTop] = e;
						frameIter[frameTop] = model.getSuccessors(e);
						frameHadSelfloop[frameTop] = false;
						frameTop++;
						pushed = true;
						break;
					} else if (onStack.get(e)) {
						lowlink[v] = Math.min(lowlink[v], nodeIndex[e]);
					}
				}

				if (!pushed) {
					// All successors of v are processed; pop v's frame (simulates return)
					frameIter[fi] = null; // release iterator for GC
					frameTop--;
					// Propagate lowlink to parent (the node that "called" v)
					if (frameTop > 0) {
						int parent = frameNode[frameTop - 1];
						lowlink[parent] = Math.min(lowlink[parent], lowlink[v]);
					}
					// If v is the root of an SCC, pop the SCC from the Tarjan stack
					if (lowlink[v] == nodeIndex[v]) {
						boolean singletonSCC = (tarjanStack[tarjanStackTop - 1] == v);
						if (singletonSCC && filterTrivialSCCs && !frameHadSelfloop[frameTop]) {
							// Singleton with no self-loop: trivial SCC, skip it
							tarjanStackTop--;
							onStack.clear(v);
						} else {
							int n;
							consumer.notifyStartSCC();
							do {
								n = tarjanStack[--tarjanStackTop];
								onStack.clear(n);
								consumer.notifyStateInSCC(n);
							} while (n != v);
							consumer.notifyEndSCC();
						}
					}
				}
			}
		}
	}
}
