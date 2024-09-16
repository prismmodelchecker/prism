//==============================================================================
//
//	Copyright (c) 2024-
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

import parser.State;
import prism.PrismException;

import java.util.ArrayList;
import java.util.List;

/**
 * Explicit-state storage of the info about observations in a (partially observable) model.
 *
 * Uses simple, mutable data structures, matching the "Simple" range of models.
 */
public class ObservationsSimple
{
	/**
	 * Number of states in the parent model
	 */
	protected int numStates;

	/**
	 * Information about the observations of this model.
	 * Each observation is a State containing the value for each observable.
	 */
	protected List<State> observationsList;

	/**
	 * Information about the unobservations of this model.
	 * Each observation is a State containing the value of variables that are not observable.
	 */
	protected List<State> unobservationsList;

	/** One state corresponding to each observation (used to look up info about it) */
	protected List<Integer> observationStates;

	/** Observable assigned to each state */
	protected List<Integer> observablesMap;

	/** Unobservable assigned to each state */
	protected List<Integer> unobservablesMap;

	/**
	 * Constructor: empty observation info
	 */
	public ObservationsSimple()
	{
		numStates = 0;
		observationsList = new ArrayList<>();
		unobservationsList = new ArrayList<>();
		observationStates = new ArrayList<>();
		observablesMap = new ArrayList<>();
		unobservablesMap = new ArrayList<>();
	}

	/**
	 * Constructor: initialise storage for observable info when the model has {@code numStates} states
	 */
	public ObservationsSimple(int numStates)
	{
		this.numStates = numStates;
		observationsList = new ArrayList<>();
		unobservationsList = new ArrayList<>();
		observationStates = new ArrayList<>();
		observablesMap = new ArrayList<>();
		unobservablesMap = new ArrayList<>();
		for (int i = 0; i < numStates; i++) {
			observablesMap.add(-1);
			unobservablesMap.add(-1);
		}
	}

	/**
	 * Copy constructor
	 */
	public ObservationsSimple(ObservationsSimple obs)
	{
		this.numStates = obs.numStates;
		observationsList = new ArrayList<>(obs.observationsList);
		unobservationsList = new ArrayList<>(obs.unobservationsList);
		observationStates = new ArrayList<>(obs.observationStates);
		observablesMap = new ArrayList<>(obs.observablesMap);
		unobservablesMap = new ArrayList<>(obs.unobservablesMap);
	}

	/**
	 * Copy constructor, but with a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 */
	public ObservationsSimple(ObservationsSimple obs, int permut[])
	{
		this.numStates = obs.numStates;
		observationsList = new ArrayList<>(obs.observationsList);
		unobservationsList = new ArrayList<>(obs.unobservationsList);
		int numObservations = obs.observationsList.size();
		observationStates = new ArrayList<>(numObservations);
		for (int o = 0; o < numObservations; o++) {
			int s = obs.observationStates.get(o);
			observationStates.add(s == -1 ? -1 : permut[s]);
		}
		observablesMap = new ArrayList<Integer>(numStates);
		unobservablesMap = new ArrayList<Integer>(numStates);
		for (int s = 0; s < numStates; s++) {
			observablesMap.add(-1);
			unobservablesMap.add(-1);
		}
		for (int s = 0; s < numStates; s++) {
			observablesMap.set(permut[s], obs.observablesMap.get(s));
			unobservablesMap.set(permut[s], obs.unobservablesMap.get(s));
		}
	}

	// Mutators

	/**
	 * Clear observation info for state {@code s}.
	 */
	public void clearState(int s)
	{
		observablesMap.set(s, -1);
		unobservablesMap.set(s, -1);
	}


	/**
	 * Initialise observation info for {@code numToAdd} new states.
	 */
	public void addStates(int numToAdd)
	{
		numStates += numToAdd;
		for (int i = 0; i < numToAdd; i++) {
			observablesMap.add(-1);
			unobservablesMap.add(-1);
		}
	}

	/**
	 * Set the associated (read-only) observation list.
	 */
	public void setObservationsList(List<State> observationsList)
	{
		this.observationsList = observationsList;
	}

	/**
	 * Set the associated (read-only) unobservation list.
	 */
	public void setUnobservationsList(List<State> unobservationsList)
	{
		this.unobservationsList = unobservationsList;
	}

	/**
	 * Set the observation info for a state.
	 * If the actions for existing states with this observation do not match,
	 * an explanatory exception is thrown (so this should be done after transitions
	 * have been added to the state). Optionally, a list of names of the
	 * observables can be passed for error reporting.
	 * @param s State
	 * @param observ Observation
	 * @param unobserv Unobservation
	 * @param observableNames Names of observables (optional)
	 * @param model Parent model (for action checks, optional)
	 */
	public void setObservation(int s, State observ, State unobserv, List<String> observableNames, NondetModel model) throws PrismException
	{
		// See if the observation already exists and add it if not
		int oIndex = observationsList.indexOf(observ);
		if (oIndex == -1) {
			// Add new observation
			observationsList.add(observ);
			oIndex = observationsList.size() - 1;
			// Also extend the observationStates list, to be filled shortly
			observationStates.add(-1);
		}
		// Assign the observation (index) to the state
		try {
			setObservation(s, oIndex, model);
		} catch (PrismException e) {
			String sObs = observableNames == null ? observ.toString() : observ.toString(observableNames);
			throw new PrismException("Problem with observation " + sObs + ": " + e.getMessage());
		}
		// See if the unobservation already exists and add it if not
		int unobservIndex = unobservationsList.indexOf(unobserv);
		if (unobservIndex == -1) {
			unobservationsList.add(unobserv);
			unobservIndex = unobservationsList.size() - 1;
		}
		// Assign the unobservation (index) to the state
		unobservablesMap.set(s, unobservIndex);
	}

	/**
	 * Assign observation with index o to state s.
	 * (assumes observation has already been added to the list)
	 * If the actions for existing states with this observation do not match,
	 * an explanatory exception is thrown (so this should be done after transitions
	 * have been added to the state).
	 * @param s State
	 * @param o Observation
	 * @param model Parent model (for action checks, optional)
	 */
	protected void setObservation(int s, int o, NondetModel model) throws PrismException
	{
		// Set observation
		observablesMap.set(s, o);
		// If this is first state with this observation, store its index
		int observationState = observationStates.get(o);
		if (observationState == -1) {
			observationStates.set(o, s);
		}
		// Otherwise, check that the actions for existing states with
		// the same observation match this one
		else {
			if (model != null) {
				model.checkActionsMatchExactly(s, observationState);
			}
		}
	}

	/**
	 * Set the observation for each state {@code s} to be {@code s}.
	 */
	protected void setIdentityObservations()
	{
		for (int s = 0; s < numStates; s++) {
			// Observation of a state is the state itself
			try {
				setObservation(s, s, null);
			} catch (PrismException e) {
				// Won't happen
			}
			// Unobservation of a state is null
			unobservablesMap.set(s, null);
		}
	}

	// Accessors

	/**
	 * Get access to a list of observations (optionally stored).
	 */
	public List<State> getObservationsList()
	{
		return observationsList;
	}

	/**
	 * Get access to a list of unobservations (optionally stored).
	 */
	public List<State> getUnobservationsList()
	{
		return unobservationsList;
	}

	/**
	 * Get the observation of state {@code s}.
	 */
	public int getObservation(int s)
	{
		return observablesMap == null ? -1 : observablesMap.get(s);
	}

	/**
	 * Get the unobservation of state {@code s}.
	 */
	public int getUnobservation(int s)
	{
		return unobservablesMap.get(s);
	}

	/**
	 * Get the index of one state corresponding to observation {@code o}.
	 */
	public int getObservationState(int o)
	{
		return observationStates.get(o);
	}
}
