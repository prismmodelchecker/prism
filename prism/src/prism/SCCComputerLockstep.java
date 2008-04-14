//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Carlos S. Bederián (Universidad Nacional de Córdoba)
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

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import java.util.*;

// SCC (strongly connected component) decomposition using lockstep search with trimming
// (from Bloem/Gabow/Somenzi 2000)

public class SCCComputerLockstep implements SCCComputer
{
	private class DecompTask
	{
		JDDNode _nodes;
		JDDNode _edges;
		DecompTask(JDDNode nodes, JDDNode edges) { _nodes = nodes; _edges = edges; }
		JDDNode getNodes() { return _nodes; }
		JDDNode getEdges() { return _edges; }
	}
	
	private Prism prism;
	private PrismLog log;
	
	private JDDNode initialNodes;
	private JDDNode initialEdges;
	private JDDVars rows;
	private JDDVars cols;
	private Vector<JDDNode> sccs;
	private JDDNode allSCCs;
	private JDDNode notInSCCs;
	private Stack<DecompTask> tasks; 
	
	public SCCComputerLockstep(Prism prism, JDDNode nodes, JDDNode edges, JDDVars rows_, JDDVars cols_)
	{
		this.prism = prism;
		log = prism.getMainLog();
		rows = rows_;
		cols = cols_;
		initialNodes = nodes;
		initialEdges = edges;
	}
	
	public void computeBSCCs()
	{
		sccs = new Vector<JDDNode>();
		allSCCs = JDD.Constant(0);
		tasks = new Stack<DecompTask>();
		JDD.Ref(initialNodes);
		JDD.Ref(initialEdges);
		tasks.push(new DecompTask(initialNodes, initialEdges));
		JDD.Ref(initialNodes);
		while (!tasks.isEmpty()) {
			lockstep(tasks.pop());
		}
		notInSCCs = JDD.And(initialNodes, JDD.Not(allSCCs));
		log.print(" BSCCs: " + sccs.size());
		log.println(" Transient states: " + JDD.GetNumMintermsString(notInSCCs, rows.n()));
	}
	
	public Vector<JDDNode> getVectBSCCs() { return sccs; }
	public JDDNode getNotInBSCCs() { return notInSCCs; }
		
	// Return the image of nodes in edges
	// Refs: result
	// Derefs: edges, nodes
	private JDDNode image(JDDNode nodes, JDDNode edges)
	{
		JDDNode tmp;

		// Get transitions that start at nodes
		tmp = JDD.Apply(JDD.TIMES, edges, nodes);
		// Get img(nodes)
		tmp = JDD.ThereExists(tmp, rows);
		tmp = JDD.PermuteVariables(tmp, cols, rows);
		return tmp;
	}

	// Return the preimage of nodes in edges
	// Refs: result
	// Derefs: edges, nodes
	private JDDNode preimage(JDDNode nodes, JDDNode edges)
	{
		JDDNode tmp;

		// Get transitions that end at nodes
		tmp = JDD.PermuteVariables(nodes, rows, cols);
		tmp = JDD.Apply(JDD.TIMES, edges, tmp);
		// Get pre(nodes)
		tmp = JDD.ThereExists(tmp, cols);
		return tmp;
	}
	
	// Report a SCC found by lockstep
	private void report(JDDNode nodes)
	{
		if (nodes.equals(JDD.ZERO)) {
			JDD.Deref(nodes);
			return;
		}
		// Sanity check, partitioning of the state space should prevent this
		assert !sccs.contains(nodes);
		
		/* if (prism.getVerbose()) {
			log.println("Found SCC:");
			JDD.PrintVector(nodes, rows);
		} */
		sccs.addElement(nodes);
		JDD.Ref(nodes);
		allSCCs = JDD.Or(allSCCs, nodes);
	}

	// Trim nodes that have no path to a node in a nontrivial SCC
	// or have no path from a node in a nontrivial SCC
	// Refs: result
	// Derefs: nodes, edges
	private JDDNode trim(JDDNode nodes, JDDNode edges) {
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
			if (prism.getVerbose()) {
				log.println("Trimming pass " + i + ":");
				JDD.PrintVector(current, rows);
				i++;
			}
		} while (!current.equals(old));
		JDD.Deref(nodes);
		JDD.Deref(edges);

		return current;
	}

	// Lockstep SCC decomposition with trimming from [BGS00]
	// Refs: reported result
	// Derefs: nodes and edges in the DecompTask
	private void lockstep(DecompTask task) {

		JDDNode nodes = task.getNodes();
		JDDNode edges = task.getEdges();
		JDDNode tmp;
				
		if (nodes.equals(JDD.ZERO)) {
			JDD.Deref(nodes);
			JDD.Deref(edges);
			return;
		}
		
		/* if (prism.getVerbose()) {
			log.println("Lockstep pass on nodes: ");
			JDD.PrintVector(nodes, rows);
		} */

		// trim nodes
		/* JDD.Ref(edges);
		nodes = trim(nodes, edges);
		JDD.Ref(nodes);
		edges = JDD.Apply(JDD.TIMES, edges, nodes);
		JDD.Ref(nodes);
		edges = JDD.Apply(JDD.TIMES, edges, JDD.PermuteVariables(nodes, rows, cols));
		*/
		
		// pick a starting node
		JDD.Ref(nodes);
		JDDNode v = JDD.RestrictToFirst(nodes, rows);
		// mainLog.println("Lockstep - picked node:");
		// JDD.PrintVector(v, allDDRowVars);

		
		JDDNode f = v;			// forward set of v
		JDDNode ffront = v;		// last update to f
		JDD.Ref(f);
		JDD.Ref(ffront);
		
		JDDNode b = v;			// backward set of v
		JDDNode bfront = v;		// last update to b
		JDD.Ref(b);
		JDD.Ref(bfront);
		
		JDD.Deref(v);

		// Compute forward and backward sets in lockstep until either one converges
		while (!ffront.equals(JDD.ZERO) && !bfront.equals(JDD.ZERO)) {

			JDD.Ref(edges);
			// Get the image of the last update
			tmp = image(ffront, edges);
			// find new states in this update
			JDD.Ref(f);
			ffront = JDD.And(tmp, JDD.Not(f));
			// add states to the forward set
			JDD.Ref(ffront);
			f = JDD.Or(f, ffront);

			JDD.Ref(edges);
			// Get the preimage of the last update
			tmp = preimage(bfront, edges);
			// find new states in this update
			JDD.Ref(b);
			bfront = JDD.And(tmp, JDD.Not(b));
			// add states to the backward set
			JDD.Ref(bfront);
			b = JDD.Or(b, bfront);
		}
		
		JDDNode convergedSet;	// set that converged first
		JDDNode sccUpdate;		// update in the last approximation of the scc
		
		// if ffront is empty, the forward set converged first
		if (ffront.equals(JDD.ZERO)) {
			convergedSet = f;
			JDD.Ref(convergedSet);
			
			// keep looking for states in the backward set until
			// its intersection with the forward set stops growing
			JDD.Ref(bfront);
			JDD.Ref(f);
			sccUpdate = JDD.And(bfront, f);
			while (!sccUpdate.equals(JDD.ZERO)) {
				JDD.Deref(sccUpdate);
				JDD.Ref(edges);
				JDD.Ref(b);
				bfront = JDD.And(preimage(bfront, edges), JDD.Not(b));
				JDD.Ref(bfront);
				b = JDD.Or(bfront, b);
				JDD.Ref(f);
				JDD.Ref(bfront);
				sccUpdate = JDD.And(bfront, f);			
			}
		}
		
		// bfront is empty, the backward set converged first
		else {
			convergedSet = b;
			JDD.Ref(convergedSet);

			// keep looking for states in the backward set until
			// its intersection with the forward set stops growing
			JDD.Ref(ffront);
			JDD.Ref(b);
			sccUpdate = JDD.And(ffront, b);
			while (!sccUpdate.equals(JDD.ZERO)) {
				JDD.Deref(sccUpdate);
				JDD.Ref(edges);
				JDD.Ref(f);
				ffront = JDD.And(image(ffront, edges), JDD.Not(f));
				JDD.Ref(ffront);
				f = JDD.Or(ffront, f);
				JDD.Ref(b);
				JDD.Ref(ffront);
				sccUpdate = JDD.And(ffront, b);
			}
		}
		JDD.Deref(sccUpdate);
		JDD.Deref(ffront);
		JDD.Deref(bfront);
		
		// Found our SCC
		JDDNode scc = JDD.Apply(JDD.TIMES, f, b);
		
		// check if SCC is nontrivial and report
		JDD.Ref(scc);
		tmp = JDD.PermuteVariables(scc, rows, cols);
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
		// newNodes1 = convergedSet \ scc
		JDD.Ref(scc);
		JDD.Ref(convergedSet);
		JDDNode newNodes1 = JDD.And(convergedSet, JDD.Not(scc));
		// newEdges1 = edges \intersect (newNodes x newNodes^t)
		JDD.Ref(edges);
		JDD.Ref(newNodes1);
		JDDNode newEdges1 = JDD.Apply(JDD.TIMES, edges, newNodes1);
		JDD.Ref(newNodes1);
		tmp = JDD.PermuteVariables(newNodes1, rows, cols);
		newEdges1 = JDD.Apply(JDD.TIMES, newEdges1, tmp);

		// newNodes2 = nodes \ convergedSet
		JDD.Ref(nodes);
		JDD.Ref(convergedSet);
		JDDNode newNodes2 = JDD.And(nodes, JDD.Not(convergedSet));
		// newEdges2 = edges \intersect (newNodes x newNodes^t)
		JDD.Ref(edges);
		JDD.Ref(newNodes2);
		JDDNode newEdges2 = JDD.Apply(JDD.TIMES, edges, newNodes2);
		JDD.Ref(newNodes2);
		tmp = JDD.PermuteVariables(newNodes2, rows, cols);
		newEdges2 = JDD.Apply(JDD.TIMES, newEdges2, tmp);
		
		// Queue new sets for search
		tasks.push(new DecompTask(newNodes2, newEdges2));
		tasks.push(new DecompTask(newNodes1, newEdges1));
		
		JDD.Deref(scc);
		JDD.Deref(convergedSet);
		JDD.Deref(nodes);
		JDD.Deref(edges);
	} 
}
