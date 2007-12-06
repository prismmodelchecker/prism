//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package odd;

public class ODDNode
{
	private long ptr;
	
	public ODDNode(long p)
	{
		ptr = p;
	}
	
	public ODDNode(ODDNode odd)
	{
		ptr = odd.ptr;
	}
	
	public long ptr()
	{
		return ptr;
	}

	public long getTOff()
	{	
		return ODDUtils.ODD_GetTOff(ptr);
	}

	public long getEOff()
	{	
		return ODDUtils.ODD_GetEOff(ptr);
	}

	public ODDNode getThen()
	{
		return new ODDNode(ODDUtils.ODD_GetThen(ptr));
	}
	
	public ODDNode getElse()
	{
		return new ODDNode(ODDUtils.ODD_GetElse(ptr));
	}

	public boolean equals(ODDNode odd)
	{
		return ptr == odd.ptr;
	}
	
	public String toString()
	{
		return "" + ptr;
	}
}

//------------------------------------------------------------------------------
