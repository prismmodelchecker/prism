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

/**
 * A region represents a subset of the valid parameter values.
 * New regions can be produced using the corresponding {@code RegionFactory},
 * or by applying operations on existing ones.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 * @see RegionFactory
 */
abstract class Region {
	static final int IMPLIES = 1;
	static final int IFF = 2;
	static final int OR = 3;
	static final int AND = 4;
	static final int EQ = 5;
	static final int NE = 6;
	static final int GT = 7;
	static final int GE = 8;
	static final int LT = 9;
	static final int LE = 10;
	static final int PLUS = 11;
	static final int MINUS = 12;
	static final int TIMES = 13;
	static final int DIVIDE = 14;
	static final int FIRST = 15;
	static final int AVG = 16;
	static final int COUNT = 17;
	static final int UMINUS = 18;
	static final int NOT = 19;
	static final int PARENTH = 20;
	static final int FORALL = 21;
	static final int EXISTS = 22;
	static final String opSymbols[] = { "", "=>", "<=>", "|", "&", "=", "!=", ">", ">=", "<", "<=", "+", "-", "*", "/", "1st", "avg", "-", "!", "()", "forall", "exists"};

	/** region factory to which this region belongs */
	protected RegionFactory factory;
	
	abstract int getDimensions();
	
	/**
	 * Get lower bound of the volume of the region.
	 * The volumes of disjoint region which cover the whole parameter
	 * space sum up to 1. This implies that the volumes are normalised
	 * according to the upper and lower bounds of the parameters.
	 * 
	 * @return lower bound of the volume of this region
	 */
	abstract BigRational volume();
	
	/**
	 * Checks whether this region contains the point {@code point}.
	 * 
	 * @param point point to check whether contained in this region
	 * @return true if the point is contained in this region, false else
	 */
	abstract boolean contains(Point point);
	
	/**
	 * Checks whether region {@code other} is contained in this region.
	 * 
	 * @param other check whether contained in this region
	 * @return true iff {@code other} is contained in this region
	 */
	abstract boolean contains(Region other);
	
	abstract RegionValues binaryOp(int op, StateValues values1, StateValues values2);

	abstract RegionValues ITE(StateValues valueI, StateValues valueT, StateValues valueE);

	/**
	 * Splits this region into several parts.
	 * How this is done exactly depends on the implementation in derived
	 * classes. Can take constraint into account.
	 * 
	 * @param constraint
	 */
	abstract ArrayList<Region> split(Function constraint);

	/**
	 * Splits this region into several parts.
	 * How this is done exactly depends on the implementation in derived
	 * classes.
	 */
	abstract ArrayList<Region> split();

	abstract ArrayList<Point> specialPoints();

	abstract Point randomPoint();

	/**
	 * Returns the region factory of this region.
	 * 
	 * @return region factory of this region
	 */
	RegionFactory getFactory() {
		return factory;
	}

	/**
	 * Returns the region operator by its string representation.
	 * If the string is not a valid operator, a {@code RuntimeException}
	 * will result.
	 * 
	 * @param opString string to convert to operator
	 * @return operator from string
	 */
	static int getOp(String opString) {
		for (int symNr = 0; symNr < Region.opSymbols.length; symNr++) {
			if (opString.equals(Region.opSymbols[symNr])) {
				return symNr;
			}
		}
		throw new RuntimeException("bad operator");
	}

	/**
	 * Returns the region which is the conjunction of this and {@code other} region.
	 * 
	 * @param other region to build conjunction with
	 * @return conjunction of this and the other region
	 */
	abstract Region conjunct(Region other);

	/**
	 * Checks whether this region is adjacent to another region.
	 * In this case, both can be glued by @{code glue}.
	 * 
	 * @param other region to check whether adjacent to
	 * @return true iff regions are adjacent
	 */
	abstract boolean adjacent(Region other);

	/**
	 * Glues this region with the adjacent region other.
	 * 
	 * @param other region to glue this region with
	 * @return combined region
	 */
	abstract Region glue(Region other);
}
