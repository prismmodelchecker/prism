//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Carlos S. Bederian (Universidad Nacional de Cordoba)
//  * Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford)
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

//	interface for end-component computing classes

public abstract class ECComputer
{
	protected Prism prism;
	protected PrismLog mainLog;

	// model info
	protected JDDNode trans;
	protected JDDNode trans01;
	protected JDDNode reach;
	protected JDDVars allDDRowVars;
	protected JDDVars allDDColVars;
	protected JDDVars allDDNondetVars;
		
	// stuff for ECs
	protected Vector<JDDNode> vectECs;
	protected JDDNode notInECs;
		
	// Constructor
	/**
	 * Find maximal EC of a sub-MDP by restricting reach and trans01.
	 * This sub-MDP needs to be deadlock-free.
	 */
	public ECComputer(Prism prism, JDDNode reach, JDDNode trans, JDDNode trans01, JDDVars allDDRowVars, JDDVars allDDColVars, JDDVars allDDNondetVars)
	{
		this.prism = prism;
		mainLog = prism.getMainLog();
		this.reach = reach;
		this.trans = trans;
		this.trans01 = trans01;		
		this.allDDRowVars = allDDRowVars;
		this.allDDColVars = allDDColVars;
		this.allDDNondetVars = allDDNondetVars;
	}
	
	// Get vector of EndComponents
	// NB: these BDDs aren't derefed by EndComponentComputer classes
	public Vector<JDDNode> getVectECs() { return vectECs; }
	
	// Get states not in any EndComponent
	// NB: this BDD isn't derefed by SCCComputer classes
	//public JDDNode getNotInECs() { return notInECs; }
	
	// End Components (EC) computation
	// NB: This creates BDDs, obtainable from getVectECs() and getNotInECs(),
	// which  the calling code is responsible for dereferencing.
	public abstract void computeECs();
}
