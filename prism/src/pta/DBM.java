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

/**
 * Implementation of the difference-bound matrix (DBM) data structure.
 * 
 * Data structures and algorithms are mostly based on those from: 
 * Johan Bengtsson, Wang Yi: Timed Automata: Semantics, Algorithms and Tools.
 * Lectures on Concurrency and Petri Nets 2003, LNCS volume 3098, pages 87-124, Springer, 2004
 */
public class DBM extends Zone
{
	/* Parent PTA */
	protected PTA pta;
	/*
	 * Canonical zone representation: DBM
	 * (numClocks+1)^2 matrix d, indexed 1...numClocks for clocks in PTA and 0 for special zero clock.
	 * Each entry d[i][j] gives the bound for clock difference xi-xj.
	 * Difference bounds are encoded as a single integer; see help class DB for details.
	 */
	protected int d[][];

	/**
	 * Construct an empty DBM (don't use this).
	 */
	public DBM(PTA pta)
	{
		this.pta = pta;
		this.d = new int[pta.numClocks + 1][pta.numClocks + 1];
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
		// Check if this causes inconsistency (i.e. zone emptiness)
		// and, if so, flag this by setting d[0][0] to -1;
		if (DB.add(d[y][x], db) < DB.LEQ_ZERO)
			d[0][0] = DB.LEQ_MINUS_ONE;
		// Now add the constraint (if it is tighter than existing one)
		else if (db < d[x][y]) {
			int i, j, n, dTmp;
			// Store new constraint
			d[x][y] = db;
			// Partial re-canonicalisation
			// Note we do 2 (separate) outer iterations of Floyd-Warshall,
			// unlike the incorrect formulation in the DBM algorithm notes.
			n = pta.numClocks;
			for (i = 0; i < n + 1; i++) {
				for (j = 0; j < n + 1; j++) {
					dTmp = DB.add(d[i][x], d[x][j]);
					if (dTmp < d[i][j])
						d[i][j] = dTmp;
				}
			}
			for (i = 0; i < n + 1; i++) {
				for (j = 0; j < n + 1; j++) {
					dTmp = DB.add(d[i][y], d[y][j]);
					if (dTmp < d[i][j])
						d[i][j] = dTmp;
				}
			}
		}
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
		int i, j, n;
		DBM dbm = (DBM) z;
		n = pta.numClocks;
		for (i = 0; i < n + 1; i++) {
			for (j = 0; j < n + 1; j++) {
				if (i != j && !DB.isInfty(dbm.d[i][j]))
					addConstraint(i, j, dbm.d[i][j]);
			}
		}
	}

	/**
	 * Up, i.e. let time elapse, subject to some constraints
	 */
	public void up(Iterable<Constraint> constraints)
	{
		int i, n;
		n = pta.numClocks;
		for (i = 1; i < n + 1; i++) {
			d[i][0] = DB.INFTY;
		}
		if (constraints != null) {
			for (Constraint c : constraints) {
				addConstraint(c);
			}
		}
	}

	/**
	 * Down, i.e. zone which can reach this one, subject to some constraints
	 */
	public void down(Iterable<Constraint> constraints)
	{
		int i, j, n;
		n = pta.numClocks;
		for (i = 1; i < n + 1; i++) {
			d[0][i] = DB.LEQ_ZERO;
			for (j = 1; j < n + 1; j++) {
				if (d[j][i] < d[0][i])
					d[0][i] = d[j][i];
			}
		}
		if (constraints != null) {
			for (Constraint c : constraints) {
				addConstraint(c);
			}
		}
	}

	/**
	 * Free, i.e. remove constraints on a clock
	 */
	public void free(int x)
	{
		int i, n;
		n = pta.numClocks;
		for (i = 0; i < n + 1; i++) {
			if (i != x) {
				d[x][i] = DB.INFTY;
				d[i][x] = d[i][0];
			}
		}
	}

	/**
	 * Reset clock x
	 */
	public void reset(int x, int v)
	{
		int i, n;
		n = pta.numClocks;
		for (i = 0; i < n + 1; i++) {
			d[x][i] = DB.add(DB.createLeq(v), d[0][i]);
			d[i][x] = DB.add(d[i][0], DB.createLeq(-v));
		}
	}

	/**
	 * Backward reset of clock x
	 */
	public void backReset(int x, int v)
	{
		// Conjunction with constraint x=v
		// (i.e. x-0 <= v && 0-x <= -v)
		addConstraint(x, 0, DB.createLeq(v));
		addConstraint(0, x, DB.createLeq(-v));
		// Then "free" clock x
		free(x);
	}

	/**
	 * c-Closure
	 */
	public void cClosure(int c)
	{
		int i, j, n;
		if (isEmpty())
			return;
		n = pta.numClocks;
		for (i = 0; i < n + 1; i++) {
			for (j = 0; j < n + 1; j++) {
				if (!DB.isInfty(d[i][j]) && DB.createLeq(c) < d[i][j]) {
					d[i][j] = DB.INFTY;
				} else if (!DB.isInfty(d[i][j]) && d[i][j] < DB.createLt(-c)) {
					d[i][j] = DB.createLt(-c);
				}
			}
		}
		canonicalise();
	}

	// Zone operations (create new zone)
	
	/**
	 * Complement this DBM; creates a new DBM list as result.
	 */
	public DBMList createComplement()
	{
		DBMList list = new DBMList(pta);
		DBM dbmNew;
		int i, j, n;
		// Special case: complement of empty DBM is True
		if (isEmpty()) {
			list.addDBM(createTrue(pta));
			return list;
		}
		n = d.length - 1;
		for (i = 0; i < n + 1; i++) {
			for (j = 0; j < n + 1; j++) {
				if (i == j)
					continue;
				if (DB.isInfty(d[i][j]))
					continue;
				dbmNew = (DBM) new DBMFactory().createTrue(pta);
				dbmNew.addConstraint(j, i, DB.dual(d[i][j]));
				if (!dbmNew.isEmpty()) {
					list.addDBM(dbmNew);
				}
			}
		}
		return list;
	}

	// Zone queries (do not modify the zone)
	
	/**
	 * Is this zone empty (i.e. inconsistent)?
	 */
	public boolean isEmpty()
	{
		// Internally, inconsistency is flagged by setting d[0][0] to -1.
		// (Note: strictly speaking "<0" checks that the difference bound is less than "<0".)
		return d[0][0] < 0;
	}

	/**
	 * Is constraint c satisfied by this zone (i.e does it overlap)?
	 */
	public boolean isSatisfied(Constraint c)
	{
		return DB.add(c.db, d[c.y][c.x]) > 0;
	}

	/**
	 * Is a DBM (fully) included in this zone?
	 */
	public boolean includes(DBM dbm)
	{
		int i, j, n;
		int[][] d2 = dbm.d;
		n = pta.numClocks;
		for (i = 0; i < n + 1; i++) {
			for (j = 0; j < n + 1; j++) {
				if (d[i][j] < d2[i][j])
					return false;
			}
		}
		return true;
	}

	/**
	 * Get the minimum value of a clock. 
	 */
	public int getClockMin(int x)
	{
		return -DB.getSignedDiff(d[0][x]);
	}
	
	/**
	 * Get the maximum value of a clock. 
	 */
	public int getClockMax(int x)
	{
		return DB.getSignedDiff(d[x][0]);
	}
	
	/**
	 * Check if a clock is unbounded (can be infinite).
	 */
	public boolean clockIsUnbounded(int x)
	{
		return DB.isInfty(d[x][0]);
	}
	
	/**
	 * Check if all clocks are unbounded (can be infinite).
	 */
	public boolean allClocksAreUnbounded()
	{
		int i, n;
		n = pta.numClocks;
		for (i = 1; i < n + 1; i++) {
			if (!DB.isInfty(d[i][0])) {
				return false;
			}
		}
		return true;
	}
	
	// Misc
	
	/**
	 * Clone this zone
	 */
	public DBM deepCopy()
	{
		int i, j, n;
		DBM copy = new DBM(pta);
		n = pta.numClocks;
		copy.d = new int[n + 1][n + 1];
		for (i = 0; i < n + 1; i++) {
			for (j = 0; j < n + 1; j++) {
				copy.d[i][j] = d[i][j];
			}
		}
		return copy;
	}

	/**
	 * Get storage info string
	 */
	public String storageInfo()
	{
		return "DBM with " + pta.numClocks + " clocks";
	}

	// Standard Java methods

	public int hashCode()
	{
		// Simple hash code
		return pta.numClocks;
	}

	public boolean equals(Object o)
	{
		DBM dbm;
		int i, j, n;
		if (o == null)
			return false;
		try {
			dbm = (DBM) o;
		} catch (ClassCastException e) {
			return false;
		}
		n = pta.numClocks;
		for (i = 0; i < n + 1; i++) {
			for (j = 0; j < n + 1; j++) {
				if (d[i][j] != dbm.d[i][j])
					return false;
			}
		}
		return true;
	}

	// To string methods

	public String toString()
	{
		return toStringTextual();
	}

	/**
	 * Convert to string - textual representation
	 */
	public String toStringTextual()
	{
		int i, j, n;
		boolean first = true;
		String s = "", s2;
		n = pta.numClocks;
		// Trivial case - empty
		if (isEmpty())
			return "empty";
		// Generate textual description for each difference (pair)
		for (i = 0; i < n + 1; i++) {
			for (j = i + 1; j < n + 1; j++) {
				s2 = null;
				if (!DB.isInfty(d[i][j])) {
					if (!DB.isInfty(d[j][i])) {
						s2 = DB.constraintPairToString(i, j, d[i][j], d[j][i], pta);
					} else {
						s2 = DB.constraintToString(i, j, d[i][j], pta);
					}
				} else if (!DB.isInfty(d[j][i])) {
					s2 = DB.constraintToString(j, i, d[j][i], pta);
				}
				if (s2 != null) {
					if (!first)
						s += ",";
					else
						first = false;
					s += s2;
				}
			}
		}
		if ("".equals(s))
			return "true";
		else
			return "{" + s + "}";
	}

	/**
	 * Convert to string - DBM representation
	 */
	public String toStringDBM()
	{
		int i, j, n;
		String s = "[ ";
		n = pta.numClocks;
		for (i = 0; i < n + 1; i++) {
			for (j = 0; j < n + 1; j++) {
				if (j > 0)
					s += " ";
				s += DB.toString(d[i][j]);
			}
			if (i < n)
				s += ", ";
		}
		s += " ]";
		return s;
	}

	/* Private utility methods */

	/**
	 * Canonicalise, by applying Floyd-Warshall SPP algorithm 
	 */
	private void canonicalise()
	{
		int k, i, j, db, n;
		n = pta.numClocks;
		for (k = 0; k < n + 1; k++) {
			for (i = 0; i < n + 1; i++) {
				for (j = 0; j < n + 1; j++) {
					db = DB.add(d[i][k], d[k][j]);
					if (db < d[i][j])
						d[i][j] = db;
				}
			}
		}
	}

	/* Static zone creation methods */

	/**
	 * All clocks = 0
	 */
	public static DBM createZero(PTA pta)
	{
		int i, j, n;
		DBM dbm = new DBM(pta);
		n = pta.numClocks;
		for (i = 0; i < n + 1; i++) {
			for (j = 0; j < n + 1; j++) {
				dbm.d[i][j] = DB.LEQ_ZERO;
			}
		}
		return dbm;
	}

	/**
	 * All clocks any (non-negative) value
	 */
	public static DBM createTrue(PTA pta)
	{
		int i, j, n;
		DBM dbm = new DBM(pta);
		n = pta.numClocks;
		for (i = 0; i < n + 1; i++) {
			for (j = 0; j < n + 1; j++) {
				if (i == j)
					dbm.d[i][j] = DB.LEQ_ZERO;
				else if (i == 0)
					dbm.d[i][j] = DB.LEQ_ZERO;
				else
					dbm.d[i][j] = DB.INFTY;
			}
		}
		return dbm;
	}

	/**
	 * Zone defined by set of constraints
	 */
	public static DBM createFromConstraints(PTA pta, Iterable<Constraint> constrs)
	{
		DBM dbm = createTrue(pta);
		for (Constraint c : constrs) {
			dbm.addConstraint(c);
		}
		return dbm;
	}
}
