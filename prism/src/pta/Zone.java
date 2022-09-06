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

public abstract class Zone
{
	/**
	 * Get parent PTA
	 */

	public abstract PTA getPTA();

	// Zone operations (modify the zone)

	/**
	 * Conjunction: add constraint x-y db
	 */
	public abstract void addConstraint(int x, int y, int db);

	/**
	 * Conjunction: add constraint c
	 */
	public abstract void addConstraint(Constraint c);

	/**
	 * Conjunction: add multiple constraints
	 */
	public abstract void addConstraints(Iterable<Constraint> constraints);

	/**
	 * Conjunction: with another zone
	 */
	public abstract void intersect(Zone z);

	/**
	 * Up, i.e. let time elapse, subject to some constraints
	 */
	public abstract void up(Iterable<Constraint> constraints);

	/**
	 * Up, i.e. let time elapse
	 */
	public void up()
	{
		up(null);
	}

	/**
	 * Down, i.e. zone which can reach this one, subject to some constraints
	 */
	public abstract void down(Iterable<Constraint> constraints);

	/**
	 * Down, i.e. zone which can reach this one
	 */
	public void down()
	{
		down(null);
	}
	
	/**
	 * Free, i.e. remove constraints on a clock
	 */
	public abstract void free(int x);

	/**
	 * Reset clock x
	 */
	public abstract void reset(int x, int v);

	/**
	 * Backward reset of clock x
	 */
	public abstract void backReset(int x, int v);

	/**
	 * c-Closure
	 */
	public abstract void cClosure(int c);

	// Zone operations (create new zone)
	
	/**
	 * Complement
	 * Creates non-convex zone so creates new one,
	 * and leaves this one unmodified
	 */
	public abstract NCZone createComplement();

	// Zone queries (do not modify the zone)
	
	/**
	 * Is this zone empty (i.e. inconsistent)?
	 */
	public abstract boolean isEmpty();

	/**
	 * Is constraint c satisfied by this zone (i.e does it overlap)?
	 */
	public abstract boolean isSatisfied(Constraint c);

	/**
	 * Is a DBM (fully) included in this zone?
	 */
	public abstract boolean includes(DBM dbm);
	
	/**
	 * Get the minimum value of a clock. 
	 */
	public abstract int getClockMin(int x);
	
	/**
	 * Get the maximum value of a clock. 
	 */
	public abstract int getClockMax(int x);
	
	/**
	 * Check if a clock is unbounded (can be infinite).
	 */
	public abstract boolean clockIsUnbounded(int x);
	
	/**
	 * Check if all clocks are unbounded (can be infinite).
	 */
	public abstract boolean allClocksAreUnbounded();
	
	// Misc
	
	/**
	 * Clone this zone
	 */
	public abstract Zone deepCopy();
	
	/**
	 * Get storage info string
	 */
	public abstract String storageInfo();
	
	// Standard Java methods

	public abstract int hashCode();

	public abstract boolean equals(Object o);
}
