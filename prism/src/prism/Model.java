//==============================================================================
//
//	Copyright (c) 2023-
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

package prism;

/**
 * Interface for classes that store models.
 * This is generic, where probabilities/rates/etc. are of type {@code Value}.
 */
public interface Model<Value>
{
	// Accessors

	/**
	 * Get the type of this model.
	 */
	ModelType getModelType();

	/**
	 * Get the number of states.
	 */
	int getNumStates();

	/**
	 * Get the number of initial states.
	 */
	int getNumInitialStates();

	/**
	 * Get the total number of transitions.
	 */
	int getNumTransitions();

	/**
	 * Does the number of states exceed {@code Integer.MAX_VALUE}?
	 */
	default boolean numStatesExceedsInt()
	{
		// Usually not
		return false;
	}

	/**
	 * Get the number of states, as a string.
	 */
	default String getNumStatesString()
	{
		return Integer.toString(getNumStates());
	}

	/**
	 * Get the number of initial states, as a string.
	 */
	default String getNumInitialStatesString()
	{
		return Integer.toString(getNumStates());
	}

	/**
	 * Get the total number of transitions, as a string.
	 */
	default String getNumTransitionsString()
	{
		return Integer.toString(getNumStates());
	}
}
