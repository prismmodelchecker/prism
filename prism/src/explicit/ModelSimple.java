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

package explicit;

import java.util.BitSet;
import java.util.List;

import parser.State;
import prism.PrismException;

/**
 * Interface for simple mutable explicit-state model representations.
 */
public interface ModelSimple extends Model
{
	/**
	 * Add a state to the list of initial states.
	 */
	public abstract void addInitialState(int i);

	/**
	 * Build (anew) from a list of transitions exported explicitly by PRISM (i.e. a .tra file).
	 * Note that initial states are not configured (since this info is not in the file),
	 * so this needs to be done separately (using {@link #addInitialState(int)}.
	 */
	public abstract void buildFromPrismExplicit(String filename) throws PrismException;

	/**
	 * Clear all information for a state (i.e. remove all transitions).
	 */
	public abstract void clearState(int i);

	/**
	 * Add a new state and return its index.
	 */
	public abstract int addState();

	/**
	 * Add multiple new states.
	 */
	public abstract void addStates(int numToAdd);

	/**
	 * Set the associated (read-only) state list.
	 */
	public void setStatesList(List<State> statesList);

	/**
	 * Adds a label and the set the states that satisfy it.
	 * Any existing label with the same name is overwritten.
	 * @param name The name of the label
	 * @param states The states that satisfy the label 
	 */
	public void addLabel(String name, BitSet states);
}