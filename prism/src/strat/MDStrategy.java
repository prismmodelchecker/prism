//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Aistis Simaitis <aistis.aimaitis@cs.ox.ac.uk> (University of Oxford)
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

import prism.PrismLog;

/**
 * Interface for classes to store memoryless deterministic (MD) strategies.
 */
public interface MDStrategy<Value> extends Strategy<Value>
{
	// Additional queries for memoryless strategies (just ignore memory)
	
	/**
	 * Get the action chosen by the strategy in the state index s
	 * Returns {@link StrategyInfo#UNDEFINED} if undefined.
	 */
	public default Object getChoiceAction(int s)
	{
		return getChoiceAction(s, -1);
	}
	
	/**
	 * Get the index of the choice picked by the strategy in the state index s.
	 * The index is defined with respect to a particular model, stored locally.
	 * Returns a negative value (not necessarily -1) if undefined.
	 */
	public default int getChoiceIndex(int s)
	{
		return getChoiceIndex(s, -1);
	}

	/**
	 * Is a choice defined by the strategy in the state index s.
	 */
	public default boolean isChoiceDefined(int s)
	{
		return isChoiceDefined(s, -1);
	}

	/**
	 * Get a string representing the choice made by the strategy in the state index s.
	 * For unlabelled choices, this should return "", not null.
	 * This may also indicate the reason why it is undefined, if it is.
	 */
	public default String getChoiceActionString(int s)
	{
		return getChoiceActionString(s, -1);
	}

	// Methods for Strategy
	
	@Override
	public default void exportActions(PrismLog out, StrategyExportOptions options)
	{
		int n = getNumStates();
		for (int s = 0; s < n; s++) {
			if (isChoiceDefined(s))
				out.println(s + "=" + getChoiceActionString(s));
		}
	}

	@Override
	public default void exportIndices(PrismLog out, StrategyExportOptions options)
	{
		int n = getNumStates();
		for (int s = 0; s < n; s++) {
			if (isChoiceDefined(s))
				out.println(s + "=" + getChoiceIndex(s));
		}
	}
}
