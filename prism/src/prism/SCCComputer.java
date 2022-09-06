//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Carlos S. Bederian (Universidad Nacional de Cordoba)
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
import java.util.Vector;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;

/**
 * Abstract class for (symbolic) classes that compute (B)SCCs,
 * i.e. (bottom) strongly connected components, for a model's transition graph.
 */
public abstract class SCCComputer extends PrismComponent
{
	// model info
	protected JDDNode trans01;
	protected JDDNode reach;
	protected JDDVars allDDRowVars;
	protected JDDVars allDDColVars;

	// stuff for SCCs
	protected Vector<JDDNode> sccs;
	protected JDDNode notInSCCs;
	protected JDDNode trivialSCCs;
	// stuff for BSCCs
	protected Vector<JDDNode> bsccs;
	protected JDDNode notInBSCCs;

	/**
	 * Static method to create a new SCCComputer object, depending on current settings.
	 */
	public static SCCComputer createSCCComputer(PrismComponent parent, Model model) throws PrismException
	{
		return createSCCComputer(parent, model.getReach(), model.getTransReln(), model.getAllDDRowVars(), model.getAllDDColVars());
	}

	/**
	 * Static method to create a new SCCComputer object, depending on current settings.
	 */
	public static SCCComputer createSCCComputer(PrismComponent parent, JDDNode reach, JDDNode trans01, JDDVars allDDRowVars, JDDVars allDDColVars)
			throws PrismException
	{
		return createSCCComputer(parent, parent.getSettings().getChoice(PrismSettings.PRISM_SCC_METHOD), reach, trans01, allDDRowVars, allDDColVars);
	}

	/**
	 * Static method to create a new SCCComputer object, depending on requested method.
	 */
	public static SCCComputer createSCCComputer(PrismComponent parent, int sccMethod, JDDNode reach, JDDNode trans01, JDDVars allDDRowVars, JDDVars allDDColVars)
			throws PrismException
	{
		SCCComputer sccComputer;
		switch (sccMethod) {
		case Prism.LOCKSTEP:
			sccComputer = new SCCComputerLockstep(parent, reach, trans01, allDDRowVars, allDDColVars);
			break;
		case Prism.SCCFIND:
			sccComputer = new SCCComputerSCCFind(parent, reach, trans01, allDDRowVars, allDDColVars);
			break;
		case Prism.XIEBEEREL:
			sccComputer = new SCCComputerXB(parent, reach, trans01, allDDRowVars, allDDColVars);
			break;
		default:
			sccComputer = new SCCComputerLockstep(parent, reach, trans01, allDDRowVars, allDDColVars);
		}
		return sccComputer;
	}

	/**
	 * Base constructor.
	 */
	public SCCComputer(PrismComponent parent, JDDNode reach, JDDNode trans01, JDDVars allDDRowVars, JDDVars allDDColVars) throws PrismException
	{
		super(parent);
		this.trans01 = trans01;
		this.reach = reach;
		this.allDDRowVars = allDDRowVars;
		this.allDDColVars = allDDColVars;

		if (jdd.SanityJDD.enabled) {
			jdd.SanityJDD.checkIsDDOverVars(trans01, allDDRowVars, allDDColVars);
			jdd.SanityJDD.checkIsDDOverVars(reach, allDDRowVars);
		}
	}

	/**
	 * Compute (non-trivial) strongly connected components (SCCs) and store them.
	 * They should be retrieved using {@link #getSCCs()}.
	 * States in trivial SCCs (those comprising a single state without a self-loop) are also stored.
	 * They should be retrieved using {@link #getNotInSCCs()}.
	 * You will need to to deref all of these afterwards.
	 */
	public abstract void computeSCCs() throws PrismException;

	/**
	 * Compute (non-trivial) strongly connected components (SCCs) containing a state from {@code filter} and store them.
	 * They should be retrieved using {@link #getSCCs()}.
	 * States in trivial SCCs (those comprising a single state without a self-loop) are also stored.
	 * They should be retrieved using {@link #getNotInSCCs()}.
	 * You will need to to deref all of these afterwards.
	 */
	public abstract void computeSCCs(JDDNode filter) throws PrismException;

	/**
	 * Get the list of computed (non-trivial) SCCs.
	 * You need to deref these BDDs when you are finished with them.
	 */
	public List<JDDNode> getSCCs()
	{
		return sccs;
	}

	/**
	 * Get the states not in any (non-trivial) SCC.
	 * In other words, this is all states in trivial SCCs (those comprising a single state without a self-loop).
	 * You need to deref this BDD when you are finished with it.
	 */
	public JDDNode getNotInSCCs()
	{
		return notInSCCs;
	}

	/**
	 * Get the list of computed BSCCs.
	 * You need to deref these BDDs when you are finished with them.
	 */
	public List<JDDNode> getBSCCs()
	{
		return bsccs;
	}

	/**
	 * Get the states not in any BSCC.
	 * You need to deref this BDDs when you are finished with it.
	 */
	public JDDNode getNotInBSCCs()
	{
		return notInBSCCs;
	}

	/**
	 * Compute bottom strongly connected components (BSCCs) and store them.
	 * They should be retrieved using {@link #getBSCCs()}.
	 * States that are not in any BSCC are also stored.
	 * They should be retrieved using {@link #getNotInBSCCs()}.
	 * You will need to to deref all of these afterwards.
	 */
	public void computeBSCCs() throws PrismException
	{
		JDDNode scc, out;
		int i, n;

		// First compute SCCs
		computeSCCs();

		// Now check which ones are BSCCs and keep them
		bsccs = new Vector<JDDNode>();
		notInBSCCs = notInSCCs;
		n = sccs.size();
		for (i = 0; i < n; i++) {
			scc = sccs.elementAt(i);
			JDD.Ref(trans01);
			JDD.Ref(scc);
			out = JDD.And(trans01, scc);
			JDD.Ref(scc);
			out = JDD.And(out, JDD.Not(JDD.PermuteVariables(scc, allDDRowVars, allDDColVars)));
			if (out.equals(JDD.ZERO)) {
				bsccs.addElement(scc);
			} else {
				JDD.Ref(scc);
				notInBSCCs = JDD.Or(scc, notInBSCCs);
				JDD.Deref(scc);
			}
			JDD.Deref(out);
		}

		// Print out some info
		mainLog.print("\nSCCs: " + sccs.size()); // Note: the BDDs in sccs have been derefed but the array still exists
		mainLog.print(", BSCCs: " + bsccs.size());
		mainLog.println(", non-BSCC states: " + JDD.GetNumMintermsString(notInBSCCs, allDDRowVars.n()));

		boolean verbose = getSettings().getBoolean(PrismSettings.PRISM_VERBOSE);
		if (!verbose && bsccs.size() > 10) {
			mainLog.print("BSCC sizes: More than 10 BSCCs, use verbose mode to view sizes for all.\n");
		} else {
			mainLog.print("BSCC sizes:");
			for (i = 0; i < bsccs.size(); i++) {
				mainLog.print(" " + (i + 1) + ":" + JDD.GetNumMintermsString(bsccs.elementAt(i), allDDRowVars.n()));
			}
			mainLog.println();
		}
	}
}
