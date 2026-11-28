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

package strat;

import strat.StrategyInfo.UndefinedReason;

/**
 * The (finite, deterministic) memory of a strategy, kept separately from its decisions,
 * e.g., for a strategy that switches between memoryless strategies according to
 * its memory value (see {@link FMStrategySwitching}). Memory values are 0, ..., {@link #size()}-1.
 */
public interface StrategyMemory
{
	/**
	 * Get the number of memory values.
	 */
	public int size();

	/**
	 * Get the memory value when starting in initial state {@code sInit}
	 * (-1 if unknown, e.g., the state is not one the memory was built for).
	 */
	public int initial(int sInit);

	/**
	 * Get the memory value after taking action {@code action} from (known) memory value {@code m}
	 * and moving to state {@code sNext} (-1 if unknown).
	 */
	public int update(int m, Object action, int sNext);

	/**
	 * Get a string describing (known) memory value {@code m}, as used in strategy exports
	 * ({@code state,memory=decision}). By default, the number itself. It should be a single
	 * name or number (not containing ",", "=" or parentheses), or a tuple of them in parentheses,
	 * as for {@link parser.State#toString()}, e.g. "(sat,open)".
	 */
	public default String toString(int m)
	{
		return Integer.toString(m);
	}

	/**
	 * Get the reason, if the memory value determines one, why the strategy's choice
	 * does not matter once the memory is {@code m} (e.g., its objective has been satisfied),
	 * or null if it does not. This is only used where the strategy leaves the choice undefined.
	 */
	public default UndefinedReason whyUndefined(int m)
	{
		return null;
	}
}
