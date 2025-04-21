//==============================================================================
//
//	Copyright (c) 2025-
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Class to manage the (ordered) list of action labels associated with a model.
 * The list can be obtained via {@link #getActions()} and the index of a given
 * action in the list can be found with {@link #actionIndex(Object)}.
 * <br><br>
 * A list of actions can be provided at the time of creation or, if not,
 * the list is created lazily from the {@code newActionSource} provided on creation.
 * Call {@link #markNeedsRecomputing()} to indicate that actions in the model
 * may have changed and the list therefore needs recomputing.
 * Actions can be added manually with {@link #addAction(Object)}
 * or {@link #addActions(List)}, which does not change whether
 * the list needs recomputing. The full list can be set by calling
 * {@link #setActions(List)}, after which it is assumed recomputation is _not_ needed.
 * In general, actions are not removed (unless {@link #clear()} is called)
 * in order to preserve the order/indexing of actions.
 */
public class ActionList
{
	/** Where to get new actions when the list needs recomputing */
	protected Supplier<List<Object>> newActionSource;

	/** The list of actions */
	protected List<Object> actionList;

	/** Map from actions to indices */
	protected Map<Object,Integer> actionLookup;

	/** Does the list need recomputing? */
	protected boolean needsRecomputing;

	/**
	 * Construct a (for now empty) new {@link ActionList}
	 * with a supplier of new actions for when the list needs (re)computing.
	 * Since the list is empty, it is assumed that it needs recomputing.
	 */
	public ActionList(Supplier<List<Object>> newActionSource)
	{
		this.newActionSource = newActionSource;
		actionList = new ArrayList<>();
		actionLookup = new HashMap<>();
		// Initially empty and therefore almost certainly incomplete
		needsRecomputing = true;
	}

	/**
	 * Construct a new {@link ActionList} from a list of (Object) actions.
	 * plus a supplier of new actions for when the list needs recomputing.
	 * For now, it is assumed that the list does _not_ need recomputing.
	 */
	public ActionList(List<Object> actions, Supplier<List<Object>> newActionSource)
	{
		this(newActionSource);
		setActions(actions);
	}

	/**
	 * Copy action info from another ActionList
	 * (but not the source of new actions)
	 */
	public void copyFrom(ActionList other)
	{
		actionList = new ArrayList<>(other.actionList);
		actionLookup = new HashMap<>(other.actionLookup);
		needsRecomputing = other.needsRecomputing;
	}

	/**
	 * Set the list of actions.
	 * After this, it is assumed that the list does _not_ need recomputing.
	 */
	public void setActions(List<Object> actions)
	{
		actionList.clear();
		actionLookup.clear();
		addActions(actions);
		needsRecomputing = false;
	}

	/**
	 * Clear the action info.
	 * Since the list is now empty, it is assumed that it needs recomputing.
	 */
	public void clear()
	{
		actionList.clear();
		actionLookup.clear();
		// Empty and therefore almost certainly incomplete
		needsRecomputing = true;
	}

	/**
	 * Add an action to the list (if it is not already present)
	 * and return its index in the list.
	 */
	public int addAction(Object action)
	{
		Integer index = actionLookup.get(action);
		if (index == null) {
			index = actionList.size();
			actionLookup.put(action, index);
			actionList.add(action);
		}
		return index;
	}

	/**
	 * Add actions to the list (any that are not already present).
	 */
	public void addActions(List<Object> actionList)
	{
		actionList.forEach(this::addAction);
	}

	/**
	 * Mark the action list as needing recomputing.
	 */
	public void markNeedsRecomputing()
	{
		needsRecomputing = true;
	}

	/**
	 * Recompute the list of actions, from the new action source.
	 * Existing actions and indices are kept.
	 */
	private void recompute()
	{
		newActionSource.get().forEach(this::addAction);
		needsRecomputing = false;
	}

	/**
	 * Get the list of actions, recomputing and caching if needed.
	 */
	public List<Object> getActions()
	{
		if (needsRecomputing) {
			recompute();
		}
		return actionList;
	}

	/**
	 * Get the index (in this list) of an action label.
	 * Returns -1 if the list is not found.
	 */
	public int actionIndex(Object action)
	{
		if (needsRecomputing) {
			recompute();
		}
		Integer index = actionLookup.get(action);
		return index == null ? -1 : index;
	}

	/**
	 * Default conversion of an action label to a string
	 * ({@code toString()} or {@code ""} for {@code null}).
	 */
	static String actionString(Object action)
	{
		return action == null ? "" : action.toString();
	}

	@Override
	public String toString()
	{
		return actionList.toString();
	}
}
