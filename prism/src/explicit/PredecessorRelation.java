//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import prism.PrismComponent;
import common.IterableBitSet;

/**
 * A class for storing and accessing the predecessor relation of an explicit Model.
 * <p>
 * As Model only provide easy access to successors of states,
 * the predecessor relation is computed and stored for subsequent efficient access.
 * <p>
 * Note: Naturally, if the model changes, the predecessor relation
 * has to be recomputed to remain accurate.
 */
public class PredecessorRelation
{
	/**
	 * pre[i] provides the list of predecessors of state with index i.
	 */
	List<ArrayList<Integer>> pre;

	/**
	 * Constructor. Computes the predecessor relation for the given model
	 * by considering the successors of each state.
	 *
	 * @param model the Model
	 */
	public PredecessorRelation(Model model)
	{
		pre = new ArrayList<ArrayList<Integer>>(model.getNumStates());
		// construct the (empty) array list for all states
		for (int s = 0; s < model.getNumStates(); s++) {
			pre.add(s, new ArrayList<Integer>());
		}

		compute(model);
	}

	/** Compute the predecessor relation using getSuccessorsIterator. */
	private void compute(Model model)
	{
		int n = model.getNumStates();

		for (int s = 0; s < n; s++) {
			Iterator<Integer> it = model.getSuccessorsIterator(s);
			while (it.hasNext()) {
				Integer successor = it.next();

				// Add the current state s to pre[successor].
				//
				// As getSuccessorsIterator guarantees that
				// there are no duplicates in the successors,
				// s will be added to successor exactly once.
				pre.get(successor).add(s);
			}
		}
	}

	/**
	 * Get an Iterable over the predecessor states of {@code s}.
	 */
	public Iterable<Integer> getPre(int s)
	{
		return pre.get(s);
	}

	/**
	 * Get an Iterator over the predecessor states of {@code s}.
	 */
	public Iterator<Integer> getPredecessorsIterator(int s)
	{
		return getPre(s).iterator();
	}

	/**
	 * Static constructor to compute the predecessor relation for the given model.
	 * Logs diagnostic information to the log of the given PrismComponent.
	 *
	 * @param parent a PrismComponent (for obtaining the log and settings)
	 * @param model the model for which the predecessor relation should be computed
	 * @returns the predecessor relation
	 **/
	public static PredecessorRelation forModel(PrismComponent parent, Model model)
	{
		long timer = System.currentTimeMillis();
		
		parent.getLog().print("Calculating predecessor relation for "+model.getModelType().fullName()+"...  ");
		parent.getLog().flush();

		PredecessorRelation pre = new PredecessorRelation(model);
		
		timer = System.currentTimeMillis() - timer;
		parent.getLog().println("done (" + timer / 1000.0 + " seconds)");

		return pre;
	}


	/**
	 * Computes the set Pre*(target) via a DFS, i.e., all states that
	 * are in {@code target} or can reach {@code target} via one or more transitions
	 * from states contained in {@code remain}.
	 * <br/>
	 * If the parameter {@code remain} is {@code null}, then
	 * {@code remain} is considered to include all states in the model.
	 * <br/>
	 * If the parameter {@code absorbing} is not {@code null},
	 * then the states in {@code absorbing} are considered to be absorbing,
	 * i.e., to have a single self-loop, disregarding other outgoing edges.

	 * @param remain restriction on the states that may occur
	 *               on the path to target, {@code null} = all states
	 * @param target The set of target states
	 * @param absorbing (optional) set of states that should be considered to be absorbing,
	 *               i.e., their outgoing edges are ignored, {@code null} = no states
	 * @return the set of states Pre*(target)
	 */
	public BitSet calculatePreStar(BitSet remain, BitSet target, BitSet absorbing)
	{
		BitSet result;

		// all target states are in Pre*
		result = (BitSet)target.clone();

		// the stack of states whose predecessors have to be considered
		Stack<Integer> todo = new Stack<Integer>();

		// initial todo: all the target states
		for (Integer s : IterableBitSet.getSetBits(target)) {
			todo.add(s);
		};

		// the set of states that are finished
		BitSet done = new BitSet();

		while (!todo.isEmpty()) {
			int s = todo.pop();
			// already considered?
			if (done.get(s)) continue;

			done.set(s);

			// for each predecessor in the graph
			for (int p : getPre(s)) {
				if (absorbing != null && absorbing.get(p)) {
					// predecessor is absorbing, thus the edge is considered to not exist
					continue;
				}
				if (remain == null || remain.get(p)) {
					// can reach result (and is in remain)
					result.set(p);
					if (!done.get(p)) {
						// add to stack
						todo.add(p);
					}
				}
			}
		}

		return result;
	}

}
