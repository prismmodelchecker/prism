//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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

	/**
	 * Returns true if all indices in the supports of all distributions are in the set. 
	 */
	public boolean isSubsetOf(BitSet set)
	{
		for (Distribution itDist : this) {
			if (!itDist.isSubsetOf(set)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns true if at least one index in the support of some distribution is in the set. 
	 */
	public boolean containsOneOf(BitSet set)
	{
		for (Distribution itDist : this) {
			if (itDist.containsOneOf(set)) {
				return true;
			}
		}
		return false;
	}
	
	public String toString()
	{
		return (action == null ? "" : "\"" + action + "\":") + super.toString();
	}

	public boolean equals(Object o)
	{
		return super.equals(o) && action == ((DistributionSet) o).action;
	}

	/**
	 * Returns the index of the distribution {@code d}, i.e. the position in the order given by the iterator of this set
	 * @param d the distribution to look up
	 * @return the index of {@code d} or -1 if not found
	 */
	public int indexOf(Distribution d)
	{
		int i = -1;
		for (Distribution itDist : this) {
			i++;
			if (itDist.equals(d)) {
				return i;
			}
		}
		return -1;
	}
}
