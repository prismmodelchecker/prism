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

package explicit.graphviz;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * A decorator that sets the shape of each state in a game model
 * to reflect the player that owns it.
 */
public class StateOwnerDecorator implements Decorator
{
	/**
	 * Shapes for players (Graphviz keywords).
	 */
	public static final List<String> playerShapes = Arrays.asList("diamond", "box", "oval", "hexagon");

	/**
	 * State ownership function: state (0-indexed) to player (0-indexed)
	 */
	protected Function<Integer, Integer> stateOwners;

	/** Constructor, single reward structure */
	public StateOwnerDecorator(Function<Integer, Integer> stateOwners)
	{
		this.stateOwners = stateOwners;
	}

	@Override
	public Decoration decorateState(int state, Decoration d)
	{
		int player = stateOwners.apply(state);
		d.attributes().put("shape", playerShapes.get(player % playerShapes.size()));
		return d;
	}
}
