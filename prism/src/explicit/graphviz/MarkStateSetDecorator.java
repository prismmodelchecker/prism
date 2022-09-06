//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

import java.util.BitSet;

/**
 * A decorator for states: Given a BitSet of state indizes, fills
 * the nodes corresponding to these states with a given color.
 */
public class MarkStateSetDecorator implements Decorator
{
	/** The set of states to be marked */
	private BitSet stateSet;
	/** The fill colour for the node */
	private String fillColour = "#cccccc";

	/**
	 * Constructor, default colour grey.
	 * @param stateSet the set of state indices to be marked.
	 */
	public MarkStateSetDecorator(BitSet stateSet)
	{
		this.stateSet = stateSet;
	}

	/**
	 * Constructor, default colour grey.
	 * @param stateSet the set of state indices to be marked.
	 * @param fillColor HTML colour string for the fill colour
	 */
	public MarkStateSetDecorator(BitSet set, String fillColor)
	{
		this.stateSet = set;
		this.fillColour = fillColor;
	}

	/**
	 * Add "style=filled" and "fillcolor=..." attributes
	 * to the decoration.
	 */
	@Override
	public Decoration decorateState(int state, Decoration d)
	{
		if (stateSet.get(state)) {
			d.attributes().put("style", "filled");
			d.attributes().put("fillcolor", fillColour);
		}
		return d;
	}

}
