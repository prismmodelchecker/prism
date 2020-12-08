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

/**
 * Explicit-state storage of the action labels attached to choices in a model.
 * 
 * Uses simple, mutable data structures, matching the "Simple" range of models.
 */
public class ChoiceActionsSimple
{
	// Action labels for each choice in each state
	// (null list means no actions; null in element s means no actions for state s)
	// Note: The number of states and choices per state is unknown,
	// so lists may be under-sized, in which case missing entries are assumed to be null.
	protected ArrayList<ArrayList<Object>> actions;

	// Constructors
	
	/**
	 * Constructor: empty action storage
	 */
	public ChoiceActionsSimple()
	{
		actions = null;
	}

	/**
	 * Copy constructor
	 */
	public ChoiceActionsSimple(ChoiceActionsSimple cas)
	{
		actions = null;
		if (cas.actions != null) {
			int numStates = cas.actions.size();
			actions = new ArrayList<ArrayList<Object>>(numStates);
			for (int s = 0; s < numStates; s++) {
				actions.add(null);
			}
			for (int s = 0; s < numStates; s++) {
				if (cas.actions.get(s) != null) {
					actions.set(s, new ArrayList<>(cas.actions.get(s)));
				}
			}
		}
	}

	/**
	 * Copy constructor, but with a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 */
	public ChoiceActionsSimple(ChoiceActionsSimple cas, int permut[])
	{
		actions = null;
		if (cas.actions != null) {
			// NB: permut.length is a more reliable source of numStates
			// since cas.actions may be undersized
			int numStates = permut.length;
			actions = new ArrayList<ArrayList<Object>>(numStates);
			for (int s = 0; s < numStates; s++) {
				actions.add(null);
			}
			for (int s = 0; s < numStates; s++) {
				if (s < cas.actions.size() && cas.actions.get(s) != null) {
					actions.set(permut[s], new ArrayList<>(cas.actions.get(s)));
				}
			}
		}
	}

	// Mutators
	
	/**
	 * Clear all actions for state {@code s}
	 */
	public void clearState(int s)
	{
		if (actions != null && actions.get(s) != null) {
			actions.get(s).clear();
		}
	}
	
	/**
	 * Set the action label for choice {@code i} of state {@code s}. 
	 */
	public void setAction(int s, int i, Object action)
	{
		// Create main list if not done yet 
		if (actions == null) {
			actions = new ArrayList<ArrayList<Object>>();
		}
		// Expand main list up to state s if needed,
		// storing null for newly added items 
		if (s >= actions.size()) {
			int n = s - actions.size() + 1;
			for (int j = 0; j < n; j++) {
				actions.add(null);
			}
		}
		// Create action list for state s if needed
		ArrayList<Object> list;
		if ((list = actions.get(s)) == null) {
			actions.set(s, (list = new ArrayList<Object>()));
		}
		// Expand action list up to choice i if needed,
		// storing null for newly added items 
		if (i >= list.size()) {
			int n = i - list.size() + 1;
			for (int j = 0; j < n; j++) {
				list.add(null);
			}
		}
		// Store the action
		list.set(i, action);
	}
	
	// Accessors
	
	public Object getAction(int s, int i)
	{
		// Null list means no (null) actions everywhere
		if (actions == null) {
			return null;
		}
		try {
			ArrayList<Object> list = actions.get(s);
			// Null list means no (null) actions in this state
			if (list == null) {
				return null;
			}
			return list.get(i);
		}
		// Lists may be under-sized, indicating no action added 
		catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
	
	/**
	 * Convert to "sparse" storage for a given model,
	 * i.e., a single array where all actions are stored in
	 * order, per state and then per choice.
	 * A corresponding NondetModel is required because the
	 * number of states and choices per state may be unknown.
	 * If this action storage is completely empty,
	 * then this method may simply return null.
	 */
	public Object[] convertToSparseStorage(NondetModel model)
	{
		if (actions == null) {
			return null;
		} else {
			Object arr[] = new Object[model.getNumChoices()];
			int numStates = model.getNumStates();
			int count = 0;
			for (int s = 0; s < numStates; s++) {
				int numChoices = model.getNumChoices(s);
				for (int i = 0; i < numChoices; i++) {
					arr[count++] = getAction(s, i);
				}
			}
			return arr;
		}
	}
}
