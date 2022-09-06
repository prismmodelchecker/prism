//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

import java.util.Map.Entry;

/**
 * Simple class to store a pair of values.
 */
public class Pair<X,Y> implements Entry<X, Y>
{
	public X first;
	public Y second;
	
	public Pair(X first, Y second)
	{
		this.first = first;
		this.second = second;
	}

	@Override
	public X getKey() {
		return first;
	}

	@Override
	public Y getValue() {
		return second;
	}

	@Override
	public Y setValue(Y value) {
		second = value;
		return second;
	}
	
	@Override
	public String toString()
	{
		return "(" + first + "," + second + ")"; 
	}
}
