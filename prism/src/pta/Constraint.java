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
 * Single clock constraint.
 */
public class Constraint
{
	protected int x;
	protected int y;
	protected int db;

	/**
	 * Create a constraint.
	 * @param x Left clock
	 * @param y Right clock
	 * @param db Difference bound encoded as an integer
	 */
	private Constraint(int x, int y, int db)
	{
		this.x = x;
		this.y = y;
		this.db = db;
	}

	/**
	 * Copy constructor. 
	 */
	public Constraint(Constraint c)
	{
		this(c.x, c.y, c.db);
	}
	
	public int hashCode()
	{
		// Simple hash code
		return db;
	}

	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof Constraint))
			return false;
		Constraint c = (Constraint) o;
		return db == c.db && x == c.x && y == c.y;
	}

	public Constraint deepCopy()
	{
		return new Constraint(x, y, db);
	}

	// Reindex clocks and return self
	public Constraint renameClocks(PTA oldPta, PTA newPta)
	{
		x = PTA.renameClock(oldPta, newPta, x);
		y = PTA.renameClock(oldPta, newPta, y);
		return this;
	}

	public String toString(PTA pta)
	{
		return DB.constraintToString(x, y, db, pta);
	}

	public static String toStringList(PTA pta, Iterable<Constraint> list)
	{
		String s = "";
		boolean first;
		if (list == null || !list.iterator().hasNext())
			s += "true";
		else {
			first = true;
			for (Constraint c : list) {
				if (first)
					first = false;
				else
					s += ",";
				s += c.toString(pta);
			}
		}
		return s;
	}

	/**
	 * Build constraint x <= v.
	 */
	public static Constraint buildLeq(int x, int v)
	{
		return new Constraint(x, 0, DB.createLeq(v));
	}

	/**
	 * Build constraint x < v.
	 */
	public static Constraint buildLt(int x, int v)
	{
		return new Constraint(x, 0, DB.createLt(v));
	}

	/**
	 * Build constraint x >= v.
	 */
	public static Constraint buildGeq(int x, int v)
	{
		return new Constraint(0, x, DB.createLeq(-v));
	}

	/**
	 * Build constraint x > v.
	 */
	public static Constraint buildGt(int x, int v)
	{
		return new Constraint(0, x, DB.createLt(-v));
	}

	/**
	 * Build constraint x <= y.
	 */
	public static Constraint buildXLeqY(int x, int y)
	{
		return new Constraint(x, y, DB.createLeq(0));
	}

	/**
	 * Build constraint x < y.
	 */
	public static Constraint buildXLtY(int x, int y)
	{
		return new Constraint(x, y, DB.createLt(0));
	}

	/**
	 * Build constraint x >= y.
	 */
	public static Constraint buildXGeqY(int x, int y)
	{
		return new Constraint(y, x, DB.createLeq(0));
	}

	/**
	 * Build constraint x > y.
	 */
	public static Constraint buildXGtY(int x, int y)
	{
		return new Constraint(y, x, DB.createLt(0));
	}
	
	/**
	 * Build constraint x - y <= v.
	 */
	public static Constraint buildXYLeq(int x, int y, int v)
	{
		return new Constraint(x, y, DB.createLeq(v));
	}

	/**
	 * Build constraint x - y < v.
	 */
	public static Constraint buildXYLt(int x, int y, int v)
	{
		return new Constraint(x, y, DB.createLt(v));
	}

	/**
	 * Build constraint x - y >= v.
	 */
	public static Constraint buildXYGeq(int x, int y, int v)
	{
		return new Constraint(y, x, DB.createLeq(-v));
	}

	/**
	 * Build constraint x - y > v.
	 */
	public static Constraint buildXYGt(int x, int y, int v)
	{
		return new Constraint(y, x, DB.createLt(-v));
	}
}
