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

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Checks if functions are (strictly) larger than zero in whole region.
 * This class implements only an approximate check. This means, that
 * functions are only evaluated in a finite number of points, so that it
 * is unlikely but not impossible that there are other points in the
 * region which are below (or equal) to zero. Derived classes might
 * use a constraint solver to guarantee validity of this check.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
class ConstraintChecker {
	private boolean usedUnsoundCheck = false;

	/**
	 * Class to store keys for the cache of the decision procedure.
	 */
	class DecisionEntryKey {
		/** constraint to be stored (representing "constraint >=/> 0") */
		Function constraint;
		/** whether constraint should be strictly larger than zero */
		boolean strict;
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof DecisionEntryKey)) {
				return false;
			}
			DecisionEntryKey other = (DecisionEntryKey) obj;
			return this.constraint.equals(other.constraint) && (this.strict == other.strict);
		}
		
		@Override
		public int hashCode() {
			int hash = 0;
			hash = constraint.hashCode() + (hash << 6) + (hash << 16) - hash;
			hash = (strict ? 13 : 17) + (hash << 6) + (hash << 16) - hash;

			return hash;
		}
	}
	
	/**
	 * Class to store keys for the cache of the decision procedure.
	 */
	class DecisionEntryValue {
		/** region this result is valid for */
		Region region;
		/** result, that is whether corresponding constraint holds in region */
		boolean result;
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof DecisionEntryValue)) {
				return false;
			}
			DecisionEntryValue other = (DecisionEntryValue) obj;
			return this.region.equals(other.region) && (this.result == other.result);
		}
		
		@Override
		public int hashCode() {
			int hash = 0;
			hash = region.hashCode() + (hash << 6) + (hash << 16) - hash;
			hash = (result ? 13 : 17) + (hash << 6) + (hash << 16) - hash;

			return hash;
		}
	}

	/** number of random points to evaluate in decision procedure */
	private int numRandomPoints;
	/** decision cache */
	protected HashMap<DecisionEntryKey,ArrayList<DecisionEntryValue>> decisions;
	
	/**
	 * Constructs a new constraint checker.
	 * 
	 * @param numRandomPoints number of inner points to evaluate in addition to border points
	 */
	ConstraintChecker(int numRandomPoints) {
		this.numRandomPoints = numRandomPoints;
		decisions = new HashMap<DecisionEntryKey,ArrayList<DecisionEntryValue>>();
	}

	/**
	 * Main decision check.
	 * In this class, does nothing. Derived class could override this method
	 * for instance by calling an external decision procedure, use a library
	 * to decide validity in a given region, etc.
	 * 
	 * @param region region for which to check validity of constraint
	 * @param constraint constraint to check (whether >=/> 0)
	 * @param strict true iff ">" shold be checked rathern than ">="
	 * @return true
	 */
	boolean mainCheck(Region region, Function constraint, boolean strict)
	{
		usedUnsoundCheck = true;
		return true;
	}

	/**
	 * Does a quick pre-check by evaluating constraint at random points.
	 * 
	 * @param region region for which to check validity of constraint
	 * @param constraint constraint to check (whether >=/> 0)
	 * @param strict true iff ">" shold be checked rathern than ">="
	 * @return true if no counterexamples to validity found
	 */
	boolean preCheck(Region region, Function constraint, boolean strict)
	{
		ArrayList<Point> points = region.specialPoints();
		for (Point point : points) {
			if (!constraint.check(point, strict)) {
				return false;
			}
		}

		for (int pointNr = 0; pointNr < numRandomPoints; pointNr++) {
			if (!constraint.check(region.randomPoint(), strict)) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks whether constraint holds in given region.
	 * 
	 * @param region region for which to check validity of constraint
	 * @param constraint constraint to check (whether >=/> 0)
	 * @param strict true iff ">" shold be checked rathern than ">="
	 * @return true iff function values are (strictly) larger than zero in whole region 
	 */
	boolean check(Region region, Function constraint, boolean strict)
	{
		// handle case where the constraint is a constant number
		if (constraint.isConstant()) {
			BigRational value = constraint.asBigRational();

			if (value.isNaN())
				return false;

			if (strict) {
				return value.signum() == 1;
			} else {
				return value.signum() >= 0;
			}
		}

		Function constr = constraint.toConstraint();
		DecisionEntryKey key = new DecisionEntryKey();
		key.constraint = constr;
		key.strict = strict;
		ArrayList<DecisionEntryValue> entries = decisions.get(key);
		if (entries != null) {
			for (DecisionEntryValue entry : entries) {
				if (entry.region.contains(region)) {
					if (entry.result) {
						return true;
					} else if (entry.region.equals(region)) {
						return false;
					}
				}
			}
		}
		
		boolean result = preCheck(region, constr, strict);
		if (result) {
			result = mainCheck(region, constr, strict);
		}

		entries = decisions.get(key);
		if (entries == null) {
			entries = new ArrayList<DecisionEntryValue>();
			decisions.put(key, entries);
		}
		DecisionEntryValue entry = new DecisionEntryValue();
		entry.region = region;
		entry.result = result;
		entries.add(entry);

		return result;
	}

	public boolean unsoundCheckWasUsed()
	{
		return usedUnsoundCheck;
	}

}
