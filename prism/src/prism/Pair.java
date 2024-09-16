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
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair<?, ?> other = (Pair<?, ?>) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}
	
	@Override
	public String toString()
	{
		return "(" + first + "," + second + ")"; 
	}

	// Utility functions

	/**
	 * Static method to compare two pairs containing the same Comparable objects.
	 */
	public static <A extends Comparable,B extends Comparable> int compare(Pair<A,B> c, Pair<A,B> d)
	{
		int comp = c.first.compareTo(d.first);
		return (comp != 0) ? comp : c.second.compareTo(d.second);
	}
}
