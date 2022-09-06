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

import java.util.Map;

public class LocZone
{
	public int loc;
	public Zone zone;

	public LocZone(int loc, Zone zone)
	{
		this.loc = loc;
		this.zone = zone;
	}

	/**
	 * Do time part of post operation (not including c-closure).
	 * Note: pta is passed in just for efficiency, could find it if we wanted.
	 */
	public void tPost(PTA pta)
	{
		// Time successor (up)
		zone.up(pta.getInvariantConstraints(loc));
	}
	
	/**
	 * Do discrete part of post operation (wrt. an edge).
	 */
	public void dPost(Edge edge)
	{
		Transition tr = edge.getParent();
		// Intersect this zone with guard of edge's transition's guard 
		for (Constraint c : tr.getGuardConstraints()) {
			zone.addConstraint(c);
		}
		// Apply clock resets in edge
		for (Map.Entry<Integer,Integer>e : edge.getResets()) {
			zone.reset(e.getKey(), e.getValue());
		}
		// Intersect with invariant of target location 
		zone.addConstraints(tr.getParent().getInvariantConstraints(edge.getDestination()));
		// Set location to target location
		loc = edge.getDestination();
	}

	/**
	 * Do c-closure.
	 * Note: pta is passed in just for efficiency, could find it if we wanted.
	 */
	public void cClosure(PTA pta)
	{
		// c-Closure
		zone.cClosure(pta.getMaxClockConstraint());
	}
	
	/**
	 * Do time part of predecessor operation (not including c-closure).
	 * Note: pta is passed in just for efficiency, could find it if we wanted.
	 */
	public void tPre(PTA pta)
	{
		// Time predecessor (down)
		zone.down(pta.getInvariantConstraints(loc));
	}
	
	/**
	 * dPre: discrete predecessor
	 */
	public void dPre(Edge edge)
	{
		Transition tr = edge.getParent();
		// Backwards reset of clocks from edge
		for (Map.Entry<Integer,Integer>e : edge.getResets()) {
			zone.backReset(e.getKey(), e.getValue());
		}
		// Intersect with guard of edge's transition's guard 
		for (Constraint c : tr.getGuardConstraints()) {
			zone.addConstraint(c);
		}
		// Intersect with invariant of source location 
		zone.addConstraints(tr.getParent().getInvariantConstraints(tr.getSource()));
		// Set location to source location
		loc = tr.getSource();
	}

	/**
	 * Clone this location/zone pair
	 */
	public LocZone deepCopy()
	{
		return new LocZone(loc, zone.deepCopy());
	}
	
	// Standard Java methods

	public int hashCode()
	{
		return loc + zone.hashCode();
	}
	
	public boolean equals(Object o)
	{
		LocZone lz;
		if (o == null) return false;
		try {
			lz = (LocZone) o;
		} catch (ClassCastException e) {
			return false;
		}
		if (loc != lz.loc)
			return false;
		return (zone.equals(lz.zone));
	}

	public String toString()
	{
		return "(" + zone.getPTA().getLocationNameString(loc) + "," + zone + ")";
	}
}
