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

import java.util.Vector;

import jdd.*;

//	interface for SCC computing classes

public abstract class SCCComputer
{
	protected Prism prism;
	protected PrismLog mainLog;

	// model info
	protected JDDNode trans01;
	protected JDDNode reach;
	protected JDDVars allDDRowVars;
	protected JDDVars allDDColVars;
	
	// stuff for SCCs
	protected Vector<JDDNode> vectSCCs;
	protected JDDNode notInSCCs;
	// stuff for BSCCs
	protected Vector<JDDNode> vectBSCCs;
	protected JDDNode notInBSCCs;

	// Get methods
	
	// Constructor
	public SCCComputer(Prism prism, JDDNode reach, JDDNode trans01, JDDVars allDDRowVars, JDDVars allDDColVars)
	{
		this.prism = prism;
		mainLog = prism.getMainLog();
		this.trans01 = trans01;
		this.reach = reach;
		this.allDDRowVars = allDDRowVars;
		this.allDDColVars = allDDColVars;
	}
	
	// Get vector of SCCs
	// NB: these BDDs aren't derefed by SCCComputer classes
	public Vector<JDDNode> getVectSCCs() { return vectSCCs; }
	
	// Get states not in any SCCs
	// NB: this BDD isn't derefed by SCCComputer classes
	public JDDNode getNotInSCCs() { return notInSCCs; }

	// Get vector of BSCCs
	// NB: these BDDs aren't derefed by SCCComputer classes
	public Vector<JDDNode> getVectBSCCs() { return vectBSCCs; }
	
	// Get states not in any BSCCs
	// NB: this BDD isn't derefed by SCCComputer classes
	public JDDNode getNotInBSCCs() { return notInBSCCs; }
	
	// Strongly connected components (SCC) computation
	// NB: This creates BDDs, obtainable from getVectSCCs() and getNotInSCCs(),
	// which  the calling code is responsible for dereferencing.
	public abstract void computeSCCs();

	// Bottom strongly connected components (BSCC) computation
	// NB: This creates BDDs, obtainable from getVectBSCCs() and getNotInBSCCs(),
	// which  the calling code is responsible for dereferencing.
	public void computeBSCCs()
	{
		JDDNode scc, out;
		int i, n;
		
		// First compute SCCs
		computeSCCs();
		
		// Now check which ones are bsccs and keep them
		vectBSCCs = new Vector<JDDNode>();
		notInBSCCs = notInSCCs;
		n = vectSCCs.size();
		for (i = 0; i < n; i++) {
			scc = vectSCCs.elementAt(i);
			JDD.Ref(trans01);
			JDD.Ref(scc);
			out = JDD.And(trans01, scc);
			JDD.Ref(scc);
			out = JDD.And(out, JDD.Not(JDD.PermuteVariables(scc, allDDRowVars, allDDColVars)));
			if (out.equals(JDD.ZERO)) {
				vectBSCCs.addElement(scc);
			}
			else {
				JDD.Ref(scc);
				notInBSCCs = JDD.Or(scc, notInBSCCs);
				JDD.Deref(scc);
			}
			JDD.Deref(out);
		}
		
		// print out some info
		mainLog.print("\nSCCs: " + vectSCCs.size()); // note: contents of vectSCCs derefed but array exiists
		mainLog.print(", BSCCs: " + vectBSCCs.size());
		mainLog.println(", non-BSCC states: " + JDD.GetNumMintermsString(notInBSCCs, allDDRowVars.n()));
		mainLog.print("BSCC sizes:");
		for (i = 0; i < vectBSCCs.size(); i++) {
			mainLog.print(" " + (i+1) + ":" + JDD.GetNumMintermsString(vectBSCCs.elementAt(i), allDDRowVars.n()));
		}
		mainLog.println();
	}
}
