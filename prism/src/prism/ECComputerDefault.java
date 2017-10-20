//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Carlos S. Bederian (Universidad Nacional de Cordoba)
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford)
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

import java.util.List;
import java.util.Stack;
import java.util.Vector;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;

/**
 * Symbolic maximal end component computer for a nondeterministic model such as an MDP.
 */
@SuppressWarnings("unused")
public class ECComputerDefault extends ECComputer
{
	/**
	 * Build (M)EC computer for a given model.
	 */
	public ECComputerDefault(PrismComponent parent, JDDNode reach, JDDNode trans, JDDNode trans01, JDDVars allDDRowVars, JDDVars allDDColVars,
			JDDVars allDDNondetVars) throws PrismException
	{
		super(parent, reach, trans, trans01, allDDRowVars, allDDColVars, allDDNondetVars);
	}

	// Methods for ECComputer interface

	@Override
	public void computeMECStates() throws PrismException
	{
		mecs = findEndComponents(null, null);
	}

	@Override
	public void computeMECStates(JDDNode restrict) throws PrismException
	{
		mecs = findEndComponents(restrict, null);
	}

	@Override
	public void computeMECStates(JDDNode restrict, JDDNode accept) throws PrismException
	{
		mecs = findEndComponents(restrict, accept);
	}

	// Computation

	/**
	 * Find all accepting maximal end components (MECs) in the submodel obtained
	 * by restricting this one to the set of states {@code restrict},
	 * where acceptance is defined as those which intersect with {@code accept}.
	 * If {@code restrict} is null, we look at the whole model, not a submodel.
	 * If {@code accept} is null, the acceptance condition is trivially satisfied.
	 * @param restrict BDD for the set of states to restrict to
	 * @param accept BDD for the set of accepting states
	 * @return a list of (referenced) BDDs representing the MECs
	 */
	private List<JDDNode> findEndComponents(JDDNode restrict, JDDNode accept) throws PrismException
	{
		Vector<JDDNode> mecs = new Vector<JDDNode>();
		SCCComputer sccComputer;

		// Initial set of candidates for MECs just contains the whole set we are searching
		// (which, if null, is all states)
		if (restrict == null)
			restrict = reach;
		Stack<JDDNode> candidates = new Stack<JDDNode>();
		JDD.Ref(restrict);
		candidates.push(restrict);

		// Go through each candidate set
		while (!candidates.isEmpty()) {
			JDDNode candidate = candidates.pop();
			
			// Compute its maximal stable set
			JDD.Ref(candidate);
			JDDNode stableSet = findMaximalStableSet(candidate);
			
			// Drop empty sets
			if (stableSet.equals(JDD.ZERO)) {
				JDD.Deref(stableSet);
				JDD.Deref(candidate);
				continue;
			}

			if (stableSet.equals(candidate) && JDD.isSingleton(stableSet, allDDRowVars)) {
				mecs.add(candidate);
				JDD.Deref(stableSet);
				continue;
			}

			// Filter bad transitions
			JDDNode stableSetTrans = getStableTransReln(stableSet.copy());

			// Find the maximal SCCs in (stableSet, stableSetTrans)
			sccComputer = SCCComputer.createSCCComputer(this, stableSet, stableSetTrans, allDDRowVars, allDDColVars);
			if (accept != null)
				sccComputer.computeSCCs(accept);
			else
				sccComputer.computeSCCs();
			JDD.Deref(stableSet);
			JDD.Deref(stableSetTrans);
			List<JDDNode> sccs = sccComputer.getSCCs();
			JDD.Deref(sccComputer.getNotInSCCs());
			
			// If there are no SCCs, do nothing 
			if (sccs.size() == 0) {
			}
			// If the whole sub-MDP is one SCC, we found an MEC
			else if (sccs.size() == 1 && sccs.get(0).equals(candidate)) {
				mecs.add(sccs.get(0));
			}
			// Otherwise add SCCs as candidates and proceed
			else {
				candidates.addAll(sccs);
			}
			JDD.Deref(candidate);
		}
		return mecs;
	}

}
