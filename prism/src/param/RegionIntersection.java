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

/**
 * Maintains the intersection of two regions.
 * This class is to be used in combination with
 * {@code RegionValuesIntersections} to iterate over the intersection
 * of two (or three) {@RegionValues}.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 * @see RegionValuesIntersections
 */
final class RegionIntersection {
	private Region region;
	private StateValues values1;
	private StateValues values2;
	private StateValues values3;

	public RegionIntersection(Region region, StateValues values1, StateValues values2)
	{
		this(region, values1, values2, null);
	}

	public RegionIntersection(Region region, StateValues values1, StateValues values2, StateValues values3)
	{
		this.region = region;
		this.values1 = values1;
		this.values2 = values2;
		this.values3 = values3;
	}

	public Region getRegion()
	{
		return region;
	}

	public StateValues getStateValues1()
	{
		return values1;
	}
	
	public StateValues getStateValues2()
	{
		return values2;
	}

	public StateValues getStateValues3()
	{
		return values3;
	}

}
