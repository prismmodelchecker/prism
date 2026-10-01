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

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

import strat.StrategyInfo.UndefinedReason;

/**
 * Policy for resolving undefined strategy choices (see {@link StrategyInfo.UndefinedReason})
 * when a strategy is applied, e.g., when building the model it induces or simulating it.
 * For each reason why a choice is undefined, it specifies a {@link Resolution}.
 * By default, the first choice is used (FIRST), except when there is no information
 * about the choice (UNKNOWN), which is an error (ERROR).
 */
public class UndefinedChoicePolicy implements Cloneable
{
	/**
	 * Ways of resolving an undefined strategy choice in a state.
	 */
	public enum Resolution {
		/** Stop: no choice is made, i.e., the state becomes a deadlock */
		STOP,
		/** Stop, but add a self-loop (as when PRISM fixes deadlocks), i.e., the state becomes absorbing */
		LOOP,
		/** Use the first choice (more precisely, the choice(s) with the same action as the first) */
		FIRST,
		/** Use all choices: kept as separate choices if nondeterminism remains, otherwise picked uniformly at random */
		ALL,
		/** Report an error */
		ERROR;

		/**
		 * Get the resolution with the given (case-insensitive) name, or null if there is none.
		 */
		public static Resolution fromName(String name)
		{
			for (Resolution res : values()) {
				if (res.name().equalsIgnoreCase(name)) {
					return res;
				}
			}
			return null;
		}
	}

	/** Resolution for each reason */
	private EnumMap<UndefinedReason, Resolution> resolutions = new EnumMap<>(UndefinedReason.class);

	/**
	 * Create a policy with the default resolutions:
	 * FIRST for all reasons, except UNKNOWN (ERROR).
	 */
	public UndefinedChoicePolicy()
	{
		setForAllKnownReasons(Resolution.FIRST);
		set(UndefinedReason.UNKNOWN, Resolution.ERROR);
	}

	/**
	 * Set the resolution for a reason.
	 */
	public UndefinedChoicePolicy set(UndefinedReason reason, Resolution resolution)
	{
		resolutions.put(reason, resolution);
		return this;
	}

	/**
	 * Set the resolution for all reasons, except UNKNOWN
	 * (i.e., those where it is known that the choice does not matter).
	 */
	public UndefinedChoicePolicy setForAllKnownReasons(Resolution resolution)
	{
		for (UndefinedReason reason : UndefinedReason.values()) {
			if (reason != UndefinedReason.UNKNOWN) {
				resolutions.put(reason, resolution);
			}
		}
		return this;
	}

	/**
	 * Get the resolution for a reason (null, i.e., no reason given, is treated as UNKNOWN).
	 */
	public Resolution get(UndefinedReason reason)
	{
		return resolutions.get(reason == null ? UndefinedReason.UNKNOWN : reason);
	}

	@Override
	public UndefinedChoicePolicy clone()
	{
		try {
			UndefinedChoicePolicy copy = (UndefinedChoicePolicy) super.clone();
			copy.resolutions = new EnumMap<>(resolutions);
			return copy;
		} catch (CloneNotSupportedException e) {
			throw new InternalError("Object#clone is expected to work for Cloneable objects", e);
		}
	}

	@Override
	public String toString()
	{
		return resolutions.entrySet().stream().map(e -> e.getKey().toString().toLowerCase() + "=" + e.getValue().toString().toLowerCase()).collect(Collectors.joining(","));
	}
}
