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

package io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Storage of values for reward structures, for the purposes of import/export.
 */
public class RewardTuple<Value>
{
	private List<Value> values;

	public RewardTuple()
	{
		values = new ArrayList<>();
	}

	public RewardTuple(int numRewardStructs)
	{
		values = new ArrayList<>(numRewardStructs);
	}

	public void add(Value value)
	{
		values.add(value);
	}

	public Value get(int i)
	{
		return values.get(i);
	}

	public String toString(Function<Value, String> valueFormatter, String sep)
	{
		return "[" + values.stream().map(e -> valueFormatter.apply(e)).collect(Collectors.joining(sep)) + "]";
	}

	public String toStringReversed(Function<Value, String> valueFormatter, String sep)
	{
		return "[" + values.stream().sorted(Collections.reverseOrder()).map(e -> valueFormatter.apply(e)).collect(Collectors.joining(sep)) + "]";
		//return "[" + values.reversed().stream().map(e -> valueFormatter.apply(e)).collect(Collectors.joining(sep)) + "]";
	}

	@Override
	public String toString()
	{
		return "[" + values.stream().map(e -> e.toString()).collect(Collectors.joining(",")) + "]";
	}
}
