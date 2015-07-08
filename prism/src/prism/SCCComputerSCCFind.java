//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Carlos S. Bederian (Universidad Nacional de Cordoba)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package prism;

import java.util.Stack;
import java.util.Vector;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;

/**
 * SCC (strongly connected component) decomposition using SCC-Find search
 * (from Gentilini/Piazza/Policriti 2003)
 */
public class SCCComputerSCCFind extends SCCComputer
{
	private class DecompTask
	{
		JDDNode nodes;
		JDDNode edges;
		JDDNode spineSetPath;
		JDDNode spineSetNode;

		DecompTask(JDDNode nodes_, JDDNode edges_, JDDNode spineSetPath_, JDDNode spineSetNode_)
		{
			nodes = nodes_;
			edges = edges_;
			spineSetPath = spineSetPath_;
			spineSetNode = spineSetNode_;
		}
	}

	private JDDNode allSCCs;
	private Stack<DecompTask> tasks;

	/**
	 * Build (B)SCC computer for a given model.
	 */
	public SCCComputerSCCFind(PrismComponent parent, JDDNode reach, JDDNode trans01, JDDVars allDDRowVars, JDDVars allDDColVars) throws PrismException
	{
		super(parent, reach, trans01, allDDRowVars, allDDColVars);
	}

	// Methods for SCCComputer

	@Override
	public void computeSCCs()
	{
		sccs = new Vector<JDDNode>();
		allSCCs = JDD.Constant(0);
		tasks = new Stack<DecompTask>();
		JDD.Ref(reach);
		JDD.Ref(trans01);
		JDDNode trimmedNodes = trim(reach, trans01);
		JDD.Ref(trans01);
		tasks.push(new DecompTask(trimmedNodes, trans01, JDD.Constant(0), JDD.Constant(0)));
		while (!tasks.isEmpty()) {
			sccFind(tasks.pop());
		}
		JDD.Ref(reach);
		notInSCCs = JDD.And(reach, JDD.Not(allSCCs));
	}

	@Override
	public void computeSCCs(JDDNode filter)
	{
		//computeSCCs();
		sccs = new Vector<JDDNode>();
		allSCCs = JDD.Constant(0);
		tasks = new Stack<DecompTask>();
		JDD.Ref(reach);
		JDD.Ref(trans01);
		JDDNode trimmedNodes = trim(reach, trans01);
		JDD.Ref(trans01);
		tasks.push(new DecompTask(trimmedNodes, trans01, JDD.Constant(0), JDD.Constant(0)));
		while (!tasks.isEmpty()) {
			//mainLog.println("length of scc queue = " + tasks.size());
			sccFind(tasks.pop(), filter);
		}
		JDD.Ref(reach);
		notInSCCs = JDD.And(reach, JDD.Not(allSCCs));
	}

	// Computation
	
	// Return the image of nodes in edges
	// Refs: result
	// Derefs: edges, nodes
	private JDDNode image(JDDNode nodes, JDDNode edges)
	{
		JDDNode tmp;

		// Get transitions that start at nodes
		tmp = JDD.Apply(JDD.TIMES, edges, nodes);
		// Get the img(nodes)
		tmp = JDD.ThereExists(tmp, allDDRowVars);
		tmp = JDD.PermuteVariables(tmp, allDDColVars, allDDRowVars);
		return tmp;
	}

	// Return the preimage of nodes in edges
	// Refs: result
	// Derefs: edges, nodes
	private JDDNode preimage(JDDNode nodes, JDDNode edges)
	{
		JDDNode tmp;

		// Get transitions that end at nodes
		tmp = JDD.PermuteVariables(nodes, allDDRowVars, allDDColVars);
		tmp = JDD.Apply(JDD.TIMES, edges, tmp);
		// Get the pre(nodes)
		tmp = JDD.ThereExists(tmp, allDDColVars);
		return tmp;
	}

	// Trim nodes that have no path to a node in a nontrivial SCC
	// or have no path from a node in a nontrivial SCC
	// Refs: result
	// Derefs: nodes, edges
	private JDDNode trim(JDDNode nodes, JDDNode edges)
	{
		JDDNode old;
		JDDNode current;
		JDDNode img;
		JDDNode pre;
		int i = 1;

		JDD.Ref(nodes);
		current = nodes;
		do {
			old = current;
			JDD.Ref(current);
			JDD.Ref(edges);
			img = image(current, edges);
			JDD.Ref(current);
			JDD.Ref(edges);
			pre = preimage(current, edges);
			current = JDD.And(current, JDD.And(img, pre));
			if (settings.getBoolean(PrismSettings.PRISM_VERBOSE)) {
				mainLog.println("Trimming pass " + i + ":");
				JDD.PrintVector(current, allDDRowVars);
				i++;
			}
		} while (!current.equals(old));
		JDD.Deref(nodes);
		JDD.Deref(edges);

		return current;
	}

	// Report a SCC found by SCC-Find
	private void report(JDDNode nodes)
	{
		if (nodes.equals(JDD.ZERO)) {
			JDD.Deref(nodes);
			return;
		}
		// Sanity check, partitioning of the state space should prevent this
		assert !sccs.contains(nodes);

		/* if (prism.getVerbose()) {
			mainLog.println("Found SCC:");
			JDD.PrintVector(nodes, rows);
		} */
		sccs.addElement(nodes);
		JDD.Ref(nodes);
		allSCCs = JDD.Or(allSCCs, nodes);
	}

	private class SkelForwardResult
	{
		JDDNode forwardSet;
		JDDNode spineSetPath;
		JDDNode spineSetNode;

		SkelForwardResult(JDDNode forwardSet_, JDDNode spineSetPath_, JDDNode spineSetNode_)
		{
			forwardSet = forwardSet_;
			spineSetPath = spineSetPath_;
			spineSetNode = spineSetNode_;
		}
	}

	// Compute a forward set and skeleton of node
	// Derefs: nothing
	// Refs: SkelForwardResult contents
	private SkelForwardResult skelForward(JDDNode nodes, JDDNode edges, JDDNode node)
	{

		Stack<JDDNode> stack = new Stack<JDDNode>();

		JDD.Ref(node);
		JDDNode level = node;
		JDDNode forwardSet = JDD.Constant(0);
		while (!level.equals(JDD.ZERO)) {
			JDD.Ref(level);
			stack.push(level);
			JDD.Ref(level);
			forwardSet = JDD.Or(forwardSet, level);
			JDD.Ref(forwardSet);
			JDD.Ref(edges);
			level = JDD.And(image(level, edges), JDD.Not(forwardSet));
		}
		JDD.Deref(level);

		level = stack.pop();
		JDDNode newNode = JDD.RestrictToFirst(level, allDDRowVars);
		JDD.Ref(newNode);
		JDDNode newPath = newNode;
		while (!stack.isEmpty()) {
			level = stack.pop();
			JDD.Ref(newPath);
			JDD.Ref(edges);
			newPath = JDD.Or(newPath, JDD.RestrictToFirst(JDD.And(preimage(newPath, edges), level), allDDRowVars));
		}
		return new SkelForwardResult(forwardSet, newPath, newNode);
	}

	// SCC-Find decomposition
	// Refs: reported result
	// Derefs: DecompTask contents
	private void sccFind(DecompTask task)
	{
		JDDNode nodes = task.nodes;
		JDDNode edges = task.edges;
		JDDNode spineSetPath = task.spineSetPath;
		JDDNode spineSetNode = task.spineSetNode;
		JDDNode tmp;

		if (nodes.equals(JDD.ZERO)) {
			JDD.Deref(nodes);
			JDD.Deref(edges);
			JDD.Deref(spineSetPath);
			JDD.Deref(spineSetNode);
			return;
		}

		/* if (prism.getVerbose()) {
			mainLog.println("SCC-Find pass on nodes: ");
			JDD.PrintVector(nodes, rows);
		} */

		if (spineSetPath.equals(JDD.ZERO)) {
			JDD.Deref(spineSetNode);
			// Pick a node
			JDD.Ref(nodes);
			spineSetNode = JDD.RestrictToFirst(nodes, allDDRowVars);
		}

		SkelForwardResult skelFw = skelForward(nodes, edges, spineSetNode);
		JDDNode forwardSet = skelFw.forwardSet;
		JDDNode newSpineSetPath = skelFw.spineSetPath;
		JDDNode newSpineSetNode = skelFw.spineSetNode;

		JDD.Ref(spineSetNode);
		JDDNode scc = spineSetNode;
		JDD.Ref(scc);
		JDD.Ref(edges);
		JDD.Ref(forwardSet);
		JDDNode intersection = JDD.And(preimage(scc, edges), forwardSet);
		JDD.Ref(intersection);
		JDD.Ref(scc);
		tmp = JDD.And(intersection, JDD.Not(scc));
		while (!tmp.equals(JDD.ZERO)) {
			scc = JDD.Or(scc, intersection);
			JDD.Ref(scc);
			JDD.Ref(edges);
			JDD.Ref(forwardSet);
			intersection = JDD.And(preimage(scc, edges), forwardSet);
			JDD.Ref(intersection);
			JDD.Ref(scc);
			JDD.Deref(tmp);
			tmp = JDD.And(intersection, JDD.Not(scc));
		}
		JDD.Deref(tmp);
		JDD.Deref(intersection);

		// check if SCC is nontrivial and report
		JDD.Ref(scc);
		tmp = JDD.PermuteVariables(scc, allDDRowVars, allDDColVars);
		JDD.Ref(edges);
		tmp = JDD.And(tmp, edges);
		JDD.Ref(scc);
		tmp = JDD.And(tmp, scc);
		if (!tmp.equals(JDD.ZERO)) {
			JDD.Ref(scc);
			report(scc);
		}
		JDD.Deref(tmp);

		// FIXME: restricting newEdges isn't necessary, needs benchmarking
		//        (speed vs memory?)
		// newNodes1 = nodes \ forwardSet
		JDD.Ref(nodes);
		JDD.Ref(forwardSet);
		JDDNode newNodes1 = JDD.And(nodes, JDD.Not(forwardSet));
		// newEdges1 = edges \intersect (newNodes1 x newNodes1^t)
		JDD.Ref(edges);
		JDD.Ref(newNodes1);
		JDDNode newEdges1 = JDD.Apply(JDD.TIMES, edges, newNodes1);
		JDD.Ref(newNodes1);
		newEdges1 = JDD.Apply(JDD.TIMES, newEdges1, JDD.PermuteVariables(newNodes1, allDDRowVars, allDDColVars));
		// newS1 = spineSetPath \ scc
		JDD.Ref(spineSetPath);
		JDD.Ref(scc);
		JDDNode newSpineSetPath1 = JDD.And(spineSetPath, JDD.Not(scc));
		// newNode1 = preimage(scc \intersect spineSetPath) \intersect (spineSetPath \ scc)
		JDD.Ref(scc);
		JDD.Ref(spineSetPath);
		JDD.Ref(edges);
		JDD.Ref(newSpineSetPath1);
		JDDNode newSpineSetNode1 = JDD.And(preimage(JDD.And(scc, spineSetPath), edges), newSpineSetPath1);

		// newNodes2 = forwardSet \ scc
		//JDD.Ref(forwardSet);
		JDD.Ref(scc);
		JDDNode newNodes2 = JDD.And(forwardSet, JDD.Not(scc));
		// newEdges2 = edges \intersect (newNodes2 x newNodes2^t)
		JDD.Ref(edges);
		JDD.Ref(newNodes2);
		JDDNode newEdges2 = JDD.Apply(JDD.TIMES, edges, newNodes2);
		JDD.Ref(newNodes2);
		newEdges2 = JDD.Apply(JDD.TIMES, newEdges2, JDD.PermuteVariables(newNodes2, allDDRowVars, allDDColVars));
		// newS2 = newS \ scc
		JDD.Ref(scc);
		JDDNode newSpineSetPath2 = JDD.And(newSpineSetPath, JDD.Not(scc));
		// newNode2 = newNode \ scc
		//JDD.Ref(scc);
		JDDNode newSpineSetNode2 = JDD.And(newSpineSetNode, JDD.Not(scc));

		tasks.push(new DecompTask(newNodes2, newEdges2, newSpineSetPath2, newSpineSetNode2));
		tasks.push(new DecompTask(newNodes1, newEdges1, newSpineSetPath1, newSpineSetNode1));

		//JDD.Deref(forwardSet);
		JDD.Deref(spineSetNode);
		JDD.Deref(spineSetPath);
		//JDD.Deref(scc);
		JDD.Deref(nodes);
		JDD.Deref(edges);
	}

	private void sccFind(DecompTask task, JDDNode filter)
	{

		JDDNode nodes = task.nodes;
		JDDNode edges = task.edges;
		JDDNode spineSetPath = task.spineSetPath;
		JDDNode spineSetNode = task.spineSetNode;
		JDDNode tmp;

		if (nodes.equals(JDD.ZERO)) {
			JDD.Deref(nodes);
			JDD.Deref(edges);
			JDD.Deref(spineSetPath);
			JDD.Deref(spineSetNode);
			return;
		}

		/* if (prism.getVerbose()) {
			mainLog.println("SCC-Find pass on nodes: ");
			JDD.PrintVector(nodes, rows);
		} */

		if (spineSetPath.equals(JDD.ZERO)) {
			JDD.Deref(spineSetNode);
			// Pick a node
			JDD.Ref(nodes);
			spineSetNode = JDD.RestrictToFirst(nodes, allDDRowVars);
		}

		SkelForwardResult skelFw = skelForward(nodes, edges, spineSetNode);
		JDDNode forwardSet = skelFw.forwardSet;
		JDDNode newSpineSetPath = skelFw.spineSetPath;
		JDDNode newSpineSetNode = skelFw.spineSetNode;

		JDD.Ref(spineSetNode);
		JDDNode scc = spineSetNode;
		JDD.Ref(scc);
		JDD.Ref(edges);
		JDD.Ref(forwardSet);
		JDDNode intersection = JDD.And(preimage(scc, edges), forwardSet);
		JDD.Ref(intersection);
		JDD.Ref(scc);
		tmp = JDD.And(intersection, JDD.Not(scc));
		while (!tmp.equals(JDD.ZERO)) {
			scc = JDD.Or(scc, intersection);
			JDD.Ref(scc);
			JDD.Ref(edges);
			JDD.Ref(forwardSet);
			intersection = JDD.And(preimage(scc, edges), forwardSet);
			JDD.Ref(intersection);
			JDD.Ref(scc);
			JDD.Deref(tmp);
			tmp = JDD.And(intersection, JDD.Not(scc));
		}
		JDD.Deref(tmp);
		JDD.Deref(intersection);

		// check if SCC is nontrivial and report
		JDD.Ref(scc);
		tmp = JDD.PermuteVariables(scc, allDDRowVars, allDDColVars);
		JDD.Ref(edges);
		tmp = JDD.And(tmp, edges);
		JDD.Ref(scc);
		tmp = JDD.And(tmp, scc);
		if (!tmp.equals(JDD.ZERO)) {
			JDD.Ref(scc);
			report(scc);
		}
		JDD.Deref(tmp);

		// FIXME: restricting newEdges isn't necessary, needs benchmarking
		//        (speed vs memory?)
		// newNodes1 = nodes \ forwardSet
		JDD.Ref(nodes);
		JDD.Ref(forwardSet);
		JDDNode newNodes1 = JDD.And(nodes, JDD.Not(forwardSet));
		if (JDD.AreIntersecting(newNodes1, filter)) {
			// newEdges1 = edges \intersect (newNodes1 x newNodes1^t)
			JDD.Ref(edges);
			JDD.Ref(newNodes1);
			JDDNode newEdges1 = JDD.Apply(JDD.TIMES, edges, newNodes1);
			JDD.Ref(newNodes1);
			newEdges1 = JDD.Apply(JDD.TIMES, newEdges1, JDD.PermuteVariables(newNodes1, allDDRowVars, allDDColVars));
			// newS1 = spineSetPath \ scc
			JDD.Ref(spineSetPath);
			JDD.Ref(scc);
			JDDNode newSpineSetPath1 = JDD.And(spineSetPath, JDD.Not(scc));
			// newNode1 = preimage(scc \intersect spineSetPath) \intersect (spineSetPath \ scc)
			JDD.Ref(scc);
			JDD.Ref(spineSetPath);
			JDD.Ref(edges);
			JDD.Ref(newSpineSetPath1);
			JDDNode newSpineSetNode1 = JDD.And(preimage(JDD.And(scc, spineSetPath), edges), newSpineSetPath1);

			tasks.push(new DecompTask(newNodes1, newEdges1, newSpineSetPath1, newSpineSetNode1));
		} else
			JDD.Deref(newNodes1);

		// newNodes2 = forwardSet \ scc
		//JDD.Ref(forwardSet);
		JDD.Ref(scc);
		JDDNode newNodes2 = JDD.And(forwardSet, JDD.Not(scc));
		if (JDD.AreIntersecting(newNodes2, filter)) {
			// newEdges2 = edges \intersect (newNodes2 x newNodes2^t)
			JDD.Ref(edges);
			JDD.Ref(newNodes2);
			JDDNode newEdges2 = JDD.Apply(JDD.TIMES, edges, newNodes2);
			JDD.Ref(newNodes2);
			newEdges2 = JDD.Apply(JDD.TIMES, newEdges2, JDD.PermuteVariables(newNodes2, allDDRowVars, allDDColVars));
			// newS2 = newS \ scc
			JDD.Ref(scc);
			JDDNode newSpineSetPath2 = JDD.And(newSpineSetPath, JDD.Not(scc));
			// newNode2 = newNode \ scc
			JDD.Ref(scc);
			JDDNode newSpineSetNode2 = JDD.And(newSpineSetNode, JDD.Not(scc));

			tasks.push(new DecompTask(newNodes2, newEdges2, newSpineSetPath2, newSpineSetNode2));
		} else {
			JDD.Deref(newNodes2);
			JDD.Deref(newSpineSetPath);
			JDD.Deref(newSpineSetNode);
		}

		//JDD.Deref(forwardSet);
		JDD.Deref(spineSetNode);
		JDD.Deref(spineSetPath);
		JDD.Deref(scc);
		JDD.Deref(nodes);
		JDD.Deref(edges);
	}
}
