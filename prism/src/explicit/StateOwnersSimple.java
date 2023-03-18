//==============================================================================
//	
//	Copyright (c) 2020-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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
import java.util.List;

/**
 * Explicit-state storage of which player owns each state in a turn-based game.
 * 
 * Uses simple, mutable data structures, matching the "Simple" range of models.
 */
public class StateOwnersSimple
{
	/**
	 * Mapping from states to the player that owns them
	 * NB: states are 0-indexed; players are 0-indexed
	 */
	protected List<Integer> stateOwners;
	
	// Constructors
	
	/**
	 * Constructor: empty list (no states).
	 */
	public StateOwnersSimple()
	{
		stateOwners = new ArrayList<Integer>();
	}

	/**
	 * Constructor: new list with fixed number of states.
	 */
	public StateOwnersSimple(int numStates)
	{
		stateOwners = new ArrayList<Integer>(numStates);
		for (int i = 0; i < numStates; i++) {
			stateOwners.add(-1);
		}
	}

	/**
	 * Copy constructor
	 */
	public StateOwnersSimple(StateOwnersSimple stateOwnersSimple)
	{
		stateOwners = new ArrayList<Integer>(stateOwnersSimple.stateOwners);
	}
	
	/**
	 * Copy constructor, but with a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 */
	public StateOwnersSimple(StateOwnersSimple stateOwnersSimple, int permut[])
	{
		// Create blank list of correct size
		int numStates = stateOwnersSimple.stateOwners.size();
		stateOwners = new ArrayList<Integer>(numStates);
		for (int i = 0; i < numStates; i++) {
			stateOwners.add(-1);
		}
		// Copy permuted player info
		for (int i = 0; i < numStates; i++) {
			stateOwners.set(permut[i], stateOwnersSimple.stateOwners.get(i));
		}
	}
	
	// Mutators
	
	/**
	 * Clear all information for a state (i.e., assign no player).
	 */
	public void clearState(int s)
	{
		stateOwners.set(s, -1);
	}
	

	/**
	 * Add a new state owned by player {@code p}.
	 * @param p Player who owns the new state (0-indexed)
	 */
	public void addState(int p)
	{
		stateOwners.add(p);
	}
	
	/**
	 * Set the player that owns state {@code s} to {@code p}.
	 * @param s State to be modified (0-indexed)
	 * @param p Player who owns the state (0-indexed)
	 */
	public void setPlayer(int s, int p)
	{
		stateOwners.set(s, p);
	}
	
	// Accessors
	
	/**
	 * Get the player that owns state {@code s}.
	 * Returns the index of the player (0-indexed).
	 * @param s Index of state (0-indexed)
	 */
	public int getPlayer(int s)
	{
		return stateOwners.get(s);
	}
}
