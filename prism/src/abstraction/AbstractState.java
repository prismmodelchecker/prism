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

package abstraction;

public class AbstractState implements Comparable
{
	private int n;
	private int vars[];

	public AbstractState(int n)
	{
		this.n = n;
		vars = new int[n];
	}

	public void set(int i, int v)
	{
		vars[i] = v;
	}

	public int compareTo(Object o)
	{
		AbstractState s = (AbstractState) o;
		for (int i = 0; i < n; i++) {
			if (vars[i] < s.vars[i])
				return -1;
			if (vars[i] > s.vars[i])
				return 1;
		}
		return 0;
	}

	public boolean equals(Object o)
	{
		return compareTo(o) == 0;
	}

	public String toString()
	{
		String s = "(";
		for (int i = 0; i < n; i++) {
			s += "" + vars[i];
			if (i < n - 1)
				s += ",";
		}
		s += ")";
		return s;
	}
}
