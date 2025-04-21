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

import java.util.List;
import java.util.stream.Collectors;

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
	 * Get a list of the action labels attached to choices/transitions.
	 * This can be a superset of the action labels that are actually used in the model.
	 * Action labels can be any Object, but will often be treated as a string,
	 * so should at least have a meaningful toString() method implemented.
	 * Absence of an action label is denoted by null,
	 * and null is also included in this list if there are unlabelled choices/transitions.
	 */
	default List<Object> getActions()
	{
		// Default implementation: find unique actions used across the model.
		// This should be cached/optimised if action indices are looked up frequently.
		return findActionsUsed();
	}

	/**
	 * Get strings for the action labels attached to choices/transitions.
	 */
	default List<String> getActionStrings()
	{
		// By default, just apply toString to getActions()
		return getActions().stream().map(ActionList::actionString).collect(Collectors.toList());
	}

	/**
	 * Produce a list of the action labels attached to choices/transitions.
	 * Absence of an action label is denoted by null,
	 * and null is also included in this list if there are unlabelled choices/transitions.
	 */
	List<Object> findActionsUsed();

	/**
	 * Do all choices/transitions have empty (null) action labels?
	 */
	default boolean onlyNullActionUsed()
	{
		// Can't assume this is true
		return false;
	}

	/**
	 * Get the index of an action label.
	 * Indices are into the list given by {@link #getActions()},
	 * which includes null if there are unlabelled choices,
	 * so this method should always return a value >= 0 for a valid action.
	 */
	default int actionIndex(Object action)
	{
		// Default (inefficient) implementation: just look up in getActions()
		return getActions().indexOf(action);
	}

	/**
	 * Get the number of states.
	 */
	int getNumStates();

	/**
	 * Get the number of players.
	 */
	default int getNumPlayers()
	{
		ModelType modelType = getModelType();
		return modelType.nondeterministic() ? (modelType == ModelType.STPG ? 2 : 1) : 0;
	}

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
