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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Computes the intersections of two (or three) regions.
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
final class RegionValuesIntersections implements Iterable<RegionIntersection> {
	final private class RegionIntersectionOperator implements Iterator<RegionIntersection> {
		private RegionValues regions1;
		private RegionValues regions2;
		private RegionValues regions3;
		private int numRegions1;
		private int regions1Index;
		private boolean hasNext;
		private Region region;
		private StateValues stateValues1;
		private StateValues stateValues2;
		private StateValues stateValues3;

		RegionIntersectionOperator(RegionValues regions1, RegionValues regions2, RegionValues regions3)
		{
			this.regions1 = regions1;
			this.regions2 = regions2;
			this.regions3 = regions3;
			regions1Index = 0;
			numRegions1 = regions1.getNumRegions();
			region = null;
			stateValues1 = null;
			stateValues2 = null;
			stateValues3 = null;
			findNext();
		}
		
		private void findNext() {
			boolean found = false;
			while (regions1Index < numRegions1 && !found) {
				region = regions1.getRegion(regions1Index);
				stateValues1 = regions1.getResult(region);
				stateValues2 = regions2.getResult(region);
				stateValues3 = regions3 != null ? regions3.getResult(region) : null;
				found = (stateValues2 != null
						&& (regions3 == null || stateValues3 != null));
				regions1Index++;
			}
			hasNext = found;
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public RegionIntersection next() {
			if (hasNext) {
				RegionIntersection result = new RegionIntersection(region, stateValues1, stateValues2, stateValues3);
				findNext();
				return result;
			} else {
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private RegionValues regions1;
	private RegionValues regions2;
	private RegionValues regions3;

	public RegionValuesIntersections(RegionValues regions1, RegionValues regions2) {
		regions1.cosplit(regions2);
		this.regions1 = regions1;
		this.regions2 = regions2;
		this.regions3 = null;
	}

	public RegionValuesIntersections(RegionValues regions1, RegionValues regions2, RegionValues regions3) {
		regions1.cosplit(regions2, regions3);
		this.regions1 = regions1;
		this.regions2 = regions2;
		this.regions3 = regions3;
	}

	@Override
	public Iterator<RegionIntersection> iterator() {
		return new RegionIntersectionOperator(regions1, regions2, regions3);
	}

}
