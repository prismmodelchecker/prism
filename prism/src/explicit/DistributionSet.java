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

package explicit;

import java.util.*;

/**
 * Represents a set of distributions.
 * The order in which distributions are inserted is preserved
 * (but does not affect equality of distributions sets).
 */
public class DistributionSet extends LinkedHashSet<Distribution>
{
	private static final long serialVersionUID = 1L;
	
	public Object action;

	public DistributionSet(Object action)
	{
		super();
		this.action = action;
	}

	public Object getAction()
	{
		return action;
	}

	public void setAction(Object action)
	{
		this.action = action;
	}

	public String toString()
	{
		return (action == null ? "" : "\"" + action + "\":") + super.toString();
	}

	public boolean equals(Object o)
	{
		return super.equals(o) && action == ((DistributionSet) o).action;
	}
}
