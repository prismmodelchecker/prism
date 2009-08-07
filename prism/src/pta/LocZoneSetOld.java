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

package pta;

import java.util.*;

public class LocZoneSetOld extends ArrayList<LocZone>
{
	private int indexOfLastAdd = -1;

	public int getIndexOfLastAdd()
	{
		return indexOfLastAdd;
	}

	public boolean add(int loc, Zone z)
	{
		return add(new LocZone(loc, z));
	}

	// Override add to store index
	public boolean add(LocZone lz)
	{
		int i = indexOf(lz);
		if (i == -1) {
			indexOfLastAdd = size();
			super.add(lz);
			return true;
		} else {
			indexOfLastAdd = i;
			return false;
		}
	}
	
	public ArrayList<LocZone> toArrayList()
	{
		return this;
	}
}
