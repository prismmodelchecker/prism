//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Xueyi Zou <xz972@york.ac.uk> (University of York)
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

import java.util.List;

import parser.State;

/**
 * Interface for classes that provide (read-only) access to an explicit-state model with partial observability.
 */
public interface PartiallyObservableModel extends Model
{
	// Accessors

	/**
	 * Get access to a list of observations (optionally stored).
	 */
	public List<State> getObservationsList();

	/**
	 * Get access to a list of unobservations (optionally stored).
	 */
	public List<State> getUnobservationsList();

	/**
	 * Get the total number of observations over all states.
	 */
	public default int getNumObservations()
	{
		return getObservationsList().size();
	}

	/**
	 * Get the total number of unobservations over all states.
	 */
	public default int getNumUnobservations()
	{
		return getUnobservationsList().size();
	}

	/**
	 * Get the observation of state {@code s}.
	 */
	public int getObservation(int s);

	/**
	 * Get the observation of state {@code s} as a {@code State}.
	 * Equivalent to calling {@link #getObservation(int)}
	 * and then looking up via {@link #getObservationsList()}.
	 * Returns null if the observation is unavailable.
	 */
	public default State getObservationAsState(int s)
	{
		int o = getObservation(s);
		List<State> obsList = getObservationsList();
		if (obsList != null && obsList.size() > o) {
			return obsList.get(o);
		}
		return null;
	}

	/**
	 * Get the unobservation of state {@code s}.
	 */
	public int getUnobservation(int s);

	/**
	 * Get the unobservation of state {@code s} as a {@code State}.
	 * Equivalent to calling {@link #getUnbservation(int)}
	 * and then looking up via {@link #getUnobservationsList()}.
	 * Returns null if the unobservation is unavailable.
	 */
	public default State getUnobservationAsState(int s)
	{
		int u = getUnobservation(s);
		List<State> unobsList = getUnobservationsList();
		if (unobsList != null && unobsList.size() > u) {
			return unobsList.get(u);
		}
		return null;
	}

	/**
	 * Get the probability of observing observation {@code o} in state {@code s}.
	 */
	public default double getObservationProb(int s, int o)
	{
		return this.getObservation(s) == o ? 1.0 : 0.0;
	}

	/**
	 * Get the number of choices for observation {@code 0}.
	 */
	public int getNumChoicesForObservation(int o);
}