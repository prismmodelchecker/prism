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

public class DBMFactory implements ZoneFactory
{
	/* Basic zone creation */

	/**
	 * All clocks = 0
	 */
	public DBM createZero(PTA pta)
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
	public DBM createTrue(PTA pta)
	{
		int i, j, n;
		DBM dbm = new DBM(pta);
		n = pta.numClocks;
		for (i = 0; i < n + 1; i++) {
			for (j = 0; j < n + 1; j++) {
				if (i == j) dbm.d[i][j] = DB.LEQ_ZERO;
				else if (i == 0) dbm.d[i][j] = DB.LEQ_ZERO;
				else  dbm.d[i][j] = DB.INFTY;
			}
		}
		return dbm;
	}

	/**
	 * Zone defined by set of constraints
	 */
	public DBM createFromConstraints(PTA pta, Iterable<Constraint> constrs)
	{
		DBM dbm = createTrue(pta);
		for (Constraint c : constrs) {
			dbm.addConstraint(c);
		}
		return dbm;
	}
}
