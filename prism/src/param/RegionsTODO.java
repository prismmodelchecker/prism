//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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

package param;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
final class RegionsTODO {
	class RegionsByVolumeComparator implements Comparator<Region> {

		@Override
		public int compare(Region reg1, Region reg2) {
			return reg1.volume().compareTo(reg2.volume()) * -1;
		}
	}

	private PriorityQueue<Region> regions;

	public RegionsTODO() {
		regions = new PriorityQueue<Region>(11, new RegionsByVolumeComparator());
	}
	
	int size() {
		return regions.size();
	}

	boolean isEmpty()
	{
		return regions.isEmpty();
	}

	void add(Region e)
	{
		regions.add(e);
	}
	
	void addAll(List<Region> newRegions)
	{
		regions.addAll(newRegions);
	}

	Region poll()
	{
		Region result = regions.poll();
		return result;
	}
}
