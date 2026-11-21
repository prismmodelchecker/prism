//==============================================================================
//
//	Copyright (c) 2026-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

/**
 * Map from the states of a model induced by a strategy (see {@link ConstructInducedModel}
 * and {@link ConstructStrategyProduct}) to the corresponding states of the original model
 * and, for strategies with memory, the strategy memory.
 */
public class InducedStateMap
{
	/** Original state for each induced state */
	private final int[] origStates;
	/** Strategy memory for each induced state (null if the strategy is memoryless) */
	private final int[] memories;

	/**
	 * Create a map, from arrays storing, for each induced state, the original state
	 * and (optionally, i.e., possibly null) the strategy memory.
	 * The arrays are stored, not copied.
	 */
	public InducedStateMap(int[] origStates, int[] memories)
	{
		this.origStates = origStates;
		this.memories = memories;
	}

	/**
	 * Get the number of states of the induced model.
	 */
	public int getNumStates()
	{
		return origStates.length;
	}

	/**
	 * Get the state of the original model corresponding to induced state {@code s}.
	 */
	public int getOriginalState(int s)
	{
		return origStates[s];
	}

	/**
	 * Does the map store strategy memory (i.e., was the strategy not memoryless)?
	 */
	public boolean hasMemory()
	{
		return memories != null;
	}

	/**
	 * Get the strategy memory corresponding to induced state {@code s}
	 * (-1 if the strategy is memoryless).
	 */
	public int getMemory(int s)
	{
		return memories == null ? -1 : memories[s];
	}
}
