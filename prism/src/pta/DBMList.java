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

/**
 * List of DBMs representing a non-convex zone.
 * 
 * See this reference for some useful algorithms:
 * S. Tripakis. The formal analysis of timed systems in practice. Joseph Fourier University, 1998.
 */
public class DBMList extends NCZone
{
	/* Parent PTA */
	protected PTA pta;
	/* List of DBMs */
	protected ArrayList<DBM> list;

	/* Should we check for inclusion of DBMs as we go? */
	public static boolean checkInclusion = true;

	/**
	 * Default constructor
	 */
	public DBMList(PTA pta)
	{
		this.pta = pta;
		list = new ArrayList<DBM>();
	}

	/**
	 * Copy constructor: create single list from existing DBM(List)
	 */
	public DBMList(Zone z)
	{
		// From DBM 
		if (z instanceof DBM) {
			DBM dbm = (DBM)z;
			this.pta = dbm.pta;
			list = new ArrayList<DBM>();
			addDBM(dbm);
		}
		// From DBM 
		else { 
			DBMList dbml = (DBMList)z;
			this.pta = dbml.pta;
			list = new ArrayList<DBM>();
			for (DBM dbm : dbml.list) {
				list.add((DBM) dbm.deepCopy());
			}
		}
	}

	/**
	 * Add a DBM to the list
	 */
	public void addDBM(DBM dbm)
	{
		int i, n;
		BitSet toRemove;
		// If we don't want to check inclusions, just add
		if (!checkInclusion) {
			list.add(dbm);
		}
		// Otherwise look at existing DBMs
		else {
			n = list.size();
			toRemove = new BitSet(n);
			i = 0;
			for (DBM dbm2 : list) {
				if (dbm2.includes(dbm)) {
					// New DBM already included inside something - don't add 
					return;
				} else if (dbm.includes(dbm2)) {
					// This DBM is will be redundant when new DBM is added
					toRemove.set(i);
				}
				i++;
			}
			// Remove (now) redundant ones
			for (i = n - 1; i >= 0; i--) {
				if (toRemove.get(i))
					list.remove(i);
			}
			// Add DBM
			list.add(dbm);
		}
	}

	/**
	 * Add a list of DBMs to the list
	 */
	public void addDBMs(DBMList dbml)
	{
		for (DBM dbm : dbml.list)
			addDBM(dbm);
	}

	/**
	 * Get the {@code i}th DBM in the list.
	 */
	public DBM getDBM(int i) 
	{
		return list.get(i);
	}

	// Methods required for Zone interface

	/**
	 * Get parent PTA
	 */
	public PTA getPTA()
	{
		return pta;
	}
	
	// Zone operations (modify the zone)

	/**
	 * Conjunction: add constraint x-y db
	 */
	public void addConstraint(int x, int y, int db)
	{
		for (DBM dbm : list) {
			dbm.addConstraint(x, y, db);
		}
		if (checkInclusion)
			removeInclusions();
	}

	/**
	 * Conjunction: add constraint c
	 */
	public void addConstraint(Constraint c)
	{
		addConstraint(c.x, c.y, c.db);
	}

	/**
	 * Conjunction: add multiple constraints
	 */
	public void addConstraints(Iterable<Constraint> constraints)
	{
		for (Constraint c : constraints)
			addConstraint(c);
	}

	/**
	 * Conjunction: with another zone
	 */
	public void intersect(Zone z)
	{
		int i, j, n1, n2;
		if (z instanceof DBM)
			z = new DBMList((DBM) z);
		DBMList dbml = (DBMList) z;
		DBMList listNew;
		DBM dbmNew;
		listNew = new DBMList(pta);
		n1 = list.size();
		n2 = dbml.list.size();
		for (i = 0; i < n1; i++) {
			for (j = 0; j < n2; j++) {
				dbmNew = list.get(i).deepCopy();
				dbmNew.intersect(dbml.list.get(j));
				if (!dbmNew.isEmpty())
					listNew.addDBM(dbmNew);
			}
		}
		list = listNew.list;
	}

	/**
	 * Up, i.e. let time elapse, subject to some constraints
	 */
	public void up(Iterable<Constraint> constraints)
	{
		for (DBM dbm : list) {
			dbm.up(constraints);
		}
		if (checkInclusion)
			removeInclusions();
	}

	/**
	 * Down, i.e. zone which can reach this one, subject to some constraints
	 */
	public void down(Iterable<Constraint> constraints)
	{
		for (DBM dbm : list) {
			dbm.down(constraints);
		}
		if (checkInclusion)
			removeInclusions();
	}

	/**
	 * Free, i.e. remove constraints on a clock
	 */
	public void free(int x)
	{
		// Existential quantification distributes over union 
		for (DBM dbm : list) {
			dbm.free(x);
		}
		if (checkInclusion)
			removeInclusions();
	}

	/**
	 * Reset clock x
	 */
	public void reset(int x, int v)
	{
		// NOT IMPLEMENTED
		throw new RuntimeException("Not implemented yet");
	}

	/**
	 * Backward reset of clock x
	 */
	public void backReset(int x, int v)
	{
		// Implemented as conjunction, then "free"
		// (see method in DBM class),
		// both of which distribute over union
		for (DBM dbm : list) {
			dbm.addConstraint(x, 0, DB.createLeq(v));
			dbm.addConstraint(0, x, DB.createLeq(-v));
		}
		for (DBM dbm : list) {
			dbm.free(x);
		}
		if (checkInclusion)
			removeInclusions();
	}

	/**
	 * c-Closure
	 */
	public void cClosure(int c)
	{
		// NOT IMPLEMENTED
		throw new RuntimeException("Not implemented yet");
	}

	// Zone operations (create new zone)
	
	/**
	 * Complement
	 * Creates non-convex zone so creates new one,
	 * and leaves this one unmodified
	 */
	public DBMList createComplement()
	{
		DBMList dbml = deepCopy();
		dbml.complement();
		return dbml;
	}

	// Zone queries (do not modify the zone)
	
	/**
	 * Is this zone empty (i.e. inconsistent)?
	 */
	public boolean isEmpty()
	{
		for (DBM dbm : list) {
			if (!dbm.isEmpty())
				return false;
		}
		return true;
	}

	/**
	 * Is constraint c satisfied by this zone (i.e does it overlap)?
	 */
	public boolean isSatisfied(Constraint c)
	{
		for (DBM dbm : list) {
			if (!dbm.isSatisfied(c))
				return false;
		}
		return true;
	}

	/**
	 * Is a DBM (fully) included in this zone?
	 */
	public boolean includes(DBM dbm)
	{
		for (DBM dbm2 : list) {
			if (dbm2.includes(dbm))
				return true;
		}
		return false;
	}

	/**
	 * Get the minimum value of a clock. 
	 */
	public int getClockMin(int x)
	{
		// Take min across all DBMs
		int min = Integer.MAX_VALUE;
		for (DBM dbm : list) {
			min = Math.min(min, dbm.getClockMin(x));
		}
		return min;
	}
	
	/**
	 * Get the maximum value of a clock. 
	 */
	public int getClockMax(int x)
	{
		// Take max across all DBMs
		int max = Integer.MIN_VALUE;
		for (DBM dbm : list) {
			max = Math.max(max, dbm.getClockMax(x));
		}
		return max;
	}
	
	/**
	 * Check if a clock is unbounded (can be infinite).
	 */
	public boolean clockIsUnbounded(int x)
	{
		// Clock is unbounded if is in any DBM
		for (DBM dbm : list) {
			if (dbm.clockIsUnbounded(x))
				return true;
		}
		return false;
	}
	
	/**
	 * Check if all clocks are unbounded (can be infinite).
	 */
	public boolean allClocksAreUnbounded()
	{
		int i, n;
		n = pta.numClocks;
		for (i = 1; i < n + 1; i++) {
			if (!clockIsUnbounded(i)) {
				return false;
			}
		}
		return true;
	}
	
	// Methods required for NCZone interface
	
	/**
	 * Conjunction: with complement of another zone
	 */
	public void intersectComplement(Zone z)
	{
		if (z instanceof DBM)
			z = new DBMList((DBM) z);
		DBMList dbml = (DBMList) z;
		for (DBM dbm : dbml.list) {
			DBMList listTmp = dbm.createComplement();
			intersect(listTmp);
		}
	}

	/**
	 * Complement
	 */
	public void complement()
	{
		DBMList listNew, res;
		res = null;
		// Special case: complement of empty DBM list is True
		if (list.size() == 0)
			addDBM(DBM.createTrue(pta));
		// The complement of a list of DBMs is the intersection
		// of the complement of each DBM (and the complement of
		// of a DBM is a DBM list).
		for (DBM dbm : list) {
			listNew = dbm.createComplement();
			if (res == null)
				res = listNew;
			else {
				res.intersect(listNew);
			}
			// Stop early if res already empty
			if (res.list.size() == 0)
				break;
		}
		list = res.list;
	}

	/**
	 * Disjunction: with another zone
	 */
	public void union(Zone z)
	{
		if (z instanceof DBM) {
			addDBM((DBM) z);
		}
		else {
			addDBMs((DBMList) z);
		}
	}
	
	/**
	 * Get some (any) convex zone contained within this zone.
	 * Returns null if this zone is empty.
	 */
	public Zone getAZone()
	{
		if (list.size() > 0)
			return list.get(0);
		else return null;
	}
	
	/**
	 * Get the number of DBMs in this DBMList.
	 */
	public int size()
	{
		return list.size();
	}

	/**
	 * Clone this zone
	 */
	public DBMList deepCopy()
	{
		DBMList copy = new DBMList(pta);
		for (DBM dbm : list) {
			copy.list.add((DBM) dbm.deepCopy());
		}
		return copy;
	}

	// Standard Java methods

	@Override
	public int hashCode()
	{
		int hash = 0;
		for (DBM dbm : list) {
			hash = (hash * 7) + dbm.hashCode();
		}
		return hash;
	}

	@Override
	public boolean equals(Object o)
	{
		DBMList dbml;
		int i, n;
		if (o == null)
			return false;
		try {
			dbml = (DBMList) o;
		} catch (ClassCastException e) {
			return false;
		}
		n = list.size();
		if (n != dbml.list.size())
			return false;
		for (i = 0; i < n; i++) {
			if (!list.get(i).equals(dbml.list.get(i)))
				return false;
		}
		return true;
	}

	// To string methods

	@Override
	public String toString()
	{
		return "" + list;
	}

	/**
	 * Get storage info string
	 */
	public String storageInfo()
	{
		return "List of " + list.size() + " DBMs with " + pta.numClocks + " clocks";
	}

	// Static zone creation methods

	/**
	 * Create empty DBM list (i.e. false)
	 */
	public static DBMList createFalse(PTA pta)
	{
		DBMList dbml = new DBMList(pta);
		return dbml;
	}

	// Private utility methods

	private void removeInclusions()
	{
		int i, j, n;
		DBM dbm1, dbm2;
		BitSet toRemove;
		n = list.size();
		toRemove = new BitSet(n);
		for (i = 0; i < n; i++) {
			if (toRemove.get(i))
				continue;
			dbm1 = list.get(i);
			for (j = i + 1; j < n; j++) {
				if (toRemove.get(i) || toRemove.get(j))
					continue;
				dbm2 = list.get(j);
				if (dbm2.includes(dbm1))
					toRemove.set(i);
				else if (dbm1.includes(dbm2))
					toRemove.set(j);
			}
		}
		for (i = n - 1; i >= 0; i--) {
			if (toRemove.get(i))
				list.remove(i);
		}
	}
	
	// Test program for complementing big DBM lists
	
	public static void main(String args[])
	{
		int numClocks = 7;
		int i, j, x, y, db;
		Random generator = new Random();
		PTA pta = new PTA(Collections.emptyList());
		for (i = 0; i < numClocks; i++) {
			pta.addClock("" + i);
		}
		DBMList dbml = new DBMList(pta);
		for (i = 0; i < 5; i++) {
			DBM dbm = DBM.createTrue(pta);
			for (j = 0; j < 10; j++) {
				x = generator.nextInt(numClocks);
				y = generator.nextInt(numClocks);
				db = generator.nextInt();
				dbm.addConstraint(x, y, db);
			}
			dbml.addDBM(dbm);
		}
		System.out.println(dbml.storageInfo());
		dbml.complement();
		System.out.println(dbml.storageInfo());
	}
}
