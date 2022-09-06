//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package pta;

/**
 * Class to store a "symbolic transition", i.e. an element of a forwards reachability graph
 */
class SymbolicTransition
{
	// Pointer to a PTA transition
	public Transition tr;
	// List of target states
	public int[] dests;
	// Stored validity constraint 
	public Zone valid;

	// Note: tr and valid are just references to other objects;
	// the objects themselves should never be modified here.

	/**
	 * Usual constructor
	 */
	public SymbolicTransition(Transition tr, int[] dests, Zone valid)
	{
		this.tr = tr;
		this.dests = dests;
		this.valid = valid;
	}

	/**
	 * Copy constructor
	 */
	public SymbolicTransition(SymbolicTransition copy)
	{
		this(copy.tr, copy.dests.clone(), copy.valid);
	}

	/**
	 * Returns true if state s is a successor (a possible destination) of this symbolic transition.
	 */
	public boolean hasSuccessor(int s)
	{
		int i, n;
		n = dests.length;
		for (i = 0; i < n; i++) {
			if (dests[i] == s) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString()
	{
		String s = "[";
		for (int i = 0; i < dests.length; i++)
			s += " " + dests[i];
		return s + " ]";
	}
}
