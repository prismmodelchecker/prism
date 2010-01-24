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
 * Helper class for encapsulation of difference bound as an integer.
 * We use the least significant bit to encode strictness in the bound
 * (1 = non-strict, 0 = strict); the rest is for the (integer) bound.
 * Upper bound of infinity is encoded as Integer.MAX_VALUE.
 */
public class DB
{
	// Bit masks, etc.
	protected final static int NON_STRICT = 0x1;
	protected final static int DIFF_MASK = ~0x1;
	protected final static int INFTY = Integer.MAX_VALUE;
	protected final static int LEQ_ZERO = 0x1;
	protected final static int LT_ZERO = 0x0;
	protected final static int LEQ_MINUS_ONE = (-1 << 1) | NON_STRICT;

	// Created bound <=v
	protected static int createLeq(int v)
	{
		return (v << 1) | NON_STRICT;
	}

	// Create bound <v
	protected static int createLt(int v)
	{
		return (v << 1);
	}

	// Get (signed) difference part of bound
	protected static int getSignedDiff(int d)
	{
		return (DIFF_MASK & d) >> 1;
	}

	// Is bound strict?
	protected static boolean isStrict(int d)
	{
		return (NON_STRICT & d) == 0;
	}

	// Is infinity
	protected static boolean isInfty(int d)
	{
		return (d == INFTY);
	}

	// Add two bounds
	protected static int add(int d1, int d2)
	{
		// Anything + inf = inf
		if (d1 == INFTY || d2 == INFTY)
			return INFTY;
		// Both leq: <=m + <=n = <=m+n; else: <m+n
		return d1 + d2 - ((d1 & NON_STRICT) | (d2 & NON_STRICT));
	}

	// Compute dual bound: flip both strictness and sign
	protected static int dual(int d)
	{
		if (d == INFTY)
			return d;
		// TODO: probably a more efficient way to do this
		return isStrict(d) ? createLeq(-getSignedDiff(d)) : createLt(-getSignedDiff(d)); 
	}

	// Get bound as a string
	protected static String toString(int d)
	{
		if (d == INFTY)
			return "<inf";
		return (isStrict(d) ? "<" : "<=") + getSignedDiff(d);
	}

	// Get bound as a string, multiplied by -1 and flipped, e.g. "<v" -> "-v<" 
	protected static String toStringFlipped(int d)
	{
		if (d == INFTY)
			return "-inf<";
		return (-getSignedDiff(d)) + (isStrict(d) ? "<" : "<=");
	}

	// Get constraint as a string
	protected static String constraintToString(int i, int j, int d, PTA pta)
	{
		String s;
		if (i > 0) {
			// x-y < v
			if (j > 0) {
				s = pta.getClockName(i);
				// x-y < inf
				if (isInfty(d))
					s += "-" + pta.getClockName(j) + "<inf";
				// x-y < 0  ->  x < y
				else if (getSignedDiff(d) == 0)
					s += (isStrict(d) ? "<" : "<=") + pta.getClockName(j);
				// x-y < v
				else
					s += "-" + pta.getClockName(j) + (isStrict(d) ? "<" : "<=") + getSignedDiff(d);
				// x-0 < v
			} else {
				s = pta.getClockName(i);
				if (isInfty(d))
					s += "<inf";
				else
					s += (isStrict(d) ? "<" : "<=") + getSignedDiff(d);
			}
		}
		// 0-y < v  ->  y > -v
		else {
			s = pta.getClockName(j);
			if (isInfty(d))
				s += ">-inf";
			else
				s += (isStrict(d) ? ">" : ">=") + (-getSignedDiff(d));
		}
		return s;
	}

	// Get constraint pair as a string
	// (x - y < v2 and y - x < v1, i.e. -v1 < x - y < v2) where x,y are clocks i,j and vk is from dk 
	protected static String constraintPairToString(int i, int j, int d2, int d1, PTA pta)
	{
		String s;
		if (i > 0) {
			// -v1 < x-y < v2
			if (j > 0) {
				if (!isInfty(d2) && (-getSignedDiff(d1) == getSignedDiff(d2))) {
					if (getSignedDiff(d1) == 0)
						s = pta.getClockName(i) + "=" + pta.getClockName(j);
					else
						s = pta.getClockName(i) + "-" + pta.getClockName(j) + "=" + getSignedDiff(d2);
				} else {
					s = toStringFlipped(d1) + pta.getClockName(i) + "-" + pta.getClockName(j) + toString(d2);
				}
				// -v1 < x-0 < v2
			} else {
				if (!isInfty(d2) && (-getSignedDiff(d1) == getSignedDiff(d2)))
					s = pta.getClockName(i) + "=" + getSignedDiff(d2);
				else
					s = toStringFlipped(d1) + pta.getClockName(i) + toString(d2);
			}
		}
		// -v1 < 0-y < v2  ->  -v2 < y < v1
		else {
			if (!isInfty(d2) && (-getSignedDiff(d1) == getSignedDiff(d2)))
				s = pta.getClockName(j) + "=" + getSignedDiff(d1);
			else
				s = toStringFlipped(d2) + pta.getClockName(j) + toString(d1);
		}
		return s;
	}
}
